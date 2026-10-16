package com.vbforge.taskflow;

public final class EditCardCommand implements Command {

    private final BoardModel model;
    private final long cardId;
    private final String oldTitle, oldDesc;
    private final String newTitle, newDesc;

    public EditCardCommand(BoardModel model, long cardId,
                           String newTitle, String newDesc) {
        this.model = model;
        this.cardId = cardId;
        Card c = model.cardById(cardId);
        this.oldTitle = c == null ? "" : c.title();
        this.oldDesc  = c == null ? "" : c.description();
        this.newTitle = newTitle;
        this.newDesc  = newDesc;
    }

    @Override public void execute() {
        Card c = model.cardById(cardId);
        if (c != null) model.replaceCard(c.withContent(newTitle, newDesc));
    }

    @Override public void undo() {
        Card c = model.cardById(cardId);
        if (c != null) model.replaceCard(c.withContent(oldTitle, oldDesc));
    }

    @Override public String label() {
        return "Edit card: " + newTitle;
    }
}