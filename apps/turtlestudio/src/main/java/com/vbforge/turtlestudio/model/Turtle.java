package com.vbforge.turtlestudio.model;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

public class Turtle {
    private double x;
    private double y;
    private double angle; // in degrees, 0 = East, 90 = South
    private boolean penDown;
    private Color color;
    private final List<Line> lines;

    public Turtle(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.angle = 0;
        this.penDown = true;
        this.color = Color.web("#f89820");
        this.lines = new ArrayList<>();
    }

    public void reset() {
        this.x = 300;
        this.y = 300;
        this.angle = 0;
        this.penDown = true;
        this.color = Color.web("#f89820");
        this.lines.clear();
    }

    public void move(double distance) {
        double oldX = x;
        double oldY = y;
        double rad = Math.toRadians(angle);
        
        x += distance * Math.cos(rad);
        y += distance * Math.sin(rad);

        if (penDown) {
            lines.add(new Line(oldX, oldY, x, y, color));
        }
    }

    public void turn(double degrees) {
        angle = (angle + degrees) % 360;
        if (angle < 0) angle += 360;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public boolean isPenDown() { return penDown; }
    public Color getColor() { return color; }
    public List<Line> getLines() { return lines; }

    public void setPenDown(boolean penDown) { this.penDown = penDown; }
    public void setColor(Color color) { this.color = color; }
}