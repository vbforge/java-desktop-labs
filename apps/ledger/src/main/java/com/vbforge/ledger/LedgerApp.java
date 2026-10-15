package com.vbforge.ledger;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ledger — a small personal finance tracker.
 *
 * Data lives in ~/.ledger/ledger.db (SQLite). Delete that file to reset.
 */
public class LedgerApp extends Application {

    private Db db;
    private TransactionDao txDao;
    private CategoryDao catDao;

    private final ObservableList<TransactionRow> rows = FXCollections.observableArrayList();

    private Label balanceLabel;
    private PieChart chart;

    @Override
    public void start(Stage stage) {
        db = new Db();
        txDao = new TransactionDao(db);
        catDao = new CategoryDao(db);

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0d1117;");
        root.setPadding(new Insets(16));

        root.setTop(buildHeader());
        root.setCenter(buildTable());
        root.setRight(buildSidebar());

        reload();

        Scene scene = new Scene(root, 1050, 620);
//        scene.getStylesheets().add("data:text/css," + java.net.URLEncoder.encode(
//            ".root{-fx-font-family: 'Segoe UI', system-ui, sans-serif;}"
//            + ".table-view{-fx-background-color:#161b22;-fx-control-inner-background:#161b22;"
//            + "-fx-table-cell-border-color:#2a313c;}"
//            + ".table-view .column-header{ -fx-background-color:#1f3a5f;}"
//            + ".table-view .column-header .label{-fx-text-fill:#e6edf3;}"
//            + ".table-row-cell{-fx-background-color:#161b22;-fx-text-fill:#e6edf3;}"
//            + ".table-row-cell:odd{-fx-background-color:#1a1f27;}"
//            + ".table-row-cell:selected{-fx-background-color:#1f3a5f;}"
//            + ".table-cell{-fx-text-fill:#e6edf3;}"
//            + ".label{-fx-text-fill:#e6edf3;}"
//            + ".button{-fx-background-color:#1c2129;-fx-text-fill:#e6edf3;"
//            + "-fx-border-color:#2a313c;-fx-border-radius:6;-fx-background-radius:6;}"
//            + ".button:hover{-fx-background-color:#242b36;}"
//            + ".chart-pie-label{-fx-fill:#e6edf3;}"
//            + ".chart-legend{ -fx-background-color: transparent;}"
//            + ".chart-legend-item{ -fx-text-fill:#e6edf3;}"
//            , java.nio.charset.StandardCharsets.UTF_8));

        scene.getStylesheets().add(LedgerApp.class.getResource("/ledger.css").toExternalForm());

        stage.setTitle("Ledger — personal finance");
        stage.setScene(scene);
        stage.setOnCloseRequest(e -> db.close());
        stage.show();
    }

    // ---------- header: balance + add + delete ----------

