package com.vbforge.slidecraft.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PresentationTest {

    @Test
    void newPresentationGetsDefaultThemeAndEmptySign() {
        Presentation p = Presentation.blank();
        assertEquals(Theme.DEFAULT, p.theme());
        assertEquals(new Sign(false, ""), p.sign());
        assertEquals(1, p.slides().size());
    }

    @Test
    void nullsAreReplacedWithDefaults() {
        Presentation p = new Presentation(null, null, null, null);
        assertEquals(Presentation.DEFAULT_TITLE, p.title());
        assertEquals(Theme.DEFAULT, p.theme());
        assertEquals(Sign.NONE, p.sign());
        assertTrue(p.slides().isEmpty());
        assertEquals("", new Slide(null, null, null).notes());
    }

    @Test
    void editingReturnsNewInstancesAndLeavesOriginalUntouched() {
        Presentation p = Presentation.blank();
        Presentation q = p.withSlideInserted(1, Slide.blank()).withTitle("  Deck  ");
        assertEquals(1, p.slides().size());
        assertEquals(2, q.slides().size());
        assertEquals("Deck", q.title());
        assertEquals(1, q.withSlideRemoved(0).slides().size());
        assertEquals("X", q.withSlide(0, new Slide("X", "", "")).slides().get(0).title());
    }

    @Test
    void signRendersOnlyWhenEnabledAndNotBlank() {
        assertFalse(new Sign(false, "hi").shouldRender());
        assertFalse(new Sign(true, "   ").shouldRender());
        assertTrue(new Sign(true, "hi").shouldRender());
        assertEquals("a b", new Sign(true, "a\nb").text());
    }

    @Test
    void slideListIsDefensivelyCopied() {
        java.util.ArrayList<Slide> mutable = new java.util.ArrayList<>(List.of(Slide.blank()));
        Presentation p = new Presentation("t", null, null, mutable);
        mutable.clear();
        assertEquals(1, p.slides().size());
    }
}
