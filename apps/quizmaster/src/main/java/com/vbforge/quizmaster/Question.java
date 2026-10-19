package com.vbforge.quizmaster;

import java.util.List;

public record Question(String category, String text, List<String> options, int correctIndex) {}