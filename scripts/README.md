# scripts

Hilfsskripte für Test, Build, Grafiken und die Pflege der Handelsliste. Alle Skripte lassen sich aus dem Projektordner aufrufen und brauchen nur Python 3 bzw. Bash. In der Mod-Jar steckt keines davon.

## Von Hook und CI verwendet: Pfad nicht ändern

| Skript | Aufgerufen von | Zweck |
|---|---|---|
| `gametests.sh` | `.githooks/pre-push` | Prüft zuerst die Client-Ressourcen mit `check_assets.py`, führt dann `./gradlew runGameTestServer` aus, schreibt das Log nach `build/gametests.log` und fasst fehlgeschlagene Tests und das Ergebnis des JSON-Prüflaufs zusammen. Weitere Argumente gehen an Gradle. Exit-Code ungleich 0, wenn ein Test oder die Asset-Prüfung fehlschlägt. |
| `check_assets.py` | `gametests.sh`, `.github/workflows/build.yml` | Prüft `assets/` ohne Spielstart: gültiges JSON, Item-Definitionen und Blockstates zeigen auf vorhandene Modelle, Modelle auf vorhandene Parents und Texturen, Upgrade-Sprites haben ein Bild, `de_de` und `en_us` haben dieselben Schlüssel und jedes Item hat einen Namen. Verweise auf Vanilla und andere Mods werden übersprungen. Ungenutzte Modelle und Texturen erscheinen als Hinweis. |

Wer eines dieser Skripte umbenennt oder verschiebt, muss den Hook, `build.yml` und `docs/71-entwicklung-und-test.md` mitziehen.

## Entwicklung

| Skript | Zweck |
|---|---|
| `deploy-prism.sh` | Baut den Mod und kopiert die Jar in den `mods`-Ordner einer Prism-Instanz (Standard: `"DTP 26.3"`). Ersetzt ältere `dtp-*.jar` und sichert `dtp-synced.toml` (Instanz und Welten) als `.last`, damit die aktuellen Defaults greifen. Mit `--keep-config` bleibt die Config erhalten. Funktioniert auch in der Flatpak-Sandbox über `flatpak-spawn --host`. |

## Texturen

Die Generatoren sind die Quelle der PNGs unter `src/main/resources/assets/dtp/textures/`. PNGs nie von Hand bearbeiten, Änderungen gehören ins Skript ([docs/73-grafikstil.md](../docs/73-grafikstil.md)). Alle Generatoren schreiben über `png_writer.py`.

| Skript | Zweck |
|---|---|
| `block_textures.py` | Erzeugt alle Blocktexturen (16×16) nach `textures/block/`. Jede Fläche ist ein handgesetztes Pixelraster mit gemeinsamer Palette; Ziffern stehen für die Tonstufen des Holzes. Mit `--preview datei.png` entsteht zusätzlich eine isometrische Vorschau aller Blöcke. |
| `item_textures.py` | Erzeugt die Item-Texturen (16×16) nach `textures/item/`: Taler, Beutel und Säcke je Stufe, die Schmuggelkisten je Sorte und die Contraband-Items, dazu das Slot-Symbol des Silberbeutels für das Schwarzmarkt-Menü. Mit `--preview datei.png` entsteht eine Vorschau in Originalgröße auf Inventarslots und vergrößert. Die Kistensorte wählt `items/contraband_crate.json` per `minecraft:select` über die Komponente `dtp:crate_loot`; eine neue Sorte ohne eigenes Bild zeigt die Rückfall-Kiste mit Fragezeichen. |
| `upgrade_icons.py` | Erzeugt die eigenen Upgrade-Symbole (GUI-Sprites) nach `textures/gui/sprites/upgrade/` im Vanilla-Stil: 16×16 wie Items, Effekt-artige (Dash, Doppelsprung, Heilpuls) 18×18 wie Vanilla-Effekte. Mit `--preview datei.png` stehen sie auf den Feldern des Baums neben Vanilla-Effekten (aus dem Sources-Jar). |
| `trader_texture.py` | Erzeugt die Textur des Schwarzmarkthändlers (`textures/entity/blackmarket_trader.png`, 64×64 im UV-Layout von `VillagerModel`, das der `WanderingTraderRenderer` nutzt). Jede Würfelfläche ist ein eigenes Raster; Kapuzenöffnung, Robensaum und Hutkrempe nutzen Transparenz (Entities rendern als Cutout). Mit `--preview datei.png` entstehen Ansichten von vorn, links, hinten und schräg, gerendert mit Minecrafts Würfel-UV-Abbildung, dazu die Textur vergrößert. |
| `png_writer.py` | Kein eigenes Skript: gemeinsames `write_png()` (RGBA-PNG ohne Abhängigkeiten), das die vier Generatoren importieren. |

Gerichtete Blöcke (Marktstand, Schwarzes Brett, Statistik-Tafel, Trade-Chest, Einzahlkiste, Schwarzmarkt, Werkstatt, Reparaturbank, Recycler) nutzen `minecraft:block/cube` mit `north` = Front, `south` = Rückseite, `east` = linke und `west` = rechte Seite (vom Spieler aus gesehen, der vor der Front steht); der Blockstate dreht per `facing`. Die Beacons sind 3D-Modelle wie der Vanilla-Beacon (Glaskörper, Sockel, Podest, um 45° gedrehter Kristall mit `light_emission`); ihre Teiltexturen `_base` und `_core` nutzen getrennte UV-Bereiche. Halbtransparente Pixel im Glas legen die Fläche automatisch in die Translucent-Schicht, deckende Teile bleiben solid. Der Spielautomat ist ebenfalls ein Elementmodell: Gehäuse mit 1 px tief eingesetztem Walzenfenster und Hebel (Atlas `_lever`), zwei Modelle für Hebel oben und unten. Das Bulletin-Board ist ein Multipart: der Würfel (`bulletin_board`, auch das Item-Modell) plus, nur bei `flag=true`, Fahnenmast mit Flagge (`bulletin_board_flag_raised`, Atlas `_flag`), der aus dem Schacht in der Oberseite (`bulletin_board_top`) bis über den Block hinausragt; eingeholt steckt alles im Block. Die Vorschau rendert die echten Modell-JSONs.

## Handelsliste (optional)

Die Handelsliste wird normalerweise direkt in `src/main/resources/data/dtp/dtp/trade_list/*.json` gepflegt. Für größere Änderungen lässt sie sich als Tabelle bearbeiten, z. B. in LibreOffice.

| Skript | Zweck |
|---|---|
| `trade_list_to_csv.py` | Exportiert die JSON-Dateien nach `scripts/trade_list.csv` (oder in die angegebene Datei). Die CSV ist gitignored. |
| `csv_to_trade_list.py` | Schreibt die JSON-Dateien aus der CSV zurück. Die Spalte `file` bestimmt die Zieldatei, die Zeilenreihenfolge bleibt erhalten. Dateien ohne CSV-Zeile bleiben unverändert. |

**Vor dem Bearbeiten immer frisch exportieren.** Eine alte CSV überschreibt beim Zurückschreiben alle Preise, die seitdem im JSON geändert wurden. Nach dem Zurückschreiben die CSV löschen.

```sh
python3 scripts/trade_list_to_csv.py
# scripts/trade_list.csv bearbeiten
python3 scripts/csv_to_trade_list.py
rm scripts/trade_list.csv
```
