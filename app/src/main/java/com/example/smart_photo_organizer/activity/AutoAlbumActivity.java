package com.example.smart_photo_organizer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.example.smart_photo_organizer.util.HumanDetectionUtil;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.AutoAlbumAdapter;
import com.example.smart_photo_organizer.model.AutoAlbum;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.AutoAlbumCreator;
import com.example.smart_photo_organizer.util.HumanDetectionUtil;
import com.example.smart_photo_organizer.util.ImageFetcher;
import com.example.smart_photo_organizer.adapter.GridImageAdapter;

import java.util.ArrayList;
import java.util.List;

import android.util.Log;                           // Log.d() için
import org.tensorflow.lite.Interpreter;            // TFLite Interpreter
import org.tensorflow.lite.support.common.FileUtil; // FileUtil.loadMappedFile
import java.nio.MappedByteBuffer;                  // MappedByteBuffer
import java.util.Arrays;                            // Arrays.toString()

public class AutoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button btnDateSort, btnHumanSort;

    private List<HashItem> allPhotos = new ArrayList<>();
    private AutoAlbumAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auto_album);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.autoAlbumActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Görünümleri bağla
        recyclerView = findViewById(R.id.rvAutoAlbums);
        btnDateSort = findViewById(R.id.btnSortByDate);
        btnHumanSort = findViewById(R.id.btnSortByHuman);

        recyclerView.setVisibility(View.GONE); // Başlangıçta görünmez
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2)); // Grid görünümü (2 sütun)

        // Buton tıklamaları
        btnDateSort.setOnClickListener(v -> loadAlbumsByDate());
        btnHumanSort.setOnClickListener(v -> loadAlbumsByHuman());

        // 🔹 Model input/output shape testi
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(this, "detect.tflite");
            Interpreter tflite = new Interpreter(model);
            int[] inputShape = tflite.getInputTensor(0).shape();
            int[] outputShape = tflite.getOutputTensor(0).shape();
            Log.d("TFLite", "Input shape: " + Arrays.toString(inputShape));
            Log.d("TFLite", "Output shape: " + Arrays.toString(outputShape));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void loadAlbumsByDate() {
        loadAllPhotos(() -> {
            List<AutoAlbum> albums = AutoAlbumCreator.createAutoAlbums(this, allPhotos);
            displayAlbums(albums);
        });
    }

    private void loadAlbumsByHuman() {

        loadAllPhotos(() -> {

            List<HashItem> humanPhotos =
                    HumanDetectionUtil.filterPhotosWithHumans(this, allPhotos);

            if (humanPhotos.isEmpty()) {
                Toast.makeText(this, "İnsan içeren fotoğraf bulunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }

            GridImageAdapter gridAdapter = new GridImageAdapter(this, humanPhotos);

            recyclerView.setAdapter(gridAdapter);

            btnDateSort.setVisibility(View.GONE);
            btnHumanSort.setVisibility(View.GONE);

            recyclerView.setVisibility(View.VISIBLE);
        });
    }

    private void loadAllPhotos(Runnable onComplete) {
        allPhotos.clear();

        ImageFetcher.loadAllImagesAsync(this, 20, new ImageFetcher.ImageBatchCallback() {
            @Override
            public void onBatch(List<HashItem> batch) {
                allPhotos.addAll(batch);
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> {
                    if (allPhotos.isEmpty()) {
                        Toast.makeText(AutoAlbumActivity.this, "Analiz edilecek fotoğraf bulunamadı.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    onComplete.run();
                });
            }
        });
    }

    private void displayAlbums(List<AutoAlbum> albums) {
        if (albums == null || albums.isEmpty()) {
            Toast.makeText(this, "Albüm oluşturmak için yeterli veri yok.", Toast.LENGTH_SHORT).show();
            return;
        }

        adapter = new AutoAlbumAdapter(this, albums);
        recyclerView.setAdapter(adapter);

        // Butonları gizle, sadece grid görünür
        btnDateSort.setVisibility(View.GONE);
        btnHumanSort.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}