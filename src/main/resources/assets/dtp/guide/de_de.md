<!--
DTP-Handbuch, Deutsch. Diese Datei ist das ganze Buch: Kapitel, Seiten, Reihenfolge und Text.
Format und Arbeitsablauf: docs/71-entwicklung-und-test.md, Abschnitt "Handbuch bearbeiten".

  # Titel                          neues Kapitel (erscheint im Inhaltsverzeichnis)
  ## Titel {#id grid=name warning}  neue Seite; #id ist Pflicht und in allen Sprachen gleich
                                   grid= zeigt ein Rezeptraster: beacon, market_stand, trade_chest,
                                   deposit_chest, bulletin_board, statistics_board, upgrade_table, repair_station, relay_beacon,
                                   chunk_loader, market_scale, blackmarket, slot_machine, recycler, taler, guide
                                   warning hebt die Seite rot hervor
  Leerzeile                        neuer Absatz (einfache Zeilenumbrüche werden zu Leerzeichen)
  **fett**  *kursiv*  - Punkt      Formatierung und Aufzählung
  <!- - … - ->                     Kommentar (ohne die Leerzeichen), wird nicht angezeigt

Zu lange Seiten laufen im Spiel automatisch auf einer Folgeseite weiter.
Die englische Datei (en_us.md) muss dieselben Seiten-IDs, Raster und warning in derselben Reihenfolge haben;
der GameTest guide_files prüft das.

Leitregel für die Einführungskapitel (Willkommen bis Werkstatt, Tiefe, Gerüchte): Jeder Satz sagt, was es gibt,
wo man es findet oder was man als Nächstes tut – nie wie viel oder wie genau. Keine Zahlen, Formeln, Kosten, Radien
oder Zeiten.

Ausnahme sind die Nachschlagekapitel Beacon-Effekte, Serverweite Upgrades und Persönliche Upgrades: Sie sagen, worauf
ein Effekt wirkt (Blöcke, Tiere, Items, Ausnahmen, Folgerungen) und dürfen dafür Listen und Zahlen nennen. Der Tooltip
im Spiel bleibt kurz (was + wie viel), das Buch liefert den Rest. Contraband und Schwarzmarkthändler bleiben draußen.
-->

# Willkommen

## Die Idee {#welcome.idea}

Ihr produziert Waren, verkauft sie am Marktstand für Taler und schaltet damit dauerhafte Verbesserungen frei – gemeinsam für den ganzen Server und jeder für sich selbst.

Vielfalt lohnt sich: Was ihr in Massen verkauft, verliert an Wert. Was eine Weile liegen bleibt, ist bald wieder gefragt.

## Es bleibt Minecraft {#welcome.minecraft}

Es gibt keine feste Reihenfolge und keinen Zwang. Baut, erkundet und farmt, wie ihr wollt – der Mod belohnt euch dafür.

Dieses Buch zeigt nur den Einstieg. Vieles entdeckt ihr am besten selbst: am Beacon, in der Werkstatt und am Marktstand.

## Dieses Buch {#welcome.book grid=guide}

Handbuch verloren? Ein Buch und ein Kupfer-, Eisen- oder Goldnugget ergeben ein neues.

Die wichtigsten DTP-Rezepte stehen außerdem schon in eurem Rezeptbuch.

# Die ersten Schritte

## 1. Der Beacon {#start.beacon grid=beacon}

Sucht euch gemeinsam einen Startplatz.

Craftet dann den Beacon und setzt ihn. Es gibt nur einen pro Server, er lässt sich nicht wieder abbauen, und mit ihm beginnt euer Gebiet.

## 2. Die ersten Taler {#start.market}

Ohne Beacon zahlt der Markt kaum etwas – setzt ihn also zuerst.

Baut dann einen Marktstand und eine Einzahlkiste direkt daneben. Verkauft eure ersten Waren am Marktstand; die Taler landen in der Einzahlkiste.

Mit den ersten Talern schaltet ihr am Beacon Neues frei oder baut euch die Werkstatt.

# Taler

## Taler {#taler grid=taler}

