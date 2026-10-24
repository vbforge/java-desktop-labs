package com.vbforge.slidecraft.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeTest {

    @Test
    void shortHexIsExpandedAndLowercased() {
        Theme t = new Theme("t", "#ABC", "#0FF", "System", "LEFT");
        assertEquals("#aabbcc", t.backgroundColor());
        assertEquals("#00ffff", t.textColor());
    }

    @Test
    void invalidColorsFallBackSoTheyCanNeverReachInlineCss() {
        Theme t = new Theme("t", "red; -fx-effect: dropshadow(gaussian, red, 99, 0, 0, 0)", "#fff'; x", "System", "LEFT");
        assertEquals("#1a1a1a", t.backgroundColor());
        assertEquals("#e6edf3", t.textColor());
        assertFalse(Theme.isValidColor("#12345"));
        assertTrue(Theme.isValidColor(" #123456 "));
    }

    @Test
    void cssFontStacksAreMappedToJavaFxFamilies() {
        assertEquals("System", new Theme("t", "#000", "#fff", "system-ui, sans-serif", "LEFT").fontFamily());
        assertEquals("Georgia", new Theme("t", "#000", "#fff", "Georgia, serif", "LEFT").fontFamily());
        assertEquals("Monospaced", new Theme("t", "#000", "#fff", "ui-monospace, monospace", "LEFT").fontFamily());
        assertEquals("Courier New", new Theme("t", "#000", "#fff", "'Courier New', monospace", "LEFT").fontFamily());
        assertEquals("System", new Theme("t", "#000", "#fff", "Arial'; -fx-x: y", "LEFT").fontFamily());
    }

    @Test
    void alignmentAndNameAreNormalised() {
        assertEquals("CENTER", new Theme("t", "#000", "#fff", "System", "weird").textAlignment());
        assertEquals("LEFT", new Theme("t", "#000", "#fff", "System", " left ").textAlignment());
        assertEquals("Custom", new Theme("  ", "#000", "#fff", "System", "LEFT").name());
        assertEquals(60, new Theme("x".repeat(100), "#000", "#fff", "System", "LEFT").name().length());
    }

    @Test
    void normalisationIsIdempotent() {
        Theme once = new Theme("Odd", "#ABC", "#DEF", "serif", "left");
        Theme twice = new Theme(once.name(), once.backgroundColor(), once.textColor(), once.fontFamily(), once.textAlignment());
        assertEquals(once, twice);
    }
}
