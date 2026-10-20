package com.vbforge.temptrack;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class TempTrackApp extends Application {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_SHORT = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
    private static final DateTimeFormatter DAY_LONG = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH);

    private Stage primaryStage;
    private String cssUrl;
    private double xOffset = 0;
    private double yOffset = 0;

    private final CityDao dao = new CityDao();
    private final WeatherEngine engine = new WeatherEngine();

    // UI Components
    private final ObservableList<City> cities = FXCollections.observableArrayList();
    private ListView<City> cityList;
    private TextField searchField;
    private Button addBtn;

    // Header
    private Label cityTitleLabel;
    private Tooltip cityTitleTooltip;
    private Label subtitleLabel;
    private Label currentTempLabel;
    private Label dateLabel;
    private Label timeLabel;

    // Forecast
    private HBox forecastGrid;
    private LineChart<String, Number> hourlyChart;
    private Label chartTitleLabel;

    // Details
    private Label windLabel, humidityLabel, precipLabel, uvLabel;

    // Data State
    private WeatherData currentWeather;
    private ZoneOffset cityOffset;      // local time zone of the selected city
    private int requestSeq = 0;         // guards against out-of-order weather responses
    private boolean searchBusy = false;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.initStyle(StageStyle.UNDECORATED);

        cssUrl = getClass().getResource("/temptrack.css").toExternalForm();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildTitleBar());
        root.setLeft(buildSidebar());
        root.setCenter(buildCenterContent());
        root.setRight(buildRightSidebar());

        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(cssUrl);

        stage.setTitle("TempTrack");
        stage.setScene(scene);
        stage.show();

        // Live clock showing the *selected city's* local time
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tickClock()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();

        loadCities();
    }

    @Override
    public void stop() {
        dao.close();
    }

    // ------------------------------------------------------------------ UI building

    private Node buildTitleBar() {
        Label title = new Label("TempTrack");
        title.getStyleClass().add("app-title");

        HBox titleArea = new HBox(title);
        titleArea.setAlignment(Pos.CENTER_LEFT);
        titleArea.setPadding(new Insets(0, 0, 0, 16));
        titleArea.setOnMousePressed(e -> { xOffset = e.getSceneX(); yOffset = e.getSceneY(); });
        titleArea.setOnMouseDragged(e -> { primaryStage.setX(e.getScreenX() - xOffset); primaryStage.setY(e.getScreenY() - yOffset); });
        HBox.setHgrow(titleArea, Priority.ALWAYS);

        Button closeBtn = new Button("×");
        closeBtn.getStyleClass().add("win-btn-close");
        closeBtn.setOnAction(e -> Platform.exit()); // stop() closes the DB

        HBox bar = new HBox(titleArea, closeBtn);
        bar.getStyleClass().add("titlebar");
        return bar;
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar-left");

        // --- search ---
        searchField = new TextField();
        searchField.setPromptText("Search city...");
        searchField.getStyleClass().add("search-input");
        searchField.setMinWidth(0);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setOnAction(e -> onSearch());

        addBtn = new Button("+");
        addBtn.getStyleClass().add("btn-add");
        addBtn.setMaxHeight(Double.MAX_VALUE);
        addBtn.setTooltip(new Tooltip("Add city"));
        addBtn.setOnAction(e -> onSearch());

        HBox searchBox = new HBox(8, searchField, addBtn);
        searchBox.getStyleClass().add("search-box");

        // --- list header ---
        Label listTitle = new Label("FAVORITES");
        listTitle.getStyleClass().add("list-header-title");
        Label countBadge = new Label();
        countBadge.getStyleClass().add("count-badge");
        countBadge.textProperty().bind(Bindings.size(cities).asString());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox listHeader = new HBox(listTitle, spacer, countBadge);
        listHeader.setAlignment(Pos.CENTER_LEFT);
        listHeader.getStyleClass().add("list-header");

        // --- list ---
        cityList = new ListView<>(cities);
        cityList.getStyleClass().add("favorites-list");
        cityList.setCellFactory(param -> new CityCell());
        cityList.setFocusTraversable(false);

        Label empty = new Label("No cities yet\nType a name above and press +");
        empty.getStyleClass().add("empty-hint");
        empty.setTextAlignment(TextAlignment.CENTER);
        empty.setWrapText(true);
        cityList.setPlaceholder(empty);

        cityList.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) fetchWeather(sel);
        });
        VBox.setVgrow(cityList, Priority.ALWAYS);

        sidebar.getChildren().addAll(searchBox, listHeader, cityList);
        return sidebar;
    }

    /** One favorite city: name + country, with a remove button that shows on hover/selection. */
    private class CityCell extends ListCell<City> {
        private final Label name = new Label();
        private final Label country = new Label();
        private final Tooltip nameTip = new Tooltip();
        private final HBox card;

        CityCell() {
            setPrefWidth(0); // never let long names force a horizontal scrollbar
            setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

            name.getStyleClass().add("city-name");
            name.setMinWidth(0);
            name.setTooltip(nameTip);
            country.getStyleClass().add("city-country");
            country.setMinWidth(0);

            VBox text = new VBox(2, name, country);
            text.setMinWidth(0);
            HBox.setHgrow(text, Priority.ALWAYS);

            Button remove = new Button("×");
            remove.getStyleClass().add("city-remove");
            remove.setFocusTraversable(false);
            remove.setTooltip(new Tooltip("Remove"));
            // don't let the click also select the row (it would trigger a pointless fetch)
            //remove.addEventHandler(MouseEvent.MOUSE_PRESSED, MouseEvent::consume);
            remove.setOnAction(e -> {
                City c = getItem();
                if (c != null) onRemoveCity(c);
            });

            card = new HBox(6, text, remove);
            card.setAlignment(Pos.CENTER_LEFT);
            card.getStyleClass().add("city-card");
            card.prefWidthProperty().bind(widthProperty());
            card.setMaxWidth(Region.USE_PREF_SIZE);
        }

        @Override
        protected void updateItem(City item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
            } else {
                name.setText(item.name());
                nameTip.setText(item.toString());
                String c = item.country();
                country.setText(c == null || c.isEmpty() ? "—" : c);
                setGraphic(card);
            }
        }
    }

    private Node buildCenterContent() {
        VBox centerContent = new VBox(24);
        centerContent.getStyleClass().add("center-content");
        centerContent.setMinWidth(0);

        // --- Header ---
        // City title: one line, ellipsis + tooltip, font shrinks a bit for long names.
        // This is what keeps a long name from pushing the rest of the window around.
        cityTitleLabel = new Label("Loading...");
        cityTitleLabel.getStyleClass().add("city-title");
        cityTitleLabel.setWrapText(false);
        cityTitleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        cityTitleLabel.setMinWidth(0);
        cityTitleLabel.setMaxWidth(Double.MAX_VALUE);
        cityTitleTooltip = new Tooltip();
        cityTitleLabel.setTooltip(cityTitleTooltip);

        subtitleLabel = new Label();
        subtitleLabel.getStyleClass().add("current-condition");
        subtitleLabel.setTextOverrun(OverrunStyle.ELLIPSIS);
        subtitleLabel.setMinWidth(0);
        subtitleLabel.setMaxWidth(Double.MAX_VALUE);
        subtitleLabel.setGraphicTextGap(8);

        VBox cityInfo = new VBox(4, cityTitleLabel, subtitleLabel);
        cityInfo.setMinWidth(0);
        cityInfo.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cityInfo, Priority.ALWAYS);

        dateLabel = new Label();
        dateLabel.getStyleClass().add("dt-date");
        timeLabel = new Label("--:--");
        timeLabel.getStyleClass().add("dt-time");
        VBox dateTimeBox = new VBox(2, dateLabel, timeLabel);
        dateTimeBox.getStyleClass().add("date-time-box");
        dateTimeBox.setAlignment(Pos.CENTER);
        fixWidth(dateTimeBox, 190);

        currentTempLabel = new Label("--°C");
        currentTempLabel.getStyleClass().add("current-temp");
        currentTempLabel.setAlignment(Pos.CENTER_RIGHT);
        fixWidth(currentTempLabel, 190);

        HBox headerSection = new HBox(20, cityInfo, dateTimeBox, currentTempLabel);
        headerSection.setAlignment(Pos.CENTER_LEFT);
        headerSection.getStyleClass().add("header-section");
        headerSection.setMinHeight(96);
        headerSection.setPrefHeight(96);
        headerSection.setMaxHeight(96);

        // --- 7 Day Forecast ---
        Label forecastTitle = new Label("7-Day Forecast");
        forecastTitle.getStyleClass().add("section-title");
        forecastGrid = new HBox(10);
        forecastGrid.setAlignment(Pos.CENTER_LEFT);
        forecastGrid.setMinHeight(112);

        // --- Chart ---
        chartTitleLabel = new Label("24-Hour Temperature");
        chartTitleLabel.getStyleClass().add("section-title");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setForceZeroInRange(false); // otherwise 15-20° looks like a flat line near the top
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, null, "°"));

        hourlyChart = new LineChart<>(xAxis, yAxis);
        hourlyChart.setAnimated(false);
        hourlyChart.setCreateSymbols(true);
        hourlyChart.setLegendVisible(false);
        hourlyChart.getStyleClass().add("hourly-chart");
        VBox.setVgrow(hourlyChart, Priority.ALWAYS);

        VBox chartContainer = new VBox(10, chartTitleLabel, hourlyChart);
        chartContainer.getStyleClass().add("chart-container");
        VBox.setVgrow(chartContainer, Priority.ALWAYS);

        centerContent.getChildren().addAll(headerSection, forecastTitle, forecastGrid, chartContainer);
        return centerContent;
    }

    private Node buildRightSidebar() {
        VBox rightContent = new VBox(24);
        rightContent.getStyleClass().add("sidebar-right");

        Label detailsTitle = new Label("Current Details");
        detailsTitle.getStyleClass().add("section-title");

        windLabel = new Label("--");
        humidityLabel = new Label("--");
        precipLabel = new Label("--");
        uvLabel = new Label("--");

        // BUG FIX: the old createMetric() returned only the value label, so the metric
        // boxes (icon + caption) were never added to the sidebar.
        VBox metricList = new VBox(12,
                createMetric(WeatherIcons.wind(20), "Wind", windLabel),
                createMetric(WeatherIcons.humidity(20), "Humidity", humidityLabel),
                createMetric(WeatherIcons.rain(20), "Precip.", precipLabel),
                createMetric(WeatherIcons.sun(20), "UV Index", uvLabel));

        Label legendTitle = new Label("Legend");
        legendTitle.getStyleClass().add("legend-title");

        VBox legendBox = new VBox(10,
                legendTitle,
                createLegendDot("High Temperature", "high"),
                createLegendDot("Low Temperature", "low"),
                createLegendIcon("Precipitation (Rain)", WeatherIcons.rain(18)),
                createLegendIcon("Clear Sky", WeatherIcons.sun(18)));
        legendBox.getStyleClass().add("legend-box");

        rightContent.getChildren().addAll(detailsTitle, metricList, legendBox);
        return rightContent;
    }

    private HBox createMetric(Node icon, String caption, Label valueLabel) {
        Label l = new Label(caption);
        l.getStyleClass().add("metric-label");
        valueLabel.getStyleClass().add("metric-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox box = new HBox(10, icon, l, spacer, valueLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("metric-item");
        return box;
    }

    private HBox createLegendDot(String text, String dotClass) {
        Region dot = new Region();
        dot.getStyleClass().addAll("legend-dot", dotClass);
        return legendRow(dot, text);
    }

    private HBox createLegendIcon(String text, Node icon) {
        return legendRow(icon, text);
    }

    private HBox legendRow(Node leading, String text) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add("legend-label");
        // leading node sits in a fixed 20px slot so all legend texts line up
        StackPane slot = new StackPane(leading);
        slot.setMinWidth(20);
        slot.setPrefWidth(20);
        slot.setMaxWidth(20);
        HBox item = new HBox(10, slot, lbl);
        item.setAlignment(Pos.CENTER_LEFT);
        return item;
    }

    private static void fixWidth(Region r, double w) {
        r.setMinWidth(w);
        r.setPrefWidth(w);
        r.setMaxWidth(w);
    }

    // ------------------------------------------------------------------ data / actions

    private void loadCities() {
        List<City> saved = dao.findAll();
        if (saved.isEmpty()) {
            dao.save(new City(0, "London", "UK", 51.5074, -0.1278));
            dao.save(new City(0, "Tokyo", "Japan", 35.6762, 139.6503));
            saved = dao.findAll();
        }
        cities.setAll(saved);
        if (!cities.isEmpty()) cityList.getSelectionModel().selectFirst();
    }

    private void onSearch() {
        String query = searchField.getText().trim();
        if (query.isEmpty() || searchBusy) return;

        setSearchBusy(true);
        Task<City> task = new Task<>() {
            @Override protected City call() throws Exception {
                return engine.searchCity(query);
            }
        };
        task.setOnSucceeded(e -> {
            setSearchBusy(false);
            City result = task.getValue();
            if (result == null) {
                showAlert("City not found: " + query);
                return;
            }
            searchField.clear();

            // Don't add the same city twice - just jump to it
            City existing = findExisting(result);
            if (existing != null) {
                cityList.getSelectionModel().select(existing);
                cityList.scrollTo(existing);
                return;
            }

            dao.save(result);
            cities.setAll(dao.findAll());
            cityList.getSelectionModel().selectLast();
            cityList.scrollTo(cities.size() - 1);
        });
        task.setOnFailed(e -> {
            setSearchBusy(false);
            showAlert("Error searching: " + messageOf(task.getException()));
        });
        runAsync(task);
    }

    private void setSearchBusy(boolean busy) {
        searchBusy = busy;
        searchField.setDisable(busy);
        addBtn.setDisable(busy);
    }

    private City findExisting(City c) {
        for (City x : cities) {
            if (x.name().equalsIgnoreCase(c.name())
                    && Math.abs(x.lat() - c.lat()) < 0.1
                    && Math.abs(x.lon() - c.lon()) < 0.1) {
                return x;
            }
        }
        return null;
    }

    private void onRemoveCity(City city) {
        boolean wasSelected = city.equals(cityList.getSelectionModel().getSelectedItem());
        int idx = cities.indexOf(city);

        dao.delete(city.id());
        cities.remove(city);

        if (cities.isEmpty()) {
            clearDisplay();
        } else if (wasSelected) {
            cityList.getSelectionModel().select(Math.max(0, Math.min(idx, cities.size() - 1)));
        }
    }

    private void fetchWeather(City city) {
        final int seq = ++requestSeq;

        showCityName(city);
        resetWeatherDisplay();
        subtitleLabel.setText("Loading…");

        Task<WeatherData> task = new Task<>() {
            @Override protected WeatherData call() throws Exception {
                return engine.fetchWeather(city.lat(), city.lon());
            }
        };

        // BUG FIX: ignore responses that arrive after the user already picked another city
        task.setOnSucceeded(e -> {
            if (seq != requestSeq) return;
            updateUI(city, task.getValue());
        });
        task.setOnFailed(e -> {
            if (seq != requestSeq) return;
            subtitleLabel.setText("Could not load weather");
            showAlert("Error loading weather: " + messageOf(task.getException()));
        });
        runAsync(task);
    }

    private void showCityName(City city) {
        String name = city.name();
        cityTitleLabel.setText(name);
        cityTitleTooltip.setText(city.toString());
        int len = name.length();
        int px = len <= 13 ? 32 : len <= 18 ? 26 : 22;
        cityTitleLabel.setStyle("-fx-font-size: " + px + "px;");
    }

    private void resetWeatherDisplay() {
        currentWeather = null;
        cityOffset = null;
        currentTempLabel.setText("--°C");
        subtitleLabel.setGraphic(null);
        dateLabel.setText("");
        timeLabel.setText("--:--");
        windLabel.setText("--");
        humidityLabel.setText("--");
        precipLabel.setText("--");
        uvLabel.setText("--");
        forecastGrid.getChildren().clear();
        hourlyChart.getData().clear();
    }

    private void clearDisplay() {
        requestSeq++; // drop any in-flight response
        cityTitleLabel.setText("No city selected");
        cityTitleLabel.setStyle("-fx-font-size: 32px;");
        cityTitleTooltip.setText("");
        resetWeatherDisplay();
        subtitleLabel.setText("Add a city to see its forecast");
        chartTitleLabel.setText("24-Hour Temperature");
    }

    private void updateUI(City city, WeatherData data) {
        currentWeather = data;
        cityOffset = ZoneOffset.ofTotalSeconds(data.utcOffsetSeconds);
        tickClock();

        currentTempLabel.setText(Math.round(data.currentTemp) + "°C");

        String country = city.country();
        String prefix = (country == null || country.isEmpty()) ? "" : country + "  ·  ";
        subtitleLabel.setText(prefix + WeatherIcons.describe(data.currentWeatherCode));
        subtitleLabel.setGraphic(WeatherIcons.forCode(data.currentWeatherCode, 18));

        windLabel.setText(Math.round(data.windSpeed) + " km/h");
        humidityLabel.setText(data.humidity + "%");
        precipLabel.setText(String.format(Locale.ROOT, "%.1f mm", data.precipitation));
        uvLabel.setText(data.uvIndexMax >= 0 ? String.format(Locale.ROOT, "%.1f", data.uvIndexMax) : "N/A");

        forecastGrid.getChildren().clear();
        for (int i = 0; i < Math.min(7, data.daily.size()); i++) {
            VBox card = createForecastCard(i, data.daily.get(i));
            forecastGrid.getChildren().add(card);
            HBox.setHgrow(card, Priority.ALWAYS);
        }

        updateChartForDay(0);
    }

    /** Shows date/time in the selected city's own time zone. */
    private void tickClock() {
        if (cityOffset == null) return;
        ZonedDateTime t = ZonedDateTime.now(cityOffset);
        dateLabel.setText(t.format(DATE_FMT).toUpperCase(Locale.ENGLISH));
        timeLabel.setText(t.format(TIME_FMT));
    }

    private VBox createForecastCard(int index, WeatherData.DailyForecast day) {
        String dayName = index == 0
                ? "TODAY"
                : LocalDate.parse(day.date()).format(DAY_SHORT).toUpperCase(Locale.ENGLISH);

        Label dayLbl = new Label(dayName);
        dayLbl.getStyleClass().add("fc-day");

        Node icon = WeatherIcons.forCode(day.weatherCode(), 34);

        Label highLbl = new Label(Math.round(day.maxTemp()) + "°");
        highLbl.getStyleClass().add("fc-high");

        Label lowLbl = new Label(Math.round(day.minTemp()) + "°");
        lowLbl.getStyleClass().add("fc-low");

        HBox temps = new HBox(8, highLbl, lowLbl);
        temps.setAlignment(Pos.CENTER);

        VBox card = new VBox(10, dayLbl, icon, temps);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("forecast-card");
        card.setPrefWidth(90);
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);

        if (index == 0) card.getStyleClass().add("selected");

        card.setOnMouseClicked(e -> {
            for (Node n : forecastGrid.getChildren()) {
                n.getStyleClass().remove("selected");
            }
            card.getStyleClass().add("selected");
            updateChartForDay(index);
        });

        return card;
    }

    private void updateChartForDay(int dayIndex) {
        if (currentWeather == null || dayIndex >= currentWeather.daily.size()) return;

        hourlyChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        int startIndex = dayIndex * 24;

        for (int hourOfDay = 0; hourOfDay < 24; hourOfDay += 3) {
            int actualIndex = startIndex + hourOfDay;
            if (actualIndex < currentWeather.hourly.size()) {
                WeatherData.HourlyForecast hour = currentWeather.hourly.get(actualIndex);
                String label = String.format("%02d:00", hourOfDay);
                series.getData().add(new XYChart.Data<>(label, Math.round(hour.temp())));
            }
        }

        hourlyChart.getData().add(series);

        for (XYChart.Data<String, Number> d : series.getData()) {
            if (d.getNode() != null) {
                Tooltip.install(d.getNode(), new Tooltip(d.getXValue() + "  ·  " + d.getYValue() + "°C"));
            }
        }

        LocalDate date = LocalDate.parse(currentWeather.daily.get(dayIndex).date());
        chartTitleLabel.setText(date.format(DAY_LONG) + " - 24-Hour Forecast");
    }

    // ------------------------------------------------------------------ helpers

    private void runAsync(Task<?> task) {
        Thread t = new Thread(task, "temptrack-io");
        t.setDaemon(true); // don't keep the JVM alive after the window is closed
        t.start();
    }

    private static String messageOf(Throwable ex) {
        if (ex == null) return "unknown error";
        String m = ex.getMessage();
        return (m == null || m.isBlank()) ? ex.getClass().getSimpleName() : m;
    }

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.initOwner(primaryStage);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.getDialogPane().getStylesheets().add(cssUrl);
        a.getDialogPane().getStyleClass().add("temp-dialog");
        a.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
