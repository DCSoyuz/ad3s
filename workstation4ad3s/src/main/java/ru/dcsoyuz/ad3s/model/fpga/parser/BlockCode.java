package ru.dcsoyuz.ad3s.model.fpga.parser;


/**
 * Created by yuri.filatov on 21.06.2016.
 */
public enum BlockCode {
    BASE(0),
    CPU1(1),
    CPU2(2),
    ROM(3)
    ;

    Integer index;


    BlockCode(int index) {
        this.index = index;
    }

    public Integer getIndex() {
        return index;
    }

    public static BlockCode getName(String command){
        for(BlockCode item  : BlockCode.values()){
            if(item.toString().equals(command)){
                return item;
            }
        }
        return null;
    }



}
