package dev.dtp.contraband;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import dev.dtp.Dtp;

/**
 * Wirkungen, die eine Weile anhalten (Partikel-Markierungen des Spawnstabs, Dorfbewohner des Taschengeld-Beutels).
 * Läuft im Intervall von {@link #INTERVAL} Ticks über {@code GameEvents}, nie pro Tick. Nichts davon wird gespeichert:
 * Nach einem Neustart ist eine laufende Wirkung vorbei, und das ist für Sekunden-Effekte in Ordnung.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class Lingering {
    public static final int INTERVAL = 10;

    /** Eine Wirkung: läuft bis {@code until} (Spielzeit) und endet früher, wenn {@code step} {@code false} liefert. */
    private record Task(ResourceKey<Level> dimension, long until, Predicate<ServerLevel> step) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Lingering() {
    }

    /** Führt {@code step} sofort und dann alle {@link #INTERVAL} Ticks für {@code ticks} Ticks aus. */
    public static void start(ServerLevel level, int ticks, Predicate<ServerLevel> step) {
        if (step.test(level) && ticks > INTERVAL) {
            TASKS.add(new Task(level.dimension(), level.getGameTime() + ticks, step));
        }
    }

    public static void tick(MinecraftServer server) {
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task task = it.next();
            ServerLevel level = server.getLevel(task.dimension);
            if (level == null || level.getGameTime() >= task.until || !task.step.test(level)) {
                it.remove();
            }
        }
    }

    /** Zahl laufender Wirkungen (für Tests). */
    public static int running() {
        return TASKS.size();
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }

    /**
     * Führt {@code step} sofort und dann alle {@link #INTERVAL} Ticks für {@code ticks} Ticks aus, solange {@code player}
     * in dieser Dimension bleibt und nicht geht. Mock-Spieler aus GameTests haben keine Verbindung und werden übergangen.
     */
    public static void forPlayer(ServerLevel level, Player player, int ticks, Consumer<ServerPlayer> step) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        start(level, ticks, l -> {
            if (serverPlayer.isRemoved() || serverPlayer.level() != l) {
                return false;
            }
            step.accept(serverPlayer);
            return true;
        });
    }

    /** Partikel an Blockpositionen, die nur {@code player} sieht, bis er die Dimension wechselt oder geht. */
    public static void markForPlayer(ServerLevel level, Player player, List<BlockPos> positions, ParticleOptions particle, int ticks) {
        if (positions.isEmpty()) {
            return;
        }
        List<BlockPos> marks = List.copyOf(positions);
        forPlayer(level, player, ticks, sp -> {
            for (BlockPos pos : marks) {
                level.sendParticles(sp, particle, true, true, pos.getX() + 0.5, pos.getY() + 0.15, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
            }
        });
    }
}
