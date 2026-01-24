package com.example.smart_photo_organizer.model;

import android.net.Uri;

public class HashItem {
    public long hash;
    public Uri uri;
    public String bucketName;

    public HashItem(long hash, Uri uri, String bucketName) {
        this.hash = hash;
        this.uri = uri;
        this.bucketName = bucketName;
    }
}

