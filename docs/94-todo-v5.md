# Todo v5

**Legende:** 

- 🧭 Entscheidung oder Konzeptarbeit
- 🔧 Umsetzung 
- 🧪 Test im Spiel

## 2. Texturen und Modelle

- [x] 🔧 Texturen und Blockmodelle statt Platzhaltern — alle Blöcke, auch Werkstatt und Neben-Beacon aus v2.
- [x] 🔧 Eigene Icons bzw. Rahmen für den Upgrade-Baum, falls die Platzhalter aus v2 nicht reichen.

## 3. Better Achievements

## 4. New Blocks

### Repair Station

Eine Werkbank mit eigener UI. Als Basis soll die UI des Blackmarket Blocks verwendet werden. Hier kann ein Rüstungsteil oder ein Werkzeug abgelegt werden und Taler. Dabei soll die Haltbarkeit des Gegenstandes abhängig von dem Taler-Item repariert werden.

Bitte mach ein Vorschlag für ein Reparatur-Kurs. Es soll etwas teurer sein und soll nur eine early-game Mod alternative zu Mending geben.

Dafür muss auch ein eigener Block erzeugt werden und die Texture sollen basierend auf den aktuellen Design Dokument erstellt werden.

Umgesetzt als **Reparaturbank** (`dtp:repair_station`), siehe [Reparaturbank](16-reparaturbank.md).

- [x] 🧭 Kurs: Preis je Haltbarkeitspunkt = `2 × (maxHaltbarkeit / 250) ^ 0,5`. Eisenspitzhacke 500, Diamantwerkzeug ≈ 7.800 (≈ Wertziel zweier Stufe-2-Contracts), Netherit ≈ 11.580. Jede Taler-Stückelung, Münzen einzeln, Wechselgeld ins Inventar.
- [x] 🔧 Gemeinsame Basis `TwoSlotMenu`/`TwoSlotScreen` aus dem Schwarzmarkt herausgelöst; Schwarzmarkt darauf umgestellt.
- [x] 🔧 Logik `RepairStation`, Menü, Screen, Block. Config `repair.enabled`, `repair.basePrice`, `repair.exponent`. Messprotokoll `repair`.
- [x] 🔧 Rezept (Kupferbeutel, 2 Eisenbarren, Schleifstein, 3 Bretter), Blockstate, Modell, Texturen (`block_textures.py`), Slot-Symbol `taler` (`item_textures.py`), Loot-Table, Axt-Tag, Lang, Creative-Tab, Handbuchseite, Rezeptbuch beim ersten Betreten, JEI-Panel.
- [x] 🔧 GameTests `repair_station_quote`, `repair_station_rejects`, `repair_station_repair`; `data_loaded`, `onboarding`, `guide_files` ergänzt.
- [x] 🔧 Doku: `16-reparaturbank.md`, `00`, `01`, `61`, `70`, `71`, `72`, `73`, `90`, `scripts/README.md`.
- [x] 🧪 Bank craften, setzen (Front zum Spieler), mit der Axt abbauen. Texturen in Originalgröße neben Werkstatt und Marktstand ansehen.
- [x] 🧪 Eisenspitzhacke teilweise mit Kupfersäcken und voll mit einem Silbertaler reparieren; Panelwerte und Wechselgeld im Inventar prüfen.
- [x] 🧪 Sperren im Panel: unbeschädigt, Scam-Fluch, Kupfertaler auf Diamantspitzhacke. Schließen gibt Item und Geld zurück.
- [x] 🧪 Schwarzmarkt nach dem Umbau auf `TwoSlotMenu`/`TwoSlotScreen` unverändert; mit JEI überdeckt die Liste keines der beiden Panels.

#### Contraband aufladen

Das Aufladen von Contraband soll dieselbe Rückgeldmechanik bekommen wie die Reparaturbank.

