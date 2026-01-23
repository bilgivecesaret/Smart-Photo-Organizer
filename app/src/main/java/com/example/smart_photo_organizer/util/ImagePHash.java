package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

public class ImagePHash {

    private static final int SIZE = 32;
    private static final int SMALL_SIZE = 8;

    public static String calculateHash(Context context, Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 8;
            opts.inPreferredConfig = Bitmap.Config.RGB_565;

            Bitmap bitmap = BitmapFactory.decodeStream(is, null, opts);
            bitmap = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, false);

            double[][] gray = new double[SIZE][SIZE];

            for (int x = 0; x < SIZE; x++) {
                for (int y = 0; y < SIZE; y++) {
                    int pixel = bitmap.getPixel(x, y);
                    int r = (pixel >> 16) & 0xff;
                    int g = (pixel >> 8) & 0xff;
                    int b = pixel & 0xff;
                    gray[x][y] = (r + g + b) / 3.0;
                }
            }

            double[][] dct = applyDCT(gray);

            double total = 0;
            for (int x = 0; x < SMALL_SIZE; x++) {
                for (int y = 0; y < SMALL_SIZE; y++) {
                    total += dct[x][y];
                }
            }

            double avg = total / (SMALL_SIZE * SMALL_SIZE);

            StringBuilder hash = new StringBuilder();
            for (int x = 0; x < SMALL_SIZE; x++) {
                for (int y = 0; y < SMALL_SIZE; y++) {
                    hash.append(dct[x][y] > avg ? "1" : "0");
                }
            }

            return hash.toString();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static int hammingDistance(String h1, String h2) {
        int dist = 0;
        for (int i = 0; i < h1.length(); i++) {
            if (h1.charAt(i) != h2.charAt(i)) {
                dist++;
            }
        }
        return dist;
    }

    private static double[][] applyDCT(double[][] f) {
        int N = SIZE;
        double[][] F = new double[N][N];

        for (int u = 0; u < N; u++) {
            for (int v = 0; v < N; v++) {
                double sum = 0.0;
                for (int i = 0; i < N; i++) {
                    for (int j = 0; j < N; j++) {
                        sum += f[i][j] *
                                Math.cos((2 * i + 1) * u * Math.PI / (2 * N)) *
                                Math.cos((2 * j + 1) * v * Math.PI / (2 * N));
                    }
                }
                double cu = (u == 0) ? 1 / Math.sqrt(2) : 1;
                double cv = (v == 0) ? 1 / Math.sqrt(2) : 1;
                F[u][v] = 0.25 * cu * cv * sum;
            }
        }
        return F;
    }
}

