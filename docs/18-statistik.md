# Statistik-Tafel

> Die Statistik-Tafel zeigt, was der Server verkauft hat, wer welche Contracts abgeschlossen hat und wie viele Taler die Welt an den letzten Tagen eingenommen hat. Sie zeigt nur an:
> Sie hat kein Inventar, nimmt kein Geld und ändert nichts am Markt.

---

## Rolle

Der Marktstand sagt, was eine Ware **jetzt** wert ist. Die Statistik-Tafel sagt, was sie dem Server **eingebracht hat**. Damit sieht die Gruppe, welche Produktionsketten tragen und wo eine einzelne Farm alles andere überholt.

**Bezug zur Vision:**
- **Vielfalt statt Monokultur:** Eine Liste, die eine einzige Ware anführt, ist das sichtbare Gegenstück zum Melonenfarm-Szenario ([Mod und Vision](01-mod-und-vision.md)). Die Tafel bewertet nicht, sie zeigt nur.
- **Gemeinsam:** Sie zählt serverweit. Die Contract-Tabelle zeigt, wer was beigetragen hat, und gibt dafür keinen Vorteil.
- **Kein Eingriff:** Sie greift nicht in Vanilla ein, darum braucht sie keine Abschaltung in der Config.

---

## Der Block

**Statistik-Tafel** (`dtp:statistics_board`). Ein voller Block mit `facing` wie das Bulletin-Board. Die Front zeigt einen einzelnen Flipchart-Bogen unter einer Eisen-Klemmleiste, ein Säulendiagramm in Tinte und einen kleinen Taler. Abgebaut wird sie mit der Axt. Sie hat kein Inventar. Das Rezept steht nicht von Anfang an im Rezeptbuch, das Handbuch zeigt es.

**Rezept** (geformt):

```
Eisennugget   Eisennugget   Eisennugget
Papier        Papier        Papier
Bretter       Kupfertaler   Bretter
```

---

## Anzeige

Rechtsklick öffnet die Oberfläche. Reiter, Zeitraum, Warengruppe, Sortierung und Scrollen wählt jeder Spieler für sich. Der Server bekommt davon nichts mit.

### Verkäufe

- **Zeitraum:** **14 Tage** (der heutige und die 13 vorherigen Markttage, wie der Preisverlauf, siehe [Marktdruck](11-marktdruck.md)) oder **Gesamt**. Ein einzelner Knopf rechts neben den Gruppenknöpfen zeigt den gewählten Zeitraum und wechselt ihn bei jedem Klick. Er gehört nur zu diesem Reiter, denn Contracts zählen immer insgesamt, und die Einnahmen zeigen fest 14 Tage.
- **Warengruppen:** dieselbe Knopfleiste wie am Marktstand. „∗“ zeigt alle Gruppen, die übrigen Knöpfe die freigeschalteten Gruppen.
- **Summenzeile:** Taler und Stück der gewählten Gruppe im gewählten Zeitraum.
- **Tabelle:** Ware, Stück und Taler. Ein Klick auf **Stück** oder **Taler** sortiert nach dieser Spalte. Ein weiterer Klick kehrt die Richtung um (▼ absteigend, ▲ aufsteigend). Standard ist Taler ▼. Der Balken unter jeder Zeile zeigt den Wert der sortierten Spalte im Verhältnis zum größten.

