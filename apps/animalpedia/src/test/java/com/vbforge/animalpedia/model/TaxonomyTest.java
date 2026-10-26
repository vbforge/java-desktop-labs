package com.vbforge.animalpedia.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxonomyTest {

    @Test
    void allNullOrBlankIsEmpty() {
        assertTrue(new Taxonomy(null, null, null, null, null, null).isEmpty());
        assertTrue(new Taxonomy(" ", "", null, null, null, null).isEmpty());
    }

    @Test
    void partialIsNotEmptyAndSkipsMissingRanks() {
        Taxonomy t = new Taxonomy("Animalia", null, "Mammalia", null, "Felidae", null);
        assertFalse(t.isEmpty());
        assertEquals(List.of("Kingdom", "Class", "Family"), List.copyOf(t.ranks().keySet()));
    }

    @Test
    void fullTaxonomyKeepsBroadToNarrowOrder() {
        Taxonomy t = new Taxonomy("Animalia", "Chordata", "Mammalia", "Carnivora", "Felidae", "Panthera");
        assertEquals(List.of("Kingdom", "Phylum", "Class", "Order", "Family", "Genus"), List.copyOf(t.ranks().keySet()));
        assertEquals("Panthera", t.ranks().get("Genus"));
    }
}
