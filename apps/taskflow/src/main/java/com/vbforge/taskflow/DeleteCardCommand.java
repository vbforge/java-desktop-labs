package com.vbforge.taskflow;

public final class DeleteCardCommand implements Command {

    private final BoardModel model;
    private final Card card;
    private final int index;

    public DeleteCardCommand(BoardModel model, long cardId) {
        this.model = model;
        this.card = model.cardById(cardId);
        this.index = model.indexOf(cardId);
    }

    @Override public void execute() {
        if (card != null) model.removeCard(card.id());
    }

    @Override public void undo() {
        if (card != null) model.addCard(card, index);
    }

    @Override public String label() {
        return card == null ? "Delete card" : "Delete card: " + card.title();
    }
}