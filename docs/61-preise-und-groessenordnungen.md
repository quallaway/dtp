# Preise und Größenordnungen

> Das Preisgerüst für Handelsliste und Upgrade-Kosten: Warenklassen mit ihren Bändern, Sonderfälle, Ankerwaren, Contract-Einkommen und die Regeln, nach denen Upgrades bepreist sind.
> Die Einzelwerte stehen nur in den Daten: `data/dtp/dtp/trade_list/*.json`, `upgrade/*.json`, `player_upgrade/*.json`. Das Verfahren für neue Waren steht in [Handelsliste — Bewertungsverfahren](60-handelsliste-bewertung.md).

---

## Grundsätze

- **Zielwerte sind Ruhepreise** — das, was man beim ersten Verkauf sieht. Der Basiswert `base` in der Handelsliste ist Ruhepreis / 1,41.
- **Drei Warengruppen** bestimmen Preis und Sättigung:
  - **A — Landwirtschaft und Massenware:** niedriger Preis, sehr viele Verkäufe bis zur Sättigung, leicht zu erhalten.
  - **B — sammelbar und farmbar** (Monsterbeute, Fisch, Erze, Metalle): höherer Preis, sättigt schneller. Funde lohnen am Anfang, eine Farm ist aber keine Dauerlösung.
  - **C — Fundstücke, nicht oder kaum farmbar:** belohnend hoher Preis. Funde sind selten genug, dass die Sättigung kaum eine Rolle spielt; wer doch eine Serie verkauft, sättigt sofort.
- **Rohes Erz wird nicht gehandelt.** Metalle verkauft man als Barren — egal ob aus der Mine oder aus einer Farm.
- **Die Marktuhr läuft nur, während Spieler online sind.** Abwesenheit wird nicht nachgeholt: Die Sättigung trägt vollständig über Sitzungen hinweg und erholt sich nur durch gespielte Zeit. Erholungszeiten (`T`) sind in gespielten Markttagen zu lesen.
- **Craften erzeugt keinen Wert:** Was sich aus handelbaren Waren herstellen lässt, ist höchstens so viel wert wie die Zutaten.

## Rechengrundlagen

