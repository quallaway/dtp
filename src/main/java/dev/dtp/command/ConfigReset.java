package dev.dtp.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.electronwill.nightconfig.core.UnmodifiableConfig;

import net.neoforged.neoforge.common.ModConfigSpec;

import dev.dtp.DtpConfig;

/**
 * Setzt Werte der Mod-Config auf ihre Standards zurück. Eine bestehende {@code dtp-synced.toml} übernimmt geänderte
 * Standards bei einem Update nicht ({@code docs/72-migration.md}). Gespeichert wird über NeoForge: Die Datei wird
 * neu geschrieben, neu geladen und an verbundene Clients geschickt.
 */
public final class ConfigReset {
    /** Ein Config-Wert mit seinem Schlüssel in Punktschreibweise, z. B. {@code beacon.radiusChunks}. */
    public record Entry(String key, ModConfigSpec.ConfigValue<?> value) {
        public boolean isDefault() {
            return Objects.equals(value.get(), value.getDefault());
        }
    }

    private ConfigReset() {
    }

    /** Alle Werte von {@link DtpConfig#SPEC} in Dateireihenfolge. */
    public static List<Entry> entries() {
        List<Entry> result = new ArrayList<>();
        collect(DtpConfig.SPEC.getValues(), result);
        return result;
    }

    private static void collect(UnmodifiableConfig config, List<Entry> result) {
        for (Object o : config.valueMap().values()) {
            if (o instanceof ModConfigSpec.ConfigValue<?> value) {
                result.add(new Entry(String.join(".", value.getPath()), value));
            } else if (o instanceof UnmodifiableConfig inner) {
                collect(inner, result);
            }
        }
    }

    /** Werte, die vom Standard abweichen. */
    public static List<Entry> changed() {
        return entries().stream().filter(e -> !e.isDefault()).toList();
    }

    /** Setzt die Werte auf ihren Standard und speichert einmal; liefert die tatsächlich geänderten Schlüssel. */
    public static List<String> reset(List<Entry> entries) {
        List<String> changed = new ArrayList<>();
        for (Entry e : entries) {
            if (!e.isDefault()) {
                setDefault(e.value());
                changed.add(e.key());
            }
        }
        if (!changed.isEmpty()) {
            DtpConfig.SPEC.save();
        }
        return changed;
    }

    private static <T> void setDefault(ModConfigSpec.ConfigValue<T> value) {
        value.set(value.getDefault());
    }
}
