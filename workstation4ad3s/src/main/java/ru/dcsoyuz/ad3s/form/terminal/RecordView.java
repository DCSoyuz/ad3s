package ru.dcsoyuz.ad3s.form.terminal;

import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.model.uart.ic.McuCommand;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketIcHelper;
import ru.dcsoyuz.ad3s.model.uart.ic.PacketToIc;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Record View - вкладка для записи потока данных с ESP32 в файл.
 * 9 пар адресов (четный + следующий нечетный), опрос каждые 1 мс.
 */
public class RecordView extends JPanel {

    private static final int NUM_PAIRS = 9;
    private static final int FIELD_COLS = 5;

    private static final int[] DEFAULT_EVEN_ADDRESSES = {
            16, 48, 162, 674, 676, 678, 680, 682, 684
    };

    private final JButton recButton;
    private final JTextField filePathField;
    private final JButton browseButton;
    private final JTextField[] evenFields = new JTextField[NUM_PAIRS];
    private final JTextField[] oddFields = new JTextField[NUM_PAIRS];
    private final JCheckBox[] cb14b14 = new JCheckBox[NUM_PAIRS];
    private final JLabel statusLabel;

    private boolean recording = false;
    private Writer recordFileWriter;
    private volatile long sampleCount = 0;

    public RecordView() {
        setLayout(new BorderLayout(10, 10));

        // --- Верхняя панель: кнопка Rec/Stop + путь к файлу ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 5, 10));

        recButton = new JButton("Rec");
        recButton.setPreferredSize(new Dimension(100, 30));
        recButton.setFont(recButton.getFont().deriveFont(Font.BOLD));
        recButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (recording) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        });
        topPanel.add(recButton);

        filePathField = new JTextField(25);
        filePathField.setMinimumSize(new Dimension(150, 25));
        filePathField.setPreferredSize(new Dimension(250, 25));
        filePathField.setText(getDefaultRecordPath());
        filePathField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { savePathToConfig(); }
            @Override
            public void removeUpdate(DocumentEvent e) { savePathToConfig(); }
            @Override
            public void changedUpdate(DocumentEvent e) { savePathToConfig(); }
        });
        topPanel.add(filePathField);

        browseButton = new JButton("...");
        browseButton.setPreferredSize(new Dimension(40, 25));
        browseButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                browseFile();
            }
        });
        topPanel.add(browseButton);

        add(topPanel, BorderLayout.NORTH);

        // --- Центральная панель: таблица адресов ---
        JPanel addressPanel = new JPanel(new GridBagLayout());
        addressPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Addresses (even / even+1)",
                TitledBorder.LEFT, TitledBorder.TOP));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Заголовок
        gbc.gridx = 0;
        gbc.gridy = 0;
        addressPanel.add(new JLabel("#"), gbc);

        gbc.gridx = 1;
        addressPanel.add(new JLabel("Addr (even)"), gbc);

        gbc.gridx = 2;
        addressPanel.add(new JLabel("Addr (odd)"), gbc);

        gbc.gridx = 3;
        addressPanel.add(new JLabel("14b14"), gbc);

        // Строки с адресами
        for (int i = 0; i < NUM_PAIRS; i++) {
            int row = i + 1;
            int defaultAddr = DEFAULT_EVEN_ADDRESSES[i];

            // Номер пары
            gbc.gridx = 0;
            gbc.gridy = row;
            addressPanel.add(new JLabel(String.valueOf(i + 1)), gbc);

            // Поле четного адреса (редактируемое)
            JTextField evenField = new JTextField(String.valueOf(defaultAddr), FIELD_COLS);
            evenField.setMinimumSize(new Dimension(60, 25));
            evenField.setPreferredSize(new Dimension(70, 25));
            evenFields[i] = evenField;
            gbc.gridx = 1;
            addressPanel.add(evenField, gbc);

            // Поле нечетного адреса (автоматическое = even + 1)
            JTextField oddField = new JTextField(String.valueOf(defaultAddr + 1), FIELD_COLS);
            oddField.setEditable(false);
            oddField.setMinimumSize(new Dimension(60, 25));
            oddField.setPreferredSize(new Dimension(70, 25));
            oddField.setEnabled(false);
            oddFields[i] = oddField;
            gbc.gridx = 2;
            addressPanel.add(oddField, gbc);

            // Checkbox 14b14
            boolean default14b14 = is14b14PropertySet(i);
            JCheckBox cb = new JCheckBox("", default14b14);
            cb14b14[i] = cb;
            final int cbIndex = i;
            cb.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    WorkstationConfig.setProperty("RECORD_14B14_" + cbIndex, String.valueOf(cb.isSelected()));
                    WorkstationConfig.storeProperties();
                }
            });
            gbc.gridx = 3;
            addressPanel.add(cb, gbc);

            // Слушатель: при изменении четного адреса обновляем нечетный
            final int index = i;
            evenField.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) { updateOddField(index); }
                @Override
                public void removeUpdate(DocumentEvent e) { updateOddField(index); }
                @Override
                public void changedUpdate(DocumentEvent e) { updateOddField(index); }
            });
        }

        add(addressPanel, BorderLayout.CENTER);

        // --- Нижняя панель: статус ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        statusLabel = new JLabel("Ready");
        bottomPanel.add(statusLabel);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void updateOddField(int index) {
        try {
            String text = evenFields[index].getText().trim();
            if (!text.isEmpty()) {
                int evenAddr = Integer.parseInt(text);
                oddFields[index].setText(String.valueOf(evenAddr + 1));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void browseFile() {
        String currentPath = filePathField.getText().trim();
        JFileChooser fileChooser = new JFileChooser(currentPath.isEmpty() ? getDefaultRecordPath() : currentPath);
        fileChooser.setDialogTitle("Save recording");
        if (!currentPath.isEmpty()) {
            fileChooser.setSelectedFile(new File(currentPath));
        }
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            filePathField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private static String getDefaultRecordPath() {
        String path = WorkstationConfig.getProperty(ConfProp.FILE_PATH_RECORD_TXT);
        if (path != null && !path.isEmpty()) {
            File f = new File(path);
            if (!f.isAbsolute()) {
                f = new File(getJarDirectory(), path);
            }
            return f.getAbsolutePath();
        }
        return new File(getJarDirectory(), "rec1.txt").getAbsolutePath();
    }

    private static File getJarDirectory() {
        try {
            String jarPath = RecordView.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI().getPath();
            File jarFile = new File(jarPath);
            if (jarFile.isFile()) {
                return jarFile.getParentFile();
            }
            return jarFile;
        } catch (Exception e) {
            return new File(System.getProperty("user.dir"));
        }
    }

    private static boolean is14b14PropertySet(int index) {
        String val = WorkstationConfig.getProperty("RECORD_14B14_" + index);
        return val != null ? Boolean.parseBoolean(val) : false;
    }

    private void savePathToConfig() {
        SwingUtilities.invokeLater(() -> {
            String path = filePathField.getText().trim();
            if (!path.isEmpty()) {
                WorkstationConfig.setProperty(ConfProp.FILE_PATH_RECORD_TXT, path);
                WorkstationConfig.storeProperties();
            }
        });
    }

    /**
     * Возвращает список всех адресов для записи (четный + нечетный для каждой пары).
     */
    private List<Integer> buildAddressList() {
        List<Integer> addresses = new ArrayList<>();
        for (int i = 0; i < NUM_PAIRS; i++) {
            int evenAddr = Integer.parseInt(evenFields[i].getText().trim());
            addresses.add(evenAddr);
            addresses.add(evenAddr + 1);
        }
        return addresses;
    }

    private void startRecording() {
        String filePath = filePathField.getText().trim();
        if (filePath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select a file first", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Валидация адресов
        int[] evenAddrs = getEvenAddresses();
        if (evenAddrs == null) {
            JOptionPane.showMessageDialog(this, "Invalid address in one of the fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<Integer> addressList = buildAddressList();

        try {
            // Открываем файл для записи
            recordFileWriter = new FileWriter(filePath);
            sampleCount = 0;

            // Формируем и отправляем команду START_RECORD (PortReader в нормальном режиме)
            PacketToIc packet = new PacketToIc(
                    McuCommand.START_RECORD,
                    addressList.get(0),
                    addressList.size(),
                    addressList
            );
            byte[] bytes = PacketIcHelper.getBytesFromPacketToIc(packet);
            Model.getUartModel().doWriteBytes(bytes);

            // Даём ESP32 время обработать и начать циклическую отправку
            Thread.sleep(50);

            // Устанавливаем callback — PortReader переключается в режим записи
            // (без смены listener, COM-порт продолжает читаться непрерывно)
            Model.getUartModel().setRecordDataCallback(values -> {
                if (values.size() < 2 * NUM_PAIRS) return; // пропускаем ACK-пакет (без данных)
                StringBuilder sb = new StringBuilder();
                for (int p = 0; p < NUM_PAIRS; p++) {
                    int even = values.get(2 * p) & 0xFFFF;
                    int odd  = values.get(2 * p + 1) & 0xFFFF;
                    int result;
                    if (cb14b14[p].isSelected()) {
                        // 14b14: 14 старших бит + 14 младших бит, 2 старших бита каждой части выкинуть
                        result = ((odd & 0x3FFF) << 14) | (even & 0x3FFF);
                    } else {
                        // по умолчанию: 12 старших бит + 16 младших бит
                        result = ((odd & 0xFFF) << 16) | even;
                    }
                    if (p > 0) sb.append("\t");
                    sb.append(String.format("%07X", result));
                }
                sb.append("\n");
                try {
                    recordFileWriter.write(sb.toString());
                    recordFileWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                sampleCount++;
                if (sampleCount % 100 == 0) {
                    SwingUtilities.invokeLater(() ->
                            statusLabel.setText("Recording... " + sampleCount + " samples"));
                }
            });

            recording = true;
            SwingUtilities.invokeLater(() -> {
                recButton.setText("Stop");
                for (JTextField f : evenFields) f.setEnabled(false);
                filePathField.setEnabled(false);
                browseButton.setEnabled(false);
                statusLabel.setText("Recording... 0 samples");
            });

            System.out.println("Recording started: " + addressList.size() + " addresses to " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to start recording: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            cleanupRecording();
        }
    }

    private void stopRecording() {
        // Отправляем команду STOP_RECORD (fire-and-forget)
        try {
            PacketToIc stopPacket = new PacketToIc(McuCommand.STOP_RECORD, 0, 0, null);
            byte[] stopBytes = PacketIcHelper.getBytesFromPacketToIc(stopPacket);
            Model.getUartModel().doWriteBytes(stopBytes);

            // Даём ESP32 время остановить циклическую задачу
            Thread.sleep(100);
        } catch (Exception e) {
            e.printStackTrace();
        }

        cleanupRecording();

        System.out.println("Recording stopped: " + sampleCount + " samples written");
        SwingUtilities.invokeLater(() ->
                statusLabel.setText("Done. " + sampleCount + " samples written"));
    }

    private void cleanupRecording() {
        recording = false;
        // Сбрасываем callback — PortReader возвращается в нормальный режим
        Model.getUartModel().clearRecordDataCallback();
        if (recordFileWriter != null) {
            try {
                recordFileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            recordFileWriter = null;
        }
        SwingUtilities.invokeLater(() -> {
            recButton.setText("Rec");
            for (JTextField f : evenFields) f.setEnabled(true);
            filePathField.setEnabled(true);
            browseButton.setEnabled(true);
        });
    }

    /**
     * Возвращает список четных адресов, введённых пользователем.
     *
     * @return массив чётных адресов, или null если любой адрес некорректен
     */
    public int[] getEvenAddresses() {
        int[] addresses = new int[NUM_PAIRS];
        for (int i = 0; i < NUM_PAIRS; i++) {
            try {
                addresses[i] = Integer.parseInt(evenFields[i].getText().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return addresses;
    }

    public boolean isRecording() {
        return recording;
    }

    public void cleanup() {
        if (recording) {
            stopRecording();
        }
    }
}
