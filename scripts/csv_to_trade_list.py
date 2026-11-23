#!/usr/bin/env python3
"""Schreibt die Handelsliste (data/dtp/dtp/trade_list/*.json) aus der bearbeiteten CSV-Datei neu.

    python3 scripts/csv_to_trade_list.py [quelle.csv]   # Standard: scripts/trade_list.csv

Die Spalte "file" bestimmt die Zieldatei; Reihenfolge der Zeilen = Reihenfolge in der Datei.
Jede in der CSV genannte Datei wird komplett überschrieben (neue Dateinamen legen neue Dateien an).
JSON-Dateien ohne Zeile in der CSV bleiben unverändert, es gibt nur eine Warnung.
Leere Zellen lassen das Feld weg. Zahlen (base, h, t) dürfen auch mit Dezimalkomma stehen.
"key" (optional) fasst Items zu einer gemeinsamen Ware zusammen; H und T gelten dann vom ersten Eintrag.
"components" und "predicates" (optional) sind JSON-Objekte im Vanilla-Format, z. B. für das Böse Banner.
"""
import csv
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TRADE_LIST = os.path.join(ROOT, "src/main/resources/data/dtp/dtp/trade_list")
DEFAULT_CSV = os.path.join(ROOT, "scripts/trade_list.csv")
FIELD_ORDER = ["item", "tag", "key", "base", "h", "t", "components", "predicates"]  # Reihenfolge im JSON, unabhängig von der CSV
WIDTHS = {"item": 59, "tag": 59, "key": 35, "base": 17, "h": 13}  # Spaltenbreiten wie in den bestehenden Dateien
NUMBER = re.compile(r"^-?\d+([.,]\d+)?$")
IDENTIFIER = re.compile(r"^[a-z0-9_.-]+:[a-z0-9_./-]+$")


def parse(value):
    value = value.strip()
    if not NUMBER.match(value):
        return value
    value = value.replace(",", ".")
    return float(value) if "." in value else int(value)


def format_entry(entry):
    parts = [f"{json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in entry.items()]
    keys = list(entry)
    cells = [(p + ",").ljust(WIDTHS.get(k, 0) - 1) + " " for k, p in zip(keys[:-1], parts[:-1])]
    return "    { " + "".join(cells) + parts[-1] + " }"


def main():
    source = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CSV
    files = {}
    with open(source, encoding="utf-8-sig", newline="") as f:
        reader = csv.DictReader(f)
        extra = [c for c in reader.fieldnames or [] if c and c.strip() not in FIELD_ORDER + ["file"]]
        if extra:
            print(f"Hinweis: Hilfsspalten {extra} werden ignoriert")
        if "key" not in [c.strip() for c in reader.fieldnames or []]:
            print("Warnung: Spalte 'key' fehlt — gemeinsame Waren (z. B. dtp:pottery_sherd) gehen verloren. "
                  "CSV neu mit scripts/trade_list_to_csv.py erzeugen?")
        for line, row in enumerate(reader, start=2):
            name = (row.pop("file") or "").strip()
            if not name:
                sys.exit(f"{source}:{line}: Spalte 'file' ist leer")
            if not name.endswith(".json") or os.sep in name:
                sys.exit(f"{source}:{line}: ungültiger Dateiname '{name}'")
            raw = {k.strip(): v for k, v in row.items()
                   if k and k.strip() in FIELD_ORDER and v is not None and v.strip()}
            entry = {}
            for k, v in raw.items():
                if k in ("base", "h", "t"):
                    entry[k] = parse(v)
                elif k in ("components", "predicates"):
                    try:
                        entry[k] = json.loads(v)
                    except json.JSONDecodeError as err:
                        sys.exit(f"{source}:{line}: Spalte '{k}' ist kein gültiges JSON: {err}")
                    if not isinstance(entry[k], dict):
                        sys.exit(f"{source}:{line}: Spalte '{k}' muss ein JSON-Objekt sein")
                else:
                    entry[k] = v.strip()
            entry = {k: entry[k] for k in sorted(entry, key=FIELD_ORDER.index)}
            if ("item" in entry) == ("tag" in entry):
                sys.exit(f"{source}:{line}: genau eine der Spalten 'item' oder 'tag' muss gefüllt sein")
            if not isinstance(entry.get("base"), (int, float)):
                sys.exit(f"{source}:{line}: Spalte 'base' muss eine Zahl sein")
            for key in ("h", "t"):
                if key in entry and not isinstance(entry[key], (int, float)):
                    sys.exit(f"{source}:{line}: Spalte '{key}' muss eine Zahl sein")
            for key in ("item", "tag", "key"):
                if key in entry and not IDENTIFIER.match(entry[key]):
                    sys.exit(f"{source}:{line}: Spalte '{key}' ist keine gültige ID (z. B. 'minecraft:stone'): '{entry[key]}'")
            files.setdefault(name, []).append(entry)

    for name, entries in files.items():
        seen = set()
        for e in entries:
            key = e.get("item") or "#" + e["tag"]
            if key in seen:
                print(f"Warnung: {name}: {key} kommt mehrfach vor")
            seen.add(key)

    # Gemeinsame Ware: H und T gelten vom ersten Eintrag des Keys (Dateien nach Namen sortiert, wie im Spiel).
    first = {}
    for name in sorted(files):
        for e in files[name]:
            if "key" not in e:
                continue
            ref = first.setdefault(e["key"], (name, e))
            if (e.get("h"), e.get("t")) != (ref[1].get("h"), ref[1].get("t")):
                print(f"Warnung: {name}: {e.get('item') or '#' + e['tag']} hat andere h/t als der erste Eintrag "
                      f"von {e['key']} ({ref[0]}) — im Spiel gelten die des ersten")

    for name, entries in files.items():
        body =",\n".join(format_entry(e) for e in entries)
        with open(os.path.join(TRADE_LIST, name), "w", encoding="utf-8") as f:
            f.write('{\n  "entries": [\n' + body + "\n  ]\n}\n")
        print(f"{name}: {len(entries)} Einträge")

    for path in sorted(glob.glob(os.path.join(TRADE_LIST, "*.json"))):
        if os.path.basename(path) not in files:
            print(f"Warnung: {os.path.basename(path)} kommt in der CSV nicht vor und wurde nicht verändert")


if __name__ == "__main__":
    main()
