package com.vbforge.pathlab;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * PathLab — a grid-based pathfinding visualizer.
 *
 * Draw walls with the mouse, choose BFS / Dijkstra / A*, and watch the
 * search expand one cell at a time before the shortest path is traced.
 */
public class PathLabApp extends Application {

    private static final int ROWS = 25;
    private static final int COLS = 45;

    private static final Duration MIN_STEP_MS = Duration.millis(4);
    private static final Duration MAX_STEP_MS = Duration.millis(60);
    private static final int TARGET_ANIMATION_MS = 2200;
    private static final Duration PATH_STEP_MS = Duration.millis(28);

    private final Grid grid = new Grid(ROWS, COLS);

    private GridView view;
    private Label status;
    private Timeline timeline;

    @Override
    public void start(Stage stage) {
        view = new GridView(grid);
        view.draw();

        ComboBox<Algorithms.Search> algoBox = new ComboBox<>();
        algoBox.getItems().addAll(Algorithms.Search.bfs(), Algorithms.Search.dijkstra(), Algorithms.Search.astar());
        algoBox.getSelectionModel().selectFirst();

        Button run   = new Button("Run");
        Button clear = new Button("Clear search");
        Button reset = new Button("Reset");

        run.setOnAction(e   -> runSearch(algoBox.getValue()));
        clear.setOnAction(e -> { stopTimeline(); grid.clearSearch(); view.draw(); setStatus("Click and drag to draw walls."); });
        reset.setOnAction(e -> { stopTimeline(); grid.reset();       view.draw(); setStatus("Click and drag to draw walls."); });

        HBox bar = new HBox(10, algoBox, run, clear, reset);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 12, 0));

        status = new Label("Click and drag to draw walls.");
        status.setTextFill(Color.web("#8b949e"));

        VBox root = new VBox(0, bar, view, status);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #0d1117;");
        VBox.setMargin(status, new Insets(12, 0, 0, 0));

        stage.setTitle("PathLab — pathfinding visualiser");
        stage.setScene(new Scene(root));
        stage.setResizable(false);
        stage.show();
    }

    private void setStatus(String text) {
        status.setText(text);
    }

    private void stopTimeline() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        view.setMouseTransparent(false);
    }

    private void runSearch(Algorithms.Search search) {
        if (search == null) return;

        stopTimeline();
        grid.clearSearch();
        view.draw();

        Algorithms.SearchResult result = search.run(grid);
        int total = result.visitedOrder().size();

        if (total == 0) {
            setStatus("Nothing to explore.");
            return;
        }

        view.setMouseTransparent(true);
        setStatus("Exploring with " + search.name() + "…");

        Duration step = stepDuration(total);
        final int[] i = {0};

        Timeline tl = new Timeline(new KeyFrame(step, e -> {
            if (i[0] >= total) return;
            int[] cell = result.visitedOrder().get(i[0]++);
            grid.setState(cell[0], cell[1], Grid.State.VISITED);
            view.draw();
        }));
        tl.setCycleCount(total);
        tl.setOnFinished(e -> animatePath(search, result));
        timeline = tl;
        tl.play();
    }

    /** Aim for roughly TARGET_ANIMATION_MS total, clamped to sane per-step bounds. */
    private static Duration stepDuration(int totalSteps) {
        long ms = TARGET_ANIMATION_MS / Math.max(totalSteps, 1);
        long clamped = Math.max((long) MIN_STEP_MS.toMillis(),
                       Math.min((long) MAX_STEP_MS.toMillis(), ms));
        return Duration.millis(clamped);
    }

    private void animatePath(Algorithms.Search search, Algorithms.SearchResult result) {
        if (result.path().isEmpty()) {
            view.setMouseTransparent(false);
            setStatus(search.name() + ": no path found — " + result.visitedCount() + " cells explored.");
            return;
        }

        setStatus("Tracing shortest path…");

        final int[] i = {0};
        int n = result.path().size();

        Timeline tl = new Timeline(new KeyFrame(PATH_STEP_MS, e -> {
            if (i[0] >= n) return;
            int[] cell = result.path().get(i[0]++);
            grid.setState(cell[0], cell[1], Grid.State.PATH);
            view.draw();
        }));
        tl.setCycleCount(n);
        tl.setOnFinished(e -> {
            view.setMouseTransparent(false);
            setStatus(search.name() + " — path length " + (n - 1)
                    + ", visited " + result.visitedCount() + " cells.");
        });
        timeline = tl;
        tl.play();
    }

    public static void main(String[] args) {
        launch(args);
    }
}