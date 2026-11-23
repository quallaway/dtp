package dev.dtp.market;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import dev.dtp.DtpConfig;
import dev.dtp.advancement.DtpAdvancements;
import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Taler;
import dev.dtp.data.DtpStorage;
import dev.dtp.data.Nearby;
import dev.dtp.data.Protocol;

/**
 * Verkauf am Marktstand — aus der Hand oder aus den Trade-Chests in Reichweite.
 * Der Erlös geht in die Einzahlkisten in Reichweite des Marktstands; findet er dort keinen Platz, ins Inventar.
 *
 * Ein Verkauf, der weniger als {@link #MIN_REVENUE} Taler brächte, findet nicht statt — die Items bleiben, wo sie
 * sind. Sonst verschwände bei Massenware mit Stückpreis unter einem Taler ein einzelnes Stück für nichts.
 */
public final class Sales {
    private Sales() {
    }

    /** Kleinster Erlös, für den verkauft wird. */
    public static final long MIN_REVENUE = 1;

    /**
     * Eine Ware im Lager der Trade-Chests. {@code total}: exakter Erlös, wenn nur diese Zeile verkauft wird.
     * {@code batchTotal}: ihr Anteil, wenn alles auf einmal verkauft wird — bei gemeinsamer Ware nach den
     * vorigen Items bepreist, 0, wenn sie dabei unter den Mindesterlös fällt und liegen bleibt.
     */
    public record StockLine(Item item, int count, long total, long batchTotal) {
    }

    /** {@code belowMinimum}: nichts verkauft, weil der Erlös unter {@link #MIN_REVENUE} läge. */
    public record Result(int count, long revenue, long deposited, boolean belowMinimum) {
        public static final Result NONE = new Result(0, 0, 0, false);
        public static final Result BELOW_MINIMUM = new Result(0, 0, 0, true);
    }

    /** Brächte der Verkauf dieser Menge mindestens {@link #MIN_REVENUE}? */
    private static boolean worthSelling(ServerLevel level, BlockPos market, Item item, int count) {
        return Market.quote(level, market, item, count).map(q -> q.total() >= MIN_REVENUE).orElse(false);
    }

    public enum HandMode { ONE, STACK, ALL }

    /** Alle Warenlager in Reichweite, nächstes zuerst: Trade-Chests und alles, was {@link DtpStorage#TRADE} anbietet. */
    public static List<Container> tradeChests(ServerLevel level, BlockPos market) {
        List<Container> chests = new ArrayList<>();
        for (DtpStorage.Found found : Nearby.find(level, market, DtpConfig.TRADE_CHEST_RADIUS.get(), DtpStorage.TRADE)) {
            chests.add(found.container());
        }
        return chests;
    }

    /**
     * Menge passender Items in den Trade-Chests um {@code center} — für Contract-Lieferungen. Anders als beim
     * Verkauf zählen auch Exemplare mit Zusatzdaten (z. B. verzauberte Bücher), weil Contracts sie fordern dürfen.
     */
    public static int countForDelivery(ServerLevel level, BlockPos center, Predicate<ItemStack> matches) {
        int count = 0;
        for (Container chest : tradeChests(level, center)) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (!stack.isEmpty() && matches.test(stack)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    /**
     * Entnimmt bis zu {@code max} passende Exemplare aus den Trade-Chests. Das schwächste zuerst — die wenigsten
     * Verzauberungsstufen —, damit neben einer schlichten Spitzhacke nicht die mit Reparatur verschwindet.
     * Bei gleicher Stärke die nächste Kiste zuerst.
     */
    public static int takeForDelivery(ServerLevel level, BlockPos center, Predicate<ItemStack> matches, int max) {
        List<Source> sources = new ArrayList<>();
        for (Container chest : tradeChests(level, center)) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                sources.add(new Source(chest, i));
            }
        }
        return takeForDelivery(sources, matches, max);
    }

    /** Ein Slot, aus dem geliefert werden darf — in einer Trade-Chest oder im Inventar (Fernlieferung). */
    public record Source(Container container, int index) {
    }

