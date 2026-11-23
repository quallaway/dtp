package dev.dtp.beacon;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.storage.LevelData;

import dev.dtp.DtpConfig;
import dev.dtp.advancement.DtpAdvancements;
import dev.dtp.contract.ContractBoard;
import dev.dtp.currency.DepositChests;
import dev.dtp.data.Protocol;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModItems;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.ServerUpgradeSync;
import dev.dtp.upgrade.UpgradeCosts;
import dev.dtp.upgrade.WorldProgress;
import dev.dtp.contraband.ContrabandItem;

/**
 * Hauptbeacon, Grenze und Server-Upgrades (docs/20-territorium-und-serverweite-effekte.md, docs/21-beacon-effekte.md).
 *
 * Das Setzen des Beacons beginnt den Durchlauf und braucht deshalb eine Bestätigung in der Oberfläche.
 * Die World-Border wird nur bei Ereignissen gesetzt — beim Start und bei jedem Radius-Upgrade.
 */
public final class BeaconLogic {
    private BeaconLogic() {
    }

    public static void onPlaced(ServerLevel level, BlockPos pos, Player placer) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        BlockPos existing = progress.beaconPos();
        if (level.dimension() != Level.OVERWORLD) {
            refuse(level, pos, placer, Component.translatable("dtp.beacon.overworld_only"));
        } else if (existing != null && !existing.equals(pos) && beaconStillThere(level, existing)) {
            refuse(level, pos, placer, Component.translatable("dtp.beacon.already_exists", existing.toShortString()));
        } else {
            progress.setBeacon(pos, false);
            if (placer != null) {
                placer.sendSystemMessage(Component.translatable("dtp.beacon.placed"));
            }
        }
    }

    /**
     * Steht der bekannte Hauptbeacon noch? Wurde er entfernt (Kreativmodus, /setblock), darf ein neuer gesetzt werden.
     * In nicht geladenen Chunks lässt sich das nicht prüfen — dann gilt er als vorhanden.
     */
    private static boolean beaconStillThere(ServerLevel level, BlockPos pos) {
        return !level.isLoaded(pos) || level.getBlockState(pos).is(ModBlocks.BEACON.get());
    }

    private static void refuse(ServerLevel level, BlockPos pos, Player placer, Component reason) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (placer != null) {
            if (!placer.getAbilities().instabuild) {
                placer.getInventory().add(new ItemStack(ModItems.BEACON.get()));
            }
            placer.sendSystemMessage(reason);
        }
    }

    /** Bestätigt den Durchlauf: Grenze setzen, ab jetzt gilt das Territorium. */
    public static boolean confirm(ServerLevel level, BlockPos pos, Player player) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        if (progress.started() || !pos.equals(progress.beaconPos())) {
            return false;
        }
        progress.setBeacon(pos, true);
        applyBorder(level.getServer());
        setSpawnToBeacon(level.getServer());
        disableSpawnProtection(level.getServer());
        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("dtp.beacon.started", player.getDisplayName(), pos.toShortString()), false);
        Protocol.log(level.getServer(), player, "beacon_start", pos.toShortString(), 0, 0);
        DtpAdvancements.beaconStarted(level.getServer());
        return true;
    }

    /** Bricht eine unbestätigte Platzierung ab und gibt den Block zurück. */
    public static boolean cancel(ServerLevel level, BlockPos pos, Player player) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        if (progress.started() || !pos.equals(progress.beaconPos())) {
            return false;
        }
        progress.setBeacon(null, false);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        if (!player.getAbilities().instabuild) {
            player.getInventory().add(new ItemStack(ModItems.BEACON.get()));
        }
        return true;
    }

    /** Setzt die Vanilla-World-Border auf den aktuellen Stand. Ein manuell gesetzter Wert wird überschrieben. */
    public static void applyBorder(MinecraftServer server) {
        WorldProgress progress = WorldProgress.get(server);
        if (!DtpConfig.BORDER_ENABLED.get() || !progress.started()) {
            return;
        }
        WorldBorder border = server.overworld().getWorldBorder();
        BlockPos pos = progress.beaconPos();
        border.setCenter(pos.getX() + 0.5, pos.getZ() + 0.5);
        if (progress.borderRemoved()) {
            border.setSize(WorldBorder.MAX_SIZE);
        } else {
            border.setSize(progress.territoryRadius() * 2.0 + 1.0);
        }
    }

    /**
     * Welt-Spawn auf den Hauptbeacon: Wer kein Bett mehr hat und neue Spieler landen am Beacon statt womöglich
     * außerhalb der Grenze. Vanilla verteilt den Respawn mit {@code respawnRadius} um diesen Punkt.
     */
    public static void setSpawnToBeacon(MinecraftServer server) {
        BlockPos pos = WorldProgress.get(server).beaconPos();
        if (pos != null) {
            server.setRespawnData(LevelData.RespawnData.of(Level.OVERWORLD, pos.above(), 0.0F, 0.0F));
        }
    }

    /**
     * Schaltet den Vanilla-Spawnschutz ab, sobald der Beacon steht: Der Spawn liegt dann mitten im Territorium, und
     * dort sollen alle bauen dürfen, nicht nur Operatoren. Betrifft nur dedizierte Server (Einzelspieler/LAN haben
     * keinen Spawnschutz). Der Wert wird wie per {@code server.properties} gesetzt und dort auch gespeichert.
     */
    public static void disableSpawnProtection(MinecraftServer server) {
        if (server instanceof DedicatedServer dedicated && dedicated.spawnProtectionRadius() > 0) {
            dedicated.setSpawnProtectionRadius(0);
        }
    }

    /** Serverstart: Spawnschutz aus, falls der Beacon schon bestätigt ist (ältere Welten, manuell wieder gesetzt). */
    public static void disableSpawnProtectionIfStarted(MinecraftServer server) {
        if (WorldProgress.get(server).started()) {
            disableSpawnProtection(server);
        }
    }

    /**
     * Ältere Welten: Liegt der Welt-Spawn außerhalb der Grenze, wird er einmalig auf den Beacon gesetzt. Einen
     * Spawnpunkt, den ein Admin bewusst innerhalb gesetzt hat, lässt das stehen.
     */
    public static void fixSpawnOutsideBorder(MinecraftServer server) {
        WorldProgress progress = WorldProgress.get(server);
        if (!progress.started() || progress.beaconPos() == null) {
            return;
        }
        LevelData.RespawnData spawn = server.getRespawnData();
        if (spawn.dimension() != Level.OVERWORLD || !server.overworld().getWorldBorder().isWithinBounds(spawn.pos())) {
            setSpawnToBeacon(server);
        }
    }

    /** Entfernt die Grenze vollständig, z. B. beim Zurücksetzen des Beacons. */
    public static void clearBorder(MinecraftServer server) {
        WorldBorder border = server.overworld().getWorldBorder();
        border.setCenter(0.0, 0.0);
        border.setSize(WorldBorder.MAX_SIZE);
    }

    public sealed interface BuyResult permits Bought, NotStarted, Maxed, NotConfigured, Insufficient, ContractsMissing, EffectSlotMissing,
            RequirementsMissing {
    }

    public record Bought(int level, long cost) implements BuyResult {
    }

    public record NotStarted() implements BuyResult {
    }

    public record Maxed() implements BuyResult {
    }

    public record NotConfigured() implements BuyResult {
    }

    public record Insufficient(long missing) implements BuyResult {
    }

    /** Für die nächste Contract-Stufe fehlen noch so viele serverweit abgeschlossene Contracts. */
    public record ContractsMissing(int missing) implements BuyResult {
    }

    /** Effekte lassen sich erst kaufen, wenn es mindestens einen Effektslot gibt — sonst wären sie gekauft, aber nutzlos. */
    public record EffectSlotMissing() implements BuyResult {
    }

    /** Ein anderes Server-Upgrade aus {@code requires} fehlt noch. */
    public record RequirementsMissing(List<PlayerUpgradeDefinitions.Requirement> missing) implements BuyResult {
    }

    /** Voraussetzungen aus dem Datapack, die noch fehlen. Verweise auf unbekannte Upgrades gelten als nie erfüllt. */
    public static List<PlayerUpgradeDefinitions.Requirement> missingRequirements(WorldProgress progress, UpgradeCosts.Definition def) {
        List<PlayerUpgradeDefinitions.Requirement> missing = new java.util.ArrayList<>();
        for (PlayerUpgradeDefinitions.Requirement r : def.requires()) {
            ServerUpgrade other = ServerUpgrade.byId(r.upgrade());
            if (other == null || progress.level(other) < r.level()) {
                missing.add(r);
            }
        }
        return missing;
    }

    /** Kauft die nächste Stufe eines Server-Upgrades. Jeder Spieler darf kaufen, bezahlt wird aus den Einzahlkisten. */
    public static BuyResult buy(ServerLevel level, BlockPos beaconPos, Player player, ServerUpgrade upgrade) {
        MinecraftServer server = level.getServer();
        WorldProgress progress = WorldProgress.get(server);
        if (!progress.started()) {
            return new NotStarted();
        }
        UpgradeCosts.Definition def = UpgradeCosts.get(upgrade);
        if (def == null) {
            return new NotConfigured();
        }
        int current = progress.level(upgrade);
        if (current >= def.maxLevel()) {
            return new Maxed();
        }
        if (upgrade.isEffect() && progress.effectSlots() < 1) {
            return new EffectSlotMissing();
        }
        List<PlayerUpgradeDefinitions.Requirement> unmet = missingRequirements(progress, def);
        if (!unmet.isEmpty()) {
            return new RequirementsMissing(unmet);
        }
        if (upgrade == ServerUpgrade.CONTRACT_TIER && progress.missingForNextContractTier() > 0) {
            return new ContractsMissing(progress.missingForNextContractTier());
        }
        long cost = def.cost(current + 1);
        DepositChests.PayResult paid = DepositChests.pay(level, beaconPos, cost, DtpConfig.BEACON_DEPOSIT_RADIUS.get());
        if (paid instanceof DepositChests.Insufficient(long missing)) {
            return new Insufficient(missing);
        }
        progress.setLevel(upgrade, current + 1);
        ServerUpgradeSync.broadcast(server);
        if (upgrade == ServerUpgrade.BORDER) {
            applyBorder(server);
        }
        if (upgrade == ServerUpgrade.CONTRACT_TIER) {
            // Neue Stufe sofort im Aushang, nicht erst mit der nächsten Rotation.
            ContractBoard.get(server).reroll(server);
        }
        server.getPlayerList().broadcastSystemMessage(Component.translatable("dtp.upgrade.bought",
                player.getDisplayName(), Component.translatable(upgrade.translationKey()), current + 1), false);
        Protocol.log(server, player, "buy_upgrade", upgrade.id, current + 1, cost);
        DtpAdvancements.serverUpgrade(server, upgrade, current + 1);
        return new Bought(current + 1, cost);
    }

    // --- Laufende Wirkungen (im Intervall, nicht pro Tick) ----------------------------------

    /**
     * Verteilt die Effekte an Spieler in Reichweite des Hauptbeacons (Oberwelt) und der Neben-Beacons (alle
     * Dimensionen). Neben-Beacons, deren Block nicht mehr steht, werden dabei aus der Liste entfernt.
     */
    public static void applyEffects(MinecraftServer server) {
        WorldProgress progress = WorldProgress.get(server);
        if (!DtpConfig.EFFECTS_ENABLED.get()) {
            return;
        }
        for (GlobalPos relay : List.copyOf(progress.relays())) {
            ServerLevel level = server.getLevel(relay.dimension());
            // Eine Dimension, die es nicht mehr gibt (etwa eine entfernte Mod), räumt den Eintrag ebenso ab.
            if (level == null || level.isLoaded(relay.pos()) && !level.getBlockState(relay.pos()).is(ModBlocks.RELAY_BEACON.get())) {
                progress.removeRelay(relay);
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (ServerUpgrade effect : effectsAt(server, player.level().dimension(), player.blockPosition())) {
                if (!effect.isVanillaEffect()) {
                    continue; // eigene Effekte wirken über Events, nicht am Spieler
                }
                // 400 Ticks: Beim Auffrischen alle 80 Ticks bleibt die Dauer über 10 s, sonst flackert Nachtsicht.
                player.addEffect(new MobEffectInstance(effect.effect, 400, 0, true, false));
            }
        }
    }

    /**
     * Server-Upgrade Mending-Aura: Spieler im Beacon-Gebiet ({@link #inBeaconArea}) bekommen das ganze Inventar
     * langsam repariert, ohne Erfahrung — Grundpflege, kein Ersatz für Vanilla-Mending. Läuft alle
     * {@value #MENDING_INTERVAL_TICKS} Ticks über alle Spieler, nie über Blöcke. Mehrfach abgedeckt (Hauptbeacon und
     * Neben-Beacon) wirkt sie einmal. Stufe II repariert mehr je Takt.
     */
    public static void applyMendingAura(MinecraftServer server) {
        int level = WorldProgress.get(server).level(ServerUpgrade.MENDING_AURA);
        if (level == 0) {
            return;
        }
        int repair = mendingAuraRepair(level);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator() && inBeaconArea(player.level(), player.blockPosition())) {
                mend(player, repair);
            }
        }
    }

    /** Haltbarkeitspunkte je Takt der Mending-Aura auf {@code level} (Stufe I oder II, siehe Welt-Config). */
    public static int mendingAuraRepair(int level) {
        return level >= 2 ? DtpConfig.MENDING_AURA_REPAIR_2.get() : DtpConfig.MENDING_AURA_REPAIR.get();
    }

    /** Ein Takt der Mending-Aura für einen Spieler: jedes beschädigte Item im Inventar um {@code repair} Punkte. */
    public static void mend(Player player, int repair) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            // Contraband-Items laden nur mit Talern auf, nie an der Aura — auch falls eins doch Haltbarkeit trägt.
            if (stack.isDamaged() && !stack.is(ContrabandItem.TAG)) {
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
            }
        }
    }

    /** Takt von {@link #applyMendingAura} in Ticks (10 s, siehe GameEvents). */
    public static final int MENDING_INTERVAL_TICKS = 200;

    /**
     * Beacon-Gebiet der ortsgebundenen Server-Upgrades ohne Slot (Mending-Aura, Ofentempo): das Effektgebiet des
     * Hauptbeacons und der Radius jedes Neben-Beacons — dieselben Flächen wie bei den Beacon-Effekten, aber ohne
     * Auswahl: Jeder Neben-Beacon trägt alle gekauften Upgrades dieser Art. Nur auf dem Server, sonst false.
     */
    public static boolean inBeaconArea(Level level, BlockPos pos) {
        return level instanceof ServerLevel server && inBeaconArea(server.getServer(), level.dimension(), pos);
    }

    /** Wie {@link #inBeaconArea(Level, BlockPos)} mit ausdrücklicher Dimension. */
    public static boolean inBeaconArea(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
        WorldProgress progress = WorldProgress.get(server);
        return inMainArea(progress, dimension, pos) || inRelayRange(progress, dimension, pos);
    }

    /** Wie {@link #effectsAt(MinecraftServer, ResourceKey, BlockPos)} für die Oberwelt. */
    public static Set<ServerUpgrade> effectsAt(MinecraftServer server, BlockPos pos) {
        return effectsAt(server, Level.OVERWORLD, pos);
    }

    /**
     * Alle Effekte, die an einer Position wirken. In der Oberwelt die des Hauptbeacons im Gebiet der höchsten
     * Grenzstufe (quadratisch wie die Grenze, damit auch ihre Ecken abgedeckt sind). In jeder Dimension die jedes
     * Neben-Beacons derselben Dimension in dessen Radius, immer: ohne Sperre, unabhängig von Durchlauf und Territorium.
     * Gleiche Effekte stapeln sich nicht: Mehrfach abgedeckt zählt jeder Effekt einmal. Die Höhe zählt nie.
     */
    public static Set<ServerUpgrade> effectsAt(MinecraftServer server, ResourceKey<Level> dimension, BlockPos pos) {
        WorldProgress progress = WorldProgress.get(server);
        Set<ServerUpgrade> effects = EnumSet.noneOf(ServerUpgrade.class);
        if (inMainArea(progress, dimension, pos)) {
            effects.addAll(progress.activeEffects());
        }
        int relayRadius = DtpConfig.RELAY_RADIUS_CHUNKS.get() * 16;
        for (GlobalPos relay : progress.relays()) {
            if (relay.dimension() == dimension && within(pos, relay.pos(), relayRadius)) {
                effects.addAll(progress.relayEffects(relay));
            }
        }
        return effects;
    }

    /**
     * Liegt die Position im Radius eines Neben-Beacons derselben Dimension? Unabhängig von dessen Effektauswahl,
     * vom Durchlauf und vom Territorium.
     */
    public static boolean inRelayRange(ServerLevel level, BlockPos pos) {
        return inRelayRange(WorldProgress.get(level.getServer()), level.dimension(), pos);
    }

    private static boolean inRelayRange(WorldProgress progress, ResourceKey<Level> dimension, BlockPos pos) {
        int relayRadius = DtpConfig.RELAY_RADIUS_CHUNKS.get() * 16;
        for (GlobalPos relay : progress.relays()) {
            if (relay.dimension() == dimension && within(pos, relay.pos(), relayRadius)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Effektgebiet des Hauptbeacons: Oberwelt, nach dem Start, Quadrat der höchsten Grenzstufe (wie die Grenze, damit
     * auch ihre Ecken abgedeckt sind) — unabhängig von der gekauften Grenzstufe. Die Höhe zählt nie.
     */
    private static boolean inMainArea(WorldProgress progress, ResourceKey<Level> dimension, BlockPos pos) {
        BlockPos beacon = progress.beaconPos();
        if (dimension != Level.OVERWORLD || !progress.started() || beacon == null) {
            return false;
        }
        int main = WorldProgress.mainEffectRadius();
        return Math.abs(pos.getX() - beacon.getX()) <= main && Math.abs(pos.getZ() - beacon.getZ()) <= main;
    }

    /**
     * Wirkt ein eigener Beacon-Effekt an dieser Stelle? Gemeinsamer Wächter der Event-Hooks: nur auf dem Server und nur
     * mit eingeschalteten Effekten; die Dimension kommt aus dem Level.
     */
    public static boolean effectAt(Level level, BlockPos pos, ServerUpgrade effect) {
        return level instanceof ServerLevel server && DtpConfig.EFFECTS_ENABLED.get()
                && effectsAt(server.getServer(), level.dimension(), pos).contains(effect);
    }

    /**
     * Reichweite eines Neben-Beacons als Zylinder: kreisförmig, unbegrenzt in der Höhe — Effekte wirken auch tief
     * unter der Basis, wo gearbeitet wird (docs/21-beacon-effekte.md).
     */
    private static boolean within(BlockPos pos, BlockPos center, int radius) {
        long dx = pos.getX() - center.getX();
        long dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (long) radius * radius;
    }

    /** Deepslate-Staffelung: unter y = 0 ohne Freischaltung wirkt Darkness. Nichts ist verboten, es lohnt nur nicht. */
    public static void applyDeepslateDarkness(MinecraftServer server) {
        if (!DtpConfig.DEEPSLATE_DARKNESS.get() || WorldProgress.get(server).level(ServerUpgrade.DEEPSLATE) > 0) {
            return;
        }
        for (ServerPlayer player : server.overworld().players()) {
            if (player.getY() < 0 && !player.isSpectator() && !player.isCreative()) {
                player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
            }
        }
    }

    // --- Befehle ----------------------------------------------------------------------------

    /** Verschiebt den Beacon auf der Y-Achse. Die Grenze bleibt unberührt, weil nur X und Z zählen. */
    public static Component moveY(ServerLevel level, int y) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        BlockPos from = progress.beaconPos();
        if (from == null) {
            return Component.translatable("dtp.beacon.none");
        }
        BlockPos to = new BlockPos(from.getX(), y, from.getZ());
        if (level.isOutsideBuildHeight(to)) {
            return Component.translatable("dtp.beacon.move_invalid");
        }
        if (!level.getBlockState(to).isAir()) {
            return Component.translatable("dtp.beacon.move_blocked", to.toShortString());
        }
        level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        level.setBlock(to, ModBlocks.BEACON.get().defaultBlockState(), Block.UPDATE_ALL);
        progress.setBeacon(to, progress.started());
        return Component.translatable("dtp.beacon.moved", to.toShortString());
    }

    /** Admin-Notausgang: Beacon an eine neue Position setzen; die Grenze folgt. */
    public static Component adminMove(ServerLevel level, BlockPos to) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        BlockPos from = progress.beaconPos();
        if (from != null && level.getBlockState(from).is(ModBlocks.BEACON.get())) {
            level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        level.setBlock(to, ModBlocks.BEACON.get().defaultBlockState(), Block.UPDATE_ALL);
        progress.setBeacon(to, progress.started() || from == null);
        applyBorder(level.getServer());
        return Component.translatable("dtp.beacon.moved", to.toShortString());
    }

    /** Admin-Notausgang: Beacon entfernen und Grenze aufheben. Server-Upgrades bleiben erhalten. */
    public static Component adminReset(ServerLevel level) {
        WorldProgress progress = WorldProgress.get(level.getServer());
        BlockPos from = progress.beaconPos();
        if (from != null && level.getBlockState(from).is(ModBlocks.BEACON.get())) {
            level.setBlock(from, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        progress.setBeacon(null, false);
        clearBorder(level.getServer());
        return Component.translatable("dtp.beacon.reset");
    }
}
