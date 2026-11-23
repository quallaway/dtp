package dev.dtp.contract;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

import dev.dtp.DtpConfig;
import dev.dtp.advancement.DtpAdvancements;
import dev.dtp.currency.DepositChests;
import dev.dtp.currency.Money;
import dev.dtp.currency.Payout;
import dev.dtp.data.Protocol;
import dev.dtp.market.Market;
import dev.dtp.market.MarketData;
import dev.dtp.market.MarketEvents;
import dev.dtp.market.Sales;
import dev.dtp.registry.ModAttachments;
import dev.dtp.stats.StatisticsData;
import dev.dtp.upgrade.PlayerUpgrade;
import dev.dtp.upgrade.PlayerUpgradeDefinitions;
import dev.dtp.upgrade.PlayerUpgrades;
import dev.dtp.upgrade.WorldProgress;

/** Die Aktionen am Bulletin-Board. Alle Prüfungen laufen serverseitig. */
public final class Contracts {
    private Contracts() {
    }

    /** Contract-Daten des Spielers; auf dem Server mit gestarteter persönlicher Uhr. */
    public static PlayerContracts of(Player player) {
        PlayerContracts mine = player.getData(ModAttachments.CONTRACTS);
        if (!mine.started() && player.level() instanceof ServerLevel level) {
            mine.ensureStarted(MarketData.get(level.getServer()).contractDay());
        }
        return mine;
    }

    /** Gleichzeitig annehmbare Contracts: Grundwert plus das Spieler-Upgrade „Mehr Contracts". */
    public static int slots(Player player) {
        return DtpConfig.BASE_SLOTS.get() + (int) PlayerUpgrades.value(player, PlayerUpgrade.CONTRACT_CAPACITY);
    }

    /** Größte mögliche Zahl gleichzeitiger Contracts: Grundwert plus das voll ausgebaute Spieler-Upgrade. */
    public static int maxSlots() {
        PlayerUpgradeDefinitions.Definition def = PlayerUpgradeDefinitions.get(PlayerUpgrade.CONTRACT_CAPACITY);
        return DtpConfig.BASE_SLOTS.get() + (def == null ? 0 : (int) def.value(def.maxLevel()));
    }

    /** Bedingungen der Wertformel für diesen Spieler — mit Quick Delivery höherer Bonus, danach schnellerer Verfall. */
    public static Contract.Terms terms(Player player) {
        if (PlayerUpgrades.level(player, PlayerUpgrade.QUICK_DELIVERY) > 0) {
            return new Contract.Terms(DtpConfig.QUICK_DELIVERY_FACTOR.get(),
                    DtpConfig.DECAY_PER_INTERVAL.get() * DtpConfig.QUICK_DELIVERY_DECAY_MULTIPLIER.get());
        }
        return Contract.Terms.standard();
    }

    /**
     * Nimmt ein Angebot an. Angesprochen wird es über seine ID, nicht über die Listenposition — sonst könnte
     * ein Mitspieler die Liste zwischen Anzeige und Klick verschieben. Danach wird nicht nachgefüllt;
     * die Lücke bleibt bis zur nächsten Rotation.
     */
    public static Component accept(ServerLevel level, BlockPos boardPos, Player player, UUID offerId) {
        MinecraftServer server = level.getServer();
        if (!usable(level, boardPos)) {
            return Component.translatable("dtp.contract.outside");
        }
        ContractBoard board = ContractBoard.get(server);
        Optional<Contract> found = board.offers().stream().filter(c -> c.id().equals(offerId)).findFirst();
        if (found.isEmpty()) {
            return Component.translatable("dtp.contract.gone");
        }
        Contract offer = found.get();
        PlayerContracts mine = of(player);
        if (mine.accepted().size() >= slots(player)) {
            return Component.translatable("dtp.contract.no_slot");
        }
        if (offer.tier() > ContractBoard.serverMaxTier(server)) {
            return Component.translatable("dtp.contract.tier_locked", offer.tier());
        }
        String requirement = offer.requirement(server.registryAccess());
        if (mine.accepted().stream().anyMatch(c -> c.requirement(server.registryAccess()).equals(requirement))) {
            return Component.translatable("dtp.contract.same_item");
        }
        board.take(offer.id());
        mine.accepted().add(offer.rebased(MarketData.get(server).contractDay(), mine.ownDay()));
        sync(player);
        Protocol.log(server, player, "contract_accept", offer.item().toString(), offer.count(), offer.reward());
        return Component.translatable("dtp.contract.accepted");
    }

