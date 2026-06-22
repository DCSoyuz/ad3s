package ru.dcsoyuz.ad3s.form.editor;


import org.apache.commons.lang3.SerializationUtils;
import ru.dcsoyuz.ad3s.form.AppFrame;
import ru.dcsoyuz.ad3s.form.AppFrameHelper;
import ru.dcsoyuz.ad3s.form.editor.FileEditorPanel;
import ru.dcsoyuz.ad3s.form.editor.table.ExcelAdapter;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.parser.CommandGroup;
import ru.dcsoyuz.ad3s.model.fpga.parser.CpuCommand;
import ru.dcsoyuz.ad3s.model.fpga.parser.ModeAttrFile;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * GUI and functionality of main window of the application.<br>
 * Singleton class, implements {@link java.awt.event.ActionListener} interface.
 *
 * TODO: fix status bar disappearance by adding JPanel under JLabel(statusBar)
 * TODO: improve undo/redo with CompountEdit class to undo/redo actions but not letters
 *
 * @author Mansur Y.
 */
public class AsmEditor extends JPanel implements ActionListener, IDataEditorListener, IAppFrameEventListener {

	private static final long serialVersionUID = 5410543365658652126L;
	/**
	 * Reference to AsmEditor class.
	 */
	private static AsmEditor instance;
	/**
	 * App's title name
	 */

	/**
	 * Size of app's width window size
	 */
	private final int WIDTH = 1200;
	/**
	 * Size of app's height window size
	 */
	final static  Integer font_size = 11;
	private final int HEIGHT = 800;
	/**
	 * <b>int</b> BUFFER_SIZE size of BufferedReader.
	 * Use default(8192) for small files which is suitable for 99% cases in daily usage.
	 * Use 32Mb (33554432) or even 64Mb(67108864) for very big files(over 1 mil lines).
	 */
	private final int BUFFER_SIZE = 8912;
	/**
	 * Map with available character sets in a system
	 */
	private Map<String, Charset> charsets = Charset.availableCharsets();
	/**
	 * Arrays which contains commonly used character sets names
	 */
	private String[] commonCharsets = {"windows-1256", "windows-1250", "windows-1257",
			"windows-1255", "windows-1254", "windows-1258", "ISO-8859-7", "ISO-8859-2",
			"Shift_JIS", "GB18030", "Big5", "ISO-8859-1", "GB2312", "GBK", "UTF-8",
			"windows-1251", "windows-1252"};
	/**
	 * JMenuBar for File, Edit, View, Help JMenu
	 */
	private JMenuBar menuBar;
	/**
	 * Scroll pane for main JTextArea
	 */
	private JScrollPane scrollPane;
	private JScrollPane scrollPane2;
	private boolean tableIsChanged = false;
	private JScrollPane scrollPaneTable;
	private JTable table;

	/**
	 * JMenu for File, View, Help, Edit and encoding menus on menu bar
	 */
	private JMenu file, view, help, edit, encoding;
	/**
	 * Pop up menu for the text are with basic features
	 */

	private JPopupMenu menuPop;
	/**
	 * Tool bar which mostly duplicates pop up menu features
	 */
	private JToolBar toolBar;

	/**
	 * All JButton of main app's window
	 */

	private Integer numOK = 0;

	private JButton buttonUndo, buttonRedo, buttonCut, buttonCopy,
		buttonPaste, buttonSelectAll, buttonDelete, buttonSearch,
		buttonNewFile, buttonSave, buttonOpen, buttonParse;




	/**
	 * JMenuItems of all items in JMenu and in pop up menu
	 */
	private JMenuItem itemNewFile, itemOpen, itemSave, itemSaveAs, itemAbout, itemExit,
	itemSelectAll, itemCopy, itemPaste, itemCut, itemDelete, itemUndo, itemRedo, folderOpen;
	/**
	 * Array with radio buttons for encoding JMenuItem in Edit JMenu
	 */
	private ArrayList<JRadioButtonMenuItem> encodItems = new ArrayList<JRadioButtonMenuItem>();
	/**
	 * Button group used to group single radio buttons, so only one button
	 * would be pressed at the time
	 */
	private ButtonGroup group = new ButtonGroup();
	protected UndoableEditListener undoHandler = new UndoHandler();
	protected UndoManager undo = new UndoManager();
	private UndoAction 	undoAction = new UndoAction();
	private RedoAction redoAction = new RedoAction();

	private Charset currentCharset = null;
	/**
	 * Label for a status bar
	 */
	private JLabel statusBar = new JLabel();
	/**
	 * Check box items for the View JMenu
	 */

	private JPanel mainZone;


	private JCheckBoxMenuItem itemStatusBar;
	private JCheckBoxMenuItem itemEditBar;
	private JCheckBoxMenuItem itemWrapLines;
	/**
	 * Icons for the application loaded from current class's classpath.
	 */
	private ImageIcon mainIcon = new ImageIcon(ClassLoader.getSystemResource("icons/icon_logo.png"));
	private ImageIcon iconNew = new ImageIcon(ClassLoader.getSystemResource("icons/new.png"));
	private ImageIcon iconOpen = new ImageIcon(ClassLoader.getSystemResource("icons/open.png"));
	private ImageIcon iconSave = new ImageIcon(ClassLoader.getSystemResource("icons/save.png"));
	private ImageIcon iconAbout = new ImageIcon(ClassLoader.getSystemResource("icons/about.png"));
	private ImageIcon iconExit = new ImageIcon(ClassLoader.getSystemResource("icons/exit.png"));
	private ImageIcon iconSelectAll = new ImageIcon(ClassLoader.getSystemResource("icons/selectAll.png"));
	private ImageIcon iconCut = new ImageIcon(ClassLoader.getSystemResource("icons/cut.png"));
	private ImageIcon iconCopy = new ImageIcon(ClassLoader.getSystemResource("icons/copy.png"));
	private ImageIcon iconPaste = new ImageIcon(ClassLoader.getSystemResource("icons/paste.png"));
	private ImageIcon iconDelete = new ImageIcon(ClassLoader.getSystemResource("icons/delete.png"));
	private ImageIcon iconUndo = new ImageIcon(ClassLoader.getSystemResource("icons/undo.png"));
	private ImageIcon iconRedo = new ImageIcon(ClassLoader.getSystemResource("icons/redo.png"));
	private ImageIcon iconSearch= new ImageIcon(ClassLoader.getSystemResource("icons/search.png"));
	private ImageIcon iconWarning = new ImageIcon(ClassLoader.getSystemResource("icons/warning.png"));


	private WordSearcher searcher;
	/**
	 * Clipboard for copy, paste system wide feature
	 */
	private Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	/**
	 * String holder for selected text in a text area
	 */
	private String selectedText;
	private StringSelection selection;
	/**
	 * Current line position in a text area
	 */
	private int lineCounter;
	/**
	 * Current caret position in a text area
	 */
	private int caretPos;
	/**
	 * Column counter in a text area
	 */
	private int columnCounter;
	/**
	 * Text to paste from a buffer
	 */
	private String pasteText;
	/**
	 * Reference to a About Dialog class
	 */
	private AboutDialog aboutDialog;


	private JLabel textStatus;


	private AppFrame appFrame;
	/**
	 * Undo handler, manager and actions for undo, redo features
	 */

	/**
	 * Main text JTextArea
	 */
	private JTextPane paneHexCodes;
	private JTextPane paneBinaryCodes;
	private JTextPane paneClearAssembler;
	private TextPane mainText;
	private TextPane mainText2;
	/**
	 * Document model for a text area
	 */
	private Document document;

	private Document document2;
	/**
	 * Rectangle for window bounds
	 */
	private Rectangle rec;
	/**
	 * Reference to a Search Dialog class
	 */
	private SearchDialog searchDialog;
	/**
	 * Boolean flag for a search button on a tool bar
	 */
	private boolean toggleButIsSelected;
	/**
	 * Current file obj
	 */
	private File currentFile;
	/**
	 * String holder for checking '*' which shows if opened file
	 * has been modifies yet or not.
	 */
	private String savedTextForSymbol;
	/**
	 * String holder for a text from main text area
	 * which shows if file was modified after last 'Save' or 'Save as..'
	 * Servers as comparison between current text and last time saved(savingUpdate)
	 */
	private String savingUpdate = "";


