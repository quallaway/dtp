#!/usr/bin/env python3
"""Erzeugt die eigenen Upgrade-Icons für die Bäume von Beacon und Werkstatt (GUI-Sprites, Vanilla-Stil).

Die Icons stehen zwischen Vanilla-Item- und Effekt-Symbolen und sollen dort nicht auffallen: Vanilla-Farben
(Glas und Kork vom Trank, Eisenspitzhacke, Schwimmer, Schwein, Bewegungsstriche vom Speed-Effekt), dunkle Kontur
im Materialton, Licht oben links. Effekt-artige Icons sind 18x18 wie die Vanilla-Effekt-Sprites, alles andere
16x16 wie Items. Jedes Icon ist ein handgesetztes Raster; '.' ist transparent, die übrigen Zeichen stehen in der
Legende des Icons.

    python3 scripts/upgrade_icons.py                    # schreibt nach textures/gui/sprites/upgrade/
    python3 scripts/upgrade_icons.py --preview X.png    # Vorschau auf den Feldern des Baums, neben Vanilla-Effekten
"""
import argparse
import os
import struct
import sys
import zipfile
import zlib

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from png_writer import write_png  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/dtp/textures/gui/sprites/upgrade")
SOURCES_JAR = os.path.join(ROOT, "build/moddev/artifacts/minecraft-patched-26.3.0.99-sources.jar")

# --- gemeinsame Vanilla-Farben ------------------------------------------------------------------------
GOLD = {"H": "#FFFDE0", "h": "#FDF55F", "g": "#E9B115", "G": "#B26411", "o": "#752802"}       # Goldbarren
OAK = {"1": "#B8945F", "2": "#9C7F4E", "3": "#7E6237", "4": "#5D4826", "5": "#3E2F17"}         # Eichenbretter
DARK = {"1": "#6B4A2A", "2": "#553A20", "3": "#432D18", "4": "#311F10", "5": "#1E1308"}        # Dunkeleiche
IRON = {"I": "#FFFFFF", "i": "#D8D8D8", "j": "#C1C1C1", "J": "#8A8A8A", "K": "#444444", "k": "#181818"}
GLASS = {"c": "#D4E5F7", "e": "#B3CFEC", "q": "#8BADD0", "Q": "#5D8FC2", "a": "#D46D49", "b": "#A94725"}
MOTION = {"m": "#C2C2C2", "M": "#EAEAEA", "n": "#636262", "N": "#505050"}                     # Striche wie Speed
BOOT = {"x": "#240805", "y": "#3B0D0A", "z": "#4D1713", "u": "#662521", "v": "#8B3833", "w": "#AD524D"}


def icon(rows, *legends):
    legend = {}
    for part in legends:
        legend.update(part)
    size = len(rows)
    out = []
    for y, row in enumerate(rows):
        assert len(row) == size, (y, row, len(row), size)
        line = []
        for ch in row:
            if ch == ".":
                line.append((0, 0, 0, 0))
            else:
                h = legend[ch]
                alpha = int(h[7:9], 16) if len(h) == 9 else 255
                line.append((int(h[1:3], 16), int(h[3:5], 16), int(h[5:7], 16), alpha))
        out.append(line)
    return out


ICONS = {}


def canvas(size=16):
    return [["."] * size for _ in range(size)]


def rows_of(grid):
    return ["".join(r) for r in grid]


def box(grid, fx, fy, w, h, d, front, top, side):
    """Kiste in leichter Schrägansicht: Front w x h ab (fx, fy), Tiefe d nach rechts oben (45°).

    front(i, j), top(i, k) und side(k, j) liefern das Zeichen je Pixel (k = 1 … d, Tiefe), None lässt es frei."""
    def put(x, y, ch):
        if ch and 0 <= y < len(grid) and 0 <= x < len(grid[0]):
            grid[y][x] = ch
    for k in range(1, d + 1):
        for i in range(w):
            put(fx + i + k, fy - k, top(i, k))
        for j in range(h):
            put(fx + w - 1 + k, fy + j - k, side(k, j))
    for j in range(h):
        for i in range(w):
            put(fx + i, fy + j, front(i, j))


