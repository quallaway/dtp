# Migration

> Was beim Aktualisieren des Mods auf einem laufenden Server zu tun ist und welche Daten sich dabei ändern. Neue Abschnitte kommen oben dazu.

---

## Grundsätzliches

Ein Update heißt: Server stoppen, `.jar` im Ordner `mods/` ersetzen, Server starten. Dabei gilt:

| Was | Verhalten beim Update |
|---|---|
| **Spielstand** (Beacon, Server-Upgrades, Markt, Contracts, Spieler-Upgrades) | Bleibt erhalten. Jeder Datenblock hat ein Versionsfeld und wird beim Laden migriert ([Technischer Rahmen](70-technischer-rahmen.md#datenmigration)). Gespeichert wird nach ID: Ein entferntes Upgrade bleibt in der Datei, wirkt aber nicht mehr. |
| **Items und Blöcke** (Taler, Kisten, Inhalte) | Bleiben erhalten, solange die Item-ID weiter existiert. |
| **Config** `config/dtp-synced.toml` | Wird **nicht** überschrieben. Neue Schlüssel kommen mit Standardwert dazu. Bestehende Werte bleiben, **auch wenn sich der Standard im Mod geändert hat**. Übernehmen lassen sich geänderte Standards per Befehl (siehe unten), von Hand oder durch Löschen der Datei bei gestopptem Server. Liegt in der Welt eine `syncedconfig/dtp-synced.toml`, gilt diese statt der Datei in `config/`. Eine `config/dtp-server.toml` stammt aus einer älteren Version und wird nicht gelesen. |
| **Ausgegebene Taler** | Werden nie erstattet, auch nicht, wenn ein Upgrade billiger wird oder wegfällt. |
| **World-Border** | Wird beim Start **nicht** neu berechnet, nur beim Kauf einer Grenzstufe oder per Admin-Befehl. |

**Vor jedem Update:** Welt und `config/` sichern (Nitrado: Backup anlegen).

### Config-Standards per Befehl übernehmen

| Befehl | Wirkung |
|---|---|
| `/dtp admin config diff` | Listet alle Werte, die vom Standard abweichen, jeweils mit dem Standard. Ändert nichts. |
| `/dtp admin config reset <key>` | Setzt einen Wert auf den Standard, z. B. `beacon.radiusChunks`. |
| `/dtp admin config reset all` | Setzt **alle** Werte auf den Standard. Eigene Anpassungen gehen verloren. |

Der Befehl schreibt die Datei neu, lädt sie und schickt sie an verbundene Clients. Ein Neustart ist nicht nötig. Betrifft der Reset `beacon.radiusChunks` oder `beacon.borderEnabled`, wird die Grenze sofort neu gesetzt.

---

## Zeitbonus wieder +20 %, Hot Delivery heißt Quick Delivery (22. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config anpassen.** Der Standard von `contracts.bonusFactor` ist wieder `1.2` (+20 %), eine bestehende `dtp-synced.toml` behält `1.1`:

   ```
   /dtp admin config reset contracts.bonusFactor
   ```

   Die Schlüssel von Hot Delivery sind umbenannt: `contracts.hotDeliveryFactor` → `contracts.quickDeliveryFactor` (Standard jetzt `1.3`), `contracts.hotDeliveryDecayMultiplier` → `contracts.quickDeliveryDecayMultiplier` (`2.0`). `contracts.hotDeliveryDayPenalty` entfällt. Die neuen Schlüssel kommen mit Standard dazu, die alten werden nicht mehr gelesen; eigene Werte von Hand übertragen.

### Was mit den Daten passiert

- **Gekaufte Hot-Delivery-Stufen** ziehen beim Laden automatisch auf `quick_delivery` um (Spieler-Upgrades Datenversion 3).
- **Bonusfrist** ist mit Quick Delivery nicht mehr kürzer. Bonus und Frist werden beim Abschluss aus der Config gelesen, gelten also sofort auch für laufende Contracts.
- **Datapacks:** Eine eigene `data/<ns>/dtp/player_upgrade/hot_delivery.json` greift nicht mehr; in `quick_delivery.json` umbenennen. Verweise in `requires` ebenso.

---

## Contraband lädt an der Reparaturbank auf (21. November 2026)

Nichts zu tun. Contraband wird jetzt an der [Reparaturbank](16-reparaturbank.md) aufgeladen, mit Wechselgeld. Das Crafting-Rezept `data/dtp/recipe/contraband_recharge.json` und der Rezepttyp `dtp:contraband_recharge` entfallen. Keine Weltdaten betroffen; die Ladungen der Items bleiben, wie sie sind.

**Datapacks:** Ein eigenes Rezept mit `"type": "dtp:contraband_recharge"` lädt nicht mehr und erscheint als Fehler im Log. Solche Dateien entfernen.

---

## Einzelstücke, Monsterköpfe und Wither-Bonus (21. November 2026)

Nichts zu tun. Netherstern, Verzauberter Goldener Apfel, Drachenkopf, Antiker Schrott und alle Monsterköpfe haben neue Preise ([Preise und Größenordnungen](61-preise-und-groessenordnungen.md#sonderfälle)). Die Monsterköpfe stehen jetzt in der Gruppe Loot, die Gruppe `trophies` entfällt. Neuer Config-Schlüssel `events.witherMinutes` (Standard `30`) kommt von selbst dazu.

**Weltdaten:** Der bisherige Marktdruck der Köpfe bleibt erhalten und rechnet ab sofort mit dem neuen H. Ein schon angekündigter Nachfrageschub auf `trophies` bekommt beim nächsten Tick eine andere Gruppe, die Wartezeit bleibt. Die Marktdaten bekommen das Feld `event_wither_counted`, ältere Stände starten mit `false`.

---

## Stallmeister: Ruhe nach dem Absteigen (21. November 2026)

Nichts zu tun. Neuer Config-Schlüssel `beacon.mountRestSeconds` (Standard `60`, `0` schaltet die Ruhe ab) kommt von selbst dazu. Keine Weltdaten betroffen ([Stallmeister](21-beacon-effekte.md#stallmeister)).

---

## Mending-Aura und Ofentempo im Beacon-Gebiet (21. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config:** Drei Schlüssel entfallen, zwei kommen mit Standardwert dazu. NeoForge räumt die alten Einträge beim Start selbst aus der `dtp-synced.toml`. Nichts anzupassen, außer die Aura war bewusst verstellt:

   | Schlüssel | vorher | jetzt |
   |---|---|---|
   | `beacon.mendingAuraRadius` | `10` | entfällt |
   | `beacon.mendingAuraRadius2` | `30` | entfällt |
   | `beacon.mendingAuraPerSecond` | `1` | entfällt |
   | `beacon.mendingAuraRepair` | – | `1` (Haltbarkeit alle 10 s, Stufe I) |
   | `beacon.mendingAuraRepair2` | – | `2` (Haltbarkeit alle 10 s, Stufe II) |

4. **Eigene Datapacks prüfen**, falls sie `minecraft:cooking/speed_default` überschreiben: Ohne `"local": true` am Eintrag `dtp:server_upgrade` wirkt das Ofentempo weiter weltweit.

### Was mit den Daten passiert

**Bleibt erhalten:** gekaufte Stufen von Mending-Aura und Ofentempo, Neben-Beacons und ihre Auswahl.

**Geändert — Mending-Aura.** Statt 1 Haltbarkeit pro Sekunde in 10 / 30 Blöcken um den Hauptbeacon repariert sie jetzt 1 / 2 Haltbarkeit alle 10 Sekunden, dafür im ganzen Effektgebiet des Hauptbeacons und im Radius jedes Neben-Beacons, auch im Nether und End.

**Geändert — Ofentempo.** Wirkt nicht mehr weltweit, sondern nur im Effektgebiet des Hauptbeacons und um Neben-Beacons. Öfen an Außenposten außerhalb brauchen einen Neben-Beacon. Ein bereits brennender Brennstoff behält sein Tempo bis zum nächsten.

---

## Contract-Erträge angeglichen (21. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config anpassen.** Fünf Standards haben sich geändert, die eine bestehende `dtp-synced.toml` nicht übernimmt:

   | Schlüssel | alt | neu |
   |---|---|---|
   | `contracts.tierValue` | `[700.0, 4000.0, 13000.0, 30000.0]` | `[800.0, 2800.0, 7000.0, 30000.0]` |
   | `contracts.rewardMultiplier` | `[2.0, 3.0, 3.5, 3.5]` | `[2.0, 2.3, 2.75, 3.5]` |
   | `contracts.bonusDays` | `[3.0, 8.0, 15.0, 20.0]` | `[3.0, 6.0, 9.0, 12.0]` |
   | `contracts.bonusFactor` | `1.2` | `1.1` |
   | `contracts.hotDeliveryFactor` | `1.3` | `1.2` |

   Im Spiel als Admin:

   ```
   /dtp admin config diff
   /dtp admin config reset contracts.tierValue
   /dtp admin config reset contracts.rewardMultiplier
   /dtp admin config reset contracts.bonusDays
   /dtp admin config reset contracts.bonusFactor
   /dtp admin config reset contracts.hotDeliveryFactor
   ```

### Was mit den Daten passiert

- **Aushängende und angenommene Contracts** behalten Menge und Grundwert; beides wird beim Erzeugen fixiert. Neue Werte gelten ab der nächsten Rotation.
- **Bonusfrist und Zeitbonus** werden beim Abschluss aus der Config gelesen. Nach dem Reset gilt für laufende Contracts der Stufen 2 und 3 sofort die kürzere Frist und +10 % statt +20 %.

*Überholt am 22. November 2026: Zeitbonus wieder +20 % / +30 %, siehe oben.*

---

## Händlerware und gecraftete Stufe-3-Contracts (21. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten. Keine neue Config.

### Was mit den Daten passiert

- **Bestands-Dorfbewohner** brauchen nichts: Ihre gespeicherten Angebote werden beim nächsten Öffnen des Handelsfensters markiert.
- **Altbestände** im Inventar und in Kisten, die vor dem Update gekauft wurden, sind **nicht** markiert und bleiben verkäuflich und lieferbar. Rückwirkend lässt sich die Herkunft nicht feststellen.
- **Contract-Vorlagen:** Kaktus fällt aus dem Pool, Stufe 3 bekommt gecraftete Waren. Was schon aushängt oder angenommen ist, bleibt; die neuen Vorlagen erscheinen ab der nächsten Rotation.
- **Stufe 4:** Verzauberte Bücher und Ausrüstung vom Dorfbewohner zählen nicht mehr. Bereits angenommene Contracts sind davon genauso betroffen.

---

## Neuer Block Statistik-Tafel (21. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten. Neue Config-Schlüssel gibt es nicht.

### Was mit den Daten passiert

- Neu ist die Datei `data/dtp/statistics.dat` (`dtp:statistics`, Version 1). Sie entsteht beim ersten Verkauf oder Login.
- **Verkäufe** beginnen bei null, weil ältere Versionen keine Mengen gespeichert haben.
- **Contracts:** Die Gesamtzahl kommt aus dem bestehenden Weltfortschritt. Die Abschlüsse je Spieler und Stufe übernimmt die Tafel beim ersten Login des Spielers aus seinen Spielerdaten. Bis dahin stehen sie unter „Ohne Zuordnung“.
- Das Rezept steht nicht im Rezeptbuch, das Handbuch zeigt es.

---

## Neuer Block Recycler (21. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten. Keine neue Config.

### Was mit den Daten passiert

Nichts an bestehenden Daten. Neu sind das Item Schrott (`dtp:scrap`) als Ware der Handelsliste (`trade_list/70_scrap.json`) und die Recyclingliste `data/dtp/dtp/recycling/vanilla.json`. Jeder Recycler speichert wie ein Ofen seine drei Fächer, Restglut und Fortschritt im eigenen Block. Das Rezept steht nicht im Rezeptbuch, das Handbuch zeigt es.

---

## Flagge am Bulletin-Board (21. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten. Keine neue Config.

### Was mit den Daten passiert

Der Aushang speichert zusätzlich einen Rotationszähler (`rotation`, beginnt bei 0). Bulletin-Boards haben jetzt einen eigenen Blockspeicher mit der zuletzt gezeigten Rotation. Bereits gesetzte Boards erhalten ihn, sobald sie das erste Mal geöffnet werden; bis dahin bleibt ihre Flagge eingeholt. Abbauen und neu setzen geht ebenso.

---

## Neuer Block Spielautomat (21. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config:** Neu sind `slotMachine.chanceJackpot` (0.005), `slotMachine.chanceWin` (0.05) und `slotMachine.chanceConsolation` (0.2). Sie kommen von selbst mit Standardwert dazu.

### Was mit den Daten passiert

Nichts an bestehenden Daten. Jeder Spielautomat speichert seine laufende Drehung im eigenen Block (Einwurf-Tick, Einsatz, Ausgang, Symbole, Spieler); eine Drehung über einen Neustart hinweg wird beim Laden aufgelöst. Das Rezept steht nicht im Rezeptbuch, das Handbuch zeigt es.

---

## Neuer Block Reparaturbank (20. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config:** Neu sind `repair.enabled` (`true`), `repair.basePrice` (2.0) und `repair.exponent` (0.5). Sie kommen von selbst mit Standardwert dazu.

### Was mit den Daten passiert

Nichts. Die Reparaturbank speichert keine Daten. Was beim Schließen in den Slots liegt, geht an den Spieler zurück. Das Rezept steht nur bei Spielern, die die Welt neu betreten, von Anfang an im Rezeptbuch. Wer schon gespielt hat, findet es im Handbuch; im Crafting-Grid funktioniert es auch ohne Eintrag im Rezeptbuch.

---

## Neuer Block Chunkloader (20. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config:** Neu sind `chunkloader.enabled`, `chunkloader.price` (2500), `chunkloader.durationTicks` (72000), `chunkloader.maxCharges` (3), `chunkloader.radiusChunks` (1), `chunkloader.depositRadius` (16) und `chunkloader.naturalSpawning` (`true`). Sie kommen von selbst mit Standardwert dazu.
4. **`server.properties` nicht ändern.** Der Chunkloader wirkt bewusst nur, solange jemand online ist. `pause-when-empty-seconds` darf auf dem Standard bleiben.

### Was mit den Daten passiert

Neu ist der Datenblock `dtp:chunk_loaders` (Version 1) mit der Restlaufzeit je Chunkloader. Bestehende Daten ändern sich nicht. Die Chunk-Tickets des Controllers `dtp:chunk_loader` speichert Vanilla pro Dimension mit. Der Mod verwirft sie aber bei jedem Start und setzt sie aus `dtp:chunk_loaders` neu. Wer den Mod wieder entfernt, hinterlässt also keine dauerhaft geladenen Chunks.

---

## 4.3.0 → Beacon-Effekte zusammengelegt (15. November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config:** Kein Schlüssel ändert sich, nichts anzupassen. Die Werte `beacon.breedingCooldownTicks`, `beacon.horseInheritanceBonus`, `beacon.mountSpeedBonus` und `beacon.roadSpeedBonus` gelten jetzt für den Stallmeister, `beacon.rebuildSeconds` für den Baumeister. Neu ist `beacon.compostBonus` (Standard 10), es kommt von selbst dazu.
4. **Eigene Datapacks prüfen**, falls sie `dtp/upgrade/effect_horse_breeding.json`, `effect_roads.json` oder `effect_rebuild.json` überschreiben. Die Dateien heißen jetzt `effect_stablemaster.json` bzw. entfallen. Der Mod überschreibt jetzt außerdem `minecraft:context_int_provider/compostable/low`, `low_medium`, `medium` und `medium_high` (Kompost-Bonus der Landwirtschaft); ein Datapack, das dieselben Dateien überschreibt, hebt den Bonus auf.

### Was mit den Daten passiert

**Zusammengelegt — Beacon-Effekte.** Aus 13 Effekten werden 11:

| Vorher | Jetzt |
|---|---|
| Landwirtschaft (`effect_agriculture`): Zuchtpause, Trampelschutz, Pflanzenwachstum | Landwirtschaft: Trampelschutz, Pflanzenwachstum, **Kompost** |
| Pferdezucht (`effect_horse_breeding`) und Straßenbau (`effect_roads`) | **Stallmeister** (`effect_stablemaster`): Zuchtpause, Fohlen, Reittiere, Wege |
| Baumeister (`effect_builder`) und Rückbau (`effect_rebuild`) | **Baumeister** (`effect_builder`): Reichweite und Rückbau |

Der Stallmeister wirkt jetzt auf **alle Reittiere** (Tag `dtp:mounts`, unter anderem Schwein, Schreiter, Nautilus, Glücklicher Ghast) und die Zuchtpause auf **alle züchtbaren Tiere**, auch Frosch, Sniffer und Schildkröte ([Beacon-Effekte](21-beacon-effekte.md#stallmeister)).

Der Spielstand (`dtp:progress`, Version 4 → 5) wird beim ersten Laden umgeschrieben:

- `effect_horse_breeding` wird zu `effect_stablemaster`.
- Wer Pferdezucht **oder** Straßenbau besaß, besitzt den Stallmeister. Wer Baumeister **oder** Rückbau besaß, besitzt den Baumeister.
- Am Hauptbeacon und an den Neben-Beacons tritt der zusammengelegte Effekt an die Stelle der alten. Steckten beide Teile, bleibt er nur einmal stecken, und ein Slot wird frei.

**Gekürzt — Vein Mining.** Das persönliche Upgrade hat nur noch 2 Stufen (8 / 16 Blöcke, 6.000 / 20.000 Taler); Stufe 3 und 4 entfallen. Wer sie gekauft hatte, behält die gespeicherte Stufe, die Wirkung ist aber auf 16 Blöcke gedeckelt. Ein Datapack, das `dtp/player_upgrade/vein_mining.json` überschreibt, gilt weiter wie bisher.

**Gekürzt — Schutz und Haltbarkeit.** Schutz (`protection_bonus`) hat nur noch 1 Stufe (+1 Protection, 20.000 Taler), Haltbarkeit (`unbreaking_bonus`) nur noch 2 (+1 / +2 Unbreaking, 10.000 / 40.000 Taler). Wer Schutz 2 oder Haltbarkeit 3 gekauft hatte, behält die gespeicherte Stufe, die Wirkung ist auf +1 bzw. +2 gedeckelt. Begründung und Zahlen: [Stärke der Ausrüstungs-Upgrades](30-persoenliche-upgrades.md#stärke-der-ausrüstungs-upgrades). Datapacks, die `dtp/player_upgrade/protection_bonus.json` oder `unbreaking_bonus.json` überschreiben, gelten weiter wie bisher.

**Keine Erstattung.** Wer beide Teile, Vein Mining 3/4, Schutz 2 oder Haltbarkeit 3 gekauft hatte, bekommt keine Taler zurück (siehe [Grundsätzliches](#grundsätzliches)). Die Preise bleiben vorerst: Stallmeister 12.000, Baumeister 15.000, Landwirtschaft 30.000.

**Admin-Befehle:** `/dtp admin upgrade effect_horse_breeding …` heißt jetzt `/dtp admin upgrade effect_stablemaster …`. `effect_roads` und `effect_rebuild` gibt es nicht mehr.

**Neu — Fortune auf Weizen und Rote Bete.** Fortune erhöht jetzt auch das Erntegut, nicht nur die Samen ([Persönliche Upgrades](30-persoenliche-upgrades.md)). Eigene Datapacks, die `minecraft:blocks/wheat` oder `minecraft:blocks/beetroots` überschreiben, behalten den Bonus, weil er als Global Loot Modifier angehängt wird. Abschalten lässt er sich per Datapack mit Dateien `dtp/loot_table/blocks/wheat_fortune.json` und `beetroots_fortune.json`, die nur `{"type": "minecraft:block", "pools": []}` enthalten.

**Zurück auf eine ältere `.jar`?** Geht nicht, die ältere Version lehnt den neueren Stand ab. Vor dem Update ein Backup anlegen.

---

## 3.1.0 → 4.0.0

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config:** Kein bestehender Standard hat sich geändert, es kommen nur neue Schlüssel mit Standardwert dazu. Nichts anzupassen.
4. **Eigene Datapacks prüfen**, falls sie die Schleimrute (`dowsing_rod`) oder die Warengruppen `stone`/`wood` überschreiben (siehe unten).

Neue Config-Schlüssel:

| Schlüssel | Standard | Wofür |
|---|---|---|
| `beacon.cropGrowthBonus` | `30` | Landwirtschaft: Chance in Prozent auf eine Zusatz-Wachstumsstufe (siehe [Landwirtschaft](#v3-stand--landwirtschaft-10-november-2026)) |
| `beacon.mendingAuraRadius2` | `30` | Reichweite der Mending-Aura Stufe II in Blöcken |
| `blackmarket.scamChance` | `0.2` | Wahrscheinlichkeit, dass ein Handel am Schwarzmarkt ein Scam ist |
| `blackmarket.weightSteal` / `weightNothing` / `weightCurse` / `weightToll` | `1` / `2` / `2` / `2` | Gewichte der Scam-Arten |
| `curse.tollPrice` | `1` | Maut-Fluch: Taler je Abbau (Mehrfachabbau zählt einmal), `0` schaltet die Maut ab |
| `curse.tollSlowdown` | `0.1` | Maut-Fluch: Faktor auf die Abbaugeschwindigkeit, wenn das Geld nicht reicht |
| `blackmarket.traderEnabled` | `true` | Schwarzmarkthändler an/aus |
| `blackmarket.checkIntervalSeconds` | `60` | Abstand der Spawnprüfungen |
| `blackmarket.attractRadius` | `24` | Umkreis um den Spieler, in dem Einzahlkisten zählen |
| `blackmarket.minChestBalance` | `60000` | Mindestbestand einer einzelnen Einzahlkiste, ab dem der Händler erscheinen kann |
| `blackmarket.baseChance` / `chanceExponent` / `maxChance` | `0.03` / `0.5` / `0.25` | Chance je Prüfung |
| `blackmarket.despawnMinutes` | `20` | Verweildauer des Händlers |
| `blackmarket.cooldownMinutes` | `120` | Pause bis zum nächsten Händler |
| `contraband.enabled` | `true` | Contraband-Items wirken; aus = sie bleiben, tun aber nichts |

Details zu Schwarzmarkt und Händler: [Blackmarket](14-blackmarket.md), [Blackmarket-Händler](15-blackmarket-trader.md), [Contraband](13-contraband.md).

### Was mit den Daten passiert

**Bleibt erhalten:** Taler und Kisten, Beacon-Position, alle Stufen der Server-Upgrades, Markt, Aushang, laufende Contracts, alle Spieler-Upgrades.

**Umbenannt — Beacon-Effekt Landwirtschaft.** Wer noch auf 3.1.0 läuft, bekommt mit diesem Update auch die Umstellung von `effect_breeding` auf `effect_agriculture` (`dtp:progress`, Version 3 → 4), siehe [Landwirtschaft](#v3-stand--landwirtschaft-10-november-2026).

**Neu — Schwarzmarkt-Daten.** Der Datenblock `dtp:blackmarket` (`BlackmarketData`, Version 1) entsteht beim ersten Start leer: Tageslimit und Händler-Abklingzeit beginnen bei null.

**Umbenannt — Schleimrute.** Das Contraband-Item heißt `dtp:oozing_rod` statt `dtp:dowsing_rod`. Ein Alias in `ModItems` erhält vorhandene Ruten in Kisten und Inventaren, sie werden beim Laden zur neuen ID. Mit umbenannt sind die Daten-Dateien:

- `data/dtp/dtp/contraband/dowsing_rod.json` → `oozing_rod.json`
- `data/dtp/villager_trade/blackmarket/contraband_dowsing_rod.json` → `contraband_oozing_rod.json`

Ein eigenes Datapack, das die alten Pfade überschreibt, wirkt nicht mehr und muss umbenannt werden. Befehle wie `/give` brauchen die neue ID.

**Erweitert — Mending-Aura.** Das Server-Upgrade hat jetzt zwei Stufen. Eine gekaufte Stufe bleibt Stufe I (Radius `beacon.mendingAuraRadius`, 10 Blöcke). Stufe II kostet 250.000 und wirkt im Radius `beacon.mendingAuraRadius2` (30 Blöcke).

**Neu — Double Bobbers.** Spieler-Upgrade im Tab Ausrüstung, eine Stufe, 150.000 Taler.

**Zusammengelegt — Warengruppen.** Die Filtergruppen *Gestein & Erde* (`stone`) und *Holz* (`wood`) des Marktstands sind zu *Baumaterial* (`construction`) zusammengefasst. War eine der alten Gruppen schon als Ziel des nächsten Marktereignisses ausgelost, wird das Ziel neu gezogen; der Countdown bleibt.

**Handelsliste.** Die Sporenblüte ist nicht mehr verkäuflich. Neue Basiswerte für Weizen, Rote Bete, Karotte, Kartoffel, Zuckerrohr, Kaktus, Melonenscheibe (dazu H 3.000 und T 1), Apfel, Giftige Kartoffel, Lohenrute, Shulkerschale und Schnüffler-Ei. Der gespeicherte Marktdruck bleibt, der Preis folgt sofort dem neuen Basiswert.

**Contracts.** Stufe 4 würfelt Verzauberungen aus einer Wertetabelle (`enchantments` in `contract/vanilla.json`). Der laufende Aushang bleibt bis zur nächsten Rotation; sofort neu würfeln geht mit `/dtp admin contracts reroll`.

**Zurück auf 3.1.0?** Geht nicht, die ältere Version lehnt den neueren Spielstand ab (`dtp:progress` Version 4). Vor dem Update ein Backup anlegen.

---

## v3-Stand → Landwirtschaft (10. November 2026)

### Was zu tun ist

Nichts. `.jar` ersetzen und den Server starten. Die neue Config-Option `beacon.cropGrowthBonus` (Standard 30) kommt von selbst dazu.

### Was mit den Daten passiert

**Umbenannt — Beacon-Effekt.** *Kurze Zuchtpause* heißt jetzt **Landwirtschaft** und bekommt Trampelschutz und Pflanzenwachstum dazu. Die ID ändert sich von `effect_breeding` zu `effect_agriculture`.

Der Spielstand (`dtp:progress`, Version 3 → 4) wird beim ersten Laden umgeschrieben:

- gekaufte Stufe
- gewählter Effekt am Hauptbeacon
- Auswahl an den Neben-Beacons

Wer den Effekt gekauft hat, behält ihn. Er ist danach weiter aktiv.

**Teurer.** Der Preis steigt von 25.000 auf 30.000 Taler. Das gilt nur für künftige Käufe, es wird nichts nachgefordert.

**Admin-Befehle:** `/dtp admin upgrade effect_breeding …` heißt jetzt `/dtp admin upgrade effect_agriculture …`.

**Zurück auf eine ältere `.jar`?** Geht nicht, die ältere Version lehnt den neueren Stand ab. Vor dem Update ein Backup anlegen.

---

## 2.0.0 (Stand 4. November 2026, Commit `02954ae`) → v3-Stand (November 2026)

### Was zu tun ist

1. **Backup** von Welt und `config/`.
2. **`.jar` ersetzen** und den Server starten.
3. **Config anpassen.** Drei Standards haben sich geändert, die eine bestehende `dtp-synced.toml` nicht übernimmt:

   | Schlüssel | alt | neu |
   |---|---|---|
   | `beacon.radiusChunks` | `[10, 14, 19, 26, 35, 46, 61, 80, 106, 140, 190]` | `[12, 16, 20, 25, 31, 39, 49, 62, 80, 106, 140]` |
   | `contracts.guaranteedPerTier` | `2` | `1` |
   | `contracts.rewardMultiplier` | `[2.0, 2.0, 2.0, 2.0]` | `[2.0, 3.0, 3.5, 3.5]` |

   Im Spiel als Admin:

   ```
   /dtp admin config diff
   /dtp admin config reset beacon.radiusChunks
   /dtp admin config reset contracts.guaranteedPerTier
   /dtp admin config reset contracts.rewardMultiplier
   ```

   `diff` zeigt vorher, was sonst noch abweicht. Wurde nichts bewusst angepasst, reicht `/dtp admin config reset all`. Neue Schlüssel (`contracts.tierValue`, `beacon.mendingAuraRadius` usw.) kommen von selbst dazu.

   Der Reset von `beacon.radiusChunks` setzt die Grenze sofort nach der neuen Liste, die Stufe bleibt gleich. **Vorher prüfen, dass niemand außerhalb der neuen Grenze steht** (siehe unten). Wer die Datei stattdessen von Hand ändert, setzt die Grenze danach mit `/dtp admin upgrade border <aktuelle Stufe>` neu.

### Was mit den Daten passiert

**Bleibt erhalten:** Taler und Kisten, Beacon-Position, alle Stufen der Server-Upgrades, Markt, Aushang, laufende Contracts, alle Spieler-Upgrades. Die Spieler-Upgrades bekommen nur das neue Feld für ausgerüstete Module (Version 1 → 2). Bestehende Upgrades sind keine Module und wirken weiter wie bisher.

**Entfallen — Beacon-Effekte.** Diese fünf Effekte gibt es nicht mehr:

- Tempo (`effect_speed`)
- Eile (`effect_haste`)
- Regeneration (`effect_regeneration`)
- Feuerresistenz (`effect_fire_resistance`)
- Sättigung (`effect_saturation`)

Wer sie gekauft hat, verliert die Wirkung. Ihr Effektslot wird frei, am Haupt- wie an Neben-Beacons. Die Kosten werden nicht erstattet. Soll die Gruppe entschädigt werden, geht das nur von Hand: Taler per `/give` oder ein anderes Upgrade per `/dtp admin upgrade <id> <stufe>`.

**Kleiner — Grenze.** Gleiche Stufe, neuer Radius in Chunks:

| Stufe | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| alt | 10 | 14 | 19 | 26 | 35 | 46 | 61 | 80 | 106 | 140 | 190 |
| neu | 12 | 16 | 20 | 25 | 31 | 39 | 49 | 62 | 80 | 106 | 140 |

- **Bis Stufe 2** wird das Gebiet größer.
- **Ab Stufe 3** wird es kleiner. Gebautes jenseits der neuen Grenze bleibt erhalten, ist aber erst wieder erreichbar, wenn die Grenze weiter wächst.
- Wer beim Neusetzen draußen steht, nimmt Schaden durch die Vanilla-World-Border. Liegt der Welt-Spawn draußen, setzt der Mod ihn beim nächsten Start auf den Beacon. Spieler-Betten jenseits der Grenze prüft er nicht.
- Der **Effektbereich des Hauptbeacons** folgt der höchsten Stufe und schrumpft von ±3040 auf ±2240 Blöcke.
- Die Grenzpreise sind deutlich gesunken (Stufe 1: 13.000 → 2.000). Eine Erstattung gibt es nicht.

**Gedeckelt, aber wirksam — Preisstabilität und Preiserholung.** Die Höchststufe sinkt von 5 auf 2. Bereits gekaufte Stufen 3–5 wirken weiter, nur nachkaufen geht nicht mehr.

**Nur neue Kosten.** Viele Upgrades haben neue Preise oder zusätzliche Stufen (z. B. Contract-Plätze 4, Baumfäller 4) und teils Voraussetzungen. Gekaufte Stufen bleiben, auch wenn eine neue Voraussetzung fehlt.

> Diese Liste vergleicht mit Commit `02954ae`. Lief auf dem Server ein Zwischenstand mit dem Spieler-Upgrade *Meisterschmied* (vor dem 7. November 2026 in der Werkstatt), ist dessen Stufe wirkungslos. Es ist jetzt ein Server-Upgrade am Beacon (`anvil_cap`) und muss neu gekauft oder per Befehl gesetzt werden.
