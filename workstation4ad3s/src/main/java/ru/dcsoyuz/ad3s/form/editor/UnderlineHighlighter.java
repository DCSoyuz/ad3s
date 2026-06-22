package ru.dcsoyuz.ad3s.form.editor;

import javax.swing.text.*;
import java.awt.*;

/**
 * Created by Администратор on 13.05.2016.
 */
public class UnderlineHighlighter extends DefaultHighlighter {

    protected static final HighlightPainter sharedPainter = new UnderlineHighlightPainter(null);// Shared painter used for default highlighting
    protected HighlightPainter painter; // Painter used for this highlighter

    public UnderlineHighlighter(Color c) {
        painter = (c == null ? sharedPainter : new UnderlineHighlightPainter(c));
    }

    // Convenience method to add a highlight with the default painter.
    public Object addHighlight(int p0, int p1) throws BadLocationException {
        return addHighlight(p0, p1, painter);
    }

    @Override
    public void setDrawsLayeredHighlights(boolean newValue) {
        if (newValue == false) {// Illegal if false - we only support layered highlights
            throw new IllegalArgumentException(
                    "UnderlineHighlighter only draws layered highlights");
        }
        super.setDrawsLayeredHighlights(true);
    }

    // Painter for underlined highlights
    public static class UnderlineHighlightPainter extends LayerPainter {

        protected Color color; // The color for the underline

        public UnderlineHighlightPainter(Color c) {
            color = c;
        }


        public void paint(Graphics g, int offs0, int offs1, Shape bounds,
                          JTextComponent c) {// Do nothing: this method will never be called
        }


        public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds,
                                JTextComponent c, View view) {
            g.setColor(color == null ? c.getSelectionColor() : color);
            Rectangle alloc = null;
            if (offs0 == view.getStartOffset() && offs1 == view.getEndOffset()) {
                if (bounds instanceof Rectangle) {
                    alloc = (Rectangle) bounds;
                } else {
                    alloc = bounds.getBounds();
                }
            } else {
                try {
                    Shape shape = view.modelToView(offs0, Position.Bias.Forward, offs1,
                            Position.Bias.Backward, bounds);
                    alloc = (shape instanceof Rectangle) ? (Rectangle) shape : shape.getBounds();
                } catch (BadLocationException e) {
                    return null;
                }
            }
            FontMetrics fm = c.getFontMetrics(c.getFont());
            int baseline = alloc.y + alloc.height - fm.getDescent() + 1;
            g.drawLine(alloc.x, baseline, alloc.x + alloc.width, baseline);
            g.drawLine(alloc.x, baseline + 1, alloc.x + alloc.width, baseline + 1);
            return alloc;
        }
    }
}