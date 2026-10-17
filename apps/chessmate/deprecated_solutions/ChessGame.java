import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

/**
 * Enhanced interactive chess game with full rules including check, checkmate, castling, and en passant
 */
public class ChessGame {
    private static final int SIZE = 8;
    private String[][] board;
    private boolean whiteTurn = true;
    private Scanner scanner;
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteRookKingsideMoved = false;
    private boolean whiteRookQueensideMoved = false;
    private boolean blackRookKingsideMoved = false;
    private boolean blackRookQueensideMoved = false;
    private int[] enPassantTarget = null; // [row, col] of square where en passant is possible
    private List<String> moveHistory = new ArrayList<>();

    public ChessGame() {
        this.board = initializeBoard();
        this.scanner = new Scanner(System.in);
    }

    public static void main(String[] args) {
        ChessGame game = new ChessGame();
        game.play();
    }

    private void play() {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  ♔♕  ENHANCED CHESS GAME  ♛♚          ║");
        System.out.println("╚════════════════════════════════════════╝");
        System.out.println("\nCommands:");
        System.out.println("  • move e2 e4    - Move piece");
        System.out.println("  • castle king   - Castle kingside");
        System.out.println("  • castle queen  - Castle queenside");
        System.out.println("  • history       - Show move history");
        System.out.println("  • quit          - Exit game\n");

        while (true) {
            printBoard();

            // Check for check or checkmate
            if (isInCheck(whiteTurn)) {
                if (isCheckmate(whiteTurn)) {
                    System.out.println("\n╔════════════════════════════════════════╗");
                    System.out.println("║          CHECKMATE!                    ║");
                    System.out.println("║  " + (whiteTurn ? "Black" : "White") + " wins!                          ║");
                    System.out.println("╚════════════════════════════════════════╝");
                    break;
                } else {
                    System.out.println("\n⚠️  CHECK! ⚠️");
                }
            }

            System.out.println("\n" + (whiteTurn ? "⚪ White" : "⚫ Black") + "'s turn");
            System.out.print("➤ ");

            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("quit")) {
                System.out.println("\n👋 Thanks for playing!");
                break;
            }

            if (input.equals("history")) {
                showHistory();
                continue;
            }

            if (input.startsWith("castle ")) {
                String side = input.substring(7).trim();
                if (performCastling(side)) {
                    whiteTurn = !whiteTurn;
                    enPassantTarget = null;
                }
                continue;
            }

            if (input.startsWith("move ")) {
                String[] parts = input.split(" ");
                if (parts.length == 3) {
                    String from = parts[1];
                    String to = parts[2];
                    if (processMove(from, to)) {
                        whiteTurn = !whiteTurn;
                    }
                } else {
                    System.out.println("❌ Invalid format. Use: move e2 e4");
                }
            } else {
                System.out.println("❌ Unknown command.");
            }
        }
    }

    private void showHistory() {
        System.out.println("\n═══════ Move History ═══════");
        if (moveHistory.isEmpty()) {
            System.out.println("No moves yet.");
        } else {
            for (int i = 0; i < moveHistory.size(); i++) {
                if (i % 2 == 0) {
                    System.out.print((i / 2 + 1) + ". " + moveHistory.get(i));
                    if (i + 1 < moveHistory.size()) {
                        System.out.println(" " + moveHistory.get(i + 1));
                    } else {
                        System.out.println();
                    }
                }
            }
        }
        System.out.println("═══════════════════════════\n");
    }

    private boolean performCastling(String side) {
        if (isInCheck(whiteTurn)) {
            System.out.println("❌ Cannot castle while in check!");
            return false;
        }

        if (whiteTurn) {
            if (whiteKingMoved) {
                System.out.println("❌ King has already moved!");
                return false;
            }
            if (side.equals("king") || side.equals("kingside")) {
                if (whiteRookKingsideMoved) {
                    System.out.println("❌ Kingside rook has already moved!");
                    return false;
                }
                if (!board[7][5].equals(" ") || !board[7][6].equals(" ")) {
                    System.out.println("❌ Squares between king and rook must be empty!");
                    return false;
                }
                if (isSquareUnderAttack(7, 5, false) || isSquareUnderAttack(7, 6, false)) {
                    System.out.println("❌ Cannot castle through or into check!");
                    return false;
                }
                board[7][6] = "♔";
                board[7][5] = "♖";
                board[7][4] = " ";
                board[7][7] = " ";
                whiteKingMoved = true;
                moveHistory.add("O-O");
                System.out.println("✓ Castled kingside!");
                return true;
            } else if (side.equals("queen") || side.equals("queenside")) {
                if (whiteRookQueensideMoved) {
                    System.out.println("❌ Queenside rook has already moved!");
                    return false;
                }
                if (!board[7][1].equals(" ") || !board[7][2].equals(" ") || !board[7][3].equals(" ")) {
                    System.out.println("❌ Squares between king and rook must be empty!");
                    return false;
                }
                if (isSquareUnderAttack(7, 2, false) || isSquareUnderAttack(7, 3, false)) {
                    System.out.println("❌ Cannot castle through or into check!");
                    return false;
                }
                board[7][2] = "♔";
                board[7][3] = "♖";
                board[7][4] = " ";
                board[7][0] = " ";
                whiteKingMoved = true;
                moveHistory.add("O-O-O");
                System.out.println("✓ Castled queenside!");
                return true;
            }
        } else {
            if (blackKingMoved) {
                System.out.println("❌ King has already moved!");
                return false;
            }
            if (side.equals("king") || side.equals("kingside")) {
                if (blackRookKingsideMoved) {
                    System.out.println("❌ Kingside rook has already moved!");
                    return false;
                }
                if (!board[0][5].equals(" ") || !board[0][6].equals(" ")) {
                    System.out.println("❌ Squares between king and rook must be empty!");
                    return false;
                }
                if (isSquareUnderAttack(0, 5, true) || isSquareUnderAttack(0, 6, true)) {
                    System.out.println("❌ Cannot castle through or into check!");
                    return false;
                }
                board[0][6] = "♚";
                board[0][5] = "♜";
                board[0][4] = " ";
                board[0][7] = " ";
                blackKingMoved = true;
                moveHistory.add("O-O");
                System.out.println("✓ Castled kingside!");
                return true;
            } else if (side.equals("queen") || side.equals("queenside")) {
                if (blackRookQueensideMoved) {
                    System.out.println("❌ Queenside rook has already moved!");
                    return false;
                }
                if (!board[0][1].equals(" ") || !board[0][2].equals(" ") || !board[0][3].equals(" ")) {
                    System.out.println("❌ Squares between king and rook must be empty!");
                    return false;
                }
                if (isSquareUnderAttack(0, 2, true) || isSquareUnderAttack(0, 3, true)) {
                    System.out.println("❌ Cannot castle through or into check!");
                    return false;
                }
                board[0][2] = "♚";
                board[0][3] = "♜";
                board[0][4] = " ";
                board[0][0] = " ";
                blackKingMoved = true;
                moveHistory.add("O-O-O");
                System.out.println("✓ Castled queenside!");
                return true;
            }
        }
        System.out.println("❌ Invalid castling side. Use 'king' or 'queen'");
        return false;
    }

    private boolean isInCheck(boolean white) {
        int[] kingPos = findKing(white);
        if (kingPos == null) return false;
        return isSquareUnderAttack(kingPos[0], kingPos[1], white);
    }

    private boolean isCheckmate(boolean white) {
        if (!isInCheck(white)) return false;

        // Try all possible moves for all pieces
        for (int fromRow = 0; fromRow < SIZE; fromRow++) {
            for (int fromCol = 0; fromCol < SIZE; fromCol++) {
                String piece = board[fromRow][fromCol];
                if (!piece.equals(" ") && isPieceOfColor(piece, white)) {
                    for (int toRow = 0; toRow < SIZE; toRow++) {
                        for (int toCol = 0; toCol < SIZE; toCol++) {
                            if (isValidMove(piece, new int[]{fromRow, fromCol}, new int[]{toRow, toCol})) {
                                // Simulate move
                                String capturedPiece = board[toRow][toCol];
                                board[toRow][toCol] = piece;
                                board[fromRow][fromCol] = " ";

                                boolean stillInCheck = isInCheck(white);

                                // Undo move
                                board[fromRow][fromCol] = piece;
                                board[toRow][toCol] = capturedPiece;

                                if (!stillInCheck) {
                                    return false; // Found a move that escapes check
                                }
                            }
                        }
                    }
                }
            }
        }
        return true; // No move can escape check
    }

    private int[] findKing(boolean white) {
        String king = white ? "♔" : "♚";
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j].equals(king)) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    private boolean isSquareUnderAttack(int row, int col, boolean byWhite) {
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                String piece = board[i][j];
                if (!piece.equals(" ") && isPieceOfColor(piece, !byWhite)) {
                    if (canAttackSquare(piece, new int[]{i, j}, new int[]{row, col})) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canAttackSquare(String piece, int[] from, int[] to) {
        // Similar to isValidMove but doesn't check for leaving king in check
        int fromRow = from[0], fromCol = from[1];
        int toRow = to[0], toCol = to[1];

        if (piece.equals("♙")) {
            return toRow == fromRow - 1 && Math.abs(toCol - fromCol) == 1;
        } else if (piece.equals("♟")) {
            return toRow == fromRow + 1 && Math.abs(toCol - fromCol) == 1;
        } else if (piece.equals("♖") || piece.equals("♜")) {
            return isValidRookMove(fromRow, fromCol, toRow, toCol);
        } else if (piece.equals("♘") || piece.equals("♞")) {
            return isValidKnightMove(fromRow, fromCol, toRow, toCol);
        } else if (piece.equals("♗") || piece.equals("♝")) {
            return isValidBishopMove(fromRow, fromCol, toRow, toCol);
        } else if (piece.equals("♕") || piece.equals("♛")) {
            return isValidRookMove(fromRow, fromCol, toRow, toCol) ||
                    isValidBishopMove(fromRow, fromCol, toRow, toCol);
        } else if (piece.equals("♔") || piece.equals("♚")) {
            return Math.abs(toRow - fromRow) <= 1 && Math.abs(toCol - fromCol) <= 1;
        }
        return false;
    }

    private boolean processMove(String from, String to) {
        int[] fromPos = algebraicToPosition(from);
        int[] toPos = algebraicToPosition(to);

        if (fromPos == null || toPos == null) {
            System.out.println("❌ Invalid position format!");
            return false;
        }

        String piece = board[fromPos[0]][fromPos[1]];

        if (piece.equals(" ")) {
            System.out.println("❌ No piece at that position!");
            return false;
        }

        if (!isPieceOfCurrentPlayer(piece)) {
            System.out.println("❌ That's not your piece!");
            return false;
        }

        if (!isValidMove(piece, fromPos, toPos)) {
            System.out.println("❌ Invalid move for this piece!");
            return false;
        }

        // Check if move leaves king in check
        String capturedPiece = board[toPos[0]][toPos[1]];
        board[toPos[0]][toPos[1]] = piece;
        board[fromPos[0]][fromPos[1]] = " ";

        if (isInCheck(whiteTurn)) {
            board[fromPos[0]][fromPos[1]] = piece;
            board[toPos[0]][toPos[1]] = capturedPiece;
            System.out.println("❌ That move would leave your king in check!");
            return false;
        }

        // Update castling rights
        if (piece.equals("♔")) whiteKingMoved = true;
        if (piece.equals("♚")) blackKingMoved = true;
        if (piece.equals("♖") && fromPos[0] == 7 && fromPos[1] == 0) whiteRookQueensideMoved = true;
        if (piece.equals("♖") && fromPos[0] == 7 && fromPos[1] == 7) whiteRookKingsideMoved = true;
        if (piece.equals("♜") && fromPos[0] == 0 && fromPos[1] == 0) blackRookQueensideMoved = true;
        if (piece.equals("♜") && fromPos[0] == 0 && fromPos[1] == 7) blackRookKingsideMoved = true;

        // Handle pawn promotion
        if ((piece.equals("♙") && toPos[0] == 0) || (piece.equals("♟") && toPos[0] == 7)) {
            board[toPos[0]][toPos[1]] = piece.equals("♙") ? "♕" : "♛";
            System.out.println("✓ Pawn promoted to Queen!");
        }

        // Record move in history
        String moveNotation = from + "-" + to;
        if (!capturedPiece.equals(" ")) {
            moveNotation += " (captures)";
        }
        moveHistory.add(moveNotation);

        // Handle en passant target
        if (piece.equals("♙") && fromPos[0] == 6 && toPos[0] == 4) {
            enPassantTarget = new int[]{5, fromPos[1]};
        } else if (piece.equals("♟") && fromPos[0] == 1 && toPos[0] == 3) {
            enPassantTarget = new int[]{2, fromPos[1]};
        } else {
            enPassantTarget = null;
        }

        System.out.println("✓ Move executed!" + (!capturedPiece.equals(" ") ? " (captured " + capturedPiece + ")" : ""));
        return true;
    }

    private boolean isPieceOfCurrentPlayer(String piece) {
        return isPieceOfColor(piece, whiteTurn);
    }

    private boolean isPieceOfColor(String piece, boolean white) {
        if (white) {
            return piece.equals("♙") || piece.equals("♖") || piece.equals("♘") ||
                    piece.equals("♗") || piece.equals("♕") || piece.equals("♔");
        } else {
            return piece.equals("♟") || piece.equals("♜") || piece.equals("♞") ||
                    piece.equals("♝") || piece.equals("♛") || piece.equals("♚");
        }
    }

    private boolean isValidMove(String piece, int[] from, int[] to) {
        int fromRow = from[0], fromCol = from[1];
        int toRow = to[0], toCol = to[1];

        // Can't capture own pieces
        String targetPiece = board[toRow][toCol];
        if (!targetPiece.equals(" ") && isPieceOfColor(targetPiece, whiteTurn)) {
            return false;
        }

        // Pawns
        if (piece.equals("♙")) {
            return isValidWhitePawnMove(fromRow, fromCol, toRow, toCol);
        } else if (piece.equals("♟")) {
            return isValidBlackPawnMove(fromRow, fromCol, toRow, toCol);
        }

        // Rooks
        if (piece.equals("♖") || piece.equals("♜")) {
            return isValidRookMove(fromRow, fromCol, toRow, toCol);
        }

        // Knights
        if (piece.equals("♘") || piece.equals("♞")) {
            return isValidKnightMove(fromRow, fromCol, toRow, toCol);
        }

        // Bishops
        if (piece.equals("♗") || piece.equals("♝")) {
            return isValidBishopMove(fromRow, fromCol, toRow, toCol);
        }

        // Queens
        if (piece.equals("♕") || piece.equals("♛")) {
            return isValidRookMove(fromRow, fromCol, toRow, toCol) ||
                    isValidBishopMove(fromRow, fromCol, toRow, toCol);
        }

        // Kings
        if (piece.equals("♔") || piece.equals("♚")) {
            return isValidKingMove(fromRow, fromCol, toRow, toCol);
        }

        return false;
    }

    private boolean isValidWhitePawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        // One square forward
        if (toRow == fromRow - 1 && fromCol == toCol && board[toRow][toCol].equals(" ")) {
            return true;
        }
        // Two squares forward from initial position
        if (fromRow == 6 && toRow == 4 && fromCol == toCol &&
                board[toRow][toCol].equals(" ") && board[5][toCol].equals(" ")) {
            return true;
        }
        // Diagonal capture
        if (toRow == fromRow - 1 && Math.abs(toCol - fromCol) == 1) {
            String target = board[toRow][toCol];
            if (!target.equals(" ") && isPieceOfColor(target, false)) {
                return true;
            }
            // En passant
            if (enPassantTarget != null && toRow == enPassantTarget[0] && toCol == enPassantTarget[1]) {
                board[fromRow][toCol] = " "; // Remove captured pawn
                return true;
            }
        }
        return false;
    }

    private boolean isValidBlackPawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        // One square forward
        if (toRow == fromRow + 1 && fromCol == toCol && board[toRow][toCol].equals(" ")) {
            return true;
        }
        // Two squares forward from initial position
        if (fromRow == 1 && toRow == 3 && fromCol == toCol &&
                board[toRow][toCol].equals(" ") && board[2][toCol].equals(" ")) {
            return true;
        }
        // Diagonal capture
        if (toRow == fromRow + 1 && Math.abs(toCol - fromCol) == 1) {
            String target = board[toRow][toCol];
            if (!target.equals(" ") && isPieceOfColor(target, true)) {
                return true;
            }
            // En passant
            if (enPassantTarget != null && toRow == enPassantTarget[0] && toCol == enPassantTarget[1]) {
                board[fromRow][toCol] = " "; // Remove captured pawn
                return true;
            }
        }
        return false;
    }

    private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) return false;
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (Math.abs(toRow - fromRow) != Math.abs(toCol - fromCol)) return false;
        return isPathClear(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidKingMove(int fromRow, int fromCol, int toRow, int toCol) {
        return Math.abs(toRow - fromRow) <= 1 && Math.abs(toCol - fromCol) <= 1;
    }

    private boolean isPathClear(int fromRow, int fromCol, int toRow, int toCol) {
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);
        int row = fromRow + rowStep;
        int col = fromCol + colStep;

        while (row != toRow || col != toCol) {
            if (!board[row][col].equals(" ")) return false;
            row += rowStep;
            col += colStep;
        }

        return true;
    }

    private int[] algebraicToPosition(String algebraic) {
        if (algebraic.length() != 2) return null;
        char file = algebraic.charAt(0);
        char rank = algebraic.charAt(1);

        if (file < 'a' || file > 'h' || rank < '1' || rank > '8') {
            return null;
        }

        int col = file - 'a';
        int row = 8 - (rank - '0');
        return new int[]{row, col};
    }

    private String[][] initializeBoard() {
        String[][] board = new String[SIZE][SIZE];

        // Black pieces
        board[0][0] = "♜"; board[0][1] = "♞"; board[0][2] = "♝"; board[0][3] = "♛";
        board[0][4] = "♚"; board[0][5] = "♝"; board[0][6] = "♞"; board[0][7] = "♜";
        for (int i = 0; i < SIZE; i++) board[1][i] = "♟";

        // White pieces
        board[7][0] = "♖"; board[7][1] = "♘"; board[7][2] = "♗"; board[7][3] = "♕";
        board[7][4] = "♔"; board[7][5] = "♗"; board[7][6] = "♘"; board[7][7] = "♖";
        for (int i = 0; i < SIZE; i++) board[6][i] = "♙";

        // Empty squares
        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < SIZE; j++) {
                board[i][j] = " ";
            }
        }

        return board;
    }

    private void printBoard() {
        System.out.println("\n   a  b  c  d  e  f  g  h");

        for (int i = 0; i < SIZE; i++) {
            System.out.print((8 - i) + " ");
            for (int j = 0; j < SIZE; j++) {
                String piece = board[i][j];
                if (piece.equals(" ")) {
                    // Checkerboard pattern
                    if ((i + j) % 2 == 0) {
                        System.out.print(" · ");
                    } else {
                        System.out.print("   ");
                    }
                } else {
                    System.out.print(" " + piece + " ");
                }
            }
            System.out.println();
        }
    }
}