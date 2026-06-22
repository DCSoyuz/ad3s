package ru.dcsoyuz.ad3s.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yuri.filatov on 15.07.2016.
 */
public class WorkstationConfig {
	private static final Logger logger = LoggerFactory.getLogger(WorkstationConfig.class);

    private static final String CONFIG_FILE_NAME = "config.properties";
    private Properties properties;

    private static WorkstationConfig instance;
    private static File configFile;

    private static File getConfigFile() {
        if (configFile == null) {
            File jarDir = getJarDirectory();
            configFile = new File(jarDir, CONFIG_FILE_NAME);
        }
        return configFile;
    }

    private static File getJarDirectory() {
        try {
            String jarPath = WorkstationConfig.class.getProtectionDomain()
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

    /**
     * Returns the base application directory.
     * <ul>
     *   <li>From JAR: parent directory of the JAR file (e.g. dist/)</li>
     *   <li>From IDE: target/ directory (when running from target/classes/)</li>
     * </ul>
     */
    public static File getBaseDirectory() {
        try {
            String loc = WorkstationConfig.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File f = new File(loc);
            if (f.isFile()) {
                // Running from JAR → parent of JAR
                return f.getParentFile();
            }
            // Running from IDE (target/classes/) → go up one level to target/
            String name = f.getName();
            if ("classes".equals(name)) {
                File parent = f.getParentFile();
                if (parent != null && "target".equals(parent.getName())) {
                    return parent;
                }
                // Fallback: just go up one level from classes/
                return parent != null ? parent : f;
            }
            return f;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    public WorkstationConfig() {
        try{
            properties = new Properties();
            File file = getConfigFile();
            if(file.exists()) {
                properties.load(new FileInputStream(file));
            }else {
                properties.store(new FileOutputStream(file), "");
            }
        } catch ( Exception e){
            logger.error("Error in WorkstationConfig", e);
        }
    }

    private static WorkstationConfig getInstance() {
        if(instance == null){
            instance = new WorkstationConfig();
        }
        return  WorkstationConfig.instance;
    }


    public static String getProperty(ConfProp property){
        return  getInstance().getProperties().getProperty(property.name());
    }

    public static String getProperty(String property){
        return  getInstance().getProperties().getProperty(property);
    }

    public static void setProperty(ConfProp property, String newValue ){
        getInstance().getProperties().setProperty(property.name(), newValue);
    }
    public static void setProperty(String property, String newValue ){
        getInstance().getProperties().setProperty(property, newValue);
    }

    public static void removeProperty(String property) {
        getInstance().getProperties().remove(property);
    }


    public synchronized  static void storeProperties(){
        try {
            getInstance().getProperties().store(new FileOutputStream(getConfigFile()), "");
        }catch (Exception e){
            logger.error("Error in storeProperties", e);
        }

    }

    private Properties getProperties() {
        return properties;
    }

    public static String getPathPropertiesFile(){
        return getConfigFile().getAbsolutePath();
    }


}
