package com.linewell.lxhdemo.ui.home.bean;

public class NavigationItem {
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getSelectedImg() {
        return selectedImg;
    }

    public void setSelectedImg(int selectedImg) {
        this.selectedImg = selectedImg;
    }

    public int getNotSelectedImg() {
        return notSelectedImg;
    }

    public void setNotSelectedImg(int notSelectedImg) {
        this.notSelectedImg = notSelectedImg;
    }

    private String text;
    private int selectedImg;
    private int notSelectedImg;
}
