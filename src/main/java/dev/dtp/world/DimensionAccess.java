package dev.dtp.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/**
 * Zugangsstaffelung für den Nether (docs/20-territorium-und-serverweite-effekte.md): harte Sperre bis zur Freischaltung
 * am Beacon. Das Portal zündet nicht; als Sicherheitsnetz scheitert auch jede Reise in den Nether, etwa durch ein
 * per Befehl gesetztes Portal.
 *
 * Das End wird nicht eigens gesperrt: Enderaugen brauchen Lohenpulver aus dem Nether, und die Festungen liegen
 * 1.280 bis 2.816 Blöcke vom Weltmittelpunkt entfernt — weit jenseits der Grenze, bis sie stark ausgebaut ist.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class DimensionAccess {
    /** Spieler in diesem Abstand zum Portal erfahren, warum es nicht zündet. */
    private static final double NOTIFY_RANGE = 8.0;

    private DimensionAccess() {
    }

    public static boolean netherLocked(MinecraftServer server) {
        return DtpConfig.NETHER_LOCK.get() && WorldProgress.get(server).level(ServerUpgrade.NETHER_ACCESS) == 0;
    }

    @SubscribeEvent
    static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !netherLocked(level.getServer())) {
            return;
        }
        event.setCanceled(true);
        AABB around = new AABB(event.getPos()).inflate(NOTIFY_RANGE);
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, around)) {
            player.sendOverlayMessage(Component.translatable("dtp.access.nether_locked"));
        }
    }

    @SubscribeEvent
    static void onTravel(EntityTravelToDimensionEvent event) {
        if (event.getDimension() != Level.NETHER || !(event.getEntity().level() instanceof ServerLevel level)
                || !netherLocked(level.getServer())) {
            return;
        }
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.translatable("dtp.access.nether_locked"));
        }
    }
}
