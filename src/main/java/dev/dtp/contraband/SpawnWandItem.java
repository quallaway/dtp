package dev.dtp.contraband;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LightLayer;

/**
 * Spawnstab: zeigt für {@code seconds} die Stellen im Radius, an denen ein Monster erscheinen könnte, mit roten
 * Partikeln nur für den Benutzer. Maßstab ist der Vanilla-Platztest für Zombies bei Blocklicht 0; das Himmelslicht
 * zählt nicht, damit der Stab auch tagsüber zeigt, was nachts gefährlich wird.
 */
public class SpawnWandItem extends ContrabandItem {
    public static final ContrabandDefinition DEFAULTS = new ContrabandDefinition(16, 1, 5.0, 300, false,
            Map.of("radius", 16.0, "seconds", 10.0, "max_marks", 400.0));

    private static final DustParticleOptions MARK = new DustParticleOptions(0xE02020, 1.0F);

    /** Höhe über und unter dem Spieler, in der gesucht wird. */
    private static final int HEIGHT = 8;

    public SpawnWandItem(Properties properties) {
        super(properties, DEFAULTS);
    }

    /** Gefährliche Stellen im Radius um {@code center}, die nächsten zuerst, höchstens {@code max}. */
    public static List<BlockPos> spawnSpots(ServerLevel level, BlockPos center, int radius, int max) {
        List<BlockPos> spots = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -HEIGHT, -radius), center.offset(radius, HEIGHT, radius))) {
            int dx = pos.getX() - center.getX();
            int dz = pos.getZ() - center.getZ();
            if (dx * dx + dz * dz > radius * radius || !level.isLoaded(pos)) {
                continue;
            }
            if (level.getBrightness(LightLayer.BLOCK, pos) == 0 && SpawnPlacements.isSpawnPositionOk(EntityTypes.ZOMBIE, level, pos)) {
                spots.add(pos.immutable());
            }
        }
        spots.sort(Comparator.comparingDouble(p -> p.distSqr(center)));
        return spots.size() > max ? spots.subList(0, max) : spots;
    }

    @Override
    protected UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def) {
        int radius = (int) Math.clamp(def.param("radius", 16.0), 1.0, 48.0);
        int ticks = (int) Math.round(Math.clamp(def.param("seconds", 10.0), 1.0, 120.0) * 20.0);
        int max = (int) Math.clamp(def.param("max_marks", 400.0), 1.0, 2000.0);
        List<BlockPos> spots = spawnSpots(level, player.blockPosition(), radius, max);
        if (spots.isEmpty()) {
            return UseOutcome.refused("dtp.contraband.spawn_wand.safe");
        }
        Lingering.markForPlayer(level, player, spots, MARK, ticks);
        player.sendOverlayMessage(Component.translatable("dtp.contraband.spawn_wand.found", spots.size()).withStyle(ChatFormatting.RED));
        return UseOutcome.USED;
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }
}
