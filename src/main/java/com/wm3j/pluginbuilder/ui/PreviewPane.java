package com.wm3j.pluginbuilder.ui;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.award.AwardPlugin;
import com.jlog.award.AwardService;
import com.jlog.model.QsoRecord;
import com.jlog.plugin.ContestPlugin;
import com.jlog.scoring.StationContext;
import com.wm3j.pluginbuilder.core.AwardPreview;
import com.wm3j.pluginbuilder.core.CabrilloPreview;
import com.wm3j.pluginbuilder.core.ContestScorePreview;
import com.wm3j.pluginbuilder.core.PluginIo;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.List;
import java.util.function.Consumer;

/**
 * Live preview. For an award: the real engine ({@link AwardService}) computes
 * progress against the operator's log. For a contest: the exact Cabrillo header
 * + exchange layout, followed by a real score/multiplier/dupe breakdown
 * ({@link ContestScorePreview}) over a handful of sample QSOs the builder
 * supplies — the same engine path the cockpit uses.
 */
public class PreviewPane extends BorderPane {

    private final TextArea jsonEditor;
    private final Consumer<String> status;
    private final TextArea out = new TextArea();

    private final TextField myCall    = new TextField("WM3J");
    private final TextField myGrid    = new TextField("FM19");
    private final TextField mySentQth = new TextField();
    private final TextArea  sampleLog = new TextArea();

    public PreviewPane(TextArea jsonEditor, Consumer<String> status) {
        this.jsonEditor = jsonEditor;
        this.status = status;
        out.setEditable(false);
        out.setFont(Font.font("monospace", 13));

        Button refresh = new Button("↻ Refresh preview");
        refresh.setOnAction(e -> refresh());

        for (TextField f : new TextField[]{myCall, myGrid, mySentQth}) f.setPrefColumnCount(8);
        mySentQth.setTooltip(new Tooltip("Your sent QTH/county — drives in-state vs out-of-state QSO-party scoring."));
        HBox stn = new HBox(6,
                new Label("My call:"), myCall,
                new Label("My grid:"), myGrid,
                new Label("Sent QTH:"), mySentQth);
        stn.setPadding(new Insets(4, 0, 4, 0));

        sampleLog.setFont(Font.font("monospace", 12));
        sampleLog.setPrefRowCount(6);
        sampleLog.setPromptText("call, band, mode, exchange…  (one QSO per line)");

        VBox box = new VBox(2, stn, new Label("Sample QSOs:"), sampleLog);
        TitledPane inputs = new TitledPane("Contest score inputs", box);
        inputs.setExpanded(true);

        setTop(new VBox(new ToolBar(refresh), inputs));
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
                ContestPlugin p = PluginIo.toContest(root);
                if (sampleLog.getText().isBlank())
                    sampleLog.setText(ContestScorePreview.sampleTemplate(p));
                List<QsoRecord> qsos = ContestScorePreview.parse(sampleLog.getText());
                StationContext ctx = StationContext.of(myCall.getText(), myGrid.getText(), mySentQth.getText());
                out.setText(CabrilloPreview.text(root)
                        + "\n\n────────────────────────────────────────\n\n"
                        + ContestScorePreview.format(p, qsos, ctx));
                status.accept("Contest Cabrillo + live score preview.");
            } else {
                out.setText("JSON has neither \"contestId\" nor \"awardId\".");
            }
        } catch (Exception e) {
            out.setText("Preview unavailable — " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
}
