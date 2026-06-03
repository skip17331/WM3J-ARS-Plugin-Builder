# ARS Plugin Builder

A standalone JavaFX desktop tool for authoring **WM3J ARS Suite** contest and
award plugins — schema-aware forms, validation, and Cabrillo / award preview,
exporting the JSON the suite drops into `~/.j-log/plugins/` and `~/.j-log/awards/`.

It **supports** the ARS Suite but is **not part of it**: a separate repo that
depends on the suite's `j-log-engine` so its validation and previews run against
the *real* engine, not a re-implementation.

> **New here?** The **[User Guide](USER_GUIDE.md)** walks through building a
> contest or award plugin end to end — the tabs, validation, the live score
> preview, and saving into the suite.

## Prerequisites

- **Java 21** + **Maven 3.8+**
- **`j-log-engine` installed to your local `~/.m2`.** It isn't published anywhere,
  so build it once from your ARS Suite checkout:
  ```bash
  mvn -q -DskipTests -f ../ARS_Suite/j-log-engine/pom.xml install
  ```
  (Adjust the path to wherever `ARS_Suite` lives. This produces
  `com.jlog:j-log-engine:1.5.0` in `~/.m2`, which this project depends on.)

## Build & run

```bash
mvn clean package          # builds target/ars-plugin-builder-0.1.0.jar (self-contained)
./run.sh                   # or: java -jar target/ars-plugin-builder-0.1.0.jar
# dev loop: mvn javafx:run
```

## Install (Linux)

```bash
./install.sh               # builds (if needed) + installs under ~/.local; re-run to upgrade
./install.sh --uninstall   # remove
```

No root, XDG-compliant: drops the jar in `~/.local/share/ars-plugin-builder/`, a
launcher at `~/.local/bin/ars-plugin-builder`, a `.desktop` entry, and a
scalable icon — so **ARS Plugin Builder** appears in your application menu.
Detects Java 21+ (and Maven, only if a build is needed) and advises if missing;
never installs system packages.

Want a **Desktop icon too** (and/or just the shortcuts, without reinstalling)?

```bash
./create-shortcuts.sh              # menu entry + a Desktop launcher icon
./create-shortcuts.sh --uninstall  # remove the shortcuts
```

## Distributable AppImage (Linux)

```bash
./build-appimage.sh        # → dist/ARS_Plugin_Builder-<ver>-x86_64.AppImage
```

Produces one self-contained file that bundles a trimmed JRE (via `jlink`) plus
the app — the target machine needs **nothing installed** (no Java, no Maven).
Hand someone the file; they `chmod +x` it and run it. Relies only on a normal
desktop's X11/GTK/OpenGL. Build host needs a JDK 21, `mksquashfs`, and (the
first time) network access to fetch `appimagetool`. ~79 MB.

## Cross-platform portable bundles (Windows / macOS / Raspberry Pi)

```bash
./build-bundles.sh         # → four zips in dist/ (all built from Linux)
```

Cross-builds self-contained, bundled-JRE zips — the target needs **no Java**:

| Bundle | Run it by |
| --- | --- |
| `…-windows-x64.zip` | double-clicking `ARS Plugin Builder.bat` |
| `…-macos-arm64.zip` / `…-macos-x64.zip` | opening `ARS Plugin Builder.app` |
| `…-linux-arm64-rpi.zip` | `./run.sh` (Raspberry Pi) |

It rebuilds the fat jar per platform with that OS's JavaFX natives, cross-`jlink`s
a trimmed JRE from each target's Adoptium JDK (downloaded/cached under `build/`),
adds a launcher, and zips. Needs a JDK 21, Maven, and network the first time
(~1 GB of JDKs, cached after). **Unsigned**: Windows SmartScreen may warn;
macOS Gatekeeper needs a first-run right-click → **Open** (or
`xattr -cr "ARS Plugin Builder.app"`). Built on Linux, so smoke-test on the real
OS before wide distribution. Raspberry Pi uses JavaFX 23.0.1 (21.x has no
linux-aarch64 build).

## How it relates to the suite

- **Schema contract:** `ARS_Suite/docs/PLUGIN_FORMAT.md` — the code-verified
  reference this tool's forms and validators are generated from.
- **Shared data dir:** the builder uses the operator's `~/.j-log/` (same as the
  suite) — it reads bundled plugins from the engine jar and reads/writes the
  auto-loaded `~/.j-log/plugins/` and `~/.j-log/awards/` drop-in dirs.
- **Engine/app split (important):** plugin parse/load, Cabrillo export, and all
  award-progress logic live in `j-log-engine` (reusable here). Contest
  scoring / multiplier / dupe dispatch currently lives in the suite's `j-log`
  app (`ContestLogController`), **not** the engine — so a live contest *score*
  preview needs either a `j-log` dependency or lifting that logic into the
  engine. Award preview and Cabrillo preview work off the engine alone.

## Dependency coupling

For now the engine is consumed from `~/.m2` (build it first, as above). If this
repo ever needs to build on a machine without the suite checked out, switch to a
git submodule of the engine or publish `j-log-engine` to GitHub Packages and pin
the version.

## Releases & CI

Releases are automated. Pushing a version tag builds every bundle and publishes
a GitHub Release with the artifacts + SHA-256 checksums:

```bash
git tag v0.2.0 && git push origin v0.2.0
```

(`.github/workflows/release.yml` — also runnable manually for an existing tag via
the Actions tab.) macOS code-signing + notarization is opt-in and activates when
the Apple secrets are present; see [SIGNING.md](SIGNING.md) for that and the
Windows signing story.

## License

[MIT](LICENSE) © 2026 Mike Kipps (WM3J).

🤖 Companion tool for the WM3J ARS Suite.
