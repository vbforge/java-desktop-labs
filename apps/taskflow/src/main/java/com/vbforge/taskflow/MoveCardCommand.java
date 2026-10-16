package com.vbforge.taskflow;

public final class MoveCardCommand implements Command {

    private final BoardModel model;
    private final long cardId;
    private final long fromColumnId;
    private final int fromIndex;
    private final long toColumnId;
    private final int toIndex;

    public MoveCardCommand(BoardModel model, long cardId,
                           long fromColumnId, int fromIndex,
                           long toColumnId, int toIndex) {
        this.model = model;
        this.cardId = cardId;
        this.fromColumnId = fromColumnId;
        this.fromIndex = fromIndex;
        this.toColumnId = toColumnId;
        this.toIndex = toIndex;
    }

    @Override public void execute() {
        model.moveCard(cardId, toColumnId, toIndex);
    }

    @Override public void undo() {
        model.moveCard(cardId, fromColumnId, fromIndex);
    }

    @Override public String label() {
        Card c = model.cardById(cardId);
        return "Move card: " + (c == null ? "?" : c.title());
    }
}