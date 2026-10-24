package com.vbforge.slidecraft.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Visual theme applied to every slide of a presentation.
 *
 * <p>All values are normalised in the compact constructor, so a {@code Theme} is always safe to
 * embed in inline JavaFX CSS: colours are strict {@code #rrggbb} hex, the font family is a single
 * plain family name, and the alignment is either {@code LEFT} or {@code CENTER}.
 */
@JsonPropertyOrder({"name", "backgroundColor", "textColor", "fontFamily", "textAlignment"})
public record Theme(
        String name,
        String backgroundColor,
        String textColor,
        String fontFamily,
        String textAlignment
) {
    public static final String LEFT = "LEFT";
    public static final String CENTER = "CENTER";

    private static final String FALLBACK_BACKGROUND = "#1a1a1a";
    private static final String FALLBACK_TEXT = "#e6edf3";
    private static final String FALLBACK_NAME = "Custom";
    private static final String FALLBACK_FONT = "System";
    private static final int MAX_NAME_LENGTH = 60;

    // NOTE: these must be declared before the built-in constants below (static init order).
    private static final Pattern HEX = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final Pattern FONT = Pattern.compile("^[\\p{L}\\p{N} ._-]{1,64}$");

    public static final Theme VIBRANT_BLUE = new Theme("Vibrant Blue", "#095be0", "#ffffff", "System", CENTER);
    public static final Theme DARK_MINIMAL = new Theme("Dark Minimal", "#1a1a1a", "#e6edf3", "Georgia", LEFT);
    public static final Theme CLEAN_LIGHT = new Theme("Clean Light", "#f5f5f5", "#222222", "System", CENTER);
    public static final Theme NEON_CYBERPUNK = new Theme("Neon Cyberpunk", "#000000", "#00e5ff", "Monospaced", LEFT);

    /** Theme used for new presentations and as the import fallback. */
    public static final Theme DEFAULT = DARK_MINIMAL;

    public Theme {
        name = cleanName(name);
        backgroundColor = orElse(normalizeColor(backgroundColor), FALLBACK_BACKGROUND);
        textColor = orElse(normalizeColor(textColor), FALLBACK_TEXT);
        fontFamily = normalizeFont(fontFamily);
        textAlignment = (textAlignment != null && LEFT.equalsIgnoreCase(textAlignment.trim())) ? LEFT : CENTER;
    }

    public boolean alignsLeft() {
        return LEFT.equals(textAlignment);
    }

    /** True if the value is a {@code #rgb} or {@code #rrggbb} colour. */
    public static boolean isValidColor(String value) {
        return normalizeColor(value) != null;
    }

    private static String normalizeColor(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        if (!HEX.matcher(t).matches()) {
            return null;
        }
        t = t.toLowerCase(Locale.ROOT);
        if (t.length() == 4) {
            StringBuilder sb = new StringBuilder("#");
            for (int i = 1; i < 4; i++) {
                sb.append(t.charAt(i)).append(t.charAt(i));
            }
            return sb.toString();
        }
        return t;
    }

    private static String normalizeFont(String value) {
        if (value == null || value.isBlank()) {
            return FALLBACK_FONT;
        }
        // Accept CSS-style stacks ("Georgia, serif"): keep the first family, drop quotes.
        String first = value.split(",", 2)[0].trim().replaceAll("^['\"]+|['\"]+$", "").trim();
        switch (first.toLowerCase(Locale.ROOT)) {
            case "system-ui", "-apple-system", "blinkmacsystemfont", "sans-serif", "ui-sans-serif", "system" -> {
                return FALLBACK_FONT;
            }
            case "serif", "ui-serif" -> {
                return "Georgia";
            }
            case "monospace", "ui-monospace" -> {
                return "Monospaced";
            }
            default -> {
                // fall through to validation
            }
        }
        return FONT.matcher(first).matches() ? first : FALLBACK_FONT;
    }

    private static String cleanName(String value) {
        if (value == null || value.isBlank()) {
            return FALLBACK_NAME;
        }
        String t = value.replaceAll("\\p{Cntrl}", " ").trim();
        if (t.isEmpty()) {
            return FALLBACK_NAME;
        }
        return t.length() > MAX_NAME_LENGTH ? t.substring(0, MAX_NAME_LENGTH).trim() : t;
    }

    private static String orElse(String value, String fallback) {
        return value != null ? value : fallback;
    }
}
