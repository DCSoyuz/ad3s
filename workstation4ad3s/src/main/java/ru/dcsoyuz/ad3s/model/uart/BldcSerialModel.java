package ru.dcsoyuz.ad3s.model.uart;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;

public class BldcSerialModel {

    private static final Logger logger = LoggerFactory.getLogger(BldcSerialModel.class);

    private static final String CONFIG_KEY = "BLDC_COM_PORT";
    private static final int BAUD_RATE = 115200;

    private String portName;
    private SerialPort serialPort;
    private volatile boolean isOpen = false;

    public BldcSerialModel() {
        String saved = WorkstationConfig.getProperty(CONFIG_KEY);
        if (saved != null && !saved.isEmpty()) {
            portName = saved;
        }
    }

    public void openSerial() {
        try {
            if (serialPort != null && serialPort.isOpened()) return;
            if (serialPort != null) {
                try { serialPort.closePort(); } catch (Exception ignored) {}
                serialPort = null;
            }
            if (portName == null || portName.isEmpty()) {
                logger.warn("BLDC: No port selected");
                return;
            }
            serialPort = new SerialPort(portName);
            serialPort.openPort();
            serialPort.setParams(BAUD_RATE, SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setDTR(false);
            serialPort.setRTS(false);
            isOpen = true;
            logger.info("BLDC: Opened {}", portName);
        } catch (SerialPortException e) {
            logger.error("BLDC: Error: {}", e.getMessage());
            isOpen = false;
            serialPort = null;
        }
    }

    public void closeSerial() {
        if (serialPort != null) {
            try { if (serialPort.isOpened()) serialPort.closePort(); }
            catch (SerialPortException e) { logger.error("Error closing BLDC serial port", e); }
            serialPort = null;
            isOpen = false;
        }
    }

    public void sendCommand(String command) {
        new Thread(() -> {
            if (serialPort == null || !serialPort.isOpened()) openSerial();
            if (serialPort == null || !serialPort.isOpened()) {
                logger.warn("BLDC: Port not open");
                return;
            }
            try {
                String payload = command + "\n";
                serialPort.writeBytes(payload.getBytes());
                logger.debug("BLDC TX: {}", payload.trim());
            } catch (SerialPortException e) {
                logger.error("BLDC: Write error: {}", e.getMessage());
            }
        }).start();
    }

    public void sendSpeed(int speed) { sendCommand("SPEED:" + speed); }
    public void sendHallMode() { sendCommand("MODE:HALL"); }

    public String getPortName() { return portName; }
    public void setPortName(String port) {
        portName = port;
        WorkstationConfig.setProperty(CONFIG_KEY, port);
        WorkstationConfig.storeProperties();
    }
    public boolean isOpen() { return isOpen; }
    public String[] getPortList() { return SerialPortList.getPortNames(); }
    public void reOpen() { closeSerial(); openSerial(); }
}
