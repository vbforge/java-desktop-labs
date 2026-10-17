public class ChessFigures {
    private static final int ROW = 8; // Number of rows
    private static final int COL = 8; // Number of columns

    public static void main(String[] args) {
        String[][] board = chessFigures(); // Initialize the chessboard with figures
        printBoard(board); // Print the chessboard

        System.out.println();

        // Test moving a white pawn from (6, 1) to (4, 1) and (6, 1) to (5, 1)
        movePawn(board, 6, 1, 4, 1);  // Valid two-step move
        printBoard(board); // Print after the move

        System.out.println();

        movePawn(board, 5, 1, 5, 2);  // Valid single-step move to (5, 2)
        printBoard(board); // Print after the move

        System.out.println();

        movePawn(board, 5, 2, 4, 3);  // Valid capture move diagonally
        printBoard(board); // Print after the move
    }

    private static String[][] chessFigures() {
        String[][] figures = new String[ROW][COL];

        // Place black pieces
        figures[0][0] = "♖"; // Black Rook
        figures[0][1] = "♘"; // Black Knight
        figures[0][2] = "♗"; // Black Bishop
        figures[0][3] = "♔"; // Black King
        figures[0][4] = "♕"; // Black Queen
        figures[0][5] = "♗"; // Black Bishop
        figures[0][6] = "♘"; // Black Knight
        figures[0][7] = "♖"; // Black Rook
        for (int i = 0; i < COL; i++) {
            figures[1][i] = "♟"; // Black Pawns
        }

        // Place white pieces
        figures[7][0] = "♖"; // White Rook
        figures[7][1] = "♘"; // White Knight
        figures[7][2] = "♗"; // White Bishop
        figures[7][3] = "♔"; // White King
        figures[7][4] = "♕"; // White Queen
        figures[7][5] = "♗"; // White Bishop
        figures[7][6] = "♘"; // White Knight
        figures[7][7] = "♖"; // White Rook
        for (int i = 0; i < COL; i++) {
            figures[6][i] = "♙"; // White Pawns
        }

        // Initialize empty squares for the rest of the board
        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < COL; j++) {
                figures[i][j] = " "; // Empty square
            }
        }

        return figures; // Return the populated chessboard
    }

    private static void printBoard(String[][] strings) {
        for (int i = 0; i < ROW; i++) {
            for (int j = 0; j < COL; j++) {
                System.out.print(strings[i][j] + " "); // Print each square
            }
            System.out.println(); // Newline after each row
        }
    }

    private static boolean movePawn(String[][] board, int fromRow, int fromCol, int toRow, int toCol) {
        String piece = board[fromRow][fromCol];

        // Check if the piece is a white pawn
        if (piece.equals("♙")) {
            // Valid move check for two squares forward (initial position)
            if (fromRow == 6 && toRow == 4 && fromCol == toCol && board[toRow][toCol].equals(" ")) {
                // Move the pawn
                board[toRow][toCol] = piece;
                board[fromRow][fromCol] = " "; // Empty the previous position
                return true; // Move successful
            }
            // Valid move check for one square forward
            else if (toRow == fromRow - 1 && fromCol == toCol && board[toRow][toCol].equals(" ")) {
                // Move the pawn
                board[toRow][toCol] = piece;
                board[fromRow][fromCol] = " "; // Empty the previous position
                return true; // Move successful
            }
            // Valid move check for diagonal capture
            else if (toRow == fromRow - 1 && (toCol == fromCol - 1 || toCol == fromCol + 1)) {
                // Check if the target square has a piece of a different color (capturing)
                if (board[toRow][toCol].equals("♟")) { // Capturing black pawn
                    // Move the pawn
                    board[toRow][toCol] = piece;
                    board[fromRow][fromCol] = " "; // Empty the previous position
                    return true; // Move successful
                }
            }
        }

        // If none of the conditions for a valid move are satisfied
        System.out.println("Invalid move for Pawn.");
        return false; // Move unsuccessful
    }
}
