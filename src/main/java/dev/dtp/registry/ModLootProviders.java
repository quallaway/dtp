package dev.dtp.registry;

import java.util.function.Supplier;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProvider;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.beacon.CompostChance;
import dev.dtp.upgrade.ServerUpgradeValue;

public final class ModLootProviders {
    public static final DeferredRegister<MapCodec<? extends ContextFloatProvider>> FLOAT_PROVIDERS =
            DeferredRegister.create(Registries.CONTEXT_FLOAT_PROVIDER_TYPE, Dtp.MODID);

    /** {@code dtp:server_upgrade}: Wert je Stufe eines Server-Upgrades, z. B. für das Ofentempo. */
    public static final Supplier<MapCodec<ServerUpgradeValue>> SERVER_UPGRADE = FLOAT_PROVIDERS.register("server_upgrade",
            () -> ServerUpgradeValue.MAP_CODEC);

    public static final DeferredRegister<MapCodec<? extends ContextIntProvider>> INT_PROVIDERS =
            DeferredRegister.create(Registries.CONTEXT_INT_PROVIDER_TYPE, Dtp.MODID);

    /** {@code dtp:compost_chance}: Kompost-Chance mit Bonus durch den Beacon-Effekt Landwirtschaft. */
    public static final Supplier<MapCodec<CompostChance>> COMPOST_CHANCE = INT_PROVIDERS.register("compost_chance",
            () -> CompostChance.MAP_CODEC);

    private ModLootProviders() {
    }

    public static void register(IEventBus bus) {
        FLOAT_PROVIDERS.register(bus);
        INT_PROVIDERS.register(bus);
    }
}
