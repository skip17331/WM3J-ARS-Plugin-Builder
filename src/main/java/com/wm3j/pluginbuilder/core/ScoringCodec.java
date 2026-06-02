package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

/**
 * Reads/writes the scalar scoring/multiplier/dupe knobs in a plugin JSON node.
 * <p>Apply <b>merges</b> into the existing {@code scoringRules}/{@code multiplierModel}
 * objects — it only touches the keys the form owns, so complex keys it doesn't
 * model (pointsByBand, distanceScoring, qsoParty, validValues, …) survive — and
 * writes exactly one dupe flag, so the "multiple dupe flags" warning is impossible.
 */
public final class ScoringCodec {
    private ScoringCodec() {}

    /** multiplierType options for the dropdown ("" = generic distinct-count). */
    public static final List<String> MULT_TYPES = List.of(
        "", "zone_country", "zone_country_state", "wpx_prefix", "state_prov_country",
        "grid_field", "wae", "qso_party", "all_asian", "russian_dx", "sac",
        "ari_dx", "wag", "oceania_dx", "dxcc", "sections", "states", "custom");

    public static ScoringModel read(ObjectNode root) {
        ScoringModel m = new ScoringModel();
        // dupe: first set flag in dispatch order wins (mirrors the engine)
        for (ScoringModel.Dupe d : ScoringModel.Dupe.values()) {
            if (d.jsonKey != null && root.path(d.jsonKey).asBoolean(false)) { m.dupe = d; break; }
        }
        m.perModeMultipliers = root.path("perModeMultipliers").asBoolean(false);
        m.autoFillDxccPrefix = root.path("autoFillDxccPrefix").asBoolean(false);
        m.autoFillWpxPrefix  = root.path("autoFillWpxPrefix").asBoolean(false);

        m.multField   = root.path("multiplierModel").path("field").asText("");
        m.multPerBand = root.path("multiplierModel").path("perBand").asBoolean(false);

        m.multiplierType     = root.path("scoringRules").path("multiplierType").asText("");
        m.pointsPerQso       = root.path("scoringRules").path("pointsPerQso").asInt(0);
        m.scoreIsPointsOnly  = root.path("scoringRules").path("scoreIsPointsOnly").asBoolean(false);
        return m;
    }

    public static void apply(ObjectNode root, ScoringModel m) {
        // dupe: clear all four, then set the chosen one (DEFAULT sets none)
        for (ScoringModel.Dupe d : ScoringModel.Dupe.values()) if (d.jsonKey != null) root.remove(d.jsonKey);
        if (m.dupe.jsonKey != null) root.put(m.dupe.jsonKey, true);

        flag(root, "perModeMultipliers", m.perModeMultipliers);
        flag(root, "autoFillDxccPrefix", m.autoFillDxccPrefix);
        flag(root, "autoFillWpxPrefix",  m.autoFillWpxPrefix);

        // multiplierModel — merge, preserving e.g. validValues
        ObjectNode mm = child(root, "multiplierModel");
        setStr(mm, "field", m.multField);
        flag(mm, "perBand", m.multPerBand);
        if (mm.isEmpty()) root.remove("multiplierModel");

        // scoringRules — merge, preserving pointsByBand/distanceScoring/qsoParty/…
        ObjectNode sr = child(root, "scoringRules");
        setStr(sr, "multiplierType", m.multiplierType);
        if (m.pointsPerQso > 0) sr.put("pointsPerQso", m.pointsPerQso); else sr.remove("pointsPerQso");
        flag(sr, "scoreIsPointsOnly", m.scoreIsPointsOnly);
        if (sr.isEmpty()) root.remove("scoringRules");
    }

    // ---- helpers --------------------------------------------------------
    private static ObjectNode child(ObjectNode parent, String key) {
        if (parent.path(key).isObject()) return (ObjectNode) parent.get(key);
        ObjectNode n = parent.objectNode();
        parent.set(key, n);
        return n;
    }
    private static void flag(ObjectNode n, String key, boolean v) {
        if (v) n.put(key, true); else n.remove(key);
    }
    private static void setStr(ObjectNode n, String key, String v) {
        if (v != null && !v.isBlank()) n.put(key, v.trim()); else n.remove(key);
    }
}
