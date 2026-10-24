package com.vbforge.slidecraft.engine;

import com.vbforge.slidecraft.model.Presentation;
import com.vbforge.slidecraft.model.Sign;
import com.vbforge.slidecraft.model.Slide;
import com.vbforge.slidecraft.model.Theme;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileManagerTest {

    private static Presentation sample() {
        return new Presentation("My First Presentation", Theme.VIBRANT_BLUE, new Sign(true, "© 2026 Your Name"),
                List.of(new Slide("Welcome", "Hello", "Say hi"), new Slide("Problem", "Fonts", "")));
    }

    @Test
    void serializesAllFieldsInDocumentedOrder() {
        String json = FileManager.toJson(sample());
        assertTrue(json.contains("\"backgroundColor\" : \"#095be0\""));
        assertTrue(json.contains("\"enabled\" : true"));
        assertTrue(json.indexOf("\"title\"") < json.indexOf("\"theme\""));
        assertTrue(json.indexOf("\"sign\"") < json.indexOf("\"slides\""));
    }

    @Test
    void roundTripWithSignIsEqual() throws IOException {
        Presentation p = sample();
        assertEquals(p, FileManager.fromJson(FileManager.toJson(p)));
    }

    @Test
    void fileRoundTripLeavesNoTempFiles(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("deck.slidecraft");
        FileManager.write(sample(), file);
        assertEquals(sample(), FileManager.read(file));
        try (Stream<Path> files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void overwritingReplacesContent(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("deck.slidecraft");
        FileManager.write(sample(), file);
        Presentation changed = sample().withTitle("Changed");
        FileManager.write(changed, file);
        assertEquals("Changed", FileManager.read(file).title());
    }

    @Test
    void missingNotesDefaultToEmptyString() throws IOException {
        Presentation p = FileManager.fromJson("{\"slides\":[{\"title\":\"T\",\"content\":\"C\"}]}");
        assertEquals("", p.slides().get(0).notes());
    }

    @Test
    void missingSignDefaultsToDisabled() throws IOException {
        assertEquals(new Sign(false, ""), FileManager.fromJson("{\"slides\":[{}]}").sign());
    }

    @Test
    void missingThemeDefaultsToDarkMinimal() throws IOException {
        assertEquals(Theme.DEFAULT, FileManager.fromJson("{\"slides\":[{}]}").theme());
    }

    @Test
    void missingTitleDefaultsToUntitled() throws IOException {
        assertEquals("Untitled Presentation", FileManager.fromJson("{\"slides\":[{}]}").title());
    }

    @Test
    void emptySlidesArrayIsRejected() {
        assertThrows(PresentationFormatException.class, () -> FileManager.fromJson("{\"slides\":[]}"));
    }

    @Test
    void unknownFieldsAreIgnored() throws IOException {
        Presentation p = FileManager.fromJson("{\"future\":42,\"slides\":[{\"title\":\"A\",\"extra\":true}]}");
        assertEquals("A", p.slides().get(0).title());
    }
}
