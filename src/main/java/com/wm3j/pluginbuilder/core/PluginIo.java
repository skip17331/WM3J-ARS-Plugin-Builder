package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.award.AwardPlugin;
import com.jlog.plugin.ContestPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Faithful load/save of plugin JSON. The editor works on the JSON tree
 * ({@link ObjectNode}) so fields the builder doesn't model yet survive a
 * round-trip; the engine model is only used for validation/preview.
 */
public final class PluginIo {

    /** Pretty, lenient mapper; never fails on unknown keys (matches the engine). */
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private PluginIo() {}

    public static ObjectMapper mapper() { return MAPPER; }

    // ---- load -----------------------------------------------------------
    public static ObjectNode read(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) { return readTree(in); }
    }

    /** Read a bundled plugin from the engine jar, e.g. "/com/jlog/plugins/arrl_dx_cw_us.json". */
    public static ObjectNode readResource(String classpath) throws IOException {
        try (InputStream in = PluginIo.class.getResourceAsStream(classpath)) {
            if (in == null) throw new IOException("resource not found: " + classpath);
            return readTree(in);
        }
    }

    public static ObjectNode parse(String json) throws IOException {
        JsonNode n = MAPPER.readTree(json);
        if (!(n instanceof ObjectNode)) throw new IOException("plugin root must be a JSON object");
        return (ObjectNode) n;
    }

    private static ObjectNode readTree(InputStream in) throws IOException {
        JsonNode n = MAPPER.readTree(in);
        if (!(n instanceof ObjectNode)) throw new IOException("plugin root must be a JSON object");
        return (ObjectNode) n;
    }

    // ---- model views for validation/preview ----------------------------
    public static ContestPlugin toContest(JsonNode node) throws IOException {
        return MAPPER.treeToValue(node, ContestPlugin.class);
    }

    public static AwardPlugin toAward(JsonNode node) throws IOException {
        return MAPPER.treeToValue(node, AwardPlugin.class);
    }

    // ---- save -----------------------------------------------------------
    /** Default drop-in dirs the suite auto-loads. */
    public static Path userContestDir() { return jlog("plugins"); }
    public static Path userAwardDir()   { return jlog("awards"); }
    private static Path jlog(String sub) {
        return Paths.get(System.getProperty("user.home"), ".j-log", sub);
    }

    /** Write {@code <id>.json} into {@code dir}; returns the path written. */
    public static Path save(JsonNode node, Path dir, String idField) throws IOException {
        String id = node.path(idField).asText("");
        if (id.isBlank()) throw new IOException(idField + " is required to name the file");
        Files.createDirectories(dir);
        Path target = dir.resolve(sanitize(id) + ".json");
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(target.toFile(), node);
        return target;
    }

    public static Path saveContest(JsonNode node) throws IOException {
        return save(node, userContestDir(), "contestId");
    }
    public static Path saveAward(JsonNode node) throws IOException {
        return save(node, userAwardDir(), "awardId");
    }

    private static String sanitize(String id) {
        return id.replaceAll("[^A-Za-z0-9_.-]", "_");
    }
}
