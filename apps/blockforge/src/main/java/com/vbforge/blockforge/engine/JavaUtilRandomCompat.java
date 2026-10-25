package com.vbforge.blockforge.engine;

/**
 * A {@link java.util.Random}-compatible generator whose entire state is a single {@code long}
 * (the 48-bit LCG state, held in the low 48 bits).
 *
 * <p>Chosen over {@link java.util.Random} directly because:
 * <ul>
 *   <li>Byte-for-byte reproducibility of the assignment's boards for a given seed.</li>
 *   <li>Full time-travel: a snapshot is just a {@code long}, so undo/redo can restore the exact
 *       RNG position.</li>
 * </ul>
 *
 * <p>For a given seed, the sequence returned by {@link #nextInt()}, {@link #nextInt(int)} and
 * {@link #nextDouble()} is identical to {@link java.util.Random}. The algorithm is the classic
 * {@code java.util.Random} LCG: {@code state = state * 0x5DEECE66D + 0xB}, masked to 48 bits.
 */
public final class JavaUtilRandomCompat {

    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND     = 0xBL;
    private static final long MASK       = (1L << 48) - 1;

    private long state;

    /** Equivalent to {@code new java.util.Random(seed)}. */
    public JavaUtilRandomCompat(long seed) {
        this.state = (seed ^ MULTIPLIER) & MASK;
    }

    /** Raw 48-bit state (low 48 bits meaningful). Round-trips through {@link #setState(long)}. */
    public long state() {
        return state;
    }

    /** Restores a state previously obtained via {@link #state()}. */
    public void setState(long state) {
        this.state = state & MASK;
    }

    /** Same contract as {@code java.util.Random.next(int)}. */
    protected int next(int bits) {
        state = (state * MULTIPLIER + ADDEND) & MASK;
        return (int) (state >>> (48 - bits));
    }

    /** Same contract as {@code java.util.Random.nextInt()}. */
    public int nextInt() {
        return next(32);
    }

    /** Same contract as {@code java.util.Random.nextInt(int)} — including the rejection loop. */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0) {
            // bound is a power of two
            r = (int) ((bound * (long) r) >> 31);
        } else {
            for (int u = r; u - (r = u % bound) + m < 0; u = next(31)) {
                // retry while u - r + m < 0
            }
        }
        return r;
    }

    /** Same contract as {@code java.util.Random.nextDouble()}. */
    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) * 0x1.0p-53;
    }

    /** Deep snapshot for undo/redo. */
    public JavaUtilRandomCompat copy() {
        JavaUtilRandomCompat c = new JavaUtilRandomCompat(0L);
        c.state = this.state;
        return c;
    }
}