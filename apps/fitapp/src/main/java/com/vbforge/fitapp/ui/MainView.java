package com.vbforge.fitapp.ui;

import com.vbforge.fitapp.AppContext;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Dashboard shell: sidebar navigation that swaps the center view (no extra windows). */
public class MainView extends BorderPane {

    private final AppContext ctx;
    private final StackPane center = new StackPane();
    private final ToggleGroup nav = new ToggleGroup();

    private final AccountView account;
    private final WorkoutView workouts;
    private final ExerciseView exercises;

    public MainView(AppContext ctx, Runnable onLogout) {
        this.ctx = ctx;
        this.account = new AccountView(ctx);
        this.workouts = new WorkoutView(ctx, this::openManager);
        this.exercises = new ExerciseView(ctx);

        Label brand = new Label("FitApp");
        brand.getStyleClass().add("brand");
        brand.setPadding(new Insets(0, 0, 24, 8));

        ToggleButton bAccount = navButton("👤  Account", account::refresh, account);
        ToggleButton bWorkouts = navButton("🏋  Workouts", workouts::refresh, workouts);
        ToggleButton bExercises = navButton("💪  Exercises", exercises::refresh, exercises);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        javafx.scene.control.Button logout = new javafx.scene.control.Button("🚪  Logout");
        logout.getStyleClass().addAll("nav-btn", "logout");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> {
            account.dispose();
            onLogout.run();
        });

        VBox sidebar = new VBox(8, brand, bAccount, bWorkouts, bExercises, spacer, logout);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setMinWidth(240);

        center.getStyleClass().add("content-area");
        center.setPadding(new Insets(24));
        center.setAlignment(Pos.TOP_LEFT);

        setLeft(sidebar);
        setCenter(center);

        bAccount.setSelected(true);
    }

    private ToggleButton navButton(String text, Runnable onShow, Node view) {
        ToggleButton b = new ToggleButton(text);
        b.getStyleClass().add("nav-btn");
        b.setToggleGroup(nav);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setFocusTraversable(false);
        b.selectedProperty().addListener((o, was, is) -> {
            if (is) {
                onShow.run();
                show(view);
            }
        });
        // never allow "nothing selected"
        b.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, e -> {
            if (b.isSelected()) {
                e.consume();
            }
        });
        return b;
    }

    private void show(Node view) {
        center.getChildren().setAll(view);
    }

    /** Opens the workout manager (exercise linking) in the center area. */
    private void openManager(com.vbforge.fitapp.model.Workout workout) {
        WorkoutManagerView manager = new WorkoutManagerView(ctx, workout, () -> {
            workouts.refresh();
            show(workouts);
        });
        show(manager);
    }
}
