package dev.dtp.market;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;

import dev.dtp.Dtp;
import dev.dtp.DtpConfig;
import dev.dtp.data.Protocol;
import dev.dtp.upgrade.WorldProgress;

/**
 * Marktereignisse (docs/11-marktdruck.md): In festen Abständen steigt die Nachfrage nach einer
 * Warengruppe — gesättigte Preise erholen sich auf einen Schlag, und alle Preise der Gruppe steigen vorübergehend
 * über den Ruhepreis (siehe {@link Market#relievePressure}). Oder der ganze Markt erholt sich eine Weile schneller
 * (Schnelle Erholung).
 *
 * Die Uhr läuft nur, während Spieler online sind, und erst nach dem Start des Durchlaufs. Jeder abgeschlossene
 * Contract bringt das nächste Ereignis näher. Das Ziel steht schon beim Planen fest und wird am Aushang
 * angekündigt — ein Ereignis soll eine Planungsaufgabe sein, keine Überraschung.
 *
 * Gesperrte Warengruppen (siehe {@link MarketCategories.Unlock}) werden nie ausgelost. Wird eine Gruppe frei,
 * reiht sie sich in eine Warteschlange ein: Das schon angekündigte Ereignis bleibt, das danach geplante gilt ihr.
 */
public final class MarketEvents {
    private MarketEvents() {
    }

    /**
     * Ereignistypen im Takt. Der Nachfrageschub ändert den Marktdruck einer Gruppe einmal; die
     * Schnelle Erholung gilt immer für alle Waren, halbiert T für Preise unter dem Ruhepreis und senkt den Druck je
     * Verkauf, beides für eine feste Marktzeit ({@link MarketData.RunningEvent}). Vollmond und Regen laufen außerhalb des
     * Takts als Aufschlag auf den Preisfaktor ({@link Market#activeBonuses}).
     */
    public enum Type {
        DEMAND_SURGE("demand_surge"),
        FAST_RECOVERY("fast_recovery");

        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        /** Unbekannte Typen (alte oder entfernte) gelten als Nachfrageschub. */
        public static Type byId(String id) {
            for (Type t : values()) {
                if (t.id.equals(id)) {
                    return t;
                }
            }
            return DEMAND_SURGE;
        }

        public Component displayName() {
            return Component.translatable("dtp.event.type." + id);
        }
    }

    /** Einmal pro Sekunde aus dem Server-Tick. Ohne Spieler online steht der Countdown. */
    public static void tick(MinecraftServer server) {
        if (server.getPlayerList().getPlayerCount() == 0) {
            return;
        }
        MarketData data = MarketData.get(server);
        trackUnlocks(server, data);
        endExpired(server, data);
        if (!DtpConfig.EVENTS_ENABLED.get() || !WorldProgress.get(server).started()) {
            return;
        }
        if (!data.eventScheduled()) {
            schedule(server, data);
        } else if (Type.byId(data.eventType()) == Type.FAST_RECOVERY && !data.eventTarget().isEmpty()) {
            // Nur alte Stände: Eine Schnelle Erholung galt früher einer Gruppe, jetzt immer allen Waren.
            data.scheduleEvent(data.eventSeconds(), "", data.eventType());
        } else if (Type.byId(data.eventType()) == Type.DEMAND_SURGE && !targetAvailable(server, data.eventTarget())) {
            // Nur alte Stände: Ein schon ausgelostes Ziel ist (wieder) gesperrt, oder ein Schub galt noch allen Waren.
            // Die Wartezeit bleibt.
            schedule(server, data, data.eventSeconds(), Type.DEMAND_SURGE);
        }
        data.countDownEvent(1);
        long warning = DtpConfig.EVENT_WARNING_MINUTES.get() * 60L;
        if (warning > 0 && !data.eventWarned() && data.eventSeconds() <= warning && data.eventSeconds() > 0) {
            data.setEventWarned();
            server.getPlayerList().broadcastSystemMessage(Component.translatable("dtp.event.warning." + data.eventType(),
                    minutesLeft(data), targetName(data.eventTarget())), false);
        }
        if (data.eventSeconds() <= 0) {
            fire(server);
        }
    }

