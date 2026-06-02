package com.wm3j.pluginbuilder;

/**
 * Non-Application entry point. The assembled fat jar's Main-Class points here
 * (not at the JavaFX {@link javafx.application.Application} subclass) so that
 * {@code java -jar} doesn't fail with "JavaFX runtime components are missing".
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        PluginBuilderApp.main(args);
    }
}
