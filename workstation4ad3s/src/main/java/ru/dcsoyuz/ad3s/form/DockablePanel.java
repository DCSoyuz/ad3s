package ru.dcsoyuz.ad3s.form;

import io.github.andrewauclair.moderndocking.Dockable;
import io.github.andrewauclair.moderndocking.DockableStyle;
import io.github.andrewauclair.moderndocking.DockableTabPreference;

import javax.swing.*;
import java.awt.*;

/**
 * Wrapper that makes any JPanel dockable without modifying the original class.
 */
public class DockablePanel extends JPanel implements Dockable {

    private final String persistentID;
    private final String tabText;
    private final JPanel content;

    public DockablePanel(String persistentID, String tabText, JPanel content) {
        this.persistentID = persistentID;
        this.tabText = tabText;
        this.content = content;
        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
    }

    /** Placeholder constructor used by modern-docking via reflection during layout restore.
     *  The framework then looks up the already-registered instance by persistentID. */
    public DockablePanel(String persistentID, String tabText) {
        this.persistentID = persistentID;
        this.tabText = tabText;
        this.content = new JPanel();
        setLayout(new BorderLayout());
        add(content, BorderLayout.CENTER);
    }

    @Override
    public String getPersistentID() {
        return persistentID;
    }

    @Override
    public String getTabText() {
        return tabText;
    }

    public JPanel getContent() {
        return content;
    }

    @Override
    public boolean isClosable() {
        return false;
    }

    @Override
    public boolean isFloatingAllowed() {
        return true;
    }

    @Override
    public DockableTabPreference getTabPreference() {
        return DockableTabPreference.TOP_ALWAYS;
    }

    @Override
    public DockableStyle getStyle() {
        return DockableStyle.BOTH;
    }

    @Override
    public boolean isWrappableInScrollpane() {
        return false;
    }
}
