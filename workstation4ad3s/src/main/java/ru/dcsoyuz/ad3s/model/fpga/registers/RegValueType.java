package ru.dcsoyuz.ad3s.model.fpga.registers;

public enum RegValueType {

    VALUE15_0_SIGNED (15, 0, true),
    VALUE15_0_UNSIGNED (15, 0, false),
    VALUE15_4_UNSIGNED (15, 4, false),

    VALUE15_4_SIGNED (15, 4, true),
    VALUE8_0_UNSIGNED (8, 0, false),

    VALUE7_0_UNSIGNED (7, 0, false),
    VALUE11_0_SIGNED (11, 0, true),

    VALUE11_0_UNSIGNED (11, 0, false),
    VALUE_FIELDS (15, 0, false);

    int msb;
    int lsb;
    boolean isSigned;

    RegValueType(int msb, int lsb, boolean isSigned) {
        this.msb = msb;
        this.lsb = lsb;
        this.isSigned = isSigned;
    }

    public int getMsb() {
        return msb;
    }

    public int getLsb() {
        return lsb;
    }

    public boolean isSigned() {
        return isSigned;
    }
}
