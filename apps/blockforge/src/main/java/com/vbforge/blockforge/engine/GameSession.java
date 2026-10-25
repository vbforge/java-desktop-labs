package com.vbforge.blockforge.engine;

import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Move;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds the current {@link GameState} plus undo/redo stacks.
 *
 * <p>Every successful move pushes the pre-move state onto the undo stack, so undo/redo is a full
 * time-travel: both the board <em>and</em> the RNG state are restored. Redo is cleared on any new
 * successful move.
 */
public final class GameSession {

    private GameState current;
    private final Deque<GameState> undoStack = new ArrayDeque<>();
    private final Deque<GameState> redoStack = new ArrayDeque<>();

    public GameSession(GameState initial) {
        if (initial == null) {
            throw new IllegalArgumentException("initial must not be null");
        }
        this.current = initial;
    }

    public GameState state() {
        return current;
    }

    /** @return true if the move was legal and applied; false if it was rejected */
    public boolean applyMove(int unitX, int unitY, int level, Move move) {
        MoveResult result = MoveEngine.apply(current, unitX, unitY, level, move);
        if (!result.accepted()) {
            return false;
        }
        undoStack.push(current);
        redoStack.clear();
        current = result.stateAfter();
        return true;
    }

    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        redoStack.push(current);
        current = undoStack.pop();
        return true;
    }

    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        undoStack.push(current);
        current = redoStack.pop();
        return true;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }
}