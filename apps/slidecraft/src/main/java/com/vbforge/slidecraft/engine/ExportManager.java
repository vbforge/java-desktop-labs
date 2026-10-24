package com.vbforge.slidecraft.engine;

import com.vbforge.slidecraft.engine.exporters.MarkdownExporter;
import com.vbforge.slidecraft.engine.exporters.PdfExporter;
import com.vbforge.slidecraft.engine.exporters.PngExporter;
import com.vbforge.slidecraft.engine.exporters.TxtExporter;
import com.vbforge.slidecraft.model.Presentation;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Dispatches exports to the format-specific exporters. Pure file output: rendering slides to images
 * needs the JavaFX toolkit and lives in {@code render.SlideSnapshot}, so everything here is safe to
 * call from a background thread.
 */
public final class ExportManager {

    private ExportManager() {
    }

    public static void exportTxt(Presentation p, Path target) throws IOException {
        writeText(TxtExporter.export(p), target);
    }

    public static void exportMarkdown(Presentation p, Path target) throws IOException {
        writeText(MarkdownExporter.export(p), target);
    }

    public static void exportJson(Presentation p, Path target) throws IOException {
        FileManager.write(p, target);
    }

    public static void exportPng(BufferedImage image, Path target) throws IOException {
        PngExporter.write(image, target);
    }

    /** Writes {@code slide-01.png}, {@code slide-02.png}, ... into {@code directory}. */
    public static List<Path> exportPngSet(List<BufferedImage> images, Path directory) throws IOException {
        List<Path> written = new ArrayList<>(images.size());
        for (int i = 0; i < images.size(); i++) {
            Path target = directory.resolve(pngFileName(i));
            PngExporter.write(images.get(i), target);
            written.add(target);
        }
        return written;
    }

    public static void exportPdf(List<BufferedImage> images, String title, Path target) throws IOException {
        PdfExporter.export(images, title, target);
    }

    /** File name for the slide at the given zero-based index: {@code slide-01.png}. */
    public static String pngFileName(int index) {
        return String.format("slide-%02d.png", index + 1);
    }

    public static List<Path> pngTargets(Path directory, int count) {
        List<Path> targets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            targets.add(directory.resolve(pngFileName(i)));
        }
        return targets;
    }

    private static void writeText(String text, Path target) throws IOException {
        // AtomicFiles lives in this package; reuse it for text output.
        AtomicFiles.write(target, text.getBytes(StandardCharsets.UTF_8));
    }
}
