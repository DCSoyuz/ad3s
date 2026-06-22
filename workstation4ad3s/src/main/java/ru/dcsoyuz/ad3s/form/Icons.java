package ru.dcsoyuz.ad3s.form;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/**
 * Created by yuri.filatov on 22.07.2016.
 */
public class Icons {

    public static ImageIcon mainIcon = createAppIcon();
    public static ImageIcon iconNew = new ImageIcon(ClassLoader.getSystemResource("icons/new.png"));
    public static ImageIcon iconOpen = new ImageIcon(ClassLoader.getSystemResource("icons/open.png"));
    public static ImageIcon iconSave = new ImageIcon(ClassLoader.getSystemResource("icons/save.png"));
    public static ImageIcon iconAbout = new ImageIcon(ClassLoader.getSystemResource("icons/about.png"));
    public static ImageIcon iconExit = new ImageIcon(ClassLoader.getSystemResource("icons/exit.png"));
    public static ImageIcon iconSelectAll = new ImageIcon(ClassLoader.getSystemResource("icons/selectAll.png"));
    public static ImageIcon iconCut = new ImageIcon(ClassLoader.getSystemResource("icons/cut.png"));
    public static ImageIcon iconCopy = new ImageIcon(ClassLoader.getSystemResource("icons/copy.png"));
    public static ImageIcon iconPaste = new ImageIcon(ClassLoader.getSystemResource("icons/paste.png"));
    public static ImageIcon iconDelete = new ImageIcon(ClassLoader.getSystemResource("icons/delete.png"));
    public static ImageIcon iconUndo = new ImageIcon(ClassLoader.getSystemResource("icons/undo.png"));
    public static ImageIcon iconRedo = new ImageIcon(ClassLoader.getSystemResource("icons/redo.png"));
    public static ImageIcon iconSearch= new ImageIcon(ClassLoader.getSystemResource("icons/search.png"));
    public static ImageIcon iconWarning = new ImageIcon(ClassLoader.getSystemResource("icons/warning.png"));

    private static Color rgba(int r, int g, int b, int a) {
        return new Color(r, g, b, a);
    }

    private static ImageIcon createAppIcon() {
        int size = 256;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        int margin = 8;
        int circleSize = size - margin * 2;
        GradientPaint bgGradient = new GradientPaint(
                margin, margin, new Color(0x87CEEB),
                size - margin, size - margin, new Color(0x4A90D9));
        g2.setPaint(bgGradient);
        g2.fill(new Ellipse2D.Float(margin, margin, circleSize, circleSize));

        int ringMargin = 16;
        g2.setColor(rgba(255, 255, 255, 50));
        g2.setStroke(new BasicStroke(3f));
        g2.draw(new Ellipse2D.Float(ringMargin, ringMargin, size - ringMargin * 2, size - ringMargin * 2));

        int innerMargin = 24;
        g2.setColor(rgba(0x3A, 0x7B, 0xBF, 60));
        g2.fill(new Ellipse2D.Float(innerMargin, innerMargin, size - innerMargin * 2, size - innerMargin * 2));

        int centerMargin = 36;
        GradientPaint centerGradient = new GradientPaint(
                size / 2f, centerMargin, new Color(0xBBE8FF),
                size / 2f, size - centerMargin, new Color(0x6BB3F0));
        g2.setPaint(centerGradient);
        g2.fill(new Ellipse2D.Float(centerMargin, centerMargin, size - centerMargin * 2, size - centerMargin * 2));

        Font font = new Font("Segoe UI", Font.BOLD, 150);
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        String letter = "W";
        int textWidth = fm.stringWidth(letter);
        int textHeight = fm.getAscent();
        int textX = (size - textWidth) / 2;
        int textY = (size + textHeight) / 2 - fm.getDescent() / 2;

        g2.setColor(rgba(0x1A, 0x2A, 0x3A, 120));
        g2.drawString(letter, textX + 3, textY + 3);

        GradientPaint textGradient = new GradientPaint(
                textX, textY - textHeight, new Color(0x1A2A3A),
                textX, textY + fm.getDescent(), new Color(0x2D4A6C));
        g2.setPaint(textGradient);
        g2.drawString(letter, textX, textY);

        g2.setColor(rgba(255, 255, 255, 60));
        g2.drawString(letter, textX - 1, textY - 1);

        g2.setColor(new Color(0x3A5A7C));
        g2.setStroke(new BasicStroke(4f));
        g2.draw(new Ellipse2D.Float(margin, margin, circleSize, circleSize));

        g2.dispose();

        return new ImageIcon(img);
    }

}
