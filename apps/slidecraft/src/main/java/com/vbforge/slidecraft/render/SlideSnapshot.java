package com.vbforge.slidecraft.render;

import com.vbforge.slidecraft.model.Presentation;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Renders slides off-screen to 1920x1080 images. Must be called on the JavaFX Application Thread. */
public final class SlideSnapshot {

    public static final int WIDTH = (int) SlideView.WIDTH;
    public static final int HEIGHT = (int) SlideView.HEIGHT;

    private SlideSnapshot() {
    }

    public static BufferedImage render(Presentation presentation, int slideIndex) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("Slides must be rendered on the JavaFX Application Thread");
        }
        SlideView view = new SlideView();
        view.update(presentation, presentation.slides().get(slideIndex));

        // A scene is needed so CSS and layout are applied before the snapshot.
        Scene scene = new Scene(view, WIDTH, HEIGHT);
        view.applyCss();
        view.layout();

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = view.snapshot(params, new WritableImage(WIDTH, HEIGHT));
        BufferedImage result = SwingFXUtils.fromFXImage(image, null);
        scene.setRoot(new javafx.scene.layout.Pane()); // detach so the view can be garbage collected
        return result;
    }

    public static List<BufferedImage> renderAll(Presentation presentation) {
        List<BufferedImage> images = new ArrayList<>(presentation.slides().size());
        for (int i = 0; i < presentation.slides().size(); i++) {
            images.add(render(presentation, i));
        }
        return images;
    }
}
