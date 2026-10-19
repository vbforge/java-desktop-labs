package com.vbforge.wordcounter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextAnalyzerTest {

    private final TextAnalyzer analyzer = new TextAnalyzer();

    @Test
    void testBasicAnalysis() {
        String text = "The quick brown fox jumped over the lazy dog. The fox is quick.";
        WordStats stats = analyzer.analyze(text, false);

        assertEquals(13, stats.totalWords());
        assertEquals(9, stats.uniqueWords());
        assertEquals("is", stats.shortestWord());
        assertEquals("jumped", stats.longestWord()); // Changed to 'jumped' (length 6)
        assertEquals(3, stats.frequencies().get("the"));
        assertEquals(2, stats.frequencies().get("quick"));
    }

    @Test
    void testStopWordsFiltering() {
        String text = "The quick brown fox jumps over the lazy dog";
        WordStats stats = analyzer.analyze(text, true);
        
        // "the" should be filtered from the chart map, but totalWords remains 9
        assertEquals(9, stats.totalWords());
        assertFalse(stats.frequencies().containsKey("the"));
        assertTrue(stats.frequencies().containsKey("quick"));
    }

    @Test
    void testEmptyText() {
        WordStats stats = analyzer.analyze("", false);
        assertEquals(0, stats.totalWords());
        assertEquals(0, stats.uniqueWords());
        assertTrue(stats.frequencies().isEmpty());
    }
}