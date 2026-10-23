package com.vbforge.dietarytrack;

/**
 * One logged day. {@code date} is an ISO-8601 string (yyyy-MM-dd) so that it sorts correctly
 * in SQLite and stays compatible with databases created by earlier versions.
 */
public record DietaryEntry(
        long id,
        String date,
        String firstName,
        String lastName,
        String phone,
        String email,
        String sex,
        int water,
        int meals,
        boolean wheat,
        boolean sugar,
        boolean dairy,
        String miles,
        double weight
) {}
