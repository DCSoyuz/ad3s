package ru.dcsoyuz.ad3s.model.fpga.registers;


public interface IRegField {


    public int getLsb() ;

    public int getMsb() ;
    public int getMask();

    public int getNumBits();

    public int getFieldValueFromRegValue(int regValue);

    public int getRange();

    public int getDefaultValue();

    public String getDescription() ;

    public String name();

    public default String getDisplayName() { return ((Enum<?>) this).name(); }

    public int getAlignedDefaultValue();

    public FieldValueType getFieldValueType();

    public default int[] getValidValues() { return null; }


}
