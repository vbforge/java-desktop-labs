package com.vbforge.fitapp.db;

import com.vbforge.fitapp.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserDao {

    private final Connection conn;

    public UserDao(Database db) {
        this.conn = db.connection();
    }

    public User insert(String username, String email, String hash, String salt) throws SQLException {
        String now = LocalDateTime.now().toString();
        String sql = "INSERT INTO users(username, email, password_hash, salt, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, hash);
            ps.setString(4, salt);
            ps.setString(5, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new User(keys.getLong(1), username, email, hash, salt, now);
            }
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        return findOne("SELECT * FROM users WHERE username = ? COLLATE NOCASE", username);
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        return findOne("SELECT * FROM users WHERE email = ? COLLATE NOCASE", email);
    }

    public Optional<User> findById(long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public void updatePassword(long userId, String hash, String salt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, salt);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    private Optional<User> findOne(String sql, String param) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private static User map(ResultSet rs) throws SQLException {
        return new User(rs.getLong("id"), rs.getString("username"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("salt"), rs.getString("created_at"));
    }
}
