package com.vbforge.taskflow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Custom-painted Kanban board.
 *
 * Everything is drawn by hand: columns, cards, drag feedback, drop
 * indicator. That gives us full control over the visuals and makes the
 * drag experience feel right, at the cost of doing hit-testing ourselves.
 *
 * Layout constants are all in pixels. Positions are computed on demand
 * from card index, so no position cache to keep in sync.
 */
public final class BoardView extends JPanel {

    // ---- layout constants ----
    static final int BOARD_PADDING    = 20;
    static final int COLUMN_GAP       = 16;
    static final int COLUMN_MAX_W     = 260;
    static final int COLUMN_MIN_W     = 180;
    static final int COLUMN_PADDING   = 12;
    static final int HEADER_HEIGHT    = 40;
    static final int CARD_HEIGHT      = 72;
    static final int CARD_GAP         = 10;
    static final int CARD_RADIUS      = 8;
    static final int COLUMN_RADIUS    = 12;
    static final int DRAG_THRESHOLD   = 5;

    // ---- palette, matching the rest of the portfolio ----
    static final Color BG        = new Color(0x0d1117);
    static final Color COL_BG    = new Color(0x161b22);
    static final Color COL_HDR   = new Color(0x1c2129);
    static final Color CARD_BG   = new Color(0x1c2129);
    static final Color CARD_HOV  = new Color(0x242b36);
    static final Color BORDER    = new Color(0x2a313c);
    static final Color TEXT      = new Color(0xe6edf3);
    static final Color TEXT_DIM  = new Color(0x8b949e);
    static final Color ACCENT    = new Color(0xf89820);
    static final Color DROP_LINE = new Color(0x6ea8d8);

    private final BoardModel model;

    // Interaction callbacks — set by the app.
    private Consumer<Long> onAddCard     = id -> {};
    private Consumer<Long> onDeleteCard  = id -> {};
    private Consumer<Long> onEditCard    = id -> {};
    private BiConsumer<MoveCardCommand, Boolean> onMoveRequested = (c, b) -> {};

    // ---- drag state ----
    private long   draggingCardId = -1;
    private long   dragSourceCol  = -1;
    private int    dragSourceIdx  = -1;
    private Point  pressPoint;
    private Point  dragPoint;
    private boolean dragging;
    private int    dropTargetCol  = -1;
    private int    dropTargetIdx  = -1;

    // ---- hover state ----
    private long hoverCardId = -1;

    public BoardView(BoardModel model) {
        this.model = model;
        setBackground(BG);
        setOpaque(true);
        setFocusable(true);

        model.addChangeListener(this::repaint);
        installMouseHandlers();
    }

    // ---- callbacks ----

    public void setOnAddCard(Consumer<Long> c)    { this.onAddCard = c; }
    public void setOnDeleteCard(Consumer<Long> c) { this.onDeleteCard = c; }
    public void setOnEditCard(Consumer<Long> c)   { this.onEditCard = c; }
    public void setOnMoveRequested(BiConsumer<MoveCardCommand, Boolean> c) {
        this.onMoveRequested = c;
    }

    // ---- layout maths ----

    private int columnWidth() {
        int n = model.columns().size();
        if (n == 0) return COLUMN_MAX_W;
        int available = getWidth() - BOARD_PADDING * 2 - COLUMN_GAP * (n - 1);
        int w = available / n;
        return Math.max(COLUMN_MIN_W, Math.min(COLUMN_MAX_W, w));
    }

    private int columnX(int index) {
        return BOARD_PADDING + index * (columnWidth() + COLUMN_GAP);
    }

    private int cardY(int columnTop, int indexInColumn) {
        return columnTop + HEADER_HEIGHT + COLUMN_PADDING
                + indexInColumn * (CARD_HEIGHT + CARD_GAP);
    }

    /** The top of a column's interior. */
    private int columnTop() {
        return BOARD_PADDING;
    }

    private int columnBottom() {
        return getHeight() - BOARD_PADDING;
    }

    // ---- hit testing ----

    private int columnIndexAt(int mx) {
        int w = columnWidth();
        for (int i = 0; i < model.columns().size(); i++) {
            int x = columnX(i);
            if (mx >= x && mx <= x + w) return i;
        }
        return -1;
    }

