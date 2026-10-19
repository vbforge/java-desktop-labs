package com.vbforge.atlasdesk;

import javafx.animation.Animation;
import javafx.animation.ScaleTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToLongFunction;

import javafx.stage.FileChooser;
import java.io.File;

public class AtlasDeskApp extends Application {

    private static final String ALL = "All Continents";

    private Stage primaryStage;
    private Db db;
    private CountryDao dao;

    private final ObservableList<Country> allCountries = FXCollections.observableArrayList();
    private FilteredList<Country> filtered;
    private TableView<Country> table;
    private TextField searchField;
    private ComboBox<String> continentFilter;
    private Label statusLabel;
    private Image worldMap;

    // detail card
    private VBox cardContent;
    private ImageView cardFlag;
    private Label cardName, cardContinent, cardCapital, cardCountryPop, cardCapitalPop, cardArea, cardCoords;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        try {
            db = new Db();
            dao = new CountryDao(db);
        } catch (RuntimeException e) {
            new Alert(Alert.AlertType.ERROR, "Could not open the database:\n" + e.getMessage()).showAndWait();
            Platform.exit();
            return;
        }

        stage.initStyle(StageStyle.UNDECORATED);
        filtered = new FilteredList<>(allCountries, c -> true);

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar());
        root.setCenter(buildMainLayout());
        root.setBottom(buildStatusBar());

        run(() -> reload(0));

        Scene scene = new Scene(root, 1200, 720);
        scene.getStylesheets().add(Fx.css());

        stage.setMinWidth(980);
        stage.setMinHeight(600);
        stage.setTitle("AtlasDesk");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (db != null) db.close();
    }

    // ------------------------------------------------------------------ title bar

    private Node buildTitleBar() {
        Label logo = new Label("");
        logo.getStyleClass().add("app-logo");
        Label title = new Label("AtlasDesk");
        title.getStyleClass().add("app-title");
        Label subtitle = new Label("Country reference");
        subtitle.getStyleClass().add("app-subtitle");

        HBox left = new HBox(8, logo, title, subtitle);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(left, Priority.ALWAYS);
        Fx.makeDraggable(left, primaryStage);
        left.setOnMouseClicked(e -> { if (e.getClickCount() == 2) toggleMaximize(); });

        Button min = new Button("—");
        min.getStyleClass().add("win-btn");
        min.setOnAction(e -> primaryStage.setIconified(true));

        Button max = new Button("▢");
        max.getStyleClass().add("win-btn");
        max.setOnAction(e -> toggleMaximize());

        Button close = new Button("×");
        close.getStyleClass().add("win-btn-close");
        close.setOnAction(e -> primaryStage.close());

        HBox bar = new HBox(left, min, max, close);
        bar.setAlignment(Pos.CENTER);
        bar.getStyleClass().add("titlebar");
        return bar;
    }

    private void toggleMaximize() {
        primaryStage.setMaximized(!primaryStage.isMaximized());
    }

    // ------------------------------------------------------------------ main layout

    private Node buildMainLayout() {
        HBox main = new HBox(16, buildLeftPanel(), buildCardPanel());
        main.setPadding(new Insets(16));
        return main;
    }

    private Node buildLeftPanel() {
        searchField = new TextField();
        searchField.setPromptText("Search by country, capital or code…");
        searchField.setMinWidth(140);
        searchField.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.textProperty().addListener((o, a, b) -> updateFilters());

        continentFilter = new ComboBox<>();
        continentFilter.setPrefWidth(170);
        continentFilter.valueProperty().addListener((o, a, b) -> updateFilters());

        Button importBtn = new Button("Import JSON");
        importBtn.getStyleClass().add("btn-secondary");
        importBtn.setOnAction(e -> importCountries());

        Button addBtn = new Button("+  Add country");
        addBtn.getStyleClass().add("btn-primary");
        addBtn.setOnAction(e -> addCountry());

        HBox toolbar = new HBox(12, searchField, continentFilter, importBtn, addBtn);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");

        VBox panel = new VBox(toolbar, buildTable());
        panel.getStyleClass().add("panel");
        panel.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(table, Priority.ALWAYS);
        HBox.setHgrow(panel, Priority.ALWAYS);
        return panel;
    }

    // ------------------------------------------------------------------ table

    private TableView<Country> buildTable() {
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("No countries match your filters"));

        // SortedList is required, otherwise clicking column headers cannot sort a FilteredList
        SortedList<Country> sorted = new SortedList<>(filtered);
        sorted.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sorted);

        TableColumn<Country, String> flagCol = new TableColumn<>("");
        flagCol.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().code()));
        flagCol.setCellFactory(col -> new FlagCell());
        flagCol.setSortable(false);
        flagCol.setMinWidth(64);
        flagCol.setPrefWidth(64);
        flagCol.setMaxWidth(64);

        TableColumn<Country, String> nameCol = textCol("Country", Country::name);
        nameCol.getStyleClass().add("name-col");
        nameCol.setPrefWidth(210);

        TableColumn<Country, String> contCol = textCol("Continent", Country::continent);
        contCol.setCellFactory(col -> new TableCell<>() {
            private final Label chip = new Label();
            { chip.getStyleClass().add("chip"); }
            @Override protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setGraphic(null);
                } else {
                    chip.setText(value);
                    setGraphic(chip);
                }
            }
        });
        contCol.setPrefWidth(150);

        TableColumn<Country, String> capCol = textCol("Capital", Country::capital);
        capCol.setPrefWidth(150);

        TableColumn<Country, Long> popCol = numCol("Population", Country::countryPopulation, Fx::compact);
        TableColumn<Country, Long> capPopCol = numCol("Capital pop.", Country::capitalPopulation, Fx::compact);
        TableColumn<Country, Long> areaCol = numCol("Area km²", Country::area, Fx::full);

        List<TableColumn<Country, ?>> columns =
                List.of(flagCol, nameCol, contCol, capCol, popCol, capPopCol, areaCol);
        table.getColumns().addAll(columns);

        table.getSelectionModel().selectedItemProperty().addListener((o, a, sel) -> updateCard(sel));
        table.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.DELETE) deleteCountry(selected());
        });
        table.setRowFactory(tv -> {
            TableRow<Country> row = new TableRow<>();
            MenuItem edit = new MenuItem("Edit…");
            edit.setOnAction(e -> editCountry(row.getItem()));
            MenuItem delete = new MenuItem("Delete…");
            delete.setOnAction(e -> deleteCountry(row.getItem()));
            ContextMenu menu = new ContextMenu(edit, delete);
            row.contextMenuProperty().bind(
                    Bindings.when(row.emptyProperty()).then((ContextMenu) null).otherwise(menu));
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) editCountry(row.getItem());
            });
            return row;
        });
        return table;
    }

    private static TableColumn<Country, String> textCol(String title, Function<Country, String> getter) {
        TableColumn<Country, String> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getter.apply(c.getValue())));
        return col;
    }

    private static TableColumn<Country, Long> numCol(String title, ToLongFunction<Country> getter,
                                                     LongFunction<String> format) {
        TableColumn<Country, Long> col = new TableColumn<>(title);
        col.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(getter.applyAsLong(c.getValue())));
        col.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(Long value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(format.apply(value));
                    setTooltip(new Tooltip(Fx.full(value)));
                }
            }
        });
        col.getStyleClass().add("num-col");
        col.setPrefWidth(120);
        return col;
    }

    /** Flag with the ISO code shown underneath as a fallback (offline / not loaded yet). */
    private static final class FlagCell extends TableCell<Country, String> {
        private final StackPane box = new StackPane();
        private final Label fallback = new Label();
        private final ImageView view = new ImageView();

        FlagCell() {
            fallback.getStyleClass().add("flag-fallback");
            view.setFitWidth(28);
            view.setFitHeight(20);
            view.setPreserveRatio(true);
            box.getChildren().addAll(fallback, view);
            box.setPrefSize(28, 20);
            box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
            setAlignment(Pos.CENTER);
        }

        @Override
        protected void updateItem(String code, boolean empty) {
            super.updateItem(code, empty);
            if (empty || code == null) {
                setGraphic(null);
                return;
            }
            fallback.setText(code);
            view.setImage(FlagService.get(code, 40));
            setGraphic(box);
        }
    }

    // ------------------------------------------------------------------ detail card

    private Node buildCardPanel() {
        cardFlag = new ImageView();
        cardFlag.setFitWidth(64);
        cardFlag.setFitHeight(44);
        cardFlag.setPreserveRatio(true);
        StackPane flagFrame = new StackPane(cardFlag);
        flagFrame.getStyleClass().add("flag-frame");
        flagFrame.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        cardName = new Label();
        cardName.getStyleClass().add("card-title");
        cardName.setWrapText(true);
        cardContinent = new Label();
        cardContinent.getStyleClass().add("chip");

        VBox nameBox = new VBox(6, cardName, cardContinent);
        nameBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(nameBox, Priority.ALWAYS);
        HBox head = new HBox(14, flagFrame, nameBox);
        head.setAlignment(Pos.CENTER_LEFT);

        cardCapital = new Label();
        cardCountryPop = new Label();
        cardCapitalPop = new Label();
        cardArea = new Label();
        cardCoords = new Label();
        VBox details = new VBox(
                detailRow("Capital", cardCapital),
                detailRow("Country population", cardCountryPop),
                detailRow("Capital population", cardCapitalPop),
                detailRow("Area (km²)", cardArea),
                detailRow("Coordinates", cardCoords));

        Button mapBtn = new Button("View on map");
        mapBtn.getStyleClass().add("map-btn");
        mapBtn.setMaxWidth(Double.MAX_VALUE);
        mapBtn.setOnAction(e -> showMapDialog());

        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        editBtn.setOnAction(e -> editCountry(selected()));

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-danger");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.setOnAction(e -> deleteCountry(selected()));

        HBox actions = new HBox(10, editBtn, deleteBtn);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);

        cardContent = new VBox(22, head, details, mapBtn, actions);
        cardContent.setVisible(false);
        cardContent.managedProperty().bind(cardContent.visibleProperty());
        StackPane.setAlignment(cardContent, Pos.TOP_LEFT);

        Label emptyTitle = new Label("No country selected");
        emptyTitle.getStyleClass().add("card-empty-title");
        Label emptyHint = new Label("Pick a country in the table to see its details, or add a new one.");
        emptyHint.getStyleClass().add("card-empty-hint");
        emptyHint.setWrapText(true);
        VBox cardEmpty = new VBox(8, emptyTitle, emptyHint);
        cardEmpty.setAlignment(Pos.CENTER);
        cardEmpty.visibleProperty().bind(cardContent.visibleProperty().not());
        cardEmpty.managedProperty().bind(cardEmpty.visibleProperty());

        StackPane host = new StackPane(cardEmpty, cardContent);
        host.getStyleClass().add("card-panel");
        host.setMinWidth(340);
        host.setPrefWidth(340);
        host.setMaxWidth(340);
        return host;
    }

    private HBox detailRow(String labelText, Label valueLabel) {
        Label lbl = new Label(labelText);
        lbl.getStyleClass().add("detail-label");
        valueLabel.getStyleClass().add("detail-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(lbl, spacer, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("detail-row");
        return row;
    }

    private void updateCard(Country c) {
        if (c == null) {
            cardContent.setVisible(false);
            return;
        }
        cardContent.setVisible(true);
        cardFlag.setImage(FlagService.get(c.code(), 80));
        cardName.setText(c.name());
        cardContinent.setText(c.continent());
        cardCapital.setText(c.capital());
        cardCountryPop.setText(Fx.full(c.countryPopulation()));
        cardCapitalPop.setText(Fx.full(c.capitalPopulation()));
        cardArea.setText(Fx.full(c.area()));
        cardCoords.setText(String.format(Locale.US, "%.2f°, %.2f°", c.latitude(), c.longitude()));
    }

    // ------------------------------------------------------------------ status bar

    private Node buildStatusBar() {
        statusLabel = new Label();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label grip = new Label("◢");
        grip.getStyleClass().add("resize-grip");
        grip.setCursor(Cursor.SE_RESIZE);
        grip.setOnMouseDragged(e -> {
            if (primaryStage.isMaximized()) return;
            primaryStage.setWidth(Math.max(primaryStage.getMinWidth(), e.getScreenX() - primaryStage.getX()));
            primaryStage.setHeight(Math.max(primaryStage.getMinHeight(), e.getScreenY() - primaryStage.getY()));
        });

        HBox bar = new HBox(10, statusLabel, spacer, grip);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("status-bar");
        return bar;
    }

    // ------------------------------------------------------------------ data & filters

    private Country selected() {
        return table.getSelectionModel().getSelectedItem();
    }

    private List<String> knownContinents() {
        return allCountries.stream().map(Country::continent).distinct().sorted().toList();
    }

    private void reload(int selectId) {
        allCountries.setAll(dao.findAll());

        String current = continentFilter.getValue();
        List<String> items = new ArrayList<>();
        items.add(ALL);
        items.addAll(knownContinents());
        continentFilter.setItems(FXCollections.observableArrayList(items));
        continentFilter.setValue(items.contains(current) ? current : ALL);

        updateFilters();

        if (selectId > 0) {
            allCountries.stream().filter(c -> c.id() == selectId).findFirst().ifPresent(target -> {
                if (!filtered.contains(target)) { // make sure the new/edited row is visible
                    searchField.clear();
                    continentFilter.setValue(ALL);
                }
                table.getSelectionModel().select(target);
                table.scrollTo(target);
            });
        }
    }

    private void updateFilters() {
        String q = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String cont = continentFilter.getValue();
        boolean anyContinent = cont == null || ALL.equals(cont);

        filtered.setPredicate(c ->
                (anyContinent || c.continent().equals(cont))
                        && (q.isEmpty()
                        || c.name().toLowerCase(Locale.ROOT).contains(q)
                        || c.capital().toLowerCase(Locale.ROOT).contains(q)
                        || c.code().toLowerCase(Locale.ROOT).equals(q)));

        long continents = allCountries.stream().map(Country::continent).distinct().count();
        statusLabel.setText(String.format("%d of %d countries  ·  %d continents",
                filtered.size(), allCountries.size(), continents));
    }

    // ------------------------------------------------------------------ add / edit / delete

    private void addCountry() {
        CountryFormDialog.show(primaryStage, null, knownContinents(), code -> dao.codeExists(code, 0))
                .ifPresent(c -> run(() -> reload(dao.insert(c).id())));
    }

    private void importCountries() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import countries from JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file == null) return;

        run(() -> {
            CountryImporter.Result result =
                    CountryImporter.importFile(file.toPath(), dao, knownContinents());
            reload(0);
            Fx.info(primaryStage, "Import finished", summary(result));
        });
    }

    private static String summary(CountryImporter.Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Added: ").append(r.added()).append('\n');
        sb.append("Skipped, already in database: ").append(r.alreadyInDb()).append('\n');
        sb.append("Skipped, repeated in the file: ").append(r.duplicatesInFile()).append('\n');
        sb.append("Skipped, invalid: ").append(r.invalid().size());

        int shown = Math.min(8, r.invalid().size());
        if (shown > 0) {
            sb.append("\n\n");
            for (int i = 0; i < shown; i++) sb.append("• ").append(r.invalid().get(i)).append('\n');
            if (r.invalid().size() > shown) {
                sb.append("… and ").append(r.invalid().size() - shown).append(" more");
            }
        }
        return sb.toString();
    }

    private void editCountry(Country existing) {
        if (existing == null) return;
        CountryFormDialog.show(primaryStage, existing, knownContinents(),
                        code -> dao.codeExists(code, existing.id()))
                .ifPresent(c -> run(() -> {
                    dao.update(c);
                    reload(c.id());
                }));
    }

    private void deleteCountry(Country c) {
        if (c == null) return;
        if (Fx.confirm(primaryStage, "Delete " + c.name() + "?",
                "This removes the country from your local database.", "Delete")) {
            run(() -> {
                dao.delete(c.id());
                reload(0);
            });
        }
    }

    private void run(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            Fx.error(primaryStage, e.getMessage());
        }
    }

    // ------------------------------------------------------------------ map dialog

    private void loadWorldMap() {
        if (worldMap != null) return;
        try (InputStream in = AtlasDeskApp.class.getResourceAsStream("/world-map.png")) {
            if (in != null) worldMap = new Image(in, 1640, 0, true, true); // downscaled: saves memory
        } catch (IOException ignored) {
            // map stays null, the dialog shows an empty black pane
        }
    }

    private void showMapDialog() {
        Country c = selected();
        if (c == null) return;
        loadWorldMap();

        double w = 820;
        double h = worldMap != null ? w * worldMap.getHeight() / worldMap.getWidth() : w / 2;

        ImageView mapView = new ImageView(worldMap);
        mapView.setFitWidth(w);
        mapView.setFitHeight(h);
        mapView.setSmooth(true);

        double x = (c.longitude() + 180.0) / 360.0 * w;
        double y = (90.0 - c.latitude()) / 180.0 * h;

        Circle glow = new Circle(x, y, 12, Color.web("#f89820", 0.3));
        glow.setMouseTransparent(true);
        Circle dot = new Circle(x, y, 6, Color.web("#f89820"));
        dot.setStroke(Color.web("#1a1206"));
        dot.setStrokeWidth(1.5);

        Pane mapPane = new Pane(mapView, glow, dot);
        mapPane.setPrefSize(w, h);
        mapPane.setMinSize(w, h);
        mapPane.setMaxSize(w, h);
        mapPane.getStyleClass().add("map-pane");

        ScaleTransition pulse = new ScaleTransition(Duration.seconds(1), glow);
        pulse.setFromX(1);
        pulse.setFromY(1);
        pulse.setToX(2);
        pulse.setToY(2);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();

        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setOnHidden(e -> pulse.stop());

        Label title = new Label("Location: " + c.name());
        title.getStyleClass().add("modal-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> dialog.close());
        HBox header = new HBox(title, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        Fx.makeDraggable(header, dialog);

        VBox box = new VBox(12, header, mapPane);
        box.setPadding(new Insets(14, 16, 16, 16));
        box.getStyleClass().add("modal-bg");

        Scene scene = new Scene(box);
        scene.getStylesheets().add(Fx.css());
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) dialog.close(); });
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}