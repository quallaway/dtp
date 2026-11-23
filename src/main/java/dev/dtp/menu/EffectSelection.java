package dev.dtp.menu;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.UpgradeCosts;
import dev.dtp.upgrade.WorldProgress;

/** Datenstand für die Effektauswahl — gleich für Hauptbeacon und Neben-Beacon. Umschalten: Knopf {@code 200 + ordinal}. */
public final class EffectSelection {
    public static final int TOGGLE = 200;

    private EffectSelection() {
    }

    public static void put(CompoundTag tag, WorldProgress progress, List<ServerUpgrade> active, int slots) {
        tag.putInt("effect_slots", slots);
        ListTag activeList = new ListTag();
        ListTag all = new ListTag();
        for (ServerUpgrade u : ServerUpgrade.values()) {
            if (!u.isEffect()) {
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putInt("ordinal", u.ordinal());
            e.putString("id", u.id);
            if (u.isVanillaEffect()) {
                // Für das Effektsymbol auf dem Client: der Registry-Name des Vanilla-Effekts.
                e.putString("effect", BuiltInRegistries.MOB_EFFECT.getKey(u.effect.value()).toString());
            } else {
                // Eigene Effekte: Symbol aus dem Datapack (sonst das Item aus dem Enum), Name mit Beschreibung ({@code .desc}) im Tooltip.
                UpgradeCosts.Definition def = UpgradeCosts.get(u);
                if (def != null) {
                    def.icon().write(e);
                } else {
                    e.putString("icon", BuiltInRegistries.ITEM.getKey(u.icon).toString());
                }
                e.putString("name", u.translationKey());
            }
            e.putBoolean("unlocked", progress.level(u) > 0);
            e.putBoolean("active", active.contains(u));
            all.add(e);
            if (active.contains(u)) {
                activeList.add(e.copy());
            }
        }
        tag.put("effects", all);
        tag.put("active_effects", activeList);
    }

    public static ServerUpgrade byButton(int buttonId) {
        ServerUpgrade[] all = ServerUpgrade.values();
        int index = buttonId - TOGGLE;
        return index >= 0 && index < all.length && all[index].isEffect() ? all[index] : null;
    }
}
