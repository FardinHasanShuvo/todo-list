package todo.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import todo.persist.TaskFile;

/**
 * The in-memory task list and its save file.
 * Call it from the Swing event thread only.
 */
public final class TaskStore {
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path file;
    private final List<Task> tasks = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final String notice;
    private String saveError;

    private TaskStore(Path file, List<Task> loaded, String notice) {
        this.file = file;
        this.notice = notice;
        Set<String> seen = new HashSet<>();
        for (Task task : loaded) {
            if (seen.add(task.id())) {
                tasks.add(task);
            }
        }
    }

    public static Path defaultFile() {
        return Path.of(System.getProperty("user.home"), ".todo-list", "tasks.json");
    }

    public static TaskStore open() {
        return open(defaultFile());
    }

    public static TaskStore open(Path file) {
        try {
            return new TaskStore(file, TaskFile.read(file), null);
        } catch (Exception ex) {
            String message = "Saved tasks could not be read (" + ex.getMessage() + ").";
            try {
                if (Files.exists(file) && Files.isRegularFile(file)) {
                    String stamp = BACKUP_STAMP.format(LocalDateTime.now());
                    Path backup = file.resolveSibling("tasks-" + stamp + ".bak");
                    Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
                    message += " A copy was saved as " + backup.getFileName() + ".";
                }
            } catch (IOException copyError) {
                message += " A backup copy could not be made (" + copyError.getMessage() + ").";
            }
            return new TaskStore(file, List.of(), message);
        }
    }

    public Path file() {
        return file;
    }

    /** Set when the save file could not be loaded. Null when the load worked. */
    public String notice() {
        return notice;
    }

    /** Set when the latest save failed. Null after a successful save. */
    public String saveError() {
        return saveError;
    }

    public List<Task> tasks() {
        return List.copyOf(tasks);
    }

    public Optional<Task> find(String id) {
        for (Task task : tasks) {
            if (task.id().equals(id)) {
                return Optional.of(task);
            }
        }
        return Optional.empty();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void add(Task task) {
        tasks.add(task);
        changed();
    }

    public void replace(Task updated) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).id().equals(updated.id())) {
                tasks.set(i, updated);
                changed();
                return;
            }
        }
    }

    public void setDone(String id, boolean done) {
        find(id).ifPresent(task -> {
            if (task.done() == done) {
                return;
            }
            replace(new Task(
                    task.id(),
                    task.title(),
                    task.notes(),
                    task.priority(),
                    task.due(),
                    done,
                    task.created()));
        });
    }

    public void remove(String id) {
        boolean removed = tasks.removeIf(task -> task.id().equals(id));
        if (removed) {
            changed();
        }
    }

    private void changed() {
        persist();
        for (Runnable listener : List.copyOf(listeners)) {
            listener.run();
        }
    }

    private void persist() {
        try {
            TaskFile.write(file, tasks);
            saveError = null;
        } catch (IOException ex) {
            saveError = "Could not save tasks (" + ex.getMessage() + ").";
        }
    }
}
