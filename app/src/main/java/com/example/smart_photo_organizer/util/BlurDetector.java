package com.example.smart_photo_organizer.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.imgproc.Imgproc;

public class BlurDetector {

    private static final String TAG = "BlurDetector";

    private static final double BLUR_THRESHOLD = 80.0;

    private static final int TARGET_SIZE = 1000;

    public static boolean isBlurry(String imagePath) {
        Bitmap bitmap = null;
        Mat matImage = new Mat();
        Mat matGray = new Mat();
        Mat laplacianImage = new Mat();

        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imagePath, options);


            options.inSampleSize = calculateInSampleSize(options, TARGET_SIZE, TARGET_SIZE);
            options.inJustDecodeBounds = false;

            bitmap = BitmapFactory.decodeFile(imagePath, options);
            if (bitmap == null) return false;

            Utils.bitmapToMat(bitmap, matImage);
            Imgproc.cvtColor(matImage, matGray, Imgproc.COLOR_BGR2GRAY);

            Imgproc.GaussianBlur(matGray, matGray, new org.opencv.core.Size(3, 3), 0);

            Imgproc.Laplacian(matGray, laplacianImage, CvType.CV_64F);

            MatOfDouble mean = new MatOfDouble();
            MatOfDouble stdDev = new MatOfDouble();
            Core.meanStdDev(laplacianImage, mean, stdDev);

            double variance = Math.pow(stdDev.get(0, 0)[0], 2);

            return variance < BLUR_THRESHOLD;

        } catch (Exception e) {
            Log.e(TAG, "Error: " + imagePath, e);
            return false;
        } finally {

            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            matImage.release();
            matGray.release();
            laplacianImage.release();
        }
    }
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
