package com.vbforge.dietarytrack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** SQLite persistence for {@link DietaryEntry}. All failures surface as {@link StorageException}. */
public class DietaryDao implements AutoCloseable {

    public static final String IN_MEMORY = ":memory:";

    private final Connection conn;

    public DietaryDao() {
        this(AppPaths.database().toString());
    }

    public DietaryDao(String dbPath) {
        Connection connection = null;
        try {
            if (!IN_MEMORY.equals(dbPath)) {
                Path parent = Paths.get(dbPath).toAbsolutePath().getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
            initSchema(connection);
            this.conn = connection;
        } catch (IOException | SQLException e) {
            closeQuietly(connection);
            throw new StorageException("Could not open the database at " + dbPath, e);
        }
    }

    private static void initSchema(Connection connection) throws SQLException {
        try (Statement s = connection.createStatement()) {
            s.execute("""
                CREATE TABLE IF NOT EXISTS entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT NOT NULL,
                    firstName TEXT NOT NULL,
                    lastName TEXT NOT NULL,
                    phone TEXT,
                    email TEXT,
                    sex TEXT,
                    water INTEGER,
                    meals INTEGER,
                    wheat BOOLEAN,
                    sugar BOOLEAN,
                    dairy BOOLEAN,
                    miles TEXT,
                    weight REAL
                )
                """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_entries_date ON entries(date)");
        }
    }

    public synchronized void insert(DietaryEntry e) {
        String sql = """
            INSERT INTO entries(date, firstName, lastName, phone, email, sex, water, meals, wheat, sugar, dairy, miles, weight)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, e.date());
            ps.setString(2, e.firstName());
            ps.setString(3, e.lastName());
            ps.setString(4, e.phone());
            ps.setString(5, e.email());
            ps.setString(6, e.sex());
            ps.setInt(7, e.water());
            ps.setInt(8, e.meals());
            ps.setBoolean(9, e.wheat());
            ps.setBoolean(10, e.sugar());
            ps.setBoolean(11, e.dairy());
            ps.setString(12, e.miles());
            ps.setDouble(13, e.weight());
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new StorageException("Could not save the entry to the database", ex);
        }
    }

    public synchronized List<DietaryEntry> findAll() {
        List<DietaryEntry> list = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM entries ORDER BY date DESC, id DESC")) {
            while (rs.next()) {
                list.add(new DietaryEntry(
                        rs.getLong("id"),
                        rs.getString("date"),
                        rs.getString("firstName"),
                        rs.getString("lastName"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("sex"),
                        rs.getInt("water"),
                        rs.getInt("meals"),
                        rs.getBoolean("wheat"),
                        rs.getBoolean("sugar"),
                        rs.getBoolean("dairy"),
                        rs.getString("miles"),
                        rs.getDouble("weight")
                ));
            }
        } catch (SQLException ex) {
            throw new StorageException("Could not read entries from the database", ex);
        }
        return list;
    }

    @Override
    public synchronized void close() {
        closeQuietly(conn);
    }

    private static void closeQuietly(Connection c) {
        try {
            if (c != null && !c.isClosed()) {
                c.close();
            }
        } catch (SQLException ignored) {
            // nothing sensible to do while closing
        }
    }
}
