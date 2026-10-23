package com.vbforge.fitapp.ui;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Objects;

/** Small helpers shared by all views. */
public final class UiUtil {

    private UiUtil() {
    }

    public static String css() {
        return Objects.requireNonNull(UiUtil.class.getResource("/css/vbforge-style.css"),
                "vbforge-style.css missing from resources").toExternalForm();
    }

    public static Window window(Node anchor) {
        return (anchor != null && anchor.getScene() != null) ? anchor.getScene().getWindow() : null;
    }

    public static void style(Dialog<?> dialog, Node anchor) {
        Window w = window(anchor);
        if (w != null) {
            dialog.initOwner(w);
        }
        dialog.getDialogPane().getStylesheets().add(css());
    }

    public static void error(Node anchor, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("FitApp");
        style(a, anchor);
        a.showAndWait();
    }

    public static void info(Node anchor, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle("FitApp");
        style(a, anchor);
        a.showAndWait();
    }

    public static boolean confirm(Node anchor, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        a.setTitle("Please confirm");
        style(a, anchor);
        return a.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    public static Label errorLabel() {
        Label l = new Label();
        l.getStyleClass().add("error-label");
        l.setWrapText(true);
        l.setManaged(false);
        l.setVisible(false);
        return l;
    }

    /** Shows (non-empty) or hides (null/empty) a message in a label created by {@link #errorLabel()}. */
    public static void setMessage(Label label, String text) {
        boolean show = text != null && !text.isEmpty();
        label.setText(show ? text : "");
        label.setManaged(show);
        label.setVisible(show);
    }

    /** A small uppercase caption above an input control. */
    public static VBox field(String caption, Node control) {
        Label l = new Label(caption.toUpperCase());
        l.getStyleClass().add("field-label");
        VBox box = new VBox(5, l, control);
        return box;
    }
}
