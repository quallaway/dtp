package dev.dtp.upgrade;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Preis je Stufe für Server- und Spieler-Upgrades.
 *
 * Preis(Stufe) = Basispreis × Wachstumsfaktor^(Stufe-1), außer {@code costs} nennt die Stufen explizit.
 */
public record CostCurve(long baseCost, double growth, Optional<List<Long>> costs) {
    public static final MapCodec<CostCurve> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.LONG.optionalFieldOf("base_cost", 100L).forGetter(CostCurve::baseCost),
            Codec.DOUBLE.optionalFieldOf("growth", 1.65).forGetter(CostCurve::growth),
            Codec.LONG.listOf().optionalFieldOf("costs").forGetter(CostCurve::costs)
    ).apply(i, CostCurve::new));

    /** Preis für das Erreichen von {@code level} (1-basiert). */
    public long cost(int level) {
        if (costs.isPresent() && !costs.get().isEmpty()) {
            List<Long> list = costs.get();
            return list.get(Math.min(level, list.size()) - 1);
        }
        return Math.round(baseCost * Math.pow(growth, level - 1));
    }
}
