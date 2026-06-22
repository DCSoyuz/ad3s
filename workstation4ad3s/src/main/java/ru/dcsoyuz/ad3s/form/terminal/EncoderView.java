package ru.dcsoyuz.ad3s.form.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.AllRegAddr;
import ru.dcsoyuz.ad3s.model.uart.ic.McuCommand;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketToIc;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketIcHelper;
import ru.dcsoyuz.ad3s.form.editor.ILongProcessEventListener;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;

/**
 * Encoder View - управление энкодерами.
 * При Start Reading: читает ResCntrl и KonturStngs, выставляет поля + Enc_en, записывает обратно.
 * При Stop / закрытии окна: снимает Enc_en.
 */
public class EncoderView extends JPanel implements ILongProcessEventListener {

    private static final Logger logger = LoggerFactory.getLogger(EncoderView.class);

    private static final int FIELD_ROW_WIDTH = 120;
    private static final int FIELD_ROW_HEIGHT = 18;
    private static final int COLS_GAP = 20;

    // Defaults
    private static final int DEF_ENC_PRESC = 0;
    private static final int DEF_VEL_RES = 0;
    private static final int DEF_COORD_RES = 10;
    private static final int DEF_LBW = 10;

    private final Preferences prefs = Preferences.userNodeForPackage(EncoderView.class);

    private JButton readValuesButton;
    private JTextField enc1AngleField;
    private JTextField enc2AngleField;
    private JTextField c1CoordField;
    private JTextField c2CoordField;
    private boolean readingEnabled = false;
    private Timer readTimer;

    // ResCntrl fields
    private JComboBox<String> encPresc1Combo;
    private JSpinner velRes1Spinner;
    private JSpinner coordRes1Spinner;
    private JComboBox<String> encPresc2Combo;
    private JSpinner velRes2Spinner;
    private JSpinner coordRes2Spinner;
    private int c1ResCntrlValue = 0;
    private int c2ResCntrlValue = 0;

    // KonturStngs / LBW fields
    private JSpinner lbw1Spinner;
    private JSpinner lbw2Spinner;
    private int c1KonturStngsValue = 0;
    private int c2KonturStngsValue = 0;

    private boolean updatingFields = false;
    private boolean encoderOwnsProcessing = false;

