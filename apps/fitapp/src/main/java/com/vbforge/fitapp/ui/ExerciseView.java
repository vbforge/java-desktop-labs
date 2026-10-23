package com.vbforge.fitapp.ui;

import com.vbforge.fitapp.AppContext;
import com.vbforge.fitapp.model.Exercise;
import com.vbforge.fitapp.service.ServiceException;
import com.vbforge.fitapp.service.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Exercise library: live search, add / edit / delete with sets and reps. */
public class ExerciseView extends VBox {

    private final AppContext ctx;
    private final ObservableList<Exercise> items = FXCollections.observableArrayList();
    private final FilteredList<Exercise> filtered = new FilteredList<>(items, e -> true);
    private final TableView<Exercise> table = new TableView<>(filtered);
    private final TextField search = new TextField();

    public ExerciseView(AppContext ctx) {
        super(16);
        this.ctx = ctx;

        Label title = new Label("Exercise Library");
        title.getStyleClass().add("page-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        search.setPromptText("Search exercises…");
        search.setPrefWidth(240);
        search.textProperty().addListener((o, a, text) -> {
            String q = text == null ? "" : text.trim().toLowerCase();
            filtered.setPredicate(e -> q.isEmpty() || e.name().toLowerCase().contains(q));
        });
        Button add = new Button("+ Add Exercise");
        add.getStyleClass().add("primary");
        add.setOnAction(e -> showEditor(null));
        HBox header = new HBox(12, title, spacer, search, add);
        header.setAlignment(Pos.CENTER_LEFT);

        TableColumn<Exercise, String> nameCol = new TableColumn<>("EXERCISE NAME");
        nameCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(cd.getValue().name()));
        nameCol.setStyle("-fx-font-weight: bold;");
        nameCol.setPrefWidth(320);
        TableColumn<Exercise, String> setsCol = new TableColumn<>("DEFAULT SETS");
        setsCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().sets())));
        setsCol.setPrefWidth(140);
        TableColumn<Exercise, String> repsCol = new TableColumn<>("DEFAULT REPS");
        repsCol.setCellValueFactory(cd -> new javafx.beans.property.SimpleStringProperty(String.valueOf(cd.getValue().reps())));
        repsCol.setPrefWidth(140);

        TableColumn<Exercise, Void> actions = new TableColumn<>("ACTIONS");
        actions.setSortable(false);
        actions.setPrefWidth(180);
        actions.setCellFactory(col -> new TableCell<>() {
            private final Button edit = new Button("Edit");
            private final Button delete = new Button("Delete");
            private final HBox box = new HBox(8, edit, delete);

            {
                edit.getStyleClass().add("small");
                delete.getStyleClass().addAll("small", "danger");
                box.setAlignment(Pos.CENTER_LEFT);
                edit.setOnAction(e -> showEditor(getTableView().getItems().get(getIndex())));
                delete.setOnAction(e -> deleteExercise(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        table.getColumns().addAll(java.util.List.of(nameCol, setsCol, repsCol, actions));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No exercises yet. Click \"+ Add Exercise\" to create one."));
        VBox.setVgrow(table, Priority.ALWAYS);

        setPadding(new Insets(0));
        getChildren().addAll(header, table);
    }

    public void refresh() {
        if (!ctx.session().isLoggedIn()) {
            return;
        }
        items.setAll(ctx.workouts().listExercises());
    }

    /** Add dialog when {@code existing} is null, otherwise edit. */
    private void showEditor(Exercise existing) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "New exercise" : "Edit exercise");
        UiUtil.style(dialog, this);

        TextField name = new TextField(existing == null ? "" : existing.name());
        name.setPromptText("e.g. Bench Press");
        TextField sets = new TextField(String.valueOf(existing == null ? 3 : existing.sets()));
        TextField reps = new TextField(String.valueOf(existing == null ? 10 : existing.reps()));
        Label error = UiUtil.errorLabel();

        HBox sr = new HBox(12, UiUtil.field("Sets", sets), UiUtil.field("Reps", reps));
        HBox.setHgrow(sr.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(sr.getChildren().get(1), Priority.ALWAYS);
        VBox content = new VBox(12, UiUtil.field("Name", name), sr, error);
        content.setPadding(new Insets(16));
        content.setPrefWidth(380);
        dialog.getDialogPane().setContent(content);

        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        Button saveBtn = (Button) dialog.getDialogPane().lookupButton(save);
        saveBtn.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            UiUtil.setMessage(error, null);
            int s = Validator.parseInt(sets.getText());
            int r = Validator.parseInt(reps.getText());
            try {
                if (existing == null) {
                    ctx.workouts().addExercise(name.getText(), s, r);
                } else {
                    ctx.workouts().updateExercise(existing.id(), name.getText(), s, r);
                }
            } catch (ServiceException ex) {
                UiUtil.setMessage(error, ex.getMessage());
                ev.consume(); // keep the dialog open
            }
        });

        if (dialog.showAndWait().filter(b -> b == save).isPresent()) {
            refresh();
        }
    }

    private void deleteExercise(Exercise e) {
        if (UiUtil.confirm(this, "Delete exercise \"" + e.name() + "\"? It will also be removed from all workouts.")) {
            try {
                ctx.workouts().deleteExercise(e.id());
                refresh();
            } catch (ServiceException ex) {
                UiUtil.error(this, ex.getMessage());
            }
        }
    }
}
