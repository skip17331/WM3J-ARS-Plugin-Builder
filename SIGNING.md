# Code signing

The released bundles are **unsigned by default**. Signing can't be faked — it
requires certificates that cost money and verify your identity — so the release
pipeline is wired to sign **only when you supply the credentials** as GitHub
repository secrets. Until then everything publishes unsigned (users just click
through a one-time OS prompt; see the release notes).

## macOS — supported, opt-in (recommended; the prompt there is the most annoying)

Signing + notarization removes the Gatekeeper "can't be opened" block entirely.
It runs on the `sign-macos` job in `.github/workflows/release.yml`
(`.github/sign-macos.sh`), which is a no-op until these secrets exist.

What you need (one-time):

1. **Apple Developer Program** membership ($99/yr) — <https://developer.apple.com/programs/>.
2. A **Developer ID Application** certificate. Create it in the Apple Developer
   portal (or Xcode → Settings → Accounts → Manage Certificates), then export it
   from Keychain Access as a `.p12` with a password.
3. An **App Store Connect API key** for notarization (Users and Access → Integrations
   → App Store Connect API → generate a key with the *Developer* role). Download
   the `.p8`; note its **Key ID** and the **Issuer ID**.

Then add these repository secrets (Settings → Secrets and variables → Actions):

| Secret | Value |
| --- | --- |
| `MACOS_CERT_P12_BASE64` | `base64 -i DeveloperID.p12` (the cert, base64-encoded) |
| `MACOS_CERT_PASSWORD` | the `.p12` export password |
| `MACOS_SIGN_IDENTITY` | e.g. `Developer ID Application: Your Name (TEAMID)` |
| `MACOS_NOTARY_API_KEY` | `base64 -i AuthKey_XXXX.p8` (the API key, base64-encoded) |
| `MACOS_NOTARY_KEY_ID` | the API key's Key ID |
| `MACOS_NOTARY_ISSUER_ID` | the API key's Issuer ID (a UUID) |

With those set, the next tagged release produces signed + notarized
`*-macos-arm64.zip` / `*-macos-x64.zip`. (The script is the standard
codesign → notarytool → stapler recipe; verify the first signed build opens
cleanly on a real Mac, since it can't be tested without the cert.)

## Windows — not wired (format mismatch, documented honestly)

Authenticode signs PE files (`.exe`/`.dll`/`.msi`). Our Windows deliverable is a
**zip of a folder** with a `.bat` launcher — there is no single PE that
represents "the app", so signing the zip is not a thing, and SmartScreen
reputation is keyed to the **downloaded file**, not the bundled (already
Adoptium-signed) `javaw.exe`.

To actually benefit you'd switch the Windows deliverable to a signed installer:

1. Build a `jpackage` `.exe`/`.msi` on a `windows-latest` runner (jpackage can't
   cross-compile, so this is a separate native job — not the current Linux
   cross-build).
2. Sign it with `signtool` using a code-signing certificate — a CA-issued OV/EV
   cert (~$200–400/yr from DigiCert, Sectigo, …) or **Azure Trusted Signing**
   (cheaper, cloud-based). EV/Trusted Signing also clears SmartScreen instantly;
   a plain OV cert needs reputation to build up.

This is a real chunk of work and a paid cert; it's deliberately left as a future
step rather than half-built. Until then Windows users click **More info → Run
anyway** once.

## Linux

No Gatekeeper/SmartScreen equivalent, so there's nothing to clear. The AppImage
*can* be GPG-signed for integrity (`appimagetool --sign`) if you want detached
verification, but it isn't required to run. Not wired by default.
