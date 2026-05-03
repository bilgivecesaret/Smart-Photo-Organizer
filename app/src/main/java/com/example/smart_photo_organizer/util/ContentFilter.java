package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.HashItem;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContentFilter {

    public interface Callback {
        void onResult(List<HashItem> result);
    }

    private final ImageLabeler labeler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Set<String> targetKeywords;

    public ContentFilter(Set<String> targetKeywords) {
        this.targetKeywords = targetKeywords;

        ImageLabelerOptions options = new ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.65f)
                .build();

        labeler = ImageLabeling.getClient(options);
    }

    public void filter(Context context, List<HashItem> input, Callback callback) {

        executor.execute(() -> {

            List<HashItem> result = new ArrayList<>();

            for (HashItem item : input) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(), item.uri);

                    Bitmap scaled = scaleBitmap(bitmap, 640);
                    bitmap.recycle();

                    InputImage image = InputImage.fromBitmap(scaled, 0);
                    CountDownLatch latch = new CountDownLatch(1);

                    labeler.process(image)
                            .addOnSuccessListener(labels -> {
                                for (ImageLabel label : labels) {
                                    String text = label.getText().toLowerCase();
                                    for (String keyword : targetKeywords) {
                                        if (text.contains(keyword)) {
                                            synchronized (result) {
                                                result.add(item);
                                            }
                                            break;
                                        }
                                    }
                                }
                                latch.countDown();
                            })
                            .addOnFailureListener(e -> latch.countDown());

                    latch.await();
                    scaled.recycle();

                } catch (Exception ignored) {}
            }

            callback.onResult(result);
        });
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