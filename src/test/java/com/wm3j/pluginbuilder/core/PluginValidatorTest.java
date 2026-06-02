package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.plugin.ContestPlugin;
import com.wm3j.pluginbuilder.core.PluginValidator.Issue;
import com.wm3j.pluginbuilder.core.PluginValidator.Severity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PluginValidatorTest {

    private List<Issue> validate(String json) throws Exception {
        ObjectNode n = PluginIo.parse(json);
        ContestPlugin p = PluginIo.toContest(n);
        return PluginValidator.validateContest(n, p);
    }

    private boolean has(List<Issue> issues, Severity sev, String fieldContains) {
        return issues.stream().anyMatch(i -> i.severity() == sev && i.field().contains(fieldContains));
    }

    @Test
    void skeletonHasNoErrors() throws Exception {
        List<Issue> issues = validate(Skeletons.contest());
        assertEquals(0, PluginValidator.errorCount(issues), "skeleton should be error-free: " + issues);
    }

    @Test
    void blankContestIdIsError() throws Exception {
        List<Issue> issues = validate("{ \"contestId\": \"\" }");
        assertTrue(has(issues, Severity.ERROR, "contestId"));
    }

    @Test
    void weirdContestIdCharsWarn() throws Exception {
        List<Issue> issues = validate("{ \"contestId\": \"My Contest!\" }");
        assertTrue(has(issues, Severity.WARN, "contestId"));
    }

    @Test
    void moreThanFiveSlotFieldsIsError() throws Exception {
        String json = "{ \"contestId\":\"X\", \"entryFields\":["
            + "{\"id\":\"a\"},{\"id\":\"b\"},{\"id\":\"c\"},"
            + "{\"id\":\"d\"},{\"id\":\"e\"},{\"id\":\"f\"}] }";
        assertTrue(has(validate(json), Severity.ERROR, "entryFields"));
    }

    @Test
    void specialAndConstantFieldsDoNotCountTowardSlots() throws Exception {
        // 5 real slots + callsign/rst (no slot) + a constant → still OK
        String json = "{ \"contestId\":\"X\", \"entryFields\":["
            + "{\"id\":\"callsign\"},{\"id\":\"rst_rcvd\"},"
            + "{\"id\":\"a\"},{\"id\":\"b\"},{\"id\":\"c\"},{\"id\":\"d\"},{\"id\":\"e\"},"
            + "{\"id\":\"myExch\",\"constant\":true}] }";
        assertEquals(0, PluginValidator.errorCount(validate(json)));
    }

    @Test
    void comboWithoutOptionsWarns() throws Exception {
        String json = "{ \"contestId\":\"X\", \"entryFields\":[{\"id\":\"a\",\"type\":\"combo\"}] }";
        assertTrue(has(validate(json), Severity.WARN, "options"));
    }

    @Test
    void unknownValidatorWarns() throws Exception {
        String json = "{ \"contestId\":\"X\", \"entryFields\":[{\"id\":\"a\",\"validator\":\"bogus\"}] }";
        assertTrue(has(validate(json), Severity.WARN, "validator"));
    }

    @Test
    void cabrilloTokenWithNoFieldIsError() throws Exception {
        String json = "{ \"contestId\":\"X\", \"cabrilloRcvd\":[\"not_a_field\"] }";
        assertTrue(has(validate(json), Severity.ERROR, "cabrilloRcvd"));
    }

    @Test
    void cabrilloSpecialAndDeclaredTokensAreOk() throws Exception {
        String json = "{ \"contestId\":\"X\", \"entryFields\":[{\"id\":\"sect_rcvd\"}],"
            + " \"cabrilloRcvd\":[\"rst_rcvd\",\"sect_rcvd\",\"field1\"] }";
        assertEquals(0, PluginValidator.errorCount(validate(json)));
    }

    @Test
    void unknownMultiplierTypeWarns() throws Exception {
        String json = "{ \"contestId\":\"X\", \"scoringRules\":{\"multiplierType\":\"zone_contry\"} }";
        assertTrue(has(validate(json), Severity.WARN, "multiplierType"));
    }

    @Test
    void multipleDupeFlagsWarn() throws Exception {
        String json = "{ \"contestId\":\"X\", \"contestWideDupe\":true, \"perBandGridDupe\":true }";
        assertTrue(has(validate(json), Severity.WARN, "dupe"));
    }

    @Test
    void unknownPaneTypeWarns() throws Exception {
        String json = "{ \"contestId\":\"X\", \"row2Panes\":[{\"paneType\":\"frobnicator\"}] }";
        assertTrue(has(validate(json), Severity.WARN, "paneType"));
    }

    @Test
    void vestigialKeysWarn() throws Exception {
        String json = "{ \"contestId\":\"X\", \"statistics\":[\"a\"],"
            + " \"scoringRules\":{\"scoreFormula\":\"x*y\",\"allowDupes\":true},"
            + " \"row2Panes\":[{\"paneType\":\"statistics\",\"paneIndex\":1}],"
            + " \"entryFields\":[{\"id\":\"a\",\"required\":true}] }";
        List<Issue> issues = validate(json);
        assertTrue(has(issues, Severity.WARN, "statistics"));
        assertTrue(has(issues, Severity.WARN, "scoreFormula"));
        assertTrue(has(issues, Severity.WARN, "allowDupes"));
        assertTrue(has(issues, Severity.WARN, "paneIndex"));
        assertTrue(has(issues, Severity.WARN, "required"));
    }

    @Test
    void shippedPluginIsErrorFree() throws Exception {
        ObjectNode n = PluginIo.readResource("/com/jlog/plugins/arrl_dx_cw_us.json");
        ContestPlugin p = PluginIo.toContest(n);
        List<Issue> issues = PluginValidator.validateContest(n, p);
        assertEquals(0, PluginValidator.errorCount(issues),
            "a shipped plugin should produce no validator ERRORs: " + issues);
    }
}
