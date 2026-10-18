package com.vbforge.squarecraft;

public class MagicSquareEngine {

    public record SolveResult(boolean success, Integer[][] grid, int magicConstant, String verificationText) {}

    // --- GENERATOR (Siam Method) ---
    public static int[][] generate(int startNumber) {
        int[][] matrix = new int[3][3];
        int row = 0;
        int col = 1;
        
        for (int num = startNumber; num < startNumber + 9; num++) {
            matrix[row][col] = num;
            int nextRow = (row - 1 + 3) % 3;
            int nextCol = (col + 1) % 3;
            
            if (matrix[nextRow][nextCol] != 0) {
                row = (row + 1) % 3;
            } else {
                row = nextRow;
                col = nextCol;
            }
        }
        return matrix;
    }

    // --- SOLVER ---
    public static SolveResult solve(Integer[][] input) {
        Integer[][] grid = new Integer[3][3];
        for (int i = 0; i < 3; i++) System.arraycopy(input[i], 0, grid[i], 0, 3);

        Integer magicConstant = findMagicConstant(grid);
        if (magicConstant == null) {
            return new SolveResult(false, grid, 0, "Cannot solve: No complete line found to determine the magic constant.");
        }

        boolean changed = true;
        int maxIterations = 20;
        int iter = 0;

        while (changed && iter++ < maxIterations) {
            changed = false;
            for (int i = 0; i < 3; i++) {
                if (solveLine(grid, i, 0, 0, 1)) changed = true;       // Row
                if (solveLine(grid, 0, i, 1, 0)) changed = true;       // Col
            }
            if (solveLine(grid, 0, 0, 1, 1)) changed = true;           // Main diag
            if (solveLine(grid, 0, 2, 1, -1)) changed = true;          // Anti diag
        }

        if (!isComplete(grid)) {
            return new SolveResult(false, grid, magicConstant, "Cannot solve: Not enough information.");
        }

        String verification = verifyAndGetDetails(grid, magicConstant);
        boolean isValid = verification.contains("✗") == false; // If no crosses, it's valid

        return new SolveResult(true, grid, magicConstant, verification);
    }

    private static Integer findMagicConstant(Integer[][] g) {
        for (int i = 0; i < 3; i++) {
            if (g[i][0] != null && g[i][1] != null && g[i][2] != null) return g[i][0] + g[i][1] + g[i][2];
            if (g[0][i] != null && g[1][i] != null && g[2][i] != null) return g[0][i] + g[1][i] + g[2][i];
        }
        if (g[0][0] != null && g[1][1] != null && g[2][2] != null) return g[0][0] + g[1][1] + g[2][2];
        if (g[0][2] != null && g[1][1] != null && g[2][0] != null) return g[0][2] + g[1][1] + g[2][0];
        return null;
    }

    private static boolean solveLine(Integer[][] g, int startR, int startC, int dr, int dc) {
        Integer sum = 0;
        int nullR = -1, nullC = -1;
        int nullCount = 0;

        for (int i = 0; i < 3; i++) {
            int r = startR + i * dr;
            int c = startC + i * dc;
            if (g[r][c] == null) {
                nullCount++;
                nullR = r;
                nullC = c;
            } else {
                sum += g[r][c];
            }
        }
        // If we don't know the magic constant yet, we can't solve
        Integer mc = findMagicConstant(g); 
        if (nullCount == 1 && mc != null) {
            g[nullR][nullC] = mc - sum;
            return true;
        }
        return false;
    }

    private static boolean isComplete(Integer[][] g) {
        for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) if (g[r][c] == null) return false;
        return true;
    }

    private static String verifyAndGetDetails(Integer[][] g, int mc) {
        StringBuilder sb = new StringBuilder();
        sb.append("Magic Constant: ").append(mc).append("\n\nRows:\n");
        for (int i = 0; i < 3; i++) {
            int sum = g[i][0] + g[i][1] + g[i][2];
            sb.append("  Row ").append(i + 1).append(": ").append(g[i][0]).append(" + ").append(g[i][1]).append(" + ").append(g[i][2]).append(" = ").append(sum).append(sum == mc ? " ✓\n" : " ✗\n");
        }
        sb.append("\nColumns:\n");
        for (int i = 0; i < 3; i++) {
            int sum = g[0][i] + g[1][i] + g[2][i];
            sb.append("  Col ").append(i + 1).append(": ").append(g[0][i]).append(" + ").append(g[1][i]).append(" + ").append(g[2][i]).append(" = ").append(sum).append(sum == mc ? " ✓\n" : " ✗\n");
        }
        sb.append("\nDiagonals:\n");
        int s1 = g[0][0] + g[1][1] + g[2][2];
        sb.append("  Main: ").append(g[0][0]).append(" + ").append(g[1][1]).append(" + ").append(g[2][2]).append(" = ").append(s1).append(s1 == mc ? " ✓\n" : " ✗\n");
        int s2 = g[0][2] + g[1][1] + g[2][0];
        sb.append("  Anti: ").append(g[0][2]).append(" + ").append(g[1][1]).append(" + ").append(g[2][0]).append(" = ").append(s2).append(s2 == mc ? " ✓\n" : " ✗\n");
        
        return sb.toString();
    }
}