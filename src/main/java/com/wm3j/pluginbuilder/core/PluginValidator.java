package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.jlog.award.AwardPlugin;
import com.jlog.plugin.ContestPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The plugin spec ({@code ARS_Suite/docs/PLUGIN_FORMAT.md}) encoded as executable
 * checks — the core value of the builder. Pure logic, no JavaFX, so it's unit-tested
 * directly. Operates on both the raw JSON tree (for presence-of-key checks like
 * vestigial fields) and the engine-parsed model (for typed checks).
 */
public final class PluginValidator {
    private PluginValidator() {}

    public enum Severity { ERROR, WARN, INFO }

    public record Issue(Severity severity, String field, String message) {
        @Override public String toString() { return severity + "  [" + field + "]  " + message; }
    }

    // ---- spec constants -------------------------------------------------
    /** multiplierType values with dedicated engine logic, plus the 4 generic ones. */
    static final Set<String> KNOWN_MULT_TYPES = Set.of(
        "sections", "dxcc", "states", "custom",
        "zone_country", "zone_country_state", "wpx_prefix", "state_prov_country",
        "grid_field", "wae", "qso_party", "all_asian", "russian_dx", "sac",
        "ari_dx", "wag", "oceania_dx");

    /** entryField ids that consume NO field1..5 DB slot. */
    static final Set<String> NO_SLOT_IDS = Set.of(
        "callsign", "serial_sent", "serial_rcvd", "band", "mode",
        "rst_sent", "rst_rcvd", "prec_sent", "check_sent", "sect_sent");

    /** Cabrillo tokens the exporter resolves without a matching entryField. */
    static final Set<String> CABRILLO_SPECIAL = Set.of(
        "callsign", "serial_sent", "serial_rcvd", "rst_sent", "rst_rcvd", "mycall",
        "band", "mode", "prec_sent", "check_sent", "sect_sent",
        "field1", "field2", "field3", "field4", "field5");

    static final Set<String> KNOWN_PANE_TYPES = Set.of(
        "dupe_checker", "section_tracker", "statistics", "us_state_map", "canada_map",
        "dxcc_list", "dxcc_map", "county_map", "county_list", "per_mode_mult_grid",
        "worked_before", "grid_map", "qtc", "ss_section_map", "sweep_progress", "worked_mults");

    static final Set<String> KNOWN_VALIDATORS = Set.of(
        "maidenhead", "maidenhead6", "numeric", "fd_class", "ss_check");

    static final Set<String> KNOWN_FIELD_TYPES = Set.of("text", "combo", "number", "checkbox");

    static final Set<String> KNOWN_MATCH_ON = Set.of(
        "state", "country", "callsign", "prefix", "dxccPrefix", "continent", "grid");

    // ---- contest --------------------------------------------------------
    public static List<Issue> validateContest(JsonNode root, ContestPlugin p) {
        List<Issue> out = new ArrayList<>();

        // identity
        if (p.getContestId() == null || p.getContestId().isBlank()) {
            out.add(new Issue(Severity.ERROR, "contestId", "contestId is required (it is the plugin id, the JSON filename, and the Cabrillo CONTEST: fallback)."));
        } else if (!p.getContestId().matches("[A-Za-z0-9_]+")) {
            out.add(new Issue(Severity.WARN, "contestId", "contestId has characters outside [A-Za-z0-9_]; it's used as a filename and as the Cabrillo token."));
        }

        // entry fields → field1..5 slot budget
        List<ContestPlugin.FieldDef> fields = p.getEntryFields() == null ? List.of() : p.getEntryFields();
        int slotUsers = 0;
        for (ContestPlugin.FieldDef f : fields) {
            String id = f.getId();
            if (id != null && !NO_SLOT_IDS.contains(id) && !f.isConstant()) slotUsers++;
            if (f.getType() != null && !KNOWN_FIELD_TYPES.contains(f.getType())) {
                out.add(new Issue(Severity.WARN, "entryFields[" + id + "].type", "type '" + f.getType() + "' is not recognized; only 'combo' changes behavior, everything else renders as a text field."));
            }
            if ("combo".equals(f.getType()) && (f.getOptions() == null || f.getOptions().isEmpty())) {
                out.add(new Issue(Severity.WARN, "entryFields[" + id + "].options", "combo field has no options[]."));
            }
            if (f.getValidator() != null && !KNOWN_VALIDATORS.contains(f.getValidator())) {
                out.add(new Issue(Severity.WARN, "entryFields[" + id + "].validator", "validator '" + f.getValidator() + "' is unknown and will be ignored (known: " + KNOWN_VALIDATORS + ")."));
            }
        }
        if (slotUsers > 5) {
            out.add(new Issue(Severity.ERROR, "entryFields", "uses " + slotUsers + " slot-consuming fields; the engine maps only field1..field5, so fields beyond the 5th are dropped."));
        }

        // cabrillo token resolvability
        Set<String> fieldIds = new java.util.HashSet<>();
        for (ContestPlugin.FieldDef f : fields) if (f.getId() != null) fieldIds.add(f.getId());
        checkCabrilloTokens(out, "cabrilloSent", p.getCabrilloSent(), fieldIds);
        checkCabrilloTokens(out, "cabrilloRcvd", p.getCabrilloRcvd(), fieldIds);

        // multiplierType
        String mt = p.getScoringRules() == null ? null : p.getScoringRules().getMultiplierType();
        if (mt != null && !KNOWN_MULT_TYPES.contains(mt)) {
            out.add(new Issue(Severity.WARN, "scoringRules.multiplierType", "'" + mt + "' is not a known multiplierType; the engine will fall back to the generic distinct-count path (possible typo)."));
        }

        // dupe flags — only the first in dispatch order is considered
        int dupeFlags = 0;
        if (p.isContestWideDupe()) dupeFlags++;
        if (p.isRoverAwareDupe()) dupeFlags++;
        if (p.isPerBandGridDupe()) dupeFlags++;
        if (p.isFieldDayModeDupe()) dupeFlags++;
        if (dupeFlags > 1) {
            out.add(new Issue(Severity.WARN, "dupe flags", "more than one dupe flag is set; the engine considers them in a fixed order (contestWide → roverAware → perBandGrid → fieldDayMode) and uses the first match."));
        }

        // panes
        List<ContestPlugin.PaneDef> panes = p.getRow2Panes() == null ? List.of() : p.getRow2Panes();
        for (ContestPlugin.PaneDef pane : panes) {
            if (pane.getPaneType() != null && !KNOWN_PANE_TYPES.contains(pane.getPaneType())) {
                out.add(new Issue(Severity.WARN, "row2Panes.paneType", "'" + pane.getPaneType() + "' is not a known pane type; it will render as an empty placeholder."));
            }
        }

        // vestigial keys (presence in the raw JSON)
        warnVestigial(out, root, "statistics", "statistics");
        JsonNode sr = root.path("scoringRules");
        warnVestigial(out, sr, "scoringRules.scoreFormula", "scoreFormula");
        warnVestigial(out, sr, "scoringRules.allowDupes", "allowDupes");
        forEachArrayMember(root, "row2Panes", n -> warnVestigial(out, n, "row2Panes[].paneIndex", "paneIndex"));
        forEachArrayMember(root, "entryFields", n -> warnVestigial(out, n, "entryFields[].required", "required"));

        return out;
    }

