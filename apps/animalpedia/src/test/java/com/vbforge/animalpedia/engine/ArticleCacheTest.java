package com.vbforge.animalpedia.engine;

import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleCacheTest {

    /** A clock the test can move forward by hand. */
    static final class TestClock extends Clock {
        private Instant now = Instant.parse("2026-10-01T12:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private static Animal capybaraLike() {
        return new Animal("red-panda", "Red panda", null, null, null, null, null, null, null, null, null,
                "Small mammal", List.of("First.", "Second."), null, DataSource.WIKIPEDIA, "https://en.wikipedia.org/wiki/Red_panda");
    }

    @TempDir
    Path dir;

    @Test
    void roundTripsAnArticleAndMarksItCached() throws IOException {
        ArticleCache cache = new ArticleCache(dir, new TestClock(), ArticleCache.DEFAULT_TTL);
        cache.write(capybaraLike());

        Optional<ArticleCache.Cached> hit = cache.read("red-panda");
        assertTrue(hit.isPresent());
        assertFalse(hit.get().stale());
        Animal a = hit.get().animal();
        assertEquals("Red panda", a.commonName());
        assertEquals(List.of("First.", "Second."), a.paragraphs());
        assertEquals("Small mammal", a.summary());
        assertEquals(DataSource.CACHED, a.source());
        assertEquals("https://en.wikipedia.org/wiki/Red_panda", a.url());
    }

    @Test
    void missingEntryIsEmpty() {
        ArticleCache cache = new ArticleCache(dir, new TestClock(), ArticleCache.DEFAULT_TTL);
        assertTrue(cache.read("nothing-here").isEmpty());
    }

    @Test
    void entriesBecomeStaleAfterTheTimeToLive() throws IOException {
        TestClock clock = new TestClock();
        ArticleCache cache = new ArticleCache(dir, clock, Duration.ofDays(7));
        cache.write(capybaraLike());

        clock.advance(Duration.ofDays(6).plusHours(23));
        assertFalse(cache.read("red-panda").orElseThrow().stale());

        clock.advance(Duration.ofHours(2));
        ArticleCache.Cached old = cache.read("red-panda").orElseThrow();
        assertTrue(old.stale());
        assertEquals("Red panda", old.animal().commonName()); // stale content is still returned
    }

    @Test
    void corruptFilesAreSkippedNotFatal() throws IOException {
        ArticleCache cache = new ArticleCache(dir, new TestClock(), ArticleCache.DEFAULT_TTL);
        Files.writeString(dir.resolve("broken.json"), "{ this is not json");
        Files.writeString(dir.resolve("empty.json"), "");
        assertTrue(cache.read("broken").isEmpty());
        assertTrue(cache.read("empty").isEmpty());
    }

    @Test
    void writingReplacesAnOlderCopyAndLeavesNoTempFiles() throws IOException {
        TestClock clock = new TestClock();
        ArticleCache cache = new ArticleCache(dir, clock, Duration.ofDays(7));
        cache.write(capybaraLike());
        clock.advance(Duration.ofDays(30));
        cache.write(capybaraLike());

        assertFalse(cache.read("red-panda").orElseThrow().stale());
        try (var files = Files.list(dir)) {
            assertEquals(List.of("red-panda.json"), files.map(p -> p.getFileName().toString()).toList());
        }
    }

    @Test
    void hostileSlugsCannotEscapeTheCacheFolder() throws IOException {
        ArticleCache cache = new ArticleCache(dir.resolve("cache"), new TestClock(), ArticleCache.DEFAULT_TTL);
        Animal sneaky = new Animal("../../evil", "Evil", null, null, null, null, null, null, null, null, null,
                null, List.of("x"), null, DataSource.WIKIPEDIA, null);
        cache.write(sneaky);
        assertTrue(Files.exists(dir.resolve("cache").resolve("evil.json")));
        assertFalse(Files.exists(dir.getParent().resolve("evil.json")));
    }
}
