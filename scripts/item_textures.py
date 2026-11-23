#!/usr/bin/env python3
"""Erzeugt die Item-Texturen von DTP (16x16, transparenter Hintergrund, pixelgenau).

Jedes Item ist ein handgesetztes 16x16-Raster. '.' ist transparent. Ziffern 1-5 sind die Tonstufen der
Hauptrampe des Items (1 = Glanzlicht oben links, 5 = Kontur), 6-9 und 0 die der Zweitrampe. Alle anderen
Zeichen stehen in PALETTE. Licht kommt wie bei den Blöcken von oben links; jedes Item hat eine dunkle
Kontur in seiner eigenen Materialfarbe, wie die Vanilla-Items.

    python3 scripts/item_textures.py                     # schreibt nach src/main/resources/assets/dtp/textures/item
    python3 scripts/item_textures.py --preview X.png     # Vorschau: Originalgröße in Inventarslots und vergrößert
"""
import argparse
import os

from png_writer import write_png

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/dtp/textures/item")
SLOT_ICON = os.path.join(ROOT, "src/main/resources/assets/dtp/textures/gui/sprites/container/slot/silver_pouch.png")
TALER_SLOT_ICON = os.path.join(ROOT, "src/main/resources/assets/dtp/textures/gui/sprites/container/slot/taler.png")

# --- Rampen: Glanz, hell, mittel, dunkel, Kontur ------------------------------------------------------
COPPER = ["#F2A57E", "#DE7C52", "#BC5B39", "#8E4027", "#5C2817"]
SILVER = ["#FFFFFF", "#DADCE0", "#B2B5BB", "#868A91", "#55585E"]
GOLD = ["#FFF4A0", "#FFDF37", "#E5B82D", "#A98721", "#6E5513"]
LEATHER = ["#B07A45", "#8F5E33", "#734828", "#56341C", "#3A2210"]      # Kupferbeutel, Taschengeld
CLOTH_SILVER = ["#A3ACB8", "#808A97", "#646D7A", "#4A515D", "#2F343D"]  # kühles Graublau
CLOTH_GOLD = ["#B5483B", "#97342B", "#7A2821", "#5C1D18", "#3D120F"]    # gedämpftes Dunkelrot
SPRUCE = ["#86643A", "#715230", "#5D4227", "#48321C", "#2A1C0E"]
OAK = ["#C29D62", "#A88452", "#8A6A3E", "#6B512E", "#3F2F18"]
DARK_OAK = ["#6B4A2A", "#553A20", "#432D18", "#311F10", "#1E1308"]
RED = ["#DE6658", "#C2453A", "#A13429", "#7A2420", "#4A1612"]
BRONZE = ["#F0CF78", "#D2A54E", "#AE8236", "#7E5C22", "#4E3914"]
SLIME = ["#A8DE92", "#7EBF6E", "#5A9E4B", "#3F7334", "#28471F"]
AMETHYST = ["#E3C2FA", "#C79CF0", "#A274DA", "#7650B0", "#4A2F78"]
STORM = ["#9AA3BB", "#6F7891", "#535A73", "#3C4058", "#23263A"]
PARCHMENT = ["#FBF3D8", "#EFE3BC", "#DACA9B", "#B49E70", "#7A6648"]
IRON = ["#F2F2F2", "#C8C8C8", "#9A9A9A", "#6A6A6A", "#3C3C3C"]

