package com.wm3j.pluginbuilder;

import com.jlog.award.AwardLoader;
import com.jlog.award.AwardPlugin;
import com.jlog.db.DatabaseManager;
import com.jlog.plugin.ContestPlugin;
import com.jlog.plugin.PluginLoader;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ARS Plugin Builder — scaffold.
 *
 * <p>Standalone authoring tool for WM3J ARS Suite contest &amp; award plugins.
 * This is the foundation only: it proves the {@code j-log-engine} dependency is
 * wired (real {@link PluginLoader}/{@link AwardLoader}/model classes on the
 * classpath) by listing the plugins the engine can see. The form-driven editor,
 * validation, and Cabrillo/score preview land on top of this shell.
 *
 * <p>The builder shares the operator's {@code ~/.j-log/} directory (it reads the
 * bundled plugins from the engine jar and reads/writes the auto-loaded
 * {@code ~/.j-log/plugins/} and {@code ~/.j-log/awards/} drop-in dirs), so it
 * calls {@link DatabaseManager#initAll()} on startup exactly as the suite does.
 */
public class PluginBuilderApp extends Application {

    private String engineStatus = "";
    private List<ContestPlugin> contestPlugins = List.of();
    private List<AwardPlugin>   awardPlugins   = List.of();

    /** Version from the jar manifest (Implementation-Version); "dev" unpackaged. */
    public static String version() {
        String v = PluginBuilderApp.class.getPackage().getImplementationVersion();
        return (v != null && !v.isBlank()) ? v : "dev";
    }

    @Override
    public void init() {
        try {
            // Sets ~/.j-log as the data dir and opens the shared DBs. PluginLoader/
            // AwardLoader resolve their user drop-in dirs from this, so it must run
            // before either loader is used.
            DatabaseManager.getInstance().initAll();
            contestPlugins = PluginLoader.getInstance().getAvailablePlugins();
            awardPlugins   = AwardLoader.getInstance().getAvailableAwards();
            engineStatus = "j-log-engine wired — "
                    + contestPlugins.size() + " contest plugins, "
                    + awardPlugins.size() + " awards visible";
        } catch (Exception e) {
            engineStatus = "Engine init failed: " + e.getClass().getSimpleName() + " — " + e.getMessage();
        }
    }

    @Override
    public void start(Stage stage) {
        Label header = new Label(engineStatus);
        header.setStyle("-fx-font-weight:bold; -fx-padding:6 0;");

        ListView<String> contestList = new ListView<>();
        contestList.getItems().setAll(contestPlugins.stream()
                .map(p -> p.getContestId() + "   —   " + p.getContestName()
                        + (p.getVersion() != null ? "  v" + p.getVersion() : ""))
                .sorted()
                .collect(Collectors.toList()));

        ListView<String> awardList = new ListView<>();
        awardList.getItems().setAll(awardPlugins.stream()
                .map(a -> a.getAwardId() + "   —   " + a.getAwardName())
                .sorted()
                .collect(Collectors.toList()));

        TabPane tabs = new TabPane(
                new Tab("Contest plugins", contestList),
                new Tab("Award plugins", awardList));
        tabs.getTabs().forEach(t -> t.setClosable(false));

        Label note = new Label("Scaffold — editor, validation, and Cabrillo / score "
                + "preview build on this shell. See ARS_Suite/docs/PLUGIN_FORMAT.md.");
        note.setStyle("-fx-text-fill:#888; -fx-padding:6 0;");

        BorderPane root = new BorderPane();
        root.setTop(new VBox(header));
        root.setCenter(tabs);
        root.setBottom(note);
        BorderPane.setMargin(root.getCenter(), new Insets(0));
        root.setPadding(new Insets(10));

        stage.setTitle("ARS Plugin Builder " + version());
        stage.setScene(new Scene(root, 720, 560));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
