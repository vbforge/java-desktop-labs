package com.vbforge.atlasdesk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads a JSON file of countries, validates every entry and inserts the new ones. */
public final class CountryImporter {

    public record Result(int added, int alreadyInDb, int duplicatesInFile, List<String> invalid) {}

    /** Boxed types so a missing field is detectable (a primitive would silently become 0). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Raw(String code, String name, String continent, String capital,
               Long countryPopulation, Long capitalPopulation, Long area,
               Double latitude, Double longitude) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private CountryImporter() {}

    public static Result importFile(Path file, CountryDao dao, Collection<String> knownContinents) {
        JsonNode entries = readEntries(file);

        // lower-case -> canonical spelling, so "europe" in the file reuses the existing "Europe"
        Map<String, String> continents = new HashMap<>();
        for (String c : knownContinents) continents.put(c.toLowerCase(Locale.ROOT), c);

        Map<String, Country> unique = new LinkedHashMap<>();
        List<String> invalid = new ArrayList<>();
        int duplicatesInFile = 0;
        int index = 0;

        for (JsonNode node : entries) {
            index++;
            try {
                Country country = toCountry(MAPPER.treeToValue(node, Raw.class), continents);
                if (unique.putIfAbsent(country.code(), country) != null) duplicatesInFile++;
            } catch (JsonProcessingException e) {
                invalid.add("#" + index + " " + label(node) + ": " + e.getOriginalMessage());
            } catch (IllegalArgumentException e) {
                invalid.add("#" + index + " " + label(node) + ": " + e.getMessage());
            }
        }

        int added = unique.isEmpty() ? 0 : dao.insertIgnoringDuplicates(List.copyOf(unique.values()));
        return new Result(added, unique.size() - added, duplicatesInFile, invalid);
    }

    // ---------- reading ----------

    private static JsonNode readEntries(Path file) {
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            if (root != null && root.isArray()) return root;
            if (root != null && root.path("countries").isArray()) return root.get("countries");
            throw new IllegalArgumentException(
                    "The file must contain a JSON array of countries (or an object with a \"countries\" array).");
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Not valid JSON (line "
                    + e.getLocation().getLineNr() + "): " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read the file: " + e.getMessage(), e);
        }
    }

    // ---------- validation ----------

    private static Country toCountry(Raw r, Map<String, String> continents) {
        if (r == null) throw new IllegalArgumentException("empty entry");

        String code = text(r.code(), "code").toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{2}")) throw new IllegalArgumentException("code must be exactly 2 letters");

        String name = text(r.name(), "name");
        String continentRaw = text(r.continent(), "continent");
        String capital = text(r.capital(), "capital");
        long countryPop = number(r.countryPopulation(), "countryPopulation", 0);
        long capitalPop = number(r.capitalPopulation(), "capitalPopulation", 0);
        long area = number(r.area(), "area", 1);
        double lat = coord(r.latitude(), "latitude", -90, 90);
        double lon = coord(r.longitude(), "longitude", -180, 180);

        // normalised last, so an invalid entry never registers a new continent spelling
        String continent = continents.computeIfAbsent(continentRaw.toLowerCase(Locale.ROOT), k -> continentRaw);

        return new Country(0, code, name, continent, capital, countryPop, capitalPop, area, lat, lon);
    }

    private static String text(String v, String field) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(field + " is missing");
        return v.trim();
    }

    private static long number(Long v, String field, long min) {
        if (v == null) throw new IllegalArgumentException(field + " is missing");
        if (v < min) throw new IllegalArgumentException(field + " must be at least " + min);
        return v;
    }

    private static double coord(Double v, String field, double min, double max) {
        if (v == null) throw new IllegalArgumentException(field + " is missing");
        if (!(v >= min && v <= max)) {
            throw new IllegalArgumentException(field + " must be between " + min + " and " + max);
        }
        return v;
    }

    private static String label(JsonNode node) {
        String l = node.path("name").asText(node.path("code").asText(""));
        return l.isBlank() ? "" : "(" + l + ")";
    }
}