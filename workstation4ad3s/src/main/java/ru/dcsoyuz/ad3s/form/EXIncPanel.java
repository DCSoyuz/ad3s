package ru.dcsoyuz.ad3s.form;

import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.model.fpga.registers.IAllRegAddr;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * RegPanel for EXInc register that can split into EXInc[15:8] and EXInc[7:0]
 * when EXO_mode = 01 (meander output mode).
 */
public class EXIncPanel extends RegPanel {

    private JSpinner spinnerHi;
    private JSpinner spinnerLo;
    private JPanel microPanelHi;
    private JPanel microPanelLo;
    private boolean splitMode = false;

    public EXIncPanel(IAllRegAddr regAddr, IMemoryEventListener inFace) {
        super(regAddr, inFace);

        ChangeListener splitChangeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (splitMode) {
                    face.updateTableValue(EXIncPanel.this);
                }
            }
        };

        // HI bits [15:8]
        microPanelHi = new JPanel();
        microPanelHi.setLayout(new BorderLayout());
        microPanelHi.setBackground(new Color(0x4A6A8C));
        JLabel labelHi = new JLabel("EXInc[15:8]");
        labelHi.setBackground(new Color(0x4A6A8C));
        labelHi.setForeground(Color.BLACK);
        labelHi.setFont(labelHi.getFont().deriveFont(11f));
        labelHi.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        microPanelHi.add(labelHi, BorderLayout.LINE_START);
        microPanelHi.setPreferredSize(new Dimension(130, 16));
        microPanelHi.setMinimumSize(new Dimension(130, 16));
        spinnerHi = createSplitSpinner(splitChangeListener);
        microPanelHi.add(spinnerHi, BorderLayout.LINE_END);

        // LO bits [7:0]
        microPanelLo = new JPanel();
        microPanelLo.setLayout(new BorderLayout());
        microPanelLo.setBackground(new Color(0x4A6A8C));
        JLabel labelLo = new JLabel("EXInc[7:0]");
        labelLo.setBackground(new Color(0x4A6A8C));
        labelLo.setForeground(Color.BLACK);
        labelLo.setFont(labelLo.getFont().deriveFont(11f));
        labelLo.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
        microPanelLo.add(labelLo, BorderLayout.LINE_START);
        microPanelLo.setPreferredSize(new Dimension(130, 16));
        microPanelLo.setMinimumSize(new Dimension(130, 16));
        spinnerLo = createSplitSpinner(splitChangeListener);
        microPanelLo.add(spinnerLo, BorderLayout.LINE_END);

        microPanelHi.setVisible(false);
        microPanelLo.setVisible(false);
        add(microPanelHi);
        add(microPanelLo);
    }

    private JSpinner createSplitSpinner(ChangeListener listener) {
        SpinnerNumberModel model = new SpinnerNumberModel(0, null, null, 1);
        JSpinner spinner = new JSpinner(model);
        spinner.setBackground(new Color(0xE8F4F8));
        spinner.setForeground(Color.BLACK);
        spinner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        spinner.setPreferredSize(new Dimension(46, 18));
        spinner.setMinimumSize(new Dimension(46, 18));
        spinner.addChangeListener(listener);
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
            tf.setFont(new Font("Consolas", Font.PLAIN, 11));
            tf.setHorizontalAlignment(SwingConstants.LEFT);
            tf.setBorder(null);
            tf.setMargin(new Insets(0, 0, 0, 0));
            editor.setBorder(null);
            DecimalFormat fmt = new DecimalFormat("#");
            fmt.setGroupingUsed(false);
            NumberFormatter nfmt = new NumberFormatter(fmt);
            nfmt.setValueClass(Integer.class);
            tf.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
                public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                    return nfmt;
                }
            });
        }
        return spinner;
    }

    public void setSplitMode(boolean split) {
        if (this.splitMode == split) return;
        this.splitMode = split;

        // The single-value microPanel is the first component added by RegPanel
        Component singlePanel = getComponent(0);

        if (split) {
            int value = (Integer) spinnerValueWithoutFields.getValue();
            spinnerHi.setValue((value >> 8) & 0xFF);
            spinnerLo.setValue(value & 0xFF);
            singlePanel.setVisible(false);
            microPanelHi.setVisible(true);
            microPanelLo.setVisible(true);
            TitledBorder border = BorderFactory.createTitledBorder(
                    BorderFactory.createLineBorder(new Color(0x6BB3F0), 1),
                    regAddr.getDisplayName());
            border.setTitleColor(Color.BLACK);
            border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD, 11f));
            setBorder(border);
            setBackground(new Color(0x4A6A8C));
        } else {
            int hi = (Integer) spinnerHi.getValue();
            int lo = (Integer) spinnerLo.getValue();
            spinnerValueWithoutFields.setValue((hi << 8) | lo);
            microPanelHi.setVisible(false);
            microPanelLo.setVisible(false);
            singlePanel.setVisible(true);
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        }

        revalidate();
        repaint();
    }

    @Override
    public int getViewValue() {
        if (splitMode) {
            int hi = (Integer) spinnerHi.getValue();
            int lo = (Integer) spinnerLo.getValue();
            return (hi << 8) | lo;
        }
        return super.getViewValue();
    }

    @Override
    public void setViewValue(int value) {
        if (splitMode) {
            spinnerHi.setValue((value >> 8) & 0xFF);
            spinnerLo.setValue(value & 0xFF);
        }
        super.setViewValue(value);
    }
}
