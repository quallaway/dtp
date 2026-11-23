#!/usr/bin/env python3
"""Erzeugt die Blocktexturen von DTP (16x16, pixelgenau, ohne externe Abhängigkeiten).

Jede Fläche ist ein handgesetztes 16x16-Raster. Ziffern 1-5 sind die fünf Tonstufen des Hauptholzes
(1 = Licht oben links, 5 = tiefster Schatten), Ziffern 6-0 die des Zweitholzes. '.' übernimmt das
Pixel der Grundschicht (Planken-Vorlage im Hauptholz). Alle übrigen Zeichen stehen in PALETTE —
eine gemeinsame Palette für alle Blöcke, damit Material- und Lichtwirkung überall gleich bleiben.

    python3 scripts/block_textures.py               # schreibt nach src/main/resources/assets/dtp/textures/block
    python3 scripts/block_textures.py --preview X.png   # zusätzlich eine isometrische Vorschau
"""
import argparse
import math
import os

from png_writer import write_png

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/dtp/textures/block")

# --- Holz: Tonstufen hell -> dunkel, an die Vanilla-Planken angelehnt ---------------------------------
OAK = ["#C29D62", "#AA8550", "#977443", "#7A5D34", "#5A4423"]
SPRUCE = ["#86643A", "#715230", "#5D4227", "#48321C", "#33230F"]
DARK_OAK = ["#5B3E22", "#4B331B", "#3D2914", "#2E1E0D", "#1F1408"]
CHEST = ["#C48C4A", "#A87238", "#8C5C2A", "#6B431C", "#4A2D11"]

PALETTE = {
    # Schatten im Inneren von Ständen und Regalen
    "#": "#1F170D", "~": "#2E2315",
    # Eisen
    "I": "#E6E6E6", "i": "#BDBDBD", "j": "#8C8C8C", "J": "#5A5A5A",
    # Gold (Taler-Töne aus textures/item/gold_taler.png)
    "H": "#FFF4B0", "h": "#FFDF37", "g": "#E5B82D", "G": "#A98721", "d": "#96781D", "e": "#6B5413",
    # Kupfer
    "+": "#D9774E", "-": "#9E4E2E",
    # Pergament und Tinte
    "p": "#F0E6C6", "q": "#DACA9B", "Q": "#B49E70", "n": "#7A6650", "N": "#4D3F2F",
    # Rotes Tuch, Siegelwachs
    "r": "#B13B2E", "R": "#8C2C23", "z": "#621C17",
    # Helles Tuch, Schnur
    "w": "#ECE4CE", "W": "#CDC2A4",
    # Schwarzmarkt-Tuch: gedecktes Dunkelrot und Kohle
    "m": "#5E2B26", "M": "#45201C", "x": "#3D3737", "X": "#2A2525",
    # Tiefenschiefer
    "s": "#73737A", "t": "#5A5A60", "u": "#444449", "v": "#2E2E33",
    # Obsidian
    "o": "#3E2C59", "O": "#281D3B", "y": "#150F20",
    # Enderperle (Töne aus Vanillas textures/item/ender_pearl.png)
    "!": "#8CF4E2", "*": "#2CCDB1", "=": "#258474", "<": "#105E51", ">": "#063931",
    # Beacon-Kristall
    "c": "#F4FFFD", "C": "#B6F4ED", "b": "#6CD8CE", "B": "#37A39A", "a": "#21706A",
    # Glas (blickdicht gemalt, dunkles Inneres mit hellen Schlieren)
    "l": "#D7ECEF", "L": "#A3C6CB", "%": "#5E878C", "&": "#4A6E72",
    # echtes Glas mit Alpha: Tönung, Kante, Schliere; ' ' ist voll transparent
    ",": "#CDECEE30", ";": "#DCF2F55A", ":": "#F2FBFC9A", " ": "#00000000",
    # Waren
    "A": "#C7392B", "K": "#8E241B", "Y": "#E5C06A", "T": "#BC8A3A", "U": "#8A5D22",
    "E": "#5BC777", "Z": "#2E8A4A", "f": "#6E9A2C", "F": "#47691C", "k": "#E3892F",
    "P": "#8D52A8", "S": "#5A2F78",
}

# --- Planken-Vorlage (Tonstufen), vier Bretter mit versetzten Stößen -----------------------------------
PLANKS = [
    "2122222212222122",
    "2222322222232222",
    "3222222322222223",
    "4444444444444444",
    "2221224222122222",
    "2322224222222322",
    "2222234223222232",
    "4444444444444444",
    "2212222222242122",
    "3222322222242222",
    "2222222322342223",
    "4444444444444444",
    "1222242221222212",
    "2232242222232222",
    "2222243222222232",
    "4444444444444444",
]
VPLANKS = ["".join(PLANKS[x][y] for x in range(16)) for y in range(16)]


def hexrgb(h):
    alpha = int(h[7:9], 16) if len(h) == 9 else 255
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5)) + (alpha,)


def paint(rows, wood, wood2=SPRUCE, base=PLANKS):
    assert len(rows) == 16, len(rows)
    out = []
    for y, row in enumerate(rows):
        assert len(row) == 16, (y, row, len(row))
        line = []
        for x, ch in enumerate(row):
            if ch == ".":
                ch = base[y][x]
            if ch in "12345":
                col = wood[int(ch) - 1]
            elif ch in "67890":
                col = wood2["67890".index(ch)]
            else:
                col = PALETTE[ch]
            line.append(hexrgb(col))
        out.append(line)
    return out


def blank():
    return [["."] * 16 for _ in range(16)]


def rows_of(g):
    return ["".join(r) for r in g]


# =====================================================================================================
# Marktplatz — gestreifte Markise, Waren auf der Theke, Taler-Schild an der Front. Eiche + Fichte.
# =====================================================================================================
MARKET_FRONT = [
    "8777777777777778",
    "rrwwrrwwrrwwrrww",
    "rrwwrrwwrrwwrrww",
    "rrwwrrwwrrwwrrww",
    "RRWWRRWWRRWWRRWW",
    "R#W#R#W#R#W#R#W9",
    "8#rA#######E#fkf",
    "8rAKA#TYY#EEZkk9",
    "8AKAKTYYTUZZUTU9",
    "1121111211111211",
    "4444444444444444",
    "7......dd......9",
    "7.....dhgd.....9",
    "7.....dgGd.....9",
    "7......dd......9",
    "0999999999999990",
]
MARKET_SIDE = [
    "8777777777777778",
    "wrrwwrrwwrrwwrrw",
    "wrrwwrrwwrrwwrrw",
    "wrrwwrrwwrrwwrrw",
    "WRRWWRRWWRRWWRRW",
    "8999999999999998",
    "7..............9",
    "7..............9",
    "7..............9",
    "1121111211111211",
    "4444444444444444",
    "7.......6777778.",
    "7.......7T9YT98.",
    "7.......8UTUT89.",
    "7.......8999999.",
    "0999999999999990",
]
MARKET_BACK = [
    "8777777777777778",
    "wwrrwwrrwwrrwwrr",
    "wwrrwwrrwwrrwwrr",
    "WWRRWWRRWWRRWWRR",
    "8999999999999998",
    "7..............9",
    "7..............9",
    "7..............9",
    "7..............9",
    "7..............9",
    "7..............9",
    "7..............9",
    "7..............9",
    "7..............9",
    "7..............9",
    "0999999999999990",
]
MARKET_TOP = (["8777777777777778"]
              + ["wwrrwwrrwwrrwwrr"] * 4 + ["WWRRWWRRWWRRWWRR"]
              + ["wwrrwwrrwwrrwwrr"] * 4 + ["WWRRWWRRWWRRWWRR"]
              + ["wwrrwwrrwwrrwwrr"] * 4 + ["9999999999999999"])

