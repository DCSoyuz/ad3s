package ru.dcsoyuz.ad3s.model.fpga.parser;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.form.AppFrame;
import ru.dcsoyuz.ad3s.form.editor.ILongProcessEventListener;
import ru.dcsoyuz.ad3s.form.editor.IParserEventListener;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.editor.FileHelper;
import ru.dcsoyuz.ad3s.model.fpga.registers.RegField;

import java.io.File;
import java.util.*;

/**
 * Created by yuri.filatov on 03.08.2016.
 */
public class ParserModel {

    private Map<BlockCode, Map<String, Variable>> variables = new HashMap<>();

    private List<String> hexAd3s;

    private Boolean isParsing;
    private List<ILongProcessEventListener> listLongProcessEventListenerList = new ArrayList<>();

    private Boolean generateMifFiles = false;

    private boolean shrdRamValue = RegField.SHRD_RAM.getDefaultValue() != 0;

    private boolean shrdCpu2Value = RegField.SHRD_CPU2.getDefaultValue() != 0;

    private List<File> currentParsingFiles = new ArrayList<>();
    private Map<ModeAttrFile, List<String>>  hexData = new HashMap<>();

    private Map<Integer, IParserEventListener> mapParserEventListeners = new HashMap<>();


    public void parseCurrentFile(){
        setParsing(true);
        currentParsingFiles.clear();
        File parentFile = Model.getEditorModel().getCurrentFile().getParentFile();

        for(File file : parentFile.listFiles()){
            if( (file.getName().contains("txt") && (!file.getName().contains("mode.config") ))){
                currentParsingFiles.add(file);
            }
        }
        for(File file : parentFile.listFiles()){
            if( (file.getName().contains("asm") && (!file.getName().contains("mode.config")))){
                currentParsingFiles.add(file);
            }
        }
        new AsmParser().start();
    }


    public void updateShrdFlags(){
        shrdRamValue    = AppFrame.getInstance().getShrdRamValue();
        shrdCpu2Value   = AppFrame.getInstance().getShrdCpu2Value();
    }

    public List<String> getHexAd3s() {
        return hexAd3s;
    }

    public void setHexAd3s(List<String> hexAd3s) {
        this.hexAd3s = hexAd3s;
    }

    public Thread parseAllFiles(){
        setParsing(true);
        currentParsingFiles.clear();
        File parentFile = Model.getEditorModel().getCurrentFile().getParentFile();

        for(File file : parentFile.listFiles()){
            if( (file.getName().contains("txt") && (!file.getName().contains("mode.config") ))){
                currentParsingFiles.add(file);
            }
        }
        for(File file : parentFile.listFiles()){
            if( (file.getName().contains("asm") && (!file.getName().contains("mode.config")))){
                currentParsingFiles.add(file);
            }
        }
        Thread t = new AsmParser();
        t.start();
        return t;
    }

