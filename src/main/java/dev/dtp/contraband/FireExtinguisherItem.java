package dev.dtp.contraband;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Feuerlöscher: löscht Feuer im Umkreis ({@code params.radius}) und brennende Wesen darin, den Spieler eingeschlossen.
 * Ohne Feuer in Reichweite kostet er nichts. Pilot-Item des Contraband-Item-Gerüsts.
 */
public class FireExtinguisherItem extends ContrabandItem {
    public static final ContrabandDefinition DEFAULTS = new ContrabandDefinition(16, 1, 1.0, 250, false, Map.of("radius", 8.0));

    public FireExtinguisherItem(Properties properties) {
        super(properties, DEFAULTS);
    }

    @Override
    protected UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def) {
        int radius = (int) Math.clamp(def.param("radius", 6.0), 1.0, 32.0);
        int put = extinguish(level, player.blockPosition(), radius);
        if (player.isOnFire()) {
            player.clearFire();
            put++;
        }
        return put > 0 ? UseOutcome.USED : UseOutcome.refused("dtp.contraband.fire_extinguisher.nothing");
    }

    /** Löscht Feuerblöcke und brennende Wesen im Würfel um {@code center}; liefert, wie viel gelöscht wurde. */
    public static int extinguish(ServerLevel level, BlockPos center, int radius) {
        int put = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (level.getBlockState(pos).is(BlockTags.FIRE)) {
                level.removeBlock(pos, false);
                level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5, 3, 0.25, 0.1, 0.25, 0.01);
                put++;
            }
        }
        for (Entity entity : level.getEntities((Entity) null, new AABB(center).inflate(radius), Entity::isOnFire)) {
            entity.clearFire();
            put++;
        }
        return put;
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.FIRE_EXTINGUISH;
    }
}
