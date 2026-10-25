package com.vbforge.blockforge.ui;

import com.vbforge.blockforge.engine.BoardFactory;
import com.vbforge.blockforge.engine.GameFormatException;
import com.vbforge.blockforge.engine.GameSaveLoad;
import com.vbforge.blockforge.engine.GameSession;
import com.vbforge.blockforge.model.Block;
import com.vbforge.blockforge.model.GameState;
import com.vbforge.blockforge.model.Move;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Glue between the engine and the UI: owns the {@link GameSession}, wires every widget to its
 * action, tracks the current file and dirty flag, and shows the game-over dialog.
 */
public final class GameController {

    private static final System.Logger LOG = System.getLogger(GameController.class.getName());
    private static final Preferences PREFS = Preferences.userNodeForPackage(GameController.class);
    private static final String PREF_LAST_DIR = "lastDirectory";
    private static final String EXT = "blockforge";

    private final Stage stage;
    private final BoardView boardView = new BoardView();
    private final ControlPanel controlPanel = new ControlPanel();
    private final StatusBar statusBar = new StatusBar();
    private final BorderPane root = new BorderPane();

    private GameSession session;
    private Path currentFile;
    private boolean dirty;
    private boolean gameOverAnnounced;

    public GameController(Stage stage) {
        this.stage = stage;
        this.session = new GameSession(BoardFactory.newGame(2L, 3, 2, 5));

        boardView.setState(session.state());
        boardView.setOnSelectionChanged(this::onSelectionChanged);
        controlPanel.setCallbacks(this::doMove, this::doUndo, this::doRedo);

        StackPane boardHolder = new StackPane(boardView);
        boardHolder.getStyleClass().add("board-holder");

        root.setTop(buildToolbar());
        root.setCenter(boardHolder);
        root.setRight(controlPanel);
        root.setBottom(statusBar);

        refreshUI();
        updateTitle();
    }

    public Parent root() {
        return root;
    }

