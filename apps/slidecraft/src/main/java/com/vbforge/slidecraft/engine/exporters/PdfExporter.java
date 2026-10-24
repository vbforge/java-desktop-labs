package com.vbforge.slidecraft.engine.exporters;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * PDF export: one landscape 16:9 page per slide, the rendered slide image drawn full-bleed.
 *
 * <p>Using the same rendered image as PNG export guarantees pixel parity with the editor preview
 * (theme background, fonts, sign) and avoids the white-margin problem of {@code PrinterJob}. The
 * trade-off is that PDF text is not selectable.
 */
public final class PdfExporter {

    /** 16:9 page, in PDF points. */
    static final float PAGE_WIDTH = 960f;
    static final float PAGE_HEIGHT = 540f;

    private PdfExporter() {
    }

    public static void export(List<BufferedImage> slides, String title, Path target) throws IOException {
        if (slides.isEmpty()) {
            throw new IOException("Nothing to export");
        }
        AtomicWrite.write(target, out -> {
            try (PDDocument doc = new PDDocument()) {
                PDDocumentInformation info = doc.getDocumentInformation();
                info.setTitle(title);
                info.setCreator("SlideCraft");

                PDRectangle size = new PDRectangle(PAGE_WIDTH, PAGE_HEIGHT);
                for (BufferedImage slide : slides) {
                    PDPage page = new PDPage(size);
                    doc.addPage(page);
                    PDImageXObject image = LosslessFactory.createFromImage(doc, toRgb(slide));
                    try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                        cs.drawImage(image, 0, 0, PAGE_WIDTH, PAGE_HEIGHT);
                    }
                }
                doc.save(out);
            }
        });
    }

    /** Drops the alpha channel so PDF viewers don't need soft masks. */
    private static BufferedImage toRgb(BufferedImage src) {
        BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        var g = rgb.createGraphics();
        try {
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return rgb;
    }
}
