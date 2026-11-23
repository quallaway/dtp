package dev.dtp;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import dev.dtp.beacon.AnimalHusbandry;
import dev.dtp.beacon.Beekeeping;
import dev.dtp.beacon.BeaconLogic;
import dev.dtp.beacon.BuilderEffects;
import dev.dtp.blackmarket.BlackmarketSpawner;
import dev.dtp.chunkloader.ChunkLoaders;
import dev.dtp.command.DtpCommands;
import dev.dtp.contract.ContractTemplates;
import dev.dtp.contract.Contracts;
import dev.dtp.contract.PlayerContracts;
import dev.dtp.market.MarketCategories;
import dev.dtp.market.Market;
import dev.dtp.market.MarketData;
import dev.dtp.market.MarketEvents;
import dev.dtp.market.TradeList;
import dev.dtp.recycling.Recycling;
import dev.dtp.contraband.ContrabandDefinitions;
import dev.dtp.contraband.Lingering;
import dev.dtp.guide.Onboarding;
import dev.dtp.stats.StatisticsData;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.UpgradeCosts;

@EventBusSubscriber(modid = Dtp.MODID)
public final class GameEvents {
    private GameEvents() {
    }

    @SubscribeEvent
    static void onReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(TradeList.ID, new TradeList());
        event.addListener(UpgradeCosts.ID, new UpgradeCosts());
        event.addListener(PlayerUpgradeDefinitions.ID, new PlayerUpgradeDefinitions());
        event.addListener(ContractTemplates.ID, new ContractTemplates());
        event.addListener(MarketCategories.ID, new MarketCategories());
        event.addListener(ContrabandDefinitions.ID, new ContrabandDefinitions());
        event.addListener(Recycling.ID, new Recycling());
    }

    @SubscribeEvent
    static void onTagsUpdated(TagsUpdatedEvent event) {
        TradeList.onTagsUpdated();
        Recycling.invalidate();
    }

    /**
     * Der Wither-Kampf rückt das nächste Marktereignis heran ({@link MarketEvents#onWitherKilled}). Zählt nur, wenn ein
     * Spieler den Kill bekommt. Niedrigste Priorität, damit ein anderswo abgebrochener Tod nicht zählt.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onWitherDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof WitherBoss wither && wither.level().getServer() instanceof MinecraftServer server
                && event.getSource().getEntity() instanceof Player) {
            MarketEvents.onWitherKilled(server);
        }
    }

    @SubscribeEvent
    static void onCommands(RegisterCommandsEvent event) {
        DtpCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    static void onServerStarted(ServerStartedEvent event) {
        // Die Zeit, in der der Server aus war, zählt nicht — weder für den Markt noch für Contracts.
        MarketData.get(event.getServer()).resetClockBaseline(event.getServer());
        BeaconLogic.fixSpawnOutsideBorder(event.getServer());
        BeaconLogic.disableSpawnProtectionIfStarted(event.getServer());
        ChunkLoaders.reset();
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MarketData.get(player.level().getServer()).markSeen(player.getUUID());
            PlayerContracts contracts = Contracts.of(player); // startet die persönliche Contract-Uhr bei älteren Spielständen
            // Statistik-Tafel: Contract-Zähler aus älteren Spielständen einmal übernehmen, den Namen aktuell halten.
            StatisticsData stats = StatisticsData.get(player.level().getServer());
            String name = player.getGameProfile().name();
            if (!stats.seed(player.getUUID(), name, contracts, DtpConfig.TIER_VALUE.get().size())) {
                stats.rememberName(player.getUUID(), name);
            }
            Onboarding.welcome(player);
        }
    }

    /** Alles Laufende passiert im Intervall, nie pro Tick und nie pro Item. */
    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int tick = server.getTickCount();
        if (tick % 20 == 0) {
            MarketData data = MarketData.get(server);
            data.followClock(server);
            MarketEvents.tick(server);
            Market.sampleHistory(server.overworld());
            BlackmarketSpawner.tick(server);
            // Wie die Marktuhr: Chunkloader halten nur, solange jemand online ist.
            ChunkLoaders.tick(server, server.getPlayerCount() > 0);
            if (tick % 1200 == 0) {
                server.getPlayerList().getPlayers().forEach(p -> data.markSeen(p.getUUID()));
            }
        }
        if (tick % 10 == 0) {
            BuilderEffects.update(server);
        }
        if (tick % Lingering.INTERVAL == 0) {
            Lingering.tick(server);
        }
        if (tick % 40 == 0) {
            BeaconLogic.applyDeepslateDarkness(server);
        }
        if (tick % 80 == 0) {
            BeaconLogic.applyEffects(server);
        }
        if (tick % BeaconLogic.MENDING_INTERVAL_TICKS == 0) {
            BeaconLogic.applyMendingAura(server);
        }
        // Ausnahme vom Intervall: nur, wenn in diesem Tick im Beacon-Radius gezüchtet wurde.
        AnimalHusbandry.flushPending(server);
        // Ebenso: nur, wenn in diesem Tick ein Bienenstock gefüllt oder geerntet wurde.
        Beekeeping.flushPending(server);
    }
}
