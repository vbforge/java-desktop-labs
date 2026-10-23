package com.vbforge.fitapp.ui;

import com.vbforge.fitapp.db.AppPaths;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Auto-playing picture slideshow: crossfade + slow zoom/pan ("Ken Burns") + progress dots.
 *
 * Picture sources, first match wins:
 *   1. ~/.fitapp/slides/*.jpg|jpeg|png   (user's own pictures, no rebuild needed)
 *   2. classpath /images/slides/slide1..slide10 (.jpg/.jpeg/.png), bundled in the jar
 *   3. built-in gradient placeholders, so the screen still looks fine with no pictures
 */
public class SlideshowPane extends StackPane {

    private static final Duration INTERVAL = Duration.seconds(4);   // time each slide stays
    private static final Duration FADE = Duration.millis(1100);     // crossfade length
    private static final Duration DRIFT = Duration.seconds(9);      // zoom/pan length per slide
    private static final double ZOOM = 1.10;
    private static final int MAX_IMAGE_WIDTH = 1600;
    private static final Color ACCENT = Color.web("#f89820");

    /** Sizes slides to "cover" the pane (like CSS background-size: cover). */
    private static final class SlideLayer extends Pane {
        @Override
        protected void layoutChildren() {
            double w = getWidth();
            double h = getHeight();
            for (Node n : getChildren()) {
                if (n instanceof ImageView iv && iv.getImage() != null) {
                    Image img = iv.getImage();
                    double scale = Math.max(w / img.getWidth(), h / img.getHeight());
                    double fw = img.getWidth() * scale;
                    double fh = img.getHeight() * scale;
                    iv.setPreserveRatio(false);
                    iv.setFitWidth(fw);
                    iv.setFitHeight(fh);
                    iv.relocate((w - fw) / 2, (h - fh) / 2);
                } else if (n instanceof Region r) {
                    r.resizeRelocate(0, 0, w, h);
                }
            }
        }
    }

    private final SlideLayer layer = new SlideLayer();
    private final List<Node> slides = new ArrayList<>();
    private final List<Circle> dots = new ArrayList<>();
    private final Timeline timer = new Timeline();
    private int current = -1;

    public SlideshowPane() {
        setMinWidth(0);
        setStyle("-fx-background-color: #0d1117;");
        loadSlides();

        // dark gradient so text/dots stay readable on bright pictures
        Region shade = new Region();
        shade.setMouseTransparent(true);
        shade.setBackground(new Background(new BackgroundFill(
                new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.rgb(13, 17, 23, 0.35)),
                        new Stop(0.5, Color.rgb(13, 17, 23, 0.10)),
                        new Stop(1, Color.rgb(13, 17, 23, 0.80))), null, null)));

        Label brand = new Label("FITAPP");
        brand.setStyle("-fx-font-size: 48px; -fx-font-weight: 900; -fx-text-fill: rgba(255,255,255,0.88);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 12, 0.2, 0, 2);");
        Label tagline = new Label("Plan it. Lift it. Track it.");
        tagline.setStyle("-fx-font-size: 15px; -fx-text-fill: rgba(255,255,255,0.80);"
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 8, 0.2, 0, 1);");
        VBox caption = new VBox(4, brand, tagline);
//        caption.setAlignment(Pos.CENTER);
        caption.setAlignment(Pos.TOP_CENTER);
        caption.setMouseTransparent(true);

        HBox dotBox = new HBox(8);
