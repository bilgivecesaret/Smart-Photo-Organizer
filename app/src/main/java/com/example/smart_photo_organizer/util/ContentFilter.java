package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.util.Log;

import com.example.smart_photo_organizer.model.HashItem;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContentFilter {

    public interface Callback {
        void onResult(List<HashItem> result);
    }

    private final Set<String> targetKeywords;

    public ContentFilter(Set<String> targetKeywords) {
        this.targetKeywords = targetKeywords;
    }

    public void filter(Context context, List<HashItem> input, Callback callback) {

        List<HashItem> limited = input.size() > 100
                ? input.subList(0, 100) : input;

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        List<HashItem> result = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch allDone = new CountDownLatch(limited.size());

        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.5f)
                .build();

        for (HashItem item : limited) {
            executor.execute(() -> {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(), item.uri);

                    Bitmap scaled = scaleBitmap(bitmap, 320);
                    bitmap.recycle();

                    InputImage image = InputImage.fromBitmap(scaled, 0);
                    ImageLabeler labeler = ImageLabeling.getClient(options);
                    CountDownLatch latch = new CountDownLatch(1);

                    labeler.process(image)
                            .addOnSuccessListener(labels -> {

                                // Tüm fotoğraflar için logla
                                StringBuilder sb = new StringBuilder();
                                sb.append(item.uri.getLastPathSegment()).append(" → ");
                                for (ImageLabel label : labels) {
                                    sb.append(label.getText())
                                            .append("(")
                                            .append(String.format("%.2f", label.getConfidence()))
                                            .append(") ");
                                }
                                Log.d("MLKitDebug", sb.toString());

                                for (ImageLabel label : labels) {
                                    String text = label.getText().toLowerCase();
                                    for (String keyword : targetKeywords) {
                                        if (text.contains(keyword)) {
                                            result.add(item);
                                            break;
                                        }
                                    }
                                }
                                latch.countDown();
                            })
                            .addOnFailureListener(e -> latch.countDown());

                    latch.await();
                    scaled.recycle();
                    labeler.close();

                } catch (Exception ignored) {
                } finally {
                    allDone.countDown();
                }
            });
        }

        new Thread(() -> {
            try {
                allDone.await();
            } catch (Exception ignored) {}
            executor.shutdown();
            callback.onResult(new ArrayList<>(result));
        }).start();
    }

    private Bitmap scaleBitmap(Bitmap bitmap, int maxSize) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width <= maxSize && height <= maxSize) return bitmap;
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        return Bitmap.createScaledBitmap(bitmap,
                Math.round(width * scale), Math.round(height * scale), true);
    }
}