def outline(grid, color, inside=None):
    """Setzt die Randpixel der Silhouette (Nachbar transparent oder Bildrand) auf die Konturfarbe."""
    size = len(grid)
    edge = []
    for y in range(size):
        for x in range(size):
            if grid[y][x] == "." or (inside and grid[y][x] not in inside):
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if not (0 <= nx < size and 0 <= ny < size) or grid[ny][nx] == ".":
                    edge.append((x, y))
                    break
    for x, y in edge:
        grid[y][x] = color

# Board Slot: ein einzelner Aushang mit roter Nadel, wie am Bulletin Board.
PINNED_NOTE = [
    "................",
    ".......oo.......",
    "......oHro......",
    "......orRo......",
    "...ttttoottttt..",
    "...tppppJpppPt..",
    "...tpLLLLLLpPt..",
    "...tppppppppPt..",
    "...tpLLLLpppPt..",
    "...tppppppppPt..",
    "...tpLLLLLLpPt..",
    "...tppppppppPt..",
    "...tpLLLppPPTt..",
    "...tpppppPTtt...",
    "...tttttttt.....",
    "................",
]
PINNED_NOTE_LEGEND = {"o": "#891414", "H": "#F08080", "r": "#D32A2A", "R": "#A61E1E", "J": "#5A5A5A",
    "t": "#8C7A55", "p": "#F0E6C6", "P": "#D9CB9F", "T": "#BCA977", "L": "#A8977A"}
ICONS["pinned_note"] = icon(PINNED_NOTE, PINNED_NOTE_LEGEND)

# Market Information: Balkenwaage aus Gold.
ICONS["market_scale"] = icon([
    "................",
    ".......Hh.......",
    "..ohhhhhhgggggo.",
    "..o....gG....o..",
    ".o.o...gG...o.o.",
    ".o.o...gG...o.o.",
    "o...o..gG..o...o",
    "Hhhhhg.gG.Hhhhhg",
    ".ogGGo.gG..oGGo.",
    "..ooo..gG...ooo.",
    ".......gG.......",
    ".......gG.......",
    ".......gG.......",
    ".....HhhggG.....",
    "....ogGGGGGo....",
    "....oooooooo....",
], GOLD)

# Faster Price Recovery: Kursverlauf steigt, Pfeil nach rechts oben.
ICONS["price_trend"] = icon([
    "................",
    ".K.........llll.",
    ".K..........lLl.",
    ".K.........lLLl.",
    ".K........lLlLl.",
    ".K.......lLl.l..",
    ".K......lLl.....",
    ".K..l..lLl......",
    ".K.lLl.Ll.......",
    ".KlLlLlLl.......",
    ".KLl.lLl........",
    ".Kl...l.........",
    ".K..............",
    ".KKKKKKKKKKKKKK.",
    "................",
    "................",
], {"K": "#5A5A5A", "l": "#2E7A12", "L": "#7BD330"})

# Slower Price Decline: drei gestapelte Holzkisten in leichter Schrägansicht.
def crate_stack():
    g = canvas()

    def front(i, j):
        if i in (0, 5) or j in (0, 4):
            return "4"
        return "3" if j == 2 else ("1" if j == 1 and i < 3 else "2")

    def top(i, k):
        return "3" if i in (0, 5) or k == 2 else "1"

    def side(k, j):
        return "4" if j in (0, 4) or k == 2 else "3"

    for fx, fy in ((0, 9), (6, 9), (3, 4)):
        box(g, fx, fy, 6, 5, 2, front, top, side)
    outline(g, "5")
    return rows_of(g)


ICONS["crate_stack"] = icon(crate_stack(), OAK)

# Expedition Prep: drei überlappende Vanilla-Tränke (rot, blau, grün).
POTION = [
    "..ab..",
    "..cQ..",
    ".c.eQ.",
    "cXXXXQ",
    "cXWXXQ",
    "eXXXYQ",
    ".QQQQ.",
]


