# Reparaturbank

> Die Reparaturbank verwandelt Taler in Haltbarkeit: ohne Erfahrung, ohne Material, ohne steigende Amboss-Strafe.
> Sie ist eine Reparatur für die Zeit vor Mending. Sie kostet etwas mehr als der Amboss, und mit haltbarer Ausrüstung
> steigt der Preis so stark, dass Mending auf Dauer der bessere Weg bleibt. Außerdem lädt sie [Contraband](13-contraband.md) auf.

---

## Rolle

Im frühen Spiel gibt es zwei Wege, Werkzeug und Rüstung zu reparieren:
- **Amboss mit Material:** kostet Erfahrung, und jede Reparatur erhöht die Amboss-Strafe.
- **Neu craften:** verliert die Verzauberungen.

Die Bank schließt diese Lücke gegen Taler. Ersetzen soll sie Mending nicht:
- Mending kommt über Contracts, Villager und Funde ins Spiel.
- Die [Mending-Aura](20-territorium-und-serverweite-effekte.md) repariert später langsam im ganzen Beacon-Gebiet nebenbei.
- Die Bank bleibt für den Übergang, und für Ausrüstung, für die sich Mending nicht lohnt.

**Bezug zur Vision:**
- **Talersenke:** Die Bank erzeugt keinen Wert. Werkzeuge und Rüstung sind unverkäuflich (Ausschlussregel A5, [Handelsliste bewerten](60-handelsliste-bewertung.md)), und die Bank füttert keine Produktion.
- **Ausnahme:** Sie gehört zu den eng begrenzten Ausnahmen von „Keine Verbrauchsgüter für Taler“ ([Mod und Vision](01-mod-und-vision.md#anti-ziele)). Es ist eine Dienstleistung ohne Vorrat, die sich nicht horten lässt.

---

## Der Block

**Reparaturbank** (`dtp:repair_station`). Eine Werkbank aus Eiche, die mit der Axt abgebaut wird. Der Block hat kein eigenes Inventar. Die Oberfläche hat denselben Aufbau wie beim [Schwarzmarkt](14-blackmarket.md):
- links ein Slot für den Gegenstand und einer für die Taler, darunter das Inventar,
- rechts ein Feld mit Kurs, Preis der vollen Reparatur und dem, was das eingelegte Geld bewirkt,
- darunter der Knopf **Reparieren**.

Was beim Schließen noch in den Slots liegt, geht an den Spieler zurück. Das Rezept steht ab dem ersten Betreten im Rezeptbuch.

**Rezept** (geformt):

```
              Kupferbeutel
Eisenbarren   Schleifstein   Eisenbarren
Bretter       Bretter        Bretter
```

---

## Ablauf

1. **Gegenstand:**
   - Alles mit Haltbarkeit: Werkzeuge, Waffen, Rüstung, Bogen, Schild, Elytren, Angel usw.
   - Contraband-Items: Sie haben Ladungen statt Haltbarkeit und werden aufgeladen statt repariert (siehe [unten](#contraband-aufladen)).
2. **Bezahlung:**
   - Taler **jeder Stückelung** im rechten Slot, beliebig viele.
   - Wie am Schwarzmarkt wird bewusst nicht aus den Einzahlkisten bezahlt. Das Geld liegt sichtbar auf dem Tisch.
3. **Reparieren:**
   - Die Bank nimmt Münzen einzeln aus dem Slot, bis der Gegenstand voll ist oder der Slot leer.
   - Je wertvoller die Münze, desto mehr wird repariert.
   - Zahlt die letzte Münze mehr, als nötig ist, kommt der Rest als **Wechselgeld** ins Inventar (in möglichst wenigen Münzen, wie bei den Einzahlkisten).
   - Reicht das Geld nicht für die volle Reparatur, gibt es so viele ganze Haltbarkeitspunkte, wie es bezahlt.

Der Knopf ist gesperrt, und das Feld nennt den Grund, wenn:
- der Gegenstand fehlt, keine Haltbarkeit hat oder unbeschädigt ist (Contraband: schon voll geladen),
- er einen Schwarzmarkt-Fluch trägt (Scam- oder Maut-Fluch, wie am Amboss). Vanilla-Flüche sperren nicht,
- keine Taler im Slot liegen,
- das ganze Geld nicht für einen einzigen Punkt reicht,
- die Bank in der Config abgeschaltet ist.

Es gibt kein Tageslimit und keine Territoriumspflicht.

---

## Kurs

```
Preis je Haltbarkeitspunkt = basePrice × (maxHaltbarkeit / 250) ^ exponent
Volle Reparatur            = Preis je Punkt × maxHaltbarkeit
```

Mit den Standardwerten (`basePrice` 2, `exponent` 0,5) kostet ein Punkt bei der Eisenspitzhacke (250) genau 2 Taler. Haltbarere Ausrüstung kostet je Punkt mehr, eine volle Reparatur wächst also mit `maxHaltbarkeit ^ 1,5`.

| Gegenstand (max. Haltbarkeit) | Taler je Punkt | Volle Reparatur |
|---|---|---|
| Lederkappe (55) | 0,94 | 52 |
| Steinspitzhacke (131) | 1,45 | 190 |
| Eisenbrustpanzer (240) | 1,96 | 470 |
| Eisenspitzhacke (250) | 2 | 500 |
| Eisenrüstung komplett | – | ≈ 1.510 |
| Bogen (384) | 2,48 | 952 |
| Elytren (432) | 2,63 | 1.136 |
| Diamantbrustpanzer (528) | 2,91 | 1.535 |
| Diamantrüstung komplett | – | ≈ 4.930 |
| Diamantspitzhacke (1.561) | 5,00 | ≈ 7.800 |
| Netheritspitzhacke (2.031) | 5,70 | ≈ 11.580 |

**Einordnung:**
- **Eisen:** Am Amboss kostet die Eisenspitzhacke 4 Eisenbarren (≈ 320 Taler Ruhepreis) plus Erfahrung. Die Bank verlangt etwa das 1,5-Fache und erspart dafür Erfahrung und Amboss-Strafe.
- **Sprung zu Diamant:** Eine volle Reparatur ist beim Diamantwerkzeug fast 16-mal so teuer wie bei der Eisenspitzhacke, ein einzelner Punkt 2,5-mal. Diamantwerkzeug ist meist verzaubert und damit die Reparatur wert. Ein Werkzeug kostet aber etwa so viel wie das Wertziel zweier Contracts der Stufe 2 ([Contracts](12-contracts.md)). Wer das regelmäßig zahlt, besorgt sich besser Mending.
- **Rüstung:** Rüstungsteile haben weniger Haltbarkeit als Werkzeuge derselben Stufe. Ihre Reparatur bleibt deshalb günstiger.

**Rechnung:**
- Gerechnet wird ganzzahlig: Der Preis je Punkt wird einmal auf Tausendstel-Taler gerundet.
- Die Kosten werden auf ganze Taler aufgerundet.
- Bezahlte, aber nicht verbrauchte Taler kommen immer als Wechselgeld zurück.

**Beispiele (Eisenspitzhacke):**
- 1 Kupferbeutel repariert 4 Punkte, 1 Kupfersack 40 Punkte.
- Ein Silbertaler repariert sie aus jedem Zustand voll.
- Bei 200 Schaden kostet die volle Reparatur 400 Taler; ein Silbertaler bringt dann 329 Taler Wechselgeld.

---

## Contraband aufladen

Contraband-Items ([Contraband](13-contraband.md)) laufen an der Bank denselben Ablauf durch, nur mit Ladungen statt Haltbarkeit:
- **Kurs:** der Preis je Ladung aus der Datapack-Definition (`charge_price`), unabhängig von `repair.basePrice` und `repair.exponent`. Beim Feuerlöscher 250 Taler.
- **Volles Aufladen:** fehlende Ladungen × Preis je Ladung.
- Münzen einzeln, Wechselgeld und Teilaufladung wie bei der Reparatur. Ist das Item voll, sperrt der Knopf.
- Das Panel spricht dann von Ladungen statt von Haltbarkeit, der Ton ist der des Seelenankers.

Die Bank ersetzt das frühere Aufladerezept im Crafting-Grid, das kein Rückgeld kannte.

---

## Config

In der Welt-Config (`dtp-synced.toml`), siehe [Technischer Rahmen](70-technischer-rahmen.md):

| Schlüssel | Standard | Wirkung |
|---|---|---|
| `repair.enabled` | true | Aus = Block lässt sich setzen und öffnen, repariert und lädt aber nicht auf |
| `repair.basePrice` | 2,0 | Taler je Haltbarkeitspunkt bei 250 maximaler Haltbarkeit |
| `repair.exponent` | 0,5 | Wie stark der Preis je Punkt mit der Haltbarkeit steigt: 0 = fester Kurs, 1 = quadratisch |

Jede Reparatur steht mit der Aktion `repair` im Messprotokoll ([Balancing](62-balancing.md)), jedes Aufladen mit `recharge`. Dabei ist `count` die Zahl der reparierten Punkte bzw. geladenen Ladungen und `amount` der bezahlte Betrag.

---

## Technik

| Teil | Wo |
|---|---|
| Logik (Kurs, Rechnung, Sperren, Contraband) | `dev.dtp.repair.RepairStation` |
| Menü | `dev.dtp.menu.RepairStationMenu` |
| Oberfläche | `dev.dtp.client.RepairStationScreen` |
| Gemeinsame Basis mit dem Schwarzmarkt | `TwoSlotMenu` / `TwoSlotScreen` |
| GameTests | `repair_station_quote`, `repair_station_rejects`, `repair_station_repair`, `contraband_recharge` |
