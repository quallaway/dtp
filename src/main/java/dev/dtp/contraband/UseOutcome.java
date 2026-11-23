package dev.dtp.contraband;

import net.minecraft.network.chat.Component;

/**
 * Ergebnis einer Item-Wirkung, gemeinsam für Contraband-Items und Verbrauchsgegenstände vom Händler.
 * Nur {@link Used} kostet etwas (eine Ladung bzw. ein Stück), {@link Refused} nicht.
 */
public sealed interface UseOutcome permits UseOutcome.Used, UseOutcome.Refused {
    record Used() implements UseOutcome {
    }

    /** Nichts passiert; {@code reason} erscheint in der Aktionsleiste ({@code null} = still). */
    record Refused(Component reason) implements UseOutcome {
    }

    UseOutcome USED = new Used();
    UseOutcome SILENT = new Refused(null);

    static UseOutcome refused(String key, Object... args) {
        return new Refused(Component.translatable(key, args));
    }
}
