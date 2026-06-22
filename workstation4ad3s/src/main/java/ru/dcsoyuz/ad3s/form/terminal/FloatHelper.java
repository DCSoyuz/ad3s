package ru.dcsoyuz.ad3s.form.terminal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.AppFrameHelper;
import ru.dcsoyuz.ad3s.form.GridBagHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Enumeration;

/**
 * Created by yuri.filatov on 26.01.2024.
 */
public class FloatHelper extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(FloatHelper.class);

    private static final String CONFIG_COUNT_KEY = "FLOAT_HELPER_COUNT";
    private int nextIndex;

    public FloatHelper() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // Buttons
        JPanel toolbar = new JPanel();
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        JButton btnAdd = AppFrameHelper.createButton(null, "+", "Add converter panel");
        JButton btnRemove = AppFrameHelper.createButton(null, "-", "Remove last converter panel");
        toolbar.add(btnAdd);
        toolbar.add(btnRemove);
        toolbar.add(Box.createHorizontalGlue());
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Load saved panel count (default 1)
        String countStr = WorkstationConfig.getProperty(CONFIG_COUNT_KEY);
        int count = 1;
        if (countStr != null && !countStr.isEmpty()) {
            try { count = Integer.parseInt(countStr); } catch (NumberFormatException ignored) {}
        }
        if (count < 1) count = 1;

        nextIndex = 0;
        for (int i = 0; i < count; i++) {
            add(createPathRow(nextIndex));
            nextIndex++;
        }
        add(toolbar);

        btnAdd.addActionListener(e -> {
            int insertPos = getComponentCount() - 1; // before toolbar
            add(createPathRow(nextIndex), insertPos);
            nextIndex++;
            savePanelCount();
            revalidate();
            repaint();
        });

        btnRemove.addActionListener(e -> {
            int panelCount = getComponentCount() - 1; // exclude toolbar
            if (panelCount <= 1) return;
            int removedIndex = nextIndex - 1;
            clearConfigForIndex(removedIndex);
            remove(panelCount - 1); // remove last panel (before toolbar)
            nextIndex--;
            savePanelCount();
            revalidate();
            repaint();
        });
    }

    private void savePanelCount() {
        WorkstationConfig.setProperty(CONFIG_COUNT_KEY, String.valueOf(getComponentCount() - 1));
        WorkstationConfig.storeProperties();
    }

    private void clearConfigForIndex(int index) {
        String prefix = "FLOAT" + index;
        String[] suffixes = {"_INPUT_FLOAT_VALUE", "_SELECTED_HEX_TYPE", "_COARSE_DOUBLE",
                "_REMAINDER_DOUBLE", "_OUT_FLOAT_HEX", "_COARSE_HEX", "_REMAINDER_HEX", "_OUTPUT_FLOAT"};
        for (String suffix : suffixes) {
            WorkstationConfig.setProperty(prefix + suffix, "");
        }
        for (int i = 0; i < 4; i++) {
            WorkstationConfig.setProperty(prefix + "_TABLE_" + i, "");
        }
        WorkstationConfig.storeProperties();
    }

    private JPanel createPathRow(int index) {
        String convName = "FLOAT" + index;
        ButtonGroup bg = new ButtonGroup();
        JRadioButton s14rb = createRadioButton(HexType.s14, bg);
        JRadioButton s28rb = createRadioButton(HexType.s28, bg);
        JRadioButton s56rb = createRadioButton(HexType.s56, bg);
        JRadioButton f28rb = createRadioButton(HexType.f28, bg);
        JRadioButton fd28rb = createRadioButton(HexType.fd28, bg);
        JRadioButton k14rb = createRadioButton(HexType.k14, bg);
        JRadioButton k28rb = createRadioButton(HexType.k24, bg);

        JTextField coarseDoubleTF = new JTextField();
        coarseDoubleTF.setColumns(6);
        coarseDoubleTF.setMinimumSize(new Dimension(72, 24));
        JTextField remainderDoubleTF = new JTextField();
        remainderDoubleTF.setColumns(6);
        remainderDoubleTF.setMinimumSize(new Dimension(72, 24));
        JTextField outFloatFromHexTF = new JTextField();
        outFloatFromHexTF.setColumns(6);
        outFloatFromHexTF.setMinimumSize(new Dimension(72, 24));

        JTextField remainderHexTF = new JTextField();
        remainderHexTF.setColumns(5);
        remainderHexTF.setMinimumSize(new Dimension(60, 24));
        JTextField coarseHexTF = new JTextField("");
        coarseHexTF.setColumns(5);
        coarseHexTF.setMinimumSize(new Dimension(60, 24));

        JPanel panel = new JPanel();

        panel.setLayout(new GridBagLayout());
        GridBagHelper helper = new GridBagHelper();
        JLabel label = new JLabel(convName.toLowerCase() + ":");
        label.setAlignmentY(Component.BOTTOM_ALIGNMENT);
        JTextField inputFloatValueTF = new JTextField("");

        inputFloatValueTF.setColumns(6);
        inputFloatValueTF.setMinimumSize(new Dimension(72, 24));
        inputFloatValueTF.setAlignmentY(Component.BOTTOM_ALIGNMENT);

        String configInputValue = WorkstationConfig.getProperty(convName + "_INPUT_FLOAT_VALUE");
        if (configInputValue != null && !configInputValue.isEmpty()) {
            inputFloatValueTF.setText(configInputValue);
        } else {
            inputFloatValueTF.setText("0");
        }
        String configHexType = WorkstationConfig.getProperty(convName + "_SELECTED_HEX_TYPE");
        if (configHexType != null && !configHexType.isEmpty()) {
            int mnemonic = Integer.parseInt(configHexType);
            getRB(bg, mnemonic).setSelected(true);
        } else {
            f28rb.setSelected(true);
        }
        JTextField outputFloatTF = new JTextField("");
        outputFloatTF.setColumns(6);
        outputFloatTF.setMinimumSize(new Dimension(72, 24));
        loadTextField(coarseDoubleTF, convName, "_COARSE_DOUBLE");
        loadTextField(remainderDoubleTF, convName, "_REMAINDER_DOUBLE");
        loadTextField(outFloatFromHexTF, convName, "_OUT_FLOAT_HEX");
        loadTextField(coarseHexTF, convName, "_COARSE_HEX");
        loadTextField(remainderHexTF, convName, "_REMAINDER_HEX");
        loadTextField(outputFloatTF, convName, "_OUTPUT_FLOAT");
        String[] columnNames = {"Hex value"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 4);
        JTable table = new JTable(tableModel);
        table.setPreferredScrollableViewportSize(new Dimension(80, 80));
        table.setShowGrid(true);
        table.setGridColor(new Color(0xBBBBBB));
        table.getColumnModel().getColumn(0).setMinWidth(60);
        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        loadTable(table, convName);
        panel.add(label, helper.get());
        panel.add(inputFloatValueTF, helper.rightColumn().get());
        panel.add(s14rb, helper.rightColumn().get());
        panel.add(s28rb, helper.downRow().get());
        panel.add(s56rb, helper.downRow().get());
        helper.upRow().upRow();
        panel.add(k14rb, helper.rightColumn().get());
        panel.add(k28rb, helper.downRow().get());
        helper.upRow();
        panel.add(f28rb, helper.rightColumn().get());
        panel.add(fd28rb, helper.downRow().get());
        helper.upRow();
        Action performConvFloatToHex = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                HexType hexType = getSelectedHexType(bg);
                WorkstationConfig.setProperty(convName + "_SELECTED_HEX_TYPE", String.valueOf(hexType.getMnemonic()));
                WorkstationConfig.setProperty(convName + "_INPUT_FLOAT_VALUE", inputFloatValueTF.getText());
                saveAllFields(convName, table, coarseDoubleTF, remainderDoubleTF, outFloatFromHexTF, coarseHexTF, remainderHexTF, outputFloatTF);

                DefaultTableModel tableModel = new DefaultTableModel(4, 1);
                String inpValueStr = inputFloatValueTF.getText();
                if (inpValueStr.equals("")) {
                    setError(table, tableModel);
                    logger.error("error: no value for parsing!");
                    return;
                }
                Double inValueDouble;
                try {
                    inValueDouble = Double.parseDouble(inpValueStr);
                } catch (NumberFormatException exception) {
                    setError(table, tableModel);
                    logger.error("Error! Incorrect number formatValue");
                    return;
                }

                switch (hexType) {
                    case k14:
                    case k24:
                        if (inValueDouble > 1 || inValueDouble < -1) {
                            logger.warn("input value must be in range [-1...1)");
                            return;
                        }
                        int numB = 0;
                        switch (hexType) {
                            case k14:
                                numB = 14;
                                break;
                            case k24:
                                numB = 24;
                                break;
                        }

                        int prevIntValue = (int) (inValueDouble * (Math.pow(2, numB - 1) - 1));
                        int intValue = (prevIntValue << (32 - numB)) >> (32 - numB);
                        double coarseV = (double) (intValue) / (Math.pow(2, numB - 1) - 1);
                        double remaindV = inValueDouble.floatValue() - coarseV;
                        coarseDoubleTF.setText(String.valueOf((float) coarseV));
                        remainderDoubleTF.setText(Float.toString((float) remaindV));
                        remainderHexTF.setText("");

                        String binaryStr = Integer.toBinaryString((intValue & ((int) Math.pow(2, numB) - 1)) | (1 << numB)).substring(1);
                        int intValue1 = Integer.parseInt(binaryStr, 2);
                        coarseHexTF.setText(Integer.toHexString((int) intValue1));
                        switch (hexType) {
                            case k14:
                                tableModel.setValueAt(getHexStr14bit(intValue1), 0, 0);
                                break;
                            case k24:
                                tableModel.setValueAt(getHexStr14bit(intValue1 & 0b11_1111_1111_1111), 0, 0);
                                tableModel.setValueAt(getHexStr14bit((intValue1 >> 14) & 0b11_1111_1111_1111), 1, 0);
                                break;
                        }

                        break;


                    case s14:
                    case s28:
                    case s56:
                        long longValue = inValueDouble.longValue();
                        double remainder = Double.sum(inValueDouble, -(double) longValue);
                        coarseDoubleTF.setText(String.valueOf(longValue));
                        remainderDoubleTF.setText(Double.toString(remainder));
                        long maxValue = 0;

                        switch (hexType) {
                            case s14:
                                maxValue = 8191l;
                                break;
                            case s28:
                                maxValue = 134217727l;
                                break;
                            case s56:
                                maxValue = 36028797018963967l;
                                break;
                        }

                        if (longValue > maxValue) {
                            setError(table, tableModel);
                            logger.warn("Value is more {}!", maxValue);
                            return;
                        } else if (longValue < -maxValue - 1) {
                            setError(table, tableModel);
                            logger.warn("Value is less {}!", -maxValue - 1);
                            return;
                        }


                        switch (hexType) {
                            case s14:
                                coarseHexTF.setText(getHexStrNbit(longValue, 14, 16));
                                tableModel.setValueAt(getHexStr14bit(longValue), 0, 0);
                                break;
                            case s28:
                                coarseHexTF.setText(getHexStrNbit(longValue, 28, 28));
                                tableModel.setValueAt(getHexStr14bit(longValue & 0b11_1111_1111_1111), 0, 0);
                                tableModel.setValueAt(getHexStr14bit((longValue >> 14) & 0b11_1111_1111_1111), 1, 0);
                                break;
                            case s56:
                                coarseHexTF.setText(getHexStrNbit(longValue, 56, 56));
                                tableModel.setValueAt(getHexStr14bit(longValue & 0b11_1111_1111_1111), 0, 0);
                                tableModel.setValueAt(getHexStr14bit((longValue >> 14) & 0b11_1111_1111_1111), 1, 0);
                                tableModel.setValueAt(getHexStr14bit((longValue >> 28) & 0b11_1111_1111_1111), 2, 0);
                                tableModel.setValueAt(getHexStr14bit((longValue >> 42) & 0b11_1111_1111_1111), 3, 0);
                                break;
                        }

                        break;
                    case f28:
                    case fd28:
                        double coarseValue = extract28bitFloatValueFromDouble(inValueDouble, coarseHexTF, coarseDoubleTF, tableModel, 0);


                        double remainderAfterExtractCoarse = inValueDouble != 0.0 ? -(1 - inValueDouble / coarseValue) : 0.0;

                        if (hexType.equals(HexType.fd28)) {
                            double remainderValue = extract28bitFloatValueFromDouble(remainderAfterExtractCoarse, remainderHexTF, remainderDoubleTF, tableModel, 2);
                        }

                        break;
                }

                table.setModel(tableModel);
                fixTableColumnWidth(table);
                table.repaint();
            }
        };
        JButton buttonConvFloatToHex = AppFrameHelper.createButton(performConvFloatToHex, "toHex", "Converting float value to 14bit  hex values");
        panel.add(buttonConvFloatToHex, helper.rightColumn().get());

        panel.add(new JLabel("Coarse:"), helper.rightColumn().get());
        panel.add(new JLabel("Remainder:"), helper.downRow().get());

        helper.upRow();

        panel.add(coarseDoubleTF, helper.rightColumn().get());
        panel.add(remainderDoubleTF, helper.downRow().get());

        helper.upRow();


        panel.add(new JLabel("Coarse HEX:"), helper.rightColumn().get());
        panel.add(new JLabel("Remain HEX:"), helper.downRow().get());
        helper.upRow();
        panel.add(coarseHexTF, helper.rightColumn().get());
        panel.add(remainderHexTF, helper.downRow().get());
        helper.upRow();
        panel.add(new JLabel("T:"), helper.rightColumn().get());

        panel.add(table, helper.rightColumn().setGridHeight(4).get());
        Action performConvHexToFloat = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                HexType hexType = getSelectedHexType(bg);

                String hexStrValue = coarseHexTF.getText();
                if (!hexStrValue.equals("")) {
                    long value = Long.parseLong(hexStrValue, 16);
                    switch (hexType) {
                        case k14:
                            int intValue1 = (((int) value) << (32 - 14)) >> (32 - 14);
                            outFloatFromHexTF.setText(Float.toString((float) ((double) intValue1 / (Math.pow(2, 13) - 1))));
                            break;
                        case k24:
                            int intValue2 = (((int) value) << (32 - 24)) >> (32 - 24);
                            outFloatFromHexTF.setText(Float.toString((float) ((double) intValue2 / (Math.pow(2, 23) - 1))));
                            break;
                        case s14:
                            outFloatFromHexTF.setText(String.valueOf((value << (64 - 14)) >> (64 - 14)));
                            break;
                        case s28:
                            outFloatFromHexTF.setText(String.valueOf((value << (64 - 28)) >> (64 - 28)));
                            break;
                        case s56:
                            outFloatFromHexTF.setText(String.valueOf((value << (64 - 56)) >> (64 - 56)));
                            break;
                        case f28:
                            long sign = (value >> 27) & 0b1;
                            long fraction = (value & 0b1111_1111_1111_1111_1111);
                            long exponent = (value >> 20) & 0b111_1111;
                            ;
                            float fv = (sign == 1 ? -1.0f : 1.0f) * (fraction | 0b1_0000_0000_0000_0000_0000) * (float) Math.pow(2, exponent - 63 - 20);
                            outFloatFromHexTF.setText(String.valueOf(fv));
                            break;

                    }

                }


                switch (hexType) {
                    case k14:
                    case k24:
                    case s14:
                    case s28:
                    case s56:
                    case f28:
                    case fd28:
                        Object row1 = table.getValueAt(0, 0);
                        if (row1 == null) {
                            outputFloatTF.setText("error");
                            logger.warn("No value to 0 cell table");
                            return;
                        }
                        if (hexType == HexType.s14) {
                            String strValue = (String) row1;
                            outputFloatTF.setText(String.valueOf(Integer.parseInt(strValue, 16)));
                            return;
                        } else if (hexType == HexType.k14) {
                            String strValue = (String) row1;
                            int intValue1 = ((Integer.parseInt(strValue)) << (32 - 14)) >> (32 - 14);
                            outputFloatTF.setText(Float.toString((float) ((double) intValue1 / (Math.pow(2, 13) - 1))));
                        }
                        Object row2 = table.getValueAt(1, 0);
                        if (row2 == null) {
                            outputFloatTF.setText("error");
                            logger.warn("No value to 1 cell table");
                            return;
                        }
                        int v1 = Integer.parseInt((String) row1, 16);
                        int v2 = Integer.parseInt((String) row2, 16);
                        int v3 = (((v2 << 14) | v1) << 4) >> 4;
                        if (hexType == hexType.s28) {
                            outputFloatTF.setText(String.valueOf(v3));
                            return;
                        } else if (hexType == hexType.f28) {
                            int sign = v3 >> 27;
                            int fraction = v3 & 0b1111_1111_1111_1111_1111;
                            int exponenta = (v3 >> 20) & 0b111_1111;

                            float res_val = ((float) sign) * (fraction | 0b1_0000_0000_0000_0000_0000) * (float) Math.pow(2, exponenta - 63 - 20);
                            outputFloatTF.setText(String.valueOf(res_val));


                            return;
                        } else if (hexType == HexType.k24) {
                            int v11 = Integer.parseInt((String) row1, 16);
                            int v22 = Integer.parseInt((String) row2, 16);
                            int v33 = (((v22 << 14) | v11) << 8) >> 8;
                            float v44 = (float) ((float) v33 / (Math.pow(2, 23) - 1));
                            outputFloatTF.setText(String.valueOf(v44));
                        } else if (hexType == HexType.s56) {
                            Object row3 = table.getValueAt(2, 0);
                            if (row3 == null) {
                                row3 = "0";
                            }
                            Object row4 = table.getValueAt(3, 0);
                            if (row4 == null) {
                                row4 = "0";
                            }

                            long g1 = Integer.parseInt((String) row1, 16);
                            long g2 = Integer.parseInt((String) row2, 16);
                            long g3 = Integer.parseInt((String) row3, 16);
                            long g4 = Integer.parseInt((String) row4, 16);
                            long res_val = (((g4 << 42) | (g3 << 28) | (g2 << 14) | g1) << (64 - 56)) >> (64 - 56);
                            outputFloatTF.setText(Long.toString(res_val));
                        }


                }

                saveAllFields(convName, table, coarseDoubleTF, remainderDoubleTF, outFloatFromHexTF, coarseHexTF, remainderHexTF, outputFloatTF);
            }
        };
        helper.setRow(0).setGridHeight(1);
        JButton buttonConvHexToFloat = AppFrameHelper.createButton(performConvHexToFloat, "toFloat", "Converting hex values to float value");
        panel.add(buttonConvHexToFloat, helper.rightColumn().get());

        panel.add(new JLabel("T:"), helper.rightColumn().get());
        panel.add(new JLabel("C:"), helper.downRow().get());
        helper.upRow();
        panel.add(outputFloatTF, helper.rightColumn().get());
        panel.add(outFloatFromHexTF, helper.downRow().get());
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(BorderFactory.createTitledBorder(""));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, panel.getPreferredSize().height));
        return panel;
    }


    private double extract28bitFloatValueFromDouble(Double inValueDouble, JTextField hexTF, JTextField doubleTF, DefaultTableModel tableModel, int index) {

        float inValueFloat = inValueDouble.floatValue();
        int bitsFloat32 = Float.floatToIntBits(inValueFloat);
        int fractionFloat32 = bitsFloat32 & 0b111_1111_1111_1111_1111_1111;
        int exponentFloat32;
        int exponentFloat28 = 0;
        if (inValueFloat != 0) {
            exponentFloat32 = ((bitsFloat32 >> 23) & 0b1111_1111) - 127;
            exponentFloat28 = (exponentFloat32 + 63) & 0b111_1111;
        } else {
            exponentFloat32 = 0;
            exponentFloat28 = 0;
        }
        int signFloat32 = (bitsFloat32 >> 31) & 0b1;

        int fractionFloat28 = fractionFloat32 >> 3;

        int signFloat28 = signFloat32;

        int bitsFloat28 = (signFloat28 << 27) | (exponentFloat28 << 20) | fractionFloat28;

        tableModel.setValueAt(getHexStr14bit(bitsFloat28 & 0b11_1111_1111_1111), index, 0);
        tableModel.setValueAt(getHexStr14bit((bitsFloat28 >> 14) & 0b11_1111_1111_1111), index + 1, 0);


        hexTF.setText(getHexStrNbit(bitsFloat28, 28, 28));

        Float coarseValue = (signFloat28 == 1 ? -1f : 1f) * (float) (0b1_0000_0000_0000_0000_0000 | fractionFloat28) * (float) Math.pow(2, exponentFloat28 - 63 - 20);
        doubleTF.setText(Float.toString(coarseValue));

        return coarseValue;

    }

    private void setError(JTable table, DefaultTableModel tableModel) {
        tableModel.setValueAt("error", 0, 0);
        table.setModel(tableModel);
        fixTableColumnWidth(table);
        table.repaint();
    }

    private void loadTextField(JTextField tf, String convName, String suffix) {
        String val = WorkstationConfig.getProperty(convName + suffix);
        if (val != null && !val.isEmpty()) tf.setText(val);
    }

    private void loadTable(JTable table, String convName) {
        for (int i = 0; i < table.getRowCount(); i++) {
            String val = WorkstationConfig.getProperty(convName + "_TABLE_" + i);
            if (val != null) table.setValueAt(val, i, 0);
        }
    }

    private void saveAllFields(String convName, JTable table,
                               JTextField coarseDoubleTF, JTextField remainderDoubleTF,
                               JTextField outFloatFromHexTF, JTextField coarseHexTF,
                               JTextField remainderHexTF, JTextField outputFloatTF) {
        WorkstationConfig.setProperty(convName + "_COARSE_DOUBLE", coarseDoubleTF.getText());
        WorkstationConfig.setProperty(convName + "_REMAINDER_DOUBLE", remainderDoubleTF.getText());
        WorkstationConfig.setProperty(convName + "_OUT_FLOAT_HEX", outFloatFromHexTF.getText());
        WorkstationConfig.setProperty(convName + "_COARSE_HEX", coarseHexTF.getText());
        WorkstationConfig.setProperty(convName + "_REMAINDER_HEX", remainderHexTF.getText());
        WorkstationConfig.setProperty(convName + "_OUTPUT_FLOAT", outputFloatTF.getText());
        for (int i = 0; i < table.getRowCount(); i++) {
            Object v = table.getValueAt(i, 0);
            if (v != null) {
                WorkstationConfig.setProperty(convName + "_TABLE_" + i, v.toString());
            }
        }
        WorkstationConfig.storeProperties();
    }

    private void fixTableColumnWidth(JTable table) {
        if (table.getColumnCount() > 0) {
            table.getColumnModel().getColumn(0).setMinWidth(60);
            table.getColumnModel().getColumn(0).setPreferredWidth(80);
        }
    }

    private JRadioButton createRadioButton(HexType type, ButtonGroup bg) {
        JRadioButton radioButton = new JRadioButton(type.name());
        radioButton.setMnemonic(type.getMnemonic());
        bg.add(radioButton);
        return radioButton;
    }


    public String getHexStr14bit(long value) {
        return Long.toHexString((value & 0b11_1111_1111_1111) | 0b1_0000_0000_0000_0000).substring(1);
    }

    public String getHexStrNbit(long value, int numBit, int numBitHex) {
        return Long.toHexString(value & (((long) 1 << (numBit + 1)) - 1) | ((long) 1 << (numBitHex + 1))).substring(1);
    }


    HexType getSelectedHexType(ButtonGroup bg) {
        JRadioButton rb = getSelectedRB(bg);
        if (rb != null) {
            return HexType.getHexType(rb.getMnemonic());
        } else {
            return null;
        }
    }

    private JRadioButton getRB(ButtonGroup bg, int mnemonic) {
        for (Enumeration<AbstractButton> buttons = bg.getElements(); buttons.hasMoreElements(); ) {
            JRadioButton button = (JRadioButton) buttons.nextElement();
            if (button.getMnemonic() == mnemonic) {
                return button;
            }
        }
        return null;
    }

    JRadioButton getSelectedRB(ButtonGroup bg) {
        for (Enumeration<AbstractButton> buttons = bg.getElements(); buttons.hasMoreElements(); ) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return (JRadioButton) button;
            }
        }
        return null;
    }


    private enum HexType {
        s14(0),
        s28(1),
        s56(2),
        f28(3),
        fd28(4),
        k14(5),
        k24(6);

        int mnemonic;

        HexType(int mnemonic) {
            this.mnemonic = mnemonic;
        }

        public int getMnemonic() {
            return mnemonic;
        }

        public static HexType getHexType(int mnemonic) {
            for (HexType type : HexType.values()) {
                if (type.getMnemonic() == mnemonic) {
                    return type;
                }
            }
            return null;
        }
    }


}
