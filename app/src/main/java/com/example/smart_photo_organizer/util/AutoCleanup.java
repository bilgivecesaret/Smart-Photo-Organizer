package com.example.smart_photo_organizer.util;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCleanup {

    private static final String TAG = "AutoCleanup";
    public static void runAutoCleanupBackground(Context context) {
        try {
            Log.d(TAG, "Auto cleanup started...");

            List<Uri> allPhotos = getAllPhotoUris(context);
            List<List<Uri>> similarGroups = findSimilarGroups(context, allPhotos);

            for (List<Uri> group : similarGroups) {
                deletePhotos(context, group);
            }

            Log.d(TAG, "Auto cleanup finished. Deleted similar photos.");
        } catch (Exception e) {
            Log.e(TAG, "Auto cleanup failed", e);
        }
    }

    /**
     * Tüm cihazdaki fotoğraf URI'lerini döndürür
     */
    private static List<Uri> getAllPhotoUris(Context context) {
        List<Uri> photoUris = new ArrayList<>();

        ContentResolver resolver = context.getContentResolver();
        String[] projection = { MediaStore.Images.Media._ID };
        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);

        try (android.database.Cursor cursor = resolver.query(collection, projection,
                null, null, null)) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    Uri uri = Uri.withAppendedPath(collection, String.valueOf(id));
                    photoUris.add(uri);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error fetching photos", e);
        }

        return photoUris;
    }
    private static List<List<Uri>> findSimilarGroups(Context context, List<Uri> photos) {
        List<List<Uri>> groups = new ArrayList<>();
        if (photos.isEmpty()) return groups;

        AIEmbeddingUtil ai = new AIEmbeddingUtil(context);
        int n = photos.size();
        float[][] embeddings = new float[n][];

        // Embeddingleri al
        for (int i = 0; i < n; i++) {
            embeddings[i] = ai.getEmbedding(context, photos.get(i));
        }

        // Union-Find kullanarak benzer fotoğrafları grupla
        UnionFind uf = new UnionFind(n);
        final double THRESHOLD = 0.80; // AI benzerlik eşik değeri

        for (int i = 0; i < n; i++) {
            if (embeddings[i] == null) continue;
            for (int j = i + 1; j < n; j++) {
                if (embeddings[j] == null) continue;
                double similarity = AIEmbeddingUtil.cosineSimilarity(embeddings[i], embeddings[j]);
                if (similarity > THRESHOLD) {
                    uf.union(i, j);
                }
            }
        }

        // Grupları oluştur
        Map<Integer, List<Uri>> map = new HashMap<>();
        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            map.putIfAbsent(root, new ArrayList<>());
            map.get(root).add(photos.get(i));
        }

        // 1’den fazla fotoğrafı olan grupları ekle
        for (List<Uri> cluster : map.values()) {
            if (cluster.size() > 1) {
                groups.add(cluster);
            }
        }

        return groups;
    }
    private static void deletePhotos(Context context, List<Uri> photos) {
        ContentResolver resolver = context.getContentResolver();
        for (Uri uri : photos) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    resolver.delete(uri, null);
                } else {
                    resolver.delete(uri, null, null);
                }
                Log.d(TAG, "Deleted photo: " + uri.toString());
            } catch (Exception e) {
                Log.e(TAG, "Failed to delete photo: " + uri, e);
            }
        }
    }
}