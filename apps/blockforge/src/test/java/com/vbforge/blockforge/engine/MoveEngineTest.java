package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.BlockColor;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MoveEngineTest {

    @Test
    void moveEngineDoesNotMutateInputBoard() {
        GameState before = BoardFactory.newGame(4L, 3, 2, 5);
        BlockColor[][] gridBefore = before.board().flatten();
        MoveEngine.apply(before, 0, 0, 1, Move.SMASH);
        assertArrayEquals(gridBefore, before.board().flatten(),
                "MoveEngine must not mutate the input board");
    }

    @Test
    void smashAdvancesRngState() {
        GameState before = BoardFactory.newGame(4L, 3, 2, 5);
        MoveResult r = MoveEngine.apply(before, 0, 0, 1, Move.SMASH);
        assertTrue(r.accepted());
        assertNotEquals(before.rngState(), r.stateAfter().rngState());
    }

    @Test
    void rotateDoesNotAdvanceRngState() {
        GameState s = BoardFactory.newGame(4L, 3, 2, 5);
        MoveResult smashed = MoveEngine.apply(s, 0, 0, 1, Move.SMASH);
        assertTrue(smashed.accepted());
        GameState afterSmash = smashed.stateAfter();
        long rngAfterSmash = afterSmash.rngState();

        MoveResult rotated = MoveEngine.apply(afterSmash, 0, 0, 1, Move.ROTATE_CW);
        assertTrue(rotated.accepted());
        assertEquals(rngAfterSmash, rotated.stateAfter().rngState(),
                "Rotations should not consume RNG");
    }

    @Test
    void rotateOnMaxDepthLeafIsRejected() {
        GameState s = BoardFactory.newGame(4L, 3, 1, 5);
        // A block at maxDepth is always a leaf, so rotate must be rejected.
        MoveResult r = MoveEngine.apply(s, 0, 0, 3, Move.ROTATE_CW);
        assertFalse(r.accepted());
        assertSame(s, r.stateAfter(), "Rejected move should return the same state instance");
    }

    @Test
    void smashOnTopLevelIsRejected() {
        GameState s = BoardFactory.newGame(4L, 3, 1, 5);
        MoveResult r = MoveEngine.apply(s, 0, 0, 0, Move.SMASH);
        assertFalse(r.accepted());
        assertSame(s, r.stateAfter());
    }

    @Test
    void outOfBoundsIsRejected() {
        GameState s = BoardFactory.newGame(4L, 3, 1, 5);
        MoveResult r = MoveEngine.apply(s, -1, 0, 1, Move.SMASH);
        assertFalse(r.accepted());
        assertSame(s, r.stateAfter());
    }

    @Test
    void turnAdvancesAfterMove() {
        GameState s = BoardFactory.newGame(4L, 3, 2, 5);
        assertEquals(0, s.turnIndex());

        MoveResult r1 = MoveEngine.apply(s, 0, 0, 1, Move.SMASH);
        assertTrue(r1.accepted());
        assertEquals(1, r1.stateAfter().turnIndex());

        MoveResult r2 = MoveEngine.apply(r1.stateAfter(), 0, 0, 1, Move.ROTATE_CW);
        assertTrue(r2.accepted());
        assertEquals(0, r2.stateAfter().turnIndex(), "Turn should wrap back to player 0");
    }

    @Test
    void movesUsedIncrementsOnAcceptedMove() {
        GameState s = BoardFactory.newGame(4L, 3, 2, 5);
        MoveResult r = MoveEngine.apply(s, 0, 0, 1, Move.SMASH);
        assertEquals(s.movesUsed() + 1, r.stateAfter().movesUsed());
    }

    @Test
    void movesUsedUnchangedOnRejectedMove() {
        GameState s = BoardFactory.newGame(4L, 3, 1, 5);
        MoveResult r = MoveEngine.apply(s, 0, 0, 0, Move.SMASH); // top-level reject
        assertEquals(s.movesUsed(), r.stateAfter().movesUsed());
    }
}