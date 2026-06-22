package ru.dcsoyuz.ad3s.model.fpga.parser;

/**
 * Created by Администратор on 21.04.2016.
 */
public enum CpuCommand {



    NOP ("00000000000", CommandGroup.TRANSFORM ),

    CLR ("00000000001", CommandGroup.TRANSFORM ),

    INC ("00000000010", CommandGroup.TRANSFORM ),

    DEC ("00000000011", CommandGroup.TRANSFORM ),

    ABS ("00000000100", CommandGroup.TRANSFORM ),

    SIN ("00000000101", CommandGroup.TRANSFORM ),

    COS ("00000000110", CommandGroup.TRANSFORM ),

    ACC ("00000000111", CommandGroup.TRANSFORM ),

    ACC2 ("00000001", CommandGroup.TRANSFORM ),

    ATAN ("00000010", CommandGroup.TRANSFORM ),

    ADD ("00000011", CommandGroup.TRANSFORM ),

    SUB ("00000100", CommandGroup.TRANSFORM ),

    ORL ("00000101", CommandGroup.TRANSFORM ),

    ANL ("00000110", CommandGroup.TRANSFORM ),

    MOVE ("00000111", CommandGroup.TRANSFORM ),

    LSL ("000010", CommandGroup.TRANSFORM ),

    ASR ("000011", CommandGroup.TRANSFORM ),

    STORE ("0001", CommandGroup.MEMORY ),

    STORE32 ("0010", CommandGroup.MEMORY ),

    LOAD ("0011", CommandGroup.MEMORY ),

    EQUAL ("010", CommandGroup.BRANCH ),

    MORE ("011", CommandGroup.BRANCH ),

    CLOAD ("1000", CommandGroup.MEMORY ),

    CSTORE ("1001", CommandGroup.MEMORY ),

    CSTORE32 ("1010", CommandGroup.MEMORY ),

    CLOAD32 ("1011", CommandGroup.MEMORY ),

    LOAD32 ("1100", CommandGroup.MEMORY ),

    CONST ("1101", CommandGroup.TRANSFORM ),

    DJNZ ("11100", CommandGroup.BRANCH ),

    CDJNZ ("111010", CommandGroup.BRANCH ),

    EQUAL0 ("111011", CommandGroup.BRANCH ),

    JUMP ("111100", CommandGroup.BRANCH ),

    MULTI ("11110100", CommandGroup.TRANSFORM ),

    MULTI_ACC ("11110101", CommandGroup.TRANSFORM ),

    MULTF ("11110110", CommandGroup.TRANSFORM ),

    MULTF_ACC ("11110111", CommandGroup.TRANSFORM ),







    MULTK24 ("11111011", CommandGroup.TRANSFORM ),

    MULTK14 ("11111100", CommandGroup.TRANSFORM ),

    MULTCUBE_ACC ("11111101", CommandGroup.TRANSFORM ),

    DEC2FLOAT ("11111110000", CommandGroup.TRANSFORM ),

    SET_A_HAND ("11111110001", CommandGroup.TRANSFORM ),

    HFIX ("11111110010", CommandGroup.TRANSFORM ),

    REZERVED2 ("11111110011", CommandGroup.TRANSFORM ),

    CFIX0 ("11111110100", CommandGroup.TRANSFORM ),

    CFIX1 ("11111110101", CommandGroup.TRANSFORM ),

    CFIX2 ("11111110110", CommandGroup.TRANSFORM ),

    CFIX3 ("11111110111", CommandGroup.TRANSFORM ),

    CFIX4 ("11111111000", CommandGroup.TRANSFORM ),

    CFIX7 ("11111111001", CommandGroup.TRANSFORM ),

    CLR_CH ("11111111010", CommandGroup.TRANSFORM ),

    CLOAD_CUBE ("11111111011", CommandGroup.TRANSFORM ),

    RJUMP ("11111111100", CommandGroup.BRANCH ),

    STOREPC ("11111111101", CommandGroup.TRANSFORM ),

    SETB_STP ("11111111110000", CommandGroup.TRANSFORM ),

    CLRB_STP ("11111111110001", CommandGroup.TRANSFORM ),

    SW_HTE1 ("11111111110010", CommandGroup.TRANSFORM ),

    SW_HTE2 ("11111111110011", CommandGroup.TRANSFORM ),











    WAIT_EXT_CPU ("11111111111000", CommandGroup.TRANSFORM ),

    PULSE_EXT_CPU ("11111111111001", CommandGroup.TRANSFORM ),

    WAIT_OWN_HAND ("11111111111010", CommandGroup.TRANSFORM ),

    WAIT_EXT_HAND ("11111111111011", CommandGroup.TRANSFORM ),

    LOADSTORE_ON ("11111111111100", CommandGroup.TRANSFORM ),

    LOADSTORE_OFF ("11111111111101", CommandGroup.TRANSFORM ),



    IDLE ("11111111111111", CommandGroup.TRANSFORM ),

    TXT ("", CommandGroup.TRANSFORM );


    private String opCode;
    private CommandGroup group;

    CpuCommand(String opCode, CommandGroup group){
        this.opCode = opCode;
        this.group = group;
    }

    public String getOpCode() {
        return opCode;
    }

    public void setOpCode(String opCode) {
        this.opCode = opCode;
    }

    public static CpuCommand getCommand(String command){
        for(CpuCommand item  : CpuCommand.values()){
            if(item.toString().equals(command)){
                return item;
            }
        }
        return null;
    }

    public CommandGroup getGroup() {
        return group;
    }
}