# =====================================================================================================
# Schwarzmarkt — derselbe Stand, aber Dunkeleiche, zerfranste Markise, verhüllte Ware, Tiefenschiefer.
# =====================================================================================================
DEEP_BRICKS = [
    "ssstttutssstttut",
    "tttuuuvuttuuuuvu",
    "vvvvvvvvvvvvvvvv",
    "ttutsssttttutsss",
    "uuvuttttuuuvuttu",
    "vvvvvvvvvvvvvvvv",
]
BLACK_FRONT = [
    "4333333333333334",
    "mmxxmmxxmmxxmmxx",
    "mmxxmmxxmmxxmmxx",
    "MMXXMMXXMMXXMMXX",
    "M#X#MM#XM##XM#X4",
    "2##############4",
    "2##xxx####x#T##4",
    "2#xxXxxx##xxSP#4",
    "2xxXxxhXxxXxPS#4",
    "1121111211111211",
    "4444444444444444",
] + [r for r in DEEP_BRICKS[:5]]
BLACK_FRONT[12] = "tttuuu2333uuuuvu"
BLACK_FRONT[13] = "vvvvvv3Jj4vvvvvv"
BLACK_FRONT[14] = "ttutss4445ttutss"
BLACK_SIDE = [
    "4333333333333334",
    "xmmxxmmxxmmxxmmx",
    "xmmxxmmxxmmxxmmx",
    "XMMXXMMXXMMXXMMX",
    "4#M##MMX#M#X#MM4",
    "2..............4",
    "2..............4",
    "2..............4",
    "2..............4",
    "1121111211111211",
    "4444444444444444",
] + DEEP_BRICKS[:5]
BLACK_BACK = [
    "4333333333333334",
    "xxmmxxmmxxmmxxmm",
    "XXMMXXMMXXMMXXMM",
    "2..............4",
    "2..............4",
    "2..............4",
    "2..............4",
    "2..............4",
    "2..............4",
    "2..............4",
    "4444444444444444",
] + DEEP_BRICKS[:5]
BLACK_TOP = (["4333333333333334"]
             + ["xxmmxxmmxxmmxxmm"] * 3 + ["xxmm#xmmxxmmxxmm", "XXMMXXMMXXMMXXMM"]
             + ["xxmmxxmmxxmmxxmm"] * 2 + ["xxmmxxmmxxXXXxmm", "xxmmxxmmxxXXXxmm", "XXMMXXMMXXMMXXMM"]
             + ["xxmmxxmmxxmmxxmm"] * 2 + ["xxmmxx#mxxmmxxmm", "xxmmxxmmxxmmxxmm", "4444444444444444"])
BLACK_BOTTOM = [
    "vuuuuuuvvuuuuuuv",
    "uttttttuuttttttv",
    "utsssttuutsssttv",
    "utsttttuutsttttv",
    "uttttttuuttttttv",
    "utttttuuutttttuv",
    "uuuuuuuvuuuuuuuv",
    "vvvvvvvvvvvvvvvv",
    "vuuuuuuvvuuuuuuv",
    "uttttttuuttttttv",
    "utsssttuutsssttv",
    "utsttttuutsttttv",
    "uttttttuuttttttv",
    "utttttuuutttttuv",
    "uuuuuuuvuuuuuuuv",
    "vvvvvvvvvvvvvvvv",
]

# =====================================================================================================
# Schwarzes Brett — Fichtenrahmen, Eichenbrett, überlappende Aushänge mit Nadeln, Siegel und Taler.
# =====================================================================================================
# Vier Aushänge mit Nadeln, Schlagschatten nach rechts unten; der dritte überlappt den ersten, der
# kleine rechts unten verspricht einen Taler als Lohn, der mittlere trägt ein Wachssiegel.
BOARD_FRONT = [
    "6666666666666667",
    "6777777777777789",
    "67......ppippq89",
    "67pprpq.pnnnpq89",
    "67pnnnq5pppppq89",
    "67ppppq5pnnppq89",
    "67pnnpq5qqqqqQ89",
    "67pnnpprpq555589",
    "67qqqpnnnq....89",
    "67.55ppppq5piq89",
    "67...pnnpq5hgq89",
    "67...ppzrq5gGq89",
    "67...qqRzQ5pnq89",
    "67....55555qqQ89",
    "6888888888888899",
    "7999999999999990",
]
BOARD_SIDE = [
    "6666666666666667",
] + ["7" + VPLANKS[y][1:15].translate(str.maketrans("12345", "67890")) + "9" for y in range(1, 15)] + [
    "7999999999999990",
]
BOARD_BACK = [
    "6666666666666667",
    "6777777777777789",
    "67............89",
    "67.8..........89",
    "67..8.........89",
    "67...8........89",
    "67....8.......89",
    "67.....8......89",
    "67......8.....89",
    "67.......8....89",
    "67........8...89",
    "67.........8..89",
    "67..........8.89",
    "67...........889",
    "6888888888888899",
    "7999999999999990",
]
BOARD_TOP = (["1111111111111112"]
             + ["2" + PLANKS[y][1:15] + "4" for y in range(1, 15)]
             + ["2444444444444445"])
# Oberseite mit Mastschacht hinten rechts (Pixel 2,13 = Mastposition im Flaggenmodell): eingeholt fährt der Mast
# samt Flagge ganz in den Block, sichtbar bleibt nur die eisengefasste Öffnung.
BOARD_TOP_SHAFT = [r[:1] + ring + r[4:] if ring else r
                   for r, ring in zip(BOARD_TOP, [None] * 12 + ["Iij", "i#j", "jjJ"] + [None])]


# =====================================================================================================
# Statistik-Tafel — ein einziger Flipchart-Bogen statt vieler Zettel: oben eine Eisen-Klemmleiste, auf dem
# Pergament ein Säulendiagramm in Tinte (Achsen dunkel, Säulen mit Schatten rechts), oben rechts ein kleiner
# Taler. Unten zeigen zwei Stufen die umgeschlagenen Bögen darunter. Rahmen und Rückseite in Fichte.
# =====================================================================================================
STATS_FRONT = [
    "6666666666666667",
    "6IiiiiiiiiiiiiJ9",
    "6jjjjjjjjjjjjjJ9",
    "6ppppppppppphgp9",
    "6pNpppppppppgGp9",
    "6pNpppppppppppp9",
    "6pNpppppppnNppp9",
    "6pNpppppppnNppp9",
    "6pNppppnNpnNppp9",
    "6pNppppnNpnNppp9",
    "6pNpnNpnNpnNppp9",
    "6pNpnNpnNpnNppp9",
    "6pNNNNNNNNNNNNp9",
    "6qqqqqqqqqqqqqq9",
    "6QQQQQQQQQQQQQQ9",
    "7999999999999990",
]
STATS_SIDE = [
    "6666666666666667",
    "6IiiiiiiiiiiiiJ9",
    "6jjjjjjjjjjjjjJ9",
] + ["7" + VPLANKS[y][1:15].translate(str.maketrans("12345", "67890")) + "9" for y in range(3, 15)] + [
    "7999999999999990",
]
STATS_BACK = (["6666666666666667"]
              + ["6" + PLANKS[y][1:15].translate(str.maketrans("12345", "67890")) + "9" for y in range(1, 7)]
              + ["6" + "6" * 14 + "9", "6" + "7" * 14 + "9", "6" + "9" * 14 + "9"]
              + ["6" + PLANKS[y][1:15].translate(str.maketrans("12345", "67890")) + "9" for y in range(10, 15)]
              + ["7999999999999990"])
