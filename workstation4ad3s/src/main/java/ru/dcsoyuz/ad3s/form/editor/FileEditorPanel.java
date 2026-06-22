package ru.dcsoyuz.ad3s.form.editor;

import org.apache.commons.lang3.SerializationUtils;
import ru.dcsoyuz.ad3s.form.AppFrame;
import ru.dcsoyuz.ad3s.form.editor.table.ExcelAdapter;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.editor.OutputCodes;
import ru.dcsoyuz.ad3s.model.fpga.parser.ModeAttrFile;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileEditorPanel extends JPanel {
	private static final Logger logger = LoggerFactory.getLogger(FileEditorPanel.class);

    private static final int BUFFER_SIZE = 8912;
    private static final int FONT_SIZE = 11;

    private final FileEditorState state;
    private final EditorTabbedPane owningTabbedPane;

    // Toolbar
    private final JToolBar toolBar;
    private final JLabel textStatus;

    // Edit components (mutually exclusive: only one visible at a time)
    private final TextPane mainText;
    private final TextPane mainText2;
    private final JTable table;
    private final JScrollPane scrollPane;
    private final JScrollPane scrollPane2;
    private final JScrollPane scrollPaneTable;
    private final JPanel mainZone;

    // Output code panes
    private final JTextPane paneClearAssembler;
    private final JTextPane paneHexCodes;
    private final JTextPane paneBinaryCodes;

    // Documents
    private Document document;
    private Document document2;

    // Table state
    private DefaultTableModel savedTableModel;

    // Popup menu
    private final JPopupMenu popupMenu;

    // Icons
    private static final ImageIcon iconSave = new ImageIcon(ClassLoader.getSystemResource("icons/save.png"));
    private static final ImageIcon iconUndo = new ImageIcon(ClassLoader.getSystemResource("icons/undo.png"));
    private static final ImageIcon iconRedo = new ImageIcon(ClassLoader.getSystemResource("icons/redo.png"));
    private static final ImageIcon iconCut = new ImageIcon(ClassLoader.getSystemResource("icons/cut.png"));
    private static final ImageIcon iconCopy = new ImageIcon(ClassLoader.getSystemResource("icons/copy.png"));
    private static final ImageIcon iconPaste = new ImageIcon(ClassLoader.getSystemResource("icons/paste.png"));
    private static final ImageIcon iconDelete = new ImageIcon(ClassLoader.getSystemResource("icons/delete.png"));
    private static final ImageIcon iconSelectAll = new ImageIcon(ClassLoader.getSystemResource("icons/selectAll.png"));
    private static final ImageIcon iconSearch = new ImageIcon(ClassLoader.getSystemResource("icons/search.png"));

    public FileEditorPanel(File file, JLabel sharedStatus, EditorTabbedPane tabbedPane) {
        super(new BorderLayout());
        this.state = new FileEditorState(file, sharedStatus);
        this.textStatus = sharedStatus;
        this.owningTabbedPane = tabbedPane;

        // Create edit components
        mainText = new TextPane();
        mainText.setBackground(new Color(0x8BC7FA));
        mainText.setForeground(Color.BLACK);
        mainText.setFocusable(true);
        mainText.setSelectedTextColor(Color.BLACK);
        mainText.setSelectionColor(new Color(0x7BC0E0));

        mainText2 = new TextPane();
        mainText2.setBackground(new Color(0x8ECCFF));
        mainText2.setForeground(Color.BLACK);

        StyledEditorKit kit = new StyledEditorKit();
        document = kit.createDefaultDocument();

        // Scroll panes for edit area
        scrollPane = new JScrollPane(mainText);
        scrollPane.setBackground(new Color(0x2D4A6C));
        scrollPane.getViewport().setBackground(new Color(0x8AC2F1));
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setRowHeaderView(new TextLineNumber(mainText));

        scrollPane2 = new JScrollPane(mainText2);
        scrollPane2.setBackground(new Color(0x2D4A6C));
        scrollPane2.getViewport().setBackground(new Color(0xE8F4F8));
        scrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane2.setRowHeaderView(new TextLineNumber(mainText2));

        table = new JTable();
        table.setShowGrid(true);
        table.setGridColor(new Color(0x6A8AAC));
        table.setCellSelectionEnabled(true);
        new ExcelAdapter(table);
        scrollPaneTable = new JScrollPane(table);
        scrollPaneTable.setVisible(false);

        // Main zone (left side of split)
        mainZone = new JPanel();
        mainZone.setLayout(new BoxLayout(mainZone, BoxLayout.Y_AXIS));
        mainZone.add(scrollPane);
        mainZone.add(scrollPane2);
        mainZone.add(scrollPaneTable, BorderLayout.CENTER);

        // Output code panes (right side of split)
        paneClearAssembler = new JTextPane();
        paneClearAssembler.setBackground(new Color(0x93C6F3));
        TextLineNumber tlnCA = new TextLineNumber(paneClearAssembler);
        JScrollPane spClearAssembler = new JScrollPane(paneClearAssembler, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        spClearAssembler.setBackground(new Color(0x2D4A6C));
        spClearAssembler.getViewport().setBackground(new Color(0x93C9FB));
        spClearAssembler.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        spClearAssembler.setRowHeaderView(tlnCA);

        paneBinaryCodes = new JTextPane();
        paneBinaryCodes.setBackground(new Color(0x6ABDFB));
        Font font = paneBinaryCodes.getFont();
        paneBinaryCodes.setFont(new Font(font.getFamily(), font.getStyle(), FONT_SIZE));
        TextLineNumber tlnBin = new TextLineNumber(paneBinaryCodes);
        JScrollPane spMachineCodes = new JScrollPane(paneBinaryCodes, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spMachineCodes.setBackground(new Color(0x2D4A6C));
        spMachineCodes.getViewport().setBackground(new Color(0x75BFFF));
        spMachineCodes.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        spMachineCodes.getVerticalScrollBar().setModel(spClearAssembler.getVerticalScrollBar().getModel());
        paneBinaryCodes.addMouseWheelListener(e -> spClearAssembler.dispatchEvent(e));
        spMachineCodes.setRowHeaderView(tlnBin);

        paneHexCodes = new JTextPane();
        paneHexCodes.setBackground(new Color(0x7ABEFA));
        paneHexCodes.setFont(new Font(font.getFamily(), font.getStyle(), FONT_SIZE));
        TextLineNumber tlnHEX = new TextLineNumber(paneHexCodes);
        JScrollPane spHexCodes = new JScrollPane(paneHexCodes, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spHexCodes.setBackground(new Color(0x2D4A6C));
        spHexCodes.getViewport().setBackground(new Color(0x7CC1F8));
        spHexCodes.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        spHexCodes.getVerticalScrollBar().setModel(spClearAssembler.getVerticalScrollBar().getModel());
        paneHexCodes.addMouseWheelListener(e -> spClearAssembler.dispatchEvent(e));
        spHexCodes.setRowHeaderView(tlnHEX);

        JSplitPane outputCodesPane2 = new JSplitPane();
        outputCodesPane2.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        outputCodesPane2.setDividerSize(8);
        outputCodesPane2.setDividerLocation(0.5);
        outputCodesPane2.setOneTouchExpandable(true);
        outputCodesPane2.setResizeWeight(0.5);
        outputCodesPane2.setLeftComponent(spHexCodes);
        outputCodesPane2.setRightComponent(spMachineCodes);

        JSplitPane outputCodesPane1 = new JSplitPane();
        outputCodesPane1.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        outputCodesPane1.setDividerSize(8);
        outputCodesPane1.setDividerLocation(0.5);
        outputCodesPane1.setOneTouchExpandable(true);
        outputCodesPane1.setResizeWeight(0.5);
        outputCodesPane1.setLeftComponent(spClearAssembler);
        outputCodesPane1.setRightComponent(outputCodesPane2);

        // Main split: edit area | output codes
        JSplitPane workSpacePane = new JSplitPane();
        workSpacePane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        workSpacePane.setDividerSize(8);
        workSpacePane.setDividerLocation(0.7);
        workSpacePane.setOneTouchExpandable(true);
        workSpacePane.setResizeWeight(0.7);
        workSpacePane.setLeftComponent(mainZone);
        workSpacePane.setRightComponent(outputCodesPane1);

        // Build toolbar
        toolBar = createToolBar();

        // Build popup menu
        popupMenu = createPopupMenu();
        mainText.setComponentPopupMenu(popupMenu);

        // Key bindings
        setupKeyBindings();

        // Layout: toolbar on top, workspace below
        add(toolBar, BorderLayout.NORTH);
        add(workSpacePane, BorderLayout.CENTER);
    }

    // --- Toolbar ---

    private JToolBar createToolBar() {
        JToolBar tb = new JToolBar(JToolBar.HORIZONTAL);
        tb.setFloatable(false);
        tb.setBackground(new Color(0x5B9BD5));

        // Save
        JButton btnSave = new JButton("Save", iconSave);
        btnSave.setToolTipText("Save file (Ctrl+S)");
        btnSave.setFocusable(false);
        btnSave.addActionListener(e -> {
            saveFile();
            Model.getParserModel().parseCurrentFile();
            updateColorDocument();
            textStatus.setText("OK");
        });
        tb.add(btnSave);

        tb.addSeparator();

        // Undo
        JButton btnUndo = new JButton("Undo", iconUndo);
        btnUndo.setToolTipText("Undo (Ctrl+Z)");
        btnUndo.setFocusable(false);
        btnUndo.addActionListener(e -> {
            if (state.getUndo().canUndo()) {
                state.getUndoAction().actionPerformed(e);
            }
        });
        tb.add(btnUndo);

        // Redo
        JButton btnRedo = new JButton("Redo", iconRedo);
        btnRedo.setToolTipText("Redo (Ctrl+Shift+Z)");
        btnRedo.setFocusable(false);
        btnRedo.addActionListener(e -> {
            if (state.getUndo().canRedo()) {
                state.getRedoAction().actionPerformed(e);
            }
        });
        tb.add(btnRedo);

        tb.addSeparator();

        // Cut
        JButton btnCut = new JButton(iconCut);
        btnCut.setToolTipText("Cut (Ctrl+X)");
        btnCut.setFocusable(false);
        btnCut.addActionListener(e -> {
            TextPane tp = getFocusedTextPane();
            if (tp != null) tp.cut();
        });
        tb.add(btnCut);

        // Copy
        JButton btnCopy = new JButton(iconCopy);
        btnCopy.setToolTipText("Copy (Ctrl+C)");
        btnCopy.setFocusable(false);
        btnCopy.addActionListener(e -> {
            TextPane tp = getFocusedTextPane();
            if (tp != null) tp.copy();
        });
        tb.add(btnCopy);

        // Paste
        JButton btnPaste = new JButton(iconPaste);
        btnPaste.setToolTipText("Paste (Ctrl+V)");
        btnPaste.setFocusable(false);
        btnPaste.addActionListener(e -> {
            TextPane tp = getFocusedTextPane();
            if (tp != null) tp.paste();
        });
        tb.add(btnPaste);

        // Delete
        JButton btnDelete = new JButton(iconDelete);
        btnDelete.setToolTipText("Delete selected");
        btnDelete.setFocusable(false);
        btnDelete.addActionListener(e -> {
            TextPane tp = getFocusedTextPane();
            if (tp != null) tp.replaceSelection("");
        });
        tb.add(btnDelete);

        tb.addSeparator();

        // Select All
        JButton btnSelectAll = new JButton(iconSelectAll);
        btnSelectAll.setToolTipText("Select all");
        btnSelectAll.setFocusable(false);
        btnSelectAll.addActionListener(e -> {
            TextPane tp = getFocusedTextPane();
            if (tp != null) tp.selectAll();
        });
        tb.add(btnSelectAll);

        tb.addSeparator();

        // Search
        JButton btnSearch = new JButton(iconSearch);
        btnSearch.setToolTipText("Search and replace (Ctrl+F)");
        btnSearch.setFocusable(false);
        btnSearch.addActionListener(e -> openSearchDialog());
        tb.add(btnSearch);

        // Parse
        JButton btnParse = new JButton("Parse");
        btnParse.setToolTipText("Parse all files");
        btnParse.setFocusable(false);
        btnParse.addActionListener(e -> {
            Model.getParserModel().parseCurrentFile();
            updateOutputCodes();
        });
        tb.add(btnParse);

        // Status (use the shared label reference)
        tb.add(Box.createHorizontalGlue());
        tb.add(textStatus);

        return tb;
    }

    private void openSearchDialog() {
        SwingUtilities.invokeLater(() -> {
            SearchDialog dlg = SearchDialog.getInstance(AppFrame.getInstance());
            if (dlg.isVisible()) {
                dlg.toFront();
            } else {
                dlg.setVisible(true);
            }
        });
    }

    // --- Popup menu ---

    private JPopupMenu createPopupMenu() {
        JPopupMenu pop = new JPopupMenu();

        JMenuItem miUndo = new JMenuItem("Undo");
        miUndo.addActionListener(e -> {
            if (state.getUndo().canUndo()) {
                state.getUndoAction().actionPerformed(e);
            }
        });
        pop.add(miUndo);

        JMenuItem miRedo = new JMenuItem("Redo");
        miRedo.addActionListener(e -> {
            if (state.getUndo().canRedo()) {
                state.getRedoAction().actionPerformed(e);
            }
        });
        pop.add(miRedo);

        pop.addSeparator();

        JMenuItem miCut = new JMenuItem("Cut");
        miCut.addActionListener(e -> { TextPane tp = getFocusedTextPane(); if (tp != null) tp.cut(); });
        pop.add(miCut);

        JMenuItem miCopy = new JMenuItem("Copy");
        miCopy.addActionListener(e -> { TextPane tp = getFocusedTextPane(); if (tp != null) tp.copy(); });
        pop.add(miCopy);

        JMenuItem miPaste = new JMenuItem("Paste");
        miPaste.addActionListener(e -> { TextPane tp = getFocusedTextPane(); if (tp != null) tp.paste(); });
        pop.add(miPaste);

        JMenuItem miDelete = new JMenuItem("Delete");
        miDelete.addActionListener(e -> { TextPane tp = getFocusedTextPane(); if (tp != null) tp.replaceSelection(""); });
        pop.add(miDelete);

        pop.addSeparator();

        JMenuItem miSelectAll = new JMenuItem("Select All");
        miSelectAll.addActionListener(e -> { TextPane tp = getFocusedTextPane(); if (tp != null) tp.selectAll(); });
        pop.add(miSelectAll);

        return pop;
    }

    // --- Key bindings ---

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        // Ctrl+Z: undo
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK), "panelUndo");
        am.put("panelUndo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (state.getUndo().canUndo()) {
                    state.getUndoAction().actionPerformed(e);
                }
            }
        });

        // Ctrl+Shift+Z: redo
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), "panelRedo");
        am.put("panelRedo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (state.getUndo().canRedo()) {
                    state.getRedoAction().actionPerformed(e);
                }
            }
        });

        // Ctrl+F: search
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK), "panelSearch");
        am.put("panelSearch", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openSearchDialog();
            }
        });

        // Ctrl+S: save
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK), "panelSave");
        am.put("panelSave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
                Model.getParserModel().parseCurrentFile();
                updateColorDocument();
                textStatus.setText("OK");
            }
        });
    }

    // --- Accessors ---

    public FileEditorState getState() { return state; }
    public File getFile() { return state.getFile(); }
    public TextPane getMainText() { return mainText; }
    public TextPane getMainText2() { return mainText2; }
    public JTable getTable() { return table; }
    public Document getDocument() { return document; }

    public TextPane getFocusedTextPane() {
        if (scrollPane2 != null && scrollPane2.isVisible()) {
            return mainText2;
        }
        return mainText;
    }

    public boolean isTableVisible() {
        return scrollPaneTable != null && scrollPaneTable.isVisible();
    }

    public String getCurrentText() {
        if (state.getFile().getName().contains("txt")) {
            return getTextFromTable();
        }
        return mainText.getText();
    }

    // --- File loading ---

    public void loadFile() {
        File file = state.getFile();
        BufferedReader br = null;
        InputStream in = null;
        try {
            in = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFFER_SIZE);

            if (file.getName().contains("asm")) {
                scrollPaneTable.setVisible(false);
                scrollPane2.setVisible(false);
                scrollPane.setVisible(true);

                Document oldDocument = mainText.getDocument();
                if (oldDocument != null) {
                    oldDocument.removeUndoableEditListener(state.getUndoHandler());
                }
                StyledEditorKit kit = new StyledEditorKit();
                document = kit.createDefaultDocument();
                kit.read(br, document, 0);
                updateColorDocument();

                state.clearUndo();
                mainText.setEditable(true);
                revalidate();
                mainText.repaint();

            } else if (file.getName().contains("txt")) {
                scrollPane.setVisible(false);
                scrollPane2.setVisible(false);
                scrollPaneTable.setVisible(true);

                ModeAttrFile attr = Model.getEditorModel().getCurrentAttr();
                String[] columnNames = {"ind", "value", "comment", "var1", "var2", "var3"};
                DefaultTableModel tableModel = new DefaultTableModel(columnNames, attr.getSize());
                String line;
                String[] at;
                Integer i = 0;
                while (i <= attr.getSize() - 1) {
                    at = new String[6];
                    if ((line = br.readLine()) != null) {
                        String[] data = line.split("\t");
                        if (data.length != 6) {
                            for (int k = 0; k < 6; k++) {
                                at[k] = k <= data.length - 1 ? data[k] : "";
                            }
                        } else {
                            at = data;
                        }
                    }
                    Integer hexSize = attr.getHexSize();
                    if (attr.getNumBits() > 16) {
                        hexSize = hexSize / 2;
                    }
                    if (i < hexSize) {
                        at[0] = i.toString();
                    } else {
                        at[0] = String.valueOf(i - hexSize);
                    }
                    tableModel.insertRow(i, at);
                    i++;
                }
                tableModel.setRowCount(attr.getSize());

                savedTableModel = SerializationUtils.clone(tableModel);
                tableModel.addTableModelListener(new TableModelListener() {
                    public void tableChanged(TableModelEvent e) {
                        DefaultTableModel currentModel = (DefaultTableModel) table.getModel();
                        String newValue = currentModel.getValueAt(e.getFirstRow(), e.getColumn()).toString();
                        Object oldValueObj = savedTableModel.getValueAt(e.getFirstRow(), e.getColumn());
                        String oldValue = oldValueObj != null ? (String) oldValueObj : "";
                        if (!newValue.equals(oldValue)) {
                            state.setTableIsChanged(true);
                        }
                    }
                });

                table.setModel(tableModel);
                table.setCellSelectionEnabled(true);
                table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                table.getColumnModel().getColumn(0).setMaxWidth(50);
                table.getColumnModel().getColumn(0).setPreferredWidth(40);
                revalidate();
                table.repaint();

            } else if (file.getName().contains("hex")) {
                scrollPaneTable.setVisible(false);
                scrollPane.setVisible(false);
                scrollPane2.setVisible(true);
                StyledEditorKit kit = new StyledEditorKit();
                document2 = kit.createDefaultDocument();
                mainText2.setDocument(document2);
                kit.read(br, document2, 0);
                mainText2.setEditable(false);
                mainText2.setBackground(new Color(0xE8F4F8));
                revalidate();
                mainText2.repaint();

            } else {
                scrollPane.setVisible(false);
                scrollPaneTable.setVisible(false);
                mainText.repaint();
                revalidate();
            }

            in.close();
            state.setTableIsChanged(false);
            state.setSavingUpdate(mainText.getText());
            state.setSavedTextForSymbol(mainText.getText());

        } catch (FileNotFoundException e) {
            logger.error("File {} not found.", e.getMessage());
        } catch (BadLocationException e) {
            logger.error("BadLocationException : {}", e.getMessage());
        } catch (IOException e) {
            logger.error("IOException : {}", e.getMessage());
        } finally {
            try { if (br != null) br.close(); } catch (IOException e) { logger.error("Error closing BufferedReader", e); }
            try { if (in != null) in.close(); } catch (IOException e) { logger.error("Error closing InputStream", e); }
        }
    }

    // --- Save ---

    public void saveFile() {
        state.setTableIsChanged(false);
        File file = state.getFile();
        Charset charset = state.getCurrentCharset();
        if (charset == null) {
            charset = Charset.forName("UTF-8");
            state.setCurrentCharset(charset);
        }
        encodingFileWriter(charset, file);
        state.markSaved(mainText.getText());
        if (owningTabbedPane != null) {
            owningTabbedPane.getFileChangeMonitor().markSavedByEditor(file);
        }
    }

    private void encodingFileWriter(Charset charset, File file) {
        try (OutputStream out = new FileOutputStream(file);
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, charset))) {
            String textAreaContent;
            if (file.getName().contains("txt")) {
                textAreaContent = getTextFromTable();
                state.setTableIsChanged(false);
            } else {
                textAreaContent = mainText.getText();
            }
            state.setSavingUpdate(textAreaContent);
            pw.write(textAreaContent);
        } catch (IOException e) {
            logger.error("Error in encodingFileWriter", e);
        }
    }

    private String getTextFromTable() {
        StringBuilder sb = new StringBuilder();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Vector data = model.getDataVector();
        for (int i = 0; i <= data.size() - 1; i++) {
            StringJoiner joiner = new StringJoiner("\t");
            Vector<String> row = (Vector<String>) data.get(i);
            for (String cs : row) {
                joiner.add(cs == null ? "" : cs);
            }
            sb.append(joiner.toString()).append("\n");
        }
        return sb.toString();
    }

    // --- Syntax highlighting ---

    public void updateColorDocument() {
        try {
            String inText = document.getText(0, document.getLength());
            StyledDocument colored = AsmEditor.getColorDocument(inText);

            StyledDocument styledDoc = (StyledDocument) document;
            styledDoc.removeUndoableEditListener(state.getUndoHandler());

            int pos = 0;
            while (pos < colored.getLength()) {
                Element elem = colored.getCharacterElement(pos);
                AttributeSet attr = elem.getAttributes();
                int start = elem.getStartOffset();
                int end = elem.getEndOffset();
                styledDoc.setCharacterAttributes(start, end - start, attr, true);
                pos = end;
            }

            // Connect document to text pane if not already connected (first load)
            if (mainText.getDocument() != document) {
                mainText.setDocument(document);
            }

            styledDoc.addUndoableEditListener(state.getUndoHandler());
            mainText.setBackground(new Color(0xE8F4F8));
            mainText.repaint();
        } catch (BadLocationException e) {
            logger.error("Error in updateColorDocument", e);
            document.addUndoableEditListener(state.getUndoHandler());
        }
    }

    // --- Output codes ---

    public void updateOutputCodes() {
        File file = state.getFile();
        OutputCodes outputCodes = Model.getEditorModel().getOutputTextCodes(file);
        if (outputCodes != null) {
            try {
                StyledDocument doc = AsmEditor.getColorDocument(outputCodes.getTextClearAssemblerCodes());
                paneClearAssembler.setDocument(doc);
                paneClearAssembler.setBackground(new Color(0xE8F4F8));
            } catch (BadLocationException e) {
                logger.error("Error in updateOutputCodes", e);
                paneClearAssembler.setText(outputCodes.getTextClearAssemblerCodes());
                paneClearAssembler.setBackground(new Color(0xE8F4F8));
            }
            paneHexCodes.setText(outputCodes.getTextHexCodes());
            paneHexCodes.setBackground(new Color(0xE8F4F8));
            paneBinaryCodes.setText(outputCodes.getTextBinaryCodes());
            paneBinaryCodes.setBackground(new Color(0xE8F4F8));

            List<String> list = Model.getParserModel().getHexAd3s();
            StringBuilder text = new StringBuilder();
            for (String line : list) {
                text.append(line).append("\n");
            }
            mainText2.setText(text.toString());
            mainText2.setBackground(new Color(0xE8F4F8));
        } else {
            paneClearAssembler.setText("");
            paneClearAssembler.setBackground(new Color(0xE8F4F8));
            paneHexCodes.setText("");
            paneHexCodes.setBackground(new Color(0xE8F4F8));
            paneBinaryCodes.setText("");
            paneBinaryCodes.setBackground(new Color(0xE8F4F8));
        }
    }

    // --- Dirty check ---

    public boolean isUnchanged() {
        return !state.isDirty(mainText.getText());
    }

    public void reloadFromDisk() {
        int caretPos = mainText.getCaretPosition();
        loadFile();
        if (mainText.isShowing() && caretPos <= mainText.getDocument().getLength()) {
            mainText.setCaretPosition(caretPos);
        }
        updateOutputCodes();
    }

    // --- Caret/scroll save-restore for tab switching ---

    public void savePosition() {
        if (mainText.isShowing()) {
            state.setCaretPosition(mainText.getCaretPosition());
        }
    }

    public void restorePosition() {
        if (mainText.isShowing()) {
            int pos = state.getCaretPosition();
            if (pos <= mainText.getDocument().getLength()) {
                mainText.setCaretPosition(pos);
            }
        }
    }
}
