package com.vbforge.slidecraft.ui;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.render.SlideCanvas;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Fullscreen presentation. Left/Right (and Space, PageUp/PageDown, ...) or clicking the left/right
 * screen edge navigate with a sliding {@link TranslateTransition}; Esc exits.
 */
public final class PresentationView extends StackPane {

    private static final Duration SLIDE_TIME = Duration.millis(320);
    private static final double EDGE_FRACTION = 0.25;

    private final Presentation presentation;
    private final Runnable onExit;
    private final Label hint = new Label("Esc to exit  ·  ← / → to navigate");

    private SlideCanvas currentCanvas;
    private int index;
    private boolean animating;

    public PresentationView(Presentation presentation, int startIndex, Runnable onExit) {
        this.presentation = presentation;
        this.onExit = onExit;
        this.index = Math.max(0, Math.min(startIndex, presentation.slides().size() - 1));

        // Letterbox bars blend into the slide instead of showing black.
        setStyle("-fx-background-color: " + presentation.theme().backgroundColor() + ";");
        setFocusTraversable(true);

        currentCanvas = canvasFor(index);

        hint.setStyle("-fx-text-fill: rgba(255,255,255,0.55); -fx-font-size: 12px;"
                + "-fx-background-color: rgba(0,0,0,0.45); -fx-padding: 4 8; -fx-background-radius: 4;");
        hint.setMouseTransparent(true);
        StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);
        StackPane.setMargin(hint, new Insets(0, 0, 16, 16));

        getChildren().addAll(currentCanvas, hint);

        addEventHandler(KeyEvent.KEY_PRESSED, this::onKey);
        addEventHandler(MouseEvent.MOUSE_CLICKED, this::onClick);

        // Fade the hint out after a moment so it never covers slide content for long.
        PauseTransition wait = new PauseTransition(Duration.seconds(2.5));
        FadeTransition fade = new FadeTransition(Duration.millis(600), hint);
        fade.setToValue(0);
        new SequentialTransition(wait, fade).play();
    }

    private SlideCanvas canvasFor(int slideIndex) {
        SlideCanvas canvas = new SlideCanvas();
        canvas.setOutlineVisible(false);
        canvas.show(presentation, presentation.slides().get(slideIndex));
        return canvas;
    }

    private void onKey(KeyEvent e) {
        switch (e.getCode()) {
            case ESCAPE -> onExit.run();
            case RIGHT, DOWN, PAGE_DOWN, SPACE, ENTER -> go(1);
            case LEFT, UP, PAGE_UP, BACK_SPACE -> go(-1);
            case HOME -> jump(0);
            case END -> jump(presentation.slides().size() - 1);
            default -> {
                return; // leave unrelated keys alone
            }
        }
        e.consume();
    }

    private void onClick(MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        double w = getWidth();
        if (e.getX() < w * EDGE_FRACTION) {
            go(-1);
        } else if (e.getX() > w * (1 - EDGE_FRACTION)) {
            go(1);
        }
    }

    private void jump(int target) {
        if (target == index || animating) {
            return;
        }
        go(target > index ? 1 : -1, target);
    }

    private void go(int delta) {
        go(delta, index + delta);
    }

    private void go(int direction, int target) {
        if (animating || target < 0 || target >= presentation.slides().size() || target == index) {
            return;
        }
        double width = Math.max(getWidth(), 1);

        SlideCanvas outgoing = currentCanvas;
        SlideCanvas incoming = canvasFor(target);
        incoming.setTranslateX(direction * width);
        getChildren().add(getChildren().size() - 1, incoming); // keep the hint on top

        TranslateTransition out = new TranslateTransition(SLIDE_TIME, outgoing);
        out.setToX(-direction * width);
        out.setInterpolator(Interpolator.EASE_BOTH);
        TranslateTransition in = new TranslateTransition(SLIDE_TIME, incoming);
        in.setToX(0);
        in.setInterpolator(Interpolator.EASE_BOTH);

        animating = true;
        ParallelTransition transition = new ParallelTransition(out, in);
        transition.setOnFinished(ev -> {
            getChildren().remove(outgoing);
            currentCanvas = incoming;
            index = target;
            animating = false;
        });
        transition.play();
    }
}
