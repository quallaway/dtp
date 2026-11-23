# Balancing

> Die Leitplanken, nach denen Preise und Grenzen gesetzt werden. Die Zahlen dazu stehen in [Preise und Größenordnungen](61-preise-und-groessenordnungen.md).

---

## Der zentrale Satz

**Upgrades, die die Talereinnahmen erhöhen, müssen vorsichtiger skaliert werden als Upgrades, die Komfort oder Kampfkraft erhöhen.**

Das ist die wichtigste Regel des gesamten Mods. Alles Weitere ist ihre Ausführung.

---

## Warum die Rückkopplung alles dominiert

Der Mod hat einen geschlossenen Kreislauf:

```
Produzieren → Verkaufen → Taler → Upgrade → mehr produzieren → ...
```

Solange ein Upgrade nur Komfort bringt, ist der Kreislauf gebremst: Die Einnahmen bleiben gleich, das nächste Upgrade dauert genauso lange. Die Progression verläuft linear in der Spielzeit.

Sobald ein Upgrade die Einnahmen erhöht, wird aus dem Kreis eine Spirale. Ein Upgrade, das 20 % mehr einbringt, verkürzt die Zeit bis zum nächsten um 17 % — und das nächste bringt wieder mehr.

Das Versagensbild: Die Gruppe spart lange auf die ersten Produktions-Upgrades, und danach kippt das Spiel innerhalb weniger Sitzungen. Alles Weitere ist sofort bezahlbar, der Markt ist bedeutungslos. Ein Mod, der auf Monate ausgelegt ist, endet nach drei Wochen.

Das passiert nicht durch schlechtes Balancing einzelner Zahlen. Es passiert strukturell, sobald Produktions-Upgrades nach derselben Kurve bepreist werden wie alles andere.

---

## Die drei Klassen

