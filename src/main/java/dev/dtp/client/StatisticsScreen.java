package dev.dtp.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.dtp.currency.Money;
import dev.dtp.menu.StatisticsMenu;

/**
 * Statistik-Tafel. Reiter „Verkäufe“: was der Server je Ware verkauft und erlöst hat, in den letzten 14 Markttagen
 * oder insgesamt, gefiltert nach Marktgruppen wie am Marktstand und sortierbar nach Stück oder Taler. Reiter
 * „Contracts“: Gesamtzahl und je Spieler die Abschlüsse je Stufe. Reiter „Einnahmen“: je Minecraft-Tag ein
 * gestapelter Balken aus Verkaufserlös und Contract-Belohnungen; die Achse richtet sich nach dem stärksten Tag, die
 * genauen Zahlen zeigt der Tooltip.
 *
 * <p>Alles hier ist Ansicht: Reiter, Zeitraum, Gruppe, Sortierung und Scrollen laufen auf dem Client.
 */
public class StatisticsScreen extends PanelScreen<StatisticsMenu> {
    private static final int WIDTH = 280;
    private static final int HEIGHT = 236;
    private static final int LEFT = 8;
    private static final int INNER = WIDTH - 2 * LEFT;
    private static final int TAB_TOP = 18;
    private static final int CATEGORY_TOP = 40;
    /** Oberkante der Summenzeile bei einer Reihe Gruppenknöpfe; jede weitere Reihe schiebt alles darunter nach unten. */
    private static final int SUMMARY_TOP = 62;
    private static final int SALES_ROW = 18;
    private static final int SALES_ROWS = 7;
    /** Rechte Kante der Spalten „Stück“ und „Taler“; rechts davon bleibt Platz für die Scrollleiste. */
    private static final int COUNT_RIGHT = 186;
    private static final int TALER_RIGHT = WIDTH - LEFT - 6;
    private static final int CONTRACT_TOP = 40;
    private static final int CONTRACT_ROW = 12;
    private static final int CONTRACT_ROWS = 13;
    private static final int TIER_STEP = 30;
    private static final int TAB_WIDTH = 64;
    /** Umschaltknopf für den Zeitraum, rechts in der Reihe der Gruppenknöpfe. */
    private static final int RANGE_WIDTH = 48;
    private static final int INCOME_TOP = 40;
    /** Platz links der Diagrammfläche für die Achsenbeschriftung. */
    private static final int AXIS_WIDTH = 26;
    private static final int CHART_LEFT = LEFT + AXIS_WIDTH;
    private static final int CHART_RIGHT = WIDTH - LEFT;
    private static final int CHART_TOP = INCOME_TOP + 18;
    /** Unterkante der Diagrammfläche bei einer Reihe Gruppenknöpfe; darunter steht die Tagesbeschriftung. */
    private static final int CHART_BOTTOM = HEIGHT - 26;

    /** Rechts bleibt Platz für den Zeitraumknopf; bei den Standardgruppen bleibt die Leiste einreihig. */
    private final CategoryBar categories = new CategoryBar(LEFT, CATEGORY_TOP, INNER - RANGE_WIDTH - 4);
    private Tab tab = Tab.SALES;
    private boolean recent = true;
    private SortColumn sort = SortColumn.TALER;
    private boolean descending = true;
    private int salesScroll;
    private int contractScroll;

    public StatisticsScreen(StatisticsMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        CompoundTag d = data();
        local(Component.translatable("dtp.stats.tab.sales"), LEFT, TAB_TOP, TAB_WIDTH, tab != Tab.SALES, () -> tab = Tab.SALES);
        local(Component.translatable("dtp.stats.tab.contracts"), LEFT + TAB_WIDTH + 2, TAB_TOP, TAB_WIDTH, tab != Tab.CONTRACTS,
                () -> tab = Tab.CONTRACTS);
        local(Component.translatable("dtp.stats.tab.income"), LEFT + 2 * (TAB_WIDTH + 2), TAB_TOP, TAB_WIDTH, tab != Tab.INCOME,
                () -> tab = Tab.INCOME);
        if (tab == Tab.SALES) {
            // Ein Knopf, der den gewählten Zeitraum zeigt und bei jedem Klick wechselt.
            int days = d.getIntOr("days", 14);
            Component range = recent ? Component.translatable("dtp.stats.range.recent", days) : Component.translatable("dtp.stats.range.all");
            local(range, WIDTH - LEFT - RANGE_WIDTH, CATEGORY_TOP, RANGE_WIDTH, true, () -> setRecent(!recent))
                    .setTooltip(Tooltip.create(Component.translatable("dtp.stats.range.hint", days)));
            categories.init(d, this::local, () -> salesScroll = 0);
        }
    }

