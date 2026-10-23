package com.vbforge.sudoku;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

//@Disabled
class PuzzleGeneratorTest {

    @Test
    void generatesCorrectNumberOfClues() {
        int[][] easy = new PuzzleGenerator().generatePuzzle(Difficulty.EASY);
        int clues = 0;
        for (int[] row : easy) for (int val : row) if (val != 0) clues++;
        
        assertEquals(81 - Difficulty.EASY.getCellsToRemove(), clues);
    }

    @Test
    void generatedPuzzleIsSolvable() {
        int[][] puzzle = new PuzzleGenerator().generatePuzzle(Difficulty.MEDIUM);
        int[][] copy = new int[9][9];
        for (int r = 0; r < 9; r++) copy[r] = puzzle[r].clone();
        
        assertTrue(new SudokuSolver(null).solve(copy));
    }
}