    /** The card at the given screen point, or null. */
    private Card cardAt(Point p) {
        int colIdx = columnIndexAt(p.x);
        if (colIdx < 0) return null;

        Column col = model.columns().get(colIdx);
        List<Card> cards = model.cardsIn(col.id());
        int top = columnTop();

        for (int i = 0; i < cards.size(); i++) {
            int y = cardY(top, i);
            int w = columnWidth();
            int x = columnX(colIdx) + COLUMN_PADDING;
            int cw = w - COLUMN_PADDING * 2;

            if (p.x >= x && p.x <= x + cw && p.y >= y && p.y <= y + CARD_HEIGHT) {
                return cards.get(i);
            }
        }
        return null;
    }

    /**
     * Given a screen point, return the column index and insert position
     * where a dragged card should drop. Returns null if outside any column.
     */
    private int[] dropTarget(Point p) {
        int colIdx = columnIndexAt(p.x);
        if (colIdx < 0) return null;

        Column col = model.columns().get(colIdx);
        List<Card> cards = model.cardsIn(col.id());
        int top = columnTop();

        // Find the insertion index by comparing against card midpoints.
        int insertAt = cards.size();
        for (int i = 0; i < cards.size(); i++) {
            int midY = cardY(top, i) + CARD_HEIGHT / 2;
            if (p.y < midY) { insertAt = i; break; }
        }
        return new int[]{colIdx, insertAt};
    }

    // ---- mouse ----

    private void installMouseHandlers() {
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleRightClick(e);
                    return;
                }
                pressPoint = e.getPoint();
                dragging = false;

                Card c = cardAt(e.getPoint());
                if (c != null) {
                    draggingCardId = c.id();
                    dragSourceCol  = c.columnId();
                    dragSourceIdx  = indexOfInColumn(c);
                } else {
                    draggingCardId = -1;
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (dragging && draggingCardId >= 0) {
                    commitDrag(e.getPoint());
                }
                resetDragState();
                repaint();
            }

