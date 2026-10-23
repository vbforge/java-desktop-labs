package com.vbforge.fitapp.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LoginHistoryDao {

    private final Connection conn;

    public LoginHistoryDao(Database db) {
        this.conn = db.connection();
    }

    public void record(long userId) throws SQLException {
        record(userId, LocalDateTime.now());
    }

    public void record(long userId, LocalDateTime time) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO login_history(user_id, login_time) VALUES (?,?)")) {
            ps.setLong(1, userId);
            ps.setString(2, time.toString());
            ps.executeUpdate();
        }
    }

    /** Most recent logins first. */
    public List<LocalDateTime> recent(long userId, int limit) throws SQLException {
        List<LocalDateTime> result = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT login_time FROM login_history WHERE user_id = ? ORDER BY id DESC LIMIT ?")) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(LocalDateTime.parse(rs.getString(1)));
                }
            }
        }
        return result;
    }
}
