package com.example.smart_photo_organizer.activity;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AutoAlbumActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private View layoutButtons;
    private LinearLayout layoutLoading;

    private Button btnSortByDate;
    private Button btnFilterLocation;
    private Button btnFilterNature;
    private Button btnFilterFood;
    private Button btnFilterAnimal;

    private List<HashItem> allPhotos = new ArrayList<>();

    private enum SortType {
        GROUP_BY_DATE,
        NATURE_FILTER,
        FOOD_FILTER,
        ANIMAL_FILTER
    }

    // ─── Keyword listeleri ─────────────────────────────────────────
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

    private static final Set<String> ANIMAL_KEYWORDS = new HashSet<>(Arrays.asList(
            "dog", "cat", "bird", "horse", "cow", "sheep", "elephant",
            "lion", "tiger", "bear", "deer", "rabbit", "fish", "snake",
            "puppy", "kitten", "monkey", "gorilla", "wolf", "fox",
            "duck", "chicken", "penguin", "dolphin", "whale", "turtle",
            "lizard", "frog", "hamster", "parrot", "eagle", "owl"
    ));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auto_album);

        recyclerView      = findViewById(R.id.rvAutoAlbums);
        layoutButtons     = findViewById(R.id.layoutButtons);
        layoutLoading     = findViewById(R.id.layoutLoading);

        btnSortByDate     = findViewById(R.id.btnSortByDate);
        btnFilterNature   = findViewById(R.id.btnFilterNature);
        btnFilterFood     = findViewById(R.id.btnFilterFood);
        btnFilterAnimal   = findViewById(R.id.btnFilterAnimal);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setVisibility(View.GONE);

        btnSortByDate.setOnClickListener(v -> loadAndShow(SortType.GROUP_BY_DATE));
        btnFilterNature.setOnClickListener(v -> loadAndShow(SortType.NATURE_FILTER));
        btnFilterFood.setOnClickListener(v -> loadAndShow(SortType.FOOD_FILTER));
        btnFilterAnimal.setOnClickListener(v -> loadAndShow(SortType.ANIMAL_FILTER));
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

        // AI filtreleri async çalışır — ayrı dalda işle
        if (type == SortType.NATURE_FILTER ||
                type == SortType.FOOD_FILTER ||
                type == SortType.ANIMAL_FILTER) {

            Set<String> keywords;
            String albumTitle;

            if (type == SortType.NATURE_FILTER) {
                keywords = NATURE_KEYWORDS;
                albumTitle = "🌿 Doğa Fotoğrafları";
            } else if (type == SortType.FOOD_FILTER) {
                keywords = FOOD_KEYWORDS;
                albumTitle = "🍎 Yiyecek Fotoğrafları";
            } else {
                keywords = ANIMAL_KEYWORDS;
                albumTitle = "🐾 Hayvan Fotoğrafları";
            }

            new ContentFilter(keywords).filter(this, allPhotos, result -> {
                List<AutoAlbum> albums = new ArrayList<>();
                albums.add(new AutoAlbum(albumTitle + " (" + result.size() + ")", result));
                runOnUiThread(() -> showAlbums(albums));
            });

            return;
        }

        // Senkron işlemler
        List<AutoAlbum> albums = new ArrayList<>();

        switch (type) {
            case GROUP_BY_DATE:
                albums = AutoAlbumCreator.createAutoAlbums(this, allPhotos);
                break;
        }

        showAlbums(albums);
    }

    private void showAlbums(List<AutoAlbum> albums) {
        recyclerView.setAdapter(new AutoAlbumAdapter(this, albums));
        layoutLoading.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
    }

    private List<AutoAlbum> groupByCity(List<HashItem> photos) {
        Map<String, List<HashItem>> cityMap = new LinkedHashMap<>();
        List<HashItem> withoutLocation = new ArrayList<>();
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        for (HashItem item : photos) {
            if (item.latitude == 0 && item.longitude == 0) {
                withoutLocation.add(item);
                continue;
            }
            String cityKey = getCityName(geocoder, item.latitude, item.longitude);
            if (!cityMap.containsKey(cityKey)) cityMap.put(cityKey, new ArrayList<>());
            cityMap.get(cityKey).add(item);
        }

        List<AutoAlbum> result = new ArrayList<>();
        for (Map.Entry<String, List<HashItem>> e : cityMap.entrySet())
            result.add(new AutoAlbum("📍 " + e.getKey(), e.getValue()));

        if (!withoutLocation.isEmpty())
            result.add(new AutoAlbum("📍 Konum Bilgisi Yok", withoutLocation));

        return result;
    }

    private String getCityName(Geocoder geocoder, double lat, double lon) {
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                String city = address.getAdminArea();
                String district = address.getSubAdminArea();
                if (city != null && district != null) return city + " / " + district;
                if (city != null) return city;
            }
        } catch (Exception ignored) {}

        double roundedLat = Math.round(lat * 10.0) / 10.0;
        double roundedLon = Math.round(lon * 10.0) / 10.0;
        return roundedLat + ", " + roundedLon;
    }
}