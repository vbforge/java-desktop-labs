package com.vbforge.taskflow;

/**
 * A reversible mutation. Every change to the board is a Command, so the
 * undo stack is just a stack of Commands — no special-casing per operation.
 */
public interface Command {
    void execute();
    void undo();
    /** Shown in the toolbar. Null means "don't announce". */
    default String label() { return null; }
}