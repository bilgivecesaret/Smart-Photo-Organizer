package com.example.smart_photo_organizer.activity;

import android.app.PendingIntent;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.example.smart_photo_organizer.adapter.DuplicateGridAdapter;

import java.util.ArrayList;

public class DuplicatePhotoGridActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    CheckBox selectAll;
    Button delete, cancel;
    DuplicateGridAdapter adapter;

    private final ActivityResultLauncher<IntentSenderRequest> deleteLauncher =
            registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            adapter.removeSelectedImages();
                            Toast.makeText(this, "Photos deleted", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_duplicate_grid);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.duplicatePhotoGrid), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerGrid);
        selectAll = findViewById(R.id.cbSelectAll);
        delete = findViewById(R.id.btnDelete);
        cancel = findViewById(R.id.btnCancel);

        ArrayList<Uri> images = getIntent().getParcelableArrayListExtra("images");

        adapter = new DuplicateGridAdapter(images);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setAdapter(adapter);

        adapter.setSelectionListener(count -> {
            boolean hasSelection = count > 0;
            delete.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
            cancel.setVisibility(hasSelection ? View.VISIBLE : View.GONE);

            selectAll.setOnCheckedChangeListener(null);
            selectAll.setChecked(count == adapter.getItemCount());
            selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                adapter.selectAll(isChecked);
            });
        });

        selectAll.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.selectAll(isChecked));
        cancel.setOnClickListener(v -> adapter.clearSelection());

        delete.setOnClickListener(v -> {
            ArrayList<Uri> selected = new ArrayList<>(adapter.getSelectedImages());
            if (selected.isEmpty()) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    PendingIntent pi = android.provider.MediaStore.createDeleteRequest(getContentResolver(), selected);
                    deleteLauncher.launch(new IntentSenderRequest.Builder(pi.getIntentSender()).build());
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Cannot start delete request", Toast.LENGTH_SHORT).show();
                }
            } else {
                for (Uri uri : selected) {
                    try {
                        getContentResolver().delete(uri, null, null);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Cannot delete: " + uri.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
                adapter.removeSelectedImages();
                Toast.makeText(this, "Photos deleted", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
