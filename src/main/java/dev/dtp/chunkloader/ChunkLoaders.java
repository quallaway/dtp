package dev.dtp.chunkloader;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;
import org.jspecify.annotations.Nullable;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.block.ChunkLoaderBlock;
import dev.dtp.currency.DepositChests;
import dev.dtp.data.Protocol;
import dev.dtp.registry.ModBlocks;

/**
 * Chunkloader (docs/20-territorium-und-serverweite-effekte.md#chunkloader): hält die Chunks um sich geladen,
 * solange er Restlaufzeit hat und mindestens ein Spieler online ist.
 *
 * Die Restlaufzeit liegt in {@link ChunkLoaderData}; die Chunk-Tickets sind nur daraus abgeleitet. Beim Serverstart
 * werden alle Tickets verworfen, der Takt setzt sie neu, sobald jemand online ist. Ohne Spieler ruhen Tickets und
 * Laufzeit — wie Marktuhr und Contracts ist der Chunkloader kein Werkzeug für Abwesenheit vom Spiel.
 */
public final class ChunkLoaders {
    public static final int INTERVAL = 20;

    public static final TicketController CONTROLLER = new TicketController(
            Identifier.fromNamespaceAndPath(Dtp.MODID, "chunk_loader"), ChunkLoaders::validate);

    /** Chunkloader, deren Tickets gerade gesetzt sind. Nur im Speicher; nach dem Start leer, wie die Tickets. */
    private static final Set<GlobalPos> ticketed = new HashSet<>();

    private ChunkLoaders() {
    }

