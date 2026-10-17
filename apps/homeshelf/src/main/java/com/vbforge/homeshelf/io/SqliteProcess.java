package com.vbforge.homeshelf.io;

import com.vbforge.homeshelf.MediaItem;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SqliteProcess implements DataProcess {
    private final File file;

    public SqliteProcess(File file) { this.file = file; }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());
    }

    @Override
    public void save(List<MediaItem> items) throws Exception {
        try (Connection c = connect()) {
            c.setAutoCommit(false);
            try (Statement s = c.createStatement()) {
                s.execute("CREATE TABLE IF NOT EXISTS media (title TEXT, creator TEXT, year INTEGER, type TEXT, rating INTEGER)");
                s.execute("DELETE FROM media");
            }
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO media VALUES (?, ?, ?, ?, ?)")) {
                for (MediaItem i : items) {
                    ps.setString(1, i.title());
                    ps.setString(2, i.creator());
                    ps.setInt(3, i.year());
                    ps.setString(4, i.type());
                    ps.setInt(5, i.rating());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
        }
    }

    @Override
    public List<MediaItem> load() throws Exception {
        List<MediaItem> items = new ArrayList<>();
        if (!file.exists()) return items;

        try (Connection c = connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM media")) {
            while (rs.next()) {
                items.add(new MediaItem(
                        rs.getString("title"),
                        rs.getString("creator"),
                        rs.getInt("year"),
                        rs.getString("type"),
                        rs.getInt("rating")
                ));
            }
        } catch (SQLException e) {
            // Table might not exist yet
            return items;
        }
        return items;
    }
}