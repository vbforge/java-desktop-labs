package com.vbforge.homeshelf;

import com.vbforge.homeshelf.io.*;
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.converter.IntegerStringConverter;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class HomeShelfApp extends Application {

    private Stage primaryStage;
    private final ObservableList<MediaItem> items = FXCollections.observableArrayList();
    
    // UI Elements
    private ComboBox<DataFormat> formatCombo;
    private TextField titleField, creatorField, yearField;
    private ComboBox<String> typeCombo;
    private TextField ratingField;
    private Label statusLabel;

    // Window dragging
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");

        root.setTop(buildTopArea());
        root.setLeft(buildSidebar());
        root.setCenter(buildTable());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1000, 650);
        scene.getStylesheets().add(getClass().getResource("/homeshelf.css").toExternalForm());

        stage.setTitle("HomeShelf");
        stage.setScene(scene);
        stage.show();
    }

    private Node buildTopArea() {
        // Title Bar
        Label title = new Label("HomeShelf");
        title.getStyleClass().add("app-title");
        
        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 16));
        titleArea.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleArea.setOnMouseDragged(e -> { primaryStage.setX(e.getScreenX() - xOffset); primaryStage.setY(e.getScreenY() - yOffset); });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> primaryStage.close());

        HBox titleBar = new HBox(titleArea, closeBtn);
        titleBar.getStyleClass().add("titlebar");

        // Toolbar
        formatCombo = new ComboBox<>(FXCollections.observableArrayList(DataFormat.values()));
        formatCombo.getSelectionModel().selectFirst();
        formatCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(DataFormat f) { return f == null ? "" : f.label(); }
            @Override public DataFormat fromString(String s) { return null; }
        });

        Button loadBtn = new Button("Load");
        loadBtn.getStyleClass().add("btn");
        loadBtn.setOnAction(e -> loadData());

        Button saveBtn = new Button("Save");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> saveData());

        HBox toolbar = new HBox(16, formatCombo, loadBtn, saveBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        return new VBox(titleBar, toolbar);
    }

    private Node buildSidebar() {
        Label formTitle = new Label("Item Details");
        formTitle.getStyleClass().add("section-title");

        titleField = new TextField();
        titleField.setPromptText("e.g. Dune");
        
        creatorField = new TextField();
        creatorField.setPromptText("e.g. Frank Herbert");

        yearField = new TextField();
        yearField.setPromptText("1965");
        // Restrict to 4-digit numbers only
        yearField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), null, 
            c -> c.getControlNewText().matches("\\d{0,4}") ? c : null));

        typeCombo = new ComboBox<>(FXCollections.observableArrayList("Book", "Movie", "Video Game"));
        typeCombo.getSelectionModel().selectFirst();
        typeCombo.setMaxWidth(Double.MAX_VALUE);

        ratingField = new TextField();
        ratingField.setPromptText("1-10");
        // Restrict to numbers 1-10
        ratingField.setTextFormatter(new TextFormatter<>(new IntegerStringConverter(), null, 
            c -> c.getControlNewText().matches("\\d{0,2}") ? c : null));

        Button addBtn = new Button("Add Item");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> addItem());

        Button clearBtn = new Button("Clear");
        clearBtn.getStyleClass().add("btn");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setOnAction(e -> clearForm());

        HBox actions = new HBox(10, clearBtn, addBtn);
        HBox.setHgrow(clearBtn, Priority.ALWAYS);
        HBox.setHgrow(addBtn, Priority.ALWAYS);

        VBox sidebar = new VBox(12, 
            formTitle, 
            inputGroup("TITLE", titleField),
            inputGroup("CREATOR", creatorField),
            inputGroup("YEAR", yearField),
            inputGroup("TYPE", typeCombo),
            inputGroup("RATING (1-10)", ratingField),
            new Region(),
            actions
        );
        VBox.setVgrow(sidebar.getChildren().get(6), Priority.ALWAYS);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
    }

    private VBox inputGroup(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("input-label");
        return new VBox(4, l, input);
    }

    private Node buildTable() {
        TableView<MediaItem> table = new TableView<>(items);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getStyleClass().add("table-view");

        TableColumn<MediaItem, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().title()));
        titleCol.setPrefWidth(200);

        TableColumn<MediaItem, String> creatorCol = new TableColumn<>("Creator");
        creatorCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().creator()));
        creatorCol.setPrefWidth(150);

        TableColumn<MediaItem, Integer> yearCol = new TableColumn<>("Year");
        yearCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().year()).asObject());
        yearCol.setPrefWidth(80);

        TableColumn<MediaItem, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().type()));
        typeCol.setPrefWidth(120);

        TableColumn<MediaItem, Integer> ratingCol = new TableColumn<>("Rating");
        ratingCol.setCellValueFactory(c -> new SimpleIntegerProperty(c.getValue().rating()).asObject());
        ratingCol.setPrefWidth(80);
        ratingCol.setStyle("-fx-alignment: CENTER-RIGHT; -fx-text-fill: #f89820; -fx-font-weight: bold;");

        table.getColumns().addAll(List.of(titleCol, creatorCol, yearCol, typeCol, ratingCol));

        // Context menu for delete
        ContextMenu cm = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Item");
        deleteItem.setOnAction(e -> {
            MediaItem selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) items.remove(selected);
        });
        cm.getItems().add(deleteItem);
        table.setContextMenu(cm);

        return table;
    }

    private Node buildStatusBar() {
        statusLabel = new Label("Ready.");
        Label countLabel = new Label();
        countLabel.textProperty().bind(javafx.beans.binding.Bindings.size(items).asString("%d items"));
        countLabel.getStyleClass().add("status-success");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(10, statusLabel, spacer, countLabel);
        bar.getStyleClass().add("status-bar");
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // --- Logic ---

    private void addItem() {
        try {
            String title = titleField.getText().trim();
            String creator = creatorField.getText().trim();
            String yearStr = yearField.getText().trim();
            String type = typeCombo.getValue();
            String ratingStr = ratingField.getText().trim();

            if (title.isEmpty() || creator.isEmpty() || yearStr.isEmpty() || ratingStr.isEmpty()) {
                statusLabel.setText("Please fill all fields.");
                return;
            }

            int year = Integer.parseInt(yearStr);
            int rating = Integer.parseInt(ratingStr);
            
            if (rating < 1 || rating > 10) {
                statusLabel.setText("Rating must be between 1 and 10.");
                return;
            }

            items.add(new MediaItem(title, creator, year, type, rating));
            clearForm();
            statusLabel.setText("Added " + title);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid number format.");
        }
    }

    private void clearForm() {
        titleField.clear();
        creatorField.clear();
        yearField.clear();
        ratingField.clear();
        typeCombo.getSelectionModel().selectFirst();
        titleField.requestFocus();
    }

    private File getDataFile(DataFormat format) {
        String name = "homeshelf." + switch (format) {
            case JSON -> "json";
            case XML -> "xml";
            case SQLITE -> "db";
            case BINARY -> "dat";
            case PROPERTIES -> "props";
            case OBJECT -> "obj";
        };
        // Save to user's home directory
        return Paths.get(System.getProperty("user.home"), ".homeshelf", name).toFile();
    }

    private DataProcess getProcess(DataFormat format) {
        File f = getDataFile(format);
        f.getParentFile().mkdirs(); // Ensure directory exists
        
        return switch (format) {
            case JSON -> new JsonProcess(f);
            case XML -> new XmlProcess(f);
            case SQLITE -> new SqliteProcess(f);
            // You can implement these using your existing code:
            case BINARY, PROPERTIES, OBJECT -> new JsonProcess(f); // Fallback to JSON for preview
        };
    }

    private void saveData() {
        DataFormat format = formatCombo.getValue();
        try {
            getProcess(format).save(items);
            statusLabel.setText("Saved " + items.size() + " items to " + format.name());
        } catch (Exception e) {
            statusLabel.setText("Error saving: " + e.getMessage());
        }
    }

    private void loadData() {
        DataFormat format = formatCombo.getValue();
        try {
            List<MediaItem> loaded = getProcess(format).load();
            items.setAll(loaded);
            statusLabel.setText("Loaded " + loaded.size() + " items from " + format.name());
        } catch (Exception e) {
            statusLabel.setText("Error loading: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}