# Schwarzmarkt

> Der Schwarzmarkt verwandelt Taler in Verzauberungsstufen: teuer, nur einmal am Tag und nicht immer ehrlich.
> Er ist eine Talersenke für Spieler, die schon alles haben, und ein Glücksspiel, auf das man sich bewusst einlässt.
> Der fahrende Händler, der zum Schwarzmarkt gehört, hat ein eigenes Dokument: [Schwarzmarkthändler](15-blackmarket-trader.md).

---

## Rolle

Der Markt und die Contracts bringen Taler herein. Der Beacon und die Werkstatt geben sie planbar wieder aus. Dem Schwarzmarkt fehlt diese Planbarkeit mit Absicht. Für einen Silberbeutel steigt eine Verzauberung um eine Stufe, **meistens**.

**Abgrenzung zum Verzauberungstisch:** Der Schwarzmarkt verzaubert nichts neu. Er hebt nur vorhandene Verzauberungen an, und zwar höchstens bis zu ihrer Vanilla-Höchststufe. Vanilla-Verzauberungen werden nicht ersetzt und nicht über ihre Grenzen gehoben.

---

## Der Block

**Schwarzmarkt** (`dtp:blackmarket`). Der Block hat kein eigenes Inventar und braucht zum Abbauen eine Spitzhacke. Die Oberfläche sieht aus wie die des Verzauberungstischs:
- links ein Slot für den Gegenstand und einer für den Silberbeutel,
- rechts ein Feld, das den Stand oder das Ergebnis zeigt,
- darunter der Knopf **Handeln**.

Was beim Schließen noch in den Slots liegt, geht an den Spieler zurück.

**Rezept** (geformt):

```
Schwarze Wolle     Schwarze Wolle     Schwarze Wolle
Marktstand         Silbersack         Verzauberungstisch
Weinender Obsidian Weinender Obsidian Weinender Obsidian
```

---

## Ablauf

1. **Gegenstand:** Er muss echte Verzauberungen tragen. Verzauberte Bücher zählen nicht, sonst ließen sich verbesserte Stufen beliebig weitergeben.
2. **Bezahlung:** genau ein **Silberbeutel** (6.561 Taler) im Slot. Kleinere Stufen oder ein Silbersack werden nicht angenommen, und bewusst wird auch nicht aus den Einzahlkisten bezahlt. Das Geld liegt sichtbar auf dem Tisch.
3. **Handeln:** Aus allen Verzauberungen unter ihrer Höchststufe wird eine gleichverteilt gewählt. Flüche zählen nicht. Bei einem Erfolg steigt sie um eins.

Der Knopf ist gesperrt, und das Feld nennt den Grund, wenn:
- der Gegenstand fehlt oder nicht verzaubert ist,
- er irgendeinen Fluch trägt, auch einen Vanilla-Fluch wie Fluch des Verschwindens,
- alle Verzauberungen schon auf der Höchststufe sind,
- kein Silberbeutel im Slot liegt,
- heute schon gehandelt wurde.

---

## Tageslimit

**Einmal pro Markttag, serverweit.** Ein Markttag ist ein Tag der Marktuhr ([Marktdruck](11-marktdruck.md)). Sie läuft nur, solange jemand online ist, Schlafen zählt mit. Jede Nutzung verbraucht den Tag, **auch ein Scam**. Das Feld zeigt, wann es wieder geht.

Gespeichert wird das in `BlackmarketData` (Feld `last_use_day`, siehe [Technischer Rahmen](70-technischer-rahmen.md)). Der Händler teilt das Tageslimit nicht, er hat seine eigene Abklingzeit.

---

## Scams

Jeder Handel ist mit 20 % Wahrscheinlichkeit ein Scam. Welcher Scam es wird, entscheidet die Gewichtung 1 : 2 : 2, bei Abbauwerkzeugen 1 : 2 : 2 : 2. In jedem Fall ist der Beutel weg.

| Ausgang | Gewicht | Anteil | Anteil bei Werkzeugen | Folge |
|---|---|---|---|---|
| Erfolg | – | 80 % | 80 % | Eine Verzauberung steigt um eine Stufe. |
| Diebstahl | 1 | 4 % | 2,9 % | Gegenstand und Beutel sind weg. |
| Nichts | 2 | 8 % | 5,7 % | Der Beutel ist weg, sonst passiert nichts. |
| Fluch | 2 | 8 % | 5,7 % | Eine Verzauberung steigt, aber der Gegenstand bekommt den **Scam-Fluch**. |
| Maut | 2 | – | 5,7 % | Eine Verzauberung steigt, aber das Werkzeug bekommt den **Maut-Fluch**. |

Die Maut zählt nur für Abbauwerkzeuge (`#minecraft:enchantable/mining`), die den Maut-Fluch noch nicht tragen. Für alles andere fällt ihr Gewicht weg.

