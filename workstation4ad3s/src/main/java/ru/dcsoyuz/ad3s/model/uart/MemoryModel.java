package ru.dcsoyuz.ad3s.model.uart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.form.editor.ILongProcessEventListener;
import ru.dcsoyuz.ad3s.form.editor.IMemoryEventListener;
import ru.dcsoyuz.ad3s.form.terminal.IGraphViewListener;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.editor.FileHelper;
import ru.dcsoyuz.ad3s.model.fpga.parser.ModeAttrFile;
import ru.dcsoyuz.ad3s.model.fpga.registers.AllRegAddr;
import ru.dcsoyuz.ad3s.model.fpga.registers.FactoryGate;
import ru.dcsoyuz.ad3s.model.fpga.registers.RegField;
import ru.dcsoyuz.ad3s.model.uart.ic.McuCommand;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketToIc;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketIcHelper;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Created by yuri.filatov on 11.08.2016.
 */
public class MemoryModel {

    private static final Logger logger = LoggerFactory.getLogger(MemoryModel.class);

    private Map<RunnerView, IMemoryEventListener> mapMemoryListeners = new HashMap<>();

    private Map<Integer, List<Integer>> reqValues;

    private Map<Integer, List<Integer>> pendingWriteAfterStop = null;
    private Runnable pendingResume = null;

    private Map<Integer, List<Integer>> initReqValues;


    private List<Integer> listRandomAddrReadValues;
    private List<Integer> respValues;
    private Integer readAddress;

    private IGraphViewListener graphViewListener;
    private Integer respAddress;
    private Integer numReadValues;

    private String fileNameRecordTxt;
    private Integer respNumValues;
    private String pathReadWriteFiles;
    private String nameFileTxtValues;
    private Integer countTxtValues;
    private Integer addressForTxtValues;

    private boolean ledValue;

    private boolean stndbyValue = false;

    private boolean vpp9vValue = false;

    private boolean nresetValue = true;

    private int spiSpeed = 10000;

    private boolean vcValue = true;

    private boolean sdiValue = true;

    private boolean dmaQuadValue = false;

    private boolean dmaSingleValue = false;

    private boolean encoderEnabled = false;

    private boolean isCyclic = false;
    private boolean isProcessing;

    private int actionHandValue = 5;

    private int numBaseMemoryValues = 96;
    private RunnerView curRunnerView;


    private Boolean signalStopProcessing = false;

    private volatile byte[] latestDmaQuadResponse = null;
    private volatile boolean dmaQuadResponseReady = false;
    private volatile SetDmaQuadValueAction activeDmaQuadAction = null;

    private int dbgCpuNum =  1;

    private int dbgCommand = 0;
    private boolean enaReadDbgCpuMem = false;
    private boolean enaReadDbgCpuBuf1 = false;
    private boolean enaReadDbgCpuBuf2 = false;

    private int dbgCpuStopAddr1 = 0;
    private boolean enaDbgCpuStopAddr1 = false;

    private int dbgHwAddr1 = 0;
    private boolean dbgHwEna1 = false;
    private int dbgHwAddr2 = 0;
    private boolean dbgHwEna2 = false;

    public int getDbgHwAddr1() { return dbgHwAddr1; }
    public boolean getDbgHwEna1() { return dbgHwEna1; }
    public int getDbgHwAddr2() { return dbgHwAddr2; }
    public boolean getDbgHwEna2() { return dbgHwEna2; }

    private int dbgCpuStopAddr2 = 0;
    private boolean enaDbgCpuStopAddr2 = false;
    List<ILongProcessEventListener> listLongProcessEventListeners = new ArrayList<>();

    /**
     * Starts an action thread with a safety net: if it throws an unhandled exception,
     * setProcessing(false) is called so the UI doesn't lock up.
     */
    private void startAction(Thread action) {
        action.setUncaughtExceptionHandler((t, e) -> {
            logger.warn("Error in " + t.getName() + ": " + e.getMessage());
            logger.error("Error", e);
            setProcessing(false);
        });
        action.start();
    }

    public void writeSelectedFilesToFlash() {
        setProcessing(true);
        startAction(new WriteSelectedFilesToFlashAction());
        return;
    }

    public void writeSelectedFilesToEspFlash() {
        setProcessing(true);
        startAction(new WriteSelectedFilesToEspFlashAction());
        return;
    }

    public void progBOTPtoROM() {
        setProcessing(true);
        startAction(new ProgBOTPtoICAction());
        return;
    }

    public void verifyBOTP() {
        setProcessing(true);
        startAction(new VerifyBOTPAction());
    }

    public void progUOTPtoROM() {
        setProcessing(true);
        startAction(new ProgUOTPtoICAction());


        return;
    }
    public void progFactoryToROM() {
        setProcessing(true);
        startAction(new ProgFactoryToICAction());


        return;
    }

    public void writeSelectedFilesToFlash(List<Integer> list) {
        listRandomAddrReadValues = list;
        setProcessing(true);
        startAction(new WriteSelectedFilesToFlashAction());

    }





    public void writeValuesFromTxtFile(){
        setProcessing(true);
        startAction(new WriteTxtValuesAction());
        return;
    }









    private class WriteSelectedFilesToFlashAction extends Thread {

        final int WORD_COUNT_ROM = 16;

        private List<PacketToIc> getPacketsToROM(Map<ModeAttrFile, List<String>> hexData) {
            List<PacketToIc> res = new ArrayList<>();
            for (Map.Entry<ModeAttrFile, List<String>> block : hexData.entrySet()) {
                List<PacketToIc> packetsForBlock = createPacketsForBlock(block.getKey(), block.getValue());
                res.addAll(packetsForBlock);
            }
            return res;
        }

        private List<PacketToIc> createPacketsForBlock(ModeAttrFile attr, List<String> hexCodes) {

            int realSize = attr.getHexSize();
            List<String> realList = new ArrayList<>();
            for(int i=0; i< realSize; i++){
                realList.add(hexCodes.get(i));
            }
           return  PacketIcHelper.createPacketsForNormValues(attr.getAddress(),realList);



        }

        @Override
        public void run() {
            List<File> selectedFiles = Model.getTreeViewModel().getSelectedFiles();
            logger.debug("\n\nStarting parsing selected files:");
            logger.debug("*****************************");

            for (int i = 0; i < selectedFiles.size(); i++) {
                File file = selectedFiles.get(i);
                ModeAttrFile attr = Model.getEditorModel().getFileAttr(file);
                if (attr == null) {
                    logger.debug("File " + file.getName() + " not found in mode.config, skipped!");
                    selectedFiles.remove(file);
                    i--;
                } else if(attr.isProgram()) {
                    logger.debug(file.getName());
                }else{
                    logger.debug("File "+ file.getName()+ " is not programming!");
                    selectedFiles.remove(file);
                    i--;
                }
            }
            logger.debug("*****************************");
            Thread parsing = Model.getParserModel().parseSelectedFiles(selectedFiles);
            Map<ModeAttrFile, List<String>> hexData = null;
            try {
                parsing.join();
                hexData = Model.getParserModel().getHexData();
            } catch (InterruptedException e) {
                logger.error("Error", e);
            }
            if (hexData != null) {




                List<PacketToIc> listForSending = getPacketsToROM(hexData);
                logger.debug("Created " + listForSending.size() + " packets for writing to flash");
                Map<Integer, List<Integer>> reqValues = new HashMap<>();
                for (PacketToIc curPacket : listForSending) {
                    reqValues.put(curPacket.getStartAddress(), curPacket.getData());
                }
                Model.getMemoryModel().setReqValues(reqValues);
                Model.getMemoryModel().writeValues();
            } else {
                logger.debug("Emptry data for write to flash");
            }

        }


    }

    /**
     * Запись начальных данных в ESP32 flash (NVS) для автозагрузки при старте.
     * Аналогично WriteSelectedFilesToFlashAction, но:
     * - Использует WRITE_INIT_FLASH вместо WRITE
     * - Ограничивает размер блоков: base_ram max 96, cpu1/cpu2 max 256
     */
    private class WriteSelectedFilesToEspFlashAction extends Thread {

        final int CHUNK_SIZE = 32; // слов на пакет (чтобы длина поместилась в 1 байт)

        @Override
        public void run() {
            List<File> selectedFiles = Model.getTreeViewModel().getSelectedFiles();
            logger.debug("\n\nWriting init data to ESP32 flash:");
            logger.debug("*****************************");

            for (int i = 0; i < selectedFiles.size(); i++) {
                File file = selectedFiles.get(i);
                ModeAttrFile attr = Model.getEditorModel().getFileAttr(file);
                if (attr == null) {
                    logger.debug("File " + file.getName() + " not found in mode.config, skipped!");
                    selectedFiles.remove(file);
                    i--;
                } else if(attr.isProgram()) {
                    logger.debug(file.getName());
                } else {
                    logger.debug("File " + file.getName() + " is not programming!");
                    selectedFiles.remove(file);
                    i--;
                }
            }
            logger.debug("*****************************");
            Thread parsing = Model.getParserModel().parseSelectedFiles(selectedFiles);
            Map<ModeAttrFile, List<String>> hexData = null;
            try {
                parsing.join();
                hexData = Model.getParserModel().getHexData();
            } catch (InterruptedException e) {
                logger.error("Error", e);
            }
            if (hexData != null) {
                int totalWords = 0;
                VerifyResult verifyResult = new VerifyResult();
                for (Map.Entry<ModeAttrFile, List<String>> block : hexData.entrySet()) {
                    ModeAttrFile attr = block.getKey();
                    List<String> hexCodes = block.getValue();

                    int address = attr.getAddress();
                    int maxSize = hexCodes.size();
                    if (address == 0) maxSize = Math.min(maxSize, 96);
                    else if (address == 512) maxSize = Math.min(maxSize, 256);
                    else if (address == 1024) maxSize = Math.min(maxSize, 256);

                    // Отправляем кусками по CHUNK_SIZE слов
                    for (int offset = 0; offset < maxSize; offset += CHUNK_SIZE) {
                        int chunkWords = Math.min(CHUNK_SIZE, maxSize - offset);
                        List<Integer> chunk = new ArrayList<>();
                        for (int j = 0; j < chunkWords; j++) {
                            chunk.add(Integer.parseInt(hexCodes.get(offset + j), 16));
                        }

                        PacketToIc pkg = new PacketToIc(McuCommand.WRITE_INIT_FLASH, address + offset, chunk.size(), chunk);
                        byte[] packet = PacketIcHelper.getBytesFromPacketToIc(pkg);

                        try {
                            Thread action = Model.getUartModel().doExchangePacket(packet);
                            action.join(1000);
                        } catch (InterruptedException e) {
                            logger.error("Error", e);
                            setProcessing(false);
                            return;
                        }

                        // Verify written data
                        verifyChunk(address + offset, chunk, McuCommand.READ_INIT_FLASH, verifyResult);

                        totalWords += chunkWords;
                    }
                    logger.debug("Written to ESP32 flash: addr=" + address + ", words=" + maxSize);
                }
                logger.debug("ESP32 flash write done. Total: " + totalWords + " words");
                verifyResult.printSummary();
            } else {
                logger.debug("Empty data for write to ESP32 flash");
            }
            setProcessing(false);
        }
    }

