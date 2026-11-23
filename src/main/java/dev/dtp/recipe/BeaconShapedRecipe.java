package dev.dtp.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import dev.dtp.upgrade.WorldProgress;

/**
 * Geformtes Rezept für den Hauptbeacon: Es passt nur, solange auf dem Server noch kein Beacon steht — auch kein
 * gesetzter, noch unbestätigter. So beginnt jeder Durchlauf mit einem günstigen Rezept, und nach dem Start
 * gibt es keinen zweiten Beacon mehr.
 */
public class BeaconShapedRecipe extends ConditionalShapedRecipe {
    public static final MapCodec<BeaconShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result))
            .apply(i, BeaconShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeaconShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            BeaconShapedRecipe::new);

    public static final RecipeSerializer<BeaconShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    public BeaconShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate result) {
        super(commonInfo, bookInfo, pattern, result);
    }

    @Override
    protected boolean allowed(MinecraftServer server) {
        return WorldProgress.get(server).beaconPos() == null;
    }

    @Override
    public RecipeSerializer<BeaconShapedRecipe> getSerializer() {
        return SERIALIZER;
    }
}
