package dev.dtp.repair;

import java.text.NumberFormat;
import java.util.Locale;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import dev.dtp.DtpConfig;
import dev.dtp.blackmarket.ScamCurse;
import dev.dtp.contraband.Charges;
import dev.dtp.contraband.ContrabandItem;
import dev.dtp.currency.Money;
import dev.dtp.currency.Payout;
import dev.dtp.currency.Taler;
import dev.dtp.data.Protocol;
import dev.dtp.market.Market;
import dev.dtp.registry.ModDataComponents;

/**
 * Die Reparaturbank: Ein beschädigtes Werkzeug oder Rüstungsteil und Taler gehen hinein, die Bank stellt so viel
 * Haltbarkeit wieder her, wie das Geld bezahlt. Etwas teurer als der Amboss mit Material, dafür ohne Erfahrung und
 * ohne steigende Amboss-Strafe — eine Reparatur für die Zeit vor Mending, kein Ersatz dafür.
 *
 * <p><b>Kurs:</b> Taler je Haltbarkeitspunkt = {@code basePrice × (maxHaltbarkeit / 250) ^ exponent}. Die volle
 * Reparatur wächst damit mit {@code maxHaltbarkeit ^ (1 + exponent)}: Eisen bleibt günstig, Diamant macht einen
 * deutlichen Sprung (Standard: Eisenspitzhacke 500, Diamantspitzhacke ≈ 7.800).
 *
 * <p>Wie am Schwarzmarkt liegt das Geld sichtbar im Slot und kommt nicht aus den Einzahlkisten. Die Bank nimmt Münzen
 * einzeln, bis das Item voll oder der Slot leer ist; zu viel Gezahltes geht als Wechselgeld ins Inventar zurück.
 * Gerechnet wird in Milli-Talern, damit kein Gleitkommafehler einen Taler zu viel verlangt.
 *
 * <p><b>Contraband:</b> Die Bank lädt auch Contraband-Items auf. Statt Haltbarkeitspunkten zählen dann die fehlenden
 * Ladungen, der Preis je Ladung kommt aus der Datapack-Definition ({@code charge_price}). Rechnung, Münzen und
 * Wechselgeld sind dieselben.
 */
public final class RepairStation {
    public static final int SLOT_ITEM = 0;
    public static final int SLOT_PAYMENT = 1;
    /** Maximale Haltbarkeit, bei der ein Punkt genau {@code basePrice} kostet: die Eisenspitzhacke. */
    public static final int REFERENCE_DURABILITY = 250;
    private static final long MILLI = 1000L;

    private RepairStation() {
    }

    /** Warum die Reparatur gerade nicht geht. */
    public enum Blocked {
        DISABLED, NO_ITEM, NOT_REPAIRABLE, LOCKED, NOT_DAMAGED, FULLY_CHARGED, NO_PAYMENT, COIN_TOO_SMALL;

