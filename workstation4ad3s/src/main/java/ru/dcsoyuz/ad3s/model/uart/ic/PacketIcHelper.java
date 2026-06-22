package ru.dcsoyuz.ad3s.model.uart.ic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dcsoyuz.ad3s.model.uart.PacketHelper;

import java.util.ArrayList;
import java.util.List;

import static ru.dcsoyuz.ad3s.model.uart.ic.McuCommand.READ_RANDOM;

/**
 * Created by yuri.filatov on 08.08.2016.
 */
public class PacketIcHelper {

    private static final Logger logger = LoggerFactory.getLogger(PacketIcHelper.class);

     static int WORD_COUNT_ROM = 32;
     static final byte START_REQ_BYTE = (byte)0x77;

    public  static final byte START_RESP_BYTE = (byte)0x55;
    public static PacketToIc createPacketForReadFromIc(Integer address, Integer numValues){
        return new PacketToIc(McuCommand.READ, address,   numValues,null );
    }
    public static PacketToIc createPacketForRandomReadFromIc(List<Integer> listAddresses){
        return new PacketToIc(READ_RANDOM, listAddresses.size() != 0 ? listAddresses.get(0) : 0,  listAddresses.size(),listAddresses );
    }


    public static PacketToIc createPacketForWriteToIc(Integer address, List<Integer> data ){
        return new PacketToIc(McuCommand.WRITE, address,  data.size(), data );
    }

    public static byte[] getBytesFromPacketToIc(PacketToIc packet) {
        byte[] res = new byte[0];
        int numBytes = 0;
        res = PacketHelper.addDataToPacket(res, START_REQ_BYTE);
        res = PacketHelper.addDataToPacket(res, numBytes);
        res = PacketHelper.addDataToPacket(res, packet.getMcuCommand().getCommandByte());
        switch (packet.getMcuCommand()){
            case WRITE:
            case READ_HANDTAP:
            case READ:
            case SET_LED:
            case SET_STNDBY:
            case SET_DMA_QUAD:
            case SET_NRESET:
            case SET_SPI_SPEED:
            case SET_VC:
            case SET_SDI:
            case SET_VPP9V:
            case SET_DMA_SINGLE:
            case READ_CPU_REGS:
            case SET_MASTER_FPGA_MODE:
            case WRITE_RANDOM:
            case WRITE_BOTP:
            case READ_RANDOM:
            case START_RECORD:
            case WRITE_INIT_FLASH:
            case READ_INIT_FLASH:
            case RUN_INIT_FLASH:
            case SET_ELEC_OFFSET:
                res = PacketHelper.addDataToPacket(res, packet.getStartAddress());
                res = PacketHelper.addDataToPacket(res, packet.getNumWords());
                switch (packet.getMcuCommand()) {
                    case WRITE:
                    case READ_HANDTAP:
                    case READ_RANDOM:
                    case WRITE_BOTP:
                    case SET_DMA_SINGLE:
                    case SET_DMA_QUAD:
                    case WRITE_RANDOM:
                    case START_RECORD:
                    case WRITE_INIT_FLASH:
                    case SET_ELEC_OFFSET:
                        res = PacketHelper.addDataToPacket(res, packet.getData());
                        break;
                }
                break;
            case STOP_RECORD:
                res = PacketHelper.addDataToPacket(res, packet.getStartAddress());
                res = PacketHelper.addDataToPacket(res, packet.getNumWords());
                break;
        }
        numBytes = (byte)(res.length + 1);
        byte [] numBytes2byte = PacketHelper.i2b(numBytes);
        res[1] = numBytes2byte[0];
        res[2] = numBytes2byte[1];
        byte checksum = (byte)(PacketHelper.calcCheckSum(res));
        res = PacketHelper.addDataToPacket(res, checksum);
        return res;
    }

    public static List<Integer> getValuesFromIcPacket(byte [] resp) {
        int size = PacketHelper.getUnsignedWord16bitInt(resp[6],resp[7]);
        int available = (resp.length - 8) / 2;
        int count = Math.min(size, available);
        List<Integer> list = new ArrayList<>();
        for(int i=0; i< count; i++){
            int value = PacketHelper.getUnsignedWord16bitInt(resp[8+2*i],resp[8+2*i+1]);
            list.add(value);

        }
        return list;
    }

    public static List<Integer> getValuesFromIcPacket(List<Byte> resp) {
        int size = PacketHelper.getUnsignedWord16bitInt(resp.get(6),resp.get(7));
        List<Integer> list = new ArrayList<>();
        for(int i=0; i< size; i++){
            int value = PacketHelper.getUnsignedWord16bitInt(resp.get(8+2*i+1),resp.get(8+2*i));
            list.add(value);
        }
        return list;
    }


    public static  List<PacketToIc> createPacketsForNormValues(Integer address, List<String> hexCodes) {
        List<PacketToIc> res = new ArrayList<>();
        try {
            int i = 1;
            int k = 1;
            int iteration = 0;

            int ref = (128-address%128)%16;
            List<Integer> dataSinglePacket = new ArrayList<>();
            for (String hex : hexCodes) {
                dataSinglePacket.add(Integer.parseInt(hex, 16));
                if (k == WORD_COUNT_ROM | (iteration == 0  &&  k == ref)) {
                    List<Integer> dataCurPacket = new ArrayList<>();
                    dataCurPacket.addAll(dataSinglePacket);
                    res.add(PacketIcHelper.createPacketForWriteToIc(i  + address - ((iteration == 0  &&  k == ref)? ref: WORD_COUNT_ROM), dataCurPacket));
                    dataSinglePacket.clear();
                    k = 0;
                    iteration++;
                }
                i++;
                k++;
            }
            if (dataSinglePacket.size() != 0) {
                res.add(PacketIcHelper.createPacketForWriteToIc(i - 1 - dataSinglePacket.size() + address, dataSinglePacket));
            }
        } catch (NumberFormatException e) {
            logger.error("Error parsing hex values for norm values packets", e);
        }
        return res;


    }

}