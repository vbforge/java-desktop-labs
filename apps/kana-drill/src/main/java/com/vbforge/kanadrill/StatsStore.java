package com.vbforge.kanadrill;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Per-character stats persisted to ~/.kanadrill/stats.csv.
 *
 * Format: character,attempts,correct,lastSeenMs — one row per character
 * that has been attempted at least once. Characters that have never been
 * seen simply aren't in the file.
 *
 * Tolerates a missing or corrupt file by starting empty. Deleting the
 * file resets progress.
 */
public final class StatsStore {

    private final Path file;
    private final Map<String, KanaStat> stats = new HashMap<>();

    private StatsStore(Path file) {
        this.file = file;
    }

    public static StatsStore open() {
        Path dir = Paths.get(System.getProperty("user.home"), ".kanadrill");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            // Fall back to cwd so the app still works in a restricted env.
            dir = Paths.get(".");
        }
        StatsStore s = new StatsStore(dir.resolve("stats.csv"));
        s.load();
        return s;
    }

    public Path file() {
        return file;
    }

    public KanaStat stat(String character) {
        return stats.getOrDefault(character, KanaStat.empty());
    }

    public void record(String character, boolean correct) {
        stats.put(character, stat(character).withAnswer(correct, System.currentTimeMillis()));
    }

    public void clear() {
        stats.clear();
    }

    // -----------------------------------------------------------------

    private void load() {
        if (!Files.exists(file)) return;
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length != 4) continue;
                try {
                    stats.put(parts[0], new KanaStat(
                            Integer.parseInt(parts[1].trim()),
                            Integer.parseInt(parts[2].trim()),
                            Long.parseLong(parts[3].trim())));
                } catch (NumberFormatException ignored) {
                    // Skip malformed rows silently. One corrupt line
                    // shouldn't nuke the whole history.
                }
            }
        } catch (IOException ignored) {
            // Unreadable file → start fresh.
        }
    }

    public void save() throws IOException {
        // Sort by character so the file diffs cleanly and reads well.
        Map<String, KanaStat> sorted = new TreeMap<>(stats);

        StringBuilder sb = new StringBuilder();
        sb.append("# character,attempts,correct,lastSeenMs\n");
        for (var e : sorted.entrySet()) {
            KanaStat s = e.getValue();
            sb.append(e.getKey()).append(',')
              .append(s.attempts()).append(',')
              .append(s.correct()).append(',')
              .append(s.lastSeenMs()).append('\n');
        }
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }
}