package ru.dcsoyuz.ad3s.config;

public enum GraphViewProp {


    SPLIT,
    ISCHEKED,
    NUMBITS,
    ADDRESSLOW,
    LEGEND,
    ISSIGNED,
    NUMGRAPH;






    public  String getKey(int index){
        return "GRAPHVIEW_" + String.valueOf(index) + "_"+ this.name();
    }



}