    public void setLedValue(boolean value){
        ledValue = value;
        SetLedValueAction action = new SetLedValueAction();
        startAction(action);
    }

    public void setStndbyValue(boolean value){
        stndbyValue = value;
        SetStndbyValueAction action = new SetStndbyValueAction();
        startAction(action);
    }

    public Thread setVpp9vValue(boolean value){
        vpp9vValue = value;
        SetPower9vValueAction action = new SetPower9vValueAction();
        startAction(action);
        return action;
    }
    public void setNresetValue(boolean value){
        nresetValue = value;
        SetNresetValueAction action = new SetNresetValueAction();
        startAction(action);
    }


    public void setSpeedSpi(int spiSpeed){
        this.spiSpeed = spiSpeed;
        SetSpiSpeedValueAction action = new SetSpiSpeedValueAction();
        startAction(action);
    }







    public void readCpuMemoryBufRegisters(int cpuNum, boolean memEna, boolean buf1Ena, boolean buf2Ena, int addr2, boolean addr2ena, Runnable onBreakpointsRead){
        dbgCpuNum = cpuNum;
        enaReadDbgCpuMem = memEna;
        enaReadDbgCpuBuf1 = buf1Ena;
        enaReadDbgCpuBuf2 = buf2Ena;
        dbgCpuStopAddr2 = addr2;
        enaDbgCpuStopAddr2 = addr2ena;
        new Thread(() -> {
            try {
                ReadCpuRegsAction readRegs = new ReadCpuRegsAction();
                readRegs.start();
                readRegs.join(5000);
                readBreakpointsFromHardware(cpuNum);
                if (onBreakpointsRead != null) {
                    SwingUtilities.invokeLater(onBreakpointsRead);
                }
            } catch (Exception e) {
                logger.error("Error", e);
            }
        }).start();
    }

    public void readCpuMemoryBufRegisters(int cpuNum, boolean memEna, boolean buf1Ena, boolean buf2Ena, int addr2, boolean addr2ena){
        dbgCpuNum = cpuNum;
        enaReadDbgCpuMem = memEna;
        enaReadDbgCpuBuf1 = buf1Ena;
        enaReadDbgCpuBuf2 = buf2Ena;
        dbgCpuStopAddr2 = addr2;
        enaDbgCpuStopAddr2 = addr2ena;
        ReadCpuRegsAction action = new ReadCpuRegsAction();
        startAction(action);

    }

    public void setVcValue(boolean value){
        vcValue = value;
        SetVcValueAction action = new SetVcValueAction();
        startAction(action);
    }

    public void setSdiValue(boolean value){
        sdiValue = value;
        SetSdiValueAction action = new SetSdiValueAction();
        startAction(action);
    }

    public void setMasterFpgaMode(boolean value){
        ledValue = value;
        SetMasterFpgaModeAction action = new SetMasterFpgaModeAction();
        startAction(action);
    }


    public byte[] consumeDmaQuadResponse() {
        dmaQuadResponseReady = false;
        return latestDmaQuadResponse;
    }
    public boolean isDmaQuadResponseReady() {
        return dmaQuadResponseReady;
    }

