package ru.dcsoyuz.ad3s.form.terminal;

import java.util.Arrays;
import java.util.List;

import static java.lang.Math.pow;

public enum DebugCpuRegs {
    pc ( 8, 0,  Arrays.asList(21), false),
    cur_com ( 13, 0,  Arrays.asList(20), false),
    ron0 (27, 0, Arrays.asList(0, 1), true),
    ron1 (27, 0, Arrays.asList(2, 3), true),
    ron2 (27, 0, Arrays.asList(4, 5), true),
    ron3 (27, 0, Arrays.asList(6, 7), true),
    ron4 (27, 0, Arrays.asList(8, 9), true),
    ron5 (27, 0, Arrays.asList(10, 11), true),
    ron6 (55, 0, Arrays.asList(12, 13, 16,17), true),
    ron7 (55, 0, Arrays.asList(14, 15, 18, 19), true),

    ch_rel ( 8, 0,  Arrays.asList(25),false),
    ch0 ( 8, 0,  Arrays.asList(22), false),
    ch1 ( 7, 0,  Arrays.asList(23), false),
    ch2 ( 9, 6,  Arrays.asList(24), false),
    ch3 ( 5, 0,  Arrays.asList(24), false),
    ch4 ( 13, 9,  Arrays.asList(22), false),
    ch7 ( 10, 9,  Arrays.asList(25), false),
    a_hand ( 10, 8,  Arrays.asList(23), false),
    stp (9, Arrays.asList(21), false),
    stop (10, Arrays.asList(21), false);
    List<Integer> addrs;
    int msb;
    int lsb;

    boolean signed;
    DebugCpuRegs(int msb, int lsb, List<Integer> list, boolean signed ) {
        this.msb = msb;
        this.lsb = lsb;
        this.addrs = list;
        this.signed = signed;
    }

    DebugCpuRegs(int index, List<Integer> list, boolean signed) {
        this.lsb = index;
        this.msb = index;
        this.addrs = list;
        this.signed = signed;
    }

    public List<Integer> getAddrs() {
        return addrs;
    }

    public int getMsb() {
        return msb;
    }

    public int getLsb() {
        return lsb;
    }

    public int getWidth(){
        return  (msb - lsb);
    }

    public int getMask(){
        int mask = 0;
        for (int i = lsb; i<=msb; i++){
            mask = mask | (int)pow(2,i);
        }
        return mask;
    }

    public boolean isSigned() {
        return signed;
    }
}
