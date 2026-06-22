package ru.dcsoyuz.ad3s.model.fpga.registers;

import java.util.ArrayList;
import java.util.List;

// https://www.ibm.com/docs/en/wxs/8.6.0?topic=applications-java-c-data-type-equivalents
public enum AllRegAddr  implements IAllRegAddr  {
    C1KampS         (Regs.KampS, RegType.C1),
    C1KampC         (Regs.KampC, RegType.C1),
    C1KbiasS        (Regs.KbiasS, RegType.C1),
    C1KbiasC        (Regs.KbiasC, RegType.C1),
    C1fbias         (Regs.fbias, RegType.C1),
    C1ExPhShft      (Regs.ExPhShft, RegType.C1),
    C1ExoStngs      (Regs.ExoStngs, RegType.C1),
    C1EXInc         (Regs.EXInc, RegType.C1),
    C1Amp_th         (Regs.Amp_th, RegType.C1),
    C1InputStngs    (Regs.InputStngs, RegType.C1),
    C1Lock_th       (Regs.Lock_th, RegType.C1),
    C1Zero          (Regs.Zero, RegType.C1),
    C1Mask          (Regs.Mask, RegType.C1),
    C1KonturStngs   (Regs.KonturStngs, RegType.C1),
    C1ResCntrl      (Regs.ResCntrl, RegType.C1),
    C1Vcnt_bound    (Regs.Vcnt_bound, RegType.C1),
    C1Coord         (Regs.Coord, RegType.C1),
    C1CoordHB       (Regs.CoordHB, RegType.C1),
    C1Vel           (Regs.Vel, RegType.C1),
    C1VelHB         (Regs.VelHB, RegType.C1),
    C1Stat          (Regs.Stat, RegType.C1),
    C1Pole_addi       (Regs.Pole_addi, RegType.C1),
    C1Amp_metric    (Regs.Amp_metric, RegType.C1),
    C1Err_metric    (Regs.Err_metric, RegType.C1),
    C1AdcS          (Regs.AdcS, RegType.C1),
    C1AdcC          (Regs.AdcC, RegType.C1),
    C1OutS          (Regs.OutS, RegType.C1),
    C1OutC          (Regs.OutC, RegType.C1),
    C1PhiS          (Regs.PhiS, RegType.C1),
    C1PhiC          (Regs.PhiC, RegType.C1),
    C1VirtualS      (Regs.VirtualS, RegType.C1),
    C1VirtualC      (Regs.VirtualC, RegType.C1),
    C2KampS         (Regs.KampS, RegType.C2),
    C2KampC         (Regs.KampC, RegType.C2),
    C2KbiasS        (Regs.KbiasS, RegType.C2),
    C2KbiasC        (Regs.KbiasC, RegType.C2),
    C2fbias         (Regs.fbias, RegType.C2),
    C2ExPhShft      (Regs.ExPhShft, RegType.C2),
    C2ExoStngs      (Regs.ExoStngs, RegType.C2),
    C2EXInc         (Regs.EXInc, RegType.C2),
    C2Amp_th        (Regs.Amp_th, RegType.C2),
    C2InputStngs    (Regs.InputStngs, RegType.C2),
    C2Lock_th       (Regs.Lock_th, RegType.C2),
    C2Zero          (Regs.Zero, RegType.C2),
    C2Mask          (Regs.Mask, RegType.C2),
    C2KonturStngs   (Regs.KonturStngs, RegType.C2),
    C2ResCntrl      (Regs.ResCntrl, RegType.C2),
    C2Vcnt_bound    (Regs.Vcnt_bound, RegType.C2),
    C2Coord         (Regs.Coord, RegType.C2),
    C2CoordHB       (Regs.CoordHB, RegType.C2),
    C2Vel           (Regs.Vel, RegType.C2),
    C2VelHB         (Regs.VelHB, RegType.C2),
    C2Stat          (Regs.Stat, RegType.C2),
    C2Pole_addi     (Regs.Pole_addi, RegType.C2),
    C2Amp_metric    (Regs.Amp_metric, RegType.C2),
    C2Err_metric    (Regs.Err_metric, RegType.C2),
    C2AdcS          (Regs.AdcS, RegType.C2),
    C2AdcC          (Regs.AdcC, RegType.C2),
    C2OutS          (Regs.OutS, RegType.C2),
    C2OutC          (Regs.OutC, RegType.C2),
    C2PhiS          (Regs.PhiS, RegType.C2),
    C2PhiC          (Regs.PhiC, RegType.C2),
    C2VirtualS      (Regs.VirtualS, RegType.C2),
    C2VirtualC      (Regs.VirtualC, RegType.C2),

    IC_addr         (Regs.IC_addr, RegType.CR),
    ADC_config      (Regs.ADC_config, RegType.CR),
    Mask_Stat       (Regs.Mask_Stat, RegType.CR),
    Flags_delay     (Regs.Flags_delay, RegType.CR),
    WR_lock         (Regs.WR_lock, RegType.CR),
    CMP_lth         (Regs.CMP_lth, RegType.CR),
    AFE_config      (Regs.AFE_config, RegType.CR),
    Mode_config     (Regs.Mode_config, RegType.CR),
    NOCLK_stat      (Regs.NOCLK_stat, RegType.CR),
    SPI_req         (Regs.SPI_req, RegType.CR),
    alive_cnt       (Regs.alive_cnt, RegType.CR),
    Stat_main       (Regs.Stat_main, RegType.CR),
    Dcpu1LB          (Regs.Dcpu1LB, RegType.CR),
    Dcpu1HB          (Regs.Dcpu1HB, RegType.CR),
    Dcpu2LB          (Regs.Dcpu2LB, RegType.CR),
    Dcpu2HB          (Regs.Dcpu2HB, RegType.CR),

