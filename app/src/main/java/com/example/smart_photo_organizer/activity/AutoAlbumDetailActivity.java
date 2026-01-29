package com.example.smart_photo_organizer.activity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;
import com.example.smart_photo_organizer.model.HashItem;
import java.util.ArrayList;
import java.util.List;

public class AutoAlbumDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auto_album_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.autoAlbumActivityDetail), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

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

        RecyclerView recyclerView = findViewById(R.id.recyclerSimilarAlbums);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 4));

        // PROGRESS BAR'I DURDURAN KOD EKLENDİ
        ProgressBar progressBar = findViewById(R.id.progressBar);
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        ImageGridAdapter adapter = new ImageGridAdapter(this, uriList, getSupportFragmentManager());
        recyclerView.setAdapter(adapter);
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