def potions():
    grid = [["."] * 16 for _ in range(16)]
    for (ox, oy, liquid) in ((1, 3, "r"), (9, 3, "g"), (5, 8, "B")):
        for dy, row in enumerate(POTION):
            for dx, ch in enumerate(row):
                if ch == ".":
                    continue
                if ch == "X":
                    ch = liquid
                elif ch == "Y":
                    ch = liquid.upper() if liquid != "B" else "D"
                grid[oy + dy][ox + dx] = ch
    return ["".join(r) for r in grid]


ICONS["expedition_potions"] = icon(potions(), GLASS, {
    "W": "#FFFFFF", "r": "#E0403A", "R": "#9A1F1C", "g": "#4FB13A", "G": "#2C6E1F", "B": "#3A6FD8", "D": "#22418C"})

# Durability: Vanilla-Eisenspitzhacke (18x18, nach oben links gerückt) mit hellgrünem Plus unten rechts über einem
# vollen Haltbarkeitsbalken wie im Inventar. Plus und Balken unterscheiden sie von Vein Mining (iron_pickaxe).
ICONS["durability"] = icon([
    ".....KKKKK........",
    "....KIijjiKef.....",
    ".....Kkkkjjhd.....",
    ".........eijk.....",
    "........efdjik....",
    ".......ehd.kjk....",
    "......efd..kjk....",
    ".....ehd...kik....",
    "....efd....kIk....",
    "...ehd......k.P...",
    "..efd........PpP..",
    ".ehd........PPpPP.",
    ".dd........PppWppP",
    "............PPpPP.",
    ".............PpP..",
    "..............P...",
    "..LLLLLLLLLLLLL...",
    "..bbbbbbbbbbbbb...",
], IRON, {"e": "#493615", "f": "#684E1E", "h": "#896727", "d": "#281E0B",
          "p": "#80FF20", "W": "#E2FFC4", "P": "#2E6B12", "L": "#00FF00", "b": "#000000"})

# Contract-Qualität: der Aushang von Contract-Stufe (pinned_note), 2 px nach links gerückt, mit dem hellgrünen Plus
# von Durability unten rechts. Das Plus unterscheidet ihn von Contract-Stufe.
PLUS_SMALL = [
    "...k...",
    "..kak..",
    ".kkakk.",
    "kaaWaak",
    ".kkakk.",
    "..kak..",
    "...k...",
]


def contract_quality():
    grid = [list(row[2:] + "..") for row in PINNED_NOTE]
    for dy, row in enumerate(PLUS_SMALL):
        for dx, ch in enumerate(row):
            if ch != ".":
                grid[9 + dy][9 + dx] = ch
    return rows_of(grid)


ICONS["contract_quality"] = icon(contract_quality(), PINNED_NOTE_LEGEND,
                                 {"a": "#80FF20", "W": "#E2FFC4", "k": "#2E6B12"})

# Fishing Treasure: kleine Piratentruhe in Schrägansicht, Deckel nach hinten offen, Goldhaufen; Eisenbänder statt
# Goldbeschläge, damit sie nicht wie Trade- oder Einzahlkiste wirkt.
def treasure_chest():
    g = canvas()
    fx, fy, w, h, d = 1, 9, 10, 5, 3
    for y in range(1, 6):                       # Deckel steht hinten offen (Innenseite sichtbar)
        for x in range(fx + d, fx + d + w):
            band = x in (fx + d + 2, fx + d + 7)
            g[y][x] = "J" if band else ("2" if y < 5 else "3")
        g[y][fx + d + w] = "4"

    def front(i, j):
        if i in (2, 7):
            return "j" if j else "J"
        if j == 0:
            return "1"
        if i in (4, 5) and j in (1, 2):
            return "K" if j == 1 else "k"
        return "2" if j < 3 else "3"

    def top(i, k):
        return "hgGhgHgGhg"[(i + 2 * k) % 10]

    def side(k, j):
        return "J" if j == 2 else "4"

    box(g, fx, fy, w, h, d, front, top, side)
    for x, y, ch in ((5, 5, "h"), (6, 5, "H"), (8, 5, "g"), (9, 4, "h"), (10, 5, "G")):   # Münzen über dem Rand
        g[y][x] = ch
    outline(g, "5")
    return rows_of(g)


