package dev.dtp.market;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import org.jspecify.annotations.Nullable;

import dev.dtp.DtpConfig;
import dev.dtp.stats.StatisticsData;
import dev.dtp.upgrade.ServerUpgrade;
import dev.dtp.upgrade.WorldProgress;

/**
 * Preisbildung nach docs/11-marktdruck.md:
 *
 * <pre>
 * Preisfaktor = clamp( 2^(-s/H), faktorMin, faktorMax )
 * Marktwert   = Basiswert × Preisfaktor
 * </pre>
 *
 * Außerhalb des Territoriums — oder bevor der Durchlauf begonnen hat — gilt die Untergrenze des Preisbands.
 *
 * Marktdruck und Preisverlauf gehören zur <b>Ware</b> ({@link TradeList#marketKey}), nicht zum Item: Items einer
 * gemeinsamen Ware teilen sich den Druck, der Basiswert bleibt je Item.
 */
public final class Market {
    private Market() {
    }

    /**
     * Ein Angebot für eine Menge; {@code total} ist exakt der Betrag, der beim Verkauf ausgezahlt wird.
     * {@code rising}: der Preis erholt sich nach oben; {@code falling}: er liegt nach einem Marktereignis über dem
     * Ruhepreis und sinkt dorthin zurück.
     */
    public record Quote(Item item, int count, long total, double unitPrice, double factor, boolean rising, boolean falling, boolean outside) {
    }

    /**
     * Parameter eines Items unter Berücksichtigung von Spielerzahl, Server-Upgrades und Marktereignissen.
     * {@code windows}: Fenster einer Schnellen Erholung, in denen die Erholung von unten doppelt zählt;
     * {@code bonus}: Aufschlag aus der Welt (Vollmond, Regen) als Multiplikator auf den Preisfaktor, 1 = keiner;
     * {@code share}: Anteil des Marktdrucks, den ein verkauftes Stück erzeugt — kleiner 1, solange ein Fenster läuft.
     */
    private record Params(double base, double h, double t, double sFloor, List<MarketData.Window> windows, double bonus, double share) {
    }

