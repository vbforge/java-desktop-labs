package com.vbforge.dietarytrack;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.List;

/**
 * Window behaviour for the undecorated stage: sizing to the screen, maximize/restore that
 * respects the taskbar, minimize, and resizing by dragging the window edges.
 */
final class WindowSupport {

    private static final double GRIP = 6;
    private static final int N = 1, S = 2, W = 4, E = 8;

    private final Stage stage;
    private final BooleanProperty maximized = new SimpleBooleanProperty(false);
    private Rectangle2D restoreBounds;

    // state of an in-progress edge drag
    private int dragEdges;
    private double startScreenX, startScreenY, startX, startY, startW, startH;

    WindowSupport(Stage stage) {
        this.stage = stage;
    }

    BooleanProperty maximizedProperty() {
        return maximized;
    }

    /** Sizes the window to the preferred size, shrunk to fit the screen, and centres it. */
    void fitToScreen(double preferredWidth, double preferredHeight, double minWidth, double minHeight) {
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        double margin = 24;
        double width = Math.min(preferredWidth, area.getWidth() - margin);
        double height = Math.min(preferredHeight, area.getHeight() - margin);
        stage.setMinWidth(Math.min(minWidth, width));
        stage.setMinHeight(Math.min(minHeight, height));
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(area.getMinX() + (area.getWidth() - width) / 2);
        stage.setY(area.getMinY() + (area.getHeight() - height) / 2);
    }

    void minimize() {
        stage.setIconified(true);
    }

    void close() {
        stage.close();
    }

    void toggleMaximize() {
        if (maximized.get()) {
            if (restoreBounds != null) {
                applyBounds(restoreBounds);
            }
            maximized.set(false);
        } else {
            restoreBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
            applyBounds(currentScreen().getVisualBounds());
            maximized.set(true);
        }
    }

    boolean isMaximized() {
        return maximized.get();
    }

    private Screen currentScreen() {
        List<Screen> screens = Screen.getScreensForRectangle(
                stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        return screens.isEmpty() ? Screen.getPrimary() : screens.get(0);
    }

    private void applyBounds(Rectangle2D r) {
        stage.setX(r.getMinX());
        stage.setY(r.getMinY());
        stage.setWidth(r.getWidth());
        stage.setHeight(r.getHeight());
    }

    /** Lets the user resize the borderless window by dragging any edge or corner. */
    void installResizing(Scene scene) {
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (maximized.get() || dragEdges != 0) {
                return;
            }
            Cursor wanted = cursorFor(edgesAt(e, scene));
            if (scene.getCursor() != wanted && (wanted != Cursor.DEFAULT || isResizeCursor(scene.getCursor()))) {
                scene.setCursor(wanted);
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (maximized.get() || e.getButton() != MouseButton.PRIMARY) {
                return;
            }
            int edges = edgesAt(e, scene);
            if (edges != 0) {
                dragEdges = edges;
                startScreenX = e.getScreenX();
                startScreenY = e.getScreenY();
                startX = stage.getX();
                startY = stage.getY();
                startW = stage.getWidth();
                startH = stage.getHeight();
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (dragEdges == 0) {
                return;
            }
            double dx = e.getScreenX() - startScreenX;
            double dy = e.getScreenY() - startScreenY;
            double minW = Math.max(stage.getMinWidth(), 1);
            double minH = Math.max(stage.getMinHeight(), 1);
            double x = startX, y = startY, w = startW, h = startH;

            if ((dragEdges & E) != 0) {
                w = Math.max(minW, startW + dx);
            }
            if ((dragEdges & S) != 0) {
                h = Math.max(minH, startH + dy);
            }
            if ((dragEdges & W) != 0) {
                w = Math.max(minW, startW - dx);
                x = startX + (startW - w);
            }
            if ((dragEdges & N) != 0) {
                h = Math.max(minH, startH - dy);
                y = startY + (startH - h);
            }
            stage.setX(x);
            stage.setY(y);
            stage.setWidth(w);
            stage.setHeight(h);
            e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (dragEdges != 0) {
                dragEdges = 0;
                e.consume();
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (dragEdges == 0 && isResizeCursor(scene.getCursor())) {
                scene.setCursor(Cursor.DEFAULT);
            }
        });
    }

    private static int edgesAt(MouseEvent e, Scene scene) {
        int edges = 0;
        if (e.getSceneY() < GRIP) edges |= N;
        if (e.getSceneY() > scene.getHeight() - GRIP) edges |= S;
        if (e.getSceneX() < GRIP) edges |= W;
        if (e.getSceneX() > scene.getWidth() - GRIP) edges |= E;
        return edges;
    }

    private static Cursor cursorFor(int edges) {
        return switch (edges) {
            case N, S -> Cursor.N_RESIZE;
            case W, E -> Cursor.E_RESIZE;
            case N | W -> Cursor.NW_RESIZE;
            case N | E -> Cursor.NE_RESIZE;
            case S | W -> Cursor.SW_RESIZE;
            case S | E -> Cursor.SE_RESIZE;
            default -> Cursor.DEFAULT;
        };
    }

    private static boolean isResizeCursor(Cursor c) {
        return c == Cursor.N_RESIZE || c == Cursor.E_RESIZE || c == Cursor.NW_RESIZE
                || c == Cursor.NE_RESIZE || c == Cursor.SW_RESIZE || c == Cursor.SE_RESIZE;
    }
}
