package com.example.smart_photo_organizer.activity;

import static com.example.smart_photo_organizer.permission.PermissionHelper.openAppSettings;
import static com.example.smart_photo_organizer.util.LocaleHelper.setLocale;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.fragment.SettingsFragment;
import com.example.smart_photo_organizer.permission.PermissionHelper;
import com.example.smart_photo_organizer.util.FullMediaScan;
import com.example.smart_photo_organizer.util.SimilarPhotoCache;
import com.example.smart_photo_organizer.worker.AutoCleanupWorker;
import com.example.smart_photo_organizer.worker.BlurCleanupWorker;
import com.example.smart_photo_organizer.worker.SimilarCleanupWorker;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SplashScreenActivity extends AppCompatActivity {
    SharedPreferences prefs;
    String lang;
    boolean autoCleanupSimilar, autoCleanupBlurred;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        lang = prefs.getString("app_language", "en");
        setLocale(lang);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_splash), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        PermissionHelper.checkStoragePermissions(this,this);

        autoCleanupSimilar = prefs.getBoolean(SettingsFragment.KEY_AUTO_CLEANUP_SIMILAR,false);
        autoCleanupBlurred = prefs.getBoolean(SettingsFragment.KEY_AUTO_CLEANUP_BLURRED, false);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(() -> {
            // MediaStore refresh
            FullMediaScan.rescanAllPublicMedia(this);
        });

        executor.execute(() -> {
            if (hasStoragePermission()) {
                if (autoCleanupSimilar && autoCleanupBlurred) {
                    startAutoCleanup();
                } else if (autoCleanupSimilar) {
                    startSimilarPhotoCleanup();
                } else if (autoCleanupBlurred) {
                    startBlurCleanupWorker();
                }
            }
        });

        executor.execute(() -> {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            }, 1000);
        });

    }
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void startAutoCleanup(){
        OneTimeWorkRequest autoCleanupRequest = new OneTimeWorkRequest.Builder(AutoCleanupWorker.class)
                .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "auto_cleanup",
                ExistingWorkPolicy.KEEP,
                autoCleanupRequest
        );
    }
    private void startBlurCleanupWorker() {
        OneTimeWorkRequest blurRequest =
                new OneTimeWorkRequest.Builder(BlurCleanupWorker.class)
                        .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "blur_cleanup",
                ExistingWorkPolicy.KEEP,
                blurRequest
        );
    }
    private void startSimilarPhotoCleanup() {

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(SimilarCleanupWorker.class)
                        .build();

        WorkManager.getInstance(this).enqueueUniqueWork(
                "similar_cleanup",
                ExistingWorkPolicy.KEEP,
                request
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Denied. Please allow in Settings.",
                        Toast.LENGTH_SHORT).show();
                openAppSettings(this);
            }
        }
    }

}


