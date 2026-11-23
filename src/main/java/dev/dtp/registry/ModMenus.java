package dev.dtp.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.menu.BeaconMenu;
import dev.dtp.menu.BlackmarketMenu;
import dev.dtp.menu.BulletinBoardMenu;
import dev.dtp.menu.ChunkLoaderMenu;
import dev.dtp.menu.DepositChestMenu;
import dev.dtp.menu.MarketMenu;
import dev.dtp.menu.RecyclerMenu;
import dev.dtp.menu.RelayBeaconMenu;
import dev.dtp.menu.RepairStationMenu;
import dev.dtp.menu.StatisticsMenu;
import dev.dtp.menu.UpgradeTableMenu;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Dtp.MODID);

    public static final Supplier<MenuType<MarketMenu>> MARKET = MENUS.register("market",
            () -> IMenuTypeExtension.create(MarketMenu::fromNetwork));
    public static final Supplier<MenuType<BeaconMenu>> BEACON = MENUS.register("beacon",
            () -> IMenuTypeExtension.create(BeaconMenu::fromNetwork));
    public static final Supplier<MenuType<DepositChestMenu>> DEPOSIT_CHEST = MENUS.register("deposit_chest",
            () -> IMenuTypeExtension.create(DepositChestMenu::fromNetwork));
    public static final Supplier<MenuType<BulletinBoardMenu>> BULLETIN_BOARD = MENUS.register("bulletin_board",
            () -> IMenuTypeExtension.create(BulletinBoardMenu::fromNetwork));
    public static final Supplier<MenuType<StatisticsMenu>> STATISTICS_BOARD = MENUS.register("statistics_board",
            () -> IMenuTypeExtension.create(StatisticsMenu::fromNetwork));
    public static final Supplier<MenuType<UpgradeTableMenu>> UPGRADE_TABLE = MENUS.register("upgrade_table",
            () -> IMenuTypeExtension.create(UpgradeTableMenu::fromNetwork));
    public static final Supplier<MenuType<RelayBeaconMenu>> RELAY_BEACON = MENUS.register("relay_beacon",
            () -> IMenuTypeExtension.create(RelayBeaconMenu::fromNetwork));
    public static final Supplier<MenuType<RecyclerMenu>> RECYCLER = MENUS.register("recycler",
            () -> IMenuTypeExtension.create(RecyclerMenu::fromNetwork));
    public static final Supplier<MenuType<ChunkLoaderMenu>> CHUNK_LOADER = MENUS.register("chunk_loader",
            () -> IMenuTypeExtension.create(ChunkLoaderMenu::fromNetwork));

    public static final Supplier<MenuType<BlackmarketMenu>> BLACKMARKET = MENUS.register("blackmarket",
            () -> IMenuTypeExtension.create(BlackmarketMenu::fromNetwork));
    public static final Supplier<MenuType<RepairStationMenu>> REPAIR_STATION = MENUS.register("repair_station",
            () -> IMenuTypeExtension.create(RepairStationMenu::fromNetwork));

    private ModMenus() {
    }

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
