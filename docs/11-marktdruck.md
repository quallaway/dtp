# Marktdruck

> Wie sich Preise bilden und erholen: Preisformel, Ruhepreis, Preisband, Marktuhr, Spielerskalierung, Marktereignisse und die Markt-Upgrades. Die Blöcke und die Handelsliste stehen in [Markt](10-markt.md).

---

## Die Preisformel

Jede Ware trägt einen **Marktdruck** `s` — einen zeitlich abklingenden Zähler verkaufter Einheiten. Marktdruck und Preise sind **serverweit geteilt**: Verkauft ein Spieler 2.000 Weizen, sinkt der Weizenpreis für alle.

```
Preisfaktor = clamp( 2 ^ (-s / H),  market.factorMin,  market.factorMax )
Marktwert   = Basiswert × Preisfaktor
```

`H` ist die **Halbwertsmenge**: die Menge, nach deren Verkauf sich der Preis halbiert.

| Marktdruck `s` | Preisfaktor | Bedeutung |
|---|---|---|
| `-H/2` | 1,41 | Ruhepreis |
| `0` | 1,00 | Basiswert |
| `H` | 0,50 | Halbiert |
| `2H` | 0,25 | Stark gesättigt |
| `3H` | 0,125 | Kaum noch lohnend |

Beim Verkauf von `n` Einheiten steigt `s` um `n`. Der Erlös wird **Einheit für Einheit** aufsummiert, weil der Preis während des Verkaufs sinkt; abgerundet wird erst der Gesamterlös. Anzeige und Auszahlung rechnen mit derselben Formel.

---

## Erholung

Über Zeit klingt `s` exponentiell ab — nicht gegen 0, sondern gegen einen **negativen Grundwert** `sFloor`:

```
s(t+Δ) = sFloor + (s(t) − sFloor) × 2 ^ (−Δ / T)
sFloor = −H × market.floorRatio
```

`T` ist die **Halbwertszeit** der Erholung in Markttagen. Mit `market.floorRatio` = 0,5 liegt `sFloor` bei `−H/2`.

### Ruhepreis

Eine lange nicht gehandelte Ware steigt dadurch über ihren Basiswert, auf den **Ruhepreis**:

```
Ruhepreis = Basiswert × 2^(0,5) ≈ 1,41 × Basiswert
```

