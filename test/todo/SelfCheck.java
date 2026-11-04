package todo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import todo.model.Priority;
import todo.model.Task;
import todo.model.TaskStore;
import todo.persist.TaskFile;

/** Checks save and load without opening the window. */
public final class SelfCheck {
    private SelfCheck() {
    }

    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("todo-self-check");
        try {
            roundTrip(dir.resolve("tasks.json"));
            storeRoundTrip(dir.resolve("store.json"));
            corruptFile(dir.resolve("broken.json"));
            check(Priority.fromStored("high") == Priority.HIGH, "priority parse");
            check(Priority.fromStored("nope") == Priority.MEDIUM, "unknown priority");
            Task overdue = new Task(
                    "id",
                    "Late",
                    "",
                    Priority.LOW,
                    LocalDate.now().minusDays(1),
                    false,
                    Instant.parse("2026-01-01T00:00:00Z"));
            check(overdue.overdue(LocalDate.now()), "overdue");
            check(!overdue.overdue(LocalDate.now().minusDays(2)), "not overdue yet");
        } finally {
            try (var paths = Files.walk(dir)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
        System.out.println("Self-check passed.");
    }

    private static void roundTrip(Path file) throws Exception {
        Task special = new Task(
                "abc",
                "Cafe \u00e9 \"quote\" \\ slash",
                "line1\nline2\tend",
                Priority.HIGH,
                LocalDate.of(2026, 11, 20),
                false,
                Instant.parse("2026-11-04T15:04:05Z"));
        Task done = new Task(
                "def",
                "Call dentist",
                "",
                Priority.LOW,
                null,
                true,
                Instant.parse("2026-11-01T08:00:00Z"));
        TaskFile.write(file, List.of(special, done));
        List<Task> loaded = TaskFile.read(file);
        check(loaded.size() == 2, "two tasks");
        Task first = loaded.get(0);
        check(first.title().equals(special.title()), "title");
        check(first.notes().equals(special.notes()), "notes");
        check(first.priority() == Priority.HIGH, "priority");
        check(LocalDate.of(2026, 11, 20).equals(first.due()), "due");
        check(!first.done(), "not done");
        check(first.created().equals(special.created()), "created");
        check(loaded.get(1).due() == null, "null due");
        check(loaded.get(1).done(), "done");
        TaskFile.write(file, List.of());
        check(TaskFile.read(file).isEmpty(), "empty file");
    }

    private static void storeRoundTrip(Path file) {
        TaskStore store = TaskStore.open(file);
        store.add(Task.create("One"));
        String id = store.tasks().get(0).id();
        store.setDone(id, true);
        TaskStore again = TaskStore.open(file);
        check(again.tasks().size() == 1, "store size");
        check(again.tasks().get(0).title().equals("One"), "store title");
        check(again.tasks().get(0).done(), "store done");
        check(again.notice() == null, "store notice");
    }

    private static void corruptFile(Path file) throws Exception {
        Files.writeString(file, "{bad}\n");
        TaskStore broken = TaskStore.open(file);
        check(broken.tasks().isEmpty(), "corrupt starts empty");
        check(broken.notice() != null && broken.notice().contains("could not be read"), "corrupt notice");
        boolean backup = false;
        try (var paths = Files.list(file.getParent())) {
            for (Path path : paths.toList()) {
                if (path.getFileName().toString().endsWith(".bak")) {
                    backup = true;
                }
            }
        }
        check(backup, "backup written");
        check(Files.exists(file), "original kept");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new RuntimeException(message);
        }
    }
}
