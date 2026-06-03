#!/usr/bin/env bash
#
# ARS Plugin Builder — build a distributable Linux AppImage.
#
# Produces a single self-contained file (dist/ARS_Plugin_Builder-<ver>-x86_64.AppImage)
# that bundles a trimmed JRE (via jlink) + the app jar, so the target machine
# needs NOTHING installed — no Java, no Maven. JavaFX rides inside the fat jar;
# the AppImage relies only on a normal desktop's X11/GTK/OpenGL (always present).
#
# Needs (build host only): a JDK 21 (jlink), Maven (if the jar isn't built),
# mksquashfs, and network access the first time (to fetch appimagetool).
#
set -euo pipefail

APP_ID="ars-plugin-builder"
APP_NAME="ARS Plugin Builder"
ARCH="x86_64"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

VERSION="$(sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p' pom.xml | head -1)"
BUILD="$SCRIPT_DIR/build/appimage"
APPDIR="$BUILD/AppDir"
TOOLS="$SCRIPT_DIR/build/tools"
DIST="$SCRIPT_DIR/dist"
OUT="$DIST/ARS_Plugin_Builder-${VERSION}-${ARCH}.AppImage"

# JDK modules the app needs at runtime. Curated but generous so no reflective /
# JDBC / FXML path is missing: JavaFX (java.desktop, jdk.unsupported, scripting,
# xml), Jackson, SQLite (java.sql/naming), logback (xml/management), prefs,
# HttpClient (QRZ lookups), TLS, locale/charset data.
MODULES="java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.charsets,jdk.localedata"

JAVA_HOME="${JAVA_HOME:-$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")}"
JLINK="$JAVA_HOME/bin/jlink"
[[ -x "$JLINK" ]] || { echo "error: jlink not found at $JLINK — a full JDK 21 is required to build."; exit 1; }

# --- 1. fat jar ------------------------------------------------------------
find_jar() { ls -t "$SCRIPT_DIR"/target/$APP_ID-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$' | head -n1 || true; }
JAR="$(find_jar)"
if [[ -z "$JAR" ]]; then
    echo "[1/5] building jar…"
    command -v mvn >/dev/null 2>&1 || { echo "error: Maven required to build the jar."; exit 1; }
    mvn -q -DskipTests clean package
    JAR="$(find_jar)"
fi
[[ -n "$JAR" ]] || { echo "error: no runnable jar under target/."; exit 1; }
echo "[1/5] jar: $(basename "$JAR")"

# --- 2. AppDir skeleton ----------------------------------------------------
echo "[2/5] assembling AppDir…"
rm -rf "$BUILD"; mkdir -p "$APPDIR/usr/lib/$APP_ID" "$APPDIR/usr/share/icons/hicolor/scalable/apps" "$APPDIR/usr/share/applications" "$DIST" "$TOOLS"
install -m 644 "$JAR" "$APPDIR/usr/lib/$APP_ID/$APP_ID.jar"

# --- 3. trimmed JRE via jlink ---------------------------------------------
echo "[3/5] jlink runtime ($MODULES)…"
"$JLINK" --module-path "$JAVA_HOME/jmods" --add-modules "$MODULES" \
    --strip-debug --no-header-files --no-man-pages --compress=2 \
    --output "$APPDIR/usr/lib/runtime"

# --- 4. AppRun + desktop + icon -------------------------------------------
cat > "$APPDIR/AppRun" <<EOF
#!/bin/sh
HERE="\$(dirname "\$(readlink -f "\$0")")"
export JAVA_HOME="\$HERE/usr/lib/runtime"
exec "\$JAVA_HOME/bin/java" -jar "\$HERE/usr/lib/$APP_ID/$APP_ID.jar" "\$@"
EOF
chmod +x "$APPDIR/AppRun"

DESKTOP_BODY="[Desktop Entry]
Type=Application
Name=$APP_NAME
GenericName=Contest & Award Plugin Editor
Comment=Create and edit ARS Suite contest & award plugins with a live score preview
Exec=$APP_ID
Icon=$APP_ID
Terminal=false
Categories=Development;HamRadio;
Keywords=ham;amateur radio;contest;cabrillo;adif;plugin;award;ARS;j-log;
StartupNotify=true"
printf '%s\n' "$DESKTOP_BODY" > "$APPDIR/$APP_ID.desktop"
printf '%s\n' "$DESKTOP_BODY" > "$APPDIR/usr/share/applications/$APP_ID.desktop"

ICON_SRC="$SCRIPT_DIR/packaging/$APP_ID.svg"
[[ -f "$ICON_SRC" ]] || { echo "error: icon $ICON_SRC missing."; exit 1; }
install -m 644 "$ICON_SRC" "$APPDIR/$APP_ID.svg"
install -m 644 "$ICON_SRC" "$APPDIR/usr/share/icons/hicolor/scalable/apps/$APP_ID.svg"
ln -sf "$APP_ID.svg" "$APPDIR/.DirIcon"

# --- 5. appimagetool -> AppImage ------------------------------------------
AIT="$TOOLS/appimagetool-${ARCH}.AppImage"
if [[ ! -x "$AIT" ]]; then
    echo "[5/5] fetching appimagetool…"
    command -v curl >/dev/null 2>&1 || { echo "error: curl needed to fetch appimagetool (one-time)."; exit 1; }
    curl -fL -o "$AIT" \
        "https://github.com/AppImage/AppImageKit/releases/download/continuous/appimagetool-${ARCH}.AppImage"
    chmod +x "$AIT"
fi

echo "[5/5] packaging AppImage…"
rm -f "$OUT"
# --appimage-extract-and-run: works even where FUSE is unavailable for the tool itself.
ARCH="$ARCH" "$AIT" --appimage-extract-and-run --no-appstream "$APPDIR" "$OUT"

echo
echo "✓ $OUT"
ls -lh "$OUT" | awk '{print "  size:", $5}'
echo "  Distribute this one file. On the target:  chmod +x <file> && ./<file>"
