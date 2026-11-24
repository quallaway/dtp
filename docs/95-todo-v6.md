# Todo v6

**Legende:**

- 🧭 Entscheidung oder Konzeptarbeit
- 🔧 Umsetzung
- 🧪 Test im Spiel

## 1. Kistenloren als Warenlager

Ware soll per Kistenlore zum Markt fahren. Eine vanilla Kistenlore auf einem **Marktgleis** in Trade-Chest-Reichweite zählt wie eine Trade-Chest. Loren auf anderen Schienen zählen nie — eine geparkte Lore wird nicht versehentlich verkauft.

Kein Preis- oder Lohnvorteil, kein Malus für dezentrale Marktstände — beide Wege bleiben wirtschaftlich gleichwertig. Der Mehrwert ist Verwaltung: ein Entscheidungsort für die Ware aller Farmen und ein Pendelbetrieb Farm → Markt → Farm, bei dem nur das Verkaufen eine Entscheidung bleibt.

- [ ] 🔧 Gemeinsame Lagersuche `Sales.storages()`: Trade-Chests und Kistenloren auf Marktgleis, nächste zuerst. Ersetzt `tradeChests()`; Lagerliste, „alles verkaufen“, Redstone-Verkauf und Contract-Lieferung lesen damit auch Loren.
- [ ] 🔧 Marktgleis `dtp:market_rail` als Unterklasse der Antriebsschiene: unbestromt bremst es die Lore bis zum Stillstand, bestromt schickt es sie weiter (Bahnhof: einfahren, verkaufen, losfahren). In `#minecraft:rails` eintragen, sonst fährt keine Lore darauf.
- [ ] 🔧 Leere Lore fährt ab: Wird eine Lore auf dem Marktgleis durch Verkauf oder Contract-Lieferung leer, bestromt sich das Gleis kurz selbst und schickt sie los (Prellbock nötig, wie bei vanilla Antriebsschienen). Bleibt Unverkäufliches liegen, bleibt die Lore stehen.
- [ ] 🔧 Rezept: Eisenbarren, Trade-Chest und Redstone ergeben 6 Marktgleise. Blockstate, Modelle, Platzhaltertextur, Loot-Table, Lang, Creative-Tab, Guide.
- [ ] 🔧 Marktstand und Bulletin-Board zeigen „Lager aus X Trade-Chests und Y Loren“; Warnung „kein Lager“ nur, wenn beides 0 ist.
- [ ] 🔧 Item-Capability (`Capabilities.Item.BLOCK`) für Trade-Chest und Einzahlkiste registrieren, damit Rohre anderer Mods (Create, Mekanism, Pipez …) sie beliefern. Die Einzahlkiste nimmt weiter nur Taler an.
- [ ] 🔧 GameTests: Lore auf Marktgleis wird verkauft; Lore auf normaler Schiene und außerhalb der Reichweite zählt nicht; Redstone-Verkauf; Contract aus Lore; Capability; Marktgleis bremst; leere Lore fährt ab, Lore mit Rest bleibt stehen.
- [ ] 🔧 Doku: `10-markt.md` (Abschnitt Marktgleis mit Einordnung: kein Preisvorteil, Mehrwert ist Verwaltung), `12-contracts.md`, `70-technischer-rahmen.md`, `90-glossar.md`.
- [ ] 🧪 Bahnhof im Spiel bauen: Lore fährt ein und hält, „Alles verkaufen“, Hebel ans Gleis, Lore fährt los.
- [ ] 🧪 Pendelbetrieb: Farm-Hopper belädt die Lore, Detektorgleis und Comparator schicken sie voll los, nach „Alles verkaufen“ fährt sie leer zurück.
- [ ] 🧪 Optional: Trade-Chest per Create-Trichter oder Pipez beliefern.


# Ideen

## Tooltips und Texte durchgehen.

## Better Advancement durcharbeiten.

## JEI compatibility

- recipe for recycler

## Death Counter / Contract Counter

## Stockmarket

- Items mit wandernden Preisdruck die gekauft und verkauft werden können.
- Haben eine Fee beim verkaufen.

## SCHNAPPT DEN DIEB

## Pet Upgrades

## Monumentalprojekt

## BUGS

- Market Statistics are all counted towards Contracts