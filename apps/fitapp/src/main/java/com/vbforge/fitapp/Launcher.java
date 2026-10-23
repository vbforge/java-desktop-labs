package com.vbforge.fitapp;

import javafx.application.Application;

/**
 * Entry point for the fat jar. It must NOT extend Application, otherwise the JVM
 * insists on JavaFX being on the module path and the shaded jar fails to start.
 */
public class Launcher {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
