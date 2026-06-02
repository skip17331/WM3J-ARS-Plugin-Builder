package com.wm3j.pluginbuilder.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wm3j.pluginbuilder.core.CabrilloTokens;
import com.wm3j.pluginbuilder.core.PluginIo;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Composes the Cabrillo exchange mapping ({@code cabrilloSent} / {@code cabrilloRcvd})
 * by picking ordered tokens from a palette of <em>valid</em> tokens (declared
 * entryField ids + special tokens). Because the palette only offers resolvable
 * tokens, a mapping built here never trips the validator's token check. The JSON
 * text area stays canonical; Load/Apply sync explicitly.
 */
public class CabrilloMappingEditor extends BorderPane {

    private final TextArea jsonEditor;
    private final Consumer<String> status;

    private final ObservableList<String> palette = FXCollections.observableArrayList();
    private final ObservableList<String> sent = FXCollections.observableArrayList();
    private final ObservableList<String> rcvd = FXCollections.observableArrayList();

    private final ListView<String> paletteView = new ListView<>(palette);
    private final ListView<String> sentView = new ListView<>(sent);
    private final ListView<String> rcvdView = new ListView<>(rcvd);

    public CabrilloMappingEditor(TextArea jsonEditor, Consumer<String> status) {
        this.jsonEditor = jsonEditor;
        this.status = status;

        Button load = new Button("⤓ Load from JSON");
        load.setOnAction(e -> loadFromJson(true));
        Button apply = new Button("⤒ Apply to JSON");
        apply.setOnAction(e -> applyToJson());
        setTop(new ToolBar(load, apply));

        Button toSent = new Button("→ Sent");
        toSent.setOnAction(e -> push(sent));
        Button toRcvd = new Button("→ Rcvd");
        toRcvd.setOnAction(e -> push(rcvd));
        VBox paletteCol = column("Available tokens", paletteView, new HBox(6, toSent, toRcvd));
        paletteView.setOnMouseClicked(e -> { if (e.getClickCount() == 2) push(rcvd); });

        VBox sentCol = orderedColumn("Sent  (cabrilloSent)", sentView, sent);
        VBox rcvdCol = orderedColumn("Received  (cabrilloRcvd)", rcvdView, rcvd);

        HBox cols = new HBox(10, paletteCol, sentCol, rcvdCol);
        cols.setPadding(new Insets(8));
        HBox.setHgrow(paletteCol, Priority.ALWAYS);
        HBox.setHgrow(sentCol, Priority.ALWAYS);
        HBox.setHgrow(rcvdCol, Priority.ALWAYS);
        setCenter(cols);
    }

    private VBox column(String title, ListView<String> lv, javafx.scene.Node controls) {
        Label l = new Label(title);
        l.setStyle("-fx-font-size:11px; -fx-text-fill:#888;");
        VBox box = new VBox(4, l, lv, controls);
        VBox.setVgrow(lv, Priority.ALWAYS);
        return box;
    }

    private VBox orderedColumn(String title, ListView<String> lv, ObservableList<String> model) {
        Button up = new Button("↑");   up.setOnAction(e -> move(lv, model, -1));
        Button down = new Button("↓"); down.setOnAction(e -> move(lv, model, 1));
        Button del = new Button("✕");  del.setOnAction(e -> { int i = lv.getSelectionModel().getSelectedIndex(); if (i >= 0) model.remove(i); });
        return column(title, lv, new HBox(6, up, down, del));
    }

    private void push(ObservableList<String> target) {
        String t = paletteView.getSelectionModel().getSelectedItem();
        if (t == null) return;
        if (target.contains(t)) { status.accept("'" + t + "' is already in that list."); return; }
        target.add(t);
    }

    private void move(ListView<String> lv, ObservableList<String> model, int delta) {
        int i = lv.getSelectionModel().getSelectedIndex();
        int j = i + delta;
        if (i < 0 || j < 0 || j >= model.size()) return;
        String s = model.remove(i);
        model.add(j, s);
        lv.getSelectionModel().select(j);
    }

    public void loadFromJson(boolean announce) {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            palette.setAll(CabrilloTokens.available(root));
            sent.setAll(CabrilloTokens.readList(root, "cabrilloSent"));
            rcvd.setAll(CabrilloTokens.readList(root, "cabrilloRcvd"));
            if (announce) status.accept("Loaded Cabrillo mapping (" + sent.size() + " sent, " + rcvd.size() + " rcvd).");
        } catch (Exception e) {
            if (announce) status.accept("Can't load mapping — JSON parse error: " + e.getMessage());
        }
    }

    private void applyToJson() {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            CabrilloTokens.writeList(root, "cabrilloSent", sent);
            CabrilloTokens.writeList(root, "cabrilloRcvd", rcvd);
            jsonEditor.setText(PluginIo.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(root));
            status.accept("Applied Cabrillo mapping to JSON. Validate/Save when ready.");
        } catch (Exception e) {
            status.accept("Can't apply mapping — JSON parse error: " + e.getMessage());
        }
    }
}