Taler gibt es in Kupfer, Silber und Gold – als Taler, Beutel und Sack. Neun ergeben die nächste Stufe, zurück geht es auch.

Bezahlt wird aus Einzahlkisten in der Nähe, nie aus dem Inventar. Taler fallen beim Tod – in der Kiste sind sie sicher.

# Die Blöcke

## Beacon {#block.beacon grid=beacon}

Das Herz eures Servers. Hier kauft ihr gemeinsame Verbesserungen: ein größeres Gebiet, neue Freischaltungen und Effekte für alle.

## Marktstand {#block.market_stand grid=market_stand}

Hier verkauft ihr – aus der Hand oder aus Trade-Chests direkt daneben. Er zeigt auch die aktuellen Preise.

Ein Redstone-Signal lässt ihn von selbst verkaufen.

## Trade-Chest {#block.trade_chest grid=trade_chest}

Das Warenlager für Marktstand und Bulletin-Board. Füllt sie von Hand oder per Trichter. Ein Comparator liest den Füllstand wie bei einer Kiste.

## Einzahlkiste {#block.deposit_chest grid=deposit_chest}

Eure Kasse: Erlöse landen hier, und alle Käufe werden von hier bezahlt.

Stellt sie in die Nähe von Marktstand, Werkstatt und Beacon. Ein Comparator liest den Füllstand wie bei einer Kiste. Ein Redstone-Signal ordnet ihre Taler wie der Knopf **Taler ordnen**.

## Bulletin-Board {#block.bulletin_board grid=bulletin_board}

Der Aushang für Contracts: Lieferaufträge annehmen und abliefern.

## Statistik-Tafel {#block.statistics_board grid=statistics_board}

Zeigt, was euer Server verkauft hat: je Ware Stück und Taler, für die letzten 14 Markttage oder insgesamt. Filtert nach denselben Gruppen wie der Marktstand; ein Klick auf **Stück** oder **Taler** sortiert auf- oder absteigend.

Der zweite Reiter zählt die abgeschlossenen Contracts: insgesamt und je Spieler nach Stufe.

## Werkstatt {#block.upgrade_table grid=upgrade_table}

Persönliche Upgrades und Module.

Für das Rezept braucht ihr einen Kupfersack – euer erstes Sparziel.

## Reparaturbank {#block.repair_station grid=repair_station}

Repariert Werkzeuge, Waffen und Rüstung gegen Taler – ohne Erfahrung und ohne Material.

Legt den Gegenstand links und Taler rechts hinein. Je wertvoller die Münze, desto mehr wird repariert; zu viel Gezahltes kommt als Wechselgeld zurück. Haltbare Ausrüstung kostet deutlich mehr, auf Dauer bleibt Mending der bessere Weg.

## Neben-Beacon {#block.relay_beacon grid=relay_beacon}

Für Außenposten, auch in anderen Dimensionen. Er verteilt Effekte, die ihr am Beacon schon freigeschaltet habt. Ofentempo und Mending-Aura bringt er ohne Slot gleich mit, sobald ihr sie gekauft habt.

## Chunkloader {#block.chunk_loader grid=chunk_loader}

Hält die Gegend um sich geladen, damit eure Farmen weiterlaufen, während ihr woanders unterwegs seid — auch im Nether.

Bezahlt wird mit Talern aus einer Einzahlkiste in der Nähe, für eine begrenzte Zeit. Die Zeit läuft nur, solange jemand online ist; ist niemand da, ruht auch der Chunkloader.

## Marktwaage {#block.market_scale grid=market_scale}

Gibt den Marktwert einer Ware als Redstone-Signal aus.

Das Rezept funktioniert erst nach einer Markt-Verbesserung am Beacon.

Mit der Auftragsuhr bindet Schleichen + Rechtsklick mit einer Uhr die Uhr an das Item der Waage.

## Schwarzmarkt {#block.blackmarket grid=blackmarket}

Verbessert Verzauberungen gegen Taler.

Aber Vorsicht: Nicht jedes Geschäft dort geht gut aus.

## Spielautomat {#block.slot_machine grid=slot_machine}