    public static void registerControllers(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    /** Beim Laden der Welt: alle gespeicherten Tickets verwerfen. Der Takt setzt sie aus den Daten neu. */
    private static void validate(ServerLevel level, TicketHelper helper) {
        for (BlockPos owner : List.copyOf(helper.getBlockTickets().keySet())) {
            helper.removeAllTickets(owner);
        }
    }

    /** Beim Serverstart: Tickets sind verworfen, also ist auch nichts mehr gesetzt (Einzelspieler lädt Welten neu). */
    public static void reset() {
        ticketed.clear();
    }

    public static long maxTicks() {
        return (long) DtpConfig.CHUNKLOADER_DURATION_TICKS.get() * DtpConfig.CHUNKLOADER_MAX_CHARGES.get();
    }

    public static int depositRadius(ServerLevel level) {
        return DtpConfig.CHUNKLOADER_DEPOSIT_RADIUS.get();
    }

    // --- Laden ----------------------------------------------------------------------------------------

    public sealed interface ChargeResult permits Charged, Disabled, Full, Insufficient {
    }

    public record Charged(long remaining) implements ChargeResult {
    }

    public record Disabled() implements ChargeResult {
    }

    public record Full() implements ChargeResult {
    }

    public record Insufficient(long missing) implements ChargeResult {
    }

    /**
     * Bezahlt eine Ladung aus den Einzahlkisten in Reichweite und verlängert die Restlaufzeit. Über die Obergrenze
     * hinaus wird nicht geladen, bei fehlenden Talern nichts entnommen.
     */
    public static ChargeResult charge(ServerLevel level, BlockPos pos, @Nullable Player player) {
        if (!DtpConfig.CHUNKLOADER_ENABLED.get()) {
            return new Disabled();
        }
        GlobalPos key = GlobalPos.of(level.dimension(), pos);
        ChunkLoaderData data = ChunkLoaderData.get(level.getServer());
        long duration = DtpConfig.CHUNKLOADER_DURATION_TICKS.get();
        long remaining = data.remaining(key);
        if (remaining + duration > maxTicks()) {
            return new Full();
        }
        long price = DtpConfig.CHUNKLOADER_PRICE.get();
        if (DepositChests.pay(level, pos, price, depositRadius(level)) instanceof DepositChests.Insufficient(long missing)) {
            return new Insufficient(missing);
        }
        data.set(key, remaining + duration);
        setActiveState(level, pos, true);
        if (level.getServer().getPlayerCount() > 0) {
            addTickets(level, key);
        }
        Protocol.log(level.getServer(), player, "buy_chunkloader", "chunk_loader", 1, price);
        return new Charged(remaining + duration);
    }

    /** Der Block wurde entfernt: Tickets weg, Restlaufzeit verfällt (keine Rückerstattung). */
    public static void remove(ServerLevel level, BlockPos pos) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos);
        removeTickets(level, key);
        ChunkLoaderData.get(level.getServer()).set(key, 0);
    }

    /** Gleicht den Blockzustand mit den Daten ab, z. B. wenn die Laufzeit ablief, während der Chunk nicht geladen war. */
    public static void syncState(ServerLevel level, BlockPos pos) {
        boolean active = ChunkLoaderData.get(level.getServer()).remaining(GlobalPos.of(level.dimension(), pos)) > 0;
        setActiveState(level, pos, active);
    }

    // --- Takt -----------------------------------------------------------------------------------------

    /**
     * Alle {@link #INTERVAL} Ticks. Mit Spielern online: Tickets setzen, wo sie fehlen, Laufzeit herunterzählen,
     * Abgelaufenes und Abgebautes entfernen. Ohne Spieler (oder abgeschaltet): alle Tickets aussetzen, die
     * Laufzeit bleibt stehen.
     */
    public static void tick(MinecraftServer server, boolean online) {
        ChunkLoaderData data = ChunkLoaderData.get(server);
        if (!online || !DtpConfig.CHUNKLOADER_ENABLED.get()) {
            for (GlobalPos key : List.copyOf(ticketed)) {
                ServerLevel level = server.getLevel(key.dimension());
                if (level != null) {
                    removeTickets(level, key);
                }
            }
            ticketed.clear();
            return;
        }
        // Tickets von Einträgen, die es nicht mehr gibt (z. B. per Befehl gelöscht).
        for (GlobalPos key : List.copyOf(ticketed)) {
            if (data.remaining(key) <= 0) {
                ServerLevel level = server.getLevel(key.dimension());
                if (level != null) {
                    removeTickets(level, key);
                }
                ticketed.remove(key);
            }
        }
        for (Map.Entry<GlobalPos, Long> entry : List.copyOf(data.all().entrySet())) {
            GlobalPos key = entry.getKey();
            ServerLevel level = server.getLevel(key.dimension());
            if (level == null) {
                data.set(key, 0);
                continue;
            }
            // Abgebaut, ohne dass der Block es gemeldet hat (z. B. Befehle oder Welteditoren).
            if (level.isLoaded(key.pos()) && !level.getBlockState(key.pos()).is(ModBlocks.CHUNK_LOADER.get())) {
                remove(level, key.pos());
                continue;
            }
            long left = entry.getValue() - INTERVAL;
            if (left <= 0) {
                remove(level, key.pos());
                setActiveState(level, key.pos(), false);
                continue;
            }
            data.set(key, left);
            if (!ticketed.contains(key)) {
                addTickets(level, key);
            }
        }
    }

    /** Ob die Chunks dieses Chunkloaders gerade geladen gehalten werden. */
    public static boolean isTicketed(ServerLevel level, BlockPos pos) {
        return ticketed.contains(GlobalPos.of(level.dimension(), pos));
    }

    // --- Tickets --------------------------------------------------------------------------------------

    private static void addTickets(ServerLevel level, GlobalPos key) {
        forceAll(level, key.pos(), true, DtpConfig.CHUNKLOADER_NATURAL_SPAWNING.get());
        ticketed.add(key);
    }

    /** Entfernt beide Ticket-Arten — die Config kann sich seit dem Setzen geändert haben. */
    private static void removeTickets(ServerLevel level, GlobalPos key) {
        forceAll(level, key.pos(), false, true);
        forceAll(level, key.pos(), false, false);
        ticketed.remove(key);
    }

    private static void forceAll(ServerLevel level, BlockPos pos, boolean add, boolean naturalSpawning) {
        int r = DtpConfig.CHUNKLOADER_RADIUS_CHUNKS.get();
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        // Beim Entfernen den größtmöglichen Radius abdecken, falls er seit dem Setzen verkleinert wurde.
        int range = add ? r : 4;
        for (int dx = -range; dx <= range; dx++) {
            for (int dz = -range; dz <= range; dz++) {
                CONTROLLER.forceChunk(level, pos, cx + dx, cz + dz, add, naturalSpawning);
            }
        }
    }

    private static void setActiveState(ServerLevel level, BlockPos pos, boolean active) {
        if (!level.isLoaded(pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.CHUNK_LOADER.get()) && state.getValue(ChunkLoaderBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(ChunkLoaderBlock.ACTIVE, active), Block.UPDATE_ALL);
        }
    }
}
