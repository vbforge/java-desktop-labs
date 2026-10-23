package com.vbforge.fitapp.service;

import com.vbforge.fitapp.db.ExerciseDao;
import com.vbforge.fitapp.db.WorkoutDao;
import com.vbforge.fitapp.model.Exercise;
import com.vbforge.fitapp.model.Workout;
import com.vbforge.fitapp.session.SessionManager;

import java.sql.SQLException;
import java.util.List;

/** Workout and exercise CRUD, always scoped to the logged-in user. */
public class WorkoutService {

    private final WorkoutDao workouts;
    private final ExerciseDao exercises;
    private final SessionManager session;

    public WorkoutService(WorkoutDao workouts, ExerciseDao exercises, SessionManager session) {
        this.workouts = workouts;
        this.exercises = exercises;
        this.session = session;
    }

    // ---- workouts --------------------------------------------------------

    public List<Workout> listWorkouts() {
        return run(() -> workouts.findAll(session.getUserId()));
    }

    public Workout addWorkout(String name) {
        check(Validator.name(name, "Workout name"));
        return run(() -> workouts.insert(session.getUserId(), name.trim()));
    }

    public void deleteWorkout(long id) {
        run(() -> {
            workouts.delete(session.getUserId(), id);
            return null;
        });
    }

    // ---- exercises -------------------------------------------------------

    public List<Exercise> listExercises() {
        return run(() -> exercises.findAll(session.getUserId()));
    }

    public Exercise addExercise(String name, int sets, int reps) {
        check(Validator.name(name, "Exercise name"));
        check(Validator.sets(sets));
        check(Validator.reps(reps));
        return run(() -> exercises.insert(session.getUserId(), name.trim(), sets, reps));
    }

    public void updateExercise(long id, String name, int sets, int reps) {
        check(Validator.name(name, "Exercise name"));
        check(Validator.sets(sets));
        check(Validator.reps(reps));
        run(() -> {
            exercises.update(session.getUserId(), id, name.trim(), sets, reps);
            return null;
        });
    }

    public void deleteExercise(long id) {
        run(() -> {
            exercises.delete(session.getUserId(), id);
            return null;
        });
    }

    // ---- linking ---------------------------------------------------------

    public List<Exercise> exercisesIn(long workoutId) {
        return run(() -> exercises.findByWorkout(session.getUserId(), workoutId));
    }

    public List<Exercise> exercisesNotIn(long workoutId) {
        return run(() -> exercises.findNotInWorkout(session.getUserId(), workoutId));
    }

    public void addToWorkout(long workoutId, long exerciseId) {
        run(() -> {
            workouts.link(session.getUserId(), workoutId, exerciseId);
            return null;
        });
    }

    public void removeFromWorkout(long workoutId, long exerciseId) {
        run(() -> {
            workouts.unlink(session.getUserId(), workoutId, exerciseId);
            return null;
        });
    }

    // ---- plumbing --------------------------------------------------------

    private interface SqlAction<T> {
        T get() throws SQLException;
    }

    private static <T> T run(SqlAction<T> action) {
        try {
            return action.get();
        } catch (SQLException e) {
            throw new ServiceException("Database error: " + e.getMessage(), e);
        }
    }

    private static void check(String error) {
        if (error != null) throw new ServiceException(error);
    }
}
