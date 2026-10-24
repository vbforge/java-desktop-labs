package com.vbforge.slidecraft;

import com.vbforge.slidecraft.engine.ThemeManager;
import com.vbforge.slidecraft.ui.EditorController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** JavaFX entry point. Start through {@link Launcher} so the app also runs from a plain classpath jar. */
public class SlideCraftApp extends Application {

    private static final String STYLESHEET = "/com/vbforge/slidecraft/vbforge-dark.css";

    @Override
    public void start(Stage stage) {
        EditorController controller = new EditorController(stage, new ThemeManager());

        Scene scene = new Scene(controller.root(), 1280, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource(STYLESHEET), "Missing stylesheet " + STYLESHEET).toExternalForm());
        controller.installShortcuts(scene);

        stage.setScene(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setOnCloseRequest(controller::onCloseRequest);
        stage.show();

        // `slidecraft <file>` (or "open with") opens the file on startup.
        List<String> args = getParameters().getUnnamed();
        if (!args.isEmpty()) {
            controller.openOnStartup(Path.of(args.get(0)));
        }
    }
}
