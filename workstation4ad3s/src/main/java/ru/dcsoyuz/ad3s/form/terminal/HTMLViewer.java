package ru.dcsoyuz.ad3s.form.terminal;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import ru.dcsoyuz.ad3s.config.ConfProp;
import ru.dcsoyuz.ad3s.config.WorkstationConfig;
import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.fpga.registers.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

public class HTMLViewer extends JPanel {

    private final JFXPanel jfxPanel = new JFXPanel();
    private WebEngine webEngine;
    private File htmlFile;
    private JTextField searchField;
    private JLabel matchLabel;

    public HTMLViewer() {
        setLayout(new BorderLayout());
        setBackground(new Color(0xD6EAF8));
        add(createToolBar(), BorderLayout.NORTH);
        add(jfxPanel, BorderLayout.CENTER);

        Platform.runLater(() -> {
            WebView webView = new WebView();
            webEngine = webView.getEngine();
            jfxPanel.setScene(new Scene(webView));

            webEngine.getLoadWorker().stateProperty().addListener((obs, old, val) -> {
                if (val == javafx.concurrent.Worker.State.SUCCEEDED) {
                    webEngine.executeScript(
                        "var orig = window.onscroll; " +
                        "window.addEventListener('wheel', function(e) { " +
                        "  e.stopPropagation(); " +
                        "}, {passive: true});"
                    );
                }
            });

            // Ускорение прокрутки колесиком
            jfxPanel.addMouseWheelListener(e -> {
                int delta = e.getWheelRotation();
                Platform.runLater(() ->
                    webEngine.executeScript("window.scrollBy(0, " + (delta * 80) + ")")
                );
            });

            loadPage();
        });
    }

