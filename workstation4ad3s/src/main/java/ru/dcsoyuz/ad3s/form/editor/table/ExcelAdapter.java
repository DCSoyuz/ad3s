package ru.dcsoyuz.ad3s.form.editor.table;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.datatransfer.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yuri.filatov on 30.09.2016.
 */
public class ExcelAdapter implements ActionListener {
	private static final Logger logger = LoggerFactory.getLogger(ExcelAdapter.class);
    private String rowstring, value;
    private Clipboard system;
    private StringSelection stsel;
    private JTable jTable1;

    /**
     * The Excel Adapter is constructed with a JTable on which it enables
     * Copy-Paste and acts as a Clipboard listener.
     */
    public ExcelAdapter(JTable myJTable) {
        jTable1 = myJTable;
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C,
                ActionEvent.CTRL_MASK, false);
        // Identifying the copy KeyStroke user can modify this
        // to copy on some other Key combination.
        KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V,
                ActionEvent.CTRL_MASK, false);
        // Identifying the Paste KeyStroke user can modify this
        // to copy on some other Key combination.
        KeyStroke cut = KeyStroke.getKeyStroke(KeyEvent.VK_X,
                ActionEvent.CTRL_MASK, false);
        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false);

        jTable1.registerKeyboardAction(this, "Copy", copy,
                JComponent.WHEN_FOCUSED);
        jTable1.registerKeyboardAction(this, "Paste", paste,
                JComponent.WHEN_FOCUSED);
        jTable1.registerKeyboardAction(this, "Cut", cut,
                JComponent.WHEN_FOCUSED);
        jTable1.registerKeyboardAction(this, "Delete", delete,
                JComponent.WHEN_FOCUSED);
        system = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    /**
     * Public Accessor methods for the Table on which this adapter acts.
     */
    public JTable getJTable() {
        return jTable1;
    }

    public void setJTable(JTable jTable1) {
        this.jTable1 = jTable1;
    }

    /**
     * This method is activated on the Keystrokes we are listening to in this
     * implementation. Here it listens for Copy and Paste ActionCommands.
     * Selections comprising non-adjacent cells result in invalid selection and
     * then copy action cannot be performed. Paste is done by aligning the upper
     * left corner of the selection with the 1st element in the current
     * selection of the JTable.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().compareTo("Copy") == 0 | e.getActionCommand().compareTo("Cut") == 0) {
            StringBuffer sbf = new StringBuffer();
            Boolean isCut =  e.getActionCommand().compareTo("Cut") == 0;
            // Check to ensure we have selected only a contiguous block of
            // cells
            int numcols = jTable1.getSelectedColumnCount();
            int numrows = jTable1.getSelectedRowCount();
            int[] rowsselected = jTable1.getSelectedRows();
            int[] colsselected = jTable1.getSelectedColumns();
            if (!((numrows - 1 == rowsselected[rowsselected.length - 1]
                    - rowsselected[0] && numrows == rowsselected.length) && (numcols - 1 == colsselected[colsselected.length - 1]
                    - colsselected[0] && numcols == colsselected.length))) {
                JOptionPane.showMessageDialog(null, "Invalid Copy Selection",
                        "Invalid Copy Selection", JOptionPane.ERROR_MESSAGE);
                return;
            }
            for (int i = 0; i < numrows; i++) {
                for (int j = 0; j < numcols; j++) {
                    sbf.append(jTable1.getValueAt(rowsselected[i], colsselected[j]));
                    if(isCut){
                        jTable1.setValueAt("",rowsselected[i], colsselected[j]);
                    }
                    if (j < numcols - 1) {
                        sbf.append("\t");
                    }
                }
                sbf.append("\n");
            }
            stsel = new StringSelection(sbf.toString());
            system = Toolkit.getDefaultToolkit().getSystemClipboard();
            system.setContents(stsel, stsel);
        }
        if(e.getActionCommand().compareTo("Delete") == 0) {
            int row = jTable1.getSelectedRow();
            int col = jTable1.getSelectedColumn();
            row = jTable1.convertRowIndexToModel(row);
            col = jTable1.convertColumnIndexToModel(col);
            int rowCount = jTable1.getSelectedRowCount();
            int colCount = jTable1.getSelectedColumnCount();
            if (row >= 0 && col >= 0) {
                for(int r = row; r < row+rowCount;r++){
                    for(int c = col; c < col+colCount; c++ ){
                        jTable1.getModel().setValueAt("", r, c);
                    }
                }
            }
        }
        if (e.getActionCommand().compareTo("Paste") == 0) {
            int startRow = (jTable1.getSelectedRows())[0];
            int startCol = (jTable1.getSelectedColumns())[0];
            try {
                String trstring = (String) (system.getContents(this)
                        .getTransferData(DataFlavor.stringFlavor));
                String[] lines = trstring.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String[] cells = lines[i].split("\t", -1);
                    for (int j = 0; j < cells.length; j++) {
                        value = cells[j];
                        if (startRow + i < jTable1.getRowCount()
                                && startCol + j < jTable1.getColumnCount())
                            jTable1.setValueAt(value, startRow + i, startCol
                                    + j);
                    }
                }
            } catch (Exception ex) {
                logger.error("Error in actionPerformed", ex);
            }
        }
    }
}