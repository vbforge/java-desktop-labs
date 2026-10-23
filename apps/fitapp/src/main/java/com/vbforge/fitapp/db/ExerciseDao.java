package com.vbforge.fitapp.db;

import com.vbforge.fitapp.model.Exercise;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ExerciseDao {

    private final Connection conn;

    public ExerciseDao(Database db) {
        this.conn = db.connection();
    }

    public Exercise insert(long userId, String name, int sets, int reps) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO exercises(user_id, name, sets, reps) VALUES (?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, name);
            ps.setInt(3, sets);
            ps.setInt(4, reps);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return new Exercise(keys.getLong(1), userId, name, sets, reps);
            }
        }
    }

    public void update(long userId, long id, String name, int sets, int reps) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE exercises SET name = ?, sets = ?, reps = ? WHERE id = ? AND user_id = ?")) {
            ps.setString(1, name);
            ps.setInt(2, sets);
            ps.setInt(3, reps);
            ps.setLong(4, id);
            ps.setLong(5, userId);
            ps.executeUpdate();
        }
    }

    public void delete(long userId, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM exercises WHERE id = ? AND user_id = ?")) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public List<Exercise> findAll(long userId) throws SQLException {
        return query("SELECT * FROM exercises WHERE user_id = ? ORDER BY name COLLATE NOCASE", userId);
    }

    public List<Exercise> findByWorkout(long userId, long workoutId) throws SQLException {
        return query("""
                SELECT e.* FROM exercises e
                JOIN workout_exercises we ON we.exercise_id = e.id
                JOIN workouts w ON w.id = we.workout_id
                WHERE w.id = ? AND w.user_id = ? ORDER BY e.name COLLATE NOCASE""", workoutId, userId);
    }

    public List<Exercise> findNotInWorkout(long userId, long workoutId) throws SQLException {
        return query("""
                SELECT * FROM exercises
                WHERE user_id = ?
                  AND id NOT IN (SELECT exercise_id FROM workout_exercises WHERE workout_id = ?)
                ORDER BY name COLLATE NOCASE""", userId, workoutId);
    }

    private List<Exercise> query(String sql, long... params) throws SQLException {
        List<Exercise> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setLong(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new Exercise(rs.getLong("id"), rs.getLong("user_id"), rs.getString("name"),
                            rs.getInt("sets"), rs.getInt("reps")));
                }
            }
        }
        return list;
    }
}
