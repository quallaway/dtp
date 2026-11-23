# Mod und Vision

> Was DTP ist, wofür es gebaut ist und nach welchen Grundsätzen es funktioniert.

---

## Der Mod

**Dynamic Trade & Progression** (kurz **DTP**) ist ein Minecraft-Mod für **NeoForge, Minecraft 26.3**. Er legt einen geteilten Markt, Lieferaufträge und dauerhafte Upgrades über das normale Spiel.

Der Kreislauf ist einfach: Spieler produzieren Waren, verkaufen sie am Marktstand gegen **Taler** und geben die Taler für permanente Upgrades aus — gemeinsam für den Server am Beacon, persönlich an der Werkstatt. Der Markt reagiert auf jeden Verkauf: Was oft verkauft wird, verliert an Wert, was lange liegt, gewinnt.

| Thema | Rahmen |
|---|---|
| Plattform | NeoForge, Minecraft 26.3 |
| Zielgruppe | 2–4 Freunde auf einem gemeinsamen Server, kooperativ |
| Spielrhythmus | Sitzungen von 3–4 Stunden, ein- bis zweimal pro Woche |
| Dauer eines Durchlaufs | 8–10 Monate, rund 150–300 Spielstunden |
| Server | Läuft nicht durchgehend, startet und stoppt automatisch |
| Einzelspieler | Funktioniert, ist aber nicht das Balancing-Ziel |
| Währung | Neun Taler-Items: Kupfer, Silber, Gold in je drei Formen |
| Sprache | Deutsch, Fachbegriffe englisch |

Die Gruppe deckt sehr unterschiedliche Spielweisen ab: Erkunden, Farmen, Angeln, Kämpfen, Tierhaltung, Automatisieren, Bauen. Jeder soll den Teil ausbauen können, der ihm Spaß macht, und trotzdem zum gemeinsamen Fortschritt beitragen.

