package com.vbforge.taskflow;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Standard undo/redo stack.
 *
 * execute() runs a command and pushes it. undo() pops from the undo stack,
 * reverses the command, and pushes to the redo stack. Any new execute()
 * clears the redo stack — the classic "you can't redo after editing" rule.
 */
public final class CommandStack {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();

    private Runnable onChanged = () -> {};

    public void setOnChanged(Runnable r) {
        this.onChanged = r;
    }

    public void execute(Command c) {
        c.execute();
        undoStack.push(c);
        redoStack.clear();
        onChanged.run();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }
    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void undo() {
        if (undoStack.isEmpty()) return;
        Command c = undoStack.pop();
        c.undo();
        redoStack.push(c);
        onChanged.run();
    }

    public void redo() {
        if (redoStack.isEmpty()) return;
        Command c = redoStack.pop();
        c.execute();
        undoStack.push(c);
        onChanged.run();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
        onChanged.run();
    }
}