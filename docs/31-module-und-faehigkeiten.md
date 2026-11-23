# Module und Fähigkeiten

> Persönliche Module: Spieler-Upgrades, die nur wirken, solange sie in einem Modulslot stecken. Kauf und Kostenmodell wie bei allen [persönlichen Upgrades](30-persoenliche-upgrades.md).

---

## Wozu Module

Manche Wirkungen sind nicht in jeder Spielphase erwünscht, etwa Flächenabbau beim Bauen, oder man soll zwischen ihnen je nach Vorhaben wählen. Solche Upgrades sind **Module**: permanent gekauft, aber nur wirksam, solange sie ausgerüstet sind. Alles andere (Herzen, Contract-Plätze, Kettenabbau, Verzauberungsboni) ist passiv und braucht keinen Slot.

Es gibt zwei Arten:

| Art | Wirkt | Beispiele |
|---|---|---|
| **Automatisches Modul** | Von selbst, solange ausgerüstet | Rausch, Barriere, Abbaufläche 3×3, Aufwind |
| **Skill** | Wird per Taste ausgelöst, danach Abklingzeit | Dash, Doppelsprung, Schutzschild, Heilimpuls, Fischernetz |

Ob ein Upgrade passiv, Modul oder Skill ist, steht im Code (`PlayerUpgrade.Kind`), nicht im Datapack.

---

## Modulslots

| ID | Name | Stufe | Preis | Slots |
|---|---|---|---|---|
| `module_slots` | Modulslots | 1 | 5.000 | 1 |
| | | 2 | 25.000 | 2 |
| | | 3 | 100.000 | 3 |

**Höchstens 3 Slots**, fest im Code (`PlayerUpgrades.MAX_MODULE_SLOTS`), unabhängig vom Datapack. Gekauft im Tab Körper der Werkstatt.

Ein Modul lässt sich erst kaufen, wenn mindestens ein Modulslot gekauft ist. Sonst wäre es bezahlt, aber wirkungslos. Im Baum sind Module mit einer blauen Ecke markiert, Fähigkeiten (aktiver Effekt, nur eine gleichzeitig ausrüstbar) mit einer orangen.

---

## Ausrüsten

Ausgerüstet wird im Tab **Module** der Werkstatt, dargestellt wie die Effektauswahl am Beacon: oben die Slots, darunter alle Module.

