package com.vbforge.squarecraft;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class SquareCraftApp extends Application {

    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    // Solver UI
    private TextField[][] solverInputs = new TextField[3][3];
    private TextArea solverVerification;

    // Generator UI
    private Label[][] genLabels = new Label[3][3];
    private TextField genStartField;
    private TextArea genVerification;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildTitleBar(stage));
        root.setCenter(buildTabs());

        // Make the window bigger to fit everything comfortably
        Scene scene = new Scene(root, 900, 850);
        scene.getStylesheets().add(getClass().getResource("/squarecraft.css").toExternalForm());

        stage.setTitle("SquareCraft");
        stage.setScene(scene);
        stage.show();
    }

    private Node buildTitleBar(Stage stage) {
        Label title = new Label("SquareCraft");
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

    private Node buildTabs() {
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("custom-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab solverTab = new Tab("Solver", buildSolverView());
        Tab genTab = new Tab("Generator", buildGeneratorView());

        tabs.getTabs().addAll(solverTab, genTab);
        return tabs;
    }

    // --- SOLVER VIEW ---

    private Node buildSolverView() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                TextField tf = new TextField();
                tf.getStyleClass().add("grid-input");
                tf.setPrefSize(110, 110); // Bigger cell size
                tf.setAlignment(Pos.CENTER);
                tf.setTextFormatter(new TextFormatter<>(change -> 
                    change.getControlNewText().matches("-?\\d{0,5}") ? change : null));
                solverInputs[r][c] = tf;
                grid.add(tf, c, r);
            }
        }

        HBox controls = new HBox(12,
                createButton("Ex 1", () -> loadExample(1)),
                createButton("Ex 2", () -> loadExample(2)),
                createButton("Clear", this::clearSolver),
                createButton("Help", () -> showHelp("solver"))
        );
        controls.setAlignment(Pos.CENTER);

        Button solveBtn = new Button("Solve");
        solveBtn.getStyleClass().add("btn-primary");
        solveBtn.setOnAction(e -> solveMagicSquare());

        Button verifyBtn = new Button("Toggle Verification");
        verifyBtn.getStyleClass().add("btn");
        verifyBtn.setOnAction(e -> toggleVisibility(solverVerification));

        HBox actions = new HBox(12, verifyBtn, solveBtn);
        actions.setAlignment(Pos.CENTER);

        solverVerification = new TextArea();
        solverVerification.setEditable(false);
        solverVerification.getStyleClass().add("verification-area");
        solverVerification.setVisible(false);
        solverVerification.setManaged(false);

        content.getChildren().addAll(grid, controls, actions, solverVerification);
        return content;
    }

    private void solveMagicSquare() {
        Integer[][] input = new Integer[3][3];
        try {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    String text = solverInputs[r][c].getText().trim();
                    input[r][c] = text.isEmpty() ? null : Integer.parseInt(text);
                }
            }
        } catch (NumberFormatException e) {
            solverVerification.setText("Error: Invalid number format.");
            solverVerification.setVisible(true);
            solverVerification.setManaged(true);
            return;
        }

        var result = MagicSquareEngine.solve(input);
        
        if (result.success()) {
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    solverInputs[r][c].setText(String.valueOf(result.grid()[r][c]));
                    solverInputs[r][c].setStyle("-fx-text-fill: #3fb950;"); // Highlight solved
                }
            }
            solverVerification.setText(result.verificationText());
        } else {
            solverVerification.setText(result.verificationText());
        }
        solverVerification.setVisible(true);
        solverVerification.setManaged(true);
    }

    private void loadExample(int num) {
        clearSolver();
        if (num == 1) {
            solverInputs[0][0].setText("80");
            solverInputs[1][0].setText("180");
            solverInputs[1][1].setText("100");
            solverInputs[2][2].setText("120");
        } else {
            solverInputs[0][2].setText("250");
            solverInputs[1][1].setText("210");
            solverInputs[1][2].setText("50");
            solverInputs[2][0].setText("170");
        }
    }

    private void clearSolver() {
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                solverInputs[r][c].clear();
                solverInputs[r][c].setStyle("-fx-text-fill: #f89820;");
            }
        }
        solverVerification.setVisible(false);
        solverVerification.setManaged(false);
    }

    // --- GENERATOR VIEW ---

    private Node buildGeneratorView() {
        VBox content = new VBox(20);
        content.setAlignment(Pos.TOP_CENTER);
        content.setPadding(new Insets(20));

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                Label lbl = new Label();
                lbl.getStyleClass().add("grid-label");
                lbl.setPrefSize(110, 110); // Bigger cell size
                lbl.setAlignment(Pos.CENTER);
                genLabels[r][c] = lbl;
                grid.add(lbl, c, r);
            }
        }

        genStartField = new TextField("1");
        genStartField.setPrefWidth(80);
        genStartField.getStyleClass().add("start-input");
        genStartField.setTextFormatter(new TextFormatter<>(change -> 
            change.getControlNewText().matches("-?\\d{0,5}") ? change : null));

        HBox controls = new HBox(12,
                new Label("Start Number:"),
                genStartField,
                createButton("Generate", this::generateSquare),
                createButton("Help", () -> showHelp("generator"))
        );
        controls.setAlignment(Pos.CENTER);
        controls.getChildren().get(0).setStyle("-fx-text-fill: #8b949e;");

        Button verifyBtn = new Button("Toggle Verification");
        verifyBtn.getStyleClass().add("btn");
        verifyBtn.setOnAction(e -> toggleVisibility(genVerification));
        HBox actions = new HBox(verifyBtn);
        actions.setAlignment(Pos.CENTER);

        genVerification = new TextArea();
        genVerification.setEditable(false);
        genVerification.getStyleClass().add("verification-area");
        genVerification.setVisible(false);
        genVerification.setManaged(false);

        content.getChildren().addAll(grid, controls, actions, genVerification);
        
        generateSquare(); // Generate default 1-9 on load
        return content;
    }

    private void generateSquare() {
        try {
            int start = Integer.parseInt(genStartField.getText().trim());
            int[][] grid = MagicSquareEngine.generate(start);
            
            Integer[][] boxed = new Integer[3][3];
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) {
                    genLabels[r][c].setText(String.valueOf(grid[r][c]));
                    boxed[r][c] = grid[r][c];
                }
            }
            var result = MagicSquareEngine.solve(boxed);
            genVerification.setText(result.verificationText());
        } catch (NumberFormatException e) {
            genVerification.setText("Error: Invalid starting number.");
            genVerification.setVisible(true);
            genVerification.setManaged(true);
        }
    }

    // --- UTILS ---

    private Button createButton(String text, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("btn");
        b.setOnAction(e -> action.run());
        return b;
    }

    private void toggleVisibility(Node node) {
        node.setVisible(!node.isVisible());
        node.setManaged(node.isVisible());
    }

    private void showHelp(String mode) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox box = new VBox(15);
        box.setPadding(new Insets(25));
        box.getStyleClass().add("modal-bg");

        Label title = new Label(mode.equals("solver") ? "Solving Magic Squares" : "Generating Magic Squares");
        title.getStyleClass().add("modal-title");

        String solverText =
                "To solve a magic square, we first need to find the 'Magic Constant'. " +
                        "A magic square is a grid where every row, column, and diagonal sum to the exact same number.\n\n" +
                        "1. Look for any complete line (a row, column, or diagonal with 3 filled numbers) and add them together. " +
                        "This sum is your Magic Constant.\n\n" +
                        "2. Next, look for any line that has exactly 2 filled cells and 1 empty cell. " +
                        "Subtract the sum of the 2 known cells from the Magic Constant to find the missing number.\n\n" +
                        "3. Repeat this process until the grid is completely filled!";

        String generatorText =
                "The Generator uses the 'Siam Method' to create magic squares.\n\n" +
                        "1. Place the starting number in the top row, middle column.\n\n" +
                        "2. Move up one row and right one column to place the next number.\n\n" +
                        "3. If you go off the top edge, wrap around to the bottom row. If you go off the right edge, wrap to the left column.\n\n" +
                        "4. If the target cell is already occupied, place the next number directly below the previous number instead.\n\n" +
                        "5. Repeat until all 9 numbers are placed!";

        Label content = new Label(mode.equals("solver") ? solverText : generatorText);
        content.setWrapText(true);
        content.setMaxWidth(550); // Force wrapping before hitting the edge
        content.setStyle("-fx-text-fill: #e6edf3; -fx-font-size: 14px; -fx-line-spacing: 4px;");

        Button close = new Button("Close");
        close.getStyleClass().add("btn-primary");
        close.setMaxWidth(Double.MAX_VALUE);
        close.setOnAction(e -> dialog.close());

        box.getChildren().addAll(title, content, close);

        // INCREASED SIZE: 600x500 gives plenty of room for the text.
        Scene scene = new Scene(box, 600, 500);
        scene.getStylesheets().add(getClass().getResource("/squarecraft.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}