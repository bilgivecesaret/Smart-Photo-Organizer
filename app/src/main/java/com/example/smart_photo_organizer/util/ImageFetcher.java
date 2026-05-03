package com.example.smart_photo_organizer.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.HashItem;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class ImageFetcher {

    public interface ImageBatchCallback {
        void onBatch(List<HashItem> batch);
        void onComplete();
    }

    public static void loadAllImagesAsync(
            Context context,
            int batchSize,
            ImageBatchCallback callback
    ) {
        Executors.newSingleThreadExecutor().execute(() -> {

            Set<String> addedUris = new HashSet<>();
            List<HashItem> batch = new ArrayList<>();
            Handler mainHandler = new Handler(Looper.getMainLooper());

            Uri imagesUri;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                imagesUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }

            String[] projection = getProjection();

            try (Cursor cursor = context.getContentResolver().query(
                    imagesUri,
                    projection,
                    MediaStore.MediaColumns.MIME_TYPE + " LIKE ?",
                    new String[]{"image/%"},
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    int bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                    int pathCol = cursor.getColumnIndexOrThrow(
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                    ? MediaStore.MediaColumns.RELATIVE_PATH
                                    : MediaStore.Images.Media.DATA
                    );
                    int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN);
                    int latCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LATITUDE);
                    int lonCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.LONGITUDE);
                    int displayNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        Uri uri = ContentUris.withAppendedId(imagesUri, id);

                        long dateTaken = cursor.getLong(dateCol) / 1000;
                        double lat = cursor.getDouble(latCol);
                        double lon = cursor.getDouble(lonCol);
                        String displayName = cursor.getString(displayNameCol);

                        if (!addedUris.add(uri.toString())) continue;

                        String folder =
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                                        ? cursor.getString(pathCol)
                                        : new File(cursor.getString(pathCol)).getParent();

                        String bucketName = resolveFolderName(cursor.getString(bucketCol), folder);
                        boolean isFrontCamera = detectFrontCamera(bucketName, displayName);

                        batch.add(new HashItem(
                                null,
                                uri,
                                bucketName,
                                dateTaken,
                                lat,
                                lon,
                                isFrontCamera
                        ));

                        if (batch.size() >= batchSize) {
                            List<HashItem> deliver = new ArrayList<>(batch);
                            batch.clear();
                            mainHandler.post(() -> callback.onBatch(deliver));
                        }
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }

            // DOWNLOADS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri downloadUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

                String[] downloadProjection = {
                        MediaStore.Downloads._ID,
                        MediaStore.Downloads.DISPLAY_NAME,
                        MediaStore.Downloads.RELATIVE_PATH,
                        MediaStore.Downloads.DATE_ADDED
                };

                try (Cursor cursor = context.getContentResolver().query(
                        downloadUri,
                        downloadProjection,
                        null,
                        null,
                        MediaStore.Downloads.DATE_ADDED + " DESC"
                )) {
                    if (cursor != null) {
                        int idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID);
                        int nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME);
                        int pathCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH);
                        int dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED);

                        while (cursor.moveToNext()) {
                            String name = cursor.getString(nameCol);
                            if (name == null) continue;

                            String lower = name.toLowerCase();
                            if (!(lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                                    || lower.endsWith(".png") || lower.endsWith(".webp"))) continue;

                            long id = cursor.getLong(idCol);
                            Uri uri = ContentUris.withAppendedId(downloadUri, id);
                            long dateAdded = cursor.getLong(dateCol);

                            if (!addedUris.add(uri.toString())) continue;

                            batch.add(new HashItem(
                                    null,
                                    uri,
                                    resolveFolderName("Download", cursor.getString(pathCol)),
                                    dateAdded,
                                    0.0,
                                    0.0,
                                    false
                            ));

                            if (batch.size() >= batchSize) {
                                List<HashItem> deliver = new ArrayList<>(batch);
                                batch.clear();
                                mainHandler.post(() -> callback.onBatch(deliver));
                            }
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }

            if (!batch.isEmpty()) {
                mainHandler.post(() -> callback.onBatch(batch));
            }
            mainHandler.post(callback::onComplete);
        });
    }

    private static boolean detectFrontCamera(String bucketName, String displayName) {
        String bucket = bucketName != null ? bucketName.toLowerCase() : "";
        String name = displayName != null ? displayName.toLowerCase() : "";

        return bucket.contains("selfie") || bucket.contains("front")
                || name.contains("selfie") || name.contains("front");
    }

    private static String[] getProjection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.LATITUDE,
                    MediaStore.Images.Media.LONGITUDE,
                    MediaStore.Images.Media.DISPLAY_NAME
            };
        } else {
            return new String[]{
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_TAKEN,
                    MediaStore.Images.Media.LATITUDE,
                    MediaStore.Images.Media.LONGITUDE,
                    MediaStore.Images.Media.DISPLAY_NAME
            };
        }
    }

    private static String resolveFolderName(String bucket, String relativePath) {
        if (bucket != null && !bucket.isEmpty()) return bucket;
        if (relativePath != null) {
            String[] parts = relativePath.split("/");
            return parts[parts.length - 1];
        }
        return "Unknown";
    }
}