# Oberseite: Die oberste Reihe ist die Vorderkante — dort sitzt die Klemmleiste.
STATS_TOP = (["IiiiiiiiiiiiiiiJ", "jjjjjjjjjjjjjjjJ"]
             + ["2" + PLANKS[y][1:15] + "4" for y in range(2, 15)]
             + ["2444444444444445"])


# =====================================================================================================
# Trade-Chest — Kistenholz, Deckelfuge, Eisenbeschläge, Preisanhänger an der Lasche.
# =====================================================================================================
def chest_face(metal, lid=True, latch=None, extra=None):
    """Baut die Kistenfläche aus Rahmen, Deckel (Zeilen 0-4), Fuge (5) und Korpus (6-15)."""
    hi, mid, sh, dk = metal
    g = [
        "4444444444444444",
        "4112111111112114",
        "4221222232222134",
        "4222232222222234",
        "4333333333333334",
        "5555555555555555",
        "4222212222222234",
        "4223222221222334",
        "4222222322222234",
        "4232222222322234",
        "4444444444444444",
        "4221222212222134",
        "4222322222232234",
        "4222223222222334",
        "4333333333333334",
        "5555555555555555",
    ]
    g = [list(r) for r in g]
    # Eckwinkel
    for (x, y, c) in [(0, 0, hi), (1, 0, hi), (2, 0, mid), (0, 1, hi), (0, 2, mid),
                      (15, 0, mid), (14, 0, mid), (13, 0, mid), (15, 1, sh), (15, 2, sh),
                      (0, 15, mid), (1, 15, sh), (2, 15, sh), (0, 14, mid), (0, 13, sh),
                      (15, 15, dk), (14, 15, dk), (13, 15, dk), (15, 14, dk), (15, 13, dk)]:
        g[y][x] = c
    # Deckelbänder
    for y in range(0, 5):
        g[y][3] = hi if y < 2 else mid
        g[y][12] = mid if y < 2 else sh
    g[5][3] = sh
    g[5][12] = dk
    if latch:
        for (x, y, c) in latch:
            g[y][x] = c
    if extra:
        for (x, y, c) in extra:
            g[y][x] = c
    return ["".join(r) for r in g]


IRON = ("I", "i", "j", "J")
GOLD = ("h", "g", "G", "d")

TRADE_LATCH = [(7, 3, "I"), (8, 3, "i"), (7, 4, "i"), (8, 4, "j"), (7, 5, "i"), (8, 5, "j"),
               (7, 6, "j"), (8, 6, "J"), (7, 7, "5"), (8, 7, "5")]
TRADE_TAG = [(9, 6, "W"), (9, 7, "W"), (10, 8, "W"),
             (10, 9, "p"), (11, 9, "p"), (12, 9, "q"),
             (10, 10, "p"), (11, 10, "G"), (12, 10, "q"),
             (10, 11, "q"), (11, 11, "q"), (12, 11, "Q"), (13, 10, "5"), (13, 11, "5")]
TRADE_FRONT = chest_face(IRON, latch=TRADE_LATCH, extra=TRADE_TAG)
SIDE_HANDLE_IRON = [(6, 7, "j"), (7, 7, "i"), (8, 7, "i"), (9, 7, "j"), (6, 8, "J"), (9, 8, "J"),
                    (7, 8, "5"), (8, 8, "5")]
TRADE_SIDE = chest_face(IRON, extra=SIDE_HANDLE_IRON)
HINGES_IRON = [(2, 4, "i"), (3, 4, "i"), (4, 4, "j"), (2, 5, "j"), (3, 5, "j"), (4, 5, "J"),
               (11, 4, "i"), (12, 4, "j"), (13, 4, "j"), (11, 5, "j"), (12, 5, "J"), (13, 5, "J")]
TRADE_BACK = chest_face(IRON, extra=HINGES_IRON)


def chest_top(metal, center=None):
    hi, mid, sh, dk = metal
    g = [list("4" + PLANKS[y][1:15] + "4") for y in range(16)]
    g[0] = list("4444444444444444")
    g[15] = list("5555555555555555")
    for y in range(16):
        g[y][3] = hi if y < 8 else mid
        g[y][12] = mid if y < 8 else sh
    for (x, y, c) in [(0, 0, hi), (1, 0, hi), (0, 1, hi), (15, 0, mid), (14, 0, mid), (15, 1, sh),
                      (0, 15, mid), (1, 15, sh), (0, 14, sh), (15, 15, dk), (14, 15, dk), (15, 14, dk)]:
        g[y][x] = c
    if center:
        for (x, y, c) in center:
            g[y][x] = c
    return ["".join(r) for r in g]


TRADE_TOP = chest_top(IRON)


def chest_bottom():
    g = [list("5" + PLANKS[y][1:15] + "5") for y in range(16)]
    g[0] = list("5555555555555555")
    g[15] = list("5555555555555555")
    return ["".join(r) for r in g]


# =====================================================================================================
# Depositchest — gleiche Kiste, aber Goldbeschläge, Goldschloss, Taler-Medaillon und Münzschlitz oben.
# =====================================================================================================
# Das Schloss ist selbst ein Taler: der Querbalken des "T" ist geprägt, der Stiel ist das Schlüsselloch.
TALER_LOCK = [
    ".dddd.",
    "dhhhgd",
    "dGGGGd",
    "dhyygd",
    "dgyyGd",
    ".dddd.",
]
DEPOSIT_LOCK = ([(5 + x, 3 + y, c) for y, r in enumerate(TALER_LOCK) for x, c in enumerate(r) if c != "."]
                + [(x, 9, "5") for x in range(6, 10)])
DEPOSIT_FRONT = chest_face(GOLD, latch=DEPOSIT_LOCK)
SIDE_HANDLE_GOLD = [(x, y, {"j": "g", "i": "h", "J": "G"}.get(c, c)) for (x, y, c) in SIDE_HANDLE_IRON]
DEPOSIT_SIDE = chest_face(GOLD, extra=SIDE_HANDLE_GOLD)
HINGES_GOLD = [(x, y, {"i": "h", "j": "g", "J": "G"}[c]) for (x, y, c) in HINGES_IRON]
DEPOSIT_BACK = chest_face(GOLD, extra=HINGES_GOLD)
COIN_SLOT = ([(x, 6, "h") for x in range(5, 11)] + [(11, 6, "g")]
             + [(5, 7, "h")] + [(x, 7, "y") for x in range(6, 10)] + [(10, 7, "y"), (11, 7, "G")]
             + [(7, 7, "g"), (8, 7, "G")]
             + [(5, 8, "g")] + [(x, 8, "G") for x in range(6, 11)] + [(11, 8, "d")])
