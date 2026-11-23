# Beacon-Effekte

> Effektslots, Reichweite und alle Effekte, die der Hauptbeacon und die Neben-Beacons ausstrahlen. Grenze, Hauptbeacon und serverweite Upgrades stehen in [Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md).

---

## Effektslots

Ein Effekt wirkt nur, solange er in einem **Effektslot** steckt. Ohne Upgrade gibt es keinen (`beacon.baseEffectSlots` = 0).

| ID | Name | Stufe | Preis | Slots danach |
|---|---|---|---|---|
| `effect_slots` | Effektslot | I | 10.000 | 1 |
| | | II | 50.000 | 2 |
| | | III | 150.000 | 3 |

**Jeder Effekt setzt Effektslot I voraus** (`requires` im Datapack, zusätzlich im Code geprüft). Sonst wäre er bezahlt, aber wirkungslos.

Das ist die wichtigste Balancing-Entscheidung am Beacon: Mit der Zeit besitzt die Gruppe viele Effekte, kann aber nur wenige gleichzeitig nutzen. 11 Effekte stehen höchstens 3 Slots gegenüber. Der Beacon bleibt dadurch eine Entscheidung statt einer Sammlung.

Gekauft werden Slots und Effekte am Hauptbeacon im Reiter **Module**. Der Reiter **Effekte** ist die Auswahl: oben die belegten Slots, darunter alle Effekte, gesperrte abgeblendet. **Umstecken ist kostenlos und jederzeit möglich.** Jeder Spieler darf umstecken. Sinkt die Slotzahl (etwa nach einer Config-Änderung), fallen die letzten Einträge weg.

---

## Reichweite

Die **Stärke** eines Effekts ist an Haupt- und Neben-Beacon gleich, nur die Fläche unterscheidet sich. Gleiche Effekte stapeln sich nicht: Wo Haupt- und Neben-Beacon denselben Effekt tragen, wirkt er einmal. Die Höhe zählt nie, damit Effekte auch beim Bergbau tief unter der Basis wirken.

| | Hauptbeacon | Neben-Beacon |
|---|---|---|
| Fläche | Quadrat im Gebiet der **Grenzstufe 10** (140 Chunks = 2.240 Blöcke um den Beacon, letzter Eintrag von `beacon.radiusChunks`) | Kreis mit **5 Chunks** = 80 Blöcken um den Block (`beacon.relayRadiusChunks`) |
| Dimension | Nur Oberwelt | Jede Dimension, jeweils nur die eigene |
| Voraussetzung | Durchlauf gestartet | Keine: wirkt unabhängig von Durchlauf, Territorium und Grenze |
| Slots | Effektslots (0–3) | max(1, Effektslots − 1), also 1 / 1 / 2 |
| Auswahl | Eigene Auswahl | Eigene Auswahl aus den am Hauptbeacon freigeschalteten Effekten |
| Kaufen | Ja | Nein, nur Auswahl |

### Hauptbeacon

Das Effektgebiet ist **unabhängig von der aktuellen Grenzstufe** immer das Gebiet der Stufe 10, quadratisch wie die Grenze, damit auch ihre Ecken abgedeckt sind. Solange eine Grenze steht, ist damit jeder erreichbare Ort der Oberwelt versorgt, und ein eigener Radius ist überflüssig. Erst nach dem Aufheben der Grenze (Stufe 11) endet die Wirkung des Hauptbeacons an diesem Gebiet.

Der Radius wird nicht durch Partikel markiert. Wer im Wirkungsbereich steht, sieht das an den aktiven Effekten.

### Neben-Beacon

