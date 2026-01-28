package com.example.smart_photo_organizer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
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

public class AutoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar; // İşlem sırasında kullanıcıya bilgi vermek için
    private List<HashItem> allPhotos = new ArrayList<>();
    private AutoAlbumAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_album);

        // Görünümleri bağla
        recyclerView = findViewById(R.id.rvAutoAlbums);
        // Eğer layout'ta progressBar varsa burayı kullanabilirsin, yoksa silebilirsin
        progressBar = findViewById(R.id.progressBarBlur);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadData();
    }

    private void loadData() {
        allPhotos.clear();
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        // ImageFetcher artık tarih ve konum verilerini de getiriyor
        ImageFetcher.loadAllImagesAsync(this, 50, new ImageFetcher.ImageBatchCallback() {
            @Override
            public void onBatch(List<HashItem> batch) {
                // Gelen her 50'lik fotoğraf grubunu listeye ekle
                allPhotos.addAll(batch);
            }

            @Override
            public void onComplete() {
                // Tüm fotoğraflar yüklendi, şimdi kümeleme (clustering) zamanı
                if (allPhotos.isEmpty()) {
                    runOnUiThread(() -> {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(AutoAlbumActivity.this, "Analiz edilecek fotoğraf bulunamadı.", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Verileri tarih ve konuma göre gruplandırıp albümleri oluştur
                List<AutoAlbum> createdAlbums = AutoAlbumCreator.createAutoAlbums(AutoAlbumActivity.this, allPhotos);

                // UI güncellemeleri her zaman ana thread'de yapılmalı
                runOnUiThread(() -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    setupRecyclerView(createdAlbums);
                });
            }
        });
    }

    private void setupRecyclerView(List<AutoAlbum> albums) {
        if (albums == null || albums.isEmpty()) {
            Toast.makeText(this, "Akıllı albümleme için yeterli veri sağlanamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Adaptörü yeni albüm listesiyle bağla
        adapter = new AutoAlbumAdapter(this, albums);
        recyclerView.setAdapter(adapter);
    }
}