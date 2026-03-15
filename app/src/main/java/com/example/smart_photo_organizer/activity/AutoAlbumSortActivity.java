package com.example.smart_photo_organizer.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smart_photo_organizer.R;

public class AutoAlbumSortActivity extends AppCompatActivity {

    public static final String EXTRA_SORT_TYPE = "SORT_TYPE";
    public static final String SORT_DATE = "DATE";
    public static final String SORT_HUMAN = "HUMAN";

    private Button btnSortByDate, btnSortByHuman;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auto_album_sort);

        btnSortByDate = findViewById(R.id.btnDateSort);
        btnSortByHuman = findViewById(R.id.btnHumanSort);

        btnSortByDate.setOnClickListener(v -> openResultActivity(SORT_DATE));
        btnSortByHuman.setOnClickListener(v -> openResultActivity(SORT_HUMAN));
    }

    private void openResultActivity(String sortType) {
        Intent intent = new Intent(this, AutoAlbumActivity.class); // AutoAlbumActivity açılır
        intent.putExtra(EXTRA_SORT_TYPE, sortType);
        startActivity(intent);
    }
}