package com.example.smart_photo_organizer.model;


import android.net.Uri;
import java.util.List;

public class DuplicateGroup {
    public long hash;
    public List<Uri> images;

    public DuplicateGroup(long hash, List<Uri> images) {
        this.hash = hash;
        this.images = images;
    }

    public List<Uri> getUris() {
        return images;
    }
}
