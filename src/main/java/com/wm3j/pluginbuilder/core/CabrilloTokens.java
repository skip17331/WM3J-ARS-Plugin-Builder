package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * The Cabrillo exchange mapping ({@code cabrilloSent} / {@code cabrilloRcvd}) is
 * an ordered list of tokens the exporter resolves. This computes the set of
 * <em>valid</em> tokens for a given plugin (its declared entryField ids + the
 * special tokens the exporter understands) so the picker can only offer
 * resolvable tokens — the inverse of {@link PluginValidator}'s token check.
 */
public final class CabrilloTokens {
    private CabrilloTokens() {}

    /** Special tokens the exporter resolves without a matching entryField (ordered for the palette). */
    public static final List<String> SPECIAL = List.of(
        "callsign", "rst_sent", "rst_rcvd", "serial_sent", "serial_rcvd",
        "band", "mode", "mycall", "prec_sent", "check_sent", "sect_sent",
        "field1", "field2", "field3", "field4", "field5");

    /** Selectable tokens: declared entryField ids first (declaration order), then the specials, deduped. */
    public static List<String> available(JsonNode root) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        JsonNode ef = root == null ? null : root.path("entryFields");
        if (ef != null && ef.isArray()) {
            for (JsonNode f : ef) {
                String id = f.path("id").asText("");
                if (!id.isBlank()) set.add(id);
            }
        }
        set.addAll(SPECIAL);
        return new ArrayList<>(set);
    }

    public static List<String> readList(JsonNode root, String key) {
        List<String> out = new ArrayList<>();
        JsonNode a = root == null ? null : root.path(key);
        if (a != null && a.isArray()) for (JsonNode n : a) out.add(n.asText());
        return out;
    }

    /** Write the ordered token list back; removes the key entirely when empty. */
    public static void writeList(ObjectNode root, String key, List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) { root.remove(key); return; }
        ArrayNode arr = root.arrayNode();
        for (String t : tokens) if (t != null && !t.isBlank()) arr.add(t.trim());
        root.set(key, arr);
    }
}
