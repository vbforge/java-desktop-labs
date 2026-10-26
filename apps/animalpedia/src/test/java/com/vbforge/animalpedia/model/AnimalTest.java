package com.vbforge.animalpedia.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimalTest {

    private static Animal minimal(String slug, String name) {
        return new Animal(slug, name, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void requiredFieldsAreEnforced() {
        assertThrows(IllegalArgumentException.class, () -> minimal(null, "Lion"));
        assertThrows(IllegalArgumentException.class, () -> minimal("lion", " "));
    }

    @Test
    void optionalFieldsGetFriendlyDefaults() {
        Animal a = minimal("lion", "Lion");
        assertEquals(Animal.DEFAULT_EMOJI, a.emoji());
        assertEquals(Animal.WEB_GROUP, a.group());
        assertEquals(DataSource.BUILT_IN, a.source());
        assertTrue(a.paragraphs().isEmpty());
    }

    @Test
    void paragraphListIsDefensivelyCopied() {
        java.util.ArrayList<String> source = new java.util.ArrayList<>(List.of("one"));
        Animal a = new Animal("x", "X", null, null, null, null, null, null, null, null, null, null, source, null, null, null);
        source.add("two");
        assertEquals(List.of("one"), a.paragraphs());
    }

    @Test
    void withSourceChangesOnlyTheSource() {
        Animal a = minimal("lion", "Lion");
        Animal b = a.withSource(DataSource.CACHED);
        assertEquals(DataSource.CACHED, b.source());
        assertEquals(a.slug(), b.slug());
        assertEquals(a.commonName(), b.commonName());
    }

    @Test
    void matchesCommonAndScientificNamesIgnoringCase() {
        Animal a = new Animal("lion", "Lion", "Panthera leo", null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertTrue(a.matches("LIO"));
        assertTrue(a.matches("panthera"));
        assertTrue(a.matches("  "));
        assertTrue(a.matches(null));
        assertFalse(a.matches("tiger"));
    }

    @Test
    void slugsAreSafeForFileNames() {
        assertEquals("african-bush-elephant", Animal.slugOf("African Bush Elephant"));
        assertEquals("père-david-s-deer", Animal.slugOf("Père David's deer"));
        assertEquals("x", Animal.slugOf("../../x"));
        assertEquals("untitled", Animal.slugOf("***"));
        assertEquals("untitled", Animal.slugOf(null));
    }

    @Test
    void wikiUrlUsesGivenUrlOrBuildsOne() {
        Animal withUrl = new Animal("a", "A", null, null, null, null, null, null, null, null, null, null, null, null, null, "https://example.org/a");
        assertEquals("https://example.org/a", withUrl.wikiUrl());

        Animal built = new Animal("lion", "Lion", "Panthera leo", null, null, null, null, null, null, null, null, null, null, null, null, null);
        assertEquals("https://en.wikipedia.org/wiki/Panthera_leo", built.wikiUrl());
    }
}
