package com.vbforge.clickrush;

import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.function.DoubleConsumer;

public class ReactionGame extends VBox {
    private enum State { IDLE, WAITING, READY }
    
    private State state = State.IDLE;
    private final PauseTransition delayTimer = new PauseTransition();
    private long startTime;
    
    private final Label title = new Label();
    private final Label sub = new Label();
    private final Label result = new Label();

    private final DoubleConsumer onNewHighScore;

    public ReactionGame(DoubleConsumer onNewHighScore) {
        this.onNewHighScore = onNewHighScore;
        setAlignment(Pos.CENTER);
        setStyle("-fx-cursor: hand;");
        
        title.getStyleClass().add("reaction-text");
        sub.getStyleClass().add("reaction-sub");
        result.getStyleClass().add("reaction-result");
        
        getChildren().addAll(title, sub, result);
        
        delayTimer.setOnFinished(e -> {
            state = State.READY;
            setStyle("-fx-background-color: #0f2e15;"); // Ready Green
            title.setText("CLICK NOW!");
            sub.setText("");
            startTime = System.nanoTime();
        });

        setOnMouseClicked(e -> handleClick());
        reset();
    }

    public void reset() {
        delayTimer.stop();
        state = State.IDLE;
        setStyle("-fx-background-color: #2a0e12;"); // Waiting Red
        title.setText("Click to Start");
        sub.setText("Test your visual reflexes");
        result.setVisible(false);
    }

    private void handleClick() {
        switch (state) {
            case IDLE -> {
                state = State.WAITING;
                setStyle("-fx-background-color: #2a0e12;");
                title.setText("Wait for Green...");
                sub.setText("Do not click yet!");
                result.setVisible(false);
                
                delayTimer.setDuration(Duration.millis(1500 + Math.random() * 3000));
                delayTimer.play();
            }
            case WAITING -> {
                delayTimer.stop();
                state = State.IDLE;
                setStyle("-fx-background-color: #f85149;"); // Flash Red
                title.setText("Too early!");
                sub.setText("Click to try again");
                // Small pause before resetting the red flash
                PauseTransition flash = new PauseTransition(Duration.millis(300));
                flash.setOnFinished(ev -> reset());
                flash.play();
            }
            case READY -> {
                long elapsed = System.nanoTime() - startTime;
                int ms = (int) (elapsed / 1_000_000);
                
                state = State.IDLE;
                setStyle("-fx-background-color: #2a0e12;");
                title.setText("Click to try again");
                sub.setText("Your reaction time:");
                result.setText(ms + " ms");
                result.setVisible(true);
                
                onNewHighScore.accept((double) ms);
            }
        }
    }
}