Jedes Upgrade gehört in eine von drei Klassen. Sie sind **Richtlinien** für die Einordnung, keine Formel: Die tatsächlichen Preise stehen als `costs`-Listen in den Upgrade-Dateien und sind an den Fortschrittskategorien ausgerichtet ([Regeln für Upgrade-Preise](61-preise-und-groessenordnungen.md#regeln-für-upgrade-preise)).

### Klasse A — Komfort

Spart Zeit oder Klicks, erhöht aber nicht die Einnahmen pro Zeiteinheit.

Beispiele: Federfall, der Wegebonus des Stallmeisters, Baumeister mit Rückbau, Schwimmtempo, die Bewegungs-Skills.

- Flacher Anstieg von Stufe zu Stufe (Richtwert Faktor 1,4–1,5)
- Darf früh und günstig sein

**Vorsicht bei der Einordnung:** Manches, was nach Komfort aussieht, ist keiner. Baumfällen spart Klicks, liefert aber auch mehr Holz pro Stunde — Klasse B.

### Klasse B — Produktion

Erhöht die Einnahmen pro Zeiteinheit, direkt oder indirekt.

Beispiele: Vein Mining, Baumfällen, Fortune+, Looting+, Lure+, Luck of the Sea+, Double Bobbers, Angelschatz, Ofentempo, Landwirtschaft, Imkerei, Mending-Aura.

- Steiler Anstieg (Richtwert Faktor 2–3 je Stufe)
- Niedrige Maximalstufen, die obersten Stufen sind Endspiel-Ziele
- Wirkung je Stufe klein

### Klasse C — Charakter und Kampf

Macht den Spieler stärker, ohne die Wirtschaft zu berühren.

Beispiele: Maximale Gesundheit, Schutz, Haltbarkeit, Beacon-Effekt Resistenz, Schutzschild, Heilimpuls.

- Mittlerer Anstieg (Maximale Gesundheit: Faktor 1,5)
- Harte Maximalstufen

Klasse C ist wirtschaftlich unkritisch, aber spielerisch heikel: Ein Spieler mit vielen Zusatzherzen, Resistenz und Schutz kann kaum noch sterben. Da der Mod bewusst **keine** stärkeren Gegner mitbringt, liegt die Grenze hier allein bei den Maximalstufen.

Verzauberungsboni dieser Klasse bekommen keine Stufen, die mit Vanilla-Maximum nur verschenkt wären: Schutz hat eine Stufe, weil schon +1 ein Protection-IV-Set an den 80-%-Deckel bringt. Haltbarkeit endet bei +2, damit Mending fern der Basis seinen Platz behält ([Zahlen](30-persoenliche-upgrades.md#stärke-der-ausrüstungs-upgrades)).

---

## Regeln für Klasse B

### 1. Stufen ersetzen, statt sich zu vervielfachen

Der Wert eines Upgrades steht je Stufe fest im Datapack (`values`, z. B. *Reiche Ernte* 10 / 20 / 30 %). Eine höhere Stufe ersetzt die niedrigere, sie multipliziert sie nicht. Die Boni auf Verzauberungen (Fortune+, Looting+, Lure+, Luck of the Sea+) erhöhen die Verzauberungsstufe um einen festen Betrag.

Der Unterschied wirkt klein und wächst exponentiell: `1,2 × 1,2 × 1,2 = 1,728` gegen `1 + 0,2 + 0,2 + 0,2 = 1,6`. Bei acht Stufen steht 4,3 gegen 2,6.

### 2. Keine Verkaufsboni

**Es gibt keine Upgrades, die Verkaufspreise anheben** — weder global noch je Kategorie.

Kategoriebezogene Preisboni scheitern am kooperativen Spielstil: Kauft einer „bessere Erzpreise“, verkaufen die anderen ihre Erze über ihn. Der Bonus wirkt für alle, bezahlt hat einer, und der Umweg ist lästiges Mikromanagement ohne Spielwert. Wirtschaftliche Upgrades, die alle betreffen, sind deshalb **Server-Upgrades** und werden gemeinsam bezahlt ([Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md)).

**Die generelle Linie:** Lieber den **Ertrag** erhöhen als den **Preis**. Mehr Ernte pro Feld ist eine Produktionsverbesserung, die der Markt anschließend normal bewertet. Ein höherer Preis umgeht den Markt und hebelt damit die wichtigste Bremse des Mods aus.

### 3. Kleine Schritte

Lieber mehrere kleine Stufen als wenige große. Häufigere Erfolgserlebnisse, feinere Steuerung, kleinere Folgen bei Fehleinschätzungen.

---

## Der Markt als zweite Bremse

Die Preisdynamik aus [Marktdruck](11-marktdruck.md) bremst die Rückkopplung von selbst: Wer mehr von einem Item produziert, drückt dessen Preis.

**Das ist die wirksamste Einzelbremse des Mods** — und der Grund, warum Preisuntergrenze und Erholungsrate nicht zu großzügig sein dürfen.

Sie hat eine Lücke: Wer **viele verschiedene** Items produziert, umgeht die Sättigung. Das ist ausdrücklich gewollt — es ist das Kernziel des Mods. Daraus folgt: Die Obergrenze des Fortschritts wird durch Produktionsvielfalt bestimmt, und Vielfalt kostet Aufbauzeit. Genau dort soll die Progression liegen.

Die **kuratierte Handelsliste** schließt dabei die Hintertür: Ohne sie ließe sich Vielfalt vortäuschen, indem man einen einzigen Rohstoff in ein Dutzend Crafting-Varianten verwandelt ([Handelsliste — Bewertungsverfahren](60-handelsliste-bewertung.md)).

Wie stark die Bremse je Ware greift, legen die **Warenklassen** fest ([Preise und Größenordnungen](61-preise-und-groessenordnungen.md#warenklassen)): Landwirtschaft und Massenware sind billig und sättigen langsam, farmbare Monsterbeute und Metalle sind wertvoller und sättigen schnell, Fundstücke sind hoch bepreist und kaum von der Sättigung betroffen. Damit die Sättigung über Sitzungen hinweg trägt, läuft die Marktuhr nur, während Spieler online sind — Pausen zwischen den Sitzungen erholen den Markt nicht.

Der **Chunkloader** folgt derselben Regel. Er hält Farmen nur am Laufen, solange jemand online ist, und kostet pro Laufzeit Taler ([Preise](61-preise-und-groessenordnungen.md#wiederkehrende-kosten-chunkloader)). Er vergrößert keine Farm, sondern verhindert nur, dass sie stillsteht, während die Gruppe woanders ist. Seine Ware trifft auf denselben Marktdruck. Deshalb zählt er weder zu Klasse B noch braucht er eine eigene Bremse: Mehr Laufzeit an einer Monokultur-Farm drückt deren Preis genauso wie eine größere Farm.

---

## Keine Einnahmen-Obergrenze

Ein harter Deckel auf Taler pro Zeiteinheit wäre der sicherste Schutz — und ist trotzdem **nicht** eingebaut.

Er trifft genau die aufwendig gebauten Produktionsnetze, die der Mod belohnen will, und bestraft damit gute Spieler. Vor allem aber verdeckt er Fehler, statt sie zu beheben: Wenn ein Deckel nötig ist, damit die Wirtschaft nicht kippt, ist die Wirtschaft bereits kaputt und der Deckel versteckt es nur.

---

## Kalibrierung

Geprüft wird im Spiel, nicht am Papier. Alle Zahlen sind ohne Neukompilierung änderbar:

| Was | Wo |
|---|---|
| Basiswerte, `H`, `T` | Handelsliste `data/dtp/dtp/trade_list/*.json` |
| Upgrade-Kosten, Stufen, Werte | `data/dtp/dtp/upgrade/*.json`, `data/dtp/dtp/player_upgrade/*.json` |
| Contract-Vorlagen | `data/dtp/dtp/contract/*.json` |
| Marktparameter (Preisband, `floorRatio`, Spielerskalierung), Contract-Fristen und -Werte, Grenzradien, Effektwerte | Config `dtp-synced.toml` |

Datapack-Änderungen greifen nach `/reload`. Eine Balancing-Runde zwischen zwei Sitzungen braucht damit keine neue Jar.

**Zielgröße:** 150–300 Spielstunden für einen Durchlauf, entsprechend 8–10 Monaten bei 3–4 Stunden ein- bis zweimal pro Woche. Bewusst langsam.

### Messprotokoll

Mit `market.protocol = true` (Standard) schreibt der Server jeden Verkauf (Hand, Trade-Chest, Redstone), jeden Upgrade-Kauf, jede Contract-Annahme, jeden Abbruch und Abschluss, den Start des Durchlaufs, Schwarzmarkt-Handel, Modulwechsel und Marktereignisse als Zeile nach `<welt>/dtp/protocol.csv`:

```
real_time,market_day,player,action,subject,count,amount
```

Zwischen zwei Sitzungen lässt sich daraus ablesen, wer womit wie viel verdient und wofür ausgegeben hat. Die Prüfgrößen:

| Größe | Erwartung |
|---|---|
| Zeit bis zum ersten Upgrade | Erste Sitzung |
| Zeit zwischen Upgrades im mittleren Spiel | Soll über die Zeit **leicht steigen**, nicht fallen |
| Einnahmen pro Stunde im späten Spiel | Höchstens ein Vielfaches des Anfangswerts, keine Größenordnung |
| Anteil der Einnahmen aus dem meistverkauften Item | Soll sinken, wenn die Gruppe fortschreitet |

Die zweite Zeile ist der eigentliche Test. Schrumpfen die Abstände zwischen Käufen, ist die Rückkopplung außer Kontrolle — unabhängig davon, wie die Einzelzahlen aussehen.

Die Prüfgrößen gelten für die Gruppe. Mehr Spieler vergrößern über die Spielerskalierung das `H` jeder Ware, liefern aber auch mehr Menge.
