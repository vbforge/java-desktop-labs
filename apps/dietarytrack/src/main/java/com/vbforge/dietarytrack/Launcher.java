package com.vbforge.dietarytrack;

/**
 * Entry point for the shaded jar. It must not extend {@code Application}, otherwise the JVM
 * refuses to start with "JavaFX runtime components are missing" when JavaFX is on the classpath.
 */
public final class Launcher {

    private Launcher() {}

    public static void main(String[] args) {
        DietaryTrackApp.main(args);
    }
}
