package dev.dtp.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import dev.dtp.guide.GuideBook;
import dev.dtp.guide.GuideBook.Page;
import dev.dtp.guide.GuideGrids;
import dev.dtp.guide.GuideGrids.Grid;

/**
 * Das DTP-Handbuch: ein Inhaltsverzeichnis und Seiten mit Titel, Text und optional einem Crafting-Raster.
 * Aufbau und Text kommen aus {@code assets/dtp/guide/<sprache>.md} ({@link GuideBooks}), die Raster aus
 * {@link GuideGrids}. Seiten, deren Text nicht passt, laufen auf Folgeseiten weiter.
 */
public class GuideScreen extends Screen {
    private static final int W = 280;
    private static final int H = 200;
    private static final int PAD = 12;
    private static final int TEXT_TOP = 40;
    private static final int LINE = 9;
    /** Zeilen pro Seite zwischen Titel und Fußzeile. */
    private static final int LINES_PER_SHEET = (H - TEXT_TOP - 26) / LINE;
    private static final int SLOT = 18;
    /** Raster (3 Felder), Pfeil, Ergebnisfeld. */
    private static final int GRID_W = 3 * SLOT + 24 + SLOT;
    private static final int TEXT_W = W - 2 * PAD;
    private static final int TEXT_W_BESIDE_GRID = TEXT_W - GRID_W - 10;
    private static final int BULLET_INDENT = 8;
    private static final int INDEX_TOP = 34;
    private static final int INDEX_MAX_LINE = 13;

    private static final int PAPER = 0xFFF4EBD3;
    private static final int PAPER_SHADE = 0xFFE6D8B5;
    private static final int LEATHER = 0xFF5A3E1B;
    private static final int INK = 0xFF2B2118;
    private static final int FADED_INK = 0xFF7A6A55;
    private static final int WARN_INK = 0xFFA02020;
    private static final int LINK_INK = 0xFF1F4E9A;

    /** Zuletzt gelesene Seite, damit das Buch dort wieder aufgeht. {@code -1} ist das Inhaltsverzeichnis. */
    private static int lastPage = -1;

    /** Eine Zeile auf der Seite, mit Einzug für Aufzählungen. */
    private record Line(FormattedCharSequence text, int indent) {
    }

    /** Eine gesetzte Seite: Ausschnitt einer Buchseite, die erste mit Raster, Folgeseiten ohne. */
    private record Sheet(Page page, int chapter, List<Line> lines, boolean continued) {
    }

    private GuideBook book;
    private final List<Sheet> sheets = new ArrayList<>();
    private final List<Integer> chapterStart = new ArrayList<>();
    private int page;
    private int left;
    private int top;
    private PageButton back;
    private PageButton forward;

    public GuideScreen() {
        super(Component.translatable("item.dtp.guide"));
        this.page = lastPage;
    }

    public static void open() {
        Minecraft.getInstance().gui.setScreen(new GuideScreen());
    }

