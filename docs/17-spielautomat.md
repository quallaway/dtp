# Spielautomat

> Der Spielautomat nimmt eine Münze und lässt drei Walzen laufen, für alle in der Nähe sichtbar. Danach gibt es einen
> Gewinn, einen Trostpreis oder nichts. Die Quote ist realistisch: Auf Dauer behält der Automat rund 12 % jedes
> Einsatzes. Er ist ein Zeitvertreib und eine Talersenke, keine Geldquelle.

---

## Rolle

Der [Schwarzmarkt](14-blackmarket.md) ist ein Glücksspiel mit einem Zweck: bessere Verzauberungen. Der Spielautomat hat keinen Zweck außer dem Spiel selbst. Er ist für den Moment gedacht, in dem die Gruppe an der Basis steht und einer „nur noch einen Taler“ einwirft.

**Bezug zur Vision:**
- **Talersenke:** Die Auszahlungsquote liegt unter 100 %. Wer lange spielt, verliert sicher. Damit kann der Automat keine Rückkopplung erzeugen ([Mod und Vision](01-mod-und-vision.md#anti-ziele)): Er vermehrt keine Taler und füttert keine Produktion.
- **Kein Verbrauchsgut:** Für Taler gibt es nur Taler zurück oder nichts. Der Automat verkauft nichts und berührt die Ausnahme „Keine Verbrauchsgüter für Taler“ nicht.
- **Gemeinsam statt allein:** Er hat keine Oberfläche. Walzen, Hebel und Geräusche nehmen alle Spieler im Umkreis wahr. Meldungen gibt es keine: Was passiert ist, hört man.

---

## Der Block

**Spielautomat** (`dtp:slot_machine`). Ein Eisengehäuse mit rotem Schild, vertieftem Walzenfenster, Münzschlitz, Auswurfschale und einem Hebel an der rechten Seite. Abgebaut wird er mit der Spitzhacke, ohne passendes Werkzeug fällt nichts. Der Block hat kein Inventar und keine Oberfläche. Das Rezept steht nicht von Anfang an im Rezeptbuch, das Handbuch zeigt es.

**Rezept** (geformt):

```
Eisenbarren   Glasscheibe   Eisenbarren
Goldbarren    Kupfersack    Hebel
Eisenbarren   Redstone      Eisenbarren
```

---

## Ablauf

1. **Einwerfen:** Rechtsklick mit einem der neun Taler. Genau eine Münze aus dem Stapel ist der Einsatz, egal welche Stufe. Im Kreativmodus kostet es nichts.
2. **Drehen:** Der Hebel geht runter, die Walzen laufen. Walze 1 hält nach 1,5 Sekunden, Walze 2 nach 2,25 und Walze 3 nach 3 Sekunden. Jeder Stopp klackt hörbar.
3. **Auszahlen:** Mit dem letzten Stopp fällt der Gewinn vorn aus der Auswurfschale, als Münzen in möglichst wenigen Stücken. Er fällt auch dann, wenn der Spieler schon gegangen ist, und unabhängig von der Gamerule für Blockdrops. Alle Spieler im Umkreis von 16 Blöcken lesen das Ergebnis über der Hotbar.

Während sich die Walzen drehen, nimmt der Automat nichts an und verbraucht nichts. Ein Rechtsklick ohne Taler zeigt nur einen Hinweis.

**Abbauen während einer Drehung:** Der Einsatz ist verloren, es gibt keine Auszahlung. Eine Drehung übersteht dagegen Chunk-Entladen und Serverneustart: Sie ist im Block gespeichert und wird beim nächsten Laden aufgelöst.

---

## Gewinnplan

Der Ausgang wird beim Einwurf gewürfelt, danach werden die passenden Symbole gewählt. Die Quote hängt damit nur an der Config, nicht an der Anordnung von Walzenstreifen.

| Ausgang | Walzen | Chance | Auszahlung |
|---|---|---|---|
| Jackpot | 3 × Goldtaler | 0,5 % | Einsatz × 81, zwei Stufen höher |
| Gewinn | 3 gleiche andere Symbole | 5 % | Einsatz × 9, eine Stufe höher |
| Trostpreis | Walze 1 und 2 gleich, Walze 3 anders | 20 % | Einsatz ÷ 9, eine Stufe kleiner |
| Nichts | Walze 1 und 2 verschieden | 74,5 % | – |

- **Symbole:** Goldtaler, Diamant, Smaragd, Glocke, Süßbeeren und Melonenscheibe. Es sind Vanilla- bzw. DTP-Items, ein Resourcepack ändert sie also mit.
- **Stufen:** Weil jede Talerstufe das Neunfache der vorigen ist, ergibt jede Auszahlung genau eine Münze der Nachbarstufe. Aus einem Silbertaler werden ein Silberbeutel (Gewinn), ein Silbersack (Jackpot) oder ein Kupfersack (Trostpreis).
- **Kupfertaler:** Eine kleinere Münze gibt es nicht. Der Trostpreis bringt dort nichts und klingt deshalb wie eine Niete.
- **Beinahe-Treffer:** „Nichts“ darf auf Walze 3 wieder das Symbol von Walze 1 zeigen, auch den Goldtaler. So sieht es bei echten Automaten auch aus.

**Auszahlungsquote** = 81 × Jackpot + 9 × Gewinn + Trostpreis ÷ 9 = 0,405 + 0,45 + 0,022 ≈ **87,7 %**, beim Kupfertaler ≈ 85,5 %. In einem Viertel der Spiele kommt etwas heraus, und im Mittel ist jede 200. Drehung ein Jackpot.

**Große Einsätze:** Ein Goldsack-Jackpot zahlt 81 Goldsäcke aus. Das ist gewollt: Der Erwartungswert bleibt negativ, der Automat lohnt sich also nie. Wer einen Goldsack einwirft, verliert ihn mit 74,5 % Wahrscheinlichkeit ganz.

Die Chancen nennt der Block nicht. Das Handbuch beschreibt nur, welche Symbole gewinnen, und warnt: „Auf Dauer gewinnt der Automat.“

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `slotMachine.chanceJackpot` | 0,005 | Chance auf den Jackpot |
| `slotMachine.chanceWin` | 0,05 | Chance auf einen Gewinn |
| `slotMachine.chanceConsolation` | 0,2 | Chance auf den Trostpreis |

Ergeben die drei Chancen zusammen mehr als 1, fallen die hinteren der Reihe nach weg. Wer die Werte ändert, sollte die Quote unter 1 halten. Darüber wird der Automat zur Geldquelle.

---

## Technik

- **Logik:** `SlotMachine` (Würfeln, Symbole, Auszahlung). Zustand der Drehung in `SlotMachineBlockEntity`: Einwurf-Tick, Einsatz, Ausgang, Symbole, Spieler.
- **Animation ohne eigenes Paket:** Beim Einwurf schickt der Server einmal das Block-Update an alle, die den Chunk sehen. `SlotMachineRenderer` rechnet die Walzen danach allein aus der Spielzeit. Die Symbole laufen von oben durch das Fenster, und der 1 px tiefe Rahmen des Modells verdeckt, was über oder unter dem Fenster liegt.
- **Hebel:** Blockstate `spinning`, eigenes Modell `slot_machine_spinning`.
- **Geräusche und Partikel:** nur Vanilla. Klackern beim Drehen (Hi-Hat), Bassdrum bei jedem Stopp. Ergebnis: Fanfare „Herausforderung abgeschlossen“ mit doppelter Hörweite und Totem-Partikeln (Jackpot), Amethyst mit Dorfbewohner-Partikeln (Gewinn), Erfahrungskugel (Trostpreis), tiefer Bass mit Rauch (nichts, auch Trostpreis beim Kupfertaler). Klick ohne Taler: leises Holz-Klacken. Klick während der Drehung: Fehlklick des Spenders.
- **Keine Meldungen:** Der Automat erklärt sich allein über Töne, Walzen und Hebel.
- **Messprotokoll:** Jede Drehung schreibt `slot_machine_<ausgang>` mit Einsatzstufe und ausgezahltem Betrag ([Balancing](62-balancing.md)).

---

## Tests

Die GameTests `slot_machine_odds` (Anteile, Quote < 1, Symbole passend zum Ausgang), `slot_machine_payout` (alle Stufen und Ausgänge) und `slot_machine_spin` (Einwurf am Block, Sperre während der Drehung, Auszahlung vor der Front) decken den Automaten ab ([Entwicklung und Test](71-entwicklung-und-test.md)).
