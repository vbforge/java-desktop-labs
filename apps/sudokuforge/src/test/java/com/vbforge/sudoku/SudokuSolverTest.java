package com.vbforge.sudoku;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SudokuSolverTest {

    @Test
    void solvesValidPuzzle() {
        int[][] board = {
            {5,3,0, 0,7,0, 0,0,0},
            {6,0,0, 1,9,5, 0,0,0},
            {0,9,8, 0,0,0, 0,6,0},
            {8,0,0, 0,6,0, 0,0,3},
            {4,0,0, 8,0,3, 0,0,1},
            {7,0,0, 0,2,0, 0,0,6},
            {0,6,0, 0,0,0, 2,8,0},
            {0,0,0, 4,1,9, 0,0,5},
            {0,0,0, 0,8,0, 0,7,9}
        };
        assertTrue(new SudokuSolver(null).solve(board));
        assertEquals(5, board[0][0]);
        assertEquals(9, board[8][8]);
    }

    @Test
    void failsOnInvalidPuzzle() {
        // The solver scans row by row, so it finds [0][8] as the first empty cell.
        // It tries numbers 1-8: they fail because they are already in row 0.
        // It tries number 9: it fails because there is already a 9 in column 8 at [1][8].
        // No other empty cells are checked. The solver immediately returns false.
        int[][] board = {
            {1,2,3, 4,5,6, 7,8,0},
            {0,0,0, 0,0,0, 0,0,9},
            {0,0,0, 0,0,0, 0,0,0},
            {0,0,0, 0,0,0, 0,0,0},
            {0,0,0, 0,0,0, 0,0,0},
            {0,0,0, 0,0,0, 0,0,0},
            {0,0,0, 0,0,0, 0,0,0},
            {0,0,0, 0,0,0, 0,0,0},
            {0,0,0, 0,0,0, 0,0,0}
        };
        assertFalse(new SudokuSolver(null).solve(board)); 
    }
}