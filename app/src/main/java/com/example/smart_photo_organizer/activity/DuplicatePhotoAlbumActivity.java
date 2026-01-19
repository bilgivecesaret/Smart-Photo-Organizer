package com.example.smart_photo_organizer.activity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.DuplicateAlbumAdapter;
import com.example.smart_photo_organizer.model.DuplicateGroup;
import com.example.smart_photo_organizer.util.ImagePHash;

import java.util.ArrayList;
import java.util.List;

public class DuplicatePhotoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private final List<DuplicateGroup> duplicateGroups = new ArrayList<>();
    private static final int HAMMING_THRESHOLD = 10;

    private static class HashItem {
        String hash;
        Uri uri;

        HashItem(String hash, Uri uri) {
            this.hash = hash;
            this.uri = uri;
        }
    }

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
        findSimilarPhotos();
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

    private void findSimilarPhotos() {

        duplicateGroups.clear();
        List<HashItem> allImages = new ArrayList<>();

        Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Cursor cursor = getContentResolver().query(
                baseUri,
                new String[]{MediaStore.Images.Media._ID},
                null,
                null,
                null
        );

        if (cursor == null) return;

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            Uri imageUri = Uri.withAppendedPath(baseUri, String.valueOf(id));

            String hash = ImagePHash.calculateHash(this, imageUri);
            if (!hash.isEmpty()) {
                allImages.add(new HashItem(hash, imageUri));
            }
        }
        cursor.close();

        // --- BENZER FOTOĞRAFLARI GRUPLA ---
        List<List<HashItem>> groups = new ArrayList<>();

        for (HashItem item : allImages) {
            boolean added = false;

            for (List<HashItem> group : groups) {
                String representativeHash = group.get(0).hash;
                int distance = ImagePHash.hammingDistance(
                        item.hash,
                        representativeHash
                );

                if (distance <= HAMMING_THRESHOLD) {
                    group.add(item);
                    added = true;
                    break;
                }
            }

            if (!added) {
                List<HashItem> newGroup = new ArrayList<>();
                newGroup.add(item);
                groups.add(newGroup);
            }
        }

        // --- DuplicateGroup MODELİNE DÖNÜŞTÜR ---
        for (List<HashItem> group : groups) {
            if (group.size() > 1) {
                List<Uri> uris = new ArrayList<>();
                for (HashItem item : group) {
                    uris.add(item.uri);
                }
                duplicateGroups.add(
                        new DuplicateGroup(group.get(0).hash, uris)
                );
            }
        }

        DuplicateAlbumAdapter adapter =
                new DuplicateAlbumAdapter(this, duplicateGroups);
        recyclerView.setAdapter(adapter);
    }

    private final ContentObserver duplicateObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            duplicateGroups.clear();
            findSimilarPhotos();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                duplicateObserver
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(duplicateObserver);
    }
}
