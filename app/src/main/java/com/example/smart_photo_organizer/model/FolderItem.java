package com.example.smart_photo_organizer.model;

import java.io.File;
import java.util.ArrayList;

public class FolderItem {

    private final String folderPath;
    private final String previewImage;
    private final ArrayList<String> imageList;

    public FolderItem(String folderPath,
                      String previewImage,
                      ArrayList<String> imageList) {
        this.folderPath = folderPath;
        this.previewImage = previewImage;
        this.imageList = imageList;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public String getPreviewImage() {
        return previewImage;
    }

    public ArrayList<String> getImageList() {
        return imageList;
    }

    public String getDisplayName() {
        if (folderPath == null) return "";

        String path = folderPath.endsWith("/")
                ? folderPath.substring(0, folderPath.length() - 1)
                : folderPath;

        int lastSlash = path.lastIndexOf("/");
        return lastSlash != -1
                ? path.substring(lastSlash + 1)
                : path;
    }
}