PALETTE = {
    ".": None,
    # Eisen
    "I": "#E6E6E6", "i": "#BDBDBD", "j": "#8C8C8C", "J": "#5A5A5A", "K": "#2E2E2E", "k": "#1C1C1C",
    # Gold (Taler-Töne)
    "H": "#FFF4A0", "h": "#FFDF37", "g": "#E5B82D", "G": "#A98721", "d": "#6E5513",
    # Schnur / Kordel
    "w": "#E6D7A6", "W": "#BFA977", "v": "#8C7647",
    # Kreide/Pergament auf dunklem Etikett
    "c": "#F1EBDA", "C": "#CFC6B0",
    # Etikett (Kohle, wie der Schwarzmarkt)
    "x": "#3D3737", "X": "#2A2525",
    # Ei
    "e": "#F3EAD2", "E": "#D8C9A4",
    # Spawn-Ei: Grund und Flecken
    "s": "#6FA544", "S": "#4E7A2E", "z": "#24301A",
    # Grün (Setzling, Weizenhalm, Smaragd)
    "f": "#7DB542", "F": "#4F7F25", "t": "#3A5C1C",
    # Weizen, Kakao
    "y": "#E8C766", "Y": "#C49A3A", "u": "#7A4A22", "U": "#5A3418",
    # Fisch
    "o": "#E8893A", "O": "#B5601F",
    # Smaragd
    "m": "#7BE39A", "M": "#3FB463", "n": "#1F7A3D",
    # Beacon-Kristall (cyan)
    "b": "#B6F4ED", "B": "#4FC4BA", "a": "#21706A",
    # Flamme
    "q": "#FFF6C8", "Q": "#FFD24A", "r": "#F08A24", "R": "#B8501A",
    # Blitz, Regen
    "l": "#F1EBA6", "L": "#A9C3E0",
    # Rot (Akzente, Nadel, Pfad)
    "p": "#C8402F", "P": "#7E231A",
    # dunkler Kristall (Spawn-Stab)
    "V": "#5B3F7A", "Z": "#2E1F45", "N": "#8A6AB0",
    # Messskala
    "A": "#F2EBD2", "T": "#4D3F2F",
}


def hexrgb(h):
    return (int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), 255)


def paint(rows, ramp=OAK, ramp2=GOLD, extra=None):
    assert len(rows) == 16, len(rows)
    out = []
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        line = []
        for ch in row:
            if extra and ch in extra:
                col = extra[ch]
            elif ch in "12345":
                col = ramp[int(ch) - 1]
            elif ch in "67890":
                col = ramp2["67890".index(ch)]
            else:
                col = PALETTE[ch]
            line.append((0, 0, 0, 0) if col is None else hexrgb(col))
        out.append(line)
    return out


# =====================================================================================================
# Währung — Münze, Beutel, Sack. Material der Münzen = Stufe; Farbe des Beutels/Sacks = Stufe.
# =====================================================================================================
COIN = [
    "................",
    "................",
    ".....555555.....",
    "....52222235....",
    "...5212223335...",
    "..522444444335..",
    "..522224433335..",
    "..522234433335..",
    "..522334433335..",
    "..523334433335..",
    "..523334433345..",
    "...5333333345...",
    "....53333445....",
    ".....555555.....",
    "................",
    "................",
]
# Beutel: Leder bzw. Tuch (Rampe 1-5), Münzen der Stufe schauen oben heraus (Rampe 6-0).
POUCH = [
    "................",
    "................",
    "......000.......",
    ".....06670.00...",
    ".....067780780..",
    "....5506785785..",
    "....512222225...",
    ".....5wWWwv5....",
    "....51222225....",
    "...5122222345...",
    "..512222233345..",
    "..512223333345..",
    "..522233333445..",
    "...5233334445...",
    "....55444455....",
    "......5555......",
]
# Sack: größer, schwerer, mit Falten und fest verschnürtem Hals, aufgenähte Münze der Stufe.
SACK = [
    "................",
    "......5..5......",
    ".....515515.....",
    ".....5122215....",
    "......5wWv5.....",
    ".....5wWWWv5....",
    "....5112222235..",
    "...511222222335.",
    "..51122222223335",
    "..51222200222335",
    ".512222067022335",
    ".522223078033345",
    ".522233003334445",
    ".523333333344445",
    "..5333333444445.",
    "...5555555555...",
]


# =====================================================================================================
# Schmuggelkisten — dieselbe Fichtenkiste mit Eisenecken; auf dem Kohle-Etikett das Sortensymbol und ein
# Fragezeichen für "zufälliger Inhalt".
# =====================================================================================================
CRATE = [
    "................",
    ".55555555555555.",
    ".5j1121112111j5.",
    ".5J2232223222J5.",
    ".55555555555555.",
    ".5i2212221222j5.",
    ".5XXXXXXX22212j5",
    ".5X#####X212225.",
    ".5X#####X222235.",
    ".5X#####X444445.",
    ".5X#####X221225.",
    ".5X#####X222235.",
    ".5XXXXXXX22232j5",
    ".5i3334333433J5.",
    ".55555555555555.",
    "................",
]


