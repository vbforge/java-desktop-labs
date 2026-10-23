package com.vbforge.dietarytrack;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Summary cards plus the table of saved entries. */
final class HistoryPanel extends VBox {

    private static final double ROW_HEIGHT = 40;

    private final Label statTotal = statValue("0");
    private final Label statAvgWater = statValue("0.0");
    private final Label statAvgMeals = statValue("0.0");
    private final TableView<DietaryEntry> table = new TableView<>();

    HistoryPanel() {
        super(16);
        getStyleClass().add("history-panel");

        HBox stats = new HBox(12,
                statCard("TOTAL ENTRIES", statTotal),
                statCard("AVG WATER / DAY", statAvgWater),
                statCard("AVG MEALS / DAY", statAvgMeals));

        configureTable();
        VBox.setVgrow(table, Priority.ALWAYS);
        getChildren().addAll(stats, table);
    }

    void setEntries(List<DietaryEntry> entries) {
        table.setItems(FXCollections.observableArrayList(entries));
        Stats stats = Stats.of(entries);
        statTotal.setText(Integer.toString(stats.total()));
        statAvgWater.setText(String.format(Locale.ROOT, "%.1f", stats.avgWater()));
        statAvgMeals.setText(String.format(Locale.ROOT, "%.1f", stats.avgMeals()));
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setFixedCellSize(ROW_HEIGHT);
        table.setPlaceholder(new Label("No entries yet. Fill out the form to add one."));
        table.getPlaceholder().getStyleClass().add("table-placeholder");

        TableColumn<DietaryEntry, String> dateCol = textColumn("Date", 110, 90, DietaryEntry::date);
        TableColumn<DietaryEntry, String> nameCol = textColumn("Name", 200, 90,
                e -> e.firstName() + " " + e.lastName());

        TableColumn<DietaryEntry, Number> waterCol = new TableColumn<>("Water");
        waterCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().water()));
        waterCol.setPrefWidth(70);
        waterCol.setMinWidth(55);
        waterCol.getStyleClass().add("num-col");

        TableColumn<DietaryEntry, Number> mealsCol = new TableColumn<>("Meals");
        mealsCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().meals()));
        mealsCol.setPrefWidth(70);
        mealsCol.setMinWidth(55);
        mealsCol.getStyleClass().add("num-col");

        TableColumn<DietaryEntry, String> milesCol = textColumn("Walking", 140, 100, DietaryEntry::miles);

        TableColumn<DietaryEntry, Number> weightCol = new TableColumn<>("Weight");
        weightCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().weight()));
        weightCol.setPrefWidth(90);
        weightCol.setMinWidth(70);
        weightCol.getStyleClass().add("num-col");
        weightCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format(Locale.ROOT, "%.1f kg", item.doubleValue()));
            }
        });

        TableColumn<DietaryEntry, DietaryEntry> dietaryCol = new TableColumn<>("Dietary");
        dietaryCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        dietaryCol.setPrefWidth(110);
        dietaryCol.setMinWidth(80);
        dietaryCol.setSortable(false);
        dietaryCol.setCellFactory(col -> new DietaryCell());

        table.getColumns().addAll(List.of(dateCol, nameCol, waterCol, mealsCol, milesCol, weightCol, dietaryCol));
    }

    private static TableColumn<DietaryEntry, String> textColumn(
            String title, double pref, double min, java.util.function.Function<DietaryEntry, String> getter) {
        TableColumn<DietaryEntry, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getter.apply(c.getValue())));
        col.setPrefWidth(pref);
        col.setMinWidth(min);
        return col;
    }

    private static Label statValue(String initial) {
        Label l = new Label(initial);
        l.getStyleClass().add("stat-value");
        return l;
    }

    private static VBox statCard(String caption, Label value) {
        Label lbl = new Label(caption);
        lbl.getStyleClass().add("stat-label");
        VBox card = new VBox(4, lbl, value);
        card.getStyleClass().addAll("card", "stat-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** Shows W / S / D badges for the dietary flags of a row. */
    private static final class DietaryCell extends TableCell<DietaryEntry, DietaryEntry> {

        DietaryCell() {
            setAlignment(Pos.CENTER_LEFT);
        }

        @Override
        protected void updateItem(DietaryEntry item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setTooltip(null);
                return;
            }
            HBox box = new HBox(4);
            box.setAlignment(Pos.CENTER_LEFT);
            List<String> names = new ArrayList<>();
            if (item.wheat()) { box.getChildren().add(pill("W")); names.add("Wheat"); }
            if (item.sugar()) { box.getChildren().add(pill("S")); names.add("Sugar"); }
            if (item.dairy()) { box.getChildren().add(pill("D")); names.add("Dairy"); }
            if (names.isEmpty()) {
                Label none = new Label("—");
                none.getStyleClass().add("muted");
                box.getChildren().add(none);
                setTooltip(null);
            } else {
                setTooltip(new Tooltip(String.join(", ", names)));
            }
            setGraphic(box);
        }

        private static Label pill(String text) {
            Label l = new Label(text);
            l.getStyleClass().add("dietary-pill");
            return l;
        }
    }
}
