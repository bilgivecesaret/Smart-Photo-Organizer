package com.example.smart_photo_organizer.activity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.PhotoViewerAdapter;

import java.util.ArrayList;

public class PhotoViewerActivity extends AppCompatActivity {

    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
        setContentView(R.layout.activity_photo_viewer);

        viewPager = findViewById(R.id.viewPager);

        ArrayList<Uri> images =
                getIntent().getParcelableArrayListExtra("images");

        int position =
                getIntent().getIntExtra("position",0);

        PhotoViewerAdapter adapter =
                new PhotoViewerAdapter(images);

        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(position,false);
    }
}