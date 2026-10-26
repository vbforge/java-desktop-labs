package com.vbforge.animalpedia;

/**
 * Plain entry point used by the fat jar. JavaFX refuses to start from a main class
 * that extends Application when it is launched from the classpath, so we delegate.
 */
public final class Launcher {
    private Launcher() {}

    public static void main(String[] args) {
        AnimalPediaApp.main(args);
    }
}
