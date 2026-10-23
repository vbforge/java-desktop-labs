package com.vbforge.fitapp.model;

public record Exercise(long id, long userId, String name, int sets, int reps) {
    @Override
    public String toString() {
        return name + " (" + sets + "x" + reps + ")";
    }
}
