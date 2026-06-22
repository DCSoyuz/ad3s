package ru.dcsoyuz.ad3s.form.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.dcsoyuz.ad3s.config.ConfCpuDebugProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.RegPanel;
import ru.dcsoyuz.ad3s.form.editor.*;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.uart.MemoryModel;
import ru.dcsoyuz.ad3s.model.uart.RunnerView;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * Created by yuri.filatov on 08.06.2024.
 */
public class CpuDebugPanel extends  JPanel implements IMemoryEventListener, IParserEventListener, ILongProcessEventListener {

    private static final Logger logger = LoggerFactory.getLogger(CpuDebugPanel.class);

    int numCpu;
    List<JButton> listDebugButtons = new LinkedList<>();

    final Integer font_size = 11;
    private JTextPane paneClearAssembler;
    private JTextPane paneHexCodes;
    private JTextField addr1TF;


    StyledDocument doc = null;
    private JTextField addr2TF;
    private JCheckBox enaAddr1CHB;
    private JCheckBox enaAddr2CHB;


    private JScrollPane spClearAssembler;
    private JCheckBox enaCpuCHB;

    private JTable tableCpuRegs;

    private BreakpointTextLineNumber breakpointTLN;



    JCheckBox enaReadMemCHB, enaReadBuf1CHB, enaReadBuf2CHB;
    String [] columnNames = {"Name", "Value"};

    public  final int WIDTH_BUTTON = 90;
    public  final int HEIGHT_BUTTON = 20;

