package com.vbforge.focussprint;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db implements AutoCloseable {

    private final Connection conn;

    public Db() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".focussprint");
            Files.createDirectories(dir);
            Path file = dir.resolve("focussprint.db");

            this.conn = DriverManager.getConnection("jdbc:sqlite:" + file);

            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
            }

            createSchema();
            seedIfEmpty();
        } catch (Exception e) {
            throw new RuntimeException("Could not open database", e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS sections (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    name       TEXT NOT NULL,
                    tone       TEXT NOT NULL DEFAULT 'orange',
                    sort_order INTEGER NOT NULL DEFAULT 0
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS sessions (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    section_id INTEGER NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
                    started_at INTEGER NOT NULL,   -- epoch millis
                    ended_at   INTEGER NOT NULL,
                    seconds    INTEGER NOT NULL
                )
            """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_sessions_started ON sessions(started_at)");
        }
    }

    private void seedIfEmpty() throws SQLException {
        try (var s = conn.createStatement();
             var rs = s.executeQuery("SELECT COUNT(*) FROM sections")) {
            if (rs.next() && rs.getLong(1) > 0) return;
        }
        try (var ps = conn.prepareStatement(
                "INSERT INTO sections(name, tone, sort_order) VALUES (?, ?, ?)")) {
            String[][] seed = {
                {"Code",  "orange"},
                {"Read",  "blue"},
                {"Sport", "green"},
                {"Write", "gray"},
            };
            for (int i = 0; i < seed.length; i++) {
                ps.setString(1, seed[i][0]);
                ps.setString(2, seed[i][1]);
                ps.setInt(3, i);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public Connection conn() {
        return conn;
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) { }
    }
}