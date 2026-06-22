package ru.dcsoyuz.ad3s.model.editor;

/**
 * Created by yuri.filatov on 03.08.2016.
 */
public class OutputCodes {

    private String textClearAssemblerCodes;
    private String textHexCodes;
    private String textBinaryCodes;

    public OutputCodes(String textClearAssemblerCodes, String textHexCodes, String textBinaryCodes) {
        this.textClearAssemblerCodes = textClearAssemblerCodes;
        this.textHexCodes = textHexCodes;
        this.textBinaryCodes = textBinaryCodes;
    }

    public String getTextClearAssemblerCodes() {
        return textClearAssemblerCodes;
    }

    public String getTextHexCodes() {
        return textHexCodes;
    }

    public String getTextBinaryCodes() {
        return textBinaryCodes;
    }
}
