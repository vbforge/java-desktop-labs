package com.vbforge.turtlestudio.ui;

import com.vbforge.turtlestudio.model.Line;
import com.vbforge.turtlestudio.model.Turtle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class TurtleCanvas extends Canvas {
    public static final int SIZE = 600;
    private final Turtle turtle;

    public TurtleCanvas(Turtle turtle) {
        this.turtle = turtle;
        setWidth(SIZE);
        setHeight(SIZE);
    }

    public void draw() {
        GraphicsContext gc = getGraphicsContext2D();
        gc.clearRect(0, 0, SIZE, SIZE);
        
        // Draw Grid
        gc.setLineWidth(1);
        gc.setStroke(Color.web("#161b22"));
        for (int i = 0; i <= SIZE; i += 30) {
            gc.strokeLine(i, 0, i, SIZE);
            gc.strokeLine(0, i, SIZE, i);
        }
        
        // Center axes
        gc.setStroke(Color.web("#2a313c"));
        gc.strokeLine(SIZE/2.0, 0, SIZE/2.0, SIZE);
        gc.strokeLine(0, SIZE/2.0, SIZE, SIZE/2.0);

        // Draw Lines
        gc.setLineWidth(2);
        for (Line line : turtle.getLines()) {
            gc.setStroke(line.color());
            gc.strokeLine(line.x1(), line.y1(), line.x2(), line.y2());
        }

        // Draw Turtle
        gc.save();
        gc.translate(turtle.getX(), turtle.getY());
        gc.rotate(turtle.getAngle());
        
        gc.setFill(Color.web("#f89820"));
        gc.beginPath();
        gc.moveTo(10, 0);
        gc.lineTo(-8, -6);
        gc.lineTo(-8, 6);
        gc.closePath();
        gc.fill();
        
        gc.restore();
    }
}