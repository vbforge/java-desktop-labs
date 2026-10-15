package com.vbforge.pixelforge;

import javafx.concurrent.Task;
import javafx.scene.image.WritableImage;

/**
 * Runs FractalRenderer.render off the JavaFX Application Thread.
 *
 * Task does three things for us that would otherwise be error-prone:
 *   1. Runs call() on a background thread.
 *   2. Fires onSucceeded / onCancelled / onFailed on the FX thread,
 *      so it is safe to touch the scene graph from them.
 *   3. Provides cancel(), which sets the cancelled flag AND interrupts
 *      the running thread — so isCancelled() checks inside call() are
 *      the only cooperation we need.
 *
 * The single-thread executor in PixelForgeApp guarantees renders run
 * one at a time. Cancelling the current task lets the next one start
 * almost immediately rather than waiting for a stale full render.
 */
public final class RenderTask extends Task<WritableImage> {

    private final Viewport viewport;
    private final int maxIter;

    public RenderTask(Viewport viewport, int maxIter) {
        this.viewport = viewport;
        this.maxIter = maxIter;
    }

    @Override
    protected WritableImage call() {
        WritableImage img = FractalRenderer.render(viewport, maxIter, this::isCancelled);
        // render() returns null if we bailed. Returning null from a
        // cancelled task is fine — onSucceeded won't fire for a cancelled
        // task anyway, so the null never reaches the UI.
        return img;
    }
}