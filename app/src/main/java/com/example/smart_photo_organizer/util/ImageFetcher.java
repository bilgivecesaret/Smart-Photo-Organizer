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

            // ===== IMAGES =====
            Uri imagesUri = MediaStore.Images.Media.getContentUri(
                    MediaStore.VOLUME_EXTERNAL
            );

            String[] projection = {
                    MediaStore.Images.Media._ID,
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH
            };

            try (Cursor cursor = context.getContentResolver().query(
                    imagesUri,
                    projection,
                    MediaStore.MediaColumns.MIME_TYPE + " LIKE ?",
                    new String[]{"image/%"},
                    MediaStore.Images.Media.DATE_ADDED + " DESC"
            )) {

                if (cursor != null) {
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
                        Uri uri = ContentUris.withAppendedId(imagesUri, id);

                        if (!addedUris.add(uri.toString())) continue;

                        batch.add(new HashItem(
                                0L,
                                uri,
                                resolveFolderName(
                                        cursor.getString(bucketCol),
                                        cursor.getString(pathCol)
                                )
                        ));

                        if (batch.size() >= batchSize) {
                            List<HashItem> deliver = new ArrayList<>(batch);
                            batch.clear();
                            mainHandler.post(() -> callback.onBatch(deliver));
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            // ===== DOWNLOADS =====
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri downloadUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI;

                String[] downloadProjection = {
                        MediaStore.Downloads._ID,
                        MediaStore.Downloads.DISPLAY_NAME,
                        MediaStore.Downloads.RELATIVE_PATH
                };

                try (Cursor cursor = context.getContentResolver().query(
                        downloadUri,
                        downloadProjection,
                        null,
                        null,
                        MediaStore.Downloads.DATE_ADDED + " DESC"
                )) {

                    if (cursor != null) {
                        int idCol = cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads._ID
                        );
                        int nameCol = cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads.DISPLAY_NAME
                        );
                        int pathCol = cursor.getColumnIndexOrThrow(
                                MediaStore.Downloads.RELATIVE_PATH
                        );

                        while (cursor.moveToNext()) {
                            String name = cursor.getString(nameCol);
                            if (name == null) continue;

                            String lower = name.toLowerCase();
                            if (!(lower.endsWith(".jpg")
                                    || lower.endsWith(".jpeg")
                                    || lower.endsWith(".png")
                                    || lower.endsWith(".webp"))) continue;

                            long id = cursor.getLong(idCol);
                            Uri uri = ContentUris.withAppendedId(downloadUri, id);

                            if (!addedUris.add(uri.toString())) continue;

                            batch.add(new HashItem(
                                    0L,
                                    uri,
                                    resolveFolderName("Download",
                                            cursor.getString(pathCol))
                            ));

                            if (batch.size() >= batchSize) {
                                List<HashItem> deliver = new ArrayList<>(batch);
                                batch.clear();
                                mainHandler.post(() -> callback.onBatch(deliver));
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!batch.isEmpty()) {
                mainHandler.post(() -> callback.onBatch(batch));
            }

            mainHandler.post(callback::onComplete);
        });
    }

    private static String resolveFolderName(
            String bucket,
            String relativePath
    ) {
        if (bucket != null && !bucket.isEmpty()) return bucket;
        if (relativePath != null) {
            String[] parts = relativePath.split("/");
            return parts[parts.length - 1];
        }
        return "Unknown";
    }
}
