package com.vbforge.pixelforge;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * PixelForge — interactive Mandelbrot explorer.
 *
 * The interesting part: all rendering happens on a single background
 * thread, one task at a time. Each new request cancels the in-flight
 * task, so the freshest view wins and stale work is abandoned rather
 * than queued.
 */
public class PixelForgeApp extends Application {

    private static final int CANVAS_W = 900;
    private static final int CANVAS_H = 620;
    private static final int PREVIEW_DIVISOR = 2;   // preview renders at 1/2 each axis

    // Single-threaded: never more than one render at a time.
    // Daemon threads so the JVM exits cleanly if the window is closed
    // while a render is in flight.
    private final ExecutorService renderExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "pixelforge-renderer");
                t.setDaemon(true);
                return t;
            });

    private FractalView view;
    private Label status;
    private Task<WritableImage> currentTask;

    private int maxIterations = 150;

    @Override
    public void start(Stage stage) {
        view = new FractalView(CANVAS_W, CANVAS_H, this::requestRender);

        Slider iterations = new Slider(50, 500, 150);
        iterations.setPrefWidth(220);
        iterations.setShowTickMarks(true);
        iterations.setMajorTickUnit(100);
        iterations.setBlockIncrement(50);

        Label iterLabel = new Label();
        iterLabel.textProperty().bind(iterations.valueProperty().asString("Iterations: %.0f"));

        Button reset = new Button("Reset view");
        reset.setOnAction(e -> view.setViewport(
                Viewport.initial(CANVAS_W, CANVAS_H)));

        // Fires continuously while dragging the slider. Each change cancels
        // the previous render — intentional, so the final position renders fast.
        iterations.valueProperty().addListener((obs, old, now) -> {
            maxIterations = now.intValue();
            requestRender(new FractalView.RenderRequest(view.viewport(), false));
        });

        status = new Label("Drag to pan · scroll to zoom · right-click to reset");

        HBox bar = new HBox(14, iterLabel, iterations, spacer(), reset);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1117;");
        root.setPadding(new Insets(16));
        root.setTop(bar);
        root.setCenter(view);
        root.setBottom(status);
        BorderPane.setMargin(status, new Insets(12, 0, 0, 0));

        Scene scene = new Scene(root, CANVAS_W + 32, CANVAS_H + 110);
        scene.getStylesheets().add(
                PixelForgeApp.class.getResource("/pixelforge.css").toExternalForm());

        stage.setTitle("PixelForge — Mandelbrot explorer");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.setOnCloseRequest(e -> renderExecutor.shutdownNow());
        stage.show();

        // First render.
        requestRender(new FractalView.RenderRequest(view.viewport(), false));
    }

    /**
     * Cancel any in-flight render and start a new one for the given request.
     *
     * Runs on the FX thread. The Task's completion callbacks (onSucceeded /
     * onCancelled / onFailed) also run on the FX thread, so drawing into
     * the canvas from them is safe.
     */
    private void requestRender(FractalView.RenderRequest req) {
        if (currentTask != null) currentTask.cancel();

        int maxIter = (int) Math.round(
                ((Slider) ((HBox) ((BorderPane) view.getParent()).getTop())
                        .getChildren().get(1)).getValue());

        Viewport renderVp = req.preview() ? downscaled(req.viewport()) : req.viewport();

        RenderTask task = new RenderTask(renderVp, maxIter);
        task.setOnSucceeded(e -> {
            view.showImage(task.getValue());
            if (!req.preview()) {
                status.setText(String.format(
                        "%.0fx zoom · %d iterations · %d×%d px",
                        3.5 / (req.viewport().scale() * req.viewport().width()),
                        maxIter,
                        req.viewport().width(), req.viewport().height()));
            }
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            status.setText("Render failed: " + (ex == null ? "unknown" : ex.getMessage()));
        });

        currentTask = task;
        renderExecutor.submit(task);
    }

    private static Viewport downscaled(Viewport vp) {
        int w = Math.max(1, vp.width()  / PREVIEW_DIVISOR);
        int h = Math.max(1, vp.height() / PREVIEW_DIVISOR);
        // Same complex region, smaller canvas → larger scale per pixel.
        double scale = vp.scale() * vp.width() / w;
        return new Viewport(vp.centerX(), vp.centerY(), scale, w, h);
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    @Override
    public void stop() {
        renderExecutor.shutdownNow();
    }

    public static void main(String[] args) {
        launch(args);
    }
}