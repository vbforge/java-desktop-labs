package com.vbforge.chessmate;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;

import java.util.List;
import java.util.Random;

public class ChessAI {

    private static final int PAWN_VAL = 100;
    private static final int KNIGHT_VAL = 320;
    private static final int BISHOP_VAL = 330;
    private static final int ROOK_VAL = 500;
    private static final int QUEEN_VAL = 900;

    public static Move calculateBestMove(Board board, boolean isAdvanced) {
        List<Move> legalMoves = board.legalMoves();
        if (legalMoves.isEmpty()) return null;

        if (!isAdvanced) {
            // Easy: random move
            Random rand = new Random();
            return legalMoves.get(rand.nextInt(legalMoves.size()));
        }

        // Advanced: Minimax with Alpha-Beta pruning (Depth 3)
        int depth = 3;
        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;

        for (Move move : legalMoves) {
            board.doMove(move);
            int boardValue = minimax(board, depth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            board.undoMove();

            if (boardValue > bestValue) {
                bestValue = boardValue;
                bestMove = move;
            }
        }
        return bestMove;
    }

    private static int minimax(Board board, int depth, int alpha, int beta, boolean isMaximizing) {
        // Changed isStalemate() to isDraw() which covers stalemate in chesslib
        if (depth == 0 || board.isMated() || board.isDraw()) {
            return evaluateBoard(board);
        }

        List<Move> moves = board.legalMoves();
        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                board.doMove(move);
                int eval = minimax(board, depth - 1, alpha, beta, false);
                board.undoMove();
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) break;
            }
            return maxEval;
        } else {
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                board.doMove(move);
                int eval = minimax(board, depth - 1, alpha, beta, true);
                board.undoMove();
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) break;
            }
            return minEval;
        }
    }

    private static int evaluateBoard(Board board) {
        int score = 0;
        for (Piece piece : Piece.values()) {
            if (piece == Piece.NONE) continue;
            long count = board.getPieceLocation(piece).size();
            int value = getPieceValue(piece);
            if (piece.getPieceSide() == com.github.bhlangonijr.chesslib.Side.WHITE) {
                score += value * count;
            } else {
                score -= value * count;
            }
        }
        return score;
    }

    private static int getPieceValue(Piece piece) {
        return switch (piece.getPieceType()) {
            case PAWN -> PAWN_VAL;
            case KNIGHT -> KNIGHT_VAL;
            case BISHOP -> BISHOP_VAL;
            case ROOK -> ROOK_VAL;
            case QUEEN -> QUEEN_VAL;
            case KING -> 20000;
            default -> 0;
        };
    }
}