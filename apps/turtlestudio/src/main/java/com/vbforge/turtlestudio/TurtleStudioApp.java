package com.vbforge.turtlestudio;

import com.vbforge.turtlestudio.engine.Command;
import com.vbforge.turtlestudio.engine.CommandParser;
import com.vbforge.turtlestudio.model.Turtle;
import com.vbforge.turtlestudio.ui.TurtleCanvas;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.embed.swing.SwingFXUtils;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;

public class TurtleStudioApp extends Application {

    private final Turtle turtle = new Turtle(TurtleCanvas.SIZE / 2.0, TurtleCanvas.SIZE / 2.0);
    private TurtleCanvas canvas;
    private Label statusLabel;
    private TextArea scriptArea;
    private Timeline scriptTimeline;

    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar(stage));
        root.setCenter(buildMainLayout());

        Scene scene = new Scene(root, 1150, 750);
        scene.getStylesheets().add(getClass().getResource("/turtlestudio.css").toExternalForm());

        stage.setTitle("TurtleStudio");
        stage.setScene(scene);
        stage.show();

        canvas.draw();
        updateStatus();
    }

    private Node buildTitleBar(Stage stage) {
        Label title = new Label("TurtleStudio");
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
        HBox main = new HBox();
        
        // Left Sidebar
        VBox leftSidebar = new VBox(16);
        leftSidebar.getStyleClass().add("sidebar");
        leftSidebar.setPrefWidth(260);

        Label moveTitle = new Label("MOVEMENT");
        moveTitle.getStyleClass().add("section-title");

        TextField stepField = new TextField("50");
        stepField.getStyleClass().add("input-field");

        GridPane btnGrid = new GridPane();
        btnGrid.setHgap(8); btnGrid.setVgap(8);
        
        Button fwdBtn = createButton("▲ Forward");
        Button backBtn = createButton("▼ Back");
        Button leftBtn = createButton("◀ Left 90°");
        Button rightBtn = createButton("▶ Right 90°");
        
        btnGrid.add(fwdBtn, 0, 0); btnGrid.add(backBtn, 1, 0);
        btnGrid.add(leftBtn, 0, 1); btnGrid.add(rightBtn, 1, 1);
        for (Node n : btnGrid.getChildren()) GridPane.setHgrow(n, Priority.ALWAYS);

        fwdBtn.setOnAction(e -> { turtle.move(parseStep(stepField)); canvas.draw(); updateStatus(); });
        backBtn.setOnAction(e -> { turtle.move(-parseStep(stepField)); canvas.draw(); updateStatus(); });
        leftBtn.setOnAction(e -> { turtle.turn(-90); canvas.draw(); updateStatus(); });
        rightBtn.setOnAction(e -> { turtle.turn(90); canvas.draw(); updateStatus(); });

        Label penTitle = new Label("PEN");
        penTitle.getStyleClass().add("section-title");

        Button penBtn = createButton("Pen Down");
        penBtn.setOnAction(e -> {
            turtle.setPenDown(!turtle.isPenDown());
            penBtn.setText(turtle.isPenDown() ? "Pen Down" : "Pen Up");
            updateStatus();
        });

        ColorPicker colorPicker = new ColorPicker(Color.web("#f89820"));
        colorPicker.setMaxWidth(Double.MAX_VALUE);
        colorPicker.setOnAction(e -> turtle.setColor(colorPicker.getValue()));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button clearBtn = createButton("Clear Canvas");
        clearBtn.getStyleClass().add("btn-danger");
        clearBtn.setOnAction(e -> {
            if (scriptTimeline != null) scriptTimeline.stop();
            turtle.reset();
            colorPicker.setValue(Color.web("#f89820"));
            penBtn.setText("Pen Down");
            canvas.draw();
            updateStatus();
        });

        leftSidebar.getChildren().addAll(
            moveTitle, stepField, btnGrid, penTitle, penBtn, colorPicker, spacer, clearBtn
        );

        // Center Canvas
        StackPane centerPane = new StackPane();
        centerPane.setStyle("-fx-background-color: #0d1117;");
        canvas = new TurtleCanvas(turtle);
        centerPane.getChildren().add(canvas);

        // Right Sidebar
        VBox rightSidebar = new VBox(16);
        rightSidebar.getStyleClass().addAll("sidebar", "sidebar-right");
        rightSidebar.setPrefWidth(260);

        Label scriptTitle = new Label("SCRIPT EDITOR");
        scriptTitle.getStyleClass().add("section-title");

        scriptArea = new TextArea();
        scriptArea.getStyleClass().add("script-area");
        scriptArea.setText("FORWARD 100\nRIGHT 90\nFORWARD 100\nRIGHT 90\nFORWARD 100\nRIGHT 90\nFORWARD 100");
        VBox.setVgrow(scriptArea, Priority.ALWAYS);

        Button runBtn = createButton("▶ Run Script");
        runBtn.getStyleClass().add("btn-primary");
        runBtn.setOnAction(e -> runScript());

        Button saveBtn = createButton("💾 Save Image");
        saveBtn.setOnAction(e -> saveImage());

        rightSidebar.getChildren().addAll(scriptTitle, scriptArea, runBtn, saveBtn);

        main.getChildren().addAll(leftSidebar, centerPane, rightSidebar);
        HBox.setHgrow(centerPane, Priority.ALWAYS);

        // Status Bar
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        HBox statusBar = new HBox(statusLabel);
        statusBar.getStyleClass().add("status-bar");

        BorderPane layout = new BorderPane();
        layout.setCenter(main);
        layout.setBottom(statusBar);
        return layout;
    }

    private Button createButton(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("btn");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private double parseStep(TextField field) {
        try { return Double.parseDouble(field.getText()); }
        catch (Exception e) { return 50; }
    }

    private void updateStatus() {
        statusLabel.setText(String.format("X: %.0f | Y: %.0f | Angle: %.0f° | Pen: %s",
                turtle.getX(), turtle.getY(), turtle.getAngle(), turtle.isPenDown() ? "Down" : "Up"));
    }

    private void runScript() {
        if (scriptTimeline != null) scriptTimeline.stop();
        turtle.reset();
        canvas.draw();

        List<Command> commands;
        try {
            commands = CommandParser.parse(scriptArea.getText());
        } catch (IllegalArgumentException e) {
            Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage());
            a.show();
            return;
        }

        scriptTimeline = new Timeline();
        scriptTimeline.setCycleCount(1);

        double timeMs = 0;
        for (Command cmd : commands) {
            timeMs += 200;
            KeyFrame kf = new KeyFrame(Duration.millis(timeMs), e -> executeCommand(cmd));
            scriptTimeline.getKeyFrames().add(kf);
        }
        scriptTimeline.play();
    }

    private void executeCommand(Command cmd) {
        switch (cmd.type()) {
            case FORWARD -> turtle.move(cmd.value());
            case BACK -> turtle.move(-cmd.value());
            case LEFT -> turtle.turn(-cmd.value());
            case RIGHT -> turtle.turn(cmd.value());
            case PEN_UP -> turtle.setPenDown(false);
            case PEN_DOWN -> turtle.setPenDown(true);
            case CLEAR -> turtle.reset();
        }
        canvas.draw();
        updateStatus();
    }

    private void saveImage() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
        fc.setInitialFileName("drawing.png");
        File file = fc.showSaveDialog(null);
        
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null), "png", file);
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Image saved successfully!");
                a.show();
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Failed to save image: " + e.getMessage());
                a.show();
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}