	/**
	 * Boolean flag which shows is file has been saved successfully or
	 * there has been a problem with accessing file etc.
	 */
	private boolean savedSuccessfully = false;
	private DefaultTableModel savedTableModel;
	/**
	 * MainClass's reference to get a string from command line(args)
	 */

	private List<List<String>> copyTableValues;


	private boolean modeTraded = false;
	private EditorTabbedPane tabbedPane;

	public  AsmEditor(AppFrame appFrame) {
		this.appFrame = appFrame;
		initializationEditor();
		Model.getEditorModel().addAppFrameEventListener(this);
		Model.getEditorModel().addDataEditorListener(this);
		tabbedPane.openFile(Model.getEditorModel().getCurrentFile());
	}

	/**
	 * Initialization all app's GUI and functionality
	 */
	private void initializationEditor() {

		toolBar = new JToolBar(JToolBar.HORIZONTAL);
		toolBar.setBackground(new Color(0x5B9BD5)); // ТОЛЬКО фон, без рамки
		final StyleContext cont = StyleContext.getDefaultStyleContext();
		final AttributeSet attr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.RED);
		final AttributeSet attrBlack = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.BLACK);

		mainText = new TextPane();
		mainText.setBackground(new Color(0x8BC7FA)); // Очень светлый голубой AliceBlue
		mainText.setForeground(Color.BLACK); // Черный текст
		mainText.setFocusable(true);	// to get focus over buttons
		mainText.setSelectedTextColor(Color.BLACK);
		mainText.setSelectionColor(new Color(0x7BC0E0)); // Светло-голубое выделение
		mainText2 = new TextPane();
		mainText2.setBackground(new Color(0x8ECCFF)); // Очень светлый голубой
		mainText2.setForeground(Color.BLACK);


		menuBar = new JMenuBar();
		menuBar.setBorder(BorderFactory.createRaisedBevelBorder());
		StyledEditorKit kit = new StyledEditorKit();
		document = kit.createDefaultDocument();
		updateColorDocument();
		file = new JMenu("File");
		edit = new JMenu("Edit");
		view = new JMenu("View");
		help = new JMenu("Help");

		scrollPane = new JScrollPane(mainText);
		scrollPane.setBackground(new Color(0x2D4A6C)); // Темно-синий фон панели
		scrollPane.getViewport().setBackground(new Color(0x8AC2F1)); // Светло-голубой viewport
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane2 = new JScrollPane(mainText2);
		scrollPane2.setBackground(new Color(0x2D4A6C)); // Темно-синий фон панели
		scrollPane2.getViewport().setBackground(new Color(0xE8F4F8)); // Светло-голубой viewport
		scrollPane2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		menuPop = new JPopupMenu();
		statusBar.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
		// removes scrolling bug with blurred text in Linux OS
		//scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

		rec = windowMovingRegister(this);

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				searchDialog = SearchDialog.getInstance(appFrame);
			}
		});


		TextLineNumber tlnMAIN = new TextLineNumber(mainText);
		TextLineNumber tlnMAIN2 = new TextLineNumber(mainText2);
		scrollPane.setRowHeaderView(tlnMAIN);
		scrollPane2.setRowHeaderView(tlnMAIN2);
		encoding = new JMenu("Encoding");
		encoding.setAutoscrolls(true);
		encoding.setToolTipText("List of Encoding options");

		itemNewFile = new JMenuItem("New", iconNew);
		itemNewFile.setToolTipText("Create a new file");
		itemNewFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_MASK));
		itemNewFile.setActionCommand("new");
		itemNewFile.addActionListener(this);

		itemOpen = new JMenuItem("Open...", iconOpen);
		itemOpen.setToolTipText("Open a file");
		itemOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_MASK));
		itemOpen.setActionCommand("open");
		itemOpen.addActionListener(this);

		folderOpen = new JMenuItem("Open folder...", iconOpen);
		folderOpen.setToolTipText("Open a folder");
		folderOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, KeyEvent.CTRL_MASK));
		folderOpen.setActionCommand("openFolder");
		folderOpen.addActionListener(this);


		itemSave = new JMenuItem("Save", iconSave);
		itemSave.setToolTipText("Save the file");
		itemSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
		itemSave.setActionCommand("save");
		itemSave.addActionListener(this);

		itemSaveAs = new JMenuItem("Save As...", iconSave);
		itemSaveAs.setToolTipText("Save the file");
		itemSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK
				| ActionEvent.SHIFT_MASK));
		itemSaveAs.setActionCommand("saveAs");
		itemSaveAs.addActionListener(this);

		itemAbout = new JMenuItem("About", iconAbout);
		itemAbout.setToolTipText("About the program");
		itemAbout.setActionCommand("about");
		itemAbout.addActionListener(this);

		itemExit = new JMenuItem("Exit", iconExit);
		itemExit.setToolTipText("Exit the application");
		itemExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.CTRL_MASK));
		itemExit.setActionCommand("exit");
		itemExit.addActionListener(this);

		mouseListenerInTextArea(mainText);
		inputListenerInTextArea(mainText);
		selectiveTextSelection(mainText);
		mainText.setMargin(new Insets(4, 8, 4, 8));

		itemSelectAll = new JMenuItem("Select All", iconSelectAll);
		itemSelectAll.setToolTipText("Select all text in this text area");
		itemSelectAll.setActionCommand("selectAll");
		itemSelectAll.addActionListener(this);

		itemCopy = new JMenuItem("Copy", iconCopy);
		itemCopy.setToolTipText("Copy to a clipboard");
		itemCopy.setActionCommand("copy");
		itemCopy.addActionListener(this);

		itemPaste = new JMenuItem("Paste", iconPaste);
		itemPaste.setToolTipText("Paste from a clipboard");
		itemPaste.setActionCommand("paste");
		itemPaste.addActionListener(this);

		itemCut = new JMenuItem("Cut", iconCut);
		itemCut.setToolTipText("Cut to a clipboard");
		itemCut.setActionCommand("cut");
		itemCut.addActionListener(this);

		itemDelete = new JMenuItem("Delete", iconDelete);
		itemDelete.setToolTipText("Delete the selected text");
		itemDelete.setActionCommand("delete");
		itemDelete.addActionListener(this);

		itemUndo = new JMenuItem(redoAction);
		itemUndo.setText("Undo");
		itemUndo.setIcon(iconUndo);
		itemUndo.setToolTipText("Undo last action");
		itemUndo.setActionCommand("undo");
		itemUndo.addActionListener(this);

		itemRedo = new JMenuItem(undoAction);
		itemRedo.setText("Redo");
		itemRedo.setIcon(iconRedo);
		itemRedo.setToolTipText("Redo last action");
		itemRedo.setActionCommand("redo");
		itemRedo.addActionListener(this);

		buttonNewFile = new JButton("New", iconNew);
		buttonNewFile.setToolTipText("Create a new file");
		buttonNewFile.setActionCommand("new");
		buttonNewFile.addActionListener(this);
		buttonNewFile.setFocusable(false); // all buttons don't have a focus, so text area could have

		buttonOpen = new JButton("Open", iconOpen);
		buttonOpen.setToolTipText("Open a file");
		buttonOpen.setActionCommand("open");
		buttonOpen.addActionListener(this);
		buttonOpen.setFocusable(false);

		buttonSave = new JButton("Save", iconSave);
		buttonSave.setToolTipText("Save a file");
		buttonSave.setActionCommand("save");
		buttonSave.addActionListener(this);
		buttonSave.setFocusable(false);

		// setAccelerator and setMnemonic both don't give possibility
		// to set CTRL+Z or SHIFT+CTRL+Z shortcuts
		// so instead used dirty and manual way below
		KeyStroke keyUndo = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK);
		Action performUndo = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for(ActionListener al : buttonUndo.getActionListeners()) {
					al.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
				}
			}
		};
		buttonUndo = new JButton(undoAction);
		buttonUndo.setText("Undo");
		buttonUndo.setIcon(iconUndo);
		buttonUndo.setToolTipText("Undo the last action");
		buttonUndo.getActionMap().put("performUndo", performUndo);
		buttonUndo.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyUndo, "performUndo");
		buttonUndo.setFocusable(false);

		KeyStroke keyRedo = KeyStroke.getKeyStroke(KeyEvent.VK_Z, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK);
		Action performRedo = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				for(ActionListener al : buttonRedo.getActionListeners()) {
					al.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
				}
			}
		};
		buttonRedo = new JButton(redoAction);
		buttonRedo.setText("Redo");
		buttonRedo.setIcon(iconRedo);
		buttonRedo.setToolTipText("Redo the last Undo action");
		buttonRedo.getActionMap().put("performRedo", performRedo);
		buttonRedo.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyRedo, "performRedo");
		buttonRedo.setFocusable(false);

		buttonCut = new JButton(iconCut);
		buttonCut.setToolTipText("Cut the selected text");
		buttonCut.setActionCommand("cut");
		buttonCut.addActionListener(this);
		buttonCut.setFocusable(false);

		buttonCopy = new JButton(iconCopy);
		buttonCopy.setToolTipText("Copy the selected text");
		buttonCopy.setActionCommand("copy");
		buttonCopy.addActionListener(this);
		buttonCopy.setFocusable(false);

		buttonPaste = new JButton(iconPaste);
		buttonPaste.setToolTipText("Paste the text from a clipboard");
		buttonPaste.setActionCommand("paste");
		buttonPaste.addActionListener(this);
		buttonPaste.setFocusable(false);

		buttonSelectAll = new JButton(iconSelectAll);
		buttonSelectAll.setToolTipText("Select all text");
		buttonSelectAll.setActionCommand("selectAll");
		buttonSelectAll.addActionListener(this);
		buttonSelectAll.setFocusable(false);

		buttonDelete = new JButton(iconDelete);
		buttonDelete.setToolTipText("Delete the selected text");
		buttonDelete.setActionCommand("delete");
		buttonDelete.addActionListener(this);
		buttonDelete.setFocusable(false);

		KeyStroke keyParse = KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK);
		Action performParse= new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				//parseAssembler();
			}
		};

		buttonParse = new JButton(performParse);
		buttonParse.setIcon(iconAbout);
		buttonParse.setToolTipText("Parse assembly to generate assembly");
		buttonParse.setActionCommand("parse");
		buttonParse.addActionListener(this);
		buttonParse.setFocusable(false);
		buttonParse.getActionMap().put("performParse", performParse);
		buttonParse.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyParse, "performParse");

		textStatus = new JLabel("Editing...");


		KeyStroke keySearch = KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK);
		Action performSearch = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				searchWindowLocationLogic();
			}
		};
		buttonSearch = new JButton(performSearch);
		buttonSearch.setIcon(iconSearch);
		buttonSearch.setToolTipText("Search and replace a text");
		buttonSearch.setSelected(toggleButIsSelected);
		searchWindowLocation(buttonSearch);
		buttonSearch.getActionMap().put("performSearch", performSearch);
		buttonSearch.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keySearch, "performSearch");
		buttonSearch.setFocusable(false);





		menuPop.add(itemUndo);
		menuPop.add(itemRedo);
		menuPop.addSeparator();
		menuPop.add(itemCut);
		menuPop.add(itemCopy);
		menuPop.add(itemPaste);
		menuPop.add(itemDelete);
		menuPop.addSeparator();
		menuPop.add(itemSelectAll);

		//toolBar.add(buttonNewFile);
		toolBar.add(buttonOpen);
		toolBar.add(buttonSave);
		toolBar.addSeparator();
		toolBar.add(buttonUndo);
		toolBar.add(buttonRedo);
		toolBar.addSeparator();
		toolBar.add(buttonCut);
		toolBar.add(buttonCopy);
		toolBar.add(buttonPaste);
		toolBar.add(buttonDelete);
		toolBar.addSeparator();
		toolBar.add(buttonSelectAll);
		toolBar.addSeparator();
		toolBar.add(buttonSearch);
		toolBar.add(buttonParse);
		toolBar.add(textStatus);

		itemStatusBar = new JCheckBoxMenuItem("Statusbar");
		itemStatusBar.setToolTipText("Show statusbar");
		itemStatusBar.setState(true);
		barsCheckBox(itemStatusBar, null, statusBar);
		view.add(itemStatusBar);
		view.setToolTipText("View settings of the program");

		itemEditBar = new JCheckBoxMenuItem("Toolbar");
		itemEditBar.setToolTipText("Show edit toolbar");
		itemEditBar.setState(true);
		barsCheckBox(itemEditBar, toolBar, null);
		view.add(itemEditBar);

		itemWrapLines = new JCheckBoxMenuItem("Wrap Lines", false);
		itemWrapLines.setToolTipText("Wrap lines depends on window's size");
		//lineWrapOption(mainText, itemWrapLines);
		view.add(itemWrapLines);

		edit.add(encoding);
		getSelectedEncoding();

		document.addUndoableEditListener(undoHandler);
		clearUndoCounter();

		menuBar.add(file);
		menuBar.add(edit);
		menuBar.add(view);
		menuBar.add(help);
		//file.add(itemNewFile);
		file.add(itemOpen);
		file.add(folderOpen);
		file.add(itemSave);
		file.add(itemSaveAs);
		file.addSeparator();
		file.add(itemExit);
		help.add(itemAbout);

		searcher = new WordSearcher(mainText);






		paneClearAssembler = new JTextPane();
		paneClearAssembler.setBackground(new Color(0x93C6F3)); // Очень светлый голубой AliceBlue



		TextLineNumber tlnCA = new TextLineNumber(paneClearAssembler);
		JScrollPane spClearAssembler = new JScrollPane(paneClearAssembler, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		spClearAssembler.setBackground(new Color(0x2D4A6C)); // Темно-синий фон
		spClearAssembler.getViewport().setBackground(new Color(0x93C9FB)); // Светло-голубой viewport
		spClearAssembler.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		spClearAssembler.setRowHeaderView( tlnCA );


		paneBinaryCodes = new JTextPane();
		paneBinaryCodes.setBackground(new Color(0x6ABDFB)); // Очень светлый голубой
		Font font = paneBinaryCodes.getFont();

		paneBinaryCodes.setFont(new Font( font.getFamily(), font.getStyle(),  font_size));

		TextLineNumber tln = new TextLineNumber(paneBinaryCodes);
		JScrollPane spMachineCodes = new JScrollPane(paneBinaryCodes, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spMachineCodes.setBackground(new Color(0x2D4A6C)); // Темно-синий фон
		spMachineCodes.getViewport().setBackground(new Color(0x75BFFF)); // Светло-голубой viewport
		spMachineCodes.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		spMachineCodes.getVerticalScrollBar().setModel(spClearAssembler.getVerticalScrollBar().getModel());
		paneBinaryCodes.addMouseWheelListener(e -> {
			spClearAssembler.dispatchEvent(e);
		});
		spMachineCodes.setRowHeaderView( tln );

		paneHexCodes = new JTextPane();
		paneHexCodes.setBackground(new Color(0x7ABEFA)); // Очень светлый голубой
		paneHexCodes.setFont(new Font( font.getFamily(), font.getStyle(),  font_size));
		TextLineNumber tlnHEX = new TextLineNumber(paneHexCodes);
		JScrollPane spHexCodes = new JScrollPane(paneHexCodes, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		spHexCodes.setBackground(new Color(0x2D4A6C)); // Темно-синий фон
		spHexCodes.getViewport().setBackground(new Color(0x7CC1F8)); // Светло-голубой viewport
		spHexCodes.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
		spHexCodes.getVerticalScrollBar().setModel(spClearAssembler.getVerticalScrollBar().getModel());
		paneHexCodes.addMouseWheelListener(e -> {
			spClearAssembler.dispatchEvent(e);
		});
		spHexCodes.setRowHeaderView( tlnHEX );




		JSplitPane outputCodesPane2 = new JSplitPane();
		outputCodesPane2.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		outputCodesPane2.setDividerSize(8);
		//outputCodesPane2.setPreferredSize(new Dimension(getWidth(), 0));
		outputCodesPane2.setDividerLocation(0.5);
		outputCodesPane2.setOneTouchExpandable(true);
		outputCodesPane2.setResizeWeight(0.5);
		outputCodesPane2.setLeftComponent(spHexCodes);
		outputCodesPane2.setRightComponent(spMachineCodes);


		JSplitPane outputCodesPane1 = new JSplitPane();
		outputCodesPane1.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		outputCodesPane1.setDividerSize(8);
		//outputCodesPane1.setPreferredSize(new Dimension(getWidth(), 0));
		outputCodesPane1.setDividerLocation(0.5);
		outputCodesPane1.setOneTouchExpandable(true);
		outputCodesPane1.setResizeWeight(0.5);
		outputCodesPane1.setLeftComponent(spClearAssembler);
		outputCodesPane1.setRightComponent(outputCodesPane2);





		mainZone = new JPanel();
		table = new JTable();
		table.setShowGrid(true);
		table.setGridColor(new java.awt.Color(0x6A8AAC));


		//createKeybindings();
		table.setCellSelectionEnabled(true);
		ExcelAdapter myAd = new ExcelAdapter(table);
		scrollPaneTable = new JScrollPane(table);

		scrollPaneTable.setVisible(false);
		//table.getInputMap().remove(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK));
		//table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK), "save");
		mainZone.setLayout(new BoxLayout(mainZone, BoxLayout.Y_AXIS));
		mainZone.add(scrollPane);
		mainZone.add(scrollPane2);
		mainZone.add(scrollPaneTable, BorderLayout.CENTER);



		// Create tabbed pane for multi-file editing
		tabbedPane = new EditorTabbedPane(appFrame, textStatus);
		setLayout(new BorderLayout());
		add(tabbedPane, BorderLayout.CENTER);











	}

	/**
	 * Handles exit from application reature with appearing
	 * menu with few options.
	 */
	public void exitApplication() {




		if(mainText.getText().equals(savingUpdate)) {
			WorkstationConfig.storeProperties();
			System.exit(0);
		} else {
			String exitMessage = "Save changes before leaving?";
			String[] options = {"Save", "Exit without saving", "Cancel"};
			int returnedOption = JOptionPane.showOptionDialog(this, exitMessage, null,
					JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
					iconWarning, options, null);
			if(returnedOption == JOptionPane.YES_OPTION) {
				saveFile();
				if(currentFile != null) {
					WorkstationConfig.storeProperties();
					System.exit(0);
				}
			} else if(returnedOption == JOptionPane.NO_OPTION) {
				WorkstationConfig.storeProperties();
				System.exit(0);
			}
		}
	}


	private int findLastNonWordChar (String text, int index) {
		while (--index >= 0) {
			if (String.valueOf(text.charAt(index)).matches("\\W")) {
				break;
			}
		}
		return index;
	}

	private int findFirstNonWordChar (String text, int index) {
		while (index < text.length()) {
			if (String.valueOf(text.charAt(index)).matches("\\W")) {
				break;
			}
			index++;
		}
		return index;
	}

	/**
	 * Create about dialog of AboutDialog class
	 */
	private void aboutDialog() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				aboutDialog = new AboutDialog();
				aboutDialog.setLocationRelativeTo(AsmEditor.this);
				aboutDialog.setVisible(true);
			}
		});
	}

	/**
	 * Create new file method which runs before main logic
	 * and it checks if current file has been opened and
	 * if it needs a save changes
	 */
	private void createNewFile() {
		if (askSaveIfNeeded()) {
			createNewFileLogic();
		}
	}

	public String getSavingUpdate() {
		return savingUpdate;
	}

	/**
	 * Main logic of create new file
	 */
	private void createNewFileLogic() {
		Document oldDocument = mainText.getDocument();
		if (oldDocument != null) {
			oldDocument.removeUndoableEditListener(undoHandler);
		}
		StyledEditorKit kit = new StyledEditorKit();
		document = kit.createDefaultDocument();
		mainText.setDocument(document);
		if (currentFile != null) {
			currentFile = new File(currentFile.getParentFile().getPath() + File.separator + "temp.asm");
			Model.getEditorModel().setCurrentFileQuiet(currentFile);
		}
		appFrame.setAppTitle(AppFrameHelper.APLICATION_NAME);
		mainText.getDocument().addUndoableEditListener(undoHandler);
		clearUndoCounter();
	}

	/**
	 * Reset undo and redo actions
	 */
	private void clearUndoCounter() {
		undo.discardAllEdits();
		undoAction.update();
		redoAction.update();
	}

	private boolean isUnchanged() {
		if (currentFile == null) return true;
		if (currentFile.getName().contains("txt")) {
			return !tableIsChanged;
		}
		return !appFrame.getAppTitle().startsWith("*")
				&& mainText.getText().equals(savingUpdate);
	}

	private void discardChanges() {
		savingUpdate = mainText.getText();
		tableIsChanged = false;
		if (currentFile != null) {
			appFrame.setAppTitle(currentFile.getName() + " - " + AppFrameHelper.APLICATION_NAME);
		}
	}

	private boolean askSaveIfNeeded() {
		if (isUnchanged()) return true;
		String message = "Do you want to save "
				+ (currentFile != null ? currentFile.getAbsolutePath() : "") + " file?";
		int option = JOptionPane.showConfirmDialog(
				this, message, null, JOptionPane.YES_NO_CANCEL_OPTION);
		if (option == JOptionPane.YES_OPTION) {
			saveFile();
			return true;
		} else if (option == JOptionPane.NO_OPTION) {
			discardChanges();
			return true;
		}
		return false;
	}

	private void openFolderLogic(File folder) {
		File[] files = folder.listFiles();
		if (files == null) return;
		File targetFile = null;
		for (File f : files) {
			if (f.isFile() && f.getName().contains(".txt") && !f.getName().contains(".config")) {
				targetFile = f;
				break;
			}
		}
		if (targetFile == null) {
			for (File f : files) {
				if (f.isFile() && f.getName().contains(".asm")) {
					targetFile = f;
					break;
				}
			}
		}
		if (targetFile != null) {
			Model.getEditorModel().setCurrentFile(targetFile);
		}
	}



		private void openFolder() {
			Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
			FileDialog dialog = new FileDialog(parentFrame, "Open Folder", FileDialog.LOAD);
			File activeFile = currentFile != null ? currentFile : Model.getEditorModel().getCurrentFile();
			if (activeFile != null) {
				dialog.setDirectory(activeFile.getParent());
			}
			dialog.setVisible(true);
			String dir = dialog.getDirectory();
			String name = dialog.getFile();
			if (dir != null && name != null) {
				if (askSaveIfNeeded()) {
					openFolderLogic(new File(dir, name));
				}
			}
		}
	/**
	 * Open file method which runs before open file logic method
	 */
			private void openFile() {
			Frame parentFrame = (Frame) SwingUtilities.getWindowAncestor(this);
			FileDialog dialog = new FileDialog(parentFrame, "Open File", FileDialog.LOAD);
			dialog.setFile("*.asm;*.txt;*.hex");
			File activeFile = currentFile != null ? currentFile : Model.getEditorModel().getCurrentFile();
			if (activeFile != null) {
				dialog.setDirectory(activeFile.getParent());
			}
			dialog.setVisible(true);
			String dir = dialog.getDirectory();
			String name = dialog.getFile();
			if (dir != null && name != null) {
				File selectedFile = new File(dir, name);
				if (askSaveIfNeeded()) {
					Model.getEditorModel().setCurrentFile(selectedFile);
				}
			}
			savedTextForSymbol = mainText.getText();
			savingUpdate = mainText.getText();
		}


