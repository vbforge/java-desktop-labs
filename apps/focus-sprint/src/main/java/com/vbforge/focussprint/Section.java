package com.vbforge.focussprint;

public record Section(long id, String name, String tone, int sortOrder) {

    public Section withName(String newName) {
        return new Section(id, newName, tone, sortOrder);
    }
}