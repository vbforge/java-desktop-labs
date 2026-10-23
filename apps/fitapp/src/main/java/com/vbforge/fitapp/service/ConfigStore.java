package com.vbforge.fitapp.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** "Remember me" storage in ~/.fitapp/config.properties. Only the username is saved - never a password. */
public class ConfigStore {

    private static final String KEY_USERNAME = "remembered.username";

    private final Path file;

    public ConfigStore(Path file) {
        this.file = file;
    }

    public String rememberedUsername() {
        Properties p = load();
        String v = p.getProperty(KEY_USERNAME);
        return (v == null || v.isBlank()) ? null : v;
    }

    public void remember(String username) {
        Properties p = load();
        p.setProperty(KEY_USERNAME, username);
        save(p);
    }

    public void forget() {
        Properties p = load();
        p.remove(KEY_USERNAME);
        save(p);
    }

    private Properties load() {
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                p.load(in);
            } catch (IOException ignored) {
                // corrupt or unreadable config just means "nothing remembered"
            }
        }
        return p;
    }

    private void save(Properties p) {
        try {
            Files.createDirectories(file.getParent());
            try (OutputStream out = Files.newOutputStream(file)) {
                p.store(out, "FitApp settings");
            }
        } catch (IOException ignored) {
            // failing to persist a convenience setting must not break the app
        }
    }
}
