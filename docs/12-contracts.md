# Contracts

> Contracts sind die zweite Einnahmequelle neben dem freien Verkauf. Sie geben dem Spieler eine Richtung: etwas, das man sich vornimmt, auf das man hinarbeitet und das man im Idealfall gemeinsam stemmt.

---

## Rolle

Freier Verkauf allein führt dazu, dass der Spieler einmal das lukrativste Item ermittelt, dafür eine Farm baut und denselben Vorgang bis zum Spielende wiederholt. Der Marktdruck ([Marktdruck](11-marktdruck.md)) bremst das, sagt aber nur, was gerade *nicht* lohnt.

Contracts geben die fehlende Richtung. Sie sind der einzige aktive Vielfaltsmechanismus des Mods: Es gibt keine Tagesboni, keine Erstverkaufsboni und keine Nachfragewürfel.

---

## Aufbau eines Contracts

```
Stufe 2   Karotten                     0 / 560
          Grundwert 6.569 Taler
          120 % · −20 % in 2,2 Tagen
```

| Feld | Bedeutung |
|---|---|
| Stufe | Größenklasse, bestimmt Menge und Belohnung je Item |
| Forderung | Genau ein Item (oder eine Warengruppe) mit Menge, bei Stufe 4 mit Mindestbedingung |
| Grundwert | Belohnung bei normaler Erfüllung, beim Erzeugen fixiert |
| Zeitbonus | +10 % bei Erfüllung innerhalb der Bonusfrist |
| Verfall | Mit dem Ende der Frist fällt der Bonus weg; nach einem Intervall zu 100 % sinkt der Wert je weiteres Intervall um 10 % des Grundwerts, bis auf 50 % |
| Fortschritt | Bereits gelieferte Menge |

Die Oberfläche zeigt immer den **aktuellen Wert in Prozent und die Zeit bis zur nächsten Stufe**: innerhalb der Bonusfrist „120 % · −20 % in 2,2 Tagen“, danach „100 % · −10 % in 0,6 Tagen“ und so weiter bis „50 % (Minimum)“. Der erste Schritt ist der ganze Bonus. Hat der Spieler Quick Delivery, zeigt sie die Werte mit seinen Bedingungen.

---

## Zeit als Bonus, nicht als Fallbeil

**Ein Contract läuft nicht ab.** Er wird nur mit der Zeit weniger wert.

```
innerhalb der Frist:  Belohnung = Grundwert × bonusFactor
                                = Grundwert × 1,2
nach der Frist:       Belohnung = Grundwert × max( minFactor , 1 − decayPerInterval × volleIntervalle )
                                = Grundwert × max( 0,5 , 1 − 0,1 × volleIntervalle )
```

Mit dem Ende der Bonusfrist fällt der Bonus ganz weg. Gezählt werden nur volle Intervalle nach der Frist: Das erste Intervall danach gilt also 100 %. Ein Stufe-1-Contract (Frist 3 Tage, Intervall 1 Tag) steht bis Tag 3 auf 120 %, bis Tag 4 auf 100 %, bis Tag 5 auf 90 % und ab Tag 8 auf dem Minimum von 50 %.

Warum diese Form:
- **Zeitdruck bleibt.** Wer schnell liefert, bekommt spürbar mehr.
- **Langsame Spieler werden nicht ausgeschlossen.** Wer nebenbei spielt, verdient weniger, aber er verdient.
- **Kein Teillieferungsproblem.** Es gibt keinen Moment, in dem 1.800 gelieferte Karotten ersatzlos verfallen.
- **Keine Strafe für Scheitern nötig**, weil es kein Scheitern gibt.

**Die Bonusfrist beginnt mit dem Erzeugen des Angebots**, nicht mit der Annahme. Sonst ließe sich Ware vor der Annahme in Ruhe vorbereiten.

### Zwei Uhren

Gemessen wird in **Minecraft-Tagen**, Schlafen zählt mit. Wer die Nacht überspringt, bringt den Markt näher an seine Erholung, aber auch den laufenden Contract näher an seinen Wertverfall.

- **Aushang:** Angebote und Rotation laufen auf der serverweiten Contract-Uhr. Sie läuft **nur, während mindestens ein Spieler online ist**. Ein gestoppter, pausierter oder leerer Server lässt den Aushang nicht altern.
- **Angenommene Contracts:** Jeder Spieler hat eine **persönliche Contract-Uhr**, die nur läuft, während er selbst online ist. Spielen nur die anderen, behält sein Contract seinen Wert.

Beim Annehmen wechselt der Contract vom Aushang auf die persönliche Uhr, beim Abbrechen zurück, jeweils mit dem bereits verstrichenen Alter. Die Bonusfrist lässt sich durch Abbrechen also nicht zurücksetzen.

---

## Belohnung

**Grundwert = Menge × Basiswert × Multiplikator der Stufe** (`contracts.rewardMultiplier`, 2,0 / 2,3 / 2,75 / 3,5). Vorlagen mit festem `reward_per_item` nutzen den Multiplikator nicht.

- **Über dem Marktwert.** Sonst gäbe es keinen Grund, den Auftrag dem freien Verkauf vorzuziehen. Höhere Stufen zahlen je Item mehr, weil sie eine Farm, ein umgebautes Feld oder die ganze Gruppe verlangen.
- **Beim Erzeugen fixiert**, nicht laufend am Markt berechnet. Wer 2.000 Karotten liefert, würde bei marktgekoppelter Belohnung seinen eigenen Preis während der Lieferung ruinieren.
- **Kein Marktdruck.** Die Lieferung an einen Contract drückt den Preis nicht. Contracts entlasten den Markt, statt ihn zu belasten.
- **Nur Taler**, keine Items. Jede Itembelohnung öffnet die Exploit-Frage neu ([Markt](10-markt.md)).

