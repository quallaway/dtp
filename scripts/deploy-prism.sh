#!/usr/bin/env bash
# Baut den Mod und kopiert die Jar in den mods-Ordner einer Prism-Launcher-Instanz.
#
#   scripts/deploy-prism.sh                     # Standard: Instanz "DTP 26.3"
#   scripts/deploy-prism.sh "Andere Instanz"    # Name genau wie in Prism angezeigt
#   scripts/deploy-prism.sh --keep-config       # dtp-synced.toml der Instanz und ihrer Welten behalten
#   PRISM_DIR=/anderer/pfad scripts/deploy-prism.sh
#
# Standardmäßig wird config/dtp-synced.toml der Instanz entfernt (gesichert als dtp-synced.toml.last),
# damit NeoForge sie beim nächsten Start mit den aktuellen Defaults aus DtpConfig.java neu anlegt.
# Eine bestehende Datei behält sonst ihre Werte — geänderte Defaults kämen nie in der Instanz an.
# Ebenso gesichert werden Überschreibungen in saves/<Welt>/syncedconfig/dtp-synced.toml: Unter NeoForge 26.3
# hat eine Datei dort Vorrang vor config/ und würde die neuen Defaults sonst wieder verdecken.
#
# Die Instanz wird über ihren exakten Namen in instance.cfg gefunden, nie über die Minecraft-Version.
# Ältere dtp-*.jar in der Instanz werden ersetzt. Läuft im Terminal des Rechners und in der
# VSCodium-Flatpak-Sandbox (dort wird der Kopierschritt über flatpak-spawn auf dem Rechner ausgeführt,
# weil Flatpak die Daten anderer Flatpak-Apps ausblendet).
set -euo pipefail

RESET_CONFIG=1
INSTANCE="DTP 26.3"
for arg in "$@"; do
    case "$arg" in
        --keep-config) RESET_CONFIG=0 ;;
        -*) echo "Unbekannte Option: $arg" >&2; exit 1 ;;
        *) INSTANCE="$arg" ;;
    esac
done

PROJECT="$(cd "$(dirname "$0")/.." && pwd)"
PRISM_DIR="${PRISM_DIR:-$HOME/.var/app/org.prismlauncher.PrismLauncher/data/PrismLauncher}"

host() {
    if [[ -f /.flatpak-info ]]; then
        flatpak-spawn --host "$@"
    else
        "$@"
    fi
}

echo "==> Baue Mod"
(cd "$PROJECT" && ./gradlew --quiet build)

JAR="$(ls -t "$PROJECT"/build/libs/dtp-*.jar | grep -v -- '-sources' | head -n1)"
# Prism legt Ordnernamen nicht immer gleich dem angezeigten Namen an — deshalb über instance.cfg suchen.
# Der Platzhalter muss auf dem Rechner aufgelöst werden: In der Sandbox ist der Prism-Ordner unsichtbar.
mapfile -t MATCHES < <(host sh -c 'grep -lFx "name=$2" -- "$1"/instances/*/instance.cfg' sh "$PRISM_DIR" "$INSTANCE" 2>/dev/null || true)
if [[ ${#MATCHES[@]} -eq 0 ]]; then
    echo "Keine Prism-Instanz mit dem Namen \"$INSTANCE\" gefunden. Vorhandene Instanzen:" >&2
    host sh -c 'grep -h "^name=" "$1"/instances/*/instance.cfg | cut -d= -f2-' sh "$PRISM_DIR" >&2 || true
    exit 1
fi
if [[ ${#MATCHES[@]} -gt 1 ]]; then
    echo "Mehrere Instanzen heißen \"$INSTANCE\" — bitte in Prism eindeutig umbenennen:" >&2
    printf '  %s\n' "${MATCHES[@]%/instance.cfg}" >&2
    exit 1
fi
INSTANCE_DIR="${MATCHES[0]%/instance.cfg}"

# Prism nutzt je nach Version "minecraft" oder ".minecraft" als Spielordner.
GAME_DIR=""
for candidate in "$INSTANCE_DIR/minecraft" "$INSTANCE_DIR/.minecraft"; do
    if host test -d "$candidate"; then
        GAME_DIR="$candidate"
        break
    fi
done
if [[ -z "$GAME_DIR" ]]; then
    echo "Instanz \"$INSTANCE\" hat noch keinen Spielordner: $INSTANCE_DIR" >&2
    echo "Hinweis: Die Instanz einmal in Prism starten, damit der Spielordner angelegt wird." >&2
    exit 1
fi

echo "==> Kopiere $(basename "$JAR") nach $GAME_DIR/mods"
host mkdir -p "$GAME_DIR/mods"
host find "$GAME_DIR/mods" -maxdepth 1 -name 'dtp-*.jar' -delete
host cp "$JAR" "$GAME_DIR/mods/"

if [[ $RESET_CONFIG -eq 1 ]]; then
    mapfile -t CONFIGS < <(host sh -c 'ls -1 -- "$1"/config/dtp-synced.toml "$1"/saves/*/syncedconfig/dtp-synced.toml' sh "$GAME_DIR" 2>/dev/null || true)
    for config in "${CONFIGS[@]}"; do
        echo "==> Setze $config zurück (Sicherung: dtp-synced.toml.last)"
        host mv -f "$config" "$config.last"
    done
else
    echo "==> Behalte dtp-synced.toml"
fi
echo "==> Fertig. Instanz in Prism starten."
