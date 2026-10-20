package com.vbforge.teamcraft;

import java.io.File;
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
        this(Paths.get(System.getProperty("user.home"), ".teamcraft", "teamcraft.db").toString());
    }

    public Db(String dbPath) {
        try {
            if (!dbPath.equals(":memory:")) {
                Path path = Paths.get(dbPath);
                Files.createDirectories(path.getParent());
            }
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            createSchema();
        } catch (Exception e) {
            throw new RuntimeException("Could not open database", e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS employees (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    job TEXT NOT NULL,
                    age INTEGER NOT NULL,
                    gender TEXT NOT NULL
                )
            """);
        }
    }

    public Connection conn() { return conn; }

    @Override
    public void close() {
        try { if (conn != null) conn.close(); } catch (SQLException ignored) {}
    }
}