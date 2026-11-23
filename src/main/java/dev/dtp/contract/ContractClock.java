package dev.dtp.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import dev.dtp.Dtp;
import dev.dtp.block.MarketScaleBlock;
import dev.dtp.block.MarketScaleBlockEntity;
import dev.dtp.currency.Money;
import dev.dtp.market.Market;
import dev.dtp.market.TradeList;
import dev.dtp.registry.ModDataComponents;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.PlayerUpgrades;

/**
 * Die Vanilla-Uhr als Handelswerkzeug (Spieler-Upgrades Auftragsuhr und Fernlieferung, docs/12-contracts.md).
 *
 * <ul>
 * <li><b>Tooltip</b>: angenommene Contracts als „Name: 23 / 64, 120%, 0.6 d“ — gerechnet auf dem Client aus dem
 * synchronisierten {@link PlayerContracts}, siehe {@code client/ContractClockTooltip}.</li>
 * <li><b>Waage binden</b>: Schleichen + Rechtsklick auf eine Marktwaage kopiert deren Filter-Item auf die Uhr; der
 * Tooltip zeigt dann dessen Marktpreis. Den Preis kennt nur der Server — er schickt ihn per {@link Payload}.</li>
 * <li><b>Fernlieferung</b>: Schleichen + Rechtsklick in die Luft beliefert die Contracts aus dem Inventar.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class ContractClock {
    /** Alle so viele Ticks prüft der Server die Preise gebundener Uhren. */
    private static final int PRICE_INTERVAL = 40;
    /** Abklingzeit der Fernlieferung in Sekunden, falls die Definition keine nennt. */
    private static final double DEFAULT_COOLDOWN_SECONDS = 5.0;

    /** Serverseitig: zuletzt gesendete Preise je Spieler — gesendet wird nur bei einer Änderung. */
    private static final Map<UUID, List<Price>> LAST_SENT = new HashMap<>();

    /** Clientseitig: zuletzt empfangene Preise je Item. */
    private static volatile Map<Identifier, Price> clientPrices = Map.of();

    private ContractClock() {
    }

    public static boolean isClock(ItemStack stack) {
        return stack.is(Items.CLOCK);
    }

    public static boolean hasClockUpgrade(Player player) {
        return PlayerUpgrades.level(player, PlayerUpgrade.CONTRACT_CLOCK) > 0;
    }

    // --- Tooltip-Zeilen ------------------------------------------------------------------------

    /**
     * Eine Zeile je Contract: „Name: geliefert / Menge, Wert %, Tage bis zum nächsten Abfall d“. An der Untergrenze
     * fällt nichts mehr — dann ohne Tagesangabe.
     */
    public static Component line(Contract contract, double now, Contract.Terms terms) {
        long percent = Math.round(contract.factor(now, terms) * 100.0);
        double days = contract.daysUntilNextDrop(now, terms);
        if (days < 0) {
            return Component.translatable("dtp.contract.clock.line_floor", contract.displayName(), contract.delivered(),
                    contract.count(), percent);
        }
        return Component.translatable("dtp.contract.clock.line", contract.displayName(), contract.delivered(),
                contract.count(), percent, String.format(Locale.ROOT, "%.1f", days));
    }

    /** Preiszeile der gebundenen Uhr: „Item: 12 T, 85%“, oder ein Hinweis, wenn der Preis (noch) fehlt. */
    public static Component priceLine(ItemStack watched, Price price) {
        Component name = watched.getHoverName();
        if (price == null) {
            return Component.translatable("dtp.contract.clock.price_pending", name);
        }
        if (!price.tradable()) {
            return Component.translatable("dtp.contract.clock.price_untradable", name);
        }
        return Component.translatable("dtp.contract.clock.price", name, Money.formatPrice(price.price()),
                Math.round(price.factor() * 100.0));
    }

    /** Clientseitig: zuletzt empfangener Preis für dieses Item, oder null. */
    public static Price clientPrice(ItemStack watched) {
        return clientPrices.get(BuiltInRegistries.ITEM.getKey(watched.getItem()));
    }

    // --- Waage binden und Fernlieferung --------------------------------------------------------

    /**
     * Schleichen + Rechtsklick mit der Uhr auf eine Marktwaage bindet die Uhr. Auf beiden Seiten abgebrochen: Der
     * Client würde sonst danach noch die Uhr „benutzen“ und damit die Fernlieferung auslösen.
     */
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!player.isSecondaryUseActive() || !isClock(stack) || !hasClockUpgrade(player)
                || !(event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MarketScaleBlock)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof MarketScaleBlockEntity scale)) {
            return;
        }
        player.sendOverlayMessage(bind(stack, scale.filter()));
    }

    /** Kopiert das Filter-Item auf die Uhr; eine leere Waage löst die Bindung. */
    public static Component bind(ItemStack clock, ItemStack filter) {
        if (filter.isEmpty()) {
            clock.remove(ModDataComponents.WATCHED_ITEM.get());
            return Component.translatable("dtp.contract.clock.unbound");
        }
        clock.set(ModDataComponents.WATCHED_ITEM.get(), ItemStackTemplate.fromNonEmptyStack(filter, 1));
        return Component.translatable("dtp.contract.clock.bound", filter.getHoverName());
    }

    /** Das gebundene Item einer Uhr als einzelnes Exemplar, oder leer. */
    public static ItemStack watched(ItemStack clock) {
        ItemStackTemplate template = isClock(clock) ? clock.get(ModDataComponents.WATCHED_ITEM.get()) : null;
        return template == null ? ItemStack.EMPTY : template.create();
    }

    /** Schleichen + Rechtsklick in die Luft: Fernlieferung. Nur der Server entscheidet, mit Abklingzeit gegen Spam. */
    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        if (!player.isSecondaryUseActive() || !isClock(stack)
                || PlayerUpgrades.level(player, PlayerUpgrade.REMOTE_DELIVERY) <= 0) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level) || player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        player.getCooldowns().addCooldown(stack, cooldownTicks());
        player.sendOverlayMessage(Contracts.deliverRemote(level, player));
    }

    public static int cooldownTicks() {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.REMOTE_DELIVERY);
        double seconds = def == null ? DEFAULT_COOLDOWN_SECONDS : def.param("cooldown", DEFAULT_COOLDOWN_SECONDS);
        return (int) Math.round(seconds * 20.0);
    }

    // --- Preise gebundener Uhren ---------------------------------------------------------------

    /** Preis eines gebundenen Items; {@code tradable == false}: nicht auf der Handelsliste. */
    public record Price(Identifier item, double price, double factor, boolean tradable) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Price> STREAM_CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, Price::item,
                ByteBufCodecs.DOUBLE, Price::price,
                ByteBufCodecs.DOUBLE, Price::factor,
                ByteBufCodecs.BOOL, Price::tradable,
                Price::new);
    }

    public record Payload(List<Price> prices) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(Dtp.MODID, "watched_prices"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> STREAM_CODEC = StreamCodec.composite(
                Price.STREAM_CODEC.apply(ByteBufCodecs.list()), Payload::prices,
                Payload::new);

        @Override
        public Type<Payload> type() {
            return TYPE;
        }

        public static void handle(Payload payload, IPayloadContext context) {
            Map<Identifier, Price> map = new HashMap<>();
            for (Price price : payload.prices()) {
                map.put(price.item(), price);
            }
            clientPrices = Map.copyOf(map);
        }
    }

    /** Aktuelle Preise aller Items, an die eine Uhr im Inventar gebunden ist. */
    public static List<Price> watchedPrices(ServerLevel level, Player player) {
        Map<Identifier, Price> prices = new HashMap<>();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            ItemStack watched = watched(stack);
            if (watched.isEmpty()) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(watched.getItem());
            if (prices.containsKey(id)) {
                continue;
            }
            Price price = TradeList.lookup(watched).isEmpty() ? null
                    : Market.fullMarketPrice(level, watched.getItem())
                            .map(p -> new Price(id, p.price(), p.factor(), true)).orElse(null);
            prices.put(id, price != null ? price : new Price(id, 0.0, 0.0, false));
        }
        List<Price> list = new ArrayList<>(prices.values());
        list.sort((a, b) -> a.item().compareTo(b.item()));
        return list;
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % PRICE_INTERVAL != 0
                || !hasClockUpgrade(player)) {
            return;
        }
        List<Price> prices = watchedPrices(player.level(), player);
        if (!prices.equals(LAST_SENT.get(player.getUUID()))) {
            LAST_SENT.put(player.getUUID(), prices);
            PacketDistributor.sendToPlayer(player, new Payload(prices));
        }
    }

    @SubscribeEvent
    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_SENT.remove(event.getEntity().getUUID());
    }
}
