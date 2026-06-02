package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CabrilloTokensTest {

    @Test
    void availableIncludesFieldIdsAndSpecialsDeduped() throws Exception {
        ObjectNode plugin = PluginIo.parse(Skeletons.contest());
        List<String> tokens = CabrilloTokens.available(plugin);
        assertTrue(tokens.contains("callsign"));
        assertTrue(tokens.contains("serial_rcvd"));   // a declared field id
        assertTrue(tokens.contains("band"));          // a pure special
        assertTrue(tokens.contains("field1"));        // direct slot token
        assertEquals(1, tokens.stream().filter("callsign"::equals).count(),
            "callsign is both a field id and a special — must appear once");
        // declared field ids come before pure specials
        assertTrue(tokens.indexOf("callsign") < tokens.indexOf("band"));
    }

    @Test
    void readsSkeletonMapping() throws Exception {
        ObjectNode plugin = PluginIo.parse(Skeletons.contest());
        assertEquals(List.of("rst_sent", "serial_sent"), CabrilloTokens.readList(plugin, "cabrilloSent"));
        assertEquals(List.of("rst_rcvd", "serial_rcvd"), CabrilloTokens.readList(plugin, "cabrilloRcvd"));
    }

    @Test
    void writeRoundTripAndEmptyRemovesKey() throws Exception {
        ObjectNode plugin = PluginIo.parse("{ \"contestId\":\"X\" }");
        CabrilloTokens.writeList(plugin, "cabrilloSent", List.of("rst_sent", "field1"));
        assertEquals(List.of("rst_sent", "field1"), CabrilloTokens.readList(plugin, "cabrilloSent"));

        CabrilloTokens.writeList(plugin, "cabrilloSent", List.of());
        assertFalse(plugin.has("cabrilloSent"), "empty mapping should drop the key, not write []");
    }

    @Test
    void pickerProducesOnlyValidatorCleanTokens() throws Exception {
        // Build a mapping purely from available() tokens, then assert the validator
        // raises no token ERRORs — the picker can't produce an unresolvable token.
        ObjectNode plugin = PluginIo.parse(Skeletons.contest());
        List<String> avail = CabrilloTokens.available(plugin);
        CabrilloTokens.writeList(plugin, "cabrilloRcvd", avail);   // worst case: all of them
        var issues = PluginValidator.validateContest(plugin, PluginIo.toContest(plugin));
        boolean tokenError = issues.stream().anyMatch(i ->
            i.severity() == PluginValidator.Severity.ERROR && i.field().startsWith("cabrillo"));
        assertFalse(tokenError, "tokens chosen from available() must never be flagged unresolvable: " + issues);
    }
}
