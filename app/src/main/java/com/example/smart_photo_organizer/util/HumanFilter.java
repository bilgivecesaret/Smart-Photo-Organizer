package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.HashItem;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HumanFilter {

    private final FaceDetector detector;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HumanFilter() {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .build();

        detector = FaceDetection.getClient(options);
    }

    public interface Callback {
        void onResult(List<HashItem> result);
    }

    public void filter(Context context, List<HashItem> input, Callback callback) {

        executor.execute(() -> {

            List<HashItem> result = new ArrayList<>();

            for (HashItem item : input) {
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                            context.getContentResolver(),
                            item.uri
                    );

                    // Büyük fotoğrafları küçült — performans için
                    Bitmap scaled = scaleBitmap(bitmap, 640);
                    bitmap.recycle();

                    InputImage image = InputImage.fromBitmap(scaled, 0);

                    // Her fotoğraf için async sonucu bekle
                    CountDownLatch latch = new CountDownLatch(1);

                    detector.process(image)
                            .addOnSuccessListener(faces -> {
                                if (!faces.isEmpty()) {
                                    synchronized (result) {
                                        result.add(item);
                                    }
                                }
                                latch.countDown();
                            })
                            .addOnFailureListener(e -> latch.countDown());

                    latch.await(); // sonuç gelene kadar bekle
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
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
}