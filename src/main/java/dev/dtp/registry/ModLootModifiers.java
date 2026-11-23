package dev.dtp.registry;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import dev.dtp.Dtp;
import dev.dtp.blackmarket.StructureFindsModifier;
import dev.dtp.currency.StructureTalerModifier;
import dev.dtp.market.MerchantGoodsModifier;
import dev.dtp.upgrade.FishingTreasureModifier;

/** Global Loot Modifier des Mods. Die Daten liegen unter {@code data/dtp/loot_modifiers/}. */
public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Dtp.MODID);

    public static final Supplier<MapCodec<FishingTreasureModifier>> FISHING_TREASURE =
            SERIALIZERS.register("fishing_treasure", () -> FishingTreasureModifier.CODEC);

    public static final Supplier<MapCodec<StructureFindsModifier>> STRUCTURE_FINDS =
            SERIALIZERS.register("structure_finds", () -> StructureFindsModifier.CODEC);

    public static final Supplier<MapCodec<StructureTalerModifier>> STRUCTURE_TALER =
            SERIALIZERS.register("structure_taler", () -> StructureTalerModifier.CODEC);

    public static final Supplier<MapCodec<MerchantGoodsModifier>> MERCHANT_GOODS =
            SERIALIZERS.register("merchant_goods", () -> MerchantGoodsModifier.CODEC);

    private ModLootModifiers() {
    }

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }
}
