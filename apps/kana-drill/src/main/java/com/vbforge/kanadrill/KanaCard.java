package com.vbforge.kanadrill;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Custom-painted card that displays the current kana character.
 *
 * Has a green/red flash animation on answer — implemented with a Swing
 * Timer, which fires on the EDT. (Unlike java.util.Timer, javax.swing.Timer
 * delivers its event on the EDT, so it is safe to touch Swing state from
 * the handler. Getting this distinction wrong is the classic Swing bug.)
 */
final class KanaCard extends JPanel {

    private static final int CORNER = 24;
    private static final Color BG      = new Color(0x161b22);
    private static final Color BORDER  = new Color(0x2a313c);
    private static final Color FOREGND = new Color(0xe6edf3);
    private static final Color OK      = new Color(0x3fb950);
    private static final Color BAD     = new Color(0xf85149);

    private String character = "";
    private Color flashColor;
    private float flashAlpha = 0f;

    KanaCard() {
        setOpaque(false);
        setPreferredSize(new Dimension(420, 340));

        // Tick every 16ms ≈ 60fps while fading out the flash.
        Timer fade = new Timer(16, e -> {
            if (flashAlpha > 0f) {
                flashAlpha = Math.max(0f, flashAlpha - 0.05f);
                repaint();
            }
        });
        fade.start();
    }

    void setCharacter(String c) {
        this.character = c == null ? "" : c;
        this.flashAlpha = 0f;
        repaint();
    }

    void flashCorrect()  { flash(OK); }
    void flashWrong()    { flash(BAD); }

    private void flash(Color c) {
        this.flashColor = c;
        this.flashAlpha = 1f;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();

        // Background
        g2.setColor(BG);
        g2.fillRoundRect(0, 0, w - 1, h - 1, CORNER, CORNER);

        // Flash overlay
        if (flashAlpha > 0f && flashColor != null) {
            int a = (int) (flashAlpha * 70);
            g2.setColor(new Color(flashColor.getRed(), flashColor.getGreen(), flashColor.getBlue(), a));
            g2.fillRoundRect(0, 0, w - 1, h - 1, CORNER, CORNER);
        }

        // Border
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(0, 0, w - 1, h - 1, CORNER, CORNER);

        // The character itself
        if (!character.isEmpty()) {
            g2.setFont(chooseKanaFont(160));
            g2.setColor(FOREGND);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(character)) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2;
            g2.drawString(character, tx, ty);
        }

        g2.dispose();
    }

    /**
     * Pick the first installed Japanese-capable font, falling back to a
     * generic serif. Java's font subsystem will substitute a CJK-capable
     * font automatically when the chosen one can't render the glyph, but
     * the native CJK fonts render the kana much more cleanly.
     */
    private static Font chooseKanaFont(int size) {
        String[] preferred = {
            "Yu Gothic UI", "Yu Gothic", "Meiryo", "MS Gothic",
            "Hiragino Kaku Gothic Pro", "Hiragino Sans",
            "Noto Sans CJK JP", "Noto Sans JP", "Source Han Sans JP"
        };
        Set<String> installed = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames()));

        for (String fam : preferred) {
            if (installed.contains(fam)) return new Font(fam, Font.PLAIN, size);
        }
        // Let the JVM substitute — most systems have some CJK fallback.
        return new Font(Font.SERIF, Font.PLAIN, size);
    }
}