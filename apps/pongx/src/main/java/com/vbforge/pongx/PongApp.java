package com.vbforge.pongx;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class PongApp extends Application {

    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    private final PongEngine engine = new PongEngine();
    private AnimationTimer gameLoop;

    private Canvas canvas;
    private BorderPane root;
    private VBox startOverlay, pauseOverlay, gameOverOverlay;
    private Label winnerLabel;

    private boolean isRunning = false;
    private boolean isPaused = false;
    private boolean isAiMode = false;
    private boolean isAiEasy = false;

    private final boolean[] keys = new boolean[256];

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar());
        root.setCenter(buildGameArea());

        Scene scene = new Scene(root, PongEngine.GAME_WIDTH, PongEngine.GAME_HEIGHT + 40);
        scene.getStylesheets().add(getClass().getResource("/pongx.css").toExternalForm());

        // Key Bindings
        scene.setOnKeyPressed(e -> {
            if (e.getCode().ordinal() < 256) keys[e.getCode().ordinal()] = true;
            
            if (e.getCode() == KeyCode.SPACE && isRunning) {
                e.consume(); // Prevents the spacebar from clicking any focused buttons
                isPaused = !isPaused;
                pauseOverlay.setVisible(isPaused);
            }
        });
        
        scene.setOnKeyReleased(e -> {
            if (e.getCode().ordinal() < 256) keys[e.getCode().ordinal()] = false;
        });

        stage.setTitle("PongX");
        stage.setScene(scene);
        stage.show();

        // Start Loop
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (isRunning && !isPaused && !engine.isGameOver) {
                    updatePhysics();
                    render();
                    
                    if (engine.isGameOver) {
                        endGame();
                    }
                }
            }
        };
        gameLoop.start();
    }

    private Node buildTitleBar() {
        Label title = new Label("PongX");
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

    private StackPane buildGameArea() {
        canvas = new Canvas(PongEngine.GAME_WIDTH, PongEngine.GAME_HEIGHT);
        StackPane gameArea = new StackPane(canvas);
        gameArea.setStyle("-fx-background-color: black;");

        // Start Screen
        Button pvpBtn = new Button("Player vs Player");
        pvpBtn.getStyleClass().add("btn");
        pvpBtn.setFocusTraversable(false); // Prevents Spacebar focus bug
        pvpBtn.setOnAction(e -> startGame(false, false));

        Button aiBtn = new Button("Player vs AI (Hard)");
        aiBtn.getStyleClass().add("btn");
        aiBtn.setFocusTraversable(false);
        aiBtn.setOnAction(e -> startGame(true, false));

        Button aiEasyBtn = new Button("Player vs AI (Easy)");
        aiEasyBtn.getStyleClass().add("btn");
        aiEasyBtn.setFocusTraversable(false);
        aiEasyBtn.setOnAction(e -> startGame(true, true));

        Label startTitle = new Label("PongX");
        startTitle.getStyleClass().add("overlay-title");
        Label startSub = new Label("First to " + PongEngine.WINNING_SCORE + " points wins");
        startSub.getStyleClass().add("overlay-sub");

        // Controls Legend
        Label p1Control = new Label("Player 1: W / S");
        p1Control.getStyleClass().add("control-key");
        Label p2Control = new Label("Player 2: ↑ / ↓");
        p2Control.getStyleClass().add("control-key");
        HBox controlLegend = new HBox(20, p1Control, p2Control);
        controlLegend.setAlignment(Pos.CENTER);

        startOverlay = new VBox(15, startTitle, startSub, pvpBtn, aiBtn, aiEasyBtn, controlLegend);
        startOverlay.setAlignment(Pos.CENTER);
        startOverlay.getStyleClass().add("overlay");

        // Pause Screen
        Label pauseTitle = new Label("PAUSED");
        pauseTitle.getStyleClass().add("overlay-title");
        Label pauseSub = new Label("Press SPACE to resume");
        pauseSub.getStyleClass().add("overlay-sub");
        
        pauseOverlay = new VBox(10, pauseTitle, pauseSub);
        pauseOverlay.setAlignment(Pos.CENTER);
        pauseOverlay.getStyleClass().add("overlay");
        pauseOverlay.setVisible(false);

        // Game Over Screen
        winnerLabel = new Label("Player 1 Wins!");
        winnerLabel.getStyleClass().add("overlay-title");
        
        Button restartBtn = new Button("Play Again");
        restartBtn.getStyleClass().add("btn-primary");
        restartBtn.setFocusTraversable(false);
        restartBtn.setOnAction(e -> resetToMenu());

        gameOverOverlay = new VBox(20, winnerLabel, restartBtn);
        gameOverOverlay.setAlignment(Pos.CENTER);
        gameOverOverlay.getStyleClass().add("overlay");
        gameOverOverlay.setVisible(false);

        gameArea.getChildren().addAll(startOverlay, pauseOverlay, gameOverOverlay);
        return gameArea;
    }

    private void startGame(boolean aiMode, boolean easyAi) {
        this.isAiMode = aiMode;
        this.isAiEasy = easyAi;
        
        engine.reset();
        isRunning = true;
        isPaused = false;
        
        startOverlay.setVisible(false);
        pauseOverlay.setVisible(false);
        gameOverOverlay.setVisible(false);
        
        // FIX: Shift focus away from the buttons to the root window
        root.requestFocus();
        
        render();
    }

    private void resetToMenu() {
        isRunning = false;
        engine.reset();
        gameOverOverlay.setVisible(false);
        startOverlay.setVisible(true);
        render();
    }

    private void endGame() {
        isRunning = false;
        if (engine.p1Score > engine.p2Score) {
            winnerLabel.setText("Player 1 Wins!");
            winnerLabel.setStyle("-fx-text-fill: #6ea8d8;");
        } else {
            winnerLabel.setText(isAiMode ? "AI Wins!" : "Player 2 Wins!");
            winnerLabel.setStyle("-fx-text-fill: #f85149;");
        }
        gameOverOverlay.setVisible(true);
    }

    private void updatePhysics() {
        // P1 Movement (W / S)
        double p1Speed = 8.0;
        if (keys[KeyCode.W.ordinal()]) engine.moveP1(-p1Speed);
        if (keys[KeyCode.S.ordinal()]) engine.moveP1(p1Speed);

        // P2 Movement
        if (isAiMode) {
            double aiSpeed = isAiEasy ? 4.0 : 6.5; 
            engine.moveAi(aiSpeed);
        } else {
            double p2Speed = 8.0;
            if (keys[KeyCode.UP.ordinal()]) engine.moveP2(-p2Speed);
            if (keys[KeyCode.DOWN.ordinal()]) engine.moveP2(p2Speed);
        }

        engine.update();
    }

    private void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        // Clear background
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, PongEngine.GAME_WIDTH, PongEngine.GAME_HEIGHT);

        // Center dashed line
        gc.setStroke(Color.web("#2a313c"));
        gc.setLineWidth(4);
        gc.setLineDashes(20, 20);
        gc.strokeLine(PongEngine.GAME_WIDTH / 2.0, 0, PongEngine.GAME_WIDTH / 2.0, PongEngine.GAME_HEIGHT);
        gc.setLineDashes(null); // Reset

        // Scores
        gc.setFill(Color.web("#1c2129"));
        gc.setFont(Font.font("monospace", FontWeight.BOLD, 80));
        gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
        gc.fillText(String.valueOf(engine.p1Score), PongEngine.GAME_WIDTH / 2.0 - 80, 90);
        gc.fillText(String.valueOf(engine.p2Score), PongEngine.GAME_WIDTH / 2.0 + 80, 90);

        // P1 Paddle (Blue)
        gc.setFill(Color.web("#6ea8d8"));
        gc.setEffect(new javafx.scene.effect.DropShadow(15, Color.web("#6ea8d8", 0.5)));
        gc.fillRect(0, engine.p1Y, PongEngine.PADDLE_WIDTH, PongEngine.PADDLE_HEIGHT);

        // P2 Paddle (Red)
        gc.setFill(Color.web("#f85149"));
        gc.setEffect(new javafx.scene.effect.DropShadow(15, Color.web("#f85149", 0.5)));
        gc.fillRect(PongEngine.GAME_WIDTH - PongEngine.PADDLE_WIDTH, engine.p2Y, PongEngine.PADDLE_WIDTH, PongEngine.PADDLE_HEIGHT);

        // Ball (Orange)
        gc.setFill(Color.web("#f89820"));
        gc.setEffect(new javafx.scene.effect.DropShadow(20, Color.web("#f89820", 0.8)));
        gc.fillOval(engine.ballX - PongEngine.BALL_RADIUS, engine.ballY - PongEngine.BALL_RADIUS, 
                    PongEngine.BALL_RADIUS * 2, PongEngine.BALL_RADIUS * 2);
        
        // Reset effect
        gc.setEffect(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}