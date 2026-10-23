package com.vbforge.dietarytrack;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Appends entries to a CSV file (RFC 4180 quoting, UTF-8 with BOM so Excel reads non-Latin
 * names correctly, locale-independent decimals).
 */
public class CsvExporter {

    static final String HEADER = "Date,FirstName,LastName,Phone,Email,Sex,Water,Meals,Wheat,Sugar,Dairy,Miles,Weight";

    private static final String LINE_END = "\r\n";
    private static final String BOM = "﻿";
    /** Values that look like phone numbers are safe even though they may start with + or -. */
    private static final Pattern PHONE_LIKE = Pattern.compile("^[+\\-]?[0-9][0-9 ()\\-]*$");

    private final Path csvFile;

    public CsvExporter() {
        this(AppPaths.csv());
    }

    public CsvExporter(Path file) {
        this.csvFile = file;
    }

    public synchronized void append(DietaryEntry entry) {
        try {
            ensureHeader();
            try (BufferedWriter w = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(toCsvLine(entry));
            }
        } catch (IOException ex) {
            throw new StorageException("Could not write to the CSV file " + csvFile, ex);
        }
    }

    private void ensureHeader() throws IOException {
        if (Files.exists(csvFile) && Files.size(csvFile) > 0) {
            return;
        }
        Path parent = csvFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(csvFile, BOM + HEADER + LINE_END, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static String toCsvLine(DietaryEntry e) {
        return String.join(",",
                escape(e.date()),
                escape(e.firstName()),
                escape(e.lastName()),
                escape(e.phone()),
                escape(e.email()),
                escape(e.sex()),
                Integer.toString(e.water()),
                Integer.toString(e.meals()),
                Boolean.toString(e.wheat()),
                Boolean.toString(e.sugar()),
                Boolean.toString(e.dairy()),
                escape(e.miles()),
                String.format(Locale.ROOT, "%.1f", e.weight())
        ) + LINE_END;
    }

    static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String v = neutralizeFormula(value);
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0 || v.indexOf('\n') >= 0 || v.indexOf('\r') >= 0) {
            return '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }

    /** Stops spreadsheet apps from executing user-typed text such as "=HYPERLINK(...)" as a formula. */
    private static String neutralizeFormula(String v) {
        char first = v.charAt(0);
        boolean risky = first == '=' || first == '@' || first == '\t' || first == '\r'
                || ((first == '+' || first == '-') && !PHONE_LIKE.matcher(v).matches());
        return risky ? "'" + v : v;
    }
}
