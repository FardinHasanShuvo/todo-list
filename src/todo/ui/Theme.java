package todo.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;

/** Colors, type, and the window icon. */
final class Theme {
    static final Color BG = new Color(0xF4F1EA);
    static final Color SURFACE = new Color(0xFFFDF8);
    static final Color HOVER = new Color(0xF3F1EC);
    static final Color INK = new Color(0x1C1917);
    static final Color MUTED = new Color(0x78716C);
    static final Color LINE = new Color(0xE7E5E4);
    static final Color ACCENT = new Color(0x0F766E);
    static final Color ACCENT_SOFT = new Color(0xCCFBF1);
    static final Color ACCENT_HOVER = new Color(0x115E59);
    static final Color DANGER = new Color(0xB91C1C);
    static final Color DANGER_HOVER = new Color(0x991B1B);
    static final Color HIGH = new Color(0xB45309);

    private static final String FAMILY = "Segoe UI";

    private Theme() {
    }

    static Font ui(int size) {
        return new Font(FAMILY, Font.PLAIN, size);
    }

    static Font uiBold(int size) {
        return new Font(FAMILY, Font.BOLD, size);
    }

    static List<Image> icons() {
        return List.of(icon(16), icon(32), icon(64));
    }

    private static Image icon(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ACCENT);
        int arc = Math.max(4, size / 4);
        g.fillRoundRect(0, 0, size - 1, size - 1, arc, arc);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(Math.max(2f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x1 = Math.round(size * 0.26f);
        int y1 = Math.round(size * 0.52f);
        int x2 = Math.round(size * 0.42f);
        int y2 = Math.round(size * 0.70f);
        int x3 = Math.round(size * 0.76f);
        int y3 = Math.round(size * 0.32f);
        g.drawLine(x1, y1, x2, y2);
        g.drawLine(x2, y2, x3, y3);
        g.dispose();
        return image;
    }
}