    @Override
    protected void init() {
        left = (width - W) / 2;
        top = Math.max(4, (height - H - 24) / 2);
        book = GuideBooks.current();
        layout();
        page = Math.clamp(page, -1, sheets.size() - 1);
        back = addRenderableWidget(new PageButton(left + PAD, top + H - 20, false, b -> turn(page - 1), true));
        forward = addRenderableWidget(new PageButton(left + W - PAD - 23, top + H - 20, true, b -> turn(page + 1), true));
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 100, top + H + 4, 200, 20).build());
        updateButtons();
    }

    private void turn(int target) {
        page = Math.clamp(target, -1, sheets.size() - 1);
        lastPage = page;
        updateButtons();
    }

    private void updateButtons() {
        back.visible = page >= 0;
        forward.visible = page < sheets.size() - 1;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    // --- Satz -------------------------------------------------------------------------------

    /**
     * Verteilt den Text aller Seiten auf Blätter. Ein Absatz wandert als Ganzes auf das nächste Blatt, wenn er nicht
     * mehr passt, und wird dort in dessen Breite neu umbrochen. Nur wer länger als ein ganzes Blatt ist, wird geteilt.
     */
    private void layout() {
        sheets.clear();
        chapterStart.clear();
        List<GuideBook.Chapter> chapters = book.chapters();
        for (int c = 0; c < chapters.size(); c++) {
            chapterStart.add(sheets.size());
            for (Page p : chapters.get(c).pages()) {
                layoutPage(p, c);
            }
        }
    }

    private void layoutPage(Page p, int chapter) {
        List<Line> lines = new ArrayList<>();
        boolean continued = false;
        for (List<String> block : blocks(p.body())) {
            int width = !continued && p.grid() != null ? TEXT_W_BESIDE_GRID : TEXT_W;
            List<Line> blockLines = wrap(block, width);
            int gap = lines.isEmpty() ? 0 : 1;
            if (lines.size() + gap + blockLines.size() > LINES_PER_SHEET && !lines.isEmpty()) {
                sheets.add(new Sheet(p, chapter, List.copyOf(lines), continued));
                lines.clear();
                continued = true;
                gap = 0;
                blockLines = wrap(block, TEXT_W);
            }
            if (gap > 0) {
                lines.add(new Line(FormattedCharSequence.EMPTY, 0));
            }
            for (Line line : blockLines) {
                if (lines.size() == LINES_PER_SHEET) {
                    sheets.add(new Sheet(p, chapter, List.copyOf(lines), continued));
                    lines.clear();
                    continued = true;
                }
                lines.add(line);
            }
        }
        sheets.add(new Sheet(p, chapter, List.copyOf(lines), continued));
    }

    /** Teilt den Seitentext an Leerzeilen in Blöcke (Absatz oder Aufzählung). */
    private static List<List<String>> blocks(List<String> body) {
        List<List<String>> blocks = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String line : body) {
            if (line.isEmpty()) {
                if (!current.isEmpty()) {
                    blocks.add(current);
                    current = new ArrayList<>();
                }
            } else {
                current.add(line);
            }
        }
        if (!current.isEmpty()) {
            blocks.add(current);
        }
        return blocks;
    }

    private List<Line> wrap(List<String> block, int width) {
        List<Line> out = new ArrayList<>();
        for (String text : block) {
            if (text.startsWith(GuideBook.BULLET)) {
                List<FormattedCharSequence> parts = font.split(inline(text.substring(GuideBook.BULLET.length())), width - BULLET_INDENT);
                for (int i = 0; i < parts.size(); i++) {
                    FormattedCharSequence part = i == 0
                            ? FormattedCharSequence.composite(FormattedCharSequence.forward("•", Style.EMPTY), parts.get(0))
                            : parts.get(i);
                    out.add(new Line(part, i == 0 ? BULLET_INDENT - font.width("•") : BULLET_INDENT));
                }
            } else {
                font.split(inline(text), width).forEach(s -> out.add(new Line(s, 0)));
            }
        }
        return out;
    }

    /** {@code **fett**}, {@code *kursiv*} und {@code \*} für ein wörtliches Sternchen. */
    static MutableComponent inline(String text) {
        MutableComponent out = Component.empty();
        StringBuilder run = new StringBuilder();
        boolean bold = false;
        boolean italic = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\\' && i + 1 < text.length()) {
                run.append(text.charAt(++i));
            } else if (c == '*') {
                flush(out, run, bold, italic);
                if (i + 1 < text.length() && text.charAt(i + 1) == '*') {
                    bold = !bold;
                    i++;
                } else {
                    italic = !italic;
                }
            } else {
                run.append(c);
            }
        }
        flush(out, run, bold, italic);
        return out;
    }

    private static void flush(MutableComponent out, StringBuilder run, boolean bold, boolean italic) {
        if (!run.isEmpty()) {
            out.append(Component.literal(run.toString()).withStyle(Style.EMPTY.withBold(bold).withItalic(italic)));
            run.setLength(0);
        }
    }

    // --- Zeichnen ---------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        g.fill(left - 3, top - 3, left + W + 3, top + H + 3, LEATHER);
        g.fill(left, top, left + W, top + H, PAPER);
        g.fill(left, top, left + 4, top + H, PAPER_SHADE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (page < 0) {
            contents(g, mouseX, mouseY);
        } else {
            sheet(g, sheets.get(page), mouseX, mouseY);
        }
    }

    private int indexLine() {
        int count = Math.max(1, book.chapters().size());
        return Math.min(INDEX_MAX_LINE, (H - INDEX_TOP - 24) / count);
    }

    private void contents(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.centeredText(font, title.copy().withStyle(ChatFormatting.BOLD), left + W / 2, top + 12, INK);
        g.centeredText(font, Component.translatable("guide.dtp.contents"), left + W / 2, top + 22, FADED_INK);
        List<GuideBook.Chapter> chapters = book.chapters();
        int hovered = chapterAt(mouseX, mouseY);
        for (int i = 0; i < chapters.size(); i++) {
            int y = top + INDEX_TOP + i * indexLine();
            MutableComponent label = Component.literal((i + 1) + ".  " + chapters.get(i).title());
            if (hovered == i) {
                label = label.withStyle(ChatFormatting.UNDERLINE);
            }
            g.text(font, label, left + PAD + 24, y, hovered == i ? LINK_INK : INK, false);
        }
    }

    private int chapterAt(double mouseX, double mouseY) {
        if (page >= 0 || mouseX < left + PAD || mouseX > left + W - PAD) {
            return -1;
        }
        int i = (int) Math.floor((mouseY - top - INDEX_TOP + 2) / indexLine());
        return i >= 0 && i < book.chapters().size() ? i : -1;
    }

    private void sheet(GuiGraphicsExtractor g, Sheet s, int mouseX, int mouseY) {
        Page p = s.page();
        g.text(font, Component.literal(book.chapters().get(s.chapter()).title()), left + PAD, top + 10, FADED_INK, false);
        String number = (page + 1) + " / " + sheets.size();
        g.text(font, number, left + W - PAD - font.width(number), top + 10, FADED_INK, false);

        if (p.warning()) {
            g.fill(left + PAD, top + 22, left + PAD + 3, top + 32, WARN_INK);
        }
        MutableComponent heading = inline(p.title()).withStyle(ChatFormatting.BOLD);
        if (s.continued()) {
            heading = heading.append(Component.literal(" ").append(Component.translatable("guide.dtp.continued"))
                    .withStyle(Style.EMPTY.withBold(false)));
        }
        g.text(font, heading, left + PAD + (p.warning() ? 7 : 0), top + 24, p.warning() ? WARN_INK : INK, false);

        for (int i = 0; i < s.lines().size(); i++) {
            Line line = s.lines().get(i);
            g.text(font, line.text(), left + PAD + line.indent(), top + TEXT_TOP + i * LINE, INK, false);
        }
        Grid grid = s.continued() || p.grid() == null ? null : GuideGrids.byName(p.grid());
        if (grid != null) {
            grid(g, grid, left + W - PAD - GRID_W, top + TEXT_TOP + 4, mouseX, mouseY);
        }

        Component toContents = Component.translatable("guide.dtp.contents");
        int cx = left + W / 2 - font.width(toContents) / 2;
        int cy = top + H - 17;
        boolean hover = overContentsLink(mouseX, mouseY);
        g.text(font, hover ? toContents.copy().withStyle(ChatFormatting.UNDERLINE) : toContents, cx, cy, hover ? LINK_INK : FADED_INK, false);
    }

    private boolean overContentsLink(double mouseX, double mouseY) {
        if (page < 0) {
            return false;
        }
        int half = font.width(Component.translatable("guide.dtp.contents")) / 2 + 2;
        int cy = top + H - 17;
        return mouseX >= left + W / 2 - half && mouseX <= left + W / 2 + half && mouseY >= cy - 2 && mouseY <= cy + LINE;
    }

    private void grid(GuiGraphicsExtractor g, Grid grid, int x, int y, int mouseX, int mouseY) {
        for (int i = 0; i < 9; i++) {
            int sx = x + (i % 3) * SLOT;
            int sy = y + (i / 3) * SLOT;
            slot(g, sx, sy);
            Gui.item(g, Gui.stack(grid.slots().get(i)), sx + 1, sy + 1, mouseX, mouseY);
        }
        // Pfeil zum Ergebnis
        int ax = x + 3 * SLOT + 5;
        int ay = y + SLOT + 8;
        g.fill(ax, ay, ax + 10, ay + 2, FADED_INK);
        for (int k = 0; k < 4; k++) {
            g.fill(ax + 10 + k, ay - 3 + k, ax + 11 + k, ay + 5 - k, FADED_INK);
        }
        int rx = x + 3 * SLOT + 24;
        int ry = y + SLOT;
        slot(g, rx, ry);
        ItemStack result = Gui.stack(grid.result());
        result.setCount(grid.count());
        Gui.item(g, result, rx + 1, ry + 1, mouseX, mouseY);
        g.itemDecorations(font, result, rx + 1, ry + 1, null);
        if (grid.shapeless()) {
            g.text(font, Component.translatable("guide.dtp.shapeless"), x, y + 3 * SLOT + 4, FADED_INK, false);
        }
    }

    /** Feld wie im Inventar: dunkle Kante oben links, helle unten rechts. */
    private static void slot(GuiGraphicsExtractor g, int x, int y) {
        g.fill(x, y, x + SLOT, y + SLOT, 0xFF373737);
        g.fill(x + 1, y + 1, x + SLOT, y + SLOT, 0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, 0xFF8B8B8B);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        // 26.3 liest die Maus über SDL: links ist 1, nicht 0 wie früher bei GLFW. Nie als Zahl prüfen.
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            int chapter = chapterAt(event.x(), event.y());
            if (chapter >= 0) {
                turn(chapterStart.get(chapter));
                return true;
            }
            if (overContentsLink(event.x(), event.y())) {
                turn(-1);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
