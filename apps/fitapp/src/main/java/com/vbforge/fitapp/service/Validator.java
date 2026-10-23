package com.vbforge.fitapp.service;

import java.util.regex.Pattern;

/** Input validation. Each method returns null when valid, otherwise a human-readable error message. */
public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_.-]+$");

    public static final int MIN_USERNAME = 3;
    public static final int MAX_USERNAME = 20;
    public static final int MIN_PASSWORD = 8;
    public static final int MAX_NAME = 60;
    public static final int MAX_SETS = 99;
    public static final int MAX_REPS = 999;

    private Validator() {
    }

    public static String username(String s) {
        if (s == null || s.isBlank()) return "Username is required.";
        if (s.length() < MIN_USERNAME || s.length() > MAX_USERNAME)
            return "Username must be " + MIN_USERNAME + "-" + MAX_USERNAME + " characters.";
        if (!USERNAME.matcher(s).matches())
            return "Username may contain only letters, digits, '_', '.' and '-'.";
        return null;
    }

    public static String email(String s) {
        if (s == null || s.isBlank()) return "Email is required.";
        if (s.length() > 120 || !EMAIL.matcher(s).matches()) return "Please enter a valid email address.";
        return null;
    }

    /** At least 8 characters, with a letter and a digit. */
    public static String password(String s) {
        if (s == null || s.isEmpty()) return "Password is required.";
        if (s.length() < MIN_PASSWORD) return "Password must be at least " + MIN_PASSWORD + " characters.";
        boolean letter = false, digit = false;
        for (char c : s.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) return "Password must contain at least one letter and one digit.";
        return null;
    }

    public static String name(String s, String label) {
        if (s == null || s.isBlank()) return label + " is required.";
        if (s.trim().length() > MAX_NAME) return label + " must be at most " + MAX_NAME + " characters.";
        return null;
    }

    public static String sets(int v) {
        return (v < 1 || v > MAX_SETS) ? "Sets must be between 1 and " + MAX_SETS + "." : null;
    }

    public static String reps(int v) {
        return (v < 1 || v > MAX_REPS) ? "Reps must be between 1 and " + MAX_REPS + "." : null;
    }

    /** Parses a positive integer; returns -1 when the text is not a number. */
    public static int parseInt(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
