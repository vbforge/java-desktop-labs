package com.vbforge.slidecraft.ui;

import com.vbforge.slidecraft.engine.ExportManager;
import com.vbforge.slidecraft.engine.FileManager;
import com.vbforge.slidecraft.engine.PresentationFormatException;
import com.vbforge.slidecraft.engine.ThemeManager;
import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Sign;
import com.vbforge.slidecraft.model.Slide;
import com.vbforge.slidecraft.model.Theme;
import com.vbforge.slidecraft.render.SlideCanvas;
import com.vbforge.slidecraft.render.SlideSnapshot;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.prefs.Preferences;
import java.util.stream.IntStream;

/**
 * Main editor logic: builds the 3-panel UI, owns the document state (immutable
 * {@link Presentation} + dirty flag + current file) and wires file, export, theme and present actions.
 */
public final class EditorController {

    private static final System.Logger LOG = System.getLogger(EditorController.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(EditorController.class);
    private static final String PREF_LAST_DIR = "lastDirectory";
    private static final String EXTENSION = "slidecraft";

    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "slidecraft-io");
        t.setDaemon(true);
        return t;
    });

    @FunctionalInterface
    private interface IoTask {
        void run() throws IOException;
    }

    private final Stage stage;
    private final ThemeManager themes;

    // ---- document state
    private Presentation current = Presentation.blank();
    private Path currentFile;
    private boolean dirty;
    private int selected;
    private Theme importedTheme; // theme from an opened/imported file that is not a known theme
    private int quiet;           // >0 while the UI is being synced from the model (suppresses listeners)
    private boolean presenting;
    private ChangeListener<Boolean> fullScreenListener;
    private Parent restoreRoot;  // editor root to put back when the presentation ends

    // ---- widgets
    private final StackPane root = new StackPane();
    private final TextField presentationTitleField = new TextField();
    private final ListView<Integer> slideList = new ListView<>();
    private final TextField slideTitleField = new TextField();
    private final TextArea contentArea = new TextArea();
    private final TextArea notesArea = new TextArea();
    private final SlideCanvas canvas = new SlideCanvas();
    private final ComboBox<Theme> themeBox = new ComboBox<>();
    private final CheckBox signCheck = new CheckBox("Add sign to each slide");
    private final TextField signField = new TextField();
    private final Label toastLabel = new Label();
    private SequentialTransition toastAnimation;

    public EditorController(Stage stage, ThemeManager themes) {
        this.stage = stage;
        this.themes = themes;
        buildUi();
        syncUiFromModel();
    }

    public Parent root() {
        return root;
    }

    // =====================================================================================
    // UI construction
    // =====================================================================================

    private void buildUi() {
        BorderPane main = new BorderPane();
        main.setTop(buildTopBar());
        main.setLeft(buildLeftPanel());
        main.setCenter(buildCenterPanel());
        main.setRight(buildRightPanel());

        toastLabel.getStyleClass().add("toast");
        toastLabel.setOpacity(0);
        toastLabel.setMouseTransparent(true);
        StackPane.setAlignment(toastLabel, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toastLabel, new Insets(0, 0, 24, 0));

        root.getChildren().addAll(main, toastLabel);
    }

    private Region buildTopBar() {
        Button newBtn = toolButton("New", this::newPresentation);
        Button openBtn = toolButton("Open", this::openPresentation);
        Button saveBtn = toolButton("Save", () -> save());

        presentationTitleField.getStyleClass().add("pres-title");
        presentationTitleField.setPromptText(Presentation.DEFAULT_TITLE);
        presentationTitleField.setAlignment(Pos.CENTER);
        presentationTitleField.setPrefWidth(300);
        presentationTitleField.setMaxWidth(300);
        presentationTitleField.textProperty().addListener((o, a, b) -> {
            if (quiet > 0) {
                return;
            }
            current = current.withTitle(b);
            markDirty();
        });

        Button importBtn = toolButton("Import", this::importPresentation);

        MenuButton export = new MenuButton("Export");
        export.getItems().addAll(
                item("Export as PDF…", this::exportPdf),
                item("Export as TXT…", this::exportTxt),
                item("Export as Markdown…", this::exportMarkdown),
                new javafx.scene.control.SeparatorMenuItem(),
                item("Export PNG (current slide)…", this::exportPngCurrent),
                item("Export PNG (all slides)…", this::exportPngAll),
                new javafx.scene.control.SeparatorMenuItem(),
                item("Export as .slidecraft (JSON)…", this::exportJson));

        Button present = new Button("▶ Present");
        present.getStyleClass().add("present");
        present.setOnAction(e -> present());

        HBox leftGroup = new HBox(8, newBtn, openBtn, saveBtn);
        leftGroup.setAlignment(Pos.CENTER_LEFT);
        HBox rightGroup = new HBox(8, importBtn, export, present);
        rightGroup.setAlignment(Pos.CENTER_RIGHT);

        StackPane leftWrapper = new StackPane(leftGroup);
        leftWrapper.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(leftWrapper, Priority.ALWAYS);

        StackPane rightWrapper = new StackPane(rightGroup);
        rightWrapper.setAlignment(Pos.CENTER_RIGHT);
        HBox.setHgrow(rightWrapper, Priority.ALWAYS);

        HBox bar = new HBox(leftWrapper, presentationTitleField, rightWrapper);
        bar.setAlignment(Pos.CENTER);
        bar.getStyleClass().add("top-bar");
        return bar;
    }

    private Region buildLeftPanel() {
        slideList.getStyleClass().add("slide-list");
        slideList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Integer idx, boolean empty) {
                super.updateItem(idx, empty);
                if (empty || idx == null || idx >= current.slides().size()) {
                    setText(null);
                    return;
                }
                String title = current.slides().get(idx).title();
                setText(String.format("%02d   %s", idx + 1, title.isBlank() ? "Untitled" : title));
            }
        });
        slideList.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> {
            if (quiet == 0 && b.intValue() >= 0) {
                selectSlide(b.intValue());
            }
        });

        Button add = new Button("+ Add");
        add.setOnAction(e -> addSlide());
        Button delete = new Button("− Delete");
        delete.getStyleClass().add("danger");
        delete.setOnAction(e -> deleteSlide());
        HBox.setHgrow(add, Priority.ALWAYS);
        HBox.setHgrow(delete, Priority.ALWAYS);
        add.setMaxWidth(Double.MAX_VALUE);
        delete.setMaxWidth(Double.MAX_VALUE);
        HBox actions = new HBox(8, add, delete);

        VBox.setVgrow(slideList, Priority.ALWAYS);
        VBox panel = new VBox(12, slideList, actions);
        panel.getStyleClass().addAll("side-panel", "left");
        panel.setPrefWidth(220);
        panel.setMinWidth(180);
        return panel;
    }

    private Region buildCenterPanel() {
        slideTitleField.textProperty().addListener((o, a, b) -> onSlideEdited());
        contentArea.textProperty().addListener((o, a, b) -> onSlideEdited());
        notesArea.textProperty().addListener((o, a, b) -> onSlideEdited());
        contentArea.setPrefRowCount(4);
        contentArea.setWrapText(true);
        notesArea.setPrefRowCount(2);
        notesArea.setWrapText(true);

        StackPane previewHolder = new StackPane(canvas);
        previewHolder.getStyleClass().add("preview-holder");
        previewHolder.setMinHeight(120);
        VBox.setVgrow(previewHolder, Priority.ALWAYS);

        VBox panel = new VBox(12,
                labeled("SLIDE TITLE", slideTitleField),
                labeled("SLIDE CONTENT", contentArea),
                labeled("SPEAKER NOTES (HIDDEN IN PRESENTATION)", notesArea),
                previewHolder);
        panel.getStyleClass().add("center-panel");
        return panel;
    }

    private Region buildRightPanel() {
        themeBox.setMaxWidth(Double.MAX_VALUE);
        themeBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Theme t) {
                if (t == null) {
                    return "";
                }
                return t.equals(importedTheme) ? t.name() + " (imported)" : t.name();
            }

            @Override
            public Theme fromString(String s) {
                return null;
            }
        });
        themeBox.getSelectionModel().selectedItemProperty().addListener((o, a, theme) -> onThemeSelected(theme));

        signCheck.selectedProperty().addListener((o, a, enabled) -> {
            signField.setDisable(!enabled);
            onSignEdited();
        });
        signField.setPromptText("e.g. © 2026 Your Name");
        signField.setDisable(true);
        signField.setTextFormatter(new TextFormatter<String>(
                change -> change.getControlNewText().length() <= Sign.MAX_LENGTH ? change : null));
        signField.textProperty().addListener((o, a, b) -> onSignEdited());

        Button customTheme = new Button("Create Custom Theme");
        customTheme.setMaxWidth(Double.MAX_VALUE);
        customTheme.setOnAction(e -> createCustomTheme());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox panel = new VBox(14,
                sectionTitle("SLIDE THEME"), themeBox,
                sectionTitle("SIGN / FOOTER"), signCheck, signField,
                spacer, customTheme);
        panel.getStyleClass().addAll("side-panel", "right");
        panel.setPrefWidth(280);
        panel.setMinWidth(240);
        return panel;
    }

    private static Button toolButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return b;
    }

    private static MenuItem item(String text, Runnable action) {
        MenuItem m = new MenuItem(text);
        m.setOnAction(e -> action.run());
        return m;
    }

    private static VBox labeled(String label, Region control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        return new VBox(4, l, control);
    }

    private static Label sectionTitle(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section-title");
        return l;
    }

    /** Registers keyboard shortcuts. They are inert while a presentation is running. */
    public void installShortcuts(Scene scene) {
        bind(scene, new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::newPresentation);
        bind(scene, new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), this::openPresentation);
        bind(scene, new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> save());
        bind(scene, new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                () -> saveAs());
        bind(scene, new KeyCodeCombination(KeyCode.F5), this::present);
    }

    private void bind(Scene scene, KeyCombination combo, Runnable action) {
        scene.getAccelerators().put(combo, () -> {
            if (!presenting) {
                action.run();
            }
        });
    }

    // =====================================================================================
    // Model <-> UI sync
    // =====================================================================================

    private void quietly(Runnable r) {
        quiet++;
        try {
            r.run();
        } finally {
            quiet--;
        }
    }

    /** Pushes the whole model into the widgets (after New / Open / Import). */
    private void syncUiFromModel() {
        quietly(() -> {
            presentationTitleField.setText(current.title());
            signCheck.setSelected(current.sign().enabled());
            signField.setText(current.sign().text());
            signField.setDisable(!current.sign().enabled());
            refreshThemeBox();
            slideList.setItems(FXCollections.observableArrayList(
                    IntStream.range(0, current.slides().size()).boxed().toList()));
            selectSlide(Math.min(selected, current.slides().size() - 1));
        });
        refreshWindowTitle();
    }

    private void refreshThemeBox() {
        quietly(() -> {
            List<Theme> all = new ArrayList<>(themes.allThemes());
            Theme theme = current.theme();
            importedTheme = all.contains(theme) ? null : theme;
            if (importedTheme != null) {
                all.add(importedTheme);
            }
            themeBox.setItems(FXCollections.observableArrayList(all));
            themeBox.setValue(theme);
        });
    }

    private void selectSlide(int index) {
        quietly(() -> {
            selected = Math.max(0, Math.min(index, current.slides().size() - 1));
            Slide s = current.slides().get(selected);
            slideTitleField.setText(s.title());
            contentArea.setText(s.content());
            notesArea.setText(s.notes());
            if (slideList.getSelectionModel().getSelectedIndex() != selected) {
                slideList.getSelectionModel().select(selected);
            }
        });
        refreshPreview();
    }

    private void refreshPreview() {
        canvas.show(current, current.slides().get(selected));
    }

    // =====================================================================================
    // Edit handlers
    // =====================================================================================

    private void onSlideEdited() {
        if (quiet > 0) {
            return;
        }
        Slide edited = new Slide(slideTitleField.getText(), contentArea.getText(), notesArea.getText());
        current = current.withSlide(selected, edited);
        markDirty();
        refreshPreview();
        slideList.refresh();
    }

    private void onThemeSelected(Theme theme) {
        if (quiet > 0 || theme == null) {
            return;
        }
        current = current.withTheme(theme);
        if (importedTheme != null && !importedTheme.equals(theme)) {
            // Picking a different theme discards the synthetic "imported" entry.
            Theme discarded = importedTheme;
            quietly(() -> {
                importedTheme = null;
                themeBox.getItems().remove(discarded);
                themeBox.setValue(theme);
            });
        }
        markDirty();
        refreshPreview();
    }

    private void onSignEdited() {
        if (quiet > 0) {
            return;
        }
        current = current.withSign(new Sign(signCheck.isSelected(), signField.getText()));
        markDirty();
        refreshPreview();
    }

    private void addSlide() {
        int at = selected + 1;
        current = current.withSlideInserted(at, Slide.blank());
        markDirty();
        reloadSlideList(at);
    }

    private void deleteSlide() {
        if (current.slides().size() <= 1) {
            toast("A presentation needs at least one slide", true);
            return;
        }
        current = current.withSlideRemoved(selected);
        markDirty();
        reloadSlideList(Math.min(selected, current.slides().size() - 1));
    }

    private void reloadSlideList(int select) {
        quietly(() -> slideList.setItems(FXCollections.observableArrayList(
                IntStream.range(0, current.slides().size()).boxed().toList())));
        selectSlide(select);
    }

    private void createCustomTheme() {
        Optional<Theme> result = new CustomThemeDialog(stage, current.theme()).showAndWait();
        result.ifPresent(theme -> {
            try {
                themes.saveCustom(theme);
            } catch (IllegalArgumentException e) {
                error("Can't save theme", e.getMessage());
                return;
            } catch (IOException e) {
                LOG.log(System.Logger.Level.ERROR, "Saving custom theme failed", e);
                error("Can't save theme", "Could not write to " + themes.directory() + ": " + e.getMessage());
                return;
            }
            current = current.withTheme(theme);
            markDirty();
            refreshThemeBox();
            refreshPreview();
            toast("Saved theme \"" + theme.name() + "\"", false);
        });
    }

    // =====================================================================================
    // File operations
    // =====================================================================================

    private void newPresentation() {
        if (!confirmDiscard()) {
            return;
        }
        replaceDocument(Presentation.blank(), null, false);
    }

    private void openPresentation() {
        if (!confirmDiscard()) {
            return;
        }
        File file = presentationChooser("Open Presentation").showOpenDialog(stage);
        if (file != null) {
            remember(file);
            load(file.toPath(), false);
        }
    }

    private void importPresentation() {
        if (!confirmDiscard()) {
            return;
        }
        File file = presentationChooser("Import Presentation").showOpenDialog(stage);
        if (file != null) {
            remember(file);
            load(file.toPath(), true);
        }
    }

    /** Parses first, then swaps state: an invalid file never leaves a partial load. */
    private void load(Path path, boolean asImport) {
        Presentation loaded;
        try {
            loaded = FileManager.read(path);
        } catch (PresentationFormatException e) {
            error("Import failed", "Invalid file: " + e.getMessage());
            return;
        } catch (IOException e) {
            error("Import failed", "Could not read file: " + e.getMessage());
            return;
        }
        selected = 0;
        replaceDocument(loaded, asImport ? null : path, asImport);
        toast((asImport ? "Imported \"" : "Opened \"") + loaded.title() + "\" (" + loaded.slides().size() + " slides)", false);
    }

    private void replaceDocument(Presentation presentation, Path file, boolean dirtyAfter) {
        current = presentation;
        currentFile = file;
        dirty = dirtyAfter;
        selected = 0;
        syncUiFromModel();
    }

    /** @return true if the document was saved. */
    private boolean save() {
        return currentFile == null ? saveAs() : writeTo(currentFile);
    }

    private boolean saveAs() {
        FileChooser chooser = presentationChooser("Save Presentation");
        chooser.setInitialFileName(fileNameFor(current.title()) + "." + EXTENSION);
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return false;
        }
        remember(file);
        Path path = file.toPath();
        String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!lower.endsWith("." + EXTENSION) && !lower.endsWith(".json")) {
            path = path.resolveSibling(path.getFileName() + "." + EXTENSION);
        }
        return writeTo(path);
    }

    private boolean writeTo(Path path) {
        try {
            FileManager.write(current, path);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "Save failed", e);
            error("Save failed", "Could not save " + path.getFileName() + ": " + e.getMessage());
            return false;
        }
        currentFile = path;
        dirty = false;
        refreshWindowTitle();
        toast("Saved " + path.getFileName(), false);
        return true;
    }

    /** Asks what to do with unsaved changes. @return true if it is OK to continue. */
    private boolean confirmDiscard() {
        if (!dirty) {
            return true;
        }
        ButtonType saveType = new ButtonType("Save", ButtonData.YES);
        ButtonType discardType = new ButtonType("Don't Save", ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "\"" + current.title() + "\" has unsaved changes. Save them first?",
                saveType, discardType, ButtonType.CANCEL);
        style(alert);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(null);
        ButtonType choice = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == saveType) {
            return save();
        }
        return choice == discardType;
    }

    /** Wire to {@code Stage.setOnCloseRequest}. */
    public void onCloseRequest(WindowEvent event) {
        if (presenting) {
            exitPresentation();
        }
        if (!confirmDiscard()) {
            event.consume();
        }
    }

    public void openOnStartup(Path path) {
        if (Files.isRegularFile(path)) {
            load(path, false);
        }
    }

    // =====================================================================================
    // Export
    // =====================================================================================

    private void exportPdf() {
        Path target = chooseExportFile("Export as PDF", "PDF document", "pdf", fileNameFor(current.title()) + ".pdf");
        if (target == null) {
            return;
        }
        Presentation snapshot = current;
        List<BufferedImage> images;
        try {
            images = SlideSnapshot.renderAll(snapshot);
        } catch (RuntimeException e) {
            failExport(e);
            return;
        }
        runAsync(() -> ExportManager.exportPdf(images, snapshot.title(), target),
                () -> toast("Exported " + target.getFileName(), false));
    }

    private void exportTxt() {
        Path target = chooseExportFile("Export as TXT", "Text file", "txt", fileNameFor(current.title()) + ".txt");
        if (target != null) {
            Presentation snapshot = current;
            runAsync(() -> ExportManager.exportTxt(snapshot, target),
                    () -> toast("Exported " + target.getFileName(), false));
        }
    }

    private void exportMarkdown() {
        Path target = chooseExportFile("Export as Markdown", "Markdown", "md", fileNameFor(current.title()) + ".md");
        if (target != null) {
            Presentation snapshot = current;
            runAsync(() -> ExportManager.exportMarkdown(snapshot, target),
                    () -> toast("Exported " + target.getFileName(), false));
        }
    }

    private void exportJson() {
        Path target = chooseExportFile("Export as .slidecraft", "SlideCraft presentation", EXTENSION,
                fileNameFor(current.title()) + "." + EXTENSION);
        if (target != null) {
            Presentation snapshot = current;
            runAsync(() -> ExportManager.exportJson(snapshot, target),
                    () -> toast("Exported " + target.getFileName(), false));
        }
    }

    private void exportPngCurrent() {
        Path target = chooseExportFile("Export current slide as PNG", "PNG image", "png",
                ExportManager.pngFileName(selected));
        if (target == null) {
            return;
        }
        BufferedImage image;
        try {
            image = SlideSnapshot.render(current, selected);
        } catch (RuntimeException e) {
            failExport(e);
            return;
        }
        runAsync(() -> ExportManager.exportPng(image, target),
                () -> toast("Exported " + target.getFileName(), false));
    }

    private void exportPngAll() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose a folder for the PNG files");
        applyLastDirectory(chooser);
        File dir = chooser.showDialog(stage);
        if (dir == null) {
            return;
        }
        PREFS.put(PREF_LAST_DIR, dir.getAbsolutePath());
        Path directory = dir.toPath();

        long existing = ExportManager.pngTargets(directory, current.slides().size())
                .stream().filter(Files::exists).count();
        if (existing > 0 && !confirm("Overwrite files?",
                existing + " file(s) named slide-NN.png already exist in this folder. Overwrite them?")) {
            return;
        }
        List<BufferedImage> images;
        try {
            images = SlideSnapshot.renderAll(current);
        } catch (RuntimeException e) {
            failExport(e);
            return;
        }
        runAsync(() -> ExportManager.exportPngSet(images, directory),
                () -> toast("Exported " + images.size() + " PNG files", false));
    }

    private Path chooseExportFile(String title, String description, String ext, String initialName) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*." + ext));
        chooser.setInitialFileName(initialName);
        applyLastDirectory(chooser);
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return null;
        }
        remember(file);
        Path path = file.toPath();
        if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            path = path.resolveSibling(path.getFileName() + "." + ext);
        }
        return path;
    }

    private void runAsync(IoTask task, Runnable onSuccess) {
        CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, IO).whenComplete((v, failure) -> Platform.runLater(() -> {
            if (failure != null) {
                failExport(failure);
            } else {
                onSuccess.run();
            }
        }));
    }

    private void failExport(Throwable failure) {
        Throwable cause = failure;
        while (cause.getCause() != null && (cause instanceof java.util.concurrent.CompletionException
                || cause instanceof UncheckedIOException)) {
            cause = cause.getCause();
        }
        LOG.log(System.Logger.Level.ERROR, "Export failed", failure);
        error("Export failed", cause.getMessage() != null ? cause.getMessage() : cause.toString());
    }

    // =====================================================================================
    // Presentation mode
    // =====================================================================================

    private void present() {
        if (presenting) {
            return;
        }
        Scene scene = stage.getScene();
        Parent editorRoot = scene.getRoot();
        presenting = true;

        PresentationView view = new PresentationView(current, selected, this::exitPresentation);
        scene.setRoot(view);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH); // Esc is handled by the view
        stage.setFullScreen(true);
        Platform.runLater(view::requestFocus);

        // If the OS leaves fullscreen (e.g. Alt+Tab, Cmd+Ctrl+F), end the presentation too.
        fullScreenListener = (o, was, isFull) -> {
            if (!isFull) {
                exitPresentation();
            }
        };
        stage.fullScreenProperty().addListener(fullScreenListener);
        restoreRoot = editorRoot;
    }

    private void exitPresentation() {
        if (!presenting) {
            return;
        }
        presenting = false;
        if (fullScreenListener != null) {
            stage.fullScreenProperty().removeListener(fullScreenListener);
            fullScreenListener = null;
        }
        stage.setFullScreen(false);
        stage.setFullScreenExitKeyCombination(null); // back to the platform default
        stage.setFullScreenExitHint(null);
        if (restoreRoot != null) {
            stage.getScene().setRoot(restoreRoot);
            restoreRoot = null;
        }
    }

    // =====================================================================================
    // Small helpers: dialogs, toast, preferences
    // =====================================================================================

    private void markDirty() {
        if (!dirty) {
            dirty = true;
        }
        refreshWindowTitle();
    }

    private void refreshWindowTitle() {
        stage.setTitle((dirty ? "• " : "") + current.title() + " — SlideCraft");
    }

    private void toast(String message, boolean isError) {
        toastLabel.setText(message);
        toastLabel.getStyleClass().remove("error");
        if (isError) {
            toastLabel.getStyleClass().add("error");
        }
        if (toastAnimation != null) {
            toastAnimation.stop();
        }
        FadeTransition in = new FadeTransition(Duration.millis(200), toastLabel);
        in.setToValue(1);
        PauseTransition hold = new PauseTransition(Duration.millis(2200));
        FadeTransition out = new FadeTransition(Duration.millis(300), toastLabel);
        out.setToValue(0);
        toastAnimation = new SequentialTransition(in, hold, out);
        toastAnimation.play();
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        style(alert);
        alert.setTitle(header);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private boolean confirm(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.OK, ButtonType.CANCEL);
        style(alert);
        alert.setTitle(header);
        alert.setHeaderText(header);
        return alert.showAndWait().filter(b -> b == ButtonType.OK).isPresent();
    }

    private void style(Alert alert) {
        alert.initOwner(stage);
        alert.getDialogPane().getStylesheets().add(Styles.dark());
    }

    private FileChooser presentationChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("SlideCraft presentations (*.slidecraft, *.json)", "*.slidecraft", "*.json"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        applyLastDirectory(chooser);
        return chooser;
    }

    private void applyLastDirectory(FileChooser chooser) {
        File dir = lastDirectory();
        if (dir != null) {
            chooser.setInitialDirectory(dir);
        }
    }

    private void applyLastDirectory(DirectoryChooser chooser) {
        File dir = lastDirectory();
        if (dir != null) {
            chooser.setInitialDirectory(dir);
        }
    }

    private static File lastDirectory() {
        String stored = PREFS.get(PREF_LAST_DIR, null);
        if (stored == null) {
            return null;
        }
        File dir = new File(stored);
        return dir.isDirectory() ? dir : null;
    }

    private static void remember(File file) {
        File parent = file.getParentFile();
        if (parent != null) {
            PREFS.put(PREF_LAST_DIR, parent.getAbsolutePath());
        }
    }

    private static String fileNameFor(String title) {
        String s = title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", " ").trim();
        return s.isEmpty() ? "presentation" : s;
    }
}
