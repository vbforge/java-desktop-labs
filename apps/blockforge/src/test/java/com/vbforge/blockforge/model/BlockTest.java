package com.vbforge.blockforge.model;

import com.vbforge.blockforge.engine.JavaUtilRandomCompat;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class BlockTest {

    /** Builds a seeded board using unit-cell coordinates (root size = 2^maxDepth). */
    private static Block seeded(int maxDepth, long seed) {
        Block b = new Block(0, maxDepth, new JavaUtilRandomCompat(seed));
        b.updateSizeAndPosition(1 << maxDepth, 0, 0);
        return b;
    }

    @Test
    void generationTopLevelIsAlwaysInternal() {
        for (long seed = 0; seed < 50; seed++) {
            Block b = seeded(3, seed);
            assertFalse(b.isLeaf(), "level-0 must be internal (seed " + seed + ")");
            assertEquals(4, b.children().length);
        }
    }

    @Test
    void generationRespectsInvariants() {
        for (long seed = 0; seed < 100; seed++) {
            assertInvariants(seeded(4, seed));
        }
    }

    private static void assertInvariants(Block b) {
        if (b.isLeaf()) {
            assertNotNull(b.color(), "leaf must have a color");
            assertEquals(0, b.children().length);
        } else {
            assertNull(b.color(), "internal node must have null color");
            assertEquals(4, b.children().length);
            int half = b.size() / 2;
            int cl = b.level() + 1;
            Block ur = b.children()[Block.UR];
            Block ul = b.children()[Block.UL];
            Block ll = b.children()[Block.LL];
            Block lr = b.children()[Block.LR];
            assertEquals(cl, ur.level());
            assertEquals(b.x() + half, ur.x());
            assertEquals(b.y(),        ur.y());
            assertEquals(b.x(),        ul.x());
            assertEquals(b.y(),        ul.y());
            assertEquals(b.x(),        ll.x());
            assertEquals(b.y() + half, ll.y());
            assertEquals(b.x() + half, lr.x());
            assertEquals(b.y() + half, lr.y());
            for (Block c : b.children()) {
                assertEquals(half, c.size());
                assertEquals(b.maxDepth(), c.maxDepth());
                assertInvariants(c);
            }
        }
    }

    @Test
    void updateSizeAndPositionRejectsInvalidSizes() {
        Block b = seeded(3, 1);
        assertThrows(IllegalArgumentException.class, () -> b.updateSizeAndPosition(-8, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> b.updateSizeAndPosition(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> b.updateSizeAndPosition(7, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> b.updateSizeAndPosition(1, 0, 0));
        assertDoesNotThrow(() -> b.updateSizeAndPosition(8, 0, 0));
        // Also valid: multiples of 2^maxDepth
        assertDoesNotThrow(() -> b.updateSizeAndPosition(16, 0, 0));
        assertDoesNotThrow(() -> b.updateSizeAndPosition(800, 0, 0));
    }

    @Test
    void referenceBoardSeed2Depth2MatchesAssignment() {
        Block b = new Block(0, 2, new JavaUtilRandomCompat(2));
        b.updateSizeAndPosition(4, 0, 0);
        BlockColor R = BlockColor.RED, G = BlockColor.GREEN,
                   Y = BlockColor.YELLOW, B = BlockColor.BLUE;
        BlockColor[][] expected = {
                { R, R, G, G },
                { R, R, G, G },
                { Y, Y, R, B },
                { Y, Y, Y, B },
        };
        assertArrayEquals(expected, b.flatten());
    }

    @Test
    void flattenCoversEveryCell() {
        Block b = seeded(3, 42);
        BlockColor[][] grid = b.flatten();
        assertEquals(8, grid.length);
        assertEquals(8, grid[0].length);
        for (BlockColor[] row : grid) {
            for (BlockColor c : row) assertNotNull(c);
        }
    }

    @Test
    void getSelectedBlockReturnsCorrectLevel() {
        // Seed 4, maxDepth 3: the point (3,3) sits in a region that subdivides all the way to
        // level 3 (verified against the reference grid). Using a point that is a solid leaf
        // would be wrong: per spec, the method returns the closest available level, not an error.
        Block b = seeded(3, 4);
        Block l1 = b.getSelectedBlock(3, 3, 1);
        Block l2 = b.getSelectedBlock(3, 3, 2);
        Block l3 = b.getSelectedBlock(3, 3, 3);
        assertEquals(1, l1.level());
        assertEquals(2, l2.level());
        assertEquals(3, l3.level());
        assertTrue(contains(l1, l2));
        assertTrue(contains(l2, l3));
    }

    private static boolean contains(Block outer, Block inner) {
        return inner.x() >= outer.x()
                && inner.y() >= outer.y()
                && inner.x() + inner.size() <= outer.x() + outer.size()
                && inner.y() + inner.size() <= outer.y() + outer.size();
    }

    @Test
    void getSelectedBlockOutsideReturnsNull() {
        Block b = seeded(3, 4);
        assertNull(b.getSelectedBlock(-1, 0, 2));
        assertNull(b.getSelectedBlock(0, -1, 2));
        assertNull(b.getSelectedBlock(8, 0, 2));
        assertNull(b.getSelectedBlock(0, 8, 2));
    }

    @Test
    void getSelectedBlockRejectsBadLevel() {
        Block b = seeded(3, 4);
        assertThrows(IllegalArgumentException.class, () -> b.getSelectedBlock(0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> b.getSelectedBlock(0, 0, 4));
    }

    @Test
    void rotateClockwiseThenCounterIsIdentity() {
        Block b = seeded(3, 4);
        BlockColor[][] before = b.flatten();
        b.rotate(1);
        assertNotEquals(deep(before), deep(b.flatten()),
                "rotate CW should change the board");
        b.rotate(0);
        assertArrayEquals(before, b.flatten(),
                "rotate CW then CCW should be identity");
    }

    @Test
    void reflectTwiceIsIdentity() {
        Block b = seeded(3, 4);
        BlockColor[][] before = b.flatten();

        b.reflect(0);
        assertNotEquals(deep(before), deep(b.flatten()));
        b.reflect(0);
        assertArrayEquals(before, b.flatten(), "reflect X twice should be identity");

        b.reflect(1);
        assertNotEquals(deep(before), deep(b.flatten()));
        b.reflect(1);
        assertArrayEquals(before, b.flatten(), "reflect Y twice should be identity");
    }

    @Test
    void reflectAndRotateRejectBadDirection() {
        Block b = seeded(3, 4);
        assertThrows(IllegalArgumentException.class, () -> b.reflect(2));
        assertThrows(IllegalArgumentException.class, () -> b.reflect(-1));
        assertThrows(IllegalArgumentException.class, () -> b.rotate(2));
        assertThrows(IllegalArgumentException.class, () -> b.rotate(-1));
    }

    @Test
    void smashTopLevelAndMaxDepthReturnFalse() {
        // Construct a leaf explicitly at maxDepth so this test doesn't depend on generation.
        Block leafAtMax = Block.leaf(0, 0, 1, 3, 3, BlockColor.RED);
        JavaUtilRandomCompat rng = new JavaUtilRandomCompat(0);
        assertFalse(leafAtMax.smash(rng), "maxDepth block cannot be smashed");

        // Top-level block is level 0 by construction.
        Block top = seeded(3, 4);
        assertFalse(top.smash(rng), "level 0 cannot be smashed");
    }

    @Test
    void smashPreservesInvariants() {
        Block b = seeded(3, 4);
        JavaUtilRandomCompat rng = new JavaUtilRandomCompat(0);
        Block target = b.getSelectedBlock(0, 0, 1);
        assertTrue(target.smash(rng));
        assertInvariants(b);
        assertEquals(4, target.children().length);
        assertEquals(2, target.children()[0].level());
    }

    @Test
    void copiesAreIndependent() {
        Block b = seeded(3, 4);
        Block c = b.copy();
        c.rotate(1);
        assertNotEquals(deep(b.flatten()), deep(c.flatten()));
    }

    private static String deep(BlockColor[][] grid) {
        return Arrays.deepToString(grid);
    }
}