        public String translationKey() {
            return "dtp.repair_station.blocked." + name().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * Ergebnis einer Rechnung: {@code points} Haltbarkeit (bei Contraband: Ladungen) für {@code cost} Taler, dafür {@code coins} Münzen aus dem
     * Slot, {@code change} Taler zurück. {@code full}, wenn das Item danach unbeschädigt ist.
     */
    public record Quote(int points, long cost, int coins, long change, boolean full) {
        public static final Quote NONE = new Quote(0, 0, 0, 0, false);
    }

    /** Angenommen wird alles mit Haltbarkeit und Contraband, das hier Ladungen statt Haltbarkeit bekommt. */
    public static boolean accepts(ItemStack stack) {
        return isContraband(stack) || stack.isDamageableItem() && !stack.is(ContrabandItem.TAG);
    }

    /** Ein Contraband-Item: wird aufgeladen statt repariert. */
    public static boolean isContraband(ItemStack stack) {
        return stack.getItem() instanceof ContrabandItem;
    }

    /** Was fehlt: Schaden in Haltbarkeitspunkten, bei Contraband fehlende Ladungen. Verändert den Stapel nicht. */
    public static int missing(ItemStack stack) {
        if (stack.getItem() instanceof ContrabandItem item) {
            int max = item.definition().maxCharges();
            return max - Math.min(ContrabandItem.charges(stack).charges(), max);
        }
        return stack.getDamageValue();
    }

    /** Preis einer Einheit in Milli-Talern: ein Haltbarkeitspunkt, bei Contraband eine Ladung. */
    public static long unitPriceMilli(ItemStack stack) {
        if (stack.getItem() instanceof ContrabandItem item) {
            return item.definition().chargePrice() * MILLI;
        }
        return pricePerPointMilli(stack);
    }

    /** Bezahlt wird mit jeder Taler-Stückelung. */
    public static boolean isPayment(ItemStack stack) {
        return Taler.isTaler(stack);
    }

    /** Preis eines Haltbarkeitspunkts in Milli-Talern für dieses Item, mit den Werten aus der Config. */
    public static long pricePerPointMilli(ItemStack stack) {
        return pricePerPointMilli(stack.getMaxDamage(), DtpConfig.REPAIR_BASE_PRICE.get(), DtpConfig.REPAIR_EXPONENT.get());
    }

    /** Preis eines Haltbarkeitspunkts in Milli-Talern, einmal gerundet und nie unter 1. */
    public static long pricePerPointMilli(int maxDamage, double basePrice, double exponent) {
        double perPoint = basePrice * Math.pow(Math.max(1, maxDamage) / (double) REFERENCE_DURABILITY, exponent);
        return Math.max(1L, Math.round(perPoint * MILLI));
    }

    /** Was die Reparatur des ganzen Schadens (bei Contraband: das volle Aufladen) kostet, in Talern. */
    public static long fullPrice(ItemStack stack) {
        return Math.ceilDiv(missing(stack) * unitPriceMilli(stack), MILLI);
    }

    /** Die Rechnung für diese Slots; {@link Quote#NONE}, wenn nichts zu tun oder kein Geld da ist. */
    public static Quote quote(ItemStack item, ItemStack payment) {
        Taler coin = Taler.of(payment);
        if (!accepts(item) || coin == null) {
            return Quote.NONE;
        }
        return quote(missing(item), unitPriceMilli(item), coin.value, payment.getCount());
    }

    /**
     * Die eigentliche Rechnung, ohne Items. Münzen werden einzeln genommen, bis der Schaden bezahlt ist; reicht der
     * Slot nicht, gibt es so viele ganze Punkte, wie das ganze Geld bezahlt.
     */
    public static Quote quote(int damage, long pricePerPointMilli, long coinValue, int coinCount) {
        if (damage <= 0 || coinCount <= 0 || coinValue <= 0) {
            return Quote.NONE;
        }
        long fullCost = Math.ceilDiv(damage * pricePerPointMilli, MILLI);
        int coins = (int) Math.min(coinCount, Math.ceilDiv(fullCost, coinValue));
        long paid = coins * coinValue;
        if (paid >= fullCost) {
            return new Quote(damage, fullCost, coins, paid - fullCost, true);
        }
        int points = (int) (paid * MILLI / pricePerPointMilli);
        if (points <= 0) {
            return Quote.NONE;
        }
        long cost = Math.ceilDiv(points * pricePerPointMilli, MILLI);
        return new Quote(points, cost, coins, paid - cost, false);
    }

    /** Sperrgrund für diese Slots, {@code null} wenn die Reparatur geht. */
    public static Blocked check(ItemStack item, ItemStack payment) {
        if (!DtpConfig.REPAIR_ENABLED.get()) {
            return Blocked.DISABLED;
        }
        if (item.isEmpty()) {
            return Blocked.NO_ITEM;
        }
        if (!accepts(item)) {
            return Blocked.NOT_REPAIRABLE;
        }
        if (ScamCurse.isLocked(item)) {
            return Blocked.LOCKED;
        }
        if (missing(item) <= 0) {
            return isContraband(item) ? Blocked.FULLY_CHARGED : Blocked.NOT_DAMAGED;
        }
        if (!isPayment(payment)) {
            return Blocked.NO_PAYMENT;
        }
        if (quote(item, payment).points() <= 0) {
            return Blocked.COIN_TOO_SMALL;
        }
        return null;
    }

    /**
     * Eine Reparatur am Block: prüfen, rechnen, Münzen nehmen, Haltbarkeit (bei Contraband: Ladungen) setzen,
     * Wechselgeld auszahlen. Liefert die
     * Meldung für die Oberfläche; bei einem Sperrgrund passiert nichts. Ohne Server wird nur nicht protokolliert.
     */
    public static Component repair(MinecraftServer server, Player player, Container slots) {
        ItemStack item = slots.getItem(SLOT_ITEM);
        ItemStack payment = slots.getItem(SLOT_PAYMENT);
        Blocked blocked = check(item, payment);
        if (blocked != null) {
            return Component.translatable(blocked.translationKey());
        }
        boolean contraband = item.getItem() instanceof ContrabandItem;
        if (item.getItem() instanceof ContrabandItem ci) {
            // Höchstwert und Preis an die aktuelle Definition angleichen, bevor gerechnet wird.
            ContrabandItem.refresh(item, ci.definition());
        }
        Quote quote = quote(item, payment);
        if (quote.points() <= 0) {
            return Component.translatable(Blocked.COIN_TOO_SMALL.translationKey());
        }
        payment.shrink(quote.coins());
        if (contraband) {
            Charges charges = ContrabandItem.charges(item);
            item.set(ModDataComponents.CHARGES.get(), charges.withCharges(charges.charges() + quote.points()));
        } else {
            item.setDamageValue(item.getDamageValue() - quote.points());
        }
        if (quote.change() > 0) {
            Payout.toPlayer(player, quote.change());
        }
        if (server != null) {
            Protocol.log(server, player, contraband ? "recharge" : "repair", Market.itemId(item.getItem()).toString(),
                    quote.points(), quote.cost());
        }
        player.level().playSound(null, player.blockPosition(),
                contraband ? SoundEvents.RESPAWN_ANCHOR_CHARGE : SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        slots.setChanged();
        String done = contraband ? "dtp.repair_station.done.charged_" : "dtp.repair_station.done.";
        return quote.full()
                ? Component.translatable(done + "full", Money.component(quote.cost()))
                : Component.translatable(done + "partial", Money.format(quote.points()), Money.component(quote.cost()));
    }

    /** Preis je Punkt zur Anzeige mit höchstens zwei Nachkommastellen, etwa „2“, „2,48“ oder „0,94“. */
    public static String formatPerPoint(long pricePerPointMilli) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.GERMANY);
        f.setMaximumFractionDigits(2);
        return f.format(pricePerPointMilli / (double) MILLI);
    }
}
