package dev.dtp.contraband;

import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Die Zahlen eines Contraband-Items aus {@code data/<ns>/dtp/contraband/<item>.json}. Die Wirkung steht im Code
 * ({@link ContrabandItem#activate}), alles Einstellbare hier.
 *
 * @param maxCharges       größte Zahl an Ladungen
 * @param costPerUse       Ladungen je Benutzung
 * @param cooldownSeconds  Sperre nach einer Benutzung (Vanilla-Cooldown je Item-Typ)
 * @param chargePrice      Taler je Ladung beim Aufladen an der Reparaturbank
 * @param consumeWhenEmpty Item verschwindet bei 0 Ladungen (Verbrauchsitems wie der Katalysator)
 * @param params           freie Werte je Item, z. B. {@code radius}
 */
public record ContrabandDefinition(int maxCharges, int costPerUse, double cooldownSeconds, long chargePrice, boolean consumeWhenEmpty,
        Map<String, Double> params) {
    /** Ganze Taler ab 1; {@code Codec} hat keinen {@code longRange}. */
    static final Codec<Long> POSITIVE_LONG = Codec.LONG.validate(
            v -> v >= 1L ? DataResult.success(v) : DataResult.error(() -> "Value must be at least 1: " + v));

    public static final Codec<ContrabandDefinition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 100_000).fieldOf("max_charges").forGetter(ContrabandDefinition::maxCharges),
            Codec.intRange(0, 100_000).optionalFieldOf("cost_per_use", 1).forGetter(ContrabandDefinition::costPerUse),
            Codec.doubleRange(0.0, 86_400.0).optionalFieldOf("cooldown_seconds", 0.0).forGetter(ContrabandDefinition::cooldownSeconds),
            POSITIVE_LONG.fieldOf("charge_price").forGetter(ContrabandDefinition::chargePrice),
            Codec.BOOL.optionalFieldOf("consume_when_empty", false).forGetter(ContrabandDefinition::consumeWhenEmpty),
            Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("params", Map.of()).forGetter(ContrabandDefinition::params))
            .apply(i, ContrabandDefinition::new));

    public int cooldownTicks() {
        return (int) Math.round(cooldownSeconds * 20.0);
    }

    public double param(String key, double fallback) {
        return params.getOrDefault(key, fallback);
    }
}
