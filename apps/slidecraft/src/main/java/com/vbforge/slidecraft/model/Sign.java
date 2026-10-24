package com.vbforge.slidecraft.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** Presentation-wide signature / footer stamped bottom-right on every slide. */
@JsonPropertyOrder({"enabled", "text"})
public record Sign(boolean enabled, String text) {

    public static final int MAX_LENGTH = 200;
    public static final Sign NONE = new Sign(false, "");

    public Sign {
        String t = text == null ? "" : text.replaceAll("\\R", " ");
        text = t.length() > MAX_LENGTH ? t.substring(0, MAX_LENGTH) : t;
    }

    /** True when the sign is enabled and has visible text. */
    public boolean shouldRender() {
        return enabled && !text.isBlank();
    }
}
