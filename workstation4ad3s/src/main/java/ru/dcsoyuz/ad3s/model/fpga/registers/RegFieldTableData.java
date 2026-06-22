package ru.dcsoyuz.ad3s.model.fpga.registers;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by yuri.filatov on 21.01.2024.
 */
public class RegFieldTableData {

    List<String> tableHeader;
    List<IRegField>tableRegField;

    public RegFieldTableData() {
        tableHeader = new ArrayList<>();
        tableRegField = new ArrayList<>();
    }

    public void addRecord(String head, IRegField field){
        tableHeader.add(head);
        tableRegField.add(field);
    }

    public List<String> getTableHeader() {
        return tableHeader;
    }

    public List<IRegField> getTableRegField() {
        return tableRegField;
    }
}
