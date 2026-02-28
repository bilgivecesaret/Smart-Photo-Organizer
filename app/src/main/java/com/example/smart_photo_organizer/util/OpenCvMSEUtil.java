package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.InputStream;

public class OpenCvMSEUtil {

    private static final int TARGET_SIZE = 128;

    public static double calculateMSE(Context context, Uri uri1, Uri uri2) {

        Mat img1 = loadGrayMat(context, uri1);
        Mat img2 = loadGrayMat(context, uri2);

        if (img1 == null || img2 == null) return Double.MAX_VALUE;

        if (!img1.size().equals(img2.size())) {
            Imgproc.resize(img2, img2, img1.size());
        }

        Mat diff = new Mat();
        org.opencv.core.Core.absdiff(img1, img2, diff);
        diff.convertTo(diff, CvType.CV_32F);
        diff = diff.mul(diff);

        double mse = org.opencv.core.Core.sumElems(diff).val[0] /
                (img1.rows() * img1.cols());

        img1.release();
        img2.release();
        diff.release();

        return mse;
    }

    private static Mat loadGrayMat(Context context, Uri uri) {
        try {
            InputStream is = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            if (bitmap == null) return null;

            Bitmap scaled = Bitmap.createScaledBitmap(
                    bitmap,
                    TARGET_SIZE,
                    TARGET_SIZE,
                    true
            );

            Mat mat = new Mat();
            Utils.bitmapToMat(scaled, mat);
            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

            bitmap.recycle();
            scaled.recycle();
            is.close();

            return mat;

        } catch (Exception e) {
            return null;
        }
    }
}