| Größe | Formel | Bedeutung |
|---|---|---|
| Ruhepreis `R` | `1,41 × B` | Preis einer nicht gesättigten Ware |
| Marktkapazität `C` | `≈ 2 × B × H` | Taler, die ein frischer Markt bis zur Preisuntergrenze hergibt ([Herleitung](60-handelsliste-bewertung.md#marktkapazität)) |
| Abverkauf bis zum halben Ruhepreis | `≈ C / 2` | |
| Dauereinkommen (laufend verkaufen) | höchstens `≈ 0,5 × B × H / T` je Markttag | erreicht bei einer Liefermenge von `H / T` pro Markttag |
| Spielerskalierung | `H × (1 + (Spieler − 1) × 0,8)` | bei 4 aktiven Spielern `H × 3,4` |

Ein Markttag sind 20 Minuten gespielte Zeit, eine Sitzung von 3,5 Stunden also gut 10 Markttage. Zwischen den Sitzungen erholt sich nichts. Daraus folgen die Halbwertszeiten:

- **`T` 1–2 (A1, A2):** erholt sich innerhalb der Sitzung, mehrfach.
- **`T` 4 (A3 bis B3, C2):** ein Abverkauf ist nach einer Sitzung weitgehend erholt.
- **`T` 10 (B4, C1):** bleibt über mehrere Sitzungen gedrückt — bei Diamanten und Scherben gewollt.

Die Taler-Stufen sind die Leiter der Größenordnungen (Faktor 9 je Stufe): Massenware rechnet in Kupfertalern, Monsterbeute und Metalle in Kupferbeuteln und -säcken, Fundstücke in Silbertalern, Einzelstücke in Silberbeuteln.

## Warenklassen

R = Ruhepreis in Talern, H = Halbwertsmenge (ein Spieler), T = Halbwertszeit der Erholung in Markttagen. Die Bänder beschreiben die aktuelle Handelsliste. Bei einer gemeinsamen Ware (`key`) gelten H und T für die ganze Ware, nicht je Item.

| Klasse | Waren (Beispiele) | R | H | T |
|---|---|---|---|---|
| **A1** Massenware | Bruchstein, Netherrack, Bambus, Seetang | ≈ 1 | 4.000 | 2 |
| **A2** Feldfrüchte und Holz | Weizen, Kartoffel, Karotte, Rote Bete, Zuckerrohr, Kaktus, Kakao, Süßbeeren, Kürbis, Melone, Stämme (`dtp:wood_log`) | 3–15 | 2.000 | 2 |
| **A3** Tierprodukte und Kleinfarm | Fleisch, Wolle (`dtp:wool`), Eier (`dtp:egg`), Federn, Honigwaben, Harz, Netherwarze, Nether-Pilze, Karmesin- und Wirrstiel, Chorus, Schlamm, Sculk, Tropfstein, Kupferbarren | 6–115 | 400 | 4 |
| **B1** einfache Monsterbeute und Fisch | Knochen, Faden, Verrottetes Fleisch, Spinnenauge, Schwarzpulver, Schleimball, Prismarin, Leder, Kabeljau, Lachs, Gürteltierschuppe, Pilze, Seegurke | 7–80 | 100 | 4 |
| **B2** Erze, Metalle und Handarbeit | Kohle, Eisen- und Goldbarren, Redstone, Lapis, Glowstone-Staub, Quarz, Obsidian, Amethyst, Ton, Korallenblöcke (`dtp:coral_block`), Froschlichter (`dtp:froglight`), Fackellilie, Kannenpflanze | 16–170 | 100 | 4 |
| **B3** Spezialbeute und seltene Fänge | Lohenrute, Böenrute, Enderperle, Ghast-Träne, Shulkerschale, Phantomhaut, Drachenatem, Hasenpfote, Schildkröten- und Schnüffler-Ei, Apfel, Giftige Kartoffel, Tintenbeutel, Kugel- und Tropenfisch | 45–900 | 40 | 4 |
| **B4** Wertrohstoffe | Diamant, Antiker Schrott (H 4) | 1.000–21.200 | 4–20 | 10 |
| **C1** häufige Fundstücke | Echo-Splitter, Sculk-Katalysator, Totem, Töpferscherben (`dtp:pottery_sherd`, H 5) | 700–1.500 | 5–10 | 10 |
| **C1K** Monsterköpfe | Creeper-, Zombie-, Skelett- und Piglinkopf, Wither-Skelett-Schädel | 5.000–7.000 | 2–3 | 10 |
| **C2** seltene Fundstücke | Rüstungsbesätze (`dtp:armor_trim`), Schallplatten, Bannervorlagen, Böses Banner, Namensschild, Nautilusschale, Herz des Meeres | 500–6.000 | 1 | 4 |
| **C3** Einzelstücke | Netherstern, Verzauberter Goldener Apfel, Drachenkopf | 25.000–40.000 | 1 | 4–10 |

### Sonderfälle

- **Schallplatten** bilden zwei gemeinsame Waren mit H 1. `dtp:farmable_music_disc`: die zwölf Creeper-Platten, dazu Tears und Lava Chicken, alle aus Mob-Farmen erreichbar, Ruhepreis 1.500. `dtp:structure_music_disc`: die Platten, die es nur in Strukturen gibt, Ruhepreis 3.000, Platte „5“ 6.000. Die zweite Platte derselben Ware bringt nur noch die Hälfte.
- **Rüstungsbesätze** und die Netherit-Vorlage lassen sich mit 7 Diamanten kopieren. Ihr Ruhepreis (1.500) liegt deshalb weit unter 7 Diamanten (7.000), und alle 19 Vorlagen teilen sich einen Markt mit H 1.
- **Barren:** Eisen (80) und Gold (120) kommen aus Bergbau und Farmen (Golems, Piglins) und sättigen deshalb schnell (H 100). Kupfer ist reichlich und kaum gefragt: Ruhepreis 15, H 400.
- **Dorfbewohner-Waren** sind wie Farmware bepreist, weil eine Handelshalle sie gratis liefert: Redstone, Lapis und Glowstone-Staub je 16 (H 100), Enderperle 200 (H 40), Apfel 100 (H 40). Smaragd steht nicht auf der Liste ([Dorfbewohnerhandel](60-handelsliste-bewertung.md#dorfbewohnerhandel)).
- **Wertvoll, aber farmbar:** Lohenrute, Enderperle, Shulkerschale, Böenrute und Schnüffler-Ei gehören zu B3, nicht zu C. Das **Totem** kommt aus Überfall-Farmen und steht bei den Mob-Drops, hat aber die Werte eines Fundstücks (H 10, T 10): Eine Raid-Farm sättigt es sofort.
- **Einzelstücke über Contract-Niveau:** Netherstern, Verzauberter Goldener Apfel, Drachenkopf und Antiker Schrott liegen im Ruhepreis mindestens beim Mittel eines Contracts der Stufe 3 (≈ 21.180 mit Bonus). Ein Fund soll eine echte Abwägung zwischen Verkaufen und Behalten sein. Apfel und Drachenkopf 25.000, Antiker Schrott 21.200 mit H 4: TNT- und Bett-Bergbau liefert ihn in Serie, die Kapazität bleibt bei ≈ 120.000. Der Drachenkopf hat H 1 und T 10, weil eine Elytra-Tour mehrere End-Schiffe findet: Der zweite Kopf bringt die Hälfte.
- **Netherstern** (40.000): drei Wither-Skelett-Schädel (3 × 7.000) plus Bosskampf. Den Kampf belohnt zusätzlich der Markt: Ein von einem Spieler besiegter Wither verkürzt die Wartezeit bis zum nächsten Marktereignis um `events.witherMinutes` (30), einmal je geplantem Ereignis ([Marktdruck](11-marktdruck.md#marktereignisse)). Der Schwere Kern ist nicht handelbar, er gehört zu einem Contract.
- **Monsterköpfe** stehen bei Loot. Creeper-, Zombie- und Skelettkopf gibt es nur über einen geladenen Creeper je Kopf: Gewitter, eigener Mob-Aufbau, riskant. Ruhepreis 5.000, H 2, T 10, jeder Kopf ein eigener Markt. Ein Regen-Katalysator (≈ 6.560 Taler) erzwingt 10 Minuten Gewitter und bringt mit gutem Aufbau 1–3 Köpfe: lohnend, aber kein sicherer Gewinn. Der Piglinkopf ist schwerer (6.500), weil der Piglin in der Oberwelt nach 15 Sekunden zombifiziert. Der Wither-Skelett-Schädel (7.000, H 3) fällt mit 2,5 % (5,5 % mit Looting III) und wird für den Wither gebraucht. Seine Obergrenze setzt der Schwarzmarkt (4 Silberbeutel ≥ 2 × base × 2,5).
- **Schrott** (`dtp:scrap`) kommt aus dem [Recycler](19-recycler.md): Ruhepreis 1, H 1.000, T 4. Ein Item bringt 5 % des Basiswerts seiner Rohstoffe, höchstens 40 Schrott. Die Kapazität (≈ 1.400 Taler je Spieler) begrenzt Dorfbewohner-Ausrüstung und Farmen.
- **Kürbis und Melone** werden in baugleichen Farmen angebaut, haben aber zwei Rollen. Der **Kürbis ist Maschinenware**: ein Stück je Ernte, kein Bonus durch Verzauberung, ein tiefer Markt (Ruhepreis 15, H 2.000, T 2), der Dauerlieferung verkraftet. Die **Melone ist Frischware**: 3–7 Scheiben je Ernte, mit Glück mehr. Ihr Markt ist flach, erholt sich aber schnell (Ruhepreis 3, H 3.000, T 1). Eine Vollautomatik flutet ihn, wer seltener und von Hand mit Glück erntet, holt den besten Preis je Ernte. Der Melonenblock steht nicht auf der Liste: Der Marktdruck zählt Stück, ein Block brächte 9 Scheiben Wert für 1 Stück Druck.

## Ankerwaren

Die Werte aus der Handelsliste, gegen die neue Waren eingeordnet werden. Kapazität `C ≈ 1,97 × B × H` für einen Spieler.

| Ware | Klasse | `base` | Ruhepreis | H | T | Kapazität `C` | Datei |
|---|---|---|---|---|---|---|---|
| Bruchstein | A1 | 0,71 | 1 | 4.000 | 2 | ≈ 5.600 | `30_resources` |
| Weizen | A2 | 6,25 | 8,8 | 2.000 | 2 | ≈ 24.600 | `10_agriculture` |
| Stamm (`dtp:wood_log`) | A2 | 7,84 | 11 | 2.000 | 2 | ≈ 30.900 | `30_resources` |
| Wolle (`dtp:wool`) | A3 | 14,1 | 20 | 400 | 4 | ≈ 11.100 | `10_agriculture` |
| Knochen | B1 | 24,7 | 35 | 100 | 4 | ≈ 4.900 | `40_mobs` |
| Eisenbarren | B2 | 56,6 | 80 | 100 | 4 | ≈ 11.100 | `30_resources` |
| Enderperle | B3 | 141 | 200 | 40 | 4 | ≈ 11.100 | `40_mobs` |
| Diamant | B4 | 707 | 1.000 | 20 | 10 | ≈ 27.800 | `30_resources` |
| Töpferscherbe (`dtp:pottery_sherd`) | C1 | 510 | 720 | 5 | 10 | ≈ 5.000 | `50_loot` |
| Rüstungsbesatz (`dtp:armor_trim`) | C2 | 1.061 | 1.500 | 1 | 4 | ≈ 2.100 | `50_loot` |
| Herz des Meeres | C2 | 3.122 | 4.400 | 1 | 4 | ≈ 6.100 | `50_loot` |
| Netherstern | C3 | 28.400 | 40.000 | 1 | 4 | ≈ 55.900 | `50_loot` |

## Contract-Einkommen

Die Belohnung eines Contracts der Stufen 1–3 ist Menge × Basiswert × Multiplikator der Stufe (`contracts.rewardMultiplier` 2,0 / 2,3 / 2,75), mit Zeitbonus × 1,2 (`contracts.bonusFactor`). Die Menge folgt aus dem Wertziel der Stufe: `contracts.tierValue` 800 / 2.800 / 7.000 Taler Marktwert zum Basiswert, ± 30 % (`contracts.tierValueSpread`). Stufe 4 verlangt verzauberte Ausrüstung mit festem Wert je Vorlage (`reward_per_item` plus Wert der gewürfelten Verzauberungen); das vierte Wertziel (30.000) greift nur bei Vorlagen ohne festen Wert, und solche gibt es in `contract/vanilla.json` für Stufe 4 nicht. Details: [Contracts](12-contracts.md).

| Contract-Stufe | Belohnung (ohne Bonus) | Mittel | Mittel mit Bonus | Bonusfrist | je Frist-Tag |
|---|---|---|---|---|---|
| 1 | 1.120 – 2.080 | ≈ 1.600 | ≈ 1.760 | 3 Tage | ≈ 590 |
| 2 | 4.510 – 8.370 | ≈ 6.440 | ≈ 7.080 | 6 Tage | ≈ 1.180 |
| 3 | 13.480 – 25.030 | ≈ 19.250 | ≈ 21.180 | 9 Tage | ≈ 2.350 |
| 4 | 2.000 – 230.000 je Vorlage, plus Verzauberungen | — | — | 12 Tage | — |

Jede Stufe zahlt etwa das 4- bzw. 3-Fache der darunter, je Frist-Tag das Doppelte. Ein Stufe-3-Contract ist damit ein besserer Ertrag je Slot, finanziert aber kein großes Upgrade allein: Contract-Stufe 3 kostet 40.000, ein Aushang-Platz 25.000 bis 120.000.

## Taler aus Strukturkisten

Kisten zahlen je nach Stufe im Schnitt 50 (Dorf) bis 3.500 Taler (End), höchstens einen Silberbeutel ([Markt](10-markt.md#taler-in-strukturkisten)). Ein Minenschacht mit rund 10 Kisten bringt etwa 2.000 Taler, gut 10 % einer Foraging-Sitzung. Eine Endstadt bringt 10.000–20.000, wenig gegen das Endspiel-Einkommen. Erkundung lohnt sich damit früh spürbar und bleibt später ein Nebenverdienst.

## Gruppeneinkommen nach Fortschrittskategorie

Die Upgrade-Kosten richten sich nach Fortschrittskategorien, geeicht am Spieltest mit 3–4 Spielern.

| Kategorie | Kennzeichen | Sitzungen | Gruppe je Sitzung | Gruppe kumuliert | je Spieler und Sitzung |
|---|---|---|---|---|---|
| **1 Foraging** | Man sammelt, was man findet; wenige Samen und Tiere; Angeln nur bei Regen. Eine Enderperle oder Nautilusschale ist ein Erfolg. | 1 | ≈ 20.000 | 20.000 | ≈ 5.000 |
| **2 Sesshaft** | Rüstung, mehrere Kisten, effektivere Minengänge, erste gefundene Verzauberungen, größeres Feld, erste Hopper, erstes persönliches Upgrade. Contracts der Stufe 1, Metalle, Phantomhaut. | 2–3 | ≈ 35.000 | 90.000 | ≈ 9.000 |
| **3 Der Ball rollt** | Mehrere spezialisierte Farmen, Marktdruck wird spürbar, Stufe 1 aus dem Lager erfüllbar, Nether offen. Contracts der Stufe 2. | 4–6 | ≈ 90.000 | 360.000 | ≈ 22.000 |
| **4 Ausbau** | Farmen werden ausgebaut, Nether-Erkundung bringt neue Waren. Contracts der Stufe 3, Nether-Farmen. | 7–10 | ≈ 180.000 | 1,1 Mio. | ≈ 45.000 |
| Endspiel | Stufe 4, alles parallel | 11+ | 250.000+ | — | — |

## Regeln für Upgrade-Preise

Nach diesen Regeln sind die Kosten in `upgrade/*.json` und `player_upgrade/*.json` gesetzt. Die einzelnen Preise stehen in [Territorium und serverweite Effekte](20-territorium-und-serverweite-effekte.md), [Beacon-Effekte](21-beacon-effekte.md), [Persönliche Upgrades](30-persoenliche-upgrades.md) und [Module und Fähigkeiten](31-module-und-faehigkeiten.md).

1. **Server:** Etwa 60 % des Einkommens einer Kategorie gehen in Server-Upgrades. Das **Torziel** einer Kategorie (Contract-Stufe, Nether-Zugang) kostet 1–1,5 Sitzungen Gruppeneinkommen, Nebenziele 0,1–0,4 Sitzungen.
2. **Contract-Stufe n kostet etwa einen durchschnittlichen Contract der Stufe n** — mit dem ersten Abschluss ist sie wieder eingespielt. Die Stufen kosten 1.000 / 12.000 / 40.000 / 100.000.
3. **Persönlich:** Etwa 40 % des Einkommens, geteilt durch die Spieler. Das erste persönliche Upgrade ist in Kategorie 2 nach etwa einer halben Sitzung erreichbar. Spieler spezialisieren sich und kaufen nicht alles.
4. **Mehr Angebot als Budget.** In Kategorie 3 und 4 gibt es mehr Angebote, als das Einkommen deckt — man muss wählen. Was über Kategorie 4 hinausgeht (Grenzstufe 11, Effektslot 3, Mending-Aura II, Double Bobbers, die oberen Markt-Upgrades), ist bewusst Endspiel-Ziel.
5. **Produktion teurer als Komfort.** Upgrades, die die Einnahmen erhöhen, steigen steiler von Stufe zu Stufe ([Balancing](62-balancing.md)). Innerhalb eines Bereichs ist der direkte Hebel auf wertvolle Ware teurer: Lure und Looting kosten weniger als Fortune und Luck of the Sea, weil Fortune wertvolle Erze vervielfacht und Luck die Schatzfänge hebt.

Einordnung der Torziele:

| Ziel | Kosten | Kategorie |
|---|---|---|
| Contract-Stufe 1 / 2 / 3 / 4 | 1.000 / 12.000 / 40.000 / 100.000 | 1 / 2 / 3 / 4 |
| Nether-Zugang | 60.000 | Übergang 2 → 3 |
| Effektslot 1 / 2 / 3 | 10.000 / 50.000 / 150.000 | 2 / 3 / Endspiel |
| Grenze Stufe 1–10 | 2.000 … 125.000, zusammen 400.000 | 1 bis 4 |
| Grenze Stufe 11 (aufheben) | 500.000 | Endspiel |

Die Summe der Grenzstufen 1–10 ist gut ein Drittel des Gruppeneinkommens bis zum Ende von Kategorie 4.

## Wiederkehrende Kosten: Chunkloader

Der [Chunkloader](20-territorium-und-serverweite-effekte.md#chunkloader) ist der einzige feste Laufpreis außerhalb des Schwarzmarkts: **2.500 Taler je 3 Minecraft-Tage**, also je 60 Minuten Spielzeit mit Spielern online. Eine Sitzung von 3–4 Stunden mit einem dauerhaft laufenden Chunkloader kostet damit 7.500–10.000 Taler.

| Kategorie | Anteil am Sitzungseinkommen der Gruppe |
|---|---|
| 3 Der Ball rollt (frühestens, das Rezept braucht Seelensand) | ≈ 10 % |
| 4 Ausbau | ≈ 5 % |
| Endspiel | ≤ 4 % |

## Wiederkehrende Kosten: Reparaturbank

Die [Reparaturbank](16-reparaturbank.md) repariert gegen Taler im Slot. Der Preis je Haltbarkeitspunkt ist `2 × (maxHaltbarkeit / 250) ^ 0,5`. Eine volle Reparatur wächst damit mit `maxHaltbarkeit ^ 1,5`.

| Gegenstand | Volle Reparatur | Einordnung |
|---|---|---|
| Eisenspitzhacke | 500 | ≈ 1,5 × Amboss mit 4 Eisenbarren (320 Ruhepreis), dafür ohne Erfahrung; Kategorie 2: rund 10–20 Minuten Einkommen eines Spielers |
| Eisenrüstung komplett | ≈ 1.510 | |
| Diamantrüstung komplett | ≈ 4.930 | |
| Diamantwerkzeug | ≈ 7.800 | Wertziel zweier Contracts der Stufe 2 (2 × 4.000); Kategorie 3: gut eine Stunde Einkommen eines Spielers |
| Netheritwerkzeug | ≈ 11.580 | Auf Dauer teurer als Mending, die Bank ist eine Lösung für die Zeit davor |

Das ist bewusst spürbar, aber kein Torziel: Ein Chunkloader lohnt sich für eine Farm, die während eines Nether-Ausflugs weiterlaufen soll, nicht als Dauerzustand an jeder Farm. Weil die Laufzeit nur mit Spielern online zählt und die Ware am Markt die normale Sättigung trifft, kann er sich nicht selbst finanzieren und keine Rückkopplung erzeugen ([Balancing](62-balancing.md)).
