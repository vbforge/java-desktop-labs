package com.vbforge.chessmate;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ChessMateApp extends Application {

    private Board board;
    private boolean isPvP = true;
    private boolean isAdvanced = false;
    private int timeLimitMins = 10;

    // Game State
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private boolean isAiThinking = false;

    // UI Elements
    private GridPane boardGrid;
    private Label[][] squareLabels;
    private Label whiteTimerLabel, blackTimerLabel, statusLabel;
    private VBox whiteTimerBox, blackTimerBox;
    private ComboBox<String> modeCombo, diffCombo;
    private RadioButton rb5, rb10, rb15;
    private Button startBtn, stopBtn, resetBtn, undoBtn, rulesBtn;
    private ListView<String> historyList;

    // Selection
    private Square selectedSquare = null;
    private List<Square> validMoves = new ArrayList<>();
    
    // Timers
    private AnimationTimer timer;
    private long whiteTimeMs, blackTimeMs;
    private long lastTick = 0;
    private boolean timerRunning = false;

    @Override
    public void start(Stage primaryStage) {
        board = new Board();
        boardGrid = new GridPane();
        boardGrid.setPrefSize(560, 560);
        squareLabels = new Label[8][8];

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setCenter(createBoardUI());
        root.setRight(createSidebar());

        resetBoard(); // Sets initial unstarted state

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(getClass().getResource("/chessmate.css").toExternalForm());

        primaryStage.setTitle("ChessMate");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.setOnCloseRequest(e -> System.exit(0));
        primaryStage.show();

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!timerRunning || !gameStarted || gameOver) {
                    lastTick = now;
                    return;
                }
                if (lastTick == 0) lastTick = now;
                long elapsedMs = (now - lastTick) / 1_000_000;
                lastTick = now;

                if (board.getSideToMove() == Side.WHITE) {
                    whiteTimeMs -= elapsedMs;
                    if (whiteTimeMs <= 0) { whiteTimeMs = 0; endGame("Black wins on time!"); }
                    updateTimers();
                } else {
                    blackTimeMs -= elapsedMs;
                    if (blackTimeMs <= 0) { blackTimeMs = 0; endGame("White wins on time!"); }
                    updateTimers();
                }
            }
        };
        timer.start();
    }

    private Node createBoardUI() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                StackPane square = new StackPane();
                square.setPrefSize(70, 70);
                boolean isLight = (r + c) % 2 == 0;
                square.getStyleClass().add(isLight ? "square-light" : "square-dark");
                
                Label pieceLabel = new Label();
                pieceLabel.getStyleClass().add("piece");
                squareLabels[r][c] = pieceLabel;
                square.getChildren().add(pieceLabel);

                final int row = r, col = c;
                square.setOnMouseClicked(e -> handleSquareClick(row, col));
                
                boardGrid.add(square, c, r);
            }
        }
        return boardGrid;
    }

    private Node createSidebar() {
        VBox sidebar = new VBox(12);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(320);
        sidebar.getStyleClass().add("sidebar");

        // Timers
        blackTimerLabel = new Label("10:00");
        whiteTimerLabel = new Label("10:00");
        blackTimerBox = createTimerBox("Black", blackTimerLabel);
        whiteTimerBox = createTimerBox("White", whiteTimerLabel);

        // Settings
        modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("Player vs Player", "Player vs AI");
        modeCombo.getSelectionModel().selectFirst();
        modeCombo.setOnAction(e -> {
            isPvP = modeCombo.getSelectionModel().getSelectedIndex() == 0;
            updateTimerVisibility();
        });

        diffCombo = new ComboBox<>();
        diffCombo.getItems().addAll("Easy", "Advanced");
        diffCombo.getSelectionModel().selectFirst();
        diffCombo.setDisable(true);
        diffCombo.setOnAction(e -> isAdvanced = diffCombo.getSelectionModel().getSelectedIndex() == 1);

        modeCombo.valueProperty().addListener((obs, old, val) -> diffCombo.setDisable(val.equals("Player vs Player")));

        ToggleGroup timeGroup = new ToggleGroup();
        rb5 = new RadioButton("5 min"); rb5.setToggleGroup(timeGroup); rb5.setUserData(5);
        rb10 = new RadioButton("10 min"); rb10.setToggleGroup(timeGroup); rb10.setUserData(10); rb10.setSelected(true);
        rb15 = new RadioButton("15 min"); rb15.setToggleGroup(timeGroup); rb15.setUserData(15);
        
        timeGroup.selectedToggleProperty().addListener((obs, old, val) -> {
            if (val != null && !gameStarted) timeLimitMins = (int) val.getUserData();
        });
        HBox timeBox = new HBox(10, rb5, rb10, rb15);

        // Action Buttons
        startBtn = new Button("Start Game");
        startBtn.getStyleClass().add("btn-primary");
        startBtn.setMaxWidth(Double.MAX_VALUE);
        startBtn.setOnAction(e -> startGame());

        stopBtn = new Button("Stop Game");
        stopBtn.getStyleClass().add("btn-danger");
        stopBtn.setMaxWidth(Double.MAX_VALUE);
        stopBtn.setDisable(true);
        stopBtn.setOnAction(e -> stopGame());

        resetBtn = new Button("Reset");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.setOnAction(e -> resetBoard());

        undoBtn = new Button("Undo");
        undoBtn.setMaxWidth(Double.MAX_VALUE);
        undoBtn.setDisable(true);
        undoBtn.setOnAction(e -> handleUndo());

        rulesBtn = new Button("Rules");
        rulesBtn.setMaxWidth(Double.MAX_VALUE);
        rulesBtn.setOnAction(e -> showRules());

        statusLabel = new Label("Ready to start");
        statusLabel.getStyleClass().add("status");

        historyList = new ListView<>();
        VBox.setVgrow(historyList, Priority.ALWAYS);
        historyList.getStyleClass().add("history");

        VBox settings = new VBox(6,
                new Label("Mode:"), modeCombo,
                new Label("Difficulty:"), diffCombo,
                new Label("Time Control:"), timeBox
        );

        HBox topRow = new HBox(10, startBtn, stopBtn);
        HBox botRow = new HBox(10, resetBtn, undoBtn, rulesBtn);
        
        sidebar.getChildren().addAll(blackTimerBox, whiteTimerBox, settings, topRow, botRow, statusLabel, historyList);
        return sidebar;
    }

    private VBox createTimerBox(String name, Label timeLabel) {
        timeLabel.getStyleClass().add("timer-text");
        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("timer-name");
        VBox box = new VBox(4, nameLabel, timeLabel);
        box.getStyleClass().add("timer-box");
        return box;
    }

    private void startGame() {
        gameStarted = true;
        gameOver = false;
        isAiThinking = false;
        
        // Lock settings
        modeCombo.setDisable(true);
        rb5.setDisable(true); rb10.setDisable(true); rb15.setDisable(true);
        startBtn.setDisable(true);
        stopBtn.setDisable(false);
        undoBtn.setDisable(false);

        // Start timers
        timerRunning = isPvP;
        lastTick = 0;
        statusLabel.setText("White's turn");
        updateBoardUI();
    }

    private void stopGame() {
        if (!gameStarted || gameOver) return;
        
        gameOver = true;
        timerRunning = false;
        statusLabel.setText("Game Stopped");
        
        // Lock UI
        startBtn.setDisable(true);
        stopBtn.setDisable(true);
        undoBtn.setDisable(true);

        int whitePoints = calculatePoints(Side.WHITE);
        int blackPoints = calculatePoints(Side.BLACK);

        String msg = String.format("Game manually stopped.\n\nWhite Points: %d\nBlack Points: %d\n\n", whitePoints, blackPoints);
        if (whitePoints > blackPoints) msg += "White is ahead by " + (whitePoints - blackPoints) + " points.";
        else if (blackPoints > whitePoints) msg += "Black is ahead by " + (blackPoints - whitePoints) + " points.";
        else msg += "It's a tie on points.";

        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText("Match Stopped");
        alert.show();
    }

    private int calculatePoints(Side side) {
        int points = 0;
        for (Piece piece : Piece.values()) {
            if (piece == Piece.NONE || piece.getPieceSide() != side) continue;
            long count = board.getPieceLocation(piece).size();
            points += switch (piece.getPieceType()) {
                case PAWN -> 1;
                case KNIGHT, BISHOP -> 3;
                case ROOK -> 5;
                case QUEEN -> 9;
                default -> 0;
            } * count;
        }
        return points;
    }

    private void resetBoard() {
        board = new Board();
        selectedSquare = null;
        validMoves.clear();
        gameStarted = false;
        gameOver = false;
        timerRunning = false;
        isAiThinking = false;
        
        whiteTimeMs = timeLimitMins * 60 * 1000L;
        blackTimeMs = timeLimitMins * 60 * 1000L;
        updateTimers();
        
        // Unlock settings
        modeCombo.setDisable(false);
        rb5.setDisable(false); rb10.setDisable(false); rb15.setDisable(false);
        startBtn.setDisable(false);
        stopBtn.setDisable(true);
        undoBtn.setDisable(true);

        historyList.getItems().clear();
        statusLabel.setText("Ready to start");
        updateTimerVisibility();
        updateBlackTimerLabel();
        updateBoardUI();
        highlightCheck();
    }

    private void updateTimerVisibility() {
        boolean visible = isPvP;
        whiteTimerBox.setVisible(visible);
        whiteTimerBox.setManaged(visible);
        blackTimerBox.setVisible(visible);
        blackTimerBox.setManaged(visible);
    }

    private void updateBlackTimerLabel() {
        Label nameLabel = (Label) blackTimerBox.getChildren().get(0);
        nameLabel.setText(isPvP ? "Black" : "Black (AI)");
    }

    private void updateTimers() {
        whiteTimerLabel.setText(formatTime(whiteTimeMs));
        blackTimerLabel.setText(formatTime(blackTimeMs));

        whiteTimerBox.getStyleClass().remove("active");
        blackTimerBox.getStyleClass().remove("active");
        if (timerRunning && gameStarted && !gameOver) {
            if (board.getSideToMove() == Side.WHITE) whiteTimerBox.getStyleClass().add("active");
            else blackTimerBox.getStyleClass().add("active");
        }
    }

    private String formatTime(long ms) {
        long totalSecs = ms / 1000;
        return String.format("%02d:%02d", totalSecs / 60, totalSecs % 60);
    }

    private void handleSquareClick(int row, int col) {
        if (!gameStarted || gameOver || isAiThinking || board.isMated() || board.isDraw()) return;

        Square clicked = Square.encode(Rank.values()[7 - row], File.values()[col]);
        
        if (selectedSquare == null) {
            Piece piece = board.getPiece(clicked);
            if (piece != Piece.NONE && piece.getPieceSide() == board.getSideToMove()) {
                selectedSquare = clicked;
                validMoves.clear();
                for (Move m : board.legalMoves()) {
                    if (m.getFrom() == clicked) validMoves.add(m.getTo());
                }
                updateBoardUI();
            }
        } else {
            if (validMoves.contains(clicked)) {
                Move move = new Move(selectedSquare, clicked);
                if (board.getPiece(selectedSquare) == Piece.WHITE_PAWN && clicked.getRank() == Rank.RANK_8) {
                    move = new Move(selectedSquare, clicked, Piece.WHITE_QUEEN);
                } else if (board.getPiece(selectedSquare) == Piece.BLACK_PAWN && clicked.getRank() == Rank.RANK_1) {
                    move = new Move(selectedSquare, clicked, Piece.BLACK_QUEEN);
                }

                board.doMove(move);
                selectedSquare = null;
                validMoves.clear();
                afterMove();
            } else {
                selectedSquare = null;
                validMoves.clear();
                updateBoardUI();
            }
        }
    }

    private void afterMove() {
        updateBoardUI();
        highlightCheck();
        updateHistory();

        if (board.isMated()) {
            endGame((board.getSideToMove() == Side.WHITE ? "Black" : "White") + " wins by Checkmate!");
            return;
        }
        if (board.isDraw()) {
            endGame(board.legalMoves().isEmpty() && !board.isKingAttacked() ? "Draw by Stalemate!" : "Draw!");
            return;
        }

        statusLabel.setText((board.getSideToMove() == Side.WHITE ? "White" : "Black") + "'s turn");

        if (!isPvP && board.getSideToMove() == Side.BLACK) {
            triggerAiMove();
        }
    }

    private void triggerAiMove() {
        isAiThinking = true;
        timerRunning = false;
        statusLabel.setText("AI is thinking...");

        Task<Move> aiTask = new Task<>() {
            @Override
            protected Move call() {
                return ChessAI.calculateBestMove(board, isAdvanced);
            }
        };

        aiTask.setOnSucceeded(e -> {
            Move bestMove = aiTask.getValue();
            if (bestMove != null) {
                board.doMove(bestMove);
                afterMove();
            }
            isAiThinking = false;
            timerRunning = isPvP;
        });

        new Thread(aiTask).start();
    }

    private void handleUndo() {
        if (isAiThinking || !gameStarted || gameOver) return;
        
        if (!isPvP) {
            if (board.getMoveCounter() >= 2) {
                board.undoMove();
                board.undoMove();
            }
        } else {
            if (board.getMoveCounter() >= 1) {
                board.undoMove();
            }
        }
        
        selectedSquare = null;
        validMoves.clear();
        updateBoardUI();
        highlightCheck();
        updateHistory();
        statusLabel.setText((board.getSideToMove() == Side.WHITE ? "White" : "Black") + "'s turn");
    }

    private void endGame(String msg) {
        gameOver = true;
        timerRunning = false;
        statusLabel.setText(msg);
        stopBtn.setDisable(true);
        undoBtn.setDisable(true);

        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg);
        alert.setHeaderText("Game Over");
        alert.show();
    }

    private void updateBoardUI() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Square sq = Square.encode(Rank.values()[7 - r], File.values()[c]);
                Piece p = board.getPiece(sq);
                
                Label lbl = squareLabels[r][c];
                lbl.setText(getPieceSymbol(p));
                lbl.getStyleClass().setAll("piece");
                if (p != Piece.NONE) {
                    lbl.getStyleClass().add(p.getPieceSide() == Side.WHITE ? "piece-w" : "piece-b");
                }

                StackPane square = (StackPane) lbl.getParent();
                square.getStyleClass().removeAll("selected", "valid-move");
                
                if (sq == selectedSquare) square.getStyleClass().add("selected");
                else if (validMoves.contains(sq)) square.getStyleClass().add("valid-move");
            }
        }
    }

    private void highlightCheck() {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                ((StackPane) squareLabels[r][c].getParent()).getStyleClass().remove("check");
            }
        }
        if (board.isKingAttacked()) {
            Square kingSq = board.getKingSquare(board.getSideToMove());
            int r = 7 - kingSq.getRank().ordinal();
            int c = kingSq.getFile().ordinal();
            ((StackPane) squareLabels[r][c].getParent()).getStyleClass().add("check");
        }
    }

    private void updateHistory() {
        historyList.getItems().clear();
        var moves = board.getHistory();
        for (int i = 0; i < moves.size(); i++) {
            String prefix = (i % 2 == 0) ? ((i / 2 + 1) + ". ") : "   ";
            historyList.getItems().add(prefix + moves.get(i).toString());
        }
        historyList.scrollTo(historyList.getItems().size() - 1);
    }

    private void showRules() {
        boolean wasRunning = timerRunning;
        timerRunning = false;

        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Chess Rules");
        
        VBox box = new VBox(12);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("modal-bg");
        
        Label title = new Label("How the Pieces Move");
        title.getStyleClass().add("modal-title");
        
        box.getChildren().addAll(
            title,
            new Label("♙ Pawn: 1 square forward (2 on first move). Captures diagonally."),
            new Label("♘ Knight: L-shape (2 in one direction, 1 perpendicular)."),
            new Label("♗ Bishop: Any number of squares diagonally."),
            new Label("♖ Rook: Any number of squares horizontally or vertically."),
            new Label("♕ Queen: Combines Rook and Bishop."),
            new Label("♔ King: 1 square in any direction."),
            new Label("\nScoring: Pawn=1, Knight=3, Bishop=3, Rook=5, Queen=9")
        );

        Button close = new Button("Got it");
        close.getStyleClass().add("btn-primary");
        close.setOnAction(e -> {
            dialog.close();
            if (wasRunning) timerRunning = true;
        });

        box.getChildren().add(close);
        Scene scene = new Scene(box, 400, 400);
        scene.getStylesheets().add(getClass().getResource("/chessmate.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private String getPieceSymbol(Piece p) {
        return switch (p) {
            case WHITE_PAWN -> "♙"; case WHITE_ROOK -> "♖"; case WHITE_KNIGHT -> "♘";
            case WHITE_BISHOP -> "♗"; case WHITE_QUEEN -> "♕"; case WHITE_KING -> "♔";
            case BLACK_PAWN -> "♟"; case BLACK_ROOK -> "♜"; case BLACK_KNIGHT -> "♞";
            case BLACK_BISHOP -> "♝"; case BLACK_QUEEN -> "♛"; case BLACK_KING -> "♚";
            default -> "";
        };
    }

    public static void main(String[] args) {
        launch(args);
    }
}