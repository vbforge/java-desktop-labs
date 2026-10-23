package com.vbforge.dietarytrack;

import java.util.List;

/** Aggregate figures shown above the history table. */
public record Stats(int total, double avgWater, double avgMeals) {

    public static final Stats EMPTY = new Stats(0, 0, 0);

    public static Stats of(List<DietaryEntry> entries) {
        if (entries.isEmpty()) {
            return EMPTY;
        }
        double water = entries.stream().mapToInt(DietaryEntry::water).average().orElse(0);
        double meals = entries.stream().mapToInt(DietaryEntry::meals).average().orElse(0);
        return new Stats(entries.size(), water, meals);
    }
}
