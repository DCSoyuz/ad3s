package ru.dcsoyuz.ad3s.model.fpga.registers;

import org.junit.Test;
import static org.junit.Assert.*;

public class AllRegAddrTest {

    @Test
    public void testAllRegAddrCount() {
        assertTrue("Expected at least 10 register addresses",
                AllRegAddr.values().length >= 10);
    }

    @Test
    public void testGetAllRegAddrReturnsNullForUnknown() {
        assertNull(AllRegAddr.getAllRegAddr(-1));
        assertNull(AllRegAddr.getAllRegAddr(99999));
    }

    @Test
    public void testGetAllRegAddrReturnsValueForKnown() {
        AllRegAddr first = AllRegAddr.values()[0];
        AllRegAddr result = AllRegAddr.getAllRegAddr(first.ordinal());
        assertEquals(first, result);
    }
}