def crate(symbol):
    """Etikett links (Sortensymbol 5x5), rechts daneben ein mit Kreide aufs Holz gemaltes Fragezeichen."""
    rows = [r[:16] if len(r) == 16 else (r[:15] + "5") for r in CRATE]
    rows = [r[:14] + "5." if i in (6, 12) else r for i, r in enumerate(rows)]
    qmark = ["cc.", "..c", ".c.", "...", ".c."]
    grid = [list(r) for r in rows]
    for dy in range(5):
        for dx in range(5):
            ch = symbol[dy][dx]
            grid[7 + dy][3 + dx] = "x" if ch == "." else ch
        for dx, ch in enumerate(qmark[dy]):
            if ch != ".":
                grid[7 + dy][10 + dx] = ch
    return ["".join(r) for r in grid]


SYM_EGG = [
    "..ee.",
    ".eeeE",
    ".eeeE",
    ".eeEE",
    "..EE.",
]
SYM_SPAWN_EGG = [   # Vanilla-Spawn-Ei: Grundfarbe mit Flecken, Flecken nur im Inneren
    "..ss.",
    ".szsS",
    ".sssS",
    ".szSS",
    "..SS.",
]
SYM_SAPLING = [
    ".f.f.",
    "fFfFf",
    ".FfF.",
    "..u..",
    ".uUu.",
]
SYM_FARM = [         # Weizengarbe mit Kordel
    "y.y.y",
    "yYyYy",
    ".YYY.",
    ".WwW.",
    "Y.Y.Y",
]
SYM_BUCKET = [       # Eimer mit Fischschwanz
    "..oO.",
    "IiOij",
    "ijjjJ",
    "ijjjJ",
    ".JJJ.",
]
SYM_UNKNOWN = [      # Rückfall für künftige Kistensorten ohne eigenes Symbol
    ".ccc.",
    "....c",
    "..cc.",
    ".....",
    "..c..",
]

# =====================================================================================================
# Contraband und Spezialitems
# =====================================================================================================
FIRE_EXTINGUISHER = [   # Rampe: Rot
    "....kkkkk.......",
    "...k.....k......",
    ".....KJjJ.......",
    "...JjAJiJkk.....",
    "...JpAJjJ..k....",
    "....5JJJ5...k...",
    "...5122235..k...",
    "...5122335..k...",
    "...5I22335..k...",
    "...5iIi335..k...",
    "...5123335..k...",
    "...5123345.kk...",
    "...5123345.Jk...",
    "...5223345.J....",
    "...5233445......",
    "....55555.......",
]
ENDLESS_TORCH = [       # Rampe 1-5: Eiche (Stiel); diagonal wie ein Werkzeug
    "................",
    "...........q....",
    "..........qQq...",
    ".........qQrQ...",
    "........qQrRQ...",
    "........QrRR....",
    ".......4QRR.....",
    "......4gBg4.....",
    ".....4gbaG4.....",
    "......4GG4......",
    ".....423........",
    "....423.........",
    "...423..........",
    "..423...........",
    ".423............",
    ".44.............",
]
OOZING_ROD = [          # Rampe 1-5: Fichte (Gabelrute), 6-0: Schleim
    "...........4....",
    "..........424...",
    "...4......424...",
    "..424....424....",
    "...424...424....",
    "....424.424.....",
    ".....4w6w4......",
    ".....w6770......",
    "......7890......",
    ".....4300.......",
    "....423.........",
    "...423..........",
    "..423...........",
    ".423............",
    ".43.............",
    "................",
]
SPAWN_WAND = [          # Rampe 1-5: Fichte (Griff), Kopf: Messinstrument mit Skala und dunklem Kristall
    "............NZ..",
    "...........NVZ..",
    "..........VVZ...",
    ".......JJJJZ....",
    "......JAAApJ....",
    ".....JAATpAJ....",
    ".....JATpAAJ....",
    ".....JAjAAAJ....",
    ".....JJjjJJ.....",
    "......Jij.......",
    ".....423........",
    "....423.........",
    "...423..........",
    "..423...........",
    ".423............",
    ".44.............",
]
POCKET_MONEY = [        # Rampe 1-5: Leder, 6-0: Gold; Smaragd vorn
    "................",
    "................",
    "......0660......",
    ".....067780.....",
    "....506678805...",
    "....51122225....",
    ".....5wWWv5.....",
    "....51222225....",
    "...5122222345...",
    "..51222mM33345..",
    "..5122mmMn3345..",
    "..5222MMnn3445..",
    "..52222nn33445..",
    "...5233334445...",
    "....55444455....",
    "......5555......",
]
GUN = [                 # Rampe 1-5: Dunkeleiche (Schaft), Eisenlauf, Messingbeschläge
    "................",
    "................",
    "......K.........",
    ".....KjK........",
    "....kJiJkkkkkkkk",
    "...5kIIiiiiiiiij",
    "..512jjjjjjjjjJk",
    "..5122233333345k",
    ".5122335kk55555.",
    ".512235.G.G.....",
    "51223335GG......",
    "5122345.........",
    "5222345.........",
    "52GG45..........",
    ".5555...........",
    "................",
]
WATCH_BELL = [          # Rampe 1-5: Bronze, Griff Dunkeleiche, rote Schlaufe
    "......pP........",
    ".....p..P.......",
    "......u0........",
    "......U0........",
    "......u0........",
    ".....5115.......",
    "....512235......",
    "....522335......",
    "....523345......",
    "...5GGGGGG5.....",
    "...5223345......",
    "..52223334.5....",
    "..5222333445....",
    ".5GGGGGGGGGG5...",
    ".55555JJ55555...",
    "......JJ........",
]
STONE = [               # gemeinsame, facettierte Steinform der Katalysatoren
    "................",
    ".......55.......",
    "......5125......",
    ".....511225.....",
    "....51112235....",
    "...5111222335...",
    "..511112223335..",
    "..511122223335..",
    "..511222233345..",
    "..512222233445..",
    "...5222233445...",
    "...5222334445...",
    "....52334445....",
    ".....533445.....",
    "......5445......",
    ".......55.......",
]


