package dev.dtp.currency;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import dev.dtp.DtpConfig;
import dev.dtp.data.DtpStorage;
import dev.dtp.data.Nearby;

/**
 * Bezahlen aus Einzahlkisten in Reichweite (docs/10-markt.md, docs/70-technischer-rahmen.md).
 *
 * Alle Kisten im Radius bilden einen gemeinsamen Vorrat. Entnommen wird in fester Reihenfolge:
 * nächste zuerst, bei gleichem Abstand nach Koordinaten. Gesucht wird nur beim Öffnen und beim Kauf.
 */
public final class DepositChests {
    private DepositChests() {
    }

    /** Alle Einzahlkisten in Reichweite, nächste zuerst, und ihr gemeinsamer Bestand. */
    public record Pool(List<DtpStorage.Found> chests, long balance) {
    }

    /** Wert aller Taler in einem Lager. */
    public static long balance(Container container) {
        long sum = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            sum += Taler.valueOf(container.getItem(slot));
        }
        return sum;
    }

    public sealed interface PayResult permits Paid, Insufficient {
    }

    public record Paid(long amount, long change) implements PayResult {
    }

    public record Insufficient(long missing) implements PayResult {
    }

    /** Sucht alle Einzahlkisten im Standardradius ({@code deposit.radius}) um {@code center}. */
    public static Pool find(ServerLevel level, BlockPos center) {
        return find(level, center, DtpConfig.DEPOSIT_RADIUS.get());
    }

    /** Sucht alle Einzahlkisten im Radius um {@code center}. Kisten in nicht geladenen Chunks zählen nicht. */
    public static Pool find(ServerLevel level, BlockPos center, int radius) {
        List<DtpStorage.Found> found = Nearby.find(level, center, radius, DtpStorage.DEPOSIT);
        long balance = 0;
        for (DtpStorage.Found chest : found) {
            balance += balance(chest.container());
        }
        return new Pool(found, balance);
    }

    /**
     * Bezahlt {@code amount} aus den Kisten um {@code center}. Der Bestand wird serverseitig neu ermittelt,
     * nie der angezeigten Summe vertraut. Bei Teildeckung wird nichts entnommen.
     *
     * Je Kiste werden die kleinsten Stufen zuerst entnommen, damit große Münzen nur angebrochen werden,
     * wenn es nötig ist. Das Wechselgeld geht in die erste Kiste, aus der entnommen wurde.
     */
    public static PayResult pay(ServerLevel level, BlockPos center, long amount) {
        return pay(level, center, amount, DtpConfig.DEPOSIT_RADIUS.get());
    }

    /** Wie {@link #pay(ServerLevel, BlockPos, long)}, aber mit eigenem Suchradius (Beacon). */
    public static PayResult pay(ServerLevel level, BlockPos center, long amount, int radius) {
        Pool pool = find(level, center, radius);
        if (pool.balance() < amount) {
            return new Insufficient(amount - pool.balance());
        }
        long remaining = amount;
        DtpStorage.Found changeTarget = null;
        outer:
        for (DtpStorage.Found found : pool.chests()) {
            Container chest = found.container();
            for (Taler denomination : Taler.values()) {
                for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                    if (remaining <= 0) {
                        break outer;
                    }
                    ItemStack stack = chest.getItem(slot);
                    if (Taler.of(stack) != denomination) {
                        continue;
                    }
                    long needed = (remaining + denomination.value - 1) / denomination.value;
                    int take = (int) Math.min(stack.getCount(), needed);
                    stack.shrink(take);
                    remaining -= take * denomination.value;
                    if (changeTarget == null) {
                        changeTarget = found;
                    }
                    chest.setChanged();
                }
            }
        }
        long change = -remaining;
        if (change > 0) {
            List<ItemStack> coins = Taler.split(change);
            for (DtpStorage.Found chest : prepend(changeTarget, pool.chests())) {
                List<ItemStack> rest = new ArrayList<>();
                for (ItemStack coin : coins) {
                    ItemStack r = Payout.insert(chest.container(), coin);
                    if (!r.isEmpty()) {
                        rest.add(r);
                    }
                }
                coins = rest;
            }
            for (ItemStack coin : coins) {
                Block.popResource(level, changeTarget.pos().above(), coin);
            }
        }
        return new Paid(amount, change);
    }

    /**
     * Zahlt einen Erlös in die Einzahlkisten um {@code center} ein, nächste Kiste zuerst.
     * Was keinen Platz findet — oder wenn keine Kiste in Reichweite steht —, geht ins Inventar des Spielers
     * bzw. fällt ohne Spieler am Block heraus.
     *
     * @return der Betrag, der in Einzahlkisten gelandet ist
     */
    public static long deposit(ServerLevel level, BlockPos center, long amount, Player player) {
        List<ItemStack> coins = Taler.split(amount);
        for (DtpStorage.Found chest : find(level, center).chests()) {
            List<ItemStack> rest = new ArrayList<>();
            for (ItemStack coin : coins) {
                ItemStack r = Payout.insert(chest.container(), coin);
                if (!r.isEmpty()) {
                    rest.add(r);
                }
            }
            coins = rest;
        }
        long overflow = 0;
        for (ItemStack coin : coins) {
            overflow += Taler.valueOf(coin);
            if (player != null) {
                Payout.toPlayer(player, coin);
            } else {
                Block.popResource(level, center.above(), coin);
            }
        }
        return amount - overflow;
    }

    private static List<DtpStorage.Found> prepend(DtpStorage.Found first, List<DtpStorage.Found> all) {
        List<DtpStorage.Found> result = new ArrayList<>();
        result.add(first);
        for (DtpStorage.Found c : all) {
            if (c != first) {
                result.add(c);
            }
        }
        return result;
    }
}