    /**
     * Annehmen und Liefern gehen nur an einem Board im Territorium — vor dem Setzen des Beacons also gar nicht.
     * Sonst ließe sich die Frühspiel-Grenze umgehen, indem man sich über Contracts finanziert und den Beacon
     * hinauszögert. Außerhalb ist das Board nur lesbar; Abbrechen bleibt erlaubt.
     */
    public static boolean usable(ServerLevel level, BlockPos board) {
        return Market.inTerritory(level, board);
    }

    private static int indexOf(PlayerContracts mine, UUID id) {
        for (int i = 0; i < mine.accepted().size(); i++) {
            if (mine.accepted().get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Liefert passende Items aus den Trade-Chests in Reichweite des Boards an einen angenommenen Contract —
     * in Kistenreihenfolge, bei einer gemeinsamen Ware (Holzstämme) auch gemischt, bis der Contract voll ist.
     * Lieferungen erzeugen keinen Marktdruck — Contract-Items verschwinden, ohne den Preis zu drücken.
     * Die Belohnung geht in die Einzahlkisten in Reichweite; nur was dort keinen Platz findet, ins Inventar.
     */
    public static Component deliver(ServerLevel level, BlockPos board, Player player, UUID contractId) {
        MinecraftServer server = level.getServer();
        if (!usable(level, board)) {
            return Component.translatable("dtp.contract.outside");
        }
        PlayerContracts mine = of(player);
        int index = indexOf(mine, contractId);
        if (index < 0) {
            return Component.translatable("dtp.contract.gone");
        }
        Contract contract = mine.accepted().get(index);
        int taken = Sales.takeForDelivery(level, board, contract::matches, contract.remaining());
        if (taken == 0) {
            return Component.translatable("dtp.contract.nothing_to_deliver", contract.displayName());
        }
        Contract updated = contract.withDelivered(contract.delivered() + taken);
        if (updated.remaining() > 0) {
            mine.accepted().set(index, updated);
            sync(player);
            return Component.translatable("dtp.contract.delivered", taken, updated.delivered(), updated.count());
        }
        long payout = complete(server, player, mine, index, updated);
        long deposited = DepositChests.deposit(level, board, payout, player);
        Component message = deposited >= payout
                ? Component.translatable("dtp.contract.completed", Money.component(payout))
                : Component.translatable("dtp.contract.completed_inventory", Money.component(payout), Money.component(payout - deposited));
        // Auch im Chat, damit die Belohnung nicht übersehen wird, wenn die Oberfläche geschlossen wird.
        player.sendSystemMessage(message);
        return message;
    }

    /**
     * Schließt einen voll belieferten Contract ab — gemeinsam für Board und Fernlieferung: austragen, zählen,
     * Marktereignis näherrücken, protokollieren. Ausgezahlt wird beim Aufrufer.
     *
     * @return der Lohn nach den persönlichen Bedingungen
     */
    private static long complete(MinecraftServer server, Player player, PlayerContracts mine, int index, Contract done) {
        long payout = done.payout(mine.ownDay(), terms(player));
        mine.accepted().remove(index);
        mine.addCompleted(done.tier());
        StatisticsData.get(server).recordContract(server, player.getUUID(), player.getGameProfile().name(), done.tier(), payout);
        WorldProgress progress = WorldProgress.get(server);
        progress.setCompletedContracts(progress.completedContracts() + 1);
        MarketEvents.onContractCompleted(server, done.tier());
        Protocol.log(server, player, "contract_complete", done.item().toString(), done.count(), payout);
        DtpAdvancements.contractCompleted(player);
        sync(player);
        return payout;
    }

    /** Ergebnis einer Fernlieferung: gelieferte Items, abgeschlossene Contracts und der Lohn dafür. */
    public record RemoteResult(int delivered, int completed, long payout) {
    }

    /** Hauptinventar und Hotbar — Rüstung und Zweithand liefert die Fernlieferung nie aus. */
    private static final int MAIN_INVENTORY_SLOTS = 36;

    /**
     * Fernlieferung (Spieler-Upgrade): beliefert alle angenommenen Contracts aus Hauptinventar und Hotbar, egal wo im
     * Territorium. Ohne Board gibt es keine Einzahlkisten — der Lohn geht ins Inventar und ist damit beim Tod
     * verloren wie alle getragenen Taler. Die Prüfung von Upgrade und Abklingzeit liegt beim Aufrufer.
     */
    public static Component deliverRemote(ServerLevel level, Player player) {
        if (!Market.inTerritory(level, player.blockPosition())) {
            return Component.translatable("dtp.contract.outside");
        }
        RemoteResult result = deliverFromInventory(level.getServer(), player);
        if (result.payout() > 0) {
            Payout.toPlayer(player, result.payout());
        }
        if (result.completed() > 0) {
            return Component.translatable("dtp.contract.remote.completed", result.delivered(), result.completed(),
                    Money.component(result.payout()));
        }
        if (result.delivered() > 0) {
            return Component.translatable("dtp.contract.remote.delivered", result.delivered());
        }
        return Component.translatable("dtp.contract.remote.nothing");
    }

    /** Der Kern der Fernlieferung ohne Ortsprüfung und Auszahlung — für Tests direkt aufrufbar. */
    public static RemoteResult deliverFromInventory(MinecraftServer server, Player player) {
        PlayerContracts mine = of(player);
        Inventory inventory = player.getInventory();
        List<Sales.Source> sources = new ArrayList<>();
        for (int i = 0; i < MAIN_INVENTORY_SLOTS && i < inventory.getContainerSize(); i++) {
            sources.add(new Sales.Source(inventory, i));
        }
        int delivered = 0;
        int completed = 0;
        long payout = 0;
        boolean changed = false;
        for (int index = 0; index < mine.accepted().size(); ) {
            Contract contract = mine.accepted().get(index);
            int taken = Sales.takeForDelivery(sources, contract::matches, contract.remaining());
            if (taken == 0) {
                index++;
                continue;
            }
            delivered += taken;
            changed = true;
            Contract updated = contract.withDelivered(contract.delivered() + taken);
            if (updated.remaining() > 0) {
                mine.accepted().set(index, updated);
                index++;
                continue;
            }
            payout += complete(server, player, mine, index, updated);
            completed++;
        }
        if (changed) {
            inventory.setChanged();
            sync(player);
        }
        return new RemoteResult(delivered, completed, payout);
    }

    /** Schickt die angenommenen Contracts an den eigenen Client — für den Tooltip der Auftragsuhr. */
    public static void sync(Player player) {
        ModAttachments.sync(player, ModAttachments.CONTRACTS);
    }

    /**
     * Bricht einen Contract ab — etwa nach einer versehentlichen Annahme. Er kehrt als gewöhnliches Angebot in
     * den Aushang zurück und wird mit der nächsten Rotation ersetzt. Bereits gelieferte Items sind verloren;
     * das ist die einzige Härte im System. Die Bonusfrist läuft weiter, sonst ließe sie sich durch Abbrechen
     * und erneutes Annehmen zurücksetzen. Dazu wird er von der persönlichen Uhr zurück auf die Aushang-Uhr gestempelt.
     */
    public static Component cancel(MinecraftServer server, Player player, UUID contractId) {
        PlayerContracts mine = of(player);
        int index = indexOf(mine, contractId);
        if (index < 0) {
            return Component.translatable("dtp.contract.gone");
        }
        Contract contract = mine.accepted().remove(index);
        sync(player);
        ContractBoard.get(server).returnCancelled(contract.rebased(mine.ownDay(), MarketData.get(server).contractDay()));
        Protocol.log(server, player, "contract_cancel", contract.item().toString(), contract.delivered(), 0);
        return Component.translatable("dtp.contract.cancelled");
    }
}
