package ru.dcsoyuz.ad3s.form;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

/**
 * Custom UI delegate for JSplitPane with blue dividers
 * This overrides FlatLaf's default UI
 */
public class BlueSplitPaneUI extends BasicSplitPaneUI {

    // Blue colors
    private static final Color DIVIDER_BG = new Color(0x5B9BD5);      // Sky blue
    private static final Color DIVIDER_BORDER = new Color(0x6BB3F0);  // Bright blue
    private static final Color DIVIDER_GRIP = new Color(0x2D4A6C);    // Dark blue
    private static final Color DIVIDER_HOVER = new Color(0x7BC0E0);   // Light blue

    public static BlueSplitPaneUI createUI(JSplitPane splitPane) {
        return new BlueSplitPaneUI();
    }

    @Override
    public BasicSplitPaneDivider createDefaultDivider() {
        return new BlueSplitPaneDivider(this);
    }

    /**
     * Custom divider with blue colors
     */
    static class BlueSplitPaneDivider extends BasicSplitPaneDivider {
        private final BlueSplitPaneUI ui;

        public BlueSplitPaneDivider(BlueSplitPaneUI ui) {
            super(ui);
            this.ui = ui;
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();

            // Fill with blue background
            g2.setColor(DIVIDER_BG);
            g2.fillRect(0, 0, width, height);

            // Draw border
            g2.setColor(DIVIDER_BORDER);
            g2.drawRect(0, 0, width - 1, height - 1);

            // Draw grip (dragging handles)
            g2.setColor(DIVIDER_GRIP);
            int gripSize = 3;
            int gripSpacing = 6;

            if (splitPane != null) {
                if (splitPane.getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
                    // Vertical divider with horizontal grip lines
                    int centerX = width / 2;
                    int gripStartY = (height - (gripSize * gripSpacing)) / 2;

                    for (int i = 0; i < gripSize; i++) {
                        int y = gripStartY + i * gripSpacing;
                        g2.fillRect(centerX - 10, y, 20, 2);
                    }
                } else {
                    // Horizontal divider with vertical grip lines
                    int centerY = height / 2;
                    int gripStartX = (width - (gripSize * gripSpacing)) / 2;

                    for (int i = 0; i < gripSize; i++) {
                        int x = gripStartX + i * gripSpacing;
                        g2.fillRect(x, centerY - 10, 2, 20);
                    }
                }
            }

            g2.dispose();
        }
    }
}
