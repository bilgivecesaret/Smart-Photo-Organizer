package com.example.smart_photo_organizer.fragment;

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
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;
import com.example.smart_photo_organizer.model.HashItem;
import com.example.smart_photo_organizer.util.ImageFetcher;

import java.util.ArrayList;
import java.util.List;

public class PhotosFragment extends Fragment {

    private ImageGridAdapter adapter;
    private ProgressBar progressBar;

    private final ArrayList<Uri> imageUris = new ArrayList<>();

    private ContentObserver observer;
    private final Handler debounceHandler = new Handler(Looper.getMainLooper());
    private static final long DEBOUNCE = 600;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.fragment_photos, container, false
        );

        RecyclerView recyclerView = view.findViewById(R.id.recyclerImages);
        progressBar = view.findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(
                new GridLayoutManager(requireContext(), 4)
        );

        adapter = new ImageGridAdapter(
                requireContext(),
                imageUris,
                this
        );
        recyclerView.setAdapter(adapter);

        loadImages();

        return view;
    }

    private void loadImages() {
        imageUris.clear();
        adapter.notifyDataSetChanged();

        ImageFetcher.loadAllImagesAsync(
                requireContext(),
                20,
                new ImageFetcher.ImageBatchCallback() {

                    @Override
                    public void onBatch(List<HashItem> batch) {
                        int start = imageUris.size();
                        for (HashItem item : batch) {
                            imageUris.add(item.uri);
                        }
                        adapter.notifyItemRangeInserted(
                                start,
                                batch.size()
                        );
                    }

                    @Override
                    public void onComplete() {
                        progressBar.setVisibility(View.GONE);
                        showAppropriateFragment();
                    }
                }
        );
    }

    private void showAppropriateFragment() {
        if (imageUris.isEmpty()) {
            // Resim yoksa NoImageFragment göster
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new NoImageFragment())
                    .commit();
        } else {
            // Resim varsa PhotosFragment göster
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, this)
                    .commit();
        }
    }


    private void setupObserver() {
        if (observer != null) return;

        observer = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                debounceHandler.removeCallbacksAndMessages(null);
                debounceHandler.postDelayed(() -> {
                    // Resimleri tekrar yükle
                    loadImages();
                }, DEBOUNCE);
            }
        };

        requireContext().getContentResolver()
                .registerContentObserver(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        true,
                        observer
                );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireContext().getContentResolver()
                    .registerContentObserver(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            true,
                            observer
                    );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setupObserver();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (observer != null) {
            requireContext().getContentResolver()
                    .unregisterContentObserver(observer);
            observer = null;
        }
        debounceHandler.removeCallbacksAndMessages(null);
    }
}
