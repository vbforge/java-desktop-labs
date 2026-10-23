package com.vbforge.fitapp.ui;

import com.vbforge.fitapp.AppContext;
import com.vbforge.fitapp.model.Workout;
import com.vbforge.fitapp.service.ServiceException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/** Workout list with live search, add and delete; "Manage" opens the exercise-linking view. */
public class WorkoutView extends VBox {

    private final AppContext ctx;
    private final Consumer<Workout> onManage;
    private final ObservableList<Workout> items = FXCollections.observableArrayList();
    private final FilteredList<Workout> filtered = new FilteredList<>(items, w -> true);
    private final TableView<Workout> table = new TableView<>(filtered);
    private final TextField search = new TextField();

    public WorkoutView(AppContext ctx, Consumer<Workout> onManage) {
        super(16);
        this.ctx = ctx;
        this.onManage = onManage;

        Label title = new Label("My Workouts");
        title.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        search.setPromptText("Search workouts…");
        search.setPrefWidth(240);
        search.textProperty().addListener((o, a, text) -> {
            String q = text == null ? "" : text.trim().toLowerCase();
            filtered.setPredicate(w -> q.isEmpty() || w.name().toLowerCase().contains(q));
        });
        Button add = new Button("+ Add Workout");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> addWorkout());
        HBox header = new HBox(12, title, spacer, search, add);
        header.setAlignment(Pos.CENTER_LEFT);

        TableColumn<Workout, String> nameCol = new TableColumn<>("WORKOUT NAME");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().name()));
        nameCol.setStyle("-fx-font-weight: bold;");
        nameCol.setPrefWidth(320);

        TableColumn<Workout, String> countCol = new TableColumn<>("EXERCISES LINKED");
        countCol.setCellValueFactory(cd -> {
            int n = cd.getValue().exerciseCount();
            return new javafx.beans.property.SimpleStringProperty(n + (n == 1 ? " Exercise" : " Exercises"));
        });
        countCol.setPrefWidth(200);

        TableColumn<Workout, Void> actions = new TableColumn<>("ACTIONS");
        actions.setSortable(false);
        actions.setPrefWidth(220);
        actions.setCellFactory(col -> new TableCell<>() {
            private final Button manage = new Button("Manage");
            private final Button delete = new Button("Delete");
            private final HBox box = new HBox(8, manage, delete);

            {
                manage.getStyleClass().add("small");
                delete.getStyleClass().addAll("small", "danger");
                box.setAlignment(Pos.CENTER_LEFT);
                manage.setOnAction(e -> onManage.accept(getTableView().getItems().get(getIndex())));
                delete.setOnAction(e -> deleteWorkout(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(java.util.List.of(nameCol, countCol, actions));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No workouts yet. Click \"+ Add Workout\" to create one."));
        table.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Workout> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    onManage.accept(row.getItem());
                }
            });
            return row;
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        Label hint = new Label("Tip: double-click a workout (or press Manage) to add exercises to it.");
        hint.getStyleClass().add("muted");

        setPadding(new Insets(0));
        getChildren().addAll(header, table, hint);
    }

    public void refresh() {
        if (!ctx.session().isLoggedIn()) {
            return;
        }
        items.setAll(ctx.workouts().listWorkouts());
    }

    private void addWorkout() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("New workout");
        d.setHeaderText("Name your workout (e.g. \"Push Day\")");
        d.setContentText("Name:");
        UiUtil.style(d, this);
        d.showAndWait().ifPresent(name -> {
            try {
                ctx.workouts().addWorkout(name);
                search.clear();
                refresh();
            } catch (ServiceException ex) {
                UiUtil.error(this, ex.getMessage());
            }
        });
    }

    private void deleteWorkout(Workout w) {
        if (UiUtil.confirm(this, "Delete workout \"" + w.name() + "\"? Its exercises stay in your library.")) {
            try {
                ctx.workouts().deleteWorkout(w.id());
                refresh();
            } catch (ServiceException ex) {
                UiUtil.error(this, ex.getMessage());
            }
        }
    }
}
