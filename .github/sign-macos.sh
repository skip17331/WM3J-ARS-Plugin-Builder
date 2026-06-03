#!/usr/bin/env bash
#
# Sign + notarize the macOS .app bundles in dist/ — runs on a macOS CI runner,
# only when the Apple signing secrets are present (see SIGNING.md). Uses Apple's
# official tooling (codesign + notarytool + stapler). Untested until a real
# Developer ID cert is supplied; treat as the ready-to-enable recipe.
#
# Required env (from GitHub Secrets):
#   MACOS_CERT_P12_BASE64   base64 of your Developer ID Application cert (.p12)
#   MACOS_CERT_PASSWORD     password for that .p12
#   MACOS_SIGN_IDENTITY     e.g. "Developer ID Application: Your Name (TEAMID)"
#   MACOS_NOTARY_API_KEY    base64 of the App Store Connect API key (.p8)
#   MACOS_NOTARY_KEY_ID     the API key ID
#   MACOS_NOTARY_ISSUER_ID  the API key issuer UUID
#
set -euo pipefail

APP_NAME="ARS Plugin Builder"
WORKROOT="$(pwd)"
KEYCHAIN="$RUNNER_TEMP/signing.keychain-db"
KPASS="$(uuidgen)"

# --- import the signing identity into a throwaway keychain ---
echo "$MACOS_CERT_P12_BASE64" | base64 --decode > "$RUNNER_TEMP/cert.p12"
security create-keychain -p "$KPASS" "$KEYCHAIN"
security set-keychain-settings -lut 21600 "$KEYCHAIN"
security unlock-keychain -p "$KPASS" "$KEYCHAIN"
security import "$RUNNER_TEMP/cert.p12" -k "$KEYCHAIN" -P "$MACOS_CERT_PASSWORD" \
    -T /usr/bin/codesign
security set-key-partition-list -S apple-tool:,apple: -s -k "$KPASS" "$KEYCHAIN" >/dev/null
security list-keychains -d user -s "$KEYCHAIN" $(security list-keychains -d user | tr -d '"')

# --- App Store Connect API key for notarytool ---
echo "$MACOS_NOTARY_API_KEY" | base64 --decode > "$RUNNER_TEMP/ac_key.p8"

sign_one() {
    local zip="$1"
    [ -f "$zip" ] || { echo "  (no $zip — skipping)"; return; }
    echo "==> $zip"
    local work; work="$(mktemp -d)"
    unzip -q "$zip" -d "$work"
    local app="$work/$APP_NAME.app"

    # Sign nested code first (the bundled JRE's dylibs/executables), then the app.
    codesign --force --options runtime --timestamp \
        --keychain "$KEYCHAIN" --sign "$MACOS_SIGN_IDENTITY" \
        --deep "$app"
    codesign --verify --deep --strict "$app"

    # Notarize a zip of the signed app, then staple the ticket into the .app.
    ( cd "$work" && /usr/bin/zip -q -r -y notarize.zip "$APP_NAME.app" )
    xcrun notarytool submit "$work/notarize.zip" \
        --key "$RUNNER_TEMP/ac_key.p8" \
        --key-id "$MACOS_NOTARY_KEY_ID" \
        --issuer "$MACOS_NOTARY_ISSUER_ID" \
        --wait
    xcrun stapler staple "$app"

    # Repackage over the original zip.
    rm -f "$WORKROOT/$zip"
    ( cd "$work" && /usr/bin/zip -q -r -y "$WORKROOT/$zip" "$APP_NAME.app" )
    rm -rf "$work"
    echo "    signed, notarized, stapled ✓"
}

for z in dist/*macos*.zip; do
    sign_one "$z"
done
echo "macOS signing complete."
