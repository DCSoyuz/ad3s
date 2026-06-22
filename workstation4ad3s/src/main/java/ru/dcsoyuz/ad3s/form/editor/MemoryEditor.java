package ru.dcsoyuz.ad3s.form.editor;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.AppFrameHelper;
import ru.dcsoyuz.ad3s.form.GridBagHelper;
import ru.dcsoyuz.ad3s.form.editor.table.ExcelAdapter;
import ru.dcsoyuz.ad3s.form.RegPanel;
import ru.dcsoyuz.ad3s.form.terminal.Ad3sFace;
// import ru.dcsoyuz.ad3s.form.terminal.HTMLViewer;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.editor.FileHelper;
import ru.dcsoyuz.ad3s.model.fpga.registers.AllRegAddr;
import ru.dcsoyuz.ad3s.model.fpga.registers.FactoryGate;
import ru.dcsoyuz.ad3s.model.fpga.registers.RegField;
import ru.dcsoyuz.ad3s.model.fpga.registers.Regs;
import ru.dcsoyuz.ad3s.model.uart.RunnerView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

import static ru.dcsoyuz.ad3s.form.AppFrameHelper.HEIGHT_BUTTON;
import static ru.dcsoyuz.ad3s.form.AppFrameHelper.WIDTH_BUTTON;

/**
 * Created by yuri.filatov on 11.08.2016.
 */
public class MemoryEditor extends JPanel implements IMemoryEventListener, ILongProcessEventListener {


    private JTable table;
    private JButton readButton, writeButton;


    private JButton loadingIcButton;

    private JCheckBox stndbyCHB;
    private JCheckBox vpp9vCHB;

    private JCheckBox watchOTPCHB;

    private JCheckBox nresetCHB;

    private JButton readBaseRamIcButton, writeIcButton;
    private JTextField addressTextField;
    private JButton loadDefaultButton;


    private JButton storeValuesToTxtButton;
    private JButton createPokeButton;


    private JButton createEnvRegsButton;
    private JButton generateRdlButton;

    private JButton loadValuesFromTxtButton;

    private JButton loadBaseValuesFromHexButton;

    private JButton storeBaseValuesHexButton;

    private JButton createBOTPDataButton;

    private JButton createRomU22bDataButton;
    private JButton createRomF23bDataButton;

    private JButton progBOTPdataButton, progUOTPdataButton, progFactoryDataButton;
    private JButton verifyBOTPdataButton;

    private JButton writeCtrlOTPButton;


    JCheckBox leftButton_CPU1_en, leftButton_CPU2_en, leftButton_CONV1_en, leftButton_CONV2_en;

    JCheckBox  setMasterFpgaCheckBox;
    private JButton createDefineButton;



