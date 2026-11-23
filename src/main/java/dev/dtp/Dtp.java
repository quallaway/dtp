package dev.dtp;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import dev.dtp.chunkloader.ChunkLoaders;
import dev.dtp.compat.Compat;
import dev.dtp.contract.ContractClock;
import dev.dtp.data.DtpStorage;
import dev.dtp.gametest.DtpGameTests;
import dev.dtp.menu.MenuSync;
import dev.dtp.beacon.Rebuild;
import dev.dtp.registry.ModAttachments;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModDataComponents;
import dev.dtp.registry.ModEntities;
import dev.dtp.registry.ModItems;
import dev.dtp.registry.ModLootModifiers;
import dev.dtp.registry.ModLootProviders;
import dev.dtp.registry.ModMenus;
import dev.dtp.registry.ModRecipes;
import dev.dtp.registry.ModTriggers;
import dev.dtp.upgrade.ServerUpgradeSync;
import dev.dtp.upgrade.Skills;

/** Dynamic Trade &amp; Progression. */
@Mod(Dtp.MODID)
public class Dtp {
    public static final String MODID = "dtp";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Dtp(IEventBus modBus, ModContainer container) {
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModDataComponents.register(modBus);
        ModEntities.register(modBus);
        ModMenus.register(modBus);
        ModRecipes.register(modBus);
        ModLootProviders.register(modBus);
        ModAttachments.register(modBus);
        ModLootModifiers.register(modBus);
        ModTriggers.register(modBus);
        DtpStorage.register(modBus);
        Compat.init(modBus);
        modBus.addListener(Dtp::registerPayloads);
        modBus.addListener(ChunkLoaders::registerControllers);
        DtpGameTests.register(modBus);
        container.registerConfig(ModConfig.Type.SYNCED, DtpConfig.SPEC);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(MenuSync.Payload.TYPE, MenuSync.Payload.STREAM_CODEC, MenuSync.Payload::handle)
                .playToClient(ServerUpgradeSync.Payload.TYPE, ServerUpgradeSync.Payload.STREAM_CODEC, ServerUpgradeSync.Payload::handle)
                .playToClient(Skills.JumpPayload.TYPE, Skills.JumpPayload.STREAM_CODEC, Skills.JumpPayload::handle)
                .playToClient(Rebuild.MarkPayload.TYPE, Rebuild.MarkPayload.STREAM_CODEC, Rebuild.MarkPayload::handle)
                .playToClient(ContractClock.Payload.TYPE, ContractClock.Payload.STREAM_CODEC, ContractClock.Payload::handle)
                .playToServer(Skills.Payload.TYPE, Skills.Payload.STREAM_CODEC, Skills.Payload::handle);
    }
}
