package ru.dcsoyuz.ad3s.form;

/**
 * Created by yuri.filatov on 01.09.2016.
 */

import javax.swing.*;
import java.awt.*;

public class GridBagHelper {

    private int gridx = 0, gridy = 0;

    private GridBagConstraints constraints =new GridBagConstraints();

    public GridBagHelper(){
        constraints.gridx = gridx;
        constraints.gridy = gridy;
    }

    public GridBagConstraints get() {
        return constraints;
    }



    public GridBagHelper nextColumn() {
        constraints.gridx = ++gridx;
        gridy = 0;
        constraints.gridy = gridy ;
        return this;
    }


    public GridBagHelper setPosition(int x, int y){
        gridx = x;
        constraints.gridx = gridx;
        gridy = y;
        constraints.gridy = gridy;
        return this;
    }


    public GridBagHelper nextCell() {
        constraints.gridx = ++gridx;
        constraints.gridy = gridy;
        return this;
    }

    public GridBagHelper nextRow() {
        gridx = 0;
        constraints.gridy = ++gridy;
        constraints.gridx = gridx;
        return this;
    }

    public GridBagHelper downRow() {
        constraints.gridy = ++gridy;
        return this;
    }
    public GridBagHelper setRow(int row) {
        gridy = row;
        constraints.gridy = row;
        return this;
    }

    public GridBagHelper upRow() {
        constraints.gridy = --gridy;
        return this;
    }

    public GridBagHelper downRow(int y) {
        gridy = gridy + y;
        constraints.gridy = gridy;
        return this;
    }

    public GridBagHelper rightColumn() {
        constraints.gridx = ++gridx;
        return this;
    }



    public GridBagHelper rightColumn(int r) {
        gridx  = gridx + r;
        constraints.gridx = gridx;
        return this;
    }


    public GridBagHelper east() {
        constraints.anchor = GridBagConstraints.NORTHEAST;
        return this;
    }
    public GridBagHelper west() {
        constraints.anchor = GridBagConstraints.WEST;
        return this;
    }

    public GridBagHelper leftColumn() {
        constraints.gridx = --gridx;
        return this;
    }

    public GridBagHelper setZeroCell() {
        gridx = 0;
        gridy = 0;

        constraints.gridx = gridx;
        constraints.gridy = gridy;
        return this;
    }

    public GridBagHelper spanX() {
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        return this;
    }

    public GridBagHelper spanXRelative() {
        constraints.gridwidth = GridBagConstraints.RELATIVE;
        return this;
    }

    public GridBagHelper fillHorizontally() {
        constraints.fill = GridBagConstraints.HORIZONTAL;
        return this;
    }

    public GridBagHelper fillVertically() {
        constraints.fill = GridBagConstraints.BOTH;
        return this;
    }
    public GridBagHelper fillNone() {
        constraints.fill = GridBagConstraints.NONE;
        return this;
    }



    public GridBagHelper gap(int size) {
        constraints.insets.right = size;
        return this;
    }

    public GridBagHelper spanY() {
        constraints.gridheight = GridBagConstraints.REMAINDER;
        return this;
    }


    public GridBagHelper fillBoth() {
        constraints.fill = GridBagConstraints.BOTH;
        return this;
    }

    public GridBagHelper alignLeft() {
        constraints.anchor = GridBagConstraints.LINE_START;
        return this;
    }

    public GridBagHelper alignRight() {
        constraints.anchor = GridBagConstraints.WEST;
        return this;
    }

    public GridBagHelper setInsets(int left, int top, int right, int bottom) {
        Insets i = new Insets(top, left, bottom, right);
        constraints.insets = i;
        return this;
    }

    public GridBagHelper setWeights(float horizontal, float vertical) {
        constraints.weightx = horizontal;
        constraints.weighty = vertical;
        return this;
    }

    public GridBagHelper setWeightY(float vertical){
        constraints.weighty = vertical;
        return this;
    }

    public GridBagHelper setWeightX(float horizontal){
        constraints.weightx = horizontal;
        return this;
    }

    public GridBagHelper setGridWidth(int w){
        constraints.gridwidth= w;
        return this;
    }
    public GridBagHelper setGridHeight(int h){
        constraints.gridheight= h;
        return this;
    }
    public void insertEmptyRow(Container c, int height) {
        Component comp = Box.createVerticalStrut(height);
        nextCell().downRow().fillHorizontally().spanX();
        c.add(comp, get());
        downRow();
    }

    public void insertEmptyFiller(Container c) {
        Component comp = Box.createGlue();
        nextCell().downRow().fillBoth().spanX().spanY().setWeights(1.0f, 1.0f);
        c.add(comp, get());
        downRow();
    }
}
