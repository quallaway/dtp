package dev.dtp.market;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Markiert Loot als {@link MerchantGoods Händlerware}, wenn die Tabelle mit einem der Präfixe aus {@code tables}
 * beginnt (vollständige ID, z. B. {@code minecraft:gameplay/hero_of_the_village/}). So zählen die Geschenke an den
 * Helden des Dorfes — Brot, Kürbiskuchen, Kekse, Ausrüstung — wie gekaufte Händlerware.
 */
public class MerchantGoodsModifier extends LootModifier {
    public static final MapCodec<MerchantGoodsModifier> CODEC = RecordCodecBuilder.mapCodec(i -> codecStart(i).and(
            Codec.STRING.listOf().fieldOf("tables").forGetter(m -> m.tables))
            .apply(i, MerchantGoodsModifier::new));

    private final List<String> tables;

    public MerchantGoodsModifier(Optional<Holder<LootItemCondition>> condition, int priority, List<String> tables) {
        super(condition, priority);
        this.tables = List.copyOf(tables);
    }

    public boolean appliesTo(Identifier table) {
        if (table == null) {
            return false;
        }
        String id = table.toString();
        return tables.stream().anyMatch(id::startsWith);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (appliesTo(context.getQueriedLootTableId())) {
            loot.forEach(MerchantGoods::mark);
        }
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
