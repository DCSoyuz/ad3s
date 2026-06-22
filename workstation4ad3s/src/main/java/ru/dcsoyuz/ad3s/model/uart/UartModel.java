package ru.dcsoyuz.ad3s.model.uart;


import jssc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketIcHelper;


import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

/**
 * Created by yuri.filatov on 01.08.2016.
 */
public class UartModel {

    private static final Logger logger = LoggerFactory.getLogger(UartModel.class);

    private int baudRate = 1000000;//
    private String portName = null;
    private byte[] packetForSending;
    private byte[] packetReceived;
    private byte[] completedPacket;

    private LinkedList <Byte> linkedListPacketReceived = new LinkedList<>();
    private SerialPort serialPort;

    private int desiredNumBytes = 1;
    private Boolean isOpen = false;

    private boolean  isRecording = false;
    private PortReader regularPortReader;

    private RecordPortListener recordPortListener;

    private boolean debugOutput = false;

    private Writer fileWriter;

    private Consumer<List<Integer>> recordDataCallback;


    private int countReceiveByte = 0;

    // Флаг для остановки WaitResponseTask вместо Thread.stop()
    private volatile boolean stopWaitResponseTask = false;

    /**
     * Проверяет, является ли порт валидным (открыт и доступен)
     * Также проверяет реальную работоспособность порта, пытаясь получить статус
     * @return true если порт валиден, false в противном случае
     */
    private boolean isPortValid() {
        if (serialPort == null) {
            return false;
        }
        try {
            if (!serialPort.isOpened()) {
                return false;
            }
            // Проверяем реальную работоспособность порта, пытаясь получить статус
            // Если USB был отключен и подключен, порт может быть "открыт" но не работает
            try {
                serialPort.getInputBufferBytesCount();
                return true;
            } catch (SerialPortException e) {
                logger.warn("Port not responding: {}", e.getMessage());
                return false;
            }
        } catch (Exception e) {
            logger.warn("Port validation failed: {}", e.getMessage());
            return false;
        }
    }

    public void doWriteBytes(byte[] bytes) {
        ensureConnected();
        // Проверяем валидность порта перед записью
        if (!isPortValid()) {
            logger.warn("Port not valid, attempting to reconnect...");
            openSerial();
        }

        // Если после попытки переподключения порт всё ещё не валиден
        if (!isPortValid()) {
            logger.error("Failed to open serial port");
            throw new RuntimeException("Serial port not available");
        }

        logger.info("PC->MCU: {}", PacketHelper.convPacketToHexString(bytes, " "));
        try {
            serialPort.writeBytes(bytes);
        } catch (SerialPortException e) {
            // Пытаемся переподключиться и повторить операцию
            logger.warn("Write failed, attempting to reconnect: {}", e.getMessage());
            openSerial();
            if (isPortValid()) {
                try {
                    serialPort.writeBytes(bytes);
                    logger.info("Write successful after reconnect");
                } catch (SerialPortException e2) {
                    throw new RuntimeException("Failed to write after reconnect", e2);
                }
            } else {
                throw new RuntimeException("Failed to reconnect to serial port", e);
            }
        }

    }

    private final Semaphore available = new Semaphore(1, true);

    private class PortReader implements SerialPortEventListener {


