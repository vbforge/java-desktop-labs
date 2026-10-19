package com.vbforge.wordcounter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarPainter;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class MainFrame extends JFrame {

    private static final int TOP_N = 10;

    private static final Color BG        = new Color(0x0d1117);
    private static final Color BG_ELEV   = new Color(0x161b22);
    private static final Color BG_ELEV2  = new Color(0x1c2129);
    private static final Color BORDER    = new Color(0x2a313c);
    private static final Color TEXT      = new Color(0xe6edf3);
    private static final Color TEXT_DIM  = new Color(0x8b949e);
    private static final Color ACCENT    = new Color(0xf89820);
    private static final Color ACCENT_HOVER = new Color(0xffa93a);
    private static final Color ACCENT_SOFT  = new Color(49, 42, 34); // ACCENT blended into BG_ELEV

    private static final Font UI_FONT   = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font UI_BOLD   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font HEADER    = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font MONO      = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    private final TextAnalyzer analyzer = new TextAnalyzer();

    private final JTextArea textArea = new JTextArea();

    private final JLabel totalWordsLbl  = new JLabel("0");
    private final JLabel uniqueWordsLbl = new JLabel("0");
    private final JLabel longestWordLbl = new JLabel("-");
    private final JLabel avgLengthLbl   = new JLabel("0.00");

    private final JPanel chartHolder = new JPanel(new BorderLayout());
    private final JCheckBox stopWordsCheck = new JCheckBox("Ignore Common Words", true);
    private final FlatToggle liveToggle = new FlatToggle("Live Analysis: ON");

    private final Timer debounceTimer;

    public MainFrame() {
        super("Word Counter");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildMainContent(), BorderLayout.CENTER);

        debounceTimer = new Timer(300, e -> runAnalysis());
        debounceTimer.setRepeats(false);

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { triggerLive(); }
            public void removeUpdate(DocumentEvent e) { triggerLive(); }
            public void changedUpdate(DocumentEvent e) { triggerLive(); }
        });

        loadSampleText();
    }

    private void triggerLive() {
        if (liveToggle.isSelected()) {
            debounceTimer.restart();
        }
    }

    // ------------------------------------------------------------------ toolbar

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_ELEV);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(12, 16, 12, 16)));

        FlatButton loadBtn = new FlatButton("Load Sample", false);
        loadBtn.addActionListener(e -> loadSampleText());

        FlatButton openBtn = new FlatButton("Open .txt...", false);
        openBtn.addActionListener(e -> openFile());

        FlatButton analyzeBtn = new FlatButton("Analyze", true);
        analyzeBtn.addActionListener(e -> runAnalysis());

        liveToggle.setSelected(true);
        liveToggle.addActionListener(e -> {
            liveToggle.setText(liveToggle.isSelected() ? "Live Analysis: ON" : "Live Analysis: OFF");
            if (liveToggle.isSelected()) runAnalysis();
        });

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(loadBtn);
        left.add(openBtn);
        left.add(Box.createHorizontalStrut(8));
        left.add(analyzeBtn);
        left.add(liveToggle);

        stopWordsCheck.setOpaque(false);
        stopWordsCheck.setForeground(TEXT_DIM);
        stopWordsCheck.setFont(UI_FONT);
        stopWordsCheck.setFocusPainted(false);
        stopWordsCheck.addActionListener(e -> runAnalysis());

        bar.add(left, BorderLayout.WEST);
        bar.add(stopWordsCheck, BorderLayout.EAST);
        return bar;
    }

    // ------------------------------------------------------------------ content

    private JComponent buildMainContent() {
        JPanel main = new JPanel(new GridLayout(1, 2, 16, 0));
        main.setBackground(BG);
        main.setBorder(new EmptyBorder(16, 16, 16, 16));

        main.add(buildInputCard());
        main.add(buildRightColumn());
        return main;
    }

    private JComponent buildInputCard() {
        textArea.setFont(MONO);
        textArea.setBackground(BG);
        textArea.setForeground(TEXT);
        textArea.setCaretColor(ACCENT);
        textArea.setSelectionColor(new Color(0x5a3b10));
        textArea.setSelectedTextColor(TEXT);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBorder(new EmptyBorder(12, 12, 12, 12));

        JScrollPane scroll = new JScrollPane(textArea);
        scroll.setBorder(new LineBorder(BORDER, 1, true));
        scroll.getViewport().setBackground(BG);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), BG_ELEV, BORDER, 14);
        card.setBorder(new EmptyBorder(14, 16, 16, 16));
        card.add(createHeader("Input Text"), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildRightColumn() {
        JPanel right = new JPanel(new BorderLayout(0, 16));
        right.setOpaque(false);
        right.add(buildStatsCard(), BorderLayout.NORTH);
        right.add(buildChartCard(), BorderLayout.CENTER);
        return right;
    }

    private JComponent buildStatsCard() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.add(createStatBox("Total Words", totalWordsLbl));
        grid.add(createStatBox("Unique Words", uniqueWordsLbl));
        grid.add(createStatBox("Longest Word", longestWordLbl));
        grid.add(createStatBox("Avg Length", avgLengthLbl));

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), BG_ELEV, BORDER, 14);
        card.setBorder(new EmptyBorder(14, 16, 16, 16));
        card.add(createHeader("Statistics"), BorderLayout.NORTH);
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JComponent buildChartCard() {
        chartHolder.setOpaque(false);

        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10), BG_ELEV, BORDER, 14);
        card.setBorder(new EmptyBorder(14, 16, 12, 16));
        card.add(createHeader("Top Words"), BorderLayout.NORTH);
        card.add(chartHolder, BorderLayout.CENTER);
        return card;
    }

    private JLabel createHeader(String title) {
        JLabel lbl = new JLabel(title.toUpperCase());
        lbl.setForeground(TEXT_DIM);
        lbl.setFont(HEADER);
        return lbl;
    }

    private JPanel createStatBox(String title, JLabel valueLabel) {
        RoundedPanel box = new RoundedPanel(new BorderLayout(0, 4), BG, BORDER, 10);
        box.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setForeground(TEXT_DIM);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));

        valueLabel.setForeground(ACCENT);
        valueLabel.setFont(new Font(Font.MONOSPACED, Font.BOLD, 20));
        valueLabel.setHorizontalAlignment(SwingConstants.LEFT);

        box.add(titleLbl, BorderLayout.NORTH);
        box.add(valueLabel, BorderLayout.CENTER);
        return box;
    }

    // ------------------------------------------------------------------ actions

    private void loadSampleText() {
        try {
            textArea.setText(analyzer.readResource("/sample.txt"));
            textArea.setCaretPosition(0);
            runAnalysis();
        } catch (IOException ex) {
            showError("Could not load sample: " + ex.getMessage());
        }
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files", "txt"));

        UIManager.put("FileChooser.background", BG_ELEV);
        UIManager.put("FileChooser.foreground", TEXT);
        UIManager.put("FileChooser.listViewBackground", BG);
        UIManager.put("FileChooser.listViewForeground", TEXT);
        UIManager.put("FileChooser.listViewSelectionBackground", ACCENT);
        UIManager.put("FileChooser.listViewSelectionForeground", Color.BLACK);
        chooser.updateUI();

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Path p = chooser.getSelectedFile().toPath();
                textArea.setText(analyzer.readFile(p));
                textArea.setCaretPosition(0);
                runAnalysis();
            } catch (IOException ex) {
                showError("Could not read file: " + ex.getMessage());
            }
        }
    }

    private void runAnalysis() {
        boolean ignoreStop = stopWordsCheck.isSelected();
        WordStats stats = analyzer.analyze(textArea.getText(), ignoreStop);

        totalWordsLbl.setText(String.valueOf(stats.totalWords()));
        uniqueWordsLbl.setText(String.valueOf(stats.uniqueWords()));
        longestWordLbl.setText(stats.longestWord().isEmpty() ? "-" : stats.longestWord());
        avgLengthLbl.setText(String.format("%.2f", stats.averageWordLength()));

        renderChart(stats);
    }

    private void renderChart(WordStats stats) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        int i = 0;
        for (Map.Entry<String, Integer> entry : stats.frequencies().entrySet()) {
            if (i++ >= TOP_N) break;
            dataset.addValue(entry.getValue(), "Occurrences", entry.getKey());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null, "", "", dataset, PlotOrientation.VERTICAL, false, true, false);

        chart.setBackgroundPaint(BG_ELEV);
        chart.setBorderVisible(false);
        chart.setPadding(new RectangleInsets(0, 0, 0, 0));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(BG_ELEV);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);
        plot.setOutlineVisible(false);
        plot.setInsets(new RectangleInsets(8, 0, 0, 0));

        CategoryAxis xAxis = plot.getDomainAxis();
        xAxis.setTickLabelPaint(TEXT_DIM);
        xAxis.setTickLabelFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        xAxis.setAxisLineVisible(false);
        xAxis.setTickMarksVisible(false);
        xAxis.setCategoryLabelPositions(CategoryLabelPositions.STANDARD);
        xAxis.setMaximumCategoryLabelLines(1);
        xAxis.setCategoryMargin(0.25);
        xAxis.setLowerMargin(0.01);
        xAxis.setUpperMargin(0.01);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setVisible(false);
        yAxis.setRange(0, stats.frequencies().values().stream()
                .mapToInt(Integer::intValue).max().orElse(1) * 1.1);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ACCENT);
        renderer.setBarPainter(new RoundedBarPainter());
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setItemMargin(0.0);
        renderer.setMaximumBarWidth(0.12);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setOpaque(false);
        chartPanel.setBackground(BG_ELEV);
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.setPopupMenu(null);
        chartPanel.setBorder(BorderFactory.createEmptyBorder());

        chartHolder.removeAll();
        chartHolder.add(chartPanel, BorderLayout.CENTER);
        chartHolder.revalidate();
        chartHolder.repaint();
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------ custom components

    /** Panel with rounded background and border. */
    private static class RoundedPanel extends JPanel {
        private final Color fill;
        private final Color line;
        private final int arc;

        RoundedPanel(LayoutManager lm, Color fill, Color line, int arc) {
            super(lm);
            this.fill = fill;
            this.line = line;
            this.arc = arc;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.setColor(line);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Shared painting for flat buttons (bypasses Nimbus painters). */
    private static void paintFlat(Graphics g, AbstractButton b, Color fill, Color border, Color fg) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(fill);
        g2.fillRoundRect(0, 0, b.getWidth() - 1, b.getHeight() - 1, 10, 10);
        g2.setColor(border);
        g2.drawRoundRect(0, 0, b.getWidth() - 1, b.getHeight() - 1, 10, 10);

        g2.setFont(b.getFont());
        FontMetrics fm = g2.getFontMetrics();
        int x = (b.getWidth() - fm.stringWidth(b.getText())) / 2;
        int y = (b.getHeight() - fm.getHeight()) / 2 + fm.getAscent();
        g2.setColor(fg);
        g2.drawString(b.getText(), x, y);
        g2.dispose();
    }

    private static Dimension flatSize(AbstractButton b) {
        FontMetrics fm = b.getFontMetrics(b.getFont());
        return new Dimension(fm.stringWidth(b.getText()) + 36, 34);
    }

    private static class FlatButton extends JButton {
        private final boolean primary;

        FlatButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(UI_BOLD);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean hover = getModel().isRollover();
            boolean pressed = getModel().isPressed();
            if (primary) {
                paintFlat(g, this, (hover || pressed) ? ACCENT_HOVER : ACCENT, ACCENT, Color.BLACK);
            } else {
                paintFlat(g, this, hover ? BORDER : BG_ELEV2, hover ? TEXT_DIM : BORDER, TEXT);
            }
        }

        @Override
        public Dimension getPreferredSize() { return flatSize(this); }
    }

    private static class FlatToggle extends JToggleButton {
        FlatToggle(String text) {
            super(text);
            setFont(UI_BOLD);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (isSelected()) {
                paintFlat(g, this, ACCENT_SOFT, ACCENT, ACCENT);
            } else {
                paintFlat(g, this, BG_ELEV2, BORDER, TEXT_DIM);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            // fixed width so the toolbar doesn't jump between ON / OFF
            FontMetrics fm = getFontMetrics(getFont());
            int w = Math.max(fm.stringWidth("Live Analysis: ON"), fm.stringWidth("Live Analysis: OFF")) + 36;
            return new Dimension(w, 34);
        }
    }

    /** Bars with rounded top corners and a flat bottom. */
    private static class RoundedBarPainter implements BarPainter {
        @Override
        public void paintBar(Graphics2D g2, BarRenderer renderer, int row, int column,
                             RectangularShape bar, RectangleEdge base) {
            g2.setPaint(renderer.getItemPaint(row, column));
            double arc = Math.min(12, bar.getWidth() / 2);
            double h = Math.min(arc, bar.getHeight());

            Area shape = new Area(new RoundRectangle2D.Double(
                    bar.getX(), bar.getY(), bar.getWidth(), bar.getHeight(), arc, arc));
            shape.add(new Area(new Rectangle2D.Double(
                    bar.getX(), bar.getMaxY() - h, bar.getWidth(), h)));
            g2.fill(shape);
        }

        @Override
        public void paintBarShadow(Graphics2D g2, BarRenderer renderer, int row, int column,
                                   RectangularShape bar, RectangleEdge base, boolean pegShadow) {
            // no shadow
        }
    }
}