public void openFileFromTree(File file) {
		Model.getEditorModel().setCurrentFile(file);
	}

	public void openFileLogic2(File file) {
		BufferedReader br = null;
		InputStream in = null;
		currentFile = file;
		appFrame.setAppTitle(currentFile.getName() + " - " + AppFrameHelper.APLICATION_NAME);
		try{
			in = new FileInputStream(currentFile);
			br = new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFFER_SIZE);
			if(file.getName().contains("asm")){
				scrollPaneTable.setVisible(false);
				scrollPane2.setVisible(false);
				scrollPane.setVisible(true);
				Document oldDocument = mainText.getDocument();
				if(oldDocument != null) {
					oldDocument.removeUndoableEditListener(undoHandler);
				}
				StyledEditorKit kit = new StyledEditorKit();
				document = kit.createDefaultDocument();
				kit.read(br, document, 0);
				updateColorDocument();

				clearUndoCounter();
				savedSuccessfully = true;
				mainText.setEditable(true);
				this.revalidate();
				mainText.repaint();
			}else if(file.getName().contains("txt") ){
				scrollPane.setVisible(false);
				scrollPane2.setVisible(false);
				scrollPaneTable.setVisible(true);
				ModeAttrFile attr = Model.getEditorModel().getCurrentAttr();
				String [] columnNames = {"ind", "value", "comment", "var1", "var2", "var3"};
				DefaultTableModel tableModel = new DefaultTableModel(columnNames, attr.getSize());
				String line;
				String[] at;

				Integer i = 0;
				while(i <= attr.getSize()-1 ){
					at = new String[6];
					if((line = br.readLine())!= null) {
						String[] data = line.split("\t");
						if (data.length != 6) {

							for (int k = 0; k < 6; k++) {
								if (k <= data.length - 1) {
									at[k] = data[k];
								} else {
									at[k] = "";
								}

							}
						}else{
							at = data;
						}
					}
					Integer hexSize = attr.getHexSize();
					if(attr.getNumBits() > 16){
						hexSize = hexSize/2;
					}

					if(i < hexSize) {
						at[0] = i.toString();
					}else{
						at[0] =String.valueOf (i - hexSize);
					}
					tableModel.insertRow(i,at);
					i++;
				}

				tableModel.setRowCount(attr.getSize());
				tableModel.addTableModelListener(new TableModelListener() {

					public void tableChanged(TableModelEvent e) {

						DefaultTableModel currentModel = (DefaultTableModel)table.getModel();
						String newValue = currentModel.getValueAt(e.getFirstRow(), e.getColumn()).toString();
						Object oldValueObj = savedTableModel.getValueAt(e.getFirstRow(), e.getColumn());
						String oldValue = "";
						if(oldValueObj != null){
							oldValue = (String) oldValueObj;
						}
						if(!newValue.equals(oldValue) ) {
							tableIsChanged = true;
						}
					}
				});

				savedTableModel =  SerializationUtils.clone(tableModel);
				table.setModel(tableModel);
				table.setCellSelectionEnabled(true);
				table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
				table.getColumnModel().getColumn(0).setMaxWidth(50);
				table.getColumnModel().getColumn(0).setPreferredWidth(40);
				this.revalidate();
				table.repaint();
			}else if(file.getName().contains("hex") ){
				scrollPaneTable.setVisible(false);
				scrollPane.setVisible(false);
				scrollPane2.setVisible(true);

				StyledEditorKit kit = new StyledEditorKit();
				document2 = kit.createDefaultDocument();
				mainText2.setDocument(document2);
				kit.read(br, document2, 0);

				savedSuccessfully = true;
				mainText2.setEditable(false);
				mainText2.setBackground(new Color(0xE8F4F8)); // AliceBlue
				this.revalidate();
				mainText2.repaint();
			} else {
				scrollPane.setVisible(false);
				scrollPaneTable.setVisible(false);

				mainText.repaint();
				this.revalidate();
			}

			in.close();
			tableIsChanged = false;
			savingUpdate = mainText.getText();
			savedTextForSymbol = mainText.getText();
		} catch (FileNotFoundException e) {
			System.err.println("File " + e.getMessage() + " not found.");
		} catch (BadLocationException e) {
			System.err.println("BadLocationException : " + e.getMessage());
		} catch (IOException e) {
			System.err.println("IOException : " + e.getMessage());
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}


	/**
	 * Read the opened file to a text area with character encoding
	 * @param charset charset from {@link java.nio.charset.Charset} class
	 * @param currentFile file object that used by {@link java.io.FileInputStream}
	 */
	private void encodingFileReader(Charset charset, File currentFile) {
		BufferedReader br = null;
		InputStream in = null;
		try {
			in = new FileInputStream(currentFile);
			br = new BufferedReader(new InputStreamReader(in, charset));
			String line = null;
			StringBuilder newText = new StringBuilder();
			while((line = br.readLine()) != null) {
				newText.append(line);
				newText.append("\n");
			}
			mainText.setText(newText.toString());
		} catch (FileNotFoundException e) {
			System.err.println("FileNotFoundException : " + e.getMessage());
		} catch (IOException e) {
			System.err.println("IOException : " + e.getMessage());
		} finally {
			if(br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * "Save as.." file logic
	 */
	private void saveAsFile() {

		File tempFile = new File("user.home"+File.separator+"temp.txt");
		JFileChooser chooser = new JFileChooser(currentFile != null ? currentFile : Model.getEditorModel().getCurrentFile());
		setFileChooserFilters(chooser);
		int returnedOption = chooser.showSaveDialog(this);
		if(returnedOption == JFileChooser.APPROVE_OPTION) {
			File newFile = chooser.getSelectedFile();
			if(newFile.exists()) {
				String errorMessage = newFile.getAbsolutePath()
						+ " file already exists.\n"
						+ "Do you want to override it?";
				if(JOptionPane.showConfirmDialog(
						this, errorMessage, "File already exists, override?",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE, iconWarning)
						== JOptionPane.YES_OPTION) {
					appFrame.setAppTitle(currentFile.getName() + " - " + AppFrameHelper.APLICATION_NAME);
					if(currentCharset != null) {
						encodingFileWriter(currentCharset, currentFile);
					} else {
						currentCharset = Charset.forName("UTF-8");
						encodingFileWriter(currentCharset, currentFile);
					}
					savedSuccessfully = true;
				}					
			} else {
				currentFile = new File(newFile.getAbsolutePath());
				Model.getEditorModel().setCurrentFile(currentFile);
				appFrame.setAppTitle(currentFile.getName() + " - " + AppFrameHelper.APLICATION_NAME);
				if(currentCharset != null) {
					encodingFileWriter(currentCharset, currentFile);
				} else {
					currentCharset = Charset.forName("UTF-8");
					encodingFileWriter(currentCharset, currentFile);
				}
			}
		}
		if(savedSuccessfully) {
			// to remove '*' from the filename in title bar
			appFrame.setAppTitle(currentFile.getName() + " - " + AppFrameHelper.APLICATION_NAME);
			if(currentCharset != null) {
				encodingFileWriter(currentCharset, currentFile);
			} else {
				currentCharset = Charset.forName("UTF-8");
				encodingFileWriter(currentCharset, currentFile);
			}
			saveDefaultSetting();
		}
		savedSuccessfully = false;
		savedTextForSymbol = mainText.getText();		
	}

	/**
	 * 'Save' file method
	 */
	public void saveFile() {
		if (tabbedPane == null) return;
		FileEditorPanel panel = tabbedPane.getActiveFilePanel();
		if (panel != null) {
			panel.saveFile();
			tabbedPane.markClean(panel.getFile());
			appFrame.setAppTitle(panel.getFile().getName() + " - " + AppFrameHelper.APLICATION_NAME);
		} else {
			saveAsFile();
		}
	}
	
	/**
	 * Encoding file writer for save and save as methods<br>
	 * Saves with selected Charset
	 * @param charset charset to save with
	 * @param currentFile file object
	 */
	private void encodingFileWriter(Charset charset, File currentFile) {
		PrintWriter pw = null;
		OutputStream out = null;
		try {
			out = new FileOutputStream(currentFile);
			pw = new PrintWriter(new OutputStreamWriter(out, charset));
		} catch (IOException e) {
			e.printStackTrace();
		}
		String textAreaContent;
		if(currentFile.getName().contains("txt")){
			textAreaContent = getTextFromTable();
			tableIsChanged = false;
		}else {
			textAreaContent = mainText.getText();
		}
		savingUpdate = textAreaContent;
		pw.write(textAreaContent);
		pw.close();
		try {
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}




	private String getTextFromTable(){

		StringBuilder sb = new StringBuilder();
		DefaultTableModel model = (DefaultTableModel)table.getModel();
		Vector data = model.getDataVector();
		for (int i = 0; i <= data.size() - 1; i++) {
			StringJoiner joiner = new StringJoiner("\t");
			Vector<String> row = (Vector<String>) data.get(i);
			int k = 0;
			for (String cs : row) {
				joiner.add(cs == null ? "":cs);
				k++;
			}
			sb.append(joiner.toString() + "\n");
		}
		return sb.toString();
	}


	private void createKeybindings() {
		InputMap inputMap = table.getInputMap(WHEN_FOCUSED);
		ActionMap actionMap = table.getActionMap();

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
		actionMap.put("delete", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				int row = table.getSelectedRow();
				int col = table.getSelectedColumn();
				row = table.convertRowIndexToModel(row);
				col = table.convertColumnIndexToModel(col);
				int rowCount = table.getSelectedRowCount();
				int colCount = table.getSelectedColumnCount();
				if (row >= 0 && col >= 0) {
					for(int r = row; r < row+rowCount;r++){
						for(int c = col; c < col+colCount; c++ ){
							table.getModel().setValueAt("", r, c);
						}
					}


				}
			}
		});



		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_MASK), "cut");
		actionMap.put("cut", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				List<List<String>>bufferValues = new ArrayList<List<String>>();
				DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
				int row = table.getSelectedRow();
				int col = table.getSelectedColumn();
				row = table.convertRowIndexToModel(row);
				col = table.convertColumnIndexToModel(col);
				int rowCount = table.getSelectedRowCount();
				int colCount = table.getSelectedColumnCount();
				if (row >= 0 && col >= 0) {
					for(int r = row; r < row+rowCount;r++){
						List<String> rowValues = new ArrayList<String>();
						for(int c = col; c < col+colCount; c++ ){
							rowValues.add(tableModel.getValueAt(r, c).toString());
							tableModel.setValueAt("",r, c);
						}
						bufferValues.add(rowValues);
					}
				}
				copyTableValues = bufferValues;

			}
		});


		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK), "copy");
		actionMap.put("copy", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				List<List<String>>bufferValues = new ArrayList<List<String>>();
				DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
				int row = table.getSelectedRow();
				int col = table.getSelectedColumn();
				row = table.convertRowIndexToModel(row);
				col = table.convertColumnIndexToModel(col);
				int rowCount = table.getSelectedRowCount();
				int colCount = table.getSelectedColumnCount();
				if (row >= 0 && col >= 0) {
					for(int r = row; r < row+rowCount;r++){
						List<String> rowValues = new ArrayList<String>();
						for(int c = col; c < col+colCount; c++ ){
							rowValues.add(tableModel.getValueAt(r, c).toString());
						}
						bufferValues.add(rowValues);
					}
				}
				copyTableValues = bufferValues;

			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_MASK), "paste");
		actionMap.put("paste", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				DefaultTableModel tableModel = (DefaultTableModel)table.getModel();
				int row = table.getSelectedRow();
				int col = table.getSelectedColumn();
				if (row >= 0 && col >= 0) {
					for(List<String> rowValues : copyTableValues){
						for(String value : rowValues ){
							tableModel.setValueAt(value,row, col);
							col = col + 1;
						}
						row = row + 1;
						col = table.getSelectedColumn();
					}
				}

			}
		});

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_MASK), "save");
		actionMap.put("save", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				table.getSelectionModel().clearSelection();
				saveFile();
				Model.getParserModel().parseCurrentFile();
			}
		});
	}

	/**
	 * Line wrap check box option in Edit Menu on a menu bar<br>
	 * If enables - wraps lines on the edge of the window
	 * @param text text contained in a main text area
	 * @param item check box item to connect with
	 */
	private void lineWrapOption(final JTextArea text, final JCheckBoxMenuItem item) {
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(item.isSelected()) {
					text.setLineWrap(true);		
					text.setWrapStyleWord(true);
				} else if(!(item.isSelected())) {
					text.setLineWrap(false);		
					text.setWrapStyleWord(false);
				}
			}
		});
	}

	/**
	 * Set extension file filters for a file chooser
	 * @param chooser JFileChooser object
	 */
	private void setFileChooserFilters(JFileChooser chooser) {
		String[] fileExtensions = {"txt", "dat", "rtf", "log", "asm"};
		FileNameExtensionFilter filter = new FileNameExtensionFilter(".rtf", fileExtensions[2]);
		FileNameExtensionFilter filter2 = new FileNameExtensionFilter(".log", fileExtensions[3]);
		FileNameExtensionFilter filter3 = new FileNameExtensionFilter(".txt", fileExtensions[0]);
		FileNameExtensionFilter filter4 = new FileNameExtensionFilter(".dat", fileExtensions[1]);
		FileNameExtensionFilter filter5 = new FileNameExtensionFilter(".asm", fileExtensions[4]);

		chooser.setFileFilter(filter);
		chooser.setFileFilter(filter2);
		chooser.setFileFilter(filter3);
		chooser.setFileFilter(filter4);
		chooser.setFileFilter(filter5);
		chooser.setFileFilter(chooser.getChoosableFileFilters()[0]);
	}

	/**
	 * Handles 'Select All' button/item menu functionality
	 */
	private void selectAllButton() {

		mainText.selectAll();
		selectedText = mainText.getSelectedText();			
	}

	/**
	 * Handles 'Paste' button/item menu functionality
	 */
	private void pasteButton() {
		Transferable transferable = clipboard.getContents(null);
		try {
			if(transferable != null && 
			transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			pasteText = (String) transferable.getTransferData(DataFlavor.stringFlavor);
			} 
		} catch (UnsupportedFlavorException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();				
		}
		if(selectedText != null) {
			mainText.insert(pasteText, mainText.getCaretPosition());
			mainText.replaceSelection("");
		} else {
			mainText.insert(pasteText, mainText.getCaretPosition());
		}
	}

	/**
	 * Handles 'Copy' button/item menu functionality
	 */
	private void copyButton() {
		selection = new StringSelection(selectedText);
		clipboard.setContents(selection, selection);		
	}

	/**
	 * Handles 'Cut' button/item menu functionality
	 */
	private void cutButton() {		
		selection = new StringSelection(selectedText);
		clipboard.setContents(selection, selection);
		mainText.cut();
	}
	
	/**
	 * Handles 'Delete' button/item menu functionality
	 */
	private void deleteButton() {
		mainText.replaceSelection("");		
	}

	private void parseButton() {
		mainText.replaceSelection("");
	}





	/**
	 * Places Search Dialog on a certain place regarding app's the main window
	 * @param button
	 */
	private void searchWindowLocation(JButton button) {
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				searchWindowLocationLogic();
			}
		});
	}





	private void saveDefaultSetting(){


	}



	/**
	 * Search window logic method which runs only one
	 * instance of SearchDialog class and makes the window focusable
	 * if it already has been opened
	 */
	private void searchWindowLocationLogic() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(searchDialog.isVisible()) {
					searchDialog.toFront();
				} else {
					searchDialog = SearchDialog.getInstance(appFrame);
					searchDialog.setLocation(
							rec.x + AsmEditor.this.getWidth() - searchDialog.getWidth() - 20,
							rec.y + searchDialog.getHeight() / 2);
					searchDialog.setVisible(true);							
				}						
			}
		});
	}
	
	/**
	 * Tool bar check box functionality(visible/hide)
	 * @param item JMenuItem which trigger the method
	 * @param toolBar JToolBar to hide
	 * @param label JLabel to hide
	 */
	private void barsCheckBox(final JMenuItem item, final JToolBar toolBar, final JLabel label) {
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(toolBar != null) {
					if(toolBar.isVisible()) {
						toolBar.setVisible(false);
					} else {
						toolBar.setVisible(true);
					}
				} else if (label != null) {
					if(label.isVisible()) {
						label.setVisible(false);
					} else {
						label.setVisible(true);
					}
				}
		}			
		});
	}
	
	/**
	 * Handles selective selection with a mouse or keyboard.
	 * @param text main text of JTextArea
	 */
	private void selectiveTextSelection(final TextPane text) {
		text.addCaretListener(new CaretListener() {
			public void caretUpdate(CaretEvent e) {
				int dot = e.getDot(); // get start location of cater
				int mark = e.getMark(); // get the 2nd end of cater
				if(dot != mark) {
					selectedText = text.getSelectedText();					
				} else {
					selectedText = null;
				}

				// determine Line number and Column position in a line 
				caretPos = text.getCaretPosition();
				try {
					lineCounter = text.getLineOfOffset(caretPos);
					columnCounter = caretPos - text.getLineStartOffset(lineCounter);
					lineCounter += 1;
					statusBar.setText("Line: " + lineCounter + ", Col: " + columnCounter);
				} catch (BadLocationException e1) {
					e1.printStackTrace();
				}
				// change filename in title bar if it needed to be saved; adds '*'
				String currentText = text.getText();
				if(!(currentText.equals(savedTextForSymbol))){
					if(currentFile != null)
						appFrame.setAppTitle("*" + currentFile.getName() + " - " + AppFrameHelper.APLICATION_NAME);
				}
				repaint();
			}
		});
	}

	/**
	 * Mouse listener for a text area, it handles Pop up menu showing
	 * @param text JTextArea which is the only area where Pop up menu shows
	 */
	private void mouseListenerInTextArea(TextPane text) {
		text.addMouseListener(new MouseAdapter() {
			public void mouseReleased(MouseEvent e) {
				if(e.getButton() == e.BUTTON3) {
					menuPop.show(e.getComponent(), e.getX(), e.getY());
				}
			}			
		});
	}


	private void inputListenerInTextArea(TextPane text) {
		text.addInputMethodListener(new InputMethodListener() {

			public void inputMethodTextChanged(InputMethodEvent inputMethodEvent) {
				textStatus.setText("Editing");
			}


			public void caretPositionChanged(InputMethodEvent inputMethodEvent) {

			}
		});



	}

	/**
	 * Listens/register main window movings for Search dialog
	 * @param frame reference of a main frame(main app's window)
	 * @return return a rectangle
	 */
	public Rectangle windowMovingRegister(JComponent frame) {
		rec = frame.getBounds();

		ComponentListener comp = new ComponentListener() {
			public void componentShown(ComponentEvent e) {}
			public void componentResized(ComponentEvent e) {}
			public void componentMoved(ComponentEvent e) {
				rec = e.getComponent().getBounds();
			}
			public void componentHidden(ComponentEvent e) {}
		};
		frame.addComponentListener(comp);
		return rec;
	}
	
	/**
	 * Action performed switch method
	 */
	public void actionPerformed(ActionEvent e) {
		String actionCommand = e.getActionCommand();
		if(actionCommand.equals("new")) {
			createNewFile();
		} else if (actionCommand.equals("open")) {
			openFile();
		} else if (actionCommand.equals("openFolder")) {
			openFolder();
		} else if (actionCommand.equals("save")) {
			textStatus.setText("Pending...");
				saveFile();
				Model.getParserModel().parseCurrentFile();
				updateColorDocument();
				this.revalidate();
			textStatus.setText("OK "+ (numOK++).toString() +"  ");
		} else if (actionCommand.equals("saveAs")) {
			saveAsFile();
		} else if (actionCommand.equals("about")) {
			aboutDialog();
		} else if (actionCommand.equals("exit")) {
			exitApplication();
		} else if (actionCommand.equals("selectAll")) {
			selectAllButton();
		} else if (actionCommand.equals("copy")) {
			copyButton();
		} else if (actionCommand.equals("paste")) {
			pasteButton();
		} else if (actionCommand.equals("cut")) {
			cutButton();
		} else if (actionCommand.equals("delete")) {
			deleteButton();
		} else if (actionCommand.equals("parse")) {
			parseButton();
		} else if (actionCommand.equals("generateComConst")) {
			//generateComConstButton();
		}
	}
	



	public TextPane  getMainText() {
		FileEditorPanel panel = tabbedPane != null ? tabbedPane.getActiveFilePanel() : null;
		return panel != null ? panel.getMainText() : mainText;
	}

	public TextPane getFocusedTextPane() {
		FileEditorPanel panel = tabbedPane != null ? tabbedPane.getActiveFilePanel() : null;
		return panel != null ? panel.getFocusedTextPane() : mainText;
	}
	public boolean isTableVisible() {
		FileEditorPanel panel = tabbedPane != null ? tabbedPane.getActiveFilePanel() : null;
		return panel != null ? panel.isTableVisible() : false;
	}

	public JTable getTable() {
		FileEditorPanel panel = tabbedPane != null ? tabbedPane.getActiveFilePanel() : null;
		return panel != null ? panel.getTable() : table;
	}
	public String getCurrentText(){
		FileEditorPanel panel = tabbedPane != null ? tabbedPane.getActiveFilePanel() : null;
		return panel != null ? panel.getCurrentText() : mainText.getText();
	}

	/**
	 * Handles selection encodings in Edit -> encodings radio button menu
	 */
	private void getSelectedEncoding() {
		int k = 0;
		for (Map.Entry<String, Charset> entry : charsets.entrySet()) {
			final Charset charset = entry.getValue();
			encodItems.add(new JRadioButtonMenuItem(charset.displayName()));
			JRadioButtonMenuItem radio = encodItems.get(k);
			radio.setHorizontalTextPosition(JMenuItem.RIGHT);
			k++;
			for (int i = 0; i < commonCharsets.length; i++) {
				if (charset.displayName().equals(commonCharsets[i])) {
					radio.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							if (currentFile != null) {
								encodingFileReader(charset, currentFile);
								currentCharset = charset;
							}
						}
					});
					group.add(radio);
					if (charset.displayName().equals("UTF-8")) {
						radio.setSelected(true);
					}
					encoding.add(radio);
				}
			}
			radio.setVisible(true);
		}
	}


		public void updateOutputCodes() {
		if (tabbedPane == null) return;
		FileEditorPanel panel = tabbedPane.getActiveFilePanel();
		if (panel != null) panel.updateOutputCodes();
	}

		public void updateCurrentFile(File newFile, File oldFile) {
		if (tabbedPane == null) return;
		tabbedPane.openFile(newFile);
	}


	class UndoHandler implements UndoableEditListener {

		public void undoableEditHappened(UndoableEditEvent e) {
			textStatus.setText("editing...");
			undo.addEdit(e.getEdit());
			undoAction.update();
			redoAction.update();
		}
	}

	class UndoAction extends AbstractAction {

		public UndoAction() {
			super("Undo");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				undo.undo();
			} catch (CannotUndoException ex) {
				System.err.println("Can'd do undo");
				ex.printStackTrace();
			}
			update();
			redoAction.update();
		}

		private void update() {
			if(undo.canUndo()) {
				setEnabled(true);
				putValue(Action.NAME, "Undo");
			}else {
				setEnabled(false);
				putValue(Action.NAME, "Undo");
			}
		}
	}

	class RedoAction extends AbstractAction {

		public RedoAction() {
			super("Redo");
			setEnabled(false);
		}

		public void actionPerformed(ActionEvent e) {
			try {
				undo.redo();
			} catch (CannotRedoException ex) {
				System.err.println("Unable to redo");
				ex.printStackTrace();
			}
			update();
			undoAction.update();
		}

		protected void update() {
			if(undo.canRedo()) {
				setEnabled(true);
				putValue(Action.NAME, "Redo");
			}else {
				setEnabled(false);
				putValue(Action.NAME, "Redo");
			}
		}
	}

	public RedoAction getRedoAction (){
		return  redoAction;
	}


	public UndoAction getUndoAction (){
		return  undoAction;
	}

	private static Color debugColor = new Color(0x7BC0E0); // Светло-голубой для подсветки вместо зеленого
	public static final Color editorBg = new Color(0xE8F4F8); // Единый светло-голубой оттенок для ВСЕХ редакторов

	final static  StyleContext cont = StyleContext.getDefaultStyleContext();

	final static AttributeSet blackAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, Color.BLACK);
	final static AttributeSet blackAttrSmall = cont.addAttribute(blackAttr,StyleConstants.FontSize, font_size);
	final static  AttributeSet blackAttrSmallHL = cont.addAttribute(blackAttrSmall,StyleConstants.Background, debugColor);

	final static AttributeSet greenAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, new Color(0,99,0,255));
	final static AttributeSet greenAttrSmall = cont.addAttribute(greenAttr, StyleConstants.FontSize, font_size);
	final static  AttributeSet greenAttrSmallHL = cont.addAttribute(greenAttrSmall,StyleConstants.Background, debugColor);
	final static AttributeSet pinkAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, new Color(202,0,170,255));
	final static AttributeSet pinkAttrBold = cont.addAttribute(pinkAttr, StyleConstants.Bold, Boolean.TRUE);
	final static AttributeSet pinkAttrBoldSmall = cont.addAttribute(pinkAttrBold,StyleConstants.FontSize, font_size);
	final static AttributeSet pinkAttrBoldSmallHL = cont.addAttribute(pinkAttrBoldSmall,StyleConstants.Background, debugColor);
	final static AttributeSet redAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, new Color(153, 22, 19, 255));
	final static AttributeSet redAttrBold = cont.addAttribute(redAttr, StyleConstants.Bold, Boolean.TRUE);
	final static AttributeSet redAttrBoldSmall = cont.addAttribute(redAttrBold, StyleConstants.FontSize, font_size);
	final static AttributeSet redAttrBoldSmallHL = cont.addAttribute(redAttrBoldSmall,StyleConstants.Background, debugColor);
	final static AttributeSet cyanAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, new Color(61,129,255, 255));
	final static AttributeSet cyanAttrBold = cont.addAttribute(cyanAttr, StyleConstants.Bold, Boolean.TRUE);
	final static AttributeSet cyanAttrBoldSmall = cont.addAttribute(cyanAttrBold,StyleConstants.FontSize, font_size);
	final static AttributeSet cyanAttrBoldSmallHL = cont.addAttribute(cyanAttrBoldSmall,StyleConstants.Background, debugColor);
	final static AttributeSet blueAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, new Color(29,10,209,255));
	final static AttributeSet blueAttrBold = cont.addAttribute(blueAttr, StyleConstants.Bold, Boolean.TRUE);
	final static AttributeSet blueAttrBoldSmall = cont.addAttribute(blueAttrBold,StyleConstants.FontSize, font_size);
	final static AttributeSet blueAttrBoldSmallHL = cont.addAttribute(blueAttrBoldSmall,StyleConstants.Background, debugColor);

	final static AttributeSet darkAttr = cont.addAttribute(cont.getEmptySet(), StyleConstants.Foreground, new Color(86, 39, 184, 231));
	final static AttributeSet darkAttrBold = cont.addAttribute(darkAttr, StyleConstants.Bold, Boolean.TRUE);
	final static AttributeSet darkAttrBoldSmall = cont.addAttribute(darkAttrBold, StyleConstants.FontSize, font_size);
	final static AttributeSet darkAttrBoldSmallHL = cont.addAttribute(darkAttrBoldSmall,StyleConstants.Background, debugColor);

	public static StyledDocument getColorDocument(String inText) throws BadLocationException {
		return  getColorDocument(inText, null);
	}

	public static StyledDocument getColorDocument(String inText, Integer numLineHighlighting) throws BadLocationException {

		StyledDocument res = new DefaultStyledDocument();


		String[] lines = null;
		int index = 0;
		try {
			Pattern patternAnyWord = Pattern.compile("[-\\w|$|#|%]+");
			Pattern patternComment = Pattern.compile("[\\s]*//.*");
			Pattern patternLineComment = Pattern.compile("^[\\s]*//.*");
			Pattern patternMetka = Pattern.compile("^[\\w]+:");
			Pattern patternDigit = Pattern.compile("^[\\d|h|d|x]+");

			Pattern patternMultiLineComment = Pattern.compile("/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/");

			List<String> multilineComments = new ArrayList<>();

			Matcher matcher = patternMultiLineComment.matcher(inText);
			while (matcher.find()) {
				String comment = matcher.group(0);
				multilineComments.add(comment);
				inText = inText.replace(comment, "multilinecommenttext ");
			}


			int multiLineCommentIndex = 0;



			lines = inText.split("\\n");
			for (String line : lines) {
				boolean isDebugLine = false;
				if(numLineHighlighting != null){
					if(index == numLineHighlighting){
						isDebugLine = true;
					}
				}
				index ++;
				Matcher matcherLineComment = patternLineComment.matcher(line);
				if (matcherLineComment.find()) {
					res.insertString(res.getLength(), line + "\n", isDebugLine ? greenAttrSmallHL :  greenAttrSmall);
					continue;
				}
				Matcher matcherAnyWord = patternAnyWord.matcher(line);
				if (!matcherAnyWord.find()) {
					res.insertString(res.getLength(), line + "\n",isDebugLine ? blackAttrSmallHL :  blackAttrSmall);
					continue;
				}
				String comment = "";
				Matcher matcherComment = patternComment.matcher(line);
				if (matcherComment.find()) {
					comment = matcherComment.group(0);
					line = line.replace(comment, "");
				}
				String metka = null;
				Matcher matcherMetka = patternMetka.matcher(line);
				if (matcherMetka.find()) {
					metka = matcherMetka.group(0);
					res.insertString(res.getLength(), metka, isDebugLine ? pinkAttrBoldSmallHL : pinkAttrBoldSmall);
					line = line.replace(metka, "");
				}
				Matcher matcherAnyWord2 = patternAnyWord.matcher(line);
				int i = 0;

				CommandGroup group = null;
				while (matcherAnyWord2.find()) {
					String arg = matcherAnyWord2.group();

					if (arg.equals("multilinecommenttext")) {
						res.insertString(res.getLength(), multilineComments.get(multiLineCommentIndex), isDebugLine ? greenAttrSmallHL :  greenAttrSmall);
						multiLineCommentIndex++;
						continue;
					}
					else if (arg.contains("#") && arg.contains("%") ) {
						res.insertString(res.getLength(),  arg, isDebugLine ? redAttrBoldSmallHL :  redAttrBoldSmall);
						continue;
					} else if (i == 0) {
						String command = arg;
						res.insertString(res.getLength(), (metka == null ? "" : " ") + command,isDebugLine ? blueAttrBoldSmallHL :  blueAttrBoldSmall );
						CpuCommand cpuCommand = CpuCommand.getCommand(command);
						if(cpuCommand != null) {
							group = cpuCommand.getGroup();
						}

					} else {
						if (arg.length() == 2 && arg.substring(0, 1).toUpperCase().equals("R")) {
							res.insertString(res.getLength(), " " + arg, isDebugLine ? cyanAttrBoldSmallHL : cyanAttrBoldSmall);
						}
						//else if (ModeCode.getName(arg) != null) {
						//	res.insertString(res.getLength(), " " + arg, darkAttrBoldSmall);
						//}
						else if (patternDigit.matcher(arg).find()) {
							res.insertString(res.getLength(), " " + arg, isDebugLine ? blackAttrSmallHL : blackAttrSmall);
						} else if (group != null && group.equals(CommandGroup.MEMORY)) {
							res.insertString(res.getLength(), " " + arg, isDebugLine ? redAttrBoldSmallHL : redAttrBoldSmall);
						} else if (group != null && group.equals(CommandGroup.BRANCH)) {
							res.insertString(res.getLength(), " " + arg, isDebugLine ? pinkAttrBoldSmallHL : pinkAttrBoldSmall);
						} else {
							res.insertString(res.getLength(), " " + arg, isDebugLine ? blackAttrSmallHL : blackAttrSmall);
						}
					}
					i++;
				}
				res.insertString(res.getLength(), comment + "\n", greenAttrSmall);
			}
		} catch (Exception e){
			e.printStackTrace();
			for(int i = index;  i < lines.length; i++){
				res.insertString(res.getLength(), lines[i] + "\n", blackAttrSmall);
			}
		} finally {


		}
		return  res;
	}
		public void updateColorDocument(){
		if (tabbedPane == null) return;
		FileEditorPanel panel = tabbedPane.getActiveFilePanel();
		if (panel != null) panel.updateColorDocument();
	}

	@Override
	public void updateColors() {
		updateColorDocument();
		this.revalidate();
	}
}



