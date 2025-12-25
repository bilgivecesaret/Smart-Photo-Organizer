package com.example.smart_photo_organizer.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.DuplicateAlbumAdapter;
import com.example.smart_photo_organizer.model.DuplicateGroup;
import com.example.smart_photo_organizer.util.ImagePHash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class DuplicatePhotoAlbumActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    List<DuplicateGroup> duplicateGroups = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duplicate_albums);

        if (!hasImagePermission()) {
            requestPermissions(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                            ? new String[]{Manifest.permission.READ_MEDIA_IMAGES}
                            : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    100
            );
            return;
        }


        recyclerView = findViewById(R.id.recyclerDuplicateAlbums);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Executors.newSingleThreadExecutor().execute(this::findDuplicatePhotos);
    }

    private boolean hasImagePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }


    private void findDuplicatePhotos() {
        Map<String, List<Uri>> hashMap = new HashMap<>();

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContentResolver().query(
                uri,
                new String[]{MediaStore.Images.Media._ID},
                null, null, null
        );

        if (cursor == null) return;

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            Uri imageUri = Uri.withAppendedPath(uri, String.valueOf(id));

            String hash = ImagePHash.calculateHash(this, imageUri);
            if (hash.isEmpty()) continue;

            hashMap.computeIfAbsent(hash, k -> new ArrayList<>()).add(imageUri);
        }
        cursor.close();

        for (String hash : hashMap.keySet()) {
            if (hashMap.get(hash).size() > 1) {
                duplicateGroups.add(new DuplicateGroup(hash, hashMap.get(hash)));
            }
        }

        runOnUiThread(() -> {
            DuplicateAlbumAdapter adapter =
                    new DuplicateAlbumAdapter(this, duplicateGroups);
            recyclerView.setAdapter(adapter);
        });
    }
}