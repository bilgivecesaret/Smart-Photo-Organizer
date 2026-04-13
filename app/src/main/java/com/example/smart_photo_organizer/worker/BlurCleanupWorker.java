package com.example.smart_photo_organizer.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.example.smart_photo_organizer.fragment.SettingsFragment;
import com.example.smart_photo_organizer.util.BlurDetector;
import com.example.smart_photo_organizer.util.SimilarPhotoCache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BlurCleanupWorker extends Worker {

    private static final String TAG = "BlurCleanupWorker";
    private static final String CHANNEL_ID = "blur_cleanup_channel";

    public BlurCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        SharedPreferences prefs = context.getSharedPreferences(
                SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isActive = prefs.getBoolean(SettingsFragment.KEY_AUTO_CLEANUP_BLURRED, false);

        if (!isActive) {
            Log.d(TAG, "Blurred auto-cleanup is off.");
            return Result.success();
        }

        Log.d(TAG, "Blurry scan started.");

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

                    if (path != null && new File(path).exists()) {
                        try {
                            if (BlurDetector.isBlurry(path)) {
                                Uri uri = ContentUris.withAppendedId(
                                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                                blurryUris.add(uri);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Analysis error: " + path, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Scan error", e);
            return Result.failure();
        }

        if (!blurryUris.isEmpty()) {
            Log.d(TAG, blurryUris.size() + " blurry photos found. Sending notification.");
            SimilarPhotoCache.cachedUris = blurryUris;
            sendNotification(context, blurryUris.size());
        } else {
            Log.d(TAG, "No blurry photos found.");
        }

        return Result.success();
    }

    private void sendNotification(Context context, int count) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Blur Cleanup", NotificationManager.IMPORTANCE_DEFAULT);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, SimilarPhotoGridActivity.class);
        intent.putExtra("from_auto_cleanup", true);
        intent.putExtra("use_cache", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT |
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(SimilarPhotoGridActivity.class);
        stackBuilder.addNextIntent(intent);
        PendingIntent pi = stackBuilder.getPendingIntent(2001, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Blurry Photos Detected")
                .setContentText(count + " blurry photos found. Tap to clean up.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(2001, builder.build());
    }
}