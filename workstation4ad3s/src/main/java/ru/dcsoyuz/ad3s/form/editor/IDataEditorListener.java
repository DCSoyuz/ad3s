package ru.dcsoyuz.ad3s.form.editor;

/**
 * Created by yuri.filatov on 01.08.2016.
 */
public interface IDataEditorListener {


    void updateOutputCodes();


    String getCurrentText();

    void updateColors();

    void saveFile();


}
