package com.vbforge.wordcounter;

import java.util.Map;

public record WordStats(
        int totalWords,
        int uniqueWords,
        String shortestWord,
        String longestWord,
        Map<String, Integer> frequencies,
        double averageWordLength
) {}