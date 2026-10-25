package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.BlockColor;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Move;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GameSessionTest {

    @Test
    void undoRestoresPreviousBoardAndRng() {
        GameState initial = BoardFactory.newGame(4L, 3, 2, 5);
        BlockColor[][] before = initial.board().flatten();
        long rngBefore = initial.rngState();

        GameSession session = new GameSession(initial);
        assertTrue(session.applyMove(0, 0, 1, Move.SMASH));

        assertTrue(session.undo());
        assertArrayEquals(before, session.state().board().flatten());
        assertEquals(rngBefore, session.state().rngState());
        assertEquals(0, session.state().movesUsed());
        assertEquals(0, session.state().turnIndex());
    }

    @Test
    void redoRestoresNextBoardAndRng() {
        GameSession session = new GameSession(BoardFactory.newGame(4L, 3, 2, 5));
        assertTrue(session.applyMove(0, 0, 1, Move.SMASH));
        BlockColor[][] after = session.state().board().flatten();
        long rngAfter = session.state().rngState();
        int movesAfter = session.state().movesUsed();

        assertTrue(session.undo());
        assertTrue(session.redo());
        assertArrayEquals(after, session.state().board().flatten());
        assertEquals(rngAfter, session.state().rngState());
        assertEquals(movesAfter, session.state().movesUsed());
    }

    @Test
    void newMoveClearsRedoStack() {
        GameSession session = new GameSession(BoardFactory.newGame(4L, 3, 2, 5));
        assertTrue(session.applyMove(0, 0, 1, Move.SMASH));
        assertTrue(session.undo());
        assertTrue(session.canRedo());

        assertTrue(session.applyMove(0, 0, 1, Move.SMASH));
        assertFalse(session.canRedo(), "A new move must clear the redo stack");
    }

    @Test
    void rejectedMoveDoesNotTouchStacks() {
        GameSession session = new GameSession(BoardFactory.newGame(4L, 3, 1, 5));
        assertFalse(session.canUndo());
        assertFalse(session.canRedo());

        // Top-level smash is always rejected.
        assertFalse(session.applyMove(0, 0, 0, Move.SMASH));
        assertFalse(session.canUndo());
        assertFalse(session.canRedo());
    }

    @Test
    void roundTripOfTwentySmashesUndoesCleanly() {
        GameState initial = BoardFactory.newGame(4L, 3, 2, 5);
        BlockColor[][] initialGrid = initial.board().flatten();
        long initialRng = initial.rngState();

        GameSession session = new GameSession(initial);
        for (int i = 0; i < 20; i++) {
            assertTrue(session.applyMove(0, 0, 1, Move.SMASH), "move " + i);
        }
        for (int i = 0; i < 20; i++) {
            assertTrue(session.undo(), "undo " + i);
        }

        assertArrayEquals(initialGrid, session.state().board().flatten());
        assertEquals(initialRng, session.state().rngState());
        assertFalse(session.canUndo());
        assertTrue(session.canRedo());
    }
}