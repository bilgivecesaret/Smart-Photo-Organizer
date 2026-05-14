package com.example.smart_photo_organizer.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.AutoAlbumAdapter;
import com.example.smart_photo_organizer.model.AutoAlbum;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.AutoAlbumCreator;
import com.example.smart_photo_organizer.util.ContentFilter;
import com.example.smart_photo_organizer.util.ImageFetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View layoutButtons;
    private LinearLayout layoutLoading;

    private Button btnSortByDate;
    private Button btnFilterNature;
    private Button btnFilterFood;
    private Button btnFilterSport;
    private Button btnFilterVehicle;
    private Button btnFilterTech;

    private List<HashItem> allPhotos = new ArrayList<>();

    private enum SortType {
        GROUP_BY_DATE,
        NATURE_FILTER,
        FOOD_FILTER,
        SPORT_FILTER,
        VEHICLE_FILTER,
        TECH_FILTER
    }

    private static final Set<String> NATURE_KEYWORDS = new HashSet<>(Arrays.asList(
            "nature", "forest", "tree", "mountain", "beach", "sky", "cloud",
            "grass", "lake", "river", "waterfall", "flower", "plant", "landscape",
            "field", "sunset", "sunrise", "ocean", "sea", "valley", "wilderness",
            "jungle", "desert", "glacier", "canyon", "meadow", "fog", "rainforest"
    ));

    private static final Set<String> FOOD_KEYWORDS = new HashSet<>(Arrays.asList(
            "food", "fruit", "vegetable", "meal", "dish", "bread", "salad",
            "dessert", "drink", "beverage", "meat", "pizza", "burger", "cake",
            "coffee", "soup", "rice", "pasta", "sandwich", "snack"
    ));

    private static final Set<String> SPORT_KEYWORDS = new HashSet<>(Arrays.asList(
            "sport", "sports", "football", "basketball", "tennis", "swimming",
            "running", "cycling", "volleyball", "baseball", "soccer", "gym",
            "fitness", "athlete", "stadium", "race", "competition", "workout",
            "exercise", "ball", "court", "track", "skiing", "surfing"
    ));

    private static final Set<String> VEHICLE_KEYWORDS = new HashSet<>(Arrays.asList(
            "car", "vehicle", "automobile", "motorcycle", "bicycle", "bus",
            "truck", "train", "boat", "ship", "aircraft", "airplane", "helicopter",
            "van", "taxi", "ambulance", "tractor", "scooter", "yacht", "ferry"
    ));

    private static final Set<String> TECH_KEYWORDS = new HashSet<>(Arrays.asList(
            "telephone", "mobile phone", "smartphone", "computer", "laptop",
            "television", "camera", "screen", "monitor", "keyboard", "tablet",
            "headphones", "speaker", "microphone", "drone", "robot",
            "electronics", "gadget", "device", "charger", "remote control"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auto_album);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.autoAlbumActivity), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right,0);
            return insets;
        });

        recyclerView       = findViewById(R.id.rvAutoAlbums);
        layoutButtons      = findViewById(R.id.layoutButtons);
        layoutLoading      = findViewById(R.id.layoutLoading);

        btnSortByDate      = findViewById(R.id.btnSortByDate);
        btnFilterNature    = findViewById(R.id.btnFilterNature);
        btnFilterFood      = findViewById(R.id.btnFilterFood);
        btnFilterSport     = findViewById(R.id.btnFilterSport);
        btnFilterVehicle   = findViewById(R.id.btnFilterVehicle);
        btnFilterTech      = findViewById(R.id.btnFilterTech);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setVisibility(View.GONE);

        btnSortByDate.setOnClickListener(v -> loadAndShow(SortType.GROUP_BY_DATE));
        btnFilterNature.setOnClickListener(v -> loadAndShow(SortType.NATURE_FILTER));
        btnFilterFood.setOnClickListener(v -> loadAndShow(SortType.FOOD_FILTER));
        btnFilterSport.setOnClickListener(v -> loadAndShow(SortType.SPORT_FILTER));
        btnFilterVehicle.setOnClickListener(v -> loadAndShow(SortType.VEHICLE_FILTER));
        btnFilterTech.setOnClickListener(v -> loadAndShow(SortType.TECH_FILTER));
    }

    private void loadAndShow(SortType type) {
        allPhotos = new ArrayList<>();
        layoutLoading.setVisibility(View.VISIBLE);
        layoutButtons.setVisibility(View.GONE);

        ImageFetcher.loadAllImagesAsync(this, 20, new ImageFetcher.ImageBatchCallback() {
            @Override
            public void onBatch(List<HashItem> batch) {
                allPhotos.addAll(batch);
            }

            @Override
            public void onComplete() {
                runOnUiThread(() -> processAndShow(type));
            }
        });
    }

    private void processAndShow(SortType type) {

        if (type == SortType.GROUP_BY_DATE) {
            showAlbums(AutoAlbumCreator.createAutoAlbums(this, allPhotos));
            return;
        }

        Set<String> keywords;
        String albumTitle;

        switch (type) {
            case NATURE_FILTER:
                keywords = NATURE_KEYWORDS;
                albumTitle = getString(R.string.albumTitleNature);
                break;
            case FOOD_FILTER:
                keywords = FOOD_KEYWORDS;
                albumTitle = getString(R.string.albumTitleFood);
                break;
            case SPORT_FILTER:
                keywords = SPORT_KEYWORDS;
                albumTitle = getString(R.string.albumTitleSport);
                break;
            case VEHICLE_FILTER:
                keywords = VEHICLE_KEYWORDS;
                albumTitle = getString(R.string.albumTitleVehicle);
                break;
            default:
                keywords = TECH_KEYWORDS;
                albumTitle = getString(R.string.albumTitleTech);
                break;
        }

        new ContentFilter(keywords).filter(this, allPhotos, result -> {
            List<AutoAlbum> albums = new ArrayList<>();
            albums.add(new AutoAlbum(albumTitle + " (" + result.size() + ")", result));
            runOnUiThread(() -> showAlbums(albums));
        });
    }

    private void showAlbums(List<AutoAlbum> albums) {
        recyclerView.setAdapter(new AutoAlbumAdapter(this, albums));
        layoutLoading.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }
}