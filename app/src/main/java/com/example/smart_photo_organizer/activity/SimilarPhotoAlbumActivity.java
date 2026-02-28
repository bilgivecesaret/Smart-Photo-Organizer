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
import com.example.smart_photo_organizer.util.AIEmbeddingUtil;
import com.example.smart_photo_organizer.util.ImageFetcher;
import com.example.smart_photo_organizer.util.UnionFind;

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
    private static final double AI_THRESHOLD = 0.80;
    private ActivityResultLauncher<Intent> gridLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
            AIEmbeddingUtil ai = new AIEmbeddingUtil(this);

            float[][] embeddings = new float[n][];

            for (int i = 0; i < n; i++) {
                embeddings[i] = ai.getEmbedding(this, allImages.get(i).uri);
            }

            UnionFind uf = new UnionFind(n);

            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (embeddings[i] == null || embeddings[j] == null) {
                        continue;
                    }
                    double similarity =
                            AIEmbeddingUtil.cosineSimilarity(
                                    embeddings[i],
                                    embeddings[j]
                            );
                    if (similarity > AI_THRESHOLD) {
                        uf.union(i, j);
                    }
                }
            }

            Map<Integer, List<Uri>> map = new HashMap<>();

            for (int i = 0; i < n; i++) {
                int root = uf.find(i);
                map.putIfAbsent(root, new ArrayList<>());
                map.get(root).add(allImages.get(i).uri);
            }

            for (List<Uri> cluster : map.values()) {
                if (cluster.size() > 1)
                    groups.add(new DuplicateGroup(cluster));
            }

            runOnUiThread(() -> {
                if (groups.isEmpty()) {

                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                            .setTitle("No Similar Photos")
                            .setMessage("No similar photos were found on your device.")
                            .setPositiveButton("OK", (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            })
                            .setCancelable(false)
                            .show();

                    return;
                }
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
