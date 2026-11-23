#!/usr/bin/env python3
"""Erzeugt die Textur des Schwarzmarkthändlers (64x64, UV-Layout von VillagerModel / Wandering Trader).

Jede Würfelfläche des Modells ist ein eigenes, handgesetztes Raster und wird an ihre UV-Position geschrieben.
Die Box-UV-Regel von Minecraft (ModelPart.Cube): Würfel bei (u, v) mit Größe (b, h, t) belegt
oben (u+t, v), unten (u+t+b, v), rechts (u, v+t), vorn (u+t, v+t), links (u+t+b, v+t), hinten (u+2t+b, v+t).
"rechts/links" meint die Seite des Händlers. Auf der rechten Fläche liegt die Vorderkante in der letzten
Spalte, auf der linken in der ersten; auf der Oberseite ist die letzte Zeile die Vorderkante.

    python3 scripts/trader_texture.py                      # schreibt textures/entity/blackmarket_trader.png
    python3 scripts/trader_texture.py --preview X.png      # zusätzlich Ansichten vorn/hinten/seitlich/schräg
"""
import argparse
import math
import os

from png_writer import write_png

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src/main/resources/assets/dtp/textures/entity/blackmarket_trader.png")

PALETTE = {
    ".": None,
    # Robe: Schwarz bis Kohle, mit abgewetzten Lichtkanten
    "K": "#191616", "k": "#221E1E", "c": "#2C2727", "C": "#3B3535", "L": "#4A4343",
    # gedecktes Dunkelrot (wie die Schwarzmarkt-Markise)
    "R": "#74362F", "m": "#5E2B26", "M": "#45201C",
    # entsättigtes Violett (Futter, Untergewand)
    "p": "#4A3A50", "P": "#36293B",
    # Leder in Dunkeleiche
    "E": "#5B3E22", "d": "#4B331B", "D": "#3D2914", "e": "#2A1C0E",
    # Gold/Bronze (Taler-Töne)
    "h": "#E5B82D", "G": "#A98721", "o": "#7A5F18",
    # Eisen
    "i": "#BDBDBD", "j": "#8C8C8C", "J": "#5A5A5A",
    # Illager-Haut, etwas dunkler als Vanilla; Brauen, Augen (bernsteinbraune Iris)
    "s": "#8D9393", "S": "#7F8585", "t": "#6B7171", "T": "#575D5D", "u": "#454A4A", "n": "#4C5252",
    "b": "#363636", "B": "#222222", "w": "#E6E6E2", "a": "#5E4A1C",
}

# --- Kopf (0,0) 8x10x8 — liegt unter der Kapuze; nur das Gesicht ist sichtbar ------------------------
HEAD_FRONT = [
    "uuuuuuuu",
    "uTTTTTTu",
    "TTTTTTTT",
    "TtTTTTtT",   # Schatten der Kapuze über der Stirn
    "bbtSStbb",
    "BbbSSbbB",
    "twaSSawt",
    "TSssssST",
    "TSnnnnST",
    "uSnnnnSu",
]
HEAD_SIDE = ["uTTTTTTT"] + ["TTTTTTTT"] * 3 + ["TTTTTTtT"] * 3 + ["TTTTTTSS"] * 3
HEAD_BACK = ["uuuuuuuu"] + ["TTTTTTTT"] * 9
HEAD_CAP = ["TTTTTTTT"] * 8

# --- Nase (24,0) 2x4x2 ---------------------------------------------------------------------------------
NOSE_TOP = ["SS", "SS"]
NOSE_BOTTOM = ["nn", "nn"]
NOSE_FRONT = ["St", "St", "St", "TT"]
NOSE_RIGHT = ["TS", "TS", "TS", "uT"]
NOSE_LEFT = ["ST", "ST", "ST", "Tu"]
NOSE_BACK = ["TT", "TT", "TT", "TT"]

