package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wm3j.pluginbuilder.core.AwardCodec.Target;
import com.wm3j.pluginbuilder.core.AwardCodec.Tier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AwardCodecTest {

    @Test
    void readsSkeleton() throws Exception {
        ObjectNode root = PluginIo.parse(Skeletons.award());
        AwardModel m = AwardCodec.read(root);
        assertEquals("MY_AWARD", m.awardId);
        assertEquals("state", m.matchOn);
        assertEquals("States", m.targetLabel);
        List<Tier> tiers = AwardCodec.readTiers(root);
        assertEquals(1, tiers.size());
        assertEquals(50, tiers.get(0).threshold());
        assertEquals("Basic", tiers.get(0).name());
    }

    @Test
    void scalarsApplyAndRemoveOnBlank() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"awardId\":\"A\", \"description\":\"x\" }");
        AwardModel m = AwardCodec.read(root);
        m.awardName = "My Award";
        m.matchOn = "country";
        m.description = "";            // clear
        AwardCodec.apply(root, m);
        assertEquals("My Award", root.path("awardName").asText());
        assertEquals("country", root.path("matchOn").asText());
        assertFalse(root.has("description"), "blank scalar is removed");
    }

    @Test
    void optionsAndWindowMergeAndCollapse() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"awardId\":\"A\" }");
        AwardModel m = AwardCodec.read(root);
        m.confirmedOnly = true;
        m.windowStart = "2026-07-01T00:00:00";
        AwardCodec.apply(root, m);
        assertTrue(root.path("options").path("confirmedOnly").asBoolean());
        assertFalse(root.path("options").has("matchBaseCallsign"), "false flag omitted");
        assertEquals("2026-07-01T00:00:00", root.path("window").path("startUtc").asText());
        assertFalse(root.path("window").has("endUtc"));

        // clearing both options collapses the object
        m.confirmedOnly = false;
        AwardCodec.apply(root, m);
        assertFalse(root.has("options"), "empty options object is removed");
    }

    @Test
    void applyPreservesUnmanagedKeys() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"awardId\":\"A\", \"x_note\":\"keep\" }");
        AwardCodec.apply(root, AwardCodec.read(root));
        assertEquals("keep", root.path("x_note").asText());
    }

    @Test
    void targetsTextRoundTrip() {
        List<Target> t = AwardCodec.parseTargets("CT | Connecticut\nMA\n  ME |  Maine \n\n");
        assertEquals(3, t.size());
        assertEquals("CT", t.get(0).id());
        assertEquals("Connecticut", t.get(0).label());
        assertEquals("MA", t.get(1).id());
        assertEquals("", t.get(1).label());
        assertEquals("Maine", t.get(2).label());
        // round-trip through JSON
        ObjectNode root = PluginIo.mapper().createObjectNode();
        AwardCodec.writeTargets(root, "targets", t);
        assertEquals(3, root.path("targets").size());
        assertEquals("Connecticut", root.path("targets").get(0).path("label").asText());
    }

    @Test
    void tiersSkipMalformedLines() {
        List<Tier> tiers = AwardCodec.parseTiers("50 | Bronze\nnotanumber | x\n100 | Silver");
        assertEquals(2, tiers.size());
        assertEquals(100, tiers.get(1).threshold());
        assertEquals("Silver", tiers.get(1).name());
    }

    @Test
    void emptyTargetsRemovesKey() {
        ObjectNode root = PluginIo.mapper().createObjectNode();
        root.putArray("targets");
        AwardCodec.writeTargets(root, "targets", List.of());
        assertFalse(root.has("targets"));
    }
}
