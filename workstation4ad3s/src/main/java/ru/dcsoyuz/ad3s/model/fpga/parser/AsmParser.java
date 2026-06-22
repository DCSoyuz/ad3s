package ru.dcsoyuz.ad3s.model.fpga.parser;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.editor.FileHelper;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import static ru.dcsoyuz.ad3s.model.fpga.parser.CpuCommand.CLR_CH;

/**
 * Created by yuri.filatov on 21.04.2016.
 */
public class AsmParser extends Thread {


    private Map<String,Integer> arrayMetok = new HashMap<>();
    private File currentFile;
    private ModeAttrFile currentAttr;
    private Map<Integer, Variable> memoryValues;

    private Map<BlockCode, Map<Integer, Variable>> icValues = new HashMap<>();
    private List<String> lines = new ArrayList<>();
    private List<String> linesBinaryCode = new ArrayList<>();
    private List<String> hexCodes = new ArrayList<>();


    @Override
    public void run() {
        try {






            Model.getParserModel().getHexData().clear();
            List<File> selectedFiles = Model.getParserModel().getCurrentParsingFiles();
            for(File file : selectedFiles){

                String name =  file.getName();
                if(name.contains("_com.asm")){
                    int index = file.getName().indexOf("_com");
                    String partname = name.substring(0, index);


                    File fileTxt = Model.getEditorModel().getFileByName(partname+"_ram");
                    ModeAttrFile currentTxt = Model.getEditorModel().getFileAttr(fileTxt);
                    if(!Model.getParserModel().getHexData().containsKey(currentTxt)){
                        currentFile = fileTxt;
                        parse();
                    }


                }
                currentFile = file;
                parse();
                if(currentFile.getName().contains("_com.asm")) {
                    createOutputAd3sHex();
                }


            }

            // Write base_ram.hex to disk
            ModeAttrFile baseRamAttr = Model.getEditorModel().getFileAttrByName("base_ram.txt");
            if (baseRamAttr != null) {
                List<String> baseRamData = Model.getParserModel().getHexData().get(baseRamAttr);
                if (baseRamData != null && !baseRamData.isEmpty()) {
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_CURRENT_FILE, "base_ram.hex", baseRamData, ".hex");
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_HEX_CODES, "base_ram.hex", baseRamData, ".hex");
                }
            }

            // Refresh tree on EDT to show newly generated hex files
            SwingUtilities.invokeLater(() -> Model.getTreeViewModel().refreshTree());

