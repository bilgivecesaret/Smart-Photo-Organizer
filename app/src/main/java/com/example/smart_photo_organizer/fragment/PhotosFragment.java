package com.example.smart_photo_organizer.fragment;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.content.ContentResolver;


import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.ImageFetcher;

import java.util.ArrayList;
import java.util.concurrent.Executors;

public class PhotosFragment extends Fragment {

    private RecyclerView recyclerView;
    private ImageGridAdapter adapter;

    private ArrayList<Uri> imageUris = new ArrayList<>();

    private ContentObserver mediaObserver;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE_DELAY = 600; // ms

    private final Runnable reloadRunnable = () -> {
        if (!isAdded()) return;

        Executors.newSingleThreadExecutor().execute(() -> {

            Context context = getContext();
            if (context == null) return;
            ArrayList<HashItem> allImages = ImageFetcher.loadAllImages(context);

            ArrayList<Uri> newUris = new ArrayList<>();
            for (HashItem item : allImages) {
                newUris.add(item.uri);
            }

            requireActivity().runOnUiThread(() -> {
                imageUris.clear();
                imageUris.addAll(newUris);
                adapter.notifyDataSetChanged();
            });
        });
    };

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photos, container, false);

        recyclerView = view.findViewById(R.id.recyclerImages);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        recyclerView.setHasFixedSize(true);

        adapter = new ImageGridAdapter(requireContext(), imageUris, this);
        recyclerView.setAdapter(adapter);

        loadInitialImages();

        return view;
    }

    private void loadInitialImages() {
        Executors.newSingleThreadExecutor().execute(() -> {

            ArrayList<HashItem> allImages =
                    ImageFetcher.loadAllImages(requireContext());

            ArrayList<Uri> uris = new ArrayList<>();
            for (HashItem item : allImages) {
                uris.add(item.uri);
            }

            requireActivity().runOnUiThread(() -> {
                imageUris.clear();
                imageUris.addAll(uris);
                adapter.notifyDataSetChanged();
            });
        });
    }

    // 🔥 ContentObserver setup
    private void setupContentObserver() {

        if (mediaObserver != null) return;

        mediaObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                super.onChange(selfChange);

                debounceHandler.removeCallbacks(reloadRunnable);
                debounceHandler.postDelayed(reloadRunnable, DEBOUNCE_DELAY);
            }
        };

        ContentResolver resolver = requireContext().getContentResolver();

        resolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.registerContentObserver(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    true,
                    mediaObserver
            );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupContentObserver();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mediaObserver != null) {
            requireContext().getContentResolver()
                    .unregisterContentObserver(mediaObserver);
            mediaObserver = null;
        }

        debounceHandler.removeCallbacksAndMessages(null);
    }
}
