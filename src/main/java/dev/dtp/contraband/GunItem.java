package dev.dtp.contraband;

import java.util.Map;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import dev.dtp.Dtp;

/**
 * Gun: ein Hitscan-Schuss in Blickrichtung bis {@code range}. Er endet am ersten Block, getroffen wird das erste Wesen
 * davor. Der Schaden kommt aus dem Schadenstyp {@code dtp:gunshot}, nicht aus dem Nahkampf — Verzauberungen und
 * Werkstatt-Boni wirken nicht. Gegen Spieler nur mit PvP (das prüft Vanilla). Auch ein Fehlschuss kostet die Ladung.
 */
public class GunItem extends ContrabandItem {
    public static final ContrabandDefinition DEFAULTS = new ContrabandDefinition(8, 1, 0.5, 250, false,
            Map.of("damage", 28.0, "range", 64.0));

    public static final ResourceKey<DamageType> GUNSHOT = ResourceKey.create(Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath(Dtp.MODID, "gunshot"));

    public GunItem(Properties properties) {
        super(properties, DEFAULTS);
    }

    @Override
    protected UseOutcome activate(ServerLevel level, Player player, ItemStack stack, ContrabandDefinition def) {
        double range = Math.clamp(def.param("range", 48.0), 1.0, 128.0);
        float damage = (float) Math.clamp(def.param("damage", 14.0), 0.0, 1000.0);
        shoot(level, player, player.getEyePosition(), player.getViewVector(1.0F), range, damage);
        return UseOutcome.USED;
    }

    /** Ein Schuss von {@code from} in Richtung {@code direction}; liefert das getroffene Wesen oder {@code null}. */
    public static Entity shoot(ServerLevel level, Player shooter, Vec3 from, Vec3 direction, double range, float damage) {
        Vec3 to = from.add(direction.normalize().scale(range));
        Vec3 end = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter)).getLocation();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(shooter, from, end,
                shooter.getBoundingBox().expandTowards(end.subtract(from)).inflate(1.0),
                e -> e != shooter && !e.isSpectator() && e.isPickable(), from.distanceToSqr(end));
        Vec3 stop = hit != null ? hit.getLocation() : end;
        trail(level, from, stop);
        if (hit == null) {
            return null;
        }
        Entity target = hit.getEntity();
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(GUNSHOT), shooter);
        target.hurtServer(level, source, damage);
        return target;
    }

    private static void trail(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        int points = (int) Math.min(96, step.length() * 2.0);
        for (int i = 1; i <= points; i++) {
            Vec3 p = from.add(step.scale(i / (double) points));
            level.sendParticles(ParticleTypes.CRIT, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected SoundEvent useSound() {
        return SoundEvents.FIREWORK_ROCKET_BLAST;
    }
}