def overlay(base, top):
    return ["".join(t if t != "." else b for b, t in zip(br, tr)) for br, tr in zip(base, top)]


AMETHYST_INCLUSION = [  # Kristallbüschel im Stein (Amethyst-Spitzen), heller Kern
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......1........",
    "......1.1.......",
    "....1..1..1.....",
    ".....1111.......",
    "......11........",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
]
RAIN_INCLUSION = [      # Gewitterwolke mit Blitz und Regen
    "................",
    "................",
    "................",
    "................",
    "......11........",
    ".....1111.1.....",
    "....11111111....",
    ".....l.L.L......",
    "......l.........",
    ".....ll.L.L.....",
    "......l.........",
    ".....l..........",
    "................",
    "................",
    "................",
    "................",
]
RETURN_SCROLL = [       # Rampe 1-5: Pergament, Endkappen Gold, Karte mit Pfad und Beacon
    "................",
    ".gG5555555555Gg.",
    "dhg1121111121ghd",
    ".dG5444444445Gd.",
    "...512222222345.",
    "...5122ppp22b45.",
    "...512p222bBa45.",
    "...512p2222a345.",
    "...5122p2222345.",
    "...51222pp22345.",
    "...512222p22345.",
    "...5222GG222345.",
    ".gG5555555555Gg.",
    "dhg1121111121ghd",
    ".dG5444444445Gd.",
    "................",
]


SCRAP = [               # Rampe 1-5: Eisen (verbogenes Blech, Zahnrad), 6-0: Leder (Riemenrest); ein Haufen
    "................",
    "................",
    "................",
    ".......555......",
    "......52325.....",
    ".....5232325....",
    "....55234325....",
    "...5122345555...",
    "..51223455125...",
    "..5122345123355.",
    ".560000055223345",
    ".57667890523345.",
    "5677889905345...",
    "50000000055.....",
    "................",
    "................",
]


