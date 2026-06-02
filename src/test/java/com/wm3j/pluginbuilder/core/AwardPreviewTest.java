package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.award.AwardPlugin;
import com.jlog.award.AwardProgress;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AwardPreviewTest {

    @Test
    void countMatchProgressFormatsCountAndTopTier() throws Exception {
        // count-match award (no targets), top tier 50
        ObjectNode n = PluginIo.parse(
            "{ \"awardId\":\"WAS\", \"awardName\":\"Worked All States\", \"matchOn\":\"state\","
            + " \"targetLabel\":\"States\", \"tiers\":[{\"threshold\":50,\"name\":\"Basic\"}] }");
        AwardPlugin a = PluginIo.toAward(n);
        AwardProgress p = new AwardProgress(a,
            Set.of("CT", "MA", "ME", "NH", "VT", "RI", "NY", "NJ", "PA", "OH"),  // 10 worked
            Set.of(), Set.of(), List.of());

        String text = AwardPreview.format(p);
        assertTrue(text.contains("States: 10 / 50"), text);
        assertTrue(text.contains("20%"), text);                 // 10/50
        assertTrue(text.contains("none yet"), text);            // 10 < 50 → no tier
        assertTrue(text.contains("count-match"), text);
    }

    @Test
    void tierIsReportedWhenThresholdMet() throws Exception {
        ObjectNode n = PluginIo.parse(
            "{ \"awardId\":\"WPX\", \"matchOn\":\"prefix\","
            + " \"tiers\":[{\"threshold\":100,\"name\":\"100\"},{\"threshold\":300,\"name\":\"300\"}] }");
        AwardPlugin a = PluginIo.toAward(n);
        // 120 distinct prefixes
        var worked = new java.util.HashSet<String>();
        for (int i = 0; i < 120; i++) worked.add("PX" + i);
        AwardProgress p = new AwardProgress(a, worked, Set.of(), Set.of(), List.of());

        String text = AwardPreview.format(p);
        assertTrue(text.contains("Current tier: 100 (100)"), text);
    }
}
