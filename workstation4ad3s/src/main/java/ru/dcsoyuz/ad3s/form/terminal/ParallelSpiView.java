package ru.dcsoyuz.ad3s.form.terminal;


import org.apache.commons.lang3.ArrayUtils;
import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import ru.dcsoyuz.ad3s.form.RegPanel;
import ru.dcsoyuz.ad3s.form.editor.ILongProcessEventListener;
import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.AddrCpuHand;
import ru.dcsoyuz.ad3s.model.uart.RunnerView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;


/**
 * Created by yuri.filatov on 11.08.2016.
 */
public class ParallelSpiView extends JPanel implements IGraphViewListener, ILongProcessEventListener, IMemoryEventListener {

    XYChart chart1;
    XYChart chart2;

    private final int WIDTH_BUTTON = 80;
    private final int HEIGHT_BUTTON = 20;

    private JComboBox<AddrItem> address1Combo;
    private JComboBox<AddrItem> address2Combo;
    private boolean addressChanged = false;
    private JCheckBox triggeredCHB;
    private JComboBox<String> triggerSignalCombo;

    private JCheckBox vcCHB;
    private JCheckBox sdiCHB;
    private JCheckBox scaleCHB;
    private JTextField yRangeTF;
    XChartPanel<XYChart> xyPanel1;
    XChartPanel<XYChart> xyPanel2;
    JButton loadingDataButton;
    private JButton onOffButton;

    ParallelSpiView parallelSpiView;
    boolean isRunGraphRefreshing = false;

    private int cursorIndex = -1;
    private CursorPanel cursorPanel1;
    private CursorPanel cursorPanel2;
    private javax.swing.Timer renderTimer;


