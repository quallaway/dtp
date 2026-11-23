package dev.dtp.contraband;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldgenRandom;

/**
 * Schleimrute: zeigt Slime-Chunks ohne Text, nur für den Benutzer. Zu jedem Slime-Chunk im Umkreis von
 * {@code search_chunks} fliegen Funken wie ein Enderauge, nahe Chunks kurz, ferne weit. Ist der eigene Chunk ein
 * Slime-Chunk, leuchten seine Kanten für {@code outline_seconds}. Dazu ein heller Ton für ja, ein dumpfer für nein.
 * Gerechnet wird wie in Vanilla ({@code Slime}), nur in der Oberwelt.
 */
public class OozingRodItem extends ContrabandItem {
    public static final ContrabandDefinition DEFAULTS = new ContrabandDefinition(10, 1, 3.0, 400, false,
            Map.of("search_chunks", 4.0, "outline_seconds", 6.0));

    /** Salz der Vanilla-Slime-Chunks ({@code Slime#checkSlimeSpawnRules}). */
    private static final long SLIME_SALT = 987234911L;

    private static final DustParticleOptions OUTLINE = new DustParticleOptions(0x7FD65B, 1.2F);

    /** Funken-Wellen: drei Stück im Abstand von {@link Lingering#INTERVAL} Ticks. */
    private static final int SPARK_TICKS = 3 * Lingering.INTERVAL;

    /** Entfernungen (Blöcke), zwischen denen die Flugweite der Funken wächst, und die zugehörigen Startgeschwindigkeiten. */
    private static final double NEAR = 16.0, FAR = 90.0, SLOW = 0.13, FAST = 0.43;

    public OozingRodItem(Properties properties) {
        super(properties, DEFAULTS);
    }

    public static boolean isSlimeChunk(ServerLevel level, int chunkX, int chunkZ) {
        return WorldgenRandom.seedSlimeChunk(chunkX, chunkZ, level.getSeed(), SLIME_SALT).nextInt(10) == 0;
    }

    /** Alle Slime-Chunks im Umkreis von {@code radius} Chunks um {@code here}, ohne {@code here} selbst. */
    public static List<ChunkPos> slimeChunksAround(ServerLevel level, ChunkPos here, int radius) {
        List<ChunkPos> found = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if ((dx != 0 || dz != 0) && isSlimeChunk(level, here.x() + dx, here.z() + dz)) {
                    found.add(new ChunkPos(here.x() + dx, here.z() + dz));
                }
            }
        }
        return found;
    }

    /**
     * Startgeschwindigkeit eines Funkens zu einem Ziel in {@code distance} Blöcken. Ein {@code END_ROD} fliegt etwa
     * 23-mal so weit, also rund 3 Blöcke zu nahen und 10 zu fernen Chunks.
     */
    public static double sparkSpeed(double distance) {
        double t = Math.clamp((distance - NEAR) / (FAR - NEAR), 0.0, 1.0);
        return SLOW + t * (FAST - SLOW);
    }

    @Override
    protected UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def) {
        if (level.dimension() != Level.OVERWORLD) {
            return UseOutcome.refused("dtp.contraband.oozing_rod.wrong_dimension");
        }
        ChunkPos here = ChunkPos.containing(player.blockPosition());
        boolean slime = isSlimeChunk(level, here.x(), here.z());
        int radius = (int) Math.clamp(def.param("search_chunks", 4.0), 0.0, 16.0);
        List<ChunkPos> targets = slimeChunksAround(level, here, radius);

        if (!targets.isEmpty()) {
            Lingering.forPlayer(level, player, SPARK_TICKS, sp -> sparks(level, sp, targets));
        }
        if (slime) {
            int outlineTicks = (int) (Math.clamp(def.param("outline_seconds", 6.0), 1.0, 30.0) * 20);
            Lingering.forPlayer(level, player, outlineTicks, sp -> outline(level, sp, here));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.6F);
        } else {
            if (targets.isEmpty() && player instanceof ServerPlayer sp) {
                level.sendParticles(sp, ParticleTypes.SMOKE, true, false, player.getX(), player.getEyeY() - 0.4, player.getZ(), 6, 0.15, 0.1, 0.15, 0.01);
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
        }
        return UseOutcome.USED;
    }

    /** Ein Funke je Ziel, von der Brust waagrecht zur Chunkmitte; bei Anzahl 0 ist der Versatz die Flugrichtung. */
    private static void sparks(ServerLevel level, ServerPlayer player, List<ChunkPos> targets) {
        double y = player.getEyeY() - 0.4;
        for (ChunkPos target : targets) {
            double dx = target.getMiddleBlockX() + 0.5 - player.getX();
            double dz = target.getMiddleBlockZ() + 0.5 - player.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < 1.0) {
                continue;
            }
            level.sendParticles(player, ParticleTypes.END_ROD, true, true, player.getX(), y, player.getZ(), 0,
                    dx / dist, 0.0, dz / dist, sparkSpeed(dist));
        }
    }

    /** Die vier Kanten von {@code chunk}, ein Partikel je Block, auf Fußhöhe des Spielers. */
    private static void outline(ServerLevel level, ServerPlayer player, ChunkPos chunk) {
        double y = player.getY() + 0.1;
        double x0 = chunk.getMinBlockX(), z0 = chunk.getMinBlockZ(), x1 = x0 + 16, z1 = z0 + 16;
        for (int i = 0; i < 16; i++) {
            double a = i + 0.5;
            level.sendParticles(player, OUTLINE, true, true, x0 + a, y, z0, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(player, OUTLINE, true, true, x0 + a, y, z1, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(player, OUTLINE, true, true, x0, y, z0 + a, 1, 0.0, 0.0, 0.0, 0.0);
            level.sendParticles(player, OUTLINE, true, true, x1, y, z0 + a, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected SoundEvent useSound() {
        return null;
    }
}
