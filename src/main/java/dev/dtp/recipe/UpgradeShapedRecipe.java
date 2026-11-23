package dev.dtp.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/**
 * Geformtes Rezept, das erst nach einem Server-Upgrade passt — an der Werkbank wie im Crafter.
 * Aufbau wie {@code minecraft:crafting_shaped}, dazu das Feld {@code "upgrade"} mit der Upgrade-ID.
 */
public class UpgradeShapedRecipe extends ConditionalShapedRecipe {
    private static final Codec<ServerUpgrade> UPGRADE_CODEC = Codec.STRING.comapFlatMap(
            id -> {
                ServerUpgrade upgrade = ServerUpgrade.byId(id);
                return upgrade != null ? DataResult.success(upgrade) : DataResult.error(() -> "Unknown server upgrade: " + id);
            },
            upgrade -> upgrade.id);

    public static final MapCodec<UpgradeShapedRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ShapedRecipePattern.MAP_CODEC.forGetter(r -> r.pattern),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
            UPGRADE_CODEC.fieldOf("upgrade").forGetter(r -> r.upgrade))
            .apply(i, UpgradeShapedRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpgradeShapedRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ShapedRecipePattern.STREAM_CODEC, r -> r.pattern,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            ByteBufCodecs.STRING_UTF8.map(ServerUpgrade::byId, u -> u.id), r -> r.upgrade,
            UpgradeShapedRecipe::new);

    public static final RecipeSerializer<UpgradeShapedRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ServerUpgrade upgrade;

    public UpgradeShapedRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ShapedRecipePattern pattern,
            ItemStackTemplate result, ServerUpgrade upgrade) {
        super(commonInfo, bookInfo, pattern, result);
        this.upgrade = upgrade;
    }

    public ServerUpgrade upgrade() {
        return upgrade;
    }

    @Override
    protected boolean allowed(MinecraftServer server) {
        return WorldProgress.get(server).level(upgrade) > 0;
    }

    @Override
    public RecipeSerializer<UpgradeShapedRecipe> getSerializer() {
        return SERIALIZER;
    }
}
