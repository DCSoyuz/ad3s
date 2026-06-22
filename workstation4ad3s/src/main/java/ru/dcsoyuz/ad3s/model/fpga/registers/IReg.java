package ru.dcsoyuz.ad3s.model.fpga.registers;

import java.util.List;

public interface IReg {

    public RegValueType getValueType();

    public Integer getDefaultValue() ;

    public int getLocalAddr();

    public boolean isOnlyRead() ;

    public String getDescription();

    public String name();

    public String getDisplayName();

    public List<IRegField> getFields();


}
