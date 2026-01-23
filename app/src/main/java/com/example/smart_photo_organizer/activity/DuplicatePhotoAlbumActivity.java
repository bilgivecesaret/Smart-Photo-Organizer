package com.example.smart_photo_organizer.activity;

import static com.example.smart_photo_organizer.util.LoadingImage.loadAllImages;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.DuplicateAlbumAdapter;
import com.example.smart_photo_organizer.model.DuplicateGroup;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.ImagePHash;

import java.util.ArrayList;
import java.util.List;

public class DuplicatePhotoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    DuplicateAlbumAdapter adapter;
    List<HashItem> allImages;
    List<List<HashItem>> groups;
    private final List<DuplicateGroup> duplicateGroups = new ArrayList<>();
    private static final int HAMMING_THRESHOLD = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_duplicate_albums);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.duplicatePhotoAlbum), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerDuplicateAlbums);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        findSimilarPhotos();
    }

    private void findSimilarPhotos() {

        duplicateGroups.clear();

        allImages =  loadAllImages(this);

        // --- BENZER FOTOĞRAFLARI GRUPLA ---
        groups = new ArrayList<>();

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

        if (adapter == null) {
            adapter = new DuplicateAlbumAdapter(this, duplicateGroups);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private final ContentObserver duplicateObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
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
