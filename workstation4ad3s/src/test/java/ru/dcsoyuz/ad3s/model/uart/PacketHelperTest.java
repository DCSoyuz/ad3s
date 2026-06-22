package ru.dcsoyuz.ad3s.model.uart;

import org.junit.Test;
import static org.junit.Assert.*;

public class PacketHelperTest {

    @Test
    public void testGetUnsignedWord16BitInt() {
        int result = PacketHelper.getUnsignedWord16bitInt((byte) 0xAB, (byte) 0xCD);
        assertEquals(0xCDAB, result);
    }

    @Test
    public void testGetUnsignedWord16BitIntZero() {
        int zero = PacketHelper.getUnsignedWord16bitInt((byte) 0, (byte) 0);
        assertTrue("Expected 0", zero == 0);
    }

    @Test
    public void testGetUnsignedWord16BitIntMax() {
        int result = PacketHelper.getUnsignedWord16bitInt((byte) 0xFF, (byte) 0xFF);
        assertEquals(0xFFFF, result);
    }
}
