package com.wm3j.pluginbuilder.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable view of one {@code entryFields[]} entry (engine {@code FieldDef}),
 * edited by the Entry Fields form and (de)serialized by {@link FieldsCodec}.
 * Plain mutable fields — the form writes to them directly.
 */
public final class FieldRow {
    public String id = "";
    public String label = "";
    public String type = "";              // "", text, combo, number, checkbox
    public int    entryRow = 0;           // 0 = Received, 1 = Sent
    public int    width = 0;              // 0 = default
    public List<String> options = new ArrayList<>();
    public String validator = "";         // "", maidenhead, maidenhead6, numeric, fd_class, ss_check
    public boolean autoIncrement = false;
    public boolean persistent = false;
    public boolean constant = false;

    public FieldRow() {}
    public FieldRow(String id) { this.id = id; }

    /** One-line summary for list cells. */
    public String summary() {
        StringBuilder b = new StringBuilder(id == null || id.isBlank() ? "(no id)" : id);
        b.append(entryRow == 1 ? "  · Sent" : "  · Recv");
        if (type != null && !type.isBlank()) b.append("  · ").append(type);
        List<String> flags = new ArrayList<>();
        if (autoIncrement) flags.add("auto#");
        if (constant) flags.add("const");
        if (persistent) flags.add("persist");
        if (!flags.isEmpty()) b.append("  · ").append(String.join(",", flags));
        return b.toString();
    }
}
