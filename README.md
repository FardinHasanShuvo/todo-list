# Tasks

A desktop to-do list written in Java. The window is Swing, and the only requirement is a JDK 17 or newer.

## Run

```powershell
cd C:\Users\fardin\projects\todo-list
.\run.ps1
```

In an IDE, mark `src` as the source folder and run `todo.Main`.

Tasks are saved in `%USERPROFILE%\.todo-list\tasks.json`. Each change is written immediately. If that file cannot be
read, the app keeps a `.bak` copy next to it and starts with an empty list.

## Use

- Type in the box at the bottom and press Enter to add a task.
- Select a task to edit its title, notes, priority, and due date, then choose Save. The due date is `yyyy-MM-dd`, or
  blank.
- Click the circle, or press Space while the list is focused, to mark a task done.
- Press Delete while the list is focused to remove the selected task.
- All, Active, and Done narrow the list. Search looks at titles and notes.
- Active tasks are listed before finished ones, then by due date, then by priority.

Shortcuts: Ctrl+N focuses the add box, Ctrl+S saves the open task, Ctrl+D toggles completed, and Ctrl+F focuses search.
