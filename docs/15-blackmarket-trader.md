# Schwarzmarkthändler

> Ein fahrender Händler, den viel Geld in einer Einzahlkiste anlockt. Er verkauft gegen Silberbeutel, was man sonst nicht oder nur mit viel Glück bekommt: Schmuggelkisten, [Contraband](13-contraband.md) und seltene Ware.
> Er gehört zum [Schwarzmarkt](14-blackmarket.md).

---

## Rolle

Der Händler (`dtp:blackmarket_trader`) übernimmt KI und Oberfläche vom Vanilla-Händler, auch die Unsichtbarkeit in der Nacht, verkauft aber nur gegen **Silberbeutel** (6.561 Taler). Ein Spawn-Ei gibt es nicht.

Er ist die bewusste Ausnahme von „kein NPC“ ([Mod und Vision](01-mod-und-vision.md)): Er ist ein flüchtiges Ereignis, und mit ihm geht kein Zustand verloren.

**Warum ein Händler mit festem Angebot keine Arbitrage öffnet:** Er ist selten, sein Inventar ist begrenzt und fest, und jedes Angebot liegt weit über dem Marktpreis. Ein begrenztes Inventar lässt sich nicht zur Geldquelle skalieren.

---

## Spawnbedingung

Jede Minute sucht der Server in der Oberwelt den Spieler, dessen Einzahlkisten im Umkreis von 24 Blöcken am stärksten locken. Zuschauer zählen nicht.

1. **Freischaltung:** Erst wenn **eine einzelne** Kiste mehr als 60.000 Taler hält, kann er kommen. Ein Silbersack (59.049) reicht nicht, zwei Kisten mit je 40.000 auch nicht.
2. **Chance je Prüfung:** `min(maxChance, baseChance × (Taler in Reichweite / minChestBalance) ^ chanceExponent)`. Hier zählen alle Kisten in Reichweite.
3. **Auftritt:** Er erscheint an der Oberfläche bis 12 Blöcke um die reichste Kiste (findet sich dort kein Platz, bis 8 Blöcke um den Spieler), läuft zu ihr hin und bleibt in ihrer Nähe. Der Spieler bekommt eine Nachricht: „Ein zwielichtiger Händler hat dein Geld gewittert und ist in der Nähe aufgetaucht …“

Mit den Standardwerten:

| Taler in Reichweite | Chance je Minute | Im Mittel nach |
|---|---|---|
| knapp über 60.000 | 3 % | ca. 33 Min. |
| 240.000 | 6 % | ca. 17 Min. |
| 1,5 Mio. | 15 % | ca. 7 Min. |
| ab ca. 4,2 Mio. | 25 % (Obergrenze) | 4 Min. |

### Dauer und Pause

- Nach **20 Minuten** (ein Markttag) verschwindet er zu einem festen Spielzeitpunkt, auch wenn sein Chunk zwischendurch nicht geladen war. Sonst könnte ein vergessener Händler in einem entladenen Chunk ewig liegen bleiben. Läuft gerade ein Handel, wartet er bis zu dessen Ende.
- Danach folgen **120 Minuten** Pause.
- Serverweit gibt es immer nur einen Händler. Der Zeitpunkt für den nächsten steht in `BlackmarketData` (Feld `next_trader_tick`, siehe [Technischer Rahmen](70-technischer-rahmen.md)).
- Das Tageslimit des Schwarzmarkt-Blocks teilt er nicht.

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `blackmarket.traderEnabled` | true | Schalter |
| `blackmarket.checkIntervalSeconds` | 60 | Abstand der Prüfungen |
| `blackmarket.attractRadius` | 24 | Umkreis der Einzahlkisten um den Spieler |
| `blackmarket.minChestBalance` | 60.000 | Schwelle für eine einzelne Kiste |
| `blackmarket.baseChance` | 0,03 | Chance an der Schwelle |
| `blackmarket.chanceExponent` | 0,5 | Wie stark mehr Geld lockt (0 = gar nicht, 1 = linear) |
| `blackmarket.maxChance` | 0,25 | Obergrenze je Prüfung |
| `blackmarket.despawnMinutes` | 20 | Verweildauer |
| `blackmarket.cooldownMinutes` | 120 | Pause nach einem Händler |

### Admin-Befehle

| Befehl | Wirkung |
|---|---|
| `/dtp admin blackmarket status` | Größte Kiste und Taler in Reichweite um den Ausführenden, Chance je Prüfung, verbleibende Pause |
| `/dtp admin blackmarket spawn` | Händler sofort beim Spieler erscheinen lassen, unabhängig von Schwelle und Pause. Verweildauer und Pause starten wie bei einem normalen Auftritt |

---

## Angebot

Das Angebot ist reine Datenpack-Sache im Vanilla-Format von 26.3:
- `data/dtp/villager_trade/blackmarket/*.json`: die einzelnen Angebote,
- `data/dtp/tags/villager_trade/blackmarket/`: welche Angebote zu welchem Set gehören,
- `data/dtp/trade_set/blackmarket/`: drei Sets mit der Zahl der Angebote je Händler.

Bezahlt wird in `wants` mit `dtp:silver_pouch`. Über 64 Beutel ginge es mit `additional_wants`. Eine unbekannte Item-ID bricht den Serverstart ab.

