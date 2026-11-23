# Entwicklung und Test

> Wie der Mod gebaut, getestet und in eine Prism-Instanz eingebunden wird, welche Befehle es gibt und wo welche Daten liegen.

---

## Voraussetzungen

| | |
|---|---|
| JDK | **25** — Minecraft 26.3 läuft auf Java 25 |
| Gradle | Kommt über `./gradlew` aus dem Projekt, keine Installation nötig |
| NeoForge | **26.3.0.99**, ModDevGradle 2.0.148 — lädt Gradle beim ersten Build nach `~/.gradle` |
| Launcher | Prism Launcher (Flatpak) |

Beim ersten Build lädt Gradle Minecraft und NeoForge als Entwicklungsdateien herunter (einmalig, rund 1–2 GB in `~/.gradle`). In die Prism-Instanzen wird dabei nichts installiert.

### Besonderheit: VSCodium als Flatpak

VSCodium sieht ohne Weiteres kein JDK. Abhilfe ist die Flatpak-Erweiterung:

```bash
flatpak install --user flathub org.freedesktop.Sdk.Extension.openjdk25//25.08
flatpak override --user --env=FLATPAK_ENABLE_SDK_EXT=openjdk25 com.vscodium.codium-insiders
```

Danach VSCodium neu starten. Flatpak blendet außerdem die Daten anderer Flatpak-Apps aus — das Deploy-Skript greift deshalb über `flatpak-spawn --host` auf Prism zu.

---

## Bauen

```bash
./gradlew build          # Ergebnis: build/libs/dtp-26.3-<version>.jar
```

Die Version steht als `mod_version` in `gradle.properties` (derzeit `5.0.0`). Der Dateiname enthält die Minecraft-Version, damit beim Download klar ist, wofür die Jar gedacht ist.

---

## Testen

Drei Wege, vom schnellsten zum realistischsten:

### 1. Automatische Tests (ohne Spiel)

```bash
./gradlew runGameTestServer
```

Startet einen Testserver, führt die GameTests aus `dev.dtp.gametest.DtpGameTests` aus und beendet sich. Geprüft werden unter anderem Taler und Bezahlen, Verkauf und Marktdruck, Uhren und Marktereignisse, Contracts, Beacon und Grenze, Beacon-Effekte, Spieler-Upgrades und Module, Contraband und Schwarzmarkt sowie das Laden von Rezepten, Loot-Tables und Datapacks. Wo der Mod Vanilla-Verhalten ohne Mixin nachbaut, vergleicht ein Test beide. Am Ende steht `All N required tests passed`.

Bequemer geht es mit `scripts/gametests.sh`. Das Skript ruft denselben Gradle-Task auf, legt das vollständige Log in `build/gametests.log` ab und zeigt am Ende eine kurze Zusammenfassung mit den fehlgeschlagenen Tests. Weitere Argumente gehen an Gradle weiter.

**JSON-Prüflauf.** Die JSON-Dateien werden von Hand gepflegt, nicht per Datagen erzeugt. Damit Fehler trotzdem vor dem Spielstart auffallen, prüfen zwei Stufen sie automatisch, ohne sie zu verändern:

- Der GameTest `data_files_valid` (`DataFileCheck`) liest jede Datei unter `data/` mit dem Codec, den Minecraft, NeoForge oder der Mod beim Laden benutzt, und schreibt sie im Speicher wieder heraus. Er meldet Dateien, die sich nicht lesen lassen, **unbekannte Schlüssel**, die Minecraft still ignoriert (etwa `functions` statt `modifier` seit 26.3, oder Tippfehler wie `"bse"`), und unbekannte IDs in Tags, Handelsliste, Kategorien, Contracts, Contraband, Recycling und Upgrade-Symbolen. Ein Ordner ohne bekannten Codec lässt den Test ebenfalls scheitern; neue Dateitypen trägt man in `DataFileCheck` ein. Explizit gesetzte Standardwerte (`"weight": 1`, `"x": 0`) sind kein Fehler. Sie stehen als `Data file check (hint)` im Log.
- `scripts/check_assets.py` prüft `assets/`, die der Testserver nicht lädt: Modell- und Texturverweise, Upgrade-Sprites und gleiche Schlüssel in beiden Sprachdateien. `scripts/gametests.sh` ruft das Skript vorher auf.

**Mit optionalen Mods.** `-Pwith_compat=true` lädt Sophisticated Backpacks (mit Core) und Better Advancements in Testserver und Entwicklungs-Client, etwa `./gradlew runGameTestServer -Pwith_compat=true`. Ohne den Schalter fehlen sie. Beide Läufe müssen grün sein: Der Test `compat_optional` prüft ohne die Mods, dass es die Rucksack-Upgrades und ihre Rezepte nicht gibt, und mit ihnen, dass ein platzierter Rucksack mit Upgrade als Trade-Chest bzw. Einzahlkiste zählt. Die Versionen stehen in `gradle.properties`.

