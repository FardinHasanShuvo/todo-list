package todo.persist;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import todo.model.Priority;
import todo.model.Task;

/**
 * Reads and writes tasks as one JSON object per line.
 * The file is small and has a fixed shape, so this stays inside the JDK.
 */
public final class TaskFile {
    private TaskFile() {
    }

    public static List<Task> read(Path path) throws IOException {
        if (Files.notExists(path)) {
            return new ArrayList<>();
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        ArrayList<Task> tasks = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (i == 0 && line.startsWith("\uFEFF")) {
                line = line.substring(1);
            }
            if (line.isBlank()) {
                continue;
            }
            try {
                tasks.add(parseLine(line));
            } catch (RuntimeException ex) {
                throw new IOException("Line " + (i + 1) + ": " + ex.getMessage(), ex);
            }
        }
        return tasks;
    }

    public static void write(Path path, List<Task> tasks) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder body = new StringBuilder();
        for (Task task : tasks) {
            writeTask(body, task);
        }
        Path temp = path.resolveSibling(path.getFileName().toString() + ".tmp");
        Files.writeString(temp, body.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void writeTask(StringBuilder out, Task task) {
        out.append('{');
        stringField(out, "id", task.id());
        out.append(',');
        stringField(out, "title", task.title());
        out.append(',');
        stringField(out, "notes", task.notes());
        out.append(',');
        stringField(out, "priority", task.priority().name());
        out.append(',');
        stringField(out, "due", task.due() == null ? null : task.due().toString());
        out.append(',');
        out.append("\"done\":").append(task.done() ? "true" : "false");
        out.append(',');
        stringField(out, "created", task.created().toString());
        out.append("}\n");
    }

    private static void stringField(StringBuilder out, String key, String value) {
        out.append('"').append(key).append("\":");
        if (value == null) {
            out.append("null");
            return;
        }
        out.append('"').append(escape(value)).append('"');
    }

    private static String escape(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '"' -> out.append("\\\"");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }

    private static Task parseLine(String line) {
        Map<String, String> map = parseObject(line);
        String id = required(map, "id");
        String title = required(map, "title");
        String notes = map.get("notes");
        if (notes == null) {
            notes = "";
        }
        Priority priority = Priority.fromStored(map.get("priority"));
        LocalDate due = null;
        if (map.containsKey("due") && map.get("due") != null && !map.get("due").isBlank()) {
            try {
                due = LocalDate.parse(map.get("due"));
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("bad due date");
            }
        }
        boolean done = parseDone(map.get("done"));
        Instant created;
        try {
            created = Instant.parse(required(map, "created"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("bad created time");
        }
        return new Task(id, title, notes, priority, due, done, created);
    }

    private static boolean parseDone(String raw) {
        if (raw == null || raw.equals("false")) {
            return false;
        }
        if (raw.equals("true")) {
            return true;
        }
        throw new IllegalArgumentException("done must be true or false");
    }

    private static String required(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing " + key);
        }
        return value;
    }

    private static Map<String, String> parseObject(String line) {
        Cursor cursor = new Cursor(line);
        Map<String, String> map = new HashMap<>();
        cursor.expect('{');
        cursor.skipSpace();
        if (cursor.remaining() && cursor.current() == '}') {
            cursor.advance();
        } else {
            while (true) {
                String key = cursor.string();
                cursor.expect(':');
                map.put(key, cursor.value());
                char separator = cursor.take();
                if (separator == '}') {
                    break;
                }
                if (separator != ',') {
                    throw new IllegalArgumentException("expected , or }");
                }
            }
        }
        cursor.skipSpace();
        if (cursor.remaining()) {
            throw new IllegalArgumentException("extra data after the task");
        }
        return map;
    }

    private static final class Cursor {
        private final String text;
        private int index;

        private Cursor(String text) {
            this.text = text;
        }

        private boolean remaining() {
            skipSpace();
            return index < text.length();
        }

        private char current() {
            return text.charAt(index);
        }

        private void advance() {
            index++;
        }

        private void skipSpace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private char peek() {
            skipSpace();
            if (index >= text.length()) {
                throw new IllegalArgumentException("unexpected end");
            }
            return text.charAt(index);
        }

        private char take() {
            char c = peek();
            index++;
            return c;
        }

        private void expect(char expected) {
            char found = take();
            if (found != expected) {
                throw new IllegalArgumentException("expected " + expected);
            }
        }

        private String value() {
            char kind = peek();
            if (kind == '"') {
                return string();
            }
            String literal = literal();
            if (literal.equals("null")) {
                return null;
            }
            if (literal.equals("true") || literal.equals("false")) {
                return literal;
            }
            throw new IllegalArgumentException("unknown value " + literal);
        }

        private String literal() {
            skipSpace();
            int start = index;
            while (index < text.length() && Character.isLetter(text.charAt(index))) {
                index++;
            }
            if (start == index) {
                throw new IllegalArgumentException("expected a value");
            }
            return text.substring(start, index);
        }

        private String string() {
            expect('"');
            StringBuilder out = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return out.toString();
                }
                if (c != '\\') {
                    out.append(c);
                    continue;
                }
                if (index >= text.length()) {
                    throw new IllegalArgumentException("bad escape");
                }
                char escaped = text.charAt(index++);
                switch (escaped) {
                    case '"', '\\', '/' -> out.append(escaped);
                    case 'n' -> out.append('\n');
                    case 'r' -> out.append('\r');
                    case 't' -> out.append('\t');
                    case 'u' -> {
                        if (index + 4 > text.length()) {
                            throw new IllegalArgumentException("bad unicode escape");
                        }
                        int code = Integer.parseInt(text.substring(index, index + 4), 16);
                        index += 4;
                        out.append((char) code);
                    }
                    default -> throw new IllegalArgumentException("bad escape");
                }
            }
            throw new IllegalArgumentException("unclosed string");
        }
    }
}
