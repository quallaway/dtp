package dev.dtp.contraband;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;

import dev.dtp.Dtp;

/**
 * Lädt die Definitionen der Contraband-Items aus {@code data/<ns>/dtp/contraband/<item>.json}; der Dateiname ist
 * die Item-ID. Fehlt eine Datei, gelten die Werte aus dem Code ({@link ContrabandItem#defaults()}), damit ein Item auch
 * ohne Datapack benutzbar bleibt.
 */
public class ContrabandDefinitions extends SimpleJsonResourceReloadListener<ContrabandDefinition> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "contraband");

    private static Map<Item, ContrabandDefinition> definitions = Map.of();

    public ContrabandDefinitions() {
        super(ContrabandDefinition.CODEC, FileToIdConverter.json("dtp/contraband"));
    }

    @Override
    protected void apply(Map<Identifier, ContrabandDefinition> loaded, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, ContrabandDefinition> result = new HashMap<>();
        loaded.forEach((id, def) -> {
            Item item = BuiltInRegistries.ITEM.getOptional(id).orElse(null);
            if (item instanceof ContrabandItem) {
                result.put(item, def);
            } else {
                Dtp.LOGGER.warn("Contraband definition '{}' does not name a contraband item, ignored", id);
            }
        });
        definitions = Map.copyOf(result);
        Dtp.LOGGER.info("Loaded {} contraband definitions", result.size());
    }

    public static ContrabandDefinition get(ContrabandItem item) {
        ContrabandDefinition def = definitions.get(item);
        return def != null ? def : item.defaults();
    }

    /** Ob das Datapack eine Definition für das Item mitbringt (für Tests). */
    public static boolean loaded(Item item) {
        return definitions.containsKey(item);
    }
}
