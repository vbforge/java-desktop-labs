package com.vbforge.slidecraft.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** One slide. Missing values default to the empty string. */
@JsonPropertyOrder({"title", "content", "notes"})
public record Slide(String title, String content, String notes) {

    public Slide {
        title = title == null ? "" : title;
        content = content == null ? "" : content;
        notes = notes == null ? "" : notes;
    }

    public static Slide blank() {
        return new Slide("New Slide", "", "");
    }
}
