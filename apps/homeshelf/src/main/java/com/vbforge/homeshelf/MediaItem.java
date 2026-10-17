package com.vbforge.homeshelf;

public record MediaItem(
        String title,
        String creator,
        int year,
        String type,
        int rating
) {}