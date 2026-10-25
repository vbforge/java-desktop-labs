package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.GameState;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BoardFactoryTest {

    @Test
    void sameSeedProducesSameGame() {
        GameState a = BoardFactory.newGame(7L, 3, 2, 5);
        GameState b = BoardFactory.newGame(7L, 3, 2, 5);
        assertArrayEquals(a.board().flatten(), b.board().flatten());
        assertEquals(a.goals(), b.goals());
        assertEquals(a.rngState(), b.rngState());
        assertEquals(a.seed(), b.seed());
    }

    @Test
    void differentSeedProducesDifferentBoards() {
        GameState a = BoardFactory.newGame(1L, 3, 2, 5);
        GameState b = BoardFactory.newGame(2L, 3, 2, 5);
        assertNotEquals(Arrays.deepToString(a.board().flatten()),
                        Arrays.deepToString(b.board().flatten()));
    }

    @Test
    void initialStateHasCorrectDefaults() {
        GameState s = BoardFactory.newGame(3L, 3, 2, 5);
        assertEquals(3L, s.seed());
        assertEquals(3, s.maxDepth());
        assertEquals(2, s.playerCount());
        assertEquals(5, s.maxTurns());
        assertEquals(0, s.turnIndex());
        assertEquals(0, s.movesUsed());
        assertEquals(2, s.goals().size());
        assertEquals(0, s.board().level());
        assertFalse(s.isGameOver());
    }

    @Test
    void twoPlayerGoalsAlwaysHaveDistinctColors() {
        for (long seed = 0; seed < 50; seed++) {
            GameState s = BoardFactory.newGame(seed, 3, 2, 5);
            assertNotEquals(s.goals().get(0).targetColor(),
                            s.goals().get(1).targetColor(),
                            "seed " + seed);
        }
    }

    @Test
    void singlePlayerGameHasOneGoal() {
        GameState s = BoardFactory.newGame(3L, 3, 1, 5);
        assertEquals(1, s.playerCount());
        assertEquals(1, s.goals().size());
        assertEquals(0, s.turnIndex());
    }

    @Test
    void rejectsInvalidParams() {
        assertThrows(IllegalArgumentException.class, () -> BoardFactory.newGame(1L, 0, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> BoardFactory.newGame(1L, 9, 1, 5));
        assertThrows(IllegalArgumentException.class, () -> BoardFactory.newGame(1L, 3, 0, 5));
        assertThrows(IllegalArgumentException.class, () -> BoardFactory.newGame(1L, 3, 3, 5));
        assertThrows(IllegalArgumentException.class, () -> BoardFactory.newGame(1L, 3, 1, 0));
    }
}