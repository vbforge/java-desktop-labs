package com.vbforge.animalpedia.engine;

import com.vbforge.animalpedia.model.Animal;
import com.vbforge.animalpedia.model.DataSource;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogTest {

    private final Catalog catalog = Catalog.load();

    @Test
    void shipsFifteenAnimals() {
        assertEquals(15, catalog.size());
    }

    @Test
    void groupsAreInDisplayOrder() {
        assertEquals(List.of("Mammals", "Birds", "Reptiles", "Amphibians", "Insects"), catalog.groups());
    }

    @Test
    void groupSizesMatchTheGuide() {
        assertEquals(6, catalog.search("", "Mammals").size());
        assertEquals(3, catalog.search("", "Birds").size());
        assertEquals(3, catalog.search("", "Reptiles").size());
        assertEquals(1, catalog.search("", "Amphibians").size());
        assertEquals(2, catalog.search("", "Insects").size());
    }

    @Test
    void everyAnimalIsFullyFilledIn() {
        Set<String> slugs = new HashSet<>();
        for (Animal a : catalog.all()) {
            String who = a.slug();
            assertTrue(slugs.add(a.slug()), "duplicate slug " + who);
            assertTrue(a.slug().matches("[a-z0-9]+(-[a-z0-9]+)*"), "url-safe slug " + who);
            assertEquals(DataSource.BUILT_IN, a.source(), who);
            assertNotNull(a.scientificName(), who);
            assertNotNull(a.emoji(), who);
            assertNotNull(a.status(), who);
            assertNotNull(a.diet(), who);
            assertNotNull(a.habitat(), who);
            assertNotNull(a.lifespan(), who);
            assertNotNull(a.size(), who);
            assertNotNull(a.funFact(), who);
            assertNotNull(a.summary(), who);
            assertNotNull(a.taxonomy(), who);
            assertFalse(a.taxonomy().isEmpty(), who);
            assertEquals(6, a.taxonomy().ranks().size(), who + " taxonomy ranks");
            assertTrue(a.paragraphs().size() >= 2, who + " needs at least two paragraphs");
            assertEquals(Animal.slugOf(a.commonName()), a.slug(), who + " slug matches name");
        }
    }

    @Test
    void searchFindsByCommonOrScientificNameAnyCase() {
        assertEquals(List.of("lion"), catalog.search("LION", null).stream().map(Animal::slug).toList());
        assertEquals(List.of("tiger"), catalog.search("panthera tigris", null).stream().map(Animal::slug).toList());
        assertEquals(2, catalog.search("panthera", null).size());
        assertTrue(catalog.search("zzzz", null).isEmpty());
    }

    @Test
    void searchCanBeLimitedToAGroup() {
        assertTrue(catalog.search("lion", "Birds").isEmpty());
        assertEquals(1, catalog.search("lion", "Mammals").size());
    }

    @Test
    void findExactMatchesWholeNamesOnly() {
        assertTrue(catalog.findExact("lion").isPresent());
        assertTrue(catalog.findExact("Panthera leo").isPresent());
        assertTrue(catalog.findExact("lio").isEmpty());
        assertTrue(catalog.findExact(" ").isEmpty());
        assertTrue(catalog.findExact(null).isEmpty());
    }

    @Test
    void duplicateSlugsAreRejected() {
        Animal a = new Animal("x", "X", null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertThrows(IllegalStateException.class, () -> new Catalog(List.of(a, a)));
    }
}
