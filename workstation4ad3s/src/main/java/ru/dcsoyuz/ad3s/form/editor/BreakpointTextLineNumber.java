package ru.dcsoyuz.ad3s.form.editor;

import ru.dcsoyuz.ad3s.form.terminal.CpuDebugPanel;

import javax.swing.border.Border;
import javax.swing.border.MatteBorder;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.Collections;

public class BreakpointTextLineNumber extends TextLineNumber implements CaretListener, DocumentListener, PropertyChangeListener {

    private final static Border OUTER = new MatteBorder(0, 0, 0, 2, Color.GRAY);
    private final static int HEIGHT = Integer.MAX_VALUE - 1000000;

    private JTextComponent component;
    private boolean updateFont;
    private int borderGap;
    private Color currentLineForeground;
    private float digitAlignment;
    private int minimumDisplayDigits;


    private Integer curDebugStopNumLine = null;


    private CpuDebugPanel cpuDebugPanel;
    private LinkedList<Integer> breakpoints = new LinkedList<>();



    public BreakpointTextLineNumber(JTextComponent component, int minimumDisplayDigits, CpuDebugPanel cpuDebugPanel) {
        super(component, minimumDisplayDigits);
        this.component = component;
        this.cpuDebugPanel = cpuDebugPanel;
        setFont(component.getFont());

        setBorderGap(5);
        setCurrentLineForeground(Color.RED);
        setDigitAlignment(RIGHT);
        setMinimumDisplayDigits(minimumDisplayDigits);

        component.getDocument().addDocumentListener(this);
        component.addCaretListener(this);
        component.addPropertyChangeListener("font", this);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int lineNumber = getLineNumberAtPoint(e.getPoint());
                if (lineNumber != -1) {
                    if (breakpoints.contains(lineNumber)) {
                        removeBreakpoint(lineNumber);
                    } else {
                        setBreakpoint(lineNumber);
                    }
                    cpuDebugPanel.updateBreakpointAddresses(breakpoints);

                }
            }
        });
    }

    // ... (getters и setters для updateFont, borderGap, currentLineForeground, digitAlignment, minimumDisplayDigits)

    private int getLineNumberAtPoint(Point point) {
        Rectangle clip = getVisibleRect();
        int rowStartOffset = component.viewToModel(new Point(clip.x, point.y));
        return component.getDocument().getDefaultRootElement().getElementIndex(rowStartOffset);
    }

    public void setBreakpoint(int lineNumber) {
        if(breakpoints.size() >= 2){
            if(breakpoints.contains(lineNumber)){
                return;
            }
            breakpoints.removeFirst();
        }
        breakpoints.offer(lineNumber);
        repaint();
    }


    public void removeAllBreakpoints() {
        breakpoints.clear();
        repaint();
    }


    public void removeBreakpoint(int lineNumber) {
        breakpoints.removeAll(Collections.singleton(lineNumber));
        repaint();
    }



    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        FontMetrics fontMetrics = component.getFontMetrics(component.getFont());
        Insets insets = getInsets();
        int availableWidth = getSize().width - insets.left - insets.right;

        Rectangle clip = g.getClipBounds();
        int rowStartOffset = component.viewToModel(new Point(0, clip.y));
        int endOffset = component.viewToModel(new Point(0, clip.y + clip.height));
        int numLine = 0;



        while (rowStartOffset <= endOffset) {
            try {


                String lineNumber = getTextLineNumber(rowStartOffset);
                int stringWidth = fontMetrics.stringWidth(lineNumber);
                int x = getOffsetX(availableWidth, stringWidth) + insets.left;
                int y = getOffsetY(rowStartOffset, fontMetrics);

                g.setColor(new Color(46, 238, 46));


                if (curDebugStopNumLine != null && Integer.parseInt(lineNumber) == curDebugStopNumLine) {
                    g.fillRect(x - 3, y - 13, 30, 16);
                    g.setColor(getCurrentLineForeground());
                } else {
                    g.setColor(getForeground());
                }


                g.drawString(lineNumber, x, y);


                if (breakpoints.contains(Integer.parseInt(lineNumber))) {
                    g.setColor(new Color(255, 0,0));
                    g.fillOval(x - 10, y - 10, 10, 10);
                }

                rowStartOffset = Utilities.getRowEnd(component, rowStartOffset) + 1;
                numLine = numLine + 1;
            } catch (Exception e) {
                break;
            }
        }

    }

    public void setCurDebugStopNumLine(Integer curDebugStopNumLine) {
        this.curDebugStopNumLine = curDebugStopNumLine;
        repaint();
    }
    // ... (остальные методы из TextLineNumber)
}