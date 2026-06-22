package ru.dcsoyuz.ad3s.form.terminal;


import org.knowm.xchart.QuickChart;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.RegPanel;
import ru.dcsoyuz.ad3s.form.editor.ILongProcessEventListener;
import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.AddrCpuHand;
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.List;


public class HandTapView extends JPanel implements IGraphViewListener, ILongProcessEventListener, IMemoryEventListener {

    XYChart chart1;
    XYChart chart2;

    private final int WIDTH_BUTTON = 80;
    private final int HEIGHT_BUTTON = 20;

    private JComboBox<AddrItem> addressCombo;
    private JTextField yRangeTF;

    private JCheckBox triggeredCHB;
    private JCheckBox scaleCHB;
    XChartPanel<XYChart> xyPanel1;
    XChartPanel<XYChart> xyPanel2;
    JButton loadingDataButton;
    private JButton onOffButton;

    HandTapView handTapView;
    boolean isRunGraphRefreshing = false;

    private JRadioButton h1RB;
    private JRadioButton h2RB;

    private JSpinner thresholdSpinner;

    // Save to file state
    private JButton saveButton;
    private File waveDataDir;
    private boolean wavePathSelected = false;
    private Map<String, double[]> lastNumberData;
    private Map<String, double[]> lastSignalData;
    private List<String> lastFieldNames;

    // Cursor state: -1 = no cursor, >=0 = data index
    private int cursorIndex = -1;
    private CursorPanel cursorPanel1;
    private CursorPanel cursorPanel2;


