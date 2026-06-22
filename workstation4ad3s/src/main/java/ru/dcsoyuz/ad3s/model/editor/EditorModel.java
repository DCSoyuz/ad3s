package ru.dcsoyuz.ad3s.model.editor;

import org.apache.commons.io.FileUtils;
import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.editor.IAppFrameEventListener;
import ru.dcsoyuz.ad3s.form.editor.IDataEditorListener;
import ru.dcsoyuz.ad3s.form.editor.IModelEditorEventListener;
import ru.dcsoyuz.ad3s.model.fpga.parser.ModeAttrFile;
import ru.dcsoyuz.ad3s.model.fpga.parser.BlockCode;
import ru.dcsoyuz.ad3s.model.fpga.parser.FileType;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yuri.filatov on 15.07.2016.
 */
public class EditorModel {
	private static final Logger logger = LoggerFactory.getLogger(EditorModel.class);

    private IDataEditorListener dataEditorListener;
    private List<IAppFrameEventListener>  appFrameEventListeners = new ArrayList<IAppFrameEventListener>();

    private File currentFile;
    private final List<File> openFiles = new ArrayList<>();
    private ModeAttrFile currentAttr;
    private String MODE_FILE_NAME = "mode.config";
    private final int BUFFER_SIZE = 8912;
    private Map<File, OutputCodes>  outputCodes = new HashMap<>();
    private Map<ModeAttrFile, OutputCodes>  outputCodesAttr = new HashMap<>();
    private String pathHexFiles;


    private IModelEditorEventListener modelEditorEventListener;
    private String activeTab = "asm-editor";
    private Map<File, ModeAttrFile> fileAttrs = new HashMap<>();


    public ModeAttrFile getFileAttrByName(String name){
        for(ModeAttrFile attr : fileAttrs.values()){
            if(attr.getName().contains(name)){
                return attr;
            }
        }
        return null;
    }


    public EditorModel() {
        String prop;
        if((prop = WorkstationConfig.getProperty(ConfProp.FILE_PATH_HEX_CODES))!=null) {
            setPathHexFiles(prop);
        }else{
            setPathHexFiles("D:/");
        }

        initModeConfigAttrs();

    }

    public File getFileByName(String name){
        for(File file: fileAttrs.keySet()){
            if(file.getName().contains(name)){
                return file;
            }
        }
        return null;
    }



    public void setModelEditorEventListener(IModelEditorEventListener modelEditorEventListener) {
        this.modelEditorEventListener = modelEditorEventListener;
    }

    public void saveCurrentFile(){
        dataEditorListener.saveFile();
    }

    public void cleanTextErrors(){
        modelEditorEventListener.cleanTextErorrs();
    }


    public void addDataEditorListener(IDataEditorListener listener){
        dataEditorListener = listener;
    }

    public void setOutputCodes(File file, String clearAssemblerCodes, String textHexCodes, String binaryCodes){
        outputCodes.put(file, new OutputCodes(clearAssemblerCodes, textHexCodes, binaryCodes));

        //dataEditorListener.updateOutputCodes();
    }

    public String getCurrentText(){
        return dataEditorListener.getCurrentText();
    }


    public void addAppFrameEventListener(IAppFrameEventListener listener){
        appFrameEventListeners.add(listener);
    }


    public void  notifyAllOfUpdateCurrentFile(File newFile, File oldFile ){
        for(IAppFrameEventListener listener : appFrameEventListeners){
            listener.updateCurrentFile(newFile, oldFile);
        }
    }

    public String getActiveTab() {
        return activeTab;
    }

    public void openActiveTab(String activeTab) {
        this.activeTab = activeTab;
        modelEditorEventListener.updateCurrentTab();
    }

    public void setActiveTab(String activeTab) {
        this.activeTab = activeTab;
    }






