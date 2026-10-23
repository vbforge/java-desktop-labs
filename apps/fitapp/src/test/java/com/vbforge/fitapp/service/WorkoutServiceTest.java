package com.vbforge.fitapp.service;

import com.vbforge.fitapp.db.*;
import com.vbforge.fitapp.model.Exercise;
import com.vbforge.fitapp.model.Workout;
import com.vbforge.fitapp.session.SessionManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorkoutServiceTest {

    private Database db;
    private WorkoutService service;

    @BeforeEach
    void setUp() throws Exception {
        db = Database.openInMemory();
        SessionManager session = new SessionManager();
        AuthService auth = new AuthService(new UserDao(db), new LoginHistoryDao(db), session);
        auth.register("john", "john@example.com", "password1");
        service = new WorkoutService(new WorkoutDao(db), new ExerciseDao(db), session);
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void validatesInput() {
        assertThrows(ServiceException.class, () -> service.addWorkout("  "));
        assertThrows(ServiceException.class, () -> service.addExercise("Squat", 0, 5));
        assertThrows(ServiceException.class, () -> service.addExercise("Squat", 3, -2));
        assertThrows(ServiceException.class, () -> service.addExercise("", 3, 5));
    }

    @Test
    void fullWorkflow() {
        Workout w = service.addWorkout("Push Day");
        Exercise bench = service.addExercise("Bench Press", 4, 8);
        service.addExercise("Dips", 3, 12);

        service.addToWorkout(w.id(), bench.id());
        assertEquals(1, service.exercisesIn(w.id()).size());
        assertEquals(1, service.exercisesNotIn(w.id()).size());
        assertEquals(1, service.listWorkouts().get(0).exerciseCount());

        service.removeFromWorkout(w.id(), bench.id());
        assertEquals(0, service.exercisesIn(w.id()).size());

        service.deleteWorkout(w.id());
        assertTrue(service.listWorkouts().isEmpty());
        assertEquals(2, service.listExercises().size());
    }
}