Werft per Rechtsklick einen beliebigen Taler ein. Die Walzen drehen sich für alle sichtbar, und was ihr gewinnt, fällt vorn heraus.

- Drei Goldtaler: Jackpot, zwei Münzstufen mehr
- Drei gleiche Symbole: eine Münzstufe mehr
- Die ersten beiden gleich: Trostpreis, eine Münzstufe kleiner

Auf Dauer gewinnt der Automat. Spielt nur, was ihr gern verliert.

## Recycler {#block.recycler grid=recycler}

Zerlegt ausgediente Ausrüstung zu **Schrott**: Lederrüstung, Steinwerkzeuge, Sättel und anderes, was niemand mehr braucht. Was ein Ofen schon zu Nuggets schmilzt, nimmt er nicht.

Er arbeitet wie ein Ofen: oben die Ausrüstung, unten Brennstoff, rechts kommt der Schrott heraus. Er braucht deutlich länger als ein Ofen. Holzwerkzeuge taugen als Brennstoff.

Hopper füllen oben Ausrüstung und seitlich Brennstoff ein und ziehen unten den Schrott ab.

Schrott verkauft ihr am Marktstand. Viel ist er nicht wert.

# Der Markt

## Preise {#market.prices}

Nicht alles lässt sich verkaufen. Der Marktstand zeigt, was er annimmt – vor allem Rohstoffe und Erzeugnisse.

Alle teilen sich dieselben Preise. Wer viel von einer Sorte verkauft, drückt ihren Preis; mit der Zeit erholt er sich wieder. Wechselt also ab.

Außerhalb eures Gebiets zahlt der Markt fast nichts.

## Marktereignisse {#market.events}

Ab und zu ist eine Warengruppe besonders gefragt: Wer dann Ware auf Lager hat, verkauft teuer. Seltener kommt eine Schnelle Erholung für den ganzen Markt. Dann erholen sich gedrückte Preise doppelt so schnell und Verkäufe drücken weniger, laufende Farmen lohnen sich also besonders. Der Marktstand und der Chat kündigen solche Marktereignisse an – haltet die Augen offen.

# Contracts

## Lieferaufträge {#contracts}

Am Bulletin-Board hängen Contracts: Bringt eine bestimmte Menge einer Ware und bekommt dafür mehr als am Markt. Geliefert wird aus den Trade-Chests daneben.

Contracts schaltet ihr am Beacon frei. Jeder erfüllte Contract bringt den ganzen Server weiter.

Schnell sein lohnt sich: Wer innerhalb der Bonusfrist liefert, bekommt 20 % mehr. Danach fällt der Bonus weg, einen Tag lang gibt es noch den vollen Lohn, dann verliert der Contract jeden Tag 10 %, bis auf die Hälfte. Ablaufen kann er nicht. Am Board seht ihr den aktuellen Wert und wann er als Nächstes fällt.

- Stufe 1: 120 % bis Tag 3, 100 % bis Tag 4, 90 % bis Tag 5 … ab Tag 8 50 %
- Die Frist beginnt, sobald der Contract am Board erscheint, nicht erst mit der Annahme.

# Beacon und Gebiet

## Euer Gebiet {#beacon.territory}

Mit dem Beacon entsteht euer Gebiet: der Bereich innerhalb der Weltgrenze. Die Grenze wächst, wenn ihr am Beacon mehr Gebiet kauft.

Marktstand, Bulletin-Board und Werkstatt arbeiten nur innerhalb des Gebiets richtig.

## Am Beacon {#beacon.upgrades}

Am Beacon kauft ihr gemeinsam: mehr Gebiet, neue Freischaltungen, Verbesserungen für den Markt und Effekte für alle.

Bezahlt wird aus Einzahlkisten in der Nähe des Beacons. Schaut euch die Reiter einfach an – vieles erklärt sich dort von selbst. Worauf die Effekte genau wirken, steht hinten im Buch.

# Werkstatt

## Eure Werkstatt {#workshop}

In der Werkstatt kauft jeder seine persönlichen Upgrades. Sie bleiben euch auch nach dem Tod erhalten.

Module rüstet ihr dort aus und könnt sie tauschen. Manche Module sind Fähigkeiten, die ihr mit einer eigenen Taste auslöst (Standard: R).

