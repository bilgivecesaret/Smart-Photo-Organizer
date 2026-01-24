package com.example.smart_photo_organizer.activity;

import static com.example.smart_photo_organizer.permission.PermissionHelper.openAppSettings;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.fragment.AlbumsFragment;
import com.example.smart_photo_organizer.fragment.CleanupFragment;
import com.example.smart_photo_organizer.fragment.PhotosFragment;
import com.example.smart_photo_organizer.fragment.SettingsFragment;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.permission.PermissionHelper;
import com.example.smart_photo_organizer.util.ImageFetcher;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    BottomNavigationView bottomNavigationView;
    List<Uri> images;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        PermissionHelper.checkStoragePermissions(this,this);
        setupNavigation();
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Denied. Please allow in Settings.", Toast.LENGTH_SHORT).show();
                openAppSettings(this);
            }
        }
    }
}
