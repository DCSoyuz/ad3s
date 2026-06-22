package ru.dcsoyuz.ad3s.form;

import javax.swing.*;
import java.io.OutputStream;
import ru.dcsoyuz.ad3s.form.editor.TextLineNumber;
import java.util.ArrayDeque;

public class CustomOutputStream extends OutputStream {
    private static final int MAX_LINES = 1000;

    private final JTextArea textArea;
    private final ArrayDeque<String> lines = new ArrayDeque<>(MAX_LINES + 64);
    private final StringBuilder buffer = new StringBuilder(256);
    private final Timer updateTimer;
    private long lineCounter = 0;
    private TextLineNumber lineNumbers;

    public CustomOutputStream(JTextArea textArea) {
        this.textArea = textArea;
        this.updateTimer = new Timer(200, e -> refresh());
        this.updateTimer.setRepeats(false);
    }

    public void setLineNumbers(TextLineNumber lineNumbers) {
        this.lineNumbers = lineNumbers;
    }

    public void clear() {
        synchronized (lines) {
            lines.clear();
            lineCounter = 0;
        }
        buffer.setLength(0);
        textArea.setText("");
        if (lineNumbers != null) {
            lineNumbers.setLineNumberOffset(1);
        }
    }

    @Override
    public void write(int b) {
        if (b == '\n') {
            addLine();
        } else {
            buffer.append((char) b);
        }
    }

    @Override
    public void write(byte[] b, int off, int len) {
        for (int i = off; i < off + len; i++) {
            if (b[i] == '\n') {
                addLine();
            } else {
                buffer.append((char) b[i]);
            }
        }
    }

    @Override
    public void flush() {
        if (buffer.length() > 0) {
            addLine();
        }
    }

    private void addLine() {
        String line = buffer.toString();
        buffer.setLength(0);
        synchronized (lines) {
            lines.addLast(line);
            while (lines.size() > MAX_LINES) {
                lines.pollFirst();
            }
            lineCounter++;
        }
        if (!updateTimer.isRunning()) {
            updateTimer.restart();
        }
    }

    private void refresh() {
        String text;
        long firstLineNum;
        synchronized (lines) {
            StringBuilder sb = new StringBuilder(lines.size() * 80);
            for (String l : lines) {
                sb.append(l).append('\n');
            }
            text = sb.toString();
            firstLineNum = lineCounter - lines.size() + 1;
        }
        textArea.setText(text);
        textArea.setCaretPosition(textArea.getDocument().getLength());
        if (lineNumbers != null) {
            lineNumbers.setLineNumberOffset(firstLineNum);
        }
    }
}
