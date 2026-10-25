package com.vbforge.blockforge.render;

import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.GameState;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Paints a {@link GameState}'s board onto a JavaFX Canvas.
 *
 * <p>All coordinates in {@link Block} are in <em>unit-cell</em> indices (root size =
 * {@code 2^maxDepth}) because that is what {@code BoardFactory} and the tests use. Rendering
 * scales those indices to the canvas pixels.
 */
public final class BoardRenderer {

    /** Selection outline color (matches the assignment's highlight). */
    private static final Color SELECTION_COLOR = Color.web("#29b6f6");

    private BoardRenderer() {}

    /**
     * Paints the board, the unit-cell grid, and the selection outline (if any) onto a square
     * canvas of side length {@code pixelSize}.
     */
    public static void draw(GraphicsContext g, GameState state, Block selected, int pixelSize) {
        if (state == null || pixelSize <= 0) return;

        g.clearRect(0, 0, pixelSize, pixelSize);

        int cells = 1 << state.maxDepth();
        double px = (double) pixelSize / cells;

        drawLeaves(g, state.board(), px);
        drawGrid(g, cells, px);

        if (selected != null) {
            drawSelection(g, selected, px);
        }
    }

    private static void drawLeaves(GraphicsContext g, Block b, double px) {
        if (b.isLeaf()) {
            g.setFill(Color.web(b.color().hex()));
            g.fillRect(b.x() * px, b.y() * px, b.size() * px, b.size() * px);
            return;
        }
        for (Block child : b.children()) {
            drawLeaves(g, child, px);
        }
    }

    private static void drawGrid(GraphicsContext g, int cells, double px) {
        g.setStroke(Color.BLACK);
        g.setLineWidth(1.0);
        // Half-pixel offset for crisp 1px lines on integer-pixel bounds.
        for (int i = 0; i <= cells; i++) {
            double p = Math.round(i * px) + 0.5;
            g.strokeLine(p, 0, p, cells * px);
            g.strokeLine(0, p, cells * px, p);
        }
    }

    private static void drawSelection(GraphicsContext g, Block b, double px) {
        g.setStroke(SELECTION_COLOR);
        g.setLineWidth(3.0);
        g.strokeRect(
                b.x() * px + 1.5,
                b.y() * px + 1.5,
                b.size() * px - 3.0,
                b.size() * px - 3.0);
    }
}