    PLL_config      (Regs.PLL_config, RegType.CN),
    INIT_conf       (Regs.INIT_conf, RegType.CN),
    UOTP_ctrl       (Regs.UOTP_ctrl, RegType.CN),
    BUS_addr        (Regs.BUS_addr, RegType.CN),
    BOTP_ctrl       (Regs.BOTP_ctrl, RegType.CN), // One Time Programmable
    BOTP_addr       (Regs.BOTP_addr, RegType.CN),
    BOTP_data       (Regs.BOTP_data, RegType.CN),
    BOTP_out        (Regs.BOTP_out, RegType.CN),

    P1BG_ctrl(Regs.DBG_ctrl, RegType.P1, "cc_dbg1_ctrl"),
    P1BG_data(Regs.DBG_data, RegType.P1, "cc_dbg1_data"),
    P2BG_ctrl(Regs.DBG_ctrl, RegType.P2, "cc_dbg2_ctrl"),
    P2BG_data(Regs.DBG_data, RegType.P2, "cc_dbg2_data");

    int address;
    Regs reg;
    RegType regType;

    String hdlName = null;

    AllRegAddr(Regs reg, RegType regType){
        this.address = address;
        this.reg = reg;
        this.regType = regType;
    }

    AllRegAddr(Regs reg, RegType regType, String hdlName){
        this.address = address;
        this.reg = reg;
        this.hdlName = hdlName;
        this.regType = regType;
    }

    public IReg getReg() {
        return reg;
    }

    public RegType getRegType() {
        return regType;
    }

    public static AllRegAddr getRegAddr(Regs reg, RegType regType){
        AllRegAddr allRegAddr = null;
        for(AllRegAddr regAddr : AllRegAddr.values()){
            if(regAddr.getRegType() == regType ) {
                if (regAddr.reg == reg) {
                    allRegAddr = regAddr;
                }
            }

        }
        return allRegAddr;
    }

    public static AllRegAddr getRegAddr(int address, RegType regType){
        AllRegAddr allRegAddr = null;
        for(AllRegAddr regAddr : AllRegAddr.values()){
            if(regAddr.getRegType() == regType ) {
                if (regAddr.getReg().getLocalAddr() == address) {
                    allRegAddr = regAddr;
                }
            }

        }
        return allRegAddr;
    }

    public static List<AllRegAddr> getAllRegsByReg(Regs reg){
        List<AllRegAddr> list = new ArrayList<>();
        for(AllRegAddr allRegAddr : AllRegAddr.values()){
            if(allRegAddr.getReg().equals(reg)){
                list.add(allRegAddr);
            }
        }
        return list;

    }



    public int getAddress() {

        switch (regType){
            case C1:return AddrDef.A_FIRM_CC1.addr * 32 + reg.localAddr;
            case C2:return AddrDef.A_FIRM_CC2.addr * 32 + reg.localAddr;
            case CR: return AddrDef.A_FIRM_CC.addr  * 32 + AddrDef.A_FIRM_CR.addr * 16 + reg.localAddr;
            case CN: return AddrDef.A_FIRM_CC.addr  * 32 + AddrDef.A_FIRM_CN.addr * 16 + reg.localAddr;
            case P1: return AddrDef.A_FIRM_CC.addr  * 32 + AddrDef.A_FIRM_CN.addr * 16 + 12 + reg.localAddr;
            case P2: return AddrDef.A_FIRM_CC.addr  * 32 + AddrDef.A_FIRM_CN.addr * 16 + 14 + reg.localAddr;
        }


        return address;
    }

    public static AllRegAddr getAllRegAddr(int address){
        for(AllRegAddr allRegAddr : AllRegAddr.values()){
            if(allRegAddr.getAddress() == address){
                return allRegAddr;
            }
        }

        return null;
    }

    public static  boolean isContain(String name){
        for(AllRegAddr allRegAddr : AllRegAddr.values()){
            if(allRegAddr.name().equals(name)){
                return true;
            }
        }
        return false;
    }

    public static List<AllRegAddr> getAllRegAddrsByType(RegType type){
        List<AllRegAddr> list = new ArrayList<>();
        for(AllRegAddr allRegAddr : AllRegAddr.values()){
            if(allRegAddr.getRegType().equals(type)){
                list.add(allRegAddr);
            }
        }
        return list;
    }


    public String getHdlName() {
        if(hdlName == null){
            return reg.getHdlName();
        } else {
            return hdlName;
        }
    }

    public String getDisplayName() {
        if (regType == RegType.C1) return "C1" + reg.getDisplayName();
        if (regType == RegType.C2) return "C2" + reg.getDisplayName();
        return reg.getDisplayName();
    }

}