ICONS["treasure_chest"] = icon(treasure_chest(), DARK, GOLD, IRON)

# Double Bobbers: zwei Vanilla-Schwimmer (weiß-rot-weiß) mit Haken, versetzt.
BOBBER = [
    "..k...",
    ".aab..",
    "aaaab.",
    "ccccd.",
    "ccccd.",
    "abbbb.",
    ".bbb..",
    "..k...",
    ".kk...",
]


def bobbers():
    grid = [["."] * 16 for _ in range(16)]
    for (ox, oy) in ((1, 1), (8, 5)):
        for dy, row in enumerate(BOBBER):
            for dx, ch in enumerate(row):
                if ch != ".":
                    grid[oy + dy][ox + dx] = ch
    for y in range(0, 1):
        grid[y][3] = "l"
    for y in range(0, 5):
        grid[y][10] = "l"
    return ["".join(r) for r in grid]


ICONS["double_bobbers"] = icon(bobbers(), {"a": "#F2F2F2", "b": "#919191", "c": "#D32A2A", "d": "#891414",
                                           "k": "#000000", "l": "#C8C8C8"})

# Hot Delivery: Sanduhr, Eichenrahmen, oberer und unterer Glaskolben gleich breit, Sand rinnt.
ICONS["hourglass"] = icon([
    "................",
    "................",
    "..5555555555....",
    "..5112111125....",
    "..5555555555....",
    "...4QeeeeQ4.....",
    "...4QyYyYQ4.....",
    "...4.QyYQ.4.....",
    "...4..QQ..4.....",
    "...4.Qy.Q.4.....",
    "...4Q.y..Q4.....",
    "...4QyYyYQ4.....",
    "..5555555555....",
    "..5112111125....",
    "..5555555555....",
    "................",
], OAK, GLASS, {"y": "#E8C766", "Y": "#C49A3A"})

# Remote Delivery: verschnürtes Paket in Schrägansicht in Bewegung. Die Kordel läuft über Front, Deckel und Seite
# und folgt dabei den Flächen.
def parcel():
    g = canvas()
    fx, fy, w, h, d = 5, 8, 8, 6, 3
    mid_i, mid_j, mid_k = w // 2, h // 2, 2

    def front(i, j):
        if i == mid_i or j == mid_j:
            return "w"
        return "3" if j == 0 else "2"           # dunkle Kante zum Deckel

    def top(i, k):
        if i == mid_i or k == mid_k:
            return "W" if (i == mid_i and k == mid_k) else "w"
        return "1"

    def side(k, j):
        if j == mid_j - k:                      # Kordel läuft auf der Seite schräg mit
            return "w"
        return "4" if k == 1 else "3"           # dunkle Kante zur Front

    box(g, fx, fy, w, h, d, front, top, side)
    outline(g, "t")
    for y, x0, line in ((7, 0, "NN.MM"), (10, 1, "nn.mM"), (13, 0, "NN.MM")):   # Striche wie beim Speed-Effekt
        for i, ch in enumerate(line):
            if ch != ".":
                g[y][x0 + i] = ch
    return rows_of(g)


ICONS["parcel"] = icon(parcel(), MOTION, {"t": "#5E4022", "1": "#D7B07A", "2": "#C49A63", "3": "#A57E4C",
                                         "4": "#8C6A3E", "w": "#E6D7A6", "W": "#BFA977"})

