package com.vbforge.kanadrill;

/**
 * Immutable per-character statistics.
 *
 * Kept as a record so updates produce a new instance rather than mutating
 * shared state — makes persistence and any future concurrency trivially safe.
 */
public record KanaStat(int attempts, int correct, long lastSeenMs) {

    public static KanaStat empty() {
        return new KanaStat(0, 0, 0L);
    }

    public KanaStat withAnswer(boolean wasCorrect, long now) {
        return new KanaStat(
                attempts + 1,
                correct + (wasCorrect ? 1 : 0),
                now);
    }

    public double accuracy() {
        return attempts == 0 ? 0.0 : (double) correct / attempts;
    }

    /** How many times this character has been answered wrong. */
    public int wrong() {
        return attempts - correct;
    }
}