def build():
    T = {}
    for tier, metal, bag in (("copper", COPPER, LEATHER), ("silver", SILVER, CLOTH_SILVER), ("gold", GOLD, CLOTH_GOLD)):
        T[f"{tier}_taler"] = paint(COIN, metal)
        T[f"{tier}_pouch"] = paint(POUCH, bag, metal)
        T[f"{tier}_sack"] = paint(SACK, bag, metal)
    for name, sym in (("egg", SYM_EGG), ("spawn_egg", SYM_SPAWN_EGG), ("sapling", SYM_SAPLING),
                      ("biome_plant", SYM_FARM), ("bucket_animal", SYM_BUCKET)):
        T[f"contraband_crate_{name}"] = paint(crate(sym), SPRUCE)
    T["contraband_crate"] = paint(crate(SYM_UNKNOWN), SPRUCE)
    T["fire_extinguisher"] = paint(FIRE_EXTINGUISHER, RED)
    T["endless_torch"] = paint(ENDLESS_TORCH, OAK)
    T["oozing_rod"] = paint(OOZING_ROD, SPRUCE, SLIME)
    T["spawn_wand"] = paint(SPAWN_WAND, SPRUCE)
    T["pocket_money_pouch"] = paint(POCKET_MONEY, LEATHER, GOLD)
    T["gun"] = paint(GUN, DARK_OAK)
    T["watch_bell"] = paint(WATCH_BELL, BRONZE, DARK_OAK)
    T["amethyst_catalyst"] = paint(overlay(STONE, AMETHYST_INCLUSION), AMETHYST)
    T["rain_catalyst"] = paint(overlay(STONE, RAIN_INCLUSION), STORM)
    T["return_scroll"] = paint(RETURN_SCROLL, PARCHMENT)
    T["scrap"] = paint(SCRAP, IRON, LEATHER)
    return T


def slot_icon(rows=None, dark="50"):
    """Leerer Bezahl-Slot, flach grau wie Vanilla-Slot-Symbole. Standard: Silhouette des Silberbeutels (Schwarzmarkt);
    mit COIN und dark="54" die Münze mit „T“ (Reparaturbank, nimmt jede Stückelung)."""
    out = []
    for row in rows or POUCH:
        line = []
        for ch in row:
            if ch == ".":
                line.append((0, 0, 0, 0))
            elif ch in dark:
                line.append((150, 150, 150, 200))
            else:
                line.append((178, 178, 178, 200))
        out.append(line)
    return out


def preview(T, path, k=8):
    """Oben jede Textur in Originalgröße (x2) auf einem Inventarslot, darunter vergrößert."""
    names = list(T)
    cols = 9
    rows = (len(names) + cols - 1) // cols
    cell = 16 * k + 12
    small = 16 * 2 + 8
    W = cols * cell + 12
    H = rows * (cell + small) + 12
    bg = (198, 198, 198, 255)
    img = [[bg] * W for _ in range(H)]
    for i, name in enumerate(names):
        cx, cy = 12 + (i % cols) * cell, 12 + (i // cols) * (cell + small)
        for y in range(16 * 2 + 4):       # Slot
            for x in range(16 * 2 + 4):
                img[cy + y][cx + x] = (139, 139, 139, 255)
        tex = T[name]
        for y in range(32):
            for x in range(32):
                p = tex[y // 2][x // 2]
                if p[3]:
                    img[cy + 2 + y][cx + 2 + x] = p
        for y in range(16 * k):
            for x in range(16 * k):
                p = tex[y // k][x // k]
                chk = 120 if ((x // k) + (y // k)) % 2 else 105
                img[cy + small + y][cx + x] = p if p[3] else (chk, chk, chk, 255)
    write_png(path, img)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    ap.add_argument("--out", default=OUT)
    args = ap.parse_args()
    T = build()
    os.makedirs(args.out, exist_ok=True)
    for name, px in T.items():
        write_png(os.path.join(args.out, name + ".png"), px)
    print(f"{len(T)} Texturen nach {args.out}")
    if args.out == OUT:
        write_png(SLOT_ICON, slot_icon())
        print("Slot-Symbol:", SLOT_ICON)
        write_png(TALER_SLOT_ICON, slot_icon(COIN, "54"))
        print("Slot-Symbol:", TALER_SLOT_ICON)
    if args.preview:
        preview(T, args.preview)
        print("Vorschau:", args.preview)


if __name__ == "__main__":
    main()