- [x] 🧭 Aufladen wandert an die Reparaturbank, das Crafting-Rezept `dtp:contraband_recharge` entfällt. Fehlende Ladungen zählen wie Haltbarkeitspunkte, Preis je Einheit = `charge_price`. Münzen einzeln, Wechselgeld ins Inventar.
- [x] 🔧 `RepairStation`: `accepts`, `missing`, `unitPriceMilli`, Sperre `FULLY_CHARGED`, Ladungen setzen, Ton Seelenanker, Messprotokoll `recharge`. Menü und Screen mit eigenen Texten für Ladungen.
- [x] 🔧 `ContrabandRechargeRecipe`, Rezeptdatei und Registrierung entfernt; Tooltip `dtp.contraband.recharge` und Meldung `dtp.contraband.empty` verweisen auf die Bank.
- [x] 🔧 GameTest `contraband_recharge` prüft jetzt die Bank (Rechnung, Wechselgeld, Sperren, Rezept weg); `repair_station_rejects` angepasst.
- [x] 🔧 Doku: `13`, `16`, `70`, `71`, `72`, `90`.
- [ ] 🧪 Feuerlöscher mit wenigen Ladungen in die Bank: Panel zeigt „1 Ladung = 250 Taler“ und „Voll aufladen“; mit einem Silbertaler aufladen, Ladungen und Wechselgeld im Inventar prüfen.
- [ ] 🧪 Volles Contraband sperrt mit „voll geladen“; Item + Taler im Crafting-Grid ergibt nichts mehr; Werkzeugreparatur unverändert.

### Gambling Machine Slots

Eine Slotmaschine, man kann Taler einzahlen und nach einer Minecraft-Style Animation erfährt man ob man gewonnen oder verloren hat und erhält dann ein Gewinn, ein Trostpreis, oder garnichts.
Die Bedienung ist per Rechtsclick mit einem der 9 Taler Items.

Da es kein ernstes Feature sein soll um die Menge an Taler zu erhöhen, möchte ich dass die Gewinnwahrscheinlichkeit möglichst realistisch ist, was dazu führen soll dass es eigentlich nur eine Taler verschwendung sein soll.

Der Block soll keine UI haben, damit andere Spieler mitfiebern können.

Dafür muss auch ein eigener Block erzeugt werden und die Texture sollen basierend auf den aktuellen Design Dokument erstellt werden.

Umgesetzt als **Spielautomat** (`dtp:slot_machine`), siehe [Spielautomat](17-spielautomat.md).

- [x] 🧭 Gewinnplan: Jackpot 0,5 % (×81), Gewinn 5 % (×9), Trostpreis 20 % (÷9, eine Stufe kleiner; beim Kupfertaler nichts). Auszahlungsquote ≈ 87,7 %. Ausgang zuerst würfeln, dann passende Symbole.
- [x] 🔧 Logik `SlotMachine`, Block mit `facing`/`spinning`, Blockentität mit gespeicherter Drehung, Auswurf vorn als Item-Entity (unabhängig von der Blockdrop-Gamerule), Meldung im Umkreis von 16 Blöcken, Messprotokoll `slot_machine_<ausgang>`. Config `slotMachine.*`.
- [x] 🔧 `SlotMachineRenderer`: Item-Symbole laufen durchs Fenster, Walzen halten nacheinander (1,5 / 2,25 / 3 s); kein eigenes Netzwerkpaket.
- [x] 🔧 Rezept (Eisen, Glasscheibe, Gold, Kupfersack, Hebel, Redstone), Blockstate, zwei Modelle (Hebel oben/unten), Texturen (`block_textures.py`), Loot-Table, Spitzhacken-Tag, Lang, Creative-Tab, Handbuchseite mit Raster.
- [x] 🔧 GameTests `slot_machine_odds`, `slot_machine_payout`, `slot_machine_spin`; `data_loaded`, `onboarding` ergänzt.
- [x] 🔧 Doku: `17-spielautomat.md`, `00`, `01`, `70`, `71`, `72`, `73`, `90`, `scripts/README.md`.
- [x] 🧪 Automat craften, setzen (Front zum Spieler), in alle vier Richtungen ansehen: Walzen im Fenster, Hebel rechts, Symbole von vorn lesbar.
- [x] 🧪 Mit Kupfersäcken spielen: Animation, Klackern, Stopps nacheinander, Auswurf vorn. Zweiter Einwurf während der Drehung wird abgelehnt.
- [x] 🧪 Zweiter Spieler sieht dieselbe Animation und hört den Ergebniston (Jackpot-Fanfare auch aus ~30 Blöcken). Für Jackpot kurz `slotMachine.chanceJackpot` auf 1 setzen.

