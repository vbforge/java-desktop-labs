package com.vbforge.blockforge.model;

import com.vbforge.blockforge.engine.BoardFactory;
import com.vbforge.blockforge.engine.JavaUtilRandomCompat;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.vbforge.blockforge.model.BlockColor.RED;
import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    private static Block smallBoard(int maxDepth) {
        Block b = new Block(0, maxDepth, new JavaUtilRandomCompat(1L));
        b.updateSizeAndPosition(1 << maxDepth, 0, 0);
        return b;
    }

    @Test
    void rejectsBadRngState() {
        Block board = smallBoard(2);
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 1L << 50, 2, 1, 5, 0, 0, List.of(new BlobGoal(RED)), board));
    }

    @Test
    void rejectsBadMaxDepth() {
        Block board = smallBoard(2);
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 0L, 0, 1, 5, 0, 0, List.of(new BlobGoal(RED)), board));
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 0L, 9, 1, 5, 0, 0, List.of(new BlobGoal(RED)), board));
    }

    @Test
    void rejectsBadPlayerCount() {
        Block board = smallBoard(2);
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 0L, 2, 0, 5, 0, 0, List.of(), board));
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 0L, 2, 3, 5, 0, 0, List.of(new BlobGoal(RED), new BlobGoal(RED), new BlobGoal(RED)), board));
    }

    @Test
    void rejectsBadTurnIndex() {
        Block board = smallBoard(2);
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 0L, 2, 2, 5, 2, 0, List.of(new BlobGoal(RED), new PerimeterGoal(RED)), board));
    }

    @Test
    void rejectsMismatchedGoalCount() {
        Block board = smallBoard(2);
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 0L, 2, 2, 5, 0, 0, List.of(new BlobGoal(RED)), board));
    }

    @Test
    void rejectsBoardWithWrongMaxDepth() {
        Block board = new Block(0, 3, new JavaUtilRandomCompat(1L));
        board.updateSizeAndPosition(8, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> new GameState(
                1L, 0L, 2, 1, 5, 0, 0, List.of(new BlobGoal(RED)), board));
    }

    @Test
    void scoresAreComputedFromGoals() {
        GameState s = BoardFactory.newGame(2L, 2, 2, 5);
        int[] scores = s.scores();
        assertEquals(2, scores.length);
        for (int i = 0; i < 2; i++) {
            assertEquals(s.goals().get(i).score(s.board()), scores[i]);
        }
    }

    @Test
    void isGameOverOnlyWhenTurnsExhausted() {
        GameState initial = BoardFactory.newGame(4L, 3, 2, 2);
        assertFalse(initial.isGameOver());

        var session = new com.vbforge.blockforge.engine.GameSession(initial);
        for (int i = 0; i < 4; i++) {
            assertTrue(session.applyMove(0, 0, 1, Move.SMASH), "move " + i);
        }
        assertTrue(session.state().isGameOver());
    }
}