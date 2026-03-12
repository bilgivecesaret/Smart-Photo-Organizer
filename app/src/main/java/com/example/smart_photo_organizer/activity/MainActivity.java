package com.example.smart_photo_organizer.activity;

import static com.example.smart_photo_organizer.permission.PermissionHelper.openAppSettings;

import android.Manifest;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.fragment.AlbumsFragment;
import com.example.smart_photo_organizer.fragment.CleanupFragment;
import com.example.smart_photo_organizer.fragment.NoImageFragment;
import com.example.smart_photo_organizer.fragment.PhotosFragment;
import com.example.smart_photo_organizer.fragment.SettingsFragment;
import com.example.smart_photo_organizer.permission.PermissionHelper;
import com.example.smart_photo_organizer.worker.AutoCleanupWorker;
import com.example.smart_photo_organizer.worker.BlurCleanupWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    private long backPressedTime = 0; // Son back tuşu zamanı
    private static final int BACK_PRESS_INTERVAL = 2000; // 2 saniye
    boolean autoCleanup;
    private ActivityResultLauncher<IntentSenderRequest> deleteLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV yükleme hatası!");
        } else {
            Log.d("OpenCV", "OpenCV başarıyla çalışıyor.");
        }

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        deleteLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartIntentSenderForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK) {
                                Toast.makeText(this,
                                        "Similar photos deleted",
                                        Toast.LENGTH_SHORT).show();
                            }
                            Log.d("AUTO_DEBUG", "Delete result code: " + result.getResultCode());
                        });

        // BACK TUŞU CALLBACK (Sadece belirli fragmentlerde uygulamayı kapatır)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Fragment currentFragment = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);

                boolean isMainFragment = currentFragment instanceof PhotosFragment
                        || currentFragment instanceof AlbumsFragment
                        || currentFragment instanceof CleanupFragment
                        || currentFragment instanceof SettingsFragment
                        || currentFragment instanceof NoImageFragment;

                if (isMainFragment) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - backPressedTime < BACK_PRESS_INTERVAL) {
                        // 2 saniye içinde tekrar back tuşuna basıldı → çık
                        finishAffinity();
                        System.exit(0);
                    } else {
                        backPressedTime = currentTime;
                        Toast.makeText(MainActivity.this,
                                "Uygulamadan çıkmak için tekrar basın",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Diğer fragment veya child fragmentlerde normal back
                    if (currentFragment != null &&
                            currentFragment.getChildFragmentManager().getBackStackEntryCount() > 0) {
                        currentFragment.getChildFragmentManager().popBackStack();
                    } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        getSupportFragmentManager().popBackStack();
                    } else {
                        finish();
                    }
                }
            }
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        PermissionHelper.checkStoragePermissions(this,this);
        setupNavigation();

        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        autoCleanup = prefs.getBoolean(SettingsFragment.KEY_AUTO_CLEANUP_SIMILAR,false);

        if (autoCleanup && hasStoragePermission()) {
            Log.d("AUTO_DEBUG", "Permission OK, starting cleanup");
            startAutoCleanup();
        }

        boolean autoCleanupBlurred = prefs.getBoolean(SettingsFragment.KEY_AUTO_CLEANUP_BLURRED, false);

        if (autoCleanupBlurred && hasStoragePermission()) {
            Log.d("BLUR_WORKER", "Starting Blurred Photo Cleanup Worker");
            startBlurCleanupWorker();
        }
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
    private void setupNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            int itemId = item.getItemId();
            if (itemId == R.id.photos) {
                selectedFragment = new PhotosFragment();
            } else if (itemId == R.id.albums) {
                selectedFragment = new AlbumsFragment();
            } else if (itemId == R.id.cleanup) {
                selectedFragment = new CleanupFragment();
            } else if (itemId == R.id.settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }

            return true;
        });
        bottomNavigationView.setSelectedItemId(R.id.photos);
    }

    private void startBlurCleanupWorker() {
        OneTimeWorkRequest blurRequest =            //PERİYODİKLEŞTİRİLİCEK
                new OneTimeWorkRequest.Builder(BlurCleanupWorker.class)
                        .build();

        WorkManager.getInstance(this).enqueue(blurRequest);
    }
    private void startAutoCleanup() {

        OneTimeWorkRequest request =
                new OneTimeWorkRequest.Builder(AutoCleanupWorker.class)
                        .build();

        WorkManager.getInstance(this).enqueue(request);

        WorkManager.getInstance(this)
                .getWorkInfoByIdLiveData(request.getId())
                .observe(this, workInfo -> {
                    Log.d("AUTO_DEBUG", "Observer triggered");
                    if (workInfo != null
                            && workInfo.getState().isFinished()) {

                        String uriString =
                                workInfo.getOutputData()
                                        .getString(AutoCleanupWorker.KEY_URIS);

                        if (uriString == null || uriString.isEmpty())
                            return;

                        List<Uri> uriList = new ArrayList<>();

                        for (String s : uriString.split(",")) {
                            if (!s.isEmpty())
                                uriList.add(Uri.parse(s));
                        }

                        Log.d("AUTO_DEBUG", "Launching delete request with size: " + uriList.size());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                PendingIntent pi =
                                        MediaStore.createDeleteRequest(
                                                getContentResolver(),
                                                uriList);

                                IntentSenderRequest request1 =
                                        new IntentSenderRequest.Builder(
                                                pi.getIntentSender())
                                                .build();

                                deleteLauncher.launch(request1);

                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {

                            for (Uri uri : uriList) {
                                getContentResolver()
                                        .delete(uri, null, null);
                            }

                            Toast.makeText(this,
                                    "Similar photos deleted",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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