            Model.getEditorModel().updateOutputCodes();
            Model.getParserModel().setParsing(false);
           // Model.getParserModel().setGenerateMifFiles(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void parse(){
        currentAttr = Model.getEditorModel().getFileAttr(currentFile);
        if(currentAttr == null){
            System.out.println("File "+ currentFile.getName() + "not registered in mode.config. This file will be ignored!");
            return;
        }
        InputStream in;
        BufferedReader br;
        lines = new ArrayList<>();
        try {
            in = new FileInputStream(currentFile);
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"), 8192);
            String line;

            while((line = br.readLine())!= null){
                lines.add(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(currentAttr.getFileType().equals(FileType.ASM)) {
            parseAssembly();
        }else if(currentAttr.getFileType().equals(FileType.HEX)){
            List<String> hexCodesToParserModel = new ArrayList<>();
            for(String line : lines){
                hexCodesToParserModel.add(line);
            }
            Model.getParserModel().getHexData().put(currentAttr, hexCodesToParserModel);
            return;
        }else {
            parseTxt();
        }
        String textAssemblyCode     = createTextFromLines(currentAttr.getFileType() == FileType.ASM ? lines : hexCodes);
        String textHexCode          = createTextFromLines(hexCodes);
        String textBinaryCode       = createTextFromLines(linesBinaryCode);

        List<String> hexCodesToParserModel = new ArrayList<>();
        hexCodesToParserModel.addAll(hexCodes);
        Model.getEditorModel().setOutputCodes(currentFile, textAssemblyCode, textHexCode, textBinaryCode);
        if(currentAttr.getFileType().equals(FileType.ASM) ) {
            Model.getParserModel().updateCpuDebuggers(currentAttr.getBlockCode().getIndex(), textAssemblyCode, textHexCode);
        }
        Model.getParserModel().getHexData().put(currentAttr, hexCodesToParserModel);
        //createOutputFiles();


    }


    private void parseTxt(){
        Map<String, Variable> res = new HashMap<>();
        List<Variable> romValuesList = new ArrayList<>();
        Map<Integer, Variable> memValues = new HashMap<>();
        icValues.put(currentAttr.getBlockCode(), memValues);
        memoryValues = memValues;
        String line = null;
        try {
            for(int i = 0; i < lines.size(); i++) {
                line = lines.get(i);
                Integer index;
                String value = "", comment = null;
                String[] params = line.split("\t");
                if (params.length >= 1) {
                    if(!params[0].isEmpty()){
                        index = i;
                        if (params.length >= 2 && params[1] != null && !params[1].isEmpty()) {
                            value = params[1];
                        }
                        if (params.length >= 3 && params[2] != null && !params[2].isEmpty()) {
                            comment = params[2];
                        }
                        Variable resVar = new Variable(value,index, comment);
                        memValues.put(index, resVar);
                        for(int k=3; k <= params.length-1; k++){
                            if (params[k] != null && !params[k].isEmpty()) {
                                String name = params[k].replaceAll("\\s+", "");
                                res.put(name, resVar);
                                if(k==5 && params[k].equals("rom")){
                                    romValuesList.add(resVar);
                                }
                            }
                        }
                    } else {
                        System.out.println("Empty index in line [" + line + "]. This line is ignored");
                    }
                } else {
                    System.out.println("Line [" + line + "] don't consist index column. Parsing this line not allowed.");
                }

            }
        }catch (NumberFormatException e){
                System.out.println("Unrecognized index in line [" + line + "]. This line is ignored");
                e.printStackTrace();
        }

        Model.getParserModel().setVariables(currentAttr.getBlockCode(), res);
        createOutputCodesFromVariables();
    }


    private void createOutputAd3sHex(){
        
        List<String > hex_data_list_ad3s = new ArrayList<>(hexCodes);
        Map<Integer, Variable> mapValues = icValues.get(currentAttr.getBlockCode());
        for(Variable var : mapValues.values() ) {
            if(!var.getValue().equals("")) {
                hex_data_list_ad3s.set(var.getIndex(), var.getValue());
            }
        }
        FileHelper.createOutputFile(ConfProp.FILE_PATH_CURRENT_FILE, "cpu" + currentAttr.getBlockCode().getIndex().toString() + "_data.hex", hex_data_list_ad3s, ".hex");
        FileHelper.createOutputFile(ConfProp.FILE_PATH_HEX_CODES, "cpu" + currentAttr.getBlockCode().getIndex().toString() + "_data.hex", hex_data_list_ad3s, ".hex");
        List<String> hexs0 = new ArrayList<>();
        List<String> hexs1 = new ArrayList<>();
        boolean is_even = true;
        for (String hex: hex_data_list_ad3s) {
            if(is_even){
                hexs0.add(hex);
            }else{
                hexs1.add(hex);
            }
            is_even = !is_even;
        }

        if (Model.isFactoryMode()) {
        int address = 0;
        address = createOutputFile( address,64, "_0", hexs0 );
        address = createOutputFile( address,32, "_0", hexs0 );
        address = createOutputFile( address,32, "_0", hexs0 );
        address = createOutputFile( address,64, "_0", hexs0 );
        address = createOutputFile( address,32, "_0", hexs0 );
        address = createOutputFile( address,16, "_0", hexs0 );
        address = createOutputFile( address,8,  "_0", hexs0 );
        address = createOutputFile( address,4,  "_0", hexs0 );

        if(!Model.getParserModel().isShrdRamValue()){

            FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex",new ArrayList<String>(hexs0.subList(128, 192)), "_"+ "buf_" + (currentAttr.getBlockCode().getIndex() == 1 ? "1_0" : "2_0" ) + ".hex");
        } else {
            if (Model.getParserModel().isShrdCpu2Value()) {
                if (currentAttr.getBlockCode().getIndex() == 2) {
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs0.subList(128, 192)), "_" + "buf_" + "2_0" + ".hex");
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs0.subList(192, 256)), "_" + "buf_" + "1_0" + ".hex");
                }
            } else {
                if (currentAttr.getBlockCode().getIndex() == 1) {
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs0.subList(128, 192)), "_" + "buf_" + "1_0" + ".hex");
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs0.subList(192, 256)), "_" + "buf_" + "2_0" + ".hex");
                }
            }
        }
        address = 0;
        address = createOutputFile( address,64, "_1", hexs1 );
        address = createOutputFile( address,32, "_1", hexs1 );
        address = createOutputFile( address,32, "_1", hexs1 );
        address = createOutputFile( address,64, "_1", hexs1 );
        address = createOutputFile( address,32, "_1", hexs1 );
        address = createOutputFile( address,16, "_1", hexs1 );
        address = createOutputFile( address,8,  "_1", hexs1 );
        address = createOutputFile( address,4,  "_1", hexs1 );

        if(!Model.getParserModel().isShrdRamValue()){

            FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex",new ArrayList<String>(hexs1.subList(128, 192)), "_"+ "buf_" + (currentAttr.getBlockCode().getIndex() == 1 ? "1_1" : "2_1" ) + ".hex");
        } else {
            if (Model.getParserModel().isShrdCpu2Value()) {
                if (currentAttr.getBlockCode().getIndex() == 2) {
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs1.subList(128, 192)), "_" + "buf_" + "2_1" + ".hex");
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs1.subList(192, 256)), "_" + "buf_" + "1_1" + ".hex");
                }
            } else {
                if (currentAttr.getBlockCode().getIndex() == 1) {
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs1.subList(128, 192)), "_" + "buf_" + "1_1" + ".hex");
                    FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs1.subList(192, 256)), "_" + "buf_" + "2_1" + ".hex");
                }
            }
        }
        } // end factory mode


        Model.getParserModel().setHexAd3s(hex_data_list_ad3s);
        Model.getParserModel().addHexsByAttr(Model.getEditorModel().getFileAttrByName("cpu" + currentAttr.getBlockCode().getIndex().toString()+"_data.hex"), hex_data_list_ad3s);
    }



    private int createOutputFile (int address, int len, String postfix,  List<String> hexs) {

        FileHelper.createOutputFile(ConfProp.FILE_PATH_MSIM_HEX_CODES, "ram_data.hex", new ArrayList<String>(hexs.subList((address>>1), ((address+len) >> 1))),   "_"+ currentAttr.getBlockCode().getIndex().toString() + String.format("%03d",address )+ String.format("%03d",address+len-1 ) + postfix + ".hex");
        return address + len;
    }

    public void createAssemblyHexCodes(){
        hexCodes.clear();
        int i = 1;
        for (String s : linesBinaryCode){
            Integer command;
            try {
                command = s.equals("") ? 0 : Integer.parseInt(s, 2);
            }catch (NumberFormatException e){
                e.printStackTrace();
                continue;
            }
            String hex;

            hex = Integer.toHexString(command | 0x10000).substring(1);

            Integer hexSize = currentAttr.getNumBits() > 16 ? Math.round(currentAttr.getHexSize()/2) : currentAttr.getHexSize();
            if(i <=  hexSize ) {
                hexCodes.add(hex);
            }else{
            }
            i++;
        }

    }

    public String createTextFromLines(List<String> list){
        String text = "";
        for (String hex : list){
            text += hex + "\n";
        }
        return text;
    }



    private void parseAssembly(){
        arrayMetok.clear();
        deleteAsmComments();
        preparePredefinedFunction();
        getArrayMetok();
        parseCommands();
        Double d =  Math.pow(2, currentAttr.getNumBits());
        int mask = d.intValue();
        if(linesBinaryCode.size() < currentAttr.getSize()){
            int count = currentAttr.getSize() - linesBinaryCode.size()-1;
            for(int i = 0 ; i <= count; i++){
                linesBinaryCode.add(Integer.toBinaryString(0));
            }
        }
        createAssemblyHexCodes();
    }




    private void createOutputCodesFromVariables(){
        hexCodes.clear();
        linesBinaryCode.clear();
        Double d =  Math.pow(2, currentAttr.getNumBits());
        BigInteger mask = BigDecimal.valueOf(d).toBigInteger();
        int remainder =  (int)(Math.pow(2, currentAttr.getNumBits() % 4)-1);

        String zero16 = mask.toString(16).substring(1);
        String zero2 = mask.toString(2).substring(1);
        for(int i=0; i < currentAttr.getHexSize(); i++) {
            Variable variable = memoryValues.get(i);
            if (variable != null ) {
                BigInteger number;
                try{
                    String value =variable.getValue() ;
                    if(value.equals("")){
                        value = "0";
                    }
                    number = new BigInteger(value, 16).or(mask);
                }catch (NumberFormatException e){
                    hexCodes.add(zero16);
                    linesBinaryCode.add(zero2);
                    System.out.println("Variable with index:" + String.valueOf(i)+ " has incorrect value!(File: "+ currentFile.getName()+")");
                    continue;
                }
                String hexString = number.toString(16);
                if(remainder == 0) {
                    hexString = hexString.substring(1);
                }else {
                    String high = hexString.substring(0, 1);
                    String newHigh = String.valueOf(Integer.parseInt(high, 16) & remainder);
                    hexString = newHigh + hexString.substring(1,hexString.length());
                }
                hexCodes.add(hexString);
                linesBinaryCode.add(number.toString(2).substring(1));
            }
            else {
                hexCodes.add(zero16);
                    linesBinaryCode.add(zero2);
            }
        }
    }


    private void fixArrayMetok(int i){
            for(Map.Entry<String, Integer> metka : arrayMetok.entrySet()){
            if(metka.getValue() > i ){
                arrayMetok.put(metka.getKey(), metka.getValue() - 1);
            }
        }



    }


    private void parseCommands(){
        linesBinaryCode.clear();
        Pattern patternAnyWord = Pattern.compile("[-\\w|$|#|%]+");
        Pattern patternLabel = Pattern.compile("^[\\w]+:\\s*");
        for(int i = 0 ; i < lines.size(); i++){
            String line = lines.get(i);

            // Strip label prefix if present
            String effectiveLine = line;
            Matcher labelMatcher = patternLabel.matcher(line);
            if (labelMatcher.find()) {
                effectiveLine = line.substring(labelMatcher.end());
            }
            if (effectiveLine.trim().isEmpty()) {
                // Label-only line — keep in lines, no binary code
                continue;
            }

            Matcher matcher = patternAnyWord.matcher(effectiveLine);
            CpuCommand cpuCommand = null;
            String commandStr = null;
            if(matcher.find()) {
                commandStr = matcher.group();

                try {

                    cpuCommand = CpuCommand.getCommand(commandStr);


                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }
            if(cpuCommand !=null){
                String binaryCode = parseArguments(effectiveLine, matcher, cpuCommand, i);
                if(binaryCode!=null) {
                    linesBinaryCode.add(binaryCode);
                }else{

                    System.out.println("Line assembly["+line+"] is rejected!");
                    fixArrayMetok(i);
                    lines.remove(i);
                    i--;
                }
            }else{
                if(commandStr.contains("#") && commandStr.contains("%")) {
                    int index = commandStr.indexOf("%");
                    index ++;
                    String numStr = commandStr.substring(index, commandStr.length());
                    if(numStr.length() > 0 ) {
                        int numBinaryLine = Integer.parseInt(numStr);
                        int sizeExistCode = linesBinaryCode.size();
                        for(int k = 0; k < numBinaryLine - sizeExistCode; k++ ) {
                            linesBinaryCode.add("0000000000");
                            lines.add(i, "");
                        }
                        i = numBinaryLine - sizeExistCode + i ;
                        lines.remove(i);
                        i--;
                        continue;
                    }else {
                        lines.remove(i);
                        i--;
                        continue;
                    }
                }
                if(commandStr != null) {
                    System.out.println("Invalid command[" + commandStr + "] in line:[" + line + "]");
                }
                System.out.println("Line assembly["+line+"] is rejected!");
                fixArrayMetok(i);
                lines.remove(i);
                i--;
            }



        }
    }





    //************************************************************************************************
    //************************************************************************************************
    //************************************************************************************************

    private String parseArguments(String line, Matcher matcher, final CpuCommand command, Integer i){
        StringBuilder binaryCommand = new StringBuilder();
        binaryCommand.append(command.getOpCode());
        switch (command) {
            case NOP:
            case SET_A_HAND:
                if(matcher.find()) {
                    String count = argToBinary(matcher.group(), 0b1000,  line);
                    if(count != null){
                        binaryCommand.append(count );
                    }else{
                        return null;
                    }
                }else {
                    binaryCommand.append("000");
                }
                break;
            case SETB_STP:
            case CLRB_STP:
            case SW_HTE1:
            case SW_HTE2:
            case WAIT_EXT_CPU:
            case WAIT_OWN_HAND:
            case TXT:
            case WAIT_EXT_HAND:
            case LOADSTORE_ON:
            case LOADSTORE_OFF:
            case IDLE:
                if(matcher.find()) {
                    System.out.println("For line[" + line + "]Any argument is ignored!");
                }
                break;
            case CFIX1:
            case CFIX2:
            case CFIX3:
            case CFIX4:
            case HFIX:
            case CFIX7:
            case SIN:
            case COS:
            case ACC:
            case INC:
            case CFIX0:
            case DEC2FLOAT:
            case DEC:

            case CLOAD_CUBE:
            case ABS:
            case STOREPC:
            case RJUMP:
                if(matcher.find()) {
                    String binArg1 =  getBinaryRON( matcher.group(),line);
                    if(binArg1!=null) {
                        binaryCommand.append(binArg1);
                    }else {
                        return null;
                    }
                } else {
                    return null;
                }
                break;
            case CLR:
                if(matcher.find()) {
                    String arg = matcher.group();
                    if(arg != null) {
                        if (arg.contains("R")) {
                            String binArg1 = getBinaryRON(matcher.group(), line);
                            if (binArg1 != null) {
                                binaryCommand.append(binArg1);
                            } else {
                                return null;
                            }
                        } else {
                            String binArg1 = getBinaryCH(matcher.group(), line);
                            binaryCommand.delete(0,binaryCommand.length());
                            binaryCommand.append(CLR_CH.getOpCode());
                            binaryCommand.append(binArg1);
                        }
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
                break;
            case CLR_CH:
                if(matcher.find()) {
                    String binArg1 =  getBinaryCH( matcher.group(),line);
                    if(binArg1!=null) {
                        binaryCommand.append(binArg1);
                    }else {
                        return null;
                    }
                } else {
                    return null;
                }
                break;
            case ADD:
            case SUB:
            case ORL:
            case ANL:

            case ATAN:
            case MOVE:
            case MULTCUBE_ACC:
            case MULTI:
            case MULTF:
            case ACC2:
            case MULTK14:
            case MULTK24:
            case MULTF_ACC:
            case MULTI_ACC:
                if(matcher.find()) {
                    String binArg1 =  getBinaryRON( matcher.group(),line);
                    if(binArg1!=null) {
                        binaryCommand.append(binArg1);
                    }else {
                        return null;
                    }
                }else {
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 =  getBinaryRON( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                break;

            case STORE:
            case CSTORE:
            case LOAD:

            case CLOAD:
            case STORE32:
            case CSTORE32:
            case LOAD32:
            case CLOAD32:
                String binArg3 = null;
                if(matcher.find()) {
                    String arg = matcher.group();
                    Variable perem;
                    perem = Model.getParserModel().getVariable(currentAttr.getBlockCode(), arg);
                    if (perem != null) {
                        arg = perem.getIndex().toString();
                    }
                    Integer address;
                    try {
                        address = Integer.parseInt(arg);
                    } catch (NumberFormatException e){
                        System.out.println("Name variable not defined for mode: "+ Model.getEditorModel().getFileAttr(currentFile).getBlockCode().toString());
                        return null;
                    }
                    if(matcher.find()) {
                        binArg3 = getBinaryRON(matcher.group(), line);
                        if (binArg3 == null) {
                            return null;
                        }
                    } else {
                        return null;
                    }

                    switch(command){
                       case CLOAD32:
                       case LOAD32:
                           if(address < 64 || address > 383){
                               System.out.println(String.format("Incorrect address value=%d, correct range [64...384]", address));
                               return null;
                           }
                           address = (address-64)/2 ;
                           break;
                        case STORE32:
                        case CSTORE32:
                            if(address < 96 || address > 383){
                                System.out.println(String.format("Incorrect address value=%d, correct range [96...384]", address));
                                return null;
                            }
                            address = (address-64)/2 ;
                            break;
                        case LOAD:
                        case CLOAD:
                            if(binArg3.equals("110") || binArg3.equals("111")) {
                                if(address < 64 || address > 383){
                                    System.out.println(String.format("Incorrect address value=%d, correct range [64...383]", address));
                                    return null;
                                }
                                address =  (address-64)/2 ;
                            }else if(address < 128 || address > 256){
                                System.out.println(String.format("Incorrect address value=%d, correct range [128...256]", address));
                                return null;
                            }
                            break;
                        case CSTORE:
                        case STORE:
                            if(binArg3.equals("110") || binArg3.equals("111")) {
                                if(address < 96 || address > 383){
                                    System.out.println(String.format("Incorrect address value=%d, correct range [96...383]", address));
                                    return null;
                                }
                                address =  (address-64)/2 ;
                            } else {
                                if(address < 96 || address > 256){
                                    System.out.println(String.format("Incorrect address value=%d, correct range [96...256]", address));
                                    return null;
                                }
                            }
                            break;
                    }



                    arg = address.toString();
                    String binaryAddressRAM ;
                    binaryAddressRAM = argToBinary(arg, 0b10000000, line);

                    if (binaryAddressRAM != null) {
                        binaryCommand.append(binaryAddressRAM);
                    } else {
                        return null;
                    }
                } else {
                    System.out.println("Expected Address for command in line: " + i.toString());
                }
                binaryCommand.append(binArg3);
                break;
            case CONST:
                if(matcher.find()){
                    String arg = matcher.group();
                    String arg2;
                    int argInt = Integer.parseInt(arg);
                    if(argInt < 0){
                        if(argInt < -64 ) {
                            System.out.println("CONST value less -64!");
                            return null;
                        }
                        arg2 = String.valueOf(128 + argInt);
                    } else {
                        if(argInt > 63 ){
                            System.out.println("CONST value more 63!");
                            return null;
                        }
                        arg2 = String.valueOf(argInt);
                    }
                    String binaryConst  = argToBinary(arg2, 0b10000000, line);
                    if(binaryConst != null){
                        binaryCommand.append(binaryConst);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 =  getBinaryRON( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                break;
            case ASR:
            case LSL:
                if(matcher.find()) {
                    String arg = matcher.group();

                    String rel = null;
                    try {
                        int rel_int = Integer.parseInt(arg, 10);
                        if((rel_int  <1)  || (rel_int > 32)){
                            System.out.println(String.format("Rel for LSL and ASR must be [1..32]! in line[%s]", line));
                            return null;
                        }
                       rel =  Integer.toBinaryString(0b100000 | (rel_int-1) ).substring(1);
                    }catch (NumberFormatException e){
                        System.out.println(String.format("Invalid rel for line [%s]", line));
                    }
                    if(rel != null){
                        binaryCommand.append(rel);
                    } else {
                        return null;
                    }
                }else {
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 =  getBinaryRON( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                break;
            case CDJNZ:
                if(matcher.find()) {
                    String metka = matcher.group();
                    String metka2 = metka;
                    if(metka.contains("$")){
                        metka2 = metka.replace("$","");
                    }
                    Integer numberROW = arrayMetok.get(metka2);
                    if(numberROW != null) {
                        int relInt;
                        if(i > numberROW){
                            relInt = i - numberROW ;
                        }else{
                            System.out.println("Error! Label :" + metka2 + " comes after CJNZ in line ["+i+"]. Required label before CJNZ ["+line+"]");
                            return null;
                        }
                        metka2 = String.valueOf(relInt);
                    }
                    String rel =  getRelModulus(metka2, 0b100000, line,4);
                    if(rel != null){
                        binaryCommand.append(rel);
                    }else{
                        return null;
                    }
                }else {
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 = null;
                    binArg2 =  getBinaryCH( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                break;
            case EQUAL0:
                if(matcher.find()) {
                    String metka = matcher.group();
                    String metka2 = metka;
                    if(metka.contains("$")){
                        metka2 = metka.replace("$","");
                    }
                    Integer numberROW = arrayMetok.get(metka2);
                    if(numberROW != null) {
                        int relInt;
                        if(i > numberROW){
                            relInt = numberROW - i;
                        }else{
                            relInt = numberROW - i;
                        }
                        metka2 = String.valueOf(relInt);
                    }
                    String rel =  getRel(metka2, 0b100000, line,4);
                    if(rel != null){
                        binaryCommand.append(rel);
                    }else{
                        return null;
                    }
                }else {
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 = null;
                             binArg2 =  getBinaryRON( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                break;

            case MORE:
            case EQUAL:
                if(matcher.find()) {
                    String metka = matcher.group();
                    String metka2 = metka;
                    if(metka.contains("$")){
                        metka2 = metka.replace("$","");
                    }
                    Integer numberROW = arrayMetok.get(metka2);
                    if(numberROW != null) {
                        int relInt;
                        if(i > numberROW){
                            relInt = numberROW - i;
                        }else{
                            relInt = numberROW - i;
                        }
                        metka2 = String.valueOf(relInt);
                    }
                    String rel =  getRel(metka2, 0b100000, line,4);
                    if(rel != null){
                        binaryCommand.append(rel);
                    }else{
                        return null;
                    }
                }else {
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 =  getBinaryRON( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 =  getBinaryRON( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                break;

            case JUMP:
                if(matcher.find()){
                    String metka = matcher.group();
                    String metka2 = metka;
                    if(metka.contains("$")){
                        metka2 = metka.replace("$","");
                    }
                    Integer numberROW = arrayMetok.get(metka2);
                    if(numberROW != null){
                        Integer  relInt = numberROW - i;
                        metka2 = String.valueOf(relInt);
                        String binaryAddressROM =    getRel(metka2, 0b100000000,  line, 7);

                        if(binaryAddressROM != null){
                            lines.set(i, lines.get(i).replace(metka, numberROW.toString()));
                            binaryCommand.append(binaryAddressROM);
                        }else{
                            return null;
                        }
                    }else{
                        System.out.println("Error! Invalid metka in line:["+line+"]");
                        return null;

                    }
                }else{
                    return null;
                }
                break;
            case DJNZ:
                if(matcher.find()) {
                    String metka = matcher.group();
                    String metka2 = metka;
                    if(metka.contains("$")){
                        metka2 = metka.replace("$","");
                    }
                    Integer numberROW = arrayMetok.get(metka2);
                    if(numberROW != null) {
                        Integer  relInt;
                        if(i > numberROW){
                            relInt = numberROW - i;
                        }else{
                            relInt = numberROW - i;
                        }
                        metka2 = String.valueOf(relInt);
                    }

                    String rel =    getRelModulus(metka2, 0b1000000,  line, 5);
                    if(rel != null){
                        binaryCommand.append(rel );
                    }else{
                        return null;
                    }
                }else {
                    return null;
                }
                if(matcher.find()) {
                    String binArg2 =  getBinaryRON( matcher.group(), line);
                    if(binArg2!=null) {
                        binaryCommand.append(binArg2);
                    }else{
                        return null;
                    }
                }else{
                    return null;
                }
                break;

        }
        return binaryCommand.toString();
    }



    private String getRelModulus(String arg, int mask, String line, Integer module){
        try {
            Double  rang = Math.pow(2, module);
            int ran = rang.intValue();
            Integer mModule = 0;
            Integer pModule =  ran -1;
            Integer  argInt = Integer.parseInt( arg, 10);
            if(argInt < -ran  || argInt > ran -1){
                System.out.println("Error! Argument ["+arg+"] out of range ["+mModule.toString()+";"+pModule.toString()+"] in line:["+line+"]");
                return null;
            }
            if(argInt >= 0) {
                return Integer.toBinaryString(mask | argInt).substring(1);
            }else {

                String s = Integer.toBinaryString(argInt);
                return   s.substring(32-module-1, 32);
            }
        }     catch (NumberFormatException ex){
            System.out.println("Error! Invalid argument in line:["+line+"]");
            return null;
        }



    }


    private String getRel(String arg, int mask, String line, Integer module){
        try {
            Double  rang = Math.pow(2, module);
            int ran = rang.intValue();
            Integer mModule = -ran;
            Integer pModule =  ran -1;
            Integer  argInt = Integer.parseInt( arg, 10);
            if(argInt < -ran  | argInt > ran -1){
                System.out.println("Error! Argument ["+arg+"] out of range ["+mModule.toString()+";"+pModule.toString()+"] in line:["+line+"]");
                return null;
            }
            if(argInt >= 0) {
                return Integer.toBinaryString(mask | argInt).substring(1);
            }else {

                String s = Integer.toBinaryString(argInt);
                return   s.substring(32-module-1, 32);
            }
        }     catch (NumberFormatException ex){
            System.out.println("Error! Invalid argument in line:["+line+"]");
            return null;
        }



    }


    public String argToBinary(String arg, int mask,  String line){
        String argUC = arg.toUpperCase();
        if( argUC.contains("H")){
            argUC = argUC.replace("H","");
            return argToBinary(argUC, mask, line, 16);
        }else if(argUC.contains("X")) {
            argUC = argUC.replace("X","");
            return argToBinary(argUC, mask, line, 8);
        }else if(argUC.contains("B")) {
            argUC = argUC.replace("B","");
            return argToBinary(argUC, mask, line, 2);
        }else {
            return argToBinary(argUC, mask, line, 10);
        }
    }



    private String argToBinary(String arg, int mask,  String line, Integer sys){
        try {
            Integer  argInt = Integer.parseInt( arg, sys);
            return Integer.toBinaryString(mask | argInt ).substring(1);
        }     catch (NumberFormatException ex){
            System.out.println("Error! Invalid argument in line:["+line+"]");
            return null;
        }

    }



    private String getBinaryRON(String arg, String line ){
        try {
            if (arg.length() == 2 & arg.contains("R")) {
                Integer numRON = Integer.parseInt(arg.replaceAll("R", ""));
                if(numRON >= 0  & numRON < 8) {
                    return Integer.toBinaryString(0b1000 | numRON).substring(1);
                }
            }
            System.out.println("Error! Invalid argument in line:["+line+"]");
            return null;

        }
        catch (NumberFormatException ex){
            System.out.println("Error! Invalid number in line:["+line+"]");
            return null;
        }
    }


    private String getBinaryCH(String arg, String line ){
        try {
            if (arg.length() == 3 & arg.contains("CH")) {
                Integer numCH = Integer.parseInt(arg.replaceAll("CH", ""));
                if(numCH >= 0  &&  (numCH < 8)) {
                    return Integer.toBinaryString(0b1000 | numCH).substring(1);
                }
            }
            System.out.println("Error! Invalid argument in line:["+line+"]");
            return null;

        }
        catch (NumberFormatException ex){
            System.out.println("Error! Invalid number in line:["+line+"]");
            return null;
        }
    }

    private void deleteComments(){
        Pattern patternAnyWord = Pattern.compile("[\\w|$|-|/*]+");

        Pattern patternComment = Pattern.compile("//.*");
        Pattern patternLineComment = Pattern.compile("^[\\s]*//.*");
        for(int i = 0; i< lines.size();i++){
            String line = lines.get(i);
            Matcher matcher = patternComment.matcher(line);
            Matcher matcher2 = patternAnyWord.matcher(line);
            if(matcher.find()) {
                if(patternLineComment.matcher(line).find()){
                    lines.remove(i);
                    i--;
                }else{
                    String comment = matcher.group(0);
                    lines.set(i, lines.get(i).replace(comment, "").replaceAll("\t",""));
                }
            }else if(!matcher2.find()){
                lines.remove(i);
                i--;
            }
        }
    }


    private void deleteAsmComments(){

        deleteComments();

        String text = "";

        for(String line : lines){
                text = text + line + "\n";

        }

        String REGEX = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)";
        Pattern patternComment= Pattern.compile(REGEX);
        Matcher matcher = patternComment.matcher(text);


        while(matcher.find()){
            String comment = matcher.group(0);
            text = text.replace(comment, "");
        }
        String[] clearLinesArray = text.split("\n");
        List<String> clearlinesList = new ArrayList<String>(Arrays.asList(clearLinesArray));
        lines.clear();
        for(String line : clearlinesList){
            if(!line.equals("")){
                lines.add(line);
            }
        }


    }


    private void preparePredefinedFunction(){
        Pattern patternMetka = Pattern.compile("^[\\w|#|%]+");
        for(int i = 0; i< lines.size();i++){
            Matcher matcher = patternMetka.matcher(lines.get(i));
            if(matcher.find()) {
                String commandStr = matcher.group();
                if(commandStr.contains("#") && commandStr.contains("%")) {
                    int index = commandStr.indexOf("%");
                    index ++;
                    String numStr = commandStr.substring(index, commandStr.length());
                    if(numStr.length() > 0 ) {
                        int numBinaryLine = Integer.parseInt(numStr);
                        int sizeExistCode = i;
                        for(int k = 0; k < numBinaryLine - sizeExistCode; k++ ) {
                            lines.add(i, "TXT");
                        }
                        i = numBinaryLine - sizeExistCode + i ;
                        lines.remove(i);
                        i--;
                        continue;
                    }else {
                        lines.remove(i);
                        i--;
                        continue;
                    }
                }
            }
        }
    }




    private void getArrayMetok(){
        Pattern patternMetka = Pattern.compile("^[\\w]+:");
        for(int i = 0; i< lines.size();i++){
            Matcher matcher = patternMetka.matcher(lines.get(i));
            if(matcher.find()) {
                String metka = matcher.group(0);
                String metka2 = metka.replace(":","");
                if (arrayMetok.get(metka2) != null) {
                    System.out.println("Warning! Metka [" + metka + "] in line:" + i + " already exist in line:" + arrayMetok.get(metka) + ". Metka in line:" + i + " is ignored!");
                } else {

                    arrayMetok.put(metka2, i);
                }
            }
        }
    }




}
