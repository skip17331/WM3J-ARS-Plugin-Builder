package com.wm3j.pluginbuilder;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jlog.award.AwardLoader;
import com.jlog.award.AwardPlugin;
import com.jlog.db.DatabaseManager;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;
import com.wm3j.pluginbuilder.core.PluginIo;
import com.wm3j.pluginbuilder.core.PluginValidator;
import com.wm3j.pluginbuilder.core.PluginValidator.Issue;
import com.wm3j.pluginbuilder.core.Skeletons;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * ARS Plugin Builder — editor (v0.1).
 *
 * <p>Open a plugin (from the user drop-in dirs or a file), or start a new one
 * from a skeleton; edit the JSON; <b>Validate</b> runs the real engine model
 * through {@link PluginValidator} (the spec as checks); <b>Save</b> writes to
 * {@code ~/.j-log/plugins/} or {@code ~/.j-log/awards/} (refusing on ERRORs).
 *
 * <p>The JSON text area is the canonical editable surface for now — full-fidelity
 * (nothing the model doesn't cover is lost). Field-by-field form controls build
 * on the same {@link PluginIo}/{@link PluginValidator} core next.
 */
public class PluginBuilderApp extends Application {

    private String engineStatus = "";
    private final TextArea editor = new TextArea();
    private final ListView<Issue> issues = new ListView<>();
    private final ListView<Path> fileList = new ListView<>();
    private final Label status = new Label();

    public static String version() {
        String v = PluginBuilderApp.class.getPackage().getImplementationVersion();
        return (v != null && !v.isBlank()) ? v : "dev";
    }

    @Override
    public void init() {
        try {
            DatabaseManager.getInstance().initAll();
            List<ContestPlugin> c = PluginLoader.getInstance().getAvailablePlugins();
            List<AwardPlugin> a = AwardLoader.getInstance().getAvailableAwards();
            engineStatus = "j-log-engine wired — " + c.size() + " contest plugins, " + a.size() + " awards visible";
        } catch (Exception e) {
            engineStatus = "Engine init failed: " + e.getClass().getSimpleName() + " — " + e.getMessage();
        }
    }

    @Override
    public void start(Stage stage) {
        editor.setFont(Font.font("monospace", 13));
        editor.setPromptText("New Contest / New Award / Open… to begin.");

        issues.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Issue it, boolean empty) {
                super.updateItem(it, empty);
                if (empty || it == null) { setText(null); setStyle(""); return; }
                setText(it.toString());
                setStyle(switch (it.severity()) {
                    case ERROR -> "-fx-text-fill:#c0392b;";
                    case WARN  -> "-fx-text-fill:#b9770e;";
                    case INFO  -> "-fx-text-fill:#2471a3;";
                });
            }
        });

        fileList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Path p, boolean empty) {
                super.updateItem(p, empty);
                setText(empty || p == null ? null
                        : p.getParent().getFileName() + " / " + p.getFileName());
            }
        });
        fileList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && fileList.getSelectionModel().getSelectedItem() != null) {
                openFile(fileList.getSelectionModel().getSelectedItem());
            }
        });
        refreshFileList();

        Button newContest = new Button("New Contest");
        newContest.setOnAction(e -> { editor.setText(Skeletons.contest().stripIndent()); issues.getItems().clear(); status.setText("New contest from skeleton."); });
        Button newAward = new Button("New Award");
        newAward.setOnAction(e -> { editor.setText(Skeletons.award().stripIndent()); issues.getItems().clear(); status.setText("New award from skeleton."); });
        Button open = new Button("Open…");
        open.setOnAction(e -> openViaChooser(stage));
        Button validate = new Button("Validate");
        validate.setOnAction(e -> runValidate());
        Button save = new Button("Save");
        save.setOnAction(e -> runSave());
        Button refresh = new Button("↻");
        refresh.setOnAction(e -> refreshFileList());

        ToolBar bar = new ToolBar(newContest, newAward, open, new Separator(), validate, save, new Separator(), refresh);

        VBox top = new VBox(new Label(engineStatus), bar);
        ((Label) top.getChildren().get(0)).setStyle("-fx-font-weight:bold; -fx-padding:6 8;");

        SplitPane center = new SplitPane(withTitle("Your plugins (double-click to open)", fileList), editor);
        center.setDividerPositions(0.28);

        TitledPane issuePane = new TitledPane("Validation", issues);
        issuePane.setCollapsible(false);
        issuePane.setPrefHeight(180);

        BorderPane root = new BorderPane();
        root.setTop(top);
        root.setCenter(center);
        root.setBottom(new VBox(issuePane, status));
        status.setStyle("-fx-text-fill:#666; -fx-padding:4 8;");
        VBox.setVgrow(issuePane, Priority.ALWAYS);

        stage.setTitle("ARS Plugin Builder " + version());
        stage.setScene(new Scene(root, 900, 680));
        stage.show();
    }

    private VBox withTitle(String title, javafx.scene.Node node) {
        Label l = new Label(title);
        l.setStyle("-fx-font-size:11px; -fx-text-fill:#888; -fx-padding:4 0;");
        VBox box = new VBox(l, node);
        VBox.setVgrow(node, Priority.ALWAYS);
        box.setPadding(new Insets(0, 6, 0, 0));
        return box;
    }

    private void refreshFileList() {
        List<Path> files = new ArrayList<>();
        files.addAll(listJson(PluginIo.userContestDir()));
        files.addAll(listJson(PluginIo.userAwardDir()));
        fileList.getItems().setAll(files);
    }

    private List<Path> listJson(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) { return List.of(); }
    }

    private void openViaChooser(Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Open plugin JSON");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Plugin JSON", "*.json"));
        Path home = PluginIo.userContestDir();
        if (Files.isDirectory(home)) fc.setInitialDirectory(home.toFile());
        java.io.File f = fc.showOpenDialog(stage);
        if (f != null) openFile(f.toPath());
    }

    private void openFile(Path p) {
        try {
            editor.setText(Files.readString(p));
            issues.getItems().clear();
            status.setText("Opened " + p);
        } catch (IOException e) {
            status.setText("Open failed: " + e.getMessage());
        }
    }

    private List<Issue> currentIssues() throws IOException {
        ObjectNode node = PluginIo.parse(editor.getText());
        if (node.has("contestId")) {
            return PluginValidator.validateContest(node, PluginIo.toContest(node));
        } else if (node.has("awardId")) {
            return PluginValidator.validateAward(node, PluginIo.toAward(node));
        }
        throw new IOException("JSON has neither \"contestId\" nor \"awardId\" — can't tell if it's a contest or award plugin.");
    }

    private void runValidate() {
        try {
            List<Issue> list = currentIssues();
            issues.getItems().setAll(list);
            long errs = PluginValidator.errorCount(list);
            status.setText(list.isEmpty() ? "✓ No issues." : (errs + " error(s), " + (list.size() - errs) + " warning(s)."));
        } catch (Exception e) {
            issues.getItems().clear();
            status.setText("Parse error: " + e.getMessage());
        }
    }

    private void runSave() {
        try {
            ObjectNode node = PluginIo.parse(editor.getText());
            List<Issue> list = node.has("contestId")
                    ? PluginValidator.validateContest(node, PluginIo.toContest(node))
                    : node.has("awardId")
                        ? PluginValidator.validateAward(node, PluginIo.toAward(node))
                        : null;
            if (list == null) { status.setText("JSON has no contestId/awardId — can't save."); return; }
            issues.getItems().setAll(list);
            long errs = PluginValidator.errorCount(list);
            if (errs > 0) { status.setText("Fix " + errs + " error(s) before saving."); return; }
            Path written = node.has("contestId") ? PluginIo.saveContest(node) : PluginIo.saveAward(node);
            refreshFileList();
            status.setText("Saved → " + written);
        } catch (Exception e) {
            status.setText("Save failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) { launch(args); }
}
