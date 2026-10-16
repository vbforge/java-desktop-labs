package com.vbforge.contacts.repo;

import com.vbforge.contacts.Contact;
import com.vbforge.contacts.ContactRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * RFC 4180-style CSV.
 *
 * Fields containing a comma, quote, CR or LF are wrapped in double
 * quotes; embedded quotes are doubled. That is what Excel, LibreOffice
 * and every other spreadsheet expects, and it round-trips names like
 * "Smith, John" without confusing the parser.
 */
public final class CsvContactRepository implements ContactRepository {

    private final Path file;

    public CsvContactRepository(Path file) {
        this.file = file;
    }

    @Override
    public List<Contact> findAll() throws IOException {
        if (!Files.exists(file)) return new ArrayList<>();

        List<Contact> out = new ArrayList<>();
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

        boolean first = true;
        for (String line : lines) {
            if (line.isBlank()) continue;

            List<String> f = parseLine(line);

            // Skip the header row (if present).
            if (first && !f.isEmpty() && f.get(0).equalsIgnoreCase("id")) {
                first = false;
                continue;
            }
            first = false;

            if (f.size() < 4) continue;

            try {
                out.add(new Contact(
                        Long.parseLong(f.get(0).trim()),
                        f.get(1),
                        f.get(2),
                        f.get(3)));
            } catch (NumberFormatException ignored) {
                // One malformed row should not kill the whole load.
            }
        }
        return out;
    }

    @Override
    public void saveAll(List<Contact> contacts) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("id,name,phone,email\n");
        for (Contact k : contacts) {
            sb.append(k.id()).append(',')
              .append(quote(k.name())).append(',')
              .append(quote(k.phone())).append(',')
              .append(quote(k.email())).append('\n');
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private static String quote(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"")
                || s.contains("\n") || s.contains("\r");
        return needsQuotes ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private static List<String> parseLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;   // consume the escaped second quote
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }

    @Override
    public String describe() {
        return "CSV (" + file.getFileName() + ")";
    }
}