    private void setRecent(boolean value) {
        recent = value;
        salesScroll = 0;
    }

    /** Knopf, der nur die Ansicht ändert und nichts an den Server schickt. */
    private Button local(Component label, int x, int y, int width, boolean active, Runnable action) {
        Button b = Button.builder(label, btn -> {
            action.run();
            rebuildWidgets();
        }).bounds(leftPos + x, topPos + y, width, 18).build();
        b.active = active;
        return addRenderableWidget(b);
    }

    @Override
    protected String layoutKey() {
        CompoundTag d = data();
        return tab + "|" + recent + "|" + d.getListOrEmpty("categories").size() + "|" + d.getIntOr("days", 14);
    }

    /** Die Gruppenknöpfe machen das Panel auf beiden Reitern gleich hoch, damit es beim Wechsel nicht springt. */
    private int extra() {
        return categories.extraHeight(data());
    }

    @Override
    protected int panelHeight() {
        return imageHeight + extra();
    }

    // --- Verkäufe: Filter und Sortierung ----------------------------------------------------

    private record Row(ItemStack stack, String name, long count, long taler) {
    }

    private List<Row> rows() {
        CompoundTag d = data();
        TagKey<Item> tag = categories.tag(d);
        ListTag list = d.getListOrEmpty(recent ? "recent" : "all");
        List<Row> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            ItemStack stack = Gui.stack(e.getStringOr("item", ""));
            if (stack.isEmpty() || !CategoryBar.matches(tag, stack)) {
                continue;
            }
            result.add(new Row(stack, stack.getHoverName().getString(), e.getLongOr("count", 0), e.getLongOr("taler", 0)));
        }
        Comparator<Row> order = sort == SortColumn.COUNT ? Comparator.comparingLong(Row::count) : Comparator.comparingLong(Row::taler);
        if (descending) {
            order = order.reversed();
        }
        // Gleichstände nach Namen, damit die Reihenfolge nicht springt.
        result.sort(order.thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private int salesTop() {
        return SUMMARY_TOP + extra() + 24;
    }

    private int headerY() {
        return SUMMARY_TOP + extra() + 12;
    }

    /** Spaltenkopf „Stück“ oder „Taler“ unter der Maus, oder null. */
    private SortColumn headerAt(int mx, int my) {
        if (tab != Tab.SALES || my < headerY() - 1 || my >= headerY() + 9) {
            return null;
        }
        int countWidth = font.width(header(SortColumn.COUNT));
        if (mx >= COUNT_RIGHT - countWidth - 2 && mx < COUNT_RIGHT + 2) {
            return SortColumn.COUNT;
        }
        int talerWidth = font.width(header(SortColumn.TALER));
        if (mx >= TALER_RIGHT - talerWidth - 2 && mx < TALER_RIGHT + 2) {
            return SortColumn.TALER;
        }
        return null;
    }

    /** Klick auf einen Spaltenkopf: dieselbe Spalte wechselt zwischen absteigend und aufsteigend, eine neue beginnt absteigend. */
    private void toggleSort(SortColumn column) {
        if (sort == column) {
            descending = !descending;
        } else {
            sort = column;
            descending = true;
        }
        salesScroll = 0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            SortColumn header = headerAt((int) event.x() - leftPos, (int) event.y() - topPos);
            if (header != null) {
                Button.playButtonClickSound(Minecraft.getInstance().getSoundManager());
                toggleSort(header);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return super.mouseScrolled(x, y, scrollX, scrollY);
        }
        int step = scrollY > 0 ? -1 : 1;
        switch (tab) {
            case SALES -> salesScroll = clamp(salesScroll + step, rows().size(), SALES_ROWS);
            case CONTRACTS -> contractScroll = clamp(contractScroll + step, contractLines(), CONTRACT_ROWS);
            case INCOME -> {
                // Das Diagramm zeigt immer alle Tage, es gibt nichts zu scrollen.
            }
        }
        return true;
    }

    private static int clamp(int scroll, int size, int visible) {
        return Math.clamp(scroll, 0, Math.max(0, size - visible));
    }

    // --- Zeichnen ---------------------------------------------------------------------------

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        switch (tab) {
            case SALES -> sales(g, mouseX, mouseY);
            case CONTRACTS -> contracts(g, mouseX, mouseY);
            case INCOME -> income(g, mouseX, mouseY);
        }
    }

