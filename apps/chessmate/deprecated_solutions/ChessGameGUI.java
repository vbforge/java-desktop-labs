import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Beautiful Swing GUI Chess Game with drag-and-drop functionality
 */
public class ChessGameGUI extends JFrame {
    private static final int BOARD_SIZE = 8;
    private static final int SQUARE_SIZE = 80;
    private static final Color LIGHT_SQUARE = new Color(240, 217, 181);
    private static final Color DARK_SQUARE = new Color(181, 136, 99);
    private static final Color SELECTED_COLOR = new Color(186, 202, 68);
    private static final Color VALID_MOVE_COLOR = new Color(130, 151, 105, 150);
    private static final Color CHECK_COLOR = new Color(255, 100, 100, 150);

    private String[][] board;
    private boolean whiteTurn = true;
    private ChessSquare selectedSquare = null;
    private JPanel boardPanel;
    private ChessSquare[][] squares;
    private JLabel statusLabel;
    private JTextArea moveHistoryArea;
    private List<String> moveHistory = new ArrayList<>();
    
    // Castling and game state
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean whiteRookKingsideMoved = false;
    private boolean whiteRookQueensideMoved = false;
    private boolean blackRookKingsideMoved = false;
    private boolean blackRookQueensideMoved = false;
    private int[] enPassantTarget = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessGameGUI game = new ChessGameGUI();
            game.setVisible(true);
        });
    }

    public ChessGameGUI() {
        setTitle("♔ Chess Game ♚");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setResizable(false);

        // Initialize board
        board = initializeBoard();

        // Create UI components
        createMenuBar();
        createBoardPanel();
        createSidePanel();
        createStatusBar();

        pack();
        setLocationRelativeTo(null);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        JMenu gameMenu = new JMenu("Game");
        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.addActionListener(e -> resetGame());
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> System.exit(0));
        
        gameMenu.add(newGameItem);
        gameMenu.addSeparator();
        gameMenu.add(exitItem);
        
        JMenu helpMenu = new JMenu("Help");
        JMenuItem rulesItem = new JMenuItem("Rules");
        rulesItem.addActionListener(e -> showRules());
        helpMenu.add(rulesItem);
        
        menuBar.add(gameMenu);
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }

    private void createBoardPanel() {
        boardPanel = new JPanel(new GridLayout(BOARD_SIZE, BOARD_SIZE));
        boardPanel.setPreferredSize(new Dimension(SQUARE_SIZE * BOARD_SIZE, SQUARE_SIZE * BOARD_SIZE));
        boardPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        
        squares = new ChessSquare[BOARD_SIZE][BOARD_SIZE];
        
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                ChessSquare square = new ChessSquare(row, col);
                squares[row][col] = square;
                boardPanel.add(square);
            }
        }
        
        add(boardPanel, BorderLayout.CENTER);
    }

    private void createSidePanel() {
        JPanel sidePanel = new JPanel(new BorderLayout(5, 5));
        sidePanel.setPreferredSize(new Dimension(250, SQUARE_SIZE * BOARD_SIZE));
        sidePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Title
        JLabel titleLabel = new JLabel("Move History", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        sidePanel.add(titleLabel, BorderLayout.NORTH);
        
        // Move history
        moveHistoryArea = new JTextArea();
        moveHistoryArea.setEditable(false);
        moveHistoryArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(moveHistoryArea);
        sidePanel.add(scrollPane, BorderLayout.CENTER);
        
        // Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> resetGame());
        
        JButton castleKingButton = new JButton("Castle Kingside");
        castleKingButton.addActionListener(e -> performCastling(true));
        
        JButton castleQueenButton = new JButton("Castle Queenside");
        castleQueenButton.addActionListener(e -> performCastling(false));
        
        JButton undoButton = new JButton("Undo (Not Implemented)");
        undoButton.setEnabled(false);
        
        buttonPanel.add(newGameButton);
        buttonPanel.add(castleKingButton);
        buttonPanel.add(castleQueenButton);
        buttonPanel.add(undoButton);
        
        sidePanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(sidePanel, BorderLayout.EAST);
    }

    private void createStatusBar() {
        statusLabel = new JLabel("White's turn", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.WHITE);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void resetGame() {
        board = initializeBoard();
        whiteTurn = true;
        selectedSquare = null;
        moveHistory.clear();
        moveHistoryArea.setText("");
        whiteKingMoved = false;
        blackKingMoved = false;
        whiteRookKingsideMoved = false;
        whiteRookQueensideMoved = false;
        blackRookKingsideMoved = false;
        blackRookQueensideMoved = false;
        enPassantTarget = null;
        updateBoard();
        updateStatus("White's turn");
    }

    private void showRules() {
        String rules = "Chess Rules:\n\n" +
                "• Click a piece to select it\n" +
                "• Click a highlighted square to move\n" +
                "• Use buttons for castling\n" +
                "• Pieces move according to chess rules\n" +
                "• Game checks for check and checkmate\n" +
                "• Pawns promote to Queens automatically\n" +
                "• En passant is supported";
        JOptionPane.showMessageDialog(this, rules, "How to Play", JOptionPane.INFORMATION_MESSAGE);
    }

    private void performCastling(boolean kingside) {
        if (isInCheck(whiteTurn)) {
            showMessage("Cannot castle while in check!");
            return;
        }

        if (whiteTurn) {
            if (whiteKingMoved) {
                showMessage("King has already moved!");
                return;
            }
            if (kingside) {
                if (whiteRookKingsideMoved || !board[7][5].equals(" ") || !board[7][6].equals(" ")) {
                    showMessage("Cannot castle kingside!");
                    return;
                }
                if (isSquareUnderAttack(7, 5, false) || isSquareUnderAttack(7, 6, false)) {
                    showMessage("Cannot castle through or into check!");
                    return;
                }
                board[7][6] = "♔";
                board[7][5] = "♖";
                board[7][4] = " ";
                board[7][7] = " ";
                addMoveToHistory("O-O");
            } else {
                if (whiteRookQueensideMoved || !board[7][1].equals(" ") || !board[7][2].equals(" ") || !board[7][3].equals(" ")) {
                    showMessage("Cannot castle queenside!");
                    return;
                }
                if (isSquareUnderAttack(7, 2, false) || isSquareUnderAttack(7, 3, false)) {
                    showMessage("Cannot castle through or into check!");
                    return;
                }
                board[7][2] = "♔";
                board[7][3] = "♖";
                board[7][4] = " ";
                board[7][0] = " ";
                addMoveToHistory("O-O-O");
            }
            whiteKingMoved = true;
        } else {
            if (blackKingMoved) {
                showMessage("King has already moved!");
                return;
            }
            if (kingside) {
                if (blackRookKingsideMoved || !board[0][5].equals(" ") || !board[0][6].equals(" ")) {
                    showMessage("Cannot castle kingside!");
                    return;
                }
                if (isSquareUnderAttack(0, 5, true) || isSquareUnderAttack(0, 6, true)) {
                    showMessage("Cannot castle through or into check!");
                    return;
                }
                board[0][6] = "♚";
                board[0][5] = "♜";
                board[0][4] = " ";
                board[0][7] = " ";
                addMoveToHistory("O-O");
            } else {
                if (blackRookQueensideMoved || !board[0][1].equals(" ") || !board[0][2].equals(" ") || !board[0][3].equals(" ")) {
                    showMessage("Cannot castle queenside!");
                    return;
                }
                if (isSquareUnderAttack(0, 2, true) || isSquareUnderAttack(0, 3, true)) {
                    showMessage("Cannot castle through or into check!");
                    return;
                }
                board[0][2] = "♚";
                board[0][3] = "♜";
                board[0][4] = " ";
                board[0][0] = " ";
                addMoveToHistory("O-O-O");
            }
            blackKingMoved = true;
        }
        
        whiteTurn = !whiteTurn;
        enPassantTarget = null;
        updateBoard();
        checkGameState();
    }

    private class ChessSquare extends JPanel {
        private int row, col;
        private JLabel pieceLabel;

        public ChessSquare(int row, int col) {
            this.row = row;
            this.col = col;
            
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(SQUARE_SIZE, SQUARE_SIZE));
            
            // Set background color
            Color bgColor = (row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
            setBackground(bgColor);
            
            // Piece label
            pieceLabel = new JLabel("", SwingConstants.CENTER);
            pieceLabel.setFont(new Font("Arial Unicode MS", Font.PLAIN, 48));
            add(pieceLabel, BorderLayout.CENTER);
            
            // Add coordinates
            if (row == 7) {
                JLabel colLabel = new JLabel(String.valueOf((char) ('a' + col)), SwingConstants.CENTER);
                colLabel.setFont(new Font("Arial", Font.BOLD, 10));
                add(colLabel, BorderLayout.SOUTH);
            }
            if (col == 0) {
                JLabel rowLabel = new JLabel(String.valueOf(8 - row), SwingConstants.CENTER);
                rowLabel.setFont(new Font("Arial", Font.BOLD, 10));
                add(rowLabel, BorderLayout.WEST);
            }
            
            // Mouse listener
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleSquareClick();
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (selectedSquare == null && !board[row][col].equals(" ") && 
                        isPieceOfColor(board[row][col], whiteTurn)) {
                        setBorder(BorderFactory.createLineBorder(Color.YELLOW, 3));
                    }
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    if (selectedSquare != ChessSquare.this) {
                        setBorder(null);
                    }
                }
            });
            
            updatePiece();
        }

        public void updatePiece() {
            pieceLabel.setText(board[row][col].equals(" ") ? "" : board[row][col]);
        }

        public void setSelected(boolean selected) {
            if (selected) {
                setBackground(SELECTED_COLOR);
                setBorder(BorderFactory.createLineBorder(Color.BLACK, 3));
            } else {
                Color bgColor = (row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                setBackground(bgColor);
                setBorder(null);
            }
        }

        public void setValidMove(boolean valid) {
            if (valid) {
                setBackground(VALID_MOVE_COLOR);
            } else {
                Color bgColor = (row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                setBackground(bgColor);
            }
        }

        public void setCheck(boolean check) {
            if (check) {
                setBackground(CHECK_COLOR);
            }
        }

        private void handleSquareClick() {
            if (selectedSquare == null) {
                // Select piece
                String piece = board[row][col];
                if (!piece.equals(" ") && isPieceOfColor(piece, whiteTurn)) {
                    selectedSquare = this;
                    setSelected(true);
                    highlightValidMoves();
                }
            } else {
                // Try to move
                if (selectedSquare == this) {
                    // Deselect
                    selectedSquare.setSelected(false);
                    selectedSquare = null;
                    updateBoard();
                } else {
                    // Attempt move
                    if (attemptMove(selectedSquare.row, selectedSquare.col, row, col)) {
                        selectedSquare.setSelected(false);
                        selectedSquare = null;
                        whiteTurn = !whiteTurn;
                        updateBoard();
                        checkGameState();
                    } else {
                        showMessage("Invalid move!");
                    }
                }
            }
        }

        private void highlightValidMoves() {
            String piece = board[selectedSquare.row][selectedSquare.col];
            for (int r = 0; r < BOARD_SIZE; r++) {
                for (int c = 0; c < BOARD_SIZE; c++) {
                    if (isValidMoveForPiece(piece, selectedSquare.row, selectedSquare.col, r, c)) {
                        // Check if move leaves king in check
                        String capturedPiece = board[r][c];
                        board[r][c] = piece;
                        board[selectedSquare.row][selectedSquare.col] = " ";
                        
                        if (!isInCheck(whiteTurn)) {
                            squares[r][c].setValidMove(true);
                        }
                        
                        board[selectedSquare.row][selectedSquare.col] = piece;
                        board[r][c] = capturedPiece;
                    }
                }
            }
        }
    }

    private boolean attemptMove(int fromRow, int fromCol, int toRow, int toCol) {
        String piece = board[fromRow][fromCol];
        
        if (!isValidMoveForPiece(piece, fromRow, fromCol, toRow, toCol)) {
            return false;
        }

        // Simulate move
        String capturedPiece = board[toRow][toCol];
        board[toRow][toCol] = piece;
        board[fromRow][fromCol] = " ";

        // Check if move leaves king in check
        if (isInCheck(whiteTurn)) {
            board[fromRow][fromCol] = piece;
            board[toRow][toCol] = capturedPiece;
            showMessage("That move would leave your king in check!");
            return false;
        }

        // Update castling rights
        updateCastlingRights(piece, fromRow, fromCol);

        // Handle pawn promotion
        if ((piece.equals("♙") && toRow == 0) || (piece.equals("♟") && toRow == 7)) {
            board[toRow][toCol] = piece.equals("♙") ? "♕" : "♛";
            showMessage("Pawn promoted to Queen!");
        }

        // Update en passant
        updateEnPassant(piece, fromRow, toRow, fromCol);

        // Add to history
        String moveNotation = positionToAlgebraic(fromRow, fromCol) + "-" + 
                             positionToAlgebraic(toRow, toCol);
        if (!capturedPiece.equals(" ")) {
            moveNotation += " x";
        }
        addMoveToHistory(moveNotation);

        return true;
    }

    private void updateCastlingRights(String piece, int fromRow, int fromCol) {
        if (piece.equals("♔")) whiteKingMoved = true;
        if (piece.equals("♚")) blackKingMoved = true;
        if (piece.equals("♖") && fromRow == 7 && fromCol == 0) whiteRookQueensideMoved = true;
        if (piece.equals("♖") && fromRow == 7 && fromCol == 7) whiteRookKingsideMoved = true;
        if (piece.equals("♜") && fromRow == 0 && fromCol == 0) blackRookQueensideMoved = true;
        if (piece.equals("♜") && fromRow == 0 && fromCol == 7) blackRookKingsideMoved = true;
    }

    private void updateEnPassant(String piece, int fromRow, int toRow, int fromCol) {
        if (piece.equals("♙") && fromRow == 6 && toRow == 4) {
            enPassantTarget = new int[]{5, fromCol};
        } else if (piece.equals("♟") && fromRow == 1 && toRow == 3) {
            enPassantTarget = new int[]{2, fromCol};
        } else {
            enPassantTarget = null;
        }
    }

    private void checkGameState() {
        if (isInCheck(whiteTurn)) {
            if (isCheckmate(whiteTurn)) {
                String winner = whiteTurn ? "Black" : "White";
                showMessage("Checkmate! " + winner + " wins!");
                updateStatus("CHECKMATE! " + winner + " wins!");
                return;
            } else {
                updateStatus((whiteTurn ? "White" : "Black") + "'s turn - CHECK!");
                highlightKingInCheck();
                return;
            }
        }
        updateStatus((whiteTurn ? "White" : "Black") + "'s turn");
    }

    private void highlightKingInCheck() {
        int[] kingPos = findKing(whiteTurn);
        if (kingPos != null) {
            squares[kingPos[0]][kingPos[1]].setCheck(true);
        }
    }

    private boolean isValidMoveForPiece(String piece, int fromRow, int fromCol, int toRow, int toCol) {
        String targetPiece = board[toRow][toCol];
        if (!targetPiece.equals(" ") && isPieceOfColor(targetPiece, whiteTurn)) {
            return false;
        }

        if (piece.equals("♙")) return isValidWhitePawnMove(fromRow, fromCol, toRow, toCol);
        if (piece.equals("♟")) return isValidBlackPawnMove(fromRow, fromCol, toRow, toCol);
        if (piece.equals("♖") || piece.equals("♜")) return isValidRookMove(fromRow, fromCol, toRow, toCol);
        if (piece.equals("♘") || piece.equals("♞")) return isValidKnightMove(fromRow, fromCol, toRow, toCol);
        if (piece.equals("♗") || piece.equals("♝")) return isValidBishopMove(fromRow, fromCol, toRow, toCol);
        if (piece.equals("♕") || piece.equals("♛")) {
            return isValidRookMove(fromRow, fromCol, toRow, toCol) || 
                   isValidBishopMove(fromRow, fromCol, toRow, toCol);
        }
        if (piece.equals("♔") || piece.equals("♚")) {
            return Math.abs(toRow - fromRow) <= 1 && Math.abs(toCol - fromCol) <= 1;
        }
        return false;
    }

    private boolean isValidWhitePawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (toRow == fromRow - 1 && fromCol == toCol && board[toRow][toCol].equals(" ")) return true;
        if (fromRow == 6 && toRow == 4 && fromCol == toCol && 
            board[toRow][toCol].equals(" ") && board[5][toCol].equals(" ")) return true;
        if (toRow == fromRow - 1 && Math.abs(toCol - fromCol) == 1) {
            String target = board[toRow][toCol];
            if (!target.equals(" ") && isPieceOfColor(target, false)) return true;
            if (enPassantTarget != null && toRow == enPassantTarget[0] && toCol == enPassantTarget[1]) {
                board[fromRow][toCol] = " ";
                return true;
            }
        }
        return false;
    }

    private boolean isValidBlackPawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (toRow == fromRow + 1 && fromCol == toCol && board[toRow][toCol].equals(" ")) return true;
        if (fromRow == 1 && toRow == 3 && fromCol == toCol && 
            board[toRow][toCol].equals(" ") && board[2][toCol].equals(" ")) return true;
        if (toRow == fromRow + 1 && Math.abs(toCol - fromCol) == 1) {
            String target = board[toRow][toCol];
            if (!target.equals(" ") && isPieceOfColor(target, true)) return true;
            if (enPassantTarget != null && toRow == enPassantTarget[0] && toCol == enPassantTarget[1]) {
                board[fromRow][toCol] = " ";
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

    private boolean isInCheck(boolean white) {
        int[] kingPos = findKing(white);
        if (kingPos == null) return false;
        return isSquareUnderAttack(kingPos[0], kingPos[1], white);
    }

    private boolean isCheckmate(boolean white) {
        if (!isInCheck(white)) return false;
        
        for (int fromRow = 0; fromRow < BOARD_SIZE; fromRow++) {
            for (int fromCol = 0; fromCol < BOARD_SIZE; fromCol++) {
                String piece = board[fromRow][fromCol];
                if (!piece.equals(" ") && isPieceOfColor(piece, white)) {
                    for (int toRow = 0; toRow < BOARD_SIZE; toRow++) {
                        for (int toCol = 0; toCol < BOARD_SIZE; toCol++) {
                            if (isValidMoveForPiece(piece, fromRow, fromCol, toRow, toCol)) {
                                String capturedPiece = board[toRow][toCol];
                                board[toRow][toCol] = piece;
                                board[fromRow][fromCol] = " ";
                                
                                boolean stillInCheck = isInCheck(white);
                                
                                board[fromRow][fromCol] = piece;
                                board[toRow][toCol] = capturedPiece;
                                
                                if (!stillInCheck) return false;
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    private int[] findKing(boolean white) {
        String king = white ? "♔" : "♚";
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                if (board[i][j].equals(king)) {
                    return new int[]{i, j};
                }
            }
        }
        return null;
    }

    private boolean isSquareUnderAttack(int row, int col, boolean byWhite) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                String piece = board[i][j];
                if (!piece.equals(" ") && isPieceOfColor(piece, !byWhite)) {
                    if (canAttackSquare(piece, i, j, row, col)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean canAttackSquare(String piece, int fromRow, int fromCol, int toRow, int toCol) {
        if (piece.equals("♙")) {
            return toRow == fromRow - 1 && Math.abs(toCol - fromCol) == 1;
        } else if (piece.equals("♟")) {
            return toRow == fromRow + 1 && Math.abs(toCol - fromCol) == 1;
        }
        return isValidMoveForPiece(piece, fromRow, fromCol, toRow, toCol);
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

    private String positionToAlgebraic(int row, int col) {
        char file = (char) ('a' + col);
        char rank = (char) ('8' - row);
        return "" + file + rank;
    }

    private void addMoveToHistory(String move) {
        moveHistory.add(move);
        updateMoveHistory();
    }

    private void updateMoveHistory() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < moveHistory.size(); i++) {
            if (i % 2 == 0) {
                sb.append((i / 2 + 1)).append(". ");
                sb.append(moveHistory.get(i));
                if (i + 1 < moveHistory.size()) {
                    sb.append(" ").append(moveHistory.get(i + 1));
                }
                sb.append("\n");
            }
        }
        moveHistoryArea.setText(sb.toString());
        moveHistoryArea.setCaretPosition(moveHistoryArea.getDocument().getLength());
    }

    private void updateBoard() {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                squares[row][col].updatePiece();
                squares[row][col].setSelected(false);
                squares[row][col].setValidMove(false);
                Color bgColor = (row + col) % 2 == 0 ? LIGHT_SQUARE : DARK_SQUARE;
                squares[row][col].setBackground(bgColor);
            }
        }
    }

    private void updateStatus(String message) {
        statusLabel.setText(message);
        if (message.contains("CHECK")) {
            statusLabel.setForeground(Color.RED);
        } else {
            statusLabel.setForeground(Color.BLACK);
        }
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message, "Chess", JOptionPane.INFORMATION_MESSAGE);
    }

    private String[][] initializeBoard() {
        String[][] board = new String[BOARD_SIZE][BOARD_SIZE];

        // Black pieces
        board[0][0] = "♜"; board[0][1] = "♞"; board[0][2] = "♝"; board[0][3] = "♛";
        board[0][4] = "♚"; board[0][5] = "♝"; board[0][6] = "♞"; board[0][7] = "♜";
        for (int i = 0; i < BOARD_SIZE; i++) board[1][i] = "♟";

        // White pieces
        board[7][0] = "♖"; board[7][1] = "♘"; board[7][2] = "♗"; board[7][3] = "♕";
        board[7][4] = "♔"; board[7][5] = "♗"; board[7][6] = "♘"; board[7][7] = "♖";
        for (int i = 0; i < BOARD_SIZE; i++) board[6][i] = "♙";

        // Empty squares
        for (int i = 2; i < 6; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = " ";
            }
        }

        return board;
    }
}