**Was zählt:** jeder Verkauf am Marktstand, egal ob aus der Hand, aus Trade-Chests oder per Redstone. Verkäufe außerhalb des Territoriums zählen auch, denn sie bringen Taler. Gezählt wird je echtem Item, nicht je gemeinsamer Ware: Eichen- und Birkenstamm stehen getrennt. Contract-Belohnungen sind keine Verkäufe und zählen hier nicht, wohl aber unter [Einnahmen](#einnahmen).

### Contracts

- **Abgeschlossene Contracts:** die serverweite Zahl, dieselbe, die die Freischaltung der Stufen bestimmt ([Contracts](12-contracts.md)).
- **Tabelle:** je Spieler die Abschlüsse je Stufe (S1–S4) und ihre Summe Σ, absteigend nach Summe. Contracts zählen immer insgesamt, ohne Zeitraum.
- **Ohne Zuordnung:** Contracts aus der Zeit vor der Statistik-Tafel, deren Spieler sich seitdem nicht eingeloggt hat. Beim nächsten Login werden sie dem Spieler zugeordnet (siehe [Datenhaltung](#datenhaltung)).

### Einnahmen

Ein Balkendiagramm über die letzten 14 **Minecraft-Tage**: links der älteste, rechts der heutige Tag.

- **Ein Balken je Tag.** Er stapelt zwei Quellen: unten in Gold den Erlös aus Verkäufen am Marktstand, darüber in Grün die ausgezahlten Contract-Belohnungen. Spielautomat und Schwarzmarkt zählen nicht.
- **Tag** heißt hier der Tageszähler der Overworld, den man in F3 sieht, nicht der Markttag. Schlafen beginnt also einen neuen Balken. Wird die Uhr mit `/time` zurückgestellt, verwirft die Tafel beim nächsten Zählen die Tage, die jetzt „in der Zukunft“ liegen.
- **Achse:** Sie reicht bis zum stärksten Tag, aufgerundet auf 1, 2 oder 5 × 10ⁿ. Zwei Gitterlinien (Hälfte und Obergrenze) geben die Größenordnung.
- **Hover:** Die Spalte unter der Maus wird hervorgehoben. Der Tooltip zeigt den Tag, die Summe und die Aufteilung nach Verkäufen und Contracts.
- **Kopfzeile:** die Summe der 14 Tage. Ohne Einnahmen steht statt des Diagramms ein Hinweis.
- Der laufende Tag ist etwas heller als die vergangenen.

---

## Datenhaltung

SavedData `dtp:statistics` (`StatisticsData`, Version 2):

| Feld | Inhalt |
|---|---|
| `all_time` | Stück und Taler je Item seit der ersten Aufzeichnung |
| `days` | Stück und Taler je Item je Markttag. Gespeichert werden nur die letzten 14 Markttage, ältere werden beim nächsten Verkauf verworfen |
| `players` | je Spieler-UUID der zuletzt gesehene Name und die Abschlüsse je Stufe |
| `income` | je Minecraft-Tag der Erlös aus Verkäufen und die Contract-Belohnungen, nur die letzten 14 Tage. Ältere und spätere Tage werden beim nächsten Zählen verworfen (seit Version 2) |

- **Verkäufe** zählt `Market.sell`, die eine Stelle, durch die jeder Verkaufsweg läuft.
- **Contracts** zählt `Contracts.complete`, samt Lohn für die Einnahmen. Gezählt wird der volle Lohn, egal ob er in der Einzahlkiste oder im Inventar landet.
- **Altbestände:** Vor der Statistik-Tafel standen die Abschlüsse je Stufe nur im Spieler-Attachment (`PlayerContracts`). Beim ersten Login nach dem Update übernimmt die Tafel sie einmal. Danach hat der Spieler einen Eintrag, und der Login aktualisiert nur noch den Namen. Verkäufe beginnen bei null, weil vorher keine Mengen gespeichert wurden.
- **Namen:** Gespeichert wird der Name beim Abschluss und beim Login. Fehlt er, fragt die Tafel den Namens-Cache des Servers.
- **Netzwerk:** Die Tafel nutzt das gemeinsame Menü-Paket (`MenuSync`). Den Datenstand baut sie nur neu, wenn sich die Statistik, der Markttag, der Minecraft-Tag, die freigeschalteten Gruppen oder die Contract-Gesamtzahl ändern.

---

## Tests

| GameTest | Prüft |
|---|---|
| `statistics_sales` | Ein Verkauf zählt insgesamt und im Zeitraum. Nach 14 Markttagen fällt er aus dem Zeitraum, insgesamt bleibt er stehen. |
| `statistics_seed` | Altbestände werden genau einmal übernommen, neue Abschlüsse zählen, der Name wird aktualisiert. |
| `statistics_snapshot` | Gruppen wie am Marktstand, Spieler nach Summe sortiert, Rest ohne Zuordnung, nie negativ. |
| `statistics_income` | Verkäufe zählen als Einnahme des Tages. Immer 14 Tage mit Nullen für leere Tage, alte und „zukünftige“ Tage fallen weg. Datenstand mit 14 Einträgen, Speicherformat v1 → v2. |
| `contract_delivery` | Ein Abschluss am Bulletin-Board landet auch in der Statistik, sein Lohn in den Einnahmen des Tages. |
| `data_loaded`, `onboarding` | Rezept und Loot-Table vorhanden, Rezept nicht im Rezeptbuch. |

Mehr dazu in [Entwicklung und Test](71-entwicklung-und-test.md).