    private void sales(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        categories.icons(g, d);
        List<Row> rows = rows();
        salesScroll = clamp(salesScroll, rows.size(), SALES_ROWS);

        long count = 0;
        long taler = 0;
        for (Row r : rows) {
            count += r.count();
            taler += r.taler();
        }
        int summaryY = SUMMARY_TOP + extra();
        Component total = Money.component(taler);
        String pieces = Component.translatable("dtp.stats.pieces", Money.format(count)).getString();
        int labelWidth = COUNT_RIGHT - font.width(pieces) - 6 - LEFT;
        g.text(font, Gui.fit(font, Component.translatable("dtp.stats.summary"), labelWidth), LEFT, summaryY, Gui.MUTED, false);
        g.text(font, pieces, COUNT_RIGHT - font.width(pieces), summaryY, Gui.TEXT, false);
        g.text(font, total, TALER_RIGHT - font.width(total), summaryY, Gui.GOLD, false);

        SortColumn hovered = headerAt(mouseX, mouseY);
        int headerY = headerY();
        g.text(font, Component.translatable("dtp.stats.column.item"), LEFT + 20, headerY, Gui.MUTED, false);
        Component countHeader = header(SortColumn.COUNT);
        g.text(font, countHeader, COUNT_RIGHT - font.width(countHeader), headerY, headerColor(SortColumn.COUNT, hovered), false);
        Component talerHeader = header(SortColumn.TALER);
        g.text(font, talerHeader, TALER_RIGHT - font.width(talerHeader), headerY, headerColor(SortColumn.TALER, hovered), false);
        if (hovered != null) {
            g.setTooltipForNextFrame(font, Component.translatable("dtp.market.sort_hint"), mouseX, mouseY);
        }

        int top = salesTop();
        if (rows.isEmpty()) {
            boolean anySales = !d.getListOrEmpty(recent ? "recent" : "all").isEmpty();
            Component empty = Component.translatable(anySales ? "dtp.market.no_match" : recent ? "dtp.stats.empty_recent" : "dtp.stats.empty");
            g.textWithWordWrap(font, empty, LEFT, top + 4, INNER, Gui.MUTED, false);
            return;
        }
        // Balken relativ zum größten Wert der Spalte, nach der sortiert wird.
        long max = 1;
        for (Row r : rows) {
            max = Math.max(max, sort == SortColumn.COUNT ? r.count() : r.taler());
        }
        for (int i = salesScroll; i < Math.min(rows.size(), salesScroll + SALES_ROWS); i++) {
            Row r = rows.get(i);
            int y = top + (i - salesScroll) * SALES_ROW;
            Gui.item(g, r.stack(), LEFT, y, mouseX, mouseY);
            String countText = Gui.compact(r.count());
            String talerText = Money.format(r.taler());
            int nameRight = COUNT_RIGHT - font.width(countText) - 6;
            g.text(font, Gui.fit(font, Component.literal(r.name()), nameRight - LEFT - 20), LEFT + 20, y + 2, Gui.TEXT, false);
            g.text(font, countText, COUNT_RIGHT - font.width(countText), y + 2, sort == SortColumn.COUNT ? Gui.TEXT : Gui.MUTED, false);
            g.text(font, talerText, TALER_RIGHT - font.width(talerText), y + 2, Gui.GOLD, false);
            long value = sort == SortColumn.COUNT ? r.count() : r.taler();
            int barWidth = (int) Math.max(1, (TALER_RIGHT - LEFT - 20) * value / max);
            g.fill(LEFT + 20, y + 12, TALER_RIGHT, y + 13, 0x30FFFFFF);
            g.fill(LEFT + 20, y + 12, LEFT + 20 + barWidth, y + 13, Gui.GOLD);
        }
        scrollbar(g, top, SALES_ROWS * SALES_ROW, rows.size(), SALES_ROWS, salesScroll);
    }

