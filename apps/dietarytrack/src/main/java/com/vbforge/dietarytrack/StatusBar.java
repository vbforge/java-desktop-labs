package com.vbforge.dietarytrack;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Bottom bar: the latest message on the left, the data folder on the right. */
final class StatusBar extends HBox {

    private static final String OK = "status-ok";
    private static final String ERROR = "status-error";

    private final Label message = new Label();

    StatusBar(String dataLocation) {
        getStyleClass().add("status-bar");
        setAlignment(Pos.CENTER_LEFT);

        message.getStyleClass().add("status-text");
        message.setMinWidth(0);

        Label location = new Label("Data: " + dataLocation);
        location.getStyleClass().addAll("status-text", "status-muted");
        location.setMinWidth(0);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(message, spacer, location);
        info("Ready");
    }

    void info(String text) {
        show(text, null);
    }

    void success(String text) {
        show(text, OK);
    }

    void error(String text) {
        show(text, ERROR);
    }

    private void show(String text, String styleClass) {
        message.setText(text);
        message.getStyleClass().removeAll(OK, ERROR);
        if (styleClass != null) {
            message.getStyleClass().add(styleClass);
        }
    }
}
