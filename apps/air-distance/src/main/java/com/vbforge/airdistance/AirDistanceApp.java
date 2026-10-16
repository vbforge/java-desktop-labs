package com.vbforge.airdistance;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Locale;

/**
 * Air Distance — Swing front end.
 *
 * Given a known distance already flown and the fraction of the trip that
 * represents, computes the remaining distance.
 */
public class AirDistanceApp {

    // ---- shared repo palette ----
    private static final Color BG       = new Color(0x0d1117);
    private static final Color BG_ELEV  = new Color(0x161b22);
    private static final Color BG_ELEV2 = new Color(0x1c2129);
    private static final Color BORDER   = new Color(0x2a313c);
    private static final Color TEXT     = new Color(0xe6edf3);
    private static final Color TEXT_DIM = new Color(0x8b949e);
    private static final Color ACCENT   = new Color(0xf89820);   // orange, primary
    private static final Color ACCENT_2 = new Color(0x6ea8d8);   // blue, informational
    private static final Color FIELD_BG = new Color(0x0a0d12);
    private static final Color ERROR    = new Color(0xf85149);

    private final Font monoFont;
    private final Font monoFontBold;
    private final Font monoFontLarge;

    private JTextField distanceField;
    private JTextField numeratorField;
    private JTextField denominatorField;
    private JLabel statusLabel;

    public AirDistanceApp() {
        monoFont      = resolveMonoFont(Font.PLAIN, 15);
        monoFontBold  = resolveMonoFont(Font.BOLD, 15);
        monoFontLarge = resolveMonoFont(Font.BOLD, 22);
    }

    public static void main(String[] args) {
        installDarkUiDefaults();
        SwingUtilities.invokeLater(() -> new AirDistanceApp().createUI());
    }

