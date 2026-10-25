package com.vbforge.blockforge.ui;

import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.render.BoardRenderer;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * Square board surface: a {@link Canvas} centred inside a {@link Region}, plus selection.
 *
 * <p>Selection model (matches the original assignment):
 * <ul>
 *   <li>Click a point → the deepest block at that point.</li>
 *   <li>{@code ↑} / {@code ↓} → move the selection up / down the tree <em>at the original click
 *       point</em>, clamped to {@code [0, maxDepth]}.</li>
 * </ul>
 *
 * <p>The click point is stored separately ({@link #clickUnitX}, {@link #clickUnitY}) because a
 * block's {@code (x, y)} is its top-left corner, not the point inside it. Querying
 * {@code getSelectedBlock(selected.x(), selected.y(), newLevel)} would occasionally fall outside
 * the enclosing parent and return {@code null}.
 */
public final class BoardView extends Region {

    private final Canvas canvas = new Canvas();

    private GameState state;
    private Block selected;
    private int clickUnitX = -1;
    private int clickUnitY = -1;
    private Consumer<Block> onSelectionChanged;

    public BoardView() {
        getChildren().add(canvas);
        setMinSize(0, 0);
        setFocusTraversable(true);

        canvas.setOnMouseClicked(this::handleClick);
        addEventHandler(KeyEvent.KEY_PRESSED, this::handleKey);
    }

    public void setState(GameState s) {
        this.state = s;
        this.selected = null;
        this.clickUnitX = -1;
        this.clickUnitY = -1;
        redraw();
        fireSelectionChanged();
    }

    public Block selected() {
        return selected;
    }

    public void setOnSelectionChanged(Consumer<Block> handler) {
        this.onSelectionChanged = handler;
    }

    // ----------------------------------------------------------------- layout

    @Override
    protected void layoutChildren() {
        double side = Math.min(getWidth(), getHeight());
        if (side <= 0) return;
        canvas.setWidth(side);
        canvas.setHeight(side);
        canvas.setLayoutX((getWidth() - side) / 2);
        canvas.setLayoutY((getHeight() - side) / 2);
        redraw();
    }

    @Override
    protected double computePrefWidth(double height) {
        return 640;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 640;
    }

    // ----------------------------------------------------------------- rendering

    private void redraw() {
        if (state == null || canvas.getWidth() <= 0) return;
        BoardRenderer.draw(canvas.getGraphicsContext2D(), state, selected, (int) canvas.getWidth());
    }

    // ----------------------------------------------------------------- input

    private void handleClick(MouseEvent e) {
        if (state == null) return;
        requestFocus();

        double px = canvas.getWidth() / (double) (1 << state.maxDepth());
        int unitX = (int) (e.getX() / px);
        int unitY = (int) (e.getY() / px);

        Block hit = state.board().getSelectedBlock(unitX, unitY, state.maxDepth());
        if (hit == null) {
            clickUnitX = -1;
            clickUnitY = -1;
            setSelected(null);
        } else {
            clickUnitX = unitX;
            clickUnitY = unitY;
            setSelected(hit);
        }
    }

    private void handleKey(KeyEvent e) {
        if (state == null || selected == null) return;

        int newLevel;
        if (e.getCode() == KeyCode.UP) {
            newLevel = Math.max(0, selected.level() - 1);
        } else if (e.getCode() == KeyCode.DOWN) {
            newLevel = Math.min(state.maxDepth(), selected.level() + 1);
        } else {
            return;
        }

        Block hit = state.board().getSelectedBlock(clickUnitX, clickUnitY, newLevel);
        if (hit != null) {
            setSelected(hit);
        }
        e.consume();
    }

    private void setSelected(Block b) {
        this.selected = b;
        redraw();
        fireSelectionChanged();
    }

    private void fireSelectionChanged() {
        if (onSelectionChanged != null) {
            onSelectionChanged.accept(selected);
        }
    }

    /** Unit-cell x of the last click; -1 if no click yet or selection was cleared. */
    public int selectedUnitX() {
        return clickUnitX;
    }

    /** Unit-cell y of the last click; -1 if no click yet or selection was cleared. */
    public int selectedUnitY() {
        return clickUnitY;
    }

    /** Re-selects the block at (unitX, unitY) clamped to the closest available level. */
    public void selectByCoordinates(int unitX, int unitY, int level) {
        if (state == null || unitX < 0 || unitY < 0) {
            return;
        }
        Block hit = state.board().getSelectedBlock(unitX, unitY, level);
        if (hit == null) {
            setSelected(null);
        } else {
            this.clickUnitX = unitX;
            this.clickUnitY = unitY;
            setSelected(hit);
        }
    }

}