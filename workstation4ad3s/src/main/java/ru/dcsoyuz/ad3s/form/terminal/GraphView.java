package ru.dcsoyuz.ad3s.form.terminal;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import ru.dcsoyuz.ad3s.config.GraphViewProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.RegPanel;
import ru.dcsoyuz.ad3s.form.editor.ILongProcessEventListener;
import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.AddrDef;
import ru.dcsoyuz.ad3s.model.uart.RunnerView;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;



/**
 * Created by yuri.filatov on 11.08.2016.
 */
public class GraphView extends JPanel implements IGraphViewListener, ILongProcessEventListener, IMemoryEventListener {

    private static final Logger logger = LoggerFactory.getLogger(GraphView.class);

    XYChart chart1;
    XYChart chart2;

    private final int WIDTH_BUTTON = 90;
    private final int HEIGHT_BUTTON = 20;
    XChartPanel<XYChart> xyPanel1;
    XChartPanel<XYChart> xyPanel2;
    JButton loadingDataButton;
    private JButton detectorButton;

    private JCheckBox scale1CHB;
    private JTextField yMin1TF, yMax1TF;
    private JCheckBox scale2CHB;
    private JTextField yMin2TF, yMax2TF;

    boolean isRunGraphRefreshing = false;

    GraphView graphView;
    private int sizeFifo = 256;
    List<SelGraphDataContainer> listSelGraphData;

    Map<String, JTextField> mapValuesTF;

    private int cursorIndex = -1;
    private CursorPanel cursorPanel1;
    private CursorPanel cursorPanel2;

    private JRadioButton h1RB, h2RB;
    private boolean detectorMode = false;
    private String currentDetectorMode = "";
    private JTextField[] addressTFs = new JTextField[4];
    private JTextField[] legendTFs = new JTextField[4];
    private JTextField[] addressHighTFs = new JTextField[4];
    private JTextField[] numBitsTFs = new JTextField[4];
    private JRadioButton[] signedRBs = new JRadioButton[4];
    private JRadioButton[] unsignedRBs = new JRadioButton[4];


    public void bindingNewSeries(XYChart chart, int numChart){

        List<SelGraphDataContainer> listCurrent = new ArrayList<>();
        for(SelGraphDataContainer container : listSelGraphData){
            if(container.getNumGraph() == numChart){
                listCurrent.add(container);
            }
        }
        int i = 0;

        Set <String > oldKeys = new HashSet<>(chart.getSeriesMap().keySet());

        for(String key : oldKeys){
            XYSeries series = chart.getSeriesMap().get(key);
            if (series != null && i < listCurrent.size()) {
                series.setLabel(listCurrent.get(i).getLegend());
            }
            i++;
        }

        xyPanel1.updateUI();

    }

    public GraphView(){
        graphView = this;



        Action performRunGetData= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {

                for(SelGraphDataContainer container : listSelGraphData){
                    WorkstationConfig.setProperty(GraphViewProp.NUMGRAPH.getKey(container.getIndex()), String.valueOf(container.getNumGraph()));
                    WorkstationConfig.setProperty(GraphViewProp.ISCHEKED.getKey(container.getIndex()), String.valueOf(container.isChecked()));
                }
                WorkstationConfig.storeProperties();
                saveCurrentSet();

                for(int i = 0; i< listSelGraphData.size(); i++){
                    if(chart1.getSeriesMap().get(listSelGraphData.get(0).getLegend()) == null){
                        bindingNewSeries(chart1, 0);
                        break;
                    }
                }



                Model.getMemoryModel().setIGraphViewListener(graphView);
                if(!Model.getMemoryModel().isProcessing()){
                    Model.getMemoryModel().runCyclicReadGraphValues();
                    isRunGraphRefreshing = true;
                    RefheshGraphAction refheshGraphAction = new RefheshGraphAction();
                    refheshGraphAction.start();
                } else {
                    if(loadingDataButton.getText().equals("Loading...")) {
                        logger.debug(" Before stop another loading process");
                    }
                    Model.getMemoryModel().stopLongProcess();
                }
            }
        };

        mapValuesTF = new HashMap<>();
        listSelGraphData = new ArrayList<>();
        loadingDataButton = createButton(performRunGetData,  "Loading...", "run/stop loading data");




        JPanel[] controlRows = createControlPanels();

        // --- Scale controls for chart1 ---
        scale1CHB = new JCheckBox("Scale:");
        scale1CHB.setSelected(true);
        yMin1TF = new JTextField(4);
        yMin1TF.setMaximumSize(new Dimension(42, 20));
        yMin1TF.setEnabled(false);
        yMax1TF = new JTextField(4);
        yMax1TF.setMaximumSize(new Dimension(42, 20));
        yMax1TF.setEnabled(false);

