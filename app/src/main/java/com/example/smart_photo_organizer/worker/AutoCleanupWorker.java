package com.example.smart_photo_organizer.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.activity.SimilarPhotoGridActivity;
import com.example.smart_photo_organizer.util.AutoCleanupSimilar;
import com.example.smart_photo_organizer.util.BlurDetector;
import com.example.smart_photo_organizer.util.SimilarPhotoCache;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoCleanupWorker extends Worker {

    private static final String TAG = "AutoCleanupWorker";
    private static final String CHANNEL_ID = "auto_cleanup_channel";

    public AutoCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();

        try {
            // 1) Benzer fotoğrafları bul
            AutoCleanupSimilar similarCleanup = new AutoCleanupSimilar(context);
            List<Uri> similarUris = similarCleanup.findAllSimilarUris();
            Log.d(TAG, "Similar photos found: " + similarUris.size());

            // 2) Bulanık fotoğrafları bul
            List<Uri> blurryUris = findBlurryUris(context);
            Log.d(TAG, "Blurry photos found: " + blurryUris.size());

            // 3) Birleştir (tekrar edenleri çıkar)
            Set<Uri> combined = new HashSet<>();
            combined.addAll(similarUris);
            combined.addAll(blurryUris);

            if (combined.isEmpty()) {
                Log.d(TAG, "No photos found.");
                return Result.success();
            }

            // 4) Cache'e yaz
            SimilarPhotoCache.cachedUris = new ArrayList<>(combined);

            // 5) Bildirim gönder
            sendNotification(context, similarUris.size(), blurryUris.size());

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "AutoCleanupWorker crashed!", e);
            return Result.failure();
        }
    }

    private List<Uri> findBlurryUris(Context context) {
        List<Uri> blurryUris = new ArrayList<>();
        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA
        };
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null)) {

            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String path = cursor.getString(dataCol);
                    if (path != null && new File(path).exists() && BlurDetector.isBlurry(path)) {
                        Uri uri = ContentUris.withAppendedId(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        blurryUris.add(uri);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Blurry scan error", e);
        }
        return blurryUris;
    }

    private void sendNotification(Context context, int similarCount, int blurCount) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Auto Cleanup", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }

        // Tıklanınca SimilarPhotoGridActivity açılacak; URI'ler cache üzerinden gelecek
        Intent intent = new Intent(context, SimilarPhotoGridActivity.class);
        intent.putExtra("from_auto_cleanup", true);
        intent.putExtra("use_cache", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(SimilarPhotoGridActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pi = stackBuilder.getPendingIntent(2001, flags);

        String text = similarCount + " similar, " + blurCount +
                " blurry photos found. Tap to review.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Photos Need Cleanup")
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(3001, builder.build());
    }
}