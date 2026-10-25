package com.vbforge.blockforge.model;

/**
 * Builds a Block tree from a 2D color grid, for deterministic scoring tests.
 * The grid must be square with side length a power of two (1, 2, 4, 8, ...).
 * Homogeneous regions collapse to leaves at whatever level they first become uniform.
 */
final class TestBlocks {

    private TestBlocks() {}

    static Block fromGrid(BlockColor[][] grid) {
        int n = grid.length;
        if (n == 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Grid side length must be a power of two, got " + n);
        }
        for (BlockColor[] row : grid) {
            if (row.length != n) {
                throw new IllegalArgumentException("Grid must be square");
            }
        }
        int maxDepth = Integer.numberOfTrailingZeros(n); // log2(n)
        return build(grid, 0, 0, n, 0, maxDepth);
    }

    private static Block build(BlockColor[][] grid, int x, int y, int size, int level, int maxDepth) {
        BlockColor first = grid[y][x];
        boolean allSame = true;
        outer:
        for (int i = y; i < y + size; i++) {
            for (int j = x; j < x + size; j++) {
                if (grid[i][j] != first) {
                    allSame = false;
                    break outer;
                }
            }
        }
        if (allSame) {
            return Block.leaf(x, y, size, level, maxDepth, first);
        }
        int half = size / 2;
        Block[] children = new Block[4];
        children[Block.UR] = build(grid, x + half, y,        half, level + 1, maxDepth);
        children[Block.UL] = build(grid, x,        y,        half, level + 1, maxDepth);
        children[Block.LL] = build(grid, x,        y + half, half, level + 1, maxDepth);
        children[Block.LR] = build(grid, x + half, y + half, half, level + 1, maxDepth);
        return Block.internal(x, y, size, level, maxDepth, children);
    }
}