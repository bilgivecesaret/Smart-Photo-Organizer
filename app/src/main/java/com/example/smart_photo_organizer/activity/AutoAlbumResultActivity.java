package com.example.smart_photo_organizer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_photo_organizer.R;

public class AutoAlbumResultActivity extends AppCompatActivity {

    private Button btnSortByDate;
    private Button btnSortByHuman;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_album);

        btnSortByDate = findViewById(R.id.btnSortByDate);
        btnSortByHuman = findViewById(R.id.btnSortByHuman);

        // Tarihe göre sıralama ekranına geçiş
        btnSortByDate.setOnClickListener(v -> {
            Intent intent = new Intent(AutoAlbumResultActivity.this, AutoAlbumDisplayActivity.class);
            intent.putExtra("SORT_TYPE", "DATE");
            startActivity(intent);
        });

        // İnsan olanları sıralama ekranına geçiş
        btnSortByHuman.setOnClickListener(v -> {
            Intent intent = new Intent(AutoAlbumResultActivity.this, AutoAlbumDisplayActivity.class);
            intent.putExtra("SORT_TYPE", "HUMAN");
            startActivity(intent);
        });
    }
}