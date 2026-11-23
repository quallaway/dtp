package dev.dtp.client;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.slf4j.Logger;

import dev.dtp.Dtp;
import dev.dtp.guide.GuideBook;

/**
 * Lädt die Handbücher aus {@code assets/dtp/guide/<sprache>.md}, bei jedem Resource-Reload (F3+T) neu. Ein
 * Resourcepack kann die Dateien überschreiben. Gewählt wird beim Öffnen: Spielsprache, sonst Englisch, sonst Deutsch.
 */
public final class GuideBooks implements ResourceManagerReloadListener {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "guide");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DIRECTORY = "guide";

    private static final Map<String, GuideBook> BOOKS = new HashMap<>();
    private static final Map<String, String> FAILURES = new HashMap<>();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, GuideBook> books = new HashMap<>();
        Map<String, String> failures = new HashMap<>();
        manager.listResources(DIRECTORY, id -> id.getNamespace().equals(Dtp.MODID) && id.getPath().endsWith(".md"))
                .forEach((id, resource) -> {
                    String lang = id.getPath().substring(DIRECTORY.length() + 1, id.getPath().length() - 3);
                    try {
                        GuideBook book = GuideBook.parse(read(resource));
                        book.problems().forEach(p -> LOGGER.warn("Handbook {}: {}", id, p));
                        books.put(lang, book);
                    } catch (IOException | RuntimeException e) {
                        LOGGER.error("Handbook {} could not be read", id, e);
                        failures.put(lang, id + ": " + e.getMessage());
                    }
                });
        synchronized (BOOKS) {
            BOOKS.clear();
            BOOKS.putAll(books);
            FAILURES.clear();
            FAILURES.putAll(failures);
        }
    }

    private static String read(Resource resource) throws IOException {
        try (Reader reader = resource.openAsReader()) {
            StringBuilder out = new StringBuilder();
            char[] buffer = new char[4096];
            for (int n; (n = reader.read(buffer)) > 0;) {
                out.append(buffer, 0, n);
            }
            return out.toString();
        }
    }

    /** Das Handbuch in der Spielsprache, mit Rückfall auf Englisch und Deutsch; nie {@code null}. */
    public static GuideBook current() {
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        synchronized (BOOKS) {
            for (String lang : new String[] {selected, "en_us", "de_de"}) {
                GuideBook book = BOOKS.get(lang);
                if (book != null && !book.chapters().isEmpty()) {
                    return book;
                }
            }
            String failure = FAILURES.getOrDefault(selected, FAILURES.values().stream().findFirst().orElse("assets/dtp/guide/*.md"));
            return GuideBook.error(I18n.get("guide.dtp.load_error"), failure);
        }
    }
}
