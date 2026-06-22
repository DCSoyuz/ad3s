package ru.dcsoyuz.ad3s.form.editor;

import ru.dcsoyuz.ad3s.form.AppFrame;
import ru.dcsoyuz.ad3s.form.AppFrameHelper;
import ru.dcsoyuz.ad3s.form.Icons;
import ru.dcsoyuz.ad3s.model.Model;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class EditorTabbedPane extends JTabbedPane {

    private final Map<File, FileEditorPanel> openPanels = new HashMap<>();
    private final Map<File, JFrame> undockedFrames = new HashMap<>();
    private final AppFrame appFrame;
    private final JLabel textStatus;
    private final FileChangeMonitor fileChangeMonitor = FileChangeMonitor.getInstance();
    private boolean opening = false;

    public EditorTabbedPane(AppFrame appFrame, JLabel textStatus) {
        super(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        this.appFrame = appFrame;
        this.textStatus = textStatus;

        addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                FileEditorPanel panel = getActivePanel();
                if (panel != null) {
                    for (FileEditorPanel p : openPanels.values()) {
                        if (p != panel) p.savePosition();
                    }
                    panel.restorePosition();
                    updateTitle(panel);
                    Model.getEditorModel().setCurrentFileQuiet(panel.getFile());
                }
            }
        });

        setupKeyBindings();
        setupFileChangeMonitor();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK), "nextTab");
        am.put("nextTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = getSelectedIndex();
                if (getTabCount() > 0) {
                    setSelectedIndex((idx + 1) % getTabCount());
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "prevTab");
        am.put("prevTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int idx = getSelectedIndex();
                if (getTabCount() > 0) {
                    setSelectedIndex((idx - 1 + getTabCount()) % getTabCount());
                }
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK), "closeTab");
        am.put("closeTab", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeTab(getSelectedIndex());
            }
        });
    }

    private void setupFileChangeMonitor() {
        fileChangeMonitor.setListener(file -> handleExternalFileChange(file));
        fileChangeMonitor.start();
    }

    private void handleExternalFileChange(File file) {
        FileEditorPanel panel = openPanels.get(file);
        if (panel == null) {
            fileChangeMonitor.unregisterFile(file);
            return;
        }

        Window dialogParent = undockedFrames.get(file);
        if (dialogParent == null) {
            dialogParent = SwingUtilities.getWindowAncestor(this);
        }

        String message = "File '" + file.getName() + "' has been changed on disk.\nReload from disk?";
        String[] options = {"Reload", "Keep Editor Version"};
        int choice = JOptionPane.showOptionDialog(
                dialogParent, message, "File Changed Externally",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                null, options, options[0]);

        if (choice == JOptionPane.YES_OPTION) {
            panel.reloadFromDisk();
            markClean(file);
        } else {
            panel.getState().setSavingUpdate("");
            markDirty(file);
        }
        fileChangeMonitor.markSavedByEditor(file);
    }

    public FileChangeMonitor getFileChangeMonitor() {
        return fileChangeMonitor;
    }

    public void openFile(File file) {
        if (file == null || opening) return;

        if (openPanels.containsKey(file)) {
            switchToFile(file);
            return;
        }

        opening = true;
        try {
            FileEditorPanel panel = new FileEditorPanel(file, textStatus, this);
            openPanels.put(file, panel);

            String tabName = file.getName();
            addTab(tabName, panel);
            int idx = indexOfComponent(panel);
            setTabComponentAt(idx, new TabComponent(tabName));

            Model.getEditorModel().setCurrentFile(file);

            panel.loadFile();
            panel.updateOutputCodes();
            fileChangeMonitor.registerFile(file);

            setSelectedIndex(idx);
            updateTitle(panel);
        } finally {
            opening = false;
        }
    }

    public void closeTab(int index) {
        if (index < 0 || index >= getTabCount()) return;

        FileEditorPanel panel = (FileEditorPanel) getComponentAt(index);
        if (!panel.isUnchanged()) {
            String message = "Do you want to save " + panel.getFile().getAbsolutePath() + "?";
            int option = JOptionPane.showConfirmDialog(
                    this, message, null, JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                panel.saveFile();
            } else if (option == JOptionPane.CANCEL_OPTION) {
                return;
            }
        }

        openPanels.remove(panel.getFile());
        fileChangeMonitor.unregisterFile(panel.getFile());
        removeTabAt(index);

        if (getTabCount() > 0) {
            FileEditorPanel active = getActivePanel();
            if (active != null) {
                updateTitle(active);
            }
        } else {
            appFrame.setAppTitle(AppFrameHelper.APLICATION_NAME);
        }
    }

    public void closeFile(File file) {
        if (undockedFrames.containsKey(file)) {
            undockedFrames.get(file).dispose();
            return;
        }
        FileEditorPanel panel = openPanels.get(file);
        if (panel == null) return;
        int idx = indexOfComponent(panel);
        if (idx >= 0) closeTab(idx);
    }

    public void switchToFile(File file) {
        if (undockedFrames.containsKey(file)) {
            undockedFrames.get(file).toFront();
            return;
        }
        FileEditorPanel panel = openPanels.get(file);
        if (panel != null) {
            setSelectedComponent(panel);
        }
    }

    public FileEditorPanel getActivePanel() {
        int idx = getSelectedIndex();
        if (idx >= 0 && idx < getTabCount()) {
            return (FileEditorPanel) getComponentAt(idx);
        }
        return null;
    }

    /**
     * Returns the focused panel: checks undocked frames first, then tabbed pane.
     */
    public FileEditorPanel getActiveFilePanel() {
        // Check if any undocked window has focus
        for (Map.Entry<File, JFrame> entry : undockedFrames.entrySet()) {
            JFrame frame = entry.getValue();
            if (frame.isActive() || frame.isFocused()) {
                return openPanels.get(entry.getKey());
            }
        }
        // Fallback: check keyboard focus owner
        Window focusedWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow();
        if (focusedWindow != null) {
            for (Map.Entry<File, JFrame> entry : undockedFrames.entrySet()) {
                if (entry.getValue() == focusedWindow) {
                    return openPanels.get(entry.getKey());
                }
            }
        }
        return getActivePanel();
    }

    public boolean isFileOpen(File file) {
        return openPanels.containsKey(file);
    }

    public FileEditorPanel getPanel(File file) {
        return openPanels.get(file);
    }

    public void markDirty(File file) {
        FileEditorPanel panel = openPanels.get(file);
        if (panel != null && !undockedFrames.containsKey(file)) {
            int idx = indexOfComponent(panel);
            if (idx >= 0) {
                Component tc = getTabComponentAt(idx);
                if (tc instanceof TabComponent) {
                    ((TabComponent) tc).setDirty(true);
                }
            }
        }
    }

    public void markClean(File file) {
        FileEditorPanel panel = openPanels.get(file);
        if (panel != null && !undockedFrames.containsKey(file)) {
            int idx = indexOfComponent(panel);
            if (idx >= 0) {
                Component tc = getTabComponentAt(idx);
                if (tc instanceof TabComponent) {
                    ((TabComponent) tc).setDirty(false);
                }
            }
        }
    }

    // --- Undock / dock ---

    public void undock(File file) {
        if (file == null) return;
        FileEditorPanel panel = openPanels.get(file);
        if (panel == null) return;
        if (undockedFrames.containsKey(file)) return;

        // Remove tab
        int idx = indexOfComponent(panel);
        if (idx >= 0) removeTabAt(idx);

        // Create floating frame — panel already has its own toolbar
        JFrame frame = new JFrame(file.getName() + " - " + AppFrameHelper.APLICATION_NAME);
        frame.setIconImage(Icons.mainIcon.getImage());
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(appFrame);
        frame.setLayout(new BorderLayout());

        // Small control bar at top with Dock/Close
        JPanel controlBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        controlBar.setBackground(new Color(0x4A86C8));
        JButton dockBtn = new JButton("Dock");
        dockBtn.setFocusable(false);
        dockBtn.addActionListener(e -> dock(file));
        JButton closeBtn = new JButton("Close");
        closeBtn.setFocusable(false);
        closeBtn.addActionListener(e -> {
            if (!panel.isUnchanged()) {
                String message = "Do you want to save " + file.getAbsolutePath() + "?";
                int option = JOptionPane.showConfirmDialog(
                        frame, message, null, JOptionPane.YES_NO_CANCEL_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    panel.saveFile();
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    return;
                }
            }
            frame.dispose();
            undockedFrames.remove(file);
            openPanels.remove(file);
            fileChangeMonitor.unregisterFile(file);
        });
        controlBar.add(dockBtn);
        controlBar.add(closeBtn);
        frame.add(controlBar, BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                dock(file);
            }
        });

        undockedFrames.put(file, frame);
        frame.setVisible(true);

        if (getTabCount() > 0) {
            FileEditorPanel active = getActivePanel();
            if (active != null) updateTitle(active);
        } else {
            appFrame.setAppTitle(AppFrameHelper.APLICATION_NAME);
        }
    }

    public void dock(File file) {
        if (file == null) return;
        JFrame frame = undockedFrames.remove(file);
        if (frame == null) return;

        FileEditorPanel panel = openPanels.get(file);
        frame.dispose();

        if (panel == null) return;

        String tabName = file.getName();
        addTab(tabName, panel);
        int idx = indexOfComponent(panel);
        setTabComponentAt(idx, new TabComponent(tabName));
        setSelectedIndex(idx);
        updateTitle(panel);
    }

    private void updateTitle(FileEditorPanel panel) {
        appFrame.setAppTitle(panel.getFile().getName() + " - " + AppFrameHelper.APLICATION_NAME);
    }

    // --- Tab component with close + undock buttons ---

    class TabComponent extends JPanel {
        private final JLabel label;
        private boolean dirty;

        TabComponent(String name) {
            setOpaque(false);
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));

            label = new JLabel(name);
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
            add(label);

            JButton undockBtn = new JButton("⤢");
            undockBtn.setFont(undockBtn.getFont().deriveFont(Font.PLAIN, 10f));
            undockBtn.setToolTipText("Undock to separate window");
            undockBtn.setFocusable(false);
            undockBtn.setMargin(new Insets(0, 2, 0, 2));
            undockBtn.setPreferredSize(new Dimension(20, 16));
            undockBtn.addActionListener(e -> {
                FileEditorPanel p = getPanelFromTabComponent();
                if (p != null) undock(p.getFile());
            });
            add(undockBtn);

            JButton closeBtn = new JButton("×");
            closeBtn.setFont(closeBtn.getFont().deriveFont(Font.BOLD, 12f));
            closeBtn.setForeground(new Color(0xAA0000));
            closeBtn.setToolTipText("Close tab (Ctrl+W)");
            closeBtn.setFocusable(false);
            closeBtn.setMargin(new Insets(0, 2, 0, 2));
            closeBtn.setPreferredSize(new Dimension(18, 16));
            closeBtn.addActionListener(e -> {
                int idx = indexOfTabComponent(TabComponent.this);
                if (idx >= 0) closeTab(idx);
            });
            add(closeBtn);
        }

        private FileEditorPanel getPanelFromTabComponent() {
            int idx = EditorTabbedPane.this.indexOfTabComponent(TabComponent.this);
            if (idx >= 0) {
                return (FileEditorPanel) EditorTabbedPane.this.getComponentAt(idx);
            }
            return null;
        }

        void setDirty(boolean dirty) {
            this.dirty = dirty;
            String text = label.getText();
            if (dirty && !text.startsWith("*")) {
                label.setText("*" + text);
            } else if (!dirty && text.startsWith("*")) {
                label.setText(text.substring(1));
            }
        }
    }
}
