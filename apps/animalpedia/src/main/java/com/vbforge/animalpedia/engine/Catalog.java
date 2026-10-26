package com.vbforge.animalpedia.engine;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/** The animals that ship with the app. Always available, even with no internet. */
public final class Catalog {

    public static final String RESOURCE = "/catalog/animals.json";

    private final List<Animal> animals;

    public Catalog(List<Animal> animals) {
        Set<String> seen = new HashSet<>();
        for (Animal a : animals) {
            if (!seen.add(a.slug())) throw new IllegalStateException("Duplicate animal: " + a.slug());
        }
        this.animals = List.copyOf(animals);
    }

    public static Catalog load() {
        try (InputStream in = Catalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("Missing resource " + RESOURCE);
            return fromJson(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the animal catalog", e);
        }
    }

    public static Catalog fromJson(InputStream in) throws IOException {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        List<Animal> list = mapper.readValue(in, new TypeReference<List<Animal>>() {});
        return new Catalog(list.stream().map(a -> a.withSource(DataSource.BUILT_IN)).toList());
    }

    public List<Animal> all() { return animals; }

    public int size() { return animals.size(); }

    /** Group names in the order they first appear (Mammals, Birds, ...). */
    public List<String> groups() {
        Set<String> groups = new LinkedHashSet<>();
        animals.forEach(a -> groups.add(a.group()));
        return List.copyOf(groups);
    }

    /** Animals matching a name fragment, optionally limited to one group. Blank query matches everything. */
    public List<Animal> search(String query, String group) {
        return animals.stream()
                .filter(a -> group == null || a.group().equals(group))
                .filter(a -> a.matches(query))
                .toList();
    }

    /** Finds an animal whose common or scientific name equals the text (ignoring case). */
    public Optional<Animal> findExact(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String n = name.trim().toLowerCase(Locale.ROOT);
        return animals.stream()
                .filter(a -> a.commonName().toLowerCase(Locale.ROOT).equals(n)
                        || (a.scientificName() != null && a.scientificName().toLowerCase(Locale.ROOT).equals(n)))
                .findFirst();
    }
}
