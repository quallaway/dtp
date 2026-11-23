# Handelsliste — Bewertungsverfahren

> Wie ein Item bewertet wird: ob es auf den Markt kommt, in welche Datei und mit welchen `base`, `H` und `T`.
> Grundlagen der Handelsliste (Format, gemeinsame Ware, Items mit Komponenten) stehen in [Markt](10-markt.md), die Preisformel in [Marktdruck](11-marktdruck.md), die Bänder je Warenklasse in [Preise und Größenordnungen](61-preise-und-groessenordnungen.md).

---

## Grundsatz

**Ein Eintrag pro Produktionsweg.**

Jedes verkaufbare Item steht für eine eigene Tätigkeit oder Quelle: einen Baum, ein Erz, einen Mob, einen Fund. Zwei Items, die aus derselben Quelle mit derselben Arbeit entstehen, sind *eine* Ware. Entweder kommt nur eines davon auf die Liste, oder alle teilen sich über `key` einen Marktdruck. Sonst entsteht Vielfalt ohne Aufwand, und die Marktsättigung läuft ins Leere.

Daraus folgt die zweite Achse: **Je weniger Handarbeit ein Item braucht, desto schneller sättigt es.** Nicht verboten, aber eingepreist.

---

## Das Verfahren in vier Schritten

Jedes Item durchläuft die Schritte der Reihe nach. Fällt es in Schritt 1 heraus, ist die Bewertung beendet.