//        dotBox.setAlignment(Pos.CENTER);
        dotBox.setAlignment(Pos.BOTTOM_CENTER);
        dotBox.setPadding(new Insets(0, 0, 22, 0));
        for (int i = 0; i < slides.size(); i++) {
            Circle c = new Circle(4.5, Color.rgb(255, 255, 255, 0.35));
            c.setStyle("-fx-cursor: hand;");
            final int target = i;
            c.setOnMouseClicked(e -> goTo(target, true));
            dots.add(c);
            dotBox.getChildren().add(c);
        }
        dotBox.setPickOnBounds(false);
        StackPane.setAlignment(dotBox, Pos.BOTTOM_CENTER);

        getChildren().addAll(layer, shade, caption, dotBox);

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(widthProperty());
        clip.heightProperty().bind(heightProperty());
        setClip(clip);

        // run only while visible on screen (stops when the user logs in and this view is replaced)
        sceneProperty().addListener((o, was, scene) -> {
            if (scene == null) {
                timer.stop();
            } else if (!slides.isEmpty()) {
                if (current < 0) {
                    goTo(0, false);
                }
                if (slides.size() > 1) {
                    timer.playFromStart();
                }
            }
        });

        timer.getKeyFrames().add(new KeyFrame(INTERVAL, e -> goTo((current + 1) % slides.size(), false)));
        timer.setCycleCount(Animation.INDEFINITE);
    }

    // ---- slides ----------------------------------------------------------

    private void loadSlides() {
        List<Image> images = new ArrayList<>();

        Path userDir = AppPaths.appDir().resolve("slides");
        if (Files.isDirectory(userDir)) {
            try (Stream<Path> files = Files.list(userDir)) {
                files.filter(SlideshowPane::isImage).sorted().forEach(p -> addImage(images, p.toUri().toString()));
            } catch (IOException ignored) {
                // fall through to bundled pictures
            }
        }
        if (images.isEmpty()) {
            for (int i = 1; i <= 12; i++) {
                for (String ext : new String[]{"jpg", "jpeg", "png"}) {
                    var url = SlideshowPane.class.getResource("/images/slide" + i + "." + ext);
                    if (url != null) {
                        addImage(images, url.toExternalForm());
                        break;
                    }
                }
            }
        }

        for (Image img : images) {
            slides.add(new ImageView(img));
        }
        if (slides.isEmpty()) {
            addPlaceholders();
        }
        for (Node n : slides) {
            n.setOpacity(0);
            n.setMouseTransparent(true);
            layer.getChildren().add(n);
        }
    }

    private static boolean isImage(Path p) {
        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
        return Files.isRegularFile(p) && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png"));
    }

    private static void addImage(List<Image> out, String url) {
        // synchronous load, downscaled so big photos don't waste memory
        Image img = new Image(url, MAX_IMAGE_WIDTH, 0, true, true, false);
        if (!img.isError() && img.getWidth() > 0) {
            out.add(img);
        }
    }

    private void addPlaceholders() {
        String[][] palettes = {
                {"#1a2a3a", "#2a1a2a"}, {"#1a3a2a", "#1a2a3a"}, {"#2a1a2a", "#3a2a1a"},
                {"#12303a", "#1a1a3a"}, {"#2f1f12", "#1a2a3a"}};
        for (String[] p : palettes) {
            Region r = new Region();
            r.setBackground(new Background(new BackgroundFill(
                    new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web(p[0])), new Stop(1, Color.web(p[1]))), null, null)));
            slides.add(r);
        }
    }

    // ---- animation -------------------------------------------------------

    private void goTo(int index, boolean userClick) {
        if (slides.isEmpty() || index == current) {
            return;
        }
        Node next = slides.get(index);
        Node prev = current >= 0 ? slides.get(current) : null;
        current = index;

        next.toFront();
        next.setOpacity(0);
        next.setScaleX(1);
        next.setScaleY(1);
        next.setTranslateX(0);
        next.setTranslateY(0);

        FadeTransition in = new FadeTransition(FADE, next);
        in.setToValue(1);

        ScaleTransition zoom = new ScaleTransition(DRIFT, next);
        zoom.setFromX(1);
        zoom.setFromY(1);
        zoom.setToX(ZOOM);
        zoom.setToY(ZOOM);
        TranslateTransition pan = new TranslateTransition(DRIFT, next);
        pan.setFromX(-14);
        pan.setToX(14);
        pan.setFromY(6);
        pan.setToY(-6);
        ParallelTransition drift = new ParallelTransition(zoom, pan);
        drift.setInterpolator(Interpolator.LINEAR);

        if (prev != null) {
            FadeTransition out = new FadeTransition(FADE, prev);
            out.setToValue(0);
            out.play();
        }
        in.play();
        drift.play();

        for (int i = 0; i < dots.size(); i++) {
            dots.get(i).setFill(i == index ? ACCENT : Color.rgb(255, 255, 255, 0.35));
        }
        if (userClick && slides.size() > 1) {
            timer.playFromStart(); // restart the 5 s countdown after a manual jump
        }
    }
}
