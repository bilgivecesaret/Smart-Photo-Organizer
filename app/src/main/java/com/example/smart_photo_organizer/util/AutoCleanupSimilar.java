package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AutoCleanupSimilar {

    private static final double AI_THRESHOLD = 0.80;
    private final Context context;

    public AutoCleanupSimilar(Context context) {
        this.context = context.getApplicationContext();
    }

    public List<Uri> findAllSimilarUris() {

        List<Uri> allImages = loadAllImagesSync();
        List<Uri> result = new ArrayList<>();

        int n = allImages.size();
        if (n == 0) return result;

        AIEmbeddingUtil ai = new AIEmbeddingUtil(context);
        float[][] embeddings = new float[n][];

        for (int i = 0; i < n; i++) {
            embeddings[i] = ai.getEmbedding(context, allImages.get(i));
        }

        UnionFind uf = new UnionFind(n);

        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {

                if (embeddings[i] == null || embeddings[j] == null) continue;

                double similarity =
                        AIEmbeddingUtil.cosineSimilarity(
                                embeddings[i], embeddings[j]);

                if (similarity > AI_THRESHOLD) {
                    uf.union(i, j);
                }
            }
        }

        Map<Integer, List<Uri>> clusters = new HashMap<>();

        for (int i = 0; i < n; i++) {
            int root = uf.find(i);
            clusters.putIfAbsent(root, new ArrayList<>());
            clusters.get(root).add(allImages.get(i));
        }

        for (List<Uri> cluster : clusters.values()) {
            if (cluster.size() > 1) {
                result.addAll(cluster);
            }
        }

        return result;
    }
    private List<Uri> loadAllImagesSync() {
        List<Uri> allImages = new ArrayList<>();

        Uri collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE
        };

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    Uri uri = Uri.withAppendedPath(collection, String.valueOf(id));
                    allImages.add(uri);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return allImages;
    }
}