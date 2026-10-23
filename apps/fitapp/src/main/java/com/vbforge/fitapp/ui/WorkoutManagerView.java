package com.vbforge.fitapp.ui;

import com.vbforge.fitapp.AppContext;
import com.vbforge.fitapp.model.Exercise;
import com.vbforge.fitapp.model.Workout;
import com.vbforge.fitapp.service.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/** Two-column manager: available exercises on the left, exercises of this workout on the right. */
public class WorkoutManagerView extends VBox {

    private final AppContext ctx;
    private final Workout workout;

    private final ObservableList<Exercise> available = FXCollections.observableArrayList();
    private final ObservableList<Exercise> linked = FXCollections.observableArrayList();
    private final FilteredList<Exercise> availableFiltered = new FilteredList<>(available, e -> true);
    private final FilteredList<Exercise> linkedFiltered = new FilteredList<>(linked, e -> true);
    private final ListView<Exercise> availableList = new ListView<>(availableFiltered);
    private final ListView<Exercise> linkedList = new ListView<>(linkedFiltered);
    private final TextField search = new TextField();

    public WorkoutManagerView(AppContext ctx, Workout workout, Runnable onBack) {
        super(16);
        this.ctx = ctx;
        this.workout = workout;

        Label title = new Label("Manage: " + workout.name());
        title.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        search.setPromptText("Search exercises…");
        search.setPrefWidth(240);
        search.textProperty().addListener((o, a, text) -> {
            String q = text == null ? "" : text.trim().toLowerCase();
            availableFiltered.setPredicate(e -> q.isEmpty() || e.name().toLowerCase().contains(q));
            linkedFiltered.setPredicate(e -> q.isEmpty() || e.name().toLowerCase().contains(q));
        });
        Button back = new Button("← Back to Workouts");
        back.setOnAction(e -> onBack.run());
        HBox header = new HBox(12, title, spacer, search, back);
        header.setAlignment(Pos.CENTER_LEFT);

        availableList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        linkedList.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        availableList.setPlaceholder(new Label("No more exercises.\nCreate some in the Exercises tab."));
        linkedList.setPlaceholder(new Label("No exercises in this workout yet."));
        availableList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) addSelected();
        });
        linkedList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) removeSelected();
        });

        Button toRight = new Button("→");
        toRight.setOnAction(e -> addSelected());
        Button toLeft = new Button("←");
        toLeft.setOnAction(e -> removeSelected());
        VBox controls = new VBox(10, toRight, toLeft);
        controls.setAlignment(Pos.CENTER);

        VBox left = column("AVAILABLE EXERCISES", "muted", availableList);
        VBox right = column("IN THIS WORKOUT", "accent-text", linkedList);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        HBox layout = new HBox(16, left, controls, right);
        VBox.setVgrow(layout, Priority.ALWAYS);

        Label hint = new Label("Select one or more exercises and use the arrows (or double-click) to move them.");
        hint.getStyleClass().add("muted");

        setPadding(new Insets(0));
        getChildren().addAll(header, layout, hint);
        reload();
    }

    private VBox column(String caption, String styleClass, ListView<Exercise> list) {
        Label l = new Label(caption);
        l.getStyleClass().addAll("field-label", styleClass);
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox box = new VBox(8, l, list);
        return box;
    }

    private void reload() {
        available.setAll(ctx.workouts().exercisesNotIn(workout.id()));
        linked.setAll(ctx.workouts().exercisesIn(workout.id()));
    }

    private void addSelected() {
        move(new ArrayList<>(availableList.getSelectionModel().getSelectedItems()), true);
    }

    private void removeSelected() {
        move(new ArrayList<>(linkedList.getSelectionModel().getSelectedItems()), false);
    }

    private void move(List<Exercise> selection, boolean add) {
        try {
            for (Exercise e : selection) {
                if (add) {
                    ctx.workouts().addToWorkout(workout.id(), e.id());
                } else {
                    ctx.workouts().removeFromWorkout(workout.id(), e.id());
                }
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
        reload();
    }
}
