package com.vbforge.slidecraft.engine;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Sign;
import com.vbforge.slidecraft.model.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportManagerTest {

    private static final String VALID = """
            {
              "title": "My First Presentation",
              "theme": {"name": "Vibrant Blue", "backgroundColor": "#095be0", "textColor": "#ffffff",
                        "fontFamily": "System", "textAlignment": "CENTER"},
              "sign": {"enabled": true, "text": "© 2026 Your Name"},
              "slides": [
                {"title": "Welcome", "content": "Hello", "notes": "Warm welcome"},
                {"title": "Problem", "content": "Fonts", "notes": ""}
              ]
            }
            """;

    @Test
    void readsAllFieldsOfAValidFile() throws IOException {
        Presentation p = ImportManager.fromJson(VALID);
        assertEquals("My First Presentation", p.title());
        assertEquals(Theme.VIBRANT_BLUE, p.theme());
        assertEquals(new Sign(true, "© 2026 Your Name"), p.sign());
        assertEquals(2, p.slides().size());
        assertEquals("Warm welcome", p.slides().get(0).notes());
    }

    @Test
    void malformedJsonGivesDescriptiveMessage() {
        PresentationFormatException e =
                assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("{not json"));
        assertTrue(e.getMessage().startsWith("Malformed JSON"), e.getMessage());
    }

    @Test
    void trailingGarbageIsRejected() {
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("{\"slides\":[{}]} xyz"));
    }

    @Test
    void rootMustBeAnObject() {
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("[1,2,3]"));
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("\"text\""));
    }

    @Test
    void slidesMustBeANonEmptyArrayOfObjects() {
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("{}"));
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("{\"slides\":{}}"));
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("{\"slides\":[]}"));
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("{\"slides\":[1]}"));
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromJson("{\"slides\":[{\"title\":{}}]}"));
    }

    @Test
    void scalarSlideFieldsAreCoercedToStrings() throws IOException {
        Presentation p = ImportManager.fromJson("{\"slides\":[{\"title\":5,\"content\":true,\"notes\":null}]}");
        assertEquals("5", p.slides().get(0).title());
        assertEquals("true", p.slides().get(0).content());
        assertEquals("", p.slides().get(0).notes());
    }

    @Test
    void missingOptionalFieldsGetDefaults() throws IOException {
        Presentation p = ImportManager.fromJson("{\"slides\":[{\"title\":\"A\"}]}");
        assertEquals(Presentation.DEFAULT_TITLE, p.title());
        assertEquals(Theme.DEFAULT, p.theme());
        assertEquals(Sign.NONE, p.sign());
    }

    @Test
    void incompleteThemeFallsBackToDefault() throws IOException {
        Presentation p = ImportManager.fromJson("{\"theme\":{\"backgroundColor\":\"#fff\"},\"slides\":[{}]}");
        assertEquals(Theme.DEFAULT, p.theme());
        Presentation bad = ImportManager.fromJson(
                "{\"theme\":{\"backgroundColor\":\"nope\",\"textColor\":\"#fff\"},\"slides\":[{}]}");
        assertEquals(Theme.DEFAULT, bad.theme());
    }

    @Test
    void themeExportedByTheHtmlDemoIsUnderstood() throws IOException {
        Presentation p = ImportManager.fromJson("""
                {"theme":{"name":"Neon Cyberpunk","backgroundColor":"#000000","textColor":"#00e5ff",
                          "fontFamily":"ui-monospace, monospace","textAlignment":"LEFT"},
                 "slides":[{}]}""");
        assertEquals(Theme.NEON_CYBERPUNK, p.theme());
    }

    @Test
    void unnamedImportedThemeGetsSyntheticName() throws IOException {
        Presentation p = ImportManager.fromJson(
                "{\"theme\":{\"backgroundColor\":\"#123456\",\"textColor\":\"#fff\"},\"slides\":[{}]}");
        assertEquals(ImportManager.IMPORTED_THEME_NAME, p.theme().name());
    }

    @Test
    void unknownFieldsAreIgnored() throws IOException {
        Presentation p = ImportManager.fromJson(
                "{\"unknown\":{\"a\":1},\"sign\":{\"enabled\":true,\"text\":\"x\",\"color\":\"red\"},\"slides\":[{\"zzz\":1}]}");
        assertEquals(new Sign(true, "x"), p.sign());
    }

    @Test
    void fileWithUtf8BomIsAccepted(@TempDir Path dir) throws IOException {
        byte[] body = "{\"slides\":[{\"title\":\"é\"}]}".getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);
        Path file = dir.resolve("bom.slidecraft");
        Files.write(file, withBom);
        assertEquals("é", ImportManager.fromFile(file).slides().get(0).title());
    }

    @Test
    void emptyFileIsRejected(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("empty.json");
        Files.write(file, new byte[0]);
        assertThrows(PresentationFormatException.class, () -> ImportManager.fromFile(file));
    }
}
