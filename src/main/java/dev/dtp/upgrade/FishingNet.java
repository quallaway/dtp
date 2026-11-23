package dev.dtp.upgrade;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Bucketable;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import dev.dtp.Dtp;

/**
 * Fischernetz (Skill): tötet die Fische im Wasser rund um den Spieler und legt ihre Beute direkt ins Inventar. Welche
 * Arten das Netz fängt, sagt der Tag {@code #dtp:fishing_net} (Kabeljau, Lachs, Tropenfisch, Kugelfisch). Fische aus
 * dem Eimer bleiben verschont — das sind Aquarienfische.
 *
 * <p>Getötet wird als Spielerangriff, damit Beute und Erfahrung wie beim Erschlagen fallen. Die Drops fängt
 * {@link #onDrops} ab, solange der Fisch hier gefangen wird; was nicht ins Inventar passt, fällt dem Spieler vor die
 * Füße. Die Erfahrungskugeln entstehen am Fisch und fliegen im kleinen Radius ohnehin zum Spieler.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class FishingNet {
    public static final TagKey<EntityType<?>> CATCH = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(Dtp.MODID, "fishing_net"));

    /** Fische, die gerade im Netz sterben. Nur während {@link #cast} belegt; der Tod läuft synchron. */
    private static final Set<UUID> NETTED = new HashSet<>();

    private FishingNet() {
    }

    /** Die fangbaren Fische im Radius: Art im Tag, lebend, im Wasser, nicht aus dem Eimer. */
    public static List<LivingEntity> targets(Player player, double radius) {
        List<LivingEntity> fish = new ArrayList<>();
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius))) {
            if (e.is(CATCH) && e.isAlive() && e.isInWater() && e.distanceToSqr(player) <= radius * radius
                    && !(e instanceof Bucketable b && b.fromBucket())) {
                fish.add(e);
            }
        }
        return fish;
    }

    /** Wirft das Netz. {@code false}, wenn kein Fisch in Reichweite ist — dann fällt auch keine Abklingzeit an. */
    static boolean cast(Player player, double radius) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        List<LivingEntity> fish = targets(player, radius);
        if (fish.isEmpty()) {
            return false;
        }
        for (LivingEntity f : fish) {
            level.sendParticles(ParticleTypes.SPLASH, f.getX(), f.getY() + f.getBbHeight() / 2, f.getZ(), 12, 0.3, 0.2, 0.3, 0.1);
            NETTED.add(f.getUUID());
            try {
                f.hurtServer(level, level.damageSources().playerAttack(player), Float.MAX_VALUE);
            } finally {
                NETTED.remove(f.getUUID());
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS, 1.0f, 0.8f);
        return true;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    static void onDrops(LivingDropsEvent event) {
        if (!NETTED.contains(event.getEntity().getUUID()) || !(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            DirectDrops.give(player, drop.getItem().copy());
        }
        event.setCanceled(true);
    }
}
