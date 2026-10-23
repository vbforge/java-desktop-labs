package com.vbforge.sudoku;

public enum Difficulty {
    EASY(36), MEDIUM(46), HARD(52), EXPERT(58);
    private final int cellsToRemove;
    Difficulty(int cellsToRemove) { this.cellsToRemove = cellsToRemove; }
    public int getCellsToRemove() { return cellsToRemove; }
}