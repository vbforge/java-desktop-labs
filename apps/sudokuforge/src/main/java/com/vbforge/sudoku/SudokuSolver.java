package com.vbforge.sudoku;

public class SudokuSolver {
    public static final int SIZE = 9;
    private static final int BOX = 3;
    private final SolverListener listener;
    private volatile boolean cancelled = false;

    public SudokuSolver(SolverListener listener) {
        this.listener = listener;
    }

    public void cancel() { cancelled = true; }

    public boolean solve(int[][] board) {
        if (cancelled) return false;
        int[] nextEmpty = findEmptyCell(board);
        if (nextEmpty == null) {
            if (listener != null) listener.onSolved(board);
            return true;
        }

        int row = nextEmpty[0];
        int col = nextEmpty[1];

        for (int candidate = 1; candidate <= SIZE; candidate++) {
            if (cancelled) return false;
            if (listener != null) listener.onTry(row, col, candidate);
            
            if (isValid(board, row, col, candidate)) {
                board[row][col] = candidate;
                if (listener != null) listener.onPlace(row, col, candidate);

                if (solve(board)) return true;

                board[row][col] = 0;
                if (listener != null) listener.onBacktrack(row, col);
            }
        }
        return false;
    }

    private int[] findEmptyCell(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) return new int[]{r, c};
            }
        }
        return null;
    }

    public static boolean isValid(int[][] board, int row, int col, int num) {
        for (int i = 0; i < SIZE; i++) {
            if (board[row][i] == num || board[i][col] == num) return false;
        }
        int boxRowStart = (row / BOX) * BOX;
        int boxColStart = (col / BOX) * BOX;
        for (int r = boxRowStart; r < boxRowStart + BOX; r++) {
            for (int c = boxColStart; c < boxColStart + BOX; c++) {
                if (board[r][c] == num) return false;
            }
        }
        return true;
    }
}