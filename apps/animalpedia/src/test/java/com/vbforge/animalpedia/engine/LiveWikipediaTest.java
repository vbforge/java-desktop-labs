package com.vbforge.animalpedia.engine;

import com.vbforge.animalpedia.model.Animal;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Talks to the real Wikipedia to confirm the recorded fixtures still match the live API.
 * Skipped by default. Run it with: mvn test -DskipTags=none -Dgroups=live
 */
@Tag("live")
class LiveWikipediaTest {

    private final WikipediaClient client = new WikipediaClient();

    @Test
    void searchReturnsTitles() throws Exception {
        List<String> titles = client.search("monkey");
        assertFalse(titles.isEmpty());
    }

    @Test
    void summaryHasTextAndTitle() throws Exception {
        Animal a = client.summary("Red panda");
        assertTrue(a.commonName().toLowerCase().contains("red panda"));
        assertFalse(a.paragraphs().isEmpty());
    }
}
