package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.HashItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class LoadingImage {

    public static ArrayList<HashItem> loadAllImages(Context context) {

        ArrayList<HashItem> allImages = new ArrayList<>();
        Set<String> addedUris = new HashSet<>();

        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.DATA
        };

        String selection = MediaStore.MediaColumns.MIME_TYPE + " LIKE ?";
        String[] selectionArgs = new String[]{"image/%"};

        try (Cursor cursor = context.getContentResolver().query(
                baseUri,
                projection,
                selection,
                selectionArgs,
                MediaStore.MediaColumns.DATE_ADDED + " DESC"
        )) {

            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int bucketCol = cursor.getColumnIndexOrThrow(
                        MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int dataCol = cursor.getColumnIndexOrThrow(
                        MediaStore.MediaColumns.DATA);

                while (cursor.moveToNext()) {

                    long id = cursor.getLong(idCol);
                    Uri imageUri = Uri.withAppendedPath(baseUri, String.valueOf(id));

                    if (addedUris.contains(imageUri.toString())) continue;
                    addedUris.add(imageUri.toString());

                    String bucketName = cursor.getString(bucketCol);
                    String dataPath = cursor.getString(dataCol);
                    String folderName = resolveFolderName(bucketName, dataPath);
                    allImages.add(new HashItem(null, imageUri, folderName));
                }
            }
        }

        File downloadsDir =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS);

        if (downloadsDir.exists() && downloadsDir.isDirectory()) {
            File[] files = downloadsDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg")
                        || lower.endsWith(".jpeg")
                        || lower.endsWith(".png");
            });

            if (files != null) {
                for (File file : files) {

                    Uri uri = Uri.fromFile(file);
                    if (addedUris.contains(uri.toString())) continue;
                    addedUris.add(uri.toString());
                }
            }
        }

        return allImages;
    }

    private static String resolveFolderName(String bucketName, String dataPath) {

        if (bucketName != null && !bucketName.trim().isEmpty()) {
            return bucketName;
        }

        if (dataPath != null) {
            File file = new File(dataPath);
            File parent = file.getParentFile();
            if (parent != null) {
                return parent.getName();
            }
        }

        return "Unknown";
    }
}
