package com.example.smart_photo_organizer.activity;

import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.AutoAlbumAdapter;
import com.example.smart_photo_organizer.adapter.GridImageAdapter;
import com.example.smart_photo_organizer.model.AutoAlbum;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.AutoAlbumCreator;
import com.example.smart_photo_organizer.util.HumanDetectionUtil;
import com.example.smart_photo_organizer.util.ImageFetcher;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.util.Log;

public class AutoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Button btnDateSort, btnHumanSort, btnAnimalSort;
    private List<HashItem> allPhotos = new ArrayList<>();
    private AutoAlbumAdapter adapter;

    // Activity seviyesinde TensorFlow Lite interpreter
    private Interpreter tflite;

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

        recyclerView = findViewById(R.id.rvAutoAlbums);
        btnDateSort = findViewById(R.id.btnSortByDate);
        btnHumanSort = findViewById(R.id.btnSortByHuman);
        btnAnimalSort = findViewById(R.id.btnSortByAnimal);

        recyclerView.setVisibility(RecyclerView.GONE);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        // Modeli yükle (sadece bir kez)
        loadTFLiteModel();

        // Buton tıklamaları
        btnDateSort.setOnClickListener(v -> loadAlbumsByDate());
        btnHumanSort.setOnClickListener(v -> loadAlbumsByHuman());
        btnAnimalSort.setOnClickListener(v -> loadAlbumsByAnimal());
    }

    private void loadTFLiteModel() {
        try {
            MappedByteBuffer model = FileUtil.loadMappedFile(this, "ml/detect.tflite");
            tflite = new Interpreter(model);
            Log.d("TFLite", "Model başarıyla yüklendi.");
        } catch (Exception e) {
            Log.e("AnimalDetection", "Model yüklenirken hata oluştu", e);
            Toast.makeText(this, "Hayvan modeli yüklenemedi.", Toast.LENGTH_SHORT).show();
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
            List<HashItem> humanPhotos = HumanDetectionUtil.filterPhotosWithHumans(this, allPhotos);

            if (humanPhotos.isEmpty()) {
                Toast.makeText(this, "İnsan içeren fotoğraf bulunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }

            GridImageAdapter gridAdapter = new GridImageAdapter(this, humanPhotos);
            recyclerView.setAdapter(gridAdapter);

            btnDateSort.setVisibility(Button.GONE);
            btnHumanSort.setVisibility(Button.GONE);
            btnAnimalSort.setVisibility(Button.GONE);

            recyclerView.setVisibility(RecyclerView.VISIBLE);
        });
    }

    private void loadAlbumsByAnimal() {
        loadAllPhotos(() -> {
            if (allPhotos.isEmpty()) {
                Toast.makeText(this, "Analiz edilecek fotoğraf bulunamadı.", Toast.LENGTH_SHORT).show();
                return;
            }

            new Thread(() -> {
                List<HashItem> animalPhotos = new ArrayList<>();
                AutoAlbumActivity activity = AutoAlbumActivity.this;

                try {
                    int[] inputShape = tflite.getInputTensor(0).shape();
                    int inputHeight = inputShape[1];
                    int inputWidth = inputShape[2];
                    int inputChannels = inputShape[3];

                    for (HashItem item : allPhotos) {
                        Bitmap bitmap;
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), item.uri);
                        } catch (Exception e) {
                            e.printStackTrace();
                            continue;
                        }

                        bitmap = Bitmap.createScaledBitmap(bitmap, inputWidth, inputHeight, true);
                        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(4 * inputHeight * inputWidth * inputChannels);
                        inputBuffer.order(ByteOrder.nativeOrder());

                        int[] intValues = new int[inputHeight * inputWidth];
                        bitmap.getPixels(intValues, 0, inputWidth, 0, 0, inputWidth, inputHeight);
                        for (int pixel : intValues) {
                            inputBuffer.putFloat(((pixel >> 16) & 0xFF) / 255.0f);
                            inputBuffer.putFloat(((pixel >> 8) & 0xFF) / 255.0f);
                            inputBuffer.putFloat((pixel & 0xFF) / 255.0f);
                        }

                        float[][] output = new float[1][1]; // Tek çıktı: hayvan olasılığı
                        tflite.run(inputBuffer, output);

                        if (output[0][0] > 0.5f) {
                            animalPhotos.add(item);
                        }
                    }

                } catch (Exception e) {
                    Log.e("AnimalDetection", "Hayvan tespiti sırasında hata: ", e);
                    runOnUiThread(() ->
                            Toast.makeText(activity, "Hayvan tespiti sırasında hata oluştu.", Toast.LENGTH_SHORT).show()
                    );
                }

                runOnUiThread(() -> {
                    if (animalPhotos.isEmpty()) {
                        Toast.makeText(activity, "Hayvan içeren fotoğraf bulunamadı.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    GridImageAdapter gridAdapter = new GridImageAdapter(activity, animalPhotos);
                    recyclerView.setAdapter(gridAdapter);

                    btnDateSort.setVisibility(View.GONE);
                    btnHumanSort.setVisibility(View.GONE);
                    btnAnimalSort.setVisibility(View.GONE);

                    recyclerView.setVisibility(RecyclerView.VISIBLE);
                });
            }).start();
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

        btnDateSort.setVisibility(Button.GONE);
        btnHumanSort.setVisibility(Button.GONE);
        btnAnimalSort.setVisibility(Button.GONE);

        recyclerView.setVisibility(RecyclerView.VISIBLE);
    }
}