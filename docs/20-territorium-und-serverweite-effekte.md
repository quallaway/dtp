# Territorium und serverweite Effekte

> Der Hauptbeacon, die wachsende Grenze um die Basis und alles, was die Gruppe gemeinsam am Beacon kauft. Die Effektslots und Effekte stehen in [Beacon-Effekte](21-beacon-effekte.md).

---

## Ein Bereich, drei Namen

**Beacon-Radius = Territorium = World-Border.**

Es gibt genau ein Quadrat um den Hauptbeacon. Es ist zugleich das nutzbare Gebiet, die Vanilla-World-Border und die Grundlage für den Wirkungsbereich der Effekte. Drei getrennte Bereiche wären weder vermittelbar noch pflegbar.

Es gibt **ein** Territorium für den ganzen Server, keine Besitzer, keine Teams, keine Claims. Zugriffsschutz ist nicht Aufgabe dieses Systems: Die Grenze regelt, wo gespielt werden kann, nicht wer was darf. Claim-Mods wie FTB Chunks kommen sich damit nicht in die Quere.

### Zweck

Die Grenze hat eine Aufgabe, und die liegt im Frühspiel: Sie begrenzt die verfügbare Fläche und damit die Menge an Ressourcen, die sich in den ersten Stunden sammeln lässt. Ohne sie legen vier Spieler am ersten Abend beliebig große Felder an, und die Wirtschaft ist schon gesättigt, bevor sie richtig losgeht. Mit ihr ist Fläche anfangs knapp, und mehr Fläche ist ein echtes, sichtbares Ziel.

Für Mittel- und Endspiel ist sie nicht gedacht. Sie rückt so weit weg, dass sie nicht mehr auffällt, und verschwindet am Ende ganz.

---

## Der Hauptbeacon

Der `dtp:beacon` (DTP-Beacon) ist nicht der Vanilla-Beacon. Er braucht keine Pyramide und keine Energie und wirkt dauerhaft, sobald er steht. Ein Beacon, der ausgeht, während niemand online war, würde nur Frust erzeugen. Der Vanilla-Beacon bleibt unverändert und funktioniert daneben weiter.

| Eigenschaft | Regel |
|---|---|
| Anzahl | Genau einer pro Server, nur in der Oberwelt |
| Bezug | **Craftbar, solange noch keiner steht**: 3 Glas oben, Kupferbarren – Kupferblock – Kupferbarren in der Mitte, 3 Steinziegel unten (Rezepttyp `dtp:beacon_shaped`). Ein gesetzter, noch unbestätigter Beacon sperrt das Rezept schon. Admins können ihn weiter per `/give @p dtp:beacon` vergeben |
| Abbauen | **Nicht möglich**, auch nicht verschiebbar durch Kolben. Territorium wird gekauft, nicht erwandert |
| Fortschritt | Hängt am Server (`WorldProgress`), nicht am Block |

### Das Setzen startet den Durchlauf

**Vor dem ersten Beacon gibt es keine Grenze.** Die Gruppe kann frei erkunden und sich gemeinsam einen Startort aussuchen.

Wird der Beacon platziert, fragt die Oberfläche nach einer **Bestätigung**, denn ein versehentlich gesetzter Beacon würde den Durchlauf an einem beliebigen Ort festnageln. Wer abbricht, bekommt den Block zurück. Erst mit der Bestätigung:

- wird die World-Border auf Stufe 0 gesetzt,
- wird der **Welt-Spawn auf den Beacon** gesetzt: Wer kein Bett mehr hat, und neue Spieler landen am Beacon statt womöglich außerhalb der Grenze,
- setzt die Mod auf dedizierten Servern **`spawn-protection` auf 0** (in `server.properties` gespeichert), damit alle Spieler rund um den Beacon bauen können, nicht nur Operatoren. Bei jedem Serverstart mit bestätigtem Beacon wird das erneut erzwungen. Einzelspieler und LAN haben ohnehin keinen Spawnschutz,
- gilt das Territorium. Ab jetzt lassen sich Server- und Spieler-Upgrades kaufen.