# --- Kapuze (32,0) 8x10x8, Hutschicht ------------------------------------------------------------------
HOOD_TOP = [
    "kkckkkck",
    "kcCcckck",
    "cCccCcck",
    "cCcCcckc",
    "cccCckcc",
    "cCcccCck",
    "cCccCcck",
    "CLCCLCCc",   # Vorderkante, Licht von oben
]
HOOD_FRONT = [
    "CLCCLCCc",
    "cCcccCck",
    "kmRmmRmk",   # Kapuzenkante: abgewetztes Dunkelrot, darunter fällt Schatten aufs Gesicht
    "k......k",
    "k......k",
    "c......k",
    "c......k",
    "k......k",
    "k......K",
    "kk....KK",
]
HOOD_RIGHT = [   # Spalte 7 = Vorderkante
    "kcccCccC",
    "kccCcCcC",
    "kcCccCcc",
    "kkCccCcc",
    "kkccCccc",
    "kkcCcckc",
    "Kkccckcc",
    "Kkkcckck",
    "KkkckkKk",
    "KKkkKkKK",
]
HOOD_LEFT = [r[::-1] for r in HOOD_RIGHT]
HOOD_BACK = [
    "kcccCcck",
    "kccCKcck",
    "kcCcKCck",
    "kcCcKcck",
    "kkcCKcck",
    "kkcCKckk",
    "KkccKckk",
    "KkcKKkkK",
    "KkkKKkKK",
    "KKkKKKKK",
]
HOOD_BOTTOM = ["........"] * 8   # offen: der Hals verschwindet im Schatten

# --- Körper (16,20) 8x12x6 — Untergewand, fast ganz von der Robe verdeckt ------------------------------
BODY = {"top": ["PPPPPPPP"] * 6, "bottom": ["PPPPPPPP"] * 6,
        "right": ["PPPPPP"] * 12, "front": ["PppPPppP"] * 12, "left": ["PPPPPP"] * 12, "back": ["PPPPPPPP"] * 12}

# --- Robe (0,38) 8x20x6, Jackenschicht -------------------------------------------------------------------
ROBE_TOP = [   # Zeile 0 = Rücken, Zeile 5 = Brust
    "kkkkkkkk",
    "kcccccck",
    "cCcccccc",
    "cCccccck",
    "cCccccck",
    "kMmGGmMk",
]
ROBE_FRONT = [
    "kMmGGmMk",   # Kragen mit Bronzespange
    "cCkpPkck",
    "cCcpPcck",
    "cCcpPcck",
    "cCcpPckk",
    "ccCpPcck",
    "ccCpPckk",
    "cccpPcck",
    "kcCpPcck",
    "dEdGhddD",   # Gürtel mit Schnalle
    "DddoGddD",
    "EdDpPcck",   # Beutel an der rechten Hüfte, Taler-Verschluss
    "dhDpPcck",
    "ddDpPckk",
    "eDepPcck",
    "cCcpPcck",
    "cCcpPckk",
    "ccCpPcck",
    "mRmpPmRm",   # Saum
    "M.MmP.mM",   # ausgefranst
]
ROBE_RIGHT = [   # Spalte 0 = Rücken, Spalte 5 = Vorderkante
    "kkcccM",
    "kcCccc",
    "kcCcCc",
    "kccCcc",
    "kkcCcc",
    "kkcCcC",
    "kkccCc",
    "kkcccC",
    "kkcCcc",
    "DddEdd",
    "DdddDd",
    "kkcCcc",
    "kkcCcc",
    "kkccCc",
    "kkccCc",
    "kkcccC",
    "kkcCcc",
    "kkcCck",
    "MmRmmR",
    "m.Mm.M",
]
ROBE_LEFT = [    # Spalte 0 = Vorderkante: verborgener Dolch am Gürtel
    "Mccckk",
    "cccCck",
    "cCcCck",
    "ccCcck",
    "ccCckk",
    "CcCckk",
    "cCcckk",
    "Ccickk",
    "ccDckk",
    "djjjdD",
    "ddedDD",
    "ccDEkk",
    "ccDEkk",
    "cCDEkk",
    "cCDekk",
    "ccGokk",
    "ccCckk",
    "kcCckk",
    "RmmRmM",
    "M.mM.m",
]
ROBE_BACK = [    # Kapuzenzipfel fällt auf den Rücken
    "kkcKKcck",
    "kcCKKcck",
    "kcCKKCck",
    "kccKKcck",
    "kcccKcck",
    "kcCcKcck",
    "kcCcccck",
    "kccCccck",
    "kkcCccck",
    "DdddEddD",
    "DddDdddD",
    "kkcKKcck",
    "kcCKKcck",
    "kcCKKcck",
    "kccKKCck",
    "kccKKcck",
    "kkcKKcck",
    "kkcKKckk",
    "mRmMmRmM",
    "M.mM.mM.",
]
ROBE_BOTTOM = ["........"] * 6

