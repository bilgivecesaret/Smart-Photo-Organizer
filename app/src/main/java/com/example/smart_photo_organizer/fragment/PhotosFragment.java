package com.example.smart_photo_organizer.fragment;

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

import java.io.File;
import java.util.ArrayList;

public class PhotosFragment extends Fragment {

    RecyclerView recyclerView;
    ArrayList<String> allImages = new ArrayList<>();
    private ContentObserver mediaObserver;

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

    private void setupContentObserver() {
        if (mediaObserver != null) return;

        mediaObserver = new ContentObserver(null) {
            @Override
            public void onChange(boolean selfChange) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> loadAllImages());
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


    private void loadAllImages() {
        allImages.clear();

        ArrayList<String> existingPaths = new ArrayList<>(); // dosya yolları kontrol için

        // 1️⃣ MediaStore Images
        try (Cursor cursor = requireContext().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA},
                MediaStore.MediaColumns.MIME_TYPE + " LIKE ?",
                new String[]{"image/%"},
                MediaStore.MediaColumns.DATE_ADDED + " DESC"
        )) {
            if (cursor != null) {
                int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                int dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idCol);
                    String path = cursor.getString(dataCol);
                    Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                    if (!existingPaths.contains(path)) {
                        existingPaths.add(path);
                        allImages.add(uri.toString());
                    }
                }
            }
        }

        // 2️⃣ MediaStore Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try (Cursor cursor = requireContext().getContentResolver().query(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE},
                    MediaStore.MediaColumns.MIME_TYPE + " LIKE ?",
                    new String[]{"image/%"},
                    MediaStore.MediaColumns.DATE_ADDED + " DESC"
            )) {
                if (cursor != null) {
                    int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
                    int dataCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);

                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idCol);
                        String path = cursor.getString(dataCol);
                        Uri uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);

                        if (!existingPaths.contains(path)) {
                            existingPaths.add(path);
                            allImages.add(uri.toString());
                        }
                    }
                }
            }
        }

        // 3️⃣ Dosya sistemi Downloads klasörü
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir.exists() && downloadDir.isDirectory()) {
            File[] files = downloadDir.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png");
            });

            if (files != null) {
                for (File f : files) {
                    String path = f.getAbsolutePath();
                    if (!existingPaths.contains(path)) {
                        existingPaths.add(path);
                        Uri uri = Uri.fromFile(f);
                        allImages.add(uri.toString());

                        // MediaScanner ile indexle
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaScannerConnection.scanFile(
                                    requireContext(),
                                    new String[]{path},
                                    new String[]{"image/*"},
                                    null
                            );
                        }
                    }
                }
            }
        }

        recyclerView.setAdapter(new ImageGridAdapter(requireContext(), allImages, this));
    }



    private void loadFromMediaStore(Uri collectionUri) {

        String[] projection = {
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.MIME_TYPE
        };

        try (Cursor cursor = requireContext().getContentResolver().query(
                collectionUri,
                projection,
                MediaStore.MediaColumns.MIME_TYPE + " LIKE ?",
                new String[]{"image/%"},
                MediaStore.MediaColumns.DATE_ADDED + " DESC"
        )) {

            if (cursor == null) return;

            int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                Uri uri = ContentUris.withAppendedId(collectionUri, id);

                String uriStr = uri.toString();
                if (!allImages.contains(uriStr)) {
                    allImages.add(uriStr);
                }
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        setupContentObserver();
        loadAllImages();
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