Jeder Abschluss zählt außerdem:
- in den **serverweiten Zähler** abgeschlossener Contracts, der weitere Stufen freischaltet,
- als Verkürzung der Wartezeit bis zum nächsten Marktereignis (`events.contractMinutes`, 5 / 15 / 30 Minuten für Stufe 1 / 2 / 3, Stufe 4 wie Stufe 3; siehe [Marktdruck](11-marktdruck.md)).

Contracts sind Daten am Spieler und **überleben den Tod**. Verloren gehen nur die getragenen Taler, nie der Auftragsfortschritt.

---

## Stufen

| Stufe | Charakter | Beacon-Upgrade „Contract-Stufe“ | Abgeschlossene Contracts (Server) | Bonusfrist | Wertziel | Multiplikator |
|---|---|---|---|---|---|---|
| 1 | **Startfreundlich**, von Hand sammelbar | 1.000 | 0 | 3 Tage | 800 | 2,0 |
| 2 | **Farmfreundlich**, praktisch nur mit einer kleinen Farm | 12.000 | 5 | 6 Tage | 2.800 | 2,3 |
| 3 | **Kollaborationsfreundlich**, ein großes Feld wird umgebaut, die ganze Gruppe sammelt oder mehrere Farmen beliefern eine Verarbeitung | 40.000 | 25 | 9 Tage | 7.000 | 2,75 |
| 4 | **Exotisch**: verzauberte Ausrüstung, Elytren, Dreizacke | 100.000 | 60 | 12 Tage | 30.000 | 3,5 |

Konfiguriert über `contracts.tierRequirement`, `contracts.bonusDays`, `contracts.tierValue` und `contracts.rewardMultiplier` (Index 0 = Stufe 1). Die Preise stehen in `data/dtp/dtp/upgrade/contract_tier.json`.

Im Mittel bringt ein Contract mit Zeitbonus ≈ 1.760 (Stufe 1), ≈ 7.080 (Stufe 2) und ≈ 21.180 Taler (Stufe 3). Jede Stufe zahlt also etwa das 4- bzw. 3-Fache der Stufe darunter, je Frist-Tag etwa das Doppelte (590 / 1.180 / 2.350). Die Sprünge sind bewusst kleiner als die Upgradepreise: Ein einzelner Contract der Stufe 3 finanziert kein großes Upgrade, er ist ein besserer Ertrag je Slot. Das hält Spieler länger bei ihren Spezialisierungen. Wertziel und Multiplikator von Stufe 4 greifen nur für Vorlagen ohne feste Menge und ohne festen Lohn; alle mitgelieferten Stufe-4-Vorlagen haben beides fest.

### Freischaltung pro Server

Freigeschaltet wird pro Server, nicht pro Spieler. Vor dem ersten Kauf am Beacon (`contracts.baseMaxTier` = 0) gibt es keinen Aushang: Das Frühspiel läuft über den Markt, Contracts sind das erste gemeinsame Ziel. Jede Stufe kostet Taler und setzt serverweit abgeschlossene Contracts voraus; alle Spieler tragen dazu bei. Der Kauf würfelt den Aushang sofort neu. Das Upgrade gehört zum Markt-Tab des Beacons ([Territorium](20-territorium-und-serverweite-effekte.md)).

**Warum Stufen statt Fortschrittsskalierung:** Was „Spielerfortschritt“ messen soll, ist unklar, und eine falsche Schätzung erzeugt unmögliche oder triviale Aufträge. Abgeschlossene Contracts sind ein direkter Nachweis: Wer liefern konnte, kann nachweislich liefern.

---

## Die Contract-Liste

Gefordert wird nur aus einer **kuratierten Liste** (`data/<ns>/dtp/contract/*.json`), nicht aus allem, was handelbar ist. Hier dürfen auch Dinge stehen, die am freien Markt nicht verkäuflich sind, etwa verzauberte Ausrüstung; dann ist `reward_per_item` Pflicht.

