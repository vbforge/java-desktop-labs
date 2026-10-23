package com.vbforge.sudoku;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.ArrayList;
import java.util.List;

public class SudokuForgeApp extends Application {

    private static final int CELL_SIZE = 48;
    private final SudokuBoard board = new SudokuBoard();
    private final PuzzleGenerator generator = new PuzzleGenerator();
    
    private Label[][] cells = new Label[9][9];
    private Label statusLabel;
    private VBox controlsBox;
    
    private Task<?> activeTask;
    private AnimationTimer animationTimer;
    private long stepDelayNs = 15_000_000L; // 15ms default
    
    private int[][] initialPuzzle;

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar(stage));
        root.setCenter(buildMainLayout());

        Scene scene = new Scene(root, 950, 750);
        scene.getStylesheets().add(getClass().getResource("/sudoku.css").toExternalForm());

        stage.setTitle("SudokuForge");
        stage.setScene(scene);
        stage.show();

        newGame(Difficulty.MEDIUM);
    }

    private Node buildTitleBar(Stage stage) {
        Label title = new Label("SudokuForge");
        title.getStyleClass().add("app-title");

        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 16));
        titleArea.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleArea.setOnMouseDragged(e -> { stage.setX(e.getScreenX() - xOffset); stage.setY(e.getScreenY() - yOffset); });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> stage.close());

        HBox bar = new HBox(titleArea, closeBtn);
        bar.getStyleClass().add("titlebar");
        return bar;
    }

    private Node buildMainLayout() {
        BorderPane main = new BorderPane();
        main.setPadding(new Insets(20));
        
        Label heading = new Label("SUDOKU FORGE");
        heading.getStyleClass().add("main-title");
        Label sub = new Label("Backtracking Algorithm Visualizer");
        sub.getStyleClass().add("subtitle");
        VBox header = new VBox(4, heading, sub);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(0, 0, 20, 0));
        main.setTop(header);

        // Nested GridPanes for perfect borders
        GridPane outerGrid = new GridPane();
        outerGrid.getStyleClass().add("sudoku-outer-grid");
        outerGrid.setAlignment(Pos.CENTER);

        for (int blockRow = 0; blockRow < 3; blockRow++) {
            for (int blockCol = 0; blockCol < 3; blockCol++) {
                GridPane innerGrid = new GridPane();
                innerGrid.getStyleClass().add("sudoku-inner-grid");

                for (int r = 0; r < 3; r++) {
                    for (int c = 0; c < 3; c++) {
                        int actualRow = blockRow * 3 + r;
                        int actualCol = blockCol * 3 + c;

                        Label cell = new Label();
                        cell.getStyleClass().add("cell");
                        cell.setPrefSize(CELL_SIZE, CELL_SIZE);
                        cell.setAlignment(Pos.CENTER);
                        cell.setFont(Font.font("Monospaced", 24));
                        
                        final int row = actualRow, col = actualCol;
                        cell.setOnMouseClicked(e -> handleCellClick(row, col));
                        
                        cells[actualRow][actualCol] = cell;
                        innerGrid.add(cell, c, r);
                    }
                }
                outerGrid.add(innerGrid, blockCol, blockRow);
            }
        }
        main.setCenter(outerGrid);

        main.setRight(buildControls());
        BorderPane.setMargin(controlsBox, new Insets(0, 0, 0, 20));
        
        return main;
    }

    private Node buildControls() {
        controlsBox = new VBox(12);
        controlsBox.getStyleClass().add("controls");
        controlsBox.setPrefWidth(280);

        Label diffLbl = new Label("DIFFICULTY");
        diffLbl.getStyleClass().add("section-label");
        
        ComboBox<Difficulty> diffCombo = new ComboBox<>();
        diffCombo.getItems().addAll(Difficulty.values());
        diffCombo.getSelectionModel().select(Difficulty.MEDIUM);
        diffCombo.setMaxWidth(Double.MAX_VALUE);
        diffCombo.getStyleClass().add("styled-combo");

        Button newBtn = createButton("NEW PUZZLE", "btn-primary");
        newBtn.setOnAction(e -> newGame(diffCombo.getValue()));

        Button solveAnimBtn = createButton("SOLVE (ANIMATED)", "btn");
        solveAnimBtn.setOnAction(e -> solveAnimated());

        Button solveInstBtn = createButton("SOLVE (INSTANT)", "btn");
        solveInstBtn.setOnAction(e -> solveInstant());

        Button clearBtn = createButton("CLEAR ENTRIES", "btn");
        clearBtn.setOnAction(e -> { stopActiveTask(); board.clearUserEntries(); renderBoard(); });

        Button resetBtn = createButton("RESET BOARD", "btn");
        resetBtn.setOnAction(e -> {
            stopActiveTask();
            if (initialPuzzle != null) {
                board.loadFrom(initialPuzzle);
                renderBoard();
                setStatus("Board reset to original puzzle.");
            }
        });

        Button helpBtn = createButton("HELP", "btn");
        helpBtn.setOnAction(e -> showHelp());

        Label speedLbl = new Label("ANIMATION SPEED");
        speedLbl.getStyleClass().add("section-label");
        
        Slider speedSlider = new Slider(0, 100, 80);
        speedSlider.setMaxWidth(Double.MAX_VALUE);
        speedSlider.valueProperty().addListener((obs, old, val) -> {
            stepDelayNs = Math.max(0, 60 - (long)(val.doubleValue() * 0.6)) * 1_000_000L;
        });

        Label statusTitle = new Label("STATUS");
        statusTitle.getStyleClass().add("section-label");
        
        statusLabel = new Label("Ready.");
        statusLabel.getStyleClass().add("status-box");
        statusLabel.setWrapText(true);
        statusLabel.setTextAlignment(TextAlignment.LEFT);
        statusLabel.setPrefHeight(70);
        statusLabel.setMaxHeight(70);

        controlsBox.getChildren().addAll(
            diffLbl, diffCombo, new Region(), newBtn, solveAnimBtn, solveInstBtn, 
            clearBtn, resetBtn, helpBtn, new Region(), speedLbl, speedSlider, statusTitle, statusLabel
        );

        VBox.setVgrow(controlsBox.getChildren().get(2), Priority.ALWAYS);
        VBox.setVgrow(controlsBox.getChildren().get(10), Priority.ALWAYS);

        return controlsBox;
    }

    private Button createButton(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private void handleCellClick(int row, int col) {
        if (board.isFixed(row, col) || activeTask != null) return;
        int current = board.get(row, col);
        int next = (current + 1) % 10;
        board.set(row, col, next);
        renderBoard();
    }

    private void renderBoard() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                int val = board.get(r, c);
                Label cell = cells[r][c];
                cell.setText(val == 0 ? "" : String.valueOf(val));
                
                cell.getStyleClass().removeAll("fixed", "user", "trying", "backtrack");
                if (board.isFixed(r, c)) {
                    cell.getStyleClass().add("fixed");
                } else if (val != 0) {
                    cell.getStyleClass().add("user");
                }
            }
        }
    }

    private void clearHighlights() {
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                cells[r][c].getStyleClass().removeAll("trying", "backtrack");
            }
        }
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void stopActiveTask() {
        if (activeTask != null) {
            activeTask.cancel(true);
            activeTask = null;
        }
        if (animationTimer != null) {
            animationTimer.stop();
            animationTimer = null;
        }
        controlsBox.setDisable(false);
        clearHighlights();
    }

    private void newGame(Difficulty difficulty) {
        stopActiveTask();
        setStatus("Generating " + difficulty + " puzzle...");
        controlsBox.setDisable(true);

        Task<int[][]> task = new Task<>() {
            @Override
            protected int[][] call() {
                return generator.generatePuzzle(difficulty);
            }
        };

        task.setOnSucceeded(e -> {
            initialPuzzle = task.getValue();
            board.loadFrom(initialPuzzle);
            renderBoard();
            setStatus("New " + difficulty + " puzzle generated.");
            controlsBox.setDisable(false);
            activeTask = null;
        });

        task.setOnFailed(e -> {
            setStatus("Failed to generate puzzle.");
            controlsBox.setDisable(false);
            activeTask = null;
        });

        activeTask = task;
        new Thread(task).start();
    }

    private void solveInstant() {
        stopActiveTask();
        controlsBox.setDisable(true);
        setStatus("Solving instantly...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                int[][] snapshot = board.toArray();
                boolean solved = new SudokuSolver(null).solve(snapshot);
                if (solved) Platform.runLater(() -> board.loadFrom(snapshot));
                return solved;
            }
        };

        task.setOnSucceeded(e -> {
            if (task.getValue()) {
                renderBoard();
                setStatus("Solved instantly.");
            } else {
                setStatus("No solution exists.");
            }
            controlsBox.setDisable(false);
            activeTask = null;
        });

        activeTask = task;
        new Thread(task).start();
    }

    private void solveAnimated() {
        stopActiveTask();
        controlsBox.setDisable(true);
        setStatus("Computing solution steps...");

        Task<List<StepEvent>> task = new Task<>() {
            @Override
            protected List<StepEvent> call() {
                int[][] snapshot = board.toArray();
                List<StepEvent> events = new ArrayList<>();
                
                SudokuSolver solver = new SudokuSolver(new SolverListener() {
                    @Override public void onTry(int r, int c, int n) { events.add(new StepEvent(StepEvent.Type.TRY, r, c, n)); }
                    @Override public void onPlace(int r, int c, int n) { events.add(new StepEvent(StepEvent.Type.PLACE, r, c, n)); }
                    @Override public void onBacktrack(int r, int c) { events.add(new StepEvent(StepEvent.Type.BACKTRACK, r, c, 0)); }
                    @Override public void onSolved(int[][] b) { events.add(new StepEvent(StepEvent.Type.SOLVED, -1, -1, 0)); }
                });
                
                if (!solver.solve(snapshot)) {
                    events.add(new StepEvent(StepEvent.Type.FAILED, -1, -1, 0));
                }
                return events;
            }
        };

        task.setOnSucceeded(e -> {
            List<StepEvent> events = task.getValue();
            setStatus("Animating...");
            playAnimation(events, 0);
        });

        activeTask = task;
        new Thread(task).start();
    }

    private void playAnimation(List<StepEvent> events, int startIndex) {
        int[] index = {startIndex};
        long[] lastTime = {0};

        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastTime[0] < stepDelayNs) return;
                lastTime[0] = now;

                if (index[0] >= events.size()) {
                    stop();
                    controlsBox.setDisable(false);
                    activeTask = null;
                    return;
                }

                StepEvent step = events.get(index[0]++);
                
                if (step.row() >= 0 && step.col() >= 0) {
                    for (int r = 0; r < 9; r++) {
                        for (int c = 0; c < 9; c++) {
                            cells[r][c].getStyleClass().removeAll("trying", "backtrack");
                        }
                    }
                }

                switch (step.type()) {
                    case TRY -> {
                        cells[step.row()][step.col()].getStyleClass().add("trying");
                        setStatus("Trying " + step.num() + " at (" + step.row() + "," + step.col() + ")");
                    }
                    case PLACE -> {
                        board.set(step.row(), step.col(), step.num());
                        cells[step.row()][step.col()].setText(String.valueOf(step.num()));
                        cells[step.row()][step.col()].getStyleClass().add("trying");
                    }
                    case BACKTRACK -> {
                        board.set(step.row(), step.col(), 0);
                        cells[step.row()][step.col()].setText("");
                        cells[step.row()][step.col()].getStyleClass().add("backtrack");
                    }
                    case SOLVED -> {
                        renderBoard();
                        setStatus("Solution found!");
                        stop();
                        controlsBox.setDisable(false);
                        activeTask = null;
                    }
                    case FAILED -> {
                        setStatus("No solution found.");
                        stop();
                        controlsBox.setDisable(false);
                        activeTask = null;
                    }
                }
            }
        };
        animationTimer.start();
    }

    private void showHelp() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.getStyleClass().add("modal-bg");
        box.setAlignment(Pos.CENTER);

        Label title = new Label("How Sudoku Backtracking Works");
        title.getStyleClass().add("modal-title");

        TextArea text = new TextArea();
        text.setEditable(false);
        text.setWrapText(true);
        text.getStyleClass().add("theory-text");
        text.setPrefHeight(400);
        text.setPrefWidth(450);
        text.setText("""
            WHAT IS IT?
            Backtracking is a recursive algorithm that tries to build a solution one piece at a time. 
            If it reaches a point where the current path cannot be completed, it "backtracks" to the 
            previous step and tries a different option.

            THE ALGORITHM:
            1. Find an empty cell.
            2. Try placing the number 1 in that cell.
            3. Check if the number is valid (no duplicates in the row, column, or 3x3 box).
            4. If valid, place the number and move to the next empty cell (recurse).
            5. If no number from 1-9 works, reset the cell to empty and backtrack to the previous cell.
            6. If the board is completely full, you have found the solution!

            COLOR LEGEND:
            - Blue numbers: Given clues (cannot be changed)
            - Orange numbers: Numbers placed by you or the solver
            - Yellow highlight: The solver is currently trying a number here
            - Red highlight: The solver tried a number here, but had to backtrack
            """);

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("btn", "btn-primary");
        closeBtn.setMaxWidth(200);
        closeBtn.setPrefHeight(40);
        closeBtn.setOnAction(e -> dialog.close());

        box.getChildren().addAll(title, text, closeBtn);

        Scene scene = new Scene(box, 520, 580);
        scene.getStylesheets().add(getClass().getResource("/sudoku.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}