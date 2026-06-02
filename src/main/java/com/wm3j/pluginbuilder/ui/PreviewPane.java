package com.wm3j.pluginbuilder.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.award.AwardPlugin;
import com.jlog.award.AwardService;
import com.wm3j.pluginbuilder.core.AwardPreview;
import com.wm3j.pluginbuilder.core.CabrilloPreview;
import com.wm3j.pluginbuilder.core.PluginIo;

import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;

import java.util.function.Consumer;

/**
 * Live preview. For an award: the real engine ({@link AwardService}) computes
 * progress against the operator's log. For a contest: the exact Cabrillo header
 * + exchange layout (faithful score preview awaits the engine scoring refactor).
 */
public class PreviewPane extends BorderPane {

    private final TextArea jsonEditor;
    private final Consumer<String> status;
    private final TextArea out = new TextArea();

    public PreviewPane(TextArea jsonEditor, Consumer<String> status) {
        this.jsonEditor = jsonEditor;
        this.status = status;
        out.setEditable(false);
        out.setFont(Font.font("monospace", 13));

        Button refresh = new Button("↻ Refresh preview");
        refresh.setOnAction(e -> refresh());
        setTop(new ToolBar(refresh));
        setCenter(out);
    }

    public void refresh() {
        try {
            ObjectNode root = PluginIo.parse(jsonEditor.getText());
            if (root.has("awardId")) {
                AwardPlugin a = PluginIo.toAward(root);
                out.setText(AwardPreview.format(AwardService.getInstance().compute(a)));
                status.accept("Award preview computed against your log.");
            } else if (root.has("contestId")) {
                out.setText(CabrilloPreview.text(root));
                status.accept("Cabrillo header / exchange preview.");
            } else {
                out.setText("JSON has neither \"contestId\" nor \"awardId\".");
            }
        } catch (Exception e) {
            out.setText("Preview unavailable — " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