    private Component header(SortColumn column) {
        Component label = Component.translatable(column == SortColumn.COUNT ? "dtp.stats.column.count" : "dtp.stats.column.taler");
        if (sort != column) {
            return label;
        }
        return Component.empty().append(label).append(descending ? " ▼" : " ▲");
    }

    private int headerColor(SortColumn column, SortColumn hovered) {
        return sort == column ? Gui.GOLD : hovered == column ? Gui.TEXT : Gui.MUTED;
    }

    // --- Contracts --------------------------------------------------------------------------

    /** Zeilen der Spielertabelle einschließlich „Ohne Zuordnung“. */
    private int contractLines() {
        CompoundTag d = data();
        return d.getListOrEmpty("players").size() + (d.getIntOr("unassigned", 0) > 0 ? 1 : 0);
    }

    /** Rechte Kante der Spalte einer Stufe; Spalte {@code tiers + 1} ist die Summe. */
    private static int tierRight(int column, int tiers) {
        return TALER_RIGHT - (tiers + 1 - column) * TIER_STEP;
    }

    private void contracts(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        CompoundTag d = data();
        int tiers = Math.max(1, d.getIntOr("tiers", 4));
        int top = CONTRACT_TOP;
        Component total = Component.literal(Money.format(d.getIntOr("contracts_total", 0)));
        g.text(font, Component.translatable("dtp.stats.contracts_total"), LEFT, top, Gui.MUTED, false);
        g.text(font, total, TALER_RIGHT - font.width(total), top, Gui.GOLD, false);

        int headerY = top + 16;
        g.text(font, Component.translatable("dtp.stats.column.player"), LEFT, headerY, Gui.MUTED, false);
        for (int tier = 1; tier <= tiers + 1; tier++) {
            boolean sum = tier > tiers;
            String label = sum ? "Σ" : Component.translatable("dtp.stats.column.tier", tier).getString();
            int right = tierRight(tier, tiers);
            g.text(font, label, right - font.width(label), headerY, sum ? Gui.TEXT : Gui.tierColor(tier), false);
            if (!sum && mouseY >= headerY - 1 && mouseY < headerY + 9 && mouseX >= right - font.width(label) && mouseX < right) {
                g.setTooltipForNextFrame(font, Component.translatable("dtp.contract.tier", tier), mouseX, mouseY);
            }
        }

        ListTag players = d.getListOrEmpty("players");
        int lines = contractLines();
        contractScroll = clamp(contractScroll, lines, CONTRACT_ROWS);
        int rowsTop = headerY + 12;
        if (lines == 0) {
            g.textWithWordWrap(font, Component.translatable("dtp.stats.contracts_empty"), LEFT, rowsTop + 4, INNER, Gui.MUTED, false);
            return;
        }
        int nameWidth = tierRight(1, tiers) - TIER_STEP + 10 - LEFT;
        for (int i = contractScroll; i < Math.min(lines, contractScroll + CONTRACT_ROWS); i++) {
            int y = rowsTop + (i - contractScroll) * CONTRACT_ROW;
            if (i >= players.size()) {
                // Abschlüsse von Spielern, die sich seit dem Update nicht eingeloggt haben.
                Component label = Component.translatable("dtp.stats.unassigned");
                g.text(font, Gui.fit(font, label, nameWidth), LEFT, y, Gui.MUTED, false);
                String sum = Money.format(d.getIntOr("unassigned", 0));
                g.text(font, sum, tierRight(tiers + 1, tiers) - font.width(sum), y, Gui.MUTED, false);
                if (mouseY >= y - 1 && mouseY < y + 9 && mouseX >= LEFT && mouseX < LEFT + nameWidth) {
                    g.setTooltipForNextFrame(font.split(Component.translatable("dtp.stats.unassigned_hint"), 200), mouseX, mouseY);
                }
                continue;
            }
            CompoundTag p = players.getCompoundOrEmpty(i);
            g.text(font, Gui.fit(font, Component.literal(p.getStringOr("name", "?")), nameWidth), LEFT, y, Gui.TEXT, false);
            ListTag perTier = p.getListOrEmpty("tiers");
            for (int tier = 1; tier <= tiers; tier++) {
                int n = perTier.getIntOr(tier - 1, 0);
                String text = Money.format(n);
                g.text(font, text, tierRight(tier, tiers) - font.width(text), y, n == 0 ? Gui.MUTED : Gui.TEXT, false);
            }
            String sum = Money.format(p.getIntOr("sum", 0));
            g.text(font, sum, tierRight(tiers + 1, tiers) - font.width(sum), y, Gui.GOLD, false);
        }
        scrollbar(g, rowsTop, CONTRACT_ROWS * CONTRACT_ROW, lines, CONTRACT_ROWS, contractScroll);
    }

