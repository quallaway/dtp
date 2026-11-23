package dev.dtp.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.dtp.Dtp;
import dev.dtp.blackmarket.BlackmarketData;
import dev.dtp.contract.PlayerContracts;
import dev.dtp.data.Versioned;
import dev.dtp.market.Market;
import dev.dtp.market.MarketData;

/**
 * Serverweite Statistik für die Statistik-Tafel: verkaufte Stück und Erlös je Item, insgesamt und je Markttag der
 * letzten {@link MarketData#HISTORY_DAYS} Tage, dazu die abgeschlossenen Contracts je Spieler und Stufe.
 *
 * <p>Verkäufe zählt {@link Market#sell} — die eine Engstelle aller Verkaufswege. Contracts zählt
 * {@link dev.dtp.contract.Contracts}, Altbestände aus {@link PlayerContracts} übernimmt {@link #seed} beim Login.
 */
public class StatisticsData extends SavedData {
    /** Verkaufte Stück und Erlös in Taler. */
    public record Tally(long count, long taler) {
        public static final Codec<Tally> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.fieldOf("count").forGetter(Tally::count),
                Codec.LONG.fieldOf("taler").forGetter(Tally::taler)
        ).apply(i, Tally::new));

        public Tally add(Tally other) {
            return new Tally(count + other.count, taler + other.taler);
        }
    }

    /** Verkäufe eines Markttags. */
    private record Day(int day, Map<Identifier, Tally> sales) {
        static final Codec<Day> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("day").forGetter(Day::day),
                Codec.unboundedMap(Identifier.CODEC, Tally.CODEC).fieldOf("sales").forGetter(Day::sales)
        ).apply(i, (day, sales) -> new Day(day, new HashMap<>(sales))));
    }

    /** Zuletzt gesehener Name und abgeschlossene Contracts je Stufe (Index 0 = Stufe 1). */
    public record PlayerTally(String name, List<Integer> perTier) {
        static final Codec<PlayerTally> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.optionalFieldOf("name", "").forGetter(PlayerTally::name),
                Codec.INT.listOf().optionalFieldOf("per_tier", List.of()).forGetter(PlayerTally::perTier)
        ).apply(i, (name, perTier) -> new PlayerTally(name, new ArrayList<>(perTier))));

        public int completed(int tier) {
            return tier - 1 < perTier.size() ? perTier.get(tier - 1) : 0;
        }

        public int total() {
            return perTier.stream().mapToInt(Integer::intValue).sum();
        }
    }

    private static final Codec<StatisticsData> INNER = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Identifier.CODEC, Tally.CODEC).optionalFieldOf("all_time", Map.of()).forGetter(d -> d.allTime),
            Day.CODEC.listOf().optionalFieldOf("days", List.of()).forGetter(d -> d.days),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerTally.CODEC).optionalFieldOf("players", Map.of()).forGetter(d -> d.players)
    ).apply(i, StatisticsData::new));

    public static final Codec<StatisticsData> CODEC = Versioned.codec(1, INNER, List.of());

    public static final SavedDataType<StatisticsData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Dtp.MODID, "statistics"), StatisticsData::new, CODEC, (DataFixTypes) null);

    private final Map<Identifier, Tally> allTime;
    private final List<Day> days;
    private final Map<UUID, PlayerTally> players;
    /** Zählt jede Änderung hoch; die Statistik-Tafel baut ihren Datenstand nur neu, wenn sie sich ändert. Nicht gespeichert. */
    private int revision;

    public StatisticsData() {
        this(Map.of(), List.of(), Map.of());
    }

    private StatisticsData(Map<Identifier, Tally> allTime, List<Day> days, Map<UUID, PlayerTally> players) {
        this.allTime = new HashMap<>(allTime);
        this.days = new ArrayList<>(days);
        this.players = new HashMap<>(players);
    }

    public static StatisticsData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public int revision() {
        return revision;
    }

    private void changed() {
        revision++;
        setDirty();
    }

    // --- Verkäufe ------------------------------------------------------------------------------

    /** Ein Verkauf am Markt: zählt insgesamt und am heutigen Markttag. */
    public void recordSale(MinecraftServer server, Item item, int count, long taler) {
        if (count <= 0) {
            return;
        }
        Identifier id = Market.itemId(item);
        Tally tally = new Tally(count, taler);
        allTime.merge(id, tally, Tally::add);
        int today = BlackmarketData.today(server);
        prune(today);
        Day day = days.stream().filter(d -> d.day() == today).findFirst().orElse(null);
        if (day == null) {
            day = new Day(today, new HashMap<>());
            days.add(day);
        }
        day.sales().merge(id, tally, Tally::add);
        changed();
    }

    /** Verwirft Markttage, die nicht mehr zu den letzten {@link MarketData#HISTORY_DAYS} gehören. */
    private void prune(int today) {
        if (days.removeIf(d -> d.day() <= today - MarketData.HISTORY_DAYS)) {
            changed();
        }
    }

    /** Alle Verkäufe seit Beginn der Aufzeichnung. */
    public Map<Identifier, Tally> allTimeSales() {
        return Map.copyOf(allTime);
    }

    /** Verkäufe der letzten {@link MarketData#HISTORY_DAYS} Markttage einschließlich heute, je Item summiert. */
    public Map<Identifier, Tally> recentSales(MinecraftServer server) {
        int today = BlackmarketData.today(server);
        Map<Identifier, Tally> result = new HashMap<>();
        for (Day day : days) {
            if (day.day() > today - MarketData.HISTORY_DAYS && day.day() <= today) {
                day.sales().forEach((id, tally) -> result.merge(id, tally, Tally::add));
            }
        }
        return result;
    }

    // --- Contracts -----------------------------------------------------------------------------

    /** Ein abgeschlossener Contract. */
    public void recordContract(UUID player, String name, int tier) {
        if (tier < 1) {
            return;
        }
        PlayerTally tally = players.computeIfAbsent(player, id -> new PlayerTally(name, new ArrayList<>()));
        List<Integer> perTier = tally.perTier();
        while (perTier.size() < tier) {
            perTier.add(0);
        }
        perTier.set(tier - 1, perTier.get(tier - 1) + 1);
        players.put(player, new PlayerTally(name.isEmpty() ? tally.name() : name, perTier));
        changed();
    }

    /**
     * Übernimmt beim ersten Login nach dem Update die Contract-Zähler aus dem Spieler-Attachment. Danach hat der
     * Spieler immer einen Eintrag — ein zweiter Aufruf ändert nichts.
     *
     * @return ob etwas übernommen wurde
     */
    public boolean seed(UUID player, String name, PlayerContracts contracts, int tiers) {
        if (players.containsKey(player)) {
            return false;
        }
        List<Integer> perTier = new ArrayList<>();
        for (int tier = 1; tier <= tiers; tier++) {
            perTier.add(contracts.completed(tier));
        }
        players.put(player, new PlayerTally(name, perTier));
        changed();
        return true;
    }

    /** Merkt sich den aktuellen Namen, damit die Tafel auch Offline-Spieler richtig benennt. */
    public void rememberName(UUID player, String name) {
        PlayerTally tally = players.get(player);
        if (tally != null && !name.isEmpty() && !name.equals(tally.name())) {
            players.put(player, new PlayerTally(name, tally.perTier()));
            changed();
        }
    }

    public Map<UUID, PlayerTally> players() {
        return Map.copyOf(players);
    }
}
