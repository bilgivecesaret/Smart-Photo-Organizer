package com.example.smart_photo_organizer.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.smart_photo_organizer.util.AutoCleanup;

public class AutoCleanupWorker extends Worker {

    public AutoCleanupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        new Thread(() -> AutoCleanup.runAutoCleanupBackground(getApplicationContext())).start();
        return Result.success();
    }
}