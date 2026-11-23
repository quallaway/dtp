package dev.dtp.currency;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

import net.minecraft.network.chat.Component;

/** Beträge werden immer in Kupfertalern ausgewiesen — ein Silbertaler ist 729, nicht 9. */
public final class Money {
    private Money() {
    }

    public static String format(long amount) {
        return NumberFormat.getIntegerInstance(Locale.GERMANY).format(amount);
    }

    /**
     * Stückpreis zur Anzeige. Ab einem Taler ganzzahlig gerundet; darunter mit einer Nachkommastelle — mit zwei,
     * wenn eine „0,0“ ergäbe. Unter einem Taler wird abgeschnitten statt gerundet: Auch 0,96 ist kein ganzer Taler
     * und lässt sich einzeln nicht verkaufen.
     */
    public static String formatPrice(double price) {
        if (price >= 1.0 || price <= 0.0) {
            return format(Math.round(price));
        }
        int digits = price >= 0.1 ? 1 : 2;
        NumberFormat f = NumberFormat.getNumberInstance(Locale.GERMANY);
        f.setRoundingMode(RoundingMode.DOWN);
        f.setMinimumFractionDigits(digits);
        f.setMaximumFractionDigits(digits);
        return f.format(price);
    }

    public static Component component(long amount) {
        return Component.translatable("dtp.money", format(amount));
    }
}
