package com.vbforge.blockforge.model;

/**
 * A player's objective. Sealed so the set of goals is exhaustively known at compile time
 * (useful for save/load dispatch and any future AI).
 */
public sealed interface Goal permits BlobGoal, PerimeterGoal {

    BlockColor targetColor();

    /** Compute the score of this goal against the given board. */
    int score(Block board);

    /** Human-readable description shown in the UI. */
    String description();
}