package dev.dtp.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.dtp.Dtp;
import dev.dtp.contraband.ContrabandDefinition;
import dev.dtp.contraband.ContrabandItem;
import dev.dtp.contract.ContractTemplates;
import dev.dtp.market.MarketCategories;
import dev.dtp.market.TradeList;
import dev.dtp.recycling.Recycling;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.UpgradeCosts;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StrictJsonParser;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.conditions.ConditionalOps;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DataPackRegistriesHooks;

/**
 * Prüflauf für die handgepflegten JSON-Dateien unter {@code data/} des Mods.
 *
 * <p>Jede Datei wird mit dem Codec gelesen, den Minecraft, NeoForge oder der Mod beim Laden benutzt, und danach
 * wieder kodiert. Gemeldet werden:
 * <ul>
 * <li>Dateien, die sich nicht lesen lassen (Minecraft würde sie mit einer Logzeile verwerfen),</li>
 * <li>unbekannte Schlüssel: Sie fehlen nach dem Rundlauf, und weder ihr Entfernen noch ein anderer Wert ändert
 * das Ergebnis. Das fängt still ignorierte Altschlüssel ({@code functions} statt {@code modifier}) und Tippfehler,</li>
 * <li>unbekannte IDs in Tags und in den eigenen Formaten (Handelsliste, Kategorien, Contracts, …),</li>
 * <li>Ordner, für die der Prüflauf keinen Codec kennt.</li>
 * </ul>
 * Explizit gesetzte Standardwerte ({@code "weight": 1}) fehlen nach dem Rundlauf ebenfalls, wirken aber, sobald
 * der Wert sich ändert; sie landen nur als Hinweis in {@link Result#defaults()}. Die Dateien bleiben unverändert.
 */
final class DataFileCheck {
    private static final String CONDITIONS = ConditionalOps.DEFAULT_CONDITIONS_KEY;

    private final RegistryAccess registries;
    private final RegistryOps<JsonElement> ops;
    /** Ordner unterhalb von {@code data/<ns>/} → Codec der Dateien darin. */
    private final Map<String, Codec<?>> codecs = new HashMap<>();
    /** Ordner unterhalb von {@code data/<ns>/} → Registry der Tags darin. */
    private final Map<String, ResourceKey<? extends Registry<?>>> tagDirs = new HashMap<>();
    private final List<String> problems = new ArrayList<>();
    private final List<String> defaults = new ArrayList<>();
    private int files;

    private DataFileCheck(MinecraftServer server) {
        this.registries = server.registryAccess();
        this.ops = server.reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
        Stream.concat(DataPackRegistriesHooks.getWorldRegistriesWithDimensions(), RegistryDataLoader.RELOADABLE_REGISTRIES.stream())
                .forEach(data -> codecs.put(Registries.elementsDirPath(data.key()), data.elementCodec()));
        codecs.put("loot_modifiers", IGlobalLootModifier.DIRECT_CODEC);
        codecs.put("dtp/trade_list", TradeList.TradeFile.CODEC);
        codecs.put("dtp/market_category", MarketCategories.Definition.CODEC);
        codecs.put("dtp/upgrade", UpgradeCosts.Definition.CODEC);
        codecs.put("dtp/player_upgrade", PlayerUpgradeDefinitions.Definition.CODEC);
        codecs.put("dtp/contract", ContractTemplates.TemplateFile.CODEC);
        codecs.put("dtp/contraband", ContrabandDefinition.CODEC);
        codecs.put("dtp/recycling", Recycling.RecyclingFile.CODEC);
        registries.registries().forEach(entry -> tagDirs.put(Registries.tagsDirPath(entry.key()), entry.key()));
    }

    /** Prüft alle Dateien; das Ergebnis ist leer, wenn alles in Ordnung ist. */
    static Result run(MinecraftServer server) {
        DataFileCheck check = new DataFileCheck(server);
        Map<String, JsonElement> sorted = new TreeMap<>();
        ModList.get().getModFileById(Dtp.MODID).getFile().getContents().visitContent("data", (path, resource) -> {
            if (!path.endsWith(".json")) {
                return;
            }
            try (Reader reader = resource.bufferedReader()) {
                sorted.put(path, StrictJsonParser.parse(reader));
            } catch (IOException | RuntimeException e) {
                check.problems.add(path + ": kein gültiges JSON (" + e.getMessage() + ")");
            }
        });
        sorted.forEach(check::checkFile);
        return new Result(check.files, List.copyOf(check.problems), List.copyOf(check.defaults));
    }

    /** {@code problems} lassen den Test scheitern; {@code defaults} sind explizit gesetzte Standardwerte (nur Hinweis). */
    record Result(int files, List<String> problems, List<String> defaults) {
    }