### Recycler

Ein Mülleimer der Items zerstört und dabei wenige Taler ausspuckt. Gedacht für items wie Lederschuhe oder Sättel die beim Angeln gefarmt werden können.

Man muss darauf achten das Kelp/Bambus nicht für ein höheren Wert recycelt werden können als am Markt verkauft werden kann.

Hier braucht es Vorschläge für die Funktionsweisen und das Craftinrezept.
- sollen Items die am Markt verkauft werden könnten, nicht recyclebar sein? Das würde dazu führen dass zB alle Holzerzeugniss recyclebar wären.
- wenn Stattdessen eine Whitelist von Items entstehen soll, dann würde sich das mit einem Festpreis auch am Markt verkaufen lassen. Das ist aber eine unintuitive Bedienung des Marktstandes.

Bitte mach Vorschläge wie mit der Vision des Mods vereinbart ist ungewollte Items mit einem Restwert zu versehen der möglichst nicht exploitable ist. Dieser Block ist optional, für den Fall wär eine sinnvolle Regelung hilfreich.

Umgesetzt als **Recycler** (`dtp:recycler`), siehe [Recycler](19-recycler.md).

- [x] 🧭 Keine Taler, sondern **Schrott** (`dtp:scrap`) als eine Ware der Handelsliste (Ruhepreis 1, H 1.000, T 4), verkauft am Marktstand. Angenommen wird nur Ausrüstung (Regel A5) ohne Ofenverwertung: Leder, Stein, Diamant, Netherit, Sattel, Bogen, Angel usw. Eisen-, Gold-, Ketten- und Kupferausrüstung schmelzen zu Nuggets, Holzwerkzeuge sind Brennstoff. Wert = 5 % der Rohstoffe, Bruchteile als Chance, Kappung 40. Seetang, Bambus und Bauformen bleiben wertlos.
- [x] 🔧 `Recycling` (Datenliste `recycling/*.json`, Ausschlüsse inkl. Ofenrezepten, Zufallsrundung), Block mit Comparator, Blockentität mit Eingabe-/Ausgabefach für Hopper. Bedienung ohne Meldungen: Töne für Fehlgriff, Bestätigung und Zerlegen.
- [x] 🔧 Rezept (Eisengitter, Trichter, Schleifstein, Eisen), Blockstate, Modell, Texturen (`block_textures.py`, Schrott in `item_textures.py`), Loot-Table, Spitzhacken-Tag, Lang, Creative-Tab, Handbuchseite mit Raster; nicht im Rezeptbuch.
- [x] 🔧 GameTests `recycler_process`, `recycler_rejects`, `recycler_hand`, `recycler_chance`, `recycling_data`, `scrap_tradable`; `data_loaded`, `onboarding` ergänzt.
- [x] 🔧 Doku: `19-recycler.md`, `00`, `01`, `10`, `60`, `61`, `70`, `71`, `72`, `73`, `90`, `scripts/README.md`.
- [x] 🧪 Recycler craften, setzen (Front zum Spieler), mit der Spitzhacke abbauen. Texturen in Originalgröße neben Spielautomat und Werkstatt ansehen.
- [x] 🧪 Angel-Loot per Hopper oben und Kohle seitlich einwerfen, Schrott unten per Hopper in eine Trade-Chest leiten und am Marktstand verkaufen; Preis sinkt und erholt sich.
- [x] 🔧 Ofen-Umbau: Fenster wie der Ofen (`RecyclerMenu`, `RecyclerScreen` mit Vanilla-Textur), Brennstoff mit Vanilla-Brennwerten, 400 Ticks je Item, Hopper-Seiten wie beim Ofen, Blockzustand `lit` mit glühender Front, Licht und Knistern. Hand-Recycling und Doppelklick entfallen. GameTest `recycler_hand` → `recycler_menu`.
- [x] 🧪 Fenster: Stiefel und Kohle einlegen, Flamme und Pfeil laufen etwa 20 s, Schrott erscheint; der Block leuchtet und knistert.
- [x] 🧪 Lavaeimer als Brennstoff: Der leere Eimer kommt unten per Hopper heraus. Seetang von Hand in die Eingabe bleibt liegen, kein Brennstoff wird verbraucht.