- **Kostenlos und jederzeit**, aber nur an einer Werkstatt **im Territorium oder im Radius eines Neben-Beacons** ([Beacon-Effekte](21-beacon-effekte.md#neben-beacon)). So geht es auch an einer Außenbasis, sogar im Nether, aber nicht mitten im Kampf. Das ist die einzige Hürde. Kaufen geht weiterhin nur im Territorium.
- **Wirkung überall**, auch in Nether und End. Anders als Beacon-Effekte sind Module nicht an einen Radius gebunden.
- **Höchstens ein Skill.** Wer einen zweiten Skill ausrüstet, ersetzt den ersten an dessen Platz. Automatische Module füllen die übrigen Slots.
- Wer eine Wirkung gerade nicht will (etwa 3×3 beim Bauen), rüstet an der Werkstatt ab. Einen Schalter unterwegs gibt es bewusst nicht.
- Ausgerüstete Module überleben Tod und Relog, wie alle Spieler-Upgrades.

---

## Automatische Module

| ID | Name | Tab | Stufen | Preise | Wirkung | Parameter |
|---|---|---|---|---|---|---|
| `rush` | Rausch | Körper | 2 | 4.000 / 20.000 | Nach einem Monster-Kill 5 s Tempo I und +1 Block Aufsammelradius, Stufe 2 zusätzlich Stärke I. Zählt auch Kills per Projektil | `values` 5 (Sekunden), `speed` 1, `strength` 0 / 1, `pickup_range` 1 (Blöcke) |
| `barrier` | Barriere | Körper | 2 | 15.000 / 60.000 | Nach einem Treffer im Kampf oder durch Feuer Absorption I (2 Schildherzen) für 10 s, danach 60 s Abklingzeit. Stufe 2: Absorption II (4 Schildherzen), 30 s Abklingzeit | `absorption` 1 / 2, `duration` 10, `cooldown` 60 / 30 |
| `area_mining` | Abbaufläche 3×3 | Ausrüstung | 1 | 40.000 | Spitzhacke und Schaufel bauen 3×3 natürliches Gestein bzw. Boden ab, jeder Block kostet Haltbarkeit | – |
| `updraft` | Aufwind | Körper | 1 | 20.000 | Raketen beim Elytra-Flug werden mit 50 % Chance nicht verbraucht | `chance` 50 |

**Barriere.** Ausgelöst wird sie von Schaden mit verursachendem Wesen (Nahkampf, Projektil, Mob-Explosion) und von Feuer. Fall, Ertrinken, Hunger und Ähnliches lösen nicht aus, sonst wäre der Schild schon verbraucht, wenn der Kampf beginnt. Die Barriere hat eine **eigene Abklingzeit**, getrennt von der der Skills.

**Abbaufläche 3×3.** Mit Spitzhacke oder Schaufel fallen die acht Nachbarn in der Ebene quer zur getroffenen Seite mit. Mit fällt nur, was in derselben Blockgruppe wie der Mittelblock liegt: für die Spitzhacke `#dtp:area_mining/pickaxe` (natürlich generiertes Gestein: Stein, Bruchstein, Granit, Diorit, Andesit, Tuffstein, Kalzit, Tropfstein, Tiefenschiefer und Bruch-Tiefenschiefer, glatter Basalt, Sandstein, Badlands-Terrakotta, Netherrack, Basalt, Schwarzstein, Magmablock, Endstein) und als eigene Gruppe `#dtp:area_mining/obsidian` (nur Obsidian: nimmt nur Obsidian mit und wird von keinem Gestein mitgenommen), für die Schaufel `#dtp:area_mining/shovel` (Grasblock, Erde, Grobe Erde, Wurzelerde, Podsol, Myzel, Trampelpfad, Ackerboden, Schlamm, Schlammige Mangrovenwurzeln, Lehm, Kies, Sand, Roter Sand, Seelensand, Seelenerde, Schneeblock). Die Regel ist symmetrisch: Gras nimmt Erde mit und Erde Gras, Stein Tiefenschiefer und Tiefenschiefer Stein. Erze, Verdächtiger Sand und Kies, Baublöcke (Steinziegel, Treppen, Mauern, Stufen) und alles außerhalb der Gruppe bleiben stehen. Liegt der Mittelblock in keiner Gruppe des Werkzeugs, gibt es keine Fläche. Die Gruppen sind Block-Tags und lassen sich per Datapack erweitern. Jeder Block läuft über den normalen Abbau und kostet Haltbarkeit. Die Drops der Fläche, auch des Mittelblocks, landen direkt im Inventar, was nicht passt, fällt um den Spieler. Schleichend an Erz oder Stamm gewinnen Vein Mining und Baumfällen, falls gekauft ([Persönliche Upgrades](30-persoenliche-upgrades.md#ausrüstung)).

**Aufwind** ist für die Erkundung außerhalb des Territoriums gedacht und wirkt überall, nicht im Kreativmodus. Ohne Mixin: Das Event feuert vor Vanillas Verbrauch, der Stapel wird vorab um eins erhöht.

---

## Skills

| ID | Name | Stufen | Preise | Wirkung | Abklingzeit | Parameter |
|---|---|---|---|---|---|---|
| `dash` | Dash | 1 | 12.000 | Schneller Satz waagerecht in Blickrichtung mit leichtem Anheben, setzt den Fallschaden zurück | 4 s | `strength` 1,0, `lift` 0,25 |
| `double_jump` | Doppelsprung | 1 | 10.000 | Ein zweiter Sprung in der Luft, einmal bis zur nächsten Landung. Kein Fallschutz | – (nur Landung) | `strength` 0,6 |
| `guard` | Schutzschild | 2 | 15.000 / 60.000 | Resistenz II, Stufe 2: Resistenz IV, für 15 s | 60 s | `resistance` 2 / 4, `duration` 15 |
| `heal_pulse` | Heilimpuls | 2 | 20.000 / 80.000 | Regeneration I für 30 s, Stufe 2: Regeneration II für 30 s, für den Auslöser und alle Spieler im Umkreis von 6 Blöcken | 60 s | `regeneration` 1 / 2, `duration` 30 / 30, `radius` 6 |
| `fishing_net` | Fischernetz | 1 | 30.000 | Tötet alle Kabeljaue, Lachse, Tropenfische und Kugelfische im Wasser im Umkreis von 3 Blöcken, Beute und Erfahrung gehen an den Auslöser | 5 s | `radius` 3 |

Die ersten vier liegen im Tab Körper, das Fischernetz im Tab Ausrüstung neben Doppelschwimmer.

Resistenz wird vor dem Verzauberungsschutz abgezogen. Mit gedeckeltem Schutz (Protection IV + Upgrade Schutz, 80 %) lässt der Schutzschild auf Stufe 2 nur noch 4 % des Schadens nach Rüstung durch, für 15 s. Das ist so gewollt: ein kurzer Notfallknopf mit 60 s Abklingzeit ([Zahlen](30-persoenliche-upgrades.md#stärke-der-ausrüstungs-upgrades)).

**Dash** scheitert, wenn man genau senkrecht nach oben oder unten blickt. **Doppelsprung** geht nicht beim Fliegen, Schwimmen, Klettern, Reiten oder Elytra-Flug. Beim Sprinten gibt er wie der Vanilla-Sprung einen Schub nach vorn, sonst würde er sich wie ein Bremsen anfühlen. **Heilimpuls** trifft nur lebende Spieler in derselben Dimension.

**Fischernetz.** Welche Arten das Netz fängt, steht im Entity-Tag `#dtp:fishing_net` (per Datapack erweiterbar). Nur Fische, die im Wasser schwimmen; Fische aus dem Eimer (Aquarien) bleiben verschont. Getötet wird als Spielerangriff, die Drops landen direkt im Inventar, was nicht passt, fällt dem Spieler vor die Füße; die Erfahrungskugeln fliegen von selbst zu ihm. Ist kein Fisch in Reichweite, scheitert der Einsatz ohne Abklingzeit. GameTest `fishing_net`.

### Auslösen

- **Eine Fähigkeitstaste** für alle Skills: Steuerung → „DTP — Fähigkeiten" → „Fähigkeit auslösen", Standard **R**. Gesendet wird nur mit ausgerüstetem Skill und ohne offenes Menü.
- Der **Doppelsprung** geht auch über die **Sprungtaste in der Luft**. Gezählt wird ein neues Drücken, nachdem die Taste seit dem Abheben einmal losgelassen wurde, damit der normale Absprung nicht gleich mitzählt.
- Der Client meldet nur den Tastendruck. Ob der Einsatz gilt, entscheidet der Server, dort laufen alle Prüfungen und Wirkungen. Während der Abklingzeit zeigt die Aktionsleiste die Restsekunden (beim Doppelsprung bleibt sie stumm).

### Kosten: nur die Abklingzeit

Alle Skills teilen sich **eine Abklingzeit** (gespeichert unter dem gemeinsamen Schlüssel `skill`). Ein Skill-Wechsel an der Werkstatt setzt sie nicht zurück. Sie beginnt nur bei einem erfolgreichen Einsatz.

Einzige Kosten eines Einsatzes ist die Abklingzeit, bewusst kein Hunger. Sollen Skills einmal etwas kosten, dann Taler. Weil nie mehrere Skills gleichzeitig ausgerüstet sind (außer durch Teamwork mehrerer Spieler), darf jeder einzelne stark sein.

Abklingzeiten werden als Spielzeit-Tick gespeichert, ab dem das Modul wieder bereit ist (`ModuleCooldowns`). Die Spielzeit ist auf Server und Client gleich und läuft in allen Dimensionen gemeinsam. Abklingzeiten **überdauern Tod und Relog**, sonst ließen sie sich durch Neueinloggen umgehen.

---

## HUD

Ausgerüstete Module mit Abklingzeit (der Skill und die Barriere) erscheinen **unten links** als Symbol:

| Rahmen | Bedeutung |
|---|---|
| Golden | Bereit |
| Grau, Restsekunden darüber | Abklingzeit läuft |

Ohne solche Module wird nichts gezeichnet. Die Symbole sind im Client fest hinterlegt, passend zu den Standard-Dateien in `data/dtp/dtp/player_upgrade/`, weil das Datapack auf einem dedizierten Server nur dem Server bekannt ist.
