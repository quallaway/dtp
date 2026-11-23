package dev.dtp.gametest;

import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.BlockItem;
import dev.dtp.market.MerchantGoodsModifier;
import dev.dtp.market.MerchantGoods;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.ItemCost;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import net.minecraft.advancements.predicates.DataComponentMatchers;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import net.minecraft.advancements.predicates.MinMaxBounds;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.storage.loot.ValidationContext;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.raid.Raid;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.EventHooks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import dev.dtp.Dtp;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.food.FoodProperties;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSpawnPhantomsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import dev.dtp.advancement.UpgradeTrigger;
import dev.dtp.beacon.BuilderEffects;
import dev.dtp.beacon.Rebuild;
import dev.dtp.beacon.ExpeditionPrep;
import dev.dtp.DtpConfig;
import dev.dtp.beacon.AnimalHusbandry;
import dev.dtp.beacon.Beekeeping;
import dev.dtp.beacon.Farming;
import dev.dtp.beacon.BeaconLogic;
import dev.dtp.block.DepositChestBlock;
import dev.dtp.block.DepositChestBlockEntity;
import dev.dtp.block.MarketScaleBlockEntity;
import dev.dtp.block.MarketStandBlock;
import dev.dtp.block.TradeChestBlockEntity;
import dev.dtp.compat.Compat;
import dev.dtp.compat.sophisticated.SophisticatedTests;
import dev.dtp.contract.Contract;
import dev.dtp.contract.ContractClock;
import dev.dtp.contract.ContractBoard;
import dev.dtp.contract.ContractTemplates;
import dev.dtp.contract.Contracts;
import dev.dtp.contract.PlayerContracts;
import dev.dtp.market.MarketCategories;
import dev.dtp.market.MarketData;
import net.minecraft.world.entity.player.Inventory;
import dev.dtp.market.MarketEvents;
import dev.dtp.market.TradeList;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgradeEffects;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.BeetrootBlock;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.world.chunk.ForcedChunkManager;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import dev.dtp.upgrade.AnvilCap;
import dev.dtp.upgrade.FishingTreasureModifier;
import dev.dtp.upgrade.HoeHarvest;
import dev.dtp.upgrade.FishingNet;
import net.minecraft.world.entity.animal.fish.Cod;
import dev.dtp.upgrade.ChainBreaker;
import dev.dtp.upgrade.PickupBoost;
import dev.dtp.upgrade.UpgradeCosts;
import dev.dtp.upgrade.TreeLayout;
import dev.dtp.upgrade.ModuleCooldowns;
import dev.dtp.upgrade.Skills;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.PlayerUpgradeLogic;
import dev.dtp.upgrade.PlayerUpgrades;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;
import dev.dtp.world.DimensionAccess;
import dev.dtp.currency.DepositChests;
import dev.dtp.chunkloader.ChunkLoaderData;
import dev.dtp.chunkloader.ChunkLoaders;
import dev.dtp.block.ChunkLoaderBlock;
import dev.dtp.block.BulletinBoardBlock;
import dev.dtp.block.BulletinBoardBlockEntity;
import dev.dtp.block.SlotMachineBlock;
import dev.dtp.block.SlotMachineBlockEntity;
import dev.dtp.casino.SlotMachine;
import dev.dtp.data.Versioned;
import dev.dtp.currency.Money;
import dev.dtp.currency.StructureTalerModifier;
import dev.dtp.currency.Taler;
import dev.dtp.blackmarket.Blackmarket;
import dev.dtp.blackmarket.BlackmarketData;
import dev.dtp.blackmarket.BlackmarketSpawner;
import dev.dtp.blackmarket.BlackmarketTrader;
import dev.dtp.blackmarket.ContrabandCrateItem;
import dev.dtp.blackmarket.StructureFindsModifier;
import dev.dtp.registry.ModEntities;
import dev.dtp.registry.ModItems;
import dev.dtp.repair.RepairStation;
import dev.dtp.guide.GuideBook;
import dev.dtp.guide.GuideGrids;
import dev.dtp.guide.Onboarding;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import dev.dtp.contraband.EndlessTorchItem;
import dev.dtp.contraband.GunItem;
import dev.dtp.contraband.OozingRodItem;
import dev.dtp.contraband.PocketMoneyPouchItem;
import dev.dtp.contraband.SpawnWandItem;
import dev.dtp.contraband.UseOutcome;
import dev.dtp.contraband.WatchBellItem;
import dev.dtp.supplies.RainCatalystItem;
import dev.dtp.supplies.ReturnScrollItem;
import dev.dtp.recipe.MerchantRepairItemRecipe;
import dev.dtp.contraband.FireExtinguisherItem;
import dev.dtp.contraband.ContrabandItem;
import dev.dtp.contraband.ContrabandDefinition;
import dev.dtp.contraband.ContrabandDefinitions;
import net.minecraft.util.Unit;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.VillagerTrade;
import net.minecraft.world.level.storage.loot.LootContext;
import dev.dtp.blackmarket.ScamCurse;
import dev.dtp.blackmarket.TollCurse;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.tags.EnchantmentTags;
import dev.dtp.market.Market;
import dev.dtp.market.Sales;
import dev.dtp.registry.ModBlocks;
import dev.dtp.menu.RecyclerMenu;
import dev.dtp.block.RecyclerBlock;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import dev.dtp.recycling.Recycling;
import dev.dtp.block.RecyclerBlockEntity;
import dev.dtp.menu.StatisticsMenu;
import dev.dtp.stats.StatisticsData;

/**
 * Automatisierte Prüfungen der Kernlogik. Ausführen mit {@code ./gradlew runGameTestServer}.
 * Getestet wird außerhalb des Territoriums (kein Beacon gesetzt), also mit Preisen an der Untergrenze.
 */
public final class DtpGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Dtp.MODID);

    private static final List<String> TESTS = List.of(
            "taler_split", "data_loaded", "contract_decay", "market_stock_sale", "deposit_payment", "deposit_redstone", "chest_comparator", "market_redstone", "band_position", "contract_delivery", "market_categories", "board_rotation", "board_flag", "clocks_players", "accepted_contract_clock", "contract_unlock", "workshop_buy", "trade_upgrades", "max_health", "chain_collect", "fortune_silk_touch", "relay_beacon", "nether_lock", "market_event", "market_event_types", "market_fast_recovery", "market_world_bonus", "price_history", "contract_conditions", "contract_condition_delivery", "trade_list_order", "chain_durability", "chain_inventory", "shared_goods", "ominous_banner", "market_minimum", "market_floor_recovery", "market_scale", "market_category_unlock", "market_event_rotation", "player_modules", "rush_kill", "barrier_shield", "area_mining", "skills", "fishing_net", "contract_group_delivery", "contract_tier_scaling", "contract_quality", "contract_template_unlock", "contract_random_potions", "animal_husbandry", "beekeeping", "farming", "server_rules", "beacon_building", "rebuild", "expedition_prep", "updraft", "farm_upgrades", "crop_fortune", "enchant_bonuses", "fishing_finds_syntax", "fishing_finds_entries", "fishing_treasure", "double_bobbers", "anvil_cap", "workshop_layout", "beacon_layout", "contract_random_enchantments", "blackmarket_upgrade", "blackmarket_rejects", "blackmarket_daily_limit", "blackmarket_outcomes", "repair_station_quote", "repair_station_rejects", "repair_station_repair", "scam_curse_contract", "scam_curse_anvil", "scam_curse_tags", "toll_curse_pay", "toll_curse_no_coins", "toll_curse_chain", "toll_curse_unbreaking", "toll_curse_tags", "blackmarket_chance", "blackmarket_lure", "blackmarket_offers", "blackmarket_no_arbitrage", "contraband_crate_open", "structure_finds_table", "structure_finds_chest", "structure_taler", "contraband_use", "contraband_recharge", "contraband_protection", "supplies_amethyst_catalyst", "supplies_rain_catalyst", "supplies_return_scroll", "contraband_watch_bell", "contraband_oozing_rod", "contraband_spawn_wand", "contraband_pocket_money_pouch", "contraband_gun", "contraband_endless_torch", "beacon_recipe", "guide_grids", "onboarding", "guide_files", "guide_parser", "remote_delivery", "contract_clock_lines", "contract_clock_binding", "chunk_loader", "slot_machine_odds", "slot_machine_payout", "slot_machine_spin", "recycler_process", "recycler_rejects", "recycler_menu", "recycler_chance", "recycling_data", "scrap_tradable", "statistics_sales", "statistics_seed", "statistics_snapshot", "statistics_income", "advancements_loaded", "data_files_valid", "compat_optional", "merchant_goods_offers", "merchant_goods_hero_gift", "merchant_goods_blocked", "merchant_goods_combine", "merchant_goods_pool");
    /** Tests mit Umkreissuche laufen je in einer eigenen Runde, damit keine Kisten benachbarter Tests mitzählen. */
    private static final List<String> ISOLATED = List.of("market_stock_sale", "deposit_payment", "deposit_redstone", "chest_comparator", "market_redstone", "contract_delivery", "contract_unlock", "workshop_buy", "chain_collect", "relay_beacon", "nether_lock", "market_event", "market_event_types", "market_fast_recovery", "market_world_bonus", "price_history", "contract_condition_delivery", "chain_durability", "chain_inventory", "shared_goods", "ominous_banner", "market_minimum", "market_floor_recovery", "market_scale", "market_category_unlock", "market_event_rotation", "player_modules", "contract_group_delivery", "contract_template_unlock", "animal_husbandry", "beekeeping", "farming", "server_rules", "beacon_building", "rebuild", "expedition_prep", "blackmarket_lure", "contraband_watch_bell", "contraband_pocket_money_pouch", "contraband_gun", "supplies_return_scroll", "supplies_rain_catalyst", "remote_delivery", "chunk_loader", "slot_machine_spin", "recycler_process", "recycler_menu", "scrap_tradable", "statistics_sales", "compat_optional");

    static {
        FUNCTIONS.register("merchant_goods_offers", () -> DtpGameTests::merchantGoodsOffers);
        FUNCTIONS.register("merchant_goods_hero_gift", () -> DtpGameTests::merchantGoodsHeroGift);
        FUNCTIONS.register("merchant_goods_blocked", () -> DtpGameTests::merchantGoodsBlocked);
        FUNCTIONS.register("merchant_goods_combine", () -> DtpGameTests::merchantGoodsCombine);
        FUNCTIONS.register("merchant_goods_pool", () -> DtpGameTests::merchantGoodsPool);
        FUNCTIONS.register("taler_split", () -> DtpGameTests::talerSplit);
        FUNCTIONS.register("remote_delivery", () -> DtpGameTests::remoteDelivery);
        FUNCTIONS.register("contract_clock_lines", () -> DtpGameTests::contractClockLines);
        FUNCTIONS.register("contract_clock_binding", () -> DtpGameTests::contractClockBinding);
        FUNCTIONS.register("chunk_loader", () -> DtpGameTests::chunkLoader);
        FUNCTIONS.register("slot_machine_odds", () -> DtpGameTests::slotMachineOdds);
        FUNCTIONS.register("slot_machine_payout", () -> DtpGameTests::slotMachinePayout);
        FUNCTIONS.register("slot_machine_spin", () -> DtpGameTests::slotMachineSpin);
        FUNCTIONS.register("recycler_process", () -> DtpGameTests::recyclerProcess);
        FUNCTIONS.register("recycler_rejects", () -> DtpGameTests::recyclerRejects);
        FUNCTIONS.register("recycler_menu", () -> DtpGameTests::recyclerMenu);
        FUNCTIONS.register("recycler_chance", () -> DtpGameTests::recyclerChance);
        FUNCTIONS.register("recycling_data", () -> DtpGameTests::recyclingData);
        FUNCTIONS.register("scrap_tradable", () -> DtpGameTests::scrapTradable);
        FUNCTIONS.register("statistics_sales", () -> DtpGameTests::statisticsSales);
        FUNCTIONS.register("statistics_seed", () -> DtpGameTests::statisticsSeed);
        FUNCTIONS.register("statistics_snapshot", () -> DtpGameTests::statisticsSnapshot);
        FUNCTIONS.register("statistics_income", () -> DtpGameTests::statisticsIncome);
        FUNCTIONS.register("advancements_loaded", () -> DtpGameTests::advancementsLoaded);
        FUNCTIONS.register("data_files_valid", () -> DtpGameTests::dataFilesValid);
        FUNCTIONS.register("compat_optional", () -> DtpGameTests::compatOptional);
        FUNCTIONS.register("blackmarket_chance", () -> DtpGameTests::blackmarketChance);
        FUNCTIONS.register("blackmarket_lure", () -> DtpGameTests::blackmarketLure);
        FUNCTIONS.register("blackmarket_offers", () -> DtpGameTests::blackmarketOffers);
        FUNCTIONS.register("blackmarket_no_arbitrage", () -> DtpGameTests::blackmarketNoArbitrage);
        FUNCTIONS.register("contraband_crate_open", () -> DtpGameTests::contrabandCrateOpen);
        FUNCTIONS.register("structure_finds_table", () -> DtpGameTests::structureFindsTable);
        FUNCTIONS.register("structure_finds_chest", () -> DtpGameTests::structureFindsChest);
        FUNCTIONS.register("structure_taler", () -> DtpGameTests::structureTaler);
        FUNCTIONS.register("contraband_use", () -> DtpGameTests::contrabandUse);
        FUNCTIONS.register("contraband_recharge", () -> DtpGameTests::contrabandRecharge);
        FUNCTIONS.register("contraband_protection", () -> DtpGameTests::contrabandProtection);
        FUNCTIONS.register("supplies_amethyst_catalyst", () -> DtpGameTests::suppliesAmethystCatalyst);
        FUNCTIONS.register("supplies_rain_catalyst", () -> DtpGameTests::suppliesRainCatalyst);
        FUNCTIONS.register("supplies_return_scroll", () -> DtpGameTests::suppliesReturnScroll);
        FUNCTIONS.register("contraband_watch_bell", () -> DtpGameTests::contrabandWatchBell);
        FUNCTIONS.register("contraband_oozing_rod", () -> DtpGameTests::contrabandOozingRod);
        FUNCTIONS.register("contraband_spawn_wand", () -> DtpGameTests::contrabandSpawnWand);
        FUNCTIONS.register("contraband_pocket_money_pouch", () -> DtpGameTests::contrabandPocketMoneyPouch);
        FUNCTIONS.register("contraband_gun", () -> DtpGameTests::contrabandGun);
        FUNCTIONS.register("contraband_endless_torch", () -> DtpGameTests::contrabandEndlessTorch);
        FUNCTIONS.register("data_loaded", () -> DtpGameTests::dataLoaded);
        FUNCTIONS.register("beacon_recipe", () -> DtpGameTests::beaconRecipe);
        FUNCTIONS.register("guide_grids", () -> DtpGameTests::guideGrids);
        FUNCTIONS.register("onboarding", () -> DtpGameTests::onboarding);
        FUNCTIONS.register("guide_files", () -> DtpGameTests::guideFiles);
        FUNCTIONS.register("guide_parser", () -> DtpGameTests::guideParser);
        FUNCTIONS.register("contract_decay", () -> DtpGameTests::contractDecay);
        FUNCTIONS.register("market_stock_sale", () -> DtpGameTests::marketStockSale);
        FUNCTIONS.register("deposit_payment", () -> DtpGameTests::depositPayment);
        FUNCTIONS.register("deposit_redstone", () -> DtpGameTests::depositRedstone);
        FUNCTIONS.register("chest_comparator", () -> DtpGameTests::chestComparator);
        FUNCTIONS.register("market_redstone", () -> DtpGameTests::marketRedstone);
        FUNCTIONS.register("band_position", () -> DtpGameTests::bandPosition);
        FUNCTIONS.register("contract_delivery", () -> DtpGameTests::contractDelivery);
        FUNCTIONS.register("market_categories", () -> DtpGameTests::marketCategories);
        FUNCTIONS.register("board_rotation", () -> DtpGameTests::boardRotation);
        FUNCTIONS.register("board_flag", () -> DtpGameTests::boardFlag);
        FUNCTIONS.register("clocks_players", () -> DtpGameTests::clocksPlayers);
        FUNCTIONS.register("accepted_contract_clock", () -> DtpGameTests::acceptedContractClock);
        FUNCTIONS.register("contract_unlock", () -> DtpGameTests::contractUnlock);
        FUNCTIONS.register("workshop_buy", () -> DtpGameTests::workshopBuy);
        FUNCTIONS.register("trade_upgrades", () -> DtpGameTests::tradeUpgrades);
        FUNCTIONS.register("max_health", () -> DtpGameTests::maxHealth);
        FUNCTIONS.register("chain_collect", () -> DtpGameTests::chainCollect);
        FUNCTIONS.register("fortune_silk_touch", () -> DtpGameTests::fortuneSilkTouch);
        FUNCTIONS.register("relay_beacon", () -> DtpGameTests::relayBeacon);
        FUNCTIONS.register("nether_lock", () -> DtpGameTests::netherLock);
        FUNCTIONS.register("market_event", () -> DtpGameTests::marketEvent);
        FUNCTIONS.register("price_history", () -> DtpGameTests::priceHistory);
        FUNCTIONS.register("contract_conditions", () -> DtpGameTests::contractConditions);
        FUNCTIONS.register("contract_condition_delivery", () -> DtpGameTests::contractConditionDelivery);
        FUNCTIONS.register("trade_list_order", () -> DtpGameTests::tradeListOrder);
        FUNCTIONS.register("chain_durability", () -> DtpGameTests::chainDurability);
        FUNCTIONS.register("chain_inventory", () -> DtpGameTests::chainInventory);
        FUNCTIONS.register("shared_goods", () -> DtpGameTests::sharedGoods);
        FUNCTIONS.register("ominous_banner", () -> DtpGameTests::ominousBanner);
        FUNCTIONS.register("market_scale", () -> DtpGameTests::marketScale);
        FUNCTIONS.register("market_minimum", () -> DtpGameTests::marketMinimum);
        FUNCTIONS.register("market_floor_recovery", () -> DtpGameTests::marketFloorRecovery);
        FUNCTIONS.register("market_category_unlock", () -> DtpGameTests::marketCategoryUnlock);
        FUNCTIONS.register("market_event_rotation", () -> DtpGameTests::marketEventRotation);
        FUNCTIONS.register("market_event_types", () -> DtpGameTests::marketEventTypes);
        FUNCTIONS.register("market_fast_recovery", () -> DtpGameTests::marketFastRecovery);
        FUNCTIONS.register("market_world_bonus", () -> DtpGameTests::marketWorldBonus);
        FUNCTIONS.register("player_modules", () -> DtpGameTests::playerModules);
        FUNCTIONS.register("rush_kill", () -> DtpGameTests::rushKill);
        FUNCTIONS.register("barrier_shield", () -> DtpGameTests::barrierShield);
        FUNCTIONS.register("area_mining", () -> DtpGameTests::areaMining);
        FUNCTIONS.register("skills", () -> DtpGameTests::skills);
        FUNCTIONS.register("fishing_net", () -> DtpGameTests::fishingNet);
        FUNCTIONS.register("farm_upgrades", () -> DtpGameTests::farmUpgrades);
        FUNCTIONS.register("crop_fortune", () -> DtpGameTests::cropFortune);
        FUNCTIONS.register("enchant_bonuses", () -> DtpGameTests::enchantBonuses);
        FUNCTIONS.register("fishing_finds_syntax", () -> DtpGameTests::fishingFindsSyntax);
        FUNCTIONS.register("fishing_finds_entries", () -> DtpGameTests::fishingFindsEntries);
        FUNCTIONS.register("fishing_treasure", () -> DtpGameTests::fishingTreasure);
        FUNCTIONS.register("double_bobbers", () -> DtpGameTests::doubleBobbers);
        FUNCTIONS.register("anvil_cap", () -> DtpGameTests::anvilCap);
        FUNCTIONS.register("workshop_layout", () -> DtpGameTests::workshopLayout);
        FUNCTIONS.register("beacon_layout", () -> DtpGameTests::beaconLayout);
        FUNCTIONS.register("contract_group_delivery", () -> DtpGameTests::contractGroupDelivery);
        FUNCTIONS.register("contract_tier_scaling", () -> DtpGameTests::contractTierScaling);
        FUNCTIONS.register("contract_quality", () -> DtpGameTests::contractQuality);
        FUNCTIONS.register("contract_template_unlock", () -> DtpGameTests::contractTemplateUnlock);
        FUNCTIONS.register("contract_random_enchantments", () -> DtpGameTests::contractRandomEnchantments);
        FUNCTIONS.register("contract_random_potions", () -> DtpGameTests::contractRandomPotions);
        FUNCTIONS.register("animal_husbandry", () -> DtpGameTests::animalHusbandry);
        FUNCTIONS.register("beekeeping", () -> DtpGameTests::beekeeping);
        FUNCTIONS.register("farming", () -> DtpGameTests::farming);
        FUNCTIONS.register("server_rules", () -> DtpGameTests::serverRules);
        FUNCTIONS.register("beacon_building", () -> DtpGameTests::beaconBuilding);
        FUNCTIONS.register("rebuild", () -> DtpGameTests::rebuild);
        FUNCTIONS.register("expedition_prep", () -> DtpGameTests::expeditionPrep);
        FUNCTIONS.register("updraft", () -> DtpGameTests::updraft);
        FUNCTIONS.register("blackmarket_upgrade", () -> DtpGameTests::blackmarketUpgrade);
        FUNCTIONS.register("blackmarket_rejects", () -> DtpGameTests::blackmarketRejects);
        FUNCTIONS.register("blackmarket_daily_limit", () -> DtpGameTests::blackmarketDailyLimit);
        FUNCTIONS.register("blackmarket_outcomes", () -> DtpGameTests::blackmarketOutcomes);
        FUNCTIONS.register("repair_station_quote", () -> DtpGameTests::repairStationQuote);
        FUNCTIONS.register("repair_station_rejects", () -> DtpGameTests::repairStationRejects);
        FUNCTIONS.register("repair_station_repair", () -> DtpGameTests::repairStationRepair);
        FUNCTIONS.register("scam_curse_contract", () -> DtpGameTests::scamCurseContract);
        FUNCTIONS.register("scam_curse_anvil", () -> DtpGameTests::scamCurseAnvil);
        FUNCTIONS.register("scam_curse_tags", () -> DtpGameTests::scamCurseTags);
        FUNCTIONS.register("toll_curse_pay", () -> DtpGameTests::tollCursePay);
        FUNCTIONS.register("toll_curse_no_coins", () -> DtpGameTests::tollCurseNoCoins);
        FUNCTIONS.register("toll_curse_chain", () -> DtpGameTests::tollCurseChain);
        FUNCTIONS.register("toll_curse_unbreaking", () -> DtpGameTests::tollCurseUnbreaking);
        FUNCTIONS.register("toll_curse_tags", () -> DtpGameTests::tollCurseTags);
    }

    private DtpGameTests() {
    }

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(DtpGameTests::registerTests);
    }

    /** Schaltet am Hauptbeacon genau diese Effekte ein (mit passend vielen Slots) und stellt danach alles wieder her. */
    private static void withBeaconEffects(GameTestHelper helper, List<ServerUpgrade> effects, Runnable body) {
        WorldProgress progress = WorldProgress.get(helper.getLevel().getServer());
        withTerritory(helper, () -> {
            List<ServerUpgrade> oldActive = List.copyOf(progress.activeEffects());
            Map<ServerUpgrade, Integer> oldLevels = new HashMap<>();
            oldLevels.put(ServerUpgrade.EFFECT_SLOTS, progress.level(ServerUpgrade.EFFECT_SLOTS));
            effects.forEach(e -> oldLevels.put(e, progress.level(e)));
            oldActive.forEach(progress::toggleEffect);
            try {
                progress.setLevel(ServerUpgrade.EFFECT_SLOTS, Math.max(0, effects.size() - DtpConfig.BASE_EFFECT_SLOTS.get()));
                for (ServerUpgrade effect : effects) {
                    progress.setLevel(effect, 1);
                    helper.assertTrue(progress.toggleEffect(effect), effect.id + " can be chosen");
                }
                body.run();
            } finally {
                List.copyOf(progress.activeEffects()).forEach(progress::toggleEffect);
                oldLevels.forEach(progress::setLevel);
                oldActive.forEach(progress::toggleEffect);
            }
        });
    }

    /**
     * Attribut-Effekte: Baumeister-Reichweite, Wegebonus des Stallmeisters nur auf Trampelpfad, Federfall halbiert, keine
     * Phantome, Reittempo am gelenkten Pferd und Schwein (und wieder weg nach dem Absteigen), Reittiere ohne Fallschaden,
     * der Glückliche Ghast bekommt sein Tempo auf FLYING_SPEED; Ruhe nach dem Absteigen, beendet durch Aufsteigen und Leine.
     */
    private static void beaconBuilding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        List<ServerUpgrade> effects = List.of(ServerUpgrade.EFFECT_BUILDER, ServerUpgrade.EFFECT_FEATHER_FALL,
                ServerUpgrade.EFFECT_QUIET_NIGHTS, ServerUpgrade.EFFECT_STABLEMASTER);
        for (ServerUpgrade effect : List.of(ServerUpgrade.EFFECT_BUILDER, ServerUpgrade.EFFECT_FEATHER_FALL, ServerUpgrade.EFFECT_STABLEMASTER,
                ServerUpgrade.EFFECT_QUIET_NIGHTS, ServerUpgrade.EFFECT_EXPEDITION)) {
            helper.assertTrue(effect.isEffect() && !effect.isVanillaEffect(), effect.id + " is an own beacon effect");
            helper.assertTrue(UpgradeCosts.get(effect) != null, effect.id + " has costs in the datapack");
        }
        withBeaconEffects(helper, effects, () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos floorRel = new BlockPos(1, 1, 1);
            BlockPos floor = helper.absolutePos(floorRel);
            helper.setBlock(floorRel, Blocks.DIRT_PATH);
            player.snapTo(floor.getX() + 0.5, floor.getY() + 0.9375, floor.getZ() + 0.5, 0f, 0f);
            helper.assertTrue(BeaconLogic.effectsAt(server, player.blockPosition()).containsAll(effects), "all effects work at the beacon");

            double reach = player.getAttributeBaseValue(Attributes.BLOCK_INTERACTION_RANGE);
            BuilderEffects.update(player, new HashSet<>());
            helper.assertTrue(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) == reach + DtpConfig.BUILDER_REACH.get(),
                    "master builder adds reach: " + player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE));
            helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.ROAD_ID) != null,
                    "on a dirt path the stablemaster's road bonus applies on foot");

            helper.setBlock(floorRel, Blocks.STONE);
            player.snapTo(floor.getX() + 0.5, floor.getY() + 1.0, floor.getZ() + 0.5, 0f, 0f);
            BuilderEffects.update(player, new HashSet<>());
            helper.assertTrue(player.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.ROAD_ID) == null,
                    "on stone there is no road bonus");

            LivingFallEvent fall = NeoForge.EVENT_BUS.post(new LivingFallEvent(player, 10, 1f));
            helper.assertTrue(Math.abs(fall.getDamageMultiplier() - DtpConfig.FALL_DAMAGE_PERCENT.get() / 100f) < 1e-6,
                    "feather fall scales fall damage: " + fall.getDamageMultiplier());
            PlayerSpawnPhantomsEvent phantoms = NeoForge.EVENT_BUS.post(new PlayerSpawnPhantomsEvent(player, 2));
            helper.assertTrue(phantoms.getResult() == PlayerSpawnPhantomsEvent.Result.DENY, "quiet nights keep phantoms away");

            AbstractHorse horse = helper.spawnWithNoFreeWill(EntityTypes.HORSE, new BlockPos(3, 2, 3));
            helper.assertTrue(player.startRiding(horse, true, false), "the player mounts the horse");
            Set<LivingEntity> ridden = new HashSet<>();
            BuilderEffects.update(player, ridden);
            helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.MOUNT_ID) == null,
                    "an unsaddled horse is not steered and gets no bonus");
            horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            BuilderEffects.update(player, ridden);
            helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.MOUNT_ID) != null,
                    "a steered horse in range of the stablemaster is faster");
            LivingFallEvent riderFall = NeoForge.EVENT_BUS.post(new LivingFallEvent(player, 10, 1f));
            helper.assertTrue(riderFall.isCanceled(), "the rider takes no fall damage from the horse");
            float health = horse.getHealth();
            horse.hurtServer(level, level.damageSources().fall(), 5f);
            helper.assertTrue(horse.getHealth() == health, "the horse takes no fall damage");
            player.stopRiding();
            BuilderEffects.update(server);
            helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.MOUNT_ID) == null,
                    "after dismounting the bonus is gone");
            helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.REST_ID) != null,
                    "after dismounting in range of the stablemaster the horse rests");
            helper.assertTrue(player.startRiding(horse, true, false), "the player mounts the resting horse");
            helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.REST_ID) == null,
                    "mounting ends the rest");
            player.stopRiding();
            helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.REST_ID) != null,
                    "dismounting again starts a new rest");
            horse.setLeashedTo(player, true);
            BuilderEffects.update(server);
            helper.assertTrue(horse.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.REST_ID) == null,
                    "a leashed horse no longer rests");
            horse.dropLeash();

            // Alle Reittiere, nicht nur die Pferdefamilie: das Schwein mit Sattel und Karottenangel.
            Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, new BlockPos(3, 2, 1));
            pig.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
            player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CARROT_ON_A_STICK));
            helper.assertTrue(player.startRiding(pig, true, false), "the player mounts the pig");
            BuilderEffects.update(player, new HashSet<>());
            helper.assertTrue(pig.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.MOUNT_ID) != null,
                    "a steered pig in range of the stablemaster is faster");
            float pigHealth = pig.getHealth();
            pig.hurtServer(level, level.damageSources().fall(), 5f);
            helper.assertTrue(pig.getHealth() == pigHealth, "the pig takes no fall damage");
            helper.assertTrue(NeoForge.EVENT_BUS.post(new LivingFallEvent(player, 10, 1f)).isCanceled(),
                    "the rider takes no fall damage from the pig");
            player.stopRiding();
            player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            BuilderEffects.update(server);
            helper.assertTrue(pig.getAttribute(Attributes.MOVEMENT_SPEED).getModifier(BuilderEffects.MOUNT_ID) == null,
                    "after dismounting the pig the bonus is gone");
            HappyGhast ghast = helper.spawnWithNoFreeWill(EntityTypes.HAPPY_GHAST, new BlockPos(1, 4, 3));
            helper.assertTrue(BuilderEffects.isMount(ghast) && BuilderEffects.speedAttribute(ghast) == Attributes.FLYING_SPEED,
                    "the happy ghast is a mount and flies on FLYING_SPEED");
            ghast.setItemSlot(EquipmentSlot.BODY, new ItemStack(Items.HARNESS.white()));
            helper.assertTrue(player.startRiding(ghast, true, false), "the player boards the happy ghast");
            player.stopRiding();
            helper.assertTrue(ghast.getAttribute(Attributes.FLYING_SPEED).getModifier(BuilderEffects.REST_ID) != null,
                    "the happy ghast hovers in place after dismounting");
            BuilderEffects.endRest(ghast);
            helper.assertFalse(BuilderEffects.isMount(helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 1))),
                    "a cow is no mount");

            WorldProgress.get(server).toggleEffect(ServerUpgrade.EFFECT_BUILDER);
            BuilderEffects.update(player, new HashSet<>());
            helper.assertTrue(player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE) == reach, "without the effect the reach is back to normal");
        });
        helper.killAllEntities();
        helper.succeed();
    }

    /**
     * Rückbau: Glas kommt sofort und ohne Werkzeugverschleiß zurück, ein fremder Spieler und ein veränderter Block
     * bekommen keinen Rückbau, eine Doppelstufe gibt zwei Stufen, eine Truhe wird nicht gemerkt.
     */
    private static void rebuild(GameTestHelper helper) {
        withBeaconEffects(helper, List.of(ServerUpgrade.EFFECT_BUILDER), () -> {
            ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            ServerPlayer other = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            BlockPos glassRel = new BlockPos(1, 2, 1);
            BlockPos glass = helper.absolutePos(glassRel);
            try {
                helper.setBlock(glassRel, Blocks.GLASS);
                helper.assertTrue(Rebuild.record(player, glass), "glass placed in range is remembered");
                helper.assertTrue(Rebuild.marked(player, glass, helper.getBlockState(glassRel)), "the glass is marked for its owner");
                helper.assertFalse(Rebuild.marked(other, glass, helper.getBlockState(glassRel)), "nobody else can undo it");
                ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
                player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
                player.gameMode.destroyBlock(glass);
                helper.assertTrue(helper.getBlockState(glassRel).isAir(), "the glass is gone");
                helper.assertTrue(player.getInventory().countItem(Items.GLASS) == 1, "the glass comes back into the inventory");
                helper.assertTrue(pickaxe.getDamageValue() == 0, "the tool takes no wear");

                BlockPos slabRel = new BlockPos(2, 2, 1);
                BlockPos slab = helper.absolutePos(slabRel);
                helper.setBlock(slabRel, Blocks.STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE));
                helper.assertTrue(Rebuild.record(player, slab), "a double slab is remembered");
                player.gameMode.destroyBlock(slab);
                helper.assertTrue(player.getInventory().countItem(Items.STONE_SLAB) == 2, "a double slab gives two slabs back");

                helper.setBlock(slabRel, Blocks.STONE_SLAB);
                Rebuild.record(player, slab);
                helper.setBlock(slabRel, Blocks.STONE);
                helper.assertFalse(Rebuild.marked(player, slab, helper.getBlockState(slabRel)), "a changed block is not undone");

                BlockPos chestRel = new BlockPos(3, 2, 1);
                helper.setBlock(chestRel, Blocks.CHEST);
                helper.assertFalse(Rebuild.record(player, helper.absolutePos(chestRel)), "blocks with contents are never remembered");
            } finally {
                Rebuild.forget(player);
                Rebuild.forget(other);
            }
        });
        helper.succeed();
    }

    /** Expeditionsvorbereitung: Trank doppelt so lang, mit erzwungenem Wurf bleibt er erhalten, Brot sättigt stärker. */
    private static void expeditionPrep(GameTestHelper helper) {
        int oldChance = DtpConfig.POTION_KEEP_CHANCE.get();
        withBeaconEffects(helper, List.of(ServerUpgrade.EFFECT_EXPEDITION), () -> {
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
            player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
            ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.STRENGTH);
            ExpeditionPrep.extendPotion(player, potion);
            MobEffectInstance strength = player.getEffect(MobEffects.STRENGTH);
            int expected = 3600 * DtpConfig.POTION_DURATION_PERCENT.get() / 100;
            helper.assertTrue(strength != null && strength.getDuration() == expected,
                    "the strength potion lasts " + expected + " ticks, got " + (strength == null ? "none" : strength.getDuration()));

            try {
                DtpConfig.POTION_KEEP_CHANCE.set(100);
                LivingEntityUseItemEvent.Finish finish = NeoForge.EVENT_BUS.post(
                        new LivingEntityUseItemEvent.Finish(player, potion.copy(), 0, new ItemStack(Items.GLASS_BOTTLE)));
                helper.assertTrue(finish.getResultStack().is(Items.POTION), "with a lucky roll the potion is kept");
            } finally {
                DtpConfig.POTION_KEEP_CHANCE.set(oldChance);
            }

            FoodProperties bread = new ItemStack(Items.BREAD).get(DataComponents.FOOD);
            player.getFoodData().setFoodLevel(20);
            player.getFoodData().setSaturation(0f);
            ExpeditionPrep.feed(player, bread);
            float extra = bread.saturation() * DtpConfig.FOOD_SATURATION_BONUS.get() / 100f;
            helper.assertTrue(Math.abs(player.getFoodData().getSaturationLevel() - extra) < 1e-4,
                    "bread saturates more: " + player.getFoodData().getSaturationLevel());
        });
        helper.succeed();
    }

    /** Aufwind: Mit Modul und gelingendem Wurf bleibt die Rakete, sonst wird sie verbraucht. */
    private static void updraft(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.assertTrue(PlayerUpgrade.UPDRAFT.isModule() && PlayerUpgradeDefinitions.get(PlayerUpgrade.UPDRAFT) != null,
                "updraft is a module from the datapack");
        ItemStack rockets = new ItemStack(Items.FIREWORK_ROCKET, 5);
        player.startFallFlying();
        helper.assertFalse(PlayerUpgradeEffects.updraft(player, rockets, 0), "without the module nothing happens");
        equip(player, PlayerUpgrade.UPDRAFT);
        // Wurf 0..99 gegen die Chance in Prozent: knapp darunter gelingt, genau darauf nicht.
        int chance = (int) param(PlayerUpgrade.UPDRAFT, "chance", 1);
        helper.assertTrue(PlayerUpgradeEffects.updraft(player, rockets, chance - 1) && rockets.getCount() == 6,
                "a lucky roll adds the rocket that vanilla takes next");
        helper.assertFalse(PlayerUpgradeEffects.updraft(player, rockets, chance), "an unlucky roll keeps nothing");
        player.stopFallFlying();
        helper.assertFalse(PlayerUpgradeEffects.updraft(player, rockets, 0), "only while gliding");
        helper.succeed();
    }

    // --- Reparaturbank ------------------------------------------------------------------------

    private static SimpleContainer repairSlots(ItemStack item, int damage, Taler coin, int count) {
        item.setDamageValue(damage);
        SimpleContainer slots = new SimpleContainer(2);
        slots.setItem(RepairStation.SLOT_ITEM, item);
        slots.setItem(RepairStation.SLOT_PAYMENT, coin == null ? ItemStack.EMPTY : new ItemStack(coin.item(), count));
        return slots;
    }

    private static long talerInInventory(Player player) {
        long sum = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            sum += Taler.valueOf(player.getInventory().getItem(i));
        }
        return sum;
    }

    /** Der Kurs: Eisen kostet 2 Taler je Punkt, der Preis je Punkt wächst mit der Wurzel der Haltbarkeit; Wechselgeld stimmt. */
    private static void repairStationQuote(GameTestHelper helper) {
        helper.assertTrue(RepairStation.pricePerPointMilli(250, 2.0, 0.5) == 2000, "iron reference: 2 taler per point");
        helper.assertTrue(RepairStation.pricePerPointMilli(55, 2.0, 0.5) == 938, "leather cap: 0.938 taler per point");
        long diamond = RepairStation.pricePerPointMilli(1561, 2.0, 0.5);
        helper.assertTrue(diamond == 4998, "diamond tools: 4.998 taler per point, got " + diamond);
        helper.assertTrue(RepairStation.pricePerPointMilli(1561, 2.0, 0.0) == 2000 && RepairStation.pricePerPointMilli(55, 2.0, 0.0) == 2000,
                "exponent 0 is a flat rate");
        helper.assertTrue(RepairStation.pricePerPointMilli(new ItemStack(Items.IRON_PICKAXE)) == 2000, "default config uses the iron reference");

        RepairStation.Quote silver = RepairStation.quote(200, 2000, Taler.SILVER_TALER.value, 1);
        helper.assertTrue(silver.equals(new RepairStation.Quote(200, 400, 1, 329, true)), "one silver taler repairs 200 points, 329 change: " + silver);
        RepairStation.Quote sacks = RepairStation.quote(200, 2000, Taler.COPPER_SACK.value, 2);
        helper.assertTrue(sacks.equals(new RepairStation.Quote(81, 162, 2, 0, false)), "two copper sacks: 81 points, no change: " + sacks);
        RepairStation.Quote cap = RepairStation.quote(50, 938, Taler.COPPER_TALER.value, 10);
        helper.assertTrue(cap.equals(new RepairStation.Quote(10, 10, 10, 0, false)), "ten copper taler on a leather cap: 10 points: " + cap);
        RepairStation.Quote full = RepairStation.quote(1561, diamond, Taler.SILVER_SACK.value, 1);
        helper.assertTrue(full.cost() == 7802 && full.full() && full.change() == Taler.SILVER_SACK.value - 7802,
                "a diamond tool costs 7.802 for its whole durability: " + full);
        RepairStation.Quote coins = RepairStation.quote(200, 2000, Taler.COPPER_SACK.value, 64);
        helper.assertTrue(coins.coins() == 5 && coins.full() && coins.change() == 5, "only as many coins as needed are taken: " + coins);
        helper.assertTrue(RepairStation.quote(100, diamond, Taler.COPPER_TALER.value, 1).equals(RepairStation.Quote.NONE),
                "a copper taler does not pay for a diamond point");
        helper.assertTrue(RepairStation.quote(0, 2000, 729, 1).equals(RepairStation.Quote.NONE), "nothing to repair");
        helper.succeed();
    }

    /** Wann die Reparaturbank nicht repariert — und dass jede Stückelung als Bezahlung zählt. */
    private static void repairStationRejects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack coin = new ItemStack(Taler.SILVER_TALER.item());
        ItemStack damaged = new ItemStack(Items.IRON_PICKAXE);
        damaged.setDamageValue(100);
        helper.assertTrue(RepairStation.check(ItemStack.EMPTY, coin) == RepairStation.Blocked.NO_ITEM, "empty slot");
        helper.assertTrue(RepairStation.check(new ItemStack(Items.STICK), coin) == RepairStation.Blocked.NOT_REPAIRABLE, "a stick");
        helper.assertFalse(RepairStation.accepts(new ItemStack(Items.STICK)), "a stick does not fit into the slot");
        helper.assertTrue(RepairStation.accepts(new ItemStack(ModItems.FIRE_EXTINGUISHER.get())), "contraband is recharged here");
        helper.assertTrue(RepairStation.check(new ItemStack(Items.IRON_PICKAXE), coin) == RepairStation.Blocked.NOT_DAMAGED, "undamaged pickaxe");
        helper.assertTrue(RepairStation.check(scamCursed(level, damaged.copy()), coin) == RepairStation.Blocked.LOCKED, "scam-cursed pickaxe");
        ItemStack bound = enchanted(level, Items.IRON_HELMET, false, Enchantments.BINDING_CURSE, 1);
        bound.setDamageValue(50);
        helper.assertTrue(RepairStation.check(bound, coin) == null, "vanilla curses are repaired like anything else");
        helper.assertTrue(RepairStation.check(damaged, ItemStack.EMPTY) == RepairStation.Blocked.NO_PAYMENT, "no money");
        helper.assertTrue(RepairStation.check(damaged, new ItemStack(Items.DIAMOND)) == RepairStation.Blocked.NO_PAYMENT, "diamonds are no money");
        ItemStack diamondPick = new ItemStack(Items.DIAMOND_PICKAXE);
        diamondPick.setDamageValue(100);
        helper.assertTrue(RepairStation.check(diamondPick, new ItemStack(Taler.COPPER_TALER.item())) == RepairStation.Blocked.COIN_TOO_SMALL,
                "one copper taler does not pay a diamond point");
        for (Taler t : Taler.values()) {
            helper.assertTrue(RepairStation.isPayment(new ItemStack(t.item())), t.id + " is accepted");
            helper.assertTrue(RepairStation.check(damaged, new ItemStack(t.item(), 64)) == null, "repair works with " + t.id);
        }
        helper.succeed();
    }

    /** Eine Reparatur am Block: Haltbarkeit, verbrauchte Münzen und Wechselgeld im Inventar; gesperrt passiert nichts. */
    private static void repairStationRepair(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SimpleContainer full = repairSlots(new ItemStack(Items.IRON_PICKAXE), 200, Taler.SILVER_TALER, 1);
        RepairStation.repair(level.getServer(), player, full);
        helper.assertTrue(full.getItem(RepairStation.SLOT_ITEM).getDamageValue() == 0, "fully repaired");
        helper.assertTrue(full.getItem(RepairStation.SLOT_PAYMENT).isEmpty(), "the silver taler is spent");
        helper.assertTrue(talerInInventory(player) == 329, "329 taler change in the inventory: " + talerInInventory(player));

        Player other = helper.makeMockPlayer(GameType.SURVIVAL);
        SimpleContainer partial = repairSlots(new ItemStack(Items.IRON_PICKAXE), 200, Taler.COPPER_SACK, 2);
        RepairStation.repair(level.getServer(), other, partial);
        helper.assertTrue(partial.getItem(RepairStation.SLOT_ITEM).getDamageValue() == 119, "81 points repaired");
        helper.assertTrue(partial.getItem(RepairStation.SLOT_PAYMENT).isEmpty(), "both sacks are spent");
        helper.assertTrue(talerInInventory(other) == 0, "no change for an exact payment");

        SimpleContainer blocked = repairSlots(new ItemStack(Items.IRON_PICKAXE), 0, Taler.SILVER_TALER, 1);
        RepairStation.repair(level.getServer(), other, blocked);
        helper.assertTrue(blocked.getItem(RepairStation.SLOT_PAYMENT).getCount() == 1, "an undamaged item costs nothing");
        helper.succeed();
    }

    // --- Schwarzmarkt --------------------------------------------------------------------------

    private static Holder<Enchantment> enchantment(ServerLevel level, ResourceKey<Enchantment> key) {
        return level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    private static SimpleContainer blackmarketSlots(ItemStack item, int pouches) {
        SimpleContainer slots = new SimpleContainer(2);
        slots.setItem(Blackmarket.SLOT_ITEM, item);
        slots.setItem(Blackmarket.SLOT_PAYMENT, new ItemStack(Taler.SILVER_POUCH.item(), pouches));
        return slots;
    }

    private static ItemStack scamCursed(ServerLevel level, ItemStack stack) {
        ScamCurse.apply(stack, level.registryAccess());
        return stack;
    }

    /** Ein Erfolg hebt genau eine Verzauberung unter ihrer Höchststufe um eins und kostet genau einen Silberbeutel. */
    private static void blackmarketUpgrade(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> sharpness = enchantment(level, Enchantments.SHARPNESS);
        Holder<Enchantment> unbreaking = enchantment(level, Enchantments.UNBREAKING);
        SimpleContainer slots = blackmarketSlots(enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 3, Enchantments.UNBREAKING, 3), 2);
        helper.assertTrue(Blackmarket.candidates(slots.getItem(0)).equals(List.of(sharpness)), "unbreaking III is already at its highest level");
        Component message = Blackmarket.apply(Blackmarket.Outcome.SUCCESS, slots, level.registryAccess(), RandomSource.create(1));
        ItemStack sword = slots.getItem(Blackmarket.SLOT_ITEM);
        helper.assertTrue(sword.getEnchantments().getLevel(sharpness) == 4, "sharpness goes up to IV: " + sword.getEnchantments());
        helper.assertTrue(sword.getEnchantments().getLevel(unbreaking) == 3, "unbreaking stays at III");
        helper.assertTrue(slots.getItem(Blackmarket.SLOT_PAYMENT).getCount() == 1, "one pouch is paid");
        helper.assertFalse(ScamCurse.isCursed(sword), "a success does not curse");
        helper.assertTrue(message.getString().contains(Enchantment.getFullname(sharpness, 4).getString()), "the message names the raised enchantment: " + message.getString());

        // Mehrere Kandidaten: Jede Runde steigt genau eine um eins, nie über die Höchststufe.
        RandomSource random = RandomSource.create(7);
        SimpleContainer many = blackmarketSlots(enchanted(level, Items.DIAMOND_PICKAXE, false, Enchantments.EFFICIENCY, 1, Enchantments.UNBREAKING, 1,
                Enchantments.FORTUNE, 1), 64);
        for (int round = 1; round <= 8; round++) {
            Blackmarket.apply(Blackmarket.Outcome.SUCCESS, many, level.registryAccess(), random);
            ItemEnchantments e = many.getItem(0).getEnchantments();
            int total = e.entrySet().stream().mapToInt(it.unimi.dsi.fastutil.objects.Object2IntMap.Entry::getIntValue).sum();
            helper.assertTrue(total == 3 + round, "round " + round + " raises exactly one level: " + e);
            for (var entry : e.entrySet()) {
                helper.assertTrue(entry.getIntValue() <= entry.getKey().value().getMaxLevel(), "never above the highest level: " + e);
            }
        }
        helper.assertTrue(Blackmarket.check(null, many.getItem(0), many.getItem(1)) == Blackmarket.Blocked.MAXED,
                "efficiency V, unbreaking III and fortune III are all at their highest level");
        helper.succeed();
    }

    /** Wann der Schwarzmarkt nicht handelt — und dass nur ein Silberbeutel als Bezahlung zählt. */
    private static void blackmarketRejects(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack pouch = new ItemStack(Taler.SILVER_POUCH.item());
        ItemStack sharp = enchanted(level, Items.IRON_SWORD, false, Enchantments.SHARPNESS, 2);
        helper.assertTrue(Blackmarket.check(null, ItemStack.EMPTY, pouch) == Blackmarket.Blocked.NO_ITEM, "empty slot");
        helper.assertTrue(Blackmarket.check(null, new ItemStack(Items.IRON_SWORD), pouch) == Blackmarket.Blocked.NOT_ENCHANTED, "plain sword");
        ItemStack book = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.SHARPNESS, 2);
        helper.assertTrue(Blackmarket.check(null, book, pouch) == Blackmarket.Blocked.NOT_ENCHANTED, "books do not count");
        helper.assertFalse(Blackmarket.accepts(book), "books do not fit into the slot");
        helper.assertTrue(Blackmarket.check(null, enchanted(level, Items.IRON_SWORD, false, Enchantments.SHARPNESS, 5), pouch) == Blackmarket.Blocked.MAXED,
                "sharpness V");
        ItemStack bound = enchanted(level, Items.IRON_HELMET, false, Enchantments.BINDING_CURSE, 1);
        helper.assertTrue(Blackmarket.candidates(bound).isEmpty(), "curses never go up");
        helper.assertTrue(Blackmarket.check(null, bound, pouch) == Blackmarket.Blocked.CURSED, "cursed items are refused");
        helper.assertTrue(Blackmarket.check(null, scamCursed(level, sharp.copy()), pouch) == Blackmarket.Blocked.CURSED, "scam-cursed item");
        helper.assertTrue(Blackmarket.check(null, enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 3, Enchantments.VANISHING_CURSE, 1), pouch)
                == Blackmarket.Blocked.CURSED, "vanishing curse");
        helper.assertTrue(Blackmarket.check(null, enchanted(level, Items.DIAMOND_CHESTPLATE, false, Enchantments.PROTECTION, 3, Enchantments.BINDING_CURSE, 1), pouch)
                == Blackmarket.Blocked.CURSED, "binding curse");
        for (Taler wrong : List.of(Taler.SILVER_TALER, Taler.COPPER_SACK, Taler.SILVER_SACK, Taler.GOLD_TALER)) {
            helper.assertTrue(Blackmarket.check(null, sharp, new ItemStack(wrong.item(), 64)) == Blackmarket.Blocked.NO_PAYMENT, "no payment with " + wrong.id);
            helper.assertFalse(Blackmarket.isPayment(new ItemStack(wrong.item())), wrong.id + " does not fit into the payment slot");
        }
        helper.assertTrue(Blackmarket.check(null, sharp, ItemStack.EMPTY) == Blackmarket.Blocked.NO_PAYMENT, "no pouch");
        helper.assertTrue(Blackmarket.check(null, sharp, pouch) == null, "sharpness II with a silver pouch works");
        helper.succeed();
    }

    /** Einmal pro Markttag für den ganzen Server, egal wie der Handel ausgeht; am nächsten Markttag wieder offen. */
    private static void blackmarketDailyLimit(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlackmarketData data = BlackmarketData.get(level.getServer());
        int old = data.lastUseDay();
        int today = BlackmarketData.today(level.getServer());
        try {
            data.setLastUseDay(today - 1);
            Player dealer = helper.makeMockPlayer(GameType.SURVIVAL);
            SimpleContainer slots = blackmarketSlots(enchanted(level, Items.IRON_SWORD, false, Enchantments.SHARPNESS, 1), 2);
            helper.assertTrue(Blackmarket.check(level.getServer(), slots.getItem(0), slots.getItem(1)) == null, "open after yesterday's deal");
            Blackmarket.trade(level.getServer(), dealer, slots, RandomSource.create(3));
            helper.assertTrue(data.lastUseDay() == today, "the deal uses up today");
            helper.assertTrue(slots.getItem(1).getCount() == 1, "the deal took one pouch");

            SimpleContainer second = blackmarketSlots(enchanted(level, Items.IRON_SWORD, false, Enchantments.SHARPNESS, 1), 1);
            helper.assertTrue(Blackmarket.check(level.getServer(), second.getItem(0), second.getItem(1)) == Blackmarket.Blocked.USED_TODAY,
                    "closed for everyone for the rest of the day");
            Blackmarket.trade(level.getServer(), helper.makeMockPlayer(GameType.SURVIVAL), second, RandomSource.create(3));
            helper.assertTrue(second.getItem(1).getCount() == 1 && second.getItem(0).getEnchantments().getLevel(enchantment(level, Enchantments.SHARPNESS)) == 1,
                    "a refused deal costs nothing and changes nothing");

            data.setLastUseDay(today - 1);
            helper.assertTrue(Blackmarket.check(level.getServer(), second.getItem(0), second.getItem(1)) == null, "open again on the next market day");
            helper.assertTrue(BlackmarketData.ticksUntilNextDay(level.getServer()) > 0 && BlackmarketData.ticksUntilNextDay(level.getServer()) <= 24000,
                    "time until the next day fits into one day");
        } finally {
            data.setLastUseDay(old);
        }
        helper.succeed();
    }

    /**
     * Die Scams einzeln und die Verteilung der Würfe: ohne Maut 80 % Erfolg, 4 % Diebstahl, je 8 % „nichts“ und Fluch;
     * mit Maut (Werkzeuge) 80 % Erfolg, 20/7 % Diebstahl und je 40/7 % für die drei anderen.
     */
    private static void blackmarketOutcomes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> sharpness = enchantment(level, Enchantments.SHARPNESS);

        SimpleContainer steal = blackmarketSlots(enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 3), 1);
        Blackmarket.apply(Blackmarket.Outcome.STEAL, steal, level.registryAccess(), RandomSource.create(1));
        helper.assertTrue(steal.getItem(0).isEmpty() && steal.getItem(1).isEmpty(), "steal: item and pouch are gone");

        ItemStack before = enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 3);
        SimpleContainer nothing = blackmarketSlots(before.copy(), 1);
        Blackmarket.apply(Blackmarket.Outcome.NOTHING, nothing, level.registryAccess(), RandomSource.create(1));
        helper.assertTrue(ItemStack.matches(nothing.getItem(0), before) && nothing.getItem(1).isEmpty(), "nothing: pouch gone, item unchanged");

        SimpleContainer curse = blackmarketSlots(enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 3), 1);
        Blackmarket.apply(Blackmarket.Outcome.CURSE, curse, level.registryAccess(), RandomSource.create(1));
        ItemStack cursed = curse.getItem(0);
        helper.assertTrue(cursed.getEnchantments().getLevel(sharpness) == 4 && ScamCurse.isCursed(cursed) && curse.getItem(1).isEmpty(),
                "curse: sharpness IV plus the scam curse: " + cursed.getEnchantments());
        helper.assertTrue(Blackmarket.check(null, cursed, new ItemStack(Taler.SILVER_POUCH.item())) == Blackmarket.Blocked.CURSED,
                "a cursed item is never accepted again");

        SimpleContainer toll = blackmarketSlots(enchanted(level, Items.DIAMOND_PICKAXE, false, Enchantments.EFFICIENCY, 3), 1);
        Blackmarket.apply(Blackmarket.Outcome.TOLL, toll, level.registryAccess(), RandomSource.create(1));
        ItemStack tolled = toll.getItem(0);
        helper.assertTrue(tolled.getEnchantments().getLevel(enchantment(level, Enchantments.EFFICIENCY)) == 4 && TollCurse.isCursed(tolled)
                && !ScamCurse.isCursed(tolled) && toll.getItem(1).isEmpty(), "toll: efficiency IV plus the toll curse: " + tolled.getEnchantments());
        helper.assertTrue(Blackmarket.check(null, tolled, new ItemStack(Taler.SILVER_POUCH.item())) == Blackmarket.Blocked.CURSED,
                "a tolled tool is never accepted again");

        RandomSource random = RandomSource.create(42);
        int rolls = 100_000;
        double[][] expected = {{0.80, 0.04, 0.08, 0.08, 0.0}, {0.80, 0.2 / 7, 0.4 / 7, 0.4 / 7, 0.4 / 7}};
        for (int variant = 0; variant < 2; variant++) {
            Map<Blackmarket.Outcome, Integer> counts = new java.util.EnumMap<>(Blackmarket.Outcome.class);
            for (int i = 0; i < rolls; i++) {
                counts.merge(Blackmarket.roll(random, 0.2, 1, 2, 2, variant * 2), 1, Integer::sum);
            }
            for (Blackmarket.Outcome o : Blackmarket.Outcome.values()) {
                double share = counts.getOrDefault(o, 0) / (double) rolls;
                double want = expected[variant][o.ordinal()];
                helper.assertTrue(Math.abs(share - want) < 0.01, o + " share " + share + " expected " + want + " (variant " + variant + ")");
            }
        }
        helper.assertTrue(TollCurse.canApply(new ItemStack(Items.IRON_PICKAXE)) && TollCurse.canApply(new ItemStack(Items.SHEARS)),
                "pickaxes and shears can get the toll");
        helper.assertFalse(TollCurse.canApply(new ItemStack(Items.DIAMOND_SWORD)), "a sword never gets the toll");
        helper.assertFalse(TollCurse.canApply(tolled), "the toll does not stack");
        for (int i = 0; i < 1000; i++) {
            helper.assertTrue(Blackmarket.roll(random, 0.0, 1, 2, 2, 2) == Blackmarket.Outcome.SUCCESS, "no scams without a scam chance");
            helper.assertTrue(Blackmarket.roll(random, 1.0, 0, 0, 0, 0) == Blackmarket.Outcome.SUCCESS, "no scams without weights");
            helper.assertTrue(Blackmarket.roll(random, 1.0, 0, 0, 1, 0) == Blackmarket.Outcome.CURSE, "only the curse is weighted");
            helper.assertTrue(Blackmarket.roll(random, 1.0, 0, 0, 0, 1) == Blackmarket.Outcome.TOLL, "only the toll is weighted");
        }
        helper.succeed();
    }

    /** Ein verfluchtes Item zählt für keinen Contract, auch wenn es die Bedingung erfüllt. */
    private static void scamCurseContract(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Contract sword = Contract.offer(Identifier.withDefaultNamespace("diamond_sword"), matchers(level, """
                { "predicates": { "minecraft:enchantments": [ { "enchantments": "minecraft:sharpness", "levels": { "min": 3 } } ] } }"""),
                1, 1000, 4, 0.0);
        ItemStack sharp = enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 4);
        helper.assertTrue(sword.matches(sharp), "the uncursed sword counts");
        helper.assertFalse(sword.matches(scamCursed(level, sharp.copy())), "the cursed sword does not");
        Contract anyBook = Contract.offer(Identifier.withDefaultNamespace("enchanted_book"), DataComponentMatchers.ANY, 1, 1000, 4, 0.0);
        ItemStack book = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.MENDING, 1);
        helper.assertTrue(anyBook.matches(book), "any book");
        helper.assertFalse(anyBook.matches(scamCursed(level, book.copy())), "not a cursed book");
        helper.succeed();
    }

    /** Im Amboss lässt sich ein verfluchtes Item mit nichts kombinieren, nur umbenennen. */
    private static void scamCurseAnvil(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player smith = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack sword = enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 3);
        ItemStack cursedSword = scamCursed(level, sword.copy());
        ItemStack book = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.UNBREAKING, 2);
        ItemStack cursedBook = scamCursed(level, book.copy());
        ItemStack wornCursed = cursedSword.copy();
        wornCursed.setDamageValue(500);

        helper.assertFalse(anvil(smith, sword, book, null).isEmpty(), "control: an uncursed sword takes the book");
        helper.assertTrue(anvil(smith, cursedSword, book, null).isEmpty(), "cursed sword + book");
        helper.assertTrue(anvil(smith, sword, cursedBook, null).isEmpty(), "sword + cursed book");
        helper.assertTrue(anvil(smith, cursedSword, sword, null).isEmpty(), "cursed sword + sword");
        helper.assertTrue(anvil(smith, sword, cursedSword, null).isEmpty(), "sword + cursed sword");
        helper.assertTrue(anvil(smith, wornCursed, new ItemStack(Items.DIAMOND, 2), null).isEmpty(), "no repair with material");
        ItemStack renamed = anvil(smith, cursedSword, ItemStack.EMPTY, "Pech gehabt");
        helper.assertTrue(!renamed.isEmpty() && ScamCurse.isCursed(renamed), "renaming still works and keeps the curse");
        helper.succeed();
    }

    private static ItemStack anvil(Player player, ItemStack left, ItemStack right, String name) {
        AnvilMenu menu = new AnvilMenu(0, player.getInventory());
        menu.getSlot(0).set(left.copy());
        menu.getSlot(1).set(right.copy());
        if (name != null) {
            menu.setItemName(name);
        }
        return menu.getSlot(2).getItem();
    }

    /** Der Fluch ist ein echter Fluch (Schleifstein lässt ihn dran) und taucht nirgends sonst auf. */
    private static void scamCurseTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> curse = enchantment(level, ScamCurse.KEY);
        helper.assertTrue(curse.is(EnchantmentTags.CURSE), "tagged as curse");
        for (var tag : List.of(EnchantmentTags.IN_ENCHANTING_TABLE, EnchantmentTags.TRADEABLE, EnchantmentTags.ON_RANDOM_LOOT,
                EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT, EnchantmentTags.ON_TRADED_EQUIPMENT, EnchantmentTags.NON_TREASURE, EnchantmentTags.TREASURE)) {
            helper.assertFalse(curse.is(tag), "not in " + tag.location());
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GrindstoneMenu grindstone = new GrindstoneMenu(0, player.getInventory());
        grindstone.getSlot(0).set(scamCursed(level, enchanted(level, Items.DIAMOND_SWORD, false, Enchantments.SHARPNESS, 3)));
        ItemStack ground = grindstone.getSlot(2).getItem();
        helper.assertTrue(ScamCurse.isCursed(ground) && ground.getEnchantments().getLevel(enchantment(level, Enchantments.SHARPNESS)) == 0,
                "the grindstone removes sharpness but keeps the curse: " + ground.getEnchantments());
        helper.succeed();
    }

    private static ItemStack tollCursed(ServerLevel level, ItemStack stack) {
        TollCurse.apply(stack, level.registryAccess());
        return stack;
    }

    /** Ein Block kostet einen Kupfertaler aus dem Inventar, ein Beutel gibt Wechselgeld; Gras ist frei. */
    private static void tollCursePay(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos stone = new BlockPos(1, 1, 1);
        BlockPos grass = new BlockPos(2, 1, 1);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, tollCursed(level, new ItemStack(Items.IRON_PICKAXE)));
            player.getInventory().add(new ItemStack(Taler.COPPER_POUCH.item()));
            helper.setBlock(stone, Blocks.STONE);
            player.gameMode.destroyBlock(helper.absolutePos(stone));
            helper.assertTrue(helper.getBlockState(stone).isAir(), "the stone is broken");
            helper.assertTrue(TollCurse.balance(player) == 8, "a pouch pays 1 and returns 8: " + TollCurse.balance(player));
            helper.assertTrue(player.getInventory().countItem(Taler.COPPER_TALER.item()) == 8
                    && player.getInventory().countItem(Taler.COPPER_POUCH.item()) == 0, "the change is copper taler");

            helper.setBlock(grass, Blocks.SHORT_GRASS);
            player.gameMode.destroyBlock(helper.absolutePos(grass));
            helper.assertTrue(TollCurse.balance(player) == 8, "instant blocks are free");

            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            helper.setBlock(stone, Blocks.STONE);
            player.gameMode.destroyBlock(helper.absolutePos(stone));
            helper.assertTrue(TollCurse.balance(player) == 8, "an uncursed pickaxe is free");
        } finally {
            helper.setBlock(stone, Blocks.AIR);
            helper.setBlock(grass, Blocks.AIR);
        }
        helper.succeed();
    }

    /** Ohne Geld ist der langsame Einzelabbau frei, und der Bremsfaktor greift. */
    private static void tollCurseNoCoins(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos stone = new BlockPos(1, 1, 1);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, tollCursed(level, new ItemStack(Items.IRON_PICKAXE)));
            helper.assertFalse(TollCurse.canPay(player), "no taler, no toll");
            helper.assertFalse(TollCurse.pay(player, 1), "paying without taler fails");
            float slow = TollCurse.speed(10.0f, false);
            helper.assertTrue(Math.abs(slow - 10.0f * DtpConfig.TOLL_SLOWDOWN.get().floatValue()) < 1e-4, "slowed down: " + slow);
            helper.assertTrue(TollCurse.speed(10.0f, true) == 10.0f, "full speed with taler");
            helper.setBlock(stone, Blocks.STONE);
            player.gameMode.destroyBlock(helper.absolutePos(stone));
            helper.assertTrue(helper.getBlockState(stone).isAir(), "the single block still breaks without taler");
        } finally {
            helper.setBlock(stone, Blocks.AIR);
        }
        helper.succeed();
    }

    /** Ohne Taler bricht nur der getroffene Block; mit Taler zahlt die ganze Ader einmal Maut. */
    private static void tollCurseChain(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.VEIN_MINING, 2);
        List<BlockPos> vein = List.of(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1), new BlockPos(1, 3, 1), new BlockPos(1, 4, 1),
                new BlockPos(1, 5, 1));
        try {
            helper.assertTrue(PlayerUpgrades.value(player, PlayerUpgrade.VEIN_MINING) >= vein.size(), "vein mining reaches the whole vein");
            vein.forEach(p -> helper.setBlock(p, Blocks.IRON_ORE));
            player.setItemInHand(InteractionHand.MAIN_HAND, tollCursed(level, new ItemStack(Items.DIAMOND_PICKAXE)));
            player.gameMode.destroyBlock(helper.absolutePos(vein.getFirst()));
            long broken = vein.stream().filter(p -> helper.getBlockState(p).isAir()).count();
            helper.assertTrue(broken == 1, "without taler only the hit block breaks, broken: " + broken);

            vein.forEach(p -> helper.setBlock(p, Blocks.IRON_ORE));
            player.getInventory().add(new ItemStack(Taler.COPPER_TALER.item(), 2));
            player.gameMode.destroyBlock(helper.absolutePos(vein.getFirst()));
            broken = vein.stream().filter(p -> helper.getBlockState(p).isAir()).count();
            helper.assertTrue(broken == vein.size(), "one taler pays for the whole vein, broken: " + broken);
            helper.assertTrue(TollCurse.balance(player) == 1, "only one taler is spent: " + TollCurse.balance(player));
        } finally {
            vein.forEach(p -> helper.setBlock(p, Blocks.AIR));
        }
        helper.succeed();
    }

    /** Der Fluch zählt für die Haltbarkeit als eine Stufe Unbreaking mehr; die eingetragenen Verzauberungen bleiben. */
    private static void tollCurseUnbreaking(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> unbreaking = enchantment(level, Enchantments.UNBREAKING);
        ItemStack plain = tollCursed(level, new ItemStack(Items.IRON_PICKAXE));
        ItemStack three = tollCursed(level, enchanted(level, Items.IRON_PICKAXE, false, Enchantments.UNBREAKING, 3));
        helper.assertTrue(plain.getEnchantmentLevel(unbreaking) == 1, "cursed alone counts as unbreaking I: " + plain.getEnchantmentLevel(unbreaking));
        helper.assertTrue(three.getEnchantmentLevel(unbreaking) == 4, "unbreaking III counts as IV: " + three.getEnchantmentLevel(unbreaking));
        helper.assertTrue(three.getEnchantments().getLevel(unbreaking) == 3, "the stored level stays III");
        helper.assertTrue(new ItemStack(Items.IRON_PICKAXE).getEnchantmentLevel(unbreaking) == 0, "control: no curse, no bonus");

        int uses = 20_000;
        long damage = 0;
        for (int i = 0; i < uses; i++) {
            damage += EnchantmentHelper.processDurabilityChange(level, three, 1);
        }
        double share = damage / (double) uses;
        helper.assertTrue(Math.abs(share - 0.2) < 0.02, "wear like unbreaking IV (1/5): " + share);
        helper.succeed();
    }

    /** Ein echter Fluch, nur für Abbauwerkzeuge, und sonst nirgends zu bekommen. */
    private static void tollCurseTags(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> curse = enchantment(level, TollCurse.KEY);
        helper.assertTrue(curse.is(EnchantmentTags.CURSE), "tagged as curse");
        for (var tag : List.of(EnchantmentTags.IN_ENCHANTING_TABLE, EnchantmentTags.TRADEABLE, EnchantmentTags.ON_RANDOM_LOOT,
                EnchantmentTags.ON_MOB_SPAWN_EQUIPMENT, EnchantmentTags.ON_TRADED_EQUIPMENT, EnchantmentTags.NON_TREASURE, EnchantmentTags.TREASURE)) {
            helper.assertFalse(curse.is(tag), "not in " + tag.location());
        }
        helper.assertTrue(curse.value().canEnchant(new ItemStack(Items.IRON_PICKAXE)), "fits a pickaxe");
        helper.assertFalse(curse.value().canEnchant(new ItemStack(Items.IRON_SWORD)), "does not fit a sword");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        GrindstoneMenu grindstone = new GrindstoneMenu(0, player.getInventory());
        grindstone.getSlot(0).set(tollCursed(level, enchanted(level, Items.DIAMOND_PICKAXE, false, Enchantments.EFFICIENCY, 3)));
        ItemStack ground = grindstone.getSlot(2).getItem();
        helper.assertTrue(TollCurse.isCursed(ground) && ground.getEnchantments().getLevel(enchantment(level, Enchantments.EFFICIENCY)) == 0,
                "the grindstone removes efficiency but keeps the curse: " + ground.getEnchantments());

        // Sperren wie beim Scam-Fluch: Contract, Amboss (Umbenennen geht).
        ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        ItemStack cursed = tollCursed(level, pickaxe.copy());
        Contract contract = Contract.offer(Identifier.withDefaultNamespace("iron_pickaxe"), DataComponentMatchers.ANY, 1, 1000, 4, 0.0);
        helper.assertTrue(contract.matches(pickaxe), "control: a plain pickaxe counts");
        helper.assertFalse(contract.matches(cursed), "a tolled pickaxe does not count for a contract");
        ItemStack book = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.EFFICIENCY, 2);
        helper.assertFalse(anvil(player, pickaxe, book, null).isEmpty(), "control: a plain pickaxe takes the book");
        helper.assertTrue(anvil(player, cursed, book, null).isEmpty(), "tolled pickaxe + book");
        helper.assertTrue(anvil(player, pickaxe, cursed, null).isEmpty(), "pickaxe + tolled pickaxe");
        ItemStack worn = cursed.copy();
        worn.setDamageValue(100);
        helper.assertTrue(anvil(player, worn, new ItemStack(Items.IRON_INGOT, 2), null).isEmpty(), "no repair with material");
        ItemStack vanishing = enchanted(level, Items.IRON_PICKAXE, false, Enchantments.VANISHING_CURSE, 1);
        helper.assertTrue(contract.matches(vanishing), "vanilla curses still count for a contract");
        helper.assertFalse(anvil(player, vanishing, book, null).isEmpty(), "vanilla curses still combine in the anvil");
        ItemStack binding = enchanted(level, Items.IRON_CHESTPLATE, false, Enchantments.BINDING_CURSE, 1);
        helper.assertFalse(anvil(player, binding, enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.PROTECTION, 2), null).isEmpty(),
                "binding still combines in the anvil");
        ItemStack renamed = anvil(player, cursed, ItemStack.EMPTY, "Mautpflichtig");
        helper.assertTrue(!renamed.isEmpty() && TollCurse.isCursed(renamed), "renaming still works and keeps the curse");
        helper.succeed();
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> shared = event.registerEnvironment(id("shared"));
        for (String name : TESTS) {
            Holder<TestEnvironmentDefinition<?>> env = ISOLATED.contains(name) ? event.registerEnvironment(id("isolated_" + name)) : shared;
            event.registerTest(id(name), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id(name)),
                    new TestData<>(env, Identifier.withDefaultNamespace("empty"), 100, 0, true)));
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Dtp.MODID, path);
    }

    /**
     * Räumt Kisten eines Tests ab. Der Testserver verwendet Testflächen wieder; stehen gebliebene Kisten
     * würden sonst von der Umkreissuche späterer Tests mitgezählt.
     */
    private static void cleanup(GameTestHelper helper, BlockPos... relative) {
        for (BlockPos rel : relative) {
            if (helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof Container container) {
                container.clearContent();
            }
            helper.setBlock(rel, Blocks.AIR);
        }
    }

    /**
     * Führt {@code body} mit einem gestarteten Durchlauf aus, dessen Beacon am Testort steht, und stellt danach
     * den vorherigen Zustand wieder her. Contracts lassen sich nur im Territorium annehmen und beliefern.
     */
    private static void withTerritory(GameTestHelper helper, Runnable body) {
        WorldProgress progress = WorldProgress.get(helper.getLevel().getServer());
        BlockPos oldBeacon = progress.beaconPos();
        boolean oldStarted = progress.started();
        int oldBorder = progress.level(ServerUpgrade.BORDER);
        progress.setBeacon(helper.absolutePos(BlockPos.ZERO), true);
        progress.setLevel(ServerUpgrade.BORDER, 0);
        try {
            body.run();
        } finally {
            progress.setBeacon(oldBeacon, oldStarted);
            progress.setLevel(ServerUpgrade.BORDER, oldBorder);
        }
    }

    private static long total(List<ItemStack> stacks) {
        return stacks.stream().mapToLong(Taler::valueOf).sum();
    }

    // --- Tests ------------------------------------------------------------------------------

    /** 500 → 6 Kupfersäcke + 1 Kupferbeutel + 5 Kupfertaler, wie in docs/10-markt.md. */
    private static void talerSplit(GameTestHelper helper) {
        List<ItemStack> split = Taler.split(500);
        helper.assertTrue(split.size() == 3, "500 should split into 3 stacks, got " + split);
        helper.assertTrue(split.get(0).is(Taler.COPPER_SACK.item()) && split.get(0).getCount() == 6, "expected 6 copper sacks");
        helper.assertTrue(split.get(1).is(Taler.COPPER_POUCH.item()) && split.get(1).getCount() == 1, "expected 1 copper pouch");
        helper.assertTrue(split.get(2).is(Taler.COPPER_TALER.item()) && split.get(2).getCount() == 5, "expected 5 copper taler");
        helper.assertTrue(Taler.GOLD_SACK.value == 43_046_721L, "gold sack value");
        for (long amount : new long[] {0, 1, 8, 9, 728, 729, 1400, 59_048, 1_000_000, 123_456_789}) {
            helper.assertTrue(total(Taler.split(amount)) == amount, "split must preserve value for " + amount);
        }
        helper.succeed();
    }

    /** Rezepte und Loot-Tables aus dem Mod-Datapack sind geladen und im aktuellen Format gültig. */
    private static void dataLoaded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (String recipe : List.of("copper_pouch_from_copper_taler", "copper_taler_from_copper_pouch",
                "gold_sack_from_gold_pouch", "silver_taler_from_copper_sack",
                "market_stand", "trade_chest", "bulletin_board", "deposit_chest", "upgrade_table", "repair_station", "relay_beacon", "market_scale",
                "beacon", "guide", "slot_machine", "statistics_board", "recycler")) {
            helper.assertTrue(level.recipeAccess().byKey(ResourceKey.create(Registries.RECIPE, id(recipe))).isPresent(),
                    "recipe missing: " + recipe);
        }
        for (String block : List.of("market_stand", "trade_chest", "bulletin_board", "deposit_chest", "upgrade_table", "repair_station", "relay_beacon", "slot_machine",
                "statistics_board", "recycler")) {
            LootTable table = level.getServer().reloadableRegistries().getLootTable(
                    ResourceKey.create(Registries.LOOT_TABLE, id("blocks/" + block)));
            helper.assertTrue(table != LootTable.EMPTY, "loot table missing: " + block);
        }
        helper.assertTrue(Market.quote(level, BlockPos.ZERO, Items.WHEAT, 1).isPresent(), "wheat should be tradable");
        helper.assertTrue(Market.quote(level, BlockPos.ZERO, Items.DIAMOND_SWORD, 1).isEmpty(), "tools must not be tradable");
        helper.succeed();
    }

    /** Innerhalb der Frist 120 %; danach fällt der Bonus weg: ein Intervall 100 %, dann −10 % je Intervall bis 50 %. */
    private static void contractDecay(GameTestHelper helper) {
        Contract c = Contract.offer(Identifier.withDefaultNamespace("wheat"), DataComponentMatchers.ANY, 100, 1000, 1, 0.0);
        double bonus = c.bonusDays();
        double interval = DtpConfig.DECAY_INTERVAL_DAYS.get();
        Contract.Terms standard = Contract.Terms.standard();
        // Faktoren aus der Konfiguration; der Test prüft die Kurve, nicht ihre Startwerte (+20 %, −10 %, 50 %).
        double bonusFactor = DtpConfig.BONUS_FACTOR.get();
        double step = DtpConfig.DECAY_PER_INTERVAL.get();
        double floor = DtpConfig.MIN_FACTOR.get();
        helper.assertTrue(c.payout(0.0) == Math.round(1000 * bonusFactor), "within bonus period: bonus factor, got " + c.payout(0.0));
        helper.assertTrue(c.payout(bonus + interval * 0.5) == 1000, "first interval after the bonus period: 100 %");
        helper.assertTrue(c.payout(bonus + interval * 1.5) == Math.round(1000 * Math.max(floor, 1 - step)), "one full interval overdue: one step less");
        helper.assertTrue(c.payout(bonus + interval * 2.5) == Math.round(1000 * Math.max(floor, 1 - 2 * step)), "two full intervals overdue: two steps less");
        helper.assertTrue(c.payout(bonus + interval * 1000) == Math.round(1000 * floor), "never below the minimum factor");

        // Anzeige: Zeit und Höhe der nächsten Stufe, nahtlos über das Ende der Bonusfrist hinweg.
        helper.assertTrue(Math.abs(c.daysUntilNextDrop(bonus - 0.8) - 0.8) < 1e-9, "in bonus: rest of the bonus period");
        helper.assertTrue(Math.abs(c.nextDropStep(0.0, standard) - (bonusFactor - 1.0)) < 1e-9, "in bonus: the whole bonus is the next step");
        helper.assertTrue(Math.abs(c.daysUntilNextDrop(bonus + interval * 0.25) - interval * 0.75) < 1e-9, "after: rest of the interval");
        helper.assertTrue(Math.abs(c.factor(bonus + interval * 0.25) - 1.0) < 1e-9, "after the bonus period: 100 %");
        helper.assertTrue(Math.abs(c.nextDropStep(bonus + interval * 0.25, standard) - Math.min(step, 1.0 - floor)) < 1e-9, "after: one decay step");
        helper.assertTrue(c.daysUntilNextDrop(bonus + interval * 1000) == -1, "at the minimum nothing drops any more");
        helper.succeed();
    }

    /**
     * Der Marktstand verkauft die Ware aus einem Trade-Chest in Reichweite. Der Erlös entspricht exakt der
     * angezeigten Lagerzeile und landet in der Einzahlkiste — nicht im Trade-Chest.
     */
    private static void marketStockSale(GameTestHelper helper) {
        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        trade.setItem(0, new ItemStack(Items.WHEAT, 64));
        trade.setItem(1, new ItemStack(Items.WHEAT, 10));
        trade.setItem(2, new ItemStack(Items.DIAMOND_SWORD));
        ServerLevel level = helper.getLevel();
        BlockPos market = helper.absolutePos(new BlockPos(1, 1, 0));

        List<Sales.StockLine> stock = Sales.stock(level, market);
        helper.assertTrue(stock.size() == 1 && stock.get(0).item() == Items.WHEAT && stock.get(0).count() == 74,
                "stock should list 74 wheat, got " + stock);
        long expected = stock.get(0).total();

        Sales.Result result = Sales.sellFromChests(level, market, null, null);
        helper.assertTrue(result.count() == 74 && result.revenue() == expected, "sold " + result + ", expected " + expected);
        helper.assertTrue(result.deposited() == expected, "everything should fit into the deposit chest");
        helper.assertTrue(deposit.balance() == expected, "deposit chest holds " + deposit.balance() + ", expected " + expected);
        boolean wheatLeft = false;
        boolean swordLeft = false;
        long coinsInTradeChest = 0;
        for (int i = 0; i < trade.getContainerSize(); i++) {
            ItemStack s = trade.getItem(i);
            wheatLeft |= s.is(Items.WHEAT);
            swordLeft |= s.is(Items.DIAMOND_SWORD);
            coinsInTradeChest += Taler.valueOf(s);
        }
        helper.assertFalse(wheatLeft, "wheat should be sold");
        helper.assertTrue(swordLeft, "unsellable items stay in the trade chest");
        helper.assertTrue(coinsInTradeChest == 0, "no Taler may land in the trade chest");
        cleanup(helper, tradeRel, depositRel);
        helper.succeed();
    }

    /** Steigende Redstone-Flanke am Marktstand verkauft genau einen Stapel aus dem Trade-Chest; Dauersignal nichts weiter. */
    private static void marketRedstone(GameTestHelper helper) {
        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos marketRel = new BlockPos(1, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        helper.setBlock(marketRel, ModBlocks.MARKET_STAND.get());
        TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        trade.setItem(0, new ItemStack(Items.WHEAT, 64));
        trade.setItem(1, new ItemStack(Items.WHEAT, 10));

        helper.setBlock(marketRel.above(), Blocks.REDSTONE_BLOCK);
        helper.assertTrue(helper.getBlockState(marketRel).getValue(MarketStandBlock.TRIGGERED), "market stand should be triggered");
        helper.assertTrue(trade.getItem(0).isEmpty() && trade.getItem(1).getCount() == 10, "exactly the first stack should be sold");
        long afterFirst = deposit.balance();
        helper.assertTrue(afterFirst > 0, "revenue should land in the deposit chest");

        // Ein Nachbar-Update bei anhaltendem Signal löst keinen zweiten Verkauf aus.
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.STONE);
        helper.assertTrue(trade.getItem(1).getCount() == 10 && deposit.balance() == afterFirst, "no second sale while powered");
        cleanup(helper, tradeRel, depositRel, marketRel, marketRel.above(), new BlockPos(1, 1, 1));
        helper.succeed();
    }

    /**
     * Massenware unter einem Taler: Ein Verkauf, der weniger als einen Taler brächte, findet nicht statt — die Items
     * bleiben in Hand oder Kiste. Redstone überspringt so einen Stapel, der Kistenverkauf verkauft den Rest.
     * Außerhalb des Territoriums kostet Seetang 0,71 × 5 % ≈ 0,035 Taler je Stück.
     */
    private static void marketMinimum(GameTestHelper helper) {
        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
        ServerLevel level = helper.getLevel();
        BlockPos market = helper.absolutePos(new BlockPos(1, 1, 0));

        // Anzeige: unter einem Taler mit Nachkommastelle, abgeschnitten; darüber ganzzahlig wie bisher.
        helper.assertTrue(Money.formatPrice(0.71).equals("0,7"), "0.71 shows as 0,7: " + Money.formatPrice(0.71));
        helper.assertTrue(Money.formatPrice(0.96).equals("0,9"), "0.96 is cut, not rounded: " + Money.formatPrice(0.96));
        helper.assertTrue(Money.formatPrice(0.0355).equals("0,03"), "tiny prices get two digits: " + Money.formatPrice(0.0355));
        helper.assertTrue(Money.formatPrice(5.54).equals("6"), "from one Taler on, prices stay whole: " + Money.formatPrice(5.54));

        Market.Quote one = Market.quote(level, market, Items.KELP, 1).orElseThrow();
        helper.assertTrue(one.total() == 0 && one.unitPrice() > 0.0 && one.unitPrice() < 1.0, "one kelp is worth less than a Taler: " + one);
        // Größte Menge Seetang, die zusammen noch unter einem Taler bleibt; hängt nicht an der Preisliste.
        int belowOne = 1;
        while (Market.quote(level, market, Items.KELP, belowOne + 1).orElseThrow().total() < Sales.MIN_REVENUE) {
            belowOne++;
        }
        helper.assertTrue(belowOne >= 2 && belowOne < 64, "a few kelp stay below one Taler, a stack does not: " + belowOne);

        // Aus der Hand: zu wenig wird abgelehnt, die Items bleiben in der Hand.
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.KELP, belowOne));
        Sales.Result single = Sales.sellFromHand(level, market, player, Sales.HandMode.ONE);
        Sales.Result few = Sales.sellFromHand(level, market, player, Sales.HandMode.STACK);
        helper.assertTrue(single.belowMinimum() && few.belowMinimum(), "sales below one Taler are refused");
        helper.assertTrue(player.getMainHandItem().getCount() == belowOne, "refused kelp stays in the hand");
        player.getMainHandItem().setCount(64);
        Sales.Result stack = Sales.sellFromHand(level, market, player, Sales.HandMode.STACK);
        helper.assertTrue(stack.count() == 64 && stack.revenue() >= Sales.MIN_REVENUE && player.getMainHandItem().isEmpty(),
                "a full stack of kelp sells: " + stack);

        // Leer verkaufte Hand: Der nächste Stapel desselben Items rückt aus dem Inventar nach, andere Items nie.
        Inventory inv = player.getInventory();
        int selected = inv.getSelectedSlot();
        int spare = selected == 20 ? 21 : 20;
        int other = selected == 5 ? 6 : 5;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.AMETHYST_SHARD, 64));
        inv.setItem(other, new ItemStack(Items.REDSTONE, 64));
        inv.setItem(spare, new ItemStack(Items.AMETHYST_SHARD, 64));
        try {
            Sales.Result first = Sales.sellFromHand(level, market, player, Sales.HandMode.STACK);
            helper.assertTrue(first.count() == 64 && player.getMainHandItem().is(Items.AMETHYST_SHARD)
                    && player.getMainHandItem().getCount() == 64 && inv.getItem(spare).isEmpty(),
                    "the spare amethyst stack moves into the hand: " + first);
            Sales.Result second = Sales.sellFromHand(level, market, player, Sales.HandMode.STACK);
            helper.assertTrue(second.count() == 64 && player.getMainHandItem().isEmpty(),
                    "with no amethyst left, the hand stays empty: " + second);
            helper.assertTrue(inv.getItem(other).is(Items.REDSTONE) && inv.getItem(other).getCount() == 64,
                    "another sellable item never moves into the hand");
        } finally {
            MarketData.get(level.getServer()).clearPressure(Market.marketKey(Items.AMETHYST_SHARD));
        }

        // Redstone überspringt den Seetang und verkauft den nächsten Stapel.
        trade.setItem(0, new ItemStack(Items.KELP, belowOne));
        trade.setItem(1, new ItemStack(Items.WHEAT, 64));
        Sales.Result redstone = Sales.sellOneStack(level, market);
        helper.assertTrue(redstone.count() == 64 && trade.getItem(1).isEmpty(), "redstone sells the wheat: " + redstone);
        helper.assertTrue(trade.getItem(0).is(Items.KELP) && trade.getItem(0).getCount() == belowOne, "redstone leaves the kelp");
        helper.assertTrue(Sales.sellOneStack(level, market).count() == 0 && trade.getItem(0).getCount() == belowOne,
                "with only kelp left, redstone sells nothing");

        // Kistenverkauf: nur Seetang → abgelehnt; mit Weizen → Weizen verkauft, Seetang bleibt, Vorschau = Erlös.
        helper.assertTrue(Sales.sellFromChests(level, market, null, Items.KELP).belowMinimum(), "kelp alone is refused");
        trade.setItem(1, new ItemStack(Items.WHEAT, 10));
        long preview = Sales.stock(level, market).stream().mapToLong(Sales.StockLine::batchTotal).sum();
        Sales.Result chests = Sales.sellFromChests(level, market, null, null);
        helper.assertTrue(chests.count() == 10 && !chests.belowMinimum() && chests.revenue() == preview,
                "only the wheat is sold, as previewed: " + chests + " vs " + preview);
        helper.assertTrue(trade.getItem(0).is(Items.KELP) && trade.getItem(0).getCount() == belowOne, "the kelp stays in the trade chest");
        cleanup(helper, tradeRel, depositRel);
        helper.succeed();
    }

    /**
     * Auch aus voller Sättigung erholt sich der Preis: Die Erholung läuft im Marktdruck, nicht im Preis, und der
     * Preisfaktor erreicht nie 0. Seetang wird an die Untergrenze gedrückt und steigt mit der Zeit zum Ruhepreis zurück.
     */
    private static void marketFloorRecovery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MarketData data = MarketData.get(level.getServer());
        Identifier kelp = Identifier.withDefaultNamespace("kelp");
        withTerritory(helper, () -> {
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            try {
                data.clearPressure(kelp);
                double resting = Market.fullMarketFactor(level, Items.KELP).orElseThrow();
                data.setPressure(kelp, 1.0e7);
                double floor = Market.fullMarketFactor(level, Items.KELP).orElseThrow();
                helper.assertTrue(floor > 0.0 && Math.abs(floor - DtpConfig.FACTOR_MIN.get()) < 1e-9, "fully saturated kelp sits on the floor: " + floor);
                helper.assertTrue(Market.quote(level, pos, Items.KELP, 1).orElseThrow().total() == 0, "one kelp is worth nothing on the floor");
                helper.assertTrue(Market.quote(level, pos, Items.KELP, 64).orElseThrow().total() >= Sales.MIN_REVENUE,
                        "a stack still sells on the floor");

                double previous = floor;
                for (int i = 0; i < 40; i++) {
                    data.advance(2.0);
                    double now = Market.fullMarketFactor(level, Items.KELP).orElseThrow();
                    helper.assertTrue(now >= previous - 1e-12, "the price never falls while recovering");
                    previous = now;
                }
                helper.assertTrue(Math.abs(previous - resting) < 0.01 * resting, "back at the resting price: " + previous + " vs " + resting);
                helper.assertTrue(Market.quote(level, pos, Items.KELP, 1).orElseThrow().total() >= Sales.MIN_REVENUE,
                        "after recovery one kelp sells again");
            } finally {
                data.clearPressure(kelp);
            }
        });
        helper.succeed();
    }

    /**
     * Contracts werden aus den Trade-Chests in Reichweite des Boards beliefert — auch in Teilen.
     * Beim Abschluss liegt die Belohnung in der Einzahlkiste, nicht im Inventar.
     */
    private static void contractDelivery(GameTestHelper helper) {
        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        ServerLevel level = helper.getLevel();
        BlockPos board = helper.absolutePos(new BlockPos(1, 1, 0));
        double day = MarketData.get(level.getServer()).contractDay();

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Contract contract = Contract.offer(Identifier.withDefaultNamespace("wheat"), DataComponentMatchers.ANY, 150, 1000, 1, day);
        Contracts.of(player).accepted().add(contract);

        trade.setItem(0, new ItemStack(Items.WHEAT, 64));
        trade.setItem(1, new ItemStack(Items.WHEAT, 36));
        Contracts.deliver(level, board, player, contract.id());
        helper.assertTrue(Contracts.of(player).accepted().get(0).delivered() == 0, "no delivery outside the territory");
        withTerritory(helper, () -> Contracts.deliver(level, board, player, contract.id()));
        helper.assertTrue(Contracts.of(player).accepted().get(0).delivered() == 100, "partial delivery of 100");
        helper.assertTrue(trade.isEmpty(), "delivered items leave the trade chest");
        helper.assertTrue(deposit.balance() == 0, "no reward before completion");
        long incomeBefore = todayIncome(level.getServer()).contracts();

        trade.setItem(0, new ItemStack(Items.WHEAT, 60));
        withTerritory(helper, () -> Contracts.deliver(level, board, player, contract.id()));
        helper.assertTrue(Contracts.of(player).accepted().isEmpty(), "contract completed");
        helper.assertTrue(trade.getItem(0).getCount() == 10, "only the missing 50 are taken");
        helper.assertTrue(deposit.balance() == contract.payout(day), "reward " + contract.payout(day) + " in deposit chest, found " + deposit.balance());
        long talerInInventory = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            talerInInventory += Taler.valueOf(player.getInventory().getItem(i));
        }
        helper.assertTrue(talerInInventory == 0, "reward must not go to the inventory when a deposit chest has room");
        helper.assertTrue(Contracts.of(player).completed(1) == 1, "completion counts towards the next tier");
        StatisticsData.PlayerTally tally = StatisticsData.get(level.getServer()).players().get(player.getUUID());
        helper.assertTrue(tally != null && tally.completed(1) == 1 && tally.total() == 1, "the statistics board counts the completion: " + tally);
        long incomeAfter = todayIncome(level.getServer()).contracts();
        helper.assertTrue(incomeAfter == incomeBefore + contract.payout(day),
                "today's income counts the reward: " + incomeBefore + " -> " + incomeAfter);
        cleanup(helper, tradeRel, depositRel);
        helper.succeed();
    }

    /** Eine Bedingung im Format der Contract-Vorlagen, mit Registerzugriff gelesen wie beim Datapack-Laden. */
    private static DataComponentMatchers matchers(ServerLevel level, String json) {
        return DataComponentMatchers.CODEC.codec()
                .parse(level.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(json))
                .getOrThrow();
    }

    private static ItemStack enchanted(ServerLevel level, Item item, boolean stored, Object... enchantmentsAndLevels) {
        ItemStack stack = new ItemStack(item);
        ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (int i = 0; i < enchantmentsAndLevels.length; i += 2) {
            @SuppressWarnings("unchecked")
            ResourceKey<Enchantment> key = (ResourceKey<Enchantment>) enchantmentsAndLevels[i];
            enchantments.set(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key), (Integer) enchantmentsAndLevels[i + 1]);
        }
        stack.set(stored ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        return stack;
    }

    /**
     * Mindestbedingungen der Stufe-4-Contracts: die geforderte Verzauberung in dieser Stufe oder höher, weitere
     * Verzauberungen erlaubt; unbeschädigt; nie umbenannt. Dazu Vergleichsschlüssel, Anzeige und Speichern.
     */
    private static void contractConditions(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Keine feste Anzahl: Es gibt Stufe-4-Vorlagen, und jede davon ergibt einen gültigen Contract.
        List<ContractTemplates.Template> tier4 = ContractTemplates.all().stream().filter(t -> t.hasTier(4)).toList();
        helper.assertFalse(tier4.isEmpty(), "there are tier 4 templates");
        List<String> broken = tier4.stream()
                .filter(t -> ContractBoard.create(t, 4, RandomSource.create(1), 0.0, level.registryAccess()).isEmpty())
                .map(t -> t.item().toString())
                .toList();
        helper.assertTrue(broken.isEmpty(), "tier 4 templates that create no contract: " + broken);

        Contract rod = Contract.offer(Identifier.withDefaultNamespace("fishing_rod"), matchers(level, """
                { "predicates": { "minecraft:enchantments": [ { "enchantments": "minecraft:lure", "levels": { "min": 3 } } ],
                                  "minecraft:damage": { "damage": 0 } } }"""), 1, 1000, 4, 0.0);
        helper.assertFalse(rod.matches(new ItemStack(Items.FISHING_ROD)), "plain rod");
        helper.assertFalse(rod.matches(enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 2)), "lure II is too low");
        helper.assertTrue(rod.matches(enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 3)), "lure III");
        helper.assertTrue(rod.matches(enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 3, Enchantments.UNBREAKING, 3)),
                "other enchantments are allowed");
        helper.assertFalse(rod.matches(enchanted(level, Items.FISHING_ROD, true, Enchantments.LURE, 3)), "stored enchantments do not count on a rod");
        ItemStack damaged = enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 3);
        damaged.setDamageValue(5);
        helper.assertFalse(rod.matches(damaged), "damaged rod");
        ItemStack renamed = enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 3);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Lieblingsangel"));
        helper.assertFalse(rod.matches(renamed), "renamed items are never delivered");
        helper.assertFalse(rod.matches(enchanted(level, Items.DIAMOND_PICKAXE, false, Enchantments.LURE, 3)), "wrong item");

        Contract book = Contract.offer(Identifier.withDefaultNamespace("enchanted_book"), matchers(level, """
                { "predicates": { "minecraft:stored_enchantments": [
                    { "enchantments": "minecraft:sharpness", "levels": { "min": 3 } },
                    { "enchantments": "minecraft:looting", "levels": { "min": 2 } } ] } }"""), 1, 1000, 4, 0.0);
        helper.assertFalse(book.matches(enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.SHARPNESS, 5)), "looting missing");
        helper.assertFalse(book.matches(enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.SHARPNESS, 2, Enchantments.LOOTING, 3)),
                "sharpness too low");
        helper.assertTrue(book.matches(enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.SHARPNESS, 4, Enchantments.LOOTING, 2)),
                "both requirements met");

        Contract anyBook = Contract.offer(Identifier.withDefaultNamespace("enchanted_book"), DataComponentMatchers.ANY, 1, 1000, 4, 0.0);
        helper.assertTrue(anyBook.matches(enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.MENDING, 1)), "any book");
        Contract helmet = Contract.offer(Identifier.withDefaultNamespace("netherite_helmet"), DataComponentMatchers.ANY, 1, 1000, 4, 0.0);
        helper.assertTrue(helmet.matches(new ItemStack(Items.NETHERITE_HELMET)), "plain helmet");
        helper.assertTrue(helmet.matches(enchanted(level, Items.NETHERITE_HELMET, false, Enchantments.PROTECTION, 4)), "enchanted helmet");
        ItemStack worn = new ItemStack(Items.NETHERITE_HELMET);
        worn.setDamageValue(200);
        helper.assertTrue(helmet.matches(worn), "damage does not matter without a damage condition");

        // Invariante: Elytra und Dreizack gibt es nur über Stufe-4-Contracts, immer mit gewürfelten Verzauberungen; abgenutzte
        // Exemplare zählen, denn die gewürfelte Bedingung verlangt kein unbeschädigtes Item.
        for (Item gear : List.of(Items.ELYTRA, Items.TRIDENT)) {
            Identifier gearId = Market.itemId(gear);
            List<ContractTemplates.Template> templates = ContractTemplates.all().stream().filter(t -> t.item().equals(gearId)).toList();
            helper.assertTrue(!templates.isEmpty() && templates.stream().allMatch(t -> t.hasTier(4) && t.randomEnchantments().isPresent()),
                    gearId + " is a tier 4 contract with random enchantments");
            Contract gearContract = ContractBoard.create(templates.getFirst(), 4, RandomSource.create(7), 0.0, level.registryAccess()).orElseThrow();
            helper.assertTrue(gearContract.requiresEnchantments() && !gearContract.requiresUndamaged(), "a worn " + gearId + " still counts");
        }

        // Gleiches Item, andere Bedingung = andere Anforderung; gleiche Bedingung, neu gelesen = gleiche.
        Contract bookAgain = Contract.offer(book.item(), matchers(level, """
                { "predicates": { "minecraft:stored_enchantments": [
                    { "enchantments": "minecraft:sharpness", "levels": { "min": 3 } },
                    { "enchantments": "minecraft:looting", "levels": { "min": 2 } } ] } }"""), 1, 1000, 4, 0.0);
        helper.assertTrue(book.requirement(level.registryAccess()).equals(bookAgain.requirement(level.registryAccess())),
                "same condition, same requirement");
        helper.assertFalse(book.requirement(level.registryAccess()).equals(anyBook.requirement(level.registryAccess())),
                "different condition, different requirement");

        ItemStack display = rod.displayStack(level.registryAccess());
        Holder<Enchantment> lure = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LURE);
        helper.assertTrue(display.getEnchantments().getLevel(lure) == 3, "board shows the minimum enchantment");
        helper.assertTrue(rod.requiresEnchantments() && rod.requiresUndamaged() && !helmet.requiresEnchantments(), "condition hints");

        // Speichern und Laden mit Registerzugriff — so wie Attachment und SavedData es tun.
        var ops = level.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag saved = Contract.CODEC.encodeStart(ops, book).getOrThrow();
        Contract loaded = Contract.CODEC.parse(ops, saved).getOrThrow();
        helper.assertTrue(loaded.matches(enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.SHARPNESS, 3, Enchantments.LOOTING, 2)),
                "condition survives saving");
        // Alte Contracts ohne Bedingung laden mit leerer Bedingung.
        CompoundTag old = (CompoundTag) Contract.CODEC.encodeStart(ops, anyBook).getOrThrow();
        helper.assertTrue(!old.contains("predicates") && Contract.CODEC.parse(ops, old).getOrThrow().match().isEmpty(),
                "contracts without condition stay compatible");
        helper.succeed();
    }

    /** Geliefert wird das schwächste passende Exemplar; umbenannte und zu schwache bleiben in der Trade-Chest. */
    private static void contractConditionDelivery(GameTestHelper helper) {
        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
        ServerLevel level = helper.getLevel();
        BlockPos board = helper.absolutePos(new BlockPos(1, 1, 0));
        double day = MarketData.get(level.getServer()).contractDay();

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Contract contract = Contract.offer(Identifier.withDefaultNamespace("fishing_rod"), matchers(level, """
                { "predicates": { "minecraft:enchantments": [ { "enchantments": "minecraft:lure", "levels": { "min": 3 } } ] } }"""),
                1, 1000, 4, day);
        Contracts.of(player).accepted().add(contract);

        trade.setItem(0, enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 3, Enchantments.MENDING, 1));
        trade.setItem(1, enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 2));
        ItemStack renamed = enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 3);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Lieblingsangel"));
        trade.setItem(2, renamed);
        trade.setItem(3, enchanted(level, Items.FISHING_ROD, false, Enchantments.LURE, 3));
        helper.assertTrue(Sales.countForDelivery(level, board, contract::matches) == 2, "two rods qualify");

        withTerritory(helper, () -> Contracts.deliver(level, board, player, contract.id()));
        helper.assertTrue(Contracts.of(player).accepted().isEmpty(), "contract completed");
        helper.assertTrue(trade.getItem(3).isEmpty(), "the plain lure III rod is delivered");
        helper.assertFalse(trade.getItem(0).isEmpty(), "the rod with mending stays");
        helper.assertFalse(trade.getItem(1).isEmpty() || trade.getItem(2).isEmpty(), "too weak and renamed rods stay");
        cleanup(helper, tradeRel, depositRel);
        helper.succeed();
    }

    /**
     * Die Serveruhren laufen nur mit Spielern online (docs/11-marktdruck.md): Weder ein gestoppter noch ein
     * leerer Server lässt Marktdruck, Aushang oder Marktereignisse altern. Schlafen und Spielzeit mit Spielern schon.
     * Dazu die Migrationskette des Speicherformats.
     */
    private static void clocksPlayers(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        MarketData clock = MarketData.get(server);
        ContractBoard board = ContractBoard.get(server);
        Identifier wheat = Identifier.withDefaultNamespace("wheat");
        boolean hadPressure = clock.hasPressure(wheat);
        double oldPressure = clock.pressure(wheat, 0.0, 1.0);
        clock.setPressure(wheat, 100.0);
        double market = clock.day();
        double contracts = clock.contractDay();
        double rotation = board.daysUntilRotation(server);

        // Server aus: Beim Start wird nur der Bezugspunkt gesetzt, nichts nachgeholt.
        clock.resetClockBaseline(server);
        helper.assertTrue(clock.day() == market && clock.contractDay() == contracts, "a stopped server catches nothing up");

        // Server an, niemand online.
        clock.tick(5.0, List.of());
        helper.assertTrue(clock.day() == market, "empty server: market clock stands");
        helper.assertTrue(clock.contractDay() == contracts, "empty server: contract clock stands");
        helper.assertTrue(clock.pressure(wheat, 0.0, 1.0) == 100.0, "empty server: pressure does not recover");
        helper.assertTrue(board.daysUntilRotation(server) == rotation, "empty server: board rotation does not come closer");
        long seconds = clock.eventSeconds();
        MarketEvents.tick(server);
        helper.assertTrue(clock.eventSeconds() == seconds, "empty server: market event countdown stands");

        // Server an, ein Spieler online.
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        clock.tick(1.0, List.of(player));
        helper.assertTrue(Math.abs(clock.day() - market - 1.0) < 1e-9, "with a player the market clock runs");
        helper.assertTrue(Math.abs(clock.contractDay() - contracts - 1.0) < 1e-9, "with a player the contract clock runs");
        helper.assertTrue(Math.abs(clock.pressure(wheat, 0.0, 1.0) - 50.0) < 1e-6, "with a player pressure recovers (half-life 1 day)");
        helper.assertTrue(rotation == 0.0 || board.daysUntilRotation(server) < rotation, "with a player the board rotation comes closer");

        if (hadPressure) {
            clock.setPressure(wheat, oldPressure);
        } else {
            clock.clearPressure(wheat);
        }

        CompoundTag v1 = new CompoundTag();
        v1.putInt(Versioned.KEY, 1);
        v1.putDouble("day", 42.5);
        MarketData migrated = MarketData.CODEC.parse(NbtOps.INSTANCE, v1).getOrThrow();
        helper.assertTrue(migrated.day() == 42.5 && migrated.contractDay() == 42.5, "v1 data: contract clock starts at market clock");
        CompoundTag v5 = new CompoundTag();
        v5.putInt(Versioned.KEY, 5);
        v5.putDouble("day", 7.0);
        v5.putLong("last_real_millis", 123456789L);
        Tag saved = MarketData.CODEC.encodeStart(NbtOps.INSTANCE, MarketData.CODEC.parse(NbtOps.INSTANCE, v5).getOrThrow()).getOrThrow();
        helper.assertTrue(((CompoundTag) saved).getIntOr(Versioned.KEY, 0) == 8, "saved with the current version");
        helper.assertFalse(((CompoundTag) saved).contains("last_real_millis"), "v6 drops the real-time stamp");
        helper.assertTrue(migrated.knownCategories() == null && migrated.eventQueue().isEmpty(),
                "v7: older data has not synced category unlocks yet");
        helper.assertFalse(migrated.eventScheduled(), "older data has no market event planned yet");
        helper.succeed();
    }

    /**
     * Angenommene Contracts altern nur, während ihr Besitzer online ist — nicht bei gestopptem Server, nicht wenn nur
     * andere spielen. Annehmen und Abbrechen wechseln die Uhr, ohne das Alter zu ändern. Alte Spielstände ohne
     * persönliche Uhr starten auf der serverweiten Contract-Uhr.
     */
    private static void acceptedContractClock(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        MarketData clock = MarketData.get(server);
        Player owner = helper.makeMockPlayer(GameType.SURVIVAL);
        Player other = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerContracts mine = Contracts.of(owner);
        helper.assertTrue(mine.ownDay() == clock.contractDay(), "the personal clock starts at the contract clock");

        // Ein Contract, dessen Bonusfrist gleich abläuft, angenommen wie in Contracts.accept.
        Contract offer = Contract.offer(Identifier.withDefaultNamespace("wheat"), DataComponentMatchers.ANY, 10, 1000, 1,
                clock.contractDay() - 2.0);
        Contract held = offer.rebased(clock.contractDay(), mine.ownDay());
        mine.accepted().add(held);
        double bonusLeft = held.bonusDays() - 2.0;
        long inBonus = held.payout(mine.ownDay());
        helper.assertTrue(inBonus == offer.payout(clock.contractDay()), "accepting keeps the value");

        // Server aus: nichts.
        clock.resetClockBaseline(server);
        helper.assertTrue(held.payout(mine.ownDay()) == inBonus, "a stopped server does not age accepted contracts");
        // Server an, nur ein anderer Spieler online: Serveruhren laufen, der Contract des Besitzers nicht.
        double personal = mine.ownDay();
        double contracts = clock.contractDay();
        clock.tick(bonusLeft + 1.5, List.of(other));
        helper.assertTrue(clock.contractDay() > contracts, "the board ages while others play");
        helper.assertTrue(mine.ownDay() == personal, "the owner's clock stands while only others play");
        helper.assertTrue(held.payout(mine.ownDay()) == inBonus, "the owner loses no value while offline");
        // Besitzer online: der Contract altert.
        clock.tick(bonusLeft + 1.5, List.of(owner, other));
        helper.assertTrue(Math.abs(mine.ownDay() - personal - bonusLeft - 1.5) < 1e-9, "the owner's clock runs while online");
        helper.assertTrue(held.payout(mine.ownDay()) < inBonus, "past the bonus period the value drops");

        // Abbrechen und wieder annehmen: das Alter bleibt.
        double age = mine.ownDay() - held.createdDay();
        Contract returned = held.rebased(mine.ownDay(), clock.contractDay());
        helper.assertTrue(Math.abs(clock.contractDay() - returned.createdDay() - age) < 1e-9, "cancelling keeps the age");
        Contract again = returned.rebased(clock.contractDay(), mine.ownDay());
        helper.assertTrue(Math.abs(mine.ownDay() - again.createdDay() - age) < 1e-9, "re-accepting keeps the age");

        // Admin-Befehl: stellt auch die persönliche Uhr der Eingeloggten vor.
        double beforeAdvance = mine.ownDay();
        clock.advance(1.0, List.of(owner));
        helper.assertTrue(Math.abs(mine.ownDay() - beforeAdvance - 1.0) < 1e-9, "admin advance includes personal clocks");

        // Spielstand v1: ohne persönliche Uhr; sie startet auf der serverweiten Contract-Uhr.
        CompoundTag v1 = new CompoundTag();
        v1.putInt(Versioned.KEY, 1);
        PlayerContracts migrated = PlayerContracts.CODEC.parse(NbtOps.INSTANCE, v1).getOrThrow();
        helper.assertFalse(migrated.started(), "v1 data: personal clock not started yet");
        migrated.ensureStarted(clock.contractDay());
        helper.assertTrue(migrated.ownDay() == clock.contractDay(), "v1 data: personal clock starts at the contract clock");
        Tag saved = PlayerContracts.CODEC.encodeStart(NbtOps.INSTANCE, migrated).getOrThrow();
        helper.assertTrue(((CompoundTag) saved).getIntOr(Versioned.KEY, 0) == 2, "saved with the current version");
        helper.succeed();
    }

    /**
     * Der Aushang rotiert periodisch und wird nach einer Annahme nicht nachgefüllt.
     * Abgebrochene Contracts kehren ohne Fortschritt zurück und werden mit der nächsten Rotation ersetzt.
     */
    private static void boardRotation(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        MarketData clock = MarketData.get(server);
        ContractBoard board = ContractBoard.get(server);
        // Mindestens Stufe 1, sonst bleibt der Aushang leer.
        WorldProgress progress = WorldProgress.get(server);
        int oldTier = progress.level(ServerUpgrade.CONTRACT_TIER);
        progress.setLevel(ServerUpgrade.CONTRACT_TIER, Math.max(oldTier, 1 - DtpConfig.BASE_MAX_TIER.get()));
        int size = ContractBoard.boardSize(server);
        double period = DtpConfig.ROTATION_DAYS.get();

        clock.advance(period + 0.1);
        helper.assertTrue(board.rotateIfDue(server), "rotation should be due");
        helper.assertTrue(board.offers().size() == size, "a fresh board is full: " + board.offers().size());
        helper.assertFalse(board.rotateIfDue(server), "no second rotation right away");
        List<UUID> before = board.offers().stream().map(Contract::id).toList();

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        UUID taken = before.get(0);
        double offeredDay = board.offers().get(0).createdDay();
        ServerLevel level = helper.getLevel();
        BlockPos boardPos = helper.absolutePos(new BlockPos(1, 1, 0));
        Contracts.accept(level, boardPos, player, taken);
        helper.assertTrue(Contracts.of(player).accepted().isEmpty() && board.offers().size() == size,
                "outside the territory the board is read-only");
        withTerritory(helper, () -> Contracts.accept(level, boardPos, player, taken));
        helper.assertTrue(board.offers().size() == size - 1, "accepting must not refill the board");

        // Abbrechen: auch außerhalb erlaubt; zurück in den Aushang, ohne Fortschritt, mit unveränderter Frist.
        Contract held = Contracts.of(player).accepted().get(0);
        Contracts.of(player).accepted().set(0, held.withDelivered(5));
        Contracts.cancel(server, player, taken);
        Contract returned = board.offers().stream().filter(c -> c.id().equals(taken)).findFirst().orElse(null);
        helper.assertTrue(returned != null, "cancelled contract returns to the board");
        helper.assertTrue(returned.delivered() == 0, "without delivery progress");
        helper.assertTrue(Math.abs(returned.createdDay() - offeredDay) < 1e-9, "the bonus period keeps running");
        helper.assertTrue(board.offers().size() == size, "board is full again");

        // Die nächste Rotation ersetzt alles, auch den zurückgegebenen Contract.
        clock.advance(period + 0.1);
        helper.assertTrue(board.rotateIfDue(server), "second rotation should be due");
        List<UUID> after = board.offers().stream().map(Contract::id).toList();
        helper.assertTrue(after.stream().noneMatch(before::contains), "all offers are replaced");
        helper.assertTrue(after.size() == size, "board is full after rotation: " + after.size());
        progress.setLevel(ServerUpgrade.CONTRACT_TIER, oldTier);
        helper.succeed();
    }

    /** Die Flagge des Boards: unten beim Setzen, oben nach einer Rotation, unten nach dem Öffnen. */
    private static void boardFlag(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ServerLevel level = helper.getLevel();
        ContractBoard board = ContractBoard.get(server);
        WorldProgress progress = WorldProgress.get(server);
        int oldTier = progress.level(ServerUpgrade.CONTRACT_TIER);
        progress.setLevel(ServerUpgrade.CONTRACT_TIER, Math.max(oldTier, 1 - DtpConfig.BASE_MAX_TIER.get()));
        board.reroll(server);

        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.BULLETIN_BOARD.get());
        BlockPos pos = helper.absolutePos(rel);
        BulletinBoardBlockEntity entity = (BulletinBoardBlockEntity) level.getBlockEntity(pos);
        helper.assertTrue(entity != null, "bulletin board has a block entity");
        entity.update(level, level.getBlockState(pos));
        helper.assertFalse(level.getBlockState(pos).getValue(BulletinBoardBlock.FLAG), "a new board starts lowered");

        board.reroll(server);
        entity.update(level, level.getBlockState(pos));
        helper.assertTrue(level.getBlockState(pos).getValue(BulletinBoardBlock.FLAG), "a new rotation raises the flag");

        entity.lower();
        helper.assertFalse(level.getBlockState(pos).getValue(BulletinBoardBlock.FLAG), "opening lowers the flag");
        entity.update(level, level.getBlockState(pos));
        helper.assertFalse(level.getBlockState(pos).getValue(BulletinBoardBlock.FLAG), "and it stays down until the next rotation");

        progress.setLevel(ServerUpgrade.CONTRACT_TIER, oldTier);
        helper.succeed();
    }

    /** Die Filtergruppen sind geladen, und jedes Item der Handelsliste steht in mindestens einer Gruppe. */
    private static void marketCategories(GameTestHelper helper) {
        List<MarketCategories.Category> categories = MarketCategories.all();
        helper.assertFalse(categories.isEmpty(), "categories are loaded");
        List<TagKey<Item>> tags = categories.stream().map(c -> TagKey.create(Registries.ITEM, c.tag())).toList();
        List<String> empty = tags.stream().filter(tag -> !BuiltInRegistries.ITEM.getTagOrEmpty(tag).iterator().hasNext())
                .map(tag -> tag.location().toString()).toList();
        helper.assertTrue(empty.isEmpty(), "categories without items: " + empty);
        // Invariante: Diese Zuordnungen sind Designentscheidungen; schlägt hier etwas fehl, ist das eine Designfrage.
        TagKey<Item> farming = TagKey.create(Registries.ITEM, id("market_category/farming"));
        helper.assertTrue(new ItemStack(Items.WHEAT).is(farming), "wheat belongs to farming");
        helper.assertFalse(new ItemStack(Items.IRON_INGOT).is(farming), "iron does not belong to farming");
        helper.assertFalse(new ItemStack(Items.CHORUS_FRUIT).is(farming) || new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).is(farming),
                "chorus fruit and the enchanted golden apple are no farm goods");
        TagKey<Item> construction = TagKey.create(Registries.ITEM, id("market_category/construction"));
        helper.assertTrue(new ItemStack(Items.OAK_LOG).is(construction) && new ItemStack(Items.COBBLESTONE).is(construction),
                "logs and cobblestone are construction goods");
        helper.assertTrue(new ItemStack(Items.ENDER_PEARL).is(TagKey.create(Registries.ITEM, id("market_category/mob_drops")))
                && new ItemStack(Items.ENDER_PEARL).is(TagKey.create(Registries.ITEM, id("market_category/end"))),
                "an item may stand in several categories");
        TagKey<Item> loot = TagKey.create(Registries.ITEM, id("market_category/loot"));
        helper.assertTrue(new ItemStack(Items.CREEPER_HEAD).is(loot) && new ItemStack(Items.DRAGON_HEAD).is(loot),
                "mob heads are loot");
        List<String> uncategorized = TradeList.allTradable().stream()
                .filter(item -> tags.stream().noneMatch(tag -> new ItemStack(item).is(tag)))
                .map(item -> Market.itemId(item).toString())
                .toList();
        helper.assertTrue(uncategorized.isEmpty(), "tradable items without a category: " + uncategorized);
        helper.succeed();
    }

    /**
     * Die Preisliste folgt der Handelsliste: Dateien nach Namen, Einträge in Dateireihenfolge, danach die nur über
     * Tags erfassten Items alphabetisch. Die Sollreihenfolge wird aus den Dateien selbst gelesen, damit neue,
     * verschobene oder gestrichene Einträge den Test nicht brechen — nur eine falsche Sortierung.
     */
    private static void tradeListOrder(GameTestHelper helper) {
        var manager = helper.getLevel().getServer().getResourceManager();
        var converter = net.minecraft.resources.FileToIdConverter.json("dtp/trade_list");
        List<Item> fileOrder = new ArrayList<>();
        Set<Item> listed = new HashSet<>();
        converter.listMatchingResources(manager).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(converter::fileToId)))
                .forEach(file -> {
                    try (Reader reader = file.getValue().openAsReader()) {
                        for (JsonElement entry : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("entries")) {
                            JsonObject object = entry.getAsJsonObject();
                            if (object.has("item")) {
                                BuiltInRegistries.ITEM.get(Identifier.parse(object.get("item").getAsString()))
                                        .filter(ref -> listed.add(ref.value()))
                                        .ifPresent(ref -> fileOrder.add(ref.value()));
                            }
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("trade list " + file.getKey() + " is unreadable", e);
                    }
                });
        List<Item> all = TradeList.allTradable();
        helper.assertTrue(fileOrder.size() > 1 && all.size() >= fileOrder.size(), "trade list is loaded: " + fileOrder.size() + " entries");
        helper.assertTrue(all.subList(0, fileOrder.size()).equals(fileOrder), "direct entries follow the files by name, then their line order");
        List<String> tagItems = all.subList(fileOrder.size(), all.size()).stream().map(item -> Market.itemId(item).toString()).toList();
        helper.assertTrue(tagItems.stream().sorted().toList().equals(tagItems), "items only covered by tags follow alphabetically");
        for (int i = 0; i < all.size(); i++) {
            helper.assertTrue(TradeList.position(all.get(i)) == i, "position matches the list for " + Market.itemId(all.get(i)));
        }
        // Invariante: Elytra und Dreizack gibt es nur über Contracts.
        helper.assertTrue(TradeList.position(Items.ELYTRA) == Integer.MAX_VALUE && TradeList.position(Items.TRIDENT) == Integer.MAX_VALUE,
                "elytra and trident are sold only through contracts");
        helper.assertTrue(TradeList.position(Items.COMMAND_BLOCK) == Integer.MAX_VALUE, "untradable items sort last");
        helper.succeed();
    }

    /**
     * Böses Banner: handelbar über die Komponenten-Bedingung der Handelsliste — geprüft am echten Banner aus dem
     * Spiel, damit eine Änderung von Muster oder Komponenten durch Mojang auffällt. Das schlichte weiße Banner und
     * ein am Webstuhl nachgebautes Muster bleiben unverkäuflich, auch beim Verkauf aus der Trade-Chest.
     */
    private static void ominousBanner(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack ominous = Raid.getOminousBannerInstance(level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN));
        helper.assertTrue(TradeList.lookup(ominous).isPresent(), "the ominous banner of a patrol is tradable");
        helper.assertTrue(TradeList.lookup(new ItemStack(Items.BANNER.white())).isEmpty(), "a plain white banner is not");
        ItemStack loomCopy = new ItemStack(Items.BANNER.white());
        loomCopy.set(DataComponents.BANNER_PATTERNS, ominous.get(DataComponents.BANNER_PATTERNS));
        helper.assertTrue(TradeList.lookup(loomCopy).isEmpty(), "a loom copy without the fixed name is not");
        ItemStack renamed = ominous.copy();
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Beute"));
        helper.assertTrue(TradeList.lookup(renamed).isPresent(), "further components do not matter");
        helper.assertTrue(ItemStack.isSameItemSameComponents(TradeList.displayStack(Items.BANNER.white()), ominous),
                "the market shows the ominous banner with pattern and name");

        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        MarketData data = MarketData.get(level.getServer());
        Identifier key = Market.marketKey(Items.BANNER.white());
        withTerritory(helper, () -> {
            try {
                data.clearPressure(key);
                helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
                helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
                TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
                trade.setItem(0, ominous.copyWithCount(2));
                trade.setItem(1, new ItemStack(Items.BANNER.white(), 3));
                trade.setItem(2, loomCopy.copy());
                BlockPos market = helper.absolutePos(new BlockPos(1, 1, 0));
                List<Sales.StockLine> stock = Sales.stock(level, market);
                helper.assertTrue(stock.size() == 1 && stock.get(0).count() == 2, "only the ominous banners are stock: " + stock);
                Sales.Result result = Sales.sellFromChests(level, market, null, null);
                helper.assertTrue(result.count() == 2 && result.revenue() > 0, "two ominous banners sold: " + result);
                helper.assertTrue(trade.getItem(1).getCount() == 3 && !trade.getItem(2).isEmpty(), "plain and loom banners stay");
            } finally {
                data.clearPressure(key);
                cleanup(helper, tradeRel, depositRel);
            }
        });
        helper.succeed();
    }

    /**
     * Gemeinsame Ware: Items mit demselben {@code key} teilen sich Marktdruck und Preisverlauf, behalten aber ihren
     * Basiswert. Ein Marktereignis entlastet die Ware einmal, die Lagervorschau rechnet die Items nacheinander,
     * und alter Druck je Item wird beim ersten Zugriff übernommen.
     */
    private static void sharedGoods(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MarketData data = MarketData.get(level.getServer());
        Identifier sherds = id("pottery_sherd");
        Identifier farmable = id("farmable_music_disc");
        Identifier angler = Identifier.withDefaultNamespace("angler_pottery_sherd");
        Identifier structureDiscs = id("structure_music_disc");

        // Invariante: alle Creeper-Platten plus tears und lava_chicken sind farmbar, Pigstep nicht.
        TagKey<Item> creeperDiscs = TagKey.create(Registries.ITEM, Identifier.withDefaultNamespace("creeper_drop_music_discs"));
        List<Item> farmableDiscs = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(creeperDiscs).forEach(h -> farmableDiscs.add(h.value()));
        helper.assertTrue(farmableDiscs.size() >= 12, "the creeper disc tag is loaded: " + farmableDiscs.size());
        farmableDiscs.add(Items.MUSIC_DISC_TEARS);
        farmableDiscs.add(Items.MUSIC_DISC_LAVA_CHICKEN);
        for (Item disc : farmableDiscs) {
            helper.assertTrue(Market.marketKey(disc).equals(farmable), Market.itemId(disc) + " should be a farmable disc");
        }
        helper.assertTrue(Market.marketKey(Items.MUSIC_DISC_PIGSTEP).equals(id("structure_music_disc")), "pigstep is a structure disc");
        helper.assertTrue(TradeList.members(farmable).size() == farmableDiscs.size(), "no other disc is farmable");
        assertSharedGood(helper, sherds, item -> new ItemStack(item).is(ItemTags.DECORATED_POT_SHERDS));
        assertSharedGood(helper, id("armor_trim"), item -> Market.itemId(item).getPath().endsWith("_smithing_template"));
        helper.assertTrue(Market.marketKey(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).equals(id("armor_trim")), "the netherite template is an armor trim good");
        helper.assertTrue(Market.marketKey(Items.CARROT).equals(Identifier.withDefaultNamespace("carrot")), "without key the item id is the key");
        helper.assertFalse(TradeList.shared(Items.CARROT), "a carrot shares its market with nobody");

        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        withTerritory(helper, () -> {
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            try {
                data.clearPressure(sherds);
                data.clearPressure(angler);
                data.clearPressure(id("armor_trim"));
                double carrotBefore = Market.fullMarketFactor(level, Items.CARROT).orElseThrow();
                double archerBefore = Market.fullMarketFactor(level, Items.ARCHER_POTTERY_SHERD).orElseThrow();
                Market.sell(level, pos, Items.ANGLER_POTTERY_SHERD, 5);
                double archerAfter = Market.fullMarketFactor(level, Items.ARCHER_POTTERY_SHERD).orElseThrow();
                helper.assertTrue(archerAfter < archerBefore, "selling one sherd lowers the price of another");
                helper.assertTrue(archerAfter == Market.fullMarketFactor(level, Items.ANGLER_POTTERY_SHERD).orElseThrow(),
                        "all sherds have the same factor");
                helper.assertTrue(Market.fullMarketFactor(level, Items.CARROT).orElseThrow() == carrotBefore, "other goods stay untouched");
                helper.assertFalse(data.hasPressure(angler), "the pressure belongs to the good, not to the item");

                // Gleicher Faktor, eigener Basiswert: Die Stückpreise stehen im Verhältnis der Basiswerte.
                data.clearPressure(structureDiscs);
                Market.sell(level, pos, Items.MUSIC_DISC_OTHERSIDE, 1);
                Market.Quote disc5 = Market.quote(level, pos, Items.MUSIC_DISC_5, 1).orElseThrow();
                Market.Quote otherside = Market.quote(level, pos, Items.MUSIC_DISC_OTHERSIDE, 1).orElseThrow();
                double baseRatio = TradeList.lookup(Items.MUSIC_DISC_5).orElseThrow().base()
                        / TradeList.lookup(Items.MUSIC_DISC_OTHERSIDE).orElseThrow().base();
                helper.assertTrue(disc5.factor() == otherside.factor()
                                && Math.abs(disc5.unitPrice() / otherside.unitPrice() - baseRatio) < 1e-6 * baseRatio,
                        "structure discs share the factor but keep their base value: " + disc5.unitPrice() + " vs " + otherside.unitPrice());

                // Ein Marktereignis entlastet die Ware einmal. Der Faktor ist im Logarithmus linear zum Druck: Halbiert die
                // Entlastung den Abstand zur Ruhe einmal, liegt log(Faktor) genau zwischen vorher und Ruhe; 23-mal entlastet
                // läge er fast auf der Ruhe. Druck 2 bleibt fern von den Faktorgrenzen.
                TagKey<Item> loot = TagKey.create(Registries.ITEM, id("market_category/loot"));
                helper.assertTrue(Items.ANGLER_POTTERY_SHERD.getDefaultInstance().is(loot), "sherds are loot");
                data.clearPressure(sherds);
                double atRest = Market.fullMarketFactor(level, Items.ANGLER_POTTERY_SHERD).orElseThrow();
                data.setPressure(sherds, 2);
                double pressed = Market.fullMarketFactor(level, Items.ANGLER_POTTERY_SHERD).orElseThrow();
                Market.relievePressure(level, loot, 0.5, 0.0);
                double relieved = Market.fullMarketFactor(level, Items.ANGLER_POTTERY_SHERD).orElseThrow();
                helper.assertTrue(Math.abs(Math.log(relieved) - (Math.log(pressed) + Math.log(atRest)) / 2.0) < 1e-9,
                        "a shared good is relieved once, not once per item: " + pressed + " -> " + relieved + " (rest " + atRest + ")");

                // Alter Druck je Item wird zum Druck der Ware.
                data.clearPressure(sherds);
                data.setPressure(sherds, 2);
                double direct = Market.fullMarketFactor(level, Items.ARCHER_POTTERY_SHERD).orElseThrow();
                data.clearPressure(sherds);
                data.setPressure(angler, 2);
                double adopted = Market.fullMarketFactor(level, Items.ARCHER_POTTERY_SHERD).orElseThrow();
                helper.assertTrue(Math.abs(adopted - direct) < 1e-9, "the good takes over the old pressure of its items");
                helper.assertTrue(data.hasPressure(sherds) && !data.hasPressure(angler), "the old item entry is gone");

                // Lagervorschau: Jede Zeile zeigt ihren Preis für sich; „alles verkaufen" bepreist die zweite
                // Scherbenart nach der ersten, wie beim Verkauf.
                data.clearPressure(sherds);
                helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
                helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
                TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
                trade.setItem(0, new ItemStack(Items.ANGLER_POTTERY_SHERD, 6));
                trade.setItem(1, new ItemStack(Items.ARCHER_POTTERY_SHERD, 4));
                BlockPos market = helper.absolutePos(new BlockPos(1, 1, 0));
                List<Sales.StockLine> lines = Sales.stock(level, market);
                long rows = lines.stream().mapToLong(Sales.StockLine::total).sum();
                long preview = lines.stream().mapToLong(Sales.StockLine::batchTotal).sum();
                long separate = Market.quote(level, market, Items.ANGLER_POTTERY_SHERD, 6).orElseThrow().total()
                        + Market.quote(level, market, Items.ARCHER_POTTERY_SHERD, 4).orElseThrow().total();
                helper.assertTrue(rows == separate, "each row is priced on its own: " + rows + " vs " + separate);
                helper.assertTrue(preview < separate, "selling all counts the second sherd after the first: " + preview + " vs " + separate);
                Sales.Result result = Sales.sellFromChests(level, market, null, null);
                helper.assertTrue(result.revenue() == preview, "preview " + preview + " equals revenue " + result.revenue());
            } finally {
                data.clearPressure(sherds);
                data.clearPressure(angler);
                data.clearPressure(id("armor_trim"));
                data.clearPressure(structureDiscs);
                cleanup(helper, tradeRel, depositRel);
            }
        });
        helper.succeed();
    }

    /**
     * Marktwaage: je angefangene 10 % Marktwert eine Signalstufe, höchstens 15. Der Filter ist eine Kopie,
     * außerhalb des Territoriums und bei nicht handelbaren Items gibt es kein Signal, und ein Comparator liest
     * Änderungen des Marktdrucks innerhalb einer Sekunde ab — auch ohne Blockupdate.
     */
    private static void marketScale(GameTestHelper helper) {
        double[][] mapping = {{0.0, 0}, {0.05, 0}, {0.094, 0}, {0.10, 1}, {0.19, 1}, {0.20, 2}, {1.0, 10}, {1.49, 14}, {1.5, 15}, {2.5, 15}};
        for (double[] m : mapping) {
            helper.assertTrue(Market.comparatorSignal(m[0]) == (int) m[1], m[0] + " should give signal " + (int) m[1] + ", not " + Market.comparatorSignal(m[0]));
        }

        ServerLevel level = helper.getLevel();
        MarketData data = MarketData.get(level.getServer());
        Identifier wheat = Market.marketKey(Items.WHEAT);

        // Craftbar erst mit „Marktinformation“ — geprüft über die Rezeptsuche, die auch Werkbank und Crafter nutzen.
        WorldProgress info = WorldProgress.get(level.getServer());
        int oldInfo = info.level(ServerUpgrade.MARKET_INFO);
        ItemStack gold = new ItemStack(Items.GOLD_INGOT);
        CraftingInput grid = CraftingInput.of(3, 3, List.of(gold, gold, gold,
                ItemStack.EMPTY, new ItemStack(Items.COMPARATOR), ItemStack.EMPTY,
                ItemStack.EMPTY, new ItemStack(Items.SMOOTH_STONE_SLAB), ItemStack.EMPTY));
        try {
            info.setLevel(ServerUpgrade.MARKET_INFO, 0);
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(),
                    "the market scale is not craftable without market information");
            info.setLevel(ServerUpgrade.MARKET_INFO, 1);
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid, level)
                    .map(r -> r.value().assemble(grid).is(ModBlocks.MARKET_SCALE.get().asItem())).orElse(false),
                    "with market information the recipe yields a market scale");
        } finally {
            info.setLevel(ServerUpgrade.MARKET_INFO, oldInfo);
        }
        BlockPos scaleRel = new BlockPos(1, 1, 0);
        BlockPos comparatorRel = new BlockPos(2, 1, 0);
        helper.setBlock(scaleRel, ModBlocks.MARKET_SCALE.get());
        MarketScaleBlockEntity scale = helper.getBlockEntity(scaleRel, MarketScaleBlockEntity.class);
        BlockPos scalePos = helper.absolutePos(scaleRel);
        // Der Comparator steht vor jeder Signaländerung — per setBlock gesetzt prüft er seinen Eingang nicht selbst.
        helper.setBlock(comparatorRel.below(), Blocks.STONE);
        helper.setBlock(comparatorRel, Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.WEST));

        ItemStack hand = new ItemStack(Items.WHEAT, 5);
        scale.setFilter(hand);
        helper.assertTrue(hand.getCount() == 5 && scale.filter().getCount() == 1 && scale.filter().is(Items.WHEAT), "the filter is a copy");
        helper.assertTrue(scale.signal() == 0, "outside the territory the floor price gives no signal");

        WorldProgress progress = WorldProgress.get(level.getServer());
        BlockPos oldBeacon = progress.beaconPos();
        boolean oldStarted = progress.started();
        int oldBorder = progress.level(ServerUpgrade.BORDER);
        Runnable restore = () -> {
            progress.setBeacon(oldBeacon, oldStarted);
            progress.setLevel(ServerUpgrade.BORDER, oldBorder);
            data.clearPressure(wheat);
            cleanup(helper, scaleRel, comparatorRel);
        };
        progress.setBeacon(helper.absolutePos(BlockPos.ZERO), true);
        progress.setLevel(ServerUpgrade.BORDER, 0);
        try {
            data.clearPressure(wheat);
            scale.refresh(level);
            int expected = Market.comparatorSignal(Market.fullMarketFactor(level, Items.WHEAT).orElseThrow());
            helper.assertTrue(expected > 10 && scale.signal() == expected, "untraded wheat rests above its base value: " + scale.signal());
            helper.assertTrue(level.getBlockState(scalePos).getAnalogOutputSignal(level, scalePos, Direction.WEST) == expected,
                    "the block reports the signal to comparators");

            scale.setFilter(new ItemStack(Items.BEDROCK));
            helper.assertTrue(scale.signal() == 0, "an item without market gives no signal");
            scale.setFilter(ItemStack.EMPTY);
            helper.assertTrue(scale.filter().isEmpty() && scale.signal() == 0, "an empty filter gives no signal");

            scale.setFilter(new ItemStack(Items.WHEAT));
            data.setPressure(wheat, -1.0e9);
            scale.refresh(level);
            helper.assertTrue(scale.signal() == 15, "at the price ceiling the signal is 15");
        } catch (RuntimeException e) {
            restore.run();
            throw e;
        }

        helper.runAfterDelay(10, () -> {
            try {
                int out = helper.getBlockEntity(comparatorRel, ComparatorBlockEntity.class).getOutputSignal();
                helper.assertTrue(out == 15, "the comparator reads 15, not " + out);
                // Ohne Blockupdate: nur der Takt der Waage bemerkt den neuen Druck.
                data.setPressure(wheat, 1.0e9);
            } catch (RuntimeException e) {
                restore.run();
                throw e;
            }
            helper.runAfterDelay(30, () -> {
                try {
                    int out = helper.getBlockEntity(comparatorRel, ComparatorBlockEntity.class).getOutputSignal();
                    helper.assertTrue(out == 0, "at the price floor the comparator reads 0, not " + out);
                } finally {
                    restore.run();
                }
                helper.succeed();
            });
        });
    }

    /** Die Druckanzeige bildet das Preisband linear auf 0..1 ab — wie die Höhe im Preisverlauf. */
    private static void bandPosition(GameTestHelper helper) {
        double min = DtpConfig.FACTOR_MIN.get();
        double max = DtpConfig.FACTOR_MAX.get();
        helper.assertTrue(Math.abs(Market.bandPosition(min)) < 1e-9, "lower bound maps to 0");
        helper.assertTrue(Math.abs(Market.bandPosition(max) - 1.0) < 1e-9, "upper bound maps to 1");
        helper.assertTrue(Math.abs(Market.bandPosition((min + max) / 2) - 0.5) < 1e-9, "the middle of the band maps to 0.5 (linear)");
        helper.assertTrue(Market.bandPosition(min / 10) == 0.0 && Market.bandPosition(max * 10) == 1.0, "values outside the band are clamped");
        double neutral = Market.bandPosition(1.0);
        helper.assertTrue(neutral > 0.0 && neutral < 1.0, "base value lies inside the band");
        helper.assertTrue(Market.bandPosition(0.5) < neutral && Market.bandPosition(2.0) > neutral, "order is preserved");
        helper.succeed();
    }

    /**
     * 1.400 Taler bezahlen, in den Kisten liegen zwei Silbertaler und Kleingeld (docs/10-markt.md):
     * automatisches Zerlegen, Wechselgeld, Teildeckung wird abgelehnt.
     */
    private static void depositPayment(GameTestHelper helper) {
        BlockPos nearRel = new BlockPos(1, 1, 0);
        BlockPos farRel = new BlockPos(2, 1, 0);
        helper.setBlock(nearRel, ModBlocks.DEPOSIT_CHEST.get());
        helper.setBlock(farRel, ModBlocks.DEPOSIT_CHEST.get());
        DepositChestBlockEntity near = helper.getBlockEntity(nearRel, DepositChestBlockEntity.class);
        DepositChestBlockEntity far = helper.getBlockEntity(farRel, DepositChestBlockEntity.class);
        near.setItem(0, new ItemStack(Taler.SILVER_TALER.item(), 2));  // 1.458
        far.setItem(0, new ItemStack(Taler.COPPER_TALER.item(), 30));  // 30
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(0, 1, 0));

        DepositChests.Pool pool = DepositChests.find(level, center);
        helper.assertTrue(pool.chests().size() == 2 && pool.balance() == 1488, "pool: " + pool.chests().size() + " chests, " + pool.balance());
        helper.assertTrue(pool.chests().get(0).container() == near, "nearest chest comes first");

        DepositChests.PayResult tooMuch = DepositChests.pay(level, center, 2000);
        helper.assertTrue(tooMuch instanceof DepositChests.Insufficient(long missing) && missing == 512, "insufficient: " + tooMuch);
        helper.assertTrue(DepositChests.find(level, center).balance() == 1488, "a refused payment takes nothing");

        DepositChests.PayResult paid = DepositChests.pay(level, center, 1400);
        helper.assertTrue(paid instanceof DepositChests.Paid, "payment should succeed: " + paid);
        helper.assertTrue(DepositChests.find(level, center).balance() == 88, "balance after paying 1400 of 1488");
        helper.assertTrue(near.balance() == 58 && far.balance() == 30, "change goes back into the nearest chest");

        near.compact();
        helper.assertTrue(near.balance() == 58, "compacting keeps the value");
        cleanup(helper, nearRel, farRel);
        helper.succeed();
    }

    /** Eine steigende Redstone-Flanke ordnet die Taler der Einzahlkiste, ein Dauersignal nur einmal. */
    private static void depositRedstone(GameTestHelper helper) {
        BlockPos depositRel = new BlockPos(1, 1, 0);
        BlockPos powerRel = new BlockPos(2, 1, 0);
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        DepositChestBlockEntity chest = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        for (int i = 0; i < 9; i++) {
            chest.setItem(i, new ItemStack(Taler.COPPER_TALER.item()));
        }

        helper.setBlock(powerRel, Blocks.REDSTONE_BLOCK);
        int used = 0;
        int sortedSlot = -1;
        for (int i = 0; i < DepositChestBlockEntity.SIZE; i++) {
            if (!chest.getItem(i).isEmpty()) {
                used++;
                sortedSlot = i;
            }
        }
        helper.assertTrue(used == 1 && chest.getItem(sortedSlot).is(Taler.COPPER_POUCH.item()) && chest.balance() == 9,
                "nine copper taler become one copper pouch: " + used + " slots, " + chest.balance());
        helper.assertTrue(helper.getBlockState(depositRel).getValue(DepositChestBlock.TRIGGERED), "the chest remembers the signal");

        int loose = sortedSlot == 0 ? 1 : 0;
        BlockPos aboveRel = new BlockPos(1, 2, 0);
        chest.setItem(loose, new ItemStack(Taler.COPPER_TALER.item(), 9));
        helper.setBlock(aboveRel, Blocks.STONE);  // Nachbar-Update bei gleichbleibendem Signal
        helper.assertTrue(chest.getItem(loose).is(Taler.COPPER_TALER.item()) && chest.getItem(loose).getCount() == 9,
                "a steady signal does not sort again");

        helper.setBlock(powerRel, Blocks.AIR);
        helper.assertTrue(!helper.getBlockState(depositRel).getValue(DepositChestBlock.TRIGGERED), "losing the signal resets the trigger");
        cleanup(helper, depositRel, aboveRel);
        helper.succeed();
    }

    /** Comparatoren lesen Trade-Chest und Einzahlkiste wie eine Vanilla-Kiste. */
    private static void chestComparator(GameTestHelper helper) {
        BlockPos tradeRel = new BlockPos(1, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        ServerLevel level = helper.getLevel();
        BlockPos tradePos = helper.absolutePos(tradeRel);
        BlockPos depositPos = helper.absolutePos(depositRel);
        TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);

        helper.assertTrue(level.getBlockState(tradePos).hasAnalogOutputSignal(), "the trade chest feeds a comparator");
        helper.assertTrue(level.getBlockState(tradePos).getAnalogOutputSignal(level, tradePos, Direction.NORTH) == 0, "an empty trade chest gives 0");
        trade.setItem(0, new ItemStack(Items.WHEAT));
        helper.assertTrue(level.getBlockState(tradePos).getAnalogOutputSignal(level, tradePos, Direction.NORTH) == 1, "one item gives 1");
        for (int i = 0; i < TradeChestBlockEntity.SIZE; i++) {
            trade.setItem(i, new ItemStack(Items.WHEAT, 64));
        }
        helper.assertTrue(level.getBlockState(tradePos).getAnalogOutputSignal(level, tradePos, Direction.NORTH) == 15, "a full trade chest gives 15");

        helper.assertTrue(level.getBlockState(depositPos).getAnalogOutputSignal(level, depositPos, Direction.NORTH) == 0, "an empty deposit chest gives 0");
        for (int i = 0; i < DepositChestBlockEntity.SIZE; i++) {
            deposit.setItem(i, new ItemStack(Taler.COPPER_TALER.item(), 64));
        }
        helper.assertTrue(level.getBlockState(depositPos).getAnalogOutputSignal(level, depositPos, Direction.NORTH) == 15, "a full deposit chest gives 15");
        DepositChests.PayResult paid = DepositChests.pay(level, depositPos, 64L * DepositChestBlockEntity.SIZE);
        helper.assertTrue(paid instanceof DepositChests.Paid, "payment should succeed: " + paid);
        helper.assertTrue(level.getBlockState(depositPos).getAnalogOutputSignal(level, depositPos, Direction.NORTH) == 0, "paying everything drops the signal to 0");
        cleanup(helper, tradeRel, depositRel);
        helper.succeed();
    }

    /**
     * Contract-Stufen werden am Beacon gekauft und setzen serverweit abgeschlossene Contracts voraus.
     * Ohne Stufe bleibt der Aushang leer; der Kauf würfelt ihn sofort neu.
     */
    private static void contractUnlock(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        ContractBoard board = ContractBoard.get(server);
        int oldTier = progress.level(ServerUpgrade.CONTRACT_TIER);
        int oldCompleted = progress.completedContracts();
        int base = DtpConfig.BASE_MAX_TIER.get();
        List<? extends Integer> requirements = DtpConfig.TIER_REQUIREMENT.get();

        BlockPos depositRel = new BlockPos(1, 1, 0);
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        for (int i = 0; i < 27; i++) {
            deposit.setItem(i, new ItemStack(Taler.GOLD_TALER.item(), 64));
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ServerLevel level = helper.getLevel();
        BlockPos beacon = helper.absolutePos(BlockPos.ZERO);

        progress.setLevel(ServerUpgrade.CONTRACT_TIER, 0);
        progress.setCompletedContracts(0);
        try {
            if (base == 0) {
                board.reroll(server);
                helper.assertTrue(board.offers().isEmpty(), "no offers before the first contract tier");
            }
            withTerritory(helper, () -> {
                int target = base + 1;
                int needed = requirements.isEmpty() ? 0 : requirements.get(Math.min(target - 1, requirements.size() - 1));
                if (needed > 0) {
                    BeaconLogic.BuyResult refused = BeaconLogic.buy(level, beacon, player, ServerUpgrade.CONTRACT_TIER);
                    helper.assertTrue(refused instanceof BeaconLogic.ContractsMissing(int missing) && missing == needed,
                            "tier " + target + " needs " + needed + " contracts: " + refused);
                    progress.setCompletedContracts(needed);
                }
                BeaconLogic.BuyResult bought = BeaconLogic.buy(level, beacon, player, ServerUpgrade.CONTRACT_TIER);
                helper.assertTrue(bought instanceof BeaconLogic.Bought, "tier " + target + " can be bought: " + bought);
                helper.assertTrue(ContractBoard.serverMaxTier(server) == target, "server tier is now " + target);
                helper.assertTrue(board.offers().size() == ContractBoard.boardSize(server), "buying a tier rerolls the board");
                helper.assertTrue(board.offers().stream().allMatch(c -> c.tier() <= target), "no offers above the unlocked tier");
            });
        } finally {
            progress.setLevel(ServerUpgrade.CONTRACT_TIER, oldTier);
            progress.setCompletedContracts(oldCompleted);
        }
        cleanup(helper, depositRel);
        helper.succeed();
    }

    /**
     * Spieler-Upgrades an der Werkstatt: nur im Territorium, Voraussetzungen werden geprüft, bezahlt wird aus
     * der Einzahlkiste in Reichweite, und die Stufe landet am Spieler.
     */
    private static void workshopBuy(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos depositRel = new BlockPos(1, 1, 0);
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        deposit.setItem(0, new ItemStack(Taler.GOLD_TALER.item(), 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos table = helper.absolutePos(new BlockPos(0, 1, 0));
        PlayerUpgradeDefinitions.Definition vein = PlayerUpgradeDefinitions.get(PlayerUpgrade.VEIN_MINING);
        helper.assertTrue(vein != null && PlayerUpgradeDefinitions.get(PlayerUpgrade.FORTUNE_BONUS) != null
                        && PlayerUpgradeDefinitions.get(PlayerUpgrade.AREA_MINING) != null,
                "pilot upgrades are loaded from the datapack");
        helper.assertTrue(!PlayerUpgradeDefinitions.get(PlayerUpgrade.AREA_MINING).requires().isEmpty(),
                "area mining needs a requirement for the locked purchase below");

        helper.assertTrue(PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.VEIN_MINING) instanceof PlayerUpgradeLogic.Outside,
                "nothing can be bought outside the territory");
        withTerritory(helper, () -> {
            PlayerUpgradeLogic.BuyResult locked = PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.AREA_MINING);
            helper.assertTrue(locked instanceof PlayerUpgradeLogic.RequirementsMissing(var missing) && missing.size() == PlayerUpgradeDefinitions.get(PlayerUpgrade.AREA_MINING).requires().size(),
                    "area mining lists all its missing requirements: " + locked);
            helper.assertTrue(PlayerUpgrades.level(player, PlayerUpgrade.AREA_MINING) == 0, "a locked purchase changes nothing");

            long before = DepositChests.find(level, table).balance();
            PlayerUpgradeLogic.BuyResult bought = PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.VEIN_MINING);
            helper.assertTrue(bought instanceof PlayerUpgradeLogic.Bought(int lvl, long cost) && lvl == 1 && cost == vein.cost(1),
                    "vein mining level 1: " + bought);
            helper.assertTrue(PlayerUpgrades.level(player, PlayerUpgrade.VEIN_MINING) == 1, "the level is stored at the player");
            helper.assertTrue(DepositChests.find(level, table).balance() == before - vein.cost(1), "paid from the deposit chest");
            helper.assertTrue(PlayerUpgrades.value(player, PlayerUpgrade.VEIN_MINING) == vein.value(1), "effect value of level 1");

            PlayerUpgrades.of(player).setLevel(PlayerUpgrade.VEIN_MINING, vein.maxLevel());
            helper.assertTrue(PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.VEIN_MINING) instanceof PlayerUpgradeLogic.Maxed,
                    "no level beyond the maximum");

            deposit.clearContent();
            PlayerUpgradeLogic.BuyResult poor = PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.FORTUNE_BONUS);
            helper.assertTrue(poor instanceof PlayerUpgradeLogic.Insufficient, "an empty chest cannot pay: " + poor);
            helper.assertTrue(PlayerUpgrades.level(player, PlayerUpgrade.FORTUNE_BONUS) == 0, "a refused purchase changes nothing");
        });
        cleanup(helper, depositRel);
        helper.succeed();
    }

    /**
     * Persönliche Module: Kauf erst mit Modulslot, wirksam nur ausgerüstet, höchstens so viele wie Slots,
     * Ausrüsten nur an einer Werkstatt im Territorium oder im Radius eines Neben-Beacons. Passive Upgrades wirken weiter ohne Slot.
     */
    private static void playerModules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos depositRel = new BlockPos(1, 1, 0);
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        deposit.setItem(0, new ItemStack(Taler.GOLD_TALER.item(), 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        BlockPos table = helper.absolutePos(new BlockPos(0, 1, 0));
        PlayerUpgradeDefinitions.Definition rush = PlayerUpgradeDefinitions.get(PlayerUpgrade.RUSH);
        helper.assertTrue(rush != null && PlayerUpgradeDefinitions.get(PlayerUpgrade.MODULE_SLOTS) != null,
                "module slots and rush are loaded from the datapack");
        helper.assertTrue(PlayerUpgrade.RUSH.isModule() && !PlayerUpgrade.VEIN_MINING.isModule(), "kinds are fixed in code");

        withTerritory(helper, () -> {
            PlayerUpgradeLogic.BuyResult noSlot = PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.RUSH);
            helper.assertFalse(noSlot instanceof PlayerUpgradeLogic.Bought, "no module without a module slot: " + noSlot);
            helper.assertTrue(mine.level(PlayerUpgrade.RUSH) == 0, "the refused purchase changes nothing");

            mine.setLevel(PlayerUpgrade.MODULE_SLOTS, 1);
            PlayerUpgradeLogic.BuyResult bought = PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.RUSH);
            helper.assertTrue(bought instanceof PlayerUpgradeLogic.Bought, "rush can be bought with a slot: " + bought);
            helper.assertTrue(mine.level(PlayerUpgrade.RUSH) == 1, "rush is owned");
            helper.assertTrue(PlayerUpgrades.level(player, PlayerUpgrade.RUSH) == 0 && PlayerUpgrades.value(player, PlayerUpgrade.RUSH) == 0,
                    "an owned module does nothing until equipped");

            helper.assertTrue(PlayerUpgradeLogic.toggleModule(level, table, player, PlayerUpgrade.VEIN_MINING) instanceof PlayerUpgradeLogic.Locked,
                    "passive upgrades cannot be equipped");
            helper.assertTrue(PlayerUpgradeLogic.toggleModule(level, table, player, PlayerUpgrade.RUSH) instanceof PlayerUpgradeLogic.Equipped,
                    "rush is equipped");
            helper.assertTrue(PlayerUpgrades.value(player, PlayerUpgrade.RUSH) == rush.value(1), "an equipped module works");
            helper.assertTrue(PlayerUpgradeLogic.toggleModule(level, table, player, PlayerUpgrade.RUSH) instanceof PlayerUpgradeLogic.Unequipped,
                    "rush is unequipped");
            helper.assertTrue(PlayerUpgrades.value(player, PlayerUpgrade.RUSH) == 0, "an unequipped module stops working");

            // Slots voll: mit null Slots passt nichts hinein, und gesunkene Slots lassen ein Modul wirkungslos werden.
            mine.toggleModule(PlayerUpgrade.RUSH);
            mine.setLevel(PlayerUpgrade.MODULE_SLOTS, 0);
            helper.assertTrue(PlayerUpgrades.value(player, PlayerUpgrade.RUSH) == 0, "modules beyond the slot count do not work");
            helper.assertTrue(PlayerUpgradeLogic.toggleModule(level, table, player, PlayerUpgrade.RUSH) instanceof PlayerUpgradeLogic.NoFreeSlot,
                    "no module fits without a free slot");
            mine.setLevel(PlayerUpgrade.MODULE_SLOTS, 10);
            helper.assertTrue(mine.moduleSlots() == PlayerUpgrades.MAX_MODULE_SLOTS, "never more than three module slots");
            helper.assertTrue(PlayerUpgrades.value(player, PlayerUpgrade.RUSH) == rush.value(1), "the kept entry works again with a slot");

            mine.setLevel(PlayerUpgrade.VEIN_MINING, 1);
            helper.assertTrue(PlayerUpgrades.level(player, PlayerUpgrade.VEIN_MINING) == 1, "passive upgrades work without a slot");
        });
        helper.assertTrue(PlayerUpgradeLogic.toggleModule(level, table, player, PlayerUpgrade.RUSH) instanceof PlayerUpgradeLogic.ToggleOutside,
                "modules are swapped only at a workshop in the territory");
        helper.assertTrue(PlayerUpgrades.value(player, PlayerUpgrade.RUSH) == rush.value(1), "equipped modules work outside the territory");

        // Im Radius eines Neben-Beacons lassen sich Module auch außerhalb des Territoriums umstecken — kaufen nicht.
        WorldProgress progress = WorldProgress.get(level.getServer());
        GlobalPos relay = GlobalPos.of(level.dimension(), table.offset(16, 0, 0));
        progress.addRelay(relay);
        try {
            helper.assertTrue(PlayerUpgradeLogic.toggleModule(level, table, player, PlayerUpgrade.RUSH) instanceof PlayerUpgradeLogic.Unequipped,
                    "modules can be swapped within a relay's radius");
            helper.assertTrue(PlayerUpgradeLogic.toggleModule(level, table, player, PlayerUpgrade.RUSH) instanceof PlayerUpgradeLogic.Equipped,
                    "and equipped again there");
            helper.assertTrue(PlayerUpgradeLogic.buy(level, table, player, PlayerUpgrade.MODULE_SLOTS) instanceof PlayerUpgradeLogic.Outside,
                    "buying still needs the territory");
        } finally {
            progress.removeRelay(relay);
        }

        CompoundTag v1 = new CompoundTag();
        v1.putInt(Versioned.KEY, 1);
        CompoundTag levels = new CompoundTag();
        levels.putInt("rush", 1);
        v1.put("levels", levels);
        PlayerUpgrades old = PlayerUpgrades.CODEC.parse(NbtOps.INSTANCE, v1).getOrThrow();
        helper.assertTrue(old.level(PlayerUpgrade.RUSH) == 1 && old.equippedModules().isEmpty(), "v1 data loads with nothing equipped");
        Tag saved = PlayerUpgrades.CODEC.encodeStart(NbtOps.INSTANCE, mine).getOrThrow();
        PlayerUpgrades reloaded = PlayerUpgrades.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(((CompoundTag) saved).getIntOr(Versioned.KEY, 0) == 3 && reloaded.isEquipped(PlayerUpgrade.RUSH),
                "equipped modules survive saving");

        cleanup(helper, depositRel);
        helper.succeed();
    }

    /** Rausch: Speed nach einem Monster-Kill, aber nur ausgerüstet und nicht für friedliche Tiere. */
    private static void rushKill(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        // Einfache Attrappe: ein Mock-ServerPlayer hat keine Verbindung und kann keine Effekt-Pakete senden.
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        mine.setLevel(PlayerUpgrade.MODULE_SLOTS, 1);
        mine.setLevel(PlayerUpgrade.RUSH, 1);

        LivingEntity first = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        first.hurtServer(level, level.damageSources().playerAttack(player), 1000f);
        helper.assertFalse(first.isAlive(), "the zombie is dead");
        helper.assertFalse(player.hasEffect(MobEffects.SPEED), "an owned but unequipped module gives no speed");

        mine.toggleModule(PlayerUpgrade.RUSH);
        LivingEntity cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 2));
        cow.hurtServer(level, level.damageSources().playerAttack(player), 1000f);
        helper.assertFalse(player.hasEffect(MobEffects.SPEED), "animals do not trigger rush");

        LivingEntity second = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(2, 2, 1));
        second.hurtServer(level, level.damageSources().playerAttack(player), 1000f);
        MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
        helper.assertTrue(hasRushEffect(player, MobEffects.SPEED, "speed", 1), "rush 1 gives the configured speed: " + speed);
        helper.assertTrue(hasRushEffect(player, MobEffects.STRENGTH, "strength", 1),
                "rush 1 gives the configured strength: " + player.getEffect(MobEffects.STRENGTH));

        mine.setLevel(PlayerUpgrade.RUSH, 2);
        player.removeAllEffects();
        LivingEntity third = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(3, 2, 1));
        third.hurtServer(level, level.damageSources().playerAttack(player), 1000f);
        helper.assertTrue(hasRushEffect(player, MobEffects.SPEED, "speed", 2) && hasRushEffect(player, MobEffects.STRENGTH, "strength", 2),
                "rush 2 gives the configured speed and strength: " + player.getActiveEffects());

        // Aufsammelradius: Ein Item knapp außerhalb der Vanilla-Reichweite (1 Block um die Hitbox) holt nur der Rausch.
        Player collector = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos stand = helper.absolutePos(new BlockPos(5, 2, 5));
        collector.snapTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0f, 0f);
        ItemEntity apple = new ItemEntity(level, collector.getX() + 1.8, collector.getY(), collector.getZ(), new ItemStack(Items.APPLE));
        apple.setNoPickUpDelay();
        apple.setNoGravity(true);
        level.addFreshEntity(apple);
        try {
            PickupBoost.pickup(collector);
            helper.assertTrue(apple.isAlive() && collector.getInventory().countItem(Items.APPLE) == 0, "without rush the item stays out of reach");
            PlayerUpgrades.of(collector).setLevel(PlayerUpgrade.MODULE_SLOTS, 1);
            PlayerUpgrades.of(collector).setLevel(PlayerUpgrade.RUSH, 1);
            PlayerUpgrades.of(collector).toggleModule(PlayerUpgrade.RUSH);
            PlayerUpgradeEffects.rush(collector);
            helper.assertTrue(PickupBoost.range(collector) == param(PlayerUpgrade.RUSH, "pickup_range", 1), "rush widens the pickup range");
            PickupBoost.pickup(collector);
            helper.assertTrue(collector.getInventory().countItem(Items.APPLE) == 1, "with rush the item is picked up");
        } finally {
            apple.discard();
        }
        helper.succeed();
    }

    /**
     * Rausch-Effekt wie in der Datei: Stufe {@code stageParam} (0 = kein Effekt) für {@code values} Sekunden der
     * Upgrade-Stufe {@code level}.
     */
    private static boolean hasRushEffect(Player player, Holder<MobEffect> type, String stageParam, int level) {
        int stage = (int) param(PlayerUpgrade.RUSH, stageParam, level);
        MobEffectInstance effect = player.getEffect(type);
        if (stage <= 0) {
            return effect == null;
        }
        return effect != null && effect.getAmplifier() == stage - 1
                && effect.getDuration() == (int) value(PlayerUpgrade.RUSH, level) * 20;
    }

    /** Rüstet ein Modul samt Slots aus — ohne Werkstatt, für Tests der Wirkung. */
    private static void equip(Player player, PlayerUpgrade module) {
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        mine.setLevel(PlayerUpgrade.MODULE_SLOTS, PlayerUpgrades.MAX_MODULE_SLOTS);
        mine.setLevel(module, 1);
        if (!mine.isEquipped(module)) {
            mine.toggleModule(module);
        }
    }

    /** Barriere: Absorption nach Kampf- und Feuerschaden, nicht nach Fallschaden, nur ausgerüstet, mit Abklingzeit. */
    private static void barrierShield(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var sources = level.damageSources();
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(1, 2, 1));
        Player attacker = helper.makeMockPlayer(GameType.SURVIVAL);
        // Welche Schäden auslösen. Mob-Treffer auf Spieler sind auf dem friedlichen Testserver null — echt ausgeteilt
        // werden unten Feuer, Sturz und ein Spielerangriff.
        helper.assertTrue(PlayerUpgradeEffects.triggersBarrier(sources.mobAttack(zombie)), "mob attacks trigger");
        helper.assertTrue(PlayerUpgradeEffects.triggersBarrier(sources.playerAttack(attacker)), "player attacks trigger");
        helper.assertTrue(PlayerUpgradeEffects.triggersBarrier(sources.inFire()) && PlayerUpgradeEffects.triggersBarrier(sources.lava()),
                "fire and lava trigger");
        helper.assertFalse(PlayerUpgradeEffects.triggersBarrier(sources.fall()) || PlayerUpgradeEffects.triggersBarrier(sources.drown())
                || PlayerUpgradeEffects.triggersBarrier(sources.starve()), "fall, drowning and hunger do not trigger");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.hurtServer(level, sources.inFire(), 2f);
        helper.assertFalse(player.hasEffect(MobEffects.ABSORPTION), "no shield without the module");

        equip(player, PlayerUpgrade.BARRIER);
        player.damageCooldownTime = 0;
        player.hurtServer(level, sources.fall(), 2f);
        helper.assertFalse(player.hasEffect(MobEffects.ABSORPTION), "fall damage does not trigger the barrier");

        player.damageCooldownTime = 0;
        player.hurtServer(level, sources.inFire(), 2f);
        MobEffectInstance shield = player.getEffect(MobEffects.ABSORPTION);
        double duration = PlayerUpgradeDefinitions.get(PlayerUpgrade.BARRIER).param("duration", -1);
        helper.assertTrue(shield != null && shield.getAmplifier() == 0 && shield.getDuration() == (int) duration * 20,
                "fire damage gives absorption I for the configured time: " + shield);
        helper.assertFalse(ModuleCooldowns.ready(player, PlayerUpgrade.BARRIER), "the cooldown runs after triggering");

        player.removeEffect(MobEffects.ABSORPTION);
        player.damageCooldownTime = 0;
        player.hurtServer(level, sources.inFire(), 2f);
        helper.assertFalse(player.hasEffect(MobEffects.ABSORPTION), "no second shield during the cooldown");

        Player fighter = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(fighter, PlayerUpgrade.BARRIER);
        fighter.hurtServer(level, sources.playerAttack(attacker), 2f);
        helper.assertTrue(fighter.hasEffect(MobEffects.ABSORPTION), "a hit by another player triggers the barrier");

        Tag saved = ModuleCooldowns.CODEC.encodeStart(NbtOps.INSTANCE, ModuleCooldowns.of(player)).getOrThrow();
        ModuleCooldowns.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(((CompoundTag) saved).getCompoundOrEmpty("ready_at").contains("barrier"), "cooldowns are saved by module id");

        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.BARRIER);
        Player veteran = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(veteran, PlayerUpgrade.BARRIER);
        PlayerUpgrades.of(veteran).setLevel(PlayerUpgrade.BARRIER, 2);
        veteran.hurtServer(level, sources.inFire(), 2f);
        MobEffectInstance strong = veteran.getEffect(MobEffects.ABSORPTION);
        helper.assertTrue(strong != null && strong.getAmplifier() == 1, "level 2 gives absorption II: " + strong);
        long cooldown = ModuleCooldowns.remaining(veteran, PlayerUpgrade.BARRIER);
        helper.assertTrue(cooldown == Math.round(def.param("cooldown", 2, -1) * 20) && cooldown < Math.round(def.param("cooldown", 1, -1) * 20),
                "level 2 has the shorter cooldown: " + cooldown);
        helper.succeed();
    }

    /**
     * Abbaufläche 3×3: acht Nachbarn quer zur getroffenen Seite, jeder kostet Haltbarkeit; mit fällt nur die Blockgruppe
     * des Werkzeugs (natürliches Gestein bzw. Boden), in beide Richtungen gleich, ohne Erze und Baublöcke. Schleichend am Erz gewinnt Vein Mining, ohne Modul fällt nur ein Block.
     */
    private static void areaMining(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        equip(player, PlayerUpgrade.AREA_MINING);
        BlockPos centerRel = new BlockPos(2, 3, 2);
        BlockPos center = helper.absolutePos(centerRel);
        List<BlockPos> wall = new ArrayList<>();
        for (int x = 1; x <= 3; x++) {
            for (int y = 2; y <= 4; y++) {
                wall.add(new BlockPos(x, y, 2));
            }
        }
        // Blick nach Süden (+z) genau auf die Mitte der Wand: getroffen wird die Nordseite, die Fläche liegt in x/y.
        player.snapTo(center.getX() + 0.5, center.getY() + 0.5 - player.getEyeHeight(), center.getZ() - 2.0, 0f, 0f);
        try {
            wall.forEach(p -> helper.setBlock(p, Blocks.STONE));
            helper.setBlock(new BlockPos(1, 2, 2), Blocks.OBSIDIAN);
            helper.setBlock(new BlockPos(3, 4, 2), Blocks.DIRT);
            ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
            List<BlockPos> targets = ChainBreaker.areaTargets(level, center, Direction.NORTH, pickaxe);
            helper.assertTrue(targets.size() == 6, "obsidian and dirt are not in the stone group, got " + targets.size());
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.UP, pickaxe).size() == 2,
                    "looking from above, the plane is horizontal and holds only the wall's left and right neighbour");

            // Obsidian ist eine eigene Gruppe: nimmt nur Obsidian mit, kein Gestein.
            wall.forEach(p -> helper.setBlock(p, Blocks.OBSIDIAN));
            helper.setBlock(new BlockPos(1, 2, 2), Blocks.STONE);
            helper.setBlock(new BlockPos(3, 4, 2), Blocks.CRYING_OBSIDIAN);
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, pickaxe).size() == 6,
                    "obsidian takes only obsidian along, no stone or crying obsidian");
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, new ItemStack(Items.IRON_PICKAXE)).isEmpty(),
                    "an iron pickaxe makes no area in obsidian");

            // Gruppen statt Härte: symmetrisch, ohne Erze und Baublöcke.
            ItemStack shovel = new ItemStack(Items.DIAMOND_SHOVEL);
            BlockPos edge = new BlockPos(1, 3, 2);
            BlockPos corner = new BlockPos(3, 4, 2);
            wall.forEach(p -> helper.setBlock(p, Blocks.GRASS_BLOCK));
            helper.setBlock(edge, Blocks.DIRT);
            helper.setBlock(corner, Blocks.GRAVEL);
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, shovel).size() == 8, "grass takes dirt and gravel along");
            wall.forEach(p -> helper.setBlock(p, Blocks.DIRT));
            helper.setBlock(edge, Blocks.GRASS_BLOCK);
            helper.setBlock(corner, Blocks.SAND);
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, shovel).size() == 8, "dirt takes grass and sand along");
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, pickaxe).isEmpty(), "the pickaxe makes no area in dirt");

            wall.forEach(p -> helper.setBlock(p, Blocks.STONE));
            helper.setBlock(edge, Blocks.DEEPSLATE);
            helper.setBlock(corner, Blocks.NETHERRACK);
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, pickaxe).size() == 8, "stone takes deepslate and netherrack along");
            helper.setBlock(centerRel, Blocks.DEEPSLATE);
            helper.setBlock(edge, Blocks.STONE);
            helper.setBlock(corner, Blocks.IRON_ORE);
            helper.setBlock(new BlockPos(1, 2, 2), Blocks.STONE_BRICKS);
            helper.setBlock(new BlockPos(3, 2, 2), Blocks.COBBLESTONE_STAIRS);
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, pickaxe).size() == 5,
                    "deepslate takes stone along, but no ore, stone bricks or stairs");
            helper.setBlock(centerRel, Blocks.IRON_ORE);
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, pickaxe).isEmpty(), "an ore in the middle makes no area");
            helper.setBlock(centerRel, Blocks.STONE_BRICKS);
            helper.assertTrue(ChainBreaker.areaTargets(level, center, Direction.NORTH, pickaxe).isEmpty(), "stone bricks in the middle make no area");

            wall.forEach(p -> helper.setBlock(p, Blocks.STONE));
            player.setItemInHand(InteractionHand.MAIN_HAND, pickaxe);
            player.gameMode.destroyBlock(center);
            helper.assertTrue(wall.stream().allMatch(p -> helper.getBlockState(p).isAir()), "the whole 3x3 wall is mined");
            helper.assertTrue(pickaxe.getDamageValue() == 9, "every block costs durability, got " + pickaxe.getDamageValue());

            // Schleichend am Erz: Vein Mining gewinnt, der Stein drumherum bleibt.
            wall.forEach(p -> helper.setBlock(p, Blocks.STONE));
            helper.setBlock(centerRel, Blocks.IRON_ORE);
            helper.setBlock(centerRel.above(), Blocks.IRON_ORE);
            PlayerUpgrades.of(player).setLevel(PlayerUpgrade.VEIN_MINING, 1);
            player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_PICKAXE));
            player.gameMode.destroyBlock(center);
            long left = wall.stream().filter(p -> helper.getBlockState(p).is(Blocks.STONE)).count();
            helper.assertTrue(helper.getBlockState(centerRel.above()).isAir() && left == 7, "vein mining wins while sneaking, stone left: " + left);
            player.setShiftKeyDown(false);

            // Ohne ausgerüstetes Modul nur der eine Block.
            wall.forEach(p -> helper.setBlock(p, Blocks.STONE));
            PlayerUpgrades.of(player).toggleModule(PlayerUpgrade.AREA_MINING);
            player.gameMode.destroyBlock(center);
            helper.assertTrue(wall.stream().filter(p -> helper.getBlockState(p).isAir()).count() == 1, "without the module only one block");
        } finally {
            wall.forEach(p -> helper.setBlock(p, Blocks.AIR));
        }
        helper.succeed();
    }

    /** Erschöpfung des Hungers — ohne Getter, also über das Speicherformat. */
    private static float exhaustion(Player player) {
        TagValueOutput out = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        player.getFoodData().addAdditionalSaveData(out);
        return out.buildResult().getFloatOr("foodExhaustionLevel", 0f);
    }

    /** Frischer Spieler mit einem Skill auf {@code level} — jeder hat seine eigene, gemeinsame Skill-Abklingzeit. */
    private static Player withSkill(GameTestHelper helper, PlayerUpgrade skill, int level) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        equip(player, skill);
        PlayerUpgrades.of(player).setLevel(skill, level);
        return player;
    }

    /**
     * Benannter Parameter aus der Upgrade-Datei. Fehlt er, scheitert der Test laut, statt still auf den Fallback des
     * Produktivcodes zu fallen — so bleibt ein umbenannter Parameter nicht unbemerkt. Tests lesen Balance-Werte hier,
     * statt sie als Literal zu wiederholen: Eine Anpassung der Datei lässt sie grün, eine falsch verdrahtete Wirkung nicht.
     */
    private static double param(PlayerUpgrade upgrade, String name, int level) {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(upgrade);
        double value = def == null ? Double.NaN : def.param(name, level, Double.NaN);
        if (Double.isNaN(value)) {
            throw new IllegalStateException(upgrade + " has no parameter '" + name + "'");
        }
        return value;
    }

    /** Wirkungswert {@code values} der Upgrade-Datei auf {@code level}. */
    private static double value(PlayerUpgrade upgrade, int level) {
        return PlayerUpgradeDefinitions.get(upgrade).value(level);
    }

    /**
     * Der Spieler hat {@code type} in der Stufe, die die Datei unter {@code stageParam} nennt (wie im Spiel angezeigt,
     * 2 = Stufe II), für {@code duration} Sekunden aus derselben Datei.
     */
    private static boolean hasConfiguredEffect(Player player, Holder<MobEffect> type, PlayerUpgrade skill, String stageParam, int level) {
        MobEffectInstance effect = player.getEffect(type);
        return effect != null && effect.getAmplifier() == (int) param(skill, stageParam, level) - 1
                && effect.getDuration() == (int) Math.round(param(skill, "duration", level) * 20);
    }

    /**
     * Aktive Skills: höchstens einer ausgerüstet, eine gemeinsame Abklingzeit, Wirkung je Stufe, keine
     * Hungerkosten. Der Doppelsprung schützt nicht vor Fallschaden.
     */
    private static void skills(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        helper.assertTrue(Skills.activateEquipped(player) instanceof Skills.NotEquipped, "the skill key does nothing without a skill");
        helper.assertTrue(Skills.activate(player, PlayerUpgrade.RUSH) instanceof Skills.Invalid, "rush is no skill");

        // Ein Skill ersetzt den anderen; automatische Module bleiben daneben stecken.
        equip(player, PlayerUpgrade.BARRIER);
        equip(player, PlayerUpgrade.GUARD);
        equip(player, PlayerUpgrade.HEAL_PULSE);
        helper.assertTrue(mine.equippedSkill() == PlayerUpgrade.HEAL_PULSE && !mine.isEquipped(PlayerUpgrade.GUARD)
                && mine.isEquipped(PlayerUpgrade.BARRIER) && mine.equippedModules().size() == 2, "only one skill: " + mine.equippedModules());

        helper.assertTrue(Skills.activateEquipped(player) instanceof Skills.Used, "the skill key uses the equipped skill");
        mine.toggleModule(PlayerUpgrade.GUARD);
        helper.assertTrue(mine.equippedSkill() == PlayerUpgrade.GUARD, "swapped to guard");
        helper.assertTrue(Skills.activateEquipped(player) instanceof Skills.Cooldown, "swapping does not reset the shared cooldown");

        Player heal1 = withSkill(helper, PlayerUpgrade.HEAL_PULSE, 1);
        Skills.activateEquipped(heal1);
        helper.assertTrue(hasConfiguredEffect(heal1, MobEffects.REGENERATION, PlayerUpgrade.HEAL_PULSE, "regeneration", 1),
                "healing pulse 1: configured regeneration: " + heal1.getEffect(MobEffects.REGENERATION));
        Player heal2 = withSkill(helper, PlayerUpgrade.HEAL_PULSE, 2);
        Skills.activateEquipped(heal2);
        helper.assertTrue(hasConfiguredEffect(heal2, MobEffects.REGENERATION, PlayerUpgrade.HEAL_PULSE, "regeneration", 2),
                "healing pulse 2: configured regeneration: " + heal2.getEffect(MobEffects.REGENERATION));
        long healCooldown = Math.round(param(PlayerUpgrade.HEAL_PULSE, "cooldown", 2) * 20);
        helper.assertTrue(ModuleCooldowns.remaining(heal2, PlayerUpgrade.HEAL_PULSE) == healCooldown, "configured cooldown");
        Player near = helper.makeMockPlayer(GameType.SURVIVAL);
        Player far = helper.makeMockPlayer(GameType.SURVIVAL);
        near.setPos(heal1.getX() + 3, heal1.getY(), heal1.getZ());
        far.setPos(heal1.getX() + 20, heal1.getY(), heal1.getZ());
        List<Player> healed = Skills.healTargets(heal1, List.of(near, far), 6);
        helper.assertTrue(healed.contains(heal1) && healed.contains(near) && !healed.contains(far), "heals self and players in the radius");

        Player guard1 = withSkill(helper, PlayerUpgrade.GUARD, 1);
        guard1.getFoodData().setFoodLevel(0);
        guard1.getFoodData().setSaturation(0f);
        float before = exhaustion(guard1);
        helper.assertTrue(Skills.activateEquipped(guard1) instanceof Skills.Used, "skills work even when starving");
        helper.assertTrue(exhaustion(guard1) == before && guard1.getFoodData().getFoodLevel() == 0, "skills cost no hunger");
        helper.assertTrue(hasConfiguredEffect(guard1, MobEffects.RESISTANCE, PlayerUpgrade.GUARD, "resistance", 1),
                "guard 1: configured resistance: " + guard1.getEffect(MobEffects.RESISTANCE));
        Player guard2 = withSkill(helper, PlayerUpgrade.GUARD, 2);
        Skills.activateEquipped(guard2);
        helper.assertTrue(hasConfiguredEffect(guard2, MobEffects.RESISTANCE, PlayerUpgrade.GUARD, "resistance", 2),
                "guard 2: configured resistance: " + guard2.getEffect(MobEffects.RESISTANCE));

        Player dasher = withSkill(helper, PlayerUpgrade.DASH, 1);
        dasher.setYRot(0f);
        dasher.setXRot(0f);
        helper.assertTrue(Skills.activateEquipped(dasher) instanceof Skills.Used, "dash is used");
        double dashStrength = param(PlayerUpgrade.DASH, "strength", 1);
        helper.assertTrue(Math.abs(dasher.getDeltaMovement().z - dashStrength) < 1e-6 && Math.abs(dasher.getDeltaMovement().x) < 1e-6,
                "dash pushes in the look direction: " + dasher.getDeltaMovement());

        Player jumper = withSkill(helper, PlayerUpgrade.DOUBLE_JUMP, 1);
        jumper.setOnGround(true);
        helper.assertTrue(Skills.activateEquipped(jumper) instanceof Skills.Invalid, "no double jump on the ground");
        jumper.setOnGround(false);
        jumper.fallDistance = 5;
        helper.assertTrue(Skills.activateEquipped(jumper) instanceof Skills.Used, "double jump in the air");
        helper.assertTrue(Math.abs(jumper.getDeltaMovement().y - param(PlayerUpgrade.DOUBLE_JUMP, "strength", 1)) < 1e-6
                && jumper.fallDistance == 5, "jumps up with the configured strength but keeps the fall distance");
        helper.assertTrue(ModuleCooldowns.ready(jumper, PlayerUpgrade.DOUBLE_JUMP), "double jump has no cooldown");
        helper.assertTrue(Skills.activateEquipped(jumper) instanceof Skills.Invalid, "only once until landing");
        helper.succeed();
    }

    /**
     * Fischernetz: fängt Kabeljau, Lachs, Tropenfisch und Kugelfisch im Wasser im Radius, die Beute landet im Inventar.
     * Verschont Eimerfische, Fische an Land und außer Reichweite und andere Wassertiere. Ohne Fisch kein Einsatz und
     * keine Abklingzeit. {@code isInWater} setzt erst der Tick des Fischs, daher die kurze Verzögerung.
     */
    private static void fishingNet(GameTestHelper helper) {
        double radius = param(PlayerUpgrade.FISHING_NET, "radius", 1);
        int far = (int) Math.ceil(radius) + 2;
        Player player = withSkill(helper, PlayerUpgrade.FISHING_NET, 1);
        BlockPos stand = new BlockPos(1, 2, 1);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(stand));
        player.setPos(at.x, at.y, at.z);
        helper.assertTrue(Skills.activateEquipped(player) instanceof Skills.Invalid, "no fish, no net");
        helper.assertTrue(ModuleCooldowns.ready(player, PlayerUpgrade.FISHING_NET), "an empty cast costs no cooldown");

        List<BlockPos> placed = new ArrayList<>();
        List<LivingEntity> spawned = new ArrayList<>();
        Runnable restore = () -> {
            spawned.forEach(e -> e.discard());
            placed.forEach(p -> helper.setBlock(p, Blocks.AIR));
        };
        try {
            for (int x = 0; x <= far + 1; x++) {
                for (int z = 0; z <= 2; z++) {
                    placed.add(new BlockPos(x, 1, z));
                    helper.setBlock(new BlockPos(x, 1, z), Blocks.GLASS);
                    placed.add(new BlockPos(x, 2, z));
                    helper.setBlock(new BlockPos(x, 2, z), Blocks.WATER);
                }
            }
            BlockPos land = new BlockPos(1, 2, 1 + (int) Math.floor(radius));       // genau am Radius, aber trocken
            BlockPos landFloor = land.below();
            placed.add(landFloor);
            helper.setBlock(landFloor, Blocks.GLASS);
        } catch (RuntimeException e) {
            restore.run();
            throw e;
        }
        List<LivingEntity> caught = new ArrayList<>(List.of(
                helper.spawnWithNoFreeWill(EntityTypes.COD, new BlockPos(2, 2, 1)),
                helper.spawnWithNoFreeWill(EntityTypes.SALMON, new BlockPos(2, 2, 0)),
                helper.spawnWithNoFreeWill(EntityTypes.TROPICAL_FISH, new BlockPos(0, 2, 1)),
                helper.spawnWithNoFreeWill(EntityTypes.PUFFERFISH, new BlockPos(1, 2, 0))));
        Cod bucketCod = helper.spawnWithNoFreeWill(EntityTypes.COD, new BlockPos(2, 2, 2));
        bucketCod.setFromBucket(true);
        List<LivingEntity> spared = new ArrayList<>(List.of(bucketCod,
                helper.spawnWithNoFreeWill(EntityTypes.COD, new BlockPos(far, 2, 1)),
                helper.spawnWithNoFreeWill(EntityTypes.COD, new BlockPos(1, 2, 1 + (int) Math.floor(radius))),
                helper.spawnWithNoFreeWill(EntityTypes.SQUID, new BlockPos(0, 2, 2))));
        spawned.addAll(caught);
        spawned.addAll(spared);

        helper.runAfterDelay(2, () -> {
            try {
                helper.assertTrue(FishingNet.targets(player, radius).size() == caught.size(),
                        "targets: " + FishingNet.targets(player, radius));
                helper.assertTrue(Skills.activateEquipped(player) instanceof Skills.Used, "the net is cast");
                for (LivingEntity fish : caught) {
                    helper.assertFalse(fish.isAlive(), fish.getType() + " in the water within reach is caught");
                }
                for (LivingEntity other : spared) {
                    helper.assertTrue(other.isAlive(), other.getType() + " at " + other.blockPosition() + " is spared");
                }
                for (Item item : List.of(Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH)) {
                    int count = 0;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack stack = player.getInventory().getItem(i);
                        count += stack.is(item) ? stack.getCount() : 0;
                    }
                    helper.assertTrue(count >= 1, "the catch goes into the inventory: " + item);
                }
                helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(radius + 2)).isEmpty(),
                        "nothing is left in the water");
                long cooldown = Math.round(param(PlayerUpgrade.FISHING_NET, "cooldown", 1) * 20);
                helper.assertTrue(ModuleCooldowns.remaining(player, PlayerUpgrade.FISHING_NET) == cooldown, "configured cooldown");
                helper.assertTrue(Skills.activateEquipped(player) instanceof Skills.Cooldown, "then the net cools down");
            } finally {
                restore.run();
            }
            helper.succeed();
        });
    }

    /** Hof: mehr Wolle, schneller wachsende Jungtiere, Flächenernte mit Nachpflanzen, Mehr-Ertrag ohne Saatgut. */
    private static void farmUpgrades(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();

        Player shearer = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerUpgrades.of(shearer).setLevel(PlayerUpgrade.WOOL_BONUS, 2);
        shearer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        Sheep sheep = helper.spawnWithNoFreeWill(EntityTypes.SHEEP, new BlockPos(1, 2, 1));
        shearer.interactOn(sheep, InteractionHand.MAIN_HAND, sheep.position());
        int wool = shearer.getInventory().getNonEquipmentItems().stream()
                .filter(s -> s.is(ItemTags.WOOL)).mapToInt(ItemStack::getCount).sum();
        int extraWool = (int) value(PlayerUpgrade.WOOL_BONUS, 2);
        helper.assertTrue(sheep.isSheared() && wool >= 1 + extraWool, "vanilla wool plus " + extraWool + " extra in the inventory, got " + wool);
        helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, sheep.getBoundingBox().inflate(3)).stream()
                .noneMatch(e -> e.getItem().is(ItemTags.WOOL)), "no wool lies around the sheep");
        helper.assertTrue(shearer.getMainHandItem().getDamageValue() == 1, "shearing costs one durability");

        Player feeder = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerUpgrades.of(feeder).setLevel(PlayerUpgrade.BABY_GROWTH, 2);
        feeder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WHEAT, 4));
        Cow calf = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(3, 2, 1));
        calf.setAge(-24000);
        feeder.interactOn(calf, InteractionHand.MAIN_HAND, calf.position());
        int grownTo = (int) (-24000 * (1.0 - Math.min(1.0, value(PlayerUpgrade.BABY_GROWTH, 2))));
        helper.assertTrue(calf.getAge() >= grownTo, "feeding grows the calf by the configured share and more, age " + calf.getAge());

        ServerPlayer farmer = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        farmer.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        BlockPos centerRel = new BlockPos(3, 2, 4);
        List<BlockPos> field = new ArrayList<>();
        for (int x = 1; x <= 5; x++) {
            for (int z = 2; z <= 6; z++) {
                field.add(new BlockPos(x, 2, z));
            }
        }
        BlockState ripe = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE);
        try {
            field.forEach(p -> {
                helper.setBlock(p.below(), Blocks.FARMLAND);
                helper.setBlock(p, ripe);
            });
            helper.setBlock(new BlockPos(2, 2, 3), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3));
            int harvested = HoeHarvest.harvest(level, farmer, helper.absolutePos(centerRel), hoe, InteractionHand.MAIN_HAND, 1);
            helper.assertTrue(harvested == 8, "eight ripe plants in 3x3, one unripe, got " + harvested);
            helper.assertTrue(helper.getBlockState(centerRel).getValue(CropBlock.AGE) == 0, "replanted at age 0");
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 2, 3)).getValue(CropBlock.AGE) == 3, "unripe plants stay");
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 2)).getValue(CropBlock.AGE) == CropBlock.MAX_AGE, "outside the area stays ripe");
            helper.assertTrue(hoe.getDamageValue() == 8, "one durability per plant, got " + hoe.getDamageValue());
            int wheat = farmer.getInventory().countItem(Items.WHEAT);
            helper.assertTrue(wheat >= 8, "the yield goes straight to the inventory, got " + wheat);
            AABB fieldBox = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(1, 1, 2))),
                    Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(6, 4, 7))));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, fieldBox).isEmpty(), "nothing is left lying on the field");
        } finally {
            field.forEach(p -> {
                helper.setBlock(p, Blocks.AIR);
                helper.setBlock(p.below(), Blocks.AIR);
            });
        }

        // Zuckerrohr und Kaktus: Säulen über dem untersten Block, je Säule 1 Haltbarkeit, alles ins Inventar.
        columnHarvest(helper, farmer, Blocks.SUGAR_CANE, Items.SUGAR_CANE);
        columnHarvest(helper, farmer, Blocks.CACTUS, Items.CACTUS);

        // Kakao an einer Wand aus Tropenholz: geerntet wird eine senkrechte Wand an der Stammseite, nur Kakao mit
        // derselben Ausrichtung; die Ausrichtung bleibt beim Nachpflanzen, die Drops landen beim Spieler.
        ItemStack cocoaHoe = new ItemStack(Items.IRON_HOE);
        farmer.setItemInHand(InteractionHand.MAIN_HAND, cocoaHoe);
        BlockState ripeCocoa = Blocks.COCOA.defaultBlockState()
                .setValue(CocoaBlock.FACING, Direction.SOUTH).setValue(CocoaBlock.AGE, CocoaBlock.MAX_AGE);
        List<BlockPos> trunk = new ArrayList<>();
        for (int x = 1; x <= 4; x++) {
            for (int y = 2; y <= 5; y++) {
                trunk.add(new BlockPos(x, y, 9));
            }
        }
        BlockPos otherTrunk = new BlockPos(1, 4, 7);
        BlockPos otherSide = new BlockPos(1, 4, 8);
        try {
            trunk.forEach(p -> helper.setBlock(p, Blocks.JUNGLE_LOG));
            helper.setBlock(otherTrunk, Blocks.JUNGLE_LOG);
            trunk.forEach(p -> helper.setBlock(p.north(), ripeCocoa));
            helper.setBlock(otherSide, ripeCocoa.setValue(CocoaBlock.FACING, Direction.NORTH));
            helper.assertTrue(HoeHarvest.isMature(ripeCocoa), "ripe cocoa counts as mature");
            helper.assertTrue(!HoeHarvest.isMature(ripeCocoa.setValue(CocoaBlock.AGE, 1)), "unripe cocoa does not");
            BlockPos feet = helper.absolutePos(new BlockPos(2, 2, 12));
            farmer.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0f, 0f);
            int pods = HoeHarvest.harvest(level, farmer, helper.absolutePos(new BlockPos(2, 3, 8)), cocoaHoe, InteractionHand.MAIN_HAND, 1);
            helper.assertTrue(pods == 8, "3x3 wall of pods minus the one facing the other trunk, got " + pods);
            BlockState replanted = helper.getBlockState(new BlockPos(2, 3, 8));
            helper.assertTrue(replanted.is(Blocks.COCOA) && replanted.getValue(CocoaBlock.AGE) == 0
                    && replanted.getValue(CocoaBlock.FACING) == Direction.SOUTH, "cocoa replanted at age 0 facing the trunk: " + replanted);
            helper.assertTrue(helper.getBlockState(new BlockPos(1, 2, 8)).getValue(CocoaBlock.AGE) == 0, "the wall reaches down");
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 4, 8)).getValue(CocoaBlock.AGE) == 0, "the wall reaches up");
            helper.assertTrue(helper.getBlockState(otherSide).getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE,
                    "a pod on another trunk side stays");
            helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 8)).getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE,
                    "outside the width stays ripe");
            helper.assertTrue(helper.getBlockState(new BlockPos(2, 5, 8)).getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE,
                    "outside the height stays ripe");
            int beans = farmer.getInventory().countItem(Items.COCOA_BEANS);
            helper.assertTrue(beans == 16, "eight pods of three beans minus eight replanted go straight to the inventory, got " + beans);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(feet).inflate(1)).isEmpty(),
                    "nothing drops at the player's feet while the inventory has room");
            AABB wall = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(1, 2, 8))),
                    Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(5, 6, 9))));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, wall).isEmpty(), "no drops on the replanted pods");
        } finally {
            trunk.forEach(p -> helper.setBlock(p.north(), Blocks.AIR));
            trunk.forEach(p -> helper.setBlock(p, Blocks.AIR));
            helper.setBlock(otherTrunk, Blocks.AIR);
        }

        List<ItemStack> drops = List.of(new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT_SEEDS, 2));
        List<ItemStack> bonus = HoeHarvest.bonusDrops(drops, 1.0, level.getRandom());
        helper.assertTrue(bonus.size() == 1 && bonus.getFirst().is(Items.WHEAT), "the bonus copies the yield, not the seeds: " + bonus);
        helper.assertTrue(HoeHarvest.bonusDrops(drops, 0.0, level.getRandom()).isEmpty(), "no bonus without the upgrade");
        List<ItemStack> beans = HoeHarvest.bonusDrops(List.of(new ItemStack(Items.COCOA_BEANS, 3)), 1.0, level.getRandom());
        helper.assertTrue(beans.size() == 1 && beans.getFirst().is(Items.COCOA_BEANS) && beans.getFirst().getCount() == 3,
                "cocoa beans are yield, not seeds: " + beans);
        helper.succeed();
    }

    /**
     * Flächenernte an Säulenpflanzen im 3×3: acht Säulen der Höhe 3, eine davon nur aus der Basis, die Mitte mit einer
     * um einen Block höheren Basis; beim Kaktus trägt eine Säule eine Blüte. Geerntet wird alles über den Basen.
     */
    private static void columnHarvest(GameTestHelper helper, ServerPlayer farmer, Block plant, Item item) {
        ServerLevel level = helper.getLevel();
        ItemStack hoe = new ItemStack(Items.IRON_HOE);
        farmer.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        int before = farmer.getInventory().countItem(item);
        int flowersBefore = farmer.getInventory().countItem(Items.CACTUS_FLOWER);
        BlockPos center = new BlockPos(2, 2, 3);
        BlockPos stub = new BlockPos(1, 2, 2);
        BlockPos empty = new BlockPos(3, 2, 4);
        BlockPos flowered = new BlockPos(3, 2, 2);
        List<BlockPos> bases = new ArrayList<>();
        for (int x = 1; x <= 3; x++) {
            for (int z = 2; z <= 4; z++) {
                BlockPos base = new BlockPos(x, 2, z);
                if (base.equals(empty)) {
                    continue;
                }
                bases.add(base.equals(center) ? base.above() : base);
            }
        }
        try {
            for (BlockPos base : bases) {
                helper.setBlock(base.below(), Blocks.SAND);
                int height = base.equals(stub) ? 1 : 3;
                for (int i = 0; i < height; i++) {
                    helper.setBlock(base.above(i), plant);
                }
            }
            boolean cactus = plant == Blocks.CACTUS;
            if (cactus) {
                helper.setBlock(flowered.above(3), Blocks.CACTUS_FLOWER);
            }
            int columns = HoeHarvest.harvestColumns(level, farmer, helper.absolutePos(center.above(2)), hoe, InteractionHand.MAIN_HAND, 1);
            helper.assertTrue(columns == 7, plant + ": seven columns above their base, got " + columns);
            helper.assertTrue(bases.stream().allMatch(b -> helper.getBlockState(b).is(plant) && helper.getBlockState(b.above()).isAir()),
                    plant + ": every base stays, everything above is gone");
            int got = farmer.getInventory().countItem(item) - before;
            helper.assertTrue(got == 14, plant + ": two blocks of seven columns go to the inventory, got " + got);
            if (cactus) {
                helper.assertTrue(farmer.getInventory().countItem(Items.CACTUS_FLOWER) - flowersBefore == 1, "the cactus flower is harvested too");
            }
            helper.assertTrue(hoe.getDamageValue() == 7, plant + ": one durability per column, got " + hoe.getDamageValue());
            AABB box = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 1, 1))),
                    Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(5, 7, 6))));
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, box).isEmpty(), plant + ": nothing drops on the field");
        } finally {
            for (BlockPos base : bases) {
                for (int i = 3; i >= 0; i--) {
                    helper.setBlock(base.above(i), Blocks.AIR);
                }
                helper.setBlock(base.below(), Blocks.AIR);
            }
        }
    }

    /**
     * Fortune auf Weizen und Rote Bete: Die Loot-Modifier {@code data/dtp/loot_modifiers/*_fortune.json} hängen je
     * reifer Pflanze zusätzlich binomial(Fortune-Stufe, 4/7) Stück Erntegut an. Ohne Fortune bleibt es bei genau 1.
     * Fortune+ zählt mit, weil der Bonus über {@code apply_bonus} läuft.
     */
    private static void cropFortune(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        ItemStack plain = new ItemStack(Items.IRON_HOE);
        ItemStack fortune = enchanted(level, Items.IRON_HOE, false, Enchantments.FORTUNE, 3);
        record Crop(BlockState ripe, BlockState unripe, Item yield) {
        }
        List<Crop> crops = List.of(
                new Crop(Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, CropBlock.MAX_AGE),
                        Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 3), Items.WHEAT),
                new Crop(Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, BeetrootBlock.MAX_AGE),
                        Blocks.BEETROOTS.defaultBlockState().setValue(BeetrootBlock.AGE, 1), Items.BEETROOT));
        int runs = 200;
        for (Crop crop : crops) {
            String name = crop.yield().toString();
            int total = 0;
            for (int i = 0; i < runs; i++) {
                int count = yieldOf(Block.getDrops(crop.ripe(), level, pos, null, player, plain), crop.yield());
                helper.assertTrue(count == 1, name + ": exactly one without fortune, got " + count);
            }
            for (int i = 0; i < runs; i++) {
                int count = yieldOf(Block.getDrops(crop.ripe(), level, pos, null, player, fortune), crop.yield());
                helper.assertTrue(count >= 1 && count <= 4, name + ": fortune III gives 1 to 4, got " + count);
                total += count;
            }
            double mean = (double) total / runs;
            helper.assertTrue(mean > 2.4 && mean < 3.0, name + ": fortune III averages about 2.7, got " + mean);
            int unripe = yieldOf(Block.getDrops(crop.unripe(), level, pos, null, player, fortune), crop.yield());
            helper.assertTrue(unripe == 0, name + ": no yield from an unripe plant, got " + unripe);
        }

        // Fortune+ ohne Verzauberung: Die Stufe aus dem Upgrade reicht für den Bonus.
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.FORTUNE_BONUS, 1);
        int boosted = 0;
        for (int i = 0; i < runs; i++) {
            boosted += yieldOf(Block.getDrops(crops.getFirst().ripe(), level, pos, null, player, plain), Items.WHEAT);
        }
        helper.assertTrue(boosted > runs * 1.2, "fortune+ raises the wheat yield too, got " + boosted + " from " + runs);
        helper.succeed();
    }

    private static int yieldOf(List<ItemStack> drops, Item item) {
        return drops.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    /** Verzauberungsboni: Unbreaking auf Hände und Rüstung, Protection nur Rüstung; Schwimm- und Abbautempo. */
    private static void enchantBonuses(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        Holder<Enchantment> efficiency = enchantments.getOrThrow(Enchantments.EFFICIENCY);
        Holder<Enchantment> protection = enchantments.getOrThrow(Enchantments.PROTECTION);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        mine.setLevel(PlayerUpgrade.UNBREAKING_BONUS, 2);
        mine.setLevel(PlayerUpgrade.EFFICIENCY_BONUS, 1);
        mine.setLevel(PlayerUpgrade.PROTECTION_BONUS, 1);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        pickaxe.enchant(unbreaking, 3);
        ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
        player.setItemSlot(EquipmentSlot.MAINHAND, pickaxe);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        PlayerUpgradeEffects.Holding inHand = PlayerUpgradeEffects.holder(pickaxe, List.of(player));
        PlayerUpgradeEffects.Holding worn = PlayerUpgradeEffects.holder(chest, List.of(player));
        helper.assertTrue(inHand != null && inHand.slot() == EquipmentSlot.MAINHAND && worn != null && worn.slot() == EquipmentSlot.CHEST,
                "holders are found by slot");
        helper.assertTrue(PlayerUpgradeEffects.holder(pickaxe.copy(), List.of(player)) == null, "only the very same stack counts");

        GetEnchantmentLevelEvent tool = new GetEnchantmentLevelEvent(pickaxe, new ItemEnchantments.Mutable(pickaxe.getEnchantments()), null, enchantments);
        PlayerUpgradeEffects.addBonuses(tool, pickaxe, inHand);
        int toolUnbreaking = 3 + (int) value(PlayerUpgrade.UNBREAKING_BONUS, 2);
        helper.assertTrue(tool.getEnchantments().getLevel(unbreaking) == toolUnbreaking,
                "unbreaking III + bonus = " + toolUnbreaking + ", even beyond the vanilla maximum");
        helper.assertTrue(tool.getEnchantments().getLevel(efficiency) == 0, "efficiency is an attribute, not an enchantment level");
        helper.assertTrue(tool.getEnchantments().getLevel(protection) == 0, "no protection on a pickaxe");

        GetEnchantmentLevelEvent armor = new GetEnchantmentLevelEvent(chest, new ItemEnchantments.Mutable(chest.getEnchantments()), null, enchantments);
        PlayerUpgradeEffects.addBonuses(armor, chest, worn);
        helper.assertTrue(armor.getEnchantments().getLevel(protection) == (int) value(PlayerUpgrade.PROTECTION_BONUS, 1)
                        && armor.getEnchantments().getLevel(unbreaking) == (int) value(PlayerUpgrade.UNBREAKING_BONUS, 2),
                "armor gets the configured protection and unbreaking");
        helper.assertTrue(armor.getEnchantments().getLevel(efficiency) == 0, "no efficiency on armor");

        // Bis 4.3 gab es Haltbarkeit 3 und Schutz 2; gespeicherte alte Käufe wirken wie die neue Höchststufe.
        mine.setLevel(PlayerUpgrade.UNBREAKING_BONUS, 3);
        mine.setLevel(PlayerUpgrade.PROTECTION_BONUS, 2);
        GetEnchantmentLevelEvent capped = new GetEnchantmentLevelEvent(chest, new ItemEnchantments.Mutable(chest.getEnchantments()), null, enchantments);
        PlayerUpgradeEffects.addBonuses(capped, chest, worn);
        int maxUnbreaking = (int) value(PlayerUpgrade.UNBREAKING_BONUS, PlayerUpgradeDefinitions.get(PlayerUpgrade.UNBREAKING_BONUS).maxLevel());
        int maxProtection = (int) value(PlayerUpgrade.PROTECTION_BONUS, PlayerUpgradeDefinitions.get(PlayerUpgrade.PROTECTION_BONUS).maxLevel());
        helper.assertTrue(capped.getEnchantments().getLevel(unbreaking) == maxUnbreaking
                        && capped.getEnchantments().getLevel(protection) == maxProtection,
                "levels above the maximum count as the maximum: unbreaking " + capped.getEnchantments().getLevel(unbreaking)
                        + ", protection " + capped.getEnchantments().getLevel(protection));
        mine.setLevel(PlayerUpgrade.UNBREAKING_BONUS, 2);
        mine.setLevel(PlayerUpgrade.PROTECTION_BONUS, 1);

        mine.setLevel(PlayerUpgrade.SWIM_SPEED, 2);
        player.setSwimming(false);
        PlayerUpgradeEffects.refresh(player);
        double walking = player.getAttributeValue(NeoForgeMod.SWIM_SPEED);
        helper.assertTrue(Math.abs(walking - 1.0) < 1e-9, "swim speed outside swimming pose " + walking + " expected 1.0");
        player.setSwimming(true);
        PlayerUpgradeEffects.updateSwimSpeed(player);
        double swim = player.getAttributeValue(NeoForgeMod.SWIM_SPEED);
        double expected = 1.0 + PlayerUpgradeDefinitions.get(PlayerUpgrade.SWIM_SPEED).value(2);
        helper.assertTrue(Math.abs(swim - expected) < 1e-9, "swim speed " + swim + " expected " + expected);
        player.setSwimming(false);
        PlayerUpgradeEffects.updateSwimSpeed(player);
        double after = player.getAttributeValue(NeoForgeMod.SWIM_SPEED);
        helper.assertTrue(Math.abs(after - 1.0) < 1e-9, "swim speed after swimming " + after + " expected 1.0");
        double breakSpeed = player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        double expectedBreak = 1.0 + PlayerUpgradeDefinitions.get(PlayerUpgrade.EFFICIENCY_BONUS).value(1);
        helper.assertTrue(Math.abs(breakSpeed - expectedBreak) < 1e-9, "break speed " + breakSpeed + " expected " + expectedBreak);
        helper.succeed();
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Angelschatz. Die Fundliste data/dtp/loot_table/gameplay/fishing/structure_finds.json wird von Hand gepflegt
    // (siehe docs/71-entwicklung-und-test.md, „Angelschatz-Fundliste erweitern“). Die drei Tests prüfen sie in Stufen:
    //   fishing_finds_syntax  – JSON lesbar, jeder Eintrag einzeln parsebar, Gewichte gültig, Vanilla-Validierung sauber
    //   fishing_finds_entries – jeder Eintrag liefert das genannte Item; Verzauberungsfunktionen verzaubern wirklich
    //   fishing_treasure      – der Loot-Modifier hängt Funde an die Vanilla-Angel, nur mit Upgrade, in passender Rate
    // Fehlermeldungen nennen die Eintragsnummer (ab 0, je Pool) und den Item-Namen, damit man die Stelle schnell findet.
    // ---------------------------------------------------------------------------------------------------------------

    private static final Identifier FISHING_FINDS_FILE = Identifier.fromNamespaceAndPath(Dtp.MODID,
            "loot_table/gameplay/fishing/structure_finds.json");
    private static final Set<String> ENCHANT_FUNCTIONS = Set.of("minecraft:enchant_randomly", "minecraft:enchant_with_levels");

    /** Syntax: Die Fundliste ist gültiges JSON, jeder Eintrag lässt sich lesen, und die Vanilla-Validierung findet nichts. */
    private static void fishingFindsSyntax(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        JsonObject json = fishingFindsJson(level, errors);
        if (json == null) {
            helper.fail(String.join("\n", errors));
            return;
        }
        RegistryOps<JsonElement> ops = level.getServer().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);

        // Jeden Eintrag einzeln lesen: Ein einziger Fehler (z. B. „minecraft:minecraft:…“) verwirft sonst still die ganze
        // Tabelle, und das Upgrade liefert gar nichts mehr.
        forEachFishingEntry(json, errors, (label, entry) -> {
            DataResult<LootTable> parsed = LootTable.DIRECT_CODEC.parse(ops, singleEntryTable(entry));
            parsed.error().ifPresent(e -> errors.add(label + ": " + e.message()));
            if (entry.has("weight") && (!entry.get("weight").isJsonPrimitive() || entry.get("weight").getAsInt() < 1)) {
                errors.add(label + ": weight muss eine ganze Zahl ≥ 1 sein, ist " + entry.get("weight"));
            }
            legacyKeys(label, entry, errors);
            for (JsonObject modifier : modifiers(entry)) {
                if (modifier.has("function")) {
                    errors.add(label + ": in einem modifier heißt \"function\" seit 26.3 \"type\"");
                }
            }
        });
        if (json.has("pools") && json.get("pools").isJsonArray()) {
            JsonArray pools = json.getAsJsonArray("pools");
            for (int p = 0; p < pools.size(); p++) {
                if (pools.get(p).isJsonObject()) {
                    legacyKeys("Pool " + p, pools.get(p).getAsJsonObject(), errors);
                }
            }
        }

        DataResult<LootTable> whole = LootTable.DIRECT_CODEC.parse(ops, json);
        whole.error().ifPresent(e -> errors.add("ganze Tabelle: " + e.message()));
        whole.result().ifPresent(table -> {
            // Dieselbe Prüfung wie beim Laden der Datapacks (Kontext-Parameter, Verweise auf andere Tabellen).
            ProblemReporter.Collector problems = new ProblemReporter.Collector();
            table.validate(new ValidationContext(problems, LootContextParamSets.FISHING, level.getServer().reloadableRegistries().lookup()));
            problems.forEach((path, problem) -> errors.add("Validierung " + path + ": " + problem.description()));
        });

        helper.assertTrue(level.getServer().reloadableRegistries().getLootTable(FishingTreasureModifier.TABLE) != LootTable.EMPTY,
                "Die Fundliste wurde beim Serverstart nicht geladen (Log nach „structure_finds“ durchsuchen)");
        helper.assertTrue(errors.isEmpty(), "Fehler in structure_finds.json:\n" + String.join("\n", errors));
        helper.succeed();
    }

    /**
     * Funktion je Eintrag: Jeder Eintrag wird als eigene Mini-Tabelle gewürfelt. Er muss das genannte Item liefern (aus
     * einem Buch darf ein verzaubertes Buch werden), Verzauberungsfunktionen müssen tatsächlich verzaubern, und
     * Schallplatten sind ausgeschlossen.
     */
    private static void fishingFindsEntries(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        List<String> errors = new ArrayList<>();
        JsonObject json = fishingFindsJson(level, errors);
        if (json == null) {
            helper.fail(String.join("\n", errors));
            return;
        }
        RegistryOps<JsonElement> ops = level.getServer().reloadableRegistries().lookup().createSerializationContext(JsonOps.INSTANCE);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        int[] count = {0};

        forEachFishingEntry(json, errors, (label, entry) -> {
            LootTable table = LootTable.DIRECT_CODEC.parse(ops, singleEntryTable(entry)).result().orElse(null);
            if (table == null) {
                errors.add(label + ": nicht lesbar (Details im Test fishing_finds_syntax)");
                return;
            }
            count[0]++;
            String type = entry.has("type") ? entry.get("type").getAsString() : "";
            String name = entry.has("name") ? entry.get("name").getAsString() : "";
            boolean conditional = entry.has("condition");
            boolean enchants = hasFunction(entry, ENCHANT_FUNCTIONS);
            for (int i = 0; i < 20; i++) {
                List<ItemStack> drops = table.getRandomItems(fishingParams(level, origin, rod, null, null));
                if (drops.isEmpty() && !conditional) {
                    errors.add(label + ": liefert nichts");
                    return;
                }
                for (ItemStack stack : drops) {
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (type.equals("minecraft:item") && !id.equals(name)
                            && !(name.equals("minecraft:book") && stack.is(Items.ENCHANTED_BOOK))) {
                        errors.add(label + ": liefert " + id + " statt " + name);
                        return;
                    }
                    if (stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
                        errors.add(label + ": Schallplatten gehören nicht in die Fundliste (" + id + ")");
                        return;
                    }
                    if (enchants && !isEnchanted(stack)) {
                        errors.add(label + ": hat eine Verzauberungsfunktion, " + id
                                + " kam aber unverzaubert heraus (passen die options zum Item?)");
                        return;
                    }
                }
            }
        });

        helper.assertTrue(count[0] > 0, "Die Fundliste hat keine lesbaren Einträge");
        helper.assertTrue(errors.isEmpty(), "Fehler in structure_finds.json:\n" + String.join("\n", errors));
        helper.succeed();
    }

    /**
     * Zusammenspiel mit dem Loot-Modifier: Ohne Upgrade bleibt die Vanilla-Angel unverändert (ein Stapel je Fang). Mit
     * höchster Stufe kommt in offenem Wasser in ungefähr der konfigurierten Rate ein zweiter Stapel aus der Fundliste
     * dazu — aus einem 1×1-Loch (AFK-Farm) nie.
     */
    private static void fishingTreasure(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable fishing = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        ItemStack rod = new ItemStack(Items.FISHING_ROD);

        // Welche Items die Fundliste liefern kann; bei Tag- oder Tabellen-Verweisen wird die Zugehörigkeit nicht geprüft.
        List<String> errors = new ArrayList<>();
        JsonObject json = fishingFindsJson(level, errors);
        helper.assertTrue(json != null, String.join("\n", errors));
        Set<String> possible = new HashSet<>();
        boolean[] onlyItems = {true};
        forEachFishingEntry(json, errors, (label, entry) -> {
            String type = entry.has("type") ? entry.get("type").getAsString() : "";
            if (type.equals("minecraft:item") && entry.has("name")) {
                possible.add(entry.get("name").getAsString());
                if (entry.get("name").getAsString().equals("minecraft:book")) {
                    possible.add("minecraft:enchanted_book");
                }
            } else {
                onlyItems[0] = false;
            }
        });

        Player angler = helper.makeMockPlayer(GameType.SURVIVAL);
        FishingHook openWater = new FishingHook(angler, level, 0, 0);
        FishingHook hole = new FishingHook(angler, level, 0, 0);
        setOpenWater(hole, false);
        helper.assertTrue(openWater.isOpenWaterFishing() && !hole.isOpenWaterFishing(), "Haken-Zustand für den Test");
        int rolls = 3000;
        helper.assertTrue(extraCatches(level, fishing, angler, openWater, origin, rod, 500, possible, onlyItems[0], errors) == 0,
                "Ohne Upgrade darf es keine Zusatzfunde geben");

        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.FISHING_TREASURE);
        helper.assertTrue(def != null, "player_upgrade/fishing_treasure.json fehlt");
        PlayerUpgrades.of(angler).setLevel(PlayerUpgrade.FISHING_TREASURE, def.maxLevel());
        double chance = def.value(def.maxLevel());
        helper.assertTrue(extraCatches(level, fishing, angler, hole, origin, rod, 1000, possible, onlyItems[0], errors) == 0,
                "Aus einem 1×1-Loch (kein offenes Wasser) darf es keine Zusatzfunde geben");
        int extra = extraCatches(level, fishing, angler, openWater, origin, rod, rolls, possible, onlyItems[0], errors);
        double expected = chance * rolls;
        helper.assertTrue(errors.isEmpty(), String.join("\n", errors));
        // Großzügiges Band (rund ±4 Standardabweichungen bei 2 % Chance), damit der Test nicht zufällig scheitert.
        helper.assertTrue(extra >= expected * 0.5 && extra <= expected * 1.6,
                "Zusatzfunde " + extra + " in " + rolls + " Fängen, erwartet etwa " + Math.round(expected) + " (Chance " + chance + ")");
        helper.succeed();
    }

    /**
     * Double Bobbers: Jeder Wurf liefert so viele Vanilla-Fänge, wie das Upgrade angibt, auch im 1×1-Loch. Der
     * Angelschatz wird je Fang gewürfelt; mit beiden Upgrades kommen Funde in etwa der doppelten Rate, auch zwei in
     * einem Wurf. Der Modifier hängt erst die weiteren Fänge, dann die Funde an.
     */
    private static void doubleBobbers(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable fishing = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        ItemStack rod = new ItemStack(Items.FISHING_ROD);

        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.DOUBLE_BOBBERS);
        helper.assertTrue(def != null, "player_upgrade/double_bobbers.json fehlt");
        int catches = (int) def.value(def.maxLevel());
        helper.assertTrue(catches >= 2, "double_bobbers liefert " + catches + " Items je Wurf, erwartet mindestens 2");

        Player angler = helper.makeMockPlayer(GameType.SURVIVAL);
        FishingHook openWater = new FishingHook(angler, level, 0, 0);
        FishingHook hole = new FishingHook(angler, level, 0, 0);
        setOpenWater(hole, false);
        helper.assertTrue(openWater.isOpenWaterFishing() && !hole.isOpenWaterFishing(), "Haken-Zustand für den Test");

        for (int i = 0; i < 200; i++) {
            int size = fishing.getRandomItems(fishingParams(level, origin, rod, angler, openWater)).size();
            helper.assertTrue(size == 1, "Ohne Upgrade genau 1 Item je Wurf, war " + size);
        }

        PlayerUpgrades.of(angler).setLevel(PlayerUpgrade.DOUBLE_BOBBERS, def.maxLevel());
        for (FishingHook hook : List.of(openWater, hole)) {
            for (int i = 0; i < 300; i++) {
                List<ItemStack> drops = fishing.getRandomItems(fishingParams(level, origin, rod, angler, hook));
                helper.assertTrue(drops.size() == catches, "Mit Double Bobbers " + catches + " Items je Wurf ("
                        + (hook == hole ? "1×1-Loch" : "offenes Wasser") + "), war " + drops.size());
                for (ItemStack stack : drops) {
                    helper.assertFalse(stack.isEmpty(), "Leerer Stapel im Fang");
                }
            }
        }

        PlayerUpgradeDefinitions.Definition treasure = PlayerUpgradeDefinitions.get(PlayerUpgrade.FISHING_TREASURE);
        helper.assertTrue(treasure != null, "player_upgrade/fishing_treasure.json fehlt");
        PlayerUpgrades.of(angler).setLevel(PlayerUpgrade.FISHING_TREASURE, treasure.maxLevel());
        double chance = treasure.value(treasure.maxLevel());
        int rolls = 3000;
        int finds = 0;
        int doubleFinds = 0;
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> drops = fishing.getRandomItems(fishingParams(level, origin, rod, angler, openWater));
            helper.assertTrue(drops.size() >= catches && drops.size() <= catches * 2,
                    "Wurf mit " + drops.size() + " Items, erwartet " + catches + " bis " + catches * 2);
            int extra = drops.size() - catches;
            finds += extra;
            if (extra >= 2) {
                doubleFinds++;
            }
        }
        double expected = chance * catches * rolls;
        helper.assertTrue(finds >= expected * 0.5 && finds <= expected * 1.6,
                "Funde " + finds + " in " + rolls + " Würfen, erwartet etwa " + Math.round(expected) + " (Chance " + chance + " × " + catches + ")");
        // Bei 8 % und 2 Fängen sind ≈ 19 Doppelfunde zu erwarten; erst unter rund 1 % Chance wäre 0 plausibel.
        helper.assertTrue(chance < 0.03 || doubleFinds > 0, "Kein Wurf mit zwei Funden in " + rolls + " Würfen");
        helper.succeed();
    }

    /**
     * Zählt Fänge mit Zusatzfund. Vanilla liefert genau einen Stapel je Fang; der Modifier hängt seinen Fund hinten an.
     * Zusatzstapel, die nicht aus der Fundliste stammen können, landen in {@code errors}.
     */
    private static int extraCatches(ServerLevel level, LootTable fishing, Player angler, FishingHook hook, Vec3 origin, ItemStack rod,
            int rolls, Set<String> possible, boolean checkPossible, List<String> errors) {
        int extra = 0;
        for (int i = 0; i < rolls; i++) {
            List<ItemStack> drops = fishing.getRandomItems(fishingParams(level, origin, rod, angler, hook));
            for (ItemStack stack : drops) {
                if (isBlackmarketFind(stack)) {
                    errors.add("Schwarzmarktfund " + stack + " beim Angeln, der gehört nur in Strukturkisten");
                }
            }
            if (drops.size() > 1) {
                extra++;
                for (ItemStack stack : drops.subList(1, drops.size())) {
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    if (checkPossible && !possible.contains(id)) {
                        errors.add("Zusatzfund " + id + " steht nicht in der Fundliste");
                    }
                }
            }
        }
        return extra;
    }

    /** Loot-Kontext wie beim echten Angeln ({@code FishingHook}): Haken als {@code THIS_ENTITY}, Angler als Angreifer. */
    private static LootParams fishingParams(ServerLevel level, Vec3 origin, ItemStack rod, Player angler, FishingHook hook) {
        LootParams.Builder builder = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.TOOL, rod);
        if (angler != null) {
            builder.withParameter(LootContextParams.ATTACKING_ENTITY, angler);
        }
        if (hook != null) {
            builder.withParameter(LootContextParams.THIS_ENTITY, hook);
        }
        return builder.create(LootContextParamSets.FISHING);
    }

    /**
     * Setzt den Offenes-Wasser-Zustand des Hakens. Vanilla ermittelt ihn nur im Tick aus der Umgebung (5×5 Wasser);
     * für den Test reicht das Feld direkt.
     */
    private static void setOpenWater(FishingHook hook, boolean open) {
        try {
            java.lang.reflect.Field field = FishingHook.class.getDeclaredField("openWater");
            field.setAccessible(true);
            field.setBoolean(hook, open);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FishingHook.openWater nicht gefunden — nach einem Minecraft-Update prüfen", e);
        }
    }

    /** Liest die Fundliste roh aus den Ressourcen; Lese- und JSON-Syntaxfehler (mit Zeilenangabe) landen in {@code errors}. */
    private static JsonObject fishingFindsJson(ServerLevel level, List<String> errors) {
        try (Reader reader = level.getServer().getResourceManager().openAsReader(FISHING_FINDS_FILE)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            errors.add(FISHING_FINDS_FILE + " ist kein gültiges JSON: " + e.getMessage());
            return null;
        }
    }

    /** Ruft {@code action} für jeden Eintrag aller Pools auf, mit einer lesbaren Bezeichnung wie „Pool 0, Eintrag 3 (minecraft:book)“. */
    private static void forEachFishingEntry(JsonObject json, List<String> errors, java.util.function.BiConsumer<String, JsonObject> action) {
        if (!json.has("pools") || !json.get("pools").isJsonArray()) {
            errors.add("\"pools\" fehlt oder ist keine Liste");
            return;
        }
        JsonArray pools = json.getAsJsonArray("pools");
        for (int p = 0; p < pools.size(); p++) {
            JsonObject pool = pools.get(p).getAsJsonObject();
            if (!pool.has("entries") || !pool.get("entries").isJsonArray()) {
                errors.add("Pool " + p + ": \"entries\" fehlt oder ist keine Liste");
                continue;
            }
            JsonArray entries = pool.getAsJsonArray("entries");
            for (int i = 0; i < entries.size(); i++) {
                JsonObject entry = entries.get(i).getAsJsonObject();
                String name = entry.has("name") ? entry.get("name").getAsString() : entry.has("type") ? entry.get("type").getAsString() : "?";
                action.accept("Pool " + p + ", Eintrag " + i + " (" + name + ")", entry);
            }
        }
    }

    /** Eine Angel-Tabelle mit nur diesem Eintrag, damit Fehler und Würfe genau einem Eintrag zugeordnet werden. */
    private static JsonObject singleEntryTable(JsonObject entry) {
        JsonArray entries = new JsonArray();
        entries.add(entry);
        JsonObject pool = new JsonObject();
        pool.addProperty("rolls", 1);
        pool.add("entries", entries);
        JsonArray pools = new JsonArray();
        pools.add(pool);
        JsonObject table = new JsonObject();
        table.addProperty("type", "minecraft:fishing");
        table.add("pools", pools);
        return table;
    }

    /**
     * Feldnamen aus dem Loot-Format vor 26.3. Der Codec ignoriert unbekannte Felder still — ein Eintrag mit „functions“
     * lädt also fehlerfrei, verzaubert aber nichts. Deshalb werden die alten Namen ausdrücklich gemeldet.
     */
    private static void legacyKeys(String label, JsonObject object, List<String> errors) {
        if (object.has("functions")) {
            errors.add(label + ": \"functions\" heißt seit 26.3 \"modifier\" (wird sonst still ignoriert)");
        }
        if (object.has("conditions")) {
            errors.add(label + ": \"conditions\" heißt seit 26.3 \"condition\" (wird sonst still ignoriert)");
        }
    }

    /** Die Modifier eines Eintrags; {@code modifier} darf ein einzelnes Objekt oder eine Liste sein. */
    private static List<JsonObject> modifiers(JsonObject entry) {
        List<JsonObject> result = new ArrayList<>();
        JsonElement modifier = entry.get("modifier");
        if (modifier == null) {
            return result;
        }
        for (JsonElement element : modifier.isJsonArray() ? modifier.getAsJsonArray().asList() : List.of(modifier)) {
            if (element.isJsonObject()) {
                result.add(element.getAsJsonObject());
            }
        }
        return result;
    }

    private static boolean hasFunction(JsonObject entry, Set<String> functions) {
        for (JsonObject modifier : modifiers(entry)) {
            if (modifier.has("type")) {
                String id = modifier.get("type").getAsString();
                if (functions.contains(id.contains(":") ? id : "minecraft:" + id)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isEnchanted(ItemStack stack) {
        return !stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
                || !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
    }

    /**
     * Amboss: Der Nachbau rechnet in normalen Fällen genau wie Vanilla (Schutz gegen Abweichungen nach Updates), und mit
     * Server-Upgrade gibt es bei Kosten über 40 ein Ergebnis zu 39 statt „Too Expensive“.
     */
    private static void anvilCap(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Holder<Enchantment> sharpness = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS);
        Player smith = helper.makeMockPlayer(GameType.SURVIVAL);

        ItemStack worn = new ItemStack(Items.DIAMOND_SWORD);
        worn.setDamageValue(1000);
        ItemStack sharp = new ItemStack(Items.DIAMOND_SWORD);
        sharp.enchant(sharpness, 3);
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        EnchantmentHelper.updateEnchantments(book, m -> m.set(sharpness, 3));
        for (ItemStack[] pair : List.of(new ItemStack[] {worn, new ItemStack(Items.DIAMOND, 2)}, new ItemStack[] {sharp, book})) {
            AnvilMenu menu = new AnvilMenu(0, smith.getInventory());
            menu.getSlot(0).set(pair[0].copy());
            menu.getSlot(1).set(pair[1].copy());
            AnvilCap.Result ours = AnvilCap.compute(pair[0], pair[1], null, smith, Integer.MAX_VALUE);
            ItemStack vanilla = menu.getSlot(2).getItem();
            helper.assertTrue(ours != null && ItemStack.matches(ours.output(), vanilla) && ours.cost() == menu.getCost(),
                    "the rebuild matches vanilla for " + pair[0] + " + " + pair[1] + ": " + (ours == null ? null : ours.output()) + " vs " + vanilla);
        }

        ItemStack costly = sharp.copy();
        costly.set(DataComponents.REPAIR_COST, 63);
        // Server-Upgrade: wirkt für alle Spieler, also den Weltstand setzen und danach zurückstellen.
        WorldProgress progress = WorldProgress.get(level.getServer());
        int oldCap = progress.level(ServerUpgrade.ANVIL_CAP);
        try {
            progress.setLevel(ServerUpgrade.ANVIL_CAP, 0);
            AnvilMenu plain = new AnvilMenu(0, smith.getInventory());
            plain.getSlot(0).set(costly.copy());
            plain.getSlot(1).set(book.copy());
            helper.assertTrue(plain.getSlot(2).getItem().isEmpty(), "too expensive without the upgrade");

            progress.setLevel(ServerUpgrade.ANVIL_CAP, 1);
            AnvilMenu capped = new AnvilMenu(0, smith.getInventory());
            capped.getSlot(0).set(costly.copy());
            capped.getSlot(1).set(book.copy());
            ItemStack result = capped.getSlot(2).getItem();
            helper.assertTrue(!result.isEmpty() && capped.getCost() == AnvilCap.MAX_COST && result.getEnchantments().getLevel(sharpness) == 4,
                    "with the server upgrade the anvil charges 39 levels: " + result + " cost " + capped.getCost());
        } finally {
            progress.setLevel(ServerUpgrade.ANVIL_CAP, oldCap);
        }
        helper.succeed();
    }

    /**
     * Werkstatt-Baum: Mehrstufige Upgrades zeigen ein Feld je Stufe (außer Maximale Gesundheit), alle Felder liegen im
     * Raster 9 × 4, und keine zwei Felder eines Tabs teilen sich einen Platz — schützt auch spätere Datapack-Änderungen.
     */
    private static void workshopLayout(GameTestHelper helper) {
        Map<PlayerUpgradeDefinitions.Tab, Map<Long, String>> used = new HashMap<>();
        for (PlayerUpgrade u : PlayerUpgrade.values()) {
            PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(u);
            helper.assertTrue(def != null, "every upgrade has a definition: " + u.id);
            if (u == PlayerUpgrade.MAX_HEALTH) {
                helper.assertFalse(def.split(), "max health stays one node");
            } else if (def.maxLevel() > 1) {
                helper.assertTrue(def.split(), u.id + " shows one node per level");
            }
            int nodes = def.split() ? def.maxLevel() : 1;
            for (int level = 1; level <= nodes; level++) {
                int[] p = def.position(level);
                helper.assertTrue(p[0] >= 0 && p[0] < TreeLayout.COLUMNS && p[1] >= 0 && p[1] < TreeLayout.ROWS,
                        u.id + " level " + level + " outside the grid");
                String other = used.computeIfAbsent(def.tab(), t -> new HashMap<>()).put(p[0] * 100L + p[1], u.id + " " + level);
                helper.assertTrue(other == null, u.id + " level " + level + " overlaps " + other + " at " + p[0] + "," + p[1]);
            }
        }
        helper.succeed();
    }

    /**
     * Beacon-Baum: jedes Server-Upgrade hat einen Platz, mehrstufige zeigen ein Feld je Stufe (außer dem Grenzradius),
     * Effekte hängen an Effektslot I, alle Felder liegen im Raster und keines teilt sich einen Platz.
     */
    private static void beaconLayout(GameTestHelper helper) {
        Map<UpgradeCosts.Tab, Map<Long, String>> used = new HashMap<>();
        for (ServerUpgrade u : ServerUpgrade.values()) {
            UpgradeCosts.Definition def = UpgradeCosts.get(u);
            helper.assertTrue(def != null, "every server upgrade has a definition: " + u.id);
            if (u == ServerUpgrade.BORDER) {
                helper.assertFalse(def.split(), "the border stays one node");
            } else if (def.maxLevel() > 1) {
                helper.assertTrue(def.split(), u.id + " shows one node per level");
            }
            if (u.isEffect()) {
                helper.assertTrue(def.tab() == UpgradeCosts.Tab.MODULES && def.requires().stream().anyMatch(r -> r.upgrade().equals("effect_slots")),
                        u.id + " sits in Modules and requires an effect slot");
            }
            int nodes = def.split() ? def.maxLevel() : 1;
            for (int level = 1; level <= nodes; level++) {
                int[] p = def.position(level);
                helper.assertTrue(p[0] >= 0 && p[0] < TreeLayout.COLUMNS && p[1] >= 0 && p[1] < TreeLayout.ROWS,
                        u.id + " level " + level + " outside the grid");
                String other = used.computeIfAbsent(def.tab(), t -> new HashMap<>()).put(p[0] * 100L + p[1], u.id + " " + level);
                helper.assertTrue(other == null, u.id + " level " + level + " overlaps " + other);
            }
        }
        helper.succeed();
    }

    /**
     * Mehr Contracts: +1 Platz je Stufe. Quick Delivery: höherer Bonus bei gleicher Frist, danach 100 % und doppelt so
     * schneller Wertverfall. Alte Spielerdaten mit Hot Delivery behalten die Stufe unter der neuen ID.
     */
    private static void tradeUpgrades(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        int base = DtpConfig.BASE_SLOTS.get();
        helper.assertTrue(Contracts.slots(player) == base, "base slots without upgrade");
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.CONTRACT_CAPACITY, 2);
        int extraSlots = (int) value(PlayerUpgrade.CONTRACT_CAPACITY, 2);
        helper.assertTrue(Contracts.slots(player) == base + extraSlots,
                extraSlots + " more slots on level 2, got " + Contracts.slots(player));

        Contract c = Contract.offer(Identifier.withDefaultNamespace("wheat"), DataComponentMatchers.ANY, 100, 1000, 1, 0.0);
        helper.assertTrue(Contracts.terms(player).equals(Contract.Terms.standard()), "standard terms without quick delivery");
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.QUICK_DELIVERY, 1);
        Contract.Terms quick = Contracts.terms(player);
        double days = c.bonusDays();
        helper.assertTrue(c.payout(0.0, quick) == Math.round(1000 * DtpConfig.QUICK_DELIVERY_FACTOR.get()), "quick delivery bonus within the window");
        helper.assertTrue(c.payout(days - 0.01, quick) == c.payout(0.0, quick) && c.payout(days - 0.01) == c.payout(0.0),
                "the window is the same with and without quick delivery");
        double decay = DtpConfig.DECAY_PER_INTERVAL.get() * DtpConfig.QUICK_DELIVERY_DECAY_MULTIPLIER.get();
        double interval = DtpConfig.DECAY_INTERVAL_DAYS.get();
        helper.assertTrue(Math.abs(c.factor(days + interval * 0.5, quick) - 1.0) < 1e-9,
                "after the window the bonus is gone: " + c.factor(days + interval * 0.5, quick));
        helper.assertTrue(Math.abs(c.factor(days + interval * 1.5, quick) - (1.0 - decay)) < 1e-9,
                "then the value drops by the doubled step: " + c.factor(days + interval * 1.5, quick));
        helper.assertTrue(Math.abs(c.factor(days + interval * 100, quick) - DtpConfig.MIN_FACTOR.get()) < 1e-9, "down to the floor");
        helper.assertTrue(Math.abs(c.nextDropStep(0.0, quick) - (quick.bonusFactor() - 1.0)) < 1e-9, "in the window the next step is the whole bonus");
        helper.assertTrue(Math.abs(c.nextDropStep(days + interval * 0.5, quick) - decay) < 1e-9, "after it the shown step is the doubled one");
        helper.assertTrue(c.payout(days + interval * 1.5, quick) < c.payout(days + interval * 1.5), "late quick delivery pays less than normal");

        CompoundTag v2 = new CompoundTag();
        v2.putInt(Versioned.KEY, 2);
        CompoundTag levels = new CompoundTag();
        levels.putInt("hot_delivery", 1);
        v2.put("levels", levels);
        PlayerUpgrades migrated = PlayerUpgrades.CODEC.parse(NbtOps.INSTANCE, v2).getOrThrow();
        helper.assertTrue(migrated.level(PlayerUpgrade.QUICK_DELIVERY) == 1, "bought hot delivery becomes quick delivery");
        helper.succeed();
    }

    /**
     * Fernlieferung: liefert aus Hauptinventar und Hotbar an alle angenommenen Contracts, nur im Territorium. Die
     * Zweithand bleibt unberührt, der Lohn geht ins Inventar. Das Upgrade setzt die Auftragsuhr voraus.
     */
    private static void remoteDelivery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0))));
        PlayerContracts mine = Contracts.of(player);
        double day = mine.ownDay();
        Contract wheat = Contract.offer(Identifier.withDefaultNamespace("wheat"), DataComponentMatchers.ANY, 100, 1000, 1, day);
        Contract carrots = Contract.offer(Identifier.withDefaultNamespace("carrot"), DataComponentMatchers.ANY, 64, 500, 1, day);
        mine.accepted().add(wheat);
        mine.accepted().add(carrots);
        player.getInventory().setItem(0, new ItemStack(Items.WHEAT, 64));
        player.getInventory().setItem(20, new ItemStack(Items.WHEAT, 50));
        player.getInventory().setItem(3, new ItemStack(Items.CARROT, 10));
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.CARROT, 30));

        Contracts.deliverRemote(level, player);
        helper.assertTrue(mine.accepted().get(0).delivered() == 0, "no remote delivery outside the territory");

        withTerritory(helper, () -> Contracts.deliverRemote(level, player));
        helper.assertTrue(mine.accepted().size() == 1, "the wheat contract is completed, carrots stay open");
        helper.assertTrue(mine.accepted().get(0).delivered() == 10, "10 carrots from the inventory, got " + mine.accepted().get(0).delivered());
        helper.assertTrue(player.getOffhandItem().getCount() == 30, "the off-hand stays untouched");
        int wheatLeft = 0;
        long taler = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.WHEAT)) {
                wheatLeft += stack.getCount();
            }
            taler += Taler.valueOf(stack);
        }
        helper.assertTrue(wheatLeft == 14, "only the missing 100 wheat are taken, left " + wheatLeft);
        helper.assertTrue(taler == wheat.payout(day), "reward " + wheat.payout(day) + " goes into the inventory, found " + taler);
        helper.assertTrue(mine.completed(1) == 1, "the remote completion counts like one at the board");

        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.REMOTE_DELIVERY);
        helper.assertTrue(def != null && def.requires().stream().anyMatch(r -> r.upgrade().equals(PlayerUpgrade.CONTRACT_CLOCK.id)),
                "remote delivery requires the contract clock");
        long clockCooldown = Math.round(param(PlayerUpgrade.REMOTE_DELIVERY, "cooldown", 1) * 20);
        helper.assertTrue(ContractClock.cooldownTicks() == clockCooldown,
                "configured cooldown " + clockCooldown + ", got " + ContractClock.cooldownTicks());
        helper.assertTrue(PlayerUpgradeDefinitions.get(PlayerUpgrade.CONTRACT_CLOCK) != null
                && PlayerUpgradeDefinitions.get(PlayerUpgrade.CONTRACT_CLOCK).cost(1) > 0, "the contract clock is for sale");
        helper.succeed();
    }

    /** Tooltip-Zeilen der Auftragsuhr: „Name: 23 / 64, 120%, 0.6 d“, an der Untergrenze ohne Tage. */
    private static void contractClockLines(GameTestHelper helper) {
        Contract c = Contract.offer(Identifier.withDefaultNamespace("carrot"), DataComponentMatchers.ANY, 64, 1000, 1, 0.0)
                .withDelivered(23);
        Contract.Terms terms = Contract.Terms.standard();
        double bonus = c.bonusDays();
        double interval = DtpConfig.DECAY_INTERVAL_DAYS.get();

        TranslatableContents inBonus = (TranslatableContents) ContractClock.line(c, bonus - 0.6, terms).getContents();
        helper.assertTrue(inBonus.getKey().equals("dtp.contract.clock.line"), "line key: " + inBonus.getKey());
        Object[] args = inBonus.getArgs();
        helper.assertTrue(args[1].equals(23) && args[2].equals(64), "delivered / count: " + args[1] + " / " + args[2]);
        long bonusPercent = Math.round(terms.bonusFactor() * 100.0);
        helper.assertTrue(args[3].equals(bonusPercent) && args[4].equals("0.6"),
                "in bonus: " + bonusPercent + "% and 0.6 d, got " + args[3] + ", " + args[4]);

        Object[] decaying = ((TranslatableContents) ContractClock.line(c, bonus + interval * 1.25, terms).getContents()).getArgs();
        String rest = String.format(java.util.Locale.ROOT, "%.1f", interval * 0.75);
        long decayPercent = Math.round(Math.max(DtpConfig.MIN_FACTOR.get(), 1.0 - terms.decayPerInterval()) * 100.0);
        helper.assertTrue(decaying[3].equals(decayPercent) && decaying[4].equals(rest),
                "decaying: " + decayPercent + "% and " + rest + " d, got " + decaying[3] + ", " + decaying[4]);

        TranslatableContents floor = (TranslatableContents) ContractClock.line(c, bonus + interval * 100, terms).getContents();
        helper.assertTrue(floor.getKey().equals("dtp.contract.clock.line_floor") && floor.getArgs().length == 4,
                "at the minimum the days are left out");
        helper.succeed();
    }

    /**
     * Spielautomat: Die Anteile der Ausgänge treffen die Config-Chancen, die Standard-Auszahlungsquote liegt unter 1
     * (Talersenke), und die Symbole passen immer zum gewürfelten Ausgang.
     */
    private static void slotMachineOdds(GameTestHelper helper) {
        double jackpot = DtpConfig.SLOT_CHANCE_JACKPOT.getDefault();
        double win = DtpConfig.SLOT_CHANCE_WIN.getDefault();
        double consolation = DtpConfig.SLOT_CHANCE_CONSOLATION.getDefault();
        double rtp = SlotMachine.returnToPlayer(jackpot, win, consolation);
        helper.assertTrue(rtp < 1.0 && rtp > 0.8, "default return to player should be below 1 and realistic, got " + rtp);

        int n = 200_000;
        RandomSource random = RandomSource.create(42);
        int[] counts = new int[SlotMachine.Outcome.values().length];
        for (int i = 0; i < n; i++) {
            counts[SlotMachine.roll(random, jackpot, win, consolation).ordinal()]++;
        }
        double[] expected = {jackpot, win, consolation, 1.0 - jackpot - win - consolation};
        for (SlotMachine.Outcome o : SlotMachine.Outcome.values()) {
            double p = expected[o.ordinal()];
            double tolerance = 4.0 * Math.sqrt(n * p * (1.0 - p));
            helper.assertTrue(Math.abs(counts[o.ordinal()] - n * p) <= tolerance,
                    o.id() + ": " + counts[o.ordinal()] + " of " + n + ", expected about " + (int) (n * p));
        }
        // Ergeben die Chancen mehr als 1, fallen die hinteren weg.
        helper.assertTrue(SlotMachine.roll(RandomSource.create(1), 0.0, 1.0, 1.0) == SlotMachine.Outcome.WIN, "capped chances");

        for (SlotMachine.Outcome o : SlotMachine.Outcome.values()) {
            for (int i = 0; i < 500; i++) {
                int[] reels = SlotMachine.symbols(o, random);
                helper.assertTrue(SlotMachine.outcomeOf(reels) == o, o.id() + " shows matching symbols, got " + Arrays.toString(reels));
                for (int r : reels) {
                    helper.assertTrue(r >= 0 && r < SlotMachine.SYMBOL_COUNT, "symbol index in range");
                }
            }
        }
        helper.succeed();
    }

    /** Auszahlung: Jackpot zwei Stufen höher, Gewinn eine Stufe höher, Trostpreis eine Stufe kleiner (Kupfertaler: nichts). */
    private static void slotMachinePayout(GameTestHelper helper) {
        for (Taler t : Taler.values()) {
            helper.assertTrue(SlotMachine.payout(SlotMachine.Outcome.JACKPOT, t) == t.value * 81, "jackpot for " + t.id);
            helper.assertTrue(SlotMachine.payout(SlotMachine.Outcome.WIN, t) == t.value * 9, "win for " + t.id);
            helper.assertTrue(SlotMachine.payout(SlotMachine.Outcome.NOTHING, t) == 0, "nothing for " + t.id);
            long consolation = SlotMachine.payout(SlotMachine.Outcome.CONSOLATION, t);
            helper.assertTrue(t == Taler.COPPER_TALER ? consolation == 0 : consolation == t.value / 9, "consolation for " + t.id);
        }
        helper.assertTrue(SlotMachine.payout(SlotMachine.Outcome.WIN, Taler.COPPER_SACK) == Taler.SILVER_TALER.value,
                "a won copper sack becomes a silver taler");
        List<ItemStack> jackpot = Taler.split(SlotMachine.payout(SlotMachine.Outcome.JACKPOT, Taler.GOLD_SACK));
        helper.assertTrue(jackpot.stream().allMatch(s -> s.is(Taler.GOLD_SACK.item()))
                        && jackpot.stream().mapToInt(ItemStack::getCount).sum() == 81,
                "a gold sack jackpot pays 81 gold sacks, got " + jackpot);
        helper.succeed();
    }

    // --- Recycler -------------------------------------------------------------------------------

    /**
     * Wie ein Ofen: Ohne Brennstoff passiert nichts. Mit einer Kohle dauern Lederstiefel {@link RecyclerBlockEntity#PROCESS_TICKS}
     * und ergeben 7 oder 8 Schrott (Wert 7,08); von der Kohle bleibt Glut für drei weitere Items. Der Ticker läuft hier
     * direkt, weil ein Item länger dauert als das Test-Timeout.
     */
    private static void recyclerProcess(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.RECYCLER.get());
        BlockPos pos = helper.absolutePos(rel);
        RecyclerBlockEntity recycler = helper.getBlockEntity(rel, RecyclerBlockEntity.class);
        recycler.setItem(RecyclerBlockEntity.INPUT, new ItemStack(Items.LEATHER_BOOTS));
        for (int i = 0; i < RecyclerBlockEntity.PROCESS_TICKS; i++) {
            RecyclerBlockEntity.serverTick(level, pos, level.getBlockState(pos), recycler);
        }
        helper.assertTrue(recycler.getItem(RecyclerBlockEntity.INPUT).is(Items.LEATHER_BOOTS) && recycler.progress() == 0,
                "without fuel nothing happens");

        recycler.setItem(RecyclerBlockEntity.FUEL, new ItemStack(Items.COAL));
        RecyclerBlockEntity.serverTick(level, pos, level.getBlockState(pos), recycler);
        helper.assertTrue(level.getBlockState(pos).getValue(RecyclerBlock.LIT) && recycler.getItem(RecyclerBlockEntity.FUEL).isEmpty(),
                "the coal is lit");
        for (int i = 1; i < RecyclerBlockEntity.PROCESS_TICKS - 1; i++) {
            RecyclerBlockEntity.serverTick(level, pos, level.getBlockState(pos), recycler);
        }
        helper.assertTrue(recycler.getItem(RecyclerBlockEntity.INPUT).is(Items.LEATHER_BOOTS), "one tick short, the boots are still there");
        RecyclerBlockEntity.serverTick(level, pos, level.getBlockState(pos), recycler);
        ItemStack out = recycler.getItem(RecyclerBlockEntity.OUTPUT);
        helper.assertTrue(recycler.getItem(RecyclerBlockEntity.INPUT).isEmpty(), "the boots are gone after " + RecyclerBlockEntity.PROCESS_TICKS + " ticks");
        double bootsValue = Recycling.entries().get(Items.LEATHER_BOOTS);
        helper.assertTrue(out.is(ModItems.SCRAP.get()) && out.getCount() >= Math.floor(bootsValue) && out.getCount() <= Math.ceil(bootsValue),
                "boots worth " + bootsValue + " scrap, got " + out);
        // Der Zündtick zählt noch nicht ab, wie im Vanilla-Ofen: 1600 − 399.
        helper.assertTrue(recycler.litTimeRemaining() == 1600 - (RecyclerBlockEntity.PROCESS_TICKS - 1),
                "a coal lasts for four items, remaining " + recycler.litTimeRemaining());
        helper.assertTrue(recycler.canTakeItemThroughFace(RecyclerBlockEntity.OUTPUT, out, Direction.DOWN), "scrap leaves through the bottom");
        helper.assertTrue(level.getBlockState(pos).getAnalogOutputSignal(level, pos, Direction.NORTH) > 0, "the comparator sees the scrap");
        cleanup(helper, rel);
        helper.succeed();
    }

    /** Hopper-Seiten wie beim Ofen: oben nur Recycelbares, seitlich nur Brennstoff, unten Schrott und leere Eimer. */
    private static void recyclerRejects(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.RECYCLER.get());
        RecyclerBlockEntity recycler = helper.getBlockEntity(rel, RecyclerBlockEntity.class);
        helper.assertTrue(Arrays.equals(recycler.getSlotsForFace(Direction.UP), new int[] {RecyclerBlockEntity.INPUT})
                && Arrays.equals(recycler.getSlotsForFace(Direction.NORTH), new int[] {RecyclerBlockEntity.FUEL})
                && Arrays.equals(recycler.getSlotsForFace(Direction.DOWN), new int[] {RecyclerBlockEntity.OUTPUT, RecyclerBlockEntity.FUEL}),
                "hopper faces like a furnace");
        for (ItemStack refused : List.of(new ItemStack(Items.KELP), new ItemStack(Items.BAMBOO), new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.IRON_SWORD), new ItemStack(Items.GOLDEN_SWORD),
                new ItemStack(Items.IRON_HORSE_ARMOR), new ItemStack(Items.CHAINMAIL_HELMET), new ItemStack(Items.COPPER_PICKAXE),
                new ItemStack(Items.ENCHANTED_BOOK), new ItemStack(Taler.COPPER_TALER.item()), new ItemStack(ModItems.FIRE_EXTINGUISHER.get()),
                new ItemStack(ModItems.SCRAP.get()))) {
            helper.assertFalse(recycler.canPlaceItemThroughFace(RecyclerBlockEntity.INPUT, refused, Direction.UP), "hopper must not insert " + refused);
        }
        for (ItemStack accepted : List.of(new ItemStack(Items.STONE_SWORD), new ItemStack(Items.LEATHER_BOOTS), new ItemStack(Items.SADDLE),
                new ItemStack(Items.SHEARS), new ItemStack(Items.SHIELD))) {
            helper.assertTrue(recycler.canPlaceItemThroughFace(RecyclerBlockEntity.INPUT, accepted, Direction.UP), "hopper must insert " + accepted);
            helper.assertFalse(recycler.canPlaceItemThroughFace(RecyclerBlockEntity.FUEL, accepted, Direction.NORTH), "no gear as fuel: " + accepted);
            helper.assertFalse(recycler.canPlaceItem(RecyclerBlockEntity.OUTPUT, accepted), "nothing goes into the output");
        }
        for (ItemStack fuel : List.of(new ItemStack(Items.COAL), new ItemStack(Items.WOODEN_SWORD), new ItemStack(Items.LAVA_BUCKET))) {
            helper.assertTrue(recycler.canPlaceItemThroughFace(RecyclerBlockEntity.FUEL, fuel, Direction.NORTH), "fuel from the side: " + fuel);
        }
        helper.assertTrue(recycler.canTakeItemThroughFace(RecyclerBlockEntity.FUEL, new ItemStack(Items.BUCKET), Direction.DOWN), "empty bucket leaves");
        helper.assertFalse(recycler.canTakeItemThroughFace(RecyclerBlockEntity.FUEL, new ItemStack(Items.COAL), Direction.DOWN), "coal stays");
        cleanup(helper, rel);
        helper.succeed();
    }

    /** Fenster: Shift-Klick legt Kohle ins Brennstofffach und Stiefel in die Eingabe; ins Schrottfach geht nichts. */
    private static void recyclerMenu(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, ModBlocks.RECYCLER.get());
        RecyclerBlockEntity recycler = helper.getBlockEntity(rel, RecyclerBlockEntity.class);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, new ItemStack(Items.COAL, 5));
        player.getInventory().setItem(1, new ItemStack(Items.LEATHER_BOOTS));
        RecyclerMenu menu = (RecyclerMenu) recycler.createMenu(1, player.getInventory(), player);
        helper.assertTrue(menu != null, "the recycler opens a menu");
        // Inventarslots: 3–29 Hauptinventar, 30–38 Schnellleiste; Schnellleiste 0 und 1 sind die Slots 30 und 31.
        menu.quickMoveStack(player, 30);
        menu.quickMoveStack(player, 31);
        helper.assertTrue(recycler.getItem(RecyclerBlockEntity.FUEL).is(Items.COAL) && recycler.getItem(RecyclerBlockEntity.FUEL).getCount() == 5,
                "shift-click puts coal into the fuel slot");
        helper.assertTrue(recycler.getItem(RecyclerBlockEntity.INPUT).is(Items.LEATHER_BOOTS), "shift-click puts boots into the input");
        helper.assertFalse(menu.getSlot(RecyclerBlockEntity.OUTPUT).mayPlace(new ItemStack(ModItems.SCRAP.get())), "the scrap slot takes nothing");
        cleanup(helper, rel);
        helper.succeed();
    }

    /** Ein Steinschwert (Wert 0,12) bringt im Mittel 0,12 Schrott: 1.000 Stück ergeben um 120. */
    private static void recyclerChance(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        RandomSource random = RandomSource.create(42L);
        int sum = 0;
        for (int i = 0; i < 1000; i++) {
            sum += Recycling.scrapFor(server, new ItemStack(Items.STONE_SWORD), random);
        }
        helper.assertTrue(sum >= 85 && sum <= 160, "stone swords average 0.12 scrap, got " + sum + " for 1000");
        ItemStack worn = new ItemStack(Items.LEATHER_CHESTPLATE);
        worn.setDamageValue(worn.getMaxDamage() / 2);
        int half = Recycling.scrapFor(server, worn, random);
        double halfValue = Recycling.entries().get(Items.LEATHER_CHESTPLATE) * Recycling.condition(worn);
        helper.assertTrue(half >= Math.floor(halfValue) && half <= Math.ceil(halfValue), "a half worn chestplate yields " + halfValue + ", got " + half);
        helper.succeed();
    }

    /**
     * Die Recyclingliste hält die Regeln aus docs/10-markt.md: nichts von der Handelsliste, nichts, was ein Ofen schmilzt,
     * Werte über 0 und höchstens 40, und die volle Reparatur kostet immer mehr, als der Schrott einbringt.
     */
    private static void recyclingData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(Recycling.entries().containsKey(Items.LEATHER_BOOTS), "recycling list loaded, got " + Recycling.entries().size());
        List<String> problems = new ArrayList<>();
        Recycling.entries().forEach((item, value) -> {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            if (TradeList.lookup(item).isPresent()) {
                problems.add(id + " is on the trade list");
            }
            if (value <= 0.0) {
                problems.add(id + " has value " + value);
            }
            ItemStack stack = new ItemStack(item);
            level.getServer().getRecipeManager().getRecipes().forEach(holder -> {
                if (holder.value() instanceof AbstractCookingRecipe cooking && cooking.input().test(stack)) {
                    problems.add(id + " smelts via " + holder.id().identifier());
                }
            });
            if (stack.isDamageableItem()) {
                ItemStack broken = stack.copy();
                broken.setDamageValue(broken.getMaxDamage() - 1);
                if (RepairStation.fullPrice(broken) <= value) {
                    problems.add(id + " repairs for " + RepairStation.fullPrice(broken) + " but recycles for " + value);
                }
            }
            if (!Recycling.accepts(level.getServer(), stack)) {
                problems.add(id + " is listed but refused");
            }
        });
        helper.assertTrue(problems.isEmpty(), "recycling list: " + problems);
        helper.succeed();
    }

    /** Schrott ist eine Ware: im Territorium höchstens der Ruhepreis (Basiswert × 1,41). */
    private static void scrapTradable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(TradeList.lookup(ModItems.SCRAP.get()).isPresent(), "scrap is on the trade list");
        withTerritory(helper, () -> {
            Market.Quote quote = Market.quote(level, helper.absolutePos(BlockPos.ZERO), ModItems.SCRAP.get(), 1).orElseThrow();
            double base = TradeList.lookup(ModItems.SCRAP.get()).orElseThrow().base();
            helper.assertTrue(quote.unitPrice() > 0.0 && quote.unitPrice() <= base * Market.restFactor() + 1e-6,
                    "scrap sells at most at its rest price, got " + quote.unitPrice());
        });
        helper.succeed();
    }

    /**
     * Am Block: Nur Taler starten eine Drehung, während der Drehung wird nichts angenommen, und nach dem letzten
     * Stopp liegt genau der Gewinn vor der Front.
     */
    private static void slotMachineSpin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos rel = new BlockPos(1, 1, 2);
        helper.setBlock(rel, ModBlocks.SLOT_MACHINE.get().defaultBlockState().setValue(SlotMachineBlock.FACING, Direction.NORTH));
        BlockPos pos = helper.absolutePos(rel);
        SlotMachineBlockEntity machine = helper.getBlockEntity(rel, SlotMachineBlockEntity.class);
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false);

        ItemStack diamonds = new ItemStack(Items.DIAMOND, 2);
        InteractionResult refused = level.getBlockState(pos).useItemOn(diamonds, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(refused == InteractionResult.TRY_WITH_EMPTY_HAND && diamonds.getCount() == 2 && !machine.spinning(),
                "other items start nothing and are not consumed");

        ItemStack coins = new ItemStack(Taler.COPPER_TALER.item(), 3);
        level.getBlockState(pos).useItemOn(coins, level, player, InteractionHand.MAIN_HAND, hit);
        helper.assertTrue(coins.getCount() == 2 && machine.spinning() && helper.getBlockState(rel).getValue(SlotMachineBlock.SPINNING),
                "a taler is inserted and the reels spin");
        // Direkt über die Blockentität: Am Block käme noch die Meldung dazu, und der Mock-Spieler hat keine Verbindung.
        helper.assertFalse(machine.insert(player, coins), "the machine refuses while spinning");
        helper.assertTrue(coins.getCount() == 2, "no second coin while spinning");

        // Ergebnis festlegen: Gewinn mit einem Silbertaler ergibt genau einen Silberbeutel.
        machine.start(level, player, Taler.SILVER_TALER, SlotMachine.Outcome.WIN, new int[] {2, 2, 2});
        AABB around = new AABB(pos).inflate(3);
        helper.runAfterDelay(SlotMachine.DURATION - 5, () -> {
            helper.assertTrue(machine.spinning() && level.getEntitiesOfClass(ItemEntity.class, around).isEmpty(),
                    "nothing is paid before the last reel stops");
            helper.runAfterDelay(8, () -> {
                List<ItemEntity> drops = level.getEntitiesOfClass(ItemEntity.class, around);
                long paid = drops.stream().mapToLong(e -> Taler.valueOf(e.getItem())).sum();
                boolean inFront = drops.stream().allMatch(e -> e.getZ() < pos.getZ() + 0.5);
                drops.forEach(ItemEntity::discard);
                helper.assertFalse(machine.spinning() || helper.getBlockState(rel).getValue(SlotMachineBlock.SPINNING),
                        "the machine is free again");
                helper.assertTrue(paid == Taler.SILVER_POUCH.value, "the win pays one silver pouch, got " + paid);
                helper.assertTrue(inFront, "the payout drops out of the front");
                helper.assertTrue(Arrays.equals(machine.symbols(), new int[] {2, 2, 2}), "the reels keep showing the result");
                helper.succeed();
            });
        });
    }

    /**
     * Chunkloader: Eine Ladung kostet genau den Preis aus einer Einzahlkiste am Rand der Reichweite, bei Unterdeckung
     * wird nichts entnommen, über die Obergrenze hinaus nicht geladen. Tickets gibt es nur mit Spielern online; ohne
     * ruht die Laufzeit. Nach Ablauf und nach dem Abbau sind Tickets und Eintrag weg.
     */
    private static void chunkLoader(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos loaderRel = new BlockPos(1, 1, 0);
        BlockPos depositRel = loaderRel.east(DtpConfig.CHUNKLOADER_DEPOSIT_RADIUS.get());
        BlockPos loader = helper.absolutePos(loaderRel);
        GlobalPos key = GlobalPos.of(level.dimension(), loader);
        ChunkLoaderData data = ChunkLoaderData.get(level.getServer());
        long price = DtpConfig.CHUNKLOADER_PRICE.get();
        long duration = DtpConfig.CHUNKLOADER_DURATION_TICKS.get();
        helper.setBlock(loaderRel, ModBlocks.CHUNK_LOADER.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        try {
            // Unterdeckung: nichts entnommen, nichts geladen.
            List<ItemStack> tooFew = Taler.split(price - 1);
            for (int i = 0; i < tooFew.size(); i++) {
                deposit.setItem(i, tooFew.get(i));
            }
            helper.assertTrue(ChunkLoaders.charge(level, loader, null) instanceof ChunkLoaders.Insufficient(long missing) && missing == 1,
                    "one taler short should be rejected");
            helper.assertTrue(deposit.balance() == price - 1 && data.remaining(key) == 0, "nothing taken, nothing loaded");

            // Genug Geld: genau der Preis wird entnommen, die Restlaufzeit ist eine Ladung.
            deposit.clearContent();
            List<ItemStack> coins = Taler.split(price + 100);
            for (int i = 0; i < coins.size(); i++) {
                deposit.setItem(i, coins.get(i));
            }
            helper.assertTrue(ChunkLoaders.charge(level, loader, null) instanceof ChunkLoaders.Charged, "charge should succeed");
            helper.assertTrue(deposit.balance() == 100, "exactly the price is taken, balance " + deposit.balance());
            helper.assertTrue(data.remaining(key) == duration, "one charge of time, got " + data.remaining(key));
            helper.assertTrue(helper.getBlockState(loaderRel).getValue(ChunkLoaderBlock.ACTIVE), "block shows the active state");

            // Mit Spielern online: Tickets gesetzt, Laufzeit zählt.
            ChunkLoaders.tick(level.getServer(), true);
            helper.assertTrue(ChunkLoaders.isTicketed(level, loader), "chunks are forced while someone is online");
            helper.assertTrue(ForcedChunkManager.hasForcedChunks(level), "the level keeps forced chunks");
            helper.assertTrue(data.remaining(key) == duration - ChunkLoaders.INTERVAL, "time counts while online");

            // Ohne Spieler: Tickets ruhen, die Laufzeit bleibt stehen.
            ChunkLoaders.tick(level.getServer(), false);
            helper.assertFalse(ChunkLoaders.isTicketed(level, loader), "no tickets while nobody is online");
            helper.assertTrue(data.remaining(key) == duration - ChunkLoaders.INTERVAL, "time stands still while offline");

            // Obergrenze: Über maxTicks hinaus wird nicht geladen, die Kiste bleibt unberührt.
            data.set(key, ChunkLoaders.maxTicks());
            for (int i = 0; i < coins.size(); i++) {
                deposit.setItem(i, coins.get(i));
            }
            long before = deposit.balance();
            helper.assertTrue(ChunkLoaders.charge(level, loader, null) instanceof ChunkLoaders.Full, "charging beyond the cap is rejected");
            helper.assertTrue(deposit.balance() == before, "nothing taken at the cap");

            // Ablauf: Eintrag und Tickets weg, der Block zeigt inaktiv.
            data.set(key, ChunkLoaders.INTERVAL);
            ChunkLoaders.tick(level.getServer(), true);
            helper.assertTrue(data.remaining(key) == 0 && !ChunkLoaders.isTicketed(level, loader), "expired loaders release their chunks");
            helper.assertFalse(helper.getBlockState(loaderRel).getValue(ChunkLoaderBlock.ACTIVE), "block shows the inactive state");

            // Abbau: Restlaufzeit verfällt, Tickets weg.
            data.set(key, duration);
            ChunkLoaders.tick(level.getServer(), true);
            helper.assertTrue(ChunkLoaders.isTicketed(level, loader), "ticketed again before removal");
            helper.setBlock(loaderRel, Blocks.AIR);
            helper.assertTrue(data.remaining(key) == 0 && !ChunkLoaders.isTicketed(level, loader), "removing the block drops time and tickets");
        } finally {
            ChunkLoaders.remove(level, loader);
            cleanup(helper, loaderRel, depositRel);
        }
        helper.succeed();
    }

    /**
     * Binden an die Waage: Die Uhr übernimmt eine Kopie des Filter-Items, eine leere Waage löst die Bindung. Der
     * Server liefert für gebundene Uhren im Inventar den Marktpreis, nicht handelbare Items gekennzeichnet.
     */
    private static void contractClockBinding(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack clock = new ItemStack(Items.CLOCK);
        ItemStack filter = new ItemStack(Items.WHEAT, 12);
        ContractClock.bind(clock, filter);
        ItemStack watched = ContractClock.watched(clock);
        helper.assertTrue(watched.is(Items.WHEAT) && watched.getCount() == 1, "the clock stores a single copy");
        helper.assertTrue(filter.getCount() == 12, "the scale's filter stays unchanged");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(0, clock);
        ItemStack swordClock = new ItemStack(Items.CLOCK);
        ContractClock.bind(swordClock, new ItemStack(Items.DIAMOND_SWORD));
        player.getInventory().setItem(1, swordClock);
        List<ContractClock.Price> prices = ContractClock.watchedPrices(level, player);
        helper.assertTrue(prices.size() == 2, "one price per bound item, got " + prices);
        ContractClock.Price wheat = prices.stream().filter(p -> p.item().equals(Identifier.withDefaultNamespace("wheat"))).findFirst().orElseThrow();
        double factor = Market.fullMarketFactor(level, Items.WHEAT).orElseThrow();
        helper.assertTrue(wheat.tradable() && Math.abs(wheat.factor() - factor) < 1e-9 && wheat.price() > 0, "wheat price: " + wheat);
        helper.assertTrue(prices.stream().anyMatch(p -> p.item().equals(Identifier.withDefaultNamespace("diamond_sword")) && !p.tradable()),
                "a diamond sword is not tradable");

        ContractClock.bind(clock, ItemStack.EMPTY);
        helper.assertTrue(ContractClock.watched(clock).isEmpty(), "an empty scale unbinds the clock");
        helper.succeed();
    }

    /** Maximale Gesundheit: +2 Lebenspunkte je Stufe, zurück auf den Grundwert ohne Upgrade. */
    private static void maxHealth(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        float base = player.getMaxHealth();
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.MAX_HEALTH);
        helper.assertTrue(def != null, "max health is configured");
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.MAX_HEALTH, 3);
        PlayerUpgradeEffects.refresh(player);
        helper.assertTrue(player.getMaxHealth() == base + def.value(3) * 2, "max health on level 3: " + player.getMaxHealth());
        PlayerUpgradeEffects.refresh(player);
        helper.assertTrue(player.getMaxHealth() == base + def.value(3) * 2, "refreshing twice does not stack");
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.MAX_HEALTH, 0);
        PlayerUpgradeEffects.refresh(player);
        helper.assertTrue(player.getMaxHealth() == base, "back to the base value");
        helper.succeed();
    }

    /** Kettenabbau sucht zusammenhängende Blöcke derselben Art, nächste zuerst, bis zur Grenze. */
    private static void chainCollect(GameTestHelper helper) {
        List<BlockPos> column = List.of(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1), new BlockPos(1, 3, 1), new BlockPos(1, 4, 1),
                new BlockPos(2, 5, 1));
        column.forEach(p -> helper.setBlock(p, Blocks.OAK_LOG));
        BlockPos birch = new BlockPos(0, 2, 1);
        helper.setBlock(birch, Blocks.BIRCH_LOG);
        ServerLevel level = helper.getLevel();
        BlockPos origin = helper.absolutePos(column.getFirst());

        List<BlockPos> all = ChainBreaker.collect(level, origin, Blocks.OAK_LOG, 10);
        helper.assertTrue(all.size() == 4, "four more oak logs incl. the diagonal one, got " + all.size());
        helper.assertFalse(all.contains(helper.absolutePos(birch)), "a different log type is not taken along");
        List<BlockPos> limited = ChainBreaker.collect(level, origin, Blocks.OAK_LOG, 2);
        helper.assertTrue(limited.size() == 2 && limited.contains(helper.absolutePos(column.get(1))), "limit respected, nearest first");

        column.forEach(p -> helper.setBlock(p, Blocks.AIR));
        helper.setBlock(birch, Blocks.AIR);
        helper.succeed();
    }

    /** Der Fortune-Bonus wirkt auf passende Werkzeuge, aber nie zusammen mit Silk Touch — sonst würden Blöcke vervielfacht. */
    private static void fortuneSilkTouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.FORTUNE_BONUS, 2);
        int expected = (int) PlayerUpgrades.value(player, PlayerUpgrade.FORTUNE_BONUS);
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> fortune = enchantments.getOrThrow(Enchantments.FORTUNE);

        ItemStack pickaxe = new ItemStack(Items.DIAMOND_PICKAXE);
        helper.assertTrue(PlayerUpgradeEffects.fortuneBonus(player, pickaxe, fortune) == expected, "bonus on a plain pickaxe");
        helper.assertTrue(PlayerUpgradeEffects.fortuneBonus(player, new ItemStack(Items.STICK), fortune) == 0, "no bonus for a stick");

        ItemStack silk = new ItemStack(Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
        helper.assertTrue(PlayerUpgradeEffects.fortuneBonus(player, silk, fortune) == 0, "no bonus together with silk touch");

        // Echte Loot-Auswertung ohne Silk Touch: Mit Fortune +2 bringen 100 Diamanterze deutlich mehr als 100 Diamanten
        // (Erwartung 150). Ohne Bonus wären es genau 100.
        BlockPos orePos = helper.absolutePos(new BlockPos(1, 1, 1));
        int diamonds = 0;
        for (int i = 0; i < 100; i++) {
            for (ItemStack drop : Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, orePos, null, player, pickaxe)) {
                diamonds += drop.is(Items.DIAMOND) ? drop.getCount() : 0;
            }
        }
        helper.assertTrue(diamonds > 115, "fortune bonus multiplies ore drops, got " + diamonds + " diamonds from 100 ores");

        // Echte Loot-Auswertung mit dem Spieler als Abbauer: Silk Touch liefert genau den einen Erzblock.
        BlockPos ore = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int i = 0; i < 20; i++) {
            List<ItemStack> drops = Block.getDrops(Blocks.DIAMOND_ORE.defaultBlockState(), level, ore, null, player, silk);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(Items.DIAMOND_ORE) && drops.getFirst().getCount() == 1,
                    "silk touch drops exactly one ore block, got " + drops);
        }
        helper.succeed();
    }

    /**
     * Neben-Beacon: registriert sich beim Setzen, hat einen Slot weniger als der Hauptbeacon (mindestens einen),
     * wählt nur freigeschaltete Effekte und wirkt nur in seinem Radius. Abbauen entfernt ihn wieder.
     */
    private static void relayBeacon(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        int oldNightVision = progress.level(ServerUpgrade.EFFECT_NIGHT_VISION);
        int oldJumpBoost = progress.level(ServerUpgrade.EFFECT_JUMP_BOOST);
        int oldSlots = progress.level(ServerUpgrade.EFFECT_SLOTS);
        BlockPos relayRel = new BlockPos(1, 1, 1);
        BlockPos relay = helper.absolutePos(relayRel);
        GlobalPos relayAt = GlobalPos.of(Level.OVERWORLD, relay);
        try {
            helper.setBlock(relayRel, ModBlocks.RELAY_BEACON.get());
            helper.assertTrue(progress.isRelay(relayAt), "a placed relay beacon is registered");

            progress.setLevel(ServerUpgrade.EFFECT_SLOTS, 0);
            int base = DtpConfig.BASE_EFFECT_SLOTS.get();
            helper.assertTrue(progress.relaySlots() == Math.max(1, base - 1), "one slot less than the main beacon, at least one");
            progress.setLevel(ServerUpgrade.EFFECT_NIGHT_VISION, 0);
            helper.assertFalse(progress.toggleRelayEffect(relayAt, ServerUpgrade.EFFECT_NIGHT_VISION), "locked effects cannot be chosen");
            progress.setLevel(ServerUpgrade.EFFECT_NIGHT_VISION, 1);
            progress.setLevel(ServerUpgrade.EFFECT_JUMP_BOOST, 1);
            helper.assertTrue(progress.toggleRelayEffect(relayAt, ServerUpgrade.EFFECT_NIGHT_VISION), "an unlocked effect can be chosen");
            if (progress.relaySlots() == 1) {
                helper.assertFalse(progress.toggleRelayEffect(relayAt, ServerUpgrade.EFFECT_JUMP_BOOST), "no second effect with one slot");
            }

            withTerritory(helper, () -> {
                // Der Hauptbeacon steht ebenfalls am Testort; ohne eigene aktive Effekte stammt Nachtsicht also vom Neben-Beacon.
                List<String> mainActive = progress.activeEffects().stream().map(u -> u.id).toList();
                mainActive.forEach(id -> progress.toggleEffect(ServerUpgrade.byId(id)));
                try {
                    helper.assertTrue(BeaconLogic.effectsAt(server, relay).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                            "the relay's effect works next to it");
                    int radius = DtpConfig.RELAY_RADIUS_CHUNKS.get() * 16;
                    BlockPos far = relay.offset(radius + 1, 0, 0);
                    helper.assertFalse(BeaconLogic.effectsAt(server, far).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                            "and not beyond its radius");
                    // Zylinder: unbegrenzt in der Höhe, aber rund — die Ecke des umschließenden Quadrats liegt außerhalb.
                    helper.assertTrue(BeaconLogic.effectsAt(server, relay.offset(0, -120, radius)).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                            "deep below, at the edge of the radius, the effect still works");
                    BlockPos corner = relay.offset(radius * 8 / 10, 0, radius * 8 / 10);
                    helper.assertFalse(BeaconLogic.effectsAt(server, corner).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                            "the reach is round, not square");
                    // Der Hauptbeacon wirkt fest im Gebiet der höchsten Grenzstufe — quadratisch, bis in die Ecken.
                    if (!progress.activeEffects().contains(ServerUpgrade.EFFECT_NIGHT_VISION)) {
                        int main = WorldProgress.mainEffectRadius();
                        BlockPos beacon = progress.beaconPos();
                        progress.setLevel(ServerUpgrade.EFFECT_SLOTS, Math.max(0, 1 - base));
                        progress.toggleEffect(ServerUpgrade.EFFECT_NIGHT_VISION);
                        helper.assertTrue(BeaconLogic.effectsAt(server, beacon.offset(main, -200, main)).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                                "the main beacon reaches the corner of the highest border stage, at any depth");
                        helper.assertFalse(BeaconLogic.effectsAt(server, beacon.offset(main + 1, 0, 0)).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                                "but not beyond it");
                        progress.toggleEffect(ServerUpgrade.EFFECT_NIGHT_VISION);
                        progress.setLevel(ServerUpgrade.EFFECT_SLOTS, 0);
                    }

                    // Ein Neben-Beacon wirkt unabhängig vom Territorium — auch jenseits der Grenze, solange sie steht.
                    BlockPos remotePos = progress.beaconPos().offset(WorldProgress.mainEffectRadius() + 1000, 0, 0);
                    GlobalPos remote = GlobalPos.of(Level.OVERWORLD, remotePos);
                    progress.addRelay(remote);
                    progress.toggleRelayEffect(remote, ServerUpgrade.EFFECT_NIGHT_VISION);
                    try {
                        helper.assertTrue(BeaconLogic.effectsAt(server, remotePos).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                                "outside the territory the relay works even while the border stands");
                        progress.setLevel(ServerUpgrade.BORDER, DtpConfig.RADIUS_CHUNKS.get().size());
                        helper.assertTrue(BeaconLogic.effectsAt(server, remotePos).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                                "and after the border is gone as well");
                    } finally {
                        progress.removeRelay(remote);
                        progress.setLevel(ServerUpgrade.BORDER, 0);
                    }

                    // Neben-Beacons wirken in jeder Dimension, dort ohne Grenze; der Hauptbeacon nur in der Oberwelt.
                    GlobalPos nether = GlobalPos.of(Level.NETHER, remotePos);
                    GlobalPos end = GlobalPos.of(Level.END, relay);
                    progress.addRelay(nether);
                    progress.addRelay(end);
                    progress.toggleRelayEffect(nether, ServerUpgrade.EFFECT_NIGHT_VISION);
                    try {
                        helper.assertTrue(BeaconLogic.effectsAt(server, Level.NETHER, remotePos).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                                "a relay in the nether works there, far beyond the overworld border");
                        helper.assertFalse(BeaconLogic.effectsAt(server, Level.OVERWORLD, remotePos).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                                "a nether relay does not reach the same coordinates in the overworld");
                        helper.assertTrue(BeaconLogic.effectsAt(server, Level.END, relay).isEmpty(),
                                "an end relay is active; without its own selection nothing comes from the main beacon");
                        helper.assertTrue(progress.relaySlots() >= 1, "relays in other dimensions share the slot count");
                        // Keine Sperre: Auch vor dem Start wirkt ein Neben-Beacon, der Hauptbeacon aber nicht.
                        BlockPos mainBeacon = progress.beaconPos();
                        progress.setBeacon(mainBeacon, false);
                        try {
                            helper.assertTrue(BeaconLogic.effectsAt(server, Level.NETHER, remotePos).contains(ServerUpgrade.EFFECT_NIGHT_VISION),
                                    "a relay works even before the run has started");
                        } finally {
                            progress.setBeacon(mainBeacon, true);
                        }
                    } finally {
                        progress.removeRelay(nether);
                        progress.removeRelay(end);
                    }

                    // Effekte lassen sich erst kaufen, wenn es einen Effektslot gibt.
                    if (base == 0) {
                        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
                        progress.setLevel(ServerUpgrade.EFFECT_SLOTS, 0);
                        BeaconLogic.BuyResult refused = BeaconLogic.buy(helper.getLevel(), progress.beaconPos(), player,
                                ServerUpgrade.EFFECT_RESISTANCE);
                        helper.assertTrue(refused instanceof BeaconLogic.EffectSlotMissing, "no effect without an effect slot: " + refused);
                    }
                } finally {
                    mainActive.forEach(id -> progress.toggleEffect(ServerUpgrade.byId(id)));
                }
            });

            helper.setBlock(relayRel, Blocks.AIR);
            helper.assertFalse(progress.isRelay(relayAt), "removing the block unregisters the relay beacon");
        } finally {
            progress.removeRelay(relayAt);
            progress.setLevel(ServerUpgrade.EFFECT_NIGHT_VISION, oldNightVision);
            progress.setLevel(ServerUpgrade.EFFECT_JUMP_BOOST, oldJumpBoost);
            progress.setLevel(ServerUpgrade.EFFECT_SLOTS, oldSlots);
        }
        helper.succeed();
    }

    /**
     * Beacon-Effekte für Tiere: Mit dem Stallmeister im Radius liegt die Pause der Eltern am Tick-Ende bei der
     * Config statt bei 6000, auch bei Fröschen; ohne ihn (oder nur mit Landwirtschaft) bleibt Vanilla. Der Stallmeister rückt die Fohlenwerte um den Bonus der
     * Lücke zum Maximum nach oben und überschreitet es nie. Eigene Effekte belegen Slots, verteilen aber nichts an Spieler.
     */
    private static void animalHusbandry(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        WorldProgress progress = WorldProgress.get(server);

        // Vererbung, deterministisch an einem Fohlen mit festen Grundwerten.
        AbstractHorse foal = helper.spawnWithNoFreeWill(EntityTypes.HORSE, new BlockPos(1, 2, 1));
        foal.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20.0);
        foal.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(0.6);
        foal.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.2);
        AnimalHusbandry.improveFoal(foal, 0.1);
        helper.assertTrue(Math.abs(foal.getAttributeBaseValue(Attributes.MAX_HEALTH) - 21.0) < 1e-6,
                "health rises by 10 % of the gap to 30: " + foal.getAttributeBaseValue(Attributes.MAX_HEALTH));
        helper.assertTrue(Math.abs(foal.getAttributeBaseValue(Attributes.JUMP_STRENGTH) - 0.64) < 1e-6,
                "jump rises by 10 % of the gap to 1.0: " + foal.getAttributeBaseValue(Attributes.JUMP_STRENGTH));
        helper.assertTrue(Math.abs(foal.getAttributeBaseValue(Attributes.MOVEMENT_SPEED) - (0.2 + 0.1 * (0.3375 - 0.2))) < 1e-6,
                "speed rises by 10 % of the gap to 0.3375: " + foal.getAttributeBaseValue(Attributes.MOVEMENT_SPEED));
        helper.assertTrue(foal.getHealth() == foal.getMaxHealth(), "the foal starts with full health");
        foal.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30.0);
        AnimalHusbandry.improveFoal(foal, 0.1);
        helper.assertTrue(foal.getAttributeBaseValue(Attributes.MAX_HEALTH) == 30.0, "the vanilla maximum is never exceeded");
        foal.discard();

        int oldAgriculture = progress.level(ServerUpgrade.EFFECT_AGRICULTURE);
        int oldStable = progress.level(ServerUpgrade.EFFECT_STABLEMASTER);
        int oldSlots = progress.level(ServerUpgrade.EFFECT_SLOTS);
        int cooldown = DtpConfig.BREEDING_COOLDOWN_TICKS.get();
        Animal[] plain = new Animal[2];
        Animal[] farming = new Animal[2];
        Animal[] boosted = new Animal[2];
        withTerritory(helper, () -> {
            List<String> mainActive = progress.activeEffects().stream().map(u -> u.id).toList();
            mainActive.forEach(id -> progress.toggleEffect(ServerUpgrade.byId(id)));
            try {
                progress.setLevel(ServerUpgrade.EFFECT_SLOTS, Math.max(0, 1 - DtpConfig.BASE_EFFECT_SLOTS.get()));
                progress.setLevel(ServerUpgrade.EFFECT_AGRICULTURE, 1);
                progress.setLevel(ServerUpgrade.EFFECT_STABLEMASTER, 1);
                helper.assertTrue(ServerUpgrade.EFFECT_STABLEMASTER.isEffect() && !ServerUpgrade.EFFECT_STABLEMASTER.isVanillaEffect(),
                        "an own beacon effect takes a slot but is no status effect");

                // Ohne aktiven Effekt: Vanilla-Pause.
                plain[0] = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 3));
                plain[1] = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 3));
                plain[0].spawnChildFromBreeding(level, plain[1]);

                // Landwirtschaft wirkt nur noch auf Pflanzen: die Pause bleibt Vanilla.
                helper.assertTrue(progress.toggleEffect(ServerUpgrade.EFFECT_AGRICULTURE), "farming can be chosen");
                farming[0] = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(3, 2, 3));
                farming[1] = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(4, 2, 3));
                farming[0].spawnChildFromBreeding(level, farming[1]);
                progress.toggleEffect(ServerUpgrade.EFFECT_AGRICULTURE);

                helper.assertTrue(progress.toggleEffect(ServerUpgrade.EFFECT_STABLEMASTER), "the stablemaster can be chosen");
                helper.assertTrue(BeaconLogic.effectsAt(server, helper.absolutePos(BlockPos.ZERO)).contains(ServerUpgrade.EFFECT_STABLEMASTER),
                        "the stablemaster works at the beacon");
                BeaconLogic.applyEffects(server); // darf mit einem eigenen Effekt nicht scheitern
                boosted[0] = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 2, 5));
                boosted[1] = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(2, 2, 5));
                boosted[0].spawnChildFromBreeding(level, boosted[1]);
                helper.assertTrue(boosted[0].getAge() == AnimalHusbandry.VANILLA_BREEDING_COOLDOWN,
                        "vanilla sets the pause after the event; it is shortened at the end of the tick");

                // Ei-Leger lösen kein BabyEntitySpawnEvent aus; der Tick-Haken kürzt ihre frisch gesetzte Pause.
                Frog frog = helper.spawnWithNoFreeWill(EntityTypes.FROG, new BlockPos(3, 2, 5));
                Frog mate = helper.spawnWithNoFreeWill(EntityTypes.FROG, new BlockPos(4, 2, 5));
                frog.spawnChildFromBreeding(level, mate);
                helper.assertTrue(frog.getAge() == AnimalHusbandry.VANILLA_BREEDING_COOLDOWN, "vanilla sets the frog's pause to 6000");
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(frog));
                NeoForge.EVENT_BUS.post(new EntityTickEvent.Post(mate));
                helper.assertTrue(frog.getAge() == cooldown && mate.getAge() == cooldown,
                        "in range frogs wait " + cooldown + " ticks too, got " + frog.getAge() + " / " + mate.getAge());
                progress.toggleEffect(ServerUpgrade.EFFECT_STABLEMASTER);
            } finally {
                progress.setLevel(ServerUpgrade.EFFECT_AGRICULTURE, oldAgriculture);
                progress.setLevel(ServerUpgrade.EFFECT_STABLEMASTER, oldStable);
                progress.setLevel(ServerUpgrade.EFFECT_SLOTS, oldSlots);
                mainActive.forEach(id -> progress.toggleEffect(ServerUpgrade.byId(id)));
            }
        });
        helper.runAfterDelay(2, () -> {
            for (Animal cow : boosted) {
                helper.assertTrue(cow.getAge() <= cooldown && cow.getAge() > cooldown - 10,
                        "in range the parents wait " + cooldown + " ticks, got " + cow.getAge());
            }
            for (Animal cow : plain) {
                helper.assertTrue(cow.getAge() > AnimalHusbandry.VANILLA_BREEDING_COOLDOWN - 10,
                        "without the effect the vanilla pause stays, got " + cow.getAge());
            }
            for (Animal cow : farming) {
                helper.assertTrue(cow.getAge() > AnimalHusbandry.VANILLA_BREEDING_COOLDOWN - 10,
                        "farming alone keeps the vanilla pause, got " + cow.getAge());
            }
            helper.killAllEntities();
            helper.succeed();
        });
    }

    /**
     * Imkerei: Honigstufe und Ernte-Bonus (mit fester Chance über die öffentlichen Helfer), Wachstum wie Vanillas
     * Bestäubung und Bienen, die im Radius keinen Spieler ins Visier nehmen — außerhalb des Effekts schon. Beim Ernten
     * ohne Lagerfeuer bleiben die Bienen im Stock; die Nestfüllung zählt nur mit einer Biene aus dem Stock.
     */
    private static void beekeeping(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(ServerUpgrade.EFFECT_BEEKEEPING.isEffect() && !ServerUpgrade.EFFECT_BEEKEEPING.isVanillaEffect(),
                "beekeeping is an own beacon effect");
        helper.assertTrue(UpgradeCosts.get(ServerUpgrade.EFFECT_BEEKEEPING) != null, "beekeeping has costs in the datapack");
        BlockPos hiveRel = new BlockPos(1, 2, 1);
        BlockPos hive = helper.absolutePos(hiveRel);
        BlockPos cropRel = new BlockPos(3, 2, 3);
        BlockPos crop = helper.absolutePos(cropRel);
        Bee bee = helper.spawnWithNoFreeWill(EntityTypes.BEE, new BlockPos(1, 3, 3));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);

        withBeaconEffects(helper, List.of(ServerUpgrade.EFFECT_BEEKEEPING), () -> {
            // Nestfüllung: eine Stufe mehr, nie über 5, ohne Chance nichts.
            level.setBlockAndUpdate(hive, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 2));
            Beekeeping.raiseHoney(level, hive, 0);
            helper.assertTrue(level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL) == 2, "no chance, no extra honey");
            Beekeeping.raiseHoney(level, hive, 100);
            helper.assertTrue(level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL) == 3, "the hive gains one honey level");
            level.setBlockAndUpdate(hive, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
            Beekeeping.raiseHoney(level, hive, 100);
            helper.assertTrue(level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL) == BeehiveBlock.MAX_HONEY_LEVELS,
                    "a full hive stays at the maximum");

            // Ernte-Bonus nur nach einer Ernte (Stufe 0).
            Beekeeping.harvestBonus(level, hive, true, 100);
            helper.assertItemEntityNotPresent(Items.HONEYCOMB, hiveRel, 2.0);
            level.setBlockAndUpdate(hive, Blocks.BEEHIVE.defaultBlockState());
            Beekeeping.harvestBonus(level, hive, true, 100);
            helper.assertItemEntityPresent(Items.HONEYCOMB, hiveRel, 2.0);
            Beekeeping.harvestBonus(level, hive, false, 100);
            helper.assertItemEntityPresent(Items.HONEY_BOTTLE, hiveRel, 2.0);

            // Bestäubung: eine Stufe wie bei Vanilla, nur Pflanzen aus dem Bienen-Tag.
            level.setBlockAndUpdate(crop.below(), Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
            helper.assertTrue(Beekeeping.grow(level, crop, level.getRandom()), "wheat grows");
            helper.assertTrue(((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(crop)) == 1, "exactly one stage");
            helper.assertFalse(Beekeeping.grow(level, crop.below(), level.getRandom()), "farmland is no bee growable");
            // Kannenpflanze: Stufe für Stufe, ab Stufe 3 mit oberer Hälfte, die auch von oben aus wächst.
            level.setBlockAndUpdate(crop, Blocks.PITCHER_CROP.defaultBlockState());
            for (int age = 1; age <= 3; age++) {
                helper.assertTrue(Beekeeping.grow(level, crop, level.getRandom()), "pitcher crop grows to stage " + age);
                helper.assertTrue(level.getBlockState(crop).getValue(PitcherCropBlock.AGE) == age, "pitcher crop at stage " + age);
            }
            helper.assertTrue(level.getBlockState(crop.above()).is(Blocks.PITCHER_CROP), "stage 3 adds the upper half");
            helper.assertTrue(Beekeeping.grow(level, crop.above(), level.getRandom()), "the upper half grows the plant");
            helper.assertTrue(level.getBlockState(crop).getValue(PitcherCropBlock.AGE) == 4, "pitcher crop ripe");
            helper.assertFalse(Beekeeping.grow(level, crop, level.getRandom()), "a ripe pitcher crop stops");
            level.setBlockAndUpdate(crop.above(), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(crop, Blocks.AIR.defaultBlockState());

            // Friedlich im Radius.
            bee.setTarget(player);
            helper.assertTrue(bee.getTarget() == null, "a bee in range does not target players");

            // Ernte ohne Lagerfeuer: Die Bienen bleiben im Stock, samt Brutzeit — kein Auftauchen für einen Tick.
            Beekeeping.flushPending(level.getServer());
            level.setBlockAndUpdate(hive, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, BeehiveBlock.MAX_HONEY_LEVELS));
            BeehiveBlockEntity hiveEntity = (BeehiveBlockEntity) level.getBlockEntity(hive);
            for (int i = 0; i < 2; i++) {
                Bee stored = EntityTypes.BEE.create(level, EntitySpawnReason.LOAD);
                hiveEntity.storeBee(new BeehiveBlockEntity.Occupant(BeehiveBlockEntity.Occupant.of(stored).entityData(), 1000, 2400));
                stored.discard();
            }
            ServerPlayer harvester = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
            harvester.snapTo(hive.getX() + 0.5, hive.getY() + 1.0, hive.getZ() + 2.5, 0f, 0f);
            ItemStack shears = new ItemStack(Items.SHEARS);
            harvester.setItemInHand(InteractionHand.MAIN_HAND, shears);
            harvester.gameMode.useItemOn(harvester, level, shears, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(hive), Direction.UP, hive, false));
            helper.assertTrue(level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL) == 0, "the hive is harvested");
            var occupants = hiveEntity.collectComponents().get(DataComponents.BEES).bees();
            helper.assertTrue(occupants.size() == 2 && hiveEntity.getOccupantCount() == 2,
                    "in range the bees stay inside when harvesting, got " + occupants.size());
            helper.assertTrue(occupants.stream().allMatch(o -> o.ticksInHive() == 1000), "the bees keep their time in the hive");

            // Nestfüllung nur mit einer Biene aus diesem Stock im selben Tick — ein bloßes Block-Update zählt nicht.
            Beekeeping.flushPending(level.getServer());
            int oldChance = DtpConfig.BEE_HONEY_CHANCE.get();
            try {
                DtpConfig.BEE_HONEY_CHANCE.set(100);
                level.setBlockAndUpdate(hive, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 1));
                Beekeeping.flushPending(level.getServer());
                helper.assertTrue(level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL) == 1,
                        "a block update without a bee does not count as a delivery");
                Bee delivered = EntityTypes.BEE.create(level, EntitySpawnReason.LOAD);
                delivered.setHivePos(hive);
                delivered.snapTo(hive.getX() + 0.5, hive.getY() + 0.5, hive.getZ() - 0.6);
                level.setBlockAndUpdate(hive, Blocks.BEEHIVE.defaultBlockState().setValue(BeehiveBlock.HONEY_LEVEL, 2));
                level.addFreshEntity(delivered);
                Beekeeping.flushPending(level.getServer());
                helper.assertTrue(level.getBlockState(hive).getValue(BeehiveBlock.HONEY_LEVEL) == 3,
                        "a delivery with a bee leaving this hive gains one level");
            } finally {
                DtpConfig.BEE_HONEY_CHANCE.set(oldChance);
            }
        });
        bee.setTarget(player);
        helper.assertTrue(bee.getTarget() == player, "without the effect bees target players as usual");
        helper.killAllEntities();
        helper.succeed();
    }

    /**
     * Landwirtschaft: Ackerboden mit Weizen übersteht im Radius das Trampeln einer Kuh, ohne den Effekt wird er zu
     * Erde; die Zusatzstufe des Pflanzenwachstums wächst genau eine Stufe, nie über die Reife und nur an Feldfrüchten.
     * Komposter im Radius füllen sich mit {@code beacon.compostBonus} Prozentpunkten mehr Chance.
     */
    private static void farming(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(UpgradeCosts.get(ServerUpgrade.EFFECT_AGRICULTURE) != null, "farming has costs in the datapack");
        BlockPos soil = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos crop = soil.above();
        BlockPos stem = helper.absolutePos(new BlockPos(3, 2, 1));
        Animal cow = helper.spawnWithNoFreeWill(EntityTypes.COW, new BlockPos(1, 3, 1));
        Runnable plant = () -> {
            level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
        };
        // Bei dieser Fallhöhe gelingt Vanillas Trampel-Wurf immer.
        Runnable trample = () -> Blocks.FARMLAND.fallOn(level, level.getBlockState(soil), soil, cow, 10.0);
        // Simuliert so oft einen echten Wachstumsschritt (Stufe 0 → 1 und das Post-Event, wie in CropBlock.randomTick)
        // und zählt, wie oft der Effekt eine Zusatzstufe obendrauf gibt.
        // Kompost über den echten Vanilla-Weg (ComposterBlock.insertItem → addLayer → überschriebenes compostable/low):
        // je Versuch Stufe 1 → Treffer, wenn Stufe 2 herauskommt. Weizensamen haben in Vanilla 30 %.
        BlockPos composter = helper.absolutePos(new BlockPos(1, 2, 3));
        int compostTrials = 2000;
        java.util.function.IntSupplier compostHits = () -> {
            int hits = 0;
            for (int i = 0; i < compostTrials; i++) {
                BlockState start = Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, 1);
                level.setBlockAndUpdate(composter, start);
                BlockState after = ComposterBlock.insertItem(null, start, level, new ItemStack(Items.WHEAT_SEEDS), composter);
                if (after.getValue(ComposterBlock.LEVEL) == 2) {
                    hits++;
                }
            }
            return hits;
        };
        java.util.function.IntConsumer assertCompost = chance -> {
            int hits = compostHits.getAsInt();
            double expected = compostTrials * chance / 100.0;
            double tolerance = 6 * Math.sqrt(compostTrials * chance / 100.0 * (1 - chance / 100.0)) + 1;
            helper.assertTrue(Math.abs(hits - expected) <= tolerance,
                    "composting at " + chance + " %: " + hits + " hits in " + compostTrials + ", expected ~" + expected);
        };
        CropBlock wheatBlock = (CropBlock) Blocks.WHEAT;
        int growthTrials = 1000;
        java.util.function.IntSupplier bonusGrowths = () -> {
            int bonus = 0;
            for (int i = 0; i < growthTrials; i++) {
                BlockState before = wheatBlock.getStateForAge(0);
                level.setBlockAndUpdate(crop, wheatBlock.getStateForAge(1));
                CommonHooks.fireCropGrowPost(level, crop, before);
                int age = wheatBlock.getAge(level.getBlockState(crop));
                helper.assertTrue(age == 1 || age == 2, "at most one bonus stage per growth step");
                if (age == 2) {
                    bonus++;
                }
            }
            return bonus;
        };

        withBeaconEffects(helper, List.of(ServerUpgrade.EFFECT_AGRICULTURE), () -> {
            plant.run();
            trample.run();
            helper.assertTrue(level.getBlockState(soil).is(Blocks.FARMLAND), "farmland in range is not trampled");
            helper.assertTrue(level.getBlockState(crop).is(Blocks.WHEAT), "the crop on it survives");

            CropBlock wheat = (CropBlock) Blocks.WHEAT;
            level.setBlockAndUpdate(crop, wheat.getStateForAge(2));
            helper.assertTrue(Farming.bonusStage(level, crop), "wheat gains a bonus stage");
            helper.assertTrue(wheat.getAge(level.getBlockState(crop)) == 3, "exactly one stage");
            level.setBlockAndUpdate(crop, wheat.getStateForAge(wheat.getMaxAge()));
            helper.assertFalse(Farming.bonusStage(level, crop), "ripe wheat does not grow further");

            level.setBlockAndUpdate(stem.below(), Blocks.FARMLAND.defaultBlockState());
            level.setBlockAndUpdate(stem, Blocks.PUMPKIN_STEM.defaultBlockState().setValue(StemBlock.AGE, 6));
            helper.assertTrue(Farming.bonusStage(level, stem), "a stem gains a bonus stage");
            helper.assertTrue(level.getBlockState(stem).getValue(StemBlock.AGE) == StemBlock.MAX_AGE, "the stem is fully grown");
            helper.assertFalse(Farming.bonusStage(level, stem), "a fully grown stem does not set fruit early");
            helper.assertFalse(Farming.bonusStage(level, soil), "farmland is no crop");

            level.setBlockAndUpdate(stem.below(), Blocks.SOUL_SAND.defaultBlockState());
            level.setBlockAndUpdate(stem, Blocks.NETHER_WART.defaultBlockState().setValue(NetherWartBlock.AGE, 2));
            helper.assertTrue(Farming.bonusStage(level, stem), "nether wart gains a bonus stage");
            helper.assertTrue(level.getBlockState(stem).getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE, "the wart is ripe");
            helper.assertFalse(Farming.bonusStage(level, stem), "ripe nether wart does not grow further");

            // Über den Event-Hook: im Radius wächst Weizen nach etwa cropGrowthBonus Prozent der Schritte eine Stufe mehr.
            plant.run();
            int chance = DtpConfig.CROP_GROWTH_BONUS.get();
            int bonus = bonusGrowths.getAsInt();
            double expected = growthTrials * chance / 100.0;
            double tolerance = 6 * Math.sqrt(growthTrials * chance / 100.0 * (1 - chance / 100.0)) + 1;
            helper.assertTrue(Math.abs(bonus - expected) <= tolerance,
                    "crops in range grow faster: " + bonus + " bonus stages in " + growthTrials + " steps, expected ~" + expected);
            helper.assertTrue(chance == 0 || bonus > 0, "the growth bonus fires at all");

            // Kompost: additiv, Samen 30 % → 30 + compostBonus %. Ein leerer Komposter füllt sich weiter immer.
            assertCompost.accept(30 + DtpConfig.COMPOST_BONUS.get());
            BlockState empty = Blocks.COMPOSTER.defaultBlockState();
            level.setBlockAndUpdate(composter, empty);
            helper.assertTrue(ComposterBlock.insertItem(null, empty, level, new ItemStack(Items.WHEAT_SEEDS), composter)
                    .getValue(ComposterBlock.LEVEL) == 1, "the first item always fills an empty composter");
        });

        // Ohne den Effekt bleibt es bei Vanillas 30 %.
        assertCompost.accept(30);

        // Ohne den Effekt bleibt es beim Vanilla-Tempo: keine einzige Zusatzstufe.
        plant.run();
        helper.assertTrue(bonusGrowths.getAsInt() == 0, "without the effect crops grow at vanilla speed");

        // Ohne den Effekt tritt die Kuh den Acker wie gewohnt zu Erde.
        plant.run();
        trample.run();
        helper.assertTrue(level.getBlockState(soil).is(Blocks.DIRT), "without the effect farmland turns to dirt");

        // Version 3 → 4: effect_breeding heißt jetzt effect_agriculture — gekauft, aktiv und am Neben-Beacon.
        CompoundTag v3 = new CompoundTag();
        v3.putInt(Versioned.KEY, 3);
        CompoundTag upgrades = new CompoundTag();
        upgrades.putInt("effect_breeding", 1);
        upgrades.putInt("effect_slots", 1);
        v3.put("upgrades", upgrades);
        ListTag active = new ListTag();
        active.add(StringTag.valueOf("effect_breeding"));
        v3.put("active_effects", active);
        CompoundTag relay = new CompoundTag();
        relay.putIntArray("pos", new int[] {10, 64, 10});
        relay.put("effects", active.copy());
        ListTag relays = new ListTag();
        relays.add(relay);
        v3.put("relays", relays);
        WorldProgress migrated = WorldProgress.CODEC.parse(NbtOps.INSTANCE, v3).getOrThrow();
        helper.assertTrue(migrated.level(ServerUpgrade.EFFECT_AGRICULTURE) == 1, "v4: the bought effect keeps its level");
        helper.assertTrue(migrated.activeEffects().contains(ServerUpgrade.EFFECT_AGRICULTURE), "v4: the effect stays active");
        helper.assertTrue(migrated.relayEffects(GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10)))
                .contains(ServerUpgrade.EFFECT_AGRICULTURE), "v4: relay beacons keep the effect");
        Tag saved = WorldProgress.CODEC.encodeStart(NbtOps.INSTANCE, migrated).getOrThrow();
        helper.assertTrue(((CompoundTag) saved).getIntOr(Versioned.KEY, 0) == 5, "saved with version 5");
        helper.assertFalse(((CompoundTag) saved).getCompoundOrEmpty("upgrades").contains("effect_breeding"), "the old id is gone");

        // Version 4 → 5: Pferdezucht heißt Stallmeister und schluckt den Straßenbau, der Baumeister den Rückbau.
        CompoundTag v4 = new CompoundTag();
        v4.putInt(Versioned.KEY, 4);
        CompoundTag v4Upgrades = new CompoundTag();
        v4Upgrades.putInt("effect_horse_breeding", 1);
        v4Upgrades.putInt("effect_roads", 1);
        v4Upgrades.putInt("effect_rebuild", 1);
        v4Upgrades.putInt("effect_builder", 1);
        v4Upgrades.putInt("effect_slots", 3);
        v4.put("upgrades", v4Upgrades);
        ListTag v4Active = new ListTag();
        v4Active.add(StringTag.valueOf("effect_roads"));
        v4Active.add(StringTag.valueOf("effect_horse_breeding"));
        v4Active.add(StringTag.valueOf("effect_rebuild"));
        v4.put("active_effects", v4Active);
        CompoundTag v4Relay = new CompoundTag();
        v4Relay.putIntArray("pos", new int[] {10, 64, 10});
        ListTag v4RelayEffects = new ListTag();
        v4RelayEffects.add(StringTag.valueOf("effect_builder"));
        v4RelayEffects.add(StringTag.valueOf("effect_rebuild"));
        v4Relay.put("effects", v4RelayEffects);
        ListTag v4Relays = new ListTag();
        v4Relays.add(v4Relay);
        v4.put("relays", v4Relays);
        WorldProgress v5 = WorldProgress.CODEC.parse(NbtOps.INSTANCE, v4).getOrThrow();
        helper.assertTrue(v5.level(ServerUpgrade.EFFECT_STABLEMASTER) == 1, "v5: horse breeding became the stablemaster");
        helper.assertTrue(v5.level(ServerUpgrade.EFFECT_BUILDER) == 1, "v5: the master builder stays bought");
        helper.assertTrue(v5.activeEffects().equals(List.of(ServerUpgrade.EFFECT_STABLEMASTER, ServerUpgrade.EFFECT_BUILDER)),
                "v5: roads and horse breeding share one slot, undo building sits in the builder's: " + v5.activeEffects());
        helper.assertTrue(v5.relayEffects(GlobalPos.of(Level.OVERWORLD, new BlockPos(10, 64, 10))).equals(List.of(ServerUpgrade.EFFECT_BUILDER)),
                "v5: relay beacons keep the builder once");
        CompoundTag v5Upgrades = ((CompoundTag) WorldProgress.CODEC.encodeStart(NbtOps.INSTANCE, v5).getOrThrow()).getCompoundOrEmpty("upgrades");
        for (String old : List.of("effect_horse_breeding", "effect_roads", "effect_rebuild")) {
            helper.assertFalse(v5Upgrades.contains(old), "v5: the old id " + old + " is gone");
        }

        // Wer nur einen aufgegangenen Teil besaß, bekommt den zusammengelegten Effekt.
        CompoundTag roadsOnly = new CompoundTag();
        roadsOnly.putInt(Versioned.KEY, 4);
        CompoundTag roadsUpgrades = new CompoundTag();
        roadsUpgrades.putInt("effect_roads", 1);
        roadsUpgrades.putInt("effect_rebuild", 1);
        roadsOnly.put("upgrades", roadsUpgrades);
        WorldProgress fromParts = WorldProgress.CODEC.parse(NbtOps.INSTANCE, roadsOnly).getOrThrow();
        helper.assertTrue(fromParts.level(ServerUpgrade.EFFECT_STABLEMASTER) == 1, "v5: road building alone grants the stablemaster");
        helper.assertTrue(fromParts.level(ServerUpgrade.EFFECT_BUILDER) == 1, "v5: undo building alone grants the master builder");
        helper.killAllEntities();
        helper.succeed();
    }

    /**
     * Serverweite Beacon-Upgrades: Endermänner und Creeper greifen nicht mehr in Blöcke ein (andere Mobs schon),
     * Ofentempo wirkt über den datengetriebenen Brennstoff-Wert auf Ofen und Hochofen, nur im Beacon-Gebiet, die
     * Mending-Aura repariert das ganze Inventar im Beacon-Gebiet (Hauptbeacon und Neben-Beacons) und der Welt-Spawn
     * liegt nach der Bestätigung am Beacon.
     */
    private static void serverRules(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        WorldProgress progress = WorldProgress.get(server);
        int oldEnder = progress.level(ServerUpgrade.ENDERMAN_GRIEFING);
        int oldCreeper = progress.level(ServerUpgrade.CREEPER_GRIEFING);
        int oldFurnace = progress.level(ServerUpgrade.FURNACE_SPEED);
        int oldAura = progress.level(ServerUpgrade.MENDING_AURA);
        LevelData.RespawnData oldSpawn = server.getRespawnData();
        boolean griefing = level.getGameRules().get(GameRules.MOB_GRIEFING);

        LivingEntity enderman = helper.spawnWithNoFreeWill(EntityTypes.ENDERMAN, new BlockPos(1, 2, 1));
        LivingEntity creeper = helper.spawnWithNoFreeWill(EntityTypes.CREEPER, new BlockPos(3, 2, 1));
        LivingEntity zombie = helper.spawnWithNoFreeWill(EntityTypes.ZOMBIE, new BlockPos(5, 2, 1));
        try {
            progress.setLevel(ServerUpgrade.ENDERMAN_GRIEFING, 0);
            progress.setLevel(ServerUpgrade.CREEPER_GRIEFING, 0);
            helper.assertTrue(EventHooks.canEntityGrief(level, enderman) == griefing, "without the upgrade endermen follow the gamerule");
            helper.assertTrue(EventHooks.canEntityGrief(level, creeper) == griefing, "without the upgrade creepers follow the gamerule");
            progress.setLevel(ServerUpgrade.ENDERMAN_GRIEFING, 1);
            helper.assertFalse(EventHooks.canEntityGrief(level, enderman), "endermen no longer pick up blocks");
            helper.assertTrue(EventHooks.canEntityGrief(level, creeper) == griefing, "the enderman upgrade leaves creepers alone");
            progress.setLevel(ServerUpgrade.CREEPER_GRIEFING, 1);
            helper.assertFalse(EventHooks.canEntityGrief(level, creeper), "creepers no longer destroy blocks");
            helper.assertTrue(EventHooks.canEntityGrief(level, zombie) == griefing, "other mobs follow the gamerule");
            // Echte Explosion: Der Stein neben dem Creeper bleibt stehen.
            BlockPos stone = new BlockPos(3, 1, 2);
            helper.setBlock(stone, Blocks.STONE);
            Vec3 at = creeper.position();
            level.explode(creeper, at.x, at.y, at.z, 3.0F, Level.ExplosionInteraction.MOB);
            helper.assertBlockPresent(Blocks.STONE, stone);

            // Mending-Aura: ganzes Inventar, Stufe II mehr je Takt.
            ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
            pick.setDamageValue(100);
            Player holder = helper.makeMockPlayer(GameType.SURVIVAL);
            holder.getInventory().setItem(20, pick);
            BeaconLogic.mend(holder, BeaconLogic.mendingAuraRepair(1));
            int repaired = 100 - holder.getInventory().getItem(20).getDamageValue();
            helper.assertTrue(repaired == DtpConfig.MENDING_AURA_REPAIR.get(),
                    "the aura repairs items anywhere in the inventory, got " + repaired);
            helper.assertTrue(BeaconLogic.mendingAuraRepair(2) > BeaconLogic.mendingAuraRepair(1), "level II repairs more");

            // Beacon-Gebiet (Mending-Aura, Ofentempo): Effektgebiet des Hauptbeacons und Radius der Neben-Beacons.
            withTerritory(helper, () -> {
                BlockPos beacon = progress.beaconPos();
                int main = WorldProgress.mainEffectRadius();
                helper.assertTrue(BeaconLogic.inBeaconArea(level, beacon.offset(main, -60, -main)),
                        "the main beacon covers its whole effect square, height ignored");
                BlockPos outside = beacon.offset(main + 1000, 0, 0);
                helper.assertFalse(BeaconLogic.inBeaconArea(level, outside), "outside the effect square nothing applies");
                helper.assertFalse(BeaconLogic.inBeaconArea(server, Level.NETHER, beacon), "the main beacon is overworld only");
                GlobalPos relay = GlobalPos.of(Level.OVERWORLD, outside);
                boolean hadRelay = progress.isRelay(relay);
                progress.addRelay(relay);
                try {
                    int relayRadius = DtpConfig.RELAY_RADIUS_CHUNKS.get() * 16;
                    helper.assertTrue(BeaconLogic.inBeaconArea(level, outside.offset(relayRadius - 1, 0, 0)),
                            "a relay beacon carries the area upgrades without a slot");
                    helper.assertFalse(BeaconLogic.inBeaconArea(level, outside.offset(relayRadius + 1, 0, 0)),
                            "beyond the relay radius nothing applies");
                } finally {
                    if (!hadRelay) {
                        progress.removeRelay(relay);
                    }
                }
            });

            // Spawnpunkt am Beacon.
            withTerritory(helper, () -> {
                BeaconLogic.setSpawnToBeacon(server);
                helper.assertTrue(server.getRespawnData().pos().equals(progress.beaconPos().above()),
                        "the world spawn sits on the beacon: " + server.getRespawnData().pos());
            });
        } finally {
            server.setRespawnData(oldSpawn);
            progress.setLevel(ServerUpgrade.ENDERMAN_GRIEFING, oldEnder);
            progress.setLevel(ServerUpgrade.CREEPER_GRIEFING, oldCreeper);
            progress.setLevel(ServerUpgrade.MENDING_AURA, oldAura);
            enderman.discard();
            creeper.discard();
            zombie.discard();
        }

        // Ofentempo Stufe 1 (Faktor aus speed_default.json, Start ×1,25): Ofen 200 → 200 / Faktor Ticks. Hochofen:
        // Rezept ebenfalls 200, Vanillas ×2 bleibt erhalten → 200 / (2 × Faktor). Der Bonus gilt nur im Beacon-Gebiet
        // und wird beim Anzünden gelesen: erst ein Ofen ohne Gebiet (kein Durchlauf, kein Neben-Beacon in Reichweite
        // vorausgesetzt), dann beide Öfen mit dem Beacon im Testgelände.
        double speed1 = furnaceSpeedFactor(level, 1);
        progress.setLevel(ServerUpgrade.FURNACE_SPEED, 1);
        BlockPos oldBeacon = progress.beaconPos();
        boolean oldStarted = progress.started();
        BlockPos furnacePos = new BlockPos(1, 1, 4);
        BlockPos blastPos = new BlockPos(3, 1, 4);
        Runnable restore = () -> {
            progress.setBeacon(oldBeacon, oldStarted);
            progress.setLevel(ServerUpgrade.FURNACE_SPEED, oldFurnace);
            cleanup(helper, furnacePos, blastPos);
        };
        progress.setBeacon(null, false);
        helper.setBlock(furnacePos, Blocks.FURNACE);
        fillFurnace(helper, furnacePos);
        helper.runAfterDelay(3, () -> {
            try {
                if (!BeaconLogic.inRelayRange(level, helper.absolutePos(furnacePos))) {
                    int plain = cookingTotalTime(helper, furnacePos);
                    helper.assertTrue(plain == 200, "outside the beacon area a furnace cooks at normal speed, got " + plain);
                }
                cleanup(helper, furnacePos);
                progress.setBeacon(helper.absolutePos(BlockPos.ZERO), true);
                helper.setBlock(furnacePos, Blocks.FURNACE);
                helper.setBlock(blastPos, Blocks.BLAST_FURNACE);
                fillFurnace(helper, furnacePos);
                fillFurnace(helper, blastPos);
            } catch (RuntimeException e) {
                restore.run();
                throw e;
            }
            helper.runAfterDelay(3, () -> {
                try {
                    int furnace = cookingTotalTime(helper, furnacePos);
                    int blast = cookingTotalTime(helper, blastPos);
                    helper.assertTrue(Math.abs(furnace - 200 / speed1) <= 1, "furnace speed level 1 cooks in 200 / " + speed1 + " ticks, got " + furnace);
                    helper.assertTrue(Math.abs(blast - 100 / speed1) <= 1, "a blast furnace keeps its x2 on top, got " + blast);
                } finally {
                    restore.run();
                }
                helper.succeed();
            });
        });
    }

    private static void fillFurnace(GameTestHelper helper, BlockPos rel) {
        if (helper.getLevel().getBlockEntity(helper.absolutePos(rel)) instanceof Container furnace) {
            furnace.setItem(0, new ItemStack(Items.RAW_IRON));
            furnace.setItem(1, new ItemStack(Items.COAL));
        }
    }

    private static int cookingTotalTime(GameTestHelper helper, BlockPos rel) {
        ServerLevel level = helper.getLevel();
        return level.getBlockEntity(helper.absolutePos(rel)).saveWithoutMetadata(level.registryAccess())
                .getIntOr("cooking_total_time", -1);
    }

    /** Ohne Freischaltung zündet ein Netherportal nicht; nach dem Kauf wie gewohnt. */
    private static void netherLock(GameTestHelper helper) {
        WorldProgress progress = WorldProgress.get(helper.getLevel().getServer());
        int old = progress.level(ServerUpgrade.NETHER_ACCESS);
        // Obsidianrahmen 4 × 5 entlang der X-Achse, Innenraum 2 × 3.
        List<BlockPos> frame = new ArrayList<>();
        for (int x = 0; x <= 3; x++) {
            for (int y = 1; y <= 5; y++) {
                if (x == 0 || x == 3 || y == 1 || y == 5) {
                    frame.add(new BlockPos(x, y, 1));
                }
            }
        }
        BlockPos inside = new BlockPos(1, 2, 1);
        try {
            frame.forEach(p -> helper.setBlock(p, Blocks.OBSIDIAN));
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, 0);
            helper.assertTrue(DimensionAccess.netherLocked(helper.getLevel().getServer()) == DtpConfig.NETHER_LOCK.get(),
                    "locked without the upgrade (if enabled)");
            helper.setBlock(inside, Blocks.FIRE);
            if (DtpConfig.NETHER_LOCK.get()) {
                helper.assertFalse(helper.getBlockState(inside).is(Blocks.NETHER_PORTAL), "a locked portal does not light");
            }

            helper.setBlock(inside, Blocks.AIR);
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, 1);
            helper.setBlock(inside, Blocks.FIRE);
            helper.assertTrue(helper.getBlockState(inside).is(Blocks.NETHER_PORTAL), "after unlocking the portal lights");
        } finally {
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, old);
            for (int x = 1; x <= 2; x++) {
                for (int y = 2; y <= 4; y++) {
                    helper.setBlock(new BlockPos(x, y, 1), Blocks.AIR);
                }
            }
            frame.forEach(p -> helper.setBlock(p, Blocks.AIR));
        }
        helper.succeed();
    }

    /**
     * Marktereignis: senkt den Marktdruck aller Waren oder nur einer Warengruppe, und Contracts bringen das
     * nächste Ereignis je nach Stufe näher.
     */
    private static void marketEvent(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        MarketData data = MarketData.get(server);
        Identifier wheat = Identifier.withDefaultNamespace("wheat");
        Identifier cobblestone = Identifier.withDefaultNamespace("cobblestone");
        Identifier potato = Identifier.withDefaultNamespace("potato");
        TagKey<Item> farming = TagKey.create(Registries.ITEM, id("market_category/farming"));
        helper.assertTrue(Items.WHEAT.getDefaultInstance().is(farming) && !Items.COBBLESTONE.getDefaultInstance().is(farming),
                "wheat is farming, cobblestone is not");

        withTerritory(helper, () -> {
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            try {
                data.setPressure(wheat, 5000);
                data.setPressure(cobblestone, 5000);
                double wheatBefore = Market.quote(level, pos, Items.WHEAT, 1).orElseThrow().factor();
                double stoneBefore = Market.quote(level, pos, Items.COBBLESTONE, 1).orElseThrow().factor();

                int affected = Market.relievePressure(level, farming, 0.5, 0.0);
                helper.assertTrue(affected >= 1, "at least wheat is affected");
                helper.assertTrue(Market.quote(level, pos, Items.WHEAT, 1).orElseThrow().factor() > wheatBefore, "wheat price recovers");
                helper.assertTrue(Market.quote(level, pos, Items.COBBLESTONE, 1).orElseThrow().factor() == stoneBefore,
                        "other groups stay untouched");

                Market.relievePressure(level, null, 0.5, 0.0);
                helper.assertTrue(Market.quote(level, pos, Items.COBBLESTONE, 1).orElseThrow().factor() > stoneBefore,
                        "an event for all goods reaches every group");

                // Nachfrageschub: Auch eine nie verkaufte Ware steigt über den Ruhepreis und sinkt danach dorthin zurück.
                data.clearPressure(potato);
                Market.Quote resting = Market.quote(level, pos, Items.POTATO, 1).orElseThrow();
                helper.assertFalse(resting.rising() || resting.falling(), "an untraded good rests at its usual maximum");
                Market.relievePressure(level, farming, 0.5, 0.5);
                Market.Quote boosted = Market.quote(level, pos, Items.POTATO, 1).orElseThrow();
                helper.assertTrue(Math.abs(boosted.factor() - Math.min(DtpConfig.FACTOR_MAX.get(), resting.factor() * Math.sqrt(2.0))) < 1e-6,
                        "a boost of H/2 lifts the price by a factor of sqrt(2): " + resting.factor() + " -> " + boosted.factor());
                helper.assertTrue(boosted.falling(), "after the event the price sinks back");
                data.advance(30);
                double later = Market.quote(level, pos, Items.POTATO, 1).orElseThrow().factor();
                helper.assertTrue(later < boosted.factor() && later > resting.factor() - 1e-6, "it settles back towards the resting price");
            } finally {
                data.clearPressure(wheat);
                data.clearPressure(cobblestone);
                data.clearPressure(potato);
            }
        });

        long oldSeconds = data.eventSeconds();
        String oldTarget = data.eventTarget();
        try {
            data.scheduleEvent(3600, "");
            MarketEvents.onContractCompleted(server, 2);
            long expected = 3600 - Math.round(DtpConfig.listValue(DtpConfig.EVENT_CONTRACT_MINUTES.get(), 1, 0.0) * 60);
            if (DtpConfig.EVENTS_ENABLED.get()) {
                helper.assertTrue(data.eventSeconds() == Math.max(0, expected), "a tier 2 contract shortens the wait: " + data.eventSeconds());
            }
        } finally {
            data.scheduleEvent(oldSeconds, oldTarget);
        }

        boolean oldWither = data.witherCounted();
        String oldType = data.eventType();
        try {
            data.scheduleEvent(7200, "");
            data.setWitherCounted(false);
            long witherSeconds = Math.round(DtpConfig.EVENT_WITHER_MINUTES.get() * 60);
            MarketEvents.onWitherKilled(server);
            if (DtpConfig.EVENTS_ENABLED.get()) {
                helper.assertTrue(data.eventSeconds() == Math.max(0, 7200 - witherSeconds), "a wither shortens the wait: " + data.eventSeconds());
                long afterFirst = data.eventSeconds();
                MarketEvents.onWitherKilled(server);
                helper.assertTrue(data.eventSeconds() == afterFirst, "a second wither in the same cycle does nothing: " + data.eventSeconds());
                MarketEvents.setCountdown(server, 120);
                MarketEvents.onWitherKilled(server);
                helper.assertTrue(data.eventSeconds() == 7200, "rescheduling the same event keeps the wither spent: " + data.eventSeconds());
                MarketData loaded = MarketData.CODEC.parse(NbtOps.INSTANCE, MarketData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
                helper.assertTrue(loaded.witherCounted(), "the spent wither survives saving");
                // Eine Schnelle Erholung, damit der Test keine Warengruppe anhebt.
                data.scheduleEvent(data.eventSeconds(), "", MarketEvents.Type.FAST_RECOVERY.id());
                MarketEvents.fire(server);
                long fresh = data.eventSeconds();
                MarketEvents.onWitherKilled(server);
                helper.assertTrue(data.eventSeconds() == Math.max(0, fresh - witherSeconds), "the next cycle counts a wither again: " + data.eventSeconds());
            }
        } finally {
            endRunningEvents(level);
            data.scheduleEvent(oldSeconds, oldTarget, oldType);
            data.setWitherCounted(oldWither);
        }
        helper.succeed();
    }

    /** Beendet alle laufenden Ereignisse mit Laufzeit — Aufräumen nach Tests. */
    private static void endRunningEvents(ServerLevel level) {
        for (MarketData.RunningEvent e : MarketData.get(level.getServer()).running()) {
            Market.endFastRecovery(level, e);
        }
    }

    /**
     * Ereignistypen: Der Typ wird zuerst gewürfelt, beide kommen vor. Eine Schnelle Erholung gilt allen Waren und
     * nimmt keine Gruppe an. Sie ersetzt die laufende, Typ und laufende Ereignisse überstehen das Speichern.
     */
    private static void marketEventTypes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        MarketData data = MarketData.get(server);
        String farming = id("farming").toString();
        String ores = id("ores").toString();
        long oldSeconds = data.eventSeconds();
        String oldTarget = data.eventTarget();
        String oldType = data.eventType();
        List<String> oldDrawn = data.drawnEvents();
        try {
            boolean surge = false, fast = false;
            for (int i = 0; i < 200; i++) {
                MarketEvents.Type type = MarketEvents.rollType(server);
                surge |= type == MarketEvents.Type.DEMAND_SURGE;
                fast |= type == MarketEvents.Type.FAST_RECOVERY;
            }
            double chance = DtpConfig.EVENT_FAST_RECOVERY_CHANCE.get();
            helper.assertTrue((chance >= 1.0 || surge) && (chance <= 0.0 || fast), "both types are rolled");

            // Typ setzen: Die Schnelle Erholung zieht das Ziel auf alle Waren, die Wartezeit bleibt.
            data.scheduleEvent(600, farming, MarketEvents.Type.DEMAND_SURGE.id());
            MarketEvents.setType(server, MarketEvents.Type.FAST_RECOVERY);
            helper.assertTrue(data.eventType().equals("fast_recovery") && data.eventTarget().isEmpty() && data.eventSeconds() == 600,
                    "fast recovery targets all goods, the wait stays");
            helper.assertFalse(MarketEvents.setTarget(server, ores), "a fast recovery takes no group");
            helper.assertTrue(MarketEvents.setTarget(server, "") && data.eventTarget().isEmpty(), "all goods is fine");

            int goods = MarketEvents.fire(server);
            helper.assertTrue(goods > 0 && data.running().size() == 1 && data.running().getFirst().target().isEmpty(),
                    "firing starts a fast recovery for all goods");
            MarketData.RunningEvent first = data.running().getFirst();
            helper.assertTrue(Math.abs(first.end() - first.start() - DtpConfig.EVENT_FAST_RECOVERY_DAYS.get()) < 1e-9, "runs for the configured days");

            // Ein Nachfrageschub behält beim Typwechsel seine Gruppe; ohne Gruppe lost er eine aus, nie „alle Waren".
            data.scheduleEvent(600, ores, MarketEvents.Type.FAST_RECOVERY.id());
            MarketEvents.setType(server, MarketEvents.Type.DEMAND_SURGE);
            helper.assertTrue(data.eventTarget().equals(ores) && data.eventType().equals("demand_surge"), "a demand surge keeps its target");
            data.scheduleEvent(600, "", MarketEvents.Type.FAST_RECOVERY.id());
            MarketEvents.setType(server, MarketEvents.Type.DEMAND_SURGE);
            helper.assertTrue(!data.eventTarget().isEmpty() && data.eventType().equals("demand_surge") && data.eventSeconds() == 600,
                    "a demand surge draws a group, the wait stays: " + data.eventTarget());
            helper.assertFalse(MarketEvents.setTarget(server, ""), "a demand surge never targets all goods");
            // Alte Stände: Ein Schub für alle Waren wird zur Schnellen Erholung.
            int before = data.running().size();
            helper.assertTrue(MarketEvents.trigger(server, MarketEvents.Type.DEMAND_SURGE, "") > 0
                    && data.running().size() == Math.max(1, before), "an old all-goods surge becomes a fast recovery");

            Market.startFastRecovery(level, null, "", 3.0);
            helper.assertTrue(data.running().size() == 1 && Math.abs(data.running().getFirst().end() - data.day() - 3.0) < 1e-9,
                    "a second fast recovery replaces the running one");

            data.scheduleEvent(600, "", MarketEvents.Type.FAST_RECOVERY.id());
            MarketData loaded = MarketData.CODEC.parse(NbtOps.INSTANCE, MarketData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow()).getOrThrow();
            helper.assertTrue(loaded.eventType().equals("fast_recovery") && loaded.running().equals(data.running()),
                    "type and running events survive saving");

            // Admin-Befehle: Ziel auflösen, gezielt auslösen, laufende beenden.
            helper.assertTrue(MarketEvents.resolveTarget("all").isEmpty()
                    && MarketEvents.resolveTarget("farming").equals(java.util.Optional.of(farming))
                    && MarketEvents.resolveTarget(farming).equals(java.util.Optional.of(farming))
                    && MarketEvents.resolveTarget("no_such_group").isEmpty(), "targets resolve by path or full id, \"all\" is gone");
            MarketEvents.endRunning(server);
            helper.assertTrue(data.running().isEmpty(), "ending clears running events");
            helper.assertTrue(MarketEvents.trigger(server, MarketEvents.Type.FAST_RECOVERY, farming) > 0
                    && data.running().size() == 1 && data.running().getFirst().target().isEmpty(),
                    "a fast recovery ignores a requested group and covers all goods");
            helper.assertTrue(data.eventTarget().isEmpty() && data.eventType().equals("fast_recovery") && data.eventSeconds() == 600,
                    "triggering leaves the schedule alone");
            helper.assertTrue(MarketEvents.endRunning(server) == 1 && data.running().isEmpty(), "end reports how many it ended");
        } finally {
            endRunningEvents(level);
            data.scheduleEvent(oldSeconds, oldTarget, oldType);
            data.resetDrawn();
            oldDrawn.forEach(data::markDrawn);
        }
        helper.succeed();
    }

    /**
     * Schnelle Erholung: Ein Markttag im Fenster zählt wie zwei, für Waren aller Gruppen; nie verkaufte Waren und
     * Waren über dem Ruhepreis bleiben unberührt. Verkäufe im Fenster erzeugen nur den eingestellten Anteil des
     * Drucks, Angebot und Auszahlung stimmen überein. Beim Ende springt kein Preis, auch wenn die Uhr schon über das
     * Ende hinaus ist.
     */
    private static void marketFastRecovery(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MarketData data = MarketData.get(level.getServer());
        Identifier wheat = Identifier.withDefaultNamespace("wheat");
        Identifier cobblestone = Identifier.withDefaultNamespace("cobblestone");
        Identifier potato = Identifier.withDefaultNamespace("potato");
        Identifier carrot = Identifier.withDefaultNamespace("carrot");
        double wheatS = TradeList.lookup(Items.WHEAT.getDefaultInstance()).orElseThrow().halfAmount();
        double stoneS = TradeList.lookup(Items.COBBLESTONE.getDefaultInstance()).orElseThrow().halfAmount();
        try {
            endRunningEvents(level);
            // Ein Markttag mit Schneller Erholung …
            data.setPressure(wheat, wheatS);
            data.setPressure(cobblestone, stoneS);
            data.clearPressure(potato);
            double resting = Market.fullMarketFactor(level, Items.POTATO).orElseThrow();
            Market.startFastRecovery(level, null, "", 3.0);
            data.advance(1);
            double fastWheat = Market.fullMarketFactor(level, Items.WHEAT).orElseThrow();
            double fastStone = Market.fullMarketFactor(level, Items.COBBLESTONE).orElseThrow();
            helper.assertTrue(Math.abs(Market.fullMarketFactor(level, Items.POTATO).orElseThrow() - resting) < 1e-9,
                    "an untraded good stays at its resting price");
            endRunningEvents(level);
            // … entspricht zwei normalen Markttagen, für Landwirtschaft wie für Baumaterial.
            data.setPressure(wheat, wheatS);
            data.setPressure(cobblestone, stoneS);
            data.advance(2);
            double normalWheat = Market.fullMarketFactor(level, Items.WHEAT).orElseThrow();
            helper.assertTrue(Math.abs(fastWheat - normalWheat) < 1e-9, "one day of fast recovery equals two days: " + fastWheat + " vs " + normalWheat);
            helper.assertTrue(Math.abs(Market.fullMarketFactor(level, Items.COBBLESTONE).orElseThrow() - fastStone) < 1e-9,
                    "goods of every group recover faster");

            // Nur aufwärts: Über dem Ruhepreis sinkt der Preis im Fenster wie ohne.
            double above = -Market.halfAmount(level, Items.WHEAT);
            data.setPressure(wheat, above);
            data.advance(1);
            double normalAbove = Market.fullMarketFactor(level, Items.WHEAT).orElseThrow();
            data.setPressure(wheat, above);
            Market.startFastRecovery(level, null, "", 3.0);
            data.advance(1);
            helper.assertTrue(Math.abs(Market.fullMarketFactor(level, Items.WHEAT).orElseThrow() - normalAbove) < 1e-9,
                    "above the rest price the window does not speed up the fall");

            // Verkäufe im Fenster erzeugen nur den eingestellten Anteil des Drucks; Angebot = Auszahlung.
            double share = DtpConfig.EVENT_FAST_RECOVERY_PRESSURE_SHARE.get();
            withTerritory(helper, () -> {
                BlockPos pos = helper.absolutePos(BlockPos.ZERO);
                data.clearPressure(carrot);
                double sFloor = Market.halfAmount(level, Items.CARROT) * -DtpConfig.FLOOR_RATIO.get();
                Market.Quote quote = Market.quote(level, pos, Items.CARROT, 100).orElseThrow();
                long paid = Market.sell(level, pos, Items.CARROT, 100);
                helper.assertTrue(paid == quote.total(), "the quote matches the payout in the window");
                double added = data.pressure(carrot, sFloor, 1.0) - sFloor;
                helper.assertTrue(Math.abs(added - 100 * share) < 1e-6, "100 carrots add " + (100 * share) + " pressure, got " + added);
            });
            endRunningEvents(level);
            withTerritory(helper, () -> {
                BlockPos pos = helper.absolutePos(BlockPos.ZERO);
                data.clearPressure(carrot);
                double sFloor = Market.halfAmount(level, Items.CARROT) * -DtpConfig.FLOOR_RATIO.get();
                Market.sell(level, pos, Items.CARROT, 100);
                helper.assertTrue(Math.abs(data.pressure(carrot, sFloor, 1.0) - (sFloor + 100)) < 1e-6, "outside the window a sale adds full pressure");
            });

            // Ende nach einem Sprung über das Fenster hinaus: kein Preissprung, danach normales T.
            data.setPressure(wheat, wheatS);
            Market.startFastRecovery(level, null, "", 1.0);
            data.advance(1.5);
            double before = Market.fullMarketFactor(level, Items.WHEAT).orElseThrow();
            endRunningEvents(level);
            helper.assertTrue(data.running().isEmpty(), "the window is gone");
            helper.assertTrue(Math.abs(Market.fullMarketFactor(level, Items.WHEAT).orElseThrow() - before) < 1e-9, "no jump at the end");
            data.advance(1);
            double after = Market.fullMarketFactor(level, Items.WHEAT).orElseThrow();
            data.setPressure(wheat, wheatS);
            data.advance(3.5);
            helper.assertTrue(Math.abs(Market.fullMarketFactor(level, Items.WHEAT).orElseThrow() - after) < 1e-9,
                    "1 fast day + 0.5 + 1 normal days = 3.5 normal days");
        } finally {
            endRunningEvents(level);
            data.clearPressure(wheat);
            data.clearPressure(cobblestone);
            data.clearPressure(carrot);
        }
        helper.succeed();
    }

    /**
     * Aufschläge aus der Welt: Regen hebt die Fischerei um den Aufschlag, gedeckelt auf die Obergrenze, außerhalb
     * des Territoriums bleibt die Untergrenze. Vollmond gilt nur nachts und nur bei Vollmond.
     */
    private static void marketWorldBonus(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        helper.assertTrue(Market.fullMoonNight(18000L, MoonPhase.FULL_MOON), "full moon at midnight");
        helper.assertTrue(Market.fullMoonNight(8 * 24000L + 14000L, MoonPhase.FULL_MOON), "on any day of the clock");
        helper.assertFalse(Market.fullMoonNight(6000L, MoonPhase.FULL_MOON), "not at noon");
        helper.assertFalse(Market.fullMoonNight(18000L, MoonPhase.WANING_GIBBOUS), "not in another phase");

        var weather = level.getServer().overworld().getWeatherData();
        boolean raining = weather.isRaining();
        int rainTime = weather.getRainTime();
        withTerritory(helper, () -> {
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            try {
                weather.setRaining(false);
                helper.assertTrue(Market.activeBonuses(level.getServer()).stream().noneMatch(b -> b.event().equals("rain")), "no rain, no bonus");
                Market.Quote dryQuote = Market.quote(level, pos, Items.COD, 1).orElseThrow();
                double dry = dryQuote.factor();
                double dryWheat = Market.quote(level, pos, Items.WHEAT, 1).orElseThrow().factor();
                weather.setRaining(true);
                weather.setRainTime(6000);
                double bonus = DtpConfig.EVENTS_ENABLED.get() ? DtpConfig.EVENT_RAIN_BONUS.get() : 0.0;
                Market.Quote wet = Market.quote(level, pos, Items.COD, 1).orElseThrow();
                helper.assertTrue(Math.abs(wet.factor() - Math.min(DtpConfig.FACTOR_MAX.get(), dry * (1.0 + bonus))) < 1e-9,
                        "rain lifts fishing by the bonus: " + dry + " -> " + wet.factor());
                helper.assertTrue(wet.rising() == dryQuote.rising() && wet.falling() == dryQuote.falling(),
                        "trend arrows only follow the pressure");
                helper.assertTrue(Market.quote(level, pos, Items.WHEAT, 1).orElseThrow().factor() == dryWheat, "other groups stay");
                helper.assertTrue(Market.quote(level, pos.offset(100000, 0, 0), Items.COD, 1).orElseThrow().factor() == DtpConfig.FACTOR_MIN.get(),
                        "outside the territory the floor stays");
                weather.setRaining(false);
                helper.assertTrue(Market.quote(level, pos, Items.COD, 1).orElseThrow().factor() == dry, "the bonus ends with the rain");
            } finally {
                weather.setRainTime(rainTime);
                weather.setRaining(raining);
            }
        });
        helper.succeed();
    }

    /**
     * Freischaltung der Warengruppen: Nether erst mit dem Nether-Zugang, End erst nach dem Enderdrachen. Neu freie
     * Gruppen reihen sich als Ziel der nächsten Ereignisse ein, ohne das angekündigte zu ersetzen; der erste Abgleich
     * (neue Welt, alter Stand) reiht nichts ein, und gesperrte Gruppen werden nie ausgelost.
     */
    private static void marketCategoryUnlock(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        MarketData data = MarketData.get(server);
        String nether = id("nether").toString();
        String end = id("end").toString();
        String farming = id("farming").toString();
        String wood = id("construction").toString();

        MarketCategories.Category netherCategory = MarketCategories.byId(nether).orElseThrow();
        MarketCategories.Category endCategory = MarketCategories.byId(end).orElseThrow();
        helper.assertTrue(netherCategory.unlock().dimension().filter(d -> d == Level.NETHER).isPresent(), "nether needs the nether");
        helper.assertTrue(endCategory.unlock().dragonKilled(), "end needs the ender dragon");
        helper.assertTrue(MarketCategories.byId(farming).orElseThrow().unlocked(server), "farming is always open");

        int oldAccess = progress.level(ServerUpgrade.NETHER_ACCESS);
        List<String> oldKnown = data.knownCategories();
        List<String> oldQueue = data.eventQueue();
        List<String> oldDrawn = data.drawnEvents();
        long oldSeconds = data.eventSeconds();
        String oldTarget = data.eventTarget();
        try {
            // Sichtbarkeit: Nether hängt am Upgrade (sofern die Sperre an ist), End am Drachen.
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, 0);
            helper.assertTrue(netherCategory.unlocked(server) == !DtpConfig.NETHER_LOCK.get(), "nether is hidden while locked");
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, 1);
            helper.assertTrue(netherCategory.unlocked(server), "nether shows after unlocking");
            helper.assertTrue(endCategory.unlocked(server) == MarketCategories.dragonKilled(server), "end follows the dragon");
            // Waren nur gesperrter Gruppen fehlen in der Preisübersicht, Waren mit einer offenen Gruppe nicht.
            helper.assertTrue(MarketCategories.visible(server, Items.CHORUS_FRUIT) == MarketCategories.dragonKilled(server),
                    "chorus fruit shows only after the dragon");
            helper.assertTrue(MarketCategories.visible(server, Items.ENDER_PEARL), "ender pearls are also mob drops");
            helper.assertTrue(MarketCategories.visible(server, Items.DIAMOND_SWORD), "items without a category are not hidden");

            // Erster Abgleich: nur merken, nichts einreihen.
            data.resetCategoryUnlocks(null, List.of());
            helper.assertTrue(data.trackUnlockedCategories(List.of(wood, nether)).isEmpty(), "the first sync queues nothing");
            helper.assertTrue(data.eventQueue().isEmpty(), "an already open nether gets no late event");

            // Freischaltung während ein Ereignis angekündigt ist: das angekündigte bleibt, die Gruppen reihen sich ein.
            data.resetCategoryUnlocks(List.of(wood), List.of());
            data.scheduleEvent(600, wood);
            data.trackUnlockedCategories(List.of(wood, nether));
            data.trackUnlockedCategories(List.of(wood, nether, farming));
            helper.assertTrue(data.eventSeconds() == 600 && data.eventTarget().equals(wood), "the announced event stays");
            helper.assertTrue(data.eventQueue().equals(List.of(nether, farming)), "queued in unlock order: " + data.eventQueue());
            data.trackUnlockedCategories(List.of(wood, nether, farming));
            helper.assertTrue(data.eventQueue().size() == 2, "a known category is not queued twice");
            helper.assertTrue(MarketEvents.nextTarget(server, data).equals(nether), "the next event targets the first unlock");
            helper.assertTrue(MarketEvents.nextTarget(server, data).equals(farming), "then the second");

            // Entfernte und gesperrte Ziele in der Warteschlange werden übersprungen; ausgelost wird nur Freies.
            if (DtpConfig.NETHER_LOCK.get()) {
                progress.setLevel(ServerUpgrade.NETHER_ACCESS, 0);
            }
            data.resetCategoryUnlocks(List.of(), List.of(id("gone").toString(), nether, wood));
            String target = MarketEvents.nextTarget(server, data);
            helper.assertTrue(target.equals(DtpConfig.NETHER_LOCK.get() ? wood : nether), "unknown and locked targets are skipped: " + target);
            for (int i = 0; i < 200; i++) {
                String drawn = MarketEvents.nextTarget(server, data);
                helper.assertFalse(DtpConfig.NETHER_LOCK.get() && drawn.equals(nether), "a locked nether is never drawn");
                helper.assertFalse(!MarketCategories.dragonKilled(server) && drawn.equals(end), "the end is never drawn before the dragon");
            }
        } finally {
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, oldAccess);
            data.resetCategoryUnlocks(oldKnown, oldQueue);
            data.scheduleEvent(oldSeconds, oldTarget);
            data.resetDrawn();
            oldDrawn.forEach(data::markDrawn);
        }
        helper.succeed();
    }

    /**
     * Marktereignisse reihum: Jede freigeschaltete Gruppe ist einmal Ziel, bevor sich eine wiederholt; kein Ziel
     * (auch nicht "alle Waren") kommt zweimal hintereinander, auch nicht über die Grenze einer Runde.
     */
    private static void marketEventRotation(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        MarketData data = MarketData.get(server);
        List<String> unlocked = MarketCategories.unlocked(server).stream().map(c -> c.id().toString()).toList();
        List<String> oldKnown = data.knownCategories();
        List<String> oldQueue = data.eventQueue();
        List<String> oldDrawn = data.drawnEvents();
        long oldSeconds = data.eventSeconds();
        String oldTarget = data.eventTarget();
        String oldType = data.eventType();
        try {
            data.resetCategoryUnlocks(unlocked, List.of());
            data.resetDrawn();
            // Die Reihum-Regeln gelten für Nachfrageschübe; eine Schnelle Erholung zieht keine Gruppe.
            data.scheduleEvent(600, "", MarketEvents.Type.DEMAND_SURGE.id());
            List<String> round = new ArrayList<>();
            String previous = "";
            for (int i = 0; i < 40 * unlocked.size(); i++) {
                String target = MarketEvents.nextTarget(server, data);
                helper.assertFalse(target.equals(previous), "no target twice in a row: " + target);
                helper.assertFalse(target.isEmpty(), "a demand surge never targets all goods");
                data.scheduleEvent(600, target);
                previous = target;
                helper.assertTrue(unlocked.contains(target), "only unlocked categories are drawn: " + target);
                helper.assertFalse(round.contains(target), "a category repeats before all had their turn: " + target + " in " + round);
                round.add(target);
                if (round.size() == unlocked.size()) {
                    round.clear();
                }
            }
        } finally {
            data.resetCategoryUnlocks(oldKnown, oldQueue);
            data.scheduleEvent(oldSeconds, oldTarget, oldType);
            data.resetDrawn();
            oldDrawn.forEach(data::markDrawn);
        }
        helper.succeed();
    }

    /**
     * Preisverlauf: Verkäufe hinterlassen höchstens einen Stützpunkt pro Markttag mit Tiefst-, Höchst- und letztem
     * Wert, ältere als 14 Tage fallen weg, und die tägliche Messung erfasst auch nie verkaufte Waren. Nach einem
     * Verkauf liegt der Basiswert in der Zukunft, ohne Marktdruck nicht.
     */
    private static void priceHistory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MarketData data = MarketData.get(level.getServer());
        Identifier carrot = Identifier.withDefaultNamespace("carrot");
        withTerritory(helper, () -> {
            BlockPos pos = helper.absolutePos(BlockPos.ZERO);
            try {
                data.clearPressure(carrot);
                helper.assertTrue(Market.daysToBase(level, Items.CARROT) == 0.0, "an unsold good is at its base value");
                helper.assertTrue(Market.daysToRest(level, Items.CARROT) == 0.0, "an unsold good is at its rest price");
                int before = data.history(carrot).size();
                double restingFactor = Market.fullMarketFactor(level, Items.CARROT).orElseThrow();
                // Verkaufen, bis der Preis unter dem Basiswert liegt — wie viel das ist, hängt von H der Handelsliste ab.
                for (int i = 0; i < 200 && Market.fullMarketFactor(level, Items.CARROT).orElseThrow() >= 1.0; i++) {
                    Market.sell(level, pos, Items.CARROT, 500);
                }
                List<MarketData.HistoryPoint> points = data.history(carrot);
                helper.assertTrue(points.size() <= before + 1, "several sales on one day leave one point");
                MarketData.HistoryPoint today = points.getLast();
                double now = Market.fullMarketFactor(level, Items.CARROT).orElseThrow();
                helper.assertTrue(Math.abs(today.low() - now) < 1e-9 && Math.abs(today.close() - now) < 1e-9,
                        "the day holds the lowest and the latest factor");
                helper.assertTrue(today.high() >= restingFactor - 1e-9, "the price before the first sale is kept as the high of the day");
                helper.assertTrue(Market.daysToBase(level, Items.CARROT) > 0.0, "after selling, the base value lies ahead");
                helper.assertTrue(Market.daysToRest(level, Items.CARROT) > Market.daysToBase(level, Items.CARROT),
                        "the rest price lies beyond the base value");
                // Nach einem Nachfrageschub liegt der Druck unter dem Grundwert: nichts mehr zu erholen.
                data.setPressure(carrot, -1.0e6);
                helper.assertTrue(Market.daysToRest(level, Items.CARROT) == 0.0, "above the rest price there is no recovery");

                data.advance(MarketData.HISTORY_DAYS + 1);
                Market.record(level, Items.CARROT);
                helper.assertTrue(data.history(carrot).size() == 1, "points older than the history window are dropped");

                // Die tägliche Messung erfasst jede handelbare Ware, auch nie verkaufte.
                data.advance(1);
                Market.sampleHistory(level);
                List<MarketData.HistoryPoint> beetroot = data.history(Identifier.withDefaultNamespace("beetroot"));
                helper.assertTrue(!beetroot.isEmpty() && beetroot.getLast().day() == (int) Math.floor(data.day()),
                        "untraded goods get a daily point too");
            } finally {
                data.clearPressure(carrot);
            }
        });
        helper.succeed();
    }

    /**
     * Kettenabbau im echten Abbau: Das Werkzeug kann durch die Kette zerbrechen, dann endet sie. Ein Werkzeug, das
     * den Block nicht ernten kann, löst keine Kette aus.
     */
    private static void chainDurability(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.TREE_FELLING, 3);
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.VEIN_MINING, 1);
        List<BlockPos> column = List.of(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1), new BlockPos(1, 3, 1), new BlockPos(1, 4, 1),
                new BlockPos(1, 5, 1));
        List<BlockPos> vein = List.of(new BlockPos(3, 1, 1), new BlockPos(3, 2, 1), new BlockPos(3, 3, 1));
        try {
            // Steinaxt mit 3 Restpunkten: Sie zerbricht in der Kette, der oberste Stamm bleibt stehen.
            column.forEach(p -> helper.setBlock(p, Blocks.OAK_LOG));
            ItemStack axe = new ItemStack(Items.STONE_AXE);
            axe.setDamageValue(axe.getMaxDamage() - 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, axe);
            player.gameMode.destroyBlock(helper.absolutePos(column.getFirst()));
            helper.assertTrue(axe.isEmpty(), "a nearly broken axe breaks during tree felling");
            helper.assertTrue(helper.getBlockState(column.getLast()).is(Blocks.OAK_LOG), "the chain ends when the axe breaks");

            // Mit einer frischen Axt fällt der ganze Stamm.
            column.forEach(p -> helper.setBlock(p, Blocks.OAK_LOG));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            player.gameMode.destroyBlock(helper.absolutePos(column.getFirst()));
            helper.assertTrue(column.stream().allMatch(p -> helper.getBlockState(p).isAir()), "a fresh axe fells the whole column");

            // Holzspitzhacke an Diamanterz: kein Ertrag möglich, also auch keine Kette.
            vein.forEach(p -> helper.setBlock(p, Blocks.DIAMOND_ORE));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WOODEN_PICKAXE));
            player.gameMode.destroyBlock(helper.absolutePos(vein.getFirst()));
            helper.assertTrue(helper.getBlockState(vein.get(1)).is(Blocks.DIAMOND_ORE) && helper.getBlockState(vein.get(2)).is(Blocks.DIAMOND_ORE),
                    "no chain with a tool that cannot harvest the ore");
        } finally {
            column.forEach(p -> helper.setBlock(p, Blocks.AIR));
            vein.forEach(p -> helper.setBlock(p, Blocks.AIR));
        }
        helper.succeed();
    }

    /**
     * Mehrfachabbau ins Inventar: Die Drops aller gefällten Stämme, auch des selbst abgebauten, landen beim Spieler,
     * keiner bleibt am Baum liegen; danach sammelt er kurz in größerem Radius auf. Ist das Inventar voll, fallen die
     * Drops um den Spieler statt am Baum.
     */
    private static void chainInventory(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        PlayerUpgrades.of(player).setLevel(PlayerUpgrade.TREE_FELLING, 1);
        BlockPos feet = helper.absolutePos(new BlockPos(5, 1, 5));
        player.snapTo(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5, 0f, 0f);
        List<BlockPos> column = List.of(new BlockPos(1, 1, 1), new BlockPos(1, 2, 1), new BlockPos(1, 3, 1), new BlockPos(1, 4, 1));
        AABB tree = new AABB(Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(0, 0, 0))),
                Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(3, 6, 3))));
        AABB around = new AABB(feet).inflate(1.5);
        try {
            column.forEach(p -> helper.setBlock(p, Blocks.OAK_LOG));
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
            player.gameMode.destroyBlock(helper.absolutePos(column.getFirst()));
            helper.assertTrue(column.stream().allMatch(p -> helper.getBlockState(p).isAir()), "the whole column is felled");
            int logs = player.getInventory().countItem(Items.OAK_LOG);
            helper.assertTrue(logs == column.size(), "all logs incl. the first go to the inventory, got " + logs);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, tree).isEmpty(), "nothing is left lying at the tree");
            helper.assertTrue(PickupBoost.range(player) == param(PlayerUpgrade.TREE_FELLING, "pickup_range", 1),
                    "felling a tree widens the pickup range");

            // Volles Inventar: Die Stämme fallen um den Spieler, nicht am Baum.
            int selected = player.getInventory().getSelectedSlot();
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                if (i != selected) {
                    player.getInventory().setItem(i, new ItemStack(Items.STONE, 64));
                }
            }
            column.forEach(p -> helper.setBlock(p, Blocks.OAK_LOG));
            player.gameMode.destroyBlock(helper.absolutePos(column.getFirst()));
            int dropped = level.getEntitiesOfClass(ItemEntity.class, around).stream()
                    .filter(e -> e.getItem().is(Items.OAK_LOG)).mapToInt(e -> e.getItem().getCount()).sum();
            helper.assertTrue(dropped == column.size(), "with a full inventory the logs drop at the player, got " + dropped);
            helper.assertTrue(level.getEntitiesOfClass(ItemEntity.class, tree).isEmpty(), "still nothing at the tree");
        } finally {
            column.forEach(p -> helper.setBlock(p, Blocks.AIR));
            level.getEntitiesOfClass(ItemEntity.class, around.minmax(tree)).forEach(ItemEntity::discard);
        }
        helper.succeed();
    }

    /**
     * Ein Contract über die gemeinsame Ware „Holzstämme“ (ebenso „Wolle“) nimmt jede Stammart, gemischt und in Kistenreihenfolge,
     * bis er voll ist. Entrindete und umbenannte Stämme zählen nicht. Der Schlüssel übersteht das Speichern.
     */
    /**
     * Eine gemeinsame Ware ohne feste Anzahl: mindestens zwei Mitglieder, jedes gehört zur Gruppe, und jedes handelbare
     * Item der Gruppe steht unter diesem Key. Neue oder gestrichene Einträge der Handelsliste lassen den Test grün,
     * ein vergessener Key nicht.
     */
    private static void assertSharedGood(GameTestHelper helper, Identifier key, Predicate<Item> belongs) {
        List<Item> members = TradeList.members(key);
        helper.assertTrue(members.size() >= 2, key + " is shared by at least two items: " + members);
        List<String> strangers = members.stream().filter(belongs.negate()).map(item -> Market.itemId(item).toString()).toList();
        helper.assertTrue(strangers.isEmpty(), key + " holds items outside its group: " + strangers);
        List<String> missing = BuiltInRegistries.ITEM.stream()
                .filter(belongs)
                .filter(item -> TradeList.lookup(item).isPresent() && !Market.marketKey(item).equals(key))
                .map(item -> Market.itemId(item).toString())
                .toList();
        helper.assertTrue(missing.isEmpty(), "tradable items of the group without the key " + key + ": " + missing);
    }

    private static void contractGroupDelivery(GameTestHelper helper) {
        BlockPos tradeRel = new BlockPos(0, 1, 0);
        BlockPos depositRel = new BlockPos(2, 1, 0);
        helper.setBlock(tradeRel, ModBlocks.TRADE_CHEST.get());
        helper.setBlock(depositRel, ModBlocks.DEPOSIT_CHEST.get());
        TradeChestBlockEntity trade = helper.getBlockEntity(tradeRel, TradeChestBlockEntity.class);
        DepositChestBlockEntity deposit = helper.getBlockEntity(depositRel, DepositChestBlockEntity.class);
        ServerLevel level = helper.getLevel();
        BlockPos board = helper.absolutePos(new BlockPos(1, 1, 0));
        double day = MarketData.get(level.getServer()).contractDay();
        Identifier wood = id("wood_log");
        assertSharedGood(helper, wood, item -> {
            String path = Market.itemId(item).getPath();
            return path.endsWith("_log") && !path.startsWith("stripped_");
        });

        // Wolle: alle 16 Farben eine Ware zum Preis der weißen, ein Wolle-Contract nimmt jede Farbe.
        Identifier wool = id("wool");
        assertSharedGood(helper, wool, item -> new ItemStack(item).is(ItemTags.WOOL));
        helper.assertTrue(TradeList.lookup(Items.WOOL.red()).map(TradeList.Entry::base).equals(TradeList.lookup(Items.WOOL.white()).map(TradeList.Entry::base)),
                "every wool color has the price of white wool");
        Contract woolContract = Contract.offer(Identifier.withDefaultNamespace("white_wool"), Optional.of(wool), DataComponentMatchers.ANY,
                10, 100, 1, day);
        helper.assertTrue(woolContract.matches(new ItemStack(Items.WOOL.blue())), "blue wool counts for wool");
        helper.assertFalse(woolContract.matches(new ItemStack(Items.CARPET.white())), "carpets do not count");

        Contract contract = Contract.offer(Identifier.withDefaultNamespace("oak_log"), Optional.of(wood), DataComponentMatchers.ANY,
                70, 1000, 1, day);
        helper.assertTrue(contract.matches(new ItemStack(Items.CHERRY_LOG)), "cherry counts for logs");
        helper.assertFalse(contract.matches(new ItemStack(Items.STRIPPED_OAK_LOG)), "stripped logs do not count");
        helper.assertFalse(contract.matches(new ItemStack(Items.OAK_PLANKS)), "planks do not count");
        HolderLookup.Provider registries = level.registryAccess();
        helper.assertFalse(contract.requirement(registries).equals(
                Contract.offer(Identifier.withDefaultNamespace("oak_log"), DataComponentMatchers.ANY, 1, 1, 1, day).requirement(registries)),
                "the log group and a plain oak log are different requirements");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Contracts.of(player).accepted().add(contract);
        ItemStack renamed = new ItemStack(Items.BIRCH_LOG, 5);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Lieblingsholz"));
        trade.setItem(0, new ItemStack(Items.OAK_LOG, 30));
        trade.setItem(1, new ItemStack(Items.STRIPPED_OAK_LOG, 10));
        trade.setItem(2, new ItemStack(Items.SPRUCE_LOG, 30));
        trade.setItem(3, renamed);
        trade.setItem(4, new ItemStack(Items.BIRCH_LOG, 30));
        helper.assertTrue(Sales.countForDelivery(level, board, contract::matches) == 90, "90 logs qualify");
        withTerritory(helper, () -> Contracts.deliver(level, board, player, contract.id()));
        helper.assertTrue(Contracts.of(player).accepted().isEmpty(), "contract completed with mixed logs");
        helper.assertTrue(trade.getItem(0).isEmpty() && trade.getItem(2).isEmpty(), "oak and spruce are taken first");
        helper.assertTrue(trade.getItem(4).getCount() == 20, "only the missing 10 birch logs are taken");
        helper.assertTrue(trade.getItem(1).getCount() == 10 && trade.getItem(3).getCount() == 5, "stripped and renamed logs stay");
        helper.assertTrue(deposit.balance() == contract.payout(day), "reward in deposit chest");

        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Contract loaded = Contract.CODEC.parse(ops, Contract.CODEC.encodeStart(ops, contract).getOrThrow()).getOrThrow();
        helper.assertTrue(loaded.key().equals(Optional.of(wood)) && loaded.matches(new ItemStack(Items.ACACIA_LOG)), "the key survives saving");
        cleanup(helper, tradeRel, depositRel);
        helper.succeed();
    }

    /** Contract-Qualität hebt jede Würfelchance je Stufe um contracts.tierChancePerLevel an, höchstens auf 1. */
    private static void contractQuality(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        int old = progress.level(ServerUpgrade.CONTRACT_QUALITY);
        UpgradeCosts.Definition def = UpgradeCosts.get(ServerUpgrade.CONTRACT_QUALITY);
        try {
            helper.assertTrue(def != null && def.maxLevel() == 2 && def.cost(1) == 200000 && def.cost(2) == 500000,
                    "contract quality has 2 levels for 200,000 / 500,000");
            double step = DtpConfig.TIER_CHANCE_PER_LEVEL.get();
            for (int level = 0; level <= 2; level++) {
                progress.setLevel(ServerUpgrade.CONTRACT_QUALITY, level);
                for (int tier = 2; tier <= 4; tier++) {
                    double base = DtpConfig.listValue(DtpConfig.TIER_CHANCE.get(), tier - 2, 0.5);
                    double expected = Math.min(1.0, base + level * step);
                    double actual = ContractBoard.tierChance(server, tier);
                    helper.assertTrue(Math.abs(actual - expected) < 1e-9,
                            "level " + level + " tier " + tier + ": " + actual + " vs " + expected);
                }
            }
            progress.setLevel(ServerUpgrade.CONTRACT_QUALITY, 1000);
            helper.assertTrue(step == 0.0 || ContractBoard.tierChance(server, 2) == 1.0, "the chance is capped at 1");
        } finally {
            progress.setLevel(ServerUpgrade.CONTRACT_QUALITY, old);
        }
        helper.succeed();
    }

    /**
     * Mehrstufige Vorlagen: Dieselbe Ware wird je Stufe in größerer Menge gefordert, im Korridor des Wertziels,
     * und höhere Stufen zahlen je Item mehr. Feste Mengen (Stufe 4) gelten unverändert.
     */
    private static void contractTierScaling(GameTestHelper helper) {
        // Irgendeine Vorlage der Stufen 1-3 mit gewürfelter Menge — unabhängig davon, welche Waren die Liste gerade dort führt.
        ContractTemplates.Template scaled = ContractTemplates.all().stream()
                .filter(t -> t.hasTier(1) && t.hasTier(2) && t.hasTier(3) && !t.fixedCount() && t.randomEnchantments().isEmpty())
                .findFirst().orElse(null);
        helper.assertTrue(scaled != null, "a template with a rolled count in tiers 1-3 exists");
        double base = TradeList.lookup(BuiltInRegistries.ITEM.getValue(scaled.item())).orElseThrow().base();
        double spread = DtpConfig.TIER_VALUE_SPREAD.get();
        RandomSource random = RandomSource.create(42);
        double lastAverage = 0;
        for (int tier = 1; tier <= 3; tier++) {
            double target = DtpConfig.listValue(DtpConfig.TIER_VALUE.get(), tier - 1, 700.0);
            double multiplier = DtpConfig.listValue(DtpConfig.REWARD_MULTIPLIER.get(), tier - 1, 2.0);
            long sum = 0;
            for (int i = 0; i < 50; i++) {
                Contract c = ContractBoard.create(scaled, tier, random, 0.0, helper.getLevel().registryAccess()).orElseThrow();
                double value = c.count() * base;
                helper.assertTrue(c.tier() == tier, "tier is taken over");
                helper.assertTrue(value >= target * (1 - spread) * 0.9 && value <= target * (1 + spread) * 1.1,
                        "tier " + tier + ": " + c.count() + " x " + scaled.item() + " is outside the value corridor");
                helper.assertTrue(c.reward() == Math.max(1, Math.round(base * multiplier * c.count())), "reward = base x count x multiplier");
                sum += c.count();
            }
            double average = sum / 50.0;
            helper.assertTrue(average > lastAverage * 2, "tier " + tier + " asks clearly more than the tier below: " + average);
            lastAverage = average;
        }
        // Die Staffelung gilt für die Standardwerte; eine Welt-Config darf davon abweichen.
        List<? extends Double> defaults = DtpConfig.REWARD_MULTIPLIER.getDefault();
        helper.assertTrue(defaults.get(0) < defaults.get(1) && defaults.get(1) < defaults.get(2), "higher tiers pay more per item by default");
        helper.assertTrue(ContractBoard.roundCount(0.3) == 1 && ContractBoard.roundCount(13) == 12 && ContractBoard.roundCount(183) == 176,
                "counts are rounded to 4 below 64 and to 16 above");

        // Jede Vorlage ohne feste Menge lässt sich in jeder ihrer Stufen würfeln.
        for (ContractTemplates.Template t : ContractTemplates.all()) {
            for (int tier : t.tiers()) {
                helper.assertTrue(ContractBoard.create(t, tier, random, 0.0, helper.getLevel().registryAccess()).isPresent(), t.item() + " cannot be created in tier " + tier);
            }
        }
        // Vorlagen mit fester Menge und festem Lohn je Item übernehmen beides unverändert, ohne Stufen-Multiplikator.
        for (ContractTemplates.Template t : ContractTemplates.all()) {
            if (!t.fixedCount() || t.rewardPerItem().isEmpty() || t.randomEnchantments().isPresent()) {
                continue;
            }
            for (int tier : t.tiers()) {
                Contract fixed = ContractBoard.create(t, tier, random, 0.0, helper.getLevel().registryAccess()).orElseThrow();
                helper.assertTrue(fixed.count() >= t.fixedMin() && fixed.count() <= t.fixedMax()
                                && fixed.reward() == Math.max(1, Math.round(t.rewardPerItem().get() * fixed.count())),
                        t.item() + " tier " + tier + ": fixed count and reward are taken as they are, got " + fixed.count() + " for " + fixed.reward());
            }
        }
        helper.succeed();
    }

    /**
     * Zufallsbuch: Beim Würfeln werden zwei verschiedene, verträgliche Verzauberungen aus dem Pool gezogen; die
     * Belohnung ist Grundpreis plus ihre Werte. Geliefert werden darf auch mit höherer Stufe und mehr Verzauberungen.
     */
    private static void contractRandomEnchantments(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        HolderLookup.Provider registries = level.registryAccess();
        Identifier bookId = Identifier.withDefaultNamespace("enchanted_book");
        ContractTemplates.Template template = ContractTemplates.all().stream()
                .filter(t -> t.item().equals(bookId) && t.randomEnchantments().isPresent()).findFirst().orElse(null);
        helper.assertTrue(template != null && template.hasTier(4), "random book is a tier 4 template");
        ContractTemplates.EnchantmentValues table = ContractTemplates.enchantmentValues();
        ContractTemplates.Template anyBook = ContractTemplates.all().stream()
                .filter(t -> t.item().equals(bookId) && t.match().isEmpty() && t.randomEnchantments().isEmpty()).findFirst().orElseThrow();
        helper.assertFalse(template.requirement(registries).equals(anyBook.requirement(registries)), "random book is not the any-book template");

        var lookup = registries.lookupOrThrow(Registries.ENCHANTMENT);
        RandomSource random = RandomSource.create(7);
        Set<String> seen = new HashSet<>();
        boolean belowMax = false;
        for (int i = 0; i < 60; i++) {
            Contract c = ContractBoard.create(template, 4, random, 0.0, registries).orElseThrow();
            ItemEnchantments wanted = c.displayStack(registries).getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            int rolls = template.randomEnchantments().get().count();
            helper.assertTrue(wanted.size() == rolls, "exactly " + rolls + " enchantments: " + wanted);
            List<Holder<Enchantment>> holders = new ArrayList<>(wanted.keySet());
            for (int a = 0; a < holders.size(); a++) {
                for (int b = a + 1; b < holders.size(); b++) {
                    helper.assertTrue(Enchantment.areCompatible(holders.get(a), holders.get(b)), "rolled enchantments are compatible");
                }
            }
            double value = 0;
            for (Holder<Enchantment> h : holders) {
                Identifier id = h.unwrapKey().orElseThrow().identifier();
                int max = h.value().getMaxLevel();
                int lvl = wanted.getLevel(h);
                helper.assertTrue(table.values().containsKey(id), id + " comes from the value table");
                helper.assertTrue(lvl == max || (max > 1 && lvl == max - 1), id + " level " + lvl + " is max or max-1");
                belowMax |= lvl < max;
                value += table.value(id, lvl, max);
                seen.add(id.toString());
            }
            helper.assertTrue(c.reward() == Math.round((template.rewardPerItem().orElseThrow() + value) * c.count()),
                    "reward = (base + values) x count");
            helper.assertTrue(template.produced(c, registries) && !anyBook.produced(c, registries), "offer is recognised as random book");

            // Genau die Bedingung, höher und mit Zusatz: erfüllt. Nur eine der beiden: nicht.
            ItemStack exact = new ItemStack(Items.ENCHANTED_BOOK);
            ItemStack better = new ItemStack(Items.ENCHANTED_BOOK);
            ItemStack half = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable e1 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable e2 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            ItemEnchantments.Mutable e3 = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            for (Holder<Enchantment> h : holders) {
                e1.set(h, wanted.getLevel(h));
                e2.set(h, wanted.getLevel(h) + 1);
            }
            e2.set(lookup.getOrThrow(Enchantments.VANISHING_CURSE), 1);
            e3.set(holders.get(0), 5);
            exact.set(DataComponents.STORED_ENCHANTMENTS, e1.toImmutable());
            better.set(DataComponents.STORED_ENCHANTMENTS, e2.toImmutable());
            half.set(DataComponents.STORED_ENCHANTMENTS, e3.toImmutable());
            helper.assertTrue(c.matches(exact) && c.matches(better), "exact and better books count");
            if (holders.size() > 1) {
                helper.assertFalse(c.matches(half), "a book with only one of them does not count");
            }
        }
        int spread = Math.min(8, table.values().size());
        helper.assertTrue(seen.size() >= spread, "the pool is actually used, at least " + spread + " different: " + seen);
        helper.assertTrue(belowMax, "levels below the maximum are rolled too");

        // Ausrüstung würfelt nur Verzauberungen, die aufs Item passen.
        Identifier bootsId = Identifier.withDefaultNamespace("diamond_boots");
        ContractTemplates.Template boots = ContractTemplates.all().stream()
                .filter(t -> t.item().equals(bootsId) && t.randomEnchantments().isPresent()).findFirst().orElseThrow();
        ItemStack bootsStack = new ItemStack(Items.DIAMOND_BOOTS);
        for (int i = 0; i < 40; i++) {
            Contract b = ContractBoard.create(boots, 4, random, 0.0, registries).orElseThrow();
            ItemEnchantments wanted = b.displayStack(registries).getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            helper.assertTrue(b.count() == 1 && wanted.size() == boots.randomEnchantments().get().count(), "boots: count 1, rolled enchantments " + wanted);
            for (Holder<Enchantment> h : wanted.keySet()) {
                helper.assertTrue(h.value().isSupportedItem(bootsStack), h + " fits on boots");
            }
            helper.assertTrue(boots.produced(b, registries), "offer is recognised as random boots");
        }

        // Gewicht: Die schwerste Vorlage kommt so viel häufiger wie ihr Gewicht verlangt, gemessen gegen die leichteste.
        ContractTemplates.Template bulk = ContractTemplates.all().stream()
                .max(Comparator.comparingInt(ContractTemplates.Template::weight)).orElseThrow();
        ContractTemplates.Template single = ContractTemplates.all().stream()
                .min(Comparator.comparingInt(ContractTemplates.Template::weight)).orElseThrow();
        helper.assertTrue(bulk.weight() > single.weight(), "templates have different weights");
        int bulkHits = 0;
        for (int i = 0; i < 900; i++) {
            if (ContractBoard.pickWeighted(List.of(bulk, single), random) == bulk) {
                bulkHits++;
            }
        }
        double share = (double) bulk.weight() / (bulk.weight() + single.weight());
        // Rund vier Standardabweichungen Spielraum bei 900 Würfen.
        helper.assertTrue(Math.abs(bulkHits / 900.0 - share) < 0.07,
                "weighted pick follows the weights " + bulk.weight() + ":" + single.weight() + ", got " + bulkHits + "/900");

        // Die gewürfelte Bedingung übersteht Speichern und Laden.
        Contract c = ContractBoard.create(template, 4, random, 0.0, registries).orElseThrow();
        var ops = registries.createSerializationContext(NbtOps.INSTANCE);
        Contract loaded = Contract.CODEC.parse(ops, Contract.CODEC.encodeStart(ops, c).getOrThrow()).getOrThrow();
        helper.assertTrue(loaded.requirement(registries).equals(c.requirement(registries)) && template.produced(loaded, registries),
                "rolled condition survives saving");
        helper.succeed();
    }

    /** Vorlagen mit Nether-Waren hängen erst aus, wenn der Nether offen ist. */
    private static void contractTemplateUnlock(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        WorldProgress progress = WorldProgress.get(server);
        ContractBoard board = ContractBoard.get(server);
        ContractTemplates.Template blaze = ContractTemplates.all().stream()
                .filter(t -> t.item().equals(Identifier.withDefaultNamespace("blaze_rod"))).findFirst().orElse(null);
        helper.assertTrue(blaze != null && blaze.unlock().dimension().filter(d -> d == Level.NETHER).isPresent(), "blaze rods need the nether");
        List<Identifier> netherItems = ContractTemplates.all().stream()
                .filter(t -> t.unlock().dimension().isPresent()).map(ContractTemplates.Template::item).toList();
        int oldAccess = progress.level(ServerUpgrade.NETHER_ACCESS);
        int oldTier = progress.level(ServerUpgrade.CONTRACT_TIER);
        try {
            progress.setLevel(ServerUpgrade.CONTRACT_TIER, Math.max(oldTier, 3 - DtpConfig.BASE_MAX_TIER.get()));
            if (DtpConfig.NETHER_LOCK.get()) {
                progress.setLevel(ServerUpgrade.NETHER_ACCESS, 0);
                helper.assertFalse(blaze.unlock().test(server), "locked before the nether");
                for (int i = 0; i < 100; i++) {
                    board.reroll(server);
                    helper.assertTrue(board.offers().stream().noneMatch(c -> netherItems.contains(c.item())), "no nether contract while locked");
                }
            }
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, 1);
            helper.assertTrue(blaze.unlock().test(server), "open after the nether");
            boolean seen = false;
            for (int i = 0; i < 300 && !seen; i++) {
                board.reroll(server);
                seen = board.offers().stream().anyMatch(c -> netherItems.contains(c.item()));
            }
            helper.assertTrue(seen, "nether contracts appear once the nether is open");
        } finally {
            progress.setLevel(ServerUpgrade.NETHER_ACCESS, oldAccess);
            progress.setLevel(ServerUpgrade.CONTRACT_TIER, oldTier);
            board.reroll(server);
        }
        helper.succeed();
    }

    /**
     * Zufallstrank (Stufe 3): immer verlängert oder verstärkt, als Trank oder Wurftrank; Wert aus den Zutaten, Menge
     * gedeckelt, Lohn = Wert × Multiplikator + Zuschlag. Gefordert wird genau der gewürfelte Trank.
     */
    private static void contractRandomPotions(GameTestHelper helper) {
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        ContractTemplates.Template template = ContractTemplates.all().stream()
                .filter(t -> t.randomPotion().isPresent()).findFirst().orElse(null);
        helper.assertTrue(template != null && template.hasTier(3), "random potion is a tier 3 template");
        helper.assertTrue(template.unlock().dimension().filter(d -> d == Level.NETHER).isPresent(), "potions need the nether");
        ContractTemplates.PotionValues table = ContractTemplates.potionValues();
        ContractTemplates.RandomPotion randomPotion = template.randomPotion().get();

        RandomSource random = RandomSource.create(11);
        boolean sawSplash = false;
        boolean sawPotion = false;
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            Contract c = ContractBoard.create(template, 3, random, 0.0, registries).orElseThrow();
            boolean splash = c.item().equals(ContractTemplates.SPLASH_POTION);
            helper.assertTrue(splash || c.item().equals(ContractTemplates.POTION), "potion or splash potion: " + c.item());
            sawSplash |= splash;
            sawPotion |= !splash;
            Holder<net.minecraft.world.item.alchemy.Potion> potion = Contract.requiredPotion(c.match()).orElseThrow();
            Identifier potionId = potion.unwrapKey().orElseThrow().identifier();
            var parsed = randomPotion.parse(potionId);
            helper.assertTrue(parsed.isPresent(), potionId + " is long_ or strong_ and comes from the table");
            seen.add(potionId.toString());
            double value = table.value(parsed.get().getKey(), parsed.get().getValue(), splash).orElseThrow();
            helper.assertTrue(c.count() >= 1 && c.count() <= table.maxCount(), "count is capped: " + c.count());
            helper.assertTrue(c.reward() == Math.max(1, Math.round(table.reward(value) * c.count())),
                    "reward = (value x multiplier + surcharge) x count for " + potionId);
            helper.assertTrue(template.produced(c, registries), "offer is recognised as random potion");

            // Genau dieser Trank zählt; der Grundtrank, die andere Form und ein umbenannter nicht.
            ItemStack brewed = PotionContents.createItemStack(splash ? Items.SPLASH_POTION : Items.POTION, potion);
            ItemStack otherForm = PotionContents.createItemStack(splash ? Items.POTION : Items.SPLASH_POTION, potion);
            ItemStack named = brewed.copy();
            named.set(DataComponents.CUSTOM_NAME, Component.literal("Mein Trank"));
            helper.assertTrue(c.matches(brewed), "the brewed potion counts: " + potionId);
            helper.assertFalse(c.matches(otherForm) || c.matches(named), "other form or renamed does not count");
            helper.assertTrue(c.displayStack(registries).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(potion),
                    "the board shows the potion");
            if (potionId.getPath().equals("long_swiftness")) {
                helper.assertTrue(c.count() >= table.cappedMin(), "cheap potions use the capped range: " + c.count());
            }
            helper.assertFalse(c.matches(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS))
                    || c.matches(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.SWIFTNESS)), "an unmodified potion never counts");
        }
        helper.assertTrue(sawSplash && sawPotion, "both forms are rolled");
        helper.assertTrue(seen.size() >= 15, "the table is actually used: " + seen);

        // Wertreihenfolge aus den Zutaten.
        Identifier regen = Identifier.withDefaultNamespace("regeneration");
        Identifier healing = Identifier.withDefaultNamespace("healing");
        Identifier swiftness = Identifier.withDefaultNamespace("swiftness");
        Identifier weakness = Identifier.withDefaultNamespace("weakness");
        Identifier poison = Identifier.withDefaultNamespace("poison");
        Identifier fire = Identifier.withDefaultNamespace("fire_resistance");
        double regenValue = table.value(regen, true, false).orElseThrow();
        double healingValue = table.value(healing, true, false).orElseThrow();
        double swiftValue = table.value(swiftness, true, false).orElseThrow();
        helper.assertTrue(regenValue > healingValue && healingValue > swiftValue, "regeneration > healing > swiftness");
        helper.assertTrue(table.value(weakness, false, false).orElseThrow() > table.value(poison, false, false).orElseThrow(),
                "weakness carries the fermented spider eye");
        helper.assertTrue(Math.abs(table.value(fire, false, false).orElseThrow() - ((9.24 + 56.6 + 90.5 + 12.49) / 3 + 2.5)) < 0.01,
                "fire resistance = wart + slime ball + half a blaze rod + redstone");
        helper.assertTrue(table.value(swiftness, false, true).orElseThrow() > table.value(swiftness, false, false).orElseThrow(),
                "splash potions are worth more");
        helper.succeed();
    }

    // --- Schwarzmarkthändler -----------------------------------------------------------------

    /** Die Spawnchance: 0 bis eine einzelne Kiste über der Schwelle liegt, dann wachsend mit allen Talern, gedeckelt. */
    private static void blackmarketChance(GameTestHelper helper) {
        long threshold = DtpConfig.TRADER_MIN_CHEST.get();
        double base = DtpConfig.TRADER_BASE_CHANCE.get();
        double exp = DtpConfig.TRADER_CHANCE_EXPONENT.get();
        double max = DtpConfig.TRADER_MAX_CHANCE.get();
        helper.assertTrue(BlackmarketSpawner.chance(threshold, threshold * 10) == 0.0, "exactly the threshold is not enough");
        helper.assertTrue(BlackmarketSpawner.chance(threshold - 1, threshold * 10) == 0.0, "a big pool of small chests is not enough");
        helper.assertTrue(BlackmarketSpawner.chance(Taler.SILVER_SACK.value, Taler.SILVER_SACK.value) == 0.0 || threshold < Taler.SILVER_SACK.value,
                "one silver sack (59,049) stays below the default threshold");
        double atThreshold = BlackmarketSpawner.chance(threshold + 1, threshold + 1);
        helper.assertTrue(Math.abs(atThreshold - Math.min(max, base * Math.pow((threshold + 1.0) / threshold, exp))) < 1e-9, "base chance: " + atThreshold);
        double fourfold = BlackmarketSpawner.chance(threshold + 1, threshold * 4);
        helper.assertTrue(Math.abs(fourfold - Math.min(max, base * Math.pow(4.0, exp))) < 1e-9, "more Taler, more chance: " + fourfold);
        helper.assertTrue(fourfold >= atThreshold, "the chance never falls with more money");
        helper.assertTrue(BlackmarketSpawner.chance(Long.MAX_VALUE / 2, Long.MAX_VALUE / 2) == max, "the cap holds");
        helper.succeed();
    }

    private static void fillChest(DepositChestBlockEntity chest, long amount) {
        List<ItemStack> stacks = Taler.split(amount);
        for (int i = 0; i < stacks.size(); i++) {
            chest.setItem(i, stacks.get(i));
        }
    }

    /** Freigeschaltet nur durch eine einzelne reiche Kiste; die Summe mehrerer Kisten reicht nicht. */
    private static void blackmarketLure(GameTestHelper helper) {
        BlockPos aRel = new BlockPos(1, 1, 0);
        BlockPos bRel = new BlockPos(2, 1, 0);
        helper.setBlock(aRel, ModBlocks.DEPOSIT_CHEST.get());
        helper.setBlock(bRel, ModBlocks.DEPOSIT_CHEST.get());
        DepositChestBlockEntity a = helper.getBlockEntity(aRel, DepositChestBlockEntity.class);
        DepositChestBlockEntity b = helper.getBlockEntity(bRel, DepositChestBlockEntity.class);
        ServerLevel level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(0, 1, 0));
        long threshold = DtpConfig.TRADER_MIN_CHEST.get();
        try {
            fillChest(a, threshold * 2 / 3);
            fillChest(b, threshold * 2 / 3);
            BlackmarketSpawner.Lure split = BlackmarketSpawner.lure(level, center);
            helper.assertTrue(split.pool() == threshold * 2 / 3 * 2 && split.chance() == 0.0, "two chests below the threshold: " + split);

            a.clearContent();
            fillChest(a, threshold + 1000);
            BlackmarketSpawner.Lure rich = BlackmarketSpawner.lure(level, center);
            helper.assertTrue(rich.maxChest() == threshold + 1000 && rich.chance() > 0.0, "one rich chest unlocks the trader: " + rich);
            helper.assertTrue(helper.absolutePos(aRel).equals(rich.richest()), "the trader heads for the richest chest");
            helper.assertTrue(rich.chance() == BlackmarketSpawner.chance(rich.maxChest(), rich.pool()), "all chests in range count for the chance");
        } finally {
            cleanup(helper, aRel, bRel);
        }
        helper.succeed();
    }

    /** Der Händler zieht seine Angebote aus den Datenpack-Sets und nimmt nur Silberbeutel. */
    private static void blackmarketOffers(GameTestHelper helper) {
        BlackmarketTrader trader = helper.spawn(ModEntities.BLACKMARKET_TRADER.get(), new BlockPos(1, 1, 1));
        try {
            List<MerchantOffer> offers = trader.getOffers();
            // Je Set so viele Angebote, wie seine Datei unter "amount" nennt: Kisten, Contraband, seltene Items.
            int wantedCrates = tradeSetAmount(helper.getLevel(), "crates");
            int wantedContraband = tradeSetAmount(helper.getLevel(), "contraband");
            int wantedRare = tradeSetAmount(helper.getLevel(), "rare");
            long crates = offers.stream().filter(o -> o.getResult().is(ModItems.CONTRABAND_CRATE.get())).count();
            long contraband = offers.stream().filter(o -> o.getResult().is(ContrabandItem.TAG)).count();
            helper.assertTrue(crates == wantedCrates, wantedCrates + " crate offers, got " + crates);
            helper.assertTrue(contraband == wantedContraband, wantedContraband + " contraband offers, got " + contraband);
            helper.assertTrue(offers.size() == crates + contraband + wantedRare,
                    "plus " + wantedRare + " rare offers, got " + offers.size() + " offers");
            for (MerchantOffer offer : offers) {
                helper.assertTrue(offer.getCostA().is(Taler.SILVER_POUCH.item()), "pays in silver pouches: " + offer.getCostA());
                helper.assertTrue(offer.getCostB().isEmpty() || offer.getCostB().is(Taler.SILVER_POUCH.item()), "second cost is pouches too");
            }
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.CONTRABAND_CRATE.get())), "sells contraband crates");
            trader.stayFor(100);
            helper.assertTrue(trader.expiresAt() == helper.getLevel().getGameTime() + 100, "fixed departure time");
        } finally {
            trader.discard();
        }
        helper.succeed();
    }

    /** Ofentempo-Faktor der Server-Upgrade-Stufe {@code stage} aus {@code minecraft:context_float_provider/cooking/speed_default}. */
    private static double furnaceSpeedFactor(ServerLevel level, int stage) {
        Identifier file = Identifier.withDefaultNamespace("context_float_provider/cooking/speed_default.json");
        try (Reader reader = level.getServer().getResourceManager().openAsReader(file)) {
            for (JsonElement input : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("inputs")) {
                JsonObject object = input.getAsJsonObject();
                if (object.has("upgrade") && object.get("upgrade").getAsString().equals("furnace_speed")) {
                    return object.getAsJsonArray("values").get(stage).getAsDouble();
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(file + " is unreadable", e);
        }
        throw new IllegalStateException(file + " has no furnace_speed input");
    }

    /** {@code amount} aus {@code data/dtp/trade_set/blackmarket/<set>.json}. */
    private static int tradeSetAmount(ServerLevel level, String set) {
        Identifier file = id("trade_set/blackmarket/" + set + ".json");
        try (Reader reader = level.getServer().getResourceManager().openAsReader(file)) {
            return JsonParser.parseReader(reader).getAsJsonObject().get("amount").getAsInt();
        } catch (Exception e) {
            throw new IllegalStateException(file + " has no readable amount", e);
        }
    }

    /**
     * Kein Angebot des Händlers ist billiger als das Doppelte dessen, was der Markt höchstens dafür zahlt.
     * Kisten werden mehrfach gewürfelt und jeder mögliche Inhalt geprüft.
     */
    private static void blackmarketNoArbitrage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlackmarketTrader trader = helper.spawn(ModEntities.BLACKMARKET_TRADER.get(), new BlockPos(1, 1, 1));
        try {
            LootContext context = new LootContext.Builder(new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, trader.position())
                    .withParameter(LootContextParams.THIS_ENTITY, trader)
                    .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
                    .create(LootContextParamSets.VILLAGER_TRADE)).create(java.util.Optional.empty());
            double factorMax = DtpConfig.FACTOR_MAX.get();
            int checked = 0;
            for (Holder.Reference<VillagerTrade> ref : level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).listElements().toList()) {
                Identifier id = ref.key().identifier();
                if (!id.getNamespace().equals(Dtp.MODID) || !id.getPath().startsWith("blackmarket/")) {
                    continue;
                }
                MerchantOffer offer = ref.value().getOffer(context);
                helper.assertTrue(offer != null, "offer " + id + " resolves");
                long price = Taler.valueOf(offer.getCostA()) + Taler.valueOf(offer.getCostB());
                List<ItemStack> goods = new ArrayList<>();
                if (offer.getResult().is(ModItems.CONTRABAND_CRATE.get())) {
                    for (int i = 0; i < 64; i++) {
                        goods.addAll(ContrabandCrateItem.roll(level, offer.getResult(), trader));
                    }
                    helper.assertTrue(!goods.isEmpty(), "crate " + id + " is never empty");
                } else {
                    goods.add(offer.getResult());
                }
                for (ItemStack good : goods) {
                    double market = TradeList.lookup(good).map(e -> e.base() * factorMax * good.getCount()).orElse(0.0);
                    helper.assertTrue(price >= 2 * market, id + ": " + price + " Taler for " + good + " which sells for up to " + market);
                }
                checked++;
            }
            helper.assertTrue(checked > 0, "black market offers were checked, got " + checked);
        } finally {
            trader.discard();
        }
        helper.succeed();
    }

    /** Eine Schmuggelkiste würfelt ihre Loot-Table, gibt den Inhalt an den Spieler und wird verbraucht. */
    private static void contrabandCrateOpen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ResourceKey<LootTable> saplings = ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(Dtp.MODID, "crate/sapling"));
        ItemStack crate = ContrabandCrateItem.of(new ItemStack(ModItems.CONTRABAND_CRATE.get()), saplings);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        for (int i = 0; i < 32; i++) {
            List<ItemStack> loot = ContrabandCrateItem.roll(level, crate, player);
            helper.assertTrue(loot.size() == 1 && (loot.getFirst().is(ItemTags.SAPLINGS) || loot.getFirst().is(Items.CRIMSON_FUNGUS)
                    || loot.getFirst().is(Items.WARPED_FUNGUS)), "a sapling crate holds one sapling: " + loot);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, crate.copyWithCount(2));
        crate.getItem().use(level, player, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1, "opening uses up one crate");
        boolean gotSapling = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            gotSapling |= stack.is(ItemTags.SAPLINGS) || stack.is(Items.CRIMSON_FUNGUS) || stack.is(Items.WARPED_FUNGUS);
        }
        helper.assertTrue(gotSapling, "the content lands in the inventory");
        helper.assertTrue(ContrabandCrateItem.roll(level, new ItemStack(ModItems.CONTRABAND_CRATE.get()), player).isEmpty(),
                "a crate without loot table is empty");
        helper.succeed();
    }

    // --- Schwarzmarktfunde in Strukturkisten -------------------------------------------------------

    /** Contraband oder einer der Verbrauchsgegenstände des Händlers: alles, was in der Fundliste stehen darf. */
    private static boolean isBlackmarketFind(ItemStack stack) {
        return stack.is(ContrabandItem.TAG) || stack.is(ModItems.RAIN_CATALYST.get()) || stack.is(ModItems.AMETHYST_CATALYST.get())
                || stack.is(ModItems.RETURN_SCROLL.get());
    }

    private static LootParams chestParams(ServerLevel level, Vec3 origin, Player opener) {
        return new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                .withParameter(LootContextParams.THIS_ENTITY, opener).create(LootContextParamSets.CHEST);
    }

    private static LootTable lootTable(ServerLevel level, String id) {
        return level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(id)));
    }

    /** Die Fundliste ist geladen, besteht die Vanilla-Validierung und liefert nur Contraband und Verbrauchsgegenstände. */
    private static void structureFindsTable(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable finds = level.getServer().reloadableRegistries().getLootTable(StructureFindsModifier.TABLE);
        helper.assertTrue(finds != LootTable.EMPTY, "gameplay/blackmarket_finds.json wurde nicht geladen");
        ProblemReporter.Collector problems = new ProblemReporter.Collector();
        finds.validate(new ValidationContext(problems, LootContextParamSets.CHEST, level.getServer().reloadableRegistries().lookup()));
        List<String> errors = new ArrayList<>();
        problems.forEach((path, problem) -> errors.add("Validierung " + path + ": " + problem.description()));
        Player opener = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        Set<Item> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            for (ItemStack stack : finds.getRandomItems(chestParams(level, origin, opener))) {
                seen.add(stack.getItem());
                if (!isBlackmarketFind(stack)) {
                    errors.add("Fundliste liefert " + stack + ", erlaubt sind nur Contraband und Verbrauchsgegenstände");
                }
            }
        }
        helper.assertTrue(errors.isEmpty(), String.join("\n", errors));
        helper.assertTrue(seen.contains(ModItems.GUN.get()) && seen.contains(ModItems.RETURN_SCROLL.get()),
                "In 1000 Würfen sollten Gun und Rückkehrrolle vorkommen: " + seen);
        helper.succeed();
    }

    /**
     * Strukturkisten bekommen in rund 20 % der Fälle einen Fund, Contraband mit 25–75 % Ladung. Dörfer, Bonustruhe und
     * Tresore bleiben Vanilla.
     */
    private static void structureFindsChest(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player opener = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        List<String> errors = new ArrayList<>();

        // Chance und Ladungsspanne aus der Modifier-Datei.
        JsonObject modifier;
        Identifier modifierFile = id("loot_modifiers/structure_finds.json");
        try (Reader reader = level.getServer().getResourceManager().openAsReader(modifierFile)) {
            modifier = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException(modifierFile + " is unreadable", e);
        }
        double chance = modifier.get("chance").getAsDouble();
        double minFraction = modifier.get("min_charge_fraction").getAsDouble();
        double maxFraction = modifier.get("max_charge_fraction").getAsDouble();

        LootTable dungeon = lootTable(level, "minecraft:chests/simple_dungeon");
        int rolls = 2000;
        int withFind = 0;
        for (int i = 0; i < rolls; i++) {
            boolean found = false;
            for (ItemStack stack : dungeon.getRandomItems(chestParams(level, origin, opener))) {
                if (!isBlackmarketFind(stack)) {
                    continue;
                }
                found = true;
                if (stack.getItem() instanceof ContrabandItem item) {
                    int max = ContrabandDefinitions.get(item).maxCharges();
                    int charges = ContrabandItem.charges(stack).charges();
                    if (charges < Mth.floor(minFraction * max) || charges > Mth.floor(maxFraction * max)) {
                        errors.add(stack + " hat " + charges + " von " + max + " Ladungen, erwartet " + minFraction + "–" + maxFraction);
                    }
                }
            }
            if (found) {
                withFind++;
            }
        }
        // Rund 4,5 Standardabweichungen Spielraum (bei 20 % Chance ±4 Prozentpunkte).
        double tolerance = Math.max(0.01, 4.5 * Math.sqrt(chance * (1 - chance) / rolls));
        helper.assertTrue(Math.abs((double) withFind / rolls - chance) <= tolerance,
                "Funde in " + withFind + " von " + rolls + " Dungeon-Kisten, erwartet etwa " + chance);

        for (String id : List.of("minecraft:chests/village/village_plains_house", "minecraft:chests/spawn_bonus_chest",
                "minecraft:chests/trial_chambers/reward", "minecraft:chests/jungle_temple_dispenser")) {
            LootTable table = lootTable(level, id);
            for (int i = 0; i < 300; i++) {
                for (ItemStack stack : table.getRandomItems(chestParams(level, origin, opener))) {
                    if (isBlackmarketFind(stack)) {
                        errors.add(id + " liefert " + stack + ", ist aber ausgenommen");
                    }
                }
            }
        }
        helper.assertTrue(errors.isEmpty(), String.join("\n", errors.subList(0, Math.min(errors.size(), 10))));
        helper.succeed();
    }

    /**
     * Taler in Strukturkisten: Chance und Spanne je Stufe, nie mehr als ein Silberbeutel, der Durchschnitt steigt von
     * Dorf bis End. Die Zahlen spiegeln {@code data/dtp/loot_modifiers/structure_taler.json}; wer dort ändert, passt sie
     * hier an.
     */
    private static void structureTaler(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player opener = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 origin = Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO));
        record Expect(String table, double chance, long min, long max) {
        }
        List<Expect> tiers = List.of(
                new Expect("minecraft:chests/village/village_plains_house", 0.4, 9, 243),
                new Expect("minecraft:chests/simple_dungeon", 0.5, 81, 729),
                new Expect("minecraft:chests/desert_pyramid", 0.6, 243, 2187),
                new Expect("minecraft:chests/nether_bridge", 0.7, 729, 4374),
                new Expect("minecraft:chests/end_city_treasure", 0.8, 2187, 6561));
        List<String> errors = new ArrayList<>();
        int rolls = 1500;
        double lastAverage = -1;
        for (Expect tier : tiers) {
            LootTable table = lootTable(level, tier.table());
            int hits = 0;
            long total = 0;
            for (int i = 0; i < rolls; i++) {
                long amount = 0;
                for (ItemStack stack : table.getRandomItems(chestParams(level, origin, opener))) {
                    amount += Taler.valueOf(stack);
                }
                if (amount == 0) {
                    continue;
                }
                hits++;
                total += amount;
                if (amount < tier.min() || amount > tier.max() || amount > StructureTalerModifier.MAX_AMOUNT) {
                    errors.add(tier.table() + ": " + amount + " Taler, erwartet " + tier.min() + "–" + tier.max());
                }
            }
            double share = (double) hits / rolls;
            if (Math.abs(share - tier.chance()) > 0.05) {
                errors.add(tier.table() + ": Taler in " + hits + " von " + rolls + " Kisten, erwartet etwa " + tier.chance());
            }
            double average = (double) total / rolls;
            if (average <= lastAverage) {
                errors.add(tier.table() + ": Durchschnitt " + Math.round(average) + " nicht höher als die Stufe davor ("
                        + Math.round(lastAverage) + ")");
            }
            lastAverage = average;
        }
        for (String id : List.of("minecraft:chests/spawn_bonus_chest", "minecraft:chests/trial_chambers/reward")) {
            LootTable table = lootTable(level, id);
            for (int i = 0; i < 300; i++) {
                for (ItemStack stack : table.getRandomItems(chestParams(level, origin, opener))) {
                    if (Taler.isTaler(stack)) {
                        errors.add(id + " liefert " + stack + ", ist aber ausgenommen");
                    }
                }
            }
        }
        helper.assertTrue(errors.isEmpty(), String.join("\n", errors.subList(0, Math.min(errors.size(), 10))));
        helper.succeed();
    }

    // --- Contraband-Items -------------------------------------------------------------------------

    private static ItemStack extinguisher(int charges) {
        return ContrabandItem.create(ModItems.FIRE_EXTINGUISHER.get(), charges);
    }

    private static void placeFire(GameTestHelper helper, BlockPos rel) {
        helper.setBlock(rel.below(), Blocks.NETHERRACK);
        helper.setBlock(rel, Blocks.FIRE);
    }

    /** Benutzung: kostet eine Ladung und startet den Cooldown; ohne Feuer oder ohne Ladung kostet nichts und nichts bricht. */
    private static void contrabandUse(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        FireExtinguisherItem item = ModItems.FIRE_EXTINGUISHER.get();
        helper.assertTrue(ContrabandDefinitions.loaded(item), "the extinguisher has a datapack definition");
        int max = item.definition().maxCharges();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.setPos(at.x, at.y, at.z);
        BlockPos fire = new BlockPos(2, 2, 1);
        try {
            ItemStack stack = extinguisher(max);
            placeFire(helper, fire);
            helper.assertTrue(item.tryUse(level, player, stack), "extinguishes the fire");
            helper.assertBlockPresent(Blocks.AIR, fire);
            helper.assertTrue(ContrabandItem.charges(stack).charges() == max - 1, "one charge used: " + ContrabandItem.charges(stack));
            helper.assertTrue(player.getCooldowns().isOnCooldown(stack), "cooldown started");
            placeFire(helper, fire);
            helper.assertFalse(item.tryUse(level, player, stack), "blocked during the cooldown");
            helper.assertBlockPresent(Blocks.FIRE, fire);

            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
            helper.setBlock(fire, Blocks.AIR);
            helper.assertFalse(item.tryUse(level, player, stack), "nothing burning, nothing used");
            helper.assertTrue(ContrabandItem.charges(stack).charges() == max - 1, "a refused use costs nothing");

            ItemStack empty = extinguisher(0);
            placeFire(helper, fire);
            helper.assertFalse(item.tryUse(level, player, empty), "an empty extinguisher does nothing");
            helper.assertTrue(empty.getCount() == 1 && empty.is(item), "an empty item does not break");
            helper.assertBlockPresent(Blocks.FIRE, fire);

            ItemStack burning = extinguisher(max);
            player.igniteForSeconds(5);
            helper.assertTrue(item.tryUse(level, player, burning) && !player.isOnFire(), "puts out a burning player");
        } finally {
            helper.setBlock(fire, Blocks.AIR);
            helper.setBlock(fire.below(), Blocks.AIR);
        }
        helper.succeed();
    }

    private static CraftingInput grid(ItemStack... stacks) {
        List<ItemStack> items = new ArrayList<>(List.of(stacks));
        while (items.size() < 9) {
            items.add(ItemStack.EMPTY);
        }
        return CraftingInput.of(3, 3, items);
    }

    private static ItemStack coins(Taler taler) {
        return new ItemStack(taler.item());
    }

    /**
     * Contraband lädt an der Reparaturbank auf: Ladungen = gezahlt / Preis bis zum Höchstwert, zu viel Gezahltes kommt
     * als Wechselgeld zurück. Voll oder ohne Kaufkraft passiert nichts; das alte Crafting-Rezept gibt es nicht mehr.
     */
    private static void contrabandRecharge(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ContrabandDefinition def = ModItems.FIRE_EXTINGUISHER.get().definition();
        int max = def.maxCharges();
        long price = def.chargePrice();

        // Rechenregel mit festen Testwerten (10 von 16 Ladungen zu je 250 Taler, es fehlen 6), unabhängig vom Datapack.
        RepairStation.Quote two = RepairStation.quote(6, 250_000, 729, 1);
        helper.assertTrue(two.points() == 2 && two.cost() == 500 && two.change() == 229 && !two.full(),
                "729 taler buy two charges, 229 back: " + two);
        RepairStation.Quote fill = RepairStation.quote(6, 250_000, 729, 4);
        helper.assertTrue(fill.points() == 6 && fill.coins() == 3 && fill.cost() == 1500 && fill.change() == 687 && fill.full(),
                "three of four silver taler fill the last 6 charges, 687 back: " + fill);
        helper.assertTrue(RepairStation.quote(6, 250_000, 81, 3).equals(RepairStation.Quote.NONE), "243 taler buy no charge");

        // Werte aus dem Datapack.
        Taler oneCharge = Arrays.stream(Taler.values()).filter(t -> t.value >= price).findFirst().orElseThrow();
        helper.assertTrue(RepairStation.accepts(extinguisher(0)), "contraband fits into the item slot");
        helper.assertTrue(RepairStation.missing(extinguisher(3)) == max - 3, "missing charges count as units");
        helper.assertTrue(RepairStation.unitPriceMilli(extinguisher(0)) == price * 1000, "one charge costs the datapack price");
        helper.assertTrue(RepairStation.fullPrice(extinguisher(0)) == max * price, "a full recharge costs max x price");
        helper.assertTrue(RepairStation.check(extinguisher(max), coins(oneCharge)) == RepairStation.Blocked.FULLY_CHARGED,
                "a full item takes no taler");
        Arrays.stream(Taler.values()).filter(t -> t.value < price).findFirst().ifPresent(cheap -> helper.assertTrue(
                RepairStation.check(extinguisher(0), coins(cheap)) == RepairStation.Blocked.COIN_TOO_SMALL, "too little for one charge"));

        // Am Block: Ladungen, verbrauchte Münzen, Wechselgeld im Inventar.
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        SimpleContainer one = rechargeSlots(extinguisher(0), oneCharge, 1);
        RepairStation.repair(level.getServer(), player, one);
        int bought = (int) Math.min(max, oneCharge.value / price);
        long change = oneCharge.value - bought * price;
        helper.assertTrue(ContrabandItem.charges(one.getItem(RepairStation.SLOT_ITEM)).charges() == bought,
                oneCharge + " buys " + bought + " charges: " + ContrabandItem.charges(one.getItem(RepairStation.SLOT_ITEM)));
        helper.assertTrue(one.getItem(RepairStation.SLOT_PAYMENT).isEmpty(), "the coin is spent");
        helper.assertTrue(talerInInventory(player) == change, change + " taler change: " + talerInInventory(player));

        Taler fills = Arrays.stream(Taler.values()).filter(t -> t.value >= max * price).findFirst().orElseThrow();
        Player other = helper.makeMockPlayer(GameType.SURVIVAL);
        SimpleContainer big = rechargeSlots(extinguisher(0), fills, 2);
        RepairStation.repair(level.getServer(), other, big);
        helper.assertTrue(ContrabandItem.charges(big.getItem(RepairStation.SLOT_ITEM)).charges() == max, "one big coin fills the item");
        helper.assertTrue(big.getItem(RepairStation.SLOT_PAYMENT).getCount() == 1, "the second coin stays in the slot");
        helper.assertTrue(talerInInventory(other) == fills.value - max * price, "the rest comes back as change: " + talerInInventory(other));

        RepairStation.repair(level.getServer(), other, big);
        helper.assertTrue(big.getItem(RepairStation.SLOT_PAYMENT).getCount() == 1, "a full item costs nothing");

        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, grid(extinguisher(0), coins(oneCharge)), level).isEmpty(),
                "the crafting grid no longer recharges");
        helper.succeed();
    }

    private static SimpleContainer rechargeSlots(ItemStack item, Taler coin, int count) {
        SimpleContainer slots = new SimpleContainer(2);
        slots.setItem(RepairStation.SLOT_ITEM, item);
        slots.setItem(RepairStation.SLOT_PAYMENT, new ItemStack(coin.item(), count));
        return slots;
    }

    /** Ladungen sind keine Haltbarkeit: keine Aura, keine Verzauberung, kein Verkauf. */
    private static void contrabandProtection(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack stack = extinguisher(3);
        helper.assertTrue(stack.is(ContrabandItem.TAG), "tagged as contraband");
        helper.assertFalse(stack.isDamageableItem(), "no durability");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getInventory().setItem(20, stack);
        BeaconLogic.mend(player, BeaconLogic.mendingAuraRepair(1));
        helper.assertTrue(ContrabandItem.charges(player.getInventory().getItem(20)).charges() == 3, "the mending aura does not recharge");

        ItemStack book = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.UNBREAKING, 3);
        helper.assertTrue(anvil(player, stack, book, null).isEmpty(), "no enchanted books on the anvil");
        ItemStack forced = enchanted(level, ModItems.FIRE_EXTINGUISHER.get(), false, Enchantments.EFFICIENCY, 5);
        helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(enchantment(level, Enchantments.EFFICIENCY), forced) == 0,
                "enchantments forced onto it have no effect");

        helper.assertTrue(TradeList.lookup(stack).isEmpty(), "not sellable on the market");
        helper.succeed();
    }

    // --- Contraband-Items und Verbrauchsgegenstände ---------------------------------------------

    private static UseOnContext clickBlock(GameTestHelper helper, Player player, ItemStack stack, BlockPos rel, Direction face) {
        BlockPos abs = helper.absolutePos(rel);
        Vec3 at = Vec3.atCenterOf(abs).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        return new UseOnContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack, new BlockHitResult(at, face, abs, false));
    }

    /** Amethyst-Katalysator: nur ein Amethystblock wird knospend, nur dann ist ein Stück weg. */
    private static void suppliesAmethystCatalyst(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.AMETHYST_CATALYST.get(), 3);
        BlockPos block = new BlockPos(1, 1, 1);
        helper.setBlock(block, Blocks.STONE);
        ModItems.AMETHYST_CATALYST.get().useOn(clickBlock(helper, player, stack, block, Direction.UP));
        helper.assertBlockPresent(Blocks.STONE, block);
        helper.assertTrue(stack.getCount() == 3, "stone stays stone and costs nothing");
        helper.setBlock(block, Blocks.AMETHYST_BLOCK);
        ModItems.AMETHYST_CATALYST.get().useOn(clickBlock(helper, player, stack, block, Direction.UP));
        helper.assertBlockPresent(Blocks.BUDDING_AMETHYST, block);
        helper.assertTrue(stack.getCount() == 2, "one catalyst used");
        helper.assertFalse(stack.is(ContrabandItem.TAG), "supplies are no contraband");
        helper.setBlock(block, Blocks.AIR);
        helper.succeed();
    }

    /** Regen-Katalysator: klar → Regen → Gewitter → „schlimmer wird es nicht“, jeweils mindestens 10 Minuten. */
    private static void suppliesRainCatalyst(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var weather = level.getWeatherData();
        int clear = weather.getClearWeatherTime(), rain = weather.getRainTime(), thunder = weather.getThunderTime();
        boolean raining = weather.isRaining(), thundering = weather.isThundering();
        try {
            weather.setRaining(false);
            weather.setThundering(false);
            weather.setRainTime(100);
            weather.setThunderTime(100);
            helper.assertTrue(RainCatalystItem.apply(level) instanceof UseOutcome.Used, "clear sky turns to rain");
            helper.assertTrue(weather.isRaining() && !weather.isThundering() && weather.getRainTime() >= RainCatalystItem.MIN_TICKS,
                    "rain for at least 10 minutes");
            helper.assertTrue(RainCatalystItem.apply(level) instanceof UseOutcome.Used, "rain turns to thunder");
            helper.assertTrue(weather.isThundering() && weather.getThunderTime() >= RainCatalystItem.MIN_TICKS, "thunder for 10 minutes");
            helper.assertTrue(RainCatalystItem.apply(level) instanceof UseOutcome.Refused, "a fresh thunderstorm cannot get worse");
            weather.setThunderTime(200);
            helper.assertTrue(RainCatalystItem.apply(level) instanceof UseOutcome.Used
                    && weather.getThunderTime() >= RainCatalystItem.MIN_TICKS, "a short thunderstorm is extended");
        } finally {
            weather.setClearWeatherTime(clear);
            weather.setRainTime(rain);
            weather.setThunderTime(thunder);
            weather.setRaining(raining);
            weather.setThundering(thundering);
        }
        helper.succeed();
    }

    /** Rückkehrrolle: landet auf einem freien Platz über dem Hauptbeacon. */
    private static void suppliesReturnScroll(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        withTerritory(helper, () -> {
            BlockPos target = ReturnScrollItem.target(level.getServer());
            helper.assertTrue(target != null && level.isEmptyBlock(target) && level.isEmptyBlock(target.above()), "free spot above the beacon");
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            Vec3 start = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)));
            player.setPos(start.x, start.y, start.z);
            helper.assertTrue(ReturnScrollItem.apply(level.getServer(), player) instanceof UseOutcome.Used, "teleport works");
            helper.assertTrue(player.blockPosition().equals(target), "lands on the beacon: " + player.blockPosition() + " vs " + target);
        });
        helper.succeed();
    }

    /** Wachglocke: Monster leuchten, beim Schleichen Tiere; der Rest nicht. */
    private static void contrabandWatchBell(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.setPos(at.x, at.y, at.z);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(3, 1, 1));
        var cow = helper.spawn(EntityTypes.COW, new BlockPos(1, 1, 3));
        // Tiere und Monster früherer Testläufe auf benachbarten Testflächen würden mitgezählt. Der Test läuft
        // isoliert, das Abräumen trifft also keinen laufenden Test.
        level.getEntitiesOfClass(LivingEntity.class,
                new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(9),
                e -> e != zombie && e != cow && !(e instanceof Player)).forEach(e -> e.discard());
        try {
            helper.assertTrue(WatchBellItem.ring(level, player, 8, 100, false) == 1, "one monster");
            helper.assertTrue(zombie.hasEffect(MobEffects.GLOWING) && !cow.hasEffect(MobEffects.GLOWING), "only the zombie glows");
            helper.assertTrue(WatchBellItem.ring(level, player, 8, 100, true) == 1 && cow.hasEffect(MobEffects.GLOWING), "sneaking: the cow");
        } finally {
            zombie.discard();
            cow.discard();
        }
        helper.succeed();
    }

    /** Schleimrute: Himmelsrichtungen und der nächste Slime-Chunk nach der Vanilla-Formel. */
    private static void contrabandOozingRod(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(BlockPos.ZERO);
        ChunkPos here = ChunkPos.containing(at);
        var found = OozingRodItem.slimeChunksAround(level, here, 8);
        helper.assertFalse(found.isEmpty(), "slime chunks within 8 chunks");
        helper.assertFalse(found.contains(here), "the own chunk is not a target");
        long seed = level.getSeed();
        for (ChunkPos c : found) {
            boolean vanilla = net.minecraft.world.level.levelgen.WorldgenRandom.seedSlimeChunk(c.x(), c.z(), seed, 987234911L).nextInt(10) == 0;
            helper.assertTrue(vanilla && OozingRodItem.isSlimeChunk(level, c.x(), c.z()), "same result as the vanilla slime rule: " + c);
        }
        double near = OozingRodItem.sparkSpeed(0), mid = OozingRodItem.sparkSpeed(50), far = OozingRodItem.sparkSpeed(1000);
        helper.assertTrue(near > 0 && near < mid && mid < far && far < 0.5, "spark range grows with distance and stays bounded");
        OozingRodItem rod = ModItems.OOZING_ROD.get();
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(
                net.minecraft.resources.Identifier.fromNamespaceAndPath("dtp", "dowsing_rod")) == rod, "old ID dtp:dowsing_rod still resolves");
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
        ItemStack stack = ContrabandItem.create(rod, 1);
        helper.assertTrue(rod.tryUse(level, player, stack), "the rod works for a player without connection");
        helper.assertTrue(ContrabandItem.charges(stack).charges() == 0, "one charge used");
        helper.succeed();
    }

    /** Spawnstab: Stein im Dunkeln ist gefährlich, Glas nicht. */
    private static void contrabandSpawnWand(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos stone = new BlockPos(1, 1, 1);
        BlockPos glass = new BlockPos(3, 1, 1);
        helper.setBlock(stone, Blocks.STONE);
        helper.setBlock(glass, Blocks.GLASS);
        try {
            var spots = SpawnWandItem.spawnSpots(level, helper.absolutePos(new BlockPos(2, 2, 1)), 3, 1000);
            helper.assertTrue(spots.contains(helper.absolutePos(stone.above())), "above dark stone a zombie could spawn");
            helper.assertFalse(spots.contains(helper.absolutePos(glass.above())), "not on glass");
            helper.assertTrue(SpawnWandItem.spawnSpots(level, helper.absolutePos(new BlockPos(2, 2, 1)), 3, 1).size() == 1, "capped");
        } finally {
            helper.setBlock(stone, Blocks.AIR);
            helper.setBlock(glass, Blocks.AIR);
        }
        helper.succeed();
    }

    /** Taschengeld-Beutel: Dorfbewohner bekommen den Spieler als Laufziel, ohne Dorfbewohner kostet er nichts. */
    private static void contrabandPocketMoneyPouch(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.setPos(at.x, at.y, at.z);
        PocketMoneyPouchItem pouch = ModItems.POCKET_MONEY_POUCH.get();
        ItemStack stack = ContrabandItem.create(pouch, 3);
        helper.assertTrue(PocketMoneyPouchItem.villagers(level, player, 8).isEmpty(), "nobody here yet");
        helper.assertFalse(pouch.tryUse(level, player, stack), "nobody to lure");
        helper.assertTrue(ContrabandItem.charges(stack).charges() == 3, "costs nothing");
        var villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(4, 1, 4));
        try {
            helper.assertTrue(pouch.tryUse(level, player, stack) && ContrabandItem.charges(stack).charges() == 2, "lures and costs a charge");
            helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isPresent(), "the villager walks to the player");
            helper.assertTrue(PocketMoneyPouchItem.isLured(villager), "the villager stays lured");
            // Ein Spaziergang oder Weg zum Arbeitsplatz überschreibt das Ziel: Weiter weg wird es ersetzt ...
            BlockPos elsewhere = helper.absolutePos(new BlockPos(6, 1, 6));
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(elsewhere, 0.5F, 1));
            helper.assertTrue(PocketMoneyPouchItem.enforce(villager), "still lured");
            var target = villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).orElseThrow().getTarget();
            helper.assertTrue(target instanceof EntityTracker tracker && tracker.getEntity() == player, "a foreign walk target is replaced");
            // ... in der Nähe des Spielers gelöscht: Er bleibt stehen.
            villager.setPos(at.x + 1.5, at.y, at.z);
            villager.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(elsewhere, 0.5F, 1));
            PocketMoneyPouchItem.enforce(villager);
            helper.assertTrue(villager.getBrain().getMemory(MemoryModuleType.WALK_TARGET).isEmpty(), "close to the player a stroll is cancelled");
            villager.discard();
            helper.assertFalse(PocketMoneyPouchItem.enforce(villager) || PocketMoneyPouchItem.isLured(villager), "ends when the villager is gone");
        } finally {
            villager.discard();
        }
        helper.succeed();
    }

    /** Gun: trifft das erste Wesen in Blickrichtung, nicht durch Wände. */
    private static void contrabandGun(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 at = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 1, 1)));
        player.setPos(at.x, at.y, at.z);
        var zombie = helper.spawn(EntityTypes.ZOMBIE, new BlockPos(1, 1, 5));
        zombie.setNoAi(true);
        BlockPos wall = new BlockPos(1, 2, 3);
        try {
            float health = zombie.getHealth();
            var hit = GunItem.shoot(level, player, player.getEyePosition(), new Vec3(0, 0, 1), 16, 5.0F);
            // Rüstung zählt (der Zombie hat natürliche), Verzauberungen nicht.
            helper.assertTrue(hit == zombie && zombie.getHealth() < health, "hits the zombie: " + hit + ", health " + zombie.getHealth());
            helper.setBlock(wall, Blocks.STONE);
            helper.assertTrue(GunItem.shoot(level, player, player.getEyePosition(), new Vec3(0, 0, 1), 16, 5.0F) == null, "not through stone");
            helper.assertTrue(GunItem.shoot(level, player, player.getEyePosition(), new Vec3(0, 0, -1), 16, 5.0F) == null, "nothing behind");
        } finally {
            zombie.discard();
            helper.setBlock(wall, Blocks.AIR);
        }
        helper.succeed();
    }

    /** Endlose Fackel: setzt Boden- und Wandfackeln, jede kostet eine Ladung, das Item bleibt. */
    private static void contrabandEndlessTorch(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EndlessTorchItem torch = ModItems.ENDLESS_TORCH.get();
        ItemStack stack = ContrabandItem.create(torch, 2);
        BlockPos stone = new BlockPos(2, 1, 2);
        helper.setBlock(stone, Blocks.STONE);
        try {
            torch.useOn(clickBlock(helper, player, stack, stone, Direction.UP));
            helper.assertBlockPresent(Blocks.TORCH, stone.above());
            helper.assertTrue(ContrabandItem.charges(stack).charges() == 1, "one charge per torch");
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
            torch.useOn(clickBlock(helper, player, stack, stone, Direction.EAST));
            helper.assertBlockPresent(Blocks.WALL_TORCH, stone.east());
            player.getCooldowns().removeCooldown(player.getCooldowns().getCooldownGroup(stack));
            helper.assertTrue(stack.getCount() == 1 && ContrabandItem.charges(stack).charges() == 0, "empty but still there");
            torch.useOn(clickBlock(helper, player, stack, stone, Direction.WEST));
            helper.assertBlockPresent(Blocks.AIR, stone.west());
        } finally {
            helper.setBlock(stone.above(), Blocks.AIR);
            helper.setBlock(stone.east(), Blocks.AIR);
            helper.setBlock(stone, Blocks.AIR);
        }
        helper.succeed();
    }

    // --- Einstieg: Beacon-Rezept, Handbuch, Grundrezepte ------------------------------------

    private static CraftingInput grid(List<String> ids) {
        return CraftingInput.of(3, 3, ids.stream()
                .map(id -> id.isEmpty() ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))))
                .toList());
    }

    /** Der Hauptbeacon ist craftbar, solange keiner steht — auch ein unbestätigter zählt schon als gesetzt. */
    private static void beaconRecipe(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldProgress progress = WorldProgress.get(level.getServer());
        BlockPos oldBeacon = progress.beaconPos();
        boolean oldStarted = progress.started();
        CraftingInput input = grid(GuideGrids.BEACON.slots());
        try {
            progress.setBeacon(null, false);
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                    .map(r -> r.value().assemble(input).is(ModItems.BEACON.get())).orElse(false),
                    "without a beacon the recipe yields the main beacon");
            progress.setBeacon(helper.absolutePos(BlockPos.ZERO), false);
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).isEmpty(),
                    "a placed, unconfirmed beacon already blocks the recipe");
            progress.setBeacon(helper.absolutePos(BlockPos.ZERO), true);
            helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).isEmpty(),
                    "after the start there is no second beacon");
        } finally {
            progress.setBeacon(oldBeacon, oldStarted);
        }
        helper.succeed();
    }

    /** Jedes Raster im Handbuch ergibt am Server genau das gezeigte Ergebnis — Schutz gegen veraltete Seiten. */
    private static void guideGrids(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        WorldProgress progress = WorldProgress.get(level.getServer());
        BlockPos oldBeacon = progress.beaconPos();
        boolean oldStarted = progress.started();
        int oldInfo = progress.level(ServerUpgrade.MARKET_INFO);
        try {
            // Beide Bedingungen erfüllt: noch kein Beacon (Beacon-Rezept) und Marktinformation gekauft (Marktwaage).
            progress.setBeacon(null, false);
            progress.setLevel(ServerUpgrade.MARKET_INFO, 1);
            for (GuideGrids.Grid g : GuideGrids.all()) {
                CraftingInput input = grid(g.slots());
                ItemStack result = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                        .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
                helper.assertTrue(result.is(BuiltInRegistries.ITEM.getValue(Identifier.parse(g.result()))) && result.getCount() == g.count(),
                        "guide grid for " + g.result() + " does not match its recipe, got " + result);
            }
        } finally {
            progress.setBeacon(oldBeacon, oldStarted);
            progress.setLevel(ServerUpgrade.MARKET_INFO, oldInfo);
        }
        helper.succeed();
    }

    /** Erstes Betreten: Handbuch und Grundrezepte genau einmal; Neben-Beacon, Marktwaage und Schwarzmarkt bleiben offen. */
    private static void onboarding(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        helper.assertTrue(Onboarding.welcome(player), "a new player is welcomed");
        helper.assertTrue(player.getInventory().countItem(ModItems.GUIDE.get()) == 1, "the new player holds the handbook");
        for (String recipe : List.of("beacon", "market_stand", "trade_chest", "deposit_chest", "bulletin_board", "upgrade_table", "repair_station", "guide",
                "copper_pouch_from_copper_taler", "gold_taler_from_silver_sack")) {
            helper.assertTrue(player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE, id(recipe))), "recipe unlocked: " + recipe);
        }
        for (String recipe : List.of("relay_beacon", "market_scale", "blackmarket", "slot_machine", "statistics_board", "recycler")) {
            helper.assertFalse(player.getRecipeBook().contains(ResourceKey.create(Registries.RECIPE, id(recipe))),
                    "left to discover: " + recipe);
        }
        helper.assertFalse(Onboarding.welcome(player), "the second login gives nothing");
        helper.assertTrue(player.getInventory().countItem(ModItems.GUIDE.get()) == 1, "still exactly one handbook");
        helper.succeed();
    }

    private static String classpathText(GameTestHelper helper, String path) {
        try (InputStream in = DtpGameTests.class.getResourceAsStream(path)) {
            helper.assertTrue(in != null, "missing on the classpath: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Die Handbuch-Dateien beider Sprachen lassen sich fehlerfrei lesen und haben denselben Aufbau: gleiche Seiten-IDs in
     * gleicher Reihenfolge und Kapitelzuordnung, gleiche Raster und Hervorhebungen. Die Oberflächen-Texte stehen in
     * beiden Sprachdateien.
     */
    private static void guideFiles(GameTestHelper helper) {
        List<String> structures = new ArrayList<>();
        for (String lang : List.of("de_de", "en_us")) {
            GuideBook book = GuideBook.parse(classpathText(helper, "/assets/dtp/guide/" + lang + ".md"));
            helper.assertTrue(book.problems().isEmpty(), lang + ".md: " + book.problems());
            helper.assertFalse(book.pages().isEmpty(), lang + ".md has pages");
            StringBuilder structure = new StringBuilder();
            for (int c = 0; c < book.chapters().size(); c++) {
                for (GuideBook.Page page : book.chapters().get(c).pages()) {
                    helper.assertFalse(page.title().isBlank() || page.body().isEmpty(), lang + ": page " + page.id() + " has title and text");
                    structure.append(c).append(':').append(page.id()).append(':').append(page.grid()).append(':').append(page.warning()).append('\n');
                }
            }
            structures.add(structure.toString());
            JsonObject json = JsonParser.parseString(classpathText(helper, "/assets/dtp/lang/" + lang + ".json")).getAsJsonObject();
            for (String key : List.of("item.dtp.guide", "dtp.guide.received", "guide.dtp.contents", "guide.dtp.shapeless",
                    "guide.dtp.continued", "guide.dtp.load_error", "block.dtp.repair_station", "dtp.repair_station.repair",
                    "dtp.repair_station.slot.item.hint", "dtp.repair_station.slot.payment.hint")) {
                helper.assertTrue(json.has(key), lang + ".json is missing " + key);
            }
            for (RepairStation.Blocked blocked : RepairStation.Blocked.values()) {
                String key = blocked.translationKey();
                helper.assertTrue(json.has(key), lang + ".json is missing " + key);
            }
        }
        helper.assertTrue(structures.get(0).equals(structures.get(1)),
                "de_de.md and en_us.md differ in chapters, page ids, grids or warnings:\n" + structures.get(0) + "---\n" + structures.get(1));
        helper.succeed();
    }

    /** Der Markdown-Leser: Kommentare, Absätze, Aufzählungen, Attribute und die Fehlermeldungen mit Zeilennummer. */
    private static void guideParser(GameTestHelper helper) {
        GuideBook ok = GuideBook.parse("""
                <!-- Kopf
                über zwei Zeilen -->
                # Kapitel

                ## Seite eins {#one grid=beacon warning}

                Erste Zeile
                geht weiter.

                - Punkt **fett**
                  mit Fortsetzung
                - zweiter Punkt <!-- Notiz -->

                ## Seite zwei {#two}
                Text.
                """);
        helper.assertTrue(ok.problems().isEmpty(), "valid file without problems: " + ok.problems());
        helper.assertTrue(ok.chapters().size() == 1 && ok.pages().size() == 2, "one chapter, two pages");
        GuideBook.Page one = ok.pages().getFirst();
        helper.assertTrue(one.id().equals("one") && one.title().equals("Seite eins") && "beacon".equals(one.grid()) && one.warning(),
                "attributes are read: " + one);
        helper.assertTrue(one.body().equals(List.of("Erste Zeile geht weiter.", "", GuideBook.BULLET + "Punkt **fett** mit Fortsetzung",
                GuideBook.BULLET + "zweiter Punkt")), "paragraphs and bullets: " + one.body());
        helper.assertTrue(ok.pages().get(1).grid() == null && !ok.pages().get(1).warning(), "defaults without attributes");

        GuideBook bad = GuideBook.parse("""
                vorher
                # K
                ## A {#a grid=nope}
                x
                ## B {#a}
                y
                ## C
                z
                ### zu tief
                """);
        List<Integer> lines = bad.problems().stream().map(GuideBook.Problem::line).toList();
        helper.assertTrue(lines.equals(List.of(1, 3, 5, 7, 9)),
                "text before a chapter, unknown grid, duplicate id, missing id, ### heading: " + bad.problems());
        helper.succeed();
    }

    // --- Statistik-Tafel ----------------------------------------------------------------------

    private static StatisticsData.Tally tally(Map<Identifier, StatisticsData.Tally> sales, Item item) {
        return sales.getOrDefault(Market.itemId(item), new StatisticsData.Tally(0, 0));
    }

    /**
     * Jeder Verkauf zählt insgesamt und am heutigen Markttag — auch außerhalb des Territoriums. Nach 14 Markttagen
     * fällt er aus dem Zeitraum „14 Tage“, bleibt aber in „Gesamt“. Isoliert, weil der Test die Marktuhr vorstellt.
     */
    private static void statisticsSales(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        StatisticsData stats = StatisticsData.get(server);
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        StatisticsData.Tally allBefore = tally(stats.allTimeSales(), Items.WHEAT);
        StatisticsData.Tally recentBefore = tally(stats.recentSales(server), Items.WHEAT);
        int revision = stats.revision();

        long paid = Market.sell(level, pos, Items.WHEAT, 64);
        StatisticsData.Tally all = tally(stats.allTimeSales(), Items.WHEAT);
        StatisticsData.Tally recent = tally(stats.recentSales(server), Items.WHEAT);
        helper.assertTrue(all.count() == allBefore.count() + 64 && all.taler() == allBefore.taler() + paid,
                "all-time counts the sale: " + allBefore + " -> " + all + ", paid " + paid);
        helper.assertTrue(recent.count() == recentBefore.count() + 64 && recent.taler() == recentBefore.taler() + paid,
                "the recent window counts the sale: " + recentBefore + " -> " + recent);
        helper.assertTrue(stats.revision() != revision, "a sale changes the revision");

        MarketData.get(server).advance(MarketData.HISTORY_DAYS);
        helper.assertTrue(tally(stats.recentSales(server), Items.WHEAT).count() == 0, "after 14 market days the sale leaves the window");
        helper.assertTrue(tally(stats.allTimeSales(), Items.WHEAT).count() == all.count(), "all-time keeps the sale");
        helper.succeed();
    }

    /** Altbestände aus dem Spieler-Attachment werden genau einmal übernommen; danach zählt nur noch die Statistik. */
    private static void statisticsSeed(GameTestHelper helper) {
        StatisticsData stats = StatisticsData.get(helper.getLevel().getServer());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerContracts mine = Contracts.of(player);
        mine.addCompleted(2);
        mine.addCompleted(2);
        mine.addCompleted(4);
        helper.assertTrue(stats.seed(player.getUUID(), "Seedy", mine, 4), "the first login seeds the counters");
        StatisticsData.PlayerTally tally = stats.players().get(player.getUUID());
        helper.assertTrue(tally.completed(2) == 2 && tally.completed(4) == 1 && tally.total() == 3, "seeded per tier: " + tally);
        mine.addCompleted(1);
        helper.assertFalse(stats.seed(player.getUUID(), "Seedy", mine, 4), "a second login seeds nothing");
        helper.assertTrue(stats.players().get(player.getUUID()).total() == 3, "seeding does not count twice");
        stats.recordContract(helper.getLevel().getServer(), player.getUUID(), "Seedy", 3, 0);
        stats.rememberName(player.getUUID(), "Renamed");
        tally = stats.players().get(player.getUUID());
        helper.assertTrue(tally.completed(3) == 1 && tally.total() == 4, "a new completion counts: " + tally);
        helper.assertTrue(tally.name().equals("Renamed"), "the latest name is kept");
        helper.succeed();
    }

    /** Der Datenstand der Tafel: Gruppen wie am Marktstand, Spieler nach Summe sortiert, der Rest ohne Zuordnung. */
    private static void statisticsSnapshot(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        StatisticsData stats = StatisticsData.get(server);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        stats.recordContract(server, player.getUUID(), "Snap", 1, 0);
        int assigned = stats.players().values().stream().mapToInt(StatisticsData.PlayerTally::total).sum();

        CompoundTag tag = StatisticsMenu.build(server, stats, assigned + 5);
        helper.assertTrue(tag.getListOrEmpty("categories").equals(MarketCategories.snapshot(server)), "the same groups as the market stand");
        helper.assertTrue(tag.getIntOr("unassigned", -1) == 5, "completions without a player: " + tag.getIntOr("unassigned", -1));
        helper.assertTrue(tag.getIntOr("contracts_total", 0) == assigned + 5, "the total comes from the world progress");
        helper.assertTrue(tag.getIntOr("tiers", 0) >= DtpConfig.TIER_VALUE.get().size(), "one column per tier");
        ListTag players = tag.getListOrEmpty("players");
        boolean found = false;
        for (int i = 0; i < players.size(); i++) {
            CompoundTag p = players.getCompoundOrEmpty(i);
            found |= p.getStringOr("name", "").equals("Snap");
            if (i > 0) {
                helper.assertTrue(players.getCompoundOrEmpty(i - 1).getIntOr("sum", 0) >= p.getIntOr("sum", 0), "players sorted by total");
            }
        }
        helper.assertTrue(found, "the player is listed by name");
        helper.assertTrue(StatisticsMenu.build(server, stats, 0).getIntOr("unassigned", -1) == 0, "never negative");
        helper.succeed();
    }

    /** Einnahmen des heutigen Minecraft-Tags laut Statistik-Tafel. */
    private static StatisticsData.DayIncome todayIncome(MinecraftServer server) {
        return StatisticsData.get(server).dailyIncome(StatisticsData.minecraftDay(server)).getLast();
    }

    /**
     * Einnahmen je Minecraft-Tag: Verkäufe und Contracts am heutigen Tag, ein festes Fenster von 14 Tagen mit Nullen
     * für leere Tage, Verwerfen alter und „zukünftiger“ Tage und das Speicherformat v1 → v2.
     */
    private static void statisticsIncome(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        MinecraftServer server = level.getServer();
        StatisticsData stats = StatisticsData.get(server);
        int days = MarketData.HISTORY_DAYS;

        long salesBefore = todayIncome(server).sales();
        long paid = Market.sell(level, helper.absolutePos(BlockPos.ZERO), Items.WHEAT, 16);
        helper.assertTrue(paid > 0 && todayIncome(server).sales() == salesBefore + paid,
                "a sale counts as income today: " + salesBefore + " + " + paid + " -> " + todayIncome(server).sales());

        // Eigene Instanz mit festen Tagen, damit weder die Weltzeit noch andere Tests hineinspielen.
        StatisticsData fresh = new StatisticsData();
        int d = 1000;
        fresh.addIncome(d, 100, 0);
        fresh.addIncome(d, 0, 50);
        fresh.addIncome(d + 1, 10, 0);
        List<StatisticsData.DayIncome> window = fresh.dailyIncome(d + 1);
        helper.assertTrue(window.size() == days, "always " + days + " days: " + window.size());
        helper.assertTrue(window.get(0).day() == d + 2 - days && window.get(days - 1).day() == d + 1, "oldest first, today last");
        StatisticsData.DayIncome yesterday = window.get(days - 2);
        helper.assertTrue(yesterday.day() == d && yesterday.sales() == 100 && yesterday.contracts() == 50 && yesterday.total() == 150,
                "yesterday sums sales and contracts: " + yesterday);
        helper.assertTrue(window.get(days - 1).sales() == 10, "today: " + window.get(days - 1));
        helper.assertTrue(window.get(0).total() == 0, "days without income count zero");
        int later = d + 1 + days;
        helper.assertTrue(fresh.dailyIncome(later).stream().allMatch(e -> e.total() == 0), days + " days later nothing is left in the window");

        fresh.addIncome(later, 1, 0);
        helper.assertTrue(fresh.dailyIncome(d + 1).stream().allMatch(e -> e.total() == 0), "older days are dropped when a new day is written");
        // Uhr zurückgestellt: der spätere Tag fällt weg, sobald auf einem früheren gezählt wird.
        fresh.addIncome(d + 5, 7, 0);
        helper.assertTrue(fresh.dailyIncome(later).getLast().total() == 0, "a day after the current clock is dropped");
        fresh.addIncome(d + 5, 0, 0);
        helper.assertTrue(fresh.dailyIncome(d + 5).getLast().sales() == 7, "zero income changes nothing");

        CompoundTag tag = StatisticsMenu.build(server, stats, 0);
        ListTag income = tag.getListOrEmpty("income");
        helper.assertTrue(income.size() == days, "the snapshot carries " + days + " days: " + income.size());
        helper.assertTrue(income.getCompoundOrEmpty(days - 1).getIntOr("day", -1) == StatisticsData.minecraftDay(server),
                "the last entry is today");

        // Speicherformat: v1 ohne Einnahmen lädt leer, gespeichert wird v2 mit den Einnahmen.
        CompoundTag v1 = new CompoundTag();
        v1.putInt(Versioned.KEY, 1);
        StatisticsData migrated = StatisticsData.CODEC.parse(NbtOps.INSTANCE, v1).getOrThrow();
        helper.assertTrue(migrated.dailyIncome(d).stream().allMatch(e -> e.total() == 0), "v1 data: no income yet");
        Tag saved = StatisticsData.CODEC.encodeStart(NbtOps.INSTANCE, fresh).getOrThrow();
        helper.assertTrue(((CompoundTag) saved).getIntOr(Versioned.KEY, 0) == 2, "saved with the current version");
        StatisticsData loaded = StatisticsData.CODEC.parse(NbtOps.INSTANCE, saved).getOrThrow();
        helper.assertTrue(loaded.dailyIncome(d + 5).getLast().sales() == 7, "income survives saving and loading");
        helper.succeed();
    }

    /** Alle Advancements der Progressionslinie sind geladen. Fehlerhaftes JSON landet sonst nur im Log. */
    private static void advancementsLoaded(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (String name : List.of("root", "beacon", "market_stand", "first_sale", "contract", "deposit_chest", "gold_taler",
                "personal_upgrade", "server_upgrade", "relay_beacon", "open_world")) {
            helper.assertTrue(server.getAdvancements().get(id("progression/" + name)) != null, "advancement not loaded: progression/" + name);
        }
        UpgradeTrigger.TriggerInstance border = new UpgradeTrigger.TriggerInstance(Optional.empty(), Optional.of("border"),
                MinMaxBounds.Ints.atLeast(11));
        helper.assertTrue(border.matches("border", 11), "border 11 matches");
        helper.assertTrue(!border.matches("border", 10), "border 10 does not");
        helper.assertTrue(!border.matches("anvil_cap", 11), "another upgrade does not");
        helper.succeed();
    }

    /**
     * Alle JSON-Dateien unter {@code data/} lassen sich mit ihrem Codec lesen, enthalten keine unbekannten Schlüssel
     * (still ignorierte Altschlüssel, Tippfehler) und verweisen nur auf vorhandene IDs. Explizite Standardwerte stehen
     * nur als Hinweis im Log. Details: {@link DataFileCheck}.
     */
    private static void dataFilesValid(GameTestHelper helper) {
        DataFileCheck.Result result = DataFileCheck.run(helper.getLevel().getServer());
        result.defaults().forEach(d -> Dtp.LOGGER.info("Data file check (hint): {}", d));
        result.problems().forEach(p -> Dtp.LOGGER.error("Data file check: {}", p));
        Dtp.LOGGER.info("Data file check: {} files, {} problem(s), {} explicit default value(s)", result.files(),
                result.problems().size(), result.defaults().size());
        helper.assertTrue(result.files() > 100, "data files found: " + result.files());
        helper.assertTrue(result.problems().isEmpty(), result.problems().size() + " problem(s) in data files, first: "
                + result.problems().stream().findFirst().orElse(""));
        helper.succeed();
    }

    /**
     * Anbindungen an andere Mods sind optional: Ohne Sophisticated Backpacks gibt es die Rucksack-Upgrades nicht —
     * weder als Item noch als Rezept. Mit dem Mod (-Pwith_compat=true) gibt es beides.
     */
    private static void compatOptional(GameTestHelper helper) {
        boolean loaded = ModList.get().isLoaded(Compat.SOPHISTICATED_BACKPACKS);
        MinecraftServer server = helper.getLevel().getServer();
        for (String name : List.of("trade_chest_upgrade", "deposit_chest_upgrade")) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(id(name)) == loaded, name + " registered only with Sophisticated Backpacks");
            boolean recipe = server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, id("compat/sophisticated/" + name))).isPresent();
            helper.assertTrue(recipe == loaded, name + " recipe only with Sophisticated Backpacks");
        }
        if (loaded) {
            // Nur mit dem Mod: Erst hier wird eine Klasse geladen, die Sophisticated Backpacks kennt.
            SophisticatedTests.backpackStorage(helper, new BlockPos(1, 1, 1), helper.absolutePos(new BlockPos(0, 1, 1)));
        }
        helper.succeed();
    }

    /**
     * Händlerware: Das Öffnen des Handelsfensters markiert die Angebote eines Dorfbewohners, nicht die Smaragde und
     * nicht den Schwarzmarkt. Ein Handel markiert Angebote nach, die erst danach dazukamen.
     */
    private static void merchantGoodsOffers(GameTestHelper helper) {
        ServerPlayer player = (ServerPlayer) helper.makeMockServerPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(2.5, 1, 2.5)));
        var villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        BlackmarketTrader dealer = helper.spawn(ModEntities.BLACKMARKET_TRADER.get(), new BlockPos(3, 1, 1));
        try {
            villager.getOffers().clear();
            MerchantOffer cake = new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.CAKE), 12, 1, 0.05f);
            MerchantOffer emerald = new MerchantOffer(new ItemCost(Items.WHEAT, 20), new ItemStack(Items.EMERALD), 12, 1, 0.05f);
            villager.getOffers().add(cake);
            villager.getOffers().add(emerald);
            villager.setTradingPlayer(player);
            NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, new MerchantMenu(1, player.getInventory(), villager)));
            helper.assertTrue(MerchantGoods.isMarked(cake.getResult()), "the villager's cake is marked when the menu opens");
            helper.assertTrue(MerchantGoods.isMarked(cake.assemble()), "the bought copy keeps the mark");
            helper.assertFalse(MerchantGoods.isMarked(emerald.getResult()), "emeralds stay plain");
            ItemStack before = cake.getResult().copy();
            MerchantGoods.markOffers(villager.getOffers());
            helper.assertTrue(ItemStack.isSameItemSameComponents(before, cake.getResult()), "marking twice changes nothing");

            MerchantOffer pie = new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.PUMPKIN_PIE, 4), 12, 1, 0.05f);
            villager.getOffers().add(pie);
            NeoForge.EVENT_BUS.post(new TradeWithVillagerEvent(player, emerald, villager));
            helper.assertTrue(MerchantGoods.isMarked(pie.getResult()), "a trade marks offers added by a level-up");
            villager.setTradingPlayer(null);

            MerchantOffer elytra = new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(Items.ELYTRA), 1, 1, 0.0f);
            dealer.getOffers().add(elytra);
            dealer.setTradingPlayer(player);
            NeoForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, new MerchantMenu(2, player.getInventory(), dealer)));
            NeoForge.EVENT_BUS.post(new TradeWithVillagerEvent(player, elytra, dealer));
            helper.assertFalse(MerchantGoods.isMarked(elytra.getResult()), "the black market belongs to DTP and stays unmarked");
        } finally {
            villager.discard();
            dealer.discard();
        }
        helper.succeed();
    }

    /** Geschenke an den Helden des Dorfes sind Händlerware, anderer Loot nicht. */
    private static void merchantGoodsHeroGift(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var villager = helper.spawn(EntityTypes.VILLAGER, new BlockPos(1, 1, 1));
        try {
            LootTable gifts = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FARMER_GIFT);
            int rolled = 0;
            for (int i = 0; i < 20; i++) {
                LootParams params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, villager.position())
                        .withParameter(LootContextParams.THIS_ENTITY, villager)
                        .create(LootContextParamSets.GIFT);
                for (ItemStack gift : gifts.getRandomItems(params)) {
                    helper.assertTrue(MerchantGoods.isMarked(gift), "the gift " + gift + " is merchant goods");
                    rolled++;
                }
            }
            helper.assertTrue(rolled > 0, "the farmer's gift table drops something");
            MerchantGoodsModifier modifier = new MerchantGoodsModifier(Optional.empty(), 0, List.of("minecraft:gameplay/hero_of_the_village/"));
            helper.assertTrue(modifier.appliesTo(Identifier.withDefaultNamespace("gameplay/hero_of_the_village/cleric_gift")), "all hero gifts");
            helper.assertFalse(modifier.appliesTo(Identifier.withDefaultNamespace("chests/village/village_plains_house")), "village chests stay finds");
            helper.assertFalse(modifier.appliesTo(Identifier.withDefaultNamespace("gameplay/piglin_bartering")), "bartering is not covered");
        } finally {
            villager.discard();
        }
        helper.succeed();
    }

    /** Händlerware zählt für keinen Contract (auch nicht Stufe 4), ist unverkäuflich und geht nicht in den Recycler. */
    private static void merchantGoodsBlocked(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ItemStack cake = new ItemStack(Items.CAKE);
        ItemStack boughtCake = cake.copy();
        MerchantGoods.mark(boughtCake);
        Contract cakes = Contract.offer(Identifier.withDefaultNamespace("cake"), DataComponentMatchers.ANY, 128, 45000, 3, 0.0);
        helper.assertTrue(cakes.matches(cake), "a baked cake counts");
        helper.assertFalse(cakes.matches(boughtCake), "a bought cake does not");

        ItemStack book = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.MENDING, 1);
        ItemStack boughtBook = book.copy();
        MerchantGoods.mark(boughtBook);
        Contract books = Contract.offer(Identifier.withDefaultNamespace("enchanted_book"), DataComponentMatchers.ANY, 4, 16000, 4, 0.0);
        helper.assertTrue(books.matches(book) && !books.matches(boughtBook), "tier 4: librarian books do not count");

        ItemStack wool = new ItemStack(Items.WOOL.white());
        ItemStack boughtWool = wool.copy();
        MerchantGoods.mark(boughtWool);
        helper.assertTrue(TradeList.lookup(wool).isPresent(), "control: shorn wool is tradable");
        helper.assertTrue(TradeList.lookup(boughtWool).isEmpty(), "bought wool is not");
        Contract woolContract = Contract.offer(Identifier.withDefaultNamespace("white_wool"), Optional.of(id("wool")),
                DataComponentMatchers.ANY, 64, 2000, 1, 0.0);
        helper.assertTrue(woolContract.matches(wool) && !woolContract.matches(boughtWool), "the wool group ignores bought wool");
        ItemStack ominous = Raid.getOminousBannerInstance(level.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN));
        MerchantGoods.mark(ominous);
        helper.assertTrue(TradeList.lookup(ominous).isEmpty(), "entries with a condition refuse merchant goods too");

        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        ItemStack boughtBoots = boots.copy();
        MerchantGoods.mark(boughtBoots);
        helper.assertTrue(Recycling.accepts(level.getServer(), boots), "control: the recycler takes leather boots");
        helper.assertFalse(Recycling.accepts(level.getServer(), boughtBoots), "but not bought ones");

        ItemStack emerald = new ItemStack(Items.EMERALD);
        MerchantGoods.mark(emerald);
        helper.assertFalse(MerchantGoods.isMarked(emerald), "emeralds are exempt");
        helper.succeed();
    }

    /**
     * Kombinieren wäscht keine Händlerware: Amboss (links oder rechts markiert), Reparatur im Crafting-Grid und
     * Schleifstein (markiert nur unten: kein Ergebnis) geben die Markierung weiter.
     */
    private static void merchantGoodsCombine(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        ItemStack book = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.LURE, 3);
        ItemStack otherBook = enchanted(level, Items.ENCHANTED_BOOK, true, Enchantments.UNBREAKING, 3);
        ItemStack boughtBook = book.copy();
        MerchantGoods.mark(boughtBook);
        ItemStack boughtRod = rod.copy();
        MerchantGoods.mark(boughtRod);

        ItemStack clean = anvil(player, rod, book, null);
        helper.assertTrue(!clean.isEmpty() && !MerchantGoods.isMarked(clean), "control: rod + crafted book stays clean");
        ItemStack fromRight = anvil(player, rod, boughtBook, null);
        helper.assertTrue(!fromRight.isEmpty() && MerchantGoods.isMarked(fromRight), "rod + librarian book is merchant goods");
        ItemStack fromLeft = anvil(player, boughtRod, book, null);
        helper.assertTrue(!fromLeft.isEmpty() && MerchantGoods.isMarked(fromLeft), "bought rod + book is merchant goods");
        ItemStack books = anvil(player, otherBook, boughtBook, null);
        helper.assertTrue(!books.isEmpty() && MerchantGoods.isMarked(books), "book + librarian book is merchant goods");

        ItemStack worn = new ItemStack(Items.DIAMOND_HELMET);
        worn.setDamageValue(200);
        ItemStack boughtWorn = worn.copy();
        MerchantGoods.mark(boughtWorn);
        CraftingInput cleanGrid = CraftingInput.of(2, 1, List.of(worn, worn.copy()));
        var repair = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, cleanGrid, level);
        helper.assertTrue(repair.isPresent() && repair.get().value() instanceof MerchantRepairItemRecipe,
                "minecraft:repair_item is the DTP recipe: " + repair.map(r -> r.value().getClass().getName()));
        ItemStack repaired = repair.get().value().assemble(cleanGrid);
        helper.assertTrue(repaired.is(Items.DIAMOND_HELMET) && !MerchantGoods.isMarked(repaired), "control: two crafted helmets stay clean");
        CraftingInput mixedGrid = CraftingInput.of(2, 1, List.of(worn, boughtWorn));
        helper.assertTrue(MerchantGoods.isMarked(repair.get().value().assemble(mixedGrid)), "one bought helmet marks the repair");
        CraftingInput boughtGrid = CraftingInput.of(2, 1, List.of(boughtWorn, boughtWorn.copy()));
        helper.assertTrue(MerchantGoods.isMarked(repair.get().value().assemble(boughtGrid)), "two bought helmets stay merchant goods");

        ItemStack sword = enchanted(level, Items.IRON_SWORD, false, Enchantments.SHARPNESS, 2);
        sword.setDamageValue(100);
        ItemStack boughtSword = sword.copy();
        MerchantGoods.mark(boughtSword);
        helper.assertTrue(MerchantGoods.isMarked(grindstone(player, boughtSword, ItemStack.EMPTY)), "a single bought sword stays marked");
        helper.assertFalse(grindstone(player, sword, sword).isEmpty(), "control: two crafted swords merge");
        helper.assertTrue(grindstone(player, sword, boughtSword).isEmpty(), "bought sword at the bottom: no result");
        ItemStack topBought = grindstone(player, boughtSword, sword);
        helper.assertTrue(!topBought.isEmpty() && MerchantGoods.isMarked(topBought), "bought sword on top: marked result");
        helper.succeed();
    }

    private static ItemStack grindstone(Player player, ItemStack top, ItemStack bottom) {
        GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory());
        menu.getSlot(0).set(top.copy());
        menu.getSlot(1).set(bottom.copy());
        return menu.getSlot(2).getItem();
    }

    /**
     * Pool-Prüfung gegen das Waschen von Händlerware. Fehlschlag, wenn ein Contract-Item oder eine Ware der Handelsliste
     * <ul>
     * <li>ein Block ist, den ein Dorfbewohner-Beruf verkauft und der sich nach dem Platzieren selbst wieder abbauen
     * lässt — die Markierung ginge dabei verloren —, oder</li>
     * <li>ein Rezept hat, dessen Zutat ein Dorfbewohner-Beruf verkauft.</li>
     * </ul>
     * Ware des fahrenden Händlers zählt nicht: Sie ist knapp, und der Händler zieht weiter. Schmiede- und
     * Umwandlungsrezepte (Netherit-Aufwertung, Besätze, Färben) übernehmen die Komponenten des Grund-Items, die
     * Markierung bleibt also erhalten.
     */
    private static void merchantGoodsPool(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        RegistryOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
        Set<Item> professionGoods = new HashSet<>();
        level.registryAccess().lookupOrThrow(Registries.VILLAGER_TRADE).listElements().forEach(holder -> {
            Identifier trade = holder.key().identifier();
            if (trade.getNamespace().equals(Dtp.MODID)) {
                return;
            }
            JsonElement json = VillagerTrade.CODEC.encodeStart(ops, holder.value()).getOrThrow();
            Item gives = BuiltInRegistries.ITEM.getValue(Identifier.parse(json.getAsJsonObject().getAsJsonObject("gives").get("id").getAsString()));
            if (!trade.getPath().startsWith("wandering_trader/") && !new ItemStack(gives).is(MerchantGoods.EXEMPT)) {
                professionGoods.add(gives);
            }
        });
        helper.assertTrue(professionGoods.contains(Items.CAKE) && professionGoods.contains(Items.PUMPKIN_PIE), "the farmer sells cake and pie");

        Set<Item> pool = new HashSet<>(TradeList.allTradable());
        for (ContractTemplates.Template t : ContractTemplates.all()) {
            pool.add(BuiltInRegistries.ITEM.getValue(t.item()));
            t.key().ifPresent(key -> pool.addAll(TradeList.members(key)));
        }
        // Bewusst hingenommen, weil jeder Block einzeln von Hand gelegt werden muss; bepreist nach D1/D2 (docs/60):
        // Wolle (Schäfer) und Redstone (Geistlicher) verlieren beim Abbauen die Markierung. Das weiße Banner steht nur als
        // Böses Banner auf der Liste, das sich nicht craften lässt. Der Goldene Apfel braucht einen Apfel (Bauer), sein
        // Zutatenwert ist aber zu 91 % Gold.
        Set<Item> accepted = new HashSet<>(List.of(Items.REDSTONE, Items.BANNER.white(), Items.GOLDEN_APPLE));
        TradeList.members(id("wool")).forEach(accepted::add);

        List<String> problems = new ArrayList<>();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ItemStack silkPick = enchanted(level, Items.DIAMOND_PICKAXE, false, Enchantments.SILK_TOUCH, 1);
        List<ItemStack> tools = List.of(ItemStack.EMPTY, new ItemStack(Items.SHEARS), new ItemStack(Items.DIAMOND_PICKAXE), silkPick);
        for (Item item : pool) {
            if (accepted.contains(item) || !professionGoods.contains(item) || !(item instanceof BlockItem blockItem)) {
                continue;
            }
            for (ItemStack tool : tools) {
                if (Block.getDrops(blockItem.getBlock().defaultBlockState(), level, pos, null, null, tool).stream().anyMatch(d -> d.is(item))) {
                    problems.add(Market.itemId(item) + " is sold by a merchant and drops itself when broken");
                    break;
                }
            }
        }
        var context = SlotDisplayContext.fromLevel(level);
        for (RecipeHolder<?> recipe : level.getServer().getRecipeManager().getRecipes()) {
            Set<Item> results = new HashSet<>();
            for (RecipeDisplay display : recipe.value().display()) {
                display.result().resolveForStacks(context).forEach(stack -> results.add(stack.getItem()));
            }
            results.retainAll(pool);
            results.removeAll(accepted);
            if (results.isEmpty() || recipe.value() instanceof SmithingRecipe || recipe.value() instanceof TransmuteRecipe
                    || recipe.value().placementInfo().isImpossibleToPlace()) {
                continue;
            }
            for (Ingredient ingredient : recipe.value().placementInfo().ingredients()) {
                ingredient.items().map(Holder::value).filter(professionGoods::contains).findFirst().ifPresent(sold ->
                        problems.add(recipe.id().identifier() + " makes " + results + " from " + Market.itemId(sold) + ", which villagers sell"));
            }
        }
        helper.assertTrue(problems.isEmpty(), "merchant goods can be laundered:\n" + String.join("\n", problems));
        helper.succeed();
    }
}
