package ru.dcsoyuz.ad3s.model.fpga.registers;

public enum RegType {

    CR( "DB", 64),
    CN( "DB",80),
    C1("DB.HAND1", 0),
    C2("DB.HAND2", 32),
    R("ROM", 1536),
    P1( "DB",76),
    P2( "DB",78),
    CP1 ("CPU1", 512),
    CP2 ("CPU2", 1024)
    ;


    private String hdlPath;
    private int address;

    RegType(String hdlPath, int address) {
        this.hdlPath = hdlPath;
        this.address = address;
    }

    public String getHdlPath() {
        return hdlPath;
    }

    public int getAddress() {
        return address;
    }
}
