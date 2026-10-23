package com.vbforge.dietarytrack;

/** Where a submitted entry is stored. */
public enum ExportMode {
    SQLITE_AND_CSV("SQLite + CSV", true, true),
    SQLITE_ONLY("SQLite (History)", true, false),
    CSV_ONLY("CSV File", false, true);

    private final String label;
    private final boolean database;
    private final boolean csv;

    ExportMode(String label, boolean database, boolean csv) {
        this.label = label;
        this.database = database;
        this.csv = csv;
    }

    public boolean usesDatabase() {
        return database;
    }

    public boolean usesCsv() {
        return csv;
    }

    @Override
    public String toString() {
        return label;
    }
}
