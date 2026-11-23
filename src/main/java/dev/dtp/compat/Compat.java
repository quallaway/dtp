package dev.dtp.compat;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

import dev.dtp.compat.sophisticated.SophisticatedCompat;

/**
 * Einstieg für optionale Anbindungen an andere Mods (docs/70-technischer-rahmen.md, „Kompatibilität").
 *
 * <p>Regel: Klassen eines fremden Mods kommen nur in {@code dev.dtp.compat.<mod>} vor und werden nur über diese
 * Klasse erreicht — und nur, wenn der Mod geladen ist. Diese Klasse selbst importiert nichts Fremdes. So lädt die JVM
 * eine Compat-Klasse nie ohne ihren Mod, und DTP läuft ohne jeden davon. Der Kern kennt die Compat-Module nicht;
 * sie docken über DTPs eigene Schnittstellen an (z. B. {@link dev.dtp.data.DtpStorage}).
 */
public final class Compat {
    public static final String SOPHISTICATED_BACKPACKS = "sophisticatedbackpacks";

    private Compat() {
    }

    public static void init(IEventBus modBus) {
        if (ModList.get().isLoaded(SOPHISTICATED_BACKPACKS)) {
            SophisticatedCompat.init(modBus);
        }
    }
}
