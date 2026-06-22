package ru.dcsoyuz.ad3s.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class ConfPropTest {

    @Test
    public void testConfPropHasEssentialProperties() {
        assertNotNull(ConfProp.FILE_PATH_HEX_CODES);
        assertNotNull(ConfProp.COM_PORT);
        assertNotNull(ConfProp.RECORD_FOLDER);
        assertNotNull(ConfProp.FILE_PATH_RECORD_TXT);
        assertNotNull(ConfProp.FILE_PATH_CURRENT_FILE);
    }

    @Test
    public void testConfPropCount() {
        assertTrue("Expected at least 10 ConfProp values", ConfProp.values().length >= 10);
    }

    @Test
    public void testConfPropRecordFolderExists() {
        boolean found = false;
        for (ConfProp p : ConfProp.values()) {
            if (p.name().equals("RECORD_FOLDER")) { found = true; break; }
        }
        assertTrue("RECORD_FOLDER should exist", found);
    }
}
