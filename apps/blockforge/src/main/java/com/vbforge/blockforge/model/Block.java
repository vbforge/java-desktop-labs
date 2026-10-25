package com.vbforge.blockforge.model;

import com.vbforge.blockforge.engine.JavaUtilRandomCompat;

/**
 * A node in the quad-tree board.
 *
 * <p>Mutations ({@link #rotate(int)}, {@link #reflect(int)}, {@link #smash(JavaUtilRandomCompat)},
 * {@link #updateSizeAndPosition}) operate in place. Undo/redo is handled at the game-state level
 * by snapshots, not by immutability.
 *
 * <p>Coordinate units: the {@code x}, {@code y}, {@code size} fields are in whatever unit the
 * caller chooses via {@link #updateSizeAndPosition}. In the game they are pixels; in unit tests
 * they are unit-cell indices. {@link #flatten()} produces a {@code 2^maxDepth × 2^maxDepth} grid
 * regardless of the pixel scale, by dividing positions by {@code size / 2^maxDepth} of the root.
 */
public final class Block {

    /** Child indices in the order the assignment uses everywhere: UR, UL, LL, LR. */
    public static final int UR = 0;
    public static final int UL = 1;
    public static final int LL = 2;
    public static final int LR = 3;

    private int x;
    private int y;
    private int size;
    private int level;
    private int maxDepth;
    private BlockColor color;   // null iff this is an internal node
    private Block[] children;   // length 0 or 4

    // ----------------------------------------------------------------- constructors

    /**
     * Creates a random block using the assignment's subdivision policy.
     *
     * <p>At {@code lvl == maxD}, no {@code nextDouble()} is consumed (subdivision is impossible
     * here), only a {@code nextInt(4)} for the color — matching the assignment's
     * "generate the random number if and only if it is possible to subdivide" clause.
     *
     * <p>At {@code lvl < maxD}, one {@code nextDouble()} is consumed. Subdivision happens iff
     * that value is {@code < exp(-0.25 * lvl)}. At level 0 this is always true ({@code exp(0) == 1.0}).
     */
    public Block(int lvl, int maxD, JavaUtilRandomCompat rng) {
        this.level = lvl;
        this.maxDepth = maxD;
        this.color = null;
        this.children = new Block[0];

        // At maxDepth, must be a leaf; no nextDouble() consumption.
        if (lvl >= maxD) {
            this.color = randomColor(rng);
            return;
        }

        // lvl < maxD: consume one nextDouble() to decide subdivision.
        boolean subdivide = rng.nextDouble() < Math.exp(-0.25 * lvl);

        // Level 0 always subdivides by rule. Defensive: the exp(0) == 1.0 formula makes this
        // always true anyway, but the invariant "level-0 is internal" is a game rule.
        if (lvl == 0) {
            subdivide = true;
        }

        if (subdivide) {
            this.children = new Block[4];
            for (int i = 0; i < 4; i++) {
                this.children[i] = new Block(lvl + 1, maxD, rng);
            }
        } else {
            this.color = randomColor(rng);
        }
    }

    /** Factory: explicit leaf. */
    public static Block leaf(int x, int y, int size, int level, int maxDepth, BlockColor color) {
        Block b = new Block(level, maxDepth);
        b.x = x;
        b.y = y;
        b.size = size;
        b.color = color;
        return b;
    }

    /** Factory: explicit internal node. */
    public static Block internal(int x, int y, int size, int level, int maxDepth, Block[] four) {
        if (four.length != 4) {
            throw new IllegalArgumentException("An internal block must have exactly 4 children");
        }
        Block b = new Block(level, maxDepth);
        b.x = x;
        b.y = y;
        b.size = size;
        b.children = four.clone();
        return b;
    }

    /** Private: empty shell used by factories. */
    private Block(int level, int maxDepth) {
        this.level = level;
        this.maxDepth = maxDepth;
        this.children = new Block[0];
    }

