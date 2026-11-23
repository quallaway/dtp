package dev.dtp.blackmarket;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Continuation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.LevelReader;

import dev.dtp.DtpConfig;
import dev.dtp.currency.DepositChests;
import dev.dtp.data.DtpStorage;
import dev.dtp.registry.ModEntities;

/**
 * Spawnbedingung des Schwarzmarkthändlers (docs/15-blackmarket-trader.md). In jeder Prüfung wird der Spieler in der Oberwelt
 * mit der stärksten Lockwirkung gesucht. Seine Einzahlkisten im {@code attractRadius} zählen:
 * <ul>
 *   <li>Freigeschaltet ist er erst, wenn <b>eine einzelne</b> Kiste mehr als {@code minChestBalance} hält.</li>
 *   <li>Die Chance wächst dann mit allen Talern in Reichweite: {@link #chance(long, long)}.</li>
 * </ul>
 * Serverweit gibt es nur einen Händler: Der nächste darf erst nach Verweildauer und Abklingzeit kommen.
 */
public final class BlackmarketSpawner {
    private static final RandomSource RANDOM = RandomSource.create();

    private BlackmarketSpawner() {
    }

    /** Lockwirkung der Einzahlkisten um eine Position. {@code richest} ist die Kiste mit den meisten Talern. */
    public record Lure(long maxChest, @Nullable BlockPos richest, long pool, double chance) {
    }

    /**
     * Chance je Prüfung: 0, solange keine einzelne Kiste über der Schwelle liegt, sonst
     * {@code min(maxChance, baseChance × (pool / minChestBalance) ^ chanceExponent)}.
     */
    public static double chance(long maxChest, long pool) {
        long threshold = DtpConfig.TRADER_MIN_CHEST.get();
        if (maxChest <= threshold) {
            return 0.0;
        }
        double ratio = threshold <= 0 ? 1.0 : (double) pool / threshold;
        double p = DtpConfig.TRADER_BASE_CHANCE.get() * Math.pow(ratio, DtpConfig.TRADER_CHANCE_EXPONENT.get());
        return Math.min(DtpConfig.TRADER_MAX_CHANCE.get(), p);
    }

    public static Lure lure(ServerPlayer player) {
        return lure((ServerLevel) player.level(), player.blockPosition());
    }

    public static Lure lure(ServerLevel level, BlockPos center) {
        DepositChests.Pool pool = DepositChests.find(level, center, DtpConfig.TRADER_ATTRACT_RADIUS.get());
        long max = 0;
        BlockPos richest = null;
        for (DtpStorage.Found chest : pool.chests()) {
            long balance = DepositChests.balance(chest.container());
            if (balance > max) {
                max = balance;
                richest = chest.pos();
            }
        }
        return new Lure(max, richest, pool.balance(), chance(max, pool.balance()));
    }

    /** Ticks bis zum nächsten möglichen Händler, 0 wenn er jetzt kommen darf. */
    public static long cooldownLeft(MinecraftServer server) {
        return Math.max(0L, BlackmarketData.get(server).nextTraderTick() - server.overworld().getGameTime());
    }

    /** Aus dem Server-Tick, alle {@code checkIntervalSeconds}. */
    public static void tick(MinecraftServer server) {
        if (!DtpConfig.TRADER_ENABLED.get() || server.getTickCount() % (20 * DtpConfig.TRADER_CHECK_SECONDS.get()) != 0
                || cooldownLeft(server) > 0) {
            return;
        }
        ServerLevel overworld = server.overworld();
        ServerPlayer bestPlayer = null;
        Lure best = null;
        for (ServerPlayer player : overworld.players()) {
            if (player.isSpectator()) {
                continue;
            }
            Lure lure = lure(player);
            if (lure.chance() > 0 && (best == null || lure.chance() > best.chance())) {
                best = lure;
                bestPlayer = player;
            }
        }
        if (best != null && RANDOM.nextDouble() < best.chance()) {
            spawn(overworld, bestPlayer, best.richest());
        }
    }

    /**
     * Lässt einen Händler bei der Kiste {@code target} (oder beim Spieler) erscheinen und startet Verweildauer und
     * Abklingzeit. Leer, wenn kein freier Platz gefunden wurde.
     */
    public static Optional<BlackmarketTrader> spawn(ServerLevel level, ServerPlayer player, @Nullable BlockPos target) {
        BlockPos center = target != null ? target : player.blockPosition();
        BlockPos pos = findSpawnPositionNear(level, center, 12);
        if (pos == null) {
            pos = findSpawnPositionNear(level, player.blockPosition(), 8);
        }
        if (pos == null) {
            return Optional.empty();
        }
        BlackmarketTrader trader = ModEntities.BLACKMARKET_TRADER.get().spawn(level, pos, EntitySpawnReason.EVENT);
        if (trader == null) {
            return Optional.empty();
        }
        int stay = DtpConfig.TRADER_STAY_MINUTES.get() * 60 * 20;
        trader.stayFor(stay);
        trader.setWanderTarget(center);
        trader.setHomeTo(center, 16);
        BlackmarketData.get(level.getServer()).setNextTraderTick(
                level.getGameTime() + stay + DtpConfig.TRADER_COOLDOWN_MINUTES.get() * 60L * 20L);
        level.playSound(null, pos, SoundEvents.WANDERING_TRADER_REAPPEARED, SoundSource.NEUTRAL, 1.0F, 0.7F);
        player.sendSystemMessage(Component.translatable("dtp.blackmarket.trader.arrived").withStyle(ChatFormatting.DARK_PURPLE));
        return Optional.of(trader);
    }

    /** Wie beim Vanilla-Händler: zufällige Oberflächenposition im Umkreis, mit Platz für die Figur. */
    private static @Nullable BlockPos findSpawnPositionNear(LevelReader level, BlockPos center, int radius) {
        SpawnPlacementType type = SpawnPlacements.getPlacementType(EntityTypes.WANDERING_TRADER);
        for (int i = 0; i < 10; i++) {
            int x = center.getX() + RANDOM.nextInt(radius * 2) - radius;
            int z = center.getZ() + RANDOM.nextInt(radius * 2) - radius;
            int y = level.getHeight(SpawnPlacements.getHeightmapType(EntityTypes.WANDERING_TRADER), x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (type.isSpawnPositionOk(level, pos, EntityTypes.WANDERING_TRADER) && hasEnoughSpace(level, pos)) {
                return pos;
            }
        }
        return null;
    }

    private static boolean hasEnoughSpace(LevelReader level, BlockPos pos) {
        return !level.findBlocksIn(pos, pos.offset(1, 2, 1))
                .forEachUntil((p, state) -> Continuation.continueIf(state.getCollisionShape(level, p).isEmpty()));
    }
}
