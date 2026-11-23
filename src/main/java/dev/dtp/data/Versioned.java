package dev.dtp.data;

import java.util.List;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Encoder;

/**
 * Versionsfeld und Migrationskette für alle gespeicherten Datenblöcke (siehe docs/70-technischer-rahmen.md).
 *
 * Jeder Datenblock trägt ein Feld {@code data_version}. Beim Laden werden alle Schritte von der
 * gespeicherten bis zur aktuellen Version nacheinander angewendet, erst dann wird dekodiert.
 * Daten ohne Versionsfeld gelten als aktuelle Version — es gibt keine Altdaten vor v1.
 */
public final class Versioned {
    public static final String KEY = "data_version";

    /** Ein Migrationsschritt von Version n auf n+1. */
    public interface Step {
        <T> Dynamic<T> migrate(Dynamic<T> data);
    }

    private Versioned() {
    }

    /**
     * @param current aktuelle Version (beginnt bei 1)
     * @param steps   steps.get(i) migriert Version i+1 auf i+2; die Liste hat also current-1 Einträge
     */
    public static <A> Codec<A> codec(int current, Codec<A> inner, List<Step> steps) {
        if (steps.size() != current - 1) {
            throw new IllegalArgumentException("Migration chain needs " + (current - 1) + " steps, got " + steps.size());
        }
        Encoder<A> encoder = new Encoder<>() {
            @Override
            public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                return inner.encode(input, ops, prefix)
                        .flatMap(map -> ops.mergeToMap(map, ops.createString(KEY), ops.createInt(current)));
            }

            @Override
            public String toString() {
                return "Versioned[" + inner + "]";
            }
        };
        Decoder<A> decoder = new Decoder<>() {
            @Override
            public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                Dynamic<T> data = new Dynamic<>(ops, input);
                int version = data.get(KEY).asInt(current);
                if (version > current) {
                    return DataResult.error(() -> "Data version " + version + " is newer than supported version " + current);
                }
                for (int v = version; v < current; v++) {
                    data = steps.get(v - 1).migrate(data);
                }
                return inner.decode(data);
            }
        };
        return Codec.of(encoder, decoder);
    }
}
