package com.example.smart_photo_organizer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_photo_organizer.R;

public class AutoAlbumResultActivity extends AppCompatActivity {

    private Button btnSortByDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 🔥 KRİTİK SATIR (EKSİK OLAN BUYDU)
        setContentView(R.layout.activity_auto_album_result);

        btnSortByDate = findViewById(R.id.btnSortByDate);

        btnSortByDate.setOnClickListener(v -> {
            Intent intent = new Intent(
                    AutoAlbumResultActivity.this,
                    AutoAlbumDisplayActivity.class
            );
            intent.putExtra("SORT_TYPE", "DATE");
            startActivity(intent);
        });
    }
}