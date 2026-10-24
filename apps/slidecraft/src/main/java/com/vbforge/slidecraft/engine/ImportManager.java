package com.vbforge.slidecraft.engine;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Sign;
import com.vbforge.slidecraft.model.Slide;
import com.vbforge.slidecraft.model.Theme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lenient-but-safe parser for {@code .slidecraft} / {@code .json} files.
 *
 * <p>Rules (see requirements §4.G / §5): the root must be an object, {@code slides} a non-empty
 * array of objects; slide fields are coerced to strings; {@code theme}, {@code sign} and
 * {@code title} are optional with defaults; unknown fields are ignored. Nothing is returned unless
 * the whole file is valid (never a partial load).
 */
public final class ImportManager {

    public static final long MAX_FILE_BYTES = 20L * 1024 * 1024;
    public static final String IMPORTED_THEME_NAME = "Imported Theme";

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private ImportManager() {
    }

    public static Presentation fromFile(Path file) throws IOException {
        long size = Files.size(file);
        if (size > MAX_FILE_BYTES) {
            throw new PresentationFormatException(
                    "File is too large (limit " + (MAX_FILE_BYTES / 1024 / 1024) + " MB)");
        }
        return fromBytes(Files.readAllBytes(file));
    }

    public static Presentation fromBytes(byte[] data) throws PresentationFormatException {
        if (data == null || data.length == 0) {
            throw new PresentationFormatException("File is empty");
        }
        try {
            return fromTree(MAPPER.readTree(data));
        } catch (JsonProcessingException e) {
            throw malformed(e);
        } catch (IOException e) {
            throw new PresentationFormatException("Could not read JSON: " + e.getMessage(), e);
        }
    }

    public static Presentation fromJson(String json) throws PresentationFormatException {
        if (json == null || json.isBlank()) {
            throw new PresentationFormatException("File is empty");
        }
        try {
            return fromTree(MAPPER.readTree(json));
        } catch (JsonProcessingException e) {
            throw malformed(e);
        }
    }

    /** Parses a standalone theme file; incomplete themes are rejected. */
    static Theme themeFromBytes(byte[] data, String defaultName) throws PresentationFormatException {
        try {
            Theme theme = parseTheme(MAPPER.readTree(data), defaultName);
            if (theme == null) {
                throw new PresentationFormatException("Theme needs valid backgroundColor and textColor");
            }
            return theme;
        } catch (JsonProcessingException e) {
            throw malformed(e);
        } catch (IOException e) {
            throw new PresentationFormatException("Could not read theme: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ internals

    private static Presentation fromTree(JsonNode root) throws PresentationFormatException {
        if (root == null || root.isMissingNode()) {
            throw new PresentationFormatException("File is empty");
        }
        if (!root.isObject()) {
            throw new PresentationFormatException("Root must be a JSON object");
        }

        JsonNode slidesNode = root.get("slides");
        if (slidesNode == null || !slidesNode.isArray() || slidesNode.isEmpty()) {
            throw new PresentationFormatException("\"slides\" must be a non-empty array");
        }
        List<Slide> slides = new ArrayList<>(slidesNode.size());
        int i = 0;
        for (JsonNode s : slidesNode) {
            i++;
            if (!s.isObject()) {
                throw new PresentationFormatException("Slide " + i + " is not an object");
            }
            slides.add(new Slide(
                    slideText(s, "title", i),
                    slideText(s, "content", i),
                    slideText(s, "notes", i)));
        }

        Theme theme = parseTheme(root.get("theme"), IMPORTED_THEME_NAME);
        if (theme == null) {
            theme = Theme.DEFAULT;
        }

        Sign sign = Sign.NONE;
        JsonNode signNode = root.get("sign");
        if (signNode != null && signNode.isObject()) {
            String text = textOrNull(signNode.get("text"));
            sign = new Sign(signNode.path("enabled").asBoolean(false), text == null ? "" : text);
        }

        String title = textOrNull(root.get("title"));
        return new Presentation(title, theme, sign, slides);
    }

    static Theme parseTheme(JsonNode node, String defaultName) {
        if (node == null || !node.isObject()) {
            return null;
        }
        String bg = textOrNull(node.get("backgroundColor"));
        String fg = textOrNull(node.get("textColor"));
        if (!Theme.isValidColor(bg) || !Theme.isValidColor(fg)) {
            return null; // missing or incomplete -> caller falls back to the default theme
        }
        String name = textOrNull(node.get("name"));
        if (name == null || name.isBlank()) {
            name = defaultName;
        }
        return new Theme(
                name,
                bg,
                fg,
                textOrNull(node.get("fontFamily")),
                textOrNull(node.get("textAlignment")));
    }

    private static String slideText(JsonNode slide, String field, int number) throws PresentationFormatException {
        JsonNode n = slide.get(field);
        if (n == null || n.isNull()) {
            return "";
        }
        if (n.isValueNode()) {
            return n.asText();
        }
        throw new PresentationFormatException("Slide " + number + ": \"" + field + "\" must be text");
    }

    private static String textOrNull(JsonNode n) {
        return (n == null || n.isNull() || !n.isValueNode()) ? null : n.asText();
    }

    private static PresentationFormatException malformed(JsonProcessingException e) {
        JsonLocation loc = e.getLocation();
        String where = (loc != null && loc.getLineNr() > 0)
                ? " (line " + loc.getLineNr() + ", column " + loc.getColumnNr() + ")"
                : "";
        return new PresentationFormatException("Malformed JSON: " + e.getOriginalMessage() + where, e);
    }
}