    private static void checkCabrilloTokens(List<Issue> out, String field, List<String> tokens, Set<String> fieldIds) {
        if (tokens == null) return;
        for (String t : tokens) {
            if (t == null) continue;
            if (!CABRILLO_SPECIAL.contains(t) && !fieldIds.contains(t)) {
                out.add(new Issue(Severity.ERROR, field, "token '" + t + "' matches no entryField id and is not a special Cabrillo token; it will resolve to an empty exchange value."));
            }
        }
    }

    // ---- award ----------------------------------------------------------
    public static List<Issue> validateAward(JsonNode root, AwardPlugin a) {
        List<Issue> out = new ArrayList<>();
        if (a.getAwardId() == null || a.getAwardId().isBlank()) {
            out.add(new Issue(Severity.ERROR, "awardId", "awardId is required."));
        }
        String m = a.getMatchOn();
        if (m != null && !KNOWN_MATCH_ON.contains(m)) {
            out.add(new Issue(Severity.WARN, "matchOn", "'" + m + "' is not recognized; it will match nothing (known: " + KNOWN_MATCH_ON + ")."));
        }
        boolean hasTargets = root.path("targets").isArray() && root.path("targets").size() > 0;
        boolean hasTiers = root.path("tiers").isArray() && root.path("tiers").size() > 0;
        if (!hasTargets && !hasTiers) {
            out.add(new Issue(Severity.WARN, "tiers", "count-match award (no targets[]) has no tiers[]; progress has no denominator."));
        }
        // tiers strictly ascending by threshold
        JsonNode tiers = root.path("tiers");
        if (tiers.isArray()) {
            int prev = Integer.MIN_VALUE;
            for (JsonNode t : tiers) {
                int thr = t.path("threshold").asInt(Integer.MIN_VALUE);
                if (thr <= prev) {
                    out.add(new Issue(Severity.WARN, "tiers", "tier thresholds should be strictly ascending; saw " + thr + " after " + prev + "."));
                    break;
                }
                prev = thr;
            }
        }
        return out;
    }

    // ---- helpers --------------------------------------------------------
    private static void warnVestigial(List<Issue> out, JsonNode node, String label, String key) {
        if (node != null && node.has(key)) {
            out.add(new Issue(Severity.WARN, label, "field '" + key + "' is vestigial (parsed but never used by the engine); safe to omit."));
        }
    }

    private static void forEachArrayMember(JsonNode root, String arrayField, java.util.function.Consumer<JsonNode> fn) {
        JsonNode arr = root.path(arrayField);
        if (arr.isArray()) for (JsonNode n : arr) fn.accept(n);
    }

    /** Convenience: count ERROR-severity issues. */
    public static long errorCount(List<Issue> issues) {
        return issues.stream().filter(i -> i.severity() == Severity.ERROR).count();
    }
}
