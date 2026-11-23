package dev.dtp.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.entity.player.Player;

import dev.dtp.data.Versioned;
import dev.dtp.registry.ModAttachments;

/**
 * Abklingzeiten der persönlichen Module (Barriere, Skills) am Spieler. Alle Skills teilen sich eine Abklingzeit —
 * es gibt nur einen Skill-Platz, und ein Wechsel an der Werkstatt soll sie nicht zurücksetzen. Gespeichert wird je Modul der Spielzeit-Tick,
 * ab dem es wieder bereit ist — die Spielzeit ist auf Server und Client gleich und läuft in allen Dimensionen
 * gemeinsam, also kann der Client die Restzeit selbst anzeigen. Überlebt Tod und Relog, sonst ließe sich eine
 * Abklingzeit durch Neueinloggen umgehen.
 */
public class ModuleCooldowns {
    private static final Codec<ModuleCooldowns> INNER = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Codec.LONG).optionalFieldOf("ready_at", Map.of()).forGetter(d -> d.readyAt)
    ).apply(i, ModuleCooldowns::new));

    public static final Codec<ModuleCooldowns> CODEC = Versioned.codec(1, INNER, List.of());

    private final Map<String, Long> readyAt;

    public ModuleCooldowns() {
        this(Map.of());
    }

    private ModuleCooldowns(Map<String, Long> readyAt) {
        this.readyAt = new HashMap<>(readyAt);
    }

    public static ModuleCooldowns of(Player player) {
        return player.getData(ModAttachments.MODULE_COOLDOWNS);
    }

    /** Speicherschlüssel: gemeinsamer für alle Skills, sonst die Modul-ID. */
    private static String key(PlayerUpgrade module) {
        return module.isSkill() ? "skill" : module.id;
    }

    /** Verbleibende Ticks bis das Modul wieder bereit ist; 0 = bereit. */
    public static long remaining(Player player, PlayerUpgrade module) {
        long now = player.level().getGameTime();
        return Math.max(0, of(player).readyAt.getOrDefault(key(module), 0L) - now);
    }

    public static boolean ready(Player player, PlayerUpgrade module) {
        return remaining(player, module) == 0;
    }

    /** Startet die Abklingzeit und schickt den Stand an den Client (für die Anzeige). */
    public static void start(Player player, PlayerUpgrade module, double seconds) {
        long now = player.level().getGameTime();
        ModuleCooldowns cooldowns = of(player);
        cooldowns.readyAt.values().removeIf(t -> t <= now);
        cooldowns.readyAt.put(key(module), now + Math.round(seconds * 20));
        ModAttachments.sync(player, ModAttachments.MODULE_COOLDOWNS);
    }
}
