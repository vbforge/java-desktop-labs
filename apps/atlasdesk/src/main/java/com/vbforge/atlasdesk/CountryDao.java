package com.vbforge.atlasdesk;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;

public final class CountryDao {

    public static final class DataAccessException extends RuntimeException {
        public DataAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    static final String INSERT_SQL = """
            INSERT INTO countries (code, name, continent, capital, country_pop, capital_pop, area, latitude, longitude)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String UPDATE_SQL = """
            UPDATE countries SET code = ?, name = ?, continent = ?, capital = ?,
                   country_pop = ?, capital_pop = ?, area = ?, latitude = ?, longitude = ?
            WHERE id = ?
            """;

    private final Db db;

    public CountryDao(Db db) {
        this.db = db;
    }

    public List<Country> findAll() {
        String sql = "SELECT id, code, name, continent, capital, country_pop, capital_pop, area, latitude, longitude "
                + "FROM countries ORDER BY name COLLATE NOCASE";
        List<Country> list = new ArrayList<>();
        try (Statement s = db.conn().createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Country(
                        rs.getInt("id"),
                        rs.getString("code"),
                        rs.getString("name"),
                        rs.getString("continent"),
                        rs.getString("capital"),
                        rs.getLong("country_pop"),
                        rs.getLong("capital_pop"),
                        rs.getLong("area"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude")));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Could not load countries", e);
        }
    }

    public boolean codeExists(String code, int excludeId) {
        try (PreparedStatement ps = db.conn().prepareStatement("SELECT 1 FROM countries WHERE code = ? AND id <> ?")) {
            ps.setString(1, code);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not check country code", e);
        }
    }

    public Country insert(Country c) {
        try (PreparedStatement ps = db.conn().prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, c);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                int id = keys.next() ? keys.getInt(1) : 0;
                return new Country(id, c.code(), c.name(), c.continent(), c.capital(),
                        c.countryPopulation(), c.capitalPopulation(), c.area(), c.latitude(), c.longitude());
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not add " + c.name() + ": " + e.getMessage(), e);
        }
    }

    public void update(Country c) {
        try (PreparedStatement ps = db.conn().prepareStatement(UPDATE_SQL)) {
            bind(ps, c);
            ps.setInt(10, c.id());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not update " + c.name() + ": " + e.getMessage(), e);
        }
    }

    public void delete(int id) {
        try (PreparedStatement ps = db.conn().prepareStatement("DELETE FROM countries WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not delete country", e);
        }
    }

    /**
     * Inserts all countries in ONE transaction; rows whose code already exists are ignored.
     * @return how many rows were actually inserted
     */
    public int insertIgnoringDuplicates(List<Country> countries) {
        String sql = INSERT_SQL + " ON CONFLICT(code) DO NOTHING";
        Connection conn = db.conn();
        try {
            conn.setAutoCommit(false);
            int added = 0;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Country c : countries) {
                    bind(ps, c);
                    added += ps.executeUpdate();   // 1 = inserted, 0 = duplicate ignored
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            return added;
        } catch (SQLException e) {
            throw new CountryDao.DataAccessException("Import failed, nothing was saved: " + e.getMessage(), e);
        }
    }

    /** Binds parameters 1..9 shared by INSERT and UPDATE. */
    static void bind(PreparedStatement ps, Country c) throws SQLException {
        ps.setString(1, c.code());
        ps.setString(2, c.name());
        ps.setString(3, c.continent());
        ps.setString(4, c.capital());
        ps.setLong(5, c.countryPopulation());
        ps.setLong(6, c.capitalPopulation());
        ps.setLong(7, c.area());
        ps.setDouble(8, c.latitude());
        ps.setDouble(9, c.longitude());
    }
}