package com.vbforge.dietarytrack;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidatorsTest {

    @Test
    void namesAreRequiredAndLengthLimited() {
        assertTrue(Validators.name("  ", "First name").isPresent());
        assertTrue(Validators.name(null, "First name").isPresent());
        assertTrue(Validators.name("x".repeat(61), "First name").isPresent());
        assertFalse(Validators.name("Олена", "First name").isPresent());
    }

    @Test
    void optionalFieldsMayBeBlankButMustBeSaneWhenFilled() {
        assertFalse(Validators.phone("").isPresent());
        assertFalse(Validators.phone("+380 (50) 123-45-67").isPresent());
        assertTrue(Validators.phone("abc").isPresent());
        assertTrue(Validators.phone("12").isPresent());

        assertFalse(Validators.email("").isPresent());
        assertFalse(Validators.email("name@example.com").isPresent());
        assertTrue(Validators.email("name@example").isPresent());
        assertTrue(Validators.email("a b@example.com").isPresent());
    }

    @Test
    void dateMustExistAndNotBeInTheFuture() {
        LocalDate today = LocalDate.of(2026, 10, 23);
        assertTrue(Validators.date(null, today).isPresent());
        assertTrue(Validators.date(today.plusDays(1), today).isPresent());
        assertFalse(Validators.date(today, today).isPresent());
        assertFalse(Validators.date(today.minusYears(1), today).isPresent());
    }

    @Test
    void waterIsAWholeNumberInRange() {
        assertEquals(15, Validators.parseWater("15").orElseThrow());
        assertEquals(0, Validators.parseWater(" 0 ").orElseThrow());
        assertTrue(Validators.parseWater("").isEmpty());
        assertTrue(Validators.parseWater("51").isEmpty());
        assertTrue(Validators.parseWater("-1").isEmpty());
        assertTrue(Validators.parseWater("1.5").isEmpty());
    }

    @Test
    void weightAcceptsDotOrCommaAndRejectsJunk() {
        assertEquals(72.5, Validators.parseWeight("72.5").orElseThrow());
        assertEquals(72.5, Validators.parseWeight("72,5").orElseThrow());
        assertEquals(72.0, Validators.parseWeight("72").orElseThrow());
        assertEquals(72.6, Validators.parseWeight("72.55").orElseThrow());
        for (String bad : new String[]{"", "abc", "NaN", "Infinity", "1e2", "0x10", "9", "501", "-5", "72.", "1d"}) {
            assertTrue(Validators.parseWeight(bad).isEmpty(), "should reject: " + bad);
        }
    }
}
