package com.vbforge.slidecraft.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Theme;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Reads and writes the native {@code .slidecraft} JSON format. */
public final class FileManager {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private FileManager() {
    }

    public static String toJson(Presentation presentation) {
        try {
            return MAPPER.writeValueAsString(presentation) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize presentation", e);
        }
    }

    public static String toJson(Theme theme) {
        try {
            return MAPPER.writeValueAsString(theme) + "\n";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize theme", e);
        }
    }

    public static Presentation fromJson(String json) throws PresentationFormatException {
        return ImportManager.fromJson(json);
    }

    public static Presentation read(Path file) throws IOException {
        return ImportManager.fromFile(file);
    }

    /** Atomically writes the presentation as UTF-8 JSON. */
    public static void write(Presentation presentation, Path file) throws IOException {
        AtomicFiles.write(file, toJson(presentation).getBytes(StandardCharsets.UTF_8));
    }

    static void write(Theme theme, Path file) throws IOException {
        AtomicFiles.write(file, toJson(theme).getBytes(StandardCharsets.UTF_8));
    }
}
