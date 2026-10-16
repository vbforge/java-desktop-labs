package com.vbforge.contacts;

public enum BackendType {

    SQLITE("SQLite", "contacts.db"),
    CSV("CSV",       "contacts.csv"),
    JSON("JSON",     "contacts.json");

    private final String display;
    private final String defaultFileName;

    BackendType(String display, String defaultFileName) {
        this.display = display;
        this.defaultFileName = defaultFileName;
    }

    public String display() { return display; }
    public String defaultFileName() { return defaultFileName; }

    @Override public String toString() { return display; }

    public static BackendType fromName(String s) {
        if (s == null) return SQLITE;
        try {
            return BackendType.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return SQLITE;
        }
    }

    /** Best guess from a file extension, or null if unrecognized. */
    public static BackendType fromFileName(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".db") || lower.endsWith(".sqlite") || lower.endsWith(".sqlite3"))
            return SQLITE;
        if (lower.endsWith(".csv"))  return CSV;
        if (lower.endsWith(".json")) return JSON;
        return null;
    }
}