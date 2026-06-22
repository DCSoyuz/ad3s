package ru.dcsoyuz.ad3s.form;

import ru.dcsoyuz.ad3s.form.editor.AsmEditor;
import ru.dcsoyuz.ad3s.form.terminal.FloatHelper;
import ru.dcsoyuz.ad3s.form.terminal.HTMLViewer;
import ru.dcsoyuz.ad3s.form.terminal.PathEditor;
import ru.dcsoyuz.ad3s.form.terminal.ParallelSpiView;
import ru.dcsoyuz.ad3s.form.terminal.EncoderView;
import ru.dcsoyuz.ad3s.form.terminal.RecordView;
import ru.dcsoyuz.ad3s.form.editor.BatchWriterPanel;
import ru.dcsoyuz.ad3s.model.Model;

import io.github.andrewauclair.moderndocking.app.Docking;
import io.github.andrewauclair.moderndocking.Dockable;
import io.github.andrewauclair.moderndocking.event.DockingEvent;
import io.github.andrewauclair.moderndocking.event.DockingListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yuri.filatov on 22.07.2016.
 */
public class MenuBar extends JMenuBar {


    private JMenuItem itemNewFile, itemOpen, itemSave, itemSaveAs, itemAbout, itemExit,
            itemSelectAll, itemCopy, itemPaste, itemCut, itemDelete, itemUndo, itemRedo;

    private JMenu file, utils, help;

    private final Map<String, JMenuItem> tabMenuItems = new LinkedHashMap<>();
    private final Map<String, String> tabLabels = new LinkedHashMap<>();




