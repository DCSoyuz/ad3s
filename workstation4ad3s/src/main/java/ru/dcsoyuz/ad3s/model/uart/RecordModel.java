package ru.dcsoyuz.ad3s.model.uart;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.model.Model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by yuri.filatov on 01.08.2016.
 */
public class RecordModel {

    private static final Logger logger = LoggerFactory.getLogger(RecordModel.class);

    private int baudRate = 461538;
    private String portName = "COM13";
    private SerialPort serialPort;
    private Boolean isConfig = false;
    private Boolean isRecording = false;
    private String recordingFileName;

    private String patch;
    private FileWriter writer;

    public RecordModel() {
        if (WorkstationConfig.getProperty("SELECED_RECORD_COM_PORT") == null) {
            WorkstationConfig.setProperty("SELECED_RECORD_COM_PORT", "COM1");
        }
        this.portName = WorkstationConfig.getProperty("SELECED_RECORD_COM_PORT");
    }

    public void startRecording(String fileName) {
        isRecording = true;
        recordingFileName = fileName;
        createPatch();
        RecordingThread t = new RecordingThread();
        t.start();
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void createPatch() {
        if (recordingFileName.equals("")) {
            logger.warn("NO Save! Patch is bad!");
        } else {
            CreateFolder(Model.getNameRecFolder());
            patch = Model.getNameRecFolder() + "/" + recordingFileName + ".txt";
            File dir = new File(patch);
            for (int i = 1; i > 0; i++) {
                if (!dir.isFile()) {
                    break;
                }
                patch = Model.getNameRecFolder() + "/" + recordingFileName + "(" + i + ")" + ".txt";
                dir = new File(patch);
            }
        }
    }



    public class RecordingThread extends Thread {

        @Override
        public void run() {
            try {
                if (serialPort == null) {
                    openSerial();
                }
                try (FileWriter writer = new FileWriter(patch, true)) {
                    serialPort.purgePort(SerialPort.PURGE_RXCLEAR);

                    serialPort.getInputBufferBytesCount();

                    while (isRecording) {
                        byte[]  response= null;
                        sleep(1);
                        response = serialPort.readBytes();
                        if (response == null || response.length == 0) {
                        } else {
                            String text;
                            text =  parseArrayByteOlegPacket(response, 8);
                            writer.write(text);
                            writer.flush();
                        }
                    }
                    writer.flush();
                    logger.info("Save - OK\nRatch is save: {}", patch);
                } catch (IOException ex) {
                    logger.error("Error writing to recording file", ex);
                }
            } catch (InterruptedException e) {
                logger.error("Recording thread interrupted", e);
            }
        }



        private String parseArrayByteOlegPacket(byte[] buffer, int numWord ){


            String res = "";
            int sizePiece = (numWord+1)*2;
            int maxIndexArr = (buffer.length - (numWord+1)*2);

            for(int i =0; i < maxIndexArr; i++){
                if(buffer[i]==(byte)0xAA){
                    byte[] pieceByte = new byte[sizePiece];
                    System.arraycopy(buffer, i, pieceByte, 0, sizePiece);
                    String row;
                    if(((row = tryParse(pieceByte, numWord)) != null)){
                        res = res + row;
                        i = i+sizePiece-1;
                    }
                }


            }
            return res;
        }

        private String  tryParse( byte [] pieceBytes, int numWordRow){
            String res = "";
            byte check_sum =(byte) 0x55;
            for(int i =1; i<pieceBytes.length-1; i++){
                check_sum =(byte) (check_sum + pieceBytes[i]);
            }
            if(check_sum != pieceBytes[pieceBytes.length-1]){
                return null;
            }else{
                for(int i=0; i<= numWordRow-1; i++){
                    int lb = pieceBytes[1+i*2];
                    int hb = pieceBytes[1+i*2+1];
                    int word = (0xFF00 &(hb << 8)) | (0xFF&lb);
                    res = res + "0x" + Integer.toHexString((word&0xFFFF)|0x10000).substring(1) + "\t";
                }
                res = res + "\n";
                return res;
            }


        }


    }

    private Double getSensorDouble(byte low_byte, byte high_byte) {
        return PacketHelper.getSensorDouble((byte) (low_byte & 0b00111111), (byte) (high_byte & 0b00111111));
    }

    private void CreateFolder(String Patch) {
        File theDir = new File(Patch);
//               if the directory does not exist, create it
        if (!theDir.exists()) {
            logger.info("creating directory: {}", theDir.getName());
            boolean result = false;
            try {
                theDir.mkdir();
                result = true;
            } catch (SecurityException se) {
                //handle it
            }
            if (result) {
                logger.info("Folder created");
            }
        }
    }

    private void openSerial() {
        try {
            if (serialPort == null || (!serialPort.isOpened())) {
                serialPort = new SerialPort(portName);
                //Открываем порт
                serialPort.openPort();
                //Выставляем параметры. Можно использовать и такую строку serialPort.setParams(9600, 8, 1, 0);
                logger.info("Reopen REC port with {}bits/sec   Port: {}", baudRate, portName);
                serialPort.setParams(baudRate,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);
            } else {
                reOpenFTDI();
            }
        } catch (SerialPortException e) {
            logger.error("Error opening REC serial port", e);
        }
    }

    public void reOpenFTDI() {
        try {
            if (serialPort != null) {
                if (serialPort.isOpened()) {
                    serialPort.closePort();
                }
                serialPort = null;
            }
            if (serialPort == null) {
                openSerial();
            }
        } catch (SerialPortException e) {
            logger.error("Error reopening FTDI serial port", e);
        }
    }

    public String getPortName() {
        return this.portName;
    }

    public void setPortName(String port) {
        this.portName = port;
        return;
    }

    public String[] getPortList() {
        String[] portNames = SerialPortList.getPortNames();
        return portNames;
    }

    public static byte[] editResponce(byte[] response) {
        byte[] resp;
        if (!((PacketHelper.getBitFromByte(response[0], 7)) == (PacketHelper.getBitFromByte(response[1], 7)))) {
            if (!((PacketHelper.getBitFromByte(response[response.length - 1], 7)) == (PacketHelper.getBitFromByte(response[response.length - 2], 7)))) {
                resp = new byte[response.length - 2];
                System.arraycopy(response, 1, resp, 0, response.length - 2);
                return resp;
            } else {
                resp = new byte[response.length - 1];
                System.arraycopy(response, 1, resp, 0, response.length - 1);
                return resp;
            }
        } else {
            if (!((PacketHelper.getBitFromByte(response[response.length - 1], 7)) == (PacketHelper.getBitFromByte(response[response.length - 2], 7)))) {
                resp = new byte[response.length - 1];
                System.arraycopy(response, 0, resp, 0, response.length - 1);
                return resp;
            } else {
                return response;
            }
        }
    }





    public static byte[] editResponce49(byte[] response) {
        byte[] resp;
        int start = -1;
        int end = -1;
        int count = 0;
        boolean count_en = false;
        for (int i = 0; i < response.length; i++) {
            if (PacketHelper.getBitFromByte(response[i], 7) == true) {
                start = start == -1 ? i : start;
                count_en = true;
            }
            if (count_en) count = count + 1;

            if (count == 7) {
                count_en = false;
                end = i;
                count = 0;
            }
        }
        if (end == -1 || start == -1) {
            resp = new byte[0];
        } else {
            resp = new byte[end - start];
            System.arraycopy(response, start, resp, 0, end - start);
        }
        return resp;
    }



}
