package com.vbforge.dietarytrack;

import java.time.LocalDate;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/** Pure validation helpers. Each {@code Optional<String>} result is an error message, empty when valid. */
final class Validators {

    static final int MAX_NAME_LENGTH = 60;
    static final int MAX_EMAIL_LENGTH = 120;
    static final int MIN_WATER = 0;
    static final int MAX_WATER = 50;
    static final double MIN_WEIGHT = 10;
    static final double MAX_WEIGHT = 500;

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s,]+@[^@\\s,]+\\.[^@\\s,]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()\\-]{5,25}$");
    private static final Pattern WEIGHT = Pattern.compile("^\\d{1,3}([.,]\\d{1,2})?$");
    private static final Pattern WATER = Pattern.compile("^\\d{1,2}$");

    private Validators() {}

    static Optional<String> name(String value, String label) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return Optional.of(label + " is required.");
        }
        if (v.length() > MAX_NAME_LENGTH) {
            return Optional.of(label + " must be at most " + MAX_NAME_LENGTH + " characters.");
        }
        return Optional.empty();
    }

    static Optional<String> phone(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return Optional.empty();
        }
        long digits = v.chars().filter(Character::isDigit).count();
        if (!PHONE.matcher(v).matches() || digits < 5) {
            return Optional.of("Phone number looks invalid.");
        }
        return Optional.empty();
    }

    static Optional<String> email(String value) {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            return Optional.empty();
        }
        if (v.length() > MAX_EMAIL_LENGTH || !EMAIL.matcher(v).matches()) {
            return Optional.of("Email address looks invalid.");
        }
        return Optional.empty();
    }

    static Optional<String> date(LocalDate value, LocalDate today) {
        if (value == null) {
            return Optional.of("Please pick a valid date.");
        }
        if (value.isAfter(today)) {
            return Optional.of("Date cannot be in the future.");
        }
        return Optional.empty();
    }

    static OptionalInt parseWater(String text) {
        String v = text == null ? "" : text.trim();
        if (!WATER.matcher(v).matches()) {
            return OptionalInt.empty();
        }
        int n = Integer.parseInt(v);
        return n >= MIN_WATER && n <= MAX_WATER ? OptionalInt.of(n) : OptionalInt.empty();
    }

    static Optional<String> water(String text) {
        return parseWater(text).isPresent()
                ? Optional.empty()
                : Optional.of("Water must be a whole number between " + MIN_WATER + " and " + MAX_WATER + ".");
    }

    /** Accepts "72", "72.5" and "72,5"; returns the value rounded to one decimal place. */
    static OptionalDouble parseWeight(String text) {
        String v = text == null ? "" : text.trim();
        if (!WEIGHT.matcher(v).matches()) {
            return OptionalDouble.empty();
        }
        double kg = Double.parseDouble(v.replace(',', '.'));
        if (kg < MIN_WEIGHT || kg > MAX_WEIGHT) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(Math.round(kg * 10.0) / 10.0);
    }

    static Optional<String> weight(String text) {
        return parseWeight(text).isPresent()
                ? Optional.empty()
                : Optional.of("Weight must be a number between " + (int) MIN_WEIGHT + " and " + (int) MAX_WEIGHT + " kg.");
    }
}