    public MenuBar(final AsmEditor editor) {
        super();

        itemNewFile = new JMenuItem("New", Icons.iconNew);
        itemNewFile.setToolTipText("Create a new file");
        itemNewFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
        itemNewFile.setActionCommand("new");
        itemNewFile.addActionListener(editor);

        itemOpen = new JMenuItem("Open...", Icons.iconOpen);
        itemOpen.setToolTipText("Open a file");
        itemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
        itemOpen.setActionCommand("open");
        itemOpen.addActionListener(editor);

        itemSave = new JMenuItem("Save", Icons.iconSave);
        itemSave.setToolTipText("Save the file");
        itemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
        itemSave.setActionCommand("save");
        itemSave.addActionListener(editor);

        itemSaveAs = new JMenuItem("Save As...", Icons.iconSave);
        itemSaveAs.setToolTipText("Save the file");
        itemSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK
                | ActionEvent.SHIFT_MASK));
        itemSaveAs.setActionCommand("saveAs");
        itemSaveAs.addActionListener(editor);

        itemAbout = new JMenuItem("About", Icons.iconAbout);
        itemAbout.setToolTipText("About the program");
        itemAbout.setActionCommand("about");
        itemAbout.addActionListener(editor);

        itemExit = new JMenuItem("Exit", Icons.iconExit);
        itemExit.setToolTipText("Exit the application");
        itemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK));
        itemExit.setActionCommand("exit");
        itemExit.addActionListener(editor);

        itemSelectAll = new JMenuItem("Select All", Icons.iconSelectAll);
        itemSelectAll.setToolTipText("Select all text in this text area");
        itemSelectAll.setActionCommand("selectAll");
        itemSelectAll.addActionListener(editor);

        itemCopy = new JMenuItem("Copy", Icons.iconCopy);
        itemCopy.setToolTipText("Copy to a clipboard");
        itemCopy.setActionCommand("copy");
        itemCopy.addActionListener(editor);

        itemPaste = new JMenuItem("Paste", Icons.iconPaste);
        itemPaste.setToolTipText("Paste from a clipboard");
        itemPaste.setActionCommand("paste");
        itemPaste.addActionListener(editor);

        itemCut = new JMenuItem("Cut", Icons.iconCut);
        itemCut.setToolTipText("Cut to a clipboard");
        itemCut.setActionCommand("cut");
        itemCut.addActionListener(editor);

        itemDelete = new JMenuItem("Delete", Icons.iconDelete);
        itemDelete.setToolTipText("Delete the selected text");
        itemDelete.setActionCommand("delete");
        itemDelete.addActionListener(editor);

        itemUndo = new JMenuItem(editor.getUndoAction());
        itemUndo.setText("Undo");
        itemUndo.setIcon(Icons.iconUndo);
        itemUndo.setToolTipText("Undo last action");
        itemUndo.setActionCommand("undo");
        itemUndo.addActionListener(editor);

        itemRedo = new JMenuItem(editor.getRedoAction());
        itemRedo.setText("Redo");
        itemRedo.setIcon(Icons.iconRedo);
        itemRedo.setToolTipText("Redo last action");
        itemRedo.setActionCommand("redo");
        itemRedo.addActionListener(editor);

        file = new JMenu("File");
        utils = new JMenu("Options");
        help = new JMenu("Help");

        file.setMinimumSize(new Dimension(30,20));
        utils.setMinimumSize(new Dimension(30,20));
        help.setMinimumSize(new Dimension(30,20));

        add(file);
        add(utils);
        add(help);

        file.add(itemOpen);
        file.add(itemSave);
        file.add(itemSaveAs);
        file.addSeparator();
        file.add(itemExit);

        // Utils
        JMenuItem itemFloatHelper = new JMenuItem("Float Helper");
        itemFloatHelper.addActionListener(e -> {
            JFrame frame = new JFrame("Float Helper");
            frame.setIconImage(Icons.mainIcon.getImage());
            frame.setSize(900, 750);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new FloatHelper());
            frame.setVisible(true);
        });
        utils.add(itemFloatHelper);
        utils.addSeparator();

        JMenuItem itemEncoder = new JMenuItem("Encoder");
        itemEncoder.addActionListener(e -> {
            JFrame frame = new JFrame("Encoder");
            frame.setIconImage(Icons.mainIcon.getImage());
            frame.setSize(600, 350);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new EncoderView());
            frame.setVisible(true);
        });
        utils.add(itemEncoder);

        JMenuItem itemPaths = new JMenuItem("Paths");
        itemPaths.addActionListener(e -> {
            JFrame frame = new JFrame("Paths");
            frame.setIconImage(Icons.mainIcon.getImage());
            frame.setSize(650, 500);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new PathEditor());
            frame.setVisible(true);
        });
        utils.add(itemPaths);

        JMenuItem itemRecord = new JMenuItem("Record");
        itemRecord.addActionListener(e -> {
            JFrame frame = new JFrame("Record");
            frame.setIconImage(Icons.mainIcon.getImage());
            frame.setSize(550, 550);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new RecordView());
            frame.setVisible(true);
        });
        utils.add(itemRecord);

        JMenuItem itemParallelSpi = new JMenuItem("ParallelSpiView");
        itemParallelSpi.addActionListener(e -> {
            JFrame frame = new JFrame("ParallelSpiView");
            frame.setIconImage(Icons.mainIcon.getImage());
            frame.setSize(700, 500);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new ParallelSpiView());
            frame.setVisible(true);
        });
        utils.add(itemParallelSpi);

        JMenuItem itemBatchWriter = new JMenuItem("BatchWriter");
        itemBatchWriter.addActionListener(e -> {
            JFrame frame = new JFrame("BatchWriter");
            frame.setIconImage(Icons.mainIcon.getImage());
            frame.setSize(900, 600);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(new BatchWriterPanel());
            frame.setVisible(true);
        });
        utils.add(itemBatchWriter);

        utils.addSeparator();

        addSwitchTabItem(utils, "HandTap", "hand-tap-view");
        addSwitchTabItem(utils, "DebuggerCpus", "debugger-cpus");
        addSwitchTabItem(utils, "Terminal", "terminal");
        addSwitchTabItem(utils, "GraphView", "graph-view");
        addSwitchTabItem(utils, "AsmEditor", "asm-editor");

        utils.addSeparator();

        JMenuItem itemTestBoard = new JMenuItem("Окно Тест");
        itemTestBoard.addActionListener(e -> {
            JFrame frame = new JFrame("Окно Тест");
            frame.setIconImage(Icons.mainIcon.getImage());
            frame.setSize(240, 170);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            // LED
            JPanel ledRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            JCheckBox ledCheckBox = new JCheckBox("LED");
            ledCheckBox.addActionListener(ev -> {
                Model.getMemoryModel().setLedValue(((JCheckBox)ev.getSource()).isSelected());
            });
            ledRow.add(ledCheckBox);
            panel.add(ledRow);
            panel.add(new JSeparator(SwingConstants.HORIZONTAL));

            // Find IC
            JPanel findRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            JButton findICButton = new JButton("Find IC");
            findICButton.addActionListener(ev -> {
                Model.getMemoryModel().findICAddress();
            });
            findRow.add(findICButton);
            panel.add(findRow);
            panel.add(new JSeparator(SwingConstants.HORIZONTAL));

            // SpiSpd
            JPanel spiRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            JLabel spiSpeedLabel = new JLabel("SpiSpd:");
            spiRow.add(spiSpeedLabel);
            JTextField spiSpeedTF = new JTextField("10000", 5);
            spiRow.add(spiSpeedTF);
            spiRow.add(Box.createHorizontalStrut(4));
            JButton setSpiSpeedBtn = new JButton("SET");
            setSpiSpeedBtn.addActionListener(ev -> {
                Model.getMemoryModel().setSpeedSpi(Integer.parseInt(spiSpeedTF.getText()));
            });
            spiRow.add(setSpiSpeedBtn);
            panel.add(spiRow);

            frame.add(panel);
            frame.setVisible(true);
        });
        utils.add(itemTestBoard);

        help.add(itemAbout);
        help.addSeparator();
        JMenuItem itemRegisters = new JMenuItem("Registers");
        itemRegisters.addActionListener(e -> HTMLViewer.openWindow());
        help.add(itemRegisters);
        help.addSeparator();
        JMenuItem itemRestoreWindow = new JMenuItem("Restore Window");
        itemRestoreWindow.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "Restore default window layout?\nCurrent layout will be reset.",
                    "Restore Window",
                    JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                AppFrame.getInstance().restoreDefaultLayout();
            }
        });
        help.add(itemRestoreWindow);

        setupMenuColors();
    }

    /**
     * Вызвать после Docking.initialize(), чтобы зарегистрировать слушатель вкладок.
     */
    public void initTabListener() {
        Docking.addDockingListener(new DockingListener() {
            @Override
            public void dockingChange(DockingEvent event) {
                if (event.getID() == DockingEvent.ID.SHOWN) {
                    Dockable d = event.getDockable();
                    if (d != null) {
                        SwingUtilities.invokeLater(() -> updateActiveTabIndicator(d.getPersistentID()));
                    }
                }
            }
        });
    }

    private void setupMenuColors() {
        Color barBg = new Color(0xA8E0FF);
        Color popupBg = new Color(0xA8E0FF);
        Color selectedBg = new Color(0x87CEEB);
        Color textBlack = Color.BLACK;

        this.setBackground(barBg);
        this.setForeground(textBlack);
        this.setOpaque(true);

        for (java.awt.Component comp : this.getComponents()) {
            if (comp instanceof JMenu) {
                comp.setBackground(barBg);
                comp.setForeground(textBlack);
                ((JMenu) comp).setOpaque(true);
            }
        }

        UIManager.put("MenuBar.background", barBg);
        UIManager.put("MenuBar.foreground", textBlack);
        UIManager.put("Menu.background", barBg);
        UIManager.put("Menu.foreground", textBlack);
        UIManager.put("Menu.selectionBackground", selectedBg);
        UIManager.put("MenuItem.background", popupBg);
        UIManager.put("MenuItem.foreground", textBlack);
        UIManager.put("MenuItem.selectionBackground", selectedBg);
        UIManager.put("MenuItem.selectionForeground", textBlack);
        UIManager.put("CheckBoxMenuItem.background", popupBg);
        UIManager.put("CheckBoxMenuItem.foreground", textBlack);
        UIManager.put("CheckBoxMenuItem.selectionBackground", selectedBg);
        UIManager.put("CheckBoxMenuItem.selectionForeground", textBlack);
        UIManager.put("PopupMenu.background", popupBg);
        UIManager.put("PopupMenu.foreground", textBlack);

        setMenuItemColors(file, popupBg, textBlack);
        setMenuItemColors(utils, popupBg, textBlack);
        setMenuItemColors(help, popupBg, textBlack);

        setItemColor(itemOpen, popupBg, textBlack);
        setItemColor(itemSave, popupBg, textBlack);
        setItemColor(itemSaveAs, popupBg, textBlack);
        setItemColor(itemExit, popupBg, textBlack);
        setItemColor(itemAbout, popupBg, textBlack);

        this.updateUI();
    }

    private void setMenuItemColors(JMenu menu, Color bg, Color fg) {
        menu.setBackground(bg);
        menu.setForeground(fg);
        for (java.awt.Component item : menu.getMenuComponents()) {
            if (item instanceof JMenuItem) {
                ((JMenuItem) item).setBackground(bg);
                ((JMenuItem) item).setForeground(fg);
            } else if (item instanceof JMenu) {
                setMenuItemColors((JMenu) item, bg, fg);
            }
        }
    }

    private void setItemColor(JMenuItem item, Color bg, Color fg) {
        item.setBackground(bg);
        item.setForeground(fg);
    }

    private void addSwitchTabItem(JMenu menu, String label, String persistentID) {
        JMenuItem item = new JMenuItem("  " + label);
        item.addActionListener(e -> SwingUtilities.invokeLater(() -> switchToTab(persistentID)));
        menu.add(item);
        tabMenuItems.put(persistentID, item);
        tabLabels.put(persistentID, label);
    }

    private void updateActiveTabIndicator(String activeID) {
        for (Map.Entry<String, JMenuItem> entry : tabMenuItems.entrySet()) {
            String id = entry.getKey();
            JMenuItem item = entry.getValue();
            String label = tabLabels.get(id);
            if (id.equals(activeID)) {
                item.setText("• " + label);  // • bullet
                item.setFont(item.getFont().deriveFont(Font.BOLD));
            } else {
                item.setText("  " + label);
                item.setFont(item.getFont().deriveFont(Font.PLAIN));
            }
        }
    }

    private void switchToTab(String persistentID) {
        try {
            Docking.bringToFront(persistentID);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

}
