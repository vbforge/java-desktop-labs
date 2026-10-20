package com.vbforge.pongx;

public class PongEngine {

    public static final int GAME_WIDTH = 900;
    public static final int GAME_HEIGHT = 560;
    public static final int PADDLE_WIDTH = 15;
    public static final int PADDLE_HEIGHT = 100;
    public static final int BALL_RADIUS = 10;
    public static final int WINNING_SCORE = 10;

    public double p1Y, p2Y;
    public double ballX, ballY, ballDX, ballDY;
    public int p1Score, p2Score;
    public boolean isGameOver;

    public PongEngine() {
        reset();
    }

    public void reset() {
        p1Y = GAME_HEIGHT / 2.0 - PADDLE_HEIGHT / 2.0;
        p2Y = GAME_HEIGHT / 2.0 - PADDLE_HEIGHT / 2.0;
        p1Score = 0;
        p2Score = 0;
        isGameOver = false;
        resetBall(Math.random() > 0.5 ? 1 : -1);
    }

    public void resetBall(int direction) {
        ballX = GAME_WIDTH / 2.0;
        ballY = GAME_HEIGHT / 2.0;
        
        double speed = 6.0;
        // Random angle between -45 and 45 degrees
        double angle = (Math.random() * Math.PI / 2) - (Math.PI / 4);
        
        ballDX = Math.cos(angle) * speed * direction;
        ballDY = Math.sin(angle) * speed;
    }

    public void moveP1(double dy) {
        p1Y += dy;
        if (p1Y < 0) p1Y = 0;
        if (p1Y > GAME_HEIGHT - PADDLE_HEIGHT) p1Y = GAME_HEIGHT - PADDLE_HEIGHT;
    }

    public void moveP2(double dy) {
        p2Y += dy;
        if (p2Y < 0) p2Y = 0;
        if (p2Y > GAME_HEIGHT - PADDLE_HEIGHT) p2Y = GAME_HEIGHT - PADDLE_HEIGHT;
    }

    public void moveAi(double aiSpeed) {
        double targetY = ballY - PADDLE_HEIGHT / 2.0;
        if (p2Y < targetY) {
            p2Y += aiSpeed;
        } else if (p2Y > targetY) {
            p2Y -= aiSpeed;
        }
        // Clamp inside bounds
        if (p2Y < 0) p2Y = 0;
        if (p2Y > GAME_HEIGHT - PADDLE_HEIGHT) p2Y = GAME_HEIGHT - PADDLE_HEIGHT;
    }

    public void update() {
        if (isGameOver) return;

        // Move Ball
        ballX += ballDX;
        ballY += ballDY;

        // Top/Bottom Wall Collision
        if (ballY - BALL_RADIUS < 0) {
            ballY = BALL_RADIUS;
            ballDY *= -1;
        }
        if (ballY + BALL_RADIUS > GAME_HEIGHT) {
            ballY = GAME_HEIGHT - BALL_RADIUS;
            ballDY *= -1;
        }

        // P1 Collision (Left Paddle)
        if (ballDX < 0 && ballX - BALL_RADIUS < PADDLE_WIDTH && 
            ballX + BALL_RADIUS > 0 && ballY > p1Y && ballY < p1Y + PADDLE_HEIGHT) {
            ballX = PADDLE_WIDTH + BALL_RADIUS;
            bounceOffPaddle(true);
        }

        // P2 Collision (Right Paddle)
        if (ballDX > 0 && ballX + BALL_RADIUS > GAME_WIDTH - PADDLE_WIDTH && 
            ballX - BALL_RADIUS < GAME_WIDTH && ballY > p2Y && ballY < p2Y + PADDLE_HEIGHT) {
            ballX = GAME_WIDTH - PADDLE_WIDTH - BALL_RADIUS;
            bounceOffPaddle(false);
        }

        // Scoring
        if (ballX - BALL_RADIUS < 0) {
            p2Score++;
            checkWin();
            resetBall(1);
        }
        if (ballX + BALL_RADIUS > GAME_WIDTH) {
            p1Score++;
            checkWin();
            resetBall(-1);
        }
    }

    private void bounceOffPaddle(boolean isP1) {
        // Calculate hit position relative to the center of the paddle (-1 to 1)
        double paddleY = isP1 ? p1Y : p2Y;
        double hitPos = (ballY - (paddleY + PADDLE_HEIGHT / 2.0)) / (PADDLE_HEIGHT / 2.0);
        
        // Max bounce angle is 60 degrees (PI/3)
        double angle = hitPos * (Math.PI / 3);
        
        // Increase speed slightly
        double speed = Math.sqrt(ballDX * ballDX + ballDY * ballDY) + 0.5;
        
        ballDX = Math.cos(angle) * speed * (isP1 ? 1 : -1);
        ballDY = Math.sin(angle) * speed;
    }

    private void checkWin() {
        if (p1Score >= WINNING_SCORE || p2Score >= WINNING_SCORE) {
            isGameOver = true;
        }
    }
}