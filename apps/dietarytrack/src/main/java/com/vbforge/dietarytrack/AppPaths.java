package com.vbforge.dietarytrack;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Locations of the files the application keeps on disk. */
final class AppPaths {

    private static final Path DATA_DIR = Paths.get(System.getProperty("user.home"), ".dietarytrack");

    private AppPaths() {}

    static Path dataDir() {
        return DATA_DIR;
    }

    static Path database() {
        return DATA_DIR.resolve("dietary.db");
    }

    static Path csv() {
        return DATA_DIR.resolve("dietary.csv");
    }
}
