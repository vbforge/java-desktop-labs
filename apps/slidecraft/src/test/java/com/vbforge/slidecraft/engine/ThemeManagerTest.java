package com.vbforge.slidecraft.engine;

import com.vbforge.slidecraft.model.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeManagerTest {

    private static final Theme OCEAN = new Theme("Ocean Night", "#001133", "#cce6ff", "Arial", "LEFT");

    @Test
    void builtInsAreProvidedInDocumentedOrder() {
        assertEquals(List.of("Vibrant Blue", "Dark Minimal", "Clean Light", "Neon Cyberpunk"),
                ThemeManager.builtIns().stream().map(Theme::name).toList());
        assertEquals("Dark Minimal", ThemeManager.defaultTheme().name());
    }

    @Test
    void customThemeSurvivesRestart(@TempDir Path dir) throws IOException {
        Path themes = dir.resolve("themes"); // does not exist yet: must be created
        new ThemeManager(themes).saveCustom(OCEAN);
        assertEquals(List.of(OCEAN), new ThemeManager(themes).loadCustom());
        assertEquals(5, new ThemeManager(themes).allThemes().size());
    }

    @Test
    void missingDirectoryMeansNoCustomThemes(@TempDir Path dir) {
        assertTrue(new ThemeManager(dir.resolve("nope")).loadCustom().isEmpty());
    }

    @Test
    void builtInNamesAreReserved(@TempDir Path dir) {
        ThemeManager tm = new ThemeManager(dir);
        assertThrows(IllegalArgumentException.class,
                () -> tm.saveCustom(new Theme("vibrant blue", "#000", "#fff", "System", "LEFT")));
    }

    @Test
    void savingSameNameOverwrites(@TempDir Path dir) throws IOException {
        ThemeManager tm = new ThemeManager(dir);
        tm.saveCustom(OCEAN);
        Theme changed = new Theme("Ocean Night", "#000000", "#ffffff", "Arial", "CENTER");
        tm.saveCustom(changed);
        assertEquals(List.of(changed), tm.loadCustom());
    }

    @Test
    void corruptOrIncompleteThemeFilesAreSkipped(@TempDir Path dir) throws IOException {
        ThemeManager tm = new ThemeManager(dir);
        tm.saveCustom(OCEAN);
        Files.writeString(dir.resolve("broken.json"), "{oops");
        Files.writeString(dir.resolve("incomplete.json"), "{\"name\":\"X\"}");
        assertEquals(List.of(OCEAN), tm.loadCustom());
    }

    @Test
    void slugIsFilesystemSafe() {
        assertEquals("ocean-night", ThemeManager.slug("Ocean Night"));
        assertEquals("theme", ThemeManager.slug("???"));
        assertEquals("a-b", ThemeManager.slug("../a/b"));
    }
}
