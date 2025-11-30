package com.example.smart_photo_organizer.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.model.FolderItem;
import com.example.smart_photo_organizer.adapter.FolderItemAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FolderListActivity extends AppCompatActivity {
    private static final int STORAGE_PERMISSION_CODE = 200;
    RecyclerView recyclerView;
    FolderItemAdapter adapter;
    List<FolderItem> folderList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_folder_list);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_folder_list), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerFolders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        checkPermissions();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                loadImageFolders();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, STORAGE_PERMISSION_CODE);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                loadImageFolders();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, STORAGE_PERMISSION_CODE);
            }
        }
    }

    private void loadImageFolders() {
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {
                MediaStore.Images.Media.DATA
        };

        Cursor cursor = getContentResolver().query(
                uri, projection, null, null, null
        );

        if (cursor != null) {
            HashMap<String, ArrayList<String>> folderMap = new HashMap<>();

            while (cursor.moveToNext()) {
                String imagePath = cursor.getString(0);
                File file = new File(imagePath);
                String folderName = file.getParentFile().getName();
                String folderPath = file.getParent();

                if (!folderMap.containsKey(folderPath)) {
                    folderMap.put(folderPath, new ArrayList<>());
                }
                folderMap.get(folderPath).add(imagePath);
            }
            cursor.close();

            for (String folderPath : folderMap.keySet()) {
                ArrayList<String> images = folderMap.get(folderPath);
                folderList.add(new FolderItem(folderPath, images.get(0), images));
            }

            adapter = new FolderItemAdapter(this, folderList);
            recyclerView.setAdapter(adapter);
        }

        if (folderList.isEmpty()) {
            startActivity(new Intent(this, NoImageActivity.class));
            finish();
            return;
        }
    }

    // Triggered when the user returns denying permission
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadImageFolders();
            } else {

                // Did the user say "Don't ask again"?
                boolean neverAskAgain = !ActivityCompat.shouldShowRequestPermissionRationale(
                        this, permissions[0]);

                if (neverAskAgain) {
                    showSettingsDialog(); // You will be directed to Settings
                } else {
                    // User normally rejected → we can request again
                    Toast toast= Toast.makeText(this,"Permission Denied.", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER_VERTICAL ,0,0);
                    toast.show();
                }
            }
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Required")
                .setMessage("This app needs image reading permission to work. " +
                        "Please allow it from settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
            // If the user presses Cancel, the application will close completely.
                    finishAffinity();   // Closes the entire activity stack
                    System.exit(0);     // Completely terminates the app (including background)
                })
                .setCancelable(false)
                .show();

    }
}