package com.vbforge.contacts;

/**
 * Entry point for the shaded jar.
 *
 * The JVM refuses to launch a jar whose main class extends
 * javafx.application.Application unless JavaFX is on the module path,
 * which a fat jar does not provide. Delegating from a plain class
 * sidesteps that check.
 */
public final class Launcher {
    public static void main(String[] args) {
        ContactsApp.main(args);
    }
    private Launcher() {}
}