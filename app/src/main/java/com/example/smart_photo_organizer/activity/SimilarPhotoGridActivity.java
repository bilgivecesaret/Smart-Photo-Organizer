package com.example.smart_photo_organizer.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.smart_photo_organizer.util.Notification;
import com.example.smart_photo_organizer.util.SimilarPhotoCache;

import java.util.ArrayList;
import java.util.List;

public class SimilarPhotoGridActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CheckBox selectAll;
    private ProgressBar progressBar;
    private Button delete, cancel;
    private SimilarGridAdapter adapter;
    ArrayList<Uri> images;
    private long lastDeletedSize = 0;

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Notification.showSuccessDialog(this,Notification.formatSize(lastDeletedSize));
                    adapter.removeSelectedImages();
                    setResult(RESULT_OK);
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_grid_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.photosGridView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerGrid);
        progressBar = findViewById(R.id.progressBar);
        selectAll = findViewById(R.id.cbSelectAll);
        delete = findViewById(R.id.btnDelete);
        cancel = findViewById(R.id.btnCancel);

        boolean useCache = getIntent().getBooleanExtra("use_cache", false);
        if (useCache && SimilarPhotoCache.cachedUris != null && !SimilarPhotoCache.cachedUris.isEmpty()) {
            images = new ArrayList<>(SimilarPhotoCache.cachedUris);
        } else {
            images = getIntent().getParcelableArrayListExtra("images");
        }

        boolean fromAuto = getIntent().getBooleanExtra("from_auto_cleanup", false);
        progressBar.setVisibility(View.GONE);
        if (images == null || images.isEmpty()) {
            Toast.makeText(this, "No photos were found to display.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        adapter = new SimilarGridAdapter(images, count -> {
            updateUI(count);
        });

        adapter.selectAll(true);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(adapter);

        if (fromAuto && images != null && !images.isEmpty()) {
            Notification.showAutoCleanupInfoDialog(this,images.size());
        }
        adapter.setOnImageClickListener((uri, position) -> {

            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putParcelableArrayListExtra("images", images);
            intent.putExtra("position", position);

            startActivity(intent);

        });

        setupSelectAllListener();

        cancel.setOnClickListener(v -> {
            adapter.clearSelection();
            delete.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
        });

        adapter.setSelectionListener(count -> {
            if (count > 0) {
                cancel.setVisibility(View.VISIBLE);
                delete.setVisibility(View.VISIBLE);
                delete.setText("Delete (" + count + ")");
            } else {
                cancel.setVisibility(View.GONE);
                delete.setVisibility(View.GONE);
            }

            selectAll.setOnCheckedChangeListener(null);
            selectAll.setChecked(count == adapter.getItemCount());
            selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.selectAll(isChecked));
        });

        delete.setOnClickListener(v -> {
            List<Uri> selected = adapter.getSelectedImages();
            if (selected.isEmpty()) return;
            lastDeletedSize = Notification.calculateTotalSize(this,selected);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    // Android 11+ güvenli silme
                    PendingIntent pi = android.provider.MediaStore.createDeleteRequest(getContentResolver(), selected);
                    deleteLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot start delete request", Toast.LENGTH_SHORT).show();
                }
            } else {
                // Android 10 ve altı
                for (Uri uri : selected) {
                    try {
                        getContentResolver().delete(uri, null, null);
                    } catch (SecurityException e) {
                        Toast.makeText(this, "Cannot delete: " + uri.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
                Notification.showSuccessDialog(this,Notification.formatSize(lastDeletedSize));
                adapter.removeSelectedImages();
                Toast.makeText(this, "Photos deleted", Toast.LENGTH_SHORT).show();
                Intent result = new Intent();
                result.putExtra("photos_deleted", true);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    private void updateUI(int count) {
        if (count > 0) {
            delete.setVisibility(View.VISIBLE);
            delete.setText("Delete (" + count + ")");
        } else {
            delete.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
        }

        // Select All Checkbox'ının durumunu güncelle ama sonsuz döngüyü engelle
        selectAll.setOnCheckedChangeListener(null);
        selectAll.setChecked(count == adapter.getItemCount() && adapter.getItemCount() > 0);
        setupSelectAllListener(); // Listener'ı tekrar bağla
    }

    private void setupSelectAllListener() {
        selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.selectAll(isChecked);
        });
    }

}
