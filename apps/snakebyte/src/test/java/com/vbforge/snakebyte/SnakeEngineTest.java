package com.vbforge.snakebyte;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SnakeEngineTest {

    private SnakeEngine engine;

    @BeforeEach
    void setUp() {
        engine = new SnakeEngine();
    }

    @Test
    void testInitialState() {
        assertEquals(3, engine.getSnake().size());
        assertFalse(engine.isGameOver());
        assertEquals(0, engine.getScore());
    }

    @Test
    void testMoveRight() {
        SnakeEngine.Point initialHead = engine.getSnake().getFirst();
        engine.tick();
        SnakeEngine.Point newHead = engine.getSnake().getFirst();
        assertEquals(initialHead.x() + 1, newHead.x());
    }

    @Test
    void testPreventReverse() {
        // Moving right by default, try to move left
        engine.setDirection(SnakeEngine.Direction.LEFT);
        engine.tick();
        // Head should still move right
        assertEquals(11, engine.getSnake().getFirst().x());
    }
}