package com.vbforge.pathlab;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Renders the grid and handles mouse input for drawing walls.
 *
 * Press-and-drag over an empty cell draws walls; press-and-drag starting
 * on a wall erases. The mode is locked in on the initial press so a single
 * drag doesn't flicker between drawing and erasing.
 */
public final class GridView extends Canvas {

    public static final double CELL = 22;

    private final Grid grid;
    private boolean erasing;

    public GridView(Grid grid) {
        this.grid = grid;
        setWidth(grid.cols() * CELL);
        setHeight(grid.rows() * CELL);

        setOnMousePressed(e -> {
            int[] rc = cellAt(e.getX(), e.getY());
            if (rc == null) return;
            erasing = grid.get(rc[0], rc[1]) == Grid.State.WALL;
            apply(rc[0], rc[1]);
        });

        setOnMouseDragged(e -> {
            int[] rc = cellAt(e.getX(), e.getY());
            if (rc != null) apply(rc[0], rc[1]);
        });
    }

    private int[] cellAt(double x, double y) {
        int c = (int) (x / CELL);
        int r = (int) (y / CELL);
        return grid.inBounds(r, c) ? new int[]{r, c} : null;
    }

    private void apply(int r, int c) {
        Grid.State current = grid.get(r, c);
        if (current == Grid.State.START || current == Grid.State.END) return;
        grid.setState(r, c, erasing ? Grid.State.EMPTY : Grid.State.WALL);
        draw();
    }

    /** Repaint every cell. Cheap enough at this grid size to skip dirty tracking. */
    public void draw() {
        GraphicsContext g = getGraphicsContext2D();

        for (int r = 0; r < grid.rows(); r++) {
            for (int c = 0; c < grid.cols(); c++) {
                double x = c * CELL;
                double y = r * CELL;

                g.setFill(colorFor(grid.get(r, c)));
                g.fillRect(x, y, CELL, CELL);

                g.setStroke(Color.web("#1b2129"));
                g.setLineWidth(0.5);
                g.strokeRect(x, y, CELL, CELL);
            }
        }
    }

    private static Color colorFor(Grid.State s) {
        return switch (s) {
            case EMPTY    -> Color.web("#12171f");
            case WALL     -> Color.web("#30363d");
            case START    -> Color.web("#3fb950");
            case END      -> Color.web("#f85149");
            case VISITED  -> Color.web("#1f3a5f");
            case FRONTIER -> Color.web("#2d6ea8");
            case PATH     -> Color.web("#f89820");
        };
    }
}