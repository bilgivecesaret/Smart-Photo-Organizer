package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.HashItem;

import java.io.File;
import java.util.ArrayList;

public class LoadingImage {
    static ArrayList<HashItem> allImages = new ArrayList<>();
    static ArrayList<String> existingPaths = new ArrayList<>();
    public static ArrayList<HashItem> loadAllImages(Context context) {
        allImages.clear();
        existingPaths.clear();

        // 1️⃣ MediaStore Images
        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        try (Cursor cursor = context.getContentResolver().query(
                baseUri,
                new String[]{
                        MediaStore.MediaColumns._ID,
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME
                },
                MediaStore.MediaColumns.MIME_TYPE + " LIKE ?",
                new String[]{"image/%"},
                MediaStore.MediaColumns.DATE_ADDED + " DESC"
        )) {
            if (cursor == null) return allImages;

            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                Uri imageUri = Uri.withAppendedPath(baseUri, String.valueOf(id));

                String hash = ImagePHash.calculateHash(context, imageUri);
                String bucket = cursor.getString(1);
                allImages.add(
                        new HashItem(hash, imageUri, bucket)
                );

                if (!hash.isEmpty()) {
                    allImages.add(new HashItem(hash, imageUri, bucket));
                }
            }
            cursor.close();
        }

        // 2️⃣ MediaStore Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try (Cursor cursor = context.getContentResolver().query(
                    baseUri,
                    new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE},
                    MediaStore.MediaColumns.MIME_TYPE + " LIKE ?",
                    new String[]{"image/%"},
                    MediaStore.MediaColumns.DATE_ADDED + " DESC"
            )) {
                if (cursor == null) return allImages;

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    Uri imageUri = Uri.withAppendedPath(baseUri, String.valueOf(id));

                    String hash = ImagePHash.calculateHash(context, imageUri);
                    String bucket = cursor.getString(1);
                    if (!hash.isEmpty()) {
                        allImages.add(new HashItem(hash, imageUri, bucket));
                    }
                }
                cursor.close();
            }
        }

        // 3️⃣ Dosya sistemi Downloads klasörü
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            File[] files = downloadDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            });

            if (files != null) {
                for (File f : files) {
                    String path = f.getAbsolutePath();
                    if (!existingPaths.contains(path)) {
                        existingPaths.add(path);
                        Uri uri = Uri.fromFile(f);
                        String hash = ImagePHash.calculateHash(context, uri);
                        String bucket = "Downloads";
                        allImages.add(new HashItem(hash, uri, bucket));

                        // MediaScanner ile indexle
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaScannerConnection.scanFile(
                                    context,
                                    new String[]{path},
                                    new String[]{"image/*"},
                                    null
                            );
                        }
                    }
                }
            }
        }

        return allImages;
    }

}
