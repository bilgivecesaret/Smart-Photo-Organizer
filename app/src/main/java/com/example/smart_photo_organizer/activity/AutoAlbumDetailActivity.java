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
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;
import com.example.smart_photo_organizer.adapter.SimilarGridAdapter;
import com.example.smart_photo_organizer.model.HashItem;
import java.util.ArrayList;
import java.util.List;

public class AutoAlbumDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CheckBox selectAll;
    private ProgressBar progressBar;
    private Button delete, cancel;
    private SimilarGridAdapter adapter;
    private long lastDeletedSize = 0;

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    showSuccessDialog(formatSize(lastDeletedSize));
                    adapter.removeSelectedImages();
                    setResult(RESULT_OK);
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
        selectAll = findViewById(R.id.cbSelectAll);
        delete = findViewById(R.id.btnDelete);
        cancel = findViewById(R.id.btnCancel);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerView.setAdapter(adapter);

        progressBar.setVisibility(View.GONE);

        String title = getIntent().getStringExtra("ALBUM_TITLE");
        ArrayList<HashItem> photos = getIntent().getParcelableArrayListExtra("ALBUM_PHOTOS");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        List<Uri> uriList = new ArrayList<>();
        if (photos != null) {
            for (HashItem item : photos) {
                uriList.add(item.uri);
            }
        }
        adapter = new SimilarGridAdapter(uriList, count -> updateUI(count));
        recyclerView.setAdapter(adapter);

        adapter.setOnImageClickListener((uri, position) -> {

            Intent intent = new Intent(this, PhotoViewerActivity.class);
            intent.putParcelableArrayListExtra("images", new ArrayList<>(uriList));
            intent.putExtra("position", position);

            startActivity(intent);
        });
        selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.selectAll(isChecked);
        });

        cancel.setOnClickListener(v -> {
            adapter.clearSelection();
        });

        delete.setOnClickListener(v -> {

            List<Uri> selected = adapter.getSelectedImages();

            if (selected.isEmpty()) return;

            lastDeletedSize = calculateTotalSize(selected);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

                try {

                    PendingIntent pi =
                            MediaStore.createDeleteRequest(getContentResolver(), selected);

                    deleteLauncher.launch(
                            new IntentSenderRequest.Builder(pi.getIntentSender()).build()
                    );

                } catch (Exception e) {
                    Toast.makeText(this, "Delete request failed", Toast.LENGTH_SHORT).show();
                }

            } else {

                for (Uri uri : selected) {
                    getContentResolver().delete(uri, null, null);
                }

                showSuccessDialog(formatSize(lastDeletedSize));
                adapter.removeSelectedImages();
            }
        });

        recyclerView = findViewById(R.id.recyclerGrid);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        // PROGRESS BAR'I DURDURAN KOD EKLENDİ
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

    }

    private void updateUI(int count) {

        if (count > 0) {

            delete.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.VISIBLE);

            delete.setText("Delete (" + count + ")");

        } else {

            delete.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
        }

        selectAll.setOnCheckedChangeListener(null);

        selectAll.setChecked(count == adapter.getItemCount());

        selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            adapter.selectAll(isChecked);
        });
    }

    private long calculateTotalSize(List<Uri> uris) {
        long totalSize = 0;
        for (Uri uri : uris) {
            try (Cursor cursor = getContentResolver().query(uri,
                    new String[]{MediaStore.Images.Media.SIZE}, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    totalSize += cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return totalSize;
    }

    // Boyutu okunabilir hale getirmek için (MB/KB)
    private String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    private void showSuccessDialog(String savedSpace) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Temizlik Tamamlandı!")
                .setMessage("Başarıyla " + savedSpace + " kadar alan boşaltıldı.")
                .setPositiveButton("Harika", (dialog, which) -> {
                    dialog.dismiss();
                    Intent result = new Intent();
                    result.putExtra("photos_deleted", true);
                    setResult(RESULT_OK, result);
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    public void showFullscreenContainer() {
        View container = findViewById(R.id.fragment_container);
        if (container != null) {
            container.setVisibility(View.VISIBLE);
            container.bringToFront();
            container.requestLayout();
            container.invalidate();
        }
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            findViewById(R.id.fragment_container).setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

}

