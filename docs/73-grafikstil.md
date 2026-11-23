# Grafikstil

> Wie die Grafiken von DTP aussehen und wie neue entstehen: Blöcke, Items, Upgrade-Symbole und der Schwarzmarkthändler.
> Ziel ist, dass jede neue Grafik aussieht, als gehöre sie schon immer dazu – und als gäbe es sie in Vanilla.

---

## Grundsätze

1. **Vanilla zuerst.** DTP soll wirken wie ein Teil von Minecraft, nicht wie ein Texturpaket. Farben, Formen und Detailgrad orientieren sich an Vanilla. Wo es ein passendes Vanilla-Item oder -Effektsymbol gibt, wird es benutzt statt neu gezeichnet.
2. **Pixelgenau.** 16×16 je Blockfläche und Item, keine Kantenglättung, keine Verläufe, keine Subpixel. Jedes Pixel ist gesetzt, nicht gerechnet.
3. **Licht von oben links.** Glanz oben links, Schatten unten rechts, auf allen Blöcken, Items, Symbolen und dem Händler.
4. **Kontur im eigenen Material.** Items und Symbole haben eine dunkle Kontur im Ton ihres Materials (dunkles Rot beim Feuerlöscher, dunkles Braun beim Leder), wie die Vanilla-Items. Kein reines Schwarz als Universalkontur.
5. **Funktion vor Dekor.** Ein Block oder Item muss auf normale Spielentfernung bzw. in Originalgröße im Inventar erkennbar sein. Details, die dabei verschwimmen, fliegen raus.
6. **Gedämpfte Farben.** Keine Neonfarben, keine übersättigten Töne. Rot, Grün und Gelb nur als gedeckte Akzente.
7. **Der Taler sparsam.** Das Taler-Motiv (Goldmünze mit „T“) ist das Erkennungszeichen der Wirtschaft – aber nur dort, wo es um Geld geht. Nicht jeder Block bekommt eine Münze.
8. **Unterscheidung über Form, nicht nur Farbe.** Verwandte Dinge (Trade-Chest/Einzahlkiste, Beacon/Neben-Beacon, die Schmuggelkisten) teilen die Bauweise und unterscheiden sich in Beschlägen, Symbolen und Silhouette.

---

## Werkzeugkette

Alle Grafiken entstehen aus Skripten in `scripts/` (nur Python 3, keine weiteren Abhängigkeiten). **PNG-Dateien nie von Hand bearbeiten** – die nächste Ausführung des Skripts überschreibt sie. Änderungen gehören ins Skript.

| Skript | Erzeugt | Ziel |
|---|---|---|
| `block_textures.py` | Blocktexturen inkl. Beacon-Teile | `assets/dtp/textures/block/` |
| `item_textures.py` | Item-Texturen, Schmuggelkisten, Slot-Symbol des Silberbeutels | `assets/dtp/textures/item/`, `textures/gui/sprites/container/slot/` |
| `upgrade_icons.py` | eigene Upgrade-Symbole | `assets/dtp/textures/gui/sprites/upgrade/` |
| `trader_texture.py` | Textur des Schwarzmarkthändlers | `assets/dtp/textures/entity/blackmarket_trader.png` |

Alle vier schreiben ihre PNGs über `write_png()` aus `scripts/png_writer.py`; das Modul ist nur zum Importieren da.

**Arbeitsweise in den Skripten:**

- **Handgesetzte Raster.** Jede Fläche ist eine Liste von Textzeilen, ein Zeichen je Pixel. `.` ist transparent bzw. übernimmt die Grundschicht.
- **Tonrampen statt Einzelfarben.** Ziffern `1`–`5` sind die fünf Stufen der Hauptrampe eines Motivs (`1` Glanz … `5` Kontur bzw. tiefster Schatten), `6`–`9`,`0` die der Zweitrampe. Dasselbe Raster ergibt mit Eiche, Fichte oder Dunkeleiche drei passende Varianten.
- **Gemeinsame Vorlagen.** Planken (`PLANKS`, `VPLANKS`), die Kiste (`chest_face`), die Münze (`COIN`), der Stein der Katalysatoren (`STONE`) werden einmal gezeichnet und wiederverwendet.
- **Vorschau immer ansehen.** Jedes Skript hat `--preview datei.png`:
  - **Blöcke:** echte Modelle in 3D, mit Glas.
  - **Items:** Originalgröße im Inventarslot plus vergrößert.
  - **Upgrade-Symbole:** auf den Baumfeldern neben Vanilla-Effekten.
  - **Händler:** Ansichten vorn, seitlich, hinten und schräg mit Minecrafts eigener Würfel-UV-Abbildung.

  Eine Grafik gilt erst als fertig, wenn sie in der Vorschau in Originalgröße funktioniert.

