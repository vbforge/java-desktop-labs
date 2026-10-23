package com.vbforge.fitapp;

import com.vbforge.fitapp.ui.AuthView;
import com.vbforge.fitapp.ui.MainView;
import com.vbforge.fitapp.ui.UiUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.sql.SQLException;

public class App extends Application {

    private AppContext ctx;
    private Stage stage;
    private final BorderPane root = new BorderPane();
    private double dragX;
    private double dragY;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        try {
            ctx = AppContext.create();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Could not open the database:\n" + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("FitApp");
        root.setTop(buildTitleBar());

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(UiUtil.css());
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();

        showAuth();
    }

    @Override
    public void stop() {
        if (ctx != null) {
            ctx.db().close();
        }
    }

    // ---- screens ---------------------------------------------------------

    private void showAuth() {
        root.setCenter(new AuthView(ctx, this::showMain));
    }

    private void showMain() {
        root.setCenter(new MainView(ctx, () -> {
            ctx.auth().logout();
            showAuth();
        }));
    }

    // ---- custom window frame --------------------------------------------

    private HBox buildTitleBar() {
        Label title = new Label("FitApp");
        title.getStyleClass().add("title-bar-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button min = windowButton("_", "minimize");
        min.setOnAction(e -> stage.setIconified(true));
        Button max = windowButton("□", "maximize");
        max.setOnAction(e -> stage.setMaximized(!stage.isMaximized()));
        Button close = windowButton("x", "close");
        close.setOnAction(e -> Platform.exit());

        HBox bar = new HBox(title, spacer, min, max, close);
        bar.getStyleClass().add("title-bar");
        bar.setAlignment(Pos.CENTER_LEFT);

        bar.setOnMousePressed(e -> {
            dragX = e.getSceneX();
            dragY = e.getSceneY();
        });
        bar.setOnMouseDragged(e -> {
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - dragX);
                stage.setY(e.getScreenY() - dragY);
            }
        });
        bar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getTarget() != min && e.getTarget() != close) {
                stage.setMaximized(!stage.isMaximized());
            }
        });
        return bar;
    }

    private static Button windowButton(String text, String extraStyle) {
        Button b = new Button(text);
        b.getStyleClass().addAll("window-button", extraStyle);
        b.setFocusTraversable(false);
        return b;
    }
}
