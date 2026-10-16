package com.vbforge.taskflow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Persists the board to ~/.taskflow/board.tsv.
 *
 * Format: one row per line, fields separated by ASCII Unit Separator
 * (U+001F). That character is specifically meant for separating fields
 * in a text stream and is impossible to type in the UI, so we get
 * escaping for free — no quoting rules, no "what if the title has a tab".
 *
 * Column order is file order. Card order within a column is file order.
 */
public final class BoardStore {

    private static final char SEP = '\u001F';
    private static final String COLUMN_PREFIX = "COLUMN";
    private static final String CARD_PREFIX   = "CARD";

    private final Path file;

    private BoardStore(Path file) {
        this.file = file;
    }

    public static BoardStore open() {
        Path dir = Paths.get(System.getProperty("user.home"), ".taskflow");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            dir = Paths.get(".");
        }
        return new BoardStore(dir.resolve("board.tsv"));
    }

    public Path file() {
        return file;
    }

    public void save(BoardModel model) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# TaskFlow board\n");

        for (Column c : model.columns()) {
            sb.append(COLUMN_PREFIX).append(SEP)
              .append(c.id()).append(SEP)
              .append(c.name()).append('\n');

            for (Card card : model.cardsIn(c.id())) {
                sb.append(CARD_PREFIX).append(SEP)
                  .append(card.id()).append(SEP)
                  .append(card.columnId()).append(SEP)
                  .append(card.title()).append(SEP)
                  .append(card.description()).append('\n');
            }
        }

        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    public void load(BoardModel model) {
        if (!Files.exists(file)) {
            model.seedIfEmpty();
            return;
        }

        model.clear();
        long maxId = 0;

        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(String.valueOf(SEP), -1);
                if (parts.length < 2) continue;

                if (COLUMN_PREFIX.equals(parts[0]) && parts.length >= 3) {
                    long id = Long.parseLong(parts[1]);
                    String name = parts[2];
                    model.addColumn(new Column(id, name));
                    maxId = Math.max(maxId, id);

                } else if (CARD_PREFIX.equals(parts[0]) && parts.length >= 5) {
                    long id       = Long.parseLong(parts[1]);
                    long columnId = Long.parseLong(parts[2]);
                    String title  = parts[3];
                    String desc   = parts[4];
                    model.addCard(new Card(id, columnId, title, desc), Integer.MAX_VALUE);
                    maxId = Math.max(maxId, id);
                }
            }
        } catch (Exception e) {
            // Corrupt file → start fresh with the seed board.
            model.clear();
            model.seedIfEmpty();
            return;
        }

        model.restoreNextId(maxId + 1);

        if (model.columns().isEmpty()) {
            model.seedIfEmpty();
        }
    }
}