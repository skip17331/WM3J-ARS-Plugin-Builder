#!/usr/bin/env bash
# Emit GitHub Release notes (markdown) for the bundles in dist/, to stdout.
# Usage: release-notes.sh <tag>
set -euo pipefail
TAG="${1:-v0.0.0}"
cd "$(dirname "$0")/.."

cat <<EOF
**ARS Plugin Builder $TAG**

A standalone JavaFX editor for WM3J ARS Suite **contest & award plugins**:
schema-aware entry-field / scoring / multiplier-&-dupe / Cabrillo-mapping forms,
plus a **live score preview** that runs the real engine scorer over your sample
QSOs (per-QSO points, dupes, multipliers, total). Exports plugin JSON straight
to \`~/.j-log/{plugins,awards}/\`.

### Downloads — no Java needed (a trimmed JRE is bundled)

| Platform | File | How to run |
| --- | --- | --- |
| **Windows x64** | \`*-windows-x64.zip\` | unzip → double-click **ARS Plugin Builder.bat** |
| **macOS (Apple Silicon)** | \`*-macos-arm64.zip\` | unzip → open **ARS Plugin Builder.app** |
| **macOS (Intel)** | \`*-macos-x64.zip\` | unzip → open **ARS Plugin Builder.app** |
| **Linux x86_64** | \`*-x86_64.AppImage\` | \`chmod +x\` then run |
| **Raspberry Pi (arm64)** | \`*-linux-arm64-rpi.zip\` | unzip → \`./run.sh\` |

### First-launch trust
If a build is **unsigned** (no signing secrets configured for this release):
- **Windows:** SmartScreen → **More info → Run anyway**.
- **macOS:** Gatekeeper → **right-click the app → Open** (or \`xattr -cr "ARS Plugin Builder.app"\`).

When the Apple signing secrets are configured the macOS apps are signed &
notarized, so they open with no Gatekeeper prompt.

### SHA-256
\`\`\`
EOF

( cd dist && sha256sum ARS_Plugin_Builder-*.zip ARS_Plugin_Builder-*.AppImage 2>/dev/null )
echo '```'
