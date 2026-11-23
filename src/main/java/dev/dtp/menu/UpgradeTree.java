package dev.dtp.menu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import dev.dtp.upgrade.UpgradeIcon;

/**
 * Gemeinsamer Datenstand der Upgrade-Bäume von Werkstatt und Beacon. Ein Feld ist ein ganzes Upgrade
 * ({@code node_level} 0) oder eine Stufe eines aufgeteilten Upgrades; jede Stufe setzt die vorige voraus.
 * Gekauft wird immer die nächste Stufe. Der Client zeichnet die Felder mit {@code client.UpgradeTreeView}.
 *
 * <p>Knopf-IDs: {@link #SELECT}{@code +ordinal} wählt ein ganzes Upgrade, {@link #SELECT_LEVEL}{@code +16·ordinal+Stufe}
 * eine Stufe.
 */
public final class UpgradeTree {
    public static final int SELECT = 100;
    public static final int SELECT_LEVEL = 1000;
    /** Höchste Stufe, die als eigenes Feld wählbar ist (Platz in der Knopf-ID). */
    public static final int LEVELS_PER_UPGRADE = 16;

    private UpgradeTree() {
    }

    /** Ein Feld. {@code cost} −1 = gekauft bzw. voll ausgebaut. */
    public static CompoundTag node(int ordinal, String id, UpgradeIcon icon, int[] pos, int nodeLevel, int level, int max, long cost) {
        CompoundTag e = new CompoundTag();
        e.putInt("ordinal", ordinal);
        e.putString("id", id);
        icon.write(e);
        e.putInt("x", pos[0]);
        e.putInt("y", pos[1]);
        e.putInt("node_level", nodeLevel);
        e.putInt("level", level);
        e.putInt("max", max);
        e.putLong("cost", cost);
        return e;
    }

    /** Voraussetzung mit Ziel-Feld ({@code ordinal}, {@code nodeLevel}) für die Linie und Erfüllt-Markierung. */
    public static CompoundTag requirement(String id, int level, int ordinal, int nodeLevel, boolean met) {
        CompoundTag req = new CompoundTag();
        req.putString("id", id);
        req.putInt("level", level);
        req.putInt("ordinal", ordinal);
        req.putInt("node_level", nodeLevel);
        req.putBoolean("met", met);
        return req;
    }

    /** Ordinal aus einer Knopf-ID oder −1. */
    public static int selectedOrdinal(int buttonId, int count) {
        if (buttonId >= SELECT && buttonId < SELECT + count) {
            return buttonId - SELECT;
        }
        if (buttonId >= SELECT_LEVEL && buttonId < SELECT_LEVEL + count * LEVELS_PER_UPGRADE) {
            return (buttonId - SELECT_LEVEL) / LEVELS_PER_UPGRADE;
        }
        return -1;
    }

    /** Gewählte Stufe aus einer Knopf-ID; 0 = ganzes Upgrade. */
    public static int selectedLevel(int buttonId) {
        return buttonId >= SELECT_LEVEL ? (buttonId - SELECT_LEVEL) % LEVELS_PER_UPGRADE : 0;
    }

    /**
     * Prüft, ob die gewählte Stufe die nächste ist; sonst die Meldung dazu. {@code null} = kaufen.
     */
    public static Component levelCheck(int selectedLevel, int current) {
        if (selectedLevel > 0 && current >= selectedLevel) {
            return Component.translatable("dtp.workshop.level_owned");
        }
        if (selectedLevel > 0 && current < selectedLevel - 1) {
            return Component.translatable("dtp.workshop.level_first", Component.translatable("enchantment.level." + (current + 1)));
        }
        return null;
    }
}
