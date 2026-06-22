package ru.dcsoyuz.ad3s.form;

import ru.dcsoyuz.ad3s.form.terminal.Indicator;

import javax.swing.*;
import java.awt.*;

/**
 * Created by yuri.filatov on 22.07.2016.
 */
public class AppFrameHelper {

    public static final String APLICATION_NAME = "AD3S ASM IDE";

    public static final int WIDTH_BUTTON = 90;
    public static final int HEIGHT_BUTTON = 20;


    public static  String getHtml(String text){
        return "<html>"+text.replaceAll("(\n|\n)", "<br />")+"</html>";
    }

    public  static void  setupLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Metal".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            try {
                UIManager.setLookAndFeel(lookAndFeel);
            } catch (Exception ex) {
                System.err.println("Nimbus look and feel not found.\n"
                        + "Default look and feel not found.");
            }
        }
    }

    /**
     * Setup modern FlatLaf look and feel.
     * Используется только из MainModern, не влияет на старый Main.java
     *
     * @return true если FlatLaf успешно установлен, false если fallback к стандартному
     */
    public static boolean setupModernLookAndFeel() {
        try {
            // Проверка наличия FlatLaf в classpath
            Class<?> flatLafClass = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            LookAndFeel lookAndFeel = (LookAndFeel) flatLafClass.getDeclaredConstructor().newInstance();

            UIManager.setLookAndFeel(lookAndFeel);

            // Настройка FlatLaf для аккуратного современного вида
            setupModernUIProperties();

            // Современные шрифты
            setupModernFonts();

            return true;

        } catch (Exception ex) {
            System.err.println("FlatLaf not found, falling back to Metal.");
            setupLookAndFeel();
            return false;
        }
    }

    /**
     * Настройка свойств UI для современного FlatLaf вида
     */
    private static void setupModernUIProperties() {
        try {
            // Скругленные углы для компонентов
            UIManager.put("TextComponent.arc", 5);
            UIManager.put("Component.arc", 5);
            UIManager.put("Button.arc", 5);
            UIManager.put("ComboBox.arc", 5);

            // Фокус
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);

            // Отступы
            UIManager.put("Layout.interval", 8);

            // ScrollBar
            UIManager.put("ScrollBar.showButtons", true);
            UIManager.put("ScrollBar.arc", 5);

            // TabbedPane
            UIManager.put("TabbedPane.tabHeight", 32);
            UIManager.put("TabbedPane.tabInsets", "8,12,8,12");

            // Table
            UIManager.put("Table.rowHeight", 24);
            UIManager.put("Table.showHorizontalLines", false);
            UIManager.put("Table.showVerticalLines", false);

            // Кнопки
            UIManager.put("Button.borderWidth", 1);
            UIManager.put("Button.margin", "4,8,4,8");
        } catch (Exception e) {
            System.err.println("Error setting modern UI properties: " + e.getMessage());
        }
    }

    /**
     * Настройка современных шрифтов для FlatLaf
     * Сделан public для вызова из MainModern
     */
    public static void setupModernFonts() {
        String os = System.getProperty("os.name").toLowerCase();

        Font defaultFont;
        Font monoFont;

        if (os.contains("linux")) {
            defaultFont = new Font("Nimbus Sans L", Font.PLAIN, 11);
            monoFont = new Font("Monospaced", Font.PLAIN, 11);
        } else if (os.contains("win")) {
            defaultFont = new Font("Segoe UI", Font.PLAIN, 11);
            monoFont = new Font("Courier New", Font.PLAIN, 9);  // Очень узкий плотный шрифт для лога
        } else {
            // macOS
            defaultFont = new Font("SF Pro Text", Font.PLAIN, 11);
            monoFont = new Font("Menlo", Font.PLAIN, 11);
        }

        UIManager.put("defaultFont", defaultFont);
        UIManager.put("TextArea.font", monoFont);
        UIManager.put("TextPane.font", monoFont);
        UIManager.put("EditorPane.font", monoFont);
        UIManager.put("Table.font", monoFont);
    }


    public static JPanel getLumpLabeled(Indicator lump, String label){
        JPanel  panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel comment= new JLabel(label);
        panel.add(lump);
        panel.add(comment);
        return panel;
    }

    /**
     * Setup the application location on the screen if there is more than one monitor
     * and they work in union mode
     * @param frame the frame of current application
     */
    public static void setupAppLocation(JFrame frame) {
        int width = 0;
        int height = 0;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] monitors = ge.getScreenDevices();
        GraphicsConfiguration gc = null;
        Rectangle screenRes = null;
        // select 1st(default monitor)
        if(monitors.length > 0) {
            gc = monitors[0].getDefaultConfiguration();
            screenRes = gc.getBounds();
        } else {
            throw new RuntimeException("No monitor found!");
        }
        //height of the task bar
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
        int taskBarSize = scnMax.bottom;

        width = (screenRes.width  > 1920) ? 1920 : screenRes.width ;
        height = screenRes.height - taskBarSize;

        frame.setSize((int)(width / 1.6),height);

        frame.setLocation((width - frame.getWidth()) / 2, 0);
    }

    public static void setupAppLocation2(JFrame frame) {
        int width = 0;
        int height = 0;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] monitors = ge.getScreenDevices();
        GraphicsConfiguration gc = null;
        Rectangle screenRes = null;
        // select 1st(default monitor)
        if(monitors.length > 0) {
            gc = monitors[0].getDefaultConfiguration();
            screenRes = gc.getBounds();
        } else {
            throw new RuntimeException("No monitor found!");
        }
        //height of the task bar
        Insets scnMax = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());
        int taskBarSize = scnMax.bottom;

        width = (screenRes.width  > 1920) ? 1920 : screenRes.width ;
        height = (int)((screenRes.height - taskBarSize));

        frame.setSize((int)(width / 2),height);

        frame.setLocation((width - frame.getWidth()) / 2, 0);
    }

    public static JPanel createPanel(String text){

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(text));
        panel.setBorder(BorderFactory.createTitledBorder(text));
        return panel;
    }

    public static JPanel createPanelWithoutLabel(String text){

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(text));
        return panel;
    }



    public static JCheckBox createLeftCheckBox(String text){
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setHorizontalTextPosition(JCheckBox.LEFT);
        return checkBox;
    }

    public static JCheckBox createRightCheckBox(String text){
        JCheckBox checkBox = new JCheckBox(text);
        checkBox.setHorizontalTextPosition(JCheckBox.RIGHT);
        return checkBox;
    }


    public static JPanel getTextFieldLabeled(JTextField textField,String label, int sizeLabel, int sizeField){
        JPanel  panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel comment= new JLabel(label, JLabel.RIGHT);
        //comment.setFont(new Font("Times New Romain",1, 16));
        comment.setMaximumSize(new Dimension(sizeLabel, 20));
        panel.add(comment);
        textField.setColumns(5);
        textField.setFont(new Font("Times New Romain",1, 16));
        textField.setMaximumSize(new Dimension(sizeField, 20));
        textField.setMinimumSize(new Dimension(sizeField, 20));
        textField.setPreferredSize(new Dimension(sizeField,20));
        panel.add(textField);
        return panel;
    }


    public static JButton createButton(Action action, String name, String toolTipText  ){
        JButton button  = new JButton(action);
        button.setText(name);
        button.setToolTipText(toolTipText);
        button.setPreferredSize(new Dimension(WIDTH_BUTTON,HEIGHT_BUTTON));
        button.setMargin(new Insets(2,2,2,2));
        button.setMinimumSize(new Dimension(WIDTH_BUTTON,HEIGHT_BUTTON));
        return button;

    }

    /**
     * Настройка цветов JSplitPane для синей темы
     * Вызывается после создания каждого JSplitPane
     *
     * @param splitPane JSplitPane для настройки
     */
    public static void setupSplitPane(JSplitPane splitPane) {
        // Устанавливаем цвета для компонента
        splitPane.setBackground(new Color(0x1A2A3A)); // Темно-синий фон

        // КРИТИЧНО: Устанавливаем кастомный UI делегат
        // Это заменяет FlatLaf UI на наш синий
        splitPane.setUI(new BlueSplitPaneUI());

        // Сброс divider size для корректного отображения
        splitPane.setDividerSize(splitPane.getDividerSize());

        // Принудительное обновление
        splitPane.revalidate();
        splitPane.repaint();
    }

    /**
     * Настройка цветов JTabbedPane для синей темы
     * Вызывается после создания JTabbedPane
     *
     * @param tabbedPane JTabbedPane для настройки
     */
    public static void setupTabbedPaneColors(JTabbedPane tabbedPane) {
        Color tabBg = new Color(0x5B9BD5);      // Светло-синий вкладка
        Color textBlack = Color.BLACK;

        // Устанавливаем цвета
        tabbedPane.setBackground(tabBg);
        tabbedPane.setForeground(textBlack);

        // Принудительное обновление UI
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tabbedPane.updateUI();
                tabbedPane.revalidate();
                tabbedPane.repaint();
            }
        });
    }

}
