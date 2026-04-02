package com.example.smart_photo_organizer.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import com.example.smart_photo_organizer.activity.BlurredPhotoActivity;
import com.example.smart_photo_organizer.fragment.SettingsFragment;
import com.example.smart_photo_organizer.util.BlurDetector;

import java.io.File;

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
        SharedPreferences prefs = context.getSharedPreferences(SettingsFragment.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isAutoCleanupActive = prefs.getBoolean(SettingsFragment.KEY_AUTO_CLEANUP_BLURRED, false);

        if (!isAutoCleanupActive) {
            Log.d(TAG, "Blurred Photo Auto Cleanup setting is off.");
            return Result.success();
        }

        Log.d(TAG, "Blurred Photo Scanning is starting");

        int blurCount = 0;
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};

        try (Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null) {
                Log.d("BLUR_WORKER", "Galeride bulunan toplam dosya: " + cursor.getCount());
                while (cursor.moveToNext()) {
                    String path = cursor.getString(0);

                    if (path != null && new File(path).exists()) {
                        try {
                            boolean isActuallyBlurry = BlurDetector.isBlurry(path);

                            if (isActuallyBlurry) {
                                blurCount++;
                            }
                        } catch (Exception e) {
                            Log.e("BLUR_WORKER", "Analiz sırasında HATA oluştu: " + path, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Scanning Error", e);
            return Result.failure();
        }

        if (blurCount > 0) {
            Log.d(TAG, blurCount + " Blurred Photos Found. Sending Notification.");
            sendNotification(context, blurCount);
        } else {
            Log.d(TAG, "No Blurred Photo Found");
        }

        return Result.success();
    }

    private void sendNotification(Context context, int count) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Smart Photo Organizer",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Bulanık fotoğraf uyarıları");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, BlurredPhotoActivity.class);
        intent.putExtra("from_notification", true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Bulanık Fotoğraflar Tespit Edildi")
                .setContentText("Cihazınızda yer açmak için " + count + " adet bulanık fotoğrafı temizleyin.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(2001, builder.build());
    }
}