---

## Gemeinsame Palette

Die Farben sind in den Skripten zentral definiert (`PALETTE` und die Rampen in `block_textures.py` und `item_textures.py`). Neue Grafiken nehmen diese Werte, statt neue zu erfinden.

| Material | Verwendung | Kernfarben |
|---|---|---|
| Eiche | Marktstand, Bulletin-Board-Tafel, Fackelstiel | `#C29D62` … `#5A4423` |
| Fichte | Rahmen, Werkstatt, Schmuggelkisten | `#86643A` … `#33230F` |
| Dunkeleiche | Schwarzmarkt, Gun-Schaft, Händler-Leder | `#5B3E22` … `#1F1408` |
| Kistenholz | Trade-Chest, Einzahlkiste | `#C48C4A` … `#4A2D11` |
| Eisen | Beschläge, Neben-Beacon, Werkzeuge | `#E6E6E6`, `#BDBDBD`, `#8C8C8C`, `#5A5A5A` |
| Gold (Taler) | Münzen, Beacon, Einzahlkiste, Akzente | `#FFDF37`, `#E5B82D`, `#A98721`, `#96781D` (aus `gold_taler.png`) |
| Pergament | Aushänge, Etiketten, Schriftrolle | `#F0E6C6`, `#DACA9B`, `#B49E70`, Tinte `#7A6650` |
| Rotes Tuch | Markise, Siegel, Nadeln | `#B13B2E`, `#8C2C23`, `#621C17` |
| Schwarzmarkt | Markise, Etiketten, Händlerrobe | Dunkelrot `#5E2B26`/`#45201C`, Kohle `#3D3737`/`#2A2525` |
| Tiefenschiefer | Schwarzmarkt-Sockel | `#73737A` … `#2E2E33` |
| Obsidian | Beacon-Sockel, Chunkloader-Rahmen | `#3E2C59`, `#281D3B`, `#150F20` |
| Beacon-Kristall | Kristall, Endlose Fackel | `#F4FFFD`, `#B6F4ED`, `#6CD8CE`, `#37A39A`, `#21706A` |
| Enderperle | Chunkloader (Sockel, Podestring, Rahmenecken) | `#8CF4E2`, `#2CCDB1`, `#258474`, `#105E51`, `#063931` (aus Vanillas `ender_pearl.png`) |

**Drei Familien, eine Welt:**
- **Handel:** helles Holz, Eisen, rot-cremefarbenes Tuch, Gold.
- **Schwarzmarkt:** Dunkeleiche, Tiefenschiefer, Kohle, gedecktes Dunkelrot.
- **Fortschritt:** Glas, Obsidian, Cyan-Kristall, Gold bzw. Eisen.

Alle drei teilen Holzrampen, Eisen und Gold und gehören dadurch zusammen.

---

## Blöcke

### Regeln

- **Flächen und Dateinamen:** `<block>_front`, `_back`, `_side`, `_top`, `_bottom`. Blöcke mit unterschiedlichen Seiten nutzen `_left`/`_right` (bisher nur die Werkstatt).
- **Ausrichtung:** Gerichtete Blöcke haben die Eigenschaft `facing` und setzen beim Platzieren die Front zum Spieler. Ihr Modell erbt von `minecraft:block/cube`, die Belegung ist:

  | Modellfläche | Textur |
  |---|---|
  | `north` | Front |
  | `south` | Rückseite |
  | `east` | linke Seite, vom Spieler vor der Front aus gesehen |
  | `west` | rechte Seite |

  Der Blockstate dreht per `y` (0/90/180/270).
- **Kanten müssen anschließen:**
  - **Oberseite:** Ihre oberste Pixelzeile ist die Vorderkante.
  - **Front:** Ihre linke Spalte liegt an der Ostseite. Streifen, die über die Kante laufen (Markise), stehen auf der Oberseite deshalb gespiegelt.
