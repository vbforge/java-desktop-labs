package com.vbforge.calcpad;

import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class CalcPadApp extends Application {

    private final CalcEngine engine = new CalcEngine();
    private Label displayNumber;
    private Label displayExpression;
    
    // For dragging the custom window
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        // 1. Remove the OS title bar
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        
        // 2. Build the custom dark title bar
        root.setTop(buildTitleBar(stage));
        root.setCenter(buildMainContent());

        // Give the window a fixed starting size
        Scene scene = new Scene(root, 360, 580);
        scene.getStylesheets().add(
                CalcPadApp.class.getResource("/calcpad.css").toExternalForm());

        // Keyboard Support
        scene.setOnKeyPressed(e -> {
            String text = e.getText();
            if (e.getCode().isDigitKey() || text.equals(".")) {
                engine.inputDigit(text);
            } else if (text.equals("+") || text.equals("-") || text.equals("*") || text.equals("/")) {
                engine.inputOperator(text);
            } else if (e.getCode() == KeyCode.ENTER || text.equals("=")) {
                engine.calculate();
            } else if (e.getCode() == KeyCode.BACK_SPACE) {
                engine.backspace();
            } else if (e.getCode() == KeyCode.ESCAPE || text.equalsIgnoreCase("c")) {
                engine.clear();
            }
            updateUI();
        });

        stage.setScene(scene);
        stage.show();
    }

    private Node buildTitleBar(Stage stage) {
        Label title = new Label("CalcPad");
        title.getStyleClass().add("title-text");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("close-btn");
        closeBtn.setOnAction(e -> stage.close());

        HBox titleBar = new HBox(title, spacer, closeBtn);
        titleBar.setAlignment(Pos.CENTER_LEFT);
        titleBar.getStyleClass().add("title-bar");

        // Enable dragging the window by the title bar
        titleBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        titleBar.setOnMouseDragged(event -> {
            stage.setX(event.getScreenX() - xOffset);
            stage.setY(event.getScreenY() - yOffset);
        });

        return titleBar;
    }

    private Node buildMainContent() {
        VBox content = new VBox();
        content.getChildren().addAll(buildDisplay(), buildGrid());
        return content;
    }

    private Node buildDisplay() {
        displayExpression = new Label();
        displayExpression.getStyleClass().add("display-expression");

        displayNumber = new Label("0");
        displayNumber.getStyleClass().add("display-number");

        VBox display = new VBox(8, displayExpression, displayNumber);
        display.setAlignment(Pos.CENTER_RIGHT);
        display.getStyleClass().add("display-area");
        return display;
    }

    private Node buildGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("grid");

        // Row 0
        addButton(grid, "C",  0, 0, "clear");
        addButton(grid, "←",  1, 0);
        addButton(grid, "%",  2, 0, "op");
        addButton(grid, "/",  3, 0, "op");

        // Row 1
        addButton(grid, "7", 0, 1);
        addButton(grid, "8", 1, 1);
        addButton(grid, "9", 2, 1);
        addButton(grid, "*", 3, 1, "op");

        // Row 2
        addButton(grid, "4", 0, 2);
        addButton(grid, "5", 1, 2);
        addButton(grid, "6", 2, 2);
        addButton(grid, "-", 3, 2, "op");

        // Row 3
        addButton(grid, "1", 0, 3);
        addButton(grid, "2", 1, 3);
        addButton(grid, "3", 2, 3);
        addButton(grid, "+", 3, 3, "op");

        // Row 4
        Button zero = addButton(grid, "0", 0, 4);
        GridPane.setColumnSpan(zero, 2); // "0" spans two columns
        
        addButton(grid, ".", 2, 4);
        addButton(grid, "=", 3, 4, "op", "equals");

        // Make columns and rows fill the space equally
        for (int i = 0; i < 4; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            grid.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < 5; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            grid.getRowConstraints().add(rc);
        }

        VBox.setVgrow(grid, Priority.ALWAYS);
        return grid;
    }

    private Button addButton(GridPane grid, String label, int col, int row, String... extraClasses) {
        Button btn = new Button(label);
        btn.getStyleClass().add("btn");
        btn.getStyleClass().addAll(extraClasses);
        btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        btn.setOnAction(e -> {
            if ("0123456789.".contains(label)) {
                engine.inputDigit(label);
            } else if ("+-*/%".contains(label)) {
                engine.inputOperator(label);
            } else if (label.equals("=")) {
                engine.calculate();
            } else if (label.equals("C")) {
                engine.clear();
            } else if (label.equals("←")) {
                engine.backspace();
            }
            updateUI();
        });

        grid.add(btn, col, row);
        return btn;
    }

    private void updateUI() {
        displayNumber.setText(engine.getDisplayText());
        displayExpression.setText(engine.getExpressionText());
    }

    public static void main(String[] args) {
        launch(args);
    }
}