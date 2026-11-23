package dev.dtp.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.blackmarket.BlackmarketTrader;

public final class ModEntities {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(Dtp.MODID);

    /** Maße wie beim Vanilla-Händler. Bewusst ohne Spawn-Ei: Er erscheint nur über die Spawnbedingung oder per Admin-Befehl. */
    public static final DeferredHolder<EntityType<?>, EntityType<BlackmarketTrader>> BLACKMARKET_TRADER = ENTITIES.registerEntityType(
            "blackmarket_trader", BlackmarketTrader::new, MobCategory.CREATURE,
            b -> b.sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10));

    private ModEntities() {
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(BLACKMARKET_TRADER.get(), Mob.createMobAttributes().build());
    }
}
