package ru.dcsoyuz.ad3s.form.terminal;

import java.util.Arrays;
import java.util.LinkedList;

public class SelGraphDataContainer {

    private int addressLow;
    private boolean isChecked;

    private int numBits;
    private boolean isSigned;

    private String legend;

    private int numGraph;

    private int index;

    private boolean split;

    private   LinkedList<Integer>  fifo;

    private int sizeFifo;

    private Object obj = new Object();

    public SelGraphDataContainer(String legend, int addressLow, int numBits, boolean isSigned, boolean isChecked, int numGraph, int sizeFifo) {
        this.addressLow = addressLow;
        this.isChecked = isChecked;
        this.numBits = numBits;
        this.isSigned = isSigned;
        this.legend = legend;
        this.numGraph = numGraph;
        this.sizeFifo = sizeFifo;
        fifo = new LinkedList<Integer>();
        for(int i =0; i<= 9; i++){
            fifo.add(0);
        }
    }

    public SelGraphDataContainer(String legend, int addressLow, int numBits, boolean isSigned, boolean isChecked, int numGraph, int sizeFifo, boolean split) {
        this.addressLow = addressLow;
        this.isChecked = isChecked;
        this.numBits = numBits;
        this.isSigned = isSigned;
        this.legend = legend;
        this.numGraph = numGraph;
        this.sizeFifo = sizeFifo;
        this.split = split;
        fifo = new LinkedList<Integer>();
        for(int i =0; i<= 9; i++){
            fifo.add(0);
        }
    }


    public int getAddressLow() {
        return addressLow;
    }

    public void setAddressLow(int addressLow) {
        this.addressLow = addressLow;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getNumBits() {
        return numBits;
    }

    public boolean isSplit() {
        return split;
    }

    public void setSplit(boolean split) {
        this.split = split;
    }

    public void setNumBits(int numBits) {
        this.numBits = numBits;
    }

    public boolean isSigned() {
        return isSigned;
    }

    public void setSigned(boolean signed) {
        isSigned = signed;
    }

    public String getLegend() {
        return legend;
    }

    public void setLegend(String legend) {
        this.legend = legend;
    }

    public int getNumGraph() {
        return numGraph;
    }

    public void setNumGraph(int numGraph) {
        this.numGraph = numGraph;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void addValueToFifo(int value){
        synchronized (obj) {
            fifo.add(value);
            if (fifo.size() > sizeFifo) {
                fifo.removeFirst();
            }
        }
    }
    public double [] getFifoArray(){
        double[] array = new double[fifo.size()];
        Object[] array_int;
        synchronized (obj) {
            Object[] array_int1 = fifo.toArray();
            array_int =  Arrays.copyOf(array_int1, array_int1.length) ;
        }
        for (int i = 0; i < array_int.length; i++) {
            array[i] = (double) (Integer)array_int[i];
        }

        return array;
    }
}
