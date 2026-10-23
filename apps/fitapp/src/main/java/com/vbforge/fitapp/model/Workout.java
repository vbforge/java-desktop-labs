package com.vbforge.fitapp.model;

public record Workout(long id, long userId, String name, int exerciseCount) {
    @Override
    public String toString() {
        return name;
    }
}
