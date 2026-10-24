package com.vbforge.slidecraft.render;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Slide;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * Fits a {@link SlideView} (fixed 1920x1080) into whatever space it is given, preserving the 16:9
 * ratio and centring it. Used for the editor preview and the fullscreen presentation.
 *
 * <p>In the editor an outline is drawn around the slide so very dark or very light themes (e.g. a
 * black slide on the dark editor background) still have a visible edge.
 */
public final class SlideCanvas extends Region {

    private final SlideView view = new SlideView();
    private final Rectangle outline = new Rectangle();

    public SlideCanvas() {
        view.setManaged(false);
        outline.setManaged(false);
        outline.setMouseTransparent(true);
        outline.setFill(Color.TRANSPARENT);
        outline.setStroke(Color.web("#3d4653"));
        outline.setStrokeWidth(1);
        setMinSize(0, 0);
        getChildren().addAll(view, outline);
    }

    /** Outline is on by default (editor preview); turn it off for fullscreen. */
    public void setOutlineVisible(boolean visible) {
        outline.setVisible(visible);
    }

    public void show(Presentation presentation, Slide slide) {
        view.update(presentation, slide);
    }

    @Override
    protected void layoutChildren() {
        double scale = Math.min(getWidth() / SlideView.WIDTH, getHeight() / SlideView.HEIGHT);
        if (scale <= 0) {
            return;
        }
        view.resize(SlideView.WIDTH, SlideView.HEIGHT);
        view.setScaleX(scale);
        view.setScaleY(scale);
        // Scaling pivots on the node centre, so centring the unscaled box centres the scaled slide.
        view.relocate((getWidth() - SlideView.WIDTH) / 2, (getHeight() - SlideView.HEIGHT) / 2);

        double w = SlideView.WIDTH * scale;
        double h = SlideView.HEIGHT * scale;
        outline.setX(Math.floor((getWidth() - w) / 2) + 0.5);
        outline.setY(Math.floor((getHeight() - h) / 2) + 0.5);
        outline.setWidth(w - 1);
        outline.setHeight(h - 1);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 480;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 270;
    }
}