Ein zweiter Beacon wird abgelehnt, solange der erste noch steht. Ältere Welten, deren Spawn außerhalb der Grenze liegt, werden beim Serverstart einmal korrigiert. Ein Spawn, den ein Admin innerhalb der Grenze gesetzt hat, bleibt stehen.

### Befehle

| Befehl | Wirkung |
|---|---|
| `/dtp beacon y <y>` | Verschiebt den Beacon auf der Y-Achse, etwa um ihn in ein Gebäude einzubauen. Die Grenze bleibt, weil nur X und Z zählen |
| `/dtp admin beacon move <pos>` | Notausgang: Beacon an eine neue Position setzen, die Grenze folgt |
| `/dtp admin beacon reset` | Notausgang: Beacon entfernen und Grenze aufheben. Die Server-Upgrades bleiben erhalten |

Alle drei brauchen Operator-Rechte (Stufe 2).

### Neben-Beacons

Für Außenposten gibt es den **craftbaren** Neben-Beacon (`dtp:relay_beacon`):

```
 . G .     G = Goldtaler
 . D .     D = Diamantblock
 I I I     I = Eisenblock
```

Er trägt eine eigene Auswahl aus den am Hauptbeacon freigeschalteten Effekten, wirkt in jeder Dimension und unabhängig vom Territorium und hat keine eigene Grenze. Über ihn lässt sich nichts kaufen. In seinem Radius lassen sich persönliche Module umstecken ([Module und Fähigkeiten](31-module-und-faehigkeiten.md)). Balanciert wird er über das Crafting-Material, nicht über Taler, so bleibt er ein Bauprojekt und kein Kaufvorgang. Radius und Slots stehen in [Beacon-Effekte](21-beacon-effekte.md#neben-beacon).

### Chunkloader

Der Chunkloader (`dtp:chunk_loader`) ist ein Glaskasten wie der Neben-Beacon, nur dass statt eines Kristalls eine Enderperle darin schwebt. Er hält die **Chunks um sich geladen**, damit Farmen weiterlaufen, während die Gruppe woanders ist, zum Beispiel im Nether, beim Erkunden oder am Außenposten. Er funktioniert in jeder Dimension und unabhängig vom Territorium.

- **Laden:** Rechtsklick öffnet die Oberfläche. Eine Ladung wird aus den Einzahlkisten in Reichweite bezahlt; fehlt Geld, wird nichts entnommen.
- **Nachladen:** Jede Ladung verlängert die Restlaufzeit. Wie viel sich höchstens im Voraus laden lässt, ist begrenzt. Eine automatische Verlängerung gibt es nicht.
- **Anzeige:** Mit Restlaufzeit schwebt die Perle, wippt und dreht sich, dazu steigen Portalpartikel auf. Ohne Restlaufzeit liegt sie still auf dem Podest.

| Wert | Standard | Config |
|---|---|---|
| Preis je Ladung | 2.500 Taler | `chunkloader.price` |
| Laufzeit je Ladung | 3 Minecraft-Tage (72.000 Ticks, 60 min) | `chunkloader.durationTicks` |
| Höchstens im Voraus | 3 Ladungen (180 min) | `chunkloader.maxCharges` |
| Geladene Fläche | 3 × 3 Chunks um den eigenen Chunk | `chunkloader.radiusChunks` |
| Reichweite für Einzahlkisten | 16 Blöcke | `chunkloader.depositRadius` |
| Natürliches Spawnen ohne Spieler in der Nähe | an | `chunkloader.naturalSpawning` |

- **Nur, solange jemand online ist.** Ist kein Spieler auf dem Server, ruhen die Chunks, und die Laufzeit steht still. Sie läuft weiter, sobald sich jemand einloggt. Der Chunkloader ist ein Werkzeug für Abwesenheit *von der Basis*, nicht vom Spiel.
- **Spielticks, nicht Tageszeit:** Die Laufzeit zählt in Ticks. Schlafen verkürzt sie nicht, ein gestoppter oder pausierter Server verbraucht nichts.
- **Abbauen** löscht die Restlaufzeit, ohne Rückerstattung. Der Block lässt sich nicht verschieben.
- Mehrere Chunkloader dürfen sich überlappen. Jeder zählt für sich.
- Mit `chunkloader.enabled = false` nimmt der Block keine Taler mehr an und lädt nichts; gespeicherte Laufzeit bleibt erhalten.

**Einordnung:** Der Chunkloader ist neben dem Schwarzmarkt die zweite eng begrenzte Ausnahme von „Taler kaufen nur Permanentes“ ([Vision](01-mod-und-vision.md#anti-ziele)). Er ist eine reine **Betriebskosten-Senke**: Er gibt keine Spielkraft, erzeugt keine Taler und wirkt nur, solange gespielt wird. Was eine Farm in dieser Zeit produziert, trifft am Markt auf die normale Sättigung. Er macht also keine Farm größer, sondern lässt nur die vorhandenen weiterlaufen, während die Gruppe etwas anderes tut.

**Rezept:** Es steht nur im Handbuch, nicht im Rezeptbuch.

```
 G G G     G = Glas
 T E T     T = Silberbeutel
 S S S     E = Enderperle
           S = Seelensand
```

Seelensand gibt es fast nur im Nether. Der Chunkloader setzt damit den Nether-Zugang voraus und kommt erst, wenn die Gruppe mehr als eine Farm-Gegend hat.

---

## Grenzstufen

Die Grenze wächst **gleichmäßig in alle Richtungen**, gemessen in Chunks um den Hauptbeacon (`beacon.radiusChunks`, halbe Kantenlänge des Quadrats). Ein Radius ist eine einzige Zahl, braucht keine eigene Oberfläche, nutzt die vorhandene World-Border und passt zur Preiskurve. Eine Vorschau gibt es nicht: Wer an der Grenze entlanggeht, sieht selbst, was dahinter liegt.

Gekauft wird am Beacon im Reiter **Welt** (`border`, Name „Grenzradius", 11 Stufen in einem Feld).

| Stufe | Radius (Chunks) | Radius (Blöcke) | Preis der Stufe |
|---|---|---|---|
| 0 | 12 | 192 | Start, beim Setzen |
| 1 | 16 | 256 | 2.000 |
| 2 | 20 | 320 | 4.000 |
| 3 | 25 | 400 | 7.000 |
| 4 | 31 | 496 | 12.000 |
| 5 | 39 | 624 | 20.000 |
| 6 | 49 | 784 | 30.000 |
| 7 | 62 | 992 | 45.000 |
| 8 | 80 | 1.280 | 65.000 |
| 9 | 106 | 1.696 | 90.000 |
| 10 | 140 | 2.240 | 125.000 |
| 11 | – | **Grenze entfernt** | 500.000 |

Zusammen kosten alle Stufen 900.000 Taler. Der Start (385 × 385 Blöcke) ist eng, aber spielbar, und schon die ersten Stufen bringen spürbar Fläche dazu.

**Stufe 11 ist der Abschluss.** Sie kostet ein Vielfaches der vorherigen und hebt die Grenze vollständig auf. Danach ist die Welt offen, und die Grenze ist nie wieder ein Thema. Einen Endgame-Zyklus gibt es bewusst nicht: Ein Durchlauf darf enden, und das Aufheben der Grenze ist ein Ende, das sich wie eines anfühlt.

Die World-Border wird **nur bei Ereignissen** gesetzt, beim Start und nach jedem Grenzkauf, nicht laufend an den Block gekoppelt. Ein Admin, der die Border selbst ändert, überschreibt sie also, und der nächste Kauf korrigiert sie wieder. Mit `beacon.borderEnabled = false` setzt die Mod keine Border.

### Außerhalb der Grenze

Gebiet jenseits der Grenze ist **nicht betretbar**, das erledigt die Vanilla-World-Border mit ihrer eigenen Darstellung (rote Wand, Warnung bei Annäherung).

Funktionsblöcke außerhalb des Territoriums sind eingeschränkt. Das betrifft alles vor dem Setzen des Beacons, alles in anderen Dimensionen und in der Oberwelt alles jenseits der Grenze (nach Stufe 11 gehört die ganze Oberwelt zum Territorium):

- Ein Marktstand handelt zu maximalem Marktdruck (rund 5 % des Basiswerts), mit Warnhinweis ([Markt](10-markt.md)).
- Ein Bulletin-Board ist nur lesbar ([Contracts](12-contracts.md)).
- An einer Werkstatt lässt sich nichts kaufen ([Persönliche Upgrades](30-persoenliche-upgrades.md)).

So lässt sich die Frühspiel-Grenze nicht umgehen, indem die Gruppe erst ausgiebig sammelt und verkauft und den Beacon erst danach setzt.

### Andere Dimensionen

Im Nether, im End und in Mod-Dimensionen gibt es **keine Grenze**. Nether und End stehen schon hinter ihrer eigenen Staffelung (siehe unten), eine zweite Einschränkung wäre Gängelung ohne Gewinn. Sie zählen aber auch nicht zum Territorium: Der Hauptbeacon wirkt nur in der Oberwelt, Effekte gibt es dort nur über einen [Neben-Beacon](21-beacon-effekte.md#neben-beacon).

---

## Zugangsstaffelung

Weltbereiche öffnen sich nicht alle von Anfang an. Gestaffelt wird **Zugang zu Orten**, nie eine Fähigkeit. Der Zweck ist wirtschaftlich: Seltene Materialien sind im Markt wertvoll, und stünden sie ab der ersten Stunde zur Verfügung, wäre die Frühphase der Wirtschaft übersprungen.

| Zugang | Sperre | Freischaltung (Reiter Welt) | Abschalten |
|---|---|---|---|
| **Deepslate-Ebene** (y < 0, Oberwelt) | Weich: Darkness-Effekt | `deepslate`, „Deepslate-Freischaltung", 3.000 | `access.deepslateDarkness` |
| **Nether** | Hart: Portale zünden nicht | `nether_access`, „Nether-Zugang", 60.000 | `access.netherLock` |
| **End** | Keine eigene Sperre | – | – |

**Deepslate über Darkness.** Wer ohne Freischaltung unter y = 0 geht, bekommt Darkness. Nichts ist verboten, nur unangenehm: Man sieht, dass es dort etwas gibt, und merkt sofort, dass etwas fehlt. Die Freischaltung ist bewusst günstig, ein frühes Erfolgserlebnis und keine Hürde. Kreativ- und Zuschauermodus sind ausgenommen.

**Nether: harte Sperre.** Ein Portal ist ein eindeutiger, bewusster Übergang, da ist eine harte Sperre vertretbar. Bis zur Freischaltung zündet kein Netherportal, Spieler in der Nähe bekommen einen Hinweis. Als Sicherheitsnetz scheitert auch jede andere Reise in den Nether, etwa durch ein per Befehl gesetztes Portal.

**End: keine eigene Sperre.** Das End ist schon doppelt gestaffelt. Enderaugen brauchen Lohenpulver, und Lohen gibt es nur im Nether. Und die Festungen liegen 1.280 bis 2.816 Blöcke vom Weltmittelpunkt entfernt: Die Grenze erreicht 1.280 Blöcke erst mit Stufe 8, weiter entfernte Festungen kommen erst nach Stufe 10 oder 11 in Reichweite, je nachdem, wo der Beacon steht.

Ozeanbiome, Ancient Cities, Woodland Mansions und Strongholds werden nicht gestaffelt, sie liegen ohnehin hinter Entfernung, Gefahr und der Grenze. Mod-Dimensionen berücksichtigt der Mod nicht.

---

## Serverweite Upgrades

Gekauft am Hauptbeacon, gelten für alle Spieler. Die Beacon-Oberfläche zeigt sie als Baum in den Reitern **Welt** und **Markt**, jede Stufe als eigenes Feld mit Preis und Beschreibung (nur der Grenzradius bleibt ein Feld). Jede Stufe setzt die vorige voraus. Der Reiter **Module** enthält Effektslots und Effekte ([Beacon-Effekte](21-beacon-effekte.md)).

Was ohnehin allen zugutekommt, gehört allen. Spielergebundene Wirtschaftsboni würden in einer kooperativen Gruppe nicht funktionieren: Kauft einer „bessere Erzpreise", verkaufen die anderen ihre Erze einfach über ihn.

Die Upgrades unten belegen keinen Effektslot und kommen ohne Mixin aus. Endermann- und Creeper-Schutz sowie Meisterschmied wirken weltweit, in allen Dimensionen. Ofentempo und Mending-Aura wirken nur im **Beacon-Gebiet**: im Effektgebiet des Hauptbeacons und im Radius jedes Neben-Beacons, also auf denselben Flächen wie die [Beacon-Effekte](21-beacon-effekte.md#reichweite). Jeder Neben-Beacon trägt sie ohne Auswahl und ohne Slot, sobald sie gekauft sind. Mehrfach abgedeckt wirken sie einmal.

### Reiter Welt

Neben Grenzradius, Deepslate und Nether (siehe oben):

| ID | Name | Stufen | Preis | Wirkung |
|---|---|---|---|---|
| `enderman_griefing` | Endermänner lassen Blöcke liegen | 1 | 8.000 | Endermänner heben keine Blöcke mehr auf und setzen keine ab, in allen Dimensionen |
| `creeper_griefing` | Creeper-Schutz | 1 | 20.000 | Creeper-Explosionen zerstören keine Blöcke. Schaden an Spielern, Mobs und Entities wie Item-Rahmen bleibt. TNT, Ghast-Feuerbälle, Wither, Betten und Seelenanker sind nicht betroffen |
| `furnace_speed` | Ofentempo | 2 | 20.000 / 60.000 | Ofen, Schmelzofen und Räucherofen im Beacon-Gebiet arbeiten ×1,25 / ×1,5 schneller, das Lagerfeuer nicht. Die Brenndauer bleibt, ein Brennstoff schafft also mehr Items (Kohle 10 / 12 statt 8). Wirkt ab dem nächsten angezündeten Brennstoff |
| `anvil_cap` | Meisterschmied | 1 | 50.000 | Amboss-Arbeiten kosten höchstens 39 Level, „Zu teuer!" entfällt |
| `mending_aura` | Mending-Aura | 2 | 75.000 / 250.000 | Im Beacon-Gebiet wird jedes beschädigte Item im ganzen Inventar alle 10 Sekunden um 1 / 2 Haltbarkeit repariert, ohne Erfahrung. Grundpflege, kein Ersatz für Mending |

Technik in Kürze:

- Endermann und Creeper hängen an `EntityMobGriefingEvent`, gefiltert auf genau diese beiden Mobs. Abgelehnte Creeper explodieren mit `BlockInteraction.KEEP`. Die Gamerule `mobGriefing` bleibt unberührt.
- Das Ofentempo nutzt Vanillas datengetriebenen Garwert: Die Mod überschreibt `minecraft:cooking/speed_default` und multipliziert ihn mit dem Zahlenwert `dtp:server_upgrade` (Faktoren `1.0, 1.25, 1.5` in derselben Datei, per Datapack änderbar). Vanillas ×2 für Hochofen und Räucherofen bleibt erhalten. Mit `"local": true` prüft der Zahlenwert die Position des Ofens (Kontextparameter `origin`) gegen `BeaconLogic.inBeaconArea`; außerhalb gilt der erste Faktor.
- Die Mending-Aura läuft alle 200 Ticks über alle Spieler, nie über Blöcke, und prüft ihre Position mit `BeaconLogic.inBeaconArea`. Contraband-Items repariert sie nicht ([Contraband](13-contraband.md)). Config: `beacon.mendingAuraRepair` (1), `beacon.mendingAuraRepair2` (2), jeweils Haltbarkeit je Takt. Eine volle Reparatur dauert mit Stufe I 4,3 h (Diamant, 1.561) bzw. 5,6 h (Netherit, 2.031), mit Stufe II die Hälfte.
- Meisterschmied rechnet das Amboss-Ergebnis selbst nach (`AnvilCap`), weil Vanilla es ab 40 Level verwirft, bevor NeoForge eingreifen kann. Der Client rechnet mit und kennt die Stufe über `ServerUpgradeSync` (beim Einloggen und nach jedem Kauf). Der GameTest `anvil_cap` prüft den Nachbau gegen Vanilla.

### Reiter Markt

| ID | Name | Stufen | Preis | Wirkung |
|---|---|---|---|---|
| `contract_tier` | Contract-Stufe | 4 | 1.000 / 12.000 / 40.000 / 100.000 | Schaltet Contract-Stufe 1 bis 4 frei. Ohne Kauf gibt es keine Contracts (`contracts.baseMaxTier` 0) |
| `contract_slots` | Aushang-Platz | 4 | 8.000 / 25.000 / 60.000 / 120.000 | +1 Platz am Aushang je Stufe, also 4 → 8 (`contracts.boardSize` 4) |
| `price_stability` | Langsamerer Preisverfall | 2 | 100.000 / 500.000 | H wächst um 10 % je Stufe (`market.stabilityPerLevel`) |
| `price_recovery` | Schnellere Preiserholung | 2 | 200.000 / 1.000.000 | T sinkt um 8 % je Stufe (`market.recoveryPerLevel`) |
| `contract_quality` | Contract-Qualität | 2 | 200.000 / 500.000 | +5 Prozentpunkte Würfelchance für Contract-Stufe 2 bis 4 je Stufe (`contracts.tierChancePerLevel`), wirkt ab der nächsten Rotation |
| `market_info` | Marktinformation | 1 | 5.000 | Der Marktstand zeigt Prozentwert und Trendpfeil der Preise, auch für die Ware in der Hand |

Die **Contract-Stufe** braucht zusätzlich serverweit abgeschlossene Contracts: 0 / 5 / 25 / 60 für Stufe 1 bis 4 (`contracts.tierRequirement`). Fehlen welche, nennt die Oberfläche die Zahl. Nach dem Kauf wird der Aushang sofort neu gewürfelt, damit die neue Stufe nicht erst mit der nächsten Rotation erscheint. Details in [Contracts](12-contracts.md).

Die **Contract-Qualität** verschiebt die frei gewürfelten Plätze des Aushangs zu höheren Stufen; die garantierten Plätze je Stufe bleiben. Anders als die Contract-Stufe würfelt sie den Aushang nach dem Kauf nicht neu, sie greift ab der nächsten Rotation.

Preisstabilität und Preiserholung wirken auf die **Dynamik** des Marktes, nicht auf einen Spielerbonus, siehe [Marktdruck](11-marktdruck.md). Verkaufspreis-Boni gibt es bewusst nicht: Lieber den Ertrag der Items erhöhen als ihren Preis, denn ein höherer Preis umgeht den Markt.

---

## Bezahlt wird aus der Einzahlkiste

Der Beacon hat **kein eigenes Geldfach**. Beim Kauf sucht er Einzahlkisten im Umkreis von **16 Blöcken** (`beacon.depositRadius`, bis 32) in alle Richtungen, auch nach oben und unten, und entnimmt den Betrag daraus. Die Kiste kann also neben dem Beacon oder in einem Raum darunter stehen. Andere Blöcke suchen nur im Umkreis von 2 (`deposit.radius`).

Praktisch stellt die Gruppe eine Einzahlkiste in die Nähe des Beacons, jeder wirft Taler hinein, und das nächste gemeinsame Ziel wird bezahlt, sobald genug drin ist. Da Taler Items sind ([Markt](10-markt.md)), ist gemeinsame Finanzierung wörtlich gemeint.

Regeln:

- **Jeder darf kaufen**, egal wer eingezahlt hat. Kein Zustimmungsverfahren, keine Anteile. Alle Upgrades sind permanent und nützlich, ein Fehlkauf verzögert, verbaut aber nichts. Wer allein gespart hat, soll sein Ziel auch allein erreichen können.
- **Keine Rückerstattung.** Gekauft ist gekauft.
- **Taler können gestohlen werden.** Jede Zahlung kommt aus derselben Kiste, also kann ein Spieler mit fremden Talern auch ein persönliches Upgrade an einer Werkstatt kaufen. Das ist die Konsequenz der Item-Währung, kein Fehler. Wer es vermeiden will, stellt seine Kiste außer Reichweite: Das Layout regelt den Zugriff.
- Gekauft werden kann erst nach dem Start des Durchlaufs. Jeder Kauf wird im Chat gemeldet und im Messprotokoll vermerkt.
