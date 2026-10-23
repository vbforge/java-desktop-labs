package com.vbforge.dietarytrack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvExporterTest {

    private static DietaryEntry entry(String first, String phone) {
        return new DietaryEntry(0, "2026-10-01", first, "Doe", phone, "a@b.co", "Female",
                8, 3, true, false, true, "1 - 2 Miles", 72.5);
    }

    @Test
    void writesBomAndHeaderOnceThenAppendsRows(@TempDir Path tmp) throws IOException {
        Path file = tmp.resolve("sub").resolve("out.csv"); // parent folder does not exist yet
        CsvExporter csv = new CsvExporter(file);
        csv.append(entry("Ann", "+380501234567"));
        csv.append(entry("Bob", ""));

        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertTrue(content.startsWith("﻿" + CsvExporter.HEADER));
        List<String> lines = content.lines().toList();
        assertEquals(3, lines.size());
        assertEquals("2026-10-01,Ann,Doe,+380501234567,a@b.co,Female,8,3,true,false,true,1 - 2 Miles,72.5",
                lines.get(1));
        assertEquals("2026-10-01,Bob,Doe,,a@b.co,Female,8,3,true,false,true,1 - 2 Miles,72.5", lines.get(2));
    }

    @Test
    void decimalSeparatorIsAlwaysADotRegardlessOfLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("uk-UA"));
            assertTrue(CsvExporter.toCsvLine(entry("Ann", "")).endsWith(",72.5\r\n"));
        } finally {
            Locale.setDefault(previous);
        }
    }

    @Test
    void quotesCommasQuotesAndNewlines() {
        assertEquals("\"Doe, Jr\"", CsvExporter.escape("Doe, Jr"));
        assertEquals("\"say \"\"hi\"\"\"", CsvExporter.escape("say \"hi\""));
        assertEquals("\"a\nb\"", CsvExporter.escape("a\nb"));
        assertEquals("", CsvExporter.escape(null));
    }

    @Test
    void neutralizesSpreadsheetFormulasButKeepsPhoneNumbers() {
        assertEquals("'=1+1", CsvExporter.escape("=1+1"));
        assertEquals("'@SUM(A1)", CsvExporter.escape("@SUM(A1)"));
        assertEquals("'-1+1", CsvExporter.escape("-1+1"));
        assertEquals("+380 (50) 123-45-67", CsvExporter.escape("+380 (50) 123-45-67"));
    }
}
