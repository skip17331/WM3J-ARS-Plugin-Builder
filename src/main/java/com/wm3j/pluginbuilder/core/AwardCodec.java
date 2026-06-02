package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads/writes award JSON. Scalars/options/window are merged (unmanaged keys
 * survive); targets/bonus ({id,label}) and tiers ({threshold,name}) are edited
 * as newline-delimited text ("id | label", "threshold | name") — robust for
 * large pasted lists (WAS=50, DXCC=217).
 */
public final class AwardCodec {
    private AwardCodec() {}

    public static final List<String> MATCH_ON = List.of(
        "", "state", "country", "callsign", "prefix", "dxccPrefix", "continent", "grid");

    public record Target(String id, String label) {}
    public record Tier(int threshold, String name) {}

    // ---- scalars --------------------------------------------------------
    public static AwardModel read(ObjectNode root) {
        AwardModel m = new AwardModel();
        m.awardId     = root.path("awardId").asText("");
        m.awardName   = root.path("awardName").asText("");
        m.description = root.path("description").asText("");
        m.matchOn     = root.path("matchOn").asText("");
        m.targetLabel = root.path("targetLabel").asText("");
        m.matchBaseCallsign = root.path("options").path("matchBaseCallsign").asBoolean(false);
        m.confirmedOnly     = root.path("options").path("confirmedOnly").asBoolean(false);
        m.windowStart = root.path("window").path("startUtc").asText("");
        m.windowEnd   = root.path("window").path("endUtc").asText("");
        return m;
    }

    public static void apply(ObjectNode root, AwardModel m) {
        setStr(root, "awardId", m.awardId);
        setStr(root, "awardName", m.awardName);
        setStr(root, "description", m.description);
        setStr(root, "matchOn", m.matchOn);
        setStr(root, "targetLabel", m.targetLabel);

        ObjectNode opts = child(root, "options");
        flag(opts, "matchBaseCallsign", m.matchBaseCallsign);
        flag(opts, "confirmedOnly", m.confirmedOnly);
        if (opts.isEmpty()) root.remove("options");

        ObjectNode win = child(root, "window");
        setStr(win, "startUtc", m.windowStart);
        setStr(win, "endUtc", m.windowEnd);
        if (win.isEmpty()) root.remove("window");
    }

    // ---- targets / bonus (id,label) ------------------------------------
    public static List<Target> readTargets(JsonNode root, String key) {
        List<Target> out = new ArrayList<>();
        JsonNode a = root.path(key);
        if (a.isArray()) for (JsonNode n : a) out.add(new Target(n.path("id").asText(""), n.path("label").asText("")));
        return out;
    }

    public static void writeTargets(ObjectNode root, String key, List<Target> targets) {
        if (targets == null || targets.isEmpty()) { root.remove(key); return; }
        ArrayNode arr = root.arrayNode();
        for (Target t : targets) {
            if (t.id() == null || t.id().isBlank()) continue;
            ObjectNode o = arr.objectNode();
            o.put("id", t.id().trim());
            if (t.label() != null && !t.label().isBlank()) o.put("label", t.label().trim());
            arr.add(o);
        }
        if (arr.isEmpty()) root.remove(key); else root.set(key, arr);
    }

    public static List<Target> parseTargets(String text) {
        List<Target> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\\R")) {
            if (line.isBlank()) continue;
            int bar = line.indexOf('|');
            String id = (bar < 0 ? line : line.substring(0, bar)).trim();
            String label = bar < 0 ? "" : line.substring(bar + 1).trim();
            if (!id.isEmpty()) out.add(new Target(id, label));
        }
        return out;
    }

    public static String targetsToText(List<Target> targets) {
        StringBuilder b = new StringBuilder();
        for (Target t : targets) {
            b.append(t.id());
            if (t.label() != null && !t.label().isBlank()) b.append(" | ").append(t.label());
            b.append('\n');
        }
        return b.toString();
    }

    // ---- tiers (threshold,name) ----------------------------------------
    public static List<Tier> readTiers(JsonNode root) {
        List<Tier> out = new ArrayList<>();
        JsonNode a = root.path("tiers");
        if (a.isArray()) for (JsonNode n : a) out.add(new Tier(n.path("threshold").asInt(0), n.path("name").asText("")));
        return out;
    }

    public static void writeTiers(ObjectNode root, List<Tier> tiers) {
        if (tiers == null || tiers.isEmpty()) { root.remove("tiers"); return; }
        ArrayNode arr = root.arrayNode();
        for (Tier t : tiers) {
            ObjectNode o = arr.objectNode();
            o.put("threshold", t.threshold());
            if (t.name() != null && !t.name().isBlank()) o.put("name", t.name().trim());
            arr.add(o);
        }
        root.set("tiers", arr);
    }

    public static List<Tier> parseTiers(String text) {
        List<Tier> out = new ArrayList<>();
        if (text == null) return out;
        for (String line : text.split("\\R")) {
            if (line.isBlank()) continue;
            int bar = line.indexOf('|');
            String thr = (bar < 0 ? line : line.substring(0, bar)).trim();
            String name = bar < 0 ? "" : line.substring(bar + 1).trim();
            try { out.add(new Tier(Integer.parseInt(thr), name)); }
            catch (NumberFormatException ignore) { /* skip malformed line */ }
        }
        return out;
    }

    public static String tiersToText(List<Tier> tiers) {
        StringBuilder b = new StringBuilder();
        for (Tier t : tiers) b.append(t.threshold()).append(" | ").append(t.name() == null ? "" : t.name()).append('\n');
        return b.toString();
    }

    // ---- helpers --------------------------------------------------------
    private static ObjectNode child(ObjectNode parent, String key) {
        if (parent.path(key).isObject()) return (ObjectNode) parent.get(key);
        ObjectNode n = parent.objectNode();
        parent.set(key, n);
        return n;
    }
    private static void flag(ObjectNode n, String key, boolean v) { if (v) n.put(key, true); else n.remove(key); }
    private static void setStr(ObjectNode n, String key, String v) {
        if (v != null && !v.isBlank()) n.put(key, v.trim()); else n.remove(key);
    }
}
