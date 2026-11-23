# Markt

> Taler, Einzahlkiste, Marktstand, Trade-Chest, Marktwaage und die Handelsliste. Wie sich Preise bilden, steht in [Marktdruck](11-marktdruck.md).

---

## Der Taler

Taler entstehen durch Verkauf am Marktstand und durch [Contracts](12-contracts.md). Ausgegeben werden sie für Server-Upgrades am Beacon und für persönliche Upgrades an der Werkstatt, außerdem am [Schwarzmarkt](14-blackmarket.md) und zum Aufladen von [Contraband](13-contraband.md).

### Taler sind Items

Die Währung ist **kein Kontostand**, sondern ein Item. Grundsatz *„Put it on the Board"*: Was im Spiel eine Rolle spielt, soll im Spiel sichtbar sein.

| Folge | Wirkung |
|---|---|
| Taler brauchen Platz | Reichtum ist ein Lagerproblem, kein Menüwert |
| Taler müssen transportiert werden | Der Weg zur Einzahlkiste ist Teil des Spiels |
| Taler lassen sich verschenken und zusammenlegen | Gemeinsame Finanzierung von Server-Upgrades ist wörtlich möglich |
| Taler fallen beim Tod wie jedes Item | Transport ist ein Risiko; Taler in der Einzahlkiste sind sicher |
| Taler lassen sich stehlen | Auf einem Freundesserver ein Reiz, kein Problem |

Spielerhandel braucht kein eigenes System — Truhen und Hände reichen.

### Talerstufen

Je neun Einheiten einer Stufe ergeben eine der nächsten, über drei Metalle in je drei Formen. Die Rezepte gehen in beide Richtungen.

| Item | ID | Wert in Kupfertalern | Ein Stack |
|---|---|---|---|
| Kupfertaler | `dtp:copper_taler` | 1 | 64 |
| Kupferbeutel | `dtp:copper_pouch` | 9 | 576 |
| Kupfersack | `dtp:copper_sack` | 81 | 5.184 |
| Silbertaler | `dtp:silver_taler` | 729 | 46.656 |
| Silberbeutel | `dtp:silver_pouch` | 6.561 | 419.904 |
| Silbersack | `dtp:silver_sack` | 59.049 | 3.779.136 |
| Goldtaler | `dtp:gold_taler` | 531.441 | 34 Mio. |
| Goldbeutel | `dtp:gold_pouch` | 4.782.969 | 306 Mio. |
| Goldsack | `dtp:gold_sack` | 43.046.721 | 2,75 Mrd. |

Die feine Leiter erlaubt **große Basiswerte**: Eine Karotte darf mehrere Taler wert sein, ohne dass Preisschritte in Rundung verschwinden. Ein Silbertaler ist 729 und nicht 9 Kupfertaler wert — die Oberfläche weist Beträge deshalb immer in Talern aus.

### Auszahlung in kleinstmöglicher Stückzahl

Jeder Erlös wird in **so wenigen Items wie möglich** ausgezahlt, größte Stufe zuerst:

```
500 Taler  →  6 Kupfersäcke (486) + 1 Kupferbeutel (9) + 5 Kupfertaler (5)
```

Dasselbe gilt für Contract-Belohnungen und Wechselgeld. Ohne diese Regel würde ein großer Verkauf das Inventar mit Einzelmünzen fluten.

### Taler in Strukturkisten

Strukturkisten sind eine kleine Nebenquelle für Taler. Jede Loot-Table unter `chests/…` (jeder Namespace außer `dtp`) gehört zu einer Stufe. Mit deren Chance liegt ein Betrag zwischen `min` und `max` in der Kiste, ausgezahlt wie oben in möglichst wenigen Münzen. **Höchstens ein Silberbeutel (6.561 Taler) je Kiste.**

| Stufe | Kisten | Chance | Taler | Ø je Kiste |
|---|---|---|---|---|
| `village` | Dorfkisten, Iglu, gewöhnliche Lagerkiste | 40 % | 9 – 243 | ≈ 50 |
| `overworld` | Dungeon, Minenschacht, Schiffswrack-Vorrat und -Karte, Unterwasserruinen, Ruinenportal, versteckte Lagerkiste, Trial-Chamber-Kisten; **außerdem jede unbekannte Kisten-Tabelle** (andere Mods) | 50 % | 81 – 729 | ≈ 200 |
| `temple` | Wüsten- und Dschungeltempel, vergrabener Schatz, Schiffswrack-Schatz, Plünderer-Außenposten, Festung, Waldanwesen | 60 % | 243 – 2.187 | ≈ 730 |
| `nether` | Netherfestung, Bastion (ohne Schatzraum), Antike Stadt | 70 % | 729 – 4.374 | ≈ 1.790 |
| `end` | Endstadt, Bastion-Schatzraum | 80 % | 2.187 – 6.561 | ≈ 3.500 |

