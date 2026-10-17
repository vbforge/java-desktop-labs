package com.vbforge.clickrush;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class ScoreDao {
    private final Db db;

    public ScoreDao(Db db) {
        this.db = db;
    }

    public double getBest(String gameId, double defaultValue) {
        try (PreparedStatement ps = db.conn().prepareStatement(
                "SELECT best_score FROM scores WHERE game_id = ?")) {
            ps.setString(1, gameId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("best_score");
            }
        } catch (SQLException ignored) {}
        return defaultValue;
    }

    public void saveBest(String gameId, double score) {
        try (PreparedStatement ps = db.conn().prepareStatement("""
            INSERT INTO scores(game_id, best_score) VALUES (?, ?)
            ON CONFLICT(game_id) DO UPDATE SET best_score = excluded.best_score
        """)) {
            ps.setString(1, gameId);
            ps.setDouble(2, score);
            ps.executeUpdate();
        } catch (SQLException ignored) {}
    }
}