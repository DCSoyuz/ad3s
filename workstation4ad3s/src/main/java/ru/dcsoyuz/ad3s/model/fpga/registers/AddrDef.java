package ru.dcsoyuz.ad3s.model.fpga.registers;

public enum AddrDef {

    A_GLOBAL_FIRM       (0 ),
    A_GLOBAL_CPU1       (1 ),
    A_GLOBAL_CPU2       (2 ),
    A_GLOBAL_PROM       (3 ),

    A_FIRM_CC1          (0 ),
    A_FIRM_CC2          (1 ),
    A_FIRM_CC           (2 ),

    A_FIRM_CR(0 ),

    A_FIRM_CN(1)
    ;

    public int getAbsoluteAddr(int relAddr){
        switch(this) {
            case A_GLOBAL_FIRM:
            case A_GLOBAL_CPU1:
            case A_GLOBAL_CPU2:
            case A_GLOBAL_PROM:
                return 512*addr + relAddr;
            default:
                return 32*addr + relAddr;
        }
    }

    public static boolean isCPUaddress(int addr){
        if(addr >= 512*A_GLOBAL_CPU1.addr && addr <512*A_GLOBAL_PROM.addr){
            return true;
        }else {
            return false;
        }


    }

    int addr;

    AddrDef(int addr) {
        this.addr = addr;
    }


    public int getAddr() {
        return addr;
    }

}
