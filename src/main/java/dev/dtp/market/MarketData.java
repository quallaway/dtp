package dev.dtp.market;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.contract.Contracts;
import dev.dtp.contract.PlayerContracts;
import dev.dtp.data.Versioned;

/**
 * Serverweiter Marktzustand: Marktdruck je Item, die beiden Uhren und die Liste aktiver Spieler.
 *
 * Liegt im Speicher des Servers, nicht einer Dimension — der Nether hat keinen eigenen Markt.
 * Der Marktdruck wird nie getickt, sondern beim Lesen aus dem letzten Stand und der Zeit seither abgeleitet.
 *
 * Zwei serverweite Uhren, beide in Minecraft-Tagen: die <b>Marktuhr</b> ({@link #day()}) für Marktdruck, Preisverlauf
 * und aktive Spieler, die <b>Contract-Uhr</b> ({@link #contractDay()}) für den Aushang. Beide laufen nur, während
 * mindestens ein Spieler online ist — ein gestoppter, pausierter oder leerer Server lässt nichts altern. Schlafen
 * zählt mit. Getrennt sind sie nur, weil Admin-Befehle und ältere Spielstände sie unterschiedlich gestellt haben können.
 *
 * Angenommene Contracts altern auf der persönlichen Uhr ihres Besitzers ({@link dev.dtp.contract.PlayerContracts#ownDay()}),
 * die nur läuft, während er selbst online ist.
 */
public class MarketData extends SavedData {
    private static final long TICKS_PER_DAY = 24000L;

