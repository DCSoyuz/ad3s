package ru.dcsoyuz.ad3s.form.terminal;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.*;
import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.form.editor.MemoryEditor;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.*;
import ru.dcsoyuz.ad3s.model.uart.RunnerView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;

/**
 * Created by yuri.filatov on 01.09.2016.
 */
public class Ad3sFace extends JPanel implements IMemoryEventListener  {

    private static final Logger logger = LoggerFactory.getLogger(Ad3sFace.class);



    private JPanel secondLeftVerticalPanel;







    private boolean blockTableChangeEvent;
    private Charset currentCharset = null;
    private final int BUFFER_SIZE = 8912;
    private Map<AllRegAddr, RegPanel> mapFormRegister;
    private JTable table;
    private final int WIDTH_BUTTON = 90;
    private final int HEIGHT_BUTTON = 20;
    private EXIncPanel c1EXIncPanel;
    private EXIncPanel c2EXIncPanel;

    private MemoryEditor memoryEditor;

    private Integer current_cell_row ;
    private Integer current_cell_col;

    // Для выделения измененной ячейки желтым цветом
    private Integer lastChangedRow;
    private Integer lastChangedColumn;
    private boolean isUserEdit = false; // Флаг, указывающий на редактирование пользователем



    // Customize the code to set the background and foreground color for each column of a JTable
    class ColumnColorRenderer extends DefaultTableCellRenderer {
        Color backgroundColor, foregroundColor;
        Float fontSize;
        Integer fontStyle;
        Font customFont;
        public ColumnColorRenderer(Color backgroundColor, Color foregroundColor) {
            super();
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
        }
        public ColumnColorRenderer(Color backgroundColor, Color foregroundColor, float fontSize) {
            super();
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            this.fontSize = fontSize;
        }
        public ColumnColorRenderer(Color backgroundColor, Color foregroundColor, int fontStyle, float fontSize) {
            super();
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            this.fontStyle = fontStyle;
            this.fontSize = fontSize;
        }
        public ColumnColorRenderer(Color backgroundColor, Color foregroundColor, Font font) {
            super();
            this.backgroundColor = backgroundColor;
            this.foregroundColor = foregroundColor;
            this.customFont = font;
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,   boolean hasFocus, int row, int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (customFont != null) {
                cell.setFont(customFont);
            } else if (fontSize != null) {
                int style = fontStyle != null ? fontStyle : cell.getFont().getStyle();
                cell.setFont(cell.getFont().deriveFont(style, fontSize));
            }
            int cellAddr = getAddress(row, column);
            AllRegAddr allRegAddr =  AllRegAddr.getAllRegAddr(cellAddr);
            if(allRegAddr == null || isFactoryOnlyAddress(cellAddr)){
                cell.setBackground(new Color(0x5A7A9C)); // Светло-синий для null (вместо темно-синего)
            } else {
                if(columnIsValue(column)){
                    // Проверяем, является ли ячейка последней измененной (желтое выделение)
                    if((lastChangedRow != null && lastChangedColumn != null) && lastChangedRow.equals(row) && lastChangedColumn.equals(column)){
                        cell.setBackground(new Color(0xFFFF00)); // Желтый для последней измененной ячейки
                    }
                    else if((current_cell_row != null && current_cell_col != null) && current_cell_row.equals(row) && current_cell_col.equals(column)){
                        cell.setBackground(new Color(0xD0F0FF)); // Очень светлый голубой для текущей ячейки
                    } else {
                        if(allRegAddr.getReg().isOnlyRead()){
                            cell.setBackground(new Color(0xC0D0D8)); // Светло-серый для read-only
                        }else {
                            cell.setBackground(backgroundColor);
                        }
                    }
                }else {
                    if(columnIsName(column)) {
                        JLabel c = (JLabel) cell;
                        c.setToolTipText(AppFrameHelper.getHtml(allRegAddr.getReg().getDescription()));
                    }
                    cell.setBackground(backgroundColor);
                }

            }

            cell.setForeground(foregroundColor);
            ((JComponent) cell).setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            return cell;
        }
    }