DEPOSIT_TOP = chest_top(GOLD, center=COIN_SLOT)

# =====================================================================================================
# Beacon und Neben-Beacon — echte 3D-Modelle wie der Vanilla-Beacon: halbtransparenter Glaskörper mit
# Metallrahmen, darin ein Obsidiansockel, ein Podest und ein um 45° gedrehter, leuchtender Kristall.
# Die Teiltexturen (_base, _core) nutzen getrennte UV-Bereiche; siehe models/block/beacon.json.
# Halbtransparente Pixel landen automatisch in der Translucent-Schicht, deckende bleiben solid.
# =====================================================================================================
def glass(frame, ring=False, uniform=False):
    """Glasfläche: deckender Rahmen, Glaskante (';'), getöntes Glas (','), Schlieren (':').

    uniform: Rahmen ohne Licht/Schatten-Kanten, alle Ecken gleich. Sonst trifft an jeder Würfelkante die helle
    Oberkante/linke Kante einer Fläche auf die dunkle Kante der Nachbarfläche, und das springt beim Umrunden.
    """
    hi, mid, sh, dk, corner = frame
    g = [[","] * 16 for _ in range(16)]
    for i in range(1, 15):
        g[1][i] = g[14][i] = g[i][1] = g[i][14] = ";"
    if uniform:
        for i in range(16):
            g[0][i] = g[15][i] = g[i][0] = g[i][15] = mid
        for (x, y) in [(0, 0), (15, 0), (0, 15), (15, 15)]:
            g[y][x] = sh
    else:
        for i in range(16):
            g[0][i] = hi
            g[i][0] = hi
            g[15][i] = sh
            g[i][15] = sh
        g[0][15] = mid
        g[15][0] = mid
        g[15][15] = dk
        g[0][0] = corner
    for (x, y) in [(3, 5), (4, 4), (5, 3), (4, 6), (11, 12), (12, 11), (12, 10)]:
        g[y][x] = ":"
    if ring:
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                if 4.6 <= math.hypot(dx, dy) <= 5.6:
                    g[y][x] = "h" if dx + dy < -2 else ("G" if dx + dy > 2 else "g")
    return ["".join(r) for r in g]


GOLD_FRAME = ("h", "g", "G", "d", "H")
IRON_FRAME = ("I", "i", "j", "J", "h")   # Eisenrahmen


def plinth(trim, runes, side_rows):
    """Obsidiansockel. Oberseite: Zeilen 0-11, Spalten 2-13. Seiten: die letzten side_rows Zeilen."""
    hi, mid, sh = trim
    noise = [
        "OOoOOOOyOOOoOOOO",
        "OyOOOooOOyOOOOyO",
        "OOOyOOOOOOOOoOOO",
        "oOOOOyOOOyOOOOOy",
    ]
    g = [list(noise[(y * 3) % 4][y % 5:] + noise[(y * 3) % 4][:y % 5]) for y in range(16)]
    for x in range(2, 14):            # Zierkante der Oberseite
        g[0][x] = hi
        g[11][x] = sh
    for y in range(12):
        g[y][2] = hi
        g[y][13] = sh
    g[0][13] = mid
    g[11][2] = mid
    top = 16 - side_rows             # erste Seitenzeile: Metallband
    for x in range(2, 14):
        g[top][x] = mid
    g[top][2] = hi
    g[top][13] = sh
    for x in runes:                  # eingelegte Runen auf der Sockelseite
        g[top + 1][x] = mid
    for x in range(16):
        g[15][x] = "y" if g[15][x] != mid else mid
    return ["".join(r) for r in g]


def core(regions):
    g = [[" "] * 16 for _ in range(16)]
    for (x0, y0, rows) in regions:
        for dy, r in enumerate(rows):
            for dx, c in enumerate(r):
                g[y0 + dy][x0 + dx] = c
    return ["".join(r) for r in g]


# Flaggen-Atlas (models/block/bulletin_board_flag_*.json, per Multipart auf dem Würfel): Fichtenmast 1x14 Licht-
# (0,0) und Schattenseite (1,0), Mastfuß 1x1 (2,0), Eisenknauf seitlich 2x2 (4,0) und oben 2x2 (4,2), rotes Tuch
# 7x5 mit Taler (7,0), Tuchkanten seitlich 1x5 (14,0) und oben/unten 7x1 (7,5). Mast und Tuch fahren aus dem
# Schacht in der Oberseite, sobald neue Contracts aushängen, und beim Öffnen des Boards ganz in den Block zurück.
BOARD_FLAG = core([
    (0, 0, ["6", "7", "7", "8", "7", "7", "7", "8", "7", "7", "7", "8", "7", "7"]),
    (1, 0, ["8", "8", "9", "8", "8", "9", "8", "8", "9", "8", "8", "9", "8", "9"]),
    (2, 0, ["8"]),
    (4, 0, ["Ii", "jJ"]),
    (4, 2, ["Ii", "ij"]),
    (7, 0, ["rrrrrrr", "rrrhgrR", "rrrgGrR", "rrrrrRR", "RRRRRRz"]),
    (14, 0, ["R", "R", "R", "R", "z"]),
    (7, 5, ["RRRRRRR"]),
])


# Hauptbeacon: Kristall-Seite 5x8 und oben 5x5, Spitze in zwei Stufen (3x2/3x3, 1x1),
# Goldpodest oben 8x8 und Seite 8x1
BEACON_CORE = core([
    (0, 0, ["CcccC", "bCccb", "bCcCB", "bCcbB", "BbCbB", "BbCba", "BbbBa", "aBbBa"]),
    (0, 8, ["CcccC", "ccccC", "cccCb", "CcCbB", "bCbBB"]),
    (5, 0, ["ccC", "CcB"]),
    (5, 2, ["ccc", "ccC", "cCb"]),
    (8, 0, ["c"]),
    (9, 0, ["c"]),
    (8, 8, ["hhhhhhhg", "hggggggG", "hgGGGGgG", "hgGddGgG", "hgGddGgG", "hgGGGGgG", "hggggggG", "gGGGGGGd"]),
    (0, 15, ["hggggggG"]),
])
# Neben-Beacon: Kristall 3x6, Spitze 1x1, Podest aus Eisen 4x4
RELAY_CORE = core([
    (0, 0, ["cCb", "Ccb", "CbB", "bCB", "bBa", "Bba"]),
    (4, 0, ["C"]),
    (6, 0, ["c"]),
    (0, 8, ["cCb", "CcB", "bBa"]),
    (8, 0, ["IIIi", "Iiij", "iiij", "ijjJ"]),
    (8, 4, ["Iiij"]),
])
BEACON_GLASS = glass(GOLD_FRAME)
BEACON_GLASS_TOP = glass(GOLD_FRAME, ring=True)
BEACON_BASE = plinth(("h", "g", "G"), [4, 7, 8, 11], 3)
RELAY_GLASS = glass(IRON_FRAME, uniform=True)
RELAY_BASE = plinth(("I", "i", "J"), [], 2)

