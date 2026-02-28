package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

public class ImageFeatureExtractor {

    private static final int SIZE = 32;

    public static float[] extractFeature(Context context, Uri uri) {

        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true);

            float[] feature = new float[SIZE * SIZE];
            int index = 0;

            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int pixel = scaled.getPixel(x, y);
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    feature[index++] = (r + g + b) / 3f;
                }
            }

            bitmap.recycle();
            scaled.recycle();
            is.close();

            return feature;

        } catch (Exception e) {
            return null;
        }
    }

    public static double l2Distance(float[] a, float[] b) {

        double sum = 0;

        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }

        return Math.sqrt(sum);
    }
}