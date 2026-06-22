package ru.dcsoyuz.ad3s.model.editor;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yuri.filatov on 08.08.2016.
 */
public class FileHelper {
	private static final Logger logger = LoggerFactory.getLogger(FileHelper.class);

    private static final int BUFFER_SIZE = 8912;



    public static void createOutputFile(ConfProp prop, String fileName, List<String> list, String ext){
        String propPath = WorkstationConfig.getProperty(prop);
        if (propPath == null || propPath.isEmpty()) {
            File jarDir = getJarDirectory();
            File genDir = new File(jarDir, "user_generated_files");
            if (!genDir.exists()) genDir.mkdirs();
            propPath = genDir.getAbsolutePath();
        }
        File path = new File(propPath);
        if(!path.isDirectory()) {
            path = path.getParentFile();
        }
        int indexPoint =fileName.indexOf(".");
        String name =fileName.substring(0, indexPoint);

        Path outFile = Paths.get(path.getPath()+ File.separator  + name + ext);
        try {
            Files.write(outFile, list, Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error("Error in createOutputFile", e);
        }
    }

    public static void deleteOutputFile(ConfProp prop, String fileName, String ext){
        String propPath = WorkstationConfig.getProperty(prop);
        if (propPath == null || propPath.isEmpty()) {
            File genDir = new File(getJarDirectory(), "user_generated_files");
            if (!genDir.exists()) genDir.mkdirs();
            propPath = genDir.getAbsolutePath();
        }
        File path = new File(propPath);
        if(!path.isDirectory()) {
            path = path.getParentFile();
        }
        int indexPoint =fileName.indexOf(".");
        String name =fileName.substring(0, indexPoint);

        Path outFile = Paths.get(path.getPath()+ File.separator  + name + ext);
        try {
            Files.deleteIfExists(outFile);
        } catch (IOException e) {
            logger.error("Error in deleteOutputFile", e);
        }
    }



    public static List<Integer> getValuesFromFile(String filename, Integer count, String strPath){

        File path;
        List<Integer> res = new ArrayList<>();

        if(strPath == null || strPath == "") {
            if (!filename.contains(File.separator)) {
                String hexPath = WorkstationConfig.getProperty(ConfProp.FILE_PATH_HEX_CODES);
                if (hexPath == null || hexPath.isEmpty()) {
                    File genDir = new File(getJarDirectory(), "user_generated_files");
                    if (!genDir.exists()) genDir.mkdirs();
                    hexPath = genDir.getAbsolutePath();
                }
                File file = new File(hexPath);
                if (!file.isDirectory()) {
                    file = file.getParentFile();
                }
                path = Paths.get(file.getPath() + File.separator + filename).toFile();
            } else {
                path = Paths.get(filename).toFile();
            }
        }else {
            path = Paths.get(strPath + File.separator + filename).toFile();
        }
        BufferedReader br = null;
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            br = new BufferedReader(new InputStreamReader(in, "UTF-8"), BUFFER_SIZE);
            String line;
            int k =1;
            while((line = br.readLine())!= null && k <= count ){
                res.add(Integer.parseInt(line));
                k++;
            }
            if(k < count){
                logger.debug("{} file consist less values than {} !", filename, count.toString());
            }

        } catch (FileNotFoundException e) {
            logger.error("Error in deleteOutputFile", e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Error in deleteOutputFile", e);
        } catch (IOException e) {
            logger.error("Error in deleteOutputFile", e);
        }

        return res;
    }



    public static void createFile(ConfProp prop, String fileName, List<String> list){
        String propPath = WorkstationConfig.getProperty(prop);
        if (propPath == null || propPath.isEmpty()) {
            File genDir = new File(getJarDirectory(), "user_generated_files");
            if (!genDir.exists()) genDir.mkdirs();
            propPath = genDir.getAbsolutePath();
        }
        File path = new File(propPath);
        if(!path.isDirectory()) {
            path = path.getParentFile();
        }
        Path outFile = Paths.get(path.getPath()+ File.separator  + fileName);
        try {
            Files.write(outFile, list, Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error("Error in createFile", e);
        }


    }

    public static void createFile(Path Path, List<String> list){
        try {
            Files.write(Path, list, Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error("Error in createFile", e);
        }


    }


    public static void createFile(String path,  List<String> text){


        Path outFile = Paths.get(path);
        try {
            Files.write(outFile, text, Charset.forName("UTF-8"));
        } catch (IOException e) {
            logger.error("Error in createFile", e);
        }

    }

    private static File getJarDirectory() {
        try {
            String jarPath = FileHelper.class.getProtectionDomain()
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
}
