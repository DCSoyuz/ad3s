package ru.dcsoyuz.ad3s.model.fpga.registers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yuri.filatov on 21.01.2024.
 */
public class RegBitHolder {

    List<IRegField> regFieldList;

    Map<Integer, IRegField> mapFields;

    public RegBitHolder(List<IRegField> regFieldList) {
        this.regFieldList = regFieldList;
        mapFields = new HashMap<>();
        for(IRegField regField : regFieldList){
            for(int i = 0; i < regField.getNumBits(); i++){
                mapFields.put(regField.getLsb() + i , regField);
            }
        }
    }

    public IRegField getField (int index){
        return mapFields.get(index);
    }

    public RegFieldTableData getNumBitListHeader (){
        RegFieldTableData tableData = new RegFieldTableData();

        for(int i = 15; i>= 0; i=i-1){
            IRegField field =  mapFields.get(i);
            if(field != null) {
                i = i - field.getNumBits() + 1;
                if (field.getNumBits() == 1) {

                    tableData.addRecord(Integer.toString(field.getLsb()), field);
                } else {
                    tableData.addRecord(String.format("%d..%d", field.getMsb(), field.getLsb()), field);
                }
            }else {
                for( int k = i-1; k >= 0; k=k-1){
                    IRegField field2 = mapFields.get(k);
                    if(field2 != null){
                        if(i-k == 1){
                            tableData.addRecord(Integer.toString(i), null);
                        }else {
                            tableData.addRecord(String.format("%d..%d", i, k + 1), null);
                        }
                        i = i - (i - k ) + 1;
                        break;
                    }
                }
            }
        }
        return tableData;
    }




}