# --- Arme: Ärmel (44,22) 4x8x4, verschränkter Unterarm (40,38) 8x4x4 -----------------------------------
SLEEVE = {
    "top": ["kcck", "cCck", "cCck", "kcck"],
    "bottom": ["KkkK", "kKKk", "kKKk", "KkkK"],
    "right": ["kcck", "kCck", "cCck", "cCcc", "cCck", "ccCk", "kcCk", "kkck"],
    "front": ["cCcc", "cCck", "cCck", "ccCk", "ccCk", "cccC", "kccC", "kkcc"],
    "left": ["kcck", "kcCk", "kcCc", "kcCc", "kcck", "kCck", "kCck", "kkck"],
    "back": ["kkck", "kkck", "kcck", "kcck", "kkck", "kkck", "Kkck", "KkkK"],
}
FOREARMS = {
    "top": ["kcmSSmck", "cCmssmCc", "cCmssmcc", "ccmSSmcc"],
    "bottom": ["kkMTTMkk", "kkMTTMkk", "kkMuuMkk", "KkMuuMkK"],
    "front": ["cCRssRCc", "ccmSsmcc", "ccmStmcc", "kkMTTMkk"],
    "right": ["cCck", "cCck", "ccCk", "kkck"],
    "left": ["kcCc", "kcCc", "kCcc", "kckk"],
    "back": ["kkkkkkkk", "kkkkkkkk", "kkKKKKkk", "KKKKKKKK"],
}

# --- Beine (0,22) 4x12x4 — Hose in Kohle, Stiefel aus Dunkeleichen-Leder -------------------------------
LEG = {
    "top": ["kkkk"] * 4,
    "bottom": ["eDDe", "eDDe", "eDDe", "eeee"],
    "front": ["cCck", "cCck", "ccck", "cCck", "cCck", "ccck", "kcck", "EEdE", "dEdD", "djdD", "dEdD", "eeee"],
    "right": ["kcck", "kCck", "kcck", "kCcc", "kcck", "kcck", "kkck", "dEEE", "DdEd", "DddE", "DdEd", "eeee"],
    "left": ["kcck", "kcCk", "kcck", "cCck", "kcck", "kcck", "kckk", "EEEd", "dEdD", "EddD", "dEdD", "eeee"],
    "back": ["kkck", "kcck", "kkck", "kcck", "kkck", "kkck", "Kkck", "dddd", "DddD", "DdDD", "DddD", "eeee"],
}


