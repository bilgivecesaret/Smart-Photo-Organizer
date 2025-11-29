package com.example.smart_photo_organizer;

import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

public class FullscreenActivity extends AppCompatActivity {
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen);

        imageView = findViewById(R.id.fullImageView);

        String path = getIntent().getStringExtra("imagePath");

        Glide.with(this)
                .load(path)
                .into(imageView);
    }
}

