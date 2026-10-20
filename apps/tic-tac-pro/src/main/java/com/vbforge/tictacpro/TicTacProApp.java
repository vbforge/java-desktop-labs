package com.vbforge.tictacpro;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class TicTacProApp extends Application {

    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    private final TicTacToeEngine engine = new TicTacToeEngine();
    private final Button[] cells = new Button[9];
    
    private Label statusLabel;
    private Label scoreXLabel, scoreOLabel, scoreDLabel;
    
    private ComboBox<String> modeCombo;
    private boolean isPvP = false;
    private boolean isEasyAi = false;
    
    private String currentPlayer = TicTacToeEngine.X;
    private boolean isGameOver = false;
    
    private PauseTransition aiPause;

    private int scoreX = 0, scoreO = 0, scoreDraw = 0;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar());
        root.setCenter(buildMainContent());
        root.setBottom(buildBottomControls());

        Scene scene = new Scene(root, 600, 720);
        scene.getStylesheets().add(getClass().getResource("/tictacpro.css").toExternalForm());

        stage.setTitle("TicTacPro");
        stage.setScene(scene);
        stage.show();
        
        resetGame(true);
    }

    private Node buildTitleBar() {
        Label title = new Label("TicTacPro");
        title.getStyleClass().add("app-title");

        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 16));
        titleArea.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleArea.setOnMouseDragged(e -> { primaryStage.setX(e.getScreenX() - xOffset); primaryStage.setY(e.getScreenY() - yOffset); });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> primaryStage.close());

        HBox bar = new HBox(titleArea, closeBtn);
        bar.getStyleClass().add("titlebar");
        return bar;
    }

    private Node buildMainContent() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 24, 10, 24));

        // Header
        statusLabel = new Label("X's Turn");
        statusLabel.getStyleClass().add("status-text");

        modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll("Player vs Player", "Player vs AI (Easy)", "Player vs AI (Unbeatable)");
        modeCombo.getSelectionModel().select(2);
        modeCombo.setPrefWidth(220);
        modeCombo.setMaxWidth(220);
        modeCombo.getStyleClass().add("mode-combo");
        modeCombo.setOnAction(e -> {
            int idx = modeCombo.getSelectionModel().getSelectedIndex();
            isPvP = idx == 0;
            isEasyAi = idx == 1;
            resetGame(true);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(20, statusLabel, spacer, modeCombo);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // Scoreboard
        scoreXLabel = createScoreValueLabel();
        scoreOLabel = createScoreValueLabel();
        scoreDLabel = createScoreValueLabel();
        
        VBox scoreXBox = createScoreBox("Player X", scoreXLabel, "score-x");
        VBox scoreOBox = createScoreBox("Player O", scoreOLabel, "score-o");
        VBox scoreDBox = createScoreBox("Draws", scoreDLabel, "score-d");
        
        HBox.setHgrow(scoreXBox, Priority.ALWAYS);
        HBox.setHgrow(scoreOBox, Priority.ALWAYS);
        HBox.setHgrow(scoreDBox, Priority.ALWAYS);
        scoreXBox.setMaxWidth(Double.MAX_VALUE);
        scoreOBox.setMaxWidth(Double.MAX_VALUE);
        scoreDBox.setMaxWidth(Double.MAX_VALUE);
        
        HBox scoreboard = new HBox(10, scoreXBox, scoreDBox, scoreOBox);

        // Board (Constrained size)
        GridPane boardGrid = new GridPane();
        boardGrid.getStyleClass().add("board");
        boardGrid.setHgap(10);
        boardGrid.setVgap(10);
        boardGrid.setAlignment(Pos.CENTER);
        
        // Force the board to stay compact
        boardGrid.setMaxSize(420, 420);
        
        for (int i = 0; i < 9; i++) {
            Button cell = new Button();
            cell.getStyleClass().add("cell");
            cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            final int index = i;
            cell.setOnAction(e -> handleCellClick(index));
            cells[i] = cell;
            
            ColumnConstraints col = new ColumnConstraints();
            col.setPercentWidth(33.33);
            RowConstraints row = new RowConstraints();
            row.setPercentHeight(33.33);
            if (i < 3) boardGrid.getColumnConstraints().add(col);
            if (i % 3 == 0) boardGrid.getRowConstraints().add(row);
            
            boardGrid.add(cell, i % 3, i / 3);
        }
        
        // Wrap it in a StackPane to center it in the remaining space
        StackPane boardWrapper = new StackPane(boardGrid);
        VBox.setVgrow(boardWrapper, Priority.ALWAYS);

        content.getChildren().addAll(header, scoreboard, boardWrapper);
        return content;
    }

    private Node buildBottomControls() {
        Button newRoundBtn = new Button("New Round");
        newRoundBtn.getStyleClass().add("btn");
        newRoundBtn.setMaxWidth(Double.MAX_VALUE);
        newRoundBtn.setPrefHeight(45);
        newRoundBtn.setOnAction(e -> resetGame(true));

        Button resetMatchBtn = new Button("Reset Match");
        resetMatchBtn.getStyleClass().addAll("btn", "primary");
        resetMatchBtn.setMaxWidth(Double.MAX_VALUE);
        resetMatchBtn.setPrefHeight(45);
        resetMatchBtn.setOnAction(e -> resetGame(false));

        HBox controls = new HBox(10, newRoundBtn, resetMatchBtn);
        HBox.setHgrow(newRoundBtn, Priority.ALWAYS);
        HBox.setHgrow(resetMatchBtn, Priority.ALWAYS);
        controls.setPadding(new Insets(0, 24, 24, 24));
        return controls;
    }

    private Label createScoreValueLabel() {
        Label l = new Label("0");
        l.getStyleClass().add("score-value");
        return l;
    }

    private VBox createScoreBox(String title, Label valueLabel, String styleClass) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("score-label");
        VBox box = new VBox(4, titleLabel, valueLabel);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().addAll("score-box", styleClass);
        return box;
    }

    private void handleCellClick(int index) {
        if (isGameOver || !engine.getMark(index).equals(TicTacToeEngine.EMPTY)) return;
        if (!isPvP && currentPlayer.equals(TicTacToeEngine.O)) return;
        makeMove(index, currentPlayer);
    }

    private void makeMove(int index, String mark) {
        engine.makeMove(index, mark);
        
        Button cell = cells[index];
        cell.setText(mark);
        cell.getStyleClass().add(mark.equals(TicTacToeEngine.X) ? "x" : "o");
        cell.setDisable(true);

        if (checkGameOver()) return;

        currentPlayer = currentPlayer.equals(TicTacToeEngine.X) ? TicTacToeEngine.O : TicTacToeEngine.X;
        updateStatus();
        
        if (!isPvP && currentPlayer.equals(TicTacToeEngine.O)) {
            triggerAiMove();
        }
    }

    private boolean checkGameOver() {
        String winner = engine.checkWinner();
        if (winner != null) {
            isGameOver = true;
            statusLabel.setText(winner + " Wins!");
            statusLabel.setStyle(winner.equals(TicTacToeEngine.X) ? "-fx-text-fill: #6ea8d8;" : "-fx-text-fill: #f85149;");
            highlightWinningLine(winner);
            
            if (winner.equals(TicTacToeEngine.X)) scoreX++;
            else scoreO++;
            updateScoreboard();
            return true;
        }

        if (engine.isDraw()) {
            isGameOver = true;
            statusLabel.setText("It's a Draw!");
            statusLabel.setStyle("-fx-text-fill: #8b949e;");
            scoreDraw++;
            updateScoreboard();
            return true;
        }
        return false;
    }

    private void highlightWinningLine(String winner) {
        int[][] lines = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for (int[] line : lines) {
            if (engine.getMark(line[0]).equals(winner) && 
                engine.getMark(line[1]).equals(winner) && 
                engine.getMark(line[2]).equals(winner)) {
                for (int idx : line) {
                    cells[idx].getStyleClass().add("win");
                }
                break;
            }
        }
    }

    private void triggerAiMove() {
        statusLabel.setText("AI is thinking...");
        
        aiPause = new PauseTransition(Duration.millis(400));
        aiPause.setOnFinished(e -> {
            if (isGameOver) return;
            int moveIndex = isEasyAi ? engine.getRandomMove() : engine.getBestMove(TicTacToeEngine.O, TicTacToeEngine.X);
            if (moveIndex != -1) {
                makeMove(moveIndex, TicTacToeEngine.O);
            }
        });
        aiPause.play();
    }

    private void resetGame(boolean keepScores) {
        if (aiPause != null) aiPause.stop();

        engine.reset();
        currentPlayer = TicTacToeEngine.X;
        isGameOver = false;

        for (Button cell : cells) {
            cell.setText("");
            cell.setDisable(false);
            cell.getStyleClass().removeAll("x", "o", "win");
        }

        updateStatus();
        
        if (!keepScores) {
            scoreX = 0; scoreO = 0; scoreDraw = 0;
            updateScoreboard();
        }
    }

    private void updateStatus() {
        if (isGameOver) return;
        statusLabel.setText(currentPlayer + "'s Turn");
        statusLabel.setStyle("-fx-text-fill: #e6edf3;");
    }

    private void updateScoreboard() {
        scoreXLabel.setText(String.valueOf(scoreX));
        scoreOLabel.setText(String.valueOf(scoreO));
        scoreDLabel.setText(String.valueOf(scoreDraw));
    }

    public static void main(String[] args) {
        launch(args);
    }
}