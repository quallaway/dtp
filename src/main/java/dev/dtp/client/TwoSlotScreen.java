package dev.dtp.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.menu.MenuSync;
import dev.dtp.menu.TwoSlotMenu;

/**
 * Bildschirm für {@link TwoSlotMenu} im Format der Einzahlkiste: links die Vanilla-Oberfläche mit zwei beschrifteten
 * Slots und dem Inventar, rechts ein Panel mit den Zeilen des Blocks, der letzten Meldung und dem Knopf. Das Panel
 * wächst mit dem Text, damit nichts übersteht.
 *
 * <p>Sprachschlüssel unter {@link #keyPrefix()}: {@code .slot.item}, {@code .slot.payment} und je {@code .hint}.
 */
public abstract class TwoSlotScreen<M extends TwoSlotMenu> extends AbstractContainerScreen<M> implements SidePanelScreen {
    private static final Identifier CONTAINER = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    /** Ab hier wird der Inventarteil der Kistentextur gezeichnet; die Inventarslots liegen 14 px tiefer (y = 84). */
    private static final int INVENTORY_TOP = 70;
    private static final int LABEL_COLOR = 0xFF404040;
    private static final int MARGIN = 4;
    private static final int PANEL_W = 132;
    private static final int PAD = 6;
    protected static final int TEXT_W = PANEL_W - 2 * PAD;
    private static final int GAP = 6;
    private static final int BUTTON_H = 18;

    /** Eine Textzeile (oder ein umbrochener Absatz) im Panel. */
    protected record Line(Component text, int color) {
    }

    private Button action;

    protected TwoSlotScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, INVENTORY_TOP + 96);
        this.inventoryLabelY = INVENTORY_TOP + 3;
    }

    /** Präfix der Sprachschlüssel, etwa {@code dtp.blackmarket}. */
    protected abstract String keyPrefix();

    /** Beschriftung des Knopfs. */
    protected abstract Component buttonLabel();

    /** Die Zeilen des Blocks über der Meldung, von oben nach unten. */
    protected abstract List<Line> lines();

    protected CompoundTag data() {
        return menu.sync().data();
    }

    /** Der Knopf ist aktiv, sobald Daten da sind und nichts sperrt. */
    protected boolean actionReady(CompoundTag d) {
        return !d.isEmpty() && !d.contains("blocked");
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(MARGIN, (width - imageWidth - MARGIN - PANEL_W) / 2);
        action = Button.builder(buttonLabel(), b -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TwoSlotMenu.ACTION);
            }
        }).bounds(0, 0, TEXT_W, BUTTON_H).build();
        action.active = false;
        addRenderableWidget(action);
        placeButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        action.active = actionReady(data());
        placeButton();
    }

    private void placeButton() {
        action.setPosition(leftPos + panelX() + PAD, topPos + panelY() + panelHeight() - PAD - BUTTON_H);
    }

    // --- Panel ------------------------------------------------------------------------------

    /** Die Zeilen des Blocks und darunter, falls vorhanden, die Meldung der letzten Aktion in Gold. */
    private List<Line> allLines() {
        List<Line> all = new ArrayList<>(lines());
        Component message = MenuSync.getComponent(data(), "message");
        if (message != null) {
            all.add(new Line(message, Gui.GOLD));
        }
        all.removeIf(line -> line.text() == null || line.text().getString().isEmpty());
        return all;
    }

    private int textHeight(Component text) {
        return font.wordWrapHeight(text, TEXT_W);
    }

    /** Alle Zeilen untereinander, darunter der Knopf; mindestens so hoch wie der obere Teil. */
    private int panelHeight() {
        int h = PAD;
        for (Line line : allLines()) {
            h += textHeight(line.text()) + GAP;
        }
        return Math.max(INVENTORY_TOP, h + BUTTON_H + PAD);
    }

    private int panelX() {
        return imageWidth + MARGIN;
    }

    /** Oben bündig mit der Oberfläche, aber immer vollständig im Bild. */
    private int panelY() {
        int top = Math.min(topPos, height - panelHeight() - MARGIN);
        return Math.max(MARGIN, top) - topPos;
    }

    @Override
    public Rect2i mainArea() {
        return new Rect2i(leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public Rect2i sidePanelArea() {
        return new Rect2i(leftPos + panelX(), topPos + panelY(), PANEL_W, panelHeight());
    }

    // --- Zeichnen ---------------------------------------------------------------------------

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        // Oberer Teil aus der Kistentextur: Kopfzeile, dann eine leere Zeile gestreckt — ohne die Kistenslots.
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER, leftPos, topPos, 0.0F, 0.0F, imageWidth, 17, 256, 256);
        for (int y = 17; y < INVENTORY_TOP; y++) {
            g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER, leftPos, topPos + y, 0.0F, 16.0F, imageWidth, 1, 256, 256);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER, leftPos, topPos + INVENTORY_TOP, 0.0F, 126.0F, imageWidth, 96, 256, 256);
        slotFrame(g, TwoSlotMenu.ITEM_X, TwoSlotMenu.SLOT_Y);
        slotFrame(g, TwoSlotMenu.PAYMENT_X, TwoSlotMenu.SLOT_Y);
        Gui.panel(g, leftPos + panelX(), topPos + panelY(), PANEL_W, panelHeight());
    }

    /** Ein Slot-Rahmen aus der Kistentextur, um die Item-Position herum. */
    private void slotFrame(GuiGraphicsExtractor g, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER, leftPos + x - 1, topPos + y - 1, 7.0F, 17.0F, 18, 18, 256, 256);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        int labelY = TwoSlotMenu.SLOT_Y + 21;
        centered(g, Component.translatable(keyPrefix() + ".slot.item"), TwoSlotMenu.ITEM_X + 8, labelY);
        centered(g, Component.translatable(keyPrefix() + ".slot.payment"), TwoSlotMenu.PAYMENT_X + 8, labelY);
        centered(g, Component.literal("+"), (TwoSlotMenu.ITEM_X + TwoSlotMenu.PAYMENT_X) / 2 + 8, TwoSlotMenu.SLOT_Y + 4);

        int x = panelX() + PAD;
        int y = panelY() + PAD;
        for (Line line : allLines()) {
            g.textWithWordWrap(font, line.text(), x, y, TEXT_W, line.color(), false);
            y += textHeight(line.text()) + GAP;
        }
    }

    private void centered(GuiGraphicsExtractor g, Component text, int centerX, int y) {
        g.text(font, text, centerX - font.width(text) / 2, y, LABEL_COLOR, false);
    }

    /** Leere Slots erklären per Tooltip, was hineingehört. */
    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (hoveredSlot != null && !hoveredSlot.hasItem() && menu.getCarried().isEmpty()) {
            if (hoveredSlot.index == TwoSlotMenu.SLOT_ITEM) {
                g.setTooltipForNextFrame(font.split(Component.translatable(keyPrefix() + ".slot.item.hint"), 170), mouseX, mouseY);
            } else if (hoveredSlot.index == TwoSlotMenu.SLOT_PAYMENT) {
                g.setTooltipForNextFrame(font.split(Component.translatable(keyPrefix() + ".slot.payment.hint"), 170), mouseX, mouseY);
            }
        }
    }
}
