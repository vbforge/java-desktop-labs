package com.vbforge.pongx;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PongEngineTest {

    private PongEngine engine;

    @BeforeEach
    void setUp() {
        engine = new PongEngine();
    }

    @Test
    void testInitialState() {
        assertEquals(0, engine.p1Score);
        assertEquals(0, engine.p2Score);
        assertFalse(engine.isGameOver);
    }

    @Test
    void testPaddleMovement() {
        engine.moveP1(50);
        assertEquals(PongEngine.GAME_HEIGHT / 2.0 - 50 + 50, engine.p1Y); 
        
        engine.moveP1(-1000);
        assertEquals(0, engine.p1Y, "Paddle should clamp at top wall");
    }

    @Test
    void testWallCollision() {
        engine.ballY = 5;
        engine.ballDY = -5;
        engine.update();
        assertTrue(engine.ballDY > 0, "Ball should bounce down off top wall");
    }

    @Test
    void testScoreP1() {
        // FIX: Move the paddle out of the way so the ball can actually pass it
        engine.p2Y = 0; 
        engine.ballY = PongEngine.GAME_HEIGHT - 20; // Put ball near the bottom
        
        engine.ballX = PongEngine.GAME_WIDTH - 5;
        engine.ballDX = 10; // Moving right
        
        engine.update();
        
        assertEquals(1, engine.p1Score, "P1 should score when ball passes right edge");
        assertEquals(PongEngine.GAME_WIDTH / 2.0, engine.ballX, "Ball should reset to center");
    }
}