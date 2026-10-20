package com.vbforge.teamcraft;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class TeamCraftApp extends javafx.application.Application {

    private Stage primaryStage;
    private double xOffset = 0;
    private double yOffset = 0;

    private final Db db = new Db();
    private final EmployeeDao dao = new EmployeeDao(db);

    private TableView<Employee> table;
    private ObservableList<Employee> masterData;
    private FilteredList<Employee> filteredData;
    private Employee selectedEmployee;

    // UI Refs
    private Label statTotal, statAvgAge;
    private TextField searchField, nameField, jobField, ageField;
    private ComboBox<String> genderCombo;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTopArea());
        root.setCenter(buildMainArea());

        Scene scene = new Scene(root, 1100, 700);
        scene.getStylesheets().add(getClass().getResource("/teamcraft.css").toExternalForm());

        stage.setTitle("TeamCraft");
        stage.setScene(scene);
        stage.show();

        loadData();
    }

    private Node buildTopArea() {
        Label title = new Label("TeamCraft");
        title.getStyleClass().add("app-title");

        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 16));
        titleArea.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleArea.setOnMouseDragged(e -> { primaryStage.setX(e.getScreenX() - xOffset); primaryStage.setY(e.getScreenY() - yOffset); });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> { db.close(); primaryStage.close(); });

        HBox titleBar = new HBox(titleArea, closeBtn);
        titleBar.getStyleClass().add("titlebar");

        statTotal = new Label("0");
        statTotal.getStyleClass().add("stat-value");
        statAvgAge = new Label("0.0");
        statAvgAge.getStyleClass().add("stat-value");

        VBox statsBox = new VBox(4, new Label("TOTAL STAFF"), statTotal);
        statsBox.getChildren().get(0).getStyleClass().add("stat-label");
        VBox avgBox = new VBox(4, new Label("AVG AGE"), statAvgAge);
        avgBox.getChildren().get(0).getStyleClass().add("stat-label");
        
        HBox statsRow = new HBox(24, statsBox, avgBox);

        searchField = new TextField();
        searchField.setPromptText("Search employees...");
        searchField.getStyleClass().add("search-input");
        searchField.textProperty().addListener((obs, old, val) -> {
            filteredData.setPredicate(emp -> {
                if (val == null || val.isEmpty()) return true;
                String lower = val.toLowerCase();
                return emp.name().toLowerCase().contains(lower) ||
                       emp.job().toLowerCase().contains(lower) ||
                       emp.gender().toLowerCase().contains(lower);
            });
            updateStats();
        });

        Button exportBtn = new Button("Export CSV");
        exportBtn.getStyleClass().add("btn");
        exportBtn.setOnAction(e -> exportCsv());

        HBox toolbar = new HBox(12, searchField, exportBtn);
        toolbar.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(toolbar, Priority.ALWAYS);

        HBox header = new HBox(20, statsRow, toolbar);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");

        return new VBox(titleBar, header);
    }

    private Node buildMainArea() {
        table = new TableView<>();
        table.getStyleClass().add("table-view");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setFixedCellSize(40);
        table.setMinWidth(400); // Prevent horizontal distortion

        TableColumn<Employee, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));

        TableColumn<Employee, String> jobCol = new TableColumn<>("Job Title");
        jobCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().job()));

        TableColumn<Employee, Integer> ageCol = new TableColumn<>("Age");
        ageCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().age()).asObject());
        ageCol.setStyle("-fx-alignment: CENTER-LEFT;");

        TableColumn<Employee, String> genderCol = new TableColumn<>("Gender");
        genderCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().gender()));

        table.getColumns().addAll(List.of(nameCol, jobCol, ageCol, genderCol));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> populateForm(sel));

        VBox tableContainer = new VBox(table);
        tableContainer.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(tableContainer, Priority.ALWAYS);

        // Form Panel
        nameField = new TextField();
        jobField = new TextField();
        ageField = new TextField();
        ageField.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().matches("\\d{0,3}") ? c : null));
        
        genderCombo = new ComboBox<>(FXCollections.observableArrayList("Male", "Female", "Other"));
        genderCombo.setMaxWidth(Double.MAX_VALUE);

        Button saveBtn = new Button("Save / Update");
        saveBtn.getStyleClass().addAll("btn", "btn-primary");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setOnAction(e -> saveEmployee());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearForm());

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().addAll("btn", "btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> deleteEmployee());

        HBox actionRow = new HBox(10, clearBtn, deleteBtn);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);

        VBox formPanel = new VBox(16,
            new Label("Employee Details") {{ getStyleClass().add("panel-title"); }},
            createInputGroup("Name", nameField),
            createInputGroup("Job Title", jobField),
            createInputGroup("Age", ageField),
            createInputGroup("Gender", genderCombo),
            new Region(),
            saveBtn,
            actionRow
        );
        VBox.setVgrow(formPanel.getChildren().get(5), Priority.ALWAYS);
        formPanel.getStyleClass().add("form-panel");
        formPanel.setPrefWidth(300);
        formPanel.setMinWidth(300);
        formPanel.setMaxWidth(300);

        HBox mainArea = new HBox(16, tableContainer, formPanel);
        mainArea.setPadding(new Insets(16));
        mainArea.setAlignment(Pos.TOP_LEFT);
        return mainArea;
    }

    private VBox createInputGroup(String label, Control input) {
        Label l = new Label(label);
        l.getStyleClass().add("input-label");
        VBox box = new VBox(6, l, input);
        return box;
    }

    // --- Logic ---

    private void loadData() {
        masterData = FXCollections.observableArrayList(dao.findAll());
        filteredData = new FilteredList<>(masterData, p -> true);
        SortedList<Employee> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
        
        table.setItems(sortedData);
        
        // FIX: Cap the maximum height so it scrolls instead of pushing buttons off-screen
        table.prefHeightProperty().unbind();
        table.prefHeightProperty().bind(
            Bindings.min(
                Bindings.size(sortedData).multiply(table.getFixedCellSize()).add(42),
                480.0 // Maximum height in pixels before scrolling
            )
        );
        
        updateStats();
    }

    private void updateStats() {
        int size = filteredData.size();
        statTotal.setText(String.valueOf(size));

        if (size == 0) {
            statAvgAge.setText("0.0");
            return;
        }

        double avg = filteredData.stream().mapToInt(Employee::age).average().orElse(0.0);
        statAvgAge.setText(String.format("%.1f", avg));
    }

    private void populateForm(Employee emp) {
        selectedEmployee = emp;
        if (emp == null) {
            clearForm();
            return;
        }
        nameField.setText(emp.name());
        jobField.setText(emp.job());
        ageField.setText(String.valueOf(emp.age()));
        genderCombo.setValue(emp.gender());
    }

    private void clearForm() {
        selectedEmployee = null;
        nameField.clear();
        jobField.clear();
        ageField.clear();
        genderCombo.setValue(null);
        table.getSelectionModel().clearSelection();
    }

    private void saveEmployee() {
        String name = nameField.getText().trim();
        String job = jobField.getText().trim();
        String ageStr = ageField.getText().trim();
        String gender = genderCombo.getValue();

        if (name.isEmpty() || job.isEmpty() || ageStr.isEmpty() || gender == null) {
            showAlert("Please fill all fields.");
            return;
        }

        int age = Integer.parseInt(ageStr);
        if (age < 16 || age > 100) {
            showAlert("Age must be between 16 and 100.");
            return;
        }

        if (selectedEmployee == null) {
            dao.insert(new Employee(0, name, job, age, gender));
        } else {
            dao.update(new Employee(selectedEmployee.id(), name, job, age, gender));
        }

        clearForm();
        loadData();
    }

    private void deleteEmployee() {
        if (selectedEmployee == null) {
            showAlert("Select an employee to delete.");
            return;
        }
        dao.delete(selectedEmployee.id());
        clearForm();
        loadData();
    }

    private void exportCsv() {
        FileChooser fc = new FileChooser();
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        fc.setInitialFileName("employees.csv");
        File file = fc.showSaveDialog(primaryStage);
        
        if (file != null) {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("ID,Name,Job Title,Age,Gender\n");
                for (Employee e : filteredData) {
                    writer.write(String.format("%d,\"%s\",\"%s\",%d,\"%s\"\n",
                            e.id(), e.name(), e.job(), e.age(), e.gender()));
                }
                showAlert("Exported " + filteredData.size() + " records to " + file.getName());
            } catch (IOException e) {
                showAlert("Error exporting: " + e.getMessage());
            }
        }
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}