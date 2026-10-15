package com.vbforge.kanadrill;

import java.util.List;
import java.util.Random;

/**
 * Chooses the next character to present.
 *
 * The rule: characters you get wrong come up more often; characters you've
 * never seen come up a lot; characters you reliably know fade into the
 * background. This is the simplest useful spaced-repetition policy —
 * weighted random selection, no scheduling queue required.
 */
public final class QuizEngine {

    /** Never repeat the currently-shown card back-to-back. */
    private final Random rng = new Random();
    private Kana current;

    public Kana current() {
        return current;
    }

    public Kana next(List<Kana> pool, StatsStore stats) {
        if (pool.isEmpty()) return null;

        double[] weights = new double[pool.size()];
        double total = 0.0;

        for (int i = 0; i < pool.size(); i++) {
            Kana k = pool.get(i);
            // Skip the one we're currently showing.
            if (k.equals(current)) {
                weights[i] = 0.0;
                continue;
            }
            weights[i] = weightOf(stats.stat(k.character()));
            total += weights[i];
        }

        // Degenerate case: pool has one item, or everything zeroed out.
        // Fall back to a uniform pick so we never get stuck.
        if (total <= 0.0) {
            Kana pick = pool.get(rng.nextInt(pool.size()));
            current = pick;
            return pick;
        }

        double r = rng.nextDouble() * total;
        double acc = 0.0;
        for (int i = 0; i < pool.size(); i++) {
            acc += weights[i];
            if (r < acc) {
                current = pool.get(i);
                return current;
            }
        }
        // Floating-point edge: fall through to the last non-zero item.
        current = pool.get(pool.size() - 1);
        return current;
    }

    /**
     * How strongly to prefer a character.
     *
     *   Never seen       → 2.0   (introduce new material)
     *   Accuracy 0%      → 4.0   (drill it)
     *   Accuracy 100%    → 1.0   (occasional reinforcement)
     *
     * No statistics on a character ever drops it out of the pool entirely —
     * you always want a chance of seeing something you've mastered, to
     * catch the day you forget it.
     */
    private static double weightOf(KanaStat s) {
        if (s.attempts() == 0) return 2.0;
        return 1.0 + (1.0 - s.accuracy()) * 3.0;
    }

    public boolean check(String input) {
        if (current == null) return false;
        return current.accepted().contains(input.trim().toLowerCase());
    }
}