package dev.dtp.advancement;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import dev.dtp.Dtp;
import dev.dtp.registry.ModTriggers;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgrades;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/**
 * Löst die DTP-Advancements aus ({@code data/dtp/advancement/progression/}).
 *
 * <p>Serverweiter Fortschritt (Beacon, Server-Upgrades) gehört der ganzen Gruppe: Er wird allen ausgelöst, die online
 * sind, und beim Login nachgeholt — wer später dazukommt oder offline war, bekommt die Advancements trotzdem.
 * Persönliche Upgrades werden beim Login ebenfalls nachgeholt, damit Spielstände von vor den Advancements sie haben.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class DtpAdvancements {
    private DtpAdvancements() {
    }

    public static void sold(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModTriggers.SOLD.get().trigger(serverPlayer);
        }
    }

    public static void contractCompleted(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModTriggers.CONTRACT_COMPLETED.get().trigger(serverPlayer);
        }
    }

    public static void playerUpgrade(Player player, PlayerUpgrade upgrade, int level) {
        if (player instanceof ServerPlayer serverPlayer) {
            ModTriggers.PLAYER_UPGRADE.get().trigger(serverPlayer, upgrade.id, level);
        }
    }

    public static void serverUpgrade(MinecraftServer server, ServerUpgrade upgrade, int level) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModTriggers.SERVER_UPGRADE.get().trigger(player, upgrade.id, level);
        }
    }

    public static void beaconStarted(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModTriggers.BEACON_STARTED.get().trigger(player);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        WorldProgress progress = WorldProgress.get(player.level().getServer());
        if (progress.started()) {
            ModTriggers.BEACON_STARTED.get().trigger(player);
        }
        for (ServerUpgrade upgrade : ServerUpgrade.values()) {
            int level = progress.level(upgrade);
            if (level > 0) {
                ModTriggers.SERVER_UPGRADE.get().trigger(player, upgrade.id, level);
            }
        }
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        for (PlayerUpgrade upgrade : PlayerUpgrade.values()) {
            int level = mine.level(upgrade);
            if (level > 0) {
                ModTriggers.PLAYER_UPGRADE.get().trigger(player, upgrade.id, level);
            }
        }
    }
}
