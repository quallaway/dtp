package dev.dtp.upgrade;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.player.Player;

import dev.dtp.data.Versioned;
import dev.dtp.registry.ModAttachments;

/**
 * Gekaufte Spieler-Upgrades und ausgerüstete Module am Spieler. Überlebt den Tod — persönlicher Fortschritt geht
 * nie verloren. Gespeichert nach ID, damit ein aus dem Mod entferntes Upgrade beim Laden nicht stört.
 *
 * <p>Zwei Sichten: Die Instanzmethode {@link #level(PlayerUpgrade)} ist der <em>besessene</em> Stand (Kauf,
 * Voraussetzungen, Baum). Die statischen {@link #level(Player, PlayerUpgrade)} und {@link #value(Player, PlayerUpgrade)}
 * sind der <em>wirksame</em> Stand — bei Modulen 0, solange sie nicht ausgerüstet sind. Wirkungsstellen nutzen immer
 * die statischen Methoden; so gelten die Modulslots überall, ohne dass eine Wirkung sie selbst prüfen muss.
 */
public class PlayerUpgrades {
    /** Obergrenze der persönlichen Modulslots, unabhängig vom Datapack. */
    public static final int MAX_MODULE_SLOTS = 3;

    private static final Codec<PlayerUpgrades> INNER = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("levels", Map.of()).forGetter(d -> d.levels),
            Codec.STRING.listOf().optionalFieldOf("modules", List.of()).forGetter(d -> d.modules)
    ).apply(i, PlayerUpgrades::new));

    /**
     * Version 2: ausgerüstete Module. Ältere Stände haben schlicht keine — das neue Feld ist optional.
     * Version 3: Hot Delivery heißt Quick Delivery; die gekaufte Stufe zieht unter die neue ID um.
     */
    public static final Codec<PlayerUpgrades> CODEC = Versioned.codec(3, INNER, List.of(new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    }, new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            Dynamic<T> levels = data.get("levels").orElseEmptyMap();
            return levels.get("hot_delivery").result()
                    .map(level -> data.set("levels", levels.remove("hot_delivery").set("quick_delivery", level)))
                    .orElse(data);
        }
    }));

    private final Map<String, Integer> levels;
    /** Ausgerüstete Module als IDs, Reihenfolge = Slotreihenfolge. */
    private final List<String> modules;

    public PlayerUpgrades() {
        this(Map.of(), List.of());
    }

    private PlayerUpgrades(Map<String, Integer> levels, List<String> modules) {
        this.levels = new HashMap<>(levels);
        this.modules = new ArrayList<>(modules);
    }

    public static PlayerUpgrades of(Player player) {
        return player.getData(ModAttachments.PLAYER_UPGRADES);
    }

    /** Wirksame Stufe eines Upgrades beim Spieler; 0 = nicht gekauft oder Modul nicht ausgerüstet. */
    public static int level(Player player, PlayerUpgrade upgrade) {
        return of(player).effectiveLevel(upgrade);
    }

    /** Wirkungswert der wirksamen Stufe aus dem Datapack; 0, wenn nicht wirksam oder nicht konfiguriert. */
    public static double value(Player player, PlayerUpgrade upgrade) {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(upgrade);
        return def == null ? 0.0 : def.value(level(player, upgrade));
    }

    /** Besessene Stufe, unabhängig davon, ob ein Modul ausgerüstet ist. */
    public int level(PlayerUpgrade upgrade) {
        return levels.getOrDefault(upgrade.id, 0);
    }

    public void setLevel(PlayerUpgrade upgrade, int level) {
        if (level <= 0) {
            levels.remove(upgrade.id);
        } else {
            levels.put(upgrade.id, level);
        }
    }

    private int effectiveLevel(PlayerUpgrade upgrade) {
        if (upgrade.isModule() && !equippedModules().contains(upgrade)) {
            return 0;
        }
        return level(upgrade);
    }

    // --- Module ------------------------------------------------------------------------------

    public int moduleSlots() {
        return Math.min(MAX_MODULE_SLOTS, level(PlayerUpgrade.MODULE_SLOTS));
    }

    /**
     * Wirksam ausgerüstete Module: nur gekaufte, höchstens {@link #moduleSlots()} Stück — sinkt die Slotzahl, fallen
     * die letzten weg. Veraltete Einträge bleiben gespeichert, wirken aber nicht.
     */
    public List<PlayerUpgrade> equippedModules() {
        int slots = moduleSlots();
        List<PlayerUpgrade> result = new ArrayList<>();
        for (String id : modules) {
            PlayerUpgrade u = PlayerUpgrade.byId(id);
            if (u != null && u.isModule() && level(u) > 0 && result.size() < slots) {
                result.add(u);
            }
        }
        return result;
    }

    public boolean isEquipped(PlayerUpgrade module) {
        return equippedModules().contains(module);
    }

    /** Der ausgerüstete Skill oder {@code null} — es gibt höchstens einen. */
    public PlayerUpgrade equippedSkill() {
        for (PlayerUpgrade u : equippedModules()) {
            if (u.isSkill()) {
                return u;
            }
        }
        return null;
    }

    /**
     * Rüstet ein gekauftes Modul aus oder ab; {@code false}, wenn es kein gekauftes Modul ist oder kein Slot frei ist.
     * Veraltete Einträge werden beim Ausrüsten verworfen — sonst rückte nach dem Abrüsten unerwartet ein altes Modul
     * nach (wie beim Beacon). Ein Skill ersetzt einen schon ausgerüsteten Skill an dessen Platz: Es gibt nur eine
     * Fähigkeitstaste, also höchstens einen Skill.
     */
    public boolean toggleModule(PlayerUpgrade module) {
        if (!module.isModule() || level(module) == 0) {
            return false;
        }
        List<PlayerUpgrade> visible = equippedModules();
        if (visible.contains(module)) {
            modules.remove(module.id);
            return true;
        }
        PlayerUpgrade skill = equippedSkill();
        if (module.isSkill() && skill != null) {
            modules.clear();
            visible.forEach(u -> modules.add(u == skill ? module.id : u.id));
            return true;
        }
        if (visible.size() >= moduleSlots()) {
            return false;
        }
        modules.clear();
        visible.forEach(u -> modules.add(u.id));
        modules.add(module.id);
        return true;
    }
}