    private JScrollPane createTable(){




        int numColumns =24;
        int numRows = 12;

        table= new JTable();
        String [] columnNames = {"A","Name", "Val", "A","Name", "Val", "A","Name", "0Val", "A","Name", "Val","A","Name", "Val","A","Name", "Val","A","Name", "Val","A","Name", "Val" };
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, numRows) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (((column + 1) % 3) != 0) return false;
                return !isFactoryOnlyAddress(getAddress(row, column));
            }
        };
        String[] at;

        for(int i = 0; i<=(numColumns/3)*numRows-1; i++){
            if(AllRegAddr.getAllRegAddr(i) != null && !isFactoryOnlyAddress(i)) {
                tableModel.setValueAt(String.valueOf(i), getRow(i), getIndexColumn(i));
            }
        }

        tableModel.setRowCount(numRows);


        table.setModel(tableModel);

        // Синие цвета для таблицы
        table.setBackground(new Color(0x4A6A8C)); // Светло-синий фон (вместо темно-синего)
        table.setSelectionBackground(new Color(0x8ACAE8)); // Светло-голубой выделение
        table.setSelectionForeground(Color.BLACK); // Черный текст при выделении
        table.setGridColor(new Color(0x6A8AAC)); // Светло-синие линии сетки
        table.getTableHeader().setBackground(new Color(0x5A7A9C)); // Заголовок светло-синий
        table.getTableHeader().setForeground(Color.BLACK); // Черный текст заголовка

        for(int c = 0; c <=23; c=c+3) {
            TableColumn tColumn = table.getColumnModel().getColumn(c);
            tColumn.setCellRenderer(new ColumnColorRenderer(new Color(0x6A8AAC), Color.black));
            tColumn.setPreferredWidth(19);
            tColumn.setMaxWidth(22);
            tColumn.setMinWidth(16);
        }
        Font nameFont = new Font("Georgia", Font.PLAIN, 10);
        for(int c = 0; c <=23; c=c+3) {
            TableColumn tColumn = table.getColumnModel().getColumn(c+1);
            tColumn.setCellRenderer(new ColumnColorRenderer(new Color(0x7BA8C8), Color.black, nameFont));
            tColumn.setPreferredWidth(70);
        }
        for(int c = 0; c <=23; c=c+3) {
            TableColumn tColumn = table.getColumnModel().getColumn(c+2);
            tColumn.setCellRenderer(new ColumnColorRenderer(new Color(0xE8F4F8), Color.black));
            tColumn.setPreferredWidth(24);
        }


        for ( AllRegAddr reg: AllRegAddr.values()) {
            if (!isFactoryOnlyAddress(reg.getAddress())) {
                tableModel.setValueAt(String.valueOf(reg.getDisplayName()), getRow(reg.getAddress()),getNameColumn(reg.getAddress()));
            }
        }
        tableModel.setValueAt("FFFF",0,2);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(7).setPreferredWidth(50);
        table.getColumnModel().getColumn(10).setPreferredWidth(50);
        table.getColumnModel().getColumn(13).setPreferredWidth(50);
        SingleCellTableUpdateListener singleCellTableUpdatelistener = new SingleCellTableUpdateListener(this);
        Model.getMemoryModel().addMemoryEventListener(RunnerView.TABLE_SINGLE_VALUE, singleCellTableUpdatelistener);

        table.getModel().addTableModelListener(
                new TableModelListener()
                {
                    public void tableChanged(TableModelEvent evt)
                    {
                        if(blockTableChangeEvent){
                            return;
                        }
                        if(columnIsValue(evt.getColumn())){
                            int address = getAddress(evt.getFirstRow(), evt.getColumn());
                            AllRegAddr allRegAddr = AllRegAddr.getAllRegAddr(address);
                            if(allRegAddr!= null) {
                                String valueStr = (String) ((TableModel) evt.getSource()).getValueAt(evt.getFirstRow(), evt.getColumn());
                                int value;
                                try {
                                    value = Integer.parseInt(valueStr, 16);
                                } catch (NumberFormatException e) {
                                    logger.debug("Incorrect value in table [{}, {}]", evt.getFirstRow(), evt.getColumn());
                                    return;
                                }
                                if (allRegAddr != null) {
                                    RegPanel panel = mapFormRegister.get(allRegAddr);
                                    if (panel != null) {
                                        panel.setViewValue(value);
                                        memoryEditor.updateAllBooleanValue(allRegAddr,value);
                                        updateEXIncSplitMode(allRegAddr, value);
                                    }
                                }

                                // Сохраняем информацию об измененной ячейке только при редактировании пользователем
                                if (isUserEdit) {
                                    lastChangedRow = evt.getFirstRow();
                                    lastChangedColumn = evt.getColumn();
                                    isUserEdit = false; // Сбрасываем флаг
                                    table.repaint(); // Перерисовка для отображения желтого фона
                                }
                            }
                        }
                    }
                });




        table.setRowHeight(18);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0x4A6A8C));
        table.setTableHeader(null);
        final JTable tableFinal = table;
        table = new JTable(table.getModel()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(0x3A5A7C));
                g2.setStroke(new java.awt.BasicStroke(2));
                TableColumnModel cm = getColumnModel();
                int x = 0;
                for (int group = 0; group < 8; group++) {
                    if (group > 0) {
                        g2.drawLine(x, 0, x, getHeight());
                    }
                    for (int i = 0; i < 3; i++) {
                        x += cm.getColumn(group * 3 + i).getWidth();
                    }
                }
                // Horizontal lines between rows
                g2.setColor(new Color(0x4A6A8C));
                g2.setStroke(new java.awt.BasicStroke(1));
                int rh = getRowHeight();
                for (int row = 1; row < getRowCount(); row++) {
                    int y = row * rh;
                    g2.drawLine(0, y, getWidth(), y);
                }
                g2.dispose();
            }
        };
        table.setModel(tableFinal.getModel());
        table.setRowHeight(18);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0x4A6A8C));
        table.setTableHeader(null);
        table.setFillsViewportHeight(true);
        // Re-apply renderers to new table
        for(int c = 0; c <=23; c=c+3) {
            TableColumn tColumn = table.getColumnModel().getColumn(c);
            tColumn.setCellRenderer(new ColumnColorRenderer(new Color(0x6A8AAC), Color.black));
            tColumn.setPreferredWidth(19);
            tColumn.setMaxWidth(22);
            tColumn.setMinWidth(16);
        }
        for(int c = 0; c <=23; c=c+3) {
            TableColumn tColumn = table.getColumnModel().getColumn(c+1);
            tColumn.setCellRenderer(new ColumnColorRenderer(new Color(0x7BA8C8), Color.black, nameFont));
            tColumn.setPreferredWidth(70);
        }
        for(int c = 0; c <=23; c=c+3) {
            TableColumn tColumn = table.getColumnModel().getColumn(c+2);
            tColumn.setCellRenderer(new ColumnColorRenderer(new Color(0xE8F4F8), Color.black));
            tColumn.setPreferredWidth(24);
        }
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(7).setPreferredWidth(50);
        table.getColumnModel().getColumn(10).setPreferredWidth(50);
        table.getColumnModel().getColumn(13).setPreferredWidth(50);

        // Context menu for register values (must be on the final table instance)
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) return;

                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row < 0 || col < 0) return;

                if (((col + 1) % 3) != 0) return;

                int clickedAddress = getAddress(row, col);
                AllRegAddr allRegAddr = AllRegAddr.getAllRegAddr(clickedAddress);
                if (allRegAddr == null) return;

                table.changeSelection(row, col, false, false);

                JPopupMenu popupMenu = new JPopupMenu();
                popupMenu.setBackground(new Color(0x6A8AAC));
                JMenuItem readItem = new JMenuItem("Read value");
                readItem.setBackground(new Color(0x8AAAC8));
                readItem.setForeground(Color.BLACK);
                readItem.addActionListener(event -> {
                    singleCellTableUpdatelistener.setRow(row);
                    singleCellTableUpdatelistener.setCol(col);
                    if ((clickedAddress % 2) != 0) {
                        singleCellTableUpdatelistener.setValueIndex(1);
                        Model.getMemoryModel().readSingleValue(clickedAddress - 1, 2);
                    } else {
                        singleCellTableUpdatelistener.setValueIndex(0);
                        Model.getMemoryModel().readSingleValue(clickedAddress);
                    }
                });
                popupMenu.add(readItem);

                if (!allRegAddr.getReg().isOnlyRead()) {
                    JMenuItem writeItem = new JMenuItem("Write value");
                    writeItem.setBackground(new Color(0x8AAAC8));
                    writeItem.setForeground(Color.BLACK);
                    writeItem.addActionListener(event -> {
                        Map<Integer, List<Integer>> map = new HashMap<>();
                        map.put(clickedAddress, getTableValuesForWriteToIc(clickedAddress, 1));
                        Model.getMemoryModel().setReqValues(map);
                        Model.getMemoryModel().writeValues();
                    });
                    popupMenu.add(writeItem);
                }

                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // CellEditorListener for tracking user edits (must be on the final table instance)
        table.getDefaultEditor(Object.class).addCellEditorListener(new javax.swing.event.CellEditorListener() {
            @Override
            public void editingStopped(javax.swing.event.ChangeEvent e) {
                isUserEdit = true;
            }

            @Override
            public void editingCanceled(javax.swing.event.ChangeEvent e) {
            }
        });

        JScrollPane pane = new JScrollPane(table);
        pane.setBackground(new Color(0x4A6A8C));
        pane.getViewport().setBackground(new Color(0x4A6A8C));
        pane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        pane.setPreferredSize(new Dimension(1000, 18 * numRows + 4));
        return pane;
    }




    public void loadDefaultsValues(){
        TableModel tableModel = table.getModel();
        for ( AllRegAddr reg: AllRegAddr.values()) {
            if (isFactoryOnlyAddress(reg.getAddress())) continue;
            Integer value = reg.getReg().getDefaultValue();
            tableModel.setValueAt( getHex16String(value == null ? 0 : value), getRow(reg.getAddress()),getValueColumn(reg.getAddress()));
        }

    }

    private String getHex16String(int v){
        return Integer.toHexString(0x10000 | (0xFFFF & v)).substring(1);
    }

    public void storeValuesToTxt(){



        StringBuilder sb = new StringBuilder();
        TableModel tableModel = table.getModel();
        for (int i = 0; i < Model.getMemoryModel().getNumBaseMemoryValues(); i++) {
            StringJoiner joiner = new StringJoiner("\t");
            if (isFactoryOnlyAddress(i)) {
                joiner.add(Integer.toString(i));
                joiner.add("");
            } else {
                AllRegAddr allRegAddr = AllRegAddr.getAllRegAddr(i);
                if (allRegAddr != null) {
                    joiner.add(Integer.toString(i));
                    String value = (String) tableModel.getValueAt(getRow(i), getValueColumn(i));
                    joiner.add(value);
                    joiner.add(allRegAddr.name());
                } else {
                    joiner.add(Integer.toString(i));
                    joiner.add("");
                }
            }
            joiner.add("");
            joiner.add("");
            joiner.add("");
            sb.append(joiner.toString() + "\n");
        }
        String path = Model.getEditorModel().getCurrentFile().getParent();
        File fileBaseRam = new File(path + File.separator + "base_ram.txt");
        if(!fileBaseRam.exists()){
            logger.info("File base_ram.txt not exist! This file will be created.");
        }
        PrintWriter pw = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileBaseRam);
            pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
        } catch (IOException e) {
            logger.error("Error opening base_ram.txt for writing", e);
        }
        pw.write(sb.toString());
        pw.close();
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing base_ram.txt", e);
        }
    }

    public void storeBaseValuesToHex(){
        StringBuilder sb = new StringBuilder();
        TableModel tableModel = table.getModel();
        for (int i = 0; i < Model.getMemoryModel().getNumBaseMemoryValues(); i++) {
            AllRegAddr allRegAddr = AllRegAddr.getAllRegAddr(i);
            if(allRegAddr != null) {
                String value = (String) tableModel.getValueAt(getRow(i), getValueColumn(i));
                sb.append(value + "\n");
            } else {
                sb.append("0000" + "\n");
            }
        }
        String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_BASE_HEX);
        File fileBaseRam = new File(path );
        if(!fileBaseRam.exists()){
            logger.info("File base_ram.txt not exist! This file will be created.");
        }
        PrintWriter pw = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileBaseRam);
            pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
        } catch (IOException e) {
            logger.error("Error opening file for writing", e);
        }
        pw.write(sb.toString());
        pw.close();
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing file", e);
        }
        logger.info("Created {}", path);

    }




    public StringJoiner getRdlForReg(AllRegAddr allRegAddr, boolean template, String tab){
        Regs reg = (Regs)allRegAddr.getReg();
        StringJoiner joiner = new StringJoiner("\n");
        boolean isMainReg = AllRegAddr.isContain(reg.name());
        if(!template) {
            joiner.add(tab + "\treg {");
        } else {
            joiner.add(String.format(tab + "\treg %s{", reg.getDisplayName()));
        }
        if(allRegAddr.getRegType().equals(RegType.P1) || allRegAddr.getRegType().equals(RegType.P2)){
            joiner.add(String.format(tab + "\t\tname = \"%s\";", allRegAddr.getDisplayName()));
        } else {
            joiner.add(String.format(tab + "\t\tname = \"%s\";", reg.getDisplayName()));
        }
        joiner.add(String.format(tab + "\t\thdl_path = \"%s\";", allRegAddr.getHdlName()));
        joiner.add(String.format(tab + "\t\tdefault sw = %s;", reg.isOnlyRead() ? "r" : "rw"));
        joiner.add(String.format(tab + "\t\tdefault hw = %s;", reg.isOnlyRead() ? "r" : "rw"));
        if(reg.isOnlyRead()) {
            joiner.add(String.format(tab + "\t\tdontcompare;"));
        }

        joiner.add(String.format(tab + "\t\tdesc = \"%s\";", reg.getDescription().replaceAll("\n", "\n\t\t\t\t\t\t")));



        //joiner.add("");
        if(reg.getValueType().equals(RegValueType.VALUE_FIELDS)){
            for(IRegField field : reg.getFields()){
                joiner.add(tab + "\t\tfield {");
                if(reg.isOnlyRead()) {
                    joiner.add(tab + "\t\t\tsw = r; hw = r;");
                }
                joiner.add(String.format(tab + "\t\t} %s[%d:%d] = %d;",field.getDisplayName(), field.getMsb(), field.getLsb(), field.getDefaultValue()));
                //joiner.add("");
            }
        } else {
            joiner.add(tab + "\t\tfield {");
            if(reg.isOnlyRead()) {
                joiner.add(tab + "\t\t\tsw = r; hw = r;");
                //joiner.add(String.format("\t\t\tsw = %s; hw = %s;", reg.isOnlyRead() ? "r" : "rw", reg.isOnlyRead() ? "r" : "rw"));
            }

            joiner.add(String.format(tab + "\t\t} %s[%d:%d] = %d;", "data", reg.isOnlyRead() ? 15 : reg.getValueType().getMsb(), reg.isOnlyRead() ? 0 : reg.getValueType().getLsb(), reg.getDefaultValue() == null ? 0 : reg.getDefaultValue()));

            //joiner.add("");
        }

        if(!template) {
            if(allRegAddr.getRegType().equals(RegType.P1) || allRegAddr.getRegType().equals(RegType.P2)){
                joiner.add(String.format(tab + "\t} %s @ %d;", allRegAddr.getDisplayName(), reg.getLocalAddr()*2));
            }else {
                joiner.add(String.format(tab + "\t} %s @ %d;", reg.getDisplayName(), reg.getLocalAddr()*2));
            }

        } else {
            joiner.add(tab + "\t};");
        }

        joiner.add("");
        joiner.add("");
        joiner.add("");

        return joiner;
    }



    public void generatePokeInitFile(){



        StringBuilder sb = new StringBuilder();
        sb.append("   class my_reg_seq extends uvm_reg_sequence;\n" +
                "\n" +
                "        `uvm_object_utils(my_reg_seq)\n" +
                "    \n" +
                "        function new (string name = \"\");\n" +
                "          super.new(name);\n" +
                "        endfunction\n" +
                "\n" +
                "        ad3s_regs sensor_regmodel;\n" +
                "        string F_D_WRITE = UVM_BACKDOOR;\n" +
                "      \n" +
                "        task body;\n" +
                "            uvm_status_e   status;\n" +
                "\n" +
                "            if (starting_phase != null)\n" +
                "                starting_phase.raise_objection(this);\n" +
                "            #900us;\n\n");



        String prefixDef = "sensor_regmodel.";


        String regFormatWithoutField = "%s%s.predict( .value( %s ));\n";
        String regFormatWithField = "%s%s.predict( .value( \n";
        String pre_tab = "            ";
        String tab = "                                        " + pre_tab;

        for(AllRegAddr regAddr : AllRegAddr.values()){
            if(regAddr.getReg().isOnlyRead()){
                continue;
            }
            int tableValue = Integer.parseInt((String)table.getValueAt(getRow(regAddr.getAddress()), getValueColumn(regAddr.getAddress())), 16);
            List<IRegField> list  =  regAddr.getReg().getFields();

            String dob = "";
            if(!regAddr.getRegType().equals(RegType.CR) && !regAddr.getRegType().equals(RegType.CN)){
                dob = regAddr.getRegType().name();
            }


            if(list.size() == 0){
                Integer value = tableValue >> regAddr.getReg().getValueType().getLsb();
                int numBits = regAddr.getReg().getValueType().getMsb()-regAddr.getReg().getValueType().getLsb()+1;
                String str =  String.format(regFormatWithoutField,pre_tab+ prefixDef, regAddr.getRegType().name() + "." +dob + regAddr.getReg().name(),  String.valueOf(numBits) + "'d" + String.valueOf(value)) ;
                sb.append(str);
            }else {
                String textFieldValues  = getTextWithRezervedBitDotNet(regAddr, tableValue, pre_tab+ prefixDef, "");
                sb.append(textFieldValues);
            }
            sb.append(pre_tab + tab+ "      " + "assert(status == UVM_IS_OK);\n");
        }
        sb.append("\n");
        sb.append("\n");
        sb.append(pre_tab + "sensor_regmodel.update(.status(status), .path(F_D_WRITE));" + "\n");
        sb.append(pre_tab + "assert(status == UVM_IS_OK);\n");



        sb.append("       endtask \n" +
                "    endclass");



        String path2;
        File fileBaseRam = new File(path2= (WorkstationConfig.getProperty(ConfProp.FILE_PATH_POKE_FILE)));
        PrintWriter pw = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileBaseRam);
            pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
        } catch (IOException e) {
            logger.error("Error opening poke file for writing", e);
        }
        pw.write(sb.toString());
        pw.close();
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing poke file", e);
        }

        logger.info("Created {}", path2);
    }


    public void generateEnvFiles(){
        createEnums();
        createAllRegAddrEnv();
        createFileRegFields();
        createFileRegs();
        createFileInitAllRegAddrs();
    }



    private void createFileRegs(){
        StringBuilder sb = new StringBuilder();

        for(Regs reg : Regs.values()){
            sb.append(String.format("regs[%-16s] = '{%-5d, %-5d};\n",  reg.getDisplayName(), reg.getLocalAddr(), reg.getDefaultValue()==null ? 0 : reg.getDefaultValue()));
        }
        createFile(sb, ConfProp.FILE_PATH_ENV_REGS);
    }

    private void createFileInitAllRegAddrs(){
        StringBuilder sb = new StringBuilder();

        for(AllRegAddr allRegAddr : AllRegAddr.values()) {
            if (allRegAddr.getRegType().equals(RegType.C1) || allRegAddr.getRegType().equals(RegType.C2) || allRegAddr.getRegType().equals(RegType.P1) || allRegAddr.getRegType().equals(RegType.P2)) {
                sb.append(String.format("allregaddrs[%-16s] = '{%-16s, %-5d, %d};\n", allRegAddr.name(), allRegAddr.getReg().name(), allRegAddr.getAddress(), allRegAddr.getReg().isOnlyRead() ? 1 : 0));
            } else {
                sb.append(String.format("allregaddrs[%-16s] = '{%-16s, %-5d, %d};\n", allRegAddr.getRegType().name() + "_" + allRegAddr.name(), allRegAddr.getReg().name(), allRegAddr.getAddress(), allRegAddr.getReg().isOnlyRead() ? 1 : 0));
            }
        }
        createFile(sb, ConfProp.FILE_PATH_ENV_INIT_ALL_REG_ADDR);
    }

    private void createFileRegFields(){
        StringBuilder sb = new StringBuilder();

        for(RegField regField : RegField.values()){
            sb.append(String.format("fields[%-16s] = '{%-16s, %-2d, %-2d, %d};\n",  regField.name(), regField.getReg().name(), regField.getMsb(), regField.getLsb(), regField.getDefaultValue()));
        }


        createFile(sb, ConfProp.FILE_PATH_ENV_REG_FIELDS);
    }




    private void createAllRegAddrEnv(){
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        boolean first = true;
        sb.append("typedef enum int { \n");
        for(AllRegAddr allRegAddr : AllRegAddr.values()){

            if(!first) sb.append(",\n");
            if(allRegAddr.getRegType().equals(RegType.C1) || allRegAddr.getRegType().equals(RegType.C2) || allRegAddr.getRegType().equals(RegType.P1) || allRegAddr.getRegType().equals(RegType.P2) ){
                sb.append("    " + allRegAddr.name() + " = " + allRegAddr.getAddress());
            } else {
                sb.append("    " + allRegAddr.getRegType().name() + "_" + allRegAddr.getReg().name() + " = " + allRegAddr.getAddress());
            }

            first = false;
        }
        sb.append("\n} allreg_name; \n");

        createFile(sb, ConfProp.FILE_PATH_ENV_ALL_REG_ADDR);
    }


    private void createEnums(){
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        sb.append("typedef enum int { \n");
        for(RegType regType : RegType.values()){

            if(!first) sb.append(",\n");
            sb.append("    " + regType.name() +  " = " + regType.getAddress());
            first = false;
        }
        sb.append("\n} block_addr; \n");


        sb.append("\n");
        first = true;
        sb.append("typedef enum int { \n");
        for(RegField field : RegField.values()){

            if(!first) sb.append(",\n");
            sb.append("    "+field.getDisplayName());
            first = false;
        }
        sb.append("\n} field_name; \n");

        sb.append("\n");
        sb.append("typedef enum int { \n");
        first = true;
        for(Regs reg : Regs.values()){
            if(!first) sb.append(",\n");
            sb.append("    "+reg.getDisplayName());
            first = false;
        }
        sb.append("\n} reg_name; \n");

        createFile(sb, ConfProp.FILE_PATH_ENV_ENUMS);


    }

    private void createFile(StringBuilder sb, ConfProp propPathFile){

        String path2;
        File fileBaseRam = new File(path2= (WorkstationConfig.getProperty(propPathFile)));
        PrintWriter pw = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileBaseRam);
            pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
        } catch (IOException e) {
            logger.error("Error opening file for writing: {}", propPathFile, e);
        }
        pw.write(sb.toString());
        pw.close();
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing file: {}", propPathFile, e);
        }

        logger.info("Created {}", path2);


    }

    public void generateRDLFile() {
        generateRDLFileSpi();
        generateRDLFileRom512b();
    }



    public void generateRDLFileRom512b(){

        StringBuilder sb = new StringBuilder();
        sb.append("addrmap ad3s_regs_rom512b {\n");
        sb.append("\n");
        sb.append("\tname = \"AD3S registers addresses and values for uvm\";\n");

        sb.append("\tdefault regwidth = 16;\n");
        sb.append("\tdefault sw = rw;\n");
        sb.append("\tdefault hw = rw;\n");
        sb.append("\n");
        TableModel tableModel = table.getModel();

        for(AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.C1)){
            sb.append(getRdlForReg(allRegAddr, true, ""));
        }


        sb.append("\n");
        sb.append("\n");
        sb.append("\n");

        sb.append(
                "  reg cpu_wr_ram_cell{\n" +
                "\t\tname = \"cpu_ram14bit\";\n" +
                "\t\t\n" +
                "\t\tdefault sw = rw;\n" +
                "\t\tdefault hw = rw;\n" +
                "\t\tdesc = \"register\";\n" +
                "\t\tfield {\n" +
                "\t\t} data[13:0] = 0;\n" +
                "\t};\n");

        sb.append("\n");
        sb.append("\n");
        sb.append("\n");


        sb.append("  regfile {\n" +
                "\t\thdl_path = \"ROM512B\";\n" +
                "\n" +
                "    cpu_wr_ram_cell ram_wr[2][116] @ 0;\n" +
                "\n" +
                "  } CP2 @ 0;\n" +
                "\n" );


        sb.append("\tregfile  {\n");
        sb.append(String.format("\t\thdl_path = \"ROM512B\";\n"));

        for (int i = 0; i<=15; i++) {
            AllRegAddr allRegAddr =  AllRegAddr.getRegAddr(i, RegType.C1);
            sb.append(String.format("\t\t%-10s %-14s @ %d;\n", allRegAddr.getReg().name(), allRegAddr.name(), allRegAddr.getReg().getLocalAddr()*2));
        }
        sb.append("\t} C1 @ 464;\n");

        sb.append("  regfile {\n" +
                "\t\thdl_path = \"ROM512B\";\n" +
                "\n" +
                "    cpu_wr_ram_cell ram_wr[2][4] @ 0;\n" +
                "\n" +
                "  } DOB8 @ 496;\n" +
                "\n" );

        sb.append("  regfile {\n" +
                "\t\thdl_path = \"ROM512B\";\n" +
                "\n" +
                "    cpu_wr_ram_cell ram_wr[2][116] @ 0;\n" +
                "\n" +
                "  } CP1 @ 512;\n" +
                "\n" );


        sb.append("\tregfile {\n");
        sb.append(String.format("\t\thdl_path = \"ROM512B\";\n"));

        for (int i = 0; i<=15; i++) {
            AllRegAddr allRegAddr =  AllRegAddr.getRegAddr(i, RegType.C2);
            sb.append(String.format("\t\t%-10s %-14s @ %d;\n", allRegAddr.getReg().name(), allRegAddr.name(), allRegAddr.getReg().getLocalAddr()*2));
        }
        sb.append("\t} C2 @ 976;\n");

        sb.append("\tregfile {\n");
        sb.append(String.format("\t\thdl_path = \"ROM512B\";\n\n"));

        for (int i = 0; i<=7; i++) {
            AllRegAddr allRegAddr =  AllRegAddr.getRegAddr(i, RegType.CR);
            sb.append(getRdlForReg(allRegAddr, false, "\t"));
        }
        sb.append("\t} CR @ 1008;\n");


        sb.append("\n");
        sb.append("};\n");



        String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_RDL_FILE);
        String path2;
        File fileBaseRam = new File(path2 = path + File.separator + "ad3s_regs_rom512b.rdl");
        if( !fileBaseRam.exists()) {
            logger.info("File ad3s_regs_rom512b.rdl not exist! This file will be created.");
        }
        PrintWriter pw = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileBaseRam);
            pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
        } catch (IOException e) {
            logger.error("Error opening ad3s_regs_rom512b.rdl for writing", e);
        }
        pw.write(sb.toString());
        pw.close();
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing ad3s_regs_rom512b.rdl", e);
        }
        logger.info("Created {}", path2);

    }



    public void generateRDLFileSpi(){

        StringBuilder sb = new StringBuilder();
        sb.append("addrmap ad3s_regs {\n");
        sb.append("\n");
        sb.append("\tname = \"AD3S registers addresses and values for uvm\";\n");

        sb.append("\tdefault regwidth = 16;\n");
        sb.append("\tdefault sw = rw;\n");
        sb.append("\tdefault hw = rw;\n");
        sb.append("\n");
        TableModel tableModel = table.getModel();

        for(AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.C1)){
            sb.append(getRdlForReg(allRegAddr, true, ""));
        }


        sb.append("\n");
        sb.append("\n");
        sb.append("\n");

        sb.append("  reg cpu_ro_ram_cell{\n" +
                "\t\tname = \"cpu_ram14bit\";\n" +
                "\t\t\n" +
                "\t\tdefault sw = r;\n" +
                "\t\tdefault hw = r;\n" +
                "\t\tdesc = \"register\";\n" +
                "\t\tfield {\n" +
                "\t\t} data[13:0] = 0;\n" +
                "\t};\n" +
                "\n" +
                "\n" +
                "  reg cpu_wr_ram_cell{\n" +
                "\t\tname = \"cpu_ram14bit\";\n" +
                "\t\t\n" +
                "\t\tdefault sw = rw;\n" +
                "\t\tdefault hw = rw;\n" +
                "\t\tdesc = \"register\";\n" +
                "\t\tfield {\n" +
                "\t\t} data[13:0] = 0;\n" +
                "\t};\n");

        sb.append("\n");
        sb.append("\n");
        sb.append("\n");


        sb.append("\tregfile  {\n");
        sb.append(String.format("\t\thdl_path = \"%s\";\n", RegType.C1.getHdlPath()));

        for (AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.C1)) {
            sb.append(String.format("\t\t%-10s %-14s @ %d;\n", allRegAddr.getReg().name(), allRegAddr.name(), allRegAddr.getReg().getLocalAddr()*2));
        }
        sb.append("\t} C1 @ 0;\n");


        sb.append("\tregfile {\n");
        sb.append(String.format("\t\thdl_path = \"%s\";\n", RegType.C2.getHdlPath()));

        for (AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.C2)) {
            sb.append(String.format("\t\t%-10s %-14s @ %d;\n", allRegAddr.getReg().name(), allRegAddr.name(), allRegAddr.getReg().getLocalAddr()*2));
        }
        sb.append("\t} C2 @ 64;\n");

        sb.append("\tregfile {\n");
        sb.append(String.format("\t\thdl_path = \"%s\";\n\n", RegType.CR.getHdlPath()));

        for(AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.CR)){
            sb.append(getRdlForReg(allRegAddr, false, "\t"));
        }
        sb.append("\t} CR @ 128;\n");





        sb.append("\tregfile {\n");
        sb.append(String.format("\t\thdl_path = \"%s\";\n\n", RegType.CN.getHdlPath()));

        for(AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.CN)){
            sb.append(getRdlForReg(allRegAddr, false, "\t"));
        }

        sb.append("\t} CN @ 160;\n");

        sb.append("\tregfile {\n");
        sb.append(String.format("\t\thdl_path = \"%s\";\n\n", RegType.P1.getHdlPath()));

        for(AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.P1)){
            sb.append(getRdlForReg(allRegAddr, false, "\t"));
        }
        sb.append("\t} P1 @ 184;\n");

        sb.append("\tregfile {\n");
        sb.append(String.format("\t\thdl_path = \"%s\";\n\n", RegType.P2.getHdlPath()));

        for(AllRegAddr allRegAddr : AllRegAddr.getAllRegAddrsByType(RegType.P2)){
            sb.append(getRdlForReg(allRegAddr, false, "\t"));
        }
        sb.append("\t} P2 @ 188;\n");



        sb.append("  regfile {\n" +
                "\t\thdl_path = \"CPU1\";\n" +
                "\n" +
                "    cpu_wr_ram_cell ram_wr[2][116] @ 0;\n" +
                "    cpu_ro_ram_cell ram_ro[2][12]  @ 464;\n" +
                "\n" +
                "  } CP1 @ 1024;\n" +
                "\n" +
                "  regfile {\n" +
                "\t\thdl_path = \"BUF1\";\n" +
                "\n" +
                "    cpu_wr_ram_cell ram_wr[2][64] @ 0;\n" +
                "\n" +
                "  } BF1 @ 1536;\n" +
                "\n" +
                "  regfile {\n" +
                "\t\thdl_path = \"CPU2\";\n" +
                "\n" +
                "    cpu_wr_ram_cell ram_wr[2][116] @ 0;\n" +
                "    cpu_ro_ram_cell ram_ro[2][12]  @ 464;\n" +
                "\n" +
                "  } CP2 @ 2048;\n" +
                "\n" +
                "  regfile {\n" +
                "\t\thdl_path = \"BUF2\";\n" +
                "\n" +
                "    cpu_wr_ram_cell ram_wr[2][64] @ 0;\n" +
                "\n" +
                "  } BF2 @ 2560;\n");





        sb.append("\n");
        sb.append("};\n");



        String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_RDL_FILE);
        String path2;
        File fileBaseRam = new File(path2 = path + File.separator + "ad3s_regs.rdl");
        if( !fileBaseRam.exists()) {
            logger.info("File ad3s_regs.rdl not exist! This file will be created.");
        }
        PrintWriter pw = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileBaseRam);
            pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
        } catch (IOException e) {
            logger.error("Error opening ad3s_regs.rdl for writing", e);
        }
        pw.write(sb.toString());
        pw.close();
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing ad3s_regs.rdl", e);
        }
        logger.info("Created {}", path2);

    }


    private String getTextIndex(int n){
        return n >= 10 ? String.format("%2d", n) : "_"+ String.valueOf(n) ;
    }

    public void createFileDef(){



        StringBuilder sb = new StringBuilder();
        sb.append("`ifndef INCLUDE_AD3S_DEFINES\n");
        sb.append("`define INCLUDE_AD3S_DEFINES\n");

        String format = "%s%-30s%2s\n";
        String prefixDef = "`define\t";


        sb.append("\n");
        for(AddrDef addrDef : AddrDef.values()){
            String line = String.format(format,prefixDef, addrDef.name(),  String.valueOf(addrDef.getAddr()));
            sb.append(line);
        }
        for(AddrCpuHand addrCpuHand : AddrCpuHand.values()){
            String line = String.format(format,prefixDef, addrCpuHand.name(),  String.valueOf(addrCpuHand.getAddr()));
            sb.append(line);
        }
        sb.append("\n");
        sb.append("\n");
        for(Regs reg : Regs.values()){
            String line = String.format(format,prefixDef, "A_" + reg.getDisplayName(),  String.valueOf(reg.getLocalAddr()));
            sb.append(line);
        }
        sb.append("\n");
        sb.append("\n");

        for(AllRegAddr regAddr : AllRegAddr.values()){
            String line = String.format(format,prefixDef, regAddr.getRegType().name() +  "_A_" +  regAddr.getReg().name(),  String.valueOf(regAddr.getAddress()));
            sb.append(line);
        }
        sb.append("\n");
        sb.append("\n");

        String format2 = "%s%-30s%2s\n";
        for(RegField field : RegField.values()){
            if(field.getMsb() != field.getLsb()) {
                String line1 = String.format(format2, prefixDef, "MSB"  + getTextIndex(field.getMsb()) +"_"+field.getDisplayName(), String.valueOf(field.getMsb()));
                sb.append(line1);
                String line2 = String.format(format2, prefixDef, "LSB" + getTextIndex(field.getLsb()) +"_"+field.getDisplayName(), String.valueOf(field.getLsb()));
                sb.append(line2);
            } else {
                String line = String.format(format2, prefixDef, "IND" + getTextIndex(field.getLsb()) +"_" +field.getDisplayName(), String.valueOf(field.getMsb()));
                sb.append(line);
            }
        }
        sb.append("\n");
        sb.append("\n");



        String regFormatWithoutField = "%s%-20s%s\n";
        String regFormatWithField = "%s%-20s      \\\n";


        for(AllRegAddr regAddr : AllRegAddr.values()){
            if(regAddr.getReg().isOnlyRead()){
                continue;
            }
            int tableValue = Integer.parseInt((String)table.getValueAt(getRow(regAddr.getAddress()), getValueColumn(regAddr.getAddress())), 16);
            List<IRegField> list  =  regAddr.getReg().getFields();
            if(list.size() == 0){
                Integer value = tableValue >> regAddr.getReg().getValueType().getLsb();
                int numBits = regAddr.getReg().getValueType().getMsb()-regAddr.getReg().getValueType().getLsb()+1;
                String str =  String.format(regFormatWithoutField,prefixDef, regAddr.getRegType().name() + "_DFLT_" +  regAddr.getReg().name(),  String.valueOf(numBits) + "'d" + String.valueOf(value));
                sb.append(str);
            }else {
                String str =  String.format(regFormatWithField,prefixDef, regAddr.getRegType().name() + "_DFLT_" +  regAddr.getReg().name());
                sb.append(str);
                sb.append("{                                 \\\n");
                String textFieldValues  = getTextWithRezervedBit(list, tableValue, "", "\\");
                sb.append(textFieldValues);
                sb.append("}\n");
            }
        }
        sb.append("\n");
        sb.append("\n");
        sb.append("`endif");
        String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_DEFINE_TXT_FILE);
        String path2 = path + File.separator + "ad3s_defines.v";
        File fileBaseRam = new File(path + File.separator + "ad3s_defines.v");
        if(!fileBaseRam.exists()){
            logger.info("File ad3s_defines.v not exist! This file will be created.");
        }
        PrintWriter pw = null;
        OutputStream out = null;
        try {
            out = new FileOutputStream(fileBaseRam);
            pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")));
        } catch (IOException e) {
            logger.error("Error opening ad3s_defines.v for writing", e);
        }
        pw.write(sb.toString());
        pw.close();
        try {
            out.close();
        } catch (IOException e) {
            logger.error("Error closing ad3s_defines.v", e);
        }

        logger.info("Created {}", path2);
    }

    private String getTextWithRezervedBitDotNet(AllRegAddr regAddr, int tableValue, String tab, String delimiter){
        StringBuilder sb = new StringBuilder();
        List<IRegField> list  =  regAddr.getReg().getFields();
        for(IRegField field : list) {
            int numBits = field.getMsb()-field.getLsb()+1;
            String radix = numBits == 1 ? "b" : "d";
            String str = tab  + regAddr.getRegType() + "." + regAddr.getDisplayName() + "." + ((RegField)field).name() + "." + "predict( .value( " + String.valueOf(numBits) + "'" + radix + String.valueOf(field.getFieldValueFromRegValue(tableValue)) + "));\n";
            sb.append(str);
        }
        return sb.toString();
    }



    private String getTextWithRezervedBit(List<IRegField> list, int tableValue, String tab, String delimiter){
        StringBuilder sb = new StringBuilder();
        String fieldFormat = "%-6s%s /* %-20s*/ "+delimiter+"\n";
        List<RegField> result;
        Map<Integer, IRegField> map = new HashMap();
        for (IRegField field : list){
            int msb = field.getMsb();
            int lsb = field.getLsb();
            for(int k = lsb ; k<= msb; k++){
                map.put(k, field);
            }
        }
        Integer lsbReserved = null;
        Integer msbReserved = null;
        int lastIndex =15;
        for(int i = 15; i>=0; i--){
            IRegField field = map.get(i);
            if(field == null){
                if(msbReserved == null) {
                    msbReserved = i;
                }
                if(i == 0 && msbReserved != null){
                    lsbReserved = i;
                    int numBits = msbReserved - lsbReserved + 1;
                    String radix = numBits == 1 ? "b" : "d";
                    String str2 = tab + String.format(fieldFormat, String.valueOf(numBits) + "'"+ radix + "0","  ", "Reserved");
                    sb.append(str2);
                    msbReserved = null;
                    lsbReserved = null;
                }
            } else {
                String comma = ",";
                if(msbReserved != null){
                    lsbReserved = i;
                    int numBits = msbReserved - lsbReserved ;
                    String radix = numBits == 1 ? "b" : "d";
                    String str2 = tab +  String.format(fieldFormat,String.valueOf(numBits) + "'"+ radix + "0",comma, "Reserved");
                    sb.append(str2);
                    msbReserved = null;
                    lsbReserved = null;
                }
                if(field.getLsb() == 0 || lastIndex==0){
                    comma = " ";
                }
                int numBits = field.getMsb()-field.getLsb()+1;
                i = i - (numBits-1);
                        String radix = numBits == 1 ? "b" : "d";
                String str2 =  tab + String.format(fieldFormat,String.valueOf(numBits) + "'"+ radix + String.valueOf(field.getFieldValueFromRegValue(tableValue)),comma, field.getDisplayName());
                sb.append(str2);
            }
            lastIndex = lastIndex - 1;


        }
        return sb.toString();
    }

    public Map<Integer, List<Integer>> getTableValuesForWriteToIc(){
        Map<Integer, List <Integer>> map = new HashMap<>();
        //map.put(9, getTableValuesForWriteToIc(9,2));
        map.put(0, getTableValuesForWriteToIc(0,16));
        map.put(31, getTableValuesForWriteToIc(31,1));
        map.put(32, getTableValuesForWriteToIc(32,16));
        map.put(63, getTableValuesForWriteToIc(63,1));
        map.put(64, getTableValuesForWriteToIc(64,8));
        return map;
    }


    public Map<Integer, List<Integer>> getTableValuesForWriteCtrlOTP(){
        Map<Integer, List <Integer>> map = new HashMap<>();
        map.put(80, getTableValuesForWriteToIc(80,3));
        map.put(84, getTableValuesForWriteToIc(84,3));
        if (Model.isFactoryMode()) {
            map.put(88, getTableValuesForWriteToIc(88,3));
        }
        return map;
    }

    public  List<Integer> getTableValuesForWriteToIc(int address, int size){
        TableModel tableModel = table.getModel();
        List <Integer> list = new ArrayList<>();
        for(int i =address ; i< address+size;  i++){
                String valueStr = (String) tableModel.getValueAt(getRow(i), getValueColumn(i));
                if(valueStr == null || valueStr.equals("")){
                    valueStr = "0";
                }
                Integer value = Integer.parseInt(valueStr, 16);
                list.add(value);
        }
        return list;
    }

    public int getCurrentRegValue(AllRegAddr allRegAddr){
        TableModel tableModel = table.getModel();
        int value = Integer.parseInt((String)tableModel.getValueAt(getRow(allRegAddr.getAddress()), getValueColumn(allRegAddr.getAddress())),16);
        return value;
    }


    private boolean columnIsValue(int col){
        if(  (col+1)%3 == 0 ){
            return true;
        } else {
            return false;
        }
    }


    private boolean columnIsName(int col){
        if(  (col+2)%3 == 0 ){
            return true;
        } else {
            return false;
        }
    }
    private int getAddress(int row, int col){
        return row * 8 + (int)col/3;
    }
    public void loadValuesFromTxt(){
        BufferedReader br = null;
        InputStream in = null;
        String path = Model.getEditorModel().getCurrentFile().getParent();
        File fileBaseRam = new File(path + File.separator + "base_ram.txt");
        if(!fileBaseRam.exists()){
            logger.warn("File base_ram.txt not exist! Data values cannot be loaded!");
            return;
        }
        try{
            in = new FileInputStream(fileBaseRam);
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFFER_SIZE);
            TableModel tableModel = table.getModel();
            int i = 0;
            String line;
            while((line = br.readLine())!= null) {
                if (isFactoryOnlyAddress(i)) { i++; continue; }
                AllRegAddr allRegAddr = AllRegAddr.getAllRegAddr(i);
                if (allRegAddr != null) {
                    String[] data = line.split("\t");
                    if(data.length >= 2) {
                        tableModel.setValueAt(data[1], getRow(i), getValueColumn(i));
                        tableModel.setValueAt(allRegAddr.name(), getRow(i), getNameColumn(i));
                        tableModel.setValueAt(String.valueOf(i), getRow(i), getIndexColumn(i));
                    }
                }
                i = i+1;

            }

        }catch (FileNotFoundException e) {
            logger.warn("File {} not found.", e.getMessage());
        } catch (IOException e) {
            logger.error("IOException loading base_ram.txt", e);
        }
    }


    public void loadBaseValuesFromHex(){
        BufferedReader br = null;
        InputStream in = null;
        String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_BASE_HEX);
        File fileBaseRam = new File(path);
        if(!fileBaseRam.exists()){
            logger.warn("File base_ram.hex not exist! Data values cannot be loaded!");
            return;
        }
        try{
            in = new FileInputStream(fileBaseRam);
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFFER_SIZE);
            TableModel tableModel = table.getModel();
            int i = 0;
            String line;
            while((line = br.readLine())!= null) {
                if (isFactoryOnlyAddress(i)) { i++; continue; }
                AllRegAddr allRegAddr = AllRegAddr.getAllRegAddr(i);
                if (allRegAddr != null) {
                    tableModel.setValueAt(line, getRow(i), getValueColumn(i));
                    tableModel.setValueAt(allRegAddr.name(), getRow(i), getNameColumn(i));
                    tableModel.setValueAt(String.valueOf(i), getRow(i), getIndexColumn(i));
                }
                i = i+1;

            }

        }catch (FileNotFoundException e) {
            logger.warn("File {} not found.", e.getMessage());
        } catch (IOException e) {
            logger.error("IOException loading base_ram.hex", e);
        }
    }
    private int getValueColumn(int address){
        int col = (address % 8) * 3 + 2;
        return col;
    }
    private int getNameColumn(int address){
        int col =( address % 8) * 3 + 1;
        return col;
    }
    private int getIndexColumn(int address){
        int col = ( address % 8 ) *3;
        return col;
    }
    private int getRow(int address){
        int row = (int)(address/8);
        return row;
    }


    public Boolean getBooleanTableValue(AllRegAddr allRegAddr, RegField field){
        int regValue = getCurrentRegValue(allRegAddr);
        int fieldValue = field.getFieldValueFromRegValue(regValue);
        if(fieldValue == 0) {
            return  false;
        }else if(fieldValue == 1){
            return true;
        }else {
            logger.debug(" No boolean!");
        }
        return null;
    }


    public Ad3sFace(){

        JPanel fakePanel = new JPanel();
        add(fakePanel);
        mapFormRegister = new HashMap<>();


        fakePanel.setLayout(new GridBagLayout());

        secondLeftVerticalPanel = new JPanel();
        secondLeftVerticalPanel.setLayout(new GridBagLayout());

        CellIcAddrTableListener cellIcAddrTableListener = new CellIcAddrTableListener(this);
        Model.getMemoryModel().addMemoryEventListener(RunnerView.TABLE_IC_ADDRESS, cellIcAddrTableListener);

        JPanel mainRegistersPanel = new JPanel();
        //mainRegistersPanel.setLayout(new GridBagLayout());
        mainRegistersPanel.setLayout(new BoxLayout(mainRegistersPanel, BoxLayout.X_AXIS));
        mainRegistersPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x6BB3F0), 1),
            "Main registers"
        ));
        ((javax.swing.border.TitledBorder)mainRegistersPanel.getBorder()).setTitleColor(Color.WHITE);
        JTabbedPane tabbedPaneMainRegs  = new JTabbedPane();
        JPanel tabMainRegPanel1 = new JPanel();
        tabMainRegPanel1.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JPanel tabMainRegPanel2 = new JPanel();
        tabMainRegPanel2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        tabbedPaneMainRegs.addTab("1", tabMainRegPanel1);
        tabbedPaneMainRegs.addTab("2", tabMainRegPanel2);

        JTabbedPane tabbedPaneHandlers = new JTabbedPane();

        JPanel tabC1Panel = createTabPanel(RegType.C1 );
        JPanel  tabC2Panel = createTabPanel(RegType.C2 );
        tabbedPaneHandlers.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0x6BB3F0), 1),
            "Handler registers"
        ));
        ((javax.swing.border.TitledBorder)tabbedPaneHandlers.getBorder()).setTitleColor(Color.WHITE);
        tabbedPaneHandlers.addTab("C1", tabC1Panel );
        tabbedPaneHandlers.addTab("C2", tabC2Panel );

        GridBagHelper help1 = new GridBagHelper();

        fakePanel.add(createTable(), help1.setGridWidth(3).fillHorizontally().get());
        // Main registers
        GridBagConstraints gbcMain = help1.nextRow().setGridWidth(1).get();
        gbcMain.weightx = 0.33;
        gbcMain.weighty = 1.0;
        gbcMain.fill = GridBagConstraints.BOTH;
        fakePanel.add(mainRegistersPanel, gbcMain);
        // Handler registers
        GridBagConstraints gbcHandlers = help1.rightColumn().get();
        gbcHandlers.weightx = 0.33;
        gbcHandlers.weighty = 1.0;
        gbcHandlers.fill = GridBagConstraints.BOTH;
        fakePanel.add(tabbedPaneHandlers, gbcHandlers);
        // Actions/Memory editor
        GridBagConstraints gbcActions = help1.rightColumn().get();
        gbcActions.weightx = 0.33;
        gbcActions.weighty = 1.0;
        gbcActions.fill = GridBagConstraints.BOTH;
        fakePanel.add(memoryEditor = new MemoryEditor(this), gbcActions);


        GridBagHelper help4 = new GridBagHelper();


        JPanel leftMainPanel = new JPanel();
        leftMainPanel.setLayout(new BoxLayout(leftMainPanel, BoxLayout.Y_AXIS));
        leftMainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Уменьшенные отступы
        JPanel rightMainPanel = new JPanel();
        rightMainPanel.setLayout(new BoxLayout(rightMainPanel, BoxLayout.Y_AXIS));
        rightMainPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Уменьшенные отступы
        JPanel leftMainPanel2 = new JPanel();
        leftMainPanel2.setLayout(new BoxLayout(leftMainPanel2, BoxLayout.Y_AXIS));
        leftMainPanel2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Уменьшенные отступы
        JPanel rightMainPanel2 = new JPanel();
        rightMainPanel2.setLayout(new BoxLayout(rightMainPanel2, BoxLayout.Y_AXIS));
        rightMainPanel2.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Уменьшенные отступы

        leftMainPanel2.add(createRegisterPanel(AllRegAddr.Mask_Stat));
        if (Model.isFactoryMode()) {
            AllRegAddr f1 = FactoryGate.regF1();
            AllRegAddr f2 = FactoryGate.regF2();
            AllRegAddr f3 = FactoryGate.regF3();
            if (f1 != null) leftMainPanel2.add(createRegisterPanel(f1));
            if (f2 != null) leftMainPanel2.add(createRegisterPanel(f2));
            if (f3 != null) leftMainPanel2.add(createRegisterPanel(f3));
        }
        leftMainPanel2.add(createRegisterPanel(AllRegAddr.BOTP_addr));
        leftMainPanel2.add(createRegisterPanel(AllRegAddr.BOTP_data));
        leftMainPanel2.add(createRegisterPanel(AllRegAddr.BOTP_ctrl));
        rightMainPanel2.add(createRegisterPanel(AllRegAddr.Stat_main));
        rightMainPanel2.add(createRegisterPanel(AllRegAddr.PLL_config));
        rightMainPanel2.add(createRegisterPanel(AllRegAddr.INIT_conf));
        rightMainPanel2.add(createRegisterPanel(AllRegAddr.UOTP_ctrl));


        leftMainPanel.add(createRegisterPanel(AllRegAddr.AFE_config));
        leftMainPanel.add(createRegisterPanel(AllRegAddr.NOCLK_stat));


        rightMainPanel.add(createRegisterPanel(AllRegAddr.Mode_config));

        rightMainPanel.add(createRegisterPanel(AllRegAddr.ADC_config));
        rightMainPanel.add(createRegisterPanel(AllRegAddr.CMP_lth));
        rightMainPanel.add(createRegisterPanel(AllRegAddr.IC_addr));


        leftMainPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        rightMainPanel.setAlignmentY(Component.TOP_ALIGNMENT);
        mainRegistersPanel.add(tabbedPaneMainRegs);

        tabMainRegPanel1.setLayout(new BoxLayout(tabMainRegPanel1, BoxLayout.X_AXIS));
        tabMainRegPanel1.add(leftMainPanel);
        tabMainRegPanel1.add(rightMainPanel);
        tabMainRegPanel1.setAlignmentY(Component.TOP_ALIGNMENT);
        tabMainRegPanel2.setLayout(new BoxLayout(tabMainRegPanel2, BoxLayout.X_AXIS));
        tabMainRegPanel2.add(leftMainPanel2);
        tabMainRegPanel2.add(rightMainPanel2);
        tabMainRegPanel2.setAlignmentY(Component.TOP_ALIGNMENT);
        loadDefaultsValues();

        Model.getMemoryModel().addMemoryEventListener(RunnerView.TABLE_MAIN, this);

    }




    public void updateValues(){
        List<Integer> values =Model.getMemoryModel().getRespValues();
        TableModel tableModel = table.getModel();
        for(int i =0; i<= Model.getMemoryModel().getNumBaseMemoryValues(); i++){
            if(isFactoryOnlyAddress(i)) continue;
            if(AllRegAddr.getAllRegAddr(i) != null) {
                tableModel.setValueAt(getHex16String(values.get(i)), getRow(i), getValueColumn(i));
            }
        }
    }



    private JPanel createRegisterPanel(AllRegAddr regAddr){
        RegPanel panel = new RegPanel(regAddr,this);

        mapFormRegister.put(regAddr, panel);
        return panel;
    }


    public void setBooleanFieldValue(AllRegAddr allRegAddr,  RegField regField, boolean value){
        int inValue = value ? 1 << regField.getLsb() : 0;
        int viewValue = mapFormRegister.get(allRegAddr).getViewValue();
        int mask = ~(1 << regField.getLsb());
        int cleanViewValue = mask & viewValue;
        int newViewValue = cleanViewValue | inValue;
        mapFormRegister.get(allRegAddr).setViewValue(newViewValue);
    }


    public void updateTableValue(RegPanel panel){
        int address = getKey(panel).getAddress();
        TableModel tableModel = table.getModel();
        int value = panel.getViewValue();
        memoryEditor.updateAllBooleanValue(getKey(panel), value);
        blockTableChangeEvent = true;
        tableModel.setValueAt(Integer.toHexString(0x10000 | (0xFFFF &value)).substring(1), getRow(address), getValueColumn(address));
        current_cell_col = getValueColumn(address);
        current_cell_row = getRow(address);

        // Устанавливаем желтое выделение на измененную ячейку
        lastChangedRow = getRow(address);
        lastChangedColumn = getValueColumn(address);

        blockTableChangeEvent = false;
        table.repaint();

        // EXO_mode change → toggle EXInc split mode
        updateEXIncSplitMode(getKey(panel), value);
    }

    private void updateEXIncSplitMode(AllRegAddr changedAddr, int regValue) {
        if (changedAddr == AllRegAddr.C1ExoStngs && c1EXIncPanel != null) {
            int exoMode = RegField.EXO_mode.getFieldValueFromRegValue(regValue);
            c1EXIncPanel.setSplitMode(exoMode == 1);
        } else if (changedAddr == AllRegAddr.C2ExoStngs && c2EXIncPanel != null) {
            int exoMode = RegField.EXO_mode.getFieldValueFromRegValue(regValue);
            c2EXIncPanel.setSplitMode(exoMode == 1);
        }
    }

    public AllRegAddr getKey( RegPanel value) {
        for (Map.Entry<AllRegAddr, RegPanel> entry : mapFormRegister.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }



    private JPanel createTabPanel(RegType regType){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2)); // Уменьшенные отступы

        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));


        panel1.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.ExoStngs, regType)));

        AllRegAddr exIncAddr = AllRegAddr.getRegAddr(Regs.EXInc, regType);
        EXIncPanel exIncPanel = new EXIncPanel(exIncAddr, this);
        if (regType == RegType.C1) c1EXIncPanel = exIncPanel;
        else c2EXIncPanel = exIncPanel;
        mapFormRegister.put(exIncAddr, exIncPanel);
        panel1.add(exIncPanel);
        panel1.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.InputStngs,  regType)));
        panel1.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.ExPhShft,   regType)));
        panel1.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.KonturStngs,    regType)));

        panel2.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.Mask,     regType)));
        panel2.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.ResCntrl, regType)));

        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.Stat, regType)));
        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.KampS, regType)));
        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.KampC, regType)));
        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.KbiasS, regType)));
        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.KbiasC, regType)));
        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.fbias,    regType)));
        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.Zero,     regType)));
        panel3.add(createRegisterPanel(AllRegAddr.getRegAddr(Regs.Amp_th,   regType)));

        panel1.setAlignmentY(Component.TOP_ALIGNMENT);
        panel2.setAlignmentY(Component.TOP_ALIGNMENT);
        panel2.setMinimumSize(new Dimension(180, 0));
        panel2.setPreferredSize(new Dimension(180, 0));
        panel3.setAlignmentY(Component.TOP_ALIGNMENT);
        panel.add(panel1);
        panel.add(panel2);
        panel.add(panel3);







        return panel;
    }

    private JPanel getTextFieldLabeled(JTextField textField,String label){
        return  AppFrameHelper.getTextFieldLabeled(textField, label,150, 100);
    }



    private class CellIcAddrTableListener implements  IMemoryEventListener {

        Ad3sFace face;


        public CellIcAddrTableListener(Ad3sFace face) {
            this.face = face;
        }

        @Override
        public void updateValues() {
            int value = Model.getMemoryModel().getRespValues().get(0);
            TableModel model = face.table.getModel();
            model.setValueAt(Integer.toHexString(65536 | value).substring(1), getRow(AllRegAddr.BUS_addr.getAddress()), getValueColumn(AllRegAddr.BUS_addr.getAddress()));
        }

        @Override
        public void updateTableValue(RegPanel panel) {

        }
    }





    private class SingleCellTableUpdateListener implements IMemoryEventListener {


        Ad3sFace face;

        int row;
        int col;
        int valueIndex;

        public SingleCellTableUpdateListener(Ad3sFace face) {
            this.face = face;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public void setCol(int col) {
            this.col = col;
        }

        public void setValueIndex(int index) {
            this.valueIndex = index;
        }

        @Override
        public void updateValues() {
            TableModel model = face.table.getModel();

            List<Integer> respValues = Model.getMemoryModel().getRespValues();
            int value = respValues.get(valueIndex);
            model.setValueAt(Integer.toHexString(65536 | value).substring(1), row, col);
        }

        @Override
        public void updateTableValue(RegPanel panel) {

        }


    }


    private static boolean isFactoryOnlyAddress(int address) {
        return FactoryGate.isFactoryOnlyAddress(address);
    }

}
