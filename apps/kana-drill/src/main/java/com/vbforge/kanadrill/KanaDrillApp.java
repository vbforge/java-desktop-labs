package com.vbforge.kanadrill;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Kana Drill — spaced-repetition flashcards for hiragana and katakana.
 *
 * All UI runs on the Swing Event Dispatch Thread. Startup is wrapped in
 * SwingUtilities.invokeLater in main() — that's the Swing equivalent of
 * JavaFX's Platform.startUp, and the rule it enforces is the same: only
 * touch UI components from the EDT.
 */
public class KanaDrillApp extends JFrame {

    // ---- app colours, matching the rest of the portfolio ----
    private static final Color BG       = new Color(0x0d1117);
    private static final Color BG_ELEV  = new Color(0x161b22);
    private static final Color BG_ELEV2 = new Color(0x1c2129);
    private static final Color BORDER   = new Color(0x2a313c);
    private static final Color TEXT     = new Color(0xe6edf3);
    private static final Color TEXT_DIM = new Color(0x8b949e);
    private static final Color ACCENT   = new Color(0xf89820);
    private static final Color OK       = new Color(0x3fb950);
    private static final Color BAD      = new Color(0xf85149);

    private static final int ADVANCE_OK_MS    = 650;
    private static final int ADVANCE_WRONG_MS = 1300;

    private final StatsStore stats = StatsStore.open();
    private final QuizEngine quiz = new QuizEngine();

    private final JComboBox<String> modeBox = new JComboBox<>(
            new String[]{"Hiragana", "Katakana", "Both"});
    private final JLabel scoreLabel = new JLabel();
    private final KanaCard card = new KanaCard();
    private final JTextField answerField = new JTextField();
    private final JLabel feedbackLabel = new JLabel(" ");
    private final JLabel detailLabel = new JLabel(" ");

    private int sessionCorrect = 0;
    private int sessionWrong = 0;
    private Timer advanceTimer;   // single-shot, set per answer

