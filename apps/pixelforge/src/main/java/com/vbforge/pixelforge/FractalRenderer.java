package com.vbforge.pixelforge;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

import java.util.function.BooleanSupplier;

/**
 * Renders the Mandelbrot set into an ARGB pixel buffer.
 *
 * No JavaFX scene-graph objects touched — WritableImage and PixelWriter are
 * safe to construct and fill on any thread. The image only becomes "FX stuff"
 * when you draw it on the UI thread.
 */
public final class FractalRenderer {

    /** Rendered inside the set — matches the app background. */
    private static final int INTERIOR = 0xff0d1117;

    private FractalRenderer() {}

    /**
     * @param vp         what to draw
     * @param maxIter    escape-time iteration ceiling
     * @param cancelled  polled once per row; return true to abandon and return null
     */
    public static WritableImage render(Viewport vp, int maxIter, BooleanSupplier cancelled) {
        int w = vp.width();
        int h = vp.height();
        int[] pixels = new int[w * h];

        double x0 = vp.centerX() - (w / 2.0) * vp.scale();
        double y0 = vp.centerY() - (h / 2.0) * vp.scale();

        for (int py = 0; py < h; py++) {
            // Cancellation checkpoint. One per row is frequent enough for
            // responsive drag handling and cheap enough to not matter.
            if (cancelled.getAsBoolean()) return null;

            double cy = y0 + py * vp.scale();
            int rowOffset = py * w;

            for (int px = 0; px < w; px++) {
                double cx = x0 + px * vp.scale();
                pixels[rowOffset + px] = mandelbrot(cx, cy, maxIter);
            }
        }

        WritableImage img = new WritableImage(w, h);
        img.getPixelWriter().setPixels(
                0, 0, w, h,
                PixelFormat.getIntArgbInstance(),
                pixels, 0, w);
        return img;
    }

    private static int mandelbrot(double cx, double cy, int maxIter) {
        double zx = 0, zy = 0;
        int i = 0;

        while (i < maxIter && zx * zx + zy * zy < 4.0) {
            double t = zx * zx - zy * zy + cx;
            zy = 2.0 * zx * zy + cy;
            zx = t;
            i++;
        }

        if (i >= maxIter) return INTERIOR;

        // Smooth escape count — removes the harsh iteration bands.
        // Based on the standard "renormalised" trick.
        double logZn = Math.log(zx * zx + zy * zy) / 2.0;
        double nu = Math.log(logZn / Math.log(2.0)) / Math.log(2.0);
        double smooth = i + 1 - nu;

        return palette(smooth / maxIter);
    }

    /**
     * Cosine palette (Iñigo Quílez). Cheap, smooth, and gives the whole
     * hue wheel without any lookup tables.
     */
    private static int palette(double t) {
        // Gamma so the outer (low-t) region doesn't collapse to one colour.
        t = Math.pow(Math.max(t, 0.0), 0.35);

        double r = 0.5 + 0.5 * Math.cos(2 * Math.PI * (t + 0.00));
        double g = 0.5 + 0.5 * Math.cos(2 * Math.PI * (t + 0.15));
        double b = 0.5 + 0.5 * Math.cos(2 * Math.PI * (t + 0.25));

        int ri = (int) (r * 255);
        int gi = (int) (g * 255);
        int bi = (int) (b * 255);
        return 0xff000000 | (ri << 16) | (gi << 8) | bi;
    }
}