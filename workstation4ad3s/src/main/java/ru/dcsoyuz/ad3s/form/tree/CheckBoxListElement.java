
package ru.dcsoyuz.ad3s.form.tree;



public class CheckBoxListElement {

  private boolean selected;
  private boolean isCurrent;
  private String text;
  private boolean hasCheckBox;

  public CheckBoxListElement(boolean selected, String text, boolean isCurrent){
    this(selected, text, isCurrent, true);
  }

  public CheckBoxListElement(boolean selected, String text, boolean isCurrent, boolean hasCheckBox){
    this.selected = selected;
    this.text = text;
    this.isCurrent = isCurrent;
    this.hasCheckBox = hasCheckBox;
  }

  public boolean isSelected() {  return selected; }
  public String getText() { return text; }
  public void setSelected(boolean selected) {
    this.selected = selected;
  }
  public void setText(String text) { this.text = text; }

  public boolean isCurrent() {
    return isCurrent;
  }

  public void setCurrent(boolean current) {
    isCurrent = current;
  }

  public boolean isHasCheckBox() { return hasCheckBox; }
  public void setHasCheckBox(boolean hasCheckBox) { this.hasCheckBox = hasCheckBox; }
}
