package com.vbforge.pixelforge;

/**
 * Immutable description of the visible region of the complex plane.
 *
 * scale = complex-units-per-pixel. Smaller scale = more zoomed in.
 * Screen coordinates are top-left origin; complex coordinates are
 * centred on (centerX, centerY).
 */
public record Viewport(double centerX, double centerY, double scale,
                       int width, int height) {

    /** Default view: the full Mandelbrot set with a little margin. */
    public static Viewport initial(int width, int height) {
        double scale = 3.5 / width;
        return new Viewport(-0.5, 0.0, scale, width, height);
    }

    /** Drag by (dxPixels, dyPixels). Positive dx moves the view left. */
    public Viewport pannedBy(double dxPixels, double dyPixels) {
        return new Viewport(
                centerX - dxPixels * scale,
                centerY - dyPixels * scale,
                scale, width, height);
    }

    /**
     * Zoom in (factor &lt; 1) or out (factor &gt; 1) about a screen point,
     * so the complex number under the cursor stays under the cursor.
     */
    public Viewport zoomedAt(double screenX, double screenY, double factor) {
        double fx = centerX + (screenX - width  / 2.0) * scale;
        double fy = centerY + (screenY - height / 2.0) * scale;

        double newScale = scale * factor;
        double newCx = fx - (screenX - width  / 2.0) * newScale;
        double newCy = fy - (screenY - height / 2.0) * newScale;

        return new Viewport(newCx, newCy, newScale, width, height);
    }
}