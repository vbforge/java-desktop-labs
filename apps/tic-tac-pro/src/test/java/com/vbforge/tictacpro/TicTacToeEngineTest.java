package com.vbforge.tictacpro;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TicTacToeEngineTest {

    private TicTacToeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new TicTacToeEngine();
    }

    @Test
    void testWinCondition() {
        engine.makeMove(0, TicTacToeEngine.X);
        engine.makeMove(1, TicTacToeEngine.X);
        engine.makeMove(2, TicTacToeEngine.X);
        assertEquals(TicTacToeEngine.X, engine.checkWinner());
    }

    @Test
    void testDrawCondition() {
        // X O X
        // X O O
        // O X X
        String[] moves = {
            TicTacToeEngine.X, TicTacToeEngine.O, TicTacToeEngine.X,
            TicTacToeEngine.X, TicTacToeEngine.O, TicTacToeEngine.O,
            TicTacToeEngine.O, TicTacToeEngine.X, TicTacToeEngine.X
        };
        for (int i = 0; i < 9; i++) engine.makeMove(i, moves[i]);
        
        assertNull(engine.checkWinner());
        assertTrue(engine.isDraw());
    }

    @Test
    void testAiBlocksWin() {
        // Human (X) has two in a row at 0, 1. AI (O) must block at 2.
        engine.makeMove(0, TicTacToeEngine.X);
        engine.makeMove(4, TicTacToeEngine.O);
        engine.makeMove(1, TicTacToeEngine.X);

        int aiMove = engine.getBestMove(TicTacToeEngine.O, TicTacToeEngine.X);
        assertEquals(2, aiMove, "AI must block the winning move at index 2");
    }

    @Test
    void testAiTakesWin() {
        // AI (O) has two in a row at 0, 1. Human (X) plays elsewhere.
        engine.makeMove(3, TicTacToeEngine.X);
        engine.makeMove(0, TicTacToeEngine.O);
        engine.makeMove(4, TicTacToeEngine.X);
        engine.makeMove(1, TicTacToeEngine.O);

        int aiMove = engine.getBestMove(TicTacToeEngine.O, TicTacToeEngine.X);
        assertEquals(2, aiMove, "AI must win at index 2");
    }
}