package com.vbforge.blockforge.model;

/**
 * Score = count of target-color unit cells on any edge, with corner cells counted twice.
 */
public record PerimeterGoal(BlockColor targetColor) implements Goal {

    public PerimeterGoal {
        if (targetColor == null) {
            throw new IllegalArgumentException("targetColor must not be null");
        }
    }

    @Override
    public int score(Block board) {
        BlockColor[][] grid = board.flatten();
        int n = grid.length;
        int total = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (grid[i][j] != targetColor) continue;
                boolean onTop    = i == 0;
                boolean onBottom = i == n - 1;
                boolean onLeft   = j == 0;
                boolean onRight  = j == n - 1;
                if (!(onTop || onBottom || onLeft || onRight)) continue;
                boolean isCorner = (onTop || onBottom) && (onLeft || onRight);
                total += isCorner ? 2 : 1;
            }
        }
        return total;
    }

    @Override
    public String description() {
        return "Place the highest number of " + targetColor.name()
                + " unit cells along the outer perimeter. Corner cells count twice.";
    }
}