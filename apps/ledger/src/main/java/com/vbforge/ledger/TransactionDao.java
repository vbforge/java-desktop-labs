package com.vbforge.ledger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * All reads and writes for the transactions table.
 *
 * PreparedStatement everywhere, even for values we control — it's the habit
 * that prevents SQL injection once user input enters the picture, which it
 * does the moment the description field exists.
 */
public final class TransactionDao {

    private final Db db;

    public TransactionDao(Db db) {
        this.db = db;
    }

    public List<Transaction> findAll() throws SQLException {
        String sql = """
            SELECT t.id, t.occurred_on, t.description, t.amount,
                   t.category_id, c.name AS category_name
            FROM transactions t
            LEFT JOIN categories c ON c.id = t.category_id
            ORDER BY t.occurred_on DESC, t.id DESC
        """;
        try (Statement s = db.conn().createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            List<Transaction> out = new ArrayList<>();
            while (rs.next()) out.add(map(rs));
            return out;
        }
    }

    public void insert(LocalDate date, String description, double amount, Long categoryId)
            throws SQLException {
        String sql = """
            INSERT INTO transactions(occurred_on, description, amount, category_id)
            VALUES (?, ?, ?, ?)
        """;
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, date.toString());
            ps.setString(2, description);
            ps.setDouble(3, amount);
            if (categoryId == null) ps.setNull(4, java.sql.Types.INTEGER);
            else                    ps.setLong(4, categoryId);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "DELETE FROM transactions WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static Transaction map(ResultSet rs) throws SQLException {
        long catId = rs.getLong("category_id");
        Long categoryId = rs.wasNull() ? null : catId;
        return new Transaction(
                rs.getLong("id"),
                LocalDate.parse(rs.getString("occurred_on")),
                rs.getString("description"),
                rs.getDouble("amount"),
                categoryId,
                rs.getString("category_name")
        );
    }
}