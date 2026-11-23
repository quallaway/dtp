package dev.dtp.client;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.menu.BlackmarketMenu;

/**
 * Schwarzmarkt: im Panel Warnung, Stand und letztes Ergebnis, darunter der Knopf. Die Scam-Chance steht bewusst
 * nirgends. Aufbau und Zeichnen kommen aus {@link TwoSlotScreen}.
 */
public class BlackmarketScreen extends TwoSlotScreen<BlackmarketMenu> {
    public BlackmarketScreen(BlackmarketMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected String keyPrefix() {
        return "dtp.blackmarket";
    }

    @Override
    protected Component buttonLabel() {
        return Component.translatable("dtp.blackmarket.trade");
    }

    @Override
    protected List<Line> lines() {
        CompoundTag d = data();
        Line warning = new Line(Component.translatable("dtp.blackmarket.warning"), Gui.WARN);
        if (d.isEmpty()) {
            return List.of(warning);
        }
        boolean usedToday = d.getBooleanOr("used_today", false);
        Component status;
        if (usedToday) {
            double days = Math.max(0.1, d.getLongOr("next_day_ticks", 0L) / 24000.0);
            status = Component.translatable("dtp.blackmarket.next_day", String.format("%.1f", days));
        } else {
            status = d.getString("blocked").map(key -> (Component) Component.translatable(key))
                    .orElse(Component.translatable("dtp.blackmarket.ready"));
        }
        return List.of(warning, new Line(status, d.contains("blocked") || usedToday ? Gui.MUTED : Gui.GOOD));
    }
}