| Feld | Bedeutung |
|---|---|
| `item` | Gefordertes Item, bei `key` nur Symbol und Basiswert |
| `tier` | Eine Stufe oder eine Liste (`[1, 2, 3]`) |
| `min_count`, `max_count` | Feste Menge. Fehlen beide, folgt die Menge aus dem Wertziel, bei `reward_per_item` ist sie 1 |
| `reward_per_item` | Fester Lohn je Stück statt Basiswert × Multiplikator |
| `weight` | Gewicht bei der Auswahl, Standard 1 |
| `key` | Marktschlüssel einer gemeinsamen Ware der Handelsliste, z. B. `dtp:wood_log` |
| `unlock` | Freischaltung wie bei den Marktkategorien: `dimension` oder `dragon_killed` |
| `components`, `predicates` | Mindestbedingung an das Item im Vanilla-Format |
| `random_enchantments` | Beim Aushängen gewürfelte Verzauberungen, siehe Stufe 4 |
| `random_potion` | Beim Aushängen gewürfelter Trank aus der Tranktabelle, siehe [Tränke](#tränke-stufe-3) |

### Menge aus dem Wertziel

Ohne feste Menge liegt der Marktwert der geforderten Menge zum Basiswert bei `contracts.tierValue` mit ± `contracts.tierValueSpread` (30 %). Gerundet wird auf glatte Zahlen: unter 64 auf Vielfache von 4, darüber auf Vielfache von 16. Teure Waren werden so in kleinerer Zahl gefordert, billige in größerer, und dieselbe Ware wächst mit der Stufe:

| Ware | Basiswert | Stufe 1 | Stufe 2 | Stufe 3 |
|---|---|---|---|---|
| Kartoffeln | 5,1 | 112–208 | 384–720 | 960–1.792 |
| Holzstämme | 7,84 | 64–128 | 256–464 | 624–1.168 |
| Eisenbarren | 56,6 | – | 36–64 | 80–160 |

### Warengruppen: Holzstämme, Wolle und Eier

Nennt die Vorlage einen `key`, zählt jedes handelbare Item dieser gemeinsamen Ware ([Markt](10-markt.md)):
- **Holzstämme** (`dtp:wood_log`): jede Stammart, auch gemischt. Entrindete Stämme, Holzblöcke und Bretter gehören nicht dazu. Der Tooltip nennt „Holzstämme (beliebige Art)“, das Symbol wechselt reihum durch die Arten.
- **Wolle** (`dtp:wool`): jede der 16 Farben, auch gemischt. Teppiche und Betten zählen nicht. Der Tooltip nennt „Wolle (beliebige Farbe)“.
- **Eier** (`dtp:egg`): Hühnerei, blaues und braunes Ei, auch gemischt. Schildkröten- und Schnüffler-Ei zählen nicht. Der Tooltip nennt „Eier (beliebige Hühnerart)“.

Die Gruppe gilt nur, wo die Vorlage sie ausdrücklich nennt.

### Melonenblock

Der Melonenblock steht nicht auf der Handelsliste. Seine Vorlagen haben deshalb feste Mengen und festen Lohn:

| Stufe | Menge | Lohn je Block |
|---|---|---|
| 2 | 96–192 | 43,9 |
| 3 | 256–480 | 52,5 |

Der Lohn ist 9 × Basiswert der Melonenscheibe (2,12) × Multiplikator (2,3 / 2,75). Ein Block aus 9 gecrafteten Scheiben bringt damit genau so viel wie 9 Scheiben im Scheiben-Contract (Stufe 1 und 2). Craften lohnt nicht, Behutsamkeit spart Ernten. Duplizieren geht nicht: Ein abgebauter Block gibt höchstens 9 Scheiben.

**Pflege:** Ändert sich der Basiswert der Scheibe oder `contracts.tierValue`, neu rechnen: Menge = `tierValue / (9 × base)` ± 30 %, Lohn = `9 × base × rewardMultiplier`.

### Gecraftete Waren (Stufe 3)

Stufe 3 soll nicht nur mehr vom Gleichen fordern. Etwa die Hälfte der Auswahl (Gewicht 2 statt 1) sind deshalb **gecraftete Waren**, die mehrere Farmen gleichzeitig brauchen. Hier gilt der Grundsatz „nur Rohstoffe, keine Crafting-Permutationen“ des Marktes bewusst nicht: Ein Contract kauft keine Ware frei, er fordert eine Produktionskette.

| Ware | Zutaten | Zutatenwert | Menge | Lohn je Stück |
|---|---|---|---|---|
| Kuchen ¹ | 3 Milch, 2 Zucker, Ei, 3 Weizen | 49,2 | 96–160 | 185 |
| Kaninchenragout ¹ | Gebratenes Kaninchen, Karotte, Ofenkartoffel, Pilz, Schüssel | ≈ 46 | 96–160 | 177 |
| Pilzsuppe ¹ | Brauner und roter Pilz, Schüssel | ≈ 11,5 | 96–160 | 82 |
| Rote-Bete-Suppe ¹ | 6 Rote Bete, Schüssel | ≈ 39 | 96–160 | 157 |
| Buch und Feder ¹ | Buch, Tintenbeutel, Feder | 108 | 44–80 | 347 |
| Lore ¹ | 5 Eisenbarren | 283 | 16–32 | 828 |
| Kürbiskuchen | Kürbis, Zucker, Ei | 20,9 | 240–432 | 57,6 |
| Keks | 2 Weizen, Kakao → 8 | 2,47 | 1.984–3.680 | 6,8 |
| Brot | 3 Weizen | 18,75 | 256–480 | 51,6 |
| Ofenkartoffel | Kartoffel, Brennstoff | ≈ 5,6 | 880–1.632 | 15,4 |
| Goldene Karotte | Karotte, 8 Goldnuggets | 80,6 | 60–112 | 222 |
| Goldener Apfel ² | Apfel, 8 Goldbarren | 750,2 | 8–12 | 2.063 |
| Feuerwerksrakete | Papier, Schwarzpulver → 3 | 16,4 | 304–560 | 45,1 |
| Buch | 3 Papier, Leder | 50,7 | 96–176 | 139 |
| Fermentiertes Spinnenauge | Spinnenauge, Pilz, Zucker | 38,4 | 128–240 | 106 |
| Leine | 4 Fäden, Schleimball → 2 | 77,7 | 64–112 | 214 |
| Getrockneter Seetangblock | 9 Seetang, Brennstoff | ≈ 7,4 | 656–1.232 | 20,4 |
| TNT | 5 Schwarzpulver, 4 Sand | ≈ 224 | 20–40 | 616 |

- **Zutatenwert** = Summe der Basiswerte der Zutaten. Ohne Listenpreis geschätzt: Milch 5 je Eimer, Sand 1, Schüssel und Brennstoff zusammen etwa 1–2, Brennstoff für ein einzelnes Schmelzgut etwa 0,5.
- **Menge** = 7.000 / Zutatenwert ± 30 %, **Lohn** = Zutatenwert × 2,75 — wie jede Stufe-3-Ware (`contracts.tierValue` und `contracts.rewardMultiplier` von Stufe 3).
- **¹ Nicht stapelbar:** Jedes Stück belegt einen Platz. Die Menge ist deshalb auf höchstens 160 begrenzt (96–160, wo die Formel mehr ergäbe), und je Stück kommt ein **Logistikzuschlag von 50 Talern** hinzu. Billige Suppen bleiben so kleine Aufträge, teure Waren wie die Lore folgen der Formel.
- **Händlerware zählt nicht.** Kuchen, Brot, Kürbiskuchen, Kekse, Kaninchenragout und Goldene Karotten verkaufen Dorfbewohner; diese Exemplare sind als [Händlerware](10-markt.md#händlerware) markiert. Gefordert werden nur Waren, die sich weder durch Platzieren und Abbauen noch über Zutaten vom Dorfbewohner waschen lassen. Deshalb fehlen etwa der verzierte Krug (Ziegel vom Maurer), Laterne und Bücherregal (Bibliothekar). Der GameTest `merchant_goods_pool` prüft den ganzen Pool.
- **² Bewusste Ausnahme:** Den Apfel für den Goldenen Apfel verkauft auch der Bauer. Er macht aber nur 71 von 750 Talern Zutatenwert aus, der Rest sind 8 Goldbarren. Gekaufte Äpfel sparen also höchstens 9 % ein; der Test führt den Goldenen Apfel deshalb unter den hingenommenen Waren.

**Pflege:** Ändern sich Basiswerte oder `contracts.tierValue`, Menge und Lohn nach den Formeln oben neu rechnen.

### Tränke (Stufe 3)

Gebraute Tränke stehen nicht einzeln in der Liste. Eine Vorlage mit `random_potion` würfelt beim Aushängen einen Trank aus der **Tranktabelle** (`potions` in `data/dtp/dtp/contract/potions.json`), so wie Stufe 4 Verzauberungen würfelt:

1. Grundtrank aus der Tabelle, gleichverteilt.
2. Immer ein Modifikator: **verlängert** (Redstone) oder **verstärkt** (Glowstone), soweit es die Variante gibt. Unveränderte Tränke werden nie gefordert.
3. Normal oder **Wurftrank** (Schwarzpulver), je zur Hälfte (`splash_chance`).

Gefordert wird genau dieser Trank in genau dieser Form. Verweiltränke und Tränke ohne Verlängerung oder Verstärkung (Glück, Windladung, Weben, Schleim, Befall) kommen nicht vor. Die Vorlage braucht den Nether (`unlock`) und hat Gewicht 4. Es hängt höchstens ein Trank-Contract gleichzeitig aus.

- **Wert je Trank** = (Netherwarze + Zutaten + Redstone oder Glowstone [+ Schwarzpulver]) / 3 + 2,5. Eine Zutat braut 3 Tränke, die 2,5 sind Flasche und Lohenstaub-Anteil (geschätzt).
- **Menge** = 7.000 / Wert ± 30 %. Liegt der Wurf über 160, gilt 96–160 wie bei anderen nicht stapelbaren Waren.
- **Lohn je Trank** = Wert × 5 + 80. Multiplikator und Zuschlag sind höher als bei den gecrafteten Waren (× 2,75, + 50), weil Brauzeit und Nicht-Stapelbarkeit belohnt werden. Trank-Contracts dürfen deshalb deutlich teurer werden als andere Stufe-3-Contracts.

| Trank | Zutaten (Basiswert) | Modifikator | Wert normal / Wurf | Lohn je Stück |
|---|---|---|---|---|
| Schnelligkeit | Zucker 5,1 | lang / stark | 11,4 / 26,1 · 18,4 / 33,1 | 137–246 |
| Sprungkraft | Hasenpfote 250 | lang / stark | 93,1 / 107,7 · 100,1 / 114,7 | 545–654 |
| Stärke | ½ Lohenrute 90,5 | lang / stark | 39,9 / 54,6 · 46,9 / 61,6 | 280–388 |
| Heilung | Glitzernde Melone 77,6 | stark | 42,6 / 57,3 | 293–366 |
| Regeneration | Ghastträne 354 | lang / stark | 127,7 / 142,4 · 134,7 / 149,4 | 719–827 |
| Feuerresistenz | Magmacreme = Schleimball + ½ Lohenrute 147,1 | lang | 58,8 / 73,4 | 374–447 |
| Wasseratmung | Kugelfisch 36,2 | lang | 21,8 / 36,5 | 189–262 |
| Nachtsicht | Goldene Karotte 80,6 | lang | 36,6 / 51,3 | 263–336 |
| Unsichtbarkeit | Goldene Karotte + FSA 119 | lang | 49,4 / 64,1 | 327–400 |
| Schwäche ¹ | Fermentiertes Spinnenauge 38,4 | lang | 19,5 / 34,1 | 177–251 |
| Sanfter Fall | Phantomhaut 212 | lang | 80,4 / 95,1 | 482–555 |
| Gift | Spinnenauge 28,3 | lang / stark | 19,2 / 33,8 · 26,2 / 40,8 | 176–284 |
| Schaden | Spinnenauge + FSA 66,7 | stark | 39,0 / 53,6 | 275–348 |
| Langsamkeit | Zucker + FSA 43,5 | lang / stark | 24,2 / 38,9 · 31,2 / 45,9 | 201–310 |

- **¹ Schwäche** braucht keine Netherwarze. Dafür zählt das fermentierte Spinnenauge (FSA) mit allen Zutaten (Spinnenauge, Pilz, Zucker); das ist ihr Aufschlag gegenüber Gift.
- Abgeleitete Tränke rechnen den günstigsten Brauweg (Schaden über Gift, Langsamkeit über Schnelligkeit).
- Ein Contract bringt so etwa 13.200 (Schnelligkeit lang, 96 Stück) bis 60.000 Taler (Feuerresistenz lang, 160 Stück); die meisten liegen bei 17.000–55.000.

**Pflege:** Die Zutatenwerte in `values` sind Summen der Basiswerte aus der Handelsliste; ändern sich diese, die Summe neu rechnen. `awkward`, `long`, `strong` und `splash` sind die Basiswerte von Netherwarze, Redstone, Glowstone und Schwarzpulver. Formel und Deckel stehen ebenfalls in der Datei (`batch`, `bottle`, `reward_multiplier`, `surcharge`, `max_count`, `capped_min`).

### Freischaltung einzelner Vorlagen

Lohe, Netherquarz, Netherwarze und die Tränke tragen `"unlock": { "dimension": "minecraft:the_nether" }` und werden erst ausgewürfelt, wenn der Nether offen ist. Was schon aushängt oder angenommen ist, bleibt.

---

## Stufe 4: verzauberte und exotische Ausrüstung

Stufe 4 fordert verzauberte Bücher, verzauberte Ausrüstung und seltene Einzelstücke. Verzauberte Items sind am freien Markt nie handelbar ([Handelsliste-Bewertung](60-handelsliste-bewertung.md)), Contracts sind ihr einziger Absatzweg.

### Bedingung ist Mindestbedingung

- Die geforderte Verzauberung muss **mindestens** die genannte Stufe haben. Haltbarkeit III erfüllt „Haltbarkeit II“.
- **Weitere Verzauberungen sind erlaubt.**
- Bei Büchern zählen die gespeicherten Verzauberungen (`stored_enchantments`), bei Ausrüstung die aktiven (`enchantments`).
- Dasselbe Item mit anderer Bedingung ist eine **andere Anforderung**: Ein gewürfeltes Buch und „beliebiges verzaubertes Buch“ dürfen gleichzeitig aushängen und angenommen werden.

Feste Bedingungen stehen in `predicates` im Vanilla-Format, wie in Advancements und Loot-Tables:

```json
{ "item": "minecraft:diamond_pickaxe", "tier": 4, "reward_per_item": 12000,
  "predicates": {
    "minecraft:enchantments": [ { "enchantments": "minecraft:unbreaking", "levels": { "min": 2 } } ]
  } }
```

Vorlagen ohne Bedingung nehmen jedes Exemplar, so fordert „beliebiges verzaubertes Buch“ einfach `enchanted_book`.

### Regeln bei der Lieferung

| Regel | Begründung |
|---|---|
| **Beschädigung zählt nicht** | Ausrüstung vor der Abgabe teuer zu reparieren lohnt nicht. Eine Vorlage kann mit `"minecraft:damage": { "damage": 0 }` trotzdem „unbeschädigt“ verlangen |
| **Das schwächste passende Exemplar zuerst** (kleinste Summe der Verzauberungsstufen) | Liegen eine schlichte und eine Spitzhacke mit Reparatur in der Trade-Chest, soll nicht die gute verschwinden |
| **Umbenannte Items werden nie geliefert** | Schutz für das Lieblingswerkzeug, das versehentlich in der Trade-Chest liegt |
| Vanilla-Flüche sind erlaubt | Bei Bedarf über `predicates` ausschließbar |
| **Scam-Fluch und Maut-Fluch sperren die Lieferung** | Ein Item vom [Schwarzmarkt](14-blackmarket.md#der-scam-fluch) soll sich nicht über Contracts zu Geld machen lassen |
| **[Händlerware](10-markt.md#händlerware) sperrt die Lieferung**, auf jeder Stufe | Bücher vom Bibliothekar oder Kuchen vom Bauern sind im Extremfall gratis und unbegrenzt |

Das Board zeigt das Item mit der geforderten Mindestverzauberung, darunter „Diese Stufe oder höher, weitere Verzauberungen erlaubt“. Weitere Bedingungen wie „Muss unbeschädigt sein“ nennt der Tooltip.

### Gewürfelte Verzauberungen

Eine Vorlage mit `random_enchantments` legt ihre Bedingung erst beim Aushängen fest:

- **Anzahl:** `count` Verzauberungen werden gezogen und als Mindestbedingung gefordert.
- **Pool:** alle Einträge der Wertetabelle, die aufs Item passen (Vanilla-Regel `isSupportedItem`); Bücher nehmen die ganze Tabelle. Mit `exclude` lassen sich einzelne Verzauberungen je Vorlage ausnehmen. Was nicht in der Tabelle steht, wird nie gefordert.
- **Stufe:** je Verzauberung zufällig das Maximum oder eins darunter. Eine Stufe darunter bringt `below_max_factor` des Werts (0,6). Einstufige Verzauberungen sind immer voll.
- **Verträglich:** Gezogen werden nur Verzauberungen, die zusammen auf ein Item passen (`Enchantment.areCompatible`). Schutzarten, Sog mit Treue oder Unendlichkeit mit Reparatur kommen nie gemeinsam.
- **Belohnung je Item** = `reward_per_item` + Summe der Werte der gezogenen Verzauberungen.
- **Vielfalt:** Je Zufallsvorlage hängt höchstens ein Angebot aus.

```json
{
  "enchantments": { "below_max_factor": 0.6, "values": { "minecraft:sharpness": 7000, "minecraft:unbreaking": 4000 } },
  "templates": [
    { "item": "minecraft:diamond_boots", "tier": 4, "reward_per_item": 50000, "random_enchantments": { "count": 2 } }
  ]
}
```

Die Wertetabelle steht auf oberster Ebene unter `enchantments` und gilt für alle Items; mehrere Dateien werden zusammengeführt.

**Wertetabelle** (bei Maximalstufe). Bewusst niedrig gewählt, solange Villager-Bücher lieferbar waren. Seit Händlerware nicht mehr zählt, steht eine Neubewertung aus ([TODO v6](95-todo-v6.md)). **Reparatur (Mending) fehlt bewusst:** Außer vom Bibliothekar gibt es sie nur aus Zufallstruhen und beim Angeln, und Villager-Bücher lassen sich nicht mehr per Amboss waschen.

| Gruppe | Verzauberungen | Wert |
|---|---|---|
| Standard | Atmung, Wasseraffinität, Dornen, Flamme, Schlag, Feuerschutz | 3.000 |
| | Explosionsschutz, Projektilschutz | 3.500 |
| | Haltbarkeit, Unendlichkeit, Behutsamkeit | 4.000 |
| Gefragt | Federfall, Tiefenläufer, Köder, Glück des Meeres, Treue, Harpune | 5.000 |
| | Schutz, Stärke, Sog, Entladung | 6.000 |
| Kernwerkzeug | Effizienz, Schärfe, Dichte | 7.000 |
| | Glück, Plünderung, Durchbruch | 8.000 |
| Schatz, nicht am Zaubertisch | Frostläufer | 10.000 |
| Struktur, nicht farmbar | Seelenläufer | 15.000 |
| | Huschen, Windstoß | 18.000 |

### Die Stufe-4-Vorlagen

Richtwert für den Grundwert: Materialwert (Diamant 707, Netherite-Barren ≈ 21.000) plus Aufwand fürs Verzaubern; die gewürfelten Verzauberungen kommen obendrauf.

| Vorlage | Bedingung | Menge | Gewicht | je Stück |
|---|---|---|---|---|
| Verzaubertes Buch | beliebig | 4–8 | 8 | 4.000 |
| Verzaubertes Buch | 2 gewürfelt | 1 | 3 | 4.000 + Werte |
| Verzaubertes Buch | 3 gewürfelt | 1 | 2 | 4.000 + Werte |
| Angel | 1 / 2 gewürfelt | 1 | je 1 | 2.000 + Werte |
| Bogen | 1 / 2 gewürfelt | 1 | je 1 | 2.000 + Werte |
| Dreizack | – | 1 | 1 | 60.000 |
| Dreizack | 2 / 3 gewürfelt | 1 | je 1 | 65.000 / 70.000 + Werte |
| Elytra | – | 1 | 1 | 200.000 |
| Elytra | 2 gewürfelt (praktisch Haltbarkeit + Reparatur) | 1 | 1 | 230.000 + Werte |
| Streitkolben | 2 / 3 gewürfelt | 1 | je 1 | 120.000 / 150.000 + Werte |
| Diamanthelm | 2 / 3 gewürfelt | 1 | je 1 | 50.000 / 55.000 + Werte |
| Diamantbrustpanzer | 2 / 3 gewürfelt | 1 | je 1 | 60.000 / 65.000 + Werte |
| Diamanthose | 2 / 3 gewürfelt | 1 | je 1 | 55.000 / 60.000 + Werte |
| Diamantstiefel | 2 / 3 gewürfelt | 1 | je 1 | 50.000 / 55.000 + Werte |
| Netheritehelm | – | 1 | 1 | 150.000 |
| Netheritehelm | 2 / 3 gewürfelt | 1 | je 1 | 150.000 / 160.000 + Werte |
| Netheritebrustpanzer | 2 / 3 gewürfelt | 1 | je 1 | 170.000 / 180.000 + Werte |
| Netheritehose | 2 / 3 gewürfelt | 1 | je 1 | 160.000 / 170.000 + Werte |
| Netheritestiefel | 2 / 3 gewürfelt | 1 | je 1 | 150.000 / 160.000 + Werte |

---

## Der Aushang

Der Aushang ist **serverweit**. Er zeigt `contracts.boardSize` Angebote (4), das Server-Upgrade **Aushang-Platz** (`contract_slots`) gibt je Stufe einen Platz mehr:

| Stufe | Preis | Angebote |
|---|---|---|
| – | – | 4 |
| I | 8.000 | 5 |
| II | 25.000 | 6 |
| III | 60.000 | 7 |
| IV | 120.000 | 8 |

Neue Plätze füllen sich mit der nächsten Rotation.

### Rotation statt Neuwürfeln

Der Aushang **rotiert alle `contracts.rotationDays` Minecraft-Tage** (3) auf der Aushang-Uhr: Alle unangenommenen Angebote werden durch neue ersetzt. Dazwischen wird **nicht nachgefüllt**. Wer ein Angebot annimmt, hinterlässt eine Lücke bis zur nächsten Rotation. Die Oberfläche zeigt, wann sie kommt.

Es gibt **kein manuelles oder bezahltes Neuwürfeln** und kein Ablehnen. Passt gerade nichts, kommt das nächste Angebot mit der Rotation, für alle Spieler gleichzeitig.

### Zusammensetzung

- **Garantiert:** Je freigeschalteter Stufe hängt mindestens ein Angebot aus (`contracts.guaranteedPerTier`, 1). Reicht der Platz nicht, wird reihum ab Stufe 1 verteilt.
- **Frei:** Die übrigen Plätze werden ausgewürfelt, von der höchsten freigeschalteten Stufe abwärts je ein Wurf mit `contracts.tierChance` (40 % / 20 % / 10 % für Stufe 2 / 3 / 4). Schlagen alle fehl, wird es Stufe 1. Fehlende Einträge übernehmen den letzten Wert der Liste. Das Server-Upgrade **Contract-Qualität** (`contract_quality`) hebt jede dieser Chancen je Stufe um `contracts.tierChancePerLevel` (5 Prozentpunkte) an, höchstens auf 100 %.
- **Ausweichen:** Gibt es für eine Stufe keine freie Vorlage mehr, rückt eine andere freigeschaltete Stufe nach.
- **Vielfalt:** Zwei Angebote fordern nie dasselbe, auch nicht in verschiedenen Stufen.
- Das Ergebnis ist nach Stufe sortiert.

Verteilung je freiem Platz bei allen 4 Stufen:

| `contracts.tierChance` | Stufe 1 | Stufe 2 | Stufe 3 | Stufe 4 |
|---|---|---|---|---|
| `[0.5, 0.3, 0.2]` (Standard + Contract-Qualität II) | 28 % | 28 % | 24 % | 20 % |
| `[0.45, 0.25, 0.15]` (Standard + Contract-Qualität I) | 35 % | 29 % | 21 % | 15 % |
| `[0.4, 0.2, 0.1]` (Standard) | 43 % | 29 % | 18 % | 10 % |
| `[0.3, 0.15, 0.05]` | 57 % | 24 % | 14 % | 5 % |

Bestehende Config-Dateien werden bei geänderten Standardwerten nicht automatisch aktualisiert. Nach einem Update die Listen in `config/dtp-synced.toml` prüfen ([Migration](72-migration.md)).

**Keine Kopplung an den Marktdruck.** Items mit hohem Marktdruck kommen nicht häufiger vor. Sonst bekäme der Spieler mit 8.000 gelagerten Weizen einen Weizenauftrag und erfüllte ihn aus dem Lager. Contracts sollen etwas anstoßen, nicht abschöpfen, was ohnehin herumliegt.

---

## Das Bulletin-Board

Contracts laufen über einen **eigenen Block**, getrennt vom Marktstand:

- **Aushang** der offenen Angebote mit Forderung, Grundwert, aktuellem Wert und Restfrist
- **Annahme** aus dieser Liste
- **Lieferung** aus den Trade-Chests in Reichweite (`market.tradeChestRadius`, 2 Blöcke). Das Board hat kein eigenes Inventar. Die Kisten werden nacheinander geleert, bei Warengruppen auch gemischt, bis der Contract voll ist; Teillieferungen werden gutgeschrieben.
- **Belohnung** in die Einzahlkisten in Reichweite (`deposit.radius`, 2 Blöcke). Nur was dort keinen Platz findet, geht ins Inventar. Die Meldung erscheint zusätzlich im Chat.
- **Abbruch** eines laufenden Contracts
- **Flagge** auf dem Mast hinten rechts: Mast und Flagge fahren aus einem eisengefassten Schacht in der Oberseite, sobald der Aushang neu gewürfelt wurde und Angebote hängen (Rotation, Stufenkauf, Admin-Befehl), und fahren ganz in den Block zurück, sobald jemand dieses Board öffnet; eingeholt ist nur der Schacht zu sehen. Jedes Board merkt sich selbst, welche Rotation es zuletzt gezeigt hat; andere Boards bleiben gehisst, bis sie selbst geöffnet werden. Kein Ton, keine Meldung. Ein frisch gesetztes Board beginnt eingeholt. Damit die Flagge auch ohne Besuch steigt, prüft ein geladenes Board einmal pro Sekunde, ob die Rotation fällig ist.

Verkauft wird am Marktstand, geliefert am Board, jeweils auf ausdrücklichen Knopfdruck und aus denselben Trade-Chests. Liefen beide über denselben Block, könnte ein falsch eingestellter Modus 2.000 Karotten verkaufen, die für einen Auftrag gedacht waren.

**Contracts hängen am Territorium.** Ein Board außerhalb der Grenze, in einer anderen Dimension oder vor dem Setzen des Beacons ist **nur lesbar**: Annehmen und Liefern sind gesperrt, Abbrechen bleibt erlaubt. Sonst könnte sich eine Gruppe über Contracts finanzieren und den Beacon hinauszögern. Einen 5-%-Preis wie am Marktstand gibt es nicht: Die Belohnung entsteht erst beim Abschluss, ein Abschlag ließe sich durch Liefern draußen und Abschließen drinnen umgehen.

---

## Annehmen: persönliche Kapazität

Gleichzeitig annehmbar sind `contracts.baseSlots` Contracts (2). Das persönliche Upgrade **Mehr Contracts** (`contract_capacity`, Werkstatt-Tab Handel, siehe [Persönliche Upgrades](30-persoenliche-upgrades.md)) erhöht das:

| Stufe | Preis | Gleichzeitige Contracts |
|---|---|---|
| – | – | 2 |
| I | 4.000 | 3 |
| II | 15.000 | 4 |

Die Begrenzung ist zentral: Ohne sie nimmt der Spieler alles an und erfüllt nebenbei, was ohnehin anfällt. Zwei eigene Contracts mit derselben Anforderung gehen nicht.

### Quick Delivery

Das persönliche Upgrade **Quick Delivery** (`quick_delivery`, 20.000 Taler) tauscht Sicherheit gegen Tempo. Es gilt für alle eigenen Contracts und wird beim Abschluss angewendet:

| | Ohne | Mit Quick Delivery |
|---|---|---|
| Zeitbonus | +20 % | +30 % (`contracts.quickDeliveryFactor` 1,3) |
| Bonusfrist | `contracts.bonusDays` | gleich |
| Nach der Frist | 1 Intervall 100 %, dann −10 % je Intervall | 1 Intervall 100 %, dann −20 % je Intervall (`contracts.quickDeliveryDecayMultiplier` 2) |
| Untergrenze | 50 % | 50 % |

Beispiel Stufe 1 (Frist 3 Tage), Wert nach Alter des Contracts:

| Alter | 0–3 Tage | 3–4 | 4–5 | 5–6 | 6–7 | 7–8 | ab 8 |
|---|---|---|---|---|---|---|---|
| Ohne | 120 % | 100 % | 90 % | 80 % | 70 % | 60 % | 50 % |
| Quick Delivery | 130 % | 100 % | 80 % | 60 % | 50 % | 50 % | 50 % |

Stufe 2 (Frist 6 Tage) läuft genauso, nur drei Tage später. Lohnend ist es nur für Spieler, die ihre Contracts konsequent innerhalb der Frist erfüllen: Einen Tag nach ihrem Ende zahlt ein Contract ohne Upgrade mehr.

### Auftragsuhr und Fernlieferung

Zwei Komfort-Upgrades im Werkstatt-Tab Handel machen die Vanilla-Uhr zum Auftragszettel. Sie ändern weder Lohn noch Fristen.

**Auftragsuhr** (`contract_clock`, 3.000 Taler): Der Tooltip jeder Uhr zeigt die angenommenen Contracts, eine Zeile je Contract:

```
Karotte: 23 / 64, 120%, 0.6 d
```

Das sind Lieferstand, aktueller Wert mit den eigenen Bedingungen (also mit Quick Delivery) und die Tage bis zum nächsten Abfall. An der Untergrenze entfällt die Tagesangabe. Ohne Contracts steht dort „Keine Contracts angenommen“.

- Die angenommenen Contracts werden an den eigenen Client synchronisiert: nach Annehmen, Liefern und Abbrechen sowie einmal je Tag der persönlichen Uhr. Dazwischen rechnet der Client die persönliche Uhr mit der Overworld-Uhr hoch, derselben Uhr, der der Server folgt. Die Config ist synchronisiert, deshalb rechnet der Client Wert und Restzeit mit denselben Formeln.
- Schleichen + Rechtsklick auf eine **Marktwaage** bindet die Uhr an deren Item (Komponente `dtp:watched_item`). Dann steht über den Contracts der Marktpreis, siehe [Markt](10-markt.md#marktwaage).

**Fernlieferung** (`remote_delivery`, 10.000 Taler, setzt die Auftragsuhr voraus): Schleichen + Rechtsklick mit der Uhr in die Luft beliefert alle angenommenen Contracts aus Hauptinventar und Hotbar.

- Nur im Territorium, wie am Board. Rüstung und Zweithand bleiben unberührt; umbenannte und verfluchte Items zählen wie am Board nicht.
- Teillieferungen werden gutgeschrieben. Der Abschluss läuft über denselben Weg wie am Board: Er zählt für die Contract-Stufe und rückt das nächste Marktereignis näher.
- Der Lohn geht **ins Inventar**, was keinen Platz findet, fällt vor dem Spieler auf den Boden. Am Board ginge er in die Einzahlkisten. Ohne Board gibt es aber keine eindeutige Kiste, und die eines Nachbarn wäre die falsche. Getragene Taler gehen beim Tod verloren.
- Abklingzeit 5 Sekunden (`params.cooldown` in `player_upgrade/remote_delivery.json`), als Vanilla-Abklingzeit an der Uhr sichtbar. Sie verhindert nur Spam.

---

## Abbrechen und Weitergabe

Contracts dürfen **jederzeit abgebrochen** werden, etwa nach einer versehentlichen Annahme. Der Contract kehrt als gewöhnliches Angebot in den Aushang zurück und wird mit der nächsten Rotation ersetzt; seine Bonusfrist läuft weiter. **Bereits gelieferte Items sind verloren.** Das ist die einzige Härte im System und genügt, damit Annehmen eine Entscheidung bleibt.

Contracts sind **Daten am Spieler**, keine Items:
- **Tod:** Ein Contract-Item fiele beim Sterben. Einen zu zwei Dritteln erfüllten Großauftrag in der Lava zu verlieren, wäre etwas anderes als verlorene Taler.
- **Stufen:** Ein handelbares Item löste den Contract vom Aushang, an dem die serverweite Stufe hängt.

Weitergabe und Teamwork gehen trotzdem:
- **Abbrechen** gibt den Contract in den Aushang zurück, wo ihn ein anderer Spieler annehmen kann. Der Lieferfortschritt verfällt dabei.
- **Mithelfen** geht ohne Übergabe: Geliefert wird aus den gemeinsamen Trade-Chests, also kann jeder Ware für den Contract eines Mitspielers einlegen. Die Belohnung landet in der Einzahlkiste am Board.

Hohe Stufen sind so faktisch Gruppenvorhaben, formal hält sie der Annehmende.

---

## Config

Alle Schlüssel stehen in `config/dtp-synced.toml` ([Technischer Rahmen](70-technischer-rahmen.md)). Listen haben Index 0 = Stufe 1.

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `contracts.boardSize` | 4 | Angebote im Aushang ohne Upgrade |
| `contracts.rotationDays` | 3,0 | Rotation in Minecraft-Tagen |
| `contracts.baseSlots` | 2 | Gleichzeitig annehmbare Contracts ohne Upgrade |
| `contracts.baseMaxTier` | 0 | Freie Stufe ohne Upgrade, 0 = gesperrt |
| `contracts.guaranteedPerTier` | 1 | Garantierte Angebote je Stufe |
| `contracts.tierChance` | 0,4 / 0,2 / 0,1 | Würfelchance für Stufe 2 / 3 / 4 |
| `contracts.tierChancePerLevel` | 0,05 | Aufschlag auf jede Würfelchance je Stufe Contract-Qualität |
| `contracts.tierRequirement` | 0 / 5 / 25 / 60 | Serverweit abgeschlossene Contracts für den Kauf der Stufe |
| `contracts.bonusDays` | 3 / 6 / 9 / 12 | Bonusfrist in Minecraft-Tagen |
| `contracts.decayIntervalDays` | 1,0 | Länge eines Verfallsintervalls, für alle Stufen |
| `contracts.bonusFactor` | 1,2 | Faktor innerhalb der Frist, entfällt mit ihrem Ende |
| `contracts.decayPerInterval` | 0,1 | Abzug vom Grundwert je vollem Intervall nach der Frist |
| `contracts.minFactor` | 0,5 | Untergrenze |
| `contracts.rewardMultiplier` | 2,0 / 2,3 / 2,75 / 3,5 | Lohn je Item als Vielfaches des Basiswerts |
| `contracts.tierValue` | 800 / 2.800 / 7.000 / 30.000 | Wertziel der Menge |
| `contracts.tierValueSpread` | 0,3 | Streuung um das Wertziel |
| `contracts.quickDeliveryFactor` | 1,3 | Quick Delivery: Faktor innerhalb der Frist |
| `contracts.quickDeliveryDecayMultiplier` | 2,0 | Quick Delivery: Verfall schneller um Faktor |

Admins würfeln den Aushang mit `/dtp admin contracts reroll` neu ([Entwicklung und Test](71-entwicklung-und-test.md)).