Die Oberfläche nennt die Wahrscheinlichkeit nicht, sondern warnt nur: „Der Schwarzmarkt ist nicht ehrlich.“

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `blackmarket.scamChance` | 0,2 | Wahrscheinlichkeit eines Scams |
| `blackmarket.weightSteal` | 1 | Gewicht Diebstahl |
| `blackmarket.weightNothing` | 2 | Gewicht Nichts |
| `blackmarket.weightCurse` | 2 | Gewicht Scam-Fluch |
| `blackmarket.weightToll` | 2 | Gewicht Maut-Fluch, nur für Abbauwerkzeuge |

---

## Der Scam-Fluch

Der Scam-Fluch (`dtp:scam_curse`) ist eine datengetriebene Verzauberung ohne eigene Wirkung im Tag `#minecraft:curse`. Deshalb steht er rot im Tooltip, und der Schleifstein entfernt ihn nicht. Er steht in keinem Tag für Verzauberungstisch, Handel, Beute oder Mob-Ausrüstung, man bekommt ihn also nur vom Schwarzmarkt.

Ein verfluchter Gegenstand:
- zählt für **keinen Contract**, auch wenn er die Bedingung erfüllt. Die Prüfung sitzt in `Contract.matches`, an derselben Stelle wie die Sperre für umbenannte Items ([Contracts](12-contracts.md#regeln-bei-der-lieferung)).
- wird am **Schwarzmarkt nicht mehr angenommen**.
- lässt sich im **Amboss mit nichts kombinieren**: weder mit Items noch mit Büchern noch mit Reparaturmaterial, auch nicht, wenn das verfluchte Stück rechts liegt. Umbenennen geht weiter, aber ein umbenanntes Item zählt ohnehin für keinen Contract.

Am Markt war er nie verkaufbar, denn verzauberte Items stehen nicht auf der Handelsliste.

---

## Der Maut-Fluch

Der Maut-Fluch (`dtp:toll_curse`, im Spiel „Fluch der Maut“, Klasse `TollCurse`) ist ein Werkzeug-Fluch mit eigener Wirkung. Wie der Scam-Fluch steht er im Tag `#minecraft:curse` und in keinem anderen Tag. Er passt nur auf Abbauwerkzeuge (Spitzhacke, Axt, Schaufel, Hacke, Schere) und wirkt in der Haupthand.

**Jeder Abbau kostet Maut.** Wer mit dem verfluchten Werkzeug einen Block mit Härte über 0 abbaut, zahlt `curse.tollPrice` Taler aus dem **Inventar**, nicht aus den Einzahlkisten. Die kleinste Münze geht zuerst, das Wechselgeld kommt ins Inventar zurück: Ein Kupferbeutel wird zu 8 Kupfertalern. Das Geld ist weg. Gras, Blumen und andere Blöcke, die sofort brechen, kosten nichts, ebenso der Kreativmodus.

**Mehrfachabbau zahlt einmal.** Bezahlt wird nur der getroffene Block; was Baumfällen, Vein Mining oder die Abbaufläche 3×3 mitnehmen, ist frei. Eine Spitzhacke mit Abbaufläche baut so 9 Blöcke für 1 Taler ab und nutzt den Unbreaking-Bonus voll aus.

**Ohne Geld:**
- Das Werkzeug baut nur mit `curse.tollSlowdown` der Geschwindigkeit ab. Dieser langsame Abbau ist frei.
- Beim Anschlagen eines Blocks erscheint eine Meldung über der Hotbar.
- Es gibt keinen Mehrfachabbau (Baumfällen, Vein Mining, Abbaufläche 3×3), nur der getroffene Block bricht.

**Ausgleich: Unbreaking +1.** Weil das Geld einen Teil der Abnutzung trägt, zählt Unbreaking eine Stufe höher als eingetragen: ohne Unbreaking wie I, mit III wie IV. Das gilt immer, auch ohne Geld. Technisch läuft das über NeoForges `GetEnchantmentLevelEvent`, also ohne Mixin. Der Tooltip zeigt die eingetragene Stufe und darunter eine eigene Zeile für den Bonus. Der Schwarzmarkt sieht ebenfalls nur die eingetragene Stufe.

**Sperren wie beim Scam-Fluch:** Ein Werkzeug mit Maut-Fluch zählt für keinen Contract, wird am Schwarzmarkt nicht mehr angenommen und lässt sich im Amboss mit nichts kombinieren. Umbenennen geht weiter. Alle drei Stellen prüfen beide Flüche gemeinsam über `ScamCurse.isLocked`.

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `curse.tollPrice` | 1 | Taler je Abbau, 0 schaltet die Maut ab |
| `curse.tollSlowdown` | 0,1 | Abbaugeschwindigkeit ohne Geld |

---

## Tests

Die GameTests `blackmarket_upgrade`, `blackmarket_rejects`, `blackmarket_daily_limit`, `blackmarket_outcomes`, `scam_curse_*` und `toll_curse_*` decken Block, Flüche und Sperren ab ([Entwicklung und Test](71-entwicklung-und-test.md)).
