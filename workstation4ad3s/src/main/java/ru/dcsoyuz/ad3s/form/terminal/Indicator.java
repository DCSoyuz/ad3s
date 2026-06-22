package ru.dcsoyuz.ad3s.form.terminal;


import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
/**
 * Created by yuri.filatov on 02.09.2016.
 */
public class Indicator extends JPanel{

    private BufferedImage image;
    StatusIndicator status;

    public Indicator() {
        image = GraphicsHelper.getCircleImage(Color.GRAY);
        status = StatusIndicator.DISABLED;
    }


    public void refresh(Boolean newState){
        if(newState == null && !status.equals(StatusIndicator.DISABLED)) {
            image = GraphicsHelper.getCircleImage(Color.GRAY);
            status = StatusIndicator.DISABLED;
        }else if (newState && !status.equals(StatusIndicator.ON)){
            image = GraphicsHelper.getCircleImage(Color.GREEN);
            status = StatusIndicator.ON;
        }else if(!newState && !status.equals(StatusIndicator.OFF)){
            image = GraphicsHelper.getCircleImage(Color.RED);
            status = StatusIndicator.OFF;
        }
        this.repaint();
    }



    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0, 0, null); // see javadoc for more info on the parameters
    }


}
