package ru.dcsoyuz.ad3s.form;


import io.github.andrewauclair.moderndocking.Dockable;
import io.github.andrewauclair.moderndocking.DockableTabPreference;
import io.github.andrewauclair.moderndocking.DockingRegion;
import io.github.andrewauclair.moderndocking.app.AppState;
import io.github.andrewauclair.moderndocking.app.Docking;
import io.github.andrewauclair.moderndocking.app.RootDockingPanel;
import io.github.andrewauclair.moderndocking.app.WindowLayoutBuilder;
import io.github.andrewauclair.moderndocking.event.DockingEvent;
import io.github.andrewauclair.moderndocking.event.DockingListener;
import io.github.andrewauclair.moderndocking.event.NewFloatingFrameListener;
import io.github.andrewauclair.moderndocking.layouts.ApplicationLayout;
import io.github.andrewauclair.moderndocking.settings.Settings;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.editor.*;

import ru.dcsoyuz.ad3s.form.terminal.*;
import ru.dcsoyuz.ad3s.form.tree.TreeViewPane;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.RegField;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by yuri.filatov on 15.07.2016.
 */
public class AppFrame extends JFrame implements IModelEditorEventListener {
	private static final Logger logger = LoggerFactory.getLogger(AppFrame.class);



    private AsmEditor asmEditor;
    private ru.dcsoyuz.ad3s.form.MenuBar menuBar;
    private JTextArea textAreaErrors;
    private CustomOutputStream customOut;
    RootDockingPanel rootPanel;

    private ConnectPanel connectPanel;
    private EncoderView encoderView;

    private JComboBox shrdRamCMB;

    private JComboBox shrdCpu2CMB;
    private static AppFrame instance;

    private DockablePanel dockableAsmEditor;
    private Dockable[] allDockables;
    private ApplicationLayout defaultLayout;
    private File persistFile;

    public static AppFrame getInstance() {
        if(AppFrame.instance == null) {
            AppFrame.instance = new AppFrame();
        }
        return AppFrame.instance;
    }

    public AppFrame() throws HeadlessException {



        if (!UIManager.getLookAndFeel().getClass().getName().contains("flatlaf")) {
            AppFrameHelper.setupLookAndFeel();
        }

        AppFrameHelper.setupAppLocation(this);
        addWindowListener(new MainWindowListener());
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(1000_000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setIconImage(Icons.mainIcon.getImage());
        setTitle(AppFrameHelper.APLICATION_NAME);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        asmEditor =  new AsmEditor(this);

        if(System.getProperty("os.name").toLowerCase().equals("linux"))
        {
            Font fontTable = new Font("Nimbus Sans L", Font.PLAIN, 10);
            Font fontLabel = new Font("Nimbus Sans L", Font.PLAIN, 10);
            UIManager.put("CheckBox.font",   fontLabel   );
            UIManager.put("Table.font",      fontTable   );
            UIManager.put("TabbedPane.font", fontLabel   );
            UIManager.put("Label.font",      fontLabel   );
            UIManager.put("Border.font",     fontLabel   );
            UIManager.put("Button.font",     fontLabel   );
        }

        ru.dcsoyuz.ad3s.form.MenuBar menuBar = new MenuBar(asmEditor);

        JPanel panelLeftTree = new JPanel();
        panelLeftTree.setLayout(new BoxLayout(panelLeftTree, BoxLayout.Y_AXIS));

        menuBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        panelLeftTree.add(menuBar);

        TreeViewPane treeViewPane =  new TreeViewPane();
        // ограничиваем высоту дерева по содержимому, чтобы кнопки были сразу после
        JTree tree = (JTree) treeViewPane.getViewport().getView();
        int treeH = Math.max(tree.getRowCount() * (tree.getRowHeight() > 0 ? tree.getRowHeight() : 20), 100);
        JPanel treeWrapper = new JPanel(new BorderLayout());
        treeWrapper.add(treeViewPane, BorderLayout.CENTER);
        treeWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, treeH + 4));
        panelLeftTree.add(treeWrapper);

        connectPanel = new ConnectPanel();
        panelLeftTree.add(connectPanel);
        // shrdRamCMB = createPanelBoolean(RegField.SHRD_RAM, panelLeftTree);
        // shrdCpu2CMB = createPanelBoolean(RegField.SHRD_CPU2, panelLeftTree);



        panelLeftTree.setMinimumSize(new Dimension(106,0));
        panelLeftTree.setPreferredSize(new Dimension(123,0));
        panelLeftTree.setMaximumSize(new Dimension(158,Integer.MAX_VALUE));

        // Create dockable panels
        dockableAsmEditor = new DockablePanel("asm-editor", "Asm Editor", asmEditor);
        DockablePanel dockableTerminal = new DockablePanel("terminal", "Terminal", new Ad3sFace());
        DockablePanel dockableGraphView = new DockablePanel("graph-view", "GraphView", new GraphView());
        DockablePanel dockableHandTap = new DockablePanel("hand-tap-view", "HandTapView", new HandTapView());
        encoderView = new EncoderView();
        DockablePanel dockableDebugger = new DockablePanel("debugger-cpus", "DebuggerCpus", new DebuggerView());