    public File getCurrentFile() {
        if(currentFile == null){
            String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_CURRENT_FILE);
            if(path != null){
                currentFile = new File(Paths.get(path).toUri());
                if(currentFile.exists()){
                    return currentFile;
                }
            }
            // First launch: open user_asm from risc_programs next to JAR
            File jarDir = getJarDirectory();
            File userAsmDir = new File(jarDir, "risc_programs" + File.separator + "user_asm");
            if (!userAsmDir.exists()) {
                try {
                    copyResourceDir("user_asm", userAsmDir);
                } catch (IOException e) {
                    logger.error("Error in getCurrentFile", e);
                }
            }
            currentFile = new File(userAsmDir.getPath() + File.separator + "base_ram.txt");

        }
        return currentFile;
    }

    private File getJarDirectory() {
        try {
            String jarPath = EditorModel.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) {
                return jarFile.getParentFile();
            }
            return jarFile;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    public void setCurrentFile(File currentFile) {
        File oldFile = this.currentFile;
        this.currentFile = currentFile;
        if (!openFiles.contains(currentFile)) {
            openFiles.add(currentFile);
        }
        WorkstationConfig.setProperty(ConfProp.FILE_PATH_CURRENT_FILE, currentFile.getPath());
        initModeConfigAttrs();
        currentAttr = getFileAttr(currentFile);
        notifyAllOfUpdateCurrentFile(currentFile, oldFile);
    }

    public void closeFile(File file) {
        openFiles.remove(file);
        if (currentFile != null && currentFile.equals(file)) {
            if (!openFiles.isEmpty()) {
                currentFile = openFiles.get(openFiles.size() - 1);
            } else {
                currentFile = null;
            }
        }
    }

    public List<File> getOpenFiles() {
        return new ArrayList<>(openFiles);
    }

    public void setCurrentFileQuiet(File file) {
        WorkstationConfig.setProperty(ConfProp.FILE_PATH_CURRENT_FILE, file.getPath());
        this.currentFile = file;
    }

    public String getPathHexFiles() {
        return pathHexFiles;
    }


    public void setPathHexFiles(String pathHexFiles) {

        this.pathHexFiles = pathHexFiles;
    }





    public void updateOutputCodes(){
        dataEditorListener.updateOutputCodes();
    }

    public OutputCodes getOutputTextCodes(File newFile) {
        return outputCodes.get(newFile);
    }

    public ModeAttrFile getCurrentAttr() {
        return currentAttr;
    }

    public void updateColors(){
        dataEditorListener.updateOutputCodes();
    };


    public void initModeConfigAttrs(){
        File file = getCurrentFile();
        String path = file.getParent() + File.separator;
        fileAttrs.clear();
        FileInputStream in;
        try {
            in = new FileInputStream(path + MODE_FILE_NAME);
        }catch (FileNotFoundException e){
            return;
        }

        try{

            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFFER_SIZE);
            String line;
            br.readLine();
            while((line = br.readLine())!= null) {
                String[] data = line.split("\t");
                ModeAttrFile attr = new ModeAttrFile();
                int i = 0;
                for(String word : data){
                    if(word != null & !word.equals("")){
                        switch(i){
                            case 0:
                                attr.setName(word);
                                break;
                            case 1:
                                FileType type = FileType.valueOf(word);
                                if(type != null) {
                                    attr.setFileType(type);
                                }else{
                                    logger.warn("FileType attr for file:{}not recognized", attr.getName());
                                }
                                break;
                            case 2:
                                Integer size = Integer.parseInt(word);
                                attr.setSize(size);
                                break;
                            case 3:
                                Integer numBits = Integer.parseInt(word);
                                attr.setNumBits(numBits);
                                break;
                            case 4:
                                BlockCode blockCode = BlockCode.getName(word);
                                if(blockCode != null){
                                    attr.setBlockCode(blockCode);
                                }else {
                                    logger.warn("Block Code attr for file:{}not recognized", attr.getName());
                                }
                                break;
                            case 5:
                                Boolean program = Boolean.parseBoolean(word);
                                if(program != null){
                                    attr.setProgram(program);
                                }else {
                                    logger.warn("Program attr for file:{} not recognized [true, false]", attr.getName());
                                }
                                break;
                            case 6:
                                Integer address = Integer.parseInt(word);
                                attr.setAddress(address);
                                break;
                            case 7:
                                Integer hexSize = Integer.parseInt(word);
                                attr.setHexSize(hexSize);
                                break;
                        }
                        i++;
                    }else{
                        logger.debug("Not define attr for file:{}", attr.getName());
                    }

                }
                fileAttrs.put(new File(path + attr.getName()), attr);
            }

        } catch (FileNotFoundException e) {
            logger.error("Error in initModeConfigAttrs", e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error in initModeConfigAttrs", e);
        } catch (IOException e) {
            logger.error("Error in initModeConfigAttrs", e);
        }
    }


    public ModeAttrFile getFileAttr(File file ){
        ModeAttrFile attr = fileAttrs.get(file);
        if(attr != null) {
            return fileAttrs.get(file);
        }else {
            logger.warn("Not found attributes for file: {} in mode.config ", file.getName());
            return null;
        }
    }

    public ModeAttrFile getCurrentFileAttr(){
        return getFileAttr(currentFile);
    }

    private void copyResourceDir(String resourcePath, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        // Try as filesystem directory first (works in IDE)
        java.net.URL url = ClassLoader.getSystemResource(resourcePath);
        if (url != null && "file".equals(url.getProtocol())) {
            try {
                File srcDir = new File(url.toURI());
                if (srcDir.isDirectory()) {
                    FileUtils.copyDirectory(srcDir, destDir);
                    return;
                }
            } catch (URISyntaxException ignored) {}
        }
        // Fallback: copy known files from classpath (works from fat-jar)
        String[] knownFiles = {
            "base_ram.txt", "cpu1_com.asm", "cpu1_data.hex", "cpu1_ram.txt",
            "cpu2_com.asm", "cpu2_data.hex", "cpu2_ram.txt", "mode.config", "rom_BOTP.hex"
        };
        for (String file : knownFiles) {
            java.net.URL fileUrl = ClassLoader.getSystemResource(resourcePath + "/" + file);
            if (fileUrl != null) {
                File outFile = new File(destDir, file);
                if (!outFile.exists()) {
                    try (InputStream is = fileUrl.openStream();
                         OutputStream os = new FileOutputStream(outFile)) {
                        byte[] buf = new byte[8192];
                        int len;
                        while ((len = is.read(buf)) > 0) {
                            os.write(buf, 0, len);
                        }
                    }
                }
            }
        }
    }

}
