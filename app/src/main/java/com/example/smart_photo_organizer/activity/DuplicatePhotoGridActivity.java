package com.example.smart_photo_organizer.activity;

import android.app.PendingIntent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.DuplicateGridAdapter;

import java.util.ArrayList;
import java.util.List;

public class DuplicatePhotoGridActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CheckBox selectAll;
    private Button delete, cancel;
    private DuplicateGridAdapter adapter;

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            adapter.removeSelectedImages();
                            Toast.makeText(this, "Photos deleted", Toast.LENGTH_SHORT).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_duplicate_grid);

        recyclerView = findViewById(R.id.recyclerGrid);
        selectAll = findViewById(R.id.cbSelectAll);
        delete = findViewById(R.id.btnDelete);
        cancel = findViewById(R.id.btnCancel);

        ArrayList<Uri> images = getIntent().getParcelableArrayListExtra("images");
        if (images == null) images = new ArrayList<>();

        adapter = new DuplicateGridAdapter(images, count -> {
            boolean hasSelection = count > 0;
            delete.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
            cancel.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
            selectAll.setOnCheckedChangeListener(null);
            selectAll.setChecked(count == adapter.getItemCount() && adapter.getItemCount() > 0);
            selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.selectAll(isChecked));
        });

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

        adapter.setSelectionListener(count -> {
            boolean hasSelection = count > 0;
            delete.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
            cancel.setVisibility(hasSelection ? View.VISIBLE : View.GONE);

            selectAll.setOnCheckedChangeListener(null);
            selectAll.setChecked(count == adapter.getItemCount());
            selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.selectAll(isChecked));
        });

        selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.selectAll(isChecked));

        cancel.setOnClickListener(v -> adapter.clearSelection());

        delete.setOnClickListener(v -> {
            List<Uri> selected = adapter.getSelectedImages();
            if (selected.isEmpty()) return;

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
                adapter.removeSelectedImages();
                Toast.makeText(this, "Photos deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
