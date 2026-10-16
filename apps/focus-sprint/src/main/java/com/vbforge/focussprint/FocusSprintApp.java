package com.vbforge.focussprint;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Node;

import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class FocusSprintApp extends Application {

    private static final int SPRINT_GOAL_SECONDS = 25 * 60;
    private static final String[] TONES = {"orange", "blue", "green", "gray"};

    // ---- persistence ----
    private Db db;
    private SectionDao sectionDao;
    private SessionDao sessionDao;
    private AppConfig config;
    private Instant dayStart;

    // ---- in-memory state ----
    private final ObservableList<Section> sections = FXCollections.observableArrayList();
    private final ObservableList<Session> todaySessions = FXCollections.observableArrayList();

    // ---- active session ----
    private Section activeSection;
    private Instant activeStartedAt;
    private long activeAccumulated;
    private Instant resumedAt;
    private boolean paused;

    // ---- ticker ----
    private AnimationTimer ticker;
    private long lastShownSecond = -1;

    // ---- UI refs ----
    private VBox sectionsBox;
    private VBox barsBox;
    private VBox statsBox;
    private VBox logBox;
    private Label dateLabel;
    private Label headerTotalLabel;
    private Label logCountLabel;
    private Label timerHeadLabel;
    private Label timerClockLabel;
    private Label timerMetaLabel;
    private ProgressBar sprintBar;
    private Button pauseBtn;
    private Button stopBtn;
    private Button discardBtn;

    @Override
    public void start(Stage stage) {
        db = new Db();
        sectionDao = new SectionDao(db);
        sessionDao = new SessionDao(db);
        config = AppConfig.open();

        ensureDayStart();
        reloadSections();
        reloadToday();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(buildHeader());
        root.setLeft(buildSidebar());
        root.setCenter(buildMain());

        Scene scene = new Scene(root, 1180, 760);
        scene.getStylesheets().add(
                FocusSprintApp.class.getResource("/focussprint.css").toExternalForm());

        stage.setTitle("FocusSprint");
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setOnCloseRequest(e -> shutdown());
        stage.show();

        ticker = new AnimationTimer() {
            @Override public void handle(long now) {
                long elapsed = activeElapsedSeconds();
                if (elapsed != lastShownSecond) {
                    lastShownSecond = elapsed;
                    refreshLive();
                }
            }
        };
        ticker.start();

        renderAll();
    }

    // ================================================================
    // Layout
    // ================================================================

    private Node buildHeader() {
        Label title = new Label("FocusSprint");
        title.getStyleClass().add("app-title");

        dateLabel = new Label();
        dateLabel.getStyleClass().add("app-date");

        headerTotalLabel = new Label();
        headerTotalLabel.getStyleClass().add("total-pill");

        Button newDay = new Button("New day");
        newDay.getStyleClass().add("subtle-btn");
        newDay.setOnAction(e -> onNewDay());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(14, title, dateLabel, spacer, headerTotalLabel, newDay);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("app-header");
        return bar;
    }

    private Node buildSidebar() {
        sectionsBox = new VBox(6);

        Button addBtn = new Button("+ New section");
        addBtn.getStyleClass().add("add-section");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.setOnAction(e -> onAddSection());

        VBox side = new VBox(10, sectionHeader(), sectionsBox, addBtn);
        side.getStyleClass().add("sidebar");
        side.setPrefWidth(240);
        side.setMinWidth(220);
        return side;
    }

    private Node sectionHeader() {
        Label h = new Label("SECTIONS");
        h.getStyleClass().add("sidebar-label");
        return h;
    }

    private Node buildMain() {
        VBox main = new VBox(16, buildTimerPanel(), buildRetro(), buildLog());
        main.getStyleClass().add("main-area");

        ScrollPane sp = new ScrollPane(main);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("main-scroll");
        return sp;
    }

    private Node buildTimerPanel() {
        timerHeadLabel = new Label();
        timerHeadLabel.getStyleClass().add("timer-head");

        timerClockLabel = new Label("00:00:00");
        timerClockLabel.getStyleClass().add("timer-clock");

        timerMetaLabel = new Label();
        timerMetaLabel.getStyleClass().add("timer-meta");

        sprintBar = new ProgressBar(0);
        sprintBar.setMaxWidth(Double.MAX_VALUE);
        sprintBar.getStyleClass().add("sprint-bar");

        pauseBtn   = new Button("Pause");
        stopBtn    = new Button("Stop & save");
        discardBtn = new Button("Discard");
        pauseBtn.getStyleClass().add("primary-btn");
        discardBtn.getStyleClass().add("danger-btn");

        pauseBtn.setOnAction(e -> onPauseResume());
        stopBtn.setOnAction(e -> { logActiveSession(); renderAll(); });
        discardBtn.setOnAction(e -> onDiscard());

        HBox actions = new HBox(10, pauseBtn, stopBtn, discardBtn);
        actions.setAlignment(Pos.CENTER);

        VBox panel = new VBox(8, timerHeadLabel, timerClockLabel,
                timerMetaLabel, sprintBar, actions);
        panel.getStyleClass().add("timer-panel");
        return panel;
    }

    private Node buildRetro() {
        barsBox  = new VBox(12);
        statsBox = new VBox(0);

        Label head = new Label("LIVE RETROSPECTIVE");
        head.getStyleClass().add("panel-label");
        Label hint = new Label("recalculates every second");
        hint.getStyleClass().add("panel-hint");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(10, head, sp, hint);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("panel-head");

        HBox body = new HBox(26, barsBox, statsBox);
        HBox.setHgrow(barsBox, Priority.ALWAYS);
        barsBox.setMaxWidth(Double.MAX_VALUE);
        statsBox.setMinWidth(240);
        body.getStyleClass().add("retro-body");

        VBox panel = new VBox(header, body);
        panel.getStyleClass().add("panel");
        return panel;
    }

    private Node buildLog() {
        logCountLabel = new Label();
        logCountLabel.getStyleClass().add("panel-hint");

        Label head = new Label("TODAY'S SESSIONS");
        head.getStyleClass().add("panel-label");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox header = new HBox(10, head, sp, logCountLabel);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("panel-head");

        logBox = new VBox(0);
        logBox.getStyleClass().add("log-body");

        VBox panel = new VBox(header, logBox);
        panel.getStyleClass().add("panel");
        return panel;
    }

    // ================================================================
    // State machine
    // ================================================================

    private long activeElapsedSeconds() {
        if (activeSection == null) return 0;
        if (resumedAt == null) return activeAccumulated;
        return activeAccumulated + TimeFormatter.secondsBetween(resumedAt, Instant.now());
    }

    private void onSectionClicked(Section s) {
        if (activeSection != null && activeSection.id() == s.id()) {
            onPauseResume();
            return;
        }
        logActiveSession();
        startSection(s);
    }

    private void startSection(Section s) {
        activeSection = s;
        activeStartedAt = Instant.now();
        activeAccumulated = 0;
        resumedAt = activeStartedAt;
        paused = false;
        lastShownSecond = -1;
        renderAll();
    }

    private void onPauseResume() {
        if (activeSection == null) return;
        if (paused) {
            resumedAt = Instant.now();
            paused = false;
        } else {
            activeAccumulated = activeElapsedSeconds();
            resumedAt = null;
            paused = true;
        }
        renderAll();
    }

    private void logActiveSession() {
        if (activeSection == null) return;
        long secs = activeElapsedSeconds();
        if (secs >= 1) {
            try {
                Session s = Session.completed(activeSection.id(), activeStartedAt, Instant.now());
                Session stored = sessionDao.insert(s);
                todaySessions.add(0, stored);
            } catch (SQLException e) {
                showError("Could not save session", e);
            }
        }
        clearActive();
    }

    private void onDiscard() {
        if (activeSection == null) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Discard the current session without saving?",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;
        clearActive();
        renderAll();
    }

    private void clearActive() {
        activeSection = null;
        activeStartedAt = null;
        activeAccumulated = 0;
        resumedAt = null;
        paused = false;
        lastShownSecond = -1;
    }

    private void onNewDay() {
        if (activeSection != null) {
            Alert busy = new Alert(Alert.AlertType.WARNING,
                    "Stop the current session before starting a new day.",
                    ButtonType.OK);
            busy.setHeaderText(null);
            busy.showAndWait();
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Start a new day?\nToday's view will clear. Everything is kept in the database.",
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;

        dayStart = Instant.now();
        config.setDayStart(dayStart);
        reloadToday();
        renderAll();
    }

    private void onAddSection() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setHeaderText("New section name");
        dlg.setContentText("Name:");
        Optional<String> r = dlg.showAndWait();
        if (r.isEmpty() || r.get().isBlank()) return;

        String name = r.get().trim();
        String tone = TONES[sections.size() % TONES.length];
        try {
            Section s = sectionDao.insert(name, tone, sections.size());
            sections.add(s);
            renderAll();
        } catch (SQLException e) {
            showError("Could not add section", e);
        }
    }

    private void onDeleteSection(Section s) {
        long todaySeconds = sessionsForSection(s.id()).stream()
                .mapToLong(Session::seconds).sum();

        String msg = todaySeconds > 0
                ? "Delete \"" + s.name() + "\"?\n\n"
                  + "This will also delete " + TimeFormatter.duration(todaySeconds)
                  + " of today's sessions for this section, and every past session.\n"
                  + "This cannot be undone."
                : "Delete \"" + s.name() + "\"?\n\n"
                  + "This section has no sessions today, but any historical\n"
                  + "sessions will also be deleted. This cannot be undone.";

        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg,
                ButtonType.OK, ButtonType.CANCEL);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        if (r.isEmpty() || r.get() != ButtonType.OK) return;

        if (activeSection != null && activeSection.id() == s.id()) clearActive();

        try {
            sectionDao.delete(s.id());
            sections.removeIf(x -> x.id() == s.id());
            todaySessions.removeIf(x -> x.sectionId() == s.id());
            renderAll();
        } catch (SQLException e) {
            showError("Could not delete section", e);
        }
    }

    // ================================================================
    // Data loading
    // ================================================================

    private void ensureDayStart() {
        Instant stored = config.dayStart();
        LocalDate today = LocalDate.now();
        if (stored == null
                || stored.atZone(ZoneId.systemDefault()).toLocalDate().isBefore(today)) {
            dayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
            config.setDayStart(dayStart);
        } else {
            dayStart = stored;
        }
    }

    private void reloadSections() {
        try {
            sections.setAll(sectionDao.findAll());
        } catch (SQLException e) {
            showError("Could not load sections", e);
        }
    }

    private void reloadToday() {
        try {
            todaySessions.setAll(sessionDao.findSince(dayStart));
        } catch (SQLException e) {
            showError("Could not load today's sessions", e);
        }
    }

    // ================================================================
    // Derived values
    // ================================================================

    private List<Session> sessionsForSection(long sectionId) {
        return todaySessions.stream().filter(s -> s.sectionId() == sectionId).toList();
    }

    private long secondsForSection(long sectionId) {
        long base = sessionsForSection(sectionId).stream().mapToLong(Session::seconds).sum();
        if (activeSection != null && activeSection.id() == sectionId) {
            base += activeElapsedSeconds();
        }
        return base;
    }

    private long totalTodaySeconds() {
        return sections.stream().mapToLong(s -> secondsForSection(s.id())).sum();
    }

    private long longestSessionSeconds() {
        long longest = 0;
        for (Session s : todaySessions) longest = Math.max(longest, s.seconds());
        if (activeSection != null) longest = Math.max(longest, activeElapsedSeconds());
        return longest;
    }

    private Section longestSessionSection() {
        long longest = 0;
        long sectionId = -1;
        for (Session s : todaySessions) {
            if (s.seconds() > longest) { longest = s.seconds(); sectionId = s.sectionId(); }
        }
        if (activeSection != null && activeElapsedSeconds() > longest) {
            sectionId = activeSection.id();
        }
        if (sectionId < 0) return null;
        final long sid = sectionId;
        return sections.stream().filter(s -> s.id() == sid).findFirst().orElse(null);
    }

    // ================================================================
    // Rendering
    // ================================================================

    private void renderAll() {
        renderHeader();
        renderSections();
        renderTimer();
        renderBars();
        renderStats();
        renderLog();
    }

    private void refreshLive() {
        renderHeader();
        renderSections();
        renderTimer();
        renderBars();
        renderStats();
        // Log doesn't change while a session runs.
    }

    private void renderHeader() {
        dateLabel.setText(TimeFormatter.humanDate(LocalDate.now()));
        headerTotalLabel.setText("Today: " + TimeFormatter.duration(totalTodaySeconds()));
    }

    private void renderSections() {
        sectionsBox.getChildren().clear();
        for (Section s : sections) {
            boolean active = activeSection != null && activeSection.id() == s.id();

            Label dot = new Label();
            dot.getStyleClass().addAll("section-dot", "tone-" + s.tone());
            if (active && !paused) dot.getStyleClass().add("pulsing");

            Label name = new Label(s.name());
            name.getStyleClass().add("section-name");
            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            Label time = new Label(TimeFormatter.duration(secondsForSection(s.id())));
            time.getStyleClass().add("section-time");

            Button del = new Button("×");
            del.getStyleClass().add("section-delete");
            del.setOnAction(e -> onDeleteSection(s));

            HBox row = new HBox(10, dot, name, sp, time, del);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("section-item");
            if (active) row.getStyleClass().add(paused ? "paused" : "active");
            row.setOnMouseClicked(e -> {
                if (e.getTarget() == del) return;
                onSectionClicked(s);
            });

            sectionsBox.getChildren().add(row);
        }
    }

    private void renderTimer() {
        if (activeSection == null) {
            timerHeadLabel.setText("Idle — select a section to start");
            timerClockLabel.setText("00:00:00");
            timerClockLabel.getStyleClass().remove("running");
            timerMetaLabel.setText("Sprint goal: 25:00   ·   Sprints done: 0");
            sprintBar.setProgress(0);
            pauseBtn.setDisable(true);
            stopBtn.setDisable(true);
            discardBtn.setDisable(true);
            pauseBtn.setText("Pause");
            return;
        }

        long elapsed = activeElapsedSeconds();
        timerClockLabel.setText(TimeFormatter.clock(elapsed));
        if (!paused && !timerClockLabel.getStyleClass().contains("running")) {
            timerClockLabel.getStyleClass().add("running");
        } else if (paused) {
            timerClockLabel.getStyleClass().remove("running");
        }

        timerHeadLabel.setText(paused
                ? "Paused · " + activeSection.name()
                : "Active · " + activeSection.name());

        long sprints = todaySessions.stream()
                .filter(s -> s.sectionId() == activeSection.id() && s.seconds() >= 20 * 60)
                .count();
        timerMetaLabel.setText(
                String.format("Sprint goal: 25:00   ·   Sprints done: %d", sprints));

        double prog = Math.min(1.0, (double) elapsed / SPRINT_GOAL_SECONDS);
        sprintBar.setProgress(prog);

        pauseBtn.setDisable(false);
        stopBtn.setDisable(false);
        discardBtn.setDisable(false);
        pauseBtn.setText(paused ? "Resume" : "Pause");
    }

    private void renderBars() {
        barsBox.getChildren().clear();
        long max = 1800;
        for (Section s : sections) max = Math.max(max, secondsForSection(s.id()));

        for (Section s : sections) {
            long secs = secondsForSection(s.id());
            boolean active = activeSection != null && activeSection.id() == s.id();

            Label name = new Label(s.name());
            name.getStyleClass().add("bar-label");
            if (active) name.getStyleClass().add("bar-label-active");
            name.setMinWidth(70);

            ProgressBar bar = new ProgressBar(max > 0 ? (double) secs / max : 0);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().addAll("bar", "tone-" + s.tone());
            HBox.setHgrow(bar, Priority.ALWAYS);

            Label val = new Label(TimeFormatter.duration(secs));
            val.getStyleClass().add("bar-value");
            val.setMinWidth(70);
            val.setAlignment(Pos.CENTER_RIGHT);

            HBox row = new HBox(12, name, bar, val);
            row.setAlignment(Pos.CENTER_LEFT);
            barsBox.getChildren().add(row);
        }
    }

    private void renderStats() {
        statsBox.getChildren().clear();
        long total = totalTodaySeconds();
        long count = todaySessions.size() + (activeSection != null ? 1 : 0);
        long longest = longestSessionSeconds();
        Section longestSection = longestSessionSection();

        statsBox.getChildren().add(statRow(
                TimeFormatter.duration(total), "Total focused time", null));
        statsBox.getChildren().add(statRow(
                String.valueOf(count),
                count == 1 ? "Session logged" : "Sessions logged", null));
        statsBox.getChildren().add(statRow(
                longest > 0 ? TimeFormatter.duration(longest) : "—",
                longestSection != null ? "Longest session · " + longestSection.name()
                                       : "Longest session",
                null));
    }

    private Node statRow(String num, String label, String toneClass) {
        Label n = new Label(num);
        n.getStyleClass().add("stat-num");
        if (toneClass != null) n.getStyleClass().add(toneClass);
        n.setMinWidth(110);

        Label l = new Label(label);
        l.getStyleClass().add("stat-lbl");

        HBox row = new HBox(14, n, l);
        row.setAlignment(Pos.BASELINE_LEFT);
        row.getStyleClass().add("stat-row");
        return row;
    }

    private void renderLog() {
        logBox.getChildren().clear();
        logCountLabel.setText(todaySessions.size()
                + (todaySessions.size() == 1 ? " entry" : " entries"));

        if (todaySessions.isEmpty()) {
            Label empty = new Label("No sessions yet today. Start a timer to begin.");
            empty.getStyleClass().add("log-empty");
            logBox.getChildren().add(empty);
            return;
        }

        for (Session s : todaySessions) {
            Section sec = sections.stream()
                    .filter(x -> x.id() == s.sectionId()).findFirst().orElse(null);

            Label time = new Label(TimeFormatter.timeOf(s.startedAt())
                    + " – " + TimeFormatter.timeOf(s.endedAt()));
            time.getStyleClass().add("log-time");
            time.setMinWidth(120);

            Label name = new Label(sec != null ? sec.name() : "(deleted)");
            name.getStyleClass().add("log-name");

            Label dur = new Label(TimeFormatter.duration(s.seconds()));
            dur.getStyleClass().add("log-dur");
            dur.setMinWidth(70);
            dur.setAlignment(Pos.CENTER_RIGHT);

            Region sp = new Region();
            HBox.setHgrow(sp, Priority.ALWAYS);

            HBox row = new HBox(14, time, name, sp, dur);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("log-row");
            logBox.getChildren().add(row);
        }
    }

    // ================================================================
    // Lifecycle
    // ================================================================

    private void shutdown() {
        if (activeSection != null) logActiveSession();
        if (ticker != null) ticker.stop();
        db.close();
        javafx.application.Platform.exit();
    }

    private void showError(String header, Exception e) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText(header);
        a.setContentText(e.getMessage());
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }


}