- **Symmetrische Rahmen:** Bei Blöcken, die von allen Seiten gleich aussehen, hat der Rahmen auf allen vier Kanten denselben Ton – keine helle Ober- und dunkle Unterkante (siehe [Erkenntnisse](#erkenntnisse-aus-den-spieltests)).
- **Holzplanken** kommen aus der gemeinsamen Vorlage, damit Bretterbreite, Fugen und Maserung überall gleich sind.

### Die Blöcke im Überblick

| Block | Idee | Kennzeichen |
|---|---|---|
| Marktstand | Marktbude | Rot-creme gestreifte Markise mit Zackensaum, Waren auf der Theke (Äpfel, Brot, Smaragd, Karotten), kleiner Taler an der Front |
| Schwarzmarkt | dunkles Gegenstück zum Marktstand, gleicher Aufbau | Dunkeleiche, zerfranste Markise in Dunkelrot/Kohle, verhüllte Ware mit Goldschimmer und Trank, Tiefenschiefer-Sockel mit Klappe |
| Bulletin Board | Anschlagtafel | Fichtenrahmen, Eichentafel, überlappende Aushänge mit Nadeln und Schlagschatten, Wachssiegel, Zettel mit Taler-Lohn |
| Statistik-Tafel | Flipchart, Verwandte des Bulletin Boards | Fichtenrahmen, **ein** großer Pergamentbogen statt vieler Zettel, Eisen-Klemmleiste oben (auch auf Seiten und Oberseite), Säulendiagramm in Tinte mit drei steigenden Säulen, kleiner Taler oben rechts, unten zwei Stufen umgeschlagener Bögen; Rückseite Fichtenplanken mit Querstrebe |
| Trade-Chest | Warenkiste | Kistenholz, Deckelfuge, Eisenbeschläge und -lasche, Preisanhänger an Schnur |
| Einzahlkiste | Geldkiste, gleiche Bauart | Goldbeschläge, Schloss in Talerform (Stiel des „T“ ist das Schlüsselloch), Münzschlitz oben |
| Beacon | aufgewerteter Vanilla-Beacon | 3D-Modell: halbtransparenter Glaskörper mit Goldrahmen, Obsidiansockel mit Goldrunen, Goldpodest, 45° gedrehter, leuchtender Kristall, goldener Taler-Ring im Deckel |
| Neben-Beacon | kleiner Verwandter | Gleicher Aufbau, Eisenrahmen ohne Licht-/Schattenkanten, niedriger Sockel ohne Runen, kleinerer, schwächer leuchtender Kristall, kein Ring |
| Chunkloader | Glaskasten-Verwandter ohne Kristall | Gleicher Glaskörper mit dunklem Obsidianrahmen ohne Licht-/Schattenkanten, Ecken im Perlentürkis, Obsidiansockel mit türkiser Kante und Runen, flacher Podestring. Statt des Kristalls schwebt die Vanilla-Enderperle (Renderer), ohne Restlaufzeit liegt sie still |
| Werkstatt | Werkbank wie der Schmiedetisch | Eisenkante, Werkzeugwand (Hammer, Zange, Axt), Schubladen; links Säge, rechts Regal mit Barren und Beutel; oben Plan, Ambossplatte, Goldbarren |
| Reparaturbank | Eichenwerkbank mit Schleifrad, Verwandte der Werkstatt in hellem Holz | Nur die Ecken der Platte in Eisen; vorn Schleifrad im Fichtenständer mit Kurbel, das in einen Wassertrog mit Eisenband taucht; seitlich Schraubstock und hängende Kupfer-Ölkanne; oben Schlitz des Rads, Lederflicken mit Nadel, Münzschale (einziger Taler), Wetzstein |
| Recycler | Werkstatt-Gerät aus Eisen, Verwandter des Spielautomaten | Eisenkasten mit hellem Rahmen; oben Einwurfgitter über dunklem Schacht, vorn in einer Kastenöffnung ein Schleifrad aus Tiefenschiefer mit Eisenachse, darunter der Schrottschacht mit Kupferkante; Seiten mit Nieten und zwei Kupferbändern, Rückseite mit Lüftungsschlitzen, unten die Ausgabeklappe. Im Betrieb (`_front_on`) glühen die Öffnung hinter dem Schleifrad und der Schacht orange. Item **Schrott**: Haufen aus verbogenem Eisenblech mit Lederriemen-Rest |
| Spielautomat | Spielautomat aus Eisen, einziger Handelsblock ohne Holz | 3D-Modell: Eisengehäuse, rote Bande wie das Schild des Marktstands rundum, vorn Schild mit Taler und zwei Lämpchen, 1 px tief eingesetztes Walzenfenster mit drei cremefarbenen Walzen und roter Gewinnlinie, roter Knopf, Münzschlitz in Gold, dunkle Auswurfschale mit Goldkante; rechts ein Hebel mit Goldknauf, der beim Drehen unten steht. Oben endet der Knauf 1 px unter dem Deckel, damit er neben einem Nachbarblock nicht mit dessen Oberseite flimmert. Die Symbole sind Items, die `SlotMachineRenderer` ins Fenster zeichnet |

### Beacon-Technik

Die Beacons sind echte 3D-Modelle wie der Vanilla-Beacon (`models/block/beacon.json`, `relay_beacon.json`):

- **Elemente:** Glaskörper, Sockel, Podest, gedrehter Kristall mit Spitze.
- **Teiltexturen:** `_glass`, `_base` und `_core` teilen sich ihre 16×16 in getrennte UV-Bereiche.
- **Transparenz:** In 26.3 entscheidet das Spiel je Fläche anhand der Pixel. Halbtransparente Pixel (Glas, Alpha `0x30`–`0x9A`) kommen in die Translucent-Schicht, deckende Teile bleiben solid. Kein Code nötig, nur `noOcclusion()` am Block.
- **Leuchten:** Der Kristall leuchtet über `light_emission` am Element.
- **Chunkloader** (`models/block/chunk_loader.json`): dieselbe Technik ohne Kristall. Die Enderperle ist kein Modellelement, sondern das Vanilla-Item, das `ChunkLoaderRenderer` wie die Marktwaage über dem Podestring zeichnet. Sie behält damit automatisch das Aussehen aus einem Resourcepack.

---

## Items

### Regeln

- 16×16, transparenter Hintergrund, Kontur im Materialton, Licht oben links.
- **Werkzeuge** (Fackel, Schleimrute, Spawnstab) liegen diagonal – Griff unten links, Spitze oben rechts – und nutzen `item/handheld`. Alles andere nutzt `item/generated`.
- **Aufrechte Gegenstände** (Feuerlöscher, Glocke) stehen senkrecht und werden wie ein Eimer gehalten.
- **Gegenstände mit eigener Haltung** (Gun) bekommen eigene `display`-Werte im Modell. Diese sind aus der Transformationskette berechnet, nicht geraten (siehe [Erkenntnisse](#erkenntnisse-aus-den-spieltests)).

### Familie Währung

| | Kupfer | Silber | Gold |
|---|---|---|---|
| Münze (`*_taler`) | rotbraun | hellgrau | warmgelb |
| Beutel (`*_pouch`) | braunes Leder | kühles graublaues Tuch | gedämpft dunkelrotes Tuch |
| Sack (`*_sack`) | wie Beutel | wie Beutel | wie Beutel |

- **Münze:** achteckige 12×12-Münze mit eingeprägtem „T“ und diagonalem Glanz. Die Form ist für alle Stufen gleich, nur das Metall wechselt.
- **Beutel:** klein, mit Kordel zugeschnürt, Münzen der Stufe schauen oben heraus.
- **Sack:** größer, breiter Boden, Falten, fest verschnürter Hals, aufgenähte Münze der Stufe.
- **Stufe auf einen Blick:** Metall der Münzen und Farbe des Beutels zeigen beide die Stufe.

### Familie Schmuggelkisten

- **Gleiche Kiste:** Alle Sorten sind dieselbe Fichtenkiste mit Deckelbrettern und Eisenecken.
- **Sortensymbol:** Links sitzt ein Kohle-Etikett (Schwarzmarktfarben) mit dem Symbol der Sorte: Ei, Spawn-Ei mit Flecken, Setzling, Weizengarbe, Eimer mit Fisch.
- **Zufall:** Rechts steht ein mit Kreide aufs Holz gemaltes „?“ für den zufälligen Inhalt.
- **Auswahl im Spiel:** `items/contraband_crate.json` wählt das Bild per `minecraft:select` über die Komponente `dtp:crate_loot`.
- **Neue Sorte:** Sie braucht nur `SYM_<SORTE>` (5×5) im Skript, ein Item-Modell und einen `case`. Ohne eigenes Bild greift die Rückfall-Kiste mit großem „?“.

### Familie Contraband

- **Eigener Charakter:** Jedes Contraband-Item darf eine eigene Form und eigene Farben haben, bleibt aber im Stil (Kontur, Licht, Detailgrad).
- **Modernes erlaubt:** Der Feuerlöscher ist bewusst modern.
- **Gemeinsame Formen:** Verwandte Items teilen eine Form, etwa die facettierte Steinform von Amethyst- und Regenkatalysator.
- **Querverweise:** Kleine Bezüge zur Mod sind erwünscht. Die Endlose Fackel trägt den Beacon-Kristall, die Rückkehr-Rolle eine Karte mit Beacon-Symbol.

---

## Upgrade-Symbole

Die Bäume von Beacon und Werkstatt stehen voller Vanilla-Symbole. Eigene Symbole dürfen darin **nicht auffallen**.

- **Vorrang:**
  1. Vanilla-Item (`"icon": "minecraft:book"`)
  2. Vanilla-Effektsymbol (`"icon": {"sprite": "minecraft:mob_effect/speed"}`)
  3. eigener DTP-Block (`"icon": "dtp:bulletin_board"`)
  4. erst dann ein neues Symbol (`"icon": {"sprite": "dtp:upgrade/<name>"}`)
- **Größe:**
  - **Gegenstände:** 16×16 wie Items.
  - **Effekt-artige Symbole** (Fähigkeiten, Bewegung, Heilung): 18×18 wie die Vanilla-Effekt-Sprites.

  Gezeichnet wird zentriert in der echten Größe.
- **Farben:** Neue Symbole nehmen Vanilla-Farben, keine DTP-Sonderfarben. Beispiele: Glas und Kork vom Trank, die Holzfarben der Vanilla-Werkzeugstiele, die Schweinefarben, der Stiefel und die Bewegungsstriche vom Speed-Effekt, die Spielerfarben (Doppelsprung).
- **Verwandte Symbole unterscheidbar halten:**
  - **Dash:** zeigt den Speed-Stiefel mit Nachbildern, weil Rush schon das Speed-Symbol trägt.
  - **Durability:** zeigt die Vanilla-Eisenspitzhacke wie Vein Mining, aber als 18×18-Symbol mit hellgrünem Plus unten rechts und vollem Haltbarkeitsbalken darunter. Plus und Balken unterscheiden es von den Spitzhacken von Efficiency, Fortune und Vein Mining.
  - **Contract-Qualität:** zeigt den Aushang mit roter Nadel von Contract-Stufe (`pinned_note`, 16×16, 2 px nach links gerückt) mit demselben hellgrünen Plus wie Durability unten rechts. Das Plus unterscheidet ihn von Contract-Stufe.
- **Gegenstände mit Tiefe:** Kisten, Pakete und Truhen stehen in leichter Schrägansicht (Front, Deckel und Seite sichtbar, Tiefe 45° nach rechts oben). Der Helfer `box()` in `upgrade_icons.py` zeichnet sie. Verzierungen wie Kordeln folgen den Flächen, statt flach darüberzuliegen.
- **Symmetrie prüfen:** Symmetrische Gegenstände wie die Sanduhr Spalte für Spalte gegenprüfen. Ein einzelnes verrutschtes Pixel fällt in Originalgröße als Knick auf.
- **Stufen:** `level_icons` (parallel zu `level_positions`, ab Stufe 2) gibt einzelnen Stufenfeldern ein eigenes Symbol, etwa Ofentempo Stufe 2 = Schmelzofen.
- **Modul-HUD:** Es kennt das Datapack auf einem dedizierten Server nicht. Ändert sich das Symbol eines Moduls mit Abklingzeit, muss es auch in `client/ModuleHud.java` geändert werden.

---

## Schwarzmarkthändler

- **Modell:** `WanderingTraderRenderer`, also `VillagerModel`. Textur 64×64 im UV-Layout des Wandering Traders. Keine eigene Geometrie: Alle Animationen (Laufen, Kopfdrehen, Trinken, Schaden, Tod) bleiben Vanilla.
- **Box-UV-Regel** (`ModelPart.Cube`): Ein Würfel bei (u, v) mit Größe (b, h, t) belegt:

  | Fläche | Position |
  |---|---|
  | oben | (u+t, v) |
  | unten | (u+t+b, v) |
  | rechts | (u, v+t) |
  | vorn | (u+t, v+t) |
  | links | (u+t+b, v+t) |
  | hinten | (u+2t+b, v+t) |

  „Rechts“ meint die Seite der Figur.
- **Bereiche:**

  | Bereich | Position | Größe |
  |---|---|---|
  | Kopf | (0,0) | 8×10×8 |
  | Kapuze (Hutschicht) | (32,0) | 8×10×8 |
  | Nase | (24,0) | 2×4×2 |
  | Körper | (16,20) | 8×12×6 |
  | Robe | (0,38) | 8×20×6 |
  | Ärmel | (44,22) | 4×8×4 |
  | verschränkter Unterarm | (40,38) | 8×4×4 |
  | Beine | (0,22) | 4×12×4 |
  | Hutkrempe | (30,47) | bleibt transparent |
- **Transparenz:** Entities rendern als Cutout ohne Rückseiten-Culling. Damit gehen die Gesichtsöffnung der Kapuze, der ausgefranste Saum und die unsichtbare Krempe.
- **Gesicht:** Illager-Aufbau (Brauen Zeile 4–5, Augen Zeile 6, dunkle Mundpartie, lange Nase). Haut etwas dunkler als Vanilla, Stirn unter der Kapuze abgeschattet, bernsteinbraune Iris, kein Leuchten.
- **Kleidung:** schwarze Robe mit violettem Futter in der Mitte, dunkelroter Saum und Kapuzenrand, Leder in Dunkeleiche, Gold nur an Beutelverschluss, Gürtelschnalle und Kragenspange.

---

## Erkenntnisse aus den Spieltests

- **Gleiche Flächen, gleiche Kanten.** Hat jede Fläche oben links eine helle und unten rechts eine dunkle Rahmenkante, trifft an jeder Würfelkante hell auf dunkel, und der Block „springt“ beim Umrunden. Beim Neben-Beacon behoben (`glass(..., uniform=True)`). Dasselbe gilt für einzelne Eckpunkte: entweder alle vier Ecken oder keine.
- **Gehaltene Items berechnen statt raten.** Die Haltung in der Hand ergibt sich aus Armrotation, `ItemInHandLayer` (−90° um X, 180° um Y) und dem `display`-Block. Für die Gun ist sie aus dieser Kette berechnet. Eine eigene Armhaltung kommt über eine Enum-Erweiterung (`DtpArmPoses`, `META-INF/enumextensions.json`) und `IClientItemExtensions#getArmPose`, ohne Mixin.
- **Transparenz kommt aus den Pixeln.** Ob eine Blockfläche durchsichtig rendert, entscheiden in 26.3 die Pixel im genutzten UV-Bereich. Deckende Teile im selben Bild bleiben solid.
- **Fertig ist, was im Spiel funktioniert.** Die Vorschauen der Skripte sind nah dran, aber Haltung, Größenwirkung im Baum und Glas sollten nach jeder größeren Änderung im Client geprüft werden.

---

## Checkliste für eine neue Grafik

1. Gibt es ein passendes Vanilla-Item, -Effektsymbol oder einen DTP-Block? Dann das nehmen.
2. Familie bestimmen (Handel, Schwarzmarkt, Fortschritt bzw. Währung, Kisten, Contraband) und deren Materialien aus der Palette nehmen.
3. Im passenden Skript als Raster zeichnen. Gemeinsame Vorlagen wiederverwenden.
4. Vorschau ansehen: in Originalgröße erkennbar? Hebt sich das Motiv von seinen Verwandten ab, ohne nur die Farbe zu wechseln?
5. Modell, Blockstate bzw. Datapack-Eintrag anlegen. Bei gerichteten Blöcken `facing` und die Flächenbelegung oben beachten.
6. `./gradlew compileJava`, `scripts/gametests.sh`, dann im Spiel ansehen.
