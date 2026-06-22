package ru.dcsoyuz.ad3s.form.editor;

import ru.dcsoyuz.ad3s.form.Icons;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AboutDialog extends JDialog {
    private static final long serialVersionUID = 1752605222930917064L;

    private final String appName = "Workstation4ad3s";
    private final String appVersion = loadVersion();
    private final String appDescription = "Settings editor for 5400TP065A-022";
    private final String appAuthor = "Created by Yuriy Filatov";
    private final String appCompany = "";

    private static String loadVersion() {
        try (InputStream is = AboutDialog.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                return "v" + props.getProperty("app.version", "?");
            }
        } catch (IOException ignored) {}
        return "v?";
    }

    public AboutDialog() {
        setTitle("About " + appName);
        setResizable(false);
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUI();
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(new Color(0x1A2A3A));
        root.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // --- Top: icon + app name ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        topPanel.setOpaque(false);

        // Icon scaled to 96x96
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(Icons.mainIcon);
        // Scale down
        Image scaled = Icons.mainIcon.getImage().getScaledInstance(96, 96, Image.SCALE_SMOOTH);
        iconLabel.setIcon(new ImageIcon(scaled));
        topPanel.add(iconLabel);

        // Right side: name + version
        JPanel namePanel = new JPanel();
        namePanel.setLayout(new BoxLayout(namePanel, BoxLayout.Y_AXIS));
        namePanel.setOpaque(false);

        JLabel nameLabel = new JLabel(appName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        nameLabel.setForeground(new Color(0xA8E0FF));
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel verLabel = new JLabel(appVersion);
        verLabel.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        verLabel.setForeground(new Color(0x87CEEB));
        verLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel descLabel = new JLabel(appDescription);
        descLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        descLabel.setForeground(new Color(0x6B7A8A));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        namePanel.add(nameLabel);
        namePanel.add(Box.createVerticalStrut(2));
        namePanel.add(verLabel);
        namePanel.add(Box.createVerticalStrut(6));
        namePanel.add(descLabel);
        topPanel.add(namePanel);

        root.add(topPanel, BorderLayout.NORTH);

        // --- Center: decorative line + info ---
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(16, 0, 8, 0));

        // Decorative separator - gradient line
        JPanel separator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                GradientPaint gp = new GradientPaint(0, 0, new Color(0x4A90D9, true),
                        w / 2f, 0, new Color(0xA8E0FF, true),
                        true);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, 2);
                g2.dispose();
            }
        };
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 2));
        separator.setPreferredSize(new Dimension(300, 2));
        separator.setOpaque(false);
        centerPanel.add(separator);

        centerPanel.add(Box.createVerticalStrut(12));

        // Info lines
        JLabel authorLabel = new JLabel(appAuthor);
        authorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        authorLabel.setForeground(new Color(0x6B7A8A));
        authorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(authorLabel);

        centerPanel.add(Box.createVerticalStrut(8));

        // Chip info
        JLabel chipLabel = new JLabel("5400TP065A-022 — Resolver-to-Digital Converter");
        chipLabel.setFont(new Font("Consolas", Font.PLAIN, 11));
        chipLabel.setForeground(new Color(0x4A90D9));
        chipLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(chipLabel);

        centerPanel.add(Box.createVerticalStrut(12));

        // Animated "wave" decoration
        JPanel wavePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                // Draw sine wave
                g2.setColor(new Color(0x4A, 0x90, 0xD9, 80));
                g2.setStroke(new BasicStroke(1.5f));
                for (int wave = 0; wave < 3; wave++) {
                    int alpha = 80 - wave * 25;
                    g2.setColor(new Color(0x4A, 0x90, 0xD9, Math.max(alpha, 20)));
                    int yOff = 10 + wave * 4;
                    double phase = wave * 0.8;
                    g2.drawPolyline(
                            getXPoints(w, phase),
                            getYPoints(w, h, yOff, phase),
                            w
                    );
                }
                g2.dispose();
            }

            private int[] getXPoints(int w, double phase) {
                int[] xs = new int[w];
                for (int i = 0; i < w; i++) xs[i] = i;
                return xs;
            }

            private int[] getYPoints(int w, int h, int yOff, double phase) {
                int[] ys = new int[w];
                for (int i = 0; i < w; i++) {
                    double t = (i / (double) w) * Math.PI * 4 + phase;
                    ys[i] = yOff + (int) (Math.sin(t) * 8);
                }
                return ys;
            }
        };
        wavePanel.setPreferredSize(new Dimension(300, 30));
        wavePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        wavePanel.setOpaque(false);
        centerPanel.add(wavePanel);

        root.add(centerPanel, BorderLayout.CENTER);

        // --- Bottom: Close button ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bottomPanel.setOpaque(false);

        JButton closeBtn = new JButton("Close");
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        closeBtn.setForeground(new Color(0x1A2A3A));
        closeBtn.setBackground(new Color(0xA8E0FF));
        closeBtn.setFocusPainted(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setPreferredSize(new Dimension(100, 30));
        closeBtn.addActionListener(e -> dispose());
        bottomPanel.add(closeBtn);

        root.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }
}
