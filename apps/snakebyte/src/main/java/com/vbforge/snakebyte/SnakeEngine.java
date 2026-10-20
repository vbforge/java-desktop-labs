package com.vbforge.snakebyte;

import java.util.LinkedList;
import java.util.Random;

public class SnakeEngine {

    public static final int GRID_SIZE = 20;
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    public record Point(int x, int y) {}

    private final LinkedList<Point> snake = new LinkedList<>();
    private final Random random = new Random();
    private Point food;
    private Direction currentDirection;
    private Direction nextDirection;
    private int score;
    private boolean gameOver;

    public SnakeEngine() {
        reset();
    }

    public void reset() {
        snake.clear();
        // Start with 3 segments in the middle
        snake.add(new Point(10, 10));
        snake.add(new Point(9, 10));
        snake.add(new Point(8, 10));
        
        currentDirection = Direction.RIGHT;
        nextDirection = Direction.RIGHT;
        score = 0;
        gameOver = false;
        spawnFood();
    }

    public void setDirection(Direction dir) {
        // Prevent moving directly backwards into yourself
        if (dir == Direction.UP && currentDirection == Direction.DOWN) return;
        if (dir == Direction.DOWN && currentDirection == Direction.UP) return;
        if (dir == Direction.LEFT && currentDirection == Direction.RIGHT) return;
        if (dir == Direction.RIGHT && currentDirection == Direction.LEFT) return;
        
        this.nextDirection = dir;
    }

    public void tick() {
        if (gameOver) return;

        currentDirection = nextDirection;
        Point head = snake.getFirst();
        Point newHead = switch (currentDirection) {
            case UP -> new Point(head.x(), head.y() - 1);
            case DOWN -> new Point(head.x(), head.y() + 1);
            case LEFT -> new Point(head.x() - 1, head.y());
            case RIGHT -> new Point(head.x() + 1, head.y());
        };

        // Check wall collisions
        if (newHead.x() < 0 || newHead.x() >= GRID_SIZE || 
            newHead.y() < 0 || newHead.y() >= GRID_SIZE) {
            gameOver = true;
            return;
        }

        // Check self collision
        for (Point p : snake) {
            if (p.equals(newHead)) {
                gameOver = true;
                return;
            }
        }

        snake.addFirst(newHead);

        // Check food collision
        if (newHead.equals(food)) {
            score += 10;
            spawnFood();
        } else {
            snake.removeLast(); // Move forward by removing tail
        }
    }

    private void spawnFood() {
        while (true) {
            Point p = new Point(random.nextInt(GRID_SIZE), random.nextInt(GRID_SIZE));
            if (!snake.contains(p)) {
                food = p;
                break;
            }
        }
    }

    // Getters for the UI
    public LinkedList<Point> getSnake() { return snake; }
    public Point getFood() { return food; }
    public int getScore() { return score; }
    public boolean isGameOver() { return gameOver; }
}