#!/usr/bin/env bash
#
# ARS Plugin Builder — cross-build portable, bundled-JRE zips for every desktop
# platform from one Linux host. Each zip is "unzip and run, no Java needed":
#
#   dist/ARS_Plugin_Builder-<ver>-windows-x64.zip          (run: ARS Plugin Builder.bat)
#   dist/ARS_Plugin_Builder-<ver>-macos-arm64.zip          (open: ARS Plugin Builder.app)
#   dist/ARS_Plugin_Builder-<ver>-macos-x64.zip            (open: ARS Plugin Builder.app)
#   dist/ARS_Plugin_Builder-<ver>-linux-arm64-rpi.zip      (run: ./run.sh)   ← Raspberry Pi
#
# How: rebuild the fat jar with the target's JavaFX natives (-Djavafx.platform),
# cross-jlink a trimmed JRE from the target JDK's jmods (using a matching-version
# linux-x64 jlink so the tool and the jmods agree), add a launcher, zip.
#
# Caveats (inherent to cross-building, not this script):
#   - Bundles are UNSIGNED. Windows SmartScreen may warn; macOS Gatekeeper needs
#     a right-click → Open (or `xattr -cr "ARS Plugin Builder.app"`) the first time.
#   - Built on Linux; not runtime-tested on the target OS.
#   - Each bundle relies on the target having a normal desktop (GTK/GL on Linux,
#     the OS's own graphics stack on Windows/macOS).
#
# Build host needs: JDK 21 (any), Maven, curl, unzip, zip, tar. ~1 GB of JDK
# downloads the first time (cached under build/jdks/).
#
set -euo pipefail

APP_ID="ars-plugin-builder"
APP_NAME="ARS Plugin Builder"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"
VERSION="$(sed -n 's/.*<version>\(.*\)<\/version>.*/\1/p' pom.xml | head -1)"

BUILD="$SCRIPT_DIR/build/bundles"
JDKS="$SCRIPT_DIR/build/jdks"
DIST="$SCRIPT_DIR/dist"
ICON="$SCRIPT_DIR/packaging/$APP_ID.svg"
mkdir -p "$BUILD" "$JDKS" "$DIST"

MODULES="java.base,java.datatransfer,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.scripting,java.sql,java.xml,jdk.unsupported,jdk.crypto.ec,jdk.zipfs,jdk.charsets,jdk.localedata"

# Fetch + extract an Adoptium Temurin 21 GA JDK; echo the extracted root dir.
# All targets use "latest 21 ga" so the jmods match the linux-x64 jlink we run.
fetch_jdk() {
    local os="$1" arch="$2"
    local tag="${os}-${arch}"
    local archive ext dest
    [[ "$os" == windows ]] && ext="zip" || ext="tar.gz"
    archive="$JDKS/jdk-21-$tag.$ext"
    dest="$JDKS/$tag"
    if [[ ! -d "$dest" ]]; then
        if [[ ! -s "$archive" ]]; then
            echo "    ↓ JDK $tag" >&2
            curl -fsSL -o "$archive" \
                "https://api.adoptium.net/v3/binary/latest/21/ga/$os/$arch/jdk/hotspot/normal/eclipse"
        fi
        mkdir -p "$dest"
        if [[ "$ext" == zip ]]; then unzip -q "$archive" -d "$dest"; else tar -xzf "$archive" -C "$dest"; fi
    fi
    echo "$dest"
}
jmods_of() { find "$1" -type d -name jmods | head -1; }

# --- the linux-x64 jlink we drive for every target (version-matched) ---
echo "[*] preparing jlink tool (linux-x64 JDK 21)…"
TOOL_JDK="$(fetch_jdk linux x64)"
JLINK="$(find "$TOOL_JDK" -type f -path '*/bin/jlink' | head -1)"
[[ -x "$JLINK" ]] || { echo "error: no jlink in $TOOL_JDK"; exit 1; }

# build a target-native fat jar (cached per platform under build/bundles)
build_jar() {  # <fx.platform> <fx.version> -> path
    local fxp="$1" fxv="$2"
    local out="$BUILD/jar-$fxp.jar"
    if [[ ! -s "$out" ]]; then
        echo "    • mvn package (javafx=$fxp $fxv)" >&2
        mvn -q -Djavafx.platform="$fxp" -Djavafx.version="$fxv" -DskipTests clean package >&2
        cp "$(ls -t target/$APP_ID-*.jar | grep -Ev -- '-(sources|javadoc)' | head -1)" "$out"
    fi
    echo "$out"
}

