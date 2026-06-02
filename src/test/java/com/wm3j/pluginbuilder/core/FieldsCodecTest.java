package com.wm3j.pluginbuilder.core;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FieldsCodecTest {

    @Test
    void readsSkeletonEntryFields() throws Exception {
        ObjectNode plugin = PluginIo.parse(Skeletons.contest());
        List<FieldRow> rows = FieldsCodec.toRows(plugin.get("entryFields"));
        assertEquals(5, rows.size());
        assertEquals("callsign", rows.get(0).id);
        assertEquals(0, rows.get(0).entryRow);
        assertEquals("serial_sent", rows.get(4).id);
        assertEquals(1, rows.get(4).entryRow);
        assertTrue(rows.get(4).autoIncrement);
    }

    @Test
    void roundTripPreservesMeaningfulFields() {
        FieldRow r = new FieldRow("sect_rcvd");
        r.label = "Section"; r.type = "combo"; r.entryRow = 0; r.width = 70;
        r.options = List.of("CT", "MA", "ME"); r.validator = "ss_check";
        r.persistent = true;

        ArrayNode arr = FieldsCodec.toArray(List.of(r));
        FieldRow back = FieldsCodec.toRows(arr).get(0);

        assertEquals("sect_rcvd", back.id);
        assertEquals("combo", back.type);
        assertEquals(70, back.width);
        assertEquals(List.of("CT", "MA", "ME"), back.options);
        assertEquals("ss_check", back.validator);
        assertTrue(back.persistent);
        assertEquals(0, back.entryRow);
    }

    @Test
    void omitsDefaultsAndVestigialRequired() {
        FieldRow r = new FieldRow("callsign");   // all else default
        ObjectNode o = (ObjectNode) FieldsCodec.toArray(List.of(r)).get(0);
        assertTrue(o.has("id"));
        assertFalse(o.has("width"), "width 0 should be omitted");
        assertFalse(o.has("entryRow"), "entryRow 0 should be omitted");
        assertFalse(o.has("type"), "blank type omitted");
        assertFalse(o.has("autoIncrement"), "false booleans omitted");
        assertFalse(o.has("required"), "vestigial 'required' is never emitted");
        assertFalse(o.has("options"), "empty options omitted");
    }

    @Test
    void typeTextIsTreatedAsDefaultAndOmitted() {
        FieldRow r = new FieldRow("a"); r.type = "text";
        ObjectNode o = (ObjectNode) FieldsCodec.toArray(List.of(r)).get(0);
        assertFalse(o.has("type"), "type 'text' is the default render and is omitted");
    }

    @Test
    void parseOptionsHandlesCommasAndSpaces() {
        assertEquals(List.of("A", "B", "C"), FieldsCodec.parseOptions(" A, B   C "));
        assertTrue(FieldsCodec.parseOptions("").isEmpty());
    }
}
