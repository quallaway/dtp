package dev.dtp.registry;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.blackmarket.ContrabandCrateItem;
import dev.dtp.currency.Taler;
import dev.dtp.guide.GuideItem;
import dev.dtp.contraband.EndlessTorchItem;
import dev.dtp.contraband.FireExtinguisherItem;
import dev.dtp.contraband.GunItem;
import dev.dtp.contraband.OozingRodItem;
import dev.dtp.contraband.PocketMoneyPouchItem;
import dev.dtp.contraband.SpawnWandItem;
import dev.dtp.contraband.WatchBellItem;
import dev.dtp.supplies.AmethystCatalystItem;
import dev.dtp.supplies.RainCatalystItem;
import dev.dtp.supplies.ReturnScrollItem;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Dtp.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dtp.MODID);

    public static final Map<Taler, DeferredItem<Item>> TALER = new EnumMap<>(Taler.class);

    static {
        for (Taler t : Taler.values()) {
            Rarity rarity = t.ordinal() >= 6 ? Rarity.RARE : t.ordinal() >= 3 ? Rarity.UNCOMMON : Rarity.COMMON;
            TALER.put(t, ITEMS.registerSimpleItem(t.id, p -> p.rarity(rarity)));
        }
    }

    public static final DeferredItem<BlockItem> MARKET_STAND = ITEMS.registerSimpleBlockItem(ModBlocks.MARKET_STAND);
    public static final DeferredItem<BlockItem> TRADE_CHEST = ITEMS.registerSimpleBlockItem(ModBlocks.TRADE_CHEST);
    public static final DeferredItem<BlockItem> BULLETIN_BOARD = ITEMS.registerSimpleBlockItem(ModBlocks.BULLETIN_BOARD);
    public static final DeferredItem<BlockItem> STATISTICS_BOARD = ITEMS.registerSimpleBlockItem(ModBlocks.STATISTICS_BOARD);
    public static final DeferredItem<BlockItem> UPGRADE_TABLE = ITEMS.registerSimpleBlockItem(ModBlocks.UPGRADE_TABLE);
    public static final DeferredItem<BlockItem> REPAIR_STATION = ITEMS.registerSimpleBlockItem(ModBlocks.REPAIR_STATION);
    public static final DeferredItem<BlockItem> BLACKMARKET = ITEMS.registerSimpleBlockItem(ModBlocks.BLACKMARKET, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> SLOT_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.SLOT_MACHINE, p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> DEPOSIT_CHEST = ITEMS.registerSimpleBlockItem(ModBlocks.DEPOSIT_CHEST);
    public static final DeferredItem<BlockItem> MARKET_SCALE = ITEMS.registerSimpleBlockItem(ModBlocks.MARKET_SCALE);
    public static final DeferredItem<BlockItem> RECYCLER = ITEMS.registerSimpleBlockItem(ModBlocks.RECYCLER);
    public static final DeferredItem<BlockItem> BEACON = ITEMS.registerSimpleBlockItem(ModBlocks.BEACON, p -> p.rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> RELAY_BEACON = ITEMS.registerSimpleBlockItem(ModBlocks.RELAY_BEACON, p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> CHUNK_LOADER = ITEMS.registerSimpleBlockItem(ModBlocks.CHUNK_LOADER, p -> p.rarity(Rarity.RARE));

    /** Schrott aus dem Recycler: eine Ware der Handelsliste, verkauft am Marktstand. */
    public static final DeferredItem<Item> SCRAP = ITEMS.registerSimpleItem("scrap");

    /** Das DTP-Handbuch: Einführung für neue Spieler, beim ersten Betreten im Inventar. */
    public static final DeferredItem<GuideItem> GUIDE = ITEMS.registerItem("guide", GuideItem::new,
            p -> p.stacksTo(1).rarity(Rarity.UNCOMMON));

    /** Schmuggelkiste vom Schwarzmarkthändler; die Sorte steckt in der Komponente {@code dtp:crate_loot}. */
    public static final DeferredItem<ContrabandCrateItem> CONTRABAND_CRATE = ITEMS.registerItem("contraband_crate", ContrabandCrateItem::new,
            p -> p.rarity(Rarity.UNCOMMON));

    // --- Contraband-Items (Todo v4, Punkt 6): Ladungen statt Haltbarkeit, Aufladen mit Talern an der Reparaturbank ---

    public static final DeferredItem<FireExtinguisherItem> FIRE_EXTINGUISHER = ITEMS.registerItem("fire_extinguisher", FireExtinguisherItem::new,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<WatchBellItem> WATCH_BELL = ITEMS.registerItem("watch_bell", WatchBellItem::new,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<OozingRodItem> OOZING_ROD = ITEMS.registerItem("oozing_rod", OozingRodItem::new,
            p -> p.rarity(Rarity.UNCOMMON));

    static {
        // Bis 4.0.0 hieß die Schleimrute dtp:dowsing_rod; der Alias erhält vorhandene Ruten in bestehenden Welten.
        ITEMS.addAlias(Identifier.fromNamespaceAndPath(Dtp.MODID, "dowsing_rod"), OOZING_ROD.getId());
    }
    public static final DeferredItem<SpawnWandItem> SPAWN_WAND = ITEMS.registerItem("spawn_wand", SpawnWandItem::new,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<PocketMoneyPouchItem> POCKET_MONEY_POUCH = ITEMS.registerItem("pocket_money_pouch", PocketMoneyPouchItem::new,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<GunItem> GUN = ITEMS.registerItem("gun", GunItem::new,
            p -> p.rarity(Rarity.RARE));
    public static final DeferredItem<EndlessTorchItem> ENDLESS_TORCH = ITEMS.registerItem("endless_torch", EndlessTorchItem::new,
            p -> p.rarity(Rarity.UNCOMMON));

    // --- Verbrauchsgegenstände vom Händler (kein Contraband): stapelbar, ein Erfolg verbraucht ein Stück ---

    public static final DeferredItem<AmethystCatalystItem> AMETHYST_CATALYST = ITEMS.registerItem("amethyst_catalyst", AmethystCatalystItem::new,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<RainCatalystItem> RAIN_CATALYST = ITEMS.registerItem("rain_catalyst", RainCatalystItem::new,
            p -> p.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<ReturnScrollItem> RETURN_SCROLL = ITEMS.registerItem("return_scroll", ReturnScrollItem::new,
            p -> p.rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.dtp"))
            .icon(() -> TALER.get(Taler.SILVER_TALER).get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(GUIDE.get());
                output.accept(MARKET_STAND.get());
                output.accept(TRADE_CHEST.get());
                output.accept(BULLETIN_BOARD.get());
                output.accept(STATISTICS_BOARD.get());
                output.accept(UPGRADE_TABLE.get());
                output.accept(REPAIR_STATION.get());
                output.accept(BLACKMARKET.get());
                output.accept(SLOT_MACHINE.get());
                output.accept(DEPOSIT_CHEST.get());
                output.accept(MARKET_SCALE.get());
                output.accept(RECYCLER.get());
                output.accept(BEACON.get());
                output.accept(RELAY_BEACON.get());
                output.accept(CHUNK_LOADER.get());
                output.accept(CONTRABAND_CRATE.get());
                output.accept(FIRE_EXTINGUISHER.get());
                output.accept(WATCH_BELL.get());
                output.accept(OOZING_ROD.get());
                output.accept(SPAWN_WAND.get());
                output.accept(POCKET_MONEY_POUCH.get());
                output.accept(GUN.get());
                output.accept(ENDLESS_TORCH.get());
                output.accept(AMETHYST_CATALYST.get());
                output.accept(RAIN_CATALYST.get());
                output.accept(RETURN_SCROLL.get());
                output.accept(SCRAP.get());
                for (Taler t : Taler.values()) {
                    output.accept(t.item());
                }
            })
            .build());

    private ModItems() {
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
