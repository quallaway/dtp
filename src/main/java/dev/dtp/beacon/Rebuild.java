package dev.dtp.beacon;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.upgrade.ServerUpgrade;

/**
 * Rückbau, Teil des Beacon-Effekts Baumeister (docs/21-beacon-effekte.md): Einen gerade selbst gesetzten Block baut man sofort wieder ab und bekommt
 * zurück, was man gesetzt hat. Kein Statuseffekt — entschieden wird je Block.
 *
 * <p>Der Server merkt sich je Spieler die zuletzt gesetzten Blöcke (nur im Speicher). Weil der Client das Abbautempo
 * vorrechnet, bekommt er jede Markierung als kleines Paket und führt dieselbe Liste. Beim Abbau bricht der Server das
 * normale Abbauen ab und entfernt den Block selbst: So kostet das Werkzeug keine Haltbarkeit, und es gibt genau die
 * beim Setzen berechneten Items zurück — die Drops wie mit Behutsamkeit, ohne Glück.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class Rebuild {
    /** Höchstens so viele gemerkte Blöcke je Spieler; der älteste fällt heraus. */
    static final int MAX_ENTRIES = 64;
    /** Tempo, das sicher für Sofortabbau reicht (Fortschritt ≥ 1 im ersten Tick, auch ohne passendes Werkzeug). */
    private static final float INSTANT = 1.0e6f;

    record Entry(BlockPos pos, BlockState state, List<ItemStack> refund, long expires) {
    }

    record ClientMark(BlockPos pos, int stateId, long expires) {
    }

    private static final Map<UUID, Deque<Entry>> SERVER = new HashMap<>();
    /** Clientseitig: Markierungen des eigenen Spielers. */
    private static final Deque<ClientMark> CLIENT = new ArrayDeque<>();

    private Rebuild() {
    }

    public record MarkPayload(BlockPos pos, int stateId, int ticks) implements CustomPacketPayload {
        public static final Type<MarkPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dtp.MODID, "rebuild_mark"));
        public static final StreamCodec<ByteBuf, MarkPayload> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, MarkPayload::pos,
                ByteBufCodecs.VAR_INT, MarkPayload::stateId,
                ByteBufCodecs.VAR_INT, MarkPayload::ticks,
                MarkPayload::new);

        @Override
        public Type<MarkPayload> type() {
            return TYPE;
        }

        public static void handle(MarkPayload payload, IPayloadContext context) {
            long now = context.player().level().getGameTime();
            CLIENT.removeIf(m -> m.pos().equals(payload.pos()) || m.expires() < now);
            CLIENT.addLast(new ClientMark(payload.pos(), payload.stateId(), now + payload.ticks()));
            while (CLIENT.size() > MAX_ENTRIES) {
                CLIENT.removeFirst();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent multi) {
            for (BlockSnapshot snapshot : multi.getReplacedBlockSnapshots()) {
                record(player, snapshot.getPos());
            }
        } else {
            record(player, event.getPos());
        }
    }

    /**
     * Merkt sich einen gerade gesetzten Block, falls dort Rückbau wirkt. Blöcke mit BlockEntity bleiben außen vor — ihr
     * Inhalt könnte sich seit dem Setzen geändert haben. Blöcke ohne Drops (etwa die obere Türhälfte) ebenso.
     *
     * @return ob der Block gemerkt wurde
     */
    public static boolean record(ServerPlayer player, BlockPos pos) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.hasBlockEntity() || !BeaconLogic.effectAt(level, pos, ServerUpgrade.EFFECT_BUILDER)) {
            return false;
        }
        List<ItemStack> refund = refund(level, pos, state, player);
        if (refund.isEmpty()) {
            return false;
        }
        int ticks = DtpConfig.REBUILD_SECONDS.get() * 20;
        BlockPos key = pos.immutable();
        Deque<Entry> entries = SERVER.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        long now = level.getGameTime();
        entries.removeIf(e -> e.pos().equals(key) || e.expires() < now);
        entries.addLast(new Entry(key, state, refund, now + ticks));
        while (entries.size() > MAX_ENTRIES) {
            entries.removeFirst();
        }
        if (player.connection != null) { // GameTest-Spieler haben keine Verbindung
            PacketDistributor.sendToPlayer(player, new MarkPayload(key, Block.getId(state), ticks));
        }
        return true;
    }

    /** Was beim Rückbau zurückkommt: die Drops wie mit einer Netherite-Spitzhacke mit Behutsamkeit — Glas und Blätter zählen mit. */
    static List<ItemStack> refund(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.SILK_TOUCH)
                .ifPresent(silk -> tool.enchant(silk, 1));
        return Block.getDrops(state, level, pos, null, player, tool).stream().filter(s -> !s.isEmpty()).toList();
    }

    /** Ist der Block an {@code pos} vom Spieler gerade gesetzt und unverändert? Auf beiden Seiten. */
    public static boolean marked(Player player, BlockPos pos, BlockState state) {
        long now = player.level().getGameTime();
        if (player.level().isClientSide()) {
            int id = Block.getId(state);
            return CLIENT.stream().anyMatch(m -> m.pos().equals(pos) && m.stateId() == id && m.expires() >= now);
        }
        Entry entry = entry(player, pos);
        return entry != null && entry.state() == state && entry.expires() >= now;
    }

    private static Entry entry(Player player, BlockPos pos) {
        Deque<Entry> entries = SERVER.get(player.getUUID());
        if (entries == null) {
            return null;
        }
        for (Entry e : entries) {
            if (e.pos().equals(pos)) {
                return e;
            }
        }
        return null;
    }

    @SubscribeEvent
    static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.getPosition().ifPresent(pos -> {
            if (marked(event.getEntity(), pos, event.getState())) {
                event.setNewSpeed(INSTANT);
            }
        });
    }

    /**
     * Nur auf dem Server: Statt des normalen Abbaus entfernt der Mod den Block selbst und gibt die gemerkten Items
     * zurück. Das Werkzeug wird dabei nicht benutzt. Der Client erfährt über das normale Block-Update, dass dort jetzt Luft ist.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    static void onBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!marked(player, pos, event.getState())) {
            return;
        }
        Entry entry = entry(player, pos);
        SERVER.get(player.getUUID()).remove(entry);
        // Keine eigene Benachrichtigung nötig: destroyBlock meldet die Luft ohnehin an alle, die den Chunk sehen.
        event.setCanceled(true);
        take(level, pos, player, entry.refund());
    }

    private static void take(ServerLevel level, BlockPos pos, ServerPlayer player, List<ItemStack> refund) {
        level.destroyBlock(pos, false, player);
        for (ItemStack stack : refund) {
            ItemStack copy = stack.copy();
            if (!player.getInventory().add(copy) && !copy.isEmpty()) {
                Block.popResource(level, pos, copy);
            }
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SERVER.remove(event.getEntity().getUUID());
    }

    /** Für GameTests: alle Markierungen eines Spielers vergessen. */
    public static void forget(Player player) {
        SERVER.remove(player.getUUID());
    }
}
