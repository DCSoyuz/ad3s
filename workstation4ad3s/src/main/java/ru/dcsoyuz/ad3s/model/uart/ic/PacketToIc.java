package ru.dcsoyuz.ad3s.model.uart.ic;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuri.filatov on 08.08.2016.
 */
public class PacketToIc {

    private McuCommand mcuCommand;
    private Integer startAddress;
    private Integer numWords;
    private List<Integer> data;


    public PacketToIc(McuCommand command, Integer address, Integer numWords, List<Integer> data) {
        mcuCommand = command;
        this.numWords = numWords;
        this.data = data;
        this.startAddress = address & 0xFFFF;

    }

    public PacketToIc(McuCommand command, Integer address,  Integer data) {
        mcuCommand = command;
        this.numWords = 1;
        List<Integer> list = new ArrayList<>();
        list.add(data);
        this.data = list ;
        this.startAddress = address & 0xFFFF;

    }


    public Integer getNumWords() {
        return numWords;
    }

    public void setNumWords(Integer numWords) {
        this.numWords = numWords;
    }

    public Integer getStartAddress() {
        return startAddress;
    }

    public void setStartAddress(Integer startAddress) {
        this.startAddress = startAddress;
    }

    public List<Integer> getData() {
        return data;
    }

    public void setData(List<Integer> data) {
        this.data = data;
    }

    public McuCommand getMcuCommand() {
        return mcuCommand;
    }
}
