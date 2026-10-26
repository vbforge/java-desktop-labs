package com.vbforge.animalpedia.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * One animal, either from the built-in guide or fetched from Wikipedia.
 * Only {@code slug} and {@code commonName} are required; every other field may be null
 * and the UI simply leaves out what is missing.
 */
public record Animal(
        String slug,
        String commonName,
        String scientificName,
        String emoji,
        String group,
        ConservationStatus status,
        String diet,
        String habitat,
        String lifespan,
        String size,
        String funFact,
        String summary,
        List<String> paragraphs,
        Taxonomy taxonomy,
        DataSource source,
        String url) {

    public static final String DEFAULT_EMOJI = "🐾"; // paw prints
    public static final String WEB_GROUP = "From Wikipedia";

    public Animal {
        if (slug == null || slug.isBlank()) throw new IllegalArgumentException("slug is required");
        if (commonName == null || commonName.isBlank()) throw new IllegalArgumentException("commonName is required");
        if (emoji == null || emoji.isBlank()) emoji = DEFAULT_EMOJI;
        if (group == null || group.isBlank()) group = WEB_GROUP;
        if (source == null) source = DataSource.BUILT_IN;
        paragraphs = paragraphs == null ? List.of() : List.copyOf(paragraphs);
    }

    public Animal withSource(DataSource newSource) {
        return new Animal(slug, commonName, scientificName, emoji, group, status, diet, habitat,
                lifespan, size, funFact, summary, paragraphs, taxonomy, newSource, url);
    }

    /** True when the query is a case-insensitive piece of the common or scientific name. */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase(Locale.ROOT);
        return commonName.toLowerCase(Locale.ROOT).contains(q)
                || (scientificName != null && scientificName.toLowerCase(Locale.ROOT).contains(q));
    }

    /** Link to the matching Wikipedia article. */
    public String wikiUrl() {
        if (url != null && !url.isBlank()) return url;
        String title = (scientificName != null && !scientificName.isBlank()) ? scientificName : commonName;
        return "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    /** File-system and URL safe id: lower-case letters and digits separated by single dashes. */
    public static String slugOf(String title) {
        if (title == null) return "untitled";
        String s = title.trim().toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s.isEmpty() ? "untitled" : s;
    }
}