# Chunkloader: derselbe Glaskasten, aber mit dunklem Obsidianrahmen und Ecken im Türkis der Enderperle; statt
# des Kristalls ein flacher Podestring, über dem die Perle schwebt (gezeichnet vom ChunkLoaderRenderer).
# Ring oben 6x6 (Obsidianrand, Perlen-Einlage, dunkle Mitte), Ringseite 6x1.
OBSIDIAN_FRAME = ("o", "O", "<", "y", "o")
CHUNK_LOADER_GLASS = glass(OBSIDIAN_FRAME, uniform=True)
CHUNK_LOADER_BASE = plinth(("*", "=", "<"), [4, 7, 8, 11], 2)
CHUNK_LOADER_CORE = core([
    (0, 0, ["oooooO", "o!**=y", "o*>>=y", "o*>>=y", "o==<<y", "Oyyyyy"]),
    (0, 6, ["o*==<y"]),
])

# =====================================================================================================
# Werkstatt — Fichtenwerkbank mit Eisenkante, Werkzeugwand vorn, Schubladen, Seitenwände mit Säge bzw.
# Regal voller Barren. Oben die Arbeitsfläche mit Plan, Amboss-Platte und Goldbarren.
# =====================================================================================================
SHOP_FRONT = [
    "IIIIIIIIIIIIIIij",
    "1111211111121111",
    "4444444444444444",
    "2~~T~~~TT~~~~T~4",
    "2IIij~j~~jIij7~4",
    "2ijjJ~~jj~ijJ8~4",
    "2~~7~~~JJ~~j~8~4",
    "2~~7~~j~~j~~~7~4",
    "2~~7~~j~~j~~~8~4",
    "2~~8~J~~~~J~~8~4",
    "6666666666666666",
    "2789999979899994",
    "2777i777977i7774",
    "2888j888988j8884",
    "2999999999999994",
    "5555555555555555",
]
SHOP_LEFT = [
    "IIIIIIIIIIIIIIij",
    "1111211111121111",
    "4444444444444444",
    "2..............4",
    "2..............4",
    "2.IiiiiiiiI78..4",
    "2.ijjjjjjji89..4",
    "2.j.j.j.j.j9...4",
    "2..............4",
    "2..............4",
    "2..TT.......T..4",
    "2..ij......iJ..4",
    "2..jJ......jJ..4",
    "2..............4",
    "2..............4",
    "5555555555555555",
]
SHOP_RIGHT = [
    "IIIIIIIIIIIIIIij",
    "1111211111121111",
    "4444444444444444",
    "2~~~~~~~~~~~~~~4",
    "2~hhhg~~~IIij~~4",
    "2~gGGd~~~ijjJ~~4",
    "6666666666666666",
    "9999999999999999",
    "2~~~~~~~~~~~~~~4",
    "2~+++-~~~~TT~~~4",
    "2~+---~~~TUhU~~4",
    "6666666666666666",
    "9999999999999999",
    "2..............4",
    "2..............4",
    "5555555555555555",
]
SHOP_BACK = [
    "IIIIIIIIIIIIIIij",
    "1111211111121111",
    "4444444444444444",
] + ["2" + VPLANKS[y][1:15] + "4" for y in range(3, 15)] + [
    "5555555555555555",
]
SHOP_TOP = [
    "ii4444444444444j",
    "i31111111111113j",
    "4377777777777774",
    "437pppq7777777 4".replace(" ", "4"),
    "437pnnq7777777 4".replace(" ", "4"),
    "437pqnQ77IIi77 4".replace(" ", "4"),
    "437qQQQ7Iiiij7 4".replace(" ", "4"),
    "4377777777ij7774",
    "43777777Jjjjj774",
    "4377777777777774",
    "4377hhg777777774",
    "4377gGd7777jJ774",
    "437777777777jJ74",
    "4377777777777774",
    "j44444444444444J",
    "jJ5555555555555J",
]


def shop_bottom():
    return ["5" + PLANKS[y][1:15] + "5" for y in range(16)]


# =====================================================================================================
# Reparaturbank — Eichenwerkbank (Familie Handel), Unterscheidung zur Werkstatt über die Form: vorn ein
# Schleifrad im Fichtenständer mit Kurbel, das in einen Wassertrog taucht; seitlich Schraubstock und Ölkanne;
# oben der Schlitz des Rads, Lederflicken mit Nadel, Wetzstein und eine Münzschale — der einzige Taler am Block.
# Eisenbeschläge nur an den Ecken der Platte, nicht als durchgehende Kante wie bei der Werkstatt.
# =====================================================================================================
REPAIR_EDGE = "Ii111211111121iJ"
REPAIR_FRONT = [
    REPAIR_EDGE,
    "4444444444444444",
    "2~~~~~~~~~~~~~~4",
    "2~7~~~JJJJ~~~7~4",
    "2~7~~JIIiiJ~~7i4",
    "2~7~JIIiiijJ~7i4",
    "2~7jJIiJJijJj7j4",
    "2~7~JiiJJjjJ~7~4",
    "2~8~JiijjjjJ~8~4",
    "2~86BJijjjJB68~4",
    "2~867777777798~4",
    "2~86jiiiiiij98~4",
    "2~899999999908~4",
    "1112111111211111",
    "4444444444444444",
    "5555555555555555",
]
REPAIR_SIDE = [
    REPAIR_EDGE,
    "4444444444444444",
    "2.JIiiJ........4",
    "2.JijjJ....i...4",
    "2.JijjJiiiiI...4",
    "2.JjjjJ....j...4",
    "2..JJJ.........4",
    "2..............4",
    "2.........j....4",
    "2........-+-...4",
    "2.......-+++-..4",
    "2.....--++++-..4",
    "2.......++++-..4",
    "2........---...4",
    "2..............4",
    "5555555555555555",
]
REPAIR_BACK = [
    REPAIR_EDGE,
    "4444444444444444",
] + ["2" + VPLANKS[y][1:15] + "4" for y in range(2, 15)] + [
    "5555555555555555",
]
REPAIR_BACK[5] = "2Iiiiiiiiiiiiij4"
REPAIR_BACK[6] = "2jjjjjjjjjjjjjJ4"
REPAIR_BACK[11] = "2Iiiiiiiiiiiiij4"
REPAIR_BACK[12] = "2jjjjjjjjjjjjjJ4"
REPAIR_TOP = [
    REPAIR_EDGE,
    "2..............4",
    "2...~JJJJJJ~...4",
    "2...~jiiiij~...4",
    "2...~~~~~~~~...4",
    "2..............4",
    "2.UUUU.........4",
    "2.UTwU.i...dd..4",
    "2.UwTU..i.dhgd.4",
    "2.UUUU....dgGd.4",
    "2..........dd..4",
    "2..............4",
    "2..iiiiij......4",
    "2..JJJJJJ......4",
    "2..............4",
    "5555555555555555",
]


def repair_bottom():
    return ["5" + PLANKS[y][1:15] + "5" for y in range(16)]


