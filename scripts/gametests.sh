#!/usr/bin/env bash
# Führt alle GameTests aus dev.dtp.gametest.DtpGameTests aus (./gradlew runGameTestServer) und fasst das Ergebnis zusammen.
#
#   scripts/gametests.sh                 # aus jedem Verzeichnis aufrufbar
#   scripts/gametests.sh --offline       # weitere Argumente gehen unverändert an Gradle
#
# Vorher prüft scripts/check_assets.py die Client-Ressourcen. Das vollständige Log liegt danach in
# build/gametests.log. Exit-Code 0, wenn alle Pflicht-Tests bestanden sind und die Assets fehlerfrei sind.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG="$ROOT/build/gametests.log"
mkdir -p "$ROOT/build"
cd "$ROOT"

# Client-Ressourcen lädt der Testserver nicht; die prüft vorab scripts/check_assets.py.
assets=0
python3 "$ROOT/scripts/check_assets.py" || assets=$?

status=0
./gradlew runGameTestServer "$@" 2>&1 | tee "$LOG" || status=$?

echo
echo "=== Zusammenfassung (vollständiges Log: build/gametests.log) ==="
# Fehlgeschlagene Tests und die Abschlusszeile des Testservers herausziehen.
grep -E -i "failed|tests? passed|required tests" "$LOG" | grep -v -i "^\s*at " | tail -n 40 || true

# Hinweise des JSON-Prüflaufs (explizite Standardwerte) nur zählen, nicht einzeln zeigen.
grep -E "Data file check: [0-9]+ files" "$LOG" | sed 's/.*Data file check: /Daten-Prüflauf: /' || true

if [ "$status" -eq 0 ]; then
    echo "GameTests: alle bestanden."
else
    echo "GameTests: FEHLGESCHLAGEN (Exit-Code $status)." >&2
fi
if [ "$assets" -ne 0 ]; then
    echo "Assets: FEHLER, siehe Ausgabe von check_assets.py oben." >&2
    [ "$status" -eq 0 ] && status=$assets
fi
exit "$status"
