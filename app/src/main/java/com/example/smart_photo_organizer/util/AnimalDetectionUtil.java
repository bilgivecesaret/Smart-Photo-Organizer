package com.example.smart_photo_organizer.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import com.example.smart_photo_organizer.model.HashItem;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class AnimalDetectionUtil {

    public static List<HashItem> filterPhotosWithAnimals(Context context, List<HashItem> photos) {
        List<HashItem> animalPhotos = new ArrayList<>();

        try {
            // 1️⃣ TFLite modelini yükle
            MappedByteBuffer modelBuffer = FileUtil.loadMappedFile(context, "detect.tflite");
            Interpreter tflite = new Interpreter(modelBuffer);

            for (HashItem item : photos) {
                // 2️⃣ Fotoğrafı bitmap olarak al
                Bitmap bitmap = getBitmapFromUri(context, item.uri);

                if (bitmap != null) {
                    // 🔹 Burada modelin input/output işlemi olacak
                    // Şimdilik test: tüm fotoğrafları ekle
                    // Daha sonra modelin çıktısına göre kontrol edeceğiz
                    animalPhotos.add(item);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return animalPhotos;
    }

    private static Bitmap getBitmapFromUri(Context context, Uri uri) {
        try {
            InputStream input = context.getContentResolver().openInputStream(uri);
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}