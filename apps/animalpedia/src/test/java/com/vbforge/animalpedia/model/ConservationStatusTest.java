package com.vbforge.animalpedia.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConservationStatusTest {

    @Test
    void thereAreNineIucnCategories() {
        assertEquals(9, ConservationStatus.values().length);
    }

    @Test
    void everyStatusHasReadableTextAndAColour() {
        for (ConservationStatus s : ConservationStatus.values()) {
            assertFalse(s.displayName().isBlank(), s + " display name");
            assertFalse(s.kidExplanation().isBlank(), s + " explanation");
            assertTrue(s.colorHex().matches("#[0-9A-Fa-f]{6}"), s + " colour");
        }
    }

    @Test
    void parsesCodesIgnoringCaseAndSpaces() {
        assertEquals(ConservationStatus.VU, ConservationStatus.fromCode("vu"));
        assertEquals(ConservationStatus.EN, ConservationStatus.fromCode(" EN "));
    }

    @Test
    void unknownOrMissingCodesBecomeNotEvaluated() {
        assertEquals(ConservationStatus.NE, ConservationStatus.fromCode("banana"));
        assertEquals(ConservationStatus.NE, ConservationStatus.fromCode(null));
        assertEquals(ConservationStatus.NE, ConservationStatus.fromCode(""));
    }

    @Test
    void displayNamesMatchTheIucnWording() {
        assertEquals("Least Concern", ConservationStatus.LC.displayName());
        assertEquals("Critically Endangered", ConservationStatus.CR.displayName());
    }
}