    /** Beendet Ereignisse mit Laufzeit, deren Ende erreicht ist — auch nach einem Sprung der Uhr durch Schlafen. */
    private static void endExpired(MinecraftServer server, MarketData data) {
        for (MarketData.RunningEvent e : data.running()) {
            if (data.day() >= e.end()) {
                end(server, e);
            }
        }
    }

    /** Admin/Test: beendet alle laufenden Ereignisse sofort. Gibt ihre Zahl zurück. */
    public static int endRunning(MinecraftServer server) {
        List<MarketData.RunningEvent> running = MarketData.get(server).running();
        for (MarketData.RunningEvent e : running) {
            end(server, e);
        }
        return running.size();
    }

    private static void end(MinecraftServer server, MarketData.RunningEvent e) {
        Market.endFastRecovery(server.overworld(), e);
        server.getPlayerList().broadcastSystemMessage(Component.translatable("dtp.event.ended." + e.type(),
                targetName(e.target())), false);
    }

    /** Laufende Ereignisse und Aufschläge aus der Welt, je eine Zeile — für Marktstand und {@code event info}. */
    public static List<Component> runningLines(MinecraftServer server) {
        MarketData data = MarketData.get(server);
        List<Component> lines = new ArrayList<>();
        for (MarketData.RunningEvent e : data.running()) {
            long minutes = Math.max(1, (long) Math.ceil((e.end() - data.day()) * 20.0));
            lines.add(Component.translatable("dtp.event.running." + e.type(), targetName(e.target()), minutes));
        }
        for (Market.WorldBonus b : Market.activeBonuses(server)) {
            lines.add(Component.translatable("dtp.event.running." + b.event(), targetName(b.category().toString()),
                    Math.round(b.bonus() * 100.0)));
        }
        return lines;
    }

    /**
     * Admin/Test: legt den Typ des geplanten Ereignisses fest, die Wartezeit bleibt. Eine Schnelle Erholung gilt
     * allen Waren; ein Nachfrageschub behält eine Gruppe als Ziel oder lost eine aus.
     */
    public static void setType(MinecraftServer server, Type type) {
        MarketData data = MarketData.get(server);
        if (!data.eventScheduled()) {
            schedule(server, data);
        }
        if (type == Type.DEMAND_SURGE && targetAvailable(server, data.eventTarget())) {
            data.scheduleEvent(data.eventSeconds(), data.eventTarget(), type.id());
        } else {
            schedule(server, data, data.eventSeconds(), type);
        }
    }

    /**
     * Admin/Test: legt das Ziel des geplanten Ereignisses fest, Typ und Wartezeit bleiben. Ein Nachfrageschub braucht
     * eine Gruppe, eine Schnelle Erholung gilt immer allen Waren ({@code ""}).
     */
    public static boolean setTarget(MinecraftServer server, String target) {
        MarketData data = MarketData.get(server);
        if (!data.eventScheduled()) {
            schedule(server, data);
        }
        if (target.isEmpty() != (Type.byId(data.eventType()) == Type.FAST_RECOVERY)) {
            return false;
        }
        data.scheduleEvent(data.eventSeconds(), target, data.eventType());
        return true;
    }

    /** Befehlsargument → Ziel: eine Warengruppe per Pfad oder voller ID. */
    public static Optional<String> resolveTarget(String arg) {
        return MarketCategories.all().stream()
                .filter(c -> c.id().getPath().equals(arg) || c.id().toString().equals(arg))
                .map(c -> c.id().toString())
                .findFirst();
    }

    /** Ein abgeschlossener Contract verkürzt die Wartezeit, je nach Stufe. */
    public static void onContractCompleted(MinecraftServer server, int tier) {
        if (!DtpConfig.EVENTS_ENABLED.get()) {
            return;
        }
        MarketData data = MarketData.get(server);
        if (data.eventScheduled()) {
            double minutes = DtpConfig.listValue(DtpConfig.EVENT_CONTRACT_MINUTES.get(), tier - 1, 0.0);
            data.countDownEvent(Math.round(minutes * 60.0));
        }
    }

    /**
     * Ein von einem Spieler besiegter Wither verkürzt die Wartezeit um {@code events.witherMinutes}, aber nur einmal je
     * geplantem Ereignis: Eine Wither-Farm holt das Ereignis nicht beliebig heran. Erst das nächste geplante Ereignis
     * gibt den Bonus wieder frei.
     */
    public static void onWitherKilled(MinecraftServer server) {
        if (!DtpConfig.EVENTS_ENABLED.get()) {
            return;
        }
        MarketData data = MarketData.get(server);
        if (data.eventScheduled() && !data.witherCounted()) {
            data.countDownEvent(Math.round(DtpConfig.EVENT_WITHER_MINUTES.get() * 60.0));
            data.setWitherCounted(true);
        }
    }

