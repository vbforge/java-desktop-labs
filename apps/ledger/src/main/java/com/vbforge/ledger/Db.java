package com.vbforge.ledger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the single SQLite connection and the schema.
 *
 * SQLite file lives in ~/.ledger/ledger.db so it survives rebuilds and
 * isn't inside target/. Delete that file to start over.
 */
public final class Db implements AutoCloseable {

    private final Connection conn;

    public Db() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".ledger");
            Files.createDirectories(dir);
            Path file = dir.resolve("ledger.db");

            // SQLite JDBC will create the file if it doesn't exist.
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + file);

            // Enforce foreign keys — off by default in SQLite for legacy reasons.
            try (Statement s = conn.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
            }

            createSchema();
        } catch (Exception e) {
            throw new RuntimeException("Could not open database", e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    id   INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                )
            """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    occurred_on TEXT    NOT NULL,          -- ISO yyyy-MM-dd
                    description TEXT    NOT NULL,
                    amount      REAL    NOT NULL,          -- positive = income, negative = expense
                    category_id INTEGER REFERENCES categories(id) ON DELETE SET NULL
                )
            """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_tx_date ON transactions(occurred_on)");
        }

        // Seed a few categories the first time only.
        try (var ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO categories(name) VALUES (?)")) {
            for (String name : new String[]{"Salary", "Groceries", "Rent", "Transport", "Other"}) {
                ps.setString(1, name);
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
        } catch (SQLException ignored) {
            // nothing useful to do at shutdown
        }
    }
}