package com.vbforge.sudoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class PuzzleGenerator {
    private static final int SIZE = 9;
    private final Random random = new Random();

    public int[][] generatePuzzle(Difficulty difficulty) {
        int[][] solved = generateSolvedBoard();
        int[][] puzzle = deepCopy(solved);
        List<int[]> positions = allPositions();
        Collections.shuffle(positions, random);

        int removed = 0;
        for (int[] pos : positions) {
            if (removed >= difficulty.getCellsToRemove()) break;
            int row = pos[0], col = pos[1];
            int backup = puzzle[row][col];
            puzzle[row][col] = 0;
            
            if (countSolutions(deepCopy(puzzle), 2) == 1) {
                removed++;
            } else {
                puzzle[row][col] = backup;
            }
        }
        return puzzle;
    }

    private int[][] generateSolvedBoard() {
        int[][] board = new int[SIZE][SIZE];
        fillBoard(board);
        return board;
    }

    private boolean fillBoard(int[][] board) {
        int[] empty = findEmptyCell(board);
        if (empty == null) return true;
        int row = empty[0], col = empty[1];
        for (int candidate : shuffledDigits()) {
            if (SudokuSolver.isValid(board, row, col, candidate)) {
                board[row][col] = candidate;
                if (fillBoard(board)) return true;
                board[row][col] = 0;
            }
        }
        return false;
    }

    private int countSolutions(int[][] board, int limit) {
        int[] empty = findEmptyCell(board);
        if (empty == null) return 1;
        int row = empty[0], col = empty[1];
        int count = 0;
        for (int candidate = 1; candidate <= SIZE; candidate++) {
            if (SudokuSolver.isValid(board, row, col, candidate)) {
                board[row][col] = candidate;
                count += countSolutions(board, limit - count);
                board[row][col] = 0;
                if (count >= limit) break;
            }
        }
        return count;
    }

    private int[] findEmptyCell(int[][] board) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                if (board[r][c] == 0) return new int[]{r, c};
            }
        }
        return null;
    }

    private List<Integer> shuffledDigits() {
        List<Integer> digits = new ArrayList<>();
        for (int i = 1; i <= SIZE; i++) digits.add(i);
        Collections.shuffle(digits, random);
        return digits;
    }

    private List<int[]> allPositions() {
        List<int[]> positions = new ArrayList<>(SIZE * SIZE);
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) positions.add(new int[]{r, c});
        }
        return positions;
    }

    private int[][] deepCopy(int[][] source) {
        int[][] copy = new int[SIZE][SIZE];
        for (int r = 0; r < SIZE; r++) copy[r] = source[r].clone();
        return copy;
    }
}