    private Node buildHeader() {
        balanceLabel = new Label();
        balanceLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: 700;");

        Button add = new Button("Add transaction");
        add.setOnAction(e -> openAddDialog());

        Button del = new Button("Delete selected");
        del.setOnAction(e -> deleteSelected());

        HBox header = new HBox(14, balanceLabel, spacer(), add, del);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 14, 0));
        return header;
    }

    private static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ---------- table ----------

    private TableView<TransactionRow> table;

    private Node buildTable() {
        table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<TransactionRow, LocalDate> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(c -> c.getValue().dateProperty());
        dateCol.setPrefWidth(110);

        TableColumn<TransactionRow, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(c -> c.getValue().descriptionProperty());
        descCol.setCellFactory(TextFieldTableCell.forTableColumn());
        descCol.setOnEditCommit(e ->
            e.getRowValue().descriptionProperty().set(e.getNewValue()));
        descCol.setPrefWidth(320);

        TableColumn<TransactionRow, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(c -> c.getValue().categoryProperty());
        catCol.setPrefWidth(140);

        TableColumn<TransactionRow, Double> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(c -> c.getValue().amountProperty().asObject());
        amtCol.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
        amtCol.setPrefWidth(110);
        amtCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // NEW: give the column a style class, then target its header in CSS
        amtCol.getStyleClass().add("right-aligned");

        table.getColumns().addAll(List.of(dateCol, descCol, catCol, amtCol));
        return table;
    }

    // ---------- sidebar: chart + categories ----------

    private Node buildSidebar() {
        chart = new PieChart();
        chart.setPrefWidth(320);
        chart.setPrefHeight(280);
        chart.setLegendVisible(true);

        Label catHeader = new Label("Categories");
        catHeader.setStyle("-fx-font-weight: 700; -fx-padding: 12 0 6 0;");

        ListView<Category> catList = new ListView<>();
        catList.setPrefHeight(180);
        catList.setStyle("-fx-background-color: #161b22; -fx-control-inner-background: #161b22;");

        Button addCat = new Button("New category");
        addCat.setOnAction(e -> {
            TextInputDialog dlg = new TextInputDialog();
            dlg.setHeaderText("New category name");
            dlg.showAndWait().ifPresent(name -> {
                if (name.isBlank()) return;
                try {
                    catDao.insert(name.trim());
                    catList.setItems(FXCollections.observableArrayList(catDao.findAll()));
                } catch (SQLException ex) {
                    showError("Could not add category", ex);
                }
            });
        });

        Button delCat = new Button("Delete category");
        delCat.setOnAction(e -> {
            Category sel = catList.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            try {
                catDao.delete(sel.id());
                catList.setItems(FXCollections.observableArrayList(catDao.findAll()));
                reload();
            } catch (SQLException ex) {
                showError("Could not delete category", ex);
            }
        });

        try {
            catList.setItems(FXCollections.observableArrayList(catDao.findAll()));
        } catch (SQLException e) {
            showError("Could not load categories", e);
        }

        HBox catButtons = new HBox(8, addCat, delCat);

        VBox side = new VBox(6, chart, catHeader, catList, catButtons);
        side.setPrefWidth(340);
        side.setPadding(new Insets(0, 0, 0, 16));
        return side;
    }

    // ---------- data loading ----------

    private void reload() {
        try {
            List<Transaction> all = txDao.findAll();

            rows.setAll(all.stream().map(TransactionRow::new).toList());

            double balance = all.stream().mapToDouble(Transaction::amount).sum();
            balanceLabel.setText(String.format("Balance: %,.2f", balance));
            balanceLabel.setTextFill(balance >= 0 ? Color.web("#3fb950") : Color.web("#f85149"));

            // Expenses only, grouped by category name.
            Map<String, Double> byCat = new LinkedHashMap<>();
            for (Transaction t : all) {
                if (t.amount() >= 0) continue;
                String key = t.categoryName() == null ? "Uncategorised" : t.categoryName();
                byCat.merge(key, -t.amount(), Double::sum);
            }

//            chart.getData().clear();
//            byCat.forEach((name, sum) ->
//                chart.getData().add(new PieChart.Data(name + " (" + String.format("%,.2f", sum) + ")", sum)));

            chart.getData().clear();
            if (byCat.isEmpty()) {
                chart.setTitle("No expenses recorded yet");
                chart.setLabelsVisible(false);
                chart.setLegendVisible(false);
            } else {
                chart.setTitle("Spending by category");
                chart.setLabelsVisible(true);
                chart.setLegendVisible(true);
                byCat.forEach((name, sum) ->
                        chart.getData().add(new PieChart.Data(
                                name + " (" + String.format("%,.2f", sum) + ")", sum)));
            }


        } catch (SQLException e) {
            showError("Could not load data", e);
        }
    }

    // ---------- actions ----------