    /**
     * Nimbus is used instead of the system LAF because the Windows LAF
     * paints its own chrome and silently ignores most UIManager colour
     * keys, producing a half-styled window with light dropdowns and
     * text fields.
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
            // fall back to default LAF
        }

        UIManager.put("nimbusBase",                BG_ELEV);
        UIManager.put("nimbusBlueGrey",            BORDER);
        UIManager.put("nimbusLightBackground",     BG_ELEV);
        UIManager.put("nimbusSelectionBackground", new Color(0x1f3a5f));
        UIManager.put("nimbusSelectedText",        TEXT);
        UIManager.put("nimbusFocus",               ACCENT);
        UIManager.put("nimbusOrange",              ACCENT);

        UIManager.put("control",              BG_ELEV);
        UIManager.put("text",                 TEXT);
        UIManager.put("background",           BG);
        UIManager.put("Label.foreground",     TEXT);
        UIManager.put("Panel.background",     BG);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Button.background",    BG_ELEV2);
        UIManager.put("Button.foreground",    TEXT);
        UIManager.put("TextField.background", FIELD_BG);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", ACCENT);
    }

    /** JetBrains Mono if installed, else the platform's default monospace. */
    private Font resolveMonoFont(int style, int size) {
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String name : available) {
            if (name.equalsIgnoreCase("JetBrains Mono")) {
                return new Font("JetBrains Mono", style, size);
            }
        }
        return new Font(Font.MONOSPACED, style, size);
    }

    private void createUI() {
        JFrame frame = new JFrame("Air Distance");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 560);
        frame.setMinimumSize(new Dimension(460, 520));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        frame.setContentPane(root);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildCenter(),  BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(BG);

        JLabel title = new JLabel("AIR DISTANCE");
        title.setFont(monoFontLarge);
        title.setForeground(ACCENT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "remaining distance from a known distance + fraction flown");
        subtitle.setFont(monoFont);
        subtitle.setForeground(TEXT_DIM);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));

        header.add(title);
        header.add(subtitle);
        return header;
    }

    private JComponent buildCenter() {
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(BG);

        distanceField    = makeInputField();
        numeratorField   = makeInputField();
        denominatorField = makeInputField();

        center.add(buildInputCard("Known distance flown (km)", distanceField));
        center.add(Box.createVerticalStrut(15));
        center.add(buildFractionCard());

        JLabel formula = new JLabel(
                "<html>remaining = known &times; "
                + "(denominator &minus; numerator) / numerator</html>");
        formula.setFont(monoFontBold);
        formula.setForeground(ACCENT_2);
        formula.setAlignmentX(Component.LEFT_ALIGNMENT);
        formula.setBorder(new EmptyBorder(18, 0, 0, 0));
        center.add(formula);

        return center;
    }

    private JComponent buildInputCard(String labelText, JTextField field) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_ELEV);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel label = new JLabel(labelText);
        label.setFont(monoFont);
        label.setForeground(TEXT_DIM);

        card.add(label, BorderLayout.NORTH);
        card.add(field, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildFractionCard() {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(BG_ELEV);
        card.setBorder(new CompoundBorder(
                new LineBorder(BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        JLabel label = new JLabel("Fraction of trip already flown");
        label.setFont(monoFont);
        label.setForeground(TEXT_DIM);

        JPanel fractionRow = new JPanel(new GridLayout(1, 3, 8, 0));
        fractionRow.setBackground(BG_ELEV);
        fractionRow.add(numeratorField);

        JLabel slash = new JLabel("/", SwingConstants.CENTER);
        slash.setFont(monoFontLarge);
        slash.setForeground(TEXT);
        fractionRow.add(slash);
        fractionRow.add(denominatorField);

        card.add(label, BorderLayout.NORTH);
        card.add(fractionRow, BorderLayout.CENTER);
        return card;
    }

    private JTextField makeInputField() {
        JTextField field = new JTextField();
        field.setFont(monoFontLarge);
        field.setHorizontalAlignment(SwingConstants.CENTER);
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setBorder(new EmptyBorder(8, 8, 8, 8));
        return field;
    }

    private JComponent buildFooter() {
        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setBackground(BG);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        buttonRow.setBackground(BG);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton calculateBtn = styledButton("CALCULATE", ACCENT);
        calculateBtn.addActionListener(this::onCalculate);

        JButton clearBtn = styledButton("CLEAR", TEXT_DIM);
        clearBtn.addActionListener(e -> onClear());

        JButton exampleBtn = styledButton("LOAD EXAMPLE", ACCENT_2);
        exampleBtn.addActionListener(e -> onLoadExample());

        buttonRow.add(calculateBtn);
        buttonRow.add(clearBtn);
        buttonRow.add(exampleBtn);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(monoFontBold);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusLabel.setBorder(new EmptyBorder(14, 0, 0, 0));

        footer.add(buttonRow);
        footer.add(statusLabel);
        return footer;
    }

    private JButton styledButton(String text, Color accent) {
        JButton button = new JButton(text);
        button.setFont(monoFontBold);
        button.setForeground(accent);
        button.setBackground(BG_ELEV2);
        button.setFocusPainted(false);
        button.setBorder(new CompoundBorder(
                new LineBorder(accent, 1),
                new EmptyBorder(8, 16, 8, 16)));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    // ---- actions ----

    private void onCalculate(ActionEvent e) {
        double known, numerator, denominator;
        try {
            known       = parseRequired(distanceField.getText(), "known distance");
            numerator   = parseRequired(numeratorField.getText(), "numerator");
            denominator = parseRequired(denominatorField.getText(), "denominator");
        } catch (IllegalArgumentException ex) {
            setStatus("Invalid input! " + ex.getMessage(), ERROR);
            return;
        }

        try {
            double remaining = AirDistanceCalculator.remainingDistance(
                    known, numerator, denominator);
            setStatus(String.format(
                    Locale.US, "Remaining distance: %.2f km", remaining), ACCENT);
        } catch (ArithmeticException ex) {
            setStatus(ex.getMessage(), ERROR);
        }
    }

    private void onClear() {
        distanceField.setText("");
        numeratorField.setText("");
        denominatorField.setText("");
        setStatus(" ", TEXT_DIM);
    }

    private void onLoadExample() {
        onClear();
        distanceField.setText("100");
        numeratorField.setText("1");
        denominatorField.setText("2");
        setStatus("example loaded — press CALCULATE (expected: 100.00 km)", TEXT_DIM);
    }

    private double parseRequired(String rawText, String fieldName) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("'" + fieldName + "' is required.");
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "'" + fieldName + "' must be a number, got '" + text + "'.");
        }
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }
}