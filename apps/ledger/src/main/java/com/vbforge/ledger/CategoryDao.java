package com.vbforge.ledger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class CategoryDao {

    private final Db db;

    public CategoryDao(Db db) {
        this.db = db;
    }

    public List<Category> findAll() throws SQLException {
        try (Statement s = db.conn().createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name FROM categories ORDER BY name")) {
            List<Category> out = new ArrayList<>();
            while (rs.next()) out.add(new Category(rs.getLong("id"), rs.getString("name")));
            return out;
        }
    }

    public void insert(String name) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "INSERT OR IGNORE INTO categories(name) VALUES (?)")) {
            ps.setString(1, name);
            ps.executeUpdate();
        }
    }

    /** Delete a category. Transactions keep their row; category_id becomes NULL. */
    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "DELETE FROM categories WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }
}