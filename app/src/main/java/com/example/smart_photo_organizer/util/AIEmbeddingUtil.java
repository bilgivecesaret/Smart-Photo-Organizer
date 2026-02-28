package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class AIEmbeddingUtil {

    private static final int INPUT_SIZE = 224;
    private static final int EMBEDDING_SIZE = 1792;

    private Interpreter interpreter;

    public AIEmbeddingUtil(Context context) {
        try {
            interpreter = new Interpreter(loadModelFile(context));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile(Context context) throws IOException {

        AssetFileDescriptor fileDescriptor =
                context.getAssets().openFd("mobilenet-v2-tensorflow2-140-224-feature-vector.tflite");

        FileInputStream inputStream =
                new FileInputStream(fileDescriptor.getFileDescriptor());

        FileChannel fileChannel = inputStream.getChannel();

        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();

        return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                startOffset,
                declaredLength
        );
    }

    public float[] getEmbedding(Context context, Uri uri) {
        Log.d("AIEmbedding", "getEmbedding START");
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

            ByteBuffer input = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3);
            input.order(ByteOrder.nativeOrder());

            for (int y = 0; y < INPUT_SIZE; y++) {
                for (int x = 0; x < INPUT_SIZE; x++) {
                    int pixel = scaled.getPixel(x, y);
                    input.putFloat(((pixel >> 16) & 0xff) / 255f);
                    input.putFloat(((pixel >> 8) & 0xff) / 255f);
                    input.putFloat((pixel & 0xff) / 255f);
                }
            }

            float[][] output = new float[1][EMBEDDING_SIZE];

            interpreter.run(input, output);
            int[] shape = interpreter.getOutputTensor(0).shape();
            Log.d("AIEmbedding", "Output shape: " + Arrays.toString(shape));

            bitmap.recycle();
            scaled.recycle();
            is.close();

            return normalize(output[0]);

        } catch (Exception e) {
            Log.e("AIEmbedding", "Embedding FAILED", e);
            return null;
        }
    }

    private float[] normalize(float[] vector) {
        double sum = 0;
        for (float v : vector) sum += v * v;
        double norm = Math.sqrt(sum);

        for (int i = 0; i < vector.length; i++)
            vector[i] /= norm;

        return vector;
    }

    public static double cosineSimilarity(float[] a, float[] b) {

        if (a == null || b == null) {
            return 0.0;
        }

        if (a.length != b.length) {
            return 0.0;
        }

        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }

        return sum;
    }
}