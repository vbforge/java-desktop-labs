package com.vbforge.temptrack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CityDao implements AutoCloseable {
    private final Connection conn;

    public CityDao() {
        this(Paths.get(System.getProperty("user.home"), ".temptrack", "temptrack.db").toString());
    }

    public CityDao(String dbPath) {
        try {
            if (!dbPath.equals(":memory:")) {
                Path path = Paths.get(dbPath);
                Files.createDirectories(path.getParent());
            }
            
            // Keep the connection open so the in-memory DB isn't destroyed
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            
            try (Statement s = conn.createStatement()) {
                s.execute("""
                    CREATE TABLE IF NOT EXISTS cities (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        country TEXT,
                        lat REAL NOT NULL,
                        lon REAL NOT NULL
                    )
                """);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not init DB", e);
        }
    }

    public List<City> findAll() {
        List<City> list = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM cities ORDER BY id")) {
            while (rs.next()) {
                list.add(new City(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("country"),
                        rs.getDouble("lat"),
                        rs.getDouble("lon")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void save(City city) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO cities(name, country, lat, lon) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, city.name());
            ps.setString(2, city.country());
            ps.setDouble(3, city.lat());
            ps.setDouble(4, city.lon());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(long id) {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM cities WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}