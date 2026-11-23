package dev.dtp.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import dev.dtp.Dtp;
import dev.dtp.market.MarketCategories;

/**
 * Die kuratierte Contract-Liste aus {@code data/<ns>/dtp/contract/*.json}. Hier dürfen auch Items stehen,
 * die am freien Markt nicht verkäuflich sind — dann muss {@code reward_per_item} gesetzt sein.
 *
 * <ul>
 * <li>{@code tier}: eine Stufe oder eine Liste von Stufen. Dieselbe Ware kann so in mehreren Stufen gefordert
 *     werden; die Menge wächst mit der Stufe.</li>
 * <li>{@code min_count}/{@code max_count}: feste Menge. Fehlen beide, folgt die Menge aus dem Wertziel der Stufe
 *     ({@code contracts.tierValue}) und dem Basiswert der Handelsliste — außer bei {@code reward_per_item}:
 *     dann ist die Menge 1.</li>
 * <li>{@code weight}: Gewicht bei der Auswahl (Standard 1), statt dieselbe Vorlage mehrfach aufzuführen.</li>
 * <li>{@code key}: Marktschlüssel einer gemeinsamen Ware der Handelsliste, etwa {@code dtp:wood_log}. Dann erfüllt
 *     jedes Item dieser Ware den Contract; {@code item} ist nur Symbol und Basiswert.</li>
 * <li>{@code unlock}: Freischaltung wie bei den Marktkategorien; bis dahin wird die Vorlage nicht ausgewürfelt.</li>
 * <li>{@code components}/{@code predicates} (Vanilla-Format wie in Advancements): Mindestbedingungen an das Item,
 *     etwa eine Verzauberung ab einer bestimmten Stufe.</li>
 * <li>{@code random_enchantments}: Beim Aushängen werden {@code count} verträgliche Verzauberungen gezogen und
 *     als Mindestbedingung gefordert. Der Pool sind alle Einträge der Wertetabelle, die aufs Item passen (Bücher:
 *     alle), ohne {@code exclude}. Die Stufe ist zufällig das Maximum oder eins darunter. Die Belohnung je Item ist
 *     {@code reward_per_item} plus die Werte der gezogenen Verzauberungen. Bei Büchern gelten die gespeicherten
 *     Verzauberungen.</li>
 * <li>{@code random_potion}: Beim Aushängen wird ein gebrauter Trank aus der Tranktabelle gezogen — immer verlängert
 *     oder verstärkt, wahlweise als Wurftrank ({@code splash_chance}, Standard 0,5). Gefordert wird genau dieser
 *     Trank. Wert, Menge und Lohn folgen aus der Tranktabelle, {@code reward_per_item} und feste Mengen gelten nicht.
 *     {@code exclude} nimmt Grundtränke aus.</li>
 * </ul>
 *
 * <p>Die Wertetabelle steht auf oberster Ebene unter {@code enchantments}: {@code values} (Wert je Verzauberung bei
 * Maximalstufe) und {@code below_max_factor} (Anteil des Werts eine Stufe darunter). Mehrere Dateien werden
 * zusammengeführt.
 *
 * <p>Die Tranktabelle steht auf oberster Ebene unter {@code potions}: Zutatenwerte je Grundtrank ({@code values}),
 * die gemeinsamen Zutaten (Netherwarze, Redstone, Glowstone, Schwarzpulver, Flasche) und die Lohnformel. Mehrere
 * Dateien werden zusammengeführt, die Werte je Grundtrank überschreiben sich.
 */
