package com.vbforge.taskflow;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * TaskFlow — offline Kanban board.
 *
 * All UI on the Swing Event Dispatch Thread. All model mutations go
 * through the CommandStack, so undo/redo works without special-casing.
 */
public class TaskFlowApp extends JFrame {

    static final Color BG       = new Color(0x0d1117);
    static final Color BG_ELEV  = new Color(0x161b22);
    static final Color BG_ELEV2 = new Color(0x1c2129);
    static final Color BORDER   = new Color(0x2a313c);
    static final Color TEXT     = new Color(0xe6edf3);
    static final Color TEXT_DIM = new Color(0x8b949e);
    static final Color ACCENT   = new Color(0xf89820);

    private final BoardModel model = new BoardModel();
    private final CommandStack commands = new CommandStack();
    private final BoardStore store = BoardStore.open();

    private BoardView view;
    private JButton undoBtn, redoBtn, addColBtn;
    private JLabel status;

    public TaskFlowApp() {
        super("TaskFlow");

        store.load(model);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { shutdown(); }
        });

        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        ((JComponent) getContentPane()).setBorder(new EmptyBorder(12, 12, 12, 12));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildBoard(), BorderLayout.CENTER);
        add(buildStatus(), BorderLayout.SOUTH);

        commands.setOnChanged(this::refreshToolbar);
        refreshToolbar();

        installKeyboardShortcuts();

        setMinimumSize(new Dimension(720, 520));
        setSize(1000, 680);
        setLocationRelativeTo(null);
    }

    // ---- layout ----

    private JComponent buildToolbar() {
        addColBtn = button("New column", e -> addColumn());
        undoBtn   = button("Undo", e -> commands.undo());
        redoBtn   = button("Redo", e -> commands.redo());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(BG);
        bar.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("TaskFlow");
        title.setForeground(TEXT);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        bar.add(title);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(addColBtn);
        bar.add(undoBtn);
        bar.add(redoBtn);
        return bar;
    }

    private JComponent buildBoard() {
        view = new BoardView(model);
        view.setOnAddCard(this::addCard);
        view.setOnDeleteCard(this::deleteCard);
        view.setOnEditCard(this::editCard);
        view.setOnMoveRequested((cmd, push) -> {
            commands.execute(cmd);
            status.setText(cmd.label());
        });

        // Custom listener for the painted "+" button on each column header.
        view.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                long colId = view.columnPlusAt(e.getPoint());
                if (colId > 0) {
                    e.consume();
                    addCard(colId);
                }
            }
        });

        return view;
    }

    private JComponent buildStatus() {
        status = new JLabel(" ");
        status.setForeground(TEXT_DIM);
        status.setFont(status.getFont().deriveFont(12f));
        status.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel s = new JPanel(new BorderLayout());
        s.setBackground(BG);
        s.add(status, BorderLayout.WEST);
        return s;
    }

    // ---- actions ----

    private void addColumn() {
        String name = JOptionPane.showInputDialog(
                this, "Column name:", "New column", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;

        Column col = new Column(model.nextId(), name.trim());
        commands.execute(new Command() {
            @Override public void execute() { model.addColumn(col); }
            @Override public void undo()    { model.removeColumn(col.id()); }
            @Override public String label() { return "Add column: " + col.name(); }
        });
        status.setText("Added column: " + col.name());
    }

    private void addCard(long columnId) {
        JTextField titleField = new JTextField();
        JTextField descField  = new JTextField();
        styleField(titleField);
        styleField(descField);

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 6));
        form.setBackground(BG);
        form.add(label("Title"));
        form.add(titleField);
        form.add(label("Description (optional)"));
        form.add(descField);

        int r = JOptionPane.showConfirmDialog(this, form,
                "Add card", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String title = titleField.getText().trim();
        if (title.isEmpty()) return;
        String desc = descField.getText().trim();

        long id = model.nextId();
        int idx = model.cardsIn(columnId).size();
        AddCardCommand cmd = new AddCardCommand(model, columnId, id, title, desc, idx);
        commands.execute(cmd);
        status.setText(cmd.label());
    }

    private void deleteCard(long cardId) {
        Card c = model.cardById(cardId);
        if (c == null) return;

        int r = JOptionPane.showConfirmDialog(this,
                "Delete \"" + c.title() + "\"?",
                "Delete card",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        DeleteCardCommand cmd = new DeleteCardCommand(model, cardId);
        commands.execute(cmd);
        status.setText(cmd.label());
    }

    private void editCard(long cardId) {
        Card c = model.cardById(cardId);
        if (c == null) return;

        JTextField titleField = new JTextField(c.title());
        JTextField descField  = new JTextField(c.description());
        styleField(titleField);
        styleField(descField);

        JPanel form = new JPanel(new GridLayout(0, 1, 4, 6));
        form.setBackground(BG);
        form.add(label("Title"));
        form.add(titleField);
        form.add(label("Description"));
        form.add(descField);

        int r = JOptionPane.showConfirmDialog(this, form,
                "Edit card", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) return;

        String title = titleField.getText().trim();
        if (title.isEmpty()) return;
        String desc = descField.getText().trim();

        EditCardCommand cmd = new EditCardCommand(model, cardId, title, desc);
        commands.execute(cmd);
        status.setText(cmd.label());
    }

    private void refreshToolbar() {
        undoBtn.setEnabled(commands.canUndo());
        redoBtn.setEnabled(commands.canRedo());
    }

    private void installKeyboardShortcuts() {
        JRootPane rp = getRootPane();
        int menuMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
          .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask), "undo");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
          .put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuMask), "redo");
        rp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
          .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask | InputEvent.SHIFT_DOWN_MASK), "redo");

        rp.getActionMap().put("undo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { commands.undo(); }
        });
        rp.getActionMap().put("redo", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { commands.redo(); }
        });
    }

    private void shutdown() {
        try {
            store.save(model);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Could not save board: " + e.getMessage(),
                    "Save failed", JOptionPane.ERROR_MESSAGE);
        }
        dispose();
        System.exit(0);
    }

    // ---- styling helpers ----

    private static JButton button(String text, ActionListener a) {
        JButton b = new JButton(text);
        b.setForeground(TEXT);
        b.setBackground(BG_ELEV2);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 14, 6, 14)));
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(a);
        return b;
    }

    private static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_DIM);
        return l;
    }

    private static void styleField(JTextField f) {
        f.setBackground(BG_ELEV);
        f.setForeground(TEXT);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 8, 6, 8)));
    }

    // ---- entry ----

    public static void main(String[] args) {
        installDarkUiDefaults();
        SwingUtilities.invokeLater(() -> new TaskFlowApp().setVisible(true));
    }

    private static void installDarkUiDefaults() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) { }

        UIManager.put("nimbusBase",                new Color(0x161b22));
        UIManager.put("nimbusBlueGrey",            new Color(0x2a313c));
        UIManager.put("nimbusLightBackground",     new Color(0x161b22));
        UIManager.put("nimbusSelectionBackground", new Color(0x1f3a5f));
        UIManager.put("nimbusSelectedText",        new Color(0xe6edf3));
        UIManager.put("nimbusFocus",               new Color(0xf89820));
        UIManager.put("nimbusOrange",              new Color(0xf89820));

        UIManager.put("control",        new Color(0x161b22));
        UIManager.put("text",           new Color(0xe6edf3));
        UIManager.put("background",     new Color(0x0d1117));
        UIManager.put("Label.foreground", new Color(0xe6edf3));
        UIManager.put("Panel.background", new Color(0x0d1117));
        UIManager.put("OptionPane.background", new Color(0x0d1117));
        UIManager.put("OptionPane.messageForeground", new Color(0xe6edf3));
        UIManager.put("Button.background", new Color(0x1c2129));
        UIManager.put("Button.foreground", new Color(0xe6edf3));
        UIManager.put("TextField.background", new Color(0x161b22));
        UIManager.put("TextField.foreground", new Color(0xe6edf3));
        UIManager.put("TextField.caretForeground", new Color(0xf89820));
        UIManager.put("PopupMenu.background", new Color(0x1c2129));
        UIManager.put("PopupMenu.foreground", new Color(0xe6edf3));
        UIManager.put("MenuItem.background",  new Color(0x1c2129));
        UIManager.put("MenuItem.foreground",  new Color(0xe6edf3));
        UIManager.put("Dialog.background",    new Color(0x0d1117));
    }
}