    /**
     * Entnimmt bis zu {@code max} passende Exemplare aus den gegebenen Slots, das schwächste zuerst; bei gleicher
     * Stärke in der Reihenfolge der Slots.
     */
    public static int takeForDelivery(List<Source> sources, Predicate<ItemStack> matches, int max) {
        record Slot(Source source, int strength) {
        }
        List<Slot> slots = new ArrayList<>();
        for (Source source : sources) {
            ItemStack stack = source.container().getItem(source.index());
            if (!stack.isEmpty() && matches.test(stack)) {
                slots.add(new Slot(source, enchantmentLevels(stack)));
            }
        }
        slots.sort(Comparator.comparingInt(Slot::strength));
        int taken = 0;
        for (Slot slot : slots) {
            if (taken >= max) {
                break;
            }
            Container container = slot.source().container();
            ItemStack stack = container.getItem(slot.source().index());
            int take = Math.min(stack.getCount(), max - taken);
            stack.shrink(take);
            container.setChanged();
            taken += take;
        }
        return taken;
    }

    /** Summe aller Verzauberungsstufen, aktive und in Büchern gespeicherte. */
    private static int enchantmentLevels(ItemStack stack) {
        int sum = 0;
        for (ItemEnchantments enchantments : List.of(stack.getEnchantments(),
                stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY))) {
            for (var entry : enchantments.entrySet()) {
                sum += entry.getIntValue();
            }
        }
        return sum;
    }

    private static boolean sellable(ItemStack stack) {
        return !stack.isEmpty() && !Taler.isTaler(stack) && TradeList.lookup(stack).isPresent();
    }

    /** Verkaufbare Ware in allen Trade-Chests in Reichweite, je Item zusammengefasst, in Reihenfolge der Handelsliste. */
    public static List<StockLine> stock(ServerLevel level, BlockPos market) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Container chest : tradeChests(level, market)) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (sellable(stack)) {
                    counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
        }
        List<StockLine> lines = new ArrayList<>();
        // Jede Zeile zeigt ihren Preis für sich, wie ihr eigener Verkaufsknopf ihn auszahlt. Für „alles
        // verkaufen" werden Items einer gemeinsamen Ware dagegen nacheinander bepreist, wie sellFromChests sie
        // verkauft. Zeilen unter dem Mindesterlös bleiben dabei liegen und erhöhen den Druck nicht.
        Map<Identifier, Integer> sold = new HashMap<>();
        counts.forEach((item, count) -> {
            Identifier key = Market.marketKey(item);
            Market.quote(level, market, item, count).ifPresent(alone -> {
                long batch = Market.quote(level, market, item, count, sold.getOrDefault(key, 0))
                        .map(Market.Quote::total).orElse(0L);
                boolean sells = batch >= MIN_REVENUE;
                lines.add(new StockLine(item, count, alone.total(), sells ? batch : 0));
                if (sells) {
                    sold.merge(key, count, Integer::sum);
                }
            });
        });
        lines.sort(Comparator.comparingInt(line -> TradeList.position(line.item())));
        return lines;
    }

    /**
     * Verkauft ein Item (oder bei {@code only == null} alles) aus den Trade-Chests in Reichweite. Items, deren
     * Gesamtmenge unter dem Mindesterlös läge, bleiben in den Kisten.
     */
    public static Result sellFromChests(ServerLevel level, BlockPos market, Player player, Item only) {
        List<Container> chests = tradeChests(level, market);
        Map<Item, Integer> counts = new LinkedHashMap<>();
        for (Container chest : chests) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (sellable(stack) && (only == null || stack.is(only))) {
                    counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
                }
            }
        }
        int sold = 0;
        long revenue = 0;
        boolean skipped = false;
        for (Map.Entry<Item, Integer> e : counts.entrySet()) {
            // Erst nach dem vorigen Item prüfen: Bei gemeinsamer Ware hat es den Preis schon gedrückt.
            if (!worthSelling(level, market, e.getKey(), e.getValue())) {
                skipped = true;
                continue;
            }
            removeFromChests(chests, e.getKey());
            long r = Market.sell(level, market, e.getKey(), e.getValue());
            Protocol.log(level.getServer(), player, "sell_trade_chest", Market.itemId(e.getKey()).toString(), e.getValue(), r);
            sold += e.getValue();
            revenue += r;
        }
        if (sold == 0) {
            return skipped ? Result.BELOW_MINIMUM : Result.NONE;
        }
        return payout(level, market, player, sold, revenue);
    }

    /** Leert alle verkaufbaren Stapel dieses Items — genau die, die {@link #sellFromChests} gezählt hat. */
    private static void removeFromChests(List<Container> chests, Item item) {
        for (Container chest : chests) {
            boolean changed = false;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (sellable(stack) && stack.is(item)) {
                    chest.setItem(i, ItemStack.EMPTY);
                    changed = true;
                }
            }
            if (changed) {
                chest.setChanged();
            }
        }
    }

    /**
     * Redstone am Marktstand: verkauft genau einen Stapel — den ersten verkaufbaren aus der nächstgelegenen
     * Trade-Chest. Ohne Spieler geht der Erlös in die Einzahlkisten; was keinen Platz findet, fällt am Marktstand heraus.
     * Ein Stapel unter dem Mindesterlös wird übersprungen, sonst bliebe die Anlage an ihm hängen.
     */
    public static Result sellOneStack(ServerLevel level, BlockPos market) {
        for (Container chest : tradeChests(level, market)) {
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (sellable(stack) && worthSelling(level, market, stack.getItem(), stack.getCount())) {
                    Item item = stack.getItem();
                    int count = stack.getCount();
                    chest.setItem(i, ItemStack.EMPTY);
                    chest.setChanged();
                    long revenue = Market.sell(level, market, item, count);
                    Protocol.log(level.getServer(), null, "sell_redstone", Market.itemId(item).toString(), count, revenue);
                    return payout(level, market, null, count, revenue);
                }
            }
        }
        return Result.NONE;
    }

    public static Result sellFromHand(ServerLevel level, BlockPos market, Player player, HandMode mode) {
        ItemStack hand = player.getMainHandItem();
        if (!sellable(hand)) {
            return Result.NONE;
        }
        Item item = hand.getItem();
        int count = switch (mode) {
            case ONE -> 1;
            case STACK -> hand.getCount();
            case ALL -> countInInventory(player, item);
        };
        if (!worthSelling(level, market, item, count)) {
            return Result.BELOW_MINIMUM;
        }
        long revenue = Market.sell(level, market, item, count);
        if (mode == HandMode.ALL) {
            removeFromInventory(player, item, count);
        } else {
            hand.shrink(count);
            if (hand.isEmpty()) {
                refillHand(player, item);
            }
        }
        Protocol.log(level.getServer(), player, "sell_market", Market.itemId(item).toString(), count, revenue);
        return payout(level, market, player, count, revenue);
    }

    private static Result payout(ServerLevel level, BlockPos market, Player player, int count, long revenue) {
        long deposited = revenue > 0 ? DepositChests.deposit(level, market, revenue, player) : 0;
        DtpAdvancements.sold(player);
        return new Result(count, revenue, deposited, false);
    }

    public static int countInInventory(Player player, Item item) {
        int count = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && sellable(s)) {
                count += s.getCount();
            }
        }
        return count;
    }

    /**
     * Legt nach einem Handverkauf, der die Hand geleert hat, den nächsten Stapel desselben Items aus dem
     * Hauptinventar in die Hand — so lässt sich „Stapel“ wiederholt klicken. Andere Items rücken nie nach;
     * Rüstung und Nebenhand bleiben unberührt.
     */
    private static void refillHand(Player player, Item item) {
        Inventory inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (i != selected && s.is(item) && sellable(s)) {
                inv.setItem(selected, s);
                inv.setItem(i, ItemStack.EMPTY);
                inv.setChanged();
                return;
            }
        }
    }

    private static void removeFromInventory(Player player, Item item, int count) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && count > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item) && sellable(s)) {
                int take = Math.min(count, s.getCount());
                s.shrink(take);
                count -= take;
            }
        }
        inv.setChanged();
    }
}
