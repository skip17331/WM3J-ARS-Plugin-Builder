package com.wm3j.pluginbuilder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jlog.plugin.ContestPlugin;
import com.jlog.award.AwardPlugin;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the j-log-engine dependency is wired without needing a display or the
 * SQLite DBs: parse a known bundled plugin resource (shipped inside the engine
 * jar) straight into the engine's model classes via Jackson.
 */
class EngineWiringTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void contestModelParsesABundledPlugin() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/com/jlog/plugins/arrl_dx_cw_us.json")) {
            assertNotNull(in, "bundled contest plugin resource should be on the classpath from j-log-engine");
            ContestPlugin p = mapper.readValue(in, ContestPlugin.class);
            assertEquals("ARRL_DX_CW_US", p.getContestId());
            assertEquals("ARRL-DX-CW", p.getCabrilloContestName());
        }
    }

    @Test
    void awardModelParsesABundledAward() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/com/jlog/awards/was.json")) {
            assertNotNull(in, "bundled award resource should be on the classpath from j-log-engine");
            AwardPlugin a = mapper.readValue(in, AwardPlugin.class);
            assertNotNull(a.getAwardId());
            assertEquals("state", a.getMatchOn());
        }
    }
}
