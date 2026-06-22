package ru.dcsoyuz.ad3s.form.terminal;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.AppFrameHelper;
import ru.dcsoyuz.ad3s.model.Model;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuri.filatov on 26.01.2024.
 */
public class PathEditor  extends JPanel {


    Map<ConfProp, JTextField> mapPathRows;



    public PathEditor() {
        mapPathRows = new HashMap<>();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(Box.createVerticalStrut(15));
        add(createPathRow("ram values *.HEX:"           , ConfProp.FILE_PATH_HEX_CODES));
        add(createPathRow("record text files *.txt:"    , ConfProp.FILE_PATH_RECORD_TXT));
        add(createPathRow("base hex file:"              , ConfProp.FILE_PATH_BASE_HEX));
        add(createPathRow("wave data folder:"            , ConfProp.FILE_PATH_WAVE_DATA));
        if (Model.isFactoryMode()) {
            add(createPathRow("values for init mem *.HEX:", ConfProp.FILE_PATH_MSIM_HEX_CODES));
            add(createPathRow("defines *.txt:", ConfProp.FILE_PATH_DEFINE_TXT_FILE));
            add(createPathRow("doc help.html:", ConfProp.FILE_PATH_HTML_FILE));
            add(createPathRow("map rdl *.rdl:", ConfProp.FILE_PATH_RDL_FILE));
            add(createPathRow("poke reg *.sv:", ConfProp.FILE_PATH_POKE_FILE));
            add(createPathRow("env enums file .sv", ConfProp.FILE_PATH_ENV_ENUMS));
            add(createPathRow("env regs names .sv", ConfProp.FILE_PATH_ENV_REGS));
            add(createPathRow("env reg fields names .sv", ConfProp.FILE_PATH_ENV_REG_FIELDS));
            add(createPathRow("env all reg addr .sv", ConfProp.FILE_PATH_ENV_ALL_REG_ADDR));
            add(createPathRow("env init array all reg .sv", ConfProp.FILE_PATH_ENV_INIT_ALL_REG_ADDR));
        }
        savePathsToFile();
        Action performSavePaths= new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                savePathsToFile();
                System.out.println("Current paths is saved to " + WorkstationConfig.getPathPropertiesFile());
            }
        };
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonRow.add(AppFrameHelper.createButton(performSavePaths, "  Save   ", "Save to config.properties"));
        buttonRow.add(AppFrameHelper.createButton(new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                Window w = SwingUtilities.getWindowAncestor(PathEditor.this);
                if (w != null) w.dispose();
            }
        }, "  Close  ", "Close window"));
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(buttonRow);
        setAlignmentX(Component.LEFT_ALIGNMENT);




    }

    private JPanel  createPathRow(String name, ConfProp prop ){

        String path = WorkstationConfig.getProperty(prop);
        // Auto-fix: wave_data was incorrectly saved as user_generated_files or empty in old config
        if (prop == ConfProp.FILE_PATH_WAVE_DATA && (path == null || path.isEmpty()
                || path.endsWith("user_generated_files"))) {
            path = null;
        }
        if(path == null){
            try {
                File baseDir = WorkstationConfig.getBaseDirectory();
                String defaultSubdir = (prop == ConfProp.FILE_PATH_WAVE_DATA) ? "wave_data" : "user_generated_files";
                File theDir = new File(baseDir, defaultSubdir);
                if (!theDir.exists()){
                    theDir.mkdirs();
                }
                path = theDir.getPath();
            } catch (Exception e) {
                path = (prop == ConfProp.FILE_PATH_WAVE_DATA) ? "wave_data" : "user_generated_files";
            }
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        JLabel label = new JLabel(name);
        label.setPreferredSize(new Dimension(150, 25));
        label.setMinimumSize(new Dimension(150, 25));
        JTextField  textField = new JTextField();
        textField.setPreferredSize(new Dimension(420, 25));
        textField.setMinimumSize(new Dimension(200, 25));
        textField.setText(path);
        panel.add(label);
        panel.add(textField);
        panel.setMaximumSize(new Dimension(600, 25));
        mapPathRows.put(prop, textField);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private void savePathsToFile(){
        for(Map.Entry<ConfProp, JTextField>entry : mapPathRows.entrySet()){
            WorkstationConfig.setProperty(entry.getKey(), entry.getValue().getText());
        }
        WorkstationConfig.storeProperties();

    }
}
