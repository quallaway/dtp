# Persönliche Upgrades

> Was ein einzelner Spieler an der Werkstatt für sich kauft. Die Module und Fähigkeiten stehen in [Module und Fähigkeiten](31-module-und-faehigkeiten.md), die gemeinsamen Upgrades am Beacon in [Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md).

---

## Grundsatz

Taler werden für **permanente Freischaltungen** ausgegeben. Jeder Kauf verändert den Spielzustand dauerhaft. Ein Verbrauchsmarkt würde Taler zu einer zweiten Ressourcenwährung machen und zum Horten einladen. Die wenigen Verbrauchsgegenstände gibt es nur beim Schwarzmarkthändler ([Contraband](13-contraband.md)).

Es gibt zwei Ebenen:

| | Spieler-Upgrade | Server-Upgrade |
|---|---|---|
| Wirkt für | Den Käufer | Alle |
| Gekauft an | **Werkstatt** | Hauptbeacon |
| Bezahlt aus | Einzahlkisten um die Werkstatt | Einzahlkisten um den Beacon |
| Gespeichert | Am Spieler | Am Server |

Was ohnehin allen zugutekommt (Markt, Grenze, Contract-Stufen), ist ein Server-Upgrade. Persönlich ist nur, was wirklich nur dem Käufer nützt: Herzen, Werkzeugfähigkeiten, Verzauberungsboni, Hof-Upgrades, Contract-Plätze.

---

## Die Werkstatt

Die Werkstatt (`dtp:upgrade_table`) ist ein eigener Block, damit der Marktstand reiner Handel bleibt.

```
 . C .     C = Kupfersack
 B S B     B = Buch, S = Schmiedetisch
 P P P     P = beliebige Holzbretter
```

Die Oberfläche zeigt die Upgrades als Baum in vier Tabs, dazu den Tab **Module** zum Ausrüsten ([Module und Fähigkeiten](31-module-und-faehigkeiten.md)):

| Tab | Inhalt |
|---|---|
| **Körper** | Maximale Gesundheit, Schwimmtempo, Modulslots und die Module für Kampf und Bewegung |
| **Ausrüstung** | Verzauberungsboni, Baumfällen, Vein Mining, Angeln, Abbaufläche 3×3, Fischernetz |
| **Handel** | Mehr Contracts, Quick Delivery, Auftragsuhr, Fernlieferung |
| **Hof** | Wolle, Jungtiere, Flächenernte, Reiche Ernte |

Mehrstufige Upgrades zeigen **jede Stufe als eigenes Feld** mit eigenem Preis und eigener Beschreibung, so sieht man sofort, was die nächste Stufe kostet. Ausnahme ist Maximale Gesundheit (10 Stufen in einem Feld). Gekauft wird immer die nächste Stufe, aufgeteilt wird nur die Anzeige (Datapack-Feld `level_positions`). Module sind im Baum mit einer blauen Ecke markiert.

**Symbole:** Das Datapack-Feld `icon` ist eine Item-ID (`"minecraft:book"`) oder ein GUI-Sprite (`{"sprite": "minecraft:mob_effect/speed"}`). Vanilla-Effekte liegen unter `minecraft:mob_effect/<effekt>`, die eigenen Symbole unter `dtp:upgrade/<name>` (`textures/gui/sprites/upgrade/`, erzeugt von `scripts/upgrade_icons.py`). `level_icons` gibt aufgeteilten Stufen ein eigenes Symbol, parallel zu `level_positions` ab Stufe 2 (Beispiel: Ofentempo Stufe 2 zeigt den Schmelzofen). Das Modul-HUD kennt das Datapack auf einem dedizierten Server nicht und hat die Symbole der Module fest eingetragen (`client/ModuleHud.java`).

Gesperrte Upgrades sind sichtbar, nicht versteckt. Fehlende Voraussetzungen werden konkret benannt.

---

## Kaufregeln

