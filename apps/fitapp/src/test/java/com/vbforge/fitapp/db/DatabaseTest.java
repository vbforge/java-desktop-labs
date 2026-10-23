package com.vbforge.fitapp.db;

import com.vbforge.fitapp.model.Exercise;
import com.vbforge.fitapp.model.User;
import com.vbforge.fitapp.model.Workout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    private Database db;
    private UserDao users;
    private WorkoutDao workouts;
    private ExerciseDao exercises;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() throws Exception {
        db = Database.openInMemory();
        users = new UserDao(db);
        workouts = new WorkoutDao(db);
        exercises = new ExerciseDao(db);
        alice = users.insert("alice", "alice@example.com", "h", "s");
        bob = users.insert("bob", "bob@example.com", "h", "s");
    }

    @AfterEach
    void tearDown() {
        db.close();
    }

    @Test
    void usersAreUnique() throws Exception {
        assertThrows(SQLException.class, () -> users.insert("alice", "x@example.com", "h", "s"));
        assertThrows(SQLException.class, () -> users.insert("zed", "alice@example.com", "h", "s"));
        assertTrue(users.findByUsername("ALICE").isPresent());
        assertTrue(users.findByEmail("bob@example.com").isPresent());
    }

    @Test
    void loginHistoryNewestFirstAndLimited() throws Exception {
        LoginHistoryDao h = new LoginHistoryDao(db);
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 8, 0);
        for (int i = 0; i < 7; i++) h.record(alice.id(), base.plusDays(i));
        List<LocalDateTime> recent = h.recent(alice.id(), 5);
        assertEquals(5, recent.size());
        assertEquals(base.plusDays(6), recent.get(0));
        assertTrue(h.recent(bob.id(), 5).isEmpty());
    }

    @Test
    void exerciseCrudWithSetsAndReps() throws Exception {
        Exercise e = exercises.insert(alice.id(), "Bench Press", 4, 8);
        assertEquals(4, exercises.findAll(alice.id()).get(0).sets());
        exercises.update(alice.id(), e.id(), "Bench", 5, 5);
        Exercise updated = exercises.findAll(alice.id()).get(0);
        assertEquals("Bench", updated.name());
        assertEquals(5, updated.reps());
        exercises.delete(alice.id(), e.id());
        assertTrue(exercises.findAll(alice.id()).isEmpty());
    }

    @Test
    void dataIsIsolatedBetweenUsers() throws Exception {
        Workout w = workouts.insert(alice.id(), "Push Day");
        exercises.insert(alice.id(), "Squat", 5, 5);
        assertTrue(workouts.findAll(bob.id()).isEmpty());
        assertTrue(exercises.findAll(bob.id()).isEmpty());
        workouts.delete(bob.id(), w.id()); // must not delete Alice's workout
        assertEquals(1, workouts.findAll(alice.id()).size());
    }

    @Test
    void linkAndUnlinkExercises() throws Exception {
        Workout w = workouts.insert(alice.id(), "Push Day");
        Exercise a = exercises.insert(alice.id(), "Bench Press", 4, 8);
        Exercise b = exercises.insert(alice.id(), "Dips", 3, 12);

        workouts.link(alice.id(), w.id(), a.id());
        workouts.link(alice.id(), w.id(), a.id()); // duplicate link is ignored
        assertEquals(1, workouts.findAll(alice.id()).get(0).exerciseCount());
        assertEquals(List.of("Bench Press"), exercises.findByWorkout(alice.id(), w.id()).stream().map(Exercise::name).toList());
        assertEquals(List.of("Dips"), exercises.findNotInWorkout(alice.id(), w.id()).stream().map(Exercise::name).toList());

        workouts.unlink(alice.id(), w.id(), a.id());
        assertEquals(0, workouts.findAll(alice.id()).get(0).exerciseCount());
        assertEquals(2, exercises.findNotInWorkout(alice.id(), w.id()).size());
    }

    @Test
    void cannotLinkAnotherUsersExercise() throws Exception {
        Workout w = workouts.insert(alice.id(), "Push Day");
        Exercise bobs = exercises.insert(bob.id(), "Curl", 3, 10);
        workouts.link(alice.id(), w.id(), bobs.id());
        assertTrue(exercises.findByWorkout(alice.id(), w.id()).isEmpty());
    }

    @Test
    void deletingWorkoutOrExerciseCascadesLinks() throws Exception {
        Workout w = workouts.insert(alice.id(), "Push Day");
        Exercise e = exercises.insert(alice.id(), "Bench Press", 4, 8);
        workouts.link(alice.id(), w.id(), e.id());
        exercises.delete(alice.id(), e.id());
        assertEquals(0, workouts.findAll(alice.id()).get(0).exerciseCount());
        assertEquals(1, workouts.findAll(alice.id()).size());
        workouts.delete(alice.id(), w.id());
        assertTrue(workouts.findAll(alice.id()).isEmpty());
    }
}