Der Ruhepreis ist der Preis, den man beim ersten Verkauf sieht, und der übliche Höchstwert einer Ware. Darüber kommt sie nur durch einen [Nachfrageschub](#marktereignisse); dieselbe Formel holt sie danach von oben wieder auf den Ruhepreis zurück. Vollmond und Regen heben einzelne Gruppen zusätzlich mit einem Aufschlag auf den Preisfaktor, ohne den Marktdruck zu ändern ([Vollmond und Regen](#vollmond-und-regen)).

Mit dem Upgrade *Marktinformation* zeigt die Preisübersicht die Richtung mit einem Trendpfeil:

| Pfeil | Bedeutung |
|---|---|
| ↑ | Der Preis erholt sich (Druck über `sFloor`) |
| ↓ | Der Preis liegt nach einem Marktereignis über dem Ruhepreis und sinkt zurück |
| → | Der Preis steht am Ruhepreis |

Technisch wird nichts getickt. Gespeichert werden Druck und Zeitpunkt der letzten Berechnung; der aktuelle Druck wird beim Lesen aus der seither verstrichenen Marktzeit abgeleitet.

### Zwei Parameter pro Ware

| Warentyp | `H` | `T` |
|---|---|---|
| Massenware (Weizen, Bruchstein) | hoch | kurz |
| Mittelware (Eisenbarren, Kohle) | mittel | mittel |
| Seltenware (Netherite, Netherstern) | sehr niedrig | sehr lang |

Fehlt ein Wert in der Handelsliste, gelten `market.defaultH` (500) und `market.defaultT` (3 Markttage). Konkrete Bänder je Warenklasse stehen in [Preise und Größenordnungen](61-preise-und-groessenordnungen.md).

---

## Preisband

| Config | Standard | Bedeutung |
|---|---|---|
| `market.factorMin` | 0,05 | Untergrenze: 5 % des Basiswerts |
| `market.factorMax` | 2,5 | Obergrenze: 250 % des Basiswerts |

Die Untergrenze ist wichtiger, als sie aussieht. Ein Preis von exakt 0 entwertet rückwirkend die Farm, die der Spieler dafür gebaut hat. Ein Restwert von 5 % signalisiert „gerade gesättigt", nicht „alles umsonst". Weil der Faktor nie 0 wird, gibt es keine Sackgasse: Auch aus voller Sättigung erholt sich der Preis.

Die Untergrenze gilt auch **außerhalb des Territoriums** ([Markt](10-markt.md#handel-außerhalb-des-territoriums)).

---

## Die Marktuhr

Zeit wird in **Markttagen** gemessen: ein Minecraft-Tag, 24.000 Ticks, 20 Minuten. Die Marktuhr folgt der Uhr der Oberwelt.

> Die Marktuhr läuft **nur, während mindestens ein Spieler online ist**. Ein gestoppter, pausierter oder leerer Server lässt den Markt nicht altern. Der Markt steht beim nächsten Einloggen genau dort, wo er beim Ausloggen stand.

So trägt die Sättigung vollständig über Sitzungen hinweg. Gesättigte Monsterbeute oder Schallplatten erholen sich nur durch gespielte Zeit, nicht durch Pausen zwischen den Spielabenden. `T` ist deshalb in gespielten Markttagen zu lesen; eine Sitzung von 3,5 Stunden sind rund 10,5 Markttage.

**Schlafen zählt mit.** Wer die Nacht überspringt, lässt die Welt altern: Der Marktdruck erholt sich, aber laufende Contracts verlieren an Wert. Aus einer Routinehandlung wird eine kleine Abwägung. Zwischen zwei Nächten liegt immer ein Tag, der gespielt werden muss; der Beschleunigungsfaktor bleibt dadurch begrenzt. Zurückgestellte Zeit wird ignoriert.

Dieselbe Uhr treibt den Preisverlauf und die Zählung aktiver Spieler. Die Uhren der Contracts beschreibt [Contracts](12-contracts.md).

### Preisverlauf

Einmal pro Markttag wird der Preisfaktor jeder handelbaren Ware festgehalten, auch nie verkaufter, dazu vor und nach jedem Verkauf und Marktereignis. Gespeichert werden je Ware und Markttag Tiefst-, Höchst- und letzter Wert, für die letzten 14 Markttage. Angezeigt wird der Verlauf mit dem Upgrade *Marktinformation* ([Markt](10-markt.md#preisanzeige)). Der Graph hat eine lineare Achse von der Unter- bis zur Obergrenze des Preisbands (5 % bis 250 %); eine transparente Linie markiert den Ruhepreis. Darunter haben Verkäufe den Preis gedrückt, darüber ein Marktereignis gehoben.

---

## Spielerskalierung

Bei vier Spielern wäre jede Ware viermal so schnell gesättigt wie allein. Deshalb wächst `H` mit der Zahl **aktiver Spieler**:

```
H_effektiv = H × (1 + (aktiveSpieler − 1) × market.playerScaling)
```

| Config | Standard | Bedeutung |
|---|---|---|
| `market.playerScaling` | 0,8 | Bei 1,0 verhält sich der Markt pro Spieler wie im Einzelspiel; kleinere Werte erzeugen spürbare Konkurrenz |
| `market.activeWindowDays` | 30 | Ein Spieler zählt als aktiv, wenn er in den letzten 30 Markttagen online war |

Gezählt werden also nicht die gerade eingeloggten Spieler, sondern alle, die im Zeitfenster gespielt haben — mindestens 1. Bei vier aktiven Spielern ist `H` 3,4-mal so groß. Weil Markttage nur mit Spielern online laufen, entsprechen 30 Markttage etwa drei Sitzungen.

`sFloor` wächst mit `H_effektiv` mit; der Ruhepreis bleibt bei 141 %.

---

## Marktereignisse

Im festen Takt gibt es zwei Ereignistypen: den **Nachfrageschub** und die [Schnelle Erholung](#schnelle-erholung). Dazu kommen [Vollmond und Regen](#vollmond-und-regen), die die Welt selbst auslöst. Beim Nachfrageschub steigt die Nachfrage nach einer Warengruppe, die Schnelle Erholung gilt dem ganzen Markt. Ereignisse wirken auf den Markt selbst, nicht auf das Verkaufsverhalten eines Spielers, und sie werden **angekündigt**: Ein Preissprung mit Vorwarnung ist eine Planungsaufgabe, keine Willkür.

### Nachfrageschub

Für alle handelbaren Waren der Gruppe, auch nie verkaufte, in zwei Schritten:

1. **Entlastung:** Gesättigte Waren rücken um den Anteil `events.pressureRelief` (0,5 = halber Abstand) an `sFloor` heran.
2. **Schub:** Danach sinkt der Marktdruck um `events.priceBoost` × `H` weiter, auch unter `sFloor`. Bei 0,5 steigt eine Ware vom Ruhepreis (141 %) auf 200 %.

Danach sinkt der Preis mit der Zeit wieder auf den Ruhepreis, schneller, wenn verkauft wird. Mehrere Ereignisse stapeln sich höchstens bis zur Preisobergrenze. Eine [gemeinsame Ware](10-markt.md#gemeinsame-ware) wird einmal entlastet, nicht einmal je Item.

### Takt und Countdown

- Wartezeit `events.intervalMinutes` = 180 Minuten **Echtzeit**.
- Der Countdown läuft **nur, während Spieler online sind**, und erst nach dem Start des Durchlaufs (Hauptbeacon gesetzt).
- **Contracts beschleunigen:** Jeder abgeschlossene Contract verkürzt die Wartezeit nach `events.contractMinutes` — Stufe 1 um 5, Stufe 2 um 15, Stufe 3 und 4 um 30 Minuten. Wer viel liefert, holt das Ereignis in die laufende Sitzung.
- **Wither beschleunigt:** Ein Wither, den ein Spieler besiegt, verkürzt die Wartezeit um `events.witherMinutes` (30 Minuten), aber nur einmal je geplantem Ereignis. Erst das nächste geplante Ereignis gibt den Bonus wieder frei, eine Wither-Farm holt das Ereignis also nicht beliebig heran. Umplanen per Admin-Befehl (`in`, `type`, `target`) gibt ihn nicht frei.
- **Ankündigung:** Der Marktstand zeigt in seiner ersten Zeile Typ, Ziel und Restzeit („Nachfrageschub in 42 Min.: Baumaterial"). Laufen gerade Ereignisse, steht dahinter „(+2 laufend)“; der Tooltip der Zeile listet sie mit Restzeit oder Aufschlag. Steht der Marktstand außerhalb des Territoriums, hat der Warnhinweis Vorrang. `events.warningMinutes` (10) vorher kommt eine Vorwarnung im Chat, beim Eintreten eine Meldung.

### Ziel

- **Typ zuerst:** Mit `events.fastRecovery.chance` (20 %) wird es eine Schnelle Erholung, sonst ein Nachfrageschub. Ziel und Typ stehen schon beim Planen fest.
- Die **Schnelle Erholung** gilt immer allen Waren. Sie verbraucht weder die Warteschlange neuer Gebiete noch eine Gruppe der Reihum-Auslosung.
- Ein **Nachfrageschub** gilt immer einer der **freigeschalteten** [Filtergruppen](10-markt.md#filtergruppen), nie allen Waren. Den ganzen Markt trifft nur die Schnelle Erholung, so hat jeder Typ seine eigene Rolle. Ist keine Gruppe frei, wird es eine Schnelle Erholung.
- **Reihum:** Eine Gruppe ist erst wieder Ziel, wenn alle freigeschalteten einmal dran waren. Kein Ziel kommt zweimal hintereinander, auch nicht über den Beginn einer neuen Runde hinweg.
- **Neue Gebiete:** Nether und Das Ende werden erst Ziel, wenn sie freigeschaltet sind. Dann reiht sich die Gruppe ein: Das schon angekündigte Ereignis bleibt, das **nächste** gilt ihr sicher. Werden beide frei, kommen sie in der Reihenfolge ihrer Freischaltung dran.

### Config und Befehle

| Config | Standard | Bedeutung |
|---|---|---|
| `events.enabled` | `true` | Marktereignisse an/aus |
| `events.intervalMinutes` | 180 | Wartezeit in Minuten |
| `events.contractMinutes` | 5, 15, 30 | Verkürzung je Contract-Stufe; der letzte Wert gilt für höhere Stufen |
| `events.witherMinutes` | 30 | Verkürzung durch einen besiegten Wither, einmal je geplantem Ereignis |
| `events.warningMinutes` | 10 | Vorwarnung im Chat, 0 = keine |
| `events.pressureRelief` | 0,5 | Entlastung gesättigter Waren |
| `events.priceBoost` | 0,5 | Schub in Vielfachen von `H` |
| `events.fastRecovery.chance` | 0,2 | Anteil Schnelle Erholung an allen Ereignissen |
| `events.fastRecovery.days` | 4 | Laufzeit der Schnellen Erholung in Markttagen |
| `events.fastRecovery.pressureShare` | 0,67 | Anteil des Marktdrucks, den ein Verkauf während der Schnellen Erholung erzeugt |
| `events.fullMoon.bonus` | 0,15 | Aufschlag auf Monsterbeute in Vollmondnächten, 0 = aus |
| `events.rain.bonus` | 0,15 | Aufschlag auf Fischerei bei Regen, 0 = aus |

Ist `events.enabled` aus, entfallen auch Vollmond und Regen.

| Befehl | Wirkung |
|---|---|
| `/dtp admin market event` | Geplantes Ereignis sofort auslösen, das nächste wird geplant |
| `/dtp admin market event info` | Nächstes Ereignis, dazu laufende Ereignisse und Aufschläge aus der Welt |
| `/dtp admin market event in <Minuten>` | Nächstes Ereignis terminieren, Typ und Ziel bleiben |
| `/dtp admin market event type <demand_surge\|fast_recovery>` | Typ des nächsten Ereignisses festlegen, die Wartezeit bleibt; der Nachfrageschub behält das Ziel, die Schnelle Erholung gilt allen Waren |
| `/dtp admin market event target <Gruppe>` | Ziel des nächsten Nachfrageschubs festlegen, Typ und Wartezeit bleiben; bei einer Schnellen Erholung abgelehnt |
| `/dtp admin market event fire demand_surge <Gruppe>` | Nachfrageschub sofort auslösen; das geplante Ereignis bleibt unberührt |
| `/dtp admin market event fire fast_recovery` | Schnelle Erholung für alle Waren sofort starten (ersetzt eine laufende); das geplante Ereignis bleibt unberührt |
| `/dtp admin market event end` | Laufende Ereignisse sofort beenden |

`<Gruppe>` ist der Name der Warengruppe ohne Namensraum, z. B. `farming`; die Tab-Vervollständigung zeigt alle, auch gesperrte. Vollmond und Regen haben keine eigenen Befehle: `/weather rain` bzw. `/time set 13000` (Tag 0 ist Vollmond, danach alle 8 Tage) reichen, `event info` zeigt, ob der Aufschlag gilt.

### Ereignisarten

| Art | Wirkung | Ereignisse |
|---|---|---|
| **Einmalig** | Ändert den Marktdruck `s` einmal, danach normale Erholung | Nachfrageschub |
| **Mit Laufzeit** | Ändert eine Regel für eine feste Marktzeit | Schnelle Erholung |
| **Aus der Welt** | Gilt, solange ein Zustand der Welt anhält; außerhalb des Takts | Vollmond, Regen |

Ereignisse mit Laufzeit liegen mit Typ, Ziel, Beginn und Ende (in Marktzeit) in den Marktdaten. Zwei Ereignisse **desselben** Typs laufen nie gleichzeitig: Eine neue Schnelle Erholung beendet eine noch laufende. Ereignisse aus der Welt brauchen keine Ankündigung, denn Mondphase und Wetter sind für alle sichtbar und vorhersehbar.

### Schnelle Erholung

Für alle Waren gelten `events.fastRecovery.days` (4) Markttage lang, also etwa 80 Minuten gespielt, zwei Regeln. Am Ende kommt eine Meldung im Chat.

1. **Doppelte Erholung, nur aufwärts:** Marktzeit innerhalb des Ereignisses zählt bei der Erholung doppelt, `T` halbiert sich also. Das gilt nur für Waren unter dem Ruhepreis. Gesättigte Waren werden schneller wieder verkäuflich, steigen dabei aber nicht über den Ruhepreis. Eine Ware über dem Ruhepreis, etwa nach einem Nachfrageschub, sinkt nicht schneller als sonst.
2. **Weniger Druck je Verkauf:** Ein verkauftes Stück erhöht `s` nur um `events.fastRecovery.pressureShare` (0,67). Anzeige und Auszahlung rechnen mit demselben Anteil.

**Rolle:** Der Nachfrageschub belohnt Lager. Wer vorher zurückhält, verkauft einmal teuer. Die Schnelle Erholung belohnt Durchsatz: Bei einer ruhenden Ware bewirkt die doppelte Erholung nichts, eine laufende Farm setzt im Fenster dagegen deutlich mehr ab. Simulation einer laufenden Farm, die sofort verkauft, Mehrerlös je Ereignis in `K = B × H`:

| Ware | Nachfrageschub | Schnelle Erholung |
|---|---|---|
| T = 2, Dauerpreis 75 % | +0,55 K | +0,62 K |
| T = 4, Dauerpreis 75 % | +0,54 K | +0,34 K |
| T = 4, Dauerpreis 50 % | +0,82 K | +0,65 K |
| T = 10, Dauerpreis 75 % | +0,49 K | +0,13 K |

Mit einem Lager wächst der Vorsprung des Schubs weiter. Dafür trifft die Schnelle Erholung immer alle Waren, der Schub nur eine Gruppe.

**Schlafen:** Die Marktuhr folgt der Oberweltuhr. Eine übersprungene Nacht sind etwa 0,48 Markttage und zählt im Fenster wie etwa 0,96 Erholungstage. Weil das Fenster in Marktzeit läuft, verbraucht Schlafen auch Fensterzeit. 4 Markttage bringen also immer 8 Tage Erholung, mit nächtlichem Schlafen aber in etwa 42 statt 80 Minuten Echtzeit.

Gerechnet wird ohne Ticken, mit dem Zeitfenster des Ereignisses:

```
s = sFloor + (s0 − sFloor) × 2 ^ (−(Δ + Überlappung) / T)
Überlappung = Marktzeit seit dem letzten Stand, die im Ereignisfenster liegt
```

Beim Ende wird der Druck jeder Ware der Gruppe auf ihren genauen Stand festgeschrieben, erst dann fällt das Fenster weg. Kein Preis springt, auch nicht, wenn Schlafen die Uhr über das Ende hinaus vorstellt.

### Vollmond und Regen

| Ereignis | Wann | Gruppe | Aufschlag |
|---|---|---|---|
| **Vollmond** | Vollmondphase und Nacht in der Oberwelt (Tageszeit 13.000 bis 23.000 Ticks) | Monsterbeute (`mob_drops`) | +15 % |
| **Regen** | solange es in der Oberwelt regnet, auch bei Gewitter | Fischerei (`fishing`) | +15 % |

Der Aufschlag ist ein **Multiplikator auf den Preisfaktor**, keine Änderung am Marktdruck. Alle Preise laufen durch dieselbe Rechnung (`Market.factor`): Marktstand, Preisübersicht, Erlös, Marktwaage und ihr Comparator-Signal. Der Aufschlag wird dort eingerechnet und danach wieder auf `market.factorMax` begrenzt, so zeigen ihn alle Anzeigen von selbst. Außerhalb des Territoriums bleibt es bei der Untergrenze. Endet der Regen, ist der Aufschlag sofort weg; eine Senkung von `s` würde dagegen noch lange nachwirken. Die Trendpfeile hängen weiter nur am Marktdruck, deshalb nennt der Tooltip am Marktstand den Aufschlag zusätzlich („Regen: Fischerei +15 %“).

### Verworfen

| Idee | Grund |
|---|---|
| **Spekulationsblase** (Preis steigt, platzt zu verdecktem Zeitpunkt) | Ähnelt dem Nachfrageschub zu sehr |
| **Gesucht-Liste** (Festpreis für die ersten N Stück) | Ist im Kern ein Contract |
| **Zolltag** (Marktverkauf −X %, Contracts +X %) | Bestraft Handel, statt ihn zu lenken |
| **Subvention** (Server-Upgrades zeitweise billiger) | Spieler würden Upgrade-Käufe aufschieben und auf die nächste Subvention warten |

### Vorgemerkt

Überschwemmung, Karawane und Saisonmarkt bleiben vorgemerkt, dazu:

- **Ersatzware:** Bei einem gekoppelten Paar (Kohle/Holzkohle) steigt die eine Ware, die andere fällt.
- **Großabnehmer:** `H` einer Gruppe verdoppelt.
- **Zähe Nachfrage:** `T` einer Gruppe verlängert.
- **Gemeinschaftsauftrag:** ein serverweiter Contract, an den alle gemeinsam liefern.
- **Razzia:** Der Schwarzmarkt schließt zeitweise oder wird riskanter.
- **Erstfund-Bonus:** Eine neu freigeschaltete Gruppe (Nether, Das Ende) löst einen Boom aus.

---

## Markt-Upgrades

Drei Server-Upgrades im Markt-Tab des Beacons verändern den Markt für alle. Sie sind serverweit, weil spielergebundene Preisboni in einer kooperativen Gruppe einfach über den Käufer umgangen würden. Preise und Kaufregeln stehen in [Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md).

| Upgrade | ID | Stufen | Wirkung |
|---|---|---|---|
| Langsamerer Preisverfall | `price_stability` | 2 | `H` wächst je Stufe um `market.stabilityPerLevel` (10 %) |
| Schnellere Preiserholung | `price_recovery` | 2 | `T` sinkt je Stufe um `market.recoveryPerLevel` (8 %), höchstens auf 20 % |
| Marktinformation | `market_info` | 1 | Prozentwerte, Trendpfeil, Preisverlauf, Rezept der Marktwaage |

Vollständig gelten damit:

```
H_effektiv = H × (1 + (aktiveSpieler − 1) × 0,8) × (1 + Stabilitätsstufe × 0,1)
T_effektiv = T × max(0,2, 1 − Erholungsstufe × 0,08)
```

Ein Upgrade, das die Verkaufspreise direkt erhöht, gibt es bewusst nicht: Es wäre eine reine Rückkopplung ([Balancing](62-balancing.md)).

---

## Rechengrundlagen

| Größe | Formel | Bedeutung |
|---|---|---|
| Ruhepreis `R` | `1,41 × B` | Preis einer nicht gesättigten Ware |
| Marktkapazität `K` | `B × H` | grob die Taler, die eine Ware hergibt, bevor ihr Preis spürbar fällt |
| Erlös vom Ruhepreis bis zum Basiswert | ≈ `0,6 K` | `K × (1,41 − 1) / ln 2` |
| Erlös vom Ruhepreis bis zum halben Basiswert | ≈ `1,3 K` | `K × (1,41 − 0,5) / ln 2` |
| Erlös vom Ruhepreis bis zur Untergrenze | ≈ `2 K` | `K × (1,41 − 0,05) / ln 2` |
| Dauereinkommen | höchstens ≈ `0,5 K / T` pro Markttag | bei laufendem Verkauf |

Daraus folgen die Erholungszeiten der Warenklassen:

- **T 2–3:** erholt sich innerhalb einer Sitzung.
- **T 6–12:** erholt sich über eine Sitzung etwa zur Hälfte.
- **T 20–30:** bleibt über mehrere Sitzungen gesättigt — bei Fundstücken unkritisch, bei Schallplatten aus der Creeper-Farm gewollt.

Wie sich die Werte im Spiel messen lassen (Messprotokoll `market.protocol`), beschreibt [Balancing](62-balancing.md).
