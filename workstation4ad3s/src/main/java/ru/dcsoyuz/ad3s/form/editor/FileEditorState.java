package ru.dcsoyuz.ad3s.form.editor;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.charset.Charset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileEditorState {
	private static final Logger logger = LoggerFactory.getLogger(FileEditorState.class);

    private final File file;
    private String savingUpdate = "";
    private String savedTextForSymbol = "";
    private boolean tableIsChanged = false;
    private boolean savedSuccessfully = false;
    private Charset currentCharset = null;
    private int caretPosition = 0;

    private final UndoManager undo = new UndoManager();
    private final UndoAction undoAction;
    private final RedoAction redoAction;
    private final UndoableEditListener undoHandler;

    public FileEditorState(File file, JLabel textStatus) {
        this.file = file;
        this.undoAction = new UndoAction();
        this.redoAction = new RedoAction();
        this.undoHandler = new UndoHandler(textStatus);
    }

    public File getFile() { return file; }

    // Dirty tracking
    public boolean isDirty(String currentText) {
        if (file.getName().contains("txt")) {
            return tableIsChanged;
        }
        return !currentText.equals(savingUpdate);
    }

    public void markSaved(String currentText) {
        savingUpdate = currentText;
        savedTextForSymbol = currentText;
        tableIsChanged = false;
    }

    public void setSavingUpdate(String text) { this.savingUpdate = text; }
    public String getSavingUpdate() { return savingUpdate; }

    public void setSavedTextForSymbol(String text) { this.savedTextForSymbol = text; }
    public String getSavedTextForSymbol() { return savedTextForSymbol; }

    public void setTableIsChanged(boolean changed) { this.tableIsChanged = changed; }
    public boolean isTableChanged() { return tableIsChanged; }

    public void setCurrentCharset(Charset charset) { this.currentCharset = charset; }
    public Charset getCurrentCharset() { return currentCharset; }

    public int getCaretPosition() { return caretPosition; }
    public void setCaretPosition(int pos) { this.caretPosition = pos; }

    // Undo system
    public UndoManager getUndo() { return undo; }
    public UndoAction getUndoAction() { return undoAction; }
    public RedoAction getRedoAction() { return redoAction; }
    public UndoableEditListener getUndoHandler() { return undoHandler; }

    public void clearUndo() {
        undo.discardAllEdits();
        undoAction.update();
        redoAction.update();
    }

    // Inner classes for undo/redo

    class UndoHandler implements UndoableEditListener {
        private final JLabel textStatus;

        UndoHandler(JLabel textStatus) {
            this.textStatus = textStatus;
        }

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            if (textStatus != null) textStatus.setText("editing...");
            undo.addEdit(e.getEdit());
            undoAction.update();
            redoAction.update();
        }
    }

    class UndoAction extends AbstractAction {
        UndoAction() {
            super("Undo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            } catch (CannotUndoException ex) {
                logger.error("Error in actionPerformed", ex);
            }
            update();
            redoAction.update();
        }

        void update() {
            setEnabled(undo.canUndo());
            putValue(Action.NAME, "Undo");
        }
    }

    class RedoAction extends AbstractAction {
        RedoAction() {
            super("Redo");
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
            } catch (CannotRedoException ex) {
                logger.error("Error in actionPerformed", ex);
            }
            update();
            undoAction.update();
        }

        void update() {
            setEnabled(undo.canRedo());
            putValue(Action.NAME, "Redo");
        }
    }
}
