package com.vbforge.animalpedia.engine;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/** Saves Wikipedia articles on disk so a second visit is instant and works offline. */
public class ArticleCache {

    public static final Duration DEFAULT_TTL = Duration.ofDays(7);
    private static final System.Logger LOG = System.getLogger(ArticleCache.class.getName());
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public record Entry(long cachedAt, Animal animal) {}

    /** A cached article, and whether it is older than the time-to-live. */
    public record Cached(Animal animal, boolean stale) {}

    private final Path dir;
    private final Clock clock;
    private final Duration ttl;

    public ArticleCache(Path dir, Clock clock, Duration ttl) {
        this.dir = dir;
        this.clock = clock;
        this.ttl = ttl;
    }

    /** {@code ~/.animalpedia/cache} with a 7 day time-to-live. */
    public static ArticleCache defaultCache() {
        Path home = Path.of(System.getProperty("user.home"), ".animalpedia", "cache");
        return new ArticleCache(home, Clock.systemUTC(), DEFAULT_TTL);
    }

    public Path directory() { return dir; }

    public Optional<Cached> read(String slug) {
        Path file = fileFor(slug);
        if (!Files.isRegularFile(file)) return Optional.empty();
        try {
            Entry entry = JSON.readValue(file.toFile(), Entry.class);
            if (entry == null || entry.animal() == null) throw new IOException("empty entry");
            long age = clock.millis() - entry.cachedAt();
            boolean stale = age > ttl.toMillis() || age < 0;
            return Optional.of(new Cached(entry.animal().withSource(DataSource.CACHED), stale));
        } catch (IOException | RuntimeException e) {
            LOG.log(System.Logger.Level.WARNING, "Skipping unreadable cache file {0}: {1}", file, e.getMessage());
            return Optional.empty();
        }
    }

    public void write(Animal animal) throws IOException {
        Files.createDirectories(dir);
        Path target = fileFor(animal.slug());
        Path tmp = Files.createTempFile(dir, "entry-", ".tmp");
        try {
            JSON.writeValue(tmp.toFile(), new Entry(clock.millis(), animal.withSource(DataSource.WIKIPEDIA)));
            try {
                Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private Path fileFor(String slug) {
        // Re-slug so a hostile name like "../../x" can never leave the cache folder.
        return dir.resolve(Animal.slugOf(slug) + ".json");
    }
}
