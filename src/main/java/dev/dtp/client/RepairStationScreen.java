package dev.dtp.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.currency.Money;
import dev.dtp.menu.RepairStationMenu;
import dev.dtp.repair.RepairStation;

/**
 * Reparaturbank: im Panel Kurs und Preis der vollen Reparatur (bei Contraband: des vollen Aufladens) für das eingelegte
 * Item, darunter was das eingelegte Geld bewirkt oder warum es nicht geht. Aufbau und Zeichnen kommen aus {@link TwoSlotScreen}.
 */
public class RepairStationScreen extends TwoSlotScreen<RepairStationMenu> {
    public RepairStationScreen(RepairStationMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected String keyPrefix() {
        return "dtp.repair_station";
    }

    @Override
    protected Component buttonLabel() {
        return Component.translatable("dtp.repair_station.repair");
    }

    @Override
    protected List<Line> lines() {
        CompoundTag d = data();
        List<Line> lines = new ArrayList<>();
        if (d.isEmpty()) {
            return lines;
        }
        // Contraband wird aufgeladen statt repariert: gleiche Werte, eigene Texte (Ladungen statt Haltbarkeit).
        boolean charge = d.getBooleanOr("contraband", false);
        d.getLong("price_per_point").ifPresent(milli -> lines.add(new Line(Component.translatable(
                charge ? "dtp.repair_station.rate_charge" : "dtp.repair_station.rate", RepairStation.formatPerPoint(milli)), Gui.TEXT)));
        d.getLong("full_price").filter(price -> price > 0).ifPresent(price -> lines.add(new Line(Component.translatable(
                charge ? "dtp.repair_station.full_charge" : "dtp.repair_station.full_price", Money.component(price)), Gui.TEXT)));
        if (d.contains("blocked")) {
            d.getString("blocked").ifPresent(key -> lines.add(new Line(Component.translatable(key), Gui.MUTED)));
            return lines;
        }
        long cost = d.getLongOr("cost", 0L);
        long change = d.getLongOr("change", 0L);
        String preview = charge ? "dtp.repair_station.preview.charged_" : "dtp.repair_station.preview.";
        Component result = d.getBooleanOr("full", false)
                ? Component.translatable(preview + "full", Money.component(cost))
                : Component.translatable(preview + "partial", Money.format(d.getIntOr("points", 0)), Money.component(cost));
        lines.add(new Line(result, Gui.GOOD));
        if (change > 0) {
            lines.add(new Line(Component.translatable("dtp.repair_station.preview.change", Money.component(change)), Gui.MUTED));
        }
        return lines;
    }
}
