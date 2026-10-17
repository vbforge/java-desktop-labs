package com.vbforge.clickrush;

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
            Path dir = Paths.get(System.getProperty("user.home"), ".clickrush");
            Files.createDirectories(dir);
            Path file = dir.resolve("clickrush.db");
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + file);

            try (Statement s = conn.createStatement()) {
                s.execute("""
                    CREATE TABLE IF NOT EXISTS scores (
                        game_id TEXT PRIMARY KEY,
                        best_score REAL NOT NULL
                    )
                """);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not open database", e);
        }
    }

    public Connection conn() { return conn; }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) {}
    }
}