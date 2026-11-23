package dev.dtp.upgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import dev.dtp.Dtp;

/**
 * Zeitweise größerer Aufsammelradius (Rausch, Baumfällen). Vanilla sammelt im Umkreis der Hitbox +1 / +0,5 / +1
 * Blöcke auf und kennt dafür kein Attribut — der Bonus berührt die Items im erweiterten Bereich daher selbst per
 * {@link ItemEntity#playerTouch}; Aufsammelverzögerung, Ziel-Spieler und Pickup-Event greifen wie gewohnt.
 * Nur auf dem Server; der Zustand ist flüchtig (Sekunden) und wird nicht gespeichert.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class PickupBoost {
    private record Boost(long until, double range) {
    }

    private static final Map<UUID, Boost> ACTIVE = new HashMap<>();

    private PickupBoost() {
    }

    /** Gewährt {@code range} Blöcke mehr Aufsammelradius für {@code ticks}; überlappende Boni nehmen jeweils das Größere. */
    public static void grant(Player player, int ticks, double range) {
        if (player.level().isClientSide() || ticks <= 0 || range <= 0) {
            return;
        }
        long until = player.level().getGameTime() + ticks;
        ACTIVE.merge(player.getUUID(), new Boost(until, range),
                (old, now) -> new Boost(Math.max(old.until(), now.until()), Math.max(old.range(), now.range())));
    }

    /** Zusätzlicher Radius gerade jetzt, 0 ohne Bonus. */
    public static double range(Player player) {
        Boost boost = ACTIVE.get(player.getUUID());
        if (boost == null) {
            return 0;
        }
        if (player.level().getGameTime() >= boost.until()) {
            ACTIVE.remove(player.getUUID());
            return 0;
        }
        return boost.range();
    }

    /** Sammelt im erweiterten Bereich auf; gibt die Zahl der berührten Items zurück. */
    public static int pickup(Player player) {
        double range = range(player);
        if (range <= 0 || player.isSpectator() || player.isDeadOrDying()) {
            return 0;
        }
        int touched = 0;
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class,
                player.getBoundingBox().inflate(1.0 + range, 0.5 + range, 1.0 + range))) {
            if (!item.isRemoved()) {
                item.playerTouch(player);
                touched++;
            }
        }
        return touched;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && !ACTIVE.isEmpty()) {
            pickup(player);
        }
    }

    @SubscribeEvent
    static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            ACTIVE.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE.remove(event.getEntity().getUUID());
    }
}
