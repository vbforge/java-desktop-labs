package com.vbforge.pixelforge;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.function.Consumer;

/**
 * Displays the most recently completed render and translates mouse input
 * into new Viewport requests.
 *
 * This class never touches the renderer. It just emits "here's a new
 * viewport, go render it" and draws whatever image it's given back.
 */
public final class FractalView extends Canvas {

    private final Consumer<RenderRequest> onRequest;

    private Image currentImage;
    private Viewport viewport;

    private double dragAnchorX, dragAnchorY;
    private Viewport dragStartViewport;

    /** A viewport plus whether it's a low-quality preview (rendering during a drag). */
    public record RenderRequest(Viewport viewport, boolean preview) {}

    public FractalView(int width, int height, Consumer<RenderRequest> onRequest) {
        this.onRequest = onRequest;
        this.viewport = Viewport.initial(width, height);

        setWidth(width);
        setHeight(height);

        installMouseHandlers();
    }

    public Viewport viewport() {
        return viewport;
    }

    /** Replace the current viewport outright (used by Reset). */
    public void setViewport(Viewport vp) {
        this.viewport = vp;
        onRequest.accept(new RenderRequest(vp, false));
    }

    /**
     * Draw the given image scaled to fill the canvas.
     *
     * The image may be smaller than the canvas (preview renders are half
     * resolution), in which case drawImage stretches it. The stretch is
     * intentional — cheap smoothing for a preview.
     */
    public void showImage(Image img) {
        this.currentImage = img;
        redraw();
    }

    private void redraw() {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
        if (currentImage != null) {
            g.drawImage(currentImage, 0, 0, getWidth(), getHeight());
        }
    }

    private void installMouseHandlers() {
        setOnMousePressed(e -> {
            dragAnchorX = e.getX();
            dragAnchorY = e.getY();
            dragStartViewport = viewport;
        });

        setOnMouseDragged(e -> {
            double dx = e.getX() - dragAnchorX;
            double dy = e.getY() - dragAnchorY;
            viewport = dragStartViewport.pannedBy(dx, dy);
            // Preview render — half resolution, fast.
            onRequest.accept(new RenderRequest(viewport, true));
        });

        setOnMouseReleased(e -> {
            // Full-quality render at the final position.
            onRequest.accept(new RenderRequest(viewport, false));
        });

        // Scroll to zoom. deltaY is positive on scroll-down (zoom out).
        setOnScroll(e -> {
            double factor = e.getDeltaY() > 0 ? 1.15 : 1.0 / 1.15;
            viewport = viewport.zoomedAt(e.getX(), e.getY(), factor);
            onRequest.accept(new RenderRequest(viewport, false));
        });

        // Right-click resets to the default view.
        setOnContextMenuRequested(e -> {
            e.consume();
            setViewport(Viewport.initial((int) getWidth(), (int) getHeight()));
        });
    }
}