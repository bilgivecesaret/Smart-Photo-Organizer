package com.example.smart_photo_organizer.activity;

import static com.example.smart_photo_organizer.util.ImageFetcher.loadAllImages;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ProgressBar;

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
import com.example.smart_photo_organizer.util.ImageFetcher;
import com.example.smart_photo_organizer.util.ImagePHash;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

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
        ProgressBar progressBar = findViewById(R.id.progressBar);

        // 1. İşlem başlamadan önce UI hazırlığı
        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
        }

        // 2. Tek bir arka plan thread'i başlatıyoruz
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Veriyi temizle ve yükle
                List<HashItem> fetchedImages = ImageFetcher.loadAllImages(this);
                List<List<HashItem>> localGroups = new ArrayList<>();

                // Benzer fotoğrafları grupla (Ağır İşlem)
                for (HashItem item : fetchedImages) {
                    if (item.hash == null) {
                        item.hash = ImagePHash.calculateHash(this, item.uri);
                    }

                    boolean added = false;
                    for (List<HashItem> group : localGroups) {
                        String representativeHash = group.get(0).hash;
                        int distance = ImagePHash.hammingDistance(item.hash, representativeHash);

                        if (distance <= HAMMING_THRESHOLD) {
                            group.add(item);
                            added = true;
                            break;
                        }
                    }

                    if (!added) {
                        List<HashItem> newGroup = new ArrayList<>();
                        newGroup.add(item);
                        localGroups.add(newGroup);
                    }
                }

                // UI için DuplicateGroup listesini hazırla
                List<DuplicateGroup> resultList = new ArrayList<>();
                for (List<HashItem> group : localGroups) {
                    if (group.size() > 1) {
                        List<Uri> uris = new ArrayList<>();
                        for (HashItem hi : group) {
                            uris.add(hi.uri);
                        }
                        resultList.add(new DuplicateGroup(group.get(0).hash, uris));
                    }
                }

                // 3. UI GÜNCELLEMESİ (Mutlaka runOnUiThread içinde olmalı)
                runOnUiThread(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    duplicateGroups.clear();
                    duplicateGroups.addAll(resultList);

                    if (adapter == null) {
                        adapter = new DuplicateAlbumAdapter(this, duplicateGroups);
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                });
            }
        });
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
        // Sayfaya her geri dönüldüğünde (örneğin silme ekranından)
        // listeyi arka planda tekrar tara.
        findSimilarPhotos();

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