1. **Ausschlussfilter** — ist das Item eine Variante, ein Verarbeitungsprodukt oder technisch ungeeignet?
2. **Arbitrage-Prüfung** — lässt es sich aus anderen verkaufbaren Items herstellen, kopieren oder bei Dorfbewohnern kaufen?
3. **Warenklasse** — wie viel Handarbeit steckt in einer Einheit, wie leicht lässt es sich farmen? Daraus folgen Datei und Band ([Warenklassen](61-preise-und-groessenordnungen.md#warenklassen)).
4. **Parameter festlegen** — `base` relativ zu Vergleichswaren, `H` und `T` aus der Ausbeute einer Referenzanlage.

---

## Schritt 1: Ausschlussfilter

Ein Item kommt **nicht** auf die Liste, wenn eine dieser Regeln greift.

| # | Regel | Beispiele | Ausnahme |
|---|---|---|---|
| A1 | **Färbe- und Mustervarianten.** Entsteht das Item aus einem gelisteten Item plus Farbstoff, Muster oder Glasur. | Kerzen, Beton, Betonpulver, Glas, Terrakotta, Teppiche, Betten, Banner | Wolle: alle 16 Farben als gemeinsame Ware `dtp:wool` zum Preis der weißen |
| A2 | **Crafting-Permutationen.** Bauformen aus einem Rohstoff. | Bretter, Stufen, Treppen, Zäune, Türen, Schilder, Boote, Truhen, polierte und gemeißelte Steine, Ziegel | — |
| A3 | **Komprimierung.** Umkehrbare 9:1- oder 4:1-Rezepte. | Eisenblock, Rohe-Eisen-Block, Goldnugget, Honigblock, Heuballen, Schleimblock, Knochenblock | Nur die Grundform wird gehandelt |
| A4 | **Bonemeal-Selbstvermehrung.** Bonemeal auf das Item selbst erzeugt eine Kopie, ohne Wachstumszeit. | Zweiblock-Blumen, hohes Gras, großer Farn, einfache Blumen, lebende Korallen und Korallenfächer | Eigener Produktionsweg mit Ernte-Aufwand: Pilze stehen auf der Liste, aber billig |
| A5 | **Ausrüstung und Verzauberung.** Werkzeuge, Waffen, Rüstung, Pferderüstung, verzauberte Bücher, Elytren. | — | Nur über Contracts ([Contracts](12-contracts.md)); ausgediente Ausrüstung ohne Ofenverwertung nimmt der [Recycler](19-recycler.md) als Schrott |
| A6 | **Technik- und Kreativ-Items.** Nicht im Überlebensmodus erhältlich. | Spawn-Eier, Befehlsblöcke, Barriere, Strukturblock, Licht, Grundgestein, Spielerkopf | — |
| A7 | **Verarbeitungsstufe.** 1:1-Verarbeitung (Schmelzen, Kochen) eines gelisteten Items. | Holzkohle, gebratenes Fleisch, Glas, Stein, getrockneter Seetang, abgestorbene Korallen | Metalle: gehandelt wird der **Barren**, nicht die Rohform (siehe unten) |
| A8 | **Nebenprodukt ohne eigenen Weg.** Fällt zwangsläufig beim Ernten eines gelisteten Items an. | Setzlinge, Laub, Samen, Stöcke | — |
| A9 | **Natürliche Steinvarianten aus demselben Abbau.** Werden beim gleichen Graben mitgenommen wie ein gelistetes Item. | Granit, Diorit, Andesit, Tuff, Kalzit, Bruchtiefenschiefer | Höchstens ein Vertreter je Gewinnungsart: Bruchstein, Netherrack |

**Natürliche Arten sind keine Varianten**, aber sie teilen sich oft einen Markt. Die Stammarten der Oberwelt kommen alle aus derselben Baumfarm mit derselben Arbeit und bilden die gemeinsame Ware `dtp:wood_log`. Karmesin- und Wirrstiel wachsen im Nether und haben einen eigenen Eintrag. Die fünf Korallenblöcke bilden `dtp:coral_block`, die drei Froschlichter `dtp:froglight`. Die drei Hühnervarianten legen gleichwertige Eier, Hühnerei, blaues und braunes Ei bilden `dtp:egg`. Gefärbte Wolle kommt vom selben Schaf mit demselben Farbstoff: gleicher Preis, ein Marktdruck. Färben bringt so keinen Gewinn, und es gibt nicht 16 Märkte zum Leerverkaufen.

### Barren statt Rohform

Metalle kommen aus zwei Wegen: Bergbau (Rohform) und Farmen (Eisengolem, zombifizierte Piglins, Ertrunkene). Gehandelt wird **eine Form je Rohstoff**, der Barren. Rohes Eisen, Kupfer und Gold stehen nicht auf der Liste. Bergbau und Farm landen damit im selben Markt, und die Farm drückt den Preis, den auch der Bergmann bekommt. Antiker Schrott und Netheritplatte bilden eine Kette ohne zweiten Weg; gelistet ist `netherite_scrap`.

### Korallen

Nur die **Korallenblöcke** stehen auf der Liste. Sie lassen sich nur mit Behutsamkeit abbauen und nicht vermehren. Lebende Korallen und Korallenfächer wachsen per Bonemeal unter Wasser nach (A4), abgestorbene Formen entstehen aus den lebenden (A7).

---

## Schritt 2: Arbitrage-Prüfung

Lässt sich ein Item aus anderen verkaufbaren Items herstellen oder eintauschen, umgeht der Spieler die Sättigung des Ausgangsitems, indem er umwandelt. **Craften erzeugt keinen Wert.**

| Fall | Regel | Beispiel |
|---|---|---|
| **Craftbar aus Gelistetem** | Nicht listen (A2/A7). Steht das Ergebnis trotzdem auf der Liste, weil es einen eigenen Weg hat, liegt sein Ruhepreis unter dem Wert der Zutaten | Wolle aus 4 Fäden: Wolle 20, 4 Fäden 140. Kaninchenfell wird zu Leder, nur Leder ist gelistet |
| **Duplizierrezept** | Ruhepreis deutlich unter dem Wert der Zutaten zum Ruhepreis, dazu eine gemeinsame Ware mit kleinem `H` | Rüstungsbesätze und Netherit-Vorlage lassen sich mit 7 Diamanten kopieren (7 × 1.000). Alle 19 Vorlagen sind eine Ware `dtp:armor_trim` mit Ruhepreis 1.500 und `H` 1: Kopieren lohnt höchstens einmal je Erholung, nicht 19-mal |
| **Bei Dorfbewohnern kaufbar** | Bepreisen wie Farmware, Kapazität begrenzen | Eigener Abschnitt: [Dorfbewohnerhandel](#dorfbewohnerhandel) |

---

## Dorfbewohnerhandel

Dorfbewohner sind ein Umrechnungsautomat: Billiges rein, Smaragd raus, Smaragd rein, Ware raus. Bewertet wird immer der **Extremfall**, nicht der Normalfall. Eine Gruppe, die den Markt kennt, baut genau diesen Extremfall.

| Annahme | Wirkung |
|---|---|
| **Maximale Reputation.** Mehrfach geheilte Zombie-Dorfbewohner plus Held des Dorfes | Der Rabatt senkt das erste Preis-Item eines Handels bis auf 1 |
| **Kostenlose Smaragdquelle.** Der Pfeilmacher kauft Stöcke, der Bibliothekar Papier — beides steht nicht auf der Liste | Ein Smaragd kostet praktisch nichts |
| **Handelshalle.** Beliebig viele Dorfbewohner, jeder Handel mehrfach täglich aufgefüllt | Jede Verkaufsware ist eine Farm mit hoher Rate |

Folge: **Jedes Item, das ein Dorfbewohner verkauft, ist im Extremfall gratis und unbegrenzt.** Begrenzt wird es nur durch die Marktsättigung. Maßgeblich ist deshalb nicht der Gleichgewichtspreis, sondern die [Marktkapazität](#marktkapazität): wie viel Taler ein frischer Markt hergibt, bevor er gesättigt ist.

**Händlerware:** Seit v6 trägt alles, was ein Händler herausgibt, die Markierung `dtp:merchant_goods` und ist unverkäuflich ([Händlerware](10-markt.md#händlerware)). Der Extremfall trifft damit nur noch Waren, deren Markierung sich waschen lässt: platzierbare Blöcke, die sich selbst wieder abbauen lassen (Wolle, Redstone), und Rezepte aus Händlerware-Zutaten. Für alle anderen Dorfbewohner-Waren (Lapis, Glowstone-Staub, Enderperle, Apfel) sind D1 und D2 nicht mehr nötig; ihre Neubewertung steht aus ([TODO v6](95-todo-v6.md)). Neue Einträge müssen die Pool-Prüfung `merchant_goods_pool` bestehen.

Daraus folgen drei Regeln:

- **Smaragd wird nicht gehandelt.** Er ist das Endprodukt jedes Ankauf-Handels und kostet im Extremfall nichts.
- **D1 — Bepreisen wie Farmware.** Ein Item, das ein Dorfbewohner verkauft, bekommt höchstens die Werte einer farmbaren Klasse, egal wie mühsam sein natürlicher Weg ist. Betroffen sind Redstone, Lapis und Glowstone-Staub (Geistlicher, Klasse B2 am unteren Rand), Enderperle (Geistlicher, B3), Apfel (Bauer, B3 mit kleinem `H`) und Wolle (Schäfer, A3).
- **D2 — Kapazität begrenzen.** Die Kapazität einer Dorfbewohner-Ware liegt nicht über der einer Feldfrucht. Richtwert: Weizen, ≈ 25.000 Taler. Die Enderperle liegt bei ≈ 11.000, Redstone bei ≈ 2.300.

Was Dorfbewohner **nur ankaufen**, ist unbedenklich: Gold, Leder, Weizen, Kohle, Fisch, Fleisch, Fäden, Federn. Für diese Items ist der Dorfbewohner nur eine weitere Smaragdquelle, und Smaragde sind wertlos.

Pfeile, Feuerstein und Fahnenvorlage „Globus“ stehen nicht auf der Liste (A2 bzw. Dorfbewohner-Ware ohne eigenen Weg).

---

## Schritt 3: Warenklasse

Die Klasse beschreibt, **wie leicht sich eine Einheit beschaffen lässt**: wie viel Handarbeit drinsteckt und ob eine Farm sie liefert. Ausschlaggebend ist der leichteste Weg im Extremfall (Farm, Generator, Dorfbewohner), nicht der übliche. Die zehn Klassen A1–C3 mit ihren Bändern für Ruhepreis, `H` und `T` stehen in [Preise und Größenordnungen](61-preise-und-groessenordnungen.md#warenklassen).

Grob:

- **A** — Landwirtschaft und Massenware. Großes `H`, kurzes `T`, Preis in Kupfertalern.
- **B** — sammelbar und farmbar: Monsterbeute, Fisch, Erze, Metalle. Mittleres bis kleines `H`, `T` 4–10.
- **C** — Fundstücke, nicht oder kaum farmbar. Hoher Preis, `H` 1–10.

---

## Schritt 4: Parameter festlegen

### `base` — der Aufwand

`base` misst den Aufwand für **eine** Einheit bei neutralem Markt. Der Ruhepreis, den der Spieler beim ersten Verkauf sieht, ist `1,41 × base` ([Marktdruck](11-marktdruck.md)). Neue Items werden **relativ** zu bestehenden Einträgen derselben Klasse eingeordnet: „Wie viele Weizen ist ein X an Arbeit wert?“ Anker:

| Anker | `base` | Ruhepreis |
|---|---|---|
| Bruchstein | 0,71 | 1 |
| Weizen | 6,25 | 8,8 |
| Eisenbarren | 56,6 | 80 |
| Diamant | 707 | 1.000 |
| Antiker Schrott | 15.000 | 21.200 |

Weitere Anker je Klasse: [Ankerwaren](61-preise-und-groessenordnungen.md#ankerwaren). Gerundet wird auf drei signifikante Stellen.

### `H` und `T` — aus der Referenzanlage

Für jedes Item schätzt man die **Referenzrate `r`**: wie viele Einheiten pro Markttag eine typische Anlage (oder ein Spieler beim Abbau) zur Mitte eines Durchlaufs liefert. Ein Markttag sind 20 Minuten gespielte Zeit.

Liefert die Anlage dauerhaft, pendelt sich der Marktdruck ein bei

```
s* = sFloor + r · T / ln 2
```

und der Preisfaktor bei

```
f* = 2 ^ (floorRatio − 1,44 · r · T / H)
```

Umgestellt nach `H` für ein gewünschtes Gleichgewicht `f*` (mit `floorRatio = 0,5`):

```
H = 1,44 · r · T / (0,5 − log2 f*)
```

| Ziel `f*` | `H ≈` | typisch für |
|---|---|---|
| 0,1 | 0,38 · r · T | Ware, die eine Farm fast beliebig liefert |
| 0,5 | 0,96 · r · T | Farmware (A2, A3) |
| 0,7 | 1,42 · r · T | Kleinfarm, mühsame Farmware |
| 1,0 | 2,9 · r · T | Handarbeit, die bei normalem Abbau kaum sättigt |

**`T` wählt man zuerst**, nach dem gewünschten Rhythmus. Die Handelsliste nutzt drei Stufen:

- **`T` 1–2** — Massenware und Feldfrüchte erholen sich innerhalb einer Sitzung (eine Sitzung von 3,5 Stunden sind gut 10 Markttage).
- **`T` 4** — der Normalfall. Ein Abverkauf ist nach einer Sitzung weitgehend erholt.
- **`T` 10** — wertvolle Rohstoffe, Trophäen, Scherben. Ein großer Fund drückt den Preis über mehrere Sitzungen.

Die Marktuhr läuft nur, während Spieler online sind. `T` ist deshalb in gespielter Zeit zu lesen, Pausen zwischen Sitzungen erholen nichts.

**Beispiel Weizen:** `T` 2, Ziel 0,5, `H` 2000 → die Referenzfarm liefert `r` ≈ 2000 / (0,96 · 2) ≈ 1.040 Stück pro Markttag.

`H` gilt für einen Spieler. Die Spielerskalierung (`market.playerScaling`) und das Server-Upgrade *Preisstabilität* vergrößern es zusätzlich, *Preiserholung* verkürzt `T` ([Marktdruck](11-marktdruck.md)).

### Marktkapazität

**Die Kapazität `C` ist die Summe an Talern, die ein frischer Markt hergibt**, wenn man vom Ruhezustand (`sFloor`, Faktor 1,41) verkauft, bis der Preis an der Untergrenze `factorMin` liegt:

```
C = base · H · (2^floorRatio − factorMin) / ln 2  ≈  1,97 · base · H  ≈  2 · base · H
```

Die Hälfte davon (≈ `base · H`) bringt der Abverkauf bis zum halben Ruhepreis. Danach bringt jede weitere Einheit nur noch `base × factorMin`. `H` ist hier wie oben das effektive `H` nach Spielerskalierung und Upgrades.

Die Kapazität ist die richtige Kennzahl für Waren, die **gratis und unbegrenzt** verfügbar sind (Dorfbewohnerhandel, große Farmen): Dort entscheidet nicht der Gleichgewichtspreis, sondern wie viel man beim ersten Abverkauf mitnimmt. Für laufende Lieferung gilt dagegen: Das höchste Dauereinkommen aus einer Ware liegt bei `r = H / T` und beträgt etwa `0,5 · base · H / T` je Markttag. Mehr liefern drückt den Preis stärker, als die Menge ausgleicht.

| Item | `base` | `H` | `C` |
|---|---|---|---|
| Weizen | 6,25 | 2000 | ≈ 24.600 |
| Enderperle | 141 | 40 | ≈ 11.100 |
| Diamant | 707 | 20 | ≈ 27.800 |
| Rüstungsbesatz (gemeinsame Ware) | 1.061 | 1 | ≈ 2.100 |

---

## Dateien

Alle JSON-Dateien unter `data/<ns>/dtp/trade_list/` werden zusammengeführt. Die Datei richtet sich nach dem Thema:

| Datei | Inhalt |
|---|---|
| `10_agriculture.json` | Pflanzen und Tierprodukte: Feldfrüchte, Pilze, Honig, Wolle, Eier, Fleisch, Froschlichter |
| `20_fishing.json` | Fischerei und Meer: Fische, Korallenblöcke, Seegurken, Seetang, Tintenbeutel, Nautilusschale, Namensschild |
| `30_resources.json` | Holz, Steine, Ton, Schlamm, Erze, Metalle |
| `40_mobs.json` | Mob-Drops: Knochen, Fäden, Schwarzpulver, Lohenruten, Membranen, Totem |
| `50_loot.json` | Fundstücke: Schallplatten, Besätze, Scherben, Banner, Monsterköpfe, Sonderfunde |

**Jedes Item steht in genau einer Datei** und höchstens einmal. Varianten desselben Items (Ziegenhörner, Stufen der Unheilvollen Flasche) lassen sich deshalb nicht einzeln bepreisen; handelbar ist nur, was ein einzelner Eintrag mit `components`/`predicates` beschreibt, etwa das Böse Banner ([Markt](10-markt.md)).

Gleichartige Waren teilen sich über `key` einen Marktdruck. `h` und `t` gelten dann für die ganze Ware und kommen vom ersten Eintrag des Keys.

Tags (`"tag": "minecraft:…"`) nur verwenden, wenn ihr Inhalt geprüft ist — ein Tag kann nachträglich um Items wachsen, die nicht auf die Liste sollen.

Bearbeiten lässt sich die Liste auch als Tabelle: `scripts/trade_list_to_csv.py` exportiert sie, `scripts/csv_to_trade_list.py` schreibt sie zurück ([scripts/README.md](../scripts/README.md)).
