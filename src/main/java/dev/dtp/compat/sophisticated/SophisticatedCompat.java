package dev.dtp.compat.sophisticated;

import net.minecraft.world.Container;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModBlocks;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.upgrades.IUpgradeWrapper;

import dev.dtp.Dtp;
import dev.dtp.data.DtpStorage;
import dev.dtp.registry.ModItems;

/**
 * Anbindung an Sophisticated Backpacks. Nur geladen, wenn der Mod da ist ({@link dev.dtp.compat.Compat}).
 *
 * <p>Zwei Rucksack-Upgrades machen einen <b>platzierten</b> Rucksack zur Trade-Chest bzw. Einzahlkiste. Er bietet dann
 * die passende {@link DtpStorage}-Capability an, und Marktstand, Bulletin-Board, Werkstatt und Beacon finden ihn wie
 * eine Kiste. Ein getragener Rucksack zählt nie — es bleibt dabei, dass nie aus dem Inventar bezahlt wird.
 *
 * <p>Ohne Sophisticated Backpacks gibt es die Items nicht: nicht registriert, kein Rezept (Condition
 * {@code neoforge:mod_loaded}), kein Eintrag im Creative-Tab oder in JEI.
 */
public final class SophisticatedCompat {
    private SophisticatedCompat() {
    }

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dtp.MODID);

    public static final DeferredItem<StorageUpgradeItem> TRADE_CHEST_UPGRADE = ITEMS.registerItem("trade_chest_upgrade",
            p -> new StorageUpgradeItem(p, StorageUpgradeItem.Kind.TRADE));
    public static final DeferredItem<StorageUpgradeItem> DEPOSIT_CHEST_UPGRADE = ITEMS.registerItem("deposit_chest_upgrade",
            p -> new StorageUpgradeItem(p, StorageUpgradeItem.Kind.DEPOSIT));

    public static void init(IEventBus modBus) {
        ITEMS.register(modBus);
        modBus.addListener(SophisticatedCompat::registerCapabilities);
        modBus.addListener(SophisticatedCompat::addToCreativeTab);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(DtpStorage.TRADE, ModBlocks.BACKPACK_TILE_TYPE.get(),
                (backpack, context) -> storage(backpack, StorageUpgradeItem.Kind.TRADE));
        event.registerBlockEntity(DtpStorage.DEPOSIT, ModBlocks.BACKPACK_TILE_TYPE.get(),
                (backpack, context) -> storage(backpack, StorageUpgradeItem.Kind.DEPOSIT));
    }

    /** Das Inventar des Rucksacks, wenn das Upgrade eingesetzt und eingeschaltet ist, sonst {@code null}. */
    private static Container storage(BackpackBlockEntity backpack, StorageUpgradeItem.Kind kind) {
        IStorageWrapper wrapper = backpack.getStorageWrapper();
        boolean active = wrapper.getUpgradeHandler().getTypeWrappers(kind.type).stream().anyMatch(IUpgradeWrapper::isEnabled);
        return active ? new BackpackContainer(wrapper.getInventoryHandler(), kind == StorageUpgradeItem.Kind.DEPOSIT) : null;
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTab() == ModItems.TAB.get()) {
            event.accept(TRADE_CHEST_UPGRADE.get());
            event.accept(DEPOSIT_CHEST_UPGRADE.get());
        }
    }
}
