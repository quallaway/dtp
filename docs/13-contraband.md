# Contraband

> Werkzeuge vom [Schwarzmarkthändler](15-blackmarket-trader.md) und aus [Strukturkisten](#fundorte-strukturkisten), die mit **Ladungen** statt mit Haltbarkeit arbeiten und nur mit Talern wieder aufgeladen werden.

---

## Rolle

Contraband-Items sind eine dauerhafte Talersenke neben Beacon und Werkstatt. Wer einmal einen Feuerlöscher oder eine Schleimrute gekauft hat, zahlt für jede Benutzung weiter in kleinen Beträgen. Das Item selbst bleibt dabei erhalten.

**Abgrenzung zu Vanilla-Werkzeugen:** Vanilla-Werkzeuge verschleißen und werden mit Material, Mending oder am Amboss repariert. Contraband-Items verschleißen nicht. Sie laufen leer und füllen sich ausschließlich gegen Taler wieder auf.

---

## Gemeinsame Regeln

- **Ladungen statt Haltbarkeit.** Die Komponente `dtp:charges` hält `charges`, `max` und `price`. Die Items haben kein `max_damage`. Deshalb greifen Mending, Mending-Aura, Amboss-Reparatur und Schleifstein nicht. Die Mending-Aura überspringt den Tag `dtp:contraband` zusätzlich.
- **Gehen nie kaputt.** Bei 0 Ladungen bleibt das Item erhalten und verweigert die Benutzung mit einer Meldung. Ausnahme wären Items mit `consume_when_empty`; die mitgelieferten nutzen das nicht.
- **Nicht verzauberbar.** Zaubertisch und Amboss nehmen sie nicht an. Auch per Befehl aufgebrachte Verzauberungen wirken nicht. Das gilt ausdrücklich für die Gun.
- **Rechtsklick und Cooldown.** Die Benutzung setzt den Vanilla-Cooldown für den Item-Typ, das Slot-Symbol zeigt ihn an.
- **Nur Erfolg kostet.** Bewirkt eine Benutzung nichts, zum Beispiel weil kein Feuer in Reichweite ist, kostet sie keine Ladung und startet keinen Cooldown.
- **Im Kreativmodus** kostet die Benutzung keine Ladung.
- **Nicht verkäuflich.** Contraband-Items stehen in keiner Handelsliste und zählen für keinen Contract.
- **Nicht stapelbar**, je Item ein Slot.
- **Abschaltbar** über `contraband.enabled`. Ist der Schalter aus, bleiben die Items im Spiel, tun aber nichts.

Balken (hellblau) und Tooltip zeigen Ladungen und Preis je Ladung.

---

## Aufladen

Aufgeladen wird an der [Reparaturbank](16-reparaturbank.md): das Item in den linken Slot, Taler **jeder Stückelung** in den rechten, dann **Reparieren**. Das Panel zeigt den Preis je Ladung, was das volle Aufladen kostet und was das eingelegte Geld bewirkt.

```
Ladungen danach = min(max, Ladungen + floor(gezahlt / Preis je Ladung))
```

- Die Bank nimmt Münzen **einzeln**, bis das Item voll ist oder der Slot leer.
- Zahlt die letzte Münze mehr, als nötig ist, kommt der Rest als **Wechselgeld** ins Inventar (in möglichst wenigen Münzen).
- Reicht das Geld nicht bis voll, gibt es so viele ganze Ladungen, wie es bezahlt; der Rest kommt ebenfalls zurück.
- Gesperrt ist das Aufladen, wenn das Item schon voll ist, das ganze Geld nicht für eine Ladung reicht oder ein Schwarzmarkt-Fluch darauf liegt.

Beispiel Feuerlöscher (16 Ladungen, 250 Taler je Ladung) mit 10 Ladungen, es fehlen 6 Ladungen = 1.500 Taler:

| Im Slot | Ergebnis |
|---|---|
| 1 Silbertaler (729) | 12 Ladungen, 229 Taler Wechselgeld |
| 3 Silbertaler (2.187) | 16 Ladungen, 687 Taler Wechselgeld |
| 4 Silbertaler | 16 Ladungen, 3 Münzen genommen, 687 Taler Wechselgeld, 1 Silbertaler bleibt im Slot |
| 3 Kupfersäcke (243) | gesperrt, reicht nicht für eine Ladung |

Früher lief das Aufladen über ein Crafting-Rezept ohne Rückgeld. Das Rezept gibt es nicht mehr ([Migration](72-migration.md)).

---

## Daten

**Wirkung im Code, Zahlen im Datapack.** Jedes Item hat eine Datei `data/<ns>/dtp/contraband/<item>.json`:

| Feld | Bedeutung | Standard |
|---|---|---|
| `max_charges` | größte Zahl an Ladungen | Pflicht |
| `cost_per_use` | Ladungen je Benutzung | 1 |
| `cooldown_seconds` | Sperre nach einer Benutzung | 0 |
| `charge_price` | Taler je Ladung | Pflicht |
| `consume_when_empty` | Item verschwindet bei 0 Ladungen | `false` |
| `params` | freie Zahlenwerte je Item, z. B. `radius` | leer |

- **Fehlt die Datei**, gelten die Werte aus dem Code.
- **Höchstwert und Preis im Stapel:** Der Stapel trägt eine Kopie davon, damit der Client Balken und Tooltip ohne eigene Synchronisierung zeichnen kann. Der Server gleicht die Kopie bei jeder Benutzung und Aufladung an die Datapack-Werte an; die Ladungen bleiben dabei erhalten, höchstens bis zum neuen Höchstwert.
- **Tag `dtp:contraband`:** Daran erkennen andere Systeme die Contraband-Items.

Der Kaufpreis beim Händler steht getrennt davon im Angebot (`data/dtp/villager_trade/blackmarket/contraband_<id>.json`, siehe [Schwarzmarkthändler](15-blackmarket-trader.md#angebot)).

---

## Die Contraband-Items

Werte aus `data/dtp/dtp/contraband/<id>.json`:

| Item | ID | Ladungen | Cooldown | Taler je Ladung | `params` |
|---|---|---|---|---|---|
| Feuerlöscher | `dtp:fire_extinguisher` | 16 | 1 s | 250 | `radius` 8 |
| Wachglocke | `dtp:watch_bell` | 16 | 10 s | 400 | `radius` 32, `seconds` 15 |
| Schleimrute | `dtp:oozing_rod` | 10 | 3 s | 400 | `search_chunks` 4, `outline_seconds` 6 |
| Spawnstab | `dtp:spawn_wand` | 16 | 5 s | 300 | `radius` 16, `seconds` 10, `max_marks` 400 |
| Taschengeld-Beutel | `dtp:pocket_money_pouch` | 8 | 25 s | 800 | `radius` 32, `seconds` 30 |
| Gun | `dtp:gun` | 8 | 0,5 s | 250 | `damage` 28, `range` 64 |
| Endlose Fackel | `dtp:endless_torch` | 256 | 0,25 s | 10 | – |

Die Schleimrute hieß bis 4.0.0 `dtp:dowsing_rod`. Ein Item-Alias (`ModItems`) erhält vorhandene Ruten in bestehenden Welten ([Migration](72-migration.md)).

### Feuerlöscher

Löscht Feuerblöcke und brennende Wesen im Würfel mit `radius` um den Spieler, den Benutzer eingeschlossen. Ohne Feuer kostet er nichts.

### Wachglocke

- Rechtsklick lässt alle Monster im Radius leuchten, Rechtsklick beim Schleichen alle Tiere.
- Das Leuchten ist der Vanilla-Effekt *Leuchten* für `seconds`. Der Umriss ist durch Wände sichtbar, und zwar **für alle Spieler**. Das ist bewusst so: Es ist einfach und kommt ohne Client-Code aus.
- Ton: Glocke (`BELL_BLOCK`).
- Ohne Ziel im Radius kostet die Benutzung nichts.

### Schleimrute

- Zeigt Slime-Chunks **ohne Text**, nur mit Partikeln und Ton, und **nur für den Benutzer**. Mehrere Slime-Chunks im Umkreis sind der Normalfall, darum zeigt die Rute alle und nicht nur den nächsten.
- **Funken wie ein Enderauge:** Zu jedem Slime-Chunk im Umkreis von `search_chunks` (ohne den eigenen) fliegt ein `END_ROD`-Funke waagrecht von der Brust zur Chunkmitte und verlischt. Die Flugweite zeigt die Entfernung: rund 3 Blöcke zu nahen, bis 10 zu fernen Chunks. Drei Wellen im Abstand von 10 Ticks, jede ab der aktuellen Position.
- **Eigener Chunk:** Ist er ein Slime-Chunk, zeichnen grüne Staubpartikel seine vier Kanten auf Fußhöhe nach, `outline_seconds` lang. Dazu ein heller Ton (Amethyst), sonst ein dumpfer.
- Ist weder der eigene noch ein Chunk im Umkreis ein Slime-Chunk, kommt ein Rauchwölkchen. Auch diese Auskunft kostet eine Ladung.
- Gerechnet wird mit der Vanilla-Formel (`WorldgenRandom.seedSlimeChunk(x, z, seed, 987234911L)`, Treffer bei `nextInt(10) == 0`).
- Nur in der Oberwelt. In anderen Dimensionen sagt die Rute als einzige Textmeldung, dass sie dort nichts findet, und kostet nichts.

### Spawnstab

- Zeigt für `seconds` alle Blöcke im Radius (8 Blöcke über und unter dem Spieler), auf denen ein Monster erscheinen könnte, mit roten Partikeln, **nur für den Benutzer**.
- Bedingungen sind der Vanilla-Platztest (`SpawnPlacements.isSpawnPositionOk` mit dem Zombie als Maßstab) und Blocklicht 0. Das Himmelslicht zählt nicht: Nachts zählt es ohnehin nicht, und der Stab soll auch tagsüber Stellen zeigen, die nachts gefährlich werden.
- Die Partikel kommen alle 10 Ticks erneut, höchstens `max_marks` Stellen, die nächsten zuerst.
- Ohne gefährliche Stelle kostet die Benutzung nichts.

### Taschengeld-Beutel

- Dorfbewohner im Radius laufen für `seconds` zum Spieler und bleiben bei ihm stehen. Dazu kommen Partikel (`HAPPY_VILLAGER`) und ein Münzklang.
- Ohne Dorfbewohner kostet die Benutzung nichts.
- **Technik:** Ein gesetztes Laufziel (`WALK_TARGET`) verliert ein Dorfbewohner auf zwei Wegen: Ist er angekommen, löscht Vanilla das Ziel und startet einen Zufallsspaziergang; die Wege zu Arbeitsplatz und Treffpunkt überschreiben es jederzeit. Deshalb prüft ein Tick-Handler direkt nach dem Gehirn-Takt **nur die angelockten Dorfbewohner** und nur für die Dauer der Wirkung. Ein fremdes Ziel wird weiter weg durch den Spieler ersetzt, in der Nähe (unter 3,5 Blöcken) gelöscht; dann bleibt der Dorfbewohner stehen und schaut den Spieler an. Ohne laufende Wirkung kostet der Handler nur einen Blick in eine leere Liste. Das ist eine bewusste Ausnahme von „nichts pro Tick“.

### Gun

- Hitscan: Der Strahl geht in Blickrichtung bis `range` und endet am ersten Block. Getroffen wird das erste Wesen davor.
- Der Schaden kommt aus dem eigenen Schadenstyp `dtp:gunshot` mit eigener Todesmeldung. Er läuft nicht über den Nahkampf, deshalb wirken weder Verzauberungen noch Werkstatt-Boni. Rüstung zählt.
- Gegen Spieler nur, wenn PvP an ist (das macht Vanilla von selbst).
- Spur aus Partikeln (`CRIT`), Knall-Ton.
- Ein Fehlschuss kostet eine Ladung, denn der Schuss ist gefallen.

### Endlose Fackel

- Rechtsklick auf einen Block setzt eine normale Fackel, je nach Seite als Boden- oder Wandfackel. Jede Fackel kostet eine Ladung, das Item bleibt.
- Der Name ist der Witz: Ladungen gibt es nur gegen Taler.

---

## Verbrauchsgegenstände vom Händler

Rückkehrrolle und Katalysatoren sind **kein Contraband**:
- Sie haben keine Ladungen und stapeln sich bis 16.
- Sie werden beim Benutzen verbraucht und nicht aufgeladen.
- Sie stehen nicht im Tag `dtp:contraband`.
- Nur ein Erfolg verbraucht ein Stück.

Beim Händler stehen sie im Set `rare`, mehrere Stück je Angebot ([Schwarzmarkthändler](15-blackmarket-trader.md#angebot)).

| Item | ID | Wirkung |
|---|---|---|
| Amethyst-Katalysator | `dtp:amethyst_catalyst` | Rechtsklick auf einen Amethystblock macht ihn zu knospendem Amethyst. Auf andere Blöcke wirkt er nicht und wird nicht verbraucht. |
| Regen-Katalysator | `dtp:rain_catalyst` | Bei klarem Himmel Regen für mindestens 10 Minuten. Regnet es schon, Gewitter für 10 Minuten. Bei Gewitter verlängert er es auf mindestens 10 Minuten; läuft es schon so lange, wirkt er nicht. Nur in der Oberwelt. Tooltip: „Es verschlechtert das Wetter.“ |
| Rückkehrrolle | `dtp:return_scroll` | 3 Sekunden lesen (Rechtsklick halten, Vanilla-Komponente `consumable`; Loslassen bricht ab), dann Teleport auf den Hauptbeacon in der Oberwelt. Das geht aus jeder Dimension. Ohne gesetzten Beacon wirkt sie nicht und wird nicht verbraucht. |

Die Rückkehrrolle hat **kein** Crafting-Rezept. Der Weg zurück zum Beacon ist damit eine Händlerware ([Territorium](20-territorium-und-serverweite-effekte.md)).

---

## Fundorte: Strukturkisten

Contraband und die drei Verbrauchsgegenstände liegen auch in Strukturkisten. Dadurch kommt man früher an einzelne Items, aber kaum an alle, bevor der Server den Händler trifft.

- **Chance:** 20 % je Kiste auf **einen** Fund. Wann gewürfelt wird, entscheidet Vanilla: beim ersten Öffnen, beim Abbauen oder wenn ein Trichter zieht.
- **Welche Kisten:** jede Loot-Table mit Pfad `chests/…`, in jedem Namespace. Dazu gehören Dungeons, Minenschächte, Tempel, Schiffswracks, Nether-Festungen, Bastionen, Endstädte und Kisten-Loren, ebenso Strukturkisten anderer Mods.
- **Ausgenommen:** Bonustruhe (`chests/spawn_bonus_chest`), Dorfkisten (`chests/village/…`), Trial-Chamber-Tresore (`chests/trial_chambers/reward…`), der Dispenser im Dschungeltempel (`chests/jungle_temple_dispenser`) und alle Tabellen im Namespace `dtp`.
- **Rucksack-Mobs** (Sophisticated Backpacks) würfeln Vanilla-Kisten-Tabellen und können deshalb ebenfalls Funde tragen. Das ist gewollt.
- **Beim Angeln gibt es keine Funde.** Die Angel-Tabelle liegt nicht unter `chests/`, und die Fundliste des Angelschatzes wird ohne Loot-Modifier gewürfelt.
- **Ladungen:** Gefundenes Contraband ist zu 25–75 % geladen (gleichverteilt, abgerundet), bezogen auf `max_charges` aus der Datapack-Definition.
- **Abschaltbar:** Ist `contraband.enabled` aus, gibt es keine Funde.
- **Taler** werden unabhängig davon gewürfelt, auch in Dorfkisten ([Markt](10-markt.md#taler-in-strukturkisten)).

Die Gewichte sind ungefähr umgekehrt proportional zum Händlerpreis:

| Item | Gewicht | Stück | je Kiste |
|---|---|---|---|
| Rückkehrrolle | 14 | 1–2 | 2,8 % |
| Regen-Katalysator | 13 | 1 | 2,6 % |
| Amethyst-Katalysator | 13 | 1–2 | 2,6 % |
| Endlose Fackel | 12 | 1 | 2,4 % |
| Schleimrute | 12 | 1 | 2,4 % |
| Wachglocke | 9 | 1 | 1,8 % |
| Spawnstab | 9 | 1 | 1,8 % |
| Feuerlöscher | 7 | 1 | 1,4 % |
| Taschengeld-Beutel | 7 | 1 | 1,4 % |
| Gun | 4 | 1 | 0,8 % |

Contraband kommt damit aus rund 12 % aller Strukturkisten.

**Daten:**
- `data/dtp/loot_modifiers/structure_finds.json` enthält `chance`, `min_charge_fraction`, `max_charge_fraction` und `exclude` (Pfad-Präfixe).
- `data/dtp/loot_table/gameplay/blackmarket_finds.json` ist die Fundliste mit Gewichten und Stückzahlen.

---

## Technik

- **`ContrabandItem`** (`dev.dtp.contraband`): Basisklasse. Eine Unterklasse liefert nur `activate(...)`; Ladungen, Cooldown, Sound, Meldungen, Balken und Tooltip regelt die Basisklasse. `ContrabandItem.create(item, charges)` erzeugt einen Stapel mit gegebenen Ladungen.
- **`ContrabandItem.activateOn(...)`:** zweiter Einstieg für Klicks auf Blöcke, eingeschaltet über `usesBlocks()`. Die Endlose Fackel nutzt ihn.
- **`UseOutcome`:** gemeinsames Ergebnis einer Wirkung. `Used` kostet etwas, `Refused` mit oder ohne Meldung nicht. Auch `TraderConsumableItem` (`dev.dtp.supplies`), die Basis der Verbrauchsgegenstände, nutzt es.
- **`Lingering`:** Wirkungen, die eine Weile anhalten (Partikel von Schleimrute und Spawnstab, nur für den Benutzer), werden alle 10 Ticks erneuert, nicht pro Tick. Nichts davon wird gespeichert; nach einem Neustart ist eine laufende Wirkung vorbei.
- **Aufladen:** `dev.dtp.repair.RepairStation` (Reparaturbank). Fehlende Ladungen zählen dort wie Haltbarkeitspunkte, Preis je Einheit ist `chargePrice`. Messprotokoll-Aktion `recharge`.
- **`StructureFindsModifier`** (`dev.dtp.blackmarket`): Global Loot Modifier für die Strukturkisten. Er prüft die Tabellen-ID im Code, weil `neoforge:loot_table_id` nur exakte IDs kennt. Die Fundliste würfelt er per `getRandomItemsRaw`, Contraband bekommt danach seine Ladung.
- **Optik:** Eigene 16×16-Texturen aus `scripts/item_textures.py`. Werkzeuge (Fackel, Schleimrute, Spawnstab) liegen diagonal und nutzen `item/handheld`, alles andere `item/generated`. Die Gun hat eigene Hand-Transformationen in `models/item/gun.json`; wer sie hält, streckt den Arm nach vorn (Armhaltung `DTP_GUN_AIM` aus `DtpArmPoses`, eingehängt per `META-INF/enumextensions.json` und `IClientItemExtensions#getArmPose`).
- **GameTests:** `contraband_*` und `supplies_*`, mindestens einer pro Item; `structure_finds_table` und `structure_finds_chest` für die Fundorte (`structure_taler` für die Taler) ([Entwicklung und Test](71-entwicklung-und-test.md)).
