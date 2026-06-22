package ru.dcsoyuz.ad3s.form.editor;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.model.Model;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

/**
 * Created by yuri.filatov on 03.08.2016.
 */
public class ConnectPanel extends JPanel implements ILongProcessEventListener {

    private JButton buttonProgram;
    private JButton buttonProgFlash;

    private JButton buttonParseAll;
    private JButton buttonCleanErrors;
    private JButton buttonReOpenRecCom;

    private JButton buttonVerify;
    private JButton   buttonReOpenFTDI;
    private JComboBox serialportComboBox;
    private String[] portList;

    private JButton buttonReOpenRECPort;
    private JButton recordingBTN;
    private JTextField fileNameRecordingTB;
    private Boolean isRecording = false;
    private String recPatch = "data";
    private String[] portListRec;
    private JComboBox serialportRecComboBox;




    public ConnectPanel(){
        super();

        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        KeyStroke keyParse = KeyStroke.getKeyStroke(KeyEvent.VK_R, KeyEvent.CTRL_MASK);



        Action parseAll= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if(!Model.getEditorModel().getCurrentFile().getName().contains("base_ram.txt")) {
                    Model.getEditorModel().saveCurrentFile();
                }
                Model.getParserModel().parseAllFiles();
                Model.getEditorModel().updateColors();
            }
        };
        KeyStroke keyProgram = KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK);
        buttonParseAll = new JButton(parseAll);
        buttonParseAll.setText("ParseAll");
        buttonParseAll.setToolTipText("Parse all txt and assembly to generate assembly");
        buttonParseAll.setActionCommand("parseAll");

        buttonParseAll.setFocusable(false);
        buttonParseAll.getActionMap().put("parseAll", parseAll);
        buttonParseAll.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyProgram, "parseAll");

        serialportComboBox = new JComboBox(new String[] {Model.getUartModel().getPortName()});
        serialportComboBox.setFont(Font.getFont("Times New Roman"));
        serialportComboBox.setMaximumSize(new Dimension(110,100));


        serialportComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshComList();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if(serialportComboBox.getSelectedItem() != null) {
                    WorkstationConfig.setProperty(ConfProp.COM_PORT, serialportComboBox.getSelectedItem().toString());
                }
            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        serialportComboBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox box = (JComboBox)e.getSource();
                String item = (String)box.getSelectedItem();
                if(item != null) {
                    Model.getUartModel().setPortName(item);
                    //WorkstationConfig.setProperty(ConfProp.COM_PORT, item);
                }
                if(serialportComboBox.isPopupVisible()){
                    serialportComboBox.transferFocus();
                }
            }

        });

        serialportComboBox.setPreferredSize(new Dimension(80,20));
        Action performReOpenFTDI = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getUartModel().reOpenComPort();

            }
        };

        buttonReOpenFTDI = new JButton(performReOpenFTDI);
        buttonReOpenFTDI.setText("Open COM");
        buttonReOpenFTDI.setMargin(new Insets(0,0,0,0));
        buttonReOpenFTDI.setToolTipText("ReOpen FTDI");
        buttonReOpenFTDI.setFocusable(false);
        buttonReOpenFTDI.getActionMap().put("performReOpenFTDI", performReOpenFTDI);






        Action performProgram = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getMemoryModel().writeSelectedFilesToFlash();
            }
        };


        buttonProgram = new JButton(performProgram);
        buttonProgram.setText("Program");

        buttonProgram.setToolTipText("Program selected files to hardware");
        buttonProgram.setActionCommand("program");

        buttonProgram.setFocusable(false);
        buttonProgram.getActionMap().put("performProgram", performProgram);


        Action performProgFlash = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getMemoryModel().writeSelectedFilesToEspFlash();
            }
        };

        buttonProgFlash = new JButton(performProgFlash);
        buttonProgFlash.setText("Prog Flash");
        buttonProgFlash.setToolTipText("Program selected files to ESP32 flash for startup init");
        buttonProgFlash.setActionCommand("progFlash");
        buttonProgFlash.setFocusable(false);
        buttonProgFlash.getActionMap().put("performProgFlash", performProgFlash);


        Action performVerify = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getParserModel().parseAllFiles();
                Model.getEditorModel().updateColors();
                Model.getMemoryModel().verifySelectedFilesToFlash();
            }
        };


        buttonVerify = new JButton(performVerify);
        buttonVerify.setText("Verify");

        buttonVerify.setToolTipText("Verify selected files to hardware");
        buttonVerify.setActionCommand("verify");

        buttonVerify.setFocusable(false);
        buttonVerify.getActionMap().put("performVerify", performVerify);




        Action performCleanErrors = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getEditorModel().cleanTextErrors();
            }
        };



        buttonCleanErrors = new JButton(performCleanErrors);
        buttonCleanErrors.setText("Clean log");

        buttonCleanErrors.setToolTipText("Clean output in errors area");
        buttonCleanErrors.setFocusable(false);
        buttonCleanErrors.getActionMap().put("performCleanErrors", performCleanErrors);




        Action performReOpenRecFTDI = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getUartModel().reOpenComPort();

            }
        };

        buttonReOpenRecCom = new JButton(performReOpenRecFTDI);
        buttonReOpenRecCom.setText("ReOpen FTDI");

        buttonReOpenRecCom.setToolTipText("ReOpen FTDI");
        buttonReOpenRecCom.setFocusable(false);
        buttonReOpenRecCom.getActionMap().put("performReOpenFTDI", performReOpenRecFTDI);



        Action recordingAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                //Model.getUartModel().reOpenFTDI();
                isRecording = !isRecording;
                if(isRecording){
                    recordingBTN.setText("STOP");
                    recordingBTN.setBackground(Color.RED);

                    fileNameRecordingTB.setEnabled(false);
                    serialportRecComboBox.setEnabled(false);
                    buttonReOpenRECPort.setEnabled(false);

                    Model.getRecordModel().startRecording(fileNameRecordingTB.getText());


                }else {
                    recordingBTN.setText("REC..");
                    recordingBTN.setBackground(Color.GREEN);

                    fileNameRecordingTB.setEnabled(true);
                    serialportRecComboBox.setEnabled(true);
                    buttonReOpenRECPort.setEnabled(true);

                    Model.getRecordModel().stopRecording();
                }
            }
        };
        recordingBTN = new JButton(recordingAction);
        recordingBTN.setText("REC..");
        recordingBTN.setBackground(Color.GREEN);
        recordingBTN.setToolTipText("Enable recording");


        fileNameRecordingTB = new JTextField(recPatch);
        fileNameRecordingTB.setToolTipText("Name REC file");


        // BEGIN------------ ComboBox for selected FTDI COM Port -------------------- //
        serialportRecComboBox = new JComboBox(new String[] {Model.getRecordModel().getPortName()});
        serialportRecComboBox.setFont(Font.getFont("Times New Roman"));
        serialportRecComboBox.setMaximumSize(new Dimension(110,100));
        serialportRecComboBox.setToolTipText("Поле выбора COM порта (RECORDER). Обновление списка происходит автоматически.");

        serialportRecComboBox.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refrashComList();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                WorkstationConfig.setProperty("SELECED_RECORD_COM_PORT", serialportRecComboBox.getSelectedItem().toString());
            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
        serialportRecComboBox.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox box = (JComboBox)e.getSource();
                String item = (String)box.getSelectedItem();
                Model.getRecordModel().setPortName(item);
            }

        });
        // END---------------- ComboBox for selected REC COM Port -------------------- //

        // BEGIN ---------------- Button reconnect  REC connection ------------------- //

        Action performReOpenREC = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Model.getRecordModel().reOpenFTDI();
            }
        };

        buttonReOpenRECPort = new JButton(performReOpenREC);
        buttonReOpenRECPort.setText("ReOpen Port");

        buttonReOpenRECPort.setToolTipText("ReOpen Port");
        buttonReOpenRECPort.setFocusable(false);
        buttonReOpenRECPort.getActionMap().put("performReOpenREC", performReOpenREC);

        // END ------------------ Button reconnect  REC connection ------------------- //




        // COM port panel
        JPanel comPanel = new JPanel();
        comPanel.setLayout(new BoxLayout(comPanel, BoxLayout.X_AXIS));
        comPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        comPanel.setPreferredSize(new Dimension(100, 20));
        comPanel.setMaximumSize(new Dimension(100, 20));
        JLabel comLabel = new JLabel("COM:");
        comLabel.setPreferredSize(new Dimension(38, 20));
        comLabel.setMaximumSize(new Dimension(38, 20));
        serialportComboBox.setPreferredSize(new Dimension(60, 20));
        serialportComboBox.setMaximumSize(new Dimension(60, 20));
        comPanel.add(comLabel);
        comPanel.add(serialportComboBox);

        add(Box.createVerticalGlue());
        add(buttonParseAll);
        add(buttonProgram);
        add(buttonVerify);
        add(Box.createRigidArea(new Dimension(0, 30)));
        add(comPanel);
        add(buttonReOpenFTDI);
        add(buttonCleanErrors);

        int btnWidth = 100;
        Dimension btnSize = new Dimension(btnWidth, 20);
        for (Component c : getComponents()) {
            if (c instanceof JComponent) {
                ((JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
                if (c instanceof JButton) {
                    ((JButton) c).setMaximumSize(btnSize);
                    ((JButton) c).setPreferredSize(btnSize);
                }
            }
        }
        serialportComboBox.setMaximumSize(btnSize);
        serialportComboBox.setPreferredSize(btnSize);

        // BLDC motor controls
        Model.getParserModel().addLongProcessEventListener(this);
        Model.getMemoryModel().addLongProcessEventListener(this);
    }



    void refreshComList(){
        portList =  Model.getUartModel().getPortList();
        String portName = Model.getUartModel().getPortName();
        serialportComboBox.removeAllItems();
        for (String s : portList) {
            serialportComboBox.insertItemAt(s, serialportComboBox.getItemCount());
        }
        for (int i = 0; i < portList.length ; i++){
            if(portName == null) portName = portList[i];
            else if (portName.equals(portList[i])) {
                serialportComboBox.setSelectedIndex(i);

            }
        }
    }




    void refrashComList(){
        portListRec =  Model.getRecordModel().getPortList();
        String portName = Model.getRecordModel().getPortName();
        serialportRecComboBox.removeAllItems();
        for (String s : portListRec) {
            serialportRecComboBox.insertItemAt(s, serialportRecComboBox.getItemCount());
        }
        for (int i = 0; i < portListRec.length ; i++){
            if (portName.equals(portListRec[i])) {
                serialportRecComboBox.setSelectedIndex(i);
            }
        }

    }




    @Override
    public void updateStatusOfProcessing() {
        if(Model.getMemoryModel().isProcessing()){
            buttonProgram.setEnabled(false);
            buttonProgFlash.setEnabled(false);
            buttonReOpenFTDI.setEnabled(false);
            buttonVerify.setEnabled(false);
        } else{
            buttonProgram.setEnabled(true);
            buttonProgFlash.setEnabled(true);
            buttonReOpenFTDI.setEnabled(true);
            buttonVerify.setEnabled(true);
        }

    }

}