# Fast Growth: Ferkelkopf (Vanilla-Schwein) mit grünem Wachstumspfeil.
ICONS["fast_growth"] = icon([
    "................",
    "................",
    ".kkkkkkkkkk.....",
    ".kaabaabbak.....",
    ".kbbbaabaak.....",
    ".kccbbabcck.....",
    ".kdecffcedk.....",
    ".kcbhhhhbck.....",
    ".kfchiihcfk.....",
    ".kfchhhhcfkl....",
    ".kgffffffglLl...",
    ".kkkkkkkklLLLl..",
    "........lLLLLLl.",
    "..........LLL...",
    "..........LLL...",
    "..........lll...",
], {"k": "#8C4A47", "a": "#EEA5A4", "b": "#F19E98", "c": "#E6918B", "d": "#020001", "e": "#FFFFFF",
    "f": "#E68583", "g": "#E67973", "h": "#FBBEBE", "i": "#894746", "l": "#2E7A12", "L": "#7BD330"})

# --- 18x18, Effekt-Stil -------------------------------------------------------------------------------
# Dash: der Stiefel des Vanilla-Speed-Effekts ohne Striche, dahinter zwei verblassende Nachbilder — ein Satz nach
# vorn statt Dauerlauf (Rush trägt das Speed-Symbol selbst).
SPEED_BOOT = [
    "..................",
    "..................",
    "...........ccc....",
    "..........cddec...",
    ".........cheiiec..",
    ".....jcddechdiddc.",
    "....jdhiideheddec.",
    "....jihdidecheech.",
    ".....jdhdddehcch..",
    ".....jdhedecchh...",
    "......jeheech.....",
    "......jehceeehh...",
    "........eheeddeh..",
    ".......jehcdiddh..",
    ".......jcehhchhhh.",
    "........jcediddeh.",
    ".........jjjjjjj..",
    "..................",
]
BOOT_COLORS = {"c": "#4D1713", "d": "#8B3833", "e": "#662521", "h": "#3B0D0A", "i": "#AD524D", "j": "#240805"}


def dash():
    grid = [["."] * 18 for _ in range(18)]
    legend = {}
    for shift, alpha, tag in ((-6, "38", "1"), (-3, "70", "2"), (1, "", "")):
        for y, row in enumerate(SPEED_BOOT):
            for x, ch in enumerate(row):
                nx = x + shift
                if ch != "." and 0 <= nx < 18:
                    key = ch if not tag else {"1": "ABCDEF", "2": "GKLNOP"}[tag]["cdehij".index(ch)]
                    legend[key] = BOOT_COLORS[ch] + alpha
                    grid[y][nx] = key
    return ["".join(r) for r in grid], legend


_dash_rows, _dash_legend = dash()
ICONS["dash"] = icon(_dash_rows, _dash_legend)

# Double Jump: Spielerfigur (Vanilla-Farben) im Sprung, Arme hoch, darunter zwei Luftstöße – der erste noch
# kräftig, der zweite schon blass – und senkrechte Bewegungsstriche.
ICONS["double_jump"] = icon([
    "..................",
    "....s....hhhh.....",
    "....ss..hhhhh.....",
    ".....c..sEsEs..s..",
    ".....cc.sssss.ss..",
    "......cTTTTTTcc...",
    ".......TTTTTT.....",
    ".......TTTTTT.....",
    ".......PPPPPP.....",
    "..m....PPPPPP..m..",
    "..m...PPP..PPP.m..",
    "..m...BB....BB.m..",
    "..................",
    "....MMMMMMMMMM....",
    ".....nnnnnnnn.....",
    "..................",
    "...AAAAAAAAAAAA...",
    "....aaaaaaaaaa....",
], MOTION, {"h": "#3B2A14", "s": "#C69276", "E": "#3A2A6E", "c": "#00A0A0", "T": "#00AFAF",
            "P": "#3A3F9E", "B": "#5A5A5A", "A": "#EAEAEA80", "a": "#63626280"})

# Healing Pulse: drei grüne Kreuze.
PLUS = [
    "..kkk..",
    "..kLk..",
    "kkkLkkk",
    "kLLlLLk",
    "kkkLkkk",
    "..kLk..",
    "..kkk..",
]


