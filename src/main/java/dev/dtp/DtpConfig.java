package dev.dtp;

import java.util.List;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Welt-Config (SYNCED): alle Balancing-Werte, die nicht pro Item oder pro Upgrade im Datapack stehen.
 * Liegt als dtp-synced.toml im Ordner config; eine Kopie im Ordner syncedconfig einer Welt überschreibt sie für diese
 * Welt. Lässt sich zwischen zwei Sitzungen ändern und wird an die Clients übertragen.
 */
public final class DtpConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // --- Markt -------------------------------------------------------------------------------

    public static final ModConfigSpec.DoubleValue FACTOR_MIN = BUILDER
            .comment("Untergrenze des Preisbands (Anteil am Basiswert). Gilt auch außerhalb des Territoriums.")
            .defineInRange("market.factorMin", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue FACTOR_MAX = BUILDER
            .comment("Obergrenze des Preisbands (Anteil am Basiswert).")
            .defineInRange("market.factorMax", 2.5, 1.0, 100.0);

    public static final ModConfigSpec.DoubleValue FLOOR_RATIO = BUILDER
            .comment("Negativer Grundwert des Marktdrucks als Anteil von H: sFloor = -H * floorRatio.")
            .defineInRange("market.floorRatio", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue DEFAULT_H = BUILDER
            .comment("Halbwertsmenge H für Handelslisten-Einträge ohne eigenen Wert.")
            .defineInRange("market.defaultH", 500.0, 1.0, 1.0e9);

    public static final ModConfigSpec.DoubleValue DEFAULT_T = BUILDER
            .comment("Halbwertszeit T der Erholung in Minecraft-Tagen für Einträge ohne eigenen Wert.")
            .defineInRange("market.defaultT", 3.0, 0.01, 10000.0);

    public static final ModConfigSpec.DoubleValue PLAYER_SCALING = BUILDER
            .comment("Spielerskalierung: H_eff = H * (1 + (aktiveSpieler - 1) * playerScaling).")
            .defineInRange("market.playerScaling", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue ACTIVE_WINDOW_DAYS = BUILDER
            .comment("Ein Spieler gilt als aktiv, wenn er innerhalb so vieler Markttage online war (Markttage laufen nur mit Spielern online; eine Sitzung ≈ 10 Markttage).")
            .defineInRange("market.activeWindowDays", 30.0, 1.0, 10000.0);

    public static final ModConfigSpec.DoubleValue STABILITY_PER_LEVEL = BUILDER
            .comment("Server-Upgrade 'Langsamerer Preisverfall': H wächst je Stufe um diesen Anteil.")
            .defineInRange("market.stabilityPerLevel", 0.1, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue RECOVERY_PER_LEVEL = BUILDER
            .comment("Server-Upgrade 'Schnellere Preiserholung': T sinkt je Stufe um diesen Anteil (mindestens auf 20 %).")
            .defineInRange("market.recoveryPerLevel", 0.08, 0.0, 1.0);

    public static final ModConfigSpec.BooleanValue PROTOCOL = BUILDER
            .comment("Messprotokoll für Balancing-Runden: schreibt Verkäufe und Käufe nach <welt>/dtp/protocol.csv.")
            .define("market.protocol", true);

    // --- Marktereignisse ---------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue EVENTS_ENABLED = BUILDER
            .comment("Marktereignisse: In Abständen sinkt der Marktdruck einer Warengruppe oder aller Waren.")
            .define("events.enabled", true);

    public static final ModConfigSpec.IntValue EVENT_INTERVAL_MINUTES = BUILDER
            .comment("Wartezeit bis zum nächsten Ereignis in Minuten Serverlaufzeit (Echtzeit, nur während der Server läuft).")
            .defineInRange("events.intervalMinutes", 180, 1, 100000);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> EVENT_CONTRACT_MINUTES = BUILDER
            .comment("Jeder abgeschlossene Contract verkürzt die Wartezeit um so viele Minuten (Index 0 = Stufe 1).")
            .defineList("events.contractMinutes", List.of(5.0, 15.0, 30.0), () -> 5.0, o -> o instanceof Number n && n.doubleValue() >= 0);

    public static final ModConfigSpec.DoubleValue EVENT_WITHER_MINUTES = BUILDER
            .comment("Ein von einem Spieler besiegter Wither verkürzt die Wartezeit um so viele Minuten, einmal je geplantem Ereignis.")
            .defineInRange("events.witherMinutes", 30.0, 0.0, 100000.0);

    public static final ModConfigSpec.IntValue EVENT_WARNING_MINUTES = BUILDER
            .comment("So viele Minuten vor dem Ereignis erscheint eine Vorwarnung im Chat. 0 = keine Vorwarnung.")
            .defineInRange("events.warningMinutes", 10, 0, 10000);

    public static final ModConfigSpec.DoubleValue EVENT_PRESSURE_RELIEF = BUILDER
            .comment("Anteil, um den der Marktdruck an den Grundwert heranrückt (0,5 = halber Abstand).")
            .defineInRange("events.pressureRelief", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue EVENT_PRICE_BOOST = BUILDER
            .comment("Nachfrageschub eines Ereignisses: Der Marktdruck sinkt zusätzlich um so viele Halbwertsmengen H,",
                    "auch unter den Grundwert. 0,5 hebt eine Ware vom Ruhepreis (141 %) auf 200 %; danach sinkt sie mit der",
                    "Zeit wieder auf den Ruhepreis. 0 = kein Schub, nur Entlastung gesättigter Waren.")
            .defineInRange("events.priceBoost", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue EVENT_FAST_RECOVERY_CHANCE = BUILDER
            .comment("Chance, dass ein Ereignis eine Schnelle Erholung ist statt eines Nachfrageschubs. Die Schnelle Erholung",
                    "gilt immer für alle Waren und verbraucht keine Warengruppe aus der Reihum-Auslosung.")
            .defineInRange("events.fastRecovery.chance", 0.2, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue EVENT_FAST_RECOVERY_DAYS = BUILDER
            .comment("Laufzeit einer Schnellen Erholung in Markttagen. Solange sie läuft, erholen sich Preise unter dem Ruhepreis",
                    "doppelt so schnell (halbe Halbwertszeit T); Preise über dem Ruhepreis fallen nicht schneller.")
            .defineInRange("events.fastRecovery.days", 4.0, 0.05, 1000.0);

    public static final ModConfigSpec.DoubleValue EVENT_FAST_RECOVERY_PRESSURE_SHARE = BUILDER
            .comment("Anteil des Marktdrucks, den ein Verkauf während einer Schnellen Erholung erzeugt (0,67 = zwei Drittel).")
            .defineInRange("events.fastRecovery.pressureShare", 0.67, 0.05, 1.0);

    public static final ModConfigSpec.DoubleValue EVENT_FULL_MOON_BONUS = BUILDER
            .comment("Aufschlag auf den Preisfaktor der Monsterbeute in Vollmondnächten (0,15 = +15 %). 0 = aus.")
            .defineInRange("events.fullMoon.bonus", 0.15, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue EVENT_RAIN_BONUS = BUILDER
            .comment("Aufschlag auf den Preisfaktor der Fischerei, solange es in der Oberwelt regnet (0,15 = +15 %). 0 = aus.")
            .defineInRange("events.rain.bonus", 0.15, 0.0, 10.0);

    // --- Einzahlkiste ------------------------------------------------------------------------

    public static final ModConfigSpec.IntValue DEPOSIT_RADIUS = BUILDER
            .comment("Reichweite in Blöcken, in der Funktionsblöcke nach Einzahlkisten suchen.")
            .defineInRange("deposit.radius", 2, 1, 32);

    public static final ModConfigSpec.IntValue TRADE_CHEST_RADIUS = BUILDER
            .comment("Reichweite in Blöcken, in der der Marktstand Trade-Chests als Warenlager liest.")
            .defineInRange("market.tradeChestRadius", 2, 1, 32);

    // --- Contracts ---------------------------------------------------------------------------

    public static final ModConfigSpec.IntValue BOARD_SIZE = BUILDER
            .comment("Anzahl der Contracts im Aushang ohne Upgrade.")
            .defineInRange("contracts.boardSize", 4, 1, 20);

    public static final ModConfigSpec.DoubleValue ROTATION_DAYS = BUILDER
            .comment("Alle so viele Minecraft-Tage wird der Aushang neu gewürfelt. Dazwischen wird nicht nachgefüllt.")
            .defineInRange("contracts.rotationDays", 3.0, 0.1, 1000.0);

    public static final ModConfigSpec.IntValue BASE_SLOTS = BUILDER
            .comment("Gleichzeitig annehmbare Contracts ohne Upgrade.")
            .defineInRange("contracts.baseSlots", 2, 1, 10);

    public static final ModConfigSpec.IntValue BASE_MAX_TIER = BUILDER
            .comment("Höchste Contract-Stufe ohne Server-Upgrade. 0 = Contracts sind gesperrt, bis die erste Stufe am Beacon gekauft ist.")
            .defineInRange("contracts.baseMaxTier", 0, 0, 10);

    public static final ModConfigSpec.IntValue GUARANTEED_PER_TIER = BUILDER
            .comment("Mindestzahl an Angeboten je freigeschalteter Stufe im Aushang. Reicht der Platz nicht,",
                    "wird reihum verteilt (erst eines je Stufe, dann das nächste).")
            .defineInRange("contracts.guaranteedPerTier", 1, 0, 20);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> TIER_CHANCE = BUILDER
            .comment("Freie Plätze werden ausgewürfelt: von der höchsten freigeschalteten Stufe abwärts mit dieser Chance",
                    "je Stufe (Index 0 = Stufe 2, Index 1 = Stufe 3 ...). Schlagen alle Würfe fehl, wird es Stufe 1.",
                    "Fehlende Einträge übernehmen den letzten Wert der Liste. Kleinere Werte = mehr niedrige Stufen.",
                    "Bei 4 Stufen je freiem Platz (T1/T2/T3/T4): [0.4, 0.2, 0.1] = 43/29/18/10 % (Standard),",
                    "[0.5, 0.3, 0.2] = 28/28/24/20 %, [0.3, 0.15, 0.05] = 57/24/14/5 %.")
            .defineList("contracts.tierChance", List.of(0.4, 0.2, 0.1), () -> 0.4, o -> o instanceof Number n && n.doubleValue() >= 0 && n.doubleValue() <= 1);

    public static final ModConfigSpec.DoubleValue TIER_CHANCE_PER_LEVEL = BUILDER
            .comment("Server-Upgrade Contract-Qualität: hebt jede Würfelchance aus contracts.tierChance je Stufe um diesen Wert an",
                    "(höchstens auf 1). Mit dem Standard bei 4 Stufen je freiem Platz (T1/T2/T3/T4): ohne Upgrade 43/29/18/10 %,",
                    "Stufe 1 = 35/29/21/15 %, Stufe 2 = 28/28/24/20 %. Garantierte Plätze bleiben unberührt.")
            .defineInRange("contracts.tierChancePerLevel", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> TIER_REQUIREMENT = BUILDER
            .comment("Serverweit abgeschlossene Contracts, die nötig sind, um Stufe n am Beacon zu kaufen (Index 0 = Stufe 1).",
                    "Alle Spieler tragen gemeinsam dazu bei.")
            .defineList("contracts.tierRequirement", List.of(0, 5, 25, 60), () -> 0, o -> o instanceof Integer n && n >= 0);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> BONUS_DAYS = BUILDER
            .comment("Frist für den Zeitbonus je Stufe in Minecraft-Tagen (Index 0 = Stufe 1).")
            .defineList("contracts.bonusDays", List.of(3.0, 6.0, 9.0, 12.0), () -> 5.0, o -> o instanceof Number n && n.doubleValue() > 0);

    public static final ModConfigSpec.DoubleValue DECAY_INTERVAL_DAYS = BUILDER
            .comment("Länge eines Verfallsintervalls nach Ablauf der Frist in Minecraft-Tagen.")
            .defineInRange("contracts.decayIntervalDays", 1.0, 0.01, 1000.0);

    public static final ModConfigSpec.DoubleValue BONUS_FACTOR = BUILDER
            .comment("Wertformel: innerhalb der Bonusfrist Grundwert x bonusFactor; danach fällt der Bonus weg und es gilt",
                    "Grundwert x max(minFactor, 1 - decayPerInterval x volleIntervalle nach der Frist).",
                    "bonusFactor: Faktor innerhalb der Bonusfrist (1,2 = +20 %).")
            .defineInRange("contracts.bonusFactor", 1.2, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue DECAY_PER_INTERVAL = BUILDER
            .comment("Wertformel: Abzug je vollem Intervall nach der Bonusfrist (0,1 = -10 % des Grundwerts); das erste Intervall nach der Frist gilt 100 %.")
            .defineInRange("contracts.decayPerInterval", 0.1, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue MIN_FACTOR = BUILDER
            .comment("Wertformel: Untergrenze der Belohnung als Anteil des Grundwerts (0,5 = 50 %).")
            .defineInRange("contracts.minFactor", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> REWARD_MULTIPLIER = BUILDER
            .comment("Belohnung = Basiswert x Menge x Multiplikator je Stufe, sofern die Vorlage keinen festen Wert nennt (Index 0 = Stufe 1).",
                    "Höhere Stufen zahlen je Item mehr, weil sie Farmen und Zusammenarbeit verlangen.")
            .defineList("contracts.rewardMultiplier", List.of(2.0, 2.3, 2.75, 3.5), () -> 2.0, o -> o instanceof Number n && n.doubleValue() > 0);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> TIER_VALUE = BUILDER
            .comment("Wertziel je Stufe (Index 0 = Stufe 1): Marktwert der geforderten Menge zum Basiswert, im Mittel.",
                    "Vorlagen ohne feste Menge fordern Wertziel / Basiswert Stück — dieselbe Ware wächst so mit der Stufe.")
            .defineList("contracts.tierValue", List.of(800.0, 2800.0, 7000.0, 30000.0), () -> 700.0, o -> o instanceof Number n && n.doubleValue() > 0);

    public static final ModConfigSpec.DoubleValue TIER_VALUE_SPREAD = BUILDER
            .comment("Streuung um das Wertziel: 0,3 = zwischen 70 % und 130 %.")
            .defineInRange("contracts.tierValueSpread", 0.3, 0.0, 0.9);

    public static final ModConfigSpec.DoubleValue QUICK_DELIVERY_FACTOR = BUILDER
            .comment("Spieler-Upgrade Quick Delivery: Faktor innerhalb der Bonusfrist (1,3 = +30 %). Die Frist ist dieselbe wie ohne Upgrade.")
            .defineInRange("contracts.quickDeliveryFactor", 1.3, 0.0, 10.0);

    public static final ModConfigSpec.DoubleValue QUICK_DELIVERY_DECAY_MULTIPLIER = BUILDER
            .comment("Spieler-Upgrade Quick Delivery: Nach der Frist (und dem ersten Intervall zu 100 %) fällt der Wert um so ein Vielfaches schneller (2 = -20 % je Intervall).")
            .defineInRange("contracts.quickDeliveryDecayMultiplier", 2.0, 1.0, 10.0);

    // --- Beacon und Grenze -------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue BORDER_ENABLED = BUILDER
            .comment("Setzt die Vanilla-World-Border beim Start des Durchlaufs und bei jedem Radius-Upgrade.")
            .define("beacon.borderEnabled", true);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> RADIUS_CHUNKS = BUILDER
            .comment("Grenzradius in Chunks je Stufe (Index 0 = Startradius). Die Stufe nach dem letzten Eintrag entfernt die Grenze vollständig.")
            .defineList("beacon.radiusChunks",
                    List.of(12, 16, 20, 25, 31, 39, 49, 62, 80, 106, 140),
                    () -> 12, o -> o instanceof Integer i && i > 0);

    public static final ModConfigSpec.BooleanValue EFFECTS_ENABLED = BUILDER
            .comment("Beacon-Effekte verteilen. Der Hauptbeacon wirkt im Gebiet der höchsten Grenzstufe (letzter Eintrag",
                    "von radiusChunks), Neben-Beacons in ihrem eigenen Radius.")
            .define("beacon.effectsEnabled", true);

    public static final ModConfigSpec.IntValue RELAY_RADIUS_CHUNKS = BUILDER
            .comment("Reichweite der Effekte um einen Neben-Beacon in Chunks, als Radius eines Zylinders (unbegrenzt in der Höhe).",
                    "Nach dem Entfernen der Grenze wirken Neben-Beacons überall, auch jenseits der früheren Grenze.")
            .defineInRange("beacon.relayRadiusChunks", 5, 1, 1000);

    public static final ModConfigSpec.IntValue BREEDING_COOLDOWN_TICKS = BUILDER
            .comment("Beacon-Effekt Stallmeister (Zuchtpause): So viele Ticks warten Elterntiere nach dem Züchten im Radius",
                    "(Vanilla: 6000 = 5 min). Gilt für alle züchtbaren Tiere, auch Frosch, Sniffer und Schildkröte.")
            .defineInRange("beacon.breedingCooldownTicks", 4500, 600, 6000);

    public static final ModConfigSpec.IntValue CROP_GROWTH_BONUS = BUILDER
            .comment("Beacon-Effekt Landwirtschaft (Pflanzenwachstum): Chance in Prozent, dass eine Feldfrucht im Radius nach",
                    "einem Wachstumsschritt eine weitere Stufe wächst. 30 % = Weizen etwa ×1,26 so schnell (rund 20 % weniger",
                    "Wartezeit). Gilt für Feldfrüchte und Kürbis-/Melonenstängel. 0 schaltet das ab.")
            .defineInRange("beacon.cropGrowthBonus", 30, 0, 49);

    public static final ModConfigSpec.IntValue COMPOST_BONUS = BUILDER
            .comment("Beacon-Effekt Landwirtschaft (Kompost): Prozentpunkte, die Komposter im Radius je Item zusätzlich Chance",
                    "haben, sich zu füllen — additiv, Samen 30 % → 40 %. Gilt für Spieler, Trichter und Bauern. 0 schaltet das ab.")
            .defineInRange("beacon.compostBonus", 10, 0, 15);

    public static final ModConfigSpec.IntValue HORSE_INHERITANCE_BONUS = BUILDER
            .comment("Beacon-Effekt Stallmeister (Fohlen): Gesundheit, Sprungkraft und Tempo des Fohlens rücken um so viel Prozent der",
                    "Lücke zum Vanilla-Maximum nach oben (Pferd, Esel, Maultier, Lama). Das Maximum wird nie überschritten.")
            .defineInRange("beacon.horseInheritanceBonus", 10, 0, 50);

    public static final ModConfigSpec.IntValue BUILDER_REACH = BUILDER
            .comment("Beacon-Effekt Baumeister (Reichweite): zusätzliche Reichweite in Blöcken beim Bauen und Abbauen.")
            .defineInRange("beacon.builderReach", 2, 0, 8);

    public static final ModConfigSpec.IntValue FALL_DAMAGE_PERCENT = BUILDER
            .comment("Beacon-Effekt Federfall: so viel Prozent des Fallschadens bleiben im Radius übrig.")
            .defineInRange("beacon.fallDamagePercent", 50, 0, 100);

    public static final ModConfigSpec.IntValue ROAD_SPEED_BONUS = BUILDER
            .comment("Beacon-Effekt Stallmeister (Wege): Tempo-Aufschlag in Prozent auf Blöcken aus dem Tag dtp:road",
                    "(zu Fuß und für gelenkte Reittiere aus dem Tag dtp:mounts).")
            .defineInRange("beacon.roadSpeedBonus", 30, 0, 200);

    public static final ModConfigSpec.IntValue MOUNT_SPEED_BONUS = BUILDER
            .comment("Beacon-Effekt Stallmeister (Reittiere): Tempo-Aufschlag in Prozent für gelenkte Reittiere aus dem Tag",
                    "dtp:mounts im Radius (Pferdefamilie, Schwein, Schreiter, Nautilus, Glücklicher Ghast).",
                    "Addiert sich mit dem Wegebonus.")
            .defineInRange("beacon.mountSpeedBonus", 20, 0, 200);

    public static final ModConfigSpec.IntValue MOUNT_REST_SECONDS = BUILDER
            .comment("Beacon-Effekt Stallmeister (Ruhe): So viele Sekunden bleibt ein Reittier aus dtp:mounts stehen, nachdem ein",
                    "Spieler im Radius abgestiegen ist. Endet früher beim Anleinen oder Aufsteigen. 0 = aus.")
            .defineInRange("beacon.mountRestSeconds", 60, 0, 600);

    public static final ModConfigSpec.IntValue BEE_HONEY_CHANCE = BUILDER
            .comment("Beacon-Effekt Imkerei: Chance in Prozent auf eine zusätzliche Honigstufe, wenn eine Biene im Radius",
                    "Nektar im Stock abliefert (Vanilla: +1 Stufe je Ablieferung, voll bei 5).")
            .defineInRange("beacon.beeHoneyChance", 50, 0, 100);

    public static final ModConfigSpec.IntValue BEE_HARVEST_BONUS = BUILDER
            .comment("Beacon-Effekt Imkerei: Chance in Prozent auf eine zusätzliche Honigwabe (Schere) oder Honigflasche",
                    "(Glasflasche), wenn ein Spieler einen vollen Stock im Radius erntet. Spender sind ausgenommen.")
            .defineInRange("beacon.beeHarvestBonus", 100, 0, 100);

    public static final ModConfigSpec.IntValue BEE_POLLINATION_RADIUS = BUILDER
            .comment("Beacon-Effekt Imkerei: Bienen mit Nektar lassen alle 2 Sekunden eine Pflanze in diesem Radius (Blöcke,",
                    "bis 3 Blöcke unter sich) um eine Stufe wachsen. Vanilla wirkt nur direkt unter der Biene. 0 schaltet das ab.")
            .defineInRange("beacon.beePollinationRadius", 3, 0, 8);

    public static final ModConfigSpec.IntValue REBUILD_SECONDS = BUILDER
            .comment("Beacon-Effekt Baumeister (Rückbau): so viele Sekunden lang lässt sich ein selbst gesetzter Block sofort zurücknehmen.")
            .defineInRange("beacon.rebuildSeconds", 10, 1, 60);

    public static final ModConfigSpec.IntValue POTION_DURATION_PERCENT = BUILDER
            .comment("Beacon-Effekt Expeditionsvorbereitung: Dauer eines im Radius getrunkenen Tranks in Prozent der normalen Dauer.")
            .defineInRange("beacon.potionDurationPercent", 200, 100, 1000);

    public static final ModConfigSpec.IntValue POTION_KEEP_CHANCE = BUILDER
            .comment("Beacon-Effekt Expeditionsvorbereitung: Chance in Prozent, dass ein im Radius getrunkener Trank erhalten bleibt.")
            .defineInRange("beacon.potionKeepChance", 25, 0, 100);

    public static final ModConfigSpec.IntValue FOOD_SATURATION_BONUS = BUILDER
            .comment("Beacon-Effekt Expeditionsvorbereitung: zusätzliche Sättigung in Prozent für im Radius gegessenes Essen.")
            .defineInRange("beacon.foodSaturationBonus", 50, 0, 200);

    public static final ModConfigSpec.IntValue MENDING_AURA_REPAIR = BUILDER
            .comment("Server-Upgrade Mending-Aura Stufe I: Haltbarkeitspunkte je beschädigtem Item im ganzen Inventar alle 10 Sekunden, im Effektgebiet des Hauptbeacons und im Radius jedes Neben-Beacons.")
            .defineInRange("beacon.mendingAuraRepair", 1, 1, 100);

    public static final ModConfigSpec.IntValue MENDING_AURA_REPAIR_2 = BUILDER
            .comment("Server-Upgrade Mending-Aura Stufe II: Haltbarkeitspunkte je beschädigtem Item alle 10 Sekunden.")
            .defineInRange("beacon.mendingAuraRepair2", 2, 1, 100);

    public static final ModConfigSpec.IntValue BEACON_DEPOSIT_RADIUS = BUILDER
            .comment("Reichweite in Blöcken, in der der Beacon nach Einzahlkisten sucht. Größer als deposit.radius, damit die Kiste neben der Pyramide stehen kann.")
            .defineInRange("beacon.depositRadius", 16, 1, 32);

    public static final ModConfigSpec.IntValue BASE_EFFECT_SLOTS = BUILDER
            .comment("Gleichzeitig aktive Beacon-Effekte ohne Upgrade. Effekte lassen sich erst kaufen, wenn es mindestens einen Slot gibt.")
            .defineInRange("beacon.baseEffectSlots", 0, 0, 10);

    public static final ModConfigSpec.BooleanValue GIVE_GUIDE = BUILDER
            .comment("Beim ersten Betreten bekommt jeder Spieler das DTP-Handbuch, und die Grundrezepte stehen im Rezeptbuch.")
            .define("onboarding.giveGuide", true);

    public static final ModConfigSpec.BooleanValue DEEPSLATE_DARKNESS = BUILDER
            .comment("Darkness unter y = 0 in der Oberwelt, bis die Deepslate-Freischaltung gekauft ist.")
            .define("access.deepslateDarkness", true);

    public static final ModConfigSpec.BooleanValue NETHER_LOCK = BUILDER
            .comment("Harte Sperre für den Nether: Portale zünden nicht und Reisen in den Nether scheitern,",
                    "bis die Freischaltung am Beacon gekauft ist.")
            .define("access.netherLock", true);

    public static final ModConfigSpec.DoubleValue BLACKMARKET_SCAM_CHANCE = BUILDER
            .comment("Schwarzmarkt: Wahrscheinlichkeit, dass ein Handel ein Scam ist (0..1).")
            .defineInRange("blackmarket.scamChance", 0.2, 0.0, 1.0);

    public static final ModConfigSpec.IntValue BLACKMARKET_WEIGHT_STEAL = BUILDER
            .comment("Schwarzmarkt-Scam: Gewicht für „Item und Beutel sind weg“.")
            .defineInRange("blackmarket.weightSteal", 1, 0, 100);

    public static final ModConfigSpec.IntValue BLACKMARKET_WEIGHT_NOTHING = BUILDER
            .comment("Schwarzmarkt-Scam: Gewicht für „Beutel weg, nichts passiert“.")
            .defineInRange("blackmarket.weightNothing", 2, 0, 100);

    public static final ModConfigSpec.IntValue BLACKMARKET_WEIGHT_CURSE = BUILDER
            .comment("Schwarzmarkt-Scam: Gewicht für „Verzauberung steigt, aber das Item bekommt den Scam-Fluch“.")
            .defineInRange("blackmarket.weightCurse", 2, 0, 100);

    public static final ModConfigSpec.IntValue BLACKMARKET_WEIGHT_TOLL = BUILDER
            .comment("Schwarzmarkt-Scam: Gewicht für „Verzauberung steigt, aber das Werkzeug bekommt den Maut-Fluch“.",
                    "Zählt nur für Abbauwerkzeuge ohne Maut-Fluch.")
            .defineInRange("blackmarket.weightToll", 2, 0, 100);

    public static final ModConfigSpec.IntValue TOLL_PRICE = BUILDER
            .comment("Maut-Fluch: Taler je Abbau, bezahlt aus dem Inventar. Ein Mehrfachabbau zählt als ein Abbau. 0 schaltet die Maut ab.")
            .defineInRange("curse.tollPrice", 1, 0, 729);

    public static final ModConfigSpec.DoubleValue TOLL_SLOWDOWN = BUILDER
            .comment("Maut-Fluch: Faktor auf die Abbaugeschwindigkeit, wenn das Geld nicht reicht.")
            .defineInRange("curse.tollSlowdown", 0.1, 0.0, 1.0);

    // --- Schwarzmarkthändler -----------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue TRADER_ENABLED = BUILDER
            .comment("Schwarzmarkthändler: Er wird von Talern in Einzahlkisten angelockt.")
            .define("blackmarket.traderEnabled", true);

    public static final ModConfigSpec.IntValue TRADER_CHECK_SECONDS = BUILDER
            .comment("Schwarzmarkthändler: Abstand der Spawnprüfungen in Sekunden.")
            .defineInRange("blackmarket.checkIntervalSeconds", 60, 5, 3600);

    public static final ModConfigSpec.IntValue TRADER_ATTRACT_RADIUS = BUILDER
            .comment("Schwarzmarkthändler: In diesem Umkreis (Blöcke) um einen Spieler zählen die Einzahlkisten.")
            .defineInRange("blackmarket.attractRadius", 24, 1, 64);

    public static final ModConfigSpec.LongValue TRADER_MIN_CHEST = BUILDER
            .comment("Schwarzmarkthändler: Erst wenn eine einzelne Einzahlkiste mehr als so viele Taler hält, kann er erscheinen.")
            .defineInRange("blackmarket.minChestBalance", 60000L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue TRADER_BASE_CHANCE = BUILDER
            .comment("Schwarzmarkthändler: Chance je Prüfung, wenn die Taler in Reichweite genau der Schwelle entsprechen.",
                    "chance = min(maxChance, baseChance * (Taler in Reichweite / minChestBalance) ^ chanceExponent)")
            .defineInRange("blackmarket.baseChance", 0.03, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue TRADER_CHANCE_EXPONENT = BUILDER
            .comment("Schwarzmarkthändler: Wie stark mehr Taler die Chance erhöhen (0 = gar nicht, 1 = linear).")
            .defineInRange("blackmarket.chanceExponent", 0.5, 0.0, 4.0);

    public static final ModConfigSpec.DoubleValue TRADER_MAX_CHANCE = BUILDER
            .comment("Schwarzmarkthändler: Obergrenze der Chance je Prüfung.")
            .defineInRange("blackmarket.maxChance", 0.25, 0.0, 1.0);

    public static final ModConfigSpec.IntValue TRADER_STAY_MINUTES = BUILDER
            .comment("Schwarzmarkthändler: So viele Minuten bleibt er, bevor er verschwindet (20 = ein Markttag).")
            .defineInRange("blackmarket.despawnMinutes", 20, 1, 1440);

    public static final ModConfigSpec.IntValue TRADER_COOLDOWN_MINUTES = BUILDER
            .comment("Schwarzmarkthändler: Pause in Minuten, nachdem er verschwunden ist, bis der nächste erscheinen kann.")
            .defineInRange("blackmarket.cooldownMinutes", 120, 0, 100000);

    // --- Contraband-Items -------------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue CONTRABAND_ENABLED = BUILDER
            .comment("Contraband-Items (Feuerlöscher usw.): Rechtsklick wirkt. Aus = die Items bleiben, tun aber nichts.")
            .define("contraband.enabled", true);

    // --- Spielautomat -----------------------------------------------------------------------------

    public static final ModConfigSpec.DoubleValue SLOT_CHANCE_JACKPOT = BUILDER
            .comment("Spielautomat: Chance auf den Jackpot (drei Goldtaler, Einsatz x81).",
                    "Auszahlungsquote = 81 * jackpot + 9 * win + consolation / 9; mit den Standardwerten rund 88 %.",
                    "Liegt sie über 1, wird der Automat zur Geldquelle — das widerspricht dem Moddesign.")
            .defineInRange("slotMachine.chanceJackpot", 0.005, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue SLOT_CHANCE_WIN = BUILDER
            .comment("Spielautomat: Chance auf einen Gewinn (drei gleiche Symbole, Einsatz x9).")
            .defineInRange("slotMachine.chanceWin", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue SLOT_CHANCE_CONSOLATION = BUILDER
            .comment("Spielautomat: Chance auf den Trostpreis (Walze 1 und 2 gleich, eine Münze eine Stufe kleiner).",
                    "Ergeben die drei Chancen zusammen mehr als 1, fallen die hinteren der Reihe nach weg.")
            .defineInRange("slotMachine.chanceConsolation", 0.2, 0.0, 1.0);

    // --- Reparaturbank ----------------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue REPAIR_ENABLED = BUILDER
            .comment("Reparaturbank: Aus = der Block lässt sich setzen und öffnen, repariert aber nicht.")
            .define("repair.enabled", true);

    public static final ModConfigSpec.DoubleValue REPAIR_BASE_PRICE = BUILDER
            .comment("Reparaturbank: Taler je Haltbarkeitspunkt bei 250 maximaler Haltbarkeit (Eisenspitzhacke).",
                    "Preis je Punkt = basePrice * (maxHaltbarkeit / 250) ^ exponent")
            .defineInRange("repair.basePrice", 2.0, 0.001, 1000.0);

    public static final ModConfigSpec.DoubleValue REPAIR_EXPONENT = BUILDER
            .comment("Reparaturbank: Wie stark der Preis je Punkt mit der Haltbarkeit steigt.",
                    "0 = fester Kurs, 0.5 = volle Reparatur wächst mit Haltbarkeit^1.5 (Standard), 1 = quadratisch.")
            .defineInRange("repair.exponent", 0.5, 0.0, 2.0);

    // --- Chunkloader ------------------------------------------------------------------------------

    public static final ModConfigSpec.BooleanValue CHUNKLOADER_ENABLED = BUILDER
            .comment("Chunkloader: Aus = der Block lässt sich setzen, lädt aber keine Chunks und nimmt keine Taler an.")
            .define("chunkloader.enabled", true);

    public static final ModConfigSpec.LongValue CHUNKLOADER_PRICE = BUILDER
            .comment("Chunkloader: Preis einer Ladung in Talern, bezahlt aus Einzahlkisten in Reichweite.")
            .defineInRange("chunkloader.price", 2500L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue CHUNKLOADER_DURATION_TICKS = BUILDER
            .comment("Chunkloader: Laufzeit einer Ladung in Ticks (24000 = ein Minecraft-Tag). Sie zählt nur, solange ein Spieler online ist.")
            .defineInRange("chunkloader.durationTicks", 72000, 20, 24000 * 100);

    public static final ModConfigSpec.IntValue CHUNKLOADER_MAX_CHARGES = BUILDER
            .comment("Chunkloader: So viele Ladungen lassen sich höchstens im Voraus stapeln.")
            .defineInRange("chunkloader.maxCharges", 3, 1, 100);

    public static final ModConfigSpec.IntValue CHUNKLOADER_RADIUS_CHUNKS = BUILDER
            .comment("Chunkloader: Radius in Chunks um den eigenen Chunk (1 = 3x3, 2 = 5x5).")
            .defineInRange("chunkloader.radiusChunks", 1, 0, 4);

    public static final ModConfigSpec.IntValue CHUNKLOADER_DEPOSIT_RADIUS = BUILDER
            .comment("Chunkloader: Reichweite in Blöcken, in der er nach Einzahlkisten sucht.")
            .defineInRange("chunkloader.depositRadius", 16, 1, 32);

    public static final ModConfigSpec.BooleanValue CHUNKLOADER_NATURAL_SPAWNING = BUILDER
            .comment("Chunkloader: In geladenen Chunks spawnen Mobs natürlich, auch ohne Spieler in der Nähe (für Mob-Farmen).")
            .define("chunkloader.naturalSpawning", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private DtpConfig() {
    }

    public static double listValue(List<? extends Number> list, int index, double fallback) {
        if (list.isEmpty()) {
            return fallback;
        }
        return list.get(Math.min(index, list.size() - 1)).doubleValue();
    }
}
