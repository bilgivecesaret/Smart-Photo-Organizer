package com.example.smart_photo_organizer;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    CheckBox cleanupCheckbox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cleanupCheckbox = findViewById(R.id.checkbox_cleanup);

        cleanupCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Toast.makeText(this, "Cleanup onaylandı", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cleanup iptal edildi", Toast.LENGTH_SHORT).show();
            }
        });

        // Albüm, sonuç vb. için benzer şekilde butonlara ya da imageView'lara listener eklenebilir.
    }
}
