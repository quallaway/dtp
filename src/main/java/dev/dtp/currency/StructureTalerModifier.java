package dev.dtp.currency;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import dev.dtp.Dtp;

/**
 * Taler in Strukturkisten: Jede Loot-Table mit Pfad {@code chests/…} (jeder Namespace außer {@code dtp}) gehört zu einer
 * Stufe und bekommt mit deren {@code chance} einen Betrag zwischen {@code min} und {@code max}, ausgezahlt in möglichst
 * wenigen Münzen ({@link Taler#split}). Mehr als ein Silberbeutel je Kiste ist nicht erlaubt. Unbekannte Kisten-Tabellen
 * (andere Mods) fallen in {@code default_tier}, Pfad-Präfixe aus {@code exclude} bleiben Vanilla.
 * <p>
 * Unabhängig vom Schwarzmarktfund ({@code dev.dtp.blackmarket.StructureFindsModifier}). Rucksack-Mobs (Sophisticated
 * Backpacks) würfeln Kisten-Tabellen und bekommen damit Taler nach der Stufe ihrer Tabelle, das ist gewollt.
 */
public class StructureTalerModifier extends LootModifier {
    /** Höchstbetrag je Kiste: ein Silberbeutel. */
    public static final long MAX_AMOUNT = Taler.SILVER_POUCH.value;

    private static final String CHESTS = "chests/";

    /** Eine Stufe. Einträge in {@code tables} auf {@code /} oder {@code *} gelten als Präfix, sonst als volle ID. */
    public record Tier(String name, double chance, int min, int max, List<String> tables) {
        private static final Codec<Tier> RAW = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Tier::name),
                Codec.doubleRange(0, 1).fieldOf("chance").forGetter(Tier::chance),
                Codec.intRange(1, (int) MAX_AMOUNT).fieldOf("min").forGetter(Tier::min),
                Codec.intRange(1, (int) MAX_AMOUNT).fieldOf("max").forGetter(Tier::max),
                Codec.STRING.listOf().optionalFieldOf("tables", List.of()).forGetter(Tier::tables))
                .apply(i, Tier::new));

        public static final Codec<Tier> CODEC = RAW.validate(t -> t.min() <= t.max() ? DataResult.success(t)
                : DataResult.error(() -> "Taler-Stufe '" + t.name() + "': min " + t.min() + " ist größer als max " + t.max()));

        boolean matches(String table) {
            for (String entry : tables) {
                if (entry.endsWith("*") ? table.startsWith(entry.substring(0, entry.length() - 1))
                        : entry.endsWith("/") ? table.startsWith(entry) : table.equals(entry)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static final MapCodec<StructureTalerModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).and(i.group(
            Tier.CODEC.listOf().fieldOf("tiers").forGetter(m -> m.tiers),
            Codec.STRING.optionalFieldOf("default_tier", "").forGetter(m -> m.defaultTier),
            Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(m -> m.exclude)))
            .apply(i, StructureTalerModifier::new));

    private final List<Tier> tiers;
    private final String defaultTier;
    private final List<String> exclude;

    public StructureTalerModifier(Optional<Holder<LootItemCondition>> condition, int priority, List<Tier> tiers,
            String defaultTier, List<String> exclude) {
        super(condition, priority);
        this.tiers = List.copyOf(tiers);
        this.defaultTier = defaultTier;
        this.exclude = List.copyOf(exclude);
    }

    /** Die Stufe einer Loot-Table, leer, wenn sie keine Strukturkiste ist oder ausgenommen. */
    public Optional<Tier> tierOf(Identifier table) {
        if (table == null || table.getNamespace().equals(Dtp.MODID) || !table.getPath().startsWith(CHESTS)) {
            return Optional.empty();
        }
        for (String prefix : exclude) {
            if (table.getPath().startsWith(prefix)) {
                return Optional.empty();
            }
        }
        String id = table.toString();
        for (Tier tier : tiers) {
            if (tier.matches(id)) {
                return Optional.of(tier);
            }
        }
        return tiers.stream().filter(t -> t.name().equals(defaultTier)).findFirst();
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        Optional<Tier> tier = tierOf(context.getQueriedLootTableId());
        if (tier.isEmpty() || context.getRandom().nextDouble() >= tier.get().chance()) {
            return loot;
        }
        int amount = Mth.randomBetweenInclusive(context.getRandom(), tier.get().min(), tier.get().max());
        loot.addAll(Taler.split(amount));
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