# =====================================================================================================
# Spielautomat — Familie Handel, aber als einziger Block ganz aus Eisen: oben ein rotes Schild mit Taler und
# zwei Lämpchen, darunter das 1 px tief eingesetzte Walzenfenster (models/block/slot_machine.json). Im Fenster
# liegen drei cremefarbene Walzen; die Symbole zeichnet der SlotMachineRenderer. Rote Marken links und rechts
# zeigen die Gewinnlinie. Unten Knopf, Münzschlitz in Gold und die dunkle Auswurfschale mit Goldkante.
# Der Hebel sitzt rechts (Westseite) und ist ein eigener Atlas (_lever).
# =====================================================================================================
SLOT_FRONT = [
    "IIIIIIIIIIIIIIIj",
    "IhrrrrrddrrrrrhJ",
    "IrrrrrdhgdrrrrRJ",
    "IhRRRRRddRRRRRhJ",
    "ijjjjjjjjjjjjjjJ",
    "iWWWWJWWWWJWWWWJ",
    "iwwwwJwwwwJwwwwJ",
    "rwwwwJwwwwJwwwwR",
    "iwwwwJwwwwJwwwwJ",
    "iWWWWJWWWWJWWWWJ",
    "iIIIIIIIIIIIIIiJ",
    "ijrrjjjjjjjdhhdJ",
    "ijRzjjjjjjjdyydJ",
    "ij~##########~jJ",
    "ijGggggggggggGjJ",
    "jJJJJJJJJJJJJJJJ",
]
SLOT_SIDE = [
    "jjjjjjjjjjjjjjjj",
    "jrrrrrrrrrrrrrrj",
    "jrrrrrrrrrrrrrrj",
    "jRRRRRRRRRRRRRRj",
    "jjjjjjjjjjjjjjjj",
    "jiiiiiiiiiiiiiij",
    "jiIiiiiiiiiiIiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiIiiiiiiiiiIiij",
    "jiiiiiiiiiiiiiij",
    "jjjjjjjjjjjjjjjj",
]
SLOT_BACK = [
    "jjjjjjjjjjjjjjjj",
    "jrrrrrrrrrrrrrrj",
    "jrrrrrrrrrrrrrrj",
    "jRRRRRRRRRRRRRRj",
    "jjjjjjjjjjjjjjjj",
    "jiiiiiiiiiiiiiij",
    "jiiJJJJJJJJJJiij",
    "jiiJiiiiiiiijiij",
    "jiiJiiiiiiiijiij",
    "jiiJiiiiyiiijiij",
    "jiiJiiiiyiiijiij",
    "jiiJiiiiiiiijiij",
    "jiiJjjjjjjjjjiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jjjjjjjjjjjjjjjj",
]
SLOT_TOP = [
    "jjjjjjjjjjjjjjjj",
    "jIiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiJJJJJJJJiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiJJJJJJJJiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiJJJJJJJJiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiJJJJJJJJiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jjjjjjjjjjjjjjjj",
]
SLOT_BOTTOM = ["J" * 16] + ["J" + "j" * 14 + "J" for _ in range(14)] + ["J" * 16]
# Hebel-Atlas: Lagerseite 4x3 (0,0), Lager vorn/hinten 1x3 (5,0), oben/unten 1x4 (12,0), Stange 1x5 (7,0),
# Knauf seitlich 2x2 (9,0) und oben 2x2 (9,2).
SLOT_LEVER = core([
    (0, 0, ["iiij", "ijjJ", "jJJJ"]),
    (5, 0, ["i", "j", "J"]),
    (12, 0, ["i", "i", "j", "J"]),
    (7, 0, ["I", "i", "i", "j", "j"]),
    (9, 0, ["hg", "gG"]),
    (9, 2, ["Hh", "hg"]),
])


# =====================================================================================================

# =====================================================================================================
# Recycler — Eisenkasten wie der Spielautomat, aber Werkstatt statt Kasino: oben ein Einwurfgitter über dunklem
# Schacht, vorn ein Schleifrad aus Tiefenschiefer mit Achse in einer Kastenöffnung, darunter der Schrottschacht
# mit Kupferkante. Seiten mit Nieten und einem Rostband aus Kupfer; unten die Ausgabeklappe.
# =====================================================================================================
RECYCLER_FRONT = [
    "jjjjjjjjjjjjjjjj",
    "jIiiiiiiiiiiiiij",
    "ji############ij",
    "ji#~~sssss~~~#ij",
    "ji#~sttttts~~#ij",
    "ji#stuuuuuts~#ij",
    "ji#stuJjJuts~#ij",
    "ji#stujIjuts~#ij",
    "ji#stuJjJuts~#ij",
    "ji#stuuuuuts~#ij",
    "ji#vstttttsv~#ij",
    "jiiiiiiiiiiiiiij",
    "ji+----------+ij",
    "ji-##########-ij",
    "jiIiiiiiiiiiiIij",
    "jjjjjjjjjjjjjjjj",
]
# Im Betrieb (Blockzustand lit): Glut hinter dem Schleifrad und im Schrottschacht, wie die Ofenfront.
RECYCLER_FRONT_ON = [row.replace("~", "k") for row in RECYCLER_FRONT[:11]] + [
    RECYCLER_FRONT[11],
    RECYCLER_FRONT[12],
    "ji-###kkkk###-ij",
    RECYCLER_FRONT[14],
    RECYCLER_FRONT[15],
]
RECYCLER_SIDE = [
    "jjjjjjjjjjjjjjjj",
    "jIiiiiiiiiiiiIij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "j++++++++++++++j",
    "j--------------j",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "jiiiiiiiiiiiiiij",
    "j++++++++++++++j",
    "j--------------j",
    "jiiiiiiiiiiiiiij",
    "jIiiiiiiiiiiiIij",
    "jjjjjjjjjjjjjjjj",
]
RECYCLER_BACK = [
    "jjjjjjjjjjjjjjjj",
    "jIiiiiiiiiiiiIij",
    "jiiiiiiiiiiiiiij",
    "jiiJJJJJJJJJJiij",
    "jiiiiiiiiiiiiiij",
    "jiiJJJJJJJJJJiij",
    "jiiiiiiiiiiiiiij",
    "jiiJJJJJJJJJJiij",
    "jiiiiiiiiiiiiiij",
    "jiiJJJJJJJJJJiij",
    "jiiiiiiiiiiiiiij",
    "j++++++++++++++j",
    "j--------------j",
    "jiiiiiiiiiiiiiij",
    "jIiiiiiiiiiiiIij",
    "jjjjjjjjjjjjjjjj",
]
RECYCLER_TOP = [
    "jjjjjjjjjjjjjjjj",
    "jIiiiiiiiiiiiiij",
    "ji############ij",
    "ji#j~j~j~j~j~#ij",
    "ji#j#j#j#j#j##ij",
    "ji#jjjjjjjjjj#ij",
    "ji#j#j#j#j#j##ij",
    "ji#j~j~j~j~j~#ij",
    "ji#j#j#j#j#j##ij",
    "ji#jjjjjjjjjj#ij",
    "ji#j#j#j#j#j##ij",
    "ji#j~j~j~j~j~#ij",
    "ji#j#j#j#j#j##ij",
    "ji############ij",
    "jiiiiiiiiiiiiiij",
    "jjjjjjjjjjjjjjjj",
]
RECYCLER_BOTTOM = [
    "JJJJJJJJJJJJJJJJ",
    "JjjjjjjjjjjjjjjJ",
    "JjjjjjjjjjjjjjjJ",
    "JjjJJJJJJJJJJjjJ",
    "JjjJ########JjjJ",
    "JjjJ#~~~~~~#JjjJ",
    "JjjJ#~~~~~~#JjjJ",
    "JjjJ#~~~~~~#JjjJ",
    "JjjJ#~~~~~~#JjjJ",
    "JjjJ#~~~~~~#JjjJ",
    "JjjJ#~~~~~~#JjjJ",
    "JjjJ########JjjJ",
    "JjjJJJJJJJJJJjjJ",
    "JjjjjjjjjjjjjjjJ",
    "JjjjjjjjjjjjjjjJ",
    "JJJJJJJJJJJJJJJJ",
]


