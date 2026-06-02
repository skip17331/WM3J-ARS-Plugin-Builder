package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts the {@code entryFields} JSON array to/from {@link FieldRow}s.
 * Writing omits defaults (0 width, entryRow 0, false booleans, empty/blank
 * strings, empty options) so the output stays clean, and never emits the
 * vestigial {@code required} flag.
 */
public final class FieldsCodec {
    private FieldsCodec() {}
    private static final JsonNodeFactory NF = JsonNodeFactory.instance;

    public static List<FieldRow> toRows(JsonNode entryFieldsArray) {
        List<FieldRow> rows = new ArrayList<>();
        if (entryFieldsArray == null || !entryFieldsArray.isArray()) return rows;
        for (JsonNode n : entryFieldsArray) {
            FieldRow r = new FieldRow();
            r.id        = n.path("id").asText("");
            r.label     = n.path("label").asText("");
            r.type      = n.path("type").asText("");
            r.entryRow  = n.path("entryRow").asInt(0);
            r.width     = n.path("width").asInt(0);
            r.validator = n.path("validator").asText("");
            r.autoIncrement = n.path("autoIncrement").asBoolean(false);
            r.persistent    = n.path("persistent").asBoolean(false);
            r.constant      = n.path("constant").asBoolean(false);
            JsonNode opts = n.path("options");
            if (opts.isArray()) for (JsonNode o : opts) r.options.add(o.asText());
            rows.add(r);
        }
        return rows;
    }

    public static ArrayNode toArray(List<FieldRow> rows) {
        ArrayNode arr = NF.arrayNode();
        for (FieldRow r : rows) {
            ObjectNode o = NF.objectNode();
            if (notBlank(r.id))    o.put("id", r.id.trim());
            if (notBlank(r.label)) o.put("label", r.label);
            if (notBlank(r.type) && !"text".equals(r.type)) o.put("type", r.type);
            if (r.entryRow != 0)   o.put("entryRow", r.entryRow);
            if (r.width > 0)       o.put("width", r.width);
            if (r.options != null && !r.options.isEmpty()) {
                ArrayNode opts = o.putArray("options");
                for (String s : r.options) if (s != null && !s.isBlank()) opts.add(s.trim());
            }
            if (notBlank(r.validator)) o.put("validator", r.validator);
            if (r.autoIncrement) o.put("autoIncrement", true);
            if (r.persistent)    o.put("persistent", true);
            if (r.constant)      o.put("constant", true);
            arr.add(o);
        }
        return arr;
    }

    /** Parse a comma/space/newline-separated options string into a list. */
    public static List<String> parseOptions(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String s : text.split("[,\\s]+")) if (!s.isBlank()) out.add(s.trim());
        return out;
    }

    public static String optionsToText(List<String> options) {
        return options == null ? "" : String.join(", ", options);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
}
