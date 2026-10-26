package com.vbforge.animalpedia.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/** The "family tree" of an animal. Any rank may be null when it is not known. */
public record Taxonomy(
        String kingdom,
        String phylum,
        @JsonProperty("class") String className,
        String order,
        String family,
        String genus) {

    @JsonIgnore
    public boolean isEmpty() {
        return blank(kingdom) && blank(phylum) && blank(className)
                && blank(order) && blank(family) && blank(genus);
    }

    /** Known ranks in order, from broadest to narrowest. Unknown ranks are left out. */
    public Map<String, String> ranks() {
        Map<String, String> m = new LinkedHashMap<>();
        put(m, "Kingdom", kingdom);
        put(m, "Phylum", phylum);
        put(m, "Class", className);
        put(m, "Order", order);
        put(m, "Family", family);
        put(m, "Genus", genus);
        return m;
    }

    private static void put(Map<String, String> m, String k, String v) {
        if (!blank(v)) m.put(k, v);
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }
}