Vor jedem Build und Spielstart läuft außerdem `./gradlew validateFishingFinds`. Das ist eine Schnellprüfung der Angelschatz-Fundliste ohne Spielstart, siehe [Angelschatz-Fundliste erweitern](#angelschatz-fundliste-erweitern).

### Automatisch: vor dem Push und auf GitHub

Der Hook `.githooks/pre-push` läuft vor jedem `git push` und führt `scripts/gametests.sh` aus (Asset-Prüfung und GameTests ohne optionale Mods). Schlägt etwas fehl, bricht der Push ab. Getestet wird der Arbeitsstand, nicht genau der gepushte Commit; reine Tag-Pushes überspringen die Tests. Der Hook lässt sich mit `--no-verify` umgehen und erspart also nur rote Läufe auf GitHub.

Verbindlich ist GitHub Actions (`.github/workflows/build.yml`): Bei jedem Push auf `main` und jedem Pull Request laufen Build und GameTests **ohne und mit** `-Pwith_compat=true` und `check_assets.py`. Die gebaute Jar liegt 14 Tage als Artefakt `dtp-jar` am Lauf. Ein Versions-Tag `vX.Y.Z` startet `.github/workflows/release.yml` und legt einen Release-Entwurf an.

Hook einschalten (einmalig): `git config core.hooksPath .githooks`. Neue Version: `mod_version` in `gradle.properties` erhöhen, committen, pushen, nach grünem Lauf `git tag vX.Y.Z && git push origin vX.Y.Z`; den Release-Entwurf auf GitHub prüfen und veröffentlichen.

### 2. Entwicklungs-Client

```bash
./gradlew runClient
```

Startet Minecraft direkt aus dem Projekt, offline als Spieler `Dev`. Spielstände liegen unter `run/`. Der schnellste Weg, Oberflächen auszuprobieren — kein Kopieren, kein Launcher. Alle Blöcke und Items finden sich im Kreativ-Tab *Dynamic Trade & Progression*.

### 3. Prism-Instanz

Einmalig eine Instanz anlegen:

1. Prism → *Instanz hinzufügen* → Minecraft **26.3**, Modloader **NeoForge** (26.3.0.99 oder neuer).
2. Einen Namen vergeben, z. B. `DTP 26.3`.
3. Die Instanz **einmal starten** und wieder schließen, damit Prism den Spielordner anlegt.

Danach bei jeder Änderung:

```bash
scripts/deploy-prism.sh                  # Ziel ist immer die Instanz "DTP 26.3"
scripts/deploy-prism.sh "Andere Instanz" # nur bei Bedarf
```

Das Skript baut den Mod, entfernt alte `dtp-*.jar` aus dem `mods`-Ordner der Instanz und kopiert die neue hinein. Die Instanz wird über ihren exakten Namen gefunden (aus `instance.cfg`), nie über die Minecraft-Version — andere 26.3-Instanzen bleiben unberührt. Die bestehende `dtp-synced.toml` sichert es als `.last`, damit die aktuellen Standards greifen; mit `--keep-config` bleibt sie erhalten ([scripts/README.md](../scripts/README.md)). Dann die Instanz in Prism starten.

Ohne Skript: In Prism die Instanz auswählen → *Ordner öffnen* → `mods` → `build/libs/dtp-26.3-<version>.jar` hineinkopieren.

**Server:** Für den Gruppenserver kommt dieselbe Jar in den `mods`-Ordner des NeoForge-Servers. Der Mod wird auf Server **und** Client gebraucht, weil er eigene Blöcke und Oberflächen mitbringt. Was beim Aktualisieren eines laufenden Servers zu beachten ist, steht in [Migration](72-migration.md).

---

## Befehle

Admin heißt Cheats an bzw. OP (Berechtigungsstufe 2).

| Befehl | Wirkung |
|---|---|
| `/dtp help` (oder nur `/dtp`) | Alle Befehle mit Kurzerklärung; Admin-Befehle nur für Admins |
| `/dtp price` | Preis des Items in der Hand |
| `/dtp beacon y <y>` | Beacon auf der Y-Achse verschieben (Admin) |
| `/dtp admin market advance <tage>` | Markt- und Contract-Uhr vorstellen, dazu die persönliche Contract-Uhr aller eingeloggten Spieler |
| `/dtp admin market reset` | Marktdruck aller Waren zurücksetzen |
| `/dtp admin market event` | Anstehendes Marktereignis sofort auslösen |
| `/dtp admin market event in <minuten>` | Countdown bis zum nächsten Marktereignis setzen (Ziel bleibt; ≤ `events.warningMinutes` löst die Vorwarnung aus) |
| `/dtp admin market event info` | Restzeit, Typ und Ziel des nächsten Marktereignisses, dazu laufende Ereignisse und Aufschläge (Vollmond, Regen) |
| `/dtp admin market event type <demand_surge\|fast_recovery>` | Typ des nächsten Marktereignisses setzen |
| `/dtp admin market event target <gruppe>` | Ziel des nächsten Nachfrageschubs setzen |
| `/dtp admin market event fire demand_surge <gruppe>` | Nachfrageschub sofort auslösen, ohne die Planung zu ändern |
| `/dtp admin market event fire fast_recovery` | Schnelle Erholung für alle Waren sofort starten, ohne die Planung zu ändern |
| `/dtp admin market event end` | Laufende Marktereignisse sofort beenden |
| `/dtp admin upgrade <id> <stufe>` | Server-Upgrade direkt setzen |
| `/dtp admin upgrade reset` | Alle Server-Upgrades auf 0 (Grenze und Contract-Aushang folgen sofort, keine Erstattung) |
| `/dtp admin playerupgrade <spieler> <id> <stufe>` | Spieler-Upgrade direkt setzen (auch `@a`) |
| `/dtp admin playerupgrade <spieler> reset` | Alle Upgrades der Spieler auf 0 (auch `@a`, keine Erstattung) |
| `/dtp admin contracts reroll` | Contract-Aushang sofort neu würfeln, Rotationsfrist startet neu |
| `/dtp admin beacon move <pos>` | Beacon versetzen (Notausgang) |
| `/dtp admin beacon reset` | Beacon entfernen, Grenze aufheben |
| `/dtp admin blackmarket status` | Wie stark die Einzahlkisten um den Spieler den Schwarzmarkthändler anlocken: größte Kiste, Talersumme, Chance je Prüfung, verbleibende Abklingzeit |
| `/dtp admin blackmarket spawn` | Schwarzmarkthändler beim Spieler erscheinen lassen, unabhängig von Schwelle und Abklingzeit |
| `/dtp admin config diff` | Alle Config-Werte, die vom Standard abweichen, mit Standard. Ändert nichts (ebenso `/dtp admin config reset` ohne Argument) |
| `/dtp admin config reset <key>` | Einen Config-Wert auf den Standard setzen |
| `/dtp admin config reset all` | Alle Config-Werte auf den Standard setzen |

Taler zum Testen: `/give @s dtp:gold_sack 1`. Den Hauptbeacon gibt es per Rezept, solange keiner steht, oder per `/give @p dtp:beacon`. Das Handbuch: `/give @s dtp:guide`.

**Erstes Betreten prüfen:** In einer neuen Welt liegt das Handbuch im Inventar, und die Grundrezepte stehen im Rezeptbuch. Beim zweiten Login und nach dem Tod kommt kein weiteres Buch. Zum erneuten Testen eine neue Welt anlegen: Das Flag hängt am Spieler, Aus- und Einschalten von `onboarding.giveGuide` setzt es nicht zurück.

---

## Wo was liegt

| Inhalt | Ort |
|---|---|
| Balancing-Werte (Preisband, Radien, Contract-Fristen, Schwarzmarkt, Schalter …) | Config `dtp-synced.toml` im Ordner `config` der Instanz bzw. des Servers; eine Kopie in `syncedconfig` der Welt überschreibt sie für diese Welt |
| Handelsliste | `data/dtp/dtp/trade_list/*.json` |
| Filtergruppen des Marktstands | Definition `data/dtp/dtp/market_category/<id>.json` (Symbol, Reihenfolge, optional Freischaltung `unlock` mit `"dimension": "<dimension>"` und/oder `"dragon_killed": true`), Inhalt per Item-Tag `data/dtp/tags/item/market_category/<id>.json` |
| Contract-Vorlagen und Verzauberungswerte für Stufe 4 | `data/dtp/dtp/contract/*.json` |
| Server-Upgrades: Kosten, Stufen, Lage im Beacon-Baum (`tab` world/market/modules, `icon` als Item-ID oder `{"sprite": …}`, `x`/`y`, `level_positions`, `level_icons`, `requires`) | `data/dtp/dtp/upgrade/<id>.json` |
| Spieler-Upgrades und persönliche Module (Tab, Position, Kosten, Werte, Voraussetzungen) | `data/dtp/dtp/player_upgrade/<id>.json`; ob passiv oder Modul, steht im Enum `PlayerUpgrade`. `level_positions` (Plätze der Stufen 2 … max) zeigt jede Stufe als eigenes Feld |
| Contraband: Ladungen, Abklingzeit, Werte | `data/dtp/dtp/contraband/<id>.json`; Item-Tag `data/dtp/tags/item/contraband.json`; Preis je Ladung (`charge_price`) bestimmt den Kurs an der Reparaturbank |
| Schwarzmarkt-Flüche | `data/dtp/enchantment/scam_curse.json`, `toll_curse.json`; Vanilla-Tag `data/minecraft/tags/enchantment/curse.json` |
| Schwarzmarkthändler: Angebote | Einzelangebote `data/dtp/villager_trade/blackmarket/*.json`, Angebotssätze `data/dtp/trade_set/blackmarket/{crates,contraband,rare}.json`, Zuordnung per Tag `data/dtp/tags/villager_trade/blackmarket/*.json` |
| Recycler: was angenommen wird und wie viel Schrott es gibt | `data/dtp/dtp/recycling/*.json`; Schrott als Ware in `trade_list/70_scrap.json` |
| Schmuggelkisten: Inhalt | `data/dtp/loot_table/crate/*.json` |
| Taler in Strukturkisten (Stufen, Chancen, Beträge) | `data/dtp/loot_modifiers/structure_taler.json` |
| Schwarzmarktfunde in Strukturkisten (Loot-Modifier und Fundliste) | `data/dtp/loot_modifiers/structure_finds.json`, `data/dtp/loot_table/gameplay/blackmarket_finds.json` |
| Angelschatz (Loot-Modifier und Fundliste) | `data/dtp/loot_modifiers/fishing_treasure.json`, `data/dtp/loot_table/gameplay/fishing/structure_finds.json` |
| Advancements (Progressionslinie) | `data/dtp/advancement/progression/*.json`; DTP-Trigger `dtp:sold`, `dtp:contract_completed`, `dtp:beacon_started`, `dtp:player_upgrade`, `dtp:server_upgrade` (die letzten beiden mit optional `upgrade` und `level`), registriert in `ModTriggers` |
| Anbindungen an andere Mods | Code in `dev.dtp.compat.<mod>`, Rezepte in `data/dtp/recipe/compat/<mod>/` (siehe [Kompatibilität](70-technischer-rahmen.md#kompatibilität)) |
| Messprotokoll | `<welt>/dtp/protocol.csv` (`market.protocol`) |
| CSV-Werkzeuge der Handelsliste | `scripts/` ([scripts/README.md](../scripts/README.md)) |
| Blocktexturen | erzeugt mit `scripts/block_textures.py` nach `assets/dtp/textures/block/`; Item-Texturen unter `assets/dtp/textures/item/` |

Alle Datapack-Inhalte lassen sich per Datapack überschreiben und mit `/reload` neu laden — eine Balancing-Runde braucht keine Neukompilierung.

---

## Handbuch bearbeiten

Das DTP-Handbuch ist je Sprache **eine Markdown-Datei**: [de_de.md](../src/main/resources/assets/dtp/guide/de_de.md) und [en_us.md](../src/main/resources/assets/dtp/guide/en_us.md). Die Datei bestimmt Kapitel, Seiten, Reihenfolge, Raster und Text. In den Sprachdateien stehen nur die Oberflächen-Texte (`guide.dtp.contents`, `guide.dtp.continued` …). Spielt jemand in einer Sprache ohne eigene Datei, zeigt das Buch Englisch, ersatzweise Deutsch.

| Element | Bedeutung |
|---|---|
| `# Titel` | Neues Kapitel; der Titel erscheint im Inhaltsverzeichnis |
| `## Titel {#id …}` | Neue Seite. `#id` ist Pflicht, eindeutig und in allen Sprachen gleich |
| `grid=<name>` | Rezeptraster rechts auf der Seite: `beacon`, `market_stand`, `trade_chest`, `deposit_chest`, `bulletin_board`, `statistics_board`, `upgrade_table`, `repair_station`, `relay_beacon`, `chunk_loader`, `market_scale`, `blackmarket`, `slot_machine`, `recycler`, `taler`, `guide`. Neue Raster kommen in `GuideGrids` |
| `warning` | Hebt die Seite hervor (roter Titel und Balken) |
| Leerzeile | Neuer Absatz. Einfache Zeilenumbrüche werden zu Leerzeichen |
| `**fett**`, `*kursiv*`, `\*` | Formatierung, wörtliches Sternchen |
| `- Punkt` | Aufzählung mit „•“; eingerückte Folgezeilen gehören zum Punkt |
| `<!-- … -->` | Kommentar für Redaktionshinweise, auch über mehrere Zeilen |

Tabellen, Bilder, Links und Überschriften ab `###` gibt es nicht; sie erscheinen als Text, und das Log meldet eine Warnung mit Zeilennummer. Zu lange Seiten laufen im Spiel automatisch auf einer Folgeseite „(Forts.)“ weiter, ein Absatz wandert dabei als Ganzes.

**Arbeitsablauf**

1. Datei bearbeiten. Im Repo: danach `./gradlew processResources` und im Entwicklungs-Client **F3+T**. In der Prism-Instanz ohne Build: die Datei in ein Resourcepack unter `assets/dtp/guide/` legen, Pack aktivieren, F3+T.
2. Probleme stehen im Log als `Handbook dtp:guide/<sprache>.md: line N: …`. Ist eine Datei gar nicht lesbar, zeigt das Buch eine Fehlerseite.
3. Vor dem Commit `scripts/gametests.sh`: `guide_files` prüft, dass beide Sprachen fehlerfrei sind und dieselben Seiten-IDs, Kapitel, Raster und Hervorhebungen in derselben Reihenfolge haben.

Für den Inhalt gilt die Leitregel aus [Einstieg](01-mod-und-vision.md#einstieg): was es gibt, wo man es findet, was man als Nächstes tut — keine Zahlen, Formeln oder Listen. Ausgenommen sind die Nachschlagekapitel zu Beacon-Effekten, serverweiten und persönlichen Upgrades.

**Nachschlagekapitel pflegen.** Seiten-IDs `fx.<id>`, `server.<id>`, `personal.<id>` mit der Upgrade-ID aus dem Datapack; der Seitentitel ist der Anzeigename aus der Sprachdatei (ohne „Effekt:“). Die Seiten nennen Standardwerte aus Datapack und Config sowie Tag-Inhalte (etwa `dtp:road`). Wer einen solchen Wert, einen Tag oder die Wirkung eines Upgrades ändert, passt die Seite in beiden Sprachen mit an — und hält den Tooltip (`.desc`, `.desc.<stufe>`) bei einem Satz: was + wie viel. Wirkungsbereich, Ausnahmen und Folgerungen gehören ins Buch.

---

## Angelschatz-Fundliste erweitern

Das Spieler-Upgrade *Angelschatz* gibt mit einer Chance je Fang einen Zusatzfund. Drei Dateien teilen sich die Arbeit:

| Was | Datei |
|---|---|
| **Was** gefunden wird | `data/dtp/loot_table/gameplay/fishing/structure_finds.json` — eine normale Loot-Table, hier wird erweitert |
| **Wie oft** | `values` in `data/dtp/dtp/player_upgrade/fishing_treasure.json` — Chance je Fang pro Stufe (0.04 = 4 %) |
| **Woran** es hängt | `data/dtp/loot_modifiers/fishing_treasure.json` — Loot-Modifier an `minecraft:gameplay/fishing`; Code in `FishingTreasureModifier` |

Funde gibt es **nur in offenem Wasser**. Es gilt dieselbe Regel wie für Vanilla-Schätze: rund 5×5 Blöcke freie Wasserfläche ohne Blöcke darin oder darüber. Aus einem 1×1-Loch, wie es AFK-Angelfarmen nutzen, kommt nichts. Luck of the Sea beeinflusst weder die Chance noch die Fundliste, solange die Einträge kein `quality` haben.

Trifft die Chance, wird die Fundliste einmal gewürfelt: ein Pool, ein Wurf. Jeder Eintrag hat ein Gewicht. Die Wahrscheinlichkeit eines Eintrags ist sein Gewicht geteilt durch die Summe aller Gewichte (bei einer Summe von ~100 bringt `weight: 3` rund 3 % der Funde). Für neue Einträge reicht die JSON-Datei. Code wird nicht geändert, und im laufenden Spiel genügt `/reload`.

### Loot-Format ab 26.3 — Achtung, umbenannte Felder

Seit Minecraft 26.3 haben einige Felder der Loot-Tables neue Namen. Ältere Anleitungen und Wiki-Beispiele nutzen noch die alten. **Minecraft ignoriert die alten Namen ohne Fehlermeldung**: Der Eintrag lädt, aber die Funktion wirkt nicht, und das Buch kommt dann unverzaubert heraus.

| Alt (bis 1.21) | Neu (ab 26.3) |
|---|---|
| `"functions": [ … ]` am Eintrag oder Pool | `"modifier": { … }` oder `"modifier": [ … ]` |
| `"function": "minecraft:…"` in einer Funktion | `"type": "minecraft:…"` |
| `"conditions": [ … ]` | `"condition": { … }` |

`type`, `name`, `weight`, `quality` und `rolls` heißen wie bisher. Im Zweifel hilft die Vanilla-Tabelle `data/minecraft/loot_table/gameplay/fishing/treasure.json` aus `build/moddev/artifacts/minecraft-patched-<version>-merged.jar` als Vorlage.

### Einfacher Eintrag

```json
{
  "type": "minecraft:item",
  "name": "minecraft:heart_of_the_sea",
  "weight": 1
}
```

Die ID hat genau einen Namensraum: `minecraft:heart_of_the_sea`, nicht `minecraft:minecraft:heart_of_the_sea`. Eine ungültige oder unbekannte ID lässt den **ganzen Server** beim Start abbrechen (`Failed to load registries due to errors`), nicht nur die Fundliste.

### Verzauberte Bücher und Ausrüstung

Zwei Modifier-Typen verzaubern. Aus einem `minecraft:book` wird dabei automatisch ein verzaubertes Buch, `enchanted_book` muss also nicht eingetragen werden.

**`minecraft:enchant_randomly`** vergibt genau *eine* zufällige Verzauberung auf zufälliger Stufe. Das passt für Bücher mit einer gezielten Auswahl.

```json
{
  "type": "minecraft:item",
  "name": "minecraft:book",
  "weight": 4,
  "modifier": {
    "type": "minecraft:enchant_randomly",
    "options": "#minecraft:on_random_loot"
  }
}
```

**`minecraft:enchant_with_levels`** verzaubert so, als hätte man am Zaubertisch eine bestimmte Stufe bezahlt. Dabei entstehen oft mehrere Verzauberungen. Das passt für Ausrüstung und für starke Bücher. `levels` ist Pflicht und darf eine feste Zahl (Vanilla-Angelschatz: `30`) oder ein Bereich sein.

```json
{
  "type": "minecraft:item",
  "name": "minecraft:diamond_sword",
  "weight": 1,
  "modifier": [
    {
      "type": "minecraft:enchant_with_levels",
      "levels": { "type": "minecraft:uniform", "min": 20, "max": 39 },
      "options": "#minecraft:on_random_loot"
    },
    {
      "type": "minecraft:set_damage",
      "damage": { "type": "minecraft:uniform", "min": 0.1, "max": 0.6 }
    }
  ]
}
```

`set_damage` ist optional und lässt das Item gebraucht aussehen (`1.0` = unbeschädigt). Mehrere Modifier stehen als Liste und werden der Reihe nach angewendet.

**`options`** begrenzt, welche Verzauberungen infrage kommen. Ohne `options` ist bei `enchant_randomly` alles erlaubt, was aufs Item passt. Mögliche Werte:

| Wert | Bedeutung |
|---|---|
| `"#minecraft:on_random_loot"` | Alles, was auch in Vanilla-Truhen vorkommt — der übliche Standard |
| `"#minecraft:in_enchanting_table"` | Nur, was der Zaubertisch auch gibt (kein Mending, kein Frost Walker) |
| `"#minecraft:treasure"` | Nur Schatz-Verzauberungen (Mending, Frost Walker, Soul Speed …) |
| `"minecraft:mending"` | Genau diese eine Verzauberung |
| `["minecraft:mending", "minecraft:unbreaking"]` | Eine Liste zur Auswahl |

Bei `enchant_randomly` sorgt `"only_compatible": true` (Standard) dafür, dass nur aufs Item passende Verzauberungen gewählt werden. Passt keine der `options` zum Item, bleibt es unverzaubert. Die Tests melden das (siehe unten).

### Gewichte im Blick behalten

Richtwerte bei einer Gewichtssumme um 100:

| Eintrag | Gewicht |
|---|---|
| Scherben, einfache Bücher (`enchant_randomly`) | 3–6 |
| Eisen-Ausrüstung, starke Bücher (`enchant_with_levels`) | 2–3 |
| Diamant-Ausrüstung, Vorlagen, Herz des Meeres | 1 |

Jeder neue Eintrag verringert den Anteil aller anderen. Schallplatten gehören bewusst nicht in die Liste, der Test prüft das.

### Prüfen

Drei Stufen, von schnell nach gründlich:

1. **`./gradlew validateFishingFinds`** läuft in wenigen Sekunden ohne Spielstart und hängt automatisch an jedem Build, `runClient`, `runGameTestServer` und `deploy-prism.sh`. Die Prüfung meldet JSON-Syntaxfehler, ungültige IDs (etwa doppelten Namensraum), Gewichte unter 1, fehlendes `levels` bei `enchant_with_levels`, ungültige `options` und alte Feldnamen (`functions`, `function`, `conditions`). Mit `-PfishingFinds=<pfad>` prüft sie die Fundliste eines Datapacks.
2. **`./gradlew runGameTestServer`** führt vier GameTests dazu aus:
   - `fishing_finds_syntax`: Jeder Eintrag wird einzeln mit dem echten Codec gelesen. Dazu kommt dieselbe Validierung wie beim Laden eines Datapacks, also fehlende Kontext-Parameter und Verweise auf andere Tabellen. Ein Item, das es nicht gibt, lässt dagegen schon den Serverstart abbrechen. Der Grund steht dann im Log unter `Errors in element dtp:gameplay/fishing/structure_finds`.
   - `fishing_finds_entries`: Jeder Eintrag wird 20-mal einzeln gewürfelt. Er muss das genannte Item liefern. Hat er eine Verzauberungsfunktion, muss das Ergebnis verzaubert sein. Schallplatten sind verboten.
   - `fishing_treasure`: Ohne Upgrade gibt es beim Vanilla-Angeln keine Zusatzfunde, aus einem 1×1-Loch auch mit Upgrade nicht. In offenem Wasser auf höchster Stufe kommen sie in ungefähr der Rate aus `values` und stammen nur aus der Fundliste. Contraband und Verbrauchsgegenstände des Schwarzmarkts kommen beim Angeln nie heraus.
   - `double_bobbers`: Ohne Upgrade 1 Item je Wurf, mit Double Bobbers so viele, wie `values` in `player_upgrade/double_bobbers.json` angibt, in offenem Wasser und im 1×1-Loch. Zusammen mit Angelschatz kommen Funde in etwa der doppelten Rate, auch zwei in einem Wurf.

   Fehlermeldungen nennen Pool, Eintragsnummer (ab 0) und Item, z. B. `Pool 0, Eintrag 40 (minecraft:book): hat eine Verzauberungsfunktion, minecraft:book kam aber unverzaubert heraus`.
3. **Im Spiel:** nach `/reload` mit `/loot give @s loot dtp:gameplay/fishing/structure_finds` einzelne Funde ziehen.

---

## Entscheidungen im Code

Wie offene Punkte im Code entschieden sind. Jede Zeile ist leicht änderbar.

| Frage | Entscheidung |
|---|---|
| Wo wird die Deepslate-Freischaltung gekauft? | Am **Beacon**, als Server-Upgrade |
| Contract-Lieferung | Über das **Bulletin-Board** |
| Form des Territoriums | **Quadrat**, wie die Vanilla-World-Border; Effekte des Hauptbeacons gelten im selben Quadrat. Neben-Beacons wirken in einem Zylinder |
| Teildeckung beim Kauf | Abgelehnt, nichts entnommen, Fehlbetrag wird genannt |
| Wechselgeld | In die erste Kiste, aus der entnommen wurde |
| Reihenfolge beim Entnehmen | Je Kiste kleinste Stufe zuerst — große Münzen werden nur bei Bedarf angebrochen |
| Erz oder Barren in der Handelsliste | **Barren** (eine Form je Rohstoff) |
| Verkauf außerhalb des Territoriums | 5 % des Basiswerts, **erzeugt keinen Marktdruck** |
| Wohin geht der Erlös? | In die **Einzahlkisten** in Reichweite des Marktstands, auch beim Verkauf aus der Hand; sonst ins Inventar |
| Trend in der Preisanzeige | Druckanzeige (Rot–Grün-Skala) immer; Prozentwert und Trendpfeil erst nach dem Server-Upgrade *Marktinformation* |
| Rolle des Trade-Chests | **Warenlager** für den Marktstand; verkauft nicht selbst. Redstone am **Marktstand** verkauft je Signal einen Stapel |
| Bulletin-Board | **Kein Inventar**; Lieferung aus den Trade-Chests in Reichweite, Belohnung in die Einzahlkiste |
| Contracts nachfüllen und neu würfeln | **Periodische Rotation** des ganzen Aushangs (`contracts.rotationDays`, Standard 3 Tage), kein Nachfüllen nach Annahme, kein manuelles Neuwürfeln; Bonusfrist ab Erzeugen |
| Bulletin-Board außerhalb des Territoriums | **Nur lesbar**: Annehmen und Liefern gesperrt (auch vor dem Beacon und in anderen Dimensionen), Abbrechen erlaubt; kein 5-%-Abschlag wie beim Marktstand |
| Abbrechen eines Contracts | Zurück in den Aushang **ohne** Lieferfortschritt, als gewöhnliches Angebot, das die nächste Rotation ersetzt; Frist läuft weiter. Eine eigene Freigabe-Funktion gibt es nicht |
| Hauptbeacon | **Craftbar, solange keiner steht** (`dtp:beacon_shaped`, gemeinsame Basis `ConditionalShapedRecipe` mit `dtp:upgrade_shaped`); danach nur noch per `/give`. GameTests `beacon_recipe` und `guide_grids` |
| Handbuch | Eigener Bildschirm (`GuideScreen`) statt Written Book: Text kommt aus `assets/dtp/guide/<sprache>.md` (`GuideBooks`, Client-Reload-Listener) und bleibt nach Updates aktuell; ein Resourcepack kann ihn überschreiben. Die Raster stehen in `GuideGrids`, weil der Client nicht alle Rezepte kennt; der GameTest `guide_grids` prüft sie gegen die echten Rezepte, `guide_files` den gleichen Aufbau beider Sprachen, `guide_parser` den Markdown-Leser, `onboarding` das erste Betreten |
| Maustasten | 26.3 liest Maus und Tastatur über **SDL3**: die linke Maustaste ist `1`, die rechte `3` (früher GLFW: 0 und 1). Immer `InputConstants.MOUSE_BUTTON_LEFT` usw. vergleichen, nie Zahlen |
| Uhren nur mit Spielern | **Alle Uhren laufen nur mit Spielern online** (Markt, Aushang, Countdown der Marktereignisse, Schwarzmarkt-Tag); keine Nachholung von Abwesenheit. Angenommene Contracts laufen auf einer **persönlichen Uhr**, nur während ihr Besitzer online ist. Speicherformat Markt Version 6, Spieler-Contracts Version 2 |
| Gesperrte Warengruppen | **Nether** am Marktstand und als Ereignisziel erst mit Nether-Zugang, **End** erst nach dem ersten besiegten Enderdrachen (Vanilla-Flag des Drachenkampfs, serverweit). Neu freie Gruppen werden Ziel des *nächsten* anstehenden Marktereignisses, das angekündigte bleibt. Speicherformat Markt Version 7 |
| Amboss „Too Expensive“ | Server-Upgrade *Meisterschmied* am Beacon (`anvil_cap`): Kosten höchstens 39 für alle Spieler; der Client kennt die Stufe über `ServerUpgradeSync`. **Ohne Mixin**, weil Vanilla das Ergebnis ab 40 vor dem NeoForge-Event verwirft: Der Mod rechnet es in `AnvilCap` nach. **Bei Minecraft-Updates** den Nachbau gegen `AnvilMenu.createResultInternal` prüfen; der GameTest `anvil_cap` vergleicht beide |
| Beacon-Effekte Bauen und Expedition | Baumeister (Reichweite und Rückbau), Federfall, Ruhige Nächte, Expeditionsvorbereitung; persönliches Modul Aufwind. **Ohne Mixin**: Attribut-Modifier alle 10 Ticks (`BuilderEffects`), sonst Events. Rückbau führt eine kurze Liste auf Server und Client (Paket `rebuild_mark`), weil der Client das Abbautempo vorrechnet; Blöcke mit BlockEntity sind ausgenommen. ID `effect_rebuild` ist in `effect_builder` aufgegangen (`WorldProgress` Version 5). GameTests `beacon_building`, `rebuild`, `expedition_prep`, `updraft` |
| Beacon-Effekt Stallmeister | Zuchtpause, Fohlen, Reittiere, Wege. **Ohne Mixin**. Zucht über `BabyEntitySpawnEvent` (`AnimalHusbandry`), für Frosch, Sniffer und Schildkröte zusätzlich `EntityTickEvent.Post` mit `instanceof`-Filter, weil sie kein Junges werfen. Reittempo und Wegebonus als Attribut-Modifier alle 10 Ticks (`BuilderEffects`), nur für gelenkte Reittiere aus dem Entity-Tag `dtp:mounts`; der Glückliche Ghast über `FLYING_SPEED`. Pferde berechnen Fallschaden ohne `LivingFallEvent`, daher `LivingIncomingDamageEvent`. ID `effect_stablemaster` (früher `effect_horse_breeding` plus `effect_roads`, `WorldProgress` Version 5). **Bei Minecraft-Updates** neue Reittiere in `dtp:mounts` aufnehmen, prüfen, ob ihr `getRiddenSpeed` weiter `MOVEMENT_SPEED` oder `FLYING_SPEED` liest, und ob neue Tiere `spawnChildFromBreeding` ohne Event überschreiben. GameTests `beacon_building`, `animal_husbandry` |
| Beacon-Effekt Imkerei | **Ohne Mixin** (`Beekeeping`): Nestfüllung über das Block-Update der Honigstufe (`NeighborNotifyEvent`), weil Vanilla die UUID der Biene im Stock verwirft, nur mit einer Biene aus demselben Stock im selben Tick (`EntityJoinLevelEvent`); beim Ernten ohne Lagerfeuer bleiben die Bienen im Stock (abgebrochener Beitritt); Zuschlag und Ernte-Bonus am Tickende. Bestäubung baut Vanillas `BeeGrowCropGoal` nach. **Bei Minecraft-Updates** prüfen, ob `BeehiveBlockEntity.releaseOccupant` die Stufe weiter per `setBlockAndUpdate` setzt und ob `BeeGrowCropGoal` neue Pflanzen kennt. GameTest `beekeeping` |
| Beacon-Effekt Landwirtschaft | **Ohne Mixin** (`Farming`). Der Trampelschutz bricht `BlockEvent.FarmlandTrampleEvent` ab. Das Pflanzenwachstum gibt nach `CropGrowEvent.Post` mit Chance eine Zusatzstufe, nicht über `Pre`, weil `Pre` Vanillas Wurf nicht kennt. ID `effect_agriculture` (früher `effect_breeding`, `WorldProgress` Version 4 benennt um, siehe [Migration](72-migration.md)). Die Zuchtpause gehört seit Version 5 zum Stallmeister. **Bei Minecraft-Updates** prüfen, ob `CropBlock`/`StemBlock.randomTick` weiter `fireCropGrowPost` aufrufen und `FarmlandBlock.fallOn` weiter über `onFarmlandTrample` läuft. GameTest `farming` |
| Persönliche Module | **Spieler-Upgrades der Art Modul**, gekauft im Baum der Werkstatt, wirksam nur ausgerüstet (Tab *Module*, höchstens 3 Slots über das Upgrade *Modulslots*). Ausrüsten kostenlos, nur an einer Werkstatt im Territorium oder im Radius eines Neben-Beacons; Wirkung überall. Speicherformat Spieler-Upgrades Version 2 |
| Module und Skills | Rausch, Barriere, Abbaufläche 3×3 (Spitzhacke und Schaufel, Abschalten nur per Werkstatt), Aufwind und die Skills Dash, Doppelsprung (ohne Fallschutz), Schutzschild (Resistenz) und Heilimpuls (Regeneration, auch Mitspieler). **Höchstens ein Skill**, eine Fähigkeitstaste, eine gemeinsame Abklingzeit; Kosten nur Abklingzeit; Abklingzeiten als eigenes Attachment (Version 1), Werte als `params` im Datapack |
| Chunkloader | **Ohne Mixin**, über einen NeoForge-`TicketController` (`ChunkLoaders`). Die Restlaufzeit liegt zentral in `ChunkLoaderData`, die Tickets sind daraus abgeleitet: beim Start verworfen, im Sekundentakt gesetzt, solange jemand online ist, sonst entfernt. Laufzeit in Spielticks, nur mit Spielern online. `server.properties` bleibt unberührt. Die schwebende Perle zeichnet `ChunkLoaderRenderer` aus dem Vanilla-Item. GameTest `chunk_loader` |
| Schleimrute | Item-ID `dtp:oozing_rod`; der Alias `dtp:dowsing_rod` in `ModItems` erhält Ruten aus älteren Welten ([Migration](72-migration.md)) |

Alle Zahlen (Basiswerte, Kosten, Radien) sind Werte für die laufende Balancing-Runde, keine Festlegung.