    public EncoderView() {
        setLayout(new BorderLayout(10, 10));

        // Основная панель с сеткой
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int fieldWidth = 80;

        // Кнопка Start/Stop Reading
        readValuesButton = new JButton("Start Reading");
        readValuesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleReading();
            }
        });
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 4;
        mainPanel.add(readValuesButton, gbc);
        gbc.gridwidth = 1;

        // Row 1: ENC1 Angle | ENC2 Angle
        enc1AngleField = createReadOnlyField(fieldWidth);
        gbc.gridx = 0; gbc.gridy = 1;
        mainPanel.add(new JLabel("ENC1 Angle:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(enc1AngleField, gbc);

        enc2AngleField = createReadOnlyField(fieldWidth);
        gbc.gridx = 2; gbc.gridy = 1;
        mainPanel.add(new JLabel("ENC2 Angle:"), gbc);
        gbc.gridx = 3;
        mainPanel.add(enc2AngleField, gbc);

        // Row 2: C1 Coord | C2 Coord
        c1CoordField = createReadOnlyField(fieldWidth);
        gbc.gridx = 0; gbc.gridy = 2;
        mainPanel.add(new JLabel("C1 Coord:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(c1CoordField, gbc);

        c2CoordField = createReadOnlyField(fieldWidth);
        gbc.gridx = 2; gbc.gridy = 2;
        mainPanel.add(new JLabel("C2 Coord:"), gbc);
        gbc.gridx = 3;
        mainPanel.add(c2CoordField, gbc);

        add(mainPanel, BorderLayout.NORTH);

        // Settings panel with C1 and C2 columns
        add(createSettingsPanel(), BorderLayout.CENTER);

        Model.getMemoryModel().addLongProcessEventListener(this);

        loadPrefs();
    }

    // ---- Settings panel construction ----

    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, COLS_GAP, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 5, 5, 5));

        panel.add(createConverterColumn(true));
        panel.add(createConverterColumn(false));

        return panel;
    }

    private JPanel createConverterColumn(boolean isC1) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x6BB3F0), 1),
                isC1 ? "C1" : "C2");
        border.setTitleColor(Color.BLACK);
        border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD, 11f));
        panel.setBorder(border);
        panel.setBackground(new Color(0x4A6A8C));

        // Enc_presc (bits [14:12], range 8)
        String[] prescItems = new String[]{"000", "001", "010", "011", "100", "101", "110", "111"};
        JComboBox<String> prescCombo = new JComboBox<>(prescItems);
        prescCombo.setSelectedIndex(0);
        prescCombo.setPreferredSize(new Dimension(50, 18));
        prescCombo.setBackground(new Color(0xE8F4F8));
        prescCombo.setForeground(Color.BLACK);
        prescCombo.addActionListener(e -> {
            if (!updatingFields && readingEnabled) writeResCntrl(isC1);
        });
        panel.add(createFieldRow("Enc_presc", prescCombo));

        // Vel_resolution (bits [8:5], range 16, default 0)
        JSpinner velSpinner = createResSpinner(DEF_VEL_RES);
        velSpinner.addChangeListener(e -> {
            if (!updatingFields && readingEnabled) writeResCntrl(isC1);
        });
        panel.add(createFieldRow("Vel_res", velSpinner));

        // Coord_resolution (bits [3:0], range 16, default 10)
        JSpinner coordSpinner = createResSpinner(DEF_COORD_RES);
        coordSpinner.addChangeListener(e -> {
            if (!updatingFields && readingEnabled) writeResCntrl(isC1);
        });
        panel.add(createFieldRow("Coord_res", coordSpinner));

        // LBW (KonturStngs bits [4:0], range 32, default 10)
        JSpinner lbwSpinner = createResSpinner(DEF_LBW);
        lbwSpinner.addChangeListener(e -> {
            if (!updatingFields && readingEnabled) writeKonturStngs(isC1);
        });
        panel.add(createFieldRow("LBW", lbwSpinner));

        // Store references
        if (isC1) {
            encPresc1Combo = prescCombo;
            velRes1Spinner = velSpinner;
            coordRes1Spinner = coordSpinner;
            lbw1Spinner = lbwSpinner;
        } else {
            encPresc2Combo = prescCombo;
            velRes2Spinner = velSpinner;
            coordRes2Spinner = coordSpinner;
            lbw2Spinner = lbwSpinner;
        }

        return panel;
    }

    private JTextField createReadOnlyField(int width) {
        JTextField field = new JTextField();
        field.setEditable(false);
        field.setBackground(new Color(0xE8F0FA));
        field.setForeground(Color.GRAY);
        field.setPreferredSize(new Dimension(width, 22));
        return field;
    }

    private JSpinner createResSpinner(int defaultValue) {
        SpinnerNumberModel model = new SpinnerNumberModel(defaultValue, null, null, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setBackground(new Color(0xE8F4F8));
        spinner.setForeground(Color.BLACK);
        spinner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        spinner.setPreferredSize(new Dimension(38, 18));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setFont(new Font("Consolas", Font.PLAIN, 11));
            tf.setHorizontalAlignment(SwingConstants.LEFT);
            tf.setBorder(null);
            tf.setMargin(new Insets(0, 0, 0, 0));
            editor.setBorder(null);
        }
        return spinner;
    }

    private JPanel createFieldRow(String labelText, JComponent editor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(new Color(0x4A6A8C));
        row.setPreferredSize(new Dimension(FIELD_ROW_WIDTH, FIELD_ROW_HEIGHT));
        row.setMaximumSize(new Dimension(FIELD_ROW_WIDTH, FIELD_ROW_HEIGHT));

        JLabel label = new JLabel(labelText);
        label.setBackground(new Color(0x4A6A8C));
        label.setForeground(Color.BLACK);
        label.setFont(label.getFont().deriveFont(11f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 1, 0, 1));
        row.add(label, BorderLayout.LINE_START);
        row.add(editor, BorderLayout.LINE_END);

        return row;
    }

    // ---- Reading / writing logic ----

    private void toggleReading() {
        if (readingEnabled) {
            stopReading();
        } else {
            startReading();
        }
    }

    /**
     * Start Reading:
     * 1. Читает ResCntrl, KonturStngs
     * 2. Выставляет биты из полей панели + Enc_en=1
     * 3. Записывает обратно
     * 4. Запускает таймер опроса
     */
    private void startReading() {
        if (Model.getMemoryModel().isProcessing()) return;

        readingEnabled = true;
        encoderOwnsProcessing = true;
        Model.getMemoryModel().setExternalProcessing(true);
        readValuesButton.setText("Stop Reading");

        // --- C1 ---
        int c1Rc = readSpiValueSync(AllRegAddr.C1ResCntrl.getAddress());
        c1ResCntrlValue = applyResCntrlFromPanel(c1Rc, true, true);
        writeSpiValue(AllRegAddr.C1ResCntrl.getAddress(), c1ResCntrlValue);
        setResCntrlFields(c1ResCntrlValue, true);

        int c1Ks = readSpiValueSync(AllRegAddr.C1KonturStngs.getAddress());
        c1KonturStngsValue = applyLbwToKontur(c1Ks, true);
        writeSpiValue(AllRegAddr.C1KonturStngs.getAddress(), c1KonturStngsValue);
        setKonturStngsFields(c1KonturStngsValue, true);

        // --- C2 ---
        int c2Rc = readSpiValueSync(AllRegAddr.C2ResCntrl.getAddress());
        c2ResCntrlValue = applyResCntrlFromPanel(c2Rc, false, true);
        writeSpiValue(AllRegAddr.C2ResCntrl.getAddress(), c2ResCntrlValue);
        setResCntrlFields(c2ResCntrlValue, false);

        int c2Ks = readSpiValueSync(AllRegAddr.C2KonturStngs.getAddress());
        c2KonturStngsValue = applyLbwToKontur(c2Ks, false);
        writeSpiValue(AllRegAddr.C2KonturStngs.getAddress(), c2KonturStngsValue);
        setKonturStngsFields(c2KonturStngsValue, false);

        // Включение энкодера в MCU
        Model.getMemoryModel().setEncoderEnabled(true);

        readTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readAllValues();
            }
        });
        readTimer.start();
    }

    private void stopReading() {
        readingEnabled = false;
        readValuesButton.setText("Start Reading");

        if (readTimer != null) {
            readTimer.stop();
            readTimer = null;
        }

        disableEncoder();

        encoderOwnsProcessing = false;
        Model.getMemoryModel().setExternalProcessing(false);
    }

    private void disableEncoder() {
        c1ResCntrlValue &= ~(1 << 15);
        writeSpiValue(AllRegAddr.C1ResCntrl.getAddress(), c1ResCntrlValue);

        c2ResCntrlValue &= ~(1 << 15);
        writeSpiValue(AllRegAddr.C2ResCntrl.getAddress(), c2ResCntrlValue);

        Model.getMemoryModel().setEncoderEnabled(false);
    }

    private void readAllValues() {
        readEncoderValues();
        readCoords();
        readResCntrlValues();
        readKonturStngsValues();
    }

    // ---- Encoder values reading ----

    private void readEncoderValues() {
        PacketToIc packet = new PacketToIc(McuCommand.READ_ENCODERS, 0, 0, null);
        byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);

        try {
            Thread responseAction = Model.getUartModel().doExchangePacket(bytes);
            responseAction.join(1000);

            byte[] response = Model.getUartModel().getResponse();
            if (response != null && response.length >= 17) {
                int enc1Value = (response[8] & 0xFF) |
                               ((response[9] & 0xFF) << 8) |
                               ((response[10] & 0xFF) << 16) |
                               ((response[11] & 0xFF) << 24);

                int enc2Value = (response[12] & 0xFF) |
                               ((response[13] & 0xFF) << 8) |
                               ((response[14] & 0xFF) << 16) |
                               ((response[15] & 0xFF) << 24);

                updateEnc1Angle(enc1Value);
                updateEnc2Angle(enc2Value);

                Model.getMemoryModel().updateEncoderValues(enc1Value, enc2Value);

                logger.debug("Encoder values: ENC1={}, ENC2={}", enc1Value, enc2Value);
            }
        } catch (InterruptedException e) {
            logger.error("Error reading encoder values", e);
        }
    }

    public void updateEnc1Angle(int angle) {
        SwingUtilities.invokeLater(() -> enc1AngleField.setText(String.valueOf(angle)));
    }

    public void updateEnc2Angle(int angle) {
        SwingUtilities.invokeLater(() -> enc2AngleField.setText(String.valueOf(angle)));
    }

    // ---- Coord reading ----

    private void readCoords() {
        readSpiValue(16, value -> SwingUtilities.invokeLater(() -> c1CoordField.setText(String.valueOf(value))));
        readSpiValue(48, value -> SwingUtilities.invokeLater(() -> c2CoordField.setText(String.valueOf(value))));
    }

    // ---- SPI read / write primitives ----

    private void readSpiValue(int address, SpiReadCallback callback) {
        PacketToIc packet = new PacketToIc(McuCommand.READ, address, 1, null);
        byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);

        try {
            Thread responseAction = Model.getUartModel().doExchangePacket(bytes);
            responseAction.join(1000);

            byte[] response = Model.getUartModel().getResponse();
            if (response != null && response.length >= 11) {
                int value = (response[8] & 0xFF) | ((response[9] & 0xFF) << 8);
                callback.onValueRead(value);
            }
        } catch (InterruptedException e) {
            logger.error("Error reading SPI value (callback)", e);
        }
    }

    private int readSpiValueSync(int address) {
        PacketToIc packet = new PacketToIc(McuCommand.READ, address, 1, null);
        byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);

        try {
            Thread responseAction = Model.getUartModel().doExchangePacket(bytes);
            responseAction.join(1000);

            byte[] response = Model.getUartModel().getResponse();
            if (response != null && response.length >= 11) {
                int value = (response[8] & 0xFF) | ((response[9] & 0xFF) << 8);
                logger.debug("SPI Read addr={}, value=0x{}", address, Integer.toHexString(value));
                return value;
            }
        } catch (InterruptedException e) {
            logger.error("Error reading SPI value (sync)", e);
        }
        return 0;
    }

    private void writeSpiValue(int address, int value) {
        PacketToIc packet = new PacketToIc(McuCommand.WRITE, address, value);
        byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);

        try {
            Thread responseAction = Model.getUartModel().doExchangePacket(bytes);
            responseAction.join(1000);
            logger.debug("SPI Write addr={}, value=0x{}", address, Integer.toHexString(value));
        } catch (InterruptedException e) {
            logger.error("Error writing SPI value", e);
        }
    }

    private interface SpiReadCallback {
        void onValueRead(int value);
    }

    // ---- ResCntrl helpers ----

    private void readResCntrlValues() {
        readSpiValue(AllRegAddr.C1ResCntrl.getAddress(), value -> {
            c1ResCntrlValue = value;
            SwingUtilities.invokeLater(() -> setResCntrlFields(value, true));
        });
        readSpiValue(AllRegAddr.C2ResCntrl.getAddress(), value -> {
            c2ResCntrlValue = value;
            SwingUtilities.invokeLater(() -> setResCntrlFields(value, false));
        });
    }

    private void setResCntrlFields(int regValue, boolean isC1) {
        updatingFields = true;
        try {
            int encPresc = (regValue >> 12) & 0x7;
            int velRes = (regValue >> 5) & 0xF;
            int coordRes = regValue & 0xF;

            if (isC1) {
                encPresc1Combo.setSelectedIndex(encPresc);
                velRes1Spinner.setValue(velRes);
                coordRes1Spinner.setValue(coordRes);
            } else {
                encPresc2Combo.setSelectedIndex(encPresc);
                velRes2Spinner.setValue(velRes);
                coordRes2Spinner.setValue(coordRes);
            }
        } finally {
            updatingFields = false;
        }
    }

    private int applyResCntrlFromPanel(int regValue, boolean isC1, boolean enableEnc) {
        // Enc_en (bit 15)
        if (enableEnc) {
            regValue |= (1 << 15);
        } else {
            regValue &= ~(1 << 15);
        }

        // Enc_presc bits [14:12]
        regValue &= ~(0x7 << 12);
        regValue |= (isC1 ? encPresc1Combo : encPresc2Combo).getSelectedIndex() << 12;

        // Vel_resolution bits [8:5]
        regValue &= ~(0xF << 5);
        regValue |= (Integer) (isC1 ? velRes1Spinner : velRes2Spinner).getValue() << 5;

        // Coord_resolution bits [3:0]
        regValue &= ~0xF;
        regValue |= (Integer) (isC1 ? coordRes1Spinner : coordRes2Spinner).getValue();

        return regValue;
    }

    private void writeResCntrl(boolean isC1) {
        int regValue = isC1 ? c1ResCntrlValue : c2ResCntrlValue;
        int addr = isC1 ? AllRegAddr.C1ResCntrl.getAddress() : AllRegAddr.C2ResCntrl.getAddress();

        // Enc_presc bits [14:12]
        regValue &= ~(0x7 << 12);
        regValue |= (isC1 ? encPresc1Combo : encPresc2Combo).getSelectedIndex() << 12;

        // Vel_resolution bits [8:5]
        regValue &= ~(0xF << 5);
        regValue |= (Integer) (isC1 ? velRes1Spinner : velRes2Spinner).getValue() << 5;

        // Coord_resolution bits [3:0]
        regValue &= ~0xF;
        regValue |= (Integer) (isC1 ? coordRes1Spinner : coordRes2Spinner).getValue();

        writeSpiValue(addr, regValue);

        if (isC1) c1ResCntrlValue = regValue;
        else c2ResCntrlValue = regValue;
    }

    // ---- KonturStngs / LBW helpers ----

    private void readKonturStngsValues() {
        readSpiValue(AllRegAddr.C1KonturStngs.getAddress(), value -> {
            c1KonturStngsValue = value;
            SwingUtilities.invokeLater(() -> setKonturStngsFields(value, true));
        });
        readSpiValue(AllRegAddr.C2KonturStngs.getAddress(), value -> {
            c2KonturStngsValue = value;
            SwingUtilities.invokeLater(() -> setKonturStngsFields(value, false));
        });
    }

    private void setKonturStngsFields(int regValue, boolean isC1) {
        updatingFields = true;
        try {
            // LBW: bits [4:0]
            int lbw = regValue & 0x1F;
            (isC1 ? lbw1Spinner : lbw2Spinner).setValue(lbw);
        } finally {
            updatingFields = false;
        }
    }

    private int applyLbwToKontur(int regValue, boolean isC1) {
        // LBW bits [4:0]
        regValue &= ~0x1F;
        regValue |= (Integer) (isC1 ? lbw1Spinner : lbw2Spinner).getValue();
        return regValue;
    }

    private void writeKonturStngs(boolean isC1) {
        int regValue = isC1 ? c1KonturStngsValue : c2KonturStngsValue;
        int addr = isC1 ? AllRegAddr.C1KonturStngs.getAddress() : AllRegAddr.C2KonturStngs.getAddress();

        // LBW bits [4:0]
        regValue &= ~0x1F;
        regValue |= (Integer) (isC1 ? lbw1Spinner : lbw2Spinner).getValue();

        writeSpiValue(addr, regValue);

        if (isC1) c1KonturStngsValue = regValue;
        else c2KonturStngsValue = regValue;
    }

    public void cleanup() {
        savePrefs();
        stopReading();
    }

    @Override
    public void updateStatusOfProcessing() {
        boolean isProcessing = Model.getMemoryModel().isProcessing();
        if (isProcessing && !encoderOwnsProcessing) {
            readValuesButton.setEnabled(false);
        } else {
            readValuesButton.setEnabled(true);
        }
    }

    // ---- Preferences persistence ----

    private void loadPrefs() {
        int encPresc = prefs.getInt("enc_presc", DEF_ENC_PRESC);
        int velRes = prefs.getInt("vel_res", DEF_VEL_RES);
        int coordRes = prefs.getInt("coord_res", DEF_COORD_RES);
        int lbw = prefs.getInt("lbw", DEF_LBW);

        updatingFields = true;
        try {
            encPresc1Combo.setSelectedIndex(encPresc);
            encPresc2Combo.setSelectedIndex(encPresc);
            velRes1Spinner.setValue(velRes);
            velRes2Spinner.setValue(velRes);
            coordRes1Spinner.setValue(coordRes);
            coordRes2Spinner.setValue(coordRes);
            lbw1Spinner.setValue(lbw);
            lbw2Spinner.setValue(lbw);
        } finally {
            updatingFields = false;
        }
    }

    private void savePrefs() {
        prefs.putInt("enc_presc", encPresc1Combo.getSelectedIndex());
        prefs.putInt("vel_res", (Integer) velRes1Spinner.getValue());
        prefs.putInt("coord_res", (Integer) coordRes1Spinner.getValue());
        prefs.putInt("lbw", (Integer) lbw1Spinner.getValue());
    }
}
