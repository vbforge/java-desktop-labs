package com.vbforge.taskflow;

/**
 * A card in a column. Cards are immutable; moving or editing a card
 * produces a new instance via the withX methods.
 */
public record Card(long id, long columnId, String title, String description) {

    public Card withColumn(long newColumnId) {
        return new Card(id, newColumnId, title, description);
    }

    public Card withContent(String newTitle, String newDescription) {
        return new Card(id, columnId, newTitle, newDescription);
    }
}