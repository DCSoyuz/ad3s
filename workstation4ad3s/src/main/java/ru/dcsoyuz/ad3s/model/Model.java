package ru.dcsoyuz.ad3s.model;

import ru.dcsoyuz.ad3s.form.AppFrame;
import ru.dcsoyuz.ad3s.model.editor.EditorModel;
import ru.dcsoyuz.ad3s.model.editor.TreeViewModel;
import ru.dcsoyuz.ad3s.model.fpga.parser.ParserModel;
import ru.dcsoyuz.ad3s.model.uart.MemoryModel;
import ru.dcsoyuz.ad3s.model.uart.BldcSerialModel;
import ru.dcsoyuz.ad3s.model.uart.RecordModel;
import ru.dcsoyuz.ad3s.model.uart.UartModel;
import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;

import java.awt.*;

/**
 * Created by yuri.filatov on 01.08.2016.
 */
public class Model {

    private static boolean factoryMode = false;

    public static boolean isFactoryMode() { return factoryMode; }
    public static void setFactoryMode(boolean enabled) { factoryMode = enabled; }



    public static void setCurSenderIsUart(boolean curSenderIsUart) {
        Model.curSenderIsUart = curSenderIsUart;
    }

    private static boolean curSenderIsUart = false;



    private static EditorModel editorModel = new EditorModel();
    private static UartModel uartModel;

    private static TreeViewModel treeViewModel = new TreeViewModel();
    private static ParserModel parserModel = new ParserModel();
    public static MemoryModel memoryModel = new MemoryModel();

    public static boolean flagQueue = false;
    private static RecordModel recordModel;
    private static BldcSerialModel bldcSerialModel;


    private static String nameRecFolder = "record";

    public static RecordModel getRecordModel() {
        if(recordModel == null) {
            recordModel = new RecordModel();
        }
        return recordModel;
    }

    public static BldcSerialModel getBldcSerialModel() {
        if (bldcSerialModel == null) {
            bldcSerialModel = new BldcSerialModel();
        }
        return bldcSerialModel;
    }


    public static void init(){
        editorModel.addAppFrameEventListener(treeViewModel);
    }

    public static UartModel getUartModel() {
        if(uartModel == null) {
            uartModel = new UartModel();
        }
        return uartModel;
    }



    public static TreeViewModel getTreeViewModel() {
        return treeViewModel;
    }

    public static ParserModel getParserModel() {
        return parserModel;
    }

    public static EditorModel getEditorModel() {
        return editorModel;
    }

    public static MemoryModel getMemoryModel() {
        return memoryModel;
    }

    private static AppFrame mainFrame;

    public static AppFrame getMainFrame() {
        return mainFrame;
    }

    public static void setMainFrame(AppFrame frame) {
        mainFrame = frame;
    }

    public static String getNameRecFolder() {
        String configured = WorkstationConfig.getProperty(ConfProp.RECORD_FOLDER.name());
        return configured != null ? configured : nameRecFolder;
    }
}
