package ru.dcsoyuz.ad3s.form.editor;

import javax.swing.text.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * Created by Администратор on 13.05.2016.
 */
public class WordSearcher {

    protected JTextComponent comp;
    protected Highlighter.HighlightPainter painter;

    public WordSearcher(JTextComponent comp) {
        this.comp = comp;



//Color.getHSBColor(245, 242, 244)
        this.painter = new DefaultHighlighter.DefaultHighlightPainter(new Color(207, 255, 253));
    }

    // Search for a word and return the offset of the first occurrence.
    // Highlights are added for all occurrences found.
    public int search(ArrayList<String> words) {
        int firstOffset = -1;
        Highlighter highlighter = comp.getHighlighter();
        // Remove any existing highlights for last word
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {
            Highlighter.Highlight h = highlights[i];
            if (h.getPainter() instanceof UnderlineHighlighter.UnderlineHighlightPainter) {
                highlighter.removeHighlight(h);
            }
        }
        if (words == null ) {
            return -1;
        }
        String content = null; // Look for the word we are given - insensitive search
        try {
            Document d = comp.getDocument();
            content = d.getText(0, d.getLength()).toLowerCase();
        } catch (BadLocationException e) {
            return -1; // Cannot happen
        }
       ;
        int lastIndex = 0;

        for(String word : words) {
            word = word.toLowerCase();
            int wordSize = word.length();
            while ((lastIndex = content.indexOf(word, lastIndex)) != -1) {
                int endIndex = lastIndex + wordSize;
                try {

                    highlighter.addHighlight(lastIndex, endIndex, painter);
                } catch (BadLocationException e) {
                    // Nothing to do
                }
                if (firstOffset == -1) {
                    firstOffset = lastIndex;
                }
                lastIndex = endIndex;
            }
        }
        return firstOffset;
    }
}