Worauf die Upgrades genau wirken, steht in den Kapiteln zum Nachschlagen weiter hinten.

<!--
Ab hier Nachschlagekapitel: Hier sind Listen, Zahlen und Ausnahmen erlaubt. Seitentitel = Upgrade-Name aus der
Lang-Datei (ohne "Effekt:"). Nur Effekte, bei denen der Tooltip Fragen offen lässt. Zahlen sind die Standardwerte aus
Datapack und Config — wer sie ändert, passt die Seite hier mit an.
-->

# Beacon-Effekte

## Wirkungsbereich {#fx.range}

Ein Beacon-Effekt wirkt nur, solange er in einem Effektslot steckt. Umstecken ist jederzeit kostenlos.

- Hauptbeacon: nur in der Oberwelt, in einem Quadrat von 2.240 Blöcken um den Beacon – egal, wie groß euer Gebiet gerade ist.
- Neben-Beacon: 80 Blöcke um den Block, nur in seiner eigenen Dimension.
- Die Höhe zählt nie: Auch tief im Bergwerk wirkt alles.
- Derselbe Effekt von mehreren Beacons zählt nur einmal.

## Landwirtschaft {#fx.effect_agriculture}

**Wachstum:** Jeder Wachstumsschritt hat 30 % Chance auf eine Stufe extra. Das gilt für:

- Weizen, Karotten, Kartoffeln, Rote Bete
- Fackellilien
- Kürbis- und Melonenpflanzen, bis sie ausgewachsen sind – die Frucht selbst erscheint so schnell wie sonst

Nicht schneller: Kannenpflanzen, Netherwarzen, Kakao, Süßbeeren, Zuckerrohr, Kaktus, Bambus, Seetang, Höhlenranken.

**Kein Zertrampeln:** Ackerboden bleibt heil, egal wer darauf springt – Spieler, Tiere oder Monster.

**Kompost:** Komposter füllen sich mit 10 % mehr Chance je Item, zum Beispiel Samen 40 statt 30 %. Das gilt auch für Trichter und Bauern.

## Stallmeister {#fx.effect_stablemaster}

**Zucht:** Alle Tiere sind nach 3¾ statt 5 Minuten wieder bereit, auch Frosch, Schnüffler und Schildkröte. Nicht bei Dorfbewohnern.

**Fohlen** erben bessere Werte: Gesundheit, Sprungkraft und Tempo rücken je 10 % näher an den Höchstwert. Das gilt für Fohlen von Pferd, Esel, Maultier, Lama und Händlerlama – die anderen Reittiere erben keine Werte.

**Reittiere** sind im Radius 20 % schneller, wenn ihr sie lenkt. Fallschaden nehmen sie keinen, auch ohne Reiter – und ihr im Sattel ebenfalls nicht. Das gilt für:

- Pferd, Esel, Maultier, Skelett- und Zombiepferd
- Dromedar und Wüstendromedar
- Lama und Händlerlama (nur Fallschutz, lenken lassen sie sich nicht)
- Schwein und Schreiter
- Nautilus und Zombie-Nautilus
- Glücklicher Ghast

**Ruhe:** Steigt ihr im Radius ab, bleibt das Reittier 1 Minute stehen – Zeit genug, es anzuleinen. Sobald es an der Leine hängt oder ihr wieder aufsteigt, ist die Ruhe vorbei.

**Wege:** 30 % schneller auf Trampelpfad, Kies, Grober Erde und Wurzelerde – zu Fuß und auf allen Reittieren. Nicht in Booten und Loren. Auf Wegen addiert sich das mit dem Reittier-Tempo.

*Tipp: Kies gibt es überall – eine Kiesstraße zum nächsten Außenposten ist schnell gebaut.*

## Imkerei {#fx.effect_beekeeping}

Gilt für Bienenstöcke und Bienennester im Radius.

