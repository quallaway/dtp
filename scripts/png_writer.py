"""Gemeinsamer PNG-Schreiber der Textur-Generatoren (block_textures, item_textures, upgrade_icons, trader_texture).

Kein eigenes Skript, nur zum Importieren. Ohne Abhängigkeiten außer Python 3.
"""
import struct
import zlib


def write_png(path, px):
    """Schreibt px (Zeilen aus (r, g, b, a)-Tupeln, 8 Bit je Kanal) als RGBA-PNG nach path."""
    h, w = len(px), len(px[0])
    raw = b"".join(b"\x00" + bytes(c for p in row for c in p) for row in px)

    def chunk(t, c):
        return struct.pack(">I", len(c)) + t + c + struct.pack(">I", zlib.crc32(t + c) & 0xFFFFFFFF)

    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
