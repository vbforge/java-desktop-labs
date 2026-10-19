package com.vbforge.quizmaster;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Db {
    private final Connection conn;

    public Db() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".quizmaster");
            Files.createDirectories(dir);
            File dbFile = dir.resolve("quiz.db").toFile();
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            
            try (Statement s = conn.createStatement()) {
                // 1. Create the table if it doesn't exist
                s.execute("""
                    CREATE TABLE IF NOT EXISTS questions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        category TEXT NOT NULL,
                        text TEXT NOT NULL,
                        option_a TEXT NOT NULL,
                        option_b TEXT NOT NULL,
                        option_c TEXT NOT NULL,
                        option_d TEXT NOT NULL,
                        correct_index INTEGER NOT NULL
                    )
                """);

                // 2. Clean up any existing duplicates in the database
                // This keeps the lowest ID for each unique category + text pair
                s.execute("""
                    DELETE FROM questions 
                    WHERE id NOT IN (
                        SELECT MIN(id) FROM questions GROUP BY category, text
                    )
                """);

                // 3. Now it is safe to create the unique index
                s.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_questions_cat_text ON questions(category, text)");
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not open database", e);
        }
    }

    public Connection conn() { return conn; }
}