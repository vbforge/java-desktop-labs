package com.vbforge.sudoku;

public interface SolverListener {
    void onTry(int row, int col, int num);
    void onPlace(int row, int col, int num);
    void onBacktrack(int row, int col);
    void onSolved(int[][] board);
}