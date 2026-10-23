package com.vbforge.dietarytrack;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/** Custom title bar with drag-to-move, double-click maximize and minimize/maximize/close buttons. */
final class TitleBar extends HBox {

    private final Stage stage;
    private final WindowSupport window;
    private double dragOffsetX;
    private double dragOffsetY;

    TitleBar(Stage stage, WindowSupport window, String title) {
        this.stage = stage;
        this.window = window;
        getStyleClass().add("titlebar");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("app-title");

        HBox dragArea = new HBox(titleLabel);
        dragArea.setAlignment(Pos.CENTER_LEFT);
        dragArea.getStyleClass().add("titlebar-drag");
        HBox.setHgrow(dragArea, Priority.ALWAYS);
        dragArea.setOnMousePressed(this::onPressed);
        dragArea.setOnMouseDragged(this::onDragged);
        dragArea.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                window.toggleMaximize();
            }
        });

        Region maxIcon = icon("icon-maximize");
        Button minBtn = windowButton(icon("icon-minimize"), "Minimize", "win-btn");
        Button maxBtn = windowButton(maxIcon, "Maximize", "win-btn");
        Button closeBtn = windowButton(icon("icon-close"), "Close", "win-btn", "win-btn-close");

        minBtn.setOnAction(e -> window.minimize());
        maxBtn.setOnAction(e -> window.toggleMaximize());
        closeBtn.setOnAction(e -> window.close());

        window.maximizedProperty().addListener((obs, was, isMax) -> {
            maxIcon.getStyleClass().removeAll("icon-maximize", "icon-restore");
            maxIcon.getStyleClass().add(isMax ? "icon-restore" : "icon-maximize");
            maxBtn.getTooltip().setText(isMax ? "Restore" : "Maximize");
        });

        getChildren().addAll(dragArea, minBtn, maxBtn, closeBtn);
    }

    private static Region icon(String shapeClass) {
        Region r = new Region();
        r.getStyleClass().addAll("win-icon", shapeClass);
        r.setMouseTransparent(true);
        return r;
    }

    private static Button windowButton(Region graphic, String tooltip, String... styleClasses) {
        Button b = new Button();
        b.setGraphic(graphic);
        b.getStyleClass().addAll(styleClasses);
        b.setFocusTraversable(false);
        b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return b;
    }

    private void onPressed(MouseEvent e) {
        dragOffsetX = e.getScreenX() - stage.getX();
        dragOffsetY = e.getScreenY() - stage.getY();
    }

    private void onDragged(MouseEvent e) {
        if (window.isMaximized() || e.getButton() != MouseButton.PRIMARY) {
            return;
        }
        stage.setX(e.getScreenX() - dragOffsetX);
        stage.setY(e.getScreenY() - dragOffsetY);
    }
}