def build():
    T = {}
    T["market_stand_front"] = paint(MARKET_FRONT, OAK, SPRUCE, VPLANKS)
    T["market_stand_side"] = paint(MARKET_SIDE, OAK, SPRUCE)
    T["market_stand_back"] = paint(MARKET_BACK, OAK, SPRUCE, VPLANKS)
    T["market_stand_top"] = paint(MARKET_TOP, OAK, SPRUCE)
    T["market_stand_bottom"] = paint(PLANKS, SPRUCE)

    T["blackmarket_front"] = paint(BLACK_FRONT, DARK_OAK, SPRUCE, VPLANKS)
    T["blackmarket_side"] = paint(BLACK_SIDE, DARK_OAK)
    T["blackmarket_back"] = paint(BLACK_BACK, DARK_OAK, SPRUCE, VPLANKS)
    T["blackmarket_top"] = paint(BLACK_TOP, DARK_OAK)
    T["blackmarket_bottom"] = paint(BLACK_BOTTOM, DARK_OAK)

    T["bulletin_board_front"] = paint(BOARD_FRONT, OAK, SPRUCE)
    T["bulletin_board_side"] = paint(BOARD_SIDE, OAK, SPRUCE)
    T["bulletin_board_back"] = paint(BOARD_BACK, SPRUCE, SPRUCE)
    T["bulletin_board_top"] = paint(BOARD_TOP_SHAFT, SPRUCE)
    T["bulletin_board_bottom"] = paint(BOARD_TOP, SPRUCE)
    T["bulletin_board_flag"] = paint(BOARD_FLAG, OAK, SPRUCE)

    T["statistics_board_front"] = paint(STATS_FRONT, SPRUCE, SPRUCE)
    T["statistics_board_side"] = paint(STATS_SIDE, SPRUCE, SPRUCE)
    T["statistics_board_back"] = paint(STATS_BACK, SPRUCE, SPRUCE)
    T["statistics_board_top"] = paint(STATS_TOP, SPRUCE)
    T["statistics_board_bottom"] = paint(BOARD_TOP, SPRUCE)

    T["trade_chest_front"] = paint(TRADE_FRONT, CHEST)
    T["trade_chest_side"] = paint(TRADE_SIDE, CHEST)
    T["trade_chest_back"] = paint(TRADE_BACK, CHEST)
    T["trade_chest_top"] = paint(TRADE_TOP, CHEST)
    T["trade_chest_bottom"] = paint(chest_bottom(), CHEST)

    T["deposit_chest_front"] = paint(DEPOSIT_FRONT, CHEST)
    T["deposit_chest_side"] = paint(DEPOSIT_SIDE, CHEST)
    T["deposit_chest_back"] = paint(DEPOSIT_BACK, CHEST)
    T["deposit_chest_top"] = paint(DEPOSIT_TOP, CHEST)
    T["deposit_chest_bottom"] = paint(chest_bottom(), CHEST)

    T["beacon_glass"] = paint(BEACON_GLASS, OAK)
    T["beacon_glass_top"] = paint(BEACON_GLASS_TOP, OAK)
    T["beacon_base"] = paint(BEACON_BASE, OAK)
    T["beacon_core"] = paint(BEACON_CORE, OAK)
    T["relay_beacon_glass"] = paint(RELAY_GLASS, OAK)
    T["relay_beacon_base"] = paint(RELAY_BASE, OAK)
    T["relay_beacon_core"] = paint(RELAY_CORE, OAK)
    T["chunk_loader_glass"] = paint(CHUNK_LOADER_GLASS, OAK)
    T["chunk_loader_base"] = paint(CHUNK_LOADER_BASE, OAK)
    T["chunk_loader_core"] = paint(CHUNK_LOADER_CORE, OAK)

    T["upgrade_table_front"] = paint(SHOP_FRONT, SPRUCE, OAK)
    T["upgrade_table_left"] = paint(SHOP_LEFT, SPRUCE, OAK)
    T["upgrade_table_right"] = paint(SHOP_RIGHT, SPRUCE, OAK)
    T["upgrade_table_back"] = paint(SHOP_BACK, SPRUCE, OAK)
    T["upgrade_table_top"] = paint(SHOP_TOP, SPRUCE, OAK)
    T["upgrade_table_bottom"] = paint(shop_bottom(), SPRUCE, OAK)

    T["repair_station_front"] = paint(REPAIR_FRONT, OAK, SPRUCE)
    T["repair_station_side"] = paint(REPAIR_SIDE, OAK, SPRUCE)
    T["repair_station_back"] = paint(REPAIR_BACK, OAK, SPRUCE, VPLANKS)
    T["repair_station_top"] = paint(REPAIR_TOP, OAK, SPRUCE)
    T["repair_station_bottom"] = paint(repair_bottom(), OAK, SPRUCE)

    T["slot_machine_front"] = paint(SLOT_FRONT, OAK)
    T["slot_machine_side"] = paint(SLOT_SIDE, OAK)
    T["slot_machine_back"] = paint(SLOT_BACK, OAK)
    T["slot_machine_top"] = paint(SLOT_TOP, OAK)
    T["slot_machine_bottom"] = paint(SLOT_BOTTOM, OAK)
    T["slot_machine_lever"] = paint(SLOT_LEVER, OAK)

    T["recycler_front"] = paint(RECYCLER_FRONT, OAK)
    T["recycler_front_on"] = paint(RECYCLER_FRONT_ON, OAK)
    T["recycler_side"] = paint(RECYCLER_SIDE, OAK)
    T["recycler_back"] = paint(RECYCLER_BACK, OAK)
    T["recycler_top"] = paint(RECYCLER_TOP, OAK)
    T["recycler_bottom"] = paint(RECYCLER_BOTTOM, OAK)
    return T


# --- PNG ---------------------------------------------------------------------------------------------
# --- Vorschau: rendert die echten Blockmodelle (Elemente, UV, Drehung, Glas mit Alpha) ----------------
MODELS = os.path.join(ROOT, "src/main/resources/assets/dtp/models/block")
SHADE = {"up": 1.0, "down": 0.5, "north": 0.8, "south": 0.8, "west": 0.6, "east": 0.6}
CUBE_PARENTS = {
    "minecraft:block/cube": {d: "#" + d for d in SHADE},
    "minecraft:block/cube_bottom_top": {"up": "#top", "down": "#bottom", "north": "#side", "south": "#side",
                                        "west": "#side", "east": "#side"},
}


