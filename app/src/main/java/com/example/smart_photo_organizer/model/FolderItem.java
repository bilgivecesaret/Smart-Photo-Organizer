package com.example.smart_photo_organizer.model;

import android.net.Uri;

import java.util.ArrayList;

public class FolderItem {

    private final String folderName;
    private final Uri previewUri;
    private final ArrayList<Uri> imageUris;

    public FolderItem(String folderName,
                      Uri previewUri,
                      ArrayList<Uri> imageUris) {
        this.folderName = folderName;
        this.previewUri = previewUri;
        this.imageUris = imageUris;
    }

    public String getFolderName() {
        return folderName;
    }

    public Uri getPreviewUri() {
        return previewUri;
    }

    public ArrayList<Uri> getImageUris() {
        return imageUris;
    }
}