    public ParallelSpiView(){

        parallelSpiView = this;
        Action performRunGetData= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(!Model.getMemoryModel().isProcessing()){
                    Model.getMemoryModel().setIGraphViewListener(parallelSpiView);
                    Model.getMemoryModel().setDmaQuadValue(true);
                    isRunGraphRefreshing = true;
                } else {
                    if(loadingDataButton.getText().equals("Loading...")) {
                        System.out.println(" Before stop another loading process");
                    }
                    Model.getMemoryModel().setDmaQuadValue(false);
                    Model.getMemoryModel().stopLongProcess();
                }
            }
        };

        AddrCpuHand[] addrEnums = AddrCpuHand.values();
        address1Combo = new JComboBox<>();
        address2Combo = new JComboBox<>();
        for (AddrCpuHand a : addrEnums) {
            List<HandValueSetting> signals = HandValueSetting.getHandValueSettings(a);
            String sigNames = signals.stream().map(HandValueSetting::name).collect(java.util.stream.Collectors.joining(", "));
            String label = a.getAddr() + " - " + sigNames;
            address1Combo.addItem(new AddrItem(a.getAddr(), label));
            address2Combo.addItem(new AddrItem(a.getAddr(), label));
        }
        address1Combo.setSelectedIndex(1);
        address2Combo.setSelectedIndex(2);
        address1Combo.addActionListener(e -> addressChanged = true);
        address2Combo.addActionListener(e -> addressChanged = true);
        triggeredCHB = new JCheckBox("Trig:");
        triggerSignalCombo = new JComboBox<>();
        triggerSignalCombo.setPreferredSize(new Dimension(160, 20));
        triggerSignalCombo.setEnabled(false);
        triggeredCHB.addActionListener(e -> {
            triggerSignalCombo.setEnabled(triggeredCHB.isSelected());
        });

        vcCHB = new JCheckBox("VC:");
        vcCHB.addActionListener(e -> {
            Model.getMemoryModel().setVcValue(vcCHB.isSelected());
        });
        sdiCHB = new JCheckBox("SDI:");
        sdiCHB.addActionListener(e -> {
            Model.getMemoryModel().setSdiValue(sdiCHB.isSelected());
        });

        scaleCHB = new JCheckBox("Scale:");
        scaleCHB.setSelected(true);
        yRangeTF = new JTextField(5);
        yRangeTF.setMaximumSize(new Dimension(60, 20));
        yRangeTF.setEnabled(false);
        yRangeTF.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyYRange(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyYRange(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyYRange(); }
            private void applyYRange() {
                if (scaleCHB.isSelected()) return;
                SwingUtilities.invokeLater(() -> {
                    String text = yRangeTF.getText().trim();
                    if (text.isEmpty()) {
                        chart1.getStyler().setYAxisMin((Double) null);
                        chart1.getStyler().setYAxisMax((Double) null);
                    } else {
                        try {
                            int val = Integer.parseInt(text);
                            if (val > 0) {
                                chart1.getStyler().setYAxisMin((double) -val);
                                chart1.getStyler().setYAxisMax((double) val);
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                    xyPanel1.revalidate(); xyPanel1.repaint();
                });
            }
        });
        scaleCHB.addActionListener(e -> {
            yRangeTF.setEnabled(!scaleCHB.isSelected());
            if (scaleCHB.isSelected()) {
                chart1.getStyler().setYAxisMin((Double) null);
                chart1.getStyler().setYAxisMax((Double) null);
            } else {
                String text = yRangeTF.getText().trim();
                if (!text.isEmpty()) {
                    try {
                        int val = Integer.parseInt(text);
                        if (val > 0) {
                            chart1.getStyler().setYAxisMin((double) -val);
                            chart1.getStyler().setYAxisMax((double) val);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            xyPanel1.revalidate(); xyPanel1.repaint();
        });

        loadingDataButton = createButton(performRunGetData, "Loading...", "run/stop loading data");

        double[][] yData = new double[2][2];
        String [] seriesNames = {"sin", "cos"};
        chart1 = QuickChart.getChart("Values", "Time", "Value", seriesNames, new double[] { 0,0  }, yData);
        chart1.getStyler().setLegendVisible(true);
        chart1.getStyler().setAxisTitlesVisible(false);
        chart1.getStyler().setChartTitleVisible(false);
        chart1.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart1.getStyler().setXAxisTicksVisible(false);
        chart1.getStyler().setYAxisLeftWidthHint(50);
        chart1.getStyler().setChartBackgroundColor(new Color(0xE8F0FA));
        chart1.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart1.getStyler().setPlotGridLinesVisible(true);
        chart1.getStyler().setPlotGridHorizontalLinesVisible(true);
        chart1.getStyler().setPlotGridVerticalLinesVisible(false);
        chart1.getStyler().setPlotBorderColor(new Color(0xC0D0E0));
        chart1.getStyler().setLegendBackgroundColor(new Color(0xE8F0FA));

        String [] seriesNames2 = {"exi", "exi_recovered"};
        chart2 = QuickChart.getChart("Signals", "Time", "Value", seriesNames2, new double[] { 0, 0  }, yData);
        chart2.getStyler().setLegendVisible(true);
        chart2.getStyler().setChartTitleVisible(false);
        chart2.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart2.getStyler().setXAxisTicksVisible(false);
        chart2.getStyler().setAxisTitlesVisible(false);
        chart2.getStyler().setYAxisLeftWidthHint(50);
        chart2.getStyler().setChartBackgroundColor(new Color(0xE8F0FA));
        chart2.getStyler().setPlotBackgroundColor(Color.WHITE);
        chart2.getStyler().setPlotGridLinesVisible(true);
        chart2.getStyler().setPlotGridHorizontalLinesVisible(true);
        chart2.getStyler().setPlotGridVerticalLinesVisible(false);
        chart2.getStyler().setPlotBorderColor(new Color(0xC0D0E0));
        chart2.getStyler().setLegendBackgroundColor(new Color(0xE8F0FA));

        xyPanel1 = new XChartPanel(chart1);
        xyPanel2 = new XChartPanel(chart2);

        cursorPanel1 = new CursorPanel(xyPanel1, chart1, true);
        cursorPanel2 = new CursorPanel(xyPanel2, chart2, false);

        setLayout(new GridBagLayout());

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));

        onOffButton = new JButton("ON");
        onOffButton.setFocusable(false);
        onOffButton.setMargin(new Insets(0, 2, 0, 2));
        onOffButton.setToolTipText("<html>ON:<br>"
                + "&nbsp;&nbsp;1. C1 ResCntrl: Enc_en=0, SPI_ext_en=1, Vel_from_cpu=1, Coord_from_cpu=1<br>"
                + "&nbsp;&nbsp;2. C2 ResCntrl: Enc_en=0, SPI_ext_en=1, Vel_from_cpu=1, Coord_from_cpu=1<br>"
                + "&nbsp;&nbsp;3. Отключить CPU1 (Mode_config)<br>"
                + "&nbsp;&nbsp;4. Загрузить dma_asm/cpu1_data.hex по адресу 512<br>"
                + "&nbsp;&nbsp;5. Включить CPU1 (Mode_config)<br>"
                + "OFF:<br>"
                + "&nbsp;&nbsp;1. C1 ResCntrl: очистить SPI_ext_en, Vel_from_cpu, Coord_from_cpu<br>"
                + "&nbsp;&nbsp;2. C2 ResCntrl: очистить SPI_ext_en, Vel_from_cpu, Coord_from_cpu<br>"
                + "&nbsp;&nbsp;3. Отключить CPU1 (Mode_config)</html>");
        onOffButton.addActionListener(e -> {
            boolean isOn = "ON".equals(onOffButton.getText());
            if (isOn) {
                onOffButton.setEnabled(false);
                Model.getMemoryModel().enableDmaQuadAnalyzer(() -> SwingUtilities.invokeLater(() -> {
                    onOffButton.setText("OFF");
                    onOffButton.setEnabled(true);
                    loadingDataButton.setEnabled(true);
                }), () -> SwingUtilities.invokeLater(() -> {
                    onOffButton.setEnabled(true);
                }));
            } else {
                onOffButton.setText("ON");
                loadingDataButton.setEnabled(false);
                Model.getMemoryModel().disableDmaQuadAnalyzer();
            }
        });

        controlsPanel.add(onOffButton);
        controlsPanel.add(new JLabel("Addr1:"));
        address1Combo.setPreferredSize(new Dimension(60, 20));
        controlsPanel.add(address1Combo);
        controlsPanel.add(new JLabel("Addr2:"));
        address2Combo.setPreferredSize(new Dimension(60, 20));
        controlsPanel.add(address2Combo);
        controlsPanel.add(vcCHB);
        controlsPanel.add(sdiCHB);
        controlsPanel.add(loadingDataButton);
        controlsPanel.add(triggeredCHB);
        controlsPanel.add(triggerSignalCombo);
        controlsPanel.add(scaleCHB);
        controlsPanel.add(yRangeTF);

        loadingDataButton.setEnabled(false);

        renderTimer = new javax.swing.Timer(40, e -> {
            if(Model.getMemoryModel().isDmaQuadResponseReady()){
                byte[] response = Model.getMemoryModel().consumeDmaQuadResponse();
                if(response != null) {
                    renderFromResponse(response);
                }
            }
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        gbc.weighty = 1.0;
        gbc.gridy = 0;
        add(cursorPanel1, gbc);

        gbc.weighty = 0;
        gbc.gridy = 1;
        add(controlsPanel, gbc);

        gbc.weighty = 1.0;
        gbc.gridy = 2;
        add(cursorPanel2, gbc);

        Model.getMemoryModel().addMemoryEventListener(RunnerView.DMATAP_VIEW, this);
        Model.getMemoryModel().addLongProcessEventListener(this);
    }


    private void repaintCursor() {
        cursorPanel1.repaint();
        cursorPanel2.repaint();
    }


    private JButton createButton(Action action, String name, String toolTipText  ){
        JButton button  = new JButton(action);
        button.setText(name);
        button.setToolTipText(toolTipText);
        button.setPreferredSize(new Dimension(WIDTH_BUTTON,HEIGHT_BUTTON));
        button.setMinimumSize(new Dimension(WIDTH_BUTTON,HEIGHT_BUTTON));
        button.setMargin(new Insets(2,2,2,2));

        return button;

    }

    @Override
    public void updateStatusOfProcessing() {
        if(Model.getMemoryModel().isProcessing()){
            loadingDataButton.setText("Stop");
            renderTimer.start();
            onOffButton.setEnabled(false);
        } else {
            loadingDataButton.setText("Loading...");
            renderTimer.stop();
            isRunGraphRefreshing = false;
            onOffButton.setEnabled(true);
        }
    }

    @Override
    public List<Integer> getListAddresses() {
        List<Integer> list = new ArrayList<>();
        int address1 = ((AddrItem) address1Combo.getSelectedItem()).addr;
        int address2 = ((AddrItem) address2Combo.getSelectedItem()).addr;
        int address = address2 << 8 | address1;
        if(triggeredCHB.isSelected()){
            //address = address | 0x80;
        }
        list.add(address);
        return list;
    }
    @Override
    public void updateValues() {
        byte[] response = Model.getUartModel().getResponse();
        LinkedList<Byte> list = new LinkedList<>(Arrays.asList(ArrayUtils.toObject(response)));
        for(int i = 0; i<=7; i++) {
            list.removeFirst();
        }
        list.removeLast();
        LinkedList<Byte> listX2 = new LinkedList<>();
        for(byte b : list){
            listX2.add((byte)(0x0F & (b>>4)));
            listX2.add((byte)(0x0F & b));
        }
        List<List<Integer>> data = new ArrayList<>();
        for(int i=0; i<=3; i++){
            data.add(new ArrayList<>());
        }
        if(listX2.size()%16 !=0){
            System.out.println("Response dma length don't multiply by 8");
        }

        for(int i=0; i< listX2.size()/16; i++){
            int a=0,b=0,c=0,d=0;
            for(int k=0; k<=15;k++ ){
                byte byteVal = listX2.get(i*16 + k);
                a = a| ((byteVal&0x01)<<(15-k));
                b = b| (((byteVal&0x02)>>1)<<(15-k));
                c = c| (((byteVal&0x04)>>2)<<(15-k));
                d = d| (((byteVal&0x08)>>3)<<(15-k));
            }
            data.get(0).add(a);
            data.get(1).add(b);
            data.get(2).add(c);
            data.get(3).add(d);
        }

        System.out.println("OK");

        Map<String, double [] > mapValuesNumber = new HashMap<>();
        Map<String, double [] > mapValues1BitSignal = new HashMap<>();

        int combinedAddress = Model.getMemoryModel().getRespAddress();
        int addressA = ((AddrItem) address1Combo.getSelectedItem()).addr;
        int addressB = ((AddrItem) address2Combo.getSelectedItem()).addr;

        if(vcCHB.isSelected()){
            addDataToMaps(data.get(3), data.get(1), mapValuesNumber, mapValues1BitSignal, addressA);
            addDataToMaps(data.get(2), data.get(0), mapValuesNumber, mapValues1BitSignal, addressB);
        } else {
            addDataToMaps(data.get(2), data.get(0), mapValuesNumber, mapValues1BitSignal, addressA);
            addDataToMaps(data.get(3), data.get(1), mapValuesNumber, mapValues1BitSignal, addressB);
        }
        // Update trigger signal combo
        Set<String> signalNames = mapValues1BitSignal.keySet();
        DefaultComboBoxModel<String> comboModel = (DefaultComboBoxModel<String>) triggerSignalCombo.getModel();
        boolean needsUpdate = comboModel.getSize() != signalNames.size();
        if (!needsUpdate) {
            for (int i = 0; i < comboModel.getSize(); i++) {
                if (!signalNames.contains(comboModel.getElementAt(i))) { needsUpdate = true; break; }
            }
        }
        if (needsUpdate) {
            String prev = (String) triggerSignalCombo.getSelectedItem();
            comboModel.removeAllElements();
            for (String name : signalNames) { comboModel.addElement(name); }
            if (prev != null) { comboModel.setSelectedItem(prev); }
        }
        // Auto-select trigger signal when address changed
        if (addressChanged) {
            addressChanged = false;
            if (triggeredCHB.isSelected()) {
                // Trig ON: always auto-select first signal for sync
                if (comboModel.getSize() > 0) {
                    comboModel.setSelectedItem(comboModel.getElementAt(0));
                }
            } else if (!signalNames.isEmpty()) {
                // Trig OFF: auto-select only if 1-bit signals exist
                comboModel.setSelectedItem(comboModel.getElementAt(0));
            }
        }
        // Apply trigger slicing
        if (triggeredCHB.isSelected() && triggerSignalCombo.getSelectedItem() != null) {
            String trigSignal = (String) triggerSignalCombo.getSelectedItem();
            double[] trigData = mapValues1BitSignal.get(trigSignal);
            if (trigData != null && trigData.length > 1) {
                int trigIdx = -1;
                for (int j = 0; j < trigData.length - 1; j++) {
                    int raw0 = (int) trigData[j] % 2;
                    int raw1 = (int) trigData[j + 1] % 2;
                    if (raw0 == 0 && raw1 == 1) { trigIdx = j + 1; break; }
                }
                if (trigIdx > 0) {
                    Map<String, double[]> sliced = new HashMap<>();
                    for (Map.Entry<String, double[]> e : mapValuesNumber.entrySet()) {
                        double[] a = e.getValue();
                        sliced.put(e.getKey(), (a != null && trigIdx < a.length)
                                ? Arrays.copyOfRange(a, trigIdx, a.length) : a);
                    }
                    mapValuesNumber.clear(); mapValuesNumber.putAll(sliced);
                    sliced.clear();
                    for (Map.Entry<String, double[]> e : mapValues1BitSignal.entrySet()) {
                        double[] a = e.getValue();
                        sliced.put(e.getKey(), (a != null && trigIdx < a.length)
                                ? Arrays.copyOfRange(a, trigIdx, a.length) : a);
                    }
                    mapValues1BitSignal.clear(); mapValues1BitSignal.putAll(sliced);
                }
            }
        }
        setDataToChart(xyPanel1.getChart(),mapValuesNumber );
        setDataToChart(xyPanel2.getChart(),mapValues1BitSignal );
        xyPanel1.revalidate();
        xyPanel2.revalidate();
        xyPanel2.repaint();
        xyPanel1.repaint();
        repaintCursor();
    }

    private void renderFromResponse(byte[] response) {
        LinkedList<Byte> list = new LinkedList<>(Arrays.asList(ArrayUtils.toObject(response)));
        for(int i = 0; i<=7; i++) {
            list.removeFirst();
        }
        list.removeLast();
        LinkedList<Byte> listX2 = new LinkedList<>();
        for(byte b : list){
            listX2.add((byte)(0x0F & (b>>4)));
            listX2.add((byte)(0x0F & b));
        }
        List<List<Integer>> data = new ArrayList<>();
        for(int i=0; i<=3; i++){
            data.add(new ArrayList<>());
        }
        if(listX2.size()%16 !=0){
            System.out.println("Response dma length don't multiply by 8");
        }
        for(int i=0; i< listX2.size()/16; i++){
            int a=0,b=0,c=0,d=0;
            for(int k=0; k<=15;k++ ){
                byte byteVal = listX2.get(i*16 + k);
                a = a| ((byteVal&0x01)<<(15-k));
                b = b| (((byteVal&0x02)>>1)<<(15-k));
                c = c| (((byteVal&0x04)>>2)<<(15-k));
                d = d| (((byteVal&0x08)>>3)<<(15-k));
            }
            data.get(0).add(a);
            data.get(1).add(b);
            data.get(2).add(c);
            data.get(3).add(d);
        }

        Map<String, double [] > mapValuesNumber = new HashMap<>();
        Map<String, double [] > mapValues1BitSignal = new HashMap<>();

        int addressA = ((AddrItem) address1Combo.getSelectedItem()).addr;
        int addressB = ((AddrItem) address2Combo.getSelectedItem()).addr;

        if(vcCHB.isSelected()){
            addDataToMaps(data.get(3), data.get(1), mapValuesNumber, mapValues1BitSignal, addressA);
            addDataToMaps(data.get(2), data.get(0), mapValuesNumber, mapValues1BitSignal, addressB);
        } else {
            addDataToMaps(data.get(2), data.get(0), mapValuesNumber, mapValues1BitSignal, addressA);
            addDataToMaps(data.get(3), data.get(1), mapValuesNumber, mapValues1BitSignal, addressB);
        }
        // Update trigger signal combo
        Set<String> signalNames = mapValues1BitSignal.keySet();
        DefaultComboBoxModel<String> comboModel = (DefaultComboBoxModel<String>) triggerSignalCombo.getModel();
        boolean needsUpdate = comboModel.getSize() != signalNames.size();
        if (!needsUpdate) {
            for (int i = 0; i < comboModel.getSize(); i++) {
                if (!signalNames.contains(comboModel.getElementAt(i))) { needsUpdate = true; break; }
            }
        }
        if (needsUpdate) {
            String prev = (String) triggerSignalCombo.getSelectedItem();
            comboModel.removeAllElements();
            for (String name : signalNames) { comboModel.addElement(name); }
            if (prev != null) { comboModel.setSelectedItem(prev); }
        }
        // Auto-select trigger signal when address changed
        if (addressChanged) {
            addressChanged = false;
            if (triggeredCHB.isSelected()) {
                // Trig ON: always auto-select first signal for sync
                if (comboModel.getSize() > 0) {
                    comboModel.setSelectedItem(comboModel.getElementAt(0));
                }
            } else if (!signalNames.isEmpty()) {
                // Trig OFF: auto-select only if 1-bit signals exist
                comboModel.setSelectedItem(comboModel.getElementAt(0));
            }
        }
        // Apply trigger slicing
        if (triggeredCHB.isSelected() && triggerSignalCombo.getSelectedItem() != null) {
            String trigSignal = (String) triggerSignalCombo.getSelectedItem();
            double[] trigData = mapValues1BitSignal.get(trigSignal);
            if (trigData != null && trigData.length > 1) {
                int trigIdx = -1;
                for (int j = 0; j < trigData.length - 1; j++) {
                    int raw0 = (int) trigData[j] % 2;
                    int raw1 = (int) trigData[j + 1] % 2;
                    if (raw0 == 0 && raw1 == 1) { trigIdx = j + 1; break; }
                }
                if (trigIdx > 0) {
                    int origLen = trigData.length;
                    Map<String, double[]> sliced = new HashMap<>();
                    for (Map.Entry<String, double[]> e : mapValuesNumber.entrySet()) {
                        double[] a = e.getValue();
                        if (a != null && trigIdx < a.length) {
                            double[] padded = new double[origLen];
                            Arrays.fill(padded, Double.NaN);
                            System.arraycopy(a, trigIdx, padded, 0, Math.min(a.length - trigIdx, origLen));
                            sliced.put(e.getKey(), padded);
                        } else {
                            sliced.put(e.getKey(), a);
                        }
                    }
                    mapValuesNumber.clear(); mapValuesNumber.putAll(sliced);
                    sliced.clear();
                    for (Map.Entry<String, double[]> e : mapValues1BitSignal.entrySet()) {
                        double[] a = e.getValue();
                        if (a != null && trigIdx < a.length) {
                            double[] padded = new double[origLen];
                            Arrays.fill(padded, Double.NaN);
                            System.arraycopy(a, trigIdx, padded, 0, Math.min(a.length - trigIdx, origLen));
                            sliced.put(e.getKey(), padded);
                        } else {
                            sliced.put(e.getKey(), a);
                        }
                    }
                    mapValues1BitSignal.clear(); mapValues1BitSignal.putAll(sliced);
                }
            }
        }
        setDataToChart(xyPanel1.getChart(), mapValuesNumber);
        setDataToChart(xyPanel2.getChart(), mapValues1BitSignal);
        updateYRangeFromData(mapValuesNumber);
        xyPanel1.revalidate();
        xyPanel2.revalidate();
        xyPanel2.repaint();
        xyPanel1.repaint();
        repaintCursor();
    }

    private void updateYRangeFromData(Map<String, double[]> mapValuesNumber) {
        if (!scaleCHB.isSelected()) return;
        double maxAbs = 0;
        for (double[] arr : mapValuesNumber.values()) {
            if (arr == null) continue;
            for (double v : arr) {
                if (!Double.isNaN(v)) {
                    double abs = Math.abs(v);
                    if (abs > maxAbs) maxAbs = abs;
                }
            }
        }
        if (maxAbs > 0) {
            int rounded = ((int) Math.ceil(maxAbs / 100.0)) * 100;
            yRangeTF.setText(String.valueOf(rounded));
        }
    }

    private void addDataToMaps(List<Integer> list1, List<Integer> list2,  Map<String, double [] > mapValuesNumber, Map<String, double [] > mapValues1BitSignal, int address){
        List <Integer> list28bValues = covert1bbTo28b(list1, list2);
        AddrCpuHand addrDef = AddrCpuHand.getCpuHandAddrDef(address);
        List<HandValueSetting> handValueSettings = HandValueSetting.getHandValueSettings(addrDef);
        int dobavka = 0;
        for(HandValueSetting setting : handValueSettings ){
            if(setting.getLsb() == setting.getMsb()){
                mapValues1BitSignal.put(setting.name(), getData(setting, list28bValues, list28bValues.size(),(dobavka=dobavka+2)));
            } else {
                mapValuesNumber.put(setting.name(), getData(setting, list28bValues, list28bValues.size(),0 ));
            }
        }
    }

    private void setDataToChart(XYChart xyChart, Map<String, double [] > values){
        Map<String, double[]> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> entry : values.entrySet()) {
            if (entry.getValue() != null && entry.getValue().length > 0) {
                filtered.put(entry.getKey(), entry.getValue());
            }
        }
        if (filtered.isEmpty()) {
            xyChart.getSeriesMap().clear();
            return;
        }
        if(xyChart.getSeriesMap().keySet().equals(filtered.keySet())) {
            for (Map.Entry<String, double[]> entry : filtered.entrySet()) {
                xyChart.updateXYSeries(entry.getKey(), null,entry.getValue(), null);
            }
        }else {
            xyChart.getSeriesMap().clear();
            for (Map.Entry<String, double[]> entry : filtered.entrySet()) {
                xyChart.addSeries(entry.getKey(), entry.getValue());
            }
        }
    }

    private List<Integer> covert1bbTo28b(List<Integer> list1, List<Integer> list2 ){
        if(list1.size() != list2.size()){
            System.out.println("size required is equal!");
        }
        List<Integer> list = new ArrayList<>();
        for(int i=0; i<list1.size(); i++){
            int val =   ((int)list1.get(i) & 0xFFFF) + ((  (list2.get(i) & 0xFFF))<<16);
            list.add(val);
        }
        return list;
    }

    private double[] getData( HandValueSetting setting, List<Integer> inList, int len, int dobavka){
        double [] outData = new double[len];
        int index = 0;
        int mask = (1 << (setting.getMsb()+1))-1;
        int shift = (32 - (setting.getMsb()-setting.getLsb()+1));
        for(int val : inList){
            int v1 = ((val & mask) >> setting.getLsb());
            if(setting.isSigned()){
                v1 = (v1 << shift) >> shift;
            }
            outData[index++] = (double) (v1 + dobavka);
        }
        return outData;
    }


    @Override
    public void updateTableValue(RegPanel panel) {

    }


    // ==================== Cursor overlay panel ====================

    private class CursorPanel extends JPanel {
        private final XYChart chart;
        private final boolean isTopChart;
        private final float[] dashPattern = {4f, 4f};

        CursorPanel(XChartPanel<XYChart> chartPanel, XYChart chart, boolean isTopChart) {
            this.chart = chart;
            this.isTopChart = isTopChart;
            setLayout(new BorderLayout());
            add(chartPanel, BorderLayout.CENTER);

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (SwingUtilities.isRightMouseButton(e)) {
                        cursorIndex = -1;
                        repaintCursor();
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setCursorFromMouse(e.getX());
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        setCursorFromMouse(e.getX());
                    }
                }

                private void setCursorFromMouse(int mouseX) {
                    int idx = pixelToDataIndex(mouseX);
                    if (idx >= 0) {
                        cursorIndex = idx;
                        repaintCursor();
                    }
                }
            };
            chartPanel.addMouseListener(ma);
            chartPanel.addMouseMotionListener(ma);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (cursorIndex >= 0) {
                paintOverlay((Graphics2D) g);
            }
        }

        private int getDataLength() {
            Map<String, XYSeries> seriesMap = chart.getSeriesMap();
            if (seriesMap.isEmpty()) return 0;
            XYSeries first = seriesMap.values().iterator().next();
            return first.getYData().length;
        }

        private int pixelToDataIndex(int mouseX) {
            int len = getDataLength();
            if (len == 0) return -1;

            double chartX = chart.getChartXFromCoordinate(mouseX);
            if (Double.isNaN(chartX)) return -1;

            double[] xData = chart.getSeriesMap().values().iterator().next().getXData();
            int idx = Arrays.binarySearch(xData, chartX);
            if (idx < 0) idx = -idx - 1;
            return Math.max(0, Math.min(idx, len - 1));
        }

        private void paintOverlay(Graphics2D g2) {
            int len = getDataLength();
            if (len == 0 || cursorIndex >= len) return;

            double[] xData = chart.getSeriesMap().values().iterator().next().getXData();
            double xVal = xData[cursorIndex];

            int cursorX = (int) chart.getScreenXFromChart(xVal);

            Double yMinObj = chart.getStyler().getYAxisMin();
            Double yMaxObj = chart.getStyler().getYAxisMax();

            int plotTop, plotBottom;
            if (yMinObj != null && yMaxObj != null) {
                plotTop = (int) chart.getScreenYFromChart(yMaxObj);
                plotBottom = (int) chart.getScreenYFromChart(yMinObj);
            } else {
                double yMin = Double.MAX_VALUE;
                double yMax = -Double.MAX_VALUE;
                for (XYSeries s : chart.getSeriesMap().values()) {
                    for (double v : s.getYData()) {
                        if (v < yMin) yMin = v;
                        if (v > yMax) yMax = v;
                    }
                }
                plotTop = (int) chart.getScreenYFromChart(yMax);
                plotBottom = (int) chart.getScreenYFromChart(yMin);
            }
            if (plotTop >= plotBottom) return;

            // Draw dashed vertical line
            g2.setColor(new Color(0x333333));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f));
            g2.drawLine(cursorX, plotTop, cursorX, plotBottom);

            // Draw circles and labels for each series
            List<XYSeries> seriesList = new ArrayList<>(chart.getSeriesMap().values());
            Font labelFont = g2.getFont().deriveFont(Font.BOLD, 11f);
            g2.setFont(labelFont);
            FontMetrics fm = g2.getFontMetrics();

            for (int si = 0; si < seriesList.size(); si++) {
                XYSeries series = seriesList.get(si);
                double[] yData = series.getYData();
                if (cursorIndex >= yData.length) continue;

                double yVal = yData[cursorIndex];
                int pixelY = (int) chart.getScreenYFromChart(yVal);

                // Circle
                Color seriesColor = series.getLineColor();
                g2.setColor(seriesColor);
                g2.setStroke(new BasicStroke(2f));
                g2.fill(new Ellipse2D.Double(cursorX - 4, pixelY - 4, 8, 8));
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(cursorX - 2, pixelY - 2, 4, 4));

                // Value label
                String labelText = String.valueOf((int) Math.round(yVal));
                int textWidth = fm.stringWidth(labelText);

                int labelX, labelY;
                if (si == 0) {
                    labelX = cursorX - textWidth - 10;
                } else {
                    labelX = cursorX + 10;
                }
                labelY = pixelY - 2;

                // Background for readability
                g2.setColor(new Color(0xFFFFFFCC, true));
                g2.fillRect(labelX - 2, labelY - fm.getAscent(), textWidth + 4, fm.getHeight());

                // Text
                g2.setColor(seriesColor);
                g2.drawString(labelText, labelX, labelY);
            }
        }
    }

    private static class AddrItem {
        final int addr;
        private final String label;

        AddrItem(int addr, String label) {
            this.addr = addr;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