### Chunkloader

Dafür muss auch ein eigener Block erzeugt werden und die Texture sollen basierend auf den aktuellen Design Dokument erstellt werden. Es soll im transparenten Glaskasten Stil wie der Neben-Beacon sein. Es soll aber eine Enderperle in der Mitte des Blocks schweben.

Der Block soll aus einer Deposit Kiste im Umkreis von 16 Blöcken 2500 Taler verbrauchen und dafür den Chunk in dem er ist und die umliegenden 8 Chunks (insgesamt 9 Chunks) für die nächsten 3 Minecraft Tagen geladen halten, damit Farms auch in Abwesenheit der Spieler weiterlaufen können. Das soll auch im Nether funktionieren.

Entschieden: wirkt nur, solange jemand online ist (Abwesenheit von der Basis, nicht vom Spiel); Laufzeit in Spielticks; Nachladen stapelt bis 3 Ladungen; Rezept `GGG / TET / SSS` (Glas, Silberbeutel, Enderperle, Seelensand). Siehe [Chunkloader](20-territorium-und-serverweite-effekte.md#chunkloader).

- [x] 🔧 Block `dtp:chunk_loader`, Ticket-Controller, `ChunkLoaderData`, Oberfläche, Renderer der schwebenden Perle, Config `chunkloader.*`.
- [x] 🔧 Texturen über `scripts/block_textures.py`, Modell, Rezept, Loot-Table, Handbuchseite mit Raster.
- [x] 🔧 Doku: Territorium, Vision (Anti-Ziele), Preise, Balancing, Technischer Rahmen, Entwicklung, Migration, Grafikstil, Glossar.
- [x] 🔧 GameTest `chunk_loader` (Bezahlen, Unterdeckung, Obergrenze, online/offline, Ablauf, Abbau).
- [x] 🧪 Im Spiel: Weizen- oder Ofenfarm läuft weiter, während man über 200 Blöcke entfernt bzw. im Nether ist; Perle schwebt nur mit Restlaufzeit.
- [x] 🧪 Im Spiel: Chunkloader im Nether (z. B. an einer Goldfarm), Mob-Spawning ohne Spieler in der Nähe.
- [x] 🧪 **Multiplayer**: Alle Spieler ausloggen, die Chunks ruhen, die Restlaufzeit bleibt; nach dem Einloggen läuft sie weiter.

### Statistics

Der Block soll eine UI erhalten an dem man sich die All-Time verkäufe des Server und die der letzten 14  Minecraft Tage ansehen kann. Dabei soll summiert werden welche Items wie viele Taler erwirtschaftet haben.

Es soll auch die Anzahl der abgegebenen Contracts angezeigt werden und welcher Spiele von welcher Stufe wie viele abgeschlossen hat.

Dafür muss auch ein eigener Block erzeugt werden und die Texture sollen basierend auf den aktuellen Design Dokument erstellt werden. Ein "Whiteboard" oder "Flipboard" aufsteller.

Umgesetzt als **Statistik-Tafel** (`dtp:statistics_board`), siehe [Statistik-Tafel](18-statistik.md).

- [x] 🧭 Voller Block mit `facing`, Flipchart aus Pergament (keine neue Farbe). Der Zeitraum 14 Tage/Gesamt gilt nur für Verkäufe, Contracts zählen immer insgesamt. Altbestände je Spieler werden beim Login übernommen.
- [x] 🔧 `StatisticsData` (`dtp:statistics` v1), Hooks in `Market.sell`, `Contracts.complete` und beim Login, `StatisticsMenu` mit zwischengespeichertem Datenstand.
- [x] 🔧 `StatisticsScreen`: Reiter Verkäufe/Contracts, Warengruppen wie am Marktstand, Sortierung nach Stück oder Taler auf- und absteigend. Die Gruppenleiste ist als `CategoryBar` aus `MarketScreen` herausgelöst, die Gruppenliste kommt aus `MarketCategories.snapshot`. `tierColor`/`compact` liegen jetzt in `Gui`.
- [x] 🔧 Rezept (Eisennuggets, Papier, Bretter, Kupfertaler), Blockstate, Modell, Texturen (`block_textures.py`), Loot-Table, Axt-Tag, Lang, Creative-Tab, Handbuchseite mit Raster.
- [x] 🔧 GameTests `statistics_sales`, `statistics_seed`, `statistics_snapshot`; `contract_delivery`, `data_loaded`, `onboarding` ergänzt.
- [x] 🔧 Doku: `18-statistik.md`, `00`, `01`, `70`, `71`, `72`, `73`, `90`, `scripts/README.md`.
- [x] 🧪 Tafel craften, setzen, in alle vier Richtungen ansehen.
- [x] 🧪 Ware verkaufen: erscheint unter 14 Tage und Gesamt. Gruppe wählen, Summen prüfen, Stück/Taler auf- und absteigend sortieren. `/dtp admin market advance 14`: 14 Tage leer, Gesamt bleibt.
- [x] 🧪 Contract abgeben: Tabelle und Gesamtzahl steigen. Marktstand: Gruppen und Sortierung unverändert.

## 5. Sophisticated Backpacks Compatibility

- Backpack Upgrades welches einen platzierten Rucksack als Deposit oder Tradechest funktionieren lässt.

## 6. Technische Finalisierung

- [x] 🔧 Automatischer Build und GameTests bei jedem Push (GitHub Actions nach MDK-Vorlage, mit und ohne optionale Mods, dazu `check_assets.py`), Hook vor dem Push, Release-Entwurf je Versions-Tag, MIT-Lizenz.
- [x] 🔧 JSON-Dateien (Modelle, Loot-Tables, Rezepte, Tags) automatisch prüfen statt per Datagen erzeugen: GameTest `data_files_valid` (Codec-Rundlauf, unbekannte Schlüssel und IDs) und `scripts/check_assets.py` (Modell-, Textur- und Sprachverweise).

## 7. Contract Update

- [x] Remove Pumpkin
- [x] Remove Cactus
- [x] Händlerware: Villager-Ware markiert (`dtp:merchant_goods`), sperrt Contracts aller Stufen, Markt und Recycler ([Markt](10-markt.md#händlerware))
- [x] Händlerware wird beim Kombinieren weitergegeben: Amboss (links oder rechts), Reparatur im Crafting-Grid, Schleifstein (Ware unten: kein Ergebnis) ([Markt](10-markt.md#händlerware))
- [x] Gecraftete Stufe-3-Waren ([Contracts](12-contracts.md#gecraftete-waren-stufe-3)), auch nicht stapelbare (Kuchen, Suppen, Ragout, Buch und Feder, Lore); verzierter Krug entfällt (Ziegel vom Maurer), Feuerstein entfällt (nur Kies, eine Quelle)
- [x] Offen: Tränke (welche Sorten, Bedingung über `potion_contents`, Anzeige am Aushang prüfen), Werkzeuge (welche), Sattel
- [x] ⚖️ D1/D2 neu bewerten: Lapis, Glowstone-Staub, Enderperle und Apfel sind als Händlerware unverkäuflich, ihre Dorfbewohner-Deckel sind nicht mehr nötig ([Dorfbewohnerhandel](60-handelsliste-bewertung.md#dorfbewohnerhandel))
- [x] ⚖️ Stufe-4-Verzauberungswerte anheben: Sie waren niedrig, weil Villager-Bücher lieferbar waren
- [x] ⚖️ Recycler-Kappung (40 Schrott) neu bewerten: Villager-Ausrüstung wird nicht mehr angenommen

Tier3 Contracts sollen nicht nur die Quantität der Items weiter erhöhen. Stattdessen sollen komplexere Items gefertigt werden die verkauft werden können. Dazu gehören auch Items die sich nicht am Markt verkaufen lassen, ihre Komponenten können durchaus verkaufbar sein.

## Miscelanious 

### Pick-Up Radius upgrade

Autopick items für:
- Vein-Mining 
- Tree Felling
- Area Harvest
- Wool Shearing

Pickup radius increase for duration of "rush"

### Hoe-right click, "item use" animation

- [x] 🔧 Flächenernte: Client sagt den Rechtsklick vorher (`SUCCESS`) und schwingt die Hand wie beim Blockplatzieren; der Server schickt den Schwung an die anderen Spieler.

## Mount Chill Timer

- [x] 🔧 Stallmeister **Ruhe**: Nach dem Absteigen im Radius bleibt ein Reittier aus `dtp:mounts` 60 s stehen (`beacon.mountRestSeconds`), −100 % auf `MOVEMENT_SPEED`/`FLYING_SPEED`. Endet beim Anleinen, Aufsteigen oder nach Ablauf. Doku `21`, Guide, Lang, GameTest `beacon_building`, Migration.

## Upgrade: Ghast Fly Speed

- [x] Bereits vorhanden: Der Stallmeister gibt dem gelenkten Glücklichen Ghast +20 % `FLYING_SPEED` (`beacon.mountSpeedBonus`). Kein eigenes Upgrade.

## Neben-Beacon

- Passive Se rverweite Effekte

## Mending Aura Rework

- [x] 🔧 Mending-Aura: Stufe I 1, Stufe II 2 Haltbarkeit alle 10 s (`beacon.mendingAuraRepair`/`2`), dafür im ganzen Effektgebiet des Hauptbeacons statt 10 / 30 Blöcken. Doku `20`, `30`, Migration.
- [x] 🔧 Neben-Beacons tragen die ortsgebundenen Server-Upgrades ohne Slot: Mending-Aura und Ofentempo (`BeaconLogic.inBeaconArea`). Ofentempo wirkt dafür nicht mehr weltweit. Griefing-Schutz und Meisterschmied bleiben weltweit. GameTest `server_rules`.

## Bulletin Board

Das Bulletin Board soll eine Flagge erhalten, die dem Spieler Still, aber visuell zeigt, wenn neue Contracts zur Verfügung stehen. Das neu rollen der Contracts soll die Flagge anheben, ein interagieren mit dem Bulletin board, also das öffnen der Gui, soll die Flagge wieder senken. 

Als Beispiel kann der Slotmachine arm dienen.

anders als der Slotmachine-Arm, darf die Flagge höher sein als das Bulletin Board.

Umgesetzt, siehe [Das Bulletin-Board](12-contracts.md#das-bulletin-board).

- [x] 🔧 Rotationszähler im `ContractBoard`, `BulletinBoardBlockEntity` (zuletzt gezeigte Rotation, Prüfung jede Sekunde), Blockzustand `flag`, Absenken beim Öffnen.
- [x] 🔧 Multipart-Blockstate: Würfel unverändert (auch als Item), dazu bei `flag=true` `bulletin_board_flag_raised` mit Mast, Knauf und Flagge bis y 31,5 aus einem Schacht in der Oberseite (eingeholt fährt alles in den Block, kein Zusatzmodell); Atlas `bulletin_board_flag` in `block_textures.py`.
- [x] 🔧 GameTest `board_flag`; Doku `12`, `72`, `scripts/README.md`.


## Personal Upgrade Module Active

### Fishing Net

- [x] 🔧 Skill `fishing_net` (Fischernetz): tötet Kabeljau, Lachs, Tropenfisch und Kugelfisch im Wasser im Umkreis von 3 Blöcken, Beute ins Inventar, 5 s Abklingzeit, 30.000 Taler. Tab Ausrüstung neben Doppelschwimmer, eigenes Symbol (`upgrade_icons.py`), Tag `#dtp:fishing_net`, GameTest `fishing_net`, Doku `31`.

## Offene Tests

Alle noch offenen Prüfungen im Spiel, gesammelt aus den Abschnitten oben.

### Händlerware (Contract Update)

Villager-Ware wird beim Kauf als `dtp:merchant_goods` markiert und ist dann für Contracts aller Stufen, den Markt und den Recycler gesperrt ([Markt](10-markt.md#händlerware)).

- [x] 🧪 Bauer zeigt „Händlerware“ im Handelsfenster; Klick und Shift-Klick aus dem Ergebnisslot behalten die Markierung; markierter Kuchen zählt am Aushang nicht als „auf Lager“; Villager-Wolle ist am Marktstand unverkäuflich; Ware vom Schwarzmarkt trägt keine Markierung.
- [x] 🧪 Waschen per Kombinieren: Villager-Buch auf selbst gebaute Angel und Buch + Villager-Buch im Amboss zeigen „Händlerware“; zwei Villager-Eisenschwerter im Crafting-Grid zeigen schon in der Vorschau „Händlerware“; Schleifstein mit Villager-Schwert unten zeigt kein Ergebnis, oben ein markiertes.

### Flächenernte: Schwung beim Rechtsklick

Der Rechtsklick mit Hacke und Flächenernte wird jetzt vom Client vorhergesagt und schwingt die Hand wie beim Blockplatzieren; der Server schickt den Schwung an die anderen Spieler.

- [x] 🧪 Weizen, Netherwarze und Kakao mit Hacke + Flächenernte rechtsklicken: Schwung in Ego- und Third-Person, ein zweiter Spieler sieht ihn auch.

### Stallmeister: Ruhe nach dem Absteigen

Reittiere aus `dtp:mounts` bleiben nach dem Absteigen im Beacon-Radius 60 s stehen (`beacon.mountRestSeconds`). Die Ruhe endet beim Anleinen, Aufsteigen oder nach Ablauf.

- [x] 🧪 Pferd im Territorium absteigen → steht ~1 min; Leine dran → folgt sofort; außerhalb des Territoriums wandert es wie Vanilla; Glücklicher Ghast schwebt nach dem Absteigen still.

### Mending-Aura und Ofentempo an Neben-Beacons

Die Mending-Aura repariert jetzt im ganzen Effektgebiet des Hauptbeacons (Stufe I 1, Stufe II 2 Haltbarkeit alle 10 s). Neben-Beacons tragen Mending-Aura und Ofentempo ohne Slot; das Ofentempo wirkt dafür nicht mehr weltweit, sondern nur noch im Beacon-Gebiet.

- [x] 🧪 Aura im Territorium fern vom Beacon und an einem Neben-Beacon im Nether; Ofen außerhalb (normales Tempo) und neben einem Neben-Beacon (schneller).
- [x] 🧪 Stärke von Stufe II (2 Haltbarkeit / 10 s) über längere Zeit auf dem Server beobachten: zu stark als Mending-Ersatz?

### Bulletin-Board-Flagge

Die Flagge am Bulletin Board hebt sich, sobald eine neue Contract-Rotation gewürfelt wurde, und senkt sich, wenn jemand das Board öffnet ([Das Bulletin-Board](12-contracts.md#das-bulletin-board)).

- [x] 🧪 Board setzen (nur Schacht sichtbar), `/dtp admin contracts reroll` (Mast und Flagge fahren aus), Board öffnen (alles im Block); in alle vier Ausrichtungen.

### Fischernetz

Neuer Skill `fishing_net` (30.000 Taler, Tab Ausrüstung): tötet Kabeljau, Lachs, Tropenfisch und Kugelfisch im Wasser im Umkreis von 3 Blöcken, Beute ins Inventar, 5 s Abklingzeit.

- [x] 🧪 Kaufen, ausrüsten, an einem Fischschwarm auslösen; eigenes Symbol im Upgrade-Baum und im Modul-HUD ansehen.

### Marktstand: Suchfeld verliert den Fokus

Der Neuaufbau der Knöpfe (`PanelScreen.containerTick`, sobald sich der Layout-Schlüssel ändert, etwa Lagerwert oder Seitenzahl) löschte über `Screen.rebuildWidgets` → `clearFocus` den Fokus des Suchfelds. `PanelScreen.rebuildWidgets` gibt ihn jetzt dem Widget zurück, das den Neuaufbau überlebt.

- [x] 🧪 Mit Ware im Lager in die Suche tippen, während sich der Marktdruck erholt: Das Feld behält den Fokus, auch wenn sich die Seitenzahl ändert.



### Trade Chest

- [x] 🔧 Comparator liest den Füllstand von Trade-Chest und Einzahlkiste wie bei einer Vanilla-Kiste (Gametest `chest_comparator`).
- [x] 🧪 Im Spiel: Comparator an Trade-Chest und Einzahlkiste; Signal steigt beim Befüllen und fällt sofort nach Verkauf bzw. Einkauf am Marktstand.

### Deposit Chest

- [x] 🔧 Eine steigende Redstone-Flanke an der Einzahlkiste löst „Taler ordnen" aus, ein Dauersignal nur einmal (Gametest `deposit_redstone`).
- [x] 🧪 Im Spiel: Kiste mit verstreutem Kleingeld, Knopf daneben; jeder Druck ordnet, ein Hebel in Dauerstellung ordnet nur einmal.

### Statistics Block

Ein eigener Reiter in der GUI mit einem Balkendiagram für die Taler Einnahmen der Welt.
Jeder Tag soll von einem Balken repräsentiert werden der die erwirtschafteten Taler darstellt. So hat man ein Rückblick der letzten Tage. Beim drüberhovern soll die Zahl dargestellt werden, da die Y-Achse vermutlich dynamisch relativ zu den anderen Balken sein soll.

Umgesetzt als Reiter **Einnahmen**, siehe [Statistik-Tafel](18-statistik.md#einnahmen): je Minecraft-Tag (F3) ein gestapelter Balken aus Verkaufserlös und Contract-Belohnungen, die letzten 14 Tage.

- [x] 🔧 `StatisticsData` v2 mit Einnahmen je Minecraft-Tag (`Market.sell`, `Contracts.complete`), Datenstand `income`, Reiter mit Diagramm, Achse und Tooltip.
- [x] 🔧 GameTest `statistics_income`; `contract_delivery` prüft den Lohn in den Einnahmen.
- [ ] 🧪 Im Spiel: verkaufen und einen Contract abschließen, der heutige Balken wächst gestapelt (Gold unten, Grün oben).
- [ ] 🧪 Schlafen oder `/time add 1d`: rechts entsteht ein neuer Balken, die übrigen rücken nach links, die Achse skaliert neu.
- [ ] 🧪 Hover zeigt Tag, Summe und Aufteilung.
- [ ] 🧪 Zeitraum als ein Umschaltknopf rechts in der Gruppenreihe (nur „Sales“): Ein Klick wechselt zwischen „14 Tage“ und „Gesamt“. Die 9 Standardgruppen bleiben einreihig, und oben stehen die drei Reiter in voller Breite, auf Deutsch und Englisch.

### Multiplayer

Fälle, die nur mit einem zweiten Spieler bzw. einem Nicht-Admin prüfbar sind.

- [ ] 🧪 **Heilimpuls** (Skill `heal_pulse`): gibt Regeneration dem Auslöser und allen Spielern im Umkreis von 6 Blöcken; mit zweitem Spieler prüfen, dass beide Regeneration erhalten.
- [ ] 🧪 **Rechte der `/dtp`-Befehle**: Ein Spieler ohne OP (unter Berechtigungsstufe 2) kann `/dtp admin …` und `/dtp beacon y` weder ausführen noch per Tab vervollständigen, `/dtp help` listet sie nicht; `/dtp price` funktioniert.
- [x] 🧪 **Multiplayer**: Die Fähigkeitstaste wirkt nur mit ausgerüstetem Skill.
- [x] 🧪 Im Spiel prüfen: `/dtp admin market event fire fast_recovery`, zwei Waren verschiedener Gruppen verkaufen; der Druck steigt weniger, und die Erholung läuft für beide schneller.

## Release

- [ ] 🧪 Öffentliches Repo nach der privaten Anleitung einrichten: Neustart der Historie, erster grüner Lauf, Release 5.0.0.