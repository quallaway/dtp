package dev.dtp.casino;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.dtp.DtpConfig;
import dev.dtp.currency.Taler;

/**
 * Regeln des Spielautomaten (docs/16-spielautomat.md): Erst wird der Ausgang gewürfelt, dann die passenden Symbole.
 * So hängt die Auszahlungsquote nur an der Config und nicht an Walzenstreifen. Sie liegt mit den Standardwerten bei
 * rund 88 % — auf Dauer ist der Automat eine Talersenke, keine Geldquelle.
 */
public final class SlotMachine {
    /** Anzahl der Symbole; Index 0 ist der Goldtaler (Jackpot). */
    public static final int SYMBOL_COUNT = 6;
    /** Ticks nach dem Einwurf, an denen die Walzen 1, 2 und 3 anhalten; mit der letzten ist die Drehung aufgelöst. */
    public static final int[] STOPS = {30, 45, 60};
    public static final int DURATION = STOPS[STOPS.length - 1];

    private SlotMachine() {
    }

    public enum Outcome {
        JACKPOT("jackpot"),
        WIN("win"),
        CONSOLATION("consolation"),
        NOTHING("nothing");

        private final String id;

        Outcome(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public static Outcome byOrdinal(int ordinal) {
            Outcome[] all = values();
            return ordinal >= 0 && ordinal < all.length ? all[ordinal] : NOTHING;
        }
    }

    /** Das Item, das ein Symbol auf der Walze zeigt. */
    public static ItemStack symbol(int index) {
        return switch (Math.floorMod(index, SYMBOL_COUNT)) {
            case 0 -> new ItemStack(Taler.GOLD_TALER.item());
            case 1 -> new ItemStack(Items.DIAMOND);
            case 2 -> new ItemStack(Items.EMERALD);
            case 3 -> new ItemStack(Items.BELL);
            case 4 -> new ItemStack(Items.SWEET_BERRIES);
            default -> new ItemStack(Items.MELON_SLICE);
        };
    }

    /** Würfelt mit den Chancen aus der Config. */
    public static Outcome roll(RandomSource random) {
        return roll(random, DtpConfig.SLOT_CHANCE_JACKPOT.get(), DtpConfig.SLOT_CHANCE_WIN.get(),
                DtpConfig.SLOT_CHANCE_CONSOLATION.get());
    }

    /** Ein Wurf, kumulativ geprüft: Ergeben die Chancen zusammen mehr als 1, fallen die hinteren der Reihe nach weg. */
    public static Outcome roll(RandomSource random, double jackpot, double win, double consolation) {
        double r = random.nextDouble();
        if (r < jackpot) {
            return Outcome.JACKPOT;
        }
        if (r < jackpot + win) {
            return Outcome.WIN;
        }
        return r < jackpot + win + consolation ? Outcome.CONSOLATION : Outcome.NOTHING;
    }

    /**
     * Auszahlung in Kupfertalern: Jackpot zwei Stufen höher (×81), Gewinn eine Stufe höher (×9), Trostpreis eine Stufe
     * kleiner (÷9). Beim Kupfertaler gibt es keine kleinere Münze, der Trostpreis bringt dort nichts.
     */
    public static long payout(Outcome outcome, Taler stake) {
        return switch (outcome) {
            case JACKPOT -> stake.value * 81;
            case WIN -> stake.value * 9;
            case CONSOLATION -> stake.value / 9;
            case NOTHING -> 0;
        };
    }

    /** Erwartete Auszahlung je eingesetztem Taler (ohne die Kupfertaler-Ausnahme beim Trostpreis). */
    public static double returnToPlayer(double jackpot, double win, double consolation) {
        return jackpot * 81 + win * 9 + consolation / 9;
    }

    /**
     * Drei Symbole passend zum Ausgang. Jackpot: drei Goldtaler. Gewinn: drei gleiche andere Symbole. Trostpreis: Walze 1
     * und 2 gleich, Walze 3 anders. Nichts: Walze 1 und 2 verschieden, Walze 3 beliebig — Beinahe-Treffer inklusive.
     */
    public static int[] symbols(Outcome outcome, RandomSource random) {
        return switch (outcome) {
            case JACKPOT -> new int[] {0, 0, 0};
            case WIN -> {
                int s = 1 + random.nextInt(SYMBOL_COUNT - 1);
                yield new int[] {s, s, s};
            }
            case CONSOLATION -> {
                int a = random.nextInt(SYMBOL_COUNT);
                yield new int[] {a, a, other(a, random)};
            }
            case NOTHING -> {
                int a = random.nextInt(SYMBOL_COUNT);
                yield new int[] {a, other(a, random), random.nextInt(SYMBOL_COUNT)};
            }
        };
    }

    /** Ein zufälliges Symbol, das nicht {@code not} ist. */
    private static int other(int not, RandomSource random) {
        int s = random.nextInt(SYMBOL_COUNT - 1);
        return s >= not ? s + 1 : s;
    }

    /** Welcher Ausgang zu drei Symbolen gehört — die Umkehrung von {@link #symbols}. */
    public static Outcome outcomeOf(int[] symbols) {
        if (symbols[0] == symbols[1] && symbols[1] == symbols[2]) {
            return symbols[0] == 0 ? Outcome.JACKPOT : Outcome.WIN;
        }
        return symbols[0] == symbols[1] ? Outcome.CONSOLATION : Outcome.NOTHING;
    }
}
