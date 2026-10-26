package com.vbforge.animalpedia.engine;

import com.vbforge.animalpedia.model.Animal;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * One place to ask for animals. Order of preference: built-in guide, then a fresh saved copy,
 * then Wikipedia, and finally an old saved copy if Wikipedia can't be reached.
 */
public class AnimalRepository {

    private static final System.Logger LOG = System.getLogger(AnimalRepository.class.getName());

    private final Catalog catalog;
    private final WikiSource wiki;
    private final ArticleCache cache;

    public AnimalRepository(Catalog catalog, WikiSource wiki, ArticleCache cache) {
        this.catalog = catalog;
        this.wiki = wiki;
        this.cache = cache;
    }

    public static AnimalRepository createDefault() {
        return new AnimalRepository(Catalog.load(), new WikipediaClient(), ArticleCache.defaultCache());
    }

    public Catalog catalog() { return catalog; }

    public ArticleCache cache() { return cache; }

    /** Online search. Titles of animals we already have in the guide are left out. */
    public List<String> searchWeb(String query) throws LookupException {
        return wiki.search(query).stream()
                .filter(title -> catalog.findExact(title).isEmpty())
                .toList();
    }

    /**
     * Opens an animal by title.
     *
     * @param refresh skip the saved copy and ask Wikipedia again
     */
    public Animal open(String title, boolean refresh) throws LookupException {
        Optional<Animal> builtIn = catalog.findExact(title);
        if (builtIn.isPresent()) return builtIn.get();

        Optional<ArticleCache.Cached> cached = cache.read(Animal.slugOf(title));
        if (cached.isPresent() && !cached.get().stale() && !refresh) {
            return cached.get().animal();
        }

        try {
            Animal fresh = wiki.summary(title);
            try {
                cache.write(fresh);
            } catch (IOException e) {
                LOG.log(System.Logger.Level.WARNING, "Could not save {0} to the cache: {1}", fresh.slug(), e.getMessage());
            }
            return fresh;
        } catch (LookupException e) {
            if (cached.isPresent()) return cached.get().animal(); // old copy beats an error message
            throw e;
        }
    }
}
