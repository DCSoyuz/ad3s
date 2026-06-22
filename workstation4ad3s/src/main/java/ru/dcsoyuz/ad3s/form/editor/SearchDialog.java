package ru.dcsoyuz.ad3s.form.editor;

import ru.dcsoyuz.ad3s.form.AppFrame;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Simple GUI for search, replace, replace all text and its functionality.
 * Supports both text panes and JTable (for txt tabular files).
 * Uses singleton pattern.
 *
 * @author Mansur Y.
 */
public class SearchDialog extends JDialog implements FocusListener {
	private static final long serialVersionUID = -3446868398435205807L;
	private static SearchDialog instance = null;
	private JPanel mainPanel;
	private JLabel searchLabel;
	private JLabel replaceLabel;
	private JTextField searchTextField;
	private JTextField replaceTextField;
	private JButton replaceBut;
	private JButton findBut;
	private JButton replaceAllBut;
	private String searchedText;
	private String mainText;
	ArrayList<Integer> caretPositions;
	private JTextPane area;
	private boolean findButtonFlag = true;
	private int arrayLenCounter = 1;
	private String oldSearchedText;
	private int replaceCounter = 0;

	// Table search state
	private boolean isTableMode = false;
	private ArrayList<Point> tableCells = new ArrayList<Point>();
	private int tableCellIndex = 0;

	private SearchDialog(JFrame frame) {
		super(frame, false);
		instance = this;
		initGUI();
	}

