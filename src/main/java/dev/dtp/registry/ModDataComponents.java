package dev.dtp.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.Unit;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.contraband.Charges;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Dtp.MODID);

    /** Die Loot-Table, die eine Schmuggelkiste beim Öffnen würfelt. Kommt aus dem Händlerangebot ({@code gives.components}). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceKey<LootTable>>> CRATE_LOOT = COMPONENTS.registerComponentType(
            "crate_loot", b -> b.persistent(LootTable.KEY_CODEC).networkSynchronized(ResourceKey.streamCodec(Registries.LOOT_TABLE)));

    /** Ladungen eines Contraband-Items ({@link Charges}) — bewusst keine Haltbarkeit. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Charges>> CHARGES = COMPONENTS.registerComponentType(
            "charges", b -> b.persistent(Charges.CODEC).networkSynchronized(Charges.STREAM_CODEC));

    /**
     * Das Item, an das eine Vanilla-Uhr über eine Marktwaage gebunden ist (Spieler-Upgrade Auftragsuhr). Beim Binden
     * kopiert, danach unabhängig von der Waage; ihr Tooltip zeigt den Marktpreis dieses Items.
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemStackTemplate>> WATCHED_ITEM = COMPONENTS.registerComponentType(
            "watched_item", b -> b.persistent(ItemStackTemplate.CODEC).networkSynchronized(ItemStackTemplate.STREAM_CODEC));

    /**
     * Händlerware: kam von einem Vanilla-Händler (Dorfbewohner, fahrender Händler, Geschenk an den Helden des Dorfes).
     * Zählt für keinen Contract, ist unverkäuflich und geht nicht in den Recycler ({@link dev.dtp.market.MerchantGoods}).
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> MERCHANT_GOODS = COMPONENTS.registerComponentType(
            "merchant_goods", b -> b.persistent(Unit.CODEC).networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));

    private ModDataComponents() {
    }

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
