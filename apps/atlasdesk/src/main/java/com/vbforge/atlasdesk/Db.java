package com.vbforge.atlasdesk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class Db implements AutoCloseable {

    private static final System.Logger LOG = System.getLogger(Db.class.getName());

    private final Connection conn;

    public Db() {
        try {
            Path dir = Paths.get(System.getProperty("user.home"), ".atlasdesk");
            Files.createDirectories(dir);
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dir.resolve("atlas.db").toAbsolutePath());
            createSchema();
            migrate();
            seedIfEmpty();
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize database: " + e.getMessage(), e);
        }
    }

    private void createSchema() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS countries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    code TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    continent TEXT NOT NULL,
                    capital TEXT NOT NULL,
                    country_pop INTEGER NOT NULL,
                    capital_pop INTEGER NOT NULL,
                    area INTEGER NOT NULL,
                    latitude REAL NOT NULL,
                    longitude REAL NOT NULL
                )
            """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_countries_continent ON countries(continent)");
        }
    }

    /** One-off data fixes for databases created by earlier versions. */
    private void migrate() throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.executeUpdate("UPDATE countries SET capital = 'Kyiv' WHERE code = 'UA' AND capital = 'Kiev'");
            s.executeUpdate("UPDATE countries SET name = 'United Kingdom' WHERE code = 'GB' AND name = 'Great Britain'");
        }
    }

    private void seedIfEmpty() throws Exception {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM countries")) {
            if (rs.next() && rs.getLong(1) > 0) return;
        }

        try (InputStream in = Db.class.getResourceAsStream("/seed-countries.json")) {
            if (in == null) {
                LOG.log(System.Logger.Level.WARNING, "seed-countries.json not found; starting with an empty database");
                return;
            }
            List<Country> seed = new ObjectMapper().readValue(in, new TypeReference<List<Country>>() {});

            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(CountryDao.INSERT_SQL)) {
                for (Country c : seed) {
                    CountryDao.bind(ps, c);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Connection conn() {
        return conn;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            LOG.log(System.Logger.Level.WARNING, "Error closing database", e);
        }
    }
}