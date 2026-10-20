package com.vbforge.snakebyte;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.EnumMap;
import java.util.Map;

public class SnakeByteApp extends Application {

    private static final int CELL_SIZE = 20;
    private static final int CANVAS_SIZE = SnakeEngine.GRID_SIZE * CELL_SIZE;

    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    private final SnakeEngine engine = new SnakeEngine();
    private AnimationTimer gameLoop;
    
    private Canvas canvas;
    private VBox overlay;
    private Label overlayTitle;
    private Label overlayMessage;
    private Button overlayBtn;
    
    private Label scoreLabel, highScoreLabel;
    private ComboBox<Integer> speedCombo;
    
    private int highScore = 0;
    private long lastTickTime = 0;
    private long currentTickRateNs = 120_000_000L; // Default: 120ms (Speed 3)
    
    private boolean isPaused = false;
    private boolean isStarted = false;

    // Keybindings
    private final Map<SnakeEngine.Direction, KeyCode> bindings = new EnumMap<>(SnakeEngine.Direction.class);

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        // Default keybindings
        bindings.put(SnakeEngine.Direction.UP, KeyCode.UP);
        bindings.put(SnakeEngine.Direction.DOWN, KeyCode.DOWN);
        bindings.put(SnakeEngine.Direction.LEFT, KeyCode.LEFT);
        bindings.put(SnakeEngine.Direction.RIGHT, KeyCode.RIGHT);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar());
        root.setCenter(buildMainLayout());
        root.setBottom(buildFooter());

        Scene scene = new Scene(root, 620, 720);
        scene.getStylesheets().add(getClass().getResource("/snakebyte.css").toExternalForm());

        // Global Key Listener
        scene.setOnKeyPressed(e -> {
            // Prevent Space from clicking focused buttons
            if (e.getCode() == KeyCode.SPACE) {
                e.consume();
                if (isStarted && !engine.isGameOver()) {
                    isPaused = !isPaused;
                    if (isPaused) {
                        overlayTitle.setText("Paused");
                        overlayTitle.setStyle("-fx-text-fill: #f89820;");
                        overlayMessage.setText("Press Space to resume");
                        overlayBtn.setVisible(false);
                        overlay.setVisible(true);
                    } else {
                        overlay.setVisible(false);
                    }
                }
                return;
            }

            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                if (!isStarted || engine.isGameOver()) {
                    startGame();
                }
                return;
            }

            if (!isStarted || isPaused || engine.isGameOver()) return;

            // Check custom keybindings
            for (Map.Entry<SnakeEngine.Direction, KeyCode> entry : bindings.entrySet()) {
                if (e.getCode() == entry.getValue()) {
                    engine.setDirection(entry.getKey());
                    break;
                }
            }
        });

        stage.setTitle("SnakeByte");
        stage.setScene(scene);
        stage.show();

        showStartScreen();
    }

    private Node buildTitleBar() {
        Label title = new Label("SnakeByte");
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

    private Node buildMainLayout() {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20, 24, 20, 24));

        // Header (Score & Speed)
        scoreLabel = new Label("0");
        scoreLabel.getStyleClass().add("score-value");

        highScoreLabel = new Label("0");
        highScoreLabel.getStyleClass().add("score-value");

        VBox scoreBox = createScoreBox("Score", scoreLabel);
        VBox highBox = createScoreBox("High Score", highScoreLabel);

        // Speed Selector
        Label speedLabel = new Label("SPEED");
        speedLabel.getStyleClass().add("score-label");
        speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll(1, 2, 3, 4, 5);
        speedCombo.getSelectionModel().select(3);
        speedCombo.setPrefWidth(80);
        speedCombo.getStyleClass().add("speed-combo");
        speedCombo.setFocusTraversable(false); // Prevent Space from opening dropdown
        speedCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) updateSpeed(newVal);
        });

        VBox speedBox = new VBox(4, speedLabel, speedCombo);
        speedBox.setAlignment(Pos.CENTER_RIGHT);

        // Controls Button
        Button controlsBtn = new Button("Controls");
        controlsBtn.getStyleClass().add("btn");
        controlsBtn.setFocusTraversable(false); // Prevent Space from clicking it
        controlsBtn.setOnAction(e -> showControlsDialog());

        VBox controlsBox = new VBox(4, new Label(" "), controlsBtn);

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        HBox header = new HBox(20, scoreBox, spacer1, speedBox, controlsBox, spacer2, highBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        // Canvas & Overlay
        canvas = new Canvas(CANVAS_SIZE, CANVAS_SIZE);
        StackPane gameArea = new StackPane(canvas);
        gameArea.getStyleClass().add("game-area");

        overlayTitle = new Label();
        overlayTitle.getStyleClass().add("game-over-title");
        overlayMessage = new Label();
        overlayMessage.getStyleClass().add("final-score-text");
        overlayBtn = new Button();
        overlayBtn.getStyleClass().add("btn-primary");
        overlayBtn.setFocusTraversable(false); // Crucial: prevents Spacebar from triggering it
        overlayBtn.setOnAction(e -> startGame());

        overlay = new VBox(15, overlayTitle, overlayMessage, overlayBtn);
        overlay.setAlignment(Pos.CENTER);
        overlay.getStyleClass().add("overlay");

        gameArea.getChildren().add(overlay);
        content.getChildren().addAll(header, gameArea);

        // Game Loop
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isStarted && !isPaused && !engine.isGameOver()) {
                    if (now - lastTickTime > currentTickRateNs) {
                        lastTickTime = now;
                        engine.tick();
                        render();
                        updateUI();
                    }
                }
            }
        };
        gameLoop.start();

        return content;
    }

    private VBox createScoreBox(String title, Label valueLabel) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("score-label");
        VBox box = new VBox(4, titleLabel, valueLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private Node buildFooter() {
        Label hint = new Label("Arrows / WASD to move. Space to pause. Enter to start/restart.");
        hint.getStyleClass().add("footer-hint");
        HBox footer = new HBox(hint);
        footer.setAlignment(Pos.CENTER);
        footer.getStyleClass().add("footer");
        return footer;
    }

    private void updateSpeed(int speedLevel) {
        currentTickRateNs = switch (speedLevel) {
            case 1 -> 300_000_000L;
            case 2 -> 200_000_000L;
            case 3 -> 120_000_000L;
            case 4 -> 80_000_000L;
            case 5 -> 40_000_000L;
            default -> 120_000_000L;
        };
    }

    private void showStartScreen() {
        isStarted = false;
        overlayTitle.setText("SnakeByte");
        overlayTitle.setStyle("-fx-text-fill: #f89820;");
        overlayMessage.setText("Select your speed and click Start");
        overlayBtn.setText("Start Game");
        overlayBtn.setVisible(true);
        overlay.setVisible(true);
        render();
    }

    private void startGame() {
        engine.reset();
        isStarted = true;
        isPaused = false;
        overlay.setVisible(false);
        lastTickTime = System.nanoTime();
        updateUI();
        render();
        // Ensure the root pane has focus, not the Start button
        primaryStage.getScene().getRoot().requestFocus();
    }

    private void updateUI() {
        scoreLabel.setText(String.valueOf(engine.getScore()));
        if (engine.getScore() > highScore) {
            highScore = engine.getScore();
            highScoreLabel.setText(String.valueOf(highScore));
        }
        
        if (engine.isGameOver()) {
            overlayTitle.setText("Game Over");
            overlayTitle.setStyle("-fx-text-fill: #f85149;");
            overlayMessage.setText("You scored " + engine.getScore());
            overlayBtn.setText("Play Again");
            overlayBtn.setVisible(true);
            overlay.setVisible(true);
        }
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        gc.setFill(Color.web("#1c2129"));
        gc.fillRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

        gc.setStroke(Color.web("#2a313c"));
        gc.setLineWidth(1);
        for (int i = 0; i <= CANVAS_SIZE; i += CELL_SIZE) {
            gc.strokeLine(i, 0, i, CANVAS_SIZE);
            gc.strokeLine(0, i, CANVAS_SIZE, i);
        }

        SnakeEngine.Point food = engine.getFood();
        gc.setFill(Color.web("#f85149"));
        gc.fillOval(food.x() * CELL_SIZE + 3, food.y() * CELL_SIZE + 3, CELL_SIZE - 6, CELL_SIZE - 6);

        var snake = engine.getSnake();
        for (int i = 0; i < snake.size(); i++) {
            SnakeEngine.Point p = snake.get(i);
            gc.setFill(i == 0 ? Color.web("#f89820") : Color.web("#3fb950"));
            gc.fillRect(p.x() * CELL_SIZE + 1, p.y() * CELL_SIZE + 1, CELL_SIZE - 2, CELL_SIZE - 2);
        }
    }

    private void showControlsDialog() {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));
        box.getStyleClass().add("modal-bg");
        
        Label title = new Label("Custom Key Bindings");
        title.getStyleClass().add("modal-title");
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        
        for (SnakeEngine.Direction dir : SnakeEngine.Direction.values()) {
            Label lbl = new Label(dir.name());
            lbl.getStyleClass().add("detail-label");
            lbl.setMinWidth(80);
            
            Button btn = new Button(bindings.get(dir).name());
            btn.getStyleClass().add("btn");
            btn.setMinWidth(120);
            
            btn.setOnAction(e -> {
                btn.setText("Press any key...");
                btn.requestFocus();
                btn.setOnKeyPressed(ke -> {
                    bindings.put(dir, ke.getCode());
                    btn.setText(ke.getCode().name());
                    btn.setOnKeyPressed(null);
                });
            });
            
            grid.add(lbl, 0, dir.ordinal());
            grid.add(btn, 1, dir.ordinal());
        }
        
        Button closeBtn = new Button("Done");
        closeBtn.getStyleClass().add("btn-primary");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());
        
        box.getChildren().addAll(title, grid, closeBtn);
        
        Scene scene = new Scene(box);
        scene.getStylesheets().add(getClass().getResource("/snakebyte.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}