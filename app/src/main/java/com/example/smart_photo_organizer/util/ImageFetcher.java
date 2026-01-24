package com.example.smart_photo_organizer.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.HashItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ImageFetcher {

    public static ArrayList<HashItem> loadAllImages(Context context) {

        ArrayList<HashItem> result = new ArrayList<>();
        Set<String> addedUris = new HashSet<>();

        // 1️⃣ DCIM + Pictures + WhatsApp + SD Card
        loadFromImages(context, result, addedUris);

        // 2️⃣ Download klasörü
        loadFromDownloads(context, result, addedUris);

        return result;
    }

    // IMAGES (DCIM / Pictures)
    private static void loadFromImages(
            Context context,
            ArrayList<HashItem> out,
            Set<String> addedUris
    ) {

        Uri uri = MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
        );

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.RELATIVE_PATH
        };

        String selection = MediaStore.MediaColumns.MIME_TYPE + " LIKE ?";
        String[] args = {"image/%"};

        try (Cursor cursor = context.getContentResolver().query(
                uri,
                projection,
                selection,
                args,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {

            if (cursor == null) return;

            int idCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media._ID
            );
            int bucketCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME
            );
            int pathCol = cursor.getColumnIndexOrThrow(
                    MediaStore.MediaColumns.RELATIVE_PATH
            );

            while (cursor.moveToNext()) {

                long id = cursor.getLong(idCol);
                Uri imageUri = ContentUris.withAppendedId(uri, id);

                if (!addedUris.add(imageUri.toString())) continue;

                String bucket = cursor.getString(bucketCol);
                String relativePath = cursor.getString(pathCol);

                String folderName = resolveFolderName(bucket, relativePath);

                out.add(new HashItem(null, imageUri, folderName));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // DOWNLOADS (image/*.jpg)
    private static void loadFromDownloads(
            Context context,
            ArrayList<HashItem> out,
            Set<String> addedUris
    ) {

        Uri uri = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
                MediaStore.Downloads._ID,
                MediaStore.Downloads.RELATIVE_PATH,
                MediaStore.Downloads.DISPLAY_NAME
        };

        try (Cursor cursor = context.getContentResolver().query(
                uri,
                projection,
                null,
                null,
                MediaStore.Downloads.DATE_ADDED + " DESC"
        )) {

            if (cursor == null) return;

            int idCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Downloads._ID
            );
            int pathCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Downloads.RELATIVE_PATH
            );
            int nameCol = cursor.getColumnIndexOrThrow(
                    MediaStore.Downloads.DISPLAY_NAME
            );

            while (cursor.moveToNext()) {

                String name = cursor.getString(nameCol);
                if (name == null) continue;

                String lower = name.toLowerCase();

                // uzantıya göre filtre
                if (!(lower.endsWith(".jpg")
                        || lower.endsWith(".jpeg")
                        || lower.endsWith(".png")
                        || lower.endsWith(".webp"))) {
                    continue;
                }

                long id = cursor.getLong(idCol);
                Uri imageUri = ContentUris.withAppendedId(uri, id);

                if (!addedUris.add(imageUri.toString())) continue;

                String relativePath = cursor.getString(pathCol);

                out.add(new HashItem(
                        null,
                        imageUri,
                        resolveFolderName("Download", relativePath)
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static String resolveFolderName(
            String bucketName,
            String relativePath
    ) {

        if (bucketName != null && !bucketName.trim().isEmpty()) {
            return bucketName;
        }

        if (relativePath != null) {
            String[] parts = relativePath.split("/");
            if (parts.length > 0) {
                return parts[parts.length - 1];
            }
        }

        return "Unknown";
    }
}
