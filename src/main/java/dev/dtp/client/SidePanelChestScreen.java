package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;

import dev.dtp.menu.MenuSync;

/**
 * Kistenoberfläche mit einem Panel rechts daneben; Kiste und Panel werden gemeinsam zentriert.
 *
 * Der Kistenhintergrund wird hier selbst an {@code leftPos} gezeichnet. Vanillas ContainerScreen zentriert
 * ihn unabhängig von {@code leftPos} — bei verschobener Kiste lägen Slots und Hintergrund sonst versetzt.
 */
public abstract class SidePanelChestScreen<M extends ChestMenu & MenuSync.Holder> extends AbstractContainerScreen<M> implements SidePanelScreen {
    private static final Identifier CONTAINER_BACKGROUND = Identifier.withDefaultNamespace("textures/gui/container/generic_54.png");
    private static final int MARGIN = 4;

    private final int panelWidth;
    private String seenLayout;

    protected SidePanelChestScreen(M menu, Inventory inventory, Component title, int panelWidth) {
        super(menu, inventory, title, 176, 114 + menu.getRowCount() * 18);
        this.panelWidth = panelWidth;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        leftPos = Math.max(MARGIN, (width - imageWidth - MARGIN - panelWidth) / 2);
    }

    /** Höhe des Panels; darf vom aktuellen Datenstand abhängen. */
    protected abstract int panelHeight();

    /** Linke obere Ecke des Panels, relativ zu {@code leftPos}/{@code topPos}. */
    protected int panelX() {
        return imageWidth + MARGIN;
    }

    /** Vertikal an der Kiste zentriert, aber immer vollständig im Bild. */
    protected int panelY() {
        int top = topPos + (imageHeight - panelHeight()) / 2;
        top = Math.min(top, height - panelHeight() - MARGIN);
        top = Math.max(top, MARGIN);
        return top - topPos;
    }

    @Override
    public Rect2i mainArea() {
        return new Rect2i(leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public Rect2i sidePanelArea() {
        return new Rect2i(leftPos + panelX(), topPos + panelY(), panelWidth, panelHeight());
    }

    protected CompoundTag data() {
        return menu.sync().data();
    }

    protected Button button(Component label, int x, int y, int width, int buttonId, boolean active) {
        Button b = Button.builder(label, btn -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            }
        }).bounds(leftPos + panelX() + x, topPos + panelY() + y, width, 18).build();
        b.active = active;
        return addRenderableWidget(b);
    }

    /** Siehe {@link PanelScreen#layoutKey()}. */
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

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int rows = menu.getRowCount();
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos, 0.0F, 0.0F, imageWidth, rows * 18 + 17, 256, 256);
        g.blit(RenderPipelines.GUI_TEXTURED, CONTAINER_BACKGROUND, leftPos, topPos + rows * 18 + 17, 0.0F, 126.0F, imageWidth, 96, 256, 256);
        Gui.panel(g, leftPos + panelX(), topPos + panelY(), panelWidth, panelHeight());
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractLabels(g, mouseX, mouseY);
        g.pose().pushMatrix();
        g.pose().translate(panelX(), panelY());
        content(g, mouseX - leftPos - panelX(), mouseY - topPos - panelY());
        Component message = MenuSync.getComponent(data(), "message");
        if (message != null) {
            g.textWithWordWrap(font, message, 6, panelHeight() - 22, panelWidth - 12, Gui.GOLD, false);
        }
        g.pose().popMatrix();
    }

    /** Inhalt in Panel-Koordinaten (0,0 = linke obere Ecke des Panels). */
    protected abstract void content(GuiGraphicsExtractor g, int mouseX, int mouseY);
}
