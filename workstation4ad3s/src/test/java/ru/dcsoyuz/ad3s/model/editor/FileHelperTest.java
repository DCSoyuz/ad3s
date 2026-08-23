package ru.dcsoyuz.ad3s.model.editor;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;

import static org.junit.Assert.*;

public class FileHelperTest {

    /** Regression for the Prog UOTP NPE: unset property must fall back to user_generated_files. */
    @Test
    public void testResolveDirFallsBackWhenPropertyMissing() {
        String saved = WorkstationConfig.getProperty(ConfProp.FILE_PATH_HEX_CODES);
        WorkstationConfig.removeProperty(ConfProp.FILE_PATH_HEX_CODES.name());
        try {
            File dir = FileHelper.resolveDir(ConfProp.FILE_PATH_HEX_CODES);
            assertNotNull(dir);
            assertTrue("Fallback dir must exist", dir.isDirectory());
            assertEquals("user_generated_files", dir.getName());
        } finally {
            if (saved != null) {
                WorkstationConfig.setProperty(ConfProp.FILE_PATH_HEX_CODES, saved);
            }
        }
    }

    @Test
    public void testResolveDirUsesPropertyDirectory() throws Exception {
        String saved = WorkstationConfig.getProperty(ConfProp.FILE_PATH_HEX_CODES);
        File tmp = Files.createTempDirectory("ad3s_resolve_dir").toFile();
        try {
            WorkstationConfig.setProperty(ConfProp.FILE_PATH_HEX_CODES, tmp.getAbsolutePath());
            File dir = FileHelper.resolveDir(ConfProp.FILE_PATH_HEX_CODES);
            assertEquals(tmp.getAbsolutePath(), dir.getAbsolutePath());
        } finally {
            WorkstationConfig.removeProperty(ConfProp.FILE_PATH_HEX_CODES.name());
            if (saved != null) {
                WorkstationConfig.setProperty(ConfProp.FILE_PATH_HEX_CODES, saved);
            }
        }
    }
}
