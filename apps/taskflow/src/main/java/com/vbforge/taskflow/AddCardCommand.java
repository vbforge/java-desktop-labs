package com.vbforge.taskflow;

public final class AddCardCommand implements Command {

    private final BoardModel model;
    private final long columnId;
    private final long cardId;
    private final String title;
    private final String description;
    private final int index;

    public AddCardCommand(BoardModel model, long columnId, long cardId,
                          String title, String description, int index) {
        this.model = model;
        this.columnId = columnId;
        this.cardId = cardId;
        this.title = title;
        this.description = description;
        this.index = index;
    }

    @Override public void execute() {
        model.addCard(new Card(cardId, columnId, title, description), index);
    }

    @Override public void undo() {
        model.removeCard(cardId);
    }

    @Override public String label() {
        return "Add card: " + title;
    }
}