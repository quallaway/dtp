package dev.dtp.contraband;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Komponente {@code dtp:charges}: die Ladungen eines Contraband-Items. Ladungen sind bewusst keine Haltbarkeit —
 * ohne {@code max_damage} greifen weder Mending, Mending-Aura, Amboss noch Schleifstein, und das Item zerbricht nie.
 *
 * <p>{@code max} und {@code price} (Taler je Ladung) sind ein Abbild der Datapack-Definition, damit der Client
 * Balken und Tooltip ohne eigene Synchronisierung zeichnen kann. Der Server gleicht sie bei jeder Benutzung und
 * Aufladung an ({@link ContrabandItem#refresh}).
 */
public record Charges(int charges, int max, long price) {
    public static final Codec<Charges> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(0, Integer.MAX_VALUE).fieldOf("charges").forGetter(Charges::charges),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("max").forGetter(Charges::max),
            ContrabandDefinition.POSITIVE_LONG.fieldOf("price").forGetter(Charges::price))
            .apply(i, Charges::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Charges> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Charges::charges,
            ByteBufCodecs.VAR_INT, Charges::max,
            ByteBufCodecs.VAR_LONG, Charges::price,
            Charges::new);

    public static Charges full(ContrabandDefinition def) {
        return new Charges(def.maxCharges(), def.maxCharges(), def.chargePrice());
    }

    public Charges withCharges(int value) {
        return new Charges(Math.clamp(value, 0, max), max, price);
    }

    public boolean full() {
        return charges >= max;
    }
}
