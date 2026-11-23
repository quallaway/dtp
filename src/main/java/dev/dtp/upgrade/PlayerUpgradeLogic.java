package dev.dtp.upgrade;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import dev.dtp.advancement.DtpAdvancements;
import dev.dtp.beacon.BeaconLogic;
import dev.dtp.currency.DepositChests;
import dev.dtp.data.Protocol;
import dev.dtp.market.Market;

/**
 * Kauf von Spieler-Upgrades an der Werkstatt (docs/30-persoenliche-upgrades.md).
 * Bezahlt wird aus den Einzahlkisten um die Werkstatt — wie am Beacon, nur dass das Ergebnis dem Käufer gehört.
 */
public final class PlayerUpgradeLogic {
    private PlayerUpgradeLogic() {
    }

    public sealed interface BuyResult permits Bought, Outside, NotConfigured, Maxed, RequirementsMissing, Insufficient, ModuleSlotMissing {
    }

    public record Bought(int level, long cost) implements BuyResult {
    }

    /** Die Werkstatt steht außerhalb des Territoriums oder der Durchlauf hat noch nicht begonnen. */
    public record Outside() implements BuyResult {
    }

    public record NotConfigured() implements BuyResult {
    }

    public record Maxed() implements BuyResult {
    }

    public record RequirementsMissing(List<PlayerUpgradeDefinitions.Requirement> missing) implements BuyResult {
    }

    public record Insufficient(long missing) implements BuyResult {
    }

    /** Module lassen sich erst kaufen, wenn ein Modulslot gekauft ist — sonst wären sie bezahlt, aber wirkungslos. */
    public record ModuleSlotMissing() implements BuyResult {
    }

    public sealed interface ToggleResult permits Equipped, Unequipped, ToggleOutside, Locked, NoFreeSlot {
    }

    public record Equipped() implements ToggleResult {
    }

    public record Unequipped() implements ToggleResult {
    }

    /** Die Werkstatt steht weder im Territorium noch im Radius eines Neben-Beacons. */
    public record ToggleOutside() implements ToggleResult {
    }

    /** Kein Modul oder noch nicht gekauft. */
    public record Locked() implements ToggleResult {
    }

    public record NoFreeSlot() implements ToggleResult {
    }

    /**
     * Gekauft wird nur an einer Werkstatt im Territorium — vor dem Setzen des Beacons also gar nicht.
     * Sonst ließe sich die Frühspiel-Grenze umgehen, indem man sich vorab persönlich ausbaut.
     */
    public static boolean usable(ServerLevel level, BlockPos table) {
        return Market.inTerritory(level, table);
    }

    /**
     * Module umstecken geht an einer Werkstatt im Territorium oder im Radius eines Neben-Beacons — so lassen sich
     * Module auch an einer Außenbasis wechseln. Kaufen bleibt aufs Territorium beschränkt ({@link #usable}).
     */
    public static boolean modulesUsable(ServerLevel level, BlockPos table) {
        return usable(level, table) || BeaconLogic.inRelayRange(level, table);
    }

    /** Voraussetzungen, die der Spieler noch nicht erfüllt. Verweise auf unbekannte Upgrades gelten als nie erfüllt. */
    public static List<PlayerUpgradeDefinitions.Requirement> missingRequirements(Player player, PlayerUpgradeDefinitions.Definition def) {
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        List<PlayerUpgradeDefinitions.Requirement> missing = new ArrayList<>();
        for (PlayerUpgradeDefinitions.Requirement r : def.requires()) {
            PlayerUpgrade other = PlayerUpgrade.byId(r.upgrade());
            if (other == null || mine.level(other) < r.level()) {
                missing.add(r);
            }
        }
        return missing;
    }

    /** Kauft die nächste Stufe für den Spieler. Alle Prüfungen laufen serverseitig. */
    public static BuyResult buy(ServerLevel level, BlockPos table, Player player, PlayerUpgrade upgrade) {
        if (!usable(level, table)) {
            return new Outside();
        }
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(upgrade);
        if (def == null) {
            return new NotConfigured();
        }
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        int current = mine.level(upgrade);
        if (current >= def.maxLevel()) {
            return new Maxed();
        }
        List<PlayerUpgradeDefinitions.Requirement> missing = missingRequirements(player, def);
        if (!missing.isEmpty()) {
            return new RequirementsMissing(missing);
        }
        if (upgrade.isModule() && mine.moduleSlots() == 0) {
            return new ModuleSlotMissing();
        }
        long cost = def.cost(current + 1);
        if (DepositChests.pay(level, table, cost) instanceof DepositChests.Insufficient(long lacking)) {
            return new Insufficient(lacking);
        }
        mine.setLevel(upgrade, current + 1);
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerUpgradeEffects.refresh(serverPlayer);
        }
        Protocol.log(level.getServer(), player, "buy_player_upgrade", upgrade.id, current + 1, cost);
        DtpAdvancements.playerUpgrade(player, upgrade, current + 1);
        return new Bought(current + 1, cost);
    }

    /**
     * Rüstet ein Modul an der Werkstatt aus oder ab. Umstecken ist kostenlos und jederzeit möglich; gebunden ist es
     * nur an eine Werkstatt im Territorium oder im Radius eines Neben-Beacons. Ausgerüstete Module wirken überall.
     */
    public static ToggleResult toggleModule(ServerLevel level, BlockPos table, Player player, PlayerUpgrade module) {
        if (!modulesUsable(level, table)) {
            return new ToggleOutside();
        }
        PlayerUpgrades mine = PlayerUpgrades.of(player);
        if (!module.isModule() || mine.level(module) == 0) {
            return new Locked();
        }
        boolean wasEquipped = mine.isEquipped(module);
        if (!mine.toggleModule(module)) {
            return new NoFreeSlot();
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerUpgradeEffects.refresh(serverPlayer);
        }
        Protocol.log(level.getServer(), player, wasEquipped ? "unequip_module" : "equip_module", module.id, mine.level(module), 0);
        return wasEquipped ? new Unequipped() : new Equipped();
    }
}
