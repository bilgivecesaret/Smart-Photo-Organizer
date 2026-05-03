package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;

import com.example.smart_photo_organizer.model.HashItem;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AnimalDetectionUtil {

    // COCO hayvan kategorisi ID'leri (1-indexed)
    private static final int[] ANIMAL_CLASSES = {
            16, 17, 18, 19, 20, 21, 22, 23, 24, 25
            // bird, cat, dog, horse, sheep, cow, elephant, bear, zebra, giraffe
    };

    private static final int INPUT_SIZE = 300;
    private static final float CONFIDENCE_THRESHOLD = 0.4f;
    private static final int MAX_DETECTIONS = 10;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface Callback {
        void onResult(List<HashItem> result);
    }

    public void filter(Context context, List<HashItem> input, Callback callback) {

        executor.execute(() -> {

            List<HashItem> result = new ArrayList<>();

            try {
                MappedByteBuffer modelBuffer =
                        FileUtil.loadMappedFile(context, "ml/detect.tflite");

                Interpreter tflite = new Interpreter(modelBuffer);

                for (HashItem item : input) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                context.getContentResolver(), item.uri);

                        Bitmap scaled = Bitmap.createScaledBitmap(
                                bitmap, INPUT_SIZE, INPUT_SIZE, true);
                        bitmap.recycle();

                        ByteBuffer inputBuffer = bitmapToByteBuffer(scaled);
                        scaled.recycle();

                        float[][][] boxes = new float[1][MAX_DETECTIONS][4];
                        float[][] classes = new float[1][MAX_DETECTIONS];
                        float[][] scores = new float[1][MAX_DETECTIONS];
                        float[] numDetections = new float[1];

                        Object[] inputs = {inputBuffer};
                        java.util.Map<Integer, Object> outputs = new java.util.TreeMap<>();
                        outputs.put(0, boxes);
                        outputs.put(1, classes);
                        outputs.put(2, scores);
                        outputs.put(3, numDetections);

                        tflite.runForMultipleInputsOutputs(inputs, outputs);

                        // SADECE İLK FOTOĞRAF İÇİN LOG AL VE DUR
                        android.util.Log.d("AnimalDebug", "numDetections: " + numDetections[0]);
                        for (int i = 0; i < (int) numDetections[0]; i++) {
                            android.util.Log.d("AnimalDebug",
                                    "i=" + i
                                            + " class=" + classes[0][i]
                                            + " score=" + scores[0][i]);
                        }
                        break; // sadece ilk fotoğraf

                    } catch (Exception e) {
                        android.util.Log.e("AnimalDebug", "Hata: " + e.getMessage());
                        break;
                    }
                }

                tflite.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

            callback.onResult(result);
        });
    }

    private ByteBuffer bitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        for (int pixel : pixels) {
            buffer.put((byte) ((pixel >> 16) & 0xFF)); // R
            buffer.put((byte) ((pixel >> 8) & 0xFF));  // G
            buffer.put((byte) (pixel & 0xFF));          // B
        }

        buffer.rewind();
        return buffer;
    }
}