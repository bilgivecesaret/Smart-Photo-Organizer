package com.example.smart_photo_organizer.activity;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;

import java.util.ArrayList;

public class ImageGridActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    ImageGridAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_image_grid);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_image_grid), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.recyclerImages);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        ArrayList<String> images = getIntent().getStringArrayListExtra("images");

        if (images == null || images.isEmpty()) {
            finish(); // döngü olmuyor, sadece kapanır
            return;
        }


        adapter = new ImageGridAdapter(this, images);
        recyclerView.setAdapter(adapter);
    }
}