- **Ausgenommen:** Bonustruhe, Trial-Chamber-Tresore (`chests/trial_chambers/reward…`) und der Dispenser im Dschungeltempel.
- **Rucksack-Mobs** (Sophisticated Backpacks) würfeln Kisten-Tabellen und bekommen Taler nach deren Stufe. Hochwertige Rucksäcke sind selten, deshalb ist das gewollt.
- **Unabhängig vom Schwarzmarktfund:** Eine Kiste kann Taler und ein [Contraband-Item](13-contraband.md#fundorte-strukturkisten) enthalten.
- **Daten:** `data/dtp/loot_modifiers/structure_taler.json` (`tiers` mit `name`, `chance`, `min`, `max`, `tables`; `default_tier`; `exclude`). Ein Eintrag in `tables`, der auf `/` oder `*` endet, ist ein Präfix. Die erste passende Stufe gewinnt. Abschalten geht per Datapack mit `chance` 0.

---

## Die Einzahlkiste

Die Einzahlkiste (`dtp:deposit_chest`) hält **Taler für Käufe bereit**. Sie hat 27 Slots und nimmt nur Taler an, auch per Hopper.

Upgrades werden **ausschließlich aus Einzahlkisten in Reichweite** bezahlt, nie aus dem Spielerinventar. Erlöse vom Marktstand und Contract-Belohnungen vom Bulletin-Board landen ebenfalls dort. Zieht ein Kauf aus dem Inventar, ist bei mehreren Spielern in der Nähe nie klar, wessen Taler ausgegeben werden. Eine Kiste macht sichtbar, was zur Verfügung steht.

Marktstand, Bulletin-Board, Werkstatt und Beacon haben kein eigenes Geldfach. Dadurch braucht nur ein Blocktyp eine Inventarverwaltung, und eine zentrale Kiste kann mehrere Funktionsblöcke versorgen. Kompakt bauen oder mehrere Kisten aufstellen ist ein Layout-Problem, also Spielinhalt.

### Mehrere Kisten in Reichweite

- Alle Kisten im Radius bilden **einen gemeinsamen Vorrat**. Die Oberfläche zeigt den Bestand der Kiste und den Vorrat aller Kisten in Reichweite.
- Entnommen wird in fester Reihenfolge: **nächste Kiste zuerst**, bei gleichem Abstand nach Koordinaten. Je Kiste gehen die kleinsten Stufen zuerst weg; Wechselgeld landet in der ersten Kiste, aus der entnommen wurde.
- Reicht der Vorrat nicht, wird **nichts** entnommen.
- Der Server ermittelt den Bestand bei jedem Kauf neu und vertraut nie der angezeigten Summe.
- Gesucht wird nur beim Öffnen einer Oberfläche und beim Kauf. Kisten in nicht geladenen Chunks zählen nicht.

| Config | Standard | Bedeutung |
|---|---|---|
| `deposit.radius` | 2 (1–32) | Reichweite in Blöcken für Marktstand, Bulletin-Board und Werkstatt |
| `beacon.depositRadius` | 16 (1–32) | Reichweite des Beacons, weil er auf einer Pyramide steht |

### Taler ordnen

Der Knopf **„Taler ordnen"** fasst die Taler der Kiste in möglichst wenige Items zusammen. Der Wert bleibt gleich, andere Items bleiben liegen.

Eine **steigende Redstone-Flanke** an der Kiste ordnet ebenfalls, einmal je Flanke; ein Dauersignal ordnet nicht erneut. So räumt eine Trichteranlage, die Kleingeld einspeist, sich per Taster oder Takt selbst auf.

---

## Marktstand und Trade-Chest

### Die Logistikkette

```
Farm → Hopper → Trade-Chest → (Verkauf am Marktstand) → Einzahlkiste → Upgrades
```

Der **Marktstand** (`dtp:market_stand`) ist die Handelsstation: ein Block, **kein NPC**. Ein Händler, der behütet werden muss, sterben oder despawnen kann, erzeugt Ärger ohne Gegenwert. Der Marktstand hat kein Inventar.

Die **Trade-Chest** (`dtp:trade_chest`) ist sein Warenlager: Items hineinlegen oder per Hopper fördern. Sie verkauft nicht selbst. Der Marktstand liest alle Trade-Chests im Radius `market.tradeChestRadius` (Standard 2 Blöcke, 1–32).

Mit Sophisticated Backpacks zählt auch ein **platzierter Rucksack** mit Trade-Chest- oder Einzahlkisten-Upgrade als die jeweilige Kiste ([Kompatibilität](70-technischer-rahmen.md#kompatibilität)).

### Verkaufen

| Weg | Wirkung |
|---|---|
| Aus der Hand | „1 verkaufen", „Stapel", „Alle" (alle gleichen Items im Inventar). Ist die Hand danach leer, rückt der nächste Stapel **desselben** Items aus dem Inventar nach — „Stapel" lässt sich so wiederholt klicken, andere Items rücken nie nach |
| Aus dem Lager | Die Ware der Trade-Chests erscheint als Liste mit Menge und Erlös; verkauft wird je Ware oder alles auf einmal |
| Per Redstone | Eine steigende Flanke am Marktstand verkauft **einen Stapel** — den ersten verkaufbaren aus der nächstgelegenen Trade-Chest |

Der Erlös geht in die Einzahlkisten in Reichweite des Marktstands. Findet er dort keinen Platz, geht er ins Inventar des Spielers; beim Redstone-Verkauf fällt er am Marktstand heraus.

Verkaufen ist immer ein **ausgelöster Vorgang**, kein stiller Dauerverkauf. Niemand ruiniert versehentlich seine eigenen Preise, und Automatisierung bleibt möglich, ohne sich zu verselbständigen. Eine Oberfläche mit Filterlisten und Preisschwellen an der Trade-Chest gibt es bewusst nicht: Eine Anlage würde damit jede Ware vollautomatisch zum besten Zeitpunkt verkaufen. Die Marktbremse lebt davon, dass Verkaufen eine Entscheidung bleibt. Den gezielten Weg bietet die [Marktwaage](#marktwaage).

Die Trade-Chest ist zugleich das Lager für Contract-Lieferungen am Bulletin-Board ([Contracts](12-contracts.md)). Verkauft wird nur am Marktstand, geliefert nur am Bulletin-Board — so ist nie unklar, was mit eingelegter Ware passiert.

### Preisanzeige

Vor jedem Verkauf sieht der Spieler:

- den aktuellen Marktwert pro Stück,
- den **Gesamterlös der gewählten Menge** — nicht Stückpreis × Menge, denn der Preis sinkt während des Verkaufs. Angezeigt wird exakt der Betrag, der ausgezahlt wird,
- eine **Druckanzeige**: eine Skala von Rot (Untergrenze des Preisbands) bis Grün (Obergrenze), mit Markierung für den aktuellen Stand und einem Strich beim Ruhepreis. Die Skala ist linear, genau wie die Höhe im Preisverlauf: Ein Preis liegt im Balken und im Graphen an derselben Stelle und hat dieselbe Farbe,
- eine **Suche** nach Itemnamen und **Filtergruppen**,
- eine **Sortierung** der Preisliste per Klick auf die Spaltenköpfe „Preis" oder „Druck": erst absteigend, dann aufsteigend, dann wieder in der Reihenfolge der Handelsliste,
- in der ersten Zeile das nächste [Marktereignis](11-marktdruck.md#marktereignisse) mit Ziel und Restzeit, außerhalb des Territoriums stattdessen den Warnhinweis.

Das Server-Upgrade **Marktinformation** (`market_info`) schaltet zusätzlich frei: Prozentwerte, den Trendpfeil (auch in der Zeile „In der Hand“: dort ersetzt der Prozentwert in der Farbe der Druckskala die Druckanzeige), den **Preisverlauf** und das Rezept der Marktwaage. Der Preisverlauf öffnet sich per Klick auf eine Zeile der Preisliste: aktueller Stückpreis, je Markttag Tiefst-, Höchst- und letzter Wert über 14 Markttage, darunter Ruhepreis, Tiefst- und Höchstwert, die Halbwertsmenge `H` (mit Spielerskalierung und Preisstabilität) und die geschätzte Erholungszeit. Sie zählt, bis der Preis 95 % des Ruhepreises erreicht hat, und erscheint nur, wenn das länger als einen Markttag dauert.

### Filtergruppen

Neun Gruppen, definiert unter `data/dtp/dtp/market_category/<id>.json`. Welche Items dazugehören, bestimmt der Item-Tag `dtp:market_category/<id>`. Ein Item darf in mehreren Gruppen stehen, etwa die Ender-Perle in Monsterbeute und Das Ende.

| Gruppe | ID | Freischaltung |
|---|---|---|
| Landwirtschaft | `farming` | – |
| Tierhaltung | `animals` | – |
| Fischerei | `fishing` | – |
| Baumaterial | `construction` | – |
| Erze & Minerale | `ores` | – |
| Monsterbeute | `mob_drops` | – |
| Nether | `nether` | Nether zugänglich (Server-Upgrade gekauft oder Sperre abgeschaltet) |
| Das Ende | `end` | Enderdrache auf dem Server einmal besiegt |
| Loot | `loot` | – |

Eine gesperrte Gruppe fehlt am Marktstand, und kein Marktereignis wählt sie aus. Ein Item, das **nur** in gesperrten Gruppen steht, fehlt in der Preisübersicht — die Chorusfrucht erscheint erst nach dem Drachensieg. Verkaufen lässt es sich trotzdem.

### Massenware unter einem Taler

Billige Massenware wie Seetang, Bambus oder Bruchstein hat den Basiswert 0,71, also einen Ruhepreis um 1 Taler. Gerechnet wird je Stück mit Bruchteilen, abgerundet wird erst der Gesamterlös. Ein Stapel Seetang bringt in Ruhe 64 Taler, bei voller Sättigung (5 %) noch 2 Taler.

- **Anzeige:** Stückpreise unter 1 Taler stehen mit Nachkommastelle da („0,7"), sehr kleine mit zwei („0,03"). Sie werden abgeschnitten, nicht gerundet. Ab 1 Taler ist die Anzeige ganzzahlig.
- **Mindesterlös 1 Taler:** Ein Verkauf, der weniger einbrächte, findet nicht statt; die Items bleiben, wo sie sind. Die Knöpfe sind ausgegraut („Verkaufspreis ist < 1 Taler"), Lagerzeilen darunter erscheinen gedämpft. „Alles aus dem Lager verkaufen" und der Redstone-Verkauf überspringen solche Waren. Der Server prüft das bei jedem Verkauf selbst.
- **Keine Sackgasse:** Der Preisfaktor wird nie 0, und die Erholung läuft im Marktdruck. Auch aus voller Sättigung steigt der Preis wieder auf den Ruhepreis.

---

## Marktwaage

Die Marktwaage (`dtp:market_scale`) ist ein flacher Block, der den **Marktwert einer Ware als Comparator-Signal** ausgibt.

- **Filter setzen:** Rechtsklick mit einem Item. Das Item bleibt in der Hand, auf der Waage schwebt nur ein Abbild.
- **Filter löschen:** Schleichen + Rechtsklick mit leerer Hand, oder den Block abbauen.
- **Signal:** je angefangene 10 % Marktwert eine Stufe, höchstens 15. Gerechnet wird mit den gerundeten Prozent der Marktstand-Anzeige.

| Marktwert | Signal |
|---|---|
| unter 10 % (Boden 5 %) | 0 |
| 10–19 % | 1 |
| … | … |
| 100 % (Basiswert) | 10 |
| ab 150 % | 15 |

- Es gilt der Preis **am Standort der Waage**: Außerhalb des Territoriums liegt er am Boden und gibt 0.
- Waren mit gemeinsamem Marktdruck zeigen den Druck der ganzen Ware.
- Nicht handelbare Items geben 0.
- Die Waage rechnet **einmal pro Sekunde** neu, weil sich der Druck mit der Marktzeit, durch Marktereignisse und durch fremde Verkäufe ändert, ohne dass ein Blockupdate fällt.

**Uhr binden:** Mit dem Spieler-Upgrade Auftragsuhr bindet Schleichen + Rechtsklick mit einer Vanilla-Uhr die Uhr an das Filter-Item der Waage. Die Uhr übernimmt eine Kopie und zeigt im Tooltip den Preis im vollen Markt, etwa „Weizen: 12 T, 85%“, unabhängig davon, wo sie ist. Danach spielt die Waage keine Rolle mehr; für ein anderes Item bindet man neu, eine leere Waage löst die Bindung. Den Preis schickt der Server alle 2 Sekunden, aber nur bei einer Änderung. Details in [Contracts](12-contracts.md#auftragsuhr-und-fernlieferung).

Eine Waage steuert **genau eine Ware** in groben Stufen. Eine kleine Farm mit An/Aus-Schalter für den Redstone-Verkauf ist damit leicht gebaut. Eine Anlage, die viele Waren einzeln nach Preis steuert, braucht je Ware eine Waage und eigene Redstone-Logik — der Aufwand wächst mit jeder Ware.

**Rezept:** drei Goldbarren über einem Comparator über einer Glatten-Stein-Stufe. Es passt erst nach dem Server-Upgrade *Marktinformation*, an der Werkbank wie im Crafter. Technisch ist das der Rezepttyp `dtp:upgrade_shaped`: ein geformtes Rezept mit dem Feld `"upgrade"`.

---

## Die Handelsliste

Nur Items auf der **kuratierten Handelsliste** sind verkaufbar. Ein Item ohne Eintrag ist nicht verkaufbar — das ist kein Fehler, sondern die sichere Voreinstellung.

Grundsatz: **Ein Eintrag pro Produktionsweg.** Jedes Item steht für eine eigene Tätigkeit oder Quelle: einen Baum, ein Erz, einen Mob, einen Fund. Zwei Items aus derselben Quelle mit derselben Arbeit sind *eine* Ware. Zweite Achse: **Je weniger Handarbeit ein Item braucht, desto schneller sättigt es** — nicht verboten, aber eingepreist.

| Aufgenommen | Nicht aufgenommen |
|---|---|
| Rohstoffe (Stämme, Steine, Barren, Edelsteine) | Crafting-Permutationen (Bretter, Treppen, Türen, Zäune) |
| Agrarerzeugnisse und Tierprodukte | Verarbeitungsprodukte (Gekochtes, Speicherblöcke) |
| Mob-Drops | Werkzeuge, Rüstung, verzauberte Bücher |
| Fundstücke und Monsterköpfe | Items mit Zusatzdaten, außer ausdrücklich zugelassen |

Bei reversiblen Rezepten (9 Barren ↔ 1 Block) wird **nur eine Form** gehandelt. Metalle werden als Barren verkauft, rohe Erze nicht.

Die Basiswerte werden **nicht** aus Rezepten abgeleitet. Rezepte mit Nebenprodukten, Rückgaben und reversible Rezepte erzeugen verlässlich kaputte Preise. Die handgepflegte Liste ist die Quelle der Wahrheit. Wie ein neues Item bewertet wird, steht in [Handelsliste bewerten](60-handelsliste-bewertung.md), Preisbänder und Ankerwaren in [Preise und Größenordnungen](61-preise-und-groessenordnungen.md).

### Dateien und Format

Alle JSON-Dateien unter `data/<ns>/dtp/trade_list/` werden zusammengeführt. Die Reihenfolge ist die **Anzeigereihenfolge am Marktstand**: Dateien nach Namen sortiert, Einträge von oben nach unten.

| Datei | Inhalt |
|---|---|
| `10_agriculture.json` | Pflanzen und Tierprodukte |
| `20_fishing.json` | Fische, Korallen, Seegurken, Seetang, Tintenbeutel, Nautilusschale |
| `30_resources.json` | Holz, Steine, Sand, Ton, Erze, Metalle |
| `40_mobs.json` | Mob-Drops |
| `50_loot.json` | Fundstücke: Schallplatten, Besätze, Scherben, Banner, Monsterköpfe, Sonderfunde |

```json
{ "item": "minecraft:wheat", "base": 6.25, "h": 2000, "t": 2 }
```

| Feld | Bedeutung |
|---|---|
| `item` | Item-ID |
| `base` | Basiswert in Talern |
| `h` | Halbwertsmenge `H` (Standard `market.defaultH` = 500) |
| `t` | Halbwertszeit `T` der Erholung in Markttagen (Standard `market.defaultT` = 3) |
| `key` | optional: gemeinsame Ware |
| `components`, `predicates` | optional: Bedingung an den Stapel |

Jedes Item steht in genau einer Datei. Steht es doch mehrfach, gilt der Wert der letzten Datei und die Position der ersten. Der Code versteht auch Tag-Einträge (`"tag"`), die Handelslisten nutzen sie nicht — jedes Item ist einzeln geprüft.

### Gemeinsame Ware

Manche Items sind in Beschaffung und Wert so ähnlich, dass jedes einen eigenen Markt wäre und man 23 Märkte nacheinander leer verkaufen könnte. Solche Einträge tragen denselben `key`. Sie teilen sich **einen Marktdruck und einen Preisverlauf**, stehen aber einzeln in der Preisliste und behalten ihren eigenen `base`.

```json
{ "item": "minecraft:angler_pottery_sherd", "key": "dtp:pottery_sherd", "base": 283, "h": 10, "t": 10 }
```

- Ohne `key` ist der Schlüssel die Item-ID.
- `h` und `t` gelten für die ganze Ware und kommen vom **ersten** Eintrag des Keys. Weicht ein weiterer Eintrag ab, steht eine Warnung im Log.
- Ein Marktereignis entlastet eine Ware einmal, nicht einmal je Item.
- Im Lager des Marktstands zeigt jede Zeile ihren Erlös für sich — so viel zahlt ihr eigener Verkaufsknopf. „Alles aus dem Lager verkaufen" verkauft die Items einer Ware nacheinander, jedes nach dem Druck der vorigen; die Summe kann daher kleiner sein als die Summe der Zeilen.
- Der Anzeigename einer Ware steht unter `dtp.market_good.<namespace>.<path>` in den Sprachdateien.

| key | Mitglieder |
|---|---|
| `dtp:wood_log` | 10 Stammarten |
| `dtp:wool` | 16 Wollfarben |
| `dtp:egg` | Hühnerei, blaues und braunes Ei |
| `dtp:froglight` | 3 Froschlichter |
| `dtp:coral_block` | 5 Korallenblöcke |
| `dtp:pottery_sherd` | 23 Töpferscherben |
| `dtp:armor_trim` | 18 Rüstungsbesatz-Vorlagen und die Netherit-Vorlage — alle mit Diamanten kopierbar |
| `dtp:farmable_music_disc` | 14 Schallplatten aus Mob-Farmen (Creeper durch Skelett, Tears, Lava Chicken) |
| `dtp:structure_music_disc` | 7 Schallplatten nur aus Strukturen |

### Items mit Komponenten

Ohne besondere Angabe ist jeder Stapel mit Komponenten unverkäuflich — verzauberte, umbenannte oder gefüllte Items kommen nicht auf den Markt.

Ein Eintrag kann mit `components` (exakter Wert) und `predicates` (Bedingung) im Vanilla-Format eine Bedingung an den Stapel stellen. Dann ist ein Stapel handelbar, wenn er sie erfüllt; weitere Komponenten stören nicht — außer der Markierung als [Händlerware](#händlerware). Das Item ohne diese Komponenten ist dann nicht handelbar. Je Item gibt es höchstens einen Eintrag.

Genutzt wird das für das **Böse Banner**: ein weißes Banner mit festem `item_name` (`block.minecraft.ominous_banner`), Muster, Seltenheit und versteckten Musterzeilen. Entscheidend ist `item_name` — den setzt nur das Spiel selbst, ein Amboss setzt `custom_name`. Ein nachgebautes Muster bleibt so unverkäuflich, das schlichte weiße Banner ebenfalls. Der GameTest `ominous_banner` prüft das am echten Banner.

### Händlerware

Alles, was ein Vanilla-Händler herausgibt, trägt die Komponente `dtp:merchant_goods`; der Tooltip zeigt eine graue Zeile **„Händlerware“**. Händlerware ist **unverkäuflich**, zählt für **keinen Contract** (auf keiner Stufe) und geht nicht in den [Recycler](19-recycler.md). So bleibt der DTP-Markt vom Dorfbewohner-Markt getrennt: Was ein Dorfbewohner verkauft, ist im Extremfall gratis und unbegrenzt ([Dorfbewohnerhandel](60-handelsliste-bewertung.md#dorfbewohnerhandel)), und darf deshalb weder Taler noch Contract-Lohn bringen.

| Quelle | Markiert |
|---|---|
| Dorfbewohner und fahrender Händler | Die Ergebnisse aller Angebote, beim Öffnen des Handelsfensters — auch alte, gespeicherte Angebote und Handel anderer Mods. Angebote aus einem Stufenaufstieg werden nach dem Handel markiert |
| Geschenke an den Helden des Dorfes | Alles (Loot-Modifier `dtp:merchant_goods`, Tabellen unter `minecraft:gameplay/hero_of_the_village/`) |
| Schwarzmarkt | Nie: Er gehört zur DTP-Wirtschaft und wird in Talern bezahlt |
| Smaragde | Nie, ebenso alles im Tag `#dtp:merchant_goods_exempt` |

Markierte Stapel stapeln sich nicht mit selbst hergestellten. Schmiede- und Umwandlungsrezepte (Netherit-Aufwertung, Besätze, Färben) übernehmen die Komponenten des Grund-Items, die Markierung bleibt also erhalten. **Weitergegeben** wird sie beim Kombinieren: Im Amboss ist das Ergebnis Händlerware, sobald das linke *oder* das rechte Item markiert ist (ein Villager-Buch auf einer selbst gebauten Angel wäscht also nicht), ebenso bei der Reparatur zweier Items im Crafting-Grid. Im Schleifstein muss die Händlerware oben liegen; liegt sie nur unten, gibt es kein Ergebnis. **Verloren** geht sie beim Platzieren und Abbauen und beim Craften aus markierten Zutaten. Deshalb gilt für Handelsliste und Contract-Pool: kein Block, den ein Dorfbewohner-Beruf verkauft und der sich selbst wieder abbauen lässt, und kein Rezept mit einer Zutat, die ein Beruf verkauft. Der GameTest `merchant_goods_pool` prüft das. Bewusst hingenommen sind Wolle (Schäfer) und Redstone (Geistlicher): Jeder Block muss einzeln von Hand gelegt werden, bepreist werden beide weiter nach D1/D2.

---

## Handel außerhalb des Territoriums

Der volle Markt gilt nur **in der Oberwelt innerhalb der Grenze** (`Market.inTerritory`). Außerhalb — jenseits der Grenze, in einer anderen Dimension oder bevor der Beacon gesetzt ist — liegen alle Preise auf der Untergrenze des Preisbands, also **5 % des Basiswerts**. Verkäufe dort erzeugen keinen Marktdruck. Die Oberfläche weist deutlich darauf hin.

Ohne diese Regel könnte eine Gruppe ausgiebig sammeln und verkaufen und den Beacon erst setzen, wenn die Frühphase vorbei ist. Die Begrenzung der Ressourcenbasis wäre wirkungslos.

Gewählt ist **Preisverfall statt Handelssperre**. Ein Block, der gar nichts tut, wirkt kaputt; ein Preis von 5 % mit Hinweis erklärt sich selbst und sagt, was zu tun ist.

---

## Exploit-Schutz

Die Rückkopplung aus Verkauf → Upgrade → mehr Verkauf macht jede Wertlücke zu einer unendlichen Geldquelle. Umgesetzt sind:

| Maßnahme | Wirkung |
|---|---|
| Kuratierte Handelsliste | Was nicht auf der Liste steht, kann nicht ausgenutzt werden |
| Eine Form je reversiblem Rezept | Umwandeln umgeht keine Sättigung |
| Gemeinsame Ware (`key`) | Gleichartige Items lassen sich nicht nacheinander leer verkaufen |
| Stapel mit Komponenten unverkäuflich | Keine Wertlücke über Verzauberungen, Namen oder Inhalte |
| Händlerware unverkäuflich | Dorfbewohner sind keine Gratis-Farm für Markt, Contracts und Recycler |
| Belohnungen nur in Talern | Contracts zahlen keine verkaufbaren Items |
| Mindesterlös 1 Taler | Kein Verlust von Einzelstücken unter einem Taler |
| Exakter Erlös vorab | Der Server rechnet Verkauf und Anzeige mit derselben Formel |
| 5 % außerhalb des Territoriums | Kein Vorabverkauf vor dem Beacon, keine Handelsposten jenseits der Grenze |
| Schrott zum Bodenpreis | Der [Recycler](19-recycler.md) zahlt höchstens 5 % der Rohstoffe, nimmt nur Ausrüstung ohne Ofenverwertung und sättigt als eigene Ware |

Craften erzeugt keinen Wert: Was sich aus handelbaren Waren herstellen lässt, ist höchstens so viel wert wie die Zutaten ([Handelsliste bewerten](60-handelsliste-bewertung.md)).
