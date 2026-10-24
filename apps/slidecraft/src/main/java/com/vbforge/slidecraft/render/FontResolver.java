package com.vbforge.slidecraft.render;

import javafx.scene.text.Font;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps a theme's font family to a family that is actually installed. JavaFX silently falls back to
 * the default UI font for unknown names, which made "Monospaced" themes (Neon Cyberpunk) render in a
 * proportional font on platforms without a logical "Monospaced" family (notably Windows).
 */
final class FontResolver {

    private static final List<String> MONO = List.of(
            "Cascadia Mono", "Consolas", "Menlo", "SF Mono", "DejaVu Sans Mono", "Liberation Mono",
            "Ubuntu Mono", "Courier New", "Monospaced", "Courier");
    private static final List<String> SERIF = List.of(
            "Georgia", "Cambria", "Times New Roman", "DejaVu Serif", "Liberation Serif", "Times", "Serif");

    private static Map<String, String> installed; // lower-case -> real name

    private FontResolver() {
    }

    /** Must be called on the JavaFX Application Thread (after the toolkit has started). */
    static synchronized String resolve(String family) {
        if (family == null || family.isBlank() || "System".equalsIgnoreCase(family)) {
            return "System";
        }
        Map<String, String> fonts = installedFamilies();
        String exact = fonts.get(family.toLowerCase(Locale.ROOT));
        if (exact != null) {
            return exact;
        }
        List<String> candidates = switch (family.toLowerCase(Locale.ROOT)) {
            case "monospaced", "monospace" -> MONO;
            case "georgia", "serif" -> SERIF;
            default -> List.of();
        };
        for (String candidate : candidates) {
            String found = fonts.get(candidate.toLowerCase(Locale.ROOT));
            if (found != null) {
                return found;
            }
        }
        return "System";
    }

    private static Map<String, String> installedFamilies() {
        if (installed == null) {
            installed = new HashMap<>();
            for (String name : Font.getFamilies()) {
                installed.put(name.toLowerCase(Locale.ROOT), name);
            }
        }
        return installed;
    }
}
