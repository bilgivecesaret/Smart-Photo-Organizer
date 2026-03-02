package com.example.smart_photo_organizer.worker;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.smart_photo_organizer.util.AutoCleanupSimilar;

import java.util.List;

public class AutoCleanupWorker extends Worker {

    public static final String KEY_URIS = "delete_uris";

    public AutoCleanupWorker(@NonNull Context context,
                             @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d("AUTO_DEBUG", "Worker started");
        AutoCleanupSimilar cleanup =
                new AutoCleanupSimilar(getApplicationContext());

        List<Uri> deleteUris =
                cleanup.findSimilarAndReturnUris();

        Log.d("AUTO_DEBUG", "Found delete count: " + deleteUris.size());

        if (deleteUris.isEmpty())
            return Result.success();

        StringBuilder builder = new StringBuilder();

        for (Uri uri : deleteUris) {
            builder.append(uri.toString()).append(",");
        }

        Data output = new Data.Builder()
                .putString(KEY_URIS, builder.toString())
                .putInt("count", deleteUris.size())
                .build();

        return Result.success(output);
    }
}