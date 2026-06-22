package ru.dcsoyuz.ad3s.form.editor;

import ru.dcsoyuz.ad3s.form.AppFrameHelper;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.*;
import ru.dcsoyuz.ad3s.model.uart.ic.McuCommand;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketIcHelper;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketToIc;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import ru.dcsoyuz.ad3s.form.editor.table.ExcelAdapter;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BatchWriterPanel extends JPanel {

    private static final int COL_RADIO = 0;
    private static final int COL_ADDR = 1;
    private static final int COL_VALUE = 2;
    private static final int COL_DECODED = 3;
    private static final int COL_REGNAME = 4;

    private static final Color STEP_BG = Color.WHITE;
    private static File getSaveFile() {
        try {
            String jarPath = BatchWriterPanel.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) {
                return new File(jarFile.getParentFile(), "batchwriter.csv");
            }
            return new File(jarFile, "batchwriter.csv");
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"), "batchwriter.csv");
        }
    }

    private final BatchTableModel tableModel;
    private final JTable table;
    private int currentStepRow = 0;

    public BatchWriterPanel() {
        setLayout(new BorderLayout(4, 4));

        tableModel = new BatchTableModel();
        if (!loadFromFile()) {
            for (int i = 0; i < 20; i++) {
                tableModel.addRow("", "");
            }
        }

        table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row) && row == currentStepRow) {
                    c.setBackground(STEP_BG);
                }
                return c;
            }
        };
        table.setShowGrid(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setRowHeight(20);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        table.getColumnModel().getColumn(COL_RADIO).setPreferredWidth(25);
        table.getColumnModel().getColumn(COL_RADIO).setMinWidth(25);
        table.getColumnModel().getColumn(COL_RADIO).setMaxWidth(25);
        table.getColumnModel().getColumn(COL_RADIO).setCellRenderer(new RadioButtonRenderer());
        table.getColumnModel().getColumn(COL_RADIO).setCellEditor(new RadioButtonEditor());

        table.getColumnModel().getColumn(COL_ADDR).setPreferredWidth(60);
        table.getColumnModel().getColumn(COL_ADDR).setMinWidth(60);
        table.getColumnModel().getColumn(COL_ADDR).setMaxWidth(60);

        table.getColumnModel().getColumn(COL_VALUE).setPreferredWidth(60);
        table.getColumnModel().getColumn(COL_VALUE).setMinWidth(60);
        table.getColumnModel().getColumn(COL_VALUE).setMaxWidth(60);

        new ExcelAdapter(table);

        tableModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int col = e.getColumn();
                if (col == COL_RADIO) {
                    int row = e.getFirstRow();
                    boolean selected = (Boolean) tableModel.getValueAt(row, COL_RADIO);
                    if (selected) {
                        currentStepRow = row;
                        for (int i = 0; i < tableModel.getRowCount(); i++) {
                            if (i != row) {
                                tableModel.setValueAtQuiet(false, i, COL_RADIO);
                            }
                        }
                    }
                    table.repaint();
                } else if (col == COL_ADDR || col == COL_VALUE) {
                    decodeRow(e.getFirstRow());
                    saveToFile();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);

        if (tableModel.getRowCount() > 0) {
            tableModel.setValueAt(true, 0, COL_RADIO);
            currentStepRow = 0;
        }

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

        JButton btnWriteStep = AppFrameHelper.createButton(null, "Write Step", "Write selected row and advance");
        btnWriteStep.addActionListener(e -> writeStep());
        buttonPanel.add(btnWriteStep);

        JButton btnWriteAll = AppFrameHelper.createButton(null, "Write All", "Write all rows sequentially");
        btnWriteAll.addActionListener(e -> writeAll());
        buttonPanel.add(btnWriteAll);

        JButton btnGoUp = AppFrameHelper.createButton(null, "Go Up", "Move cursor to first row");
        btnGoUp.addActionListener(e -> {
            currentStepRow = 0;
            selectRadio(0);
            highlightStepRow();
        });
        buttonPanel.add(btnGoUp);

        JButton btnAddRow = AppFrameHelper.createButton(null, "+Row", "Insert empty row before selected");
        btnAddRow.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            int insertAt = (selectedRow >= 0) ? selectedRow : currentStepRow;
            tableModel.insertRow(insertAt, "", "");
            selectRadio(insertAt);
            table.setRowSelectionInterval(insertAt, insertAt);
            table.scrollRectToVisible(table.getCellRect(insertAt, 0, true));
            saveToFile();
        });
        buttonPanel.add(btnAddRow);

        JButton btnRemoveRow = AppFrameHelper.createButton(null, "-Row", "Remove selected row");
        btnRemoveRow.addActionListener(e -> {
            if (tableModel.getRowCount() == 0) return;
            int removedRow = table.getSelectedRow();
            if (removedRow < 0) return;
            boolean wasRadioRow = (removedRow == currentStepRow);
            tableModel.removeRow(removedRow);
            int count = tableModel.getRowCount();
            if (count == 0) {
                currentStepRow = 0;
            } else {
                int nextRow = Math.min(removedRow, count - 1);
                table.setRowSelectionInterval(nextRow, nextRow);
                if (wasRadioRow) {
                    currentStepRow = nextRow;
                    selectRadio(currentStepRow);
                } else if (removedRow < currentStepRow) {
                    currentStepRow--;
                }
            }
            table.scrollRectToVisible(table.getCellRect(
                    count > 0 ? Math.min(removedRow, count - 1) : 0, 0, true));
            saveToFile();
        });
        buttonPanel.add(btnRemoveRow);

        JButton btnFilter = AppFrameHelper.createButton(null, "Filter", "Remove rows with default register values");
        btnFilter.addActionListener(e -> {
            List<Integer> toRemove = new ArrayList<>();
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String valStr = (String) tableModel.getValueAt(i, COL_VALUE);
                if (valStr == null || valStr.trim().isEmpty()) continue;
                try {
                    int value = parseNumberAllowHex(valStr);
                    AllRegAddr regAddr = null;
                    // Try by reg name from hidden column
                    String regName = (String) tableModel.getValueAt(i, COL_REGNAME);
                    if (regName != null && !regName.isEmpty()) {
                        try {
                            regAddr = AllRegAddr.valueOf(regName);
                        } catch (IllegalArgumentException ignored) {}
                    }
                    // Fallback: try by absolute address
                    if (regAddr == null) {
                        String addrStr = (String) tableModel.getValueAt(i, COL_ADDR);
                        if (addrStr != null && !addrStr.trim().isEmpty()) {
                            try {
                                regAddr = AllRegAddr.getAllRegAddr(parseNumber(addrStr));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    if (regAddr == null) continue;
                    Regs reg = (Regs) regAddr.getReg();
                    if (reg.isOnlyRead()) {
                        toRemove.add(i);
                        continue;
                    }
                    Integer defVal = reg.getDefaultValue();
                    if (defVal != null && value == defVal) {
                        toRemove.add(i);
                    }
                } catch (NumberFormatException ignored) {}
            }
            for (int i = toRemove.size() - 1; i >= 0; i--) {
                int row = toRemove.get(i);
                tableModel.removeRow(row);
                if (row < currentStepRow) currentStepRow--;
            }
            if (currentStepRow >= tableModel.getRowCount()) currentStepRow = Math.max(0, tableModel.getRowCount() - 1);
            selectRadio(currentStepRow);
            highlightStepRow();
            saveToFile();
        });
        buttonPanel.add(btnFilter);

        JButton btnClean = AppFrameHelper.createButton(null, "Clean", "Clear all data");
        btnClean.addActionListener(e -> {
            tableModel.clearAll();
            currentStepRow = 0;
            saveToFile();
        });
        buttonPanel.add(btnClean);

        JButton btnSaveFile = AppFrameHelper.createButton(null, "Save to file", "Save table to tab-separated file");
        btnSaveFile.addActionListener(e -> saveToFileDialog());
        buttonPanel.add(btnSaveFile);

        JButton btnLoadFile = AppFrameHelper.createButton(null, "Load from file", "Load table from tab-separated file");
        btnLoadFile.addActionListener(e -> loadFromFileDialog());
        buttonPanel.add(btnLoadFile);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void selectRadio(int row) {
        if (row >= 0 && row < tableModel.getRowCount()) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                tableModel.setValueAtQuiet(i == row, i, COL_RADIO);
            }
            table.repaint();
        }
    }

    private void highlightStepRow() {
        if (currentStepRow >= 0 && currentStepRow < tableModel.getRowCount()) {
            table.setRowSelectionInterval(currentStepRow, currentStepRow);
            table.scrollRectToVisible(table.getCellRect(currentStepRow, 0, true));
        }
        table.repaint();
    }

    private void decodeRow(int row) {
        String addrStr = (String) tableModel.getValueAt(row, COL_ADDR);
        String valStr = (String) tableModel.getValueAt(row, COL_VALUE);
        String decoded = decodeRegister(addrStr, valStr);
        tableModel.setValueAt(decoded, row, COL_DECODED);
    }

    static String decodeRegister(String addrStr, String valStr) {
        int address, value;
        try {
            address = parseNumber(addrStr);
            value = parseNumber(valStr);
        } catch (NumberFormatException e) {
            return "";
        }

        AllRegAddr regAddr = AllRegAddr.getAllRegAddr(address);
        if (regAddr == null) {
            return "unknown addr " + address;
        }

        Regs reg = (Regs) regAddr.getReg();

        if (reg.getValueType() == RegValueType.VALUE_FIELDS) {
            List<IRegField> fields = reg.getFields();
            List<String> nonzero = new ArrayList<>();
            List<String> zero = new ArrayList<>();
            for (IRegField field : fields) {
                int fv = field.getFieldValueFromRegValue(value);
                String s = field.getDisplayName() + "=" + fv;
                if (fv != 0) nonzero.add(s);
                else zero.add(s);
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<html>").append(regAddr.getDisplayName()).append(": ");
            if (!nonzero.isEmpty()) {
                sb.append("<font color='#008800'>");
                sb.append(String.join(", ", nonzero));
                sb.append("</font>");
                if (!zero.isEmpty()) sb.append(" | ");
            }
            if (!zero.isEmpty()) {
                sb.append("<font color='#4444CC'>");
                sb.append(String.join(", ", zero));
                sb.append("</font>");
            }
            sb.append("</html>");
            return sb.toString();
        } else {
            RegValueType vt = reg.getValueType();
            StringBuilder sb = new StringBuilder();
            sb.append("<html>").append(regAddr.getDisplayName()).append(": ");
            if (vt.isSigned()) {
                int signed = (value << (16 - vt.getMsb() - 1)) >> (16 - vt.getMsb() - 1);
                sb.append("value=").append(signed);
            } else {
                sb.append("value=").append(value);
            }
            sb.append("</html>");
            return sb.toString();
        }
    }

    public static String decodeRegisterByName(String regName, String valStr) {
        int value;
        try {
            value = parseNumber(valStr);
        } catch (NumberFormatException e) {
            return "";
        }
        try {
            AllRegAddr regAddr = AllRegAddr.valueOf(regName);
            Regs reg = (Regs) regAddr.getReg();
            if (reg.getValueType() == RegValueType.VALUE_FIELDS) {
                List<IRegField> fields = reg.getFields();
                List<String> nonzero = new ArrayList<>();
                List<String> zero = new ArrayList<>();
                for (IRegField field : fields) {
                    int fv = field.getFieldValueFromRegValue(value);
                    String s = field.getDisplayName() + "=" + fv;
                    if (fv != 0) nonzero.add(s);
                    else zero.add(s);
                }
                StringBuilder sb = new StringBuilder();
                sb.append("<html>").append(regAddr.getDisplayName()).append(": ");
                if (!nonzero.isEmpty()) {
                    sb.append("<font color='#008800'>");
                    sb.append(String.join(", ", nonzero));
                    sb.append("</font>");
                    if (!zero.isEmpty()) sb.append(" | ");
                }
                if (!zero.isEmpty()) {
                    sb.append("<font color='#4444CC'>");
                    sb.append(String.join(", ", zero));
                    sb.append("</font>");
                }
                sb.append("</html>");
                return sb.toString();
            } else {
                RegValueType vt = reg.getValueType();
                StringBuilder sb = new StringBuilder();
                sb.append("<html>").append(regAddr.getDisplayName()).append(": ");
                if (vt.isSigned()) {
                    int signed = (value << (16 - vt.getMsb() - 1)) >> (16 - vt.getMsb() - 1);
                    sb.append("value=").append(signed);
                } else {
                    sb.append("value=").append(value);
                }
                sb.append("</html>");
                return sb.toString();
            }
        } catch (IllegalArgumentException e) {
            return regName + ": " + valStr;
        }
    }

    static String decodeRegisterPlain(String addrStr, String valStr) {
        int address, value;
        try {
            address = parseNumber(addrStr);
            value = parseNumber(valStr);
        } catch (NumberFormatException e) {
            return "";
        }

        AllRegAddr regAddr = AllRegAddr.getAllRegAddr(address);
        if (regAddr == null) {
            return "unknown addr " + address;
        }

        Regs reg = (Regs) regAddr.getReg();

        if (reg.getValueType() == RegValueType.VALUE_FIELDS) {
            List<IRegField> fields = reg.getFields();
            List<String> nonzero = new ArrayList<>();
            List<String> zero = new ArrayList<>();
            for (IRegField field : fields) {
                int fv = field.getFieldValueFromRegValue(value);
                String s = field.getDisplayName() + "=" + fv;
                if (fv != 0) nonzero.add(s);
                else zero.add(s);
            }
            StringBuilder sb = new StringBuilder();
            sb.append(regAddr.getDisplayName()).append(": ");
            if (!nonzero.isEmpty()) {
                sb.append(String.join(", ", nonzero));
                if (!zero.isEmpty()) sb.append(" | ");
            }
            if (!zero.isEmpty()) {
                sb.append(String.join(", ", zero));
            }
            return sb.toString();
        } else {
            RegValueType vt = reg.getValueType();
            if (vt.isSigned()) {
                int signed = (value << (16 - vt.getMsb() - 1)) >> (16 - vt.getMsb() - 1);
                return regAddr.getDisplayName() + ": value=" + signed;
            } else {
                return regAddr.getDisplayName() + ": value=" + value;
            }
        }
    }

    private static int parseNumber(String s) throws NumberFormatException {
        if (s == null || s.trim().isEmpty()) throw new NumberFormatException("empty");
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseInt(s.substring(2), 16);
        }
        return Integer.parseInt(s);
    }

    private static int parseNumberAllowHex(String s) throws NumberFormatException {
        if (s == null || s.trim().isEmpty()) throw new NumberFormatException("empty");
        s = s.trim();
        if (s.startsWith("0x") || s.startsWith("0X")) {
            return Integer.parseInt(s.substring(2), 16);
        }
        // base_ram.txt stores values as hex without 0x prefix (e.g. "0400", "dec0")
        // If all chars are hex digits, parse as hex
        if (s.matches("[0-9a-fA-F]+")) {
            return Integer.parseInt(s, 16);
        }
        return Integer.parseInt(s);
    }

    private void saveToFile() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(getSaveFile()))) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String addr = (String) tableModel.getValueAt(i, COL_ADDR);
                String val = (String) tableModel.getValueAt(i, COL_VALUE);
                if (addr == null) addr = "";
                if (val == null) val = "";
                pw.println(addr + ";" + val);
            }
        } catch (IOException ignored) {
        }
    }

    private boolean loadFromFile() {
        File file = getSaveFile();
        if (!file.exists()) return false;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";", 2);
                String addr = parts.length > 0 ? parts[0] : "";
                String val = parts.length > 1 ? parts[1] : "";
                tableModel.addRow(addr, val);
            }
            return tableModel.getRowCount() > 0;
        } catch (IOException e) {
            return false;
        }
    }

    private void saveToFileDialog() {
        FileDialog dialog = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Save Batch Data", FileDialog.SAVE);
        dialog.setFile("batch_data.txt");
        dialog.setVisible(true);
        String dir = dialog.getDirectory();
        String name = dialog.getFile();
        if (dir == null || name == null) return;
        if (!name.endsWith(".txt")) name += ".txt";
        File file = new File(dir, name);
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String addr = (String) tableModel.getValueAt(i, COL_ADDR);
                String val = (String) tableModel.getValueAt(i, COL_VALUE);
                if (addr == null) addr = "";
                if (val == null) val = "";
                String decoded = decodeRegisterPlain(addr, val);
                pw.println(addr + "\t" + val + "\t" + decoded);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Save error: " + ex.getMessage());
        }
    }

    private void loadFromFileDialog() {
        FileDialog dialog = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Load Batch Data", FileDialog.LOAD);
        dialog.setFile("*.txt");
        dialog.setVisible(true);
        String dir = dialog.getDirectory();
        String name = dialog.getFile();
        if (dir == null || name == null) return;
        File file = new File(dir, name);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            tableModel.clearAllSilent();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\t", 4);
                String addr = parts.length > 0 ? parts[0].trim() : "";
                String val = parts.length > 1 ? parts[1].trim() : "";
                String regName = parts.length > 2 ? parts[2].trim() : "";
                if (!regName.isEmpty()) {
                    // base_ram.txt format: index, value, regName — use regName for decoding
                    String decoded = decodeRegisterByName(regName, val);
                    tableModel.addRowRaw(addr, val, decoded, regName);
                } else {
                    tableModel.addRow(addr, val);
                }
            }
            currentStepRow = 0;
            selectRadio(0);
            saveToFile();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Load error: " + ex.getMessage());
        }
    }

    private void writeStep() {
        int row = currentStepRow;
        if (row < 0 || row >= tableModel.getRowCount()) return;

        String addrStr = (String) tableModel.getValueAt(row, COL_ADDR);
        String valStr = (String) tableModel.getValueAt(row, COL_VALUE);

        if (addrStr == null || addrStr.trim().isEmpty()) {
            // Empty address — skip to next row without writing
            int nextRow = row + 1;
            if (nextRow < tableModel.getRowCount()) {
                currentStepRow = nextRow;
                selectRadio(currentStepRow);
                highlightStepRow();
            }
            return;
        }

        int address, value;
        try {
            address = parseNumber(addrStr);
            value = parseNumber(valStr);
        } catch (NumberFormatException e) {
            return;
        }

        writeSingleWord(address, value, () -> {
            currentStepRow = row + 1;
            selectRadio(currentStepRow);
            SwingUtilities.invokeLater(this::highlightStepRow);
        });
    }

    private void writeAll() {
        List<int[]> writes = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String addrStr = (String) tableModel.getValueAt(i, COL_ADDR);
            String valStr = (String) tableModel.getValueAt(i, COL_VALUE);
            try {
                int address = parseNumber(addrStr);
                int value = parseNumber(valStr);
                writes.add(new int[]{address, value});
            } catch (NumberFormatException ignored) {
            }
        }
        if (writes.isEmpty()) return;

        currentStepRow = 0;
        selectRadio(0);
        writeSequential(writes, 0);
    }

    private void writeSequential(List<int[]> writes, int index) {
        if (index >= writes.size()) {
            currentStepRow = writes.size() - 1;
            if (currentStepRow >= 0) selectRadio(currentStepRow);
            SwingUtilities.invokeLater(this::highlightStepRow);
            return;
        }
        int address = writes.get(index)[0];
        int value = writes.get(index)[1];
        currentStepRow = index;
        SwingUtilities.invokeLater(this::highlightStepRow);

        writeSingleWord(address, value, () -> writeSequential(writes, index + 1));
    }

    private void writeSingleWord(int address, int value, Runnable onComplete) {
        new Thread(() -> {
            try {
                PacketToIc packet = new PacketToIc(McuCommand.WRITE, address, 1, Collections.singletonList(value));
                byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
                System.out.println("BatchWriter: wrote addr=" + address + " value=0x" + String.format("%04X", value));

                // Verify written data
                Model.getMemoryModel().verifySingleWord(address, value);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                Model.getUartModel().resetPermits();
                if (onComplete != null) onComplete.run();
            }
        }).start();
    }

    private static class BatchTableModel extends AbstractTableModel {
        private final String[] columnNames = {"", "Address", "Value", "Decoded"};
        private final List<Object[]> data = new ArrayList<>();

        void addRow(String addr, String value) {
            String decoded = decodeRegisterStatic(addr, value);
            data.add(new Object[]{false, addr, value, decoded, ""});
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        void insertRow(int index, String addr, String value) {
            String decoded = decodeRegisterStatic(addr, value);
            data.add(index, new Object[]{false, addr, value, decoded, ""});
            fireTableRowsInserted(index, index);
        }

        void addRowRaw(String addr, String value, String decoded) {
            data.add(new Object[]{false, addr, value, decoded, ""});
        }

        void addRowRaw(String addr, String value, String decoded, String regName) {
            data.add(new Object[]{false, addr, value, decoded, regName});
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        void removeRow(int row) {
            if (row >= 0 && row < data.size()) {
                data.remove(row);
                fireTableRowsDeleted(row, row);
            }
        }

        void clearAll() {
            int size = data.size();
            data.clear();
            if (size > 0) fireTableRowsDeleted(0, size - 1);
            for (int i = 0; i < 20; i++) {
                addRow("", "");
            }
        }

        void clearAllSilent() {
            int size = data.size();
            data.clear();
            if (size > 0) fireTableRowsDeleted(0, size - 1);
        }

        @Override public int getRowCount() { return data.size(); }
        @Override public int getColumnCount() { return columnNames.length; }
        @Override public String getColumnName(int column) { return columnNames[column]; }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == COL_RADIO) return Boolean.class;
            return String.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return data.get(rowIndex)[columnIndex];
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            data.get(rowIndex)[columnIndex] = aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        void setValueAtQuiet(Object aValue, int rowIndex, int columnIndex) {
            data.get(rowIndex)[columnIndex] = aValue;
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex != COL_DECODED;
        }

        private static String decodeRegisterStatic(String addrStr, String valStr) {
            return decodeRegister(addrStr, valStr);
        }
    }

    private class RadioButtonRenderer implements TableCellRenderer {
        private final JRadioButton rb = new JRadioButton();

        RadioButtonRenderer() {
            rb.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable tbl, Object value, boolean isSelected,
                                                        boolean hasFocus, int row, int column) {
            rb.setSelected((Boolean) value);
            if (isSelected) {
                rb.setBackground(tbl.getSelectionBackground());
            } else {
                rb.setBackground(tbl.getBackground());
            }
            return rb;
        }
    }

    private class RadioButtonEditor extends AbstractCellEditor implements TableCellEditor {
        private final JRadioButton rb = new JRadioButton();
        private int editingRow = -1;

        RadioButtonEditor() {
            rb.setHorizontalAlignment(SwingConstants.CENTER);
            rb.addActionListener(e -> {
                int row = editingRow;
                if (row >= 0 && rb.isSelected()) {
                    currentStepRow = row;
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        if (i != row) {
                            tableModel.setValueAtQuiet(false, i, COL_RADIO);
                        }
                    }
                    highlightStepRow();
                }
                fireEditingStopped();
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable tbl, Object value, boolean isSelected, int row, int column) {
            editingRow = row;
            rb.setSelected((Boolean) value);
            return rb;
        }

        @Override
        public Object getCellEditorValue() {
            return rb.isSelected();
        }
    }
}