    private void checkFile(String path, JsonElement json) {
        files++;
        // data/<namespace>/<ordner…>/<name>.json
        String[] parts = path.split("/", 3);
        if (parts.length < 3 || !parts[0].equals("data")) {
            problems.add(path + ": liegt nicht unter data/<namespace>/");
            return;
        }
        String namespace = parts[1];
        String rest = parts[2].substring(0, parts[2].length() - ".json".length());
        if (json.isJsonObject() && json.getAsJsonObject().has(CONDITIONS)) {
            if (!ICondition.conditionsMatched(ops, json)) {
                return; // z. B. Compat-Rezept ohne den anderen Mod: Minecraft lädt die Datei auch nicht
            }
            json = json.deepCopy();
            json.getAsJsonObject().remove(CONDITIONS);
        }
        String tagDir = longestPrefix(tagDirs.keySet(), rest);
        if (tagDir != null) {
            TagFile tags = decode(path, TagFile.CODEC, json);
            if (tags != null) {
                checkTag(path, tagDirs.get(tagDir), tags);
            }
            return;
        }
        String dir = longestPrefix(codecs.keySet(), rest);
        if (dir == null) {
            problems.add(path + ": kein Codec für diesen Ordner – Prüflauf in DataFileCheck ergänzen");
            return;
        }
        Object value = decode(path, codecs.get(dir), json);
        if (value != null && json.isJsonObject()) {
            Identifier id = Identifier.fromNamespaceAndPath(namespace, rest.substring(dir.length() + 1));
            checkIds(path, dir, id, json.getAsJsonObject());
        }
    }

    /** Der längste Ordner aus {@code dirs}, unter dem {@code rest} liegt (Ordner können verschachtelt sein). */
    private static String longestPrefix(Iterable<String> dirs, String rest) {
        String best = null;
        for (String dir : dirs) {
            if (rest.startsWith(dir + "/") && (best == null || dir.length() > best.length())) {
                best = dir;
            }
        }
        return best;
    }

    // ---- Rundlauf ----

    /** Liest die Datei, meldet Lesefehler und wirkungslose Schlüssel; {@code null}, wenn sie sich nicht lesen lässt. */
    private <T> T decode(String path, Codec<T> codec, JsonElement json) {
        DataResult<T> parsed = codec.parse(ops, json);
        if (parsed.error().isPresent()) {
            problems.add(path + ": lässt sich nicht lesen – " + parsed.error().get().message());
            return null;
        }
        T value = parsed.getOrThrow();
        DataResult<JsonElement> encoded = codec.encodeStart(ops, value);
        if (encoded.error().isPresent()) {
            // Manche Codecs lassen sich nur lesen; dann entfällt der Schlüsselvergleich.
            Dtp.LOGGER.info("Data file check: {} cannot be re-encoded, key comparison skipped: {}", path, encoded.error().get().message());
            return value;
        }
        compare(path, codec, json, json, encoded.getOrThrow(), new ArrayList<>());
        return value;
    }

