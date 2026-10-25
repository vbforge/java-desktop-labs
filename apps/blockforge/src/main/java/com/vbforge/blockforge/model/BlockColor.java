package com.vbforge.blockforge.model;

/**
 * The four Mondrian colors used on the board.
 *
 * <p>Order matters: {@link #values()} is indexed by {@code rng.nextInt(4)} during generation, and
 * the assignment's reference boards assume the palette order RED, GREEN, BLUE, YELLOW.
 */
public enum BlockColor {
    RED("#cc0000"),
    GREEN("#4c9900"),
    BLUE("#0066cc"),
    YELLOW("#fcd12a");

    private final String hex;

    BlockColor(String hex) {
        this.hex = hex;
    }

    public String hex() {
        return hex;
    }
}