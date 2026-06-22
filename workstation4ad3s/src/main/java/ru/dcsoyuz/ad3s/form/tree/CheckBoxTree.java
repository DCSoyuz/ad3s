
package ru.dcsoyuz.ad3s.form.tree;

import ru.dcsoyuz.ad3s.model.Model;
import ru.dcsoyuz.ad3s.model.editor.TreeViewModel;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

public class CheckBoxTree extends JTree {

  public CheckBoxTree(TreeModel model) {
    super(model);

    setCellRenderer(new CheckBoxRenderer());
    addMouseListener(new MouseL());

    setShowsRootHandles(false);
    setRowHeight(0);
    if (getUI() instanceof javax.swing.plaf.basic.BasicTreeUI) {
      javax.swing.plaf.basic.BasicTreeUI treeUI = (javax.swing.plaf.basic.BasicTreeUI) getUI();
      treeUI.setLeftChildIndent(0);
      treeUI.setRightChildIndent(4);
    }
  }

  private DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();


  class CheckBoxRenderer extends JCheckBox  implements TreeCellRenderer {

    private JLabel plainLabel;

    public CheckBoxRenderer() {

      setOpaque(true);
      plainLabel = new JLabel();
      plainLabel.setOpaque(true);
      plainLabel.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
    }

    private JPanel rootPanel;

    private JPanel getRootPanel(String folderName, Color bg) {
      if (rootPanel == null) {
        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setOpaque(true);
      }
      rootPanel.removeAll();
      rootPanel.setBackground(bg);

      Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
      JLabel label = new JLabel(folderName, folderIcon, SwingConstants.LEFT);
      label.setFont(label.getFont().deriveFont(Font.BOLD));
      label.setBackground(bg);
      label.setOpaque(true);
      rootPanel.add(label, BorderLayout.CENTER);
      rootPanel.add(new JSeparator(), BorderLayout.SOUTH);
      rootPanel.setPreferredSize(new Dimension(getVisibleRect().width > 0 ? getVisibleRect().width : 120, 28));
      return rootPanel;
    }

    private JLabel separatorLabel;

    private JLabel getSeparatorPanel() {
      if (separatorLabel == null) {
        separatorLabel = new JLabel();
        separatorLabel.setOpaque(true);
      }
      separatorLabel.setText("");
      separatorLabel.setBackground(new Color(0x1A2A3A));
      separatorLabel.setForeground(new Color(0x5B9BD5));
      separatorLabel.setBorder(BorderFactory.createMatteBorder(3, 4, 0, 4, new Color(0x5B9BD5)));
      separatorLabel.setPreferredSize(new Dimension(120, 8));
      return separatorLabel;
    }

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row,  boolean hasFocus) {

        if (!(value instanceof DefaultMutableTreeNode)) {
            return renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
        }

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        boolean isRoot = node.getParent() == null;
        Object data = node.getUserObject();

        // Separator between file groups
        if (TreeViewModel.SEPARATOR.equals(data)) {
            return getSeparatorPanel();
        }

        if ( isRoot && data instanceof CheckBoxListElement ) {
            CheckBoxListElement element = (CheckBoxListElement) data;
            return getRootPanel(element.getText(), new Color(0x1A2A3A));
        }

        if ( data instanceof CheckBoxListElement ) {
            CheckBoxListElement element = (CheckBoxListElement)data;

            if (element.isHasCheckBox()) {
                setSelected(element.isSelected());
                setText(element.getText());
                if(element.getText().contains("asm")) {
                   this.setForeground(new Color(0x6BB3F0));
                } else{
                  this.setForeground(new Color(0xE8F0F8));
                }
                if(element.getText().equals(Model.getEditorModel().getCurrentFile().getName())){
                  this.setBackground(new Color(0x5B9BD5));
                  this.setForeground(Color.BLACK);
                } else{
                  this.setBackground(new Color(0x1A2A3A));
                }
                return this;
            } else {
                plainLabel.setText(element.getText());
                if(element.getText().contains("asm")) {
                   plainLabel.setForeground(new Color(0x6BB3F0));
                } else{
                  plainLabel.setForeground(new Color(0xE8F0F8));
                }
                if(element.getText().equals(Model.getEditorModel().getCurrentFile().getName())){
                  plainLabel.setBackground(new Color(0x5B9BD5));
                  plainLabel.setForeground(Color.BLACK);
                } else{
                  plainLabel.setBackground(new Color(0x1A2A3A));
                }
                return plainLabel;
            }
        }

        // Directory nodes (String userObject)
        if (isRoot) {
            return getRootPanel(data.toString(), new Color(0x1A2A3A));
        }

        return renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }
  }




  class MouseL extends MouseAdapter {
    public void mousePressed(MouseEvent e) {

      TreePath path = getClosestPathForLocation(
          e.getX(), e.getY());
      if ( path == null ) return;

      Object _node = path.getLastPathComponent();
      if (_node instanceof DefaultMutableTreeNode) {
          DefaultMutableTreeNode node = (DefaultMutableTreeNode)_node;

          // Skip clicks on root
          if (node.getParent() == null) return;

          Object data = node.getUserObject();

          // Skip separator
          if (TreeViewModel.SEPARATOR.equals(data)) return;

          if ( data instanceof CheckBoxListElement ) {
          CheckBoxListElement element = (CheckBoxListElement)data;
          if(e.getClickCount() == 2){
              String pathParent = Model.getEditorModel().getCurrentFile().getParent();
              Model.getEditorModel().setCurrentFile(new File(pathParent +  File.separator + element.getText()));
              Model.getEditorModel().openActiveTab("asm-editor");
          } else if (element.isHasCheckBox()) {
              element.setSelected(!element.isSelected());
          }
          repaint(getPathBounds(path));
        }
      }



    }




  }
}
