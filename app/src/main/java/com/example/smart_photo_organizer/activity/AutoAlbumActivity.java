package com.example.smart_photo_organizer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.AutoAlbumAdapter;
import com.example.smart_photo_organizer.model.AutoAlbum;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.AutoAlbumCreator;
import com.example.smart_photo_organizer.util.ImageFetcher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AutoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View layoutButtons;

    private Button btnSortByDate;
    private Button btnSortByFolder;
    private Button btnSortNewest;
    private Button btnSortOldest;
    private Button btnFilterHuman;
    private TextView tvLoading;

    private List<HashItem> allPhotos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auto_album);

        recyclerView = findViewById(R.id.rvAutoAlbums);
        layoutButtons = findViewById(R.id.layoutButtons);

        btnSortByDate   = findViewById(R.id.btnSortByDate);
        btnSortByFolder = findViewById(R.id.btnSortByFolder);
        btnSortNewest   = findViewById(R.id.btnSortNewest);
        btnSortOldest   = findViewById(R.id.btnSortOldest);
        btnFilterHuman = findViewById(R.id.btnFilterHuman);
        tvLoading = findViewById(R.id.tvLoading);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setVisibility(View.GONE);

        btnSortByDate.setOnClickListener(v -> loadAndShow(SortType.GROUP_BY_DATE));
        btnSortByFolder.setOnClickListener(v -> loadAndShow(SortType.GROUP_BY_FOLDER));
        btnSortNewest.setOnClickListener(v -> loadAndShow(SortType.NEWEST_FIRST));
        btnSortOldest.setOnClickListener(v -> loadAndShow(SortType.OLDEST_FIRST));
        btnFilterHuman.setOnClickListener(v -> loadAndShow(SortType.HUMAN_FILTER));
    }

    // ─── Sort tipleri ────────────────────────────────────────────────
    private enum SortType {
        GROUP_BY_DATE,
        GROUP_BY_FOLDER,
        NEWEST_FIRST,
        OLDEST_FIRST,
        HUMAN_FILTER
    }

    // ─── Ana yükleme metodu ───────────────────────────────────────────
    private void loadAndShow(SortType type) {
        allPhotos = new ArrayList<>();

        ImageFetcher.loadAllImagesAsync(this, 20, new ImageFetcher.ImageBatchCallback() {
            @Override
            public void onBatch(List<HashItem> batch) {
                allPhotos.addAll(batch);
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> processAndShow(type));
            }
        });
    }

    // ─── Sıralama / gruplama ──────────────────────────────────────────
    private void processAndShow(SortType type) {
        List<AutoAlbum> albums = new ArrayList<>();

        switch (type) {
            case HUMAN_FILTER:
                new com.example.smart_photo_organizer.util.HumanFilter()
                        .filter(this, allPhotos, result -> {
                            List<AutoAlbum> humanAlbums = new ArrayList<>();
                            humanAlbums.add(new AutoAlbum("İnsan İçerikli Fotoğraflar", result));
                            runOnUiThread(() -> {
                                recyclerView.setAdapter(new AutoAlbumAdapter(this, humanAlbums));
                                layoutButtons.setVisibility(View.GONE);
                                recyclerView.setVisibility(View.VISIBLE);
                            });
                        });
                return;

            case GROUP_BY_DATE:
                albums = AutoAlbumCreator.createAutoAlbums(this, allPhotos);
                break;

            case GROUP_BY_FOLDER:
                albums = groupByFolder(allPhotos);
                break;

            case NEWEST_FIRST:
                List<HashItem> newest = new ArrayList<>(allPhotos);
                Collections.sort(newest, (a, b) -> Long.compare(b.timestamp, a.timestamp));
                List<HashItem> newestLimited = newest.subList(0, Math.min(30, newest.size()));
                albums.add(new AutoAlbum("En Yeni Fotoğraflar", newestLimited));
                break;

            case OLDEST_FIRST:
                List<HashItem> oldest = new ArrayList<>(allPhotos);
                Collections.sort(oldest, (a, b) -> Long.compare(a.timestamp, b.timestamp));
                List<HashItem> oldestLimited = oldest.subList(0, Math.min(30, oldest.size()));
                albums.add(new AutoAlbum("En Eski Fotoğraflar", oldestLimited));
                break;
        }

        recyclerView.setAdapter(new AutoAlbumAdapter(this, albums));
        layoutButtons.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    // ─── Klasöre göre gruplama ────────────────────────────────────────
    private List<AutoAlbum> groupByFolder(List<HashItem> photos) {
        Map<String, List<HashItem>> map = new LinkedHashMap<>();

        for (HashItem item : photos) {
            String folder = item.bucketName != null ? item.bucketName : "Diğer";
            if (!map.containsKey(folder)) {
                map.put(folder, new ArrayList<>());
            }
            map.get(folder).add(item);
        }

        List<AutoAlbum> result = new ArrayList<>();
        for (Map.Entry<String, List<HashItem>> entry : map.entrySet()) {
            result.add(new AutoAlbum(entry.getKey(), entry.getValue()));
        }
        return result;
    }
}