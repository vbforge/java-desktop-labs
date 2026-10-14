package com.vbforge.pathlab;

/**
 * Entry point for the shaded jar.
 *
 * The JVM treats a main class that extends javafx.application.Application
 * specially — it demands JavaFX be on the module path, which a fat jar
 * does not provide. Delegating from a plain class avoids that check.
 */
public final class Launcher {
    public static void main(String[] args) {
        PathLabApp.main(args);
    }

    private Launcher() {}
}