def load_elements(name):
    import json
    with open(os.path.join(MODELS, name + ".json")) as f:
        m = json.load(f)
    tex = m["textures"]
    if "elements" in m:
        elements = m["elements"]
    else:
        faces = {d: {"uv": [0, 0, 16, 16], "texture": t} for d, t in CUBE_PARENTS[m["parent"]].items()}
        elements = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces}]

    def resolve(ref):
        while ref.startswith("#"):
            ref = tex[ref[1:]]
        return ref.split("/")[-1]
    return elements, resolve


def face_corners(f, t, d):
    """Ecken (u0v0, u1v0, u0v1) einer Elementfläche nach Minecrafts UV-Konvention."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    return {
        "north": ((x1, y1, z0), (x0, y1, z0), (x1, y0, z0)),
        "south": ((x0, y1, z1), (x1, y1, z1), (x0, y0, z1)),
        "west": ((x0, y1, z0), (x0, y1, z1), (x0, y0, z0)),
        "east": ((x1, y1, z1), (x1, y1, z0), (x1, y0, z1)),
        "up": ((x0, y1, z0), (x1, y1, z0), (x0, y1, z1)),
        "down": ((x0, y0, z1), (x1, y0, z1), (x0, y0, z0)),
    }[d]


def render_model(name, T, S=8):
    """Blick von Nordwest oben: Nordseite (Front) links, Westseite rechts."""
    r3, r2, r6 = math.sqrt(3), math.sqrt(2), math.sqrt(6)
    fwd, right, up = (1 / r3, -1 / r3, 1 / r3), (-1 / r2, 0, 1 / r2), (1 / r6, 2 / r6, 1 / r6)
    # Oben Platz für Teile über dem Block (Fahnenmast bis y 31.5).
    W, H, top = int(23 * S), int(39 * S), 12 * S
    img = [[None] * W for _ in range(H)]
    zbuf = [[1e9] * W for _ in range(H)]
    frags = [[[] for _ in range(W)] for _ in range(H)]

    def dot(a, b):
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]

    def proj(p):
        q = (p[0] - 8, p[1] - 8, p[2] - 8)
        return (W / 2 + dot(q, right) * S, (H + top) / 2 - dot(q, up) * S, dot(q, fwd))

    # "a+b" setzt Multipart-Teile zusammen, z. B. Würfel und Fahnenmast des Bulletin Boards.
    parts = []
    for part in name.split("+"):
        elements, resolve = load_elements(part)
        parts += [(el, resolve) for el in elements]
    for el, resolve in parts:
        rot = el.get("rotation")
        for d, face in el["faces"].items():
            pts = list(face_corners(el["from"], el["to"], d))
            if rot:
                a = math.radians(rot.get("angle", 0))
                ox, _, oz = rot["origin"]
                pts = [(ox + (x - ox) * math.cos(a) + (z - oz) * math.sin(a), y,
                        oz - (x - ox) * math.sin(a) + (z - oz) * math.cos(a)) for (x, y, z) in pts]
            e1 = tuple(pts[1][k] - pts[0][k] for k in range(3))
            e2 = tuple(pts[2][k] - pts[0][k] for k in range(3))
            n = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0])
            if dot(n, fwd) <= 0:
                continue
            tex = T[resolve(face["texture"])]
            u0, v0, u1, v1 = face["uv"]
            P0, P1, P2 = proj(pts[0]), proj(pts[1]), proj(pts[2])
            ax, ay, bx, by = P1[0] - P0[0], P1[1] - P0[1], P2[0] - P0[0], P2[1] - P0[1]
            det = ax * by - ay * bx
            if abs(det) < 1e-9:
                continue
            xs = [P0[0], P1[0], P2[0], P1[0] + bx]
            ys = [P0[1], P1[1], P2[1], P1[1] + by]
            for py in range(max(0, int(min(ys))), min(H, int(max(ys)) + 1)):
                for px in range(max(0, int(min(xs))), min(W, int(max(xs)) + 1)):
                    dx, dy = px + 0.5 - P0[0], py + 0.5 - P0[1]
                    sx = (dx * by - dy * bx) / det
                    ty = (ax * dy - ay * dx) / det
                    if not (0 <= sx < 1 and 0 <= ty < 1):
                        continue
                    u = int(u0 + sx * (u1 - u0)) if u1 > u0 else int(u0 - sx * (u0 - u1) - 1e-6)
                    v = int(v0 + ty * (v1 - v0)) if v1 > v0 else int(v0 - ty * (v0 - v1) - 1e-6)
                    r, g, b, alpha = tex[min(15, max(0, v))][min(15, max(0, u))]
                    if alpha == 0:
                        continue
                    lum = 1.0 if el.get("light_emission", 0) >= 10 and d != "down" else SHADE[d]
                    z = P0[2] + sx * (P1[2] - P0[2]) + ty * (P2[2] - P0[2])
                    col = (r * lum, g * lum, b * lum)
                    if alpha == 255:
                        if z < zbuf[py][px]:
                            zbuf[py][px] = z
                            img[py][px] = col
                    else:
                        frags[py][px].append((z, col, alpha / 255))
    out = []
    for py in range(H):
        row = []
        for px in range(W):
            base = img[py][px]
            a_acc = 1.0 if base else 0.0
            col = base or (0, 0, 0)
            for z, c, a in sorted(frags[py][px], key=lambda f: -f[0]):
                if z >= zbuf[py][px]:
                    continue
                col = tuple(c[k] * a + col[k] * (1 - a) for k in range(3)) if a_acc else c
                a_acc = a_acc + a * (1 - a_acc) if a_acc else a
            row.append(tuple(int(min(255, v)) for v in col) + (int(a_acc * 255),))
        out.append(row)
    return out


def preview(T, path):
    blocks = ["market_stand", "blackmarket", "bulletin_board",
              "bulletin_board+bulletin_board_flag_raised", "statistics_board", "trade_chest", "deposit_chest",
              "beacon", "relay_beacon", "chunk_loader", "upgrade_table", "repair_station", "slot_machine", "recycler"]
    S, k, gap = 8, 6, 10
    cw, ch = 23 * S, 39 * S
    rowh = ch + gap
    W = cw + gap + 7 * (16 * k + gap)
    bg = (54, 54, 60)
    img = [[bg + (255,)] * W for _ in range(rowh * len(blocks))]
    for bi, name in enumerate(blocks):
        cube = render_model(name, T, S)
        y0 = bi * rowh
        for y in range(ch):
            for x in range(cw):
                r, g, b, a = cube[y][x]
                if a:
                    f = a / 255
                    img[y0 + y][x] = tuple(int(c * f + bg[i] * (1 - f)) for i, c in enumerate((r, g, b))) + (255,)
        names = [n for n in T if n.startswith(name.split("+")[0] + "_")]
        for fi, face in enumerate(names[:7]):
            tex = T[face]
            x0 = cw + gap + fi * (16 * k + gap)
            for y in range(16 * k):
                for x in range(16 * k):
                    r, g, b, a = tex[y // k][x // k]
                    chk = 90 if ((x // k) + (y // k)) % 2 else 70
                    f = a / 255
                    img[y0 + 50 + y][x0 + x] = (int(r * f + chk * (1 - f)), int(g * f + chk * (1 - f)),
                                                int(b * f + chk * (1 - f)), 255)
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
    if args.preview:
        preview(T, args.preview)
        print("Vorschau:", args.preview)


if __name__ == "__main__":
    main()
