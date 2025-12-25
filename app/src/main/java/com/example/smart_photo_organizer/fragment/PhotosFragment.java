package com.example.smart_photo_organizer.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smart_photo_organizer.R;
import com.example.smart_photo_organizer.adapter.ImageGridAdapter;


import java.util.ArrayList;

public class PhotosFragment extends Fragment {

    private static final int STORAGE_PERMISSION_CODE = 200;

    RecyclerView recyclerView;
    ArrayList<String> allImages = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photos, container, false);
        recyclerView = view.findViewById(R.id.recyclerImages);
        recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));

        loadAllImages();
        return view;
    }

    private void loadAllImages() {
        allImages.clear();

        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media.DATA};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        Cursor cursor = getContext()
                .getContentResolver()
                .query(uri, projection, null,null, sortOrder);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                allImages.add(cursor.getString(0));
            }
            cursor.close();
        }

        recyclerView.setAdapter(
                new ImageGridAdapter(requireContext(), allImages, this)
        );
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllImages();
    }
}