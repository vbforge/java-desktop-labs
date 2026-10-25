package com.vbforge.blockforge.model;

import com.vbforge.blockforge.engine.JavaUtilRandomCompat;
import org.junit.jupiter.api.Test;

import static com.vbforge.blockforge.model.BlockColor.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlobGoalTest {

    private static Block grid(BlockColor[][] g) {
        return TestBlocks.fromGrid(g);
    }

    @Test
    void emptyGridScoresZero() {
        BlockColor[][] g = {
                { RED, RED,   RED,   RED   },
                { RED, RED,   RED,   RED   },
                { RED, RED,   RED,   RED   },
                { RED, RED,   RED,   RED   },
        };
        assertEquals(0, new BlobGoal(GREEN).score(grid(g)));
    }

    @Test
    void singleCellScoresOne() {
        BlockColor[][] g = {
                { GREEN, RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
                { RED,   RED,   RED,   RED   },
        };
        assertEquals(1, new BlobGoal(GREEN).score(grid(g)));
    }

    @Test
    void twoByTwoSolidScoresFour() {
        BlockColor[][] g = {
                { GREEN, GREEN, RED, RED },
                { GREEN, GREEN, RED, RED },
                { RED,   RED,   RED, RED },
                { RED,   RED,   RED, RED },
        };
        assertEquals(4, new BlobGoal(GREEN).score(grid(g)));
    }

    @Test
    void fullBoardScoresEveryCell() {
        BlockColor[][] g = {
                { GREEN, GREEN, GREEN, GREEN },
                { GREEN, GREEN, GREEN, GREEN },
                { GREEN, GREEN, GREEN, GREEN },
                { GREEN, GREEN, GREEN, GREEN },
        };
        assertEquals(16, new BlobGoal(GREEN).score(grid(g)));
    }

    @Test
    void checkerboardScoresOne() {
        BlockColor[][] g = {
                { GREEN, RED,   GREEN, RED   },
                { RED,   GREEN, RED,   GREEN },
                { GREEN, RED,   GREEN, RED   },
                { RED,   GREEN, RED,   GREEN },
        };
        // Each GREEN cell is isolated orthogonally.
        assertEquals(1, new BlobGoal(GREEN).score(grid(g)));
    }

    @Test
    void diagonalDoesNotConnect() {
        BlockColor[][] g = {
                { GREEN, RED,   RED,   RED   },
                { RED,   GREEN, RED,   RED   },
                { RED,   RED,   GREEN, RED   },
                { RED,   RED,   RED,   GREEN },
        };
        // Diagonal touch does not count as connected.
        assertEquals(1, new BlobGoal(GREEN).score(grid(g)));
    }

    @Test
    void lShapeScoresThree() {
        BlockColor[][] g = {
                { GREEN, GREEN, RED, RED },
                { GREEN, RED,   RED, RED },
                { RED,   RED,   RED, RED },
                { RED,   RED,   RED, RED },
        };
        assertEquals(3, new BlobGoal(GREEN).score(grid(g)));
    }

    @Test
    void descriptionMentionsTargetColor() {
        String d = new BlobGoal(GREEN).description();
        assertTrue(d.contains("GREEN"));
        assertTrue(d.toLowerCase().contains("blob"));
    }

    // ------------------------------------------------------------------ reference (from PDF)

    @Test
    void referenceSeed123Depth4Green() {
        // Assignment PDF, page 15: "with maximum depth of 4 ... using the seed 123 ...
        // the score for the blob goal would be 65" for GREEN.
        Block b = new Block(0, 4, new JavaUtilRandomCompat(123));
        b.updateSizeAndPosition(16, 0, 0);
        assertEquals(65, new BlobGoal(GREEN).score(b),
                "Blob GREEN score for seed 123, maxDepth 4");
    }
}