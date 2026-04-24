package org.duhen.dglyphs;

public class StyleItem {

    private final String folder;
    private final String fileName;
    private final String display;

    public StyleItem(String folder, String fileName, String display) {
        this.folder = folder;
        this.fileName = fileName;
        this.display = display;
    }

    public String folder() {
        return folder;
    }

    public String fileName() {
        return fileName;
    }

    public String display() {
        return display;
    }
}