def box(u, v, w, h, d, faces):
    """Ordnet die Flächen eines Würfels ihren UV-Positionen zu."""
    pos = {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
           "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h)}
    out = []
    for name, rows in faces.items():
        x, y, fw, fh = pos[name]
        assert len(rows) == fh and all(len(r) == fw for r in rows), (u, v, name, rows)
        out.append((x, y, rows))
    return out


def build():
    img = [[(0, 0, 0, 0)] * 64 for _ in range(64)]
    regions = []
    regions += box(0, 0, 8, 10, 8, {"top": HEAD_CAP, "bottom": HEAD_CAP, "right": HEAD_SIDE, "front": HEAD_FRONT,
                                    "left": [r[::-1] for r in HEAD_SIDE], "back": HEAD_BACK})
    regions += box(24, 0, 2, 4, 2, {"top": NOSE_TOP, "bottom": NOSE_BOTTOM, "right": NOSE_RIGHT, "front": NOSE_FRONT,
                                    "left": NOSE_LEFT, "back": NOSE_BACK})
    regions += box(32, 0, 8, 10, 8, {"top": HOOD_TOP, "bottom": HOOD_BOTTOM, "right": HOOD_RIGHT, "front": HOOD_FRONT,
                                     "left": HOOD_LEFT, "back": HOOD_BACK})
    regions += box(16, 20, 8, 12, 6, BODY)
    regions += box(0, 38, 8, 20, 6, {"top": ROBE_TOP, "bottom": ROBE_BOTTOM, "right": ROBE_RIGHT, "front": ROBE_FRONT,
                                     "left": ROBE_LEFT, "back": ROBE_BACK})
    regions += box(44, 22, 4, 8, 4, SLEEVE)
    regions += box(40, 38, 8, 4, 4, FOREARMS)
    regions += box(0, 22, 4, 12, 4, LEG)
    # Hutkrempe (30,47) bleibt transparent: Kapuze statt Hut.
    for x, y, rows in regions:
        for dy, r in enumerate(rows):
            for dx, ch in enumerate(r):
                col = PALETTE[ch]
                img[y + dy][x + dx] = (0, 0, 0, 0) if col is None else (
                    int(col[1:3], 16), int(col[3:5], 16), int(col[5:7], 16), 255)
    return img


# --- Vorschau: VillagerModel mit exakter Würfel-UV-Abbildung (wie ModelPart.Cube), ohne Backface-Culling --
def cube_polys(u, v, x, y, z, w, h, d, grow=0.0, mirror=False):
    x0, y0, z0, x1, y1, z1 = x - grow, y - grow, z - grow, x + w + grow, y + h + grow, z + d + grow
    if mirror:
        x0, x1 = x1, x0
    t0, t1, t2, t3 = (x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)
    l0, l1, l2, l3 = (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)
    U0, U1, U2, U22, U3, U4 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    V0, V1, V2 = v, v + d, v + d + h
    faces = [((l1, l0, t0, t1), U1, V0, U2, V1), ((t2, t3, l3, l2), U2, V1, U22, V0),
             ((t0, l0, l3, t3), U0, V1, U1, V2), ((t1, t0, t3, t2), U1, V1, U2, V2),
             ((l1, t1, t2, l2), U2, V1, U3, V2), ((l0, l1, l2, l3), U3, V1, U4, V2)]
    out = []
    for verts, a0, b0, a1, b1 in faces:
        uvs = [(a1, b0), (a0, b0), (a0, b1), (a1, b1)]
        out.append(list(zip(verts, uvs)))
    return out


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def rot_x(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]


def rot_y(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]


def model_polys():
    I = translate(0, 0, 0)
    arms = mat_mul(translate(0, 3, -1), rot_x(-0.75))
    hat_rim = rot_x(-math.pi / 2)
    parts = [
        (I, cube_polys(0, 0, -4, -10, -4, 8, 10, 8)),
        (I, cube_polys(32, 0, -4, -10, -4, 8, 10, 8, 0.51)),
        (hat_rim, cube_polys(30, 47, -8, -8, -6, 16, 16, 1)),
        (translate(0, -2, 0), cube_polys(24, 0, -1, -1, -6, 2, 4, 2)),
        (I, cube_polys(16, 20, -4, 0, -3, 8, 12, 6)),
        (I, cube_polys(0, 38, -4, 0, -3, 8, 20, 6, 0.5)),
        (arms, cube_polys(44, 22, -8, -2, -2, 4, 8, 4)),
        (arms, cube_polys(44, 22, 4, -2, -2, 4, 8, 4, mirror=True)),
        (arms, cube_polys(40, 38, -4, 2, -2, 8, 4, 4)),
        (translate(-2, 12, 0), cube_polys(0, 22, -2, 0, -2, 4, 12, 4)),
        (translate(2, 12, 0), cube_polys(0, 22, -2, 0, -2, 4, 12, 4, mirror=True)),
    ]
    out = []
    for m, polys in parts:
        for poly in polys:
            out.append([((m[0][0] * p[0] + m[0][1] * p[1] + m[0][2] * p[2] + m[0][3],
                          m[1][0] * p[0] + m[1][1] * p[1] + m[1][2] * p[2] + m[1][3],
                          m[2][0] * p[0] + m[2][1] * p[1] + m[2][2] * p[2] + m[2][3]), uv) for p, uv in poly])
    return out


def render(tex, yaw, pitch=0.0, S=10):
    """Orthografisch; Modellraum: y nach unten, Front zeigt nach -z. yaw=0 ist die Frontansicht."""
    view = mat_mul(rot_x(-pitch), rot_y(yaw))
    W, H = 22 * S, 38 * S
    img = [[None] * W for _ in range(H)]
    zb = [[1e9] * W for _ in range(H)]
    light = (-0.35, -0.75, -0.55)
    ln = math.sqrt(sum(c * c for c in light))
    light = tuple(c / ln for c in light)
    for poly in model_polys():
        pts = []
        for (x, y, z), uv in poly:
            vx = view[0][0] * x + view[0][1] * y + view[0][2] * z
            vy = view[1][0] * x + view[1][1] * y + view[1][2] * z
            vz = view[2][0] * x + view[2][1] * y + view[2][2] * z
            pts.append(((W / 2 + vx * S, H / 2 + (vy - 6.5) * S, vz), uv))
        a, b, c = [p[0] for p in pts[:3]]
        e1 = (b[0] - a[0], b[1] - a[1], (b[2] - a[2]) * S)
        e2 = (c[0] - a[0], c[1] - a[1], (c[2] - a[2]) * S)
        n = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0])
        nl = math.sqrt(sum(k * k for k in n)) or 1
        n = tuple(k / nl for k in n)
        if n[2] > 0:
            n = tuple(-k for k in n)
        lum = 0.45 + 0.55 * max(0.0, sum(n[i] * light[i] for i in range(3)))
        umin = min(p[1][0] for p in pts)
        vmin = min(p[1][1] for p in pts)
        umax = max(p[1][0] for p in pts)
        vmax = max(p[1][1] for p in pts)
        for tri in ((0, 1, 2), (0, 2, 3)):
            (p0, t0), (p1, t1), (p2, t2) = [pts[i] for i in tri]
            den = (p1[1] - p2[1]) * (p0[0] - p2[0]) + (p2[0] - p1[0]) * (p0[1] - p2[1])
            if abs(den) < 1e-9:
                continue
            for py in range(max(0, int(min(p0[1], p1[1], p2[1]))), min(H, int(max(p0[1], p1[1], p2[1])) + 1)):
                for px in range(max(0, int(min(p0[0], p1[0], p2[0]))), min(W, int(max(p0[0], p1[0], p2[0])) + 1)):
                    X, Y = px + 0.5, py + 0.5
                    w0 = ((p1[1] - p2[1]) * (X - p2[0]) + (p2[0] - p1[0]) * (Y - p2[1])) / den
                    w1 = ((p2[1] - p0[1]) * (X - p2[0]) + (p0[0] - p2[0]) * (Y - p2[1])) / den
                    w2 = 1 - w0 - w1
                    if w0 < -1e-6 or w1 < -1e-6 or w2 < -1e-6:
                        continue
                    z = w0 * p0[2] + w1 * p1[2] + w2 * p2[2]
                    if z >= zb[py][px]:
                        continue
                    u = w0 * t0[0] + w1 * t1[0] + w2 * t2[0]
                    v = w0 * t0[1] + w1 * t1[1] + w2 * t2[1]
                    tu = int(min(max(u, umin), umax - 1e-4))
                    tv = int(min(max(v, vmin), vmax - 1e-4))
                    r, g, bl, al = tex[tv][tu]
                    if al == 0:
                        continue
                    zb[py][px] = z
                    img[py][px] = (int(r * lum), int(g * lum), int(bl * lum), 255)
    return img


