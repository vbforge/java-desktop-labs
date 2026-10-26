package com.vbforge.animalpedia.engine;

import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalRepositoryTest {

    /** A scripted stand-in for Wikipedia that counts calls. */
    static final class FakeWiki implements WikiSource {
        final List<String> summaryCalls = new ArrayList<>();
        final List<String> searchCalls = new ArrayList<>();
        List<String> searchResult = List.of();
        LookupException failure;

        @Override
        public List<String> search(String query) throws LookupException {
            searchCalls.add(query);
            if (failure != null) throw failure;
            return searchResult;
        }

        @Override
        public Animal summary(String title) throws LookupException {
            summaryCalls.add(title);
            if (failure != null) throw failure;
            return new Animal(Animal.slugOf(title), title, null, null, null, null, null, null, null, null, null,
                    "Fetched " + title, List.of("Body of " + title), null, DataSource.WIKIPEDIA, null);
        }
    }

    /** Moves in steps when told; starts on a fixed day. */
    static final class MovableClock extends Clock {
        private Instant now = Instant.parse("2026-10-01T00:00:00Z");
        void advance(Duration d) { now = now.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    @TempDir
    Path dir;

    private final FakeWiki wiki = new FakeWiki();
    private final MovableClock clock = new MovableClock();

    private AnimalRepository repo() {
        return new AnimalRepository(Catalog.load(), wiki, new ArticleCache(dir, clock, Duration.ofDays(7)));
    }

    private static LookupException offline() {
        return new LookupException(LookupException.Kind.OFFLINE, "no internet");
    }

    @Test
    void builtInAnimalsNeverTouchTheNetworkOrCache() throws Exception {
        Animal lion = repo().open("lion", false);
        assertEquals(DataSource.BUILT_IN, lion.source());
        assertEquals("Panthera leo", lion.scientificName());
        assertTrue(wiki.summaryCalls.isEmpty());
    }

    @Test
    void builtInAlsoMatchesScientificName() throws Exception {
        assertEquals("Lion", repo().open("Panthera leo", false).commonName());
    }

    @Test
    void cacheMissFetchesOnceThenServesFromCache() throws Exception {
        AnimalRepository repo = repo();

        Animal first = repo.open("Red panda", false);
        assertEquals(DataSource.WIKIPEDIA, first.source());
        assertEquals(1, wiki.summaryCalls.size());

        Animal second = repo.open("Red panda", false);
        assertEquals(DataSource.CACHED, second.source());
        assertEquals("Body of Red panda", second.paragraphs().get(0));
        assertEquals(1, wiki.summaryCalls.size(), "cache hit must not call Wikipedia");
    }

    @Test
    void staleCopyIsRefreshedWhenOnline() throws Exception {
        AnimalRepository repo = repo();
        repo.open("Red panda", false);
        clock.advance(Duration.ofDays(8));

        Animal again = repo.open("Red panda", false);
        assertEquals(DataSource.WIKIPEDIA, again.source());
        assertEquals(2, wiki.summaryCalls.size());
    }

    @Test
    void staleCopyIsStillShownWhenOffline() throws Exception {
        AnimalRepository repo = repo();
        repo.open("Red panda", false);
        clock.advance(Duration.ofDays(30));
        wiki.failure = offline();

        Animal old = repo.open("Red panda", false);
        assertEquals(DataSource.CACHED, old.source());
        assertEquals("Red panda", old.commonName());
    }

    @Test
    void manualRefreshBypassesAFreshCopy() throws Exception {
        AnimalRepository repo = repo();
        repo.open("Red panda", false);
        repo.open("Red panda", true);
        assertEquals(2, wiki.summaryCalls.size());
    }

    @Test
    void offlineWithNothingSavedReportsTheProblem() {
        wiki.failure = offline();
        LookupException e = assertThrows(LookupException.class, () -> repo().open("Red panda", false));
        assertEquals(LookupException.Kind.OFFLINE, e.kind());
    }

    @Test
    void failuresAreNotCached() throws Exception {
        wiki.failure = new LookupException(LookupException.Kind.UNUSABLE, "disambiguation");
        AnimalRepository repo = repo();
        assertThrows(LookupException.class, () -> repo.open("Jaguar", false));

        wiki.failure = null;
        assertEquals(DataSource.WIKIPEDIA, repo.open("Jaguar", false).source());
    }

    @Test
    void webSearchHidesAnimalsAlreadyInTheGuide() throws Exception {
        wiki.searchResult = List.of("Lion", "Lions Gate Bridge", "Panthera leo", "Lioness");
        assertEquals(List.of("Lions Gate Bridge", "Lioness"), repo().searchWeb("lion"));
    }

    @Test
    void webSearchErrorsPropagate() {
        wiki.failure = offline();
        assertThrows(LookupException.class, () -> repo().searchWeb("monkey"));
    }

    @Test
    void catalogIsExposed() {
        AnimalRepository repo = repo();
        assertEquals(15, repo.catalog().size());
        assertSame(repo.catalog(), repo.catalog());
        assertFalse(repo.catalog().all().isEmpty());
    }
}
