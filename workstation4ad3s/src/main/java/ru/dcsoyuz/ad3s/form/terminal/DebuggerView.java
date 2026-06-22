package ru.dcsoyuz.ad3s.form.terminal;

import ru.dcsoyuz.ad3s.form.RegPanel;
import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.uart.RunnerView;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/**
 * Created by yuri.filatov on 26.01.2024.
 */
public class DebuggerView extends JPanel {



    public DebuggerView() {

        JTabbedPane paneCpus = new JTabbedPane();
        paneCpus.addTab("CPU1", new CpuDebugPanel(1));
        paneCpus.addTab("CPU2", new CpuDebugPanel(2));

        JTabbedPane paneBuffers = new JTabbedPane();
        paneBuffers.addTab("BUF1", new BufferPanel(1));
        paneBuffers.addTab("BUF2", new BufferPanel(2));

        JSplitPane splitMain0 = new JSplitPane();
        splitMain0.setOrientation(JSplitPane.VERTICAL_SPLIT);
        splitMain0.setDividerSize(9);
        splitMain0.setResizeWeight(1.0);
        splitMain0.setOneTouchExpandable(true);
        splitMain0.setTopComponent(paneCpus);
        splitMain0.setBottomComponent(paneBuffers);
        splitMain0.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int bufHeight = paneBuffers.getPreferredSize().height;
                splitMain0.setDividerLocation(splitMain0.getHeight() - bufHeight - splitMain0.getDividerSize());
                splitMain0.removeComponentListener(this);
            }
        });

        setLayout(new BorderLayout());
        add(splitMain0, BorderLayout.CENTER);




    }




    private class BufferPanel extends JPanel implements IMemoryEventListener {

        private JTable tableBuf;
        private DefaultTableCellRenderer centerRenderer;
        String [] columnNames = {"Addr", "0", "1","2","3","4","5","6", "7","8", "9","10","11","12","13","14", "15"};
        public BufferPanel(int numBuf) {

            setLayout(new BorderLayout());
            add(createTableBufScrollPane());
            Model.getMemoryModel().addMemoryEventListener(numBuf == 1 ? RunnerView.DBG_BUF1_MEM : RunnerView.DBG_BUF2_MEM , this );
        }


        private JScrollPane createTableBufScrollPane(){
            tableBuf = new JTable();
            tableBuf.setShowGrid(true);
            tableBuf.setGridColor(new java.awt.Color(0x6A8AAC));
            tableBuf.setRowHeight(18);

            DefaultTableModel tableModel = new DefaultTableModel(columnNames,0);
            tableBuf.setModel(tableModel);

            JScrollPane scrollPaneTable = new JScrollPane(tableBuf);
            centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment( JLabel.CENTER );
            for(int i = 0; i<=7; i++){
                String[]row = new String [17];
                row[0] = String.valueOf(i*16);
                tableModel.insertRow(i, row);
            }
            tableBuf.getColumnModel().getColumn(0).setCellRenderer( centerRenderer );
            tableBuf.setModel(tableModel);

            int tableHeight = tableBuf.getRowHeight() * tableModel.getRowCount() + tableBuf.getTableHeader().getPreferredSize().height;
            scrollPaneTable.setPreferredSize(new Dimension(10000, tableHeight));
            scrollPaneTable.setMaximumSize(new Dimension(10000, tableHeight));
            return scrollPaneTable;
        }


        @Override
        public void updateValues() {
            List<Integer> list = Model.getMemoryModel().getRespValues();
            if (list.size() < 128) return;
            DefaultTableModel tableModel = (DefaultTableModel) tableBuf.getModel();

            for (int i = 0; i <= 7; i++) {
                for (int k = 0; k <= 15; k++) {
                    tableModel.setValueAt(Integer.toHexString((1 << 16) + list.get(i * 16 + k)).substring(1), i, 1 + k);
                }
            }
        }

        @Override
        public void updateTableValue(RegPanel panel) {

        }
    }


}
