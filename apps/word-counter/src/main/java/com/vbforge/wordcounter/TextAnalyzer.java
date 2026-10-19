package com.vbforge.wordcounter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TextAnalyzer {

    // Common words to optionally ignore
    private static final Set<String> STOP_WORDS = Set.of(
        "a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if", "in", "into", "is", "it", 
        "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there", "these", "they", 
        "this", "to", "was", "will", "with", "without", "have", "has", "had", "do", "does", "did", "can", 
        "could", "should", "would", "you", "your", "we", "our", "i", "my", "me", "he", "him", "his", "she", 
        "her", "them", "what", "which", "who", "whom", "whose", "where", "when", "why", "how", "all", "any", 
        "both", "each", "few", "more", "most", "other", "some", "only", "own", "same", "so", "than", "too", "very"
    );

    public String readResource(String resourcePath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    public WordStats analyze(String text, boolean ignoreStopWords) {
        List<String> words = Arrays.stream(text.split("[^\\p{L}\\p{N}']+"))
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .toList();

        if (words.isEmpty()) {
            return new WordStats(0, 0, "", "", Map.of(), 0.0);
        }

        int total = words.size();
        Set<String> unique = new HashSet<>(words);

        String shortest = words.stream().min(Comparator.comparingInt(String::length)).orElse("");
        String longest  = words.stream().max(Comparator.comparingInt(String::length)).orElse("");
        double avgLen = words.stream().mapToInt(String::length).average().orElse(0.0);

        // Filter stop words for the chart if requested
        List<String> chartWords = words;
        if (ignoreStopWords) {
            chartWords = words.stream().filter(w -> !STOP_WORDS.contains(w)).toList();
        }

        Map<String, Integer> freq = chartWords.stream()
                .collect(Collectors.groupingBy(w -> w, Collectors.summingInt(w -> 1)));

        Map<String, Integer> sortedFreq = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a,
                        LinkedHashMap::new));

        return new WordStats(total, unique.size(), shortest, longest, sortedFreq, avgLen);
    }
}