        // Initialize docking framework
        Settings.setDefaultTabPreference(DockableTabPreference.TOP_ALWAYS);
        Docking.initialize(this);
        menuBar.initTabListener();

        rootPanel = new RootDockingPanel(this);

        Dockable[] dockables = {
            dockableAsmEditor, dockableTerminal, dockableGraphView, dockableHandTap,
            dockableDebugger
        };
        allDockables = dockables;
        for (Dockable d : dockables) {
            Docking.registerDockable(d);
        }

        // Build default layout: all tabs grouped together
        defaultLayout = new WindowLayoutBuilder("asm-editor")
                .dock("terminal", "asm-editor")
                .dock("graph-view", "asm-editor")
                .dock("hand-tap-view", "asm-editor")
                .dock("debugger-cpus", "asm-editor")
                .display("asm-editor")
                .buildApplicationLayout();

        AppState.setDefaultApplicationLayout(defaultLayout);

        // Restore persisted layout or use default
        persistFile = new File("dock_layout.xml");
        AppState.setPersistFile(persistFile);
        boolean restoredOk = false;
        try {
            restoredOk = AppState.restore();
        } catch (Exception e) {
            logger.error("Failed to restore docking layout, using default: {}", e.getMessage());
            persistFile.delete();
        }
        // Ensure all registered dockables are visible after restore.
        // If a panel was undocked and its floating frame was lost (e.g. app killed),
        // dock it into the main window.
        SwingUtilities.invokeLater(() -> {
            for (Dockable d : dockables) {
                if (!Docking.isDocked(d)) {
                    try {
                        Docking.dock(d, AppFrame.this, DockingRegion.CENTER);
                    } catch (Exception ex) {
                        logger.error("Failed to re-dock {}: {}", d.getPersistentID(), ex.getMessage());
                    }
                }
            }
        });

        // Force tabs to top after restore
        Settings.setDefaultTabPreference(DockableTabPreference.TOP_ALWAYS);
        AppState.setAutoPersist(true);

        // Listen for docking focus changes to sync active tab
        String[] tabOrder = {
            "asm-editor", "terminal", "graph-view", "hand-tap-view",
            "combined-view", "dma-quad-view", "encoder", "dma-single-view",
            "regs-html", "paths", "float-helper",
            "debugger-cpus", "record"
        };
        boolean[] restoring = {false};
        Docking.addDockingListener(new DockingListener() {
            @Override
            public void dockingChange(DockingEvent event) {
                try {
                Dockable d = event.getDockable();
                if (d == null) return;
                String id = d.getPersistentID();

                if (event.getID() == DockingEvent.ID.SHOWN) {
                    Model.getEditorModel().setActiveTab(id);
                }

                // Fix tab order after any DOCKED event in main window
                if (event.getID() == DockingEvent.ID.DOCKED && !restoring[0]
                        && SwingUtilities.isDescendingFrom(d instanceof JPanel ? (JPanel) d : null, AppFrame.this)) {
                    restoring[0] = true;
                    SwingUtilities.invokeLater(() -> {
                        try {
                            // Re-dock all tabs in fixed order
                            Docking.dock(tabOrder[0], AppFrame.this);
                            for (int i = 1; i < tabOrder.length; i++) {
                                if (Docking.isDockableRegistered(tabOrder[i])) {
                                    Docking.dock(tabOrder[i], tabOrder[0], DockingRegion.CENTER);
                                }
                            }
                        } catch (Exception ex) {
                            logger.error("DockingListener tab reorder error: {}", ex.getMessage());
                        } finally {
                            restoring[0] = false;
                        }
                    });
                }
                } catch (Exception ex) {
                    logger.error("DockingListener error: {}", ex.getMessage());
                }
            }
        });

