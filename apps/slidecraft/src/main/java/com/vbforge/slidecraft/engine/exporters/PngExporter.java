package com.vbforge.slidecraft.engine.exporters;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/** Writes already-rendered slide images (see {@code SlideSnapshot}) to PNG files. */
public final class PngExporter {

    private PngExporter() {
    }

    public static void write(BufferedImage image, Path target) throws IOException {
        AtomicWrite.write(target, out -> {
            if (!ImageIO.write(image, "png", out)) {
                throw new IOException("No PNG writer available");
            }
        });
    }
}