def three_plus():
    grid = [["."] * 18 for _ in range(18)]
    for (ox, oy) in ((5, 1), (1, 9), (10, 9)):
        for dy, row in enumerate(PLUS):
            for dx, ch in enumerate(row):
                if ch != ".":
                    grid[oy + dy][ox + dx] = ch
    return ["".join(r) for r in grid]


ICONS["heal_pulse"] = icon(three_plus(), {"k": "#1C4A12", "L": "#5FC23A", "l": "#B6F28E"})

# Fishing Net: Netzbeutel aus Schnur (Farben der Vanilla-Schnur) an einem Seil (Farben der Vanilla-Leine), darin ein
# gefangener Kabeljau (Farben des Vanilla-Kabeljaus). Die Maschen laufen über den Fisch, er sitzt im Netz.
NET_ROPE = [
    "..................",
    "..................",
    ".qRrRrRrRrRrRrRrq.",
    ".qqRqRqRqRqRqRqqq.",
]
NET_BAG = [                          # Umriss des Beutels unter dem Seil: (links, rechts) je Zeile
    (2, 15), (2, 15), (2, 15), (2, 15), (3, 14), (3, 14), (3, 14), (4, 13), (4, 13), (5, 12), (6, 11), (7, 10),
]
SMALL_COD = [
    "...ddd....",
    ".ddacbd..e",
    "dfaaccbdee",
    "dabbbbbdee",
    ".dddddd..e",
]


def fishing_net():
    g = canvas(18)
    for y, row in enumerate(NET_ROPE):
        for x, ch in enumerate(row):
            if ch != ".":
                g[y][x] = ch
    top = len(NET_ROPE)
    for j, (left, right) in enumerate(NET_BAG):
        y = top + j
        last = j == len(NET_BAG) - 1
        for x in range(left, right + 1):
            rim = x in (left, right) or last
            if rim or (x + y) % 4 == 0 or (x - y) % 4 == 0:
                # Licht oben links: rechter Rand und Unterkante im Schattenton der Schnur, Maschen unten rechts grauer.
                g[y][x] = "u" if (last or x == right) else ("s" if x + y < 19 else "t")
    fx, fy = 4, top + 2
    for dy, row in enumerate(SMALL_COD):             # der Fisch liegt im Netz ...
        for dx, ch in enumerate(row):
            if ch != ".":
                g[fy + dy][fx + dx] = ch
    for dy, row in enumerate(SMALL_COD):             # ... und eine Maschenreihe läuft über ihn
        for dx, ch in enumerate(row):
            x, y = fx + dx, fy + dy
            if ch not in ".f" and (x - y) % 4 == 0 and 0 < dy < 4:
                g[y][x] = "t"
    return rows_of(g)


ICONS["fishing_net"] = icon(fishing_net(), {
    "q": "#5E2F10", "R": "#85461B", "r": "#AE734B",                             # Leine
    "s": "#F7F7F7", "t": "#DBDBDB", "u": "#2F4547",                             # Schnur
    "a": "#C6A271", "b": "#B18953", "c": "#D6C5AD", "d": "#986D4E", "e": "#6B442B", "f": "#000000"})   # Kabeljau


