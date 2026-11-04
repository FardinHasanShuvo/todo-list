package todo.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import todo.model.Priority;
import todo.model.Task;
import todo.model.TaskStore;

/** Desktop window for the task list. */
public final class TodoFrame extends JFrame {
    private static final int SAVE = 0;
    private static final int DISCARD = 1;
    private static final int CANCEL = 2;

    private final TaskStore store;
    private final javax.swing.DefaultListModel<Task> model = new javax.swing.DefaultListModel<>();
    private final JList<Task> list = new JList<>(model);
    private final PromptField searchField = new PromptField("Search tasks", 24);
    private final PromptField addField = new PromptField("Add a task and press Enter", 28);
    private final PromptField dueField = new PromptField("yyyy-MM-dd", 12);
    private final JTextField titleField = new JTextField();
    private final JTextArea notesArea = new JTextArea();
    private final JScrollPane notesScroll = new JScrollPane();
    private final JComboBox<Priority> priorityCombo = new JComboBox<>(Priority.values());
    private final JCheckBox doneCheck = new JCheckBox("Completed");
    private final FlatButton saveButton = new FlatButton("Save", FlatButton.Kind.PRIMARY);
    private final FlatButton deleteButton = new FlatButton("Delete", FlatButton.Kind.DANGER);
    private final FlatButton addButton = new FlatButton("Add", FlatButton.Kind.PRIMARY);
    private final FilterChip allChip = new FilterChip("All");
    private final FilterChip activeChip = new FilterChip("Active");
    private final FilterChip doneChip = new FilterChip("Done");
    private final JRadioButtonMenuItem allItem = new JRadioButtonMenuItem("All");
    private final JRadioButtonMenuItem activeItem = new JRadioButtonMenuItem("Active");
    private final JRadioButtonMenuItem doneItem = new JRadioButtonMenuItem("Done");
    private final JMenuItem saveItem = new JMenuItem("Save details");
    private final JMenuItem deleteItem = new JMenuItem("Delete");
    private final JLabel countLabel = new JLabel(" ");
    private final JLabel footer = new JLabel(" ");
    private final JLabel formMessage = new JLabel(" ");
    private final JLabel emptyTitle = new JLabel("No tasks yet");
    private final JLabel emptySub = new JLabel("Type one below and press Enter.");
    private final CardLayout detailLayout = new CardLayout();
    private final JPanel detailCardsPanel = new JPanel(detailLayout);
    private final JPanel emptyPanel = new FillPanel(new GridBagLayout());

    private JScrollPane scroll;
    private Filter filter = Filter.ALL;
    private Task draftBase;
    private boolean dirty;
    private boolean loadingForm;
    private boolean adjustingSelection;
    private int hoverIndex = -1;