    public KanaDrillApp() {
        super("Kana Drill");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(16, 16, 16, 16));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildStatus(), BorderLayout.SOUTH);

        setMinimumSize(new Dimension(760, 620));
        pack();
        setLocationRelativeTo(null);

        // Kick off with the first card.
        SwingUtilities.invokeLater(this::nextCard);
    }

    // ---------- layout ----------

    private JComponent buildToolbar() {
        styleCombo(modeBox);
        modeBox.addActionListener(e -> restart());

        JButton cheatsheet = button("Cheatsheet", this::showCheatsheet);
        JButton reset      = button("Reset progress", this::confirmReset);

        scoreLabel.setForeground(TEXT);
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD, 14f));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(BG);
        bar.setBorder(new EmptyBorder(0, 0, 14, 0));

        bar.add(label("Script"));
        bar.add(modeBox);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(cheatsheet);
        bar.add(reset);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(scoreLabel);

        refreshScore();
        return bar;
    }

    private JComponent buildCenter() {
        JPanel wrap = new JPanel();
        wrap.setBackground(BG);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));

        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.add(Box.createVerticalGlue());
        wrap.add(card);
        wrap.add(Box.createVerticalStrut(24));

        styleField(answerField);
        answerField.setMaximumSize(new Dimension(420, 42));
        answerField.setPreferredSize(new Dimension(420, 42));
        answerField.setAlignmentX(Component.CENTER_ALIGNMENT);
        answerField.addActionListener(e -> submitAnswer());
        wrap.add(answerField);
        wrap.add(Box.createVerticalStrut(14));

        feedbackLabel.setForeground(TEXT_DIM);
        feedbackLabel.setFont(feedbackLabel.getFont().deriveFont(14f));
        feedbackLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        wrap.add(feedbackLabel);
        wrap.add(Box.createVerticalGlue());

        return wrap;
    }

    private JComponent buildStatus() {
        detailLabel.setForeground(TEXT_DIM);
        detailLabel.setFont(detailLabel.getFont().deriveFont(12f));
        detailLabel.setBorder(new EmptyBorder(10, 0, 0, 0));

        JPanel s = new JPanel(new BorderLayout());
        s.setBackground(BG);
        s.add(detailLabel, BorderLayout.WEST);
        return s;
    }

    // ---------- quiz flow ----------

    private void nextCard() {
        List<Kana> pool = activePool();
        Kana k = quiz.next(pool, stats);
        if (k == null) return;

        card.setCharacter(k.character());
        answerField.setText("");
        answerField.setEnabled(true);
        answerField.requestFocusInWindow();
        feedbackLabel.setText(" ");
    }

    private void submitAnswer() {
        if (!answerField.isEnabled()) return;
        String input = answerField.getText().trim();
        if (input.isEmpty()) return;

        Kana current = quiz.current();
        if (current == null) return;

        boolean correct = quiz.check(input);
        stats.record(current.character(), correct);

        if (correct) {
            sessionCorrect++;
            card.flashCorrect();
            feedbackLabel.setForeground(OK);
            feedbackLabel.setText("✓ Correct — " + current.romaji());
        } else {
            sessionWrong++;
            card.flashWrong();
            feedbackLabel.setForeground(BAD);
            feedbackLabel.setText("✗ " + input + " — answer: " + current.romaji());
        }
        refreshScore();
        updateDetail(current);
        persistStats();

        answerField.setEnabled(false);
        scheduleAdvance(correct ? ADVANCE_OK_MS : ADVANCE_WRONG_MS);
    }

    private void scheduleAdvance(int delayMs) {
        if (advanceTimer != null && advanceTimer.isRunning()) advanceTimer.stop();
        advanceTimer = new Timer(delayMs, e -> nextCard());
        advanceTimer.setRepeats(false);
        advanceTimer.start();
    }

    private void restart() {
        if (advanceTimer != null) advanceTimer.stop();
        sessionCorrect = 0;
        sessionWrong = 0;
        refreshScore();
        detailLabel.setText(" ");
        nextCard();
    }

    private List<Kana> activePool() {
        String mode = (String) modeBox.getSelectedItem();
        List<Kana> pool = new ArrayList<>();
        if (mode == null || mode.equals("Hiragana") || mode.equals("Both"))
            pool.addAll(Kana.hiragana());
        if (mode == null || mode.equals("Katakana") || mode.equals("Both"))
            pool.addAll(Kana.katakana());
        return pool;
    }

    // ---------- derived views ----------

    private void refreshScore() {
        int total = sessionCorrect + sessionWrong;
        int acc = total == 0 ? 0 : (int) Math.round(100.0 * sessionCorrect / total);
        scoreLabel.setText(String.format(
                "Session: %d correct · %d wrong · %d%%",
                sessionCorrect, sessionWrong, acc));
        scoreLabel.setForeground(total > 0 && acc >= 70 ? OK : TEXT);
    }

    private void updateDetail(Kana k) {
        KanaStat s = stats.stat(k.character());
        detailLabel.setText(String.format(
                "%s — seen %d times, %d correct (%.0f%%), %d wrong",
                k.character(), s.attempts(), s.correct(),
                s.accuracy() * 100.0, s.wrong()));
    }

    private void persistStats() {
        try {
            stats.save();
        } catch (IOException e) {
            // Non-fatal — just note it in the status line.
            detailLabel.setText("Could not save progress: " + e.getMessage());
        }
    }

    // ---------- actions ----------

    private void confirmReset() {
        int r = JOptionPane.showConfirmDialog(
                this,
                "Erase all progress and start from zero?",
                "Reset progress",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        stats.clear();
        persistStats();
        restart();
    }

    private void showCheatsheet() {
        JDialog dlg = new JDialog(this, "Cheatsheet", true);
        dlg.getContentPane().setBackground(BG);

        JPanel grid = new JPanel(new GridLayout(0, 10, 6, 6));
        grid.setBackground(BG);
        grid.setBorder(new EmptyBorder(16, 16, 16, 16));

        List<Kana> all = new ArrayList<>();
        all.addAll(Kana.hiragana());
        all.addAll(Kana.katakana());

        for (Kana k : all) {
            JLabel l = new JLabel(
                    "<html><div style='text-align:center'>"
                    + "<span style='font-size:22px'>" + k.character() + "</span><br>"
                    + "<span style='color:#8b949e;font-size:10px'>" + k.romaji() + "</span>"
                    + "</div></html>",
                    SwingConstants.CENTER);
            l.setForeground(TEXT);
            l.setPreferredSize(new Dimension(56, 56));
            l.setOpaque(true);
            l.setBackground(BG_ELEV);
            l.setBorder(BorderFactory.createLineBorder(BORDER));
            grid.add(l);
        }

        JScrollPane scroller = new JScrollPane(grid);
        scroller.setBorder(null);
        scroller.getViewport().setBackground(BG);
        scroller.getVerticalScrollBar().setUnitIncrement(16);

        dlg.setContentPane(scroller);
        dlg.setSize(700, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private void shutdown() {
        persistStats();
        dispose();
        System.exit(0);
    }

    // ---------- styling helpers ----------

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_DIM);
        return l;
    }

    private static JButton button(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setForeground(TEXT);
        b.setBackground(BG_ELEV2);
        b.setOpaque(true);                    // ← new
        b.setContentAreaFilled(true);         // ← new
        b.setBorderPainted(false);            // ← new (we supply our own border)
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 14, 6, 14)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    private static void styleCombo(JComboBox<?> combo) {
        combo.setBackground(BG_ELEV2);
        combo.setForeground(TEXT);
        combo.setBorder(BorderFactory.createLineBorder(BORDER));
        combo.setFocusable(false);

        // Nimbus paints the closed combo and its popup list independently,
        // and won't reliably pick up our UIManager colours. A custom renderer
        // guarantees both are legible.
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                JLabel l = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                l.setOpaque(true);
                l.setBackground(isSelected ? new Color(0x1f3a5f) : BG_ELEV2);
                l.setForeground(TEXT);
                l.setBorder(new EmptyBorder(4, 8, 4, 8));
                return l;
            }
        });
    }

    private static void styleField(JTextField f) {
        f.setBackground(BG_ELEV);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(8, 12, 8, 12)));
        f.setFont(f.getFont().deriveFont(18f));
        f.setHorizontalAlignment(SwingConstants.CENTER);
    }

    // ---------- entry point ----------

    public static void main(String[] args) {
        installDarkUiDefaults();
        SwingUtilities.invokeLater(() -> new KanaDrillApp().setVisible(true));
    }

    /**
     * Swing has no CSS, so we set defaults in the UIManager before any
     * components are constructed.
     *
     * Nimbus is used instead of the system LAF because the Windows LAF
     * paints its own dropdowns and buttons and ignores most UIManager
     * colour keys. Nimbus is fully themable.
     */
    private static void installDarkUiDefaults() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // Fall back to whatever the JVM's default is.
        }

        // ---- Nimbus-specific keys (these are the ones Nimbus actually reads) ----
        UIManager.put("nimbusBase",                 new Color(0x161b22));
        UIManager.put("nimbusBlueGrey",             new Color(0x2a313c));
        UIManager.put("nimbusLightBackground",      new Color(0x161b22));
        UIManager.put("nimbusSelectionBackground",  new Color(0x1f3a5f));
        UIManager.put("nimbusSelectedText",         new Color(0xe6edf3));
        UIManager.put("nimbusFocus",                new Color(0xf89820));
        UIManager.put("nimbusOrange",               new Color(0xf89820));
        UIManager.put("nimbusDisabledText",         new Color(0x6e7681));

        // ---- Generic keys Nimbus honours ----
        UIManager.put("control",        new Color(0x161b22));
        UIManager.put("info",           new Color(0x1c2129));
        UIManager.put("text",           new Color(0xe6edf3));
        UIManager.put("background",     new Color(0x0d1117));
        UIManager.put("Label.foreground", new Color(0xe6edf3));
        UIManager.put("Panel.background", new Color(0x0d1117));

        // Text fields
        UIManager.put("TextField.background",       new Color(0x161b22));
        UIManager.put("TextField.foreground",       new Color(0xe6edf3));
        UIManager.put("TextField.caretForeground",  new Color(0xf89820));
        UIManager.put("TextField.inactiveForeground", new Color(0x6e7681));

        // ComboBox — the closed box and the dropdown list are styled separately
        UIManager.put("ComboBox.background",              new Color(0x1c2129));
        UIManager.put("ComboBox.foreground",              new Color(0xe6edf3));
        UIManager.put("ComboBox.selectionBackground",     new Color(0x1f3a5f));
        UIManager.put("ComboBox.selectionForeground",     new Color(0xe6edf3));
        UIManager.put("ComboBox.buttonBackground",        new Color(0x1c2129));
        UIManager.put("ComboBox.buttonDarkShadow",        new Color(0x2a313c));
        UIManager.put("ComboBox.buttonHighlight",         new Color(0x2a313c));
        UIManager.put("ComboBox.buttonShadow",            new Color(0x2a313c));
        UIManager.put("ComboBox.disabledBackground",      new Color(0x161b22));
        UIManager.put("ComboBox.disabledForeground",      new Color(0x6e7681));

        // The popup list inside the ComboBox
        UIManager.put("List.background",             new Color(0x1c2129));
        UIManager.put("List.foreground",             new Color(0xe6edf3));
        UIManager.put("List.selectionBackground",    new Color(0x1f3a5f));
        UIManager.put("List.selectionForeground",    new Color(0xe6edf3));
        UIManager.put("List.border",                 BorderFactory.createLineBorder(new Color(0x2a313c)));

        // Popup / scrollpane
        UIManager.put("PopupMenu.background",        new Color(0x1c2129));
        UIManager.put("PopupMenu.foreground",        new Color(0xe6edf3));
        UIManager.put("ScrollPane.background",       new Color(0x0d1117));

        // Buttons
        UIManager.put("Button.background",           new Color(0x1c2129));
        UIManager.put("Button.foreground",           new Color(0xe6edf3));
        UIManager.put("Button.disabledText",         new Color(0x6e7681));

        // Dialogs
        UIManager.put("OptionPane.background",       new Color(0x0d1117));
        UIManager.put("OptionPane.messageForeground", new Color(0xe6edf3));
        UIManager.put("Dialog.background",           new Color(0x0d1117));
    }
}