    // --- Einnahmen --------------------------------------------------------------------------

    /**
     * Balkendiagramm der Einnahmen je Minecraft-Tag, ältester Tag links, heute rechts. Jeder Balken stapelt den
     * Verkaufserlös (Gold, unten) und die Contract-Belohnungen (Grün, oben). Die Achse reicht bis zum stärksten Tag,
     * aufgerundet auf einen runden Wert; die Gitterlinien geben die Größenordnung, die genaue Zahl der Tooltip. Die
     * Gruppenknöpfe des Verkäufe-Reiters machen das Panel höher — das Diagramm nutzt den Platz mit.
     */
    private void income(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        ListTag days = data().getListOrEmpty("income");
        int n = days.size();
        long sum = 0;
        long max = 0;
        for (int i = 0; i < n; i++) {
            long total = total(days.getCompoundOrEmpty(i));
            sum += total;
            max = Math.max(max, total);
        }
        Component label = Component.translatable("dtp.stats.income.total", n);
        Component total = Money.component(sum);
        g.text(font, Gui.fit(font, label, TALER_RIGHT - font.width(total) - 6 - LEFT), LEFT, INCOME_TOP, Gui.MUTED, false);
        g.text(font, total, CHART_RIGHT - font.width(total), INCOME_TOP, Gui.GOLD, false);
        if (n == 0 || max == 0) {
            g.textWithWordWrap(font, Component.translatable("dtp.stats.income.empty"), LEFT, CHART_TOP + 4, INNER, Gui.MUTED, false);
            return;
        }

        int top = CHART_TOP;
        int bottom = CHART_BOTTOM + extra();
        int h = bottom - top;
        long scale = niceCeiling(max);
        g.fill(CHART_LEFT - 1, top - 1, CHART_RIGHT + 1, bottom + 1, 0xFF101010);
        // Gitterlinien bei der Hälfte und am oberen Ende der Achse.
        for (int step = 1; step <= 2; step++) {
            int y = bottom - h * step / 2;
            g.fill(CHART_LEFT, y, CHART_RIGHT, y + 1, 0x30FFFFFF);
            String value = Gui.compact(scale * step / 2);
            g.text(font, value, CHART_LEFT - 3 - font.width(value), Math.max(top - 4, y - 4), Gui.MUTED, false);
        }
        String zero = "0";
        g.text(font, zero, CHART_LEFT - 3 - font.width(zero), bottom - 8, Gui.MUTED, false);

        int column = (CHART_RIGHT - CHART_LEFT) / n;
        int left = CHART_RIGHT - column * n;
        int hovered = mouseX >= left && mouseX < CHART_RIGHT && mouseY >= top && mouseY < bottom ? (mouseX - left) / column : -1;
        for (int i = 0; i < n; i++) {
            CompoundTag day = days.getCompoundOrEmpty(i);
            int x0 = left + i * column;
            int x1 = x0 + column;
            if (i == hovered) {
                g.fill(x0, top, x1, bottom, 0x20FFFFFF);
            }
            int barLeft = x0 + 2;
            int barRight = x1 - 2;
            long sales = day.getLongOr("sales", 0);
            long dayTotal = total(day);
            if (dayTotal <= 0) {
                g.fill(barLeft, bottom - 1, barRight, bottom, 0x40FFFFFF);
                continue;
            }
            int totalHeight = Math.max(1, (int) Math.round(h * (double) dayTotal / scale));
            int salesHeight = (int) Math.round(h * (double) sales / scale);
            // Vergangene Tage etwas gedämpft, damit der laufende Tag heraussticht.
            int alpha = i == n - 1 || i == hovered ? 0xFF000000 : 0xC0000000;
            g.fill(barLeft, bottom - totalHeight, barRight, bottom - salesHeight, (Gui.GOOD & 0x00FFFFFF) | alpha);
            g.fill(barLeft, bottom - salesHeight, barRight, bottom, (Gui.GOLD & 0x00FFFFFF) | alpha);
        }

        Component first = Component.translatable("dtp.stats.income.day", days.getCompoundOrEmpty(0).getIntOr("day", 0));
        g.text(font, first, left, bottom + 3, Gui.MUTED, false);
        Component today = Component.translatable("dtp.market.chart.today");
        g.text(font, today, CHART_RIGHT - font.width(today), bottom + 3, Gui.MUTED, false);

        if (hovered >= 0) {
            CompoundTag day = days.getCompoundOrEmpty(hovered);
            List<FormattedCharSequence> lines = new ArrayList<>();
            int number = day.getIntOr("day", 0);
            lines.add(Component.translatable(hovered == n - 1 ? "dtp.stats.income.today" : "dtp.stats.income.day", number).getVisualOrderText());
            lines.add(Money.component(total(day)).copy().withColor(Gui.TEXT).getVisualOrderText());
            lines.add(Component.translatable("dtp.stats.income.sales", Money.component(day.getLongOr("sales", 0)))
                    .withColor(Gui.GOLD).getVisualOrderText());
            lines.add(Component.translatable("dtp.stats.income.contracts", Money.component(day.getLongOr("contracts", 0)))
                    .withColor(Gui.GOOD).getVisualOrderText());
            g.setTooltipForNextFrame(lines, mouseX, mouseY);
        }
    }

