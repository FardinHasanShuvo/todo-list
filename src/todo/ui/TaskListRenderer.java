package todo.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import todo.model.Priority;
import todo.model.Task;

/** One row: a completion circle, the title, a note preview, the due date, and the priority. */
final class TaskListRenderer extends JPanel implements ListCellRenderer<Task> {
    private static final int CIRCLE_X = 28;
    private static final int CIRCLE_R = 9;

    private Task task;
    private boolean selected;
    private boolean hover;
    private LocalDate today = LocalDate.now();

    TaskListRenderer() {
        setOpaque(true);
    }

    static boolean circleHit(int localX, int localY, int cellHeight) {
        int dx = localX - CIRCLE_X;
        int dy = localY - cellHeight / 2;
        int reach = CIRCLE_R + 6;
        return dx * dx + dy * dy <= reach * reach;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends Task> list,
            Task value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        task = value;
        selected = isSelected;
        today = LocalDate.now();
        Object marker = list.getClientProperty("hoverRow");
        hover = !isSelected && marker instanceof Integer row && row == index;
        return this;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (task == null) {
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int width = getWidth();
            int height = getHeight();
            Color background = selected ? Theme.ACCENT_SOFT : hover ? Theme.HOVER : Theme.SURFACE;
            g.setColor(background);
            g.fillRect(0, 0, width, height);
            if (selected) {
                g.setColor(Theme.ACCENT);
                g.fillRect(0, 0, 4, height);
            }
            paintCircle(g, height);
            paintText(g, width, height);
            g.setColor(Theme.LINE);
            g.drawLine(16, height - 1, width - 16, height - 1);
        } finally {
            g.dispose();
        }
    }

    private void paintCircle(Graphics2D g, int height) {
        int cy = height / 2;
        if (task.done()) {
            g.setColor(Theme.ACCENT);
            g.fillOval(CIRCLE_X - CIRCLE_R, cy - CIRCLE_R, CIRCLE_R * 2, CIRCLE_R * 2);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(CIRCLE_X - 5, cy, CIRCLE_X - 1, cy + 4);
            g.drawLine(CIRCLE_X - 1, cy + 4, CIRCLE_X + 5, cy - 4);
            return;
        }
        g.setColor(task.overdue(today) ? Theme.DANGER : Theme.INK);
        g.setStroke(new BasicStroke(1.6f));
        g.drawOval(CIRCLE_X - CIRCLE_R, cy - CIRCLE_R, CIRCLE_R * 2, CIRCLE_R * 2);
    }

    private void paintText(Graphics2D g, int width, int height) {
        String preview = preview(task.notes());
        boolean hasNotes = !preview.isEmpty();
        Font titleFont = Theme.uiBold(14);
        Font metaFont = Theme.ui(12);
        g.setFont(titleFont);
        FontMetrics titleMetrics = g.getFontMetrics();
        int textX = 50;
        int right = width - 16;
        int titleMax = Math.max(24, right - 120 - textX);
        String title = clip(titleMetrics, task.title(), titleMax);
        int titleBaseline = hasNotes
                ? 30
                : (height + titleMetrics.getAscent() - titleMetrics.getDescent()) / 2;

        g.setColor(task.done() ? Theme.MUTED : Theme.INK);
        g.drawString(title, textX, titleBaseline);
        if (task.done()) {
            int mid = titleBaseline - titleMetrics.getAscent() / 3;
            g.drawLine(textX, mid, textX + titleMetrics.stringWidth(title), mid);
        }

        g.setFont(metaFont);
        FontMetrics metaMetrics = g.getFontMetrics();
        if (hasNotes) {
            g.setColor(Theme.MUTED);
            g.drawString(clip(metaMetrics, preview, titleMax), textX, 50);
        }

        String due = dueLabel();
        String priority = task.priority().label();
        int dueY = hasNotes ? 30 : titleBaseline - 8;
        int priorityY = due.isEmpty() ? titleBaseline : dueY + 18;
        if (!due.isEmpty()) {
            g.setColor(dueColor());
            g.drawString(due, right - metaMetrics.stringWidth(due), dueY);
        }
        g.setColor(task.done() ? Theme.MUTED : priorityColor(task.priority()));
        g.drawString(priority, right - metaMetrics.stringWidth(priority), priorityY);
    }

    private String dueLabel() {
        LocalDate due = task.due();
        if (due == null) {
            return "";
        }
        if (due.equals(today)) {
            return "Today";
        }
        if (due.equals(today.plusDays(1))) {
            return "Tomorrow";
        }
        if (due.equals(today.minusDays(1))) {
            return "Yesterday";
        }
        DateTimeFormatter format = due.getYear() == today.getYear()
                ? DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
                : DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault());
        return due.format(format);
    }

    private Color dueColor() {
        if (task.done()) {
            return Theme.MUTED;
        }
        if (task.overdue(today)) {
            return Theme.DANGER;
        }
        return Theme.INK;
    }

    private static Color priorityColor(Priority priority) {
        return switch (priority) {
            case HIGH -> Theme.HIGH;
            case MEDIUM -> Theme.MUTED;
            case LOW -> new Color(0xA8A29E);
        };
    }

    private static String preview(String notes) {
        StringBuilder out = new StringBuilder();
        int limit = Math.min(notes.length(), 300);
        for (int i = 0; i < limit; i++) {
            char c = notes.charAt(i);
            out.append(c == '\n' || c == '\r' || c == '\t' ? ' ' : c);
        }
        return out.toString().trim();
    }

    private static String clip(FontMetrics metrics, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        int ellipsisWidth = metrics.stringWidth(ellipsis);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }
        int end = text.length();
        while (end > 0 && metrics.stringWidth(text.substring(0, end)) + ellipsisWidth > maxWidth) {
            end--;
        }
        return text.substring(0, end) + ellipsis;
    }
}
