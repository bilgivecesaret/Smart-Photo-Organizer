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
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.Notification;

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
                    adapter.removeSelectedImages();
                    Notification.showSuccessDialog(
                            this,
                            Notification.formatSize(lastDeletedSize),
                            () -> {
                                setResult(RESULT_OK);
                                finish();
                            }
                    );
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
            lastDeletedSize = Notification.calculateTotalSize(getBaseContext(),selected);

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

            delete.setText(getString(R.string.delete) + " (" + count + ")");

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

