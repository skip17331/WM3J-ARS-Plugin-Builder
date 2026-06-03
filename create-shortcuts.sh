#!/usr/bin/env bash
#
# ARS Plugin Builder — create a Desktop icon AND an application-menu entry (Linux).
#
# Lighter than install.sh: it does not copy/build the app, it just drops launcher
# shortcuts for whatever is already here. It finds the app in this order:
#   1. the installed launcher   ~/.local/bin/ars-plugin-builder   (from install.sh)
#   2. the installed jar        ~/.local/share/ars-plugin-builder/ars-plugin-builder.jar
#   3. the freshly built jar     ./target/ars-plugin-builder-*.jar
# It writes a .desktop file to the application menu and to your Desktop folder
# (XDG-aware), installs the SVG icon, and marks the Desktop file trusted so
# GNOME shows it as a launchable icon rather than a text file.
#
# Usage:
#   ./create-shortcuts.sh              create / refresh the shortcuts
#   ./create-shortcuts.sh --uninstall  remove them
#
set -euo pipefail

APP_ID="ars-plugin-builder"
APP_NAME="ARS Plugin Builder"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DATA_HOME="${XDG_DATA_HOME:-$HOME/.local/share}"
APPS_DIR="$DATA_HOME/applications"
ICON_DIR="$DATA_HOME/icons/hicolor/scalable/apps"
ICON="$ICON_DIR/$APP_ID.svg"
MENU_ENTRY="$APPS_DIR/$APP_ID.desktop"

# Desktop folder (honours a localized/relocated Desktop via xdg-user-dir).
if command -v xdg-user-dir >/dev/null 2>&1; then
    DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || true)"
fi
DESKTOP_DIR="${DESKTOP_DIR:-$HOME/Desktop}"
DESKTOP_ENTRY="$DESKTOP_DIR/$APP_ID.desktop"

refresh_caches() {
    command -v update-desktop-database >/dev/null 2>&1 && update-desktop-database "$APPS_DIR" 2>/dev/null || true
    command -v gtk-update-icon-cache  >/dev/null 2>&1 && gtk-update-icon-cache -q -t "$DATA_HOME/icons/hicolor" 2>/dev/null || true
}

if [[ "${1:-}" == "--uninstall" || "${1:-}" == "-u" ]]; then
    echo "Removing $APP_NAME shortcuts…"
    rm -f "$MENU_ENTRY" "$DESKTOP_ENTRY" "$ICON"
    refresh_caches
    echo "Done."
    exit 0
fi

# --- resolve how to launch the app ---
if [[ -x "$HOME/.local/bin/$APP_ID" ]]; then
    EXEC="$HOME/.local/bin/$APP_ID"
elif [[ -f "$DATA_HOME/$APP_ID/$APP_ID.jar" ]]; then
    EXEC="java -jar \"$DATA_HOME/$APP_ID/$APP_ID.jar\""
else
    JAR="$(ls -t "$SCRIPT_DIR"/target/$APP_ID-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$' | head -n1 || true)"
    if [[ -n "$JAR" ]]; then
        EXEC="java -jar \"$JAR\""
    else
        echo "error: app not found. Run ./install.sh (or 'mvn package') first, then re-run."
        exit 1
    fi
fi

# --- install icon so Icon=$APP_ID resolves from the theme ---
mkdir -p "$ICON_DIR" "$APPS_DIR" "$DESKTOP_DIR"
if [[ -f "$SCRIPT_DIR/packaging/$APP_ID.svg" ]]; then
    install -m 644 "$SCRIPT_DIR/packaging/$APP_ID.svg" "$ICON"
    ICON_KEY="$APP_ID"
else
    ICON_KEY="applications-development"   # stock fallback
fi

# --- the shared .desktop body ---
write_entry() {
    cat > "$1" <<EOF
[Desktop Entry]
Type=Application
Version=1.0
Name=$APP_NAME
GenericName=Contest & Award Plugin Editor
Comment=Create and edit ARS Suite contest & award plugins with a live score preview
Exec=$EXEC
Icon=$ICON_KEY
Terminal=false
Categories=Development;HamRadio;
Keywords=ham;amateur radio;contest;cabrillo;adif;plugin;award;ARS;j-log;
StartupNotify=true
EOF
}

# Menu entry.
write_entry "$MENU_ENTRY"

# Desktop entry — must be executable, and GNOME wants it explicitly trusted.
write_entry "$DESKTOP_ENTRY"
chmod +x "$DESKTOP_ENTRY"
command -v gio >/dev/null 2>&1 && gio set "$DESKTOP_ENTRY" metadata::trusted true 2>/dev/null || true

refresh_caches

echo "✓ Shortcuts created for $APP_NAME"
echo "    menu:    $MENU_ENTRY"
echo "    desktop: $DESKTOP_ENTRY"
echo "    icon:    ${ICON:-($ICON_KEY, stock)}"
echo "    runs:    $EXEC"
echo
echo "If the desktop icon still looks like a document, right-click it →"
echo "\"Allow Launching\" (GNOME) — some desktops require that once."
