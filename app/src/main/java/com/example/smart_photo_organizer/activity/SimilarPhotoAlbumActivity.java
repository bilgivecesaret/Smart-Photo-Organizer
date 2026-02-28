package com.example.smart_photo_organizer.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.SimilarAlbumAdapter;
import com.example.smart_photo_organizer.model.DuplicateGroup;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.ImageFeatureExtractor;
import com.example.smart_photo_organizer.util.ImageFetcher;
import com.example.smart_photo_organizer.util.OpenCvMSEUtil;
import com.example.smart_photo_organizer.util.UnionFind;

import org.opencv.android.OpenCVLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class SimilarPhotoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private final List<HashItem> allImages = new ArrayList<>();
    private final List<DuplicateGroup> groups = new ArrayList<>();
    private SimilarAlbumAdapter adapter;
    private static final double FAST_THRESHOLD = 3000; // L2 ön filtre
    private static final double MSE_THRESHOLD = 2000;   // Kesin benzerlik
    private ActivityResultLauncher<Intent> gridLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVLoader.initDebug();

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_similar_albums);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.similarPhotoAlbum), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerSimilarAlbums);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        gridLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getBooleanExtra("photos_deleted", false)) {
                            startScan();
                        }
                    }
                }
        );

        startScan();
    }

    private void startScan() {
        allImages.clear();
        groups.clear();

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);

        ImageFetcher.loadAllImagesAsync(
                this,
                50,
                new ImageFetcher.ImageBatchCallback() {
                    @Override
                    public void onBatch(List<HashItem> batch) {
                        allImages.addAll(batch);
                    }
                    @Override
                    public void onComplete() {
                        buildGroups();
                    }
                }
        );
    }

    private void buildGroups() {

        Executors.newSingleThreadExecutor().execute(() -> {

            int n = allImages.size();

            float[][] features = new float[n][];

            for (int i = 0; i < n; i++) {
                features[i] = ImageFeatureExtractor.extractFeature(
                        this,
                        allImages.get(i).uri
                );
            }

            UnionFind uf = new UnionFind(n);

            for (int i = 0; i < n; i++) {

                for (int j = i + 1; j < n; j++) {

                    double fastDist = ImageFeatureExtractor.l2Distance(
                            features[i],
                            features[j]
                    );

                    if (fastDist < FAST_THRESHOLD) {

                        double mse = OpenCvMSEUtil.calculateMSE(
                                this,
                                allImages.get(i).uri,
                                allImages.get(j).uri
                        );

                        if (mse < MSE_THRESHOLD) {
                            uf.union(i, j);
                        }
                    }
                }
            }

            // Cluster oluştur
            Map<Integer, List<Uri>> map = new HashMap<>();

            for (int i = 0; i < n; i++) {
                int root = uf.find(i);
                map.putIfAbsent(root, new ArrayList<>());
                map.get(root).add(allImages.get(i).uri);
            }

            for (List<Uri> cluster : map.values()) {
                if (cluster.size() > 1) {
                    groups.add(new DuplicateGroup(0L, cluster));
                }
            }

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                adapter = new SimilarAlbumAdapter(
                        this,
                        groups,
                        (intent, pos) -> gridLauncher.launch(intent)
                );
                recyclerView.setAdapter(adapter);
            });
        });
    }
}
