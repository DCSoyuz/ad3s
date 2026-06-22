package ru.dcsoyuz.ad3s.model.fpga.registers;

public interface IAllRegAddr {

    public IReg getReg() ;

    public RegType getRegType() ;

    public int getAddress() ;

    public String name();

    public default String getDisplayName() { return getReg().getDisplayName(); }


    public static <T extends Enum<T> & IAllRegAddr> T getValueOf(Class<T> enumClass, Integer addr) {
        for (T e : enumClass.getEnumConstants()) {
            if (e.getAddress() == addr) {
                return e;
            }
        }
        return null;
    }


}