    /**
     * Admin/Test: setzt die Wartezeit bis zum geplanten Ereignis neu, das Ziel bleibt. Die Vorwarnung wird
     * zurückgesetzt und kommt wieder, sobald die Restzeit unter die Vorwarnzeit fällt.
     */
    public static void setCountdown(MinecraftServer server, long minutes) {
        MarketData data = MarketData.get(server);
        if (!data.eventScheduled()) {
            schedule(server, data);
        }
        data.scheduleEvent(minutes * 60L, data.eventTarget());
    }

    /** Admin/Test: Restzeit und Ziel des nächsten Ereignisses; plant eines, falls noch keines feststeht. */
    public static Component describeNext(MinecraftServer server) {
        MarketData data = MarketData.get(server);
        if (!data.eventScheduled()) {
            schedule(server, data);
        }
        Component state = !DtpConfig.EVENTS_ENABLED.get() ? Component.translatable("dtp.command.event_disabled")
                : !WorldProgress.get(server).started() ? Component.translatable("dtp.command.event_not_started")
                : Component.empty();
        return Component.translatable("dtp.command.event_info", minutesLeft(data), Type.byId(data.eventType()).displayName(),
                targetName(data.eventTarget())).append(state);
    }

    /** Löst das geplante Ereignis sofort aus und plant das nächste. Gibt die Zahl der betroffenen Waren zurück. */
    public static int fire(MinecraftServer server) {
        MarketData data = MarketData.get(server);
        if (!data.eventScheduled()) {
            schedule(server, data);
        }
        int items = trigger(server, Type.byId(data.eventType()), data.eventTarget());
        schedule(server, data);
        return items;
    }

    /**
     * Löst ein Ereignis dieses Typs für dieses Ziel aus, ohne die Planung zu berühren. Eine Schnelle Erholung gilt
     * immer allen Waren, ein mitgegebenes Ziel wird ignoriert. Ein Nachfrageschub ohne bekannte Gruppe (alte Stände)
     * wird zur Schnellen Erholung. Gibt die Zahl der betroffenen Waren zurück.
     */
    public static int trigger(MinecraftServer server, Type requested, String requestedTarget) {
        Type type = requested == Type.DEMAND_SURGE && tag(requestedTarget) == null ? Type.FAST_RECOVERY : requested;
        String target = type == Type.FAST_RECOVERY ? "" : requestedTarget;
        TagKey<Item> tag = tag(target);
        int items = switch (type) {
            case DEMAND_SURGE -> Market.relievePressure(server.overworld(), tag, DtpConfig.EVENT_PRESSURE_RELIEF.get(),
                    DtpConfig.EVENT_PRICE_BOOST.get());
            case FAST_RECOVERY -> Market.startFastRecovery(server.overworld(), null, target, DtpConfig.EVENT_FAST_RECOVERY_DAYS.get());
        };
        server.getPlayerList().broadcastSystemMessage(Component.translatable("dtp.event.announce." + type.id(), targetName(target)), false);
        Protocol.log(server, null, "market_event", type.id() + ":" + (target.isEmpty() ? "all" : target), items, 0);
        return items;
    }

    /**
     * Gleicht die freigeschalteten Warengruppen ab; neu freie Gruppen kommen in die Warteschlange. Läuft auch,
     * solange Ereignisse abgeschaltet sind oder der Durchlauf noch nicht begonnen hat — die Gruppe wartet dann.
     */
    public static void trackUnlocks(MinecraftServer server, MarketData data) {
        List<String> unlocked = MarketCategories.unlocked(server).stream().map(c -> c.id().toString()).toList();
        for (String id : data.trackUnlockedCategories(unlocked)) {
            Dtp.LOGGER.info("Market category {} unlocked, queued as a market event target", id);
        }
    }

