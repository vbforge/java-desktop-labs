package com.vbforge.atlasdesk;

/** Separate entry point so the shaded fat jar can start a JavaFX app from the classpath. */
public final class Launcher {
    public static void main(String[] args) {
        AtlasDeskApp.main(args);
    }
}