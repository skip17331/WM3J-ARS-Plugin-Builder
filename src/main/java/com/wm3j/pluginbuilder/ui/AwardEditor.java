package com.wm3j.pluginbuilder.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wm3j.pluginbuilder.core.AwardCodec;
import com.wm3j.pluginbuilder.core.AwardModel;
import com.wm3j.pluginbuilder.core.PluginIo;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * Form for award plugins. Scalars/options/window are a simple grid; targets,
 * bonus, and tiers are newline-delimited text areas ("id | label",
 * "threshold | name") — robust for large pasted lists. No live binding:
 * Load populates, Apply writes back through {@link AwardCodec} (merge-preserving).
 */
public class AwardEditor extends BorderPane {

    private final TextArea jsonEditor;
    private final Consumer<String> status;

    private final TextField awardId = new TextField();
    private final TextField awardName = new TextField();
    private final TextField description = new TextField();
    private final ComboBox<String> matchOn = new ComboBox<>(FXCollections.observableArrayList(AwardCodec.MATCH_ON));
    private final TextField targetLabel = new TextField();
    private final CheckBox matchBaseCallsign = new CheckBox("match base callsign (strip /P, /M…)");
    private final CheckBox confirmedOnly = new CheckBox("confirmed only (QSL received)");
    private final TextField windowStart = new TextField();
    private final TextField windowEnd = new TextField();
    private final TextArea targets = new TextArea();
    private final TextArea bonus = new TextArea();
    private final TextArea tiers = new TextArea();

    public AwardEditor(TextArea jsonEditor, Consumer<String> status) {
        this.jsonEditor = jsonEditor;
        this.status = status;

        matchOn.setEditable(true);
        windowStart.setPromptText("2026-07-01T00:00:00 (optional)");
        windowEnd.setPromptText("2026-07-08T23:59:59 (optional)");
        for (TextArea a : new TextArea[]{targets, bonus, tiers}) { a.setPrefRowCount(5); a.setFont(javafx.scene.text.Font.font("monospace", 12)); }
        targets.setPromptText("one per line:  CT | Connecticut");
        bonus.setPromptText("one per line:  K2A | Bonus station");
        tiers.setPromptText("one per line:  50 | Worked All States");

        Button load = new Button("⤓ Load from JSON");
        load.setOnAction(e -> loadFromJson(true));
        Button apply = new Button("⤒ Apply to JSON");
        apply.setOnAction(e -> applyToJson());
        setTop(new ToolBar(load, apply));

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(10));
        int r = 0;
        g.addRow(r++, new Label("awardId"), awardId);
        g.addRow(r++, new Label("awardName"), awardName);
        g.addRow(r++, new Label("description"), description);
        g.addRow(r++, new Label("matchOn"), matchOn);
        g.addRow(r++, new Label("targetLabel"), targetLabel);
        g.add(matchBaseCallsign, 1, r++);
        g.add(confirmedOnly, 1, r++);
        g.addRow(r++, new Label("window start"), windowStart);
        g.addRow(r++, new Label("window end"), windowEnd);
        for (TextField tf : new TextField[]{awardId, awardName, description, targetLabel, windowStart, windowEnd})
            GridPane.setHgrow(tf, Priority.ALWAYS);
        GridPane.setHgrow(matchOn, Priority.ALWAYS);

        VBox lists = new VBox(4,
            section("Targets  (id | label) — leave empty for a count-match award", targets),
            section("Bonus  (id | label)", bonus),
            section("Tiers  (threshold | name) — progress is measured against the top tier", tiers));
        lists.setPadding(new Insets(0, 10, 10, 10));
        VBox.setVgrow(targets, Priority.ALWAYS);

        VBox body = new VBox(g, new Separator(), lists);
        setCenter(new ScrollPane(body) {{ setFitToWidth(true); }});
    }

    private VBox section(String title, TextArea area) {
        Label l = new Label(title);
        l.setStyle("-fx-font-size:11px; -fx-text-fill:#888;");
        VBox b = new VBox(2, l, area);
        VBox.setVgrow(area, Priority.ALWAYS);
        return b;
    }

    public void loadFromJson(boolean announce) {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            AwardModel m = AwardCodec.read(root);
            awardId.setText(m.awardId);
            awardName.setText(m.awardName);
            description.setText(m.description);
            matchOn.setValue(m.matchOn);
            targetLabel.setText(m.targetLabel);
            matchBaseCallsign.setSelected(m.matchBaseCallsign);
            confirmedOnly.setSelected(m.confirmedOnly);
            windowStart.setText(m.windowStart);
            windowEnd.setText(m.windowEnd);
            targets.setText(AwardCodec.targetsToText(AwardCodec.readTargets(root, "targets")));
            bonus.setText(AwardCodec.targetsToText(AwardCodec.readTargets(root, "bonus")));
            tiers.setText(AwardCodec.tiersToText(AwardCodec.readTiers(root)));
            if (announce) status.accept("Loaded award fields from JSON.");
        } catch (Exception e) {
            if (announce) status.accept("Can't load award — JSON parse error: " + e.getMessage());
        }
    }

    private void applyToJson() {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            AwardModel m = new AwardModel();
            m.awardId = awardId.getText();
            m.awardName = awardName.getText();
            m.description = description.getText();
            m.matchOn = matchOn.getValue() == null ? "" : matchOn.getValue();
            m.targetLabel = targetLabel.getText();
            m.matchBaseCallsign = matchBaseCallsign.isSelected();
            m.confirmedOnly = confirmedOnly.isSelected();
            m.windowStart = windowStart.getText();
            m.windowEnd = windowEnd.getText();
            AwardCodec.apply(root, m);
            AwardCodec.writeTargets(root, "targets", AwardCodec.parseTargets(targets.getText()));
            AwardCodec.writeTargets(root, "bonus", AwardCodec.parseTargets(bonus.getText()));
            AwardCodec.writeTiers(root, AwardCodec.parseTiers(tiers.getText()));
            jsonEditor.setText(PluginIo.mapper().writerWithDefaultPrettyPrinter().writeValueAsString(root));
            status.accept("Applied award fields to JSON. Validate/Save when ready.");
        } catch (Exception e) {
            status.accept("Can't apply award — JSON parse error: " + e.getMessage());
        }
    }
}
