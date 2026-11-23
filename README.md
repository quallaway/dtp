# Dynamic Trade & Progression

> **Minecraft soll dafür belohnen, eine vielfältige und wachsende Produktionskette aufzubauen, statt die effizienteste einzelne Farm endlos zu skalieren.**

[![Build](https://github.com/quallaway/dtp/actions/workflows/build.yml/badge.svg)](https://github.com/quallaway/dtp/actions/workflows/build.yml)

Ein Mod für **NeoForge** und Minecraft **26.3**, kurz **DTP**. Version 5.0.0.

**→ [Download](https://github.com/quallaway/dtp/releases/latest)**

---

## Worum es geht

Eine Farm läuft, die Truhen füllen sich — und dann? DTP gibt Ressourcen einen Zweck: einen serverweiten Markt, der viel aufnimmt, aber nicht alles und nicht unbegrenzt. Was oft verkauft wird, verliert an Wert; was lange niemand geliefert hat, gewinnt. Mit den Einnahmen kauft man Fortschritt, für sich selbst und gemeinsam für den ganzen Server.

## Die Kernsysteme

| System | Kurz |
|---|---|
| **Taler** | Die Währung ist ein echtes Item in drei Metallen (Kupfer, Silber, Gold), als Münze, Beutel und Sack. Sie braucht Platz, lässt sich teilen und geht beim Tod verloren. Bezahlt wird aus **Einzahlkisten** in der Nähe, nie aus dem Inventar. |
| **Markt** | Der **Marktstand** kauft Waren aus einer kuratierten Handelsliste, aus der Hand, aus **Trade-Chests** oder per Redstone. Jede Ware hat einen dynamischen Preis: Verkäufe drücken ihn, mit der Zeit erholt er sich. Marktereignisse verschieben die Nachfrage. |
| **Contracts** | Am **Bulletin-Board** gibt es Lieferaufträge über größere Mengen in mehreren Stufen. Sie laufen nicht ab, verlieren aber mit der Zeit an Wert. Sie sind der wichtigste Anreiz, Vielfalt zu produzieren. |
| **Beacon und Grenze** | Der **DTP-Beacon** ist das Herz des Servers. Er trägt die Weltgrenze, die in Stufen wächst, schaltet Deepslate und Nether frei und verkauft gemeinsam finanzierte Server-Upgrades und Beacon-Effekte. |
| **Persönliche Upgrades** | An der **Werkstatt** kauft jeder Spieler eigene Verbesserungen und Module mit aktiven Fähigkeiten. |
| **Talersenken mit Risiko** | Schwarzmarkt und Schwarzmarkthändler (Verzauberungen und besondere Werkzeuge, mit Scam-Risiko), Spielautomat. |
| **Werkzeuge rund um den Markt** | Reparaturbank, Recycler (Ausrüstung zu verkaufbarem Schrott), Chunkloader gegen Taler, Statistik-Tafel, Marktwaage mit Comparator-Signal. |

Ein Handbuch im Spiel erklärt alle Blöcke mit ihren Rezepten. Alle Blöcke und Items finden sich im Kreativ-Tab *Dynamic Trade & Progression*.

### Was DTP bewusst nicht tut

- **Keine Fähigkeiten zurückverkaufen.** Gestaffelt wird nur der Zugang zu Orten (Deepslate, Nether), nie etwas, das Vanilla schon kann.
- **Kein Idle-Game.** Taler entstehen nur durch Verkaufen und Liefern.
- **Kein Ersatz für gute Mods.** Rucksäcke, Grabsteine, Reisepunkte und Kampfsysteme bleiben deren Sache.

---

## Technische Anforderungen

| | |
|---|---|
| Minecraft | **26.3** (genau diese Version) |
| Modloader | **NeoForge 26.3.0.99** oder neuer |
| Java | **25** (bringt der Minecraft-Launcher mit) |
| Seite | **Server und Client.** DTP bringt eigene Blöcke und Oberflächen mit; jeder Spieler braucht dieselbe Version wie der Server |
| Abhängigkeiten | **Keine.** DTP läuft ohne jeden anderen Mod |

### Installation

1. `dtp-26.3-<version>.jar` vom [neuesten Release](https://github.com/quallaway/dtp/releases/latest) herunterladen.
2. In den `mods`-Ordner des Servers und jedes Clients legen.
3. Server starten. Die Einstellungen stehen danach in `config/dtp-synced.toml` und werden an die Clients übertragen.

**Aktualisieren:** Vorher Welt und `config/` sichern. Spielstände werden beim Laden automatisch auf das neue Format gebracht. Eine **ältere** Jar lädt einen Spielstand, den eine neuere gespeichert hat, nicht — sie lehnt ihn ab, statt ihn zu beschädigen. Steht in der Config noch ein alter Standardwert, bleibt er erhalten; wer die neuen Standards will, löscht `dtp-synced.toml` und lässt sie neu anlegen.

---

## Kompatibilität

DTP arbeitet ohne Mixin, nur über NeoForge-Events, Attribute und Datapacks. Das hält Konflikte mit anderen Mods klein.

### Unterstützt

| Mod | Zusammenspiel |
|---|---|
| **Sophisticated Backpacks** (mit Core) | Optional. Zwei Rucksack-Upgrades machen einen **platzierten** Rucksack zur Trade-Chest oder Einzahlkiste. Ein getragener Rucksack zählt nie. Ohne den Mod gibt es die Upgrades gar nicht. Geprüft ab Backpacks 3.26 / Core 1.5.22 |
| **JEI** | Optional. Auf den DTP-Oberflächen weicht JEI zur Seite bzw. zeigt seine Itemliste in einem eigenen Bereich |
| **Better Advancements** | Optional, ohne Anbindung. Zeigt den DTP-Fortschrittsbaum größer und navigierbar |
| **Andere Mods mit Lagerblöcken** | Jeder Block, der die Capabilities `dtp:trade_storage` bzw. `dtp:deposit_storage` anbietet, zählt als Trade-Chest bzw. Einzahlkiste |
| **Items aus anderen Mods** | Verkaufbar, sobald ein Modpack-Datapack ihnen einen Preis in der Handelsliste gibt. DTP bringt selbst nur Preise für Vanilla-Items mit |

### Bekannte Berührungspunkte und mögliche Konflikte

DTP greift an einigen Stellen in Vanilla-Verhalten ein. Mods, die dieselben Stellen verändern, können sich mit DTP überschneiden. Fast alles davon lässt sich in `dtp-synced.toml` abschalten.

| Bereich | Was DTP tut | Mögliche Konflikte | Abschalten |
|---|---|---|---|
| **Weltgrenze** | Nutzt die Vanilla-World-Border und setzt sie beim Start und bei jedem Grenzkauf | Mods oder Admins, die die Border selbst setzen; DTP überschreibt sie beim nächsten Grenzkauf | `beacon.borderEnabled` |
| **Nether-Sperre** | Netherportale zünden erst nach der Freischaltung; auch andere Reisen in den Nether scheitern | Mods mit eigenen Wegen in den Nether. **Mod-Dimensionen** werden nicht gesperrt | `access.netherLock` |
| **Deepslate** | Darkness-Effekt unter y = 0 bis zur Freischaltung | Mods, die Darkness entfernen oder abwandeln | `access.deepslateDarkness` |
| **Beacon-Effekte** | Attribut-Modifier und Events (Wachstum, Zucht, Bienen, Reittiere) | Mods, die dieselben Attribute oder Vanilla-Mechaniken stark verändern | `beacon.effectsEnabled` |
| **Amboss** | Server-Upgrade *Meisterschmied* deckelt die Kosten bei 39 und rechnet das Vanilla-Ergebnis dafür nach | Mods, die den Amboss umbauen — dann kann das Ergebnis vom Nachbau abweichen | Upgrade nicht kaufen |
| **Dorfbewohner** | Händlerware wird markiert, damit sie sich nicht über den Markt waschen lässt | Mods, die den Handel mit Dorfbewohnern ersetzen oder neue Wege der Warenausgabe schaffen | – |
| **Vanilla-Daten** | Überschreibt `minecraft:cooking/speed_default` (Ofentempo als Server-Upgrade) und die Kompostier-Chancen; ergänzt Loot per Global Loot Modifier (Angelschätze, Strukturtruhen, Fortune auf Weizen und Rote Bete, Geschenke an den Helden des Dorfes) | Datapacks oder Mods, die **dieselben Dateien** überschreiben — dann gewinnt eine der beiden Fassungen. Loot-Modifier vertragen sich mit geänderten Loot-Tables | per Datapack |
| **Schwarzmarkthändler, Marktereignisse, Contraband, Reparatur, Chunkloader** | Eigene Systeme mit Spawns, Ladungen und geladenen Chunks | Server mit strengen Regeln für Spawns oder Chunkloading | je ein eigener Schalter (`blackmarket.traderEnabled`, `events.enabled`, `contraband.enabled`, `repair.enabled`, `chunkloader.enabled`) |
| **Rezept-Sperren** (KubeJS, Quest-Mods) | Einige Rezepte hängen am Fortschritt | Doppelte Sperren mit Modpack-Gating | per Config |

Nicht abschaltbar sind Marktstand, Taler, Preise und Contracts — sie sind der Mod.

### Nicht vorgesehen

- **Andere Minecraft-Versionen.** DTP wird bewusst nicht jeder Version hinterherportiert.
- **Nur auf dem Server oder nur auf dem Client.** Beide Seiten brauchen die Jar.
- **Fabric/Quilt.** Nur NeoForge.

---

## Aus dem Quellcode bauen

```bash
./gradlew build                  # Ergebnis: build/libs/dtp-26.3-<version>.jar
scripts/gametests.sh             # Asset-Prüfung und GameTests
```

Voraussetzung ist ein JDK 25. Gradle lädt Minecraft und NeoForge beim ersten Build selbst herunter. Jeder Push wird von GitHub Actions gebaut und getestet, ohne und mit den optionalen Mods.

---

## Lizenz

[MIT](LICENSE) — © 2026 Quallaway.

*Die Texte im Spiel gibt es auf Deutsch und Englisch.*
