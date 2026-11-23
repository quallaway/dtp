package dev.dtp.registry;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import dev.dtp.block.BlackmarketBlock;
import dev.dtp.block.BulletinBoardBlock;
import dev.dtp.block.BulletinBoardBlockEntity;
import dev.dtp.block.ChunkLoaderBlock;
import dev.dtp.block.ChunkLoaderBlockEntity;
import dev.dtp.block.DepositChestBlock;
import dev.dtp.block.DepositChestBlockEntity;
import dev.dtp.block.DtpBeaconBlock;
import dev.dtp.block.MarketScaleBlock;
import dev.dtp.block.MarketScaleBlockEntity;
import dev.dtp.block.MarketStandBlock;
import dev.dtp.block.RecyclerBlock;
import dev.dtp.block.RecyclerBlockEntity;
import dev.dtp.block.RelayBeaconBlock;
import dev.dtp.block.RepairStationBlock;
import dev.dtp.block.SlotMachineBlock;
import dev.dtp.block.SlotMachineBlockEntity;
import dev.dtp.block.StatisticsBoardBlock;
import dev.dtp.block.TradeChestBlock;
import dev.dtp.block.TradeChestBlockEntity;
import dev.dtp.block.UpgradeTableBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Dtp.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Dtp.MODID);

    public static final DeferredBlock<MarketStandBlock> MARKET_STAND = BLOCKS.registerBlock("market_stand", MarketStandBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<TradeChestBlock> TRADE_CHEST = BLOCKS.registerBlock("trade_chest", TradeChestBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<BulletinBoardBlock> BULLETIN_BOARD = BLOCKS.registerBlock("bulletin_board", BulletinBoardBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<StatisticsBoardBlock> STATISTICS_BOARD = BLOCKS.registerBlock("statistics_board", StatisticsBoardBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<UpgradeTableBlock> UPGRADE_TABLE = BLOCKS.registerBlock("upgrade_table", UpgradeTableBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
    public static final DeferredBlock<RepairStationBlock> REPAIR_STATION = BLOCKS.registerBlock("repair_station", RepairStationBlock::new,
            p -> p.mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD));
    // Schwarzmarkt: robust wie ein Verzauberungstisch.
    public static final DeferredBlock<BlackmarketBlock> BLACKMARKET = BLOCKS.registerBlock("blackmarket", BlackmarketBlock::new,
            p -> p.mapColor(MapColor.COLOR_BLACK).strength(5.0f, 1200.0f).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<DepositChestBlock> DEPOSIT_CHEST = BLOCKS.registerBlock("deposit_chest", DepositChestBlock::new,
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(2.5f).sound(SoundType.COPPER));
    // Marktwaage: flach, durchsichtig — das Filter-Item schwebt darüber.
    public static final DeferredBlock<MarketScaleBlock> MARKET_SCALE = BLOCKS.registerBlock("market_scale", MarketScaleBlock::new,
            p -> p.mapColor(MapColor.GOLD).strength(2.5f).sound(SoundType.COPPER).noOcclusion());
    // Recycler: Eisenkasten mit Schleifrad, arbeitet wie ein Ofen und leuchtet wie dieser, solange er brennt.
    public static final DeferredBlock<RecyclerBlock> RECYCLER = BLOCKS.registerBlock("recycler", RecyclerBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.5f).requiresCorrectToolForDrops().sound(SoundType.METAL)
                    .lightLevel(s -> s.getValue(RecyclerBlock.LIT) ? 13 : 0));
    // Der Hauptbeacon ist nicht abbaubar und lässt sich nicht verschieben; versetzt wird er nur per Befehl.
    // Beide Beacons haben einen Glaskörper (3D-Modell): noOcclusion, damit Nachbarflächen sichtbar bleiben.
    public static final DeferredBlock<DtpBeaconBlock> BEACON = BLOCKS.registerBlock("beacon", DtpBeaconBlock::new,
            p -> p.mapColor(MapColor.DIAMOND).strength(-1.0f, 3600000.0f).noLootTable().pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(s -> 15).sound(SoundType.GLASS).noOcclusion());
    // Neben-Beacon: abbaubar, aber nicht verschiebbar — seine Position ist am Server registriert.
    public static final DeferredBlock<RelayBeaconBlock> RELAY_BEACON = BLOCKS.registerBlock("relay_beacon", RelayBeaconBlock::new,
            p -> p.mapColor(MapColor.DIAMOND).strength(3.5f).requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(s -> 12).sound(SoundType.METAL).noOcclusion());
    // Chunkloader: Glaskasten wie der Neben-Beacon; leuchtet heller, solange er Restlaufzeit hat.
    public static final DeferredBlock<ChunkLoaderBlock> CHUNK_LOADER = BLOCKS.registerBlock("chunk_loader", ChunkLoaderBlock::new,
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(3.5f).requiresCorrectToolForDrops().pushReaction(PushReaction.IMMOVEABLE)
                    .lightLevel(s -> s.getValue(ChunkLoaderBlock.ACTIVE) ? 10 : 3).sound(SoundType.METAL).noOcclusion());

    // Spielautomat: Eisengehäuse mit vertieftem Walzenfenster und Hebel (3D-Modell), daher noOcclusion.
    public static final DeferredBlock<SlotMachineBlock> SLOT_MACHINE = BLOCKS.registerBlock("slot_machine", SlotMachineBlock::new,
            p -> p.mapColor(MapColor.METAL).strength(3.5f).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());

    public static final Supplier<BlockEntityType<TradeChestBlockEntity>> TRADE_CHEST_BE = BLOCK_ENTITIES.register("trade_chest",
            () -> new BlockEntityType<>(TradeChestBlockEntity::new, TRADE_CHEST.get()));
    public static final Supplier<BlockEntityType<DepositChestBlockEntity>> DEPOSIT_CHEST_BE = BLOCK_ENTITIES.register("deposit_chest",
            () -> new BlockEntityType<>(DepositChestBlockEntity::new, DEPOSIT_CHEST.get()));
    public static final Supplier<BlockEntityType<MarketScaleBlockEntity>> MARKET_SCALE_BE = BLOCK_ENTITIES.register("market_scale",
            () -> new BlockEntityType<>(MarketScaleBlockEntity::new, MARKET_SCALE.get()));
    public static final Supplier<BlockEntityType<RecyclerBlockEntity>> RECYCLER_BE = BLOCK_ENTITIES.register("recycler",
            () -> new BlockEntityType<>(RecyclerBlockEntity::new, RECYCLER.get()));
    public static final Supplier<BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER_BE = BLOCK_ENTITIES.register("chunk_loader",
            () -> new BlockEntityType<>(ChunkLoaderBlockEntity::new, CHUNK_LOADER.get()));

    public static final Supplier<BlockEntityType<SlotMachineBlockEntity>> SLOT_MACHINE_BE = BLOCK_ENTITIES.register("slot_machine",
            () -> new BlockEntityType<>(SlotMachineBlockEntity::new, SLOT_MACHINE.get()));
    public static final Supplier<BlockEntityType<BulletinBoardBlockEntity>> BULLETIN_BOARD_BE = BLOCK_ENTITIES.register("bulletin_board",
            () -> new BlockEntityType<>(BulletinBoardBlockEntity::new, BULLETIN_BOARD.get()));

    private ModBlocks() {
    }

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
