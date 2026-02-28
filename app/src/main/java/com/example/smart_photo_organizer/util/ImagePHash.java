package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

public class ImagePHash {

    private static final int SIZE = 32;
    private static final int SMALL_SIZE = 8;
    private static final double[][] COS = precomputeCos();

    public static long calculateHash(Context context, Uri uri) {
        Bitmap bitmap = null;
        InputStream is = null;
        try {
            is = context.getContentResolver().openInputStream(uri);
            bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) return 0L;
            bitmap = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, false);

            int[] pixels = new int[SIZE * SIZE];
            bitmap.getPixels(pixels, 0, SIZE, 0, 0, SIZE, SIZE);

            double[][] gray = new double[SIZE][SIZE];
            int idx = 0;

            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    int p = pixels[idx++];
                    int r = (p >> 16) & 0xff;
                    int g = (p >> 8) & 0xff;
                    int b = p & 0xff;
                    gray[x][y] = (r + g + b) / 3.0;
                }
            }

            double[][] dct = applyDCT(gray);

            double avg = 0;
            for (int x = 0; x < SMALL_SIZE; x++) {
                for (int y = 0; y < SMALL_SIZE; y++) {
                    avg += dct[x][y];
                }
            }

            avg /= SMALL_SIZE * SMALL_SIZE;

            long hash = 0L;
            int bit = 0;

            for (int x = 0; x < SMALL_SIZE; x++) {
                for (int y = 0; y < SMALL_SIZE; y++) {
                    if (dct[x][y] > avg) {
                        hash |= (1L << bit);
                    }
                    bit++;
                }
            }

            return hash;

        } catch (Exception e) {
            return 0L;
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception ignored) {}
            if (bitmap != null) bitmap.recycle();
        }
    }

    private static double[][] applyDCT(double[][] f) {
        double[][] F = new double[SIZE][SIZE];

        for (int u = 0; u < SIZE; u++) {
            for (int v = 0; v < SIZE; v++) {
                double sum = 0;
                for (int i = 0; i < SIZE; i++) {
                    for (int j = 0; j < SIZE; j++) {
                        sum += f[i][j] * COS[i][u] * COS[j][v];
                    }
                }
                F[u][v] = sum;
            }
        }
        return F;
    }

    private static double[][] precomputeCos() {
        double[][] cos = new double[SIZE][SIZE];
        for (int i = 0; i < SIZE; i++) {
            for (int u = 0; u < SIZE; u++) {
                cos[i][u] =
                        Math.cos((2 * i + 1) * u * Math.PI / (2 * SIZE));
            }
        }
        return cos;
    }

    public static int hammingDistance(long hash1, long hash2) {
        return Long.bitCount(hash1 ^ hash2);
    }
}
