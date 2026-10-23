package com.vbforge.sudoku;

public record StepEvent(Type type, int row, int col, int num) {
    public enum Type { TRY, PLACE, BACKTRACK, SOLVED, FAILED }
}