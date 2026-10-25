package com.vbforge.blockforge;

import javafx.application.Application;

/**
 * Plain main class (does not extend {@link Application}) so a shaded classpath jar can start
 * without "JavaFX runtime components are missing" errors.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Application.launch(BlockForgeApp.class, args);
    }
}