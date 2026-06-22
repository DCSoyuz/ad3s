package ru.dcsoyuz.ad3s.form.terminal;

/**
 * Created by yuri.filatov on 02.09.2016.
 */
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

class GraphicsHelper {

    public static JToggleButton getButton(
            Image selected,
            Image unselected,
            boolean decorated) {

        JToggleButton b = new JToggleButton();
        b.setSelectedIcon(new ImageIcon(selected));
        b.setIcon(new ImageIcon(unselected));
        b.setBorderPainted(decorated);

        return b;
    }





    public static BufferedImage getCircleImage(Color c) {
        BufferedImage bi = new BufferedImage(
                16,16,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();

        g.setColor(c);
        g.fillOval(0,0,16,16);

        g.dispose();
        return bi;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                Image red = getCircleImage(Color.RED);
                Image green = getCircleImage(Color.GREEN);

                JPanel p = new JPanel(new GridLayout(0,1));

                JToolBar tb1 = new JToolBar();
                for (int ii=0; ii<5; ii++) {
                    tb1.add( getButton(red, green, true) );
                }
                p.add(tb1);

                JToolBar tb2 = new JToolBar();
                for (int ii=0; ii<5; ii++) {
                    tb2.add( getButton(red, green, false) );
                }
                p.add(tb2);

                JOptionPane.showMessageDialog(null, p);
            }
        });
    }
}