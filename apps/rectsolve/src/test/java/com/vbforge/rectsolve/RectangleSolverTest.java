package com.vbforge.rectsolve;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RectangleSolverTest {

    @Test
    void solvesForBottomRightUnknown() {
        assertEquals(9.0, RectangleSolver.solve(8.0, 6.0, 12.0, null), 1e-9);
    }

    @Test
    void solvesForTopLeftUnknown() {
        assertEquals(8.0, RectangleSolver.solve(null, 6.0, 12.0, 9.0), 1e-9);
    }

    @Test
    void solvesForTopRightUnknown() {
        assertEquals(6.0, RectangleSolver.solve(8.0, null, 12.0, 9.0), 1e-9);
    }

    @Test
    void solvesForBottomLeftUnknown() {
        assertEquals(12.0, RectangleSolver.solve(8.0, 6.0, null, 9.0), 1e-9);
    }

    @Test
    void throwsWhenNoValueIsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> RectangleSolver.solve(8.0, 6.0, 12.0, 9.0));
    }

    @Test
    void throwsWhenMoreThanOneValueIsUnknown() {
        assertThrows(IllegalArgumentException.class, () -> RectangleSolver.solve(8.0, null, null, 9.0));
    }

    @Test
    void throwsOnDivisionByZero() {
        assertThrows(ArithmeticException.class, () -> RectangleSolver.solve(null, 6.0, 12.0, 0.0));
    }

    @Test
    void supportsNonIntegerResults() {
        assertEquals(15.0 / 7.0, RectangleSolver.solve(null, 5.0, 3.0, 7.0), 1e-9);
    }

    @Test
    void verifyReturnsConsistentForValidRectangle() {
        var result = RectangleSolver.verify(8.0, 6.0, 12.0, 9.0);
        assertTrue(result.consistent());
        assertEquals(72.0, result.leftProduct(), 1e-9);
        assertEquals(72.0, result.rightProduct(), 1e-9);
    }

    @Test
    void verifyReturnsInconsistentForInvalidRectangle() {
        var result = RectangleSolver.verify(10.0, 6.0, 12.0, 20.0);
        assertFalse(result.consistent());
    }
}