package com.vbforge.fitapp.db;

import com.vbforge.fitapp.model.Workout;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class WorkoutDao {

    private final Connection conn;

    public WorkoutDao(Database db) {
        this.conn = db.connection();
    }

    public Workout insert(long userId, String name) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO workouts(user_id, name) VALUES (?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Workout(keys.getLong(1), userId, name, 0);
            }
        }
    }

    public void delete(long userId, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM workouts WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public List<Workout> findAll(long userId) throws SQLException {
        List<Workout> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("""
                SELECT w.id, w.user_id, w.name, COUNT(we.exercise_id) AS cnt
                FROM workouts w LEFT JOIN workout_exercises we ON we.workout_id = w.id
                WHERE w.user_id = ?
                GROUP BY w.id ORDER BY w.name COLLATE NOCASE""")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Workout(rs.getLong("id"), rs.getLong("user_id"),
                            rs.getString("name"), rs.getInt("cnt")));
                }
            }
        }
        return list;
    }

    /** Links an exercise to a workout, only if both belong to the user. */
    public void link(long userId, long workoutId, long exerciseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                INSERT OR IGNORE INTO workout_exercises(workout_id, exercise_id)
                SELECT w.id, e.id FROM workouts w, exercises e
                WHERE w.id = ? AND e.id = ? AND w.user_id = ? AND e.user_id = ?""")) {
            ps.setLong(1, workoutId);
            ps.setLong(2, exerciseId);
            ps.setLong(3, userId);
            ps.setLong(4, userId);
            ps.executeUpdate();
        }
    }

    public void unlink(long userId, long workoutId, long exerciseId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("""
                DELETE FROM workout_exercises
                WHERE workout_id = ? AND exercise_id = ?
                  AND workout_id IN (SELECT id FROM workouts WHERE user_id = ?)""")) {
            ps.setLong(1, workoutId);
            ps.setLong(2, exerciseId);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }
}