        public void serialEvent(SerialPortEvent event) {
            try {
                if (event.isRXCHAR() && event.getEventValue() > 0) {
                    byte[] resp = serialPort.readBytes();

                    if (recordDataCallback != null) {
                        // Recording mode: accumulate into linked list, extract complete packets
                        for (int i = 0; i < resp.length; i++) {
                            linkedListPacketReceived.addLast(resp[i]);
                        }
                        while (linkedListPacketReceived.size() > 3
                                && linkedListPacketReceived.get(0) == PacketIcHelper.START_RESP_BYTE) {
                            int len = PacketHelper.getUnsignedWord16bitInt(
                                    linkedListPacketReceived.get(1), linkedListPacketReceived.get(2));
                            if (linkedListPacketReceived.size() >= len) {
                                // Извлекаем пакет в byte[] для корректного парсинга
                                byte[] pkt = new byte[len];
                                for (int i = 0; i < len; i++) {
                                    pkt[i] = linkedListPacketReceived.removeFirst();
                                }
                                // Парсим: little-endian (low byte first) — без swap
                                int numWords = (len - 9) / 2;
                                List<Integer> values = new ArrayList<>();
                                for (int i = 0; i < numWords; i++) {
                                    int lo = pkt[8 + 2 * i] & 0xFF;
                                    int hi = pkt[8 + 2 * i + 1] & 0xFF;
                                    values.add((hi << 8) | lo);
                                }
                                recordDataCallback.accept(values);
                            } else {
                                break;
                            }
                        }
                        return;
                    }

                    // Normal mode
                    if (debugOutput) {
                        logger.debug("PortReader: {}", PacketHelper.convPacketToHexString(resp, " "));
                    }

                    if (countReceiveByte == 0 || packetReceived == null) {
                        packetReceived = resp;
                        countReceiveByte = resp.length;
                    } else {
                        byte[] newBuf = new byte[packetReceived.length + resp.length];
                        System.arraycopy(packetReceived, 0, newBuf, 0, packetReceived.length);
                        System.arraycopy(resp, 0, newBuf, packetReceived.length, resp.length);
                        packetReceived = newBuf;
                        countReceiveByte = countReceiveByte + resp.length;
                    }
                    // Resync: if first byte is not 0x55, search for it and discard preceding bytes
                    if (packetReceived != null && packetReceived[0] != PacketIcHelper.START_RESP_BYTE && packetReceived.length > 1) {
                        int startIdx = -1;
                        for (int i = 1; i < packetReceived.length; i++) {
                            if (packetReceived[i] == PacketIcHelper.START_RESP_BYTE) {
                                startIdx = i;
                                break;
                            }
                        }
                        if (startIdx > 0) {
                            int newLen = packetReceived.length - startIdx;
                            byte[] synced = new byte[newLen];
                            System.arraycopy(packetReceived, startIdx, synced, 0, newLen);
                            packetReceived = synced;
                            countReceiveByte = newLen;
                        } else if (packetReceived.length > 2048) {
                            // No sync byte found and buffer is too large, discard everything
                            packetReceived = null;
                            countReceiveByte = 0;
                        }
                    }
                    if (packetReceived != null && (packetReceived[0] == PacketIcHelper.START_RESP_BYTE) && (packetReceived.length > 3)) {
                        int len = PacketHelper.getUnsignedWord16bitInt(packetReceived[1], packetReceived[2]);
                        if (len == countReceiveByte) {
                            if (debugOutput) {
                                logger.debug("release (PortReader)");
                            }
                            // Save completed packet and reset accumulator for next packet
                            completedPacket = packetReceived;
                            packetReceived = null;
                            countReceiveByte = 0;
                            available.drainPermits();
                            available.release();

                        }
                    }

                }
            } catch (SerialPortException e) {
                throw new RuntimeException(e);
            }
        }
    }


    private class RecordPortListener implements SerialPortEventListener {


