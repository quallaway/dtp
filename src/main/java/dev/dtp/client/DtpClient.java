package dev.dtp.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import dev.dtp.Dtp;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModEntities;
import dev.dtp.registry.ModItems;
import dev.dtp.registry.ModMenus;

/** Lädt nur auf dem Client. */
@Mod(value = Dtp.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Dtp.MODID, value = Dist.CLIENT)
public class DtpClient {
    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MARKET.get(), MarketScreen::new);
        event.register(ModMenus.BEACON.get(), BeaconScreen::new);
        event.register(ModMenus.DEPOSIT_CHEST.get(), DepositChestScreen::new);
        event.register(ModMenus.RECYCLER.get(), RecyclerScreen::new);
        event.register(ModMenus.BULLETIN_BOARD.get(), BulletinBoardScreen::new);
        event.register(ModMenus.STATISTICS_BOARD.get(), StatisticsScreen::new);
        event.register(ModMenus.UPGRADE_TABLE.get(), UpgradeTableScreen::new);
        event.register(ModMenus.RELAY_BEACON.get(), RelayBeaconScreen::new);
        event.register(ModMenus.CHUNK_LOADER.get(), ChunkLoaderScreen::new);
        event.register(ModMenus.BLACKMARKET.get(), BlackmarketScreen::new);
        event.register(ModMenus.REPAIR_STATION.get(), RepairStationScreen::new);
    }

    /** Handbuch-Texte aus assets/dtp/guide/*.md, neu geladen bei jedem Resource-Reload (F3+T). */
    @SubscribeEvent
    static void onClientReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(GuideBooks.ID, new GuideBooks());
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlocks.MARKET_SCALE_BE.get(), MarketScaleRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.CHUNK_LOADER_BE.get(), ChunkLoaderRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.SLOT_MACHINE_BE.get(), SlotMachineRenderer::new);
        event.registerEntityRenderer(ModEntities.BLACKMARKET_TRADER.get(), BlackmarketTraderRenderer::new);
    }

    /** Wer die Gun hält, streckt den Arm nach vorn (DtpArmPoses.GUN_AIM). */
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return DtpArmPoses.GUN_AIM.getValue();
            }
        }, ModItems.GUN.get());
    }
}
