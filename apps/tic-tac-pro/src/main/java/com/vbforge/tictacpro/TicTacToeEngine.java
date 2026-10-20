package com.vbforge.tictacpro;

import java.util.ArrayList;
import java.util.List;

public class TicTacToeEngine {

    public static final String EMPTY = "";
    public static final String X = "X";
    public static final String O = "O";

    private final String[] board = new String[9];

    public TicTacToeEngine() {
        reset();
    }

    public void reset() {
        for (int i = 0; i < 9; i++) {
            board[i] = EMPTY;
        }
    }

    public String getMark(int index) {
        return board[index];
    }

    public boolean makeMove(int index, String mark) {
        if (index < 0 || index > 8 || !board[index].equals(EMPTY)) {
            return false;
        }
        board[index] = mark;
        return true;
    }

    public String checkWinner() {
        int[][] lines = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // Rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // Cols
            {0, 4, 8}, {2, 4, 6}             // Diags
        };

        for (int[] line : lines) {
            if (!board[line[0]].equals(EMPTY) &&
                board[line[0]].equals(board[line[1]]) &&
                board[line[0]].equals(board[line[2]])) {
                return board[line[0]];
            }
        }
        return null;
    }

    public boolean isDraw() {
        for (String cell : board) {
            if (cell.equals(EMPTY)) return false;
        }
        return checkWinner() == null;
    }

    public List<Integer> getEmptyIndices() {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i].equals(EMPTY)) empty.add(i);
        }
        return empty;
    }

    // --- MINIMAX AI ---

    public int getBestMove(String aiMark, String humanMark) {
        int bestScore = Integer.MIN_VALUE;
        int bestMove = -1;

        for (int i = 0; i < 9; i++) {
            if (board[i].equals(EMPTY)) {
                board[i] = aiMark;
                int score = minimax(0, false, aiMark, humanMark);
                board[i] = EMPTY;

                if (score > bestScore) {
                    bestScore = score;
                    bestMove = i;
                }
            }
        }
        return bestMove;
    }

    private int minimax(int depth, boolean isMaximizing, String aiMark, String humanMark) {
        String winner = checkWinner();
        if (winner != null) {
            if (winner.equals(aiMark)) return 10 - depth;
            if (winner.equals(humanMark)) return depth - 10;
        }
        if (isDraw()) return 0;

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i].equals(EMPTY)) {
                    board[i] = aiMark;
                    int score = minimax(depth + 1, false, aiMark, humanMark);
                    board[i] = EMPTY;
                    bestScore = Math.max(score, bestScore);
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 9; i++) {
                if (board[i].equals(EMPTY)) {
                    board[i] = humanMark;
                    int score = minimax(depth + 1, true, aiMark, humanMark);
                    board[i] = EMPTY;
                    bestScore = Math.min(score, bestScore);
                }
            }
            return bestScore;
        }
    }

    public int getRandomMove() {
        List<Integer> empty = getEmptyIndices();
        if (empty.isEmpty()) return -1;
        return empty.get((int) (Math.random() * empty.size()));
    }
}