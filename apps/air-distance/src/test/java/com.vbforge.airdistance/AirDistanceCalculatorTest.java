package com.vbforge.airdistance;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers the behaviour of {@link AirDistanceCalculator}.
 *
 * The original test-cases.md listed {@code known=100, numerator=0,
 * denominator=1 -> -100.00} as a valid result. That does not follow from
 * the formula (or any formula consistent with the other six rows):
 * numerator=0 means "0 of the trip has been flown", which contradicts a
 * non-zero known distance and is mathematically undefined. This
 * implementation treats numerator=0 as a validation error, same as
 * denominator=0 — see {@link #numeratorZeroThrows()}.
 */
class AirDistanceCalculatorTest {

    private static final double EPSILON = 0.01;

    // ---- normal cases -------------------------------------------------

    @ParameterizedTest(name = "{0} km at {1}/{2} of the trip -> {3} km remaining")
    @CsvSource({
            "100, 1, 2, 100.00",
            " 50, 3, 4,  16.67",
            "200, 2, 5, 300.00",
            "150, 1, 3, 300.00",
    })
    @DisplayName("remaining distance matches the derived formula")
    void normalCases(double known, double num, double den, double expected) {
        assertEquals(expected,
                AirDistanceCalculator.remainingDistance(known, num, den),
                EPSILON);
    }

    // ---- edge cases ---------------------------------------------------

    @Test
    @DisplayName("zero known distance gives zero remaining, regardless of fraction")
    void zeroKnownDistance() {
        assertEquals(0.00,
                AirDistanceCalculator.remainingDistance(0, 1, 2),
                EPSILON);
    }

    @Test
    @DisplayName("fraction equal to 1 means the whole trip is flown, zero remaining")
    void fractionEqualsOne() {
        assertEquals(0.00,
                AirDistanceCalculator.remainingDistance(100, 1, 1),
                EPSILON);
    }

    @Test
    @DisplayName("numerator greater than denominator gives negative remaining — mathematically valid")
    void fractionGreaterThanOne() {
        // 100 * (2 - 3) / 3 = -33.33
        assertEquals(-33.33,
                AirDistanceCalculator.remainingDistance(100, 3, 2),
                EPSILON);
    }

    @Test
    @DisplayName("negative known distance scales linearly — the formula has no guard clause")
    void negativeKnownDistance() {
        // The formula is linear in knownDistance:
        //   -100 * (2 - 1) / 1 = -100
        // Not physically meaningful, but the contract is "no guard, formula applies."
        assertEquals(-100.00,
                AirDistanceCalculator.remainingDistance(-100, 1, 2),
                EPSILON);
    }

    @Test
    @DisplayName("large denominator represents a tiny fraction flown -> large remaining")
    void tinyFractionFlown() {
        // 100 * (1000 - 1) / 1 = 99900
        assertEquals(99_900.00,
                AirDistanceCalculator.remainingDistance(100, 1, 1000),
                EPSILON);
    }

    // ---- error cases --------------------------------------------------

    @Test
    @DisplayName("zero denominator throws with a clear message")
    void zeroDenominatorThrows() {
        ArithmeticException ex = assertThrows(ArithmeticException.class,
                () -> AirDistanceCalculator.remainingDistance(100, 1, 0));
        assertEquals("Denominator cannot be zero.", ex.getMessage());
    }

    @Test
    @DisplayName("zero numerator throws — 0/denominator is undefined as a fraction flown")
    void numeratorZeroThrows() {
        assertThrows(ArithmeticException.class,
                () -> AirDistanceCalculator.remainingDistance(100, 0, 1));
    }
}