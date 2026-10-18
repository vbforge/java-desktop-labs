package com.vbforge.squarecraft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MagicSquareEngineTest {

    @Test
    void testGenerateStandard() {
        int[][] grid = MagicSquareEngine.generate(1);
        // Standard 1-9 square
        assertEquals(8, grid[0][0]);
        assertEquals(1, grid[0][1]);
        assertEquals(6, grid[0][2]);
        assertEquals(5, grid[1][1]);
        assertEquals(2, grid[2][2]);
        
        // Magic constant should be 15
        assertEquals(15, grid[0][0] + grid[0][1] + grid[0][2]);
    }

    @Test
    void testGenerateCustom() {
        int[][] grid = MagicSquareEngine.generate(10);
        assertEquals(17, grid[0][0]);
        // Magic constant: 3 * (10 + 18) / 2 = 42
        assertEquals(42, grid[0][0] + grid[0][1] + grid[0][2]);
    }

    @Test
    void testSolveExample1() {
        Integer[][] input = {
            {80, null, null},
            {180, 100, null},
            {null, null, 120}
        };
        var result = MagicSquareEngine.solve(input);
        assertTrue(result.success());
        assertEquals(300, result.magicConstant());
        
        // The correct solution for the empty cells
        assertEquals(60, result.grid()[0][1]);
        assertEquals(160, result.grid()[0][2]);
        assertEquals(20, result.grid()[1][2]);
        assertEquals(40, result.grid()[2][0]);
        assertEquals(140, result.grid()[2][1]);
    }

    @Test
    void testSolveInsufficientData() {
        Integer[][] input = {
            {null, null, null},
            {null, 100, null},
            {null, null, null}
        };
        var result = MagicSquareEngine.solve(input);
        assertFalse(result.success());
    }
}