    /** Ein Aufschlag aus der Welt, der gerade gilt: Ereignis ({@code full_moon}, {@code rain}), Warengruppe und Höhe. */
    public record WorldBonus(String event, Identifier category, double bonus) {
        public TagKey<Item> tag() {
            return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(category.getNamespace(), "market_category/" + category.getPath()));
        }
    }

    private static Params params(ServerLevel level, Item item, TradeList.Entry entry) {
        MarketData data = MarketData.get(level.getServer());
        WorldProgress progress = WorldProgress.get(level.getServer());
        double players = data.activePlayers();
        double h = entry.halfAmount()
                * (1.0 + (players - 1.0) * DtpConfig.PLAYER_SCALING.get())
                * (1.0 + progress.level(ServerUpgrade.PRICE_STABILITY) * DtpConfig.STABILITY_PER_LEVEL.get());
        double t = entry.recoveryDays()
                * Math.max(0.2, 1.0 - progress.level(ServerUpgrade.PRICE_RECOVERY) * DtpConfig.RECOVERY_PER_LEVEL.get());
        List<MarketData.Window> windows = recoveryWindows(data, item);
        return new Params(entry.base(), h, t, -h * DtpConfig.FLOOR_RATIO.get(), windows, bonus(level, item), share(data, windows));
    }

    /** Druckanteil je verkauftem Stück: {@code events.fastRecovery.pressureShare}, solange ein Fenster läuft, sonst 1. */
    private static double share(MarketData data, List<MarketData.Window> windows) {
        double day = data.day();
        for (MarketData.Window w : windows) {
            if (day >= w.start() && day < w.end()) {
                return DtpConfig.EVENT_FAST_RECOVERY_PRESSURE_SHARE.get();
            }
        }
        return 1.0;
    }

    // --- Schnelle Erholung -------------------------------------------------------------------

    /** Fenster der laufenden Schnellen Erholungen, die dieses Item betreffen (Ziel {@code ""} = alle Waren). */
    private static List<MarketData.Window> recoveryWindows(MarketData data, Item item) {
        List<MarketData.Window> windows = new ArrayList<>();
        for (MarketData.RunningEvent e : data.running()) {
            if (e.type().equals(MarketEvents.Type.FAST_RECOVERY.id())) {
                TagKey<Item> tag = MarketEvents.tag(e.target());
                // Alte Stände führen noch eine Gruppe als Ziel; eine unbekannte Gruppe betrifft nichts.
                if (e.target().isEmpty() || (tag != null && item.builtInRegistryHolder().is(tag))) {
                    windows.add(new MarketData.Window(e.start(), e.end()));
                }
            }
        }
        return windows;
    }

    /**
     * Schnelle Erholung: Für die Gruppe ({@code null} = alle Waren) zählt Marktzeit bis {@code days} Markttage von
     * jetzt doppelt — das halbiert T, aber nur für Preise unter dem Ruhepreis. Verkäufe erzeugen in dieser Zeit nur
     * {@code events.fastRecovery.pressureShare} des Drucks. Ein laufendes Ereignis desselben Typs wird vorher
     * beendet. Gibt die Zahl der betroffenen Waren zurück.
     */
    public static int startFastRecovery(ServerLevel level, @Nullable TagKey<Item> tag, String target, double days) {
        MarketData data = MarketData.get(level.getServer());
        for (MarketData.RunningEvent e : data.running()) {
            if (e.type().equals(MarketEvents.Type.FAST_RECOVERY.id())) {
                endFastRecovery(level, e);
            }
        }
        int[] count = {0};
        forEachGood(level, tag, (item, entry) -> {
            record(level, item);
            count[0]++;
        });
        data.startRunning(new MarketData.RunningEvent(MarketEvents.Type.FAST_RECOVERY.id(), target, data.day(), data.day() + days));
        return count[0];
    }

    /**
     * Beendet eine Schnelle Erholung: Jede Ware der Gruppe mit gespeichertem Druck wird auf ihren exakten Stand
     * (noch mit Fenster) gesetzt, erst dann fällt das Fenster weg — so springt kein Preis, auch nicht nach Schlafen.
     */
    public static void endFastRecovery(ServerLevel level, MarketData.RunningEvent event) {
        MarketData data = MarketData.get(level.getServer());
        TagKey<Item> tag = MarketEvents.tag(event.target());
        if (tag != null || event.target().isEmpty()) {
            forEachGood(level, tag, (item, entry) -> {
                Identifier key = marketKey(item);
                if (data.hasPressure(key)) {
                    data.setPressure(key, pressure(level, item, params(level, item, entry)));
                }
            });
        }
        data.removeRunning(event);
    }

    // --- Aufschläge aus der Welt -------------------------------------------------------------

    /** Die Aufschläge aus der Welt, die gerade gelten — leer, wenn Marktereignisse abgeschaltet sind. */
    public static List<WorldBonus> activeBonuses(MinecraftServer server) {
        if (!DtpConfig.EVENTS_ENABLED.get()) {
            return List.of();
        }
        ServerLevel overworld = server.overworld();
        List<WorldBonus> bonuses = new ArrayList<>();
        double moon = DtpConfig.EVENT_FULL_MOON_BONUS.get();
        if (moon > 0 && fullMoonNight(overworld)) {
            bonuses.add(new WorldBonus("full_moon", Identifier.fromNamespaceAndPath(dev.dtp.Dtp.MODID, "mob_drops"), moon));
        }
        double rain = DtpConfig.EVENT_RAIN_BONUS.get();
        if (rain > 0 && overworld.getWeatherData().isRaining()) {
            bonuses.add(new WorldBonus("rain", Identifier.fromNamespaceAndPath(dev.dtp.Dtp.MODID, "fishing"), rain));
        }
        return bonuses;
    }

    /** Vollmondphase und Nacht (Tageszeit 13.000 bis 23.000 Ticks) in der Oberwelt. */
    public static boolean fullMoonNight(ServerLevel overworld) {
        // Die Mondphase ist ein Positionsattribut; der Aufschlag gilt serverweit, gelesen wird am Weltspawn.
        BlockPos spawn = overworld.getRespawnData().pos();
        return fullMoonNight(overworld.getOverworldClockTime(), overworld.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, spawn));
    }

    /** Vollmondnacht zu dieser Uhrzeit der Oberwelt und Mondphase: Nacht heißt Tageszeit 13.000 bis 23.000 Ticks. */
    public static boolean fullMoonNight(long clockTime, MoonPhase phase) {
        long time = Math.floorMod(clockTime, 24000L);
        return phase == MoonPhase.FULL_MOON && time >= 13000L && time < 23000L;
    }

    /** Multiplikator aus allen Aufschlägen, die für dieses Item gelten. */
    private static double bonus(ServerLevel level, Item item) {
        double bonus = 1.0;
        for (WorldBonus b : activeBonuses(level.getServer())) {
            if (item.builtInRegistryHolder().is(b.tag())) {
                bonus *= 1.0 + b.bonus();
            }
        }
        return bonus;
    }

    /**
     * Ruft {@code action} für jede handelbare Ware der Gruppe einmal auf ({@code null} = alle Waren). Eine gemeinsame
     * Ware zählt einmal, nicht einmal je Item.
     */
    private static void forEachGood(ServerLevel level, @Nullable TagKey<Item> tag, BiConsumer<Item, TradeList.Entry> action) {
        Set<Identifier> done = new HashSet<>();
        for (Item item : TradeList.allTradable()) {
            if (tag != null && !item.getDefaultInstance().is(tag)) {
                continue;
            }
            Optional<TradeList.Entry> entry = TradeList.lookup(item);
            if (entry.isEmpty() || !done.add(TradeList.keyOf(item, entry.get()))) {
                continue;
            }
            action.accept(item, entry.get());
        }
    }

    /**
     * Marktereignis in zwei Schritten:
     * <ol>
     * <li>Gesättigte Waren rücken um {@code share} an den Grundwert heran (0,5 = halber Abstand).</li>
     * <li>Danach sinkt der Marktdruck um {@code boost} × H unter diesen Stand — auch unter den Grundwert. Der Preis
     *     steigt damit über den Ruhepreis (bei 0,5: von 141 % auf 200 %) und fällt mit der Zeit dorthin zurück,
     *     schneller, wenn verkauft wird.</li>
     * </ol>
     * Erfasst werden alle handelbaren Waren der Gruppe, auch nie verkaufte. Mehrere Ereignisse hintereinander
     * stapeln sich höchstens bis zur Preisobergrenze.
     *
     * @param tag Warengruppe, {@code null} = alle Waren
     * @return Zahl der betroffenen Waren
     */
    public static int relievePressure(ServerLevel level, @Nullable TagKey<Item> tag, double share, double boost) {
        MarketData data = MarketData.get(level.getServer());
        int[] count = {0};
        // Eine gemeinsame Ware wird nur einmal entlastet, nicht einmal je Item.
        forEachGood(level, tag, (item, entry) -> {
            Params p = params(level, item, entry);
            double s = pressure(level, item, p);
            double relieved = s > p.sFloor() ? p.sFloor() + (s - p.sFloor()) * (1.0 - share) : s;
            // Unter diesem Druck greift ohnehin die Preisobergrenze — tiefer zu gehen verlängerte nur den Abbau.
            double lowest = -p.h() * Math.log(DtpConfig.FACTOR_MAX.get()) / Math.log(2.0);
            record(level, item);
            data.setPressure(TradeList.keyOf(item, entry), Math.max(lowest, relieved - boost * p.h()));
            record(level, item);
            count[0]++;
        });
        return count[0];
    }

    private static double factor(double s, Params p) {
        double f = Math.pow(2.0, -s / p.h()) * p.bonus();
        return Math.clamp(f, DtpConfig.FACTOR_MIN.get(), DtpConfig.FACTOR_MAX.get());
    }

    /**
     * Position eines Preisfaktors im Preisband, 0 = Untergrenze, 1 = Obergrenze — für die Druckanzeige.
     * Linear, genau wie die Höhe im Preisverlauf — so liegt ein Preis im Balken und im Graphen an derselben Stelle.
     */
    public static double bandPosition(double factor) {
        double min = DtpConfig.FACTOR_MIN.get();
        double max = DtpConfig.FACTOR_MAX.get();
        if (max <= min) {
            return 0.5;
        }
        return Math.clamp((factor - min) / (max - min), 0.0, 1.0);
    }

    /** Handelt dieser Ort zum vollen Markt? Nur in der Oberwelt innerhalb des Territoriums. */
    public static boolean inTerritory(ServerLevel level, BlockPos pos) {
        return level.dimension() == Level.OVERWORLD && WorldProgress.get(level.getServer()).inTerritory(pos);
    }

    /**
     * Preis für {@code count} Einheiten, verkauft an einem Block bei {@code pos}. Der Erlös wird Einheit
     * für Einheit aufsummiert, weil der Preis während des Verkaufs sinkt.
     */
    public static Optional<Quote> quote(ServerLevel level, BlockPos pos, Item item, int count) {
        return quote(level, pos, item, count, 0.0);
    }

    /**
     * Wie {@link #quote(ServerLevel, BlockPos, Item, int)}, aber so, als wären vorher schon {@code extraPressure}
     * Einheiten derselben Ware verkauft worden — für Vorschauen über mehrere Items einer gemeinsamen Ware.
     */
    public static Optional<Quote> quote(ServerLevel level, BlockPos pos, Item item, int count, double extraPressure) {
        Optional<TradeList.Entry> entry = TradeList.lookup(item);
        if (entry.isEmpty() || count <= 0) {
            return Optional.empty();
        }
        Params p = params(level, item, entry.get());
        double s = pressure(level, item, p) + extraPressure * p.share();
        double currentFactor = factor(s, p);
        boolean rising = s > p.sFloor() + 1e-6;
        boolean falling = s < p.sFloor() - 1e-6;
        if (!inTerritory(level, pos)) {
            double floor = DtpConfig.FACTOR_MIN.get();
            long total = (long) Math.floor(p.base() * floor * count);
            return Optional.of(new Quote(item, count, total, p.base() * floor, floor, rising, falling, true));
        }
        double sum = 0.0;
        for (int i = 0; i < count; i++) {
            sum += p.base() * factor(s + i * p.share(), p);
        }
        return Optional.of(new Quote(item, count, (long) Math.floor(sum), p.base() * currentFactor, currentFactor, rising, falling, false));
    }

    /**
     * Führt einen Verkauf aus: erhöht den Marktdruck um die verkaufte Menge (während einer Schnellen Erholung nur um
     * ihren Anteil) und liefert den Erlös.
     * Außerhalb des Territoriums entsteht kein Marktdruck — der Preis liegt dort ohnehin am Boden.
     */
    public static long sell(ServerLevel level, BlockPos pos, Item item, int count) {
        Optional<Quote> quote = quote(level, pos, item, count);
        if (quote.isEmpty()) {
            return 0;
        }
        if (!quote.get().outside()) {
            TradeList.Entry entry = TradeList.lookup(item).orElseThrow();
            Params p = params(level, item, entry);
            MarketData data = MarketData.get(level.getServer());
            double s = pressure(level, item, p);
            // Vorher und nachher festhalten: So zeigt der Tag die ganze Spanne, auch beim ersten Verkauf.
            record(level, item);
            data.setPressure(marketKey(item), s + count * p.share());
            record(level, item);
        }
        // Auch Verkäufe außerhalb des Territoriums bringen Taler und zählen für die Statistik-Tafel.
        StatisticsData.get(level.getServer()).recordSale(level.getServer(), item, count, quote.get().total());
        return quote.get().total();
    }

    // --- Preisverlauf ------------------------------------------------------------------------

    /** Halbwertsmenge H, wie sie gerade gilt: mit Spielerskalierung und Preisstabilität; 0 für nicht handelbare Items. */
    public static double halfAmount(ServerLevel level, Item item) {
        return TradeList.lookup(item).map(entry -> params(level, item, entry).h()).orElse(0.0);
    }

    /** Preisfaktor des Ruhepreises: 2^floorRatio, mit den Standardwerten ≈ 1,41. */
    public static double restFactor() {
        return Math.pow(2.0, DtpConfig.FLOOR_RATIO.get());
    }

    /** Stückpreis und Faktor im vollen Markt, wie der Marktstand im Territorium sie für ein einzelnes Stück zeigt. */
    public record UnitPrice(double price, double factor) {
    }

    /** Stückpreis im vollen Markt (innerhalb des Territoriums), unabhängig vom Standort — für die gebundene Uhr. */
    public static Optional<UnitPrice> fullMarketPrice(ServerLevel level, Item item) {
        return TradeList.lookup(item).map(entry -> {
            Params p = params(level, item, entry);
            double factor = factor(pressure(level, item, p), p);
            return new UnitPrice(p.base() * factor, factor);
        });
    }

    /** Preisfaktor im vollen Markt (innerhalb des Territoriums), unabhängig vom Standort. */
    public static Optional<Double> fullMarketFactor(ServerLevel level, Item item) {
        return TradeList.lookup(item).map(entry -> {
            Params p = params(level, item, entry);
            return factor(pressure(level, item, p), p);
        });
    }

    /**
     * Preisfaktor für einen Verkauf dieses Stapels bei {@code pos} — leer, wenn er nicht handelbar ist.
     * Außerhalb des Territoriums gilt die Untergrenze. Grundlage der Marktwaage.
     */
    public static Optional<Double> factorAt(ServerLevel level, BlockPos pos, ItemStack stack) {
        if (TradeList.lookup(stack).isEmpty()) {
            return Optional.empty();
        }
        return quote(level, pos, stack.getItem(), 1).map(Quote::factor);
    }

    /**
     * Comparator-Signal der Marktwaage: je angefangene 10 % Marktwert eine Stufe, höchstens 15.
     * Gerechnet wird mit den gerundeten Prozent, die auch der Marktstand anzeigt —
     * unter 10 % = 0, 100 % (Basiswert) = 10, ab 150 % = 15.
     */
    public static int comparatorSignal(double factor) {
        long percent = Math.round(factor * 100.0);
        return (int) Math.clamp(percent / 10, 0, 15);
    }

    /** Hält den aktuellen Preisfaktor im Verlauf fest (siehe {@link MarketData#recordFactor}). */
    public static void record(ServerLevel level, Item item) {
        fullMarketFactor(level, item).ifPresent(f -> MarketData.get(level.getServer()).recordFactor(marketKey(item), f));
    }

    /**
     * Einmal pro Markttag: tastet alle handelbaren Waren ab, auch nie verkaufte — jede Ware hat so einen
     * durchgehenden Verlauf, und die Erholung wird sichtbar, auch wenn nichts verkauft wird.
     */
    public static void sampleHistory(ServerLevel level) {
        MarketData data = MarketData.get(level.getServer());
        if (!data.startSampleDay()) {
            return;
        }
        Set<Identifier> done = new HashSet<>();
        for (Item item : TradeList.allTradable()) {
            if (done.add(marketKey(item))) {
                record(level, item);
            }
        }
    }

    /**
     * Markttage, bis der Preis wieder auf dem Basiswert liegt (Marktdruck 0); 0, wenn er schon dort oder darüber ist.
     * Aus der Erholungsformel: s(t) = sFloor + (s0 - sFloor) × 2^(-t/T) = 0.
     */
    public static double daysToBase(ServerLevel level, Item item) {
        Optional<TradeList.Entry> entry = TradeList.lookup(item);
        if (entry.isEmpty()) {
            return 0.0;
        }
        Params p = params(level, item, entry.get());
        double s = pressure(level, item, p);
        if (s <= 0.0 || p.sFloor() >= 0.0) {
            return 0.0;
        }
        return daysFor(level, p, Math.log((s - p.sFloor()) / -p.sFloor()) / Math.log(2.0));
    }

    /**
     * Anteil am Ruhepreis, ab dem eine Ware als erholt gilt. Den Ruhepreis selbst erreicht die Erholung nie ganz —
     * sie wird immer langsamer —, deshalb zählt die Zeit bis 95 % davon.
     */
    public static final double REST_SHARE = 0.95;

    /**
     * Markttage, bis der Preis {@link #REST_SHARE} des Ruhepreises erreicht; 0, wenn er schon dort oder darüber ist
     * (auch nach einem Nachfrageschub). Zieldruck: sTarget = sFloor − H × log2(REST_SHARE).
     */
    public static double daysToRest(ServerLevel level, Item item) {
        Optional<TradeList.Entry> entry = TradeList.lookup(item);
        if (entry.isEmpty()) {
            return 0.0;
        }
        Params p = params(level, item, entry.get());
        double s = pressure(level, item, p);
        double gap = -p.h() * Math.log(REST_SHARE) / Math.log(2.0);
        if (s <= p.sFloor() + gap || gap <= 0.0) {
            return 0.0;
        }
        return daysFor(level, p, Math.log((s - p.sFloor()) / gap) / Math.log(2.0));
    }

    /** Markttage für so viele Halbwertszeiten; im Fenster einer Schnellen Erholung zählt ein Markttag doppelt. */
    private static double daysFor(ServerLevel level, Params p, double halvings) {
        double day = MarketData.get(level.getServer()).day();
        double fast = 0.0;
        for (MarketData.Window w : p.windows()) {
            fast = Math.max(fast, w.end() - Math.max(day, w.start()));
        }
        double needed = halvings * p.t();
        return needed <= 2.0 * fast ? needed / 2.0 : fast + (needed - 2.0 * fast);
    }

    /** Marktdruck jetzt; übernimmt beim ersten Zugriff auf eine gemeinsame Ware den alten Druck ihrer Items. */
    private static double pressure(ServerLevel level, Item item, Params p) {
        MarketData data = MarketData.get(level.getServer());
        Identifier key = marketKey(item);
        adoptMemberPressure(data, key, p);
        return data.pressure(key, p.sFloor(), p.t(), p.windows());
    }

    /**
     * Spielstände von vor der gemeinsamen Ware führen den Druck je Item. Fehlt der Druck der Ware noch, übernimmt
     * sie den höchsten ihrer Items — so wird nichts billiger, als es war — und die alten Einträge entfallen.
     */
    private static void adoptMemberPressure(MarketData data, Identifier key, Params p) {
        if (data.hasPressure(key)) {
            return;
        }
        double highest = Double.NEGATIVE_INFINITY;
        for (Item member : TradeList.members(key)) {
            Identifier id = itemId(member);
            if (id.equals(key) || !data.hasPressure(id)) {
                continue;
            }
            highest = Math.max(highest, data.pressure(id, p.sFloor(), p.t(), p.windows()));
            data.clearPressure(id);
            data.clearHistory(id);
        }
        if (highest > Double.NEGATIVE_INFINITY) {
            data.setPressure(key, highest);
        }
    }

    /** Die Ware, deren Marktdruck dieses Item nutzt; für nicht verkaufbare Items die Item-ID. */
    public static Identifier marketKey(Item item) {
        return TradeList.marketKey(item).orElseGet(() -> itemId(item));
    }

    /** Die Item-ID — für Client, Protokoll und Anzeige. */
    public static Identifier itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }
}
