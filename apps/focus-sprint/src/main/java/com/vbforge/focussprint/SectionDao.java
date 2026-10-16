package com.vbforge.focussprint;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SectionDao {

    private final Db db;

    public SectionDao(Db db) {
        this.db = db;
    }

    public List<Section> findAll() throws SQLException {
        try (Statement s = db.conn().createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT id, name, tone, sort_order FROM sections ORDER BY sort_order, id")) {
            List<Section> out = new ArrayList<>();
            while (rs.next()) {
                out.add(new Section(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("tone"),
                        rs.getInt("sort_order")));
            }
            return out;
        }
    }

    public Section insert(String name, String tone, int sortOrder) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT INTO sections(name, tone, sort_order) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, tone);
            ps.setInt(3, sortOrder);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Section(keys.getLong(1), name, tone, sortOrder);
            }
        }
    }

    public void rename(long id, String newName) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "UPDATE sections SET name = ? WHERE id = ?")) {
            ps.setString(1, newName);
            ps.setLong(2, id);
            ps.executeUpdate();
        }
    }

    /**
     * Deletes a section and every session that references it.
     * ON DELETE CASCADE in the schema means the sessions go with it —
     * which is why the UI asks for confirmation before calling this.
     */
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "DELETE FROM sections WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}