package com.vbforge.blockforge.model;

import com.vbforge.blockforge.engine.JavaUtilRandomCompat;
import org.junit.jupiter.api.Test;

import static com.vbforge.blockforge.model.BlockColor.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerimeterGoalTest {

    private static Block grid(BlockColor[][] g) {
        return TestBlocks.fromGrid(g);
    }

    @Test
    void emptyPerimeterScoresZero() {
        BlockColor[][] g = {
                { RED, RED, RED, RED },
                { RED, RED, RED, RED },
                { RED, RED, RED, RED },
                { RED, RED, RED, RED },
        };
        assertEquals(0, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void fullBoardScoresFourTimesSide() {
        // n = 4 → formula 4n = 16 (4 corners × 2 + 8 non-corner edge cells × 1).
        BlockColor[][] g = {
                { GREEN, GREEN, GREEN, GREEN },
                { GREEN, GREEN, GREEN, GREEN },
                { GREEN, GREEN, GREEN, GREEN },
                { GREEN, GREEN, GREEN, GREEN },
        };
        assertEquals(16, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void fullBoardEightByEightScoresThirtyTwo() {
        // n = 8 → 4n = 32.
        BlockColor[][] g = new BlockColor[8][8];
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                g[i][j] = GREEN;
        assertEquals(32, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void singleCornerCountsTwice() {
        BlockColor[][] g = {
                { GREEN, RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
        };
        assertEquals(2, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void singleNonCornerEdgeCountsOnce() {
        BlockColor[][] g = {
                { RED,   GREEN, RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
        };
        assertEquals(1, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void interiorCellCountsZero() {
        BlockColor[][] g = {
                { RED, RED, RED, RED },
                { RED, GREEN, RED, RED },
                { RED, RED, RED, RED },
                { RED, RED, RED, RED },
        };
        assertEquals(0, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void allFourCornersScoreEight() {
        BlockColor[][] g = {
                { GREEN, RED,   RED,   GREEN },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { GREEN, RED,   RED,   GREEN },
        };
        assertEquals(8, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void fullTopRowScoresSix() {
        // 4 cells on top row; 2 are corners (×2) and 2 are not (×1) → 4 + 2 = 6.
        BlockColor[][] g = {
                { GREEN, GREEN, GREEN, GREEN },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
        };
        assertEquals(6, new PerimeterGoal(GREEN).score(grid(g)));
    }

    @Test
    void descriptionMentionsTargetColor() {
        String d = new PerimeterGoal(RED).description();
        assertTrue(d.contains("RED"));
        assertTrue(d.toLowerCase().contains("perimeter"));
    }

    // ------------------------------------------------------------------ reference (from PDF)

    @Test
    void referenceSeed123Depth4Green() {
        // Assignment PDF, page 15: "with maximum depth of 4 ... using the seed 123 ...
        // the score for the perimeter goal would be 24" for GREEN.
        Block b = new Block(0, 4, new JavaUtilRandomCompat(123));
        b.updateSizeAndPosition(16, 0, 0);
        assertEquals(24, new PerimeterGoal(GREEN).score(b),
                "Perimeter GREEN score for seed 123, maxDepth 4");
    }
}