Einzelspielerwelten gleicht der geteilte Markt weitgehend selbst aus: Mehr Spieler verkaufen mehr und drücken die Preise, brauchen aber auch mehr Taler für ihre persönlichen Upgrades. Die Sättigung skaliert zusätzlich mit der Zahl aktiver Spieler ([Marktdruck](11-marktdruck.md#spielerskalierung)).

---

## Die Idee

### Der Leitsatz

> **Minecraft soll den Spieler dafür belohnen, eine vielfältige und wachsende Produktionskette aufzubauen, anstatt die effizienteste einzelne Farm unendlich zu skalieren.**

Alles im Mod leitet sich aus diesem Satz ab. Widerspricht eine Designentscheidung ihm, ist die Entscheidung falsch — nicht der Satz.

### Das Problem

Minecraft-Automatisierung hat ein Zielproblem. Eine Farm läuft, die Truhen füllen sich — und dann? Die übliche Antwort ist: noch eine Farm, größer. Sobald die effizienteste Farm gefunden ist, gibt es keinen Grund mehr, etwas anderes zu bauen.

Der eigentliche Mangel: **Ressourcen haben keinen Zweck jenseits ihrer selbst.** 10.000 Eisenbarren sind nicht besser als 1.000, wenn es nichts gibt, wofür man sie ausgeben könnte.

### Die Antwort

Ein Markt, der viel aufnimmt — aber nicht alles und nicht unbegrenzt.

Was häufig verkauft wird, verliert an Wert; was lange nicht gehandelt wurde, gewinnt ([Marktdruck](11-marktdruck.md)). Die Frage „Was baue ich als Nächstes?" beantwortet sich dadurch von selbst, und jedes Mal anders. Die Antwort ist nie „dieselbe Farm nochmal".

Dazu kommen **Contracts** am Bulletin-Board: Sie fordern gezielt bestimmte Waren und zahlen dafür deutlich mehr als der freie Markt ([Contracts](12-contracts.md)). Marktsättigung bestraft Monokultur passiv, Contracts belohnen Breite aktiv.

### Die kuratierte Handelsliste

Der Markt handelt eine **kuratierte Liste**, nicht beliebige Items. Das ist notwendig, keine Sparmaßnahme.

Wäre alles verkaufbar, entstünde Vielfalt aus dem Nichts: Eine Holzfarm liefert über Crafting auch Bretter, Stöcke, Truhen, Türen, Treppen, Zäune und Boote — ein Dutzend formal verschiedener Waren aus einer Quelle. Die Marktsättigung liefe ins Leere.

Verkauft werden deshalb **Rohstoffe und Erzeugnisse**, nicht ihre Crafting-Permutationen. Grundsatz: **ein Eintrag pro Produktionsweg** ([Markt](10-markt.md#die-handelsliste), Bewertungsverfahren in [Handelsliste bewerten](60-handelsliste-bewertung.md)).

---

## Die Vision für Effekte und Verbesserungen

### Zwei Ebenen: Server und Spieler

| | **Server-Upgrade** | **Spieler-Upgrade** |
|---|---|---|
| Wirkt für | Alle | Den Käufer |
| Bezahlt von | Allen gemeinsam | Dem Käufer |
| Gekauft am | Beacon | Werkstatt |
| Beispiele | Grenze, Beacon-Effekte, Nether-Zugang, Marktverhalten, Contract-Stufen | Gesundheit, Tempo, Werkzeugfähigkeiten, Module, Contract-Kapazität |

**Warum wirtschaftliche Upgrades serverweit sind:** In einer kooperativen Gruppe funktionieren spielergebundene Wirtschaftsboni nicht. Kauft einer „bessere Erzpreise", verkaufen die anderen ihre Erze einfach über ihn. Der Bonus wirkt faktisch für alle, bezahlt hat nur einer, und der Umweg ist Mikromanagement ohne Spielwert. Was ohnehin allen zugutekommt, gehört deshalb allen und wird von allen bezahlt. Weil Taler Items sind, ist das wörtlich möglich: Jeder legt etwas in die Einzahlkiste.

**Warum es trotzdem persönliche Upgrades gibt:** Charakterwerte, Komfort und Fähigkeiten lassen sich nicht über einen anderen Spieler umgehen. Sie geben jedem ein eigenes Ziel und erlauben unterschiedliche Schwerpunkte in derselben Gruppe. Gekauft wird an einem eigenen Block, der **Werkstatt**, damit der Marktstand reiner Handel bleibt.

Jeder darf jedes Server-Upgrade auslösen, ohne Zustimmungsverfahren. Alle Upgrades sind permanent und nützlich — ein Fehlkauf verzögert, verbaut aber nichts.

### Permanent und exponentiell teurer

Taler werden für **permanente Freischaltungen** ausgegeben. Jeder Kauf verändert den Spielzustand dauerhaft. Ein Verbrauchsmarkt würde Taler zu einer zweiten Ressourcenwährung machen und zum Horten einladen; permanente Käufe geben eine klare Richtung.

Stufen werden **exponentiell teurer**. So reicht die Progression über Monate realer Spielzeit, und jede nächste Stufe bleibt ein spürbares Ziel. Es gibt keine Rückerstattung.

### Produktions-Upgrades sind strenger bepreist

Ein Upgrade, das die Einnahmen erhöht, finanziert seine Nachfolger. Diese **Rückkopplung** ist das einzige Problem, das den Mod strukturell zerstören kann. Produktions-Upgrades sind deshalb deutlich teurer als Komfort-Upgrades gleicher Wirkung, und der Markt bremst zusätzlich: Mehr Ware drückt den Preis ([Balancing](62-balancing.md)).

### Kein Endgame: Grenzstufe 11 als Abschluss

Die Welt darf ein Ende haben, und alle Upgrades dürfen gekauft werden. Es gibt kein Prestige und keine künstlichen Dehnungsmechaniken. Prestige wäre mathematisch immer die beste Strategie, stammt aus Idle-Games und passt nicht zu einer Welt, in der die Spieler gebaut haben.

Die Kurve zielt auf 150–300 Spielstunden. Die letzten Upgrades sollen sich wie ein Abschluss anfühlen. Das Finale ist **Grenzstufe 11**: Sie entfernt die World-Border vollständig, danach steht die Welt offen ([Territorium](20-territorium-und-serverweite-effekte.md)).

---

## Die fünf Versprechen

**1. Automatisierung bekommt ein Ziel.**
Eine Farm produziert nicht mehr ins Leere. Was sie herstellt, lässt sich in Fortschritt umwandeln.

**2. Vielfalt schlägt Spezialisierung.**
Mehrere Produktionslinien bringen mehr ein als eine große — nicht durch Verbot, sondern weil der Markt es so bewertet und Contracts Breite verlangen.

**3. Es gibt immer ein nächstes Ziel.**
Upgrades sind permanent, exponentiell teurer und reichen über Monate realer Spielzeit.

**4. Die Basis wächst mit.**
Fortschritt heißt nicht nur höhere Zahlen, sondern mehr Fläche, ein stärkerer Beacon, neue Möglichkeiten.

**5. Es bleibt Minecraft.**
Kein Zwang, keine vorgeschriebene Reihenfolge, kein Questbuch.

---

## Anti-Ziele

**Kein Vanilla zurückverkaufen.** Springen, Sprinten, Essen, Crafting und das normale Inventar bleiben unangetastet. Vanilla-Beacon, Verzauberungen und Zaubertränke werden nicht ersetzt; Mod-Inhalte dürfen mit ihnen konkurrieren.

**Ausnahme Zugang.** Gestaffelt wird der Zugang zu **Orten**, nicht zu Fähigkeiten: die Grenze, der Nether-Zugang und Darkness unterhalb von y 0, bis die Deepslate-Ebene freigeschaltet ist ([Territorium](20-territorium-und-serverweite-effekte.md)). Der Spieler verliert nichts, was er schon konnte, sondern bekommt Neues in einer Reihenfolge, die der Wirtschaft Zeit gibt.

**Kein Idle-Game.** Taler fließen nicht von allein. Marktuhr und Contracts laufen nur, während Spieler online sind. Ist der optimale Zug, das Spiel laufen zu lassen, ist etwas falsch. Auch der [Chunkloader](20-territorium-und-serverweite-effekte.md#chunkloader) hält Farmen nur am Laufen, solange jemand online ist — er überbrückt Abwesenheit von der Basis, nicht vom Spiel.

**Keine vorgeschriebene Reihenfolge.** Keine Questkette, keine exklusiven Spezialisierungen.

**Keine Verbrauchsgüter für Taler.** Taler kaufen am Beacon und an der Werkstatt nur Permanentes. Eng begrenzte Ausnahmen sind der Schwarzmarkthändler mit Contraband und wenigen Verbrauchsgütern ([Contraband](13-contraband.md)) und die Laufzeit des [Chunkloaders](20-territorium-und-serverweite-effekte.md#chunkloader) — eine reine Betriebskosten-Senke, die weder Spielkraft noch Taler bringt. Ebenso die [Reparaturbank](16-reparaturbank.md): eine Dienstleistung ohne Vorrat, die sich nicht horten lässt. Sie repariert etwas teurer als der Amboss und wird mit haltbarer Ausrüstung so teuer, dass auf Dauer Mending gewinnt. Keine Ausnahme ist der [Spielautomat](17-spielautomat.md): Er tauscht Taler nur gegen Taler, mit einer Auszahlungsquote unter 100 %. Er ist damit eine reine Senke.

**Kein zweiter Mod für fremde Aufgaben.** Kampfsysteme, Rucksäcke, Grabsteine, Reisepunkte und Bossinhalte bleiben Sache anderer Mods. Der Fokus liegt auf **Handeln, Produzieren und Erkunden**.

### Das Versagensbild

> Der Spieler findet nach zwei Stunden heraus, dass Melonen das lukrativste Item sind. Er baut eine große Melonenfarm. Der Preis fällt, aber die Farm ist so groß, dass es reicht. Nach zehn Stunden hat er alles gekauft. Er hat nie etwas anderes produziert.

Jede Designentscheidung lässt sich daran prüfen: *Macht sie dieses Szenario wahrscheinlicher oder unwahrscheinlicher?* Die Gegenmittel sind Marktsättigung, kuratierte Handelsliste, Contracts und die strengere Preiskurve für Produktions-Upgrades.

---

## Die vierzehn Blöcke

| Block | ID | Rolle |
|---|---|---|
| **Marktstand** | `dtp:market_stand` | Preisübersicht, Verkauf aus der Hand und aus Trade-Chests, Redstone-Verkauf. Ein Block, kein NPC |
| **Trade-Chest** | `dtp:trade_chest` | Warenlager des Marktstands und des Bulletin-Boards — Ware rein, auch per Hopper |
| **Bulletin-Board** | `dtp:bulletin_board` | Aushang der Contracts: annehmen, liefern, abbrechen |
| **Statistik-Tafel** | `dtp:statistics_board` | Zeigt die Verkäufe des Servers je Ware und die Contracts je Spieler und Stufe; nur Anzeige |
| **Einzahlkiste** | `dtp:deposit_chest` | Hält Taler bereit; alle Käufe werden daraus bezahlt, Erlöse landen dort |
| **Beacon** | `dtp:beacon` | Der Hauptbeacon: Territorium und World-Border, Server-Upgrades, Effekte. Einer pro Server, sein Setzen startet den Durchlauf |
| **Neben-Beacon** | `dtp:relay_beacon` | Craftbarer Zusatzbeacon für Außenposten, in jeder Dimension; eigene Effektauswahl auf kleinem Radius, Module umstecken |
| **Werkstatt** | `dtp:upgrade_table` | Persönliche Upgrades und Module |
| **Reparaturbank** | `dtp:repair_station` | Repariert Werkzeuge und Rüstung gegen Taler im Slot, ohne Erfahrung; eine Reparatur für die Zeit vor Mending |
| **Marktwaage** | `dtp:market_scale` | Gibt den Marktwert einer Ware als Comparator-Signal aus |
| **Schwarzmarkt** | `dtp:blackmarket` | Steigert Verzauberungen gegen einen Silberbeutel, mit Scam-Risiko |
| **Chunkloader** | `dtp:chunk_loader` | Hält gegen Taler 3 × 3 Chunks geladen, solange jemand online ist, in jeder Dimension |
| **Spielautomat** | `dtp:slot_machine` | Glücksspiel um eine Münze, ohne Oberfläche, für alle in der Nähe sichtbar; auf Dauer eine Talersenke |
| **Recycler** | `dtp:recycler` | Zerlegt ausgediente Ausrüstung wie ein Ofen mit Brennstoff zu Schrott, einer Ware für den Marktstand; nimmt nichts, was der Ofen schon verwertet |

Warenausgang (Trade-Chest) und Geldeingang (Einzahlkiste) sind getrennt. Das macht die Richtung jeder Leitung eindeutig und damit automatisierbar. Marktstand, Bulletin-Board, Beacon und Werkstatt haben kein eigenes Geldfach — sie greifen auf Einzahlkisten in Reichweite zu.

---

## Einstieg

Neue Spieler sollen den Mod ohne diese Doku beginnen können.

- **Handbuch** (`dtp:guide`): Jeder Spieler bekommt es beim ersten Betreten, genau einmal (Attachment `dtp:onboarded`, übersteht den Tod). Wer schon vor dem Handbuch gespielt hat, bekommt es beim nächsten Login nachgereicht. Ersatz: Buch + Kupfernugget, formlos. Abschaltbar mit `onboarding.giveGuide`.
- **Grundrezepte im Rezeptbuch**: Beacon, Marktstand, Trade-Chest, Einzahlkiste, Bulletin-Board, Werkstatt, Reparaturbank, Handbuch und alle Taler-Umrechnungen stehen ab dem ersten Betreten im Rezeptbuch. Neben-Beacon, Chunkloader, Marktwaage und Schwarzmarkt zeigt nur das Handbuch, das Rezeptbuch lässt sie zum Entdecken offen.
- **Advancements**: Ein eigener Reiter „Dynamic Trade & Progression" zeigt die Progressionslinie, vom Hauptbeacon über Marktstand, ersten Verkauf und Einzahlkiste bis zu Upgrades, mit dem Aufheben der Grenze als Abschluss. Serverweiter Fortschritt (Beacon, Server-Upgrades) zählt für alle Spieler, auch wer später dazukommt, bekommt ihn beim Login. Mit Better Advancements wird der Baum größer dargestellt, nötig ist der Mod nicht ([Kompatibilität](70-technischer-rahmen.md#kompatibilität)).
- **Beacon per Rezept**: günstig und aus Material über y = 0, aber nur, solange noch kein Beacon steht ([Territorium](20-territorium-und-serverweite-effekte.md#der-hauptbeacon)). Die Gruppe sucht sich den Startplatz gemeinsam aus; niemand muss „der erste Spieler“ sein.

Das Handbuch ist eine Einführung, keine Doku. Jeder Satz sagt, *was* es gibt, *wo* man es findet oder *was man als Nächstes tut* — nie wie viel oder wie genau. Keine Zahlen, Formeln, Kosten, Radien oder Zeiten, keine Listen von Contraband, und der Schwarzmarkthändler bleibt ein Gerücht.

Ausnahme sind die drei **Nachschlagekapitel** am Ende: Beacon-Effekte, Serverweite Upgrades, Persönliche Upgrades. Die Arbeitsteilung: Der Tooltip im Spiel sagt in einem Satz *was* ein Upgrade tut und *wie viel* (Stufenwert). Das Handbuch sagt, *worauf* es wirkt — welche Pflanzen, Tiere, Blöcke, Werkzeuge —, welche Ausnahmen gelten, was daraus folgt (Kohle schmilzt 10 statt 8 Items) und welche Farm sich damit lohnt. Dort sind Listen und Zahlen erlaubt. Einen Eintrag bekommt nur, was der Tooltip nicht schon vollständig beantwortet; Kosten stehen nie im Buch. Beschrieben wird das tatsächliche Verhalten im Code, auch wo es überrascht (der Wegebonus des Stallmeisters wirkt auch auf Kies, Landwirtschaft nicht auf die Kannenpflanze). Aufbau und Text stehen je Sprache in `assets/dtp/guide/<sprache>.md`, die Rezeptraster in `dev.dtp.guide.GuideGrids`; bearbeiten siehe [Handbuch bearbeiten](71-entwicklung-und-test.md#handbuch-bearbeiten).

---

## Aufbau der Dokumentation

| Dokument | Inhalt |
|---|---|
| [Übersicht](00-index.md) | Einstieg, Lesereihenfolge |
| [Markt](10-markt.md) | Taler, Einzahlkiste, Marktstand, Trade-Chest, Marktwaage, Handelsliste |
| [Marktdruck](11-marktdruck.md) | Preisformel, Erholung, Marktuhr, Spielerskalierung, Marktereignisse |
| [Contracts](12-contracts.md) | Lieferaufträge, Stufen, Bulletin-Board |
| [Contraband](13-contraband.md) | Werkzeuge mit Ladungen, Verbrauchsgüter |
| [Schwarzmarkt](14-blackmarket.md) | Verzauberungen steigern, Scams, Flüche |
| [Schwarzmarkthändler](15-blackmarket-trader.md) | Der flüchtige Händler und sein Angebot |
| [Reparaturbank](16-reparaturbank.md) | Reparatur gegen Taler, Kurs, Wechselgeld |
| [Recycler](19-recycler.md) | Ausrüstung zu Schrott, Wertregel |
| [Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md) | Beacon, Grenze, Zugang, Server-Upgrades |
| [Beacon-Effekte](21-beacon-effekte.md) | Effektslots, Reichweite, alle Effekte |
| [Persönliche Upgrades](30-persoenliche-upgrades.md) | Werkstatt, Kaufregeln, passive Upgrades |
| [Module und Fähigkeiten](31-module-und-faehigkeiten.md) | Modulslots, automatische Module, aktive Fähigkeiten |
| [Handelsliste bewerten](60-handelsliste-bewertung.md) | Verfahren für neue Items |
| [Preise und Größenordnungen](61-preise-und-groessenordnungen.md) | Warenklassen, Preisbänder, Ankerwaren |
| [Balancing](62-balancing.md) | Rückkopplungsregel, Preisregeln, Messprotokoll |
| [Technischer Rahmen](70-technischer-rahmen.md) | Config, Datenhaltung, Kompatibilität |
| [Entwicklung und Test](71-entwicklung-und-test.md) | Bauen, Testen, Befehle |
| [Migration](72-migration.md) | Server-Update zwischen Versionen |
| [Glossar](90-glossar.md) | Verbindliche Begriffe |
