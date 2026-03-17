package com.example.smart_photo_organizer.worker;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.smart_photo_organizer.util.AutoCleanupSimilar;
import com.example.smart_photo_organizer.util.SimilarPhotoCache;

import java.util.List;

public class AutoCleanupWorker extends Worker {
    public AutoCleanupWorker(@NonNull Context context,
                             @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        try {

            Log.d("AUTO_DEBUG", "Worker started");

            AutoCleanupSimilar cleanup =
                    new AutoCleanupSimilar(getApplicationContext());

            List<Uri> similarUris =
                    cleanup.findAllSimilarUris();

            Log.d("AUTO_DEBUG", "Found count: " + similarUris.size());

            if (similarUris.isEmpty()) {
                return Result.success();
            }

            StringBuilder builder = new StringBuilder();

            for (Uri uri : similarUris) {
                builder.append(uri.toString()).append(",");
            }

            SimilarPhotoCache.cachedUris = similarUris;

            Data output = new Data.Builder()
                    .putBoolean("HAS_RESULT", true)
                    .build();

            return Result.success(output);

        } catch (Exception e) {

            Log.e("AUTO_DEBUG", "WORKER CRASH!", e);

            return Result.failure();
        }
    }
}