    /** Marktdruck {@code s} eines Items zum Zeitpunkt {@code day}. */
    public record Pressure(double s, double day) {
        static final Codec<Pressure> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("s").forGetter(Pressure::s),
                Codec.DOUBLE.fieldOf("day").forGetter(Pressure::day)
        ).apply(i, Pressure::new));
    }

    /** Ein Markttag im Preisverlauf einer Ware: tiefster, höchster und zuletzt beobachteter Preisfaktor. */
    public record HistoryPoint(int day, double low, double high, double close) {
        static final Codec<HistoryPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("day").forGetter(HistoryPoint::day),
                Codec.DOUBLE.fieldOf("low").forGetter(HistoryPoint::low),
                Codec.DOUBLE.fieldOf("high").forGetter(HistoryPoint::high),
                Codec.DOUBLE.fieldOf("close").forGetter(HistoryPoint::close)
        ).apply(i, HistoryPoint::new));
    }

    /**
     * Ein laufendes Ereignis mit Laufzeit (siehe {@link MarketEvents.Type}): Typ, Ziel (Gruppen-ID, {@code ""} = alle
     * Waren) und Fenster in Marktzeit. Zwei Ereignisse desselben Typs laufen nie gleichzeitig.
     */
    public record RunningEvent(String type, String target, double start, double end) {
        static final Codec<RunningEvent> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("type").forGetter(RunningEvent::type),
                Codec.STRING.fieldOf("target").forGetter(RunningEvent::target),
                Codec.DOUBLE.fieldOf("start").forGetter(RunningEvent::start),
                Codec.DOUBLE.fieldOf("end").forGetter(RunningEvent::end)
        ).apply(i, RunningEvent::new));
    }

    /** Ein Zeitfenster in Marktzeit, in dem die Erholung von unten doppelt zählt (Schnelle Erholung). */
    public record Window(double start, double end) {
    }

    /** So viele Markttage reicht der Preisverlauf zurück; ältere Stützpunkte werden verworfen. */
    public static final int HISTORY_DAYS = 14;

    private static final Codec<MarketData> INNER = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Identifier.CODEC, Pressure.CODEC).optionalFieldOf("pressure", Map.of()).forGetter(d -> d.pressure),
            Codec.DOUBLE.optionalFieldOf("day", 0.0).forGetter(d -> d.day),
            Codec.DOUBLE.optionalFieldOf("contract_day", 0.0).forGetter(d -> d.contractDay),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.DOUBLE).optionalFieldOf("last_seen", Map.of()).forGetter(d -> d.lastSeen),
            Codec.LONG.optionalFieldOf("event_seconds", -1L).forGetter(d -> d.eventSeconds),
            Codec.STRING.optionalFieldOf("event_target", "").forGetter(d -> d.eventTarget),
            Codec.BOOL.optionalFieldOf("event_warned", false).forGetter(d -> d.eventWarned),
            Codec.unboundedMap(Identifier.CODEC, HistoryPoint.CODEC.listOf()).optionalFieldOf("history", Map.of()).forGetter(d -> d.history),
            Codec.STRING.listOf().optionalFieldOf("known_categories").forGetter(d -> Optional.ofNullable(d.knownCategories)),
            Codec.STRING.listOf().optionalFieldOf("event_queue", List.of()).forGetter(d -> d.eventQueue),
            Codec.STRING.listOf().optionalFieldOf("event_drawn", List.of()).forGetter(d -> d.eventDrawn),
            Codec.STRING.optionalFieldOf("event_type", "demand_surge").forGetter(d -> d.eventType),
            RunningEvent.CODEC.listOf().optionalFieldOf("running_events", List.of()).forGetter(d -> d.running),
            Codec.BOOL.optionalFieldOf("event_wither_counted", false).forGetter(d -> d.witherCounted)
    ).apply(i, MarketData::new));

    /**
     * Version 2 führt die Contract-Uhr ein. Bestehende Contracts sind mit der Marktuhr gestempelt;
     * die Contract-Uhr startet deshalb auf deren Stand, damit Fristen nicht springen.
     */
    private static final Versioned.Step V1_ADD_CONTRACT_CLOCK = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data.set("contract_day", data.createDouble(data.get("day").asDouble(0.0)));
        }
    };

    /** Version 3 führt die Marktereignisse ein. Ältere Stände haben noch kein geplantes Ereignis — es wird beim ersten Tick geplant. */
    private static final Versioned.Step V2_ADD_EVENTS = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    };

    /** Version 4 führt den Preisverlauf ein. Ältere Stände beginnen ohne Verlauf. */
    private static final Versioned.Step V3_ADD_HISTORY = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    };

    /**
     * Version 5: Ein Tag im Verlauf hält Tiefst-, Höchst- und letzten Wert statt nur des Tiefstwerts. Der alte Verlauf
     * war kaum gefüllt und wird verworfen; er baut sich ab dem nächsten Markttag neu auf.
     */
    private static final Versioned.Step V4_HISTORY_RANGE = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data.remove("history");
        }
    };

    /** Version 6: Abwesenheit wird nicht mehr nachgeholt, der Zeitstempel der letzten Echtzeit entfällt. */
    private static final Versioned.Step V5_DROP_REAL_TIME = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data.remove("last_real_millis");
        }
    };

    /**
     * Version 7: freigeschaltete Warengruppen und die Warteschlange der Ereignisziele. Ältere Stände haben keine
     * Liste der bekannten Gruppen — sie wird beim ersten Tick still mit den bereits freien Gruppen gefüllt, damit ein
     * längst offener Nether kein nachträgliches Ereignis auslöst.
     */
    private static final Versioned.Step V6_CATEGORY_UNLOCKS = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    };

    /** Version 8: Ereignistypen und Ereignisse mit Laufzeit. Ältere Stände planen einen Nachfrageschub, nichts läuft. */
    private static final Versioned.Step V7_EVENT_TYPES = new Versioned.Step() {
        @Override
        public <T> Dynamic<T> migrate(Dynamic<T> data) {
            return data;
        }
    };

    public static final Codec<MarketData> CODEC = Versioned.codec(8, INNER,
            List.of(V1_ADD_CONTRACT_CLOCK, V2_ADD_EVENTS, V3_ADD_HISTORY, V4_HISTORY_RANGE, V5_DROP_REAL_TIME, V6_CATEGORY_UNLOCKS,
                    V7_EVENT_TYPES));

    public static final SavedDataType<MarketData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Dtp.MODID, "market"), MarketData::new, CODEC, (DataFixTypes) null);

    private final Map<Identifier, Pressure> pressure;
    private double day;
    private double contractDay;
    private final Map<UUID, Double> lastSeen;
    /** Stand der Overworld-Uhr beim letzten Uhrabgleich; nur zur Laufzeit. */
    private long lastClockTicks = -1;
    /** Sekunden Serverlaufzeit bis zum nächsten Marktereignis; -1 = noch keines geplant. */
    private long eventSeconds;
    /** Ziel des nächsten Ereignisses: ID einer Warengruppe oder leer für alle Waren. */
    private String eventTarget;
    private boolean eventWarned;
    /** Preisverlauf je Ware: höchstens ein Stützpunkt pro Markttag, nur die letzten {@link #HISTORY_DAYS} Tage. */
    private final Map<Identifier, List<HistoryPoint>> history = new HashMap<>();
    /** Markttag, für den zuletzt alle Waren abgetastet wurden; nur zur Laufzeit. */
    private int sampledDay = -1;
    /** Bereits freigeschaltete Warengruppen; {@code null} = noch nie abgeglichen (neue Welt oder Stand vor Version 7). */
    private List<String> knownCategories;
    /** Ziele, die beim Planen der nächsten Ereignisse Vorrang haben — neu freigeschaltete Gruppen, in Reihenfolge. */
    private final List<String> eventQueue;
    /** Warengruppen, die in der laufenden Runde schon Ziel waren; erst wenn alle dran waren, beginnt eine neue. */
    private final List<String> eventDrawn;
    /** Typ des geplanten Ereignisses ({@link MarketEvents.Type#id()}). */
    private String eventType;
    /** Laufende Ereignisse mit Laufzeit. */
    private final List<RunningEvent> running;
    /** Ein Wither hat die Wartezeit bis zum geplanten Ereignis schon verkürzt; das nächste geplante Ereignis setzt zurück. */
    private boolean witherCounted;

    public MarketData() {
        this(Map.of(), 0.0, 0.0, Map.of(), -1L, "", false, Map.of(), Optional.empty(), List.of(), List.of(), "demand_surge", List.of(),
                false);
    }

    private MarketData(Map<Identifier, Pressure> pressure, double day, double contractDay, Map<UUID, Double> lastSeen,
            long eventSeconds, String eventTarget, boolean eventWarned, Map<Identifier, List<HistoryPoint>> history,
            Optional<List<String>> knownCategories, List<String> eventQueue, List<String> eventDrawn, String eventType,
            List<RunningEvent> running, boolean witherCounted) {
        this.witherCounted = witherCounted;
        this.eventType = eventType;
        this.running = new ArrayList<>(running);
        this.knownCategories = knownCategories.map(ArrayList::new).orElse(null);
        this.eventQueue = new ArrayList<>(eventQueue);
        this.eventDrawn = new ArrayList<>(eventDrawn);
        history.forEach((id, points) -> this.history.put(id, new ArrayList<>(points)));
        this.pressure = new HashMap<>(pressure);
        this.day = day;
        this.contractDay = contractDay;
        this.lastSeen = new HashMap<>(lastSeen);
        this.eventSeconds = eventSeconds;
        this.eventTarget = eventTarget;
        this.eventWarned = eventWarned;
    }

    public static MarketData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    // --- Uhr --------------------------------------------------------------------------------

    /** Marktuhr in Minecraft-Tagen: nur Zeit, in der Spieler online waren. */
    public double day() {
        return day;
    }

    /** Contract-Uhr in Minecraft-Tagen: nur Zeit, in der Spieler online waren. */
    public double contractDay() {
        return contractDay;
    }

    /** Beim Serverstart: Die Zeit, in der der Server gestoppt war, zählt nicht — nur der Bezugspunkt wird gesetzt. */
    public void resetClockBaseline(MinecraftServer server) {
        lastClockTicks = server.overworld().getOverworldClockTime();
    }

    /**
     * Folgt der Overworld-Uhr, solange der Server läuft. Schlafen springt die Uhr vor und lässt Markt und Contracts
     * altern — gewollt. Zurückgestellte Zeit wird ignoriert. Ein pausierter Server ruft hier gar nicht auf, und seine
     * Overworld-Uhr steht; ein leerer, nicht pausierter Server zieht nur den Bezugspunkt nach (siehe {@link #tick}).
     */
    public void followClock(MinecraftServer server) {
        long ticks = server.overworld().getOverworldClockTime();
        double played = lastClockTicks >= 0 && ticks > lastClockTicks ? (double) (ticks - lastClockTicks) / TICKS_PER_DAY : 0.0;
        lastClockTicks = ticks;
        tick(played, server.getPlayerList().getPlayers());
    }

    /**
     * Lässt {@code played} Tage vergehen — aber nur, wenn jemand online ist. Dann laufen beide Serveruhren und die
     * persönliche Contract-Uhr jedes eingeloggten Spielers; wer offline ist, dessen angenommene Contracts stehen.
     */
    public void tick(double played, Collection<? extends Player> online) {
        if (played <= 0.0 || online.isEmpty()) {
            return;
        }
        day += played;
        contractDay += played;
        for (Player player : online) {
            PlayerContracts mine = Contracts.of(player);
            double before = mine.ownDay();
            mine.advance(played);
            // Der Client rechnet die persönliche Uhr selbst hoch; einmal je Tag wird sie nachgestellt.
            if (Math.floor(before) != Math.floor(mine.ownDay())) {
                Contracts.sync(player);
            }
        }
        setDirty();
    }

    /**
     * Nur für Tests und Admin-Befehle: stellt beide Serveruhren vor. Persönliche Contract-Uhren bleiben stehen —
     * dafür {@link #advance(double, Collection)}.
     */
    public void advance(double days) {
        day += days;
        contractDay += days;
        setDirty();
    }

    /** Admin-Befehl: stellt beide Serveruhren und die persönlichen Contract-Uhren der genannten Spieler vor. */
    public void advance(double days, Collection<? extends Player> players) {
        advance(days);
        for (Player player : players) {
            Contracts.of(player).advance(days);
            Contracts.sync(player);
        }
    }

    // --- Marktdruck --------------------------------------------------------------------------

    /**
     * Marktdruck jetzt: s(t+Δ) = sFloor + (s(t) - sFloor) × 2^(-Δ/T).
     * Ein nie gehandeltes Item steht auf sFloor, also über dem Basiswert.
     */
    public double pressure(Identifier item, double sFloor, double t) {
        return pressure(item, sFloor, t, List.of());
    }

    /**
     * Wie {@link #pressure(Identifier, double, double)}, aber Marktzeit innerhalb der {@code windows} zählt doppelt —
     * das entspricht halbem T (Schnelle Erholung). Exakt für jeden Zeitpunkt, auch nach dem Ende eines Fensters.
     * Die Fenster gelten nur aufwärts: Liegt der Druck unter {@code sFloor} (Preis über dem Ruhepreis), zählen sie
     * nicht. Die Kurve kreuzt sFloor nie, die Richtung steht also mit dem gespeicherten Stand fest.
     */
    public double pressure(Identifier item, double sFloor, double t, List<Window> windows) {
        Pressure p = pressure.get(item);
        if (p == null) {
            return sFloor;
        }
        double elapsed = Math.max(0.0, day - p.day());
        for (Window w : p.s() > sFloor ? windows : List.<Window>of()) {
            elapsed += Math.max(0.0, Math.min(day, w.end()) - Math.max(p.day(), w.start()));
        }
        return sFloor + (p.s() - sFloor) * Math.pow(2.0, -elapsed / t);
    }

    public void setPressure(Identifier item, double s) {
        pressure.put(item, new Pressure(s, day));
        setDirty();
    }

    public boolean hasPressure(Identifier item) {
        return pressure.containsKey(item);
    }

    /** Setzt eine Ware auf „nie gehandelt“ zurück. */
    public void clearPressure(Identifier item) {
        if (pressure.remove(item) != null) {
            setDirty();
        }
    }

    public void clearPressure() {
        pressure.clear();
        setDirty();
    }

    /** Items mit gespeichertem Marktdruck — alle anderen stehen ohnehin auf ihrem Grundwert. */
    public Set<Identifier> pressuredItems() {
        return Set.copyOf(pressure.keySet());
    }

    // --- Marktereignisse ---------------------------------------------------------------------

    public boolean eventScheduled() {
        return eventSeconds >= 0;
    }

    public long eventSeconds() {
        return eventSeconds;
    }

    public String eventTarget() {
        return eventTarget;
    }

    public boolean eventWarned() {
        return eventWarned;
    }

    public String eventType() {
        return eventType;
    }

    /** Plant ein Ereignis; der Typ bleibt der bisher geplante. */
    public void scheduleEvent(long seconds, String target) {
        scheduleEvent(seconds, target, eventType);
    }

    public void scheduleEvent(long seconds, String target, String type) {
        eventType = type;
        eventSeconds = seconds;
        eventTarget = target;
        eventWarned = false;
        setDirty();
    }

    /** Zieht Wartezeit ab, nie unter null — fällig ist das Ereignis dann beim nächsten Tick. */
    public void countDownEvent(long seconds) {
        eventSeconds = Math.max(0, eventSeconds - seconds);
        setDirty();
    }

    public boolean witherCounted() {
        return witherCounted;
    }

    public void setWitherCounted(boolean counted) {
        if (witherCounted != counted) {
            witherCounted = counted;
            setDirty();
        }
    }

    public void setEventWarned() {
        eventWarned = true;
        setDirty();
    }

    // --- Ereignisse mit Laufzeit -------------------------------------------------------------

    public List<RunningEvent> running() {
        return List.copyOf(running);
    }

    /** Startet ein Ereignis mit Laufzeit; ein laufendes desselben Typs muss vorher beendet sein. */
    public void startRunning(RunningEvent event) {
        running.removeIf(e -> e.type().equals(event.type()));
        running.add(event);
        setDirty();
    }

    public void removeRunning(RunningEvent event) {
        if (running.remove(event)) {
            setDirty();
        }
    }

    // --- Freischaltung der Warengruppen -------------------------------------------------------

    /**
     * Gleicht die freigeschalteten Gruppen ab. Neu hinzugekommene werden gemerkt und hinten in die Warteschlange
     * gestellt — beim allerersten Abgleich nur gemerkt. Gibt die neu eingereihten Gruppen zurück.
     */
    public List<String> trackUnlockedCategories(List<String> unlocked) {
        if (knownCategories == null) {
            knownCategories = new ArrayList<>(unlocked);
            setDirty();
            return List.of();
        }
        List<String> added = new ArrayList<>();
        for (String id : unlocked) {
            if (!knownCategories.contains(id)) {
                knownCategories.add(id);
                eventQueue.add(id);
                added.add(id);
            }
        }
        if (!added.isEmpty()) {
            setDirty();
        }
        return added;
    }

    public List<String> eventQueue() {
        return List.copyOf(eventQueue);
    }

    /** Nimmt das erste Ziel aus der Warteschlange; leer, wenn keines wartet. */
    public Optional<String> pollEventQueue() {
        if (eventQueue.isEmpty()) {
            return Optional.empty();
        }
        String next = eventQueue.removeFirst();
        setDirty();
        return Optional.of(next);
    }

    // --- Rotation der Ereignisziele ----------------------------------------------------------

    /** Gruppen, die in der laufenden Runde schon Ziel eines Ereignisses waren. */
    public List<String> drawnEvents() {
        return List.copyOf(eventDrawn);
    }

    public void markDrawn(String id) {
        if (!eventDrawn.contains(id)) {
            eventDrawn.add(id);
            setDirty();
        }
    }

    /** Beginnt eine neue Runde: Alle Gruppen dürfen wieder Ziel werden. */
    public void resetDrawn() {
        if (!eventDrawn.isEmpty()) {
            eventDrawn.clear();
            setDirty();
        }
    }

    /** Nur für Tests: vergisst den Abgleich, als wäre es ein Stand vor Version 7. */
    public void resetCategoryUnlocks(List<String> known, List<String> queue) {
        knownCategories = known == null ? null : new ArrayList<>(known);
        eventQueue.clear();
        eventQueue.addAll(queue);
        setDirty();
    }

    /** Nur für Tests: aktueller Abgleichstand, {@code null} = noch nie abgeglichen. */
    public List<String> knownCategories() {
        return knownCategories == null ? null : List.copyOf(knownCategories);
    }

    // --- Preisverlauf ------------------------------------------------------------------------

    /**
     * Hält einen beobachteten Preisfaktor fest. Pro Markttag bleibt ein Punkt mit Tiefst-, Höchst- und letztem Wert —
     * so wächst der Verlauf nie mit der Zahl der Verkäufe, sondern höchstens um einen Punkt pro Tag und Ware.
     */
    public void recordFactor(Identifier item, double factor) {
        int today = (int) Math.floor(day);
        List<HistoryPoint> points = history.computeIfAbsent(item, k -> new ArrayList<>());
        HistoryPoint last = points.isEmpty() ? null : points.getLast();
        if (last != null && last.day() == today) {
            points.set(points.size() - 1, new HistoryPoint(today, Math.min(last.low(), factor), Math.max(last.high(), factor), factor));
        } else {
            points.add(new HistoryPoint(today, factor, factor, factor));
        }
        points.removeIf(p -> p.day() <= today - HISTORY_DAYS);
        setDirty();
    }

    public void clearHistory(Identifier item) {
        if (history.remove(item) != null) {
            setDirty();
        }
    }

    public List<HistoryPoint> history(Identifier item) {
        return List.copyOf(history.getOrDefault(item, List.of()));
    }

    /** Liefert true einmal pro neuem Markttag — dann werden alle Waren mit Marktdruck abgetastet. */
    public boolean startSampleDay() {
        int today = (int) Math.floor(day);
        if (today == sampledDay) {
            return false;
        }
        sampledDay = today;
        return true;
    }

    // --- Aktive Spieler ----------------------------------------------------------------------

    public void markSeen(UUID player) {
        lastSeen.put(player, day);
        setDirty();
    }

    /** Über ein Zeitfenster gemittelte Spielerzahl, nicht die gerade eingeloggten. Mindestens 1. */
    public int activePlayers() {
        double window = DtpConfig.ACTIVE_WINDOW_DAYS.get();
        long count = lastSeen.values().stream().filter(seen -> day - seen <= window).count();
        return (int) Math.max(1, count);
    }
}
