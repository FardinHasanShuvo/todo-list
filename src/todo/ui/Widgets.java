package todo.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/** Text field that shows a prompt, plus the flat buttons used in the window. */
final class PromptField extends JTextField {
    private final String prompt;

    PromptField(String prompt, int columns) {
        super(columns);
        this.prompt = prompt;
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent event) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent event) {
                repaint();
            }
        });
    }

    @Override
    public void updateUI() {
        super.updateUI();
        Inputs.style(this);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (!getText().isEmpty() || isFocusOwner()) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Theme.MUTED);
        g.setFont(getFont());
        FontMetrics metrics = g.getFontMetrics();
        Insets insets = getInsets();
        int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(prompt, insets.left, y);
        g.dispose();
    }
}

final class Inputs {
    private Inputs() {
    }

    static void style(JTextField field) {
        field.setFont(Theme.ui(14));
        field.setForeground(Theme.INK);
        field.setCaretColor(Theme.INK);
        field.setBackground(Theme.SURFACE);
        field.setSelectionColor(Theme.ACCENT_SOFT);
        field.setSelectedTextColor(Theme.INK);
        field.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.LINE, 1, true),
                new EmptyBorder(8, 10, 8, 10)));
    }
}

final class FlatButton extends JButton {
    enum Kind { PRIMARY, NORMAL, DANGER }

    private final Kind kind;

    FlatButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(Theme.ui(13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        int width = Math.max(72, metrics.stringWidth(getText()) + 36);
        return new Dimension(width, 36);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean hot = isEnabled() && (getModel().isRollover() || (getModel().isArmed() && getModel().isPressed()));
        Color fill;
        Color text;
        if (!isEnabled()) {
            fill = Theme.BG;
            text = Theme.MUTED;
        } else if (kind == Kind.PRIMARY) {
            fill = hot ? Theme.ACCENT_HOVER : Theme.ACCENT;
            text = Color.WHITE;
        } else if (kind == Kind.DANGER) {
            fill = hot ? Theme.DANGER_HOVER : Theme.DANGER;
            text = Color.WHITE;
        } else {
            fill = hot ? Theme.HOVER : Theme.SURFACE;
            text = Theme.INK;
        }
        g.setColor(fill);
        g.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);
        if (!isEnabled() || kind == Kind.NORMAL) {
            g.setColor(Theme.LINE);
            g.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);
        }
        g.setFont(getFont());
        g.setColor(text);
        FontMetrics metrics = g.getFontMetrics();
        int x = (w - metrics.stringWidth(getText())) / 2;
        int y = (h - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(getText(), x, y);
        if (isFocusOwner()) {
            g.setColor(Theme.ACCENT);
            g.drawRoundRect(1, 1, w - 4, h - 4, 10, 10);
        }
        g.dispose();
    }
}

final class FilterChip extends JToggleButton {
    FilterChip(String text) {
        super(text);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setFont(Theme.ui(13));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setMargin(new Insets(0, 0, 0, 0));
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(getFont());
        return new Dimension(metrics.stringWidth(getText()) + 28, 32);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        boolean hot = getModel().isRollover();
        if (isSelected()) {
            g.setColor(Theme.ACCENT_SOFT);
        } else if (hot) {
            g.setColor(Theme.SURFACE);
        } else {
            g.setColor(Theme.BG);
        }
        g.fillRoundRect(0, 0, w - 1, h - 1, 16, 16);
        g.setColor(isSelected() || isFocusOwner() ? Theme.ACCENT : Theme.LINE);
        g.drawRoundRect(0, 0, w - 1, h - 1, 16, 16);
        g.setFont(getFont());
        g.setColor(isSelected() ? Theme.ACCENT : Theme.INK);
        FontMetrics metrics = g.getFontMetrics();
        int x = (w - metrics.stringWidth(getText())) / 2;
        int y = (h - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(getText(), x, y);
        g.dispose();
    }
}
