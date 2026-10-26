package com.vbforge.animalpedia.engine;

import java.util.ArrayList;
import java.util.List;

/** Turns the plain text returned by Wikipedia into short paragraphs that are easy to read. */
public final class ArticleFormatter {

    /** Paragraphs longer than this are split at sentence boundaries. */
    static final int MAX_PARAGRAPH = 380;

    private ArticleFormatter() {}

    public static List<String> paragraphs(String extract) {
        List<String> out = new ArrayList<>();
        if (extract == null) return out;

        for (String block : extract.replace("\r\n", "\n").split("\\n\\s*\\n")) {
            String text = block.replaceAll("\\s*\\n\\s*", " ").replaceAll("[ \\t]{2,}", " ").trim();
            if (text.isEmpty()) continue;
            if (text.length() <= MAX_PARAGRAPH) {
                out.add(text);
            } else {
                out.addAll(splitAtSentences(text));
            }
        }
        return out;
    }

    private static List<String> splitAtSentences(String text) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : text.split("(?<=[.!?])\\s+(?=[\\p{Lu}\"'(])")) {
            if (current.length() > 0 && current.length() + sentence.length() + 1 > MAX_PARAGRAPH) {
                chunks.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(sentence);
        }
        if (current.length() > 0) chunks.add(current.toString());
        return chunks;
    }
}
