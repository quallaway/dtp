package dev.dtp.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.entity.player.Inventory;

import dev.dtp.DtpConfig;
import dev.dtp.blackmarket.BlackmarketData;
import dev.dtp.market.MarketCategories;
import dev.dtp.market.MarketData;
import dev.dtp.registry.ModBlocks;
import dev.dtp.registry.ModMenus;
import dev.dtp.stats.StatisticsData;
import dev.dtp.upgrade.WorldProgress;

/**
 * Statistik-Tafel: nur lesend, ohne Knöpfe zum Server. Reiter, Zeitraum, Gruppe und Sortierung wählt der Client;
 * der Datenstand enthält darum beide Zeiträume und die Marktgruppen.
 *
 * <p>Der Datenstand wird nur neu gebaut, wenn sich die Statistik, der Markttag, die freigeschalteten Gruppen oder
 * die Contract-Gesamtzahl ändern — gesendet wird er wie überall einmal pro Sekunde.
 */
public class StatisticsMenu extends BlockBoundMenu {
    private CompoundTag cached;
    private String cachedKey = "";

    public StatisticsMenu(int id, Inventory inventory, BlockPos pos) {
        super(ModMenus.STATISTICS_BOARD.get(), id, inventory, pos, ModBlocks.STATISTICS_BOARD.get());
    }

    public static StatisticsMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new StatisticsMenu(id, inventory, buf.readBlockPos());
    }

    @Override
    protected CompoundTag snapshot(ServerLevel level) {
        MinecraftServer server = level.getServer();
        StatisticsData stats = StatisticsData.get(server);
        int total = WorldProgress.get(server).completedContracts();
        String key = stats.revision() + "|" + BlackmarketData.today(server) + "|" + total
                + "|" + MarketCategories.unlocked(server).size();
        if (cached == null || !key.equals(cachedKey)) {
            cached = build(server, stats, total);
            cachedKey = key;
        }
        return cached;
    }

    /** Baut den Datenstand; öffentlich für die GameTests. */
    public static CompoundTag build(MinecraftServer server, StatisticsData stats, int contractsTotal) {
        CompoundTag tag = new CompoundTag();
        tag.put("categories", MarketCategories.snapshot(server));
        tag.putInt("days", MarketData.HISTORY_DAYS);
        tag.put("recent", sales(stats.recentSales(server)));
        tag.put("all", sales(stats.allTimeSales()));

        int tiers = DtpConfig.TIER_VALUE.get().size();
        List<Map.Entry<UUID, StatisticsData.PlayerTally>> players = new ArrayList<>(stats.players().entrySet());
        players.removeIf(e -> e.getValue().total() == 0);
        for (Map.Entry<UUID, StatisticsData.PlayerTally> e : players) {
            tiers = Math.max(tiers, e.getValue().perTier().size());
        }
        players.sort(Comparator.<Map.Entry<UUID, StatisticsData.PlayerTally>>comparingInt(e -> e.getValue().total()).reversed()
                .thenComparing(e -> name(server, e.getKey(), e.getValue()).toLowerCase(Locale.ROOT)));
        ListTag playerList = new ListTag();
        int assigned = 0;
        for (Map.Entry<UUID, StatisticsData.PlayerTally> e : players) {
            CompoundTag p = new CompoundTag();
            p.putString("name", name(server, e.getKey(), e.getValue()));
            ListTag perTier = new ListTag();
            for (int tier = 1; tier <= tiers; tier++) {
                perTier.add(IntTag.valueOf(e.getValue().completed(tier)));
            }
            p.put("tiers", perTier);
            p.putInt("sum", e.getValue().total());
            assigned += e.getValue().total();
            playerList.add(p);
        }
        tag.putInt("tiers", tiers);
        tag.put("players", playerList);
        tag.putInt("contracts_total", Math.max(contractsTotal, assigned));
        // Abschlüsse von Spielern, die sich seit der Statistik-Tafel nicht eingeloggt haben.
        tag.putInt("unassigned", Math.max(0, contractsTotal - assigned));
        return tag;
    }

    private static ListTag sales(Map<Identifier, StatisticsData.Tally> sales) {
        ListTag list = new ListTag();
        sales.forEach((id, tally) -> {
            CompoundTag e = new CompoundTag();
            e.putString("item", id.toString());
            e.putLong("count", tally.count());
            e.putLong("taler", tally.taler());
            list.add(e);
        });
        return list;
    }

    /** Gespeicherter Name, sonst der Namens-Cache des Servers, sonst der Anfang der UUID. */
    private static String name(MinecraftServer server, UUID id, StatisticsData.PlayerTally tally) {
        if (!tally.name().isEmpty()) {
            return tally.name();
        }
        return server.services().nameToIdCache().get(id).map(NameAndId::name).orElse(id.toString().substring(0, 8));
    }
}
