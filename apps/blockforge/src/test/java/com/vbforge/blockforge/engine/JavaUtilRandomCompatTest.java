package com.vbforge.blockforge.engine;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class JavaUtilRandomCompatTest {

    @Test
    void matchesJavaUtilRandomForNextInt() {
        for (long seed : new long[]{ 0L, 1L, 2L, 4L, 42L, 123L, -1L, Long.MIN_VALUE }) {
            Random std = new Random(seed);
            JavaUtilRandomCompat ours = new JavaUtilRandomCompat(seed);
            for (int i = 0; i < 100; i++) {
                assertEquals(std.nextInt(), ours.nextInt(),
                        "seed " + seed + " step " + i);
            }
        }
    }

    @Test
    void matchesJavaUtilRandomForNextDouble() {
        for (long seed : new long[]{ 0L, 1L, 2L, 4L, 42L, 123L }) {
            Random std = new Random(seed);
            JavaUtilRandomCompat ours = new JavaUtilRandomCompat(seed);
            for (int i = 0; i < 100; i++) {
                assertEquals(std.nextDouble(), ours.nextDouble(), 0.0,
                        "seed " + seed + " step " + i);
            }
        }
    }

    @Test
    void matchesJavaUtilRandomForNextIntBound() {
        int[] bounds = { 2, 3, 4, 5, 7, 100, 1000 };
        for (long seed : new long[]{ 2L, 4L, 123L }) {
            for (int bound : bounds) {
                Random std = new Random(seed);
                JavaUtilRandomCompat ours = new JavaUtilRandomCompat(seed);
                for (int i = 0; i < 200; i++) {
                    assertEquals(std.nextInt(bound), ours.nextInt(bound),
                            "seed " + seed + " bound " + bound + " step " + i);
                }
            }
        }
    }

    @Test
    void stateRoundTrips() {
        JavaUtilRandomCompat rng = new JavaUtilRandomCompat(123);
        rng.nextDouble();
        rng.nextInt(4);
        long saved = rng.state();

        int next = rng.nextInt();
        double dnext = rng.nextDouble();

        rng.setState(saved);
        assertEquals(next,  rng.nextInt());
        assertEquals(dnext, rng.nextDouble(), 0.0);
    }

    @Test
    void copyIsIndependent() {
        JavaUtilRandomCompat a = new JavaUtilRandomCompat(7);
        JavaUtilRandomCompat b = a.copy();
        for (int i = 0; i < 10; i++) {
            assertEquals(a.nextInt(), b.nextInt());
        }
        a.nextDouble();
        assertNotEquals(a.state(), b.state());
    }
}