    /**
     * Sucht Schlüssel, die im Original stehen, aber nach dem Rundlauf fehlen. Liefert die Datei ohne ihn dasselbe
     * Ergebnis, entscheidet eine Gegenprobe mit anderem Wert: Ändert sich dann etwas (oder scheitert das Lesen), kennt
     * der Codec den Schlüssel und er steht auf dem Standardwert; sonst wird er ignoriert. Ändert schon das Entfernen
     * etwas, hat der Codec ihn nur anders geschrieben.
     */
    private <T> void compare(String path, Codec<T> codec, JsonElement root, JsonElement original, JsonElement encoded, List<Object> at) {
        if (original.isJsonObject() && encoded.isJsonObject()) {
            JsonObject enc = encoded.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : original.getAsJsonObject().entrySet()) {
                List<Object> child = new ArrayList<>(at);
                child.add(entry.getKey());
                if (enc.has(entry.getKey())) {
                    compare(path, codec, root, entry.getValue(), enc.get(entry.getKey()), child);
                } else if (sameResult(codec, root, child, null)) {
                    if (sameResult(codec, root, child, probe(entry.getValue()))) {
                        problems.add(path + ": unbekannter Schlüssel " + format(child) + " wird ignoriert (Tippfehler oder veraltet?)");
                    } else {
                        defaults.add(path + ": " + format(child) + " steht auf dem Standardwert");
                    }
                }
            }
        } else if (original.isJsonArray() && encoded.isJsonArray()) {
            JsonArray orig = original.getAsJsonArray();
            JsonArray enc = encoded.getAsJsonArray();
            if (orig.size() == enc.size()) {
                for (int i = 0; i < orig.size(); i++) {
                    List<Object> child = new ArrayList<>(at);
                    child.add(i);
                    compare(path, codec, root, orig.get(i), enc.get(i), child);
                }
            }
        }
    }

    /** Ergibt die Datei dasselbe, wenn der Schlüssel entfernt ({@code replacement == null}) oder ersetzt wird? */
    private <T> boolean sameResult(Codec<T> codec, JsonElement root, List<Object> key, JsonElement replacement) {
        JsonElement full = encode(codec, root);
        JsonElement copy = root.deepCopy();
        JsonElement parent = copy;
        for (Object step : key.subList(0, key.size() - 1)) {
            parent = step instanceof Integer i ? parent.getAsJsonArray().get(i) : parent.getAsJsonObject().get((String) step);
        }
        String name = (String) key.getLast();
        if (replacement == null) {
            parent.getAsJsonObject().remove(name);
        } else {
            parent.getAsJsonObject().add(name, replacement);
        }
        JsonElement changed = encode(codec, copy);
        return full != null && full.equals(changed);
    }

    /** Ein anderer Wert für die Gegenprobe: Zahlen +1, Wahrheitswerte umgekehrt, alles andere eine Zahl statt Text/Objekt. */
    private static JsonElement probe(JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
            return new JsonPrimitive(value.getAsDouble() + 1);
        }
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean()) {
            return new JsonPrimitive(!value.getAsBoolean());
        }
        return new JsonPrimitive(12345);
    }

    private <T> JsonElement encode(Codec<T> codec, JsonElement json) {
        return codec.parse(ops, json).flatMap(value -> codec.encodeStart(ops, value)).result().orElse(null);
    }

    private static String format(List<Object> key) {
        StringBuilder out = new StringBuilder();
        for (Object step : key) {
            out.append(step instanceof Integer i ? "[" + i + "]" : (out.isEmpty() ? "" : ".") + step);
        }
        return "'" + out + "'";
    }

    // ---- IDs ----

    private void checkTag(String path, ResourceKey<? extends Registry<?>> registryKey, TagFile file) {
        Registry<?> registry = registries.lookupOrThrow(registryKey);
        for (TagEntry entry : file.entries()) {
            if (entry.isRequired() && !known(registry, entry)) {
                problems.add(path + ": unbekannt in " + registryKey.identifier().getPath() + ": " + (entry.isTag() ? "#" : "") + entry.getId());
            }
        }
    }

    private static <T> boolean known(Registry<T> registry, TagEntry entry) {
        return entry.isTag() ? registry.get(TagKey.create(registry.key(), entry.getId())).isPresent() : registry.containsKey(entry.getId());
    }

    /** Verweise der eigenen Formate, die ihr Codec nur als {@link Identifier} liest. */
    private void checkIds(String path, String dir, Identifier id, JsonObject json) {
        Consumer<JsonElement> item = e -> requireItem(path, e);
        switch (dir) {
            case "dtp/trade_list" -> each(json, "entries", entry -> {
                item.accept(entry.get("item"));
                if (entry.has("tag")) {
                    Identifier tag = Identifier.parse(entry.get("tag").getAsString());
                    if (BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tag)).isEmpty()) {
                        problems.add(path + ": unbekannter Item-Tag #" + tag);
                    }
                }
            });
            case "dtp/recycling" -> each(json, "entries", entry -> item.accept(entry.get("item")));
            case "dtp/market_category" -> {
                item.accept(json.get("icon"));
                Identifier tag = id.withPrefix("market_category/");
                if (BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, tag)).isEmpty()) {
                    problems.add(path + ": Item-Tag #" + tag + " der Kategorie fehlt");
                }
            }
            case "dtp/contract" -> {
                each(json, "templates", template -> {
                    item.accept(template.get("item"));
                    if (template.has("random_enchantments")
                            && template.getAsJsonObject("random_enchantments").get("exclude") instanceof JsonArray exclude) {
                        exclude.forEach(e -> requireEnchantment(path, e));
                    }
                });
                if (json.has("enchantments") && json.getAsJsonObject("enchantments").has("values")) {
                    json.getAsJsonObject("enchantments").getAsJsonObject("values").keySet()
                            .forEach(key -> requireEnchantment(path, new JsonPrimitive(key)));
                }
            }
            case "dtp/contraband" -> {
                if (!(BuiltInRegistries.ITEM.getOptional(id).orElse(null) instanceof ContrabandItem)) {
                    problems.add(path + ": " + id + " ist kein Contraband-Item");
                }
            }
            case "dtp/upgrade", "dtp/player_upgrade" -> {
                requireIcon(path, json.get("icon"));
                if (json.has("level_icons")) {
                    json.getAsJsonArray("level_icons").forEach(e -> requireIcon(path, e));
                }
            }
            default -> {
            }
        }
    }

    /** Für jedes Objekt in {@code json[key]}. */
    private static void each(JsonObject json, String key, Consumer<JsonObject> action) {
        if (json.get(key) instanceof JsonArray array) {
            for (JsonElement e : array) {
                if (e.isJsonObject()) {
                    action.accept(e.getAsJsonObject());
                }
            }
        }
    }

    private void requireItem(String path, JsonElement e) {
        if (e == null || !e.isJsonPrimitive()) {
            return;
        }
        Identifier id = Identifier.tryParse(e.getAsString());
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            problems.add(path + ": unbekanntes Item " + e.getAsString());
        }
    }

    /** Ein Icon ist eine Item-ID oder ein Objekt mit Sprite; Sprites sind Client-Ressourcen (scripts/check_assets.py). */
    private void requireIcon(String path, JsonElement e) {
        if (e != null && e.isJsonPrimitive()) {
            requireItem(path, e);
        }
    }

    private void requireEnchantment(String path, JsonElement e) {
        Identifier id = Identifier.tryParse(e.getAsString());
        if (id == null || !registries.lookupOrThrow(Registries.ENCHANTMENT).containsKey(id)) {
            problems.add(path + ": unbekannte Verzauberung " + e.getAsString());
        }
    }
}
