package dev.dtp.market;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.advancements.predicates.DataComponentMatchers;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;

/**
 * Die kuratierte Handelsliste aus {@code data/<ns>/dtp/trade_list/*.json}.
 *
 * Vorrang: direkter Item-Eintrag schlägt Tag-Eintrag, ein kleinerer (spezifischerer) Tag schlägt einen größeren.
 * Ein Item ohne Eintrag ist nicht verkaufbar — das ist kein Fehler, sondern die sichere Voreinstellung.
 *
 * Die Reihenfolge der Dateien (nach Namen, z. B. {@code 10_agriculture.json} vor {@code 20_fishing.json}) und der
 * Einträge darin ist die Anzeigereihenfolge am Marktstand. Steht ein Item mehrfach, gilt der Wert der späteren
 * Datei, die Position der ersten.
 *
 * Einträge mit demselben {@code key} bilden eine <b>gemeinsame Ware</b>: Sie teilen sich einen Marktdruck und
 * einen Preisverlauf, behalten aber ihren eigenen Basiswert. H und T legt der erste Eintrag des Keys fest.
 * Ohne {@code key} ist der Schlüssel die Item-ID.
 *
 * Optional stellen {@code components} (exakter Wert) und {@code predicates} (Bedingung) im Vanilla-Format eine
 * Bedingung an den Stapel, etwa den festen Namen des Bösen Banners. Dann ist ein Stapel handelbar, wenn er sie
 * erfüllt; weitere Komponenten stören nicht. Ohne Bedingung bleibt jeder Stapel mit Komponenten unverkäuflich.
 * Je Item gibt es höchstens einen Eintrag — ein Item ohne die Komponenten (das schlichte weiße Banner) ist dann
 * nicht handelbar.
 */
