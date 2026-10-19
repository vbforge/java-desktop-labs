package com.vbforge.atlasdesk;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Locale;
import java.util.Objects;

final class Fx {

    private Fx() {}

    static String css() {
        return Objects.requireNonNull(Fx.class.getResource("/atlasdesk.css"), "atlasdesk.css missing").toExternalForm();
    }

    static void makeDraggable(Node handle, Stage stage) {
        double[] offset = new double[2];
        handle.setOnMousePressed(e -> { offset[0] = e.getSceneX(); offset[1] = e.getSceneY(); });
        handle.setOnMouseDragged(e -> {
            if (stage.isMaximized()) return;
            stage.setX(e.getScreenX() - offset[0]);
            stage.setY(e.getScreenY() - offset[1]);
        });
    }

    /** 47415750 -> "47.4 M" */
    static String compact(long n) {
        if (n >= 1_000_000_000L) return String.format(Locale.US, "%.2f B", n / 1e9);
        if (n >= 1_000_000L) return String.format(Locale.US, "%.1f M", n / 1e6);
        if (n >= 1_000L) return String.format(Locale.US, "%.0f K", n / 1e3);
        return Long.toString(n);
    }

    /** 47415750 -> "47,415,750" */
    static String full(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    static boolean confirm(Stage owner, String header, String text, String actionLabel) {
        ButtonType action = new ButtonType(actionLabel, ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, text, action, ButtonType.CANCEL);
        style(alert, owner);
        alert.setHeaderText(header);
        return alert.showAndWait().filter(b -> b == action).isPresent();
    }

    static void error(Stage owner, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Unexpected error" : message, ButtonType.OK);
        style(alert, owner);
        alert.setHeaderText("Something went wrong");
        alert.showAndWait();
    }

    static void info(Stage owner, String header, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, text, ButtonType.OK);
        style(alert, owner);
        alert.setHeaderText(header);
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }

    private static void style(Alert alert, Stage owner) {
        alert.initOwner(owner);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.setGraphic(null);
        alert.getDialogPane().getStylesheets().add(css());
        alert.getDialogPane().getStyleClass().add("app-dialog");
    }
}