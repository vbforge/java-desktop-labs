package com.vbforge.animalpedia.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.text.Font;

import java.util.List;
import java.util.Locale;

/** Colours and small visual building blocks shared by the screens. */
public final class Theme {

    public static final String INK = "#2B2D42";
    public static final String ORANGE = "#FF8A3D";

    private static final List<String> EMOJI_FONTS = List.of(
            "Apple Color Emoji", "Segoe UI Emoji", "Noto Color Emoji", "Twemoji Mozilla", "EmojiOne Color");

    private static boolean probed;
    private static String emojiFont;

    private Theme() {}

    /** Colour for an animal group (Mammals, Birds, ...). Anything else, such as web results, is purple. */
    public static String groupColor(String group) {
        return switch (group == null ? "" : group) {
            case "Mammals" -> "#F59E0B";
            case "Birds" -> "#3B82F6";
            case "Reptiles" -> "#10B981";
            case "Amphibians" -> "#06B6D4";
            case "Insects" -> "#EC4899";
            default -> "#8B5CF6";
        };
    }

    /** The first installed emoji font, or null when this computer has none. Call after JavaFX has started. */
    public static synchronized String emojiFont() {
        if (!probed) {
            probed = true;
            List<String> families = Font.getFamilies();
            for (String f : EMOJI_FONTS) {
                if (families.contains(f)) {
                    emojiFont = f;
                    break;
                }
            }
        }
        return emojiFont;
    }

    public static boolean emojiSupported() {
        return emojiFont() != null;
    }

    /** The emoji when this computer can draw it, otherwise the first letter of the fallback text. */
    public static String glyph(String emoji, String fallbackText) {
        if (emojiSupported()) return emoji;
        String t = fallbackText == null ? "" : fallbackText.trim();
        return t.isEmpty() ? "?" : t.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    /** A round, softly tinted badge holding an emoji (or a letter). */
    public static Label badge(String emoji, String fallbackText, String colorHex, double size) {
        Label l = new Label(glyph(emoji, fallbackText));
        l.setAlignment(Pos.CENTER);
        l.setMinSize(size, size);
        l.setPrefSize(size, size);
        l.setMaxSize(size, size);
        String font = emojiSupported() ? "-fx-font-family: '" + emojiFont() + "';" : "";
        l.setStyle("-fx-background-color: " + colorHex + "2E;"
                + "-fx-background-radius: " + size + ";"
                + "-fx-text-fill: " + colorHex + ";"
                + "-fx-font-size: " + (int) (size * 0.5) + "px;"
                + "-fx-font-weight: bold;" + font);
        return l;
    }

    /** A small rounded label such as "Mammals" or "Endangered". */
    public static Label pill(String text, String colorHex) {
        Label l = new Label(text);
        l.setStyle("-fx-background-color: " + colorHex + ";-fx-background-radius: 999;"
                + "-fx-text-fill: white;-fx-font-size: 12px;-fx-font-weight: bold;-fx-padding: 4 12 4 12;");
        return l;
    }
}
