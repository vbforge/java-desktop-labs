package com.vbforge.turtlestudio;

import com.vbforge.turtlestudio.model.Turtle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TurtleTest {
    private Turtle turtle;

    @BeforeEach
    void setUp() {
        turtle = new Turtle(300, 300);
    }

    @Test
    void movingForwardIncreasesX() {
        turtle.move(100);
        assertEquals(400, turtle.getX(), 0.01);
        assertEquals(300, turtle.getY(), 0.01);
        assertEquals(1, turtle.getLines().size());
    }

    @Test
    void movingWithPenUpDoesNotDrawLine() {
        turtle.setPenDown(false);
        turtle.move(100);
        assertEquals(0, turtle.getLines().size());
    }

    @Test
    void turningChangesAngle() {
        turtle.turn(90);
        assertEquals(90, turtle.getAngle(), 0.01);
        turtle.turn(-45);
        assertEquals(45, turtle.getAngle(), 0.01);
    }
}