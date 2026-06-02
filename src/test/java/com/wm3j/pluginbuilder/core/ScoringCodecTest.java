package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScoringCodecTest {

    @Test
    void readsSkeleton() throws Exception {
        ObjectNode root = PluginIo.parse(Skeletons.contest());
        ScoringModel m = ScoringCodec.read(root);
        assertEquals("dxcc", m.multiplierType);
        assertEquals(1, m.pointsPerQso);
        assertEquals(ScoringModel.Dupe.DEFAULT, m.dupe);
        assertEquals("callsign", m.multField);
        assertFalse(m.multPerBand);
    }

    @Test
    void dupeIsSingleSelect() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"contestId\":\"X\", \"contestWideDupe\":true }");
        ScoringModel m = ScoringCodec.read(root);
        assertEquals(ScoringModel.Dupe.CONTEST_WIDE, m.dupe);

        m.dupe = ScoringModel.Dupe.PER_BAND_GRID;
        ScoringCodec.apply(root, m);
        assertFalse(root.has("contestWideDupe"), "old dupe flag must be cleared");
        assertTrue(root.path("perBandGridDupe").asBoolean());
        // and only one flag total
        long flags = java.util.Arrays.stream(ScoringModel.Dupe.values())
            .filter(d -> d.jsonKey != null && root.path(d.jsonKey).asBoolean(false)).count();
        assertEquals(1, flags);
    }

    @Test
    void defaultDupeWritesNoFlag() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"contestId\":\"X\", \"roverAwareDupe\":true }");
        ScoringModel m = ScoringCodec.read(root);
        m.dupe = ScoringModel.Dupe.DEFAULT;
        ScoringCodec.apply(root, m);
        assertFalse(root.has("roverAwareDupe"));
        assertFalse(root.has("contestWideDupe"));
    }

    @Test
    void applyPreservesUnmanagedNestedKeys() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"contestId\":\"X\","
            + " \"multiplierModel\":{\"field\":\"sect_rcvd\",\"validValues\":[\"CT\",\"MA\"]},"
            + " \"scoringRules\":{\"multiplierType\":\"sections\",\"pointsByBand\":{\"6m\":2}} }");
        ScoringModel m = ScoringCodec.read(root);
        m.pointsPerQso = 3;          // change something the form owns
        ScoringCodec.apply(root, m);

        // form-owned change took effect
        assertEquals(3, root.path("scoringRules").path("pointsPerQso").asInt());
        // unmanaged keys survived
        assertEquals(2, root.path("scoringRules").path("pointsByBand").path("6m").asInt(),
            "pointsByBand must survive a scoring-form apply");
        assertTrue(root.path("multiplierModel").path("validValues").isArray(),
            "multiplierModel.validValues must survive");
        assertEquals("sect_rcvd", root.path("multiplierModel").path("field").asText());
    }

    @Test
    void roundTrip() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"contestId\":\"X\" }");
        ScoringModel m = new ScoringModel();
        m.dupe = ScoringModel.Dupe.FIELD_DAY;
        m.multiplierType = "qso_party";
        m.pointsPerQso = 2;
        m.scoreIsPointsOnly = true;
        m.perModeMultipliers = true;
        m.multField = "field1";
        m.multPerBand = true;
        ScoringCodec.apply(root, m);

        ScoringModel back = ScoringCodec.read(root);
        assertEquals(ScoringModel.Dupe.FIELD_DAY, back.dupe);
        assertEquals("qso_party", back.multiplierType);
        assertEquals(2, back.pointsPerQso);
        assertTrue(back.scoreIsPointsOnly);
        assertTrue(back.perModeMultipliers);
        assertEquals("field1", back.multField);
        assertTrue(back.multPerBand);
    }

    @Test
    void emptyModelLeavesNoEmptyContainers() throws Exception {
        ObjectNode root = PluginIo.parse("{ \"contestId\":\"X\" }");
        ScoringCodec.apply(root, new ScoringModel());   // all defaults
        assertFalse(root.has("scoringRules"), "no scoring knobs set → no empty scoringRules object");
        assertFalse(root.has("multiplierModel"));
    }
}
