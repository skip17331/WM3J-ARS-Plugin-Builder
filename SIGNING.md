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

## Windows — supported, opt-in

The `windows-installer` job (in `.github/workflows/release.yml`, on a real
`windows-latest` runner) wraps the build into a `jpackage` **installer `.exe`** —
a proper PE that Authenticode *can* sign and that SmartScreen reputation attaches
to (the portable `.zip` can't be signed; that's why the installer exists). The
`signtool` step is a no-op until the cert secrets are set; then the installer is
signed automatically.

What you need (one-time): a **code-signing certificate** as a `.pfx`. Options:

- A CA-issued cert — **OV** (~$200–400/yr, DigiCert / Sectigo / …) or **EV**.
  EV clears SmartScreen instantly; a plain OV cert builds reputation over the
  first downloads.
- **Azure Trusted Signing** — cheaper, cloud-based, no `.pfx` to manage (clears
  SmartScreen like EV). If you go this route, swap the `signtool` step for the
  `azure/trusted-signing-action` — say the word and I'll wire it.

Then add these repository secrets:

| Secret | Value |
| --- | --- |
| `WINDOWS_CERT_PFX_BASE64` | `base64 -w0 cert.pfx` (the cert, base64-encoded) |
| `WINDOWS_CERT_PASSWORD` | the `.pfx` password |

With those set, the next tagged release produces a signed
`*-windows-x64-setup.exe`. (The timestamp URL in the job defaults to DigiCert's;
change it if your CA specifies another. Verify the first signed installer on a
real Windows box, since it can't be tested without the cert.)

## Linux

No Gatekeeper/SmartScreen equivalent, so there's nothing to clear. The AppImage
*can* be GPG-signed for integrity (`appimagetool --sign`) if you want detached
verification, but it isn't required to run. Not wired by default.