    public void createBOTPfile(){
        ModeAttrFile baseAttr = Model.getEditorModel().getFileAttrByName("base_ram.txt");
        ModeAttrFile cpu1Attr = Model.getEditorModel().getFileAttrByName("cpu1_data.hex");
        ModeAttrFile cpu2Attr = Model.getEditorModel().getFileAttrByName("cpu2_data.hex");
        ParserModel parserModel = Model.getParserModel();
        if(parserModel.getHexsByAttr(baseAttr) == null ||
           parserModel.getHexsByAttr(cpu1Attr) == null ||
           parserModel.getHexsByAttr(cpu2Attr) == null
        ) {
            System.out.println("Before parse all files!");
            return;
        }
        List<String> list = new ArrayList<>();
        list.addAll(hexData.get(cpu1Attr).subList(0,232));
        list.addAll(hexData.get(baseAttr).subList(0,16));
        list.addAll(hexData.get(cpu1Attr).subList(248,252));
        list.addAll(hexData.get(cpu2Attr).subList(248,252));
        list.addAll(hexData.get(cpu2Attr).subList(0,232));
        list.addAll(hexData.get(baseAttr).subList(32,48));
        list.addAll(hexData.get(baseAttr).subList(64,72));
        FileHelper.createOutputFile(ConfProp.FILE_PATH_HEX_CODES, "rom_BOTP.hex", list,   ".hex");
        FileHelper.createOutputFile(ConfProp.FILE_PATH_CURRENT_FILE, "rom_BOTP.hex", list,   ".hex");

        List<String> lowBytes = new ArrayList<>();
        List<String> highBytes = new ArrayList<>();

        for(String strVal : list){
            int intVal = Integer.parseInt(strVal,16);
            int lowByte = intVal & 0xFF;
            int highByte = (intVal >> 8) & 0xFF;
            lowBytes.add(Integer.toHexString(lowByte | 0x100).substring(1));
            highBytes.add(Integer.toHexString(highByte | 0x100).substring(1));
        }

        FileHelper.createOutputFile(ConfProp.FILE_PATH_HEX_CODES, "rom_BOTP_lb.hex", lowBytes,   ".hex");
        FileHelper.createOutputFile(ConfProp.FILE_PATH_HEX_CODES, "rom_BOTP_hb.hex", highBytes,   ".hex");
        System.out.println("Was be created rom_BOTP.hex file");
    }




    public Thread parseSelectedFiles(List<File>selectedFiles){
        setParsing(true);
        currentParsingFiles.clear();
        for(File file : selectedFiles){
            if( (!file.getName().contains("mode.config") )){
                currentParsingFiles.add(file);
            }
        }
        AsmParser thread = new AsmParser();
        thread.start();
        return thread;
    }

    public List<File> getCurrentParsingFiles() {
        return currentParsingFiles;
    }


    public Variable getVariable(BlockCode blockCode, String name){
        Map<String, Variable> map = variables.get(blockCode);
        if (map != null) {
            Variable variable = map.get(name);
            return variable;
        }else
        {
            System.out.println("Variables for block:" + blockCode + " not initialized!");
            return null;
        }
    }

    public Map<String, Variable>    getBlockVariables(BlockCode blockCode){
        return variables.get(blockCode);
    }

    public void setVariables(BlockCode blockCode, Map<String , Variable> newVariables){
        variables.put(blockCode, newVariables);
    }


    public Map<ModeAttrFile, List<String>> getHexData() {
        return hexData;
    }

    public void removeHexsByAttr(ModeAttrFile attr){
        hexData.remove(attr);
    }

    public List<String> getHexsByAttr(ModeAttrFile attr){
        return      hexData.get(attr);
    }
    public void addHexsByAttr(ModeAttrFile attr, List<String> hexs){
         hexData.put(attr, hexs);
    }


    public Boolean getParsing() {
        return isParsing;
    }

    public void setParsing(Boolean parsing) {
        isParsing = parsing;
        for(ILongProcessEventListener listener : listLongProcessEventListenerList){
            //listener.updateStatusOfParsing();
        }
    }


    public void addLongProcessEventListener(ILongProcessEventListener listener){
        listLongProcessEventListenerList.add(listener);
    }

    public Boolean getGenerateMifFiles() {
        return generateMifFiles;
    }

    public void setGenerateMifFiles(Boolean generateMifFiles) {
        this.generateMifFiles = generateMifFiles;
    }

    public boolean isShrdRamValue() {
        return shrdRamValue;
    }

    public boolean isShrdCpu2Value() {
        return shrdCpu2Value;
    }

    public void addParserEventListener(IParserEventListener listener, int numCpu){
        mapParserEventListeners.put(numCpu, listener);
    }
    public void updateCpuDebuggers(int numCpu, String assemblyCodes, String hexCodes){
        mapParserEventListeners.get(numCpu).updateDebuggerCpuView(assemblyCodes, hexCodes);

    }

}