    public void setDmaQuadValue(boolean value){
        dmaQuadValue = value;
        if(value) {
            SetDmaQuadValueAction action = new SetDmaQuadValueAction();
            startAction(action);
        } else {
            // Stop: send disable command, no port reopening, no purgeRxBuffer
            signalStopProcessing = true;
            PacketToIc packet = new PacketToIc(McuCommand.SET_DMA_QUAD, 0, 0, new ArrayList<>());
            byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);
            Model.getUartModel().safeWriteBytes(bytes);
            // Give ESP32 time to process stop and drain stale queue items
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            // Reset semaphore and clear stale response state (no purgeRxBuffer — it can break USB CDC)
            Model.getUartModel().resetPermits();
            isCyclic = false;
            setProcessing(false);
        }
    }

    public void setDmaSingleValue(boolean value){
        dmaSingleValue = value;
        SetDmaSingleValueAction action = new SetDmaSingleValueAction();
        startAction(action);
    }

    public void setEncoderEnabled(boolean value){
        encoderEnabled = value;
        if (value) {
            SetEncoderOnAction action = new SetEncoderOnAction();
            startAction(action);
        } else {
            SetEncoderOffAction action = new SetEncoderOffAction();
            startAction(action);
        }
    }

    public void readEncoders(){
        ReadEncoderAction action = new ReadEncoderAction();
        startAction(action);

    }

    public boolean isEncoderEnabled() {
        return encoderEnabled;
    }

    // Encoder values storage
    private int enc1Value = 0;
    private int enc2Value = 0;

    public int getEnc1Value() {
        return enc1Value;
    }

    public int getEnc2Value() {
        return enc2Value;
    }

    public void updateEncoderValues(int enc1, int enc2) {
        this.enc1Value = enc1;
        this.enc2Value = enc2;
    }



    private class SetLedValueAction extends Thread {

        @Override
        public void run() {

            int address = ledValue ? 1 : 0;

            PacketToIc packet =  new PacketToIc(McuCommand.SET_LED, address,  0, null );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);

            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            setProcessing(false);
        }
    }


    public void verifySelectedFilesToFlash() {
        startAction(new VerifySelectedFilesAction());
    }


    private class VerifySelectedFilesAction extends Thread {

        final int CHUNK_SIZE = 32;

        @Override
        public void run() {
            setProcessing(true);
            List<File> selectedFiles = Model.getTreeViewModel().getSelectedFiles();
            logger.debug("\n\nVerify: starting...");
            logger.debug("*****************************");

            for (int i = 0; i < selectedFiles.size(); i++) {
                File file = selectedFiles.get(i);
                if (!Model.getEditorModel().getFileAttr(file).isProgram()) {
                    selectedFiles.remove(file);
                    i--;
                }
            }

            Thread parsing = Model.getParserModel().parseSelectedFiles(selectedFiles);
            Map<ModeAttrFile, List<String>> hexData = null;
            try {
                parsing.join();
                hexData = Model.getParserModel().getHexData();
            } catch (InterruptedException e) {
                logger.error("Error", e);
            }

            if (hexData == null) {
                logger.debug("Verify: no data to verify");
                setProcessing(false);
                return;
            }

            int totalChecked = 0;
            int totalMismatch = 0;

            for (Map.Entry<ModeAttrFile, List<String>> block : hexData.entrySet()) {
                ModeAttrFile attr = block.getKey();
                List<String> hexCodes = block.getValue();
                int address = attr.getAddress();
                int maxSize = hexCodes.size();

                logger.debug("Verify: block addr=" + address + ", words=" + maxSize);

                // Читаем из IC кусками по CHUNK_SIZE
                for (int offset = 0; offset < maxSize; offset += CHUNK_SIZE) {
                    int chunkWords = Math.min(CHUNK_SIZE, maxSize - offset);

                    // Формируем ожидаемые значения из файла
                    List<Integer> expected = new ArrayList<>();
                    for (int j = 0; j < chunkWords; j++) {
                        expected.add(Integer.parseInt(hexCodes.get(offset + j), 16));
                    }

                    // Читаем фактические значения из IC
                    PacketToIc pkg = new PacketToIc(McuCommand.READ, address + offset, chunkWords, null);
                    byte[] packet = PacketIcHelper.getBytesFromPacketToIc(pkg);
                    byte[] response = null;
                    try {
                        Thread action = Model.getUartModel().doExchangePacket(packet);
                        action.join(1000);
                        response = Model.getUartModel().getResponse();
                    } catch (InterruptedException e) {
                        logger.error("Error", e);
                        break;
                    }

                    if (response != null && response.length > 9) {
                        List<Integer> actual = PacketIcHelper.getValuesFromIcPacket(response);
                        for (int j = 0; j < chunkWords && j < actual.size(); j++) {
                            int addr = address + offset + j;
                            int expVal = expected.get(j);
                            int actVal = actual.get(j);
                            totalChecked++;
                            if (expVal != actVal) {
                                totalMismatch++;
                                System.out.printf("MISMATCH addr=%d: expected=0x%04X, read=0x%04X%n", addr, expVal, actVal);
                            }
                        }
                    } else {
                        logger.debug("Verify: no response for addr=" + (address + offset));
                    }
                }
            }

            logger.debug("*****************************");
            System.out.printf("Verify: done. Checked=%d, Mismatches=%d%n", totalChecked, totalMismatch);
            if (totalMismatch == 0) {
                logger.debug("Verify: OK - all values match!");
            }
            setProcessing(false);
        }
    }


        private class SetStndbyValueAction extends Thread {

        @Override
        public void run() {

            int address = stndbyValue ? 1 : 0;

            PacketToIc packet =  new PacketToIc(McuCommand.SET_STNDBY, address,  0, null );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);

            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            setProcessing(false);
        }



    }

    private class SetPower9vValueAction extends Thread {

        @Override
        public void run() {

            int address = vpp9vValue ? 1 : 0;

            PacketToIc packet = new PacketToIc(McuCommand.SET_VPP9V, address, 0, null);
            byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);

            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            setProcessing(false);
        }
    }
    private class SetNresetValueAction extends Thread {

        @Override
        public void run() {

            int address = nresetValue ? 1 : 0;

            PacketToIc packet =  new PacketToIc(McuCommand.SET_NRESET, address,  0, null );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);

            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            setProcessing(false);
        }
    }

    private class SetSpiSpeedValueAction extends Thread {

        @Override
        public void run() {

            PacketToIc packet =  new PacketToIc(McuCommand.SET_SPI_SPEED, spiSpeed,  0, null );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);

            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            setProcessing(false);
        }
    }

    private class SetVcValueAction extends Thread {

        @Override
        public void run() {

            int address = vcValue ? 1 : 0;

            PacketToIc packet =  new PacketToIc(McuCommand.SET_VC, address,  0, null );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);

            Model.getUartModel().doWriteBytes(bytes);

            setProcessing(false);
        }
    }

    private class SetSdiValueAction extends Thread {

        @Override
        public void run() {

            int address = sdiValue ? 1 : 0;

            PacketToIc packet =  new PacketToIc(McuCommand.SET_SDI, address,  0, null );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);
            Model.getUartModel().doWriteBytes(bytes);
            setProcessing(false);
        }
    }

    private class SetEncoderOnAction extends Thread {
        @Override
        public void run() {
            PacketToIc packet = new PacketToIc(McuCommand.ENC_ON, 0, 0, null);
            byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);
            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            logger.debug("Encoder ON command sent");
            setProcessing(false);
        }
    }


    private class ReadEncoderAction extends Thread {
        @Override
        public void run() {
            PacketToIc packet = new PacketToIc(McuCommand.READ_ENCODERS, 0, 0, null);
            byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);
            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            setProcessing(false);
        }
    }

    private class SetEncoderOffAction extends Thread {
        @Override
        public void run() {
            PacketToIc packet = new PacketToIc(McuCommand.ENC_OFF, 0, 0, null);
            byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);
            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            logger.debug("Encoder OFF command sent");
            setProcessing(false);
        }
    }

    private class ReadEncodersAction extends Thread {
        @Override
        public void run() {
            PacketToIc packet = new PacketToIc(McuCommand.READ_ENCODERS, 0, 0, null);
            byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);
            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            logger.debug("Encoder READ command sent");
            setProcessing(false);
        }
    }


    private class SetMasterFpgaModeAction extends Thread {

        @Override
        public void run() {

            int address = ledValue ? 1 : 0;

            PacketToIc packet =  new PacketToIc(McuCommand.SET_MASTER_FPGA_MODE, address,  0, null );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);

            try {
                Thread action = Model.getUartModel().doExchangePacket(bytes);
                action.join(1000);
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            setProcessing(false);
        }


    }


    private class ProgBOTPtoICAction extends Thread {



        private List<PacketToIc> createPackets(ModeAttrFile attr, List<String> hexCodes) {

            int     cur_address = 0;



            List <PacketToIc> listPackets = new ArrayList<>();


            for(String hex : hexCodes){
                List<Integer> dataPacket = new ArrayList<>();
                dataPacket.add(AllRegAddr.BOTP_addr.getAddress());
                dataPacket.add(cur_address);
                dataPacket.add(AllRegAddr.BOTP_data.getAddress());
                dataPacket.add(Integer.parseInt(hex, 16));
                dataPacket.add(AllRegAddr.BOTP_ctrl.getAddress());
                PacketToIc packet =  new PacketToIc(McuCommand.WRITE_BOTP, 0,  dataPacket.size(), dataPacket );
                listPackets.add(packet);
                cur_address = cur_address + 1;
            }


            return listPackets;
        }




        @Override
        public void run() {
            ModeAttrFile modeAttrFile = Model.getEditorModel().getFileAttrByName("rom_BOTP.hex");
            List<File> parseFiles = new ArrayList<>();
            parseFiles.add( Model.getEditorModel().getFileByName("rom_BOTP.hex"));
            Thread parsing = Model.getParserModel().parseSelectedFiles(parseFiles);
            List<String> hexData = null;
            try {
                parsing.join();
                hexData =  Model.getParserModel().getHexsByAttr(modeAttrFile);
            } catch (InterruptedException e) {
                logger.error("Error", e);
            }
            if (hexData != null) {
                List<PacketToIc> listForSending = createPackets(modeAttrFile, hexData);
                logger.debug("Created " + listForSending.size() + " packets for writing to flash");
                int     PGM_ctrl    = 0x08;
                List<Integer> dataPacketPrev = new ArrayList<>();
                dataPacketPrev.add(AllRegAddr.BOTP_ctrl.getAddress());
                dataPacketPrev.add(PGM_ctrl);
                PacketToIc packetPrev =  new PacketToIc(McuCommand.WRITE_RANDOM, 0,  dataPacketPrev.size(), dataPacketPrev );
                try {
                    Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetPrev));
                    action.join();
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    sendPgmDisable();
                    setProcessing(false);
                    return;
                }

                if (signalStopProcessing) {
                    sendPgmDisable();
                    signalStopProcessing = false;
                    setProcessing(false);
                    return;
                }

                int writtenCount = 0;
                int totalCount = listForSending.size();
                for (int idx = 0; idx < totalCount; idx++) {
                    if (signalStopProcessing) {
                        logger.debug("BOTP programming stopped by user after " + writtenCount + " words");
                        break;
                    }
                    PacketToIc packet = listForSending.get(idx);
                    int expectedValue = Integer.parseInt(hexData.get(idx), 16);
                    byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);

                    try {
                        Thread action = Model.getUartModel().doExchangePacket(bytes);
                        action.join();
                        writtenCount++;
                    } catch (InterruptedException e) {
                        logger.error("Error", e);
                        sendPgmDisable();
                        signalStopProcessing = false;
                        setProcessing(false);
                        return;
                    }

                    // --- Verify: clear PGM, set REN ---
                    try {
                        PacketToIc renPacket = new PacketToIc(McuCommand.WRITE, AllRegAddr.BOTP_ctrl.getAddress(), 0x04);
                        Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(renPacket));
                        action.join();

                        // Read BOTP_out (address already set by WRITE_BOTP)
                        PacketToIc readPacket = new PacketToIc(McuCommand.READ, AllRegAddr.BOTP_out.getAddress(), null);
                        action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(readPacket));
                        action.join();

                        byte[] response = Model.getUartModel().getResponse();
                        if (response != null && response.length >= 11) {
                            int readValue = PacketHelper.getUnsignedWord16bitInt(response[8], response[9]);
                            if (readValue != expectedValue) {
                                logger.debug(String.format(
                                    "VERIFY FAILED at addr=%d: read=0x%04X expected=0x%04X. Programming stopped.",
                                    idx, readValue, expectedValue));
                                sendPgmDisable();
                                setProcessing(false);
                                return;
                            }
                        } else {
                            logger.debug("VERIFY FAILED at addr=" + idx + ": no valid response. Programming stopped.");
                            sendPgmDisable();
                            setProcessing(false);
                            return;
                        }

                        // Restore PGM for next word (skip on last word)
                        if (idx < totalCount - 1) {
                            PacketToIc pgmPacket = new PacketToIc(McuCommand.WRITE, AllRegAddr.BOTP_ctrl.getAddress(), 0x08);
                            action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(pgmPacket));
                            action.join();
                        }
                    } catch (InterruptedException e) {
                        logger.error("Error", e);
                        sendPgmDisable();
                        signalStopProcessing = false;
                        setProcessing(false);
                        return;
                    }
                }

                signalStopProcessing = false;
                int     noPGM_ctrl  = 0x00;
                List<Integer> dataPacketAfter = new ArrayList<>();
                dataPacketAfter.add(AllRegAddr.BOTP_ctrl.getAddress());
                dataPacketAfter.add(noPGM_ctrl);
                PacketToIc packetAfter =  new PacketToIc(McuCommand.WRITE_RANDOM, 0,  dataPacketAfter.size(), dataPacketAfter );
                try {
                    Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetAfter));
                    action.join();
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                }



            } else {
                logger.debug("Emptry data for write to flash");
            }

            setProcessing(false);
        }

        private void sendPgmDisable() {
            try {
                List<Integer> data = new ArrayList<>();
                data.add(AllRegAddr.BOTP_ctrl.getAddress());
                data.add(0x00);
                PacketToIc disablePacket = new PacketToIc(McuCommand.WRITE_RANDOM, 0, data.size(), data);
                Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(disablePacket));
                action.join();
            } catch (Exception e) {
                logger.error("Error", e);
            }
        }


    }

    private class VerifyBOTPAction extends Thread {
        @Override
        public void run() {
            ModeAttrFile modeAttrFile = Model.getEditorModel().getFileAttrByName("rom_BOTP.hex");
            List<File> parseFiles = new ArrayList<>();
            parseFiles.add(Model.getEditorModel().getFileByName("rom_BOTP.hex"));
            Thread parsing = Model.getParserModel().parseSelectedFiles(parseFiles);
            List<String> hexData = null;
            try {
                parsing.join();
                hexData = Model.getParserModel().getHexsByAttr(modeAttrFile);
            } catch (InterruptedException e) {
                logger.error("Error", e);
            }
            if (hexData == null) {
                logger.debug("No BOTP hex data found");
                setProcessing(false);
                return;
            }

            // Enable BOTP read mode: REN=1 (bit 2)
            int REN_ctrl = 0x04;
            PacketToIc packetEnable = new PacketToIc(McuCommand.WRITE, AllRegAddr.BOTP_ctrl.getAddress(), REN_ctrl);
            try {
                Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetEnable));
                action.join();
            } catch (InterruptedException e) {
                logger.error("Error", e);
                setProcessing(false);
                return;
            }

            int totalCount = hexData.size();
            int errorCount = 0;
            logger.debug("=== Verify BOTP: " + totalCount + " words ===");

            for (int i = 0; i < totalCount; i++) {
                if (signalStopProcessing) {
                    logger.debug("Verify BOTP stopped by user at word " + i);
                    break;
                }

                // Write address to BOTP_addr
                try {
                    PacketToIc packetAddr = new PacketToIc(McuCommand.WRITE, AllRegAddr.BOTP_addr.getAddress(), i);
                    Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetAddr));
                    action.join();

                    // Read result from BOTP_out
                    PacketToIc packetRead = new PacketToIc(McuCommand.READ, AllRegAddr.BOTP_out.getAddress(), null);
                    action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetRead));
                    action.join();

                    byte[] response = Model.getUartModel().getResponse();
                    int readValue = PacketHelper.getUnsignedWord16bitInt(response[8], response[9]);
                    int expectedValue = Integer.parseInt(hexData.get(i), 16);

                    if (readValue != expectedValue) {
                        logger.debug(String.format("MISMATCH addr=%d: read=0x%04X expected=0x%04X", i, readValue, expectedValue));
                        errorCount++;
                    }
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    break;
                }
            }

            // Disable BOTP read mode
            signalStopProcessing = false;
            try {
                PacketToIc packetDisable = new PacketToIc(McuCommand.WRITE, AllRegAddr.BOTP_ctrl.getAddress(), 0x00);
                Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetDisable));
                action.join();
            } catch (InterruptedException e) {
                logger.error("Error", e);
            }

            if (errorCount == 0) {
                logger.debug("=== Verify BOTP PASSED: all " + totalCount + " words OK ===");
            } else {
                logger.debug("=== Verify BOTP FAILED: " + errorCount + "/" + totalCount + " words mismatch ===");
            }
            setProcessing(false);
        }
    }


    private class ProgUOTPtoICAction extends Thread {
        final int WORD_COUNT_ROM = 16;

        @Override
        public void run() {
            setProcessing(true);
            reqValues = new HashMap<>(initReqValues);
            reqValues.get(AllRegAddr.PLL_config.getAddress()).set( 2, reqValues.get(AllRegAddr.PLL_config.getAddress()).get(2) |  0x0002 );
            Thread action = new WriteValuesAction();
            startAction(action);
            try {
                action.join(1000);
                Thread actionVpp1 = setVpp9vValue(true);
                actionVpp1.join(1000);
                sleep(1);
                Thread actionVpp2 = setVpp9vValue(false);
                actionVpp2.join(1000);
                reqValues.get(AllRegAddr.PLL_config.getAddress()).set( 2, reqValues.get(AllRegAddr.PLL_config.getAddress()).get(2) & 0xFFFD );
                Thread action3 = new WriteValuesAction();
                startAction(action3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            setProcessing(false);
        }


    }


    private class ProgFactoryToICAction extends Thread {
        final int WORD_COUNT_ROM = 16;

        @Override
        public void run() {
            setProcessing(true);
            reqValues = new HashMap<>(initReqValues);
            int f1Addr = FactoryGate.regF1Address();
            if (f1Addr >= 0 && reqValues.containsKey(f1Addr)) {
                reqValues.get(f1Addr).set( 2, reqValues.get(f1Addr).get(2) |  0x0002 );
            }
            Thread action = new WriteValuesAction();
            startAction(action);
            try {
                action.join(1000);
                Thread actionVpp1 = setVpp9vValue(true);
                actionVpp1.join(1000);
                sleep(200);
                Thread actionVpp2 = setVpp9vValue(false);
                actionVpp2.join(1000);
                if (f1Addr >= 0 && reqValues.containsKey(f1Addr)) {
                    reqValues.get(f1Addr).set( 2, reqValues.get(f1Addr).get(2) & 0xFFFD );
                }
                Thread action3 = new WriteValuesAction();
                startAction(action3);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            setProcessing(false);
        }


    }




    private class WriteTxtValuesAction extends Thread {

        @Override
        public void run() {

            List<Integer>  values = FileHelper.getValuesFromFile(nameFileTxtValues, countTxtValues, pathReadWriteFiles );
            logger.debug("Is readed " + values.size() +" values");
            List<String> hexValues = PacketHelper.createHex(values, 0x10000);
            if (values.size() != 0) {
                List<PacketToIc> listForSending = PacketIcHelper.createPacketsForNormValues(addressForTxtValues,hexValues);
                logger.debug("Created " + listForSending.size() + " packets for writing to flash");
                for (PacketToIc curPacket : listForSending) {
                    try {
                        byte[] romData = PacketIcHelper.getBytesFromPacketToIc(curPacket);
                        byte[] cebData = PacketHelper.createCebPacket(romData);
                      //  byte[] boxData = PacketHelper.createBoxPacketToCeb(cebData);

                        UartModel.EchangePacketAction t = Model.getUartModel().doExchangePacket(cebData);
                        t.join(3000);
                        if (t.isAlive()) {
                            t.interrupt();
                            t.join();
                            logger.debug("Error for sending packet. This packet will be ignored! ");
                            return;
                        }
                    } catch (InterruptedException e) {
                        logger.error("Error", e);
                    }
                }
            } else {
                logger.debug("Emptry data for write to flash");
            }

            setProcessing(false);
        }
    }



    public Thread readBaseRamValues(RunnerView runnerView) {
        setProcessing(true);
        readAddress = 0;
        numReadValues = numBaseMemoryValues;
        ReadValuesAction action = new ReadValuesAction();
        curRunnerView = runnerView;
        startAction(action);
        return action;
    }




    public void cyclicReadBaseRamValues(RunnerView runnerView) {
        isCyclic = true;
        pendingResume = () -> cyclicReadBaseRamValues(runnerView);
        setProcessing(true);
        curRunnerView = runnerView;
        CyclicReadValuesAction action = new CyclicReadValuesAction();
        startAction(action);
    }


    public void runCyclicReadGraphValues() {
        isCyclic = true;
        pendingResume = () -> runCyclicReadGraphValues();
        setProcessing(true);
        CyclicRandomReadValuesAction action = new CyclicRandomReadValuesAction();
        startAction(action);
    }

    public void runHandTapReadValues() {
        isCyclic = true;
        pendingResume = () -> runHandTapReadValues();
        setProcessing(true);
        CyclicHandTapReadValuesAction action = new CyclicHandTapReadValuesAction();
        startAction(action);
    }

    public void stopLongProcess() {
        signalStopProcessing = true;
    }
    private class CyclicReadValuesAction extends Thread {
        @Override
        public void run() {
            isCyclic = true;
            readAddress = 0;
            numReadValues = numBaseMemoryValues;
            while( isProcessing && !signalStopProcessing){
                ReadValuesAction action = new ReadValuesAction();

                startAction(action);
                try {
                    action.join(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            isCyclic = false;
            signalStopProcessing = false;
            setProcessing(false);
        }
    }


    public void setIGraphViewListener(IGraphViewListener listener){
        this.graphViewListener = listener;
    }

    private class CyclicRandomReadValuesAction extends Thread {
        @Override
        public void run() {
            isCyclic = true;
            curRunnerView = RunnerView.GRAPH_VIEW;
            listRandomAddrReadValues = graphViewListener.getListAddresses();

            while(isProcessing && !signalStopProcessing){
                ReadRandomAddressValuesAction action = new ReadRandomAddressValuesAction();
                startAction(action);
                try {
                    action.join(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            signalStopProcessing = false;
            isCyclic = false;
            setProcessing(false);
        }
    }






    private class CyclicHandTapReadValuesAction extends Thread {
        @Override
        public void run() {

            ReadValuesAction actionRead = new ReadValuesAction();
            curRunnerView = null;
            readAddress = 760;
            numReadValues = 1;
            startAction(actionRead);
            try {
                actionRead.join(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            if(respValues != null && respValues.size() != 0){
                actionHandValue = respValues.get(0);
                setProcessing(true);
            }else {
                logger.debug("Aborted!");
                setProcessing(false);
                return;
            }

            isCyclic = true;
            curRunnerView = RunnerView.HANDTAP_VIEW;
            listRandomAddrReadValues = graphViewListener.getListAddresses();

            while(isProcessing && !signalStopProcessing){
                ReadTapValuesAction action = new ReadTapValuesAction();
                startAction(action);
                try {
                    action.join(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //break;
            }
            signalStopProcessing = false;
            isCyclic = false;
            setProcessing(false);
        }
    }



    private class SetDmaQuadValueAction extends Thread {

        @Override
        public void run() {

            setProcessing(true);
            activeDmaQuadAction = this;
            signalStopProcessing = false;

            // Get Addr1 and Addr2 from the listener
            List<Integer> listAddresses = graphViewListener.getListAddresses();
            int combinedAddr = listAddresses.get(0);
            int addr1 = combinedAddr & 0xFF;
            int addr2 = (combinedAddr >> 8) & 0xFF;
            List<Integer> addrData = new ArrayList<>();
            addrData.add(addr1);
            addrData.add(addr2);
            PacketToIc packet = new PacketToIc(McuCommand.SET_DMA_QUAD, 1, addrData.size(), addrData);
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);
            Model.getUartModel().safeWriteBytes(bytes);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                setProcessing(false);
                return;
            }

            isCyclic = true;
            curRunnerView = RunnerView.DMATAP_VIEW;

            while(isProcessing && !signalStopProcessing && activeDmaQuadAction == this){
                PacketToIc readPacket = new PacketToIc(McuCommand.SET_DMA_QUAD, 2, 0, new ArrayList<>());
                byte[] readBytes = PacketIcHelper.getBytesFromPacketToIc(readPacket);
                UartModel.SafeWaitResponseAction waitAction = Model.getUartModel().doSafeWaitResponse();
                Model.getUartModel().safeWriteBytes(readBytes);
                try {
                    waitAction.join(5000);
                } catch (InterruptedException e) {
                    break;
                }
                byte[] response = Model.getUartModel().getResponse();
                if(response != null && response.length != 0) {
                    latestDmaQuadResponse = response;
                    dmaQuadResponseReady = true;
                }
            }
            signalStopProcessing = false;
        }
    }

    private class SetDmaSingleValueAction extends Thread {

        @Override
        public void run() {

            setProcessing(true);
            int address = dmaSingleValue ? 1 : 0;
            listRandomAddrReadValues = graphViewListener.getListAddresses();
            listRandomAddrReadValues.add(listRandomAddrReadValues.get(listRandomAddrReadValues.size()-1));
            listRandomAddrReadValues.add(listRandomAddrReadValues.get(listRandomAddrReadValues.size()-1));
            List<Integer>listReadAddresses = new ArrayList<>();
            for(int a : listRandomAddrReadValues){
                listReadAddresses.add(PacketHelper.getReadSdiWord(a));
            }
            PacketToIc packet =  new PacketToIc(McuCommand.SET_DMA_SINGLE, address,  listReadAddresses.size(), listReadAddresses );
            byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);
            if(dmaSingleValue) {
                String path = createPath();
                Model.getUartModel().enableRecording(path);
                Model.getUartModel().doWriteBytes(bytes);
            }else{
                Model.getUartModel().disableRecording();
                Model.getUartModel().doWriteBytes(bytes);
                signalStopProcessing = false;
                isCyclic = false;
                setProcessing(false);
                return;
            }
            isCyclic = true;
            curRunnerView = RunnerView.DMASINGLE_VIEW;

        }
    }


    public String createPath() {

        String resultPath = "";
        String path =  WorkstationConfig.getProperty(ConfProp.FILE_PATH_RECORD_TXT);
        if (fileNameRecordTxt.equals("")) {
            logger.debug("NO filename, create data_... .txt");
            fileNameRecordTxt = "data.txt";
        } else {
            CreateFolder(path);
//                CreateFolder("D://UDM//" + numGITextField.getText());
            resultPath = path + File.separator + fileNameRecordTxt + ".txt";
            File dir = new File(resultPath);
           /* if (dir.isFile()){
                logger.debug("File true!");
            }*/
            for (int i = 1; i > 0; i++) {
                if (!dir.isFile()) {
                    break;
                }
                resultPath = path + File.separator + fileNameRecordTxt + "(" + i + ")" + ".txt";
                dir = new File(resultPath);
            }
        }
        return resultPath;
    }
    private void CreateFolder(String Patch) {
        File theDir = new File(Patch);
//               if the directory does not exist, create it
        if (!theDir.exists()) {
            logger.debug("creating directory: " + theDir.getName());
            boolean result = false;
            try {
                theDir.mkdir();
                result = true;
            } catch (SecurityException se) {
                //handle it
            }
            if (result) {
                logger.debug("Folder created");
            }
        }
    }


    public Thread readValues() {
        setProcessing(true);
        ReadValuesAction action = new ReadValuesAction();
        curRunnerView = RunnerView.TABLE_8CELL;
        startAction(action);
        return action;
    }

    public Thread readOTPvalues() {
        setProcessing(true);
        ReadOTPValuesAction action = new ReadOTPValuesAction();
        curRunnerView = RunnerView.TABLE_8CELL;
        startAction(action);
        return action;
    }




    public void writeValues() {
        setProcessing(true);
        startAction(new WriteValuesAction());
    }

    public void writeThenResumeCyclic(Map<Integer, List<Integer>> values) {
        if (isCyclic && isProcessing) {
            pendingWriteAfterStop = values;
            signalStopProcessing = true;
        }
    }

    public void findICAddress() {
        new Thread(() -> {
            final int BUS_ADDR = AllRegAddr.BUS_addr.getAddress();
            final int READ_ADDR = BUS_ADDR - 1;
            logger.debug("Find IC: scanning addresses 1..255...");
            setProcessing(true);
            try {
                for (int i = 1; i <= 255; i++) {
                    // Write value i to BUS_addr
                    PacketToIc writePkg = PacketIcHelper.createPacketForWriteToIc(
                            BUS_ADDR, Collections.singletonList(i));
                    byte[] writePacket = PacketIcHelper.getBytesFromPacketToIc(writePkg);
                    Thread writeAction = Model.getUartModel().doExchangePacket(writePacket);
                    writeAction.join(500);
                    Model.getUartModel().resetPermits();

                    // Read pair (even-odd) since BUS_addr is odd
                    PacketToIc readPkg = new PacketToIc(McuCommand.READ, READ_ADDR, 2, null);
                    byte[] readPacket = PacketIcHelper.getBytesFromPacketToIc(readPkg);
                    Thread readAction = Model.getUartModel().doExchangePacket(readPacket);
                    readAction.join(500);
                    byte[] response = Model.getUartModel().getResponse();
                    Model.getUartModel().resetPermits();

                    if (response != null && response.length > 9) {
                        List<Integer> values = PacketIcHelper.getValuesFromIcPacket(response);
                        if (values != null && values.size() >= 2) {
                            int regValue = values.get(1);
                            if (regValue == i) {
                                logger.debug("current address IC is " + i);
                                return;
                            }
                        }
                    }
                    Thread.sleep(10);
                }
                logger.debug("Find IC: no IC address found in range 1..255");
            } catch (InterruptedException e) {
                logger.debug("Find IC: interrupted");
            } finally {
                Model.getUartModel().resetPermits();
                setProcessing(false);
            }
        }).start();
    }

    public void resetAction(RegField regField1, RegField regField2, int viewValue) {
        setProcessing(true);
        List<Integer> writeValues = new ArrayList<>(1);
        int newValue = viewValue & (~regField1.getMask()) & (~regField2.getMask());
        writeValues.add(newValue);
        setReqValues(AllRegAddr.Mode_config.getAddress(), writeValues );
        Thread t1 = new WriteValuesAction();
        startAction(t1);
        try {
            t1.join(2000);
            writeValues.clear();
            writeValues.add(viewValue);
            setReqValues(AllRegAddr.Mode_config.getAddress(), writeValues );
            Thread t2 = new WriteValuesAction();
            startAction(t2);
            t2.join(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void resetCpus() {
        setProcessing(true);
        try {
            PacketToIc readPkg = new PacketToIc(McuCommand.READ, AllRegAddr.Mode_config.getAddress(), 1, null);
            byte[] readPacket = PacketIcHelper.getBytesFromPacketToIc(readPkg);
            Thread readAction = Model.getUartModel().doExchangePacket(readPacket);
            readAction.join(2000);
            byte[] readResponse = Model.getUartModel().getResponse();
            if (readResponse == null) {
                setProcessing(false);
                return;
            }
            List<Integer> readValues = PacketIcHelper.getValuesFromIcPacket(readResponse);
            if (readValues.isEmpty()) {
                setProcessing(false);
                return;
            }
            int viewValue = readValues.get(0);
            resetAction(RegField.CPU1_en, RegField.CPU2_en, viewValue);
        } catch (InterruptedException e) {
            setProcessing(false);
        }
    }

    private void readBreakpointsFromHardware(int cpuNum) throws Exception {
        int bgCtrlAddr = (cpuNum == 1) ? AllRegAddr.P1BG_ctrl.getAddress() : AllRegAddr.P2BG_ctrl.getAddress();
        int bgDataAddr = (cpuNum == 1) ? AllRegAddr.P1BG_data.getAddress() : AllRegAddr.P2BG_data.getAddress();
        int hwCtrl = readRegister(bgCtrlAddr);
        int hwData = readRegister(bgDataAddr);
        dbgHwAddr1 = hwCtrl & 0x1FF;
        dbgHwEna1 = (hwCtrl & (1 << 9)) != 0;
        dbgHwAddr2 = hwData & 0x1FF;
        dbgHwEna2 = (hwData & (1 << 9)) != 0;
    }

    public void setCpuEnabled(int cpuNum, boolean enabled) {
        RegField cpuField = (cpuNum == 1) ? RegField.CPU1_en : RegField.CPU2_en;
        try {
            int modeConfig = readRegister(AllRegAddr.Mode_config.getAddress());
            int newConfig;
            if (enabled) {
                newConfig = modeConfig | cpuField.getMask();
            } else {
                newConfig = modeConfig & ~cpuField.getMask();
            }
            writeRegisterDirect(AllRegAddr.Mode_config.getAddress(), newConfig);
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    public void resetCpuWithRun(int cpuNum, int stopAddr1, boolean enaStopAddr1, int stopAddr2, boolean enaStopAddr2, Runnable onBreakpointsRead) {
        RegField cpuField = (cpuNum == 1) ? RegField.CPU1_en : RegField.CPU2_en;
        try {
            // Step 1: disable CPU
            int modeConfig = readRegister(AllRegAddr.Mode_config.getAddress());
            int disabled = modeConfig & ~cpuField.getMask();
            writeRegisterDirect(AllRegAddr.Mode_config.getAddress(), disabled);

            // Step 2: write breakpoint addresses + RUN command (bits 12..11 = 00 = RUN)
            int dbg_data = stopAddr2 + (enaStopAddr2 ? (1 << 9) : 0);
            int dbg_ctrl = stopAddr1 + (enaStopAddr1 ? (1 << 9) : 0) + (0 << 11);

            List<Integer> dataPacket = new ArrayList<>();
            dataPacket.add(cpuNum == 1 ? AllRegAddr.P1BG_data.getAddress() : AllRegAddr.P2BG_data.getAddress());
            dataPacket.add(dbg_data);
            dataPacket.add(cpuNum == 1 ? AllRegAddr.P1BG_ctrl.getAddress() : AllRegAddr.P2BG_ctrl.getAddress());
            dataPacket.add(dbg_ctrl);
            PacketToIc packet1 = new PacketToIc(McuCommand.WRITE_RANDOM, 0, dataPacket.size(), dataPacket);
            Thread action1 = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packet1));
            action1.join(1000);

            // Trigger: same with bit 10 set
            List<Integer> dataPacket2 = new ArrayList<>();
            dataPacket2.add(cpuNum == 1 ? AllRegAddr.P1BG_data.getAddress() : AllRegAddr.P2BG_data.getAddress());
            dataPacket2.add(dbg_data);
            dataPacket2.add(cpuNum == 1 ? AllRegAddr.P1BG_ctrl.getAddress() : AllRegAddr.P2BG_ctrl.getAddress());
            dataPacket2.add(dbg_ctrl | (1 << 10));
            PacketToIc packet2 = new PacketToIc(McuCommand.WRITE_RANDOM, 0, dataPacket2.size(), dataPacket2);
            Thread action2 = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packet2));
            action2.join(1000);

            // Step 3: enable CPU
            writeRegisterDirect(AllRegAddr.Mode_config.getAddress(), disabled | cpuField.getMask());

            // Step 4: read breakpoint state from hardware
            readBreakpointsFromHardware(cpuNum);

            // Step 5: read CPU registers
            dbgCpuNum = cpuNum;
            ReadCpuRegsAction readAction = new ReadCpuRegsAction();
            startAction(readAction);

            // Step 6: update UI with hardware breakpoint state
            if (onBreakpointsRead != null) {
                SwingUtilities.invokeLater(onBreakpointsRead);
            }
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    public void enableHandTapAnalyzer(Runnable onSuccess, Runnable onError) {
        setProcessing(true);
        startAction(new EnableHandTapAnalyzerAction(onSuccess, onError));
    }

    public void enableDmaQuadAnalyzer(Runnable onSuccess, Runnable onError) {
        setProcessing(true);
        startAction(new EnableDmaQuadAnalyzerAction(onSuccess, onError));
    }

    public void disableDmaQuadAnalyzer() {
        Thread action = new DisableDmaQuadAnalyzerAction();
        action.start();
        try { action.join(5000); } catch (InterruptedException ignored) {}
    }

    public void disableHandTapAnalyzer() {
        Thread action = new DisableHandTapAnalyzerAction();
        action.start();
        try { action.join(5000); } catch (InterruptedException ignored) {}
    }

    public void enableDetectorAnalyzer(Runnable onSuccess, Runnable onError) {
        setProcessing(true);
        startAction(new EnableDetectorAnalyzerAction(onSuccess, onError));
    }

    public void disableDetectorAnalyzer() {
        Thread action = new DisableDetectorAnalyzerAction();
        action.start();
        try { action.join(5000); } catch (InterruptedException ignored) {}
    }

    private int readRegister(int address) throws Exception {
        Model.getUartModel().ensureConnected();
        PacketToIc readPkg = new PacketToIc(McuCommand.READ, address, 1, null);
        byte[] readPacket = PacketIcHelper.getBytesFromPacketToIc(readPkg);
        UartModel.SafeWaitResponseAction waitAction = Model.getUartModel().doSafeWaitResponse();
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        Model.getUartModel().safeWriteBytes(readPacket);
        waitAction.join(5000);
        byte[] readResponse = Model.getUartModel().getResponse();
        if (readResponse == null) {
            throw new Exception("No response from address " + address);
        }
        List<Integer> readValues = PacketIcHelper.getValuesFromIcPacket(readResponse);
        if (readValues.isEmpty()) {
            throw new Exception("Empty response from address " + address);
        }
        return readValues.get(0);
    }

    private void writeRegisterDirect(int address, int value) throws Exception {
        Model.getUartModel().ensureConnected();
        List<Integer> data = Collections.singletonList(value);
        PacketToIc pkg = PacketIcHelper.createPacketForWriteToIc(address, data);
        byte[] packet = PacketIcHelper.getBytesFromPacketToIc(pkg);
        UartModel.SafeWaitResponseAction waitAction = Model.getUartModel().doSafeWaitResponse();
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        Model.getUartModel().safeWriteBytes(packet);
        waitAction.join(5000);
    }

    private List<String> loadHandTapCpu1Hex() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream("handtap_asm/cpu1_data.hex");
        if (is == null) {
            throw new IOException("Resource handtap_asm/cpu1_data.hex not found");
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }
        return lines;
    }

    private List<String> loadDmaCpu1Hex() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream("dma_asm/cpu1_data.hex");
        if (is == null) {
            throw new IOException("Resource dma_asm/cpu1_data.hex not found");
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }
        return lines;
    }

    private List<String> loadDetectorCpu1Hex() throws IOException {
        InputStream is = ClassLoader.getSystemResourceAsStream("detector_asm/cpu1_data.hex");
        if (is == null) {
            throw new IOException("Resource detector_asm/cpu1_data.hex not found");
        }
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
        }
        return lines;
    }

    private class EnableHandTapAnalyzerAction extends Thread {
        private final Runnable onSuccess;
        private final Runnable onError;

        EnableHandTapAnalyzerAction(Runnable onSuccess, Runnable onError) {
            this.onSuccess = onSuccess;
            this.onError = onError;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                int modeAddr = AllRegAddr.Mode_config.getAddress();
                int afeAddr = AllRegAddr.AFE_config.getAddress();

                // Step 1: Disable CPU1 and CPU2
                logger.debug("[HandTap] Step 1: Disabling CPU1 and CPU2...");
                int modeConfig = readRegister(modeAddr);
                int newMode = modeConfig & ~RegField.CPU1_en.getMask() & ~RegField.CPU2_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[HandTap] Step 1 done. Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                // Step 2: Configure shared RAM
                logger.debug("[HandTap] Step 2: Configuring AFE_config...");
                int afeConfig = readRegister(afeAddr);
                int newAfe = (afeConfig & ~RegField.SHRD_CPU2.getMask()) | RegField.SHRD_RAM.getMask();
                writeRegisterDirect(afeAddr, newAfe);
                logger.debug("[HandTap] Step 2 done. AFE_config: 0x"
                        + Integer.toHexString(afeConfig) + " -> 0x" + Integer.toHexString(newAfe));

                // Step 3: Program cpu1_data.hex to address 512
                logger.debug("[HandTap] Step 3: Programming cpu1_data.hex...");
                List<String> hexLines = loadHandTapCpu1Hex();
                List<PacketToIc> packets = PacketIcHelper.createPacketsForNormValues(512, hexLines);
                for (PacketToIc pkt : packets) {
                    byte[] packetBytes = PacketIcHelper.getBytesFromPacketToIc(pkt);
                    Thread action = Model.getUartModel().doExchangePacket(packetBytes);
                    action.join(2000);
                }
                logger.debug("[HandTap] Step 3 done. Wrote " + hexLines.size()
                        + " words in " + packets.size() + " packets.");

                // Step 4: Enable CPU1
                logger.debug("[HandTap] Step 4: Enabling CPU1...");
                modeConfig = readRegister(modeAddr);
                newMode = modeConfig | RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[HandTap] Step 4 done. Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                // Step 5: Success
                logger.debug("[HandTap] Analyzer mode enabled.");
                success = true;
                if (onSuccess != null) {
                    onSuccess.run();
                }

            } catch (Exception e) {
                logger.debug("[HandTap] Error: " + e.getMessage());
                logger.error("Error", e);
            } finally {
                setProcessing(false);
                if (!success && onError != null) {
                    onError.run();
                }
            }
        }
    }

    private class EnableDmaQuadAnalyzerAction extends Thread {
        private final Runnable onSuccess;
        private final Runnable onError;

        EnableDmaQuadAnalyzerAction(Runnable onSuccess, Runnable onError) {
            this.onSuccess = onSuccess;
            this.onError = onError;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                int c1ResCntrlAddr = AllRegAddr.C1ResCntrl.getAddress();
                int c2ResCntrlAddr = AllRegAddr.C2ResCntrl.getAddress();
                int modeAddr = AllRegAddr.Mode_config.getAddress();

                // Step 1: Configure C1 ResCntrl
                logger.debug("[DMA Quad] Step 1: Configuring C1 ResCntrl...");
                int c1ResCntrl = readRegister(c1ResCntrlAddr);
                int newC1ResCntrl = (c1ResCntrl & ~RegField.Enc_en.getMask())
                        | RegField.SPI_ext_en.getMask()
                        | RegField.Vel_from_cpu.getMask()
                        | RegField.Coord_from_cpu.getMask();
                writeRegisterDirect(c1ResCntrlAddr, newC1ResCntrl);
                logger.debug("[DMA Quad] Step 1 done. C1 ResCntrl: 0x"
                        + Integer.toHexString(c1ResCntrl) + " -> 0x" + Integer.toHexString(newC1ResCntrl));

                // Step 2: Configure C2 ResCntrl
                logger.debug("[DMA Quad] Step 2: Configuring C2 ResCntrl...");
                int c2ResCntrl = readRegister(c2ResCntrlAddr);
                int newC2ResCntrl = (c2ResCntrl & ~RegField.Enc_en.getMask())
                        | RegField.SPI_ext_en.getMask()
                        | RegField.Vel_from_cpu.getMask()
                        | RegField.Coord_from_cpu.getMask();
                writeRegisterDirect(c2ResCntrlAddr, newC2ResCntrl);
                logger.debug("[DMA Quad] Step 2 done. C2 ResCntrl: 0x"
                        + Integer.toHexString(c2ResCntrl) + " -> 0x" + Integer.toHexString(newC2ResCntrl));

                // Step 3: Disable CPU1 in Mode_config
                logger.debug("[DMA Quad] Step 3: Disabling CPU1...");
                int modeConfig = readRegister(modeAddr);
                int newMode = modeConfig & ~RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[DMA Quad] Step 3 done. Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                // Step 4: Program dma_asm/cpu1_data.hex at address 512
                logger.debug("[DMA Quad] Step 4: Programming cpu1_data.hex...");
                List<String> hexLines = loadDmaCpu1Hex();
                List<PacketToIc> packets = PacketIcHelper.createPacketsForNormValues(512, hexLines);
                for (PacketToIc pkt : packets) {
                    byte[] packetBytes = PacketIcHelper.getBytesFromPacketToIc(pkt);
                    UartModel.SafeWaitResponseAction waitAction = Model.getUartModel().doSafeWaitResponse();
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                    Model.getUartModel().safeWriteBytes(packetBytes);
                    waitAction.join(5000);
                }
                logger.debug("[DMA Quad] Step 4 done. Wrote " + hexLines.size()
                        + " words in " + packets.size() + " packets.");

                // Step 5: Enable CPU1 in Mode_config
                logger.debug("[DMA Quad] Step 5: Enabling CPU1...");
                modeConfig = readRegister(modeAddr);
                newMode = modeConfig | RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[DMA Quad] Step 5 done. Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                logger.debug("[DMA Quad] Analyzer mode enabled.");
                success = true;
                if (onSuccess != null) {
                    onSuccess.run();
                }

            } catch (Exception e) {
                logger.debug("[DMA Quad] Error: " + e.getMessage());
                logger.error("Error", e);
            } finally {
                setProcessing(false);
                if (!success && onError != null) {
                    onError.run();
                }
            }
        }
    }

    private class DisableDmaQuadAnalyzerAction extends Thread {
        @Override
        public void run() {
            try {
                int c1ResCntrlAddr = AllRegAddr.C1ResCntrl.getAddress();
                int c2ResCntrlAddr = AllRegAddr.C2ResCntrl.getAddress();
                int modeAddr = AllRegAddr.Mode_config.getAddress();

                // Clear SPI_ext_en, Vel_from_cpu, Coord_from_cpu in C1 ResCntrl
                int c1ResCntrl = readRegister(c1ResCntrlAddr);
                int newC1 = c1ResCntrl
                        & ~RegField.SPI_ext_en.getMask()
                        & ~RegField.Vel_from_cpu.getMask()
                        & ~RegField.Coord_from_cpu.getMask();
                writeRegisterDirect(c1ResCntrlAddr, newC1);
                logger.debug("[DMA Quad] Disable: C1 ResCntrl: 0x"
                        + Integer.toHexString(c1ResCntrl) + " -> 0x" + Integer.toHexString(newC1));

                // Clear SPI_ext_en, Vel_from_cpu, Coord_from_cpu in C2 ResCntrl
                int c2ResCntrl = readRegister(c2ResCntrlAddr);
                int newC2 = c2ResCntrl
                        & ~RegField.SPI_ext_en.getMask()
                        & ~RegField.Vel_from_cpu.getMask()
                        & ~RegField.Coord_from_cpu.getMask();
                writeRegisterDirect(c2ResCntrlAddr, newC2);
                logger.debug("[DMA Quad] Disable: C2 ResCntrl: 0x"
                        + Integer.toHexString(c2ResCntrl) + " -> 0x" + Integer.toHexString(newC2));

                // Disable CPU1
                int modeConfig = readRegister(modeAddr);
                int newMode = modeConfig & ~RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[DMA Quad] Disable: Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                logger.debug("[DMA Quad] Analyzer mode disabled.");
            } catch (Exception e) {
                logger.debug("[DMA Quad] Disable error: " + e.getMessage());
            }
        }
    }

    private class DisableHandTapAnalyzerAction extends Thread {
        @Override
        public void run() {
            try {
                int modeAddr = AllRegAddr.Mode_config.getAddress();

                // Disable CPU1
                int modeConfig = readRegister(modeAddr);
                int newMode = modeConfig & ~RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[HandTap] Disable: Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                logger.debug("[HandTap] Analyzer mode disabled.");
            } catch (Exception e) {
                logger.debug("[HandTap] Disable error: " + e.getMessage());
            }
        }
    }

    private class EnableDetectorAnalyzerAction extends Thread {
        private final Runnable onSuccess;
        private final Runnable onError;

        EnableDetectorAnalyzerAction(Runnable onSuccess, Runnable onError) {
            this.onSuccess = onSuccess;
            this.onError = onError;
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                int modeAddr = AllRegAddr.Mode_config.getAddress();
                int c2KonturAddr = AllRegAddr.C2KonturStngs.getAddress();

                // Step 1: Disable CPU1
                logger.debug("[Detector] Step 1: Disabling CPU1...");
                int modeConfig = readRegister(modeAddr);
                int newMode = modeConfig & ~RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[Detector] Step 1 done. Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                // Step 2: Program detector_asm/cpu1_data.hex at address 512
                logger.debug("[Detector] Step 2: Programming cpu1_data.hex...");
                List<String> hexLines = loadDetectorCpu1Hex();
                List<PacketToIc> packets = PacketIcHelper.createPacketsForNormValues(512, hexLines);
                for (PacketToIc pkt : packets) {
                    byte[] packetBytes = PacketIcHelper.getBytesFromPacketToIc(pkt);
                    UartModel.SafeWaitResponseAction waitAction = Model.getUartModel().doSafeWaitResponse();
                    try { Thread.sleep(10); } catch (InterruptedException ignored) {}
                    Model.getUartModel().safeWriteBytes(packetBytes);
                    waitAction.join(5000);
                }
                logger.debug("[Detector] Step 2 done. Wrote " + hexLines.size()
                        + " words in " + packets.size() + " packets.");

                // Step 3: Set HandToEXT in C2 KonturStngs
                logger.debug("[Detector] Step 3: Setting HandToEXT in C2 KonturStngs...");
                int c2Kontur = readRegister(c2KonturAddr);
                int newC2Kontur = c2Kontur | RegField.HandToEXT.getMask();
                writeRegisterDirect(c2KonturAddr, newC2Kontur);
                logger.debug("[Detector] Step 3 done. C2KonturStngs: 0x"
                        + Integer.toHexString(c2Kontur) + " -> 0x" + Integer.toHexString(newC2Kontur));

                // Step 4: Enable CPU1
                logger.debug("[Detector] Step 4: Enabling CPU1...");
                modeConfig = readRegister(modeAddr);
                newMode = modeConfig | RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[Detector] Step 4 done. Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                logger.debug("[Detector] Analyzer mode enabled.");
                success = true;
                if (onSuccess != null) {
                    onSuccess.run();
                }

            } catch (Exception e) {
                logger.debug("[Detector] Error: " + e.getMessage());
                logger.error("Error", e);
            } finally {
                setProcessing(false);
                if (!success && onError != null) {
                    onError.run();
                }
            }
        }
    }

    private class DisableDetectorAnalyzerAction extends Thread {
        @Override
        public void run() {
            try {
                int modeAddr = AllRegAddr.Mode_config.getAddress();
                int c2KonturAddr = AllRegAddr.C2KonturStngs.getAddress();

                // Clear HandToEXT in C2 KonturStngs
                int c2Kontur = readRegister(c2KonturAddr);
                int newC2Kontur = c2Kontur & ~RegField.HandToEXT.getMask();
                writeRegisterDirect(c2KonturAddr, newC2Kontur);
                logger.debug("[Detector] Disable: C2KonturStngs: 0x"
                        + Integer.toHexString(c2Kontur) + " -> 0x" + Integer.toHexString(newC2Kontur));

                // Disable CPU1
                int modeConfig = readRegister(modeAddr);
                int newMode = modeConfig & ~RegField.CPU1_en.getMask();
                writeRegisterDirect(modeAddr, newMode);
                logger.debug("[Detector] Disable: Mode_config: 0x"
                        + Integer.toHexString(modeConfig) + " -> 0x" + Integer.toHexString(newMode));

                logger.debug("[Detector] Analyzer mode disabled.");
            } catch (Exception e) {
                logger.debug("[Detector] Disable error: " + e.getMessage());
            }
        }
    }


    // --- Write verification helpers ---

    private static class VerifyResult {
        int totalWords;
        int totalMismatches;
        final List<String> errors = new ArrayList<>();

        void addOk(int count) { totalWords += count; }
        void addError(int addr, int written, int read) {
            totalWords++;
            totalMismatches++;
            errors.add(String.format("addr=%d: written=0x%04X != read=0x%04X", addr, written, read));
        }

        void printSummary() {
            if (totalMismatches == 0) {
                System.out.printf("Successfully wrote %d values%n", totalWords);
            } else {
                System.out.printf("Failed write: %d of %d values mismatched%n", totalMismatches, totalWords);
                for (String err : errors) {
                    logger.debug("  " + err);
                }
            }
        }
    }

    private VerifyResult verifyChunk(int address, List<Integer> expectedValues, McuCommand readCommand, VerifyResult result) {
        if (expectedValues == null || expectedValues.isEmpty()) return result;

        PacketToIc pkg = new PacketToIc(readCommand, address, expectedValues.size(), null);
        byte[] packet = PacketIcHelper.getBytesFromPacketToIc(pkg);
        byte[] response = null;
        try {
            Thread action = Model.getUartModel().doExchangePacket(packet);
            action.join(1000);
            response = Model.getUartModel().getResponse();
        } catch (InterruptedException e) {
            logger.error("Error", e);
            result.addOk(expectedValues.size());
            return result;
        }

        if (response == null || response.length <= 9) {
            System.out.printf("VERIFY ERROR: no response for addr=%d%n", address);
            for (int j = 0; j < expectedValues.size(); j++) {
                result.addError(address + j, expectedValues.get(j), 0);
            }
            return result;
        }

        List<Integer> actual = PacketIcHelper.getValuesFromIcPacket(response);
        for (int j = 0; j < expectedValues.size() && j < actual.size(); j++) {
            int written = expectedValues.get(j);
            int read = actual.get(j);
            if (written != read) {
                result.addError(address + j, written, read);
            } else {
                result.addOk(1);
            }
        }
        // If actual is shorter than expected, count missing as errors
        for (int j = actual.size(); j < expectedValues.size(); j++) {
            result.addError(address + j, expectedValues.get(j), 0);
        }
        return result;
    }

    /**
     * Public wrapper for single-word verification (used by BatchWriterPanel).
     */
    public void verifySingleWord(int address, int expectedValue) {
        VerifyResult result = new VerifyResult();
        List<Integer> list = new ArrayList<>();
        list.add(expectedValue);
        verifyChunk(address, list, McuCommand.READ, result);
        result.printSummary();
    }


    private class WriteValuesAction extends Thread {
        @Override
        public void run() {
            VerifyResult result = new VerifyResult();
            for (Map.Entry<Integer, List<Integer>> entry : reqValues.entrySet()){
                PacketToIc pkg = PacketIcHelper.createPacketForWriteToIc(entry.getKey(), entry.getValue());
                byte [] packet  = PacketIcHelper.getBytesFromPacketToIc(pkg);

                try {
                    Thread action = Model.getUartModel().doExchangePacket(packet);

                    action.join(1000);
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    return;
                }

                // Verify written data
                verifyChunk(entry.getKey(), entry.getValue(), McuCommand.READ, result);
            }

            result.printSummary();
            setProcessing(false);


        }
    }

    private class WriteThenResumeAction extends Thread {
        private final Map<Integer, List<Integer>> writeData;
        private final Runnable resume;

        WriteThenResumeAction(Map<Integer, List<Integer>> writeData, Runnable resume) {
            this.writeData = writeData;
            this.resume = resume;
        }

        @Override
        public void run() {
            setProcessing(true);
            VerifyResult result = new VerifyResult();
            for (Map.Entry<Integer, List<Integer>> entry : writeData.entrySet()) {
                PacketToIc pkg = PacketIcHelper.createPacketForWriteToIc(entry.getKey(), entry.getValue());
                byte[] packet = PacketIcHelper.getBytesFromPacketToIc(pkg);
                try {
                    Thread action = Model.getUartModel().doExchangePacket(packet);
                    action.join(1000);
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    break;
                }

                // Verify written data
                verifyChunk(entry.getKey(), entry.getValue(), McuCommand.READ, result);
            }
            result.printSummary();
            if (!signalStopProcessing && resume != null) {
                resume.run();
            } else {
                signalStopProcessing = false;
                setProcessing(false);
            }
        }
    }





    public void addLongProcessEventListener(ILongProcessEventListener listener){
        listLongProcessEventListeners.add(listener);
    }


    private class ReadValuesAction extends Thread {
        @Override
        public void run() {

            PacketToIc pkg = PacketIcHelper.createPacketForReadFromIc(readAddress, numReadValues);
            byte [] packet  = PacketIcHelper.getBytesFromPacketToIc(pkg);
            byte[] response = null;
            try {
                Thread action = Model.getUartModel().doExchangePacket(packet);

                action.join();
                response = Model.getUartModel().getResponse();
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            if(response != null && response.length != 0) {

                List<Integer> values = PacketIcHelper.getValuesFromIcPacket(response);
                int addr = PacketHelper.getUnsignedWord16bitInt(response[4],response[5]);
                int numVals = PacketHelper.getUnsignedWord16bitInt(response[6],response[7]);
                dispatchResponse(values, addr, numVals, curRunnerView);
            }else {
                logger.debug("Read Values canceled");
            }
            if(!isCyclic) {
                setProcessing(false);
            }


        }
    }


    public  void runDebuggerCommandAndRead(int command, int cpuNum, int stopAddr1, boolean enaStopAddr1, int stopAddr2, boolean  enaStopAddr2, boolean memEna, boolean buf1Ena, boolean buf2Ena){
        dbgCpuNum           = cpuNum;
        dbgCommand          = command;
        enaReadDbgCpuMem    = memEna;
        enaReadDbgCpuBuf1   = buf1Ena;
        enaReadDbgCpuBuf2   = buf2Ena;
        dbgCpuStopAddr2     = stopAddr2;
        enaDbgCpuStopAddr2  = enaStopAddr2;
        dbgCpuStopAddr1     = stopAddr1;
        enaDbgCpuStopAddr1 = enaStopAddr1;
        runDebuggerCommandAction action1 = new runDebuggerCommandAction();
        startAction(action1);
        try {
            action1.join(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ReadCpuRegsAction action2 = new ReadCpuRegsAction();
        startAction(action2);
    }




        private class runDebuggerCommandAction extends Thread {
            @Override
            public void run() {

                int dbg_data = dbgCpuStopAddr2 + (enaDbgCpuStopAddr2 ? (1<<9) : 0 );
                int dbg_ctrl = dbgCpuStopAddr1 + (enaDbgCpuStopAddr1 ? (1<<9) : 0 ) + (dbgCommand <<11);
                List<Integer> dataPacketPrev = new ArrayList<>();
                dataPacketPrev.add(dbgCpuNum  == 1 ? AllRegAddr.P1BG_data.getAddress() : AllRegAddr.P2BG_data.getAddress());
                dataPacketPrev.add(dbg_data);
                dataPacketPrev.add(dbgCpuNum  == 1 ? AllRegAddr.P1BG_ctrl.getAddress() : AllRegAddr.P2BG_ctrl.getAddress());
                dataPacketPrev.add(dbg_ctrl);

                PacketToIc packetPrev =  new PacketToIc(McuCommand.WRITE_RANDOM, 0,  dataPacketPrev.size(), dataPacketPrev );
                try {
                    Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetPrev));
                    action.join(1000);
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    return;
                }
                List<Integer> dataPacketPrev2 = new ArrayList<>();
                dataPacketPrev2.add(dbgCpuNum  == 1 ? AllRegAddr.P1BG_data.getAddress() : AllRegAddr.P2BG_data.getAddress());
                dataPacketPrev2.add(dbg_data);
                dataPacketPrev2.add(dbgCpuNum  == 1 ? AllRegAddr.P1BG_ctrl.getAddress() : AllRegAddr.P2BG_ctrl.getAddress());
                dataPacketPrev2.add(dbg_ctrl | (1 << 10));
                PacketToIc packetPrev2 =  new PacketToIc(McuCommand.WRITE_RANDOM, 0,  dataPacketPrev.size(), dataPacketPrev2 );
                try {
                    Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetPrev2));
                    action.join(1000);
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    return;
                }

                if (!isCyclic) {
                    setProcessing(false);
                }

            }

        }


        private class ReadCpuRegsAction extends Thread {
        @Override
        public void run() {

            getCpuRegs();
            if(enaReadDbgCpuMem){
                getMemoryCpuValues();
            }
            if(enaReadDbgCpuBuf1){
                curRunnerView       =  RunnerView.DBG_BUF1_MEM;
                PacketToIc pkg      = PacketIcHelper.createPacketForReadFromIc(768, 128);
                readValues(pkg);
            }
            if(enaReadDbgCpuBuf2){
                curRunnerView       =  RunnerView.DBG_BUF2_MEM;
                PacketToIc pkg      = PacketIcHelper.createPacketForReadFromIc(1280, 128);
                readValues(pkg);
            }
            if(!isCyclic) {
                setProcessing(false);
            }

        }

        private void getCpuRegs(){
            curRunnerView = dbgCpuNum  == 1 ? RunnerView.DBG_CPU1_REGS : RunnerView.DBG_CPU2_REGS;
            int dummyAddress = dbgCpuStopAddr2 +  (enaDbgCpuStopAddr2 ? 1<<9 : 0) + (dbgCpuNum << 10);
            PacketToIc pkg =  new PacketToIc(McuCommand.READ_CPU_REGS, dummyAddress,  null );
            readValues(pkg);

        }
        private void getMemoryCpuValues(){
            curRunnerView       = dbgCpuNum  == 1 ? RunnerView.DBG_CPU1_MEM : RunnerView.DBG_CPU2_MEM;
            int addressMemory   = dbgCpuNum  == 1 ? 512 : 1024;
            PacketToIc pkg      = PacketIcHelper.createPacketForReadFromIc(addressMemory, 256);
            readValues(pkg);
        }





        private void readValues(PacketToIc pkg){
            byte [] packet  = PacketIcHelper.getBytesFromPacketToIc(pkg);
            byte[] response = null;
            try {
                Thread action = Model.getUartModel().doExchangePacket(packet);
                action.join();
                response = Model.getUartModel().getResponse();
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            if(response != null && response.length != 0) {
                List<Integer> values = PacketIcHelper.getValuesFromIcPacket(response);
                int addr = PacketHelper.getUnsignedWord16bitInt(response[4],response[5]);
                int numVals = PacketHelper.getUnsignedWord16bitInt(response[6],response[7]);
                dispatchResponse(values, addr, numVals, curRunnerView);
            }else {
                logger.debug("Read Values canceled");
            }
        }

    }





    private class ReadOTPValuesAction extends Thread {


        @Override
        public void run() {


                int     REN_ctrl    = 0x04;
                PacketToIc packetPrev = new PacketToIc(McuCommand.WRITE, AllRegAddr.BOTP_ctrl.getAddress(),  REN_ctrl );
                try {
                    Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetPrev));
                    action.join(1000);
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    return;
                }
                List<Integer> resultReadList = new ArrayList<>();
                for(int  i = 0; i< numReadValues; i++){
                    PacketToIc packet =  new PacketToIc(McuCommand.WRITE, AllRegAddr.BOTP_addr.getAddress(),  readAddress+i );
                    byte [] bytes  = PacketIcHelper.getBytesFromPacketToIc(packet);
                    try {
                        Thread action = Model.getUartModel().doExchangePacket(bytes);
                        action.join(1000);
                    } catch (InterruptedException e) {
                        logger.error("Error", e);
                        return;
                    }
                    PacketToIc packetRead = new PacketToIc(McuCommand.READ, AllRegAddr.BOTP_out.getAddress(), null );
                    byte [] bytes2  = PacketIcHelper.getBytesFromPacketToIc(packetRead);
                    try {
                        Thread action = Model.getUartModel().doExchangePacket(bytes2);
                        action.join(1000);
                    } catch (InterruptedException e) {
                        logger.error("Error", e);
                        return;
                    }
                    byte[] response = Model.getUartModel().getResponse();
                    resultReadList.add(PacketHelper.getUnsignedWord16bitInt(response[8],response[9]));

                }

                dispatchResponse(resultReadList, readAddress, numReadValues, curRunnerView);



                int     noPGM_ctrl  = 0x00;
                List<Integer> dataPacketAfter = new ArrayList<>();
                dataPacketAfter.add(AllRegAddr.BOTP_ctrl.getAddress());
                dataPacketAfter.add(noPGM_ctrl);
                PacketToIc packetAfter =  new PacketToIc(McuCommand.WRITE, 0,  0x0003 );
                try {
                    Thread action = Model.getUartModel().doExchangePacket(PacketIcHelper.getBytesFromPacketToIc(packetAfter));
                    action.join(1000);
                } catch (InterruptedException e) {
                    logger.error("Error", e);
                    return;
                }


            setProcessing(false);
        }
    }



    private class ReadRandomAddressValuesAction extends Thread {
        @Override
        public void run() {
            PacketToIc pkg = PacketIcHelper.createPacketForRandomReadFromIc(listRandomAddrReadValues);
            byte [] packet  = PacketIcHelper.getBytesFromPacketToIc(pkg);
            byte[] response;
            try {
                Thread action = Model.getUartModel().doExchangePacket(packet);
                action.join();
                response = Model.getUartModel().getResponse();
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            if(response != null && response.length != 0) {
                List<Integer> values = PacketIcHelper.getValuesFromIcPacket(response);
                int addr = PacketHelper.getUnsignedWord16bitInt(response[4], response[5]);
                int numVals = PacketHelper.getUnsignedWord16bitInt(response[6], response[7]);
                dispatchResponse(values, addr, numVals, curRunnerView);
            }else {
                logger.debug("Read Values canceled");
            }
            if(!isCyclic) {
                setProcessing(false);
            }
        }

    }



    private class DmaQuadReadValuesAction extends Thread {
        @Override
        public void run() {
            byte[] response;
            try {
                Thread action = Model.getUartModel().doWaitReponsePacket();
                action.join();
                response = Model.getUartModel().getResponse();
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            if(response != null && response.length != 0) {
                List<Integer> values = PacketIcHelper.getValuesFromIcPacket(response);
                int addr = PacketHelper.getUnsignedWord16bitInt(response[4],response[5]);
                int numVals = PacketHelper.getUnsignedWord16bitInt(response[6],response[7]);
                dispatchResponse(values, addr, numVals, curRunnerView);
            }else {
                logger.debug("Read Values canceled");
            }
            if(!isCyclic) {
                setProcessing(false);
            }
        }
    }




    private class ReadTapValuesAction extends Thread {
        @Override
        public void run() {
            List <Integer> data = new ArrayList<>();
            actionHandValue = actionHandValue + 1;
            data.add(actionHandValue);
            data.add(listRandomAddrReadValues.get(0));
            PacketToIc pkg =  new PacketToIc(McuCommand.READ_HANDTAP, 760,  2, data );
            byte [] packet  = PacketIcHelper.getBytesFromPacketToIc(pkg);
            byte[] response;
            try {
                Thread action = Model.getUartModel().doExchangePacket(packet);
                action.join();
                response = Model.getUartModel().getResponse();
            } catch (InterruptedException e) {
                logger.error("Error", e);
                return;
            }
            if(response != null && response.length != 0) {
                try {
                    List<Integer> values = PacketIcHelper.getValuesFromIcPacket(response);
                    int addr = PacketHelper.getUnsignedWord16bitInt(response[4],response[5]);
                    int numVals = PacketHelper.getUnsignedWord16bitInt(response[6],response[7]);
                    dispatchResponse(values, addr, numVals, curRunnerView);
                } catch ( ArrayIndexOutOfBoundsException e ){
                    logger.error("Error", e);
                }
            }else {
                logger.debug("Read Values canceled");
            }
            if(!isCyclic) {
                setProcessing(false);
            }
        }
    }


    public void readSingleValue (int address){
        readAddress = address;
        curRunnerView = RunnerView.TABLE_SINGLE_VALUE;
        numReadValues = 1;
        ReadValuesAction action = new ReadValuesAction();
        startAction(action);

    }

    public void readSingleValue (int address, int count){
        readAddress = address;
        curRunnerView = RunnerView.TABLE_SINGLE_VALUE;
        numReadValues = count;
        ReadValuesAction action = new ReadValuesAction();
        startAction(action);
    }

    public void addMemoryEventListener (RunnerView runnerView, IMemoryEventListener listener){
        mapMemoryListeners.put(runnerView, listener);
    }





    public void setReqValues(int address , List<Integer> reqValues ) {
        Map<Integer, List<Integer>> values = new HashMap<>();
        values.put(address, reqValues);
        this.reqValues = values;

    }
    public void setReqValues(Map<Integer, List<Integer>> reqValues) {
        this.reqValues = reqValues;

    }

    private void setProcessing(boolean value){
        isProcessing = value;
        if(value == false) {
            if (pendingWriteAfterStop != null) {
                Map<Integer, List<Integer>> writeData = pendingWriteAfterStop;
                pendingWriteAfterStop = null;
                Runnable resume = pendingResume;
                pendingResume = null;
                Model.getUartModel().resetPermits();
                startAction(new WriteThenResumeAction(writeData, resume));
                return;
            }
            pendingResume = null;
            Model.getUartModel().resetPermits();
        }
        for(ILongProcessEventListener listener: listLongProcessEventListeners){
            listener.updateStatusOfProcessing();
        }

    }

    public void setExternalProcessing(boolean value) {
        isProcessing = value;
        if (!value) {
            Model.getUartModel().resetPermits();
        }
        for (ILongProcessEventListener listener : listLongProcessEventListeners) {
            listener.updateStatusOfProcessing();
        }
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public boolean isCyclic() {
        return isCyclic;
    }

    public Integer getReqReadAddress() {
        return readAddress;
    }

    public void setReqReadAddress(Integer reqReadAddress) {
        this.readAddress = reqReadAddress;
    }



    public void setNameFileTxtValues(String nameFileTxtValues) {
        this.nameFileTxtValues = nameFileTxtValues;
    }

    public void setCountTxtValues(Integer countTxtValues) {
        this.countTxtValues = countTxtValues;
    }

    public void setAddressForTxtValues(Integer addressForTxtValues) {
        this.addressForTxtValues = addressForTxtValues;
    }

    public void setNumReadValues(Integer numReadValues) {
        this.numReadValues = numReadValues;
    }

    public String getPathReadWriteFiles() {
        return pathReadWriteFiles;
    }

    public void setPathReadWriteFiles(String pathReadWriteFiles) {
        this.pathReadWriteFiles = pathReadWriteFiles;
    }

    public void setCurRunnerView(RunnerView curRunnerView) {
        this.curRunnerView = curRunnerView;
    }

    private final Object dispatchLock = new Object();

    public void dispatchResponse(List<Integer> values, int address, int numValues, RunnerView view) {
        synchronized (dispatchLock) {
            this.respValues = values;
            this.respAddress = address;
            this.respNumValues = numValues;
            IMemoryEventListener listener = mapMemoryListeners.get(view);
            if (listener != null) {
                listener.updateValues();
            }
        }
    }


    public List<Integer> getRespValues() {
        return respValues;
    }

    public void setFileNameRecordTxt(String fileNameRecordTxt) {
        this.fileNameRecordTxt = fileNameRecordTxt;
    }

    public Integer getRespAddress() {
        return respAddress;
    }

    public Integer getRespNumValues() {
        return respNumValues;
    }

    public void setInitReqValues(Map<Integer, List<Integer>> initReqValues) {
        this.initReqValues = initReqValues;
    }

    public int getNumBaseMemoryValues() {
        return numBaseMemoryValues;
    }




}