- Honig: Jede Nektar-Ablieferung hat 50 % Chance auf eine Honigstufe extra.
- Ernte von Hand: mit der Schere immer eine Honigwabe mehr, mit der Glasflasche eine zweite Honigflasche. Werfer bekommen keinen Bonus.
- Bestäubung: Bienen mit Nektar lassen bis zu 3 Blöcke neben und 1–3 Blöcke unter sich Pflanzen wachsen: Weizen, Karotten, Kartoffeln, Rote Bete, Fackellilien, Kannenpflanzen, Kürbis- und Melonenpflanzen, Süßbeeren und Höhlenranken.
- Bienen stechen keine Spieler. Erntet ihr ohne Lagerfeuer, bleiben die Bienen im Stock.

*Tipp: Blumen auf die eine Seite des Feldes, Bienenstöcke auf die andere – auf dem Heimflug düngen die Bienen das Feld.*

## Baumeister {#fx.effect_builder}

**Reichweite:** +2 Blöcke für alles, was ihr mit Blöcken macht: bauen, abbauen, Truhen öffnen, Hebel und Knöpfe drücken. Angriffe auf Wesen reichen nicht weiter.

**Rückbau:** Was ihr selbst gesetzt habt, baut ihr 10 Sekunden lang sofort wieder ab. Der Block kommt ins Inventar zurück wie mit Behutsamkeit – Glas bleibt Glas –, und das Werkzeug nutzt sich nicht ab.

- Nur im Überlebensmodus, bis zu 64 Blöcke auf einmal
- Nicht für Blöcke mit Inhalt oder Daten: Truhen, Öfen, Fässer, Schilder, Banner, Betten …
- Nicht für Blöcke, die nie etwas fallen lassen

*Tipp: Hohe Wände und Dächer brauchen deutlich weniger Gerüst – und Hilfsblöcke sammelt ihr sofort wieder ein.*

## Federfall {#fx.effect_feather_fall}

Halbiert den Fallschaden von Spielern im Radius, zusätzlich zur Verzauberung Federfall.

Tiere und Monster fallen wie gewohnt. Reittiere schützt der Stallmeister.

## Expeditionsvorbereitung {#fx.effect_expedition}

- Tränke, die ihr im Radius trinkt, halten doppelt so lang – auch wenn ihr danach das Gebiet verlasst. Mit 25 % Chance bleibt der Trank erhalten.
- Nur trinkbare Tränke, keine Wurf- und Verweiltränke. Sofortwirkungen wie Heilung bleiben einmalig.
- Jedes Essen gibt 50 % mehr Sättigung. Die Hungerleiste füllt sich wie gewohnt.

*Tipp: Vor dem Aufbruch im Gebiet Tränke trinken und satt essen.*

# Serverweite Upgrades

## Ofentempo {#server.furnace_speed}

Gilt für Ofen, Schmelzofen und Räucherofen im Beacon-Gebiet (dieselbe Fläche wie die Beacon-Effekte) und um jeden Neben-Beacon, nicht für das Lagerfeuer. Schmelz- und Räucherofen bleiben doppelt so schnell wie der Ofen, der Bonus kommt obendrauf.

Ein Brennstoff brennt gleich lang, schmilzt im schnelleren Ofen aber mehr Items:

- Kohle: 10 (Stufe I) bzw. 12 (Stufe II) statt 8
- Lavaeimer: 125 bzw. 150 statt 100

*Tipp: Große Schmelz- und Räucheranlagen sparen spürbar Brennstoff.*

## Endermänner lassen Blöcke liegen {#server.enderman_griefing}

Endermen heben keine Blöcke mehr auf und setzen auch keine ab – in allen Dimensionen, auch außerhalb des Gebiets. Gras, Erde, Blumen und Kürbisse rund um die Basis bleiben, wo sie sind.

## Creeper-Schutz {#server.creeper_griefing}

Creeper-Explosionen zerstören keine Blöcke mehr, überall und in allen Dimensionen. Spieler, Tiere und herumliegende Items nehmen weiter Schaden.

Unverändert zerstörerisch: TNT, Ghasts, Wither, Enderkristalle, Betten im Nether und Seelenanker.

## Meisterschmied {#server.anvil_cap}

