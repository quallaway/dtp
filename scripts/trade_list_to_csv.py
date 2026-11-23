#!/usr/bin/env python3
"""Exportiert die Handelsliste (data/dtp/dtp/trade_list/*.json) in eine CSV-Datei zum Bearbeiten.

    python3 scripts/trade_list_to_csv.py [ziel.csv]     # Standard: scripts/trade_list.csv

Jede Zeile ist ein Eintrag; die Spalte "file" nennt die JSON-Datei, aus der er stammt.
Die Spalte "key" (optional) fasst Items zu einer gemeinsamen Ware zusammen (gemeinsamer Marktdruck,
H und T vom ersten Eintrag des Keys). "components" und "predicates" stehen als JSON in der Zelle.
Zurückschreiben mit scripts/csv_to_trade_list.py.
"""
import csv
import glob
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TRADE_LIST = os.path.join(ROOT, "src/main/resources/data/dtp/dtp/trade_list")
DEFAULT_CSV = os.path.join(ROOT, "scripts/trade_list.csv")


def main():
    target = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_CSV
    rows, columns = [], ["file", "item", "tag", "key", "base", "h", "t"]  # Felder wie TradeList.RawEntry
    for path in sorted(glob.glob(os.path.join(TRADE_LIST, "*.json"))):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        if set(data) != {"entries"}:
            sys.exit(f"{path}: unerwartete Felder {sorted(data)}, nur 'entries' wird unterstützt")
        for entry in data["entries"]:
            for key in entry:
                if key not in columns:
                    columns.append(key)
            rows.append({"file": os.path.basename(path),
                         **{k: json.dumps(v, ensure_ascii=False) if isinstance(v, (dict, list)) else v for k, v in entry.items()}})

    with open(target, "w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=columns)
        writer.writeheader()
        writer.writerows(rows)
    print(f"{len(rows)} Einträge nach {os.path.relpath(target, ROOT)} geschrieben")


if __name__ == "__main__":
    main()
