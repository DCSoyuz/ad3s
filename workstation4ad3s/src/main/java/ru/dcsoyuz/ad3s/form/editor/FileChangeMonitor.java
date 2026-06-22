package ru.dcsoyuz.ad3s.form.editor;

import javax.swing.Timer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileChangeMonitor {

    private static final int POLL_INTERVAL_MS = 2000;

    private static FileChangeMonitor instance;

    private Timer timer;
    private final Map<File, Long> trackedFiles = new HashMap<>();
    private FileChangeListener listener;

    public interface FileChangeListener {
        void onFileChangedExternally(File file);
    }

    private FileChangeMonitor() {}

    public static FileChangeMonitor getInstance() {
        if (instance == null) {
            instance = new FileChangeMonitor();
        }
        return instance;
    }

    public void setListener(FileChangeListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (timer != null && timer.isRunning()) return;
        timer = new Timer(POLL_INTERVAL_MS, e -> checkFiles());
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    public void registerFile(File file) {
        trackedFiles.put(file, file.lastModified());
    }

    public void unregisterFile(File file) {
        trackedFiles.remove(file);
    }

    public void markSavedByEditor(File file) {
        trackedFiles.put(file, file.lastModified());
    }

    private void checkFiles() {
        if (listener == null) return;

        List<File> changedFiles = new ArrayList<>();
        for (Map.Entry<File, Long> entry : trackedFiles.entrySet()) {
            File file = entry.getKey();
            long storedTimestamp = entry.getValue();
            long currentTimestamp = file.lastModified();
            if (currentTimestamp != storedTimestamp) {
                changedFiles.add(file);
            }
        }

        for (File file : changedFiles) {
            listener.onFileChangedExternally(file);
        }
    }
}