    private static long total(CompoundTag day) {
        return day.getLongOr("sales", 0) + day.getLongOr("contracts", 0);
    }

    /** Kleinster Wert der Form 1, 2 oder 5 × 10ⁿ, der {@code value} erreicht — mindestens 10, damit die halbe Achse ganzzahlig bleibt. */
    static long niceCeiling(long value) {
        long magnitude = 1;
        while (true) {
            for (long m : new long[] {1, 2, 5}) {
                if (m * magnitude >= Math.max(10, value)) {
                    return m * magnitude;
                }
            }
            if (magnitude > Long.MAX_VALUE / 10) {
                return value;
            }
            magnitude *= 10;
        }
    }

    /** Scrollleiste am rechten Rand — nur, wenn mehr Zeilen da sind als sichtbar. */
    private static void scrollbar(GuiGraphicsExtractor g, int top, int height, int size, int visible, int scroll) {
        if (size <= visible) {
            return;
        }
        int x = WIDTH - LEFT - 3;
        int thumb = Math.max(6, height * visible / size);
        int thumbY = top + (height - thumb) * scroll / (size - visible);
        g.fill(x, top, x + 3, top + height, 0xFF202020);
        g.fill(x, thumbY, x + 3, thumbY + thumb, Gui.MUTED);
    }

    private enum Tab { SALES, CONTRACTS, INCOME }

    private enum SortColumn { COUNT, TALER }
}
