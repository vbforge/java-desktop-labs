package com.vbforge.contacts;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Reads and writes ~/.contacts/config.properties.
 *
 * Currently a single key: backend. Kept generic so future settings
 * (window geometry, sort order) can be added without changing the
 * file format or any call site.
 */
public final class AppConfig {

    private static final String KEY_BACKEND = "backend";

    private final Path file;
    private final Properties props = new Properties();

    private AppConfig(Path file) {
        this.file = file;
    }

    public static AppConfig open() {
        Path dir = Paths.get(System.getProperty("user.home"), ".contacts");
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

    public BackendType backend() {
        return BackendType.fromName(props.getProperty(KEY_BACKEND));
    }

    public void setBackend(BackendType t) {
        props.setProperty(KEY_BACKEND, t.name());
        save();
    }

    private void load() {
        if (!Files.exists(file)) return;
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        } catch (IOException ignored) {
            // Corrupt or unreadable → defaults.
        }
    }

    private void save() {
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "Contacts settings");
        } catch (IOException ignored) {
            // Non-fatal.
        }
    }
}