    public void installShortcuts(Scene scene) {
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::doNewGame);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN), this::doOpen);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::doSave);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                this::doSaveAs);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F11), this::toggleFullscreen);

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT1), () -> doMove(Move.ROTATE_CW));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT2), () -> doMove(Move.ROTATE_CCW));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT3), () -> doMove(Move.REFLECT_X));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT4), () -> doMove(Move.REFLECT_Y));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.DIGIT5), () -> doMove(Move.SMASH));

        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.U), this::doUndo);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN), this::doUndo);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R), this::doRedo);
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Y, KeyCombination.SHORTCUT_DOWN), this::doRedo);
    }

    public void onCloseRequest(WindowEvent e) {
        if (!confirmDiscardIfDirty()) {
            e.consume();
        }
    }

    // ------------------------------------------------------------------ toolbar

    private Region buildToolbar() {
        Button newGame = toolButton("New Game", this::doNewGame);
        Button open = toolButton("Open", this::doOpen);
        Button save = toolButton("Save", this::doSave);
        Button saveAs = toolButton("Save As", this::doSaveAs);
        Button undo = toolButton("Undo", this::doUndo);
        Button redo = toolButton("Redo", this::doRedo);
        Button fullscreen = toolButton("Fullscreen", this::toggleFullscreen);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bar = new HBox(8, newGame, open, save, saveAs, spacer, undo, redo, fullscreen);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("toolbar");
        return bar;
    }

    private static Button toolButton(String text, Runnable action) {
        Button b = new Button(text);
        b.setOnAction(e -> action.run());
        return b;
    }

    // ------------------------------------------------------------------ moves

    private void doMove(Move move) {
        if (session.state().isGameOver()) return;
        Block sel = boardView.selected();
        if (sel == null) return;

        int cx = boardView.selectedUnitX();
        int cy = boardView.selectedUnitY();
        int cl = sel.level();

        if (!session.applyMove(cx, cy, cl, move)) {
            return; // move was rejected; nothing changed
        }

        boardView.setState(session.state());
        boardView.selectByCoordinates(cx, cy, cl); // fires onSelectionChanged -> refreshUI

        onStateChanged();
        maybeAnnounceGameOver();
    }

    private void doUndo() {
        if (!session.undo()) return;
        boardView.setState(session.state());
        onStateChanged();
    }

    private void doRedo() {
        if (!session.redo()) return;
        boardView.setState(session.state());
        onStateChanged();
    }

    private void onSelectionChanged(Block selected) {
        refreshUI();
    }

    private void refreshUI() {
        controlPanel.update(session.state(), boardView.selected(),
                session.canUndo(), session.canRedo());
        statusBar.update(session.state(), boardView.selected());
    }

    private void onStateChanged() {
        if (!session.state().isGameOver()) {
            gameOverAnnounced = false;
        }
        dirty = true;
        updateTitle();
    }

    private void maybeAnnounceGameOver() {
        if (!session.state().isGameOver() || gameOverAnnounced) return;
        gameOverAnnounced = true;

        int[] scores = session.state().scores();
        String body;
        if (session.state().playerCount() == 1) {
            body = "Your final score is " + scores[0] + ".";
        } else if (scores[0] == scores[1]) {
            body = "It's a tie! Both players scored " + scores[0] + ".";
        } else {
            int winner = scores[0] > scores[1] ? 1 : 2;
            body = "Player " + winner + " wins!\n\n"
                    + "Player 1: " + scores[0] + "\n"
                    + "Player 2: " + scores[1];
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION, body, ButtonType.OK);
        style(alert);
        alert.setTitle("Game Over");
        alert.setHeaderText("Game over");
        alert.showAndWait();
    }

    // ------------------------------------------------------------------ new / open / save

    private void doNewGame() {
        if (!confirmDiscardIfDirty()) return;
        Optional<SettingsDialog.Settings> result = new SettingsDialog(
                session.state().seed(),
                session.state().maxDepth(),
                session.state().playerCount(),
                session.state().maxTurns()
        ).showAndWait();
        result.ifPresent(s -> {
            session = new GameSession(
                    BoardFactory.newGame(s.seed(), s.maxDepth(), s.playerCount(), s.maxTurns()));
            currentFile = null;
            dirty = false;
            gameOverAnnounced = false;
            boardView.setState(session.state());
            refreshUI();
            updateTitle();
        });
    }

    private void doOpen() {
        if (!confirmDiscardIfDirty()) return;
        FileChooser chooser = gameChooser("Open Game");
        File file = chooser.showOpenDialog(stage);
        if (file == null) return;
        remember(file);
        try {
            GameState loaded = GameSaveLoad.read(file.toPath());
            session = new GameSession(loaded);
            currentFile = file.toPath();
            dirty = false;
            gameOverAnnounced = loaded.isGameOver();
            boardView.setState(session.state());
            refreshUI();
            updateTitle();
        } catch (GameFormatException e) {
            error("Open failed", "Invalid file: " + e.getMessage());
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "Open failed", e);
            error("Open failed", "Could not read file: " + e.getMessage());
        }
    }

    private void doSave() {
        if (currentFile == null) {
            doSaveAs();
        } else {
            writeTo(currentFile);
        }
    }

    private void doSaveAs() {
        FileChooser chooser = gameChooser("Save Game");
        int cells = 1 << session.state().maxDepth();
        chooser.setInitialFileName("game-" + session.state().seed() + "-" + cells + "x" + cells + "." + EXT);
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        remember(file);

        Path path = file.toPath();
        String lower = path.getFileName().toString().toLowerCase();
        if (!lower.endsWith("." + EXT) && !lower.endsWith(".json")) {
            path = path.resolveSibling(path.getFileName() + "." + EXT);
        }
        writeTo(path);
    }

    private void writeTo(Path path) {
        try {
            GameSaveLoad.write(session.state(), path);
        } catch (IOException e) {
            LOG.log(System.Logger.Level.ERROR, "Save failed", e);
            error("Save failed", "Could not save " + path.getFileName() + ": " + e.getMessage());
            return;
        }
        currentFile = path;
        dirty = false;
        updateTitle();
    }

    // ------------------------------------------------------------------ window helpers

    private void updateTitle() {
        String name = currentFile != null ? currentFile.getFileName().toString() : "untitled";
        stage.setTitle((dirty ? "• " : "") + name + " — BlockForge");
    }

    private void toggleFullscreen() {
        stage.setFullScreen(!stage.isFullScreen());
    }

    private boolean confirmDiscardIfDirty() {
        if (!dirty) return true;
        ButtonType save = new ButtonType("Save", ButtonData.YES);
        ButtonType discard = new ButtonType("Don't Save", ButtonData.NO);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "This game has unsaved changes. Save them first?",
                save, discard, ButtonType.CANCEL);
        style(alert);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText(null);
        ButtonType choice = alert.showAndWait().orElse(ButtonType.CANCEL);
        if (choice == save) {
            doSave();
            return !dirty; // save dialog may have been cancelled
        }
        return choice == discard;
    }

    private void error(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        style(alert);
        alert.setTitle(header);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private void style(Alert alert) {
        alert.initOwner(stage);
        var url = GameController.class.getResource("/com/vbforge/blockforge/vbforge-dark.css");
        if (url == null) {
            url = GameController.class.getResource("/vbforge-dark.css");
        }
        if (url != null) {
            alert.getDialogPane().getStylesheets().add(url.toExternalForm());
        }
    }

    // ------------------------------------------------------------------ file chooser

    private FileChooser gameChooser(String title) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("BlockForge games (*." + EXT + ", *.json)",
                        "*." + EXT, "*.json"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        String last = PREFS.get(PREF_LAST_DIR, null);
        if (last != null) {
            File dir = new File(last);
            if (dir.isDirectory()) chooser.setInitialDirectory(dir);
        }
        return chooser;
    }

    private static void remember(File file) {
        File parent = file.getParentFile();
        if (parent != null) {
            PREFS.put(PREF_LAST_DIR, parent.getAbsolutePath());
        }
    }
}