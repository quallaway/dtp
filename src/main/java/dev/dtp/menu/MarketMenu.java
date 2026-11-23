package dev.dtp.menu;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.dtp.DtpConfig;
import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Money;
import dev.dtp.market.Market;
import dev.dtp.market.MarketCategories;
import dev.dtp.market.MarketData;
import dev.dtp.market.MarketEvents;
import dev.dtp.market.Sales;
import dev.dtp.market.TradeList;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/**
 * Marktstand: Verkauf aus der Hand und aus den Trade-Chests in Reichweite, dazu die Preisübersicht.
 * Knopf-IDs: 0–2 aus der Hand, 3 alle Lagerwaren, 100+i eine Lagerware, 999 Preisverlauf schließen,
 * 1000+i Preisverlauf der i-ten Ware der zuletzt gesendeten Preisliste (nur mit „Marktinformation").
 *
 * Der Preisverlauf geht nur für die gewählte Ware an den Client, nicht für die ganze Liste.
 *
 * Die Preisliste geht vollständig an den Client: Suche und Gruppenfilter laufen dort, weil nur der Client
 * die übersetzten Itemnamen kennt.
 */
public class MarketMenu extends BlockBoundMenu {
    public static final int SELL_ONE = 0;
    public static final int SELL_STACK = 1;
    public static final int SELL_ALL = 2;
    public static final int SELL_STOCK_ALL = 3;
    public static final int SELL_STOCK = 100;
    /** Sichtbare Lagerzeilen; mehr Ware wird im Client gescrollt. */
    public static final int STOCK_ROWS = 5;
    public static final int CHART_CLOSE = 999;
    public static final int CHART = 1000;
    /** Höchstens so viele Lagerzeilen gehen an den Client — die Knopf-IDs dürfen nicht in CHART_CLOSE laufen. */
    public static final int STOCK_MAX = CHART_CLOSE - SELL_STOCK;

    private Component message;
    /** Reihenfolge der zuletzt gesendeten Preisliste — der Client wählt per Position. */
    private List<Item> shownPrices = List.of();
    /** Reihenfolge des zuletzt gesendeten Lagers — ein Klick trifft immer die angezeigte Ware. */
    private List<Item> shownStock = List.of();
    private Item chartItem;

