package todo.model;

import java.util.Locale;

/** How urgent a task is. Stored by enum name. */
public enum Priority {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Lower sorts first. */
    public int rank() {
        return switch (this) {
            case HIGH -> 0;
            case MEDIUM -> 1;
            case LOW -> 2;
        };
    }

    public static Priority fromStored(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEDIUM;
        }
        try {
            return valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MEDIUM;
        }
    }
}
