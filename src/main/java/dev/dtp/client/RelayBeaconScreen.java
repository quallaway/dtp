package dev.dtp.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.menu.EffectSelection;
import dev.dtp.menu.RelayBeaconMenu;

/** Neben-Beacon: Status und eigene Effektauswahl. Gekauft wird nur am Hauptbeacon. */
public class RelayBeaconScreen extends PanelScreen<RelayBeaconMenu> {
    private static final int PICKER_TOP = 46;

    public RelayBeaconScreen(RelayBeaconMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 280, 176);
    }

    @Override
    protected String layoutKey() {
        return "";
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        int ordinal = EffectPicker.clicked(data(), 8, PICKER_TOP, (int) event.x() - leftPos, (int) event.y() - topPos);
        if (ordinal >= 0) {
            click(EffectSelection.TOGGLE + ordinal);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void content(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        Component line = Component.translatable("dtp.relay.radius", data().getIntOr("radius", 0));
        g.text(font, Gui.fit(font, line, imageWidth - 16), 8, 18, Gui.GOOD, false);
        g.text(font, Gui.fit(font, Component.translatable("dtp.relay.hint"), imageWidth - 16), 8, 30, Gui.MUTED, false);
        EffectPicker.draw(g, font, data(), 8, PICKER_TOP, mouseX, mouseY);
    }
}
