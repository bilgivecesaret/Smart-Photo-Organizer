package com.example.smart_photo_organizer.worker;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.activity.SimilarPhotoGridActivity;
import com.example.smart_photo_organizer.util.AutoCleanupSimilar;
import com.example.smart_photo_organizer.util.SimilarPhotoCache;

import java.util.ArrayList;
import java.util.List;

public class SimilarCleanupWorker extends Worker {

    private static final String TAG = "SimilarCleanupWorker";
    private static final String CHANNEL_ID = "similar_cleanup_channel";

    public SimilarCleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Worker started");

            AutoCleanupSimilar cleanup = new AutoCleanupSimilar(getApplicationContext());
            List<Uri> similarUris = cleanup.findAllSimilarUris();

            Log.d(TAG, "Found: " + similarUris.size());

            if (similarUris.isEmpty()) return Result.success();

            // Cache'e kaydet
            SimilarPhotoCache.cachedUris = similarUris;

            // Bildirim gönder
            sendNotification(getApplicationContext(), similarUris.size());

            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "Worker crashed!", e);
            return Result.failure();
        }
    }

    private void sendNotification(Context context, int count) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Similar Photos", NotificationManager.IMPORTANCE_DEFAULT);
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
                .setContentTitle("Similar Photos Found")
                .setContentText(count + " similar photos detected. Tap to review.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

        nm.notify(2002, builder.build());
    }
}