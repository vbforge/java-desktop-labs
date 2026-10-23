package com.vbforge.fitapp.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Connection manager and schema creation.
 * Uses a single shared connection (SQLite is embedded, single-user desktop app).
 */
public class Database implements AutoCloseable {

    private static final String[] SCHEMA = {
            """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                created_at TEXT NOT NULL
            )""",
            """
            CREATE TABLE IF NOT EXISTS login_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                login_time TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )""",
            """
            CREATE TABLE IF NOT EXISTS workouts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )""",
            """
            CREATE TABLE IF NOT EXISTS exercises (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                name TEXT NOT NULL,
                sets INTEGER NOT NULL DEFAULT 3,
                reps INTEGER NOT NULL DEFAULT 10,
                FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
            )""",
            """
            CREATE TABLE IF NOT EXISTS workout_exercises (
                workout_id INTEGER NOT NULL,
                exercise_id INTEGER NOT NULL,
                PRIMARY KEY(workout_id, exercise_id),
                FOREIGN KEY(workout_id) REFERENCES workouts(id) ON DELETE CASCADE,
                FOREIGN KEY(exercise_id) REFERENCES exercises(id) ON DELETE CASCADE
            )"""
    };

    private final Connection connection;

    private Database(String url) throws SQLException {
        this.connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            for (String ddl : SCHEMA) {
                st.execute(ddl);
            }
        }
    }

    /** Opens (creating if needed) the database at ~/.fitapp/fitapp.db. */
    public static Database openDefault() throws SQLException {
        Path dir = AppPaths.appDir();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new SQLException("Cannot create data directory " + dir, e);
        }
        return new Database("jdbc:sqlite:" + dir.resolve("fitapp.db"));
    }

    /** Opens a private in-memory database (used by tests). */
    public static Database openInMemory() throws SQLException {
        return new Database("jdbc:sqlite::memory:");
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException ignored) {
            // nothing sensible to do on shutdown
        }
    }
}
