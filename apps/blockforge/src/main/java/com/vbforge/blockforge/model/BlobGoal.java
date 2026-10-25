package com.vbforge.blockforge.model;

/**
 * Score = size (in unit cells) of the largest 4-neighbour-connected component of the target
 * color. Diagonal adjacency does not count.
 */
public record BlobGoal(BlockColor targetColor) implements Goal {

    public BlobGoal {
        if (targetColor == null) {
            throw new IllegalArgumentException("targetColor must not be null");
        }
    }

    @Override
    public int score(Block board) {
        BlockColor[][] grid = board.flatten();
        int n = grid.length;
        boolean[][] visited = new boolean[n][n];
        int best = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (!visited[i][j] && grid[i][j] == targetColor) {
                    int size = blobSize(i, j, grid, visited);
                    if (size > best) {
                        best = size;
                    }
                }
            }
        }
        return best;
    }

    /** Flood fill returning the count of unvisited target-color cells connected to (i, j). */
    private int blobSize(int i, int j, BlockColor[][] grid, boolean[][] visited) {
        int n = grid.length;
        if (i < 0 || j < 0 || i >= n || j >= n) return 0;
        if (visited[i][j] || grid[i][j] != targetColor) return 0;
        visited[i][j] = true;
        return 1
                + blobSize(i + 1, j, grid, visited)
                + blobSize(i - 1, j, grid, visited)
                + blobSize(i, j + 1, grid, visited)
                + blobSize(i, j - 1, grid, visited);
    }

    @Override
    public String description() {
        return "Create the largest connected blob of " + targetColor.name()
                + " blocks, anywhere within the board";
    }
}