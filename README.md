# ARS Plugin Builder

A standalone JavaFX desktop tool for authoring **WM3J ARS Suite** contest and
award plugins — schema-aware forms, validation, and Cabrillo / award preview,
exporting the JSON the suite drops into `~/.j-log/plugins/` and `~/.j-log/awards/`.

It **supports** the ARS Suite but is **not part of it**: a separate repo that
depends on the suite's `j-log-engine` so its validation and previews run against
the *real* engine, not a re-implementation.

> Status: **scaffold.** The shell builds, wires in `j-log-engine`, and lists the
> plugins the engine can see. The editor / validators / preview build on top.

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

## Distributable AppImage (Linux)

```bash
./build-appimage.sh        # → dist/ARS_Plugin_Builder-<ver>-x86_64.AppImage
```

Produces one self-contained file that bundles a trimmed JRE (via `jlink`) plus
the app — the target machine needs **nothing installed** (no Java, no Maven).
Hand someone the file; they `chmod +x` it and run it. Relies only on a normal
desktop's X11/GTK/OpenGL. Build host needs a JDK 21, `mksquashfs`, and (the
first time) network access to fetch `appimagetool`. ~79 MB.

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

🤖 Companion tool for the WM3J ARS Suite.
