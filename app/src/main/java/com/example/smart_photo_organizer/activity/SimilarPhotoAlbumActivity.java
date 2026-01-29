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
import com.example.smart_photo_organizer.util.ImageFetcher;
import com.example.smart_photo_organizer.util.ImagePHash;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SimilarPhotoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private final List<HashItem> allImages = new ArrayList<>();
    private final List<DuplicateGroup> groups = new ArrayList<>();

    private SimilarAlbumAdapter adapter;

    private static final int HAMMING_THRESHOLD = 1;

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
                20,
                new ImageFetcher.ImageBatchCallback() {

                    @Override
                    public void onBatch(List<HashItem> batch) {
                        allImages.addAll(batch);
                    }

                    @Override
                    public void onComplete() {
                        calculateHashes();
                    }
                }
        );
    }

    private void calculateHashes() {

        progressBar.setIndeterminate(false);
        progressBar.setMax(allImages.size());

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        Runtime.getRuntime().availableProcessors()
                );

        AtomicInteger done = new AtomicInteger(0);

        for (HashItem item : allImages) {
            executor.execute(() -> {

                if (item.hash == 0L) {
                    item.hash = ImagePHash.calculateHash(
                            this, item.uri
                    );
                }

                int progress = done.incrementAndGet();
                runOnUiThread(() -> progressBar.setProgress(progress));
            });
        }

        executor.shutdown();

        Executors.newSingleThreadExecutor().execute(() -> {
            while (!executor.isTerminated()) { }

            buildGroups();

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                adapter = new SimilarAlbumAdapter(this, groups,
                        (intent, pos) -> gridLauncher.launch(intent));
                recyclerView.setAdapter(adapter);
            });
        });
    }

    private void buildGroups() {
        List<List<HashItem>> temp = new ArrayList<>();

        for (HashItem item : allImages) {
            boolean added = false;

            for (List<HashItem> group : temp) {

                boolean fitsGroup = true;

                for (HashItem member : group) {
                    if (ImagePHash.hammingDistance(
                            item.hash,
                            member.hash
                    ) > HAMMING_THRESHOLD) {
                        fitsGroup = false;
                        break;
                    }
                }

                if (fitsGroup) {
                    group.add(item);
                    added = true;
                    break;
                }
            }

            if (!added) {
                List<HashItem> newGroup = new ArrayList<>();
                newGroup.add(item);
                temp.add(newGroup);
            }
        }

        for (List<HashItem> g : temp) {
            if (g.size() > 1) {
                List<Uri> uris = new ArrayList<>();
                for (HashItem h : g) uris.add(h.uri);

                groups.add(new DuplicateGroup(
                        g.get(0).hash,
                        uris
                ));
            }
        }
    }

}
