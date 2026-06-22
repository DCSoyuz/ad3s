package ru.dcsoyuz.ad3s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.dcsoyuz.ad3s.form.AppFrame;
import ru.dcsoyuz.ad3s.form.AppFrameHelper;
import ru.dcsoyuz.ad3s.config.FactoryCrypto;
import ru.dcsoyuz.ad3s.model.Model;

import javax.swing.*;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.io.File;

/**
 * Modern UI entry point with FlatLaf theming - Синяя тема
 *
 * Custom UI delegate for JSplitPane dividers
 */
public class MainModern {

    private static final Logger logger = LoggerFactory.getLogger(MainModern.class);

    /**
     * Тема приложения
     */
    public enum Theme {
        LIGHT_BLUE,           // Светло-синяя (по умолчанию)
        SKY_BLUE,             // Небесно-голубая
        OCEAN_DARK,           // Темно-синяя океаническая
        MIDNIGHT_BLUE,        // Полуночно-синяя
        FLAT_DARK             // Оригинальная FlatLaf
    }

    private static Theme currentTheme = Theme.LIGHT_BLUE;

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("--restore")) {
                new File("dock_layout.xml").delete();
                logger.info("--restore: deleted dock_layout.xml");
            }
            if (arg.startsWith("--factory")) {
                String[] parts = arg.split("=", 2);
                if (parts.length == 2 && FactoryCrypto.isValidKey(parts[1])) {
                    Model.setFactoryMode(true);
                }
                break;
            }
        }
        setupFlatLaf();

        SwingUtilities.invokeLater(() -> {
            try {
                Model.init();
                AppFrame.getInstance();

            } catch (Exception e) {
                logger.error("Error starting application: {}", e.getMessage());
                logger.error("Error starting application", e);
                JOptionPane.showMessageDialog(null,
                    "Ошибка запуска приложения: " + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }

    /**
     * Настройка темы
     * v2.9 - Используем Metal L&F с кастомными цветами + BlueSplitPaneUI
     */
    private static void setupFlatLaf() {
        try {
            // FlatLaf as base L&F (required for Modern Docking top tabs)
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());

            // Apply blue theme overrides on top of FlatLaf
            overrideAllUIDefaults();

            logger.info("Blue theme initialized: {}", currentTheme);

        } catch (Exception ex) {
            logger.error("Theme initialization failed: {}", ex.getMessage());
            logger.error("Theme initialization failed", ex);
            // Fallback
            AppFrameHelper.setupLookAndFeel();
            AppFrameHelper.setupModernFonts();
        }
    }

    /**
     * ПОЛНОЕ переопределение UIManager defaults
     */
    private static void overrideAllUIDefaults() {
        Color primary = new ColorUIResource(getPrimaryColor());
        Color secondary = new ColorUIResource(getSecondaryColor());
        Color background = new ColorUIResource(getBackgroundColor());
        Color foreground = new ColorUIResource(getForegroundColor());
        Color accent = new ColorUIResource(getAccentColor());
        Color selectionBg = new ColorUIResource(getSelectionBackground());
        Color selectionFg = new ColorUIResource(getSelectionForeground());
        Color disabled = new ColorUIResource(getDisabledColor());
        Color buttonTextColor = Color.BLACK;

        // Button - небесно-голубой как меню
        Color buttonBg = new ColorUIResource(new Color(0xA8E0FF));    // Небесно-голубой
        Color buttonHover = new ColorUIResource(new Color(0xBEEAFA));  // Светлее при наведении
        Color buttonPressed = new ColorUIResource(new Color(0x87CEEB)); // Темнее при нажатии
        Color buttonFocused = new ColorUIResource(new Color(0xBBE8FF)); // Фокус
        Color buttonBorder = new ColorUIResource(new Color(0x87CEEB));  // Обводка

        UIManager.put("Button.background", buttonBg);
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("Button.hoverBackground", buttonHover);
        UIManager.put("Button.pressedBackground", buttonPressed);
        UIManager.put("Button.focusedBackground", buttonFocused);
        UIManager.put("Button.borderColor", buttonBorder);
        UIManager.put("Button.startBackground", new ColorUIResource(new Color(0xBBE8FF)));
        UIManager.put("Button.endBackground", new ColorUIResource(new Color(0x9DD8F8)));
        UIManager.put("Button.startBorderColor", new ColorUIResource(new Color(0x87CEEB)));
        UIManager.put("Button.endBorderColor", new ColorUIResource(new Color(0x78BDD8)));
        UIManager.put("Button.arc", 8);
        UIManager.put("Button.borderWidth", 1);
        UIManager.put("Button.margin", new Insets(2, 8, 2, 8));
        UIManager.put("Button.shadowWidth", 0);
        //ToolBar цвета
        UIManager.put("ToolBar.background", new ColorUIResource(new Color(0x5B9BD5))); // Фон toolbar
        UIManager.put("ToolBar.foreground", Color.BLACK);

        // RadioButton
        UIManager.put("RadioButton.background", background);
        UIManager.put("RadioButton.foreground", foreground);
        UIManager.put("RadioButton.icon", new RadioButtonIcon());

        // CheckBox
        UIManager.put("CheckBox.background", background);
        UIManager.put("CheckBox.foreground", foreground);
        UIManager.put("CheckBox.icon", new CheckBoxIcon());

        // JSplitPane - базовые
        UIManager.put("SplitPane.background", background);
        UIManager.put("SplitPane.foreground", foreground);

        // TabbedPane - вкладки (яркая заливка выбранной вкладки)
        Color tabSelectedBg = new ColorUIResource(new Color(0xFFD700));   // Выбранная вкладка - золотисто-жёлтая
        Color tabAreaBg = new ColorUIResource(new Color(0x1E2D3D));      // Фон полосы вкладок - тёмный
        Color tabUnselectedBg = new ColorUIResource(new Color(0x2A3F55)); // Неактивные вкладки - чуть светлее фона

        UIManager.put("TabbedPane.background", tabUnselectedBg);          // Фон НЕвыбранных вкладок - тёмный
        UIManager.put("TabbedPane.foreground", new ColorUIResource(new Color(0xAABBCC)));
        UIManager.put("TabbedPane.selectedBackground", tabSelectedBg);    // Заливка выбранной вкладки - жёлтая
        UIManager.put("TabbedPane.selectedForeground", Color.BLACK);
        UIManager.put("TabbedPane.hoverColor", new ColorUIResource(new Color(0xA8E0FF)));
        UIManager.put("TabbedPane.hoverForeground", Color.BLACK);
        UIManager.put("TabbedPane.contentAreaColor", tabSelectedBg);

        UIManager.put("TabbedPane.tabType", "card");   // card = заливка фона вкладки (не подчёркивание)
        UIManager.put("TabbedPane.tabHeight", 22);
        UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(1, 6, 1, 6));
        UIManager.put("TabbedPane.selectedTabPadInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("TabbedPane.tabAreaBackground", tabAreaBg);
        UIManager.put("TabbedPane.tabsOverlapBorder", true);
        UIManager.put("TabbedPane.hasFullBorder", false);
        UIManager.put("TabbedPane.showTabSeparators", true);
        UIManager.put("TabbedPane.tabSeparatorColor", new ColorUIResource(new Color(0x3A5570)));

        // Table - таблица светлая, заголовок синий
        UIManager.put("Table.background", new ColorUIResource(new Color(0xE8F4F8))); // Светло-голубая таблица
        UIManager.put("Table.foreground", Color.BLACK); // Черный текст
        UIManager.put("Table.selectionBackground", new ColorUIResource(new Color(0x7BC0E0))); // Выбор светло-голубой
        UIManager.put("Table.selectionForeground", Color.BLACK);
        UIManager.put("Table.gridColor", new ColorUIResource(new Color(0x6A8AAC))); // Сетка светло-синяя
        UIManager.put("TableHeader.background", new ColorUIResource(new Color(0x5B9BD5))); // Заголовок синий
        UIManager.put("TableHeader.foreground", Color.BLACK); // Черный текст в заголовке

        // Tree
        UIManager.put("Tree.background", background);
        UIManager.put("Tree.foreground", foreground);
        UIManager.put("Tree.selectionBackground", selectionBg);
        UIManager.put("Tree.selectionForeground", selectionFg);

        // Прочие компоненты
        UIManager.put("Panel.background", background);
        UIManager.put("Panel.foreground", foreground);
        // TextArea и TextPane - ЕДИНЫЙ светло-голубой оттенок
        final Color editorBg = new ColorUIResource(new Color(0xE8F4F8)); // Единый оттенок
        UIManager.put("TextArea.background", editorBg);
        UIManager.put("TextArea.foreground", Color.BLACK);
        UIManager.put("TextPane.background", editorBg);
        UIManager.put("TextPane.foreground", Color.BLACK);
        UIManager.put("TextPane.selectionBackground", new ColorUIResource(new Color(0x7BC0E0)));
        UIManager.put("TextPane.selectionForeground", Color.BLACK);
        UIManager.put("TextArea.selectionBackground", new ColorUIResource(new Color(0x7BC0E0)));
        UIManager.put("TextArea.selectionForeground", Color.BLACK);
        // TextField - светло-голубой фон
        Color textFieldBg = new ColorUIResource(new Color(0xE8F4F8)); // Почти белый светло-голубой
        UIManager.put("TextField.background", textFieldBg);
        UIManager.put("TextField.foreground", Color.BLACK);
        UIManager.put("TextField.selectionBackground", new ColorUIResource(new Color(0x7BC0E0)));
        UIManager.put("TextField.selectionForeground", Color.BLACK);
        //FormattedTextField (используется в JSpinner)
        UIManager.put("FormattedTextField.background", textFieldBg);
        UIManager.put("FormattedTextField.foreground", Color.BLACK);
        UIManager.put("FormattedTextField.border", BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("FormattedTextField.margin", new java.awt.Insets(0, 0, 0, 0));
        // ComboBox - выпадающие списки для битов
        UIManager.put("ComboBox.background", textFieldBg);
        UIManager.put("ComboBox.foreground", Color.BLACK);
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(new Color(0x7BC0E0)));
        UIManager.put("ComboBox.selectionForeground", Color.BLACK);
        // Spinner
        UIManager.put("Spinner.background", textFieldBg);
        UIManager.put("Spinner.foreground", Color.BLACK);
        UIManager.put("Spinner.border", BorderFactory.createEmptyBorder(0, 0, 0, 0));
        UIManager.put("Spinner.padding", new java.awt.Insets(0, 0, 0, 0));
        UIManager.put("Label.background", background);
        UIManager.put("Label.foreground", foreground);

        // MenuBar - синие цвета как вкладки
        Color menuBg = new ColorUIResource(new Color(0xA8E0FF));      // Небесно-голубой
        Color menuSelected = new ColorUIResource(new Color(0x87CEEB)); // Выбранный пункт
        Color popupBg = new ColorUIResource(new Color(0xD6EAF8));     // Выпадающий список - светло-голубой
        UIManager.put("MenuBar.background", menuBg);
        UIManager.put("MenuBar.foreground", Color.BLACK);
        UIManager.put("Menu.background", menuBg);
        UIManager.put("Menu.foreground", Color.BLACK);
        UIManager.put("MenuItem.background", menuBg);
        UIManager.put("MenuItem.foreground", Color.BLACK);
        UIManager.put("MenuItem.selectionBackground", menuSelected);
        UIManager.put("MenuItem.selectionForeground", Color.BLACK);
        UIManager.put("CheckBoxMenuItem.background", menuBg);
        UIManager.put("CheckBoxMenuItem.foreground", Color.BLACK);
        UIManager.put("CheckBoxMenuItem.selectionBackground", menuSelected);
        UIManager.put("CheckBoxMenuItem.selectionForeground", Color.BLACK);
        UIManager.put("PopupMenu.background", popupBg);
        UIManager.put("PopupMenu.foreground", Color.BLACK);

        setupModernFonts();
    }

    /**
     * Цвет границы
     */
    private static Color borderColor() {
        switch (currentTheme) {
            case LIGHT_BLUE:
            case SKY_BLUE:
                return new Color(0x3A5A7C);
            case OCEAN_DARK:
                return new Color(0x4C566A);
            case MIDNIGHT_BLUE:
                return new Color(0x30363D);
            case FLAT_DARK:
            default:
                return new Color(0x555555);
        }
    }

    /**
     * Получить основной цвет темы
     */
    private static Color getPrimaryColor() {
        switch (currentTheme) {
            case LIGHT_BLUE:
                return new Color(0x4A90D9);
            case SKY_BLUE:
                return new Color(0x5B9BD5);
            case OCEAN_DARK:
                return new Color(0x5E81AC);
            case MIDNIGHT_BLUE:
                return new Color(0x58A6FF);
            case FLAT_DARK:
            default:
                return new Color(0x4A90E2);
        }
    }

    /**
     * Получить акцентный цвет
     */
    private static Color getAccentColor() {
        switch (currentTheme) {
            case LIGHT_BLUE:
                return new Color(0x6BB3F0);
            case SKY_BLUE:
                return new Color(0x7BC0E0);
            case OCEAN_DARK:
                return new Color(0x81A1C1);
            case MIDNIGHT_BLUE:
                return new Color(0x79C0FF);
            case FLAT_DARK:
            default:
                return new Color(0x4A90E2);
        }
    }

    /**
     * Получить вторичный цвет
     */
    private static Color getSecondaryColor() {
        switch (currentTheme) {
            case LIGHT_BLUE:
                return new Color(0x2D4A6C);
            case SKY_BLUE:
                return new Color(0x3A5A7C);
            case OCEAN_DARK:
                return new Color(0x3B4252);
            case MIDNIGHT_BLUE:
                return new Color(0x30363D);
            case FLAT_DARK:
            default:
                return new Color(0x3C3F41);
        }
    }

    /**
     * Получить цвет фона
     */
    private static Color getBackgroundColor() {
        switch (currentTheme) {
            case LIGHT_BLUE:
                return new Color(0x1A2A3A);
            case SKY_BLUE:
                return new Color(0x1E2D3D);
            case OCEAN_DARK:
                return new Color(0x2E3440);
            case MIDNIGHT_BLUE:
                return new Color(0x0D1117);
            case FLAT_DARK:
            default:
                return new Color(0x212121);
        }
    }

    /**
     * Получить цвет текста
     */
    private static Color getForegroundColor() {
        switch (currentTheme) {
            case LIGHT_BLUE:
            case SKY_BLUE:
                return new Color(0xE8F0F8);
            case OCEAN_DARK:
                return new Color(0xECEFF4);
            case MIDNIGHT_BLUE:
                return new Color(0xC9D1D9);
            case FLAT_DARK:
            default:
                return new Color(0xE0E0E0);
        }
    }

    /**
     * Получить цвет выделения
     */
    private static Color getSelectionBackground() {
        switch (currentTheme) {
            case LIGHT_BLUE:
                return new Color(0x5B9BD5);
            case SKY_BLUE:
                return new Color(0x6BB3F0);
            case OCEAN_DARK:
                return new Color(0x4C566A);
            case MIDNIGHT_BLUE:
                return new Color(0x1F6FEB);
            case FLAT_DARK:
            default:
                return new Color(0x4A90E2);
        }
    }

    /**
     * Получить цвет выделенного текста
     */
    private static Color getSelectionForeground() {
        switch (currentTheme) {
            case LIGHT_BLUE:
            case SKY_BLUE:
            case MIDNIGHT_BLUE:
                return Color.WHITE;
            case OCEAN_DARK:
                return new Color(0xECEFF4);
            case FLAT_DARK:
            default:
                return Color.WHITE;
        }
    }

    /**
     * Получить цвет отключенных элементов
     */
    private static Color getDisabledColor() {
        switch (currentTheme) {
            case LIGHT_BLUE:
            case SKY_BLUE:
                return new Color(0x6B7A8A);
            case OCEAN_DARK:
                return new Color(0x4C566A);
            case MIDNIGHT_BLUE:
                return new Color(0x6E7681);
            case FLAT_DARK:
            default:
                return new Color(0x808080);
        }
    }

    /**
     * Настройка современных шрифтов
     */
    private static void setupModernFonts() {
        String os = System.getProperty("os.name").toLowerCase();

        Font defaultFont;
        Font monoFont;
        Font boldFont;

        if (os.contains("linux")) {
            defaultFont = new Font("Nimbus Sans L", Font.PLAIN, 12);
            monoFont = new Font("Monospaced", Font.PLAIN, 12);
            boldFont = new Font("Nimbus Sans L", Font.BOLD, 12);
        } else if (os.contains("win")) {
            defaultFont = new Font("Segoe UI", Font.PLAIN, 12);
            monoFont = new Font("Consolas", Font.PLAIN, 12);
            boldFont = new Font("Segoe UI", Font.BOLD, 12);
        } else {
            defaultFont = new Font("SF Pro Text", Font.PLAIN, 12);
            monoFont = new Font("Menlo", Font.PLAIN, 12);
            boldFont = new Font("SF Pro Text", Font.BOLD, 12);
        }

        UIManager.put("defaultFont", defaultFont);
        UIManager.put("TextArea.font", monoFont);
        UIManager.put("TextPane.font", monoFont);
        UIManager.put("EditorPane.font", monoFont);
        UIManager.put("Table.font", monoFont);
        UIManager.put("TableHeader.font", boldFont);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", boldFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("TabbedPane.font", boldFont);
        UIManager.put("Menu.font", defaultFont);
        UIManager.put("MenuItem.font", defaultFont);
        UIManager.put("MenuBar.font", boldFont);
        UIManager.put("CheckBox.font", defaultFont);
        UIManager.put("RadioButton.font", defaultFont);
        UIManager.put("Tree.font", defaultFont);
    }

    /**
     * Кастомная иконка для CheckBox
     */
    private static class CheckBoxIcon implements Icon {
        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = false;
            boolean armed = false;
            boolean focused = false;
            boolean enabled = c.isEnabled();

            if (c instanceof AbstractButton) {
                AbstractButton btn = (AbstractButton) c;
                selected = btn.isSelected();
                armed = btn.getModel().isArmed();
                focused = btn.hasFocus();
            }

            Color bg = getSecondaryColor();
            Color border = borderColor();
            Color check = getAccentColor();

            if (!enabled) {
                bg = getDisabledColor();
                border = getDisabledColor();
            } else if (armed) {
                bg = getAccentColor();
            } else if (focused) {
                border = getAccentColor();
            }

            g2.setColor(bg);
            g2.fillRect(x, y, SIZE - 2, SIZE - 2);
            g2.setColor(border);
            g2.drawRect(x, y, SIZE - 2, SIZE - 2);

            if (selected) {
                g2.setColor(check);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(x + 3, y + SIZE / 2, x + SIZE / 2 - 1, y + SIZE - 5);
                g2.drawLine(x + SIZE / 2 - 1, y + SIZE - 5, x + SIZE - 4, y + 3);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }

    /**
     * Кастомная иконка для RadioButton
     */
    private static class RadioButtonIcon implements Icon {
        private static final int SIZE = 16;

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean selected = false;
            boolean focused = false;
            boolean enabled = c.isEnabled();

            if (c instanceof AbstractButton) {
                AbstractButton btn = (AbstractButton) c;
                selected = btn.isSelected();
                focused = btn.hasFocus();
            }

            Color bg = getSecondaryColor();
            Color border = borderColor();
            Color dot = getAccentColor();

            if (!enabled) {
                bg = getDisabledColor();
                border = getDisabledColor();
            } else if (focused) {
                border = getAccentColor();
            }

            int centerX = x + SIZE / 2 - 1;
            int centerY = y + SIZE / 2 - 1;
            int radius = SIZE / 2 - 2;

            g2.setColor(border);
            g2.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);

            g2.setColor(bg);
            g2.fillOval(centerX - radius + 2, centerY - radius + 2, (radius - 2) * 2, (radius - 2) * 2);

            if (selected) {
                g2.setColor(dot);
                int dotRadius = radius / 2;
                g2.fillOval(centerX - dotRadius, centerY - dotRadius, dotRadius * 2, dotRadius * 2);
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