    /**
     * Plant das nächste Ereignis mit voller Wartezeit: erst den Typ würfeln, dann das Ziel. Eine Schnelle Erholung
     * gilt allen Waren und verbraucht weder die Warteschlange noch eine Gruppe der Reihum-Auslosung. Ein neuer Zyklus
     * gibt die Verkürzung durch einen Wither wieder frei ({@link #onWitherKilled}).
     */
    private static void schedule(MinecraftServer server, MarketData data) {
        schedule(server, data, DtpConfig.EVENT_INTERVAL_MINUTES.get() * 60L, rollType(server));
        data.setWitherCounted(false);
    }

    /** Plant ein Ereignis dieses Typs mit dieser Wartezeit. Ohne freie Gruppe wird ein Schub zur Schnellen Erholung. */
    private static void schedule(MinecraftServer server, MarketData data, long seconds, Type type) {
        String target = type == Type.FAST_RECOVERY ? "" : nextTarget(server, data);
        data.scheduleEvent(seconds, target, target.isEmpty() ? Type.FAST_RECOVERY.id() : type.id());
    }

    /** Würfelt den Typ: mit {@code events.fastRecovery.chance} eine Schnelle Erholung, sonst ein Nachfrageschub. */
    public static Type rollType(MinecraftServer server) {
        return server.overworld().getRandom().nextDouble() < DtpConfig.EVENT_FAST_RECOVERY_CHANCE.get() ? Type.FAST_RECOVERY : Type.DEMAND_SURGE;
    }

    /**
     * Ziel des nächsten Ereignisses: zuerst eine neu freigeschaltete Gruppe aus der Warteschlange (inzwischen
     * entfernte oder wieder gesperrte werden übersprungen), sonst ausgelost.
     *
     * Ausgelost wird reihum: Eine Gruppe kommt erst wieder dran, wenn alle freigeschalteten einmal Ziel waren.
     * Auch über die Grenze einer Runde hinweg gibt es kein Ziel zweimal hintereinander. {@code data.eventTarget()}
     * ist dabei noch das vorige Ziel. {@code ""} nur, wenn keine Gruppe frei ist.
     */
    public static String nextTarget(MinecraftServer server, MarketData data) {
        String previous = data.eventScheduled() ? data.eventTarget() : "";
        for (var queued = data.pollEventQueue(); queued.isPresent(); queued = data.pollEventQueue()) {
            if (MarketCategories.byId(queued.get()).filter(c -> c.unlocked(server)).isPresent()) {
                data.markDrawn(queued.get());
                return queued.get();
            }
        }
        RandomSource random = server.overworld().getRandom();
        List<String> unlocked = MarketCategories.unlocked(server).stream().map(c -> c.id().toString()).toList();
        if (unlocked.isEmpty()) {
            return "";
        }
        List<String> drawn = data.drawnEvents();
        List<String> open = unlocked.stream().filter(id -> !drawn.contains(id)).toList();
        if (open.isEmpty()) {
            data.resetDrawn();
            open = unlocked.stream().filter(id -> !id.equals(previous)).toList();
            if (open.isEmpty()) {
                open = unlocked;
            }
        }
        String target = open.get(random.nextInt(open.size()));
        data.markDrawn(target);
        return target;
    }

    /** Eine geladene und freigeschaltete Gruppe. Unbekannte Ziele (entfernte Gruppen) werden neu ausgelost. */
    private static boolean targetAvailable(MinecraftServer server, String target) {
        return MarketCategories.byId(target).map(c -> c.unlocked(server)).orElse(false);
    }

    /** Ganze Minuten bis zum Ereignis, aufgerundet. */
    public static long minutesLeft(MarketData data) {
        return Math.max(0, (data.eventSeconds() + 59) / 60);
    }

    /** Item-Tag der Warengruppe; {@code null} = alle Waren oder unbekannte Gruppe. */
    public static TagKey<Item> tag(String target) {
        for (MarketCategories.Category c : MarketCategories.all()) {
            if (c.id().toString().equals(target)) {
                return TagKey.create(Registries.ITEM, c.tag());
            }
        }
        return null;
    }

    /** Anzeigename des Ziels — gleiche Übersetzung wie die Filtergruppen am Marktstand. */
    public static Component targetName(String target) {
        Identifier id = Identifier.tryParse(target);
        if (target.isEmpty() || id == null) {
            return Component.translatable("dtp.event.all");
        }
        return Component.translatableWithFallback("dtp.category." + id.getPath(), id.getPath());
    }
}
