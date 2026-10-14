package com.vbforge.pathlab;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * BFS, Dijkstra and A* on a uniform-cost 4-connected grid.
 *
 * On a grid where every step costs 1, all three find an optimal path.
 * They differ only in the order they explore — which is exactly what
 * the visualizer is showing.
 */
public final class Algorithms {

    public static final String BFS = "BFS";
    public static final String DIJKSTRA = "Dijkstra";
    public static final String A = "A*";

    /** A single search's output: the order cells were settled, and the final path. */
    public record SearchResult(List<int[]> visitedOrder, List<int[]> path, int visitedCount) {
        public static SearchResult empty() {
            return new SearchResult(List.of(), List.of(), 0);
        }
    }

//    public interface Search {
//        String name();
//        SearchResult run(Grid grid);
//    }

    public record Search(String name, boolean byCost, boolean heuristic) {

        public static Search bfs()      { return new Search(BFS,      false, false); }
        public static Search dijkstra() { return new Search(DIJKSTRA, true,  false); }
        public static Search astar()    { return new Search(A,       true,  true);  }

        public SearchResult run(Grid g) {
            return Algorithms.search(g, byCost, heuristic);
        }

        @Override public String toString() { return name; }
    }

    private static final int[][] DIRS = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };

//    public static Search bfs() {
//        return named(BFS, false, false);
//    }
//
//    public static Search dijkstra() {
//        return named(DIJKSTRA, true, false);
//    }
//
//    public static Search astar() {
//        return named(A, true, true);
//    }

//    private static Search named(String name, boolean byCost, boolean heuristic) {
//        return new Search() {
//            @Override public String name() { return name; }
//            @Override public SearchResult run(Grid g) { return search(g, byCost, heuristic); }
//        };
//    }

    static SearchResult search(Grid g, boolean byCost, boolean heuristic) {
        final int rows = g.rows(), cols = g.cols();
        final int sr = g.startRow(), sc = g.startCol();
        final int er = g.endRow(),   ec = g.endCol();

        int[][] dist    = new int[rows][cols];
        int[][] prevRow = new int[rows][cols];
        int[][] prevCol = new int[rows][cols];
        boolean[][] settled = new boolean[rows][cols];

        for (int[] row : dist)    Arrays.fill(row, Integer.MAX_VALUE);
        for (int[] row : prevRow) Arrays.fill(row, -1);
        for (int[] row : prevCol) Arrays.fill(row, -1);

        List<int[]> visitedOrder = new ArrayList<>();

        Queue<int[]> open;
        if (heuristic) {
            open = new PriorityQueue<>(Comparator.comparingInt(
                a -> dist[a[0]][a[1]] + manhattan(a[0], a[1], er, ec)));
        } else if (byCost) {
            open = new PriorityQueue<>(Comparator.comparingInt(a -> dist[a[0]][a[1]]));
        } else {
            open = new ArrayDeque<>();
        }

        dist[sr][sc] = 0;
        open.add(new int[]{sr, sc});

        while (!open.isEmpty()) {
            int[] cur = open.poll();
            int r = cur[0], c = cur[1];
            if (settled[r][c]) continue;   // stale queue entry
            settled[r][c] = true;
            visitedOrder.add(new int[]{r, c});

            if (r == er && c == ec) break;

            for (int[] d : DIRS) {
                int nr = r + d[0], nc = c + d[1];
                if (!g.inBounds(nr, nc) || g.isWall(nr, nc) || settled[nr][nc]) continue;

                int nd = dist[r][c] + 1;
                if (nd < dist[nr][nc]) {
                    dist[nr][nc]    = nd;
                    prevRow[nr][nc] = r;
                    prevCol[nr][nc] = c;
                    open.add(new int[]{nr, nc});
                }
            }
        }

        List<int[]> path = new ArrayList<>();
        if (settled[er][ec]) {
            int r = er, c = ec;
            while (r != -1 && c != -1) {
                path.add(new int[]{r, c});
                int pr = prevRow[r][c], pc = prevCol[r][c];
                r = pr; c = pc;
            }
            Collections.reverse(path);
        }

        return new SearchResult(visitedOrder, path, visitedOrder.size());
    }

    private static int manhattan(int r1, int c1, int r2, int c2) {
        return Math.abs(r1 - r2) + Math.abs(c1 - c2);
    }

    private Algorithms() {}
}