package ru.dcsoyuz.ad3s.form.terminal;

import ru.dcsoyuz.ad3s.model.fpga.registers.AddrCpuHand;

import java.util.ArrayList;
import java.util.List;
public enum HandValueSetting {
    FullAngle (AddrCpuHand.A_CPU_HAND_COORD, 27, 0, false),
    Wca (AddrCpuHand.A_CPU_HAND_SINCOS, 27, 16,true),
    BScos (AddrCpuHand.A_CPU_HAND_SINCOS, 15),
    ex_shifted (AddrCpuHand.A_CPU_HAND_SINCOS, 14),
    Wsa (AddrCpuHand.A_CPU_HAND_SINCOS, 13,2,true),
    BSsin (AddrCpuHand.A_CPU_HAND_SINCOS, 1),
    ex_ref (AddrCpuHand.A_CPU_HAND_SINCOS, 0),


    VirtualS(AddrCpuHand.A_CPU_HAND_OUTVIRTSIN, 27, 15,true),
    ex_recovered_S(AddrCpuHand.A_CPU_HAND_OUTVIRTSIN, 14),

    BSsin_V(AddrCpuHand.A_CPU_HAND_OUTVIRTSIN, 13,1,true),

    ex_recovered_90dgr_S(AddrCpuHand.A_CPU_HAND_OUTVIRTSIN, 0),
    Amp_metric(AddrCpuHand.A_CPU_HAND_ERRAMPMETRIC, 27, 16, false),
    Err_metric(AddrCpuHand.A_CPU_HAND_ERRAMPMETRIC, 15, 0, true),
    FullVel(AddrCpuHand.A_CPU_HAND_VEL, 27,0,true),

    PhiC(AddrCpuHand.A_CPU_HAND_PHIMODEL,27,14 ,true),
    PhiS(AddrCpuHand.A_CPU_HAND_PHIMODEL, 13, 0, true),

    VirtualC(AddrCpuHand.A_CPU_HAND_OUTVIRTCOS, 27,15,true),

    ex_recovered_C(AddrCpuHand.A_CPU_HAND_OUTVIRTCOS, 14),
    BScos_V (AddrCpuHand.A_CPU_HAND_OUTVIRTCOS, 13,1,true),
    ex_recovered_90dgr_C(AddrCpuHand.A_CPU_HAND_OUTVIRTCOS, 0),
    STAT (AddrCpuHand.A_CPU_HAND_STAT, 15,0, false);

    HandValueSetting(AddrCpuHand addr, int index) {
        this(addr, index, index, false);
    }

    HandValueSetting(AddrCpuHand addr, int msb, int lsb, boolean isSigned) {
        this.addr = addr;
        this.msb = msb;
        this.lsb = lsb;
        this.isSigned = isSigned;
    }

    AddrCpuHand addr;

    int msb;
    int lsb;

    boolean isSigned;

    boolean isSplit;

    public AddrCpuHand getAddr() {
        return addr;
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

    public static List<HandValueSetting> getHandValueSettings(AddrCpuHand addrDef){
        List<HandValueSetting> list = new ArrayList<>();
        for(HandValueSetting setting : HandValueSetting.values()){
            if(setting.getAddr().equals(addrDef)){
                list.add(setting);
            }
        }
        return list;
    }

}
