package com.vbforge.blockforge;

import com.vbforge.blockforge.ui.GameController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * JavaFX entry point. Start through {@link Launcher} so the app also runs from a plain classpath jar.
 */
public class BlockForgeApp extends Application {

    private static final String STYLESHEET = "/com/vbforge/blockforge/vbforge-dark.css";

    @Override
    public void start(Stage stage) {
        GameController controller = new GameController(stage);

        Scene scene = new Scene(controller.root(), 1100, 760);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource(STYLESHEET), "Missing stylesheet " + STYLESHEET).toExternalForm());
        controller.installShortcuts(scene);

        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(controller::onCloseRequest);
        stage.show();

        Platform.runLater(() -> scene.getRoot().requestFocus());
    }
}

//package com.vbforge.blockforge;
//
//import com.vbforge.blockforge.engine.BoardFactory;
//import com.vbforge.blockforge.model.Block;
//import com.vbforge.blockforge.model.GameState;
//import com.vbforge.blockforge.model.Move;
//import com.vbforge.blockforge.ui.BoardView;
//import com.vbforge.blockforge.ui.ControlPanel;
//import com.vbforge.blockforge.ui.SettingsDialog;
//import com.vbforge.blockforge.ui.StatusBar;
//import javafx.application.Application;
//import javafx.application.Platform;
//import javafx.geometry.Pos;
//import javafx.scene.Scene;
//import javafx.scene.control.Button;
//import javafx.scene.layout.BorderPane;
//import javafx.scene.layout.HBox;
//import javafx.scene.layout.Priority;
//import javafx.scene.layout.Region;
//import javafx.scene.layout.StackPane;
//import javafx.stage.Stage;
//
//import java.util.Optional;
//
///**
// * JavaFX entry point. Step 7: full layout assembled — toolbar, board, control panel, status
// * bar, and working New Game / Settings dialog. Move buttons and undo/redo emit to stdout; they
// * are wired to the engine in Step 8.
// */
//public class BlockForgeApp extends Application {
//
//    //private static final String STYLESHEET = "/com/vbforge/blockforge/vbforge-dark.css";
//
//    private Stage stage;
//    private GameState state;
//
//    private final BoardView boardView = new BoardView();
//    private final ControlPanel controlPanel = new ControlPanel();
//    private final StatusBar statusBar = new StatusBar();
//
//    @Override
//    public void start(Stage stage) {
//        this.stage = stage;
//
//        state = BoardFactory.newGame(2L, 3, 2, 5);
//        boardView.setState(state);
//        boardView.setOnSelectionChanged(this::onSelectionChanged);
//
//        controlPanel.setCallbacks(this::doMove, this::doUndo, this::doRedo);
//
//        StackPane boardHolder = new StackPane(boardView);
//        boardHolder.getStyleClass().add("board-holder");
//
//        BorderPane root = new BorderPane();
//        root.setTop(buildToolbar());
//        root.setCenter(boardHolder);
//        root.setRight(controlPanel);
//        root.setBottom(statusBar);
//
//        refreshAll();
//
//        Scene scene = new Scene(root, 1100, 760);
//        scene.getStylesheets().add(stylesheetUrl());
//        stage.setTitle("BlockForge");
//        stage.setScene(scene);
//        stage.setMinWidth(900);
//        stage.setMinHeight(640);
//        stage.show();
//
//        Platform.runLater(boardView::requestFocus);
//    }
//
//    // ------------------------------------------------------------------ toolbar
//
//    private Region buildToolbar() {
//        Button newGame = toolButton("New Game", this::openSettings);
//        Button open = toolButton("Open", () -> noop("Open"));
//        Button save = toolButton("Save", () -> noop("Save"));
//        Button saveAs = toolButton("Save As", () -> noop("Save As"));
//        Button undo = toolButton("Undo", this::doUndo);
//        Button redo = toolButton("Redo", this::doRedo);
//        Button fullscreen = toolButton("Fullscreen", this::toggleFullscreen);
//
//        Region spacer = new Region();
//        HBox.setHgrow(spacer, Priority.ALWAYS);
//
//        HBox bar = new HBox(8, newGame, open, save, saveAs, spacer, undo, redo, fullscreen);
//        bar.setAlignment(Pos.CENTER_LEFT);
//        bar.getStyleClass().add("toolbar");
//        return bar;
//    }
//
//    private static Button toolButton(String text, Runnable action) {
//        Button b = new Button(text);
//        b.setOnAction(e -> action.run());
//        return b;
//    }
//
//    // ------------------------------------------------------------------ actions
//
//    /** Step 7 placeholder; wired to GameSession in Step 8. */
//    private void doMove(Move move) {
//        System.out.println("[blockforge] move: " + move + " (not yet wired to engine)");
//    }
//
//    /** Step 7 placeholder; wired to GameSession in Step 8. */
//    private void doUndo() {
//        System.out.println("[blockforge] undo (not yet wired to engine)");
//    }
//
//    /** Step 7 placeholder; wired to GameSession in Step 8. */
//    private void doRedo() {
//        System.out.println("[blockforge] redo (not yet wired to engine)");
//    }
//
//    private void openSettings() {
//        Optional<SettingsDialog.Settings> result = new SettingsDialog(
//                state.seed(), state.maxDepth(), state.playerCount(), state.maxTurns()
//        ).showAndWait();
//        result.ifPresent(s -> {
//            state = BoardFactory.newGame(s.seed(), s.maxDepth(), s.playerCount(), s.maxTurns());
//            boardView.setState(state);
//            refreshAll();
//        });
//    }
//
//    private void toggleFullscreen() {
//        stage.setFullScreen(!stage.isFullScreen());
//    }
//
//    private void noop(String what) {
//        System.out.println("[blockforge] " + what + " not yet implemented");
//    }
//
//    // ------------------------------------------------------------------ refresh
//
//    private void onSelectionChanged(Block selected) {
//        controlPanel.update(state, selected, false, false);
//        statusBar.update(state, selected);
//    }
//
//    private void refreshAll() {
//        Block selected = boardView.selected();
//        controlPanel.update(state, selected, false, false);
//        statusBar.update(state, selected);
//    }
//
//    // ------------------------------------------------------------------ stylesheet
//
//    private static String stylesheetUrl() {
//        String[] candidates = {
//                "/com/vbforge/blockforge/vbforge-dark.css",
//                "/vbforge-dark.css",
//        };
//        for (String path : candidates) {
//            var url = BlockForgeApp.class.getResource(path);
//            if (url != null) return url.toExternalForm();
//        }
//        throw new IllegalStateException(
//                "vbforge-dark.css not found on classpath. Expected at one of: "
//                        + String.join(", ", candidates));
//    }
//}