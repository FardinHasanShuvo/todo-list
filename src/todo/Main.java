package todo;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import todo.model.Task;
import todo.model.TaskStore;
import todo.ui.TodoFrame;

/** Starts the task list. Pass {@code --check} to build the window and exit. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        boolean check = args.length > 0 && "--check".equals(args[0]);
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Tasks needs a desktop session.");
            System.exit(1);
        }
        Path checkDir = check ? Files.createTempDirectory("tasks-ui-check") : null;
        if (check) {
            try {
                SwingUtilities.invokeAndWait(() -> open(true, checkDir));
            } finally {
                deleteTree(checkDir);
            }
            return;
        }
        SwingUtilities.invokeLater(() -> open(false, null));
    }

    private static void open(boolean check, Path checkDir) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The system look and feel is optional. The window still paints its own controls.
        }
        Path file = check ? checkDir.resolve("tasks.json") : TaskStore.defaultFile();
        TaskStore store = TaskStore.open(file);
        TodoFrame frame = new TodoFrame(store);
        if (!check) {
            frame.setVisible(true);
            return;
        }
        store.add(Task.create("Buy milk"));
        frame.pack();
        frame.setSize(980, 700);
        BufferedImage image = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        frame.printAll(graphics);
        graphics.dispose();
        frame.dispose();
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || Files.notExists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