Der Neben-Beacon (`dtp:relay_beacon`, Rezept in [Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md#neben-beacons)) ist die teure Möglichkeit, Effekte gezielt an einen Ort zu bringen, ohne den Radius global zu vergrößern: an einen Außenposten außerhalb des Territoriums, an eine Lohenfarm im Nether, an ein Basislager im End. Innerhalb des Territoriums ergänzt er die Effekte des Hauptbeacons in einem kleinen Bereich um **weitere**.

Ohne Slot und ohne Auswahl trägt jeder Neben-Beacon außerdem die ortsgebundenen Server-Upgrades Ofentempo und Mending-Aura, sobald sie gekauft sind ([Serverweite Upgrades](20-territorium-und-serverweite-effekte.md#reiter-welt)).

Er ist abbaubar, aber nicht verschiebbar. Seine Position und Auswahl liegen am Server, nicht am Block. Fehlt der Block, fällt der Eintrag beim nächsten Effekttakt weg. Im Radius eines Neben-Beacons lassen sich auch persönliche Module umstecken ([Module und Fähigkeiten](31-module-und-faehigkeiten.md)).

### Takt

Vanilla-Effekte werden alle 80 Ticks an die Spieler im Wirkungsbereich verteilt, mit 400 Ticks Dauer (sonst flackert Nachtsicht), als Umgebungseffekt ohne Partikel. Eigene Effekte wirken über Events und prüfen am Ereignisort, ob dort ein Beacon den Effekt trägt (`BeaconLogic.effectAt`). Über Blöcke, Tiere oder Bienen im Radius wird nie iteriert. Mit `beacon.effectsEnabled = false` verteilt der Beacon keine Effekte.

---

## Die Effekte

Alle 11 Effekte sind Freischaltungen mit einer Stufe, gekauft am Hauptbeacon, und belegen einen Effektslot. Alle kommen ohne Mixin aus.

| ID | Name | Preis | Kurz |
|---|---|---|---|
| `effect_jump_boost` | Sprungkraft | 6.000 | Sprungkraft I |
| `effect_night_vision` | Nachtsicht | 6.000 | Nachtsicht |
| `effect_water_breathing` | Wasseratmung | 6.000 | Wasseratmung |
| `effect_resistance` | Resistenz | 30.000 | Resistenz I |
| `effect_agriculture` | Landwirtschaft | 30.000 | Trampelschutz, Pflanzenwachstum, Kompost |
| `effect_stablemaster` | Stallmeister | 12.000 | Zuchtpause, bessere Fohlen, Reittiere schneller, ohne Fallschaden und nach dem Absteigen ruhig, +30 % Tempo auf `dtp:road` |
| `effect_beekeeping` | Imkerei | 15.000 | Mehr Honig, Ernte-Bonus, Bestäubung, friedliche Bienen |
| `effect_builder` | Baumeister | 15.000 | +2 Blöcke Reichweite, selbst gesetzte Blöcke 10 s lang sofort zurücknehmen |
| `effect_feather_fall` | Federfall | 8.000 | Fallschaden halbiert |
| `effect_quiet_nights` | Ruhige Nächte | 6.000 | Keine Phantome |
| `effect_expedition` | Expeditionsvorbereitung | 25.000 | Längere Tränke, mehr Sättigung |

### Spielereffekte

Sprungkraft, Nachtsicht, Wasseratmung und Resistenz sind Vanilla-Statuseffekte, **immer auf Stufe I**. Sie haben keine Rückkopplung auf die Wirtschaft. Regeneration, Bewegungs- und Abbautempo, Sättigung und Feuerresistenz gibt es bewusst nicht als Beacon-Effekt.

### Landwirtschaft

| Teil | Wirkung | Config |
|---|---|---|
| **Trampelschutz** | Ackerboden wird nicht zu Erde, egal wer darauf springt oder fällt: Spieler, Mobs, Villager, Tiere, Reittiere. Damit bleiben auch die Pflanzen stehen. Hasen, die Karotten fressen, sind nicht betroffen | – |
| **Pflanzenwachstum** | Nach jedem Wachstumsschritt wächst eine Feldfrucht mit 30 % Chance eine weitere Stufe. Weizen, Karotten, Kartoffeln, Rote Bete, Netherwarzen, Fackellilie, Kürbis- und Melonenstängel (Stängel nur bis zur letzten Stufe, das Ansetzen der Frucht bleibt Vanilla). Die Kannenpflanze wächst nicht schneller: `PitcherCropBlock` feuert in NeoForge kein `CropGrowEvent`, und ohne Mixin oder Block-Iteration im Radius gibt es keinen anderen Haken | `beacon.cropGrowthBonus` (30, 0–49) |
| **Kompost** | Komposter im Radius füllen sich je Item mit 10 Prozentpunkten mehr Chance, additiv: 30/50/65/85 % → 40/60/75/95 % (Samen, Laub, Gras → 40 %). Das erste Item in einem leeren Komposter füllt ihn wie in Vanilla immer. Gilt für Spieler, Trichter und Bauern-Villager | `beacon.compostBonus` (10, 0–15) |

Das Pflanzenwachstum hängt an `CropGrowEvent.Post`, also an einem echten Wachstumsschritt. Ein erzwungenes Wachsen in `Pre` würde schlecht angelegte Felder bis ×4 beschleunigen, eine Zusatzstufe nach jedem Schritt ist dagegen ein fester Faktor, egal wie das Feld angelegt ist. Der Trampelschutz bricht `BlockEvent.FarmlandTrampleEvent` ab. Der Kompost-Bonus braucht kein Event: In 26.3 sind die Kompost-Chancen datengetrieben (`minecraft:context_int_provider/compostable/low`, `low_medium`, `medium`, `medium_high`). Der Mod überschreibt diese vier Dateien und ersetzt die Gewichtsliste durch `dtp:compost_chance`, das am Komposter (`ORIGIN`) den Effekt prüft. `ComposterBlock.addLayer` wertet sie für jeden Befüllweg aus. Tiere betrifft Landwirtschaft nicht mehr, die Zuchtpause gehört zum [Stallmeister](#stallmeister).

| Pflanze | Stufen | Schritte Vanilla → mit Effekt | Tempo |
|---|---|---|---|
| Weizen, Karotte, Kartoffel, Stängel | 7 | 7 → 5,56 | ×1,26 |
| Rote Bete, Netherwarze | 3 | 3 → 2,49 | ×1,20 |
| Fackellilie | 2 | 2 → 1,70 | ×1,18 |

Bei `randomTickSpeed` 3 braucht Weizen auf einem ideal angelegten Feld im Schnitt etwa 24 statt 19 Minuten bis zur Reife, auf einem bewässerten Vollfeld einer Sorte etwa 48 statt 38 Minuten.

### Stallmeister

Alles für Tiere und Reittiere. Bis Version 4.3 waren das die Effekte Pferdezucht und Straßenbau, die Zuchtpause gehörte zur Landwirtschaft ([Migration](72-migration.md)).

| Teil | Wirkung | Config |
|---|---|---|
| **Zuchtpause** | Eltern sind nach 4.500 statt 6.000 Ticks (3¾ statt 5 Minuten) wieder zuchtbereit. Alle züchtbaren Tiere, auch Frosch, Sniffer und Schildkröte, die Eier legen. Dorfbewohner sind keine Tiere und bleiben ausgenommen | `beacon.breedingCooldownTicks` (4500, 600–6000) |
| **Fohlen** | Gesundheit, Sprungkraft und Tempo rücken nach Vanillas Wurf um 10 % der Lücke zum Vanilla-Maximum nach oben, nie darüber. Pferd, Esel, Maultier, Lama, Händlerlama (`Horse` und `AbstractChestedHorse`). Die übrigen Reittiere erben keine Werte | `beacon.horseInheritanceBonus` (10) |
| **Reittiere** | Gelenkte Reittiere sind 20 % schneller. Fallschaden nehmen Reittiere im Radius keinen, auch unberitten, ihre Reiter auch nicht | `beacon.mountSpeedBonus` (20) |
| **Ruhe** | Steigt ein Spieler im Radius ab, bleibt das Reittier 60 s stehen, damit es sich in Ruhe anleinen lässt. Gilt für alle Reittiere, auch ungelenkte (Lama, ungesatteltes Pferd). Endet früher beim Anleinen und beim Aufsteigen | `beacon.mountRestSeconds` (60, 0–600, 0 = aus) |
| **Wege** | +30 % Tempo auf Blöcken aus dem Tag `dtp:road`, zu Fuß und auf gelenkten Reittieren. Standard: Trampelpfad, Kies, Grobe Erde, Wurzelerde, per Datapack änderbar. Nicht in Boot oder Lore. Addiert sich mit dem Reittier-Tempo | `beacon.roadSpeedBonus` (30) |

**Reittiere** sind alle Wesen im Entity-Tag `dtp:mounts` (per Datapack erweiterbar): Pferd, Esel, Maultier, Lama, Händlerlama, Dromedar, Wüstendromedar, Skelett- und Zombiepferd, Schwein, Schreiter, Nautilus, Zombie-Nautilus und Glücklicher Ghast. Tempo gibt es nur, wenn der Spieler das Tier selbst lenkt (`getControllingPassenger`): Pferde brauchen einen Sattel, Schweine und Schreiter zusätzlich die Angel in der Hand, der Glückliche Ghast ein Geschirr. Lamas lassen sich nicht lenken, sie bekommen nur den Fallschutz. Der Glückliche Ghast rechnet sein Reittempo aus `FLYING_SPEED`, alle anderen aus `MOVEMENT_SPEED`; der Modifier sitzt jeweils auf dem passenden Attribut.

Der Stallmeister nützt auch, wenn alle Pferde schon ausgezüchtet sind. Er setzt alle 10 Ticks einen Tempo-Modifier am Reittier (nach dem Absteigen wird er entfernt) und bricht den Fallschaden über `LivingIncomingDamageEvent` ab, weil Pferde den Fallschaden ohne `LivingFallEvent` berechnen.

**Ruhe, Technik.** `EntityMountEvent` beim Absteigen setzt einen flüchtigen Modifier −100 % (`ADD_MULTIPLIED_TOTAL`) auf `MOVEMENT_SPEED` und `FLYING_SPEED`; das Tier kann dann weder wandern noch fliegen, wird aber von Leine und Stößen weiter bewegt. Der 10-Tick-Durchlauf beendet die Ruhe nach Ablauf, beim Anleinen und wenn wieder jemand aufsitzt; Aufsteigen beendet sie schon im Event. Weil der Modifier flüchtig ist, endet die Ruhe auch beim Entladen des Tiers. Gemerkt werden nur ruhende Tiere, über Tiere im Radius wird nicht iteriert.

Der Wegebonus belohnt Infrastruktur statt pauschales Tempo wie am Vanilla-Beacon. Trampelpfad ist eindeutig, in der Natur fast nie vorhanden und mit der Schaufel billig in Strecke gebaut. Der Standard-Tag enthält außerdem Kies, Grobe Erde und Wurzelerde.

**Zuchtpause, Technik.** Tiere, die ein Junges werfen, laufen über `BabyEntitySpawnEvent`. Die Pause wird am Ende des Ticks verkürzt, weil Vanilla sie erst nach dem Event auf 6000 setzt. Frosch, Sniffer und Schildkröte setzen die Pause ohne dieses Event (Laich, Ei oder Gelege statt Junges). Für sie prüft `EntityTickEvent.Post` per `instanceof`, ob das Alter gerade frisch auf 6000 steht. Nur dann wird der Radius geprüft. Über Tiere im Radius wird nicht iteriert.

### Imkerei

| Teil | Wirkung | Config |
|---|---|---|
| **Nestfüllung** | Liefert eine Biene Nektar ab, steigt die Honigstufe mit 50 % Chance um eine weitere Stufe | `beacon.beeHoneyChance` (50) |
| **Ernte-Bonus** | Erntet ein Spieler einen vollen Stock mit Schere oder Glasflasche, gibt es eine Wabe bzw. Honigflasche mehr, mit der Glasflasche also 2 Honigflaschen. Spender sind ausgenommen | `beacon.beeHarvestBonus` (100) |
| **Bestäubung** | Bienen mit Nektar lassen alle 2 Sekunden eine Pflanze im Umkreis von 3 Blöcken (bis 3 Blöcke unter sich) um eine Stufe wachsen. Vanilla wirkt nur direkt unter der Biene. Wachsen können Feldfrüchte, Stängel, Süßbeeren, Höhlenranken und die Kannenpflanze aus `#bee_growables`. Die Kannenpflanze lässt Vanilla trotz Tag aus; hier wächst sie per Knochenmehl-Logik um eine Stufe, egal welche Hälfte die Biene trifft | `beacon.beePollinationRadius` (3) |
| **Friedlich** | Bienen nehmen keine Spieler ins Visier, auch nicht beim Ernten ohne Lagerfeuer oder wenn man sie schlägt. Erntet ein Spieler ohne Lagerfeuer, bleiben die Bienen wie unter einem Lagerfeuer im Stock und behalten ihre Brutzeit | – |

Vanilla verwirft die UUID einer Biene beim Einzug in den Stock. Die Nestfüllung hängt deshalb am Block-Update der Honigstufe (`NeighborNotifyEvent`), verrechnet am Ende des Ticks. Sie zählt nur, wenn im selben Tick auch eine Biene aus genau diesem Stock in die Welt kam (`EntityJoinLevelEvent` mit passender `hivePos`); sonst ließe sich jedes Block-Update (Hebel, Redstone, Neu-Setzen mit Behutsamkeit) als Ablieferung ausgeben. Beim Ernten ohne Lagerfeuer bricht derselbe Hook den Beitritt der freigelassenen Bienen ab, solange die Honigstufe 0 ist: `addFreshEntity` scheitert und Vanilla behält sie im Stock. Vorher tauchten sie für einen Tick auf und flogen mit Nektar sofort wieder hinein, mit zurückgesetzter Brutzeit. Ob wirklich geerntet wurde, zeigt erst die Honigstufe 0 am Tickende. Such-Radius für Blumen und Stock, Ausflüge bei Regen oder Nacht und die Verweildauer im Stock bleiben Vanilla.

### Bauen

| Effekt | Wirkung | Config | Technik |
|---|---|---|---|
| **Baumeister: Reichweite** | +2 Blöcke Reichweite beim Bauen und Abbauen | `beacon.builderReach` (2) | Attribut-Modifier `block_interaction_range`, alle 10 Ticks gesetzt oder entfernt |
| **Baumeister: Rückbau** | Selbst gesetzte Blöcke lassen sich 10 s lang sofort abbauen und kommen zurück ins Inventar, ohne Werkzeugverschleiß. Nicht für Blöcke mit Inhalt (BlockEntity) wie Truhen | `beacon.rebuildSeconds` (10) | siehe unten |
| **Federfall** | Fallschaden halbiert, nur für Spieler (Tiere und Monster nicht; Reittiere schützt der Stallmeister) | `beacon.fallDamagePercent` (50 = Anteil, der übrig bleibt) | `LivingFallEvent` |
| **Ruhige Nächte** | Keine Phantome für Spieler im Radius. Der Preis ist der Slot, und ohne Phantome gibt es keine Phantomhaut | – | `PlayerSpawnPhantomsEvent` |

Der Baumeister bündelt beides, was beim Bauen Wege spart: weiter greifen und Fehlgriffe oder Hilfsblöcke sofort zurücknehmen. Bis Version 4.3 war der Rückbau ein eigener Effekt.

**Rückbau, Technik.** Der Server merkt sich je Spieler die zuletzt gesetzten Blöcke (`BlockEvent.EntityPlaceEvent`), höchstens 64, nur im Speicher und nur dort, wo Rückbau wirkt. Die Rückgabe wird beim Setzen berechnet, als Drops einer Netherite-Spitzhacke mit Behutsamkeit: Glas, Eis und Blätter kommen zurück, eine Doppelstufe gibt zwei Stufen, Blöcke ohne Drops (obere Türhälfte) werden nicht gemerkt. Weil der Client das Abbautempo vorrechnet, bekommt er jede Markierung als kleines Paket. `PlayerEvent.BreakSpeed` gibt auf beiden Seiten Sofortabbau, wenn Position, unveränderter Blockzustand und Spieler passen. Beim Abbau entfernt der Server den Block selbst und legt die gemerkten Items ins Inventar, das Werkzeug wird nicht benutzt, Glück bringt nichts extra.

### Expeditionsvorbereitung

Die Basis wird zum Ort, an dem man sich für den Aufbruch rüstet.

| Wirkung | Config |
|---|---|
| Im Radius getrunkene Tränke halten **doppelt so lang**, auch nach dem Verlassen des Radius | `beacon.potionDurationPercent` (200) |
| Der Trank bleibt mit 25 % Chance ganz erhalten | `beacon.potionKeepChance` (25) |
| Im Radius gegessenes Essen gibt +50 % Sättigung, höchstens bis zur Hungerleiste | `beacon.foodSaturationBonus` (50) |

Beacon-Effekte und Sofortwirkungen (Heilung, Schaden) sind nicht betroffen. Tränke bleiben Tränke: Der Effekt senkt den Aufwand drumherum, statt sie zu ersetzen. Technik: `LivingEntityUseItemEvent.Finish`.

---

## Verkaufspreise bleiben unberührt

Kein Beacon-Effekt hebt Verkaufspreise an. Ein ortsgebundener Preisbonus würde zum Verkaufen an einem bestimmten Ort zwingen und wäre der riskanteste Hebel im ganzen Mod. Die Linie dahinter gilt für alle Effekte: **Lieber den Ertrag erhöhen als den Preis.** Mehr Ernte ist eine Produktionsverbesserung, die der Markt anschließend normal bewertet ([Balancing](62-balancing.md)).
