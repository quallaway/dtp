package dev.dtp.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.recipe.BeaconShapedRecipe;
import dev.dtp.recipe.MerchantRepairItemRecipe;
import dev.dtp.recipe.UpgradeShapedRecipe;

public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Dtp.MODID);

    /** {@code dtp:upgrade_shaped}: geformtes Rezept, das erst nach einem Server-Upgrade passt. */
    public static final Supplier<RecipeSerializer<UpgradeShapedRecipe>> UPGRADE_SHAPED = SERIALIZERS.register("upgrade_shaped",
            () -> UpgradeShapedRecipe.SERIALIZER);

    /** {@code dtp:beacon_shaped}: geformtes Rezept, das nur passt, solange noch kein Hauptbeacon steht. */
    public static final Supplier<RecipeSerializer<BeaconShapedRecipe>> BEACON_SHAPED = SERIALIZERS.register("beacon_shaped",
            () -> BeaconShapedRecipe.SERIALIZER);

    /** {@code dtp:repair_item}: Vanilla-Reparatur im Grid, die die Markierung als Händlerware weitergibt. */
    public static final Supplier<RecipeSerializer<RepairItemRecipe>> REPAIR_ITEM = SERIALIZERS.register("repair_item",
            () -> MerchantRepairItemRecipe.SERIALIZER);

    private ModRecipes() {
    }

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
