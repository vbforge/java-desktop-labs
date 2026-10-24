package com.vbforge.turtlestudio.engine;

public record Command(Type type, double value) {
    public enum Type { FORWARD, BACK, LEFT, RIGHT, PEN_UP, PEN_DOWN, COLOR, CLEAR }
}