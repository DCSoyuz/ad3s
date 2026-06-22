package ru.dcsoyuz.ad3s.model.editor;

import ru.dcsoyuz.ad3s.form.editor.IAppFrameEventListener;
import ru.dcsoyuz.ad3s.form.tree.CheckBoxListElement;
import ru.dcsoyuz.ad3s.model.Model;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TreeViewModel implements IAppFrameEventListener {


    public static final String SEPARATOR = "---SEP---";
    public TreeModel treeModel;

    public TreeModel createTreeModel(File infile){
        DefaultMutableTreeNode root;
        root  =new DefaultMutableTreeNode(new CheckBoxListElement(false, infile.getParentFile().getName(), false));
        createChildren(infile.getParentFile(), root);
        return new DefaultTreeModel(root);
    }



    private void updateTreeModel(File infile){
        // Save selected file names before rebuilding
        List<String> selectedNames = new ArrayList<>();
        if (treeModel != null) {
            DefaultMutableTreeNode oldRoot = (DefaultMutableTreeNode) ((DefaultTreeModel) treeModel).getRoot();
            collectSelectedNames(oldRoot, selectedNames);
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new CheckBoxListElement(false, infile.getParentFile().getName(), false));
        createChildren(infile.getParentFile(), root);

        // Restore selected state (only if same directory)
        if (!selectedNames.isEmpty()) {
            restoreSelectedNames(root, selectedNames);
        }

        DefaultTreeModel defaultTreeModel = ((DefaultTreeModel)treeModel);
        defaultTreeModel.setRoot(root);
        defaultTreeModel.reload();
    }

    private void createChildren(File fileParent,
                                DefaultMutableTreeNode node) {
        File[] files = fileParent.listFiles();
        if (files == null) return;

        List<File> txtAsmFiles = new ArrayList<>();
        List<File> hexFiles = new ArrayList<>();
        List<File> dirs = new ArrayList<>();

        for (File file : files) {
            if (file.getName().contains(".config")) continue;
            if (file.isDirectory()) {
                dirs.add(file);
            } else if (file.getName().toLowerCase().endsWith(".hex")) {
                hexFiles.add(file);
            } else {
                txtAsmFiles.add(file);
            }
        }

        // Add directories first
        for (File dir : dirs) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(dir.getName());
            node.add(childNode);
            createChildren(dir, childNode);
        }

        // Add txt/asm files (no checkboxes)
        for (File file : txtAsmFiles) {
            node.add(new DefaultMutableTreeNode(new CheckBoxListElement(false, file.getName(), false, false)));
        }

        // Separator between groups
        if (!hexFiles.isEmpty() && !txtAsmFiles.isEmpty()) {
            node.add(new DefaultMutableTreeNode(SEPARATOR));
        }

        // Add hex files (with checkboxes)
        for (File file : hexFiles) {
            node.add(new DefaultMutableTreeNode(new CheckBoxListElement(false, file.getName(), false, true)));
        }
    }

    public void updateCurrentFile(File newFile, File oldFile) {
        if(oldFile == null ||  !oldFile.getParent().equals(newFile.getParent()) ){
            updateTreeModel(newFile);
        }
    }

    public void refreshTree() {
        File currentFile = Model.getEditorModel().getCurrentFile();
        if (currentFile != null && currentFile.getParentFile() != null) {
            // Save selected file names before rebuilding
            List<String> selectedNames = new ArrayList<>();
            if (treeModel != null) {
                DefaultMutableTreeNode oldRoot = (DefaultMutableTreeNode) ((DefaultTreeModel) treeModel).getRoot();
                collectSelectedNames(oldRoot, selectedNames);
            }

            DefaultMutableTreeNode root = new DefaultMutableTreeNode(
                    new CheckBoxListElement(false, currentFile.getParentFile().getName(), false));
            createChildren(currentFile.getParentFile(), root);

            // Restore selected state
            if (!selectedNames.isEmpty()) {
                restoreSelectedNames(root, selectedNames);
            }

            DefaultTreeModel defaultTreeModel = ((DefaultTreeModel) treeModel);
            defaultTreeModel.setRoot(root);
            defaultTreeModel.reload();
        }
    }

    private void collectSelectedNames(DefaultMutableTreeNode node, List<String> names) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object data = child.getUserObject();
            if (data instanceof CheckBoxListElement) {
                CheckBoxListElement element = (CheckBoxListElement) data;
                if (element.isSelected() && element.isHasCheckBox()) {
                    names.add(element.getText());
                }
            }
            collectSelectedNames(child, names);
        }
    }

    private void restoreSelectedNames(DefaultMutableTreeNode node, List<String> names) {
        for (int i = 0; i < node.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) node.getChildAt(i);
            Object data = child.getUserObject();
            if (data instanceof CheckBoxListElement) {
                CheckBoxListElement element = (CheckBoxListElement) data;
                if (names.contains(element.getText())) {
                    element.setSelected(true);
                }
            }
            restoreSelectedNames(child, names);
        }
    }

    public TreeModel getTreeModel() {
        if(treeModel == null){
            treeModel = createTreeModel(Model.getEditorModel().getCurrentFile());
        }
        return treeModel;
    }

    public void selectAllCheckBox(boolean mode){
        DefaultTreeModel defaultTreeModel = ((DefaultTreeModel)treeModel);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) defaultTreeModel.getRoot();
        for(int i = 0; i < root.getChildCount(); i++){
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)root.getChildAt(i);
            Object data = node.getUserObject();
            if ( data instanceof CheckBoxListElement ) {
                CheckBoxListElement element = (CheckBoxListElement) data;
                if (element.isHasCheckBox()) {
                    element.setSelected(mode);
                }
            }
        }
        defaultTreeModel.setRoot(root);
        defaultTreeModel.reload();
    }


    public List<File> getSelectedFiles(){
        DefaultMutableTreeNode root =(DefaultMutableTreeNode)treeModel.getRoot();
        List<File> selectedFiles = new ArrayList<>();
        getSelectedFilesFromNode(root, selectedFiles);
        return selectedFiles;
    }

    private void getSelectedFilesFromNode(DefaultMutableTreeNode node, List<File>selectedFiles){
        for(int i = 0; i < node.getChildCount(); i++){
            DefaultMutableTreeNode child =(DefaultMutableTreeNode) node.getChildAt(i);
            if(child.getChildCount() == 0){
                Object data = child.getUserObject();
                if(data instanceof CheckBoxListElement){
                    CheckBoxListElement element = (CheckBoxListElement)data;
                    if(element.isSelected()){
                        File file = new File(Model.getEditorModel().getCurrentFile().getParent() + File.separator + element.getText());
                        selectedFiles.add(file);
                    }
                }
            }else{
                getSelectedFilesFromNode(child, selectedFiles);
            }
        }




    }



}
