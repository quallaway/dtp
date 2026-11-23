package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

import dev.dtp.menu.MenuSync;

/**
 * Basis für Oberflächen ohne Slots. Gezeichnet wird ausschließlich aus dem letzten Datenstand des Servers;
 * Knöpfe senden nur ihre ID.
 */
public abstract class PanelScreen<M extends AbstractContainerMenu & MenuSync.Holder> extends AbstractContainerScreen<M> {
    private String seenLayout;

    protected PanelScreen(M menu, Inventory inventory, Component title, int width, int height) {
        super(menu, inventory, title, width, height);
    }

    protected CompoundTag data() {
        return menu.sync().data();
    }

    protected void click(int buttonId) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        }
    }

    protected Button button(Component label, int x, int y, int width, int buttonId, boolean active) {
        Button b = Button.builder(label, btn -> click(buttonId)).bounds(leftPos + x, topPos + y, width, 18).build();
        b.active = active;
        return addRenderableWidget(b);
    }

    /**
     * Alles aus dem Datenstand, wovon Anzahl, Beschriftung oder Aktivität der Knöpfe abhängt.
     * Ändert es sich, werden die Knöpfe neu aufgebaut; reine Anzeigewerte zeichnen sich ohnehin jedes Bild neu.
     */
    protected abstract String layoutKey();

    @Override
    protected void containerTick() {
        super.containerTick();
        String layout = layoutKey();
        if (!layout.equals(seenLayout)) {
            seenLayout = layout;
            rebuildWidgets();
        }
    }

    /**
     * Der Neuaufbau löscht in Vanilla den Fokus ({@code clearFocus}). Ein Widget, das ihn überlebt (etwa das Suchfeld
     * am Marktstand), behält ihn, sonst bricht das Tippen ab, sobald ein Preis den Layout-Schlüssel ändert.
     */
    @Override
    protected void rebuildWidgets() {
        GuiEventListener focused = getFocused();
        super.rebuildWidgets();
        if (focused != null && children().contains(focused)) {
            setFocused(focused);
        }
    }

    /**
     * Sichtbare Höhe des Panels. Standard ist die feste Höhe aus dem Konstruktor; Oberflächen mit wechselndem
     * Inhalt überschreiben das ({@code imageHeight} ist in 26.3 unveränderlich).
     */
    protected int panelHeight() {
        return imageHeight;
    }

    @Override
    protected void init() {
        super.init();
        topPos = Math.max(4, (height - panelHeight()) / 2);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        Gui.panel(g, leftPos, topPos, imageWidth, panelHeight());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, titleLabelX, titleLabelY, Gui.TEXT, false);
        Component message = MenuSync.getComponent(data(), "message");
        if (message != null) {
            g.text(font, Gui.fit(font, message, imageWidth - 16), 8, panelHeight() - 14, Gui.GOLD, false);
        }
        content(g, mouseX - leftPos, mouseY - topPos);
    }

    /** Inhalt in lokalen Koordinaten (0,0 = linke obere Ecke des Panels). */
    protected abstract void content(GuiGraphicsExtractor g, int mouseX, int mouseY);
}