    public MarketMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.MARKET.get(), id, inventory, pos, ModBlocks.MARKET_STAND.get());
    }

    public static MarketMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new MarketMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (buttonId == CHART_CLOSE) {
            chartItem = null;
        } else if (buttonId >= CHART) {
            int index = buttonId - CHART;
            if (index >= shownPrices.size() || WorldProgress.get(level.getServer()).level(ServerUpgrade.MARKET_INFO) == 0) {
                return false;
            }
            chartItem = shownPrices.get(index);
        } else if (buttonId >= SELL_STOCK && buttonId < SELL_STOCK + STOCK_MAX) {
            int index = buttonId - SELL_STOCK;
            if (index < shownStock.size()) {
                Item item = shownStock.get(index);
                report(Sales.sellFromChests(level, pos, player, item), TradeList.displayStack(item).getHoverName());
            }
        } else {
            switch (buttonId) {
                case SELL_ONE -> sellFromHand(level, player, Sales.HandMode.ONE);
                case SELL_STACK -> sellFromHand(level, player, Sales.HandMode.STACK);
                case SELL_ALL -> sellFromHand(level, player, Sales.HandMode.ALL);
                case SELL_STOCK_ALL -> report(Sales.sellFromChests(level, pos, player, null),
                        Component.translatable("dtp.market.stock_goods"));
                default -> {
                    return false;
                }
            }
        }
        sync.markChanged();
        return true;
    }

    private void sellFromHand(ServerLevel level, Player player, Sales.HandMode mode) {
        Component name = player.getMainHandItem().getHoverName();
        report(Sales.sellFromHand(level, pos, player, mode), name);
    }

    private void report(Sales.Result result, Component what) {
        if (result.belowMinimum()) {
            message = Component.translatable("dtp.market.below_minimum_sold");
        } else if (result.count() == 0) {
            message = Component.translatable("dtp.market.nothing_sold");
        } else if (result.deposited() < result.revenue()) {
            message = Component.translatable("dtp.market.sold_to_inventory", result.count(), what, Money.component(result.revenue()));
        } else {
            message = Component.translatable("dtp.market.sold", result.count(), what, Money.component(result.revenue()));
        }
    }

    /** Schickt den Anzeige-Stapel mit, wenn die Ware Komponenten trägt (Böses Banner) — sonst reicht die Item-ID. */
    private static void putDisplay(CompoundTag e, Item item, ServerLevel level) {
        ItemStack display = TradeList.displayStack(item);
        if (!display.isComponentsPatchEmpty()) {
            MenuSync.putStack(e, "stack", display, level.registryAccess());
        }
    }

    /**
     * Preisverlauf einer Ware: je Markttag (als Abstand zu heute) Spanne und letzter Wert als rohe Preisfaktoren,
     * dazu Stückpreis an diesem Stand, Ruhepreis, Preisband, Halbwertsmenge und Erholungszeit.
     */
    private static CompoundTag chart(ServerLevel level, BlockPos pos, Item item) {
        MarketData data = MarketData.get(level.getServer());
        int today = (int) Math.floor(data.day());
        CompoundTag chart = new CompoundTag();
        chart.putString("item", Market.itemId(item).toString());
        putDisplay(chart, item, level);
        chart.putInt("days", MarketData.HISTORY_DAYS);
        double now = Market.fullMarketFactor(level, item).orElse(1.0);
        chart.putDouble("factor", now);
        Market.quote(level, pos, item, 1).ifPresent(q -> chart.putDouble("price", q.unitPrice()));
        double rest = Market.restFactor();
        chart.putDouble("rest", rest);
        chart.putDouble("rest_price", TradeList.lookup(item).map(TradeList.Entry::base).orElse(0.0) * rest);
        chart.putDouble("min", DtpConfig.FACTOR_MIN.get());
        chart.putDouble("max", DtpConfig.FACTOR_MAX.get());
        chart.putDouble("half_amount", Market.halfAmount(level, item));
        chart.putDouble("days_to_rest", Market.daysToRest(level, item));
        double low = now;
        double high = now;
        ListTag points = new ListTag();
        for (MarketData.HistoryPoint p : data.history(Market.marketKey(item))) {
            CompoundTag e = new CompoundTag();
            e.putInt("ago", today - p.day());
            e.putDouble("low", p.low());
            e.putDouble("high", p.high());
            e.putDouble("close", p.close());
            points.add(e);
            low = Math.min(low, p.low());
            high = Math.max(high, p.high());
        }
        chart.put("points", points);
        chart.putDouble("low", low);
        chart.putDouble("high", high);
        return chart;
    }

    @Override
    protected CompoundTag snapshot(ServerLevel level) {
        CompoundTag tag = new CompoundTag();
        boolean info = WorldProgress.get(level.getServer()).level(ServerUpgrade.MARKET_INFO) > 0;
        tag.putBoolean("outside", !Market.inTerritory(level, pos));
        tag.putBoolean("info", info);
        // Angekündigtes Marktereignis: Ziel und Restzeit, damit man Ware dafür zurückhalten kann.
        MarketData market = MarketData.get(level.getServer());
        if (DtpConfig.EVENTS_ENABLED.get() && market.eventScheduled() && WorldProgress.get(level.getServer()).started()) {
            tag.putLong("event_minutes", MarketEvents.minutesLeft(market));
            MenuSync.putComponent(tag, "event_target", MarketEvents.targetName(market.eventTarget()));
            tag.putString("event_type", market.eventType());
            // Laufende Ereignisse und Aufschläge aus der Welt — für den Tooltip der ersten Zeile.
            List<Component> running = MarketEvents.runningLines(level.getServer());
            tag.putInt("running_count", running.size());
            for (int i = 0; i < running.size(); i++) {
                MenuSync.putComponent(tag, "running_" + i, running.get(i));
            }
        }
        // Druckanzeige: Position im Preisband (0 = Untergrenze, 1 = Obergrenze) und wo der Ruhepreis liegt — wie im Preisverlauf.
        tag.putDouble("neutral", Market.bandPosition(Market.restFactor()));

        DepositChests.Pool pool = DepositChests.find(level, pos);
        tag.putInt("chests", pool.chests().size());

        ItemStack hand = player.getMainHandItem();
        if (!hand.isEmpty()) {
            tag.putString("hand_item", Market.itemId(hand.getItem()).toString());
            Optional<Market.Quote> unit = Market.quote(level, pos, hand.getItem(), 1);
            if (TradeList.lookup(hand).isPresent() && unit.isPresent()) {
                tag.putBoolean("hand_tradable", true);
                if (!hand.isComponentsPatchEmpty()) {
                    MenuSync.putStack(tag, "hand_display", hand.copyWithCount(1), level.registryAccess());
                }
                tag.putDouble("hand_unit", unit.get().unitPrice());
                tag.putLong("hand_one", unit.get().total());
                tag.putDouble("hand_band", Market.bandPosition(unit.get().factor()));
                // Mit „Marktinformation“ zeigt die Hand-Zeile den Prozentwert statt der Druckanzeige.
                if (info) {
                    tag.putDouble("hand_factor", unit.get().factor());
                    tag.putBoolean("hand_rising", unit.get().rising());
                    tag.putBoolean("hand_falling", unit.get().falling());
                }
                tag.putInt("hand_count", hand.getCount());
                tag.putLong("hand_stack", Market.quote(level, pos, hand.getItem(), hand.getCount()).map(Market.Quote::total).orElse(0L));
                int all = Sales.countInInventory(player, hand.getItem());
                tag.putInt("hand_all_count", all);
                tag.putLong("hand_all", Market.quote(level, pos, hand.getItem(), all).map(Market.Quote::total).orElse(0L));
            }
        }

        // Lager: Ware aus den Trade-Chests in Reichweite.
        tag.putInt("trade_chests", Sales.tradeChests(level, pos).size());
        List<Sales.StockLine> stock = Sales.stock(level, pos);
        ListTag stockTag = new ListTag();
        long stockTotal = 0;
        List<Item> shownStockItems = new ArrayList<>();
        for (int i = 0; i < stock.size(); i++) {
            Sales.StockLine line = stock.get(i);
            stockTotal += line.batchTotal();
            if (i < STOCK_MAX) {
                shownStockItems.add(line.item());
                CompoundTag e = new CompoundTag();
                e.putString("item", Market.itemId(line.item()).toString());
                putDisplay(e, line.item(), level);
                e.putInt("count", line.count());
                e.putLong("total", line.total());
                Market.quote(level, pos, line.item(), 1).ifPresent(q -> e.putDouble("band", Market.bandPosition(q.factor())));
                stockTag.add(e);
            }
        }
        tag.put("stock", stockTag);
        shownStock = shownStockItems;
        tag.putLong("stock_total", stockTotal);

        // Nur freigeschaltete Gruppen — Nether und End erscheinen erst mit ihrer Freischaltung.
        tag.put("categories", MarketCategories.snapshot(level.getServer()));

        ListTag prices = new ListTag();
        List<Item> shown = new ArrayList<>();
        for (Item item : TradeList.allTradable()) {
            // Waren, die nur in gesperrten Gruppen stehen (Chorusfrucht vor dem Drachensieg), bleiben verborgen.
            if (!MarketCategories.visible(level.getServer(), item)) {
                continue;
            }
            Market.quote(level, pos, item, 1).ifPresent(q -> {
                CompoundTag e = new CompoundTag();
                e.putInt("index", shown.size());
                shown.add(item);
                e.putString("item", Market.itemId(item).toString());
                putDisplay(e, item, level);
                e.putDouble("price", q.unitPrice());
                e.putDouble("band", Market.bandPosition(q.factor()));
                // Preisfaktor und Trend zeigt der Marktstand erst nach dem Server-Upgrade "Marktinformation".
                if (info) {
                    e.putDouble("factor", q.factor());
                    e.putBoolean("rising", q.rising());
                    e.putBoolean("falling", q.falling());
                }
                prices.add(e);
            });
        }
        tag.put("prices", prices);
        shownPrices = shown;
        if (info && chartItem != null) {
            tag.put("chart", chart(level, pos, chartItem));
        }
        MenuSync.putComponent(tag, "message", message);
        return tag;
    }
}
