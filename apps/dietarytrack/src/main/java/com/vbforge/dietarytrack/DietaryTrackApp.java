package com.vbforge.dietarytrack;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

public class DietaryTrackApp extends Application {

    private static final System.Logger LOG = System.getLogger(DietaryTrackApp.class.getName());
    private static final String STYLESHEET = "/dietarytrack.css";

    private DietaryDao dao;
    private CsvExporter csv;

    private HistoryPanel history;
    private StatusBar status;
    private ComboBox<ExportMode> exportCombo;

    @Override
    public void start(Stage stage) {
        try {
            dao = new DietaryDao();
        } catch (StorageException e) {
            LOG.log(System.Logger.Level.ERROR, "Startup failed", e);
            showFatalError(e);
            Platform.exit();
            return;
        }
        csv = new CsvExporter();

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("DietaryTrack");

        WindowSupport window = new WindowSupport(stage);
        status = new StatusBar(AppPaths.dataDir().toString());
        history = new HistoryPanel();
        EntryFormPanel form = new EntryFormPanel(status, this::save);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(new VBox(new TitleBar(stage, window, "DietaryTrack"), buildHeader()));
        root.setCenter(buildMainArea(form));
        root.setBottom(status);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource(STYLESHEET), "Missing " + STYLESHEET).toExternalForm());

        window.fitToScreen(1280, 780, 1080, 620);
        window.installResizing(scene);
        stage.setScene(scene);
        stage.show();

        refreshHistory();
    }

    @Override
    public void stop() {
        if (dao != null) {
            dao.close();
        }
    }

    private Node buildHeader() {
        Label title = new Label("Dietary Tracker");
        title.getStyleClass().add("main-title");
        Label subtitle = new Label("Log your daily habits — water, meals, exercise, dietary intake");
        subtitle.getStyleClass().add("subtitle");
        VBox titles = new VBox(4, title, subtitle);

        exportCombo = new ComboBox<>();
        exportCombo.getItems().addAll(ExportMode.values());
        exportCombo.getSelectionModel().selectFirst();
        exportCombo.getStyleClass().addAll("input", "export-combo");
        exportCombo.setPrefWidth(200);

        Label exportLabel = new Label("EXPORT TO");
        exportLabel.getStyleClass().add("export-label");
        VBox exportBox = new VBox(5, exportLabel, exportCombo);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(20, titles, spacer, exportBox);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header");
        return header;
    }

    private Node buildMainArea(EntryFormPanel form) {
        HBox layout = new HBox(16, form, history);
        layout.setPadding(new Insets(16));
        layout.getStyleClass().add("layout");
        HBox.setHgrow(history, Priority.ALWAYS);
        return layout;
    }

    /** Stores the entry as chosen in the export selector. Returns true when the form should be cleared. */
    private boolean save(DietaryEntry entry) {
        ExportMode mode = exportCombo.getValue();
        boolean savedToDb = false;

        try {
            if (mode.usesDatabase()) {
                dao.insert(entry);
                savedToDb = true;
            }
        } catch (StorageException e) {
            LOG.log(System.Logger.Level.ERROR, "Database save failed", e);
            status.error("⚠ Could not save the entry: " + rootMessage(e));
            return false;
        }

        try {
            if (mode.usesCsv()) {
                csv.append(entry);
            }
        } catch (StorageException e) {
            LOG.log(System.Logger.Level.ERROR, "CSV export failed", e);
            if (savedToDb) {
                refreshHistory();
                status.error("⚠ Saved to the database, but the CSV export failed: " + rootMessage(e));
                return true; // the entry exists already; keeping the form would create a duplicate
            }
            status.error("⚠ Could not save the entry: " + rootMessage(e));
            return false;
        }

        if (savedToDb) {
            refreshHistory();
        }
        status.success(switch (mode) {
            case SQLITE_AND_CSV -> "✓ Entry saved to SQLite and CSV.";
            case SQLITE_ONLY -> "✓ Entry saved to SQLite.";
            case CSV_ONLY -> "✓ Entry saved to CSV (history only lists SQLite entries).";
        });
        return true;
    }

    private void refreshHistory() {
        try {
            history.setEntries(dao.findAll());
        } catch (StorageException e) {
            LOG.log(System.Logger.Level.ERROR, "Loading history failed", e);
            status.error("⚠ Could not load history: " + rootMessage(e));
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        String msg = root.getMessage();
        return msg == null || msg.isBlank() ? root.getClass().getSimpleName() : msg;
    }

    private void showFatalError(StorageException e) {
        Alert alert = new Alert(Alert.AlertType.ERROR,
                "DietaryTrack could not open its database in\n" + AppPaths.dataDir()
                        + "\n\n" + rootMessage(e), ButtonType.CLOSE);
        alert.setTitle("DietaryTrack");
        alert.setHeaderText("Startup failed");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
