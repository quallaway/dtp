package dev.dtp.recipe;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RepairItemRecipe;

import dev.dtp.market.MerchantGoods;

/**
 * {@code dtp:repair_item}: Vanillas Reparatur im Crafting-Grid (zwei gleiche Items ergeben eines), ersetzt per Datapack
 * unter {@code minecraft:repair_item}. Vanilla baut das Ergebnis mit {@code new ItemStack} neu und verwirft dabei alle
 * Komponenten — auch die Markierung als {@link MerchantGoods Händlerware}. Diese Unterklasse markiert das Ergebnis,
 * sobald eine der beiden Zutaten Händlerware ist. Bei Minecraft-Updates gegen {@link RepairItemRecipe} prüfen.
 */
public class MerchantRepairItemRecipe extends RepairItemRecipe {
    public static final MerchantRepairItemRecipe INSTANCE = new MerchantRepairItemRecipe();
    public static final MapCodec<RepairItemRecipe> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final StreamCodec<RegistryFriendlyByteBuf, RepairItemRecipe> STREAM_CODEC = StreamCodec.unit(INSTANCE);
    public static final RecipeSerializer<RepairItemRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack result = super.assemble(input);
        if (input.items().stream().anyMatch(MerchantGoods::isMarked)) {
            MerchantGoods.mark(result);
        }
        return result;
    }

    @Override
    public RecipeSerializer<RepairItemRecipe> getSerializer() {
        return SERIALIZER;
    }
}
