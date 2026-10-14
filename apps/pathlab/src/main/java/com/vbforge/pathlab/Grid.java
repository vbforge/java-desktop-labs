package com.vbforge.pathlab;

import java.util.Arrays;

/**
 * Mutable state of the grid: which cells are walls, where start and end are,
 * and which cells the current search has touched. Rendering reads from here.
 */
public final class Grid {

    public enum State { EMPTY, WALL, START, END, VISITED, FRONTIER, PATH }

    private final int rows;
    private final int cols;
    private final State[][] cells;

    private int startRow, startCol, endRow, endCol;

    public Grid(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new State[rows][cols];
        reset();
    }

    /** Wipe everything and put start/end back in their default positions. */
    public void reset() {
        for (State[] row : cells) Arrays.fill(row, State.EMPTY);

        startRow = rows / 2;
        startCol = cols / 5;
        endRow = rows / 2;
        endCol = cols - 1 - cols / 5;

        cells[startRow][startCol] = State.START;
        cells[endRow][endCol] = State.END;
    }

    /** Remove VISITED / FRONTIER / PATH marks but keep walls, start and end. */
    public void clearSearch() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                State s = cells[r][c];
                if (s == State.VISITED || s == State.FRONTIER || s == State.PATH) {
                    cells[r][c] = State.EMPTY;
                }
            }
        }
    }

    public int rows() { return rows; }
    public int cols() { return cols; }

    public int startRow() { return startRow; }
    public int startCol() { return startCol; }
    public int endRow()   { return endRow; }
    public int endCol()   { return endCol; }

    public boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    public State get(int r, int c) {
        return cells[r][c];
    }

    public boolean isWall(int r, int c) {
        return inBounds(r, c) && cells[r][c] == State.WALL;
    }

    /** Overwrite a cell's state, but never clobber START or END. */
    public void setState(int r, int c, State s) {
        if (!inBounds(r, c)) return;
        State current = cells[r][c];
        if (current == State.START || current == State.END) return;
        cells[r][c] = s;
    }
}