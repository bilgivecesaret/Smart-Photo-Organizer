package com.example.smart_photo_organizer.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class HashItem implements Parcelable { // Parcelable eklendi
    public long hash;
    public Uri uri;
    public String bucketName;
    public long timestamp;
    public double latitude;
    public double longitude;

    public HashItem(long hash, Uri uri, String bucketName, long timestamp, double latitude, double longitude) {
        this.hash = hash;
        this.uri = uri;
        this.bucketName = bucketName;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;

    }

    // Parcelable için gereken metodlar
    protected HashItem(Parcel in) {
        hash = in.readLong();
        uri = in.readParcelable(Uri.class.getClassLoader());
        bucketName = in.readString();
        timestamp = in.readLong();
        latitude = in.readDouble();
        longitude = in.readDouble();
    }

    public static final Creator<HashItem> CREATOR = new Creator<HashItem>() {
        @Override
        public HashItem createFromParcel(Parcel in) {
            return new HashItem(in);
        }

        @Override
        public HashItem[] newArray(int size) {
            return new HashItem[size];
        }
    };

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(hash);
        dest.writeParcelable(uri, flags);
        dest.writeString(bucketName);
        dest.writeLong(timestamp);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
    }
}