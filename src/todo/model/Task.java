package todo.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** One to-do item. Identity is {@code id}; the other fields are the editable state. */
public record Task(
        String id,
        String title,
        String notes,
        Priority priority,
        LocalDate due,
        boolean done,
        Instant created
) {
    public Task {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        title = Objects.requireNonNull(title, "title");
        notes = notes == null ? "" : notes;
        priority = Objects.requireNonNull(priority, "priority");
        created = Objects.requireNonNull(created, "created");
    }

    public static Task create(String title) {
        return new Task(
                UUID.randomUUID().toString(),
                title.trim(),
                "",
                Priority.MEDIUM,
                null,
                false,
                Instant.now());
    }

    public boolean overdue(LocalDate today) {
        return !done && due != null && due.isBefore(today);
    }
}
