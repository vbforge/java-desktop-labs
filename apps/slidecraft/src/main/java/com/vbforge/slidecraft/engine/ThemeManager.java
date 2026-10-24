package com.vbforge.slidecraft.engine;

import com.vbforge.slidecraft.model.Theme;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Provides built-in themes and persists user-made themes in {@code ~/.slidecraft/themes/}. */
public final class ThemeManager {

    private static final System.Logger LOG = System.getLogger(ThemeManager.class.getName());
    private static final List<Theme> BUILT_IN = List.of(
            Theme.VIBRANT_BLUE, Theme.DARK_MINIMAL, Theme.CLEAN_LIGHT, Theme.NEON_CYBERPUNK);

    private final Path directory;

    public ThemeManager() {
        this(Path.of(System.getProperty("user.home"), ".slidecraft", "themes"));
    }

    public ThemeManager(Path directory) {
        this.directory = directory;
    }

    public static List<Theme> builtIns() {
        return BUILT_IN;
    }

    public static Theme defaultTheme() {
        return Theme.DEFAULT;
    }

    public static boolean isBuiltInName(String name) {
        return name != null && BUILT_IN.stream().anyMatch(t -> t.name().equalsIgnoreCase(name.trim()));
    }

    public Path directory() {
        return directory;
    }

    /** Loads all custom themes; unreadable or invalid files are skipped (and logged). */
    public List<Theme> loadCustom() {
        List<Theme> result = new ArrayList<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(directory, "*.json")) {
            List<Path> sorted = new ArrayList<>();
            files.forEach(sorted::add);
            sorted.sort(null);
            for (Path file : sorted) {
                try {
                    Theme theme = ImportManager.themeFromBytes(Files.readAllBytes(file), "Custom");
                    if (!isBuiltInName(theme.name()) && result.stream().noneMatch(t -> sameName(t, theme))) {
                        result.add(theme);
                    }
                } catch (IOException e) {
                    LOG.log(System.Logger.Level.WARNING, "Skipping invalid theme file " + file + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LOG.log(System.Logger.Level.WARNING, "Could not list themes in " + directory + ": " + e.getMessage());
        }
        return result;
    }

    /**
     * Saves a custom theme (overwriting a custom theme of the same name).
     *
     * @throws IllegalArgumentException if the name collides with a built-in theme
     */
    public Path saveCustom(Theme theme) throws IOException {
        if (isBuiltInName(theme.name())) {
            throw new IllegalArgumentException("\"" + theme.name() + "\" is a built-in theme name; choose another name");
        }
        Path file = directory.resolve(slug(theme.name()) + ".json");
        FileManager.write(theme, file);
        return file;
    }

    /** Built-in themes followed by custom themes. */
    public List<Theme> allThemes() {
        List<Theme> all = new ArrayList<>(BUILT_IN);
        all.addAll(loadCustom());
        return all;
    }

    static String slug(String name) {
        String s = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "theme" : s;
    }

    private static boolean sameName(Theme a, Theme b) {
        return a.name().equalsIgnoreCase(b.name());
    }
}
