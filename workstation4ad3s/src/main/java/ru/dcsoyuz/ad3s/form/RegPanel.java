package ru.dcsoyuz.ad3s.form;



import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.model.fpga.registers.*;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.JFormattedTextField;
import java.text.DecimalFormat;
import javax.swing.text.NumberFormatter;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.dcsoyuz.ad3s.model.fpga.registers.RegValueType.VALUE_FIELDS;


public class RegPanel extends JPanel {

    IAllRegAddr regAddr;


    Map<IRegField, JComponent> mapFormFields;
    JSpinner spinnerValueWithoutFields;

    IMemoryEventListener face;
    //"Nimbus Sans L"

    public RegPanel(IAllRegAddr regAddr, IMemoryEventListener in_face){
        face = in_face;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS ));
        setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1)); // Уменьшенные отступы панели


        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateParentFace();
            }
        };

        this.regAddr = regAddr;
        IReg reg = regAddr.getReg();
        if(reg.getValueType() == VALUE_FIELDS){
            List<IRegField> fields = reg.getFields();
            mapFormFields = new HashMap<>();
            for(IRegField field : fields){
                int range = field.getRange();
                JPanel microPanel = new JPanel();
                microPanel.setLayout(new BorderLayout());
                microPanel.setBackground(new Color(0x4A6A8C)); // Светло-синий фон панели
                JLabel label = new JLabel(field.getDisplayName());
                label.setBackground(new Color(0x4A6A8C));
                label.setForeground(Color.BLACK);
                label.setFont(label.getFont().deriveFont(11f)); // Уменьшенный шрифт
                String indexes = field.getLsb() == field.getMsb() ? String.valueOf(field.getLsb()) : (String.valueOf(field.getMsb())+":" + String.valueOf(field.getLsb()));
                String header = field.getDisplayName() + " = " + regAddr.getDisplayName() + "[" + indexes  + "]\n";

                label.setToolTipText(AppFrameHelper.getHtml(header +
                        field.getDescription()));
                label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2)); // Горизонтальные отступы
                microPanel.add(label, BorderLayout.LINE_START);

                microPanel.setPreferredSize(new Dimension(150,16));
                microPanel.setMinimumSize(new Dimension(150,16));



                if(range <= 8){
                    String [] items;
                    int numBits = field.getMsb() - field.getLsb() + 1;
                    int[] valid = field.getValidValues();
                    if (valid != null) {
                        items = new String[valid.length];
                        for (int vi = 0; vi < valid.length; vi++) {
                            items[vi] = String.format("%" + numBits + "s", Integer.toBinaryString(valid[vi])).replace(' ', '0');
                        }
                    } else if(range == 2){
                        items = new String[]{"0", "1"};
                    } else if(range == 4){
                        items = new String[]{ "00", "01", "10", "11"};
                    } else {
                         items =new String[]{ "000", "001", "010", "011", "100", "101", "110", "111"};
                    }
                    JComboBox cmb = new JComboBox(items);
                    String defaultStr = String.format("%" + numBits + "s", Integer.toBinaryString(field.getDefaultValue())).replace(' ', '0');
                    cmb.setSelectedItem(defaultStr);

                    cmb.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {

                            updateParentFace();
                        }
                    });
                    cmb.setBackground(new Color(0xE8F4F8)); // Светло-голубой вместо белого
                    cmb.setForeground(Color.BLACK); // Черный текст
                    cmb.setPreferredSize(new Dimension(53, 18));
                    microPanel.add(cmb, BorderLayout.LINE_END);

                    mapFormFields.put(field, cmb);
                } else {
                    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(field.getDefaultValue(), null, null, 1);
                    JSpinner spinner = new JSpinner(spinnerModel);
                    spinner.setBackground(new Color(0xE8F4F8));
                    spinner.setForeground(Color.BLACK);
                    spinner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                    microPanel.add(spinner, BorderLayout.LINE_END);
                    spinner.addChangeListener(changeListener);
                    spinner.setPreferredSize(new Dimension(53, 18));
                    JComponent editor = spinner.getEditor();
                    if (editor instanceof JSpinner.DefaultEditor) {
                        JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                        tf.setFont(new Font("Consolas", Font.PLAIN, 11));
                        tf.setHorizontalAlignment(SwingConstants.LEFT);
                        tf.setBorder(null);
                        tf.setMargin(new Insets(0, 4, 0, 0));
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
                    mapFormFields.put(field, spinner);
                }
                //mapFormFields.get(field).addAncestorListener( new A);
                add(microPanel);
            }
        } else {
            JPanel microPanel = new JPanel();

            microPanel.setLayout(new BorderLayout());
            microPanel.setBackground(new Color(0x4A6A8C)); // Светло-синий фон панели

            JLabel label = new JLabel(regAddr.getDisplayName());
            label.setBackground(new Color(0x4A6A8C));
            label.setForeground(Color.BLACK);
            label.setFont(label.getFont().deriveFont(11f)); // Уменьшенный шрифт
            label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2)); // Горизонтальные отступы
            label.setToolTipText(AppFrameHelper.getHtml(regAddr.getReg().getDescription()));

            microPanel.add(label, BorderLayout.LINE_START);
            microPanel.setPreferredSize(new Dimension(130,16));
            microPanel.setMinimumSize(new Dimension(130,16));

            Integer value = reg.getDefaultValue();
            spinnerValueWithoutFields = new JSpinner(new SpinnerNumberModel(
                (value == null ? 0 : (int)value), null, null, 1));
            spinnerValueWithoutFields.setBackground(new Color(0xE8F4F8));
            spinnerValueWithoutFields.setForeground(Color.BLACK);
            spinnerValueWithoutFields.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            spinnerValueWithoutFields.setPreferredSize(new Dimension(53,18));
            spinnerValueWithoutFields.setMinimumSize(new Dimension(53,18));
            microPanel.add(spinnerValueWithoutFields, BorderLayout.LINE_END);
            spinnerValueWithoutFields.addChangeListener(changeListener);
            JComponent editor = spinnerValueWithoutFields.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                JFormattedTextField tf = ((JSpinner.DefaultEditor) editor).getTextField();
                tf.setFont(new Font("Consolas", Font.PLAIN, 11));
                tf.setHorizontalAlignment(SwingConstants.LEFT);
                tf.setBorder(null);
                tf.setMargin(new Insets(0, 4, 0, 0));
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
            add(microPanel);
        }
        if(regAddr.getReg().getValueType().equals(VALUE_FIELDS)) {
            // Светло-синяя рамка с черным текстом
            TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0x6BB3F0), 1),
                regAddr.getDisplayName()            );
            border.setTitleColor(Color.BLACK);
            border.setTitleFont(border.getTitleFont().deriveFont(Font.BOLD, 11f)); // Уменьшенный шрифт
            setBorder(border);
            setBackground(new Color(0x4A6A8C)); // Светло-синий фон
            setToolTipText(AppFrameHelper.getHtml(regAddr.getReg().getDescription()));
        } else {

        }
    }


    private void updateParentFace(){
        face.updateTableValue(this);

    }


    public int getViewValue(){
        if(regAddr.getReg().getValueType().equals(VALUE_FIELDS)){
            int sum = 0;
            for(Map.Entry<IRegField,JComponent> entry : mapFormFields.entrySet()){
                int value  = getFieldValue(entry.getKey(), entry.getValue());
                sum = sum + value ;
            }
            return sum;
        }else{
            Integer value = (Integer)spinnerValueWithoutFields.getValue();
            switch(regAddr.getReg().getValueType()){

                case VALUE15_4_UNSIGNED:
                    return value * 16;
                case VALUE11_0_SIGNED:
                    return value & 4095;
                default:
                    return value;
            }
        }
    }

    private int getFieldValue(IRegField field, JComponent component){
        int range = field.getRange();
        if( range <= 8){
            JComboBox cmb = (JComboBox) component;
            int value = Integer.parseInt((String) cmb.getSelectedItem(), 2);
            int value2 = value << field.getLsb();
            return value2;
        }else {
            JSpinner spinner = (JSpinner) component;
            int value = (Integer) spinner.getValue();
            int value2 = value << field.getLsb();
            return value2;
        }
    }

    private void setFieldValue(IRegField field, JComponent component, int value){
        int range = field.getRange();
        if( range <= 8){
            JComboBox cmb = (JComboBox) component;
            int numBits = field.getMsb() - field.getLsb() + 1;
            String target = String.format("%" + numBits + "s", Integer.toBinaryString(value)).replace(' ', '0');
            cmb.setSelectedItem(target);
        }else {
            JSpinner spinner = (JSpinner) component;
            int val = value;
            if(field.getFieldValueType() == FieldValueType.RANGE_SIGNED){
                val = (value << (32- field.getNumBits())) >> (32- field.getNumBits());
            }
            spinner.setValue(val);
        }
    }

    public void setViewValue(int value){
        if(regAddr.getReg().getValueType().equals(VALUE_FIELDS)){
            for(Map.Entry<IRegField,JComponent> entry : mapFormFields.entrySet()){
                int value2  = value & entry.getKey().getMask();
                int value3 = value2 >> entry.getKey().getLsb();
                setFieldValue(entry.getKey(), entry.getValue(), value3);

            }
        }else{
            int val1 = value;
            if(regAddr.getReg().getValueType().isSigned()){
                val1 = (value << 16) >> 16;
            }
            Integer value2 = val1 >> regAddr.getReg().getValueType().getLsb();
            spinnerValueWithoutFields.setValue(value2);

        }


    }





}
