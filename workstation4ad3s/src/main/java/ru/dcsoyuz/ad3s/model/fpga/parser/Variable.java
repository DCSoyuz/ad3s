package ru.dcsoyuz.ad3s.model.fpga.parser;

/**
 * Created by yuri.filatov on 21.07.2016.
 */
public class Variable {

    private String value;
    private Integer index;
    private String comment;

    public Variable(String value, Integer index, String comment) {

        this.value = value;
        this.index = index;
        this.comment = comment;
    }


    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
