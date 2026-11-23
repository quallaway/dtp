package dev.dtp.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

import dev.dtp.beacon.BeaconLogic;
import dev.dtp.blackmarket.BlackmarketSpawner;
import dev.dtp.contract.ContractBoard;
import dev.dtp.currency.Money;
import dev.dtp.market.Market;
import dev.dtp.market.MarketCategories;
import dev.dtp.market.MarketData;
import dev.dtp.market.MarketEvents;
import dev.dtp.market.TradeList;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgradeEffects;
import dev.dtp.upgrade.PlayerUpgrades;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.ServerUpgradeSync;
import dev.dtp.upgrade.WorldProgress;

/**
 * {@code /dtp} — Preis abfragen (alle Spieler), Beacon auf der Y-Achse verschieben (Admins)
 * und Admin-Notausgänge unter {@code /dtp admin}.
 */
public final class DtpCommands {
    private static final SuggestionProvider<CommandSourceStack> UPGRADES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(ServerUpgrade.values()).map(u -> u.id), builder);
    private static final SuggestionProvider<CommandSourceStack> PLAYER_UPGRADES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(Arrays.stream(PlayerUpgrade.values()).map(u -> u.id), builder);
    private static final SuggestionProvider<CommandSourceStack> EVENT_TARGETS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(MarketCategories.all().stream().map(c -> c.id().getPath()), builder);
    private static final SuggestionProvider<CommandSourceStack> CONFIG_KEYS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(ConfigReset.entries().stream().map(ConfigReset.Entry::key), builder);

    /** Einträge für {@code /dtp help}: Syntax, Übersetzungsschlüssel der Kurzerklärung, nur für Admins. */
    private record HelpEntry(String syntax, String key, boolean admin) {
    }

    private static final List<HelpEntry> HELP = List.of(
            new HelpEntry("/dtp help", "dtp.command.help.help", false),
            new HelpEntry("/dtp price", "dtp.command.help.price", false),
            new HelpEntry("/dtp beacon y <y>", "dtp.command.help.beacon_y", true),
            new HelpEntry("/dtp admin contracts reroll", "dtp.command.help.contract_reroll", true),
            new HelpEntry("/dtp admin beacon reset", "dtp.command.help.beacon_reset", true),
            new HelpEntry("/dtp admin beacon move <pos>", "dtp.command.help.beacon_move", true),
            new HelpEntry("/dtp admin market reset", "dtp.command.help.market_reset", true),
            new HelpEntry("/dtp admin market event", "dtp.command.help.event", true),
            new HelpEntry("/dtp admin market event info", "dtp.command.help.event_info", true),
            new HelpEntry("/dtp admin market event in <minutes>", "dtp.command.help.event_in", true),
            new HelpEntry("/dtp admin market event type <demand_surge|fast_recovery>", "dtp.command.help.event_type", true),
            new HelpEntry("/dtp admin market event target <group>", "dtp.command.help.event_target", true),
            new HelpEntry("/dtp admin market event fire demand_surge <group>", "dtp.command.help.event_fire_demand_surge", true),
            new HelpEntry("/dtp admin market event fire fast_recovery", "dtp.command.help.event_fire_fast_recovery", true),
            new HelpEntry("/dtp admin market event end", "dtp.command.help.event_end", true),
            new HelpEntry("/dtp admin market advance <days>", "dtp.command.help.advance", true),
            new HelpEntry("/dtp admin upgrade <id> <level>", "dtp.command.help.upgrade", true),
            new HelpEntry("/dtp admin upgrade reset", "dtp.command.help.upgrade_reset", true),
            new HelpEntry("/dtp admin playerupgrade <player> <id> <level>", "dtp.command.help.playerupgrade", true),
            new HelpEntry("/dtp admin playerupgrade <player> reset", "dtp.command.help.playerupgrade_reset", true),
            new HelpEntry("/dtp admin blackmarket status", "dtp.command.help.trader_status", true),
            new HelpEntry("/dtp admin blackmarket spawn", "dtp.command.help.trader_spawn", true),
            new HelpEntry("/dtp admin config diff", "dtp.command.help.config_diff", true),
            new HelpEntry("/dtp admin config reset <key>", "dtp.command.help.config_reset", true),
            new HelpEntry("/dtp admin config reset all", "dtp.command.help.config_reset_all", true));

    private DtpCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("dtp")
                .executes(DtpCommands::help)
                .then(Commands.literal("help").executes(DtpCommands::help))
                .then(Commands.literal("price").executes(DtpCommands::price))
                .then(Commands.literal("beacon")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("y")
                                .then(Commands.argument("y", IntegerArgumentType.integer())
                                        .executes(ctx -> feedback(ctx, BeaconLogic.moveY(ctx.getSource().getServer().overworld(),
                                                IntegerArgumentType.getInteger(ctx, "y")))))))
                .then(Commands.literal("admin")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("contracts")
                                .then(Commands.literal("reroll").executes(ctx -> {
                                    MinecraftServer server = ctx.getSource().getServer();
                                    ContractBoard board = ContractBoard.get(server);
                                    board.reroll(server);
                                    return feedback(ctx, Component.translatable("dtp.command.contract_rerolled", board.offers().size()));
                                })))
                        .then(Commands.literal("beacon")
                                .then(Commands.literal("reset")
                                        .executes(ctx -> feedback(ctx, BeaconLogic.adminReset(ctx.getSource().getServer().overworld()))))
                                .then(Commands.literal("move")
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(ctx -> feedback(ctx, BeaconLogic.adminMove(ctx.getSource().getServer().overworld(),
                                                        BlockPosArgument.getLoadedBlockPos(ctx, ctx.getSource().getServer().overworld(), "pos")))))))
                        .then(Commands.literal("market")
                                .then(Commands.literal("reset").executes(ctx -> {
                                    MarketData.get(ctx.getSource().getServer()).clearPressure();
                                    return feedback(ctx, Component.translatable("dtp.command.market_reset"));
                                }))
                                .then(Commands.literal("event")
                                        .executes(ctx -> {
                                            int items = MarketEvents.fire(ctx.getSource().getServer());
                                            return feedback(ctx, Component.translatable("dtp.command.event_fired", items));
                                        })
                                        .then(Commands.literal("info").executes(DtpCommands::eventInfo))
                                        .then(Commands.literal("in")
                                                .then(Commands.argument("minutes", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> {
                                                            MinecraftServer server = ctx.getSource().getServer();
                                                            MarketEvents.setCountdown(server, IntegerArgumentType.getInteger(ctx, "minutes"));
                                                            return feedback(ctx, MarketEvents.describeNext(server));
                                                        })))
                                        .then(eventType(MarketEvents.Type.DEMAND_SURGE))
                                        .then(eventType(MarketEvents.Type.FAST_RECOVERY))
                                        .then(Commands.literal("target")
                                                .then(Commands.argument("target", StringArgumentType.word()).suggests(EVENT_TARGETS)
                                                        .executes(DtpCommands::eventTarget)))
                                        .then(Commands.literal("fire")
                                                .then(Commands.literal(MarketEvents.Type.DEMAND_SURGE.id())
                                                        .then(Commands.argument("target", StringArgumentType.word()).suggests(EVENT_TARGETS)
                                                                .executes(ctx -> eventFire(ctx, MarketEvents.Type.DEMAND_SURGE))))
                                                .then(Commands.literal(MarketEvents.Type.FAST_RECOVERY.id())
                                                        .executes(ctx -> {
                                                            int items = MarketEvents.trigger(ctx.getSource().getServer(), MarketEvents.Type.FAST_RECOVERY, "");
                                                            return feedback(ctx, Component.translatable("dtp.command.event_fired", items));
                                                        })))
                                        .then(Commands.literal("end").executes(ctx -> {
                                            int ended = MarketEvents.endRunning(ctx.getSource().getServer());
                                            return feedback(ctx, Component.translatable("dtp.command.event_ended", ended));
                                        })))
                                .then(Commands.literal("advance")
                                        .then(Commands.argument("days", DoubleArgumentType.doubleArg(0.0))
                                                .executes(ctx -> {
                                                    double days = DoubleArgumentType.getDouble(ctx, "days");
                                                    MinecraftServer server = ctx.getSource().getServer();
                                                    // Auch die persönlichen Contract-Uhren der Eingeloggten — sonst altern angenommene Contracts nicht mit.
                                                    MarketData.get(server).advance(days, server.getPlayerList().getPlayers());
                                                    return feedback(ctx, Component.translatable("dtp.command.advanced", days));
                                                }))))
                        .then(Commands.literal("upgrade")
                                .then(Commands.literal("reset").executes(DtpCommands::resetUpgrades))
                                .then(Commands.argument("id", StringArgumentType.word()).suggests(UPGRADES)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                .executes(DtpCommands::setUpgrade))))
                        .then(Commands.literal("playerupgrade")
                                .then(Commands.argument("player", EntityArgument.players())
                                        .then(Commands.literal("reset").executes(DtpCommands::resetPlayerUpgrades))
                                        .then(Commands.argument("id", StringArgumentType.word()).suggests(PLAYER_UPGRADES)
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                        .executes(DtpCommands::setPlayerUpgrade)))))
                        .then(Commands.literal("blackmarket")
                                .then(Commands.literal("status").executes(DtpCommands::traderStatus))
                                .then(Commands.literal("spawn").executes(DtpCommands::traderSpawn)))
                        .then(Commands.literal("config")
                                .then(Commands.literal("diff").executes(DtpCommands::configDiff))
                                .then(Commands.literal("reset")
                                        .executes(DtpCommands::configDiff)
                                        .then(Commands.literal("all")
                                                .executes(ctx -> configReset(ctx, ConfigReset.entries())))
                                        .then(Commands.argument("key", StringArgumentType.word()).suggests(CONFIG_KEYS)
                                                .executes(DtpCommands::configResetKey)))))
        );
    }

    /** Zeigt, wie stark die Einzahlkisten um den Spieler den Schwarzmarkthändler gerade anlocken. */
    private static int traderStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        BlackmarketSpawner.Lure lure = BlackmarketSpawner.lure(player);
        long cooldown = BlackmarketSpawner.cooldownLeft(server);
        return feedback(ctx, Component.translatable("dtp.command.trader_status",
                Money.component(lure.maxChest()), Money.component(lure.pool()),
                String.format(java.util.Locale.ROOT, "%.1f", lure.chance() * 100.0),
                (cooldown + 1199) / 1200));
    }

    /** Erzwingt einen Schwarzmarkthändler beim Spieler, unabhängig von Schwelle und Abklingzeit. */
    private static int traderSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        BlackmarketSpawner.Lure lure = BlackmarketSpawner.lure(player);
        boolean spawned = BlackmarketSpawner.spawn((net.minecraft.server.level.ServerLevel) player.level(), player, lure.richest()).isPresent();
        if (!spawned) {
            ctx.getSource().sendFailure(Component.translatable("dtp.command.trader_spawn_failed"));
            return 0;
        }
        return feedback(ctx, Component.translatable("dtp.command.trader_spawned"));
    }

    /**
     * {@code /dtp admin market event type <typ>}: Typ des geplanten Ereignisses festlegen. Ein Nachfrageschub behält
     * das Ziel, eine Schnelle Erholung gilt allen Waren.
     */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> eventType(MarketEvents.Type type) {
        return Commands.literal("type").then(Commands.literal(type.id()).executes(ctx -> {
            MinecraftServer server = ctx.getSource().getServer();
            MarketEvents.setType(server, type);
            return feedback(ctx, MarketEvents.describeNext(server));
        }));
    }

    /** {@code /dtp admin market event info}: das geplante Ereignis, dazu laufende Ereignisse und Aufschläge aus der Welt. */
    private static int eventInfo(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        feedback(ctx, MarketEvents.describeNext(server));
        List<Component> running = MarketEvents.runningLines(server);
        if (running.isEmpty()) {
            ctx.getSource().sendSystemMessage(Component.translatable("dtp.command.event_none_running").withStyle(ChatFormatting.GRAY));
        } else {
            ctx.getSource().sendSystemMessage(Component.translatable("dtp.event.running_header").withStyle(ChatFormatting.GOLD));
            for (Component line : running) {
                ctx.getSource().sendSystemMessage(line.copy().withStyle(ChatFormatting.GRAY));
            }
        }
        return 1;
    }

    /** {@code /dtp admin market event target <ziel>}: Ziel des geplanten Ereignisses festlegen, Typ und Wartezeit bleiben. */
    private static int eventTarget(CommandContext<CommandSourceStack> ctx) {
        String arg = StringArgumentType.getString(ctx, "target");
        var target = MarketEvents.resolveTarget(arg);
        if (target.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("dtp.command.unknown_event_target", arg));
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
        if (!MarketEvents.setTarget(server, target.get())) {
            ctx.getSource().sendFailure(Component.translatable("dtp.command.event_fast_recovery_all"));
            return 0;
        }
        return feedback(ctx, MarketEvents.describeNext(server));
    }

    /** {@code /dtp admin market event fire demand_surge <ziel>}: Nachfrageschub sofort auslösen, das geplante bleibt unberührt. */
    private static int eventFire(CommandContext<CommandSourceStack> ctx, MarketEvents.Type type) {
        String arg = StringArgumentType.getString(ctx, "target");
        var target = MarketEvents.resolveTarget(arg);
        if (target.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("dtp.command.unknown_event_target", arg));
            return 0;
        }
        int items = MarketEvents.trigger(ctx.getSource().getServer(), type, target.get());
        return feedback(ctx, Component.translatable("dtp.command.event_fired", items));
    }

    private static int feedback(CommandContext<CommandSourceStack> ctx, Component message) {
        ctx.getSource().sendSuccess(() -> message, true);
        return 1;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        boolean admin = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(source);
        source.sendSystemMessage(Component.translatable("dtp.command.help.header").withStyle(ChatFormatting.GOLD));
        for (HelpEntry entry : HELP) {
            if (entry.admin() && !admin) {
                continue;
            }
            source.sendSystemMessage(Component.literal(entry.syntax()).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" – ").withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.translatable(entry.key()).withStyle(ChatFormatting.GRAY)));
        }
        return 1;
    }

    private static int price(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack hand = player.getMainHandItem();
        BlockPos pos = player.blockPosition();
        var quote = Market.quote(player.level(), pos, hand.getItem(), Math.max(1, hand.getCount()));
        if (quote.isEmpty() || TradeList.lookup(hand).isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("dtp.market.not_tradable"));
            return 0;
        }
        Market.Quote q = quote.get();
        ctx.getSource().sendSuccess(() -> Component.translatable("dtp.command.price", hand.getHoverName(),
                Money.format(Math.round(q.unitPrice())), String.format("%.2f", q.factor()), q.count(), Money.component(q.total())), false);
        return 1;
    }

    private static int setUpgrade(CommandContext<CommandSourceStack> ctx) {
        ServerUpgrade upgrade = ServerUpgrade.byId(StringArgumentType.getString(ctx, "id"));
        if (upgrade == null) {
            ctx.getSource().sendFailure(Component.translatable("dtp.command.unknown_upgrade"));
            return 0;
        }
        int level = IntegerArgumentType.getInteger(ctx, "level");
        WorldProgress.get(ctx.getSource().getServer()).setLevel(upgrade, level);
        if (upgrade == ServerUpgrade.BORDER) {
            BeaconLogic.applyBorder(ctx.getSource().getServer());
        }
        return feedback(ctx, Component.translatable("dtp.command.upgrade_set", Component.translatable(upgrade.translationKey()), level));
    }

    private static int setPlayerUpgrade(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        PlayerUpgrade upgrade = PlayerUpgrade.byId(StringArgumentType.getString(ctx, "id"));
        if (upgrade == null) {
            ctx.getSource().sendFailure(Component.translatable("dtp.command.unknown_upgrade"));
            return 0;
        }
        int level = IntegerArgumentType.getInteger(ctx, "level");
        int count = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "player")) {
            PlayerUpgrades.of(player).setLevel(upgrade, level);
            PlayerUpgradeEffects.refresh(player);
            feedback(ctx, Component.translatable("dtp.command.player_upgrade_set", player.getDisplayName(),
                    Component.translatable(upgrade.translationKey()), level));
            count++;
        }
        return count;
    }

    /** Listet die Config-Werte, die vom Standard abweichen — vor einem Zurücksetzen zum Prüfen. */
    private static int configDiff(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<ConfigReset.Entry> changed = ConfigReset.changed();
        if (changed.isEmpty()) {
            source.sendSystemMessage(Component.translatable("dtp.command.config.all_default"));
            return 0;
        }
        source.sendSystemMessage(Component.translatable("dtp.command.config.diff_header", changed.size()).withStyle(ChatFormatting.GOLD));
        for (ConfigReset.Entry e : changed) {
            source.sendSystemMessage(Component.literal(e.key()).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" = " + e.value().get()).withStyle(ChatFormatting.WHITE))
                    .append(Component.translatable("dtp.command.config.default", String.valueOf(e.value().getDefault()))
                            .withStyle(ChatFormatting.GRAY)));
        }
        source.sendSystemMessage(Component.translatable("dtp.command.config.reset_hint").withStyle(ChatFormatting.GRAY));
        return changed.size();
    }

    private static int configResetKey(CommandContext<CommandSourceStack> ctx) {
        String key = StringArgumentType.getString(ctx, "key");
        List<ConfigReset.Entry> match = ConfigReset.entries().stream().filter(e -> e.key().equals(key)).toList();
        if (match.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("dtp.command.config.unknown_key", key));
            return 0;
        }
        return configReset(ctx, match);
    }

    /**
     * Schreibt die Standards in die Config-Datei. Die Grenze folgt sofort, weil sie sonst erst beim nächsten Kauf
     * neu berechnet würde. Werte, die beim Laden eines Datapacks gelesen werden, greifen erst nach {@code /reload} oder einem Neustart.
     */
    private static int configReset(CommandContext<CommandSourceStack> ctx, List<ConfigReset.Entry> entries) {
        List<String> changed = ConfigReset.reset(entries);
        if (changed.isEmpty()) {
            ctx.getSource().sendSystemMessage(Component.translatable("dtp.command.config.all_default"));
            return 0;
        }
        if (changed.contains("beacon.radiusChunks") || changed.contains("beacon.borderEnabled")) {
            BeaconLogic.applyBorder(ctx.getSource().getServer());
        }
        return feedback(ctx, Component.translatable("dtp.command.config.reset", changed.size(), String.join(", ", changed)));
    }

    /** Alle Server-Upgrades auf 0; Grenze und Contract-Aushang folgen sofort. Ausgegebene Taler bleiben weg. */
    private static int resetUpgrades(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        WorldProgress progress = WorldProgress.get(server);
        for (ServerUpgrade upgrade : ServerUpgrade.values()) {
            progress.setLevel(upgrade, 0);
        }
        ServerUpgradeSync.broadcast(server);
        BeaconLogic.applyBorder(server);
        ContractBoard.get(server).reroll(server);
        return feedback(ctx, Component.translatable("dtp.command.upgrades_reset"));
    }

    /** Alle Upgrades der Zielspieler auf 0; Wirkungen werden sofort neu berechnet. */
    private static int resetPlayerUpgrades(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int count = 0;
        for (ServerPlayer player : EntityArgument.getPlayers(ctx, "player")) {
            PlayerUpgrades upgrades = PlayerUpgrades.of(player);
            for (PlayerUpgrade upgrade : PlayerUpgrade.values()) {
                upgrades.setLevel(upgrade, 0);
            }
            PlayerUpgradeEffects.refresh(player);
            feedback(ctx, Component.translatable("dtp.command.player_upgrades_reset", player.getDisplayName()));
            count++;
        }
        return count;
    }
}