//    private void openAddDialog() {
//        Dialog<ButtonType> dlg = new Dialog<>();
//        dlg.setTitle("Add transaction");
//        dlg.setHeaderText(null);
//
//        DatePicker date = new DatePicker(LocalDate.now());
//        TextField desc = new TextField();
//        desc.setPromptText("Description");
//        TextField amount = new TextField();
//        amount.setPromptText("e.g. -42.50 for an expense");
//
//        ComboBox<Category> catBox = new ComboBox<>();
//        try {
//            catBox.setItems(FXCollections.observableArrayList(catDao.findAll()));
//        } catch (SQLException e) {
//            showError("Could not load categories", e);
//        }
//        catBox.getSelectionModel().selectFirst();
//
//        GridPane gp = new GridPane();
//        gp.setHgap(10); gp.setVgap(10);
//        gp.setPadding(new Insets(14));
//        gp.addRow(0, new Label("Date"), date);
//        gp.addRow(1, new Label("Description"), desc);
//        gp.addRow(2, new Label("Amount"), amount);
//        gp.addRow(3, new Label("Category"), catBox);
//
//        dlg.getDialogPane().setContent(gp);
//        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
//
//        dlg.showAndWait().ifPresent(bt -> {
//            if (bt != ButtonType.OK) return;
//            try {
//                double amt = Double.parseDouble(amount.getText().trim());
//                String d = desc.getText().trim();
//                if (d.isEmpty()) return;
//                Category c = catBox.getSelectionModel().getSelectedItem();
//                txDao.insert(date.getValue(), d, amt, c == null ? null : c.id());
//                reload();
//            } catch (NumberFormatException nfe) {
//                showError("Amount must be a number", nfe);
//            } catch (SQLException ex) {
//                showError("Could not save transaction", ex);
//            }
//        });
//    }

    private void openAddDialog() {
        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Add transaction");
        dlg.setHeaderText(null);

        DatePicker date = new DatePicker(LocalDate.now());
        TextField desc = new TextField();
        desc.setPromptText("Description");

        TextField amount = new TextField();
        amount.setPromptText("42.50");

        ToggleGroup kindGroup = new ToggleGroup();
        RadioButton expense = new RadioButton("Expense");
        RadioButton income  = new RadioButton("Income");
        expense.setToggleGroup(kindGroup);
        income.setToggleGroup(kindGroup);
        expense.setSelected(true);
        HBox kindBox = new HBox(12, expense, income);

        ComboBox<Category> catBox = new ComboBox<>();
        try {
            catBox.setItems(FXCollections.observableArrayList(catDao.findAll()));
        } catch (SQLException e) {
            showError("Could not load categories", e);
        }
        catBox.getSelectionModel().selectFirst();

        GridPane gp = new GridPane();
        gp.setHgap(10); gp.setVgap(10);
        gp.setPadding(new Insets(14));
        gp.addRow(0, new Label("Date"),        date);
        gp.addRow(1, new Label("Description"), desc);
        gp.addRow(2, new Label("Type"),        kindBox);
        gp.addRow(3, new Label("Amount"),      amount);
        gp.addRow(4, new Label("Category"),    catBox);

        dlg.getDialogPane().setContent(gp);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dlg.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            try {
                String raw = amount.getText().trim();
                if (raw.isEmpty()) {
                    showError("Amount is required", new IllegalArgumentException("Enter an amount."));
                    return;
                }
                double magnitude = Math.abs(Double.parseDouble(raw));
                double signed    = expense.isSelected() ? -magnitude : magnitude;

                String d = desc.getText().trim();
                if (d.isEmpty()) {
                    showError("Description is required",
                            new IllegalArgumentException("Give the transaction a description."));
                    return;
                }
                Category c = catBox.getSelectionModel().getSelectedItem();
                txDao.insert(date.getValue(), d, signed, c == null ? null : c.id());
                reload();
            } catch (NumberFormatException nfe) {
                showError("Amount must be a number", nfe);
            } catch (SQLException ex) {
                showError("Could not save transaction", ex);
            }
        });
    }

    private void deleteSelected() {
        TransactionRow sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        try {
            txDao.delete(sel.getId());
            reload();
        } catch (SQLException e) {
            showError("Could not delete transaction", e);
        }
    }

    private static void showError(String header, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(header);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}