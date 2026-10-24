package com.vbforge.slidecraft.engine;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Sign;
import com.vbforge.slidecraft.model.Slide;
import com.vbforge.slidecraft.model.Theme;
import com.vbforge.slidecraft.render.SlideSnapshot;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportManagerTest {

    private static Presentation deck(boolean sign) {
        return new Presentation("Quarterly Review", Theme.VIBRANT_BLUE,
                new Sign(sign, "© 2026 Acme"),
                List.of(new Slide("Intro", "Welcome everyone", "Smile\nbreathe"),
                        new Slide("Numbers", "Up and to the right", "")));
    }

    @Test
    void jsonExportParsesBackToTheSamePresentation(@TempDir Path dir) throws IOException {
        Presentation p = deck(true);
        Path file = dir.resolve("deck.slidecraft");
        ExportManager.exportJson(p, file);
        assertEquals(p, ImportManager.fromFile(file));
    }

    @Test
    void txtContainsSlidesAndSignWhenEnabled(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("deck.txt");
        ExportManager.exportTxt(deck(true), file);
        String txt = Files.readString(file);
        assertTrue(txt.contains("PRESENTATION: Quarterly Review"));
        assertTrue(txt.contains("--- Slide 1 ---"));
        assertTrue(txt.contains("Intro") && txt.contains("Welcome everyone"));
        assertTrue(txt.contains("Speaker Notes: Smile"));
        assertTrue(txt.contains("Footer: © 2026 Acme"));
    }

    @Test
    void txtOmitsSignWhenDisabled(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("deck.txt");
        ExportManager.exportTxt(deck(false), file);
        assertFalse(Files.readString(file).contains("Footer:"));
    }

    @Test
    void markdownHasHeadingsNotesSignAndSeparators(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("deck.md");
        ExportManager.exportMarkdown(deck(true), file);
        String md = Files.readString(file);
        assertTrue(md.startsWith("# Quarterly Review"));
        assertTrue(md.contains("## Slide 1: Intro"));
        assertTrue(md.contains("## Slide 2: Numbers"));
        assertTrue(md.contains("> **Speaker Notes:** Smile\n> breathe"));
        assertTrue(md.contains("*© 2026 Acme*"));
        assertTrue(md.trim().endsWith("---"));
    }

    @Test
    void markdownOmitsSignWhenDisabled(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("deck.md");
        ExportManager.exportMarkdown(deck(false), file);
        assertFalse(Files.readString(file).contains("2026 Acme"));
    }

    @Test
    void pdfHasOnePagePerSlide(@TempDir Path dir) throws IOException {
        List<BufferedImage> images = List.of(solid(Color.BLUE), solid(Color.RED), solid(Color.GREEN));
        Path file = dir.resolve("deck.pdf");
        ExportManager.exportPdf(images, "Deck", file);
        try (PDDocument doc = Loader.loadPDF(file.toFile())) {
            assertEquals(3, doc.getNumberOfPages());
            assertEquals("Deck", doc.getDocumentInformation().getTitle());
        }
    }

    @Test
    void pngSetUsesTwoDigitNumbering(@TempDir Path dir) throws IOException {
        List<Path> written = ExportManager.exportPngSet(List.of(solid(Color.BLUE), solid(Color.RED)), dir);
        assertEquals(List.of(dir.resolve("slide-01.png"), dir.resolve("slide-02.png")), written);
        assertTrue(Files.size(written.get(0)) > 0);
    }

    @Test
    void renderedSlidesAre1920x1080AndUseTheThemeBackground(@TempDir Path dir) throws Exception {
        Assumptions.assumeTrue(FxTestSupport.available(), "JavaFX toolkit not available (use -Pheadless-fx)");
        Presentation p = deck(true);

        List<BufferedImage> images = FxTestSupport.onFxThread(() -> SlideSnapshot.renderAll(p));
        assertEquals(2, images.size());
        for (BufferedImage image : images) {
            assertEquals(1920, image.getWidth());
            assertEquals(1080, image.getHeight());
            assertEquals(0x095be0, image.getRGB(4, 4) & 0xFFFFFF);
        }

        Path png = dir.resolve("slide.png");
        ExportManager.exportPng(images.get(0), png);
        BufferedImage read = ImageIO.read(png.toFile());
        assertEquals(1920, read.getWidth());
        assertEquals(1080, read.getHeight());
    }

    private static BufferedImage solid(Color color) {
        BufferedImage image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 1920, 1080);
        g.dispose();
        return image;
    }
}
