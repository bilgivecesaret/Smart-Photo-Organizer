package com.example.smart_photo_organizer.activity;

import static com.example.smart_photo_organizer.util.LocaleHelper.setLocale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.util.FullMediaScan;

import java.util.concurrent.Executors;

public class SplashScreenActivity extends AppCompatActivity {
    SharedPreferences prefs;
    String lang;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        lang = prefs.getString("app_language", "en");
        setLocale(lang);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_splash), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        Executors.newSingleThreadExecutor().execute(() -> {

            // MediaStore refresh
            FullMediaScan.rescanAllPublicMedia(this);

            // 2 saniye sonra ana ekrana geç
            runOnUiThread(() -> {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }, 2000);
            });
        });
    }

}