Kein „Zu teuer!“ mehr: Jede Amboss-Arbeit, die sonst 40 Level oder mehr kosten würde, kostet höchstens 39. Gilt für alle Spieler und jeden Amboss – Reparieren, Verzaubern mit Büchern und Umbenennen.

Günstigere Arbeiten kosten wie gewohnt.

*Tipp: Lieblingswerkzeuge lassen sich so beliebig oft reparieren und nachverzaubern.*

## Mending-Aura {#server.mending_aura}

Repariert beschädigte Gegenstände im ganzen Inventar – auch Rüstung und Zweithand – langsam nebenbei, überall im Beacon-Gebiet (dieselbe Fläche wie die Beacon-Effekte) und um jeden Neben-Beacon, auch im Nether und End.

- Stufe I: 1 Haltbarkeit alle 10 Sekunden, Stufe II: 2
- Mehrere Beacons zugleich zählen nur einmal

*Tipp: Die Aura pflegt, Mending repariert. Für Werkzeuge, die ihr oft benutzt, bleibt Mending der Hauptweg.*

# Persönliche Upgrades

## Flächenernte {#personal.hoe_harvest}

Rechtsklick mit einer Hacke auf eine reife Pflanze erntet die Fläche drumherum auf derselben Höhe und pflanzt sofort nach. Das Saatgut kommt aus der Ernte, Glück auf der Hacke wirkt.

- Weizen, Karotten, Kartoffeln, Rote Bete
- Netherwarzen
- Kakao: Die Fläche steht senkrecht an der Stammseite, geerntet wird nur Kakao auf derselben Seite. Die Bohnen landen direkt im Inventar.

Nicht: Süßbeeren, Kürbisse, Melonen, Zuckerrohr, Fackellilien, Kannenpflanzen.

*Tipp: Eine Wand aus Tropenbaumstämmen, eine Seite voller Kakao – ein Klick erntet die ganze Plantage. Auch Netherwarzen-Felder auf Seelensand lohnen sich.*

## Reiche Ernte {#personal.harvest_bonus}

Chance auf doppelten Ertrag, wenn ihr eine reife Pflanze mit der Hacke erntet – beim normalen Abbauen und bei der Flächenernte.

- Dieselben Pflanzen wie bei der Flächenernte: Weizen, Karotten, Kartoffeln, Rote Bete, Netherwarzen, Kakao
- Weizenkörner und Rote-Bete-Samen werden nicht verdoppelt. Karotten, Kartoffeln, Netherwarzen und Kakaobohnen sind selbst das Saatgut und verdoppeln sich voll.
- Wirkt zusammen mit Glück.
- Glück bringt bei Weizen und Roter Bete – anders als in Vanilla – auch mehr Weizen bzw. Rote Bete, nicht nur mehr Samen.

## Mehr Wolle {#personal.wool_bonus}

Extra-Wolle mit jeder Schere, in der Farbe des Schafs. Nur, wenn ihr selbst schert – Werfer mit Schere bekommen keinen Bonus.

## Schnelles Wachstum {#personal.baby_growth}

Füttert ein Jungtier mit seinem Futter, und es wächst zusätzlich ein Stück – auf der letzten Stufe sofort. Gilt für alle Tiere, die sich füttern lassen: Kühe, Schweine, Schafe, Hühner, Pferde, Wölfe, Katzen, Bienen …

Nicht für Dorfbewohner und Kaulquappen.

*Tipp: Mit Landwirtschaft am Beacon wird aus einer kleinen Herde schnell eine große.*

## Baumfällen {#personal.tree_felling}

Schleichend mit der Axt fällt ein Stamm, und alle verbundenen Stämme derselben Sorte fallen mit – auch schräg verbunden. Jeder Stamm kostet Haltbarkeit.

- Alle Baumstämme und Holzblöcke, auch entrindet, dazu Karmesin- und Wirrstiele
- Nur genau dieselbe Blockart: Ein Eichenstamm nimmt keinen Birkenstamm mit
- Blätter bleiben stehen

*Tipp: Große Fichten und Tropenbäume lohnen sich jetzt richtig.*

## Vein Mining {#personal.vein_mining}

