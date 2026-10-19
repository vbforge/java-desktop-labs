package com.vbforge.atlasdesk;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

public final class CountryFormDialog {

    private static final List<String> DEFAULT_CONTINENTS =
            List.of("Africa", "Antarctica", "Asia", "Europe", "North America", "Oceania", "South America");

    private final Country existing;
    private final Predicate<String> codeTaken;
    private final Stage stage = new Stage();
    private Country result;

    private final TextField name = new TextField();
    private final TextField code = new TextField();
    private final ComboBox<String> continent = new ComboBox<>();
    private final TextField capital = new TextField();
    private final TextField countryPop = new TextField();
    private final TextField capitalPop = new TextField();
    private final TextField area = new TextField();
    private final TextField lat = new TextField();
    private final TextField lon = new TextField();
    private final Label error = new Label();

    /**
     * @param existing   country to edit, or null to add a new one
     * @param codeTaken  returns true if the ISO code is already used by ANOTHER country
     */
    public static Optional<Country> show(Stage owner, Country existing,
                                         Collection<String> knownContinents, Predicate<String> codeTaken) {
        CountryFormDialog dialog = new CountryFormDialog(owner, existing, knownContinents, codeTaken);
        dialog.stage.showAndWait();
        return Optional.ofNullable(dialog.result);
    }

    private CountryFormDialog(Stage owner, Country existing, Collection<String> known, Predicate<String> codeTaken) {
        this.existing = existing;
        this.codeTaken = codeTaken;

        Set<String> options = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        options.addAll(DEFAULT_CONTINENTS);
        options.addAll(known);
        continent.setItems(FXCollections.observableArrayList(options));
        continent.setEditable(true);

        code.setTextFormatter(new TextFormatter<String>(ch -> {
            ch.setText(ch.getText().toUpperCase(Locale.ROOT));
            return ch.getControlNewText().length() <= 2 ? ch : null;
        }));

        name.setPromptText("Poland");
        code.setPromptText("PL");
        continent.setPromptText("Europe");
        capital.setPromptText("Warsaw");
        countryPop.setPromptText("37 750 000");
        capitalPop.setPromptText("1 860 000");
        area.setPromptText("312 696");
        lat.setPromptText("51.9194");
        lon.setPromptText("19.1451");

        if (existing != null) {
            name.setText(existing.name());
            code.setText(existing.code());
            continent.setValue(existing.continent());
            capital.setText(existing.capital());
            countryPop.setText(String.valueOf(existing.countryPopulation()));
            capitalPop.setText(String.valueOf(existing.capitalPopulation()));
            area.setText(String.valueOf(existing.area()));
            lat.setText(String.valueOf(existing.latitude()));
            lon.setText(String.valueOf(existing.longitude()));
        }

        // Header
        Label title = new Label(existing == null ? "Add country" : "Edit " + existing.name());
        title.getStyleClass().add("modal-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> stage.close());
        HBox header = new HBox(title, spacer, closeBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Form grid
        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(14);
        grid.getColumnConstraints().addAll(column(), column());
        grid.add(field("Country name", name), 0, 0);
        grid.add(field("ISO code (2 letters)", code), 1, 0);
        grid.add(field("Continent (pick or type a new one)", continent), 0, 1);
        grid.add(field("Capital", capital), 1, 1);
        grid.add(field("Country population", countryPop), 0, 2);
        grid.add(field("Capital population", capitalPop), 1, 2);
        grid.add(field("Area (km²)", area), 0, 3);
        grid.add(field("Latitude (-90 to 90)", lat), 0, 4);
        grid.add(field("Longitude (-180 to 180)", lon), 1, 4);

        error.getStyleClass().add("form-error");
        error.setWrapText(true);
        error.managedProperty().bind(error.textProperty().isNotEmpty());
        error.visibleProperty().bind(error.textProperty().isNotEmpty());

        // Footer
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("btn-secondary");
        cancel.setOnAction(e -> stage.close());
        Button save = new Button(existing == null ? "Add country" : "Save changes");
        save.getStyleClass().add("btn-primary");
        save.setDefaultButton(true);
        save.setOnAction(e -> save());
        HBox footer = new HBox(10, cancel, save);
        footer.setAlignment(Pos.CENTER_RIGHT);

        VBox box = new VBox(16, header, grid, error, footer);
        box.setPadding(new Insets(18, 20, 20, 20));
        box.setPrefWidth(580);
        box.getStyleClass().add("modal-bg");
        Fx.makeDraggable(header, stage);

        Scene scene = new Scene(box);
        scene.getStylesheets().add(Fx.css());
        scene.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ESCAPE) stage.close(); });

        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(scene);
    }

    private void save() {
        try {
            String isoCode = code.getText() == null ? "" : code.getText().trim().toUpperCase(Locale.ROOT);
            if (!isoCode.matches("[A-Z]{2}")) {
                throw new IllegalArgumentException("ISO code must be exactly 2 letters, e.g. PL");
            }
            if (codeTaken.test(isoCode)) {
                throw new IllegalArgumentException("A country with code " + isoCode + " already exists");
            }

            String countryName = required(name.getText(), "Country name");
            String typedContinent = required(continent.getEditor().getText(), "Continent");
            // reuse the existing spelling so "europe" does not create a second "Europe"
            String continentName = continent.getItems().stream()
                    .filter(i -> i.equalsIgnoreCase(typedContinent))
                    .findFirst().orElse(typedContinent);
            String capitalName = required(capital.getText(), "Capital");

            long pop = parseLong(countryPop, "Country population", 0);
            long capPop = parseLong(capitalPop, "Capital population", 0);
            long areaKm = parseLong(area, "Area", 1);
            double latitude = parseDouble(lat, "Latitude", -90, 90);
            double longitude = parseDouble(lon, "Longitude", -180, 180);

            result = new Country(existing == null ? 0 : existing.id(), isoCode, countryName, continentName,
                    capitalName, pop, capPop, areaKm, latitude, longitude);
            stage.close();
        } catch (RuntimeException ex) {
            error.setText(ex.getMessage());
        }
    }

    // ---------- helpers ----------

    private static ColumnConstraints column() {
        ColumnConstraints cc = new ColumnConstraints();
        cc.setPercentWidth(50);
        cc.setHgrow(Priority.ALWAYS);
        return cc;
    }

    private static VBox field(String label, Region input) {
        Label l = new Label(label);
        l.getStyleClass().add("form-label");
        input.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, l, input);
    }

    private static String required(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException(label + " is required");
        return v;
    }

    private static long parseLong(TextField f, String label, long min) {
        String raw = f.getText() == null ? "" : f.getText().replaceAll("[\\s,_]", "");
        try {
            long v = Long.parseLong(raw);
            if (v < min) throw new IllegalArgumentException(label + " must be at least " + min);
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a whole number");
        }
    }

    private static double parseDouble(TextField f, String label, double min, double max) {
        String raw = f.getText() == null ? "" : f.getText().replaceAll("\\s", "").replace(',', '.');
        try {
            double v = Double.parseDouble(raw);
            if (!(v >= min && v <= max)) {
                throw new IllegalArgumentException(label + " must be between " + min + " and " + max);
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " must be a number, e.g. 51.9194");
        }
    }
}