    public HandTapView(){

        handTapView = this;
        extractResourceDir("matlab");
        Action performRunGetData= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(!Model.getMemoryModel().isProcessing()){

                    int threshold = ((SpinnerNumberModel) thresholdSpinner.getModel()).getNumber().intValue();
                    List<Integer> writeValues = new ArrayList<>(1);
                    writeValues.add(threshold);
                    Model.getMemoryModel().setReqValues(763, writeValues);
                    Model.getMemoryModel().writeValues();

                    // Wait for write to complete before starting read
                    new Thread(() -> {
                        while (Model.getMemoryModel().isProcessing()) {
                            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
                        }
                        SwingUtilities.invokeLater(() -> {
                            Model.getMemoryModel().setIGraphViewListener(handTapView);
                            Model.getMemoryModel().runHandTapReadValues();
                            isRunGraphRefreshing = true;
                        });
                    }).start();
                } else {
                    if(loadingDataButton.getText().equals("Loading...")) {
                        System.out.println(" Before stop another loading process");
                    }
                    Model.getMemoryModel().stopLongProcess();
                }
            }
        };

        String savedAddress = WorkstationConfig.getProperty("HANDTAP_ADDRESS");
        addressCombo = new JComboBox<>();
        for (AddrCpuHand a : AddrCpuHand.values()) {
            List<HandValueSetting> signals = HandValueSetting.getHandValueSettings(a);
            String sigNames = signals.stream().map(HandValueSetting::name).collect(java.util.stream.Collectors.joining(", "));
            String label = a.getAddr() + " - " + sigNames;
            addressCombo.addItem(new AddrItem(a.getAddr(), label));
        }
        int defaultAddr = 1;
        if (savedAddress != null) {
            try { defaultAddr = Integer.parseInt(savedAddress); } catch (NumberFormatException ignored) {}
        }
        addressCombo.setSelectedItem(null);
        for (int i = 0; i < addressCombo.getItemCount(); i++) {
            if (((AddrItem) addressCombo.getItemAt(i)).addr == defaultAddr) {
                addressCombo.setSelectedIndex(i);
                break;
            }
        }
        addressCombo.addActionListener(e -> {
            AddrItem item = (AddrItem) addressCombo.getSelectedItem();
            if (item != null) {
                WorkstationConfig.setProperty("HANDTAP_ADDRESS", String.valueOf(item.addr));
                WorkstationConfig.storeProperties();
            }
        });

        // Y range for top chart (-value .. +value)
        String savedYRange = WorkstationConfig.getProperty("HANDTAP_YRANGE");
        int yRangeValue = 2000;
        if (savedYRange != null) {
            try { yRangeValue = Integer.parseInt(savedYRange); } catch (NumberFormatException ignored) {}
        }
        yRangeTF = new JTextField(String.valueOf(yRangeValue), 5);
        yRangeTF.setMaximumSize(new Dimension(60, 20));
        yRangeTF.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyYRange(); }
            public void removeUpdate(DocumentEvent e) { applyYRange(); }
            public void changedUpdate(DocumentEvent e) { applyYRange(); }

            private void applyYRange() {
                if (scaleCHB != null && scaleCHB.isSelected()) return;
                SwingUtilities.invokeLater(() -> {
                    applyManualYRange();
                });
            }
        });

        scaleCHB = new JCheckBox("Scale:");
        scaleCHB.setSelected(false);
        yRangeTF.setEnabled(true);
        scaleCHB.addActionListener(e -> {
            yRangeTF.setEnabled(!scaleCHB.isSelected());
            if (scaleCHB.isSelected()) {
                chart1.getStyler().setYAxisMin((Double) null);
                chart1.getStyler().setYAxisMax((Double) null);
            } else {
                applyManualYRange();
            }
            xyPanel1.revalidate(); xyPanel1.repaint();
        });

        triggeredCHB = new JCheckBox("Trig");
        String savedTrig = WorkstationConfig.getProperty("HANDTAP_TRIG");
        triggeredCHB.setSelected("true".equalsIgnoreCase(savedTrig));
        triggeredCHB.addActionListener(e -> {
            WorkstationConfig.setProperty("HANDTAP_TRIG", String.valueOf(triggeredCHB.isSelected()));
            WorkstationConfig.storeProperties();
        });

        String savedThreshold = WorkstationConfig.getProperty("HANDTAP_THRESHOLD");
        int thresholdValue = 0;
        if (savedThreshold != null) {
            try { thresholdValue = Integer.parseInt(savedThreshold); } catch (NumberFormatException ignored) {}
        }
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(thresholdValue, 0, 99, 1);
        thresholdSpinner = new JSpinner(spinnerModel);
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(thresholdSpinner, "#");
        editor.getTextField().setColumns(2);
        thresholdSpinner.setEditor(editor);
        Dimension tfSize = editor.getTextField().getPreferredSize();
        thresholdSpinner.setPreferredSize(new Dimension(tfSize.width + 10, tfSize.height));
        thresholdSpinner.addChangeListener(e -> {
            WorkstationConfig.setProperty("HANDTAP_THRESHOLD", String.valueOf(spinnerModel.getNumber().intValue()));
            WorkstationConfig.storeProperties();
        });

        loadingDataButton = createButton(performRunGetData,  "Loading...", "run/stop loading data");

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

        // Apply saved Y range on startup
        if (yRangeValue > 0) {
            chart1.getStyler().setYAxisMin((double) -yRangeValue);
            chart1.getStyler().setYAxisMax((double) yRangeValue);
        }

        // Wrap chart panels with cursor overlay
        cursorPanel1 = new CursorPanel(xyPanel1, chart1, true);
        cursorPanel2 = new CursorPanel(xyPanel2, chart2, false);

        setLayout(new GridBagLayout());

        // Controls row
        h1RB = new JRadioButton("H1");
        h2RB = new JRadioButton("H2");
        String savedH = WorkstationConfig.getProperty("HANDTAP_H1H2");
        h1RB.setSelected(!"H2".equals(savedH));
        h2RB.setSelected("H2".equals(savedH));
        ButtonGroup h12BG = new ButtonGroup();
        h12BG.add(h1RB);
        h12BG.add(h2RB);
        ActionListener h12Listener = e -> {
            WorkstationConfig.setProperty("HANDTAP_H1H2", h2RB.isSelected() ? "H2" : "H1");
            WorkstationConfig.storeProperties();
        };
        h1RB.addActionListener(h12Listener);
        h2RB.addActionListener(h12Listener);

        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));

        onOffButton = new JButton("ON");
        onOffButton.setFocusable(false);
        onOffButton.setMargin(new Insets(0, 2, 0, 2));
        onOffButton.setToolTipText("<html>ON:<br>"
                + "&nbsp;&nbsp;1. Отключить CPU1, CPU2 (Mode_config)<br>"
                + "&nbsp;&nbsp;2. Настроить AFE_config: SHRD_RAM=1, SHRD_CPU2=0<br>"
                + "&nbsp;&nbsp;3. Загрузить handtap_asm/cpu1_data.hex по адресу 512<br>"
                + "&nbsp;&nbsp;4. Включить CPU1 (Mode_config)<br>"
                + "OFF:<br>"
                + "&nbsp;&nbsp;1. Отключить CPU1 (Mode_config)</html>");
        onOffButton.addActionListener(e -> {
            boolean isOn = "ON".equals(onOffButton.getText());
            if (isOn) {
                onOffButton.setEnabled(false);
                Model.getMemoryModel().enableHandTapAnalyzer(() -> SwingUtilities.invokeLater(() -> {
                    onOffButton.setText("OFF");
                    onOffButton.setEnabled(true);
                    loadingDataButton.setEnabled(true);
                }), () -> SwingUtilities.invokeLater(() -> {
                    onOffButton.setEnabled(true);
                }));
            } else {
                onOffButton.setText("ON");
                loadingDataButton.setEnabled(false);
                Model.getMemoryModel().disableHandTapAnalyzer();
            }
        });
        controlsPanel.add(onOffButton);

        loadingDataButton.setEnabled(false);
        controlsPanel.add(h1RB);
        controlsPanel.add(h2RB);
        controlsPanel.add(new JLabel("Addr:"));
        addressCombo.setPreferredSize(new Dimension(60, 20));
        controlsPanel.add(addressCombo);
        controlsPanel.add(new JLabel("Y:"));
        controlsPanel.add(yRangeTF);
        controlsPanel.add(scaleCHB);
        controlsPanel.add(triggeredCHB);
        controlsPanel.add(thresholdSpinner);
        controlsPanel.add(loadingDataButton);

        // Save to file button
        saveButton = new JButton("Save to file");
        saveButton.setFocusable(false);
        saveButton.setMargin(new Insets(0, 2, 0, 2));
        saveButton.setToolTipText("Save chart data to tab-separated text file");
        saveButton.addActionListener(e -> saveWaveData());
        controlsPanel.add(saveButton);
        waveDataDir = getDefaultWaveDataDir();

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
        gbc.weightx = 1.0;
        gbc.gridy = 2;
        add(cursorPanel2, gbc);

        Model.getMemoryModel().addMemoryEventListener(RunnerView.HANDTAP_VIEW, this);
        Model.getMemoryModel().addLongProcessEventListener(this);
    }


    private void repaintCursor() {
        cursorPanel1.repaint();
        cursorPanel2.repaint();
    }


    /**
     * Extract a resource directory (e.g. "matlab") from the classpath/JAR to the
     * base directory. Existing files are not overwritten (user customisations preserved).
     */
    private static void extractResourceDir(String resourceDir) {
        try {
            File baseDir = WorkstationConfig.getBaseDirectory();
            File targetDir = new File(baseDir, resourceDir);
            if (!targetDir.exists()) {
                targetDir.mkdirs();
            }
            // List all entries in the resource directory
            java.net.URL dirUrl = HandTapView.class.getClassLoader().getResource(resourceDir);
            if (dirUrl == null) return;
            if ("jar".equals(dirUrl.getProtocol())) {
                // Running from JAR — scan the JAR entries
                String jarEntryPrefix = resourceDir + "/";
                java.util.jar.JarFile jar = ((java.net.JarURLConnection) dirUrl.openConnection()).getJarFile();
                java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    java.util.jar.JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (!name.startsWith(jarEntryPrefix) || name.length() == jarEntryPrefix.length()) continue;
                    String relativePath = name.substring(jarEntryPrefix.length());
                    File outFile = new File(targetDir, relativePath);
                    if (entry.isDirectory()) {
                        outFile.mkdirs();
                    } else if (!outFile.exists()) {
                        try (InputStream is = jar.getInputStream(entry)) {
                            Files.copy(is, outFile.toPath());
                        }
                    }
                }
            } else {
                // Running from IDE (filesystem) — copy only if target doesn't exist
                File srcDir = new File(dirUrl.toURI());
                for (File srcFile : srcDir.listFiles()) {
                    File outFile = new File(targetDir, srcFile.getName());
                    if (srcFile.isFile() && !outFile.exists()) {
                        Files.copy(srcFile.toPath(), outFile.toPath());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to extract resource dir: " + resourceDir + " — " + e.getMessage());
        }
    }

    private static File getDefaultWaveDataDir() {
        String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_WAVE_DATA);
        // Auto-fix: if old default user_generated_files or empty was saved, override to wave_data
        if (path == null || path.isEmpty() || path.endsWith("user_generated_files")) {
            path = null;
        }
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (!f.isAbsolute()) {
                f = new File(WorkstationConfig.getBaseDirectory(), path);
            }
            // Ensure it's treated as a directory
            if (!f.exists()) {
                f.mkdirs();
            }
            return f.isDirectory() ? f : f.getParentFile();
        }
        return new File(WorkstationConfig.getBaseDirectory(), "wave_data");
    }

    private void saveWaveData() {
        // Refresh directory from Path panel config
        String configPath = WorkstationConfig.getProperty(ConfProp.FILE_PATH_WAVE_DATA);
        if (configPath != null && !configPath.isEmpty()) {
            File f = new File(configPath);
            if (!f.isAbsolute()) {
                f = new File(WorkstationConfig.getBaseDirectory(), configPath);
            }
            waveDataDir = f.isDirectory() ? f : f.getParentFile();
        }

        if (lastFieldNames == null || lastFieldNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No data to save. Load data first.",
                    "Save", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File targetFile;
        if (!wavePathSelected) {
            // First time — show dialog
            if (!waveDataDir.exists()) {
                waveDataDir.mkdirs();
            }
            JFileChooser fileChooser = new JFileChooser(waveDataDir);
            fileChooser.setDialogTitle("Save wave data");
            fileChooser.setSelectedFile(new File(waveDataDir, generateFileName()));
            if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            targetFile = fileChooser.getSelectedFile();
            waveDataDir = targetFile.getParentFile();
            wavePathSelected = true;
        } else {
            // Subsequent saves — generate new file with timestamp in same directory
            targetFile = new File(waveDataDir, generateFileName());
        }

        // Write file
        try (FileWriter writer = new FileWriter(targetFile)) {
            // Header
            writer.write("# HandTap data\n");
            writer.write("# Addr: " + ((AddrItem) addressCombo.getSelectedItem()).addr
                    + "  H" + (h2RB.isSelected() ? "2" : "1")
                    + "  Trig: " + triggeredCHB.isSelected()
                    + "  Threshold: " + thresholdSpinner.getValue()
                    + "\n");
            writer.write(String.join("\t", lastFieldNames) + "\n");

            // Data rows — tab-separated hex values
            int len = getMaxLength();
            for (int i = 0; i < len; i++) {
                StringBuilder sb = new StringBuilder();
                for (int f = 0; f < lastFieldNames.size(); f++) {
                    if (f > 0) sb.append("\t");
                    String name = lastFieldNames.get(f);
                    double[] arr = lastNumberData.containsKey(name) ? lastNumberData.get(name) : lastSignalData.get(name);
                    if (arr != null && i < arr.length) {
                        sb.append(String.format("%07X", ((long) arr[i]) & 0xFFFFFFF));
                    } else {
                        sb.append("");
                    }
                }
                sb.append("\n");
                writer.write(sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to save: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        System.out.println("Wave data saved: " + targetFile.getAbsolutePath());
    }

    private int getMaxLength() {
        int max = 0;
        for (double[] arr : lastNumberData.values()) {
            if (arr != null && arr.length > max) max = arr.length;
        }
        for (double[] arr : lastSignalData.values()) {
            if (arr != null && arr.length > max) max = arr.length;
        }
        return max;
    }

    private String generateFileName() {
        AddrItem item = (AddrItem) addressCombo.getSelectedItem();
        String addr = item != null ? String.valueOf(item.addr) : "0";
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        return "wave_addr" + addr + "_" + ts + ".txt";
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
            onOffButton.setEnabled(false);
        } else {
            loadingDataButton.setText("Loading...");
            isRunGraphRefreshing = false;
            onOffButton.setEnabled(true);
        }
    }

    @Override
    public List<Integer> getListAddresses() {
        List<Integer> list = new ArrayList<>();
        int address = ((AddrItem) addressCombo.getSelectedItem()).addr;
        if(triggeredCHB.isSelected()){
            address = address | 0x08;
        }
        if(h2RB.isSelected()){
            address = address | 0x10;
        }
        list.add(address);
        return list;
    }
    @Override
    public void updateValues() {
        List <Integer> memoryValues = Model.getMemoryModel().getRespValues();
        List <Integer> list28bValues = orderValues(covert1bbTo28b(memoryValues));

        AddrCpuHand addrDef = AddrCpuHand.getCpuHandAddrDef(Model.getMemoryModel().getRespAddress());
        List<HandValueSetting> handValueSettings = HandValueSetting.getHandValueSettings(addrDef);
        Map<String, double [] > mapValuesNumber = new HashMap<>();
        Map<String, double [] > mapValues1BitSignal = new HashMap<>();

        int dobavka = 0;
        for(HandValueSetting setting : handValueSettings ){
            if(setting.getLsb() == setting.getMsb()){
                mapValues1BitSignal.put(setting.name(), getData(setting, list28bValues, list28bValues.size(),(dobavka=dobavka+2)));
            } else {
                mapValuesNumber.put(setting.name(), getData(setting, list28bValues, list28bValues.size(),0 ));
            }
        }

        setDataToChart(xyPanel1.getChart(), mapValuesNumber);
        setDataToChart(xyPanel2.getChart(), mapValues1BitSignal);
        updateYRangeFromData(mapValuesNumber);

        // Store data for Save to file
        lastNumberData = new LinkedHashMap<>(mapValuesNumber);
        lastSignalData = new LinkedHashMap<>(mapValues1BitSignal);
        lastFieldNames = new ArrayList<>();
        lastFieldNames.addAll(mapValuesNumber.keySet());
        lastFieldNames.addAll(mapValues1BitSignal.keySet());

        xyPanel1.revalidate();
        xyPanel2.revalidate();
        xyPanel2.repaint();
        xyPanel1.repaint();
        repaintCursor();
    }

    private void applyManualYRange() {
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
                    WorkstationConfig.setProperty("HANDTAP_YRANGE", String.valueOf(val));
                    WorkstationConfig.storeProperties();
                }
            } catch (NumberFormatException ignored) {}
        }
        xyPanel1.revalidate(); xyPanel1.repaint();
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

    private List<Integer> covert1bbTo28b(List<Integer> inList){
        List<Integer> list = new ArrayList<>();
        int len = inList.size()/2;
        for(int i=0; i<len; i++){
            int val = inList.get(2*i) + (inList.get(2*i+1)<<14);
            list.add(val);
        }
        return list;
    }

    private List<Integer> orderValues(List<Integer> inList){
        List<Integer> list = new ArrayList<>();
        for(int i=63; i>=0; i--){
            list.add(inList.get(i));
        }
        for(int i=191; i>=64; i--){
            list.add(inList.get(i));
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

                // Position: top chart -> first series LEFT, second RIGHT
                //            bottom chart -> all RIGHT, offset vertically
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
}
