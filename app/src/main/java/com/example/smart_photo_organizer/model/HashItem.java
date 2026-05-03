package com.example.smart_photo_organizer.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class HashItem implements Parcelable {
    public float[] embedding;
    public Uri uri;
    public String bucketName;
    public long timestamp;
    public double latitude;
    public double longitude;
    public boolean isFrontCamera;

    public HashItem(float[] embedding, Uri uri, String bucketName, long timestamp, double latitude, double longitude, boolean isFrontCamera) {
        this.embedding = embedding;
        this.uri = uri;
        this.bucketName = bucketName;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isFrontCamera = isFrontCamera;
    }

    protected HashItem(Parcel in) {
        embedding = in.createFloatArray();
        uri = in.readParcelable(Uri.class.getClassLoader());
        bucketName = in.readString();
        timestamp = in.readLong();
        latitude = in.readDouble();
        longitude = in.readDouble();
        isFrontCamera = in.readByte() != 0;
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
        dest.writeFloatArray(embedding);
        dest.writeParcelable(uri, flags);
        dest.writeString(bucketName);
        dest.writeLong(timestamp);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeByte((byte) (isFrontCamera ? 1 : 0));
    }
}