        // When a floating window closes, re-dock its panel back to the tab group
        Docking.addNewFloatingFrameListener(new NewFloatingFrameListener() {
            @Override
            public void newFrameCreated(JFrame frame, io.github.andrewauclair.moderndocking.api.RootDockingPanelAPI root) {
            }

            @Override
            public void newFrameCreated(JFrame frame, io.github.andrewauclair.moderndocking.api.RootDockingPanelAPI root, Dockable dockable) {
                String dockableID = dockable.getPersistentID();
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent e) {
                        SwingUtilities.invokeLater(() -> {
                            try {
                            if (Docking.isDockableRegistered(dockableID)) {
                                Docking.dock(dockableID, "asm-editor", DockingRegion.CENTER);
                            }
                            } catch (Exception ex) {
                                logger.error("Re-dock after float close error: {}", ex.getMessage());
                            }
                        });
                    }
                });
            }
        });


			/* system output */
        JPanel outPanel = new JPanel();
        outPanel.setLayout(new BorderLayout());





        // Left panel (tree) + right panel (docking tabs) in a simple BorderLayout
        JPanel mainHorizontalPanel = new JPanel(new BorderLayout());
        mainHorizontalPanel.add(panelLeftTree, BorderLayout.WEST);
        mainHorizontalPanel.add(rootPanel, BorderLayout.CENTER);


        textAreaErrors = new JTextArea();
        textAreaErrors.setLineWrap(true);
        textAreaErrors.setWrapStyleWord(true);
        JScrollPane sysLogScrollPane =  new JScrollPane(textAreaErrors, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outPanel.add(sysLogScrollPane, BorderLayout.CENTER);

        JPopupMenu logPopupMenu = new JPopupMenu();
        JMenuItem copyItem = new JMenuItem("Copy");
        copyItem.addActionListener(ev -> textAreaErrors.copy());
        logPopupMenu.add(copyItem);
        JMenuItem clearItem = new JMenuItem("Clear");
        clearItem.addActionListener(ev -> customOut.clear());
        logPopupMenu.add(clearItem);
        textAreaErrors.setComponentPopupMenu(logPopupMenu);

        TextLineNumber textLineNumber = new TextLineNumber(textAreaErrors);
        sysLogScrollPane.setRowHeaderView(textLineNumber);
        customOut = new CustomOutputStream(textAreaErrors);
        customOut.setLineNumbers(textLineNumber);
        PrintStream printStream = new PrintStream(customOut);
        System.setOut(printStream);
        System.setErr(printStream);





        JSplitPane splitMain0 = new JSplitPane();
        splitMain0.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitMain0.setDividerSize(9);
        splitMain0.setSize(getWidth(), getHeight());
        splitMain0.setDividerLocation(0.75);
        splitMain0.setResizeWeight(1);
        splitMain0.setOneTouchExpandable(true);
        splitMain0.setTopComponent(mainHorizontalPanel);
        splitMain0.setBottomComponent(outPanel);

        // Настройка синих цветов для divider'ов (кастомный UI)
        AppFrameHelper.setupSplitPane(splitMain0);

        add(splitMain0);

        Model.getEditorModel().setModelEditorEventListener(this);
        Model.setMainFrame(this);
        setVisible(true);

    }


    public boolean getShrdRamValue(){
        return false;
    }

    public boolean getShrdCpu2Value(){
        return false;
    }

    private JComboBox createPanelBoolean (RegField field, JPanel panel2){
        JPanel panel  = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(new JLabel(field.getDisplayName() + ":"));
        String [] items = new String[]{"0", "1"};
        JComboBox cmb = new JComboBox(items);
        cmb.setSelectedIndex(field.getDefaultValue());
        cmb.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Model.getParserModel().updateShrdFlags();
            }
        });

        cmb.setMaximumSize(new Dimension(50,20));
        cmb.setMinimumSize(new Dimension(50,20));
        panel.add(cmb);
        panel.setAlignmentX(JFrame.RIGHT_ALIGNMENT);
        panel2.add(panel);
        return  cmb;
    }

    public static void setUIFont (javax.swing.plaf.FontUIResource f){
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get (key);
            if (value instanceof javax.swing.plaf.FontUIResource)
                UIManager.put (key, f);
        }
    }




    public String getAppTitle(){
        return this.getTitle();
    }



    public void setAppTitle(String title){
        this.setTitle(title);
    }

    public AsmEditor getAsmEditor() {
        return asmEditor;
    }
    class MainWindowListener extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            WorkstationConfig.storeProperties();
            System.exit(0);
        }
    }


    @Override
    public void cleanTextErorrs() {
        if (customOut != null) {
            customOut.clear();
        }
    }


    @Override
    public void updateCurrentTab() {
        String tabId = Model.getEditorModel().getActiveTab();
        if (tabId != null && Docking.isDockableRegistered(tabId)) {
            Docking.bringToFront(tabId);
        }
    }

    /**
     * Обновление значений энкодеров (вызывается из UartModel при получении ответа)
     */
    public void updateEncoderAngles(int enc1, int enc2) {
        if (encoderView != null) {
            encoderView.updateEnc1Angle(enc1);
            encoderView.updateEnc2Angle(enc2);
        }
    }

    public void restoreDefaultLayout() {
        if (persistFile != null) persistFile.delete();
        if (defaultLayout == null || allDockables == null) return;
        for (Dockable d : allDockables) {
            try { Docking.undock(d); } catch (Exception ignored) {}
        }
        AppState.setDefaultApplicationLayout(defaultLayout);
        try {
            AppState.restore();
        } catch (Exception e) {
            logger.error("Failed to restore default layout: {}", e.getMessage());
        }
        for (Dockable d : allDockables) {
            if (d instanceof DockablePanel) {
                JPanel content = ((DockablePanel) d).getContent();
                if (content instanceof GraphView) {
                    ((GraphView) content).restoreDefaults();
                }
            }
        }
        SwingUtilities.invokeLater(() -> {
            for (Dockable d : allDockables) {
                if (!Docking.isDocked(d)) {
                    try {
                        Docking.dock(d, AppFrame.this, DockingRegion.CENTER);
                    } catch (Exception ignored) {}
                }
            }
        });
    }
}
