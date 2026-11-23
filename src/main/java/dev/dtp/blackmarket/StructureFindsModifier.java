package dev.dtp.blackmarket;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.contraband.ContrabandDefinitions;
import dev.dtp.contraband.ContrabandItem;

/**
 * Schwarzmarktfunde in Strukturkisten: Jede Loot-Table mit Pfad {@code chests/…} (jeder Namespace außer {@code dtp})
 * bekommt mit {@code chance} zusätzlich einen Wurf aus {@code dtp:gameplay/blackmarket_finds} — Contraband und die
 * Verbrauchsgegenstände des Händlers. Pfad-Präfixe aus {@code exclude} (Bonustruhe, Dörfer, Tresore, Dispenser) bleiben
 * Vanilla. Contraband liegt teilgeladen in der Kiste, zwischen {@code min_charge_fraction} und
 * {@code max_charge_fraction} des Höchstwerts aus der Datapack-Definition.
 * <p>
 * Rucksack-Mobs (Sophisticated Backpacks) würfeln dieselben Kisten-Tabellen und können damit auch Funde tragen, das ist
 * gewollt. Der Angelschatz bekommt keine Funde: Die Angel-Tabelle liegt nicht unter {@code chests/}, und seine Fundliste
 * wird ohne Loot-Modifier gewürfelt.
 */
public class StructureFindsModifier extends LootModifier {
    public static final MapCodec<StructureFindsModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).and(i.group(
            Codec.doubleRange(0, 1).optionalFieldOf("chance", 0.2).forGetter(m -> m.chance),
            Codec.doubleRange(0, 1).optionalFieldOf("min_charge_fraction", 0.25).forGetter(m -> m.minChargeFraction),
            Codec.doubleRange(0, 1).optionalFieldOf("max_charge_fraction", 0.75).forGetter(m -> m.maxChargeFraction),
            Codec.STRING.listOf().optionalFieldOf("exclude", List.of()).forGetter(m -> m.exclude)))
            .apply(i, StructureFindsModifier::new));

    public static final ResourceKey<LootTable> TABLE = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(Dtp.MODID, "gameplay/blackmarket_finds"));

    private static final String CHESTS = "chests/";

    private final double chance;
    private final double minChargeFraction;
    private final double maxChargeFraction;
    private final List<String> exclude;

    public StructureFindsModifier(Optional<Holder<LootItemCondition>> condition, int priority, double chance,
            double minChargeFraction, double maxChargeFraction, List<String> exclude) {
        super(condition, priority);
        this.chance = chance;
        this.minChargeFraction = Math.min(minChargeFraction, maxChargeFraction);
        this.maxChargeFraction = Math.max(minChargeFraction, maxChargeFraction);
        this.exclude = List.copyOf(exclude);
    }

    /** Ob die Tabelle eine Strukturkiste ist, in der Funde liegen dürfen. */
    public boolean appliesTo(Identifier table) {
        if (table == null || table.getNamespace().equals(Dtp.MODID) || !table.getPath().startsWith(CHESTS)) {
            return false;
        }
        for (String prefix : exclude) {
            if (table.getPath().startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!DtpConfig.CONTRABAND_ENABLED.get() || !appliesTo(context.getQueriedLootTableId())
                || context.getRandom().nextDouble() >= chance) {
            return loot;
        }
        LootTable finds = context.getLevel().getServer().reloadableRegistries().getLootTable(TABLE);
        finds.getRandomItemsRaw(context, stack -> loot.add(partlyCharged(stack, context)));
        return loot;
    }

    /** Contraband bekommt eine zufällige Ladung im eingestellten Bereich, alles andere bleibt, wie es ist. */
    private ItemStack partlyCharged(ItemStack stack, LootContext context) {
        if (!(stack.getItem() instanceof ContrabandItem item)) {
            return stack;
        }
        int max = ContrabandDefinitions.get(item).maxCharges();
        int low = Mth.floor(minChargeFraction * max);
        int high = Mth.floor(maxChargeFraction * max);
        return ContrabandItem.create(item, Mth.randomBetweenInclusive(context.getRandom(), low, high));
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
