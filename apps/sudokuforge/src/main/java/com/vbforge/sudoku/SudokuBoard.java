package com.vbforge.sudoku;

public class SudokuBoard {
    public static final int SIZE = 9;
    private final int[][] grid;
    private final boolean[][] fixed;

    public SudokuBoard() {
        this.grid = new int[SIZE][SIZE];
        this.fixed = new boolean[SIZE][SIZE];
    }

    public int get(int row, int col) { return grid[row][col]; }
    public void set(int row, int col, int value) {
        if (!fixed[row][col]) grid[row][col] = value;
    }
    public boolean isFixed(int row, int col) { return fixed[row][col]; }

    public int[][] toArray() {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) {
            copy[r] = grid[r].clone();
        }
        return copy;
    }

    public void loadFrom(int[][] source) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                grid[r][c] = source[r][c];
                fixed[r][c] = source[r][c] != 0;
            }
        }
    }
    public void clearUserEntries() {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (!fixed[r][c]) grid[r][c] = 0;
            }
        }
    }
}