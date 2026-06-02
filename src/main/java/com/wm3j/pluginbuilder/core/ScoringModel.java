package com.wm3j.pluginbuilder.core;

/** The scalar/enum scoring knobs the form owns (the complex maps stay in JSON). */
public final class ScoringModel {

    /** Mutually-exclusive dupe rule — the engine considers the flags in this order and uses the first. */
    public enum Dupe {
        DEFAULT      ("Default (call + band + mode)",        null),
        CONTEST_WIDE ("Contest-wide (call only)",            "contestWideDupe"),
        ROVER_AWARE  ("Rover-aware (/R re-workable per grid)", "roverAwareDupe"),
        PER_BAND_GRID("Per band + grid (every call)",        "perBandGridDupe"),
        FIELD_DAY    ("Field Day (per band + mode class)",   "fieldDayModeDupe");

        public final String label;
        public final String jsonKey;   // null for DEFAULT
        Dupe(String label, String jsonKey) { this.label = label; this.jsonKey = jsonKey; }
        @Override public String toString() { return label; }
    }

    public Dupe dupe = Dupe.DEFAULT;
    public boolean perModeMultipliers = false;
    public boolean autoFillDxccPrefix = false;
    public boolean autoFillWpxPrefix = false;
    public String  multField = "";       // multiplierModel.field
    public boolean multPerBand = false;  // multiplierModel.perBand
    public String  multiplierType = "";  // scoringRules.multiplierType
    public int     pointsPerQso = 0;     // scoringRules.pointsPerQso
    public boolean scoreIsPointsOnly = false;
}
