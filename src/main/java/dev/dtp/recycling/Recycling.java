package dev.dtp.recycling;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import org.jspecify.annotations.Nullable;

import dev.dtp.Dtp;
import dev.dtp.contraband.ContrabandItem;
import dev.dtp.currency.Taler;
import dev.dtp.market.MerchantGoods;
import dev.dtp.market.TradeList;
import dev.dtp.registry.ModItems;

/**
 * Was der Recycler annimmt, aus {@code data/<ns>/dtp/recycling/*.json}: je Item ein Schrottwert, meist 5 % des
 * Basiswerts seiner Rohstoffe (die Preisuntergrenze des Markts). Recyceln bringt so nie mehr als der Verkauf der
 * Rohstoffe bei voller Sättigung.
 *
 * <p>Nie recycelbar, auch wenn ein Datapack es einträgt: Waren der Handelsliste (die gehören an den Marktstand),
 * Taler, Contraband, Schrott selbst und alles, was ein Ofen oder Schmelzofen schon verwertet (Eisen-, Gold-, Kupfer-
 * und Kettenausrüstung schmelzen zu Nuggets).
 */
public class Recycling extends SimpleJsonResourceReloadListener<Recycling.RecyclingFile> {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Dtp.MODID, "recycling");

    /**
     * Die Item-ID bleibt beim Einlesen ein String: Eine ungültige ID (Tippfehler wie {@code minecraft:minecraft:x})
     * ließe sonst den Codec scheitern, und Minecraft verwürfe die ganze Datei. So kostet sie nur ihren eigenen Eintrag.
     */
    public record RawEntry(String item, double scrap) {
        public static final Codec<RawEntry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("item").forGetter(RawEntry::item),
                Codec.doubleRange(0.0, 1.0e6).fieldOf("scrap").forGetter(RawEntry::scrap)
        ).apply(i, RawEntry::new));
    }

    public record RecyclingFile(List<RawEntry> entries) {
        public static final Codec<RecyclingFile> CODEC = RecordCodecBuilder.create(i -> i.group(
                RawEntry.CODEC.listOf().fieldOf("entries").forGetter(RecyclingFile::entries)
        ).apply(i, RecyclingFile::new));
    }

    private static Map<Item, Double> values = Map.of();
    /** Zutaten von Ofen- und Schmelzofenrezepten; {@code null} = beim nächsten Zugriff neu sammeln. */
    private static volatile @Nullable Set<Item> smeltable;

    public Recycling() {
        super(RecyclingFile.CODEC, FileToIdConverter.json("dtp/recycling"));
    }

    @Override
    protected void apply(Map<Identifier, RecyclingFile> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, Double> result = new LinkedHashMap<>();
        files.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            for (RawEntry raw : e.getValue().entries()) {
                Identifier id = Identifier.tryParse(raw.item());
                if (id == null) {
                    Dtp.LOGGER.warn("Recycling {}: invalid item id '{}', skipped", e.getKey(), raw.item());
                    continue;
                }
                BuiltInRegistries.ITEM.getOptional(id).ifPresentOrElse(item -> {
                    // Ein späterer Eintrag mit 0 nimmt das Item wieder heraus (Datapack-Überschreibung).
                    if (raw.scrap() > 0.0) {
                        result.put(item, raw.scrap());
                    } else {
                        result.remove(item);
                    }
                }, () -> Dtp.LOGGER.warn("Recycling {}: unknown item {}, skipped", e.getKey(), id));
            }
        });
        values = result;
        smeltable = null;
        Dtp.LOGGER.info("Loaded recycling list: {} items", result.size());
    }

    /** Nach einem Reload der Rezepte oder Tags neu prüfen. */
    public static void invalidate() {
        smeltable = null;
    }

    /** Die eingetragenen Items mit ihrem Schrottwert, ungefiltert — für Tests. */
    public static Map<Item, Double> entries() {
        return values;
    }

    /**
     * Schrottwert eines unbeschädigten Items, wenn der Recycler es annimmt. Braucht den Server für die Ofenrezepte;
     * ohne Server (Client) gilt nur die Datenliste.
     */
    public static Optional<Double> value(@Nullable MinecraftServer server, ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        Item item = stack.getItem();
        Double value = values.get(item);
        if (value == null || excluded(stack)) {
            return Optional.empty();
        }
        if (server != null && smeltable(server).contains(item)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public static boolean accepts(@Nullable MinecraftServer server, ItemStack stack) {
        return value(server, stack).isPresent();
    }

    /** Waren, Währung, Contraband, Schrott, Händlerware und Behälter mit Inhalt nimmt der Recycler nie an. */
    private static boolean excluded(ItemStack stack) {
        return Taler.isTaler(stack)
                || stack.getItem() instanceof ContrabandItem
                || stack.is(ModItems.SCRAP.get())
                || MerchantGoods.isMarked(stack)
                || TradeList.lookup(stack.getItem()).isPresent()
                || stack.has(DataComponents.CONTAINER)
                || stack.has(DataComponents.BUNDLE_CONTENTS);
    }

    /**
     * Schrott für einen Stapel: Wert × Stückzahl × verbleibende Haltbarkeit. Der Bruchteil ist die Chance auf ein
     * weiteres Stück, so stimmt der Erwartungswert genau — ein Steinschwert (0,12) gibt in 12 % der Fälle 1 Schrott.
     */
    public static int scrapFor(@Nullable MinecraftServer server, ItemStack stack, RandomSource random) {
        Optional<Double> value = value(server, stack);
        if (value.isEmpty()) {
            return 0;
        }
        double amount = value.get() * stack.getCount() * condition(stack);
        int whole = (int) Math.floor(amount);
        return whole + (random.nextDouble() < amount - whole ? 1 : 0);
    }

    /** Anteil der verbleibenden Haltbarkeit, 1 bei Items ohne Haltbarkeit. */
    public static double condition(ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return 1.0;
        }
        return Math.clamp(1.0 - (double) stack.getDamageValue() / stack.getMaxDamage(), 0.0, 1.0);
    }

    private static Set<Item> smeltable(MinecraftServer server) {
        Set<Item> result = smeltable;
        if (result == null) {
            Set<Item> built = new HashSet<>();
            server.getRecipeManager().getRecipes().forEach(holder -> {
                if (holder.value() instanceof AbstractCookingRecipe cooking) {
                    for (Item item : values.keySet()) {
                        if (cooking.input().test(new ItemStack(item))) {
                            if (built.add(item)) {
                                Dtp.LOGGER.warn("Recycling: {} can be smelted ({}), not recyclable",
                                        BuiltInRegistries.ITEM.getKey(item), holder.id().identifier());
                            }
                        }
                    }
                }
            });
            result = Set.copyOf(built);
            smeltable = result;
        }
        return result;
    }
}