public class TradeList extends SimpleJsonResourceReloadListener<TradeList.TradeFile> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "trade_list");

    /**
     * Ein Eintrag mit Basiswert, Halbwertsmenge H und Erholungs-Halbwertszeit T (in Minecraft-Tagen).
     * {@code key} ist die Ware, deren Marktdruck der Eintrag nutzt; leer = die Item-ID.
     * {@code match} ist die Bedingung an den Stapel; {@link DataComponentMatchers#ANY} = nur Stapel ohne Komponenten.
     */
    public record Entry(double base, Optional<Double> h, Optional<Double> t, Optional<Identifier> key, DataComponentMatchers match) {
        public double halfAmount() {
            return h.orElseGet(DtpConfig.DEFAULT_H::get);
        }

        public double recoveryDays() {
            return t.orElseGet(DtpConfig.DEFAULT_T::get);
        }
    }

    public record RawEntry(Optional<Identifier> item, Optional<Identifier> tag, Optional<Identifier> key, double base,
            Optional<Double> h, Optional<Double> t, DataComponentMatchers match) {
        public static final Codec<RawEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Identifier.CODEC.optionalFieldOf("item").forGetter(RawEntry::item),
                Identifier.CODEC.optionalFieldOf("tag").forGetter(RawEntry::tag),
                Identifier.CODEC.optionalFieldOf("key").forGetter(RawEntry::key),
                Codec.doubleRange(0.0, 1.0e12).fieldOf("base").forGetter(RawEntry::base),
                Codec.doubleRange(1.0, 1.0e12).optionalFieldOf("h").forGetter(RawEntry::h),
                Codec.doubleRange(0.001, 1.0e6).optionalFieldOf("t").forGetter(RawEntry::t),
                DataComponentMatchers.CODEC.forGetter(RawEntry::match)
        ).apply(i, RawEntry::new));
    }

    public record TradeFile(List<RawEntry> entries) {
        public static final Codec<TradeFile> CODEC = RecordCodecBuilder.create(i -> i.group(
                RawEntry.CODEC.listOf().fieldOf("entries").forGetter(TradeFile::entries)
        ).apply(i, TradeFile::new));
    }

    private record TagRule(TagKey<Item> tag, Entry entry) {
    }

    private static Map<Item, Entry> items = Map.of();
    private static List<TagRule> tags = List.of();
    /** Aufgelöste Einträge; wird bei jedem Reload und Tag-Update geleert. */
    private static final Map<Item, Optional<Entry>> RESOLVED = new ConcurrentHashMap<>();
    /** Anzeigereihenfolge aller verkaufbaren Items; {@code null} = bei nächster Abfrage neu bilden. */
    private static volatile List<Item> ordered;
    private static volatile Map<Item, Integer> positions;
    /** Mitglieder je Ware in Anzeigereihenfolge; {@code null} = bei nächster Abfrage neu bilden. */
    private static volatile Map<Identifier, List<Item>> members;

    public TradeList() {
        super(TradeFile.CODEC, FileToIdConverter.json("dtp/trade_list"));
    }

    @Override
    protected void apply(Map<Identifier, TradeFile> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, Entry> newItems = new LinkedHashMap<>();
        List<TagRule> newTags = new ArrayList<>();
        // Der erste Eintrag einer gemeinsamen Ware legt H und T fest — sonst erholte sich der Druck je nach Item anders.
        Map<Identifier, RawEntry> canon = new HashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            Identifier file = e.getKey();
            for (RawEntry raw : e.getValue().entries()) {
                Optional<Double> h = raw.h();
                Optional<Double> t = raw.t();
                if (raw.key().isPresent()) {
                    RawEntry first = canon.putIfAbsent(raw.key().get(), raw);
                    if (first != null) {
                        if (!first.h().equals(h) || !first.t().equals(t)) {
                            Dtp.LOGGER.warn("Trade list {}: key {} uses h/t {}/{} but its first entry {}/{}, using the first",
                                    file, raw.key().get(), h, t, first.h(), first.t());
                        }
                        h = first.h();
                        t = first.t();
                    }
                }
                Entry entry = new Entry(raw.base(), h, t, raw.key(), raw.match());
                if (raw.item().isPresent()) {
                    Optional<Holder.Reference<Item>> item = BuiltInRegistries.ITEM.get(raw.item().get());
                    // Fehlende Items (z. B. entfernter Mod) werden ersatzlos ignoriert.
                    item.ifPresentOrElse(ref -> newItems.put(ref.value(), entry),
                            () -> Dtp.LOGGER.debug("Trade list {}: unknown item {}, skipped", file, raw.item().get()));
                } else if (raw.tag().isPresent()) {
                    newTags.add(new TagRule(TagKey.create(Registries.ITEM, raw.tag().get()), entry));
                } else {
                    Dtp.LOGGER.warn("Trade list {}: entry without 'item' or 'tag', skipped", file);
                }
            }
        });
        items = newItems;
        tags = newTags;
        clearCaches();
        Dtp.LOGGER.info("Loaded trade list: {} items, {} tags", newItems.size(), newTags.size());
    }

    public static void onTagsUpdated() {
        clearCaches();
    }

    private static void clearCaches() {
        RESOLVED.clear();
        ordered = null;
        positions = null;
        members = null;
    }

    public static Optional<Entry> lookup(Item item) {
        return RESOLVED.computeIfAbsent(item, TradeList::resolve);
    }

    public static Optional<Entry> lookup(ItemStack stack) {
        if (stack.isEmpty() || MerchantGoods.isMarked(stack)) {
            return Optional.empty();
        }
        // Händlerware ist nie handelbar, auch nicht bei Einträgen mit Bedingung (die ignorieren weitere Komponenten).
        // Ohne Bedingung sind Items mit Zusatzdaten (gefüllte Behälter, Verzauberungen, Haltbarkeit) nie handelbar;
        // mit Bedingung zählt, ob der Stapel sie erfüllt.
        return lookup(stack.getItem()).filter(entry -> entry.match().isEmpty()
                ? stack.isComponentsPatchEmpty()
                : entry.match().test(stack));
    }

    /**
     * Wie die Ware am Marktstand erscheint: das Item mit den exakt geforderten Komponenten, beim Bösen Banner also
     * mit Muster und Namen. Ohne Bedingung das schlichte Item.
     */
    public static ItemStack displayStack(Item item) {
        ItemStack stack = new ItemStack(item);
        lookup(item).ifPresent(entry -> stack.applyComponents(entry.match().exact().asPatch()));
        return stack;
    }

    private static Optional<Entry> resolve(Item item) {
        Entry direct = items.get(item);
        if (direct != null) {
            return Optional.of(direct);
        }
        Holder<Item> holder = item.builtInRegistryHolder();
        return tags.stream()
                .filter(rule -> holder.is(rule.tag()))
                .min(Comparator.comparingInt(rule -> tagSize(rule.tag())))
                .map(TagRule::entry);
    }

    /** Die Ware, deren Marktdruck dieses Item nutzt: der {@code key} des Eintrags oder die Item-ID. */
    public static Optional<Identifier> marketKey(Item item) {
        return lookup(item).map(entry -> keyOf(item, entry));
    }

    static Identifier keyOf(Item item, Entry entry) {
        return entry.key().orElseGet(() -> BuiltInRegistries.ITEM.getKey(item));
    }

    /** Alle verkaufbaren Items einer Ware in Anzeigereihenfolge; leer, wenn es die Ware nicht gibt. */
    public static List<Item> members(Identifier key) {
        Map<Identifier, List<Item>> index = members;
        if (index == null) {
            Map<Identifier, List<Item>> built = new HashMap<>();
            for (Item item : allTradable()) {
                marketKey(item).ifPresent(k -> built.computeIfAbsent(k, x -> new ArrayList<>()).add(item));
            }
            built.replaceAll((k, v) -> List.copyOf(v));
            index = built;
            members = index;
        }
        return index.getOrDefault(key, List.of());
    }

    /** Teilt dieses Item seinen Marktdruck mit anderen Items? */
    public static boolean shared(Item item) {
        return marketKey(item).map(k -> members(k).size() > 1).orElse(false);
    }

    private static int tagSize(TagKey<Item> tag) {
        int size = 0;
        for (Holder<Item> ignored : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            size++;
        }
        return size;
    }

    /**
     * Alle verkaufbaren Items in Anzeigereihenfolge — für die Preisübersicht. Erst die direkten Einträge in der
     * Reihenfolge der Handelsliste, danach die nur über Tags erfassten Items, alphabetisch.
     */
    public static List<Item> allTradable() {
        List<Item> result = ordered;
        if (result == null) {
            result = new ArrayList<>(items.keySet());
            Set<Item> seen = new HashSet<>(result);
            List<Item> fromTags = new ArrayList<>();
            for (TagRule rule : tags) {
                for (Holder<Item> h : BuiltInRegistries.ITEM.getTagOrEmpty(rule.tag())) {
                    if (seen.add(h.value())) {
                        fromTags.add(h.value());
                    }
                }
            }
            fromTags.sort(Comparator.comparing(i -> BuiltInRegistries.ITEM.getKey(i).toString()));
            result.addAll(fromTags);
            result = List.copyOf(result);
            Map<Item, Integer> index = new HashMap<>();
            for (int i = 0; i < result.size(); i++) {
                index.put(result.get(i), i);
            }
            positions = index;
            ordered = result;
        }
        return result;
    }

    /** Position in der Anzeigereihenfolge; nicht verkaufbare Items ganz hinten. */
    public static int position(Item item) {
        allTradable();
        Map<Item, Integer> index = positions;
        return index == null ? Integer.MAX_VALUE : index.getOrDefault(item, Integer.MAX_VALUE);
    }
}
