#!/usr/bin/env python3
"""Prüft die Client-Ressourcen unter src/main/resources/assets auf kaputte Verweise.

Der GameTest-Server lädt keine Client-Ressourcen; diese Prüfung ergänzt den GameTest data_files_valid:

- jede JSON-Datei ist gültiges JSON,
- Item-Definitionen (items/*.json) und Blockstates verweisen nur auf vorhandene Modelle,
- Modelle verweisen nur auf vorhandene Parents und Texturen; Texturvariablen (#name) sind definiert,
- Upgrade-Symbole ({"sprite": ...} in data/dtp/dtp/upgrade und player_upgrade) haben ein Bild,
- de_de.json und en_us.json haben dieselben Schlüssel, und jede Item-Definition hat einen Namen.

Geprüft werden nur Verweise in Namespaces, die unter assets/ liegen; Vanilla und andere Mods nicht. Nicht verwendete Modelle und Texturen
erscheinen nur als Hinweis. Exit-Code 1 bei Fehlern, sonst 0.

    python3 scripts/check_assets.py
"""
import json
import sys
from pathlib import Path

RESOURCES = Path(__file__).resolve().parent.parent / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets"
DATA = RESOURCES / "data"

errors: list[str] = []
hints: list[str] = []
used_models: set[Path] = set()
used_textures: set[Path] = set()


def rel(path: Path) -> str:
    return str(path.relative_to(RESOURCES))


def load(path: Path):
    try:
        with path.open(encoding="utf-8") as f:
            return json.load(f)
    except (json.JSONDecodeError, UnicodeDecodeError) as e:
        errors.append(f"{rel(path)}: kein gültiges JSON ({e})")
        return None


def split_id(ref: str) -> tuple[str, str]:
    ns, _, path = ref.rpartition(":")
    return (ns or "minecraft"), path


def own(ns: str) -> bool:
    """Nur eigene Namespaces prüfen; Verweise auf Vanilla oder andere Mods liegen nicht in diesem Projekt."""
    return (ASSETS / ns).is_dir() and ns != "minecraft"


def require_model(source: Path, ref: str) -> None:
    ns, path = split_id(ref)
    if not own(ns) or path.startswith("builtin/"):
        return
    target = ASSETS / ns / "models" / f"{path}.json"
    used_models.add(target)
    if not target.is_file():
        errors.append(f"{rel(source)}: Modell {ref} fehlt ({rel(target)})")


def require_texture(source: Path, ref: str, folder: str = "textures") -> None:
    ns, path = split_id(ref)
    if not own(ns):
        return
    target = ASSETS / ns / folder / f"{path}.png"
    used_textures.add(target)
    if not target.is_file():
        errors.append(f"{rel(source)}: Textur {ref} fehlt ({rel(target)})")


def walk_models(source: Path, node) -> None:
    """Item-Definitionen: jedes Objekt mit "model" (Text) oder "base" (Spezialmodelle) ist ein Modellverweis."""
    if isinstance(node, dict):
        for key in ("model", "base"):
            if isinstance(node.get(key), str):
                require_model(source, node[key])
        for value in node.values():
            walk_models(source, value)
    elif isinstance(node, list):
        for value in node:
            walk_models(source, value)


def check_blockstate(path: Path, data: dict) -> None:
    variants = list((data.get("variants") or {}).values())
    variants += [part.get("apply") for part in data.get("multipart") or []]
    for variant in variants:
        for v in variant if isinstance(variant, list) else [variant]:
            if isinstance(v, dict) and isinstance(v.get("model"), str):
                require_model(path, v["model"])
            else:
                errors.append(f"{rel(path)}: Variante ohne Modell")


def check_model(path: Path, data: dict) -> None:
    if isinstance(data.get("parent"), str):
        require_model(path, data["parent"])
    textures = data.get("textures") or {}
    for name, ref in textures.items():
        if not isinstance(ref, str):
            continue
        if ref.startswith("#"):
            if ref[1:] not in textures and "parent" not in data:
                errors.append(f"{rel(path)}: Texturvariable {name} zeigt auf undefiniertes {ref}")
        else:
            require_texture(path, ref)
    # Flächen dürfen nur Variablen nutzen, die das Modell definiert (mit Parent kann sie auch dort stehen).
    if "parent" not in data:
        for element in data.get("elements") or []:
            for face, spec in (element.get("faces") or {}).items():
                ref = spec.get("texture", "")
                if ref.startswith("#") and ref[1:] not in textures:
                    errors.append(f"{rel(path)}: Fläche {face} nutzt undefiniertes {ref}")


def check_upgrade_icons() -> None:
    for folder in ("upgrade", "player_upgrade"):
        for path in sorted((DATA / "dtp" / "dtp" / folder).glob("*.json")):
            data = load(path)
            if not isinstance(data, dict):
                continue
            icons = [data.get("icon")] + list(data.get("level_icons") or [])
            for icon in icons:
                if isinstance(icon, dict) and isinstance(icon.get("sprite"), str):
                    ns, sprite = split_id(icon["sprite"])
                    # Der GUI-Atlas sammelt textures/gui/sprites/** und textures/mob_effect/** (Präfix mob_effect/).
                    if sprite.startswith("mob_effect/"):
                        require_texture(path, f"{ns}:{sprite}")
                    else:
                        require_texture(path, f"{ns}:{sprite}", "textures/gui/sprites")


def check_lang(namespace_dir: Path) -> None:
    lang_dir = namespace_dir / "lang"
    files = {p.stem: load(p) for p in sorted(lang_dir.glob("*.json"))}
    files = {k: v for k, v in files.items() if isinstance(v, dict)}
    if len(files) < 2:
        return
    all_keys = set().union(*(set(v) for v in files.values()))
    for lang, data in files.items():
        missing = sorted(all_keys - set(data))
        for key in missing:
            errors.append(f"{rel(lang_dir / (lang + '.json'))}: Schlüssel {key} fehlt")
    ns = namespace_dir.name
    for item in sorted((namespace_dir / "items").glob("*.json")):
        name = item.stem
        if not any(f"{kind}.{ns}.{name}" in data for data in files.values() for kind in ("item", "block")):
            errors.append(f"{rel(item)}: kein Name in lang (item.{ns}.{name} oder block.{ns}.{name})")


def main() -> int:
    for ns_dir in sorted(p for p in ASSETS.iterdir() if p.is_dir()):
        for path in sorted(ns_dir.rglob("*.json")):
            data = load(path)
            if data is None:
                continue
            kind = path.relative_to(ns_dir).parts[0]
            if kind == "items":
                walk_models(path, data)
            elif kind == "blockstates":
                check_blockstate(path, data)
            elif kind == "models":
                check_model(path, data)
        check_lang(ns_dir)
    check_upgrade_icons()

    for ns_dir in sorted(p for p in ASSETS.iterdir() if p.is_dir()):
        for path in sorted((ns_dir / "models").rglob("*.json")):
            if path not in used_models:
                hints.append(f"{rel(path)}: wird von keinem Item, Blockstate oder Modell benutzt")
        for sub in ("block", "item"):
            for path in sorted((ns_dir / "textures" / sub).rglob("*.png")):
                if path not in used_textures:
                    hints.append(f"{rel(path)}: wird von keinem Modell benutzt")

    for hint in hints:
        print(f"Hinweis: {hint}")
    for error in errors:
        print(f"Fehler:  {error}")
    print(f"Assets: {len(errors)} Fehler, {len(hints)} Hinweis(e).")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
