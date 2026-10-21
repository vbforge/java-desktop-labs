package com.vbforge.rectsolve;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Locale;

public class RectSolveApp extends Application {

    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    private TextField fieldA, fieldB, fieldC, fieldD;
    private Label statusLabel;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar());
        root.setCenter(buildMainContent());

        Scene scene = new Scene(root, 600, 780);
        scene.getStylesheets().add(getClass().getResource("/rectsolve.css").toExternalForm());

        stage.setTitle("RectSolve");
        stage.setScene(scene);
        stage.show();
    }

    private Node buildTitleBar() {
        Label title = new Label("RectSolve");
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
        content.setPadding(new Insets(24));
        content.setAlignment(Pos.TOP_CENTER); // Central orientation

        // Header
        Label title = new Label("RECTANGLE AREA SOLVER");
        title.getStyleClass().add("main-title");
        Label subtitle = new Label("Find the missing piece area (x) of a 4-way split rectangle");
        subtitle.getStyleClass().add("subtitle");
        VBox header = new VBox(4, title, subtitle);
        header.setAlignment(Pos.CENTER);

        // Instructions
        Label instructions = new Label("Leave ONE field empty (or type x) for the unknown piece.\nFill all four to verify consistency.");
        instructions.getStyleClass().add("instructions");
        instructions.setWrapText(true);
        instructions.setAlignment(Pos.CENTER);
        instructions.setTextAlignment(TextAlignment.CENTER);

        // Grid
        GridPane grid = new GridPane();
        grid.getStyleClass().add("grid");
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setAlignment(Pos.CENTER);
        
        fieldA = makeField();
        fieldB = makeField();
        fieldC = makeField();
        fieldD = makeField();

        grid.add(createCell("a (top-left)", fieldA), 0, 0);
        grid.add(createCell("b (top-right)", fieldB), 1, 0);
        grid.add(createCell("c (bottom-left)", fieldC), 0, 1);
        grid.add(createCell("d (bottom-right)", fieldD), 1, 1);

        // Formula
        Label formula = new Label("a × d = b × c  (diagonal products)");
        formula.getStyleClass().add("formula");
        formula.setAlignment(Pos.CENTER);

        // Buttons
        Button calcBtn = new Button("Calculate");
        calcBtn.getStyleClass().addAll("btn", "btn-primary");
        calcBtn.setOnAction(e -> onCalculate());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn");
        clearBtn.setOnAction(e -> onClear());

        Button exampleBtn = new Button("Example");
        exampleBtn.getStyleClass().add("btn");
        exampleBtn.setOnAction(e -> onLoadExample());

        Button helpBtn = new Button("Help");
        helpBtn.getStyleClass().add("btn");
        helpBtn.setOnAction(e -> showHelp());

        HBox btnRow = new HBox(10, calcBtn, clearBtn, exampleBtn, helpBtn);
        btnRow.setAlignment(Pos.CENTER);
        HBox.setHgrow(calcBtn, Priority.ALWAYS);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        HBox.setHgrow(exampleBtn, Priority.ALWAYS);
        HBox.setHgrow(helpBtn, Priority.ALWAYS);
        btnRow.getChildren().forEach(n -> ((Button)n).setMaxWidth(Double.MAX_VALUE));

        // Result Area with Label
        Label resultTitle = new Label("RESULT");
        resultTitle.getStyleClass().add("cell-label");
        
        statusLabel = new Label(" ");
        statusLabel.getStyleClass().add("status-box");
        statusLabel.setAlignment(Pos.CENTER);
        
        VBox resultBox = new VBox(6, resultTitle, statusLabel);
        resultBox.setAlignment(Pos.CENTER);

        content.getChildren().addAll(header, instructions, grid, formula, new Region(), btnRow, resultBox);
        VBox.setVgrow(content.getChildren().get(5), Priority.ALWAYS);

        return content;
    }

    private TextField makeField() {
        TextField tf = new TextField();
        tf.getStyleClass().add("input-field");
        tf.setAlignment(Pos.CENTER);
        return tf;
    }

    private VBox createCell(String labelText, TextField field) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("cell-label");
        VBox cell = new VBox(8, lbl, field);
        cell.getStyleClass().add("cell");
        return cell;
    }

    // --- Logic ---

    private void onCalculate() {
        try {
            Double a = parseField(fieldA);
            Double b = parseField(fieldB);
            Double c = parseField(fieldC);
            Double d = parseField(fieldD);

            int unknownCount = (a == null ? 1 : 0) + (b == null ? 1 : 0) + (c == null ? 1 : 0) + (d == null ? 1 : 0);

            if (unknownCount == 0) {
                var result = RectangleSolver.verify(a, b, c, d);
                String msg = String.format(Locale.US, "(a×d = %s, b×c = %s)", trimZeros(result.leftProduct()), trimZeros(result.rightProduct()));
                if (result.consistent()) {
                    setStatus("✓ consistent — these 4 areas form a valid rectangle split " + msg, "-fx-text-fill: #3fb950;");
                } else {
                    setStatus("✗ NOT consistent " + msg, "-fx-text-fill: #f85149;");
                }
                return;
            }

            double result = RectangleSolver.solve(a, b, c, d);
            String formatted = formatResult(result);

            TextField target = a == null ? fieldA : b == null ? fieldB : c == null ? fieldC : fieldD;
            target.setText(formatted);
            target.setStyle("-fx-text-fill: #f89820;"); 

            setStatus("x = " + formatted, "-fx-text-fill: #f89820;");
        } catch (IllegalArgumentException | ArithmeticException ex) {
            setStatus(ex.getMessage(), "-fx-text-fill: #f85149;");
        }
    }

    private void onClear() {
        for (TextField f : new TextField[]{fieldA, fieldB, fieldC, fieldD}) {
            f.clear();
            f.setStyle("-fx-text-fill: #e6edf3;");
        }
        setStatus(" ", "-fx-text-fill: #8b949e;");
    }

    private void onLoadExample() {
        onClear();
        fieldA.setText("8");
        fieldB.setText("6");
        fieldC.setText("12");
        fieldD.setText("x");
        setStatus("Example loaded — press Calculate (expected x = 9)", "-fx-text-fill: #8b949e;");
    }

    private Double parseField(TextField tf) {
        String text = tf.getText().trim();
        if (text.isEmpty() || text.equalsIgnoreCase("x")) return null;
        return Double.parseDouble(text);
    }

    private String formatResult(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) return String.format(Locale.US, "%.0f", value);
        return String.format(Locale.US, "%.4f", value);
    }

    private String trimZeros(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) return String.format(Locale.US, "%.0f", value);
        return String.format(Locale.US, "%.2f", value);
    }

    private void setStatus(String text, String style) {
        statusLabel.setText(text);
        statusLabel.setStyle(style);
    }

    private void showHelp() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);

        VBox box = new VBox(16);
        box.setPadding(new Insets(24));
        box.getStyleClass().add("modal-bg");

        Label title = new Label("How it works");
        title.getStyleClass().add("modal-title");

        Label text1 = new Label("When you draw one horizontal line and one vertical line through a rectangle, you create four smaller rectangles.");
        text1.setWrapText(true);
        text1.getStyleClass().add("modal-text");

        Label text2 = new Label("A magical mathematical property occurs: the product of the opposite diagonal areas are always equal.");
        text2.setWrapText(true);
        text2.getStyleClass().add("modal-text");

        Label diagram = new Label("+---------+---------+\n|    a    |    b    |\n+---------+---------+\n|    c    |    d    |\n+---------+---------+");
        diagram.getStyleClass().add("modal-diagram");
        diagram.setAlignment(Pos.CENTER);

        Label formula = new Label("a × d = b × c");
        formula.getStyleClass().add("modal-formula");
        formula.setAlignment(Pos.CENTER);

        Label text3 = new Label("If you know 3 of the areas, we can use this formula to solve for the 4th (x). If you know all 4, we can check if they form a real rectangle.");
        text3.setWrapText(true);
        text3.getStyleClass().add("modal-text");

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().addAll("btn", "btn-primary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        box.getChildren().addAll(title, text1, text2, diagram, formula, text3, closeBtn);

        Scene scene = new Scene(box, 550, 650); // Increased size
        scene.getStylesheets().add(getClass().getResource("/rectsolve.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}