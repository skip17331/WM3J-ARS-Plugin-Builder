package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.ContestScore;
import com.jlog.scoring.StationContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContestScorePreviewTest {

    private final ObjectMapper M = new ObjectMapper();
    private ContestPlugin plugin(String json) throws Exception { return M.readValue(json, ContestPlugin.class); }
    private final StationContext ctx = StationContext.of("WM3J", "FM19", "");

    @Test void parse_mapsPositionalSlots_skipsCommentsAndBlanks() {
        List<QsoRecord> qs = ContestScorePreview.parse(
            "W1AW, 20m, CW, 05, MA\n# a comment\n\nDL1ABC 40m SSB 14\n");
        assertEquals(2, qs.size());
        QsoRecord a = qs.get(0);
        assertEquals("W1AW", a.getCallsign());   // setter upper-trims
        assertEquals("20m", a.getBand());
        assertEquals("CW", a.getMode());
        assertEquals("05", a.getContestField1());
        assertEquals("MA", a.getContestField2());
        QsoRecord b = qs.get(1);
        assertEquals("DL1ABC", b.getCallsign());
        assertEquals("40m", b.getBand());
        assertEquals("SSB", b.getMode());
        assertEquals("14", b.getContestField1());
    }

    @Test void compute_pointsByBand_distinctMults_dupeExcluded() throws Exception {
        ContestPlugin pl = plugin("{\"contestId\":\"X\","
            + "\"entryFields\":[{\"id\":\"callsign\"},{\"id\":\"band\"},{\"id\":\"mode\"},{\"id\":\"sect_rcvd\"}],"
            + "\"scoringRules\":{\"pointsByBand\":{\"20m\":2,\"40m\":3}},"
            + "\"multiplierModel\":{\"field\":\"sect_rcvd\"}}");
        List<QsoRecord> qsos = ContestScorePreview.parse(
            "W1AW, 20m, CW, EMA\nK5XYZ, 40m, CW, WTX\nW1AW, 20m, CW, EMA");
        ContestScore s = ContestScorePreview.compute(pl, qsos, ctx);
        assertEquals(2, s.qsoCount());     // 3rd is a dupe
        assertEquals(5, s.points());       // 2 + 3 (dupe excluded)
        assertEquals(2, s.mults());        // distinct sections {EMA, WTX}
        assertEquals(10, s.score());       // 5 × 2
        assertFalse(qsos.get(0).isDupe());
        assertTrue(qsos.get(2).isDupe());  // same call/band/mode as #1
        assertEquals(2, qsos.get(0).getPoints());
    }

    @Test void format_rendersTotalsAndDupeMarker() throws Exception {
        ContestPlugin pl = plugin("{\"contestId\":\"X\","
            + "\"entryFields\":[{\"id\":\"callsign\"},{\"id\":\"band\"},{\"id\":\"mode\"},{\"id\":\"sect_rcvd\"}],"
            + "\"scoringRules\":{\"pointsByBand\":{\"20m\":2,\"40m\":3}},"
            + "\"multiplierModel\":{\"field\":\"sect_rcvd\"}}");
        List<QsoRecord> qsos = ContestScorePreview.parse(
            "W1AW, 20m, CW, EMA\nK5XYZ, 40m, CW, WTX\nW1AW, 20m, CW, EMA");
        String text = ContestScorePreview.format(pl, qsos, ctx);
        assertTrue(text.contains("SCORE: 10"), text);
        assertTrue(text.contains("Multipliers: 2"), text);
        assertTrue(text.contains("dupe"), text);
    }

    @Test void format_emptyLog_returnsTemplate() throws Exception {
        ContestPlugin pl = plugin("{\"contestId\":\"X\","
            + "\"entryFields\":[{\"id\":\"callsign\"},{\"id\":\"band\"},{\"id\":\"mode\"},{\"id\":\"sect_rcvd\"}]}");
        String text = ContestScorePreview.format(pl, List.of(), ctx);
        assertTrue(text.contains("Add sample QSOs"), text);
        assertTrue(text.contains("sect_rcvd"), text);   // slot named in the template header
    }
}