    public TodoFrame(TaskStore store) {
        super("Tasks");
        this.store = store;
        configureComponents();
        setContentPane(buildRoot());
        setJMenuBar(buildMenu());
        wireListeners();
        store.addListener(this::onStoreChanged);
        onStoreChanged();
        loadForm(null);
        setIconImages(Theme.icons());
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                exitApp();
            }
        });
        setMinimumSize(new Dimension(820, 560));
        pack();
        Dimension size = getSize();
        setSize(Math.max(size.width, 980), Math.max(size.height, 700));
        setLocationRelativeTo(null);
    }

    private void configureComponents() {
        Inputs.style(titleField);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(Theme.ui(14));
        notesArea.setForeground(Theme.INK);
        notesArea.setCaretColor(Theme.INK);
        notesArea.setBackground(Theme.SURFACE);
        notesArea.setSelectionColor(Theme.ACCENT_SOFT);
        notesArea.setSelectedTextColor(Theme.INK);
        notesArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        notesArea.setRows(6);
        notesScroll.setViewportView(notesArea);
        notesScroll.setBorder(new LineBorder(Theme.LINE, 1, true));
        notesScroll.getViewport().setBackground(Theme.SURFACE);

        priorityCombo.setFont(Theme.ui(14));
        priorityCombo.setBackground(Theme.SURFACE);
        priorityCombo.setForeground(Theme.INK);
        priorityCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> source,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(source, value, index, isSelected, cellHasFocus);
                if (value instanceof Priority priority) {
                    setText(priority.label());
                }
                return this;
            }
        });

        doneCheck.setFont(Theme.ui(14));
        doneCheck.setForeground(Theme.INK);
        doneCheck.setOpaque(false);
        formMessage.setFont(Theme.ui(12));
        formMessage.setText(" ");

        list.setCellRenderer(new TaskListRenderer());
        list.setFixedCellHeight(72);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(Theme.SURFACE);
        list.setFont(Theme.ui(14));
        list.setSelectionBackground(Theme.ACCENT_SOFT);
        list.setSelectionForeground(Theme.INK);

        emptyPanel.setBackground(Theme.SURFACE);
        emptyTitle.setFont(Theme.uiBold(18));
        emptyTitle.setForeground(Theme.INK);
        emptySub.setFont(Theme.ui(14));
        emptySub.setForeground(Theme.MUTED);
        GridBagConstraints emptyConstraints = new GridBagConstraints();
        emptyConstraints.gridx = 0;
        emptyConstraints.gridy = 0;
        emptyPanel.add(emptyTitle, emptyConstraints);
        emptyConstraints.gridy = 1;
        emptyConstraints.insets = new Insets(6, 0, 0, 0);
        emptyPanel.add(emptySub, emptyConstraints);

        countLabel.setFont(Theme.ui(13));
        footer.setFont(Theme.ui(12));
        saveButton.setEnabled(false);
        addButton.setEnabled(false);
        deleteButton.setEnabled(false);
    }

    private JPanel buildRoot() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        root.add(buildNorth(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildSouth(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildNorth() {
        JPanel north = new JPanel(new GridBagLayout());
        north.setBackground(Theme.BG);
        north.setBorder(new EmptyBorder(18, 20, 12, 20));

        JLabel title = new JLabel("Tasks");
        title.setFont(Theme.uiBold(26));
        title.setForeground(Theme.INK);
        JLabel subtitle = new JLabel("Click the circle when something is done.");
        subtitle.setFont(Theme.ui(13));
        subtitle.setForeground(Theme.MUTED);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(allChip);
        chips.add(activeChip);
        chips.add(doneChip);

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        north.add(title, c);
        c.gridx = 1;
        c.weightx = 0;
        c.anchor = GridBagConstraints.EAST;
        north.add(countLabel, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 0, 0, 0);
        north.add(subtitle, c);
        c.gridy = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(14, 0, 0, 0);
        north.add(searchField, c);
        c.gridy = 3;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(10, 0, 0, 0);
        north.add(chips, c);
        return north;
    }

    private JComponent buildCenter() {
        scroll = new JScrollPane();
        scroll.setBorder(new LineBorder(Theme.LINE, 1, true));
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(Theme.SURFACE);
        scroll.setViewportView(emptyPanel);
        scroll.setPreferredSize(new Dimension(540, 460));
        scroll.setMinimumSize(new Dimension(280, 200));

        JPanel listWrap = new JPanel(new BorderLayout());
        listWrap.setBackground(Theme.BG);
        listWrap.setBorder(new EmptyBorder(0, 20, 0, 8));
        listWrap.add(scroll, BorderLayout.CENTER);

        detailCardsPanel.setBackground(Theme.SURFACE);
        detailCardsPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(Theme.LINE, 1, true),
                new EmptyBorder(18, 18, 18, 18)));
        detailCardsPanel.add(buildForm(), "form");
        detailCardsPanel.add(buildPlaceholder(), "empty");
        detailLayout.show(detailCardsPanel, "empty");

        JPanel detailWrap = new JPanel(new BorderLayout());
        detailWrap.setBackground(Theme.BG);
        detailWrap.setBorder(new EmptyBorder(0, 8, 0, 20));
        detailWrap.add(detailCardsPanel, BorderLayout.CENTER);
        detailWrap.setPreferredSize(new Dimension(380, 460));
        detailWrap.setMinimumSize(new Dimension(300, 200));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listWrap, detailWrap);
        split.setResizeWeight(0.58);
        split.setContinuousLayout(true);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setBackground(Theme.BG);
        split.setOpaque(false);
        return split;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 12, 0);

        JLabel heading = new JLabel("Details");
        heading.setFont(Theme.uiBold(18));
        heading.setForeground(Theme.INK);
        form.add(heading, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(caption("Title"), c);
        c.gridy++;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(titleField, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 4, 0);
        form.add(caption("Notes"), c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 12, 0);
        form.add(notesScroll, c);

        JPanel attributes = new JPanel(new GridLayout(1, 2, 12, 0));
        attributes.setOpaque(false);
        attributes.add(labeled("Priority", priorityCombo));
        attributes.add(labeled("Due date", dueField));
        c.gridy++;
        c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(attributes, c);

        c.gridy++;
        c.insets = new Insets(12, 0, 4, 0);
        c.fill = GridBagConstraints.NONE;
        form.add(doneCheck, c);

        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);
        form.add(formMessage, c);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(deleteButton);
        buttons.add(saveButton);
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(buttons, c);
        return form;
    }

    private JPanel buildPlaceholder() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        JLabel heading = new JLabel("Select a task");
        heading.setFont(Theme.uiBold(18));
        heading.setForeground(Theme.INK);
        JLabel sub = new JLabel("Or add a new one below.");
        sub.setFont(Theme.ui(14));
        sub.setForeground(Theme.MUTED);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        panel.add(heading, c);
        c.gridy = 1;
        c.insets = new Insets(6, 0, 0, 0);
        panel.add(sub, c);
        return panel;
    }

    private JPanel buildSouth() {
        JPanel south = new JPanel(new BorderLayout());
        south.setBackground(Theme.BG);

        JPanel addRow = new JPanel(new GridBagLayout());
        addRow.setOpaque(false);
        addRow.setBorder(new EmptyBorder(12, 20, 4, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 8);
        addRow.add(addField, c);
        c.gridx = 1;
        c.weightx = 0;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 0);
        addRow.add(addButton, c);

        footer.setBorder(new EmptyBorder(4, 20, 14, 20));
        south.add(addRow, BorderLayout.NORTH);
        south.add(footer, BorderLayout.SOUTH);
        return south;
    }

    private JMenuBar buildMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu taskMenu = new JMenu("Task");
        taskMenu.setMnemonic(KeyEvent.VK_T);

        JMenuItem newItem = new JMenuItem("New");
        newItem.setAccelerator(shortcut(KeyEvent.VK_N));
        newItem.addActionListener(event -> addField.requestFocusInWindow());

        saveItem.setAccelerator(shortcut(KeyEvent.VK_S));
        saveItem.addActionListener(event -> {
            if (dirty) {
                saveDetails();
            }
        });

        JMenuItem toggleItem = new JMenuItem("Toggle completed");
        toggleItem.setAccelerator(shortcut(KeyEvent.VK_D));
        toggleItem.addActionListener(event -> toggleCurrent());

        deleteItem.addActionListener(event -> deleteCurrent());

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(event -> exitApp());

        taskMenu.add(newItem);
        taskMenu.add(saveItem);
        taskMenu.add(toggleItem);
        taskMenu.add(deleteItem);
        taskMenu.addSeparator();
        taskMenu.add(exitItem);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic(KeyEvent.VK_V);
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(allItem);
        viewGroup.add(activeItem);
        viewGroup.add(doneItem);
        allItem.setSelected(true);
        JMenuItem searchItem = new JMenuItem("Search");
        searchItem.setAccelerator(shortcut(KeyEvent.VK_F));
        searchItem.addActionListener(event -> searchField.requestFocusInWindow());
        viewMenu.add(allItem);
        viewMenu.add(activeItem);
        viewMenu.add(doneItem);
        viewMenu.addSeparator();
        viewMenu.add(searchItem);

        bar.add(taskMenu);
        bar.add(viewMenu);
        return bar;
    }

    private void wireListeners() {
        ButtonGroup chipGroup = new ButtonGroup();
        chipGroup.add(allChip);
        chipGroup.add(activeChip);
        chipGroup.add(doneChip);
        allChip.setSelected(true);

        bindFilter(allChip, Filter.ALL);
        bindFilter(activeChip, Filter.ACTIVE);
        bindFilter(doneChip, Filter.DONE);
        bindFilter(allItem, Filter.ALL);
        bindFilter(activeItem, Filter.ACTIVE);
        bindFilter(doneItem, Filter.DONE);

        onChange(searchField.getDocument(), this::refreshList);
        searchField.addActionListener(event -> {
            if (!model.isEmpty()) {
                list.requestFocusInWindow();
                if (list.getSelectedIndex() < 0) {
                    list.setSelectedIndex(0);
                }
            }
        });
        onChange(addField.getDocument(), () -> addButton.setEnabled(!addField.getText().trim().isEmpty()));
        addField.addActionListener(event -> addTask());
        addButton.addActionListener(event -> addTask());

        onChange(titleField.getDocument(), this::recomputeDirty);
        onChange(notesArea.getDocument(), this::recomputeDirty);
        onChange(dueField.getDocument(), this::recomputeDirty);
        priorityCombo.addItemListener(event -> recomputeDirty());
        titleField.addActionListener(event -> {
            if (dirty) {
                saveDetails();
            }
        });
        dueField.addActionListener(event -> {
            if (dirty) {
                saveDetails();
            }
        });
        doneCheck.addActionListener(event -> {
            if (loadingForm || draftBase == null) {
                return;
            }
            store.setDone(draftBase.id(), doneCheck.isSelected());
        });
        saveButton.addActionListener(event -> saveDetails());
        deleteButton.addActionListener(event -> deleteCurrent());

        list.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                onSelectionChanged();
            }
        });
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                int index = list.locationToIndex(event.getPoint());
                Rectangle cell = index < 0 ? null : list.getCellBounds(index, index);
                if (cell == null || !cell.contains(event.getPoint())) {
                    SwingUtilities.invokeLater(list::clearSelection);
                    return;
                }
                if (event.getClickCount() != 1) {
                    return;
                }
                int localX = event.getX() - cell.x;
                int localY = event.getY() - cell.y;
                if (!TaskListRenderer.circleHit(localX, localY, cell.height)) {
                    return;
                }
                String id = model.get(index).id();
                SwingUtilities.invokeLater(() -> {
                    Task selected = list.getSelectedValue();
                    if (selected != null && selected.id().equals(id)) {
                        toggleDone(id);
                    }
                });
            }

            @Override
            public void mouseExited(MouseEvent event) {
                setHover(-1);
                list.setCursor(Cursor.getDefaultCursor());
            }
        });
        list.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                int index = rowAt(event);
                setHover(index);
                boolean onCircle = false;
                if (index >= 0) {
                    Rectangle cell = list.getCellBounds(index, index);
                    if (cell != null) {
                        onCircle = TaskListRenderer.circleHit(
                                event.getX() - cell.x,
                                event.getY() - cell.y,
                                cell.height);
                    }
                }
                list.setCursor(Cursor.getPredefinedCursor(
                        onCircle ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });

        list.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggle-done");
        list.getActionMap().put("toggle-done", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                Task selected = list.getSelectedValue();
                if (selected != null) {
                    toggleDone(selected.id());
                }
            }
        });
        list.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete-task");
        list.getActionMap().put("delete-task", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                deleteCurrent();
            }
        });
    }

    private void bindFilter(javax.swing.AbstractButton button, Filter value) {
        button.addActionListener(event -> {
            if (button.isSelected()) {
                applyFilter(value);
            }
        });
    }

    private void onStoreChanged() {
        refreshList();
        if (loadingForm || draftBase == null) {
            return;
        }
        store.find(draftBase.id()).ifPresent(task -> {
            if (doneCheck.isSelected() != task.done()) {
                loadingForm = true;
                doneCheck.setSelected(task.done());
                loadingForm = false;
            }
        });
    }

    private void refreshList() {
        Task selected = list.getSelectedValue();
        String keep = selected == null ? null : selected.id();
        adjustingSelection = true;
        model.clear();
        for (Task task : visibleTasks()) {
            model.addElement(task);
        }
        int index = indexOf(keep);
        if (index >= 0) {
            list.setSelectedIndex(index);
        } else {
            list.clearSelection();
        }
        adjustingSelection = false;
        updateEmpty();
        updateCount();
        updateFooter();
        updateHiddenHint();
    }

    private List<Task> visibleTasks() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        ArrayList<Task> shown = new ArrayList<>();
        for (Task task : store.tasks()) {
            if (filter == Filter.ACTIVE && task.done()) {
                continue;
            }
            if (filter == Filter.DONE && !task.done()) {
                continue;
            }
            if (!query.isEmpty()) {
                String haystack = (task.title() + "\n" + task.notes()).toLowerCase(Locale.ROOT);
                if (!haystack.contains(query)) {
                    continue;
                }
            }
            shown.add(task);
        }
        shown.sort(Comparator
                .comparing(Task::done)
                .thenComparing(task -> task.due() == null ? LocalDate.MAX : task.due())
                .thenComparingInt(task -> task.priority().rank())
                .thenComparing(task -> task.title().toLowerCase(Locale.ROOT)));
        return shown;
    }

    private void updateEmpty() {
        if (model.isEmpty()) {
            boolean noneSaved = store.tasks().isEmpty();
            emptyTitle.setText(noneSaved ? "No tasks yet" : "No matching tasks");
            emptySub.setText(noneSaved
                    ? "Type one below and press Enter."
                    : "Try another filter or search.");
            if (scroll.getViewport().getView() != emptyPanel) {
                scroll.setViewportView(emptyPanel);
            }
            return;
        }
        if (scroll.getViewport().getView() != list) {
            scroll.setViewportView(list);
        }
    }

    private void updateCount() {
        LocalDate today = LocalDate.now();
        int active = 0;
        int overdue = 0;
        for (Task task : store.tasks()) {
            if (task.done()) {
                continue;
            }
            active++;
            if (task.overdue(today)) {
                overdue++;
            }
        }
        if (overdue > 0) {
            countLabel.setText(active + " active - " + overdue + " overdue");
            countLabel.setForeground(Theme.DANGER);
            return;
        }
        countLabel.setText(active == 1 ? "1 active" : active + " active");
        countLabel.setForeground(Theme.MUTED);
    }

    private void updateFooter() {
        if (store.saveError() != null) {
            setFooter(store.saveError(), Theme.DANGER);
            return;
        }
        if (store.notice() != null) {
            setFooter(store.notice(), Theme.DANGER);
            return;
        }
        setFooter("Saved in " + store.file(), Theme.MUTED);
    }

    private void setFooter(String text, Color color) {
        footer.setForeground(color);
        String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        footer.setText("<html><div style='width:720px'>" + escaped + "</div></html>");
        footer.setToolTipText(store.file().toString());
    }

    private void updateHiddenHint() {
        if (draftBase == null || dirty || loadingForm) {
            return;
        }
        if (indexOf(draftBase.id()) < 0 && store.find(draftBase.id()).isPresent()) {
            showFormMessage("Hidden by the current filter.", Theme.MUTED);
        }
    }

    private void applyFilter(Filter next) {
        if (next == filter) {
            return;
        }
        if (!confirmLeave()) {
            syncFilterWidgets();
            return;
        }
        filter = next;
        syncFilterWidgets();
        refreshList();
    }

    private void syncFilterWidgets() {
        switch (filter) {
            case ALL -> {
                allChip.setSelected(true);
                allItem.setSelected(true);
            }
            case ACTIVE -> {
                activeChip.setSelected(true);
                activeItem.setSelected(true);
            }
            case DONE -> {
                doneChip.setSelected(true);
                doneItem.setSelected(true);
            }
        }
    }

    private void onSelectionChanged() {
        if (adjustingSelection) {
            return;
        }
        Task next = list.getSelectedValue();
        String nextId = next == null ? null : next.id();
        String currentId = draftBase == null ? null : draftBase.id();
        if (Objects.equals(nextId, currentId)) {
            return;
        }
        if (!confirmLeave()) {
            selectQuiet(currentId);
            return;
        }
        loadForm(nextId == null ? null : store.find(nextId).orElse(null));
    }

    private void loadForm(Task task) {
        loadingForm = true;
        draftBase = task;
        if (task == null) {
            titleField.setText("");
            notesArea.setText("");
            dueField.setText("");
            priorityCombo.setSelectedItem(Priority.MEDIUM);
            doneCheck.setSelected(false);
            detailLayout.show(detailCardsPanel, "empty");
            showFormMessage(" ", Theme.MUTED);
        } else {
            titleField.setText(task.title());
            notesArea.setText(task.notes());
            dueField.setText(task.due() == null ? "" : task.due().toString());
            priorityCombo.setSelectedItem(task.priority());
            doneCheck.setSelected(task.done());
            detailLayout.show(detailCardsPanel, "form");
            showFormMessage(" ", Theme.MUTED);
        }
        loadingForm = false;
        dirty = false;
        updateActions();
    }

    private void recomputeDirty() {
        if (loadingForm) {
            return;
        }
        if (draftBase == null) {
            dirty = false;
            updateActions();
            return;
        }
        String dueText = draftBase.due() == null ? "" : draftBase.due().toString();
        dirty = !titleField.getText().equals(draftBase.title())
                || !notesArea.getText().equals(draftBase.notes())
                || priorityCombo.getSelectedItem() != draftBase.priority()
                || !dueField.getText().trim().equals(dueText);
        updateActions();
        if (dirty) {
            showFormMessage("Unsaved changes", Theme.MUTED);
        }
    }

    private void updateActions() {
        boolean hasTask = draftBase != null;
        deleteButton.setEnabled(hasTask);
        deleteItem.setEnabled(hasTask);
        saveButton.setEnabled(dirty && hasTask);
        saveItem.setEnabled(dirty && hasTask);
    }

    private void addTask() {
        String title = addField.getText().trim();
        if (title.isEmpty()) {
            addField.requestFocusInWindow();
            return;
        }
        if (title.length() > 200) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        if (!confirmLeave()) {
            return;
        }
        if (filter == Filter.DONE) {
            filter = Filter.ACTIVE;
            syncFilterWidgets();
        }
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (!query.isEmpty() && !title.toLowerCase(Locale.ROOT).contains(query)) {
            searchField.setText("");
        }
        Task task = Task.create(title);
        store.add(task);
        addField.setText("");
        selectAndLoad(task.id());
    }

    private void selectAndLoad(String id) {
        int index = indexOf(id);
        adjustingSelection = true;
        if (index >= 0) {
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }
        adjustingSelection = false;
        loadForm(store.find(id).orElse(null));
        notesArea.requestFocusInWindow();
    }

    private boolean saveDetails() {
        if (draftBase == null) {
            return false;
        }
        String title = titleField.getText().trim();
        if (title.isEmpty()) {
            showFormMessage("Give the task a title.", Theme.DANGER);
            titleField.requestFocusInWindow();
            return false;
        }
        if (title.length() > 200) {
            showFormMessage("Title must be 200 characters or fewer.", Theme.DANGER);
            titleField.requestFocusInWindow();
            return false;
        }
        String notes = notesArea.getText();
        if (notes.length() > 4000) {
            showFormMessage("Notes must be 4,000 characters or fewer.", Theme.DANGER);
            notesArea.requestFocusInWindow();
            return false;
        }
        LocalDate due;
        String dueRaw = dueField.getText().trim();
        try {
            due = dueRaw.isEmpty() ? null : LocalDate.parse(dueRaw);
        } catch (DateTimeParseException ex) {
            showFormMessage("Use a due date like 2026-11-20, or leave it blank.", Theme.DANGER);
            dueField.requestFocusInWindow();
            return false;
        }
        Priority priority = (Priority) priorityCombo.getSelectedItem();
        if (priority == null) {
            priority = Priority.MEDIUM;
        }
        Task existing = store.find(draftBase.id()).orElse(null);
        if (existing == null) {
            showFormMessage("This task is no longer in the list.", Theme.DANGER);
            return false;
        }
        Task updated = new Task(
                existing.id(),
                title,
                notes,
                priority,
                due,
                existing.done(),
                existing.created());
        draftBase = updated;
        store.replace(updated);
        loadingForm = true;
        titleField.setText(title);
        loadingForm = false;
        recomputeDirty();
        showFormMessage("Saved.", Theme.ACCENT);
        return true;
    }

    private void deleteCurrent() {
        String id = null;
        Task selected = list.getSelectedValue();
        if (selected != null) {
            id = selected.id();
        } else if (draftBase != null) {
            id = draftBase.id();
        }
        if (id == null) {
            return;
        }
        Task task = store.find(id).orElse(null);
        if (task == null) {
            return;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Delete \"" + shortTitle(task.title()) + "\"?\nThis cannot be undone.",
                "Delete task",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        dirty = false;
        store.remove(id);
        if (draftBase != null && draftBase.id().equals(id)) {
            loadForm(null);
        }
    }

    private void toggleDone(String id) {
        store.find(id).ifPresent(task -> store.setDone(id, !task.done()));
    }

    private void toggleCurrent() {
        if (draftBase != null) {
            toggleDone(draftBase.id());
            return;
        }
        Task selected = list.getSelectedValue();
        if (selected != null) {
            toggleDone(selected.id());
        }
    }

    private boolean confirmLeave() {
        if (!dirty || draftBase == null) {
            return true;
        }
        int choice = askSave(draftBase);
        if (choice == CANCEL) {
            return false;
        }
        if (choice == DISCARD) {
            dirty = false;
            updateActions();
            return true;
        }
        return saveDetails();
    }

    private int askSave(Task task) {
        Object[] options = {"Save", "Don't save", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
                this,
                "Save changes to \"" + shortTitle(task.title()) + "\"?",
                "Unsaved changes",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);
        if (choice == 0) {
            return SAVE;
        }
        if (choice == 1) {
            return DISCARD;
        }
        return CANCEL;
    }

    private void exitApp() {
        if (!confirmLeave()) {
            return;
        }
        dispose();
    }

    private void selectQuiet(String id) {
        adjustingSelection = true;
        int index = indexOf(id);
        if (index < 0) {
            list.clearSelection();
        } else {
            list.setSelectedIndex(index);
        }
        adjustingSelection = false;
    }

    private int indexOf(String id) {
        if (id == null) {
            return -1;
        }
        for (int i = 0; i < model.size(); i++) {
            if (model.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private int rowAt(MouseEvent event) {
        int index = list.locationToIndex(event.getPoint());
        if (index < 0) {
            return -1;
        }
        Rectangle cell = list.getCellBounds(index, index);
        if (cell == null || !cell.contains(event.getPoint())) {
            return -1;
        }
        return index;
    }

    private void setHover(int index) {
        if (hoverIndex == index) {
            return;
        }
        hoverIndex = index;
        list.putClientProperty("hoverRow", index);
        list.repaint();
    }

    private void showFormMessage(String text, Color color) {
        formMessage.setForeground(color);
        formMessage.setText(text == null || text.isBlank() ? " " : text);
    }

    private static JLabel caption(String text) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.ui(12));
        label.setForeground(Theme.MUTED);
        return label;
    }

    private static JPanel labeled(String name, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setOpaque(false);
        panel.add(caption(name), BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private static void onChange(javax.swing.text.Document document, Runnable action) {
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        });
    }

    private static KeyStroke shortcut(int keyCode) {
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        return KeyStroke.getKeyStroke(keyCode, mask);
    }

    private static String shortTitle(String title) {
        String trimmed = title.trim();
        if (trimmed.isEmpty()) {
            return "this task";
        }
        if (trimmed.length() <= 60) {
            return trimmed;
        }
        return trimmed.substring(0, 57) + "...";
    }

    private enum Filter {
        ALL, ACTIVE, DONE
    }

    private static final class FillPanel extends JPanel implements javax.swing.Scrollable {
        private FillPanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return Math.max(visibleRect.height - 16, 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return true;
        }
    }
}
