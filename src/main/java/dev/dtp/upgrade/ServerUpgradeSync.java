package dev.dtp.upgrade;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.dtp.Dtp;

/**
 * Die Stufen der Server-Upgrades auch auf dem Client — für Wirkungen, die der Client selbst mitrechnet (etwa
 * {@link AnvilCap}). Gesendet beim Einloggen und nach jeder Änderung; der Server bleibt die einzige Wahrheit.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class ServerUpgradeSync {
    /** Clientseitig: zuletzt empfangener Stand. Wird beim nächsten Login vollständig ersetzt. */
    private static volatile Map<String, Integer> clientLevels = Map.of();

    private ServerUpgradeSync() {
    }

    public record Payload(Map<String, Integer> levels) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dtp.MODID, "server_upgrades"));
        public static final StreamCodec<ByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT), Payload::levels,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }

        public static void handle(Payload payload, IPayloadContext context) {
            clientLevels = Map.copyOf(payload.levels());
        }
    }

    /** Stufe auf der Seite des Spielers: Server aus {@link WorldProgress}, Client aus dem letzten Sync. */
    public static int level(Player player, ServerUpgrade upgrade) {
        if (player.level().isClientSide()) {
            return clientLevels.getOrDefault(upgrade.id, 0);
        }
        MinecraftServer server = player.level().getServer();
        return server == null ? 0 : WorldProgress.get(server).level(upgrade);
    }

    /** Nach einer Änderung an den Server-Upgrades: an alle Spieler. */
    public static void broadcast(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(payload(server));
    }

    private static Payload payload(MinecraftServer server) {
        WorldProgress progress = WorldProgress.get(server);
        Map<String, Integer> levels = new HashMap<>();
        for (ServerUpgrade upgrade : ServerUpgrade.values()) {
            levels.put(upgrade.id, progress.level(upgrade));
        }
        return new Payload(levels);
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, payload(player.level().getServer()));
        }
    }
}