            @Override public void mouseExited(MouseEvent e) {
                hoverCardId = -1;
                repaint();
            }

            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Card c = cardAt(e.getPoint());
                    if (c != null) onEditCard.accept(c.id());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                Card c = cardAt(e.getPoint());
                long newHover = c == null ? -1 : c.id();
                if (newHover != hoverCardId) {
                    hoverCardId = newHover;
                    repaint();
                }
            }

            @Override public void mouseDragged(MouseEvent e) {
                if (draggingCardId < 0) return;

                if (!dragging && pressPoint != null) {
                    int dx = Math.abs(e.getX() - pressPoint.x);
                    int dy = Math.abs(e.getY() - pressPoint.y);
                    if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
                        dragging = true;
                    }
                }
                if (dragging) {
                    dragPoint = e.getPoint();
                    int[] t = dropTarget(dragPoint);
                    dropTargetCol = t == null ? -1 : t[0];
                    dropTargetIdx = t == null ? -1 : t[1];
                    repaint();
                }
            }
        });
    }

    private int indexOfInColumn(Card c) {
        List<Card> cards = model.cardsIn(c.columnId());
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).id() == c.id()) return i;
        }
        return -1;
    }

    private void handleRightClick(MouseEvent e) {
        Card c = cardAt(e.getPoint());
        if (c == null) return;

        JPopupMenu menu = new JPopupMenu();
        menu.setBackground(COL_HDR);
        menu.setBorder(BorderFactory.createLineBorder(BORDER));

        JMenuItem edit   = menuItem("Edit…");
        JMenuItem delete = menuItem("Delete");

        edit.addActionListener(a -> onEditCard.accept(c.id()));
        delete.addActionListener(a -> onDeleteCard.accept(c.id()));

        menu.add(edit);
        menu.add(delete);
        menu.show(this, e.getX(), e.getY());
    }

    private static JMenuItem menuItem(String text) {
        JMenuItem i = new JMenuItem(text);
        i.setBackground(COL_HDR);
        i.setForeground(TEXT);
        return i;
    }

    private void resetDragState() {
        draggingCardId = -1;
        dragSourceCol  = -1;
        dragSourceIdx  = -1;
        dragPoint      = null;
        pressPoint     = null;
        dragging       = false;
        dropTargetCol  = -1;
        dropTargetIdx  = -1;
    }

    private void commitDrag(Point release) {
        int[] target = dropTarget(release);
        if (target == null) return;

        Column targetCol = model.columns().get(target[0]);
        long toColumnId  = targetCol.id();
        int  toIndex     = target[1];

        // Dropping onto the same slot: no-op.
        if (toColumnId == dragSourceCol && toIndex == dragSourceIdx) return;
        // Dropping "just below itself" in the same column means dropping in place.
        if (toColumnId == dragSourceCol && toIndex == dragSourceIdx + 1) return;

        MoveCardCommand cmd = new MoveCardCommand(
                model, draggingCardId,
                dragSourceCol, dragSourceIdx,
                toColumnId, toIndex);
        onMoveRequested.accept(cmd, true);
    }

    // ---- painting ----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        List<Column> cols = model.columns();
        for (int i = 0; i < cols.size(); i++) {
            paintColumn(g2, i, cols.get(i));
        }

        if (dragging && draggingCardId >= 0) {
            paintDropIndicator(g2);
            paintDraggedCard(g2);
        }

        g2.dispose();
    }

    private void paintColumn(Graphics2D g2, int colIdx, Column col) {
        int x = columnX(colIdx);
        int y = columnTop();
        int w = columnWidth();
        int h = columnBottom() - columnTop();

        // Column body
        g2.setColor(COL_BG);
        g2.fillRoundRect(x, y, w, h, COLUMN_RADIUS, COLUMN_RADIUS);

        // Header bar
        g2.setColor(COL_HDR);
        g2.fillRoundRect(x, y, w, HEADER_HEIGHT, COLUMN_RADIUS, COLUMN_RADIUS);
        g2.fillRect(x, y + HEADER_HEIGHT - COLUMN_RADIUS, w, COLUMN_RADIUS);

        // Border
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, h, COLUMN_RADIUS, COLUMN_RADIUS);

        // Header text and count
        List<Card> cards = model.cardsIn(col.id());
        g2.setColor(TEXT);
        g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics fm = g2.getFontMetrics();
        int ty = y + (HEADER_HEIGHT + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(col.name(), x + 12, ty);

        String count = String.valueOf(cards.size());
        g2.setColor(TEXT_DIM);
        g2.setFont(getFont().deriveFont(Font.PLAIN, 12f));
        int cw = g2.getFontMetrics().stringWidth(count);
        g2.drawString(count, x + w - 12 - cw - 24, ty);

        // The "+" button
        int btnSize = 22;
        int bx = x + w - 12 - btnSize;
        int by = y + (HEADER_HEIGHT - btnSize) / 2;
        g2.setColor(TEXT_DIM);
        g2.setFont(getFont().deriveFont(Font.BOLD, 15f));
        FontMetrics bfm = g2.getFontMetrics();
        int plusX = bx + (btnSize - bfm.stringWidth("+")) / 2;
        int plusY = by + (btnSize + bfm.getAscent() - bfm.getDescent()) / 2;
        g2.drawString("+", plusX, plusY);

        // Cards
        for (int i = 0; i < cards.size(); i++) {
            // Skip the card currently being dragged — leave a placeholder slot.
            if (dragging && cards.get(i).id() == draggingCardId) {
                paintPlaceholder(g2, colIdx, i);
                continue;
            }
            paintCard(g2, cards.get(i), colIdx, i, false);
        }
    }

    private void paintPlaceholder(Graphics2D g2, int colIdx, int cardIdx) {
        int x = columnX(colIdx) + COLUMN_PADDING;
        int y = cardY(columnTop(), cardIdx);
        int w = columnWidth() - COLUMN_PADDING * 2;

        g2.setColor(new Color(0x2a313c));
        g2.setStroke(new BasicStroke(1.5f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1f, new float[]{6f, 4f}, 0f));
        g2.drawRoundRect(x, y, w, CARD_HEIGHT, CARD_RADIUS, CARD_RADIUS);
    }

    private void paintCard(Graphics2D g2, Card c, int colIdx, int cardIdx, boolean ghost) {
        int x = columnX(colIdx) + COLUMN_PADDING;
        int y = cardY(columnTop(), cardIdx);
        int w = columnWidth() - COLUMN_PADDING * 2;

        boolean hovered = !ghost && c.id() == hoverCardId;

        g2.setColor(hovered ? CARD_HOV : CARD_BG);
        g2.fillRoundRect(x, y, w, CARD_HEIGHT, CARD_RADIUS, CARD_RADIUS);

        // Accent stripe on the left edge
        g2.setColor(ACCENT);
        g2.fillRoundRect(x, y, 4, CARD_HEIGHT, 4, 4);

        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, CARD_HEIGHT, CARD_RADIUS, CARD_RADIUS);

        // Title
        g2.setColor(TEXT);
        g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics fm = g2.getFontMetrics();
        int textX = x + 14;
        int textW = w - 22;
        String title = ellipsise(c.title(), fm, textW);
        g2.drawString(title, textX, y + 26);

        // Description (dim, one line)
        if (!c.description().isBlank()) {
            g2.setColor(TEXT_DIM);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            FontMetrics dfm = g2.getFontMetrics();
            String desc = ellipsise(c.description(), dfm, textW);
            g2.drawString(desc, textX, y + 50);
        }
    }

    private void paintDropIndicator(Graphics2D g2) {
        if (dropTargetCol < 0 || dropTargetIdx < 0) return;

        Column col = model.columns().get(dropTargetCol);
        List<Card> cards = model.cardsIn(col.id());
        int idx = Math.min(dropTargetIdx, cards.size());

        int x = columnX(dropTargetCol) + COLUMN_PADDING;
        int w = columnWidth() - COLUMN_PADDING * 2;
        int y;

        if (idx >= cards.size()) {
            if (cards.isEmpty()) {
                y = columnTop() + HEADER_HEIGHT + COLUMN_PADDING;
            } else {
                y = cardY(columnTop(), cards.size() - 1) + CARD_HEIGHT + CARD_GAP / 2;
            }
        } else {
            y = cardY(columnTop(), idx) - CARD_GAP / 2;
        }

        g2.setColor(DROP_LINE);
        g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x, y, x + w, y);

        // Small circles at the ends for a designed look
        g2.fillOval(x - 4, y - 4, 8, 8);
        g2.fillOval(x + w - 4, y - 4, 8, 8);
    }

    private void paintDraggedCard(Graphics2D g2) {
        Card c = model.cardById(draggingCardId);
        if (c == null || dragPoint == null) return;

        int w = columnWidth() - COLUMN_PADDING * 2;
        int x = dragPoint.x - w / 2;
        int y = dragPoint.y - CARD_HEIGHT / 2;

        // Shadow
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRoundRect(x + 3, y + 6, w, CARD_HEIGHT, CARD_RADIUS, CARD_RADIUS);

        // Card
        g2.setColor(CARD_BG);
        g2.fillRoundRect(x, y, w, CARD_HEIGHT, CARD_RADIUS, CARD_RADIUS);

        g2.setColor(ACCENT);
        g2.fillRoundRect(x, y, 4, CARD_HEIGHT, 4, 4);

        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w, CARD_HEIGHT, CARD_RADIUS, CARD_RADIUS);

        g2.setColor(TEXT);
        g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(ellipsise(c.title(), fm, w - 22), x + 14, y + 26);

        if (!c.description().isBlank()) {
            g2.setColor(TEXT_DIM);
            g2.setFont(getFont().deriveFont(Font.PLAIN, 11f));
            FontMetrics dfm = g2.getFontMetrics();
            g2.drawString(ellipsise(c.description(), dfm, w - 22), x + 14, y + 50);
        }
    }

    private static String ellipsise(String s, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(s) <= maxWidth) return s;
        String dots = "…";
        int dotsW = fm.stringWidth(dots);
        int lo = 0, hi = s.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (fm.stringWidth(s.substring(0, mid)) + dotsW <= maxWidth) lo = mid;
            else hi = mid - 1;
        }
        return s.substring(0, lo) + dots;
    }

    // ---- public helper for the app to detect "+" clicks ----

    /**
     * Returns the column whose "+" button was clicked, or -1.
     * Called from a mouse listener installed by the app.
     */
    public long columnPlusAt(Point p) {
        List<Column> cols = model.columns();
        for (int i = 0; i < cols.size(); i++) {
            int x = columnX(i);
            int y = columnTop();
            int w = columnWidth();
            int btnSize = 22;
            int bx = x + w - 12 - btnSize;
            int by = y + (HEADER_HEIGHT - btnSize) / 2;
            if (p.x >= bx && p.x <= bx + btnSize && p.y >= by && p.y <= by + btnSize) {
                return cols.get(i).id();
            }
        }
        return -1;
    }
}