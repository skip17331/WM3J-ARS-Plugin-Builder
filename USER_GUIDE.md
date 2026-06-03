# ARS Plugin Builder — User Guide

ARS Plugin Builder is a desktop tool for authoring the **contest** and **award**
plugins used by the WM3J ARS Suite (J-Log). You build a plugin with guided forms
and a live preview, then save it — and the suite picks it up with no code changes.

Plugins are plain JSON. The builder is the friendly front end (and the validation
layer) over the format documented in
[`ARS_Suite/docs/PLUGIN_FORMAT.md`](https://github.com/skip17331/WM3J-ARS-Suite/blob/main/docs/PLUGIN_FORMAT.md), which is
the authoritative field-by-field reference. This guide is the *how-to*; that file
is the *what-each-key-means*.

---

## Contents

1. [What it makes & where it goes](#1-what-it-makes--where-it-goes)
2. [Launching it](#2-launching-it)
3. [The window at a glance](#3-the-window-at-a-glance)
4. [The one rule that matters: JSON is canonical](#4-the-one-rule-that-matters-json-is-canonical)
5. [Walkthrough: build a contest plugin](#5-walkthrough-build-a-contest-plugin)
6. [Walkthrough: build an award plugin](#6-walkthrough-build-an-award-plugin)
7. [Validate](#7-validate)
8. [Save & use it in the suite](#8-save--use-it-in-the-suite)
9. [Reference tables](#9-reference-tables)
10. [Tips & gotchas](#10-tips--gotchas)
11. [Troubleshooting](#11-troubleshooting)

---

## 1. What it makes & where it goes

| You author | …as a file | …saved to | …the suite |
| --- | --- | --- | --- |
| A **contest** plugin | `<contestId>.json` | `~/.j-log/plugins/` | loads it on next launch |
| An **award** plugin | `<awardId>.json` | `~/.j-log/awards/` | loads it on **Refresh** |

The builder shares the suite's `~/.j-log/` data directory. A user plugin whose id
matches a bundled one **replaces** the bundled version. The suite **silently
skips** a malformed plugin (it just won't appear) — which is exactly why you
should **Validate before you Save**.

---

## 2. Launching it

See the [README](README.md) for install options — `install.sh` (menu entry),
the AppImage, or the per-platform bundles. Once installed, launch **ARS Plugin
Builder** from your application menu (or run `ars-plugin-builder`). The title bar
shows the version.

A healthy start shows, in the bold line at the top:
`j-log-engine wired — N contest plugins, M awards visible`. (If it instead says
*Engine init failed…*, see [Troubleshooting](#11-troubleshooting).)

---

## 3. The window at a glance

```
┌ j-log-engine wired — 86 contest plugins, 5 awards visible ───────────────┐
│ [New Contest] [New Award] [Open…] | [Validate] [Save] | [↻]              │  ← toolbar
├──────────────┬──────────────────────────────────────────────────────────┤
│ Your plugins │  JSON · Entry Fields · Scoring · Cabrillo · Award · Preview│  ← tabs
│ (dbl-click)  │                                                            │
│              │  (the selected tab's editor)                               │
├──────────────┴──────────────────────────────────────────────────────────┤
│ Validation  (errors & warnings land here)                                 │
│ status line (what just happened)                                          │
└───────────────────────────────────────────────────────────────────────────┘
```

**Toolbar**
- **New Contest / New Award** — load a minimal valid skeleton into the JSON tab.
- **Open…** — file chooser (defaults to `~/.j-log/plugins/`).
- **Validate** — run the checks; results fill the Validation pane.
- **Save** — validate, then (if no errors) write to the drop-in dir.
- **↻** — refresh the "Your plugins" list.

**Your plugins** (left) — every `.json` in `~/.j-log/plugins/` and
`~/.j-log/awards/`. Double-click to open one (great for starting from an existing
plugin).

---

## 4. The one rule that matters: JSON is canonical

The **JSON** tab is the real document. The structured tabs (Entry Fields,
Scoring, Cabrillo, Award) are *views* onto parts of it, and they sync **only when
you tell them to**:

- **⤓ Load from JSON** — pull the current JSON into this form. (Also happens
  automatically when you switch to a structured tab.)
- **⤒ Apply to JSON** — write this form's section back into the JSON.

This means **your hand edits in the JSON tab are never silently clobbered** — a
form only changes the keys it owns, and only when you hit Apply. The normal loop:

> edit a form → **Apply to JSON** → **Validate** → fix → **Save**.

Anything the forms don't cover (complex scoring maps, panes, `qsoParty`, …) you
edit directly in the JSON tab; Apply from the other tabs preserves it.

---

## 5. Walkthrough: build a contest plugin

We'll make a simple contest: exchange **RST + your state**, **1 point/QSO**,
multiplier = **distinct states worked**.

### 5.1 Start from the skeleton

Click **New Contest**. You get a valid RST-+-serial starting point:

```json
{
  "contestId": "MY_CONTEST",
  "contestName": "My Contest",
  "cabrilloContestName": "",
  "version": "1.0.0",
  "exchangeFormat": "RST + Serial",
  "entryFields": [ … ],
  "cabrilloSent": ["rst_sent", "serial_sent"],
  "cabrilloRcvd": ["rst_rcvd", "serial_rcvd"],
  "scoringRules": { "pointsPerQso": 1, "multiplierType": "dxcc" },
  "multiplierModel": { "field": "callsign", "perBand": false }
}
```

In the **JSON** tab, set `contestId` (e.g. `MY_STATE_TEST` — it becomes the
filename and the Cabrillo `CONTEST:` fallback) and `contestName`. Set
`cabrilloContestName` only if the sponsor's Cabrillo name differs from the id.

### 5.2 Entry Fields tab

Master-detail: the list (left) is your `entryFields[]`; the form (right) edits the
selected row. **Add / Remove / ↑ / ↓** manage rows; the detail form has:

| Control | Meaning |
| --- | --- |
| **id** | machine name (`callsign`, `rst_rcvd`, or your own like `state_rcvd`) |
| **label** | what's shown on the entry bar |
| **type** | `text`, `combo`, `number`, `checkbox` — **only `combo` changes behavior** (dropdown; needs **options**); the rest render as a text box |
| **row** | Received (0) or Sent (1) — entry-bar row / Tab order |
| **width** | pixel width (0 = default) |
| **options** | combo items, comma-separated (also red-rings a text field on mismatch) |
| **validator** | `(none)`, `maidenhead`, `maidenhead6`, `numeric`, `fd_class`, `ss_check` |
| **autoIncrement** | serial fields: prefilled, non-editable |
| **persistent** | value survives Clear |
| **constant** | operator-constant sent value (no per-QSO slot; Cabrillo fills it from station config) |

**The 5-slot rule (important).** The engine stores received-exchange values in
five columns, `field1`–`field5`. Most field ids consume one slot **in declaration
order** — *except* these, which use none: `callsign, serial_sent, serial_rcvd,
band, mode, rst_sent, rst_rcvd, prec_sent, check_sent, sect_sent` (and any field
marked `constant`). **You get five slots; a 6th slot-consuming field is dropped**
(Validate flags it as an error).

*For our example:* delete `serial_rcvd`/`serial_sent`, and **Add** a field
`id = state_rcvd`, `label = "St"`, row = Received. It takes the first slot
(`field1`). Click **⤒ Apply to JSON**.

### 5.3 Scoring tab

Scalar knobs (the complex maps stay in JSON — there's a hint at the bottom of the
tab saying so):

| Control | Writes | Notes |
| --- | --- | --- |
| **multiplierType** | `scoringRules.multiplierType` | pick from the list or type your own; blank = generic distinct-count |
| **multiplier field** | `multiplierModel.field` | the entry-field id holding the mult value (offers your field ids) |
| **multiplier counts per band** | `multiplierModel.perBand` | |
| **dupe rule** | one dupe flag | see [dupe rules](#dupe-rules) |
| **points / QSO** | `scoringRules.pointsPerQso` | |
| **score is points only** | `scoringRules.scoreIsPointsOnly` | Field-Day style (no multiplier) |
| **multipliers per mode** | `perModeMultipliers` | |
| **auto-fill DXCC / WPX prefix** | the autofill flags | ⚠ DXCC autofill can corrupt Cabrillo on dual-mapped columns |

*For our example:* multiplier field = `state_rcvd`, multiplierType = `states`
(documentary — the generic distinct-count path does the real work), points/QSO =
1, dupe rule = Default. **Apply to JSON**.

> Most `multiplierType` values (`dxcc`, `states`, `sections`, `custom`, blank) all
> use the same *points × distinct(multiplier field)* engine path — what differs is
> your `multiplierModel`, lists, and panes, not the string. Thirteen values
> (`zone_country`, `wpx_prefix`, `qso_party`, `wae`, `all_asian`, …) trigger
> dedicated, asymmetric logic. See the [catalog](#multipliertype-catalog).

### 5.4 Cabrillo tab

Defines the **exchange order** in the exported Cabrillo log. Three columns:
**Available tokens** (a palette of only *resolvable* tokens — your entry-field ids
plus specials like `callsign`, `rst_sent`, `mycall`), **Sent** (`cabrilloSent`),
and **Received** (`cabrilloRcvd`).

- Select a token → **→ Sent** / **→ Rcvd** (or double-click → Rcvd).
- Reorder each list with **↑ / ↓**, remove with **✕**.
- Because the palette only offers valid tokens, a mapping you build here **can't
  trip the validator's token check**.

*For our example:* Sent = `rst_sent`; Received = `rst_rcvd`, `state_rcvd`. **Apply
to JSON**.

### 5.5 Preview tab

This is the real engine, not a mock. It shows the exact **Cabrillo `CONTEST:`
header + exchange order**, then a **live score breakdown** computed by the suite's
scorer over sample QSOs you type:

- **My call / My grid / Sent QTH** — your station facts (they matter for
  asymmetric-DX and QSO-party scoring).
- **Sample QSOs** — one per line: `call, band, mode, exchange…` (the exchange
  values fill `field1…` in order). Lines starting with `#` are comments.

You get per-QSO points, dupe flags, multiplier count, and total score — exactly
what the cockpit would show. It's a *claimed/running* figure; sponsors
re-adjudicate from the submitted log.

---

## 6. Walkthrough: build an award plugin

Awards are simpler and run entirely against your **normal log** (`j-log.db`) —
one credit per distinct matched value (no per-band/per-mode crediting).

Click **New Award**, then use the **Award** tab:

| Control | Key | Notes |
| --- | --- | --- |
| **awardId** | `awardId` | required; becomes the filename |
| **awardName / description** | | card title / details |
| **matchOn** | `matchOn` | which QSO field to credit — `state`, `country`, `callsign`, `prefix` (WPX), `dxccPrefix`, `continent`, `grid` |
| **targetLabel** | | the count axis ("States") |
| **match base callsign** | `options.matchBaseCallsign` | strip `/P`, `/M`, … |
| **confirmed only** | `options.confirmedOnly` | require QSL received |
| **window start / end** | `window` | optional ISO-8601 time gate |
| **Targets** | `targets[]` | one per line, `id | label` (e.g. `CT | Connecticut`) |
| **Bonus** | `bonus[]` | extra targets, same format |
| **Tiers** | `tiers[]` | one per line, `threshold | name` (e.g. `50 | Worked All States`) |

**Set-match vs count-match:**
- **Targets present** → set-match: progress = how many of your targets you've
  worked (total = targets + bonus). *Example: Worked All States — list 50 targets.*
- **Targets empty** → count-match: progress = distinct matched values, measured
  against the **top tier** threshold. *Example: WPX — no targets, a tier at 300.*

The **Preview** tab computes **real award progress** against your actual log via
the engine's `AwardService`.

---

## 7. Validate

**Validate** encodes the plugin spec as live checks. **Errors** block Save;
**warnings** don't (they're advisory). It reports against the JSON in the editor
(it knows it's a contest vs award by the presence of `contestId` / `awardId`).

**Errors** (must fix):
- missing `contestId` / `awardId`;
- more than 5 slot-consuming entry fields;
- a `cabrilloSent`/`cabrilloRcvd` token that matches no field id and isn't a
  special token (would export blank).

**Warnings** (worth a look):
- `contestId` with characters outside `[A-Za-z0-9_]` (it's a filename + token);
- unknown `multiplierType`, `validator`, `paneType`, or award `matchOn` (likely a
  typo — falls back / ignored);
- a `combo` field with no `options`;
- more than one dupe flag set (only the first in dispatch order is used);
- **vestigial** keys that the engine never reads (`scoreFormula`, `allowDupes`,
  `statistics`, `paneIndex`, `required`) — safe to delete;
- award tier thresholds not strictly ascending; a count-match award with no tiers.

---

## 8. Save & use it in the suite

**Save** parses, validates, and — if there are **no errors** — writes
`<id>.json` (the id is sanitized for the filename) to `~/.j-log/plugins/` (contest)
or `~/.j-log/awards/` (award), then refreshes the plugin list. If there are
errors it refuses and tells you how many to fix.

In the suite:
- a **contest** plugin appears on the next launch;
- an **award** appears on **Refresh**;
- a user plugin with the same id as a bundled one **wins**.

---

## 9. Reference tables

The full schema is in
[`ARS_Suite/docs/PLUGIN_FORMAT.md`](https://github.com/skip17331/WM3J-ARS-Suite/blob/main/docs/PLUGIN_FORMAT.md). The
vocabularies the builder enforces:

### Dupe rules
| Rule | Dupe key |
| --- | --- |
| Default | call + band + mode |
| Contest-wide | callsign only (Sweepstakes) |
| Rover-aware | `/R`: call+band+grid; else call+band |
| Per band + grid | every call: call+band+grid (10 GHz & Up) |
| Field Day | call + band + mode-class (CW/PH/DG) |

`multiplierType:"qso_party"` uses its own call+band+mode-class+county dupe.

### multiplierType catalog
**Dedicated logic:** `zone_country`, `zone_country_state`, `wpx_prefix`,
`state_prov_country`, `grid_field`, `wae`, `qso_party`, `all_asian`, `russian_dx`,
`sac`, `ari_dx`, `wag`, `oceania_dx`.
**Generic distinct-count:** `dxcc`, `sections`, `states`, `custom`, or blank.

### Field validators
`maidenhead`, `maidenhead6`, `numeric`, `fd_class`, `ss_check`. (Unknown → ignored.)

### Pane types (`row2Panes[].paneType`)
`dupe_checker`, `section_tracker`, `statistics`, `us_state_map`, `canada_map`,
`dxcc_list`, `dxcc_map`, `county_map`, `county_list`, `per_mode_mult_grid`,
`worked_before`, `grid_map`, `qtc`, `ss_section_map`, `sweep_progress`,
`worked_mults`. (Edit these in the JSON tab.)

### Award `matchOn`
`state`, `country`, `callsign`, `prefix`, `dxccPrefix`, `continent`, `grid`.

### Don't emit these (vestigial)
`scoringRules.scoreFormula`, `scoringRules.allowDupes`, `statistics`,
`row2Panes[].paneIndex`, `entryFields[].required`.

---

## 10. Tips & gotchas

- **Five field slots, in declaration order.** Plan which received-exchange fields
  matter; specials (`callsign`, `rst_*`, `serial_*`, `band`, `mode`, …) are free.
- **`type` only branches on `combo`.** `number`/`checkbox` render as text today —
  use `text` or `combo`.
- **DXCC autofill + Cabrillo.** `autoFillDxccPrefix` can corrupt the exported
  exchange when a column is dual-mapped — check the Cabrillo preview after.
- **Complex scoring lives in JSON.** `pointsByBand`, `distanceScoring`, and the
  ~50-field `qsoParty` block aren't in the Scoring form by design; edit them in the
  JSON tab — the forms preserve them.
- **Score preview is claimed/running.** It mirrors the cockpit; the sponsor's
  checker is the final word.
- **Start from a real one.** Double-click a bundled plugin in "Your plugins",
  tweak, and **Save under a new `contestId`** — often faster than from scratch.

---

## 11. Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| **"Engine init failed: NullPointerException … this.prefs is null"** | A pre-0.1.1 startup bug. Update to **v0.1.1+** (it's fixed). |
| **"Your plugins" is empty** | No `.json` files in `~/.j-log/{plugins,awards}/` yet, or the engine failed to init (see above). |
| **"Parse error: …" on Validate** | The JSON tab isn't valid JSON — fix the syntax (a trailing comma, a missing quote). |
| **"JSON has neither contestId nor awardId"** | Every plugin needs one of those keys so the tool knows which kind it is. |
| **"Fix N error(s) before saving"** | Open the Validation pane; resolve the `ERROR` rows (warnings are fine). |
| **A form's Apply did nothing** | The JSON tab must be parseable first — Validate to find the syntax error. |
| **Saved plugin doesn't show in the suite** | Contest needs a relaunch; award needs Refresh. Confirm the id is unique/intended and the file is in the right drop-in dir. |

---

*Companion tool for the WM3J ARS Suite. MIT licensed.*
