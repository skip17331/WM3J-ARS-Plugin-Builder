package com.wm3j.pluginbuilder.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wm3j.pluginbuilder.core.PluginIo;
import com.wm3j.pluginbuilder.core.ScoringCodec;
import com.wm3j.pluginbuilder.core.ScoringModel;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Form for the scalar scoring / multiplier / dupe knobs. No live binding — Load
 * populates the controls, Apply gathers them through {@link ScoringCodec} (which
 * merges into the JSON, so complex keys survive, and writes a single dupe flag).
 */
public class ScoringEditor extends BorderPane {

    private final TextArea jsonEditor;
    private final Consumer<String> status;

    private final ComboBox<String> multiplierType = new ComboBox<>(FXCollections.observableArrayList(ScoringCodec.MULT_TYPES));
    private final ComboBox<String> multField = new ComboBox<>();
    private final CheckBox multPerBand = new CheckBox("multiplier counts per band");
    private final ComboBox<ScoringModel.Dupe> dupe = new ComboBox<>(FXCollections.observableArrayList(ScoringModel.Dupe.values()));
    private final Spinner<Integer> pointsPerQso = new Spinner<>(0, 100000, 0, 1);
    private final CheckBox scoreIsPointsOnly = new CheckBox("score is points only (no multiplier)");
    private final CheckBox perModeMultipliers = new CheckBox("multipliers per mode");
    private final CheckBox autoFillDxcc = new CheckBox("auto-fill DXCC prefix");
    private final CheckBox autoFillWpx = new CheckBox("auto-fill WPX prefix");

    public ScoringEditor(TextArea jsonEditor, Consumer<String> status) {
        this.jsonEditor = jsonEditor;
        this.status = status;

        multiplierType.setEditable(true);   // tolerate/show arbitrary values
        multiplierType.setPromptText("(generic distinct-count)");
        multField.setEditable(true);
        multField.setPromptText("entryField id (e.g. sect_rcvd) or grid_rcvd / cq_zone");

        Button load = new Button("⤓ Load from JSON");
        load.setOnAction(e -> loadFromJson(true));
        Button apply = new Button("⤒ Apply to JSON");
        apply.setOnAction(e -> applyToJson());
        setTop(new ToolBar(load, apply));

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(10); g.setPadding(new Insets(12));
        int r = 0;
        g.addRow(r++, new Label("multiplierType"), multiplierType);
        g.addRow(r++, new Label("multiplier field"), multField);
        g.add(multPerBand, 1, r++);
        g.add(new Separator(), 0, r++, 2, 1);
        g.addRow(r++, new Label("dupe rule"), dupe);
        g.add(new Separator(), 0, r++, 2, 1);
        g.addRow(r++, new Label("points / QSO"), pointsPerQso);
        g.add(scoreIsPointsOnly, 1, r++);
        g.add(perModeMultipliers, 1, r++);
        g.add(autoFillDxcc, 1, r++);
        g.add(autoFillWpx, 1, r++);
        GridPane.setHgrow(multiplierType, Priority.ALWAYS);
        GridPane.setHgrow(multField, Priority.ALWAYS);

        Label hint = new Label("Complex scoring (pointsByBand, distanceScoring, qsoParty …) "
            + "stays in the JSON tab; this form only edits these knobs and preserves the rest.");
        hint.setWrapText(true);
        hint.setStyle("-fx-text-fill:#888; -fx-font-size:11px; -fx-padding:0 12 8 12;");

        setCenter(new VBox(g, hint));
    }

    public void loadFromJson(boolean announce) {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            // seed the multiplier-field combo with this plugin's entryField ids
            List<String> ids = new ArrayList<>();
            JsonNode ef = root.path("entryFields");
            if (ef.isArray()) for (JsonNode f : ef) { String id = f.path("id").asText(""); if (!id.isBlank()) ids.add(id); }
            multField.setItems(FXCollections.observableArrayList(ids));

            ScoringModel m = ScoringCodec.read(root);
            multiplierType.setValue(m.multiplierType);
            multField.setValue(m.multField);
            multPerBand.setSelected(m.multPerBand);
            dupe.setValue(m.dupe);
            pointsPerQso.getValueFactory().setValue(m.pointsPerQso);
            scoreIsPointsOnly.setSelected(m.scoreIsPointsOnly);
            perModeMultipliers.setSelected(m.perModeMultipliers);
            autoFillDxcc.setSelected(m.autoFillDxccPrefix);
            autoFillWpx.setSelected(m.autoFillWpxPrefix);
            if (announce) status.accept("Loaded scoring settings from JSON.");
        } catch (Exception e) {
            if (announce) status.accept("Can't load scoring — JSON parse error: " + e.getMessage());
        }
    }

    private void applyToJson() {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            ScoringModel m = new ScoringModel();
            m.multiplierType    = text(multiplierType.getValue());
            m.multField         = text(multField.getValue());
            m.multPerBand       = multPerBand.isSelected();
            m.dupe              = dupe.getValue() == null ? ScoringModel.Dupe.DEFAULT : dupe.getValue();
            m.pointsPerQso      = pointsPerQso.getValue() == null ? 0 : pointsPerQso.getValue();
            m.scoreIsPointsOnly = scoreIsPointsOnly.isSelected();
            m.perModeMultipliers = perModeMultipliers.isSelected();
            m.autoFillDxccPrefix = autoFillDxcc.isSelected();
            m.autoFillWpxPrefix  = autoFillWpx.isSelected();
            ScoringCodec.apply(root, m);
            jsonEditor.setText(PluginIo.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(root));
            status.accept("Applied scoring settings to JSON. Validate/Save when ready.");
        } catch (Exception e) {
            status.accept("Can't apply scoring — JSON parse error: " + e.getMessage());
        }
    }

    private static String text(String s) { return s == null ? "" : s.trim(); }
}
