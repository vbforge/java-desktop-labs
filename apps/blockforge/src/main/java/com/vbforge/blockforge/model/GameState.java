package com.vbforge.blockforge.model;

import java.util.List;

/**
 * Immutable-by-convention snapshot of the game state.
 *
 * <p>The {@code board} reference is treated as read-only by all callers. The only code that
 * mutates a {@link Block} is {@code engine.MoveEngine}, which always deep-copies the board first.
 * This makes it safe to keep {@code GameState} instances in undo/redo stacks.
 *
 * <p>Scores are not stored; they are computed on demand from {@code board} + {@code goals}.
 * This avoids staleness after undo/redo or manual board changes.
 */
public record GameState(
        long seed,
        long rngState,
        int maxDepth,
        int playerCount,
        int maxTurns,
        int turnIndex,
        int movesUsed,
        List<Goal> goals,
        Block board
) {
    private static final long RNG_MASK = (1L << 48) - 1;

    public GameState {
        if ((rngState & ~RNG_MASK) != 0) {
            throw new IllegalArgumentException(
                    "rngState must be a 48-bit value, got 0x" + Long.toHexString(rngState));
        }
        if (maxDepth < 1 || maxDepth > 8) {
            throw new IllegalArgumentException("maxDepth must be in [1, 8], got " + maxDepth);
        }
        if (playerCount < 1 || playerCount > 2) {
            throw new IllegalArgumentException("playerCount must be 1 or 2, got " + playerCount);
        }
        if (maxTurns < 1) {
            throw new IllegalArgumentException("maxTurns must be >= 1, got " + maxTurns);
        }
        if (turnIndex < 0 || turnIndex >= playerCount) {
            throw new IllegalArgumentException(
                    "turnIndex must be in [0, " + playerCount + "), got " + turnIndex);
        }
        if (movesUsed < 0) {
            throw new IllegalArgumentException("movesUsed must be >= 0, got " + movesUsed);
        }
        if (goals == null || goals.size() != playerCount) {
            throw new IllegalArgumentException(
                    "goals.size() must equal playerCount, got "
                            + (goals == null ? "null" : goals.size()));
        }
        if (board == null) {
            throw new IllegalArgumentException("board must not be null");
        }
        if (board.level() != 0) {
            throw new IllegalArgumentException("board.level() must be 0, got " + board.level());
        }
        if (board.maxDepth() != maxDepth) {
            throw new IllegalArgumentException(
                    "board.maxDepth() (" + board.maxDepth() + ") must equal maxDepth (" + maxDepth + ")");
        }
        goals = List.copyOf(goals);
    }

    /** Scores for all players, computed from the current board and each player's goal. */
    public int[] scores() {
        int[] s = new int[playerCount];
        for (int i = 0; i < playerCount; i++) {
            s[i] = goals.get(i).score(board);
        }
        return s;
    }

    /** True when the allotted turns have been used. */
    public boolean isGameOver() {
        return movesUsed >= maxTurns * playerCount;
    }
}