- **Nur im Territorium.** Gekauft wird nur an einer Werkstatt im Territorium, vor dem Setzen des Beacons also gar nicht. Sonst ließe sich die Frühspiel-Grenze umgehen, indem man sich vorab persönlich ausbaut. In Nether und End lässt sich nichts kaufen.
- **Bezahlt aus der Einzahlkiste** im Umkreis von 2 Blöcken um die Werkstatt (`deposit.radius`), wie am Marktstand. Die Kiste muss nicht dem Käufer gehören: Wer Taler offen liegen lässt, kann bestohlen werden ([Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md#bezahlt-wird-aus-der-einzahlkiste)).
- **Voraussetzungen** (`requires` im Datapack) nennen andere Spieler-Upgrades mit Mindeststufe. Jede Stufe setzt außerdem die vorige voraus. Module brauchen Modulslots Stufe 1.
- **Keine Rückerstattung.** Module umzustecken ist davon unberührt und kostenlos.
- **Tod kostet keine Upgrades.** Gekaufte Upgrades, Stufen und ausgerüstete Module überleben den Tod. Attribut-Modifier werden nach dem Respawn neu gesetzt, man startet mit vollen Herzen. Verloren gehen nur getragene Taler, die wie jedes Item fallen.

Ob ein Upgrade passiv, ein Modul oder ein Skill ist, steht im Code (`PlayerUpgrade.Kind`), nicht im Datapack. Ein Datapack soll eine dauerhafte Wirkung nicht unbemerkt in eine slotpflichtige verwandeln.

---

## Kostenmodell

Kosten stehen je Upgrade im Datapack (`data/dtp/dtp/player_upgrade/<id>.json`, Server-Upgrades in `dtp/upgrade/`). Die Formel steht in `CostCurve`:

```
Preis(Stufe) = costs[Stufe − 1]                              wenn costs angegeben ist
Preis(Stufe) = base_cost × growth ^ (Stufe − 1)              sonst
```

| Form | Verwendet bei |
|---|---|
| **Explizite `costs`-Liste** | Allen Spieler-Upgrades außer Maximale Gesundheit, und bei den mehrstufigen Server-Upgrades Grenze, Effektslots, Contract-Stufe, Aushang-Platz, Ofentempo, Mending-Aura |
| **`base_cost` + `growth`** | Maximale Gesundheit (2.500 × 1,5), Langsamerer Preisverfall (100.000 × 5), Schnellere Preiserholung (200.000 × 5) |
| **Nur `base_cost`** | Einstufigen Server-Upgrades: der Preis selbst |

Fehlen beide Angaben, gilt `base_cost` 100 und `growth` 1,65. Die Wirkung je Stufe steht in `values` (Index 0 = Stufe 1), Parameter der Module in `params`. Ein Upgrade ohne Datei ist nicht kaufbar.

Die expliziten Listen sind die Regel, weil sie sich je Stufe einzeln kalibrieren lassen. Wie die Preise zu den Einnahmen stehen, steht in [Preise und Größenordnungen](61-preise-und-groessenordnungen.md).

---

## Passive Upgrades

Passive Upgrades wirken, sobald sie gekauft sind, überall und ohne Slot. Preise in Talern je Stufe.

### Körper

| ID | Name | Stufen | Preise | Wirkung je Stufe |
|---|---|---|---|---|
| `max_health` | Maximale Gesundheit | 10 | 2.500 / 3.750 / 5.625 / 8.438 / 12.656 / 18.984 / 28.477 / 42.715 / 64.072 / 96.108 | +1 Herz je Stufe, bis +10 Herzen |
| `swim_speed` | Schwimmtempo | 3 | 3.000 / 10.000 / 30.000 | +15 / +30 / +45 % Schwimmtempo beim Schwimmen |
| `module_slots` | Modulslots | 3 | 5.000 / 25.000 / 100.000 | 1 / 2 / 3 persönliche Modulslots, siehe [Module und Fähigkeiten](31-module-und-faehigkeiten.md) |

Maximale Gesundheit kostet insgesamt 283.325 Taler. Gesundheit ist ein permanenter Attribut-Modifier (`max_health`). Schwimmtempo ist ein transienter Modifier auf NeoForge `swim_speed`, den ein Server-Tick nur in der Schwimmhaltung (Sprint-Schwimmen) setzt; am Grund gehen bleibt Vanilla-Tempo. Vanilla multipliziert `swim_speed` nach Depth Strider, beide Boni wirken also multiplikativ.

### Ausrüstung

| ID | Name | Stufen | Preise | Wirkung je Stufe |
|---|---|---|---|---|
| `efficiency_bonus` | Effizienz | 3 | 10.000 / 40.000 / 120.000 | +10 / +20 / +30 % Abbautempo, mit jedem Werkzeug |
| `unbreaking_bonus` | Haltbarkeit | 2 | 10.000 / 40.000 | +1 / +2 Unbreaking auf getragene Rüstung und Werkzeuge in der Hand |
| `protection_bonus` | Schutz | 1 | 20.000 | +1 Protection auf jedes getragene Rüstungsteil |
| `fortune_bonus` | Glück+ | 2 | 15.000 / 75.000 | +1 / +2 Fortune auf Werkzeuge in der Hand |
| `looting_bonus` | Plünderung+ | 2 | 8.000 / 60.000 | +1 / +2 Looting auf Schwert oder Speer in der Haupthand |
| `lure_bonus` | Köder+ | 2 | 8.000 / 60.000 | +1 / +2 Lure auf die Angel |
| `luck_of_the_sea_bonus` | Glück des Meeres+ | 2 | 15.000 / 75.000 | +1 / +2 Luck of the Sea auf die Angel |
| `tree_felling` | Baumfällen | 4 | 3.000 / 15.000 / 60.000 / 100.000 | Bis zu 8 / 16 / 32 / 64 Stämme, direkt ins Inventar; danach 5 s +1 Block Aufsammelradius (`params` `pickup_seconds` 5, `pickup_range` 1) |
| `vein_mining` | Vein Mining | 2 | 6.000 / 20.000 | Bis zu 8 / 16 Erzblöcke, direkt ins Inventar |
| `fishing_treasure` | Angelschatz | 2 | 40.000 / 120.000 | 4 / 8 % Chance je Fang auf einen Strukturfund |
| `double_bobbers` | Doppelschwimmer | 1 | 150.000 | Jeder Angelwurf bringt 2 Items |

**Verzauberungsboni** addieren sich zur Verzauberung des Gegenstands, **bewusst auch über das Vanilla-Maximum hinaus** (Fortune III + 2 = Fortune V). Sie gelten nur, wo die Verzauberung passt, und werden nie ins Item geschrieben. Fortune-Bonus gibt es nicht mit Silk Touch, sonst würde eine Loot-Table, die Silk Touch nicht zuerst prüft, den Block selbst vervielfachen. Looting gilt nur für Schwerter und Speere, die Waffen, die Looting in 26.3 tragen können. Mit Axt, Streitkolben, Dreizack, Bogen, Armbrust, Werkzeug oder leerer Hand gibt es keinen Bonus. Bei Kills per Projektil zählt das Schwert in der Haupthand, nicht der Bogen. Stirbt ein Mob an Brand, Lava oder dem Pfeil eines anderen Mobs, gilt der Spieler nicht als Killer: Dann greift weder der Bonus noch das Looting der Waffe, wie in Vanilla.

**Effizienz** ist keine zusätzliche Efficiency-Stufe, sondern ein prozentualer Aufschlag aufs Attribut `block_break_speed`. Efficiency wirkt quadratisch (Stufe² + 1), schon Diamant mit Efficiency VI baut Stein sofort ab. Zahlen zu allen Ausrüstungs-Upgrades stehen unter [Stärke der Ausrüstungs-Upgrades](#stärke-der-ausrüstungs-upgrades).

#### Stärke der Ausrüstungs-Upgrades

Gerechnet mit den Formeln aus Minecraft 26.3 (`enchantment/*.json`, Block-Loot-Tables, `FishingHook`, `CombatRules`). Erwartungswerte, keine Messungen.

**Maßstab.** Die Angel-Upgrades und Looting+ haben sich im Spiel bewährt. Ihre letzte Stufe bringt auf ein Werkzeug mit Vanilla-Maximum etwa +40 bis +80 % und kostet 60.000 bis 75.000:

| Upgrade | Stufe 2 auf Vanilla-Maximum | Kosten gesamt |
|---|---|---|
| Köder+ | Lure III → V: Wurfzyklus ~11 s → ~6 s, ≈ +80 % Fänge | 68.000 |
| Glück des Meeres+ | Luck of the Sea III → V: Schatz 11,3 → 15,8 % (+40 %), Müll 4 → 0 % | 90.000 |
| Plünderung+ | Looting III → V: Phantomhaut Ø 2,0 → 3,0 (+50 %) | 68.000 |
| Glück+ | Fortune III → V: Erze Ø ×2,20 → ×3,14 (+43 %) | 90.000 |

**Haltbarkeit.** Werkzeuge, Waffen und Elytren nehmen mit Unbreaking *n* nur mit Chance 1/(*n*+1) Schaden, halten also (*n*+1)-mal so lange. Rüstung rechnet Vanilla schwächer (0,4 · *n*/(*n*+1) Chance, keinen Schaden zu nehmen).

| Diamant-Werkzeug (1.561) | ohne Upgrade | +1 | +2 |
|---|---|---|---|
| ohne Unbreaking | 1.561 | 3.122 | 4.683 |
| Unbreaking III | 6.244 | 7.805 (+25 %) | 9.366 (+50 %) |

Netherit (2.031) mit Unbreaking III: 8.124 → 12.186. Elytra (432 s, zählt nicht als Rüstung): Unbreaking III ≈ 29 min Flug, +2 ≈ 43 min. Diamant-Brustplatte mit Unbreaking III: 754 → 792 Treffer (+5 %), praktisch wirkungslos.

*Verhältnis zu Mending.* Die [Mending-Aura](20-territorium-und-serverweite-effekte.md) repariert im ganzen Beacon-Gebiet 1 (Stufe II: 2) Haltbarkeit alle 10 Sekunden, mit Unbreaking III also 0,4 (0,8) Nutzungen pro Sekunde — weit weniger, als man abbaut (Diamant Efficiency V auf Stein: bis ~2,9 Blöcke/s). Sie füllt nebenbei auf, eine volle Reparatur dauert mit Stufe I 4,3 h (Diamant) bzw. 5,6 h (Netherit), und ersetzt Mending nicht. Das Upgrade verlängert die Zeit zwischen zwei Reparaturen. Dort bleibt Mending überlegen: Ein Werkzeug mit Unbreaking III braucht 1 EP je 8 Blöcke und hält mit EP-Nachschub unbegrenzt. Bis 4.3 gab es eine dritte Stufe (+75 %); sie wurde gestrichen, damit Mending fern der Basis nicht überflüssig wird.

**Schutz.** Protection gibt 1 Verzauberungsschutzpunkt je Stufe und Teil, gedeckelt bei 20 Punkten = 80 % (`CombatRules`). Das Upgrade zählt für jedes getragene Teil, also +4 Punkte (16 %) für ein volles Set:

| Set | ohne Upgrade | mit Upgrade |
|---|---|---|
| ohne Verzauberung | 0 % | 16 % |
| Protection II | 32 % | 48 % |
| Protection III | 48 % | 64 % |
| Protection IV | 64 % | **80 % (Deckel)** |

Bis 4.3 gab es eine zweite Stufe. Mit Protection IV verschenkte sie alles, sie half nur schwächerer Rüstung.

Resistenz wird **vor** dem Verzauberungsschutz abgezogen, beide multiplizieren sich. Mehrere Resistenz-Effekte addieren sich nicht, es gilt der stärkste. Übrig bleibt (nach Rüstungspunkten):

| | Protection IV | Protection IV + Upgrade |
|---|---|---|
| ohne Resistenz | 36 % (2,8× effektive Gesundheit) | 20 % (5×) |
| Beacon-Effekt Resistenz I | 28,8 % | 16 % (6,25×) |
| Schutzschild 1 (Resistenz II) | 21,6 % | 12 % (8,3×) |
| Schutzschild 2 (Resistenz IV) | 7,2 % | 4 % (25×, 15 s alle 60 s) |

Mit +10 Herzen aus Maximale Gesundheit hat ein Spieler im Gebiet etwa 4,5-mal so viel effektive Gesundheit wie mit Vanilla-Endgame-Rüstung.

**Effizienz.** Abgebaut wird in ganzen Ticks: ⌈30 · Härte / Tempo⌉ und danach 5 Ticks Pause; beim Sofortabbau (Tempo > 30 · Härte) entfällt die Pause. Ticks je Block mit +0 / 10 / 20 / 30 %:

| Werkzeug | Stein (1,5) | Deepslate (3) | Deepslate-Erz (4,5) |
|---|---|---|---|
| Eisen ohne Efficiency | 8 / 7 / 7 / 6 | 15 / 14 / 13 / 12 | 23 / 21 / 19 / 18 |
| Diamant Efficiency III | 3 / 3 / 3 / 2 | 5 / 5 / 5 / 4 | 8 / 7 / 7 / 6 |
| Diamant Efficiency V | 2 / 2 / 2 / 2 | 3 / 3 / 3 / 3 | 4 / 4 / 4 / 4 |
| Netherit Efficiency V | 2 / 2 / 2 / **sofort** | 3 / 3 / 3 / 2 | 4 / 4 / 4 / 3 |

Mit Endgame-Werkzeug bringt das Upgrade wenig. Der Sprung ist der Sofortabbau von Stein mit Netherit Efficiency V auf Stufe 3 (7 Ticks → 1 Tick je Block), ein bewusstes Endspiel-Ziel.

**Glück+.** Was von Fortune profitiert, nach Formel (Fortune III → IV → V):

| Formel | Blöcke | Wirkung |
|---|---|---|
| `ore_drops`: Ø-Faktor (*L*+1)/2 + 1/(*L*+2), ungedeckelt | Kohle-, Kupfer-, Eisen-, Gold-, Diamant-, Smaragd-, Lapis-, Netherquarz- und Nethergold-Erz, Amethyst-Cluster | ×2,20 → ×2,67 → ×3,14 (+43 %) |
| `uniform_bonus_count`: +*L*/2 je Block, ungedeckelt | Redstone-Erz, Netherwarze, Süßbeerenstrauch, Knarzherz (Harz) | Redstone 6,0 → 7,0; Netherwarze 4,5 → 5,5; Süßbeeren 4,0 → 5,0 |
| `uniform_bonus_count`, gedeckelt | Glowstone (max. 4), Melone (max. 9), Seelaterne (max. 5), Gras und Farn (Samen) | Glowstone schon mit Vanilla-Fortune am Deckel |
| `binomial`: +4/7 je Stufe | Karotten, Kartoffeln, Weizen und Rote Bete (Erntegut über die Mod-Tables, siehe [Hof](#hof)), Samen | Karotte 4,43 → 5,57; Weizen 2,71 → 3,86 |
| `table_bonus`, gedeckelt bei III | Kies (Feuerstein), Nether-Ranken, Setzlinge aus Laub | keine Wirkung über III |
| `table_bonus` mit 5. Stufe | Laub: Stöcke, Äpfel (Eiche, Schwarzeiche) | Apfel 0,83 % → 2,5 % (×3) ab Fortune IV |

Die Erze sind nicht gedeckelt, gebremst wird über die Marktsättigung (Diamant `h` 20, übrige Erze `h` 100). Glück+ liegt damit auf der Höhe von Glück des Meeres+.

**Baumfällen und Vein Mining** wirken nur **beim Schleichen**: Baumfällen mit einer Axt an Stämmen (`#logs`), Vein Mining mit einer Spitzhacke an Erzen (`#c:ores`). Gesucht wird nur derselbe Block wie der abgebaute, über alle 26 Nachbarn, nächste zuerst: Ein Eichenstamm nimmt keine Birke mit, Erz keine Deepslate-Variante. Jeder Block läuft über den normalen Abbau des Spielers, Drops, Fortune-Bonus, Haltbarkeit und Schutz-Mods greifen also wie beim Einzelabbau. Kann das Werkzeug den Block nicht ernten, gibt es keine Kette. Zerbricht es, endet sie. Die Drops aller Blöcke der Kette, auch des selbst abgebauten Startblocks, landen direkt im Inventar; was nicht passt, fällt um den Spieler zu Boden statt an der Abbaustelle. Erfahrung erscheint am Spieler, Mending nimmt sie wie gewohnt auf. Hat Baumfällen mindestens einen weiteren Stamm gefällt, sammelt der Spieler danach 5 s lang in einem um 1 Block größeren Radius auf, damit er Setzlinge, Stöcke und Äpfel aus dem Laubzerfall leichter einsammelt (die Zerfallsgeschwindigkeit regelt auf dem Server die Mod „Faster Leaf Decay“). Umgesetzt ohne Attribut: `PickupBoost` berührt im Spieler-Tick die Items im erweiterten Bereich per `ItemEntity.playerTouch`, Aufsammelverzögerung und Pickup-Event greifen wie bei Vanilla. GameTest `chain_inventory`.

**Angelschatz** würfelt in **offenem Wasser** zusätzlich einen Fund aus `dtp:gameplay/fishing/structure_finds`: Töpferscherben, Rüstungsbesatz-Vorlagen, Herz des Meeres, Prüfungsschlüssel, verzaubertes Buch, Silbersack, bewusst keine Schallplatten. AFK-Farmen im 1×1-Loch haben davon nichts. **Doppelschwimmer** würfelt die Vanilla-Angeltabelle zweimal je Wurf, überall, auch im 1×1-Loch. Mit Angelschatz wird der Fund für jedes Item einzeln gewürfelt. Beides hängt als Loot-Modifier an der Vanilla-Angel-Loot-Table.

### Handel

| ID | Name | Stufen | Preise | Wirkung |
|---|---|---|---|---|
| `contract_capacity` | Mehr Contracts | 2 | 4.000 / 15.000 | +1 / +2 gleichzeitig annehmbare Contracts (Grundwert 2, `contracts.baseSlots`) |
| `quick_delivery` | Quick Delivery | 1 | 20.000 | Zeitbonus +30 % statt +20 % bei gleicher Bonusfrist, danach −20 % statt −10 % je Tag |

| `contract_clock` | Auftragsuhr | 1 | 3.000 | Tooltip der Vanilla-Uhr zeigt die angenommenen Contracts; an eine Marktwaage gebunden auch den Marktpreis ihres Items |
| `remote_delivery` | Fernlieferung | 1 | 10.000 | Schleichen + Rechtsklick mit der Uhr liefert aus dem Inventar an die eigenen Contracts, überall im Territorium; Abklingzeit 5 s (`params.cooldown`). Setzt die Auftragsuhr voraus |

Die Handel-Upgrades sind **Komfort, kein Wettbewerbsvorteil**: Sie geben keinen bevorzugten Zugriff auf den geteilten Aushang, beeinflussen keine Marktereignisse und automatisieren keine Verkäufe. Alles, was Funktionsblöcke verändert (etwa die Kistenreichweite), ist ein Server-Upgrade am Beacon, damit sich ein Block für alle gleich verhält, auch per Redstone. Auftragsuhr und Fernlieferung sind in [Contracts](12-contracts.md#auftragsuhr-und-fernlieferung) beschrieben.

Die Werte von Quick Delivery stehen in der Config (`contracts.quickDeliveryFactor` 1,3, `contracts.quickDeliveryDecayMultiplier` 2). Wie Frist und Verfall rechnen, steht in [Contracts](12-contracts.md).

### Hof

| ID | Name | Stufen | Preise | Wirkung je Stufe |
|---|---|---|---|---|
| `wool_bonus` | Mehr Wolle | 3 | 3.000 / 12.000 / 40.000 | +1 / +2 / +3 Wolle beim Scheren, in der Farbe des Schafs; alle Wolle direkt ins Inventar |
| `baby_growth` | Schnelles Wachstum | 3 | 3.000 / 12.000 / 40.000 | Füttern lässt Jungtiere zusätzlich 25 / 50 / 100 % ihrer Restzeit wachsen (Stufe 3: sofort erwachsen) |
| `hoe_harvest` | Flächenernte | 3 | 5.000 / 20.000 / 60.000 | Rechtsklick mit der Hacke auf eine reife Pflanze erntet 3×3 / 5×5 / 7×7 und pflanzt nach; Zuckerrohr und Kaktus über dem untersten Block; Ernte direkt ins Inventar |
| `harvest_bonus` | Reiche Ernte | 3 | 8.000 / 30.000 / 90.000 | 10 / 20 / 30 % Chance je reifer Pflanze auf doppelten Ertrag, ohne zusätzliches Saatgut |

Wolle und Jungtiere hängen an der Interaktion des Spielers, Dispenser profitieren nicht. Mit Mehr Wolle schert die Mod selbst (wie `ShearsItem`, über NeoForges `IShearable.onSheared`, 1 Haltbarkeit): Vanilla-Wolle und Zusatzwolle landen direkt im Inventar, was nicht passt, fällt um den Spieler. Vanilla lässt Jungtiere beim Füttern ohnehin um 10 % wachsen, der Bonus kommt dazu.

**Flächenernte** erntet alle reifen Pflanzen im Quadrat auf gleicher Höhe und pflanzt aus den Drops nach. Jede Pflanze kostet 1 Haltbarkeit (Unbreaking wirkt wie üblich), Fortune greift. Zerbricht die Hacke, endet die Ernte. Reif zählen Weizen, Karotten, Kartoffeln, Rote Bete (und andere `CropBlock`s), Netherwarze und Kakao (Stufe 2). Ein Klick auf Kakao erntet statt des waagrechten Quadrats eine senkrechte Wand an der Stammseite (3×3 / 5×5 / 7×7 aus Breite × Höhe), und zwar nur Kakao mit derselben Ausrichtung wie die angeklickte Schote; beim Nachpflanzen behält er seine Ausrichtung zum Stamm. Alle Drops der Flächenernte landen direkt im Inventar, damit sie weder verstreut auf dem Feld oder auf den nachgepflanzten Schoten liegen bleiben noch wegen der Aufsammelverzögerung zurückbleiben, wenn der Spieler weitergeht; was nicht passt, fällt um ihn zu Boden. **Zuckerrohr und Kaktus** erntet ein Klick als Säulen: im selben Quadrat je Spalte alles über dem untersten Block, beim Kaktus samt Blüte; die Basis bleibt stehen und wächst nach. Die Basis einer Spalte darf bis zu einen Block höher oder tiefer liegen als die der angeklickten, geerntet wird nur dieselbe Pflanze. Jede Säule kostet 1 Haltbarkeit; Reiche Ernte gilt hier nicht. **Reiche Ernte** gilt für die Flächenernte und für das normale Abbauen reifer Pflanzen mit der Hacke.

**Fortune auf Weizen und Rote Bete.** In Vanilla erhöht Fortune bei Weizen und Rote Bete nur die Samen. Die Mod gibt je reifer Pflanze zusätzlich `binomial(Fortune-Stufe, 4/7)` Stück Erntegut, im Schnitt +0,6 / +1,1 / +1,7 bei Fortune I / II / III; ohne Fortune bleibt es bei 1. Das entspricht dem Fortune-Anteil der Vanilla-Karottenformel, und die Basiswerte der Handelsliste rechnen damit. Es gilt mit jedem Werkzeug, beim normalen Abbauen und bei der Flächenernte; Fortune+ zählt mit, Reiche Ernte kopiert den erhöhten Ertrag. Umgesetzt ohne Code als Global Loot Modifier `neoforge:add_table` (`data/dtp/loot_modifiers/wheat_fortune.json`, `beetroots_fortune.json`) mit den Zusatz-Tables `data/dtp/loot_table/blocks/*_fortune.json`; die Vanilla-Tables bleiben unangetastet. GameTest `crop_fortune`.

---

## Abgrenzung zu den Beacon-Effekten

Hof-Upgrades und Beacon-Effekte ergänzen sich, ohne sich zu überschneiden: Die Werkstatt verbessert, was **der Spieler** tut (scheren, füttern, ernten), der Beacon verbessert, was **im Radius** von selbst passiert (Wachstum, Zuchtpause, Honig, siehe [Beacon-Effekte](21-beacon-effekte.md)). Beide erhöhen den Ertrag, nie den Preis.