    private List<JButton> listUartButtons = new ArrayList<>();
    private List<JCheckBox> listUartcheckBox = new ArrayList<>();
    private Ad3sFace ad3sFace;
    public MemoryEditor( Ad3sFace face) {
        super();
        ad3sFace = face;
        setBorder(BorderFactory.createTitledBorder("Actions:"));

        GridBagHelper helper = new GridBagHelper();
        setLayout(new GridBagLayout());



        createTable();


        String savedAddr = WorkstationConfig.getProperty("MEMORY_EDITOR_ADDRESS");
        addressTextField = new JTextField(isValidAddress(savedAddr) ? savedAddr : "32");
        addressTextField.setAlignmentX(LEFT_ALIGNMENT);


        JScrollPane scrollPaneTable = new JScrollPane(table);


        table.setMinimumSize(new Dimension(WIDTH_BUTTON, 230));
        scrollPaneTable.setMinimumSize(new Dimension(WIDTH_BUTTON, 230));
        scrollPaneTable.setPreferredSize(new Dimension(WIDTH_BUTTON, 175));
        scrollPaneTable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        Action performReadMemory= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Integer address = Integer.parseInt(addressTextField.getText(), 10);
                saveAddress();
                if (address % 2 != 0) {
                    System.out.println("Warning: odd address " + address + ". Read/Write is only possible from even addresses (0, 2, 4, ...)");
                    return;
                }
                Model.getMemoryModel().setReqReadAddress(address);
                Model.getMemoryModel().setNumReadValues(8);
                if(!watchOTPCHB.isSelected()) {
                    Model.getMemoryModel().readValues();
                } else {
                    Model.getMemoryModel().readOTPvalues();

                }
            }
        };
        readButton = createButton(performReadMemory, "Read","Read memory from address" , true);

        Action performWriteMemory= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Integer address =Integer.parseInt(addressTextField.getText(), 10);
                saveAddress();
                if (address % 2 != 0) {
                    System.out.println("Warning: odd address " + address + ". Read/Write is only possible from even addresses (0, 2, 4, ...)");
                }
                {
                    Model.getMemoryModel().setReqValues(address, getTableValues());
                    Model.getMemoryModel().writeValues();
                }
            }
        };

        watchOTPCHB = new JCheckBox("watch OTP");
        watchOTPCHB.setSelected(false);



        writeButton = createButton(performWriteMemory, "Write","Write memory to address from this block", true );

        Action performLoadDefault= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.loadDefaultsValues();
            }
        };
        loadDefaultButton = createButton(performLoadDefault, "Load defaults", "Load fabric values for registers", true);

        Action performStoreTxt = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.storeValuesToTxt();
            }
        };
        storeValuesToTxtButton = createButton(performStoreTxt, "Store to txt", "Store all values to base_ram.txt", false);
        Action performGenerateRdl = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.generateRDLFile();
            }
        };
        generateRdlButton = createButton(performGenerateRdl, "Create RDL", "Generate RDL file with default values", false);

        Action performLoadBaseHex = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.loadBaseValuesFromHex();
            }
        };
        loadBaseValuesFromHexButton = createButton(performLoadBaseHex, "Load from hex", "Store all values to base_ram.hex", false);

        Action performStoreBaseHex = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.storeBaseValuesToHex();
            }
        };
        storeBaseValuesHexButton = createButton(performStoreBaseHex, "Store to hex", "Store all values to base_ram.hex", false);






        Action performGeneratePoke = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.generatePokeInitFile();
            }
        };
        createPokeButton = createButton(performGeneratePoke, "Create Poke", "Generate sv class with poke  init  current values", false);

        Action performCreateEnvRegs = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.generateEnvFiles();
            }
        };
        createEnvRegsButton = createButton(performCreateEnvRegs, "cr env regs", "Create env regs", false);
        Action performLoadTxt = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ad3sFace.loadValuesFromTxt();
            }
        };
        loadValuesFromTxtButton = createButton(performLoadTxt, "Load from txt", "Load all values from base_ram.txt" , true);

        if (Model.isFactoryMode()) {
            Action performCreateDefine = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    ad3sFace.createFileDef();
                }
            };
            createDefineButton = createButton(performCreateDefine, "Create def", "Create file ad3s_defines.v", false);
        }


        Action performReadBaseRamIc = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getMemoryModel().readBaseRamValues(RunnerView.TABLE_MAIN);
            }
        };
        ActionListener changeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JCheckBox checkBox = ((JCheckBox)e.getSource());
                RegField field = RegField.valueOf(checkBox.getText());
                boolean value = checkBox.isSelected();
                ad3sFace.setBooleanFieldValue(AllRegAddr.Mode_config, field, value);
                int  newValue = ad3sFace.getCurrentRegValue(AllRegAddr.Mode_config);
                List<Integer> list = Arrays.asList(newValue);
                Model.getMemoryModel().setReqValues(AllRegAddr.Mode_config.getAddress(), list);
                Model.getMemoryModel().writeValues();
            }


        };

        leftButton_CPU1_en  = new JCheckBox("CPU1_en");
        leftButton_CPU2_en = new JCheckBox("CPU2_en");
        leftButton_CONV1_en = new JCheckBox("CONV1_en");
        leftButton_CONV2_en = new JCheckBox("CONV2_en");

        leftButton_CPU1_en.addActionListener(changeListener);
        leftButton_CPU2_en.addActionListener(changeListener);
        leftButton_CONV1_en.addActionListener(changeListener);
        leftButton_CONV2_en.addActionListener(changeListener);
        setMasterFpgaCheckBox = new JCheckBox("MasteFpga");
        setMasterFpgaCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Model.getMemoryModel().setMasterFpgaMode(((JCheckBox)e.getSource()).isSelected());
            }
        });
        listUartcheckBox.add(leftButton_CPU1_en);
        listUartcheckBox.add(leftButton_CPU2_en);
        listUartcheckBox.add(leftButton_CONV1_en);
        listUartcheckBox.add(leftButton_CONV2_en);

        readBaseRamIcButton = createButton(performReadBaseRamIc, "Load from IC", "Load all values from chip ad3s", true);

        Action performWriteIc = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (Model.getMemoryModel().isCyclic()) {
                    Model.getMemoryModel().writeThenResumeCyclic(face.getTableValuesForWriteToIc());
                } else {
                    Model.getMemoryModel().setReqValues(face.getTableValuesForWriteToIc());
                    Model.getMemoryModel().writeValues();
                }
            }
        };
        writeIcButton = createButton(performWriteIc, "Write to IC", "Store all values to chip", false);
        Action performLoadingIc = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(Model.getMemoryModel().isProcessing()){
                   Model.getMemoryModel().stopLongProcess();
                }else {
                    Model.getMemoryModel().cyclicReadBaseRamValues(RunnerView.TABLE_MAIN);
                }
            }
        };

        Action performWriteCtrlOTP = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getMemoryModel().setReqValues(face.getTableValuesForWriteCtrlOTP());
                Model.getMemoryModel().writeValues();
            }
        };
        writeCtrlOTPButton = createButton(performWriteCtrlOTP, "Write ctrl OTP", "Store values for OTP registers", true);

        loadingIcButton = createButton(performLoadingIc, "Loading...", "Cyclic read from IC all firmware regs", false);

        add(loadValuesFromTxtButton, helper.nextRow().setGridWidth(2).get());
        add(loadDefaultButton, helper.rightColumn().rightColumn().setGridWidth(1).get());
        add(loadBaseValuesFromHexButton, helper.nextRow().rightColumn().rightColumn().setGridWidth(1).get());
        add(storeValuesToTxtButton, helper.nextRow().setGridWidth(2).get());
        add(storeBaseValuesHexButton, helper.rightColumn().rightColumn().setGridWidth(1).get());
        add(readBaseRamIcButton, helper.nextRow().setGridWidth(2).get());
        add(writeIcButton, helper.rightColumn().rightColumn().setGridWidth(1).get());
        add(loadingIcButton,  helper.nextRow().setGridWidth(2).get());
        add(writeCtrlOTPButton, helper.rightColumn().rightColumn().setGridWidth(1).get());
        JPanel ledPlaceholder = new JPanel();
        ledPlaceholder.setOpaque(false);
        add(ledPlaceholder, helper.nextRow().nextRow().setGridWidth(2).get());

        if (Model.isFactoryMode()) {
            add(createDefineButton, helper.rightColumn().rightColumn().setGridWidth(1).get());
        }

        add(leftButton_CPU2_en, helper.nextRow().setGridWidth(2).get());
        add(leftButton_CONV2_en, helper.rightColumn().rightColumn().setGridWidth(1).get());
        add(leftButton_CPU1_en, helper.nextRow().setGridWidth(2).get());
        add(leftButton_CONV1_en, helper.rightColumn().rightColumn().setGridWidth(1).get());


        JPanel freePanel  = new JPanel();
        freePanel.setPreferredSize(new Dimension( WIDTH_BUTTON, HEIGHT_BUTTON *1));
        add(freePanel, helper.nextRow().setGridWidth(3).get());

        addressTextField.setPreferredSize(new Dimension(40, HEIGHT_BUTTON));
        JLabel label = new JLabel("Address:");
        add(label, helper.nextRow().setGridWidth(1).get());
        add(addressTextField, helper.rightColumn().get());
        add(scrollPaneTable, helper.rightColumn().setGridWidth(1).setGridHeight(4).get());
        add(readButton,helper.nextRow().setGridWidth(2).setGridHeight(1).get());
        add(writeButton,helper.nextRow().get());
        add(watchOTPCHB,helper.nextRow().get());
        JPanel freePanel2 = new JPanel();



        Action performCreateRomDataButton= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getParserModel().createBOTPfile();
            }
        };

        createBOTPDataButton = createButton(performCreateRomDataButton, "Create BOTP","Create rom_BOTP.hex" , false);

        Action performCreateRomU22bDataButton= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                List<String> bytes = new ArrayList<>();
                getValuesFromReg(AllRegAddr.PLL_config, bytes);
                getValuesFromReg(AllRegAddr.INIT_conf, bytes);
                FileHelper.createOutputFile(ConfProp.FILE_PATH_HEX_CODES, "rom_u22b.hex", bytes,   ".hex");
                System.out.println("Was be created rom_u22b.hex file");

            }
        };



        createRomU22bDataButton = createButton(performCreateRomU22bDataButton, "Create U22B","Create rom_u22b.hex" , false);

        if (Model.isFactoryMode()) {
            Action performCreateRomF23bDataButton = new AbstractAction() {
                public void actionPerformed(ActionEvent e) {
                    List<String> bytes = new ArrayList<>();
                    AllRegAddr f1 = FactoryGate.regF1();
                    AllRegAddr f2 = FactoryGate.regF2();
                    if (f1 != null) getValuesFromReg(f1, bytes);
                    if (f2 != null) getValuesFromReg(f2, bytes);
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_HEX_CODES, "rom_f23b.hex", bytes, ".hex");
                    System.out.println("Was be created rom_f23b.hex file");
                }
            };
            createRomF23bDataButton = createButton(performCreateRomF23bDataButton, "Create F23B", "Create rom_f23b.hex", false);
        }

        Action performProgBOTPdataButton= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!confirmProg("Block OTP registers")) return;
                Model.getMemoryModel().progBOTPtoROM();
            }
        };
        progBOTPdataButton = createButton(performProgBOTPdataButton, "Prog BOTP","Program permanently B OTP memory", true );

        Action performVerifyBOTPdataButton= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getMemoryModel().verifyBOTP();
            }
        };
        verifyBOTPdataButton = createButton(performVerifyBOTPdataButton, "Verify BOTP","Read and verify BOTP memory", true );




        Action performProgUOTPdataButton= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!confirmProg("PLL_config, INIT_conf")) return;
                File hexFile = getHexFile("rom_u22b.hex");
                if (!hexFile.exists()) {
                    JOptionPane.showMessageDialog(null,
                        "Файл rom_u22b.hex не найден!\nСначала нажмите 'Create U22B'.",
                        "Файл не найден", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                List<Integer> values = readRegValuesFromHex(hexFile, 2);
                if (values == null) {
                    JOptionPane.showMessageDialog(null,
                        "Некорректный файл rom_u22b.hex!",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                values.add(ad3sFace.getCurrentRegValue(AllRegAddr.UOTP_ctrl));
                Map<Integer, List<Integer>> reqValues = new HashMap<>();
                reqValues.put(AllRegAddr.PLL_config.getAddress(), values);
                Model.getMemoryModel().setInitReqValues(reqValues);
                Model.getMemoryModel().progUOTPtoROM();
            }
        };
        progUOTPdataButton = createButton(performProgUOTPdataButton, "Prog UOTP","Program permanently B OTP memory", true );


        if (Model.isFactoryMode()) {
        Action performProgFactoryDataButton = new AbstractAction() {
            public void actionPerformed( ActionEvent e ) {
                AllRegAddr f1 = FactoryGate.regF1();
                AllRegAddr f2 = FactoryGate.regF2();
                AllRegAddr f3 = FactoryGate.regF3();
                String fNames = (f1 != null ? f1.getReg().getDisplayName() : "") + ", " + (f2 != null ? f2.getReg().getDisplayName() : "");
                if (!confirmProg(fNames)) return;
                File hexFile = getHexFile("rom_f23b.hex");
                if (!hexFile.exists()) {
                    JOptionPane.showMessageDialog(null,
                        "Файл rom_f23b.hex не найден!\nСначала нажмите 'Create F23B'.",
                        "Файл не найден", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                List<Integer> values = readRegValuesFromHex(hexFile, 2);
                if (values == null) {
                    JOptionPane.showMessageDialog(null,
                        "Некорректный файл rom_f23b.hex!",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (f3 != null) values.add(ad3sFace.getCurrentRegValue(f3));
                Map<Integer, List<Integer>> reqValues = new HashMap<>();
                reqValues.put(FactoryGate.regF1Address(), values);
                Model.getMemoryModel().setInitReqValues(reqValues);
                Model.getMemoryModel().progFactoryToROM();
            }
        };
        progFactoryDataButton = createButton(performProgFactoryDataButton, "Prog FOTP","Program permanently B OTP memory", true );
        }


        stndbyCHB = new JCheckBox("STNDBY");
        stndbyCHB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean value = ((JCheckBox)e.getSource()).isSelected();
                Model.getMemoryModel().setStndbyValue(value);
            }
        });

        vpp9vCHB = new JCheckBox("vpp9v");
        vpp9vCHB.addActionListener(new ActionListener() {
            private boolean vppWarningShown = false;
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean value = ((JCheckBox)e.getSource()).isSelected();
                if (value && !vppWarningShown) {
                    int result = JOptionPane.showConfirmDialog(
                            (JCheckBox)e.getSource(),
                            "Подача 9V может привести к программированию данных в UOTP и BOTP областях ПЗУ.\nПродолжить?",
                            "Внимание: VPP 9V",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (result != JOptionPane.YES_OPTION) {
                        ((JCheckBox)e.getSource()).setSelected(false);
                        return;
                    }
                    vppWarningShown = true;
                }
                Model.getMemoryModel().setVpp9vValue(value);
            }
        });

        nresetCHB = new JCheckBox("NRESET");

        nresetCHB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean value = ((JCheckBox)e.getSource()).isSelected();
                Model.getMemoryModel().setNresetValue(value);
            }
        });



        nresetCHB.setSelected(true);
        add(createBOTPDataButton, helper.nextRow().setGridWidth(2).get());
        add(progBOTPdataButton, helper.rightColumn(2).setGridWidth(2).get());
        add(createRomU22bDataButton, helper.nextRow().setGridWidth(2).get());
        add(progUOTPdataButton, helper.rightColumn(2).setGridWidth(2).get());
        if (Model.isFactoryMode()) {
            add(createRomF23bDataButton, helper.nextRow().setGridWidth(2).get());
            add(progFactoryDataButton, helper.rightColumn(2).setGridWidth(2).get());
        }
        add(nresetCHB, helper.nextRow().setGridWidth(2).get());
        add(verifyBOTPdataButton, helper.rightColumn(2).setGridWidth(2).get());
        add(stndbyCHB, helper.nextRow().setGridWidth(2).get());
        add(vpp9vCHB, helper.rightColumn(2).setGridWidth(2).get());


        add(freePanel2, helper.nextRow().setWeightY(1).get());




        Model.getMemoryModel().addMemoryEventListener(RunnerView.TABLE_8CELL, this);
        Model.getMemoryModel().addLongProcessEventListener(this);




    }

    private void getValuesFromReg(AllRegAddr allRegAddr,  List<String> bytes) {
        int value  = ad3sFace.getCurrentRegValue(allRegAddr);
        int lb = 0xFF & value;
        int hb = 0xFF & (value >> 8);
        bytes.add(Integer.toHexString(lb | 0x100).substring(1));
        bytes.add(Integer.toHexString(hb | 0x100).substring(1));
    }

    private File getHexFile(String fileName) {
        File path = new File(WorkstationConfig.getProperty(ConfProp.FILE_PATH_HEX_CODES));
        if (!path.isDirectory()) path = path.getParentFile();
        return new File(path, fileName);
    }

    private List<Integer> readRegValuesFromHex(File file, int wordCount) {
        try {
            List<String> lines = Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
            if (lines.size() < wordCount * 2) return null;
            List<Integer> values = new ArrayList<>();
            for (int i = 0; i < wordCount; i++) {
                int lo = Integer.parseInt(lines.get(i * 2).trim(), 16);
                int hi = Integer.parseInt(lines.get(i * 2 + 1).trim(), 16);
                values.add(lo | (hi << 8));
            }
            return values;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean confirmProg(String registers) {
        String msg1 = "<html><b style='color:red'>&#9888; ВНИМАНИЕ: НЕОБРАТИМАЯ ОПЕРАЦИЯ</b><br><br>" +
            "Вы собираетесь навсегда записать " + registers + " в OTP память.<br>" +
            "Эта операция <b>не может быть отменена</b>. Вы уверены?</html>";
        int r1 = JOptionPane.showConfirmDialog(this, msg1,
            "Подтверждение программирования OTP", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (r1 != JOptionPane.YES_OPTION) return false;

        String msg2 = "<html><b style='color:red'>&#9888; ПОСЛЕДНЕЕ ПРЕДУПРЕЖДЕНИЕ</b><br><br>" +
            "Это последний шанс. " + registers + " будут <b>безвозвратно</b> записаны.<br>" +
            "Продолжить?</html>";
        int r2 = JOptionPane.showConfirmDialog(this, msg2,
            "Окончательное подтверждение", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r2 == JOptionPane.YES_OPTION;
    }

    public void updateAllBooleanValue(AllRegAddr allRegAddr,  int value){
        switch(allRegAddr){
            case Mode_config:
                leftButton_CPU1_en.setSelected(RegField.CPU1_en.getFieldValueFromRegValue(value) == 1);
                leftButton_CPU2_en.setSelected(RegField.CPU2_en.getFieldValueFromRegValue(value) == 1);
                leftButton_CONV1_en.setSelected(RegField.CONV1_en.getFieldValueFromRegValue(value) == 1);
                leftButton_CONV2_en.setSelected(RegField.CONV2_en.getFieldValueFromRegValue(value) == 1);
                break;
        }
    }

    private JButton createButton(Action action, String name, String toolTipText, boolean isUart  ){
        JButton button  = AppFrameHelper.createButton(action, name, toolTipText);
        if(isUart){
            listUartButtons.add(button);
        }
        return button;

    }





    public JPanel getTextFieldLabeled(JTextField textField,String label, int sizeLabel, int sizeField){
        JPanel  panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel comment= new JLabel(label, JLabel.RIGHT);
        comment.setMaximumSize(new Dimension(sizeLabel, 20));
        panel.add(comment);
        textField.setColumns(5);
        textField.setMinimumSize(new Dimension(sizeField, 20));
        panel.add(textField);
        return panel;
    }


    private List<Integer> getTableValues(){
        List<Integer> res = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel)table.getModel();
        Vector data = model.getDataVector();
        for (int i = 0; i <= 7; i++) {
            Vector<String> row = (Vector<String>)  data.get(i);
            Integer word = row.get(1) != null ? Integer.parseInt(row.get(1), 16) : 0;
            res.add(word);
        }
        return res;
    }


    public void createTable(){
        String [] columnNames = {"Address", "Value"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames,8);
        table = new JTable(tableModel);
        table.setShowGrid(true);
        table.setGridColor(new java.awt.Color(0x6A8AAC));
        table.setRowHeight(18);
        table.setCellSelectionEnabled(true);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        new ExcelAdapter(table);
    }

    @Override
    public void updateValues() {
        List <Integer> memoryValues = Model.getMemoryModel().getRespValues();
        Integer address = Model.getMemoryModel().getRespAddress();
        if (memoryValues == null || memoryValues.isEmpty()) return;

        DefaultTableModel tableModel = (DefaultTableModel) table.getModel();
        tableModel.setRowCount(memoryValues.size());

        for (int i = 0; i < memoryValues.size(); i++) {
            Integer value = memoryValues.get(i);
            tableModel.setValueAt(String.valueOf(i + address), i, 0);
            Integer v = (value | 0x10000);
            String vString = Integer.toHexString(v).toUpperCase().substring(1);
            tableModel.setValueAt(vString, i, 1);
        }

        this.revalidate();
        table.repaint();

    }



    @Override
    public void updateStatusOfProcessing() {
        boolean isProcessing = Model.getMemoryModel().isProcessing();
        boolean isCyclic = Model.getMemoryModel().isCyclic();
        for(JButton button : listUartButtons){
            button.setEnabled(!isProcessing);
        }
        for(JCheckBox checkBox : listUartcheckBox){
            checkBox.setEnabled(!isProcessing);
        }
        writeIcButton.setEnabled(!isProcessing || isCyclic);
        loadingIcButton.setText(isProcessing ? "STOP" : "Loading...");
    }

    @Override
    public void updateTableValue(RegPanel panel) {

    }

    private static boolean isValidAddress(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        try {
            Integer.parseInt(s.trim(), 10);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void saveAddress() {
        String text = addressTextField.getText();
        if (isValidAddress(text)) {
            WorkstationConfig.setProperty("MEMORY_EDITOR_ADDRESS", text.trim());
        }
    }
}