	private void initGUI() {
		mainPanel = new JPanel();
		mainPanel.setSize(240, 170);
		Container contentPane = this.getContentPane();
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints con = new GridBagConstraints();
		layout.setConstraints(mainPanel, con);
		mainPanel.setLayout(layout);

		con.insets = new Insets(4, 4, 4, 4);
		con.anchor = GridBagConstraints.CENTER;
		searchLabel = new JLabel("Search for:");
		searchLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
		con.anchor = GridBagConstraints.WEST;
		con.gridx = 0;
		con.gridy = 0;
		layout.setConstraints(searchLabel, con);

		Action enterAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (findButtonFlag) {
					findTextInTextArea();
				} else {
					findNextInText();
				}
			}
		};
		searchTextField = new JTextField(8);
		searchTextField.setFocusable(true);
		searchTextField.setFont(new Font("Serif", Font.PLAIN, 14));
		searchTextField.setBounds(3, 3, 3, 3);
		searchTextField.setPreferredSize(new Dimension(100, 25));
		searchTextField.setMinimumSize(new Dimension(120, 25));
		searchTextField.setMaximumSize(new Dimension(120, 25));
		searchTextField.setDocument(new JTextFieldLimited(200));
		searchTextField.setAction(enterAction);
		searchTextField.addFocusListener(this);
		con.anchor = GridBagConstraints.CENTER;
		con.gridx = 1;
		con.gridy = 0;
		layout.setConstraints(searchTextField, con);

		con.anchor = GridBagConstraints.WEST;
		replaceLabel = new JLabel("Replace with:");
		replaceLabel.setFont(new Font("Dialog", Font.PLAIN, 14));
		con.gridx = 0;
		con.gridy = 1;
		layout.setConstraints(replaceLabel, con);

		Action replaceAction = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				replaceTextInTextArea();
			}
		};
		replaceTextField = new JTextField(8);
		replaceTextField.setFocusable(true);
		replaceTextField.setFont(new Font("Serif", Font.PLAIN, 14));
		replaceTextField.setBounds(3, 3, 3, 3);
		replaceTextField.setPreferredSize(new Dimension(120, 25));
		replaceTextField.setMinimumSize(new Dimension(120, 25));
		replaceTextField.setMaximumSize(new Dimension(120, 25));
		replaceTextField.setDocument(new JTextFieldLimited(200));
		replaceTextField.setAction(replaceAction);
		replaceTextField.addFocusListener(this);
		con.anchor = GridBagConstraints.CENTER;
		con.gridx = 1;
		con.gridy = 1;
		layout.setConstraints(replaceTextField, con);

		findBut = new JButton(enterAction);
		findBut.setText("Find");
		findBut.setFocusable(false);
		findBut.setPreferredSize(new Dimension(98, 25));
		findBut.setMinimumSize(new Dimension(98, 25));
		findBut.setMaximumSize(new Dimension(98, 25));
		findBut.setMnemonic(KeyEvent.VK_ENTER);
		con.insets = new Insets(25, 4, 4, 4);
		con.gridx = 0;
		con.gridy = 2;
		layout.setConstraints(findBut, con);

		replaceBut = new JButton(replaceAction);
		replaceBut.setText("Replace");
		replaceBut.setFocusable(false);
		replaceBut.setPreferredSize(new Dimension(98, 25));
		replaceBut.setMinimumSize(new Dimension(98, 25));
		replaceBut.setMaximumSize(new Dimension(98, 25));
		replaceBut.setMnemonic(KeyEvent.VK_ENTER);
		con.insets = new Insets(25, 4, 4, 4);
		con.gridx = 1;
		con.gridy = 2;
		layout.setConstraints(replaceBut, con);

		replaceAllBut = new JButton("Replace All");
		replaceAllBut.setFocusable(false);
		replaceAllBut.setMargin(new Insets(1, 1, 1, 1));
		replaceAllBut.setPreferredSize(new Dimension(98, 25));
		replaceAllBut.setMinimumSize(new Dimension(98, 25));
		replaceAllBut.setMaximumSize(new Dimension(98, 25));
		replaceAllBut.addActionListener(new ReplaceAllAction());
		con.insets = new Insets(4, 4, 4, 4);
		con.gridx = 1;
		con.gridy = 3;

		mainPanel.add(searchLabel);
		mainPanel.add(searchTextField);
		mainPanel.add(findBut);
		mainPanel.add(replaceBut);
		mainPanel.add(replaceLabel);
		mainPanel.add(replaceTextField);
		contentPane.add(mainPanel, BorderLayout.CENTER);
		setTitle("Search and Replace");
		setResizable(false);
		setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
		setSize(260, 180);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(WindowEvent e) {
				findButtonFlag = true;
				arrayLenCounter = 1;
			}
		});
	}

	public static SearchDialog getInstance(JFrame frame) {
		if (instance == null) {
			synchronized (SearchDialog.class) {
				if (instance == null) {
					instance = new SearchDialog(frame);
				}
			}
		}
		return instance;
	}

	private void findTextInTextArea() {
		replaceCounter = 0;
		findButtonFlag = false;

		AsmEditor editor = AppFrame.getInstance().getAsmEditor();
		isTableMode = editor.isTableVisible();

		if (isTableMode) {
			findInTable();
			return;
		}

		caretPositions = new ArrayList<Integer>();
		searchedText = searchTextField.getText();
		int lenSearchedText = searchedText.length();
		oldSearchedText = searchedText;
		area = editor.getFocusedTextPane();
		area.getCaret().setSelectionVisible(true);
		Highlighter highlighter = area.getHighlighter();
		Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(164, 255, 183, 255));
		highlighter.removeAllHighlights();
		if (!(searchedText.equals(""))) {
			try {
				mainText = area.getText();
				mainText = mainText.replaceAll("\r\n", "\n");
				int index = mainText.indexOf(searchedText);
				while (index >= 0) {
					int next = index + lenSearchedText;
					highlighter.addHighlight(index, next, painter);
					caretPositions.add(index);
					index = mainText.indexOf(searchedText, index + lenSearchedText);
				}
			} catch (BadLocationException e) {
				e.getMessage();
			}
			if (caretPositions.size() > 0) {
				int caretPos0 = caretPositions.get(0);
				area.setCaretPosition(caretPos0);
				area.setSelectionStart(caretPos0);
				area.setSelectionEnd(caretPos0 + lenSearchedText);
			}
		}
	}

	private void findInTable() {
		searchedText = searchTextField.getText();
		oldSearchedText = searchedText;
		tableCells = new ArrayList<Point>();
		tableCellIndex = 0;

		if (searchedText.isEmpty()) return;

		JTable table = AppFrame.getInstance().getAsmEditor().getTable();
		DefaultTableModel model = (DefaultTableModel) table.getModel();
		String searchLower = searchedText.toLowerCase();

		for (int row = 0; row < model.getRowCount(); row++) {
			for (int col = 0; col < model.getColumnCount(); col++) {
				Object val = model.getValueAt(row, col);
				if (val != null && val.toString().toLowerCase().contains(searchLower)) {
					tableCells.add(new Point(row, col));
				}
			}
		}

		if (!tableCells.isEmpty()) {
			selectTableCell(0);
		}
	}

	private void selectTableCell(int index) {
		JTable table = AppFrame.getInstance().getAsmEditor().getTable();
		Point cell = tableCells.get(index);
		table.setRowSelectionInterval(cell.x, cell.x);
		table.setColumnSelectionInterval(cell.y, cell.y);
		table.scrollRectToVisible(table.getCellRect(cell.x, cell.y, true));
	}

	private void replaceTextInTextArea() {
		AsmEditor editor = AppFrame.getInstance().getAsmEditor();

		if (editor.isTableVisible()) {
			replaceInTable();
			return;
		}

		replaceCounter++;
		if (replaceCounter >= 1 && replaceCounter <= caretPositions.size()
				&& caretPositions.size() > 0) {
			area.getCaret().setSelectionVisible(true);
			area.replaceSelection(replaceTextField.getText());
			int delta = replaceTextField.getText().length()
					- searchedText.length();
			for (int i = 0; i < caretPositions.size(); i++) {
				caretPositions.set(i, caretPositions.get(i) + delta);
			}
			findNextInText();
		}
	}

	private void replaceInTable() {
		if (tableCellIndex >= tableCells.size()) return;
		JTable table = AppFrame.getInstance().getAsmEditor().getTable();
		Point cell = tableCells.get(tableCellIndex);
		Object val = table.getModel().getValueAt(cell.x, cell.y);
		String cellText = val != null ? val.toString() : "";
		String newText = cellText.replace(searchedText, replaceTextField.getText());
		table.getModel().setValueAt(newText, cell.x, cell.y);
		// Refresh search after replace
		findInTable();
		// Jump to next match after the one we just replaced
		if (tableCellIndex < tableCells.size()) {
			selectTableCell(tableCellIndex);
		}
	}

	private void findNextInText() {
		isTableMode = AppFrame.getInstance().getAsmEditor().isTableVisible();

		if (isTableMode) {
			findNextInTable();
			return;
		}

		String textFromSearchTextField = searchTextField.getText();
		if (oldSearchedText.equals(textFromSearchTextField)
				&& caretPositions.size() > 0
				&& !(textFromSearchTextField.equals(""))) {
			if (arrayLenCounter < caretPositions.size()) {
				int caretPos = caretPositions.get(arrayLenCounter);
				if (caretPos >= 0) {
					area.setCaretPosition(caretPos);
					area.setSelectionStart(caretPos);
					area.setSelectionEnd(caretPos + searchedText.length());
					arrayLenCounter++;
				}
			} else {
				arrayLenCounter = 1;
				findTextInTextArea();
			}
		} else {
			findTextInTextArea();
		}
	}

	private void findNextInTable() {
		String textFromSearchTextField = searchTextField.getText();
		if (!oldSearchedText.equals(textFromSearchTextField) || tableCells.isEmpty()) {
			findInTable();
			return;
		}
		tableCellIndex++;
		if (tableCellIndex >= tableCells.size()) {
			tableCellIndex = 0;
		}
		selectTableCell(tableCellIndex);
	}

	class JTextFieldLimited extends PlainDocument {
		private int limit;

		public JTextFieldLimited(int limit) {
			super();
			this.limit = limit;
		}

		public void insertString(int offs, String str, AttributeSet a)
				throws BadLocationException {
			if (str == null)
				return;
			if ((getLength() + str.length()) <= limit) {
				super.insertString(offs, str, a);
			}
		}
	}

	class ReplaceAllAction implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			AsmEditor editor = AppFrame.getInstance().getAsmEditor();
			if (editor.isTableVisible()) {
				replaceAllInTable();
				return;
			}
			replaceCounter = 1;
			int lenCaretPositions = caretPositions.size();
			if (caretPositions != null) {
				if (replaceCounter >= 1 && replaceCounter <= lenCaretPositions
						&& lenCaretPositions > 0) {
					area.getCaret().setSelectionVisible(true);
					area.setText(replaceAllLogic(mainText,
							searchedText, replaceTextField.getText()));
					repaint();
				}
			}
		}

		private void replaceAllInTable() {
			JTable table = AppFrame.getInstance().getAsmEditor().getTable();
			DefaultTableModel model = (DefaultTableModel) table.getModel();
			for (int row = 0; row < model.getRowCount(); row++) {
				for (int col = 0; col < model.getColumnCount(); col++) {
					Object val = model.getValueAt(row, col);
					if (val != null) {
						String cellText = val.toString();
						if (cellText.toLowerCase().contains(searchedText.toLowerCase())) {
							String newText = cellText.replace(searchedText, replaceTextField.getText());
							model.setValueAt(newText, row, col);
						}
					}
				}
			}
			findInTable();
		}

		private String replaceAllLogic(String text, String toFind, String toReplace) {
			StringBuilder textToReturn = new StringBuilder();
			StringTokenizer strTok = new StringTokenizer(text, " ");
			ArrayList<String> words = new ArrayList<String>(strTok.countTokens());
			int i = 0;
			while (strTok.hasMoreTokens()) {
				String nextToken = strTok.nextToken();
				words.add(i, nextToken);
				int index = nextToken.indexOf(toFind);
				while (index != -1) {
					StringBuilder sb = new StringBuilder(nextToken);
					sb.replace(index, toFind.length() + index, toReplace);
					words.add(i, sb.toString());
					index = nextToken.indexOf(toFind, index + 1);
				}
				if (i == words.size() - 1) {
					textToReturn.append(words.get(i));
				} else {
					textToReturn.append(words.get(i));
				}
				i++;
			}
			return textToReturn.toString();
		}
	}

	public void focusGained(FocusEvent e) {
		searchTextField.select(0, searchTextField.getText().length());
		replaceTextField.select(0, replaceTextField.getText().length());
	}

	public void focusLost(FocusEvent e) {
	}
}