Schleichend mit der Spitzhacke fällt ein Erz, und die ganze Ader kommt mit – auch schräg verbunden. Jeder Block kostet Haltbarkeit.

- Alle Erze, auch Nethererze und Antiker Schutt
- Nur genau dieselbe Sorte: Eisenerz nimmt kein Tiefenschiefer-Eisenerz mit
- Die Spitzhacke muss das Erz abbauen können

## Abbaufläche 3×3 {#personal.area_mining}

Spitzhacke und Schaufel bauen 3×3 ab, quer zur angeklickten Seite: nach vorn eine Wand, nach unten eine Bodenfläche. Jeder Block kostet Haltbarkeit.

Mit fallen nur natürliche Blöcke derselben Gruppe wie der angeklickte, in beide Richtungen gleich:

- Spitzhacke: Gestein – Stein, Bruchstein, Granit, Diorit, Andesit, Tuffstein, Kalzit, Tropfsteinblock, Tiefenschiefer, Sandstein, Terrakotta, Netherrack, Basalt, Schwarzstein, Endstein
- Spitzhacke: Obsidian – nur Obsidian, eine eigene Gruppe
- Schaufel: Boden – Grasblock, Erde, Grobe Erde, Podsol, Myzel, Trampelpfad, Ackerboden, Schlamm, Lehm, Kies, Sand, Seelensand, Seelenerde, Schneeblock

Erze, Baublöcke wie Steinziegel, Treppen und Mauern sowie alles andere bleiben stehen. Ist der angeklickte Block selbst nicht in der Gruppe, baut nur er ab.

Schleichend an Erz oder Stamm gehen Vein Mining und Baumfällen vor.

## Haltbarkeit {#personal.unbreaking_bonus}

Zählt als zusätzliche Stufen der Verzauberung Haltbarkeit, auch über III hinaus, auf allem, was ihr tragt oder haltet:

- Rüstung und Elytren
- Werkzeuge, Waffen, Schild, Angel, Bogen … in Haupt- und Zweithand

Gegenstände im Inventar zählen nicht.

Werkzeuge und Elytren halten deutlich länger: Eine Diamant-Spitzhacke mit Haltbarkeit III hält etwa 6.200 Blöcke, mit Stufe 2 dieses Upgrades etwa 9.400. Rüstung profitiert kaum, Vanilla rechnet Haltbarkeit dort schwächer.

## Schutz {#personal.protection_bonus}

Jedes getragene Rüstungsteil zählt eine Stufe Schutz mehr, auch ohne Verzauberung. Vanilla deckelt den Schutz durch Verzauberungen insgesamt bei 80 %: Ein Set mit Schutz IV erreicht ihn mit diesem Upgrade genau.

## Glück+ {#personal.fortune_bonus}

Werkzeuge in der Hand zählen zusätzliche Stufen Glück, auch über das Maximum hinaus: Spitzhacke, Axt, Schaufel, Hacke.

- Mit Behutsamkeit wirkt es nicht.
- Wirkt auch bei Flächenernte, Vein Mining und Baumfällen.

## Plünderung+ {#personal.looting_bonus}

Zählt zusätzliche Stufen Plünderung für die Waffe in der Haupthand, auch über das Maximum hinaus.

- Nur Schwerter und Speere
- Nicht für Axt, Streitkolben, Dreizack, Bogen und Armbrust

## Angelschatz {#personal.fishing_treasure}

Chance je Fang auf einen Strukturfund: Töpferscherben, Rüstungsbesatz-Vorlagen, Herz des Meeres, Prüfungsschlüssel, verzauberte Bücher oder ein Silbersack.

Nur in offenem Wasser: Rund um den Schwimmer muss ein Feld von 5×5 Blöcken aus Wasser sein, darüber nur Luft oder Seerosenblätter. Aus einem 1×1-Loch gibt es keine Funde.

## Doppelschwimmer {#personal.double_bobbers}

Jeder Fang bringt zwei Items – überall, auch aus einem 1×1-Loch. In offenem Wasser können beide Schätze oder, mit Angelschatz, Strukturfunde sein.

## Rausch {#personal.rush}