    public CpuDebugPanel(int numCpu) {
        this.numCpu = numCpu;
        Model.getParserModel().addParserEventListener(this, numCpu);
        Model.getMemoryModel().addLongProcessEventListener(this);
        setLayout(new BorderLayout());
        JPanel westPanel = new JPanel();
        westPanel.setLayout(new BoxLayout(westPanel, BoxLayout.Y_AXIS));
        westPanel.add(createControlPanel());
        westPanel.add(createCpuRegsPanel());
        add(westPanel, BorderLayout.WEST);

        paneClearAssembler = new JTextPane();

        breakpointTLN = new BreakpointTextLineNumber(paneClearAssembler, 4, this);
        spClearAssembler = new JScrollPane(paneClearAssembler, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        spClearAssembler.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        spClearAssembler.setRowHeaderView( breakpointTLN );

        Font font = paneClearAssembler.getFont();
        paneHexCodes = new JTextPane();
        paneHexCodes.setBackground(new Color(0xE8F4F8));
         paneHexCodes.setFont(new Font( font.getFamily(), font.getStyle(),  font_size));
        TextLineNumber tlnHEX = new TextLineNumber(paneHexCodes);
        JScrollPane spHexCodes = new JScrollPane(paneHexCodes, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        spHexCodes.getViewport().setBackground(new Color(0xE8F4F8));
        spHexCodes.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        spHexCodes.getVerticalScrollBar().setModel(spClearAssembler.getVerticalScrollBar().getModel());
        paneHexCodes.addMouseWheelListener(e -> {
            spClearAssembler.dispatchEvent(e);
        });
        spHexCodes.setRowHeaderView( tlnHEX );


        JSplitPane outputCodesPane1 = new JSplitPane();
        outputCodesPane1.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        outputCodesPane1.setDividerSize(8);
        //outputCodesPane1.setPreferredSize(new Dimension(getWidth(), 0));
        outputCodesPane1.setDividerLocation(0.88);
        outputCodesPane1.setOneTouchExpandable(true);
        outputCodesPane1.setResizeWeight(0.88);
        outputCodesPane1.setLeftComponent(spClearAssembler);
        outputCodesPane1.setRightComponent(spHexCodes);







        add( outputCodesPane1, BorderLayout.CENTER);

        add( new MemoryCpuRegs(numCpu), BorderLayout.EAST);
        Model.getMemoryModel().addMemoryEventListener(numCpu ==  1? RunnerView.DBG_CPU1_REGS : RunnerView.DBG_CPU2_REGS, this);

    }


    private void ensureParsed(){
        if(doc != null) return;
        try {
            if(!Model.getEditorModel().getCurrentFile().getName().contains("base_ram.txt")) {
                Model.getEditorModel().saveCurrentFile();
            }
            Thread t = Model.getParserModel().parseAllFiles();
            t.join(5000);
            Model.getEditorModel().updateColors();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void runCommand(int command){
        ensureParsed();
        Model.getMemoryModel().runDebuggerCommandAndRead(command, numCpu, Integer.parseInt(addr1TF.getText()), enaAddr1CHB.isSelected(), Integer.parseInt(addr2TF.getText()), enaAddr2CHB.isSelected(), enaReadMemCHB.isSelected(), enaReadBuf1CHB.isSelected(), enaReadBuf2CHB.isSelected());


    }

    private JPanel createControlPanel(){




        JPanel controlPanel = new JPanel();
        controlPanel.setBorder(BorderFactory.createTitledBorder("Control:"));
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        Action performResetCpu  = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(Model.getMemoryModel().isProcessing()) return;
                ensureParsed();
                Model.getMemoryModel().resetCpuWithRun(numCpu,
                        Integer.parseInt(addr1TF.getText()), enaAddr1CHB.isSelected(),
                        Integer.parseInt(addr2TF.getText()), enaAddr2CHB.isSelected(),
                        () -> updateBreakpointsFromHardware());
                storeProperties();
            }
        };


        Action performStop= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(Model.getMemoryModel().isProcessing()) return;
                runCommand(1);
                storeProperties();
            }
        };
        Action performRun= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(Model.getMemoryModel().isProcessing()) return;
                runCommand(0);
                storeProperties();
            }
        };
        Action performStep= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(Model.getMemoryModel().isProcessing()) return;
                runCommand(2);
                storeProperties();
            }
        };

        Action performRead= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(Model.getMemoryModel().isProcessing()) return;
                ensureParsed();
                Model.getMemoryModel().readCpuMemoryBufRegisters(numCpu, enaReadMemCHB.isSelected(), enaReadBuf1CHB.isSelected(), enaReadBuf2CHB.isSelected(), Integer.parseInt(addr2TF.getText()), enaAddr2CHB.isSelected(), () -> updateBreakpointsFromHardware());
                storeProperties();
            }
        };



        String addr1 = WorkstationConfig.getProperty(ConfCpuDebugProp.STOP_ADDR1_VALUE.name()+String.valueOf(numCpu));
        String addr2 = WorkstationConfig.getProperty(ConfCpuDebugProp.STOP_ADDR2_VALUE.name()+String.valueOf(numCpu));
        String addr1EnaStr = WorkstationConfig.getProperty(ConfCpuDebugProp.STOP_ADDR1_ENA.name()+String.valueOf(numCpu));
        String addr2EnaStr = WorkstationConfig.getProperty(ConfCpuDebugProp.STOP_ADDR2_ENA.name()+String.valueOf(numCpu));
        String enaReadMemStr = WorkstationConfig.getProperty(ConfCpuDebugProp.ENA_READ_MEM.name()+String.valueOf(numCpu));
        String enaReadBuffer1Str = WorkstationConfig.getProperty(ConfCpuDebugProp.ENA_READ_BUFFER1.name()+String.valueOf(numCpu));
        String enaReadBuffer2Str = WorkstationConfig.getProperty(ConfCpuDebugProp.ENA_READ_BUFFER2.name()+String.valueOf(numCpu));
        enaCpuCHB = new JCheckBox("");
        enaCpuCHB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!Model.getMemoryModel().isProcessing()) {
                    Model.getMemoryModel().setCpuEnabled(numCpu, enaCpuCHB.isSelected());
                }
            }
        });


        JPanel panel0chb = new JPanel();
        JButton btnReset = createButton( performResetCpu, "Reset", "reset all cpus");
        listDebugButtons.add(btnReset);
        panel0chb.add(btnReset);
        panel0chb.setLayout(new BoxLayout(panel0chb, BoxLayout.X_AXIS));
        panel0chb.add( new JLabel(String.format("CPU%s_en", String.valueOf(numCpu))));
        panel0chb.add( enaCpuCHB);

        controlPanel.add(panel0chb);


        //enaCpuCHB.setMinimumSize(new Dimension(40, 20));
        JPanel panel1buttons = new JPanel();
        panel1buttons.setLayout(new BoxLayout(panel1buttons, BoxLayout.X_AXIS));
        JButton btnStop = createButton( performStop, "  Stop ", "stop cpu" + String.valueOf(numCpu));
        JButton btnRun = createButton( performRun,  "  Run  ", "run cpu"  + String.valueOf(numCpu));
        listDebugButtons.add(btnStop);
        listDebugButtons.add(btnRun);
        panel1buttons.add(btnStop);
        panel1buttons.add(btnRun);
        controlPanel.add(panel1buttons);

        JPanel panel2buttons = new JPanel();
        panel2buttons.setLayout(new BoxLayout(panel2buttons, BoxLayout.X_AXIS));
        JButton btnStep = createButton( performStep, "  Step ", "step cpu" + String.valueOf(numCpu));
        JButton btnRead = createButton( performRead, "  Read ", "read from cpu" + String.valueOf(numCpu));
        listDebugButtons.add(btnStep);
        listDebugButtons.add(btnRead);
        panel2buttons.add(btnStep);
        panel2buttons.add(btnRead);
        controlPanel.add(panel2buttons);
        controlPanel.add(createAddressPanel(addr1TF = new JTextField(addr1 == null ? "0" : addr1), enaAddr1CHB = new JCheckBox("ena")));
        controlPanel.add(createAddressPanel(addr2TF = new JTextField(addr2 == null ? "0" : addr2), enaAddr2CHB = new JCheckBox("ena")));

        ActionListener enaAddrCHBListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                breakpointTLN.removeAllBreakpoints();
                if(!addr1TF.getText().equals("")){
                    if(enaAddr1CHB.isSelected()) {
                        breakpointTLN.setBreakpoint(Integer.parseInt(addr1TF.getText()));
                    }
                }
                if(!addr2TF.getText().equals("")){
                    if(enaAddr2CHB.isSelected()) {
                        breakpointTLN.setBreakpoint(Integer.parseInt(addr2TF.getText()));
                    }
                }
            }
        };


        enaAddr1CHB.addActionListener(enaAddrCHBListener);
        enaAddr2CHB.addActionListener(enaAddrCHBListener);
        JPanel panel3chb = new JPanel();
        panel3chb.setLayout(new BoxLayout(panel3chb, BoxLayout.X_AXIS));
        panel3chb.add( enaReadMemCHB = new JCheckBox("Mem"));
        panel3chb.add( enaReadBuf1CHB = new JCheckBox("Bf1"));
        panel3chb.add( enaReadBuf2CHB = new JCheckBox("Bf2"));
        controlPanel.add(panel3chb);

        if(addr1EnaStr !=null){
            enaAddr1CHB.setSelected(Boolean.parseBoolean(addr1EnaStr));
        }
        if(addr2EnaStr !=null){
            enaAddr2CHB.setSelected(Boolean.parseBoolean(addr2EnaStr));
        }
        if(enaReadMemStr != null){
            enaReadMemCHB.setSelected(Boolean.parseBoolean(enaReadMemStr));
        }
        if(enaReadBuffer1Str != null){
            enaReadBuf1CHB.setSelected(Boolean.parseBoolean(enaReadBuffer1Str));
        }
        if(enaReadBuffer2Str != null){
            enaReadBuf2CHB.setSelected(Boolean.parseBoolean(enaReadBuffer2Str));
        }
        return  controlPanel;

    }


    private JPanel createCpuRegsPanel(){
        JPanel cpusRegsPanel = new JPanel();
        cpusRegsPanel.setBorder(BorderFactory.createTitledBorder("CPU regs:"));
        cpusRegsPanel.setLayout(new BoxLayout(cpusRegsPanel, BoxLayout.Y_AXIS));
        cpusRegsPanel.add(createTableCpuRegsScrollPane());

        return  cpusRegsPanel;
    }




    @Override
    public void updateDebuggerCpuView(String assemblyCodes, String hexCodes) {

        try {
            doc = AsmEditor.getColorDocument(assemblyCodes);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }


        if(doc != null) {
            paneClearAssembler.setDocument(doc);
            paneClearAssembler.setEditable(false);
            paneClearAssembler.setBackground(AsmEditor.editorBg);
        }
        paneHexCodes.setText(hexCodes);
        paneHexCodes.setEditable(false);
        breakpointTLN.repaint();
    }


    private void highlightLine(int numLine){
        String text = "";
        breakpointTLN.setCurDebugStopNumLine(numLine);
        try {


            text = doc.getText(0, doc.getLength());
            paneClearAssembler.setStyledDocument( doc =  AsmEditor.getColorDocument(text, numLine));
            int lineHeight = paneClearAssembler.getFontMetrics(paneClearAssembler.getFont()).getHeight();
            JScrollBar vertical =  spClearAssembler.getVerticalScrollBar();

            int currentYmin = vertical.getValue();
            int currentYmax = currentYmin + vertical.getHeight() - 2*(lineHeight-1) ;
            int lineY =  ( numLine - 1) * (lineHeight-1);
            if(lineY < currentYmin || lineY > currentYmax){
                int windowNumLineSize = vertical.getHeight()/(lineHeight-1);
                int demiWindow = windowNumLineSize/4;
                int x = ( numLine- demiWindow  - 1) * (lineHeight-1);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                vertical.setValue( x );

            }

        } catch (BadLocationException e) {
            logger.error("Error in highlightLine", e);
            throw new RuntimeException(e);
        }





    }


    private class MemoryCpuRegs extends JPanel implements IMemoryEventListener {

        private String [] memColumnNames = {"Addr", "  0  ", "  1  ","2","3","4","5","6", "7"};
        private JTable tableCpuMemory;
        DefaultTableCellRenderer centerRenderer;
        public MemoryCpuRegs(int numCpu) {
            setLayout(new BorderLayout());
            add(createTableCpuMemoryScrollPane());
            Model.getMemoryModel().addMemoryEventListener(numCpu == 1 ? RunnerView.DBG_CPU1_MEM : RunnerView.DBG_CPU2_MEM, this );
        }


        private JScrollPane createTableCpuMemoryScrollPane(){


            DefaultTableModel tableModel = new DefaultTableModel(memColumnNames,0);
            tableCpuMemory = new JTable(tableModel);
            tableCpuMemory.setShowGrid(true);
            tableCpuMemory.setGridColor(new java.awt.Color(0x6A8AAC));

            JScrollPane scrollPaneTable = new JScrollPane(tableCpuMemory);

            scrollPaneTable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment( JLabel.CENTER );

            for(int i = 0; i<=32; i++){
                String[]row = new String [9];
                row[0] = String.valueOf(i*8);
                tableModel.insertRow(i, row);
            }
            tableCpuMemory.getColumnModel().getColumn(0).setCellRenderer( centerRenderer );
            tableCpuMemory.setModel(tableModel);
            tableCpuMemory.setMinimumSize(new Dimension(WIDTH_BUTTON*9, 180));
            return scrollPaneTable;
        }
        @Override
        public void updateValues() {
            List<Integer> list = Model.getMemoryModel().getRespValues();
            if (list.size() < 256) return;
            DefaultTableModel tableModel = (DefaultTableModel) tableCpuMemory.getModel();
            for(int i = 0; i<=31; i++){
                for(int k = 0; k<=7; k++ ){
                    tableModel.setValueAt(Integer.toHexString((1<<16) +  list.get(i*8 + k)).substring(1), i, 1+k);
                }
            }
        }

        @Override
        public void updateTableValue(RegPanel panel) {

        }


    }





    private  JPanel createAddressPanel(JTextField textField, JCheckBox checkBox){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        textField.setMaximumSize(new Dimension(40, 24));
        panel.add(textField);
        panel.add(checkBox);
        return panel;
    }
    private  JButton createButton( Action action, String name, String toolTipText  ){
        JButton button  = new JButton(action);
        button.setText(name);
        button.setToolTipText(toolTipText);
        //button.setPreferredSize(new Dimension(WIDTH_BUTTON,HEIGHT_BUTTON));
        button.setMargin(new Insets(2,2,2,2));

        button.setMinimumSize(new Dimension(WIDTH_BUTTON,HEIGHT_BUTTON));
        return button;
    }

    private void storeProperties(){
        WorkstationConfig.setProperty(ConfCpuDebugProp.STOP_ADDR1_VALUE.name()+String.valueOf(numCpu), addr1TF.getText());
        WorkstationConfig.setProperty(ConfCpuDebugProp.STOP_ADDR2_VALUE.name()+String.valueOf(numCpu), addr2TF.getText());
        WorkstationConfig.setProperty(ConfCpuDebugProp.STOP_ADDR1_ENA.name()+String.valueOf(numCpu), String.valueOf(enaAddr1CHB.isSelected()));
        WorkstationConfig.setProperty(ConfCpuDebugProp.STOP_ADDR2_ENA.name()+String.valueOf(numCpu), String.valueOf(enaAddr2CHB.isSelected()));
        WorkstationConfig.setProperty(ConfCpuDebugProp.ENA_READ_MEM.name()+String.valueOf(numCpu), String.valueOf(enaReadMemCHB.isSelected()));
        WorkstationConfig.setProperty(ConfCpuDebugProp.ENA_READ_BUFFER1.name()+String.valueOf(numCpu), String.valueOf(enaReadBuf1CHB.isSelected()));
        WorkstationConfig.setProperty(ConfCpuDebugProp.ENA_READ_BUFFER2.name()+String.valueOf(numCpu), String.valueOf(enaReadBuf2CHB.isSelected()));
    }

    private JScrollPane createTableCpuRegsScrollPane(){



        String [] columnNames = {"Name", "Value"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames,0);
        tableCpuRegs = new JTable(tableModel);
        tableCpuRegs.setShowGrid(true);
        tableCpuRegs.setGridColor(new java.awt.Color(0x6A8AAC));
        tableCpuRegs.setRowHeight(17);

        JScrollPane scrollPaneTable = new JScrollPane(tableCpuRegs);
        scrollPaneTable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);


        int i = 0;

        for(DebugCpuRegs cpuRegs : DebugCpuRegs.values()){
            String[]row = new String [2];
            row[0] = cpuRegs.name();
            row[1] = "";
            tableModel.insertRow(i, row);
            i++;
        }
        tableCpuRegs.setModel(tableModel);

        int tableHeight = tableCpuRegs.getRowHeight() * tableModel.getRowCount() + tableCpuRegs.getTableHeader().getPreferredSize().height;
        scrollPaneTable.setPreferredSize(new Dimension(WIDTH_BUTTON, tableHeight));
        tableCpuRegs.setMinimumSize(new Dimension(WIDTH_BUTTON, tableHeight));
        return scrollPaneTable;
    }

    public void updateBreakpointAddresses(LinkedList<  Integer >breakpoints){
        boolean curBreakpoint = false;
        if(breakpoints.size()==0){
            enaAddr1CHB.setSelected(false);
            enaAddr2CHB.setSelected(false);
        }
        else {
            if(!addr2TF.getText().equals("") && Integer.parseInt(addr2TF.getText()) == breakpoints.getFirst()){
                enaAddr2CHB.setSelected(true);
                curBreakpoint = true;
            } else {
                enaAddr1CHB.setSelected(true);
                addr1TF.setText(String.valueOf(breakpoints.getFirst()));
            }
            if(breakpoints.size() ==2 ){
                if(curBreakpoint){
                    enaAddr1CHB.setSelected(true);
                    addr1TF.setText(String.valueOf(breakpoints.getLast()));
                } else {
                    enaAddr2CHB.setSelected(true);
                    addr2TF.setText(String.valueOf(breakpoints.getLast()));
                }
            } else {
                if(curBreakpoint) {
                    enaAddr1CHB.setSelected(false);
                } else {
                    enaAddr2CHB.setSelected(false);
                }
            }
        }
        if(!Model.getMemoryModel().isProcessing()) {
            ensureParsed();
            Model.getMemoryModel().readCpuMemoryBufRegisters(numCpu, enaReadMemCHB.isSelected(), enaReadBuf1CHB.isSelected(), enaReadBuf2CHB.isSelected(), Integer.parseInt(addr2TF.getText()), enaAddr2CHB.isSelected());
        }
    }

    private void updateBreakpointsFromHardware() {
        MemoryModel mm = Model.getMemoryModel();
        addr1TF.setText(String.valueOf(mm.getDbgHwAddr1()));
        enaAddr1CHB.setSelected(mm.getDbgHwEna1());
        addr2TF.setText(String.valueOf(mm.getDbgHwAddr2()));
        enaAddr2CHB.setSelected(mm.getDbgHwEna2());
        breakpointTLN.removeAllBreakpoints();
        if (mm.getDbgHwEna1()) {
            breakpointTLN.setBreakpoint(mm.getDbgHwAddr1());
        }
        if (mm.getDbgHwEna2()) {
            breakpointTLN.setBreakpoint(mm.getDbgHwAddr2());
        }
    }

    @Override
    public void updateValues() {
        List<Integer> list = Model.getMemoryModel().getRespValues();
        if (list.isEmpty()) return;
        Map<DebugCpuRegs, Long> result = new HashMap<>();
        for(DebugCpuRegs cpuReg : DebugCpuRegs.values()){
            long value = 0;
            if(cpuReg.getAddrs().size() == 1 ){
                value = (cpuReg.getMask() & list.get(cpuReg.getAddrs().get(0))) >> cpuReg.getLsb();
            } else {
                for(int i = 0; i< cpuReg.getAddrs().size(); i++){
                    long value14 = ( 0x3FFF & list.get(cpuReg.getAddrs().get(i))) >> cpuReg.getLsb();
                    value = value + (value14 << (14*i));
                }
            }
            if(cpuReg.isSigned()){
                if(value >> (cpuReg.getMsb() - cpuReg.getLsb()) != 0){
                    long l1 =( 1l << (cpuReg.getMsb() - cpuReg.getLsb()) + 1);
                    long l2 = -(l1 - value);
                    value = l2;
                }

            }
            result.put(cpuReg, value);
        }

        DefaultTableModel tableModel = (DefaultTableModel) tableCpuRegs.getModel();
        int i = 0;

        for(DebugCpuRegs cpuReg : DebugCpuRegs.values()){
            if(cpuReg.equals(DebugCpuRegs.pc)){
                highlightLine(result.get(cpuReg).intValue());
            }
            if(!cpuReg.equals(DebugCpuRegs.cur_com)) {
                tableModel.setValueAt(Long.toString(result.get(cpuReg)), i, 1);
            } else {
                tableModel.setValueAt(Long.toHexString(result.get(cpuReg)), i, 1);
            }
            i++;
        }
        breakpointTLN.repaint();
    }

    @Override
    public void updateStatusOfProcessing() {
        boolean isProcessing = Model.getMemoryModel().isProcessing();
        for(JButton button : listDebugButtons){
            button.setEnabled(!isProcessing);
        }
    }


    @Override
    public void updateTableValue(RegPanel panel) {

    }
}
