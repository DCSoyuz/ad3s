package ru.dcsoyuz.ad3s.form.tree;

import ru.dcsoyuz.ad3s.model.Model;

import javax.swing.*;

public class TreeViewPane extends JScrollPane {


    public TreeViewPane() {
        super(new CheckBoxTree(Model.getTreeViewModel().getTreeModel()));
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    }


}