# --- Vorschau ------------------------------------------------------------------------------------------
def read_png(data):
    """Kleiner PNG-Leser für die Vanilla-Vergleichsbilder (8 Bit RGBA/RGB/Palette, auch Paletten < 8 Bit)."""
    i, idat, pal, trns = 8, b"", None, b""
    while i < len(data):
        n, = struct.unpack(">I", data[i:i + 4])
        t, c = data[i + 4:i + 8], data[i + 8:i + 8 + n]
        i += 12 + n
        if t == b"IHDR":
            w, h, bd, ct = struct.unpack(">IIBB", c[:10])
        elif t == b"PLTE":
            pal = [tuple(c[j:j + 3]) for j in range(0, len(c), 3)]
        elif t == b"tRNS":
            trns = c
        elif t == b"IDAT":
            idat += c
    ch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw, stride, bpp = zlib.decompress(idat), (w * ch * bd + 7) // 8, max(1, ch * bd // 8)
    prev, p, out = bytearray(stride), 0, []
    for _ in range(h):
        f, line = raw[p], bytearray(raw[p + 1:p + 1 + stride])
        p += 1 + stride
        for x in range(stride):
            a = line[x - bpp] if x >= bpp else 0
            b, c2 = prev[x], (prev[x - bpp] if x >= bpp else 0)
            if f == 1:
                line[x] = (line[x] + a) & 255
            elif f == 2:
                line[x] = (line[x] + b) & 255
            elif f == 3:
                line[x] = (line[x] + (a + b) // 2) & 255
            elif f == 4:
                pp = a + b - c2
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c2)
                line[x] = (line[x] + (a if pa <= pb and pa <= pc else b if pb <= pc else c2)) & 255
        prev, row = line, []
        for x in range(w):
            if ct == 3:
                v = (line[(x * bd) // 8] >> (8 - bd - (x * bd) % 8)) & ((1 << bd) - 1)
                row.append(pal[v] + ((trns[v] if v < len(trns) else 255),))
            elif ct == 6:
                row.append(tuple(line[x * 4:x * 4 + 4]))
            elif ct == 2:
                row.append(tuple(line[x * 3:x * 3 + 3]) + (255,))
        out.append(row)
    return out


def vanilla_refs():
    names = ["speed", "jump_boost", "resistance", "slow_falling", "regeneration", "water_breathing"]
    refs = {}
    if os.path.exists(SOURCES_JAR):
        with zipfile.ZipFile(SOURCES_JAR) as z:
            for n in names:
                refs["vanilla:" + n] = read_png(z.read(f"assets/minecraft/textures/mob_effect/{n}.png"))
    return refs


def blend(top, under):
    a = top[3] / 255
    return tuple(int(top[i] * a + under[i] * (1 - a)) for i in range(3)) + (255,)


def preview(path, k=3):
    """Jedes Icon auf einem Baumfeld (22x22, dunkel und gekauft) in 1x und k-fach, dahinter Vanilla-Effekte."""
    items = list(ICONS.items()) + list(vanilla_refs().items())
    node, gap = 22, 6
    cell = node * k + gap
    cols = 7
    rows = (len(items) + cols - 1) // cols
    W = cols * (cell + node * 2 + gap) + gap
    H = rows * (node * k + gap) + gap
    img = [[(198, 198, 198, 255)] * W for _ in range(H)]
    for idx, (name, px) in enumerate(items):
        cx = gap + (idx % cols) * (cell + node * 2 + gap)
        cy = gap + (idx // cols) * (node * k + gap)
        size = len(px)
        off = (node - size) // 2
        for bi, inner in enumerate(((0x20, 0x20, 0x20), (0x3A, 0x32, 0x20))):      # 1x auf beiden Hintergründen
            bx, by = cx + bi * (node + 2), cy
            for y in range(node):
                for x in range(node):
                    edge = x in (0, node - 1) or y in (0, node - 1)
                    img[by + y][bx + x] = (160, 160, 160, 255) if edge else inner + (255,)
            for y in range(size):
                for x in range(size):
                    if px[y][x][3]:
                        img[by + off + y][bx + off + x] = blend(px[y][x], img[by + off + y][bx + off + x])
        zx = cx + 2 * (node + 2) + gap                                                # vergrößert
        for y in range(node * k):
            for x in range(node * k):
                sx, sy = x // k - off, y // k - off
                inside = 0 <= sx < size and 0 <= sy < size and px[sy][sx][3]
                img[cy + y][zx + x] = blend(px[sy][sx], (0x20, 0x20, 0x20)) if inside else (0x20, 0x20, 0x20, 255)
    write_png(path, img)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    ap.add_argument("--out", default=OUT)
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)
    for name, px in ICONS.items():
        write_png(os.path.join(args.out, name + ".png"), px)
    print(f"{len(ICONS)} Icons nach {args.out}")
    if args.preview:
        preview(args.preview)
        print("Vorschau:", args.preview)


if __name__ == "__main__":
    main()
