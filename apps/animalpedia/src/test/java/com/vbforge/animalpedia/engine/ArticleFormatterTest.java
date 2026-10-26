package com.vbforge.animalpedia.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArticleFormatterTest {

    @Test
    void nullAndBlankGiveNoParagraphs() {
        assertTrue(ArticleFormatter.paragraphs(null).isEmpty());
        assertTrue(ArticleFormatter.paragraphs("  \n\n \n").isEmpty());
    }

    @Test
    void splitsOnBlankLinesAndCollapsesExtraOnes() {
        assertEquals(List.of("One.", "Two.", "Three."),
                ArticleFormatter.paragraphs("One.\n\n\n\nTwo.\n  \nThree."));
    }

    @Test
    void trimsAndJoinsSingleNewlinesInsideAParagraph() {
        assertEquals(List.of("Line one line two."), ArticleFormatter.paragraphs("  Line one\nline two.  "));
    }

    @Test
    void handlesWindowsLineEndings() {
        assertEquals(List.of("A.", "B."), ArticleFormatter.paragraphs("A.\r\n\r\nB."));
    }

    @Test
    void splitsLongParagraphsAtSentenceBoundaries() {
        String sentence = "This is a fairly ordinary sentence about an animal. ";
        String text = sentence.repeat(20).trim();
        List<String> out = ArticleFormatter.paragraphs(text);
        assertTrue(out.size() > 1, "should be split");
        for (String p : out) {
            assertTrue(p.length() <= ArticleFormatter.MAX_PARAGRAPH, "paragraph too long: " + p.length());
            assertTrue(p.endsWith("."), "should end at a sentence boundary");
        }
        assertEquals(text, String.join(" ", out));
    }

    @Test
    void doesNotSplitInsideAbbreviationsBeforeLowercaseWords() {
        String s = "Loxodonta africana is large. L. africana lives in Africa.";
        assertEquals(List.of(s), ArticleFormatter.paragraphs(s));
    }
}
