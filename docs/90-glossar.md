# Glossar

> Verbindliche Begriffe. Jedes Dokument in `docs/` benutzt diese Bezeichnungen. Wer einen neuen Begriff einführt, trägt ihn hier ein.

---

## Markt

**Taler**
Die Währung des Mods, ein **echtes Item** in neun Stufen (Kupfer, Silber, Gold in je drei Formen, jeweils Faktor 9). Verdient durch Verkauf am Marktstand und durch Contracts, nebenbei auch in Strukturkisten. Siehe [Markt](10-markt.md#der-taler).

**Marktstand**
Die Handelsstation: ein Block, **kein NPC**. Preisübersicht, Verkauf aus der Hand und aus den Trade-Chests in Reichweite, Redstone-Verkauf eines Stapels.

**Trade-Chest**
Das Warenlager des Marktstands und des Bulletin-Boards. Verkauft nicht selbst; Ware hinein von Hand oder per Hopper.

**Einzahlkiste**
Ein Block, der **Taler bereithält**. Alle Upgrades werden aus Einzahlkisten in Reichweite bezahlt, Erlöse und Contract-Belohnungen landen dort. Mehrere Kisten in Reichweite bilden einen gemeinsamen Vorrat.

**Marktwaage**
Ein flacher Block, der den Marktwert einer gewählten Ware als Comparator-Signal (0–15) ausgibt. Craftbar nach dem Server-Upgrade *Marktinformation*. Siehe [Markt](10-markt.md#marktwaage).

**Handelsliste**
Die kuratierte Liste der verkaufbaren Items (`data/<ns>/dtp/trade_list/*.json`). Items ohne Eintrag sind nicht verkaufbar.

**Ware**
Was einen Marktdruck trägt: ein Item oder eine **gemeinsame Ware** aus mehreren Items mit demselben `key` (etwa alle Töpferscherben).

**Filtergruppe**
Eine der zehn Gruppen der Preisübersicht (Landwirtschaft, Tierhaltung, Fischerei, Baumaterial, Erze & Minerale, Monsterbeute, Nether, Das Ende, Loot, Monsterköpfe). Zugleich die Ziele der Marktereignisse.

**Basiswert**
Der feste Referenzpreis eines Items aus der Handelsliste (`base`).

**Marktwert**
Der aktuelle Verkaufspreis: Basiswert × Preisfaktor. Serverweit geteilt.

**Marktdruck**
Der Zustand einer Ware im Markt (`s`). Steigt mit jedem verkauften Stück, klingt mit der Zeit gegen `sFloor` ab. Siehe [Marktdruck](11-marktdruck.md).

**Halbwertsmenge** (`H`)
Die Menge, nach deren Verkauf sich der Preis halbiert.

**Halbwertszeit** (`T`)
Die Zeit in Markttagen, in der sich der Abstand des Marktdrucks zu `sFloor` halbiert.

**Ruhepreis**
Der Preis einer lange nicht gehandelten Ware: rund 1,41 × Basiswert. Der übliche Höchstwert — darüber kommt eine Ware nur durch ein Marktereignis.

**Preisband**
Unter- und Obergrenze des Preisfaktors: 0,05 bis 2,5 des Basiswerts.

**Markttag**
Die Zeiteinheit von Markt und Contracts: ein Minecraft-Tag, 20 Minuten.

**Marktuhr**
Die serverweite Uhr in Markttagen. Läuft **nur, während mindestens ein Spieler online ist**; Schlafen zählt mit.

**Aktive Spieler**
Alle Spieler, die in den letzten `market.activeWindowDays` (30) Markttagen online waren. Bestimmen die Spielerskalierung von `H`.

**Marktereignis**
Ein angekündigter **Nachfrageschub** für eine Filtergruppe oder alle Waren: Gesättigte Preise erholen sich, alle Preise der Gruppe steigen vorübergehend über den Ruhepreis.

---

## Contracts

**Contract** (Lieferauftrag)
Ein Auftrag über eine bestimmte Itemmenge gegen eine Belohnung über dem Marktwert, mit Zeitbonus statt hartem Zeitlimit. Siehe [Contracts](12-contracts.md).

**Contract-Stufe**
Die Größenklasse eines Contracts, Stufe 1 bis 4. Höhere Stufen fordern und zahlen mehr. Freigeschaltet am Beacon; jede Stufe braucht serverweit abgeschlossene Contracts.

**Händlerware**
Ein Item von einem Vanilla-Händler (Dorfbewohner, fahrender Händler, Geschenk an den Helden des Dorfes), erkennbar an der grauen Tooltip-Zeile „Händlerware“. Zählt für keinen Contract, ist unverkäuflich und geht nicht in den Recycler. Smaragde und Schwarzmarkt-Ware sind nie Händlerware. Siehe [Markt](10-markt.md#händlerware).

**Bulletin-Board**
Der Block für Contracts: Aushang lesen, annehmen, liefern, abbrechen. Geliefert wird aus den Trade-Chests in Reichweite.

**Statistik-Tafel**
Der Block, der die Verkäufe des Servers je Ware (14 Markttage oder gesamt) und die abgeschlossenen Contracts je Spieler und Stufe zeigt. Nur Anzeige.

**Aushang**
Die offenen Contract-Angebote am Bulletin-Board. Serverweit gleich, rotieren nach einigen Markttagen. Weitere Plätze bringt das Server-Upgrade *Aushang-Platz*.

---

## Schwarzmarkt und Contraband

**Schwarzmarkt**
Ein Block, der eine Verzauberung eines Gegenstands gegen einen Silberbeutel um eine Stufe steigert, höchstens bis zur Vanilla-Höchststufe. Einmal pro Markttag, mit Scam-Risiko. Siehe [Schwarzmarkt](14-blackmarket.md).

**Scam-Fluch**
Ein Fluch, den nur der Schwarzmarkt vergibt. Ein verfluchter Gegenstand zählt für keinen Contract, wird am Schwarzmarkt nicht mehr angenommen und lässt sich im Amboss nicht kombinieren.

**Maut-Fluch** („Fluch der Maut")
Ein Werkzeug-Fluch vom Schwarzmarkt. Jeder Abbau kostet Kupfertaler aus dem Inventar, ein Mehrfachabbau zählt einmal; ohne Geld baut das Werkzeug sehr langsam ab. Zum Ausgleich zählt Unbreaking eine Stufe höher. Sperrt Contracts, Schwarzmarkt und Amboss wie der Scam-Fluch.

**Schwarzmarkthändler**
Ein flüchtiger Händler, den Taler in Einzahlkisten anlocken. Die einzige Entity des Mods; er hält keinen Zustand, der mit ihm verloren gehen könnte. Siehe [Schwarzmarkthändler](15-blackmarket-trader.md).

**Contraband**
Ein Werkzeug vom Schwarzmarkthändler, etwa der Feuerlöscher. Arbeitet mit Ladungen statt Haltbarkeit, geht nie kaputt, ist nicht verzauberbar. Siehe [Contraband](13-contraband.md).

**Ladung**
Eine Benutzung eines Contraband-Items. Aufgefüllt nur mit Talern an der Reparaturbank, nie durch Mending oder die Mending-Aura. Überzahltes kommt als Wechselgeld zurück.

**Schmuggelkiste**
Ein Item vom Schwarzmarkthändler mit verdecktem Inhalt, etwa ein Spawn-Ei, ein Setzling oder ein Tier im Eimer. Rechtsklick öffnet sie.

---

## Territorium und Beacon

**Beacon** (Hauptbeacon)
Der zentrale Block des Mods, einer pro Server. Sein Setzen startet den Durchlauf. Er trägt die Grenze, die Server-Upgrades und die Effekte. „Beacon" ohne Zusatz meint immer diesen Block; der Vanilla-Block heißt „Vanilla-Beacon". Siehe [Territorium](20-territorium-und-serverweite-effekte.md).

**Handbuch**
Das DTP-Handbuch (`dtp:guide`), eine Einführung für neue Spieler. Liegt beim ersten Betreten im Inventar; Ersatz aus Buch und einem Kupfer-, Eisen- oder Goldnugget. Siehe [Einstieg](01-mod-und-vision.md#einstieg).

**Neben-Beacon**
Ein craftbarer Zusatzbeacon für Außenposten, in jeder Dimension. Trägt eine eigene Auswahl der freigeschalteten Effekte im Radius von 5 Chunks, mit einem Effektslot weniger als der Hauptbeacon (mindestens einem). In seinem Radius lassen sich Module umstecken.

**Chunkloader**
Ein Glaskasten mit schwebender Enderperle (`dtp:chunk_loader`). Gegen Taler aus einer Einzahlkiste hält er 3 × 3 Chunks geladen, in jeder Dimension, aber nur, solange jemand online ist. Siehe [Chunkloader](20-territorium-und-serverweite-effekte.md#chunkloader).

**Territorium**
Das Gebiet innerhalb der Grenze, gemeinsam für alle Spieler. Beacon-Radius = Territorium = World-Border: ein Bereich, drei Namen. Voller Markt gilt nur hier und nur in der Oberwelt.

**Grenze**
Die Vanilla-World-Border um den Hauptbeacon. Wächst in Grenzstufen; Grenzstufe 11 entfernt sie vollständig.

**Effektslot**
Ein Platz am Beacon für genau einen aktiven Beacon-Effekt. Start 0, bis zu 3 über das Server-Upgrade *Effektslot*. Siehe [Beacon-Effekte](21-beacon-effekte.md).

**Beacon-Effekt**
Eine Wirkung, die der Beacon in seinem Wirkungsbereich verteilt, wenn sie freigeschaltet ist und in einem Effektslot steckt — etwa Nachtsicht, Landwirtschaft oder Baumeister.

---

## Upgrades

**Upgrade**
Ein mit Talern gekaufter, **permanenter** Fortschritt. Einstufig oder in Stufen, jede Stufe teurer als die vorige. Keine Rückerstattung.

**Server-Upgrade**
Ein Upgrade, das für alle Spieler zugleich gilt und gemeinsam finanziert wird. Gekauft am Beacon: Grenze, Zugang, Beacon-Effekte, Weltregeln, Markt und Contracts.

**Spieler-Upgrade** (persönliches Upgrade)
Ein Upgrade, das nur den Käufer betrifft: Charakterwerte, Ausrüstung, Handel, Hof, Module. Gekauft an der Werkstatt. Siehe [Persönliche Upgrades](30-persoenliche-upgrades.md).

**Werkstatt**
Der Block für persönliche Upgrades und das Ausrüsten von Modulen (`dtp:upgrade_table`). Eigener Block, damit der Marktstand reiner Handel bleibt.

**Reparaturbank**
Werkbank, die Werkzeuge und Rüstung gegen Taler im Slot repariert, ohne Erfahrung, und Contraband auflädt (`dtp:repair_station`). Der Preis je Haltbarkeitspunkt steigt mit der Haltbarkeit des Gegenstands; zu viel Gezahltes kommt als Wechselgeld zurück. Siehe [Reparaturbank](16-reparaturbank.md).

**Recycler**
Zerlegt ausgediente Ausrüstung zu Schrott (`dtp:recycler`), wie ein Ofen mit Brennstoff und Hopper-Seiten. Nimmt nichts, was auf der Handelsliste steht oder im Ofen schmilzt. Siehe [Recycler](19-recycler.md).

**Schrott**
Ware aus dem Recycler (`dtp:scrap`), verkauft am Marktstand. Höchstens so viel wert wie die Rohstoffe am Boden des Markts.

**Spielautomat**
Glücksspiel ohne Oberfläche (`dtp:slot_machine`): Rechtsklick mit einem Taler, drei Walzen drehen sich für alle sichtbar, der Gewinn fällt vorn heraus. Die Auszahlungsquote liegt unter 100 %, auf Dauer ist der Automat eine Talersenke. Siehe [Spielautomat](17-spielautomat.md).

**Stufe**
Der aktuelle Ausbaugrad eines Upgrades. Stufe 0 bedeutet „nicht gekauft".

**Modul**
Ein persönliches Upgrade, das nur wirkt, solange es in einem Modulslot steckt. Siehe [Module und Fähigkeiten](31-module-und-faehigkeiten.md).

**Modulslot**
Ein persönlicher Platz für ein Modul, 1 bis 3 über das Spieler-Upgrade *Modulslots*. Umstecken ist kostenlos, geht aber nur an der Werkstatt im Territorium oder im Radius eines Neben-Beacons.

**Fähigkeit** (Skill)
Ein Modul mit aktiver Wirkung, ausgelöst mit der Fähigkeitstaste (Standard `R`): Dash, Doppelsprung, Schutzschild, Heilimpuls. Höchstens eine Fähigkeit zugleich ausgerüstet.

---

## Balancing

**Rückkopplung**
Ein Upgrade, das die Einnahmen erhöht, finanziert dadurch die nächsten Upgrades schneller. Der zentrale Balancing-Begriff. Siehe [Balancing](62-balancing.md).

**Produktions-Upgrade**
Ein Upgrade mit Rückkopplung. Strenger bepreist als alles andere.

**Komfort-Upgrade**
Ein Upgrade ohne Rückkopplung. Darf großzügiger bepreist sein.

---

## Vanilla-Begriffe

Unverändert übernommen: Chunk, Hopper, Comparator, Crafter, Elytra, Mending, Fortune, Looting, Datapack, Tag, World-Border, Raid.
