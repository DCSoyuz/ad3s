package ru.dcsoyuz.ad3s.model.fpga.registers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum AddrCpuHand {

    A_CPU_HAND_COORD   (0),

    A_CPU_HAND_SINCOS (1),

    A_CPU_HAND_OUTVIRTSIN (2),

    A_CPU_HAND_ERRAMPMETRIC (3),

    A_CPU_HAND_VEL (4),

    A_CPU_HAND_PHIMODEL (5),

    A_CPU_HAND_OUTVIRTCOS(6),

    A_CPU_HAND_STAT(7);

    private static final Logger logger = LoggerFactory.getLogger(AddrCpuHand.class);

    int addr;

    AddrCpuHand(int addr) {
        this.addr = addr;
    }


    public int getAddr() {
        return addr;
    }

    public static AddrCpuHand getCpuHandAddrDef (int val){
        if(val >= 0 && val <= 7){
            for(AddrCpuHand addrDef : AddrCpuHand.values()){
                if(addrDef.getAddr() == val ){
                    return addrDef;
                }
            }
        } else {
            logger.debug("Error value for cpu hand addrDef");
        }
        return null;
    }
}
