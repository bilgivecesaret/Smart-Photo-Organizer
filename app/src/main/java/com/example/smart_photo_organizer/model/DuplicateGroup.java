package com.example.smart_photo_organizer.model;


import android.net.Uri;
import java.util.List;

public class DuplicateGroup {
    public String hash;
    public List<Uri> images;

    public DuplicateGroup(String hash, List<Uri> images) {
        this.hash = hash;
        this.images = images;
    }
}

