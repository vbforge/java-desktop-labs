package com.vbforge.focussprint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Properties;

/**
 * Reads and writes ~/.focussprint/config.properties.
 *
 * Currently holds one thing: the timestamp at which the current "day"
 * began. Sessions started before this timestamp are hidden from the
 * live view but stay in the database.
 */
public final class AppConfig {

    private static final String KEY_DAY_START = "dayStart";

    private final Path file;
    private final Properties props = new Properties();

    private AppConfig(Path file) {
        this.file = file;
    }

    public static AppConfig open() {
        Path dir = Paths.get(System.getProperty("user.home"), ".focussprint");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            dir = Paths.get(".");
        }
        AppConfig cfg = new AppConfig(dir.resolve("config.properties"));
        cfg.load();
        return cfg;
    }

    public Path baseDir() {
        return file.getParent();
    }

    public Instant dayStart() {
        String raw = props.getProperty(KEY_DAY_START);
        if (raw == null) return null;
        try {
            return Instant.ofEpochMilli(Long.parseLong(raw));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setDayStart(Instant instant) {
        props.setProperty(KEY_DAY_START, String.valueOf(instant.toEpochMilli()));
        save();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException ignored) { }
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "FocusSprint settings");
        } catch (IOException ignored) { }
    }
}