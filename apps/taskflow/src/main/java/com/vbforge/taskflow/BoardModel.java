package com.vbforge.taskflow;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory state of the board.
 *
 * Cards are stored in a single list; the order of that list is the
 * vertical order within each column. "Column order" is implicit in the
 * columns list. All mutations go through here, and every mutation fires
 * the change listener so the view can repaint.
 *
 * Deliberately knows nothing about Swing, so it is testable alone.
 */
public final class BoardModel {

    private final List<Column> columns = new ArrayList<>();
    private final List<Card> cards = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();

    private long nextId = 1;

    public long nextId() {
        return nextId++;
    }

    public void seedIfEmpty() {
        if (!columns.isEmpty()) return;

        Column backlog    = new Column(nextId(), "Backlog");
        Column inProgress = new Column(nextId(), "In Progress");
        Column done       = new Column(nextId(), "Done");

        columns.add(backlog);
        columns.add(inProgress);
        columns.add(done);

        cards.add(new Card(nextId(), backlog.id(),    "Fix login bug",
                "Users report intermittent failures on Safari"));
        cards.add(new Card(nextId(), backlog.id(),    "Update docs",
                ""));
        cards.add(new Card(nextId(), inProgress.id(), "Deploy to staging",
                "Awaiting QA sign-off"));
        cards.add(new Card(nextId(), done.id(),       "Set up CI",
                ""));
    }

    // ---- listeners ----

    public void addChangeListener(Runnable r) {
        listeners.add(r);
    }

    public void fireChanged() {
        for (Runnable r : listeners) r.run();
    }

    // ---- reads ----

    public List<Column> columns() {
        return List.copyOf(columns);
    }

    public Column columnById(long id) {
        for (Column c : columns) if (c.id() == id) return c;
        return null;
    }

    public Card cardById(long id) {
        for (Card c : cards) if (c.id() == id) return c;
        return null;
    }

    /** Cards in a column, in display order. */
    public List<Card> cardsIn(long columnId) {
        List<Card> out = new ArrayList<>();
        for (Card c : cards) if (c.columnId() == columnId) out.add(c);
        return out;
    }

    public int indexOf(long cardId) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id() == cardId) return i;
        }
        return -1;
    }

    // ---- mutations (called from commands) ----

    public void addColumn(Column col) {
        columns.add(col);
    }

    public void removeColumn(long columnId) {
        columns.removeIf(c -> c.id() == columnId);
        // Also drop the cards in it — otherwise they become orphans.
        cards.removeIf(c -> c.columnId() == columnId);
    }

    public void addCard(Card card, int indexInColumn) {
        List<Card> existing = cardsIn(card.columnId());
        if (indexInColumn < 0 || indexInColumn >= existing.size()) {
            // Append: insert after the last card of this column, or at the
            // end of the whole list if none exist.
            if (existing.isEmpty()) {
                cards.add(card);
            } else {
                Card last = existing.get(existing.size() - 1);
                int pos = indexOf(last.id());
                cards.add(pos + 1, card);
            }
        } else {
            Card ref = existing.get(indexInColumn);
            int pos = indexOf(ref.id());
            cards.add(pos, card);
        }
    }

    public void removeCard(long cardId) {
        cards.removeIf(c -> c.id() == cardId);
    }

    public void replaceCard(Card updated) {
        int idx = indexOf(updated.id());
        if (idx >= 0) cards.set(idx, updated);
    }

    /**
     * Move a card to a specific column and position within it.
     * Positions are 0-based indices within the target column's existing
     * cards. Position equal to that column's size means "at the end".
     */
    public void moveCard(long cardId, long targetColumnId, int targetIndex) {
        Card card = cardById(cardId);
        if (card == null) return;

        // Remove from the flat list first.
        cards.removeIf(c -> c.id() == cardId);

        Card moved = card.withColumn(targetColumnId);

        List<Card> target = cardsIn(targetColumnId);
        if (targetIndex < 0) targetIndex = 0;
        if (targetIndex > target.size()) targetIndex = target.size();

        if (target.isEmpty()) {
            cards.add(moved);
        } else if (targetIndex == target.size()) {
            Card last = target.get(target.size() - 1);
            int pos = indexOf(last.id());
            cards.add(pos + 1, moved);
        } else {
            Card ref = target.get(targetIndex);
            int pos = indexOf(ref.id());
            cards.add(pos, moved);
        }
    }

    public void clear() {
        columns.clear();
        cards.clear();
        nextId = 1;
    }

    public void restoreNextId(long value) {
        nextId = Math.max(nextId, value);
    }
}