        DocumentListener yRange1Listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyYRange1(); }
            public void removeUpdate(DocumentEvent e) { applyYRange1(); }
            public void changedUpdate(DocumentEvent e) { applyYRange1(); }
        };
        yMin1TF.getDocument().addDocumentListener(yRange1Listener);
        yMax1TF.getDocument().addDocumentListener(yRange1Listener);
        scale1CHB.addActionListener(e -> {
            yMin1TF.setEnabled(!scale1CHB.isSelected());
            yMax1TF.setEnabled(!scale1CHB.isSelected());
            if (scale1CHB.isSelected()) {
                chart1.getStyler().setYAxisMin((Double) null);
                chart1.getStyler().setYAxisMax((Double) null);
            } else {
                applyYRange1();
            }
            xyPanel1.revalidate(); xyPanel1.repaint();
        });

        JPanel topControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        topControls.add(scale1CHB);
        topControls.add(new JLabel("Ymin:"));
        topControls.add(yMin1TF);
        topControls.add(new JLabel("Ymax:"));
        topControls.add(yMax1TF);
        topControls.add(loadingDataButton);

        // --- Scale controls for chart2 ---
        scale2CHB = new JCheckBox("Scale:");
        scale2CHB.setSelected(true);
        yMin2TF = new JTextField(4);
        yMin2TF.setMaximumSize(new Dimension(42, 20));
        yMin2TF.setEnabled(false);
        yMax2TF = new JTextField(4);
        yMax2TF.setMaximumSize(new Dimension(42, 20));
        yMax2TF.setEnabled(false);

        DocumentListener yRange2Listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyYRange2(); }
            public void removeUpdate(DocumentEvent e) { applyYRange2(); }
            public void changedUpdate(DocumentEvent e) { applyYRange2(); }
        };
        yMin2TF.getDocument().addDocumentListener(yRange2Listener);
        yMax2TF.getDocument().addDocumentListener(yRange2Listener);
        scale2CHB.addActionListener(e -> {
            yMin2TF.setEnabled(!scale2CHB.isSelected());
            yMax2TF.setEnabled(!scale2CHB.isSelected());
            if (scale2CHB.isSelected()) {
                chart2.getStyler().setYAxisMin((Double) null);
                chart2.getStyler().setYAxisMax((Double) null);
            } else {
                applyYRange2();
            }
            xyPanel2.revalidate(); xyPanel2.repaint();
        });

        JPanel bottomControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        bottomControls.add(scale2CHB);
        bottomControls.add(new JLabel("Ymin:"));
        bottomControls.add(yMin2TF);
        bottomControls.add(new JLabel("Ymax:"));
        bottomControls.add(yMax2TF);

        detectorButton = new JButton("Detector On");
        detectorButton.setFocusable(false);
        detectorButton.setMargin(new Insets(2, 2, 2, 2));
        detectorButton.setPreferredSize(new Dimension(WIDTH_BUTTON, HEIGHT_BUTTON));
        detectorButton.setToolTipText("<html>Detector On:<br>"
                + "&nbsp;&nbsp;1. Отключить CPU1 (Mode_config)<br>"
                + "&nbsp;&nbsp;2. Загрузить detector_asm/cpu1_data.hex по адресу 512<br>"
                + "&nbsp;&nbsp;3. Установить HandToEXT в C2 KonturStngs<br>"
                + "&nbsp;&nbsp;4. Включить CPU1 (Mode_config)<br>"
                + "Detector Off:<br>"
                + "&nbsp;&nbsp;1. Очистить HandToEXT в C2 KonturStngs<br>"
                + "&nbsp;&nbsp;2. Отключить CPU1 (Mode_config)</html>");
        detectorButton.addActionListener(e -> {
            boolean isOn = detectorButton.getText().equals("Detector On");
            if (isOn) {
                detectorButton.setText("Detector Off");
                detectorButton.setEnabled(false);
                Model.getMemoryModel().enableDetectorAnalyzer(
                        () -> SwingUtilities.invokeLater(() -> {
                            detectorButton.setEnabled(true);
                            detectorMode = true;
                            h1RB.setVisible(true);
                            h2RB.setVisible(true);
                            h1RB.setSelected(true);
                            saveCurrentSet();
                            loadSet("DET_H1");
                        }),
                        () -> SwingUtilities.invokeLater(() -> {
                            detectorButton.setText("Detector On");
                            detectorButton.setEnabled(true);
                        })
                );
            } else {
                detectorButton.setEnabled(false);
                Model.getMemoryModel().disableDetectorAnalyzer();
                detectorButton.setText("Detector On");
                detectorButton.setEnabled(true);
                detectorMode = false;
                h1RB.setVisible(false);
                h2RB.setVisible(false);
                saveCurrentSet();
                loadSet("");
            }
        });
        bottomControls.add(detectorButton);

        h1RB = new JRadioButton("H1");
        h2RB = new JRadioButton("H2");
        ButtonGroup hGroup = new ButtonGroup();
        hGroup.add(h1RB);
        hGroup.add(h2RB);
        h1RB.setSelected(true);
        h1RB.setVisible(false);
        h2RB.setVisible(false);
        h1RB.addActionListener(ev -> switchDetectorMode("DET_H1"));
        h2RB.addActionListener(ev -> switchDetectorMode("DET_H2"));
        bottomControls.add(h1RB);
        bottomControls.add(h2RB);

        // --- Charts ---
        setLayout(new GridBagLayout());
        double[][] yData = new double[2][2];
        String [] seriesKeys1 = {"s0", "s1"};
        String [] seriesKeys2 = {"s2", "s3"};
        chart1 = QuickChart.getChart("Coord", "Time", "Value", seriesKeys1, new double[] { 0, 0  }, yData);
        chart1.getStyler().setLegendVisible(true);
        chart1.getStyler().setAxisTitlesVisible(false);
        chart1.getStyler().setChartTitleVisible(false);
        chart1.getStyler().setLegendPosition(Styler.LegendPosition.InsideSW);
        chart1.getStyler().setXAxisTicksVisible(false);
        chart1.getStyler().setYAxisLeftWidthHint(50);
        chart1.getStyler().setChartBackgroundColor(new Color(230, 243, 255));
        chart1.getStyler().setPlotBackgroundColor(new Color(230, 243, 255));
        chart1.getStyler().setMarkerSize(4);
        chart1.getSeriesMap().get("s0").setLabel(listSelGraphData.get(0).getLegend());
        chart1.getSeriesMap().get("s1").setLabel(listSelGraphData.get(1).getLegend());
        chart2 = QuickChart.getChart("Vel", "Time", "Value", seriesKeys2, new double[] { 0, 0  }, yData);
        chart2.getStyler().setLegendVisible(true);
        chart2.getStyler().setChartTitleVisible(false);
        chart2.getStyler().setLegendPosition(Styler.LegendPosition.InsideNW);
        chart2.getStyler().setXAxisTicksVisible(false);
        chart2.getStyler().setAxisTitlesVisible(false);
        chart2.getStyler().setYAxisLeftWidthHint(50);
        chart2.getStyler().setChartBackgroundColor(new Color(230, 243, 255));
        chart2.getStyler().setPlotBackgroundColor(new Color(230, 243, 255));
        chart2.getStyler().setMarkerSize(4);
        chart2.getSeriesMap().get("s2").setLabel(listSelGraphData.get(2).getLegend());
        chart2.getSeriesMap().get("s3").setLabel(listSelGraphData.get(3).getLegend());

        xyPanel1 = new XChartPanel(chart1);
        xyPanel2 = new XChartPanel(chart2);

        cursorPanel1 = new CursorPanel(xyPanel1, chart1, true);
        cursorPanel2 = new CursorPanel(xyPanel2, chart2, false);

        // --- Layout ---
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;

        gbc.weighty = 1.0;
        gbc.gridy = 0;
        add(cursorPanel1, gbc);

        gbc.weighty = 0;
        gbc.gridy = 1;
        add(topControls, gbc);

        for (int i = 0; i < controlRows.length; i++) {
            gbc.gridy = 2 + i;
            add(controlRows[i], gbc);
        }

        gbc.gridy = 2 + controlRows.length;
        add(bottomControls, gbc);

        gbc.weighty = 1.0;
        gbc.gridy = 3 + controlRows.length;
        add(cursorPanel2, gbc);

        Model.getMemoryModel().addMemoryEventListener(RunnerView.GRAPH_VIEW, this);

        Model.getMemoryModel().addLongProcessEventListener(this);

    }




    private JPanel[] createControlPanels(){
        JPanel[] rows = new JPanel[4];
        for (int i = 0; i < 4; i++) {
            rows[i] = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            rows[i].setMinimumSize(new Dimension(0, HEIGHT_BUTTON));
        }

        String temp;
        temp = WorkstationConfig.getProperty(GraphViewProp.ADDRESSLOW.getKey(0));
        int address0 = temp != null ? Integer.parseInt(temp) : 16;
        temp = WorkstationConfig.getProperty(GraphViewProp.LEGEND.getKey(0));
        String legend0 = temp != null ? temp : "COORD1";
        temp = WorkstationConfig.getProperty(GraphViewProp.NUMBITS.getKey(0));
        int numBits0 = temp != null ? Integer.parseInt(temp) : 16;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISSIGNED.getKey(0));
        boolean isSigned0 = temp != null ? Boolean.parseBoolean(temp) : false;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISCHEKED.getKey(0));
        boolean isChecked0 = temp != null ? Boolean.parseBoolean(temp) : true;

        temp = WorkstationConfig.getProperty(GraphViewProp.ADDRESSLOW.getKey(1));
        int address1 = temp != null ? Integer.parseInt(temp) : 16;
        temp = WorkstationConfig.getProperty(GraphViewProp.LEGEND.getKey(1));
        String legend1 = temp != null ? temp : "COORD1";
        temp = WorkstationConfig.getProperty(GraphViewProp.NUMBITS.getKey(1));
        int numBits1 = temp != null ? Integer.parseInt(temp) : 16;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISSIGNED.getKey(1));
        boolean isSigned1 = temp != null ? Boolean.parseBoolean(temp) : false;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISCHEKED.getKey(1));
        boolean isChecked1 = temp != null ? Boolean.parseBoolean(temp) : true;

        listSelGraphData.add(new SelGraphDataContainer(legend0, address0, numBits0, isSigned0, isChecked0, 0, sizeFifo));
        listSelGraphData.add(new SelGraphDataContainer(legend1, address1, numBits1, isSigned1, isChecked1, 0, sizeFifo));

        rows[0].add(createSelectDataPanel("0", listSelGraphData.get(0)));
        listSelGraphData.get(0).setIndex(0);

        rows[1].add(createSelectDataPanel("1", listSelGraphData.get(1)));
        listSelGraphData.get(1).setIndex(1);

        temp = WorkstationConfig.getProperty(GraphViewProp.ADDRESSLOW.getKey(2));
        int address2 = temp != null ? Integer.parseInt(temp) : 24;
        temp = WorkstationConfig.getProperty(GraphViewProp.LEGEND.getKey(2));
        String legend2 = temp != null ? temp : "VEL1";
        temp = WorkstationConfig.getProperty(GraphViewProp.NUMBITS.getKey(2));
        int numBits2 = temp != null ? Integer.parseInt(temp) : 16;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISSIGNED.getKey(2));
        boolean isSigned2 = temp != null ? Boolean.parseBoolean(temp) : true;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISCHEKED.getKey(2));
        boolean isChecked2 = temp != null ? Boolean.parseBoolean(temp) : true;

        temp = WorkstationConfig.getProperty(GraphViewProp.ADDRESSLOW.getKey(3));
        int address3 = temp != null ? Integer.parseInt(temp) : 24;
        temp = WorkstationConfig.getProperty(GraphViewProp.LEGEND.getKey(3));
        String legend3 = temp != null ? temp : "VEL1";
        temp = WorkstationConfig.getProperty(GraphViewProp.NUMBITS.getKey(3));
        int numBits3 = temp != null ? Integer.parseInt(temp) : 16;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISSIGNED.getKey(3));
        boolean isSigned3 = temp != null ? Boolean.parseBoolean(temp) : true;
        temp = WorkstationConfig.getProperty(GraphViewProp.ISCHEKED.getKey(3));
        boolean isChecked3 = temp != null ? Boolean.parseBoolean(temp) : true;

        listSelGraphData.add(new SelGraphDataContainer(legend2, address2, numBits2, isSigned2, isChecked2, 1, sizeFifo));
        listSelGraphData.add(new SelGraphDataContainer(legend3, address3, numBits3, isSigned3, isChecked3, 1, sizeFifo));

        rows[2].add(createSelectDataPanel("2", listSelGraphData.get(2)));
        listSelGraphData.get(2).setIndex(2);

        rows[3].add(createSelectDataPanel("3", listSelGraphData.get(3)));
        listSelGraphData.get(3).setIndex(3);

        return rows;
    }


    // --- Y range helpers ---

    private void applyYRange1() {
        if (scale1CHB.isSelected()) return;
        SwingUtilities.invokeLater(() -> {
            try {
                double min = Double.parseDouble(yMin1TF.getText().trim());
                double max = Double.parseDouble(yMax1TF.getText().trim());
                chart1.getStyler().setYAxisMin(min);
                chart1.getStyler().setYAxisMax(max);
            } catch (NumberFormatException ignored) {}
            xyPanel1.revalidate(); xyPanel1.repaint();
        });
    }

    private void applyYRange2() {
        if (scale2CHB.isSelected()) return;
        SwingUtilities.invokeLater(() -> {
            try {
                double min = Double.parseDouble(yMin2TF.getText().trim());
                double max = Double.parseDouble(yMax2TF.getText().trim());
                chart2.getStyler().setYAxisMin(min);
                chart2.getStyler().setYAxisMax(max);
            } catch (NumberFormatException ignored) {}
            xyPanel2.revalidate(); xyPanel2.repaint();
        });
    }

    private void updateYRangeFromChart(XYChart chart, JTextField yMinTF, JTextField yMaxTF, JCheckBox scaleCHB) {
        if (!scaleCHB.isSelected()) return;
        Double minObj = chart.getStyler().getYAxisMin();
        Double maxObj = chart.getStyler().getYAxisMax();
        if (minObj == null || maxObj == null) {
            // Compute from data
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (XYSeries s : chart.getSeriesMap().values()) {
                for (double v : s.getYData()) {
                    if (!Double.isNaN(v)) {
                        if (v < min) min = v;
                        if (v > max) max = v;
                    }
                }
            }
            if (min != Double.MAX_VALUE) {
                int rMin = (int) Math.floor(min / 100.0) * 100;
                int rMax = ((int) Math.ceil(max / 100.0)) * 100;
                yMinTF.setText(String.valueOf(rMin));
                yMaxTF.setText(String.valueOf(rMax));
            }
        }
    }

    private void repaintCursor() {
        cursorPanel1.repaint();
        cursorPanel2.repaint();
    }

    // --- Detector mode: address/legend set management ---

    private String configKey(String mode, int index, String field) {
        if (mode.isEmpty()) return "GRAPHVIEW_" + index + "_" + field;
        return "GRAPHVIEW_" + mode + "_" + index + "_" + field;
    }

    private void saveCurrentSet() {
        for (int i = 0; i < 4; i++) {
            SelGraphDataContainer dc = listSelGraphData.get(i);
            WorkstationConfig.setProperty(configKey(currentDetectorMode, i, "ADDRESSLOW"), String.valueOf(dc.getAddressLow()));
            WorkstationConfig.setProperty(configKey(currentDetectorMode, i, "LEGEND"), dc.getLegend());
            WorkstationConfig.setProperty(configKey(currentDetectorMode, i, "NUMBITS"), String.valueOf(dc.getNumBits()));
            WorkstationConfig.setProperty(configKey(currentDetectorMode, i, "ISSIGNED"), String.valueOf(dc.isSigned()));
        }
        WorkstationConfig.storeProperties();
    }

    private void loadSet(String mode) {
        boolean isDetector = !mode.isEmpty();
        String[] defaultLegends = mode.equals("DET_H1") ?
                new String[]{"SIN1_IN", "SIN1_VIRT", "COS1_IN", "COS1_VIRT"} :
                mode.equals("DET_H2") ?
                new String[]{"SIN2_IN", "SIN2_VIRT", "COS2_IN", "COS2_VIRT"} :
                new String[]{"COORD1", "COORD1", "VEL1", "VEL1"};
        int[] defaultAddrs = mode.equals("DET_H1") ?
                new int[]{640, 648, 642, 650} :
                mode.equals("DET_H2") ?
                new int[]{644, 652, 646, 654} :
                new int[]{16, 16, 24, 24};
        int[] defaultNumBits = isDetector ?
                new int[]{13, 13, 13, 13} :
                new int[]{16, 16, 16, 16};
        boolean[] defaultSigned = isDetector ?
                new boolean[]{true, true, true, true} :
                new boolean[]{false, false, true, true};

        for (int i = 0; i < 4; i++) {
            String addrStr = WorkstationConfig.getProperty(configKey(mode, i, "ADDRESSLOW"));
            int addr = addrStr != null ? Integer.parseInt(addrStr) : defaultAddrs[i];
            String legend = WorkstationConfig.getProperty(configKey(mode, i, "LEGEND"));
            legend = legend != null ? legend : defaultLegends[i];
            String nbStr = WorkstationConfig.getProperty(configKey(mode, i, "NUMBITS"));
            int numBits = nbStr != null ? Integer.parseInt(nbStr) : defaultNumBits[i];
            String sStr = WorkstationConfig.getProperty(configKey(mode, i, "ISSIGNED"));
            boolean isSigned = sStr != null ? Boolean.parseBoolean(sStr) : defaultSigned[i];

            SelGraphDataContainer dc = listSelGraphData.get(i);
            dc.setAddressLow(addr);
            dc.setLegend(legend);
            dc.setNumBits(numBits);
            dc.setSigned(isSigned);

            addressTFs[i].setText(String.valueOf(addr));
            legendTFs[i].setText(legend);
            numBitsTFs[i].setText(String.valueOf(numBits));
            if (isSigned) {
                signedRBs[i].setSelected(true);
            } else {
                unsignedRBs[i].setSelected(true);
            }
        }
        currentDetectorMode = mode;
    }

    private void switchDetectorMode(String newMode) {
        if (newMode.equals(currentDetectorMode)) return;
        saveCurrentSet();
        loadSet(newMode);
    }

    public void restoreDefaults() {
        for (String prefix : new String[]{"", "DET_H1", "DET_H2"}) {
            for (int i = 0; i < 4; i++) {
                WorkstationConfig.removeProperty(configKey(prefix, i, "ADDRESSLOW"));
                WorkstationConfig.removeProperty(configKey(prefix, i, "LEGEND"));
                WorkstationConfig.removeProperty(configKey(prefix, i, "NUMBITS"));
                WorkstationConfig.removeProperty(configKey(prefix, i, "ISSIGNED"));
            }
        }
        WorkstationConfig.storeProperties();
        loadSet(currentDetectorMode);
    }


    public JTextField createAddressTextField(int address){
        JTextField textField = new JTextField(String.valueOf(address));
        Dimension d = new Dimension(30, HEIGHT_BUTTON);
        textField.setMinimumSize(d);
        textField.setPreferredSize(d);
        textField.setMaximumSize(d);
        textField.setMargin(new Insets(0, 0, 0, 0));
        return textField;
    }

    public JTextField createNumBitsTextField(int numBits){
        JTextField textField = new JTextField(String.valueOf(numBits));
        textField.setPreferredSize(new Dimension(30,HEIGHT_BUTTON));
        textField.setMinimumSize(new Dimension(20,HEIGHT_BUTTON));
        textField.setMargin(new Insets(0, 0, 0, 0));
        textField.getDocument().addDocumentListener( createDocumentListenerNB(textField));
        return textField;
    }

    public JTextField createLegendTextField(String legend, String index){
        JTextField textField = new JTextField(legend);
        textField.setName(index);
        textField.setPreferredSize(new Dimension(50,HEIGHT_BUTTON));
        textField.setMinimumSize(new Dimension(20,HEIGHT_BUTTON));
        textField.setMargin(new Insets(0, 0, 0, 0));
        textField.getDocument().addDocumentListener(createDocumentListenerLegend(textField));
        return textField;
    }

    public JTextField createValueTextField(String index){
        JTextField textField = new JTextField("0");
        mapValuesTF.put(index, textField);
        textField.setPreferredSize(new Dimension(40,HEIGHT_BUTTON));
        textField.setMinimumSize(new Dimension(20,HEIGHT_BUTTON));
        textField.setEditable(false);
        textField.setBackground(new Color(0xC0D0D8));
        textField.setMargin(new Insets(0, 0, 0, 0));
        return textField;
    }


    public JPanel createFreePanel(){
        JPanel panel = new JPanel();
        panel.setMaximumSize(new Dimension(WIDTH_BUTTON, HEIGHT_BUTTON));
        return panel;
    }

    public JPanel createFreePanel(int width){
        JPanel panel = new JPanel();
        panel.setMaximumSize(new Dimension(width, HEIGHT_BUTTON));
        return panel;
    }

    private JLabel createLabel(String text, int width){
        JLabel label = new JLabel(text);
        label.setMaximumSize(new Dimension(width, HEIGHT_BUTTON));
        return label;
    }

    public JPanel createTitlePanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(createFreePanel( 30));
        panel.add(createLabel("Legend", 70));
        panel.add(createLabel("B", 25));
        panel.add(createLabel("S", 20));
        panel.add(createLabel("U", 20));
        panel.add(createLabel("LA:", 30));
        panel.add(createLabel("SA:", 30));
        return panel;
    }

    private JPanel createSelectDataPanel(String index, SelGraphDataContainer dc ){
        int idx = Integer.parseInt(index);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));

        JCheckBox checkBoxGetData = new JCheckBox();
        checkBoxGetData.setSelected(dc.isChecked());
        checkBoxGetData.setName(index);
        checkBoxGetData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox chb = ((JCheckBox)e.getSource());
                listSelGraphData.get(Integer.parseInt(chb.getName())).setChecked(chb.isSelected());
            }
        });
        panel.add(checkBoxGetData);

        JTextField legendTF = createLegendTextField(dc.getLegend(), index);
        legendTFs[idx] = legendTF;
        panel.add(legendTF);
        panel.add(createValueTextField(index));

        JTextField numBitsTF = createNumBitsTextField(dc.getNumBits());
        numBitsTF.setName(index);
        numBitsTFs[idx] = numBitsTF;
        panel.add(new JLabel("B"));
        panel.add(numBitsTF);

        JRadioButton rb_signed = new JRadioButton("S");
        JRadioButton rb_unsigned = new JRadioButton("U");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rb_signed);
        bg.add(rb_unsigned);
        rb_unsigned.setName(index);
        rb_signed.setName(index);
        signedRBs[idx] = rb_signed;
        unsignedRBs[idx] = rb_unsigned;
        if(dc.isSigned()){
            rb_signed.setSelected(true);
        } else {
            rb_unsigned.setSelected(true);
        }
        rb_unsigned.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JRadioButton rb =   ((JRadioButton)e.getSource());
                listSelGraphData.get(Integer.parseInt(rb.getName())).setSigned(!rb.isSelected());
            }
        });
        rb_signed.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               JRadioButton rb =   ((JRadioButton)e.getSource());
               listSelGraphData.get(Integer.parseInt(rb.getName())).setSigned(rb.isSelected());
            }
        });
        panel.add(rb_signed);
        panel.add(rb_unsigned);

        JTextField addressLowTF =  createAddressTextField(dc.getAddressLow());
        addressLowTF.setName(index);
        JTextField addressHighTF = createAddressTextField(dc.getAddressLow()+1);
        addressLowTF.getDocument().addDocumentListener(createDocumentListener(addressLowTF, addressHighTF));
        panel.add(new JLabel("LA:"));
        panel.add(addressLowTF);
        addressHighTF.setEditable(false);
        addressHighTF.setBackground(new Color(0xD0D0D0));
        panel.add(new JLabel("SA:"));
        panel.add(addressHighTF);

        addressTFs[idx] = addressLowTF;
        addressHighTFs[idx] = addressHighTF;

        return panel;
    }



    private DocumentListener createDocumentListener (JTextField lowAddrTF, JTextField highAddrTF) {
            DocumentListener listener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                int addr = 0;
                if(lowAddrTF.getText().equals("")){
                    highAddrTF.setText("1");
                }else {
                    try {
                         addr = Integer.parseInt(lowAddrTF.getText());
                        highAddrTF.setText(String.valueOf(addr+1));
                    }catch (NumberFormatException  e){
                        logger.debug("Illegal value in addr textfield");
                    }
                }
                listSelGraphData.get(Integer.parseInt(lowAddrTF.getName())).setAddressLow(addr);
            }

        };
        return listener;
    }


    private DocumentListener createDocumentListenerNB (JTextField numBitsTF) {
        DocumentListener listener = new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                int value = 0;
                if(numBitsTF.getText().equals("")){

                }else {
                    try {
                        value = Integer.parseInt(numBitsTF.getText());
                    }catch (NumberFormatException  e){
                        logger.debug("Illegal value in addr textfield");
                    }
                }
                listSelGraphData.get(Integer.parseInt(numBitsTF.getName())).setNumBits(value);
            }

        };
        return listener;
    }


    private DocumentListener createDocumentListenerLegend (JTextField legendsTF) {
        DocumentListener listener = new DocumentListener() {
            final boolean[] isUpdating = {false};

            public void changedUpdate(DocumentEvent e) {
                warn();
            }

            public void removeUpdate(DocumentEvent e) {
                warn();
            }

            public void insertUpdate(DocumentEvent e) {
                warn();
            }

            public void warn() {
                if (isUpdating[0]) return;
                isUpdating[0] = true;
                try {
                    int idx = Integer.parseInt(legendsTF.getName());
                    SelGraphDataContainer dc = listSelGraphData.get(idx);
                    String newLegend = legendsTF.getText();
                    if (newLegend != null && !newLegend.isEmpty()) {
                        XYChart chart = dc.getNumGraph() == 0 ? chart1 : chart2;
                        XYSeries series = chart.getSeriesMap().get("s" + idx);
                        if (series != null) {
                            series.setLabel(newLegend);
                        }
                        dc.setLegend(newLegend);
                    }
                } finally {
                    isUpdating[0] = false;
                }
            }

        };
        return listener;
    }


    private JButton createButton(Action action, String name, String toolTipText  ){
        JButton button  = new JButton(action);
        button.setText(name);
        button.setToolTipText(toolTipText);
        button.setPreferredSize(new Dimension(WIDTH_BUTTON,HEIGHT_BUTTON));
        button.setMargin(new Insets(2,2,2,2));
        return button;

    }


    private class RefheshGraphAction extends Thread {
        @Override
        public void run() {

            while (isRunGraphRefreshing){

                SwingUtilities.invokeLater(() -> {
                    for(SelGraphDataContainer dc : listSelGraphData){
                        if(dc.isChecked()){
                            try {
                                if(dc.getNumGraph() == 0) {
                                    chart1.updateXYSeries("s" + dc.getIndex(), null, dc.getFifoArray(), null);
                                } else {
                                    chart2.updateXYSeries("s" + dc.getIndex(), null, dc.getFifoArray(), null);
                                }
                            } catch (IllegalArgumentException ignored) {
                            }
                        }
                    }
                    updateYRangeFromChart(chart1, yMin1TF, yMax1TF, scale1CHB);
                    updateYRangeFromChart(chart2, yMin2TF, yMax2TF, scale2CHB);
                    xyPanel1.revalidate();
                    xyPanel2.revalidate();
                    xyPanel2.repaint();
                    xyPanel1.repaint();
                    repaintCursor();
                });
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    logger.error("InterruptedException in graph refresh", e);
                }
            }
        }
    }

    @Override
    public void updateStatusOfProcessing() {
        if(Model.getMemoryModel().isProcessing()){
            loadingDataButton.setText("Stop");
            detectorButton.setEnabled(false);
        } else {
            loadingDataButton.setText("Loading...");
            isRunGraphRefreshing = false;
            detectorButton.setEnabled(true);
        }
    }

    private int getFormatValue (int value , SelGraphDataContainer dc ){
        int mask = (int)Math.pow(2, dc.getNumBits()) - 1;
        int maskValue = value & mask;
        int newValue = maskValue;
        if(dc.isSigned()){
            int rel = 32- dc.getNumBits();
            newValue = (maskValue << rel) >> rel;
            newValue = newValue;
        }
        return newValue;
    }



    @Override
    public List<Integer> getListAddresses() {
        List<Integer> list = new ArrayList<>();
        for(SelGraphDataContainer dc : listSelGraphData){
            if(dc.isChecked()){
                list.add(dc.getAddressLow());
                list.add(dc.getAddressLow() + 1);
            }
        }
        return list;
    }

    @Override
    public void updateValues() {
        List <Integer> memoryValues = Model.getMemoryModel().getRespValues();

        int index = 0;
        for(int k = 0; k< listSelGraphData.size(); k++){
            if(listSelGraphData.get(k).isChecked()) {
                int lowValue = memoryValues.get(index);
                int highValue = memoryValues.get(index + 1);
                int address = listSelGraphData.get(k).getAddressLow();
                int value = (highValue << 16) | lowValue;
                if(AddrDef.isCPUaddress(address)){
                    value = (highValue << 14) | lowValue;
                }
                int newValue = getFormatValue(value, listSelGraphData.get(k));
                listSelGraphData.get(k).addValueToFifo(newValue);
                JTextField tf =    mapValuesTF.get(String.valueOf(k));
                tf.setText(String.valueOf(newValue));
                index = index+2;
            }
        }

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

            g2.setColor(new Color(0x333333));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f));
            g2.drawLine(cursorX, plotTop, cursorX, plotBottom);

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

                Color seriesColor = series.getLineColor();
                g2.setColor(seriesColor);
                g2.setStroke(new BasicStroke(2f));
                g2.fill(new Ellipse2D.Double(cursorX - 4, pixelY - 4, 8, 8));
                g2.setColor(Color.WHITE);
                g2.fill(new Ellipse2D.Double(cursorX - 2, pixelY - 2, 4, 4));

                String labelText = String.valueOf((int) Math.round(yVal));
                int textWidth = fm.stringWidth(labelText);

                int labelX, labelY;
                if (si == 0) {
                    labelX = cursorX - textWidth - 10;
                } else {
                    labelX = cursorX + 10;
                }
                labelY = pixelY - 2;

                g2.setColor(new Color(0xFFFFFFCC, true));
                g2.fillRect(labelX - 2, labelY - fm.getAscent(), textWidth + 4, fm.getHeight());

                g2.setColor(seriesColor);
                g2.drawString(labelText, labelX, labelY);
            }
        }
    }
}