Jeder Händler zieht aus jedem Set **2 Angebote**, zusammen also 6. Preise in Silberbeuteln:

### Set `crates`: Schmuggelkisten

| Angebot | Preis | Vorrat |
|---|---|---|
| Schmuggelkiste: Spawn-Ei | 3 | 3 |
| Schmuggelkiste: Setzling | 1 | 3 |
| Schmuggelkiste: Ei | 1 | 3 |
| Schmuggelkiste: Biom-Pflanze | 1 | 3 |
| Schmuggelkiste: Tier im Eimer | 2 | 3 |

### Set `contraband`: die 7 Contraband-Items

| Angebot | Preis | Vorrat |
|---|---|---|
| Feuerlöscher | 10 | 1 |
| Wachglocke | 8 | 1 |
| Schleimrute | 6 | 1 |
| Spawnstab | 8 | 1 |
| Taschengeld-Beutel | 10 | 1 |
| Gun | 16 | 1 |
| Endlose Fackel | 6 | 1 |

Werte und Wirkung der Items: [Contraband](13-contraband.md#die-contraband-items). Einzelne Contraband-Items und Verbrauchsgegenstände liegen auch in Strukturkisten ([Fundorte](13-contraband.md#fundorte-strukturkisten)). Der Händler bleibt der einzige verlässliche Weg zu einem bestimmten Item.

### Set `rare`: seltene Ware und Verbrauchsgegenstände

| Angebot | Menge | Preis | Vorrat |
|---|---|---|---|
| Schwerer Kern | 1 | 8 | 1 |
| Elytren | 1 | 32 | 1 |
| Herz des Meeres | 1 | 4 | 1 |
| Unheilvoller Prüfungsschlüssel | 1 | 4 | 1 |
| Witherskelettschädel | 1 | 4 | 1 |
| Amethyst-Katalysator | 4 | 3 | 2 |
| Regen-Katalysator | 2 | 2 | 2 |
| Rückkehrrolle | 4 | 2 | 2 |

Die Verbrauchsgegenstände sind in [Contraband](13-contraband.md#verbrauchsgegenstände-vom-händler) beschrieben.

### Keine Arbitrage

Jedes Angebot kostet mindestens das Doppelte dessen, was der Markt höchstens für dieselbe Ware zahlt (Basiswert × `market.factorMax`, siehe [Marktdruck](11-marktdruck.md)). Bei Kisten gilt das für jeden möglichen Inhalt. Der GameTest `blackmarket_no_arbitrage` prüft das, `blackmarket_offers` die Zusammensetzung der Sets.

Beispiel Herz des Meeres: Basiswert 3.122, Marktpreis höchstens 7.805, beim Händler 4 Beutel = 26.244 Taler.

---

## Schmuggelkisten

Eine Lootbox (`dtp:contraband_crate`). Was drin ist, zeigt sich erst beim Öffnen per Rechtsklick. Die Loot-Table steht in der Komponente `dtp:crate_loot`, den Namen setzt das Angebot über `minecraft:item_name`. Eine neue Sorte braucht also nur eine Loot-Table unter `data/dtp/loot_table/crate/` und ein Angebot, keinen Code.

**Regel:** Kisten schließen Lücken, sie kürzen keine Sammlungen ab. Mit Glück sichert man sich die letzte fehlende Holzart oder ein Schnüffler-Ei. Schallplatten, Töpferscherben und Vorlagen gibt es nicht, sonst würde Sammeln zu Pay-to-Complete.

| Kiste | Inhalt |
|---|---|
| Spawn-Ei | Alle Spawn-Eier außer Enderdrache, Wither, Warden und Großem Wächter, gleich gewichtet |
| Setzling | Alle Setzlinge, Mangroven-Keimling, Azalee, blühende Azalee, Karmesin- und Wirrpilz, gleich gewichtet |
| Ei | Hühnerei 35 %, blaues und braunes Ei je 17,5 %, Schildkrötenei 20 %, Schnüffler-Ei 9 %, Drachenei 1 % |
| Biom-Pflanze | 1–3 Stück: Kakaobohnen, Bambus, Kaktus, Süßbeeren, Leuchtbeeren, Zuckerrohr, Seetang, Seerosenblatt, Seegurke, Kürbis- und Melonenkerne. Ohne Fackellilien-Samen und Kannenpflanzen-Kapsel |
| Tier im Eimer | Axolotl (Rosa, Braun, Gold, Türkis je 13,3 %, Blau 1,3 %), Kaulquappe 16 %, Tropenfisch in einer der 22 häufigen Varianten (zusammen 29,3 %) |

---

## Technik

- **`BlackmarketTrader`:** Unterklasse des Vanilla-Händlers. Füllt sein Angebot aus den drei Sets `dtp:blackmarket/crates`, `dtp:blackmarket/contraband`, `dtp:blackmarket/rare` und speichert seinen Ablaufzeitpunkt (`dtp_expires_at`).
- **`BlackmarketSpawner`:** Lockwirkung, Chance und Auftritt.
- **`ContrabandCrateItem`:** die Schmuggelkiste.
- **GameTests:** `blackmarket_chance`, `blackmarket_lure`, `blackmarket_offers`, `blackmarket_no_arbitrage`, `contraband_crate_open` ([Entwicklung und Test](71-entwicklung-und-test.md)).
