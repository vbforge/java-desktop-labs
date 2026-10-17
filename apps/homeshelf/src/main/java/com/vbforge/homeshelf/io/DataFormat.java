package com.vbforge.homeshelf.io;

public enum DataFormat {
    JSON("JSON (.json)"),
    XML("XML (.xml)"),
    SQLITE("SQLite (.db)"),
    BINARY("Binary (.dat)"),
    PROPERTIES("Properties (.props)"),
    OBJECT("Java Object (.obj)");

    private final String label;
    DataFormat(String label) { this.label = label; }
    public String label() { return label; }
}