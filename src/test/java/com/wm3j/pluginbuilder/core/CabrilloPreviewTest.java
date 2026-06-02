package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CabrilloPreviewTest {

    @Test
    void headerUsesOverrideWhenSet() throws Exception {
        ObjectNode n = PluginIo.parse("{ \"contestId\":\"ARRL_RRU_SSB\", \"cabrilloContestName\":\"ARRL-RR-PH\" }");
        assertEquals("ARRL-RR-PH", CabrilloPreview.contestHeader(n));
    }

    @Test
    void headerDerivesFromContestIdWhenNoOverride() throws Exception {
        ObjectNode n = PluginIo.parse("{ \"contestId\":\"MY_COOL_TEST\" }");
        assertEquals("MY-COOL-TEST", CabrilloPreview.contestHeader(n));
    }

    @Test
    void exchangeLayoutShowsOrderedTokens() throws Exception {
        ObjectNode n = PluginIo.parse("{ \"cabrilloSent\":[\"rst_sent\",\"serial_sent\"] }");
        assertEquals("<rst_sent> <serial_sent>", CabrilloPreview.exchangeLayout(n, "cabrilloSent"));
        assertEquals("(none)", CabrilloPreview.exchangeLayout(n, "cabrilloRcvd"));
    }

    @Test
    void fullTextContainsHeaderAndBothExchanges() throws Exception {
        ObjectNode n = PluginIo.parse(Skeletons.contest());
        String t = CabrilloPreview.text(n);
        assertTrue(t.contains("CONTEST: MY-CONTEST"));
        assertTrue(t.contains("Sent exchange order:"));
        assertTrue(t.contains("Received exchange order:"));
    }
}
