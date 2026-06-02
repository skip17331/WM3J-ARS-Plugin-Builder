package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.plugin.ContestPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PluginIoTest {

    @Test
    void roundTripPreservesUnmodeledKeys(@TempDir Path dir) throws Exception {
        ObjectNode n = PluginIo.parse(Skeletons.contest());
        n.put("x_authoring_note", "keep me");          // a key the engine model doesn't have
        Path written = PluginIo.save(n, dir, "contestId");

        assertTrue(written.getFileName().toString().equals("MY_CONTEST.json"));
        ObjectNode reloaded = PluginIo.read(written);
        assertEquals("MY_CONTEST", reloaded.path("contestId").asText());
        assertEquals("keep me", reloaded.path("x_authoring_note").asText(),
            "editing via the JSON tree must not drop fields the builder doesn't model");
    }

    @Test
    void saveSanitizesFilename(@TempDir Path dir) throws Exception {
        ObjectNode n = PluginIo.parse("{ \"contestId\": \"weird id/../x\" }");
        Path written = PluginIo.save(n, dir, "contestId");
        assertFalse(written.getFileName().toString().contains("/"));
        assertTrue(written.getFileName().toString().endsWith(".json"));
    }

    @Test
    void blankIdRefusesSave(@TempDir Path dir) throws Exception {
        ObjectNode n = PluginIo.parse("{ \"contestId\": \"\" }");
        assertThrows(Exception.class, () -> PluginIo.save(n, dir, "contestId"));
    }

    @Test
    void readResourceParsesIntoModel() throws Exception {
        ObjectNode n = PluginIo.readResource("/com/jlog/plugins/arrl_dx_cw_us.json");
        ContestPlugin p = PluginIo.toContest(n);
        assertEquals("ARRL_DX_CW_US", p.getContestId());
        assertEquals("ARRL-DX-CW", p.getCabrilloContestName());
    }
}
