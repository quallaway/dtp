# Recycler

> Der Recycler zerlegt ausgediente Ausrüstung zu **Schrott**. Schrott ist eine Ware der Handelsliste und wird am
> Marktstand verkauft. Er bringt wenig, und mehr als die Rohstoffe am Boden des Markts bringt er nie.

---

## Rolle

Beim Angeln, in Mob-Farmen und in Strukturkisten fällt Ausrüstung an, die niemand braucht: Lederstiefel, Sättel, Steinschwerter. Bisher landet sie in Lava. Der Recycler gibt ihr einen kleinen Restwert.

**Bezug zur Vision:**
- **Die Handelsliste bleibt kuratiert.** Recycelbar ist nicht „alles, was nicht auf dem Markt steht“. Dann bekämen Bretter, Treppen und Türen einen Wert, und Craften würde Wert erzeugen. Der Recycler nimmt nur **Ausrüstung** an, also genau das, was die Handelsliste nach Regel A5 ausschließt ([Handelsliste bewerten](60-handelsliste-bewertung.md#schritt-1-ausschlussfilter)).
- **Was Vanilla schon verwertet, bleibt draußen.** Eisen-, Ketten-, Gold- und Kupferausrüstung samt Pferde- und Nautilusrüstung schmelzen im Ofen zu Nuggets. Holzwerkzeuge sind Brennstoff. Der Recycler nimmt beides nicht.
- **Eine Ware statt einer Sonderliste.** Am Marktstand erscheint nur eine Zeile „Schrott“ mit eigenem Marktdruck. Preis, Sättigung, Territoriumsregel, Preisverlauf, Marktwaage und Redstone-Verkauf funktionieren wie bei jeder Ware.
- **Verkaufen bleibt eine Entscheidung.** Der Recycler darf per Hopper durchlaufen, denn er verkauft nichts. Verkauft wird erst am Marktstand.
- **Wie ein Ofen.** Fenster, Brennstoff und Hopper-Seiten kennt jeder vom Ofen. Ins Fach legen ist die bewusste Entscheidung, eine Bestätigung braucht es nicht. Solange er brennt, leuchtet die Front, er knistert und raucht; jedes fertige Item klingt nach Schleifstein.
- **Recyceln kostet Brennstoff.** 20 Sekunden je Item, eine Kohle reicht für vier. Holzwerkzeuge, die er nicht annimmt, verheizt er dafür.

---

## Der Block

**Recycler** (`dtp:recycler`): ein Eisenkasten mit Einwurfgitter oben, einem Schleifrad vorn und der Ausgabeklappe unten. Abgebaut wird er mit der Spitzhacke, ohne passendes Werkzeug fällt nichts. Er hat drei Fächer wie ein Ofen: Eingabe, Brennstoff und Schrott. Solange er brennt (Blockzustand `lit`), glüht die Front und er leuchtet mit Stärke 13. Beim Abbauen fällt der Inhalt heraus. Das Rezept steht nicht im Rezeptbuch, das Handbuch zeigt es.

**Rezept** (geformt):

```
Eisengitter   Trichter      Eisengitter
Eisenbarren   Schleifstein  Eisenbarren
Eisenbarren   Eisenbarren   Eisenbarren
```

---

## Bedienung

Rechtsklick öffnet ein Fenster wie beim Ofen: oben die Eingabe, unten der Brennstoff, rechts der Schrott, dazu Flamme und Fortschrittspfeil. Shift-Klick legt Brennstoff ins Brennstofffach und alles andere in die Eingabe.

| | |
|---|---|
| Dauer | 400 Ticks (20 s) je Item; ohne Glut läuft der Fortschritt doppelt so schnell zurück, wie beim Ofen |
| Brennstoff | Vanilla-Brennwerte (`cooking_fuel`): Kohle 1.600 Ticks = 4 Items, Holzwerkzeug 200 Ticks = ½ Item. Lavaeimer lassen einen leeren Eimer im Brennstofffach zurück |
| Zünden | Neuer Brennstoff brennt nur an, wenn die Eingabe recycelbar ist und der Schrott sicher ins Ausgabefach passt |
| Hopper oben | Eingabe, nur recycelbare Items |
| Hopper seitlich | Brennstofffach |
| Hopper unten | Schrott, aus dem Brennstofffach nur leere Eimer |
| Comparator | Füllstand aller Fächer, wie beim Ofen |

Von Hand nimmt die Eingabe jedes Item an, wie der Ofen. Was nicht recycelbar ist, bleibt liegen, der Pfeil bewegt sich nicht und kein Brennstoff wird verbraucht. Die Recyclingliste kennt nur der Server; ein Filter im Fenster würde auf dem Client falsch vorhersagen.

---|---|
| Rechtsklick mit Ausrüstung | Zerlegt den Stapel in der Hand. Der Schrott geht ins Inventar, bei vollem Inventar fällt er zu Boden |
| Verzaubert oder umbenannt | Der erste Klick klingt nur wie ein Amboss. Erst ein zweiter Klick mit demselben Item am selben Recycler innerhalb von 3 Sekunden zerlegt es |
| Rechtsklick mit leerer Hand | Nimmt den Schrott aus dem Ausgabefach |
| Hopper oben oder seitlich | Füllt das Eingabefach. Alle 8 Ticks wird ein Item zerlegt, solange der Schrott sicher ins Ausgabefach passt |
| Hopper unten | Zieht Schrott ab, nur aus dem Ausgabefach |
| Comparator | Füllstand des Ausgabefachs |

Nicht recycelbare Items klacken und bleiben, wo sie sind. Blöcke in der Hand lassen sich wie gewohnt an den Recycler setzen, etwa ein Hopper an die Seite.

---

## Wert

1. **Bodenpreis-Regel:** Der Schrottwert eines Items ist 5 % (`market.factorMin`) der Basiswerte seiner Rohstoffe. Schrott hat `base` 0,71, also einen Ruhepreis von 1 Taler. Recyceln bringt damit nie mehr als der Verkauf der Rohstoffe bei voller Sättigung. Leder zu Rüstung zu craften und zu recyceln umgeht keine Ledersättigung.
2. **Bruchteile sind eine Chance.** Ein Steinschwert ist 0,12 wert und gibt in 12 % der Fälle 1 Schrott. Ein Bruchsteingenerator mit Autocrafter bringt im Mittel also nicht mehr als Bruchstein am Boden des Markts.
3. **Kappung bei 40 Schrott je Item.** Dorfbewohner verkaufen Diamant- und Lederausrüstung und Sättel im Extremfall gratis (Regel D1, [Dorfbewohnerhandel](60-handelsliste-bewertung.md#dorfbewohnerhandel)). Die Kappung trifft Diamant- und Netherit-Ausrüstung. Seit v6 nimmt der Recycler [Händlerware](10-markt.md#händlerware) gar nicht mehr an, auch nicht als Netherit-Aufwertung (die Markierung bleibt beim Schmieden erhalten); ob die Kappung so niedrig bleiben muss, steht im [TODO v6](95-todo-v6.md).
4. **Eigene Sättigung (D2).** Schrott ist eine Ware mit H 1.000 und T 4: Kapazität ≈ 1.400 Taler je Spieler, Dauereinkommen höchstens ≈ 90 Taler je Markttag und Spieler. Das begrenzt Handelshalle, Skelettfarm und AFK-Angeln.
5. **Haltbarkeit zählt.** Der Wert wird mit der verbleibenden Haltbarkeit multipliziert. Verzauberungen und Namen bringen nichts.
6. **Reparieren lohnt nicht.** Eine volle Reparatur an der [Reparaturbank](16-reparaturbank.md) kostet immer mehr, als der Schrott einbringt, bei Lederstiefeln rund 65 Taler gegen 7 Schrott.

| Item | Schrott |
|---|---|
| Lederkappe / -brustpanzer / -hose / -stiefel | 8,85 / 14,16 / 12,39 / 7,08 |
| Leder-Pferderüstung | 12,39 |
| Steinschwert / -spitzhacke / -axt / -schaufel / -hacke / -speer | 0,12 / 0,2 / 0,2 / 0,13 / 0,17 / 0,13 |
| Sattel (3 Leder + 1 Eisen) | 8,14 |
| Schild | 3,42 |
| Karottenrute / Wirrpilzrute | 2,87 / 2,83 |
| Schere / Feuerzeug / Pinsel | 5,66 / 2,83 / 0,79 |
| Schildkrötenpanzer (keine gelistete Zutat) | 10 |
| Diamant- und Netherit-Pferderüstung, -Nautilusrüstung | 40 (Kappung) |

**Nie recycelbar**, auch wenn ein Datapack es einträgt:
- Waren der Handelsliste, Taler, Contraband und Schrott selbst
- alles, was Zutat eines Ofen- oder Schmelzofenrezepts ist. Der Code prüft das gegen die geladenen Rezepte, also auch gegen Rezepte anderer Mods
- Behälter mit Inhalt

Nicht eingetragen sind Holzwerkzeuge (Brennstoff), Elytren, Streitkolben und verzauberte Bücher. Bücher liefert der Bibliothekar gratis.

---

## Daten

**Recyclingliste:** `data/<ns>/dtp/recycling/*.json`, alle Dateien werden zusammengeführt.

```json
{ "entries": [ { "item": "minecraft:leather_boots", "scrap": 7.08 } ] }
```

- Unbekannte Items werden übersprungen.
- Ein Eintrag mit `scrap` 0 entfernt das Item.
- Abschalten per Datapack: alle Einträge entfernen oder `trade_list/70_scrap.json` leeren. Ohne Handelslisten-Eintrag ist Schrott unverkäuflich.

**Schrott als Ware:** `data/dtp/dtp/trade_list/70_scrap.json`, Filtergruppe Loot.

**Code:** `dev.dtp.recycling.Recycling` (Liste, Ausschlüsse, Zufallsrundung), `dev.dtp.block.RecyclerBlock` (Blockzustand, Fenster öffnen, Knistern) und `RecyclerBlockEntity` (Fächer, Hopper-Seiten, Brenn- und Fortschrittslogik nach dem Vanilla-Ofen), `dev.dtp.menu.RecyclerMenu` und `dev.dtp.client.RecyclerScreen` (Fenster mit der Vanilla-Ofentextur, ohne Rezeptbuch).

**GameTests:** `recycler_process` (ohne Brennstoff nichts, mit Kohle nach 400 Ticks), `recycler_rejects` (Hopper-Seiten), `recycler_menu` (Shift-Klick), `recycler_chance`, `recycling_data` (Regeln der Liste: nicht auf der Handelsliste, nicht schmelzbar, Wert ≤ 40, Reparatur teurer als Schrott) und `scrap_tradable`.