jlink_runtime() {  # <target-jdk-root> <out-dir>
    rm -rf "$2"
    # NB: --strip-java-debug-attributes (not --strip-debug) — the latter also runs
    # the HOST objcopy on the TARGET's native binaries, which fails for foreign
    # archs/formats. Bytecode stripping is platform-independent and cross-safe.
    "$JLINK" --module-path "$(jmods_of "$1")" --add-modules "$MODULES" \
        --strip-java-debug-attributes --no-header-files --no-man-pages \
        --compress=2 --output "$2"
}

# ---- per-target builders -------------------------------------------------
build_windows() {
    local root="$BUILD/win" app="$BUILD/win/$APP_NAME"
    rm -rf "$root"; mkdir -p "$app"
    cp "$(build_jar win 21.0.3)" "$app/$APP_ID.jar"
    jlink_runtime "$(fetch_jdk windows x64)" "$app/runtime"
    cat > "$app/$APP_NAME.bat" <<EOF
@echo off
rem ARS Plugin Builder — no Java install required.
start "" "%~dp0runtime\\bin\\javaw.exe" -jar "%~dp0$APP_ID.jar" %*
EOF
    ( cd "$root" && zip -q -r -y "$DIST/ARS_Plugin_Builder-${VERSION}-windows-x64.zip" "$APP_NAME" )
}

build_mac() {  # <adopt-arch> <fx.platform> <out-suffix>
    local arch="$1" fxp="$2" suffix="$3"
    local root="$BUILD/mac-$suffix" app="$BUILD/mac-$suffix/$APP_NAME.app"
    rm -rf "$root"; mkdir -p "$app/Contents/MacOS" "$app/Contents/Resources"
    cp "$(build_jar "$fxp" 21.0.3)" "$app/Contents/Resources/$APP_ID.jar"
    [[ -f "$ICON" ]] && cp "$ICON" "$app/Contents/Resources/$APP_ID.svg"
    jlink_runtime "$(fetch_jdk mac "$arch")" "$app/Contents/Resources/runtime"
    cat > "$app/Contents/MacOS/ARSPluginBuilder" <<EOF
#!/bin/sh
DIR="\$(cd "\$(dirname "\$0")/../Resources" && pwd)"
exec "\$DIR/runtime/bin/java" -jar "\$DIR/$APP_ID.jar" "\$@"
EOF
    chmod +x "$app/Contents/MacOS/ARSPluginBuilder"
    cat > "$app/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
  <key>CFBundleName</key><string>$APP_NAME</string>
  <key>CFBundleDisplayName</key><string>$APP_NAME</string>
  <key>CFBundleIdentifier</key><string>com.wm3j.pluginbuilder</string>
  <key>CFBundleVersion</key><string>$VERSION</string>
  <key>CFBundleShortVersionString</key><string>$VERSION</string>
  <key>CFBundlePackageType</key><string>APPL</string>
  <key>CFBundleExecutable</key><string>ARSPluginBuilder</string>
  <key>NSHighResolutionCapable</key><true/>
</dict></plist>
EOF
    ( cd "$root" && zip -q -r -y "$DIST/ARS_Plugin_Builder-${VERSION}-macos-${suffix}.zip" "$APP_NAME.app" )
}

build_rpi() {
    local root="$BUILD/rpi" app="$BUILD/rpi/$APP_ID"
    rm -rf "$root"; mkdir -p "$app"
    cp "$(build_jar linux-aarch64 23.0.1)" "$app/$APP_ID.jar"
    [[ -f "$ICON" ]] && cp "$ICON" "$app/$APP_ID.svg"
    jlink_runtime "$(fetch_jdk linux aarch64)" "$app/runtime"
    cat > "$app/run.sh" <<EOF
#!/bin/sh
HERE="\$(dirname "\$(readlink -f "\$0")")"
exec "\$HERE/runtime/bin/java" -jar "\$HERE/$APP_ID.jar" "\$@"
EOF
    chmod +x "$app/run.sh"
    ( cd "$root" && zip -q -r -y "$DIST/ARS_Plugin_Builder-${VERSION}-linux-arm64-rpi.zip" "$APP_ID" )
}

echo "[1/4] Windows x64…";        build_windows
echo "[2/4] macOS arm64…";        build_mac aarch64 mac-aarch64 arm64
echo "[3/4] macOS x64…";          build_mac x64     mac         x64
echo "[4/4] Raspberry Pi arm64…"; build_rpi

echo
echo "✓ Bundles in dist/:"
ls -lh "$DIST"/ARS_Plugin_Builder-${VERSION}-*.zip | awk '{printf "    %-52s %s\n", $9, $5}'
