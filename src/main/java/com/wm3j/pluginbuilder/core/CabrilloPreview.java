package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Previews the Cabrillo header + exchange layout a contest plugin will produce —
 * the most error-prone contest output (e.g. a wrong CONTEST: name). The header
 * rule mirrors the engine's CabrilloExporter exactly (cabrilloContestName when
 * set, else contestId with underscores → hyphens). The exchange line is a
 * positional template (token names in order), not a scored line — a faithful
 * score/multiplier preview needs the engine scoring refactor.
 */
public final class CabrilloPreview {
    private CabrilloPreview() {}

    /** The exact CONTEST: header the exporter will emit. */
    public static String contestHeader(JsonNode root) {
        String c = root.path("cabrilloContestName").asText("");
        if (c.isBlank()) c = root.path("contestId").asText("").replace("_", "-");
        return c;
    }

    /** Ordered exchange tokens joined for display ("(none)" if empty). */
    public static String exchangeLayout(JsonNode root, String key) {
        JsonNode a = root.path(key);
        if (!a.isArray() || a.size() == 0) return "(none)";
        StringBuilder b = new StringBuilder();
        for (JsonNode n : a) { if (b.length() > 0) b.append(' '); b.append('<').append(n.asText()).append('>'); }
        return b.toString();
    }

    public static String text(JsonNode root) {
        String sent = exchangeLayout(root, "cabrilloSent");
        String rcvd = exchangeLayout(root, "cabrilloRcvd");
        return  "CONTEST: " + contestHeader(root) + "\n"
              + "CALLSIGN: <your call>\n"
              + "\n"
              + "Sent exchange order:      " + sent + "\n"
              + "Received exchange order:  " + rcvd + "\n"
              + "\n"
              + "Example QSO line:\n"
              + "  QSO: <freq> <mode> <date> <time> <your call> " + sent
              + " <their call> " + rcvd + "\n"
              + "\n"
              + "— Header is exact (matches the engine). The exchange line shows field\n"
              + "  ORDER, not scored values. Full score/multiplier preview is pending the\n"
              + "  engine scoring refactor (contest scoring lives in j-log's controller).";
    }
}