    private static BlockColor randomColor(JavaUtilRandomCompat rng) {
        BlockColor[] palette = BlockColor.values();
        return palette[rng.nextInt(palette.length)];
    }

    // ----------------------------------------------------------------- snapshots

    /** Deep copy, used for undo/redo snapshots. */
    public Block copy() {
        Block c = new Block(this.level, this.maxDepth);
        c.x = this.x;
        c.y = this.y;
        c.size = this.size;
        c.color = this.color;
        if (this.children.length == 0) {
            c.children = new Block[0];
        } else {
            c.children = new Block[this.children.length];
            for (int i = 0; i < children.length; i++) {
                c.children[i] = children[i].copy();
            }
        }
        return c;
    }

    // ----------------------------------------------------------------- accessors

    public int x() { return x; }
    public int y() { return y; }
    public int size() { return size; }
    public int level() { return level; }
    public int maxDepth() { return maxDepth; }
    public BlockColor color() { return color; }
    public boolean isLeaf() { return children.length == 0; }
    public Block[] children() { return children; }

    // ----------------------------------------------------------------- layout

    /**
     * Sets this block's size and position and pushes the same down the tree.
     *
     * <p>Valid sizes are positive integers divisible by {@code 2^(maxDepth - level)} — i.e., the
     * size can be halved {@code maxDepth - level} times without remainder.
     *
     * @throws IllegalArgumentException if {@code size} is not positive or not divisible as above
     */
    public void updateSizeAndPosition(int size, int xCoord, int yCoord) {
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive, got " + size);
        }
        int remainingLevels = this.maxDepth - this.level;
        int divisor = remainingLevels >= 0 ? (1 << remainingLevels) : 1;
        if (size % divisor != 0) {
            throw new IllegalArgumentException(
                    "Size " + size + " must be divisible by " + divisor
                            + " (2^(maxDepth - level)) for level " + this.level
                            + " of a maxDepth-" + this.maxDepth + " board");
        }
        applyLayout(size, xCoord, yCoord, this.level);
    }

    private void applyLayout(int newSize, int newX, int newY, int newLevel) {
        this.x = newX;
        this.y = newY;
        this.size = newSize;
        this.level = newLevel;
        if (children.length == 0) return;
        int half = newSize / 2;
        int childLevel = newLevel + 1;
        children[UR].applyLayout(half, newX + half, newY,        childLevel);
        children[UL].applyLayout(half, newX,        newY,        childLevel);
        children[LL].applyLayout(half, newX,        newY + half, childLevel);
        children[LR].applyLayout(half, newX + half, newY + half, childLevel);
    }

    // ----------------------------------------------------------------- selection

    /**
     * Returns the block at the given coordinate, at the given level.
     *
     * @throws IllegalArgumentException if {@code lvl} is outside {@code [this.level, this.maxDepth]}
     * @return {@code null} if {@code (x, y)} is outside this block's bounds
     */
    public Block getSelectedBlock(int x, int y, int lvl) {
        if (lvl < this.level || lvl > this.maxDepth) {
            throw new IllegalArgumentException(
                    "Level " + lvl + " out of range [" + this.level + ", " + this.maxDepth + "]");
        }
        if (x < this.x || x >= this.x + this.size || y < this.y || y >= this.y + this.size) {
            return null;
        }
        if (this.children.length == 0 || this.level == lvl) {
            return this;
        }
        for (Block child : children) {
            Block hit = child.getSelectedBlock(x, y, lvl);
            if (hit != null) return hit;
        }
        return null;
    }

    // ----------------------------------------------------------------- transforms

    /**
     * Reflects this block and all its descendants.
     *
     * @param direction {@code 0} = over the horizontal x-axis, {@code 1} = over the vertical y-axis
     */
    public void reflect(int direction) {
        if (direction != 0 && direction != 1) {
            throw new IllegalArgumentException("Reflect direction must be 0 or 1, got " + direction);
        }
        if (children.length == 0) return;
        Block ur = children[UR], ul = children[UL], ll = children[LL], lr = children[LR];
        if (direction == 0) {
            children[UR] = lr;
            children[UL] = ll;
            children[LL] = ul;
            children[LR] = ur;
        } else {
            children[UR] = ul;
            children[UL] = ur;
            children[LL] = lr;
            children[LR] = ll;
        }
        for (Block child : children) child.reflect(direction);
        // Re-establish invariant: each child is at its UR/UL/LL/LR position.
        applyLayout(this.size, this.x, this.y, this.level);
    }

    /**
     * Rotates this block and all its descendants.
     *
     * @param direction {@code 1} = clockwise, {@code 0} = counter-clockwise
     */
    public void rotate(int direction) {
        if (direction != 0 && direction != 1) {
            throw new IllegalArgumentException("Rotate direction must be 0 or 1, got " + direction);
        }
        if (children.length == 0) return;
        Block ur = children[UR], ul = children[UL], ll = children[LL], lr = children[LR];
        if (direction == 1) {
            children[UR] = ul;
            children[UL] = ll;
            children[LL] = lr;
            children[LR] = ur;
        } else {
            children[UR] = lr;
            children[UL] = ur;
            children[LL] = ul;
            children[LR] = ll;
        }
        for (Block child : children) child.rotate(direction);
        // Re-establish invariant: each child is at its UR/UL/LL/LR position.
        applyLayout(this.size, this.x, this.y, this.level);
    }

    /**
     * Replaces this block's children with four freshly generated ones.
     *
     * @return {@code true} if the block was smashed, {@code false} if it is top-level or at
     *         {@code maxDepth}
     */
    public boolean smash(JavaUtilRandomCompat rng) {
        if (this.level == 0 || this.level >= this.maxDepth) return false;
        Block[] fresh = new Block[4];
        for (int i = 0; i < 4; i++) {
            fresh[i] = new Block(this.level + 1, this.maxDepth, rng);
        }
        this.children = fresh;
        this.color = null;
        int half = this.size / 2;
        if (half > 0) {
            int cl = this.level + 1;
            fresh[UR].applyLayout(half, this.x + half, this.y,        cl);
            fresh[UL].applyLayout(half, this.x,        this.y,        cl);
            fresh[LL].applyLayout(half, this.x,        this.y + half, cl);
            fresh[LR].applyLayout(half, this.x + half, this.y + half, cl);
        }
        return true;
    }

    // ----------------------------------------------------------------- flatten

    /**
     * Returns a {@code 2^maxDepth × 2^maxDepth} grid of unit-cell colors.
     *
     * <p>Intended to be called on the top-level block. Cell index {@code (i, j)} corresponds to
     * pixel region starting at {@code (j * unitCellSize, i * unitCellSize)} of the root.
     */
    public BlockColor[][] flatten() {
        int n = 1 << this.maxDepth;
        int unitCellSize = this.size > 0 ? Math.max(1, this.size / n) : 1;
        BlockColor[][] grid = new BlockColor[n][n];
        fillGrid(grid, unitCellSize);
        return grid;
    }

    private void fillGrid(BlockColor[][] grid, int unitCellSize) {
        if (children.length == 0) {
            int x0 = this.x / unitCellSize;
            int y0 = this.y / unitCellSize;
            int span = this.size / unitCellSize;
            for (int i = y0; i < y0 + span; i++) {
                for (int j = x0; j < x0 + span; j++) {
                    grid[i][j] = this.color;
                }
            }
            return;
        }
        for (Block child : children) {
            child.fillGrid(grid, unitCellSize);
        }
    }

    // ----------------------------------------------------------------- debug

    public void printBlock() {
        printIndented(0);
    }

    private void printIndented(int indentation) {
        String indent = "\t".repeat(indentation);
        if (children.length == 0) {
            System.out.println(indent + color + ", " + this);
        } else {
            System.out.println(indent + this);
            for (Block b : children) b.printIndented(indentation + 1);
        }
    }

    @Override
    public String toString() {
        return String.format("pos=(%d,%d), size=%d, level=%d", x, y, size, level);
    }
}