def preview(tex, path):
    bg = (60, 64, 58, 255)
    views = [render(tex, 0.0), render(tex, math.pi / 2), render(tex, math.pi),
             render(tex, -0.6, 0.18)]
    k = 6
    sheet_tex = [[None] * (64 * k) for _ in range(64 * k)]
    for y in range(64 * k):
        for x in range(64 * k):
            r, g, b, a = tex[y // k][x // k]
            chk = 96 if ((x // k) + (y // k)) % 2 else 80
            sheet_tex[y][x] = (r, g, b, 255) if a else (chk, chk, chk, 255)
    gap = 16
    W = sum(len(v[0]) for v in views) + gap * (len(views) + 1) + 64 * k + gap
    H = max(len(views[0]), 64 * k) + 2 * gap
    out = [[bg] * W for _ in range(H)]
    x0 = gap
    for v in views + [sheet_tex]:
        for y, row in enumerate(v):
            for x, p in enumerate(row):
                if p:
                    out[gap + y][x0 + x] = p
        x0 += len(v[0]) + gap
    write_png(path, out)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview")
    ap.add_argument("--out", default=OUT)
    args = ap.parse_args()
    tex = build()
    write_png(args.out, tex)
    print("Textur:", args.out)
    if args.preview:
        preview(tex, args.preview)
        print("Vorschau:", args.preview)


if __name__ == "__main__":
    main()
