package com.wm3j.pluginbuilder.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wm3j.pluginbuilder.core.FieldRow;
import com.wm3j.pluginbuilder.core.FieldsCodec;
import com.wm3j.pluginbuilder.core.PluginIo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

/**
 * Master-detail form for {@code entryFields[]}. The list on the left is the
 * working model; the form on the right edits the selected row. Load pulls the
 * array out of the JSON editor; Apply writes it back. The JSON text area stays
 * the canonical document — this just edits one section structurally.
 */
public class EntryFieldsEditor extends BorderPane {

    private final TextArea jsonEditor;
    private final Consumer<String> status;
    private final ObservableList<FieldRow> rows = FXCollections.observableArrayList();
    private final ListView<FieldRow> list = new ListView<>(rows);

    // detail controls
    private final TextField id = new TextField();
    private final TextField label = new TextField();
    private final ComboBox<String> type = new ComboBox<>(FXCollections.observableArrayList("text", "combo", "number", "checkbox"));
    private final ComboBox<String> entryRow = new ComboBox<>(FXCollections.observableArrayList("Received (0)", "Sent (1)"));
    private final Spinner<Integer> width = new Spinner<>(0, 400, 0, 5);
    private final TextField options = new TextField();
    private final ComboBox<String> validator = new ComboBox<>(FXCollections.observableArrayList("(none)", "maidenhead", "maidenhead6", "numeric", "fd_class", "ss_check"));
    private final CheckBox autoIncrement = new CheckBox("autoIncrement");
    private final CheckBox persistent = new CheckBox("persistent");
    private final CheckBox constant = new CheckBox("constant");

    private boolean loading = false;

    public EntryFieldsEditor(TextArea jsonEditor, Consumer<String> status) {
        this.jsonEditor = jsonEditor;
        this.status = status;

        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(FieldRow r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null : r.summary());
            }
        });
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> populate(b));

        Button load = new Button("⤓ Load from JSON");
        load.setOnAction(e -> loadFromJson(true));
        Button apply = new Button("⤒ Apply to JSON");
        apply.setOnAction(e -> applyToJson());
        Button add = new Button("Add");
        add.setOnAction(e -> { FieldRow r = new FieldRow("new_field"); rows.add(r); list.getSelectionModel().select(r); });
        Button remove = new Button("Remove");
        remove.setOnAction(e -> { int i = list.getSelectionModel().getSelectedIndex(); if (i >= 0) rows.remove(i); });
        Button up = new Button("↑");
        up.setOnAction(e -> move(-1));
        Button down = new Button("↓");
        down.setOnAction(e -> move(1));

        ToolBar bar = new ToolBar(load, apply, new Separator(), add, remove, up, down);

        wireDetail();
        setDetailDisabled(true);

        SplitPane split = new SplitPane(list, detailPane());
        split.setDividerPositions(0.42);

        setTop(bar);
        setCenter(split);
    }

    private GridPane detailPane() {
        GridPane g = new GridPane();
        g.setHgap(8); g.setVgap(8); g.setPadding(new Insets(8, 4, 4, 10));
        int r = 0;
        g.addRow(r++, new Label("id"), id);
        g.addRow(r++, new Label("label"), label);
        g.addRow(r++, new Label("type"), type);
        g.addRow(r++, new Label("row"), entryRow);
        g.addRow(r++, new Label("width"), width);
        g.addRow(r++, new Label("options"), options);
        g.addRow(r++, new Label("validator"), validator);
        g.add(new VBox(6, autoIncrement, persistent, constant), 1, r);
        id.setPromptText("callsign, rst_rcvd, field id…");
        options.setPromptText("combo items, comma-separated");
        GridPane.setHgrow(id, Priority.ALWAYS);
        return g;
    }

    private void wireDetail() {
        id.textProperty().addListener((o, a, b) -> set(r -> r.id = b));
        label.textProperty().addListener((o, a, b) -> set(r -> r.label = b));
        type.valueProperty().addListener((o, a, b) -> set(r -> r.type = b == null ? "" : b));
        entryRow.valueProperty().addListener((o, a, b) -> set(r -> r.entryRow = (b != null && b.startsWith("Sent")) ? 1 : 0));
        width.valueProperty().addListener((o, a, b) -> set(r -> r.width = b == null ? 0 : b));
        options.textProperty().addListener((o, a, b) -> set(r -> r.options = FieldsCodec.parseOptions(b)));
        validator.valueProperty().addListener((o, a, b) -> set(r -> r.validator = (b == null || b.equals("(none)")) ? "" : b));
        autoIncrement.selectedProperty().addListener((o, a, b) -> set(r -> r.autoIncrement = b));
        persistent.selectedProperty().addListener((o, a, b) -> set(r -> r.persistent = b));
        constant.selectedProperty().addListener((o, a, b) -> set(r -> r.constant = b));
    }

    private void set(Consumer<FieldRow> mut) {
        if (loading) return;
        FieldRow r = list.getSelectionModel().getSelectedItem();
        if (r == null) return;
        mut.accept(r);
        list.refresh();
    }

    private void populate(FieldRow r) {
        loading = true;
        try {
            boolean none = (r == null);
            setDetailDisabled(none);
            id.setText(none ? "" : r.id);
            label.setText(none ? "" : r.label);
            type.setValue(none || r.type == null || r.type.isBlank() ? "text" : r.type);
            entryRow.setValue(none || r.entryRow != 1 ? "Received (0)" : "Sent (1)");
            width.getValueFactory().setValue(none ? 0 : r.width);
            options.setText(none ? "" : FieldsCodec.optionsToText(r.options));
            validator.setValue(none || r.validator == null || r.validator.isBlank() ? "(none)" : r.validator);
            autoIncrement.setSelected(!none && r.autoIncrement);
            persistent.setSelected(!none && r.persistent);
            constant.setSelected(!none && r.constant);
        } finally { loading = false; }
    }

    private void setDetailDisabled(boolean d) {
        for (Control c : List.of(id, label, type, entryRow, width, options, validator, autoIncrement, persistent, constant))
            c.setDisable(d);
    }

    private void move(int delta) {
        int i = list.getSelectionModel().getSelectedIndex();
        int j = i + delta;
        if (i < 0 || j < 0 || j >= rows.size()) return;
        FieldRow r = rows.remove(i);
        rows.add(j, r);
        list.getSelectionModel().select(j);
    }

    /** Pull entryFields out of the JSON editor into the table. */
    public void loadFromJson(boolean announce) {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            List<FieldRow> r = FieldsCodec.toRows(root.get("entryFields"));
            rows.setAll(r);
            if (announce) status.accept("Loaded " + r.size() + " entry field(s) from JSON.");
        } catch (Exception e) {
            if (announce) status.accept("Can't load fields — JSON parse error: " + e.getMessage());
        }
    }

    /** Write the table back into the JSON editor's entryFields. */
    private void applyToJson() {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            root.set("entryFields", FieldsCodec.toArray(rows));
            jsonEditor.setText(PluginIo.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(root));
            status.accept("Applied " + rows.size() + " entry field(s) to JSON. Validate/Save when ready.");
        } catch (Exception e) {
            status.accept("Can't apply fields — JSON parse error: " + e.getMessage());
        }
    }
}
