package com.vbforge.clickrush;

import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.function.DoubleConsumer;

public class SpeedGame extends VBox {
    private int durationSecs = 5;
    private int clicks = 0;
    private long endTime = 0;
    private boolean running = false;

    private final AnimationTimer timer;
    private final DoubleConsumer onNewHighScore;

    private final Label timerLabel = new Label();
    private final Label cpsLabel = new Label();
    private final Label cpsValue = new Label("0.0");
    private final Button startBtn = new Button("Start");
    private final StackPane target = new StackPane();

    public SpeedGame(DoubleConsumer onNewHighScore) {
        this.onNewHighScore = onNewHighScore;
        setAlignment(Pos.CENTER);
        setSpacing(30);

        // Header
        timerLabel.getStyleClass().add("speed-timer");
        cpsLabel.getStyleClass().add("speed-cps");
        cpsValue.getStyleClass().add("speed-cps-value");
        cpsLabel.setText("Current: ");
        
        HBox cpsBox = new HBox(cpsLabel, cpsValue);
        cpsBox.setAlignment(Pos.CENTER);
        cpsLabel.setTextFill(javafx.scene.paint.Color.web("#8b949e"));
        cpsLabel.setStyle("-fx-font-size: 16px;");
        
        VBox header = new VBox(10, timerLabel, cpsBox);
        header.setAlignment(Pos.CENTER);

        // Target Circle
        Label targetText = new Label("Click Start");
        targetText.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 20px; -fx-font-weight: bold;");
        target.getChildren().add(targetText);
        target.getStyleClass().add("click-target");
        target.getStyleClass().add("disabled");

        // Controls
        Button btn5 = new Button("5s");
        Button btn10 = new Button("10s");
        Button btn30 = new Button("30s");
        
        btn5.setOnAction(e -> { if(!running) { durationSecs = 5; reset(); }});
        btn10.setOnAction(e -> { if(!running) { durationSecs = 10; reset(); }});
        btn30.setOnAction(e -> { if(!running) { durationSecs = 30; reset(); }});

        startBtn.getStyleClass().add("btn-primary");
        startBtn.setOnAction(e -> start());

        HBox controls = new HBox(10, btn5, btn10, btn30, startBtn);
        controls.setAlignment(Pos.CENTER);

        getChildren().addAll(header, target, controls);

        // Input
        target.setOnMousePressed(e -> {
            if (running) {
                e.consume();
                clicks++;
                double elapsed = (durationSecs * 1000.0 - (endTime - System.currentTimeMillis())) / 1000.0;
                cpsValue.setText(String.format("%.1f", clicks / Math.max(elapsed, 0.1)));
            }
        });

        // Game Loop
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!running) return;
                long remaining = endTime - System.currentTimeMillis();
                
                if (remaining <= 0) {
                    finish();
                } else {
                    timerLabel.setText(String.format("%.1fs", remaining / 1000.0));
                }
            }
        };
        
        reset();
    }

    public void reset() {
        running = false;
        timer.stop();
        clicks = 0;
        timerLabel.setText(String.format("%.1fs", (double) durationSecs));
        cpsValue.setText("0.0");
        target.getStyleClass().add("disabled");
        ((Label) target.getChildren().get(0)).setText("Click Start");
        startBtn.setText("Start");
    }

    private void start() {
        if (running) return;
        
        running = true;
        clicks = 0;
        endTime = System.currentTimeMillis() + (durationSecs * 1000L);
        
        target.getStyleClass().remove("disabled");
        ((Label) target.getChildren().get(0)).setText("CLICK!");
        startBtn.setText("...");
        
        timer.start();
    }

    private void finish() {
        running = false;
        timer.stop();
        target.getStyleClass().add("disabled");
        ((Label) target.getChildren().get(0)).setText("Done!");
        startBtn.setText("Restart");
        
        double finalCps = (double) clicks / durationSecs;
        cpsValue.setText(String.format("%.1f", finalCps));
        timerLabel.setText("0.0s");
        
        onNewHighScore.accept(finalCps);
    }
}