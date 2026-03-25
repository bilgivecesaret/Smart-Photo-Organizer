package com.example.smart_photo_organizer.activity;

import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.SimilarGridAdapter;
import com.example.smart_photo_organizer.util.BlurDetector;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BlurredPhotoActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout topBar;
    private CheckBox cbSelectAll;
    private Button btnDelete, btnCancel;

    private SimilarGridAdapter adapter;
    private final List<Uri> blurredUris = Collections.synchronizedList(new ArrayList<>());
    private final List<ScanItem> candidateImages = new ArrayList<>();
    private static class ScanItem {
        long id;
        String path;

        ScanItem(long id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            adapter.removeSelectedImages();
                            updateUI(0);
                            Toast.makeText(this, "Photos deleted", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_blurred_photo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV Problem!", Toast.LENGTH_LONG).show();
            return;
        }

        initViews();
        startAdvancedBlurScan();
    }

    private void initViews() {

        recyclerView = findViewById(R.id.recyclerGrid);
        progressBar = findViewById(R.id.progressBarBlur);
        topBar = findViewById(R.id.topBar);
        cbSelectAll = findViewById(R.id.cbSelectAll);
        btnDelete = findViewById(R.id.btnDelete);
        btnCancel = findViewById(R.id.btnCancel);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        btnDelete.setOnClickListener(v -> deleteSelectedPhotos());
        btnCancel.setOnClickListener(v -> {
            adapter.clearSelection();
            btnDelete.setVisibility(View.GONE);
            btnCancel.setVisibility(View.GONE);
        });

        cbSelectAll.setOnClickListener(v -> {
            if (adapter != null) {
                adapter.selectAll(cbSelectAll.isChecked());
            }
        });
    }

    private void startAdvancedBlurScan() {
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        blurredUris.clear();
        candidateImages.clear();

        new Thread(this::fetchCandidates).start();
    }

    private void fetchCandidates() {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATA};

        Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String path = cursor.getString(1);

                if (path != null && new File(path).exists()) {
                    candidateImages.add(new ScanItem(id, path));
                }
            }
            cursor.close();
        }

        runOnUiThread(this::processImagesInParallel);
    }

    private void processImagesInParallel() {
        if (candidateImages.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "No photos to scan were found.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setIndeterminate(false);
        progressBar.setMax(candidateImages.size());
        progressBar.setProgress(0);

        int cores = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(cores);

        AtomicInteger processedCount = new AtomicInteger(0);

        for (ScanItem item : candidateImages) {
            executor.execute(() -> {
                if (BlurDetector.isBlurry(item.path)) {
                    Uri contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, item.id);
                    blurredUris.add(contentUri);
                }

                int progress = processedCount.incrementAndGet();
                runOnUiThread(() -> progressBar.setProgress(progress));
            });
        }

        executor.shutdown();

        new Thread(() -> {
            try {
                while (!executor.isTerminated()) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            runOnUiThread(this::setupAdapter);
        }).start();
    }

    private void setupAdapter() {
        progressBar.setVisibility(View.GONE);

        if (blurredUris.isEmpty()) {
            Toast.makeText(this, "Bulanık fotoğraf bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        topBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.VISIBLE);

        adapter = new SimilarGridAdapter(new ArrayList<>(blurredUris), this::updateUI);

        adapter.setSelectionListener(count -> updateUI(count));

        adapter.setOnImageClickListener((uri, position) -> {
            // Tek tık ile full screen PhotoViewerActivity aç
            Intent intent = new Intent(BlurredPhotoActivity.this, PhotoViewerActivity.class);
            intent.putParcelableArrayListExtra("images", new ArrayList<>(blurredUris));
            intent.putExtra("position", position);
            startActivity(intent);
        });

        recyclerView.setAdapter(adapter);

        boolean fromNotification = getIntent().getBooleanExtra("from_notification", false);
        if (fromNotification) {
            adapter.selectAll(true);
            cbSelectAll.setChecked(true);
            updateUI(adapter.getItemCount());
        }

    }

    private void updateUI(int count) {
        if (count > 0) {
            btnCancel.setVisibility(View.VISIBLE);
            btnDelete.setVisibility(View.VISIBLE);
            btnDelete.setText("Delete (" + count + ")");
        } else {
            btnCancel.setVisibility(View.GONE);
            btnDelete.setVisibility(View.GONE);
        }

        cbSelectAll.setOnCheckedChangeListener(null);
        cbSelectAll.setChecked(adapter.getItemCount() > 0 && count == adapter.getItemCount());

        cbSelectAll.setOnClickListener(v -> {
            adapter.selectAll(cbSelectAll.isChecked());
        });
    }

    private void deleteSelectedPhotos() {
        ArrayList<Uri> selected = new ArrayList<>(adapter.getSelectedImages());
        if (selected.isEmpty()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                PendingIntent pi = MediaStore.createDeleteRequest(getContentResolver(), selected);
                deleteLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "The deletion request could not be started", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            for (Uri uri : selected) {
                try {
                    getContentResolver().delete(uri, null, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            adapter.removeSelectedImages();
            updateUI(0); // UI'ı sıfırla
            Toast.makeText(this, "Photos have been deleted", Toast.LENGTH_SHORT).show();
        }
    }
}
