package com.example.smart_photo_organizer.model;

import java.io.File;
import java.util.ArrayList;

public class FolderItem {
    public String folderPath;
    public String previewImage;
    public ArrayList<String> imageList;

    public FolderItem(String folderPath, String previewImage, ArrayList<String> imageList) {
        this.folderPath = folderPath;
        this.previewImage = previewImage;
        this.imageList = imageList;
    }

    public String getFolderName() {
        return new File(folderPath).getName();
    }
}

