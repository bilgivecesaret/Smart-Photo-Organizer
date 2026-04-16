package com.example.smart_photo_organizer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.AutoAlbumAdapter;
import com.example.smart_photo_organizer.model.AutoAlbum;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.AutoAlbumCreator;
import com.example.smart_photo_organizer.util.ImageFetcher;

import java.util.ArrayList;
import java.util.List;

public class AutoAlbumDisplayActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private AutoAlbumAdapter adapter;
    private List<HashItem> allPhotos = new ArrayList<>();
    private List<AutoAlbum> autoAlbums = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_album_display); // Yeni layout dosyası
        recyclerView = findViewById(R.id.rvAutoAlbumsDisplay);
        progressBar = findViewById(R.id.progressBarDisplay);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AutoAlbumAdapter(this, autoAlbums);
        recyclerView.setAdapter(adapter);

        String sortType = getIntent().getStringExtra("SORT_TYPE");

        loadData(sortType);
    }

    private void loadData(String sortType) {
        progressBar.setVisibility(View.VISIBLE);
        allPhotos.clear();

        ImageFetcher.loadAllImagesAsync(this, 20, new ImageFetcher.ImageBatchCallback() {
            @Override
            public void onBatch(List<HashItem> batch) {
                allPhotos.addAll(batch);
            }

            @Override
            public void onComplete() {
                List<HashItem> filteredPhotos = new ArrayList<>(allPhotos);

                autoAlbums.clear();
                autoAlbums.addAll(AutoAlbumCreator.createAutoAlbums(AutoAlbumDisplayActivity.this, filteredPhotos));

                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}