package com.example.smart_photo_organizer.model;


import android.net.Uri;
import java.util.List;

public class DuplicateGroup {
    public List<Uri> images;

    public DuplicateGroup(List<Uri> images) {
        this.images = images;
    }

    public List<Uri> getUris() {
        return images;
    }
}
