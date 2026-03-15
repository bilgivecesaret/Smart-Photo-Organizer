package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.provider.MediaStore;
import android.util.Log;

import com.example.smart_photo_organizer.model.HashItem;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HumanDetectionUtil {

    private static final String TAG = "HumanDetectionUtil";
    private static final String MODEL_PATH = "ml/detect.tflite";

    private Interpreter tflite;
    private int inputSize = 300; // Model input size, 300x300
    private float[][][] outputLocations;
    private float[][] outputClasses;
    private float[][] outputScores;
    private float[] numDetections;

    /** Constructor: Model yükle ve interpreter hazırla */
    public HumanDetectionUtil(Context context) {
        try {
            MappedByteBuffer tfliteModel = loadModelFile(context);
            tflite = new Interpreter(tfliteModel);
            Log.d(TAG, "TFLite Model loaded successfully.");

            // Output tensorları modelin output yapısına göre oluştur
            outputLocations = new float[1][10][4]; // 1 batch, 10 detections, 4 coords
            outputClasses = new float[1][10];
            outputScores = new float[1][10];
            numDetections = new float[1];
        } catch (IOException e) {
            Log.e(TAG, "Model yüklenemedi: " + MODEL_PATH, e);
        }
    }

    /** Modeli assets klasöründen yükle */
    private MappedByteBuffer loadModelFile(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return inputStream.getChannel().map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Bitmap'i ByteBuffer'a çevir */
    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[inputSize * inputSize];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int pixel = 0;
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                final int val = intValues[pixel++];
                byteBuffer.putFloat(((val >> 16) & 0xFF) / 255.0f); // R
                byteBuffer.putFloat(((val >> 8) & 0xFF) / 255.0f);  // G
                byteBuffer.putFloat((val & 0xFF) / 255.0f);         // B
            }
        }
        return byteBuffer;
    }

    /** Tek bir fotoğraf üzerinde insan tespiti yap */
    public List<DetectionResult> detectHumans(Bitmap bitmap) {
        if (tflite == null) {
            Log.e(TAG, "TFLite interpreter null, detection skipped.");
            return new ArrayList<>();
        }

        // Bitmap'i modele uygun boyuta getir
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, false);
        ByteBuffer inputBuffer = convertBitmapToByteBuffer(resizedBitmap);

        // TFLite model output map'i
        Object[] inputArray = {inputBuffer};
        @SuppressWarnings("unchecked")
        Map<Integer, Object> outputMap = new HashMap<>();
        outputMap.put(0, outputLocations);
        outputMap.put(1, outputClasses);
        outputMap.put(2, outputScores);
        outputMap.put(3, numDetections);

        tflite.runForMultipleInputsOutputs(inputArray, outputMap);

        // Sonuçları listeye çevir
        List<DetectionResult> results = new ArrayList<>();
        int detections = Math.min(10, (int) numDetections[0]);
        for (int i = 0; i < detections; i++) {
            float score = outputScores[0][i];
            if (score > 0.5) { // Eşik, ihtiyacına göre değiştir
                float top = outputLocations[0][i][0] * bitmap.getHeight();
                float left = outputLocations[0][i][1] * bitmap.getWidth();
                float bottom = outputLocations[0][i][2] * bitmap.getHeight();
                float right = outputLocations[0][i][3] * bitmap.getWidth();
                results.add(new DetectionResult(new RectF(left, top, right, bottom), score));
            }
        }
        return results;
    }

    /** Detection sonucunu saklamak için helper class */
    public static class DetectionResult {
        public final RectF location;
        public final float confidence;

        public DetectionResult(RectF location, float confidence) {
            this.location = location;
            this.confidence = confidence;
        }
    }

    /** 🔹 Yeni Fonksiyon: List<HashItem> üzerinden insan filtreleme */
    public static List<HashItem> filterPhotosWithHumans(Context context, List<HashItem> photos) {
        HumanDetectionUtil detector = new HumanDetectionUtil(context);
        List<HashItem> result = new ArrayList<>();

        for (HashItem item : photos) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), item.uri);
                List<DetectionResult> detections = detector.detectHumans(bitmap);
                if (!detections.isEmpty()) {
                    result.add(item);
                }
            } catch (Exception e) {
                Log.e(TAG, "Fotoğraf işlenemedi: " + item.uri, e);
            }
        }

        return result;
    }
}