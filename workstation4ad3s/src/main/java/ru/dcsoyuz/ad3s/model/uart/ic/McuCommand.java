package ru.dcsoyuz.ad3s.model.uart.ic;

public enum McuCommand {

    READ ((byte)0x01),
    WRITE((byte)0x02),
    START_READ_CYCLIC((byte)0x03),
    STOP_READ_CYCLIC((byte)0x04),
    READ_RANDOM ((byte)0x05),
    WRITE_RANDOM ((byte)0x06),
    PROG_BOTP ((byte)0x07),
    READ_BOTP ((byte)0x08),
    PROG_UOTP ((byte)0x09),
    PROG_FOTP_MEM ((byte)0x0A),
    SET_LED ((byte)0x0B),
    READ_WITH_INDUCTOSYN ((byte)0x0C),
    SET_MASTER_FPGA_MODE ((byte)0x0D),
    READ_HANDTAP((byte)0x0E),
    SET_STNDBY((byte)0x0F),

    SET_NRESET((byte)0x10),

    SET_DMA_QUAD((byte)0x11),
    SET_VC((byte)0x12),

    SET_SDI((byte)0x13),

    SET_DMA_SINGLE((byte)0x14),

    SET_VPP9V((byte)0x15),

    WRITE_BOTP((byte)0x16),

    READ_CPU_REGS((byte)0x17),

    SET_SPI_SPEED((byte)0x18),

    ENC_ON((byte)0x19),
    ENC_OFF((byte)0x1A),
    READ_ENCODERS((byte)0x1B),
    START_RECORD((byte)0x1C),
    STOP_RECORD((byte)0x1D),
    WRITE_INIT_FLASH((byte)0x1E),
    READ_INIT_FLASH((byte)0x1F),
    ERASE_INIT_FLASH((byte)0x20),
    RUN_INIT_FLASH((byte)0x21),
    SET_ELEC_OFFSET((byte)0x22);

    private byte startByte;

    McuCommand(byte startByte) {
        this.startByte = startByte;
    }

    public byte getCommandByte() {
        return startByte;
    }
}