        public void serialEvent(SerialPortEvent event) {
            try {
                if (event.isRXCHAR() && event.getEventValue() > 0) {
                    byte[] resp = serialPort.readBytes();
                    logger.debug("RecordPortListener received byte: {}", resp.length);
                     if (debugOutput) {
                        logger.debug("RecordPortListener: {}", PacketHelper.convPacketToHexString(resp, " "));
                     }
                    if ((resp[0] == PacketIcHelper.START_RESP_BYTE) && (resp.length > 3)) {
                        int len = PacketHelper.getUnsignedWord16bitInt(resp[1], resp[2]);
                        if(linkedListPacketReceived.size() > len){
                            List<Byte> packet = new ArrayList<>();
                            int len2 = PacketHelper.getUnsignedWord16bitInt(linkedListPacketReceived.get(linkedListPacketReceived.size()-len+1),linkedListPacketReceived.get(linkedListPacketReceived.size()-len+2));
                            if(linkedListPacketReceived.get(linkedListPacketReceived.size()-len) == PacketIcHelper.START_RESP_BYTE && len2 == len) {
                                for (int i = linkedListPacketReceived.size() - len; i < linkedListPacketReceived.size(); i++) {
                                    packet.add(linkedListPacketReceived.get(i));
                                }
                                printValues(packet);
                            }
                            linkedListPacketReceived.clear();
                        }

                    }
                    for( int i = 0; i<resp.length; i++){
                        linkedListPacketReceived.addLast(resp[i]);
                    }
                    while(true){
                        if (linkedListPacketReceived.size() >3 && (linkedListPacketReceived.get(0) == PacketIcHelper.START_RESP_BYTE) && (linkedListPacketReceived.size() > 3)) {
                            int len = PacketHelper.getUnsignedWord16bitInt(linkedListPacketReceived.get(1), linkedListPacketReceived.get(2));
                            if(linkedListPacketReceived.size() >= len){
                                List<Byte> packet = new ArrayList<>();
                                for(int i = 0; i< len; i++ ){
                                    packet.add(linkedListPacketReceived.getFirst());
                                    linkedListPacketReceived.removeFirst();
                                }
                                logger.debug("Extract size packet: {}", packet.size());
                                printValues(packet);

                                logger.debug("Small packet: {}", PacketHelper.convPacketToHexString(packet, " "));
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }



                }
            } catch (SerialPortException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void printValues(List<Byte> list){

                List<Integer> respValues = PacketIcHelper.getValuesFromIcPacket(list);
                if (recordDataCallback != null) {
                    recordDataCallback.accept(respValues);
                    return;
                }
                LinkedList <Integer> memoryValues = new LinkedList<>(respValues);
                memoryValues.removeFirst();
                memoryValues.removeLast();

                String text = "";
                for(int v : memoryValues){
                    text += String.valueOf(v);
                    text += "\t";
                }
                text += "\n";
                try {
                    fileWriter.write(text);
                    fileWriter.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }



    }

    public int getDesiredNumBytes() {
        return desiredNumBytes;
    }

    public void setDesiredNumBytes(int desiredNumBytes) {
        this.desiredNumBytes = desiredNumBytes;
    }

    public void resetCountReceiveByte() {
        countReceiveByte = 0;
    }


    public UartModel() {
        String savedPort = WorkstationConfig.getProperty(ConfProp.COM_PORT);
        portName = (savedPort != null && !savedPort.isEmpty()) ? savedPort : null;
    }

    public String getPortName() {
        return this.portName;
    }

    public SerialPort getSerialPort() {
        return this.serialPort;
    }

    public boolean getIsOpen() {
        return this.isOpen;
    }


    public void setPortName(String port) {
        this.portName = port;
        WorkstationConfig.setProperty(ConfProp.COM_PORT, portName);
        WorkstationConfig.storeProperties();
        return;
    }

    public String[] getPortList() {
        String[] portNames = SerialPortList.getPortNames();
        return portNames;
    }


    public WaitReponseAction doWaitReponsePacket() {
        ensureConnected();
        try {
            if (debugOutput) {
                logger.debug("acquire (WaitReponseAction)");
                logger.debug("(WaitReponseAction) avaible permits:{}", available.availablePermits());
            }
            available.acquire();
            if (debugOutput) {
                logger.debug("(WaitReponseAction) avaible permits:{}", available.availablePermits());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        WaitReponseAction t = new WaitReponseAction();
        t.start();
        return t;
    }


    public EchangePacketAction doExchangePacket(byte[] packet) {
        ensureConnected();
        if(isRecording){
            disableRecording();
        }
        packetForSending = packet;
        try {
            if (debugOutput) {
                logger.debug("acquire (EchangePacketAction)");
                logger.debug("(EchangePacketAction) avaible permits:{}", available.availablePermits());
            }
            available.acquire();
            if (debugOutput) {
                logger.debug("(EchangePacketAction) avaible permits:{}", available.availablePermits());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        EchangePacketAction t = new EchangePacketAction();
        t.start();
        return t;
    }


    public void reOpenComPort() {
        logger.info("Reopening COM port...");
        try {
            if (serialPort != null) {
                try {
                    if (serialPort.isOpened()) {
                        serialPort.removeEventListener();
                        serialPort.closePort();
                    }
                } catch (SerialPortException e) {
                    logger.warn("Error closing port during reopen: {}", e.getMessage());
                    // Игнорируем ошибку при закрытии, порт может быть уже недоступен
                }
                serialPort = null;
            }
            openSerial();
        } catch (Exception e) {
            logger.error("Error reopening COM port: {}", e.getMessage());
            logger.error("Error reopening COM port", e);
        }
    }


    public String[] getSerialPortList() {
        String[] portNames = SerialPortList.getPortNames();
        for (int i = 0; i < portNames.length; i++) {
            logger.debug(portNames[i]);
        }
        return portNames;
    }


    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
    }


    public void setComPort(String portName) {
        WorkstationConfig.setProperty(ConfProp.COM_PORT, portName);
        WorkstationConfig.storeProperties();
        this.portName = portName;

    }

    public byte[] getResponse() {
        return completedPacket != null ? completedPacket : packetReceived;
    }

    /**
     * Ensures the serial port is open and valid before any transaction.
     * Opens the port if needed and waits for it to settle.
     * @return true if port is ready, false if port could not be opened
     */
    public boolean ensureConnected() {
        if (isPortValid()) {
            return true;
        }
        openSerial();
        if (!isPortValid()) {
            logger.warn("ensureConnected: failed to open port {}", portName);
            return false;
        }
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        return true;
    }


    public void openSerial() {
        try {
            // Если порт уже открыт и валиден, ничего не делаем
            if (serialPort != null && isPortValid()) {
                logger.debug("Serial port already opened and valid");
                return;
            }

            // Закрываем старый порт если существует
            if (serialPort != null) {
                try {
                    if (serialPort.isOpened()) {
                        serialPort.removeEventListener();
                        serialPort.closePort();
                    }
                } catch (SerialPortException e) {
                    logger.warn("Error closing old port: {}", e.getMessage());
                }
                serialPort = null;
            }

            // Открываем новый порт
            if (portName != null) {
                serialPort = new SerialPort(portName);
                serialPort.openPort();
                logger.info("Open serial port {} with {}bits/sec", portName, baudRate);

                serialPort.setParams(baudRate,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

                int mask = SerialPort.MASK_RXCHAR;
                serialPort.setEventsMask(mask);
                regularPortReader = new PortReader();
                recordPortListener = new RecordPortListener();
                serialPort.addEventListener(regularPortReader);

                isOpen = true;
            } else {
                logger.warn("First select COM port!");
            }

        } catch (SerialPortException e) {
            isOpen = false;
            serialPort = null;
        }
    }

    private class WaitResponseTask extends Thread {

        @Override
        public void run() {

            try {
                if (debugOutput) {
                    logger.debug("acquire (WaitResponseTask)");
                }
                available.acquire();
                if (debugOutput) {
                    logger.debug("(WaitResponseTask) available permits:{}", available.availablePermits());
                }
            } catch (InterruptedException e) {
                // Поток был прерван через interrupt(), это нормальное поведение
                logger.debug("WaitResponseTask interrupted");
                return;
            }
            if (debugOutput) {
                logger.debug("release (WaitResponseTask)");
            }
            available.drainPermits();
            available.release();

        }
    }


    public void resetPermits() {
        available.drainPermits();
        available.release();
        if (debugOutput) {
            logger.debug("(resetPermits) avaible permits:{}", available.availablePermits());
        }
    }

    public void purgeRxBuffer() {
        if (serialPort != null && serialPort.isOpened()) {
            try {
                serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
            } catch (SerialPortException e) {
                // ignore
            }
        }
        countReceiveByte = 0;
        packetReceived = null;
        completedPacket = null;
    }    public void enableRecording(String path){
        enableRecording();
        try {
            fileWriter = new FileWriter(path, true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void enableRecording(){
        try {
            isRecording = true;
            linkedListPacketReceived.clear();
            serialPort.removeEventListener();
            serialPort.readBytes();
            serialPort.addEventListener(recordPortListener);
        } catch (SerialPortException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRecordDataCallback(Consumer<List<Integer>> callback) {
        this.recordDataCallback = callback;
        linkedListPacketReceived.clear();
        countReceiveByte = 0;
        packetReceived = null;
    }

    public void clearRecordDataCallback() {
        this.recordDataCallback = null;
        linkedListPacketReceived.clear();
        countReceiveByte = 0;
        packetReceived = null;
        resetPermits();
    }

    public void closeFileWriter() {
        if (fileWriter != null) {
            try {
                fileWriter.close();
            } catch (IOException e) {
                logger.error("Error closing file writer", e);
            }
            fileWriter = null;
        }
    }

    public void disableRecording(){
        try {
            isRecording = false;
            serialPort.removeEventListener();
            serialPort.addEventListener(regularPortReader);
        } catch (SerialPortException e) {
            throw new RuntimeException(e);
        }
    }

    public class WaitReponseAction extends Thread {
        @Override
        public void run() {

            try {
                // Проверяем валидность порта перед ожиданием ответа
                if (!isPortValid()) {
                    logger.warn("Port not valid, attempting to reconnect...");
                    openSerial();
                }

                if (!isPortValid()) {
                    logger.error("Failed to open serial port");
                    available.drainPermits();
                    available.release();
                    return;
                }

                countReceiveByte = 0;
                packetReceived = null;
                completedPacket = null;
                WaitResponseTask waitTask = new WaitResponseTask();
                waitTask.start();
                waitTask.join(5000);
                if (available.availablePermits() == 0) {
                    logger.warn("No response from MCU!");
                    logger.debug("waitTask interrupted");
                    waitTask.interrupt();
                    logger.debug("release (EchangePacketAction)");
                    available.drainPermits();
                    available.release();
                } else {
                    logger.info("MCU->PC: {}", PacketHelper.convPacketToHexString(packetReceived, " "));
                }


            } catch (InterruptedException e) {
                logger.warn(" Interrupt! ", e);
            }

        }


    }


    // --- Safe methods for DMA Quad (no port reopening) ---

    public void safeWriteBytes(byte[] bytes) {
        if (serialPort != null) {
            try {
                logger.info("PC->MCU: {}", PacketHelper.convPacketToHexString(bytes, " "));
                serialPort.writeBytes(bytes);
            } catch (SerialPortException e) {
                logger.error("[SafeWrite] Write failed: {}", e.getMessage());
            }
        }
    }

    public class SafeWaitResponseAction extends Thread {
        @Override
        public void run() {
            try {
                countReceiveByte = 0;
                packetReceived = null;
                completedPacket = null;
                WaitResponseTask waitTask = new WaitResponseTask();
                waitTask.start();
                waitTask.join(5000);
                if (available.availablePermits() == 0) {
                    logger.warn("[SafeWait] No response from MCU (timeout)");
                    waitTask.interrupt();
                    available.drainPermits();
                    available.release();
                }
            } catch (InterruptedException e) {
                logger.debug("[SafeWait] Interrupted");
                available.drainPermits();
                available.release();
            }
        }
    }

    public SafeWaitResponseAction doSafeWaitResponse() {
        try {
            available.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        SafeWaitResponseAction t = new SafeWaitResponseAction();
        t.start();
        return t;
    }


    public class EchangePacketAction extends Thread {
        @Override
        public void run() {

            try {
                // Проверяем валидность порта перед обменом
                if (!isPortValid()) {
                    logger.warn("Port not valid, attempting to reconnect...");
                    openSerial();
                }

                if (!isPortValid()) {
                    logger.error("Failed to open serial port");
                    available.drainPermits();
                    available.release();
                    return;
                }

                logger.info("PC->MCU: {}", PacketHelper.convPacketToHexString(packetForSending, " "));
                packetReceived = null;
                completedPacket = null;
                countReceiveByte = 0;

                try {
                    serialPort.writeBytes(packetForSending);
                } catch (SerialPortException e) {
                    // Пытаемся переподключиться и повторить операцию
                    logger.warn("Write failed, attempting to reconnect: {}", e.getMessage());
                    openSerial();
                    if (isPortValid()) {
                        serialPort.writeBytes(packetForSending);
                        logger.info("Write successful after reconnect");
                    } else {
                        logger.error("Failed to reconnect to serial port");
                        available.drainPermits();
                        available.release();
                        return;
                    }
                }

                WaitResponseTask waitTask = new WaitResponseTask();
                waitTask.start();
                waitTask.join(1000);
                if (available.availablePermits() == 0) {
                    // Нет ответа от MCU - возможно USB был переподключен
                    // Пытаемся переподключиться и повторить операцию
                    logger.warn("No response from MCU! Attempting to reconnect...");
                    waitTask.interrupt();

                    // Принудительно переподключаемся
                    logger.info("Forcing port reopen...");
                    reOpenComPort();

                    // Повторяем операцию после переподключения
                    if (isPortValid()) {
                        logger.info("Retrying packet exchange after reconnect...");
                        packetReceived = null;
                        countReceiveByte = 0;
                        try {
                            serialPort.writeBytes(packetForSending);
                            WaitResponseTask retryWaitTask = new WaitResponseTask();
                            retryWaitTask.start();
                            retryWaitTask.join(1000);
                            if (available.availablePermits() == 0) {
                                logger.warn("Still no response after reconnect");
                                logger.debug("release (EchangePacketAction)");
                                available.drainPermits();
                                available.release();
                            } else if (completedPacket != null) {
                                logger.info("MCU->PC: {}", PacketHelper.convPacketToHexString(completedPacket, " "));
                            }
                        } catch (SerialPortException e2) {
                            logger.error("Write failed on retry: {}", e2.getMessage());
                            logger.debug("release (EchangePacketAction)");
                            available.drainPermits();
                            available.release();
                        }
                    } else {
                        logger.error("Failed to reconnect to serial port");
                        logger.debug("release (EchangePacketAction)");
                        available.drainPermits();
                        available.release();
                    }
                } else if (completedPacket != null) {
                    logger.info("MCU->PC: {}", PacketHelper.convPacketToHexString(completedPacket, " "));
                }


            } catch (InterruptedException e) {
                logger.warn(" Interrupt! ", e);
            } catch (SerialPortException e) {
                logger.error("SerialPortException: {}", e.getMessage());
                logger.error("SerialPortException in EchangePacketAction", e);
                available.drainPermits();
                available.release();
            }
        }


    }
}
