package com.vbforge.fitapp.db;

import java.nio.file.Path;

/** Locations of the app's local files. */
public final class AppPaths {

    private AppPaths() {
    }

    public static Path appDir() {
        return Path.of(System.getProperty("user.home"), ".fitapp");
    }

    public static Path configFile() {
        return appDir().resolve("config.properties");
    }
}
