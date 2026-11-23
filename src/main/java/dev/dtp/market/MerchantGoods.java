package dev.dtp.market;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;

import dev.dtp.Dtp;
import dev.dtp.blackmarket.BlackmarketTrader;
import dev.dtp.registry.ModDataComponents;

/**
 * Händlerware: Alles, was ein Vanilla-Händler herausgibt, trägt die Komponente {@code dtp:merchant_goods}. Sie trennt
 * den DTP-Markt vom Dorfbewohner-Markt: Händlerware zählt für keinen Contract, ist unverkäuflich und geht nicht in den
 * Recycler. Smaragde und alles in {@code #dtp:merchant_goods_exempt} bleiben unmarkiert.
 * <p>
 * Markiert werden die Ergebnisse der Angebote selbst ({@link MerchantOffer#getResult()} ist der lebende Stapel,
 * {@code assemble()} kopiert ihn samt Komponenten), und zwar beim Öffnen des Handelsfensters — vor dem Senden an den
 * Client, so zeigt schon das Fenster die Markierung — und nach jedem Handel für Angebote, die ein Stufenaufstieg neu
 * erzeugt hat. Der Schwarzmarkt gehört zur DTP-Wirtschaft und bleibt unmarkiert. Geschenke an den Helden des Dorfes
 * markiert {@link MerchantGoodsModifier}.
 * <p>
 * Kombinieren wäscht nicht: Amboss, Reparatur im Crafting-Grid ({@link dev.dtp.recipe.MerchantRepairItemRecipe}) und
 * Schleifstein geben die Markierung weiter, sobald eine der Zutaten Händlerware ist. Vanilla kopiert sonst nur die
 * Komponenten des linken bzw. oberen Items — ein Villager-Buch auf einer selbst gebauten Angel wäre sauber.
 */
@EventBusSubscriber(modid = Dtp.MODID)
public final class MerchantGoods {
    public static final TagKey<Item> EXEMPT = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(Dtp.MODID, "merchant_goods_exempt"));

    /** Umkreis, in dem der Händler des geöffneten Fensters gesucht wird. */
    private static final double TRADER_SEARCH = 16;

    private MerchantGoods() {
    }

    public static boolean isMarked(ItemStack stack) {
        return stack.has(ModDataComponents.MERCHANT_GOODS.get());
    }

    /** Markiert den Stapel an Ort und Stelle, außer er ist leer oder ausgenommen. */
    public static void mark(ItemStack stack) {
        if (!stack.isEmpty() && !stack.is(EXEMPT) && !isMarked(stack)) {
            stack.set(ModDataComponents.MERCHANT_GOODS.get(), Unit.INSTANCE);
        }
    }

    /** Markiert die Ergebnisse aller Angebote. Mehrfacher Aufruf ändert nichts. */
    public static void markOffers(MerchantOffers offers) {
        for (MerchantOffer offer : offers) {
            mark(offer.getResult());
        }
    }

    @SubscribeEvent
    static void onOpen(PlayerContainerEvent.Open event) {
        if (!(event.getContainer() instanceof MerchantMenu menu) || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        // Das Feld MerchantMenu.trader ist privat; der Händler hat den Spieler aber schon als Handelspartner gesetzt.
        boolean blackmarket = !player.level().getEntitiesOfClass(BlackmarketTrader.class,
                player.getBoundingBox().inflate(TRADER_SEARCH), trader -> trader.getTradingPlayer() == player).isEmpty();
        if (!blackmarket) {
            markOffers(menu.getOffers());
        }
    }

    @SubscribeEvent
    static void onTrade(TradeWithVillagerEvent event) {
        AbstractVillager trader = event.getAbstractVillager();
        if (!(trader instanceof BlackmarketTrader) && !trader.level().isClientSide()) {
            markOffers(trader.getOffers());
        }
    }

    /**
     * Amboss: Ist links oder rechts Händlerware, ist es auch das Ergebnis. Läuft zuletzt, nach {@code AnvilCap}, das
     * das Ergebnis neu setzen kann; abgebrochene Events (Scam- und Maut-Fluch) kommen hier nicht an.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (isMarked(event.getLeft()) || isMarked(event.getRight())) {
            mark(event.getOutput());
        }
    }

    /**
     * Schleifstein mit zwei Items: Vanilla kopiert das obere, die Markierung des unteren ginge verloren. Liegt
     * Händlerware nur unten, gibt es kein Ergebnis; oben übernimmt Vanilla die Markierung selbst.
     */
    @SubscribeEvent
    static void onGrindstone(GrindstoneEvent.OnPlaceItem event) {
        ItemStack top = event.getTopItem();
        ItemStack bottom = event.getBottomItem();
        if (!top.isEmpty() && isMarked(bottom) && !isMarked(top)) {
            event.setCanceled(true);
        }
    }
}
