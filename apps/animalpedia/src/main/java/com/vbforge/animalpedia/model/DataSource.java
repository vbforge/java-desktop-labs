package com.vbforge.animalpedia.model;

/** Where an {@link Animal} came from. */
public enum DataSource {
    BUILT_IN("Built-in guide"),
    WIKIPEDIA("Wikipedia"),
    CACHED("Saved from Wikipedia");

    private final String label;

    DataSource(String label) { this.label = label; }

    public String label() { return label; }
}