    public static void openWindow() {
        // Suppress JavaFX module warnings during initialization
        PrintStream origErr = System.err;
        try {
            System.setErr(new PrintStream(new java.io.OutputStream() {
                @Override public void write(int b) {}
                @Override public void write(byte[] b, int off, int len) {}
            }));
            javafx.application.Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Platform already running
        } finally {
            System.setErr(origErr);
        }
        JFrame frame = new JFrame("Регистры конфигурации");
        frame.setSize(1100, 750);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new HTMLViewer());
        frame.setVisible(true);
    }

    private void loadPage() {
        try {
            if (htmlFile == null) {
                htmlFile = File.createTempFile("registers_", ".html");
                htmlFile.deleteOnExit();
            }
            try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(htmlFile), Charset.forName("UTF-8")))) {
                pw.write(generateHtml());
            }
            webEngine.load(htmlFile.toURI().toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(new Color(0xD6EAF8));
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xAED6F1)));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setOpaque(true);
        searchIcon.setBackground(new Color(0xD6EAF8));
        JLabel findLabel = new JLabel(" Поиск:");
        findLabel.setOpaque(true);
        findLabel.setBackground(new Color(0xD6EAF8));
        findLabel.setForeground(Color.BLACK);
        searchField = new JTextField("", 30);
        searchField.setMaximumSize(new Dimension(210, 26));
        searchField.setBackground(Color.WHITE);
        searchField.setForeground(Color.BLACK);
        searchField.setCaretColor(Color.BLACK);
        matchLabel = new JLabel("");
        matchLabel.setPreferredSize(new Dimension(60, 26));
        matchLabel.setBackground(new Color(0xD6EAF8));
        matchLabel.setForeground(Color.BLACK);
        matchLabel.setOpaque(true);

        JButton btnPrev = new JButton("▲");
        JButton btnNext = new JButton("▼");
        JButton btnTop = new JButton("↑ Наверх");

        for (JButton btn : new JButton[]{btnPrev, btnNext, btnTop}) {
            btn.setBackground(new Color(0xAED6F1));
            btn.setForeground(Color.BLACK);
            btn.setContentAreaFilled(true);
        }

        btnPrev.setToolTipText("Предыдущее совпадение");
        btnNext.setToolTipText("Следующее совпадение");

        searchField.addActionListener(e -> findText(true));
        btnNext.addActionListener(e -> findText(true));
        btnPrev.addActionListener(e -> findText(false));
        btnTop.addActionListener(e -> Platform.runLater(() -> {
            if (webEngine.getDocument() != null) {
                webEngine.executeScript("window.scrollTo(0,0)");
            }
        }));

        // Clear highlights when search text changes
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { clearHighlights(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { clearHighlights(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { clearHighlights(); }
        });

        toolBar.add(btnTop);
        toolBar.addSeparator(new Dimension(10, 0));
        toolBar.add(searchIcon);
        toolBar.add(findLabel);
        toolBar.add(searchField);
        toolBar.add(btnPrev);
        toolBar.add(btnNext);
        toolBar.add(matchLabel);
        toolBar.add(Box.createHorizontalGlue());

        return toolBar;
    }

    private void findText(boolean forward) {
        String text = searchField.getText().trim();
        if (text.isEmpty()) return;
        Platform.runLater(() -> {
            boolean found = (boolean) webEngine.executeScript(
                "window.find('" + text.replace("\\", "\\\\").replace("'", "\\'") + "', "
                + "false, " + !forward + ", true, false, true, false)");
            if (!found) {
                // Wrap around
                if (forward) {
                    webEngine.executeScript("window.scrollTo(0,0)");
                } else {
                    webEngine.executeScript("window.scrollTo(0,document.body.scrollHeight)");
                }
            }
            SwingUtilities.invokeLater(() -> matchLabel.setText(found ? "" : "0 / 0"));
        });
    }

    private void clearHighlights() {
        matchLabel.setText("");
        Platform.runLater(() -> {
            if (webEngine.getDocument() != null) {
                webEngine.executeScript(
                    "window.getSelection().removeAllRanges();"
                );
            }
        });
    }

    // ===================== HTML generation =====================

    private static String generateHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\">");
        sb.append("<style>\n");
        sb.append("* { box-sizing: border-box; }\n");
        sb.append("body {\n");
        sb.append("  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n");
        sb.append("  margin: 20px; padding: 20px;\n");
        sb.append("  color: #1a1a2e; background: #fafbfc;\n");
        sb.append("  line-height: 1.6;\n");
        sb.append("}\n");
        sb.append("h2 {\n");
        sb.append("  color: #16213e; font-size: 22px; margin-bottom: 16px;\n");
        sb.append("  border-bottom: 3px solid #0f3460; padding-bottom: 6px;\n");
        sb.append("}\n");
        sb.append("h3 {\n");
        sb.append("  color: #0f3460; font-size: 17px; margin-top: 28px; margin-bottom: 8px;\n");
        sb.append("  padding: 6px 10px; background: #e8f0fe; border-left: 4px solid #0f3460;\n");
        sb.append("  border-radius: 0 4px 4px 0;\n");
        sb.append("}\n");
        sb.append("h3:target { background: #fff3cd; }\n");
        sb.append("p.desc {\n");
        sb.append("  margin: 4px 0 12px 14px; color: #444; font-size: 14px;\n");
        sb.append("}\n");
        sb.append("table {\n");
        sb.append("  border-collapse: collapse; width: auto; margin: 10px 0 16px 10px;\n");
        sb.append("  font-size: 13px; background: #fff;\n");
        sb.append("  box-shadow: 0 1px 3px rgba(0,0,0,0.1); border-radius: 4px; overflow: hidden;\n");
        sb.append("}\n");
        sb.append("th {\n");
        sb.append("  background: #0f3460; color: #fff; padding: 8px 14px;\n");
        sb.append("  text-align: center; font-weight: 600; font-size: 12px;\n");
        sb.append("  text-transform: uppercase; letter-spacing: 0.5px;\n");
        sb.append("}\n");
        sb.append("td {\n");
        sb.append("  padding: 6px 14px; border-bottom: 1px solid #e0e4e8;\n");
        sb.append("  text-align: center;\n");
        sb.append("}\n");
        sb.append("tr:nth-child(even) td { background: #f4f7fa; }\n");
        sb.append("tr:hover td { background: #e1ecf7; }\n");
        sb.append("td.desc-cell { text-align: left; max-width: 400px; }\n");
        sb.append("td.field-name { font-weight: 600; color: #0f3460; }\n");
        sb.append("td.ro { color: #c0392b; font-weight: 600; }\n");
        sb.append("td.rw { color: #27ae60; font-weight: 600; }\n");
        sb.append("a {\n");
        sb.append("  color: #1565c0; text-decoration: none; font-weight: 600;\n");
        sb.append("  transition: color 0.15s;\n");
        sb.append("}\n");
        sb.append("a:hover { color: #0d47a1; text-decoration: underline; }\n");
        sb.append(".addr-range {\n");
        sb.append("  margin: 3px 0 3px 10px; padding: 6px 12px;\n");
        sb.append("  background: #fff; border-left: 4px solid #5b9bd5;\n");
        sb.append("  font-size: 13px; color: #333; border-radius: 0 3px 3px 0;\n");
        sb.append("  box-shadow: 0 1px 2px rgba(0,0,0,0.06);\n");
        sb.append("}\n");
        sb.append(".cpu-sep {\n");
        sb.append("  margin: 16px 10px 8px; border: none;\n");
        sb.append("  border-top: 2px dashed #5b9bd5;\n");
        sb.append("}\n");
        sb.append(".small-table th {\n");
        sb.append("  background: #5b9bd5; padding: 5px 10px; font-size: 11px;\n");
        sb.append("}\n");
        sb.append(".small-table td { padding: 4px 10px; font-size: 12px; }\n");
        sb.append("</style></head><body>\n");

        sb.append("<h2>Регистры конфигурации</h2>\n");

        sb.append("<table><tr><th>+</th>");
        for (int i = 0; i <= 7; i++) sb.append("<th>2 × ").append(i).append("</th>");
        sb.append("</tr>\n");
        for (int r = 0; r <= 12; r++) {
            sb.append("<tr><td><b>2 × ").append(r * 8).append("</b></td>");
            for (int c = 0; c <= 7; c++) {
                IAllRegAddr addr = IAllRegAddr.getValueOf(AllRegAddr.class, r * 8 + c);
                sb.append("<td>");
                if (addr != null) {
                    Regs reg = (Regs) addr.getReg();
                    if (!isFactoryOnlyReg(reg) || Model.isFactoryMode()) {
                        sb.append("<a href=\"#").append(reg.name()).append("\">")
                          .append(reg.getDisplayName()).append("</a>");
                    }
                }
                sb.append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</table>\n");

        addAddrRange(sb, 104, 512 - 104, "Не используются");
        addAddrRange(sb, 512, 160, "Память программ/данных для CPU1. Ячейки доступны для записи в любом состоянии CPU1. Инициализируются из ПЗУ. Недоступны для записи из CPU1.");
        addAddrRange(sb, 512 + 160, 72, "Память программ/данных для CPU1. Ячейки доступны для записи только при выключенном CPU1. Инициализируются из ПЗУ.");
        addAddrRange(sb, 512 + 160 + 72, 8, "Ячейки ОЗУ CPU1. Ячейки доступны для записи при выключенном CPU1");
        addAddrRange(sb, 512 + 160 + 72 + 8, 8, "Ячейки ОЗУ CPU1 доступные для чтения CPU2. Ячейки доступны для записи при выключенном CPU1");
        addAddrRange(sb, 512 + 160 + 72 + 8 + 8, 8, "Используются CPU1 для чтения угловой информации из контура.");
        addAddrRange(sb, 512 + 160 + 72 + 8 + 8 + 8, 128, "Ячейки ОЗУ CPU1. Ячейки доступны для записи по SPI при выключенном CPU1.");
        addAddrRange(sb, 512 + 160 + 72 + 8 + 8 + 8 + 128, 128, "Не используются");

        sb.append("<hr class=\"cpu-sep\">\n");

        addAddrRange(sb, 1024, 160, "Память программ/данных для CPU2. Ячейки доступны для записи в любом состоянии CPU2. Инициализируются из ПЗУ. Недоступны для записи из СPU2.");
        addAddrRange(sb, 1024 + 160, 72, "Память программ/данных для CPU2. Ячейки доступны для записи только при выключенном CPU2. Инициализируются из ПЗУ.");
        addAddrRange(sb, 1024 + 160 + 72, 8, "Ячейки ОЗУ CPU2. Ячейки доступны для записи при выключенном CPU2");
        addAddrRange(sb, 1024 + 160 + 72 + 8, 8, "Ячейки ОЗУ CPU2 доступные для чтения из CPU1. Ячейки доступны для записи при выключенном CPU2");
        addAddrRange(sb, 1024 + 160 + 72 + 8 + 8, 8, "Используются CPU2 для чтения угловой информации из контура.");
        addAddrRange(sb, 1024 + 160 + 72 + 8 + 8 + 8, 128, "Ячейки ОЗУ CPU2. Ячейки доступны для записи по SPI при выключенном CPU2.");
        addAddrRange(sb, 1024 + 160 + 72 + 8 + 8 + 8 + 128, 128, "Не используются");

        int index = 1;
        for (Regs reg : Regs.values()) {
            if (isFactoryOnlyReg(reg) && !Model.isFactoryMode()) continue;
            sb.append("<h3 id=\"").append(reg.name()).append("\">")
              .append(index).append(". ").append(reg.getDisplayName()).append("</h3>\n");

            String desc = reg.getDescription().replaceAll("[\n\r]+", " ").trim();
            if (!desc.isEmpty()) {
                sb.append("<p class=\"desc\">").append(desc).append("</p>\n");
            }

            if (reg.getValueType() != null && reg.getValueType().equals(RegValueType.VALUE_FIELDS)) {
                List<IRegField> fields = reg.getFields();
                if (fields != null && !fields.isEmpty()) {
                    RegBitHolder bitHolder = new RegBitHolder(fields);
                    RegFieldTableData tableData = bitHolder.getNumBitListHeader();
                    List<String> hdrs = tableData.getTableHeader();
                    List<IRegField> flds = tableData.getTableRegField();

                    sb.append("<table class=\"small-table\"><tr><th>№</th>");
                    for (String h : hdrs) sb.append("<th>").append(h).append("</th>");
                    sb.append("</tr>");

                    sb.append("<tr><td>Доступ</td>");
                    for (IRegField f : flds) {
                        sb.append("<td class=\"").append(f != null ? (reg.isOnlyRead() ? "ro" : "rw") : "")
                          .append("\">").append(f != null ? (reg.isOnlyRead() ? "RO" : "R/W") : "-").append("</td>");
                    }
                    sb.append("</tr>");

                    sb.append("<tr><td>Сброс</td>");
                    for (IRegField f : flds) {
                        sb.append("<td>").append(f != null ? f.getDefaultValue() : "-").append("</td>");
                    }
                    sb.append("</tr>");

                    sb.append("<tr><td>Имя</td>");
                    for (IRegField f : flds) {
                        sb.append("<td class=\"field-name\">").append(f != null ? f.getDisplayName() : "").append("</td>");
                    }
                    sb.append("</tr></table>\n");

                    sb.append("<table><tr><th>№ бита</th><th>Имя поля</th><th>Доступ</th><th>Сброс</th><th>Описание</th></tr>");
                    for (int i = 0; i < hdrs.size(); i++) {
                        sb.append("<tr><td>").append(hdrs.get(i)).append("</td>");
                        IRegField field = flds.get(i);
                        if (field != null) {
                            sb.append("<td class=\"field-name\">").append(field.getDisplayName()).append("</td>");
                            sb.append("<td class=\"").append(reg.isOnlyRead() ? "ro" : "rw").append("\">")
                              .append(reg.isOnlyRead() ? "RO" : "R/W").append("</td>");
                            sb.append("<td>").append(field.getDefaultValue()).append("</td>");
                            sb.append("<td class=\"desc-cell\">").append(field.getDescription().replaceAll("[\n\r]+", " ")).append("</td>");
                        } else {
                            sb.append("<td></td><td></td><td></td><td></td>");
                        }
                        sb.append("</tr>\n");
                    }
                    sb.append("</table>\n");
                }
            }
            index++;
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private static boolean isFactoryOnlyReg(Regs reg) {
        return FactoryGate.isFactoryOnlyReg(reg);
    }

    private static void addAddrRange(StringBuilder sb, int address, int width, String text) {
        sb.append("<div class=\"addr-range\">2 × ").append(address)
          .append(" — <b>").append(width).append(" адресов:</b> ")
          .append(text).append("</div>\n");
    }

    // ===================== Static: generate HTML file =====================

    public static void createMapRegistersFile() {
        String html = generateHtml();
        File pathHtml = new File(WorkstationConfig.getProperty(ConfProp.FILE_PATH_HTML_FILE)
                + File.separator + "map_registers.html");
        try (OutputStream out = new FileOutputStream(pathHtml);
             PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, Charset.forName("UTF-8")))) {
            pw.write(html);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Created " + pathHtml.getAbsolutePath());
    }
}
