package com.example.smart_photo_organizer.model;

import java.util.List;

public class AutoAlbum {
    public String title;
    public List<HashItem> photos;

    public AutoAlbum(String title, List<HashItem> photos) {
        this.title = title;
        this.photos = photos;
    }

}

