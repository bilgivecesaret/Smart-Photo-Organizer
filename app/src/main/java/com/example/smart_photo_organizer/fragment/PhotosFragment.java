package com.example.smart_photo_organizer.fragment;

import static com.example.smart_photo_organizer.util.LoadingImage.loadAllImages;

import android.content.ContentUris;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;
import com.example.smart_photo_organizer.model.HashItem;

import java.io.File;
import java.util.ArrayList;

public class PhotosFragment extends Fragment {

    RecyclerView recyclerView;
    ArrayList<HashItem> allImages;
    private ContentObserver mediaObserver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photos, container, false);
        recyclerView = view.findViewById(R.id.recyclerImages);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        allImages = loadAllImages(requireContext());

        ArrayList<Uri> imageUris = new ArrayList<>();
        for (HashItem item : allImages) {
            imageUris.add(item.uri);
        }

        recyclerView.setAdapter(new ImageGridAdapter(requireContext(), imageUris, this));

        return view;
    }

    private void setupContentObserver() {
        if (mediaObserver != null) return;

        mediaObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> loadAllImages(requireContext()));
                }
            }
        };

        requireContext().getContentResolver().registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                mediaObserver
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requireContext().getContentResolver().registerContentObserver(
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
        loadAllImages(this.requireContext());
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaObserver != null) {
            requireContext().getContentResolver().unregisterContentObserver(mediaObserver);
            mediaObserver = null;
        }
    }
}