public class ContractTemplates extends SimpleJsonResourceReloadListener<ContractTemplates.TemplateFile> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "contracts");

    public record Template(Identifier item, Optional<Identifier> key, DataComponentMatchers match, List<Integer> tiers,
            Optional<Integer> minCount, Optional<Integer> maxCount, Optional<Double> rewardPerItem, MarketCategories.Unlock unlock,
            Optional<RandomEnchantments> randomEnchantments, Optional<RandomPotion> randomPotion, int weight) {
        public static final Codec<Template> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.fieldOf("item").forGetter(Template::item),
                Identifier.CODEC.optionalFieldOf("key").forGetter(Template::key),
                DataComponentMatchers.CODEC.forGetter(Template::match),
                ExtraCodecs.compactListCodec(Codec.intRange(1, 100)).fieldOf("tier").forGetter(Template::tiers),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("min_count").forGetter(Template::minCount),
                Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("max_count").forGetter(Template::maxCount),
                Codec.DOUBLE.optionalFieldOf("reward_per_item").forGetter(Template::rewardPerItem),
                MarketCategories.Unlock.CODEC.optionalFieldOf("unlock", MarketCategories.Unlock.NONE).forGetter(Template::unlock),
                RandomEnchantments.CODEC.optionalFieldOf("random_enchantments").forGetter(Template::randomEnchantments),
                RandomPotion.CODEC.optionalFieldOf("random_potion").forGetter(Template::randomPotion),
                Codec.intRange(1, 1000).optionalFieldOf("weight", 1).forGetter(Template::weight)
        ).apply(i, Template::new));

        public boolean hasTier(int tier) {
            return tiers.contains(tier);
        }

        public int lowestTier() {
            return tiers.stream().mapToInt(Integer::intValue).min().orElse(1);
        }

        /**
         * Feste Menge aus der Vorlage; sonst wird sie aus dem Wertziel der Stufe berechnet. Ein fester Preis ohne
         * Mengenangabe bedeutet Menge 1.
         */
        public boolean fixedCount() {
            return minCount.isPresent() || rewardPerItem.isPresent();
        }

        /** Würfelt die Vorlage ihre Bedingung erst beim Aushängen? */
        public boolean isRandom() {
            return randomEnchantments.isPresent() || randomPotion.isPresent();
        }

        public int fixedMin() {
            return minCount.orElse(1);
        }

        public int fixedMax() {
            return Math.max(fixedMin(), maxCount.orElse(1));
        }

        /**
         * Item und Bedingung als Schlüssel — gleich für alle Stufen dieser Vorlage. Zufallsvorlagen bekommen einen
         * eigenen Schlüssel, damit sie nicht mit „beliebiges Buch“ (gleiches Item, leere Bedingung) zusammenfallen.
         */
        public String requirement(HolderLookup.Provider registries) {
            if (isRandom()) {
                return randomRequirement(item);
            }
            return Contract.requirement(item, key, match, registries);
        }

        /**
         * Stammt das Angebot aus dieser Zufallsvorlage? Gleiches Item und genau {@code count} geforderte
         * Verzauberungen, alle aus der Wertetabelle und nicht ausgeschlossen. So hängt je Zufallsvorlage höchstens
         * ein Angebot aus.
         */
        public boolean produced(Contract contract, HolderLookup.Provider registries) {
            if (randomPotion.isPresent()) {
                return randomPotion.get().produced(contract);
            }
            if (randomEnchantments.isEmpty() || !contract.item().equals(item) || contract.key().isPresent()
                    || !contract.requiresEnchantments()) {
                return false;
            }
            RandomEnchantments random = randomEnchantments.get();
            ItemStack shown = contract.displayStack(registries);
            ItemEnchantments enchantments = shown.getOrDefault(random.component(item), ItemEnchantments.EMPTY);
            if (enchantments.size() != random.count()) {
                return false;
            }
            return enchantments.keySet().stream().allMatch(holder -> holder.unwrapKey()
                    .map(k -> random.allows(k.identifier()))
                    .orElse(false));
        }
    }

    public static String randomRequirement(Identifier item) {
        return item + "?random";
    }

    /** Wert je Verzauberung bei Maximalstufe; eine Stufe darunter gilt {@code below_max_factor} davon. */
    public record EnchantmentValues(double belowMaxFactor, Map<Identifier, Double> values) {
        public static final EnchantmentValues EMPTY = new EnchantmentValues(0.6, Map.of());
        public static final Codec<EnchantmentValues> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.0, 1.0).optionalFieldOf("below_max_factor", 0.6).forGetter(EnchantmentValues::belowMaxFactor),
                Codec.unboundedMap(Identifier.CODEC, Codec.DOUBLE).optionalFieldOf("values", Map.of()).forGetter(EnchantmentValues::values)
        ).apply(i, EnchantmentValues::new));

        /** Wert der Verzauberung in dieser Stufe. */
        public double value(Identifier enchantment, int level, int maxLevel) {
            double full = values.getOrDefault(enchantment, 0.0);
            return level < maxLevel ? full * belowMaxFactor : full;
        }
    }

    /** Gewürfelte Bedingung und die Summe der Aufschläge. */
    public record Rolled(DataComponentMatchers match, double value) {
    }

    public record RandomEnchantments(int count, List<Identifier> exclude) {
        public static final Codec<RandomEnchantments> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.intRange(1, 16).fieldOf("count").forGetter(RandomEnchantments::count),
                Identifier.CODEC.listOf().optionalFieldOf("exclude", List.of()).forGetter(RandomEnchantments::exclude)
        ).apply(i, RandomEnchantments::new));

        /** Bücher speichern ihre Verzauberungen, Ausrüstung trägt sie. */
        public net.minecraft.core.component.DataComponentType<ItemEnchantments> component(Identifier item) {
            return isBook(item) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
        }

        private static boolean isBook(Identifier item) {
            return item.equals(Identifier.withDefaultNamespace("enchanted_book"));
        }

        /** Darf diese Verzauberung gewürfelt werden? Sie muss in der Wertetabelle stehen und nicht ausgeschlossen sein. */
        public boolean allows(Identifier enchantment) {
            return enchantmentValues.values().containsKey(enchantment) && !exclude.contains(enchantment);
        }

        /** Alle erlaubten Verzauberungen, die aufs Item passen; Bücher nehmen jede. Sortiert, damit das Würfeln reproduzierbar ist. */
        public List<Holder.Reference<Enchantment>> pool(Identifier item, HolderLookup.Provider registries) {
            HolderLookup.RegistryLookup<Enchantment> lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(item));
            List<Holder.Reference<Enchantment>> result = new ArrayList<>();
            enchantmentValues.values().keySet().stream().sorted().filter(this::allows).forEach(id -> {
                Optional<Holder.Reference<Enchantment>> holder = lookup.get(ResourceKey.create(Registries.ENCHANTMENT, id));
                if (holder.isEmpty()) {
                    Dtp.LOGGER.warn("Contract enchantment values: unknown enchantment {}", id);
                } else if (isBook(item) || holder.get().value().isSupportedItem(stack)) {
                    result.add(holder.get());
                }
            });
            return result;
        }

        /**
         * Zieht {@code count} verschiedene, untereinander verträgliche Verzauberungen in zufälliger Reihenfolge, je
         * in Maximalstufe oder eins darunter. Leer, wenn der Pool nicht genug verträgliche hergibt.
         */
        public Optional<Rolled> roll(Identifier item, HolderLookup.Provider registries, RandomSource random) {
            List<Holder.Reference<Enchantment>> order = pool(item, registries);
            for (int n = order.size() - 1; n > 0; n--) {
                int j = random.nextInt(n + 1);
                Holder.Reference<Enchantment> tmp = order.get(n);
                order.set(n, order.get(j));
                order.set(j, tmp);
            }
            List<Holder.Reference<Enchantment>> picked = new ArrayList<>();
            for (Holder.Reference<Enchantment> holder : order) {
                if (picked.size() >= count) {
                    break;
                }
                if (picked.stream().allMatch(h -> Enchantment.areCompatible(h, holder))) {
                    picked.add(holder);
                }
            }
            if (picked.size() < count) {
                Dtp.LOGGER.warn("Contract template for {}: pool has fewer than {} compatible enchantments", item, count);
                return Optional.empty();
            }
            JsonArray list = new JsonArray();
            double value = 0;
            for (Holder.Reference<Enchantment> holder : picked) {
                Identifier id = holder.key().identifier();
                int max = Math.max(1, holder.value().getMaxLevel());
                int level = max > 1 && random.nextBoolean() ? max - 1 : max;
                value += enchantmentValues.value(id, level, max);
                JsonObject predicate = new JsonObject();
                predicate.addProperty("enchantments", id.toString());
                JsonObject levels = new JsonObject();
                levels.addProperty("min", level);
                predicate.add("levels", levels);
                list.add(predicate);
            }
            JsonObject predicates = new JsonObject();
            predicates.add(isBook(item) ? "minecraft:stored_enchantments" : "minecraft:enchantments", list);
            JsonObject root = new JsonObject();
            root.add("predicates", predicates);
            double total = value;
            return DataComponentMatchers.CODEC.codec()
                    .parse(registries.createSerializationContext(JsonOps.INSTANCE), root)
                    .resultOrPartial(error -> Dtp.LOGGER.warn("Contract template for {}: {}", item, error))
                    .map(match -> new Rolled(match, total));
        }
    }

    /** Zutatenwert eines Grundtranks; {@code awkward} = false, wenn er ohne Netherwarze gebraut wird (Schwäche). */
    public record PotionValue(double ingredients, boolean awkward) {
        public static final Codec<PotionValue> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.0, Double.MAX_VALUE).fieldOf("ingredients").forGetter(PotionValue::ingredients),
                Codec.BOOL.optionalFieldOf("awkward", true).forGetter(PotionValue::awkward)
        ).apply(i, PotionValue::new));
    }

    /**
     * Tranktabelle. Wert je Trank = (Netherwarze + Zutaten + Redstone oder Glowstone [+ Schwarzpulver]) / {@code batch}
     * + {@code bottle}, denn eine Zutat braut {@code batch} Tränke. Lohn je Trank = Wert × {@code reward_multiplier}
     * + {@code surcharge}. Die Menge folgt aus dem Wertziel der Stufe; liegt sie über {@code max_count}, wird
     * {@code capped_min}–{@code max_count} gewürfelt.
     */
    public record PotionValues(double batch, double awkward, double bottle, double longer, double stronger, double splash,
            double rewardMultiplier, double surcharge, int maxCount, int cappedMin, Map<Identifier, PotionValue> values) {
        public static final PotionValues EMPTY = new PotionValues(3, 0, 0, 0, 0, 0, 1, 0, 160, 96, Map.of());
        public static final Codec<PotionValues> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(1.0, 64.0).optionalFieldOf("batch", 3.0).forGetter(PotionValues::batch),
                Codec.DOUBLE.optionalFieldOf("awkward", 0.0).forGetter(PotionValues::awkward),
                Codec.DOUBLE.optionalFieldOf("bottle", 0.0).forGetter(PotionValues::bottle),
                Codec.DOUBLE.optionalFieldOf("long", 0.0).forGetter(PotionValues::longer),
                Codec.DOUBLE.optionalFieldOf("strong", 0.0).forGetter(PotionValues::stronger),
                Codec.DOUBLE.optionalFieldOf("splash", 0.0).forGetter(PotionValues::splash),
                Codec.doubleRange(0.0, 1000.0).optionalFieldOf("reward_multiplier", 1.0).forGetter(PotionValues::rewardMultiplier),
                Codec.DOUBLE.optionalFieldOf("surcharge", 0.0).forGetter(PotionValues::surcharge),
                Codec.intRange(1, 10000).optionalFieldOf("max_count", 160).forGetter(PotionValues::maxCount),
                Codec.intRange(1, 10000).optionalFieldOf("capped_min", 96).forGetter(PotionValues::cappedMin),
                Codec.unboundedMap(Identifier.CODEC, PotionValue.CODEC).optionalFieldOf("values", Map.of()).forGetter(PotionValues::values)
        ).apply(i, PotionValues::new));

        /** Wert eines Tranks aus dem Grundtrank {@code family}; leer, wenn er nicht in der Tabelle steht. */
        public Optional<Double> value(Identifier family, boolean strong, boolean splashPotion) {
            PotionValue v = values.get(family);
            if (v == null) {
                return Optional.empty();
            }
            double sum = (v.awkward() ? awkward : 0) + v.ingredients() + (strong ? stronger : longer) + (splashPotion ? splash : 0);
            return Optional.of(sum / batch + bottle);
        }

        /** Lohn je Trank. */
        public double reward(double value) {
            return value * rewardMultiplier + surcharge;
        }
    }

    /** Gewürfelter Trank: Item (Trank oder Wurftrank), exakte Bedingung und Wert je Stück. */
    public record RolledPotion(Identifier item, DataComponentMatchers match, double value) {
    }

    public static final Identifier POTION = Identifier.withDefaultNamespace("potion");
    public static final Identifier SPLASH_POTION = Identifier.withDefaultNamespace("splash_potion");

    public record RandomPotion(double splashChance, List<Identifier> exclude) {
        public static final Codec<RandomPotion> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.doubleRange(0.0, 1.0).optionalFieldOf("splash_chance", 0.5).forGetter(RandomPotion::splashChance),
                Identifier.CODEC.listOf().optionalFieldOf("exclude", List.of()).forGetter(RandomPotion::exclude)
        ).apply(i, RandomPotion::new));

        /** Variante eines Grundtranks: {@code minecraft:long_swiftness} bzw. {@code minecraft:strong_swiftness}. */
        public static Identifier variant(Identifier family, boolean strong) {
            return Identifier.fromNamespaceAndPath(family.getNamespace(), (strong ? "strong_" : "long_") + family.getPath());
        }

        /**
         * Grundtrank und Modifikator eines Tranks aus der Tabelle, etwa {@code long_swiftness} → (swiftness, false).
         * Leer für Tränke ohne Modifikator oder außerhalb der Tabelle.
         */
        public Optional<Map.Entry<Identifier, Boolean>> parse(Identifier potion) {
            for (String prefix : List.of("long_", "strong_")) {
                if (potion.getPath().startsWith(prefix)) {
                    Identifier family = Identifier.fromNamespaceAndPath(potion.getNamespace(), potion.getPath().substring(prefix.length()));
                    if (potionValues.values().containsKey(family) && !exclude.contains(family)) {
                        return Optional.of(Map.entry(family, prefix.equals("strong_")));
                    }
                }
            }
            return Optional.empty();
        }

        /** Stammt das Angebot aus dieser Vorlage? Trank oder Wurftrank mit genau einem Trank aus der Tabelle. */
        public boolean produced(Contract contract) {
            if (contract.key().isPresent() || !(contract.item().equals(POTION) || contract.item().equals(SPLASH_POTION))) {
                return false;
            }
            return Contract.requiredPotion(contract.match())
                    .flatMap(holder -> holder.unwrapKey())
                    .flatMap(key -> parse(key.identifier()))
                    .isPresent();
        }

        /**
         * Zieht einen Grundtrank (gleichverteilt), dann Verlängerung oder Verstärkung, soweit es sie gibt, dann
         * Trank oder Wurftrank. Leer, wenn die Tabelle keinen brauchbaren Trank hergibt.
         */
        public Optional<RolledPotion> roll(HolderLookup.Provider registries, RandomSource random) {
            HolderLookup.RegistryLookup<Potion> lookup = registries.lookupOrThrow(Registries.POTION);
            List<List<Holder.Reference<Potion>>> families = new ArrayList<>();
            List<Identifier> ids = new ArrayList<>();
            potionValues.values().keySet().stream().sorted().filter(id -> !exclude.contains(id)).forEach(id -> {
                List<Holder.Reference<Potion>> variants = new ArrayList<>();
                for (boolean strong : new boolean[] {false, true}) {
                    lookup.get(ResourceKey.create(Registries.POTION, variant(id, strong))).ifPresent(variants::add);
                }
                if (variants.isEmpty()) {
                    Dtp.LOGGER.warn("Contract potion values: {} has no long_ or strong_ variant", id);
                } else {
                    families.add(variants);
                    ids.add(id);
                }
            });
            if (families.isEmpty()) {
                return Optional.empty();
            }
            int f = random.nextInt(families.size());
            List<Holder.Reference<Potion>> variants = families.get(f);
            Holder.Reference<Potion> potion = variants.get(random.nextInt(variants.size()));
            boolean strong = potion.key().identifier().getPath().startsWith("strong_");
            boolean splash = random.nextDouble() < splashChance;
            double value = potionValues.value(ids.get(f), strong, splash).orElseThrow();
            DataComponentMatchers match = DataComponentMatchers.Builder.components()
                    .exact(DataComponentExactPredicate.expect(DataComponents.POTION_CONTENTS, new PotionContents(potion)))
                    .build();
            return Optional.of(new RolledPotion(splash ? SPLASH_POTION : POTION, match, value));
        }
    }

    public record TemplateFile(Optional<EnchantmentValues> enchantments, Optional<PotionValues> potions, List<Template> templates) {
        public static final Codec<TemplateFile> CODEC = RecordCodecBuilder.create(i -> i.group(
                EnchantmentValues.CODEC.optionalFieldOf("enchantments").forGetter(TemplateFile::enchantments),
                PotionValues.CODEC.optionalFieldOf("potions").forGetter(TemplateFile::potions),
                Template.CODEC.listOf().optionalFieldOf("templates", List.of()).forGetter(TemplateFile::templates)
        ).apply(i, TemplateFile::new));
    }

    private static List<Template> templates = List.of();
    private static EnchantmentValues enchantmentValues = EnchantmentValues.EMPTY;
    private static PotionValues potionValues = PotionValues.EMPTY;

    public ContractTemplates() {
        super(TemplateFile.CODEC, FileToIdConverter.json("dtp/contract"));
    }

    @Override
    protected void apply(Map<Identifier, TemplateFile> files, ResourceManager manager, ProfilerFiller profiler) {
        List<Template> result = new ArrayList<>();
        Map<Identifier, Double> values = new java.util.HashMap<>();
        double factor = EnchantmentValues.EMPTY.belowMaxFactor();
        PotionValues potions = PotionValues.EMPTY;
        Map<Identifier, PotionValue> potionMap = new java.util.HashMap<>();
        for (Map.Entry<Identifier, TemplateFile> file : files.entrySet()) {
            file.getValue().templates().stream().filter(t -> valid(file.getKey(), t)).forEach(result::add);
            if (file.getValue().enchantments().isPresent()) {
                values.putAll(file.getValue().enchantments().get().values());
                factor = file.getValue().enchantments().get().belowMaxFactor();
            }
            if (file.getValue().potions().isPresent()) {
                potions = file.getValue().potions().get();
                potionMap.putAll(potions.values());
            }
        }
        templates = result;
        enchantmentValues = new EnchantmentValues(factor, Map.copyOf(values));
        potionValues = new PotionValues(potions.batch(), potions.awkward(), potions.bottle(), potions.longer(), potions.stronger(),
                potions.splash(), potions.rewardMultiplier(), potions.surcharge(), potions.maxCount(),
                Math.min(potions.cappedMin(), potions.maxCount()), Map.copyOf(potionMap));
        Dtp.LOGGER.info("Loaded {} contract templates, {} enchantment values and {} potion values", result.size(), values.size(),
                potionMap.size());
    }

    private static boolean valid(Identifier file, Template t) {
        if (!BuiltInRegistries.ITEM.containsKey(t.item())) {
            return false;
        }
        if (t.tiers().isEmpty()) {
            Dtp.LOGGER.warn("Contract templates {}: {} has no tier, skipped", file, t.item());
            return false;
        }
        if (t.minCount().isPresent() != t.maxCount().isPresent()) {
            Dtp.LOGGER.warn("Contract templates {}: {} needs both min_count and max_count or neither, skipped", file, t.item());
            return false;
        }
        if (t.randomEnchantments().isPresent() && t.rewardPerItem().isEmpty()) {
            Dtp.LOGGER.warn("Contract templates {}: {} with random_enchantments needs reward_per_item, skipped", file, t.item());
            return false;
        }
        if (t.randomPotion().isPresent() && t.randomEnchantments().isPresent()) {
            Dtp.LOGGER.warn("Contract templates {}: {} cannot have both random_potion and random_enchantments, skipped", file, t.item());
            return false;
        }
        return true;
    }

    public static List<Template> all() {
        return templates;
    }

    public static EnchantmentValues enchantmentValues() {
        return enchantmentValues;
    }

    public static PotionValues potionValues() {
        return potionValues;
    }
}