Löst aus, wenn ihr ein Monster tötet, auch mit Pfeil oder Dreizack: Zombies, Skelette, Creeper, Spinnen, Schleime, Ghasts, Hoglins, Shulker, Phantome …

Tiere, Dorfbewohner und Spieler zählen nicht.

## Barriere {#personal.barrier}

Löst aus bei Treffern durch Monster, Spieler, Pfeile und Creeper-Explosionen und bei Feuer oder Lava.

Nicht bei Fallschaden, Ertrinken, Gift, Hunger oder Ersticken – so ist der Schild noch da, wenn der Kampf beginnt.

## Aufwind {#personal.updraft}

Nur Feuerwerksraketen, die ihr im Elytren-Flug zündet. Wirkt überall, auch außerhalb des Gebiets und in anderen Dimensionen.

## Doppelsprung {#personal.double_jump}

Ein zweiter Sprung in der Luft, einmal bis zur nächsten Landung. Nicht im Wasser, beim Klettern, beim Reiten, beim Fliegen und im Elytren-Flug.

## Fischernetz {#personal.fishing_net}

Fängt mit der Fähigkeitstaste alle Kabeljaue, Lachse, Tropenfische und Kugelfische, die im Umkreis von 3 Blöcken im Wasser schwimmen. Die Beute landet direkt im Inventar.

- Fische aus dem Eimer – etwa im Aquarium – bleiben verschont.
- Ohne Fisch in Reichweite passiert nichts, die Abklingzeit startet dann nicht.

## Quick Delivery {#personal.quick_delivery}

Mehr Bonus für schnelle Lieferanten, dafür weniger Geduld: 30 % statt 20 % Zeitbonus bei gleicher Bonusfrist. Danach fällt der Bonus weg, einen Tag lang gibt es den vollen Lohn, dann verliert der Contract 20 % statt 10 % je Tag, bis auf die Hälfte.

- Stufe 1: 130 % bis Tag 3, 100 % bis Tag 4, 80 % bis Tag 5, 60 % bis Tag 6, danach 50 %
- Gilt für alle eigenen Contracts und lässt sich nicht abschalten.

*Tipp: Nur für Spieler, die ihre Contracts sicher innerhalb der Bonusfrist schaffen – einen Tag nach ihrem Ende zahlt ein Contract ohne Quick Delivery mehr.*

## Auftragsuhr {#personal.contract_clock}

Fahrt im Inventar mit der Maus über eine Uhr: Der Tooltip zeigt jeden angenommenen Contract mit Lieferstand, aktuellem Wert und der Zeit bis zum nächsten Abfall, etwa „Karotte: 23 / 64, 120%, 0.6 d“.

Schleichen + Rechtsklick mit der Uhr auf eine Marktwaage bindet die Uhr an deren Item. Dann zeigt sie zusätzlich dessen Marktpreis, etwa „Weizen: 12 T, 85%“. Für ein anderes Item bindet ihr neu; eine leere Waage löst die Bindung.

## Fernlieferung {#personal.remote_delivery}

Schleichen + Rechtsklick mit der Uhr in die Luft liefert passende Items aus Inventar und Hotbar an eure Contracts – überall im Gebiet, ohne Board. Rüstung und Zweithand bleiben unberührt.

Der Lohn landet im Inventar. Getragene Taler gehen beim Tod verloren, also bald einzahlen.

- Abklingzeit: 5 Sekunden
- Braucht die Auftragsuhr

# Vorsicht, Tiefe!

## Dunkelheit in der Tiefe {#depth warning}

Unterhalb der Deepslate-Grenze umgibt euch Dunkelheit. Verboten ist dort nichts – angenehm aber auch nicht.

Am Beacon lässt sich die Tiefe freischalten.

Auch ein Nether-Portal bleibt dunkel, bis ihr den Nether am Beacon freischaltet.

# Gerüchte

## Ein Gerücht {#rumors}

Viele Taler an einem Ort bleiben nicht lange unbemerkt.

Man erzählt sich von einem Händler, der vorbeischaut, wenn sich genug in euren Einzahlkisten angesammelt hat – und der Dinge verkauft, die es auf keinem Markt gibt.
