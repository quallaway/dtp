# Technischer Rahmen

> Die technischen Weichenstellungen, die das Design begrenzen: Plattform, Blöcke, Datenhaltung, Konfiguration, Abschaltbarkeit.

---

## Plattform

| | |
|---|---|
| Modloader | **NeoForge** 26.3.0.99 |
| Minecraft-Version | **26.3** |
| Sprache | Java 25 |
| Build | Gradle mit ModDevGradle |
| CI und Releases | GitHub Actions: Build und GameTests bei jedem Push, Release-Entwurf je Versions-Tag |

**Minecraft 26.3** ist gesetzt; beide Mods, mit denen gespielt wird, liegen dafür vor. Nitrado bietet NeoForge nur bis 26.2 an. Der Gruppenserver startet 26.3 trotzdem: Auf ein NeoForge-26.2-Paket von Nitrado werden per FTP die lokal installierten 26.3-Serverdateien (`libraries`, `run.sh`, `user_jvm_args.txt`) und der [ServerStarterJar](https://github.com/neoforged/ServerStarterJar) als `minecraft_server.jar` hochgeladen.

| Mod | Rolle |
|---|---|
| **Sophisticated Backpacks** | Wird im Pack gespielt — Inventarerweiterung ist dadurch keine Aufgabe von DTP. Optional: Rucksack-Upgrades machen einen platzierten Rucksack zur Trade-Chest oder Einzahlkiste (siehe [Kompatibilität](#kompatibilität)) |
| **JEI** | Optional. Neben Einzahlkiste und Schwarzmarkt zeigt JEI seine Itemliste in einem eigenen Panel (`JeiCompat`) |
| **Better Advancements** | Optional, ohne jede Anbindung. Zeigt DTPs Advancement-Baum größer und navigierbar an |

Die Version wird bewusst **nicht laufend nachgezogen**. Ein Projekt, das jeder Minecraft-Version hinterherportiert, kommt nie zum Spielen.

---

## Taler als Item

Die Währung ist ein Item ([Markt](10-markt.md)). Das hat technische Folgen:

| | |
|---|---|
| Registrierung | **Neun Items** (Kupfer, Silber, Gold × Taler, Beutel, Sack), durchgehend Faktor 9 |
| Rezepte | Komprimieren und Zerlegen zwischen benachbarten Stufen |
| Guthabenermittlung | Summe über alle **Einzahlkisten in Reichweite** — **kein** Kontofeld, **kein** Inventarzugriff |
| Bezahlvorgang | Taler entnehmen, bei Bedarf automatisch zerlegen, Wechselgeld zurücklegen |
| Auszahlung | Greedy über die Stufen: größte passende zuerst, Rest absteigend |

Wer 1.400 Taler bezahlt und nur Silbertaler plus Kleingeld hat, muss nicht selbst rechnen. Automatisches Zerlegen und Wechselgeld sind Pflicht, keine Komfortfunktion. Die Auszahlung gibt eine Liste von Stapeln zurück, beliebig viele und gemischt: 500 Taler werden zu 6 Kupfersäcken, 1 Kupferbeutel und 5 Kupfertalern.

Regeln beim Bezahlen:

- **Teildeckung** wird abgelehnt. Es wird nichts entnommen, die Oberfläche nennt den Fehlbetrag.
- **Entnahme** je Kiste von der kleinsten Stufe aufwärts — große Münzen werden nur bei Bedarf angebrochen.
- **Wechselgeld** geht in die erste Kiste, aus der entnommen wurde.
- Der Server prüft beim Kauf **erneut** und vertraut nicht auf die angezeigte Summe. Zwei Spieler an zwei Blöcken können sich dieselbe Kiste teilen.

---

## Die dreizehn Blöcke

| Block | ID | Inventar | Oberfläche | Aufgabe |
|---|---|---|---|---|
| **Marktstand** | `market_stand` | – | Ja | Preise, Verkauf aus der Hand und aus Trade-Chests, Redstone-Verkauf je Signal |
| **Trade-Chest** | `trade_chest` | Ja | Vanilla-Kiste | Warenlager, gelesen von Marktstand und Bulletin-Board; Comparator: Füllstand |
| **Marktwaage** | `market_scale` | – | – | Comparator-Signal aus dem Preisfaktor eines Filter-Items |
| **Einzahlkiste** | `deposit_chest` | Ja | Ja | Taler für Käufe bereithalten, Erlöse empfangen; Comparator: Füllstand |
| **Bulletin-Board** | `bulletin_board` | – | Ja | Contracts annehmen, aus Trade-Chests liefern, abbrechen |
| **Statistik-Tafel** | `statistics_board` | – | Ja | Verkäufe je Ware und Contracts je Spieler anzeigen, nur lesend ([Statistik-Tafel](18-statistik.md)) |
| **Werkstatt** | `upgrade_table` | – | Ja | Persönliche Upgrades kaufen, Module ausrüsten |
| **Reparaturbank** | `repair_station` | – | Ja | Haltbarkeit und Contraband-Ladungen gegen Taler im Slot, mit Wechselgeld ([Reparaturbank](16-reparaturbank.md)) |
| **DTP-Beacon** | `beacon` | – | Ja | Hauptbeacon: Server-Upgrades, Effekte, Grenze |
| **Neben-Beacon** | `relay_beacon` | – | Ja | Eigene Auswahl freigeschalteter Effekte im kleinen Radius, Module ausrüsten; kein Kauf |
| **Schwarzmarkt** | `blackmarket` | – | Ja | Verzauberungen gegen Taler, mit Scam-Risiko ([Blackmarket](14-blackmarket.md)) |
| **Chunkloader** | `chunk_loader` | – | Ja | Hält gegen Taler 3 × 3 Chunks geladen, solange jemand online ist ([Chunkloader](20-territorium-und-serverweite-effekte.md#chunkloader)) |
| **Spielautomat** | `slot_machine` | – | – | Glücksspiel um eine Münze aus der Hand, Walzen für alle sichtbar ([Spielautomat](17-spielautomat.md)) |
| **Recycler** | `recycler` | Ja | Ja | Zerlegt Ausrüstung zu Schrott wie ein Ofen, mit Brennstoff; Hopper-Seiten wie beim Ofen ([Recycler](19-recycler.md)) |

Nur Trade-Chest und Einzahlkiste führen ein Inventar. Die Marktwaage hält nur eine Kopie ihres Filter-Items. Schwarzmarkt und Reparaturbank arbeiten wie ein Verzauberungstisch: Was in den Slots liegt, geht beim Schließen zurück an den Spieler. Der Spielautomat nimmt die Münze direkt aus der Hand und wirft den Gewinn vorn aus. Der Recycler führt wie ein Ofen drei Fächer: Eingabe, Brennstoff und Schrott. Alle anderen Blöcke greifen auf die Kisten in Reichweite zu.

### Einzahlkisten in Reichweite finden

Marktstand, Bulletin-Board, Werkstatt und Beacon bestimmen ihr Guthaben, indem sie **Einzahlkisten im Umkreis** suchen:

1. Die Block-Entities der geladenen Chunks im Radius durchgehen und Einzahlkisten herausfiltern (Standard **2 Blöcke**, `deposit.radius`, bis 32; der Beacon nutzt **16 Blöcke**, `beacon.depositRadius`)
2. Gefundene Einzahlkisten nach Abstand sortieren, bei Gleichstand nach Koordinaten
3. Bestände summieren — das ist das angezeigte Guthaben
4. Bei einem Kauf in dieser Reihenfolge entnehmen, bis der Betrag gedeckt ist

Dadurch gibt es **keinen Fehlerfall bei mehreren Kisten**: Sie bilden einen gemeinsamen Vorrat und verhalten sich vorhersagbar. Eine Kiste darf in den Radien mehrerer Funktionsblöcke liegen — das ist der Normalfall bei kompakt gebauten Anlagen.

- Die Suche läuft **nur beim Öffnen einer Oberfläche und beim Kauf**, bei offener Oberfläche einmal pro Sekunde. Geprüft werden nur die Block-Entities der betroffenen Chunks; selbst die 16 Blöcke des Beacons berühren höchstens 3 × 3 Chunks.
- Kisten in nicht geladenen Chunks werden ignoriert.
- Die Prüfung ist eine **reine Abstandsprüfung**, keine Sichtlinie.
- Die Oberfläche zeigt, wie viele Kisten erkannt wurden und wie hoch der Gesamtbestand ist.

Trade-Chests findet der Marktstand auf dieselbe Weise (`market.tradeChestRadius`, Standard 2).

---

## Datenhaltung

Jeder gespeicherte Datenblock trägt ein Versionsfeld `data_version` und durchläuft beim Laden die Migrationskette ([Datenmigration](#datenmigration)).

### Serverweit — `SavedData`

Auf Ebene der Server-Welt, nicht pro Dimension — sonst hätte der Nether seinen eigenen Markt.

| ID | Klasse | Version | Inhalt |
|---|---|---|---|
| `dtp:market` | `MarketData` | **7** | Marktdruck je Ware, Markttag, Contract-Uhr, zuletzt gesehene Spieler (für die Spielerskalierung), Marktereignis (Countdown, Ziel, Vorwarnung, Ziel-Warteschlange), Preisverlauf, bekannte Warengruppen |
| `dtp:progress` | `WorldProgress` | **4** | Durchlauf gestartet, Beacon-Position und Dimension, Server-Upgrades, aktive Effekte am Hauptbeacon, Neben-Beacons mit ihrer Auswahl, serverweit abgeschlossene Contracts |
| `dtp:contract_board` | `ContractBoard` | **1** | Angebote im Aushang, nächste Rotation |
| `dtp:blackmarket` | `BlackmarketData` | **1** | Markttag der letzten Nutzung (Tageslimit), frühester Tick für den nächsten Schwarzmarkthändler |
| `dtp:chunk_loaders` | `ChunkLoaderData` | **1** | Restlaufzeit in Ticks je Chunkloader (Dimension und Position) |
| `dtp:statistics` | `StatisticsData` | **1** | Verkäufe je Item (gesamt und je Markttag der letzten 14), abgeschlossene Contracts je Spieler und Stufe mit letztem Namen |

Die **Chunk-Tickets** des Chunkloaders sind kein eigener Zustand, sondern aus `dtp:chunk_loaders` abgeleitet. Sie laufen über einen NeoForge-`TicketController` (`dtp:chunk_loader`). Beim Serverstart verwirft er alle gespeicherten Tickets, und der Takt alle 20 Ticks setzt sie neu, sobald ein Spieler online ist. Ohne Spieler nimmt er sie wieder weg. So kann kein Ticket einen abgebauten Block oder eine abgelaufene Laufzeit überleben.

Der Marktdruck wird **nicht getickt**, sondern bei Bedarf aus dem letzten Stand und der vergangenen Marktzeit abgeleitet. Das vermeidet eine Tick-Schleife über alle Waren. Die Marktuhr läuft nur, während Spieler online sind; Abwesenheit wird nicht nachgeholt.

### Pro Spieler — Data Attachments

| ID | Klasse | Version | Inhalt | Sync |
|---|---|---|---|---|
| `dtp:contracts` | `PlayerContracts` | **2** | Angenommene Contracts mit Fortschritt, persönliche Contract-Uhr, abgeschlossene Contracts je Stufe | – |
| `dtp:player_upgrades` | `PlayerUpgrades` | **3** | Stufen der persönlichen Upgrades, ausgerüstete Module | eigener Client |
| `dtp:module_cooldowns` | `ModuleCooldowns` | **1** | Zeitpunkt, ab dem ein Modul wieder bereit ist | eigener Client |

Alle drei überleben Tod und Dimensionswechsel (`copyOnDeath` — in NeoForge eine bewusste Einstellung, kein Standard). Upgrades und Abklingzeiten gehen an den eigenen Client, weil Tasten und HUD der Module wissen müssen, was ausgerüstet ist.

### Im Inventar

Taler, Contraband-Items mit ihren Ladungen, Schmuggelkisten, die Markierung als Händlerware (`dtp:merchant_goods`). Kein Attachment, keine Synchronisation, kein Sonderfall beim Tod — sie verhalten sich wie jedes andere Item.

### Statisch

Handelsliste, Warengruppen, Contract-Vorlagen, Upgrade-Definitionen, Contraband-Werte, Schwarzmarkt-Angebote — über **Datapack**. Balancing-Werte — über die **Config**.

---

## Netzwerk

**Der Server ist die einzige Wahrheit.** Der Client bekommt Anzeigedaten, nie Entscheidungsgewalt. Jeder Kauf, Verkauf und jede Contract-Annahme wird serverseitig geprüft.

| Richtung | Inhalt |
|---|---|
| Server → Client | Menü-Daten (`MenuSync`), Stufen der Server-Upgrades (`ServerUpgradeSync`), Spieler-Upgrades und Abklingzeiten (Attachment-Sync), Doppelsprung-Impuls, Rückbau-Markierung (Baumeister) |
| Client → Server | Nur der Tastendruck eines Skills (`dtp:skill`) außerhalb von Menüs; Menü-Aktionen laufen über die Menüs selbst |

Marktwerte werden nur übertragen, solange eine Marktstand-Oberfläche offen ist — dann allerdings die ganze Handelsliste: Suche und Gruppenfilter laufen auf dem Client, weil nur er die übersetzten Itemnamen kennt.

Der Client kennt die Stufen der Server-Upgrades, weil manche Wirkungen clientseitig vorgerechnet werden (Amboss-Kosten bei *Meisterschmied*). Für den *Rückbau* des Baumeisters führen Server und Client dieselbe kurze Liste zurücknehmbarer Blöcke (Paket `rebuild_mark`), weil der Client das Abbautempo vorrechnet.

---

## Konfiguration

Es gibt **eine** Config: `dtp-synced.toml`, Typ `SYNCED`. Sie liegt im Ordner `config` der Instanz bzw. des Servers und wird an verbundene Clients geschickt. Eine Kopie unter `<welt>/syncedconfig/dtp-synced.toml` gilt statt ihr für diese Welt. Eine Client-Config gibt es nicht.

| Inhalt | Ort |
|---|---|
| Handelsliste mit Basiswerten, `H`, `T` | **Datapack** `dtp/trade_list` |
| Warengruppen des Marktstands | **Datapack** `dtp/market_category` plus Item-Tags |
| Contract-Vorlagen und Verzauberungswerte | **Datapack** `dtp/contract` |
| Server- und Spieler-Upgrades: Kosten, Stufen, Werte, Voraussetzungen, Lage im Baum | **Datapack** `dtp/upgrade`, `dtp/player_upgrade` |
| Contraband: Ladungen, Abklingzeit, Werte | **Datapack** `dtp/contraband` |
| Schwarzmarkthändler: Angebote | **Datapack** `villager_trade/blackmarket`, `trade_set/blackmarket`, Tags |
| Preisband, `floorRatio`, Spielerskalierung, Marktereignisse, Contract-Fristen und -Werte, Grenzradien, Effektwerte, Schwarzmarkt, Feature-Schalter | **Config** `dtp-synced.toml` |

Die Config lässt sich im Spiel prüfen und zurücksetzen (`/dtp admin config diff` / `reset`), siehe [Migration](72-migration.md#config-standards-per-befehl-übernehmen).

### Wirkungen im Code, Zahlen in den Daten

Vollständig datengetriebene Upgrades sind eine Illusion: Jedes Upgrade braucht Code für seine Wirkung. Die Arbeitsteilung:

- **Im Code:** was ein Upgrade *tut* (die IDs stehen in den Enums `ServerUpgrade` und `PlayerUpgrade`).
- **Im Datapack:** was es *kostet*, wie viele Stufen es hat, welche Werte es je Stufe hat, was es *voraussetzt*.

Damit läuft eine Balancing-Runde zwischen zwei Sitzungen ohne Neukompilierung, Datapack-Änderungen greifen nach `/reload` ([Balancing](62-balancing.md#kalibrierung)).

### Tag-basierte Preise

Preise greifen auch über **Tags**, mit Vorrangregel: direkter Item-Eintrag schlägt Tag-Eintrag, spezifischer Tag schlägt allgemeinen.

**Sichere Voreinstellung:** Ein Item ohne Eintrag ist nicht verkaufbar. Das ist zugleich das zentrale Balancing-Instrument der kuratierten Handelsliste.

### Verwaiste Marktdaten

Wird ein Mod aus dem Pack entfernt, überspringt die Handelsliste dessen Einträge ersatzlos; gespeicherter Marktdruck dieser Items wirkt nicht mehr. Ein Reset dieser Daten ist unproblematisch — es geht kein Spielerfortschritt verloren.

---

## Integration mit Vanilla-Systemen

### World-Border

Die Grenze nutzt die **Vanilla-World-Border**. Das spart die gesamte Darstellung (rote Wand, Warnungen) und das Kollisionsverhalten.

Gesetzt wird sie **bei Ereignissen**, nicht laufend: beim Start des Durchlaufs, bei jedem Kauf einer Grenzstufe, per Admin-Befehl und beim Zurücksetzen von `beacon.radiusChunks` oder `beacon.borderEnabled`. Ein Admin, der die Border manuell setzt, überschreibt sie; das nächste Grenz-Upgrade korrigiert sie wieder.

### Darkness und Nether-Sperre

Die Deepslate-Staffelung ([Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md)) nutzt den Vanilla-Darkness-Effekt, geprüft in einem Intervall statt in jedem Tick. Die Nether-Sperre verhindert das Zünden von Portalen und Reisen in den Nether, bis der Nether-Zugang gekauft ist.

### Ohne Mixin

DTP greift nur über NeoForge-Events, Attribute und Datapacks ein, nie per Mixin. Wo Vanilla kein passendes Event hat, rechnet der Mod das Vanilla-Verhalten nach (Amboss-Kosten in `AnvilCap`, Bestäubung in `Beekeeping`). Diese Stellen sind bei Minecraft-Updates zu prüfen; GameTests vergleichen sie mit Vanilla ([Entwicklung und Test](71-entwicklung-und-test.md#entscheidungen-im-code)).

**Händlerware** ([Markt](10-markt.md#händlerware)) kommt ohne Mixin aus. Seit 26.x gibt es kein `VillagerTradesEvent` mehr, die Angebote sind datengetrieben (`villager_trade`, `trade_set`). `MerchantGoods` markiert deshalb die lebenden Ergebnis-Stapel der Angebote (`MerchantOffer.getResult()`; `assemble()` kopiert sie samt Komponenten in den Ergebnis-Slot):
- bei `PlayerContainerEvent.Open` für ein `MerchantMenu` — das feuert, bevor die Angebote an den Client gehen. Den Händler findet die Umkreissuche über `getTradingPlayer()`, weil `MerchantMenu.trader` privat ist; nur der Schwarzmarkthändler wird ausgelassen.
- bei `TradeWithVillagerEvent` — für Angebote, die ein Stufenaufstieg mitten im Handel erzeugt.

Kombinieren gibt die Markierung weiter, weil Vanilla nur die Komponenten des linken bzw. oberen Items kopiert:
- Amboss: `AnvilUpdateEvent` mit Priorität `LOWEST` (nach `AnvilCap`) markiert das Ergebnis, wenn links oder rechts Händlerware liegt.
- Crafting-Reparatur: `data/minecraft/recipe/repair_item.json` zeigt auf `dtp:repair_item` (`MerchantRepairItemRecipe`, Unterklasse von `RepairItemRecipe`), weil Vanilla das Ergebnis mit `new ItemStack` neu baut. Bei Minecraft-Updates gegen `RepairItemRecipe` prüfen.
- Schleifstein: `GrindstoneEvent.OnPlaceItem` bricht ab, wenn nur das untere Item markiert ist (`mergeItems` ist privat und wird nicht nachgebaut).

Der GameTest `merchant_goods_combine` prüft alle drei. Die Geschenke an den Helden des Dorfes markiert der Loot-Modifier `MerchantGoodsModifier`. Bei Minecraft-Updates prüfen: neue Wege, auf denen Dorfbewohner Items herausgeben, und ob `assemble()` weiter Komponenten kopiert (GameTest `merchant_goods_offers`).

---

## Abschaltbarkeit

Jedes System außerhalb des Wirtschaftskerns lässt sich in `dtp-synced.toml` abschalten:

| Schlüssel | Standard | Aus heißt |
|---|---|---|
| `beacon.borderEnabled` | `true` | Keine World-Border |
| `beacon.effectsEnabled` | `true` | Keine Beacon-Effekte, weder am Hauptbeacon noch an Neben-Beacons |
| `access.deepslateDarkness` | `true` | Keine Darkness unter y = 0 |
| `access.netherLock` | `true` | Nether ohne Freischaltung erreichbar |
| `events.enabled` | `true` | Keine Marktereignisse |
| `blackmarket.traderEnabled` | `true` | Kein Schwarzmarkthändler |
| `blackmarket.scamChance` | `0.2` | Mit `0` kein Scam am Schwarzmarkt |
| `curse.tollPrice` | `1` | Mit `0` kostet der Maut-Fluch nichts |
| `contraband.enabled` | `true` | Contraband-Items bleiben erhalten, tun aber nichts |
| `repair.enabled` | `true` | Reparaturbänke lassen sich öffnen, reparieren aber nicht |
| `chunkloader.enabled` | `true` | Chunkloader nehmen keine Taler an und laden nichts; gespeicherte Laufzeit bleibt erhalten |
| `market.protocol` | `true` | Kein Messprotokoll `protocol.csv` |

Der Grund ist Selbstschutz, nicht Höflichkeit: Jedes dieser Systeme greift in Vanilla-Verhalten ein und kollidiert absehbar mit anderen Mods. Ein abschaltbares System, das kollidiert, ist ein Config-Eintrag. Ein nicht abschaltbares ist ein Fehlerbericht.

Nicht abschaltbar: Marktstand, Taler, Preise, Contracts. Ohne sie gibt es keinen Mod.

---

## Kompatibilität

DTP **ersetzt keine gut gebauten bestehenden Mods**. Wo sich Funktionen überschneiden, wird die eigene weggelassen oder abschaltbar gemacht — nie die fremde blockiert.

| Bereich | Haltung |
|---|---|
| Sophisticated Backpacks | Inventarerweiterung ist **kein** DTP-Feature. Optional zwei Rucksack-Upgrades als Lager (unten) |
| Grabmods | Tod-Upgrades werden nicht nachgebaut |
| Waystones u. ä. | Reisepunkte werden nicht nachgebaut |
| Kampf- und Boss-Mods | Kampfprogression ist nicht DTPs Aufgabe |
| Claim-Mods | Die Grenze ist kein Schutzsystem, Kollision unwahrscheinlich |
| Rezept-Gating (KubeJS, Quests) | Eigene Sperren per Config abschaltbar |

Items aus fremden Mods sind verkaufbar, sobald ein Preis existiert — über Tags oder Modpack-Datapacks. DTP bringt selbst keine Preise für fremde Items mit.

### Optionale Anbindungen ohne Abhängigkeit

DTP läuft ohne jeden anderen Mod. Eine Anbindung darf das nie ändern. Dafür gelten fünf Regeln:

| Ebene | Regel | Ohne den fremden Mod |
|---|---|---|
| Build | Fremde Mods nur als `compileOnly`, zum Testen `localRuntime` hinter `-Pwith_compat=true`. Nie `implementation` | Nichts davon steckt in der JAR |
| `neoforge.mods.toml` | Eintrag mit `type="optional"` und `versionRange` | DTP lädt normal; nur eine zu alte Version wird abgelehnt |
| Code | Fremde Klassen nur in `dev.dtp.compat.<mod>`, erreichbar nur über `dev.dtp.compat.Compat` hinter `ModList.isLoaded`. Kernklassen importieren nie etwas daraus | Die JVM lädt die Klasse nie, also kein `NoClassDefFoundError` |
| Daten | Rezepte, Loot und Tags für fremde Inhalte tragen `"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "…"}]` | Die Datei wird übersprungen |
| Kern | Der Kern bietet eigene Schnittstellen an (Capabilities, Trigger, Tags), an die Compat-Module andocken | Der Kern weiß nichts von fremden Mods |

Wo es ohne Code geht, gibt es keinen Code: **Better Advancements** zeigt nur Vanilla-Advancements an. DTPs Progressionslinie ist ein normaler Advancement-Baum (`data/dtp/advancement/progression/`) und erscheint ohne den Mod im Vanilla-Bildschirm, mit ihm in dessen Ansicht.

**Lager-Schnittstelle.** Marktstand, Bulletin-Board, Werkstatt, Beacon und Schwarzmarkt suchen Trade-Chests und Einzahlkisten nicht nach Blockklasse, sondern nach den Block-Capabilities `dtp:trade_storage` und `dtp:deposit_storage` (`DtpStorage`, Typ `Container`). Jeder Block, der sie anbietet, zählt als Lager — auch einer aus einem anderen Mod. Ein Anbieter von `dtp:deposit_storage` muss Nicht-Taler in `canPlaceItem` ablehnen.

**Sophisticated Backpacks.** Zwei Rucksack-Upgrades (`dtp:trade_chest_upgrade`, `dtp:deposit_chest_upgrade`, Rezept: Upgrade-Basis + Trade-Chest bzw. Einzahlkiste) lassen einen **platzierten** Rucksack die passende Lager-Capability anbieten. Ein getragener Rucksack zählt nie: Bezahlt wird weiterhin nie aus dem Inventar. Je Rucksack ist nur eines der beiden möglich, denn ein Rucksack ist wie die Kisten entweder Waren- oder Geldlager. Das Upgrade passt nur in Rucksäcke, nicht in andere Speicher von Sophisticated Core. Rucksäcke nehmen in den Upgrade-Slot nur Items mit dem Tag `#sophisticatedbackpacks:upgrade`; DTP trägt seine beiden dort ein (`data/sophisticatedbackpacks/tags/item/upgrade.json`, mit `"required": false`, damit der Tag ohne den Mod nicht scheitert). Ohne Sophisticated Backpacks werden die Items gar nicht registriert: kein Rezept, kein Eintrag im Kreativ-Tab, keiner in JEI.

---

## Datenmigration

Jeder Datenblock trägt ein Versionsfeld (`data_version`), und beim Laden läuft eine Kette von Migrationsschritten von der gespeicherten bis zur aktuellen Version (`Versioned`). Ist die gespeicherte Version neuer als die unterstützte, lehnt der Mod den Stand ab, statt ihn zu beschädigen — eine ältere Jar lädt keinen neueren Spielstand.

**Das Grundgerüst ist billig, wenn es von Anfang an da ist.** Nachträglich wird es teuer: Altdaten ohne Versionsfeld lassen sich nur raten. **Die einzelnen Schritte sind meist klein:** ein Feld umbenennen, einen Standardwert für ein neues Feld setzen, eine ID umschreiben. Der eigentliche Aufwand liegt in der Disziplin, bei jeder Formatänderung daran zu denken. Bei einem Progressionsmod, in dem Monate an Fortschritt hängen, ist der Verlust einer Spielerdatei der schlimmste denkbare Fehler.

Was sich bei einem Update konkret ändert, steht in [Migration](72-migration.md).

---

## Serverleistung

- **Marktdruck** wird bedarfsgesteuert berechnet, nicht getickt.
- **Beacon-Effekte** laufen in einem Intervall, nur bei Spielern in Reichweite; Attribut-Effekte alle 10 Ticks.
- **Grenzprüfungen** erledigt die Vanilla-World-Border.
- **Trade-Chests** werden nur gelesen, solange eine Oberfläche offen ist, und verkauft nur auf Knopfdruck oder Redstone-Signal.
- **Marktwaage** rechnet ihr Signal einmal pro Sekunde neu.
- **Chunkloader** werden einmal pro Sekunde zentral aus `ChunkLoaderData` getaktet, nicht pro Block. Jeder hält 9 Chunks (`chunkloader.radiusChunks`), und das nur, solange jemand online ist. Der Preis je Ladung bremst, wie viele Chunks gleichzeitig laufen.
- **Preisverlauf** wächst nicht pro Tick: ein Punkt pro Markttag und Ware (Tiefst-, Höchst- und letzter Wert), höchstens 14 Tage (`MarketData.HISTORY_DAYS`).
- **Produktionseffekte** wie Pflanzenwachstum hängen an den Vanilla-Events des einzelnen Blocks (`CropGrowEvent`) und iterieren nie über alle Blöcke im Radius.
